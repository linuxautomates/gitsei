package io.levelops.integrations.gerrit.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class LabelInfoTest {

    private static final String RESPONSE_FILE_NAME = "labels.json";
    private static final int EXPECTED_NUM_LABELS = 1;

    @Test
    public void deSerialize() throws IOException {
        List<ProjectInfo.LabelDefinitionInfo> response = DefaultObjectMapper.get()
                .readValue(LabelInfoTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        DefaultObjectMapper.get().getTypeFactory().constructParametricType(List.class, ProjectInfo.LabelDefinitionInfo.class));
        assertThat(response).isNotNull();
        assertThat(response).hasSize(EXPECTED_NUM_LABELS);
    }
}
