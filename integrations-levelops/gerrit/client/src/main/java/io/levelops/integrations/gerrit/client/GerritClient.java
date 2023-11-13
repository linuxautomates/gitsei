package io.levelops.integrations.gerrit.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.integrations.gerrit.models.*;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Gerrit Client class which should be used for making calls to Gerrit.
 */
@Log4j2
public class GerritClient {

    private static final String PROJECTS = "projects/";
    private static final String DESCRIPTION_PARAMETER = "d";
    private static final String TREE_PARAMETER = "t";
    private static final String LIMIT = "n";
    private static final String OFFSET = "S";
    private static final String OPTIONAL_PARAMETER = "o";
    private static final String INCLUDES = "INCLUDES";
    private static final String MEMBERS = "MEMBERS";
    private static final String GROUP_LIMIT = "limit";
    private static final String GROUP_OFFSET = "start";
    private static final String GROUPS = "groups/";
    private static final String BACKSLASH = "/";
    private static final String BRANCHES = "branches";
    private static final String TAGS = "tags";
    private static final String LABELS = "labels";
    private static final String QUERY = "q";
    private static final String IS_ACTIVE = "is:active";
    private static final String ACCOUNTS = "accounts/";
    private static final String DETAILS = "DETAILS";
    private static final String ALL_EMAILS = "ALL_EMAILS";
    private static final String AFTER_DATE = "after:\"%s\"";
    private static final String DETAILED_LABELS = "DETAILED_LABELS";
    private static final String ALL_REVISIONS = "ALL_REVISIONS";
    private static final String ALL_COMMITS = "ALL_COMMITS";
    private static final String ALL_FILES = "ALL_FILES";
    private static final String DETAILED_ACCOUNTS = "DETAILED_ACCOUNTS";
    private static final String SUBMITTABLE = "SUBMITTABLE";
    private static final String CURRENT_ACTIONS = "CURRENT_ACTIONS";
    private static final String CHANGE_ACTIONS = "CHANGE_ACTIONS";
    private static final String REVIEWER_UPDATES = "REVIEWER_UPDATES";
    private static final String MESSAGES = "MESSAGES";
    private static final String REVIEWED = "REVIEWED";
    private static final String CHANGES = "changes/";
    private static final String YYYY_MM_DD_HH_MM_SS_SSS_Z = "yyyy-MM-dd HH:mm:ss.SSS Z";
    private static final String AUTHENTICATION_PARAMETER = "a/";
    private static final String REVIEWERS = "reviewers";
    private static final String REVISIONS = "revisions";
    private static final Integer PAGE_LIMIT = 50;

    private final ClientHelper<GerritClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String resourceUrl;
    private final boolean enrichmentEnabled;

    /**
     * all arg constructor for {@link GerritClient} class
     *
     * @param okHttpClient      {@link OkHttpClient} object to be used for making http calls
     * @param objectMapper      {@link ObjectMapper} for deserializing the responses
     * @param resourceUrl       Gerrit base url
     * @param enrichmentEnabled true if the responses need to be enriched, otherwise false
     */
    @Builder
    public GerritClient(final OkHttpClient okHttpClient, final ObjectMapper objectMapper,
                        String resourceUrl, Boolean enrichmentEnabled) {
        this.resourceUrl = resourceUrl;
        this.enrichmentEnabled = MoreObjects.firstNonNull(enrichmentEnabled, true);
        this.objectMapper = objectMapper;
        this.clientHelper = ClientHelper.<GerritClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(GerritClientException.class)
                .build();
    }

    public Stream<ProjectInfo> streamProjects() {
        return PaginationUtils.stream(0, PAGE_LIMIT, offset -> {
            try {
                return getProjects(offset, PAGE_LIMIT);
            } catch (GerritClientException e) {
                log.error("Encountered gerrit client error ", e);
                return null;
            }
        });
    }

    public Stream<ChangeInfo> streamChanges(GerritQuery query) {
        return PaginationUtils.stream(0, PAGE_LIMIT, offset -> {
            try {
                return getChanges(offset, PAGE_LIMIT, query);
            } catch (GerritClientException e) {
                log.error("Encountered gerrit client error ", e);
                return null;
            }
        });
    }

