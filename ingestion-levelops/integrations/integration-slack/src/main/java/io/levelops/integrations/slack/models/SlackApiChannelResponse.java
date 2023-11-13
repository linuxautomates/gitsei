package io.levelops.integrations.slack.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SlackApiChannelResponse.SlackApiChannelResponseBuilder.class)
public class SlackApiChannelResponse {

    @JsonProperty("channel")
    SlackApiChannel channel;

    public static JavaType getJavaType(ObjectMapper objectMapper) {
        return objectMapper.getTypeFactory().constructParametricType(SlackApiResponse.class, SlackApiChannelResponse.class);
    }
}
