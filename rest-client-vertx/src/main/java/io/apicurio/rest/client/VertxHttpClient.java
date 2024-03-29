package io.apicurio.rest.client;

import io.apicurio.rest.client.auth.Auth;
import io.apicurio.rest.client.config.ApicurioClientConfig;
import io.apicurio.rest.client.error.RestClientErrorHandler;
import io.apicurio.rest.client.request.Request;
import io.apicurio.rest.client.response.ResponseHandler;
import io.apicurio.rest.client.spi.ApicurioHttpClient;
import io.apicurio.rest.client.util.ConcurrentUtil;
import io.apicurio.rest.client.util.IoUtil;
import io.apicurio.rest.client.util.UriUtil;
import io.vertx.core.Context;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author Carles Arnal 'carnalca@redhat.com'
 */
public class VertxHttpClient implements ApicurioHttpClient {

    private final WebClient webClient;
    private final Auth auth;
    private final String basePath;
    private final RestClientErrorHandler errorHandler;

    private final Map<String, String> DEFAULT_HEADERS = new HashMap<>();
    private static final ThreadLocal<Map<String, String>> requestHeaders = ThreadLocal.withInitial(Collections::emptyMap);

    public VertxHttpClient(Vertx vertx, String basePath, Map<String, Object> options, Auth auth, RestClientErrorHandler errorHandler) {
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }

        Object disableAutoBasePathAppend = options.get("apicurio.rest.client.disable-auto-basepath-append");
        if (!toBoolean(disableAutoBasePathAppend) && !basePath.endsWith(String.valueOf(options.get("apicurio.rest.client.auto-base-path")))) {
            basePath = basePath + options.getOrDefault("apicurio.rest.client.auto-base-path", "");
        }

