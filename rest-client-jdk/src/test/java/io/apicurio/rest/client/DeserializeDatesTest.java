package io.apicurio.rest.client;

import static io.apicurio.rest.client.config.ApicurioClientConfig.APICURIO_REQUEST_HEADERS_PREFIX;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import io.apicurio.rest.client.request.Operation;
import io.apicurio.rest.client.request.Request;


public class DeserializeDatesTest {

    private static final WiremockDateTimeServer wiremockServer = new WiremockDateTimeServer();
    private static JdkHttpClient jdkHttpClient;

    @BeforeAll
    public static void init() throws JsonProcessingException {
        String mockServerUrl = wiremockServer.start();
        DeserializeDatesTest.jdkHttpClient = new JdkHttpClient(mockServerUrl, Map.of(APICURIO_REQUEST_HEADERS_PREFIX, "issue #3024"), null, null);
    }

    @Test
    public void testGet() {
        final List<DateValue> dates = jdkHttpClient.sendRequest(new Request.RequestBuilder<List<DateValue>>()
                .path("dates")
                .operation(Operation.GET)
                .responseType(new TypeReference<List<DateValue>>() {})
                .build());

        Assertions.assertNotNull(dates);
        Assertions.assertEquals(2, dates.size());
    }


    @AfterAll
    public static void stop() {
        wiremockServer.stop();
    }
}
