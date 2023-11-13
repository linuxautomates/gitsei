package io.levelops.integrations.github.actions.sources;

import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.github.actions.models.GithubActionsIngestionQuery;
import io.levelops.integrations.github.actions.services.GithubActionsWorkflowRunJobService;
import io.levelops.integrations.github.actions.services.GithubActionsWorkflowRunService;
import io.levelops.integrations.github.client.GithubClient;
import io.levelops.integrations.github.client.GithubClientException;
import io.levelops.integrations.github.client.GithubClientFactory;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.github.services.GithubOrganizationService;
import io.levelops.integrations.github.services.GithubRepositoryService;
import io.levelops.integrations.github_actions.models.GithubActionsEnrichedWorkflowRun;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflowRun;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflowRunJob;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class GithubActionsWorkflowDataSource implements DataSource<GithubActionsEnrichedWorkflowRun, GithubActionsIngestionQuery> {

    private static final int ONBOARDING_IN_DAYS = 30;
    private final GithubClientFactory githubClientFactory;
    private final GithubRepositoryService repositoryService;
    private final GithubOrganizationService organizationService;
    private final GithubActionsWorkflowRunService workflowRunService;
    private final GithubActionsWorkflowRunJobService workflowRunJobService;

    public GithubActionsWorkflowDataSource(GithubClientFactory githubClientFactory,
                                           GithubRepositoryService repositoryService,
                                           GithubOrganizationService organizationService,
                                           GithubActionsWorkflowRunService workflowRunService,
                                           GithubActionsWorkflowRunJobService workflowRunJobService) {
        this.githubClientFactory = githubClientFactory;
        this.repositoryService = repositoryService;
        this.organizationService = organizationService;
        this.workflowRunService = workflowRunService;
        this.workflowRunJobService = workflowRunJobService;
    }

    @Override
    public Stream<Data<GithubActionsEnrichedWorkflowRun>> fetchMany(GithubActionsIngestionQuery query) throws FetchException {
        throw new UnsupportedOperationException("This should never be reached");
    }

    @Override
    public Data<GithubActionsEnrichedWorkflowRun> fetchOne(GithubActionsIngestionQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<GithubActionsEnrichedWorkflowRun>> fetchMany(JobContext jobContext, GithubActionsIngestionQuery query) throws FetchException {
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        IntegrationKey integrationKey = query.getIntegrationKey();

        GithubClient githubClient;
        try {
            githubClient = githubClientFactory.get(integrationKey, false);
        } catch (GithubClientException e) {
            throw new RuntimeException(e);
        }

        Instant from = DateUtils.toInstant(query.getFrom(),
                Instant.now().minus(Duration.ofDays(ONBOARDING_IN_DAYS)));
        Instant to = DateUtils.toInstant(query.getTo(), Instant.now());


        return getRepositoryStream(query)
                .filter(Objects::nonNull)
                .flatMap(repository -> getEnrichedWorkflowRuns(githubClient, repository, from, to))
                .map(BasicData.mapper(GithubActionsEnrichedWorkflowRun.class));
    }

    private Stream<GithubActionsEnrichedWorkflowRun> getEnrichedWorkflowRuns(GithubClient client, GithubRepository repository, Instant from, Instant to) {
        return workflowRunService.getWorkflowRuns(client, repository.getFullName(), from, to)
                .filter(Objects::nonNull)
                .map(workflowRun -> getWorkflowRunJobs(client, repository.getFullName(), workflowRun, from, to));
    }

    private GithubActionsEnrichedWorkflowRun getWorkflowRunJobs(GithubClient client, String repoFullName, GithubActionsWorkflowRun workflowRun, Instant from, Instant to) {
        List<GithubActionsWorkflowRunJob> jobs = workflowRunJobService.getWorkflowRunJobs(client, repoFullName, workflowRun, from, to)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        log.debug("{} jobs found for workflow run={}, repository={}", jobs.size(), workflowRun.getId(), repoFullName);

        return GithubActionsEnrichedWorkflowRun.builder()
                .workflowRun(workflowRun)
                .jobs(jobs)
                .build();
    }

    public Stream<GithubRepository> getRepositoryStream(GithubActionsIngestionQuery query) throws FetchException {
        if (CollectionUtils.isNotEmpty(query.getRepos())) {
            return ListUtils.emptyIfNull(query.getRepos()).stream()
                    .filter(StringUtils::isNotBlank)
                    .map(StringUtils::trim)
                    .map(StringUtils::lowerCase)
                    .map(repoId -> {
                        try {
                            return repositoryService.getRepository(query.getIntegrationKey(), repoId);
                        } catch (FetchException e) {
                            log.warn("Failed to ingest github repository {}", repoId, e);
                            return null;
                        }
                    }).filter(Objects::nonNull);
        } else if (BooleanUtils.isTrue(query.getGithubApp())) {
            // in GitHub Apps, there is no authenticated user, so we have to get the list of repos directly from the app installation
            return repositoryService.streamInstallationRepositories(query.getIntegrationKey());
        } else {
            List<String> organizations = organizationService.getOrganizations(query.getIntegrationKey());
            return organizations.stream().flatMap(org -> {
                try {
                    return repositoryService.streamAllRepositories(query.getIntegrationKey(), org);
                } catch (FetchException e) {
                    throw new RuntimeStreamException(e);
                }
            });
        }
    }
}