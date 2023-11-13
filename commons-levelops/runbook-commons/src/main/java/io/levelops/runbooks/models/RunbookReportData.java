package io.levelops.runbooks.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.runbooks.RunbookReport;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RunbookReportData.RunbookReportDataBuilder.class)
public class RunbookReportData {

    @JsonProperty("report")
    RunbookReport report;

    @JsonProperty("records")
    List<Map<String, Object>> records;

    @JsonProperty("columns")
    List<String> columns;

    @JsonProperty("section_titles")
    List<String> sectionTitles;

    // TODO paginate

}
