package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.runbooks.RunbookReport;
import io.levelops.commons.databases.models.database.runbooks.RunbookReportSection;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class RunbookReportSectionDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    RunbookReportDatabaseService reportDatabaseService;
    RunbookReportSectionDatabaseService reportSectionDatabaseService;

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
        reportSectionDatabaseService = new RunbookReportSectionDatabaseService(dataSource, DefaultObjectMapper.get());
        reportSectionDatabaseService.ensureTableExistence("test");
    }

    @Test
    public void name() throws SQLException {
        UUID uuid1 = UUID.randomUUID();
        UUID runbookId = UUID.randomUUID();
        RunbookReport report = RunbookReport.builder()
                .runId(uuid1.toString())
                .runbookId(runbookId.toString())
                .source("N1")
                .title("reportTitle")
                .gcsPath("/path")
                .build();
        String reportId = reportDatabaseService.insert("test", report);
        String reportId2 = reportDatabaseService.insert("test", report.toBuilder()
                .runId(UUID.randomUUID().toString())
                .build());

        RunbookReportSection section = RunbookReportSection.builder()
                .source("s1")
                .reportId(reportId)
                .gcsPath("path/to/section")
                .pageCount(2)
                .pageSize(1000)
                .totalCount(10000)
                .title("title123")
                .metadata(Map.of("hello", "world"))
                .build();
        String sectionId = reportSectionDatabaseService.insert("test", section);

        RunbookReportSection r = reportSectionDatabaseService.get("test", sectionId).orElse(null);
        assertThat(r).isEqualToIgnoringGivenFields(section.toBuilder()
                .id(sectionId)
                .build(), "createdAt");

        assertThat(reportSectionDatabaseService.filter(0, 10, "test", null, null).getRecords().stream().map(RunbookReportSection::getId))
                .containsExactly(sectionId);
        assertThat(reportSectionDatabaseService.filter(0, 10, "test", "s1", null).getRecords().stream().map(RunbookReportSection::getId))
                .containsExactly(sectionId);
        assertThat(reportSectionDatabaseService.filter(0, 10, "test", "s1", reportId).getRecords().stream().map(RunbookReportSection::getId))
                .containsExactly(sectionId);
        assertThat(reportSectionDatabaseService.filter(0, 10, "test", "wrong", reportId).getRecords().stream().map(RunbookReportSection::getId))
                .isEmpty();

        // test uniqueness
        assertThat(reportSectionDatabaseService.insertAndReturnId("test", section)).isEmpty();
        assertThat(reportSectionDatabaseService.insertAndReturnId("test", section.toBuilder()
                .source(UUID.randomUUID().toString())
                .build())).isPresent();
        assertThat(reportSectionDatabaseService.insertAndReturnId("test", section.toBuilder()
                .reportId(reportId2)
                .build())).isPresent();

    }
}