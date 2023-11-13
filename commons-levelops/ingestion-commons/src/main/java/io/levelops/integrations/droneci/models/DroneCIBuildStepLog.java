package io.levelops.integrations.droneci.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DroneCIBuildStepLog.DroneCIBuildStepLogBuilder.class)
public class DroneCIBuildStepLog {

    @JsonProperty("time")
    Long time;

    @JsonProperty("pos")
    Long pos;

    @JsonProperty("out")
    String out;
}
