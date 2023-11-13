package io.levelops.commons.databases.services.business_alignment;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme.ActiveWork;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme.Goal;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme.Goals;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme.IssuesActiveWork;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme.TicketCategorization;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme.TicketCategorizationConfig;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.response.BaAllocation;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IssueMgmtSprintMappingDatabaseService;
import io.levelops.commons.databases.services.IssueMgmtTestUtil;
import io.levelops.commons.databases.services.IssuesMilestoneService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.WorkItemFieldsMetaService;
import io.levelops.commons.databases.services.WorkItemTimelineService;
import io.levelops.commons.databases.services.WorkItemsPrioritySLAService;
import io.levelops.commons.databases.services.WorkItemsService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.web.exceptions.NotFoundException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.database.TicketCategorizationScheme.Uncategorized.builder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Log4j2
public class BaWorkItemsAggsActiveWorkTest {

    private static final String company = "test";
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static BaWorkItemsAggsDatabaseService baWorkItemsAggsDatabaseService;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        IntegrationService integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        UserIdentityService userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        WorkItemsPrioritySLAService workItemsPrioritySLAService = new WorkItemsPrioritySLAService(dataSource);
        WorkItemsService workItemsService = new WorkItemsService(dataSource, null, null,
                null, null, null,
                null, null, workItemsPrioritySLAService,
                null, null, null, workItemFieldsMetaService, null);
        WorkItemTimelineService workItemTimelineService = new WorkItemTimelineService(dataSource);
        IssuesMilestoneService issuesMilestoneService = new IssuesMilestoneService(dataSource);
        IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService = new IssueMgmtSprintMappingDatabaseService(dataSource);

        GitRepositoryService repositoryService = new GitRepositoryService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        Date currentTime = DateUtils.truncate(new Date(Instant.parse("2021-05-05T15:00:00-08:00").getEpochSecond()), Calendar.DATE);

