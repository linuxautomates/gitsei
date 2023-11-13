package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = AggregationRecord.AggregationRecordBuilder.class)
public class AggregationRecord {
    @JsonProperty("id")
    private String id;

    @JsonProperty("version")
    private String version;

    @JsonProperty("successful")
    private Boolean successful;

    @JsonProperty("type")
    private Type type;

    @JsonProperty("tool_type")
    private String toolType;

    @JsonProperty("gcs_path")
    private String gcsPath;

    @JsonProperty("product_ids")
    private List<Integer> productIds;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("created_at")
    private Instant createdAt;

    public enum Type {
        PLUGIN_AGGREGATION,
        COMBINATION_AGGREGATION;

        @JsonCreator
        @Nullable
        public static Type fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(Type.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString();
        }
    }
}
