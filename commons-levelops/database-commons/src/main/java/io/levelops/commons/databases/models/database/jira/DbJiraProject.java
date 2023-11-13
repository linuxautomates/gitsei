package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.jira.models.JiraPriority;
import io.levelops.integrations.jira.models.JiraPriorityScheme;
import io.levelops.integrations.jira.models.JiraProject;
import io.levelops.integrations.jira.models.JiraVersion;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbJiraProject.DbJiraProjectBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbJiraProject {

    @JsonProperty("id")
    String id;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("cloud_id")
    String cloudId;

    @JsonProperty("name")
    String name;

    @JsonProperty("key")
    String key;

    @JsonProperty("is_private")
    Boolean isPrivate;

    @JsonProperty("components")
    List<String> components;

    @JsonProperty("issue_types")
    List<String> issueTypes;

    @JsonProperty("lead_user_id")
    String leadUserId;

    @JsonProperty("created_at")
    Long createdAt;

    @JsonProperty("priority_scheme")
    JiraPriorityScheme priorityScheme;

    @JsonProperty("default_priorities")
    List<JiraPriority> defaultPriorities;

    // Added as part of SEI-3577: this field is used for write purpose only in db
    @JsonProperty("versions")
    List<JiraVersion> versions;

    @JsonProperty("ingested_at")
    Long jiraProjectsIngestedAt;

    public static DbJiraProject fromJiraProject(JiraProject source, String integrationId) {
        List<String> components = new ArrayList<>();
        if (source.getComponents() != null && source.getComponents().size() > 0) {
            source.getComponents().forEach((component) -> components.add(component.getName()));
        }
        List<String> issueTypes = new ArrayList<>();
        if (source.getIssueTypes() != null && source.getIssueTypes().size() > 0) {
            source.getIssueTypes().forEach((issueType) -> issueTypes.add(issueType.getName()));
        }
        return DbJiraProject.builder()
                .cloudId(source.getId())
                .integrationId(integrationId)
                .key(source.getKey())
                .leadUserId((source.getLead() != null) ? source.getLead().getAccountId() : null)
                .name(source.getName())
                .components(components)
                .isPrivate(Boolean.FALSE.equals(source.getIsPrivate()))
                .issueTypes(issueTypes)
                .priorityScheme(source.getPriorityScheme())
                .defaultPriorities(source.getDefaultPriorities())
                .versions(source.getVersions())
                .jiraProjectsIngestedAt(source.getJiraProjectsIngestedAt())
                .build();
    }
}
