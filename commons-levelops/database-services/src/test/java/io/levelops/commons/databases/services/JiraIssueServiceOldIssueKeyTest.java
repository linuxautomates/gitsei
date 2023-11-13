package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.converters.DbJiraIssueConverters;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraAssignee;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.models.database.organization.OrgUserId;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
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
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class JiraIssueServiceOldIssueKeyTest {
    private static final String company1 = "version1";
    private static final String company2 = "version2";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static NamedParameterJdbcTemplate template1;
    private static NamedParameterJdbcTemplate template2;

    private static JiraIssueService jiraIssueService1;
    private static JiraIssueService jiraIssueService2;

    private static List<DbJiraIssue> jiraIssueList1 = new ArrayList<>();
    private static List<DbJiraIssue> jiraIssueList2 = new ArrayList<>();

    @BeforeClass
    public static void setup() throws SQLException, GeneralSecurityException, IOException {

       setupCompany(company1, false);
       setupCompany(company2, true);
    }

    private static void setupCompany(String company, boolean writeV2) throws SQLException, IOException {

        DataSource dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        JdbcTemplate t = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS " + company + " CASCADE; ",
                "CREATE SCHEMA " + company + " ; "
        ).forEach(t::execute);


        IntegrationService integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);

        UserIdentityService userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);

        JiraFieldService jiraFieldService = new JiraFieldService(dataSource);
        jiraFieldService.ensureTableExistence(company);

        JiraProjectService jiraProjectService = new JiraProjectService(dataSource);
        jiraProjectService.ensureTableExistence(company);

        JiraIssueStoryPointsDatabaseService jiraIssueStoryPointsDatabaseService = new JiraIssueStoryPointsDatabaseService(dataSource);
        jiraIssueStoryPointsDatabaseService.ensureTableExistence(company);

        JiraIssueSprintMappingDatabaseService jiraIssueSprintMappingDatabaseService = new JiraIssueSprintMappingDatabaseService(dataSource);
        jiraIssueSprintMappingDatabaseService.ensureTableExistence(company);

        JiraStatusMetadataDatabaseService jiraStatusMetadataDatabaseService = new JiraStatusMetadataDatabaseService(dataSource);
        jiraStatusMetadataDatabaseService.ensureTableExistence(company);

        JiraCustomFieldConditionsBuilder customFieldConditionsBuilder = new JiraCustomFieldConditionsBuilder(dataSource, jiraFieldService, integrationService);
        JiraFieldConditionsBuilder fieldConditionsBuilder = new JiraFieldConditionsBuilder(jiraFieldService);
        JiraPartialMatchConditionsBuilder partialMatchConditionsBuilder = new JiraPartialMatchConditionsBuilder(dataSource, fieldConditionsBuilder, customFieldConditionsBuilder);
        JiraConditionsBuilder jiraConditionsBuilder = new JiraConditionsBuilder(dataSource, fieldConditionsBuilder, customFieldConditionsBuilder, partialMatchConditionsBuilder, true);
        JiraIssueQueryBuilder jiraIssueQueryBuilder = new JiraIssueQueryBuilder(jiraConditionsBuilder);

        JiraIssuePrioritySlaService prioritySlaService = new JiraIssuePrioritySlaService(dataSource);
        JiraIssueSprintService sprintService = new JiraIssueSprintService(dataSource);
        JiraIssueVersionService versionService = new JiraIssueVersionService(dataSource);
        JiraIssueUserService jiraIssueUserService = new JiraIssueUserService(dataSource);
        JiraIssueAggService aggService = new JiraIssueAggService(dataSource, jiraProjectService, customFieldConditionsBuilder, jiraConditionsBuilder, jiraIssueQueryBuilder, jiraStatusMetadataDatabaseService, 1);
        JiraIssueStatusService statusService = new JiraIssueStatusService(dataSource, jiraConditionsBuilder);
        JiraIssueReadService jiraIssueReadService = new JiraIssueReadService(dataSource, sprintService, statusService, jiraConditionsBuilder, jiraIssueQueryBuilder, jiraStatusMetadataDatabaseService);
        JiraIssueWriteService jiraIssueWriteService = new JiraIssueWriteService(dataSource, m, aggService, jiraIssueReadService);

        JiraIssueService jiraIssueService = new JiraIssueService(dataSource, jiraIssueWriteService, jiraIssueReadService, aggService, jiraIssueUserService, sprintService, versionService, prioritySlaService, statusService);

        TagItemDBService tagItemService = new TagItemDBService(dataSource);
        TagsService tagsService = new TagsService(dataSource);
        OrgVersionsDatabaseService versionsService = new OrgVersionsDatabaseService(dataSource);
        DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, DefaultObjectMapper.get());

        OrgUsersDatabaseService usersService = new OrgUsersDatabaseService(dataSource, DefaultObjectMapper.get(), versionsService, userIdentityService);
        UserService userService = new UserService(dataSource, DefaultObjectMapper.get());
        OrgUnitsDatabaseService unitsService = new OrgUnitsDatabaseService(dataSource, DefaultObjectMapper.get(), tagItemService, usersService, versionsService, dashboardWidgetService);
        ProductService productService = new ProductService(dataSource);
        usersService.ensureTableExistence(company);
        userService.ensureTableExistence(company);
        tagsService.ensureTableExistence(company);
        tagItemService.ensureTableExistence(company);
        OrgUnitHelper unitsHelper = new OrgUnitHelper(unitsService, integrationService);
        dashboardWidgetService.ensureTableExistence(company);
        productService.ensureTableExistence(company);

        OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, unitsHelper, m);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        unitsService.ensureTableExistence(company);
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        versionsService.ensureTableExistence(company);

        String integrationId = integrationService.insert(company, Integration.builder()
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

        Long ingestedAt = 1647138501L;
        String user1 = userIdentityService.insert(company, DbScmUser.builder()
                .cloudId("maxime")
                .integrationId("1")
                .id(String.valueOf(UUID.randomUUID()))
                .displayName("maxime")
                .originalDisplayName("maxime")
                .createdAt(Instant.EPOCH.getEpochSecond())
                .updatedAt(Instant.EPOCH.getEpochSecond())
                .build());
        String user2 = userIdentityService.insert(company, DbScmUser.builder()
                .cloudId("srinath")
                .integrationId("1")
                .id(String.valueOf(UUID.randomUUID()))
                .displayName("srinath")
                .originalDisplayName("srinath")
                .createdAt(Instant.EPOCH.getEpochSecond())
                .updatedAt(Instant.EPOCH.getEpochSecond())
                .build());
        String user3 = userIdentityService.insert(company, DbScmUser.builder()
                .cloudId("viraj")
                .integrationId("1")
                .id(String.valueOf(UUID.randomUUID()))
                .displayName("viraj")
                .originalDisplayName("viraj")
                .createdAt(Instant.EPOCH.getEpochSecond())
                .updatedAt(Instant.EPOCH.getEpochSecond())
                .build());

        OrgUnitCategory orgUnitCategory1 = OrgUnitCategory.builder()
                .name("TEAM A")
                .description("Sample team")
                .isPredefined(true)
                .build();
        String orgGroupId1 = orgUnitCategoryDatabaseService.insert(company, orgUnitCategory1);

        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("fullName1")
                .active(true)
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("maxime").username("maxime").integrationType("jira")
                        .integrationId(Integer.parseInt(integrationId)).build()))
                .versions(Set.of(1))
                .build();
        var userId1 = usersService.upsert(company, orgUser1);
        usersService.linkCloudIds(company, userId1.getId(), Set.of(DBOrgUser.LoginId.builder()
                .cloudId("maxime")
                .username("maxime")
                .integrationId(Integer.parseInt(integrationId))
                .integrationType("jira")
                .build()), DbScmUser.MappingStatus.MANUAL);

        var manager1 = OrgUserId.builder().id(userId1.getId()).refId(userId1.getRefId()).fullName(orgUser1.getFullName()).email(orgUser1.getEmail()).build();
