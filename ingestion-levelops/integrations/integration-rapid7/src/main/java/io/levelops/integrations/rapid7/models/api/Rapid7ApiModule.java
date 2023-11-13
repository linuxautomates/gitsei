package io.levelops.integrations.rapid7.models.api;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Rapid7ApiModule.Rapid7ApiModuleBuilder.class)
public class Rapid7ApiModule {

    @JsonProperty("id")
    String id;
    @JsonProperty("name")
    String name;
    @JsonProperty("description")
    String description;

}
