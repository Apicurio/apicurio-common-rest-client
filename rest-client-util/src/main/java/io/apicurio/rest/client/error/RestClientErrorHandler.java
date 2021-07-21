package io.apicurio.rest.client.error;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.InputStream;

public interface RestClientErrorHandler {

    public RestClientException handleErrorResponse(InputStream body, int statusCode);

    public RestClientException parseError(Exception ex);

    public RestClientException parseInputSerializingError(JsonProcessingException ex);
}
