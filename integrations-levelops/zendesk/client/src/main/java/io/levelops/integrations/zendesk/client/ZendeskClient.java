package io.levelops.integrations.zendesk.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.client.ClientHelper.BodyAndHeaders;
import io.levelops.integrations.zendesk.models.*;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Zendesk Client class which should be used for making calls to Zendesk.
 */
@Log4j2
public class ZendeskClient {

    private static final String CURSOR = "cursor";
    private static final String START_TIME = "start_time";
    private static final String EXPORT_API_PATH = "api/v2/incremental/tickets/cursor.json";
    private static final String JIRA_LINK_API_PATH = "api/services/jira/links";
    private static final String TICKET_ID = "ticket_id";
    private static final String PER_PAGE = "per_page";
    private static final String REQUEST_API_PATH = "api/v2/requests";
    private static final String JSON_EXT = ".json";
    private static final String COMMENTS = "comments";
    private static final String LIMIT = "limit";
    private static final String INCLUDE = "include";
    private static final String EXPORT_TICKETS_SIDELOAD_ENTITIES = "users,groups,metric_sets,brands,organizations";
    private static final String EXPORT_METRIC_EVENTS_API_PATH = "api/v2/incremental/ticket_metric_events.json";
    private static final String TICKET_API_PATH = "api/v2/tickets";
    private static final String COMMENT = "comment";
    private static final String TICKET = "ticket";
    private static final String SEARCH_API_PATH = "api/v2/search.json";
    private static final String SEARCH_QUERY_KEY = "query";
    private static final String USER_EMAIL_QUERY = "type:user email:";
    private static final String GROUP_NAME_QUERY = "type:group name:";
    private static final String FIELDS_API_PATH = "api/v2/ticket_fields";

    private final ClientHelper<ZendeskClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String resourceUrl;
    private final int pageSize;
    private final boolean enrichmentEnabled;
    private final boolean jiralinksEnabled;

    /**
     * all arg constructor for {@link ZendeskClient} class
     *
     * @param okHttpClient      {@link OkHttpClient} object to be used for making http calls
     * @param objectMapper      {@link ObjectMapper} for deserializing the responses
     * @param resourceUrl       Zendesk base url
     * @param pageSize          response page size
     * @param enrichmentEnabled true if the responses need to be enriched, otherwise false
     * @param jiralinksEnabled  true if jira links are enabled
     */
    @Builder
    public ZendeskClient(final OkHttpClient okHttpClient, final ObjectMapper objectMapper, String resourceUrl,
                         int pageSize, Boolean enrichmentEnabled, Boolean jiralinksEnabled) {
        this.resourceUrl = resourceUrl;
        this.pageSize = pageSize != 0 ? pageSize : ZendeskClientFactory.DEFAULT_PAGE_SIZE;
        this.enrichmentEnabled = MoreObjects.firstNonNull(enrichmentEnabled, true);
        this.objectMapper = objectMapper;
        this.jiralinksEnabled = MoreObjects.firstNonNull(jiralinksEnabled, false);
        this.clientHelper = ClientHelper.<ZendeskClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(ZendeskClientException.class)
                .build();
    }

    /**
     * fetch all Zendesk tickets based on the query. The tickets are fetched with the {@code cursor},
     * if {@link ZendeskTicketQuery#getCursor()} returns a non-null value.
     * The tickets are fetched by the {@link java.util.Date} if {@link ZendeskTicketQuery#getFrom()}
     * returns a non-null value. Otherwise, it returns a response with an empty list.
     *
     * @param query {@link ZendeskTicketQuery} based on which the tickets are fetched
     * @return {@link ExportTicketsResponse} containing the {@link java.util.List<Ticket>} and pagination data
     * @throws ZendeskClientException when the client encounters an exception while making the call
     */
    public ExportTicketsResponse getTickets(ZendeskTicketQuery query) throws ZendeskClientException {
        String queryParamName;
        String queryParamValue;
        if (query.getCursor() != null) {
            queryParamName = CURSOR;
            queryParamValue = query.getCursor();
        } else if (query.getFrom() != null) {
            queryParamName = START_TIME;
            queryParamValue = String.valueOf(query.getFrom().getTime() / 1000);
        } else {
            return ExportTicketsResponse.builder()
                    .tickets(Collections.emptyList())
                    .build();
        }
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(EXPORT_API_PATH)
                .addQueryParameter(queryParamName, queryParamValue)
                .addQueryParameter(INCLUDE, EXPORT_TICKETS_SIDELOAD_ENTITIES)
                .addQueryParameter(PER_PAGE, String.valueOf(pageSize))
                .build();
        Request request = buildRequest(url);
        BodyAndHeaders<ExportTicketsResponse> page = clientHelper.executeAndParseWithHeaders(request,
                ExportTicketsResponse.class);
        return page.getBody();
    }

