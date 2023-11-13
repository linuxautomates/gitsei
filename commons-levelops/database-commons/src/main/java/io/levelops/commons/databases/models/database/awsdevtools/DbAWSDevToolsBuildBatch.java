package io.levelops.commons.databases.models.database.awsdevtools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.awsdevtools.models.CBBuildBatch;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.Objects;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbAWSDevToolsBuildBatch.DbAWSDevToolsBuildBatchBuilder.class)
public class DbAWSDevToolsBuildBatch {

    @JsonProperty("id")
    String id;

    @JsonProperty("build_batch_id")
    String buildBatchId;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("arn")
    String arn;

    @JsonProperty("build_batch_number")
    Long buildBatchNumber;

    @JsonProperty("build_batch_started_at")
    Date buildBatchStartedAt;

    @JsonProperty("build_batch_ended_at")
    Date buildBatchEndedAt;

    @JsonProperty("build_batch_complete")
    String buildBatchComplete;

    @JsonProperty("last_phase")
    String lastPhase;

    @JsonProperty("last_phase_status")
    String lastPhaseStatus;

    @JsonProperty("status")
    String status;

    @JsonProperty("source_version")
    String sourceVersion;

    @JsonProperty("resolved_source_version")
    String resolvedSourceVersion;

    @JsonProperty("project_name")
    String projectName;

    @JsonProperty("project_arn")
    String projectArn;

    @JsonProperty("initiator")
    String initiator;

    @JsonProperty("source_type")
    String sourceType;

    @JsonProperty("source_location")
    String sourceLocation;

    @JsonProperty("region")
    String region;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("updated_at")
    Date updatedAt;

    public static DbAWSDevToolsBuildBatch fromBuildBatch(CBBuildBatch buildBatch, String integrationId) {
        String buildBatchCompleted = (Boolean.TRUE.equals(buildBatch.getBuildBatch().getComplete())) ? "COMPLETED" : "INCOMPLETE";
        String lastPhaseStatus = Objects.requireNonNull(buildBatch.getBuildBatch().getPhases().stream().filter(buildBatchPhase ->
                buildBatchPhase.getPhaseType().equalsIgnoreCase(buildBatch.getBuildBatch().getCurrentPhase()))
                .findFirst().orElse(null)).getPhaseStatus();
        if (lastPhaseStatus == null)
            lastPhaseStatus = "_UNKNOWN_";
        Date date = new Date();
        return DbAWSDevToolsBuildBatch.builder()
                .integrationId(integrationId)
                .buildBatchId(buildBatch.getBuildBatch().getId())
                .arn(buildBatch.getBuildBatch().getArn())
                .buildBatchNumber(buildBatch.getBuildBatch().getBuildBatchNumber())
                .buildBatchStartedAt(buildBatch.getBuildBatch().getStartTime())
                .buildBatchEndedAt(buildBatch.getBuildBatch().getEndTime())
                .buildBatchComplete(buildBatchCompleted)
                .lastPhase(buildBatch.getBuildBatch().getCurrentPhase())
                .lastPhaseStatus(lastPhaseStatus)
                .status(buildBatch.getBuildBatch().getBuildBatchStatus())
                .sourceVersion(buildBatch.getBuildBatch().getSourceVersion())
                .resolvedSourceVersion(buildBatch.getBuildBatch().getResolvedSourceVersion())
                .projectName(buildBatch.getBuildBatch().getProjectName())
                .projectArn(buildBatch.getProjectArn())
                .initiator(buildBatch.getBuildBatch().getInitiator())
                .sourceType(buildBatch.getBuildBatch().getSource().getType())
                .sourceLocation(buildBatch.getBuildBatch().getSource().getLocation())
                .region(buildBatch.getRegion())
                .createdAt(date)
                .updatedAt(date)
                .build();
    }
}
