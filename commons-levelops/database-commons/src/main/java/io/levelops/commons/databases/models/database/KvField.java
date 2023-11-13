package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.models.ContentType;
import io.levelops.commons.models.ValueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class KvField {

    @JsonProperty("key")
    private String key;

    /**
     * To specify in which order the fields will appear in the UI.
     */
    @JsonProperty("index")
    Integer index;

    /**
     * In case the field should be displayed differently from the key.
     * Example: key="product_id", display_name="Product"
     */
    @JsonProperty("display_name")
    private String displayName;

    /**
     * UI type (which widget this field must use).
     * NB: this is not the same as content type. (ui type could be "textbox" while content type could be "integer")
     */
    @JsonProperty("type")
    private String type;

    @JsonProperty("content_type")
    private ContentType contentType;

    @JsonProperty("value_type")
    private ValueType valueType;

    /**
     * To explain what the field is for
     */
    @JsonProperty("description")
    private String description;

    @Singular
    @JsonProperty("options")
    private List<String> options;

    @JsonProperty("required")
    private Boolean required;

    @JsonProperty("hidden")
    @Default
    private Boolean hidden = false;

    @JsonProperty("dynamic_resource_name")
    private String dynamicResourceName;

    @JsonProperty("search_field")
    private String searchField;

    @JsonProperty("validation")
    private String validation;

    @JsonProperty("default_value")
    private String defaultValue;

    @JsonProperty("use_input_fields")
    Map<String, String> useInputFields;

    @Singular
    @JsonProperty("filters")
    private List<KvField> filters;

    public String getDisplayName() {
        return StringUtils.firstNonBlank(displayName, key);
    }
}
