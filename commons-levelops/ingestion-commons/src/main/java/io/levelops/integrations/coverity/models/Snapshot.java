package io.levelops.integrations.coverity.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.sql.Timestamp;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Snapshot.SnapshotBuilder.class)
public class Snapshot {

    @JsonProperty("snapshot_id")
    Map<String, Integer> snapshotId;

    @JsonProperty("analysis_host")
    String analysisHost;

    @JsonProperty("analysis_version")
    String analysisVersion;

    @JsonProperty("analysis_time")
    Integer analysisTime;

    @JsonProperty("build_failure_count")
    Integer buildFailureCount;

    @JsonProperty("build_success_count")
    Integer buildSuccessCount;

    @JsonProperty("commit_user")
    String commitUser;

    @JsonProperty("date_created")
    Timestamp dateCreated;
}
