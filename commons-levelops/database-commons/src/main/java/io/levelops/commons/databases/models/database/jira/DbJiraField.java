package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.jira.models.JiraField;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.regex.Pattern;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbJiraField.DbJiraFieldBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbJiraField {
    public static final Pattern CUSTOM_FIELD_KEY_PATTERN = Pattern.compile("^customfield_[0-9]{4,6}$");
    public static final String EPIC_LINK_NAME = "Epic Link";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_OPTION = "option";
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_ARRAY = "array";
    public static final String TYPE_USER = "user";
    public static final String TYPE_DATE = "date";
    public static final String TYPE_DATETIME = "datetime";
    public static final String TYPE_OPTION_WITH_CHILD = "option-with-child";
    public static final String CUSTOM_TYPE_TEAM = "com.atlassian.teams:rm-teams-custom-field-team";
    public static final String CUSTOM_TYPE_EPIC_LINK = "com.pyxis.greenhopper.jira:gh-epic-link";
    public static final String CUSTOM_TYPE_RELEASE_LINK_2 = "com.atlassian.plugins.atlassian-connect-plugin:net.caelor.jira.cloud.advancedissuelinks__releaselink2-1642081770445";
    public static final String CUSTOM_TYPE_TEMPO_ACCOUNT = "com.atlassian.plugins.atlassian-connect-plugin:io.tempo.jira__account";
    public static final String CUSTOM_TYPE_FABRIC_PARENT_LINK = "com.atlassian.jpo:jpo-custom-field-parent";

    public static final Set<String> SUPPORTED_TYPES = Set.of(TYPE_STRING, TYPE_OPTION, TYPE_NUMBER, TYPE_ARRAY, TYPE_USER, TYPE_DATE, TYPE_DATETIME, TYPE_OPTION_WITH_CHILD);
    public static final Set<String> SUPPORTED_CUSTOM_TYPES = Set.of(CUSTOM_TYPE_EPIC_LINK, CUSTOM_TYPE_TEAM, CUSTOM_TYPE_RELEASE_LINK_2, CUSTOM_TYPE_TEMPO_ACCOUNT, CUSTOM_TYPE_FABRIC_PARENT_LINK);

    @JsonProperty("id")
    private String id;

    @JsonProperty("integration_id")
    private String integrationId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("custom")
    private Boolean custom;

    @JsonProperty("field_key")
    private String fieldKey;

    @JsonProperty("field_type")
    private String fieldType; //String or Array is what we support

    @JsonProperty("field_items")
    private String fieldItems; //null or String is what we support

    @JsonProperty("created_at")
    private Long createdAt;

    @Nullable
    public static DbJiraField fromJiraField(JiraField source, String integrationId) {
        // we only store custom fields now.
        if (!Boolean.TRUE.equals(source.getCustom())) {
            return null;
        }

        JiraField.Schema schema = source.getSchema();
        if (schema == null) {
            return null;
        }
        String fieldType = StringUtils.defaultString(schema.getType()).toLowerCase();
        String fieldCustom = StringUtils.defaultString(schema.getCustom()).toLowerCase();
        boolean isTypeSupported = SUPPORTED_TYPES.contains(fieldType);
        boolean isCustomTypeSupported = SUPPORTED_CUSTOM_TYPES.contains(fieldCustom);
        if (!isTypeSupported && !isCustomTypeSupported) {
            return null;
        }

        String type = isCustomTypeSupported? fieldCustom : fieldType;
        String items = StringUtils.lowerCase(schema.getItems());
        String key;
        if (isCustomFieldKey(source.getKey())) {
            key = source.getKey();
        } else if (isCustomFieldKey(source.getId())) {
            key = source.getId();
        } else {
            return null; //no point in storing a field with no key or id, or a weird key
        }

        return DbJiraField.builder()
                .custom(true)
                .integrationId(integrationId)
                .fieldKey(key)
                .fieldType(type)
                .fieldItems(items)
                .name(source.getName())
                .build();
    }

    public static boolean isCustomFieldKey(String value) {
        return StringUtils.isNotBlank(value) && CUSTOM_FIELD_KEY_PATTERN.matcher(value).matches();
    }
}
