package io.levelops.integrations.harnessng.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HarnessNGPipelineStepLogs.HarnessNGPipelineStepLogsBuilder.class)
public class HarnessNGPipelineStepLogs {

    @JsonProperty("message")
    String message;

    @JsonProperty("failureTypeList")
    List<String> failureTypeList;

}
