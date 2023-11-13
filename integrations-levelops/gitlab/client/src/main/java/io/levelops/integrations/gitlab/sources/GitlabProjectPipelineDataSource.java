package io.levelops.integrations.gitlab.sources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.utils.ListUtils;
import io.levelops.ingestion.controllers.IntermediateStateUpdater;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.client.GitlabClientException;
import io.levelops.integrations.gitlab.client.GitlabClientFactory;
import io.levelops.integrations.gitlab.models.GitlabIntermediateState;
import io.levelops.integrations.gitlab.models.GitlabPipeline;
import io.levelops.integrations.gitlab.models.GitlabProject;
import io.levelops.integrations.gitlab.services.GitlabFetchPipelineService;
import io.levelops.integrations.gitlab.services.GitlabFetchProjectsService;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static io.levelops.integrations.gitlab.models.GitlabIntermediateState.updateIntermediateState;

/**
 * Gitlab's implementation of the {@link DataSource}. This class can be used to fetch {@link GitlabPipeline}
 * data from Gitlab.
 */
@Log4j2
public class GitlabProjectPipelineDataSource implements DataSource<GitlabProject,
        GitlabProjectPipelineDataSource.GitlabProjectQuery> {
    private final GitlabFetchPipelineService fetchPipelineService;
    private final GitlabFetchProjectsService fetchProjectsService;
    private final GitlabClientFactory clientFactory;

    public GitlabProjectPipelineDataSource(GitlabClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        fetchPipelineService = new GitlabFetchPipelineService();
        fetchProjectsService = new GitlabFetchProjectsService();
    }

    private GitlabClient getClient(IntegrationKey integrationKey) throws FetchException {
        GitlabClient client;
        try {
            client = clientFactory.get(integrationKey, false);
        } catch (GitlabClientException e) {
            throw new FetchException("Could not fetch Gitlab Client", e);
        }
        return client;
    }

    @Override
    public Data<GitlabProject> fetchOne(GitlabProjectQuery query) throws FetchException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Data<GitlabProject>> fetchMany(GitlabProjectQuery gitlabProjectQuery) throws FetchException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Data<GitlabProject>> fetchMany(JobContext jobContext, GitlabProjectQuery query, IntermediateStateUpdater intermediateStateUpdater) throws FetchException {
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        IntegrationKey integrationKey = query.getIntegrationKey();
        Date lastActivityAfter = query.getFrom();
        Date lastActivityBefore = query.getTo();
        GitlabClient client = getClient(integrationKey);
        List<String> projectsIdsToExclude = ListUtils.emptyIfNull(query.getProjectsIdsToExclude());

        // if 'resume from repo' was provided, we want to skip all repos before seeing the given repo
        GitlabIntermediateState intermediateState = GitlabIntermediateState.parseIntermediateState(jobContext.getIntermediateState());
        String resumeFromRepo = intermediateState.getResumeFromRepo();
        boolean checkMembership = query.getCheckProjectMembership() != null ? query.getCheckProjectMembership() : true;

        MutableInt projectsProcessed = new MutableInt(1);
        int totalProjectCount = getTotalProjectCount(client, checkMembership);
        Stream<Data<GitlabProject>> stream = fetchProjectsService.getProjectStream(
                        client,
                        query.getProjects(),
                        projectsIdsToExclude,
                        checkMembership,
                        resumeFromRepo)
                .filter(Objects::nonNull)
                .filter(project -> {
                    if ((project.getBuildsAccessLevel() != null &&
                            "disabled".equals(project.getBuildsAccessLevel())) || !project.isJobsEnabled()) {
                        log.info("Pipelines are disabled for the project {}", project.getId());
                        return false;
                    }
                    return true;
                })
                .flatMap(project -> {
                    try {
                        log.info("-> processing project={}:{} [{}/{}], project id={} (jobId={})",
                                project.getNameWithNamespace(), project.getId(), projectsProcessed.getAndIncrement(), totalProjectCount, project.getId(),
                                jobContext.getJobId());
                        Stream<GitlabProject> enrichedProject = parseAndEnrichProject(client, project, lastActivityAfter, lastActivityBefore);
                        updateIntermediateState(project.getNameWithNamespace(), intermediateStateUpdater);
                        return enrichedProject;
                    } catch (GitlabClientException e) {
                        log.error("failed to get pipelines for projects {}", project.getId(), e);
                    }
                    return Stream.empty();
                })
                .map(BasicData.mapper(GitlabProject.class));
        return stream.filter(Objects::nonNull);
    }

    private int getTotalProjectCount(GitlabClient client, boolean checkMembership) {
        int totalProjectCount = -1;
        try {
            totalProjectCount = client.getProjectCount(checkMembership);
        } catch (GitlabClientException e) {
            log.warn("Could not get total project count");
        }
        return totalProjectCount;
    }

    private Stream<GitlabProject> parseAndEnrichProject(GitlabClient client,
                                                        GitlabProject project,
                                                        Date from,
                                                        Date to) throws GitlabClientException {
        return fetchPipelineService.fetchPipelines(client, project, from, to, 100);
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GitlabProjectQuery.GitlabProjectQueryBuilder.class)
    public static class GitlabProjectQuery implements IntegrationQuery {

        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        @JsonProperty("from")
        Date from;
        @JsonProperty("to")
        Date to;

        @JsonProperty("projects")
        List<String> projects; // fetch specific projects (format: "owner/name")

        @JsonProperty("project_ids_to_exclude")
        List<String> projectsIdsToExclude; // exclude these projects (format: project_id eg. 123)

        @JsonProperty("limit")
        Integer limit;

        @JsonProperty("check_project_membership")
        Boolean checkProjectMembership;
    }
}
