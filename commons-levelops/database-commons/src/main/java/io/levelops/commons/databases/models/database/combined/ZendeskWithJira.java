package io.levelops.commons.databases.models.database.combined;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ZendeskWithJira.ZendeskWithJiraBuilder.class)
public class ZendeskWithJira {
    @JsonProperty
    String id;

    @JsonProperty("ticket_id")
    Long ticketId;

    @JsonProperty("brand")
    String brand;

    @JsonProperty("jira_keys")
    List<String> jiraKeys;

    @JsonProperty
    String type;

    @JsonProperty("ticket_url")
    String ticketUrl;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty
    String subject;

    @JsonProperty
    String priority;

    @JsonProperty
    String status;

    @JsonProperty("submitter_email")
    String submitterEmail;

    @JsonProperty("assignee_email")
    String assigneeEmail;

    @JsonProperty("ticket_created_at")
    Date ticketCreatedAt;

    @JsonProperty("ticket_updated_at")
    Date ticketUpdatedAt;

    @JsonProperty("escalation_time")
    Long escalationTime;

    @JsonProperty("jira_key")
    String jiraKey;
}
