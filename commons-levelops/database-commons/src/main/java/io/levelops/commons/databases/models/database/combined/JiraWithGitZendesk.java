package io.levelops.commons.databases.models.database.combined;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ZendeskWithJira.ZendeskWithJiraBuilder.class)
public class JiraWithGitZendesk {
    @JsonProperty
    String id;

    @JsonProperty("key")
    String key;

    @JsonProperty("components")
    List<String> components;

    @JsonProperty("labels")
    List<String> labels;

    @JsonProperty("issue_type")
    String issueType;

    @JsonProperty("reporter")
    String reporter;

    @JsonProperty("assignee")
    String assignee;

    @JsonProperty
    String status;

    @JsonProperty("bounces")
    Integer bounces;

    @JsonProperty("issue_created_at")
    Long issueCreatedAt;

    @JsonProperty("custom_fields")
    Map<String, Object> customFields;

    @JsonProperty("zendesk_tickets")
    List<Long> zendeskTickets;
}
