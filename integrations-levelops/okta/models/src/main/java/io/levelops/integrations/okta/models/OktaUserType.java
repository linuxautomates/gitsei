package io.levelops.integrations.okta.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OktaUserType.OktaUserTypeBuilder.class)
public class OktaUserType {

    @JsonProperty
    String id;

    @JsonProperty("displayName")
    String displayName;

    @JsonProperty("name")
    String name;

    @JsonProperty
    String description;

    @JsonProperty("createdBy")
    String createdBy;

    @JsonProperty("lastUpdatedBy")
    String lastUpdatedBy;

    @JsonProperty("default")
    Boolean isDefault;

    @JsonProperty
    Date created;

    @JsonProperty("lastUpdated")
    Date lastUpdated;

    @JsonProperty("_links")
    Map<String, OktaHref> links;
}
