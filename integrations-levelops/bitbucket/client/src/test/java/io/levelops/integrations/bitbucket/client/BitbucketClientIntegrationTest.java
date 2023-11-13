package io.levelops.integrations.bitbucket.client;

import io.levelops.commons.client.oauth.OauthTokenInterceptor;
import io.levelops.commons.client.oauth.StaticOauthTokenProvider;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.databases.models.database.scm.converters.bitbucket.BitbucketPullRequestConverters;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.bitbucket.models.BitbucketCommit;
import io.levelops.integrations.bitbucket.models.BitbucketPullRequestActivity;
import okhttp3.OkHttpClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BitbucketClientIntegrationTest {
    BitbucketClient client;

    @Before
    public void setUp() throws Exception {
        String token = System.getenv("BB-TOKEN");
        client = BitbucketClient.builder()
                .objectMapper(DefaultObjectMapper.get())
                .okHttpClient(new OkHttpClient().newBuilder()
                        .addInterceptor(OauthTokenInterceptor.using(StaticOauthTokenProvider.builder()
                                .token(token)
                                .build()))
                        .build())
                .build();
    }

    @Test
    public void test() {
        //from=2022-01-21T05:04:19.308+0000, to=2022-01-21T05:09:19.307+0000
        Instant from = Instant.parse("2022-01-21T05:04:19Z");
        Instant to = Instant.parse("2022-01-21T05:09:19Z");
        String query = BitbucketClient.generateDateQuery("created_on", from, to);
        Assert.assertNotNull(query);
        Assert.assertEquals("created_on > 2022-01-21T05:04:19 AND created_on <= 2022-01-21T05:09:19", query);
    }

    @Test
    public void testPRsAndCommits() throws BitbucketClientException {
        Instant from = Instant.parse("2021-01-15T19:25:10Z");
        Instant to = Instant.parse("2022-05-15T20:05:10Z");
//        Instant.now().minus(190, ChronoUnit.DAYS), Instant.now()
        List<String> pullRequestsCommits = new ArrayList<>();
        client.streamWorkspaces()
                .peek(w -> System.out.println(" >>>> WORKSPACE: " + w.getName()))
                .forEach(bitbucketWorkspace -> {
                    client.streamRepositories(bitbucketWorkspace.getSlug()).forEach(repo -> {
                        System.out.println(" --- REPO: " + repo.getFullName() + " " + repo.getUuid());
                        client.streamPullRequests(repo.getWorkspaceSlug(), repo.getUuid(), from, to)
                                .forEach(pr -> {
                                    System.out.println("    --- PR: " + pr.getId());
                                    client.streamPullRequestCommits(bitbucketWorkspace.getSlug(), repo.getUuid(), pr.getId())
                                            .map(BitbucketCommit::getMessage)
                                            .forEach(s -> {
                                                System.out.println("      commit: " + s);
                                                pullRequestsCommits.add(s);
                                            });
                                });
                    });
                });
        Assert.assertEquals(pullRequestsCommits.size(), 3);
    }

    @Test
    public void testTags() throws BitbucketClientException {
        List<String> tagNames = new ArrayList<>();
        client.streamWorkspaces()
                .peek(w -> System.out.println(" >>>> WORKSPACE: " + w.getName()))
                .forEach(bitbucketWorkspace -> {
                    client.streamRepositories(bitbucketWorkspace.getSlug()).forEach(repo -> {
                        System.out.println(" --- REPO: " + repo.getFullName() + " " + repo.getUuid());
                        client.streamTags(repo.getWorkspaceSlug(), repo.getName(), false)
                                .forEach(tag -> {
                                    System.out.println("    --- TAG: " + tag.getName());
                                    tagNames.add(tag.getName());
                                });
                    });
                });
        System.out.println(tagNames);
        Assert.assertEquals(tagNames.size(), 6);
    }

    @Test
    public void testCommitsAndDiffStats() throws BitbucketClientException {
        List<String> bitbucketCommitDiffStats = new ArrayList<>();
        client.streamWorkspaces()
                .peek(w -> System.out.println(" >>>> WORKSPACE: " + w.getName()))
                .forEach(bitbucketWorkspace -> {
                    client.streamRepositories(bitbucketWorkspace.getSlug()).forEach(repo -> {
                        System.out.println(" --- REPO: " + repo.getFullName());
                        client.streamRepoCommits(repo.getWorkspaceSlug(), repo.getUuid())
                                .forEach(c -> {
                                    System.out.println("    commit: " + c.getMessage());
                                    client.streamRepoCommitDiffSets(c.getWorkspaceSlug(), c.getRepoUuid(), c.getHash())
                                            .forEach(
                                                    d -> {
                                                        System.out.println("      diffset: " + d.toString());
                                                        bitbucketCommitDiffStats.add(d.getStatus());
                                                    }
                                            );
                                });
                    });
                });
        Assert.assertEquals(bitbucketCommitDiffStats.size(), 8);
    }

    @Test
    public void testPRActivity() throws BitbucketClientException {
//        BitbucketPullRequest pr = client.getPullRequest("maxime-harness-test", "maxime-bb-test-1", "2");
//        DefaultObjectMapper.prettyPrint(pr);

//        DefaultObjectMapper.prettyPrint(client.streamPullRequestActivity("maxime-harness-test", "maxime-bb-test-1", "3").collect(Collectors.toList()));

        List<DbScmReview> output = client.streamPullRequestActivity("maxime-harness-test", "maxime-bb-test-1", "1")
                .flatMap(e -> BitbucketPullRequestConverters.parseActivity("1", e).stream())
                .collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(output);

    }
}