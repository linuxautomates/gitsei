package io.levelops.integrations.testrails.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CaseField.CaseFieldBuilder.class)
public class CaseField {

    @JsonProperty("id")
    Long id;
    @JsonProperty("is_active")
    Boolean isActive;
    @JsonProperty("label")
    String label;
    @JsonProperty("name")
    String name;
    @JsonProperty("system_name")
    String systemName;
    @JsonProperty("type_id")
    Integer typeId;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    FieldType type;

    public FieldType getType(){
        return FieldType.fromTypeId(typeId).orElse(null);
    }

    @JsonProperty("configs")
    List<FieldConfig> configs;

    @Getter
    @AllArgsConstructor
    public enum FieldType {
        STRING(1),
        INTEGER(2),
        TEXT(3),
        URL(4),
        CHECKBOX(5),
        DROPDOWN(6),
        USER(7),
        DATE(8),
        MILESTONE(9),
        STEPS(10),
        MULTI_SELECT(12),
        SCENARIO(13);

        private final Integer typeId;

        public static Optional<FieldType> fromTypeId(int typeId){
            return Arrays.stream(FieldType.values()).filter(id -> typeId == id.typeId).findFirst();
        }
        public static FieldType fromString(String type){
            return EnumUtils.getEnumIgnoreCase(FieldType.class, type);
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = FieldConfig.FieldConfigBuilder.class)
    public static class FieldConfig {
        @JsonProperty("context")
        ConfigContext context;
        @JsonProperty("options")
        ConfigOption options;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = ConfigContext.ConfigContextBuilder.class)
        public static class ConfigContext {
            @JsonProperty("is_global")
            Boolean isGlobal;
            @JsonProperty("project_ids")
            List<Integer> projectIds;
        }

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = ConfigOption.ConfigOptionBuilder.class)
        public static class ConfigOption {
            @JsonProperty("items")
            String items;

            public Map<Integer, String> getItemsMap(){
                if(StringUtils.isEmpty(items)){
                    return new HashMap<>();
                }
                return Arrays.stream(items.split("\n")).map(item -> {
                    String[] itemSplit = item.split(",");
                    return Map.of("item_id", itemSplit[0].trim(),"item_name", itemSplit[1].trim());
                }).collect(Collectors.toMap(i -> Integer.valueOf(i.get("item_id")), s -> s.get("item_name")));
            }
        }
    }
}