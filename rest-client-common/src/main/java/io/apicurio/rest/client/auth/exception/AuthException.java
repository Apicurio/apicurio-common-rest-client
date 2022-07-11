package io.apicurio.rest.client.auth.exception;

import io.apicurio.rest.client.error.ApicurioRestClientException;

public class AuthException extends ApicurioRestClientException {

    private static final long serialVersionUID = -1633015340711808478L;

    public AuthException(String error) {
        super(error);
    }

    public AuthException(Throwable throwable) {
        super(throwable);
    }
}
