package io.levelops.etl.utils;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregation_shared.test_utils.JobInstanceTestUtils;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.models.SnapshottingSettings;
import io.levelops.aggregations_shared.utils.IngestionResultPayloadUtils;
import io.levelops.aggs.test_utilities.TestUtils;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.GcsDataResultWithDataType;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.etl.parameter_suppliers.JiraParameterSupplier;
import io.levelops.etl.parameter_suppliers.JobDefinitionParameterSupplierRegistry;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.models.controlplane.TriggerResults;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.gcs.models.BlobId;
import io.levelops.integrations.gcs.models.GcsDataResult;
import io.levelops.integrations.storage.models.StorageData;
import io.levelops.integrations.storage.models.StorageResult;
import io.levelops.services.GcsStorageService;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SchedulingUtilsTest {
    @Mock
    ControlPlaneService controlPlaneService;

    @Mock
    IntegrationService integrationService;

    @Mock
    IntegrationTrackingService integrationTrackingService;

    @Mock
    JobInstanceDatabaseService jobInstanceDatabaseService;

    @Mock
    JobDefinitionDatabaseService jobDefinitionDatabaseService;

    @Mock
    GcsStorageService gcsStorageService;

    ObjectMapper objectMapper;

    JiraParameterSupplier jiraParameterSupplier;
    JobDefinitionParameterSupplierRegistry registry;
    IngestionResultPayloadUtils ingestionResultPayloadUtils;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        objectMapper = DefaultObjectMapper.get();
        jiraParameterSupplier = new JiraParameterSupplier(integrationService, integrationTrackingService, jobInstanceDatabaseService, SnapshottingSettings.builder().build());
        var genericIntegrationJobSupplier = new JiraParameterSupplier(integrationService, integrationTrackingService, jobInstanceDatabaseService, SnapshottingSettings.builder().build()) {
            @Override
            public JobType getJobType() {
                return JobType.GENERIC_INTEGRATION_JOB;
            }

            @NotNull
            @Override
            public String getEtlProcessorName() {
                return "generic";
            }
        };
        registry = new JobDefinitionParameterSupplierRegistry(List.of(jiraParameterSupplier, genericIntegrationJobSupplier));
        ingestionResultPayloadUtils = new IngestionResultPayloadUtils(
                objectMapper, controlPlaneService, jobInstanceDatabaseService, jobDefinitionDatabaseService, gcsStorageService);
    }

    @Test
    public void testGetGcsDataResultForJobDefinitionFull() throws IngestionServiceException, SQLException {
        when(jobInstanceDatabaseService.stream(any())).thenReturn(Stream.of());

        long t = 0;
        long tplus1 = t + 1;
        // Create 2 jobDto's with 2 gcs files each of data type jira
        Pair<JobDTO, List<StorageResult>> jPair1 = TestUtils.createIngestionJobDto(objectMapper, List.of("jira", "jira"), "j1", t);
        Pair<JobDTO, List<StorageResult>> jPair2 = TestUtils.createIngestionJobDto(objectMapper, List.of("jira", "jira"), "j2", tplus1);

        TriggerResults result = TriggerResults.builder()
                .jobs(List.of(jPair2.getLeft(), jPair1.getLeft()))
                .build();
        when(controlPlaneService.getAllTriggerResults(any(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(MultipleTriggerResults.builder()
                .triggerResults(List.of(result)).build());

        DbJobDefinition jobDefinition = DbJobDefinition.builder()
                .id(UUID.randomUUID())
                .integrationId("i1")
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .build();
        var gcsResults = ingestionResultPayloadUtils.getGcsDataResultForJobDefinition(jobDefinition, true);
        assertThat(gcsResults).hasSize(4);
        var jobIdSet = gcsResults.stream().map(a -> a.getIngestionJobId()).collect(Collectors.toSet());
        assertThat(jobIdSet).containsExactly("j1", "j2");
    }

    private DbJobInstance createJobInstance(List<String> ingestionJobIds, String gcsFilename, boolean isFull) throws IOException {
        return DbJobInstance.builder()
                .payloadGcsFilename(gcsFilename)
                .isFull(isFull)
                .build();
    }

    private DbJobInstance createJobInstanceAndPayload(List<String> ingestionJobIds, boolean isFull) throws IOException {
        var payload = JobInstanceTestUtils.createPayload(ingestionJobIds.stream()
                .map(ingestionJobId -> GcsDataResultWithDataType.builder()
                        .index(0)
                        .dataTypeName("tickets")
                        .ingestionJobId(ingestionJobId)
                        .gcsDataResult(GcsDataResult.builder()
                                .uri("uri")
                                .htmlUri("htmluri")
                                .blobId(BlobId.builder()
                                        .name("blob")
                                        .generation(1L)
                                        .bucket("bucket")
                                        .build())
                                .build())
                        .build()).toList());
        String dummyGcsFilename = UUID.randomUUID().toString();
        when(gcsStorageService.read(dummyGcsFilename)).thenReturn(
                StorageData.builder()
                        .content(objectMapper.writeValueAsString(payload).getBytes())
                        .build()
        );
        return createJobInstance(ingestionJobIds, dummyGcsFilename, isFull);
    }

    @Test
    public void testGetIngestionJobIdsNotYetProcessed() throws IOException {
        long t = 0;
        long tplus1 = t + 1;
        long tplus2 = t + 2;
        long tplus3 = t + 3;
        // Create 5 jobDto's with 2 gcs files each of data type jira
        Pair<JobDTO, List<StorageResult>> jPair1 = TestUtils.createIngestionJobDto(objectMapper, List.of("jira", "jira"), "j1", t);
        Pair<JobDTO, List<StorageResult>> jPair2 = TestUtils.createIngestionJobDto(objectMapper, List.of("jira", "jira"), "j2", tplus1);
        Pair<JobDTO, List<StorageResult>> jPair3 = TestUtils.createIngestionJobDto(objectMapper, List.of("jira", "jira"), "j3", tplus1);
        Pair<JobDTO, List<StorageResult>> jPair4 = TestUtils.createIngestionJobDto(objectMapper, List.of("jira", "jira"), "j4", tplus2);
        Pair<JobDTO, List<StorageResult>> jPair5 = TestUtils.createIngestionJobDto(objectMapper, List.of("jira", "jira"), "j5", tplus3);

        var a = createJobInstanceAndPayload(List.of("j1", "j3"), false);
        var b = createJobInstanceAndPayload(List.of("j5"), false);
        when(jobInstanceDatabaseService.stream(any())).thenReturn(Stream.of(a, b));

        TriggerResults result = TriggerResults.builder()
                .jobs(List.of(jPair5.getLeft(), jPair4.getLeft(), jPair1.getLeft(), jPair2.getLeft(), jPair3.getLeft()))
                .build();
        var ingestionJobIdsToProcess =
                ingestionResultPayloadUtils.getIngestionJobIdsNotYetProcessed(result, DbJobDefinition.builder()
                        .id(UUID.randomUUID())
                        .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                        .build());
        // Only j2 and j4 have not been processed
        assertThat(ingestionJobIdsToProcess).containsExactly("j2", "j4");

        // Test empty result
        a = createJobInstanceAndPayload(List.of("j1", "j3"), false);
        b = createJobInstanceAndPayload(List.of("j5"), false);
        when(jobInstanceDatabaseService.stream(any())).thenReturn(Stream.of(a, b));
        TriggerResults emptyResult = TriggerResults.builder()
                .jobs(List.of())
                .build();
        ingestionJobIdsToProcess =
                ingestionResultPayloadUtils.getIngestionJobIdsNotYetProcessed(emptyResult, DbJobDefinition.builder()
                        .id(UUID.randomUUID())
                        .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                        .build());
        assertThat(ingestionJobIdsToProcess.size()).isEqualTo(0);

        // Test empty job instances
        when(jobInstanceDatabaseService.stream(any())).thenReturn(Stream.of());
        ingestionJobIdsToProcess =
                ingestionResultPayloadUtils.getIngestionJobIdsNotYetProcessed(result, DbJobDefinition.builder()
                        .id(UUID.randomUUID())
                        .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                        .build());
        assertThat(ingestionJobIdsToProcess).containsExactly("j1", "j2", "j3", "j4", "j5");


        // Test full/incremental
        a = createJobInstanceAndPayload(List.of("j4"), false);
        b = createJobInstanceAndPayload(List.of("j3"), true);
        var c = createJobInstanceAndPayload(List.of("j2"), true);
        var d = createJobInstanceAndPayload(List.of("j1"), false);
        when(jobInstanceDatabaseService.stream(any())).thenReturn(Stream.of(a, b, c, d));

        result = TriggerResults.builder()
                .jobs(List.of(jPair5.getLeft(), jPair4.getLeft(), jPair1.getLeft(), jPair2.getLeft(), jPair3.getLeft()))
                .build();
        ingestionJobIdsToProcess =
                ingestionResultPayloadUtils.getIngestionJobIdsNotYetProcessed(result, DbJobDefinition.builder()
                        .id(UUID.randomUUID())
                        .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                        .build());
        // we only look at job instances uptil the latest full, which in this case is j3 and j4 - rest are detected as not preocessed
        assertThat(ingestionJobIdsToProcess).containsExactly("j1", "j2", "j5");
    }

    @Test
    public void testScheduleGenericJob() throws IOException, IngestionServiceException {
        SchedulingUtils schedulingUtils = new SchedulingUtils(jobInstanceDatabaseService, registry, ingestionResultPayloadUtils);
        schedulingUtils.scheduleJobDefinition(DbJobDefinition.builder()
                .id(UUID.randomUUID())
                .jobType(JobType.GENERIC_INTEGRATION_JOB)
                .aggProcessorName("generic")
                .integrationType("jira")
                .build(), Instant.now(), true, false, false);
        ArgumentCaptor<DbJobInstance> argumentCaptor = ArgumentCaptor.forClass(DbJobInstance.class);
        verify(jobInstanceDatabaseService, times(1)).insertAndUpdateJobDefinition(argumentCaptor.capture(), any(), any());
        assertThat(argumentCaptor.getValue().getPayload()).isNull();
    }
}

