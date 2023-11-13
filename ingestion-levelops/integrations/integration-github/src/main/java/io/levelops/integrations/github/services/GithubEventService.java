package io.levelops.integrations.github.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.utils.MapUtils;
import io.levelops.integrations.github.client.GithubClientException;
import io.levelops.integrations.github.client.GithubClient;
import io.levelops.integrations.github.model.GithubApiCommit;
import io.levelops.integrations.github.model.GithubApiRepoEvent;
import io.levelops.integrations.github.model.GithubApiRepoEvent.PushPayload;
import io.levelops.integrations.github.model.GithubConverters;
import io.levelops.integrations.github.models.GithubCommit;
import io.levelops.integrations.github.models.GithubEvent;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class GithubEventService {

    private final ObjectMapper objectMapper;

    public GithubEventService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Nullable
    public List<GithubEvent> getEvents(String repositoryId, Instant from, Instant to, GithubClient levelopsClient) {
        MutableInt nbEvents = new MutableInt(0);
        MutableInt nbCommits = new MutableInt(0);
        return levelopsClient.streamRepoEvents(repositoryId)
                .filter(event -> event.getCreatedAt() != null && event.getCreatedAt().toInstant().isBefore(to))
                .takeWhile(event -> event.getCreatedAt() != null && event.getCreatedAt().toInstant().isAfter(from))
                .map(event -> parseAndEnrichEvent(levelopsClient, repositoryId, event, from, to))
                .filter(Objects::nonNull)
                .peek(e -> {
                    nbEvents.increment();
                    nbCommits.add(CollectionUtils.size(e.getCommits()));
                    if (nbEvents.getValue() % 50 == 0) {
                        log.info("Processing repo={}: events={}, commits={} (ts={})", repositoryId, String.format("%4d", nbEvents.getValue()), String.format("%4d", nbCommits.getValue()), e.getCreatedAt());
                    }
                })
                .limit(10000)  // TODO configurable
                .collect(Collectors.toList());
    }

    private GithubEvent parseAndEnrichEvent(GithubClient levelopsClient, String repositoryId, GithubApiRepoEvent event,
                                            Instant from, Instant to) {
        List<GithubCommit> commits = null;
        if (GithubApiRepoEvent.EventType.PUSH.getValue().equals(event.getType())) {
            PushPayload payload = objectMapper.convertValue(MapUtils.emptyIfNull(event.getPayload()), PushPayload.class);
            String branch = parseBranchFromCommitRef(payload.getRef());
            Stream<String> shaStream = CollectionUtils.emptyIfNull(payload.getCommits())
                    .stream()
                    .map(GithubApiCommit::getSha);
            if (levelopsClient.getIngestCommitFiles()) {
                commits = getCommitsWithFiles(levelopsClient, repositoryId, shaStream);
            } else {
                commits = getCommitsWithoutFiles(levelopsClient, repositoryId, from, to, shaStream.collect(Collectors.toSet()));
            }
            log.trace("Fetched {} commits for event {}", commits.size(), event.getId());
            commits = commits.stream()
                    .map(githubCommit -> githubCommit.toBuilder()
                            .branch(branch)
                            .build())
                    .collect(Collectors.toList());
        }
        return GithubConverters.parseGithubEvent(event, commits);
    }

    @Nullable
    private static String parseBranchFromCommitRef(String commitRef) {
        if (StringUtils.isBlank(commitRef)) {
            return null;
        }
        int i = commitRef.lastIndexOf('/');
        if (i < 0 || i >= commitRef.length() - 1) {
            return null;
        }
        return commitRef.substring(i + 1);
    }

    private List<GithubCommit> getCommitsWithFiles(GithubClient levelopsClient, String repositoryId, Stream<String> shaStream) {
        return shaStream
                .map(sha -> {
                    try {
                        return levelopsClient.getCommit(repositoryId, sha);
                    } catch (GithubClientException e) {
                        log.warn("Failed to get commit for repo={} and sha={}", repositoryId, sha, e);
                        // TODO improve error handling?
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<GithubCommit> getCommitsWithoutFiles(GithubClient levelopsClient, String repositoryId,
                                                      Instant from, Instant to, Set<String> shaStream) {
        return levelopsClient.streamCommitsWithoutFiles(repositoryId, from, to)
                .filter(Objects::nonNull)
                .filter(repositoryCommit -> shaStream.contains(repositoryCommit.getSha()))
                .map(GithubConverters::parseGithubCommit)
                .collect(Collectors.toList());
    }
}
