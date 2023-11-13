package io.levelops.integrations.awsdevtools.services;

import com.amazonaws.services.codebuild.model.*;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.awsdevtools.client.AWSDevToolsClient;
import io.levelops.integrations.awsdevtools.client.AWSDevToolsClientException;
import io.levelops.integrations.awsdevtools.models.CBBuild;
import io.levelops.integrations.awsdevtools.models.CBBuildBatch;
import io.levelops.integrations.awsdevtools.models.CBReport;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

/**
 * This class can be used for enriching {@link List<Build>} and {@link List<BuildBatch>}.
 * A {@link ForkJoinPool} is maintained for performing all
 * the enrichment tasks. The {@link ForkJoinPool} is shared across all the jobs.
 */
@Log4j2
public class AWSDevToolsEnrichmentService {

    private final int forkThreshold;
    private final ForkJoinPool pool;

    /**
     * all arg constructor
     *
     * @param threadCount   the number of threads for the {@link ForkJoinPool}
     * @param forkThreshold the max number of projects & test plans to be enriched
     *                      by each{@link EnrichBuildTask} & {@link EnrichBuildBatchTask}
     */
    public AWSDevToolsEnrichmentService(int threadCount, int forkThreshold) {
        this.forkThreshold = forkThreshold;
        this.pool = new ForkJoinPool(threadCount);
    }

    /**
     * call this method to enrich {@link Build}
     *
     * @param client         {@link AWSDevToolsClient} used to make calls to code build
     * @param integrationKey {@link IntegrationKey} for the job, used for logging purposes
     * @param region         {@link String} aws region to be enriched with build batch
     * @param builds         {@link List<Build>} to be enriched
     * @return {@link List<CBBuild>} enriched builds
     */
    public List<CBBuild> enrichBuilds(AWSDevToolsClient client, IntegrationKey integrationKey,
                                      String region, List<Build> builds) {
        EnrichBuildTask enrichBuildTask = new EnrichBuildTask(client, builds, region, forkThreshold);
        log.info("enrichBuilds: started enriching {} builds for {}", builds.size(), integrationKey);
        return pool.invoke(enrichBuildTask);
    }

    /**
     * call this method to enrich {@link BuildBatch}
     *
     * @param client         {@link AWSDevToolsClient} used to make calls to code build
     * @param integrationKey {@link IntegrationKey} for the job, used for logging purposes
     * @param region         {@link String} aws region to be enriched with build
     * @param buildBatches   {@link List<BuildBatch>} to be enriched
     * @return {@link List<CBBuildBatch>} enriched build batches
     */
    public List<CBBuildBatch> enrichBuildBatches(AWSDevToolsClient client, IntegrationKey integrationKey,
                                                 String region, List<BuildBatch> buildBatches) {
        EnrichBuildBatchTask enrichBuildBatchTask = new EnrichBuildBatchTask(client, buildBatches, region, forkThreshold);
        log.info("enrichBuildBatches: started enriching {} build batches for {}", buildBatches.size(), integrationKey);
        return pool.invoke(enrichBuildBatchTask);
    }

    /**
     * Implementation of the {@link RecursiveTask} for enriching a {@link List<Build>}
     */
    @Log4j2
    static class EnrichBuildTask extends RecursiveTask<List<CBBuild>> {

        private final AWSDevToolsClient client;
        private final List<Build> builds;
        private final String region;
        private final int forkThreshold;

        /**
         * all arg constructor
         *
         * @param client        {@link AWSDevToolsClient} used to make calls to code build
         * @param builds        {@link List<Build>} to be enriched
         * @param region        {@link String} aws region to be enriched with build
         * @param forkThreshold {@link IntegrationKey} for the job, used for logging purposes
         */
        public EnrichBuildTask(AWSDevToolsClient client, List<Build> builds, String region, int forkThreshold) {
            this.client = client;
            this.builds = builds;
            this.region = region;
            this.forkThreshold = forkThreshold;
        }

