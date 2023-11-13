package io.levelops.integrations.azureDevops.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.client.exceptions.HttpException;
import io.levelops.commons.client.exceptions.NoContentException;
import io.levelops.integrations.azureDevops.models.Account;
import io.levelops.integrations.azureDevops.models.AccountResponse;
import io.levelops.integrations.azureDevops.models.AzureDevopsPipelineRunStageStep;
import io.levelops.integrations.azureDevops.models.AzureDevopsRelease;
import io.levelops.integrations.azureDevops.models.Branch;
import io.levelops.integrations.azureDevops.models.BranchResponse;
import io.levelops.integrations.azureDevops.models.BuildChange;
import io.levelops.integrations.azureDevops.models.BuildChangeResponse;
import io.levelops.integrations.azureDevops.models.BuildResponse;
import io.levelops.integrations.azureDevops.models.BuildTimelineResponse;
import io.levelops.integrations.azureDevops.models.Change;
import io.levelops.integrations.azureDevops.models.ChangeSet;
import io.levelops.integrations.azureDevops.models.ChangeSetChange;
import io.levelops.integrations.azureDevops.models.ChangeSetChangeResponse;
import io.levelops.integrations.azureDevops.models.ChangeSetResponse;
import io.levelops.integrations.azureDevops.models.ChangeSetWorkitem;
import io.levelops.integrations.azureDevops.models.ChangeSetWorkitemResponse;
import io.levelops.integrations.azureDevops.models.ClassificationNode;
import io.levelops.integrations.azureDevops.models.Comment;
import io.levelops.integrations.azureDevops.models.CommentResponse;
import io.levelops.integrations.azureDevops.models.Commit;
import io.levelops.integrations.azureDevops.models.CommitChangesResponse;
import io.levelops.integrations.azureDevops.models.CommitResponse;
import io.levelops.integrations.azureDevops.models.Iteration;
import io.levelops.integrations.azureDevops.models.IterationResponse;
import io.levelops.integrations.azureDevops.models.Label;
import io.levelops.integrations.azureDevops.models.LabelResponse;
import io.levelops.integrations.azureDevops.models.Pipeline;
import io.levelops.integrations.azureDevops.models.PipelineResponse;
import io.levelops.integrations.azureDevops.models.Profile;
import io.levelops.integrations.azureDevops.models.ProjectPropertiesResponse;
import io.levelops.integrations.azureDevops.models.ProjectProperty;
import io.levelops.integrations.azureDevops.models.ProjectResponse;
import io.levelops.integrations.azureDevops.models.PullRequest;
import io.levelops.integrations.azureDevops.models.PullRequestHistory;
import io.levelops.integrations.azureDevops.models.PullRequestHistoryResponse;
import io.levelops.integrations.azureDevops.models.PullRequestResponse;
import io.levelops.integrations.azureDevops.models.ReleaseResponse;
import io.levelops.integrations.azureDevops.models.Repository;
import io.levelops.integrations.azureDevops.models.RepositoryResponse;
import io.levelops.integrations.azureDevops.models.RunResponse;
import io.levelops.integrations.azureDevops.models.Tag;
import io.levelops.integrations.azureDevops.models.TagResponse;
import io.levelops.integrations.azureDevops.models.Team;
import io.levelops.integrations.azureDevops.models.TeamResponse;
import io.levelops.integrations.azureDevops.models.TeamSetting;
import io.levelops.integrations.azureDevops.models.WorkItem;
import io.levelops.integrations.azureDevops.models.WorkItemField;
import io.levelops.integrations.azureDevops.models.WorkItemFieldResponse;
import io.levelops.integrations.azureDevops.models.WorkItemHistory;
import io.levelops.integrations.azureDevops.models.WorkItemHistoryResponse;
import io.levelops.integrations.azureDevops.models.WorkItemQueryResult;
import io.levelops.integrations.azureDevops.models.WorkItemResponse;
import io.levelops.integrations.azureDevops.models.WorkItemType;
import io.levelops.integrations.azureDevops.models.WorkItemTypeCategory;
import io.levelops.integrations.azureDevops.models.WorkItemTypeCategoryResponse;
import io.levelops.integrations.azureDevops.models.WorkItemTypeResponse;
import io.levelops.integrations.azureDevops.models.WorkItemTypeState;
import io.levelops.integrations.azureDevops.models.WorkItemTypeStateResponse;
import io.levelops.rate_limiter.IntervalRateLimiter;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.levelops.integrations.azureDevops.client.AzureDevopsClientFactory.parseIngestWorkItemComments;

@Log4j2
public class AzureDevopsClient {
    public static final Predicate<Response> AZURE_FAILED_RESPONSE_PREDICATE = response -> !response.isSuccessful() || (response.code() == 203);

    public static final String LIST_PIPELINES_API = "_apis/pipelines/";
    public static final String LIST_PIPELINE_RUNS_API = "/runs";
    public static final String LIST_RELEASES_API = "_apis/release/releases";
    public static final String BUILD_TIMELINE_API = "/timeline";
    public static final String LIST_PROJECTS_API = "_apis/projects";
    public static final String LIST_BUILDS_API = "_apis/build/builds";
    public static final String LIST_REPOSITORIES_API = "_apis/git/repositories/";
    public static final String LIST_COMMITS_API = "commits";
    public static final String LIST_PULLREQUESTS_API = "pullrequests";
    public static final String LIST_ANNOTATED_API = "annotatedtags";
    public static final String LIST_COMMITCHANGES_API = "changes";
    public static final String LIST_TEAMS_API = "teams";
    public static final String LIST_BOARDS_API = "_apis/work/boards";
    public static final String LIST_ITERATIONS_API = "_apis/work/teamsettings/iterations";
    public static final String LIST_TASKBOARD_WORKITEMS_API = "_apis/work/taskboardworkitems";
    public static final String LIST_WORKITEMS_API = "_apis/wit/workitems";
    public static final String LIST_WORKITEMS_FIELDS_API = "_apis/wit/fields";
    public static final String LIST_WIQL_API = "_apis/wit/wiql";
    public static final String LIST_BACKLOGS_API = "_apis/work/backlogs";
    public static final String LIST_WORKITEMTYPE_CATEGORIES_API = "_apis/wit/workitemtypecategories";
    public static final String LIST_WORKITEM_COMMENTS_API = "comments";
    public static final String LIST_WORKITEM_COMMENTS_ORDER_API = "order";
    public static final String WORKITEM_COMMENTS_ASCENDING_ORDER_API = "asc";
    public static final String LIST_WORKITEMTYPES_API = "_apis/wit/workitemtypes";
    public static final String LIST_WORKITEM_BACKLOGS_API = "workItems";
    private static final String LIST_PROPERTIES_PROJECT_API = "properties";
    private static final String WORKITEMS_EXPAND_RESPONSE = "$expand";


