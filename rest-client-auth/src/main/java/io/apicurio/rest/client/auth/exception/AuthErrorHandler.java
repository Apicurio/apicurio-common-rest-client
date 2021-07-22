package io.apicurio.rest.client.auth.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.apicurio.rest.client.error.RestClientErrorHandler;
import io.apicurio.rest.client.error.ApicurioRestClientException;
import io.apicurio.rest.client.util.IoUtil;

import java.io.InputStream;

public class AuthErrorHandler implements RestClientErrorHandler {

    private static final int UNAUTHORIZED_CODE = 401;
    private static final int FORBIDDEN_CODE = 403;

    @Override
    public ApicurioRestClientException handleErrorResponse(InputStream body, int statusCode) {
        if (statusCode == UNAUTHORIZED_CODE) {
            //authorization error
            return new NotAuthorizedException(IoUtil.toString(body));
        } else {
            if (statusCode == FORBIDDEN_CODE) {
                //forbidden error
                return new ForbiddenException(IoUtil.toString(body));
            }
        }
        return null;
    }

    @Override
    public ApicurioRestClientException parseError(Exception ex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ApicurioRestClientException parseInputSerializingError(JsonProcessingException ex) {
        throw new UnsupportedOperationException();
    }
}
