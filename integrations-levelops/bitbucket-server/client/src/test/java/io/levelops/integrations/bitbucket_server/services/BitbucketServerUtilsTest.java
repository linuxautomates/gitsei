package io.levelops.integrations.bitbucket_server.services;

import io.levelops.integrations.bitbucket.models.BitbucketProject;
import io.levelops.integrations.bitbucket.models.BitbucketRepository;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClient;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientException;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerProject;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerRepository;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class BitbucketServerUtilsTest {
    @Test
    public void testFetchRepos() throws BitbucketServerClientException {
        BitbucketServerClient client = Mockito.mock(BitbucketServerClient.class);
        List<BitbucketServerProject> projects = List.of(
                BitbucketServerProject.builder().key("STEPH").name("STEPH").build(),
                BitbucketServerProject.builder().key("KLAY").name("KLAY").build()
        );
        List<BitbucketServerRepository> stephRepos = List.of(
                BitbucketServerRepository.builder().slug("steph-repo-1").name("Steph's Repo 1").build(),
                BitbucketServerRepository.builder().slug("steph-repo-2").name("Steph's Repo 2").build()
        );
        List<BitbucketServerRepository> klayRepos = List.of(
                BitbucketServerRepository.builder().slug("klay-repo-1").name("Klay's Repo 1").build(),
                BitbucketServerRepository.builder().slug("klay-repo-2").name("Klay's Repo 2").build()
        );
        when(client.streamRepositories("STEPH")).thenAnswer(i -> stephRepos.stream());
        when(client.streamRepositories("KLAY")).thenAnswer(i -> klayRepos.stream());
        when(client.streamProjects()).thenAnswer(i -> projects.stream());
        var s = BitbucketServerUtils.fetchRepos(client, null, null).collect(Collectors.toList());
        assertThat(s).hasSize(4);

        s = BitbucketServerUtils.fetchRepos(client, List.of("Steph's Repo 1"), null).collect(Collectors.toList());
        assertThat(s).hasSize(1);

        s = BitbucketServerUtils.fetchRepos(client, null, List.of("KLAY")).collect(Collectors.toList());
        assertThat(s).hasSize(2);

        s = BitbucketServerUtils.fetchRepos(client, List.of("Steph's Repo 1"), List.of("KLAY")).collect(Collectors.toList());
        assertThat(s).hasSize(0);

        s = BitbucketServerUtils.fetchRepos(client, List.of(), List.of()).collect(Collectors.toList());
        assertThat(s).hasSize(4);
    }

}