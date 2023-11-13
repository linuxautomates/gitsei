package io.levelops.commons.databases.models.database.runbooks;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RunbookReport.RunbookReportBuilder.class)
public class RunbookReport {

    @JsonProperty("id")
    String id;

    @JsonProperty("runbook_id")
    String runbookId;

    @JsonProperty("run_id")
    String runId;

    @JsonProperty("node_id")
    String source;

    @JsonProperty("title")
    String title;

    @JsonProperty("gcs_path")
    String gcsPath;

    @JsonProperty("created_at")
    Instant createdAt;

}
