package io.levelops.commons.databases.services;

import io.levelops.commons.databases.issue_management.DbIssueStatusMetadata;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
public class WorkItemsMetadataServiceTest {
    private static final String company = "test";
    private static final int INTEGRATIONS_COUNT = 2;
    private static final Random RANDOM = new Random(System.currentTimeMillis());

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private DataSource dataSource;
    private IntegrationService integrationService;
    private WorkItemsMetadataService workItemsMetadataService;


    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        integrationService = new IntegrationService(dataSource);
        workItemsMetadataService = new WorkItemsMetadataService(dataSource);

        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        workItemsMetadataService.ensureTableExistence(company);
    }

    //region Test Get
    private void testGet(DbIssueStatusMetadata allExpected) throws SQLException {
        Optional<DbIssueStatusMetadata> result = workItemsMetadataService.get(company, allExpected.getId().toString());
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isPresent());
        verifyRecord(result.get(), allExpected);
    }
    //endregion

    public void testGetByStatus(Integration integration, String status) {
        Optional<DbIssueStatusMetadata> metadataServiceByStatus = workItemsMetadataService.getByStatus(company, integration.getId(), status);
        Assertions.assertTrue(metadataServiceByStatus.isPresent());
    }

    private DbIssueStatusMetadata createDbIssueStatusMetadata(Integration integration, int i) {
        DbIssueStatusMetadata expected = DbIssueStatusMetadata.builder()
                .integrationId(integration.getId())
                .projectId("project-" + i)
                .statusId("status-" + i)
                .statusCategory("status-category-" + i)
                .status("status-" + i)
                .build();
        return expected;
    }

    private DbIssueStatusMetadata testInsert(Integration integration, int i) throws SQLException {
        DbIssueStatusMetadata expected = createDbIssueStatusMetadata(integration, i);
        String id = workItemsMetadataService.insert(company, expected);
        Assert.assertNotNull(id);
        expected = expected.toBuilder().id(UUID.fromString(id)).build();
        return expected;
    }

    private List<DbIssueStatusMetadata> testInserts(Integration integration, int n) throws SQLException {
        List<DbIssueStatusMetadata> expected = new ArrayList<>();
        for(int i=0; i<n; i++) {
            DbIssueStatusMetadata current = testInsert(integration, i);
            testGet(current);
            expected.add(current);
        }
        return expected;
    }

    @Test
    public void test() throws SQLException {
        int n = 5;
        List<Integration> integrations = IntegrationUtils.createIntegrations(integrationService, company, INTEGRATIONS_COUNT);
        List<DbIssueStatusMetadata> allExpected = new ArrayList<>();
        for(Integration integration : integrations) {
            List<DbIssueStatusMetadata> currentExpected = testInserts(integration, n);
            allExpected.addAll(currentExpected);
        }
        for(int i=0; i < INTEGRATIONS_COUNT; i++) {
            Integration integration = integrations.get(i);
            int testIndex = RANDOM.nextInt(n);
            String projectId = "project-" + testIndex;
            String statusId = "status-" + testIndex;
            DbListResponse<DbIssueStatusMetadata> dbListResponse = workItemsMetadataService.listByFilter(company, 0, 100, null, List.of(Integer.parseInt(integration.getId())), List.of(projectId), List.of(statusId));
            Assert.assertNotNull(dbListResponse);
            Assert.assertEquals(1, dbListResponse.getCount().intValue());
            Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
            Assert.assertEquals(1, dbListResponse.getRecords().size());
            verifyRecord(dbListResponse.getRecords().get(0), allExpected.get((i *n) + testIndex));
            testGetByStatus(integration, "status-" + i);
        }
    }

    //region Verify
    private void verifyRecord(DbIssueStatusMetadata a, DbIssueStatusMetadata e) {
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getIntegrationId(), e.getIntegrationId());
        Assert.assertEquals(a.getProjectId(), e.getProjectId());
        Assert.assertEquals(a.getStatus(), e.getStatus());
        Assert.assertEquals(a.getStatusCategory(), e.getStatusCategory());
        Assert.assertEquals(a.getStatusId(), e.getStatusId());
        Assert.assertNotNull(a.getCreatedAt());
    }
    private void verifyRecords(List<DbIssueStatusMetadata> a, List<DbIssueStatusMetadata> e) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, DbIssueStatusMetadata> actualMap = a.stream().collect(Collectors.toMap(DbIssueStatusMetadata::getId, x -> x));
        Map<UUID, DbIssueStatusMetadata> expectedMap = e.stream().collect(Collectors.toMap(DbIssueStatusMetadata::getId, x -> x));

        for (UUID key : actualMap.keySet()) {
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }
    //endregion
}