package io.levelops.integrations.github.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubEvent.GithubEventBuilder.class)
public class GithubEvent implements Serializable {

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("is_public")
    private boolean isPublic;

    @JsonProperty("actor")
    private String actor;

    @JsonProperty("org")
    private String org;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("commits")
    private List<GithubCommit> commits; // enriched
}
