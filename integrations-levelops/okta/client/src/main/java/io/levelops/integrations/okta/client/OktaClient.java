package io.levelops.integrations.okta.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.integrations.okta.models.*;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Okta client which should be used for making calls to Okta APIs.
 */
@Log4j2
public class OktaClient {

    private static final String LAST_UPDATED_FILTER = "lastUpdated gt \"%s\"";
    private static final String FILTER = "filter";
    private static final String GROUPS = "groups";
    private static final String OKTA_DATE_FOMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String USERS = "users";
    private static final String LINK = "link";
    private static final String LIMIT = "limit";
    private static final String META = "meta";
    private static final String TYPES = "types";
    private static final String USER = "user";
    private static final String ISO_8601 = "ISO 8601";
    private static final String LINKED_OBJECTS = "linkedObjects";
    public static final String SCHEMAS = "schemas";

    private final ClientHelper<OktaClientException> clientHelper;
    private final int pageSize;
    private final ObjectMapper objectMapper;
    private final String resourceUrl;
    private SimpleDateFormat sdf = new SimpleDateFormat(OKTA_DATE_FOMAT);
    private final boolean enrichmentEnabled;

    /**
     * @param okHttpClient      {@link OkHttpClient} object to be used for making http calls
     * @param objectMapper      {@link ObjectMapper} for deserializing the responses
     * @param pageSize          response page size
     * @param enrichmentEnabled true if the responses need to be enriched, otherwise false
     */
    @Builder
    public OktaClient(final OkHttpClient okHttpClient, final ObjectMapper objectMapper, String resourceUrl, int pageSize,
                      Boolean enrichmentEnabled) {
        this.resourceUrl = resourceUrl;
        this.pageSize = pageSize != 0 ? pageSize : OktaClientFactory.DEFAULT_PAGE_SIZE;
        this.objectMapper = objectMapper;
        this.enrichmentEnabled = MoreObjects.firstNonNull(enrichmentEnabled, true);
        this.clientHelper = ClientHelper.<OktaClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(OktaClientException.class)
                .build();
        this.sdf.setTimeZone(TimeZone.getTimeZone(ISO_8601));
    }

    /**
     * fetch all Okta groups created based on the query. The groups are fetched using cursor
     * if  {@link OktaScanQuery#getCursor()} returns non null
     * The groups are fetched by the {@link java.util.Date} if {@link OktaScanQuery#getFrom()}
     * returns a non-null value. Otherwise, it returns a response with an empty list.
     *
     * @param query {@link OktaScanQuery} based on which groups are fetched.
     * @return {@link PaginatedOktaResponse} containing {@link List<OktaGroup>}
     * @throws OktaClientException when the client encounters an exception while making the call
     */
    public PaginatedOktaResponse<OktaGroup> getGroups(OktaScanQuery query) throws OktaClientException {
        HttpUrl url;
        if (query.getCursor() != null) {
            url = HttpUrl.parse(query.getCursor());
        } else if (query.getFrom() != null) {
            Date lastUpdatedFrom = query.getFrom();
            url = HttpUrl.parse(resourceUrl).newBuilder()
                    .addPathSegments(GROUPS)
                    .addQueryParameter(FILTER, String.format(LAST_UPDATED_FILTER, sdf.format(lastUpdatedFrom)))
                    .addQueryParameter(LIMIT, String.valueOf(pageSize))
                    .build();
        } else {
            url = HttpUrl.parse(resourceUrl).newBuilder()
                    .addPathSegments(GROUPS)
                    .addQueryParameter(LIMIT, String.valueOf(pageSize))
                    .build();
        }
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<List<OktaGroup>> groupsResponse =
                clientHelper.executeAndParseWithHeaders(request, objectMapper.getTypeFactory().constructCollectionType(List.class, OktaGroup.class));
        List<String> cursors = getCursors(groupsResponse.getHeaders().get(LINK));
        return PaginatedOktaResponse.<OktaGroup>builder()
                .values(groupsResponse.getBody())
                .presentCursor(cursors.get(0))
                .nextCursor(cursors.get(1))
                .build();
    }

    /**
     * fetch all Okta users created based on the query. The groups are fetched using cursor
     * if  {@link OktaScanQuery#getCursor()} returns non null
     * The users are fetched by the {@link java.util.Date} if {@link OktaScanQuery#getFrom()}
     * returns a non-null value. Otherwise, it returns a response with an empty list.
     *
     * @param query {@link OktaScanQuery} based on which users are fetched.
     * @return {@link PaginatedOktaResponse} containing {@link List<OktaUser>}
     * @throws OktaClientException when the client encounters an exception while making the call
     */
    public PaginatedOktaResponse<OktaUser> getUsers(OktaScanQuery query) throws OktaClientException {
        HttpUrl url;
        if (query.getCursor() != null) {
            url = HttpUrl.parse(query.getCursor());
        } else if (query.getFrom() != null) {
            Date lastUpdatedFrom = query.getFrom();
            url = HttpUrl.parse(resourceUrl).newBuilder()
                    .addPathSegments(USERS)
                    .addQueryParameter(FILTER, String.format(LAST_UPDATED_FILTER, sdf.format(lastUpdatedFrom)))
                    .addQueryParameter(LIMIT, String.valueOf(pageSize))
                    .build();
        } else {
            url = HttpUrl.parse(resourceUrl).newBuilder()
                    .addPathSegments(USERS)
                    .addQueryParameter(LIMIT, String.valueOf(pageSize))
                    .build();
        }
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<List<OktaUser>> usersResponse =
                clientHelper.executeAndParseWithHeaders(request, objectMapper.getTypeFactory().constructCollectionType(List.class, OktaUser.class));
        List<String> cursors = getCursors(usersResponse.getHeaders().get(LINK));
        return PaginatedOktaResponse.<OktaUser>builder()
                .values(usersResponse.getBody())
                .presentCursor(cursors.get(0))
                .nextCursor(cursors.get(1))
                .build();
    }

