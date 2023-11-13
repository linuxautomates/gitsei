package io.levelops.commons.databases.models.database.bullseye;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize
public class BullseyeSourceFile {

    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("modification_time")
    Date modificationTime;

    @JsonProperty("total_functions")
    Integer totalFunctions;

    @JsonProperty("functions_covered")
    Integer functionsCovered;

    @JsonProperty("functions_uncovered")
    Integer functionsUncovered;

    @JsonProperty("functions_percentage_coverage")
    Double functionsPercentageCoverage;

    @JsonProperty("total_decisions")
    Integer totalDecisions;

    @JsonProperty("decisions_covered")
    Integer decisionsCovered;

    @JsonProperty("decisions_uncovered")
    Integer decisionsUncovered;

    @JsonProperty("decisions_percentage_coverage")
    Double decisionsPercentageCoverage;

    @JsonProperty("total_conditions")
    Integer totalConditions;

    @JsonProperty("conditions_covered")
    Integer conditionsCovered;

    @JsonProperty("conditions_uncovered")
    Integer conditionsUncovered;

    @JsonProperty("conditions_percentage_coverage")
    Double conditionsPercentageCoverage;
}
