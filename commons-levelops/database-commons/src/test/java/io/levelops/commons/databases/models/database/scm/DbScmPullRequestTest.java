package io.levelops.commons.databases.models.database.scm;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.converters.devops.AzureDevOpsPullRequestConverters;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.PullRequest;
import io.levelops.integrations.azureDevops.models.Repository;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerPullRequest;
import io.levelops.integrations.github.models.GithubPullRequest;
import io.levelops.integrations.gitlab.models.GitlabEvent;
import io.levelops.integrations.gitlab.models.GitlabMergeRequest;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReview;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.database.scm.DbScmPullRequest.SEPARATE_APPROVAL_AND_COMMENT;
import static org.assertj.core.api.Assertions.assertThat;

public class DbScmPullRequestTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String INTEGRATION_ID = "456";

    @Test
    public void testFromHelixSwarmReview() throws IOException {
        String content = ResourceUtils.getResourceAsString("helix/helix_swarm_review_single.json");
        HelixSwarmReview review = MAPPER.readValue(content, HelixSwarmReview.class);
        DbScmPullRequest actual = DbScmPullRequest.fromHelixSwarmReview(review, Set.of("JamCode"), INTEGRATION_ID);
        Assert.assertNotNull(actual);
        DbScmPullRequest expected = DbScmPullRequest.builder()
                .repoIds(List.of("JamCode"))
                .project("JamCode")
                .number("9")
                .integrationId(INTEGRATION_ID)
                .creator("super")
                .workitemIds(List.of())
                .issueKeys(List.of())
                .creatorInfo(DbScmUser.builder()
                        .integrationId(INTEGRATION_ID)
                        .displayName("super")
                        .originalDisplayName("super")
                        .cloudId("super")
                        .build())
                .title("submit after edit\n")
                .sourceBranch("unknown")
                .targetBranch("unknown")
                .state("needs_review")
                .merged(false)
                .assignees(List.of("super"))
                .labels(List.of())
                .commitShas(List.of("4"))
                .reviews(List.of(DbScmReview.builder()
                        .reviewerInfo(DbScmUser.builder().integrationId(INTEGRATION_ID).cloudId("super").displayName("super").originalDisplayName("super").build())
                        .reviewId("10")
                        .reviewer("super")
                        .state("REQUESTED")
                        .reviewedAt(1608801270l)
                        .build()
                ))
                .prUpdatedAt(1608801270l)
                .prCreatedAt(1608801270l)
                .build();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFromGithubPullRequest() throws IOException {
        String data = ResourceUtils.getResourceAsString("github/github_pr.json");
        GithubPullRequest githubPullRequest = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(GithubPullRequest.class));
        DbScmPullRequest dbScmPullRequest = DbScmPullRequest
                .fromGithubPullRequest(githubPullRequest, "repo_1", "1", null);
        Assert.assertEquals(List.of("repo_1"), dbScmPullRequest.getRepoIds());
        Assert.assertEquals("repo_1", dbScmPullRequest.getProject());
    }

    @Test
    public void testFromGithubPullRequestApprovalAndReviewTogether() throws IOException {
        String data = ResourceUtils.getResourceAsString("github/github_pr_approval_review_together.json");
        GithubPullRequest githubPullRequest = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(GithubPullRequest.class));
        DbScmPullRequest actual = DbScmPullRequest
                .fromGithubPullRequest(githubPullRequest, "levelops/commons-levelops", "1", null);
        DbScmPullRequest expected = MAPPER.readValue(ResourceUtils.getResourceAsString("github/scm_pr_approval_review_together_not_separate.json"), DbScmPullRequest.class);
        Assert.assertEquals(expected, actual);

        actual = DbScmPullRequest
                .fromGithubPullRequest(githubPullRequest, "levelops/commons-levelops", "1", SEPARATE_APPROVAL_AND_COMMENT, null);
        expected = MAPPER.readValue(ResourceUtils.getResourceAsString("github/scm_pr_approval_review_together_separate.json"), DbScmPullRequest.class);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFromGithubPullRequestApprovalAndPRComment() throws IOException {
        String data = ResourceUtils.getResourceAsString("github/github_pr_approval_no_review_and_pr_comment.json");
        GithubPullRequest githubPullRequest = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(GithubPullRequest.class));
        DbScmPullRequest actual = DbScmPullRequest
                .fromGithubPullRequest(githubPullRequest, "levelops/commons-levelops", "1", null);
        DbScmPullRequest expected = MAPPER.readValue(ResourceUtils.getResourceAsString("github/scm_pr_approval_no_review_and_pr_comment.json"), DbScmPullRequest.class);
        Assert.assertEquals(expected, actual);

        actual = DbScmPullRequest
                .fromGithubPullRequest(githubPullRequest, "levelops/commons-levelops", "1", SEPARATE_APPROVAL_AND_COMMENT, null);
        expected = MAPPER.readValue(ResourceUtils.getResourceAsString("github/scm_pr_approval_no_review_and_pr_comment.json"), DbScmPullRequest.class);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFromGithubPullRequestExtensive() throws IOException {
        String data = ResourceUtils.getResourceAsString("github/github_pr_extensive.json");
        GithubPullRequest githubPullRequest = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(GithubPullRequest.class));
        DbScmPullRequest actual = DbScmPullRequest
                .fromGithubPullRequest(githubPullRequest, "levelops/commons-levelops", "1", null);
        DbScmPullRequest expected = MAPPER.readValue(ResourceUtils.getResourceAsString("github/scm_pr_extensive_not_separate.json"), DbScmPullRequest.class);
        Assert.assertEquals(expected, actual);

        actual = DbScmPullRequest
                .fromGithubPullRequest(githubPullRequest, "levelops/commons-levelops", "1", SEPARATE_APPROVAL_AND_COMMENT, null);
        expected = MAPPER.readValue(ResourceUtils.getResourceAsString("github/scm_pr_extensive_separate.json"), DbScmPullRequest.class);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFromBitbucketServerPR() throws IOException {
        String data = ResourceUtils.getResourceAsString("bitbucket-server/bitbucket_server_pr.json");
        BitbucketServerPullRequest bitbucketServerPR = MAPPER.readValue(
                data, MAPPER.getTypeFactory().constructType(BitbucketServerPullRequest.class));
        DbScmPullRequest dbScmPullRequest = DbScmPullRequest
                .fromBitbucketServerPullRequest(bitbucketServerPR, "repo_1", "1");
        Assert.assertEquals(List.of("repo_1"), dbScmPullRequest.getRepoIds());
        Assert.assertEquals("cgn-test", dbScmPullRequest.getProject());
        Assert.assertEquals("3", dbScmPullRequest.getNumber());
        Assert.assertEquals("1", dbScmPullRequest.getIntegrationId());
        Assert.assertEquals("Srinath Chandrashekhar", dbScmPullRequest.getCreator());
        Assert.assertEquals(DbScmUser.builder()
                        .integrationId("1")
                        .cloudId("3")
                        .displayName("Srinath Chandrashekhar")
                        .originalDisplayName("Srinath Chandrashekhar")
                        .build(),
                dbScmPullRequest.getCreatorInfo());
        Assert.assertEquals("style.css edited online with Bitbucket", dbScmPullRequest.getTitle());
        Assert.assertEquals("test", dbScmPullRequest.getSourceBranch());
        Assert.assertEquals("OPEN", dbScmPullRequest.getState());
        Assert.assertTrue(dbScmPullRequest.getAssignees().containsAll(List.of("Srinath Chandrashekhar", "Chandrashekhar")));
        Assert.assertTrue(dbScmPullRequest.getCommitShas().containsAll(List.of(
                "2ad66cf2d9a12dfcbe8094df39cd01b6492ab1f8", "ba32baa44b601018f1ecb0b78c956827e97849aa")));
        Assert.assertFalse(dbScmPullRequest.getMerged());
        Assert.assertNull(dbScmPullRequest.getMergeSha());
        Assert.assertNull(dbScmPullRequest.getPrMergedAt());
        Assert.assertEquals(Long.valueOf("1622216383"), dbScmPullRequest.getPrUpdatedAt());
        Assert.assertNull(dbScmPullRequest.getPrClosedAt());
        Assert.assertEquals(Long.valueOf("1622216383"), dbScmPullRequest.getPrCreatedAt());
        Assert.assertEquals(List.of("REVIEWED", "UNAPPROVED", "APPROVED"), dbScmPullRequest.getReviews().stream()
                .map(DbScmReview::getState).collect(Collectors.toList()));
        Assert.assertEquals(List.of(DbScmUser.builder().cloudId("2").displayName("Srinath Chandrashekhar").originalDisplayName("Srinath Chandrashekhar").integrationId("1").build(),
                DbScmUser.builder().cloudId("2").displayName("Srinath Chandrashekhar").originalDisplayName("Srinath Chandrashekhar").integrationId("1").build(),
                DbScmUser.builder().cloudId("2").displayName("Srinath Chandrashekhar").originalDisplayName("Srinath Chandrashekhar").integrationId("1").build()),
                dbScmPullRequest.getReviews().stream().map(DbScmReview::getReviewerInfo).collect(Collectors.toList()));
    }

    @Test
    public void testFromAzurePullRequest() throws IOException {
        String projectData = ResourceUtils.getResourceAsString("azuredevops/azure_devops_project.json");
        String repositoryData = ResourceUtils.getResourceAsString("azuredevops/azure_devops_repository.json");
        String pullRequestData = ResourceUtils.getResourceAsString("azuredevops/azure_devops_pullrequests.json");
        String azurePrData = ResourceUtils.getResourceAsString("azuredevops/azure_pr.json");

        Project project = MAPPER.readValue(projectData, MAPPER.getTypeFactory().constructType(Project.class));
        Repository repository = MAPPER.readValue(repositoryData, MAPPER.getTypeFactory().constructType(Repository.class));
        PullRequest pullRequest = MAPPER.readValue(pullRequestData, MAPPER.getTypeFactory().constructType(PullRequest.class));

        DbScmPullRequest dbScmPullRequestActual = AzureDevOpsPullRequestConverters.fromAzureDevopsPullRequest(pullRequest, project, repository, "4197");
        DbScmPullRequest dbScmPullRequestExpected = MAPPER.readValue(azurePrData, MAPPER.getTypeFactory().constructType(DbScmPullRequest.class));

        Assert.assertEquals(dbScmPullRequestExpected, dbScmPullRequestActual);
    }

    @Test
    public void testFromAzurePullRequestForOpenAdoPRs() throws IOException {
        String projectData = ResourceUtils.getResourceAsString("azuredevops/azure_devops_project.json");
        String repositoryData = ResourceUtils.getResourceAsString("azuredevops/azure_devops_repository.json");
        String pullRequestData = ResourceUtils.getResourceAsString("azuredevops/azure_devops_pullrequests_open_pr.json");
        String azurePrData = ResourceUtils.getResourceAsString("azuredevops/azure_pr_open_pr.json");

        Project project = MAPPER.readValue(projectData, MAPPER.getTypeFactory().constructType(Project.class));
        Repository repository = MAPPER.readValue(repositoryData, MAPPER.getTypeFactory().constructType(Repository.class));
        PullRequest pullRequest = MAPPER.readValue(pullRequestData, MAPPER.getTypeFactory().constructType(PullRequest.class));

        DbScmPullRequest dbScmPullRequestActual = AzureDevOpsPullRequestConverters.fromAzureDevopsPullRequest(pullRequest, project, repository, "4197");
        DbScmPullRequest dbScmPullRequestExpected = MAPPER.readValue(azurePrData, MAPPER.getTypeFactory().constructType(DbScmPullRequest.class));

        Assert.assertEquals(dbScmPullRequestExpected, dbScmPullRequestActual);
    }

    @Test
    public void testHelixSwarmProject() throws IOException {
        String data = ResourceUtils.getResourceAsString("helix/helix_swarm_reviews.json");
        List<HelixSwarmReview> reviews = MAPPER.readValue(data,
                MAPPER.getTypeFactory().constructCollectionType(List.class, HelixSwarmReview.class));
        List<DbScmPullRequest> pullRequests = new ArrayList<>();
        reviews.forEach(helixSwarmReview -> {
            DbScmPullRequest dbScmPullRequest = DbScmPullRequest.fromHelixSwarmReview(helixSwarmReview,
                    Set.of("JamCode", "Dummy"), INTEGRATION_ID);
            pullRequests.add(dbScmPullRequest);
        });
        Assert.assertEquals(pullRequests.get(0).getProject(), "Dummy");
        Assert.assertEquals(pullRequests.get(1).getProject(), "Dummy");
        List<DbScmPullRequest> dbScmPullRequests = new ArrayList<>();
        reviews.forEach(helixSwarmReview -> {
            DbScmPullRequest dbScmPullRequest = DbScmPullRequest.fromHelixSwarmReview(helixSwarmReview,
                    Set.of("Dummy", "JamCode"), INTEGRATION_ID);
            dbScmPullRequests.add(dbScmPullRequest);
        });
        Assert.assertEquals(pullRequests.get(0).getProject(), "Dummy");
        Assert.assertEquals(pullRequests.get(1).getProject(), "Dummy");
    }

    @Test
    public void testGitlabState() {
        assertThat(DbScmPullRequest.parseGitlabPrState(GitlabEvent.builder().actionName("accepted").build())).isEqualTo("ACCEPTED");
        assertThat(DbScmPullRequest.parseGitlabPrState(GitlabEvent.builder().actionName("reviewed").build())).isEqualTo("REVIEWED");
        assertThat(DbScmPullRequest.parseGitlabPrState(GitlabEvent.builder().actionName("commented on").build())).isEqualTo("COMMENTED");
        assertThat(DbScmPullRequest.parseGitlabPrState(GitlabEvent.builder().actionName("").build())).isEqualTo("UNKNOWN");
    }

    @Test
    public void testFromGitlabMergeRequest() throws IOException {
        GitlabMergeRequest apiPr = ResourceUtils.getResourceAsObject("gitlab/gitlab_api_pr.json", GitlabMergeRequest.class);
        DbScmPullRequest out = DbScmPullRequest.fromGitlabMergeRequest(apiPr, "levelops/pipeline", "1",null);
        DefaultObjectMapper.prettyPrint(out);

        DbScmPullRequest expected = ResourceUtils.getResourceAsObject("gitlab/gitlab_expected_pr.json", DbScmPullRequest.class);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    public void testFromGitlabMergeRequest2() throws IOException {
        GitlabMergeRequest apiPr = ResourceUtils.getResourceAsObject("gitlab/gitlab_api_pr_2.json", GitlabMergeRequest.class);
        DbScmPullRequest out = DbScmPullRequest.fromGitlabMergeRequest(apiPr, "levelops/pipeline", "1",null);
        DefaultObjectMapper.prettyPrint(out);

        DbScmPullRequest expected = ResourceUtils.getResourceAsObject("gitlab/gitlab_expected_pr_2.json", DbScmPullRequest.class);
        out.getAssigneesInfo().get(0).toBuilder().originalDisplayName(null).build();
        assertThat(out).isEqualTo(expected);
    }

    @Test
    public void testFromGitlabMergeRequest3() throws IOException {
        GitlabMergeRequest apiPr = ResourceUtils.getResourceAsObject("gitlab/gitlab_api_pr_3.json", GitlabMergeRequest.class);
        DbScmPullRequest out = DbScmPullRequest.fromGitlabMergeRequest(apiPr, "levelops/pipeline", "1",null);
        DefaultObjectMapper.prettyPrint(out);

        DbScmPullRequest expected = ResourceUtils.getResourceAsObject("gitlab/gitlab_expected_pr_3.json", DbScmPullRequest.class);
        assertThat(out).isEqualTo(expected);
    }

    @Test
    public void testFromGithubPullRequestMetaPRLink() throws IOException {
        String data = ResourceUtils.getResourceAsString("github/github_pr_approval_no_review_and_pr_comment.json");
        GithubPullRequest githubPullRequest = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(GithubPullRequest.class));
        Integration integration = Integration.builder()
                .id("1")
                .name("Test-Integration")
                .url("https://test-satellite-api-url.com/")
                .satellite(true)
                .build();
        DbScmPullRequest actual = DbScmPullRequest
                .fromGithubPullRequest(githubPullRequest, "levelops/commons-levelops", "1", integration);

        Assert.assertNotNull(actual);
        Assertions.assertThat(actual.getMetadata().get("pr_link")).isEqualTo("https://test-satellite-api-url.com/levelops/commons-levelops/pull/3852");

        integration = Integration.builder()
                .id("1")
                .name("Test-Integration")
                .url("https://test-satellite-api-url.com")
                .satellite(true)
                .build();


        actual = DbScmPullRequest
                .fromGithubPullRequest(githubPullRequest, "levelops/commons-levelops", "1", SEPARATE_APPROVAL_AND_COMMENT, integration);

        Assert.assertNotNull(actual);
        Assertions.assertThat(actual.getMetadata().get("pr_link")).isEqualTo("https://test-satellite-api-url.com/levelops/commons-levelops/pull/3852");

        integration = Integration.builder()
                .id("1")
                .name("Test-Integration-1")
                .url("https://api-github.com/")
                .satellite(false)
                .build();

        actual = DbScmPullRequest
                .fromGithubPullRequest(githubPullRequest, "levelops/commons-levelops", "1", integration);

        Assert.assertNotNull(actual);
        Assertions.assertThat(actual.getMetadata().get("pr_link")).isEqualTo("https://github.com/levelops/commons-levelops/pull/3852");

    }

    @Test
    public void testFromGitlabMergeRequestMetaPRLink() throws IOException {
        GitlabMergeRequest apiPr = ResourceUtils.getResourceAsObject("gitlab/gitlab_api_pr_2.json", GitlabMergeRequest.class);
        Integration integration = Integration.builder()
                .id("1")
                .name("Test-Integration")
                .url("https://test-satellite-api-url.com/")
                .satellite(true)
                .build();
        DbScmPullRequest actual = DbScmPullRequest.fromGitlabMergeRequest(apiPr, "levelops/commons-levelops", "1",integration);

        Assert.assertNotNull(actual);
        Assertions.assertThat(actual.getMetadata().get("pr_link")).isEqualTo("https://test-satellite-api-url.com/levelops/commons-levelops/-/merge_requests/13");

        integration = Integration.builder()
                .id("1")
                .name("Test-Integration")
                .url("https://test-satellite-api-url.com")
                .satellite(true)
                .build();
        actual = DbScmPullRequest.fromGitlabMergeRequest(apiPr, "levelops/commons-levelops", "1",integration);

        Assert.assertNotNull(actual);
        Assertions.assertThat(actual.getMetadata().get("pr_link")).isEqualTo("https://test-satellite-api-url.com/levelops/commons-levelops/-/merge_requests/13");

        integration = Integration.builder()
                .id("1")
                .name("Test-Integration")
                .url("test-satellite-api-url.com")
                .satellite(true)
                .build();
        actual = DbScmPullRequest.fromGitlabMergeRequest(apiPr, "levelops/commons-levelops", "1",integration);

        Assert.assertNotNull(actual);
        Assertions.assertThat(actual.getMetadata().get("pr_link")).isEqualTo("https://test-satellite-api-url.com/levelops/commons-levelops/-/merge_requests/13");

        integration = Integration.builder()
                .id("1")
                .name("Test-Integration-1")
                .url("https://api-gitlab.com/")
                .satellite(false)
                .build();

        actual = DbScmPullRequest.fromGitlabMergeRequest(apiPr, "levelops/commons-levelops", "1",integration);

        Assert.assertNotNull(actual);
        Assertions.assertThat(actual.getMetadata().get("pr_link")).isEqualTo("https://gitlab.com/levelops/commons-levelops/-/merge_requests/13");
    }
}
