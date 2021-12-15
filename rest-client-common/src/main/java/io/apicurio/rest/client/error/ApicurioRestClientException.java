package io.apicurio.rest.client.error;

/**
 * @author Carles Arnal 'carles.arnal@redhat.com'
 */
public abstract class ApicurioRestClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ApicurioRestClientException(String error) {
        super(error);
    }
}
