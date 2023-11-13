package io.levelops.commons.databases.models.database.bullseye;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize
public class BullseyeAggregationResult {

    @JsonProperty("functions_covered")
    Integer functionsCovered;

    @JsonProperty("functions_uncovered")
    Integer functionsUncovered;

    @JsonProperty("total_functions")
    Integer totalFunctions;

    @JsonProperty("function_percentage_coverage")
    Double functionPercentageCoverage;

    @JsonProperty("decisions_coverage")
    Integer decisionsCovered;

    @JsonProperty("decisions_uncovered")
    Integer decisionsUncovered;

    @JsonProperty("total_decisions")
    Integer totalDecisions;

    @JsonProperty("decision_percentage_coverage")
    Double decisionPercentageCoverage;

    @JsonProperty("conditions_covered")
    Integer conditionsCovered;

    @JsonProperty("conditions_uncovered")
    Integer conditionsUncovered;

    @JsonProperty("total_conditions")
    Integer totalConditions;

    @JsonProperty("condition_percentage_coverage")
    Double conditionPercentageCoverage;
}
