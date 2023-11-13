package io.levelops.commons.databases.models.database.plugins;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Used for intermediate aggregation results.
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbPluginLabelsAgg.DbPluginLabelsAggBuilder.class)
public class DbPluginLabelsAgg {
    @JsonProperty("key")
    String key;
    @JsonProperty("values")
    List<String> values;
}
