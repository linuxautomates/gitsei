package io.levelops.commons.databases.models.database.awsdevtools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.awsdevtools.models.CBBuild;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbAWSDevToolsBuild.DbAWSDevToolsBuildBuilder.class)
public class DbAWSDevToolsBuild {

    @JsonProperty("id")
    String id;

    @JsonProperty("build_id")
    String buildId;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("arn")
    String arn;

    @JsonProperty("build_number")
    Long buildNumber;

    @JsonProperty("build_started_at")
    Date buildStartedAt;

    @JsonProperty("build_ended_at")
    Date buildEndedAt;

    @JsonProperty("last_phase")
    String lastPhase;

    @JsonProperty("last_phase_status")
    String lastPhaseStatus;

    @JsonProperty("status")
    String status;

    @JsonProperty("build_complete")
    String buildComplete;

    @JsonProperty("project_name")
    String projectName;

    @JsonProperty("project_arn")
    String projectArn;

    @JsonProperty("initiator")
    String initiator;

    @JsonProperty("build_batch_arn")
    String buildBatchArn;

    @JsonProperty("source_type")
    String sourceType;

    @JsonProperty("source_location")
    String sourceLocation;

    @JsonProperty("resolved_source_version")
    String resolvedSourceVersion;

    @JsonProperty("region")
    String region;

    @JsonProperty("reports")
    List<DbAWSDevToolsReport> reports;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("updated_at")
    Date updatedAt;

    public static DbAWSDevToolsBuild fromBuild(CBBuild build, String integrationId) {
        String buildCompleted = (Boolean.TRUE.equals(build.getBuild().getBuildComplete())) ? "COMPLETED" : "INCOMPLETE";
        String lastPhaseStatus = Objects.requireNonNull(build.getBuild().getPhases().stream().filter(buildPhase ->
                buildPhase.getPhaseType().equalsIgnoreCase(build.getBuild().getCurrentPhase())).findFirst()
                .orElse(null)).getPhaseStatus();
        if (lastPhaseStatus == null)
            lastPhaseStatus = "_UNKNOWN_";
        String buildBatchArn = build.getBuild().getBuildBatchArn();
        if (buildBatchArn == null)
            buildBatchArn = "_UNKNOWN_";
        Date date = new Date();
        return DbAWSDevToolsBuild.builder()
                .integrationId(integrationId)
                .buildId(build.getBuild().getId())
                .arn(build.getBuild().getArn())
                .buildNumber(build.getBuild().getBuildNumber())
                .buildStartedAt(build.getBuild().getStartTime())
                .buildEndedAt(build.getBuild().getEndTime())
                .lastPhase(build.getBuild().getCurrentPhase())
                .lastPhaseStatus(lastPhaseStatus)
                .status(build.getBuild().getBuildStatus())
                .buildComplete(buildCompleted)
                .projectName(build.getBuild().getProjectName())
                .projectArn(build.getProjectArn())
                .initiator(build.getBuild().getInitiator())
                .buildBatchArn(buildBatchArn)
                .sourceType(build.getBuild().getSource().getType())
                .sourceLocation(build.getBuild().getSource().getLocation())
                .resolvedSourceVersion(build.getBuild().getResolvedSourceVersion())
                .region(build.getRegion())
                .reports(CollectionUtils.emptyIfNull(build.getReports()).stream()
                        .map(report -> DbAWSDevToolsReport.fromReport(report, integrationId))
                        .collect(Collectors.toList()))
                .createdAt(date)
                .updatedAt(date)
                .build();
    }
}
