package io.apicurio.rest.client.config;

public class ApicurioClientConfig {

    public static final String APICURIO_REQUEST_HEADERS_PREFIX = "apicurio.rest.request.headers.";
    public static final String APICURIO_REQUEST_TRUSTSTORE_PREFIX = "apicurio.rest.request.ssl.truststore";
    public static final String APICURIO_REQUEST_TRUSTSTORE_LOCATION = APICURIO_REQUEST_TRUSTSTORE_PREFIX + ".location";
    public static final String APICURIO_REQUEST_TRUSTSTORE_TYPE = APICURIO_REQUEST_TRUSTSTORE_PREFIX + ".type";
    public static final String APICURIO_REQUEST_TRUSTSTORE_PASSWORD = APICURIO_REQUEST_TRUSTSTORE_PREFIX + ".password";
    public static final String APICURIO_REQUEST_KEYSTORE_PREFIX = "apicurio.rest.request.ssl.keystore";
    public static final String APICURIO_REQUEST_KEYSTORE_LOCATION = APICURIO_REQUEST_KEYSTORE_PREFIX + ".location";
    public static final String APICURIO_REQUEST_KEYSTORE_TYPE = APICURIO_REQUEST_KEYSTORE_PREFIX + ".type";
    public static final String APICURIO_REQUEST_KEYSTORE_PASSWORD = APICURIO_REQUEST_KEYSTORE_PREFIX + ".password";
    public static final String APICURIO_REQUEST_KEY_PASSWORD = "apicurio.rest.request.ssl.key.password";
    public static final String APICURIO_CLIENT_DISABLE_AUTO_BASE_PATH_APPEND = "apicurio.rest.client.disable-auto-basepath-append";
    public static final String APICURIO_CLIENT_AUTO_BASE_PATH = "apicurio.rest.client.auto-base-path";

}
