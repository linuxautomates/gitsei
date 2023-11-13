package io.levelops.aggregations_shared.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.GcsDataResultWithDataType;
import io.levelops.commons.etl.models.JobInstancePayload;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.gcs.models.BlobId;
import io.levelops.integrations.gcs.models.GcsDataResult;
import io.levelops.services.GcsStorageService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class IngestionResultPayloadUtilsIntegrationTest {

    @Mock
    private ControlPlaneService controlPlaneService;

    @Mock
    private JobInstanceDatabaseService jobInstanceDatabaseService;

    @Mock
    private JobDefinitionDatabaseService jobDefinitionDatabaseService;
    private ObjectMapper mapper;
    private GcsStorageService gcsStorageService;
    private IngestionResultPayloadUtils ingestionResultPayloadUtils;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mapper = DefaultObjectMapper.get();
        gcsStorageService = new GcsStorageService("sidb-sei-test", "");
        ingestionResultPayloadUtils = new IngestionResultPayloadUtils(
                mapper, controlPlaneService, jobInstanceDatabaseService, jobDefinitionDatabaseService, gcsStorageService
        );
    }

    private JobInstancePayload createPayload() {
        return JobInstancePayload.builder()
                .gcsRecords(List.of(
                                GcsDataResultWithDataType.builder()
                                        .dataTypeName("pf")
                                        .ingestionJobId("my-ingestion-job")
                                        .gcsDataResult(GcsDataResult.builder()
                                                .uri("klay's-uri")
                                                .blobId(BlobId.builder()
                                                        .name("klay's-blob-id")
                                                        .bucket("sidb-sei-test")
                                                        .generation(1L)
                                                        .build())
                                                .htmlUri("klay's-html-uri")
                                                .build())
                                        .build()
                        )
                )
                .ingestionJobStatusMap(Map.of(
                        "my-ingestion-job", JobInstancePayload.IngestionJobStatus.builder()
                                .ingestionJobId("my-ingestion-job")
                                .isComplete(true)
                                .build()
                ))
                .build();
    }

    @Test
    public void testUploadAndDownload() throws IOException {
        DbJobDefinition jobDefinition = DbJobDefinition.builder()
                .id(UUID.randomUUID())
                .tenantId("warriors")
                .integrationId("1")
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .build();
        JobInstancePayload initialPayload = createPayload();
        when(jobDefinitionDatabaseService.get(any())).thenReturn(Optional.ofNullable(jobDefinition));
        GcsDataResult gcsDataResult = ingestionResultPayloadUtils.uploadPayloadToGcs(
                initialPayload,
                DbJobInstance.builder()
                        .jobDefinitionId(jobDefinition.getId())
                        .instanceId(1)
                        .build());
        System.out.println(gcsDataResult);
        JobInstancePayload downloadedPayload = ingestionResultPayloadUtils.downloadPayloadFromGcs(gcsDataResult.getBlobId().getName()).get();
        DefaultObjectMapper.prettyPrint(initialPayload);
        DefaultObjectMapper.prettyPrint(downloadedPayload);
        assertThat(initialPayload).isEqualTo(downloadedPayload);
    }

    @Test
    public void testEdgeCases() throws IOException {
        DbJobDefinition jobDefinition = DbJobDefinition.builder()
                .id(UUID.fromString("bbf0934d-1031-4f41-b5c1-6d2624cfbbc9"))
                .tenantId("warriors")
                .integrationId("1")
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .build();
        JobInstancePayload initialPayload = createPayload();
        when(jobDefinitionDatabaseService.get(any())).thenReturn(Optional.ofNullable(jobDefinition));

        GcsDataResult gcsDataResult = ingestionResultPayloadUtils.uploadPayloadToGcs(
                initialPayload,
                DbJobInstance.builder()
                        .jobDefinitionId(jobDefinition.getId())
                        .instanceId(1)
                        .build());
        var downloaded = ingestionResultPayloadUtils.downloadPayloadFromGcs(gcsDataResult.getBlobId().getName()).get();
        System.out.println(downloaded);

        // Upload the same thing again and ensure that it gets overwritten
        GcsDataResult gcsDataResult2 = ingestionResultPayloadUtils.uploadPayloadToGcs(
                JobInstancePayload.builder().build(),
                DbJobInstance.builder()
                        .jobDefinitionId(jobDefinition.getId())
                        .instanceId(1)
                        .build());

        System.out.println(gcsDataResult);
        System.out.println(gcsDataResult2);

        downloaded = ingestionResultPayloadUtils.downloadPayloadFromGcs(gcsDataResult.getBlobId().getName()).get();
        System.out.println(downloaded);

        // Download a non-existent payload and ensure that it returns empty
        var downloadedOption = ingestionResultPayloadUtils.downloadPayloadFromGcs("does-not-exist");
        assertThat(downloadedOption).isEmpty();
    }
}