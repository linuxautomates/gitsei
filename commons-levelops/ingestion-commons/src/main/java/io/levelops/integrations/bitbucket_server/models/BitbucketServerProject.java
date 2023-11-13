package io.levelops.integrations.bitbucket_server.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder =BitbucketServerProject.BitbucketServerProjectBuilder.class)
public class BitbucketServerProject {

    @JsonProperty("key")
    String key;

    @JsonProperty("id")
    Integer id;

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("public")
    Boolean isPublic;

    @JsonProperty("type")
    String type;

    @JsonProperty("links")
    BitbucketServerLink links;

}