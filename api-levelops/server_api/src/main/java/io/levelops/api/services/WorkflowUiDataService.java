package io.levelops.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Log4j2
@Service
@SuppressWarnings("unused")
public class WorkflowUiDataService {

    private final String bucketName;
    private final Storage storage;

    @Autowired
    public WorkflowUiDataService(
            @Value("${WORKFLOW_BUCKET_NAME:levelops-workflows}") final String bucketName,
            ObjectMapper objectMapper,
            Storage storage) {
        this.bucketName = bucketName;
        this.storage = storage;
    }

    public String downloadData(String tenantId, String workflowId) {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notBlank(workflowId, "workflowId cannot be null or empty.");

        String path = generatePath(tenantId, workflowId);
        return downloadDataFromGcs(bucketName, workflowId);
    }

    public String uploadData(String tenantId, String workflowId, String data) {
        Validate.notBlank(tenantId, "tenantId cannot be null or empty.");
        Validate.notBlank(workflowId, "workflowId cannot be null or empty.");
        Validate.notBlank(data, "data cannot be null or empty.");
        String path = generatePath(tenantId, workflowId);
        uploadDataToGcs(bucketName, path, "application/json", data.getBytes(StandardCharsets.UTF_8));
        return path;
    }

    private String downloadDataFromGcs(String bucketName, String path) {
        log.info("Downloading content from {}:{}", bucketName, path);
        BlobId blobId = BlobId.of(bucketName, path);
        Blob blob = storage.get(blobId);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blob.downloadTo(outputStream);
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private Blob uploadDataToGcs(String bucketName, String path, String contentType, byte[] content) {
        BlobId blobId = BlobId.of(bucketName, path);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();
        log.info("Uploading '{}' content to {}:{}", contentType, bucketName, path);
        return storage.create(blobInfo, content);
    }

    private static String generatePath(String tenantId, String pathSuffix) {
        return String.format("workflows/tenant-%s/%s", tenantId, pathSuffix);
    }

    public void deleteResultsFromGcs(String path) {
        storage.delete(bucketName, path);
    }

}
