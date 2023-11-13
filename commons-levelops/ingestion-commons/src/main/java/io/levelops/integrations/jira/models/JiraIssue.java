package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraIssue.JiraIssueBuilder.class)
public class JiraIssue {

    @JsonProperty("expand")
    String expand;

    @JsonProperty("id")
    String id;

    @JsonProperty("self")
    String self;

    @JsonProperty("key")
    String key;

    @JsonProperty("fields")
    JiraIssueFields fields;

    @JsonProperty("changelog")
    JiraIssueChangeLog changeLog;

    // derived field - extraction from fields and changeLog
    @JsonProperty("firstCommentAt")
    Long firstCommentAt;
}
