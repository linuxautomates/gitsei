package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraUser;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.database.okta.DbOktaAssociation;
import io.levelops.commons.databases.models.database.okta.DbOktaGroup;
import io.levelops.commons.databases.models.database.okta.DbOktaUser;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.OktaGroupsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraUser;
import io.levelops.integrations.okta.models.OktaGroup;
import io.levelops.integrations.okta.models.OktaUser;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraOktaServiceTest {

    public static final String STORY_POINTS_FIELD = "customfield_10030";
    private static final String COMPANY = "test";
    private static final ObjectMapper OBJECT_MAPPER = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static OktaAggService oktaAggService;
    private static JiraIssueService jiraIssueService;
    private static JiraOktaService jiraOktaService;
    private static DbJiraIssue randomIssue;
    private static Date currentTime;
    private static Long ingestedAt;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        if (dataSource != null)
            return;

        dataSource =  DatabaseTestUtils.setUpDataSource(pg, COMPANY);
        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, COMPANY);
        jiraIssueService = jiraTestDbs.getJiraIssueService();

        JiraFieldService jiraFieldService = jiraTestDbs.getJiraFieldService();
        JiraProjectService jiraProjectService = jiraTestDbs.getJiraProjectService();
        IntegrationService integrationService = jiraTestDbs.getIntegrationService();

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .application("jira")
                .name("jira test")
                .status("enabled")
                .build());
        jiraIssueService.ensureTableExistence(COMPANY);

        String input = ResourceUtils.getResourceAsString("json/databases/jiraissues_jun62020.json");
        PaginatedResponse<JiraIssue> issues = OBJECT_MAPPER.readValue(input,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraIssue.class));
        currentTime = new Date();
        List<IntegrationConfig.ConfigEntry> entries = List.of(
                IntegrationConfig.ConfigEntry.builder().key("customfield_12641").name("something").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_12746").name("something 1").build(),
                IntegrationConfig.ConfigEntry.builder().key("customfield_12716").name("something 2").build());
        issues.getResponse().getRecords().forEach(issue -> {
            try {
                DbJiraIssue tmp = JiraIssueParser.parseJiraIssue(issue, "1", currentTime, JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField("customfield_10014")
                        .storyPointsField("customfield_10030")
                        .customFieldConfig(entries)
                        .build());
                if (randomIssue == null)
                    randomIssue = tmp;
                else
                    randomIssue = (new Random().nextInt(100)) > 50 ? tmp : randomIssue;

                jiraIssueService.insert(COMPANY, JiraIssueParser.parseJiraIssue(issue, "1",
                        DateUtils.addSeconds(currentTime, 2 * -86400), JiraIssueParser.JiraParserConfig.builder()
                                .epicLinkField("customfield_10014")
                                .storyPointsField("customfield_10030")
                                .customFieldConfig(entries)
                                .build()));
                if (jiraIssueService.get(COMPANY, tmp.getKey(), tmp.getIntegrationId(),
                        tmp.getIngestedAt() - 86400L).isPresent())
                    throw new RuntimeException("This issue shouldnt exist.");
                jiraIssueService.insert(COMPANY, JiraIssueParser.parseJiraIssue(issue, "1",
                        DateUtils.addSeconds(currentTime, -86400), JiraIssueParser.JiraParserConfig.builder()
                                .epicLinkField("customfield_10014")
                                .storyPointsField("customfield_10030")
                                .customFieldConfig(entries)
                                .build()));
                if (jiraIssueService.get(COMPANY, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt()).isPresent())
                    throw new RuntimeException("This issue shouldnt exist.");
                jiraIssueService.insert(COMPANY, tmp);
                if (jiraIssueService.get(COMPANY, tmp.getKey(), tmp.getIntegrationId(), tmp.getIngestedAt()).isEmpty())
                    throw new RuntimeException("This issue should exist.");
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
        });
        input = ResourceUtils.getResourceAsString("json/databases/jirausers_aug12.json");
        PaginatedResponse<JiraUser> jiraUsers = OBJECT_MAPPER.readValue(input,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, JiraUser.class));
        jiraUsers.getResponse().getRecords().forEach(issue -> {
            DbJiraUser tmp = DbJiraUser.fromJiraUser(issue, "1");
            jiraIssueService.insertJiraUser(COMPANY, tmp);
        });
        ingestedAt = io.levelops.commons.dates.DateUtils.truncate(currentTime, Calendar.DATE);

        //okta setup
        oktaAggService = new OktaAggService(dataSource);
        integrationService.ensureTableExistence(COMPANY);
        integrationService.insert(COMPANY, Integration.builder()
                .application("okta")
                .name("okta_test")
                .status("enabled")
                .build());
        oktaAggService.ensureTableExistence(COMPANY);
        final String usersInput = ResourceUtils.getResourceAsString("json/databases/okta_users.json");
        PaginatedResponse<OktaUser> paginatedUsers = OBJECT_MAPPER.readValue(usersInput,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, OktaUser.class));
        List<OktaUser> users = paginatedUsers.getResponse().getRecords();
        List<DbOktaUser> dbOktaUsers = new ArrayList<>();
        String integrationId = "1";
        for (OktaUser user : users) {
            dbOktaUsers.add(DbOktaUser.fromOktaUSer(user, integrationId, currentTime));
        }
        dbOktaUsers.forEach(user -> oktaAggService.insert(COMPANY, user));
        final String groupsInput = ResourceUtils.getResourceAsString("json/databases/okta_groups.json");
        PaginatedResponse<OktaGroup> paginatedGroups = OBJECT_MAPPER.readValue(groupsInput,
                OBJECT_MAPPER.getTypeFactory().constructParametricType(PaginatedResponse.class, OktaGroup.class));
        List<OktaGroup> groups = paginatedGroups.getResponse().getRecords();
        List<DbOktaGroup> dbGroups = new ArrayList<>();
        for (OktaGroup group : groups) {
            dbGroups.add(DbOktaGroup.fromOktaGroup(group, integrationId, currentTime));
        }
        dbGroups.forEach(group -> oktaAggService.insert(COMPANY, group));
        users.forEach(user -> {
            if (user.getAssociatedMembers() != null) {
                user.getAssociatedMembers().forEach(associatedMembers -> {
                    associatedMembers.getAssociatedMembers().forEach(memberId -> {
                        oktaAggService.insert(COMPANY,
                                DbOktaAssociation.fromOktaAssociation(user.getId(), memberId, associatedMembers, integrationId, currentTime));
                    });
                });
            }
        });

        jiraOktaService = new JiraOktaService(dataSource, jiraTestDbs.getJiraConditionsBuilder(), oktaAggService);
    }

    @Test
    public void test() throws SQLException {
        assertThat(oktaAggService.list(COMPANY, 0, 100).getCount()).isEqualTo(7);
        assertThat(oktaAggService.listAssociations(COMPANY, 0, 100).getCount()).isEqualTo(2);
        assertThat(oktaAggService.lisGroups(COMPANY, 0, 100).getCount()).isEqualTo(6);
        List<DbAggregationResult> counts = jiraOktaService.groupJiraIssuesWithOkta(COMPANY,
                JiraIssuesFilter.builder()
                        .ingestedAt(ingestedAt)
                        .hygieneCriteriaSpecs(Map.of())
                        .integrationIds(List.of("1")).build(),
                OktaGroupsFilter.builder()
                        .integrationIds(List.of("1")).build(), null).getRecords();
        assertThat(counts.size()).isEqualTo(5);
        Map<String, Long> groupCounts = new HashMap<>();
        counts.forEach(gc -> groupCounts.put(gc.getKey(), gc.getCount()));
        assertThat(groupCounts.get("Everyone")).isEqualTo(9L);
        assertThat(groupCounts.get("ProjectManagers")).isEqualTo(0L);
        assertThat(groupCounts.get("Engineering")).isEqualTo(9L);
        assertThat(groupCounts.get("Sales")).isEqualTo(0L);
    }
}
