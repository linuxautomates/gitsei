package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = IdentityRef.IdentityRefBuilder.class)
public class IdentityRef {

    @JsonProperty("_links")
    Link _links;

    @JsonProperty("descriptor")
    String descriptor;

    @JsonProperty("directoryAlias")
    String directoryAlias;

    @JsonProperty("displayName")
    String displayName;

    @JsonProperty("id")
    String id;

    @JsonProperty("imageUrl")
    String imageUrl;

    @JsonProperty("inactive")
    Boolean inactive;

    @JsonProperty("isAadIdentity")
    Boolean isAadIdentity;

    @JsonProperty("isContainer")
    Boolean isContainer;

    @JsonProperty("isDeletedInOrigin")
    Boolean isDeletedInOrigin;

    @JsonProperty("profileUrl")
    String profileUrl;

    @JsonProperty("uniqueName")
    String uniqueName;

    @JsonProperty("url")
    String url;
}
