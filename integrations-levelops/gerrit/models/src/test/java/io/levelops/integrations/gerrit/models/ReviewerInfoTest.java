package io.levelops.integrations.gerrit.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewerInfoTest {

    private static final String RESPONSE_FILE_NAME = "reviewers.json";
    private static final int EXPECTED_NUM_REVIEWERS = 1;

    @Test
    public void deSerialize() throws IOException {
        List<ReviewerInfo> response = DefaultObjectMapper.get()
                .readValue(ReviewerInfoTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        DefaultObjectMapper.get().getTypeFactory().constructParametricType(List.class, ReviewerInfo.class));
        assertThat(response).isNotNull();
        assertThat(response).hasSize(EXPECTED_NUM_REVIEWERS);
    }
}
