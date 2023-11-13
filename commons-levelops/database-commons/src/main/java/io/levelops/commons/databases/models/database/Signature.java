package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.signature.SignatureOperator;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Signature.SignatureBuilder.class)
public class Signature {

    @JsonProperty("id")
    String id;

    @JsonProperty("type")
    String type;

    @JsonProperty("output_type")
    OutputType outputType;

    @JsonProperty("description")
    String description;

    @JsonProperty("product_ids")
    List<String> productIds;

    @JsonProperty("labels")
    Map<String, List<String>> labels;

    @JsonProperty("config")
    Map<String, ? extends SignatureOperator> config;

    @JsonProperty("created_at")
    Long createdAt;

    public enum OutputType {
        ITEMS, // itemized data
        AGGREGATION, // aggregated data
        BLOB; // json blob

        @JsonValue
        public String toString() {
            return super.toString().toLowerCase();
        }

        @JsonCreator
        public OutputType fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(OutputType.class, value);
        }

    }

}
