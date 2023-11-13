package io.levelops.commons.databases.models.database.zendesk;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.zendesk.models.Field;
import lombok.Builder;
import lombok.Value;

import java.util.regex.Pattern;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbZendeskField.DbZendeskFieldBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbZendeskField {

    public static final Pattern CUSTOM_FIELD_KEY_PATTERN = Pattern.compile("[0-9]+");

    @JsonProperty("id")
    private String id;

    @JsonProperty("field_id")
    private Long fieldId;

    @JsonProperty("integration_id")
    private String integrationId;

    @JsonProperty("title")
    String title;

    @JsonProperty("field_type")
    private String fieldType;

    @JsonProperty("description")
    String description;

    @JsonProperty("created_at")
    private Long createdAt;

    public static DbZendeskField fromZendeskField(Field source, String integrationId) {
        return DbZendeskField.builder()
                .fieldId(source.getId())
                .integrationId(integrationId)
                .title(source.getTitle())
                .fieldType(source.getType())
                .description(source.getDescription())
                .build();
    }
}