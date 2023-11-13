package io.levelops.runbooks.services;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import io.levelops.commons.databases.services.RunbookReportDatabaseService;
import io.levelops.commons.databases.services.RunbookReportSectionDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.runbooks.models.RunbookReportData;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RunbookReportServiceTest {
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    @Mock
    Storage storage;

    RunbookReportDatabaseService reportDatabaseService;
    RunbookReportSectionDatabaseService reportSectionDatabaseService;
    RunbookReportServiceImpl reportService;

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

        MockitoAnnotations.initMocks(this);

        Blob blob = Mockito.mock(Blob.class);
        Mockito.doAnswer(ans -> {
            ans.getArgument(0, OutputStream.class).write(DefaultObjectMapper.get().writeValueAsBytes(
                    Map.of("records", List.of(
                            Map.of("row", "1", "field1", "some"),
                            Map.of("row", "2", "field2", "data"),
                            Map.of("row", "3")
                    ))));
            return null;
        }).when(blob).downloadTo(any(OutputStream.class));

        when(storage.get(Mockito.any(BlobId.class))).thenReturn(blob);

        reportService = new RunbookReportServiceImpl("bucket", reportDatabaseService, reportSectionDatabaseService, DefaultObjectMapper.get(), storage, 3);

    }


    @Test
    public void test() throws SQLException {
        String rbId = "6252d750-5a8b-42d8-ba04-46c1119b069d";
        String runId = "5f98d1ae-c0f7-48ac-91b4-aef3537a7dff";
        Map<String, Object> row = Map.of("some", "data");
        reportService.createReportSection("test", rbId, runId, "N1", "rn1", "reportTitle", "sectionA", "jira/issues",
                Stream.of(row, row, row, row, row));
        reportService.createReportSection("test", rbId, runId, "N1", "rn2", "reportTitle", "sectionB", "jira/issues",
                Stream.of(row, row, row, row, row));
        reportService.createReportSection("test", rbId, runId, "N2", "rn1", "reportTitle2", "sectionA", "jira/issues",
                Stream.of(row, row, row, row, row));

        RunbookReportData reportData = reportService.fetchReportData("test", rbId, runId, "N1").orElse(null);
        DefaultObjectMapper.prettyPrint(reportData);

        assertThat(reportData.getReport().getRunbookId()).isEqualTo(rbId);
        assertThat(reportData.getReport().getRunId()).isEqualTo(runId);
        assertThat(reportData.getRecords()).hasSize(12);

        ArgumentCaptor<BlobInfo> blobInfoArgumentCaptor = ArgumentCaptor.forClass(BlobInfo.class);
        verify(storage, times(6)).create(blobInfoArgumentCaptor.capture(), any(byte[].class));

        assertThat(blobInfoArgumentCaptor.getAllValues()).containsExactly(
                BlobInfo.newBuilder("bucket", "/test/runbook_reports/rb-6252d750-5a8b-42d8-ba04-46c1119b069d/run-5f98d1ae-c0f7-48ac-91b4-aef3537a7dff/node-N1/running-node-rn1/0.json").setContentType("application/json").build(),
                BlobInfo.newBuilder("bucket", "/test/runbook_reports/rb-6252d750-5a8b-42d8-ba04-46c1119b069d/run-5f98d1ae-c0f7-48ac-91b4-aef3537a7dff/node-N1/running-node-rn1/1.json").setContentType("application/json").build(),
                BlobInfo.newBuilder("bucket", "/test/runbook_reports/rb-6252d750-5a8b-42d8-ba04-46c1119b069d/run-5f98d1ae-c0f7-48ac-91b4-aef3537a7dff/node-N1/running-node-rn2/0.json").setContentType("application/json").build(),
                BlobInfo.newBuilder("bucket", "/test/runbook_reports/rb-6252d750-5a8b-42d8-ba04-46c1119b069d/run-5f98d1ae-c0f7-48ac-91b4-aef3537a7dff/node-N1/running-node-rn2/1.json").setContentType("application/json").build(),
                BlobInfo.newBuilder("bucket", "/test/runbook_reports/rb-6252d750-5a8b-42d8-ba04-46c1119b069d/run-5f98d1ae-c0f7-48ac-91b4-aef3537a7dff/node-N2/running-node-rn1/0.json").setContentType("application/json").build(),
                BlobInfo.newBuilder("bucket", "/test/runbook_reports/rb-6252d750-5a8b-42d8-ba04-46c1119b069d/run-5f98d1ae-c0f7-48ac-91b4-aef3537a7dff/node-N2/running-node-rn1/1.json").setContentType("application/json").build()
        );

        // delete
        Page<Blob> page = Mockito.mock(Page.class);
        when(storage.list(eq("bucket"), any())).thenReturn(page);
        Blob blob = Mockito.mock(Blob.class);
        when(blob.getName()).thenReturn("/test/runbook_reports/rb-" + rbId + "/.../1");
        when(page.iterateAll()).thenReturn(List.of(blob));
        reportService.deleteReport("test", reportData.getReport().getId());

        ArgumentCaptor<BlobId> blobIdArgumentCaptor = ArgumentCaptor.forClass(BlobId.class);
        verify(storage, times(1)).delete(eq("bucket"), eq("/test/runbook_reports/rb-6252d750-5a8b-42d8-ba04-46c1119b069d/.../1"));
    }

//    @Test
//    public void name() {
//        Storage storage = StorageOptions.getDefaultInstance().getService();
//
//        String pathPrefix = "data/tenant-coke/integration-jira/2019/10/";
//        ArrayList<Storage.BlobListOption> listOptions = new ArrayList<Storage.BlobListOption>();
//        listOptions.add(Storage.BlobListOption.prefix(pathPrefix));
//
//            listOptions.add(Storage.BlobListOption.currentDirectory());
//        Iterable<Blob> list = storage.list("ingestion-levelops", listOptions.toArray(Storage.BlobListOption[]::new)).iterateAll();
//        list.forEach(blob -> System.out.println(blob.getName()));
//    }
}