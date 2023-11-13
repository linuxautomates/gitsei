package io.levelops.ingestion.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = LogWithMetadata.LogWithMetadataBuilder.class)
public class LogWithMetadata {

    @JsonProperty("id")
    String id;

    @JsonIgnore
    byte[] log;

    @JsonProperty("log_metadata")
    Map<String, Object> metadata;
}
