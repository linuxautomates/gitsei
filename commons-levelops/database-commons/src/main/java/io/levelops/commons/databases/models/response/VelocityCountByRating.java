package io.levelops.commons.databases.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import lombok.Builder;
import lombok.Value;

import java.util.concurrent.TimeUnit;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = VelocityCountByRating.VelocityCountByRatingBuilder.class)
public class VelocityCountByRating {

    @JsonProperty("rating")
    private final VelocityConfigDTO.Rating rating;

    @JsonProperty("count")
    Long count;

}
