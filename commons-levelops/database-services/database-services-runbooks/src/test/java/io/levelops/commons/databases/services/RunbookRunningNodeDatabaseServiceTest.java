package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.TriggerType;
import io.levelops.commons.databases.models.database.runbooks.Runbook;
import io.levelops.commons.databases.models.database.runbooks.RunbookError;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunningNode.RunbookRunningNodeResult;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunningNodeState;
import io.levelops.commons.databases.models.database.runbooks.RunbookRun;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunningNode;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RunbookRunningNodeDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    RunbookRunningNodeDatabaseService runningNodeDatabaseService;
    RunbookDatabaseService runbookDatabaseService;
    RunbookRunDatabaseService runbookRunDatabaseService;
    String runbookId;
    String runId;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        runningNodeDatabaseService = new RunbookRunningNodeDatabaseService(dataSource, DefaultObjectMapper.get());
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));
        runbookDatabaseService = new RunbookDatabaseService(dataSource, DefaultObjectMapper.get());
        runbookRunDatabaseService = new RunbookRunDatabaseService(dataSource, DefaultObjectMapper.get());
        runbookDatabaseService.ensureTableExistence("test");
        runbookRunDatabaseService.ensureTableExistence("test");
        runningNodeDatabaseService.ensureTableExistence("test");

        runbookId = runbookDatabaseService.insert("test", Runbook.builder()
                .name("r")
                .triggerType(TriggerType.MANUAL)
                .build());
        runId = runbookRunDatabaseService.insert("test", RunbookRun.builder()
                .runbookId(runbookId)
                .triggerType(TriggerType.MANUAL.toString())
                .build());

    }

    @Test
    public void test() throws SQLException {
        // run_id, node_id, state
        RunbookRunningNode r = RunbookRunningNode.builder()
                .runId("aldskfa;skda")
                .nodeId("1")
                .build();

        // -- insert with no runbook
        final RunbookRunningNode rr = r;
        assertThatThrownBy(() -> runningNodeDatabaseService.insert("test", rr))
                .isExactlyInstanceOf(DataIntegrityViolationException.class);

        // -- insert
        r = r.toBuilder().runId(runId).build();
        String id = runningNodeDatabaseService.insert("test", r);

        // -- get
        RunbookRunningNode output = runningNodeDatabaseService.get("test", id).orElse(null);
        assertThat(output).isEqualToIgnoringGivenFields(r.toBuilder()
                .id(id)
                .state(RunbookRunningNodeState.WAITING)
                .output(Collections.emptyMap())
                .data(Collections.emptyMap())
                .triggeredBy(Collections.emptyMap())
                .hasWarnings(false)
                .result(RunbookRunningNodeResult.builder().build())
                .build(), "createdAt");
        assertThat(output.getCreatedAt()).isNotNull();

        // -- update
        Instant now = Instant.now();
        RunbookRunningNodeResult nodeResult = RunbookRunningNodeResult.builder()
                .errors(List.of(RunbookError.builder()
                        .type("npe")
                        .description("desc")
                        .build()))
                .build();
        runningNodeDatabaseService.update("test", RunbookRunningNode.builder()
                .id(id)
                .output(Map.of("var", RunbookVariable.builder().build()))
                .data(Map.of("some", "data"))
                .state(RunbookRunningNodeState.SUCCESS)
                .triggeredBy(Map.of("A", "a1", "B", "b1"))
                .hasWarnings(true)
                .result(nodeResult)
                .build());

        output = runningNodeDatabaseService.get("test", id).orElse(null);
        assertThat(output).isEqualToIgnoringGivenFields(r.toBuilder()
                .id(id)
                .state(RunbookRunningNodeState.SUCCESS)
                .output(Map.of("var", RunbookVariable.builder().build()))
                .data(Map.of("some", "data"))
                .triggeredBy(Map.of("A", "a1", "B", "b1"))
                .hasWarnings(true)
                .result(nodeResult)
                .build(), "createdAt", "stateChangedAt");
        assertThat(output.getCreatedAt()).isNotNull();
        assertThat(output.getStateChangedAt()).isNotNull();
        System.out.println(">>>>> " + now);
        System.out.println(">>>>> " + output.getCreatedAt());
        System.out.println(">>>>> " + output.getStateChangedAt());
        assertThat(output.getStateChangedAt()).isAfterOrEqualTo(now);
        assertThat(output.getStateChangedAt()).isBefore(now.plus(5, ChronoUnit.SECONDS));

        // -- filter
        List<RunbookRunningNode> records;
        records = runningNodeDatabaseService.filter(0, 10, "test", null, null, null, null, null).getRecords();
        assertThat(records.stream().map(RunbookRunningNode::getId)).containsExactly(id);

        records = runningNodeDatabaseService.filter(0, 10, "test", runId, null, null, null, null).getRecords();
        assertThat(records.stream().map(RunbookRunningNode::getId)).containsExactly(id);

        records = runningNodeDatabaseService.filter(0, 10, "test", "edbb8cbf-3b67-4d73-9393-c51113e59cec", null, null, null, null).getRecords();
        assertThat(records.stream().map(RunbookRunningNode::getId)).isEmpty();

        records = runningNodeDatabaseService.filter(0, 10, "test", null, List.of("1"), null, null, null).getRecords();
        assertThat(records.stream().map(RunbookRunningNode::getId)).containsExactly(id);

        records = runningNodeDatabaseService.filter(0, 10, "test", null, List.of("1", "2"), null, null, null).getRecords();
        assertThat(records.stream().map(RunbookRunningNode::getId)).containsExactly(id);

        records = runningNodeDatabaseService.filter(0, 10, "test", null, List.of("2"), null, null, null).getRecords();
        assertThat(records.stream().map(RunbookRunningNode::getId)).isEmpty();

        records = runningNodeDatabaseService.filter(0, 10, "test", null, null, null, List.of(RunbookRunningNodeState.SUCCESS), null).getRecords();
        assertThat(records.stream().map(RunbookRunningNode::getId)).containsExactly(id);

        records = runningNodeDatabaseService.filter(0, 10, "test", null, null, null, List.of(RunbookRunningNodeState.SUCCESS, RunbookRunningNodeState.FAILURE), null).getRecords();
        assertThat(records.stream().map(RunbookRunningNode::getId)).containsExactly(id);

        records = runningNodeDatabaseService.filter(0, 10, "test", null, null, null, List.of(RunbookRunningNodeState.FAILURE), null).getRecords();
        assertThat(records.stream().map(RunbookRunningNode::getId)).isEmpty();

        records = runningNodeDatabaseService.filter(0, 10, "test", runId, List.of("1"), null, List.of(RunbookRunningNodeState.SUCCESS), null).getRecords();
        assertThat(records.stream().map(RunbookRunningNode::getId)).containsExactly(id);

        records = runningNodeDatabaseService.filter(0, 10, "test", runId, List.of("1"), null,List.of( RunbookRunningNodeState.SUCCESS), true).getRecords();
        assertThat(records.stream().map(RunbookRunningNode::getId)).containsExactly(id);

        records = runningNodeDatabaseService.filter(0, 10, "test", runId, List.of("1"), null, List.of(RunbookRunningNodeState.SUCCESS), false).getRecords();
        assertThat(records.stream().map(RunbookRunningNode::getId)).isEmpty();

        // -- delete
        runningNodeDatabaseService.delete("test", id);
        assertThat(runningNodeDatabaseService.get("test", id)).isEmpty();

        // -- insert with triggeredby
        RunbookRunningNode r2 = RunbookRunningNode.builder()
                .runId(runId)
                .nodeId("1")
                .triggeredBy(Map.of("A", "a1"))
                .build();
        String id2 = runningNodeDatabaseService.insert("test", r2);
        output = runningNodeDatabaseService.get("test", id2).orElse(null);
        assertThat(output).isEqualToIgnoringGivenFields(r2.toBuilder()
                .id(id2)
                .state(RunbookRunningNodeState.WAITING)
                .output(Collections.emptyMap())
                .data(Collections.emptyMap())
                .hasWarnings(false)
                .result(RunbookRunningNodeResult.builder().build())
                .build(), "createdAt");
    }
}