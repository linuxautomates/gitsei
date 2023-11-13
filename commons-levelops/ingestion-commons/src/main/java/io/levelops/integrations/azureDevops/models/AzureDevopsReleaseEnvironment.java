package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = AzureDevopsReleaseEnvironment.AzureDevopsReleaseEnvironmentBuilder.class)
public class AzureDevopsReleaseEnvironment {
    @JsonProperty("id")
    Long id;
    @JsonProperty("releaseId")
    Long releaseId;
    @JsonProperty("name")
    String name;
    @JsonProperty("status")
    String status;
    @JsonProperty("createdOn")
    String createdOn;
    @JsonProperty("modifiedOn")
    String modifiedOn;
    @JsonProperty("timeToDeploy")
    Double timeToDeploy;
    @JsonProperty("variables")
    Map<String, Configuration.Variable> variables;
    @JsonProperty("variableGroups")
    List<Configuration.VariableGroup> variableGroups;
    @JsonProperty("deploySteps")
    List<DeployStep> deploySteps;
    @JsonProperty("rank")
    Long rank;
    @JsonProperty("steps")
    List<AzureDevopsReleaseStep> steps;

    @Value
    @Builder
    @JsonDeserialize(builder = DeployStep.DeployStepBuilder.class)
    public static class DeployStep {
        @JsonProperty("releaseDeployPhases")
        List<ReleaseDeployPhase> releaseDeployPhases;

        @Value
        @Builder
        @JsonDeserialize(builder = DeployStep.ReleaseDeployPhase.ReleaseDeployPhaseBuilder.class)
        public static class ReleaseDeployPhase {
            @JsonProperty("deploymentJobs")
            List<DeploymentJob> deploymentJobs;

            @Value
            @Builder
            @JsonDeserialize(builder = ReleaseDeployPhase.DeploymentJob.DeploymentJobBuilder.class)
            public static class DeploymentJob {
                @JsonProperty("job")
                AzureDevopsReleaseStep job;
                @JsonProperty("tasks")
                List<AzureDevopsReleaseStep> tasks;
            }
        }
    }
}
