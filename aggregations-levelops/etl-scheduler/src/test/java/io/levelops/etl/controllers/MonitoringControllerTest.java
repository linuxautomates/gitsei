package io.levelops.etl.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.GcsDataResultWithDataType;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobInstancePayload;
import io.levelops.commons.etl.models.JobPriority;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.etl.utils.SchedulingUtils;
import io.levelops.ingestion.services.ControlPlaneService;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static io.levelops.aggregation_shared.test_utils.JobInstanceTestUtils.createInstance;
import static io.levelops.aggregation_shared.test_utils.JobInstanceTestUtils.createPayload;
import static io.levelops.aggregations_shared.database.EtlDatabaseConstants.ETL_SCHEMA;
import static io.levelops.aggregations_shared.database.JobDefinitionDatabaseService.JOB_DEFINITION_TABLE;
import static org.assertj.core.api.Assertions.assertThat;

public class MonitoringControllerTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static ObjectMapper objectMapper;
    private static JobDefinitionDatabaseService jobDefinitionDatabaseService;
    private static JobInstanceDatabaseService jobInstanceDatabaseService;
    private static MonitoringController monitoringController;
    @Mock
    private static ControlPlaneService controlPlaneService;
    @Mock
    private static SchedulingUtils schedulingUtils;

    @BeforeClass
    public static void setupStatic() throws SQLException {
        objectMapper = DefaultObjectMapper.get();
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        DatabaseSchemaService schemaService = new DatabaseSchemaService(dataSource);
        schemaService.ensureSchemaExistence(ETL_SCHEMA);
        jobDefinitionDatabaseService = new JobDefinitionDatabaseService(objectMapper, dataSource);
        jobDefinitionDatabaseService.ensureTableExistence();
        jobInstanceDatabaseService = new JobInstanceDatabaseService(objectMapper, dataSource, jobDefinitionDatabaseService);
        jobInstanceDatabaseService.ensureTableExistence();
    }

    @Before
    public void setup() throws SQLException {
        String sql = "DELETE FROM " + JOB_DEFINITION_TABLE + ";";
        dataSource.getConnection().prepareStatement(sql).execute();

        MockitoAnnotations.openMocks(this);
        monitoringController = new MonitoringController(jobInstanceDatabaseService, jobDefinitionDatabaseService, controlPlaneService, schedulingUtils);
    }

    private JobInstancePayload createPayloadForIngestionJobIds(List<String> ingestionJobIds) {
        AtomicInteger i = new AtomicInteger();
        var gcs = ingestionJobIds.stream()
                .map(jobId -> GcsDataResultWithDataType.builder()
                        .index(i.getAndIncrement())
                        .ingestionJobId(jobId)
                        .build())
                .collect(Collectors.toList());
        return createPayload(gcs);
    }

    @Test
    public void testGetIngestionProcessingJobInstances() throws IOException {
        // Create a job definition for a ingestion job
        // Create a job instance with the payload containing 2 ingestion job ids: a and b
        // Call the endpoint with job id a and b
        // Assert that the endpoint returns the job instance
        DbJobDefinition jobDefinition = DbJobDefinition.builder()
                .aggProcessorName("testProcessorName")
                .ingestionTriggerId("testTriggerId")
                .id(UUID.randomUUID())
                .fullFrequencyInMinutes(30)
                .tenantId("1")
                .integrationId("1")
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .defaultPriority(JobPriority.HIGH)
                .isActive(true)
                .build();
        UUID jobDefinitionId = jobDefinitionDatabaseService.insert(jobDefinition);
        DbJobInstance jobInstance1 = createInstance(DbJobInstance.builder()
                .payload(createPayload(List.of(GcsDataResultWithDataType.builder()
                                .index(0)
                                .ingestionJobId("a")
                                .build(),
                        GcsDataResultWithDataType.builder()
                                .index(1)
                                .ingestionJobId("b")
                                .build())))
                .status(JobStatus.SUCCESS)
                .build(), jobDefinitionId);
        // Job Instances: 1 -> (a, b)
        JobInstanceId jobInstanceId1 = jobInstanceDatabaseService.insert(jobInstance1);
        var result = monitoringController.getIngestionProcessingJobInstances("1", "1", List.of("a", "b"), false);
        assertThat(result.values().stream().map(DbJobInstance::getJobInstanceId).toList()).containsExactly(jobInstanceId1, jobInstanceId1);
        result = monitoringController.getIngestionProcessingJobInstances("1", "1", List.of("a"), false);
        assertThat(result.values().stream().map(DbJobInstance::getJobInstanceId).toList()).containsExactly(jobInstanceId1);
        result = monitoringController.getIngestionProcessingJobInstances("1", "1", List.of("b"), false);
        assertThat(result.values().stream().map(DbJobInstance::getJobInstanceId).toList()).containsExactly(jobInstanceId1);
        result = monitoringController.getIngestionProcessingJobInstances("1", "1", List.of(), false);
        assertThat(result.values().stream().map(DbJobInstance::getJobInstanceId).toList()).containsExactly();

        // Job Instances: 1 -> (a, b), 2 -> (a, b)
        JobInstanceId jobInstanceId2 = jobInstanceDatabaseService.insert(jobInstance1);
        result = monitoringController.getIngestionProcessingJobInstances("1", "1", List.of("a", "b"), false);
        // Should return instance id 2 for both since it's more recent
        assertThat(result.values().stream().map(DbJobInstance::getJobInstanceId).toList()).containsExactly(jobInstanceId2, jobInstanceId2);

        // Job Instances: 1 -> (a, b), 2 -> (a, b), 3 (Failed) -> (a,b,c)
        JobInstanceId jobInstanceId3 = jobInstanceDatabaseService.insert(jobInstance1.toBuilder()
                .status(JobStatus.FAILURE)
                .payload(createPayloadForIngestionJobIds(List.of("a", "b", "c")))
                .build());
        result = monitoringController.getIngestionProcessingJobInstances("1", "1", List.of("a", "b", "c"), false);
        // Should return instance id 2 for both since it's more recent
        assertThat(result.get("a").getJobInstanceId()).isEqualTo(jobInstanceId2);
        assertThat(result.get("b").getJobInstanceId()).isEqualTo(jobInstanceId2);
        assertThat(result.get("c").getJobInstanceId()).isEqualTo(jobInstanceId3);
    }
}