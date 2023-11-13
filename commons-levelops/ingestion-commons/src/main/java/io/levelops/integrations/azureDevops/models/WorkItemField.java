package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkItemField.WorkItemFieldBuilder.class)
public class WorkItemField {

    @JsonProperty("id")
    Integer id;

    @JsonProperty("referenceName")
    String referenceName;

    @JsonProperty("name")
    String name;

    @JsonProperty("type")
    String type;

}
