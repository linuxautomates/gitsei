package io.levelops.ingestion.clients;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.exceptions.InternalApiClientException;
import okhttp3.OkHttpClient;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class IngestionClientIntegrationTest {
    @Test
    public void testClient() throws InternalApiClientException {
        IngestionClient client = IngestionClient.builder()
                .client(new OkHttpClient().newBuilder().readTimeout(20, TimeUnit.SECONDS).build())
                .internalApiUri("http://localhost:8080")
                .objectMapper(DefaultObjectMapper.get())
                .build();
        client.getIngestionLogs("h2o", "15", DefaultListRequest.builder().page(1).pageSize(1).build(), Boolean.FALSE);
    }
}