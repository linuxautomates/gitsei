package io.levelops.commons.databases.models.database.bullseye;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize
public class DbBullseyeBuild {

    @JsonProperty("id")
    String id;

    @JsonProperty("cicd_job_run_id")
    String cicdJobRunId;

    @JsonProperty("cicd_job_name")
    String cicdJobName;

    @JsonProperty("cicd_job_full_name")
    String cicdJobFullName;

    @JsonProperty("cicd_job_normalized_full_name")
    String cicdJobNormalizedFullName;

    @JsonProperty("build_id")
    String buildId;

    @JsonProperty("project")
    String jobName;

    @JsonProperty("built_at")
    Date builtAt;

    @JsonProperty("name")
    String name;

    @JsonProperty("file_hash")
    String fileHash;

    @JsonProperty("directory")
    String directory;

    @JsonProperty("functions_covered")
    Integer functionsCovered;

    @JsonProperty("total_functions")
    Integer totalFunctions;

    @JsonProperty("functions_uncovered")
    Integer functionsUncovered;

    @JsonProperty("functions_percentage_coverage")
    Double functionsPercentageCoverage;

    @JsonProperty("decisions_covered")
    Integer decisionsCovered;

    @JsonProperty("total_decisions")
    Integer totalDecisions;

    @JsonProperty("decisions_uncovered")
    Integer decisionsUncovered;

    @JsonProperty("decisions_percentage_coverage")
    Double decisionsPercentageCoverage;

    @JsonProperty("conditions_covered")
    Integer conditionsCovered;

    @JsonProperty("total_conditions")
    Integer totalConditions;

    @JsonProperty("conditions_uncovered")
    Integer conditionsUncovered;

    @JsonProperty("conditions_percentage_coverage")
    Double conditionsPercentageCoverage;
}
