package io.levelops.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.databases.models.database.TriggerSchema;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@SuperBuilder(toBuilder = true)
public class UITriggerSchema extends TriggerSchema {
    @JsonProperty("ui_data")
    private Map<String, Object> uiData;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("type")
    private String type;

    @JsonProperty("category")
    private String category;
}