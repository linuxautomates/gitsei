package io.levelops.integrations.azureDevops.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class BuildResponseTest {
    private static final String RESPONSE_FILE_NAME = "build-response.json";

    @Test
    public void deSerialize() throws IOException {
         BuildResponse response = DefaultObjectMapper.get()
                .readValue(BuildResponseTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        BuildResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getBuilds()).isNotNull();
    }
}