    private static final String API_VERSION_QUERY_PARAM = "api-version";
    private static final String API_VERSION_VALUE = "6.1-preview.1";
    private static final String TAGS_API_VERSION_VALUE = "5.1-preview.1";
    private static final String PROJECT_API_VERSION_VALUE = "6.1-preview.4";
    private static final String BUILDS_API_VERSION_VALUE = "6.1-preview.6";
    private static final String OLD_API_VERSION_VALUE = "6.0";
    private static final String WORKITEMS_API_VERSION_VALUE = "6.1-preview.3";
    private static final String PROJECT_PROPERTIES_API_VERSION_VALUE = "6.0-preview.1";
    private static final String WORKITEMS_COMMENTS_API_VERSION_VALUE = "6.0-preview.3";
    private static final String ITERATIONS_VERSION_VALUE = "4.1";


    private static final String CONTINUATION_TOKEN_QUERY_PARAM = "continuationToken";
    private static final String CONTINUATION_TOKEN_HEADER = "x-ms-continuationtoken";
    private static final String FORMAT = "yyyy-MM-dd'T'hh:mm:ss";
    private static final String PAGE_SIZE_QUERY_PARAM = "$top";
    private static final String PAGE_OFFSET_QUERY_PARAM = "$skip";
    private static final String FROM_QUERY_PARAM = "minTime";
    private static final String TO_QUERY_PARAM = "maxTime";
    public static final String LIST_BUILD_CHANGES = "_apis/build/changes";
    public static final String BUILD_COMMITS_API_VERSION = "6.0-preview.2";
    public static final String FROM_BUILD_QUERY_PARAM = "fromBuildId";
    public static final String TO_BUILD_QUERY_PARAM = "toBuildId";
    private static final String FROM_DATE_QUERY_PARAM = "fromDate";
    private static final String TO_DATE_QUERY_PARAM = "toDate";
    private static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss";
    private static final String SKIP_QUERY_PARAM = "$skip";
    private static final String TIMEFRAME_QUERY_PARAM = "$timeframe";
    private static final String IDS_QUERY_PARAM = "ids";
    public static final String AZURE_DEVOPS_URL = "https://app.vssps.visualstudio.com";
    public static final String AZURE_DEVOPS_RELEASE_URL = "https://vsrm.dev.azure.com";
    public static final String APIS = "_apis";
    public static final String PROFILE = "profile";
    public static final String PROFILES = "profiles";
    public static final String STATES = "states";
    public static final String REFS = "refs";
    public static final String TAGS = "tags";
    public static final String FILTER = "filter";
    public static final String CURRENT_USER = "me";
    public static final String PROFILE_API_VALUE = "5.1";
    public static final String ACCOUNT_API_VALUE = "6.0";
    public static final String ACCOUNTS = "accounts";
    public static final String MEMBER_ID_QUERY_PARAM = "memberId";
    public static final String INCLUDECOMMITS = "includecommits";
    public static final String LABELS = "labels";
    public static final String PULLREQUESTS_FOR_LABELS = "pullRequests";
    public static final String CHANGESETS_API = "_apis/tfvc/changesets";
    public static final String APIS_TFVC_BRANCHES = "_apis/tfvc/branches";
    public static final String APIS_TFVC_LABELS = "_apis/tfvc/labels";
    public static final String APIS_TEAMSETTINGS = "_apis/work/teamsettings/teamfieldvalues";
    public static final String PULL_REQUESTS_THREADS = "threads";
    public static final String SEARCH_CRITERIA_STATUS = "searchCriteria.status";
    public static final String ALL = "all";
    public static final String DEPTH = "$depth";
    public static final String DEFAULT_DEPTH = "100"; // this is arbitrary
    public static final String CLASSIFICATIONNODES_AREAS = "_apis/wit/classificationnodes/Areas";

    private final ClientHelper<AzureDevopsClientException> clientHelper;
    private final String resourceUrl;
    private final int pageSize;
    private final Supplier<Map<String, Object>> metadataSupplier;
    private final IntervalRateLimiter rateLimiter;

    @Builder
    public AzureDevopsClient(final OkHttpClient okHttpClient,
                             final ObjectMapper objectMapper,
                             String resourceUrl, int pageSize,
                             Supplier<Map<String, Object>> metadataSupplier,
                             Integer throttlingIntervalMs) throws AzureDevopsClientException {
        this.resourceUrl = resourceUrl;
        this.pageSize = pageSize != 0 ? pageSize : AzureDevopsClientFactory.DEFAULT_PAGE_SIZE;
        this.clientHelper = ClientHelper.<AzureDevopsClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(AzureDevopsClientException.class)
                .build();
        this.metadataSupplier = metadataSupplier;
        rateLimiter = new IntervalRateLimiter(MoreObjects.firstNonNull(throttlingIntervalMs, 0));
    }

    public ProjectResponse getProjects(String organization, String continuationToken) throws AzureDevopsClientException {
        return getProjects(organization, continuationToken, AZURE_FAILED_RESPONSE_PREDICATE);
    }

