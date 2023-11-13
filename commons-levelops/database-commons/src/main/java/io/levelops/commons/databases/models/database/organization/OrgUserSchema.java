package io.levelops.commons.databases.models.database.organization;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;

import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

import java.time.Instant;
import java.util.Set;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OrgUserSchema.OrgUserSchemaBuilder.class)
public class OrgUserSchema {
    @JsonProperty("version")
    Integer version;
    @JsonProperty("created_at")
    Instant createdAt;
    @JsonProperty("fields")
    @Default
    Set<Field> fields = Set.of();

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = OrgUserSchema.Field.FieldBuilder.class)
    public static class Field {
        @JsonProperty("index")
        Integer index;
        @JsonProperty("key")
        String key;
        @JsonProperty("display_name")
        String displayName;
        @JsonProperty("description")
        String description;
        @JsonProperty("type")
        FieldType type;
        @Default
        @JsonProperty("system_field")
        Boolean systemField = false;

        
        public static enum FieldType {
            STRING,
            DATE,
            BOOLEAN;

            @JsonCreator
            @Nullable
            public static FieldType fromString(@Nullable String value) {
                return EnumUtils.getEnumIgnoreCase(FieldType.class, value);
            }
            
            @JsonValue
            @Override
            public String toString() {
                return super.toString().toLowerCase();
            }
        }
    }
}
