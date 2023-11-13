package io.levelops.integrations.circleci.services;

import io.levelops.integrations.circleci.client.CircleCIClient;
import io.levelops.integrations.circleci.client.CircleCIClientException;
import io.levelops.integrations.circleci.models.CircleCIBuild;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

/**
 * This class can be used for enriching a {@link List<CircleCIBuild>}. A {@link ForkJoinPool} is maintained for performing all
 * the enrichment tasks. The {@link ForkJoinPool} is shared across all the jobs.
 */
@Log4j2
public class CircleCIBuildEnrichmentService {

    private final int forkThreshold;
    private final ForkJoinPool pool;

    public CircleCIBuildEnrichmentService(int threadCount, int forkThreshold) {
        this.forkThreshold = forkThreshold;
        this.pool = new ForkJoinPool(threadCount);
    }

    public List<CircleCIBuild> enrichBuilds(CircleCIClient client, List<CircleCIBuild> recentBuilds) {
        EnrichBuildTask task = new EnrichBuildTask(client, recentBuilds, forkThreshold);
        return pool.invoke(task);
    }

    private static class EnrichBuildTask extends RecursiveTask<List<CircleCIBuild>> {

        private final CircleCIClient client;
        private final List<CircleCIBuild> builds;
        private final int forkThreshold;

        EnrichBuildTask(CircleCIClient client, List<CircleCIBuild> builds, int forkThreshold) {
            this.client = client;
            this.builds = builds;
            this.forkThreshold = forkThreshold;
        }

        @Override
        protected List<CircleCIBuild> compute() {
            if (builds.size() > forkThreshold) {
                return computeInSubTask();
            } else {
                return enrichBuilds();
            }
        }

        private List<CircleCIBuild> computeInSubTask() {
            int size = builds.size();
            EnrichBuildTask enrichBuildSubTask1 = new EnrichBuildTask(client, builds.subList(0, size / 2), forkThreshold);
            EnrichBuildTask enrichBuildSubTask2 = new EnrichBuildTask(client, builds.subList(size / 2, size), forkThreshold);
            enrichBuildSubTask1.fork();
            enrichBuildSubTask2.fork();
            List<CircleCIBuild> enrichedBuilds = new ArrayList<>(enrichBuildSubTask1.join());
            enrichedBuilds.addAll(enrichBuildSubTask2.join());
            return enrichedBuilds;
        }

        private List<CircleCIBuild> enrichBuilds() {
            List<CircleCIBuild> enrichedBuilds = this.builds.stream()
                    .map(this::enrichBuild)
                    .collect(Collectors.toList());
            log.debug("process: enriched {} builds", enrichedBuilds.size());
            return enrichedBuilds;
        }

        private CircleCIBuild enrichBuild(CircleCIBuild build) {
            try {
                int buildNumber = build.getBuildNumber();
                String slug = build.getSlug();
                CircleCIBuild fetchedBuild = client.getBuild(slug, buildNumber);
                return build.toBuilder()
                        .steps(fetchedBuild.getSteps())
                        .scmInfoList(fetchedBuild.getScmInfoList())
                        .build();
            } catch (CircleCIClientException e) {
                log.error("process: encountered client exception while enriching job or build"
                        + e.getMessage(), e);
                return build;
            }
        }
    }
}
