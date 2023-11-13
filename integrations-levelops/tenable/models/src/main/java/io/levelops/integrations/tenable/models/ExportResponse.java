package io.levelops.integrations.tenable.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

/**
 * Bean for export request response <a href="https://cloud.tenable.com/vulns/export</a>
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ExportResponse.ExportResponseBuilder.class)
public class ExportResponse {
    @JsonProperty("export_uuid")
    String exportUUID;
}