        String azureDevopsIntegrationId = integrationService.insert(company, Integration.builder()
                .application("azure_devops")
                .name("azure test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        workItemsService.ensureTableExistence(company);
        workItemTimelineService.ensureTableExistence(company);
        issueMgmtSprintMappingDatabaseService.ensureTableExistence(company);
        issuesMilestoneService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        workItemsPrioritySLAService.ensureTableExistence(company);

        BaWorkItemsAggsQueryBuilder baWorkItemsAggsQueryBuilder = new BaWorkItemsAggsQueryBuilder(workItemFieldsMetaService);
        BaWorkItemsAggsActiveWorkQueryBuilder baWorkItemsAggsActiveWorkQueryBuilder = new BaWorkItemsAggsActiveWorkQueryBuilder(baWorkItemsAggsQueryBuilder);
        TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService = Mockito.mock(TicketCategorizationSchemeDatabaseService.class);
        when(ticketCategorizationSchemeDatabaseService.get(eq(company), anyString())).thenReturn(Optional.of(TicketCategorizationScheme.builder()
                .config(TicketCategorizationConfig.builder()
                        .uncategorized(builder()
                                .goals(Goals.builder()
                                        .enabled(true)
                                        .idealRange(new Goal(20, 30))
                                        .acceptableRange(new Goal(20, 30))
                                        .build())
                                .build())
                        .categories(Map.of(
                                "cat1", TicketCategorization.builder()
                                        .name("cat1")
                                        .goals(Goals.builder()
                                                .enabled(true)
                                                .idealRange(new Goal(20, 30))
                                                .acceptableRange(new Goal(20, 45))
                                                .build())
                                        .build(),
                                "cat2", TicketCategorization.builder()
                                        .name("cat2")
                                        .goals(Goals.builder()
                                                .enabled(true)
                                                .idealRange(new Goal(20, 30))
                                                .acceptableRange(new Goal(20, 30))
                                                .build())
                                        .build()))
                        .activeWork(ActiveWork.builder()
                                .issues(IssuesActiveWork.builder()
                                        .assigned(true)
                                        .activeSprints(true)
                                        .inProgress(true)
                                        .build())
                                .build())
                        .build())
                .build()));
        baWorkItemsAggsDatabaseService = new BaWorkItemsAggsDatabaseService(dataSource, baWorkItemsAggsQueryBuilder,
                baWorkItemsAggsActiveWorkQueryBuilder, null, ticketCategorizationSchemeDatabaseService);

        String workItemsResource = "json/databases/azure_devops_work_items_3.json";
        String timelinesResource = "json/databases/azure_devops_workitem_history_3.json";
        String iterationsResource = "json/databases/azure_devops_iterations_2.json";
        IssueMgmtTestUtil.setupHistories(company, azureDevopsIntegrationId, workItemTimelineService, timelinesResource);
        IssueMgmtTestUtil.setupIterations(company, azureDevopsIntegrationId, issuesMilestoneService, iterationsResource);
        IssueMgmtTestUtil.setupWorkItems(company, azureDevopsIntegrationId, workItemsService,
                workItemTimelineService, issuesMilestoneService, issueMgmtSprintMappingDatabaseService, currentTime,
                workItemsResource, List.of(), List.of(), List.of(), userIdentityService);
    }

    @Test
    public void testActiveWork() throws NotFoundException {

        // -- across ticket categories
        DbListResponse<DbAggregationResult> response = baWorkItemsAggsDatabaseService.calculateTicketCountActiveWork(
                company,
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.TICKET_CATEGORY,
                WorkItemsFilter.builder()
                        .ticketCategorizationSchemeId("This is mocked")
                        .ticketCategorizationFilters(List.of(
                                WorkItemsFilter.TicketCategorizationFilter.builder()
                                        .index(0)
                                        .name("cat1")
                                        .filter(WorkItemsFilter.builder().workItemTypes(List.of("Bug")).build())
                                        .build(),
                                WorkItemsFilter.TicketCategorizationFilter.builder()
                                        .index(1)
                                        .name("cat2")
                                        .filter(WorkItemsFilter.builder().workItemTypes(List.of("Task")).build())
                                        .build()
                        ))
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .excludeProjects(List.of())
                        .build(),
                null);

        DbAggregationResult expected = DbAggregationResult.builder()
                .alignmentScore(2)
                .percentageScore(0.6666667f)
                .categoryAllocations(Map.of(
                        "Other", BaAllocation.builder()
                                .alignmentScore(1)
                                .percentageScore(0.0f)
                                .allocation(0.5f)
                                .effort(5)
                                .totalEffort(10)
                                .build(),
                        "cat2", BaAllocation.builder()
                                .alignmentScore(3)
                                .percentageScore(1.0f)
                                .allocation(0.3f)
                                .effort(3)
                                .totalEffort(10)
                                .build(),
                        "cat1", BaAllocation.builder()
                                .alignmentScore(3)
                                .percentageScore(1.0f)
                                .allocation(0.2f)
                                .effort(2)
                                .totalEffort(10)
                                .build()))
                .build();
        assertThat(response.getRecords())
                .overridingErrorMessage("Expected: " + DefaultObjectMapper.writeAsPrettyJson(expected) + "\nbut got: " + DefaultObjectMapper.writeAsPrettyJson(response.getRecords()))
                .containsExactlyInAnyOrder(expected);

        // -- across assignees
        response = baWorkItemsAggsDatabaseService.calculateTicketCountActiveWork(
                company,
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.ASSIGNEE,
                WorkItemsFilter.builder()
                        .ticketCategorizationSchemeId("This is mocked")
                        .ticketCategorizationFilters(List.of(
                                WorkItemsFilter.TicketCategorizationFilter.builder()
                                        .index(0)
                                        .name("cat1")
                                        .filter(WorkItemsFilter.builder().workItemTypes(List.of("Bug")).build())
                                        .build(),
                                WorkItemsFilter.TicketCategorizationFilter.builder()
                                        .index(1)
                                        .name("cat2")
                                        .filter(WorkItemsFilter.builder().workItemTypes(List.of("Task")).build())
                                        .build()
                        ))
                        .build(),
                WorkItemsMilestoneFilter.builder()
                        .excludeProjects(List.of())
                        .build(),
                null);


        DbAggregationResult unassigned = DbAggregationResult.builder()
                .key("_UNASSIGNED_")
                .alignmentScore(1)
                .percentageScore(0.0f)
                .categoryAllocations(Map.of(
                        "cat2", BaAllocation.builder()
                                .alignmentScore(1)
                                .percentageScore(0.0f)
                                .allocation(0.0f)
                                .effort(0)
                                .totalEffort(2)
                                .build(),
                        "cat1", BaAllocation.builder()
                                .alignmentScore(1)
                                .percentageScore(0.0f)
                                .allocation(0.5f)
                                .effort(1)
                                .totalEffort(2)
                                .build(),
                        "Other", BaAllocation.builder()
                                .alignmentScore(1)
                                .percentageScore(0.0f)
                                .allocation(0.5f)
                                .effort(1)
                                .totalEffort(2)
                                .build()))
                .build();
        DbAggregationResult gaurav = DbAggregationResult.builder()
                .key("gaurav@cognitree.com")
                .alignmentScore(1)
                .percentageScore(0.0f)
                .categoryAllocations(Map.of(
                        "cat2", BaAllocation.builder()
                                .alignmentScore(1)
                                .percentageScore(0.0f)
                                .allocation(0.0f)
                                .effort(0)
                                .totalEffort(1)
                                .build(),
                        "cat1", BaAllocation.builder()
                                .alignmentScore(1)
                                .percentageScore(0.0f)
                                .allocation(1.0f)
                                .effort(1)
                                .totalEffort(1)
                                .build(),
                        "Other", BaAllocation.builder()
                                .alignmentScore(1)
                                .percentageScore(0.0f)
                                .allocation(0.0f)
                                .effort(0)
                                .totalEffort(1)
                                .build()))
                .build();
        DbAggregationResult praveen = DbAggregationResult.builder()
                .key("praveen@cognitree.com")
                .alignmentScore(1)
                .percentageScore(0.0f)
                .categoryAllocations(Map.of(
                        "cat2", BaAllocation.builder()
                                .alignmentScore(1)
                                .percentageScore(0.0f)
                                .allocation(0.0f)
                                .effort(0)
                                .totalEffort(1)
                                .build(),
                        "cat1", BaAllocation.builder()
                                .alignmentScore(1)
                                .percentageScore(0.0f)
                                .allocation(0.0f)
                                .effort(0)
                                .totalEffort(1)
                                .build(),
                        "Other", BaAllocation.builder()
                                .alignmentScore(1)
                                .percentageScore(0.0f)
                                .allocation(1.0f)
                                .effort(1)
                                .totalEffort(1)
                                .build()))
                .build();
        DbAggregationResult srinath = DbAggregationResult.builder()
                .key("srinath.chandrashekhar")
                .alignmentScore(1)
                .percentageScore(0.0f)
                .categoryAllocations(Map.of(
                        "cat2", BaAllocation.builder()
                                .alignmentScore(1)
                                .percentageScore(0.0f)
                                .allocation(0.5f)
                                .effort(3)
                                .totalEffort(6)
                                .build(),
                        "cat1", BaAllocation.builder()
                                .alignmentScore(1)
                                .percentageScore(0.0f)
                                .allocation(0.0f)
                                .effort(0)
                                .totalEffort(6)
                                .build(),
                        "Other", BaAllocation.builder()
                                .alignmentScore(1)
                                .percentageScore(0.0f)
                                .allocation(0.5f)
                                .effort(3)
                                .totalEffort(6)
                                .build()))
                .build();
        assertThat(response.getRecords()).hasSize(4);
        Map<String, DbAggregationResult> map = response.getRecords().stream().collect(Collectors.toMap(DbAggregationResult::getKey, r -> r));
        assertThat(map.get("srinath.chandrashekhar"))
                .overridingErrorMessage("Expected: " + DefaultObjectMapper.writeAsPrettyJson(srinath) + "\nbut got: " + DefaultObjectMapper.writeAsPrettyJson(map.get("srinath.chandrashekhar")))
                .isEqualTo(srinath);
        assertThat(map.get("praveen@cognitree.com"))
                .overridingErrorMessage("Expected: " + DefaultObjectMapper.writeAsPrettyJson(srinath) + "\nbut got: " + DefaultObjectMapper.writeAsPrettyJson(map.get("praveen@cognitree.com")))
                .isEqualTo(praveen);
        assertThat(map.get("gaurav@cognitree.com"))
                .overridingErrorMessage("Expected: " + DefaultObjectMapper.writeAsPrettyJson(srinath) + "\nbut got: " + DefaultObjectMapper.writeAsPrettyJson(map.get("gaurav@cognitree.com")))
                .isEqualTo(gaurav);
        assertThat(map.get("_UNASSIGNED_"))
                .overridingErrorMessage("Expected: " + DefaultObjectMapper.writeAsPrettyJson(srinath) + "\nbut got: " + DefaultObjectMapper.writeAsPrettyJson(map.get("_UNASSIGNED_")))
                .isEqualTo(unassigned);

    }
}
