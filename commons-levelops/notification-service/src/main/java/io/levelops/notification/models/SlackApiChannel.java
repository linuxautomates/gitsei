package io.levelops.notification.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SlackApiChannel.SlackApiChannelBuilder.class)
public class SlackApiChannel {

    @JsonProperty("id")
    String id;
}
