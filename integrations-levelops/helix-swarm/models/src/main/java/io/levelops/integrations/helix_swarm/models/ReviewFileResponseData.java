package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ReviewFileResponseData.ReviewFileResponseDataBuilder.class)
public class ReviewFileResponseData {
    @JsonProperty("root")
    String root;
    @JsonProperty("limited")
    Boolean limited;
    @JsonProperty("files")
    List<ReviewFileInfo> files;
}
