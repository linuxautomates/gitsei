package io.levelops.integrations.bitbucket.services;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.integrations.bitbucket.client.BitbucketClient;
import io.levelops.integrations.bitbucket.models.BitbucketCommit;
import io.levelops.integrations.bitbucket.models.BitbucketCommitDiffStat;
import io.levelops.integrations.bitbucket.models.BitbucketRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.mutable.MutableInt;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class BitbucketFetchCommitsService {
    private static final int BATCH_SIZE = 200;

    public Stream<BitbucketRepository> getRepoCommits(BitbucketClient client, BitbucketRepository repo, Instant from, Instant to, boolean fetchCommitFiles) {
        final String workspaceSlug = repo.getWorkspaceSlug();
        final String repoId = repo.getUuid();

        MutableInt commitsCount = new MutableInt(0);
        MutableInt diffStatsCount = new MutableInt(0);
        Stream<BitbucketCommit> commitStream = client.streamRepoCommits(workspaceSlug, repoId)
                .filter(c -> c.getDate() != null && c.getDate().toInstant().isBefore(to))
                .takeWhile(c -> c.getDate() != null && c.getDate().toInstant().isAfter(from))
                .map(c -> enrichCommit(client, workspaceSlug, repoId, c, fetchCommitFiles))
                .filter(Objects::nonNull)
                .peek(c -> {
                    commitsCount.increment();
                    diffStatsCount.add(CollectionUtils.size(c.getDiffStats()));
                    if (commitsCount.getValue() % 50 == 0) {
                        log.info("Processed Commits for workspaceSlug={}, repo={}: commitsCount={} diffStatsCount={} (ts={})", workspaceSlug, repoId, commitsCount.getValue(), diffStatsCount.getValue(), c.getDate());
                    }
                });

        return StreamUtils.partition(commitStream, BATCH_SIZE)
                .map(batch -> repo.toBuilder()
                        .commits(batch)
                        .build());
    }

    private BitbucketCommit enrichCommit(BitbucketClient client, String workspaceSlug, String repoId, BitbucketCommit c, Boolean fetchCommitFiles) {
        if (c == null || c.getHash() == null) {
            return c;
        }
        if (!fetchCommitFiles) {
            return c;
        }
        try {
            List<BitbucketCommitDiffStat> diffStats = client.streamRepoCommitDiffSets(workspaceSlug, repoId, c.getHash())
                    .collect(Collectors.toList());
            return c.toBuilder()
                    .diffStats(diffStats)
                    .build();
        } catch (RuntimeStreamException e) {
            log.warn("Failed to enrich repo {}/{} commit {} with diff set", workspaceSlug, repoId, c.getHash(), e);
        }
        return c;
    }
}
