package io.levelops.commons.databases.models.database.bullseye;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize
public class BullseyeFolder {

    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("total_functions")
    Integer totalFunctions;

    @JsonProperty("functions_covered")
    Integer functionsCovered;

    @JsonProperty("total_decisions")
    Integer totalDecisions;

    @JsonProperty("decisions_covered")
    Integer decisionsCovered;

    @JsonProperty("total_conditions")
    Integer totalConditions;

    @JsonProperty("conditions_covered")
    Integer conditionsCovered;

    @JsonProperty("source_files")
    List<BullseyeSourceFile> sourceFiles;

    @JsonProperty("folders")
    List<BullseyeFolder> folders;
}
