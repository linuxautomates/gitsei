package io.levelops.integrations.awsdevtools.sources;

import com.amazonaws.services.codebuild.model.BatchGetProjectsResult;
import com.amazonaws.services.codebuild.model.ListProjectsResult;
import com.amazonaws.services.codebuild.model.Project;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.awsdevtools.client.AWSDevToolsClient;
import io.levelops.integrations.awsdevtools.client.AWSDevToolsClientException;
import io.levelops.integrations.awsdevtools.client.AWSDevToolsClientFactory;
import io.levelops.integrations.awsdevtools.models.AWSDevToolsQuery;
import io.levelops.integrations.awsdevtools.models.CBProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class AWSDevToolsProjectDataSourceTest {

    private static final AWSDevToolsQuery.RegionIntegrationKey TEST_KEY = AWSDevToolsQuery.RegionIntegrationKey.builder()
            .integrationKey(IntegrationKey.builder()
                    .integrationId(EMPTY)
                    .tenantId(EMPTY)
                    .build())
            .region("US_EAST_2")
            .build();

    private static final String NEXT_TOKEN = "next_token";

    AWSDevToolsProjectDataSource dataSource;

    @Before
    public void setup() throws AWSDevToolsClientException {
        AWSDevToolsClient client = Mockito.mock(AWSDevToolsClient.class);
        AWSDevToolsClientFactory clientFactory = Mockito.mock(AWSDevToolsClientFactory.class);

        dataSource = new AWSDevToolsProjectDataSource(clientFactory);

        when(clientFactory.get(TEST_KEY)).thenReturn(client);

        ListProjectsResult listProjectsResult1 = new ListProjectsResult();
        listProjectsResult1.setProjects(List.of("build-project-demo1"));
        listProjectsResult1.setNextToken(NEXT_TOKEN);
        when(client.listProjects(null))
                .thenReturn(listProjectsResult1);

        ListProjectsResult listProjectsResult2 = new ListProjectsResult();
        listProjectsResult2.setProjects(List.of("build-project-demo2", "build-project-demo3"));
        when(client.listProjects(listProjectsResult1.getNextToken()))
                .thenReturn(listProjectsResult2);

        Project project = new Project();
        project.setName(listProjectsResult1.getProjects().get(0));
        List<Project> projects1 = List.of(project);
        BatchGetProjectsResult batchGetProjectsResult1 = new BatchGetProjectsResult();
        batchGetProjectsResult1.setProjects(projects1);
        when(client.getProjects(eq(AWSDevToolsQuery.builder()
                .regionIntegrationKey(TEST_KEY)
                .build()), eq(listProjectsResult1.getProjects())))
                .thenReturn(batchGetProjectsResult1.getProjects());

        Project project2 = new Project();
        project.setName(listProjectsResult2.getProjects().get(0));
        Project project3 = new Project();
        project.setName(listProjectsResult2.getProjects().get(1));
        List<Project> projects2 = List.of(project2, project3);
        BatchGetProjectsResult batchGetProjectsResult2 = new BatchGetProjectsResult();
        batchGetProjectsResult2.setProjects(projects2);
        when(client.getProjects(eq(AWSDevToolsQuery.builder()
                .regionIntegrationKey(TEST_KEY)
                .build()), eq(listProjectsResult2.getProjects())))
                .thenReturn(batchGetProjectsResult2.getProjects());
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(AWSDevToolsQuery.builder().regionIntegrationKey(TEST_KEY).build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<CBProject>> projects = dataSource.fetchMany(AWSDevToolsQuery.builder()
                .regionIntegrationKey(TEST_KEY)
                .from(null)
                .token(null)
                .build())
                .collect(Collectors.toList());
        assertThat(projects).hasSize(3);
    }
}
