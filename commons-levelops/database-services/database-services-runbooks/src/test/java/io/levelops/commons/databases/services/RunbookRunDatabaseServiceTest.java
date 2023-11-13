package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.TriggerType;
import io.levelops.commons.databases.models.database.runbooks.Runbook;
import io.levelops.commons.databases.models.database.runbooks.RunbookError;
import io.levelops.commons.databases.models.database.runbooks.RunbookRun;
import io.levelops.commons.databases.models.database.runbooks.RunbookRun.RunbookRunResult;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunState;
import io.levelops.commons.databases.models.database.runbooks.RunbookVariable;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RunbookRunDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    RunbookRunDatabaseService runbookRunDatabaseService;
    RunbookDatabaseService runbookDatabaseService;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        runbookRunDatabaseService = new RunbookRunDatabaseService(dataSource, DefaultObjectMapper.get());
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));
        runbookDatabaseService = new RunbookDatabaseService(dataSource, DefaultObjectMapper.get());
        runbookDatabaseService.ensureTableExistence("test");
        runbookRunDatabaseService.ensureTableExistence("test");
    }

    @Test
    public void test() throws SQLException {
        // runbook_id, trigger_type, args, state
        RunbookRun r = RunbookRun.builder()
                .runbookId("aldskfa;skda")
                .triggerType(TriggerType.MANUAL.toString())
                .args(Map.of(
                        "var", RunbookVariable.builder()
                                .name("var")
                                .build()))
                .build();

        // -- insert with no runbook
        final RunbookRun rr = r;
        assertThatThrownBy(() -> runbookRunDatabaseService.insert("test", rr))
                .isExactlyInstanceOf(DataIntegrityViolationException.class);

       // -- create runbook
        RunbookDatabaseService.InsertResult runbookIds = runbookDatabaseService.insertAndReturnIds("test", Runbook.builder()
                .name("r")
                .triggerType(TriggerType.MANUAL)
                .build())
                .orElseThrow();
        String runbookId = runbookIds.getId();
        String permanentId = runbookIds.getPermanentId();

        // -- insert with runbook
        r = r.toBuilder().runbookId(runbookId).build();
        String id = runbookRunDatabaseService.insert("test", r);

        // -- get
        RunbookRun output = runbookRunDatabaseService.get("test", id).orElse(null);
        assertThat(output).isEqualToIgnoringGivenFields(r.toBuilder()
                .id(id)
                .permanentId(permanentId)
                .state(RunbookRunState.RUNNING)
                .hasWarnings(false)
                .result(RunbookRunResult.builder().build())
                .build(), "createdAt");

        // -- update
        var result = RunbookRunResult.builder()
                .errors(List.of(RunbookError.builder()
                        .type("NPE")
                        .description("dec")
                        .build()))
                .build();
        runbookRunDatabaseService.update("test", RunbookRun.builder()
                .id(id)
                .state(RunbookRunState.FAILURE)
                .hasWarnings(true)
                .result(result)
                .build());

        output = runbookRunDatabaseService.get("test", id).orElse(null);
        assertThat(output).isEqualToIgnoringGivenFields(r.toBuilder()
                .id(id)
                .permanentId(permanentId)
                .state(RunbookRunState.FAILURE)
                .hasWarnings(true)
                .result(result)
                .build(), "createdAt", "stateChangedAt");
        assertThat(output.getStateChangedAt()).isNotNull();

        // -- filter
        List<RunbookRun> records;
        records = runbookRunDatabaseService.filter(0, 10, "test", null, null, null).getRecords();
        assertThat(records.stream().map(RunbookRun::getId)).containsExactly(id);

        records = runbookRunDatabaseService.filter(0, 10, "test", runbookId, null, null).getRecords();
        assertThat(records.stream().map(RunbookRun::getId)).containsExactly(id);

        records = runbookRunDatabaseService.filter(0, 10, "test", "edbb8cbf-3b67-4d73-9393-c51113e59cec", null, null).getRecords();
        assertThat(records.stream().map(RunbookRun::getId)).isEmpty();

        records = runbookRunDatabaseService.filter(0, 10, "test", null, RunbookRunState.FAILURE, null).getRecords();
        assertThat(records.stream().map(RunbookRun::getId)).containsExactly(id);

        records = runbookRunDatabaseService.filter(0, 10, "test", null, RunbookRunState.SUCCESS, null).getRecords();
        assertThat(records.stream().map(RunbookRun::getId)).isEmpty();

        records = runbookRunDatabaseService.filter(0, 10, "test", runbookId, RunbookRunState.FAILURE, null).getRecords();
        assertThat(records.stream().map(RunbookRun::getId)).containsExactly(id);

        // -- delete
        runbookRunDatabaseService.delete("test", id);
        assertThat(runbookRunDatabaseService.get("test", id)).isEmpty();
    }

    @Test
    public void testBulkDelete() throws SQLException {
        RunbookRun r1 = RunbookRun.builder()
                .runbookId("aldskfa;skda")
                .triggerType(TriggerType.MANUAL.toString())
                .args(Map.of(
                        "var1", RunbookVariable.builder()
                                .name("var1")
                                .build()))
                .build();

        final RunbookRun rr1 = r1;
        assertThatThrownBy(() -> runbookRunDatabaseService.insert("test", rr1))
                .isExactlyInstanceOf(DataIntegrityViolationException.class);

        RunbookDatabaseService.InsertResult runbookIds = runbookDatabaseService.insertAndReturnIds("test", Runbook.builder()
                .name("r1")
                .triggerType(TriggerType.MANUAL)
                .build())
                .orElseThrow();
        String runbookId = runbookIds.getId();
        r1 = r1.toBuilder().runbookId(runbookId).build();
        String id1 = runbookRunDatabaseService.insert("test", r1);
        RunbookRun r2 = RunbookRun.builder()
                .runbookId("aldskfa;skda")
                .triggerType(TriggerType.MANUAL.toString())
                .args(Map.of(
                        "var2", RunbookVariable.builder()
                                .name("var2")
                                .build()))
                .build();
        RunbookDatabaseService.InsertResult runbookIds2 = runbookDatabaseService.insertAndReturnIds("test", Runbook.builder()
                .name("r2")
                .triggerType(TriggerType.MANUAL)
                .build())
                .orElseThrow();
        String runbookId2 = runbookIds2.getId();
        r2 = r2.toBuilder().runbookId(runbookId2).build();
        String id2 = runbookRunDatabaseService.insert("test", r2);
        runbookRunDatabaseService.bulkDelete("test", List.of(id1, id2));
        assertThat(runbookRunDatabaseService.get("test", id1)).isEmpty();
        assertThat(runbookRunDatabaseService.get("test", id2)).isEmpty();
    }
}