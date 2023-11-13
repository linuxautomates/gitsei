package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Group.GroupBuilder.class)
public class Group {

    @JsonProperty("subjectKind")
    String subjectKind;

    @JsonProperty("description")
    String description;

    @JsonProperty("domain")
    String domain;

    @JsonProperty("principalName")
    String principalName;

    @JsonProperty("mailAddress")
    String mailAddress;

    @JsonProperty("origin")
    String origin;

    @JsonProperty("originId")
    String originId;

    @JsonProperty("displayName")
    String displayName;

    @JsonProperty("_links")
    Link link;

    @JsonProperty("url")
    String url;

    @JsonProperty("descriptor")
    String descriptor;
}