    /**
     * fetch members of a group with id given
     *
     * @param groupId Id of a Group
     * @return {@link List<OktaGroup>}
     * @throws OktaClientException when the client encounters an exception while making the call
     */
    public List<OktaUser> getMembersOfGroup(String groupId) throws OktaClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(GROUPS)
                .addPathSegment(groupId)
                .addPathSegment(USERS)
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<List<OktaUser>> usersResponse =
                clientHelper.executeAndParseWithHeaders(request, objectMapper.getTypeFactory().constructCollectionType(List.class, OktaUser.class));
        return usersResponse.getBody();
    }

    public List<OktaGroup> getGroupsOfUser(String userId) throws OktaClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(USERS)
                .addPathSegment(userId)
                .addPathSegment(GROUPS)
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<List<OktaGroup>> groupsResponse =
                clientHelper.executeAndParseWithHeaders(request, objectMapper.getTypeFactory().constructCollectionType(List.class, OktaGroup.class));
        return groupsResponse.getBody();
    }

    /**
     * fetch all user types created till date
     *
     * @return {@link List<OktaUserType>}
     * @throws OktaClientException when the client encounters an exception while making the call
     */
    public List<OktaUserType> getUserTypes() throws OktaClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegment(META)
                .addPathSegment(TYPES)
                .addPathSegment(USER)
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<List<OktaUserType>> userTypes = clientHelper.executeAndParseWithHeaders(request,
                objectMapper.getTypeFactory().constructCollectionType(List.class, OktaUserType.class));
        return userTypes.getBody();
    }

    /**
     * fetch all Linked Objects created till date
     *
     * @return {@link List<OktaLinkedObject>}
     * @throws OktaClientException when the client encounters an exception while making the call
     */
    public List<OktaLinkedObject> getLinkedObjectDefinitions() throws OktaClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegment(META)
                .addPathSegment(SCHEMAS)
                .addPathSegment(USER)
                .addPathSegment(LINKED_OBJECTS)
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<List<OktaLinkedObject>> linkedObjects = clientHelper.executeAndParseWithHeaders(request,
                objectMapper.getTypeFactory().constructCollectionType(List.class, OktaLinkedObject.class));
        return linkedObjects.getBody();
    }

    /**
     * fetch all linked object user values of {@param relationName} for the user id
     *
     * @param userId       user Id for whom linked object values are to be fetched.
     * @param relationName relation name it can be either {@link OktaLinkedObject#getPrimary()#getName()} or
     *                     {@link OktaLinkedObject#getAssociated()} ()#getName()}
     * @return List of user id's linked with user id provided in arguments
     * @throws OktaClientException when the client encounters an exception while making the call
     */
    public List<String> getLinkedObjectUsers(String userId, String relationName) throws OktaClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegment(USERS)
                .addPathSegment(userId)
                .addPathSegment(LINKED_OBJECTS)
                .addPathSegment(relationName)
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<List<OktaLinkedUserValues>> links = clientHelper.executeAndParseWithHeaders(request,
                objectMapper.getTypeFactory().constructCollectionType(List.class, OktaLinkedUserValues.class));
        Pattern linkPattern = Pattern.compile(".*/users/(.*)");
        return links.getBody().stream()
                .map(OktaLinkedUserValues::getLinks)
                .map(OktaLinkedUserValues.Links::getSelf)
                .map(OktaHref::getHref)
                .map(linkPattern::matcher)
                .filter(Matcher::find)
                .map(link -> link.group(1))
                .collect(Collectors.toList());
    }

    /**
     * extract cursors from the headers of the response
     *
     * @param headers list of header of okta response
     * @return list of cursors for pagination
     */
    private List<String> getCursors(List<String> headers) {
        List<String> cursors = new ArrayList<>();
        if (headers.size() >= 1) {
            cursors.add(extractCursor(headers.get(0)));
            if (headers.size() == 2) {
                cursors.add(extractCursor(headers.get(1)));
            } else {
                cursors.add(null);
            }
        }
        return cursors;
    }

    /**
     * extracts cursor link for pagination from the header
     *
     * @param header header string containing cursor
     * @return cursor
     */
    private String extractCursor(String header) {
        Pattern pattern = Pattern.compile("<(.*)>.*");
        Matcher matcher = pattern.matcher(header);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * returns true if the data need to be enriched otherwise false
     *
     * @return boolean value telling if the Okta data need to be enriched
     */
    public boolean isEnrichmentEnabled() {
        return enrichmentEnabled;
    }

    @NotNull
    private Request buildRequest(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
    }
}
