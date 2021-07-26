package io.apicurio.rest.client.auth.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.apicurio.rest.client.auth.AccessTokenResponse;
import io.apicurio.rest.client.request.Request;

import java.util.Collections;
import java.util.Map;

import static io.apicurio.rest.client.request.Operation.POST;
import static io.apicurio.rest.client.request.Request.CONTENT_TYPE;

public class TokenRequestsProvider {

    private static final String TOKEN_ENDPOINT = "protocol/openid-connect/token";

    public static Request<AccessTokenResponse> obtainAccessToken(String paramsEncoded) throws JsonProcessingException {
        return new Request.RequestBuilder<AccessTokenResponse>()
                .operation(POST)
                .path(TOKEN_ENDPOINT)
                .data(paramsEncoded)
                .headers(Map.of(CONTENT_TYPE, "application/x-www-form-urlencoded"))
                .responseType(new TypeReference<AccessTokenResponse>() {
                })
                .build();
    }
}
