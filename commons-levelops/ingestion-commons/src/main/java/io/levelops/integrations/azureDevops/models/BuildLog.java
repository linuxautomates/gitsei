package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BuildLog.BuildLogBuilder.class)
public class BuildLog {

    @JsonProperty("id")
    int id;

    @JsonProperty("type")
    String type;

    @JsonProperty("url")
    String url;
}
