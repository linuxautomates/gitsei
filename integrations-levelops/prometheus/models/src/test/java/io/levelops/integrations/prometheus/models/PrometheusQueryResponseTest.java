package io.levelops.integrations.prometheus.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class PrometheusQueryResponseTest {
    private static final String RESPONSE_FILE_NAME = "prometheus/query.json";

    @Test
    public void deSerialize() throws IOException {
        PrometheusQueryResponse response = DefaultObjectMapper.get()
                .readValue(PrometheusQueryResponse.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        PrometheusQueryResponse.class);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isNotNull();
    }
}
