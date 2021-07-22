package io.apicurio.rest.client;

import io.apicurio.rest.client.auth.Auth;
import io.apicurio.rest.client.error.RestClientErrorHandler;
import io.apicurio.rest.client.spi.ApicurioHttpClient;
import io.apicurio.rest.client.spi.ApicurioHttpClientProvider;
import io.vertx.core.Vertx;

import java.util.Map;

/**
 * @author Carles Arnal 'carnalca@redhat.com'
 */
public class VertxHttpClientProvider implements ApicurioHttpClientProvider {

    private final Vertx vertx;

    public VertxHttpClientProvider(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public ApicurioHttpClient create(String endpoint, Map<String, Object> configs, Auth auth, RestClientErrorHandler errorHandler) {
        return new VertxHttpClient(vertx, endpoint, configs, auth, errorHandler);
    }
}
