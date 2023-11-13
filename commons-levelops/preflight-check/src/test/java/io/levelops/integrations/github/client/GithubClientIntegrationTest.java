package io.levelops.integrations.github.client;

import com.amazonaws.util.IOUtils;
import io.levelops.commons.client.graphql.GraphQlResponse;
import io.levelops.commons.client.oauth.OauthTokenInterceptor;
import io.levelops.commons.client.oauth.StaticOauthTokenProvider;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.github.model.GithubApiCommit;
import io.levelops.integrations.github.model.GithubApiRepoEvent;
import io.levelops.integrations.github.model.GithubConverters;
import io.levelops.integrations.github.model.GithubPaginatedResponse;
import io.levelops.integrations.github.models.GithubIssueEvent;
import io.levelops.integrations.github.models.GithubOrganization;
import io.levelops.integrations.github.models.GithubPullRequest;
import io.levelops.integrations.github.models.GithubPullRequestSearchResult;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.github.models.GithubWebhookConfig;
import io.levelops.integrations.github.models.GithubWebhookRequest;
import io.levelops.integrations.github.models.GithubWebhookResponse;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GithubClientIntegrationTest {

    private static final String GITHUB_ORGANIZATION = System.getenv("GITHUB_ORG");

    private GithubClient client;

    @Before
    public void setUp() throws Exception {
        String githubAppId = "308821";
        var pemFilePath = System.getenv("GITHUB_PEM_FILE_PATH");
        String pemPrivateKey;
        String jwtToken = "";
        if (pemFilePath != null) {
            pemPrivateKey = IOUtils.toString(new FileInputStream(pemFilePath));
            jwtToken = GithubAppTokenService.generateGithubAppJwtToken(pemPrivateKey, githubAppId, Instant.now());
        }

        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .addInterceptor(new OauthTokenInterceptor((StaticOauthTokenProvider.builder()
                        .token(System.getenv("GITHUB_API_KEY"))
                        .build())))
                .build();
        OkHttpClient jwtOkHttpClient = new OkHttpClient().newBuilder()
                .addInterceptor(new OauthTokenInterceptor((StaticOauthTokenProvider.builder()
                        .token(jwtToken)
                        .build())))
                .build();
        client = new GithubClient(okHttpClient, jwtOkHttpClient, DefaultObjectMapper.get(), null, 0, true);
    }

    // This test is failing with 404, since given repoId might not exists for the provided github account.
    //@Test
    public void test() throws GithubClientException {
        String repoId = "levelops/commons-levelops";

        DefaultObjectMapper.prettyPrint(client.getIssues(repoId, Map.of("direction", "desc"), 0));
        if (true) {
            return;
        }

        Stream<GithubPullRequest> pullRequests = client.streamPullRequests(repoId);

        long count = pullRequests.peek(pr -> {
            System.out.println("* * * * * * * * *");
            System.out.println("#" + pr.getNumber());
            System.out.println(pr.getTitle());
            client.streamReviews(repoId, pr.getNumber())
                    .forEach(DefaultObjectMapper::prettyPrint);
            client.streamPullRequestCommits(repoId, pr.getNumber())
                    .map(GithubConverters::parseGithubApiCommit)
                    .forEach(DefaultObjectMapper::prettyPrint);
        }).limit(1).count();
        System.out.println("Count: " + count);

        // ISSUE EVENTS

        List<GithubIssueEvent> o = client.streamIssueTimelineEvents(repoId, 757)
                .map(GithubConverters::parseGithubIssueTimelineEvent)
                .limit(20)
                .collect(Collectors.toList());

        DefaultObjectMapper.prettyPrint(o);
    }

    @Test
    public void testListWebhook() {
        Stream<GithubWebhookResponse> webhookResponse = client.streamWebhooks(GITHUB_ORGANIZATION, IntegrationKey.builder().build());
        List<GithubWebhookResponse> webhookResponseList = webhookResponse.collect(Collectors.toList());
        Assert.assertNotNull("This cannot be null", webhookResponse);
        Assert.assertNotNull(webhookResponseList.get(0));
        Assert.assertEquals("web", webhookResponseList.get(0).getName());
    }

    @Test
    public void testCreateAndUpdateWebhook() throws GithubClientException {
        GithubWebhookRequest webhookRequest = GithubWebhookRequest.builder()
                .name("web")
                .events(List.of("project_card",
                        "project_column"))
                .config(GithubWebhookConfig.builder()
                        .secret("********")
                        .url("https://webhook.site/80be479e-1e49-409a-bc98-45f82a5b1c")
                        .build())
                .build();
        GithubWebhookResponse webhookResponse = client.createWebhook(GITHUB_ORGANIZATION, webhookRequest);
        Assert.assertNotNull("This cannot be null", webhookResponse);
        Assert.assertEquals("web", webhookResponse.getName());
        testUpdateWebhook(webhookResponse);
    }

    public void testUpdateWebhook(GithubWebhookResponse webhook) throws GithubClientException {
        GithubWebhookRequest webhookRequest = GithubWebhookRequest.builder()
                .name("web")
                .events(List.of("project_card",
                        "project_column"))
                .config(GithubWebhookConfig.builder()
                        .secret("######")
                        .url("https://webhook.site/80be479e-1e49-409a-bc98-45f82a5b1c28")
                        .build())
                .build();
        GithubWebhookResponse webhookResponse = client.updateWebhook(GITHUB_ORGANIZATION, webhook.getId(), webhookRequest);
        Assert.assertNotNull("This cannot be null", webhookResponse);
        Assert.assertEquals("web", webhookResponse.getName());
    }

    @Test
    public void testGraphQl() throws GithubClientException {
        GraphQlResponse response = client.queryGraphQl("{\n" +
                "  viewer {\n" +
                "    pullRequests(first: 2) {\n" +
                "      pageInfo {\n" +
                "        endCursor\n" +
                "        hasNextPage\n" +
                "        hasPreviousPage\n" +
                "        startCursor\n" +
                "      }\n" +
                "      nodes {\n" +
                "        id\n" +
                "        merged\n" +
                "        title\n" +
                "        commits(first: 10) {\n" +
                "          nodes {\n" +
                "            commit {\n" +
                "              message\n" +
                "              author {\n" +
                "                name\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}");
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void searchPrs() throws GithubClientException {
        List<GithubPullRequestSearchResult> prs = client.searchPullRequests("levelops/commons-levelops", Instant.parse("2022-03-15T10:00:00+07:00"), Instant.parse("2022-03-15T11:30:15Z"), 0);
        DefaultObjectMapper.prettyPrint(prs);
        GithubPullRequest pr = client.getPullRequest("levelops", "commons-levelops", "2850");
        DefaultObjectMapper.prettyPrint(pr);
    }

    @Test
    public void searchPrsCustom() throws GithubClientException {
        List<GithubPullRequestSearchResult> prs = client.searchPullRequests("is:pr author:maxime-levelops created:2023-02-28..2023-02-28", null, null, 1, 5);
        DefaultObjectMapper.prettyPrint(prs);
    }

    @Test
    public void searchCommits() throws GithubClientException {
        List<GithubApiCommit> prs = client.searchCommits("author:maxime-levelops  author-date:2023-02-28..2023-02-28", "author-date", "desc", 1, 5);
        DefaultObjectMapper.prettyPrint(prs);
    }

    @Test
    public void orgs() {
        Stream<GithubOrganization> out = client.streamOrganizations();
        DefaultObjectMapper.prettyPrint(out.collect(Collectors.toList()));
    }

    @Test
    public void repos() throws GithubClientException {
        List<GithubRepository> repos = client.streamRepositories("levelops")
                .collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(repos);

        GithubPaginatedResponse<GithubApiRepoEvent> events = client.getRepoEvents(repos.get(0).getId(), 0, 10);
        DefaultObjectMapper.prettyPrint(events);
    }

    @Test
    public void installRepos() {
        client.streamInstallationRepositories().map(GithubRepository::getFullName).forEach(System.out::println);
    }

    @Test
    public void testCommits() throws GithubClientException {
        var commit = client.getCommit("levelops/commons-levelops", "9cc4deccbb9d5ec9a159d08b7e520d6a9ca12d58");
        System.out.println(commit);
        System.out.println(commit.getGitAuthor());
    }

    @Test
    public void testGetIssueComments() throws GithubClientException {
        var comments = client.streamIssueComments("levelops/commons-levelops", 3850).collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(comments);
    }

    @Test
    public void testGetUsers() throws GithubClientException {
//        String org = "harness-testing";
//        String org = "levelops";
        String org = "harness-io";
        var response = client.getOrgUsers(org, "", 100);
        var streamResponse = client.streamOrgUsers(org, false);
        System.out.println(response);
        DefaultObjectMapper.prettyPrint(streamResponse.collect(Collectors.toList()));
    }

    @Test
    public void testGetInstallationOrgs() throws GithubClientException {
        var installationOrgs = client.getAppInstallationOrgs(1, 10);
        var installationOrgsStream = client.streamAppInstallationOrgs().collect(Collectors.toList());

        var installations = client.getAppInstallations(1, 10);
        var installationsStream = client.streamAppInstallations().collect(Collectors.toList());

        System.out.println(installationOrgs);
        System.out.println(installationOrgsStream);

        System.out.println(installations);
        System.out.println(installationsStream);
    }

    @Test
    public void testSearchRepos() {
        List<String> repos = client.searchRepositories("etn-electrical", "iot")
                .map(GithubRepository::getName)
                .collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(repos);
    }
}