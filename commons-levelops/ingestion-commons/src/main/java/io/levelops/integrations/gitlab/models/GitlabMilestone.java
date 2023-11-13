package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;


@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabMilestone.GitlabMilestoneBuilder.class)
public class GitlabMilestone {
    @JsonProperty("id")
    String id;
    @JsonProperty("iid")
    String iids;
    @JsonProperty("project_id")
    String projectId;
    @JsonProperty("title")
    String title;
    @JsonProperty("description")
    String description;
    @JsonProperty("due_date")
    Date dueDate;
    @JsonProperty("start_date")
    Date startDate;
    @JsonProperty("state")
    String state;
    @JsonProperty("updated_at")
    Date updatedAt;
    @JsonProperty("created_at")
    Date createdAt;
    @JsonProperty("expired")
    boolean expired;
}
