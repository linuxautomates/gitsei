package io.levelops.ingestion.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = LogEntityWrapper.LogEntityWrapperBuilder.class)
public class LogEntityWrapper<D> {

    @JsonProperty("data")
    D data;

    @JsonProperty("_metadata")
    Map<String, LogMetadata> metadata;

}
