package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ReviewResponse.ReviewResponseBuilder.class)
public class ReviewResponse {

    @JsonProperty("lastSeen")
    Long lastSeen;

    @JsonProperty("reviews")
    List<HelixSwarmReview> reviews;

    @JsonProperty("totalCount")
    Integer totalCount;
}
