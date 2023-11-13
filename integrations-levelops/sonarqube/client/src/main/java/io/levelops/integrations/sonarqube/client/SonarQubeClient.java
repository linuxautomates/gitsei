package io.levelops.integrations.sonarqube.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.integrations.sonarqube.models.Analyse;
import io.levelops.integrations.sonarqube.models.Branch;
import io.levelops.integrations.sonarqube.models.ComponentWithMeasures;
import io.levelops.integrations.sonarqube.models.Group;
import io.levelops.integrations.sonarqube.models.Issue;
import io.levelops.integrations.sonarqube.models.IssueResponse;
import io.levelops.integrations.sonarqube.models.MeasureResponse;
import io.levelops.integrations.sonarqube.models.MetricResponse;
import io.levelops.integrations.sonarqube.models.Project;
import io.levelops.integrations.sonarqube.models.ProjectAnalysesResponse;
import io.levelops.integrations.sonarqube.models.ProjectBranchResponse;
import io.levelops.integrations.sonarqube.models.ProjectResponse;
import io.levelops.integrations.sonarqube.models.PullRequest;
import io.levelops.integrations.sonarqube.models.PullRequestResponse;
import io.levelops.integrations.sonarqube.models.QualityGate;
import io.levelops.integrations.sonarqube.models.QualityGateResponse;
import io.levelops.integrations.sonarqube.models.User;
import io.levelops.integrations.sonarqube.models.UserGroupResponse;
import io.levelops.integrations.sonarqube.models.UserResponse;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

/**
 * Sonarqube Client class which should be used for making calls to Sonarqube.
 */
@Log4j2
public class SonarQubeClient {

    public static final String SEARCH_PROJECTS_API_PATH = "api/projects/search";
    public static final String SEARCH_COMPONENTS_API_PATH = "api/components/search";
    public static final String SEARCH_ISSUES_API_PATH = "api/issues/search";
    public static final String SEARCH_PROJECT_ANALYSES_API_PATH = "api/project_analyses/search";
    public static final String LIST_PULL_REQUEST_API_PATH = "api/project_pull_requests/list";
    public static final String LIST_PROJECT_BRANCHES_API_PATH = "api/project_branches/list";
    public static final String LIST_QUALITY_GATES_API_PATH = "api/qualitygates/list";
    public static final String LIST_USER_GROUP_API_PATH = "api/user_groups/search";
    public static final String LIST_USERS_API_PATH = "api/user_groups/users";
    public static final String MEASURES_API_PATH = "api/measures/component";
    public static final String METRICS_API_PATH = "api/metrics/search";

    private static final String PROJECT_KEY_PARAM = "project";
    private static final String FROM_PARAM = "from";
    private static final String TO_PARAM = "to";
    private static final String PULL_REQUEST_PARAM = "pullRequest";
    private static final String PAGE_INDEX_PARAM = "p";
    private static final String PAGE_SIZE_PARAM = "ps";
    private static final String CREATED_AFTER_PARAM = "createdAfter";
    private static final String CREATED_BEFORE_PARAM = "createdBefore";
    private static final String SIDELOADS_PARAM = "comments,rules,transitions,actions,users";
    private static final String ADDITIONAL_FIELDS_PARAM = "additionalFields";
    private static final String ORGANIZATION = "organization";
    private static final String GROUP_NAME = "name";
    private static final String FORMAT = "yyyy-MM-dd";
    private static final String COMPONENT_PARAM = "component";
    private static final String METRIC_KEYS_PARAM = "metricKeys";
    private static final String BRANCH_PARAM = "branch";
    private static final String COMPONENT_KEYS = "componentKeys";
    private static final String TIME_ZONE = "timeZone";
    private static final String SORT_FIELD = "s";
    private static final String ASC = "asc";
    private static final String TYPES = "types";
    public static final String QUALIFIERS = "qualifiers";
    public static final String QUALIFIERS_VALUE = "TRK";

    private final ClientHelper<SonarQubeClientException> clientHelper;
    private final String resourceUrl;
    private final int pageSize;
    private final String organization;
    private final String timezone;

    @Builder
    public SonarQubeClient(final OkHttpClient okHttpClient,
                           final ObjectMapper objectMapper,
                           String resourceUrl, int pageSize,
                           String organization, String timezone) {
        this.resourceUrl = resourceUrl;
        this.pageSize = pageSize != 0 ? pageSize : SonarQubeClientFactory.DEFAULT_PAGE_SIZE;
        this.organization = organization;
        this.timezone = timezone;
        this.clientHelper = ClientHelper.<SonarQubeClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(SonarQubeClientException.class)
                .build();
    }

    public int getPageSize() {
        return pageSize;
    }

