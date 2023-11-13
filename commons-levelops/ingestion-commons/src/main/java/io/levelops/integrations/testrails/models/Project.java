package io.levelops.integrations.testrails.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

/**
 * Bean definition for testrails project
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Project.ProjectBuilder.class)
public class Project {

    @JsonProperty("id")
    Integer id;

    @JsonProperty("name")
    String name;

    @JsonProperty("suite_mode")
    Integer suiteMode;

    @JsonProperty("announcement")
    String announcement;

    @JsonProperty("show_announcement")
    Boolean showAnnouncement;

    @JsonProperty("url")
    String url;

    @JsonProperty("is_completed")
    Boolean isCompleted;

    @JsonFormat(shape=JsonFormat.Shape.NUMBER, pattern="s")
    @JsonProperty("completed_on")
    Date completedOn;

    @JsonProperty("milestones_list")
    List<Milestone> milestones;
}
