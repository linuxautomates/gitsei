package io.levelops.commons.databases.services.dev_productivity.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.database.dev_productivity.*;
import io.levelops.commons.databases.models.database.jira.DbJiraAssignee;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.business_alignment.BaJiraAggsQueryBuilder;
import io.levelops.commons.databases.services.dev_productivity.JiraFeatureHandlerService;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.models.IntegrationType;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Log4j2
public class JiraFeatureHandlersTest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static JiraIssueService jiraIssueService;
    private static TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    private static TagsService tagsService;
    private static TagItemDBService tagItemDBService;
    private static IntegrationService integrationService;
    private static List<DbJiraIssue> jiraIssues;
    private static JiraFilterParser jiraFilterParser;
    private static IssueManagementFeatureHandler issueManagementHandler;
    private static long ingestedAt;
    private static String schemeId;
    private static String categoryId1;
    private static String categoryId2;
    private static JiraFeatureHandlerService jiraFeatureHandlerService;
    private static UserIdentityService userIdentityService;
    private static long TIME_RANGE_LEFT = 1629347800l;
    private static long TIME_RANGE_RIGHT = 1631774300l;


    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;

        dataSource =  DatabaseTestUtils.setUpDataSource(pg, company);
        tagsService = new TagsService(dataSource);
        tagsService.ensureTableExistence(company);
        tagItemDBService = new TagItemDBService(dataSource);
        tagItemDBService.ensureTableExistence(company);

        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        userIdentityService = jiraTestDbs.getUserIdentityService();
        jiraIssueService = jiraTestDbs.getJiraIssueService();
        integrationService = jiraTestDbs.getIntegrationService();
        ticketCategorizationSchemeDatabaseService = jiraTestDbs.getTicketCategorizationSchemeDatabaseService();
        integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test")
                .status("enabled")
                .build());

        ingestedAt = io.levelops.commons.dates.DateUtils.truncate(new Date(), Calendar.DATE);

        schemeId = ticketCategorizationSchemeDatabaseService.insert("test", TicketCategorizationScheme.builder()
                .name("scheme-1")
                .config(TicketCategorizationScheme.TicketCategorizationConfig.builder()
                        .description("desc scheme-1")
                        .integrationType("jira")
                        .categories(Map.of("1", TicketCategorizationScheme.TicketCategorization.builder()
                                .index(1)
                                .name("category 1")
                                .description("desc cat1")
                                .filter(Map.of("priorities", List.of("High","Highest")))
                                .build(),"2",TicketCategorizationScheme.TicketCategorization.builder()
                                .index(2)
                                .name("category 2")
                                .description("desc cat2")
                                .filter(Map.of("priorities", List.of("Low","Medium")))
                                .build()))
                        .build())
                .build());
        TicketCategorizationScheme scheme = ticketCategorizationSchemeDatabaseService.get("test",schemeId).get();
        categoryId1 = scheme.getConfig().getCategories().get("1").getId();
        categoryId2 = scheme.getConfig().getCategories().get("2").getId();

        jiraIssues = List.of(getDBJiraIssue("LEV-123", 1629347837, null, "High", "Done","BUG",getAssigneeList("LEV-123"),getStatusList("LEV-123")),
                getDBJiraIssue("LEV-456", 1629347906, 6, "Medium", "Done","BUG",getAssigneeList("LEV-456"),getStatusList("LEV-456")),
                getDBJiraIssue("LEV-789", 1630685288, null,"High", "Done","BUG",getAssigneeList("LEV-789"),getStatusList("LEV-789")),
                getDBJiraIssue("LEV-112", 1630685309, 8,"Highest", "Done","BUG",getAssigneeList("LEV-112"),getStatusList("LEV-112")),
                getDBJiraIssue("LEV-121", 1631559873, 1, "Low", "Done","BUG",getAssigneeList("LEV-121"),getStatusList("LEV-121")),
                getDBJiraIssue("LEV-131", 1631774210,3,"Medium","Done","BUG",getAssigneeList("LEV-131"),getStatusList("LEV-131")),
                getDBJiraIssue("LEV-141", 1631774210,8,"High","Done","BUG",getAssigneeList("LEV-141"),getStatusList("LEV-141")),
                getDBJiraIssue("LEV-142", 1631774210,8,"High","Done","BUG",getAssigneeList("LEV-142"),getStatusList("LEV-142")),
                getDBJiraIssue("LEV-143", 1631774210,8,"High","Done","BUG",getAssigneeList("LEV-143"),getStatusList("LEV-143"))
        );

        jiraIssues.forEach(dbJiraIssue -> {
            try {
                jiraIssueService.insert(company,dbJiraIssue);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });

        jiraFilterParser = jiraTestDbs.getJiraFilterParser();
        BaJiraAggsQueryBuilder baJiraAggsQueryBuilder = new BaJiraAggsQueryBuilder(jiraTestDbs.getJiraIssueQueryBuilder(), jiraTestDbs.getJiraConditionsBuilder(), false);
        jiraFeatureHandlerService = new JiraFeatureHandlerService(dataSource,baJiraAggsQueryBuilder, jiraTestDbs.getJiraConditionsBuilder());
        issueManagementHandler = new IssueManagementFeatureHandler(jiraIssueService,null,jiraFilterParser,ticketCategorizationSchemeDatabaseService, jiraFeatureHandlerService, null, List.of("test"), List.of(company));
    }

    private static List<DbJiraStatus> getStatusList(String issueKey) {
        return List.of(DbJiraStatus.builder().issueKey(issueKey).integrationId("1").status("ToDo").startTime(TIME_RANGE_LEFT- TimeUnit.DAYS.toSeconds(30)).endTime(TIME_RANGE_LEFT-TimeUnit.DAYS.toSeconds(28)).build(),
                DbJiraStatus.builder().issueKey(issueKey).integrationId("1").status("In Progress").startTime(TIME_RANGE_LEFT-TimeUnit.DAYS.toSeconds(28)).endTime(TIME_RANGE_LEFT-TimeUnit.DAYS.toSeconds(25)).build(),
                DbJiraStatus.builder().issueKey(issueKey).integrationId("1").status("Ready for QA").startTime(TIME_RANGE_LEFT-TimeUnit.DAYS.toSeconds(25)).endTime(TIME_RANGE_LEFT-TimeUnit.DAYS.toSeconds(21)).build(),
                DbJiraStatus.builder().issueKey(issueKey).integrationId("1").status("In Progress").startTime(TIME_RANGE_LEFT-TimeUnit.DAYS.toSeconds(21)).endTime(TIME_RANGE_LEFT-TimeUnit.DAYS.toSeconds(18)).build(),
                DbJiraStatus.builder().issueKey(issueKey).integrationId("1").status("Ready for QA").startTime(TIME_RANGE_LEFT-TimeUnit.DAYS.toSeconds(18)).endTime(TIME_RANGE_LEFT-TimeUnit.DAYS.toSeconds(16)).build(),
                DbJiraStatus.builder().issueKey(issueKey).integrationId("1").status("Ready for Prod").startTime(TIME_RANGE_LEFT-TimeUnit.DAYS.toSeconds(16)).endTime(TIME_RANGE_LEFT-TimeUnit.DAYS.toSeconds(10)).build()
                );
    }

    private static List<DbJiraAssignee> getAssigneeList(String issueKey) {
        return List.of(DbJiraAssignee.builder().issueKey(issueKey).integrationId("1").assignee("UNASSIGNED").startTime(TIME_RANGE_LEFT- TimeUnit.DAYS.toSeconds(30)).endTime(TIME_RANGE_LEFT- TimeUnit.DAYS.toSeconds(26)).build(),
                DbJiraAssignee.builder().issueKey(issueKey).integrationId("1").assignee("ashish").startTime(TIME_RANGE_LEFT- TimeUnit.DAYS.toSeconds(26)).endTime(TIME_RANGE_LEFT- TimeUnit.DAYS.toSeconds(25)).build(),
                DbJiraAssignee.builder().issueKey(issueKey).integrationId("1").assignee("UNASSIGNED").startTime(TIME_RANGE_LEFT- TimeUnit.DAYS.toSeconds(25)).endTime(TIME_RANGE_LEFT- TimeUnit.DAYS.toSeconds(23)).build(),
                DbJiraAssignee.builder().issueKey(issueKey).integrationId("1").assignee("maxime").startTime(TIME_RANGE_LEFT- TimeUnit.DAYS.toSeconds(23)).endTime(TIME_RANGE_LEFT- TimeUnit.DAYS.toSeconds(18)).build(),
                DbJiraAssignee.builder().issueKey(issueKey).integrationId("1").assignee("ashish").startTime(TIME_RANGE_LEFT- TimeUnit.DAYS.toSeconds(18)).endTime(TIME_RANGE_LEFT- TimeUnit.DAYS.toSeconds(14)).build(),
                DbJiraAssignee.builder().issueKey(issueKey).integrationId("1").assignee("UNASSIGNED").startTime(TIME_RANGE_LEFT- TimeUnit.DAYS.toSeconds(14)).endTime(TIME_RANGE_LEFT- TimeUnit.DAYS.toSeconds(12)).build(),
                DbJiraAssignee.builder().issueKey(issueKey).integrationId("1").assignee("maxime").startTime(TIME_RANGE_LEFT- TimeUnit.DAYS.toSeconds(12)).endTime(TIME_RANGE_LEFT- TimeUnit.DAYS.toSeconds(10)).build());
    }

    @Test
    public void test() throws SQLException, IOException {

        DevProductivityFilter devProductivityFilter = DevProductivityFilter.builder()
                .timeRange(ImmutablePair.of(TIME_RANGE_LEFT,TIME_RANGE_RIGHT))
                .build();

        String ashish = userIdentityService.insertAndReturnId(company, DbScmUser.builder().integrationId("1").cloudId("ashish").displayName("ashish").originalDisplayName("ashish").build()).get();
        String siva = userIdentityService.insertAndReturnId(company, DbScmUser.builder().integrationId("1").cloudId("siva").displayName("siva").originalDisplayName("siva").build()).get();
        String maxime = userIdentityService.insertAndReturnId(company, DbScmUser.builder().integrationId("1").cloudId("maxime").displayName("maxime").originalDisplayName("maxime").build()).get();
        String viraj = userIdentityService.insertAndReturnId(company, DbScmUser.builder().integrationId("1").cloudId("viraj").displayName("viraj").originalDisplayName("viraj").build()).get();

        OrgUserDetails ashishOrg = OrgUserDetails.builder()
                .orgUserId(UUID.randomUUID())
                .fullName("ashish")
                .IntegrationUserDetailsList(List.of(
                        IntegrationUserDetails.builder()
                                .integrationUserId(UUID.fromString(ashish))
                                .integrationId(1).integrationType(IntegrationType.JIRA)
                                .displayName("ashish")
                                .cloudId("ashish")
                                .build()))
                .build();

        OrgUserDetails sivaOrg = OrgUserDetails.builder()
                .orgUserId(UUID.randomUUID())
                .fullName("siva")
                .IntegrationUserDetailsList(List.of(
                        IntegrationUserDetails.builder()
                                .integrationUserId(UUID.fromString(ashish))
                                .integrationId(1).integrationType(IntegrationType.JIRA)
                                .displayName("siva")
                                .cloudId("siva")
                                .build()))
                .build();

        OrgUserDetails maximeOrg = OrgUserDetails.builder()
                .orgUserId(UUID.randomUUID())
                .fullName("maxime")
                .IntegrationUserDetailsList(List.of(
                        IntegrationUserDetails.builder()
                                .integrationUserId(UUID.fromString(ashish))
                                .integrationId(1).integrationType(IntegrationType.JIRA)
                                .displayName("maxime")
                                .cloudId("maxime")
                                .build()))
                .build();

        OrgUserDetails virajOrg = OrgUserDetails.builder()
                .orgUserId(UUID.randomUUID())
                .fullName("viraj")
                .IntegrationUserDetailsList(List.of(
                        IntegrationUserDetails.builder()
                                .integrationUserId(UUID.fromString(ashish))
                                .integrationId(1).integrationType(IntegrationType.JIRA)
                                .displayName("viraj")
                                .cloudId("viraj")
                                .build()))
                .build();

        Map<String,Long> latestIngestedAtByIntegrationId = Map.of("1",ingestedAt);

       DevProductivityProfile.Feature feature = DevProductivityProfile.Feature.builder()
                .name("Number of Bugs fixed per month")
                .maxValue(10l)
                .lowerLimitPercentage(25)
                .upperLimitPercentage(75)
                .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_BUGS_FIXED_PER_MONTH)
                .params(Map.of("aggInterval",List.of("day")))
                .build();
        FeatureResponse response = issueManagementHandler.calculateFeature(company, 0, feature, Map.of("development_stages",List.of("In Progress","Ready for QA","Ready for Prod")), devProductivityFilter, ashishOrg, latestIngestedAtByIntegrationId, null);
        Assertions.assertThat(response.getMean()).isEqualTo(0.14);
        Assertions.assertThat(response.getName()).isEqualTo("Number of Bugs fixed per month");

        FeatureBreakDown fb = issueManagementHandler.getBreakDown(company,feature, Map.of("development_stages",List.of("In Progress","Ready for QA","Ready for Prod")), devProductivityFilter,ashishOrg,latestIngestedAtByIntegrationId,null,null, 0,5);
        Assertions.assertThat(fb.getRecords().size()).isEqualTo(5);

        fb = issueManagementHandler.getBreakDown(company,feature, Map.of("development_stages",List.of("In Progress","Ready for QA","Ready for Prod")), devProductivityFilter,ashishOrg,latestIngestedAtByIntegrationId,null,null, 1,5);
        Assertions.assertThat(fb.getRecords().size()).isEqualTo(4);


        feature = DevProductivityProfile.Feature.builder()
                .maxValue(10l)
                .lowerLimitPercentage(25)
                .upperLimitPercentage(75)
                .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_BUGS_FIXED_PER_MONTH)
                .params(Map.of("aggInterval",List.of("week")))
                .build();

        response = issueManagementHandler.calculateFeature(company, 0, feature, Map.of(), devProductivityFilter, ashishOrg, latestIngestedAtByIntegrationId, null);
        Assertions.assertThat(response.getMean()).isEqualTo(0.8);

        feature = DevProductivityProfile.Feature.builder()
                .maxValue(10l)
                .lowerLimitPercentage(25)
                .upperLimitPercentage(75)
                .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_BUGS_FIXED_PER_MONTH)
                //.params(Map.of("aggInterval",List.of("month")))
                .build();
        response = issueManagementHandler.calculateFeature(company, 0, feature, Map.of(), devProductivityFilter, ashishOrg, latestIngestedAtByIntegrationId, null);
        Assertions.assertThat(response.getMean()).isEqualTo(2.0);

        feature = DevProductivityProfile.Feature.builder()
                .maxValue(10l)
                .lowerLimitPercentage(25)
                .upperLimitPercentage(75)
                .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_CRITICAL_BUGS_RESOLVED_PER_MONTH)
                //.ticketCategories(List.of(UUID.fromString(categoryId1))) //when cateogry is not included, default response is expected
                .build();
        response = issueManagementHandler.calculateFeature(company, 0, feature, Map.of(), devProductivityFilter, ashishOrg, latestIngestedAtByIntegrationId, null);
        Assertions.assertThat(response.getResult()).isEqualTo(feature.getFeatureType().getDefaultValue());

        fb = issueManagementHandler.getBreakDown(company,feature, Map.of(), devProductivityFilter,ashishOrg,latestIngestedAtByIntegrationId,null,null, 1,5);
        Assertions.assertThat(fb.getRecords().size()).isEqualTo(0);

        feature = DevProductivityProfile.Feature.builder()
                .maxValue(10l)
                .lowerLimitPercentage(25)
                .upperLimitPercentage(75)
                .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_CRITICAL_BUGS_RESOLVED_PER_MONTH)
                .ticketCategories(List.of(UUID.fromString(categoryId2)))
                .build();
        response = issueManagementHandler.calculateFeature(company, 0, feature, Map.of(), devProductivityFilter, ashishOrg, latestIngestedAtByIntegrationId, null);
        Assertions.assertThat(response.getMean()).isEqualTo(0.5);

        feature = DevProductivityProfile.Feature.builder()
                .maxValue(20l)
                .lowerLimitPercentage(25)
                .upperLimitPercentage(75)
                .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_STORY_POINTS_DELIVERED_PER_MONTH)
                //.params(Map.of("aggInterval",List.of("month")))
                .build();
        response = issueManagementHandler.calculateFeature(company, 0, feature, Map.of(), devProductivityFilter, ashishOrg, latestIngestedAtByIntegrationId, null);
        Assertions.assertThat(response.getMean()).isEqualTo(9.0);

        fb = issueManagementHandler.getBreakDown(company,feature, Map.of("development_stages",List.of("In Progress","Ready for QA","Ready for Prod")), devProductivityFilter,ashishOrg,latestIngestedAtByIntegrationId,null,null, 0,20);
        Assertions.assertThat(fb.getRecords().size()).isEqualTo(7);

        feature = DevProductivityProfile.Feature.builder()
                .maxValue(2l)
                .lowerLimitPercentage(25)
                .upperLimitPercentage(50)
                .featureType(DevProductivityProfile.FeatureType.AVG_ISSUE_RESOLUTION_TIME)
                .params(Map.of("use_issues",List.of("true")))
                .build();
        response = issueManagementHandler.calculateFeature(company, 0, feature, Map.of(), devProductivityFilter.toBuilder().interval(ReportIntervalType.LAST_WEEK).build(), maximeOrg, latestIngestedAtByIntegrationId, null);
        Assertions.assertThat(response.getMean()).isEqualTo(432000);

        feature = DevProductivityProfile.Feature.builder()
                .maxValue(10l)
                .lowerLimitPercentage(25)
                .upperLimitPercentage(75)
                .enabled(true)
                .featureType(DevProductivityProfile.FeatureType.NUMBER_OF_BUGS_FIXED_PER_MONTH)
                .build();

        response = issueManagementHandler.calculateFeature(company, 0, feature, Map.of(), devProductivityFilter.toBuilder().timeRange(ImmutablePair.of(1631109085l, 1631774300l)).interval(ReportIntervalType.LAST_WEEK).build(), ashishOrg, latestIngestedAtByIntegrationId, null);
        Assertions.assertThat(response.getName()).isEqualTo("Number of bugs worked on in one week");
        Assertions.assertThat(response.getCount()).isEqualTo(2);
        Assertions.assertThat(response.getScore()).isEqualTo(72);

        fb = issueManagementHandler.getBreakDown(company,feature, Map.of("development_stages",List.of("In Progress","Ready for QA","Ready for Prod")), devProductivityFilter,ashishOrg,latestIngestedAtByIntegrationId,null,null, 0,20);
        Assertions.assertThat(fb.getRecords().size()).isEqualTo(9);

        fb = issueManagementHandler.getBreakDown(company,feature, Map.of("development_stages",List.of("In Progress","Ready for QA","Ready for Prod")), devProductivityFilter,ashishOrg,latestIngestedAtByIntegrationId,null,null, 0,5);
        Assertions.assertThat(fb.getRecords().size()).isEqualTo(5);

        fb = issueManagementHandler.getBreakDown(company,feature, Map.of("development_stages",List.of("In Progress","Ready for QA","Ready for Prod")), devProductivityFilter,ashishOrg,latestIngestedAtByIntegrationId,null,null, 1,5);
        Assertions.assertThat(fb.getRecords().size()).isEqualTo(4);

    }

    private static DbJiraIssue getDBJiraIssue(String key, long issueResolvedAt, Integer storyPoints, String priority, String statusCategory, String issueType, List<DbJiraAssignee> jiraAssignees, List<DbJiraStatus> jiraStatuses) {
        return DbJiraIssue.builder()
                .key(key)
                .assignee("ashish")
                .integrationId("1")
                .issueResolvedAt(issueResolvedAt)
                .storyPoints(storyPoints)
                .status("DONE")
                .statusCategory(statusCategory)
                .ingestedAt(ingestedAt)
                .project("test-project")
                .descSize(1)
                .customFields(Map.of())
                .priority(priority)
                .reporter("xyz")
                .issueType(issueType)
                .hops(0)
                .bounces(0)
                .numAttachments(0)
                .issueCreatedAt(18870l)
                .issueUpdatedAt(issueResolvedAt)
                .assigneeList(jiraAssignees)
                .statuses(jiraStatuses)
                .build();
    }
}
