package io.levelops.integrations.gerrit.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchInfoTest {
    private static final String RESPONSE_FILE_NAME = "branches.json";
    private static final int EXPECTED_NUM_BRANCHES = 7;

    @Test
    public void deSerialize() throws IOException {
        List<ProjectInfo.BranchInfo> response = DefaultObjectMapper.get()
                .readValue(BranchInfoTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        DefaultObjectMapper.get().getTypeFactory().constructParametricType(List.class, ProjectInfo.BranchInfo.class));
        assertThat(response).isNotNull();
        assertThat(response).hasSize(EXPECTED_NUM_BRANCHES);
    }
}
