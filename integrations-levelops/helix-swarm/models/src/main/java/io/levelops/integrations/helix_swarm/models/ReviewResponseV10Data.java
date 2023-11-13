package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ReviewResponseV10Data.ReviewResponseV10DataBuilder.class)
public class ReviewResponseV10Data {
    @JsonProperty("reviews")
    List<HelixSwarmReview> reviews;
}
