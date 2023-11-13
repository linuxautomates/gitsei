package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.Map;
import java.util.UUID;

@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@AllArgsConstructor(onConstructor = @__(@JsonIgnore))
@FieldDefaults(makeFinal = false, level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = TriggerSchema.TriggerSchemaBuilderImpl.class)
public class TriggerSchema {
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("trigger_type")
    private TriggerType triggerType;
    @JsonProperty("description")
    private String description;
    @JsonProperty("fields")
    private Map<String, KvField> fields;
    @JsonProperty("examples")
    private Map<String, Object> examples;
    
    @JsonPOJOBuilder(withPrefix = "")
    static final class TriggerSchemaBuilderImpl
            extends TriggerSchemaBuilder<TriggerSchema, TriggerSchemaBuilderImpl> {
    }
}