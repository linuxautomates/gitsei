package io.levelops.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RunbookReportDTO.RunbookReportDTOBuilder.class)
public class RunbookReportDTO {

    @JsonProperty("id")
    String id;

    @JsonProperty("runbook_id")
    String runbookId;

    @JsonProperty("run_id")
    String runId;

    @JsonProperty("title")
    String title;

    @JsonProperty("records")
    List<Map<String, Object>> records;

    @JsonProperty("columns")
    List<String> columns;

    @JsonProperty("section_titles")
    List<String> sectionTitles;

    @JsonProperty("created_at")
    Instant createdAt;

}
