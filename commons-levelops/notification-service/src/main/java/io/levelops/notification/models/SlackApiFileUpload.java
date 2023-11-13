package io.levelops.notification.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SlackApiFileUpload.SlackApiFileUploadBuilder.class)
public class SlackApiFileUpload {

    @JsonProperty("id")
    String id;
}
