package io.levelops.integrations.circleci.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.circleci.client.CircleCIClient;
import io.levelops.integrations.circleci.client.CircleCIClientException;
import io.levelops.integrations.circleci.client.CircleCIClientFactory;
import io.levelops.integrations.circleci.models.CircleCIBuild;
import io.levelops.integrations.circleci.models.CircleCIIngestionQuery;
import io.levelops.integrations.circleci.models.CircleCIProject;
import io.levelops.integrations.circleci.services.CircleCIBuildEnrichmentService;
import io.levelops.integrations.circleci.source.CircleCIBuildDataSource;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

public class CircleCIBuildDataSourceTest {

    private static final String BUILD_FILE_NAME = "build.json";

    private static final String PROJECT_FILE_NAME = "project.json";
    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();
    private CircleCIBuildDataSource dataSource;
    private List<CircleCIBuild> recentBuilds;

    private List<CircleCIProject> projects;

    @Before
    public void setup() throws CircleCIClientException, IOException {
        CircleCIClient client = Mockito.mock(CircleCIClient.class);
        CircleCIClientFactory clientFactory = Mockito.mock(CircleCIClientFactory.class);
        CircleCIBuildEnrichmentService enrichmentService = new CircleCIBuildEnrichmentService(1, 10);
        dataSource = new CircleCIBuildDataSource(clientFactory, enrichmentService);

        when(clientFactory.get(TEST_KEY)).thenReturn(client);

        CircleCIBuild build = DefaultObjectMapper.get().readValue(CircleCIBuildDataSourceTest.class.getClassLoader()
                .getResourceAsStream(BUILD_FILE_NAME), CircleCIBuild.class);
        recentBuilds = new ArrayList<>();
        recentBuilds.add(build);

        CircleCIProject project = DefaultObjectMapper.get().readValue(CircleCIBuildDataSourceTest.class.getClassLoader()
                .getResourceAsStream(PROJECT_FILE_NAME), CircleCIProject.class);
        projects = new ArrayList<>();
        projects.add(project);

        when(client.streamBuilds()).thenReturn(recentBuilds.stream());
        when(client.getProjects()).thenReturn(projects);
        when(client.getBuild(anyString(), anyInt())).thenReturn(CircleCIBuild.builder().build());
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(CircleCIIngestionQuery.builder().integrationKey(TEST_KEY).build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<CircleCIBuild>> builds = dataSource.fetchMany(CircleCIIngestionQuery.builder()
                        .integrationKey(TEST_KEY)
                        .from(recentBuilds.get(0).getStartTime())
                        .to(DateUtils.addMinutes(recentBuilds.get(0).getStopTime(),1))
                        .build())
                .collect(Collectors.toList());
        assertThat(builds).hasSize(1);
    }
}
