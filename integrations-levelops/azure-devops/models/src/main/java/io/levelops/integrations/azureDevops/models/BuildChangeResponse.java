package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BuildChangeResponse.BuildChangeResponseBuilder.class)
public class BuildChangeResponse {

    @JsonProperty("value")
    List<BuildChange> buildChanges;

}