package io.levelops.commons.databases.models.database.azuredevops;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.dates.DateUtils;
import io.levelops.integrations.azureDevops.models.AzureDevopsPipelineRunStageStep;
import io.levelops.integrations.azureDevops.models.Configuration;
import io.levelops.integrations.azureDevops.models.Pipeline;
import io.levelops.integrations.azureDevops.models.Run;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.MapUtils;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.models.database.azuredevops.DbAzureDevopsProject.camelCaseToSnakeCase;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbAzureDevopsPipelineRun.DbAzureDevopsPipelineRunBuilder.class)
public class DbAzureDevopsPipelineRun {

    @JsonProperty("id")
    String id;

    @JsonProperty("projectName")
    String projectName;

    @JsonProperty("pipeline_id")
    int pipelineId;

    @JsonProperty("revision")
    int revision;

    @JsonProperty("pipeline_name")
    String pipelineName;

    @JsonProperty("state")
    String state;

    @JsonProperty("result")
    String result;

    @JsonProperty("createdDate")
    Instant createdDate;

    @JsonProperty("finishedDate")
    Instant finishedDate;

    @JsonProperty("url")
    String url;

    @JsonProperty("runId")
    int runId;

    @JsonProperty("name")
    String name;

    @JsonProperty("scm_url")
    String scmUrl;

    @JsonProperty("commit_ids")
    List<String> commitIds;

    @JsonProperty("variables")
    Map<String, Configuration.Variable> variables;

    @JsonProperty("ingested_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:SSZ")
    Date ingestedAt;

    @JsonProperty("stages")
    List<AzureDevopsPipelineRunStageStep> stages;

    public static DbAzureDevopsPipelineRun fromRun(Pipeline pipeline, Run run, Date ingestedAt) {
        return DbAzureDevopsPipelineRun.builder()
                .pipelineId(run.getPipeline().getId())
                .pipelineName(run.getPipeline().getName())
                .revision(run.getPipeline().getRevision())
                .state(camelCaseToSnakeCase(run.getState()))
                .result(camelCaseToSnakeCase(run.getResult()))
                .createdDate(DateUtils.parseDateTime(run.getCreatedDate()))
                .finishedDate(DateUtils.parseDateTime(run.getFinishedDate()))
                .runId(run.getId())
                .name(run.getName())
                .scmUrl(pipeline.getConfiguration() != null ? pipeline.getConfiguration().getDesignerJson() != null ?
                        pipeline.getConfiguration().getDesignerJson().getRepository().getId() : null : null)
                .commitIds(run.getCommitIds())
                .variables(getVariables(pipeline, run))
                .ingestedAt(ingestedAt)
                .stages(run.getStages())
                .build();
    }

    private static Map<String, Configuration.Variable> getVariables(Pipeline pipeline, Run run) {
        Map<String, Configuration.Variable> variables = null;
        var config = pipeline.getConfiguration();
        if (MapUtils.isNotEmpty(run.getVariables())) {
            variables = run.getVariables();
        } else if (config != null) {
            if (config.getVariables() != null) {
                variables = config.getVariables();
            } else if (config.getDesignerJson() != null && config.getDesignerJson().getVariables() != null) {
                variables = config.getDesignerJson().getVariables();
            }
        }
        return variables;
    }
}
