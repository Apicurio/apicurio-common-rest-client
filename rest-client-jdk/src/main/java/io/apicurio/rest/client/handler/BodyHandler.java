package io.apicurio.rest.client.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.apicurio.rest.client.error.RestClientErrorHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse;
import java.util.function.Supplier;

/**
 * @author Carles Arnal 'carnalca@redhat.com'
 */
public class BodyHandler<W> implements HttpResponse.BodyHandler<Supplier<W>> {

    private final TypeReference<W> wClass;
    private final RestClientErrorHandler errorHandler;
    private static final ObjectMapper mapper = new ObjectMapper();

    public BodyHandler(TypeReference<W> wClass, RestClientErrorHandler errorHandler) {
        this.wClass = wClass;
        this.errorHandler = errorHandler;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public HttpResponse.BodySubscriber<Supplier<W>> apply(HttpResponse.ResponseInfo responseInfo) {
        return asJSON(wClass, responseInfo, errorHandler);
    }

    public static <W> HttpResponse.BodySubscriber<Supplier<W>> asJSON(TypeReference<W> targetType, HttpResponse.ResponseInfo responseInfo, RestClientErrorHandler errorHandler) {
        HttpResponse.BodySubscriber<InputStream> upstream = HttpResponse.BodySubscribers.ofInputStream();
        return HttpResponse.BodySubscribers.mapping(
                upstream,
                inputStream -> toSupplierOfType(inputStream, targetType, responseInfo, errorHandler));
    }

    @SuppressWarnings("unchecked")
    public static <W> Supplier<W> toSupplierOfType(InputStream body, TypeReference<W> targetType, HttpResponse.ResponseInfo responseInfo, RestClientErrorHandler errorHandler) {
        return () -> {
            try {
                if (isFailure(responseInfo)) {
                    throw errorHandler.handleErrorResponse(body, responseInfo.statusCode());
                } else {
                    //TODO think of a better solution to this
                    final String typeName = targetType.getType().getTypeName();
                    if (typeName.contains("InputStream")) {
                        return (W) body;
                    } else if (typeName.contains("Void")) {
                        //Intended null return
                        return null;
                    } else {
                        return mapper.readValue(body, targetType);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    private static boolean isFailure(HttpResponse.ResponseInfo responseInfo) {
        return responseInfo.statusCode() / 100 != 2;
    }
}
