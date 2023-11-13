package io.levelops.integrations.bitbucket_server.services;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClient;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientException;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerCommit;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerEnrichedProjectData;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerPRActivity;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerProject;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerPullRequest;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class BitbucketServerFetchPullRequestsService {

    private static final int BATCH_SIZE = 1000;
    private static final int MAX_SUB_OBJ_SIZE = 250;

    public Stream<BitbucketServerEnrichedProjectData> fetchPullRequests(BitbucketServerClient client,
                                                                        Long from,
                                                                        Long to,
                                                                        List<String> repos,
                                                                        List<String> projects,
                                                                        boolean fetchPrReviews, boolean fetchPRCommits, boolean fetchCommitFiles) throws NoSuchElementException, BitbucketServerClientException {
        Stream<ImmutablePair<BitbucketServerRepository, BitbucketServerPullRequest>> prDataStream = BitbucketServerUtils.fetchRepos(client, repos, projects)
                .flatMap(pairOfRepoAndPR -> {
                    Stream<ImmutablePair<BitbucketServerRepository, BitbucketServerPullRequest>> enrichedPRs;
                    try {
                        enrichedPRs = client.streamPullRequests(pairOfRepoAndPR.getLeft().getKey(), pairOfRepoAndPR.getRight().getSlug())
                                .filter(pr -> pr.getUpdatedDate() != null && pr.getUpdatedDate() <= to)
                                .takeWhile(pr -> pr.getUpdatedDate() != null && pr.getUpdatedDate() >= from)
                                .map(pr -> enrichPullRequest(client, pairOfRepoAndPR.getLeft(), pairOfRepoAndPR.getRight(), pr, fetchPrReviews, fetchPRCommits, fetchCommitFiles))
                                .map(pr -> ImmutablePair.of(pairOfRepoAndPR.getRight(), pr));
                    } catch (BitbucketServerClientException e) {
                        log.warn("Failed to get pull requests for project {} and repository {}", pairOfRepoAndPR.getLeft().getName(),
                                pairOfRepoAndPR.getRight().getName(), e);
                        throw new RuntimeStreamException("Failed to get pull requests for project " + pairOfRepoAndPR.getLeft().getName()
                                + "and repository " + pairOfRepoAndPR.getRight().getName(), e);
                    }
                    return enrichedPRs;
                });
        return StreamUtils.partition(prDataStream, BATCH_SIZE)
                .flatMap(pairs -> {
                    Map<BitbucketServerRepository, List<ImmutablePair<BitbucketServerRepository, BitbucketServerPullRequest>>> groupedBatchOfRepoPrs = pairs.stream()
                            .collect(Collectors.groupingBy(ImmutablePair::getLeft, Collectors.toList()));
                    return groupedBatchOfRepoPrs.entrySet().stream()
                            .map(entry -> BitbucketServerEnrichedProjectData.builder()
                                    .project(entry.getKey().getProject())
                                    .repository(entry.getKey())
                                    .pullRequests(entry.getValue().stream()
                                            .map(ImmutablePair::getRight)
                                            .collect(Collectors.toList()))
                                    .build());
                });
    }

    private BitbucketServerPullRequest enrichPullRequest(BitbucketServerClient client,
                                                         BitbucketServerProject project,
                                                         BitbucketServerRepository repo,
                                                         BitbucketServerPullRequest pr, boolean fetchPrReviews,
                                                         boolean fetchPRCommits, boolean fetchCommitFiles) {
        List<BitbucketServerPRActivity> prActivities = null;
        String repoSlug = repo.getSlug();
        if (fetchPrReviews) {
            try {
                prActivities = client.streamPrActivities(project.getKey(), repoSlug, pr.getId())
                        .limit(MAX_SUB_OBJ_SIZE)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } catch (BitbucketServerClientException e) {
                log.warn("Failed to fetch pr activities for prId={} in repo {}/{}", pr.getId(), project.getKey(), repoSlug, e);
            }
        }

        List<BitbucketServerCommit> prCommits = null;
        if (fetchPRCommits) {
            try {
                prCommits = client.streamPrCommits(project.getKey(), repoSlug, pr.getId())
                        .limit(MAX_SUB_OBJ_SIZE)
                        .map(commit -> BitbucketServerUtils.enrichCommit(client, repo, commit, fetchCommitFiles))
                        .collect(Collectors.toList());
            } catch (BitbucketServerClientException e) {
                log.warn("Failed to fetch pr commits for prId={} in repo {}/{}", pr.getId(), project.getKey(), repoSlug, e);
            }
        }

        BitbucketServerPullRequest.BitbucketServerPullRequestBuilder bldr = pr.toBuilder();
        if (prCommits != null) {
            bldr.prCommits(prCommits);
        }
        if (prActivities != null) {
            bldr.activities(Objects.requireNonNull(prActivities));
        }

        return bldr.build();
    }
}
