package io.levelops.commons.faceted_search.db.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = EsIntegrationUser.EsIntegrationUserBuilder.class)
public class EsIntegrationUser {

    @JsonProperty("id")
    String id;

    @JsonProperty("cloud_id")
    String cloudId;

    @JsonProperty("display_name")
    String displayName;

    @JsonProperty("active")
    Boolean active;
}
