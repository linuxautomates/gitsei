package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BestPracticesItem {
    @JsonProperty(value = "id")
    private UUID id;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "type")
    private BestPracticeType type;

    @JsonProperty(value = "value")
    private String value;

    @JsonProperty(value = "metadata")
    private String metadata;

    @JsonProperty(value = "updated_at")
    private Long updatedAt;

    @JsonProperty(value = "created_at")
    private Long createdAt;

    private List<String> tags;

    public enum BestPracticeType {
        LINK,
        TEXT,
        FILE;

        @JsonCreator
        @Nullable
        public static BestPracticeType fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(BestPracticeType.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString();
        }
    }
}