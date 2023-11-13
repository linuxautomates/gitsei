package io.levelops.integrations.bitbucket.services;

import io.levelops.commons.functional.StreamUtils;
import io.levelops.integrations.bitbucket.client.BitbucketClient;
import io.levelops.integrations.bitbucket.models.BitbucketRepository;
import io.levelops.integrations.bitbucket.models.BitbucketTag;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Log4j2
public class BitbucketFetchTagsService {

    private static final int BATCH_SIZE = 250;

    public Stream<BitbucketRepository> getRepoTags(BitbucketClient client, BitbucketRepository repo, Instant from, Instant to) {
        final String workspaceSlug = repo.getWorkspaceSlug();
        final String repoName = repo.getName();

        Stream<BitbucketTag> tags = client.streamTags(workspaceSlug, repoName, true)
                .filter(c -> c.getDate() != null && c.getDate().toInstant().isBefore(to))
                .takeWhile(c -> c.getDate() != null && c.getDate().toInstant().isAfter(from))
                .filter(Objects::nonNull);
        return StreamUtils.partition(tags, BATCH_SIZE)
                .map(batch -> repo.toBuilder()
                        .tags(batch)
                        .build());
    }
}