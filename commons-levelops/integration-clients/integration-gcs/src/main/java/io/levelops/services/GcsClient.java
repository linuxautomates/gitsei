package io.levelops.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageClass;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import io.levelops.integrations.gcs.models.GcsDataResult;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.threeten.bp.Duration;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Needs GOOGLE_APPLICATION_CREDENTIALS defined as an environment variable for authentication.
 */
@Log4j2
public class GcsClient {

    private static final String PLAIN_TEXT = "text/plain";
    private final Storage storage;

    public GcsClient() {

        /* defult retry settings of defaultStorageInstance
        RetrySettings defaultRetrySettings = RetrySettings.newBuilder()
                .setMaxAttempts(6)
                .setInitialRetryDelay(Duration.ofMillis(1000L))
                .setMaxRetryDelay(Duration.ofMillis(32_000L))
                .setRetryDelayMultiplier(2.0)
                .setTotalTimeout(Duration.ofMillis(50_000L))
                .setInitialRpcTimeout(Duration.ofMillis(50_000L))
                .setRpcTimeoutMultiplier(1.0)
                .setMaxRpcTimeout(Duration.ofMillis(50_000L)).build();*/

        RetrySettings retrySettings = RetrySettings
                .newBuilder()
                .setMaxAttempts(15)
                .setInitialRetryDelay(Duration.ofMillis(1000L))
                .setMaxRetryDelay(Duration.ofMillis(32_000L))
                .setRetryDelayMultiplier(2.0)
                .setTotalTimeout(Duration.ofMinutes(5L))
                .setInitialRpcTimeout(Duration.ofMillis(50_000L))
                .setRpcTimeoutMultiplier(2.0)
                .setMaxRpcTimeout(Duration.ofMinutes(5L)).build();

        StorageOptions storageOption = StorageOptions.getDefaultInstance();
        storage = storageOption.toBuilder().setRetrySettings(retrySettings).build().getService();
    }

    // region GCS calls

    public Bucket createBucket(String bucketName) {
        try {
            BucketInfo bucketInfo = BucketInfo.newBuilder(bucketName)
                    // See here for possible values: http://g.co/cloud/storage/docs/storage-classes
                    .setStorageClass(StorageClass.MULTI_REGIONAL)
                    // Possible values: http://g.co/cloud/storage/docs/bucket-locations#location-mr
                    .setLocation("us")
                    .build();
            return storage.create(bucketInfo);
        } catch (StorageException e) {
            if (e.getMessage().contains("You already own this bucket. Please select another name.")) {
                log.debug("Ignoring bucket creation request, bucket already exists. (name={})", bucketName);
                return null;
            }
            throw e;
        }
    }

    private Blob uploadPlainTextData(String bucketName, String path, String content) {
        return uploadData(bucketName, path, PLAIN_TEXT, content.getBytes(UTF_8));
    }

    private Blob uploadData(String bucketName, String path, String contentType, byte[] content) {
        BlobId blobId = BlobId.of(bucketName, path);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();
        log.info("Uploading '{}' content to {}:{}", contentType, bucketName, path);
        return storage.create(blobInfo, content);
    }

    private void downloadData(String bucketName, String path, OutputStream output) {
        Blob blob = storage.get(BlobId.of(bucketName, path));
        blob.downloadTo(output);
    }

    // endregion

    public GcsDataResult pushOne(GcsData payload) {
        Blob blob = uploadData(payload.getBucketName(), payload.getPath(), payload.getContentType(), payload.getContent());
        return GcsDataResult.builder()
                .blobId(fromApiResponse(blob.getBlobId()))
                .uri(String.format("gs://%s/%s", blob.getBlobId().getBucket(), blob.getBlobId().getName()))
                .htmlUri(String.format("https://console.cloud.google.com/storage/browser/_details/%s/%s", blob.getBlobId().getBucket(), blob.getBlobId().getName()))
                .build();
    }

    public GcsDataResult pushMany(Stream<GcsData> dataStream) {
        dataStream.forEach(this::pushOne); // TODO batch?
        return GcsDataResult.builder().build(); // TODO output batch link?
    }

    public GcsData getData(String bucketName, String path) throws FileNotFoundException {
        try {
            Blob blob = storage.get(BlobId.of(bucketName, path));
            return GcsData.builder()
                    .content(blob.getContent())
                    .build();
        } catch (Exception e) {
            log.warn("File not found ", e);
            throw new FileNotFoundException("File " +path + " not found");
        }
    }

    private io.levelops.integrations.gcs.models.BlobId fromApiResponse(BlobId blobId) {
        return io.levelops.integrations.gcs.models.BlobId.builder()
                .bucket(blobId.getBucket())
                .name(blobId.getName())
                .generation(blobId.getGeneration())
                .build();
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GcsData.GcsDataBuilder.class)
    public static class GcsData {

        @JsonProperty("bucket_name")
        private String bucketName;
        @JsonProperty("path")
        private String path;
        @JsonProperty("content_type")
        private String contentType;
        @JsonProperty("content")
        private byte[] content;

        public static class GcsDataBuilder {

            /**
             * Convenience method to pass plain text string.
             * Do not use content() and contentType() when using this.
             */
            public GcsDataBuilder plainTextContent(String content) {
                this.content(content.getBytes(UTF_8));
                this.contentType(PLAIN_TEXT);
                return this;
            }

        }
    }

}
