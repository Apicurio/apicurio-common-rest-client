package io.apicurio.rest.client.config;

public class ClientConfig {

    public static final String REGISTRY_REQUEST_HEADERS_PREFIX = "apicurio.registry.request.headers.";
    public static final String REGISTRY_REQUEST_TRUSTSTORE_PREFIX = "apicurio.registry.request.ssl.truststore";
    public static final String REGISTRY_REQUEST_TRUSTSTORE_LOCATION = REGISTRY_REQUEST_TRUSTSTORE_PREFIX + ".location";
    public static final String REGISTRY_REQUEST_TRUSTSTORE_TYPE = REGISTRY_REQUEST_TRUSTSTORE_PREFIX + ".type";
    public static final String REGISTRY_REQUEST_TRUSTSTORE_PASSWORD = REGISTRY_REQUEST_TRUSTSTORE_PREFIX + ".password";
    public static final String REGISTRY_REQUEST_KEYSTORE_PREFIX = "apicurio.registry.request.ssl.keystore";
    public static final String REGISTRY_REQUEST_KEYSTORE_LOCATION = REGISTRY_REQUEST_KEYSTORE_PREFIX + ".location";
    public static final String REGISTRY_REQUEST_KEYSTORE_TYPE = REGISTRY_REQUEST_KEYSTORE_PREFIX + ".type";
    public static final String REGISTRY_REQUEST_KEYSTORE_PASSWORD = REGISTRY_REQUEST_KEYSTORE_PREFIX + ".password";
    public static final String REGISTRY_REQUEST_KEY_PASSWORD = "apicurio.registry.request.ssl.key.password";
    public static final String REGISTRY_CLIENT_DISABLE_AUTO_BASE_PATH_APPEND = "apicurio.registry.client.disable-auto-basepath-append";
}
