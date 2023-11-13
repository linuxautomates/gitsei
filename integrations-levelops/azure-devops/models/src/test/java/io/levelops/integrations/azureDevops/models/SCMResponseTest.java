package io.levelops.integrations.azureDevops.models;

import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class SCMResponseTest {

    private static final String REPOSITORY_RESPONSE_FILE_NAME = "repository-response.json";
    private static final String COMMIT_RESPONSE_FILE_NAME = "commit-response.json";
    private static final String COMMITCHANGE_RESPONSE_FILE_NAME = "commitchange-response.json";
    private static final String PULLREQUEST_RESPONSE_FILE_NAME = "pullrequest-response.json";
    private static final String TAG_RESPONSE_FILE_NAME = "tag-response.json";

    @Test
    public void deSerializeRepositoryResponse() throws IOException {
        RepositoryResponse response = DefaultObjectMapper.get()
                .readValue(SCMResponseTest.class.getClassLoader()
                                .getResourceAsStream(REPOSITORY_RESPONSE_FILE_NAME),
                        RepositoryResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getRepositories()).isNotNull();
        assertThat(response.getCount()).isEqualTo(4);
    }

    @Test
    public void deSerializeCommitResponse() throws IOException {
        CommitResponse response = DefaultObjectMapper.get()
                .readValue(SCMResponseTest.class.getClassLoader()
                                .getResourceAsStream(COMMIT_RESPONSE_FILE_NAME),
                        CommitResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getCommits()).isNotNull();
        assertThat(response.getCount()).isEqualTo(10);
    }

    @Test
    public void deSerializeCommitChangesResponse() throws IOException {
        CommitChangesResponse response = DefaultObjectMapper.get()
                .readValue(SCMResponseTest.class.getClassLoader()
                                .getResourceAsStream(COMMITCHANGE_RESPONSE_FILE_NAME),
                        CommitChangesResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getChanges()).isNotNull();
    }

    @Test
    public void deSerializePullRequestResponse() throws IOException {
        PullRequestResponse response = DefaultObjectMapper.get()
                .readValue(SCMResponseTest.class.getClassLoader()
                                .getResourceAsStream(PULLREQUEST_RESPONSE_FILE_NAME),
                        PullRequestResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getPullRequests()).isNotNull();
        assertThat(response.getCount()).isEqualTo(2);
    }

    @Test
    public void deSerializeTagResponse() throws IOException {
        TagResponse response = DefaultObjectMapper.get()
                .readValue(SCMResponseTest.class.getClassLoader()
                                .getResourceAsStream(TAG_RESPONSE_FILE_NAME),
                        TagResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.getTags()).isNotNull();
        assertThat(response.getCount()).isEqualTo(3);
    }
}
