package io.apicurio.rest.client;


import io.apicurio.rest.client.auth.Auth;
import io.apicurio.rest.client.config.ClientConfig;
import io.apicurio.rest.client.error.RestClientErrorHandler;
import io.apicurio.rest.client.handler.BodyHandler;
import io.apicurio.rest.client.request.Request;
import io.apicurio.rest.client.spi.ApicurioHttpClient;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Carles Arnal 'carnalca@redhat.com'
 */

//TODO right now this is just a copy of the jdk http client in Apicurio Registry, we need to extract all the config parameters to new ones and fix them in registry
public class JdkHttpClient implements ApicurioHttpClient {

    private static final String BASE_PATH = "apis/registry/v2/";

    private final HttpClient client;
    private final String endpoint;
    private final Auth auth;
    private final RestClientErrorHandler errorHandler;

    private static final Map<String, String> DEFAULT_HEADERS = new HashMap<>();
    private static final ThreadLocal<Map<String, String>> requestHeaders = ThreadLocal.withInitial(Collections::emptyMap);

    public JdkHttpClient(String endpoint, Map<String, Object> configs, Auth auth, RestClientErrorHandler errorHandler) {
        if (!endpoint.endsWith("/")) {
            endpoint += "/";
        }
        Object disableAutoBasePathAppend = configs.get(ClientConfig.REGISTRY_CLIENT_DISABLE_AUTO_BASE_PATH_APPEND);
        if (!toBoolean(disableAutoBasePathAppend)) {
            if (!endpoint.endsWith(BASE_PATH)) {
                endpoint += BASE_PATH;
            }
        }

        final HttpClient.Builder httpClientBuilder = handleConfiguration(configs);
        this.endpoint = endpoint;
        this.auth = auth;
        this.client = httpClientBuilder.build();
        this.errorHandler = errorHandler;
    }

    private static HttpClient.Builder handleConfiguration(Map<String, Object> configs) {
        HttpClient.Builder clientBuilder = HttpClient.newBuilder();
        clientBuilder.version(HttpClient.Version.HTTP_1_1);
        addHeaders(configs);
        clientBuilder = addSSL(clientBuilder, configs);
        return clientBuilder;
    }

    private static void addHeaders(Map<String, Object> configs) {

        Map<String, String> requestHeaders = configs.entrySet().stream()
                .filter(map -> map.getKey().startsWith(ClientConfig.REGISTRY_REQUEST_HEADERS_PREFIX))
                .collect(Collectors.toMap(map -> map.getKey()
                        .replace(ClientConfig.REGISTRY_REQUEST_HEADERS_PREFIX, ""), map -> map.getValue().toString()));

        if (!requestHeaders.isEmpty()) {
            requestHeaders.forEach(DEFAULT_HEADERS::put);
        }
    }

    private static HttpClient.Builder addSSL(HttpClient.Builder httpClientBuilder, Map<String, Object> configs) {

        try {
            KeyManager[] keyManagers = getKeyManagers(configs);
            TrustManager[] trustManagers = getTrustManagers(configs);

            if (trustManagers != null && (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager))) {
                throw new IllegalStateException("A single X509TrustManager is expected. Unexpected trust managers: " + Arrays.toString(trustManagers));
            }

            if (keyManagers != null || trustManagers != null) {
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(keyManagers, trustManagers, new SecureRandom());
                return httpClientBuilder.sslContext(sslContext);
            } else {
                return httpClientBuilder;
            }
        } catch (IOException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException | CertificateException | KeyManagementException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static TrustManager[] getTrustManagers(Map<String, Object> configs) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        TrustManager[] trustManagers = null;

        if (configs.containsKey(ClientConfig.REGISTRY_REQUEST_TRUSTSTORE_LOCATION)) {
            String truststoreType = (String) configs.getOrDefault(ClientConfig.REGISTRY_REQUEST_TRUSTSTORE_TYPE, "JKS");
            KeyStore truststore = KeyStore.getInstance(truststoreType);
            String truststorePwd = (String) configs.getOrDefault(ClientConfig.REGISTRY_REQUEST_TRUSTSTORE_PASSWORD, "");
            truststore.load(new FileInputStream((String) configs.get(ClientConfig.REGISTRY_REQUEST_TRUSTSTORE_LOCATION)), truststorePwd.toCharArray());
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(truststore);
            trustManagers = trustManagerFactory.getTrustManagers();
        }
        return trustManagers;
    }

