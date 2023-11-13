package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BuildChange.BuildChangeBuilder.class)
public class BuildChange {

    @JsonProperty("id")
    String id;

    @JsonProperty("message")
    String message;

    @JsonProperty("author")
    IdentityRef author;

    @JsonProperty("timestamp")
    String timestamp;

    @JsonProperty("type")
    String type;

    @JsonProperty("location")
    String location;

    @JsonProperty("pusher")
    String pusher;
}
