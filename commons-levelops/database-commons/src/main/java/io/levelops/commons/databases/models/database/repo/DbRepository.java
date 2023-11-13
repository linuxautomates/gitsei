package io.levelops.commons.databases.models.database.repo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.databases.models.database.GitTechnology;
import io.levelops.integrations.bitbucket.models.BitbucketRepository;
import io.levelops.integrations.gerrit.models.ProjectInfo;
import io.levelops.integrations.github.models.GithubRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbRepository {

    @JsonProperty("id")
    private String id;

    @JsonProperty("integration_id")
    private String integrationId;

    @JsonProperty("cloud_id")
    private String cloudId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("owner_name")
    private String ownerName;

    @JsonProperty("owner_type")
    private String ownerType;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("master_branch")
    private String masterBranch;

    @JsonProperty("languages")
    private List<GitTechnology> languages;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("cloud_created_at")
    private Long cloudCreatedAt;

    @JsonProperty("cloud_pushed_at")
    private Long cloudPushedAt;

    @JsonProperty("cloud_updated_at")
    private Long cloudUpdatedAt;

    @JsonProperty("size")
    private Integer size;

    @JsonProperty("30d_commit_count")
    private Integer commitCount30d;

    @JsonProperty("repo_type")
    private String repoType;

    @JsonProperty("is_private")
    private Boolean isPrivate;

    public static DbRepository fromGithubRepository(GithubRepository source,
                                                    String integrationId) {
        List<GitTechnology> gitTechnologyList = new ArrayList<>();
        String repoType = "unknown";
        if (source.getLanguages() != null && source.getLanguages().size() > 0) {
            repoType = (source.getLanguages().containsKey("HCL")
                    || source.getLanguages().containsKey("Puppet")) ? "infra" : "code";
            source.getLanguages()
                    .forEach((lang, lines) -> gitTechnologyList.add(GitTechnology.builder()
                            .integrationId(integrationId)
                            .repoId(source.getId())
                            .name(lang)
                            .build()));
        }
        return DbRepository.builder()
                .cloudCreatedAt((source.getCreatedAt() != null) ?
                        source.getCreatedAt().getTime() / 1000 : null)
                .cloudId(source.getId())
                .cloudPushedAt((source.getPushedAt() != null) ?
                        source.getPushedAt().getTime() / 1000 : null)
                .cloudUpdatedAt((source.getUpdatedAt() != null) ?
                        source.getUpdatedAt().getTime() / 1000 : null)
                .htmlUrl(source.getHtmlUrl())
                .languages(gitTechnologyList)
                .integrationId(integrationId)
                .isPrivate(source.getIsPrivate())
                .masterBranch(source.getMasterBranch())
                .name(source.getName())
                .ownerName(source.getOwner().getLogin())
                .repoType(repoType)
                .ownerType(source.getOwner().getType().toString())
                .size(source.getSize())
                .build();
    }

    public static DbRepository fromBitbucketRepository(BitbucketRepository source, String integrationId) {
        String repoType = "unknown";
        return DbRepository.builder()
                .cloudCreatedAt((source.getCreatedOn() != null) ?
                        source.getCreatedOn().getTime() / 1000 : null)
                .cloudId(source.getUuid())
                .cloudPushedAt((source.getUpdatedOn() != null) ?
                        source.getUpdatedOn().getTime() / 1000 : null)
                .cloudUpdatedAt((source.getUpdatedOn() != null) ?
                        source.getUpdatedOn().getTime() / 1000 : null)
                .htmlUrl(source.getLinks().getHtml().getHref())
                .languages(List.of())
                .integrationId(integrationId)
                .isPrivate(source.getIsPrivate())
                .masterBranch((source.getMainbranch() != null) ? source.getMainbranch().getName() : null)
                .name(source.getName())
                .ownerName((source.getOwner() != null) ? source.getOwner().getUsername() : null)
                .repoType(repoType)
                .ownerType((source.getOwner() != null) ? source.getOwner().getType() : null)
                .size((source.getSize() != null) ? source.getSize().intValue() : null)
                .build();
    }

    //todo: owner name, createdAt, pushedAt, updatedAt, ownerName, repoType, OwnerType, size
    public static DbRepository fromGerritProject(ProjectInfo source, String integrationId) {
        String repoType = "unknown";
        return DbRepository.builder()
                .cloudCreatedAt(0L)
                .cloudId(source.getId())
                .cloudPushedAt(0L)
                .size(0)
                .cloudUpdatedAt(0L)
                .name("_UNKNOWN_")
                .htmlUrl(source.getWebLinks() != null && source.getWebLinks().size() != 0 ?
                        source.getWebLinks().get(0).getUrl() : null)
                .languages(List.of())
                .integrationId(integrationId)
                .isPrivate(Objects.equals(source.getState(), "HIDDEN"))
                .masterBranch(source.getEnrichedBranches() != null ? source.getEnrichedBranches().stream()
                        .filter(branchInfo -> branchInfo.getRef().equals("HEAD")).limit(1).toString() : null)
                .repoType(repoType)
                .build();

    }
}
