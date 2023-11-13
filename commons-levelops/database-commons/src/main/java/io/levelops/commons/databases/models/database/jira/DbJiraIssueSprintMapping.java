package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbJiraIssueSprintMapping.DbJiraIssueSprintMappingBuilder.class)
public class DbJiraIssueSprintMapping {


    @JsonProperty("id")
    String id;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("issue_key")
    String issueKey;

    @JsonProperty("sprint_id")
    String sprintId;

    @JsonProperty("added_at")
    Long addedAt;

    @JsonProperty("planned")
    Boolean planned;

    @JsonProperty("delivered")
    Boolean delivered;

    @JsonProperty("outside_of_sprint")
    Boolean outsideOfSprint;

    @JsonProperty("ignorable_issue_type")
    Boolean ignorableIssueType;

    @JsonProperty("story_points_planned")
    Integer storyPointsPlanned;

    @JsonProperty("story_points_delivered")
    Integer storyPointsDelivered;

    @JsonProperty("removed_mid_sprint")
    Boolean removedMidSprint;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    Instant createdAt;
}
