/*
 * Copyright 2020 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.rest.client.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.apicurio.rest.client.VertxHttpClient;
import io.apicurio.rest.client.auth.exception.AuthErrorHandler;
import io.apicurio.rest.client.auth.request.KeycloakRequestsProvider;
import io.apicurio.rest.client.spi.ApicurioHttpClient;
import io.apicurio.rest.client.spi.ApicurioHttpClientProvider;
import io.apicurio.rest.client.spi.ApicurioHttpClientServiceLoader;
import io.vertx.core.Vertx;

import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * @author carnalca@redhat.com
 */
public class KeycloakAuth implements Auth {

    private static final String BEARER = "Bearer ";
    private static final String GRANT_TYPE = "client_credentials";

    private static final ApicurioHttpClientServiceLoader serviceLoader = new ApicurioHttpClientServiceLoader();

    private final String serverUrl;

    private final String realm;
    private final String clientId;
    private final String clientSecret;

    private String cachedAccessToken;
    private long cachedAccessTokenExpTime;

    private final ApicurioHttpClient apicurioHttpClient;

    public KeycloakAuth(String serverUrl, String realm, String clientId, String clientSecret) {
        this.serverUrl = serverUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.apicurioHttpClient = resolveApicurioHttpClient();
    }

    public KeycloakAuth(Vertx vertx, String serverUrl, String realm, String clientId, String clientSecret) {
        this.serverUrl = serverUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.apicurioHttpClient = new VertxHttpClient(vertx, serverUrl, Collections.emptyMap(), null, new AuthErrorHandler());
    }

    private static ApicurioHttpClientProvider resolveProviderInstance() {
        //We get first provider available
        return serviceLoader.providers(true)
                .next();
    }

    private ApicurioHttpClient resolveApicurioHttpClient() {
        return resolveProviderInstance().create(serverUrl, Collections.emptyMap(), null, new AuthErrorHandler());
    }

    /**
     * @see Auth#apply(java.util.Map)
     */
    @Override
    public void apply(Map<String, String> requestHeaders) {
        if (isAccessTokenRequired()) {
            this.cachedAccessToken = obtainAccessToken().getToken();
        }
        requestHeaders.put("Authorization", BEARER + cachedAccessToken);
    }

    private AccessTokenResponse obtainAccessToken() {
        try {
            final Map<String, String> params = Map.of("grant_type", GRANT_TYPE, "client_id", clientId, "client_secret", clientSecret);
            final String paramsEncoded = params.entrySet().stream().map(entry -> String.join("=",
                    URLEncoder.encode(entry.getKey(), UTF_8),
                    URLEncoder.encode(entry.getValue(), UTF_8))
            ).collect(Collectors.joining("&"));

            return apicurioHttpClient.sendRequest(KeycloakRequestsProvider.obtainAccessToken(paramsEncoded, realm));

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error found while trying to request a new token to keycloak");
        }
    }

    private boolean isAccessTokenRequired() {
        return null == cachedAccessToken || isTokenExpired();
    }

    private boolean isTokenExpired() {
        return true;
        //return (cachedAccessTokenExpTime != 0L) && (long) Time.currentTime() > accessTokenParsed.getExp();
    }

    public static class Builder {
        private String serverUrl;
        private String realm;
        private String clientId;
        private String clientSecret;

        public Builder() {
        }

        public Builder withRealm(String realm) {
            this.realm = realm;
            return this;
        }

        public Builder withClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder withClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        public Builder withServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        public KeycloakAuth build() {
            return new KeycloakAuth(this.serverUrl, this.realm, this.clientId, this.clientSecret);
        }
    }
}