package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Profile.ProfileBuilder.class)
public class Profile {

    @JsonProperty("id")
    String id;

    @JsonProperty("displayName")
    String displayName;

    @JsonProperty("emailAddress")
    String emailAddress;

}
