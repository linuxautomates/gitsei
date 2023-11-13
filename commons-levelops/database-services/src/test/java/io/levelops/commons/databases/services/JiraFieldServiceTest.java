package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraFieldServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    JiraFieldService jiraFieldService;
    IntegrationService integrationService;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        jiraFieldService = new JiraFieldService(dataSource);
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));

        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence("test");
        jiraFieldService.ensureTableExistence("test");
    }

    @Test
    public void test() throws SQLException {
        String id = integrationService.insert("test", Integration.builder().name("123").application("jira").status("a").build());

        jiraFieldService.batchUpsert("test", List.of(DbJiraField.builder()
                .name("Story Points")
                .fieldKey("customfield_10030")
                .custom(true)
                .integrationId(id)
                .fieldType("option")
                .build()));

        DbJiraField field = jiraFieldService.list("test", 0, 1).getRecords().get(0);
        DefaultObjectMapper.prettyPrint(field);

        DbListResponse<DbJiraField> fields = jiraFieldService.listByFilter("test", null, null, null, null, List.of("customfield_10030"), 0, 1);
        assertThat(fields.getRecords()).containsExactly(field);

        fields = jiraFieldService.listByFilter("test", List.of(id), null, null, null, null, 0, 1);
        assertThat(fields.getRecords()).containsExactly(field);
        fields = jiraFieldService.listByFilter("test", List.of("1239999"), null, null, null, null, 0, 1);
        assertThat(fields.getRecords()).isEmpty();

        fields = jiraFieldService.listByFilter("test", null, true, null, null, null, 0, 1);
        assertThat(fields.getRecords()).containsExactly(field);
        fields = jiraFieldService.listByFilter("test", null, false, null, null, null, 0, 1);
        assertThat(fields.getRecords()).isEmpty();

        fields = jiraFieldService.listByFilter("test", null, null, "Story Points", null, null, 0, 1);
        assertThat(fields.getRecords()).containsExactly(field);
        fields = jiraFieldService.listByFilter("test", null, null, "lksdjfls", null, null, 0, 1);
        assertThat(fields.getRecords()).isEmpty();

        fields = jiraFieldService.listByFilter("test", null, null, null, "Story", null, 0, 1);
        assertThat(fields.getRecords()).containsExactly(field);
        fields = jiraFieldService.listByFilter("test", null, null, null, "StoNO", null, 0, 1);
        assertThat(fields.getRecords()).isEmpty();

        fields = jiraFieldService.listByFilter("test", null, null, null, null, null, 0, 1);
        assertThat(fields.getRecords()).containsExactly(field);

    }
}