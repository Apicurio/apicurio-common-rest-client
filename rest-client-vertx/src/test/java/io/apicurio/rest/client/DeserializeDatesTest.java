package io.apicurio.rest.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.apicurio.rest.client.request.Operation;
import io.apicurio.rest.client.request.Request;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;


public class DeserializeDatesTest {

    private static final WiremockDateTimeServer wiremockServer = new WiremockDateTimeServer();
    private static VertxHttpClient vertxHttpClient;

    @BeforeAll
    public static void init() throws JsonProcessingException {
        String mockServerUrl = wiremockServer.start();
        DeserializeDatesTest.vertxHttpClient = new VertxHttpClient(Vertx.vertx(), mockServerUrl, Collections.emptyMap(), null, null);
    }

    @Test
    public void testGet() {
        final List<DateValue> dates = vertxHttpClient.sendRequest(new Request.RequestBuilder<List<DateValue>>()
                .path("dates")
                .operation(Operation.GET)
                .responseType(new TypeReference<List<DateValue>>() {
                })
                .build());

        Assertions.assertNotNull(dates);
        Assertions.assertEquals(2, dates.size());
    }


    @AfterAll
    public static void stop() {
        wiremockServer.stop();
    }
}
