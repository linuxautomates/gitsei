package io.levelops.plugins.services;

import com.google.cloud.storage.Storage;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.Validate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Log4j2
@Value
public class JenkinsPluginJobRunCompleteStorageService extends GcsStorageService{
    private final String JSON_FILE_NAME_FORMAT = "%s-json-file.json";
    private final String ZIP_FILE_NAME_FORMAT = "%s-zip-file.zip";
    private final Storage storage;
    private final String bucketName;

    public JenkinsPluginJobRunCompleteStorageService(Storage storage, String bucketName) {
        this.storage = storage;
        this.bucketName = bucketName;
    }

    private String generateFileName(String resultId, boolean jsonFile){
        String fileName = (jsonFile) ? String.format(JSON_FILE_NAME_FORMAT, resultId) : String.format(ZIP_FILE_NAME_FORMAT, resultId);
        return fileName;
    }

    private String uploadFile(String tenantId, String resultId, String contentType, byte[] content, boolean jsonFile) {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notBlank(resultId, "resultId cannot be null or empty.");
        Validate.notNull(content, "content cannot be null.");
        String path = generatePath(tenantId, Instant.now(), generateFileName(resultId, jsonFile));
        uploadBytes(storage, bucketName, path, contentType, content);
        return path;
    }
    @Deprecated
    public String uploadJsonFile(String tenantId, String resultId, String contentType, byte[] content) {
        return uploadFile(tenantId, resultId, contentType, content, true);
    }
    @Deprecated
    public String uploadResultsZipFile(String tenantId, String resultId, String contentType, byte[] content) {
        return uploadFile(tenantId, resultId, contentType, content, false);
    }

    private String uploadFileStream(String tenantId, String resultId, String contentType, InputStream is, boolean jsonFile) throws IOException {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notBlank(resultId, "resultId cannot be null or empty.");
        Validate.notNull(is, "InputStream cannot be null.");
        String path = generatePath(tenantId, Instant.now(), generateFileName(resultId, jsonFile));
        upload(storage, bucketName, path, contentType, is);
        return path;
    }

    public String uploadJsonFile(String tenantId, String resultId, String contentType, InputStream is) throws IOException {
        return uploadFileStream(tenantId, resultId, contentType, is, true);
    }
    public String uploadResultsZipFile(String tenantId, String resultId, String contentType, InputStream is) throws IOException {
        return uploadFileStream(tenantId, resultId, contentType, is, false);
    }

    private static String generatePath(String tenantId, Instant date, String fileName) {
        return String.format("pre-results/tenant-%s/%s/%s",
                tenantId,
                DateTimeFormatter.ofPattern("uuuu/MM/dd", Locale.US).format(date.atZone(ZoneOffset.UTC)),
                fileName);
    }

    public void deleteResult(String gcsPath) {
        storage.delete(bucketName, gcsPath);
    }

    @Deprecated
    public byte[] downloadResult(String gcsPath) {
        log.debug("Download content form {}:{}", bucketName, gcsPath);
        return storage.readAllBytes(bucketName, gcsPath);
    }

    public void downloadResult(String gcsPath, final File destinationFile) throws IOException {
        download(storage, bucketName, gcsPath, destinationFile);
    }
}
