package io.levelops.commons.databases.models.database.mappings;

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

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TagItemMapping {
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "tag_id")
    private String tagId;

    @JsonProperty(value = "item_id")
    private String itemId;

    @JsonProperty(value = "item_type")
    private TagItemType tagItemType;

    @JsonProperty(value = "created_at")
    private Long createdAt;

    public enum TagItemType {
        PLUGIN_RESULT,
        POLICY,
        SECTION,
        INTEGRATION,
        BEST_PRACTICE,
        QUESTIONNAIRE_TEMPLATE,
        QUESTION,
        WORK_ITEM;

        @JsonCreator
        @Nullable
        public static TagItemType fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(TagItemType.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString();
        }
    }
}