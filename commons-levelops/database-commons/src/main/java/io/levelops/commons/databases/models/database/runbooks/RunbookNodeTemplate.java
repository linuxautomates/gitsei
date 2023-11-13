package io.levelops.commons.databases.models.database.runbooks;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.KvField;
import io.levelops.commons.utils.MapUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RunbookNodeTemplate.RunbookNodeTemplateBuilder.class)
public class RunbookNodeTemplate {

    @JsonProperty("id")
    String id;

    /**
     * UNIQUE type of template; visible in UI
     */
    @JsonProperty("type")
    String type;

    /**
     * BE type used to identify which node handler will execute this node - NOT UNIQUE
     * (multiple nodes with different parameters can be executed by the same node handler)
     */
    @JsonProperty("node_handler")
    String nodeHandler;

    /**
     * Flag to hide template from UI
     */
    @JsonProperty("hidden")
    Boolean hidden;

    @JsonProperty("name")
    String name;
    @JsonProperty("description")
    String description;
    @JsonProperty
    String category;
    @JsonProperty("input")
    Map<String, KvField> input;
    @JsonProperty("output")
    Map<String, RunbookOutputField> output; // only {key, content_type, value_type} needed
    @JsonProperty("options")
    List<String> options;
    @JsonProperty("ui_data")
    Map<String, Object> uiData;
    @JsonProperty("created_at")
    Instant createdAt;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    @SuperBuilder(toBuilder = true)
    public static class RunbookOutputField {
        @NonNull
        @JsonUnwrapped
        KvField kvField;

        @JsonProperty("content_type_from_input")
        String contentTypeFromInput;

        @JsonProperty("content_type_from_input_config_table")
        String contentTypeFromInputConfigTable;
    }

    public static class RunbookNodeTemplateBuilder {

        public RunbookNodeTemplateBuilder inputField(KvField field) {
            this.input = MapUtils.append(this.input, field.getKey(), field);
            return this;
        }

        public RunbookNodeTemplateBuilder outputField(RunbookOutputField field) {
            this.output = MapUtils.append(this.output, field.getKvField().getKey(), field);
            return this;
        }
    }

}
