package io.levelops.integrations.snyk.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SnykDepGraphWrapper.SnykDepGraphWrapperBuilder.class)
public class SnykDepGraphWrapper {
    @JsonProperty("depGraph")
    private final SnykDepGraph depGraph;
}
