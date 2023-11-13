package io.levelops.runbooks.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RunbookReportSectionMetadata.RunbookReportSectionMetadataBuilder.class)
public class RunbookReportSectionMetadata {

    @JsonProperty("content_type")
    String contentType;

    @JsonProperty("failed_pages")
    Integer failedPages;

}
