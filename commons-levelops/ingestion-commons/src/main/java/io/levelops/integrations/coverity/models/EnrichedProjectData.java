package io.levelops.integrations.coverity.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = EnrichedProjectData.EnrichedProjectDataBuilder.class)
public class EnrichedProjectData {

    @JsonProperty("stream")
    Stream stream;

    @JsonProperty("snapshot")
    Snapshot snapshot;

    @JsonProperty("defects")
    List<Defect> defects;
}
