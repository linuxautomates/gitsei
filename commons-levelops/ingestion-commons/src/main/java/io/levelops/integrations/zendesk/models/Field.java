package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Field.FieldBuilder.class)
public class Field {
    @JsonProperty("url")
    String url;

    @JsonProperty("id")
    Long id;

    @JsonProperty("type")
    String type;

    @JsonProperty("title")
    String title;

    @JsonProperty("raw_title")
    String rawTitle;

    @JsonProperty("description")
    String description;

    @JsonProperty("raw_description")
    String rawDescription;

    @JsonProperty("position")
    Integer position;

    @JsonProperty("active")
    Boolean active;

    @JsonProperty("required")
    Boolean required;

    @JsonProperty("collapsed_for_agents")
    Boolean collapsedForAgents;

    @JsonProperty("regexp_for_validation")
    String regexpForValidation;

    @JsonProperty("title_in_portal")
    String titleInPortal;

    @JsonProperty("raw_title_in_portal")
    String rawTitleInPortal;

    @JsonProperty("visible_in_portal")
    Boolean visibleInPortal;

    @JsonProperty("editable_in_portal")
    Boolean editableInPortal;

    @JsonProperty("required_in_portal")
    Boolean requiredInPortal;

    @JsonProperty("tag")
    String tag;

    @JsonProperty("created_at")
    String createdAt;

    @JsonProperty("updated_at")
    String updatedAt;

    @JsonProperty("removable")
    Boolean removable;

    @JsonProperty("agent_description")
    String agentDescription;

    @JsonProperty("custom_field_options")
    List<Map<String, String>>  customFieldOptions;

    @JsonProperty("sub_type_id")
    Integer subTypeId;

    @JsonProperty("system_field_options")
    List<Map<String, String>> systemFieldOptions;
}
