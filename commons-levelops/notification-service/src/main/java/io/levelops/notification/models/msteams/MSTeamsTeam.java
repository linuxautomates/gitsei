package io.levelops.notification.models.msteams;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = MSTeamsTeam.MSTeamsTeamBuilder.class)
public class MSTeamsTeam {

    @JsonProperty("id")
    String id;

    @JsonProperty("displayName")
    String displayName;

    @JsonProperty("description")
    String description;

    public static JavaType getJavaType(ObjectMapper objectMapper) {
        return objectMapper.getTypeFactory().constructParametricType(MSTeamsApiResponse.class, MSTeamsTeam.class);
    }

}
