package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.runbooks.RunbookTemplate;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RunbookTemplateDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    List<RunbookTemplate> templates = List.of(
            RunbookTemplate.builder()
                    .name("Test Playbook")
                    .description("Wow")
                    .metadata(Map.of("field", "abc"))
                    .data(Map.of("some", "data"))
                    .build(),
            RunbookTemplate.builder()
                    .hidden(true)
                    .name("Hidden Playbook")
                    .description("Sneaky")
                    .metadata(Map.of("test", 123))
                    .category("WIP")
                    .build());

    RunbookTemplateDatabaseService templateDatabaseService;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        templateDatabaseService = new RunbookTemplateDatabaseService(dataSource, DefaultObjectMapper.get());
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));
        templateDatabaseService.populateData = false;
        templateDatabaseService.ensureTableExistence("test");
    }

    @Test
    public void testInsertBulk() throws SQLException {
        int updatedRows = templateDatabaseService.insertBulk("test", templates, true);

        var out = templateDatabaseService.stream("test", null);
        assertThat(out).usingElementComparatorIgnoringFields("id", "createdAt")
                .containsExactlyInAnyOrderElementsOf(List.of(
                        RunbookTemplate.builder()
                                .hidden(false)
                                .name("Test Playbook")
                                .description("Wow")
                                .metadata(Map.of("field", "abc"))
                                .category("LevelOps")
                                .data(Map.of("some", "data"))
                                .build(),
                        RunbookTemplate.builder()
                                .hidden(true)
                                .name("Hidden Playbook")
                                .description("Sneaky")
                                .metadata(Map.of("test", 123))
                                .category("WIP")
                                .data(Map.of())
                                .build()
                ));
        assertThat(updatedRows).isEqualTo(2);
    }

    @Test
    public void test() throws SQLException {
        templateDatabaseService.insertBulk("test", templates, true);

        // --- update
        assertThatThrownBy(() -> templateDatabaseService.update("test", null))
                .isExactlyInstanceOf(UnsupportedOperationException.class);

        // --- filter
        assertThat(templateDatabaseService.stream("test", RunbookTemplateDatabaseService.RunbookTemplateFilter.builder()
                .hidden(false)
                .build()).map(RunbookTemplate::getName))
                .containsExactly("Test Playbook");
        assertThat(templateDatabaseService.stream("test", RunbookTemplateDatabaseService.RunbookTemplateFilter.builder()
                .hidden(true)
                .build()).map(RunbookTemplate::getName))
                .containsExactly("Hidden Playbook");

        assertThat(templateDatabaseService.stream("test", RunbookTemplateDatabaseService.RunbookTemplateFilter.builder()
                .partialName("Dd")
                .build()).map(RunbookTemplate::getName))
                .containsExactly("Hidden Playbook");

        assertThat(templateDatabaseService.stream("test", RunbookTemplateDatabaseService.RunbookTemplateFilter.builder()
                .description("o")
                .build()).map(RunbookTemplate::getName))
                .containsExactly("Test Playbook");

        assertThat(templateDatabaseService.stream("test", RunbookTemplateDatabaseService.RunbookTemplateFilter.builder()
                .categories(List.of("WIP"))
                .build()).map(RunbookTemplate::getName))
                .containsExactly("Hidden Playbook");
    }


    @Test
    public void testPopulate() throws SQLException {
        templateDatabaseService.populateData = true;
        templateDatabaseService.ensureTableExistence("test");

        long count = templateDatabaseService.stream("test", null).count();
        System.out.println("COUNT: " + count);
        assertThat(count).isGreaterThan(0);
    }

}