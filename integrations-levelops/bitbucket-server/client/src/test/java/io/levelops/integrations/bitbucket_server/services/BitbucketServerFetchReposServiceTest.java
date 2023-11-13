package io.levelops.integrations.bitbucket_server.services;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClient;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientException;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientFactory;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerBranchInfo;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerEnrichedProjectData;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerProject;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerRepository;
import io.levelops.integrations.bitbucket_server.sources.BitbucketServerProjectDataSource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class BitbucketServerFetchReposServiceTest {

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId("1").tenantId("test").build();
    BitbucketServerClient client = Mockito.mock(BitbucketServerClient.class);
    BitbucketServerClientFactory clientFactory = Mockito.mock(BitbucketServerClientFactory.class);

    BitbucketServerProjectDataSource dataSource;
    BitbucketServerFetchReposService reposService;
    BitbucketServerRepository bitbucketServerRepository;

    @Before
    public void setup() throws BitbucketServerClientException {
        dataSource = new BitbucketServerProjectDataSource(clientFactory, EnumSet.of(BitbucketServerProjectDataSource.Enrichment.REPOSITORIES));
        reposService = new BitbucketServerFetchReposService();

        when(clientFactory.get(TEST_INTEGRATION_KEY, true)).thenReturn(client);
        BitbucketServerProject bitbucketServerProject = BitbucketServerProject.builder().id(1).name("test").key("TEST").build();
        when(client.streamProjects())
                .thenReturn(Stream.of(bitbucketServerProject));
        bitbucketServerRepository = BitbucketServerRepository.builder().id(1).name("test/repo").slug("repo").project(bitbucketServerProject).build();
        when(client.streamRepositories(eq(bitbucketServerProject.getKey())))
                .thenReturn(Stream.of(bitbucketServerRepository));
        BitbucketServerBranchInfo branchInfo = BitbucketServerBranchInfo.builder().id("1").displayId("main").build();
        when(client.getDefaultBranch(eq(bitbucketServerProject.getKey()), eq(bitbucketServerRepository.getSlug())))
                .thenReturn(branchInfo);
    }

    @Test
    public void test() throws BitbucketServerClientException {
        List<BitbucketServerEnrichedProjectData> bitbucketServerEnrichedProjectData = reposService.fetchRepos(client, null).collect(Collectors.toList());
        assertThat(bitbucketServerEnrichedProjectData).hasSize(1);
        assertThat(bitbucketServerEnrichedProjectData.get(0).getPullRequests()).isNull();
        assertThat(bitbucketServerEnrichedProjectData.get(0).getCommits()).isNull();
        assertThat(bitbucketServerEnrichedProjectData.get(0).getRepository().getName()).isEqualTo("test/repo");
        assertThat(bitbucketServerEnrichedProjectData.get(0).getRepository().getDefaultBranch()).isEqualTo("main");
    }
}
