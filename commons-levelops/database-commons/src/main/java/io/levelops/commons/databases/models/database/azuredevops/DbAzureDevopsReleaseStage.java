package io.levelops.commons.databases.models.database.azuredevops;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.dates.DateUtils;
import io.levelops.integrations.azureDevops.models.AzureDevopsReleaseEnvironment;
import io.levelops.integrations.azureDevops.models.AzureDevopsReleaseStep;
import io.levelops.integrations.azureDevops.models.Configuration;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbAzureDevopsReleaseStage.DbAzureDevopsReleaseStageBuilder.class)
public class DbAzureDevopsReleaseStage {
    @JsonProperty("id")
    String id;
    @JsonProperty("stageId")
    Long stageId;
    @JsonProperty("releaseId")
    Long releaseId;
    @JsonProperty("name")
    String name;
    @JsonProperty("status")
    String status;
    @JsonProperty("createdOn")
    Instant createdOn;
    @JsonProperty("modifiedOn")
    Instant modifiedOn;
    @JsonProperty("timeToDeploy")
    Double timeToDeploy;
    @JsonProperty("variables")
    Map<String, Configuration.Variable> variables;
    @JsonProperty("steps")
    List<AzureDevopsReleaseStep> steps;

    public static DbAzureDevopsReleaseStage fromEnvironment(AzureDevopsReleaseEnvironment environment){
        return DbAzureDevopsReleaseStage.builder()
                .stageId(environment.getId())
                .name(environment.getName())
                .createdOn(DateUtils.parseDateTime(environment.getCreatedOn()))
                .modifiedOn(DateUtils.parseDateTime(environment.getModifiedOn()))
                .status(environment.getStatus())
                .timeToDeploy(environment.getTimeToDeploy())
                .steps(environment.getSteps())
                .variables(getVariables(environment))
                .build();
    }

    public static Map<String, Configuration.Variable> getVariables(AzureDevopsReleaseEnvironment environment) {
        Map<String, Configuration.Variable> variables = new HashMap<>();
        ListUtils.emptyIfNull(environment.getVariableGroups()).stream()
                .map(Configuration.VariableGroup::getVariables)
                .filter(MapUtils::isNotEmpty)
                .forEach(variables::putAll);
        if (MapUtils.isNotEmpty(environment.getVariables())) {
            variables.putAll(environment.getVariables());
        }
        return variables;
    }
}
