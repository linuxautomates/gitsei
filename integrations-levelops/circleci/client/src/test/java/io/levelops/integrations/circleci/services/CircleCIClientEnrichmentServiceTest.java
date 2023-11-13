package io.levelops.integrations.circleci.services;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.circleci.client.CircleCIClient;
import io.levelops.integrations.circleci.client.CircleCIClientException;
import io.levelops.integrations.circleci.models.CircleCIBuild;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class CircleCIClientEnrichmentServiceTest {

    private static final String BUILD_FILE_NAME = "build.json";
    private CircleCIClient client;
    private CircleCIBuildEnrichmentService enrichmentService;
    private final List<CircleCIBuild> recentBuilds = new ArrayList<>();

    @Before
    public void setup() throws CircleCIClientException, IOException {
        client = Mockito.mock(CircleCIClient.class);
        enrichmentService = new CircleCIBuildEnrichmentService(2, 10);
        CircleCIBuild build = DefaultObjectMapper.get().readValue(CircleCIClientEnrichmentServiceTest.class.getClassLoader()
                        .getResourceAsStream(BUILD_FILE_NAME), CircleCIBuild.class);
        recentBuilds.add(build);
        when(client.getBuild(anyString(), anyInt())).thenReturn(CircleCIBuild.builder()
                .steps(List.of())
                .build());
    }

    @Test
    public void enrich() {

        CircleCIBuild build = recentBuilds.get(0);
        assertThat(build.getSteps()).isNull();

        List<CircleCIBuild> enrichedRecentBuilds = enrichmentService.enrichBuilds(client, recentBuilds);
        assertThat(enrichedRecentBuilds).isNotNull();
        assertThat(enrichedRecentBuilds).hasSize(1);
        build = enrichedRecentBuilds.get(0);

        assertThat(build.getSteps()).isNotNull();
    }
}
