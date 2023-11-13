package io.levelops.commons.databases.issue_management;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbIssueMgmtSprintMapping.DbIssueMgmtSprintMappingBuilder.class)
public class DbIssueMgmtSprintMapping {

    @JsonProperty("id")
    String id;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("workitem_id")
    String workitemId;

    @JsonProperty("sprint_id")
    String sprintId;

    @JsonProperty("added_at")
    Long addedAt;

    @JsonProperty("removed_at")
    Long removedAt;

    @JsonProperty("planned")
    Boolean planned;

    @JsonProperty("delivered")
    Boolean delivered;

    @JsonProperty("outside_of_sprint")
    Boolean outsideOfSprint;

    @JsonProperty("ignorable_workitem_type")
    Boolean ignorableWorkitemType;

    @JsonProperty("story_points_planned")
    Float storyPointsPlanned;

    @JsonProperty("story_points_delivered")
    Float storyPointsDelivered;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    Instant createdAt;
}
