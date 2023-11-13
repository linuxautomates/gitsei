package io.levelops.internal_api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PluginsSpec.PluginsSpecBuilder.class)
public class PluginsSpec {

    @JsonProperty("plugins")
    Map<String, PluginSpec> plugins;

    public Optional<PluginSpec> getSpec(String tool) {
        return Optional.ofNullable(plugins)
                .map(m -> m.get(tool));
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = PluginSpec.PluginSpecBuilder.class)
    public static class PluginSpec {

        @JsonProperty("tool")
        String tool;

        /**
         * JSON Paths to base the diff on.
         */
        @JsonProperty("paths")
        List<String> paths;
    }
}
