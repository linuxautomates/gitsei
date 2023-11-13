package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraSprint.JiraSprintBuilder.class)
public class JiraSprint {

    @JsonProperty("id")
    String id;
    @JsonProperty("self")
    String self;
    @JsonProperty("state")
    String state;
    @JsonProperty("name")
    String name;
    @JsonProperty("startDate")
    String startDate;
    @JsonProperty("endDate")
    String endDate;
    @JsonProperty("completeDate")
    String completeDate;
    @JsonProperty("originBoardId")
    String originBoardId;
    @JsonProperty("goal")
    String goal;

}