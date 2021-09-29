package io.apicurio.rest.client.auth.exception;

import io.apicurio.rest.client.error.ApicurioRestClientException;

/**
 * @author Carles Arnal 'carnalca@redhat.com'
 */
public class ForbiddenException extends ApicurioRestClientException {

    private static final long serialVersionUID = 1L;

    public ForbiddenException(String error) {
        super(error);
    }
}
