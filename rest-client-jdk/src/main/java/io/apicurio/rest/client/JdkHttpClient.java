package io.apicurio.rest.client;


import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import io.apicurio.rest.client.auth.Auth;
import io.apicurio.rest.client.config.ApicurioClientConfig;
import io.apicurio.rest.client.error.RestClientErrorHandler;
import io.apicurio.rest.client.handler.BodyHandler;
import io.apicurio.rest.client.request.Request;
import io.apicurio.rest.client.spi.ApicurioHttpClient;
import io.apicurio.rest.client.util.UriUtil;

/**
 * @author Carles Arnal 'carnalca@redhat.com'
 */
public class JdkHttpClient implements ApicurioHttpClient {

    public static final String INVALID_EMPTY_HTTP_KEY = "";
    private final HttpClient client;
    private final String endpoint;
    private final Auth auth;
    private final RestClientErrorHandler errorHandler;

    private final Map<String, String> DEFAULT_HEADERS = new HashMap<>();
    private static final ThreadLocal<Map<String, String>> requestHeaders = ThreadLocal.withInitial(Collections::emptyMap);

    public JdkHttpClient(String endpoint, Map<String, Object> configs, Auth auth, RestClientErrorHandler errorHandler) {
        if (!endpoint.endsWith("/")) {
            endpoint += "/";
        }
        Object disableAutoBasePathAppend = configs.get(ApicurioClientConfig.APICURIO_CLIENT_DISABLE_AUTO_BASE_PATH_APPEND);
        if (!toBoolean(disableAutoBasePathAppend)) {
            if (!endpoint.endsWith(String.valueOf(configs.get(ApicurioClientConfig.APICURIO_CLIENT_AUTO_BASE_PATH)))) {
                endpoint += configs.getOrDefault(ApicurioClientConfig.APICURIO_CLIENT_AUTO_BASE_PATH, "");
            }
        }

        final HttpClient.Builder httpClientBuilder = handleConfiguration(configs);
        this.endpoint = endpoint;
        this.auth = auth;
        this.client = httpClientBuilder.build();
        this.errorHandler = errorHandler;
    }

    private HttpClient.Builder handleConfiguration(Map<String, Object> configs) {
        HttpClient.Builder clientBuilder = HttpClient.newBuilder();
        clientBuilder.version(HttpClient.Version.HTTP_1_1);
        addHeaders(configs);
        clientBuilder = addSSL(clientBuilder, configs);
        return clientBuilder;
    }

    private void addHeaders(Map<String, Object> configs) {

        Map<String, String> requestHeaders = configs.entrySet().stream()
                .filter(map -> map.getKey().startsWith(ApicurioClientConfig.APICURIO_REQUEST_HEADERS_PREFIX))
                .collect(Collectors.toMap(map -> map.getKey()
                        .replace(ApicurioClientConfig.APICURIO_REQUEST_HEADERS_PREFIX, ""), map -> map.getValue().toString()));

        if (!requestHeaders.isEmpty()) {
            requestHeaders.remove(INVALID_EMPTY_HTTP_KEY);
            DEFAULT_HEADERS.putAll(requestHeaders);
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

        if (configs.containsKey(ApicurioClientConfig.APICURIO_REQUEST_TRUSTSTORE_LOCATION)) {
            String truststoreType = (String) configs.getOrDefault(ApicurioClientConfig.APICURIO_REQUEST_TRUSTSTORE_TYPE, "JKS");
            KeyStore truststore = KeyStore.getInstance(truststoreType);
            String truststorePwd = (String) configs.getOrDefault(ApicurioClientConfig.APICURIO_REQUEST_TRUSTSTORE_PASSWORD, "");
            truststore.load(new FileInputStream((String) configs.get(ApicurioClientConfig.APICURIO_REQUEST_TRUSTSTORE_LOCATION)), truststorePwd.toCharArray());
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(truststore);
            trustManagers = trustManagerFactory.getTrustManagers();
        } else if (configs.containsKey(ApicurioClientConfig.APICURIO_REQUEST_CA_BUNDLE_LOCATION)) {
            File serviceCaFile = new File((String)configs.get(ApicurioClientConfig.APICURIO_REQUEST_CA_BUNDLE_LOCATION));
            KeyStore truststore = KeyStore.getInstance("JKS");
            truststore.load(null);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            truststore.setCertificateEntry("service-ca", cf.generateCertificate(new FileInputStream(serviceCaFile)));

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(truststore);
            trustManagers = trustManagerFactory.getTrustManagers();
        }
        return trustManagers;
    }

    private static KeyManager[] getKeyManagers(Map<String, Object> configs) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        KeyManager[] keyManagers = null;

        if (configs.containsKey(ApicurioClientConfig.APICURIO_REQUEST_KEYSTORE_LOCATION)) {
            String keystoreType = (String) configs.getOrDefault(ApicurioClientConfig.APICURIO_REQUEST_KEYSTORE_TYPE, "JKS");
            KeyStore keystore = KeyStore.getInstance(keystoreType);
            String keyStorePwd = (String) configs.getOrDefault(ApicurioClientConfig.APICURIO_REQUEST_KEYSTORE_PASSWORD, "");
            keystore.load(new FileInputStream((String) configs.get(ApicurioClientConfig.APICURIO_REQUEST_KEYSTORE_LOCATION)), keyStorePwd.toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            // If no key password provided, try using the keystore password
            String keyPwd = (String) configs.getOrDefault(ApicurioClientConfig.APICURIO_REQUEST_KEY_PASSWORD, keyStorePwd);
            keyManagerFactory.init(keystore, keyPwd.toCharArray());
            keyManagers = keyManagerFactory.getKeyManagers();
        }
        return keyManagers;
    }

    @Override
    public <T> T sendRequest(Request<T> request) {
        try {
            requireNonNull(request.getOperation(), "Request operation cannot be null");
            requireNonNull(request.getResponseType(), "Response type cannot be null");

            final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(UriUtil.buildURI(endpoint + request.getRequestPath(), request.getQueryParams(), request.getPathParams()));

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
                    if (request.getDataString() != null) {
                        requestBuilder.POST(HttpRequest.BodyPublishers.ofString(request.getDataString()));
                    } else {
                        requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(request.getData().readAllBytes()));
                    }
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

        } catch (IOException | InterruptedException e) {
            throw errorHandler.parseError(e);
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

    @Override
    public void close() {}
}
