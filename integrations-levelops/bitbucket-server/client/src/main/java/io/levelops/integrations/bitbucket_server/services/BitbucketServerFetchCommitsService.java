package io.levelops.integrations.bitbucket_server.services;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClient;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientException;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerCommit;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerEnrichedProjectData;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class BitbucketServerFetchCommitsService {

    enum Action {
        ADDED, REMOVED
    }

    private static final String TOTAL = "_total_";
    private static final int BATCH_SIZE = 1000;

    public Stream<BitbucketServerEnrichedProjectData> fetchCommits(BitbucketServerClient client,
                                                                   Long from,
                                                                   Long to,
                                                                   List<String> repos,
                                                                   List<String> projects,
                                                                   boolean fetchCommitFiles) throws NoSuchElementException, BitbucketServerClientException {
        Stream<ImmutablePair<BitbucketServerRepository, BitbucketServerCommit>> commitDataStream = BitbucketServerUtils.fetchRepos(client, repos, projects)
                .flatMap(pairOfRepoAndCommit -> {
                    Stream<ImmutablePair<BitbucketServerRepository, BitbucketServerCommit>> enrichedCommits;
                    try {
                        enrichedCommits = client.streamCommits(pairOfRepoAndCommit.getLeft().getKey(), pairOfRepoAndCommit.getRight().getSlug())
                                .filter(commit -> commit.getCommitterTimestamp() != null && commit.getCommitterTimestamp() <= to)
                                .takeWhile(commit -> commit.getCommitterTimestamp() != null && commit.getCommitterTimestamp() >= from)
                                .map(commit -> BitbucketServerUtils.enrichCommit(client, pairOfRepoAndCommit.getRight(), commit, fetchCommitFiles))
                                .map(commit -> ImmutablePair.of(pairOfRepoAndCommit.getRight(), commit));
                    } catch (BitbucketServerClientException e) {
                        log.warn("Failed to get commits for project {} and repository {}", pairOfRepoAndCommit.getLeft().getName(), pairOfRepoAndCommit.getRight().getName(), e);
                        throw new RuntimeStreamException("Failed to get commits for project " + pairOfRepoAndCommit.getLeft().getName() + "and repository " + pairOfRepoAndCommit.getRight().getName(), e);
                    }
                    return enrichedCommits;
                });
        return StreamUtils.partition(commitDataStream, BATCH_SIZE)
                .flatMap(pairs -> {
                    Map<BitbucketServerRepository, List<ImmutablePair<BitbucketServerRepository, BitbucketServerCommit>>> groupedBatchOfRepoAndCommits = pairs.stream()
                            .collect(Collectors.groupingBy(ImmutablePair::getLeft, Collectors.toList()));
                    return groupedBatchOfRepoAndCommits.entrySet().stream()
                            .map(entry -> BitbucketServerEnrichedProjectData.builder()
                                    .project(entry.getKey().getProject())
                                    .repository(entry.getKey())
                                    .commits(entry.getValue().stream()
                                            .map(ImmutablePair::getRight)
                                            .collect(Collectors.toList()))
                                    .build());
                });
    }
}
