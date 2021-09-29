package io.apicurio.rest.client.auth;

import io.apicurio.rest.client.VertxHttpClientProvider;
import io.apicurio.rest.client.auth.exception.AuthException;
import io.apicurio.rest.client.auth.exception.NotAuthorizedException;
import io.apicurio.rest.client.spi.ApicurioHttpClientProvider;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

public class VertxAuthTest {

    private static final KeycloakTestResource keycloakTestResource = new KeycloakTestResource();

    String adminClientId = "registry-api";
    String testUsername = "sr-test-user";
    String testPassword = "sr-test-password";

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

    @AfterAll
    public static void close() {
        keycloakTestResource.stop();
    }

    private OidcAuth createOidcAuth(String authServerUrl, String adminClientId, String test1) {
        final ApicurioHttpClientProvider provider = new VertxHttpClientProvider(Vertx.vertx());
        return new OidcAuth(provider, authServerUrl, adminClientId, test1, Optional.empty());
    }
}