    /**
     * Lists the projects accessible by the caller.
     *
     * @param offset offset to start fetching data from
     * @param limit  limit of the query result
     * @return {@link List<ProjectInfo>} contains details of the project
     * @throws GerritClientException when the client encounters an exception while making the call
     */
    public List<ProjectInfo> getProjects(Integer offset, Integer limit) throws GerritClientException {
        var url = HttpUrl.parse(resourceUrl + AUTHENTICATION_PARAMETER).newBuilder()
                .addPathSegments(PROJECTS)
                .addQueryParameter(DESCRIPTION_PARAMETER, null)
                .addQueryParameter(TREE_PARAMETER, null)
                .addQueryParameter(OFFSET, String.valueOf(offset))
                .addQueryParameter(LIMIT, String.valueOf(limit))
                .build();
        Request request = buildRequest(url);
        var response = clientHelper.executeRequestWithHeaders(request);
        String body = formatDateStrings(stripPrefix(response.getBody()));
        Map<String, ProjectInfo> responseBody = clientHelper.parseResponse(body,
                objectMapper.getTypeFactory().constructParametricType(HashMap.class, String.class, ProjectInfo.class));
        return responseBody.entrySet().stream()
                .map(project -> project.getValue().toBuilder().name(project.getKey()).build())
                .collect(Collectors.toList());
    }

    /**
     * fetches List of branches of a project.
     *
     * @param encodedProjectName URL encoded project name of which branches need to be fetched
     * @return {@link List<ProjectInfo.BranchInfo>} contains ref to the branch.
     * @throws GerritClientException when the client encounters an exception while making the call
     */
    public List<ProjectInfo.BranchInfo> getBranches(String encodedProjectName) throws GerritClientException {
        var url = HttpUrl.parse(resourceUrl + AUTHENTICATION_PARAMETER).newBuilder()
                .addPathSegments(PROJECTS + encodedProjectName)
                .addPathSegment(BRANCHES)
                .build();
        Request request = buildRequest(url);
        var response = clientHelper.executeRequestWithHeaders(request);
        String body = stripPrefix(response.getBody());
        return clientHelper.parseResponse(body,
                objectMapper.getTypeFactory().constructParametricType(List.class, ProjectInfo.BranchInfo.class));
    }

    /**
     * fetches the list of Tags associated with the project
     *
     * @param encodedProjectName URL encoded project name of which tags need to be fetched
     * @return {@link ProjectInfo.TagInfo} contains info about the tag and tagger.
     * @throws GerritClientException when the client encounters an exception while making the call
     */
    public List<ProjectInfo.TagInfo> getTags(String encodedProjectName) throws GerritClientException {
        var url = HttpUrl.parse(resourceUrl + AUTHENTICATION_PARAMETER).newBuilder()
                .addPathSegments(PROJECTS + encodedProjectName)
                .addPathSegment(TAGS)
                .build();
        Request request = buildRequest(url);
        var response = clientHelper.executeRequestWithHeaders(request);
        String body = formatDateStrings(stripPrefix(response.getBody()));
        return clientHelper.parseResponse(body,
                objectMapper.getTypeFactory().constructParametricType(List.class, ProjectInfo.TagInfo.class));
    }

    /**
     * fetches the list of Tags associated with the project
     *
     * @param encodedProjectName URL encoded project name of which tags need to be fetched
     * @return {@link ProjectInfo.LabelDefinitionInfo}
     * @throws GerritClientException when the client encounters an exception while making the call
     */
    public List<ProjectInfo.LabelDefinitionInfo> getLabels(String encodedProjectName) throws GerritClientException {
        var url = HttpUrl.parse(resourceUrl + AUTHENTICATION_PARAMETER).newBuilder()
                .addPathSegments(PROJECTS + encodedProjectName)
                .addPathSegment(LABELS)
                .build();
        Request request = buildRequest(url);
        var response = clientHelper.executeRequestWithHeaders(request);
        String body = stripPrefix(response.getBody());
        return clientHelper.parseResponse(body,
                objectMapper.getTypeFactory().constructParametricType(List.class, ProjectInfo.LabelDefinitionInfo.class));
    }

