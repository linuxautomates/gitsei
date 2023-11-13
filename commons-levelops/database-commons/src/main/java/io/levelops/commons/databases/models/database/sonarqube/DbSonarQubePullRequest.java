package io.levelops.commons.databases.models.database.sonarqube;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.sonarqube.models.PullRequest;
import io.levelops.integrations.sonarqube.models.Status;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbSonarQubePullRequest.DbSonarQubePullRequestBuilder.class)
public class DbSonarQubePullRequest {

    @JsonProperty("id")
    String id;

    @JsonProperty("project_id")
    String projectId;

    @JsonProperty("key")
    String key;

    @JsonProperty("title")
    String title;

    @JsonProperty("branch")
    String branch;

    @JsonProperty("base_branch")
    String baseBranch;

    @JsonProperty("target_branch")
    String targetBranch;

    @JsonProperty("quality_gate_status")
    String qualityGateStatus;

    @JsonProperty("bugs")
    Long bugs;

    @JsonProperty("vulnerabilities")
    Long vulnerabilities;

    @JsonProperty("code_smells")
    Long codeSmells;

    @JsonProperty("analysis_date")
    Date analysisDate;

    @JsonProperty("url")
    String url;

    @JsonProperty("measures")
    List<DbSonarQubeMeasure> measures;

    public static DbSonarQubePullRequest fromPullRequest(PullRequest pullRequest, Date ingestedAt) {
        return DbSonarQubePullRequest.builder()
                .key(pullRequest.getKey())
                .title(pullRequest.getTitle())
                .branch(pullRequest.getBranch())
                .baseBranch(pullRequest.getBase())
                .targetBranch(pullRequest.getTarget())
                .measures(CollectionUtils.emptyIfNull(pullRequest.getMeasures()).stream()
                        .map(measure -> DbSonarQubeMeasure.fromMeasure(measure, ingestedAt))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .qualityGateStatus(Optional.ofNullable(pullRequest.getStatus())
                        .map(Status::getQualityGateStatus)
                        .orElse(null))
                .bugs(Optional.ofNullable(pullRequest.getStatus()).map(Status::getBugs).orElse(null))
                .vulnerabilities(Optional.ofNullable(pullRequest.getStatus())
                        .map(Status::getVulnerabilities)
                        .orElse(null))
                .codeSmells(Optional.ofNullable(pullRequest.getStatus()).map(Status::getCodeSmells).orElse(null))
                .analysisDate(pullRequest.getAnalysisDate())
                .url(pullRequest.getUrl())
                .build();
    }

}
