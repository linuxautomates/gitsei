package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraCommentsResult.JiraCommentsResultBuilder.class)
public class JiraCommentsResult {

    @JsonProperty("comments")
    List<JiraComment> comments;

}
