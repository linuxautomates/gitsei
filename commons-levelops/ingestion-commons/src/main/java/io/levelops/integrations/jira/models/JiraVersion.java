package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraVersion.JiraVersionBuilder.class)
public class JiraVersion {

    @JsonProperty("self")
    String self;
    @JsonProperty("id")
    String id;
    @JsonProperty("description")
    String description;
    @JsonProperty("name")
    String name;
    @JsonProperty("archived")
    Boolean archived;
    @JsonProperty("released")
    Boolean released;
    @JsonProperty("overdue")
    Boolean overdue;
    @JsonProperty("startDate")
    String startDate;
    @JsonProperty("releaseDate")
    String releaseDate;
    @JsonProperty("userStartDate")
    String userStartDate;
    @JsonProperty("userReleaseDate")
    String userReleaseDate;
    @JsonProperty("projectId")
    String projectId;

}