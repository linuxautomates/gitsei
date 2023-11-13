package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TextRange.TextRangeBuilder.class)
public class TextRange {

    @JsonProperty("startLine")
    long startLine;

    @JsonProperty("endLine")
    long endLine;

    @JsonProperty("startOffset")
    long startOffset;

    @JsonProperty("endOffset")
    long endOffset;

}
