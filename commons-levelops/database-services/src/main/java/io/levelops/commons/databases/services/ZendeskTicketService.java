package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.databases.converters.DbZendeskTicketConverters;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskField;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskTicket;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ZendeskTicketsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Log4j2
@Service
public class ZendeskTicketService extends DatabaseService<DbZendeskTicket> {

    private static final String ZENDESK_TICKETS = "zendesk_tickets";
    private static final String ZENDESK_TICKET_ASSIGNEES = "zendesk_ticket_assignees";
    private static final String ZENDESK_TICKET_EXTERNAL_IDS = "zendesk_ticket_external_ids";
    private static final String ZENDESK_TICKET_JIRA_KEYS = "zendesk_ticket_jira_keys";

    private static final int NUM_IDLE_DAYS = 30;

    private static final Set<String> SORTABLE_COLUMNS = Set.of("hops", "bounces", "created_at", "updated_at", "reopens",
            "replies", "assigned_at", "solved_at", "first_resolution_time", "first_reply_time", "full_resolution_time",
            "agent_wait_time", "requester_wait_time", "assignee_updated_at", "ticket_created_at", "ticket_updated_at");

    private final NamedParameterJdbcTemplate template;
    private final IntegrationService configService;
    private final ZendeskFieldService zendeskFieldService;

