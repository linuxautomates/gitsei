package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

/**
 * WARNING: also used by the client (no normalization)
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubIssue.GithubIssueBuilder.class)
public class GithubIssue {
    @JsonProperty("id")
    String id;
    @JsonProperty("node_id")
    String nodeId;
    @JsonProperty("number")
    Long number;
    @JsonProperty("html_url")
    String htmlUrl;
    @JsonProperty("title")
    String title;
    @JsonProperty("user")
    GithubUser user;
    @JsonProperty("labels")
    List<Label> labels;
    @JsonProperty("state")
    String state;
    @JsonProperty("locked")
    Boolean locked;
    @JsonProperty("active_lock_reason")
    String activeLockReason;
    @JsonProperty("assignee")
    GithubUser assignee; // returns last assignee (vestige of v2)
    @JsonProperty("assignees")
    List<GithubUser> assignees;
    @JsonProperty("comments")
    Integer comments;
    @JsonProperty("created_at")
    Date createdAt;
    @JsonProperty("updated_at")
    Date updatedAt;
    @JsonProperty("closed_at")
    Date closedAt;
    @JsonProperty("author_association")
    String authorAssociation;
    @JsonProperty("body")
    String body;
    @JsonProperty("milestone")
    Milestone milestone;

    @JsonProperty("events")
    List<GithubIssueEvent> events; // enriched

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Milestone.MilestoneBuilder.class)
    public static class Milestone {
        @JsonProperty("id")
        String id;
        @JsonProperty("number")
        Long number;
        @JsonProperty("title")
        String title;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Label.LabelBuilder.class)
    public static class Label {
        @JsonProperty("id")
        String id;
        @JsonProperty("node_id")
        String nodeId;
        @JsonProperty("url")
        String url;
        @JsonProperty("name")
        String name;
        @JsonProperty("color")
        String color;
        @JsonProperty("description")
        String description;
        @JsonProperty("default")
        Boolean isDefault;
    }

}