    /**
     * fetch all Sonarqube quality gates.
     * returns a non-null value. Otherwise, it returns a response with an empty list.
     *
     * @return {@link QualityGateResponse} containing the {@link java.util.List<QualityGate>} and pagination data
     * @throws SonarQubeClientException when the client encounters an exception while making the call
     */
    public QualityGateResponse getQualityGates() throws SonarQubeClientException {
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(LIST_QUALITY_GATES_API_PATH);
        if (StringUtils.isNotBlank(organization)) {
            builder.addQueryParameter(ORGANIZATION, organization);
        }
        Request request = buildRequest(builder.build());
        log.info("Quality Gate url is : {}", request.url());
        ClientHelper.BodyAndHeaders<QualityGateResponse> page = clientHelper
                .executeAndParseWithHeaders(request, QualityGateResponse.class);
        return page.getBody();
    }

    /**
     * fetch all Sonarqube UserGroups based on the page index.
     * returns a non-null value. Otherwise, it returns a response with an empty list.
     *
     * @param pageIndex based on which the UserGroups are fetched page wise
     * @return {@link UserGroupResponse} containing the {@link java.util.List<Group>} and pagination data
     * @throws SonarQubeClientException when the client encounters an exception while making the call
     */
    public UserGroupResponse getUserGroups(int pageIndex) throws SonarQubeClientException {

        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(LIST_USER_GROUP_API_PATH)
                .addQueryParameter(PAGE_INDEX_PARAM, String.valueOf(pageIndex))
                .addQueryParameter(PAGE_SIZE_PARAM, String.valueOf(pageSize));
        if (StringUtils.isNotBlank(organization)) {
            builder.addQueryParameter(ORGANIZATION, organization);
        }
        Request request = buildRequest(builder.build());
        log.info("User Group url is : {}", request.url());
        ClientHelper.BodyAndHeaders<UserGroupResponse> page = clientHelper
                .executeAndParseWithHeaders(request, UserGroupResponse.class);
        return page.getBody();
    }

    /**
     * fetch all Sonarqube Users based on the page index.
     * returns a non-null value. Otherwise, it returns a response with an empty list.
     *
     * @param pageIndex based on which the Users are fetched page wise
     * @return {@link UserResponse} containing the {@link java.util.List<User>} and pagination data
     * @throws SonarQubeClientException when the client encounters an exception while making the call
     */
    public UserResponse getUsers(String groupName, int pageIndex) throws SonarQubeClientException {

        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(LIST_USERS_API_PATH)
                .addQueryParameter(PAGE_INDEX_PARAM, String.valueOf(pageIndex))
                .addQueryParameter(PAGE_SIZE_PARAM, String.valueOf(pageSize));
        if (StringUtils.isNotBlank(organization)) {
            builder.addQueryParameter(ORGANIZATION, organization);
        }
        if (StringUtils.isNotBlank(groupName)) {
            builder.addQueryParameter(GROUP_NAME, groupName);
        }
        Request request = buildRequest(builder.build());
        log.info("User url is : {}", request.url());
        ClientHelper.BodyAndHeaders<UserResponse> page = clientHelper
                .executeAndParseWithHeaders(request, UserResponse.class);
        return page.getBody();
    }

    /**
     * fetch all Sonarqube Projects  based on the page index.
     * returns a non-null value. Otherwise, it returns a response with an empty list.
     *
     * @param pageIndex based on which the Projects are fetched page wise
     * @return {@link ProjectResponse} containing the {@link java.util.List<Project>} and pagination data
     * @throws SonarQubeClientException when the client encounters an exception while making the call
     */
    public ProjectResponse getProjects(Boolean usePrivilegedAPIs, int pageIndex) throws SonarQubeClientException {
        Request request;
        if (usePrivilegedAPIs) {
            var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                    .addPathSegments(SEARCH_PROJECTS_API_PATH)
                    .addQueryParameter(PAGE_INDEX_PARAM, String.valueOf(pageIndex))
                    .addQueryParameter(PAGE_SIZE_PARAM, String.valueOf(pageSize))
                    .addQueryParameter(ADDITIONAL_FIELDS_PARAM, SIDELOADS_PARAM);
            if (StringUtils.isNotBlank(organization)) {
                builder.addQueryParameter(ORGANIZATION, organization);
            }
            request = buildRequest(builder.build());

        } else {
            var newBuilder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                    .addPathSegments(SEARCH_COMPONENTS_API_PATH)
                    .addQueryParameter(QUALIFIERS, QUALIFIERS_VALUE)
                    .addQueryParameter(PAGE_INDEX_PARAM, String.valueOf(pageIndex))
                    .addQueryParameter(PAGE_SIZE_PARAM, String.valueOf(pageSize));
            if (StringUtils.isNotBlank(organization)) {
                newBuilder.addQueryParameter(ORGANIZATION, organization);
            }
            request = buildRequest(newBuilder.build());
        }
        log.info("Using Privileged APIs : {}", usePrivilegedAPIs);
        log.info("Projects url is : {}", request.url());
        ClientHelper.BodyAndHeaders<ProjectResponse> response = clientHelper
                .executeAndParseWithHeaders(request, ProjectResponse.class);
        return response.getBody();
    }

