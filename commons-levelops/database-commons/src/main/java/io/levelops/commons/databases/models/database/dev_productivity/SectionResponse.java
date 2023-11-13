package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SectionResponse.SectionResponseBuilder.class)
public class SectionResponse {
    @JsonProperty("name")
    private final String name;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("order")
    private final Integer order;

    @JsonProperty("feature_responses")
    private final List<FeatureResponse> featureResponses;

    @JsonProperty("score")
    private final Integer score;

    @JsonProperty("weighted_score")
    private final Integer weightedScore;

    @JsonProperty("enabled")
    private Boolean enabled;
}
