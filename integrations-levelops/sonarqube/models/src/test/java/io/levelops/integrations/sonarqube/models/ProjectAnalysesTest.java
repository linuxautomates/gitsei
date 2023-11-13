package io.levelops.integrations.sonarqube.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectAnalysesTest {

    private static final String RESPONSE_FILE_NAME = "project-analyses-response.json";

    @Test
    public void deSerialize() throws IOException {
        ProjectAnalysesResponse response = DefaultObjectMapper.get()
                .readValue(ProjectAnalysesTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        ProjectAnalysesResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getAnalyses()).isNotNull();
    }
}
