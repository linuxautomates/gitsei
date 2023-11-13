package io.levelops.commons.databases.models.database.coverity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.integrations.coverity.models.CoverityAttributes;
import io.levelops.integrations.coverity.models.Defect;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbCoverityDefect {

    @JsonProperty("id")
    private String id;

    @JsonProperty("integration_id")
    private Integer integrationId;

    @JsonProperty("snapshot_id")
    private String snapshotId;

    @JsonProperty("cid")
    private Integer cid;

    @JsonProperty("checker_name")
    private String checkerName;

    @JsonProperty("component_name")
    private String componentName;

    @JsonProperty("cwe")
    private Integer cwe;

    @JsonProperty("attributes")
    private List<CoverityAttributes> attributes;

    @JsonProperty("defect_attributes")
    private Map<String, Object> dbAttributes;

    @JsonProperty("category")
    private String category;

    @JsonProperty("impact")
    private String impact;

    @JsonProperty("kind")
    private String kind;

    @JsonProperty("type")
    private String type;

    @JsonProperty("domain")
    private String domain;

    @JsonProperty("file_path")
    private String filePath;

    @JsonProperty("function_name")
    private String functionName;

    @JsonProperty("first_detected_at")
    private Timestamp firstDetectedAt;

    @JsonProperty("first_detected_by")
    private String firstDetectedBy;

    @JsonProperty("first_detected_stream")
    private String firstDetectedStream;

    @JsonProperty("first_detected_snapshot_id")
    private Integer firstDetectedSnapshotId;

    @JsonProperty("last_detected_at")
    private Timestamp lastDetectedAt;

    @JsonProperty("last_detected_stream")
    private String lastDetectedStream;

    @JsonProperty("last_detected_snapshot_id")
    private Integer lastDetectedSnapshotId;

    @JsonProperty("merge_key")
    private String mergeKey;

    @JsonProperty("misra_category")
    private String misraCategory;

    @JsonProperty("occurrence_count")
    private Integer occurrenceCount;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSZ")
    private Date createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSZ")
    private Date updatedAt;

    public static DbCoverityDefect fromDefect(Defect defects, String integrationId, String snapshotId) {
        return DbCoverityDefect.builder()
                .integrationId(Integer.valueOf(integrationId))
                .snapshotId(snapshotId)
                .cid(defects.getCid())
                .checkerName(defects.getCheckerName())
                .componentName(defects.getComponentName())
                .cwe(defects.getCwe())
                .attributes(defects.getDefectStateAttributeValues())
                .category(defects.getDisplayCategory())
                .impact(defects.getDisplayImpact())
                .kind(defects.getDisplayIssueKind())
                .type(defects.getDisplayType())
                .domain(defects.getDomain())
                .filePath(defects.getFilePathname())
                .functionName(defects.getFunctionName())
                .firstDetectedAt(defects.getFirstDetected())
                .firstDetectedBy(defects.getFirstDetectedBy())
                .firstDetectedStream(defects.getFirstDetectedStream())
                .firstDetectedSnapshotId(defects.getFirstDetectedSnapshotId())
                .lastDetectedAt(defects.getLastDetected())
                .lastDetectedStream(defects.getLastDetectedStream())
                .lastDetectedSnapshotId(defects.getFirstDetectedSnapshotId())
                .mergeKey(defects.getMergeKey())
                .misraCategory(defects.getMisraCategory())
                .occurrenceCount(defects.getOccurrenceCount())
                .build();
    }
}
