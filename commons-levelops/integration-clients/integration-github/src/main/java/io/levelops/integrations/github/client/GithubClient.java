package io.levelops.integrations.github.client;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.client.ClientHelper.BodyAndHeaders;
import io.levelops.commons.client.graphql.GraphQlQuery;
import io.levelops.commons.client.graphql.GraphQlResponse;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.github.model.GithubApiCommit;
import io.levelops.integrations.github.model.GithubApiIssueEvent;
import io.levelops.integrations.github.model.GithubApiRepoEvent;
import io.levelops.integrations.github.model.GithubContributor;
import io.levelops.integrations.github.model.GithubConverters;
import io.levelops.integrations.github.model.GithubInstallationRepositoriesResponse;
import io.levelops.integrations.github.model.GithubPaginatedResponse;
import io.levelops.integrations.github.model.GithubUserGraphQlResponse;
import io.levelops.integrations.github.model.GithubUserRepos;
import io.levelops.integrations.github.models.GithubCommit;
import io.levelops.integrations.github.models.GithubAppInstallation;
import io.levelops.integrations.github.models.GithubIssue;
import io.levelops.integrations.github.models.GithubIssueEvent;
import io.levelops.integrations.github.models.GithubOrganization;
import io.levelops.integrations.github.models.GithubProject;
import io.levelops.integrations.github.models.GithubProjectCard;
import io.levelops.integrations.github.models.GithubProjectColumn;
import io.levelops.integrations.github.models.GithubPullRequest;
import io.levelops.integrations.github.models.GithubPullRequestSearchResult;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.github.models.GithubReview;
import io.levelops.integrations.github.models.GithubSearchResult;
import io.levelops.integrations.github.models.GithubTag;
import io.levelops.integrations.github.models.GithubUser;
import io.levelops.integrations.github.models.GithubWebhookRequest;
import io.levelops.integrations.github.models.GithubWebhookResponse;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflow;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflowPaginatedResponse;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflowRun;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflowRunJob;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflowRunJobPaginatedResponse;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflowRunPaginatedResponse;
import io.levelops.rate_limiter.IntervalRateLimiter;
import io.levelops.rate_limiter.RateLimiter;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.util.Strings;
import org.eclipse.egit.github.core.RepositoryCommit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class GithubClient {
    private static final String CLOUD_BASE_URL = "https://api.github.com/";
    public static final String CLOUD_HOST = "api.github.com";
    private static final String LINK_HEADER = "link";
    public static final String PER_PAGE = "100";
    private static final int SEARCH_PAGE_SIZE = 100;
    private final ClientHelper<GithubClientException> clientHelper;
    private final ClientHelper<GithubClientException> jwtClientHelper;
    private final ObjectMapper objectMapper;
    private final Supplier<String> urlSupplier;
    private final boolean ingestCommitFiles;
    private final RateLimiter rateLimiter;

    @Builder
    public GithubClient(OkHttpClient okHttpClient,
                        OkHttpClient jwtOkHttpClient,
                        ObjectMapper objectMapper,
                        Supplier<String> urlSupplier,
                        int throttlingIntervalMs,
                        Boolean ingestCommitFiles) {
        this.objectMapper = objectMapper;
        this.urlSupplier = () -> sanitizeUrl(urlSupplier != null ? urlSupplier.get() : null);
        clientHelper = ClientHelper.<GithubClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(GithubClientException.class)
                .build();
        jwtClientHelper = ClientHelper.<GithubClientException>builder()
                .client(jwtOkHttpClient)
                .objectMapper(objectMapper)
                .exception(GithubClientException.class)
                .build();
        this.ingestCommitFiles = BooleanUtils.isNotFalse(ingestCommitFiles);
        rateLimiter = new IntervalRateLimiter(throttlingIntervalMs);
    }

    protected static String sanitizeUrl(String url) {
        if (Strings.isBlank(url)) {
            return CLOUD_BASE_URL; // default to cloud
        }
        url = url.trim();
        if (url.startsWith("https://") || url.startsWith("http://")) {
            return url;
        }
        return "https://" + url;
    }

    protected static HttpUrl.Builder baseUrlBuilder(String url) {
        HttpUrl baseUrl = HttpUrl.parse(url);
        Validate.notNull(baseUrl, "baseUrl cannot be null.");

        // -- cloud github (just use the predefined URL)
        if (CLOUD_HOST.equalsIgnoreCase(baseUrl.host())) {
            return HttpUrl.parse(CLOUD_BASE_URL).newBuilder();
        }

        // -- enterprise github
        boolean appendVersion = false;
        if (CollectionUtils.size(baseUrl.pathSegments()) < 2) {
            appendVersion = true;
        } else {
            String last = baseUrl.pathSegments().get(baseUrl.pathSegments().size() - 1);
            String nextToLast = baseUrl.pathSegments().get(baseUrl.pathSegments().size() - 2);
            if (!"api".equalsIgnoreCase(nextToLast) || !"v3".equalsIgnoreCase(last)) {
                appendVersion = true;
            }
        }
        var builder = baseUrl.newBuilder();
        if (appendVersion) {
            builder.addPathSegment("api").addPathSegment("v3");
        }
        return builder;
    }

    private HttpUrl.Builder baseUrlBuilder() {
        return baseUrlBuilder(urlSupplier.get());
    }

    public GraphQlResponse queryGraphQl(String query) throws GithubClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("graphql")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(GraphQlQuery.builder()
                        .query(query)
                        .build()))
                .build();
        return executeAndParseRequest(request, GraphQlResponse.class);
    }

    public Stream<GithubProject> streamProjects(String org) throws RuntimeStreamException {
        return stream(RuntimeStreamException.wrap(page -> {
            log.info("Fetching projects {} for github organisation={}", page, org);
            return getProjects(org, page);
        }));
    }

    public Boolean getIngestCommitFiles() {
        return ingestCommitFiles;
    }

    // GET /orgs/{org}/projects
    public GithubPaginatedResponse<GithubProject> getProjects(String org, int pageNumber) throws GithubClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("orgs")
                .addPathSegment(org)
                .addPathSegment("projects")
                .addQueryParameter("state", "all")
                .addQueryParameter("per_page", PER_PAGE)
                .addQueryParameter("page", String.valueOf(pageNumber))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.API_PREVIEW_HEADER.toString())
                .get()
                .build();
        return executeAndParseListRequest(request, GithubProject.class);
    }

    // GET /projects/{project_id}
    public GithubProject getProject(String projectId) throws GithubClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.API_PREVIEW_HEADER.toString())
                .get()
                .build();
        return executeAndParseRequest(request, GithubProject.class);
    }

    public Stream<GithubProjectColumn> streamProjectColumns(String projectId) throws RuntimeStreamException {
        return stream(RuntimeStreamException.wrap(page -> {
            log.info("Fetching columns {} for github project={}", page, projectId);
            return getProjectColumns(projectId, page);
        }));
    }

    // GET /projects/{project_id}/columns
    public GithubPaginatedResponse<GithubProjectColumn> getProjectColumns(String projectId, int pageNumber) throws GithubClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("columns")
                .addQueryParameter("per_page", PER_PAGE)
                .addQueryParameter("page", String.valueOf(pageNumber))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.API_PREVIEW_HEADER.toString())
                .get()
                .build();
        return executeAndParseListRequest(request, GithubProjectColumn.class);
    }

    public Stream<GithubProjectCard> streamProjectColumnCards(String columnId, boolean includeArchived) throws RuntimeStreamException {
        return stream(RuntimeStreamException.wrap(page -> {
            log.info("Fetching cards {} for github project column={}", page, columnId);
            return getProjectColumnCards(columnId, page, includeArchived);
        }));
    }

    // GET /projects/columns/{column_id}/cards
    public GithubPaginatedResponse<GithubProjectCard> getProjectColumnCards(String columnId, int pageNumber, boolean includeArchived) throws GithubClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment("columns")
                .addPathSegment(columnId)
                .addPathSegment("cards")
                .addQueryParameter("archived_state", includeArchived ? "all" : "not_archived")
                .addQueryParameter("per_page", PER_PAGE)
                .addQueryParameter("page", String.valueOf(pageNumber))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.API_PREVIEW_HEADER.toString())
                .get()
                .build();
        return executeAndParseListRequest(request, GithubProjectCard.class);
    }

    // GET /projects/columns/cards/{card_id}
    public GithubProjectCard getProjectColumnCard(String cardId) throws GithubClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment("columns")
                .addPathSegment("cards")
                .addPathSegment(cardId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.API_PREVIEW_HEADER.toString())
                .get()
                .build();
        return executeAndParseRequest(request, GithubProjectCard.class);
    }

    public Stream<GithubPullRequest> streamPullRequests(String repoId) throws RuntimeStreamException {
        return stream(RuntimeStreamException.wrap(page -> {
            log.info("Fetching page {} of github PRs for repo={}", page, repoId);
            return getPullRequests(repoId, page);
        }));
    }

    /**
     * List pull requests
     *
     * @param repoId     ":owner/:repo". Use repo.generateId() e.g. "levelops/api-levelops"
     * @param pageNumber 1-based
     * @return
     * @throws GithubClientException
     */
    // https://api.github.com/repos/octocat/Hello-World/pulls/
    public GithubPaginatedResponse<GithubPullRequest> getPullRequests(String repoId, int pageNumber) throws GithubClientException {
        // TODO state / sort / pagination
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("repos")
                .addPathSegments(repoId) // (2 segments) ":owner/:repo"
                .addPathSegment("pulls")
                .addQueryParameter("state", "all")
                .addQueryParameter("sort", "updated")
                .addQueryParameter("direction", "desc")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", PER_PAGE)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();

        return executeAndParseListRequest(request, GithubPullRequest.class);
    }

    public GithubPullRequest getPullRequest(String repoOwner, String repoName, String prNumber) throws GithubClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("repos")
                .addPathSegments(repoOwner)
                .addPathSegment(repoName)
                .addPathSegment("pulls")
                .addPathSegment(prNumber)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return executeAndParseRequest(request, GithubPullRequest.class);
    }

    /**
     * Search for PRs updated within given range in given repo.
     */
    public List<GithubPullRequestSearchResult> searchPullRequests(String repoId, @Nonnull Instant from, @Nonnull Instant to, int pageNumber) throws GithubClientException {
        String query = String.format("repo:%s is:pr updated:%s..%s sort:updated-desc ", repoId, from, to);
        return searchPullRequests(query, null, null, pageNumber, SEARCH_PAGE_SIZE);
    }

    public Stream<GithubPullRequestSearchResult> searchPullRequestsAndStreamResults(String query, @Nullable String sort, @Nullable String order) {
        return PaginationUtils.streamThrowingRuntime(1, 1, pageNumber -> searchPullRequests(query, sort, order, pageNumber, SEARCH_PAGE_SIZE));
    }

    /**
     * Search for any PR using GitHub's query language.
     * Note query that the query must at least include "is:pr" or it will also return GitHub issues.
     * {@see https://docs.github.com/en/search-github/searching-on-github/searching-issues-and-pull-requests}
     */
    public List<GithubPullRequestSearchResult> searchPullRequests(String query, @Nullable String sort, @Nullable String order, int pageNumber, int pageSize) throws GithubClientException {
        HttpUrl.Builder url = baseUrlBuilder()
                .addPathSegment("search")
                .addPathSegment("issues")
                .addQueryParameter("q", query)
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(pageSize));
        if (StringUtils.isNoneEmpty(sort)) {
            url.addQueryParameter("sort", sort);
        }
        if (StringUtils.isNoneEmpty(order)) {
            url.addQueryParameter("order", order);
        }
        Request request = new Request.Builder()
                .url(url.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();

        GithubSearchResult<GithubPullRequestSearchResult> r = executeAndParseRequest(request,
                objectMapper.getTypeFactory().constructParametricType(GithubSearchResult.class, GithubPullRequestSearchResult.class));
        return r.getItems();
    }

    public Stream<GithubReview> streamReviews(String repoId, Integer prNumber) throws RuntimeStreamException {
        return stream(RuntimeStreamException.wrap(page -> {
            log.info("Fetching page {} of github reviews for repo={} pr={}", page, repoId, prNumber);
            return getReviews(repoId, prNumber, page);
        }));
    }


    // /repos/:owner/:repo/pulls/:pull_number/reviews
    public GithubPaginatedResponse<GithubReview> getReviews(String repoId, Integer prNumber, int pageNumber) throws GithubClientException {
        // TODO state / sort / pagination
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("repos")
                .addPathSegments(repoId) // (2 segments) ":owner/:repo"
                .addPathSegment("pulls")
                .addPathSegment(String.valueOf(prNumber))
                .addPathSegment("reviews")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", PER_PAGE)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return executeAndParseListRequest(request, GithubReview.class);
    }

    public Stream<GithubReview> streamIssueComments(String repoId, Integer issueNumber) throws RuntimeStreamException {
        return stream(RuntimeStreamException.wrap(page -> {
            log.info("Fetching page {} of github issues for repo={} pr/issue={}", page, repoId, issueNumber);
            return getIssueComments(repoId, issueNumber, page);
        }));
    }

    // There are 3 different type of comments in github: this is the one for issues
    // Documentation: https://docs.github.com/en/rest/guides/working-with-comments?apiVersion=2022-11-28
    // NOTE: Issue comments do not contain a commit_id field which is available through other comment types
    // We leave this field as blank for now because it doesn't seem to be used anywhere
    public GithubPaginatedResponse<GithubReview> getIssueComments(String repoId, Integer issueNumber, int pageNumber) throws GithubClientException {
        // /repos/:owner/:repo/issues/:issue_number/comments
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("repos")
                .addPathSegments(repoId) // (2 segments) ":owner/:repo"
                .addPathSegment("issues")
                .addPathSegment(String.valueOf(issueNumber))
                .addPathSegment("comments")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", PER_PAGE)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        GithubPaginatedResponse<GithubReview> comments = executeAndParseListRequest(request, GithubReview.class);
        // Issue comments do not have a "state" parameter, but we need this for some ETL queries
        return comments.toBuilder()
                .records(comments.getRecords().stream()
                        .map(githubReview -> githubReview.toBuilder().state("COMMENTED").build())
                        .collect(Collectors.toList()))
                .build();
    }

    public Stream<GithubApiCommit> streamPullRequestCommits(String repoId, Integer prNumber) throws RuntimeStreamException {
        return stream(RuntimeStreamException.wrap(page -> {
            log.info("Fetching page {} of github PR commits for repo={} pr={}", page, repoId, prNumber);
            return getPullRequestCommits(repoId, prNumber, page);
        }));
    }

    // GET /repos/:owner/:repo/pulls/:pull_number/commits
    public GithubPaginatedResponse<GithubApiCommit> getPullRequestCommits(String repoId, Integer prNumber, int pageNumber) throws GithubClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("repos")
                .addPathSegments(repoId) // (2 segments) ":owner/:repo"
                .addPathSegment("pulls")
                .addPathSegment(String.valueOf(prNumber))
                .addPathSegment("commits")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", PER_PAGE)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return executeAndParseListRequest(request, GithubApiCommit.class);
    }

    // GET /repos/:owner/:repo/commits/:commit-sha
    public GithubApiCommit getPullRequestMergeCommit(String repoId, String prMergeCommitSha) throws GithubClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("repos")
                .addPathSegments(repoId) // (2 segments) ":owner/:repo"
                .addPathSegment("commits")
                .addPathSegment(prMergeCommitSha)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return executeAndParseRequest(request, GithubApiCommit.class);
    }


    public Stream<GithubIssue> streamIssues(String repoId, Map<String, String> params) throws RuntimeStreamException {
        return stream(RuntimeStreamException.wrap(page -> {
            log.info("Fetching page {} of github issues for repo={}", page, repoId);
            return getIssues(repoId, params, page);
        }));
    }

    public GithubPaginatedResponse<GithubIssue> getIssues(String repoId, Map<String, String> params, int pageNumber) throws GithubClientException {
        /*
        PARAMS
        milestone 	integer or string 	If an integer is passed, it should refer to a milestone by its number field. If the string * is passed, issues with any milestone are accepted. If the string none is passed, issues without milestones are returned.
        state 	string 	Indicates the state of the issues to return. Can be either open, closed, or all. Default: open
        assignee 	string 	Can be the name of a user. Pass in none for issues with no assigned user, and * for issues assigned to any user.
        creator 	string 	The user that created the issue.
        mentioned 	string 	A user that's mentioned in the issue.
        labels 	string 	A list of comma separated label names. Example: bug,ui,@high
        sort 	string 	What to sort results by. Can be either created, updated, comments. Default: created
        direction 	string 	The direction of the sort. Can be either asc or desc. Default: desc
        since 	string 	Only issues updated at or after this time are returned. This is a timestamp in ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ.
         */
        var builder = baseUrlBuilder()
                .addPathSegment("repos")
                .addPathSegments(repoId) // (2 segments) ":owner/:repo"
                .addPathSegment("issues")
                .addQueryParameter("page", String.valueOf(pageNumber));
        MapUtils.emptyIfNull(params).entrySet().stream()
                .filter(e -> StringUtils.isNotEmpty(e.getValue()))
                .forEach(e -> builder.addQueryParameter(e.getKey(), e.getValue()));
        HttpUrl url = builder.build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return executeAndParseListRequest(request, GithubIssue.class);
    }

    public Stream<RepositoryCommit> streamCommitsWithoutFiles(String repoId, Instant from, Instant to) throws RuntimeStreamException {
        return stream(RuntimeStreamException.wrap(page -> {
            log.info("Fetching page {} of github commits without files for repo={}", page, repoId);
            return getCommitsWithoutFiles(repoId, from, to, page);
        }));
    }

    public GithubPaginatedResponse<RepositoryCommit> getCommitsWithoutFiles(String repoId, Instant from, Instant to,
                                                                            int pageNumber) throws GithubClientException {
        var builder = baseUrlBuilder()
                .addPathSegment("repos")
                .addPathSegments(repoId) // (2 segments) ":owner/:repo"
                .addPathSegment("commits")
                .addQueryParameter("since", from.toString())
                .addQueryParameter("until", to.toString())
                .addQueryParameter("page", String.valueOf(pageNumber));
        HttpUrl url = builder.build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return executeAndParseListRequest(request, RepositoryCommit.class);
    }

    public Stream<GithubApiCommit> searchCommitAndStreamResults(String query, @Nullable String sort, @Nullable String order) {
        return PaginationUtils.streamThrowingRuntime(1, 1, pageNumber -> searchCommits(query, sort, order, pageNumber, SEARCH_PAGE_SIZE));
    }

    /**
     * Search for any commit using GitHub's query language.
     * {@see https://docs.github.com/en/search-github/searching-on-github/searching-commits}
     */
    public List<GithubApiCommit> searchCommits(String query, @Nullable String sort, @Nullable String order, int pageNumber, int pageSize) throws GithubClientException {
        HttpUrl.Builder url = baseUrlBuilder()
                .addPathSegment("search")
                .addPathSegment("commits")
                .addQueryParameter("q", query)
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(pageSize));
        if (StringUtils.isNoneEmpty(sort)) {
            url.addQueryParameter("sort", sort);
        }
        if (StringUtils.isNoneEmpty(order)) {
            url.addQueryParameter("order", order);
        }
        Request request = new Request.Builder()
                .url(url.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();

        GithubSearchResult<GithubApiCommit> r = executeAndParseRequest(request,
                objectMapper.getTypeFactory().constructParametricType(GithubSearchResult.class, GithubApiCommit.class));
        return r.getItems();
    }

    public Stream<GithubApiIssueEvent> streamIssueTimelineEvents(String repoId, int issueNumber) throws RuntimeStreamException {
        return stream(RuntimeStreamException.wrap(page -> {
            log.info("Fetching page {} of github issue timeline events for repo={}, issue={}", page, repoId, issueNumber);
            return getIssueTimelineEvents(repoId, issueNumber, page);
        }));
    }

    public GithubPaginatedResponse<GithubApiIssueEvent> getIssueTimelineEvents(String repoId, int issueNumber, int pageNumber) throws GithubClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("repos")
                .addPathSegments(repoId) // (2 segments) ":owner/:repo"
                .addPathSegment("issues")
                .addPathSegment(String.valueOf(issueNumber))
                .addPathSegment("timeline")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", PER_PAGE)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .header(ClientConstants.ACCEPT, "application/vnd.github.mockingbird-preview")
                .get()
                .build();
        return executeAndParseListRequest(request, GithubApiIssueEvent.class);
    }

    public GithubPaginatedResponse<GithubTag> getGithubTags(String owner, String repo, int page, int pageSize) throws GithubClientException {
        String countPerPage = (pageSize == 0) ? PER_PAGE : String.valueOf(pageSize);
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("repos")
                .addPathSegments(owner)
                .addPathSegment(repo)
                .addPathSegment("tags")
                .addQueryParameter("page", String.valueOf(page))
                .addQueryParameter("per_page", countPerPage)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return executeAndParseListRequest(request, GithubTag.class);
    }


    public Stream<GithubTag> streamTags(String owner, String repo, int perPage) {
        return stream(RuntimeStreamException.wrap(page -> {
            try {
                return getGithubTags(owner, repo, page, perPage);
            } catch (GithubClientException e) {
                log.warn("Failed to get tags for repo" + repo + " after page " + page, e);
                throw e;
            }
        }));
    }

    public Stream<GithubWebhookResponse> streamWebhooks(String org, IntegrationKey integrationKey) throws RuntimeStreamException {
        return stream(RuntimeStreamException.wrap(page -> {
            log.info("Fetching page {} of webhook for organization={}, integration={}", page, org, integrationKey);
            return listWebhook(org, page);
        }));
    }

    public GithubPaginatedResponse<GithubWebhookResponse> listWebhook(String org, int pageNumber) throws GithubClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("orgs")
                .addPathSegment(org)
                .addPathSegment("hooks")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", "100")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .header(ClientConstants.ACCEPT, "application/vnd.github.v3+json")
                .get()
                .build();
        return executeAndParseListRequest(request, GithubWebhookResponse.class);
    }

    public GithubWebhookResponse createWebhook(String org, GithubWebhookRequest webhook) throws GithubClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("orgs")
                .addPathSegment(org)
                .addPathSegment("hooks")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .header(ClientConstants.ACCEPT, "application/vnd.github.v3+json")
                .post(clientHelper.createJsonRequestBody(webhook))
                .build();
        return executeAndParseRequest(request, GithubWebhookResponse.class);
    }

    public GithubWebhookResponse updateWebhook(String org, Integer hookId, GithubWebhookRequest webhook) throws GithubClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("orgs")
                .addPathSegment(org)
                .addPathSegment("hooks")
                .addPathSegment(String.valueOf(hookId))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .header(ClientConstants.ACCEPT, "application/vnd.github.v3+json")
                .patch(clientHelper.createJsonRequestBody(webhook))
                .build();
        return executeAndParseRequest(request, GithubWebhookResponse.class);
    }

    // region utils ---

    private <T> BodyAndHeaders<String> executeRequest(Request request) throws GithubClientException {
        return executeRequest(request, false);
    }

    private <T> BodyAndHeaders<String> executeRequest(Request request, boolean useJwtClient) throws GithubClientException {
        rateLimiter.waitForTurn();

        if (useJwtClient && jwtClientHelper == null) {
            throw new GithubClientException("JWT client is not configured. " +
                    "Please ensure that this integration authenticated using github apps.");
        }

        ClientHelper<GithubClientException> clientHelperToUse = useJwtClient ? jwtClientHelper : clientHelper;
        return clientHelperToUse.executeRequestWithHeaders(request);
    }

    private <T> T executeAndParseRequest(Request request, Class<T> clazz) throws GithubClientException {
        BodyAndHeaders<String> stringBodyAndHeaders = executeRequest(request);
        T record = clientHelper.parseResponse(stringBodyAndHeaders.getBody(), clazz);
        return record;
    }

    private <T> T executeAndParseRequest(Request request, JavaType type) throws GithubClientException {
        BodyAndHeaders<String> stringBodyAndHeaders = executeRequest(request);
        T record = clientHelper.parseResponse(stringBodyAndHeaders.getBody(), type);
        return record;
    }

    private <T> GithubPaginatedResponse<T> executeAndParseListRequest(Request request, Class<T> clazz) throws GithubClientException {
        return executeAndParseListRequest(request, clazz, false);
    }

    private <T> GithubPaginatedResponse<T> executeAndParseListRequest(Request request, Class<T> clazz, boolean useJwtClient) throws GithubClientException {
        BodyAndHeaders<String> stringBodyAndHeaders = executeRequest(request, useJwtClient);
        List<T> records;
        if (stringBodyAndHeaders.getBody().equals("") && stringBodyAndHeaders.getCode() == 204) {
            records = List.of();
        } else {
            records = clientHelper.parseResponse(stringBodyAndHeaders.getBody(),
                    objectMapper.getTypeFactory().constructCollectionLikeType(List.class, clazz));
        }
        return GithubPaginatedResponse.<T>builder()
                .records(records)
                .linkHeader(stringBodyAndHeaders.getHeader(LINK_HEADER))
                .build();
    }

    public <T> Stream<T> stream(Function<Integer, GithubPaginatedResponse<T>> pageSupplier) {
        MutableBoolean hasNext = new MutableBoolean(true);
        return PaginationUtils.stream(1, 1, page -> {
            if (hasNext.isFalse()) {
                return Collections.emptyList();
            }
            GithubPaginatedResponse<T> response = pageSupplier.apply(page);
            if (response == null) {
                return Collections.emptyList();
            }
            if (!hasNextPredicate(response.getLinkHeader())) {
                hasNext.setFalse();
            }
            return response.getRecords();
        });
    }


    private boolean hasNextPredicate(String linkHeader) {
        if (Strings.isEmpty(linkHeader)) {
            return true; // when in doubt, just get next page
        }
        // examples
        // has next: <https://api.github.com/repositories/205456849/pulls?state=close&page=6>; rel="prev", <https://api.github.com/repositories/205456849/pulls?state=close&page=8>; rel="next", <https://api.github.com/repositories/205456849/pulls?state=close&page=8>; rel="last", <https://api.github.com/repositories/205456849/pulls?state=close&page=1>; rel="first"
        // last: <https://api.github.com/repositories/205456849/pulls?state=close&page=8>; rel="prev", <https://api.github.com/repositories/205456849/pulls?state=close&page=8>; rel="last", <https://api.github.com/repositories/205456849/pulls?state=close&page=1>; rel="first"
        return linkHeader.contains("; rel=\"next\", ");
    }

    // endregion

    public Stream<GithubRepository> searchRepositories(String org, String repoName) {
        try {
            return PaginationUtils.stream(1, 1, page -> {
                try {
                    return searchRepositories(org, repoName, page, Integer.valueOf(PER_PAGE)).getItems();
                } catch (GithubClientException e) {
                    log.warn("Failed to get repo after page " + page, e);
                    return List.of();
                }
            });
        } catch (RuntimeStreamException e) {
            log.warn("Failed to list all repos", e);
            return Stream.of();
        }
    }

    public GithubSearchResult<GithubRepository> searchRepositories(String org, String repoName, Integer pageNumber, Integer pageSize) throws GithubClientException {
        pageNumber = Math.max(1, pageNumber);
        pageSize = pageSize == 0 ? Integer.valueOf(PER_PAGE) : pageSize;
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("search")
                .addPathSegment("repositories")
                .addEncodedQueryParameter("q", repoName + "+in:name+org:" + org + "+fork:true")
                .addQueryParameter("sort", "updated")
                .addQueryParameter("direction", "desc")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(pageSize))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();

        return executeAndParseRequest(request, objectMapper.getTypeFactory().constructParametricType(GithubSearchResult.class, GithubRepository.class));
    }

    public Stream<GithubRepository> streamRepositories(String org) {
        return Stream.concat(
                streamRepositories(org, false),
                streamRepositories(org, true));
    }

    public Stream<GithubRepository> streamRepositories(String org, boolean internalRepos) {
        return stream(RuntimeStreamException.wrap(page -> {
            try {
                return getRepositories(org, internalRepos, page, Integer.parseInt(PER_PAGE));
            } catch (Exception e) {
                log.warn("Failed to get repo after page " + page, e);
                throw e;
            }
        }));
    }

    public GithubPaginatedResponse<GithubRepository> getRepositories(String org, boolean internalRepos, int pageNumber, int pageSize) throws GithubClientException {
        // /orgs/:orgId/repos
        pageNumber = Math.max(1, pageNumber);
        pageSize = pageSize == 0 ? Integer.parseInt(PER_PAGE) : pageSize;
        HttpUrl.Builder url = baseUrlBuilder()
                .addPathSegment("orgs")
                .addPathSegment(org)
                .addPathSegment("repos")
                .addQueryParameter("sort", "updated")
                .addQueryParameter("direction", "desc")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(pageSize));
        if (internalRepos) {
            // due to some quirk of the API, the default type "all", does not include "internal" repos
            // therefore, we have to make 1 call for internal repos + 1 more call for all other repos
            url.addQueryParameter("type", "internal");
        }
        Request request = new Request.Builder()
                .url(url.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();

        return executeAndParseListRequest(request, GithubRepository.class);
    }

    public Stream<GithubOrganization> streamOrganizations() throws RuntimeStreamException {
        return stream(RuntimeStreamException.wrap(page -> {
            try {
                return getOrganizations(page, Integer.parseInt(PER_PAGE));
            } catch (Exception e) {
                log.warn("Failed to get org after page " + page, e);
                throw e;
            }
        }));
    }

    public GithubPaginatedResponse<GithubOrganization> getOrganizations(int pageNumber, int pageSize) throws GithubClientException {
        pageNumber = Math.max(1, pageNumber);
        pageSize = pageSize == 0 ? Integer.parseInt(PER_PAGE) : pageSize;
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("user")
                .addPathSegment("orgs")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(pageSize))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();

        return executeAndParseListRequest(request, GithubOrganization.class);
    }

    public Stream<GithubRepository> streamInstallationRepositories() throws RuntimeStreamException {
        return stream(RuntimeStreamException.wrap(page -> {
            try {
                GithubInstallationRepositoriesResponse installationRepos = getInstallationRepositories(page, Integer.parseInt(PER_PAGE));
                return GithubPaginatedResponse.<GithubRepository>builder()
                        .linkHeader(installationRepos.getLinkHeader())
                        .records(installationRepos.getRepositories())
                        .build();
            } catch (Exception e) {
                log.warn("Failed to get installation repos after page " + page, e);
                throw e;
            }
        }));
    }

    /**
     * In GitHub Apps, there is no authenticated user, so we have to get the list of repos directly from the app installation
     */
    public GithubInstallationRepositoriesResponse getInstallationRepositories(int pageNumber, int pageSize) throws GithubClientException {
        // /installation/repositories
        pageNumber = Math.max(1, pageNumber);
        pageSize = pageSize == 0 ? Integer.parseInt(PER_PAGE) : pageSize;
        HttpUrl.Builder url = baseUrlBuilder()
                .addPathSegment("installation")
                .addPathSegment("repositories")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(pageSize));
        Request request = new Request.Builder()
                .url(url.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();

        BodyAndHeaders<String> responseBodyAndHeaders = executeRequest(request);
        GithubInstallationRepositoriesResponse response = clientHelper.parseResponse(responseBodyAndHeaders.getBody(), GithubInstallationRepositoriesResponse.class);
        return response.toBuilder()
                .linkHeader(responseBodyAndHeaders.getHeader(LINK_HEADER))
                .build();
    }

    public GithubCommit getCommit(String repoId, String commitSha) throws GithubClientException {
        // https://docs.github.com/en/rest/commits/commits#get-a-commit
        // /repos/:id/commits/:sha
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("repos")
                .addPathSegments(repoId)
                .addPathSegment("commits")
                .addPathSegment(commitSha)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        GithubApiCommit apiCommit = clientHelper.executeAndParse(request, GithubApiCommit.class);
        return GithubConverters.parseGithubApiCommit(apiCommit);
    }

    public Stream<GithubApiRepoEvent> streamRepoEvents(String repoId) throws RuntimeStreamException {
        return stream(RuntimeStreamException.wrap(page -> {
            try {
                return getRepoEvents(repoId, page, Integer.parseInt(PER_PAGE));
            } catch (Exception e) {
                log.warn("Failed to get repo events for repo={} after page {}", repoId, page, e);
                throw e;
            }
        }));
    }

    public GithubPaginatedResponse<GithubApiRepoEvent> getRepoEvents(String repoId, int pageNumber, int pageSize) throws GithubClientException {
        // /repos/:id/events

        pageNumber = Math.max(1, pageNumber);
        pageSize = pageSize == 0 ? Integer.parseInt(PER_PAGE) : pageSize;
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("repos")
                .addPathSegments(repoId)
                .addPathSegment("events")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(pageSize))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();

        return executeAndParseListRequest(request, GithubApiRepoEvent.class);
    }

    public Stream<GithubIssueEvent> streamIssueEvents(String repoId) throws RuntimeStreamException {
        return stream(RuntimeStreamException.wrap(page -> {
            try {
                return getIssueEvents(repoId, page, Integer.parseInt(PER_PAGE));
            } catch (Exception e) {
                log.warn("Failed to get issue events for repo={} after page {}", repoId, page, e);
                throw e;
            }
        }));
    }

    public GithubPaginatedResponse<GithubIssueEvent> getIssueEvents(String repoId, int pageNumber, int pageSize) throws GithubClientException {
        // /repos/:user/:repo/issues/events

        pageNumber = Math.max(1, pageNumber);
        pageSize = pageSize == 0 ? Integer.parseInt(PER_PAGE) : pageSize;
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("repos")
                .addPathSegments(repoId)
                .addPathSegment("issues")
                .addPathSegment("events")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(pageSize))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();

        return executeAndParseListRequest(request, GithubIssueEvent.class);
    }

    public GithubRepository getRepository(String repoId) throws GithubClientException {
        // /repo/:id
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("repos")
                .addPathSegments(repoId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request, GithubRepository.class);
    }

    public Map<String, Long> getRepositoryLanguages(String repoId) throws GithubClientException {
        // /repos/:id/languages
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("repos")
                .addPathSegments(repoId)
                .addPathSegment("languages")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Long.class));
    }

    public Stream<GithubContributor> streamRepoContributors(String fullRepoName) throws RuntimeStreamException {
        return stream(RuntimeStreamException.wrap(page -> {
            try {
                return getRepoContributors(fullRepoName, page, Integer.parseInt(PER_PAGE));
            } catch (Exception e) {
                log.warn("Failed to get repo contributors for repo={} after page {}", fullRepoName, page, e);
                throw e;
            }
        }));
    }

    public GithubPaginatedResponse<GithubContributor> getRepoContributors(String fullRepoName, int pageNumber, int pageSize) throws GithubClientException {
        // /repos/:owner/:repo/contributors
        pageNumber = Math.max(1, pageNumber);
        pageSize = pageSize == 0 ? Integer.parseInt(PER_PAGE) : pageSize;
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("repos")
                .addPathSegments(fullRepoName)
                .addPathSegment("contributors")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(pageSize))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();

        return executeAndParseListRequest(request, GithubContributor.class);
    }

    public Stream<GithubRepository> streamUserRecentRepos(String userLogin, Boolean swallowErrors) throws RuntimeStreamException {
        return PaginationUtils.stream("", continuationToken -> {
            var emptyCursorPageData = PaginationUtils.CursorPageData.<GithubRepository>builder()
                    .data(List.of())
                    .cursor(null)
                    .build();
            if (continuationToken == null) {
                return emptyCursorPageData;
            }
            try {
                GithubUserRepos response = getUserRecentRepos(userLogin, continuationToken, Integer.parseInt(PER_PAGE));
                return PaginationUtils.CursorPageData.<GithubRepository>builder()
                        .data(response.getRepos())
                        .cursor(response.getEndCursor())
                        .build();
            } catch (GithubClientException e) {
                log.warn("Failed to get recent user repos for {} after cursor {}", userLogin, continuationToken, e);
                if (!swallowErrors) {
                    throw new RuntimeException(e);
                }
                return emptyCursorPageData;
            }
        });
    }

    public GithubUserRepos getUserRecentRepos(String userLogin, String endCursor, int pageSize) throws GithubClientException {
        String paginationClause = String.format("first: %s", pageSize);
        if (StringUtils.isNotEmpty(endCursor)) {
            paginationClause = paginationClause + String.format(", after: \"%s\"", endCursor);
        }
        String query = "query { \n" +
                String.format("  user(login:\"%s\") {\n", userLogin) +
                "    login\n" +
                "    repositoriesContributedTo(" + paginationClause + ", contributionTypes: [COMMIT, ISSUE, PULL_REQUEST, PULL_REQUEST_REVIEW]) {\n" +
                "      totalCount\n" +
                "      nodes {\n" +
                "        owner {\n" +
                "          login\n" +
                "        }\n" +
                "        nameWithOwner\n" +
                "      }\n" +
                "      pageInfo {\n" +
                "        endCursor\n" +
                "        hasNextPage\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        GraphQlResponse graphQlResponse = queryGraphQl(query);
        if (CollectionUtils.isNotEmpty(graphQlResponse.getErrors())) {
            throw new GithubClientException("Failed to get user recent repos. Errors: " + graphQlResponse.getErrors());
        }
        return objectMapper.convertValue(graphQlResponse, GithubUserRepos.class);
    }

    public Stream<GithubUser> streamOrgUsers(String organization, Boolean swallowErrors) throws RuntimeStreamException {
        return PaginationUtils.stream("", continuationToken -> {
            var emptyCursorPageData = PaginationUtils.CursorPageData.<GithubUser>builder()
                    .data(List.of())
                    .cursor(null)
                    .build();
            if (continuationToken == null) {
                return emptyCursorPageData;
            }
            try {
                GithubUserGraphQlResponse response = getOrgUsers(organization, continuationToken, Integer.parseInt(PER_PAGE));
                return PaginationUtils.CursorPageData.<GithubUser>builder()
                        .data(response.getUsers())
                        .cursor(response.getEndCursor())
                        .build();
            } catch (GithubClientException e) {
                log.warn("Failed to get org users for org {} after cursor {}", organization, continuationToken, e);
                if (!swallowErrors) {
                    throw new RuntimeException(e);
                }
                return emptyCursorPageData;
            }
        });
    }

    public GithubUserGraphQlResponse getOrgUsers(String organization, String endCursor, int pageSize) throws GithubClientException {
        String paginationClause = String.format("first: %s", pageSize);
        if (StringUtils.isNotEmpty(endCursor)) {
            paginationClause = paginationClause + String.format(", after: \"%s\"", endCursor);
        }
        String query = "query { \n" +
                String.format("  organization(login: \"%s\") {\n", organization) +
                "    membersWithRole(" + paginationClause + ") {\n" +
                "      totalCount\n" +
                "      edges {\n" +
                "        node {\n" +
                String.format("          organizationVerifiedDomainEmails(login: \"%s\")\n", organization) +
                "          login\n" +
                "          name\n" +
                "          email\n" +
                "        }\n" +
                "      }\n" +
                "      pageInfo {\n" +
                "        endCursor\n" +
                "        hasNextPage\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        GraphQlResponse graphQlResponse = queryGraphQl(query);
        if (CollectionUtils.isNotEmpty(graphQlResponse.getErrors())) {
            throw new GithubClientException("Failed to get user recent repos. Errors: " + graphQlResponse.getErrors());
        }
        return objectMapper.convertValue(graphQlResponse, GithubUserGraphQlResponse.class);
    }

    // This only works for github apps
    public Stream<GithubOrganization> streamAppInstallationOrgs() throws RuntimeStreamException {
        return stream(RuntimeStreamException.wrap(page -> {
            try {
                return getAppInstallationOrgs(page, Integer.parseInt(PER_PAGE));
            } catch (Exception e) {
                log.warn("Failed to get installations orgs after page {}", page, e);
                throw e;
            }
        }));
    }

    public GithubPaginatedResponse<GithubOrganization> getAppInstallationOrgs(int pageNumber, int pageSize) throws GithubClientException {
        GithubPaginatedResponse<GithubAppInstallation> installations = getAppInstallations(pageNumber, pageSize);
        return GithubPaginatedResponse.<GithubOrganization>builder()
                .linkHeader(installations.getLinkHeader())
                .records(installations.getRecords().stream()
                        .map(installation -> installation.getAccount().toOrganization())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                )
                .build();
    }

    public Stream<GithubAppInstallation> streamAppInstallations() throws RuntimeStreamException {
        return stream(RuntimeStreamException.wrap(page -> {
            try {
                return getAppInstallations(page, Integer.parseInt(PER_PAGE));
            } catch (Exception e) {
                log.warn("Failed to get installations after page {}", page, e);
                throw e;
            }
        }));
    }

    public GithubPaginatedResponse<GithubAppInstallation> getAppInstallations(int pageNumber, int pageSize) throws GithubClientException {
        // /app/installations
        pageNumber = Math.max(1, pageNumber);
        pageSize = pageSize == 0 ? Integer.parseInt(PER_PAGE) : pageSize;
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("app")
                .addPathSegments("installations")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(pageSize))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.GITHUB_APPLICATION_JSON.toString())
                .get()
                .build();
        return executeAndParseListRequest(request, GithubAppInstallation.class, true);
    }

    public Stream<GithubActionsWorkflowRun> streamWorkflowRuns(String repoFullName, int perPage) {
        return stream(page -> {
            try {
                return getWorkflowRuns(repoFullName, page, perPage);
            } catch (GithubClientException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Stream<GithubActionsWorkflowRunJob> streamWorkflowRunJobs(String repoFullName, Long workflowRunId, int perPage) {
        return stream(page -> {
            try {
                return getWorkflowRunJobs(repoFullName, workflowRunId, page, perPage);
            } catch (GithubClientException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Stream<GithubActionsWorkflow> streamWorkflows(String repoFullName, int perPage) {
        return stream(page -> {
            try {
                return getWorkflows(repoFullName, page, perPage);
            } catch (GithubClientException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Get workflow run jobs executed within the given repo and workflow run.
     * If repository is private you must use an access token with the repo scope.
     * GitHub Apps must have the actions:read permission to use this endpoint
     *
     * @param repoFullName  the repo full name having organization name followed by repository name and seperated by /
     * @param workflowRunId the workflow run id under which jobs are executed
     * @param page          1-based page number
     * @param pageSize      the page size for paginated response
     * @return the workflow run jobs
     * @throws GithubClientException the Github client exception
     */
    // https://api.github.com/repos/Krina-Test-Org/Github-Actions-K/actions/runs/5795418557/jobs?page=1&per_page=10
    public GithubPaginatedResponse<GithubActionsWorkflowRunJob> getWorkflowRunJobs(String repoFullName, Long workflowRunId, int page, int pageSize) throws GithubClientException {
        // /repos/{repo_full_name}/actions/runs/{runId}/jobs
        /**
         * Possible Query Params
         * filter  string  Filters jobs by their completed_at timestamp
         *                 latest  returns jobs from the most recent execution of the workflow run.
         *                 all  returns all jobs for a workflow run, including from old executions of the workflow run.
         *                 Default: latest. Can be one of: latest, all
         * page  integer Page number of the results to fetch. Default: 1
         * per_page  integer  The number of results per page (max 100). Default: 30
         */
        String countPerPage = (pageSize == 0) ? PER_PAGE : String.valueOf(pageSize);
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("repos")
                .addPathSegments(repoFullName)
                .addPathSegment("actions")
                .addPathSegments("runs")
                .addPathSegments(workflowRunId.toString())
                .addPathSegments("jobs")
                .addQueryParameter("page", String.valueOf(page))
                .addQueryParameter("per_page", countPerPage)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<GithubActionsWorkflowRunJobPaginatedResponse> response = clientHelper.executeAndParseWithHeaders(request, GithubActionsWorkflowRunJobPaginatedResponse.class);
        return GithubPaginatedResponse.<GithubActionsWorkflowRunJob>builder()
                .records(response.getBody().getJobs())
                .linkHeader(response.getHeader(LINK_HEADER))
                .build();
    }


    /**
     * Get workflow runs done within the given repo.
     * If repository is private you must use an access token with the repo scope.
     * GitHub Apps must have the actions:read permission to use this endpoint
     *
     * @param repoFullName the repo full name having organization name followed by repository name and seperated by /
     * @param page         1-based page number
     * @param pageSize     the page size for paginated response
     * @return the workflow runs
     * @throws GithubClientException the Github client exception
     */
    // https://api.github.com/repos/Krina-Test-Org/Github-Actions-K/actions/runs?page=1&per_page=2
    public GithubPaginatedResponse<GithubActionsWorkflowRun> getWorkflowRuns(String repoFullName, int page, int pageSize) throws GithubClientException {
        // /repos/{repo_full_name}/actions/runs
        /*
         * Possible Query Params
         * actor  string  user who created the push associated with the check suite or workflow run.
         * branch  string  workflow runs associated with a branch. Use the name of the branch of the push
         * event  string  workflow run triggered by the specific event like push, pull_request or issue
         * status  string  workflow runs with the check run status or conclusion that you specify. Can be one of: completed, action_required, cancelled, failure, neutral, skipped, stale, success, timed_out, in_progress, queued, requested, waiting, pending
         * page  integer Page number of the results to fetch. Default: 1
         * per_page  integer  The number of results per page (max 100). Default: 30
         * created  string  Returns workflow runs created within the given date-time range
         * exclude_pull_requests  boolean  If true pull requests are omitted from the response (empty array). Default: false
         * check_suite_id  integer  Returns workflow runs with the check_suite_id that you specify
         * head_sha  string  Only returns workflow runs that are associated with the specified head_sha
         */
        String countPerPage = (pageSize == 0) ? PER_PAGE : String.valueOf(pageSize);
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("repos")
                .addPathSegments(repoFullName)
                .addPathSegment("actions")
                .addPathSegments("runs")
                .addQueryParameter("page", String.valueOf(page))
                .addQueryParameter("per_page", countPerPage)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<GithubActionsWorkflowRunPaginatedResponse> response = clientHelper.executeAndParseWithHeaders(request, GithubActionsWorkflowRunPaginatedResponse.class);
        return GithubPaginatedResponse.<GithubActionsWorkflowRun>builder()
                .records(response.getBody().getWorkflowRuns())
                .linkHeader(response.getHeader(LINK_HEADER))
                .build();
    }

    /**
     * Get all workflow details configured within the given repo.
     * If repository is private you must use an access token with the repo scope.
     * GitHub Apps must have the actions:read permission to use this endpoint
     *
     * @param repoFullName the repo full name having organization name followed by repository name and seperated by /
     * @param page         1-based page number
     * @param pageSize     the page size for paginated response
     * @return the workflows
     * @throws GithubClientException the Github client exception
     */
    // https://api.github.com/repos/Krina-Test-Org/Github-Actions-K/actions/workflows?page=1&per_page=2
    public GithubPaginatedResponse<GithubActionsWorkflow> getWorkflows(String repoFullName, int page, int pageSize) throws GithubClientException {
        // /repos/{repo_full_name}/actions/workflows
        /*
         * Possible Query Params
         * page  integer Page number of the results to fetch. Default: 1
         * per_page  integer  The number of results per page (max 100). Default: 30
         */
        String countPerPage = (pageSize == 0) ? PER_PAGE : String.valueOf(pageSize);
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("repos")
                .addPathSegments(repoFullName)
                .addPathSegment("actions")
                .addPathSegments("workflows")
                .addQueryParameter("page", String.valueOf(page))
                .addQueryParameter("per_page", countPerPage)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<GithubActionsWorkflowPaginatedResponse> response = clientHelper.executeAndParseWithHeaders(request, GithubActionsWorkflowPaginatedResponse.class);
        return GithubPaginatedResponse.<GithubActionsWorkflow>builder()
                .records(response.getBody().getWorkflows())
                .linkHeader(response.getHeader(LINK_HEADER))
                .build();
    }
}