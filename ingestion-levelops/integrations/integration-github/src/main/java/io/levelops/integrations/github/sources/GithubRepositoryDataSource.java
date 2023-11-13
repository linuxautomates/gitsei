package io.levelops.integrations.github.sources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.data.FailedData;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.github.client.GithubClientException;
import io.levelops.integrations.github.client.GithubClientFactory;
import io.levelops.integrations.github.client.GithubClient;
import io.levelops.integrations.github.model.GithubConverters;
import io.levelops.integrations.github.model.GithubIntermediateState;
import io.levelops.integrations.github.models.GithubEvent;
import io.levelops.integrations.github.models.GithubIssue;
import io.levelops.integrations.github.models.GithubIssueEvent;
import io.levelops.integrations.github.models.GithubPullRequest;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.github.models.GithubTag;
import io.levelops.integrations.github.services.GithubEventService;
import io.levelops.integrations.github.services.GithubIssueService;
import io.levelops.integrations.github.services.GithubOrganizationService;
import io.levelops.integrations.github.services.GithubPullRequestService;
import io.levelops.integrations.github.services.GithubRepositoryService;
import io.levelops.integrations.github.services.GithubTagService;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.util.Strings;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Log4j2
public class GithubRepositoryDataSource implements DataSource<GithubRepository, GithubRepositoryDataSource.GithubRepositoryQuery> {

    private static final int ONBOARDING_IN_DAYS = 30;
    private static final Pattern REPO_PATTERN = Pattern.compile("\\s*([^/\\s]*)\\s*/\\s*([^/\\s]*)\\s*");
    private final GithubClientFactory levelOpsClientFactory;
    private final GithubEventService githubEventService;
    private final GithubPullRequestService pullRequestService;
    private final GithubTagService githubTagService;
    private final GithubIssueService githubIssueService;
    private final GithubOrganizationService organizationService;
    private final EnumSet<Enrichment> enrichments;
    private final GithubRepositoryService repositoryService;

    public enum Enrichment {
        LANGUAGES, COMMITS, PULL_REQUESTS, ISSUES, ISSUE_EVENTS, TAGS
    }

    public GithubRepositoryDataSource(ObjectMapper objectMapper,
                                      GithubClientFactory levelOpsClientFactory,
                                      GithubOrganizationService organizationService,
                                      GithubRepositoryService repositoryService) {
        this(objectMapper, levelOpsClientFactory, organizationService, repositoryService, EnumSet.of(Enrichment.LANGUAGES));
    }

    public GithubRepositoryDataSource(ObjectMapper objectMapper,
                                      GithubClientFactory levelOpsClientFactory,
                                      GithubOrganizationService organizationService,
                                      GithubRepositoryService repositoryService,
                                      EnumSet<Enrichment> enrichments) {
        this.levelOpsClientFactory = levelOpsClientFactory;
        this.organizationService = organizationService;
        this.enrichments = enrichments;
        githubEventService = new GithubEventService(objectMapper);
        pullRequestService = new GithubPullRequestService();
        githubTagService = new GithubTagService();
        githubIssueService = new GithubIssueService(levelOpsClientFactory);
        this.repositoryService = repositoryService;
    }

    @Override
    public Data<GithubRepository> fetchOne(GithubRepositoryQuery query) throws FetchException {
        if (Strings.isEmpty(query.getName()) || Strings.isEmpty(query.getOwner())) {
            return BasicData.empty(GithubRepository.class);
        }

        Instant from = DateUtils.toInstant(query.getFrom(),
                Instant.now().minus(Duration.ofDays(ONBOARDING_IN_DAYS)));
        Instant to = DateUtils.toInstant(query.getTo(), Instant.now());
        boolean onboarding = BooleanUtils.isTrue(query.getOnboarding());

        IntegrationKey integrationKey = query.getIntegrationKey();
        try {
            GithubClient githubClient = levelOpsClientFactory.get(integrationKey, false);
            GithubRepository repository = repositoryService.getRepository(integrationKey, query.owner + "/" + query.name);
            GithubRepository githubRepository = parseAndEnrichRepository(githubClient,
                    integrationKey, repository, from, to, onboarding,
                    BooleanUtils.isNotFalse(query.getFetchPrCommits()),
                    BooleanUtils.isNotFalse(query.getFetchPrReviews()),
                    BooleanUtils.isNotFalse(query.getFetchPrPatches()));
            return BasicData.of(GithubRepository.class, githubRepository);
        } catch (GithubClientException e) {
            throw new FetchException("Failed to fetch Github repository for query=" + query, e);
        }
    }

