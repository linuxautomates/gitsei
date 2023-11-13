package io.levelops.notification.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SlackChatPostMessageResponse.SlackChatPostMessageResponseBuilder.class)
public class SlackChatPostMessageResponse {

    @JsonProperty("channel")
    String channel;
    @JsonProperty("ts")
    String ts;

    public static JavaType getJavaType(ObjectMapper objectMapper) {
        return objectMapper.getTypeFactory().constructParametricType(SlackApiResponse.class, SlackChatPostMessageResponse.class);
    }
}
