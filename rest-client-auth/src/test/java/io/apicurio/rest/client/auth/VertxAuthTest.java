package io.apicurio.rest.client.auth;

import io.vertx.core.Vertx;

public class VertxAuthTest extends AuthTest {

    @Override
    public OidcAuth createOidcAuth(String authServerUrl, String adminClientId, String test1) {
        return new OidcAuth(Vertx.vertx(), authServerUrl, adminClientId, test1);
    }
}
