package io.levelops.integrations.azureDevops.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class PipelineResponseTest {
    private static final String RESPONSE_FILE_NAME = "pipeline-response.json";

    @Test
    public void deSerialize() throws IOException {
        PipelineResponse response = DefaultObjectMapper.get()
                .readValue(PipelineResponseTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        PipelineResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getPipelines()).isNotNull();
    }
}
