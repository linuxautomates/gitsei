package io.levelops.integrations.prometheus.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class PrometheusQueryRequestTest {

    private static final String RESPONSE_FILE_NAME = "prometheus/query-request.json";

    @Test
    public void deSerialize() throws IOException {
        PrometheusQueryRequest request = DefaultObjectMapper.get()
                .readValue(PrometheusQueryResponse.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        PrometheusQueryRequest.class);
        DefaultObjectMapper.prettyPrint(request);
        assertThat(request).isNotNull();
        assertThat(request.getQueryString()).isNotNull();
    }
}
