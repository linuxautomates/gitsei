package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.runbooks.RunbookReport;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class RunbookReportDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    RunbookReportDatabaseService reportDatabaseService;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));
        reportDatabaseService = new RunbookReportDatabaseService(dataSource, DefaultObjectMapper.get());
        reportDatabaseService.ensureTableExistence("test");
        reportDatabaseService.ensureTableExistence("test");
    }

    @Test
    public void name() throws SQLException {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        RunbookReport report = RunbookReport.builder()
                .runId(uuid1.toString())
                .runbookId(uuid2.toString())
                .source("node123")
                .gcsPath("/path")
                .title("mon titre")
                .build();
        String id = reportDatabaseService.insert("test", report);

        RunbookReport r = reportDatabaseService.get("test", id).orElse(null);
        assertThat(r).isEqualToIgnoringGivenFields(report.toBuilder()
                .id(id)
                .build(), "createdAt");

        assertThat(reportDatabaseService.filter(0, 10, "test", List.of(uuid2.toString()), null, null, null).getRecords().stream().map(RunbookReport::getId))
                .containsExactly(id);
        assertThat(reportDatabaseService.filter(0, 10, "test", List.of(uuid2.toString()), uuid1.toString(), null, null).getRecords().stream().map(RunbookReport::getId))
                .containsExactly(id);
        assertThat(reportDatabaseService.filter(0, 10, "test", List.of(uuid2.toString(), uuid1.toString()), uuid1.toString(), null, null).getRecords().stream().map(RunbookReport::getId))
                .containsExactly(id);
        assertThat(reportDatabaseService.filter(0, 10, "test", List.of(uuid1.toString()), null, null, null).getRecords().stream().map(RunbookReport::getId))
                .isEmpty();
        assertThat(reportDatabaseService.filter(0, 10, "test", null, null, null, "tit").getRecords().stream().map(RunbookReport::getId))
                .containsExactly(id);

        // test uniqueness
        assertThat(reportDatabaseService.insertAndReturnId("test", report)).isEmpty();
        assertThat(reportDatabaseService.insertAndReturnId("test", report.toBuilder()
                .runId(UUID.randomUUID().toString())
                .build())).isPresent();
        assertThat(reportDatabaseService.insertAndReturnId("test", report.toBuilder()
                .runbookId(UUID.randomUUID().toString())
                .build())).isPresent();
    }

    @Test
    public void testBulkDelete() throws SQLException {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        UUID uuid4 = UUID.randomUUID();
        RunbookReport report1 = RunbookReport.builder()
                .runId(uuid1.toString())
                .runbookId(uuid2.toString())
                .source("node123")
                .gcsPath("/path")
                .title("mon titre")
                .build();
        RunbookReport report2 = RunbookReport.builder()
                .runId(uuid3.toString())
                .runbookId(uuid4.toString())
                .source("node123")
                .gcsPath("/path")
                .title("mon titre")
                .build();
        String id1 = reportDatabaseService.insert("test", report1);
        String id2 = reportDatabaseService.insert("test", report2);
        reportDatabaseService.deleteBulkReports("test", List.of(id1, id2));
        assertThat(reportDatabaseService.get("test", id1)).isEmpty();
        assertThat(reportDatabaseService.get("test", id2)).isEmpty();
    }
}