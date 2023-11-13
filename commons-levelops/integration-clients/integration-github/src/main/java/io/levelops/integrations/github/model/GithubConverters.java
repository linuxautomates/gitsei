package io.levelops.integrations.github.model;

import io.levelops.integrations.github.models.GithubCommit;
import io.levelops.integrations.github.models.GithubCommitFile;
import io.levelops.integrations.github.models.GithubCommitStats;
import io.levelops.integrations.github.models.GithubCommitUser;
import io.levelops.integrations.github.models.GithubEvent;
import io.levelops.integrations.github.models.GithubIssue;
import io.levelops.integrations.github.models.GithubIssueEvent;
import io.levelops.integrations.github.models.GithubPullRequest;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.github.models.GithubTag;
import io.levelops.integrations.github.models.GithubUser;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.CommitStats;
import org.eclipse.egit.github.core.CommitUser;
import org.eclipse.egit.github.core.IssueEvent;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.event.Event;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.functional.IterableUtils.parseIterable;

public class GithubConverters {

    @Nullable
    public static GithubRepository parseGithubRepository(@Nullable Repository repository,
                                                         Map<String, Long> languages,
                                                         List<GithubEvent> events,
                                                         List<GithubPullRequest> pullRequests,
                                                         List<GithubTag> tags,
                                                         List<GithubIssue> issues,
                                                         List<GithubIssueEvent> issueEvents) {
        if (repository == null) {
            return null;
        }
        GithubRepository.GithubRepositoryBuilder builder = GithubRepository.builder()
                .name(repository.getName())
                .htmlUrl(repository.getHtmlUrl())
                .masterBranch(repository.getMasterBranch())
                .createdAt(repository.getCreatedAt())
                .updatedAt(repository.getUpdatedAt())
                .pushedAt(repository.getPushedAt())
                .size(repository.getSize())
                .isPrivate(repository.isPrivate())
                .languages(parseGithubLanguages(languages, repository.getLanguage()))
                .events(events)
                .pullRequests(pullRequests)
                .tags(tags)
                .issues(issues)
                .issueEvents(issueEvents);
        GithubUser owner = parseGithubUser(repository.getOwner());
        builder.owner(owner);
        return builder.build();
    }

    @Nullable
    public static GithubRepository parseGithubRepository(@Nullable GithubRepository repository,
                                                         Map<String, Long> languages,
                                                         List<GithubEvent> events,
                                                         List<GithubPullRequest> pullRequests,
                                                         List<GithubTag> tags,
                                                         List<GithubIssue> issues,
                                                         List<GithubIssueEvent> issueEvents) {
        if (repository == null) {
            return null;
        }
        return repository.toBuilder()
                .languages(parseGithubLanguages(languages, repository.getLanguage()))
                .events(events)
                .pullRequests(pullRequests)
                .tags(tags)
                .issues(issues)
                .issueEvents(issueEvents)
                .build();
    }

    public static Map<String, Long> parseGithubLanguages(Map<String, Long> languages, String repoLanguage) {
        if (MapUtils.isNotEmpty(languages)) {
            return languages;
        }
        if (StringUtils.isNotEmpty(repoLanguage)) {
            return Map.of(repoLanguage, 1L);
        }
        return Collections.emptyMap();
    }

    public static GithubUser parseGithubUser(User user) {
        if (user == null) {
            return null;
        }
        return GithubUser.builder()
                .login(user.getLogin())
                .type(GithubUser.OwnerType.fromString(user.getType()))
                .build();
    }