    private static KeyManager[] getKeyManagers(Map<String, Object> configs) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        KeyManager[] keyManagers = null;

        if (configs.containsKey(ClientConfig.REGISTRY_REQUEST_KEYSTORE_LOCATION)) {
            String keystoreType = (String) configs.getOrDefault(ClientConfig.REGISTRY_REQUEST_KEYSTORE_TYPE, "JKS");
            KeyStore keystore = KeyStore.getInstance(keystoreType);
            String keyStorePwd = (String) configs.getOrDefault(ClientConfig.REGISTRY_REQUEST_KEYSTORE_PASSWORD, "");
            keystore.load(new FileInputStream((String) configs.get(ClientConfig.REGISTRY_REQUEST_KEYSTORE_LOCATION)), keyStorePwd.toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            // If no key password provided, try using the keystore password
            String keyPwd = (String) configs.getOrDefault(ClientConfig.REGISTRY_REQUEST_KEY_PASSWORD, keyStorePwd);
            keyManagerFactory.init(keystore, keyPwd.toCharArray());
            keyManagers = keyManagerFactory.getKeyManagers();
        }
        return keyManagers;
    }

    @Override
    public <T> T sendRequest(Request<T> request) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(buildURI(endpoint + request.getRequestPath(), request.getQueryParams(), request.getPathParams()));

            DEFAULT_HEADERS.forEach(requestBuilder::header);

            //Add current request headers
            requestHeaders.get().forEach(requestBuilder::header);
            requestHeaders.remove();

            Map<String, String> headers = request.getHeaders();
            if (this.auth != null) {
                //make headers mutable...
                headers = new HashMap<>(headers);
                this.auth.apply(headers);
            }
            headers.forEach(requestBuilder::header);

            switch (request.getOperation()) {
                case GET:
                    requestBuilder.GET();
                    break;
                case PUT:
                    requestBuilder.PUT(HttpRequest.BodyPublishers.ofByteArray(request.getData().readAllBytes()));
                    break;
                case POST:
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(request.getData().readAllBytes()));
                    break;
                case DELETE:
                    requestBuilder.DELETE();
                    break;
                default:
                    throw new IllegalStateException("Operation not allowed");
            }

            return client.send(requestBuilder.build(), new BodyHandler<>(request.getResponseType(), errorHandler))
                    .body()
                    .get();

        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw errorHandler.parseError(e);
        }
    }

    private static URI buildURI(String basePath, Map<String, List<String>> queryParams, List<String> pathParams) throws URISyntaxException {
        final Object[] encodedPathParams = pathParams
                .stream()
                .map(JdkHttpClient::encodeURIComponent)
                .toArray();

        String path = String.format(basePath, encodedPathParams);

        if (!queryParams.isEmpty()) {
            path = path.concat("?");

            //Iterate over query params list so we can add multiple query params with the same key
            for (String key : queryParams.keySet()) {
                for (String value : queryParams.get(key)) {
                    path = path.concat(key).concat("=").concat(value);
                }
            }
        }

        return URI.create(path);
    }

    private static String encodeURIComponent(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void setNextRequestHeaders(Map<String, String> headers) {
        requestHeaders.set(headers);
    }

    @Override
    public Map<String, String> getHeaders() {
        return requestHeaders.get();
    }

    private static boolean toBoolean(Object parameter) {
        if (parameter == null) {
            return false;
        }
        if (parameter instanceof Boolean) {
            return (Boolean) parameter;
        }
        if (parameter instanceof String) {
            return Boolean.parseBoolean((String) parameter);
        }
        return false;
    }
}
