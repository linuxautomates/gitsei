package io.levelops.etl.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import io.levelops.commons.models.ListResponse;
import io.levelops.integrations.gcs.models.GcsDataResult;
import io.levelops.integrations.storage.models.StorageContent;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Log4j2
@Service
public class GcsUtils {

    private final Storage storage;
    private final ObjectMapper objectMapper;

    @Autowired
    public GcsUtils(@NonNull Storage storage, @NonNull ObjectMapper objectMapper) {
        this.storage = storage;
        this.objectMapper = objectMapper;
    }

    // merge this function in commons code
    public <C> StorageContent<ListResponse<C>> fetchRecordsFromGcs(GcsDataResult record, Class<C> contentTypeClass, JavaType cContentTypeClass, String customer, String integrationId) throws JsonProcessingException {
        String issuesBucket = record.getBlobId().getBucket();
        String issuesPath = record.getBlobId().getName();
        Long issuesGeneration = record.getBlobId().getGeneration();
        byte[] bytes = null;

        RetryPolicy<Object> retryPolicy = RetryPolicy.builder()
                .handle(Exception.class)
                .withDelay(Duration.ofSeconds(2))
                .withMaxRetries(3)
                .onRetry(e -> log.error("Failure in reading gcs file - retrying", e.getLastException()))
                .onFailure(e -> log.error("Failed to read gcs file - reties exceeded", e.getException()))
                .build();

        try {
            bytes = Failsafe.with(retryPolicy).get(() -> storage.readAllBytes(BlobId.of(issuesBucket, issuesPath, issuesGeneration)));
        } catch (Exception e) {
            log.warn("Error downloading file customer={}, integrationId={}, {}, {}, {}, will skip this file!", customer, integrationId, issuesBucket, issuesPath, issuesGeneration, e);
            return null;
        }
        StorageContent<ListResponse<C>> response = objectMapper.readValue(new String(bytes),
                StorageContent.getListStorageContentJavaType(objectMapper, cContentTypeClass));
        log.info("Fetched {} records from file {}.", response.getData().getRecords().size(), issuesPath);
        return response;
    }
}