    public ProjectResponse getProjects(String organization, String continuationToken, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegments(LIST_PROJECTS_API)
                .addQueryParameter(PAGE_SIZE_QUERY_PARAM, String.valueOf(pageSize))
                .addQueryParameter(API_VERSION_QUERY_PARAM, PROJECT_API_VERSION_VALUE);
        if (StringUtils.isNotEmpty(continuationToken)) {
            builder.addQueryParameter(CONTINUATION_TOKEN_QUERY_PARAM, continuationToken);
        }
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<ProjectResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws 401 - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, ProjectResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, ProjectResponse.class);
        }
        var projects = page.getBody().getProjects().stream()
                .map(project -> project.toBuilder()
                        .organization(organization)
                        .build())
                .collect(Collectors.toList());
        return ProjectResponse.builder()
                .continuationToken(page.getHeader(CONTINUATION_TOKEN_HEADER))
                .projects(projects)
                .build();

    }

    public List<ProjectProperty> getProjectProperties(String organization, String projectId) throws AzureDevopsClientException {
        try {
            return getProjectProperties(organization, projectId, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch project properties for org: " + organization + " project: " + projectId, e);
            throw e;
        }
    }

    public List<ProjectProperty> getProjectProperties(String organization, String projectId, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegments(LIST_PROJECTS_API)
                .addPathSegment(projectId)
                .addPathSegment(LIST_PROPERTIES_PROJECT_API)
                .addQueryParameter(API_VERSION_QUERY_PARAM, PROJECT_PROPERTIES_API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<ProjectPropertiesResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, ProjectPropertiesResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, ProjectPropertiesResponse.class);
        }
        return page.getBody().getProjectProperties();
    }

    public ReleaseResponse getReleases(String organization, String project, String continuationToken) throws AzureDevopsClientException {
        try {
            return getReleases(organization, project, continuationToken, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch project releases for org: " + organization + " project: " + project, e);
            throw e;
        }
    }

    public ReleaseResponse getReleases(String organization, String project, String continuationToken,
                                       Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(AZURE_DEVOPS_RELEASE_URL)).newBuilder()
                .addPathSegments(organization)
                .addPathSegments(project)
                .addPathSegments(LIST_RELEASES_API)
                .addQueryParameter(PAGE_SIZE_QUERY_PARAM, String.valueOf(pageSize))
                .addQueryParameter(API_VERSION_QUERY_PARAM, API_VERSION_VALUE);
        if (!continuationToken.isEmpty()) {
            builder.addQueryParameter(CONTINUATION_TOKEN_QUERY_PARAM, continuationToken);
        }
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<ReleaseResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws 401 - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, responsePredicate, ReleaseResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, ReleaseResponse.class);
        }
        return ReleaseResponse.builder()
                .continuationToken(page.getHeader(CONTINUATION_TOKEN_HEADER))
                .releases(page.getBody().getReleases())
                .count(page.getBody().getCount())
                .build();
    }

    public AzureDevopsRelease getRelease(String organization, String project, int releaseId) throws AzureDevopsClientException {
        try {
            return getRelease(organization, project, releaseId, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch release for org: " + organization + " project: " + project + " release id " + releaseId, e);
            throw e;
        }
    }

    public AzureDevopsRelease getRelease(String organization, String project, int releaseId, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(AZURE_DEVOPS_RELEASE_URL)).newBuilder()
                .addPathSegments(organization)
                .addPathSegments(project)
                .addPathSegments(LIST_RELEASES_API)
                .addPathSegments(String.valueOf(releaseId))
                .addQueryParameter(API_VERSION_QUERY_PARAM, API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<AzureDevopsRelease> page;
        if (responsePredicate != null) {
            page = clientHelper.executeAndParseWithHeaders(request, responsePredicate, AzureDevopsRelease.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, AzureDevopsRelease.class);
        }
        return page.getBody();
    }

    public PipelineResponse getPipelines(String organization, String project,
                                         String continuationToken) throws AzureDevopsClientException {
        try {
            return getPipelines(organization, project, continuationToken, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch project pipelines for org: " + organization + " project: " + project, e);
            throw e;
        }
    }

    public PipelineResponse getPipelines(String organization, String project,
                                         String continuationToken, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(organization)
                .addPathSegments(project)
                .addPathSegments(LIST_PIPELINES_API)
                .addQueryParameter(PAGE_SIZE_QUERY_PARAM, String.valueOf(pageSize))
                .addQueryParameter(API_VERSION_QUERY_PARAM, API_VERSION_VALUE);
        if (!continuationToken.isEmpty()) {
            builder.addQueryParameter(CONTINUATION_TOKEN_QUERY_PARAM, continuationToken);
        }
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<PipelineResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws 401 - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, PipelineResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, PipelineResponse.class);
        }
        return PipelineResponse.builder()
                .continuationToken(page.getHeader(CONTINUATION_TOKEN_HEADER))
                .pipelines(page.getBody().getPipelines())
                .build();
    }

    public Pipeline getPipeline(String organization, String project, int pipelineId) throws AzureDevopsClientException {
        try {
            return getPipeline(organization, project, pipelineId, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch pipeline for org: " + organization + " project: " + project + " pipelined id " + pipelineId, e);
            throw e;
        }
    }

    public Pipeline getPipeline(String organization, String project, int pipelineId, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(organization)
                .addPathSegments(project)
                .addPathSegments(LIST_PIPELINES_API)
                .addPathSegments(String.valueOf(pipelineId))
                .addQueryParameter(PAGE_SIZE_QUERY_PARAM, String.valueOf(pageSize))
                .addQueryParameter(API_VERSION_QUERY_PARAM, API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<Pipeline> page;
        if (responsePredicate != null) {
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, Pipeline.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, Pipeline.class);
        }
        return page.getBody();
    }

    public RunResponse getRuns(String organization, String project, int pipelineId) throws AzureDevopsClientException {
        try {
            return getRuns(organization, project, pipelineId, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch runs for org: " + organization + " project: " + project + " pipelined id " + pipelineId, e);
            throw e;
        }
    }

    public RunResponse getRuns(String organization, String project, int pipelineId, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegments(project)
                .addPathSegments(LIST_PIPELINES_API)
                .addPathSegments(String.valueOf(pipelineId))
                .addPathSegments(LIST_PIPELINE_RUNS_API)
                .addQueryParameter(API_VERSION_QUERY_PARAM, API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<RunResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, RunResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, RunResponse.class);
        }
        return page.getBody();
    }

    public BuildResponse getBuilds(String organization, String project, Date minTime, Date maxTime,
                                   String continuationToken) throws AzureDevopsClientException {
        try {
            return getBuilds(organization, project, minTime, maxTime, continuationToken, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch builds for org: " + organization + " project: " + project, e);
            throw e;
        }
    }

    public BuildResponse getBuilds(String organization, String project, Date minTime, Date maxTime,
                                   String continuationToken, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegments(project)
                .addPathSegments(LIST_BUILDS_API)
                .addQueryParameter(PAGE_SIZE_QUERY_PARAM, String.valueOf(pageSize))
                .addQueryParameter(API_VERSION_QUERY_PARAM, BUILDS_API_VERSION_VALUE);
        if (!continuationToken.isEmpty()) {
            builder.addQueryParameter(CONTINUATION_TOKEN_QUERY_PARAM, continuationToken);
        }
        if (minTime != null) {
            String parsedDate = getDateTimeWithTimezone(minTime);
            builder.addQueryParameter(FROM_QUERY_PARAM, parsedDate);
        }
        if (maxTime != null) {
            String parsedDate = getDateTimeWithTimezone(maxTime);
            builder.addQueryParameter(TO_QUERY_PARAM, parsedDate);
        }
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<BuildResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, BuildResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, BuildResponse.class);
        }
        return BuildResponse.builder()
                .continuationToken(page.getHeader(CONTINUATION_TOKEN_HEADER))
                .builds(page.getBody().getBuilds())
                .build();

    }

    public List<BuildChange> getBuildCommits(String organization, String project,
                                             int fromBuild, int toBuild) throws AzureDevopsClientException {
        try {
            return getBuildCommits(organization, project, fromBuild, toBuild, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch build commits for org: " + organization + " project: " + project, e);
            throw e;
        }
    }

    public List<BuildChange> getBuildCommits(String organization, String project,
                                             int fromBuild, int toBuild, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegments(project)
                .addPathSegments(LIST_BUILD_CHANGES)
                .addQueryParameter(API_VERSION_QUERY_PARAM, BUILD_COMMITS_API_VERSION)
                .addQueryParameter(FROM_BUILD_QUERY_PARAM, String.valueOf(fromBuild))
                .addQueryParameter(TO_BUILD_QUERY_PARAM, String.valueOf(toBuild));
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<BuildChangeResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, BuildChangeResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, BuildChangeResponse.class);
        }
        return page.getBody().getBuildChanges();

    }

    public List<AzureDevopsPipelineRunStageStep> getBuildTimeline(String organization, String project,
                                                                  int runId) throws AzureDevopsClientException {
        try {
            return getBuildTimeline(organization, project, runId, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause instanceof NoContentException) {
                log.warn("No content present for runId: " + runId + " for org: " + organization + " project: " + project);
                return List.of();
            }
            log.warn("Could not fetch build stages for org: " + organization + " project: " + project, e);
            throw e;
        }
    }

    public List<AzureDevopsPipelineRunStageStep> getBuildTimeline(String organization, String project,
                                           int runId, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegments(project)
                .addPathSegments(LIST_BUILDS_API)
                .addPathSegments(String.valueOf(runId))
                .addPathSegments(BUILD_TIMELINE_API)
                .addQueryParameter(API_VERSION_QUERY_PARAM, BUILD_COMMITS_API_VERSION);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<BuildTimelineResponse> page;
        if (responsePredicate != null) {
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, BuildTimelineResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, BuildTimelineResponse.class);
        }
        return page.getBody().getStages();
    }

    public List<String> getStepLogs(String url) throws AzureDevopsClientException {
        ClientHelper.BodyAndHeaders<Map> page = clientHelper.executeAndParseWithHeaders(new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build(), Map.class);
        return (List<String>) page.getBody().get("value");
    }

    public String getReleaseStepLogs(String url) throws AzureDevopsClientException {
        String stepLogs = clientHelper.executeRequest(new Request.Builder()
                .url(url)
                .get()
                .build());
        return stepLogs;
    }

    public String getProfile() throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(AZURE_DEVOPS_URL)).newBuilder()
                .addPathSegment(APIS)
                .addPathSegments(PROFILE)
                .addPathSegments(PROFILES)
                .addPathSegments(CURRENT_USER)
                .addQueryParameter(API_VERSION_QUERY_PARAM, PROFILE_API_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<Profile> page = clientHelper
                .executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, Profile.class);
        return page.getBody().getId();
    }

    public List<String> getAccounts() throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        String memberId = getProfile();
        var builder = Objects.requireNonNull(HttpUrl.parse(AZURE_DEVOPS_URL)).newBuilder()
                .addPathSegment(APIS)
                .addPathSegments(ACCOUNTS)
                .addQueryParameter(MEMBER_ID_QUERY_PARAM, memberId)
                .addQueryParameter(API_VERSION_QUERY_PARAM, ACCOUNT_API_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<AccountResponse> page = clientHelper
                .executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, AccountResponse.class);
        return page.getBody().getAccounts().stream()
                .map(Account::getAccountName)
                .collect(Collectors.toList());
    }

    public List<String> getOrganizations() {
        List<String> organizations = getOrganizations(MapUtils.emptyIfNull(metadataSupplier.get()));
        if (CollectionUtils.isEmpty(organizations)) {
            try {
                return getAccounts();
            } catch (AzureDevopsClientException e) {
                throw new RuntimeException("Failed to get accounts", e);
            }
        }
        return organizations;
    }

    public List<String> getQualifiedProjects() {
        return getQualifiedProjects(MapUtils.emptyIfNull(metadataSupplier.get()));
    }

    public List<Repository> getRepositories(String organization, String project) throws AzureDevopsClientException {
        try {
            return getRepositories(organization, project, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch repositories for org: " + organization + " project: " + project, e);
            throw e;
        }
    }

    public List<Repository> getRepositories(String organization, String project, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegment(LIST_REPOSITORIES_API)
                .addQueryParameter(API_VERSION_QUERY_PARAM, API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<RepositoryResponse> page;
        if (responsePredicate != null) {
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, RepositoryResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, RepositoryResponse.class);
        }
        return page.getBody().getRepositories();
    }

    public List<Commit> getCommits(String organization, String project, String repositoryName, String repositoryId,
                                   Date from, Date to, int offset) throws AzureDevopsClientException {
        try {
            return getCommits(organization, project, repositoryId, from, to, offset, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause instanceof HttpException) {
                HttpException httpException = (HttpException) rootCause;
                if (httpException.getCode() != null
                        && httpException.getCode() == 404 &&
                        httpException.getMessage() != null &&
                        httpException.getMessage().contains("does not exist or you do not have permissions for the operation you are attempting")) {
                    log.warn("Could not fetch commits for org={}, project={}, repoName={} because of lacking repo permissions (404 TF401019) - ignoring. (repo_id={})", organization, project, repositoryName, repositoryId);
                    return List.of();
                }
            }
            log.warn("Could not fetch commits for org={}, project={}, repo='{}', repoId={}", organization, project, repositoryName, repositoryId, e);
            throw e;
        }
    }

    public List<Commit> getCommits(String organization, String project, String repositoryId,
                                   Date from, Date to, int offset, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegments(LIST_REPOSITORIES_API)
                .addPathSegment(repositoryId)
                .addPathSegment(LIST_COMMITS_API)
                .addQueryParameter(PAGE_OFFSET_QUERY_PARAM, String.valueOf(offset))
                .addQueryParameter(PAGE_SIZE_QUERY_PARAM, String.valueOf(pageSize))
                .addQueryParameter(API_VERSION_QUERY_PARAM, API_VERSION_VALUE);
        if (from != null) {
            builder.addQueryParameter(FROM_DATE_QUERY_PARAM, getDateTimeWithTimezone(from));
        }
        if (to != null) {
            builder.addQueryParameter(TO_DATE_QUERY_PARAM, getDateTimeWithTimezone(to));
        }
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<CommitResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws 404 - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, CommitResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, CommitResponse.class);
        }
        return page.getBody().getCommits();
    }

    public List<Change> getChanges(String organization, String project, String repositoryId, String commitId,
                                   int offset) throws AzureDevopsClientException {
        try {
            return getChanges(organization, project, repositoryId, commitId, offset, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch commit changes for org: " + organization +
                    " project: " + project + " repo : " + repositoryId + " commit id " + commitId, e);
            throw e;
        }
    }

    public List<Change> getChanges(String organization, String project, String repositoryId, String commitId,
                                   int offset, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegments(LIST_REPOSITORIES_API)
                .addPathSegment(repositoryId)
                .addPathSegment(LIST_COMMITS_API)
                .addPathSegment(commitId)
                .addPathSegment(LIST_COMMITCHANGES_API)
                .addQueryParameter("skip", String.valueOf(offset))
                .addQueryParameter("top", String.valueOf(pageSize))
                .addQueryParameter(API_VERSION_QUERY_PARAM, API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<CommitChangesResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws 401 - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, CommitChangesResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, CommitChangesResponse.class);
        }
        return page.getBody().getChanges();
    }

    public List<PullRequest> getPullRequests(String organization, String project, String repositoryId,
                                             int offset) throws AzureDevopsClientException {
        try {
            return getPullRequests(organization, project, repositoryId, offset, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch pull requests for org: " + organization + " project: " +
                    project + " repo " + repositoryId, e);
            throw e;
        }
    }

    public List<PullRequest> getPullRequests(String organization, String project, String repositoryId,
                                             int offset, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        try {
            var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                    .addPathSegment(organization)
                    .addPathSegment(project)
                    .addPathSegments(LIST_REPOSITORIES_API)
                    .addPathSegment(repositoryId)
                    .addPathSegment(LIST_PULLREQUESTS_API)
                    .addQueryParameter(SEARCH_CRITERIA_STATUS, ALL)
                    .addQueryParameter(PAGE_OFFSET_QUERY_PARAM, String.valueOf(offset))
                    .addQueryParameter(PAGE_SIZE_QUERY_PARAM, String.valueOf(pageSize))
                    .addQueryParameter(API_VERSION_QUERY_PARAM, API_VERSION_VALUE);
            Request request = buildRequest(builder.build());
            ClientHelper.BodyAndHeaders<PullRequestResponse> page;
            if (responsePredicate != null) {
                // LEV-3738: sometimes throws 404 - TODO catch specific error
                page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, PullRequestResponse.class);
            } else {
                page = clientHelper.executeAndParseWithHeaders(request, PullRequestResponse.class);
            }
            return page.getBody().getPullRequests();
        } catch (AzureDevopsClientException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause instanceof HttpException) {
                HttpException httpException = (HttpException) rootCause;
                log.info(httpException.getMessage());
                if (httpException.getCode() != null
                        && httpException.getCode() == 404
                        && httpException.getMessage() != null
                        && httpException.getMessage().contains("you do not have permissions for the operation you are attempting.")) {
                    log.info("Got expected 404 exception while fetching pull request (The Git repository with name or identifier not exist or you do not have permissions for the operation you are attempting.) - ignoring.");
                    return List.of();
                }
            }
            log.warn("Error while making pull request API call", e);
            throw e;
        }
    }


    public List<PullRequestHistory> getPullRequestsHistories(String organization, String project, String repositoryId,
                                                             String pullRequestId, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegments(LIST_REPOSITORIES_API)
                .addPathSegment(repositoryId)
                .addPathSegment(LIST_PULLREQUESTS_API)
                .addPathSegment(pullRequestId)
                .addPathSegment(PULL_REQUESTS_THREADS)
                .addQueryParameter(API_VERSION_QUERY_PARAM, PROJECT_PROPERTIES_API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<PullRequestHistoryResponse> page;
        if (responsePredicate != null) {
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, PullRequestHistoryResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, PullRequestHistoryResponse.class);
        }
        return page.getBody().getPullRequestHistories();
    }

    public PullRequest getPullRequestsWithCommiterInfo(String organization, String project, String repositoryId,
                                                       String pullRequestId) throws AzureDevopsClientException {
        try {
            return getPullRequestsWithCommiterInfo(organization, project, repositoryId, pullRequestId, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch pr committers info for org: " + organization + " project: "
                    + project + " repo " + repositoryId + " pr id " + pullRequestId, e);
            throw e;
        }
    }

    public PullRequest getPullRequestsWithCommiterInfo(String organization, String project, String repositoryId,
                                                       String pullRequestId, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegments(LIST_REPOSITORIES_API)
                .addPathSegment(repositoryId)
                .addPathSegment(LIST_PULLREQUESTS_API)
                .addPathSegment(pullRequestId)
                .addQueryParameter(PAGE_SIZE_QUERY_PARAM, String.valueOf(pageSize))
                .addQueryParameter(API_VERSION_QUERY_PARAM, API_VERSION_VALUE)
                .addQueryParameter(INCLUDECOMMITS, "true");
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<PullRequest> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, PullRequest.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, PullRequest.class);
        }
        return page.getBody();
    }


    public List<Label> getPullRequestsWithLabelInfo(String organization, String project, String repositoryId,
                                                    String pullRequestId) throws AzureDevopsClientException {
        try {
            return getPullRequestsWithLabelInfo(organization, project, repositoryId, pullRequestId, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch pull requests label for org: " + organization + " project: " +
                    project + " repo " + repositoryId + " pr id " + pullRequestId, e);
            throw e;
        }
    }

    public List<Label> getPullRequestsWithLabelInfo(String organization, String project, String repositoryId,
                                                    String pullRequestId, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegments(LIST_REPOSITORIES_API)
                .addPathSegment(repositoryId)
                .addPathSegment(PULLREQUESTS_FOR_LABELS)
                .addPathSegment(pullRequestId)
                .addPathSegment(LABELS)
                .addQueryParameter(PAGE_SIZE_QUERY_PARAM, String.valueOf(pageSize))
                .addQueryParameter(API_VERSION_QUERY_PARAM, API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<LabelResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, LabelResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, LabelResponse.class);
        }
        return page.getBody().getLabels();
    }

    public List<Team> getTeams(String organization, String project,
                               int skip) throws AzureDevopsClientException {
        try {
            return getTeams(organization, project, skip, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch teams for org: " + organization + " project: " + project, e);
            throw e;
        }
    }

    public List<Team> getTeams(String organization, String project,
                               int skip, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegments(LIST_PROJECTS_API)
                .addPathSegment(project)
                .addPathSegment(LIST_TEAMS_API)
                .addQueryParameter(SKIP_QUERY_PARAM, String.valueOf(skip))
                .addQueryParameter(PAGE_SIZE_QUERY_PARAM, String.valueOf(pageSize))
                .addQueryParameter(API_VERSION_QUERY_PARAM, OLD_API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<TeamResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, TeamResponse.class);
            return page.getBody().getTeams();
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, TeamResponse.class);
        }
        return page.getBody().getTeams();
    }

    public List<Iteration> getIterations(String organization, String project, String team,
                                         boolean fetchCurrentOnly) throws AzureDevopsClientException {
        try {
            return getIterations(organization, project, team, fetchCurrentOnly, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause instanceof HttpException) {
                HttpException httpException = (HttpException) rootCause;
                if (httpException.getCode() != null
                        && httpException.getCode() == 404
                        && httpException.getMessage() != null
                        && httpException.getMessage().contains("No current iteration was found. No iteration contains today's date.")) {
                    log.info("Got expected 404 exception while fetching iterations (No current iteration was found. No iteration contains today's date.) - ignoring.");
                    return List.of();
                }
            }
            log.warn("Error while making iterations API call", e);
            throw e;
        }
    }

    public List<Iteration> getIterations(String organization, String project, String team,
                                         boolean fetchCurrentOnly, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegment(team)
                .addPathSegments(LIST_ITERATIONS_API)
                .addQueryParameter(TIMEFRAME_QUERY_PARAM, fetchCurrentOnly ? "current" : "")
                .addQueryParameter(API_VERSION_QUERY_PARAM, ITERATIONS_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<IterationResponse> page;
        if (responsePredicate != null) {
            // LEV-3682 : Iterations API returns 404 when there are no active sprints - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, IterationResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, IterationResponse.class);
        }
        return page.getBody().getIterations();
    }

    public List<WorkItem> getWorkItems(String organization, String project, List<String> ids) throws AzureDevopsClientException {
        try {
            return getWorkItems(organization, project, ids, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch workitem for org: " + organization + " project: " + project + "ids " + ids, e);
            throw e;
        }
    }

    public List<WorkItem> getWorkItems(String organization, String project, List<String> ids,
                                       Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        String idParamValues = String.join(",", ids);
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegments(LIST_WORKITEMS_API)
                .addQueryParameter(IDS_QUERY_PARAM, idParamValues)
                .addQueryParameter(WORKITEMS_EXPAND_RESPONSE, "All")
                .addQueryParameter(API_VERSION_QUERY_PARAM, WORKITEMS_API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<WorkItemResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, WorkItemResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, WorkItemResponse.class);
        }
        return page.getBody().getWorkItems();
    }

    public List<WorkItemHistory> getWorkItemsUpdates(String organization, String project, String id) throws AzureDevopsClientException {
        try {
            return getWorkItemsUpdates(organization, project, id, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch workitem updates for org: " + organization + " project: " + project + "id " + id, e);
            throw e;
        }
    }

    public List<WorkItemHistory> getWorkItemsUpdates(String organization, String project, String id,
                                                     Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegments(LIST_WORKITEMS_API)
                .addPathSegment(id)
                .addPathSegment("updates")
                .addQueryParameter(API_VERSION_QUERY_PARAM, WORKITEMS_API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<WorkItemHistoryResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, WorkItemHistoryResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, WorkItemHistoryResponse.class);
        }
        return page.getBody().getWorkItemsHistory();
    }

    public List<WorkItemField> getFields(String organization, String project) throws AzureDevopsClientException {
        try {
            return getFields(organization, project, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch workitem fields for org: " + organization + " project: " + project, e);
            throw e;
        }
    }

    public List<WorkItemField> getFields(String organization, String project, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegments(LIST_WORKITEMS_FIELDS_API)
                .addQueryParameter(API_VERSION_QUERY_PARAM, OLD_API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<WorkItemFieldResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, WorkItemFieldResponse.class);
            return page.getBody().getWorkItemField();
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, WorkItemFieldResponse.class);
            return page.getBody().getWorkItemField();
        }

    }

    public WorkItemQueryResult getWorkItemQuery(String organization, String project,
                                                Date from, Date to) throws AzureDevopsClientException {
        try {
            return getWorkItemQuery(organization, project, from, to, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch workitems from wiql for org: " + organization + " project: " + project, e);
            throw e;
        }
    }

    public WorkItemQueryResult getWorkItemQuery(String organization, String project,
                                                Date from, Date to, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        // This endpoint has a hard limit of 20000.
        // But sadly, it does not support pagination: no $skip, only $top.
        // LEV-3757: will add a limit to unblock the scan
        // TODO we need to change the query to build the pagination inside the WIQL to be able to fetch more than 20k results.
        // https://stackoverflow.com/questions/64448477/azure-devops-wiql-query-by-id-is-there-a-skip-parameter
        // https://stackoverflow.com/questions/64215062/vsts-vs402337-the-number-of-work-items-returned-exceeds-the-size-limit-of-200

        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegments(LIST_WIQL_API)
//                .addQueryParameter(SKIP_QUERY_PARAM, String.valueOf(offset)) // DOESN'T WORK
                .addQueryParameter(PAGE_SIZE_QUERY_PARAM, "19999") // hard limit: 20k
                .addQueryParameter("timePrecision", "True")
                .addQueryParameter(API_VERSION_QUERY_PARAM, OLD_API_VERSION_VALUE);
        HttpUrl url = builder.build();
        String formattedFrom = getDateTimeWithTimezone(from);
        String formattedTo = getDateTimeWithTimezone(to);
        String wiqlQuery = MessageFormat.format(" '{'" +
                " \"query\" : \"Select [System.Id] " +
                " From WorkItems Where [System.TeamProject] = " + "''{0}''" +
                " AND [System.ChangedDate] > ''{1}''" +
                " AND [System.ChangedDate] < ''{2}''" +
                " order by [System.Id] asc\"" +
                " '}'", project, formattedFrom, formattedTo);
        log.info("getWorkItemQuery org={}, project={} url={} wiqlQuery = {}", organization, project, url, wiqlQuery);
        RequestBody body = RequestBody.create(wiqlQuery, MediaType.parse("application/json"));
        Request request = buildRequestForPostRequest(url, body);
        ClientHelper.BodyAndHeaders<WorkItemQueryResult> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws 401 - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, WorkItemQueryResult.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, WorkItemQueryResult.class);
        }
        log.debug("org={} proj={} from={} to={} headers={}", organization, project, from, to, page.getHeaders());
        return page.getBody();
    }

    public List<ChangeSet> getChangesets(String organization, String project, Date from, int offset) throws AzureDevopsClientException {
        try {
            return getChangesets(organization, project, from, offset, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch changesets for org: " + organization + " project: " + project, e);
            throw e;
        }
    }

    public List<ChangeSet> getChangesets(String organization, String project, Date from, int offset,
                                         Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegment(CHANGESETS_API)
                .addQueryParameter(SKIP_QUERY_PARAM, String.valueOf(offset))
                .addQueryParameter(PAGE_SIZE_QUERY_PARAM, String.valueOf(pageSize))
                .addQueryParameter(API_VERSION_QUERY_PARAM, OLD_API_VERSION_VALUE);
        if (from != null) {
            builder.addQueryParameter(FROM_DATE_QUERY_PARAM, getDateTimeWithTimezone(from));
        }
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<ChangeSetResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, ChangeSetResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, ChangeSetResponse.class);
        }
        return page.getBody().getChangeSets();
    }

    public List<ChangeSetChange> getChangesetChanges(String organization, String changesetId, int offset) throws AzureDevopsClientException {
        try {
            return getChangesetChanges(organization, changesetId, offset, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch changeset changes for org: " + organization + " changeset id: " + changesetId, e);
            throw e;
        }
    }

    public List<ChangeSetChange> getChangesetChanges(String organization, String changesetId, int offset,
                                                     Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(CHANGESETS_API)
                .addPathSegment(changesetId)
                .addPathSegment("changes")
                .addQueryParameter(SKIP_QUERY_PARAM, String.valueOf(offset * pageSize))
                .addQueryParameter(PAGE_SIZE_QUERY_PARAM, String.valueOf(pageSize))
                .addQueryParameter(API_VERSION_QUERY_PARAM, OLD_API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<ChangeSetChangeResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, ChangeSetChangeResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, ChangeSetChangeResponse.class);
        }
        return page.getBody().getChangeSetChanges();
    }

    public List<ChangeSetWorkitem> getChangesetWorkitems(String organization, String changeSetId) throws AzureDevopsClientException {
        try {
            return getChangesetWorkitems(organization, changeSetId, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch changesets workitems for org: " + organization + " changeset id: " + changeSetId, e);
            throw e;
        }
    }

    public List<ChangeSetWorkitem> getChangesetWorkitems(String organization, String changeSetId,
                                                         Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(CHANGESETS_API)
                .addPathSegment(changeSetId)
                .addPathSegment("workItems")
                .addQueryParameter(API_VERSION_QUERY_PARAM, OLD_API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<ChangeSetWorkitemResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, ChangeSetWorkitemResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, ChangeSetWorkitemResponse.class);
        }
        return page.getBody().getChangeSetWorkitems();
    }

    public Boolean shouldIngestWorkitemComments() {
        return parseIngestWorkItemComments(metadataSupplier.get());
    }

    public List<Comment> getComments(String organization, String project, String workItemId) throws AzureDevopsClientException {
        try {
            return getComments(organization, project, workItemId, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch comments for org: " + organization + "project " + project + "workitem id: " + workItemId, e);
            throw e;
        }
    }

    public List<Comment> getComments(String organization, String project, String workItemId,
                                     Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegments(LIST_WORKITEMS_API)
                .addPathSegment(workItemId)
                .addPathSegment(LIST_WORKITEM_COMMENTS_API)
                .addQueryParameter(LIST_WORKITEM_COMMENTS_ORDER_API, WORKITEM_COMMENTS_ASCENDING_ORDER_API)
                .addQueryParameter(API_VERSION_QUERY_PARAM, WORKITEMS_COMMENTS_API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<CommentResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, CommentResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, CommentResponse.class);
        }
        return page.getBody().getComments();
    }

    public List<Branch> getBranches(String organization, String project) throws AzureDevopsClientException {
        try {
            return getBranches(organization, project, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch branches for org: " + organization + "project " + project, e);
            throw e;
        }
    }

    public List<Branch> getBranches(String organization, String project, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegment(APIS_TFVC_BRANCHES)
                .addQueryParameter("includeChildren", "true")
                .addQueryParameter(API_VERSION_QUERY_PARAM, OLD_API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<BranchResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, BranchResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, BranchResponse.class);
        }
        return page.getBody().getBranches();
    }

    public List<Label> getLabels(String organization, String project, int offset) throws AzureDevopsClientException {
        try {
            return getLabels(organization, project, offset, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (Exception e) {
            log.warn("Could not fetch labels for org: " + organization + "project " + project, e);
            throw e;
        }
    }

    public List<Label> getLabels(String organization, String project, int offset,
                                 Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegment(APIS_TFVC_LABELS)
                .addQueryParameter(SKIP_QUERY_PARAM, String.valueOf(offset * pageSize))
                .addQueryParameter(PAGE_SIZE_QUERY_PARAM, String.valueOf(pageSize))
                .addQueryParameter(API_VERSION_QUERY_PARAM, OLD_API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<LabelResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper
                    .executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, LabelResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, LabelResponse.class);
        }
        return page.getBody().getLabels();
    }

    public TagResponse getTags(String organization, String project, String repositoryId, String continuationToken) throws AzureDevopsClientException {
        try {
            return getTags(organization, project, repositoryId, continuationToken, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch tags for org: " + organization + " project " + project, e);
            throw e;
        }
    }

    public TagResponse getTags(String organization, String project, String repositoryId, String continuationToken, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegment(LIST_REPOSITORIES_API)
                .addPathSegment(repositoryId)
                .addPathSegment(REFS)
                .addQueryParameter(FILTER, TAGS)
                .addQueryParameter(API_VERSION_QUERY_PARAM, OLD_API_VERSION_VALUE);
        if (StringUtils.isNotEmpty(continuationToken)) {
            builder.addQueryParameter(CONTINUATION_TOKEN_QUERY_PARAM, continuationToken);
        }
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<TagResponse> page;
        try {
            if (responsePredicate != null) {
                // LEV-3738: sometimes throws 401 - TODO catch specific error
                page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, TagResponse.class);
            } else {
                page = clientHelper.executeAndParseWithHeaders(request, TagResponse.class);
            }
            return TagResponse.builder()
                    .count(page.getBody().getCount())
                    .tags(page.getBody().getTags())
                    .build();
        } catch (AzureDevopsClientException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause instanceof HttpException) {
                HttpException httpException = (HttpException) rootCause;
                if (httpException.getCode() != null
                        && httpException.getCode() == 404 &&
                        httpException.getMessage() != null &&
                        httpException.getMessage().contains("does not exist or you do not have permissions for the operation you are attempting")) {
                    log.warn("Could not fetch tags for org={}, project={}, repo={} because of lacking repo permissions (404 TF401019) - ignoring.", organization, project, repositoryId);
                    return TagResponse.builder()
                            .count(0)
                            .tags(List.of())
                            .build();
                }
            }
            log.warn("Could not fetch tags for org={}, project={}, repo={}", organization, project, repositoryId, e);
            throw e;
        }
    }

    public Optional<Tag> getAnnotatedTags(String organization, String project, String repositoryId, String objectId) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegment(LIST_REPOSITORIES_API)
                .addPathSegment(repositoryId)
                .addPathSegment(LIST_ANNOTATED_API)
                .addPathSegment(objectId)
                .addQueryParameter(API_VERSION_QUERY_PARAM, TAGS_API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<Tag> page;
        try {
            page = clientHelper.executeAndParseWithHeaders(request, Tag.class);
            return Optional.ofNullable(page.getBody());
        } catch (AzureDevopsClientException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause instanceof HttpException) {
                HttpException httpException = (HttpException) rootCause;
                if (httpException.getCode() != null &&
                        httpException.getCode() == 404 &&
                        httpException.getMessage() != null &&
                        (httpException.getMessage().contains("Expected a Tag, but objectId") || httpException.getMessage().contains("GitUnexpectedObjectTypeException"))) {
                    log.warn("Could not fetch annotated tags for org={}, project={}, repo={}, objectId={}: not a tag (404 GitUnexpectedObjectTypeException) - ignoring.", organization, project, repositoryId, objectId);
                    return Optional.empty();
                }
            }
            log.warn("Could not fetch annotated tags for org={}, project={}, repo={}, objectId={}", organization, project, repositoryId, objectId, e);
            throw e;
        }
    }

    public List<WorkItemTypeCategory> getWorkItemTypeCategories(String organization, String project) throws AzureDevopsClientException {
        try {
            return getWorkItemTypeCategories(organization, project, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch workitem type categories for org: " + organization + "project " + project, e);
            throw e;
        }
    }

    public List<WorkItemTypeCategory> getWorkItemTypeCategories(String organization, String project, Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegments(LIST_WORKITEMTYPE_CATEGORIES_API)
                .addQueryParameter(API_VERSION_QUERY_PARAM, OLD_API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<WorkItemTypeCategoryResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, WorkItemTypeCategoryResponse.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, WorkItemTypeCategoryResponse.class);
        }
        return page.getBody().getWorkItemTypeCategories();
    }

    public List<WorkItemType> getWorkItemTypes(String organization, String project) throws AzureDevopsClientException {
        try {
            return getWorkItemTypes(organization, project, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch workitem types for org: " + organization + "project " + project, e);
            throw e;
        }
    }

    public List<WorkItemType> getWorkItemTypes(String organization, String project,
                                               Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegments(LIST_WORKITEMTYPES_API)
                .addQueryParameter(API_VERSION_QUERY_PARAM, OLD_API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<WorkItemTypeResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, WorkItemTypeResponse.class);
            return page.getBody().getWorkItemTypes();
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, WorkItemTypeResponse.class);
        }
        return page.getBody().getWorkItemTypes();
    }

    public List<WorkItemTypeState> getWorkItemTypeStates(String organization, String project, String type) throws AzureDevopsClientException {
        try {
            return getWorkItemTypeStates(organization, project, type, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch project properties for org: " + organization + "project " + project, e);
            throw e;
        }
    }

    public List<WorkItemTypeState> getWorkItemTypeStates(String organization, String project, String type,
                                                         Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegments(LIST_WORKITEMTYPES_API)
                .addPathSegment(type)
                .addPathSegment(STATES)
                .addQueryParameter(API_VERSION_QUERY_PARAM, PROJECT_PROPERTIES_API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<WorkItemTypeStateResponse> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper
                    .executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, WorkItemTypeStateResponse.class);
            return page.getBody().getWorkItemTypeStates();
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, WorkItemTypeStateResponse.class);
        }
        return page.getBody().getWorkItemTypeStates();
    }

    public TeamSetting getTeamSettings(String organization, String project, String team) throws AzureDevopsClientException {
        try {
            return getTeamSettings(organization, project, team, AZURE_FAILED_RESPONSE_PREDICATE);
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch team settings for org: " + organization + "project " + project, e);
            throw e;
        }
    }

    public TeamSetting getTeamSettings(String organization, String project, String team,
                                       Predicate<Response> responsePredicate) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegment(team)
                .addPathSegments(APIS_TEAMSETTINGS)
                .addQueryParameter(API_VERSION_QUERY_PARAM, OLD_API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<TeamSetting> page;
        if (responsePredicate != null) {
            // LEV-3738: sometimes throws ? - TODO catch specific error
            page = clientHelper.executeAndParseWithHeaders(request, AZURE_FAILED_RESPONSE_PREDICATE, TeamSetting.class);
        } else {
            page = clientHelper.executeAndParseWithHeaders(request, TeamSetting.class);
        }
        return page.getBody();
    }

    public ClassificationNode getCodeAreas(String organization, String project) throws AzureDevopsClientException {
        rateLimiter.waitForTurn();
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(organization)
                .addPathSegment(project)
                .addPathSegments(CLASSIFICATIONNODES_AREAS)
                .addQueryParameter(DEPTH, DEFAULT_DEPTH)
                .addQueryParameter(API_VERSION_QUERY_PARAM, OLD_API_VERSION_VALUE);
        Request request = buildRequest(builder.build());
        try {
            ClientHelper.BodyAndHeaders<ClassificationNode> page = clientHelper
                    .executeAndParseWithHeaders(request, ClassificationNode.class);
            return page.getBody();
        } catch (AzureDevopsClientException e) {
            log.warn("Could not fetch classification nodes for org: " + organization + "project " + project, e);
            throw e;
        }
    }

    @NotNull
    private Request buildRequest(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void throwException(Throwable t) throws T {
        throw (T) t;
    }

    @NotNull
    private Request buildRequestForPostRequest(HttpUrl url, RequestBody body) {
        return new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .post(body)
                .build();
    }

    private String getDateTimeWithTimezone(Date date) {
        String timezone = metadataSupplier.get() == null ? null :
                (String) metadataSupplier.get().getOrDefault("timezone", null);
        return date.toInstant().atZone(getTimeZone(timezone)).format(DateTimeFormatter.ISO_INSTANT);
    }

    private ZoneId getTimeZone(String timezone) {
        if (timezone == null) {
            return ZoneId.systemDefault();
        } else {
            return ZoneId.of(timezone);
        }
    }

    public static List<String> getQualifiedProjects(Map<String, Object> metadata) {
        List<String> qualifiedProjects = new ArrayList<>();
        Object projects = metadata.get("projects");
        if (projects instanceof String) {
            qualifiedProjects.addAll(getOrgProjectsFromStr((String) projects));
        } else if (projects instanceof List) {
            qualifiedProjects.addAll(getOrgProjectsFromList((List<String>) projects));
        }
        return qualifiedProjects.stream()
                .map(StringUtils::trimToNull)
                .map(StringUtils::lowerCase)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());
    }


    @SuppressWarnings("unchecked")
    public static List<String> getOrganizations(Map<String, Object> metadata) {
        List<String> finalOrganizations = new ArrayList<>();
        Object organization = metadata.get("organization");
        if (organization instanceof String) {
            finalOrganizations.addAll(getOrgProjectsFromStr((String) organization));
        } else if (organization instanceof List) {
            finalOrganizations.addAll(getOrgProjectsFromList((List<String>) organization));
        }
        Object organizations = metadata.get("organizations");
        if (organizations instanceof String) {
            finalOrganizations.addAll(getOrgProjectsFromStr((String) organizations));
        } else if (organizations instanceof List) {
            finalOrganizations.addAll(getOrgProjectsFromList((List<String>) organizations));
        }
        return finalOrganizations;
    }

    @NotNull
    private static List<String> getOrgProjectsFromStr(String orgProjParam) {
        return getOrgProjectsFromList(Arrays.asList(orgProjParam.split(",")));
    }

    @NotNull
    private static List<String> getOrgProjectsFromList(List<String> orgProjectParams) {
        return orgProjectParams.stream()
                .map(String::strip)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());
    }
}
