package io.levelops.integrations.gitlab.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.client.GitlabClientException;
import io.levelops.integrations.gitlab.models.GitlabChange;
import io.levelops.integrations.gitlab.models.GitlabCommit;
import io.levelops.integrations.gitlab.models.GitlabCommitStat;
import io.levelops.integrations.gitlab.models.GitlabProject;
import io.levelops.integrations.gitlab.models.GitlabPushEvent;
import io.levelops.integrations.gitlab.models.GitlabUser;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class can be used for enriching a {@link GitlabProject} with {@link GitlabCommit}.
 */
@Log4j2
public class GitlabFetchCommitsService {

    private static final int MAX_CHANGES = 250;

    public Stream<GitlabProject> getProjectCommits(
            GitlabClient client,
            GitlabProject project,
            Date from,
            Date to,
            int perPage,
            boolean fetchCommitPatches) {
        final String projectId = project.getId();
        MutableInt commitsCount = new MutableInt(0);
        LoadingCache<String, List<GitlabUser>> projectIdVsUsers = CacheBuilder.from("maximumSize=250,expireAfterWrite=15m")
                .build(CacheLoader.from(projectIdForUsers -> client.streamUsers(projectIdForUsers, from, to, perPage)
                        .collect(Collectors.toList())));
        List<GitlabPushEvent> pushEvents = List.of();
        try {
            // TODO REVISIT THIS CODE: only fetching first page? we can't fetch more without loading everything in memory --> needs larger refactoring
            pushEvents = client.getPushEvents(projectId, from, to, 1)
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(x -> List.of("pushed new", "pushed to").contains(x.getActionName()))
                    .filter(x -> x.getPushData().getCommitTo() != null)
                    .collect(Collectors.toList());
        } catch (GitlabClientException e) {
            log.warn("Failed to get push events for project:{}", projectId);
        }
        List<GitlabPushEvent> finalPushEvents = pushEvents;
        Stream<GitlabCommit> commits = client.streamProjectCommits(projectId, from, to, perPage)
                .filter(Objects::nonNull)
                .filter(commit -> commit.getCommittedDate() != null && commit.getCommittedDate().before(to))
                .takeWhile(commit -> commit.getCommittedDate() != null && commit.getCommittedDate().after(from))
                .filter(Objects::nonNull)
                .map(gitlabCommit -> {
                    try {
                        return parseAndEnrichProjectCommit(client, project, gitlabCommit, perPage, projectIdVsUsers, finalPushEvents, fetchCommitPatches);
                    } catch (GitlabClientException e) {
                        log.error("failed to get commit for projects {}", project.getId(), e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .peek(commit -> {
                    commitsCount.increment();
                    if (commitsCount.getValue() % 50 == 0) {
                        log.info("Processed Commits for projectId={}: commitsCount={} (ts={})",
                                projectId, commitsCount.getValue(), commit.getCommittedDate());
                    }
                });

        return StreamUtils.partition(commits, 100)
                .map(batch -> project.toBuilder()
                        .commits(batch)
                        .build());
    }


    private GitlabCommit parseAndEnrichProjectCommit(GitlabClient client,
                                                     GitlabProject project,
                                                     GitlabCommit commit,
                                                     int perPage,
                                                     LoadingCache<String, List<GitlabUser>> projectIdVsUsers,
                                                     List<GitlabPushEvent> pushEvents,
                                                     boolean fetchCommitPatches) throws GitlabClientException {
        GitlabCommitEnrichmentService enrichmentService = new GitlabCommitEnrichmentService();
        GitlabCommitStat commitStat = enrichmentService.getCommitStat(client, project, commit.getShortId());
        Stream<GitlabChange> changes = Stream.of();
        if (fetchCommitPatches) {
            changes = enrichmentService.getCommitChanges(client, project, commit, perPage);
        }
        GitlabUser authorDetails = getUserDetails(projectIdVsUsers, project.getId(), commit.getAuthorName());
        GitlabUser committerDetails = getUserDetails(projectIdVsUsers, project.getId(), commit.getCommitterName());
        Optional<GitlabPushEvent> pushEventOptional = pushEvents.stream()
                .filter(Objects::nonNull)
                .filter(x -> x.getPushData().getCommitTo().equalsIgnoreCase(commit.getId()))
                .findFirst();
        String refBranch = pushEventOptional
                .map(GitlabPushEvent::getPushData)
                .map(GitlabPushEvent.GitlabPushData::getRef)
                .orElse(null);
        return commit.toBuilder()
                .stats(commitStat)
                .changes(changes.limit(MAX_CHANGES).collect(Collectors.toList()))
                .authorDetails(authorDetails)
                .committerDetails(committerDetails)
                .refBranch(refBranch)
                .build();
    }

    private GitlabUser getUserDetails(LoadingCache<String, List<GitlabUser>> projectIdVsUsers, String projectId, String name) {
        GitlabUser user = null;
        try {
            user = projectIdVsUsers.get(projectId)
                    .stream()
                    .filter(gitlabUser -> name.equalsIgnoreCase(gitlabUser.getName()))
                    .findFirst()
                    .orElse(null);
        } catch (ExecutionException exception) {
            log.error("Failed to get user for name {} ", name, exception);
        }
        return user;
    }
}
