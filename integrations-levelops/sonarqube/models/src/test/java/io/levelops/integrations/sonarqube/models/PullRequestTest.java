package io.levelops.integrations.sonarqube.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class PullRequestTest {

    private static final String RESPONSE_FILE_NAME = "pull-request-response.json";

    @Test
    public void deSerialize() throws IOException {
        PullRequestResponse response = DefaultObjectMapper.get()
                .readValue(PullRequestTest.class.getClassLoader().getResourceAsStream(RESPONSE_FILE_NAME),
                        PullRequestResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getPullRequests()).isNotNull();
    }
}