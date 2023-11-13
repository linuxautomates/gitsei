package io.levelops.ingestion.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = LogMetadata.LogMetadataBuilder.class)
public class LogMetadata {

    @JsonProperty("log_bucket")
    String logBucket;

    @JsonProperty("log_location")
    String logLocation;

    @JsonProperty("log_metadata")
    Map<String, Object> metadata;

}
