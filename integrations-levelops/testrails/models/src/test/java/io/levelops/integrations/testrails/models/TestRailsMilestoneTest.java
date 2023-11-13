package io.levelops.integrations.testrails.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRailsMilestoneTest {

    private static final String RESPONSE_FILE_NAME = "milestones.json";

    @Test
    public void deSerialize() throws IOException {
        Milestone response = DefaultObjectMapper.get()
                .readValue(Milestone.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        Milestone.class);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }
}
