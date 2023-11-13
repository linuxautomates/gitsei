package io.levelops.integrations.testrails.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRailsTestRunTest {

    private static final String RESPONSE_FILE_NAME = "test-runs.json";

    @Test
    public void deSerialize() throws IOException {
        TestRun response = DefaultObjectMapper.get()
                .readValue(TestRun.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        TestRun.class);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
    }
}
