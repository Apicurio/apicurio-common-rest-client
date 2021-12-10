package io.apicurio.rest.client.spi;

import io.apicurio.rest.client.auth.Auth;
import io.apicurio.rest.client.error.RestClientErrorHandler;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ApicurioHttpClientFactory {

    private static final AtomicReference<ApicurioHttpClientProvider> providerReference = new AtomicReference<>();
    private static final ApicurioHttpClientServiceLoader serviceLoader = new ApicurioHttpClientServiceLoader();

    public static ApicurioHttpClient create(String basePath, RestClientErrorHandler errorHandler) {
        return create(basePath, Collections.emptyMap(), null, errorHandler);
    }

    public static ApicurioHttpClient create(String baseUrl, Map<String, Object> configs, Auth auth, RestClientErrorHandler errorHandler) {
        ApicurioHttpClientProvider p = providerReference.get();
        if (p == null) {
            providerReference.compareAndSet(null, resolveProviderInstance());
            p = providerReference.get();
        }
        return p.create(baseUrl, configs, auth, errorHandler);
    }

    public static boolean setProvider(ApicurioHttpClientProvider provider) {
        return providerReference.compareAndSet(null, provider);
    }

    private static ApicurioHttpClientProvider resolveProviderInstance() {
        return serviceLoader.providers(true)
                .next();
    }
}