//        var manager2 = OrgUserId.builder().id(userId2.getId()).refId(userId2.getRefId()).fullName(orgUser2.getFullName()).email(orgUser2.getEmail()).build();
        var managers = Set.of(
                manager1
//                ,manager2
        );


        DBOrgUnit unit1 = DBOrgUnit.builder()
                .name("unit1")
                .description("My unit1")
                .active(true)
                .versions(Set.of(1))
                .managers(managers)
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.valueOf(integrationId))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .refId(1)
                .build();
        unitsService.insertForId(company, unit1);

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
                .oldIssueKey("key1")
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
                .oldIssueKey("key2")
                .build();

        if(writeV2){
            jiraIssueService2 = jiraIssueService;
            jiraIssueList2.add(issue1);
            jiraIssueList2.add(issue2);
            jiraIssueList2.add(issue3);
            template2 = new NamedParameterJdbcTemplate(dataSource);
        }else{
            jiraIssueService1 = jiraIssueService;
            jiraIssueList1.add(issue1);
            jiraIssueList1.add(issue2);
            jiraIssueList1.add(issue3);
            template1 = new NamedParameterJdbcTemplate(dataSource);
        }
    }

    @Test
    public void test() throws SQLException {
        testInserts_(company1, template1, jiraIssueService1, jiraIssueList1);
        testInserts_(company2, template2, jiraIssueService2, jiraIssueList2);
    }

    private void testInserts_(String company, NamedParameterJdbcTemplate template, JiraIssueService jiraIssueService, List<DbJiraIssue> jiraIssueList) throws SQLException {

        DbJiraIssue issue1 = jiraIssueList.get(0);
        DbJiraIssue issue2 = jiraIssueList.get(1);
        DbJiraIssue issue3 = jiraIssueList.get(2);

        jiraIssueService.insert(company, issue1);
        jiraIssueService.insert(company, issue2);
        jiraIssueService.insert(company, issue3);

        List<DbJiraIssue> list = getJiraIssueList(company, template);
        Assertions.assertNotNull(list);
        Assertions.assertEquals(list.size(),3);
        Assertions.assertFalse(list.get(0).getIsActive());
        Assertions.assertFalse(list.get(1).getIsActive());
        Assertions.assertTrue(list.get(2).getIsActive());

        truncateTable(company, template);

        jiraIssueService.insert(company, issue3);
        jiraIssueService.insert(company, issue1);
        jiraIssueService.insert(company, issue2);

        list = getJiraIssueList(company, template);
        Assertions.assertNotNull(list);
        Assertions.assertEquals(list.size(),3);
        Assertions.assertFalse(list.get(0).getIsActive());
        Assertions.assertFalse(list.get(1).getIsActive());
        Assertions.assertTrue(list.get(2).getIsActive());

        truncateTable(company, template);

        jiraIssueService.insert(company, issue2);
        jiraIssueService.insert(company, issue1);
        jiraIssueService.insert(company, issue3);

        list = getJiraIssueList(company, template);
        Assertions.assertNotNull(list);
        Assertions.assertEquals(list.size(),3);
        Assertions.assertFalse(list.get(0).getIsActive());
        Assertions.assertFalse(list.get(1).getIsActive());
        Assertions.assertTrue(list.get(2).getIsActive());

        truncateTable(company, template);

        jiraIssueService.insert(company, issue2);
        jiraIssueService.insert(company, issue3);
        jiraIssueService.insert(company, issue1);

        list = getJiraIssueList(company, template);
        Assertions.assertNotNull(list);
        Assertions.assertEquals(list.size(),3);
        Assertions.assertFalse(list.get(0).getIsActive());
        Assertions.assertFalse(list.get(1).getIsActive());
        Assertions.assertTrue(list.get(2).getIsActive());

        truncateTable(company, template);

        jiraIssueService.insert(company, issue3);
        jiraIssueService.insert(company, issue2);
        jiraIssueService.insert(company, issue1);

        list = getJiraIssueList(company, template);
        Assertions.assertNotNull(list);
        Assertions.assertEquals(list.size(),3);
        Assertions.assertFalse(list.get(0).getIsActive());
        Assertions.assertFalse(list.get(1).getIsActive());
        Assertions.assertTrue(list.get(2).getIsActive());
    }

    private List<DbJiraIssue> getJiraIssueList(String company, NamedParameterJdbcTemplate template) {
        String sql = "select * from "+company+".jira_issues order by key";
        return template.query(sql,  DbJiraIssueConverters.listRowMapper(false, false, false, false, false, false, false));
   }

    private void truncateTable(String company, NamedParameterJdbcTemplate template) {
        String sql = "truncate "+company+".jira_issues";
        template.update(sql, Map.of());
    }
}