        /**
         * The computation to be performed by this task.
         * Forks the task if {@link EnrichBuildTask#builds} has more than
         * {@link EnrichBuildTask#forkThreshold} builds
         *
         * @return {@link List<CBBuild>} enriched builds
         */
        @Override
        protected List<CBBuild> compute() {
            if (builds.size() > forkThreshold) {
                return computeInSubTask();
            } else {
                return enrichBuilds();
            }
        }

        /**
         * Creates and executes {@link EnrichBuildTask} sub-tasks and
         * then joins the results returned from the sub-tasks
         *
         * @return {@link List<CBBuild>} enriched builds
         */
        private List<CBBuild> computeInSubTask() {
            int size = builds.size();
            EnrichBuildTask enrichBuildTask1 = new EnrichBuildTask(client, builds.subList(0, size / 2),
                    region, forkThreshold);
            EnrichBuildTask enrichBuildTask2 = new EnrichBuildTask(client, builds.subList(size / 2, size),
                    region, forkThreshold);
            enrichBuildTask1.fork();
            enrichBuildTask2.fork();
            List<CBBuild> cbBuilds = new ArrayList<>(enrichBuildTask1.join());
            cbBuilds.addAll(enrichBuildTask2.join());
            return cbBuilds;
        }

        /**
         * Enriches each {@link Build}
         *
         * @return {@link List<CBBuild>} enriched builds
         */
        private List<CBBuild> enrichBuilds() {
            List<CBBuild> cbBuilds = this.builds
                    .stream()
                    .map(this::enrichBuild)
                    .collect(Collectors.toList());
            log.debug("process: enriched {} builds", cbBuilds.size());
            return cbBuilds;
        }

        /**
         * Enriches {@code build} with projectName and reports
         *
         * @param build the {@link Build} to be enriched
         * @return {@link CBBuild} the enriched build
         */
        private CBBuild enrichBuild(Build build) {
            List<Project> projects;
            try {
                projects = client.getProjects(List.of(build.getProjectName()));
            } catch (AWSDevToolsClientException e) {
                log.error("process: encountered client exception while fetching the project "
                        + e.getMessage(), e);
                return CBBuild.builder()
                        .build(build)
                        .region(region)
                        .build();
            }
            if (CollectionUtils.isEmpty(build.getReportArns())) {
                return CBBuild.builder()
                        .build(build)
                        .projectArn(Objects.requireNonNull(projects.stream().findFirst().orElse(null)).getArn())
                        .region(region)
                        .build();
            }
            List<Report> reports;
            try {
                reports = client.getReports(build.getReportArns());
            } catch (AWSDevToolsClientException e) {
                log.error("process: encountered client exception while fetching the reports "
                        + e.getMessage(), e);
                return CBBuild.builder()
                        .build(build)
                        .region(region)
                        .build();
            }
            List<CBReport> cbReports = reports.stream()
                    .map(this::enrichReport)
                    .collect(Collectors.toList());
            return CBBuild.builder()
                    .build(build)
                    .projectArn(Objects.requireNonNull(projects.stream().findFirst().orElse(null)).getArn())
                    .reports(cbReports)
                    .region(region)
                    .build();
        }

        /**
         * Enriches {@code report} with {@link ReportGroup}, {@link List<TestCase>}
         *
         * @param report the {@link Report} to be enriched
         * @return {@link CBReport} the enriched report
         */
        private CBReport enrichReport(Report report) {
            List<ReportGroup> reportGroups;
            try {
                reportGroups = client.getReportGroups(List.of(
                        report.getReportGroupArn()));
            } catch (AWSDevToolsClientException e) {
                log.error("process: encountered client exception while fetching the report group "
                        + e.getMessage(), e);
                return CBReport.builder()
                        .report(report)
                        .build();
            }
            List<TestCase> testCases;
            try {
                testCases = client.getTestCase(report.getArn());
            } catch (AWSDevToolsClientException e) {
                log.error("process: encountered client exception while fetching the test cases "
                        + e.getMessage(), e);
                return CBReport.builder()
                        .report(report)
                        .reportGroup(reportGroups.stream().findFirst().orElse(null))
                        .build();
            }
            return CBReport.builder()
                    .report(report)
                    .reportGroup(reportGroups.stream().findFirst().orElse(null))
                    .testCases(testCases)
                    .build();
        }
    }

