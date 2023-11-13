package io.levelops.integrations.gerrit.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class ProjectInfoTest {
    private static final String RESPONSE_FILE_NAME = "projects.json";
    private static final int EXPECTED_NUM_PROJECTS = 3;

    @Test
    public void deSerialize() throws IOException {
        Map<String, ProjectInfo> response = DefaultObjectMapper.get()
                .readValue(DateFormatter.formatDates(new String(ProjectInfoTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME).readAllBytes(), UTF_8)),
                        DefaultObjectMapper.get().getTypeFactory().constructParametricType(HashMap.class, String.class, ProjectInfo.class));
        assertThat(response).isNotNull();
        assertThat(response).hasSize(EXPECTED_NUM_PROJECTS);
    }
}
