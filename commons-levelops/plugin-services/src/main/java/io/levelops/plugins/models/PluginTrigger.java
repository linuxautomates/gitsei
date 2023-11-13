package io.levelops.plugins.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@EqualsAndHashCode
@Builder(toBuilder = true)
@JsonDeserialize(builder = PluginTrigger.PluginTriggerBuilderImpl.class)
public class PluginTrigger {
    @JsonProperty("company")
    private String company;
    @JsonProperty("plugin_id")
    private UUID pluginId;
    @JsonProperty("plugin_type")
    private String pluginType;
    @JsonProperty("tag_ids")
    private List<String> tagIds;
    @JsonProperty("product_ids")
    private List<String> productIds;
    @JsonProperty("labels")
    private List<String> labels;
    @JsonProperty("trigger")
    private Trigger trigger;


    @Value
    @Builder
    @JsonDeserialize(builder = PluginTrigger.Trigger.TriggerBuilderImpl.class)
    public static class Trigger {
        @JsonProperty("type")
        private String type;
        @JsonProperty("value")
        private String value;
    
        @JsonPOJOBuilder(withPrefix = "")
        static final class TriggerBuilderImpl extends TriggerBuilder {
        }
    }
    
    @JsonPOJOBuilder(withPrefix = "")
    static final class PluginTriggerBuilderImpl extends PluginTriggerBuilder {
    }
}
