package io.levelops.runbooks.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RunbookReportSectionData.RunbookReportSectionDataBuilder.class)
public class RunbookReportSectionData {

    @JsonProperty("records")
    List<Map<String, Object>> records;

}
