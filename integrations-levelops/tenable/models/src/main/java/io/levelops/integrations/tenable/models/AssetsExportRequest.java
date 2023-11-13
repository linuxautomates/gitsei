package io.levelops.integrations.tenable.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

/**
 * Request parameters for fetching assets. For more information check parameters of <a href="https://developer.tenable.com/reference#exports-assets-request-export</a>
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = AssetsExportRequest.AssetsExportRequestBuilder.class)
public class AssetsExportRequest {
    @JsonProperty("chunk_size")
    Integer chunkSize;

    @JsonProperty("filters")
    Filter filters;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Filter.FilterBuilder.class)
    public static class Filter {
        @JsonProperty("created_at")
        Long createdAt;

        @JsonProperty("updated_at")
        Long updatedAt;
    }
}
