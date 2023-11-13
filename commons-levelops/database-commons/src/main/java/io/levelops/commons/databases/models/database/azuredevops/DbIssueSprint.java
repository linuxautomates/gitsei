package io.levelops.commons.databases.models.database.azuredevops;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.sql.Timestamp;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbIssueSprint.DbIssueSprintBuilder.class)
public class DbIssueSprint {

    @JsonProperty("id")
    UUID id;

    @JsonProperty("sprint_id")
    String sprintId;

    @JsonProperty("parent_sprint")
    String parentSprint;

    @JsonProperty("name")
    String name;

    @JsonProperty("integration_id")
    Integer integrationId;

    @JsonProperty("project_id")
    String projectId;

    @JsonProperty("state")
    String state;

    @JsonProperty("start_date")
    Timestamp startDate;

    @JsonProperty("end_date")
    Timestamp endDate;

    @JsonProperty("completed_date")
    Timestamp completedDate;
}
