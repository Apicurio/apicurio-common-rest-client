package io.apicurio.rest.client.auth.exception;

import io.apicurio.rest.client.error.ApicurioRestClientException;

/**
 * @author Carles Arnal 'carnalca@redhat.com'
 */
public class NotAuthorizedException extends ApicurioRestClientException {

    private static final long serialVersionUID = 1L;

    public NotAuthorizedException(String error) {
        super(error);
    }
}
