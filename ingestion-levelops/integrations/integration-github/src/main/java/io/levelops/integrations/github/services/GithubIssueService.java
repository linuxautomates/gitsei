package io.levelops.integrations.github.services;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.github.client.GithubClientException;
import io.levelops.integrations.github.client.GithubClientFactory;
import io.levelops.integrations.github.client.GithubClient;
import io.levelops.integrations.github.model.GithubConverters;
import io.levelops.integrations.github.models.GithubIssue;
import io.levelops.integrations.github.models.GithubIssueEvent;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
public class GithubIssueService {

    private final GithubClientFactory levelOpsClientFactory;

    public GithubIssueService(GithubClientFactory levelOpsClientFactory) {
        this.levelOpsClientFactory = levelOpsClientFactory;
    }

    public List<GithubIssue> getIssues(IntegrationKey integrationKey, String repositoryId, Instant from, Instant to, boolean onboarding, boolean fetchEvents) throws GithubClientException {
        Map<String, String> params = Map.of(
                "state", "all",
                "since", from.toString(),
                "direction", "asc",
                "sort", "updated");
        return levelOpsClientFactory.get(integrationKey, false).streamIssues(repositoryId, params)
                .filter(issue -> issue.getUpdatedAt() != null && issue.getUpdatedAt().toInstant().isBefore(to))
                .takeWhile(issue -> issue.getUpdatedAt() != null && issue.getUpdatedAt().toInstant().isAfter(from))
                .map(i -> parseAndEnrichIssue(integrationKey, i, repositoryId, from, to, onboarding, fetchEvents))
                .collect(Collectors.toList());
    }

    public GithubIssue parseAndEnrichIssue(IntegrationKey integrationKey, GithubIssue issue, String repositoryId, Instant from, Instant to, boolean onboarding, boolean fetchEvents) {
        if (!fetchEvents || issue.getNumber() == null) {
            return issue;
        }
        List<GithubIssueEvent> events = null;
        try {
            events = getIssueTimelineEvents(integrationKey, repositoryId, issue.getNumber().intValue(), from, to, onboarding);
        } catch (GithubClientException e) {
            log.warn("Failed to get issue timeline events for {} issue #{}", repositoryId, issue.getNumber(), e);
        }
        return issue.toBuilder()
                .events(events)
                .build();
    }

    public List<GithubIssueEvent> getIssueTimelineEvents(IntegrationKey integrationKey, String repositoryId, int issueNumber, Instant from, Instant to, boolean onboarding) throws GithubClientException {
        GithubClient client = levelOpsClientFactory.get(integrationKey, false);
        // sort is from oldest to newest....
        return client.streamIssueTimelineEvents(repositoryId, issueNumber)
                .filter(event -> onboarding || event.getCreatedAt() != null && event.getCreatedAt().toInstant().isAfter(from))
                .takeWhile(event -> event.getCreatedAt() != null && event.getCreatedAt().toInstant().isBefore(to))
                .map(GithubConverters::parseGithubIssueTimelineEvent)
                .collect(Collectors.toList());
    }

    public List<GithubIssueEvent> getIssueEvents(GithubClient levelopsClient, String repositoryId, Instant from, Instant to) {
        return levelopsClient.streamIssueEvents(repositoryId)
                .filter(event -> event.getCreatedAt() != null && event.getCreatedAt().toInstant().isBefore(to))
                .takeWhile(event -> event.getCreatedAt() != null && event.getCreatedAt().toInstant().isAfter(from))
                .collect(Collectors.toList());
    }
}
