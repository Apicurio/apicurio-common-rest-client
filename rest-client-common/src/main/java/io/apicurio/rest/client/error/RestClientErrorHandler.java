package io.apicurio.rest.client.error;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.InputStream;

public interface RestClientErrorHandler {

    public ApicurioRestClientException handleErrorResponse(InputStream body, int statusCode);

    public ApicurioRestClientException parseError(Exception ex);

    public ApicurioRestClientException parseInputSerializingError(JsonProcessingException ex);
}
