package io.levelops.integrations.awsdevtools.sources;

import com.amazonaws.services.codebuild.model.BatchGetBuildsResult;
import com.amazonaws.services.codebuild.model.Build;
import com.amazonaws.services.codebuild.model.ListBuildsResult;
import com.amazonaws.services.codebuild.model.Project;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.awsdevtools.client.AWSDevToolsClient;
import io.levelops.integrations.awsdevtools.client.AWSDevToolsClientException;
import io.levelops.integrations.awsdevtools.client.AWSDevToolsClientFactory;
import io.levelops.integrations.awsdevtools.models.AWSDevToolsQuery;
import io.levelops.integrations.awsdevtools.models.CBBuild;
import io.levelops.integrations.awsdevtools.services.AWSDevToolsEnrichmentService;
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

public class AWSDevToolsBuildDataSourceTest {

    private static final AWSDevToolsQuery.RegionIntegrationKey TEST_KEY = AWSDevToolsQuery.RegionIntegrationKey.builder()
            .integrationKey(IntegrationKey.builder()
                    .integrationId(EMPTY)
                    .tenantId(EMPTY)
                    .build())
            .region("US_EAST_2")
            .build();

    private static final String NEXT_TOKEN = "next_token";

    AWSDevToolsBuildDataSource dataSource;

    @Before
    public void setup() throws AWSDevToolsClientException {
        AWSDevToolsClient client = Mockito.mock(AWSDevToolsClient.class);
        AWSDevToolsClientFactory clientFactory = Mockito.mock(AWSDevToolsClientFactory.class);

        AWSDevToolsEnrichmentService enrichmentService = new AWSDevToolsEnrichmentService(1, 10);
        dataSource = new AWSDevToolsBuildDataSource(clientFactory, enrichmentService);

        when(clientFactory.get(TEST_KEY)).thenReturn(client);

        Project project = new Project();
        project.setName("Demo-project");
        project.setArn("arn:aws:codebuild:us-east-2:project-demo");
        List<Project> projects = List.of(project);
        when(client.getProjects(List.of("Demo-project")))
                .thenReturn(projects);

        ListBuildsResult listBuildsResult1 = new ListBuildsResult();
        listBuildsResult1.setIds(List.of("codebuild-build-demo"));
        listBuildsResult1.setNextToken(NEXT_TOKEN);
        when(client.listBuilds(null))
                .thenReturn(listBuildsResult1);

        ListBuildsResult listBuildsResult2 = new ListBuildsResult();
        listBuildsResult2.setIds(List.of("codebuild-build-demo2", "codebuild-build-demo3"));
        when(client.listBuilds(NEXT_TOKEN))
                .thenReturn(listBuildsResult2);

        Build build1 = new Build();
        build1.setId(listBuildsResult1.getIds().get(0));
        build1.setProjectName("Demo-project");
        List<Build> builds = List.of(build1);
        BatchGetBuildsResult batchGetBuildsResult1 = new BatchGetBuildsResult();
        batchGetBuildsResult1.setBuilds(builds);
        when(client.getBuilds(eq(AWSDevToolsQuery.builder()
                .regionIntegrationKey(TEST_KEY)
                .build()), eq(listBuildsResult1.getIds())))
                .thenReturn(batchGetBuildsResult1.getBuilds());

        Build build2 = new Build();
        build2.setId(listBuildsResult2.getIds().get(0));
        build2.setProjectName("Demo-project");
        Build build3 = new Build();
        build3.setId(listBuildsResult2.getIds().get(1));
        build3.setProjectName("Demo-project");
        List<Build> builds2 = List.of(build2, build3);
        BatchGetBuildsResult batchGetBuildsResult2 = new BatchGetBuildsResult();
        batchGetBuildsResult2.setBuilds(builds2);
        when(client.getBuilds(eq(AWSDevToolsQuery.builder()
                .regionIntegrationKey(TEST_KEY)
                .build()), eq(listBuildsResult2.getIds())))
                .thenReturn(batchGetBuildsResult2.getBuilds());
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(AWSDevToolsQuery.builder().regionIntegrationKey(TEST_KEY).build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<CBBuild>> builds = dataSource.fetchMany(AWSDevToolsQuery.builder()
                .regionIntegrationKey(TEST_KEY)
                .from(null)
                .token(null)
                .build())
                .collect(Collectors.toList());
        assertThat(builds).hasSize(3);
    }
}
