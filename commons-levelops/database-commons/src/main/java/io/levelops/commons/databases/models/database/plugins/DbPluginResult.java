package io.levelops.commons.databases.models.database.plugins;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbPluginResult.DbPluginResultBuilder.class)
public class DbPluginResult {


    @JsonProperty("id")
    String id; // UUID

    @JsonProperty("class")
    String pluginClass;

    @JsonProperty("pluginId")
    UUID pluginId; // UUID if in the plugins table

    @JsonProperty("plugin_name")
    String pluginName;

    @JsonProperty("tool")
    String tool; // unique name of the tool or UUID if custom

    @JsonProperty("version")
    String version;

    @JsonProperty("product_ids")
    List<Integer> productIds;

    @JsonProperty("successful")
    Boolean successful;

    @JsonProperty("metadata")
    Map<String, Object> metadata;

    @JsonProperty("gcs_path")
    String gcsPath;

    @JsonProperty("labels")
    List<DbPluginResultLabel> labels;

    @JsonProperty("created_at")
    Long createdAt; // epoch in sec

}
