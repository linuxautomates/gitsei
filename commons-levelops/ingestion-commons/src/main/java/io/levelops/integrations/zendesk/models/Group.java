package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

/**
 * Bean describing a group from https://developer.zendesk.com/rest_api/docs/support/groups#json-format
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Group.GroupBuilder.class)
public class Group {

    @JsonProperty
    Long id;

    @JsonProperty
    String url;

    @JsonProperty
    String name;

    @JsonProperty
    String description;

    @JsonProperty("default")
    Boolean defaultGroup;

    @JsonProperty
    Boolean deleted;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("updated_at")
    Date updatedAt;
}
