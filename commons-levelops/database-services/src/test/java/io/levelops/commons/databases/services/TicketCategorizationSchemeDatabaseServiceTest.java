package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.utils.IssueMgmtUtil;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.web.exceptions.BadRequestException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.database.TicketCategorizationScheme.Uncategorized.builder;
import static io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService.TicketCategorizationSchemeFilter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TicketCategorizationSchemeDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    TicketCategorizationSchemeDatabaseService databaseService;
    JdbcTemplate template;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        databaseService = new TicketCategorizationSchemeDatabaseService(dataSource, DefaultObjectMapper.get());
        template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));
        databaseService.populateData = false;
        databaseService.ensureTableExistence("test");
    }

    @Test
    public void test() throws SQLException {

        String id1 = databaseService.insert("test", TicketCategorizationScheme.builder()
                .name("scheme-1")
                .build());
        TicketCategorizationScheme output = databaseService.get("test", id1).orElse(null);
        assertThat(output.getName()).isEqualTo("scheme-1");
        assertThat(output.getDefaultScheme()).isFalse();
        assertThat(output.getConfig().getCategories()).isEmpty();

        String id2 = databaseService.insert("test", TicketCategorizationScheme.builder()
                .name("scheme-2")
                .config(TicketCategorizationScheme.TicketCategorizationConfig.builder()
                        .description("desc scheme-2")
                        .integrationType("int-type")
                        .categories(Map.of("cat1", TicketCategorizationScheme.TicketCategorization.builder()
                                //.id("cat1")
                                .index(10)
                                .name("category 1")
                                .description("desc cat1")
                                .filter(Map.of("test", "filter"))
                                .metadata(Map.of("range_filter_choice", Map.of("attribute_name", "attribute_value")))
                                .build()))
                        .build())
                .build());

        output = databaseService.get("test", id2).orElse(null);
        assertThat(output.getName()).isEqualTo("scheme-2");
        assertThat(output.getDefaultScheme()).isFalse();
        assertThat(output.getConfig().getDescription()).isEqualTo("desc scheme-2");
        assertThat(output.getConfig().getIntegrationType()).isEqualTo("int-type");
        assertThat(output.getConfig().getCategories()).hasSize(1);
        assertThat(output.getConfig().getCategories()).containsKeys("10");
        //assertThat(output.getConfig().getCategories().get("10").getId()).isEqualTo("cat1");
        assertThat(output.getConfig().getCategories().get("10").getName()).isEqualTo("category 1");
        assertThat(output.getConfig().getCategories().get("10").getDescription()).isEqualTo("desc cat1");
        assertThat(output.getConfig().getCategories().get("10").getIndex()).isEqualTo(10);
        assertThat(output.getConfig().getCategories().get("10").getFilter()).isEqualTo(Map.of("test", "filter"));
        assertThat(output.getConfig().getCategories().get("10").getMetadata()).isEqualTo(
                Map.of("range_filter_choice", Map.of("attribute_name", "attribute_value")));

        // -- default scheme
        String id3 = databaseService.insert("test", TicketCategorizationScheme.builder()
                .name("scheme-3")
                .defaultScheme(true)
                .build());
        output = databaseService.get("test", id3).orElse(null);
        assertThat(output.getName()).isEqualTo("scheme-3");
        assertThat(output.getDefaultScheme()).isTrue();
        assertThat(databaseService.getDefaultScheme("test").map(TicketCategorizationScheme::getId).orElse(null)).isEqualTo(id3);

        String id4 = databaseService.insert("test", TicketCategorizationScheme.builder()
                .name("scheme-4")
                .defaultScheme(true)
                .build());

        assertThat(databaseService.getDefaultScheme("test").map(TicketCategorizationScheme::getId).orElse(null)).isEqualTo(id4);

        // -- unique name
        assertThatThrownBy(() -> databaseService.insert("test", TicketCategorizationScheme.builder()
                .name("scheme-4")
                .defaultScheme(true)
                .build())).hasMessageContaining("duplicate key value violates unique constraint \"ticket_categorization_schemes_name_index\"");
        assertThatThrownBy(() -> databaseService.insert("test", TicketCategorizationScheme.builder()
                .name("SCHEME-4")
                .defaultScheme(true)
                .build())).hasMessageContaining("duplicate key value violates unique constraint \"ticket_categorization_schemes_name_index\"");

        // --- update

        String id5 = databaseService.insert("test", TicketCategorizationScheme.builder()
                .name("scheme-5")
                .config(TicketCategorizationScheme.TicketCategorizationConfig.builder()
                        .description("desc scheme-5")
                        .integrationType("int-type")
                        .categories(Map.of("1", TicketCategorizationScheme.TicketCategorization.builder()
                                .index(1)
                                .name("category 1")
                                .description("desc cat1")
                                .filter(Map.of("test1", "filter1"))
                                .build(),"2",TicketCategorizationScheme.TicketCategorization.builder()
                                        .index(2)
                                        .name("category 2")
                                        .description("desc cat2")
                                        .filter(Map.of("test2", "filter2"))
                                        .build()))
                        .build())
                .build());

        output = databaseService.get("test",id5).get();
        String categoryId1 = output.getConfig().getCategories().get("1").getId();
        String categoryId2 = output.getConfig().getCategories().get("2").getId();
        TicketCategorizationScheme.TicketCategorization category = databaseService.getCategoryById("test",categoryId1).get();
        Assertions.assertThat(category.getName()).isEqualTo("category 1");

        Boolean update = databaseService.update("test", TicketCategorizationScheme.builder()
                .id(id5)
                .name("scheme-5-edit")
                .config(TicketCategorizationScheme.TicketCategorizationConfig.builder()
                        .categories(Map.of("1", TicketCategorizationScheme.TicketCategorization.builder()
                                .id(categoryId1)
                                .index(1)
                                .name("category 1 - edit")
                                .filter(Map.of("test1 - eidt", "filter1 - edit"))
                                .build(),
                                "2",TicketCategorizationScheme.TicketCategorization.builder()
                                        .index(2)
                                                .id(UUID.randomUUID().toString())
                                        .name("category 3")
                                        .filter(Map.of("test3", "filter3"))
                                        .build(),
                                "3",TicketCategorizationScheme.TicketCategorization.builder()
                                        .index(3)
                                        .id(UUID.randomUUID().toString())
                                        .name("category 4")
                                        .filter(Map.of("test4", "filter4"))
                                        .build()))
                        .build())
                .build());
        assertThat(update).isTrue();
        output = databaseService.get("test", id5).orElse(null);
        assertThat(output.getName()).isEqualTo("scheme-5-edit");
        assertThat(output.getConfig().getCategories().size()).isEqualTo(3);
        List<String> categoryIdsOp = output.getConfig().getCategories().values().stream().map(c -> c.getId()).collect(Collectors.toList());
        assertThat(categoryIdsOp).contains(categoryId1);
        assertThat(categoryIdsOp).doesNotContain(categoryId2);

        assertThat(databaseService.getDefaultScheme("test").map(TicketCategorizationScheme::getId).orElse(null)).isEqualTo(id4);
        databaseService.update("test", TicketCategorizationScheme.builder()
                .id(id1)
                .defaultScheme(true)
                .build());
        assertThat(databaseService.getDefaultScheme("test").map(TicketCategorizationScheme::getId).orElse(null)).isEqualTo(id1);

        // -- list
        assertThat(databaseService.stream("test", null).map(TicketCategorizationScheme::getId))
                .containsExactly(id1, id2, id3, id4, id5);
        assertThat(databaseService.stream("test", TicketCategorizationSchemeFilter.builder()
                .ids(List.of(id2, id3))
                .build()).map(TicketCategorizationScheme::getId))
                .containsExactly(id2, id3);
        assertThat(databaseService.stream("test", TicketCategorizationSchemeFilter.builder()
                .partialName("eDi")
                .build()).map(TicketCategorizationScheme::getId))
                .containsExactly(id5);
        assertThat(databaseService.stream("test", TicketCategorizationSchemeFilter.builder()
                .defaultScheme(true)
                .build()).map(TicketCategorizationScheme::getId))
                .containsExactly(id1);
        assertThat(databaseService.stream("test", TicketCategorizationSchemeFilter.builder()
                .partialName("scheme-1")
                .build()).map(TicketCategorizationScheme::getId))
                .containsExactly(id1);
        assertThat(databaseService.stream("test", TicketCategorizationSchemeFilter.builder()
                .name("scheme-5")
                .build()).map(TicketCategorizationScheme::getId))
                .isEmpty();
        assertThat(databaseService.stream("test", TicketCategorizationSchemeFilter.builder()
                .name("scheme-5-EDIT")
                .build()).map(TicketCategorizationScheme::getId))
                .containsExactly(id5);

        // -- delete
        assertThat(databaseService.delete("test", id2)).isTrue();
        assertThat(databaseService.get("test", id2)).isEmpty();
        assertThat(databaseService.delete("test", id1)).isFalse();

        // -- populate
        template.execute("delete from test.ticket_categorization_schemes;");
        assertThat(databaseService.stream("test", TicketCategorizationSchemeFilter.builder()
                .name("system ticket categorization scheme")
                .build()).map(TicketCategorizationScheme::getId))
                .isEmpty();
        databaseService.populateDefaultData("test");
        assertThat(databaseService.stream("test", TicketCategorizationSchemeFilter.builder()
                .name("system ticket categorization profile")
                .build()).map(TicketCategorizationScheme::getId))
                .hasSize(1);
        databaseService.populateDefaultData("test");
        assertThat(databaseService.stream("test", TicketCategorizationSchemeFilter.builder()
                .name("system ticket categorization profile")
                .build()).map(TicketCategorizationScheme::getId))
                .hasSize(1);

    }

    @Test
    public void retrieveCategories() {
        TicketCategorizationScheme scheme = TicketCategorizationScheme.builder()
                .config(TicketCategorizationScheme.TicketCategorizationConfig.builder()
                        .categories(Map.of("cat1", TicketCategorizationScheme.TicketCategorization.builder()
                                        .index(20)
                                        .name("category 2")
                                        .build(),
                                "cat2", TicketCategorizationScheme.TicketCategorization.builder()
                                        .index(10)
                                        .name("category 1")
                                        .build()))
                        .build())
                .build();
        assertThat(scheme.retrieveCategories()).containsExactly("category 1", "category 2", "Other");
    }

    @Test
    public void retrieveGoals() {
        TicketCategorizationScheme scheme = TicketCategorizationScheme.builder()
                .config(TicketCategorizationScheme.TicketCategorizationConfig.builder()
                        .uncategorized(builder()
                                .goals(TicketCategorizationScheme.Goals.builder()
                                        .enabled(true)
                                        .idealRange(new TicketCategorizationScheme.Goal(20, 30))
                                        .acceptableRange(new TicketCategorizationScheme.Goal(20, 30))
                                        .build())
                                .build())
                        .categories(Map.of(
                                "cat1", TicketCategorizationScheme.TicketCategorization.builder()
                                        .name("category 1")
                                        .goals(TicketCategorizationScheme.Goals.builder()
                                                .enabled(false)
                                                .idealRange(new TicketCategorizationScheme.Goal(20, 30))
                                                .acceptableRange(new TicketCategorizationScheme.Goal(20, 45))
                                                .build())
                                        .build(),
                                "cat2", TicketCategorizationScheme.TicketCategorization.builder()
                                        .name("category 2")
                                        .goals(TicketCategorizationScheme.Goals.builder()
                                                .enabled(true)
                                                .idealRange(new TicketCategorizationScheme.Goal(20, 30))
                                                .acceptableRange(new TicketCategorizationScheme.Goal(20, 45))
                                                .build())
                                        .build(),
                                "cat3", TicketCategorizationScheme.TicketCategorization.builder()
                                        .name("category 3")
                                        .goals(null)
                                        .build()))
                        .build())
                .build();
        assertThat(scheme.retrieveGoals("Other")).isPresent();
        assertThat(scheme.retrieveGoals("category 1")).isEmpty();
        assertThat(scheme.retrieveGoals("category 2")).isPresent();
        assertThat(scheme.retrieveGoals("category 3")).isEmpty();
        assertThat(scheme.retrieveGoals("...")).isEmpty();
    }

    @Test
    public void testIssueMgmt() throws SQLException, BadRequestException {
        String id1 = databaseService.insert("test", TicketCategorizationScheme.builder()
                .name("scheme-2")
                .config(TicketCategorizationScheme.TicketCategorizationConfig.builder()
                        .description("desc scheme-2")
                        .integrationType("int-type")
                        .categories(Map.of(
                                "1", TicketCategorizationScheme.TicketCategorization.builder()
                                        .id("1")
                                        .name("New Features")
                                        .index(10)
                                        .filter(Map.of("workitem_types", List.of("NEW FEATURE", "STORY")))
                                        .build(),
                                "2", TicketCategorizationScheme.TicketCategorization.builder()
                                        .id("2")
                                        .name("Bugs")
                                        .index(20)
                                        .filter(Map.of("workitem_types", List.of("BUG")))
                                        .build(),
                                "3", TicketCategorizationScheme.TicketCategorization.builder()
                                        .id("3")
                                        .name("Technical Tasks")
                                        .index(30)
                                        .filter(Map.of("workitem_types", List.of("TASK")))
                                        .build()
                        ))
                        .build())
                .build());

        List<WorkItemsFilter.TicketCategorizationFilter> filters1 = IssueMgmtUtil.generateTicketCategorizationFilters("test", id1, databaseService);
        Map<String, WorkItemsFilter> testFilters = filters1.stream()
                .collect(Collectors.toMap(WorkItemsFilter.TicketCategorizationFilter::getName, WorkItemsFilter.TicketCategorizationFilter::getFilter));
        assertThat(testFilters.get("New Features").getWorkItemTypes()).isEqualTo(List.of("NEW FEATURE", "STORY"));
        assertThat(testFilters.get("Bugs").getWorkItemTypes()).isEqualTo(List.of("BUG"));
        assertThat(testFilters.get("Technical Tasks").getWorkItemTypes()).isEqualTo(List.of("TASK"));

        String id2 = databaseService.insert("test", TicketCategorizationScheme.builder()
                .name("scheme-3")
                .config(TicketCategorizationScheme.TicketCategorizationConfig.builder()
                        .description("desc scheme-3")
                        .integrationType("int-type")
                        .categories(Map.of())
                        .build())
                .build());

        List<WorkItemsFilter.TicketCategorizationFilter> filters2 = IssueMgmtUtil.generateTicketCategorizationFilters("test", id2, databaseService);
        assertThat(filters2).isEqualTo(null);
    }
}