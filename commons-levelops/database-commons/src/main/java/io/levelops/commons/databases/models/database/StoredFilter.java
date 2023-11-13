package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoredFilter {
    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("type")
    String type;

    @JsonProperty("description")
    String description;

    @Builder.Default
    @JsonProperty("is_default")
    Boolean isDefault = false;

    @JsonProperty("filter")
    Map<String, Object> filter;

    @JsonProperty("created_at")
    Long createdAt;

    @JsonProperty("updated_at")
    Long updatedAt;
}
