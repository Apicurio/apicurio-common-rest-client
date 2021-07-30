package io.apicurio.rest.client;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

public class WiremockCountriesServer {

    private WireMockServer wireMockServer;

    public String start() throws JsonProcessingException {
        wireMockServer = new WireMockServer();
        wireMockServer.start();

        stubFor(post(urlEqualTo("/countries"))
                .withRequestBody(equalToJson(new ObjectMapper().writeValueAsString(new Country("Greece","Athens")))
                )
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{ \"name\": \"Greece\", \"capital\": \"Athens\" }"
                        )));

        stubFor(get(urlEqualTo("/countries/FR"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{ \"name\": \"France\", \"capital\": \"Paris\" }"
                        )));

        stubFor(get(urlEqualTo("/countries"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                " [ {\"name\": \"France\", \"capital\": \"Paris\"} ," +
                                        "{\"name\": \"Italy\", \"capital\": \"Rome\"} ," +
                                        "{\"name\": \"Spain\", \"capital\": \"Madrid\"} ," +
                                        "{\"name\": \"United Kingdom\", \"capital\": \"London\"} ," +
                                        "{\"name\": \"United States\", \"capital\": \"Washington\"} ," +
                                        "{\"name\": \"Germany\", \"capital\": \"Berlin\"}" +
                                        "]"
                        )));

        stubFor(get(urlEqualTo("/countries"))
                .withHeader("X-Reduced", containing("true"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                " [ {\"name\": \"France\", \"capital\": \"Paris\"} ," +
                                        "{\"name\": \"Italy\", \"capital\": \"Rome\"} ," +
                                        "{\"name\": \"Germany\", \"capital\": \"Berlin\"}" +
                                        "]"
                        )));

        stubFor(get(urlMatching(".*")).atPriority(10)
                .willReturn(aResponse().proxiedFrom("https://restcountries.eu/rest")));

        return wireMockServer.baseUrl();
    }

    public void stop() {
        if (null != wireMockServer) {
            wireMockServer.stop();
        }
    }
}
