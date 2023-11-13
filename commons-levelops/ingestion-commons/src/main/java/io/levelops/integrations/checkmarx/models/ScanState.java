package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ScanState.ScanStateBuilder.class)
public class ScanState {

    @JsonProperty("path")
    String path;

    @JsonProperty("sourceId")
    String sourceId;

    @JsonProperty("filesCount")
    Integer filesCount;

    @JsonProperty("linesOfCode")
    Integer linesOfCode;

    @JsonProperty("failedLinesOfCode")
    Integer failedLinesOfCode;

    @JsonProperty("cxVersion")
    String cxVersion;

    @JsonProperty("languageStateCollection")
    List<LanguageState> languageStateList;
}
