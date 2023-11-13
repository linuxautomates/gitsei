package io.levelops.commons.databases.models.database.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.ingestion.models.IntegrationType;
import lombok.Builder;
import lombok.Value;
import lombok.Builder.Default;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DBOrgContentSection.DBOrgContentSectionBuilder.class)
public class DBOrgContentSection {
    @JsonProperty("id")
    UUID id;
    @JsonProperty("integration_filters")
    Map<String, Object> integrationFilters;
    @JsonProperty("dynamic_users_definition")
    Map<String, Object> dynamicUsers;
    @JsonProperty("user_ref_ids")
    Set<Integer> users;
    @JsonProperty("integration_id")
    Integer integrationId;
    @JsonProperty("integration_type")
    IntegrationType integrationType;
    @JsonProperty("integration_name")
    String integrationName;
    @Default
    @JsonProperty("default_section")
    Boolean defaultSection = false;
}
