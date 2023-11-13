package io.levelops.integrations.bitbucket_server.services;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.models.ChangeVolumeStats;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClient;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientException;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerCommit;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerCommitDiffInfo;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerFile;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerProject;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class BitbucketServerUtils {
    private static final String TOTAL = "_total_";

    public static BitbucketServerCommit enrichCommit(BitbucketServerClient client,
                                                     BitbucketServerRepository repository,
                                                     BitbucketServerCommit commit, boolean fetchCommitFiles) {
        if (commit == null) {
            return commit;
        }
        //default enrichment
        commit = commit.toBuilder()
                .projectName(repository.getProject().getName())
                .repoName(repository.getName())
                .commitUrl(client.resourceUrl + "/projects/" + repository.getProject().getKey() + "/repos/"
                        + repository.getSlug() + "/commits/" + commit.getId())
                .build();
        if (!fetchCommitFiles) {
            return commit;
        }
        try {
            BitbucketServerCommitDiffInfo commitDiff = client.getCommitDiff(repository.getProject().getKey(), repository.getSlug(), commit.getId());
            Map<String, ChangeVolumeStats> diffStatsMap = extractCommitStats(commitDiff);
            return commit.toBuilder()
                    .additions(diffStatsMap.get(TOTAL).getAdditions())
                    .deletions(diffStatsMap.get(TOTAL).getDeletions())
                    .files(extractFileDetails(commitDiff, diffStatsMap))
                    .build();
        } catch (RuntimeStreamException | BitbucketServerClientException e) {
            log.warn("Failed to enrich repo {}/{} commit {}", repository.getProject().getKey(), repository.getSlug(), commit.getId(), e);
        }
        return commit;
    }

    private static Map<String, ChangeVolumeStats> extractCommitStats(BitbucketServerCommitDiffInfo commitDiff) {
        Map<String, ChangeVolumeStats> diffStatsMap = new HashMap<>();
        int totalAdd = 0;
        int totalDelete = 0;
        List<BitbucketServerCommitDiffInfo.Diff> diffs = commitDiff.getDiffs();
        for (BitbucketServerCommitDiffInfo.Diff diff : diffs) {
            List<BitbucketServerCommitDiffInfo.Diff.Hunk> hunks = diff.getHunks();
            String fileName;
            if (diff.getDestination() != null) {
                fileName = diff.getDestination().getName();
            } else {
                fileName = diff.getSource().getName();
            }
            int add = 0;
            int delete = 0;
            if (CollectionUtils.isNotEmpty(hunks)) {
                add = getStatForFile(hunks, BitbucketServerFetchCommitsService.Action.ADDED.name());
                delete = getStatForFile(hunks, BitbucketServerFetchCommitsService.Action.REMOVED.name());
                totalAdd += add;
                totalDelete += delete;
            }
            diffStatsMap.put(fileName, ChangeVolumeStats.builder().fileName(fileName).additions(add).deletions(delete).build());
        }
        diffStatsMap.put(TOTAL, ChangeVolumeStats.builder().fileName(TOTAL).additions(totalAdd).deletions(totalDelete).build());
        return diffStatsMap;
    }

    private static int getStatForFile(List<BitbucketServerCommitDiffInfo.Diff.Hunk> hunks, String action) {
        int num = 0;
        for (BitbucketServerCommitDiffInfo.Diff.Hunk hunk : hunks) {
            List<BitbucketServerCommitDiffInfo.Diff.Hunk.Segment> segments = hunk.getSegments().stream()
                    .filter(segment -> segment.getType().equalsIgnoreCase(action))
                    .collect(Collectors.toList());
            for (BitbucketServerCommitDiffInfo.Diff.Hunk.Segment segment : segments) {
                num += segment.getLines().size();
            }
        }
        return num;
    }

    private static List<BitbucketServerFile> extractFileDetails(BitbucketServerCommitDiffInfo commitDiff, Map<String, ChangeVolumeStats> diffStatsMap) {
        return commitDiff.getDiffs().stream().map(diff -> {
            String fileName;
            if (diff.getDestination() != null) {
                fileName = diff.getDestination().getName();
            } else {
                fileName = diff.getSource().getName();
            }
            return BitbucketServerFile.builder()
                    .name(fileName)
                    .sourceFile(getFileRef(diff.getSource()))
                    .destinationFile(getFileRef(diff.getDestination()))
                    .linesAdded(diffStatsMap.get(fileName).getAdditions())
                    .linesRemoved(diffStatsMap.get(fileName).getDeletions())
                    .build();
        }).collect(Collectors.toList());
    }

    private static BitbucketServerFile.FileRef getFileRef(BitbucketServerCommitDiffInfo.Diff.FileReference fileRef) {
        if (fileRef == null) {
            return BitbucketServerFile.FileRef.builder().build();
        }
        return BitbucketServerFile.FileRef.builder()
                .components(fileRef.getComponents())
                .name(fileRef.getName())
                .fileExtension(fileRef.getExtension())
                .fileFullName(fileRef.getToString())
                .parent(fileRef.getParent())
                .build();
    }

    public static Stream<ImmutablePair<BitbucketServerProject, BitbucketServerRepository>> fetchRepos(
            BitbucketServerClient client, List<String> repos, List<String> projects
    ) throws BitbucketServerClientException {
        List<String> projectNamesLower = Objects.isNull(projects) ? List.of() : projects.stream().map(String::toLowerCase).collect(Collectors.toList());
        List<String> repoNamesLower = Objects.isNull(repos) ? List.of() : repos.stream().map(String::toLowerCase).collect(Collectors.toList());
        log.info("Fetching repositories from Bitbucket server. Whitelisted repos: {}, Whitelisted projects: {}"
                , repoNamesLower, projectNamesLower);
        return Objects.requireNonNull(client.streamProjects())
                .filter(project -> {
                    if (CollectionUtils.isNotEmpty(projects)) {
                        boolean selected = projectNamesLower.contains(project.getName().toLowerCase());
                        if (selected) {
                            log.info("Selected project from whitelist {}", project.getName());
                        } else {
                            log.info("Skipping project {}", project.getName());
                        }
                        return selected;
                    } else {
                        return true;
                    }
                })
                .flatMap(project -> {
                    Stream<ImmutablePair<BitbucketServerProject, BitbucketServerRepository>> collectRepositories;
                    try {
                        collectRepositories = client.streamRepositories(project.getKey())
                                .filter(repository -> {
                                    if (CollectionUtils.isNotEmpty(repos)) {
                                        boolean selected = repoNamesLower.contains(repository.getName().toLowerCase());
                                        if (selected) {
                                            log.info("Selected repository from whitelist {}", repository.getName());
                                        } else {
                                            log.info("Skipping repository {}", repository.getName());
                                        }
                                        return selected;
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
                });
    }
}
