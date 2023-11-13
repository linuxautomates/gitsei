package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CxSastProject.CxSastProjectBuilder.class)
public class CxSastProject {

    @JsonProperty("id")
    String id;

    @JsonProperty("teamId")
    String teamId;

    @JsonProperty("name")
    String name;

    @JsonProperty("isPublic")
    boolean isPublic;

    @JsonProperty("sourceSettingsLink")
    SourceSettingsLink sourceSettingsLink;

    @JsonProperty("link")
    Link link;

    @JsonProperty("links")
    List<Link> links;

    @JsonProperty("vcs_settings")
    VCSSettings vcsSettings;
}
