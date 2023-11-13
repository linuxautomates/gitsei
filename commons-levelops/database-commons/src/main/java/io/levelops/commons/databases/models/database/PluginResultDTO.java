package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import lombok.Builder.Default;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PluginResultDTO.PluginResultDTOBuilder.class)
public class PluginResultDTO {

    @JsonProperty("id")
    String id; // UUID

    @JsonProperty("plugin_name")
    String pluginName;

    @JsonProperty("class")
    String pluginClass;

    @JsonProperty("tool")
    String tool; // unique name of the tool or UUID if custom

    @JsonProperty("version")
    String version;

    @JsonProperty("tags")
    @Default
    List<String> tags = new ArrayList<>();

    @JsonProperty("product_ids")
    List<String> productIds;

    @JsonProperty("successful")
    Boolean successful;

    @JsonProperty("metadata")
    Map<String, Object> metadata;

    @JsonProperty("labels")
    Map<String, List<String>> labels;

    @JsonProperty("results")
    Map<String, Object> results;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("created_at_epoch")
    Long createdAtEpoch;
}
