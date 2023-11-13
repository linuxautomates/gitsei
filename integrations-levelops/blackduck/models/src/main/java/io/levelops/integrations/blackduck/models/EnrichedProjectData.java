package io.levelops.integrations.blackduck.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = EnrichedProjectData.EnrichedProjectDataBuilder.class)
public class EnrichedProjectData {
    @JsonProperty("project")
    BlackDuckProject project;

    @JsonProperty("version")
    BlackDuckVersion version;

    @JsonProperty("issues")
    List<BlackDuckIssue> issues;
}
