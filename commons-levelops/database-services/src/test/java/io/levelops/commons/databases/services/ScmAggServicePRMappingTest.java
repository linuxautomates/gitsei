package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class ScmAggServicePRMappingTest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static JiraIssueService jiraIssueService;
    private static UserIdentityService userIdentityService;

    private static String gitHubIntegrationId;
    final DbScmUser testScmUser = DbScmUser.builder()
            .integrationId(gitHubIntegrationId)
            .cloudId("sid-levelops")
            .displayName("sid-levelops")
            .build();

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null) {
            return;
        }

        dataSource = DatabaseTestUtils.setUpDataSource(pg, company);

        DatabaseTestUtils.JiraTestDbs jiraTestDbs = DatabaseTestUtils.setUpJiraServices(dataSource, company);
        jiraIssueService = jiraTestDbs.getJiraIssueService();
        userIdentityService = jiraTestDbs.getUserIdentityService();

        IntegrationService integrationService = jiraTestDbs.getIntegrationService();
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        gitHubIntegrationId = integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        jiraIssueService.ensureTableExistence(company);
    }

    private DbScmPullRequest createPR(String issueKey, String workItemId, String integrationId, String repoId) {
        Long now = Instant.now().getEpochSecond();
        var pr = DbScmPullRequest.builder()
                .creator("sid")
                .title("example-test")
                .project("test-sid-project")
                .state("closed")
                .integrationId(integrationId)
                .merged(true)
                .number("1")
                .creatorId(testScmUser.getId())
                .creatorInfo(testScmUser)
                .repoIds(List.of(repoId))
                .labels(List.of("sid-label"))
                .commitShas(List.of("abcdefgh"))
                .prUpdatedAt(now)
                .prCreatedAt(now)
                .createdAt(now);
        if (!StringUtils.isEmpty(issueKey)) {
            pr.issueKeys(List.of(issueKey));
        } else {
            pr.workitemIds(List.of(workItemId));
        }
        return pr.build();
    }

    @Test
    public void testInsertPRJira() throws SQLException {
        String issueKey = "PROP-123";
        String integrationId = "1";
        String prId = scmAggService.insert(company, createPR(issueKey, null, integrationId, "repo-1"));

        var statement = dataSource.getConnection().prepareStatement(
                "SELECT * FROM test.scm_pullrequests_jira_mappings WHERE pr_uuid = ?");
        statement.setObject(1, UUID.fromString(prId));
        var rs = statement.executeQuery();
        rs.next();
        assertThat(rs.getString("issue_key")).isEqualTo("PROP-123");
        assertThat(rs.getObject("pr_uuid")).isEqualTo(UUID.fromString(prId));
        assertThat(rs.getString("scm_integration_id")).isEqualTo("1");

        // Create a new PR with the same parameters, except the repo and it should still go through because the
        // unique constraint is on the pr uuid now
        String prId2 = scmAggService.insert(company, createPR(issueKey, null, integrationId, "repo-2"));
        var count_rs = dataSource.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM test.scm_pullrequests_jira_mappings").executeQuery();
        count_rs.next();
        assertThat(count_rs.getInt(1)).isEqualTo(2);
    }

    @Test
        public void testInsertPRWorkItem() throws SQLException {
        String workItemKey = "PROP-123";
        String integrationId = "1";
        String prId = scmAggService.insert(company, createPR(null, workItemKey, integrationId, "repo-1"));

        var statement = dataSource.getConnection().prepareStatement(
                "SELECT * FROM test.scm_pullrequests_workitem_mappings WHERE pr_uuid = ?");
        statement.setObject(1, UUID.fromString(prId));
        var rs = statement.executeQuery();
        rs.next();
        assertThat(rs.getString("workitem_id")).isEqualTo("PROP-123");
        assertThat(rs.getObject("pr_uuid")).isEqualTo(UUID.fromString(prId));
        assertThat(rs.getString("scm_integration_id")).isEqualTo("1");

        // Create a new PR with the same parameters, except the repo and it should still go through because the
        // unique constraint is on the pr uuid now
        String prId2 = scmAggService.insert(company, createPR(null, workItemKey, integrationId, "repo-2"));
        var count_rs = dataSource.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM test.scm_pullrequests_workitem_mappings").executeQuery();
        count_rs.next();
        assertThat(count_rs.getInt(1)).isEqualTo(2);
    }
}