        this.webClient = WebClient.create(vertx, createClientOptions(options));
        this.auth = auth;
        this.basePath = basePath;
        this.errorHandler = errorHandler;
        processConfiguration(options);
    }

    private WebClientOptions createClientOptions(Map<String, Object> config) {
        WebClientOptions options = new WebClientOptions();
        if (config.containsKey(ApicurioClientConfig.APICURIO_REQUEST_CA_BUNDLE_LOCATION)) {
            options.setPemTrustOptions(new PemTrustOptions().addCertPath((String)config.get(ApicurioClientConfig.APICURIO_REQUEST_CA_BUNDLE_LOCATION)));
            options.setSsl(true);
        }
        return options;
    }

    private void processConfiguration(Map<String, Object> configs) {
        Map<String, String> requestHeaders = configs.entrySet().stream()
                .filter(map -> map.getKey().startsWith(ApicurioClientConfig.APICURIO_REQUEST_HEADERS_PREFIX))
                .collect(Collectors.toMap(map -> map.getKey()
                        .replace(ApicurioClientConfig.APICURIO_REQUEST_HEADERS_PREFIX, ""), map -> map.getValue().toString()));

        if (!requestHeaders.isEmpty()) {
            DEFAULT_HEADERS.putAll(requestHeaders);
        }
    }

    @Override
    public <T> T sendRequest(Request<T> request) {
        if (Context.isOnEventLoopThread()) {
            throw new UnsupportedOperationException("Must not be called on event loop");
        }

        final URI uri = UriUtil.buildURI(basePath + request.getRequestPath(), request.getQueryParams(), request.getPathParams());
        final RequestOptions requestOptions = new RequestOptions();

        requestOptions.setHost(uri.getHost());
        requestOptions.setURI(uri.getPath());
        requestOptions.setAbsoluteURI(uri.toString());

        DEFAULT_HEADERS.forEach(requestOptions::addHeader);

        //Add current request headers
        requestHeaders.get().forEach(requestOptions::addHeader);
        requestHeaders.remove();

        Map<String, String> headers = request.getHeaders();
        if (this.auth != null) {
            //make headers mutable...
            headers = new HashMap<>(headers);
            this.auth.apply(headers);
        }
        headers.forEach(requestOptions::addHeader);

        CompletableFuture<T> resultHolder;

        final String uriString = uri.toString();

        switch (request.getOperation()) {
            case GET:
                resultHolder = executeGet(request, requestOptions.getHeaders(), uriString);
                break;
            case PUT:
                resultHolder = executePut(request, requestOptions.getHeaders(), uriString);
                break;
            case POST:
                resultHolder = executePost(request, requestOptions.getHeaders(), uriString);
                break;
            case DELETE:
                resultHolder = executeDelete(request, requestOptions.getHeaders(), uriString);
                break;
            default:
                throw new IllegalStateException("Operation not allowed");
        }

        return ConcurrentUtil.result(resultHolder);
    }

    private <T> CompletableFuture<T> executeGet(Request<T> request, MultiMap requestHeaders, String absoluteUri) {
        return sendRequestWithoutPayload(HttpMethod.GET, request, requestHeaders, absoluteUri);
    }

    private <T> CompletableFuture<T> executeDelete(Request<T> request, MultiMap requestHeaders, String absoluteUri) {
        return sendRequestWithoutPayload(HttpMethod.DELETE, request, requestHeaders, absoluteUri);
    }

    private <T> CompletableFuture<T> executePost(Request<T> request, MultiMap requestHeaders, String absoluteUri) {
        return sendRequestWithPayload(HttpMethod.POST, request, requestHeaders, absoluteUri);
    }

    private <T> CompletableFuture<T> executePut(Request<T> request, MultiMap requestHeaders, String absoluteUri) {
        return sendRequestWithPayload(HttpMethod.PUT, request, requestHeaders, absoluteUri);
    }

    private <T> CompletableFuture<T> sendRequestWithoutPayload(HttpMethod httpMethod, Request<T> request, MultiMap requestHeaders, String absoluteUri) {
        final HttpRequest<Buffer> httpClientRequest = webClient.requestAbs(httpMethod, absoluteUri);
        httpClientRequest.putHeaders(requestHeaders);

        //Iterate over query params list so we can add multiple query params with the same key
        request.getQueryParams().forEach((key, paramList) -> paramList
                .forEach(value -> httpClientRequest.setQueryParam(key, value)));

        final CompletableFuture<T> resultHolder = new CompletableFuture<T>();
        final ResponseHandler<T> responseHandler = new ResponseHandler<>(resultHolder, request.getResponseType(), errorHandler);
        httpClientRequest.send(responseHandler);
        return resultHolder;
    }

    private <T> CompletableFuture<T> sendRequestWithPayload(HttpMethod httpMethod, Request<T> request, MultiMap requestHeaders, String absoluteUri) {
        final HttpRequest<Buffer> httpClientRequest = webClient.requestAbs(httpMethod, absoluteUri);
        httpClientRequest.putHeaders(requestHeaders);
        final CompletableFuture<T> resultHolder = new CompletableFuture<T>();

        //Iterate over query params list so we can add multiple query params with the same key
        request.getQueryParams().forEach((key, paramList) -> paramList
                .forEach(value -> httpClientRequest.setQueryParam(key, value)));

        final ResponseHandler<T> responseHandler = new ResponseHandler<>(resultHolder, request.getResponseType(), errorHandler);

        Buffer buffer;
        if (request.getData() != null) {
            buffer = Buffer.buffer(IoUtil.toBytes(request.getData()));
        } else {
            buffer = Buffer.buffer(IoUtil.toBytes(request.getDataString()));
        }
        httpClientRequest.sendBuffer(buffer, responseHandler);

        return resultHolder;
    }

    private static boolean toBoolean(Object parameter) {
        if (parameter == null) {
            return false;
        } else if (parameter instanceof Boolean) {
            return (Boolean)parameter;
        } else {
            return parameter instanceof String && Boolean.parseBoolean((String) parameter);
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

    @Override
    public void close() {
        webClient.close();
    }
}
