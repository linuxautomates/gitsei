package io.levelops.integrations.bitbucket.services;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.integrations.bitbucket.client.BitbucketClient;
import io.levelops.integrations.bitbucket.models.BitbucketPullRequest;
import io.levelops.integrations.bitbucket.models.BitbucketPullRequestActivity;
import io.levelops.integrations.bitbucket.models.BitbucketRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.mutable.MutableInt;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class BitbucketFetchPullRequestsService {

    private static final int PR_BATCH_SIZE = 50;
    private static final int PR_ACTIVITY_LIMIT = 10000;

    public Stream<BitbucketRepository> getRepoPrs(BitbucketClient client, BitbucketRepository repo, Instant from, Instant to, boolean fetchPrReviews) {
        final String workspaceSlug = repo.getWorkspaceSlug();
        final String repoId = repo.getUuid();

        MutableInt prsCount = new MutableInt(0);
        Stream<BitbucketPullRequest> prStream = client.streamPullRequests(workspaceSlug, repoId, from, to)
                .filter(Objects::nonNull)
                .map(pullRequest -> enrichPullRequest(client, workspaceSlug, repoId, pullRequest, fetchPrReviews))
                .peek(c -> {
                    prsCount.increment();
                    if (prsCount.getValue() % 50 == 0) {
                        log.info("Processed PRs for account={}, repo={}: PRsCount={}", workspaceSlug, repoId, prsCount.getValue());
                    }
                });

        return StreamUtils.partition(prStream, PR_BATCH_SIZE)
                .map(batch -> repo.toBuilder()
                        .pullRequests(batch)
                        .build());
    }

    private BitbucketPullRequest enrichPullRequest(BitbucketClient client, String workspaceSlug, String repoId, BitbucketPullRequest pullRequest, boolean fetchPrReviews) {
        List<BitbucketPullRequestActivity> approvalsResponse = null;

        if (fetchPrReviews) {
            try {
                approvalsResponse = client.streamPullRequestActivity(workspaceSlug, repoId, String.valueOf(pullRequest.getId()))
                        .limit(PR_ACTIVITY_LIMIT)
                        .collect(Collectors.toList());
            } catch (RuntimeStreamException e) {
                log.warn("Failed to fetch pull request activity for repo={}/{}, prId={}", workspaceSlug, repoId, pullRequest.getId(), e);
            }
        }

        return pullRequest.toBuilder()
                .approvals(approvalsResponse)
                .build();
    }
}
