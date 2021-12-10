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
import io.apicurio.rest.client.auth.request.TokenRequestsProvider;
import io.apicurio.rest.client.spi.ApicurioHttpClient;

import java.net.URLEncoder;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * @author carnalca@redhat.com
 */
public class OidcAuth implements Auth, AutoCloseable {

    private static final String BEARER = "Bearer ";
    private static final String CLIENT_CREDENTIALS_GRANT = "client_credentials";
    private static final String PASSWORD_GRANT = "password";

    private final String clientId;
    private final String clientSecret;

    private String cachedAccessToken;
    private long cachedAccessTokenExp;

    private final ApicurioHttpClient apicurioHttpClient;

    public OidcAuth(ApicurioHttpClient httpClient, String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.apicurioHttpClient = httpClient;
    }

    /**
     * @see Auth#apply(java.util.Map)
     */
    @Override
    public void apply(Map<String, String> requestHeaders) {
        if (isAccessTokenRequired()) {
            requestAccessToken();
        }
        requestHeaders.put("Authorization", BEARER + cachedAccessToken);
    }

    private void requestAccessToken() {
        try {
            final Map<String, String> params = Map.of("grant_type", CLIENT_CREDENTIALS_GRANT, "client_id", clientId, "client_secret", clientSecret);
            final String paramsEncoded = params.entrySet().stream().map(entry -> String.join("=",
                    URLEncoder.encode(entry.getKey(), UTF_8),
                    URLEncoder.encode(entry.getValue(), UTF_8))
            ).collect(Collectors.joining("&"));
            final AccessTokenResponse accessTokenResponse = apicurioHttpClient.sendRequest(TokenRequestsProvider.obtainAccessToken(paramsEncoded));
            this.cachedAccessToken = accessTokenResponse.getToken();
            this.cachedAccessTokenExp = System.currentTimeMillis() / 1000 + accessTokenResponse.getExpiresIn();

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error found while trying to request a new token");
        }
    }

    public String authenticate() {
        if (isAccessTokenRequired()) {
            requestAccessToken();
        }
        return cachedAccessToken;
    }

    public String obtainAccessTokenPaswordGrant(String username, String password) {
        try {
            final Map<String, String> params = Map.of("grant_type", PASSWORD_GRANT, "client_id", clientId, "client_secret", clientSecret, "username", username, "password", password);
            final String paramsEncoded = params.entrySet().stream().map(entry -> String.join("=",
                    URLEncoder.encode(entry.getKey(), UTF_8),
                    URLEncoder.encode(entry.getValue(), UTF_8))
            ).collect(Collectors.joining("&"));

            return apicurioHttpClient.sendRequest(TokenRequestsProvider.obtainAccessToken(paramsEncoded)).getToken();

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error found while trying to request a new token");
        }
    }

    private boolean isAccessTokenRequired() {
        return null == cachedAccessToken || isTokenExpired();
    }

    private boolean isTokenExpired() {
        return (int) (System.currentTimeMillis() / 1000L) > cachedAccessTokenExp;
    }

    public void close() {
        this.apicurioHttpClient.close();
    }
}