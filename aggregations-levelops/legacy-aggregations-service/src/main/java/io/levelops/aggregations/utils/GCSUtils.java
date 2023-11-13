package io.levelops.aggregations.utils;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import io.levelops.commons.utils.ListUtils;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Log4j2
public class GCSUtils {

    public static String uploadLogsToGCS(Storage storage, String bucketName, String company, String stageId, String stepId, List<String> stepLogs) {
        String path;
        try {
            if (!ListUtils.isEmpty(stepLogs)) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(String.join("", stepLogs));
                byte[] bytes = bos.toByteArray();
                log.debug("Uploading step log to gcs starting");
                path = generateJobRunStageStepLogsPath(company, Instant.now(), stageId, stepId);
                uploadDataToGcs(bucketName, path, bytes, storage);
                log.debug("Uploading step log to gcs completed");

                return path;
            } else {
                log.debug("step log is null or unzipFolder is null, cannot upload logs to gcs!");
                return null;
            }
        } catch (IOException e) {
            log.error("Failed to upload step log file to gcs");
            return null;
        }
    }

    private static String generateJobRunStageStepLogsPath(String tenantId, Instant date, String stageId, String stepId) {
        return String.format("%s/tenant-%s/%s/%s/%s.log",
                "cicd-job-run-stage-step-logs",
                tenantId,
                DateTimeFormatter.ofPattern("uuuu/MM/dd", Locale.US).format(date.atZone(ZoneOffset.UTC)),
                stageId,
                stepId);
    }

    public static void uploadDataToGcs(String bucketName, String gcsPath, byte[] content, Storage storage) {
        BlobId blobId = BlobId.of(bucketName, gcsPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType("application/json")
                .build();
        log.info("Uploading content to {}:{}", bucketName, gcsPath);
        storage.create(blobInfo, content);
    }
}
