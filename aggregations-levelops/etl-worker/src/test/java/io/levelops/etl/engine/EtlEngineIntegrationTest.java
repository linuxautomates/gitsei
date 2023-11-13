package io.levelops.etl.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.aggregations_shared.utils.IngestionResultPayloadUtils;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.InventoryServiceImpl;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.etl.job_framework.EtlJobRunner;
import io.levelops.etl.job_framework.EtlProcessor;
import io.levelops.etl.job_framework.EtlProcessorRegistry;
import io.levelops.etl.jobs.jira_user_emails.JiraUserEmailsService;
import io.levelops.etl.jobs.jira_user_emails.UserEmailsStage;
import io.levelops.etl.services.JobTrackingUtilsService;
import io.levelops.etl.utils.GcsUtils;
import io.levelops.ingestion.services.ControlPlaneJobService;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.services.GcsStorageService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.OkHttpClient;
import org.junit.Test;
import org.postgresql.PGProperty;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// With great power comes great responsibility
public class EtlEngineIntegrationTest {
    private static String DATABASE_IP = "104.155.177.90";
    private static String DATABASE_USERNAME = "";
    private static String DATABASE_PASSWORD = "";
    private static String SSL_MODE = "require";
    private static String clientCert = "";
    private static String clientKey = "";
    private static String clientKeyPassword = "";
    private static String serverCert = "";
    private static Long DB_CONNECTION_TIMEOUT = TimeUnit.MINUTES.toMillis(2);
    ;
    private static int maxPoolSize = 2;
    private static Long DB_LEAK_DETECTION_THRESHOLD = TimeUnit.MINUTES.toMillis(2);

    private DataSource createDataSource() {
        String url = "jdbc:postgresql://" + DATABASE_IP + "/postgres?";
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(DATABASE_USERNAME);
        config.setPassword(DATABASE_PASSWORD);
        config.addDataSourceProperty(PGProperty.SSL_MODE.getName(), SSL_MODE);
        config.addDataSourceProperty(PGProperty.SSL_CERT.getName(), clientCert);
        config.addDataSourceProperty(PGProperty.SSL_KEY.getName(), clientKey);
        config.addDataSourceProperty(PGProperty.SSL_PASSWORD.getName(), clientKeyPassword);
        config.addDataSourceProperty(PGProperty.SSL_ROOT_CERT.getName(), serverCert);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(DB_CONNECTION_TIMEOUT);
        config.setLeakDetectionThreshold(DB_LEAK_DETECTION_THRESHOLD);
        return new HikariDataSource(config);
    }

    @Test
    public void test() throws IOException, SQLException, ExecutionException, InterruptedException {
        var dataSource = createDataSource();
        ObjectMapper mapper = DefaultObjectMapper.get();

        OkHttpClient okHttpClient = new OkHttpClient();
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        ControlPlaneService controlPlaneService = new ControlPlaneService(okHttpClient, objectMapper, "http://localhost:8081", true);
        ControlPlaneJobService controlPlaneJobService = new ControlPlaneJobService(controlPlaneService, 2);
        Storage storage = StorageOptions.getDefaultInstance().getService();
        GcsUtils gcsUtils = new GcsUtils(storage, objectMapper);
        UserIdentityService userIdentityService = new UserIdentityService(dataSource);
        InventoryService inventoryService = new InventoryServiceImpl("http://localhost:8080", okHttpClient, objectMapper);
        JiraUserEmailsService jiraUserEmailsService = new JiraUserEmailsService(controlPlaneJobService, objectMapper, gcsUtils, userIdentityService, controlPlaneService, inventoryService);
        JobDefinitionDatabaseService jobDefinitionDatabaseService = new JobDefinitionDatabaseService(objectMapper, dataSource);
        JobInstanceDatabaseService jobInstanceDatabaseService = new JobInstanceDatabaseService(objectMapper, dataSource, jobDefinitionDatabaseService);
        UserEmailsStage userEmailsStage = new UserEmailsStage(
                DefaultObjectMapper.get(),
                jiraUserEmailsService,
                inventoryService,
                jobDefinitionDatabaseService
        );
        JobTrackingUtilsService jobTrackingUtilsService = new JobTrackingUtilsService(jobInstanceDatabaseService, "test-worker");
        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        EtlJobRunner jobRunner = new EtlJobRunner(gcsUtils, objectMapper, jobInstanceDatabaseService, jobTrackingUtilsService, jobDefinitionDatabaseService, meterRegistry);
        // region jira
        EtlProcessorRegistry etlProcessorRegistry = mock(EtlProcessorRegistry.class);
        EtlProcessor mockJiraEtlProcessor = mock(EtlProcessor.class);
        when(etlProcessorRegistry.getAggProcessor(anyString())).thenReturn(mockJiraEtlProcessor);

        GcsStorageService gcsStorageService = new GcsStorageService("etl-payload", "");
        IngestionResultPayloadUtils ingestionResultPayloadUtils = new IngestionResultPayloadUtils(objectMapper, controlPlaneService, jobInstanceDatabaseService, jobDefinitionDatabaseService, gcsStorageService);
        EtlEngine engine = new EtlEngine(1, 30, jobRunner, jobTrackingUtilsService, jobInstanceDatabaseService, etlProcessorRegistry, meterRegistry, ingestionResultPayloadUtils);
        JobContext jobContext = JobContext.builder()
                .jobInstanceId(JobInstanceId.builder()
                        .jobDefinitionId(UUID.fromString("c1d1deb3-65ae-4b55-9d10-c0a353d465d6"))
                        .instanceId(2712)
                        .build())
                .tenantId("foo")
                .integrationId("4284")
                .integrationType("jira")
                .jobScheduledStartTime(new Date())
                .etlProcessorName("JiraEtlProcessor")
                .timeoutInMinutes(10L)
                .isFull(true)
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .gcsRecords(null)
                .build();

        var engineJob = engine.submitJob(jobContext);
        var result = engineJob.get().f.get();
    }
}
