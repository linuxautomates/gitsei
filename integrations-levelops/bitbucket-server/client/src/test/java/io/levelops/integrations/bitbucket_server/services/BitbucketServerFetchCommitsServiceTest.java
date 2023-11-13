package io.levelops.integrations.bitbucket_server.services;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClient;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientException;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientFactory;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerCommit;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerCommitDiffInfo;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerEnrichedProjectData;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerProject;
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

public class BitbucketServerFetchCommitsServiceTest {

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId("1").tenantId("test").build();
    BitbucketServerClient client = Mockito.mock(BitbucketServerClient.class);
    BitbucketServerClientFactory clientFactory = Mockito.mock(BitbucketServerClientFactory.class);

    BitbucketServerProjectDataSource dataSource;
    BitbucketServerFetchCommitsService commitsService;
    BitbucketServerRepository bitbucketServerRepository;

    @Before
    public void setup() throws BitbucketServerClientException {
        dataSource = new BitbucketServerProjectDataSource(clientFactory, EnumSet.of(BitbucketServerProjectDataSource.Enrichment.COMMITS));
        commitsService = new BitbucketServerFetchCommitsService();

        when(clientFactory.get(TEST_INTEGRATION_KEY, true)).thenReturn(client);
        BitbucketServerProject bitbucketServerProject = BitbucketServerProject.builder().id(1).name("test").key("TEST").build();
        when(client.streamProjects())
                .thenReturn(Stream.of(bitbucketServerProject));
        bitbucketServerRepository = BitbucketServerRepository.builder().id(1).name("test/repo").slug("repo").project(bitbucketServerProject).build();
        when(client.streamRepositories(eq(bitbucketServerProject.getKey())))
                .thenReturn(Stream.of(bitbucketServerRepository));
        BitbucketServerCommit bitbucketServerCommit = BitbucketServerCommit.builder().id("def0123abcdef4567abcdef8987abcdef6543abc")
                .committerTimestamp(1622539968000L).build();
        when(client.streamCommits(bitbucketServerProject.getKey(), bitbucketServerRepository.getSlug()))
                .thenReturn(Stream.of(bitbucketServerCommit));
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
        List<BitbucketServerEnrichedProjectData> bitbucketServerEnrichedProjectData = commitsService.fetchCommits(client,
                1621549968000L, new Date().toInstant().toEpochMilli(), null, null, true).collect(Collectors.toList());
        assertThat(bitbucketServerEnrichedProjectData).hasSize(1);
        assertThat(bitbucketServerEnrichedProjectData.get(0).getPullRequests()).isNull();
        assertThat(bitbucketServerEnrichedProjectData.get(0).getProject().getName()).isEqualTo("test");
        assertThat(bitbucketServerEnrichedProjectData.get(0).getRepository().getName()).isEqualTo("test/repo");
        BitbucketServerCommit commit = bitbucketServerEnrichedProjectData.get(0).getCommits().get(0);
        assertThat(commit).isNotNull();
        assertThat(commit.getCommitUrl()).isEqualTo(null + "/projects/TEST/repos/repo/commits/def0123abcdef4567abcdef8987abcdef6543abc");
        assertThat(commit.getRepoName()).isEqualTo("test/repo");
        assertThat(commit.getProjectName()).isEqualTo("test");
        assertThat(commit.getAdditions()).isEqualTo(1);
        assertThat(commit.getDeletions()).isEqualTo(2);
    }
}
