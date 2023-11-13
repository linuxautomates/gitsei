package io.levelops.integrations.awsdevtools.client;

import com.amazonaws.services.codebuild.model.*;
import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.awsdevtools.models.AWSDevToolsQuery;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AWSDevToolsClientIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "codebuild";
    private static final String APPLICATION = "awsdevelopertools";
    private static final String AWS_ACCESS_KEY_ID = System.getenv("AWS_ACCESS_KEY_ID");
    private static final String AWS_SECRET_KEY = System.getenv("AWS_SECRET_KEY");
    private static final String AWS_REGION = System.getenv("AWS_REGION");

    private static final AWSDevToolsQuery.RegionIntegrationKey TEST_INTEGRATION_KEY = AWSDevToolsQuery.RegionIntegrationKey.builder()
            .integrationKey(IntegrationKey.builder()
                    .integrationId(INTEGRATION_ID)
                    .tenantId(TENANT_ID)
                    .build())
            .region(AWS_REGION)
            .build();

    private AWSDevToolsClientFactory clientFactory;

    @Before
    public void setup() {
        if (clientFactory != null)
            return;
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory
                .builder()
                .apiKey(TENANT_ID, INTEGRATION_ID, APPLICATION, null, Collections.emptyMap(),
                        AWS_ACCESS_KEY_ID, AWS_SECRET_KEY)
                .build());
        clientFactory = AWSDevToolsClientFactory.builder()
                .inventoryService(inventoryService)
                .build();
    }

    @Test
    public void testProjects() throws AWSDevToolsClientException {
        ListProjectsResult listProjectsResult = clientFactory.get(TEST_INTEGRATION_KEY).listProjects(null);
        DefaultObjectMapper.prettyPrint(listProjectsResult);
        assertThat(listProjectsResult.getProjects()).isNotNull();
        List<Project> projects = clientFactory.get(TEST_INTEGRATION_KEY).getProjects(AWSDevToolsQuery.builder()
                        .to(Date.from(Instant.now())).build(),
                listProjectsResult.getProjects());
        DefaultObjectMapper.prettyPrint(projects);
        assertThat(projects).isNotNull();
    }

    @Test
    public void testBuilds() throws AWSDevToolsClientException {
        ListBuildsResult listBuildsResult = clientFactory.get(TEST_INTEGRATION_KEY).listBuilds(null);
        DefaultObjectMapper.prettyPrint(listBuildsResult);
        assertThat(listBuildsResult.getIds()).isNotNull();
        List<Build> builds = clientFactory.get(TEST_INTEGRATION_KEY).getBuilds(AWSDevToolsQuery.builder()
                        .to(Date.from(Instant.now())).build(),
                listBuildsResult.getIds());
        DefaultObjectMapper.prettyPrint(builds);
        assertThat(builds).isNotNull();
    }

    @Test
    public void testBuildBatches() throws AWSDevToolsClientException {
        ListBuildBatchesResult listBuildBatchesResult = clientFactory.get(TEST_INTEGRATION_KEY).listBuildBatches(null);
        DefaultObjectMapper.prettyPrint(listBuildBatchesResult);
        assertThat(listBuildBatchesResult.getIds()).isNotNull();
        List<BuildBatch> buildBatches = clientFactory.get(TEST_INTEGRATION_KEY).getBuildBatches(AWSDevToolsQuery.builder()
                        .to(Date.from(Instant.now())).build(),
                listBuildBatchesResult.getIds());
        DefaultObjectMapper.prettyPrint(buildBatches);
        assertThat(buildBatches).isNotNull();
    }

    @Test
    public void testReports() throws AWSDevToolsClientException {
        ListReportsResult listReportsResult = clientFactory.get(TEST_INTEGRATION_KEY).listReports(null);
        DefaultObjectMapper.prettyPrint(listReportsResult);
        assertThat(listReportsResult.getReports()).isNotNull();
        List<Report> reports = clientFactory.get(TEST_INTEGRATION_KEY).getReports(listReportsResult.getReports());
        DefaultObjectMapper.prettyPrint(reports);
        assertThat(reports).isNotNull();
    }

    @Test
    public void testReportGroups() throws AWSDevToolsClientException {
        ListReportGroupsResult listReportGroupsResult = clientFactory.get(TEST_INTEGRATION_KEY).listReportGroups(null);
        DefaultObjectMapper.prettyPrint(listReportGroupsResult);
        assertThat(listReportGroupsResult.getReportGroups()).isNotNull();
        List<ReportGroup> reportGroups = clientFactory.get(TEST_INTEGRATION_KEY).getReportGroups(listReportGroupsResult.getReportGroups());
        DefaultObjectMapper.prettyPrint(reportGroups);
        assertThat(reportGroups).isNotNull();
    }

    @Test
    public void testRegion() throws AWSDevToolsClientException {
        assertThat(clientFactory.get(TEST_INTEGRATION_KEY).getRegion()).isNotNull();
    }
}
