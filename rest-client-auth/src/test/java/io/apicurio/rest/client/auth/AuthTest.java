package io.apicurio.rest.client.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AuthTest {

    private static final KeycloakTestResource keycloakTestResource = new KeycloakTestResource();

    String adminClientId = "registry-api";
    String testUsername = "sr-test-user";
    String testPassword = "sr-test-password";

    public OidcAuth createOidcAuth(String authServerUrl, String adminClientId, String test1) {
        return new OidcAuth(authServerUrl, adminClientId, "test1");
    }

    @BeforeAll
    public static void init() {
        keycloakTestResource.start();
    }

    @Test
    public void oidcAuthTest() {
        OidcAuth auth = createOidcAuth(keycloakTestResource.getAuthServerUrl(), adminClientId, "test1");
        final String authenticate = auth.authenticate();

        Assertions.assertNotNull(authenticate);
    }

    @Test
    public void basicAuthOidcTest() {
        OidcAuth auth = createOidcAuth(keycloakTestResource.getAuthServerUrl(), adminClientId, "test1");
        final String authenticate = auth.obtainAccessTokenWithBasicCredentials(testUsername, testPassword);

        Assertions.assertNotNull(authenticate);
    }
}
