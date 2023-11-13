package io.levelops.notification.models.msteams;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = MSTeamsUser.MSTeamsUserBuilder.class)
public class MSTeamsUser {

    @JsonProperty("id")
    String id;

    @JsonProperty("displayName")
    String displayName;

    @JsonProperty("mail")
    String mail;

    @JsonProperty("userPrincipalName")
    String userPrincipalName;

    @JsonProperty("givenName")
    String givenName;

    @JsonProperty("surname")
    String surname;

    public static JavaType getJavaType(ObjectMapper objectMapper) {
        return objectMapper.getTypeFactory().constructParametricType(MSTeamsApiResponse.class, MSTeamsUser.class);
    }

}
