package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbJiraPriority.DbJiraPriorityBuilder.class)
public class DbJiraPriority {

    @JsonProperty("id")
    String id;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("project_key")
    String projectKey;

    @JsonProperty("scheme")
    String scheme;

    @JsonProperty("name")
    String name;

    @JsonProperty("order")
    Integer order;

    @JsonProperty("description")
    String description;
}
