package io.levelops.commons.databases.services.dev_productivity.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureResponse;
import io.levelops.commons.databases.models.database.dev_productivity.IntegrationUserDetails;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.WorkItemFieldsMetaService;
import io.levelops.commons.databases.services.WorkItemTimelineService;
import io.levelops.commons.databases.services.WorkItemsAgeReportService;
import io.levelops.commons.databases.services.WorkItemsFirstAssigneeReportService;
import io.levelops.commons.databases.services.WorkItemsPrioritySLAService;
import io.levelops.commons.databases.services.WorkItemsReportService;
import io.levelops.commons.databases.services.WorkItemsResolutionTimeReportService;
import io.levelops.commons.databases.services.WorkItemsResponseTimeReportService;
import io.levelops.commons.databases.services.WorkItemsService;
import io.levelops.commons.databases.services.WorkItemsStageTimesReportService;
import io.levelops.commons.databases.services.dev_productivity.JiraFeatureHandlerService;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.models.IntegrationType;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorkItemFeatureHandlersTest {
    public static final String COMPANY = "test";
    private static WorkItemsService workItemService;
    private static JiraIssueService jiraIssueService;
    private static IntegrationService integrationService;
    private static WorkItemsStageTimesReportService workItemsStageTimesReportService;
    private static WorkItemsReportService workItemsReportService;
    private static WorkItemsAgeReportService workItemsAgeReportService;
    private static WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService;
    private static WorkItemsResponseTimeReportService workItemsResponseTimeReportService;
    private static TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    private static JiraFilterParser jiraFilterParser;
    private static DataSource dataSource;
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static List<DbWorkItem> dbWorkItems = new ArrayList<>();
    private static Date currentTime;
    private static Long ingestedAt;
    private static String integrationId;
    private static List<DbWorkItem> workItems;
    private static IssueManagementFeatureHandler issueManagementFeaturehandler;
    private static UserIdentityService userIdentityService;
    private static JiraFeatureHandlerService jiraFeatureHandlerService;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(COMPANY);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(COMPANY);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        workItemsReportService = new WorkItemsReportService(dataSource, workItemFieldsMetaService);
        workItemsStageTimesReportService = new WorkItemsStageTimesReportService(dataSource, workItemFieldsMetaService);
        workItemsAgeReportService = new WorkItemsAgeReportService(dataSource, workItemFieldsMetaService);
        workItemsResolutionTimeReportService = new WorkItemsResolutionTimeReportService(dataSource, workItemFieldsMetaService);
        workItemsResponseTimeReportService = new WorkItemsResponseTimeReportService(dataSource, workItemFieldsMetaService);
        WorkItemsPrioritySLAService workItemsPrioritySLAService = new WorkItemsPrioritySLAService(dataSource);
        WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService = new WorkItemsFirstAssigneeReportService(dataSource, workItemFieldsMetaService);
        workItemService = new WorkItemsService(dataSource, workItemsReportService, workItemsStageTimesReportService,
                workItemsAgeReportService, workItemsResolutionTimeReportService, workItemsResponseTimeReportService,
                null, null, workItemsPrioritySLAService, null,
                null, workItemsFirstAssigneeReportService, workItemFieldsMetaService, null);
        workItemService.ensureTableExistence(COMPANY);
        workItemsPrioritySLAService.ensureTableExistence(COMPANY);
        WorkItemTimelineService workItemTimelineService = new WorkItemTimelineService(dataSource);
        workItemTimelineService.ensureTableExistence(COMPANY);
        workItemFieldsMetaService.ensureTableExistence(COMPANY);
        ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource,DefaultObjectMapper.get());
        ticketCategorizationSchemeDatabaseService.ensureTableExistence(COMPANY);

        integrationId = integrationService.insert(COMPANY, Integration.builder()
                .application("issue_mgmt")
                .name("issue mgmt test")
                .status("enabled")
                .build());

        currentTime = new Date();
        ingestedAt = DateUtils.truncate(currentTime, Calendar.DATE);

        workItems = List.of(getWorkItem("LEV-1", toTimeStamp(1629347837l) , 5, "Low", "Done","BUG"),
                getWorkItem("LEV-2", toTimeStamp(1629347837l) , 5, "Low", "Done","BUG"),
                getWorkItem("LEV-3", toTimeStamp(1629347906l) , 5, "Low", "Done","BUG"),
                getWorkItem("LEV-4", toTimeStamp(1629347906l) , 5, "Low", "Done","BUG"),
                getWorkItem("LEV-5", toTimeStamp(1630685288l) , 5, "Low", "Done","BUG"),
                getWorkItem("LEV-6", toTimeStamp(1630685288l) , 5, "Low", "Done","BUG"),
                getWorkItem("LEV-7", toTimeStamp(1630685309l) , 5, "Low", "Done","BUG"),
                getWorkItem("LEV-8", toTimeStamp(1630685309l) , 5, "Low", "Done","BUG"),
                getWorkItem("LEV-9", toTimeStamp(1630685309l) , 5, "Low", "Done","BUG"),
                getWorkItem("LEV-10", toTimeStamp(1631559873l) , 5, "Low", "Done","BUG"),
                getWorkItem("LEV-11", toTimeStamp(1631559873l) , 5, "Low", "Done","BUG"),
                getWorkItem("LEV-12", toTimeStamp(1631774210l) , 5, "Low", "Done","BUG"));

        workItems.forEach(workItem -> {
            try {
                workItemService.insert(COMPANY,workItem);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        jiraFeatureHandlerService = new JiraFeatureHandlerService(dataSource,null, null);

        issueManagementFeaturehandler = new IssueManagementFeatureHandler(jiraIssueService,workItemService,jiraFilterParser,ticketCategorizationSchemeDatabaseService, jiraFeatureHandlerService, null, List.of("test"), null);
    }

    private static Timestamp toTimeStamp(long l) {
        return DateUtils.toTimestamp(DateUtils.fromEpochSecond(l));
    }

    private static DbWorkItem getWorkItem(String key, Timestamp issueResolvedAt, int storyPoints, String priority, String statusCategory, String issueType) {
        return DbWorkItem.builder()
                .workItemId(key)
                .assignee("shiva")
                .integrationId(integrationId)
                .workItemResolvedAt(issueResolvedAt)
                .status("DONE")
                .statusCategory(statusCategory)
                .ingestedAt(ingestedAt)
                .project("test-project")
                .descSize(1)
                .customFields(Map.of())
                .priority(priority)
                .reporter("xyz")
                .workItemType(issueType)
                .hops(0)
                .bounces(0)
                .numAttachments(0)
                .workItemCreatedAt(toTimeStamp(18870l))
                .workItemUpdatedAt(issueResolvedAt)
                .build();
    }

    @Test
    public void test() throws SQLException, IOException {

        DevProductivityFilter devProductivityFilter = DevProductivityFilter.builder()
                .timeRange(ImmutablePair.of(1629347800l,1631774300l))
                .build();

        OrgUserDetails orgUser = OrgUserDetails.builder()
                .orgUserId(UUID.randomUUID())
                .fullName("shiva")
                .IntegrationUserDetailsList(List.of(
                        IntegrationUserDetails.builder()
                                .integrationId(1).integrationType(IntegrationType.AZURE_DEVOPS)
                                .cloudId("shiva")
                                .build()))
                .build();

        Map<String,Long> latestIngestedAtByIntegrationId = Map.of(integrationId,ingestedAt);

        DevProductivityProfile.Feature feature = DevProductivityProfile.Feature.builder()
                .maxValue(10l)
                .lowerLimitPercentage(25)
                .upperLimitPercentage(75)
                .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_BUGS_FIXED_PER_MONTH)
                .params(Map.of("aggInterval",List.of("day")))
                .build();
        FeatureResponse response = issueManagementFeaturehandler.calculateFeature(COMPANY, 0, feature, Map.of(), devProductivityFilter, orgUser, latestIngestedAtByIntegrationId, null);
        Assertions.assertThat(response.getMean()).isEqualTo(0.41);

        feature = DevProductivityProfile.Feature.builder()
                .maxValue(10l)
                .lowerLimitPercentage(25)
                .upperLimitPercentage(75)
                .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_BUGS_FIXED_PER_MONTH)
                .params(Map.of("aggInterval",List.of("week")))
                .build();

        response = issueManagementFeaturehandler.calculateFeature(COMPANY, 0, feature, Map.of(), devProductivityFilter, orgUser, latestIngestedAtByIntegrationId, null);
        Assertions.assertThat(response.getMean()).isEqualTo(2.4);

        feature = DevProductivityProfile.Feature.builder()
                .maxValue(10l)
                .lowerLimitPercentage(25)
                .upperLimitPercentage(75)
                .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_BUGS_FIXED_PER_MONTH)
                //.params(Map.of("aggInterval",List.of("month")))
                .build();
        response = issueManagementFeaturehandler.calculateFeature(COMPANY, 0, feature, Map.of(), devProductivityFilter, orgUser, latestIngestedAtByIntegrationId, null);
        Assertions.assertThat(response.getMean()).isEqualTo(6.0);
    }

}
