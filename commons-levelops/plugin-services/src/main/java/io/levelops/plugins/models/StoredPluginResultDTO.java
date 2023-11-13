package io.levelops.plugins.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.PluginResultDTO;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = StoredPluginResultDTO.StoredPluginResultDTOBuilder.class)
public class StoredPluginResultDTO {
    @JsonProperty("plugin_result")
    private final PluginResultDTO pluginResult;
    @JsonProperty("result_id")
    private final UUID resultId;
    @JsonProperty("plugin_result_storage_path")
    private final String pluginResultStoragePath;
}
