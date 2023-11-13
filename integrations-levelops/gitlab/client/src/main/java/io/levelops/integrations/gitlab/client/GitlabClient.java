package io.levelops.integrations.gitlab.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.client.exceptions.HttpException;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.IngestionFailure;
import io.levelops.commons.functional.IngestionResult;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.integrations.gitlab.models.GitlabBranch;
import io.levelops.integrations.gitlab.models.GitlabChange;
import io.levelops.integrations.gitlab.models.GitlabCommit;
import io.levelops.integrations.gitlab.models.GitlabEvent;
import io.levelops.integrations.gitlab.models.GitlabGroup;
import io.levelops.integrations.gitlab.models.GitlabIssue;
import io.levelops.integrations.gitlab.models.GitlabIssueNote;
import io.levelops.integrations.gitlab.models.GitlabJob;
import io.levelops.integrations.gitlab.models.GitlabMergeRequest;
import io.levelops.integrations.gitlab.models.GitlabMergeRequestChanges;
import io.levelops.integrations.gitlab.models.GitlabMilestone;
import io.levelops.integrations.gitlab.models.GitlabNote;
import io.levelops.integrations.gitlab.models.GitlabPipeline;
import io.levelops.integrations.gitlab.models.GitlabProject;
import io.levelops.integrations.gitlab.models.GitlabPushEvent;
import io.levelops.integrations.gitlab.models.GitlabRepository;
import io.levelops.integrations.gitlab.models.GitlabStateEvent;
import io.levelops.integrations.gitlab.models.GitlabStatistics;
import io.levelops.integrations.gitlab.models.GitlabTag;
import io.levelops.integrations.gitlab.models.GitlabTestReport;
import io.levelops.integrations.gitlab.models.GitlabUser;
import io.levelops.integrations.gitlab.models.GitlabUtils;
import io.levelops.integrations.gitlab.models.GitlabVariable;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nullable;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Gitlab Client class which should be used for making calls to Gitlab.
 */
@Log4j2
public class GitlabClient {
    private static final String TOTAL_COUNT_HEADER_KEY = "x-total";
    private final ClientHelper<GitlabClientException> clientHelper;
    private final String resourceUrl;
    private final ObjectMapper objectMapper;
    private final int DEFAULT_PAGE_SIZE = 20;
    private final int DEFAULT_PAGE = 1;
    private final int pageSize;
    public final boolean fetchFileDiff;
    public final boolean swallowExceptions;

    public GitlabClient(
            OkHttpClient okHttpClient,
            final ObjectMapper objectMapper,
            String resourceUrl,
            int pageSize,
            Boolean allowUnsafeSSL,
            boolean fetchFileDiff) {
        this(okHttpClient, objectMapper, resourceUrl, pageSize, allowUnsafeSSL, fetchFileDiff, false);
    }

    @Builder
    public GitlabClient(
            OkHttpClient okHttpClient,
            final ObjectMapper objectMapper,
            String resourceUrl,
            int pageSize,
            Boolean allowUnsafeSSL,
            boolean fetchFileDiff,
            boolean swallowExceptions) {
        this.pageSize = pageSize;
        this.objectMapper = objectMapper;
        this.resourceUrl = resourceUrl;
        this.fetchFileDiff = fetchFileDiff;
        this.swallowExceptions = swallowExceptions;
        if (BooleanUtils.isTrue(allowUnsafeSSL)) {
            try {
                okHttpClient = ClientHelper.configureToIgnoreCertificate(okHttpClient.newBuilder()).build();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                log.warn("Failed to configure Gitlab client to ignore SSL certificate validation", e);
            }
        }
        this.clientHelper = ClientHelper.<GitlabClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(GitlabClientException.class)
                .build();
    }

    public List<GitlabProject> getProjects(int page, int pageSize) throws GitlabClientException {
        return getProjects(page, pageSize, true);
    }

    /**
     * Get all projects whose lastActivity is between lastActivityAfter and lastActivityBefore
     *
     * @param page            - represents page number
     * @param pageSize        - number of records per page
     * @param checkMembership - should we enforce the membership check - should be true for everything except
     *                        for some on prem instances where customers have a unique setup
     * @return - list of GitlabProject
     * @throws GitlabClientException - when the client encounters an exception while making the call
     */
    public List<GitlabProject> getProjects(int page, int pageSize, boolean checkMembership) throws GitlabClientException {
        int pageNumber = (page <= 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize <= 0) ? DEFAULT_PAGE_SIZE : pageSize;
        countPerPage = Math.min(countPerPage, 100);
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addQueryParameter("membership", checkMembership ? "true" : "false")
                .addQueryParameter("order_by", "id") // MUST BE ID TO BE ABLE TO USE "PAGE" FOR PAGINATION!!!
                // Using "asc" ensures that if new projects are added between calls the pagination does not get messed up
                .addQueryParameter("sort", "asc")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<GitlabProject[]> response = clientHelper.executeAndParseWithHeaders(request, GitlabProject[].class);
        return Arrays.asList(response.getBody());
    }

