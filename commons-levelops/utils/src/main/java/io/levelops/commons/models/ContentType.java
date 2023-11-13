package io.levelops.commons.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.util.Strings;

import javax.annotation.Nullable;

/**
 * Represents type of a content.
 * <p>
 * ContentType can be linked to a component:
 * - component_type = integration, component_name = "jira", type = "issue" --> serialized as "integration/jira/issue"
 * Or not:
 * - component_type = NONE, component_name = null, type = "integer" --> serialized as: "integer"
 * <p>
 * NB: This is used by KvData (TicketData) and RunbookVariables.
 */
@Getter
@EqualsAndHashCode
public class ContentType {

    public final static String ID_PREFIX = "id:";
    private final ComponentType componentType;
    private final String componentName;
    private final String type;
    private final String jsonPath;
    private final boolean id;

    protected ContentType(ComponentType componentType, String componentName, String type, String jsonPath, boolean id) {
        Validate.notNull(componentType, "contentType cannot be null.");
        if (componentType != ComponentType.NONE) {
            Validate.notBlank(componentName, "componentName cannot be null or empty.");
        }
        Validate.notBlank(type, "type cannot be null or empty.");
        this.componentType = componentType;
        this.componentName = componentName;
        this.type = type;
        this.jsonPath = jsonPath;
        this.id = id;
    }

    public ContentType toBaseType() {
        return new ContentType(this.componentType, this.componentName, this.type, this.jsonPath,false);
    }

    public ContentType toIdType() {
        return new ContentType(this.componentType, this.componentName, this.type, this.jsonPath, true);
    }

    public ContentType withJsonPath(String jsonPath) {
        return new ContentType(this.componentType, this.componentName, this.type, jsonPath, this.id);
    }

    public ContentType withoutJsonPath() {
        return withJsonPath(null);
    }

    public static ContentType withoutComponent(String type) {
        return new ContentType(ComponentType.NONE, null, type, null, false);
    }

    public static ContentType withComponent(ComponentType componentType, String componentName, String type) {
        return new ContentType(componentType, componentName, type, null, false);
    }

    @Nullable
    @JsonCreator
    public static ContentType fromString(@Nullable String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        if (!value.contains("/")) {
            return ContentType.withoutComponent(StringUtils.lowerCase(value));
        }
        boolean isId = value.startsWith(ID_PREFIX);
        if (isId) {
            value = value.substring(ID_PREFIX.length());
        }
        ContentType contentType;
        String[] parts = value.split("/");
        if (parts.length != 3) {
            contentType = ContentType.withoutComponent(StringUtils.lowerCase(value));
        } else {
            ComponentType componentType = ComponentType.fromString(parts[0]);
            String componentName = StringUtils.lowerCase(parts[1]);
            String type = StringUtils.lowerCase(parts[2]);
            String jsonPath = null;

            int firstDot = type.indexOf(".");
            if (firstDot >= 0) {
                jsonPath = type.substring(firstDot + 1);
                type = type.substring(0, firstDot);
            }

            contentType = ContentType.withComponent(componentType, componentName, type);
            if (jsonPath != null) {
                contentType = contentType.withJsonPath(jsonPath);
            }
        }
        if (isId) {
            contentType = contentType.toIdType();
        }
        return contentType;
    }

    @JsonValue
    @Override
    public String toString() {
        String str;
        if (componentType == null || componentType == ComponentType.NONE || componentType == ComponentType.UNKNOWN) {
            str = type;
        } else {
            str = String.join("/", componentType.toString(), componentName, type);
        }
        if (isId()) {
            str = ID_PREFIX + str;
        }
        if (Strings.isNotEmpty(jsonPath)) {
            str = str + "." + jsonPath;
        }
        return str;
    }
}
