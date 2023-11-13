package io.levelops.services;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.storage.models.StorageData;
import io.levelops.integrations.storage.models.StorageResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Service used by Ingestion and Server API to push ingestion data to GCS
 */
public class GcsStorageService {

    private final GcsClient gcsClient;
    private final String bucketName;
    private final String pathPrefix;

    public GcsStorageService(String bucketName, String pathPrefix) {
        this.bucketName = bucketName;
        this.pathPrefix = sanitizePathPrefix(pathPrefix);
        gcsClient = new GcsClient();
    }

    private GcsClient.GcsData convertToGcs(StorageData payload) {
        Validate.notNull(payload.getIntegrationKey(), "payload.getIntegrationKey() cannot be null.");
        Validate.notBlank(payload.getJobId(), "payload.getJobId() cannot be null or empty.");
        Validate.notBlank(payload.getRelativePath(), "payload.getRelativePath() cannot be null or empty.");
        return GcsClient.GcsData.builder()
                .bucketName(bucketName)
                .path(generatePath(pathPrefix, payload.getIntegrationKey(), payload.getJobId(), payload.getRelativePath()))
                .contentType(payload.getContentType())
                .content(payload.getContent())
                .build();
    }

    protected static String sanitizePathPrefix(String pathPrefix) {
        String stripped = StringUtils.strip(pathPrefix, "/");
        if (StringUtils.isEmpty(stripped)) {
            return "";
        }
        return stripped + "/";
    }

    private static String generatePath(String pathPrefix, IntegrationKey integrationKey, String jobId, String pathSuffix) {
        return generatePath(pathPrefix, integrationKey, jobId, Instant.now(), pathSuffix);
    }

    protected static String generatePath(String pathPrefix, IntegrationKey integrationKey, String jobId, Instant date, String pathSuffix) {
        return String.format("%stenant-%s/integration-%s/%s/job-%s/%s",
                pathPrefix, // must have trailing '/' if not empty (see sanitizeInputPathPrefix())
                integrationKey.getTenantId(),
                integrationKey.getIntegrationId(),
                DateTimeFormatter.ofPattern("uuuu/MM/dd", Locale.US).format(date.atZone(ZoneOffset.UTC)),
                jobId,
                pathSuffix);
    }

    public StorageResult pushOne(StorageData data) {
        return StorageResult.builder()
                .record(gcsClient.pushOne(convertToGcs(data)))
                .build();
    }

    public StorageResult pushMany(Stream<StorageData> dataStream) {
        return StorageResult.builder()
                .record(gcsClient.pushMany(dataStream.map(this::convertToGcs)))
                .build();
    }

    public StorageData read(String fileName) throws IOException {
        return StorageData.builder()
                .content(gcsClient.getData(bucketName, sanitizeFilePath(fileName)).getContent())
                .build();
    }

    String sanitizeFilePath(String fileName) {
        return pathPrefix + fileName;
    }
}
