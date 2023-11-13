package io.levelops.plugins.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.Validate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Log4j2
@Builder
public class PluginResultsStorageService extends GcsStorageService{
    private final String bucketName;
    private final Storage storage;
    private final ObjectMapper objectMapper;

    public PluginResultsStorageService(String bucketName, Storage storage, ObjectMapper objectMapper) {
        this.bucketName = bucketName;
        this.storage = storage;
        this.objectMapper = objectMapper;
    }

    @Deprecated
    public String downloadResultsAsString(String gcsPath) {
        log.debug("Download content form {}:{}", bucketName, gcsPath);
        return new String(storage.readAllBytes(bucketName, gcsPath), StandardCharsets.UTF_8);
    }

    public void downloadResults(String gcsPath, final File destinationFile) throws IOException {
        download(storage, bucketName, gcsPath, destinationFile);
    }

    public Map<String, Object> downloadResults(String gcsPath) throws JsonProcessingException {
        String json = downloadResultsAsString(gcsPath);
        return objectMapper.readValue(json, objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
    }

    public String uploadResults(String tenantId, String tool, String resultId, String contentType, byte[] content) {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notBlank(tool, "tool cannot be null or empty.");
        Validate.notBlank(resultId, "resultId cannot be null or empty.");
        Validate.notNull(content, "content cannot be null.");
        String path = generatePath(tenantId, tool, Instant.now(), resultId);
        uploadBytes(storage, bucketName, path, contentType, content);
        return path;
    }

    public String uploadResults(String tenantId, String tool, String resultId, String contentType, InputStream is) throws IOException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notBlank(tool, "tool cannot be null or empty.");
        Validate.notBlank(resultId, "resultId cannot be null or empty.");
        Validate.notNull(is, "InputStream cannot be null.");
        String path = generatePath(tenantId, tool, Instant.now(), resultId);
        upload(storage, bucketName, path, contentType, is);
        return path;
    }

    private static String generatePath(String tenantId, String tool, Instant date, String pathSuffix) {
        return String.format("results/tenant-%s/tool-%s/%s/%s",
                tenantId,
                tool,
                DateTimeFormatter.ofPattern("uuuu/MM/dd", Locale.US).format(date.atZone(ZoneOffset.UTC)),
                pathSuffix);
    }

    public void deleteResults(String path) {
        storage.delete(bucketName, path);
    }

}
