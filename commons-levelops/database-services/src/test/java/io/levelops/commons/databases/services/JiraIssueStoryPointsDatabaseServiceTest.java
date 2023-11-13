package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.jira.DbJiraStoryPoints;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JiraIssueStoryPointsDatabaseServiceTest {
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    JiraIssueStoryPointsDatabaseService databaseService;
    String integrationId;
    String integrationId2;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        databaseService = new JiraIssueStoryPointsDatabaseService(dataSource);
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));
        IntegrationService integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence("test");
        databaseService.ensureTableExistence("test");

        integrationId = integrationService.insert("test", Integration.builder()
                .name("name")
                .url("http")
                .status("good")
                .application("jira")
                .description("desc")
                .satellite(true)
                .build());
        integrationId2 = integrationService.insert("test", Integration.builder()
                .name("name2")
                .url("http")
                .status("good")
                .application("jira")
                .description("desc")
                .satellite(true)
                .build());
    }

    @Test
    public void test() throws SQLException {
        // insert
        String id = databaseService.insert("test", DbJiraStoryPoints.builder()
                .integrationId(integrationId)
                .issueKey("LEV-1")
                .storyPoints(42)
                .startTime(1000L)
                .endTime(2000L)
                .build());

        // insert unique
        assertThatThrownBy(() -> databaseService.insert("test", DbJiraStoryPoints.builder()
                .integrationId(integrationId)
                .issueKey("LEV-1")
                .storyPoints(0)
                .startTime(1000L)
                .endTime(9000L)
                .build())).hasMessageContaining("duplicate key value violates unique constraint");

        // get
        DbJiraStoryPoints o = databaseService.get("test", id).orElse(null);
        assertThat(o.getId()).isEqualTo(id);
        assertThat(o.getIntegrationId()).isEqualTo(integrationId);
        assertThat(o.getIssueKey()).isEqualTo("LEV-1");
        assertThat(o.getStoryPoints()).isEqualTo(42);
        assertThat(o.getStartTime()).isEqualTo(1000L);
        assertThat(o.getEndTime()).isEqualTo(2000L);

        // filter
        String id2 = databaseService.insert("test", DbJiraStoryPoints.builder()
                .integrationId(integrationId)
                .issueKey("LEV-1")
                .storyPoints(1)
                .startTime(2000L)
                .endTime(3000L)
                .build());
        String id3 = databaseService.insert("test", DbJiraStoryPoints.builder()
                .integrationId(integrationId)
                .issueKey("LEV-2")
                .storyPoints(42)
                .startTime(2500L)
                .endTime(4500L)
                .build());
        String id4 = databaseService.insert("test", DbJiraStoryPoints.builder()
                .integrationId(integrationId2)
                .issueKey("LEV-1")
                .storyPoints(2)
                .startTime(1000L)
                .endTime(2000L)
                .build());

        Stream<DbJiraStoryPoints> stream = databaseService.stream("test", JiraIssueStoryPointsDatabaseService.JiraStoryPointsFilter.builder().build());
        assertThat(stream.map(DbJiraStoryPoints::getId)).containsExactlyInAnyOrder(id, id2, id3, id4);

        stream = databaseService.stream("test", JiraIssueStoryPointsDatabaseService.JiraStoryPointsFilter.builder()
                .integrationId(integrationId)
                .build());
        assertThat(stream.map(DbJiraStoryPoints::getId)).containsExactlyInAnyOrder(id, id2, id3);

        stream = databaseService.stream("test", JiraIssueStoryPointsDatabaseService.JiraStoryPointsFilter.builder()
                .integrationId(integrationId2)
                .build());
        assertThat(stream.map(DbJiraStoryPoints::getId)).containsExactlyInAnyOrder(id4);

        stream = databaseService.stream("test", JiraIssueStoryPointsDatabaseService.JiraStoryPointsFilter.builder()
                .startTime(1000L)
                .build());
        assertThat(stream.map(DbJiraStoryPoints::getId)).containsExactlyInAnyOrder(id, id4);

        stream = databaseService.stream("test", JiraIssueStoryPointsDatabaseService.JiraStoryPointsFilter.builder()
                .issueKey("LEV-1")
                .build());
        assertThat(stream.map(DbJiraStoryPoints::getId)).containsExactlyInAnyOrder(id, id2, id4);

        // delete
        databaseService.delete("test", id3);
        assertThat(databaseService.get("test", id3)).isEmpty();

        // upsert
        String upsertId = databaseService.upsert("test", DbJiraStoryPoints.builder()
                .integrationId(integrationId)
                .issueKey("LEV-1")
                .storyPoints(12)
                .startTime(2000L)
                .endTime(4000L)
                .build());
        assertThat(upsertId).isEqualTo(id2);
        o = databaseService.get("test", upsertId).orElse(null);
        assertThat(o.getId()).isEqualTo(id2);
        assertThat(o.getIntegrationId()).isEqualTo(integrationId);
        assertThat(o.getIssueKey()).isEqualTo("LEV-1");
        assertThat(o.getStoryPoints()).isEqualTo(12);
        assertThat(o.getStartTime()).isEqualTo(2000L);
        assertThat(o.getEndTime()).isEqualTo(4000L);

    }
}