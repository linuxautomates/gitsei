package io.levelops.integrations.testrails.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRailsProjectTest {

    private static final String RESPONSE_FILE_NAME = "projects.json";

    @Test
    public void deSerialize() throws IOException {
        Project response = DefaultObjectMapper.get()
                .readValue(Project.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        Project.class);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }
}
