package io.levelops.aggs.test_utilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.integrations.gcs.models.GcsDataResult;
import io.levelops.integrations.storage.models.StorageMetadata;
import io.levelops.integrations.storage.models.StorageResult;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestUtils {
    public static Pair<JobDTO, List<StorageResult>> createIngestionJobDto(
            ObjectMapper objectMapper, List<String> dataTypes, String id) {
        return createIngestionJobDto(objectMapper, dataTypes, id, Instant.now().getEpochSecond());
    }

    public static Pair<JobDTO, List<StorageResult>> createIngestionJobDto(
            ObjectMapper objectMapper, List<String> dataTypes, String id, Long createdAt) {
        var storageResults = dataTypes.stream()
                .map(dataType -> StorageResult.builder()
                        .record(GcsDataResult.builder()
                                .htmlUri("htmluri-" + RandomStringUtils.random(5))
                                .blobId(null)
                                .uri("uri" + RandomStringUtils.random(5))
                                .build())
                        .storageMetadata(StorageMetadata.builder()
                                .dataType(dataType)
                                .build())
                        .build()
                )
                .collect(Collectors.toList());

        return Pair.of(JobDTO.builder()
                .id(id)
                .result(objectMapper.convertValue(ListResponse.of(storageResults), Map.class))
                .createdAt(createdAt)
                .build(), storageResults);
    }
}
