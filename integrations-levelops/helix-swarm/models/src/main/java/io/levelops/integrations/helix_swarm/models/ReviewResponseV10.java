package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ReviewResponseV10.ReviewResponseV10Builder.class)
public class ReviewResponseV10 {
    @JsonProperty("data")
    ReviewResponseV10Data data;
}