    public static GithubCommitUser parseGithubCommitUser(CommitUser user) {
        if (user == null) {
            return null;
        }
        return GithubCommitUser.builder()
                .date(user.getDate())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    public static GithubEvent parseGithubEvent(Event event, List<GithubCommit> commits) {
        return GithubEvent.builder()
                .id(event.getId())
                .type(event.getType())
                .isPublic(event.isPublic())
                .actor((event.getActor() != null) ? event.getActor().getLogin() : null)
                .org((event.getOrg() != null) ? event.getOrg().getLogin() : null)
                .createdAt(event.getCreatedAt())
                .commits(commits)
                .build();
    }

    public static GithubEvent parseGithubEvent(GithubApiRepoEvent event, List<GithubCommit> commits) {
        return GithubEvent.builder()
                .id(event.getId())
                .type(event.getType())
                .isPublic(BooleanUtils.isTrue(event.getIsPublic()))
                .actor((event.getActor() != null) ? event.getActor().getLogin() : null)
                .createdAt(event.getCreatedAt())
                .commits(commits)
                .build();
    }

    public static GithubCommit parseGithubCommit(RepositoryCommit commit) {
        if (commit == null) {
            return null;
        }
        GithubCommit.GithubCommitBuilder builder = GithubCommit.builder()
                .sha(commit.getSha())
                .url(commit.getUrl())
                .author(parseGithubUser(commit.getAuthor()))
                .committer(parseGithubUser(commit.getCommitter()))
                .stats(parseGithubCommitStats(commit.getStats()))
                .files(parseIterable(commit.getFiles(), GithubConverters::parseGithubCommitFile));
        if (commit.getCommit() != null) {
            builder.message(commit.getCommit().getMessage())
                    .gitAuthor(parseGithubCommitUser(commit.getCommit().getAuthor()))
                    .gitCommitter(parseGithubCommitUser(commit.getCommit().getCommitter()));
        }
        return builder.build();
    }

    public static GithubCommit parseGithubApiCommit(GithubApiCommit commit) {
        if (commit == null) {
            return null;
        }
        GithubCommit.GithubCommitBuilder builder = GithubCommit.builder()
                .sha(commit.getSha())
                .url(commit.getUrl())
                .author(commit.getAuthor())
                .committer(commit.getCommitter())
                .files(commit.getFiles())
                .stats(commit.getStats())
                .branch(commit.getBranch());
        if (commit.getCommit() != null) {
            builder.message(commit.getCommit().getMessage())
                    .gitAuthor(commit.getCommit().getAuthor())
                    .gitCommitter(commit.getCommit().getCommitter());
        }
        return builder.build();
    }

    public static GithubCommitStats parseGithubCommitStats(CommitStats commitStats) {
        if (commitStats == null) {
            return null;
        }
        return GithubCommitStats.builder()
                .additions(commitStats.getAdditions())
                .deletions(commitStats.getDeletions())
                .total(commitStats.getTotal())
                .build();
    }

    public static GithubCommitFile parseGithubCommitFile(CommitFile commitFile) {
        if (commitFile == null) {
            return null;
        }
        return GithubCommitFile.builder()
                .additions(commitFile.getAdditions())
                .changes(commitFile.getChanges())
                .deletions(commitFile.getDeletions())
                .blobUrl(commitFile.getBlobUrl())
                .filename(commitFile.getFilename())
                .patch(commitFile.getPatch())
                .rawUrl(commitFile.getRawUrl())
                .sha(commitFile.getSha())
                .status(commitFile.getStatus())
                .build();
    }

    public static GithubIssueEvent parseGithubIssueEvent(IssueEvent issueEvent) {
        if (issueEvent == null) {
            return null;
        }
        return GithubIssueEvent.builder()
                .id(issueEvent.getId())
                .createdAt(issueEvent.getCreatedAt())
                .commitId(issueEvent.getCommitId())
                .event(issueEvent.getEvent())
                .url(issueEvent.getUrl())
                .actor(issueEvent.getActor() != null ? issueEvent.getActor().getLogin() : null)
                .build();
    }

    public static GithubIssueEvent parseGithubIssueTimelineEvent(GithubApiIssueEvent issueEvent) {
        if (issueEvent == null) {
            return null;
        }
        return GithubIssueEvent.builder()
                .id(issueEvent.getId())
                .createdAt(issueEvent.getCreatedAt())
                .commitId(issueEvent.getCommitId())
                .event(issueEvent.getEvent())
                .url(issueEvent.getUrl())
                .actor(extractLogin(issueEvent.getActor()))
                .assignee(extractLogin(issueEvent.getAssignee()))
                .assigner(extractLogin(issueEvent.getAssigner()))
                .body(issueEvent.getBody())
                .label(issueEvent.getLabel() != null ? issueEvent.getLabel().getName() : null)
                .lockReason(issueEvent.getLockReason())
                .state(issueEvent.getState())
                .user(extractLogin(issueEvent.getUser()))
                .pullRequestUrl(issueEvent.getPullRequestUrl())
                .htmlUrl(issueEvent.getHtmlUrl())
                .submittedAt(issueEvent.getSubmittedAt())
                .reviewRequester(extractLogin(issueEvent.getReviewRequester()))
                .requestedReviewer(extractLogin(issueEvent.getRequestedReviewer()))
                .dismissedReview(issueEvent.getDismissedReview())
                .rename(issueEvent.getRename())
                .projectId(issueEvent.getProjectId())
                .projectUrl(issueEvent.getProjectUrl())
                .columnName(issueEvent.getColumnName())
                .previousColumnName(issueEvent.getPreviousColumnName())
                .milestone(issueEvent.getMilestone() != null ? (String) issueEvent.getMilestone().get("title") : null)
                .source(issueEvent.getSource())
                .author(extractLogin(issueEvent.getAuthor()))
                .committer(extractLogin(issueEvent.getCommitter()))
                .message(issueEvent.getMessage())
                .build();
    }

    @Nullable
    public static String extractLogin(@Nullable GithubUser user) {
        return user != null ? user.getLogin() : null;
    }
}
