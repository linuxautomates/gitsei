package io.levelops.commons.databases.issue_management;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.integrations.azureDevops.models.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbProject {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("project_id")
    private String projectId;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "integration_id")
    private Integer integrationId;

    @JsonProperty(value = "project_key")
    private String projectKey;

    @JsonProperty(value = "components")
    private List<String> components;

    @JsonProperty(value = "is_private")
    private Boolean isPrivate;

    @JsonProperty(value = "lead_user_id")
    private String leadUserId;

    @JsonProperty(value = "created_at")
    private Timestamp createdAt;

    @JsonProperty(value = "attributes")
    private Map<String, Object> attributes;


    public static DbProject fromAzureDevOpsProject(String integrationId, Project project) {
        return DbProject.builder()
                .projectId(project.getId())
                .projectKey(project.getAbbreviation())
                .name(project.getName())
                .integrationId(Integer.parseInt(integrationId))
                .isPrivate("private".equalsIgnoreCase(project.getVisibility()))
                .attributes(project.getState() != null ? Map.of("state", project.getState()) : Map.of())
                .build();
    }
}
