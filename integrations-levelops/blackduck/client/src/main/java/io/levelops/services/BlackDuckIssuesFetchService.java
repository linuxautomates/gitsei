package io.levelops.services;


import com.google.common.collect.Iterators;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.integrations.blackduck.BlackDuckClient;
import io.levelops.integrations.blackduck.BlackDuckClientException;
import io.levelops.integrations.blackduck.models.*;
import io.levelops.integrations.blackduck.utils.BlackDuckUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class BlackDuckIssuesFetchService {

    private static final int BATCH_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 10;

    public Stream<EnrichedProjectData> fetch(BlackDuckClient client, BlackDuckIterativeScanQuery query) {
        var entryStream =
                Objects.requireNonNull(streamProjects(client))
                        .flatMap(project -> {
                            Stream<ImmutablePair<BlackDuckProject, BlackDuckVersion>> versionStream =
                                    streamVersions(client, project)
                                            .map(version -> ImmutablePair.of(project, version));
                            return versionStream;
                        })
                        .filter(projectVersion -> DateUtils.isBetween(projectVersion.getRight().getSettingUpdatedAt(),
                                query.getFrom(), true, query.getTo(), true))
                        .flatMap(pair -> {
                            Stream<BlackDuckIssue> issueStream = streamIssues(client, pair.getLeft(), pair.getRight());
                            return Stream.of(ImmutablePair.of(ImmutablePair.of(pair.left, pair.right), issueStream.collect(Collectors.toList())));
                        });
        Stream<EnrichedProjectData> enrichedDataStream = StreamUtils.partition(entryStream, BATCH_SIZE)
                .flatMap(pairs -> {
                    Stream<EnrichedProjectData> enrichedProjectDataStream = pairs.stream().map(pair -> EnrichedProjectData.builder()
                            .project(pair.getKey().getLeft())
                            .version(pair.getKey().getRight())
                            .issues(pair.getRight())
                            .build());
                    return enrichedProjectDataStream;
                });
        return enrichedDataStream;
    }

    private Stream<BlackDuckProject> streamProjects(BlackDuckClient client) {
        try {
            return client.getProjects().stream();
        } catch (BlackDuckClientException e) {
            log.error("streamProjects: Encountered BlackDuckClient client error " +
                    "while fetching projects", e);
            return Stream.empty();
        }
    }

    private Stream<BlackDuckVersion> streamVersions(BlackDuckClient client, BlackDuckProject project) {
        try {
            return client.getVersions(BlackDuckUtils.extractIdFromProject(project)).stream();
        } catch (BlackDuckClientException e) {
            log.error("streamVersions: Encountered BlackDuck client error " +
                    "while fetching versions for project " + project.getProjectName(), e);
            return Stream.empty();
        }
    }

    private Stream<BlackDuckIssue> streamIssues(BlackDuckClient client, BlackDuckProject project, BlackDuckVersion version) {
        return PaginationUtils.stream(0, DEFAULT_PAGE_SIZE, (offset) -> {
            try {
                return client.getIssues(BlackDuckUtils.extractIdFromProject(project),
                        BlackDuckUtils.extractIdFromVersion(version), offset);
            } catch (BlackDuckClientException e) {
                log.error("streamIssues: Encountered BlackDuck client error " +
                        "while fetching issues for project " + project.getProjectName(), e);
                return List.of();
            }
        });
    }
}
