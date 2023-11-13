package io.levelops.integrations.gitlab.services;

import io.levelops.commons.functional.StreamUtils;
import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.client.GitlabClientException;
import io.levelops.integrations.gitlab.models.GitlabJob;
import io.levelops.integrations.gitlab.models.GitlabPipeline;
import io.levelops.integrations.gitlab.models.GitlabProject;
import io.levelops.integrations.gitlab.models.GitlabTestReport;
import io.levelops.integrations.gitlab.models.GitlabVariable;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static io.levelops.integrations.gitlab.services.GitlabFetchPipelineService.Enrichment.*;

/**
 * This class can be used for enriching a {@link GitlabProject} with {@link GitlabPipeline}.
 */
@Log4j2
public class GitlabFetchPipelineService {

    private static final EnumSet<Enrichment> ENRICHMENTS = EnumSet.of(JOBS);

    public Stream<GitlabProject> fetchPipelines(GitlabClient client, GitlabProject project, Date lastActivityAfter,
                                                Date lastActivityBefore, int perPage) {
        final String projectId = project.getId();
        MutableInt pipelinesCount = new MutableInt(0);
        Stream<GitlabPipeline> pipelines = client.streamPipelines(projectId, lastActivityAfter, lastActivityBefore, perPage)
                .filter(Objects::nonNull)
                .filter(pipeline -> pipeline.getUpdatedAt() != null && pipeline.getUpdatedAt().before(lastActivityBefore))
                .takeWhile(pipeline -> pipeline.getUpdatedAt() != null && pipeline.getUpdatedAt().after(lastActivityAfter))
                .filter(Objects::nonNull)
                .map(gitlabPipeline -> {
                    GitlabPipeline detailedPipeline = null;
                    try {
                        detailedPipeline = client.getProjectPipeline(projectId, gitlabPipeline.getPipelineId());
                    } catch (GitlabClientException e) {
                        log.error("Error fetching gitlab pipeline for project id " + project.getId() +
                                " and pipeline id " + gitlabPipeline.getPipelineId(), e);
                    }
                    return detailedPipeline;
                })
                .filter(Objects::nonNull)
                .flatMap(gitlabPipeline -> parseAndEnrichProject(client, project, gitlabPipeline, perPage))
                .peek(commit -> {
                    pipelinesCount.increment();
                    if (pipelinesCount.getValue() % 50 == 0) {
                        log.info("Processed Pipelines for projectId={}: pipelinesCount={}",
                                projectId, pipelinesCount.getValue());
                    }
                });
        //Ideally we do not need a feature flag, we do not have Gitlab Setup which has pipelines in dev or staging. So adding feature flag while we verify on one tenant
        return StreamUtils.partition(pipelines, 10)
                .map(batch -> project.toBuilder()
                        .pipelines(batch)
                        .build()
                );
    }

    private Stream<GitlabPipeline> parseAndEnrichProject(GitlabClient client,
                                                         GitlabProject project,
                                                         GitlabPipeline gitlabPipeline,
                                                         int perPage) {
        Stream<GitlabJob> jobs = Stream.empty();
        GitlabFetchJobsService fetchJobsService = new GitlabFetchJobsService();
        if (ENRICHMENTS.contains(JOBS)) {
            jobs = fetchJobsService.getPipelineJobs(client, project, gitlabPipeline, perPage);
        }

        List<GitlabVariable> gitlabVariables = fetchGitlabVariables(client, project, gitlabPipeline);
        GitlabTestReport gitlabTestReport = fetchGitlabTestReport(client, project, gitlabPipeline);

        return StreamUtils.partition(jobs, 100)
                .map(batch -> gitlabPipeline.toBuilder()
                        .variables(gitlabVariables)
                        .testReport(gitlabTestReport)
                        .jobs(batch)
                        .build());
    }

    private List<GitlabVariable> fetchGitlabVariables(GitlabClient client,
                                                 GitlabProject project,
                                                 GitlabPipeline gitlabPipeline) {
        try {
            return client.getPipelineVariables(project.getId(), gitlabPipeline.getPipelineId());
        } catch (GitlabClientException e) {
            log.error("Error fetching gitlab pipeline variables for project id " + project.getId() +
                    " and pipeline id " + gitlabPipeline.getPipelineId(), e);
           return null;
        }
    }

    private GitlabTestReport fetchGitlabTestReport(GitlabClient client,
                                                 GitlabProject project,
                                                 GitlabPipeline gitlabPipeline) {
        try {
            return client.getPipelineTestReport(project.getId(), gitlabPipeline.getPipelineId());
        } catch (GitlabClientException e) {
            log.error("Error fetching gitlab pipeline test report for project id " + project.getId() +
                    " and pipeline id " + gitlabPipeline.getPipelineId(), e);
            return null;
        }
    }

    public enum Enrichment {
        JOBS
    }
}

