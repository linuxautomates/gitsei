package io.levelops.integrations.gitlab.sources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.utils.ListUtils;
import io.levelops.ingestion.controllers.IntermediateStateUpdater;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.data.FailedData;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.client.GitlabClientException;
import io.levelops.integrations.gitlab.client.GitlabClientFactory;
import io.levelops.integrations.gitlab.models.*;
import io.levelops.integrations.gitlab.services.GitlabFetchCommitsService;
import io.levelops.integrations.gitlab.services.GitlabFetchMergeRequestsService;
import io.levelops.integrations.gitlab.services.GitlabFetchProjectsService;
import io.levelops.integrations.gitlab.services.GitlabFetchTagService;
import io.levelops.integrations.gitlab.services.GitlabProjectEnrichmentService;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.integrations.gitlab.models.GitlabIntermediateState.updateIntermediateState;

/**
 * Gitlab's implementation of the {@link DataSource}. This class can be used to fetch {@link GitlabProject}
 * along with enriched data from Gitlab.
 */
@Log4j2
public class GitlabProjectDataSource implements DataSource<GitlabProject,
        GitlabProjectDataSource.GitlabProjectQuery> {
    private static final int PR_COMMIT_LIMIT = 250;
    private final EnumSet<Enrichment> enrichments;
    private final GitlabFetchProjectsService fetchProjectsService;
    private final GitlabFetchMergeRequestsService fetchMergeRequestsService;
    private final GitlabFetchCommitsService fetchCommitsService;
    private final GitlabFetchTagService fetchTagsService;
    private final GitlabProjectEnrichmentService projectEnrichmentService;
    private final GitlabClientFactory clientFactory;

    public GitlabProjectDataSource(GitlabClientFactory clientFactory) {
        this(clientFactory, EnumSet.noneOf(Enrichment.class));
    }

    public GitlabProjectDataSource(GitlabClientFactory clientFactory, EnumSet<Enrichment> enrichments) {
        this.enrichments = enrichments;
        this.clientFactory = clientFactory;
        fetchCommitsService = new GitlabFetchCommitsService();
        fetchTagsService = new GitlabFetchTagService();
        fetchProjectsService = new GitlabFetchProjectsService();
        fetchMergeRequestsService = new GitlabFetchMergeRequestsService();
        projectEnrichmentService = new GitlabProjectEnrichmentService();
    }

    private GitlabClient getClient(IntegrationKey integrationKey) throws FetchException {
        GitlabClient client;
        try {
            client = clientFactory.get(integrationKey, false);
        } catch (GitlabClientException e) {
            throw new FetchException("Could not fetch Gitlab client", e);
        }
        return client;
    }

    @Override
    public Data<GitlabProject> fetchOne(GitlabProjectQuery query) throws FetchException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Data<GitlabProject>> fetchMany(GitlabProjectQuery query) throws FetchException {
        throw new UnsupportedOperationException("Only the jobContext version of fetchMany is supported because Gitlab " +
                "supports checkpointing.");
    }

    @Override
    public Stream<Data<GitlabProject>> fetchMany(JobContext jobContext, GitlabProjectQuery query, IntermediateStateUpdater intermediateStateUpdater) throws FetchException {
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        IntegrationKey integrationKey = query.getIntegrationKey();
        Date lastActivityAfter = query.getFrom();
        Date lastActivityBefore = query.getTo();
        GitlabClient client = getClient(integrationKey);

        GitlabIntermediateState intermediateState = GitlabIntermediateState.parseIntermediateState(jobContext.getIntermediateState());

        // if 'resume from repo' was provided, we want to skip all repos before seeing the given repo
        String resumeFromRepo = intermediateState.getResumeFromRepo();
        boolean checkMembership = query.getCheckProjectMembership() != null ? query.getCheckProjectMembership() : true;

        MutableInt projectsProcessed = new MutableInt(1);
        int finalTotalProjectCount = getTotalProjectCount(client, checkMembership);
        return fetchProjectsService.getProjectStream(
                        client,
                        query.getProjects(),
                        query.getProjectsIdsToExclude(),
                        checkMembership,
                        resumeFromRepo
                ).flatMap(project -> {
                    log.info("-> processing project={}:{} [{}/{}], project id={} (enrichments={}, jobId={})",
                            getProjectName(project), project.getId(), projectsProcessed.getAndIncrement(), finalTotalProjectCount, project.getId(),
                            enrichments, jobContext.getJobId());

                    // -- process project --
                    String projectName = getProjectName(project);
                    try {
                        Stream<GitlabProject> enrichedProjectStream = parseAndEnrichProject(client, project, lastActivityAfter, lastActivityBefore, query);
                        Stream<Data<GitlabProject>> streamToReturn = enrichedProjectStream.<Data<GitlabProject>>map(BasicData.mapper(GitlabProject.class));
                        var l = streamToReturn.collect(Collectors.toList());
                        updateIntermediateState(projectName, intermediateStateUpdater);
                        return l.stream();
                    } catch (Exception e) {
                        log.error("Failed to ingest project={}. Will attempt to resume job from here. (enrichments={}, job_id{}) - {}",
                                projectName, enrichments, jobContext.getJobId(), e.toString());
                        GitlabIntermediateState newState = intermediateState.toBuilder()
                                .resumeFromRepo(projectName)
                                .build();
                        return Stream.<Data<GitlabProject>>of(FailedData.of(GitlabProject.class, e, newState));
                    }
                })
                .filter(Objects::nonNull);
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

    private String getProjectName(GitlabProject project) {
        return project.getNameWithNamespace();
    }

    private Stream<GitlabProject> parseAndEnrichProject(GitlabClient client,
                                                        GitlabProject project,
                                                        Date from,
                                                        Date to,
                                                        GitlabProjectQuery query) {
        if (enrichments.isEmpty()) {
            return Stream.of(project);
        }

        boolean fetchCommitPatches = BooleanUtils.isTrue(query.getFetchCommitPatches());
        boolean fetchPrPatches = BooleanUtils.isTrue(query.getFetchPrPatches());
        boolean fetchStateEvents = BooleanUtils.isTrue(query.getFetchStateEvents());
        int prCommitsLimit = query.getPrCommitsLimit() != null ? query.getPrCommitsLimit() : PR_COMMIT_LIMIT;

        Stream<GitlabProject> mergeRequests = Stream.empty();
        Stream<GitlabProject> commits = Stream.empty();
        Stream<GitlabProject> tags = Stream.empty();
        Stream<GitlabProject> branches = Stream.empty();
        Stream<GitlabProject> users = Stream.empty();
        Stream<GitlabProject> milestones = Stream.empty();

        int DEFAULT_PER_PAGE = 100;
        if (enrichments.contains(Enrichment.COMMITS)) {
            commits = fetchCommitsService.getProjectCommits(client, project, from, to, DEFAULT_PER_PAGE, fetchCommitPatches);
        }
        if (enrichments.contains(Enrichment.TAGS)) {
            tags = fetchTagsService.getProjectTags(client, project, DEFAULT_PER_PAGE);
        }
        if (enrichments.contains(Enrichment.MERGE_REQUESTS)) {
            mergeRequests = fetchMergeRequestsService.getProjectMrs(client, project, from, to, DEFAULT_PER_PAGE, fetchPrPatches, fetchStateEvents, prCommitsLimit);
        }
        if (enrichments.contains(Enrichment.BRANCHES)) {
            branches = projectEnrichmentService.getProjectBranches(client, project, DEFAULT_PER_PAGE);
        }
        // TODO revisit this
//        if (enrichments.contains(Enrichment.USERS)) {
//             users = projectEnrichmentService.getProjectUsers(client, project, from, to, DEFAULT_PER_PAGE);
//        }
        if (enrichments.contains(Enrichment.MILESTONES)) {
            milestones = projectEnrichmentService.getProjectMilestones(client, project, DEFAULT_PER_PAGE);
        }
        return Stream.of(commits, mergeRequests, tags, branches, users, milestones).flatMap(s -> s);
    }

    public enum Enrichment {
        COMMITS, MERGE_REQUESTS, BRANCHES, USERS, MILESTONES, PIPELINES, TAGS
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

        @JsonProperty("fetch_commit_patches")
        Boolean fetchCommitPatches;

        @JsonProperty("fetch_pr_patches")
        Boolean fetchPrPatches;

        @JsonProperty("fetch_state_events")
        Boolean fetchStateEvents;

        @JsonProperty("pr_commits_limit")
        Integer prCommitsLimit;
    }
}
