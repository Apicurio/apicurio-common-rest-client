package io.apicurio.rest.client.auth;

import io.apicurio.rest.client.auth.exception.AuthException;
import io.apicurio.rest.client.auth.exception.NotAuthorizedException;
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
    public void oidcAuthTest() throws InterruptedException {
        OidcAuth auth = createOidcAuth(keycloakTestResource.getAuthServerUrl(), adminClientId, "test1");

        //Test token is reused
        String firstToken = auth.authenticate();
        String secondToken = auth.authenticate();
        Assertions.assertEquals(firstToken, secondToken);

        //Wait until the token is expired
        Thread.sleep(9000);

        String thirdToken = auth.authenticate();
        Assertions.assertNotEquals(firstToken, thirdToken);
    }

    @Test
    public void basicAuthOidcTest() {
        OidcAuth auth = createOidcAuth(keycloakTestResource.getAuthServerUrl(), adminClientId, "test1");
        final String authenticate = auth.obtainAccessTokenWithBasicCredentials(testUsername, testPassword);

        Assertions.assertNotNull(authenticate);
    }

    @Test
    public void basicAuthOidcTestWrondCreds() {
        OidcAuth auth = createOidcAuth(keycloakTestResource.getAuthServerUrl(), adminClientId, "test1");
        Assertions.assertThrows(NotAuthorizedException.class, () -> auth.obtainAccessTokenWithBasicCredentials(testUsername, "22222"));
    }

    @Test
    public void basicAuthNonExistingClient() {
        OidcAuth auth = createOidcAuth(keycloakTestResource.getAuthServerUrl(), "NonExistingClient", "test1");
        Assertions.assertThrows(AuthException.class, () -> auth.obtainAccessTokenWithBasicCredentials(testUsername, testPassword));
    }
}