    public ListTicketFieldsResponse getTicketFields() throws ZendeskClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(FIELDS_API_PATH)
                .build();
        Request request = buildRequest(url);
        BodyAndHeaders<ListTicketFieldsResponse> ticketFieldsResponse =
                clientHelper.executeAndParseWithHeaders(request, ListTicketFieldsResponse.class);
        return ticketFieldsResponse.getBody();
    }

    /**
     * Fetches the list of corresponding {@link JiraLink} for the Zendesk ticket with {@code ticketId}
     *
     * @param ticketId id of the Zendesk ticket
     * @return {@link GetJiraLinkResponse} containing the links
     * @throws ZendeskClientException when the client encounters an exception while making the call
     */
    public GetJiraLinkResponse getJiraLinks(long ticketId) throws ZendeskClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(JIRA_LINK_API_PATH)
                .addQueryParameter(TICKET_ID, String.valueOf(ticketId))
                .build();
        Request request = buildRequest(url);
        BodyAndHeaders<GetJiraLinkResponse> jiraLinkResponse =
                clientHelper.executeAndParseWithHeaders(request, GetJiraLinkResponse.class);
        return jiraLinkResponse.getBody();
    }

    /**
     * fetches all jira links for the account associated with {@code this} client. Limits the number of links in the
     * response to {@link ZendeskClient#pageSize}
     *
     * @return {@link GetJiraLinkResponse} containing the links
     * @throws ZendeskClientException when the client encounters an exception while making the call
     */
    public GetJiraLinkResponse getJiraLinks() throws ZendeskClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(JIRA_LINK_API_PATH)
                .addQueryParameter(LIMIT, String.valueOf(pageSize))
                .build();
        Request request = buildRequest(url);
        BodyAndHeaders<GetJiraLinkResponse> jiraLinkResponse =
                clientHelper.executeAndParseWithHeaders(request, GetJiraLinkResponse.class);
        return jiraLinkResponse.getBody();
    }

    /**
     * fetches all jira links for the account associated with {@code this} client. Paginates the response with
     * page size equal to {@link ZendeskClient#pageSize}.
     *
     * @return {@link ListRequestsResponse} containing the requests
     * @throws ZendeskClientException when the client encounters an exception while making the call
     */
    public ListRequestsResponse getRequestAttributes() throws ZendeskClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(REQUEST_API_PATH + JSON_EXT)
                .addQueryParameter(PER_PAGE, String.valueOf(pageSize))
                .build();
        Request request = buildRequest(url);
        return clientHelper.executeAndParse(request, ListRequestsResponse.class);
    }

    /**
     * Fetches the {@link Ticket.RequestAttributes} corresponding to the {@code ticketId}
     *
     * @param ticketId id of the Zendesk ticket
     * @return {@link Ticket.RequestAttributes} containing the corresponding request attributes for the ticket
     * @throws ZendeskClientException when the client encounters an exception while making the call
     */
    public Ticket.RequestAttributes getRequestAttributes(long ticketId) throws ZendeskClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(REQUEST_API_PATH)
                .addPathSegment(ticketId + JSON_EXT)
                .build();
        Request request = buildRequest(url);
        return clientHelper.executeAndParse(request, Ticket.RequestAttributes.class);
    }

    /**
     * Fetches the {@link RequestComment} for the request associated with the Zendesk ticket with {@code ticketId}
     *
     * @param ticketId id of the Zendesk ticket
     * @return {@link RequestCommentResponse} containing a list of {@link RequestComment} and pagination details
     * @throws ZendeskClientException when the client encounters an exception while making the call
     */
    public RequestCommentResponse getRequestComments(long ticketId) throws ZendeskClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(REQUEST_API_PATH)
                .addPathSegment(String.valueOf(ticketId))
                .addPathSegment(COMMENTS + JSON_EXT)
                .build();
        Request request = buildRequest(url);
        return clientHelper.executeAndParse(request, RequestCommentResponse.class);
    }

    /**
     * fetches the {@link TicketMetricEvent} for the account linked to {@code this} {@link ZendeskClient}
     *
     * @param startTime start epoch time to fetch events from
     * @return {@link ListTicketMetricEventResponse} containing the response from zendesk
     * @throws ZendeskClientException when the client encounters an exception while making the call
     */
    public ListTicketMetricEventResponse getTicketMetricEvents(long startTime) throws ZendeskClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(EXPORT_METRIC_EVENTS_API_PATH)
                .addQueryParameter(START_TIME, String.valueOf(startTime))
                .build();
        Request request = buildRequest(url);
        return clientHelper.executeAndParse(request, ListTicketMetricEventResponse.class);
    }

    /**
     * create a new ticket with the fields from the {@code ticket}
     *
     * @param ticket {@link TicketRequestBody} defining the fields for the ticket to be created
     * @return {@link WriteTicketResponse} response from zendesk
     * @throws ZendeskClientException when the client encounters an exception while making the call
     */
    public WriteTicketResponse createTicket(TicketRequestBody ticket) throws ZendeskClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(TICKET_API_PATH + JSON_EXT)
                .build();
        final Map<String, Object> ticketBody = getTicketBody(ticket);
        var request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .post(clientHelper.createJsonRequestBody(ticketBody))
                .build();
        return clientHelper.executeAndParse(request, WriteTicketResponse.class);
    }

    /**
     * updates the ticket having {@code id} with the fields from the {@code ticket}
     *
     * @param id     id of the ticket to be updated
     * @param ticket {@link TicketRequestBody} defining the fields to be updated
     * @return {@link WriteTicketResponse} response from zendesk
     * @throws ZendeskClientException when the client encounters an exception while making the call
     */
    public WriteTicketResponse updateTicket(long id, TicketRequestBody ticket) throws ZendeskClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(TICKET_API_PATH)
                .addPathSegment(id + JSON_EXT)
                .build();
        final Map<String, Object> ticketBody = getTicketBody(ticket);
        var request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .put(clientHelper.createJsonRequestBody(ticketBody))
                .build();
        return clientHelper.executeAndParse(request, WriteTicketResponse.class);
    }

    /**
     * resolves a user email address to the corresponding user id
     *
     * @param userEmail email address of the user
     * @return id of the user, if it exists else {@code null}, {@code null} for any error that may occur
     */
    public Long getUserIdByEmail(String userEmail) {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(SEARCH_API_PATH)
                .addQueryParameter(SEARCH_QUERY_KEY, USER_EMAIL_QUERY + userEmail)
                .build();
        Request request = buildRequest(url);
        return getIdFromSearchRequest(request);
    }

    /**
     * executes the {@code request} to get a {@link IdSearchResult} from the response
     *
     * @param request {@link Request} to be executed
     * @return id of the first entity returned in the result or {@code null} for any errors or empty response
     */
    @Nullable
    private Long getIdFromSearchRequest(Request request) {
        try {
            IdSearchResult result = clientHelper.executeAndParse(request, IdSearchResult.class);
            List<IdSearchResult.EntityId> ids = result.getResults();
            if (!ids.isEmpty()) {
                return ids.get(0).getId();
            } else {
                return null;
            }
        } catch (ZendeskClientException e) {
            log.error("getIdFromSearchRequest: error while fetching id for user: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * resilves a group name to the corresponding group id
     *
     * @param groupName name of the group
     * @return id of the group, or {@code null} for any errors
     */
    public Long getGroupIdByName(String groupName) {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(SEARCH_API_PATH)
                .addQueryParameter(SEARCH_QUERY_KEY, GROUP_NAME_QUERY + groupName)
                .build();
        Request request = buildRequest(url);
        return getIdFromSearchRequest(request);
    }

    @NotNull
    private Map<String, Object> getTicketBody(TicketRequestBody requestBody) {
        final Map<String, Object> ticket = objectMapper.convertValue(requestBody.getTicket(),
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        ticket.put(COMMENT, requestBody.getComment());
        final Map<String, Object> ticketRequestBody = new HashMap<>();
        ticketRequestBody.put(TICKET, ticket);
        return ticketRequestBody;
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

    /**
     * returns true if the jira links are enabled for the zendesk account associated with {@code this} client
     *
     * @return true jira links are enabled for the zendesk account, false otherwise
     */
    public boolean isJiralinksEnabled() {
        return jiralinksEnabled;
    }
}