    /**
     * fetch all Sonarqube ProjectAnalyses  based on the project.
     * returns a non-null value. Otherwise, it returns a response with an empty list.
     *
     * @param project   based on which the projectAnalyses are fetched
     * @param from      fetches the data from the specified date
     * @param pageIndex based on which the ProjectAnalyses are fetched page wise
     * @return {@link ProjectAnalysesResponse} containing the {@link java.util.List<Analyse>} and pagination data
     * @throws SonarQubeClientException when the client encounters an exception while making the call
     */
    public ProjectAnalysesResponse getProjectAnalyses(String project, Date from, Date to,
                                                      int pageIndex) throws SonarQubeClientException {
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(SEARCH_PROJECT_ANALYSES_API_PATH)
                .addQueryParameter(PAGE_INDEX_PARAM, String.valueOf(pageIndex))
                .addQueryParameter(PAGE_SIZE_PARAM, String.valueOf(pageSize))
                .addQueryParameter(PROJECT_KEY_PARAM, project);
        if (from != null) {
            String parsedDate = getSonarQubeDate(timezone, from);
            builder.addQueryParameter(FROM_PARAM, parsedDate);
        }
        if (to != null) {
            String parsedDate = getSonarQubeDate(timezone, to);
            builder.addQueryParameter(TO_PARAM, parsedDate);
        }
        Request request = buildRequest(builder.build());
        log.info("Project Anlayses url is : {}", request.url());
        ClientHelper.BodyAndHeaders<ProjectAnalysesResponse> page = clientHelper
                .executeAndParseWithHeaders(request, ProjectAnalysesResponse.class);
        return page.getBody();
    }

    /**
     * fetch all Sonarqube ProjectBranches based on the project.
     * returns a non-null value. Otherwise, it returns a response with an empty list.
     *
     * @param project based on which the ProjectBranches are fetched
     * @return {@link ProjectBranchResponse} containing the {@link java.util.List<Branch>} and pagination data
     * @throws SonarQubeClientException when the client encounters an exception while making the call
     */
    public ProjectBranchResponse getProjectBranches(String project) throws SonarQubeClientException {
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(LIST_PROJECT_BRANCHES_API_PATH)
                .addQueryParameter(PROJECT_KEY_PARAM, project);
        Request request = buildRequest(builder.build());
        ClientHelper.BodyAndHeaders<ProjectBranchResponse> page = clientHelper
                .executeAndParseWithHeaders(request, ProjectBranchResponse.class);
        log.info("Project Branches url is : {}", request.url());
        return page.getBody();
    }

    /**
     * fetch all Sonarqube PullRequests  based on the project.
     * returns a non-null value. Otherwise, it returns a response with an empty list.
     *
     * @param project based on which the PullRequests are fetched
     * @return {@link PullRequestResponse} containing the {@link java.util.List<PullRequest>} and pagination data
     * @throws SonarQubeClientException when the client encounters an exception making the call
     */
    public PullRequestResponse getPullRequests(String project) throws SonarQubeClientException {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(LIST_PULL_REQUEST_API_PATH)
                .addQueryParameter(PROJECT_KEY_PARAM, project)
                .build();
        Request request = buildRequest(url);
        log.info("Pull Requests url is : {}", request.url());
        ClientHelper.BodyAndHeaders<PullRequestResponse> page = clientHelper
                .executeAndParseWithHeaders(request, PullRequestResponse.class);
        return page.getBody();
    }

