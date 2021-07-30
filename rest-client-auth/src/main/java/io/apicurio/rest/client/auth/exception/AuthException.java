package io.apicurio.rest.client.auth.exception;

import io.apicurio.rest.client.error.ApicurioRestClientException;
import io.apicurio.rest.client.error.RestClientErrorHandler;

public class AuthException extends ApicurioRestClientException {

    public AuthException(String error) {
        super(error);
    }
}
