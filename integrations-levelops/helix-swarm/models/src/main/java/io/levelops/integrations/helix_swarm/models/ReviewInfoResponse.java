package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ReviewInfoResponse.ReviewInfoResponseBuilder.class)
public class ReviewInfoResponse {

    @JsonProperty("review")
    HelixSwarmReviewInfo review;
}
