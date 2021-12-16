package io.apicurio.rest.client.auth;

import io.apicurio.rest.client.auth.exception.AuthErrorHandler;
import io.apicurio.rest.client.auth.exception.AuthException;
import io.apicurio.rest.client.auth.exception.NotAuthorizedException;
import io.apicurio.rest.client.spi.ApicurioHttpClient;
import io.apicurio.rest.client.spi.ApicurioHttpClientFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class AuthTest {

    private static final KeycloakTestResource keycloakTestResource = new KeycloakTestResource();

    String adminClientId = "registry-api";
    String testUsername = "sr-test-user";
    String testPassword = "sr-test-password";

    private static ApicurioHttpClient httpClient;

    private OidcAuth createOidcAuth(String authServerUrl, String adminClientId, String test1) {
        return new OidcAuth(httpClient, adminClientId, "test1");
    }

    @BeforeAll
    public static void init() {
        keycloakTestResource.start();
        httpClient = ApicurioHttpClientFactory.create(keycloakTestResource.getAuthServerUrl(), Collections.emptyMap(), null, new AuthErrorHandler());

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
        final String authenticate = auth.obtainAccessTokenPasswordGrant(testUsername, testPassword);

        Assertions.assertNotNull(authenticate);
    }

    @Test
    public void basicAuthOidcTestWrondCreds() {
        OidcAuth auth = createOidcAuth(keycloakTestResource.getAuthServerUrl(), adminClientId, "test1");
        Assertions.assertThrows(NotAuthorizedException.class, () -> auth.obtainAccessTokenPasswordGrant(testUsername, "22222"));
    }

    @Test
    public void basicAuthNonExistingClient() {
        OidcAuth auth = createOidcAuth(keycloakTestResource.getAuthServerUrl(), "NonExistingClient", "test1");
        Assertions.assertThrows(AuthException.class, () -> auth.obtainAccessTokenPasswordGrant(testUsername, testPassword));
    }

    @AfterAll
    public static void close() {
        keycloakTestResource.stop();
    }
}
