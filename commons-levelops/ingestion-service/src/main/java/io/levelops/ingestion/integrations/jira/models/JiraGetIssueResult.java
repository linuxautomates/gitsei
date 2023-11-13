package io.levelops.ingestion.integrations.jira.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.integrations.jira.models.JiraIssue;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraGetIssueResult.JiraGetIssueResultBuilder.class)
public class JiraGetIssueResult implements ControllerIngestionResult {

    @JsonProperty("issue")
    JiraIssue issue;

}
