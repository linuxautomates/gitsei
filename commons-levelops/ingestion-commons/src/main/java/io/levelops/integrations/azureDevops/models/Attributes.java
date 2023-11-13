package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Attributes.AttributesBuilder.class)
public class Attributes {

    @JsonProperty("isLocked")
    Boolean isLocked;

    @JsonProperty("name")
    String name;
}
