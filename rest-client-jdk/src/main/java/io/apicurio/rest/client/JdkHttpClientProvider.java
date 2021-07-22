package io.apicurio.rest.client;

import io.apicurio.rest.client.auth.Auth;
import io.apicurio.rest.client.error.RestClientErrorHandler;
import io.apicurio.rest.client.spi.ApicurioHttpClient;
import io.apicurio.rest.client.spi.ApicurioHttpClientProvider;

import java.util.Map;

public class JdkHttpClientProvider implements ApicurioHttpClientProvider {

    @Override
    public ApicurioHttpClient create(String endpoint, Map<String, Object> configs, Auth auth, RestClientErrorHandler errorHandler) {
        return new JdkHttpClient(endpoint, configs, auth, errorHandler);
    }
}
