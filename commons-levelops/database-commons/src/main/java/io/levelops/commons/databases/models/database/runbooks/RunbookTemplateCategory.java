package io.levelops.commons.databases.models.database.runbooks;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RunbookTemplateCategory.RunbookTemplateCategoryBuilder.class)
public class RunbookTemplateCategory {

    @JsonProperty("id")
    String id;

    @JsonProperty("hidden")
    Boolean hidden;

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("metadata")
    Map<String, Object> metadata;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    Instant createdAt;

}
