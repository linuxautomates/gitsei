package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.runbooks.RunbookTemplateCategory;
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

public class RunbookTemplateCategoryDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    List<RunbookTemplateCategory> categories = List.of(
            RunbookTemplateCategory.builder()
                    .name("Test Category")
                    .description("Wow")
                    .metadata(Map.of("field", "abc"))
                    .build(),
            RunbookTemplateCategory.builder()
                    .hidden(true)
                    .name("Hidden Category")
                    .description("Sneaky")
                    .metadata(Map.of("test", 123))
                    .build());

    RunbookTemplateCategoryDatabaseService templateCategoryDatabaseService;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        templateCategoryDatabaseService = new RunbookTemplateCategoryDatabaseService(dataSource, DefaultObjectMapper.get());
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));
        templateCategoryDatabaseService.populateData = false;
        templateCategoryDatabaseService.ensureTableExistence("test");
    }


    @Test
    public void testInsertBulk() throws SQLException {
        int updatedRows = templateCategoryDatabaseService.insertBulk("test", categories, true);

        var out = templateCategoryDatabaseService.stream("test", null);
        assertThat(out).usingElementComparatorIgnoringFields("id", "createdAt")
                .containsExactlyInAnyOrderElementsOf(List.of(
                        RunbookTemplateCategory.builder()
                                .hidden(false)
                                .name("Test Category")
                                .description("Wow")
                                .metadata(Map.of("field", "abc"))
                                .build(),
                        RunbookTemplateCategory.builder()
                                .hidden(true)
                                .name("Hidden Category")
                                .description("Sneaky")
                                .metadata(Map.of("test", 123))
                                .build()
                ));
        assertThat(updatedRows).isEqualTo(2);
    }

    @Test
    public void test() throws SQLException {
        templateCategoryDatabaseService.insertBulk("test", categories, true);

        // --- update
        assertThatThrownBy(() -> templateCategoryDatabaseService.update("test", null))
                .isExactlyInstanceOf(UnsupportedOperationException.class);

        // --- filter
        assertThat(templateCategoryDatabaseService.stream("test", RunbookTemplateCategoryDatabaseService.RunbookTemplateCategoryFilter.builder()
                .hidden(false)
                .build()).map(RunbookTemplateCategory::getName))
                .containsExactly("Test Category");
        assertThat(templateCategoryDatabaseService.stream("test", RunbookTemplateCategoryDatabaseService.RunbookTemplateCategoryFilter.builder()
                .hidden(true)
                .build()).map(RunbookTemplateCategory::getName))
                .containsExactly("Hidden Category");

        assertThat(templateCategoryDatabaseService.stream("test", RunbookTemplateCategoryDatabaseService.RunbookTemplateCategoryFilter.builder()
                .partialName("Dd")
                .build()).map(RunbookTemplateCategory::getName))
                .containsExactly("Hidden Category");

        assertThat(templateCategoryDatabaseService.stream("test", RunbookTemplateCategoryDatabaseService.RunbookTemplateCategoryFilter.builder()
                .name("Dd")
                .build()).map(RunbookTemplateCategory::getName))
                .isEmpty();
        assertThat(templateCategoryDatabaseService.stream("test", RunbookTemplateCategoryDatabaseService.RunbookTemplateCategoryFilter.builder()
                .name("Hidden Category")
                .build()).map(RunbookTemplateCategory::getName))
                .containsExactly("Hidden Category");

        assertThat(templateCategoryDatabaseService.stream("test", RunbookTemplateCategoryDatabaseService.RunbookTemplateCategoryFilter.builder()
                .description("o")
                .build()).map(RunbookTemplateCategory::getName))
                .containsExactly("Test Category");

    }


    @Test
    public void testPopulate() throws SQLException {
        templateCategoryDatabaseService.populateData = true;
        templateCategoryDatabaseService.ensureTableExistence("test");

        long count = templateCategoryDatabaseService.stream("test", null)
                .count();
        System.out.println("COUNT: " + count);
        assertThat(count).isGreaterThan(0);
    }
}