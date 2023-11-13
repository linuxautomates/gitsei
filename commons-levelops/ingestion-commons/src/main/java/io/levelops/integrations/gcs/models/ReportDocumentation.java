package io.levelops.integrations.gcs.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ReportDocumentation.ReportDocumentationBuilder.class)
public class ReportDocumentation {

    @JsonProperty("id")
    private String id;

    @JsonProperty("image-url")
    private String imageUrl;

    @JsonProperty("description")
    private String description;

    @JsonProperty("report-categories")
    private List<String> categories;

    @JsonProperty("reports")
    private List<String> reports;

    @JsonProperty("content")
    private String content;

    @JsonProperty("variants")
    private List<String> variants;

    @JsonProperty("applications")
    private List<String> applications;

    @JsonProperty("related-reports")
    private List<String> relatedReports;
}
