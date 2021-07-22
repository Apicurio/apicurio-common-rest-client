package io.apicurio.rest.client.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.apicurio.rest.client.error.RestClientErrorHandler;
import io.apicurio.rest.client.error.ApicurioRestClientException;

import java.io.InputStream;

public class AuthErrorHandler implements RestClientErrorHandler {

    @Override
    public ApicurioRestClientException handleErrorResponse(InputStream body, int statusCode) {
        return null;
    }

    @Override
    public ApicurioRestClientException parseError(Exception ex) {
        return null;
    }

    @Override
    public ApicurioRestClientException parseInputSerializingError(JsonProcessingException ex) {
        return null;
    }
}
