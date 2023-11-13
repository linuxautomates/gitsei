package io.levelops.commons.databases.models.database.scm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.github.models.GithubPullRequest;
import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class DbScmPRLabelTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialize() throws JsonProcessingException {
        DbScmPRLabel expected = DbScmPRLabel.builder()
                .id(UUID.randomUUID())
                .scmPullRequestId(UUID.randomUUID())
                .cloudId("3169210240").name("lbl_1").description("Label 1")
                .build();
        String serialized = MAPPER.writeValueAsString(expected);
        DbScmPRLabel actual = MAPPER.readValue(serialized, DbScmPRLabel.class);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFromGithubPullRequest() {
        DbScmPRLabel expected = DbScmPRLabel.builder()
                .cloudId("3169210240").name("lbl_1").description("Label 1").build();
        DbScmPRLabel actual = DbScmPRLabel.fromGithubPullRequest(GithubPullRequest.Label.builder().id("3169210240").name("lbl_1").description("Label 1").build());
        Assert.assertEquals(expected, actual);
    }
}