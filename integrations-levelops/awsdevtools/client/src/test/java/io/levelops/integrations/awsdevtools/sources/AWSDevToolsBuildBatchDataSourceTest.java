package io.levelops.integrations.awsdevtools.sources;

import com.amazonaws.services.codebuild.model.BatchGetBuildBatchesResult;
import com.amazonaws.services.codebuild.model.BuildBatch;
import com.amazonaws.services.codebuild.model.ListBuildBatchesResult;
import com.amazonaws.services.codebuild.model.Project;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.awsdevtools.client.AWSDevToolsClient;
import io.levelops.integrations.awsdevtools.client.AWSDevToolsClientException;
import io.levelops.integrations.awsdevtools.client.AWSDevToolsClientFactory;
import io.levelops.integrations.awsdevtools.models.AWSDevToolsQuery;
import io.levelops.integrations.awsdevtools.models.CBBuildBatch;
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

public class AWSDevToolsBuildBatchDataSourceTest {

    private static final AWSDevToolsQuery.RegionIntegrationKey TEST_KEY = AWSDevToolsQuery.RegionIntegrationKey.builder()
            .integrationKey(IntegrationKey.builder()
                    .integrationId(EMPTY)
                    .tenantId(EMPTY)
                    .build())
            .region("US_EAST_2")
            .build();

    private static final String NEXT_TOKEN = "next_token";

    AWSDevToolsBuildBatchDataSource dataSource;

    @Before
    public void setup() throws AWSDevToolsClientException {
        AWSDevToolsClient client = Mockito.mock(AWSDevToolsClient.class);
        AWSDevToolsClientFactory clientFactory = Mockito.mock(AWSDevToolsClientFactory.class);

        AWSDevToolsEnrichmentService enrichmentService = new AWSDevToolsEnrichmentService(1, 10);
        dataSource = new AWSDevToolsBuildBatchDataSource(clientFactory, enrichmentService);

        when(clientFactory.get(TEST_KEY)).thenReturn(client);

        Project project = new Project();
        project.setName("Demo-project");
        project.setArn("arn:aws:codebuild:us-east-2:project-demo");
        List<Project> projects = List.of(project);
        when(client.getProjects(List.of("Demo-project")))
                .thenReturn(projects);

        ListBuildBatchesResult listBuildBatchesResult1 = new ListBuildBatchesResult();
        listBuildBatchesResult1.setIds(List.of("build-batch-demo1"));
        listBuildBatchesResult1.setNextToken(NEXT_TOKEN);
        when(client.listBuildBatches(null))
                .thenReturn(listBuildBatchesResult1);

        ListBuildBatchesResult listBuildBatchesResult2 = new ListBuildBatchesResult();
        listBuildBatchesResult2.setIds(List.of("build-batch-demo2", "build-batch-demo3"));
        when(client.listBuildBatches(NEXT_TOKEN))
                .thenReturn(listBuildBatchesResult2);

        BuildBatch buildBatch1 = new BuildBatch();
        buildBatch1.setId(listBuildBatchesResult1.getIds().get(0));
        buildBatch1.setProjectName("Demo-project");
        List<BuildBatch> buildBatches1 = List.of(buildBatch1);
        BatchGetBuildBatchesResult batchGetBuildBatchesResult1 = new BatchGetBuildBatchesResult();
        batchGetBuildBatchesResult1.setBuildBatches(buildBatches1);
        when(client.getBuildBatches(eq(AWSDevToolsQuery.builder()
                .regionIntegrationKey(TEST_KEY)
                .build()), eq(listBuildBatchesResult1.getIds())))
                .thenReturn(batchGetBuildBatchesResult1.getBuildBatches());

        BuildBatch buildBatch2 = new BuildBatch();
        buildBatch2.setId(listBuildBatchesResult2.getIds().get(0));
        buildBatch2.setProjectName("Demo-project");
        BuildBatch buildBatch3 = new BuildBatch();
        buildBatch3.setId(listBuildBatchesResult2.getIds().get(1));
        buildBatch3.setProjectName("Demo-project");
        List<BuildBatch> buildBatches2 = List.of(buildBatch2, buildBatch3);
        BatchGetBuildBatchesResult batchGetBuildBatchesResult2 = new BatchGetBuildBatchesResult();
        batchGetBuildBatchesResult2.setBuildBatches(buildBatches2);
        when(client.getBuildBatches(eq(AWSDevToolsQuery.builder()
                .regionIntegrationKey(TEST_KEY)
                .build()), eq(listBuildBatchesResult2.getIds())))
                .thenReturn(batchGetBuildBatchesResult2.getBuildBatches());
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(AWSDevToolsQuery.builder().regionIntegrationKey(TEST_KEY).build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<CBBuildBatch>> buildBatches = dataSource.fetchMany(AWSDevToolsQuery.builder()
                .regionIntegrationKey(TEST_KEY)
                .from(null)
                .token(null)
                .build())
                .collect(Collectors.toList());
        assertThat(buildBatches).hasSize(3);
    }
}
