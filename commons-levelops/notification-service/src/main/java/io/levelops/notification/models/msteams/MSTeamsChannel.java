package io.levelops.notification.models.msteams;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = MSTeamsChannel.MSTeamsChannelBuilder.class)
public class MSTeamsChannel {

    @JsonProperty("@odata.id")
    String odataId;

    @JsonProperty("id")
    String id;

    @JsonProperty("createdDateTime")
    String createdDateTime;

    @JsonProperty("displayName")
    String displayName;

    @JsonProperty("description")
    String description;

    @JsonProperty("membershipType")
    String membershipType;

    @JsonProperty("tenantId")
    String tenantId;

    public static JavaType getJavaType(ObjectMapper objectMapper) {
        return objectMapper.getTypeFactory().constructParametricType(MSTeamsApiResponse.class, MSTeamsChannel.class);
    }
}
