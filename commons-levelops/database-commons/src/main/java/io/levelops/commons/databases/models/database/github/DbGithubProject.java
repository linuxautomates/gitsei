package io.levelops.commons.databases.models.database.github;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.levelops.integrations.github.models.GithubProject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbGithubProject {

    private static final String UNKNOWN = "_UNKNOWN_";

    @JsonProperty("id")
    private String id;

    @JsonProperty("project_id")
    private String projectId;

    @JsonProperty("project")
    private String project;

    @JsonProperty("integration_id")
    private String integrationId;

    @JsonProperty("organization")
    private String organization;

    @JsonProperty("description")
    private String description;

    @JsonProperty("state")
    private String state;

    @JsonProperty("creator")
    private String creator;

    @JsonProperty("private")
    private Boolean isPrivate;

    @JsonProperty("project_created_at")
    private Long projectCreatedAt;

    @JsonProperty("project_updated_at")
    private Long projectUpdatedAt;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;

    public static DbGithubProject fromProject(GithubProject source, String integrationId) {
        return DbGithubProject.builder()
                .projectId(source.getId())
                .project(source.getName())
                .integrationId(integrationId)
                .organization(getOrganization(source))
                .description(MoreObjects.firstNonNull(source.getBody(), UNKNOWN))
                .state(source.getState())
                .creator(source.getCreator().getLogin())
                .isPrivate(source.getIsPrivate())
                .projectCreatedAt(source.getCreatedAt().toInstant().getEpochSecond())
                .projectUpdatedAt(source.getUpdatedAt().toInstant().getEpochSecond())
                .build();
    }

    private static String getOrganization(GithubProject source) {
        String ownerUrl = Optional.ofNullable(source.getOwnerUrl())
                .orElse(null);
        String organization = "";
        if (StringUtils.isNotEmpty(ownerUrl)) {
            organization = ownerUrl.substring(ownerUrl.lastIndexOf("/") + 1);
        }
        return organization;
    }

}