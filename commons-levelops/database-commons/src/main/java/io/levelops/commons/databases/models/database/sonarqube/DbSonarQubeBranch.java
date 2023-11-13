package io.levelops.commons.databases.models.database.sonarqube;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.sonarqube.models.Branch;
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
@JsonDeserialize(builder = DbSonarQubeBranch.DbSonarQubeBranchBuilder.class)
public class DbSonarQubeBranch {

    @JsonProperty("id")
    String id;

    @JsonProperty("project_id")
    String projectId;

    @JsonProperty("name")
    String name;

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

    @JsonProperty("measures")
    List<DbSonarQubeMeasure> measures;

    public static DbSonarQubeBranch fromBranch(Branch branch, Date ingestedAt) {
        return DbSonarQubeBranch.builder()
                .name(branch.getName())
                .qualityGateStatus(Optional.ofNullable(branch.getStatus()).map(Status::getQualityGateStatus).orElse(null))
                .bugs(Optional.ofNullable(branch.getStatus()).map(Status::getBugs).orElse(null))
                .vulnerabilities(Optional.ofNullable(branch.getStatus()).map(Status::getVulnerabilities).orElse(null))
                .codeSmells(Optional.ofNullable(branch.getStatus()).map(Status::getCodeSmells).orElse(null))
                .analysisDate(branch.getAnalysisDate())
                .measures(CollectionUtils.emptyIfNull(branch.getMeasures()).stream()
                        .map(measure -> DbSonarQubeMeasure.fromMeasure(measure, ingestedAt))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .build();
    }
}
