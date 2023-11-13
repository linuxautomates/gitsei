package io.levelops.integrations.sonarqube.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectBranchesTest {

    private static final String RESPONSE_FILE_NAME = "project-branch-response.json";

    @Test
    public void deSerialize() throws IOException {
        ProjectBranchResponse response = DefaultObjectMapper.get()
                .readValue(ProjectBranchesTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        ProjectBranchResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getBranches()).isNotNull();
    }
}