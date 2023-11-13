package io.levelops.integrations.circleci.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CircleCIStepAction.CircleCIStepActionBuilder.class)
public class CircleCIStepAction {

    @JsonProperty("bash_command")
    String bashCommand;

    @JsonProperty("run_time_millis")
    int runtimeMillis;

    @JsonProperty("start_time")
    Date startTime;

    @JsonProperty("end_time")
    Date endTime;

    @JsonProperty("name")
    String name;

    @JsonProperty("type")
    String type;

    @JsonProperty("truncated")
    Boolean truncated;

    @JsonProperty("index")
    Integer index;

    @JsonProperty("parallel")
    Boolean parallel;

    @JsonProperty("status")
    String status;

    @JsonProperty("allocation_id")
    String allocationId;

    @JsonProperty("output_url")
    String outputUrl;

    @JsonProperty("background")
    Boolean background;

    @JsonProperty("insignificant")
    Boolean insignificant;

    @JsonProperty("step")
    Integer step;

    @JsonProperty("run_time_millis")
    Long runTimeMillis;

    @JsonProperty("has_output")
    Boolean hasOutput;

    @JsonProperty("exit_code")
    Integer exitCode;

    @JsonProperty("logs")
    List<CircleCIStepActionLog> actionLogs;

    // timedout
    // continue
    // cancelled
}
