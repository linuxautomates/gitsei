package io.levelops.integrations.circleci.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CircleCIStepActionLog.CircleCIStepActionLogBuilder.class)
public class CircleCIStepActionLog {

    @JsonProperty("message")
    String message;

    @JsonProperty("time")
    String time;

    @JsonProperty("type")
    String type;
}
