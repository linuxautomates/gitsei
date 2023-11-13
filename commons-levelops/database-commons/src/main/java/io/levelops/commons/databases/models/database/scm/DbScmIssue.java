package io.levelops.commons.databases.models.database.scm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.levelops.integrations.github.models.GithubIssue;
import io.levelops.integrations.github.models.GithubIssueEvent;
import io.levelops.integrations.github.models.GithubUser;
import io.levelops.integrations.gitlab.models.GitlabIssue;
import io.levelops.integrations.gitlab.models.GitlabIssueNote;
import io.levelops.integrations.gitlab.models.GitlabUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbScmIssue {
    static final String UNKNOWN = "_UNKNOWN_";

    @JsonProperty("id")
    @JsonAlias({"i_id"})
    private String id;

    @JsonProperty("integration_id")
    @JsonAlias({"i_integration_id"})
    private String integrationId;

    @JsonProperty("repo_id")
    @JsonAlias({"i_repo_id"})
    private String repoId; //repository id -- something like: "levelops/api-levelops"

    @JsonProperty("project")
    @JsonAlias({"i_project"})
    private String project;

    @JsonProperty("issue_id")
    @JsonAlias({"i_issue_id"})
    private String issueId;

    @JsonProperty("number")
    @JsonAlias({"i_number"})
    private String number;

    @JsonProperty("creator")
    @JsonAlias({"i_creator"})
    private String creator;

    @JsonProperty("creator_info")
    @JsonAlias({"i_creator_info"})
    private DbScmUser creatorInfo; //contains DbScmUser object for creator

    @JsonProperty("assignees")
    @JsonAlias({"i_assignees"})
    private List<String> assignees;

    @JsonProperty("labels")
    @JsonAlias({"i_labels"})
    private List<String> labels;

    @JsonProperty("state")
    @JsonAlias({"i_state"})
    private String state;

    @JsonProperty("title")
    @JsonAlias({"i_title"})
    private String title;

    @JsonProperty("url")
    @JsonAlias({"i_url"})
    private String url;

    @JsonProperty("num_comments")
    @JsonAlias({"i_num_comments"})
    private Integer numComments;

    @JsonProperty("issue_created_at")
    @JsonAlias({"i_issue_created_at"})
    private Long issueCreatedAt;

    @JsonProperty("issue_updated_at")
    @JsonAlias({"i_issue_updated_at"})
    private Long issueUpdatedAt;

    @JsonProperty("issue_closed_at")
    @JsonAlias({"i_issue_closed_at"})
    private Long issueClosedAt;

    @JsonProperty("first_comment_at")
    @JsonAlias({"i_first_comment_at"})
    private Long firstCommentAt;

    @JsonProperty("created_at")
    @JsonAlias({"i_created_at"})
    private Long createdAt;

    @JsonProperty("creator_id")
    @JsonAlias({"i_creator_id"})
    private String creatorId;

    @JsonProperty("response_time")
    @JsonAlias({"i_response_time"})
    private Long responseTime;

    @JsonProperty("solve_time")
    @JsonAlias({"i_solve_time"})
    private Long solveTime;

    public static DbScmIssue fromGithubIssue(GithubIssue githubIssue,
                                             String repoId,
                                             String integrationId) {
        String creator = githubIssue.getUser().getLogin();
        Long firstCommentAt = null;
        if (CollectionUtils.isNotEmpty(githubIssue.getEvents())) {
            for (GithubIssueEvent event : githubIssue.getEvents()) {
                if ("commented".equals(event.getEvent()) && !Objects.equals(creator, event.getActor())) {
                    if (firstCommentAt != null
                            && firstCommentAt < event.getCreatedAt().toInstant().getEpochSecond())
                        continue;
                    firstCommentAt = event.getCreatedAt().toInstant().getEpochSecond();
                }
            }
        }
        Long issueClosedAt = githubIssue.getClosedAt() != null ?
                githubIssue.getClosedAt().toInstant().getEpochSecond() : null;
        return DbScmIssue.builder()
                .repoId(repoId)
                .project(MoreObjects.firstNonNull(repoId, ""))
                .creator(creator)
                .creatorInfo(DbScmUser.fromGithubIssueCreator(githubIssue, integrationId))
                .integrationId(integrationId)
                .issueId(githubIssue.getId())
                .issueClosedAt(issueClosedAt)
                .firstCommentAt(firstCommentAt)
                .state(githubIssue.getState())
                .title(MoreObjects.firstNonNull(githubIssue.getTitle(), ""))
                .url(githubIssue.getHtmlUrl())
                .numComments(githubIssue.getComments())
                .number(String.valueOf(githubIssue.getNumber()))
                .issueUpdatedAt(githubIssue.getUpdatedAt().toInstant().getEpochSecond())
                .issueCreatedAt(githubIssue.getCreatedAt().toInstant().getEpochSecond())
                .assignees(ListUtils.emptyIfNull(
                        githubIssue.getAssignees()).stream().map(GithubUser::getLogin).collect(Collectors.toList()))
                .labels(ListUtils.emptyIfNull(
                        githubIssue.getLabels()).stream().map(GithubIssue.Label::getName).collect(Collectors.toList()))
                .build();
    }

    public static DbScmIssue fromGitlabIssue(GitlabIssue gitlabIssue,
                                             String projectId,
                                             String integrationId) {

        Long firstCommentAt = null;
        for (GitlabIssueNote note : CollectionUtils.emptyIfNull(gitlabIssue.getNotes())) {
            if (firstCommentAt != null
                    && firstCommentAt < note.getCreatedAt().toInstant().getEpochSecond())
                continue;
            firstCommentAt = note.getCreatedAt().toInstant().getEpochSecond();
        }
        return DbScmIssue.builder()
                .id(gitlabIssue.getIid())
                .repoId(projectId)
                .project(MoreObjects.firstNonNull(projectId, ""))
                .integrationId(integrationId)
                .title(MoreObjects.firstNonNull(gitlabIssue.getTitle(), ""))
                .issueId(gitlabIssue.getIid())
                .creator(gitlabIssue.getAuthor().getUsername())
                .creatorInfo(DbScmUser.fromGitlabIssueCreator(gitlabIssue, integrationId))
                .state(gitlabIssue.getState())
                .assignees(ListUtils.emptyIfNull(
                        gitlabIssue.getAssignees()).stream().map(GitlabUser::getUsername)
                        .collect(Collectors.toList()))
                .labels(MoreObjects.firstNonNull(gitlabIssue.getLabels(), Collections.emptyList()))
                .number(gitlabIssue.getIid())
                .url(gitlabIssue.getWebUrl())
                .numComments(gitlabIssue.getUserNotesCount())
                .issueUpdatedAt(gitlabIssue.getUpdatedAt().toInstant().getEpochSecond())
                .issueCreatedAt(gitlabIssue.getCreatedAt().toInstant().getEpochSecond())
                .issueClosedAt(gitlabIssue.getClosedAt() != null ? gitlabIssue.getClosedAt()
                        .toInstant().getEpochSecond() : null)
                .createdAt(gitlabIssue.getCreatedAt().toInstant().getEpochSecond())
                .firstCommentAt(firstCommentAt)
                .build();
    }
}
