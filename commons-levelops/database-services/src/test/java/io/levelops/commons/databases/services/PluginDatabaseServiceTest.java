package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Plugin;
import io.levelops.commons.databases.models.database.Plugin.PluginClass;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    PluginDatabaseService pluginService;

    private final Set<String> tools = Set.of(
            "sast_brakeman",
            "report_ms_tmt",
            "report_praetorian",
            "report_nccgroup",
            "csv");

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        pluginService = new PluginDatabaseService(dataSource, DefaultObjectMapper.get());
        JdbcTemplate template = pluginService.template.getJdbcTemplate();
        template.execute(
                "DROP SCHEMA IF EXISTS test CASCADE;" +
                        "CREATE SCHEMA test;"
        );
        System.out.println(template.queryForObject("SELECT current_database();", String.class));
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        pluginService.ensureTableExistence("test");
    }

    @Test
    public void test() throws SQLException {
        pluginService.ensureTableExistence("test");
        String id = pluginService.insert("test", Plugin.builder()
                .custom(true)
                .pluginClass(PluginClass.REPORT_FILE)
                .tool("mytool")
                .version("1.0")
                .name("cool tool")
                .description("bool rule")
                .readme(Map.of("usage", "-h"))
                .gcsPath("/download/me")
                .build());
        System.out.println(id);
        Optional<Plugin> out = pluginService.get("test", id);
        assertThat(out).isPresent();
        assertThat(out.get().getGcsPath()).isEqualTo("/download/me");
        assertThat(out.get().getCustom()).isTrue();
        assertThat(pluginService.getByTool("test", "mytool")).isPresent();
        assertThat(pluginService.getByTool("test", "random")).isEmpty();

        assertThat(pluginService.list("test", 0, 10).getRecords()).hasSize(tools.size() + 1);
        assertThat(pluginService.listByFilter("test", "cool", true, 0, 10).getRecords()).hasSize(1);
        assertThat(pluginService.listByFilter("test", "cool", null, 0, 10).getRecords()).hasSize(1);
        assertThat(pluginService.listByFilter("test", "cool", false, 0, 10).getRecords()).hasSize(0);

    }

    @Test
    public void testLevelOpsPluginsInitialization() throws SQLException {
        pluginService.ensureTableExistence("test");
        var plugins = pluginService.list("test", 0, 20);
        int found = 0;
        for(var record:plugins.getRecords()){
            if(tools.contains(record.getTool())){
                found++;
            }
        }
        assertThat(found).isEqualTo(tools.size());
    }
}