package io.levelops.commons.databases.models.database.azuredevops;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.dates.DateUtils;
import io.levelops.integrations.azureDevops.models.Build;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

import static io.levelops.commons.databases.models.database.azuredevops.DbAzureDevopsProject.camelCaseToSnakeCase;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbAzureDevopsBuild.DbAzureDevopsBuildBuilder.class)
public class DbAzureDevopsBuild {

    @JsonProperty("buildId")
    int buildId;

    @JsonProperty("id")
    String id;

    @JsonProperty("projectName")
    String projectName;

    @JsonProperty("buildNumber")
    String buildNumber;

    @JsonProperty("status")
    String status;

    @JsonProperty("result")
    String result;

    @JsonProperty("queueTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSZ")
    Date queueTime;

    @JsonProperty("startTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSZ")
    Date startTime;

    @JsonProperty("finishTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSZ")
    Date finishTime;

    @JsonProperty("buildNumberRevision")
    int buildNumberRevision;

    @JsonProperty("project_id")
    String projectId;

    @JsonProperty("sourceBranch")
    String sourceBranch;

    @JsonProperty("sourceVersion")
    String sourceVersion;

    @JsonProperty("priority")
    String priority;

    @JsonProperty("requestedFor")
    String requestedFor;

    @JsonProperty("requestedBy")
    String requestedBy;

    @JsonProperty("requestedByUniqueName")
    String requestedByUniqueName;

    @JsonProperty("cloud_id")
    String cloudId;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("lastChangedDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSZ")
    Date lastChangedDate;

    @JsonProperty("lastChangedBy")
    String lastChangedBy;

    @JsonProperty("repositoryId")
    String repositoryId;

    @JsonProperty("repositoryType")
    String repositoryType;

    @JsonProperty("repository_url")
    String repositoryUrl;

    @JsonProperty("pipeline_name")
    String pipelineName;


    @JsonProperty("ingested_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:SSZ")
    Date ingestedAt;

    public static DbAzureDevopsBuild fromBuild(Build build, String integrationId, Date ingestedAt) {
        return DbAzureDevopsBuild.builder()
                .buildId(build.getId())
                .integrationId(integrationId)
                .repositoryUrl(build.getRepository().getUrl() != null ? build.getRepository().getUrl() : "")
                .buildNumber(build.getBuildNumber())
                .status(camelCaseToSnakeCase(build.getStatus()))
                .result(camelCaseToSnakeCase(build.getResult()))
                .queueTime(DateUtils.toDate(DateUtils.parseDateTime(build.getQueueTime())))
                .startTime(DateUtils.toDate(DateUtils.parseDateTime(build.getStartTime())))
                .finishTime(DateUtils.toDate(DateUtils.parseDateTime(build.getFinishTime())))
                .buildNumberRevision(build.getBuildNumberRevision())
                .projectId(build.getProject().getId())
                .projectName(build.getProject().getName())
                .sourceBranch(build.getSourceBranch())
                .sourceVersion(build.getSourceVersion())
                .priority(camelCaseToSnakeCase(build.getPriority()))
                .cloudId(build.getRequestedBy().getUniqueName())
                .requestedBy(build.getRequestedBy().getDisplayName())
                .requestedByUniqueName(build.getRequestedBy().getUniqueName())
                .requestedFor(build.getRequestedFor().getDisplayName())
                .lastChangedDate(DateUtils.toDate(DateUtils.parseDateTime(build.getLastChangedDate())))
                .lastChangedBy(build.getLastChangedBy().getDisplayName())
                .repositoryId(build.getRepository().getId())
                .repositoryType(build.getRepository().getType())
                .pipelineName(build.getDefinition().getName())
                .ingestedAt(ingestedAt)
                .build();
    }
}
