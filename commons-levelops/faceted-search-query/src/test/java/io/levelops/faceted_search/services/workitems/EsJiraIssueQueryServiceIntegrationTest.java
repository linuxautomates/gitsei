package io.levelops.faceted_search.services.workitems;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraAssignee;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.jira.DbJiraStoryPoints;
import io.levelops.commons.databases.models.database.jira.DbJiraVersion;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.JiraFieldService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.JiraIssueSprintMappingDatabaseService;
import io.levelops.commons.databases.services.JiraIssueStoryPointsDatabaseService;
import io.levelops.commons.databases.services.JiraProjectService;
import io.levelops.commons.databases.services.JiraStatusMetadataDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.jira.JiraIssueAggService;
import io.levelops.commons.databases.services.jira.JiraIssuePrioritySlaService;
import io.levelops.commons.databases.services.jira.JiraIssueReadService;
import io.levelops.commons.databases.services.jira.JiraIssueSprintService;
import io.levelops.commons.databases.services.jira.JiraIssueStatusService;
import io.levelops.commons.databases.services.jira.JiraIssueUserService;
import io.levelops.commons.databases.services.jira.JiraIssueVersionService;
import io.levelops.commons.databases.services.jira.JiraIssueWriteService;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.databases.services.jira.conditions.JiraCustomFieldConditionsBuilder;
import io.levelops.commons.databases.services.jira.conditions.JiraFieldConditionsBuilder;
import io.levelops.commons.databases.services.jira.conditions.JiraPartialMatchConditionsBuilder;
import io.levelops.commons.databases.services.jira.utils.JiraIssueQueryBuilder;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.elasticsearch_clients.factory.ESClientFactory;
import io.levelops.commons.elasticsearch_clients.models.ESClusterInfo;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/*
 * Before running this integration test, you have to run below-mentioned test class to make sure data is there into ES
 * io.levelops.faceted_search.services.jira.EsJiraIssueIntegrationTest
 */
public class EsJiraIssueQueryServiceIntegrationTest {
    private static final String company = "test";


    private static DataSource dataSource;
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static EsJiraIssueQueryService esJiraIssueQueryService;
    private static Long ingestedAt;
    private static final String esIp = System.getenv("ES_IP");
    private static final Integer esPort = Integer.valueOf(System.getenv("ES_PORT"));
    private static String user1;
    private static String user2;
    private static String user3;
    private static String integrationId;
    private static ESClientFactory esClientFactory;
    private static EsJiraDBHelperService esJiraDBHelperService;

    @Before
    public void setup() throws SQLException, GeneralSecurityException, IOException {
        if (dataSource != null) {
            return;
        }

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS " + company + " CASCADE; ",
                "CREATE SCHEMA " + company + " ; "
        ).forEach(template::execute);

