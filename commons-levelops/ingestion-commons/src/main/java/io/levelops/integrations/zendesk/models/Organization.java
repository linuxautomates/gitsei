package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

/**
 * Bean definition for zendesk organization (https://developer.zendesk.com/rest_api/docs/support/organizations)
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Organization.OrganizationBuilder.class)
public class Organization {

    @JsonProperty
    Long id;

    @JsonProperty
    String url;

    @JsonProperty("external_id")
    String externalId;

    @JsonProperty
    String name;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("updated_at")
    Date updatedAt;

    @JsonProperty("domain_names")
    List<String> domainNames;

    @JsonProperty
    String details;

    @JsonProperty
    String notes;

    @JsonProperty("group_id")
    Long groupId;

    @JsonProperty
    Group group;

    @JsonProperty("shared_tickets")
    Boolean sharedTickets;

    @JsonProperty("shared_comments")
    Boolean sharedComments;

    @JsonProperty
    List<String> tags;

}
