package io.levelops.notification.models.msteams;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = MSTeamsTeamRequest.MSTeamsTeamRequestBuilder.class)
public class MSTeamsTeamRequest {
    @JsonProperty("@odata.type")
    String dataType;

    @JsonProperty("roles")
    List<String> roles;

    @JsonProperty("user@odata.bind")
    String userDataBind;
}
