package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubOrganization.GithubOrganizationBuilder.class)
public class GithubOrganization {
    
    @JsonProperty("login")
    String login;
    
    @JsonProperty("id")
    Long id;
    
    @JsonProperty("node_id")
    String nodeId;
    
    @JsonProperty("url")
    String url;
    
    @JsonProperty("repos_url")
    String reposUrl;
    
    @JsonProperty("events_url")
    String eventsUrl;
    
    @JsonProperty("hooks_url")
    String hooksUrl;
    
    @JsonProperty("issues_url")
    String issuesUrl;
    
    @JsonProperty("members_url")
    String membersUrl;
    
    @JsonProperty("public_members_url")
    String publicMembersUrl;
    
    @JsonProperty("avatar_url")
    String avatarUrl;
    
    @JsonProperty("description")
    String description;
}
