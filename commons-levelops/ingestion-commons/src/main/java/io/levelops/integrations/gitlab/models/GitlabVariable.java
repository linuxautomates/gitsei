package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabVariable.GitlabVariableBuilder.class)
public class GitlabVariable {

    @JsonProperty("variable_type")
    String variableType;
    @JsonProperty("key")
    String key;
    @JsonProperty("value")
    String value;
}
