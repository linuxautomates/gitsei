package io.levelops.etl.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.etl.models.GcsDataResultWithDataType;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Log4j2
public class IngestionJobUtils {
    /**
     * This function assumes that the results of the ingestion job in question are in the format of a single page.
     * Notably, this means that the result is interpretted as a single StorageResult rather than a ListReponse of
     * StorageResult's
     * @param jobDTO ingestion job dto
     * @param objectMapper
     * @return
     */
    public static List<GcsDataResultWithDataType> getSinglePageIngestionJobResults(JobDTO jobDTO, ObjectMapper objectMapper) {
        StorageResult storageResult = objectMapper.convertValue(jobDTO.getResult(), StorageResult.class);
        if (storageResult == null || storageResult.getRecords() == null) {
            log.error("Failed to get storage results for job: {}", jobDTO);
            return List.of();
        }
        AtomicInteger index = new AtomicInteger();

        return storageResult.getRecords().stream()
                .map(gcsDataResult -> GcsDataResultWithDataType.builder()
                        .gcsDataResult(gcsDataResult)
                        .dataTypeName(storageResult.getStorageMetadata().getDataType())
                        .ingestionJobId(jobDTO.getId())
                        .index(index.getAndIncrement())
                        .build())
                .collect(Collectors.toList());
    }
}
