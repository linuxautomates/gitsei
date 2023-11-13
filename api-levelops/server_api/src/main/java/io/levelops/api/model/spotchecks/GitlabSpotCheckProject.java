package io.levelops.api.model.spotchecks;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.models.ExceptionPrintout;
import io.levelops.integrations.gitlab.models.GitlabProject;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

import static io.levelops.api.model.spotchecks.DateUtils.gitlabDateToString;

@Value
@Builder(toBuilder = true)
public class GitlabSpotCheckProject {
    @JsonProperty("id")
    String id;
    @JsonProperty("name")
    String name;
    @JsonProperty("name_with_namespace")
    String nameWithNamespace;
    @JsonProperty("path_with_namespace")
    String pathWithNamespace;

    @JsonProperty("created_at")
    String createdAt;
    @JsonProperty("last_activity_at")
    String lastActivityAt;

    @JsonProperty("http_url")
    String httpUrl;
    @JsonProperty("default_branch")
    String defaultBranch;

    @JsonProperty("pull_requests")
    List<GitlabSpotCheckPR> pullRequests;

    @JsonProperty("error")
    ExceptionPrintout error;

    public static GitlabSpotCheckProject fromGitlabProject (final GitlabProject p, final List<GitlabSpotCheckPR> pullRequests) {
        GitlabSpotCheckProject.GitlabSpotCheckProjectBuilder bldr = GitlabSpotCheckProject.builder()
                .id(p.getId())
                .name(p.getName())
                .nameWithNamespace(p.getNameWithNamespace())
                .pathWithNamespace(p.getPathWithNamespace())
                .createdAt(gitlabDateToString(p.getCreatedAt()))
                .lastActivityAt(gitlabDateToString(p.getLastActivityAt()))
                .httpUrl(p.getHttpUrlToRepo())
                .defaultBranch(p.getDefaultBranch());
        if (CollectionUtils.isNotEmpty(pullRequests)) {
            bldr.pullRequests(pullRequests);
        }

        GitlabSpotCheckProject r = bldr.build();
        return r;
    }

}
