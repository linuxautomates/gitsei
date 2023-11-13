package io.levelops.integrations.azureDevops.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class RunResponseTest {

    private static final String RESPONSE_FILE_NAME = "run-response.json";

    @Test
    public void deSerialize() throws IOException {
        RunResponse response = DefaultObjectMapper.get()
                .readValue(RunResponseTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        RunResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getRuns()).isNotNull();
        assertThat(response.getRuns().stream().noneMatch(run -> run.getVariables().isEmpty())).isTrue();
    }
}
