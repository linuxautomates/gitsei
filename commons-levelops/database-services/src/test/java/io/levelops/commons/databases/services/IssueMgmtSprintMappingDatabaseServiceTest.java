package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbIssueMgmtSprintMapping;
import io.levelops.commons.databases.issue_management.DbIssuesMilestone;
import io.levelops.commons.databases.issue_management.DbWorkItemHistory;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.utils.WorkItemUtils;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.data.util.Pair;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class IssueMgmtSprintMappingDatabaseServiceTest {

    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static final String SUB_TASK_ISSUE_TYPE = "SUB-TASK";
    private static final String CLOSED_STATUS_CATEGORY = "Closed";
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static IssueMgmtSprintMappingDatabaseService sprintMappingService;
    private static IssuesMilestoneService issuesMilestoneService;
    private static WorkItemTimelineService workItemsTimelineService;
    private static WorkItemsService workItemService;
    private static String integrationId;
    private static Date currentTime;
    private static UserIdentityService userIdentityService;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        sprintMappingService = new IssueMgmtSprintMappingDatabaseService(dataSource);
        issuesMilestoneService = new IssuesMilestoneService(dataSource);
        workItemsTimelineService = new WorkItemTimelineService(dataSource);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        WorkItemsPrioritySLAService workItemsPrioritySLAService = new WorkItemsPrioritySLAService(dataSource);
        IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService = new IssueMgmtSprintMappingDatabaseService(dataSource);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        workItemService = new WorkItemsService(dataSource, null, null,
                null, null, null,
                null, null, workItemsPrioritySLAService, null, null,
                workItemsFirstAssigneeReportService, workItemFieldsMetaService, null);
        IntegrationService integrationService = new IntegrationService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        integrationId = integrationService.insert(company, Integration.builder()
                .application("azure_devops")
                .name("Azure-Devops-test")
                .status("enabled")
                .build());
        workItemService.ensureTableExistence(company);
        workItemsPrioritySLAService.ensureTableExistence(company);
        issueMgmtSprintMappingDatabaseService.ensureTableExistence(company);
        sprintMappingService.ensureTableExistence(company);
        issuesMilestoneService.ensureTableExistence(company);
        workItemsTimelineService.ensureTableExistence(company);
        currentTime = new Date();

        String input = ResourceUtils.getResourceAsString("json/databases/azure_devops_workitem_history_2.json");
        PaginatedResponse<EnrichedProjectData> data = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, EnrichedProjectData.class));
        data.getResponse().getRecords().forEach(projectData -> {
            Project project = projectData.getProject();
            projectData.getWorkItemHistories().stream()
                    .sorted(Comparator.comparing(WorkItemUtils::getChangedDateFromHistory))
                    .forEach(workItemHistory -> {
                        List<DbWorkItemHistory> dbWorkItemHistories = DbWorkItemHistory
                                .fromAzureDevopsWorkItemHistories(String.valueOf(integrationId), workItemHistory, new Date());
                        dbWorkItemHistories.forEach(dbWorkItemHistory -> {
                            try {
                                Optional<DbWorkItemHistory> lastEvent = workItemsTimelineService
                                        .getLastEvent(company, Integer.valueOf(integrationId), dbWorkItemHistory.getFieldType(),
                                                dbWorkItemHistory.getWorkItemId());
                                if (lastEvent.isPresent()) {
                                    String changedDate = workItemHistory.getFields().getChangedDate().getNewValue();
                                    DbWorkItemHistory lastEventUpdatedHistory = lastEvent.get().toBuilder()
                                            .endDate(Timestamp.from(DateUtils.parseDateTime(changedDate)))
                                            .build();
                                    workItemsTimelineService.updateEndDate(company, lastEventUpdatedHistory);//update
                                }
                                workItemsTimelineService.insert(company, dbWorkItemHistory);//insert except endTime.
                            } catch (Exception ex) {
                                log.warn("setupAzureDevopsWorkItems: error inserting project: " + workItemHistory.getId()
                                        + " for project id: " + project.getId(), ex);
                            }
                        });
                    });
        });
        String iterationsResourcePath = "json/databases/azure_devops_iterations_2.json";
        IssueMgmtTestUtil.setupIterations(company, "1", issuesMilestoneService, iterationsResourcePath);
        String workitemsResourcePath = "json/databases/azure_devops_work_items_2.json";
        IssueMgmtTestUtil.setupWorkItems(company, "1", workItemService,
                workItemsTimelineService, issuesMilestoneService, issueMgmtSprintMappingDatabaseService, currentTime,
                workitemsResourcePath, List.of(), List.of(), List.of(), userIdentityService);
    }

    @Test
    public void testDataInserted() throws SQLException {
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "110", "project-test-9\\Sprint 1")
                .getSprintId()).isEqualTo("project-test-9\\Sprint 1");
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "110", "project-test-9\\Sprint 1")
                .getPlanned()).isEqualTo(false);
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "110", "project-test-9\\Sprint 1")
                .getDelivered()).isEqualTo(false);
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "110", "project-test-9\\Sprint 1")
                .getIgnorableWorkitemType()).isEqualTo(false);
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "110", "project-test-9\\Sprint 1")
                .getAddedAt()).isEqualTo(1626345377L);
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "110", "project-test-9\\Sprint 1")
                .getOutsideOfSprint()).isEqualTo(false);
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "110", "project-test-9\\Sprint 1")
                .getStoryPointsDelivered()).isEqualTo(0);
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "110", "project-test-9\\Sprint 1")
                .getStoryPointsPlanned()).isEqualTo(0);

        Assertions.assertThat(sprintMappingService.get(company, integrationId, "108", "Agile-Project\\Test")
                .getSprintId()).isEqualTo("Agile-Project\\Test");
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "108", "Agile-Project\\Test")
                .getAddedAt()).isEqualTo(1624360912L);
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "108", "Agile-Project\\Test")
                .getPlanned()).isEqualTo(true);
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "108", "Agile-Project\\Test")
                .getDelivered()).isEqualTo(false);
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "108", "Agile-Project\\Test")
                .getOutsideOfSprint()).isEqualTo(true);
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "108", "Agile-Project\\Test")
                .getIgnorableWorkitemType()).isEqualTo(false);
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "108", "Agile-Project\\Test")
                .getStoryPointsPlanned()).isEqualTo(100);
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "108", "Agile-Project\\Test")
                .getStoryPointsDelivered()).isEqualTo(5);

        Assertions.assertThat(sprintMappingService.get(company, integrationId, "143", "Agile-Project\\Iteration 3")
                .getSprintId()).isEqualTo("Agile-Project\\Iteration 3");
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "143", "Agile-Project\\Iteration 3")
                .getAddedAt()).isEqualTo(1629111088L);
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "143", "Agile-Project\\Iteration 3")
                .getPlanned()).isEqualTo(false);
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "143", "Agile-Project\\Iteration 3")
                .getDelivered()).isEqualTo(false);
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "143", "Agile-Project\\Iteration 3")
                .getOutsideOfSprint()).isEqualTo(true);
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "143", "Agile-Project\\Iteration 3")
                .getIgnorableWorkitemType()).isEqualTo(false);
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "143", "Agile-Project\\Iteration 3")
                .getStoryPointsPlanned()).isEqualTo(5);
        Assertions.assertThat(sprintMappingService.get(company, integrationId, "143", "Agile-Project\\Iteration 3")
                .getStoryPointsDelivered()).isEqualTo(2);
    }

    @Test
    public void listTest() throws SQLException {
        issuesMilestoneService.insert(company, DbIssuesMilestone.builder()
                .fieldType("sprint")
                .fieldValue("s1")
                .integrationId(Integer.valueOf(integrationId))
                .name("Iteration 3")
                .projectId("1234")
                .state("done")
                .parentFieldValue("project-test-9")
                .completedAt(new Timestamp(0))
                .endDate(new Timestamp(10))
                .startDate(new Timestamp(0))
                .build());
        issuesMilestoneService.insert(company, DbIssuesMilestone.builder()
                .fieldType("sprint")
                .fieldValue("s2")
                .integrationId(Integer.valueOf(integrationId))
                .name("Sprint 1")
                .projectId("123273")
                .state("past")
                .parentFieldValue("Agile-Project")
                .completedAt(new Timestamp(15))
                .endDate(new Timestamp(120))
                .startDate(new Timestamp(1))
                .build());

        List<Pair<DbIssueMgmtSprintMapping, DbIssuesMilestone>> actualSprintMappingsAndMilestone =  sprintMappingService.list(company, List.of("110", "143"), List.of(integrationId,integrationId)).getRecords();

        Assertions.assertThat(actualSprintMappingsAndMilestone.size()).isEqualTo(4);
        Set<String> workitemIds = new HashSet<>();
        actualSprintMappingsAndMilestone.forEach(dbIssueMgmtSprintMappingAndMilestone -> workitemIds.add(dbIssueMgmtSprintMappingAndMilestone.getFirst().getWorkitemId()));
        Assertions.assertThat(workitemIds).isEqualTo(Set.of("110", "143"));
        Assertions.assertThat(actualSprintMappingsAndMilestone.stream().map(Pair::getSecond).map(DbIssuesMilestone::getFieldType).collect(Collectors.toList()))
                .isEqualTo(List.of("sprint", "sprint", "sprint", "sprint"));
        Assertions.assertThat(actualSprintMappingsAndMilestone.stream().map(Pair::getSecond).map(DbIssuesMilestone::getParentFieldValue).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("project-test-9", "project-test-9", "Agile-Project", "Agile-Project");
    }
}
