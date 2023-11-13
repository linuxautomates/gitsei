package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.databases.models.database.KvField;
import io.levelops.commons.databases.models.database.runbooks.RunbookNodeTemplate;
import io.levelops.commons.databases.models.database.runbooks.RunbookNodeTemplate.RunbookOutputField;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.ContentType;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.ValueType;
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
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RunbookNodeTemplateDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    RunbookNodeTemplateDatabaseService runbookNodeTemplateDatabaseService;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        runbookNodeTemplateDatabaseService = new RunbookNodeTemplateDatabaseService(dataSource, DefaultObjectMapper.get());
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));
        runbookNodeTemplateDatabaseService.populateData = false;
        runbookNodeTemplateDatabaseService.ensureTableExistence("test");
    }

    @Test
    public void testSerial() throws JsonProcessingException {
        RunbookNodeTemplate runbookNodeTemplate = DefaultObjectMapper.get().readValue("{\"options\":[]}", RunbookNodeTemplate.class);
        DefaultObjectMapper.prettyPrint(runbookNodeTemplate);
        RunbookNodeTemplate nodeTemplate = RunbookNodeTemplate.builder()
                .type("slack_notification")
                .name("Send Slack notification")
                .description("Sends a Slack message to a user")
                .inputField(KvField.builder()
                        .key("user_email")
                        .required(true)
                        .type("dynamic-single-dropdown")
                        .dynamicResourceName("users")
                        .searchField("email")
                        .build())
                .inputField(KvField.builder()
                        .key("text")
                        .required(true)
                        .type("text")
                        .build())
                .inputField(KvField.builder()
                        .key("bot_name")
                        .required(false)
                        .type("text")
                        .build())
                .uiData(Map.of())
                .build();
        DefaultObjectMapper.prettyPrint(nodeTemplate);

    }

    @Test
    public void test() throws SQLException {
        RunbookNodeTemplate nodeTemplate = RunbookNodeTemplate.builder()
                .type("if_condition")
                .nodeHandler("disp")
                .name("If ... Then")
                .hidden(false)
                .description("Evaluates condition")
                .category("control flow")
                .inputField(KvField.builder()
                        .key("condition")
                        .required(true)
                        .index(10)
                        .build())
                .outputField(RunbookOutputField.builder()
                        .kvField(KvField.builder()
                                .key("result")
                                .contentType(ContentType.fromString("integration/jira/issues"))
                                .valueType(ValueType.JSON_BLOB)
                                .build())
                        .contentTypeFromInput("test")
                        .build())
                .options(List.of("true", "false", "any"))
                .uiData(Map.of("some", "data"))
                .build();
        String id = runbookNodeTemplateDatabaseService.insert("test", nodeTemplate);

        // --- get
        Optional<RunbookNodeTemplate> output = runbookNodeTemplateDatabaseService.get("test", id);
        assertThat(output).isPresent();
        DefaultObjectMapper.prettyPrint(output.get());
        assertThat(output.get()).isEqualToIgnoringGivenFields(nodeTemplate, "id", "createdAt");

        // --- update
        assertThatThrownBy(() -> runbookNodeTemplateDatabaseService.update("test", null))
                .isExactlyInstanceOf(UnsupportedOperationException.class);

        // --- filter
        assertThat(runbookNodeTemplateDatabaseService.filter(0, 10, "test", null, null, null, null, null)
                .getRecords().stream().map(RunbookNodeTemplate::getId))
                .containsExactly(id);
        assertThat(runbookNodeTemplateDatabaseService.filter(0, 10, "test", List.of("if_condition"), null, null, null, null)
                .getRecords().stream().map(RunbookNodeTemplate::getId))
                .containsExactly(id);
        assertThat(runbookNodeTemplateDatabaseService.filter(0, 10, "test", null, "...", null, null, null)
                .getRecords().stream().map(RunbookNodeTemplate::getId))
                .containsExactly(id);
        assertThat(runbookNodeTemplateDatabaseService.filter(0, 10, "test", List.of("lkjflaksjflksdfa"), null, null, null, null)
                .getRecords()).isEmpty();
        assertThat(runbookNodeTemplateDatabaseService.filter(0, 10, "test", List.of("if_condition", "kjfklsdjflksdf"), "...", null, null, null)
                .getRecords().stream().map(RunbookNodeTemplate::getId))
                .containsExactly(id);

        // --- delete
        runbookNodeTemplateDatabaseService.delete("test", id);
        assertThat(runbookNodeTemplateDatabaseService.get("test", id)).isEmpty();
    }


    @Test
    public void testInsertBulk() throws SQLException {
        RunbookNodeTemplate nodeTemplate = RunbookNodeTemplate.builder()
                .nodeHandler("if_condition")
                .name("If ... Then")
                .description("Evaluates condition")
                .inputField(KvField.builder()
                        .key("condition")
                        .required(true)
                        .build())
                .outputField(RunbookOutputField.builder()
                        .kvField(KvField.builder()
                                .key("result")
                                .build())
                        .build())
                .options(List.of("true", "false", "any"))
                .uiData(Map.of("some", "data"))
                .build();
        int updatedRows = runbookNodeTemplateDatabaseService.insertBulk("test", List.of(
                nodeTemplate.toBuilder().type("a").category("cat2").build(),
                nodeTemplate.toBuilder().type("b").category("cat1").build()), true);
        assertThat(updatedRows).isEqualTo(2);

        List<String> ids = runbookNodeTemplateDatabaseService.filter(0, 10, "test", null, null, null, null, null)
                .getRecords().stream().map(RunbookNodeTemplate::getId).collect(Collectors.toList());
        assertThat(ids).hasSize(2);

        assertThat(runbookNodeTemplateDatabaseService.filter(0, 10, "test", null, null, List.of(), null, null)
                .getRecords()).hasSize(2);
        assertThat(runbookNodeTemplateDatabaseService.filter(0, 10, "test", null, null, ids, null, null)
                .getRecords()).hasSize(2);
        assertThat(runbookNodeTemplateDatabaseService.filter(0, 10, "test", null, null, List.of(ids.get(0), "123", "456"), null, null)
                .getRecords()).hasSize(1);

        DbListResponse<String> response = runbookNodeTemplateDatabaseService.listCategories("test");
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response.getRecords()).containsExactly("cat1", "cat2");

    }

    @Test
    public void testPopulate() throws SQLException {
        runbookNodeTemplateDatabaseService.populateData = true;
        runbookNodeTemplateDatabaseService.ensureTableExistence("test");

        long count = runbookNodeTemplateDatabaseService.stream("test", null, null, null, null, null)
                .count();
        System.out.println("COUNT: " + count);
        assertThat(count).isGreaterThan(0);
    }

}