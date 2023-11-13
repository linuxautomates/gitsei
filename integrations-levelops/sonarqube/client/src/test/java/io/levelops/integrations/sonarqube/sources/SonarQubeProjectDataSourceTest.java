package io.levelops.integrations.sonarqube.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.sonarqube.client.SonarQubeClient;
import io.levelops.integrations.sonarqube.client.SonarQubeClientException;
import io.levelops.integrations.sonarqube.client.SonarQubeClientFactory;
import io.levelops.integrations.sonarqube.models.*;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class SonarQubeProjectDataSourceTest {

    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    SonarQubeProjectDataSource dataSource;

    @Before
    public void setup() throws SonarQubeClientException {

        SonarQubeClient sonarQubeClient = Mockito.mock(SonarQubeClient.class);
        SonarQubeClientFactory sonarQubeClientFactory = Mockito.mock(SonarQubeClientFactory.class);
        dataSource = new SonarQubeProjectDataSource(sonarQubeClientFactory, EnumSet.noneOf(SonarQubeProjectDataSource.Enrichment.class),null);
        when(sonarQubeClientFactory.get(TEST_KEY)).thenReturn(sonarQubeClient);

        Paging pg1 =  Paging.builder().pageIndex(1).pageSize(100).build();
        Paging pg2 =  Paging.builder().pageIndex(2).pageSize(100).build();

        List<Project> projectList1 = List.of(Project.builder().key("A").build(),
                Project.builder().key("B").build(),
                Project.builder().key("C").build());

        List<Project> projectList2 = List.of(Project.builder().key("X").build(),
                Project.builder().key("Y").build(),
                Project.builder().key("Z").build());

        ProjectResponse pr1 = ProjectResponse.builder().projects(projectList1).paging(pg1).build();
        ProjectResponse pr2 = ProjectResponse.builder().projects(projectList2).paging(pg2).build();

        MetricResponse response = MetricResponse.builder().build();
        when(sonarQubeClient.getMetrics(eq(1))).thenReturn(response);
        when(sonarQubeClient.getProjects(any(),eq(1))).thenReturn(pr1);
        when(sonarQubeClient.getProjects(any(),eq(2))).thenReturn(pr2);
    }

    @Test
    public void fetchMany() throws FetchException {
        SonarQubeProjectDataSource.SonarQubeProjectQuery sqpq = SonarQubeProjectDataSource
                .SonarQubeProjectQuery.builder().integrationKey(TEST_KEY).projectKeys(Set.of("X","Y","P")).build();
        Stream<Data<Project>> data = dataSource.fetchMany(sqpq);
        assertThat(data).hasSize(2);

        SonarQubeProjectDataSource.SonarQubeProjectQuery sqpq1 = SonarQubeProjectDataSource
                .SonarQubeProjectQuery.builder().integrationKey(TEST_KEY).projectKeys(Set.of("A","B","C")).build();
        Stream<Data<Project>> data1 = dataSource.fetchMany(sqpq1);
        assertThat(data1).hasSize(3);
    }
}