    /**
     * Implementation of the {@link RecursiveTask} for enriching a {@link List<BuildBatch>}
     */
    @Log4j2
    static class EnrichBuildBatchTask extends RecursiveTask<List<CBBuildBatch>> {

        private final AWSDevToolsClient client;
        private final List<BuildBatch> buildBatches;
        private final String region;
        private final int forkThreshold;

        /**
         * all arg constructor
         *
         * @param client        {@link AWSDevToolsClient} used to make calls to code build
         * @param buildBatches  {@link List<BuildBatch>} to be enriched
         * @param region        {@link String} aws region to be enriched with build batch
         * @param forkThreshold {@link IntegrationKey} for the job, used for logging purposes
         */
        public EnrichBuildBatchTask(AWSDevToolsClient client, List<BuildBatch> buildBatches, String region, int forkThreshold) {
            this.client = client;
            this.buildBatches = buildBatches;
            this.region = region;
            this.forkThreshold = forkThreshold;
        }

        /**
         * The computation to be performed by this task.
         * Forks the task if {@link EnrichBuildBatchTask#buildBatches} has more than
         * {@link EnrichBuildBatchTask#forkThreshold} buildBatches
         *
         * @return {@link List<CBBuildBatch>} enriched build batches
         */
        @Override
        protected List<CBBuildBatch> compute() {
            if (buildBatches.size() > forkThreshold) {
                return computeInSubTask();
            } else {
                return enrichBuildBatches();
            }
        }

        /**
         * Creates and executes {@link EnrichBuildBatchTask} sub-tasks and
         * then joins the results returned from the sub-tasks
         *
         * @return {@link List<CBBuildBatch>} enriched build batches
         */
        private List<CBBuildBatch> computeInSubTask() {
            int size = buildBatches.size();
            EnrichBuildBatchTask enrichBuildBatchTask1 = new EnrichBuildBatchTask(client, buildBatches.subList(0, size / 2),
                    region, forkThreshold);
            EnrichBuildBatchTask enrichBuildBatchTask2 = new EnrichBuildBatchTask(client, buildBatches.subList(size / 2, size),
                    region, forkThreshold);
            enrichBuildBatchTask1.fork();
            enrichBuildBatchTask2.fork();
            List<CBBuildBatch> cbBuildBatches = new ArrayList<>(enrichBuildBatchTask1.join());
            cbBuildBatches.addAll(enrichBuildBatchTask2.join());
            return cbBuildBatches;
        }

        /**
         * Enriches each {@link BuildBatch}
         *
         * @return {@link List<CBBuildBatch>} enriched build batches
         */
        private List<CBBuildBatch> enrichBuildBatches() {
            List<CBBuildBatch> cbBuildBatches = this.buildBatches
                    .stream()
                    .map(this::enrichBuildBatch)
                    .collect(Collectors.toList());
            log.debug("process: enriched {} build batches", cbBuildBatches.size());
            return cbBuildBatches;
        }

        /**
         * Enriches {@code buildBatch} with projectName
         *
         * @param buildBatch the {@link BuildBatch} to be enriched
         * @return {@link CBBuildBatch} the enriched build batch
         */
        private CBBuildBatch enrichBuildBatch(BuildBatch buildBatch) {
            List<Project> projects;
            try {
                projects = client.getProjects(List.of(buildBatch.getProjectName()));
            } catch (AWSDevToolsClientException e) {
                log.error("process: encountered client exception while fetching the project "
                        + e.getMessage(), e);
                return CBBuildBatch.builder()
                        .buildBatch(buildBatch)
                        .region(region)
                        .build();
            }
            return CBBuildBatch.builder()
                    .buildBatch(buildBatch)
                    .projectArn(Objects.requireNonNull(projects.stream().findFirst().orElse(null)).getArn())
                    .region(region)
                    .build();
        }
    }
}
