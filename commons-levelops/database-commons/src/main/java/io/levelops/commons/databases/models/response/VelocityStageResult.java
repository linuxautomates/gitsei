package io.levelops.commons.databases.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import lombok.Builder;
import lombok.Value;

import java.util.concurrent.TimeUnit;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = VelocityStageResult.VelocityStageResultBuilder.class)
public class VelocityStageResult {
    @JsonProperty("lower_limit_value")
    private final Long lowerLimitValue;

    @JsonProperty("lower_limit_unit")
    private final TimeUnit lowerLimitUnit;

    @JsonProperty("upper_limit_value")
    private final Long upperLimitValue;

    @JsonProperty("upper_limit_unit")
    private final TimeUnit upperLimitUnit;

    @JsonProperty("rating")
    private final VelocityConfigDTO.Rating rating;
}
