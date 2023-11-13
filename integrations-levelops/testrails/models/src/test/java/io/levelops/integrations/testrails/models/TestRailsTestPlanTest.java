package io.levelops.integrations.testrails.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRailsTestPlanTest {

    private static final String RESPONSE_FILE_NAME = "test-plans.json";

    @Test
    public void deSerialize() throws IOException {
        TestPlan response = DefaultObjectMapper.get()
                .readValue(TestPlan.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        TestPlan.class);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }
}
