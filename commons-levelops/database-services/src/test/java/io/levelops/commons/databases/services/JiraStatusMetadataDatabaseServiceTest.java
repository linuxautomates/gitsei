package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.jira.DbJiraStatusMetadata;
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

public class JiraStatusMetadataDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    JiraStatusMetadataDatabaseService databaseService;
    String integrationId;
    String integrationId2;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        databaseService = new JiraStatusMetadataDatabaseService(dataSource);
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
        String id = databaseService.insert("test", DbJiraStatusMetadata.builder()
                .integrationId(integrationId)
                .status("backlog")
                .statusCategory("to do")
                .statusId("123")
                .build());
        // insert unique
        assertThatThrownBy(() -> databaseService.insert("test", DbJiraStatusMetadata.builder()
                .integrationId(integrationId)
                .status("test")
                .statusId("123")
                .build())).hasMessageContaining("duplicate key value violates unique constraint");

        // get
        DbJiraStatusMetadata status = databaseService.get("test", id).orElse(null);
        assertThat(status.getId()).isEqualTo(id);
        assertThat(status.getStatus()).isEqualTo("BACKLOG");
        assertThat(status.getStatusCategory()).isEqualTo("TO DO");
        assertThat(status.getStatusId()).isEqualTo("123");
        assertThat(status.getIntegrationId()).isEqualTo(integrationId);

        // get by sprint id
        status = databaseService.getByStatusId("test", integrationId,"123").orElse(null);
        assertThat(status.getId()).isEqualTo(id);
        assertThat(status.getStatus()).isEqualTo("BACKLOG");
        assertThat(status.getStatusCategory()).isEqualTo("TO DO");
        assertThat(status.getStatusId()).isEqualTo("123");
        assertThat(status.getIntegrationId()).isEqualTo(integrationId);

        // update
        databaseService.update("test", DbJiraStatusMetadata.builder()
                .id(id)
                .statusCategory("done")
                .statusId("456")
                .build());
        status = databaseService.get("test", id).orElse(null);
        assertThat(status.getStatus()).isEqualTo("BACKLOG");
        assertThat(status.getStatusCategory()).isEqualTo("DONE");
        assertThat(status.getStatusId()).isEqualTo("456");
        assertThat(status.getIntegrationId()).isEqualTo(integrationId);

        // filter
        String id2 = databaseService.insert("test", DbJiraStatusMetadata.builder()
                .integrationId(integrationId)
                .status("to do")
                .statusCategory("to do")
                .statusId("2")
                .build());
        String id3 = databaseService.insert("test", DbJiraStatusMetadata.builder()
                .integrationId(integrationId)
                .status("won't do")
                .statusCategory("done")
                .statusId("3")
                .build());
        String id4 = databaseService.insert("test", DbJiraStatusMetadata.builder()
                .integrationId(integrationId2)
                .status("to do")
                .statusCategory("to do")
                .statusId("2")
                .build());

        Stream<DbJiraStatusMetadata> stream = databaseService.stream("test", JiraStatusMetadataDatabaseService.JiraStatusMetadataFilter.builder().build());
        assertThat(stream.map(DbJiraStatusMetadata::getId)).containsExactlyInAnyOrder(id, id2, id3, id4);

        stream = databaseService.stream("test", JiraStatusMetadataDatabaseService.JiraStatusMetadataFilter.builder()
                .integrationId(integrationId)
                .build());
        assertThat(stream.map(DbJiraStatusMetadata::getId)).containsExactlyInAnyOrder(id, id2, id3);

        stream = databaseService.stream("test", JiraStatusMetadataDatabaseService.JiraStatusMetadataFilter.builder()
                .integrationId(integrationId2)
                .build());
        assertThat(stream.map(DbJiraStatusMetadata::getId)).containsExactlyInAnyOrder(id4);

        stream = databaseService.stream("test", JiraStatusMetadataDatabaseService.JiraStatusMetadataFilter.builder()
                .status("tO Do")
                .build());
        assertThat(stream.map(DbJiraStatusMetadata::getId)).containsExactlyInAnyOrder(id2, id4);

        stream = databaseService.stream("test", JiraStatusMetadataDatabaseService.JiraStatusMetadataFilter.builder()
                .partialStatus("dO")
                .build());
        assertThat(stream.map(DbJiraStatusMetadata::getId)).containsExactlyInAnyOrder(id2, id3, id4);

        stream = databaseService.stream("test", JiraStatusMetadataDatabaseService.JiraStatusMetadataFilter.builder()
                .status("dO")
                .build());
        assertThat(stream.map(DbJiraStatusMetadata::getId)).isEmpty();

        stream = databaseService.stream("test", JiraStatusMetadataDatabaseService.JiraStatusMetadataFilter.builder()
                .statusCategory("DoNe")
                .build());
        assertThat(stream.map(DbJiraStatusMetadata::getId)).containsExactlyInAnyOrder(id, id3);

        // get by name
        status = databaseService.getByName("test", integrationId2, "to DO").orElse(null);
        assertThat(status.getStatus()).isEqualTo("TO DO");
        assertThat(status.getStatusCategory()).isEqualTo("TO DO");
        assertThat(status.getStatusId()).isEqualTo("2");
        assertThat(status.getIntegrationId()).isEqualTo(integrationId2);

        // delete
        databaseService.delete("test", status.getId());
        assertThat(databaseService.getByName("test", integrationId2, "to DO")).isEmpty();

        // upsert
        String upsertId = databaseService.upsert("test", DbJiraStatusMetadata.builder()
                .integrationId(integrationId)
                .status("upsert")
                .statusId("456")
                .statusCategory("done")
                .build());
        assertThat(upsertId).isEqualTo(id);
        status = databaseService.get("test", upsertId).orElse(null);
        assertThat(status.getId()).isEqualTo(upsertId);
        assertThat(status.getStatus()).isEqualTo("UPSERT");
        assertThat(status.getStatusCategory()).isEqualTo("DONE");
        assertThat(status.getStatusId()).isEqualTo("456");
        assertThat(status.getIntegrationId()).isEqualTo(integrationId);

        String upsertId2 = databaseService.upsert("test", DbJiraStatusMetadata.builder()
                .integrationId(integrationId)
                .status("new")
                .statusId("newId")
                .statusCategory("newCat")
                .build());
        assertThat(upsertId2).isNotIn(id, id2, id3, id4);
        status = databaseService.get("test", upsertId2).orElse(null);
        assertThat(status.getId()).isEqualTo(upsertId2);
        assertThat(status.getStatus()).isEqualTo("NEW");
        assertThat(status.getStatusCategory()).isEqualTo("NEWCAT");
        assertThat(status.getStatusId()).isEqualTo("newId");
        assertThat(status.getIntegrationId()).isEqualTo(integrationId);

        List<DbJiraStatusMetadata.IntegStatusCategoryMetadata> integStatusCategoryToStatuses =
                databaseService.getIntegStatusCategoryMetadata("test", List.of());
        assertThat(integStatusCategoryToStatuses.size()).isEqualTo(3);

    }
}