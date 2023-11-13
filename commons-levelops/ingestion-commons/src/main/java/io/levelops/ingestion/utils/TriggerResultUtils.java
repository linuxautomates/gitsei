package io.levelops.ingestion.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.JobStatus;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.models.controlplane.TriggerResults;
import io.levelops.integrations.gcs.models.BlobId;
import io.levelops.integrations.gcs.models.GcsDataResult;
import io.levelops.integrations.storage.models.StorageResult;

import java.util.Collection;
import java.util.stream.Stream;

public class TriggerResultUtils {

    public static Stream<StorageResult> streamStorageResults(ObjectMapper objectMapper, MultipleTriggerResults results) {
        if (results == null) {
            return Stream.empty();
        }
        // we only expect 1 trigger per integration, so getting first
        return IterableUtils.getFirst(results.getTriggerResults()).stream()
                .map(TriggerResults::getJobs)
                .flatMap(Collection::stream)
                .filter(job -> JobStatus.SUCCESS.equals(job.getStatus()))
                .map(JobDTO::getResult)
                .map(r -> convertControllerIngestionResultList(objectMapper, r))
                .map(ListResponse::getRecords)
                .flatMap(Collection::stream);
    }

    public static ListResponse<StorageResult> convertControllerIngestionResultList(ObjectMapper objectMapper, Object o) {
        return objectMapper.convertValue(o, ListResponse.typeOf(objectMapper, StorageResult.class));
    }

    public static Stream<BlobId> streamGcsBlobIds(Stream<StorageResult> storageResultStream) {
        if (storageResultStream == null) {
            return Stream.empty();
        }
        return storageResultStream
                .map(StorageResult::getRecords)
                .flatMap(Collection::stream)
                .map(GcsDataResult::getBlobId);
    }

}