    public int getProjectCount(boolean checkMembership) throws GitlabClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addQueryParameter("membership", checkMembership ? "true" : "false")
                .addQueryParameter("order_by", "id") // MUST BE ID TO BE ABLE TO USE "PAGE" FOR PAGINATION!!!
                .addQueryParameter("page", "1")
                .addQueryParameter("per_page", "10");
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        try {
            ClientHelper.BodyAndHeaders<String> response = clientHelper.executeRequestWithHeaders(request);
            String projectCountStr = response.getHeaders().get(TOTAL_COUNT_HEADER_KEY).get(0);
            return Integer.parseInt(projectCountStr);
        } catch (Exception e) {
            log.error("Failed to get project count", e);
            throw new GitlabClientException(e);
        }
    }

    public List<GitlabProject> getProjectByName(String projectName, int page, int pageSize) throws GitlabClientException {

        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;

        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addQueryParameter("search", projectName)
                .addQueryParameter("membership", "true")
                .addQueryParameter("order_by", "id") // MUST BE ID TO BE ABLE TO USE "PAGE" FOR PAGINATION!!!
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        ;
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<GitlabProject[]> response = clientHelper.executeAndParseWithHeaders(request, GitlabProject[].class);
        return Arrays.asList(response.getBody());

    }

    /**
     * Get all Repositories
     *
     * @param projectId - Id which is unique identifier for each project
     * @param page      - represents page number
     * @param pageSize  - number of records per page
     * @return - a list of GitlabRepository
     * @throws GitlabClientException - when the client encounters an exception while making the call
     */
    public List<GitlabRepository> getRepositories(String projectId, int page, int pageSize)
            throws GitlabClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("repository")
                .addPathSegment("tree")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabRepository>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabRepository.class));
        return response.getBody();
    }

    /**
     * Get all branches
     *
     * @param projectId - Id which is unique identifier for each project
     * @param page      - represents page number
     * @param pageSize  - number of records per page
     * @return - a list of GitlabBranch
     * @throws GitlabClientException - when the client encounters an exception while making the call
     */
    public List<GitlabBranch> getBranches(String projectId, int page, int pageSize) throws GitlabClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("repository")
                .addPathSegment("branches")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabBranch>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabBranch.class));
        return response.getBody();
    }

    /**
     * Get all commits created between sinceDate and untilDate
     *
     * @param projectId - Id which is unique identifier for each project
     * @param from      - starting date
     * @param to        - ending date
     * @param page      - represents page number
     * @param pageSize  - number of records per page
     * @return - list of GitlabCommit
     * @throws GitlabClientException - when the client encounters an exception while making the call
     */
    public List<GitlabCommit> getCommits(String projectId, Date from, Date to,
                                         int page, int pageSize) throws GitlabClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("repository")
                .addPathSegment("commits")
                .addQueryParameter("since", GitlabUtils.format(from))
                .addQueryParameter("until", GitlabUtils.format(to))
                .addQueryParameter("all", "true")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabCommit>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabCommit.class));
        return response.getBody();
    }

    public Stream<GitlabPushEvent> streamPushEvents(String projectId, Date from, Date to) {
        return PaginationUtils.stream(1, 1,
                RuntimeStreamException.wrap(page -> getPushEvents(projectId, from, to, page)));
    }

    /**
     * Get all commits created between sinceDate and untilDate
     *
     * @param projectId  - Id which is unique identifier for each project
     * @param from       - starting date
     * @param to         - ending date
     * @param pageNumber 1-indexed page number
     * @return - list of GitlabPushEvent
     * @throws GitlabClientException - when the client encounters an exception while making the call
     */
    public List<GitlabPushEvent> getPushEvents(String projectId, Date from, Date to, int pageNumber) throws GitlabClientException {
        from = new Date(from.toInstant().minus(1, ChronoUnit.DAYS).getEpochSecond());
        to = new Date(to.toInstant().plus(1, ChronoUnit.DAYS).getEpochSecond());
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("events")
                .addQueryParameter("after", GitlabUtils.format(from)) // this should be YYYY-MM-DD but gitlab ignores the time
                .addQueryParameter("before", GitlabUtils.format(to))
                .addQueryParameter("action", "pushed")
                .addQueryParameter("per_page", String.valueOf(DEFAULT_PAGE_SIZE))
                .addQueryParameter("page", String.valueOf(pageNumber));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        try {
            ClientHelper.BodyAndHeaders<List<GitlabPushEvent>> response = clientHelper
                    .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, GitlabPushEvent.class));
            return response.getBody();
        } catch (GitlabClientException e) {
            if (swallowExceptions) {
                log.warn("Swallowing exception and returning empty list", e);
                return List.of();
            }
            throw e;
        }
    }

    /**
     * Get all merge requests updated between updatedAfter and updatedBefore
     *
     * @param projectId     - Id which is unique identifier for each project
     * @param updatedAfter  - updated after this date
     * @param updatedBefore - updated before this date
     * @param page          - represents page number
     * @param pageSize      - number of records per page
     * @return - list of GitlabMergeRequests
     * @throws GitlabClientException - when the client encounters an exception while making the call
     */
    public List<GitlabMergeRequest> getMergeRequests(String projectId, Date updatedAfter, Date updatedBefore,
                                                     int page, int pageSize) throws GitlabClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("merge_requests")
                .addQueryParameter("scope", "all")
                .addQueryParameter("updated_after", GitlabUtils.format(updatedAfter))
                .addQueryParameter("updated_before", GitlabUtils.format(updatedBefore))
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabMergeRequest>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabMergeRequest.class));
        return response.getBody();
    }

    /**
     * get all issues updated between updatedAfter and updatedBefore
     *
     * @param updatedBefore - updated before this date
     * @param updatedAfter  - updated after this date
     * @param page          - represents page number
     * @param pageSize      - number of records per page
     * @return - a list of GitlabIssue
     * @throws GitlabClientException - when the client encounters an exception while making the call
     */
    public List<GitlabIssue> getIssues(Date updatedAfter, Date updatedBefore,
                                       int page, int pageSize) throws GitlabClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("issues")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        if (updatedAfter != null) {
            urlBuilder = urlBuilder
                    .addQueryParameter("updated_after", GitlabUtils.format(updatedAfter));
        }
        if (updatedBefore != null) {
            urlBuilder = urlBuilder
                    .addQueryParameter("updated_before", GitlabUtils.format(updatedBefore));
        }
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabIssue>> resultPage = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabIssue.class));
        return resultPage.getBody();
    }

    /**
     * Get Issue Statistics between updatedAfter and updatedBefore
     *
     * @param updatedBefore - issues modified before this date
     * @param updatedAfter  - issues modified after this date
     * @return - Statistics of issues - {all, open, closed}
     * @throws GitlabClientException - when the client encounters an exception while making the call
     */
    public GitlabStatistics getIssueStatistics(Date updatedAfter, Date updatedBefore)
            throws GitlabClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("issues_statistics")
                .addQueryParameter("updated_after", GitlabUtils.format(updatedAfter))
                .addQueryParameter("updated_before", GitlabUtils.format(updatedBefore));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<GitlabStatistics> resultPage = clientHelper
                .executeAndParseWithHeaders(request, GitlabStatistics.class);
        return resultPage.getBody();
    }

    /**
     * @param projectId     - unique identifier for project
     * @param createdAfter  - users created after this date
     * @param createdBefore - users created before this date
     * @param page          - represents page number
     * @param pageSize      - number of records per page
     * @return - list of GitlabUser
     * @throws GitlabClientException when the client encounters an exception while making the call
     */
    public List<GitlabUser> getUsers(String projectId, Date createdAfter, Date createdBefore,
                                     int page, int pageSize) throws GitlabClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("users")
                .addQueryParameter("created_after", GitlabUtils.format(createdAfter))
                .addQueryParameter("created_before", GitlabUtils.format(createdBefore))
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabUser>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabUser.class));
        return response.getBody();
    }

    // /users
    // For self-hosted gitlab instances + admin PATs this retrieves the emails of all users as well
    public List<GitlabUser> getUsers(
            @Nullable Date createdAfter,
            @Nullable Date createdBefore,
            int page,
            int pageSize) throws GitlabClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("users")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        if (createdAfter != null) {
            urlBuilder
                    .addQueryParameter("created_after", GitlabUtils.format(createdAfter));
        }
        if (createdBefore != null) {
            urlBuilder
                    .addQueryParameter("created_before", GitlabUtils.format(createdBefore));
        }
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabUser>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabUser.class));
        return response.getBody();
    }

    public Stream<GitlabUser> streamUsers(Date createdAfter, Date createdBefore, int perPage) {
        return PaginationUtils.stream(1, 1, page -> {
            try {
                return getUsers(createdAfter, createdBefore, page, perPage);
            } catch (GitlabClientException e) {
                log.warn("Failed to get users after page " + page, e);
                if (swallowExceptions) {
                    log.warn("Swallowing exception and returning empty list", e);
                    return List.of();
                }
                throw new RuntimeStreamException("Failed to get users after page " + page, e);
            }
        });
    }

    /**
     * @param page     - represents page number
     * @param pageSize - number of records per page
     * @return - list of GitlabGroup
     * @throws GitlabClientException - when the client encounters an exception while making the call
     */
    public IngestionResult<GitlabGroup> getGroups(int page, int pageSize) throws GitlabClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("groups")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabGroup>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabGroup.class));
        return IngestionResult.<GitlabGroup>builder().data(response.getBody()).build();
    }

    /**
     * Get project pipelines between two dates for projects
     *
     * @param projectId     - unique identifier for project
     * @param updatedAfter  - pipelines updated after this date
     * @param updatedBefore - pipelines updated before this date
     * @param page          - denotes page number
     * @param pageSize      - number of records per page
     * @return - list of GitlabPipeline
     * @throws GitlabClientException - when the client encounters an exception while making the call
     */
    public List<GitlabPipeline> getProjectPipelines(String projectId, Date updatedAfter, Date updatedBefore,
                                                    int page, int pageSize) throws GitlabClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("pipelines")
                .addQueryParameter("updated_after", GitlabUtils.format(updatedAfter))
                .addQueryParameter("updated_before", GitlabUtils.format(updatedBefore))
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabPipeline>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabPipeline.class));
        if (response.getCode() == 403) {
            log.info("Pipelines are disabled for the project {}", projectId);
            return List.of();
        }
        return response.getBody();
    }

    public GitlabPipeline getProjectPipeline(String projectId, String pipelineId) throws GitlabClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("pipelines")
                .addPathSegment(pipelineId);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<GitlabPipeline> response = clientHelper
                .executeAndParseWithHeaders(request, GitlabPipeline.class);
        return response.getBody();
    }

    public List<GitlabVariable> getPipelineVariables(String projectId, String pipelineId) throws GitlabClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("pipelines")
                .addPathSegment(pipelineId)
                .addPathSegment("variables");
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabVariable>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabVariable.class));
        return response.getBody();
    }

    public GitlabTestReport getPipelineTestReport(String projectId, String pipelineId) throws GitlabClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("pipelines")
                .addPathSegment(pipelineId)
                .addPathSegment("test_report");
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<GitlabTestReport> response = clientHelper
                .executeAndParseWithHeaders(request, GitlabTestReport.class);
        return response.getBody();
    }

    /**
     * Get project milestones
     *
     * @param projectId - unique identifier for project
     * @param page      - denotes page number
     * @param pageSize  - number of records per page
     * @return - a list of GitlabMilestone
     * @throws GitlabClientException - when the client encounters an exception while making the call
     */
    public List<GitlabMilestone> getMilestones(String projectId, int page,
                                               int pageSize) throws GitlabClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("milestones")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabMilestone>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabMilestone.class));
        return response.getBody();
    }

    /**
     * Get jobs for pipeline
     *
     * @param projectId  - unique identifier for project
     * @param pipelineId - unique identifier for pipeline
     * @param page       - denotes page number
     * @param pageSize   - number of records per page
     * @return - a list of GitlabJobs
     * @throws GitlabClientException - when the client encounters an exception while making the call
     */
    public List<GitlabJob> getJobs(String projectId, String pipelineId, int page,
                                   int pageSize) throws GitlabClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("pipelines")
                .addPathSegment(pipelineId)
                .addPathSegment("jobs")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabJob>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabJob.class));
        return response.getBody();
    }

    public List<GitlabIssueNote> getProjectIssueNotes(String projectId, String issueIID,
                                                      int page, int pageSize) throws GitlabClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("issues")
                .addPathSegment(issueIID)
                .addPathSegment("notes")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabIssueNote>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabIssueNote.class));
        return response.getBody();
    }

    public List<GitlabCommit> getMRCommits(String projectId, String mrIID, int page,
                                           int pageSize) throws GitlabClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("merge_requests")
                .addPathSegment(mrIID)
                .addPathSegment("commits")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabCommit>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabCommit.class));
        return response.getBody();
    }

    public List<GitlabStateEvent> getMRStateEvents(String projectId, String mrIID, int page,
                                                   int pageSize) throws GitlabClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("merge_requests")
                .addPathSegment(mrIID)
                .addPathSegment("resource_state_events")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabStateEvent>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabStateEvent.class));
        return response.getBody();
    }

    public List<GitlabEvent> getMREvents(String projectId, int page,
                                         int pageSize) throws GitlabClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("events")
                .addQueryParameter("target_type", "merge_request")
                .addQueryParameter("scope", projectId)
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabEvent>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabEvent.class));
        return response.getBody();
    }

    public Optional<GitlabMergeRequestChanges> getMRChanges(String projectId, String mrIID) {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("merge_requests")
                .addPathSegment(mrIID)
                .addPathSegment("changes");
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        try {
            ClientHelper.BodyAndHeaders<GitlabMergeRequestChanges> resultPage = clientHelper
                    .executeAndParseWithHeaders(request, GitlabMergeRequestChanges.class);
            return Optional.ofNullable(resultPage.getBody());
        } catch (GitlabClientException e) {
            log.warn("Could not fetch gitlab merge request changes projectId {} mrIID {}", projectId, mrIID, e);
            return Optional.empty();
        }
    }

    public Optional<GitlabCommit> getCommitWithStats(String projectId, String sha) {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("repository")
                .addPathSegment("commits")
                .addPathSegment(sha);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        try {
            ClientHelper.BodyAndHeaders<GitlabCommit> resultPage = clientHelper
                    .executeAndParseWithHeaders(request, GitlabCommit.class);
            return Optional.ofNullable(resultPage.getBody());
        } catch (GitlabClientException e) {
            log.warn("Could not fetch gitlab commit with stats for projectId {} sha {}", projectId, sha, e);
            return Optional.empty();
        }
    }

    public List<GitlabChange> getCommitChanges(String projectId, String commitId, int page,
                                               int pageSize) throws GitlabClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("repository")
                .addPathSegment("commits")
                .addPathSegment(commitId)
                .addPathSegment("diff")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabChange>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabChange.class));
        return response.getBody();
    }

    /**
     * Get Stream of GitlabProjects between two dates
     *
     * @param checkMembership - only get projects for which the user is a member. This should always be true, except
     *                        for some onprem gitlab instances where customers have a funky usecase.
     * @return - stream of GitlabProject
     */
    public Stream<GitlabProject> streamProjects(boolean checkMembership) {
        try {
            return PaginationUtils.stream(1, 1, page -> {
                try {
                    return getProjects(page, DEFAULT_PAGE_SIZE, checkMembership);
                } catch (GitlabClientException e) {
                    log.warn("Failed to get projects after page " + page, e);
                    if (swallowExceptions) {
                        log.warn("Swallowing exception and returning empty list", e);
                        return List.of();
                    }
                    throw new RuntimeStreamException("Failed to get projects after page " + page, e);
                }
            });
        } catch (RuntimeStreamException e) {
            log.warn("Failed to list all projects", e);
            throw new RuntimeStreamException("Failed to list all projects", e);
        }
    }

    public Stream<GitlabProject> streamProjectsByName(String projectName) {
        try {
            return PaginationUtils.stream(1, 1, page -> {
                try {
                    return getProjectByName(projectName, page, DEFAULT_PAGE_SIZE);
                } catch (GitlabClientException e) {
                    log.warn("Failed to get projects after page " + page, e);
                    if (swallowExceptions) {
                        log.warn("Swallowing exception and returning empty list", e);
                        return List.of();
                    }
                    throw new RuntimeStreamException("Failed to get projects after page " + page, e);
                }
            });
        } catch (RuntimeStreamException e) {
            log.warn("Failed to list all projects", e);
            throw new RuntimeStreamException("Failed to list all projects", e);
        }
    }

    /**
     * Get Stream of GitlabGroups
     *
     * @param perPage - number of Groups per page
     * @return - list of GitlabGroup
     */
    public Stream<IngestionResult<GitlabGroup>> streamGroups(int perPage) {
        try {
            return PaginationUtils.streamData(1, 1, page -> {
                try {
                    return getGroups(page, perPage);
                } catch (GitlabClientException e) {
                    String message = "Failed to get groups after page " + page;
                    log.warn(message, e);
                    return IngestionResult.<GitlabGroup>builder()
                            .ingestionFailures(Collections.singletonList(
                                    IngestionFailure.builder().message(message).severity(IngestionFailure.Severity.ERROR)
                                            .build())).build();
                }
            });
        } catch (RuntimeStreamException e) {
            log.warn("Failed to list all groups", e);
            throw new RuntimeStreamException("Failed to list all groups", e);
        }
    }

    public Stream<GitlabPipeline> streamPipelines(final String projectId, Date lastActivityAfter,
                                                  Date lastActivityBefore, int perPage) {
        try {
            return PaginationUtils.stream(1, 1, page -> {
                try {
                    return getProjectPipelines(projectId, lastActivityAfter,
                            lastActivityBefore, page, perPage);
                } catch (GitlabClientException e) {
                    log.warn("Failed to get pipelines after page " + page + " for projectId " + projectId, e);
                    if (isExceptionRootCause404(e)) {
                        log.warn("404 Error Code Found, returning empty list");
                        return List.of();
                    }
                    if (swallowExceptions) {
                        log.warn("Swallowing exception and returning empty list", e);
                        return List.of();
                    }
                    throw new RuntimeStreamException("Failed to get pipelines after page " + page + " for projectId " + projectId, e);
                }
            });
        } catch (RuntimeStreamException e) {
            log.warn("Failed to list all pipelines for projectId " + projectId, e);
            throw new RuntimeStreamException("Failed to list all pipelines for projectId " + projectId, e);
        }
    }

    public Stream<GitlabJob> streamJobs(final String projectId, String pipelineId, int perPage) {
        return PaginationUtils.stream(1, 1, page -> {
            try {
                return getJobs(projectId, pipelineId, page, perPage);
            } catch (GitlabClientException e) {
                log.warn("Failed to get jobs after page " + page + " for projectId " + projectId +
                        " pipelineId " + pipelineId, e);
                if (swallowExceptions) {
                    log.warn("Swallowing exception and returning empty list", e);
                    return List.of();
                }
                throw new RuntimeStreamException("Failed to get jobs after page " + page + " for projectId " + projectId +
                        " pipelineId " + pipelineId, e);
            }
        });
    }

    public Stream<GitlabCommit> streamProjectCommits(final String projectId, Date sinceDate,
                                                     Date untilDate, int perPage) {
        return PaginationUtils.stream(1, 1, page -> {
            try {
                return getCommits(projectId, sinceDate, untilDate, page, perPage);
            } catch (GitlabClientException e) {
                log.warn("Failed to get commits after page " + page + " for projectId " + projectId, e);
                if (isExceptionRootCause404(e)) {
                    log.warn("404 Error Code Found, returning empty list");
                    return List.of();
                }
                if (swallowExceptions) {
                    log.warn("Swallowing exception and returning empty list", e);
                    return List.of();
                }
                throw new RuntimeStreamException("Failed to get commits after page " + page + " for projectId " + projectId, e);
            }
        });
    }

    public Stream<GitlabBranch> streamProjectBranches(final String projectId, int perPage) {
        return PaginationUtils.stream(1, 1, page -> {
            try {
                return getBranches(projectId, page, perPage);
            } catch (GitlabClientException e) {
                log.warn("Failed to get branches after page " + page + " for projectId " + projectId, e);
                if (isExceptionRootCause404(e)) {
                    log.warn("404 Error Code Found, returning empty list");
                    return List.of();
                }
                if (swallowExceptions) {
                    log.warn("Swallowing exception and returning empty list", e);
                    return List.of();
                }
                throw new RuntimeStreamException("Failed to get branches after page " + page + " for projectId " + projectId, e);
            }
        });
    }

    public Stream<GitlabMergeRequest> streamMergeRequests(String projectId, Date from, Date to, int perPage) {
        return PaginationUtils.stream(1, 1, page -> {
            try {
                return getMergeRequests(projectId, from, to, page, perPage);
            } catch (GitlabClientException e) {
                log.warn("Failed to get merge requests after page " + page + " for projectId " + projectId, e);
                if (isExceptionRootCauseHttpError(e, List.of(404, 403))) {
                    // We may get a 403 if the repo is disabled
                    log.warn("404/403 Error Code Found, returning empty list");
                    return List.of();
                }
                if (swallowExceptions) {
                    log.warn("Swallowing exception and returning empty list", e);
                    return List.of();
                }
                throw new RuntimeStreamException("Failed to get merge requests after page " + page + " for projectId " + projectId, e);
            }
        });
    }

    public Stream<GitlabUser> streamUsers(String projectId, Date from, Date to, int perPage) {
        return PaginationUtils.stream(1, 1, page -> {
            try {
                return getUsers(projectId, from, to, page, perPage);
            } catch (GitlabClientException e) {
                log.warn("Failed to get users after page " + page + " for projectId " + projectId, e);
                if (swallowExceptions) {
                    log.warn("Swallowing exception and returning empty list", e);
                    return List.of();
                }
                throw new RuntimeStreamException("Failed to get users after page " + page + " for projectId " + projectId, e);
            }
        });
    }

    public Stream<GitlabIssue> streamIssues(Date from, Date to, int perPage) {
        try {
            return PaginationUtils.stream(1, 1, page -> {
                try {
                    return getIssues(from, to, page, perPage);
                } catch (GitlabClientException e) {
                    log.warn("Failed to get issues after page " + page, e);
                    if (isExceptionRootCause404(e)) {
                        log.warn("404 Error Code Found, returning empty list");
                        return List.of();
                    }
                    if (swallowExceptions) {
                        log.warn("Swallowing exception and returning empty list", e);
                        return List.of();
                    }
                    throw new RuntimeStreamException("Failed to get issues after page " + page, e);
                }
            });
        } catch (RuntimeStreamException e) {
            log.warn("Failed to list all issues", e);
            throw new RuntimeStreamException("Failed to list all issues", e);

        }
    }

    public Stream<GitlabMilestone> streamMilestones(String projectId, int perPage) {
        return PaginationUtils.stream(1, 1, page -> {
            try {
                return getMilestones(projectId, page, perPage);
            } catch (GitlabClientException e) {
                log.warn("Failed to get milestones after page " + page + " for projectId " + projectId, e);
                if (isExceptionRootCause404(e)) {
                    log.warn("404 Error Code Found, returning empty list");
                    return List.of();
                }
                if (swallowExceptions) {
                    log.warn("Swallowing exception and returning empty list", e);
                    return List.of();
                }
                throw new RuntimeStreamException("Failed to get milestones after page " + page + " for projectId " + projectId, e);
            }
        });
    }

    public Stream<GitlabCommit> streamMRCommit(final String projectId, final String mergeRequestIID, int perPage) {
        return PaginationUtils.stream(1, 1, page -> {
            try {
                return getMRCommits(projectId, mergeRequestIID, page, perPage);
            } catch (GitlabClientException e) {
                log.warn("Failed to get MR commits after page " + page + " for projectId " + projectId +
                        " mergeRequestID " + mergeRequestIID, e);
                if (swallowExceptions) {
                    log.warn("Swallowing exception and returning empty list", e);
                    return List.of();
                }
                throw new RuntimeStreamException("Failed to get MR commits after page " + page + " for projectId " + projectId +
                        " mergeRequestID " + mergeRequestIID, e);
            }
        });
    }

    public Stream<GitlabStateEvent> streamMRStateEvent(final String projectId,
                                                       final String mergeRequestIID, int perPage) {
        return PaginationUtils.stream(1, 1, page -> {
            try {
                return getMRStateEvents(projectId, mergeRequestIID, page, perPage);
            } catch (GitlabClientException e) {
                log.warn("Failed to get MR state events after page " + page + " for projectId " + projectId +
                        " mergeRequestID " + mergeRequestIID, e);
                if (swallowExceptions) {
                    log.warn("Swallowing exception and returning empty list", e);
                    return List.of();
                }
                throw new RuntimeStreamException("Failed to get MR state events after page " + page + " for projectId " + projectId +
                        " mergeRequestID " + mergeRequestIID, e);
            }
        });
    }

    public Stream<GitlabEvent> streamMREvent(final String projectId, int perPage) {
        return PaginationUtils.stream(1, 1, page -> {
            try {
                return getMREvents(projectId, page, perPage);
            } catch (GitlabClientException e) {
                log.warn("Failed to get MR events after page " + page + " for projectId " + projectId, e);
                if (isExceptionRootCause404(e)) {
                    log.warn("404 Error Code Found, returning empty list");
                    return List.of();
                }
                if (swallowExceptions) {
                    log.warn("Swallowing exception and returning empty list", e);
                    return List.of();
                }
                throw new RuntimeStreamException("Failed to get MR events after page " + page + " for projectId " + projectId, e);
            }
        });
    }

    public Stream<GitlabIssueNote> streamIssueNotes(final String projectId, final String issueIID, int perPage) {
        return PaginationUtils.stream(1, 1, page -> {
            try {
                return getProjectIssueNotes(projectId, issueIID, page, perPage);
            } catch (GitlabClientException e) {
                log.warn("Failed to get issue notes after page " + page + " for projectId " + projectId +
                        " issueID " + issueIID, e);
                if (swallowExceptions) {
                    log.warn("Swallowing exception and returning empty list", e);
                    return List.of();
                }
                throw new RuntimeStreamException("Failed to get issue notes after page " + page + " for projectId " + projectId +
                        " issueID " + issueIID, e);
            }
        });
    }

    public Stream<GitlabChange> streamCommitChanges(final String projectId, final String commitId, int perPage) {
        return PaginationUtils.stream(1, 1, page -> {
            try {
                return getCommitChanges(projectId, commitId, page, perPage);
            } catch (GitlabClientException e) {
                log.warn("Failed to get commit changes after page " + page + " for projectId " + projectId +
                        " commitId " + commitId, e);
                if (swallowExceptions) {
                    log.warn("Swallowing exception and returning empty list", e);
                    return List.of();
                }
                throw new RuntimeStreamException("Failed to get commit changes after page " + page + " for projectId " + projectId +
                        " commitId " + commitId, e);
            }
        });
    }

    public List<GitlabUser> getCommitUsers(String email) throws GitlabClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("users")
                .addQueryParameter("search", email);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabUser>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabUser.class));
        return response.getBody();
    }

    public List<GitlabTag> getTags(String projectId, int page, int pageSize) throws GitlabClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("repository")
                .addPathSegment("tags")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabTag>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabTag.class));
        return response.getBody();
    }

    public Stream<GitlabTag> streamTags(String projectId, int perPage) {
        try {
            return PaginationUtils.stream(1, 1, page -> {
                try {
                    return getTags(projectId, page, perPage);
                } catch (GitlabClientException e) {
                    log.warn("Failed to get tags for project" + projectId + " after page " + page, e);
                    if (isExceptionRootCause404(e)) {
                        log.warn("404 Error Code Found, returning empty list");
                        return List.of();
                    }
                    if (swallowExceptions) {
                        log.warn("Swallowing exception and returning empty list", e);
                        return List.of();
                    }
                    throw new RuntimeStreamException("Failed to get tags for project" + projectId + " after page " + page, e);
                }
            });
        } catch (RuntimeStreamException e) {
            log.warn("Failed to get tags for project" + projectId, e);
            throw new RuntimeStreamException("Failed to get tags for project" + projectId, e);
        }
    }

    public List<GitlabEvent> getMRCommentEvents(String projectId, String mergeRequestIid, int page, int pageSize) throws GitlabClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("projects")
                .addPathSegment(projectId)
                .addPathSegment("merge_requests")
                .addPathSegment(mergeRequestIid)
                .addPathSegment("notes")
                .addQueryParameter("order_by", "updated_at")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("per_page", String.valueOf(countPerPage));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<GitlabNote>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, GitlabNote.class));

        return response.getBody().stream()
                .map(GitlabNote::toEvent)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public Stream<GitlabEvent> streamMRCommentEvents(String projectId, String mergeRequestIid, int perPage) {
        try {
            return PaginationUtils.stream(1, 1, page -> {
                try {
                    return getMRCommentEvents(projectId, mergeRequestIid, page, perPage);
                } catch (GitlabClientException e) {
                    log.warn("Failed to get notes for project" + projectId + ", MR: " + mergeRequestIid + " after page " + page, e);
                    if (swallowExceptions) {
                        log.warn("Swallowing exception and returning empty list", e);
                        return List.of();
                    }
                    throw new RuntimeStreamException("Failed to get notes for project" + projectId + ", MR: " + mergeRequestIid + " after page " + page, e);
                }
            });
        } catch (RuntimeStreamException e) {
            log.warn("Failed to get MRs for project" + projectId + ", MR: " + mergeRequestIid, e);
            throw new RuntimeStreamException("Failed to get tags for project" + projectId + ", MR: " + mergeRequestIid, e);
        }
    }


    /**
     * @return HttpUrl
     */
    private HttpUrl.Builder baseUrlBuilder() {
        return Objects.requireNonNull(HttpUrl.parse(resourceUrl + GitlabConstants.BASE_URL)).newBuilder();
    }


    private boolean isExceptionRootCause404(Throwable e) {
        return isExceptionRootCauseHttpError(e, List.of(404));
    }

    private boolean isExceptionRootCauseHttpError(Throwable e, List<Integer> httpErrorCodes) {
        Throwable rootCause = ExceptionUtils.getRootCause(e);
        if (rootCause instanceof HttpException) {
            HttpException httpException = (HttpException) rootCause;
            return httpException.getCode() != null && httpErrorCodes.contains(httpException.getCode());
        }
        return false;
    }
}