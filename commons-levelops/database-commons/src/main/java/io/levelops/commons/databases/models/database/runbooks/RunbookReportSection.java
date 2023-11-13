package io.levelops.commons.databases.models.database.runbooks;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RunbookReportSection.RunbookReportSectionBuilder.class)
public class RunbookReportSection {

    @JsonProperty("id")
    String id;

    @JsonProperty("source")
    String source;

    @JsonProperty("report_id")
    String reportId;

    @JsonProperty("gcs_path")
    String gcsPath;

    @JsonProperty("page_count")
    Integer pageCount;

    @JsonProperty("page_size")
    Integer pageSize;

    @JsonProperty("total_count")
    Integer totalCount;

    @JsonProperty("title")
    String title;

    @JsonProperty("metadata")
    Map<String, Object> metadata;

    @JsonProperty("created_at")
    Instant createdAt;

}
