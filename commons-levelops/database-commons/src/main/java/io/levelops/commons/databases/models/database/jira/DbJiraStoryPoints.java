package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbJiraStoryPoints.DbJiraStoryPointsBuilder.class)
public class DbJiraStoryPoints {

    @JsonProperty("id")
    String id;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("issue_key")
    String issueKey;

    @JsonProperty("story_points")
    Integer storyPoints;

    @JsonProperty("start_time")
    Long startTime;

    @JsonProperty("end_time")
    Long endTime;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    Instant createdAt;

}
