package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DependencyPath.DependencyPathBuilder.class)
public class DependencyPath {

    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("version")
    String version;

    @JsonProperty("isResolved")
    Boolean isResolved;

    @JsonProperty("isDevelopment")
    Boolean isDevelopment;

}
