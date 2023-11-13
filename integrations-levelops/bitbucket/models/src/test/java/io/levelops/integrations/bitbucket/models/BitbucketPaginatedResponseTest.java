package io.levelops.integrations.bitbucket.models;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BitbucketPaginatedResponseTest {

    @Test
    public void testExtract() {
        BitbucketPaginatedResponse<Object> o = BitbucketPaginatedResponse.builder()
                .next("https://api.bitbucket.org/2.0/repositories/levelopsteama/team-a-project-1-repo-1/pullrequests/4/commits?page=dbtR")
                .build();

        assertThat(o.extractNextPage().orElse(null)).isEqualTo("dbtR");
    }

    @Test
    public void testExtractEmpty() {
        BitbucketPaginatedResponse<Object> o = BitbucketPaginatedResponse.builder()
                .build();

        assertThat(o.extractNextPage().isEmpty()).isTrue();
    }

    @Test
    public void testExtractEmpty2() {
        BitbucketPaginatedResponse<Object> o = BitbucketPaginatedResponse.builder()
                .next("https://api.bitbucket.org/2.0/repositories/levelopsteama/team-a-project-1-repo-1/pullrequests/4/commits?test=blah")
                .build();

        assertThat(o.extractNextPage().isEmpty()).isTrue();
    }
}