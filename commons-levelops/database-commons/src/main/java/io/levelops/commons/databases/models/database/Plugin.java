package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import java.util.Map;

import javax.annotation.Nullable;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Plugin.PluginBuilder.class)
public class Plugin {
    @JsonProperty("id")
    String id;

    @JsonProperty("class")
    PluginClass pluginClass;

    @JsonProperty("custom")
    Boolean custom;

    @JsonProperty("tool")
    String tool;

    @JsonProperty("version")
    String version;

    @JsonProperty("name")
    String name;

    @JsonProperty("description")
    String description;

    @JsonProperty("readme")
    Map<String, Object> readme;

    // only for non custom plugins
    @JsonProperty("gcs_path")
    String gcsPath;

    @JsonProperty("created_at")
    Long createdAt;

    public static enum PluginClass {
        REPORT_FILE,
        SOURCES_SCAN,
        CONFIGURATION,
        JENKINS,
        MONITORING;

        @JsonCreator
        @Nullable
        public static PluginClass fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(PluginClass.class, value);
        }
        
        @JsonValue
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }
}
