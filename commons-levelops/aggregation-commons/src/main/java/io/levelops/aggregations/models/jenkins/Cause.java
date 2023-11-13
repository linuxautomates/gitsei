package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
@JsonDeserialize(builder = Cause.CauseBuilder.class)
public class Cause {
    @JsonProperty("shortDescription")
    private final String shortDescription;
    @JsonProperty("userId")
    private final String userId;
    @JsonProperty("userName")
    private final String userName;

    @JsonProperty("upstreamBuild")
    private final Long upstreamBuild;
    @JsonProperty("upstreamProject")
    private final String upstreamProject;
    @JsonProperty("upstreamUrl")
    private final String upstreamUrl;
}
