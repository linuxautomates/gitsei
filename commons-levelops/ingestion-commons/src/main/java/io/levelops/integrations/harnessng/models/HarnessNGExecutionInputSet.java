package io.levelops.integrations.harnessng.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HarnessNGExecutionInputSet.HarnessNGExecutionInputSetBuilder.class)
public class HarnessNGExecutionInputSet {
    @JsonProperty("inputSetTemplateYaml")
    String inputSetTemplateYaml;
    @JsonProperty("inputSetYaml")
    String inputSetYaml;
}
