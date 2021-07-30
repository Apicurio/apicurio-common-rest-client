package io.apicurio.rest.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.apicurio.rest.client.request.Operation;
import io.apicurio.rest.client.request.Request;
import io.apicurio.rest.client.util.IoUtil;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class VertxClientTest {

    private static final WiremockCountriesServer wiremockCountriesServer = new WiremockCountriesServer();
    private static VertxHttpClient vertxHttpClient;

    @BeforeAll
    public static void init() throws JsonProcessingException {
        String mockServerUrl = wiremockCountriesServer.start();
        vertxHttpClient = new VertxHttpClient(Vertx.vertx(), mockServerUrl, Collections.emptyMap(), null, null);
    }

    @Test
    public void testPost() throws JsonProcessingException {

        final Country created = vertxHttpClient.sendRequest(new Request.RequestBuilder<Country>()
                .path("countries")
                .operation(Operation.POST)
                .data(IoUtil.toStream(new ObjectMapper().writeValueAsString(new Country("Greece", "Athens"))))
                .responseType(new TypeReference<Country>() {
                })
                .build());

        Assertions.assertEquals(created.getName(), "Greece");
        Assertions.assertEquals(created.getCapital(), "Athens");
    }

    @Test
    public void testGet() {
        final Country france = vertxHttpClient.sendRequest(new Request.RequestBuilder<Country>()
                .path("countries/FR")
                .operation(Operation.GET)
                .responseType(new TypeReference<Country>() {
                })
                .build());

        Assertions.assertEquals(france.getCapital(), "Paris");
        Assertions.assertEquals(france.getName(), "France");
    }

    @Test
    public void testGetList() {

        final List<Country> countries = vertxHttpClient.sendRequest(new Request.RequestBuilder<List<Country>>()
                .path("countries")
                .operation(Operation.GET)
                .responseType(new TypeReference<List<Country>>() {
                })
                .build());

        Assertions.assertEquals(countries.size(), 6);
    }

    @Test
    public void testSetRequestHeaders() {
        vertxHttpClient.setNextRequestHeaders(Map.of("X-Reduced", "true"));

        final List<Country> reducedCountries = vertxHttpClient.sendRequest(new Request.RequestBuilder<List<Country>>()
                .path("countries")
                .operation(Operation.GET)
                .responseType(new TypeReference<List<Country>>() {
                })
                .build());

        Assertions.assertEquals(reducedCountries.size(), 3);
    }


    @AfterAll
    public static void stop() {
        wiremockCountriesServer.stop();
    }
}
