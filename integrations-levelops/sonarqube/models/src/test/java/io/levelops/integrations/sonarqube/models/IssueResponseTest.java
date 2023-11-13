package io.levelops.integrations.sonarqube.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueResponseTest {

    private static final String RESPONSE_FILE_NAME = "issue-response.json";

    @Test
    public void deSerialize() throws IOException {
        IssueResponse response = DefaultObjectMapper.get()
                .readValue(IssueResponseTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        IssueResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getIssues()).isNotNull();
    }
}
