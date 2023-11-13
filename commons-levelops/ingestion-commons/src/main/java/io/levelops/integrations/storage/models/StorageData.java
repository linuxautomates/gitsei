package io.levelops.integrations.storage.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import lombok.Builder;
import lombok.Value;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Model to represent Data being pushed to Storage.
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = StorageData.StorageDataBuilder.class)
public class StorageData {
    @JsonProperty("integration_key")
    private IntegrationKey integrationKey;
    @JsonProperty("integration_type")
    private String integrationType;
    @JsonProperty("data_type")
    private String dataType;
    @JsonProperty("job_id")
    private String jobId;
    @JsonProperty("relative_path")
    private String relativePath; // prefix will be added
    @JsonProperty("content_type")
    private String contentType;
    @JsonProperty("content")
    private byte[] content;

    public static class StorageDataBuilder {

        private static final String PLAIN_TEXT = "text/plain";
        private static final String APPLICATION_JSON = "application/json; charset=UTF-8";

        /**
         * Convenience method to pass plain text string.
         * Do not use content() and contentType() when using this.
         */
        public StorageDataBuilder plainTextContent(String content) {
            this.content(content.getBytes(UTF_8));
            this.contentType(PLAIN_TEXT);
            return this;
        }

        public StorageDataBuilder jsonContent(String content) {
            this.content(content.getBytes(UTF_8));
            this.contentType(APPLICATION_JSON);
            return this;
        }

    }
}