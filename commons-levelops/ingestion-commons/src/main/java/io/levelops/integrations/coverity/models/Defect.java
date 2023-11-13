package io.levelops.integrations.coverity.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.sql.Timestamp;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Defect.DefectBuilder.class)
public class Defect {

    @JsonProperty("cid")
    Integer cid;

    @JsonProperty("checker_name")
    String checkerName;

    @JsonProperty("component_name")
    String componentName;

    @JsonProperty("cwe")
    Integer cwe;

    @JsonProperty("defect_state_attribute_values")
    List<CoverityAttributes> defectStateAttributeValues;

    @JsonProperty("display_category")
    String displayCategory;

    @JsonProperty("display_impact")
    String displayImpact;

    @JsonProperty("display_issue_kind")
    String displayIssueKind;

    @JsonProperty("display_type")
    String displayType;

    @JsonProperty("domain")
    String domain;

    @JsonProperty("file_pathname")
    String filePathname;

    @JsonProperty("function_name")
    String functionName;

    @JsonProperty("first_detected")
    Timestamp firstDetected;

    @JsonProperty("first_detected_by")
    String firstDetectedBy;

    @JsonProperty("first_detected_stream")
    String firstDetectedStream;

    @JsonProperty("first_detected_snapshot_id")
    Integer firstDetectedSnapshotId;

    @JsonProperty("last_detected")
    Timestamp lastDetected;

    @JsonProperty("last_detected_stream")
    String lastDetectedStream;

    @JsonProperty("last_detected_snapshot_id")
    Long lastDetectedSnapshotId;

    @JsonProperty("merge_key")
    String mergeKey;

    @JsonProperty("misra_category")
    String misraCategory;

    @JsonProperty("occurrence_count")
    Integer occurrenceCount;
}
