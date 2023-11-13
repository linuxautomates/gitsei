package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraLink.JiraLinkBuilder.class)
public class JiraLink {

    @JsonProperty
    Long id;

    @JsonProperty("ticket_id")
    Long ticketId;

    @JsonProperty("issue_id")
    Long issueId;

    @JsonProperty("issue_key")
    String issueKey;

    @JsonProperty
    String url;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("updated_at")
    Date updatedAt;
}
