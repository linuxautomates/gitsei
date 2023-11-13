package io.levelops.commons.databases.models.database.runbooks;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.models.ContentType;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RunbookVariable.RunbookVariableBuilder.class)
public class RunbookVariable {

    @JsonProperty("key")
    @JsonAlias("name")
    String name;

    @JsonProperty("type")
    String type; // FE type - type of UI widget to show / input this variable

    @JsonProperty("content_type")
    ContentType contentType; // to keep track of what 'value' contains
    @JsonProperty("value_type")
    RunbookValueType valueType; // to keep track of how 'value' is stored
    @JsonProperty("value")
    Object value; // value - store data there by default, unless the value is not "displayable" - used by FE & BE

    @JsonProperty("data_type")
    RunbookDataType dataType; // BE type - identifies what the internal data is
    @JsonProperty("data")
    Map<String, Object> data; // BE data - store internal data that cannot be displayed here0111

    public enum RunbookValueType {
        NONE, // for variables with no FE visible value
        STRING,
        JSON_BLOB,
        JSON_ARRAY;

        @JsonValue
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }

        @JsonCreator
        @Nullable
        public static RunbookValueType fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(RunbookValueType.class, value);
        }
    }

    public enum RunbookDataType {
        NONE, // for variables with no FE visible value
        GCS_ARRAY_POINTER,
        GCS_ITEM_POINTER;

        @JsonValue
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }

        @JsonCreator
        @Nullable
        public static RunbookDataType fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(RunbookDataType.class, value);
        }
    }
}
