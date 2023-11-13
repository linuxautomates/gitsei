package io.levelops.integrations.tenable.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Bean for the status of Asset export request <a href="https://cloud.tenable.com/vulns/export/export_uuid/status</a>
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ExportStatusResponse.ExportStatusResponseBuilder.class)
public class ExportStatusResponse {
    @JsonProperty
    String status;

    @JsonProperty("chunks_available")
    List<Integer> chunksAvailable;

    @JsonProperty("chunks_failed")
    List<Integer> chunksFailed;

    @JsonProperty("chunks_cancelled")
    List<Integer> chunksCancelled;
}
