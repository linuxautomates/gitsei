package io.levelops.commons.databases.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = VelocityRatingResult.VelocityRatingResultBuilder.class)
public class VelocityRatingResult {
    @JsonProperty("index")
    Integer index;
    @JsonProperty("name")
    String name;
    @JsonProperty("buckets")
    List<VelocityCountByRating> buckets;
}
