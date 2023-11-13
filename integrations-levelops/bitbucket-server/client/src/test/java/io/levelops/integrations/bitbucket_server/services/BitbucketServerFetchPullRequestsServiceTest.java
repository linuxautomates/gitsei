package io.levelops.integrations.bitbucket_server.services;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClient;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientException;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientFactory;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerCommit;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerCommitDiffInfo;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerEnrichedProjectData;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerPRActivity;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerProject;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerPullRequest;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerRepository;
import io.levelops.integrations.bitbucket_server.sources.BitbucketServerProjectDataSource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class BitbucketServerFetchPullRequestsServiceTest {

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId("1").tenantId("test").build();
    BitbucketServerClient client = Mockito.mock(BitbucketServerClient.class);
    BitbucketServerClientFactory clientFactory = Mockito.mock(BitbucketServerClientFactory.class);

    BitbucketServerProjectDataSource dataSource;
    BitbucketServerFetchPullRequestsService pullRequestsService;
    BitbucketServerRepository bitbucketServerRepository;

    @Before
    public void setup() throws BitbucketServerClientException {
        dataSource = new BitbucketServerProjectDataSource(clientFactory, EnumSet.of(BitbucketServerProjectDataSource.Enrichment.PULL_REQUESTS));
        pullRequestsService = new BitbucketServerFetchPullRequestsService();

        when(clientFactory.get(TEST_INTEGRATION_KEY, true)).thenReturn(client);
        BitbucketServerProject bitbucketServerProject = BitbucketServerProject.builder().id(1).name("test").key("TEST").build();
        when(client.streamProjects())
                .thenReturn(Stream.of(bitbucketServerProject));
        bitbucketServerRepository = BitbucketServerRepository.builder().id(1).name("test/repo").slug("repo").project(bitbucketServerProject).build();
        when(client.streamRepositories(eq(bitbucketServerProject.getKey())))
                .thenReturn(Stream.of(bitbucketServerRepository));
        BitbucketServerCommit bitbucketServerCommit = BitbucketServerCommit.builder().id("def0123abcdef4567abcdef8987abcdef6543abc").build();
        when(client.streamCommits(bitbucketServerProject.getKey(), bitbucketServerRepository.getSlug()))
                .thenReturn(Stream.of(bitbucketServerCommit));
        BitbucketServerPullRequest bitbucketServerPullRequest = BitbucketServerPullRequest.builder().id(1).updatedDate(1622539968000L).build();
        when(client.streamPullRequests(bitbucketServerProject.getKey(), bitbucketServerRepository.getSlug()))
                .thenReturn(Stream.of(bitbucketServerPullRequest));
        when(client.streamPrCommits(bitbucketServerProject.getKey(), bitbucketServerRepository.getSlug(), bitbucketServerPullRequest.getId()))
                .thenReturn(Stream.of(bitbucketServerCommit));
        BitbucketServerPRActivity bitbucketServerPRActivity = BitbucketServerPRActivity.builder().id(1).action("OPENED").build();
        when(client.streamPrActivities(bitbucketServerProject.getKey(), bitbucketServerRepository.getSlug(), bitbucketServerPullRequest.getId()))
                .thenReturn(Stream.of(bitbucketServerPRActivity));
        BitbucketServerCommitDiffInfo commitDiffInfo = BitbucketServerCommitDiffInfo.builder()
                .diffs(List.of(BitbucketServerCommitDiffInfo.Diff.builder()
                        .destination(BitbucketServerCommitDiffInfo.Diff.FileReference.builder()
                                .name("test.txt")
                                .build())
                        .hunks(List.of(BitbucketServerCommitDiffInfo.Diff.Hunk.builder()
                                .segments(List.of(BitbucketServerCommitDiffInfo.Diff.Hunk.Segment.builder()
                                                .type("ADDED")
                                                .lines(List.of(BitbucketServerCommitDiffInfo.Diff.Hunk.Segment.Line.builder()
                                                        .destination(1)
                                                        .source(2)
                                                        .build()))
                                                .build(),
                                        BitbucketServerCommitDiffInfo.Diff.Hunk.Segment.builder()
                                                .type("REMOVED")
                                                .lines(List.of(BitbucketServerCommitDiffInfo.Diff.Hunk.Segment.Line.builder()
                                                                .destination(1)
                                                                .source(2)
                                                                .build(),
                                                        BitbucketServerCommitDiffInfo.Diff.Hunk.Segment.Line.builder()
                                                                .destination(1)
                                                                .source(2)
                                                                .build()))
                                                .build()))
                                .build()))
                        .build()))
                .build();
        when(client.getCommitDiff(bitbucketServerProject.getKey(), bitbucketServerRepository.getSlug(), bitbucketServerCommit.getId()))
                .thenReturn(commitDiffInfo);
    }

    @Test
    public void test() throws BitbucketServerClientException {
        List<BitbucketServerEnrichedProjectData> bitbucketServerEnrichedProjectData = pullRequestsService.fetchPullRequests(client,
                1621549968000L, new Date().toInstant().toEpochMilli(), null, null, true, true, true).collect(Collectors.toList());
        assertThat(bitbucketServerEnrichedProjectData).hasSize(1);
        assertThat(bitbucketServerEnrichedProjectData.get(0).getCommits()).isNull();
        assertThat(bitbucketServerEnrichedProjectData.get(0).getProject().getName()).isEqualTo("test");
        assertThat(bitbucketServerEnrichedProjectData.get(0).getRepository().getName()).isEqualTo("test/repo");
        BitbucketServerPullRequest pr = bitbucketServerEnrichedProjectData.get(0).getPullRequests().get(0);
        assertThat(pr).isNotNull();
        assertThat(pr.getPrCommits()).hasSize(1);
        assertThat(pr.getActivities()).hasSize(1);
    }
}
