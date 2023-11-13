package io.levelops.commons.databases.models.config_tables;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Value;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ConfigTable.ConfigTableBuilder.class)
public class ConfigTable {

    @JsonProperty("id")
    String id;
    @JsonProperty("name")
    String name;
    @JsonProperty("schema")
    Schema schema;
    @JsonProperty("total_rows")
    Integer totalRows;
    @JsonProperty("rows")
    Map<String, Row> rows;
    @JsonProperty("version")
    String version;
    @JsonProperty("history")
    Map<String, Revision> history;
    @JsonProperty("created_by")
    String createdBy;
    @JsonProperty("updated_by")
    String updatedBy;
    @JsonProperty("created_at")
    Instant createdAt;
    @JsonProperty("updated_at")
    Instant updatedAt;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Schema.SchemaBuilder.class)
    public static class Schema {
        @JsonProperty("columns")
        Map<String, Column> columns;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Column.ColumnBuilder.class)
    public static class Column {
        @JsonProperty("index")
        Integer index;
        @JsonProperty("id")
        String id;
        @JsonProperty("key")
        String key;
        @JsonProperty("display_name")
        String displayName;
        @JsonProperty("type")
        String type;
        @JsonProperty("options")
        List<String> options;
        @JsonProperty("required")
        Boolean required;
        @JsonProperty("read_only")
        Boolean readOnly;
        @JsonProperty("default_value")
        String defaultValue;
        @JsonProperty("multi_value")
        Boolean multiValue;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    @EqualsAndHashCode
    @Builder(toBuilder = true)
    public static class Row {

        @Getter
        @JsonProperty("id")
        String id;

        @Getter
        @JsonProperty("index")
        Integer index;

        @JsonIgnore
        Map<String, String> values;

        @JsonAnyGetter
        public Map<String, String> getValues() {
            return values;
        }

        @JsonAnySetter
        public void setValue(String key, String value) {
            if (this.values == null) {
                this.values = new HashMap<>();
            }
            this.values.put(key, value);
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Revision.RevisionBuilder.class)
    public static class Revision {
        @JsonProperty("version")
        String version;
        @JsonProperty("created_at")
        Instant createdAt;
        @JsonProperty("user_id")
        String userId;
    }

}
