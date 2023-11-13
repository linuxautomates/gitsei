package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.jira.DbJiraIssueSprintMapping;
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

import static io.levelops.commons.databases.services.JiraIssueSprintMappingDatabaseService.JiraIssueSprintMappingFilter.builder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JiraIssueSprintMappingDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    JiraIssueSprintMappingDatabaseService databaseService;
    String integrationId;
    String integrationId2;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        databaseService = new JiraIssueSprintMappingDatabaseService(dataSource);
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
        String id = databaseService.insert("test", DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .issueKey("LEV-1")
                .sprintId("1")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(true)
                .storyPointsPlanned(2)
                .storyPointsDelivered(3)
                .build());

        // insert unique
        assertThatThrownBy(() -> databaseService.insert("test", DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .issueKey("LEV-1")
                .sprintId("1")
                .addedAt(11L)
                .build())).hasMessageContaining("duplicate key value violates unique constraint");

        // get
        DbJiraIssueSprintMapping o = databaseService.get("test", id).orElse(null);
        assertThat(o.getId()).isEqualTo(id);
        assertThat(o.getIntegrationId()).isEqualTo(integrationId);
        assertThat(o.getIssueKey()).isEqualTo("LEV-1");
        assertThat(o.getSprintId()).isEqualTo("1");
        assertThat(o.getAddedAt()).isEqualTo(10L);
        assertThat(o.getPlanned()).isEqualTo(true);
        assertThat(o.getDelivered()).isEqualTo(true);
        assertThat(o.getOutsideOfSprint()).isEqualTo(true);
        assertThat(o.getIgnorableIssueType()).isEqualTo(true);
        assertThat(o.getStoryPointsPlanned()).isEqualTo(2);
        assertThat(o.getStoryPointsDelivered()).isEqualTo(3);


        // filter
        String id2 = databaseService.insert("test", DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .integrationId(integrationId)
                .issueKey("LEV-1")
                .sprintId("2")
                .addedAt(20L)
                .planned(false)
                .delivered(false)
                .storyPointsPlanned(10)
                .storyPointsDelivered(1)
                .build());
        String id3 = databaseService.insert("test", DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId)
                .issueKey("LEV-2")
                .sprintId("1")
                .addedAt(10L)
                .planned(true)
                .delivered(true)
                .storyPointsPlanned(20)
                .storyPointsDelivered(30)
                .build());
        String id4 = databaseService.insert("test", DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId2)
                .issueKey("LEV-1")
                .sprintId("1")
                .addedAt(10L)
                .planned(false)
                .delivered(true)
                .storyPointsPlanned(1)
                .storyPointsDelivered(2)
                .build());
        o = databaseService.get("test", id4).orElse(null);
        assertThat(o.getId()).isEqualTo(id4);
        assertThat(o.getIntegrationId()).isEqualTo(integrationId2);
        assertThat(o.getIssueKey()).isEqualTo("LEV-1");
        assertThat(o.getSprintId()).isEqualTo("1");
        assertThat(o.getAddedAt()).isEqualTo(10L);
        assertThat(o.getPlanned()).isEqualTo(false);
        assertThat(o.getDelivered()).isEqualTo(true);
        assertThat(o.getOutsideOfSprint()).isEqualTo(false);
        assertThat(o.getIgnorableIssueType()).isEqualTo(false);

        Stream<DbJiraIssueSprintMapping> stream = databaseService.stream("test", builder().build());
        assertThat(stream.map(DbJiraIssueSprintMapping::getId)).containsExactlyInAnyOrder(id, id2, id3, id4);

        System.out.println("id1="+ id);
        System.out.println("id2="+ id2);
        System.out.println("id3="+ id3);
        System.out.println("id4="+ id4);
        stream = databaseService.filter(0, 2, "test", builder().build()).getRecords().stream();
        assertThat(stream.map(DbJiraIssueSprintMapping::getId)).containsExactlyInAnyOrder(id, id3);
        stream = databaseService.filter(1, 2, "test", builder().build()).getRecords().stream();
        assertThat(stream.map(DbJiraIssueSprintMapping::getId)).containsExactlyInAnyOrder(id2, id4);

        stream = databaseService.stream("test", builder()
                .integrationIds(List.of(integrationId))
                .build());
        assertThat(stream.map(DbJiraIssueSprintMapping::getId)).containsExactlyInAnyOrder(id, id2, id3);

        stream = databaseService.stream("test", builder()
                .integrationIds(List.of(integrationId2))
                .build());
        assertThat(stream.map(DbJiraIssueSprintMapping::getId)).containsExactlyInAnyOrder(id4);

        stream = databaseService.stream("test", builder()
                .integrationIds(List.of(integrationId, integrationId2))
                .build());
        assertThat(stream.map(DbJiraIssueSprintMapping::getId)).containsExactlyInAnyOrder(id, id2, id3, id4);


        stream = databaseService.stream("test",builder()
                .sprintIds(List.of("1"))
                .build());
        assertThat(stream.map(DbJiraIssueSprintMapping::getId)).containsExactlyInAnyOrder(id, id3, id4);

        stream = databaseService.stream("test",builder()
                .sprintIds(List.of("1", "2"))
                .build());
        assertThat(stream.map(DbJiraIssueSprintMapping::getId)).containsExactlyInAnyOrder(id, id2, id3, id4);

        stream = databaseService.stream("test", builder()
                .issueKey("LEV-1")
                .build());
        assertThat(stream.map(DbJiraIssueSprintMapping::getId)).containsExactlyInAnyOrder(id, id2, id4);

        stream = databaseService.stream("test", builder()
                .outsideOfSprint(true)
                .build());
        assertThat(stream.map(DbJiraIssueSprintMapping::getId)).containsExactlyInAnyOrder(id);

        stream = databaseService.stream("test", builder()
                .ignorableIssueType(true)
                .build());
        assertThat(stream.map(DbJiraIssueSprintMapping::getId)).containsExactlyInAnyOrder(id);

        // delete
        databaseService.delete("test", id3);
        assertThat(databaseService.get("test", id3)).isEmpty();


        // upsert
        String upsertId = databaseService.upsert("test", DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId2)
                .issueKey("LEV-1")
                .sprintId("1")
                .addedAt(8L)
                .planned(false)
                .delivered(true)
                .outsideOfSprint(true)
                .ignorableIssueType(true)
                .storyPointsPlanned(6)
                .storyPointsDelivered(3)
                .removedMidSprint(false)
                .build());
        assertThat(upsertId).isEqualTo(id4);
        o = databaseService.get("test", upsertId).orElse(null);
        assertThat(o.getId()).isEqualTo(id4);
        assertThat(o.getIntegrationId()).isEqualTo(integrationId2);
        assertThat(o.getIssueKey()).isEqualTo("LEV-1");
        assertThat(o.getSprintId()).isEqualTo("1");
        assertThat(o.getAddedAt()).isEqualTo(10L);
        assertThat(o.getPlanned()).isEqualTo(false);
        assertThat(o.getDelivered()).isEqualTo(true);
        assertThat(o.getOutsideOfSprint()).isEqualTo(true);
        assertThat(o.getIgnorableIssueType()).isEqualTo(true);
        assertThat(o.getStoryPointsPlanned()).isEqualTo(6);
        assertThat(o.getStoryPointsDelivered()).isEqualTo(3);

        upsertId = databaseService.upsert("test", DbJiraIssueSprintMapping.builder()
                .integrationId(integrationId2)
                .issueKey("LEV-1")
                .sprintId("1")
                .addedAt(8L)
                .planned(true)
                .delivered(false)
                .outsideOfSprint(true)
                .ignorableIssueType(true)
                .storyPointsPlanned(5)
                .storyPointsDelivered(12)
                .build());
        assertThat(upsertId).isEqualTo(id4);
        o = databaseService.get("test", upsertId).orElse(null);
        assertThat(o.getId()).isEqualTo(id4);
        assertThat(o.getIntegrationId()).isEqualTo(integrationId2);
        assertThat(o.getIssueKey()).isEqualTo("LEV-1");
        assertThat(o.getSprintId()).isEqualTo("1");
        assertThat(o.getAddedAt()).isEqualTo(10L);
        assertThat(o.getPlanned()).isEqualTo(true);
        assertThat(o.getDelivered()).isEqualTo(false);
        assertThat(o.getOutsideOfSprint()).isEqualTo(true);
        assertThat(o.getIgnorableIssueType()).isEqualTo(true);
        assertThat(o.getStoryPointsPlanned()).isEqualTo(5);
        assertThat(o.getStoryPointsDelivered()).isEqualTo(12);
    }

}