        IntegrationService integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);

        UserIdentityService userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);

        JiraFieldService jiraFieldService = new JiraFieldService(dataSource);
        jiraFieldService.ensureTableExistence(company);

        JiraProjectService jiraProjectService = new JiraProjectService(dataSource);
        jiraProjectService.ensureTableExistence(company);

        JiraStatusMetadataDatabaseService jiraStatusMetadataDatabaseService = new JiraStatusMetadataDatabaseService(dataSource);
        jiraStatusMetadataDatabaseService.ensureTableExistence(company);

        JiraIssueStoryPointsDatabaseService jiraIssueStoryPointsDatabaseService = new JiraIssueStoryPointsDatabaseService(dataSource);
        jiraIssueStoryPointsDatabaseService.ensureTableExistence(company);
        JiraIssueSprintMappingDatabaseService jiraIssueSprintMappingDatabaseService = new JiraIssueSprintMappingDatabaseService(dataSource);
        jiraIssueSprintMappingDatabaseService.ensureTableExistence(company);
        JiraCustomFieldConditionsBuilder customFieldConditionsBuilder = new JiraCustomFieldConditionsBuilder(dataSource, jiraFieldService, integrationService);
        JiraFieldConditionsBuilder fieldConditionsBuilder = new JiraFieldConditionsBuilder(jiraFieldService);
        JiraPartialMatchConditionsBuilder partialMatchConditionsBuilder = new JiraPartialMatchConditionsBuilder(dataSource, fieldConditionsBuilder, customFieldConditionsBuilder);
        JiraConditionsBuilder jiraConditionsBuilder = new JiraConditionsBuilder(dataSource, fieldConditionsBuilder, customFieldConditionsBuilder, partialMatchConditionsBuilder, true);
        JiraIssueQueryBuilder jiraIssueQueryBuilder = new JiraIssueQueryBuilder(jiraConditionsBuilder);

        JiraIssueAggService aggService = new JiraIssueAggService(dataSource, jiraProjectService, customFieldConditionsBuilder, jiraConditionsBuilder, jiraIssueQueryBuilder, jiraStatusMetadataDatabaseService, 0);
        JiraIssueSprintService sprintService = new JiraIssueSprintService(dataSource);
        JiraIssueUserService userService = new JiraIssueUserService(dataSource);
        JiraIssueVersionService versionService = new JiraIssueVersionService(dataSource);
        JiraIssuePrioritySlaService prioritySlaService = new JiraIssuePrioritySlaService(dataSource);
        JiraIssueStatusService statusService = new JiraIssueStatusService(dataSource, jiraConditionsBuilder);
        JiraIssueReadService readService = new JiraIssueReadService(dataSource, sprintService, statusService, jiraConditionsBuilder, jiraIssueQueryBuilder, jiraStatusMetadataDatabaseService);
        JiraIssueWriteService writeService = new JiraIssueWriteService(dataSource, DefaultObjectMapper.get(), aggService, readService);

        JiraIssueService jiraIssueService = new JiraIssueService(dataSource, writeService, readService, aggService, userService, sprintService, versionService, prioritySlaService, statusService);
        integrationId = integrationService.insert(company, Integration.builder()
                .id("1")
                .application("jira")
                .name("jira test")
                .status("enabled")
                .build());
        integrationService.insertConfig(company, IntegrationConfig.builder()
                .integrationId("1")
                .config(Map.of("agg_custom_fields",
                        List.of(IntegrationConfig.ConfigEntry.builder()
                                .key("customfield_20001")
                                .name("hello")
                                .delimiter(",")
                                .build())))
                .build());
        integrationService.insert(company, Integration.builder()
                .application("jira")
                .name("jira test 2")
                .status("enabled")
                .build());
        jiraIssueService.ensureTableExistence(company);

        ingestedAt = 1647138501L;
        user1 = userIdentityService.insert(company, DbScmUser.builder()
                .cloudId("maxime")
                .integrationId("1")
                .id(String.valueOf(UUID.randomUUID()))
                .displayName("maxime")
                .originalDisplayName("maxime")
                .createdAt(Instant.EPOCH.getEpochSecond())
                .updatedAt(Instant.EPOCH.getEpochSecond())
                .build());
        user2 = userIdentityService.insert(company, DbScmUser.builder()
                .cloudId("srinath")
                .integrationId("1")
                .id(String.valueOf(UUID.randomUUID()))
                .displayName("srinath")
                .originalDisplayName("srinath")
                .createdAt(Instant.EPOCH.getEpochSecond())
                .updatedAt(Instant.EPOCH.getEpochSecond())
                .build());
        user3 = userIdentityService.insert(company, DbScmUser.builder()
                .cloudId("viraj")
                .integrationId("1")
                .id(String.valueOf(UUID.randomUUID()))
                .displayName("viraj")
                .originalDisplayName("viraj")
                .createdAt(Instant.EPOCH.getEpochSecond())
                .updatedAt(Instant.EPOCH.getEpochSecond())
                .build());
        DbJiraIssue issue1 = DbJiraIssue.builder()
                .key("key1")
                .salesforceFields(Map.of("k1", List.of("f1", "f2")))
                .integrationId("1")
                .ingestedAt(ingestedAt)
                .project("p1")
                .summary("summary")
                .components(List.of("comp1", "comp2"))
                .labels(List.of("label1", "label2"))
                .fixVersions(List.of())
                .sprintIds(List.of())
                .storyPoints(1)
                .epic("epic2")
                .statusCategory("category1")
                .descSize(4)
                .resolution("IN PROGRESS")
                .priority("LOW")
                .reporter("srinath")
                .reporterId(user2)
                .assignee("viraj")
                .assigneeId(user3)
                .assigneeList(List.of(DbJiraAssignee.builder()
                                .assignee("a1")
                                .createdAt(1613658414L)
                                .integrationId("1")
                                .issueKey("key1")
                                .startTime(1613658414L)
                                .endTime(1613658414L)
                                .build(),
                        DbJiraAssignee.builder()
                                .assignee("a2")
                                .createdAt(1613658414L)
                                .integrationId("1")
                                .issueKey("key1")
                                .startTime(1613658414L)
                                .endTime(1613658414L)
                                .build()))
                .customFields(Map.of("customfield_10048", 1, "customfield_10049", "1"))
                .status("s1")
                .statuses(List.of(DbJiraStatus.builder()
                        .status("s1")
                        .statusId("213")
                        .integrationId("1")
                        .issueKey("key1")
                        .startTime(1613658414L)
                        .endTime(1613658414L)
                        .build()))
                .issueType("issue1")
                .versions(List.of("v1", "v2"))
                .sprintIds(List.of(1, 2))
                .hops(1)
                .bounces(0)
                .numAttachments(2)
                .issueCreatedAt(1513658414L)
                .issueUpdatedAt(1613657908L)
                .issueResolvedAt(1613658414L)
                .issueDueAt(1613658414L)
                .build();

        DbJiraIssue issue2 = DbJiraIssue.builder()
                .key("key2")
                .salesforceFields(Map.of("k1", List.of("f1", "f2")))
                .integrationId("1")
                .ingestedAt(ingestedAt)
                .project("p2")
                .summary("summary")
                .components(List.of("comp1", "comp2"))
                .labels(List.of("label1", "label2"))
                .fixVersions(List.of())
                .sprintIds(List.of())
                .statusCategory("category1")
                .epic("epic1")
                .storyPoints(1)
                .descSize(4)
                .resolution("NOT RESOLVED")
                .priority("HIGH")
                .reporter("viraj")
                .reporterId(user3)
                .assignee("srinath")
                .assigneeId(user2)
                .assigneeList(List.of(DbJiraAssignee.builder()
                                .assignee("a1")
                                .createdAt(1613658414L)
                                .integrationId("1")
                                .issueKey("key1")
                                .startTime(1613658414L)
                                .endTime(1613658414L)
                                .build(),
                        DbJiraAssignee.builder()
                                .assignee("a2")
                                .createdAt(1613658414L)
                                .integrationId("1")
                                .issueKey("key1")
                                .startTime(1613658414L)
                                .endTime(1613658414L)
                                .build()))
                .customFields(Map.of("customfield_10048", 1, "customfield_10050", "String"))
                .status("s1")
                .statuses(List.of(DbJiraStatus.builder()
                        .status("s1")
                        .statusId("213")
                        .integrationId("1")
                        .issueKey("key1")
                        .startTime(1613658414L)
                        .endTime(1613658414L)
                        .build()))
                .issueType("issue1")
                .versions(List.of("v1", "v2"))
                .sprintIds(List.of(1, 2))
                .hops(1)
                .bounces(0)
                .numAttachments(2)
                .issueCreatedAt(1612658414L)
                .issueUpdatedAt(1613658550L)
                .issueResolvedAt(1613658414L)
                .issueDueAt(1613658414L)
                .build();

        DbJiraIssue issue3 = DbJiraIssue.builder()
                .key("key3")
                .salesforceFields(Map.of("k3", List.of("f3", "f4")))
                .integrationId("1")
                .ingestedAt(ingestedAt)
                .project("p3")
                .epic("epic1")
                .summary("This is a summary")
                .components(List.of("comp1", "comp2"))
                .labels(List.of("HIGH", "label2"))
                .fixVersions(List.of())
                .sprintIds(List.of(2))
                .statusCategory("category1")
                .storyPoints(1)
                .descSize(4)
                .priority("MEDIUM")
                .reporter("srinath")
                .reporterId(user2)
                .assignee("maxime")
                .assigneeId(user1)
                .resolution("RESOLVED")
                .assigneeList(List.of(DbJiraAssignee.builder()
                                .assignee("a1")
                                .createdAt(1613658414L)
                                .integrationId("1")
                                .issueKey("key1")
                                .startTime(1613658414L)
                                .endTime(1613658414L)
                                .build(),
                        DbJiraAssignee.builder()
                                .assignee("a2")
                                .createdAt(1613658414L)
                                .integrationId("1")
                                .issueKey("key1")
                                .startTime(1613658414L)
                                .endTime(1613658414L)
                                .build()))
                .customFields(Map.of("customfield_10048", 1, "customfield_10050", "sample-value"))
                .status("s1")
                .statuses(List.of(DbJiraStatus.builder()
                        .status("s1")
                        .statusId("213")
                        .integrationId("1")
                        .issueKey("key1")
                        .startTime(1613658414L)
                        .endTime(1613658414L)
                        .build()))
                .issueType("issue3")
                .versions(List.of("v4", "v6"))
                .sprintIds(List.of(1, 4))
                .hops(3)
                .bounces(2)
                .numAttachments(2)
                .issueCreatedAt(1613358414L)
                .issueUpdatedAt(1613653490L)
                .issueResolvedAt(1613658414L)
                .issueDueAt(1613658414L)
                .build();

        jiraIssueService.insert(company, issue1);
        jiraIssueService.insert(company, issue2);
        jiraIssueService.insert(company, issue3);
        jiraFieldService.batchUpsert(company,
                List.of(DbJiraField.builder().custom(true).name("yello").integrationId("1").fieldType("number")
                                .fieldKey("customfield_10048").fieldItems("user").build(),
                        DbJiraField.builder().custom(true).name("yello").integrationId("1").fieldType("string")
                                .fieldKey("customfield_10049").build(),
                        DbJiraField.builder().custom(true).name("hello").integrationId("1").fieldType("string")
                                .fieldKey("customfield_10050").build()));
        DbJiraVersion v1 = DbJiraVersion.builder()
                .versionId(1)
                .name("v1")
                .integrationId(1)
                .build();
        DbJiraVersion v2 = DbJiraVersion.builder()
                .versionId(2)
                .name("v2")
                .integrationId(1)
                .build();
        DbJiraVersion v3 = DbJiraVersion.builder()
                .versionId(3)
                .name("v3")
                .integrationId(1)
                .build();
        jiraIssueService.insertJiraVersion(company, v1);
        jiraIssueService.insertJiraVersion(company, v2);
        jiraIssueService.insertJiraVersion(company, v3);


        DbJiraStoryPoints storyPoints1 = DbJiraStoryPoints.builder()
                .storyPoints(1)
                .issueKey("key1")
                .integrationId("1")
                .startTime(1649944549L)
                .endTime(1649944549L)
                .build();
        DbJiraStoryPoints storyPoints2 = DbJiraStoryPoints.builder()
                .storyPoints(2)
                .issueKey("key1")
                .integrationId("1")
                .startTime(1649949549L)
                .endTime(1649949549L)
                .build();
        jiraIssueStoryPointsDatabaseService.insert(company, storyPoints1);
        jiraIssueStoryPointsDatabaseService.insert(company, storyPoints2);


        jiraIssueService.insertJiraLinkedIssueRelation(company, "1", "key1",
                "key_to1", "duplicate");
        jiraIssueService.insertJiraLinkedIssueRelation(company, "1",
                "key1", "key_to2", "duplicate");

        DbJiraSprint dbJiraSprint1 = DbJiraSprint.builder()
                .sprintId(1)
                .integrationId(1)
                .name("sprint1")
                .goal("goal1")
                .startDate(1613658414L)
                .updatedAt(1613658414L)
                .endDate(1613658414L)
                .completedDate(1613658414L)
                .build();

        DbJiraSprint dbJiraSprint2 = DbJiraSprint.builder()
                .sprintId(2)
                .integrationId(1)
                .name("sprint2")
                .goal("goal2")
                .startDate(1613658414L)
                .updatedAt(1613658414L)
                .endDate(1613658414L)
                .completedDate(1613658414L)
                .build();

        jiraIssueService.insertJiraSprint(company, dbJiraSprint1);
        jiraIssueService.insertJiraSprint(company, dbJiraSprint2);

        esClientFactory = new ESClientFactory(List.of(ESClusterInfo.builder()
                .name("CLUSTER_1")
                .ipAddresses(List.of("10.128.15.195"))
                .port(9220)
                .defaultCluster(true)
                .build()));
        OrgUsersDatabaseService orgUsersDatabaseService = new OrgUsersDatabaseService(dataSource, new ObjectMapper(), new OrgVersionsDatabaseService(dataSource), userIdentityService);

        esJiraDBHelperService = new EsJiraDBHelperService(jiraFieldService, orgUsersDatabaseService, jiraStatusMetadataDatabaseService);

        esJiraIssueQueryService = new EsJiraIssueQueryService(esClientFactory, esJiraDBHelperService);
    }

    @Test
    public void testGetListFromEs() throws IOException, SQLException {
        DbListResponse<DbJiraIssue> list = esJiraIssueQueryService.getJiraIssuesList(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .integrationIds(List.of("1"))
                .build(), null, null, 0, 10, java.util.Optional.empty());
        Assertions.assertThat(list).isNotNull();
        Assertions.assertThat(list.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(list.getRecords().size()).isEqualTo(3);
        Assertions.assertThat(list.getRecords().stream().map(DbJiraIssue::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("key1", "key2", "key3");

        list = esJiraIssueQueryService.getJiraIssuesList(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .keys(List.of("key1"))
                .integrationIds(List.of("1"))
                .build(), null,null, 0, 10, java.util.Optional.empty());
        Assertions.assertThat(list).isNotNull();
        Assertions.assertThat(list.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(list.getRecords().size()).isEqualTo(1);
        Assertions.assertThat(list.getRecords().stream().map(DbJiraIssue::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("key1");

        list = esJiraIssueQueryService.getJiraIssuesList(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .keys(List.of("key2"))
                .integrationIds(List.of("1"))
                .build(), null,null, 0, 10, java.util.Optional.empty());
        Assertions.assertThat(list).isNotNull();
        Assertions.assertThat(list.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(list.getRecords().size()).isEqualTo(1);
        Assertions.assertThat(list.getRecords().stream().map(DbJiraIssue::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("key2");

        list = esJiraIssueQueryService.getJiraIssuesList(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .keys(List.of("key3"))
                .integrationIds(List.of("1"))
                .build(), null,null, 0, 10, java.util.Optional.empty());
        Assertions.assertThat(list).isNotNull();
        Assertions.assertThat(list.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(list.getRecords().size()).isEqualTo(1);
        Assertions.assertThat(list.getRecords().stream().map(DbJiraIssue::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("key3");

        list = esJiraIssueQueryService.getJiraIssuesList(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .projects(List.of("p3"))
                .integrationIds(List.of("1"))
                .build(), null, null, 0, 10, java.util.Optional.empty());
        Assertions.assertThat(list).isNotNull();
        Assertions.assertThat(list.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(list.getRecords().size()).isEqualTo(1);
        Assertions.assertThat(list.getRecords().stream().map(DbJiraIssue::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("key3");
        Assertions.assertThat(list.getRecords().stream().map(DbJiraIssue::getPriority).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("MEDIUM");
    }


    @Test
    public void testAcross() throws IOException, SQLException {
        DbListResponse<DbAggregationResult> ticketReport = esJiraIssueQueryService.getAggReport(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.project)
                .integrationIds(List.of("1"))
                .build(), null, null, null, null, false);
        Assertions.assertThat(ticketReport.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("p1", "p2", "p3");

        ticketReport = esJiraIssueQueryService.getAggReport(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.priority)
                .integrationIds(List.of("1"))
                .build(), null, null, null, null, false);
        Assertions.assertThat(ticketReport.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("HIGH", "MEDIUM", "LOW");
        ticketReport = esJiraIssueQueryService.getAggReport(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.status_category)
                .integrationIds(List.of("1"))
                .build(), List.of(), null, null, null, false);
        Assertions.assertThat(ticketReport.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(ticketReport.getCount()).isEqualTo(1);
        Assertions.assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("category1");
        Assertions.assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getTotalTickets).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(3L);
        ticketReport = esJiraIssueQueryService.getAggReport(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.epic)
                .integrationIds(List.of("1"))
                .build(), List.of(), null, null, null, false);
        Assertions.assertThat(ticketReport.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(ticketReport.getCount()).isEqualTo(2);
        Assertions.assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("epic1", "epic2");
        Assertions.assertThat(ticketReport.getRecords().stream().filter(x -> x.getKey().equalsIgnoreCase("epic1")).map(DbAggregationResult::getTotalTickets).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(2L);
        Assertions.assertThat(ticketReport.getRecords().stream().filter(x -> x.getKey().equalsIgnoreCase("epic2")).map(DbAggregationResult::getTotalTickets).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1L);

        ticketReport = esJiraIssueQueryService.getAggReport(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.component)
                .integrationIds(List.of("1"))
                .build(), List.of(), null, null, null, false);
        Assertions.assertThat(ticketReport.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(ticketReport.getCount()).isEqualTo(2);
        Assertions.assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("comp1", "comp2");
        Assertions.assertThat(ticketReport.getRecords().stream().filter(x -> x.getKey().equalsIgnoreCase("comp1")).map(DbAggregationResult::getTotalTickets).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(3L);
        Assertions.assertThat(ticketReport.getRecords().stream().filter(x -> x.getKey().equalsIgnoreCase("comp2")).map(DbAggregationResult::getTotalTickets).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(3L);

        ticketReport = esJiraIssueQueryService.getAggReport(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.label)
                .integrationIds(List.of("1"))
                .build(), List.of(), null, null, null, false);
        Assertions.assertThat(ticketReport.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(ticketReport.getCount()).isEqualTo(3);
        Assertions.assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("label1", "label2", "HIGH");
        Assertions.assertThat(ticketReport.getRecords().stream().filter(x -> x.getKey().equalsIgnoreCase("label1")).map(DbAggregationResult::getTotalTickets).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(2L);
        Assertions.assertThat(ticketReport.getRecords().stream().filter(x -> x.getKey().equalsIgnoreCase("label2")).map(DbAggregationResult::getTotalTickets).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(3L);

        ticketReport = esJiraIssueQueryService.getAggReport(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .priorities(List.of("HIGH"))
                .across(JiraIssuesFilter.DISTINCT.status_category)
                .integrationIds(List.of("1"))
                .build(), List.of(), null, null, null, false);
        Assertions.assertThat(ticketReport.getTotalCount()).isEqualTo(1);
        Assertions.assertThat(ticketReport.getCount()).isEqualTo(1);
        Assertions.assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("category1");

        ticketReport = esJiraIssueQueryService.getAggReport(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.assignee)
                .integrationIds(List.of("1"))
                .build(), List.of(), null, null, null, false);
        Assertions.assertThat(ticketReport.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(ticketReport.getCount()).isEqualTo(3);
        Assertions.assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("maxime", "srinath", "viraj");

        ticketReport = esJiraIssueQueryService.getAggReport(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.reporter)
                .integrationIds(List.of("1"))
                .build(), List.of(), null, null, null, false);
        Assertions.assertThat(ticketReport.getTotalCount()).isEqualTo(2);
        Assertions.assertThat(ticketReport.getCount()).isEqualTo(2);
        Assertions.assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getAdditionalKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("srinath", "viraj");

        ticketReport = esJiraIssueQueryService.getAggReport(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.resolution)
                .integrationIds(List.of("1"))
                .build(), List.of(), null, null, null, false);
        Assertions.assertThat(ticketReport.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(ticketReport.getCount()).isEqualTo(3);
        Assertions.assertThat(ticketReport.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("IN PROGRESS", "NOT RESOLVED", "RESOLVED");

        //-- Time Across fields -- not working
        ticketReport = esJiraIssueQueryService.getAggReport(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.issue_created)
                .integrationIds(List.of("1"))
                .build(), List.of(), null, null, null, false);

        ticketReport = esJiraIssueQueryService.getAggReport(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.issue_updated)
                .integrationIds(List.of("1"))
                .build(), List.of(), null, null, null, false);

        // -- throwing some error in query builder // check this
        ticketReport = esJiraIssueQueryService.getAggReport(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.custom_field)
                .customAcross("customfield_10048")
                .integrationIds(List.of("1"))
                .build(), List.of(), null, null, null, false);
        Assertions.assertThat(ticketReport.getTotalCount()).isEqualTo(1);
    }

    @Test
    public void testStacks() throws IOException, SQLException {
        DbListResponse<DbAggregationResult> ticketReport = esJiraIssueQueryService.getAggReport(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.project)
                .integrationIds(List.of("1"))
                .build(), List.of(JiraIssuesFilter.DISTINCT.assignee), null, null, null, false);
        Assertions.assertThat(ticketReport.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(ticketReport.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);
        Assertions.assertThat(ticketReport.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("p1", "p2", "p3");

        ticketReport = esJiraIssueQueryService.getAggReport(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.project)
                .integrationIds(List.of("1"))
                .build(), List.of(JiraIssuesFilter.DISTINCT.priority), null, null, null, false);

        Assertions.assertThat(ticketReport.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(ticketReport.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);
        Assertions.assertThat(ticketReport.getRecords()
                .stream()
                .map(DbAggregationResult::getKey)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("p1", "p2", "p3");

        ticketReport = esJiraIssueQueryService.getAggReport(company, JiraIssuesFilter.builder()
                .ingestedAt(ingestedAt)
                .across(JiraIssuesFilter.DISTINCT.assignee)
                .integrationIds(List.of("1"))
                .build(), List.of(JiraIssuesFilter.DISTINCT.status_category), null, null, null, false);
        Assertions.assertThat(ticketReport.getTotalCount()).isEqualTo(3);
        Assertions.assertThat(ticketReport.getRecords()
                .stream()
                .findFirst()
                .orElseThrow()
                .getStacks()
                .size()).isEqualTo(1);
        Assertions.assertThat(ticketReport.getRecords()
                .stream()
                .map(DbAggregationResult::getAdditionalKey)
                .collect(Collectors.toList())).containsExactlyInAnyOrder("srinath", "maxime", "viraj");


    }

}
