package io.apicurio.rest.client;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;

public class WiremockDateTimeServer {

    private WireMockServer wireMockServer;

    public String start() throws JsonProcessingException {
        wireMockServer = new WireMockServer();
        wireMockServer.start();

        stubFor(get(urlEqualTo("/dates"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                " [ " +
                                "   { \"value\": \"2022-01-17T07:30:06+0000\" }," +
                                "   { \"value\": \"2022-01-17T07:30:06Z\" }" +
                                "]"
                        )));

        return wireMockServer.baseUrl();
    }

    public void stop() {
        if (null != wireMockServer) {
            wireMockServer.stop();
        }
    }
}
