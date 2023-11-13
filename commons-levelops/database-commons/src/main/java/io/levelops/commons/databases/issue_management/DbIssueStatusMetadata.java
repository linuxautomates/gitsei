package io.levelops.commons.databases.issue_management;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.integrations.azureDevops.models.Metadata;
import io.levelops.integrations.azureDevops.models.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbIssueStatusMetadata {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("integration_id")
    private String integrationId;

    @JsonProperty("project_id")
    private String projectId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("status_category")
    private String statusCategory;

    @JsonProperty("status_id")
    private String statusId;

    @JsonProperty("created_at")
    private Timestamp createdAt;

    public static List<DbIssueStatusMetadata> fromAzureDevopsWorkItemMetadata(String integrationId, Project project, Metadata metadata) {
        return metadata.getWorkItemTypeStates()
                .stream()
                .map(state -> DbIssueStatusMetadata.builder().status(state.getName())
                        .statusCategory(state.getCategory())
                        .statusId(state.getName())
                        .integrationId(integrationId)
                        .projectId(project.getId())
                        .build())
                .collect(Collectors.toList());
    }
}