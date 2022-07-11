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
        switch (statusCode) {
            case UNAUTHORIZED_CODE:
                return new NotAuthorizedException(body != null ? IoUtil.toString(body) : "");
            case FORBIDDEN_CODE:
                return new ForbiddenException(body != null ? IoUtil.toString(body) : "");
            default:
                return new AuthException(body != null ? IoUtil.toString(body) : "");
        }
    }

    @Override
    public ApicurioRestClientException parseError(Exception ex) {
        throw new AuthException(ex);
    }

    @Override
    public ApicurioRestClientException parseInputSerializingError(JsonProcessingException ex) {
        return new AuthException(ex);
    }
}
