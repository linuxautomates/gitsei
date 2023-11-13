package io.levelops.integrations.bitbucket_server.services;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClient;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientException;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerEnrichedProjectData;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerProject;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerRepository;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerTag;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class BitbucketServerFetchTagsService {

    private static final int BATCH_SIZE = 1000;

    public Stream<BitbucketServerEnrichedProjectData> fetchTags(BitbucketServerClient client, List<String> repos) throws NoSuchElementException, BitbucketServerClientException {
        Stream<ImmutablePair<BitbucketServerRepository, BitbucketServerTag>> tagDataStream = Objects.requireNonNull(client.streamProjects())
                .flatMap(project -> {
                    Stream<ImmutablePair<BitbucketServerProject, BitbucketServerRepository>> collectRepositories;
                    try {
                        collectRepositories = client.streamRepositories(project.getKey())
                                .filter(repository -> {
                                    if (CollectionUtils.isNotEmpty(repos)) {
                                        return repos.contains(repository.getName());
                                    } else {
                                        return true;
                                    }
                                })
                                .map(repository -> ImmutablePair.of(project, repository));
                    } catch (BitbucketServerClientException e) {
                        log.warn("Failed to get repositories for project {}", project.getName(), e);
                        throw new RuntimeStreamException("Failed to get repositories for project " + project.getName(), e);
                    }
                    return collectRepositories;
                })
                .flatMap(pairOfRepoAndTag -> {
                    Stream<ImmutablePair<BitbucketServerRepository, BitbucketServerTag>> tags;
                    try {
                        tags = client.streamTags(pairOfRepoAndTag.getLeft().getKey(), pairOfRepoAndTag.getRight().getSlug())
                                .map(pr -> ImmutablePair.of(pairOfRepoAndTag.getRight(), pr));
                    } catch (BitbucketServerClientException e) {
                        log.warn("Failed to get tag for project {} and repository {}", pairOfRepoAndTag.getLeft().getName(),
                                pairOfRepoAndTag.getRight().getName(), e);
                        throw new RuntimeStreamException("Failed to get tags for project " + pairOfRepoAndTag.getLeft().getName()
                                + "and repository " + pairOfRepoAndTag.getRight().getName(), e);
                    }
                    return tags;
                });
        return StreamUtils.partition(tagDataStream, BATCH_SIZE)
                .flatMap(pairs -> {
                    Map<BitbucketServerRepository, List<ImmutablePair<BitbucketServerRepository, BitbucketServerTag>>> groupedBatchOfRepoPrs = pairs.stream()
                            .collect(Collectors.groupingBy(ImmutablePair::getLeft, Collectors.toList()));
                    return groupedBatchOfRepoPrs.entrySet().stream()
                            .map(entry -> BitbucketServerEnrichedProjectData.builder()
                                    .project(entry.getKey().getProject())
                                    .repository(entry.getKey())
                                    .tags(entry.getValue().stream()
                                            .map(ImmutablePair::getRight)
                                            .collect(Collectors.toList()))
                                    .build());
                });
    }
}