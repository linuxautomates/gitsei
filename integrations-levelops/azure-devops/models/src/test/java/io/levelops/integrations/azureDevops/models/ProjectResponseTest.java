package io.levelops.integrations.azureDevops.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectResponseTest {

    private static final String RESPONSE_FILE_NAME = "project-response.json";

    @Test
    public void deSerialize() throws IOException {
        ProjectResponse response = DefaultObjectMapper.get()
                .readValue(ProjectResponseTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        ProjectResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getProjects()).isNotNull();
    }
}
