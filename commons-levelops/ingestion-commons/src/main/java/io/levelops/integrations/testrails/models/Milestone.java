package io.levelops.integrations.testrails.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

/**
 * Bean definition for testrails milestone
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Milestone.MilestoneBuilder.class)
public class Milestone {

    @JsonProperty("id")
    Integer id;

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("project_id")
    Integer projectId;

    @JsonProperty("parent_id")
    Integer parentId;

    @JsonFormat(shape=JsonFormat.Shape.NUMBER, pattern="s")
    @JsonProperty("start_on")
    Date startOn;

    @JsonProperty("url")
    String url;

    @JsonProperty("is_started")
    Boolean isStarted;

    @JsonFormat(shape=JsonFormat.Shape.NUMBER, pattern="s")
    @JsonProperty("started_on")
    Date startedOn;

    @JsonFormat(shape=JsonFormat.Shape.NUMBER, pattern="s")
    @JsonProperty("due_on")
    Date dueOn;

    @JsonProperty("is_completed")
    Boolean isCompleted;

    @JsonFormat(shape=JsonFormat.Shape.NUMBER, pattern="s")
    @JsonProperty("completed_on")
    Date completedOn;

    @JsonProperty("refs")
    String refs;

    @JsonProperty("milestones")
    List<Milestone> milestones;
}
