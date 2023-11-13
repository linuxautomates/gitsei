package io.levelops.commons.report_models.ba;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.report_models.filters.BACategoryFilter;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BACategory.BACategoryBuilder.class)
public class BACategory {
    @JsonProperty("id")
    UUID id;

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("index")
    Integer index;

    @JsonProperty("color")
    String color;

    @JsonProperty("category_filter")
    BACategoryFilter categoryFilter;

    @JsonProperty("goals")
    Goals goals;

    @JsonProperty("sub_categories")
    List<BACategory> subCategories;
}
