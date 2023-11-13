package io.levelops.integrations.bitbucket_server.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder =BitbucketServerUser.BitbucketServerUserBuilder.class)
public class BitbucketServerUser {

    @JsonProperty("name")
    String name;

    @JsonProperty("emailAddress")
    String emailAddress;

    @JsonProperty("id")
    Integer id;

    @JsonProperty("displayName")
    String displayName;

    @JsonProperty("active")
    Boolean active;

    @JsonProperty("slug")
    String slug;

    @JsonProperty("type")
    String type;

}
