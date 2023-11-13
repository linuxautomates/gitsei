package io.levelops.integrations.snyk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SnykUser.SnykUserBuilder.class)
public class SnykUser {

    @JsonProperty("id")
    String id;

    @JsonProperty("username")
    String username;

    @JsonProperty("email")
    String email;

    @JsonProperty("orgs")
    List<SnykOrg> orgs;
}
