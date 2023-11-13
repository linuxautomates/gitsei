package io.levelops.commons.databases.models.database.coverity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.integrations.coverity.models.Snapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.sql.Date;
import java.sql.Timestamp;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbCoveritySnapshot {

    @JsonProperty("id")
    private String id;

    @JsonProperty("integration_id")
    Integer integrationId;

    @JsonProperty("stream_id")
    private String streamId;

    @JsonProperty("snapshot_id")
    private Integer snapshotId;

    @JsonProperty("analysis_host")
    private String analysisHost;

    @JsonProperty("analysis_version")
    private String analysisVersion;

    @JsonProperty("time_taken")
    private Integer timeTaken;

    @JsonProperty("build_failure_count")
    private Integer buildFailureCount;

    @JsonProperty("build_success_count")
    private Integer buildSuccessCount;

    @JsonProperty("commit_user")
    private String commitUser;

    @JsonProperty("snapshot_created_at")
    private Timestamp snapshotCreatedAt;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSZ")
    private Date createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSZ")
    private Date updatedAt;

    public static DbCoveritySnapshot fromSnapshot(Snapshot snapshot, String integrationId, String streamId) {
        return DbCoveritySnapshot.builder()
                .integrationId(Integer.valueOf(integrationId))
                .streamId(streamId)
                .snapshotId(snapshot.getSnapshotId().get("id"))
                .analysisHost(snapshot.getAnalysisHost())
                .analysisVersion(snapshot.getAnalysisVersion())
                .timeTaken(snapshot.getAnalysisTime())
                .buildFailureCount(snapshot.getBuildFailureCount())
                .buildSuccessCount(snapshot.getBuildSuccessCount())
                .commitUser(snapshot.getCommitUser())
                .snapshotCreatedAt(snapshot.getDateCreated())
                .build();
    }
}
