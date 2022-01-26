package io.apicurio.rest.client.auth;

import io.apicurio.rest.client.VertxHttpClientProvider;
import io.apicurio.rest.client.auth.exception.AuthErrorHandler;
import io.apicurio.rest.client.auth.exception.AuthException;
import io.apicurio.rest.client.auth.exception.NotAuthorizedException;
import io.apicurio.rest.client.spi.ApicurioHttpClient;
import io.apicurio.rest.client.spi.ApicurioHttpClientProvider;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;

public class VertxAuthTest {

    private static final KeycloakTestResource keycloakTestResource = new KeycloakTestResource();

    String adminClientId = "registry-api";
    String testUsername = "sr-test-user";
    String testPassword = "sr-test-password";

    private static ApicurioHttpClient httpClient;

    @BeforeAll
    public static void init() {
        final ApicurioHttpClientProvider provider = new VertxHttpClientProvider(Vertx.vertx());
        keycloakTestResource.start();
        httpClient = provider.create(keycloakTestResource.getAuthServerUrl(), Collections.emptyMap(), null, new AuthErrorHandler());
    }

    @Test
    public void oidcAuthTest() throws InterruptedException {
        OidcAuth auth = createOidcAuth(adminClientId);

        //Test token is reused
        String firstToken = auth.authenticate();
        String secondToken = auth.authenticate();
        Assertions.assertEquals(firstToken, secondToken);

        //After 3s the token should be expired since we're subtracting 20s from the expiration time and the lifespan has been set to 20s in the test realm.
        Thread.sleep(3000);
        String thirdToken = auth.authenticate();
        Assertions.assertNotEquals(firstToken, thirdToken);
    }

    @Test
    public void basicAuthOidcTest() {
        OidcAuth auth = createOidcAuth(adminClientId);
        final String authenticate = auth.obtainAccessTokenPasswordGrant(testUsername, testPassword);

        Assertions.assertNotNull(authenticate);
    }

    @Test
    public void basicAuthOidcTestWrondCreds() {
        OidcAuth auth = createOidcAuth(adminClientId);
        Assertions.assertThrows(NotAuthorizedException.class, () -> auth.obtainAccessTokenPasswordGrant(testUsername, "22222"));
    }

    @Test
    public void basicAuthNonExistingClient() {
        OidcAuth auth = createOidcAuth("NonExistingClient");
        Assertions.assertThrows(AuthException.class, () -> auth.obtainAccessTokenPasswordGrant(testUsername, testPassword));
    }

    @AfterAll
    public static void close() {
        keycloakTestResource.stop();
    }

    private OidcAuth createOidcAuth(String adminClientId) {
        return new OidcAuth(httpClient, adminClientId, "test1", Duration.ofSeconds(20));
    }
}