    /**
     * Fetches List of groups accessible by the caller
     *
     * @param offset offset to start fetching data from
     * @param limit  limit of the query result
     * @return {@link List<GroupInfo>}
     * @throws GerritClientException when the client encounters an exception while making the call
     */
    public List<GroupInfo> getGroups(Integer offset, Integer limit) throws GerritClientException {
        var url = HttpUrl.parse(resourceUrl + AUTHENTICATION_PARAMETER).newBuilder()
                .addPathSegments(GROUPS)
                .addQueryParameter(OPTIONAL_PARAMETER, INCLUDES)
                .addQueryParameter(OPTIONAL_PARAMETER, MEMBERS)
                .addQueryParameter(GROUP_LIMIT, String.valueOf(limit))
                .addQueryParameter(GROUP_OFFSET, String.valueOf(offset))
                .build();
        Request request = buildRequest(url);
        var response = clientHelper.executeRequestWithHeaders(request);
        String body = formatDateStrings(stripPrefix(response.getBody()));
        Map<String, GroupInfo> responseBody = clientHelper.parseResponse(body,
                objectMapper.getTypeFactory().constructParametricType(HashMap.class, String.class, GroupInfo.class));
        return responseBody.entrySet().stream()
                .map(group -> group.getValue().toBuilder().name(group.getKey()).build())
                .collect(Collectors.toList());
    }

    /**
     * Fetches List of accounts accessible by the caller
     *
     * @param offset offset to start fetching data from
     * @param limit  limit of the query result
     * @return {@link AccountInfo} contains details of the account like email, username etc
     * @throws GerritClientException when the client encounters an exception while making the call
     */
    public List<AccountInfo> getAccounts(Integer offset, Integer limit) throws GerritClientException {
        var url = HttpUrl.parse(resourceUrl + AUTHENTICATION_PARAMETER).newBuilder()
                .addPathSegments(ACCOUNTS)
                .addQueryParameter(OPTIONAL_PARAMETER, DETAILS)
                .addQueryParameter(OPTIONAL_PARAMETER, ALL_EMAILS)
                .addQueryParameter(LIMIT, String.valueOf(limit))
                .addQueryParameter(OFFSET, String.valueOf(offset))
                .addQueryParameter(QUERY, IS_ACTIVE)
                .build();
        Request request = buildRequest(url);
        var response = clientHelper.executeRequestWithHeaders(request);
        String body = stripPrefix(response.getBody());
        return clientHelper.parseResponse(body,
                objectMapper.getTypeFactory().constructParametricType(List.class, AccountInfo.class));
    }

