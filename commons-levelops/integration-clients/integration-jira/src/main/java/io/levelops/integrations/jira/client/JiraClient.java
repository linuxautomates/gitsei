package io.levelops.integrations.jira.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Integration.Authentication;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.integrations.jira.models.JiraApiSearchQuery;
import io.levelops.integrations.jira.models.JiraApiSearchResult;
import io.levelops.integrations.jira.models.JiraBoard;
import io.levelops.integrations.jira.models.JiraBoardResult;
import io.levelops.integrations.jira.models.JiraCommentsResult;
import io.levelops.integrations.jira.models.JiraCreateIssueFields;
import io.levelops.integrations.jira.models.JiraCreateMeta;
import io.levelops.integrations.jira.models.JiraDeploymentType;
import io.levelops.integrations.jira.models.JiraField;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraIssueFields.JiraStatus;
import io.levelops.integrations.jira.models.JiraMyself;
import io.levelops.integrations.jira.models.JiraPriority;
import io.levelops.integrations.jira.models.JiraPriorityScheme;
import io.levelops.integrations.jira.models.JiraProject;
import io.levelops.integrations.jira.models.JiraServerInfo;
import io.levelops.integrations.jira.models.JiraSprint;
import io.levelops.integrations.jira.models.JiraSprintResult;
import io.levelops.integrations.jira.models.JiraTransitions;
import io.levelops.integrations.jira.models.JiraUpdateIssue;
import io.levelops.integrations.jira.models.JiraUser;
import io.levelops.integrations.jira.models.JiraUserEmail;
import io.levelops.integrations.jira.models.JiraVersion;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class JiraClient {

    private static final String DEFAULT_API_VERSION = "2";
    private static final String DEFAULT_AGILE_API_VERSION = "1.0";
    private static final int DEFAULT_PAGE_SIZE = 50;
    private final ClientHelper<JiraClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final Supplier<String> jiraUrl;
    private final String apiVersion;
    private final String agileApiVersion;
    private final List<String> sensitiveFields;
    private final Authentication authType;

    /**
     * Jira Client
     *
     * @param okHttpClient         authenticated client
     * @param objectMapper         mapper
     * @param jiraUrlSupplier      supplier of Jira's instance or cloud URL (e.g pepsi.atlassian.net)
     * @param disableUrlSanitation Optional: If True, takes URL as is instead of adding https (False by default)
     * @param apiVersion           Optional: version of API to be used (e.g. "2" or "3") (Default: "2")
     */
    @Builder
    public JiraClient(OkHttpClient okHttpClient,
                      ObjectMapper objectMapper,
                      Supplier<String> jiraUrlSupplier,
                      Boolean disableUrlSanitation,
                      String apiVersion,
                      String agileApiVersion,
                      Boolean allowUnsafeSSL,
                      List<String> sensitiveFields,
                      Authentication authType) {
        this.objectMapper = objectMapper;
        this.apiVersion = StringUtils.defaultString(apiVersion, DEFAULT_API_VERSION);
        this.agileApiVersion = StringUtils.defaultString(agileApiVersion, DEFAULT_AGILE_API_VERSION);
        if (BooleanUtils.isTrue(disableUrlSanitation)) {
            this.jiraUrl = jiraUrlSupplier;
        } else {
            this.jiraUrl = () -> sanitizeUrl(jiraUrlSupplier.get());
        }
        if (BooleanUtils.isTrue(allowUnsafeSSL)) {
            try {
                okHttpClient = ClientHelper.configureToIgnoreCertificate(okHttpClient.newBuilder()).build();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                log.warn("Failed to configure Jira client to ignore SSL certificate validation", e);
            }
        }
        this.sensitiveFields = sensitiveFields;
        this.authType = authType;
        clientHelper = ClientHelper.<JiraClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(JiraClientException.class)
                .build();
    }

    protected static String sanitizeUrl(String jiraUrl) {
        if (Strings.isBlank(jiraUrl)) {
            return null;
        }
        jiraUrl = jiraUrl.trim();
        if (jiraUrl.startsWith("https://")) {
            return jiraUrl;
        }
        if (jiraUrl.startsWith("http://")) {
            return "https://" + jiraUrl.substring("http://".length());
        }
        return "https://" + jiraUrl;
    }

    private HttpUrl.Builder baseUrlBuilderWithoutVersion() {
        return HttpUrl.parse(jiraUrl.get()).newBuilder()
                .addPathSegment("rest")
                .addPathSegment("api");
    }

    private HttpUrl.Builder baseUrlBuilder() {
        return baseVersionBuilder(baseUrlBuilderWithoutVersion(), apiVersion);
    }

    private HttpUrl.Builder baseVersionBuilder(HttpUrl.Builder builder, String version) {
        return builder
                .addPathSegment(apiVersion);
    }

    private HttpUrl.Builder agileBaseUrlBuilder() {
        return Objects.requireNonNull(HttpUrl.parse(jiraUrl.get())).newBuilder()
                .addPathSegment("rest")
                .addPathSegment("agile")
                .addPathSegment(agileApiVersion);
    }

    // https://levelops.atlassian.net/rest/api/3/myself
    public JiraMyself getMyself() throws JiraClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("myself")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        JiraMyself jiraMyself = clientHelper.executeAndParse(request, JiraMyself.class);
        return JiraClientUtils.sanitizeJiraMyself(jiraMyself, sensitiveFields);
    }

    // https://levelops.atlassian.net/rest/api/3/project?expand=lead
    public List<JiraProject> getProjects() throws JiraClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("project")
                .addEncodedQueryParameter("expand", "description,lead,issueTypes,url,projectKeys,permissions")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        List<JiraProject> jiraProjects = clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructCollectionLikeType(List.class, JiraProject.class));
        return JiraClientUtils.sanitizeJiraProjects(jiraProjects, sensitiveFields);
    }

    public JiraProject getProject(String projectIdOrKey) throws JiraClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("project")
                .addPathSegment(projectIdOrKey)
                .addEncodedQueryParameter("expand", "description,lead,issueTypes,url,projectKeys,permissions")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        JiraProject jiraProject = clientHelper.executeAndParse(request, JiraProject.class);
        return JiraClientUtils.sanitizeJiraProject(jiraProject, sensitiveFields);
    }

    public List<JiraVersion> getProjectVersions(String projectIdOrKey) throws JiraClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("project")
                .addPathSegment(projectIdOrKey)
                .addPathSegment("versions")
                .addEncodedQueryParameter("expand", "description,lead,issueTypes,url,projectKeys,permissions")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructCollectionLikeType(List.class, JiraVersion.class));
    }

    public JiraApiSearchResult search(JiraApiSearchQuery searchQuery) throws JiraClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("search")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .header(ClientConstants.CONTENT_TYPE, ClientConstants.APPLICATION_JSON.toString())
                .post(clientHelper.createJsonRequestBody(searchQuery))
                .build();
        JiraApiSearchResult jiraApiSearchResult = clientHelper.executeAndParse(request, JiraApiSearchResult.class);
        return CollectionUtils.isNotEmpty(sensitiveFields) ? JiraClientUtils.sanitizeResults(jiraApiSearchResult, sensitiveFields) : jiraApiSearchResult;
    }

    public JiraCommentsResult getIssueComments(String issueIdOrKey) throws JiraClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("issue")
                .addPathSegment(issueIdOrKey)
                .addPathSegment("comment")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        JiraCommentsResult jiraCommentsResult = clientHelper.executeAndParse(request, JiraCommentsResult.class);
        return JiraClientUtils.sanitizeJiraCommentsResult(jiraCommentsResult, sensitiveFields);
    }

    public List<JiraField> getFields() throws JiraClientException {
        // https://levelops.atlassian.net/rest/api/3/field
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("field")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructCollectionLikeType(List.class, JiraField.class));
    }

    public Map<String, Object> getIssueChangeLog(String issueIdOrKey, Integer startAt) throws JiraClientException {
        // rest/api/3/issue/{issueIdOrKey}/changelog?maxResults=100&startAt=0
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("issue")
                .addPathSegment(issueIdOrKey)
                .addPathSegment("changelog")
                .addQueryParameter("maxResults", "100")
                .addQueryParameter("startAt", String.valueOf(MoreObjects.firstNonNull(startAt, 0)))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
    }

    // TODO
    public List<Map> getBacklog() throws JiraClientException {
        // https://your-domain.atlassian.net/rest/agile/1.0/backlog/issue
        HttpUrl url = HttpUrl.parse(jiraUrl.get()).newBuilder()
                .addPathSegment("rest")
                .addPathSegment("agile")
                .addPathSegment("1.0")
                .addPathSegment("backlog")
                .addPathSegment("issue")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructCollectionLikeType(List.class, Map.class));
    }

    // NOTE: This does not work for Jira Connect apps
    // https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-users/#api-rest-api-2-users-search-get
    public Stream<JiraUser> streamUsers() throws JiraClientException {
        if (authType != null && authType == Authentication.ATLASSIAN_CONNECT_JWT) {
            throw new JiraClientException("Connect Apps do not support listing users");
        }
        try {
            return PaginationUtils.stream(0, DEFAULT_PAGE_SIZE,
                    RuntimeStreamException.wrap(index -> listUsers(index, DEFAULT_PAGE_SIZE)));
        } catch (RuntimeStreamException e) {
            throw new JiraClientException(e);
        }
    }

    // NOTE: This does not work for Jira connect apps
    // https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-users/#api-rest-api-2-users-search-get
    public List<JiraUser> listUsers(Integer startAt, Integer maxResults) throws JiraClientException {
        // JIRA SERVER: https://docs.atlassian.com/software/jira/docs/api/REST/7.6.1/#api/2/user-findUsers
        // JIRA CLOUD: https://developer.atlassian.com/cloud/jira/platform/rest/v2/#api-rest-api-2-user-search-get
        if (authType != null && authType == Authentication.ATLASSIAN_CONNECT_JWT) {
            throw new JiraClientException("Connect Apps do not support listing users");
        }
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("users")
                .addPathSegment("search")
                .addQueryParameter("startAt", String.valueOf(MoreObjects.firstNonNull(startAt, 0)))
                .addQueryParameter("maxResults", String.valueOf(MoreObjects.firstNonNull(maxResults, 50)))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        List<JiraUser> jiraUsers = clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructCollectionLikeType(List.class, JiraUser.class));
        return JiraClientUtils.sanitizeJiraUsers(jiraUsers, sensitiveFields);
    }

    /**
     * Search users by display name (JIRA CLOUD) or both name and email (JIRA SERVER)
     */
    public List<JiraUser> searchUsers(String searchString, JiraDeploymentType deploymentType) throws JiraClientException {
        // JIRA SERVER: https://docs.atlassian.com/software/jira/docs/api/REST/7.6.1/#api/2/user-findUsers
        // JIRA CLOUD: https://developer.atlassian.com/cloud/jira/platform/rest/v2/#api-rest-api-2-user-search-get
        HttpUrl.Builder url = baseUrlBuilder()
                .addPathSegment("user")
                .addPathSegment("search");
        if (deploymentType == JiraDeploymentType.SERVER) {
            // JIRA SERVER ONLY (will search all fields, not only user name)
            url.addQueryParameter("username", searchString);
        } else {
            // JIRA CLOUD ONLY (will search display name & email if and only if email scope is there)
            // REQUIRES "BROWSE USERS" PERMISSION
            url.addQueryParameter("query", searchString);
        }
        Request request = new Request.Builder()
                .url(url.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        List<JiraUser> jiraUsers = clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructCollectionLikeType(List.class, JiraUser.class));
        return JiraClientUtils.sanitizeJiraUsers(jiraUsers, sensitiveFields);
    }

    /**
     * JIRA CLOUD - Will only work if access_user_email scope has been granted
     */
    public JiraUser getUserByEmail(String accountId) throws JiraClientException {
        // /rest/api/2/user/email
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("user")
                .addPathSegment("email")
                .addQueryParameter("accountId", accountId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        JiraUser jiraUser = clientHelper.executeAndParse(request, JiraUser.class);
        return JiraClientUtils.sanitizeJiraUser(jiraUser, sensitiveFields);
    }

    public JiraCreateMeta getCreateMeta(List<String> projectKeys) throws JiraClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("issue")
                .addPathSegment("createmeta")
                .addQueryParameter("projectKeys", String.join(",", projectKeys))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        JiraCreateMeta jiraCreateMeta = clientHelper.executeAndParse(request, JiraCreateMeta.class);
        return JiraClientUtils.sanitizeJiraCreateMeta(jiraCreateMeta, sensitiveFields);
    }

    /**
     * Create Jira issue
     *
     * @return id & key only
     */
    public JiraIssue createIssue(JiraCreateIssueFields fields) throws JiraClientException {
        // https://developer.atlassian.com/cloud/jira/platform/rest/v2/#api-rest-api-2-issue-post
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("issue")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .post(clientHelper.createJsonRequestBody(Map.of("fields", fields)))
                .build();
        return clientHelper.executeAndParse(request, JiraIssue.class);
    }

    public JiraServerInfo getServerInfo() throws JiraClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("serverInfo")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request, JiraServerInfo.class);
    }

    public void editIssue(String issueIdOrKey, JiraUpdateIssue updateIssue) throws JiraClientException {
        // https://docs.atlassian.com/software/jira/docs/api/REST/8.9.0/#api/2/issue-editIssue
        //PUT /rest/api/2/issue/{issueIdOrKey}

        HttpUrl url = baseUrlBuilder()
                .addPathSegment("issue")
                .addPathSegment(issueIdOrKey)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .put(clientHelper.createJsonRequestBody(Map.of("update", updateIssue)))
                .build();
        clientHelper.executeRequest(request);
    }


    public void doTransition(String issueIdOrKey, JiraUpdateIssue updateIssue, String transitionId) throws JiraClientException {
        // https://docs.atlassian.com/software/jira/docs/api/REST/8.9.0/#api/2/issue-doTransition

        HttpUrl url = baseUrlBuilder()
                .addPathSegment("issue")
                .addPathSegment(issueIdOrKey)
                .addPathSegment("transitions")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .post(clientHelper.createJsonRequestBody(Map.of(
                        "update", updateIssue,
                        "transition", Map.of("id", transitionId))))
                .build();
        clientHelper.executeRequest(request);
    }

    public JiraTransitions getTransitions(String issueIdOrKey) throws JiraClientException {
        // https://docs.atlassian.com/software/jira/docs/api/REST/8.9.0/#api/2/issue-getTransitions
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("issue")
                .addPathSegment(issueIdOrKey)
                .addPathSegment("transitions")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request, JiraTransitions.class);
    }

    /**
     * Requires `Manage watcher list` project permission.
     *
     * @param watcher can either be an accountId (JIRA CLOUD) or a name (JIRA SERVER)
     */
    public void addWatcher(String issueIdOrKey, String watcher) throws JiraClientException {
        // https://docs.atlassian.com/software/jira/docs/api/REST/8.9.0/#api/2/issue-addWatcher
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("issue")
                .addPathSegment(issueIdOrKey)
                .addPathSegment("watchers")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .post(clientHelper.createJsonRequestBody(watcher))
                .build();
        clientHelper.executeRequest(request);
    }

    /**
     * @throws if the version already exists, throws {@code HttpException(code=400, method=POST, url=https://levelops.atlassian.net/rest/api/2/version, body={"errorMessages":[],"errors":{"name":"A version with this name already exists in this project."}})}
     */
    public JiraVersion createVersion(String projectId, String versionName) throws JiraClientException {
        // https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-project-versions/#api-rest-api-2-version-post
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("version")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .post(clientHelper.createJsonRequestBody(Map.of(
                        "name", versionName,
                        "projectId", projectId)))
                .build();
        return clientHelper.executeAndParse(request, JiraVersion.class);
    }

    public JiraVersion getJiraVersion(JiraVersion version) throws JiraClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("version")
                .addPathSegment(version.getId())
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request, JiraVersion.class);
    }

    public List<JiraStatus> getStatuses() throws JiraClientException {
        // JIRA SERVER: https://docs.atlassian.com/software/jira/docs/api/REST/7.6.1/#api/2/status
        // JIRA CLOUD: https://developer.atlassian.com/cloud/jira/platform/rest/v2/api-group-workflow-statuses/#api-rest-api-2-status-get
        HttpUrl.Builder url = baseUrlBuilder()
                .addPathSegment("status");
        Request request = new Request.Builder()
                .url(url.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructCollectionLikeType(List.class, JiraStatus.class));

    }

    //https://levelops.atlassian.com/rest/api/2/priority
    public List<JiraPriority> getPriorities() throws JiraClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("priority")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        CollectionLikeType javaType = objectMapper.getTypeFactory().constructCollectionLikeType(List.class, JiraPriority.class);
        return clientHelper.executeAndParse(request, javaType);
    }

    public JiraPriorityScheme getPrioritySchemes(String projectId) throws JiraClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("project")
                .addPathSegment(projectId)
                .addPathSegment("priorityscheme")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request, JiraPriorityScheme.class);
    }

    public Stream<JiraBoard> streamBoards() throws JiraClientException {
        try {
            return PaginationUtils.stream(0, DEFAULT_PAGE_SIZE,
                    RuntimeStreamException.wrap(index -> getBoards(index, DEFAULT_PAGE_SIZE)));
        } catch (RuntimeStreamException e) {
            throw new JiraClientException(e);
        }
    }

    //https://levelops.atlassian.com/rest/agile/1.0/board
    public List<JiraBoard> getBoards(Integer startAt, Integer maxResults) throws JiraClientException {
        HttpUrl url = agileBaseUrlBuilder()
                .addPathSegment("board")
                .addQueryParameter("startAt", String.valueOf(MoreObjects.firstNonNull(startAt, 0)))
                .addQueryParameter("maxResults", String.valueOf(MoreObjects.firstNonNull(maxResults, 50)))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        JiraBoardResult boardResult = clientHelper.executeAndParse(request, JiraBoardResult.class);
        return boardResult.getBoards();
    }

    public Stream<JiraSprint> streamSprints(String boardId) throws JiraClientException {
        try {
            return PaginationUtils.stream(0, DEFAULT_PAGE_SIZE,
                    RuntimeStreamException.wrap(index -> getSprints(boardId, index, DEFAULT_PAGE_SIZE)));
        } catch (RuntimeStreamException e) {
            throw new JiraClientException(e);
        }
    }

    //https://levelops.atlassian.com/rest/agile/1.0/board/3/sprint
    public List<JiraSprint> getSprints(String boardId, Integer startAt, Integer maxResults) {
        HttpUrl url = agileBaseUrlBuilder()
                .addPathSegment("board")
                .addPathSegment(boardId)
                .addPathSegment("sprint")
                .addQueryParameter("startAt", String.valueOf(MoreObjects.firstNonNull(startAt, 0)))
                .addQueryParameter("maxResults", String.valueOf(MoreObjects.firstNonNull(maxResults, 50)))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        try {
            JiraSprintResult jiraSprintResult = clientHelper.executeAndParse(request, JiraSprintResult.class);
            return jiraSprintResult.getSprints();
        } catch (JiraClientException e) {
            log.warn("getSprints: Error while fetching sprints for board {}", boardId, e);
            return List.of();
        }
    }

    // /rest/api/3/user/email?accountId=<accountId>
    public JiraUserEmail getUserEmail(String accountId) throws JiraClientException {
        if (authType != null && authType != Authentication.ATLASSIAN_CONNECT_JWT) {
            throw new JiraClientException(authType + " does not support getting user emails.");
        }
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("user")
                .addPathSegment("email")
                .addQueryParameter("accountId", accountId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        try {
            return clientHelper.executeAndParse(request, JiraUserEmail.class);
        } catch (JiraClientException e) {
            log.warn("getUserEmail: Error while fetching email for accountId {}", accountId, e);
            return null;
        }
    }

    public List<JiraUserEmail> getUserEmailBulk(List<String> accountIds) throws JiraClientException {
        if (authType != null && authType != Authentication.ATLASSIAN_CONNECT_JWT) {
            throw new JiraClientException(authType + " does not support getting user emails.");
        }
        int MAX_ACCOUNT_IDS = 90; // This is enforced by the endpoint
        return Lists.partition(accountIds, MAX_ACCOUNT_IDS).stream()
                .map(this::getUserEmailInternal)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    // /rest/api/3/user/email/bulk?accountId=<accountId>&&accountId=<accountId>
    private List<JiraUserEmail> getUserEmailInternal(List<String> accountIds) {
        var urlBuilder = baseUrlBuilder()
                .addPathSegment("user")
                .addPathSegment("email")
                .addPathSegment("bulk");
        for (String accountId : accountIds) {
            urlBuilder.addQueryParameter("accountId", accountId);
        }
        HttpUrl url = urlBuilder.build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        try {
            return clientHelper.executeAndParse(request,
                    objectMapper.getTypeFactory().constructCollectionLikeType(List.class, JiraUserEmail.class));
        } catch (JiraClientException e) {
            log.warn("getUserEmailBulk: Error while fetching email for accountIds {}", accountIds, e);
            return List.of();
        }
    }
}
