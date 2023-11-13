package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubProject.GithubProjectBuilder.class)
public class GithubProject {

    @JsonProperty("owner_url")
    String ownerUrl;

    @JsonProperty("url")
    String url;

    @JsonProperty("html_url")
    String htmlUrl;

    @JsonProperty("columns_url")
    String columnsUrl;

    @JsonProperty("id")
    String id;

    @JsonProperty("node_id")
    String nodeId;

    @JsonProperty("name")
    String name;

    @JsonProperty("body")
    String body;

    @JsonProperty("number")
    Long number;

    @JsonProperty("state")
    String state;

    @JsonProperty("organization_permission")
    String organizationPermission;

    @JsonProperty("private")
    Boolean isPrivate;

    @JsonProperty("creator")
    GithubCreator creator;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("updated_at")
    Date updatedAt;

    @JsonProperty("columns")
    List<GithubProjectColumn> columns;

}
