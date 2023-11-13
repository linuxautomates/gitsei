package io.levelops.notification.models.msteams;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = MSTeamsChatMessage.MSTeamsChatMessageBuilder.class)
public class MSTeamsChatMessage {

    @JsonProperty("id")
    String id;

    @JsonProperty("body")
    MSTeamsChatMessageBody body;

    @JsonProperty("chatId")
    String chatId;

    @JsonProperty("messageType")
    String messageType;

    @JsonProperty("createdDateTime")
    String createdDateTime;

    @JsonProperty("lastModifiedDateTime")
    String lastModifiedDateTime;

    @JsonProperty("channelIdentity")
    MSTeamsChannelIdentity channelIdentity;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = MSTeamsChannelIdentity.MSTeamsChannelIdentityBuilder.class)
    public static class MSTeamsChannelIdentity {

        @JsonProperty("teamId")
        String teamId;

        @JsonProperty("channelId")
        String channelId;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = MSTeamsChatMessageBody.MSTeamsChatMessageBodyBuilder.class)
    public static class MSTeamsChatMessageBody {

        @JsonProperty("content")
        String content;

        @Builder.Default
        @JsonProperty("contentType")
        String contentType = "html";
    }

    public static JavaType getJavaType(ObjectMapper objectMapper) {
        return objectMapper.getTypeFactory().constructParametricType(MSTeamsApiResponse.class, MSTeamsChatMessage.class);
    }
}
