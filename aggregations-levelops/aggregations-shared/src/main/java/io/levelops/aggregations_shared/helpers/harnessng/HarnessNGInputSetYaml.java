package io.levelops.aggregations_shared.helpers.harnessng;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HarnessNGInputSetYaml.HarnessNGInputSetYamlBuilder.class)
public class HarnessNGInputSetYaml {

    @JsonProperty("pipeline")
    Pipeline pipeline;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Pipeline.PipelineBuilder.class)
    public static class Pipeline {

        @JsonProperty("variables")
        List<Variable> variables;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = Variable.VariableBuilder.class)
        public static class Variable {
            @JsonProperty("name")
            String name;
            @JsonProperty("type")
            String type;
            @JsonProperty("value")
            Object value;
        }
    }
}
