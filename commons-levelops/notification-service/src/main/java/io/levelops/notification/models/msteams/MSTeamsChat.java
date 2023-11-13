package io.levelops.notification.models.msteams;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = MSTeamsChat.MSTeamsChatBuilder.class)
public class MSTeamsChat {

    @JsonProperty("id")
    String id;

    @JsonProperty("chatType")
    String chatType;

    @JsonProperty("createdDateTime")
    String createdDateTime;

    @JsonProperty("lastUpdatedDateTime")
    String lastUpdatedDateTime;

    @JsonProperty("tenantId")
    String tenantId;

    @JsonProperty("webUrl")
    String webUrl;

    public static JavaType getJavaType(ObjectMapper objectMapper) {
        return objectMapper.getTypeFactory().constructParametricType(MSTeamsApiResponse.class, MSTeamsChat.class);
    }

}