    /**
     * fetch all Sonarqube Issues  based on the project.
     * returns a non-null value. Otherwise, it returns a response with an empty list.
     *
     * @param componentKeys comma-list of project keys (case sensitive); fetches everything if empty
     * @param pullRequestId based on which the Issues are fetched
     * @param from          fetches the data from the specified date
     * @param pageIndex     based on which the ProjectAnalyses are fetched page wise
     * @param timezone      to resolve dates passed to 'from' (i.e. 'Europe/Paris', 'Z' or '+02:00')
     * @param sortField     Sort field (i.e. UPDATE_DATE, CLOSE_DATE, HOTSPOTS, FILE_LINE, SEVERITY, CREATION_DATE, ASSIGNEE, STATUS)
     * @param asc           Ascending sort  (i.e. true, false, yes, no)
     * @param types         Comma-separated list of types (i.e. CODE_SMELL, BUG, VULNERABILITY)
     * @return {@link IssueResponse} containing the {@link java.util.List<Issue>} and pagination data
     * @throws SonarQubeClientException when the client encounters an exception making the call
     */
    public IssueResponse getIssues(@Nullable String componentKeys, @Nullable String pullRequestId,
                                   @Nullable Date from, @Nullable Date to, @Nullable String timezone,
                                   @Nullable String sortField, @Nullable String asc,
                                   @Nullable String types, int pageIndex) throws SonarQubeClientException {
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(SEARCH_ISSUES_API_PATH)
                .addQueryParameter(PAGE_INDEX_PARAM, String.valueOf(pageIndex))
                .addQueryParameter(PAGE_SIZE_PARAM, String.valueOf(pageSize))
                .addQueryParameter(ADDITIONAL_FIELDS_PARAM, SIDELOADS_PARAM);
        if (StringUtils.isNotBlank(organization)) {
            builder.addQueryParameter(ORGANIZATION, organization);
        }
        if (StringUtils.isNotBlank(pullRequestId)) {
            builder.addQueryParameter(PULL_REQUEST_PARAM, pullRequestId);
        }
        if (StringUtils.isNotBlank(componentKeys)) {
            builder.addQueryParameter(COMPONENT_KEYS, componentKeys);
        }
        if (from != null) {
            String parsedDate = getSonarQubeDate(timezone, from);
            builder.addQueryParameter(CREATED_AFTER_PARAM, parsedDate);

        }
        if (to != null) {
            String parsedDate = getSonarQubeDate(timezone, to);
            builder.addQueryParameter(CREATED_BEFORE_PARAM, parsedDate);
        }
        if (StringUtils.isNotBlank(timezone)) {
            builder.addQueryParameter(TIME_ZONE, timezone);
        }
        if (StringUtils.isNotBlank(sortField)) {
            builder.addQueryParameter(SORT_FIELD, sortField);
        }
        if (StringUtils.isNotBlank(asc)) {
            builder.addQueryParameter(ASC, asc);
        }
        if (StringUtils.isNotBlank(types)) {
            builder.addQueryParameter(TYPES, types);
        }
        Request request = buildRequest(builder.build());
        log.info("Issues url is : {}", request.url());
        ClientHelper.BodyAndHeaders<IssueResponse> page = clientHelper
                .executeAndParseWithHeaders(request, IssueResponse.class);
        return page.getBody();
    }

    public MetricResponse getMetrics(int pageIndex) throws SonarQubeClientException {
        var url = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(METRICS_API_PATH)
                .addQueryParameter(PAGE_INDEX_PARAM, String.valueOf(pageIndex))
                .addQueryParameter(PAGE_SIZE_PARAM, String.valueOf(pageSize))
                .build();
        Request request = buildRequest(url);
        log.info("Metrics url is : {}", request.url());
        return clientHelper.executeAndParse(request, MetricResponse.class);
    }

    public ComponentWithMeasures getMeasures(String project,
                                             Set<String> metricKeys) throws SonarQubeClientException {
        return getMeasures(project, null, null, metricKeys);
    }

    public ComponentWithMeasures getMeasuresForBranch(String project,
                                                      String branch,
                                                      Set<String> metricKeys) throws SonarQubeClientException {
        return getMeasures(project, branch, null, metricKeys);
    }

    public ComponentWithMeasures getMeasuresForPullRequest(String project,
                                                           String pullRequest,
                                                           Set<String> metricKeys) throws SonarQubeClientException {
        return getMeasures(project, null, pullRequest, metricKeys);
    }

    private ComponentWithMeasures getMeasures(String project,
                                              String branch,
                                              String pullRequest,
                                              Set<String> metricKeys) throws SonarQubeClientException {
        var builder = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(MEASURES_API_PATH)
                .addQueryParameter(COMPONENT_PARAM, project)
                .addQueryParameter(METRIC_KEYS_PARAM, String.join(",", metricKeys));
        if (StringUtils.isNotBlank(branch)) {
            builder.addQueryParameter(BRANCH_PARAM, branch);
        }
        if (StringUtils.isNotBlank(pullRequest)) {
            builder.addQueryParameter(PULL_REQUEST_PARAM, pullRequest);
        }
        Request request = buildRequest(builder.build());
        log.info("Measures url is : {}", request.url());
        final MeasureResponse measureResponse = clientHelper.executeAndParse(request, MeasureResponse.class);
        return measureResponse.getComponent();
    }

    @NotNull
    private Request buildRequest(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
    }

    private String getSonarQubeDate(String timezone, Date from) {
        LocalDate zonedDate;
        if (timezone == null) {
            zonedDate = from.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        } else {
            zonedDate = from.toInstant().atZone(ZoneId.of(timezone)).toLocalDate();
        }
        return DateTimeFormatter.ofPattern(FORMAT).format(zonedDate);
    }
}
