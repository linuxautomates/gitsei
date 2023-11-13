package io.levelops.integrations.azureDevops.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class IterationResponseTest {

    private static final String RESPONSE_FILE_NAME = "iteration-response.json";

    @Test
    public void deSerialize() throws IOException {
        IterationResponse response = DefaultObjectMapper.get()
                .readValue(IterationResponseTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        IterationResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getIterations()).isNotNull();
    }

}
