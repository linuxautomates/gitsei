package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbWebhookData;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.integrations.github.models.GithubWebhookEvent;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ScmWebhooksDataServiceTest {

    public static final String COMPANY = "test";
    private static ScmWebhooksDataService scmWebhooksDataService;
    private static IntegrationService integrationService;
    private static DataSource dataSource;
    private static final ObjectMapper m = DefaultObjectMapper.get();
    private static String integrationId;

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(COMPANY);
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(COMPANY);
        scmWebhooksDataService = new ScmWebhooksDataService(dataSource);
        scmWebhooksDataService.ensureTableExistence(COMPANY);

        integrationId = integrationService.insert(COMPANY, Integration.builder()
                .application("issue_mgmt")
                .name("issue mgmt test")
                .status("enabled")
                .build());

        DbWebhookData dbWebhookData1 = DbWebhookData.builder()
                .integrationId(1)
                .webhookId("55311faa-a359-4603-a66c-b34ae63afe2b")
                .jobId("27e32966-eaaa-4dea-897c-ab499eeb4f1a")
                .status(DbWebhookData.Status.ENQUEUED.toString())
                .webhookEvent(GithubWebhookEvent.builder().zen("zen2").build()).build();
        DbWebhookData dbWebhookData2 = DbWebhookData.builder()
                .integrationId(1)
                .webhookId("65311faa-a359-4603-a66c-b34ae63afe4b")
                .jobId("37e32966-eaaa-4dea-897c-ab499eeb4f0a")
                .status(DbWebhookData.Status.SUCCESS.toString())
                .webhookEvent(GithubWebhookEvent.builder().zen("zen3").build()).build();
        DbWebhookData dbWebhookData3 = DbWebhookData.builder()
                .integrationId(1)
                .webhookId("25311faa-a359-4603-a66c-b34ae63afe5b")
                .jobId("47e32966-eaaa-4dea-897c-ab499eeb4f2a")
                .status(DbWebhookData.Status.JOB_FAIL.toString())
                .webhookEvent(GithubWebhookEvent.builder().zen("zen4").build()).build();
        DbWebhookData dbWebhookData4 = DbWebhookData.builder()
                .integrationId(1)
                .webhookId("25311faa-a359-4603-a66c-b34ae63af55b")
                .jobId("47e32966-eaaa-4dea-897c-ab499eeb4f2a")
                .status(DbWebhookData.Status.WRITE_FAIL.toString())
                .webhookEvent(GithubWebhookEvent.builder().zen("zen5").build()).build();
        DbWebhookData dbWebhookData5 = DbWebhookData.builder()
                .integrationId(1)
                .webhookId("15311faa-a359-4603-a66c-b34ae63afe3b")
                .status(DbWebhookData.Status.ENQUEUED.toString())
                .webhookEvent(GithubWebhookEvent.builder().zen("zen6").build()).build();
        DbWebhookData dbWebhookData8 = DbWebhookData.builder()
                .integrationId(1)
                .webhookId("15311faa-a359-4603-a66c-b34ae63af23b")
                .status(DbWebhookData.Status.WRITE_FAIL.toString())
                .webhookEvent(GithubWebhookEvent.builder().zen("zen6").build()).build();
        scmWebhooksDataService.insert(COMPANY, dbWebhookData1);
        scmWebhooksDataService.insert(COMPANY, dbWebhookData2);
        scmWebhooksDataService.insert(COMPANY, dbWebhookData3);
        scmWebhooksDataService.insert(COMPANY, dbWebhookData4);
        scmWebhooksDataService.insert(COMPANY, dbWebhookData5);
        scmWebhooksDataService.insert(COMPANY, dbWebhookData8);

    }

    @Test
    public void insert() throws SQLException {
        DbListResponse<DbWebhookData> webhookDataList =  scmWebhooksDataService.list(COMPANY, 0, 100);
        assertThat(webhookDataList.getRecords().size()).isEqualTo(6);
    }

    @Test
    public void testGetWebhook() throws SQLException {
        List<DbWebhookData> webhookDataList1 =  scmWebhooksDataService.
                getWebhookEventData(COMPANY, "1", "37e32966-eaaa-4dea-897c-ab499eeb4f0a");
        assertThat(webhookDataList1).isNotNull();
        assertThat(webhookDataList1.size()).isEqualTo(1);
        assertThat(webhookDataList1.get(0).getStatus()).isEqualTo(DbWebhookData.Status.SUCCESS.toString());
        List<DbWebhookData> webhookDataList2 =  scmWebhooksDataService.
                getWebhookEventData(COMPANY, "1", "47e32966-eaaa-4dea-897c-ab499eeb4f2a");
        assertThat(webhookDataList2).isNotNull();
        assertThat(webhookDataList2.size()).isEqualTo(2);
        assertThat(webhookDataList2.get(0).getStatus()).isEqualTo(DbWebhookData.Status.JOB_FAIL.toString());
        assertThat(webhookDataList2.get(1).getStatus()).isEqualTo(DbWebhookData.Status.WRITE_FAIL.toString());
    }

    @Test
    public void testNullJobIds() throws SQLException {
        DbWebhookData dbWebhookData6 = DbWebhookData.builder()
                .integrationId(1)
                .webhookId("15311faa-a359-4603-a66c-b34ae63a222b")
                .status(DbWebhookData.Status.ENQUEUED.toString())
                .webhookEvent(GithubWebhookEvent.builder().zen("zen6").build()).build();
        scmWebhooksDataService.insert(COMPANY, dbWebhookData6);
        List<DbWebhookData> dbWebhookDataResponse = scmWebhooksDataService.
                getByNullJobId(COMPANY, 0, 100);
        assertThat(dbWebhookDataResponse.get(0).getWebhookId()).isEqualTo("15311faa-a359-4603-a66c-b34ae63a222b");
        assertThat(dbWebhookDataResponse.get(0).getStatus()).isEqualTo(DbWebhookData.Status.ENQUEUED.toString());
        assertThat(dbWebhookDataResponse.size()).isEqualTo(1);
    }

    @Test
    public void testBulkUpdateStatus() {
        List<DbWebhookData> webhookDataList1 =  scmWebhooksDataService.
                getWebhookEventData(COMPANY, "1", "37e32966-eaaa-4dea-897c-ab499eeb4f0a");
        assertThat(webhookDataList1.get(0).getStatus()).isEqualTo(DbWebhookData.Status.SUCCESS.toString());
        scmWebhooksDataService.updateStatus(COMPANY, "1", "37e32966-eaaa-4dea-897c-ab499eeb4f0a", DbWebhookData.Status.ENQUEUED);
        List<DbWebhookData> webhookDataList2 =  scmWebhooksDataService.
                getWebhookEventData(COMPANY, "1", "37e32966-eaaa-4dea-897c-ab499eeb4f0a");
        assertThat(webhookDataList2.get(0).getStatus()).isEqualTo(DbWebhookData.Status.ENQUEUED.toString());
    }

    @Test
    public void testSingleUpdateStatus() {
        List<DbWebhookData> webhookDataList1 =  scmWebhooksDataService.
                getWebhookEventData(COMPANY, "1", "37e32966-eaaa-4dea-897c-ab499eeb4f0a");
        assertThat(webhookDataList1.get(0).getStatus()).isEqualTo(DbWebhookData.Status.ENQUEUED.toString());
        scmWebhooksDataService.updateStatus(COMPANY, "1", List.of("65311faa-a359-4603-a66c-b34ae63afe4b"), "37e32966-eaaa-4dea-897c-ab499eeb4f0a", DbWebhookData.Status.SUCCESS);
        List<DbWebhookData> webhookDataList2 =  scmWebhooksDataService.
                getWebhookEventData(COMPANY, "1", "37e32966-eaaa-4dea-897c-ab499eeb4f0a");
        assertThat(webhookDataList2.get(0).getStatus()).isEqualTo(DbWebhookData.Status.SUCCESS.toString());
    }

    @Test
    public void testAssignJobIds() {
        scmWebhooksDataService.
                assignJobIds(COMPANY, "1", List.of("15311faa-a359-4603-a66c-b34ae63afe3b", "15311faa-a359-4603-a66c-b34ae63af23b"),"47e32966-eaaa-4dea-897c-ab499eeb3333");
        List<DbWebhookData> webhookDataList1 =  scmWebhooksDataService.
                getWebhookEventData(COMPANY, "1", "47e32966-eaaa-4dea-897c-ab499eeb3333");
        assertThat(webhookDataList1.get(0).getJobId()).isEqualTo("47e32966-eaaa-4dea-897c-ab499eeb3333");
    }

    @Test
    public void testGetPendingJobIds() throws SQLException {
        DbWebhookData dbWebhookData = DbWebhookData.builder()
                .integrationId(1)
                .webhookId("55311faa-a359-4603-a66c-b34ae63afe2b")
                .jobId("27e32966-eaaa-4dea-897c-ab499eeb4f1a")
                .status(DbWebhookData.Status.ENQUEUED.toString())
                .webhookEvent(GithubWebhookEvent.builder().zen("zen2").build()).build();
        scmWebhooksDataService.insert(COMPANY, dbWebhookData);
        List<String> pendingJobIds = scmWebhooksDataService.getPendingJobIds(COMPANY, 0, DbWebhookData.Status.ENQUEUED, 0, 100);
        assertThat(pendingJobIds.size()).isEqualTo(1);
    }
}