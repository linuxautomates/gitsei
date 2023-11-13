package io.levelops.integrations.gitlab.services;

import io.levelops.commons.functional.StreamUtils;
import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.client.GitlabClientException;
import io.levelops.integrations.gitlab.models.GitlabCommit;
import io.levelops.integrations.gitlab.models.GitlabEvent;
import io.levelops.integrations.gitlab.models.GitlabMergeRequest;
import io.levelops.integrations.gitlab.models.GitlabMergeRequestChanges;
import io.levelops.integrations.gitlab.models.GitlabProject;
import io.levelops.integrations.gitlab.models.GitlabStateEvent;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Date;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.integrations.gitlab.services.GitlabFetchMergeRequestsService.MREnrichment.*;

/**
 * This class can be used for enriching {@link GitlabMergeRequest}
 */
@Log4j2
public class GitlabFetchMergeRequestsService {

    private static final int MAX_SUB_OBJECTS = 250;
    private static final int MAX_STATE_EVENTS_TO_EVENT_CONVERSIONS = 50;
    private GitlabMergeRequestEnrichmentService enrichmentService;
    private static final EnumSet<MREnrichment> ENRICHMENTS = EnumSet.of(COMMITS, STATE_EVENTS, CHANGES, EVENTS);

    public Stream<GitlabProject> getProjectMrs(
            GitlabClient client,
            GitlabProject project,
            Date from,
            Date to,
            int perPage,
            boolean fetchPrPatches,
            boolean fetchStateEvents,
            int prCommitsLimit) {
        enrichmentService = new GitlabMergeRequestEnrichmentService();
        final String projectId = project.getId();
        MutableInt mrsCount = new MutableInt(0);
        Stream<GitlabMergeRequest> mergeRequests = client.streamMergeRequests(projectId, from, to, perPage)
                .filter(Objects::nonNull)
                .map(gitlabMergeRequest -> {
                    try {
                        return parseAndEnrichProjectMR(client, project, gitlabMergeRequest, perPage, fetchPrPatches, fetchStateEvents, prCommitsLimit);
                    } catch (GitlabClientException e) {
                        log.error("failed to get merge requests for projects {}", project.getId(), e);
                    }
                    return null;
                }).filter(Objects::nonNull)
                .peek(c -> {
                    mrsCount.increment();
                    if (mrsCount.getValue() % 50 == 0) {
                        log.info("Processed MRS for project={}: MRsCount={}", projectId,
                                mrsCount.getValue());
                    }
                });
        return StreamUtils.partition(mergeRequests, 10)
                .map(batch -> project.toBuilder()
                        .mergeRequests(batch)
                        .build());
    }

    private GitlabMergeRequest parseAndEnrichProjectMR(GitlabClient client,
                                                       GitlabProject project,
                                                       GitlabMergeRequest mergeRequest,
                                                       int perPage,
                                                       boolean fetchPrPatches,
                                                       boolean fetchStateEvents,
                                                       int prCommitsLimit) throws GitlabClientException {
        Stream<GitlabCommit> commits = Stream.empty();
        Stream<GitlabStateEvent> stateEvents = Stream.empty();
        Stream<GitlabEvent> gitlabEvents = Stream.empty();
        GitlabMergeRequestChanges changes = null;

        if (ENRICHMENTS.contains(COMMITS)) {
            commits = enrichmentService.getMRCommits(client, project, mergeRequest, perPage);
        }
        // These events are not processed by aggs/ETL right now
        if (fetchStateEvents && ENRICHMENTS.contains(STATE_EVENTS)) {
            stateEvents = enrichmentService.getMRStateEvents(client, project, mergeRequest, perPage);
        }

        // MR changes are not processed by aggs/ETL right now
        boolean isClosed = mergeRequest.getMergeStatus().equalsIgnoreCase("closed");
        if (!isClosed && fetchPrPatches && ENRICHMENTS.contains(CHANGES)) {
            changes = enrichmentService.getMRChanges(client, project, mergeRequest);
        }
        if (ENRICHMENTS.contains(EVENTS)) {
            // When processing events in ETL - we only care about: "commented on", "approved" and "closed" event types
            // MRCommentEvents gets us the "commented on" and "approved" events, and we get the "closed" events from
            // the state events endpoint

            // NOTE: Do not use the project level events because that can be a very expensive operation since it gets
            // all event for the entire project
            Stream<GitlabEvent> mrCommentEvents = enrichmentService.getMRCommentEvents(client, project, mergeRequest, perPage);
            Stream<GitlabEvent> gitlabStateEventStream = enrichmentService.getMRStateEvents(client, project, mergeRequest, perPage)
                    // This is defensive because we don't have enough data points for how large this can get, but should be small usually
                    .limit(MAX_STATE_EVENTS_TO_EVENT_CONVERSIONS)
                    .map(GitlabStateEvent::toEvent)
                    .filter(Optional::isPresent)
                    .map(Optional::get);
            gitlabEvents = Stream.concat(gitlabStateEventStream, mrCommentEvents);
        }
        return mergeRequest.toBuilder()
                .commits(commits.limit(prCommitsLimit).collect(Collectors.toList()))
                .stateEvents(stateEvents.limit(MAX_SUB_OBJECTS).collect(Collectors.toList()))
                .mergeRequestEvents(gitlabEvents.limit(MAX_SUB_OBJECTS).collect(Collectors.toList()))
                .mergeRequestChanges(changes)
                .build();
    }

    public enum MREnrichment {
        COMMITS, STATE_EVENTS, CHANGES, EVENTS
    }
}