    /**
     * Fetches List of changes accessible by the caller
     *
     * @param offset offset to start fetching data from
     * @param limit  limit of the query result
     * @param query  {@link GerritQuery} contains the integrationId and date from which changes has to be queried
     * @return {@link List<ChangeInfo>} contains all the details about a change like reviews, reviewers, commits etc
     * @throws GerritClientException when the client encounters an exception while making the call
     */
    public List<ChangeInfo> getChanges(Integer offset, Integer limit, GerritQuery query) throws GerritClientException {
        SimpleDateFormat sdf = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS_SSS_Z);
        if (query.getAfter() == null) {
            return Collections.emptyList();
        }
        String after = URLEncoder.encode(sdf.format(query.getAfter()), StandardCharsets.UTF_8);
        var url = HttpUrl.parse(resourceUrl + AUTHENTICATION_PARAMETER).newBuilder()
                .addPathSegments(CHANGES)
                .addQueryParameter(LIMIT, String.valueOf(limit))
                .addQueryParameter(OFFSET, String.valueOf(offset))
                .addQueryParameter(OPTIONAL_PARAMETER, DETAILED_LABELS)
                .addQueryParameter(OPTIONAL_PARAMETER, ALL_REVISIONS)
                .addQueryParameter(OPTIONAL_PARAMETER, ALL_COMMITS)
                .addQueryParameter(OPTIONAL_PARAMETER, DETAILED_ACCOUNTS)
                .addQueryParameter(OPTIONAL_PARAMETER, SUBMITTABLE)
                .addQueryParameter(OPTIONAL_PARAMETER, ALL_FILES)
                .addQueryParameter(OPTIONAL_PARAMETER, CURRENT_ACTIONS)
                .addQueryParameter(OPTIONAL_PARAMETER, CHANGE_ACTIONS)
                .addQueryParameter(OPTIONAL_PARAMETER, REVIEWER_UPDATES)
                .addQueryParameter(OPTIONAL_PARAMETER, MESSAGES)
                .addQueryParameter(OPTIONAL_PARAMETER, REVIEWED)
                .addEncodedQueryParameter(QUERY, String.format(AFTER_DATE, after))
                .build();
        Request request = buildRequest(url);
        var response = clientHelper.executeRequestWithHeaders(request);
        String body = formatDateStrings(stripPrefix(response.getBody()));
        return clientHelper.parseResponse(body,
                objectMapper.getTypeFactory().constructParametricType(List.class, ChangeInfo.class));
    }

    /**
     * date strings from gerrit response are in the form of "yyyy-MM-dd hh:mm:ss.SSSSSSSSS" which cannot be parsed by
     * {@link java.time.LocalDateTime} so these are converted to "yyyy-MM-ddThh:mm:ss.SSSSSSSSS"
     *
     * @param body response body
     * @return response body with parsable date strings
     */
    private String formatDateStrings(String body) {
        Pattern datePattern = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{9}");
        Matcher matcher = datePattern.matcher(body);
        List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        for (String match : matches) {
            body = body.replace(match, match.replace(" ", "T"));
        }
        return body;
    }

    /**
     * Fetches list of Reviewers for a revision
     *
     * @param changeId   Id of the change
     * @param revisionId This can be:
     *                   - the literal current to name the current patch set/revision
     *                   - a commit ID ("674ac754f91e64a0efb8087e59a176484bd534d1")
     *                   - an abbreviated commit ID that uniquely identifies one revision of the change ("674ac754"), at least 4 digits are required
     *                   - a legacy numeric patch number ("1" for first patch set of the change)
     *                   - "0", or the literal edit for a change edit
     * @return {@link List<ReviewerInfo>} contains all the details of Account user in addition with approvals
     * @throws GerritClientException when the client encounters an exception while making the call
     */
    public List<ReviewerInfo> getRevisionReviewers(String changeId, String revisionId) throws GerritClientException {
        var url = HttpUrl.parse(resourceUrl + AUTHENTICATION_PARAMETER).newBuilder()
                .addPathSegments(CHANGES + changeId)
                .addPathSegment(REVISIONS)
                .addPathSegment(revisionId)
                .addPathSegment(REVIEWERS)
                .build();
        Request request = buildRequest(url);
        var response = clientHelper.executeRequestWithHeaders(request);
        String body = stripPrefix(response.getBody());
        return clientHelper.parseResponse(body,
                objectMapper.getTypeFactory().constructParametricType(List.class, ReviewerInfo.class));
    }

    /**
     * To prevent against Cross Site Script Inclusion (XSSI) attacks, the JSON response body from gerrit starts with a
     * magic prefix (")]}'") line that must be stripped before feeding the rest of the response body to a JSON parser.
     * This method strips of the magic prefix.
     *
     * @param response gerrit response
     * @return stripped response
     */
    private String stripPrefix(String response) {
        return response.substring(response.indexOf('\n') + 1);
    }

    @NotNull
    private Request buildRequest(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
    }

    /**
     * returns true if the tickets need to be enriched otherwise false
     *
     * @return boolean value telling if the Zendesk tickets need to be enriched
     */
    public boolean isEnrichmentEnabled() {
        return enrichmentEnabled;
    }

}
