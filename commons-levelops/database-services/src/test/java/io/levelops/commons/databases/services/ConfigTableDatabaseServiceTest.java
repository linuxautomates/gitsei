package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigTableDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    ConfigTableDatabaseService configTableDatabaseService;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        configTableDatabaseService = new ConfigTableDatabaseService(dataSource, DefaultObjectMapper.get());
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "CREATE SCHEMA IF NOT EXISTS test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));
        configTableDatabaseService.ensureTableExistence("test");
    }

    @Test
    public void test() throws SQLException {
        String id = configTableDatabaseService.insert("test", ConfigTable.builder()
                .name("my table")
                .createdBy("max")
                .build());

        ConfigTable table = configTableDatabaseService.get("test", id).orElse(null);
        DefaultObjectMapper.prettyPrint(table);

        // -- update
        configTableDatabaseService.update("test", ConfigTable.builder()
                .id(id)
                .name("my table 2")
                .schema(ConfigTable.Schema.builder()
                        .columns(Map.of("a", ConfigTable.Column.builder()
                                .id("a")
                                .key("a")
                                .index(10)
                                .required(false)
                                .type("string")
                                .displayName("A")
                                .build()))
                        .build())
                .totalRows(1)
                .rows(Map.of("1", ConfigTable.Row.builder()
                        .id("1")
                        .index(10)
                        .values(Map.of("a", "1", "b", "2"))
                        .build()))
                .version("2")
                .history(Map.of("1", ConfigTable.Revision.builder()
                        .version("1")
                        .userId("path")
                        .createdAt(Instant.now())
                        .build()))
                .updatedBy("mb")
                .build());


        // -- get
        table = configTableDatabaseService.get("test", id).orElse(null);
        DefaultObjectMapper.prettyPrint(table);
        assertThat(table.getSchema().getColumns()).hasSize(1);
        assertThat(table.getRows()).containsKey("1");
        assertThat(table.getRows().get("1").getId()).isEqualTo("1");
        assertThat(table.getRows().get("1").getIndex()).isEqualTo(10);
        assertThat(table.getRows().get("1").getValues()).containsExactlyInAnyOrderEntriesOf(Map.of("a", "1", "b", "2"));
        assertThat(table.getTotalRows()).isEqualTo(1);
        assertThat(table.getHistory()).hasSize(1);
        assertThat(table.getVersion()).isEqualTo("2");
        assertThat(table.getUpdatedBy()).isEqualTo("mb");
        assertThat(table.getUpdatedAt()).isNotNull();

        // -- increment version and update and return old
        Optional<String> update = configTableDatabaseService.updateAndReturnVersion("test", ConfigTable.builder()
                .id(id)
                .version(ConfigTableDatabaseService.INCREMENT_VERSION)
                .name("my table 3")
                .rows(Map.of(
                        "1", ConfigTable.Row.builder().id("1").values(Map.of("a", "1")).build(),
                        "2", ConfigTable.Row.builder().id("2").values(Map.of("a", "2")).build()))
                .build());
        assertThat(update).contains("3");

        table = configTableDatabaseService.get("test", id).orElse(null);
        assertThat(table.getVersion()).isEqualTo("3");
        assertThat(table.getName()).isEqualTo("my table 3");
        assertThat(table.getRows()).hasSize(2);

        // -- insert revision
        assertThat(table.getHistory()).hasSize(1);
        configTableDatabaseService.insertRevision("test", id, ConfigTable.Revision.builder()
                .version("2")
                .userId("maxime")
                .createdAt(Instant.now())
                .build());
        table = configTableDatabaseService.get("test", id).orElse(null);
        DefaultObjectMapper.prettyPrint(table);
        assertThat(table.getHistory().keySet()).containsExactly("1", "2");
        assertThat(table.getHistory().get("2").getUserId()).isEqualTo("maxime");
        configTableDatabaseService.insertRevision("test", id, ConfigTable.Revision.builder()
                .version("3")
                .userId("maxime")
                .createdAt(Instant.now())
                .build());
        configTableDatabaseService.insertRevision("test", id, ConfigTable.Revision.builder()
                .version("4")
                .userId("maxime")
                .createdAt(Instant.now())
                .build());
        table = configTableDatabaseService.get("test", id).orElse(null);
        assertThat(table.getHistory().keySet()).containsExactly("1", "2", "3", "4");

        // -- delete revisions
        configTableDatabaseService.deleteRevisions("test", id, List.of("2", "4"));
        table = configTableDatabaseService.get("test", id).orElse(null);
        assertThat(table.getHistory().keySet()).containsExactly("1", "3");

        // -- filter
        Stream<String> ids;
        ids = configTableDatabaseService.filter(0, 10, "test", null, null,null, EnumSet.noneOf(ConfigTableDatabaseService.Field.class)).getRecords().stream().map(ConfigTable::getId);
        assertThat(ids).containsExactly(id);
        // name
        ids = configTableDatabaseService.filter(0, 10, "test", null, null,"tab", EnumSet.noneOf(ConfigTableDatabaseService.Field.class)).getRecords().stream().map(ConfigTable::getId);
        assertThat(ids).containsExactly(id);
        ids = configTableDatabaseService.filter(0, 10, "test", null, null,"ljsdfjlsdf", EnumSet.noneOf(ConfigTableDatabaseService.Field.class)).getRecords().stream().map(ConfigTable::getId);
        assertThat(ids).isEmpty();
        // ids
        ids = configTableDatabaseService.filter(0, 10, "test", List.of(id), null,null, EnumSet.noneOf(ConfigTableDatabaseService.Field.class)).getRecords().stream().map(ConfigTable::getId);
        assertThat(ids).containsExactly(id);
        ids = configTableDatabaseService.filter(0, 10, "test", List.of("aljkfsdlkg"), null,null, EnumSet.noneOf(ConfigTableDatabaseService.Field.class)).getRecords().stream().map(ConfigTable::getId);
        assertThat(ids).isEmpty();

        //expand
        table = configTableDatabaseService.filter(0, 10, "test", List.of(id),null, null, EnumSet.noneOf(ConfigTableDatabaseService.Field.class)).getRecords().get(0);
        assertThat(table.getSchema()).isNull();
        assertThat(table.getRows()).isNull();
        assertThat(table.getHistory()).isNull();
        table = configTableDatabaseService.filter(0, 10, "test", List.of(id), null,null, EnumSet.of(ConfigTableDatabaseService.Field.SCHEMA)).getRecords().get(0);
        assertThat(table.getSchema().getColumns()).hasSize(1);
        assertThat(table.getRows()).isNull();
        assertThat(table.getHistory()).isNull();
        table = configTableDatabaseService.filter(0, 10, "test", List.of(id), null,null, EnumSet.of(ConfigTableDatabaseService.Field.ROWS)).getRecords().get(0);
        assertThat(table.getSchema()).isNull();
        assertThat(table.getRows()).hasSize(2);
        assertThat(table.getHistory()).isNull();
        table = configTableDatabaseService.filter(0, 10, "test", List.of(id), null,null, EnumSet.of(ConfigTableDatabaseService.Field.HISTORY)).getRecords().get(0);
        assertThat(table.getSchema()).isNull();
        assertThat(table.getRows()).isNull();
        assertThat(table.getHistory()).hasSize(2);
    }
}