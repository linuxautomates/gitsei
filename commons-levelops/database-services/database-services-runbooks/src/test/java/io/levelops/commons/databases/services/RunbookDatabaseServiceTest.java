package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.TriggerType;
import io.levelops.commons.databases.models.database.runbooks.Runbook;
import io.levelops.commons.databases.models.database.runbooks.RunbookNode;
import io.levelops.commons.databases.models.database.runbooks.RunbookVariable;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.ContentType;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class RunbookDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    RunbookDatabaseService runbookDatabaseService;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        runbookDatabaseService = new RunbookDatabaseService(dataSource, DefaultObjectMapper.get());
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));
        runbookDatabaseService.ensureTableExistence("test");
    }

    @Test
    public void test() throws SQLException {
        // name, description, enabled, trigger_type, cron, smart_ticket_type, input, nodes
        String previousId = UUID.randomUUID().toString();
        Runbook r = Runbook.builder()
                .name("name")
                .description("desc")
                .triggerType(TriggerType.MANUAL)
                .triggerTemplateType("abc")
                .input(Collections.emptyMap())
                .previousId(previousId)
                .triggerData(Map.of(
                        "var", RunbookVariable.builder()
                                .name("var")
                                .contentType(ContentType.fromString("boolean"))
                                .valueType(RunbookVariable.RunbookValueType.STRING)
                                .build()))
                .nodes(Map.of("1", RunbookNode.builder()
                        .name("node1")
                        .description("desc")
                        .type("nodeType")
                        .fromNodes(Map.of("1", RunbookNode.NodeTransition.builder().wait(true).build()))
                        .toNodes(Map.of("2", RunbookNode.NodeTransition.builder().build()))
                        .build()))
                .settings(Runbook.Setting.builder()
                        .notifications(Runbook.Setting.Notification.builder()
                                .enabled(true)
                                .type("email")
                                .recipients(List.of("user@levelops.io"))
                                .build())
                        .build())
                .build();
        String id = runbookDatabaseService.insert("test", r);

        // -- get
        Runbook output = runbookDatabaseService.get("test", id).orElse(null);
        r = r.toBuilder()
                .uiData(Collections.emptyMap())
                .build();
        assertThat(output).isEqualToIgnoringGivenFields(r.toBuilder()
                .id(id)
                .enabled(true)
                .build(), "createdAt", "updatedAt", "permanentId");
        assertThat(output.getPermanentId()).isNotNull();

        // -- update
        Instant date = DateUtils.fromEpochSecond(123456L);
        runbookDatabaseService.update("test", Runbook.builder()
                .id(id)
                .name("hello")
                .description("new")
                .enabled(false)
                .triggerData(Map.of("cron", RunbookVariable.builder().name("123").build()))
                .lastRunAt(date)
                .uiData(Map.of("some", "data"))
                .build());

        System.out.println(" >>>>> " + date);

        output = runbookDatabaseService.get("test", id).orElse(null);
        System.out.println(" >>>>> " + output.getLastRunAt());
        assertThat(output.getLastRunAt()).isEqualTo(date);
        assertThat(output).isEqualToIgnoringGivenFields(r.toBuilder()
                .id(id)
                .name("hello")
                .description("new")
                .enabled(false)
                .triggerData(Map.of("cron", RunbookVariable.builder().name("123").build()))
                .uiData(Map.of("some", "data"))
                .settings(Runbook.Setting.builder()
                        .notifications(Runbook.Setting.Notification.builder()
                                .enabled(true)
                                .type("email")
                                .recipients(List.of("user@levelops.io"))
                                .build())
                        .build())
                .lastRunAt(date)
                .build(), "createdAt", "updatedAt", "permanentId");
        assertThat(output.getPermanentId()).isNotNull();

        // -- filter
        List<Runbook> records;

        DbListResponse<Runbook> response = runbookDatabaseService.filter(0, 10, "test", null, null, null, null, null, null, null, null);
        records = response.getRecords();
        assertThat(response.getCount()).isEqualTo(1);
        assertThat(records.stream().map(Runbook::getId)).containsExactly(id);

        records = runbookDatabaseService.filter(0, 10, "test", "lo", null, null, null, null, null, null, null).getRecords();
        assertThat(records.stream().map(Runbook::getId)).containsExactly(id);

        response = runbookDatabaseService.filter(0, 10, "test", "sdfsdfsd", null, null, null, null, null, null, null);
        records = response.getRecords();
        assertThat(response.getCount()).isEqualTo(0);
        assertThat(records.stream().map(Runbook::getId)).isEmpty();

        records = runbookDatabaseService.filter(0, 10, "test", null, true, null, null, null, null, null, null).getRecords();
        assertThat(records.stream().map(Runbook::getId)).isEmpty();

        records = runbookDatabaseService.filter(0, 10, "test", "lo", false, null, null, null, null, null, null).getRecords();
        assertThat(records.stream().map(Runbook::getId)).containsExactly(id);

        records = runbookDatabaseService.filter(0, 10, "test", null, null, TriggerType.MANUAL, null, null, null, null, null).getRecords();
        assertThat(records.stream().map(Runbook::getId)).containsExactly(id);

        records = runbookDatabaseService.filter(0, 10, "test", null, null, TriggerType.SCHEDULED, null, null, null, null, null).getRecords();
        assertThat(records.stream().map(Runbook::getId)).isEmpty();

        records = runbookDatabaseService.filter(0, 10, "test", "lo", false, TriggerType.MANUAL, null, null, null, null, null).getRecords();
        assertThat(records.stream().map(Runbook::getId)).containsExactly(id);

        records = runbookDatabaseService.filter(0, 10, "test", null, null, null, null, List.of(id), null, null, null).getRecords();
        assertThat(records.stream().map(Runbook::getId)).containsExactly(id);

        records = runbookDatabaseService.filter(0, 10, "test", null, null, null, null, List.of(id, "123"), null, null, null).getRecords();
        assertThat(records.stream().map(Runbook::getId)).containsExactly(id);

        records = runbookDatabaseService.filter(0, 10, "test", null, null, null, null, List.of("123"), null, null, null).getRecords();
        assertThat(records.stream().map(Runbook::getId)).isEmpty();

        records = runbookDatabaseService.filter(0, 10, "test", null, null, null, "abc", null, null, null, null).getRecords();
        assertThat(records.stream().map(Runbook::getId)).containsExactly(id);

        records = runbookDatabaseService.filter(0, 10, "test", null, null, null, null, null, null, previousId, null).getRecords();
        assertThat(records.stream().map(Runbook::getId)).containsExactly(id);

        // -- delete
        runbookDatabaseService.delete("test", id);
        assertThat(runbookDatabaseService.get("test", id)).isEmpty();
    }

    @Test
    public void testPreviousId() throws SQLException {

        // there are 2 runbooks, id4 has 3 revisions, and id3 only has 1:
        // id4 -> id2 -> id1
        // id3

        Runbook r = Runbook.builder()
                .previousId(null)
                .name("name")
                .description("desc")
                .triggerType(TriggerType.MANUAL)
                .input(Collections.emptyMap())
                .build();
        String id1 = runbookDatabaseService.insert("test", r);

        Runbook r2 = Runbook.builder()
                .previousId(id1)
                .name("name")
                .description("desc")
                .triggerType(TriggerType.MANUAL)
                .input(Collections.emptyMap())
                .build();
        String id2 = runbookDatabaseService.insert("test", r2);

        Runbook r3 = Runbook.builder()
                .name("name")
                .description("desc")
                .triggerType(TriggerType.MANUAL)
                .input(Collections.emptyMap())
                .build();
        String id3 = runbookDatabaseService.insert("test", r3);

        Runbook r4 = Runbook.builder()
                .previousId(id2)
                .name("name")
                .description("desc")
                .triggerType(TriggerType.MANUAL)
                .input(Collections.emptyMap())
                .build();
        String id4 = runbookDatabaseService.insert("test", r4);

        DbListResponse<Runbook> records = runbookDatabaseService.filter(0, 10, "test", null, null, null, null, null, null, null, null);
        assertThat(records.getRecords().stream().map(Runbook::getId)).containsExactlyInAnyOrder(id1, id2, id3, id4);
        assertThat(records.getTotalCount()).isEqualTo(4);
        records = runbookDatabaseService.filter(0, 10, "test", null, null, null, null, null, false, null, null);
        assertThat(records.getRecords().stream().map(Runbook::getId)).containsExactlyInAnyOrder(id1, id2, id3, id4);
        assertThat(records.getTotalCount()).isEqualTo(4);

        records = runbookDatabaseService.filter(0, 10, "test", null, null, null, null, null, true, null, null);
        assertThat(records.getRecords().stream().map(Runbook::getId)).containsExactlyInAnyOrder(id3, id4);
        assertThat(records.getTotalCount()).isEqualTo(2);

        // test listPreviousRevisions

        assertThat(runbookDatabaseService.listPreviousRevisions("test", id1)).containsExactly(id1);
        assertThat(runbookDatabaseService.listPreviousRevisions("test", id2)).containsExactly(id2, id1);
        assertThat(runbookDatabaseService.listPreviousRevisions("test", id3)).containsExactly(id3);
        assertThat(runbookDatabaseService.listPreviousRevisions("test", id4)).containsExactly(id4, id2, id1);

        // test listNewerRevisions

        assertThat(runbookDatabaseService.listNewerRevisions("test", id1)).containsExactly(id1, id2, id4);
        assertThat(runbookDatabaseService.listNewerRevisions("test", id2)).containsExactly(id2, id4);
        assertThat(runbookDatabaseService.listNewerRevisions("test", id3)).containsExactly(id3);
        assertThat(runbookDatabaseService.listNewerRevisions("test", id4)).containsExactly(id4);

        // test getLatestRevision
        assertThat(runbookDatabaseService.getLatestRevision("test", id1)).isEqualTo(id4);
        assertThat(runbookDatabaseService.getLatestRevision("test", id2)).isEqualTo(id4);
        assertThat(runbookDatabaseService.getLatestRevision("test", id3)).isEqualTo(id3);
        assertThat(runbookDatabaseService.getLatestRevision("test", id4)).isEqualTo(id4);

        // test delete

        boolean status = runbookDatabaseService.deletePreviousRevisions("test", id2, false); // should only remove id1
        assertThat(status).isTrue();
        assertThat(runbookDatabaseService.get("test", id1)).isEmpty();
        assertThat(runbookDatabaseService.listPreviousRevisions("test", id4)).containsExactly(id4, id2);

        status = runbookDatabaseService.deletePreviousRevisions("test", id4, true); // should only remove both id4 and id2
        assertThat(status).isTrue();
        assertThat(runbookDatabaseService.get("test", id4)).isEmpty();
        assertThat(runbookDatabaseService.get("test", id2)).isEmpty();
        assertThat(runbookDatabaseService.listPreviousRevisions("test", id4)).isEmpty();

        assertThat(runbookDatabaseService.deletePreviousRevisions("test", id4, true)).isFalse();
    }

    @Test
    public void testPermanentId() throws SQLException {

        Runbook r1 = Runbook.builder()
                .previousId(null)
                .name("name")
                .description("desc")
                .triggerType(TriggerType.MANUAL)
                .input(Collections.emptyMap())
                .build();
        RunbookDatabaseService.InsertResult id1 = runbookDatabaseService.insertAndReturnIds("test", r1)
                .orElse(null);
        assertThat(id1.getId()).isNotNull();
        assertThat(id1.getPermanentId()).isNotNull();

        Runbook rb1a = runbookDatabaseService.get("test", id1.getId()).orElse(null);
        Runbook rb1b = runbookDatabaseService.getLatestByPermanentId("test", id1.getPermanentId()).orElse(null);
        assertThat(rb1b).isEqualToIgnoringGivenFields(rb1a, "createdAt", "updatedAt");
        assertThat(runbookDatabaseService.filter(0, 10, "test", null, null, null, null, null,
                true, null, List.of(id1.getPermanentId())).getRecords().stream().map(Runbook::getId))
                .containsExactly(id1.getId());

        DefaultObjectMapper.prettyPrint(rb1a);

        // --

        Runbook r2 = Runbook.builder()
                .previousId(id1.getId())
                .permanentId(id1.getPermanentId())
                .name("name")
                .description("desc")
                .triggerType(TriggerType.MANUAL)
                .input(Collections.emptyMap())
                .build();
        RunbookDatabaseService.InsertResult id2 = runbookDatabaseService.insertAndReturnIds("test", r2)
                .orElse(null);
        assertThat(id2.getId()).isNotNull();
        assertThat(id2.getPermanentId()).isNotNull();
        // the id will be new but the permanent id won't change:
        assertThat(id2.getId()).isNotEqualTo(id1.getId());
        assertThat(id2.getPermanentId()).isEqualTo(id1.getPermanentId());

        // querying latest by permanent id will now return rb2
        Runbook rb2a = runbookDatabaseService.get("test", id2.getId()).orElse(null);
        Runbook rb2b = runbookDatabaseService.getLatestByPermanentId("test", id2.getPermanentId()).orElse(null);
        assertThat(rb2b).isEqualToIgnoringGivenFields(rb2a, "createdAt", "updatedAt");

        DefaultObjectMapper.prettyPrint(rb2a);

        // --- test filter

        // inserted 3rd playbook for negative test
        RunbookDatabaseService.InsertResult id3 = runbookDatabaseService.insertAndReturnIds("test", Runbook.builder()
                .name("name")
                .triggerType(TriggerType.MANUAL)
                .build())
                .orElse(null);

        assertThat(runbookDatabaseService.filter(0, 10, "test", null, null, null, null, null,
                false, null, null).getRecords().stream().map(Runbook::getId))
                .containsExactly(id3.getId(), id2.getId(), id1.getId());
        assertThat(runbookDatabaseService.filter(0, 10, "test", null, null, null, null, null,
                false, null, List.of(id1.getPermanentId())).getRecords().stream().map(Runbook::getId))
                .containsExactly(id2.getId(), id1.getId());
        assertThat(runbookDatabaseService.filter(0, 10, "test", null, null, null, null, null,
                true, null, null).getRecords().stream().map(Runbook::getId))
                .containsExactly(id3.getId(), id2.getId());
        assertThat(runbookDatabaseService.filter(0, 10, "test", null, null, null, null, null,
                true, null, List.of(id1.getPermanentId())).getRecords().stream().map(Runbook::getId))
                .containsExactly(id2.getId());

        // -- test incorrect history (for pre-migration runbooks)
        RunbookDatabaseService.InsertResult id4 = runbookDatabaseService.insertAndReturnIds("test", Runbook.builder()
                .previousId(id2.getId())
                .permanentId(null) // in normal scenario, this should have taken the same permanent id as id1 & id2
                .name("name")
                .triggerType(TriggerType.MANUAL)
                .build())
                .orElse(null);
        assertThat(runbookDatabaseService.filter(0, 10, "test", null, null, null, null, null,
                false, null, null).getRecords().stream().map(Runbook::getId))
                .containsExactly(id4.getId(), id3.getId(), id2.getId(), id1.getId());
        assertThat(runbookDatabaseService.filter(0, 10, "test", null, null, null, null, null,
                false, null, List.of(id1.getPermanentId())).getRecords().stream().map(Runbook::getId))
                .containsExactly(id2.getId(), id1.getId());
        assertThat(runbookDatabaseService.filter(0, 10, "test", null, null, null, null, null,
                true, null, null).getRecords().stream().map(Runbook::getId))
                .containsExactly(id4.getId(), id3.getId());
        assertThat(runbookDatabaseService.filter(0, 10, "test", null, null, null, null, null,
                true, null, List.of(id1.getPermanentId())).getRecords().stream().map(Runbook::getId))
                .isEmpty();
        assertThat(runbookDatabaseService.filter(0, 10, "test", null, null, null, null, null,
                true, null, List.of(id4.getPermanentId())).getRecords().stream().map(Runbook::getId))
                .containsExactly(id4.getId());

        boolean isMaxedOut = runbookDatabaseService.isCountMaxedOut("test", Map.of("PROPELS_COUNT", "10"));
        assertThat(isMaxedOut).isFalse();
        isMaxedOut = runbookDatabaseService.isCountMaxedOut("test", Map.of("PROPELS_COUNT", "3"));
        assertThat(isMaxedOut).isTrue();

    }

}