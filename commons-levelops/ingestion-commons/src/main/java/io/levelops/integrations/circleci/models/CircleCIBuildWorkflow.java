package io.levelops.integrations.circleci.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CircleCIBuildWorkflow.CircleCIBuildWorkflowBuilder.class)
public class CircleCIBuildWorkflow {

    @JsonProperty("job_name")
    String jobName;

    @JsonProperty("job_id")
    String jobId;

    @JsonProperty("workflow_id")
    String workflowId;

    @JsonProperty("workspace_id")
    String workspaceId;

    @JsonProperty("workflow_name")
    String workflowName;
}
