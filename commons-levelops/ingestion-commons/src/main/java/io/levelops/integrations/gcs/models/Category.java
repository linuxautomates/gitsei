package io.levelops.integrations.gcs.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Category.CategoryBuilder.class)
public class Category {

    @JsonProperty("category")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("reports")
    private List<ReportDocumentation> reportDocumentations;
}
