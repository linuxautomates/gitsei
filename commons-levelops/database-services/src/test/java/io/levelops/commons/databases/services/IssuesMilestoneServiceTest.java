package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbIssuesMilestone;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.azureDevops.models.Iteration;
import io.levelops.integrations.azureDevops.models.Project;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IssuesMilestoneServiceTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static IssuesMilestoneService issuesMilestoneService;
    private static List<DbIssuesMilestone> dbIssuesMilestones;
    private static UserIdentityService userIdentityService;
    private static WorkItemTimelineService workItemTimelineService;
    static String integrationId = "";

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);

        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        dataSource.getConnection().prepareStatement("DROP SCHEMA IF EXISTS " + company + " CASCADE").execute();

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);

        WorkItemTestUtils.TestDbs testDbs = WorkItemTestUtils.initDbServices(dataSource, company);
        integrationId = testDbs.getIntegrationService().insert(company, Integration.builder()
                .application("azure_devops")
                .name("issue mgmt test")
                .status("enabled")
                .build());

        workItemTimelineService = new WorkItemTimelineService(dataSource);
        workItemTimelineService.ensureTableExistence(company);

        issuesMilestoneService = new IssuesMilestoneService(dataSource);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        WorkItemsReportService workItemsReportService = new WorkItemsReportService(dataSource, workItemFieldsMetaService);
        WorkItemsStageTimesReportService workItemsStageTimesReportService = new WorkItemsStageTimesReportService(dataSource, workItemFieldsMetaService);
        WorkItemsAgeReportService workItemsAgeReportService = new WorkItemsAgeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService = new WorkItemsResolutionTimeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsResponseTimeReportService workItemsResponseTimeReportService  = new WorkItemsResponseTimeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsService workItemsService = new WorkItemsService(dataSource, workItemsReportService,
                workItemsStageTimesReportService, workItemsAgeReportService, workItemsResolutionTimeReportService,
                workItemsResponseTimeReportService, null, null, null, null, null,
                workItemsFirstAssigneeReportService, workItemFieldsMetaService, null);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        IntegrationService integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        integrationService.insert(company, Integration.builder()
                .application("azure_devops")
                .name("Azure-Devops-test")
                .status("enabled")
                .build());
        workItemsService.ensureTableExistence(company);
        issuesMilestoneService.ensureTableExistence(company);

        String input = ResourceUtils.getResourceAsString("json/databases/azure_devops_iterations.json");
        List<Iteration> iterations = m.readValue(input,
                m.getTypeFactory().constructParametricType(List.class, Iteration.class));
        dbIssuesMilestones = new ArrayList<>();
        iterations.forEach(iteration -> dbIssuesMilestones.add(DbIssuesMilestone
                .fromAzureDevOpsIteration("1", Project.builder().id("71737302-3511-4626-a89f-585fe0674cef").name("project-test-4").build(), iteration)));
        for (DbIssuesMilestone dbIssuesMilestone : dbIssuesMilestones) {
            issuesMilestoneService.insert(company, dbIssuesMilestone);
        }
    }

    @Test
    public void testGetMilestones() throws SQLException {
        for (DbIssuesMilestone dbIssuesMilestone : dbIssuesMilestones) {
            Optional<DbIssuesMilestone> milestone = issuesMilestoneService
                    .getMilestone(company, "1", dbIssuesMilestone.getFieldType(), dbIssuesMilestone.getFieldValue());
            Assert.assertNotNull(milestone);
            Assert.assertTrue(milestone.isPresent());
            validateMilestonesEqual(dbIssuesMilestone, milestone.get());
        }
    }

    private void validateMilestonesEqual(DbIssuesMilestone actual, DbIssuesMilestone expected) {
        Assert.assertEquals(actual.getFieldType(), expected.getFieldType());
        Assert.assertEquals(actual.getFieldValue(), expected.getFieldValue());
        Assert.assertEquals(actual.getParentFieldValue(), expected.getParentFieldValue());
        Assert.assertEquals(actual.getName(), expected.getName());
        Assert.assertEquals(actual.getIntegrationId(), expected.getIntegrationId());
        Assert.assertEquals(actual.getProjectId(), expected.getProjectId());
        Assert.assertEquals(actual.getState(), expected.getState());
        Assert.assertEquals(actual.getStartDate(), expected.getStartDate());
        Assert.assertEquals(actual.getEndDate(), expected.getEndDate());
        Assert.assertEquals(actual.getCompletedAt(), expected.getCompletedAt());
    }
}