    @Override
    public Stream<Data<GithubRepository>> fetchMany(GithubRepositoryQuery githubRepositoryQuery) throws FetchException {
        throw new UnsupportedOperationException("This should never be reached");
    }

    @Override
    public Stream<Data<GithubRepository>> fetchMany(JobContext jobContext, GithubRepositoryQuery query) throws FetchException {
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        IntegrationKey integrationKey = query.getIntegrationKey();

        GithubClient levelopsClient;
        try {
            levelopsClient = levelOpsClientFactory.get(integrationKey, false);
        } catch (GithubClientException e) {
            throw new FetchException("Failed to get client", e);
        }

        Instant from = DateUtils.toInstant(query.getFrom(),
                Instant.now().minus(Duration.ofDays(ONBOARDING_IN_DAYS)));
        Instant to = DateUtils.toInstant(query.getTo(), Instant.now());
        boolean onboarding = BooleanUtils.isTrue(query.getOnboarding());

        // using iterator instead of stream to handle exceptions using FailedData
        Iterator<GithubRepository> repositoryIterator = getRepositoryStream(query).iterator();

        GithubIntermediateState intermediateState = GithubIntermediateState.parseIntermediateState(jobContext.getIntermediateState());

        // if 'resume from repo' was provided, we want to skip all repos before seeing the given repo
        String resumeFromRepo = intermediateState.getResumeFromRepo();
        MutableBoolean skipRepos = new MutableBoolean(false);
        if (StringUtils.isNotBlank(resumeFromRepo)) {
            log.info("Will skip repos before '{}'", resumeFromRepo);
            skipRepos.setTrue();
        }

        MutableObject<String> lastRepoProcessed = new MutableObject<>(null);
        Stream<Data<GithubRepository>> stream = Stream.generate(() -> {
                    // -- find repo --
                    GithubRepository repo = null;
                    try {
                        if (skipRepos.isTrue()) {
                            while (repositoryIterator.hasNext()) {
                                repo = repositoryIterator.next();
                                if (getRepoName(repo).equalsIgnoreCase(resumeFromRepo)) {
                                    log.info("Resuming scan from repo={}", resumeFromRepo);
                                    skipRepos.setFalse();
                                    break;
                                }
                                log.debug(">>> skipping repo {}", getRepoName(repo));
                            }
                        } else {
                            if (repositoryIterator.hasNext()) {
                                repo = repositoryIterator.next();
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to stream repositories. Will attempt to resume job from here. (enrichments={}, job_id={}) - {}", enrichments, jobContext.getJobId(), e.toString());
                        GithubIntermediateState newState;
                        if (skipRepos.isFalse()) {
                            // if we reached the repo we were supposed to resume from, we can resume from farther
                            newState = intermediateState.toBuilder()
                                    .resumeFromRepo(lastRepoProcessed.getValue())
                                    .build();
                        } else {
                            // if we didn't reach the repo we wanted, then don't change intermediateState
                            newState = intermediateState;
                        }
                        return FailedData.of(GithubRepository.class, e, newState);
                    }

                    if (repo == null) {
                        // if we exhausted the stream, return null to trigger takeWhile
                        return null;
                    }

                    log.info("-> processing repo={} (enrichments={}, jobId={})", getRepoName(repo), enrichments, jobContext.getJobId());

                    // -- process repo --
                    String repoName = getRepoName(repo);
                    lastRepoProcessed.setValue(repoName);
                    try {
                        BasicData<GithubRepository> enrichedRepository = BasicData.of(GithubRepository.class, parseAndEnrichRepository(
                                levelopsClient, integrationKey, repo, from, to, onboarding,
                                BooleanUtils.isNotFalse(query.getFetchPrCommits()),
                                BooleanUtils.isNotFalse(query.getFetchPrReviews()),
                                BooleanUtils.isNotFalse(query.getFetchPrPatches())));
                        log.info("Successfully processed repo={} (enrichments={}, jobId={})", getRepoName(repo), enrichments, jobContext.getJobId());
                        return enrichedRepository;
                    } catch (Exception e) {
                        log.error("Failed to ingest repo={}. Will attempt to resume job from here. (enrichments={}, job_id={}) - {}", repoName, enrichments, jobContext.getJobId(), e.toString());
                        GithubIntermediateState newState = intermediateState.toBuilder()
                                .resumeFromRepo(repoName)
                                .build();
                        return FailedData.of(GithubRepository.class, e, newState);
                    }
                })
                .takeWhile(Objects::nonNull);

        if (query.getLimit() != null) {
            stream = stream.limit(query.getLimit());
        }
        return stream.filter(Objects::nonNull);
    }

    public Stream<GithubRepository> getRepositoryStream(GithubRepositoryQuery query) throws FetchException {
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

    protected static String getRepoName(GithubRepository repo) {
        if (repo.getOwner() != null) {
            return repo.getOwner().getLogin() + "/" + repo.getName();
        } else {
            return repo.getName() + "#" + repo.getId();
        }
    }

    private GithubRepository parseAndEnrichRepository(GithubClient levelopsClient,
                                                      IntegrationKey integrationKey,
                                                      GithubRepository repository,
                                                      Instant from,
                                                      Instant to,
                                                      boolean onboarding,
                                                      boolean fetchPrCommits,
                                                      boolean fetchPrReviews,
                                                      boolean fetchPrPatches) throws FetchException {
        Map<String, Long> languages = null;
        List<GithubEvent> events = null;
        List<GithubPullRequest> pullRequests = null;
        List<GithubTag> tags = null;
        List<GithubIssue> issues = null;
        List<GithubIssueEvent> issueEvents = null;
        int DEFAULT_PER_PAGE = 100;
        if (enrichments.contains(Enrichment.LANGUAGES)) {
            languages = repositoryService.getLanguages(integrationKey, repository.getId());
        }
        if (enrichments.contains(Enrichment.COMMITS)) {
            events = githubEventService.getEvents(repository.getId(), from, to, levelopsClient);
        }
        if (enrichments.contains(Enrichment.PULL_REQUESTS)) {
            pullRequests = pullRequestService.getPullRequests(levelopsClient, repository.getId(), from, to, fetchPrCommits, fetchPrReviews, fetchPrPatches);
        }
        if (enrichments.contains(Enrichment.TAGS)) {
            tags = githubTagService.getTags(levelopsClient, repository.getOwner().getLogin(), repository.getName(), DEFAULT_PER_PAGE);
        }
        if (enrichments.contains(Enrichment.ISSUES)) {
            try {
                issues = githubIssueService.getIssues(integrationKey, repository.getId(), from, to, onboarding,
                        enrichments.contains(Enrichment.ISSUE_EVENTS));
            } catch (GithubClientException e) {
                log.warn("Failed to get issues for repo = {}", repository, e);
            }
        }

        return GithubConverters.parseGithubRepository(repository, languages, events, pullRequests, tags, issues, issueEvents);
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GithubRepositoryQuery.GithubRepositoryQueryBuilder.class)
    public static class GithubRepositoryQuery implements IntegrationQuery {

        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        @JsonProperty("from")
        Date from;
        @JsonProperty("to")
        Date to;
        @JsonProperty("onboarding")
        Boolean onboarding;

        @JsonProperty("repos")
        List<String> repos; // fetch specific repos (format: "owner/name")

        @JsonProperty("github_app")
        Boolean githubApp;

        // region fetch one
        @JsonProperty("name")
        String name;
        @JsonProperty("owner")
        String owner;
        //endregion

        @JsonProperty("limit")
        Integer limit;

        // region pull request flags
        @JsonProperty("fetch_pr_commits")
        Boolean fetchPrCommits;
        @JsonProperty("fetch_pr_reviews")
        Boolean fetchPrReviews;
        @JsonProperty("fetch_pr_patches")
        Boolean fetchPrPatches;
        // endregion
    }

}
