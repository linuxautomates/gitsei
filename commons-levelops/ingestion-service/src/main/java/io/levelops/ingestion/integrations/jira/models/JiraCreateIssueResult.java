package io.levelops.ingestion.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraCreateIssueResult.JiraCreateIssueResultBuilder.class)
public class JiraCreateIssueResult implements ControllerIngestionResult {
    @JsonProperty("id")
    String id;
    @JsonProperty("key")
    String key;
}