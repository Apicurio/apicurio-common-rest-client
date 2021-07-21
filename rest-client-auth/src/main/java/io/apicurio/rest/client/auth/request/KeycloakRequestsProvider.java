package io.apicurio.rest.client.auth.request;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.apicurio.rest.client.auth.AccessTokenResponse;
import io.apicurio.rest.client.request.Request;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;

import static io.apicurio.rest.client.request.Operation.POST;

public class KeycloakRequestsProvider {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String TOKEN_ENDPOINT = "/protocol/openid-connect/token";
    private static final String TOKEN_URL_FORMAT = "/realms/%s" + TOKEN_ENDPOINT;

    public static Request<AccessTokenResponse> obtainAccessToken(TokenRequestBody data, String realm) throws JsonProcessingException {
        return new Request.RequestBuilder<AccessTokenResponse>()
                .operation(POST)
                .path(TOKEN_URL_FORMAT)
                .pathParams(Collections.singletonList(realm))
                .data(toStream(mapper.writeValueAsBytes(data)))
                .responseType(new TypeReference<AccessTokenResponse>() {})
                .build();
    }

    public static InputStream toStream(byte [] content) {
        return new ByteArrayInputStream(content);
    }
}
