package io.apicurio.rest.client.response;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.apicurio.rest.client.error.RestClientErrorHandler;
import io.apicurio.rest.client.util.IoUtil;
import io.apicurio.rest.client.util.RegistryDateDeserializer;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carles Arnal 'carnalca@redhat.com'
 */
public class ResponseHandler<T> implements Handler<AsyncResult<HttpResponse<Buffer>>> {

    final CompletableFuture<T> resultHolder;
    final TypeReference<T> targetType;
    final RestClientErrorHandler errorHandler;
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        SimpleModule module = new SimpleModule("Custom date handler");
        module.addDeserializer(Date.class, new RegistryDateDeserializer());
        mapper.registerModule(module);
    }

    public ResponseHandler(CompletableFuture<T> resultHolder, TypeReference<T> targetType, RestClientErrorHandler errorHandler) {
        this.resultHolder = resultHolder;
        this.targetType = targetType;
        this.errorHandler = errorHandler;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handle(AsyncResult<HttpResponse<Buffer>> event) {
        try {
            if (event.result() != null && isFailure(event.result().statusCode())) {
                if (event.result().body() != null) {
                    resultHolder.completeExceptionally(errorHandler.handleErrorResponse(IoUtil.toStream(event.result().body().getBytes()), event.result().statusCode()));
                } else {
                    resultHolder.completeExceptionally(errorHandler.handleErrorResponse(null, event.result().statusCode()));
                }
            } else if (event.succeeded()) {
                final HttpResponse<Buffer> result = event.result();
                final String typeName = targetType.getType().getTypeName();
                if (typeName.contains("InputStream")) {
                    resultHolder.complete((T) IoUtil.toStream(result.body().getBytes()));
                } else if (typeName.contains("Void")) {
                    //Intended null return
                    resultHolder.complete(null);
                } else {
                    resultHolder.complete(mapper.readValue(result.body().getBytes(), targetType));
                }
            } else {
                resultHolder.completeExceptionally(event.cause());
            }
        } catch (Exception e) {
            resultHolder.completeExceptionally(e);
        }
    }

    private static boolean isFailure(int statusCode) {
        return statusCode / 100 != 2;
    }
}
