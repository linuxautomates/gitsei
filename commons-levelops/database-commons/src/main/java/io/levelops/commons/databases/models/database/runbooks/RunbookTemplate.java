package io.levelops.commons.databases.models.database.runbooks;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RunbookTemplate.RunbookTemplateBuilder.class)
public class RunbookTemplate {

    @JsonProperty("id")
    String id;

    @JsonProperty("hidden")
    Boolean hidden;

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("category")
    String category;

    /**
     * Contains metadata, like UI background color, etc.
     */
    @JsonProperty("metadata")
    Map<String, Object> metadata;

    /**
     * Contains Playbook data using FE export format.
     */
    @JsonProperty("data")
    Map<String, Object> data;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    Instant createdAt;

}
