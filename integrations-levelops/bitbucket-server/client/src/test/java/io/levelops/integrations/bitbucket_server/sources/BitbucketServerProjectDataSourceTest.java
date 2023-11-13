package io.levelops.integrations.bitbucket_server.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClient;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientException;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientFactory;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerBranchInfo;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerCommit;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerCommitDiffInfo;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerEnrichedProjectData;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerIterativeScanQuery;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerPRActivity;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerProject;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerPullRequest;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class BitbucketServerProjectDataSourceTest {

    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    private BitbucketServerProjectDataSource commitsDataSource;
    private BitbucketServerProjectDataSource pullRequestsDataSource;

    @Before
    public void setup() throws BitbucketServerClientException {
        BitbucketServerClient client = Mockito.mock(BitbucketServerClient.class);
        BitbucketServerClientFactory clientFactory = Mockito.mock(BitbucketServerClientFactory.class);
        commitsDataSource = new BitbucketServerProjectDataSource(clientFactory, EnumSet.of(BitbucketServerProjectDataSource.Enrichment.COMMITS));
        pullRequestsDataSource = new BitbucketServerProjectDataSource(clientFactory, EnumSet.of(BitbucketServerProjectDataSource.Enrichment.PULL_REQUESTS));
        when(clientFactory.get(TEST_KEY, false)).thenReturn(client);
        BitbucketServerProject bitbucketServerProject = BitbucketServerProject.builder().id(1).name("test").key("TEST").build();
        when(client.streamProjects())
                .thenReturn(Stream.of(bitbucketServerProject));
        BitbucketServerRepository bitbucketServerRepository = BitbucketServerRepository.builder().id(1).name("test/repo").slug("repo").project(bitbucketServerProject).build();
        when(client.streamRepositories(bitbucketServerProject.getKey()))
                .thenReturn(Stream.of(bitbucketServerRepository));
        BitbucketServerCommit bitbucketServerCommit = BitbucketServerCommit.builder().id("def0123abcdef4567abcdef8987abcdef6543abc").committerTimestamp(new Date().toInstant().toEpochMilli()).build();
        when(client.streamCommits(bitbucketServerProject.getKey(), bitbucketServerRepository.getSlug()))
                .thenReturn(Stream.of(bitbucketServerCommit));
        BitbucketServerPullRequest bitbucketServerPullRequest = BitbucketServerPullRequest.builder().id(1).updatedDate(new Date().toInstant().toEpochMilli()).build();
        when(client.streamPullRequests(bitbucketServerProject.getKey(), bitbucketServerRepository.getSlug()))
                .thenReturn(Stream.of(bitbucketServerPullRequest));
        when(client.streamPrCommits(bitbucketServerProject.getKey(), bitbucketServerRepository.getSlug(), bitbucketServerPullRequest.getId()))
                .thenReturn(Stream.of(bitbucketServerCommit));
        BitbucketServerPRActivity bitbucketServerPRActivity = BitbucketServerPRActivity.builder().id(1).action("OPENED").build();
        when(client.streamPrActivities(bitbucketServerProject.getKey(), bitbucketServerRepository.getSlug(), bitbucketServerPullRequest.getId()))
                .thenReturn(Stream.of(bitbucketServerPRActivity));
        BitbucketServerBranchInfo bitbucketServerBranchInfo = BitbucketServerBranchInfo.builder().id("id").displayId("main").build();
        when(client.getDefaultBranch(bitbucketServerProject.getKey(), bitbucketServerRepository.getSlug()))
                .thenReturn(bitbucketServerBranchInfo);
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
    public void testCommits() throws FetchException {
        List<Data<BitbucketServerEnrichedProjectData>> projects = commitsDataSource.fetchMany(
                BitbucketServerProjectDataSource.BitbucketServerProjectQuery.builder()
                        .integrationKey(TEST_KEY)
                        .build()).collect(Collectors.toList());
        List<BitbucketServerCommit> commits = projects.stream()
                .map(Data::getPayload)
                .flatMap(project -> project.getCommits().stream())
                .collect(Collectors.toList());
        assertThat(commits).hasSize(1);
    }

    @Test
    public void testPullRequests() throws FetchException {
        List<Data<BitbucketServerEnrichedProjectData>> projects = pullRequestsDataSource.fetchMany(
                BitbucketServerProjectDataSource.BitbucketServerProjectQuery.builder()
                        .integrationKey(TEST_KEY)
                        .build()).collect(Collectors.toList());
        List<BitbucketServerPullRequest> pullRequests = projects.stream()
                .map(Data::getPayload)
                .flatMap(pr -> pr.getPullRequests().stream())
                .collect(Collectors.toList());
        assertThat(pullRequests).hasSize(1);
    }
}
