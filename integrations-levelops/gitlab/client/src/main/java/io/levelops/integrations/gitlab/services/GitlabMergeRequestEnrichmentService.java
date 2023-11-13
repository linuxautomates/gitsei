package io.levelops.integrations.gitlab.services;

import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.models.GitlabCommit;
import io.levelops.integrations.gitlab.models.GitlabEvent;
import io.levelops.integrations.gitlab.models.GitlabMergeRequest;
import io.levelops.integrations.gitlab.models.GitlabMergeRequestChanges;
import io.levelops.integrations.gitlab.models.GitlabProject;
import io.levelops.integrations.gitlab.models.GitlabStateEvent;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * This class can be used for enriching a {@link GitlabMergeRequest}.
 */
@Log4j2
public class GitlabMergeRequestEnrichmentService {
    public Stream<GitlabCommit> getMRCommits(GitlabClient client, GitlabProject project,
                                             GitlabMergeRequest gitlabMergeRequest, int perPage) {
        final String projectId = project.getId();
        final String mergeRequestId = gitlabMergeRequest.getIid();
        MutableInt commitsCount = new MutableInt(0);
        return client.streamMRCommit(projectId, mergeRequestId, perPage)
                .filter(Objects::nonNull)
                .peek(commit -> {
                    commitsCount.increment();
                    if (commitsCount.getValue() % 100 == 0) {
                        log.info("Processed Commits for projectId={}: mergeRequest={}:" +
                                        " commitsCount={} (ts={})", projectId, mergeRequestId, commitsCount.getValue(),
                                commit.getCommittedDate());
                    }
                });
    }

    public Stream<GitlabStateEvent> getMRStateEvents(GitlabClient client, GitlabProject project,
                                                     GitlabMergeRequest mergeRequest, int perPage) {
        final String projectId = project.getId();
        final String mergeRequestIID = mergeRequest.getIid();
        MutableInt stateEventsCount = new MutableInt(0);
        return client.streamMRStateEvent(projectId, mergeRequestIID, perPage)
                .filter(Objects::nonNull)
                .peek(stateEvent -> {
                    stateEventsCount.increment();
                    if (stateEventsCount.getValue() % 50 == 0) {
                        log.info("Processed state events  for projectId={}: mergeRequestIID={}:" +
                                " commitsCount={} ", projectId, mergeRequestIID, stateEventsCount.getValue());
                    }
                });
    }

    @Deprecated(since="This loops through ALL project level events and is incredibly wasteful")
    public Stream<GitlabEvent> getMREvents(GitlabClient client, GitlabProject project,
                                           GitlabMergeRequest mergeRequest, int perPage) {
        final String projectId = project.getId();
        final String mergeRequestIID = mergeRequest.getIid();
        MutableInt eventsCount = new MutableInt(0);
        return client.streamMREvent(projectId, perPage)
                .filter(Objects::nonNull)
                .filter(event -> event.getTargetIid().equals(mergeRequestIID))
                .peek(MREvent -> {
                    eventsCount.increment();
                    if (eventsCount.getValue() % 50 == 0) {
                        log.info("Processed events  for projectId={}:" +
                                " eventsCount={} ", projectId, eventsCount.getValue());
                    }
                });
    }

    public Stream<GitlabEvent> getMRCommentEvents(GitlabClient client, GitlabProject project,
                                                  GitlabMergeRequest mergeRequest, int perPage) {
        final String projectId = project.getId();
        final String mergeRequestIID = mergeRequest.getIid();
        MutableInt eventsCount = new MutableInt(0);
        return client.streamMRCommentEvents(projectId, mergeRequestIID, perPage)
                .peek(MREvent -> {
                    eventsCount.increment();
                    if (eventsCount.getValue() % 5 == 0) {
                        log.info("Processed comment events  for projectId={}:" +
                                ", merge request={}, eventsCount={} ", projectId, mergeRequestIID, eventsCount.getValue());
                    }
                });
    }

    public GitlabMergeRequestChanges getMRChanges(GitlabClient client, GitlabProject project,
                                                  GitlabMergeRequest mergeRequest) {
        final String projectId = project.getId();
        final String mergeRequestIid = mergeRequest.getIid();
        return client.getMRChanges(projectId, mergeRequestIid).orElse(null);
    }
}
