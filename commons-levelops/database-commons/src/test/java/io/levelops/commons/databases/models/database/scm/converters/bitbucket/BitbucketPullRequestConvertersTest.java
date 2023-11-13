package io.levelops.commons.databases.models.database.scm.converters.bitbucket;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.bitbucket.models.BitbucketPullRequest;
import io.levelops.integrations.bitbucket.models.BitbucketPullRequestActivity;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class BitbucketPullRequestConvertersTest {

    @Test
    public void testFromBitbucketPullRequest() throws IOException {
        BitbucketPullRequest pr = ResourceUtils.getResourceAsObject("bitbucket/bitbucket_pr.json", BitbucketPullRequest.class);
        DbScmPullRequest output = BitbucketPullRequestConverters
                .fromBitbucketPullRequest(pr, "repo_1", "project_1", "1", (pr.getMergeCommit() != null) ? pr.getMergeCommit().getHash() : null, null);
        DefaultObjectMapper.prettyPrint(output);

        DbScmPullRequest expected = ResourceUtils.getResourceAsObject("bitbucket/bitbucket_pr_expected.json", DbScmPullRequest.class);
        assertThat(output).isEqualTo(expected);
    }

    @Test
    public void testFromBitbucketPullRequestMetaPRLink() throws IOException {
        BitbucketPullRequest pr = ResourceUtils.getResourceAsObject("bitbucket/bitbucket_pr.json", BitbucketPullRequest.class);
        Integration integration = Integration.builder()
                .id("3")
                .name("Test-Integration")
                .url("https://bitbucket-test-satellite-api-url.com/")
                .satellite(true)
                .build();
        DbScmPullRequest actual = BitbucketPullRequestConverters.fromBitbucketPullRequest(pr, "repo_1", "project_1","1",(pr.getMergeCommit() != null) ? pr.getMergeCommit().getHash() : null,integration);
        Assert.assertNotNull(actual);
        assertThat(actual.getMetadata().get("pr_link")).isEqualTo("https://bitbucket-test-satellite-api-url.com/srinath_levelops/repository1/pull-requests/3");

        integration = Integration.builder()
                .id("3")
                .name("Test-Integration")
                .url("https://bitbucket-test-satellite-api-url.com")
                .satellite(true)
                .build();
        actual = BitbucketPullRequestConverters.fromBitbucketPullRequest(pr, "repo_1", "project_1","1",(pr.getMergeCommit() != null) ? pr.getMergeCommit().getHash() : null,integration);
        Assert.assertNotNull(actual);
        assertThat(actual.getMetadata().get("pr_link")).isEqualTo("https://bitbucket-test-satellite-api-url.com/srinath_levelops/repository1/pull-requests/3");

        integration = Integration.builder()
                .id("3")
                .name("Test-Integration-1")
                .url("https://api-bitbucket.org/")
                .satellite(false)
                .build();

        actual = BitbucketPullRequestConverters.fromBitbucketPullRequest(pr, "repo_1", "project_1","1",(pr.getMergeCommit() != null) ? pr.getMergeCommit().getHash() : null,integration);
        Assert.assertNotNull(actual);
        assertThat(actual.getMetadata().get("pr_link")).isEqualTo("https://bitbucket.org/srinath_levelops/repository1/pull-requests/3");

        integration = Integration.builder()
                .id("3")
                .name("Test-Integration-1")
                .url("bitbucket-test-satellite-api-url.com")
                .satellite(true)
                .build();

        actual = BitbucketPullRequestConverters.fromBitbucketPullRequest(pr, "repo_1", "project_1","1",(pr.getMergeCommit() != null) ? pr.getMergeCommit().getHash() : null,integration);
        Assert.assertNotNull(actual);
        assertThat(actual.getMetadata().get("pr_link")).isEqualTo("https://bitbucket-test-satellite-api-url.com/srinath_levelops/repository1/pull-requests/3");


    }

    @Test
    public void parseActivity() throws IOException {
        checkActivity("bitbucket/bitbucket_pr_activity_0.json", "bitbucket/bitbucket_pr_activity_0_expected.json");
        checkActivity("bitbucket/bitbucket_pr_activity_1.json", "bitbucket/bitbucket_pr_activity_1_expected.json");
        checkActivity("bitbucket/bitbucket_pr_activity_2.json", "bitbucket/bitbucket_pr_activity_2_expected.json");
        checkActivity("bitbucket/bitbucket_pr_activity_3.json", "bitbucket/bitbucket_pr_activity_3_expected.json");
        checkActivity("bitbucket/bitbucket_pr_activity_4.json", "bitbucket/bitbucket_pr_activity_4_expected.json");
        checkActivity("bitbucket/bitbucket_pr_activity_5.json", "bitbucket/bitbucket_pr_activity_5_expected.json");
        checkActivity("bitbucket/bitbucket_pr_activity_6.json", "bitbucket/bitbucket_pr_activity_6_expected.json");
    }

    private void checkActivity(String input, String expectedResource) throws IOException {
        System.out.println("input=" + input);

        BitbucketPullRequestActivity activity = ResourceUtils.getResourceAsObject(input, BitbucketPullRequestActivity.class);

        List<DbScmReview> output = BitbucketPullRequestConverters.parseActivity("1", activity);
        DefaultObjectMapper.prettyPrint(output);

        List<DbScmReview> expected = ResourceUtils.getResourceAsList(expectedResource, DbScmReview.class);
        assertThat(output).containsExactlyElementsOf(expected);
    }
}