package io.levelops.integrations.bitbucket.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketWorkspace.BitbucketWorkspaceBuilder.class)
public class BitbucketWorkspace {

    @JsonProperty("uuid")
    String uuid;
    @JsonProperty("name")
    String name;
    @JsonProperty("slug")
    String slug;
    @JsonProperty("is_private")
    Boolean isPrivate;
    @JsonProperty("created_on")
    Date createdOn;
    @JsonProperty("updated_on")
    Date updatedOn;
}
