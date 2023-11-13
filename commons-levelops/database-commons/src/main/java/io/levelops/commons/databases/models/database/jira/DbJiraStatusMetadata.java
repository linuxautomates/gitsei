package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.jira.models.JiraIssueFields.JiraStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbJiraStatusMetadata.DbJiraStatusMetadataBuilder.class)
public class DbJiraStatusMetadata {

    @JsonProperty("id")
    String id; // internal uuid

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("status_id")
    String statusId;

    @JsonProperty("status")
    String status;

    @JsonProperty("status_category")
    String statusCategory;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    Instant createdAt;

    public static DbJiraStatusMetadata fromJiraStatus(String integrationId, JiraStatus jiraStatus) {
        return DbJiraStatusMetadata.builder()
                .integrationId(integrationId)
                .statusId(jiraStatus.getId())
                .status(jiraStatus.getName())
                .statusCategory((jiraStatus.getStatusCategory() != null) ? jiraStatus.getStatusCategory().getName() : null)
                .build();
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = IntegStatusCategoryMetadata.IntegStatusCategoryMetadataBuilder.class)
    public static class IntegStatusCategoryMetadata {

        @JsonProperty("integration_id")
        String integrationId;

        @JsonProperty("status_category")
        String statusCategory;

        @JsonProperty("statuses")
        List<String> statuses;
    }
}
