package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ReviewFileResponse.ReviewFileResponseBuilder.class)
public class ReviewFileResponse {
    @JsonProperty("data")
    ReviewFileResponseData data;
}

