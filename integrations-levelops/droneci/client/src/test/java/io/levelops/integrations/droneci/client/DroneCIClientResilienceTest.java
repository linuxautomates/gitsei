package io.levelops.integrations.droneci.client;

import io.levelops.integrations.droneci.models.DroneCIBuild;
import io.levelops.integrations.droneci.models.DroneCIEnrichRepoData;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class DroneCIClientResilienceTest {
    public static final int PER_PAGE = 20;
    public static final int PAGE = 1;

    public static final String OWNER_NAME = System.getenv("DRONECI_OWNER");

    public static final String REPO_NAME = System.getenv("DRONECI_REPO");
    public static DroneCIClient client;

    @Before
    public void setup() throws DroneCIClientException {
        client = Mockito.mock(DroneCIClient.class);

        when(client.streamRepositories()).thenCallRealMethod();
        when(client.getRepositories(PAGE, PER_PAGE)).thenThrow(new DroneCIClientException("Not Authorised"));

        when(client.streamRepoBuilds(OWNER_NAME, REPO_NAME)).thenCallRealMethod();
        when(client.getRepoBuilds(OWNER_NAME, REPO_NAME, PAGE, PER_PAGE))
                .thenThrow(new DroneCIClientException("Not Authorised"));
    }

    @Test
    public void testResilience() {
        List<DroneCIEnrichRepoData> repos = client.streamRepositories().collect(Collectors.toList());
        assertThat(repos).hasSize(0);

        List<DroneCIBuild> builds = client.streamRepoBuilds(OWNER_NAME, REPO_NAME).collect(Collectors.toList());
        assertThat(builds).hasSize(0);
    }
}
