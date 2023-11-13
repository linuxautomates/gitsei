package io.levelops.integrations.slack.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SlackApiChatMessagePermalinkResponse.SlackApiChatMessagePermalinkResponseBuilder.class)
public class SlackApiChatMessagePermalinkResponse {
    @JsonProperty("permalink")
    String permalink;

    @JsonProperty("channel")
    String channel;

    public static JavaType getJavaType(ObjectMapper objectMapper) {
        return objectMapper.getTypeFactory().constructParametricType(SlackApiResponse.class, SlackApiChatMessagePermalinkResponse.class);
    }
}