    @Autowired
    public ZendeskTicketService(DataSource dataSource, IntegrationService configService,
                                ZendeskFieldService zendeskFieldService) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        this.configService = configService;
        this.zendeskFieldService = zendeskFieldService;

    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbZendeskTicket ticket) throws SQLException {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            int integrationId = NumberUtils.toInt(ticket.getIntegrationId());
            String insertTicket = "INSERT INTO " + company + "." + ZENDESK_TICKETS + " (ticket_id, integration_id," +
                    " custom_fields," +
                    " ingested_at, brand, type, subject, priority, status, recipient, requester," +
                    " submitter, assignee, organization, hops, created_at, updated_at, ticket_created_at," +
                    " ticket_updated_at, reopens, replies, assigned_at, solved_at, due_at, first_resolution_time," +
                    " first_reply_time, full_resolution_time, agent_wait_time, requester_wait_time, assignee_updated_at)" +
                    " VALUES(?,?,to_json(?::json),?,?,?,?,?,?,?,?,?,?,?,?,'now','now',?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                    " ON CONFLICT (ticket_id, integration_id, ingested_at) DO UPDATE SET brand=EXCLUDED.brand," +
                    " custom_fields=EXCLUDED.custom_fields," +
                    " type=EXCLUDED.type, subject=EXCLUDED.subject," +
                    " priority=EXCLUDED.priority, status=EXCLUDED.status," +
                    " recipient=EXCLUDED.recipient, requester=EXCLUDED.requester, submitter=EXCLUDED.submitter," +
                    " assignee=EXCLUDED.assignee, organization=EXCLUDED.organization, hops=EXCLUDED.hops," +
                    " created_at=" + company + "." + ZENDESK_TICKETS + ".created_at, updated_at=EXCLUDED.updated_at," +
                    " ticket_created_at=EXCLUDED.ticket_created_at," +
                    " ticket_updated_at=EXCLUDED.ticket_updated_at, reopens=EXCLUDED.reopens, replies=EXCLUDED.replies," +
                    " assigned_at=EXCLUDED.assigned_at, solved_at=EXCLUDED.solved_at," +
                    " due_at=EXCLUDED.due_at, first_resolution_time=EXCLUDED.first_resolution_time," +
                    " first_reply_time=EXCLUDED.first_reply_time, full_resolution_time=EXCLUDED.full_resolution_time," +
                    " agent_wait_time=EXCLUDED.agent_wait_time, requester_wait_time=EXCLUDED.requester_wait_time," +
                    " assignee_updated_at=EXCLUDED.assignee_updated_at WHERE " + company + "." + ZENDESK_TICKETS +
                    ".ticket_updated_at < EXCLUDED.ticket_updated_at";
            String insertAssignee = "INSERT INTO " + company + "." + ZENDESK_TICKET_ASSIGNEES +
                    " (ticket_id, assignee, assigned_at) VALUES(?,?,?)" +
                    " ON CONFLICT (ticket_id, assignee) DO UPDATE SET " +
                    " assigned_at = EXCLUDED.assigned_at, reassigned = " + company + "." + ZENDESK_TICKET_ASSIGNEES +
                    ".reassigned + 1 WHERE " + company + "." + ZENDESK_TICKET_ASSIGNEES +
                    ".assigned_at < EXCLUDED.assigned_at";
            String insertExternalId = "INSERT INTO " + company + "." + ZENDESK_TICKET_EXTERNAL_IDS +
                    " (ticket_id, external_id) VALUES(?,?,?)" +
                    " ON CONFLICT (ticket_id, external_id) DO NOTHING";
            String updateBounces = "UPDATE " + company + "." + ZENDESK_TICKETS + " SET bounces = t.bounces," +
                    " updated_at='now' FROM (SELECT SUM(reassigned) AS bounces, ticket_id FROM " + company + "." +
                    ZENDESK_TICKET_ASSIGNEES + " GROUP BY ticket_id) AS t WHERE " + company + "." + ZENDESK_TICKETS +
                    ".id = t.ticket_id AND ingested_at = ? AND " + company + "." + ZENDESK_TICKETS +
                    ".bounces != t.bounces";
            String insertJiraKeys = "INSERT INTO " + company + "." + ZENDESK_TICKET_JIRA_KEYS +
                    " (ticket_id, issue_key) VALUES(?,?)" +
                    " ON CONFLICT (ticket_id, issue_key) DO NOTHING";
            String deleteJiraKeys = "DELETE FROM " + company + "." + ZENDESK_TICKET_JIRA_KEYS +
                    " USING " + company + "." + ZENDESK_TICKETS + " WHERE " + company + "." + ZENDESK_TICKET_JIRA_KEYS +
                    ".ticket_id = " + company + "." + ZENDESK_TICKETS + ".id AND " + company + "." + ZENDESK_TICKETS +
                    ".ticket_id = ?";
            String deleteExternalId = "DELETE FROM " + company + "." + ZENDESK_TICKET_EXTERNAL_IDS +
                    " USING " + company + "." + ZENDESK_TICKETS + " WHERE " + company + "." + ZENDESK_TICKET_EXTERNAL_IDS +
                    ".ticket_id = " + company + "." + ZENDESK_TICKETS + ".id AND " + company + "." + ZENDESK_TICKETS +
                    ".ticket_id = ?";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertTicket, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement assigneeStmt = conn.prepareStatement(insertAssignee, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement externalIdStmt = conn.prepareStatement(insertExternalId, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement bouncesStmt = conn.prepareStatement(updateBounces);
                 PreparedStatement jiraStmt = conn.prepareStatement(insertJiraKeys, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement deleteJiraStmt = conn.prepareStatement(deleteJiraKeys, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement deleteExternalIdStmt = conn.prepareStatement(deleteExternalId, Statement.RETURN_GENERATED_KEYS)) {
                final Timestamp ingestedAt = getTimestamp(ticket.getIngestedAt());
                int i = 1;
                insertStmt.setLong(i++, ticket.getTicketId());
                insertStmt.setInt(i++, integrationId);
                try {
                    insertStmt.setObject(i++, DefaultObjectMapper.get().writeValueAsString(ticket.getCustomFields()));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize custom field json. will store empty json.", e);
                    insertStmt.setObject(i - 1, "{}");
                }
                insertStmt.setTimestamp(i++, ingestedAt);
                insertStmt.setString(i++, ticket.getBrand());
                insertStmt.setString(i++, StringUtils.upperCase(ticket.getType()));
                insertStmt.setString(i++, ticket.getSubject());
                insertStmt.setString(i++, StringUtils.upperCase(ticket.getPriority()));
                insertStmt.setString(i++, StringUtils.upperCase(ticket.getStatus()));
                insertStmt.setString(i++, ticket.getRecipientEmail());
                insertStmt.setString(i++, ticket.getRequesterEmail());
                insertStmt.setString(i++, ticket.getSubmitterEmail());
                insertStmt.setString(i++, ticket.getAssigneeEmail());
                insertStmt.setString(i++, ticket.getOrganizationName());
                insertStmt.setObject(i++, ticket.getHops());
                insertStmt.setObject(i++, getTimestamp(ticket.getTicketCreatedAt()));
                insertStmt.setObject(i++, getTimestamp(ticket.getTicketUpdatedAt()));
                insertStmt.setObject(i++, ticket.getReopens());
                insertStmt.setObject(i++, ticket.getReplies());
                insertStmt.setObject(i++, getTimestamp(ticket.getAssignedAt()));
                insertStmt.setObject(i++, getTimestamp(ticket.getSolvedAt()));
                insertStmt.setObject(i++, getTimestamp(ticket.getDueAt()));
                insertStmt.setObject(i++, ticket.getFirstResolutionTime());
                insertStmt.setObject(i++, ticket.getFirstReplyTime());
                insertStmt.setObject(i++, ticket.getFullResolutionTime());
                insertStmt.setObject(i++, ticket.getAgentWaitTime());
                insertStmt.setObject(i++, ticket.getRequesterWaitTime());
                insertStmt.setObject(i, getTimestamp(ticket.getAssigneeUpdatedAt()));
                insertStmt.executeUpdate();
                String ticketId = getIdFromResultOrFetch(insertStmt, company, ticket.getTicketId(), integrationId, ingestedAt);
                final UUID uuid = UUID.fromString(ticketId);
                if (ticket.getAssigneeEmail() != null) {
                    assigneeStmt.setObject(1, uuid);
                    assigneeStmt.setString(2, ticket.getAssigneeEmail());
                    assigneeStmt.setTimestamp(3, getTimestamp(ticket.getAssignedAt()));
                    assigneeStmt.executeUpdate();
                }
                deleteExternalIdStmt.setObject(1, ticket.getTicketId());
                deleteExternalIdStmt.executeUpdate();
                if (ticket.getExternalId() != null) {
                    externalIdStmt.setObject(1, uuid);
                    externalIdStmt.setString(2, ticket.getExternalId());
                    externalIdStmt.executeUpdate();
                }
                bouncesStmt.setTimestamp(1, ingestedAt);
                bouncesStmt.executeUpdate();
                deleteJiraStmt.setObject(1, ticket.getTicketId());
                deleteJiraStmt.executeUpdate();
                if (CollectionUtils.isNotEmpty(ticket.getJiraIssueKeys())) {
                    for (String jiraIssueKey : ticket.getJiraIssueKeys()) {
                        jiraStmt.setObject(1, uuid);
                        jiraStmt.setString(2, jiraIssueKey);
                        jiraStmt.addBatch();
                    }
                    jiraStmt.executeBatch();
                }
                return ticketId;
            }
        }));
    }

    private String getIdFromResultOrFetch(PreparedStatement insertStmt, String company, Long ticketId,
                                          int integrationId, Timestamp ingestedAt) throws SQLException {
        String id;
        try (ResultSet rs = insertStmt.getGeneratedKeys()) {
            if (rs.next())
                id = rs.getString(1);
            else {
                final Optional<String> idOpt = getId(company, ticketId, integrationId, ingestedAt);
                if (idOpt.isPresent()) {
                    id = idOpt.get();
                } else {
                    throw new SQLException("Failed to get ticket row id");
                }
            }
        }
        return id;
    }

    /**
     * {@link #insert(String, DbZendeskTicket)} already implements update as well
     */
    @Override
    public Boolean update(String company, DbZendeskTicket ticket) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported this get because the filter requires: integration_id + ticket_id
     */
    @Override
    public Optional<DbZendeskTicket> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Optional<DbZendeskTicket> get(String company, long ticketId, int integrationId, Date ingestedAt) {
        String query = "SELECT * FROM (SELECT STRING_AGG(j.issue_key, ',') jira_issues, t.* FROM "
                + company + "." + ZENDESK_TICKETS + " t LEFT JOIN " + company + "." + ZENDESK_TICKET_JIRA_KEYS
                + " j ON t.id = j.ticket_id GROUP BY t.id) as t LEFT JOIN " + company + "." + ZENDESK_TICKET_EXTERNAL_IDS
                + " ids ON t.id=ids.ticket_id " + " where t.ticket_id = :ticket_id AND" +
                " t.integration_id = :integration_id AND t.ingested_at = :ingested_at";
        Map<String, Object> params = Map.of(
                "ticket_id", ticketId,
                "integration_id", integrationId,
                "ingested_at", ingestedAt);
        return Optional.ofNullable(template.query(query, params, DbZendeskTicketConverters.rowMapper()));
    }

    private Optional<String> getId(String company, long ticketId, int integrationId, Date ingestedAt) {
        String query = "SELECT id FROM " + company + "." + ZENDESK_TICKETS + " where ticket_id = :ticket_id AND" +
                " integration_id = :integration_id AND ingested_at = :ingested_at";
        final Map<String, Object> params = Map.of(
                "ticket_id", ticketId,
                "integration_id", integrationId,
                "ingested_at", ingestedAt);
        return Optional.ofNullable(template.query(query, params, DbZendeskTicketConverters.idMapper()));
    }

    @Override
    public DbListResponse<DbZendeskTicket> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return list(company, ZendeskTicketsFilter.builder().build(), Collections.emptyMap(), pageNumber, pageSize);
    }

    public DbListResponse<DbZendeskTicket> list(String company,
                                                ZendeskTicketsFilter filter,
                                                Map<String, SortingOrder> sortBy,
                                                Integer pageNumber,
                                                Integer pageSize) throws SQLException {
        Map<String, Object> params = new HashMap<>();
        final Long latestIngestedDate = filter.getIngestedAt();
        final List<String> conditions = createWhereClauseAndUpdateParams(company, params, latestIngestedDate, filter.getBrands(),
                filter.getTypes(), filter.getPriorities(), filter.getStatuses(), filter.getOrganizations(),
                filter.getRequesterEmails(), filter.getSubmitterEmails(), filter.getAssigneeEmails(),
                filter.getIntegrationIds(), filter.getAge(), filter.getExtraCriteria(), filter.getTicketCreatedStart(),
                filter.getTicketCreatedEnd(), filter.getCustomFields(), filter.getExcludeCustomFields());
        setPagingParams(pageNumber, pageSize, params);
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (SORTABLE_COLUMNS.contains(entry.getKey()))
                        return entry.getKey();
                    return "ticket_created_at";
                })
                .orElse("ticket_created_at");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        String whereClause = getWhereClause(conditions);
        String limitClause = " OFFSET :offset LIMIT :limit";
        String orderByClause = " ORDER BY " + sortByKey + " " + sortOrder.name();
        String selectStatement = "SELECT * FROM (SELECT STRING_AGG(j.issue_key, ',') jira_issues, t.* FROM "
                + company + "." + ZENDESK_TICKETS + " t LEFT JOIN " + company + "." + ZENDESK_TICKET_JIRA_KEYS
                + " j ON t.id = j.ticket_id GROUP BY t.id) as t LEFT JOIN " + company + "." + ZENDESK_TICKET_EXTERNAL_IDS
                + " ids ON t.id=ids.ticket_id";
        String query = selectStatement + whereClause + orderByClause + limitClause;
        final List<DbZendeskTicket> tickets = template.query(query, params, DbZendeskTicketConverters.listRowMapper());
        String countQuery = "SELECT count(*) from " + company + "." + ZENDESK_TICKETS + whereClause;
        final Integer count = template.queryForObject(countQuery, params, Integer.class);
        return DbListResponse.of(tickets, count);
    }

    private String getWhereClause(List<String> conditions) {
        String whereClause = EMPTY;
        if (!conditions.isEmpty()) {
            whereClause = " WHERE " + String.join(" AND ", conditions);
        }
        return whereClause;
    }

    private void setPagingParams(Integer pageNumber, Integer pageSize, Map<String, Object> params) {
        params.put("offset", pageNumber * pageSize);
        params.put("limit", pageSize);
    }

    public int cleanUpOldData(String company, Date fromTime, Long olderThanSeconds) {
        return template.update("DELETE FROM " + company + "." + ZENDESK_TICKETS + " WHERE ingested_at < :date",
                Map.of("date", Timestamp.from(fromTime.toInstant().minus(olderThanSeconds, ChronoUnit.SECONDS))));
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company, ZendeskTicketsFilter filter)
            throws SQLException {
        final ZendeskTicketsFilter.DISTINCT DISTINCT = filter.getDISTINCT();
        Validate.notNull(DISTINCT, "Across must be present for group by query");
        if (filter.getDISTINCT() == (ZendeskTicketsFilter.DISTINCT.custom_field)
                && (StringUtils.isEmpty(filter.getCustomAcross()) ||
                !DbZendeskField.CUSTOM_FIELD_KEY_PATTERN.matcher(filter.getCustomAcross()).matches()))
            throw new SQLException("Invalid custom field name provided. will not execute query.");
        final ZendeskTicketsFilter.CALCULATION CALCULATION = MoreObjects.firstNonNull(filter.getCALCULATION(),
                ZendeskTicketsFilter.CALCULATION.ticket_count);
        Map<String, Object> params = new HashMap<>();
        String selectDistinctString = null;
        Optional<String> additionalKey = Optional.empty();
        String aggSql;
        String orderBySql;
        switch (CALCULATION) {
            case hops:
            case bounces:
            case reopens:
            case replies:
                String aggName = CALCULATION.toString();
                aggSql = " MIN(" + aggName + ") as mn, MAX(" + aggName + ") as mx, COUNT(id) as ct, " +
                        "PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY " + aggName + ")";
                orderBySql = " mx DESC ";
                break;
            case response_time:
                aggSql = " MIN(first_reply_time) as mn, MAX(first_reply_time) as mx, COUNT(id) as ct, PERCENTILE_DISC(0.5) " +
                        "WITHIN GROUP(ORDER BY first_reply_time)";
                orderBySql = " mx DESC ";
                break;
            case resolution_time:
                aggSql = " MIN(full_resolution_time) as mn, MAX(full_resolution_time) as mx, COUNT(id) as ct," +
                        " PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY full_resolution_time)";
                orderBySql = " mx DESC ";
                break;
            case agent_wait_time:
                aggSql = " MIN(agent_wait_time) as mn, MAX(agent_wait_time) as mx, COUNT(id) as ct," +
                        " PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY agent_wait_time)";
                orderBySql = " mx DESC ";
                break;
            case requester_wait_time:
                aggSql = " MIN(requester_wait_time) as mn, MAX(requester_wait_time) as mx, COUNT(id) as ct," +
                        " PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY requester_wait_time)";
                orderBySql = " mx DESC ";
                break;
            default:
                aggSql = "COUNT(id) as ct";
                orderBySql = " ct DESC ";
                break;
        }
        String groupBySql;
        String key = DISTINCT.name();
        String intervalColumn = "";
        switch (DISTINCT) {
            case brand:
            case type:
            case priority:
            case status:
            case organization:
            case assignee:
            case requester:
            case submitter:
                groupBySql = " GROUP BY " + DISTINCT;
                break;
            case custom_field:
                if (isArray(company, filter.getCustomAcross(), filter.getIntegrationIds())) {
                    selectDistinctString = "jsonb_array_elements_text(custom_fields->'" + filter.getCustomAcross() + "')" +
                            " AS custom_field";
                } else {
                    selectDistinctString = "custom_fields->>'" + filter.getCustomAcross() + "' AS custom_field";
                }
                groupBySql = "GROUP BY custom_field";
                key = "";
                break;
            case trend:
                AggTimeQueryHelper.AggTimeQuery trendAggQuery = AggTimeQueryHelper.getAggTimeQuery("ingested_at",
                                DISTINCT.toString(), filter.getAggInterval().toString(), false);
                selectDistinctString = trendAggQuery.getSelect();
                intervalColumn = trendAggQuery.getHelperColumn();
                additionalKey = Optional.of(trendAggQuery.getIntervalKey());
                groupBySql = "GROUP BY " + trendAggQuery.getGroupBy();
                orderBySql = trendAggQuery.getOrderBy();
                key = "";
                break;
            case ticket_created:
            case ticket_updated:
            case assigned:
            case solved:
            case due:
            case assignee_updated:
                AggTimeQueryHelper.AggTimeQuery ticketModAggQuery = AggTimeQueryHelper.getAggTimeQuery(DISTINCT + "_at",
                                DISTINCT.toString(), filter.getAggInterval().toString(), false);
                selectDistinctString = ticketModAggQuery.getSelect();
                intervalColumn = ticketModAggQuery.getHelperColumn();
                additionalKey = Optional.of(ticketModAggQuery.getIntervalKey());
                groupBySql = "GROUP BY " + ticketModAggQuery.getGroupBy();
                orderBySql = ticketModAggQuery.getOrderBy();
                key = "";
                break;
            default:
                throw new SQLException("Unsupported across: " + DISTINCT);
        }
        Long ingestedDate = DISTINCT != ZendeskTicketsFilter.DISTINCT.trend ? filter.getIngestedAt() : null;
        final List<String> conditions = createWhereClauseAndUpdateParams(company, params, ingestedDate, filter.getBrands(),
                filter.getTypes(), filter.getPriorities(), filter.getStatuses(), filter.getOrganizations(),
                filter.getRequesterEmails(), filter.getSubmitterEmails(), filter.getAssigneeEmails(),
                filter.getIntegrationIds(), filter.getAge(), filter.getExtraCriteria(), filter.getTicketCreatedStart(),
                filter.getTicketCreatedEnd(), filter.getCustomFields(), filter.getExcludeCustomFields());
        if (DISTINCT == ZendeskTicketsFilter.DISTINCT.custom_field) {
            conditions.add(" custom_fields ??'" + filter.getCustomAcross() + "' ");
        }
        final String whereClause = getWhereClause(conditions);
        String fromTable = company + "." + ZENDESK_TICKETS;
        String query = "SELECT " + selectDistinctString + ", " + aggSql + (StringUtils.isNotEmpty(key) ? ", " : "") + key
                + " FROM( SELECT *" + intervalColumn + " FROM " + fromTable + whereClause + " ) AS finalTable "
                + groupBySql + " ORDER BY " + orderBySql;
        final List<DbAggregationResult> aggregationResults = template.query(query, params,
                DbZendeskTicketConverters.aggRowMapper(DISTINCT.toString(), CALCULATION, additionalKey));
        return DbListResponse.of(aggregationResults, aggregationResults.size());
    }

    public Date getMaxIngestedDate(String company) {
        String query = "SELECT MAX(ingested_at) AS latest_ingested_date FROM " + company + "." + ZENDESK_TICKETS;
        return template.query(query, rs -> {
            if (!rs.next())
                return null;
            return rs.getDate("latest_ingested_date");
        });
    }

    protected List<String> createWhereClauseAndUpdateParams(String company,
                                                            Map<String, Object> params,
                                                            Long latestIngestedDate,
                                                            List<String> brands,
                                                            List<String> types,
                                                            List<String> priorities,
                                                            List<String> statuses,
                                                            List<String> organizations,
                                                            List<String> requesterEmails,
                                                            List<String> submitterEmails,
                                                            List<String> assigneeEmails,
                                                            List<String> integrationIds,
                                                            Map<String, Object> age,
                                                            List<ZendeskTicketsFilter.EXTRA_CRITERIA> extraCriteria,
                                                            Long ticketCreatedStart,
                                                            Long ticketCreatedEnd,
                                                            Map<String, List<String>> customFields,
                                                            Map<String, List<String>> excludeCustomFields

    ) {
        List<String> ticketConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(brands)) {
            ticketConditions.add("brand IN (:zd_brands)");
            params.put("zd_brands", brands);
        }
        if (MapUtils.isNotEmpty(customFields)) {
            createCustomFieldConditions(company, params, integrationIds,
                    customFields, ticketConditions, true);
        }
        if (MapUtils.isNotEmpty(excludeCustomFields)) {
            createCustomFieldConditions(company, params, integrationIds,
                    excludeCustomFields, ticketConditions, false);
        }
        if (CollectionUtils.isNotEmpty(types)) {
            ticketConditions.add("type IN (:zd_types)");
            params.put("zd_types", types);
        }
        if (CollectionUtils.isNotEmpty(priorities)) {
            ticketConditions.add("priority IN (:zd_priorities)");
            params.put("zd_priorities", priorities);
        }
        if (CollectionUtils.isNotEmpty(statuses)) {
            ticketConditions.add("status IN (:zd_statuses)");
            params.put("zd_statuses", statuses);
        }
        if (CollectionUtils.isNotEmpty(organizations)) {
            ticketConditions.add("organization IN (:zd_organizations)");
            params.put("zd_organizations", organizations);
        }
        if (CollectionUtils.isNotEmpty(requesterEmails)) {
            ticketConditions.add("requester IN (:zd_requesters)");
            params.put("zd_requesters", requesterEmails);
        }
        if (CollectionUtils.isNotEmpty(submitterEmails)) {
            ticketConditions.add("submitter IN (:zd_submitters)");
            params.put("zd_submitters", submitterEmails);
        }
        if (CollectionUtils.isNotEmpty(assigneeEmails)) {
            ticketConditions.add("assignee IN (:zd_assignees)");
            params.put("zd_assignees", assigneeEmails);
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            final List<Integer> ids = integrationIds.stream()
                    .map(NumberUtils::toInt)
                    .collect(Collectors.toList());
            ticketConditions.add("integration_id IN (:zd_integration_ids)");
            params.put("zd_integration_ids", ids);
        }
        if (latestIngestedDate != null) {
            ticketConditions.add("ingested_at = to_timestamp(:zd_ingested_at)");
            params.put("zd_ingested_at", latestIngestedDate);
        }
        if (ticketCreatedStart != null) {
            ticketConditions.add("ticket_created_at > to_timestamp(:zd_created_start)");
            params.put("zd_created_start", ticketCreatedStart);
        }
        if (ticketCreatedEnd != null) {
            ticketConditions.add("ticket_created_at < to_timestamp(:zd_created_end)");
            params.put("zd_created_end", ticketCreatedEnd);
        }

        if (MapUtils.isNotEmpty(age)) {
            Integer ticketAgeMin = age.get("$gt") != null ? Integer.valueOf(age.get("$gt").toString()) : null;
            Integer ticketAgeMax = age.get("$lt") != null ? Integer.valueOf(age.get("$lt").toString()) : null;

            if (ticketAgeMin != null && ticketAgeMin != 0) {
                ticketConditions.add("ticket_created_at <= TIMESTAMP 'now' - interval '" + ticketAgeMin + " days'");
            }

            if (ticketAgeMax != null && ticketAgeMax != 0) {
                ticketConditions.add("ticket_created_at >= TIMESTAMP 'now' - interval '" + ticketAgeMax + " days'");
            }
        }

        if (CollectionUtils.isNotEmpty(extraCriteria)) {
            List<String> extraConditions = new ArrayList<>();
            for (ZendeskTicketsFilter.EXTRA_CRITERIA extraCriterion : extraCriteria) {
                switch (extraCriterion) {
                    case idle:
                        extraConditions.add("ticket_updated_at < TIMESTAMP 'now' - interval '"
                                + NUM_IDLE_DAYS + " days'");
                        break;
                    case no_due_date:
                        extraConditions.add("due_at IS NULL");
                        break;
                    case no_brand:
                        extraConditions.add("brand = '_UNASSIGNED_'");
                        break;
                    case no_assignee:
                        extraConditions.add("assignee = '_UNASSIGNED_'");
                        break;
                    case missed_resolution_time:
                        extraConditions.add("due_at < COALESCE(solved_at, 'now')");
                        break;
                }
            }
            ticketConditions.addAll(extraConditions);
        }
        return ticketConditions;
    }

    /**
     * Delete not supported as cascade delete does the work
     */
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddlStmts = List.of("CREATE TABLE IF NOT EXISTS " + company + "." + ZENDESK_TICKETS +
                        " (id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " ticket_id BIGINT NOT NULL," +
                        " brand VARCHAR NOT NULL," +
                        " integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE," +
                        " custom_fields JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        " type VARCHAR NOT NULL," +
                        " subject VARCHAR," +
                        " priority VARCHAR NOT NULL," +
                        " status VARCHAR NOT NULL," +
                        " recipient VARCHAR," +
                        " requester VARCHAR NOT NULL, " +
                        " submitter VARCHAR NOT NULL," +
                        " assignee VARCHAR NOT NULL," +
                        " organization VARCHAR NOT NULL," +
                        " hops INTEGER NOT NULL DEFAULT 0," +
                        " bounces INTEGER NOT NULL DEFAULT 0," +
                        " created_at TIMESTAMP WITH TIME ZONE NOT NULL," +
                        " updated_at TIMESTAMP WITH TIME ZONE," +
                        " ingested_at DATE NOT NULL," +
                        " ticket_created_at TIMESTAMP WITH TIME ZONE NOT NULL," +
                        " ticket_updated_at TIMESTAMP WITH TIME ZONE," +
                        " reopens INTEGER NOT NULL DEFAULT 0, " +
                        " replies INTEGER NOT NULL DEFAULT 0," +
                        " assigned_at TIMESTAMP WITH TIME ZONE," +
                        " solved_at TIMESTAMP WITH TIME ZONE," +
                        " due_at TIMESTAMP WITH TIME ZONE," +
                        " first_resolution_time INTEGER," +
                        " first_reply_time INTEGER," +
                        " full_resolution_time INTEGER," +
                        " agent_wait_time INTEGER," +
                        " requester_wait_time INTEGER," +
                        " assignee_updated_at TIMESTAMP WITH TIME ZONE," +
                        " UNIQUE(integration_id, ticket_id, ingested_at))",

                "CREATE INDEX IF NOT EXISTS " + ZENDESK_TICKETS + "_ticket_id_integration_id_compound_idx " + "on " +
                        company + "." + ZENDESK_TICKETS + " (ticket_id, integration_id)",

                "CREATE INDEX IF NOT EXISTS " + ZENDESK_TICKETS + "_ingested_at_idx " +
                        "on " + company + "." + ZENDESK_TICKETS + "(ingested_at)",

                "CREATE INDEX IF NOT EXISTS " + ZENDESK_TICKETS + "_custom_fields_idx " +
                        "on " + company + "." + ZENDESK_TICKETS + " USING GIN(custom_fields)",

                "CREATE INDEX IF NOT EXISTS " + ZENDESK_TICKETS + "_ticket_created_at_idx " +
                        "on " + company + "." + ZENDESK_TICKETS + "(ticket_created_at)",

                "CREATE INDEX IF NOT EXISTS " + ZENDESK_TICKETS + "_ticket_updated_at_idx " +
                        "on " + company + "." + ZENDESK_TICKETS + "(ticket_updated_at)",

                "CREATE TABLE IF NOT EXISTS " + company + "." + ZENDESK_TICKET_ASSIGNEES +
                        " (id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " ticket_id UUID NOT NULL REFERENCES " +
                        company + "." + ZENDESK_TICKETS + "(id) ON DELETE CASCADE," +
                        " assignee VARCHAR NOT NULL," +
                        " assigned_at TIMESTAMP WITH TIME ZONE," +
                        " reassigned INTEGER NOT NULL DEFAULT 0," +
                        " UNIQUE(ticket_id, assignee))",

                "CREATE INDEX IF NOT EXISTS " + ZENDESK_TICKET_ASSIGNEES + "_compound_idx on "
                        + company + "." + ZENDESK_TICKET_ASSIGNEES + " (ticket_id, assignee)",

                "CREATE TABLE IF NOT EXISTS " + company + "." + ZENDESK_TICKET_EXTERNAL_IDS +
                        " (id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " ticket_id UUID NOT NULL REFERENCES " +
                        company + "." + ZENDESK_TICKETS + "(id) ON DELETE CASCADE," +
                        " external_id VARCHAR NOT NULL," +
                        " UNIQUE(ticket_id, external_id))",

                "CREATE INDEX IF NOT EXISTS " + ZENDESK_TICKET_EXTERNAL_IDS + "_compound_idx on "
                        + company + "." + ZENDESK_TICKET_EXTERNAL_IDS + " (ticket_id, external_id)",

                "CREATE TABLE IF NOT EXISTS " + company + "." + ZENDESK_TICKET_JIRA_KEYS +
                        " (id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " ticket_id UUID NOT NULL REFERENCES " +
                        company + "." + ZENDESK_TICKETS + "(id) ON DELETE CASCADE," +
                        " issue_key VARCHAR NOT NULL," +
                        " UNIQUE(ticket_id, issue_key))"
        );
        ddlStmts.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

    private void createCustomFieldConditions(String company,
                                             Map<String, Object> params,
                                             List<String> integrationIds,
                                             Map<String, List<String>> customFields,
                                             List<String> issueTblConditions,
                                             boolean include) {
        if (MapUtils.isEmpty(customFields))
            return;
        int fieldNumber = 0;
        for (String key : customFields.keySet()) {
            List<String> values = customFields.get(key);
            if (StringUtils.isEmpty(key)
                    || !DbZendeskField.CUSTOM_FIELD_KEY_PATTERN.matcher(key).matches()
                    || CollectionUtils.isEmpty(values))
                continue;
            String fieldRef = (include ? "customfield" : "not_customfield") + fieldNumber;
            String condition = (include ? "" : "NOT ") + " custom_fields @> :";
            int valNum = 0;
            StringBuilder conditionBuilder = new StringBuilder("(");
            boolean isArray;
            try {
                isArray = isArray(company, key, integrationIds);
            } catch (SQLException e) {
                isArray = false;
            }
            if (isArray) {
                fieldRef = fieldRef + "val" + (valNum);
                conditionBuilder.append(condition).append(fieldRef).append("::jsonb");
                params.put(fieldRef, "{\"" + key + "\":[" + values.stream().map(val -> "\"" +
                        StringEscapeUtils.escapeJson(val) + "\"")
                        .collect(Collectors.joining(",")) + "]}");
            } else {
                for (String value : values) {
                    fieldRef = fieldRef + "val" + (valNum++);
                    if (valNum > 1)
                        conditionBuilder.append(" OR ");
                    conditionBuilder.append(condition).append(fieldRef).append("::jsonb");
                    params.put(fieldRef, "{\"" + key + "\":\"" + StringEscapeUtils.escapeJson(value) + "\"}");
                }
            }
            issueTblConditions.add(conditionBuilder.append(")").toString());
            fieldNumber += 1;
        }
    }

    private boolean isArray(String company, String customField, List<String> integrationIds) throws SQLException {
        final DbListResponse<DbZendeskField> response = zendeskFieldService.listByFilter(company,
                integrationIds, null, null,
                List.of(customField), null, 0, 1);
        final Optional<DbZendeskField> dbZendeskFieldOpt = response.getCount() > 0 ?
                response.getRecords().stream().findFirst() : Optional.empty();
        if (dbZendeskFieldOpt.isEmpty())
            return false;
        if ("multiselect".equalsIgnoreCase(dbZendeskFieldOpt.get().getFieldType()))
            return true;
        Set<String> customArrayFields = configService
                .listConfigs(company, integrationIds, 0, 10000)
                .getRecords()
                .stream()
                .map(conf -> conf.getConfig().get("agg_custom_fields"))
                .flatMap(Collection::stream)
                .filter(configEntry -> StringUtils.isNotEmpty(configEntry.getDelimiter()))
                .map(IntegrationConfig.ConfigEntry::getKey)
                .collect(Collectors.toSet());
        return customArrayFields.contains(customField);
    }


    public DbListResponse<DbAggregationResult> stackedGroupBy(String company,
                                                              ZendeskTicketsFilter filter,
                                                              List<ZendeskTicketsFilter.DISTINCT> stacks)
            throws SQLException {
        DbListResponse<DbAggregationResult> result = groupByAndCalculate(company, filter);
        final Set<ZendeskTicketsFilter.DISTINCT> stackSupported = Set.of(
                ZendeskTicketsFilter.DISTINCT.brand,
                ZendeskTicketsFilter.DISTINCT.type,
                ZendeskTicketsFilter.DISTINCT.priority,
                ZendeskTicketsFilter.DISTINCT.custom_field,
                ZendeskTicketsFilter.DISTINCT.status,
                ZendeskTicketsFilter.DISTINCT.organization,
                ZendeskTicketsFilter.DISTINCT.assignee,
                ZendeskTicketsFilter.DISTINCT.requester,
                ZendeskTicketsFilter.DISTINCT.submitter,
                ZendeskTicketsFilter.DISTINCT.trend,
                ZendeskTicketsFilter.DISTINCT.ticket_created
        );
        if (stacks == null
                || stacks.size() == 0
                || !stackSupported.contains(stacks.get(0))
                || !stackSupported.contains(filter.getDISTINCT()))
            return result;
        ZendeskTicketsFilter.DISTINCT stack = stacks.get(0);
        String customStack = null;
        if (stack == ZendeskTicketsFilter.DISTINCT.custom_field) {
            customStack = ListUtils.emptyIfNull(filter.getCustomStacks()).stream().findFirst()
                    .orElseThrow(() -> new SQLException("custom_stacks field must be present with custom_field as stack"));
        }
        List<DbAggregationResult> finalList = new ArrayList<>();
        for (DbAggregationResult row : result.getRecords()) {
            ZendeskTicketsFilter newFilter;
            final ZendeskTicketsFilter.ZendeskTicketsFilterBuilder newFilterBuilder = filter.toBuilder();
            if (customStack != null) {
                newFilterBuilder.customAcross(customStack);
            }
            switch (filter.getDISTINCT()) {
                case brand:
                    newFilter = newFilterBuilder.brands(List.of(row.getKey())).DISTINCT(stack).build();
                    break;
                case type:
                    newFilter = newFilterBuilder.types(List.of(row.getKey())).DISTINCT(stack).build();
                    break;
                case priority:
                    newFilter = newFilterBuilder.priorities(List.of(row.getKey())).DISTINCT(stack).build();
                    break;
                case status:
                    newFilter = newFilterBuilder.statuses(List.of(row.getKey())).DISTINCT(stack).build();
                    break;
                case organization:
                    newFilter = newFilterBuilder.organizations(List.of(row.getKey())).DISTINCT(stack).build();
                    break;
                case assignee:
                    newFilter = newFilterBuilder.assigneeEmails(List.of(row.getKey())).DISTINCT(stack).build();
                    break;
                case requester:
                    newFilter = newFilterBuilder.requesterEmails(List.of(row.getKey())).DISTINCT(stack).build();
                    break;
                case submitter:
                    newFilter = newFilterBuilder.submitterEmails(List.of(row.getKey())).DISTINCT(stack).build();
                    break;
                case trend:
                case ticket_created:
                    newFilter = stacksFilterForTrend(stack, row,filter.getAggInterval(),filter.toBuilder());
                    break;
                case custom_field:
                    newFilter = newFilterBuilder.customFields(Map.of(filter.getCustomAcross(), List.of(row.getKey())))
                            .DISTINCT(stack)
                            .build();
                    break;
                default:
                    throw new SQLException("This stack is not available for zendesk issues." + stack);
            }

            finalList.add(row.toBuilder().stacks(groupByAndCalculate(company, newFilter).getRecords()).build());
        }
        return DbListResponse.of(finalList, finalList.size());
    }

    private ZendeskTicketsFilter stacksFilterForTrend(ZendeskTicketsFilter.DISTINCT stack, DbAggregationResult row,
                                                      AGG_INTERVAL aggInterval,
                                                      ZendeskTicketsFilter.ZendeskTicketsFilterBuilder zendeskTicketsFilterBuilder) {
        Calendar cal = Calendar.getInstance();
        long startTimeInSeconds = Long.parseLong(row.getKey());
        cal.setTimeInMillis(TimeUnit.SECONDS.toMillis(startTimeInSeconds));
        if(aggInterval.equals(AGG_INTERVAL.month))
            cal.add(Calendar.MONTH, 1);
        else if (aggInterval.equals(AGG_INTERVAL.day))
            cal.add(Calendar.DATE, 1);
        else if (aggInterval.equals(AGG_INTERVAL.year))
            cal.add(Calendar.YEAR, 1);
        else if (aggInterval.equals(AGG_INTERVAL.quarter))
            cal.add(Calendar.MONTH, 3);
        else
            cal.add(Calendar.DATE, 7);
        long endTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(cal.getTimeInMillis());
        return zendeskTicketsFilterBuilder
                .ticketCreatedStart(startTimeInSeconds)
                .ticketCreatedEnd(endTimeInSeconds)
                .DISTINCT(stack).build();
    }


    private Timestamp getTimestamp(Date date) {
        return date != null ? Timestamp.from(date.toInstant()) : null;
    }

}
