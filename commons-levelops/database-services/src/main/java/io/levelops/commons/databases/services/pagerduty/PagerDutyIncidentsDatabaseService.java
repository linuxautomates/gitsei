package io.levelops.commons.databases.services.pagerduty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.DBPagerdutyConverters;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.pagerduty.DbPDIncident;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyIncident;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyStatus;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyUser;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.PagerDutyFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.parsers.PagerDutyFilterParserCommons;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.databases.utils.TeamUtils;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.ingestion.models.IntegrationType;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.databases.models.filters.CiCdUtils.parseSortBy;
import static io.levelops.commons.databases.services.pagerduty.PagerDutyUtils.checkForInvalidFilters;
import static io.levelops.commons.databases.services.pagerduty.PagerDutyUtils.getListSqlStmt;
import static io.levelops.commons.databases.services.pagerduty.PagerDutyUtils.hasAlertFilters;
import static io.levelops.commons.databases.services.pagerduty.PagerDutyUtils.hasIncidentFilters;
import static io.levelops.commons.databases.services.pagerduty.PagerDutyUtils.hasOfficeFilters;
import static io.levelops.commons.databases.services.pagerduty.PagerDutyUtils.sqlForPagerDutyAlerts;
import static io.levelops.commons.databases.services.pagerduty.PagerDutyUtils.sqlForPagerDutyIncidents;
import static io.levelops.commons.databases.utils.AggTimeQueryHelper.getTimeRangeForStacks;

@Log4j2
@Service
public class PagerDutyIncidentsDatabaseService extends DatabaseService<DbPagerDutyIncident> {

    public static final String INCIDENTS_TABLE = "pd_incidents";
    public static final String USERS_TABLE_NAME = "pd_users";
    public static final String STATUS_TABLE_NAME = "pd_statuses";
    public static final List<String> PAGERDUTY_APPLICATIONS = List.of(IntegrationType.PAGERDUTY.name().toLowerCase());
    private static final String ALERTS_TABLE = "pd_alerts";
    private static final String INCIDENT = "incident";
    public static final String ALERT = "alert";
    private static final List<String> ddl = List.of(
            "CREATE TABLE IF NOT EXISTS {0}.{1} (\n"
                    + "   id               uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),\n"
                    + "   pd_service_id    uuid REFERENCES {0}.pd_services(id) ON DELETE SET NULL ON UPDATE CASCADE,\n"
                    + "   pd_id            varchar(50) NOT NULL,\n"
                    + "   summary          text,\n"
                    + "   urgency          varchar(100) NOT NULL,\n"
                    + "   priority         varchar(100) NOT NULL,\n"
                    + "   details          jsonb NOT NULL DEFAULT '''{}''',\n"
                    + "   status           varchar(20) NOT NULL,\n"
                    + "   created_at       bigint NOT NULL DEFAULT extract(epoch from now()),\n"
                    + "   updated_at       bigint NOT NULL DEFAULT extract(epoch from now()),\n"
                    + "   last_status_at   bigint NOT NULL DEFAULT extract(epoch from now()),\n"
                    + "   CONSTRAINT {1}_unique_id UNIQUE(pd_service_id, pd_id)\n"
                    + ");",

            "CREATE INDEX IF NOT EXISTS {1}_updated_at_idx ON {0}.{1}(updated_at)",

            "CREATE INDEX IF NOT EXISTS {1}_created_at_idx ON {0}.{1}(created_at)",

            "CREATE INDEX IF NOT EXISTS {1}_last_status_at_idx ON {0}.{1}(last_status_at)",

            "CREATE INDEX IF NOT EXISTS {1}_pd_service_id_idx ON {0}.{1}(pd_service_id)",

            "CREATE INDEX IF NOT EXISTS {1}_urgency_idx ON {0}.{1}(lower(urgency))",

            "CREATE INDEX IF NOT EXISTS {1}_priority_idx ON {0}.{1}(lower(priority))",

            "CREATE INDEX IF NOT EXISTS {1}_status_idx ON {0}.{1}(lower(status))",

            "CREATE TABLE IF NOT EXISTS {0}.{2} (\n"
                    + "   id               uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),\n"
                    + "   pd_id            varchar(50) NOT NULL,\n"
                    + "   name             varchar(200) NOT NULL,\n"
                    + "   email            varchar(200),\n"
                    + "   time_zone        varchar(200) NOT NULL DEFAULT ''America/Los_Angeles''\n"
                    + ");",

            "CREATE UNIQUE INDEX IF NOT EXISTS {2}_pd_id_idx ON {0}.{2}(lower(pd_id))",

            "CREATE TABLE IF NOT EXISTS {0}.{3} (\n"
                    + "   id               uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),\n"
                    + "   pd_incident_id   uuid REFERENCES {0}.{1}(id) ON DELETE SET NULL ON UPDATE CASCADE,\n"
                    + "   pd_user_id       uuid REFERENCES {0}.{2}(id) ON DELETE SET NULL ON UPDATE CASCADE,\n"
                    + "   status           varchar(100),\n"
                    + "   timestamp        bigint NOT NULL\n"
                    + ");",

            "CREATE UNIQUE INDEX IF NOT EXISTS {3}_unique_id_idx ON {0}.{3}(pd_incident_id, pd_user_id, lower(status))",

            "CREATE INDEX IF NOT EXISTS {3}_status_idx ON {0}.{3}(lower(status))",

            "CREATE INDEX IF NOT EXISTS {3}_timestamp_idx ON {0}.{3}(timestamp)");
    private static final String INSERT_SQL_FORMAT = "INSERT INTO {0}.{1}(pd_service_id, pd_id, summary, urgency, priority, details, status, created_at, updated_at, last_status_at) "
            + "VALUES(:pdServiceId, :pdId, :summary, :urgency, :priority, :details::jsonb, :status, :createdAt, :updatedAt, :lastStatusAt) "
            + "ON CONFLICT(pd_service_id, pd_id) DO UPDATE SET (summary,urgency,priority,details,status,updated_at,last_status_at) = "
            + "(EXCLUDED.summary, EXCLUDED.urgency, EXCLUDED.priority, EXCLUDED.details::jsonb, EXCLUDED.status, EXCLUDED.updated_at, EXCLUDED.last_status_at)";
    private static final String INSERT_USER_SQL_FORMAT = "INSERT INTO {0}.{1}(pd_id, name, email, time_zone) "
            + "VALUES(:pdId, :name, :email, :timeZone) "
            + "ON CONFLICT(lower(pd_id)) DO UPDATE SET (name, email, time_zone) = (EXCLUDED.name, EXCLUDED.email, EXCLUDED.time_zone)";
    private static final String INSERT_STATUS_SQL_FORMAT = "INSERT INTO {0}.{1}(pd_incident_id, pd_user_id, status, timestamp) "
            + "VALUES(:pdIncidentId, :pdUserId, :status, :timestamp) "
            + "ON CONFLICT(pd_incident_id, pd_user_id, lower(status)) DO NOTHING";
    private static final Set<String> allowedFilters = Set.of("id", "pd_service_id", "pd_id", "summary", "urgency", "priority", "status",
            "created_at", "updated_at", "last_status_at", "from_created", "to_created", "user_ids");
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DEFAULT_STACK_PARALLELISM = 2;
    private static final Set<String> PD_SORTABLE_COLUMNS = Set.of("alert_created_at", "alert_resolved_at", "incident_created_at", "incident_resolved_at",
            "user_id", "pd_service", "incident_status", "alert_severity", "incident_priority", "resolution_time", "response_time");
    private final String queryForUserId = "SELECT id FROM {0}.{1} WHERE pd_id = :pdId AND name = :name";
    private final String queryForStatusId = "SELECT id FROM {0}.{1} WHERE (pd_incident_id, pd_user_id, status) = (:pdIncidentId, :pdUserId, :status)";
    private final String queryForIncidentId = "SELECT id FROM {0}.{1} WHERE (pd_service_id, pd_id) = (:pdServiceId, :pdId)";

    private final String ackTrendBaseQuery =
        "SELECT row_to_json(t) FROM(\n" +
        "    SELECT u.id, u.name, u.email, array_to_json(ARRAY(SELECT row_to_json(l) FROM(\n" +
        "        SELECT \n" +
        "        extract(epoch from date_trunc(''day'', to_timestamp(c.created_at))::date)::bigint as key, avg(s.timestamp - c.created_at)::numeric(10) as value\n" +
        "        FROM {0}.pd_statuses s, {0}.pd_incidents c \n" +
        "        WHERE u.id = s.pd_user_id AND c.id = s.pd_incident_id {1}\n" +
        "        GROUP BY pd_user_id, key\n" +
        "    ) AS l)) AS aggregations\n" +
        "    FROM {0}.pd_users u {2}\n" +
        ") AS t";

    private final String afterHoursBaseQuery =
        "SELECT u.id as user_id, u.name, u.email, json_agg(s.timestamp) as timestamps, u.time_zone\n" +
        "FROM \n" +
        "    {0}.pd_statuses s,\n" +
        "    {0}.pd_users u\n" +
        "    {2}\n" +
        "WHERE \n" +
        "    s.pd_user_id = u.id\n" +
        "    {3} {4}\n" +
        "    AND ({1}) \n" +
        "GROUP BY u.id ";
    private final String afterHoursTime =
        "(s.timestamp > extract(epoch from to_timestamp(''{0} {3}:00'', ''YYYY-MM-DD HH24:MI:SS'') at time zone ''UTC'' at time zone u.time_zone) " +
        "AND s.timestamp < extract(epoch from to_timestamp(''{1} {2}:00'', ''YYYY-MM-DD HH24:MI:SS'') at time zone ''UTC'' at time zone u.time_zone))";

    private final Set<PagerDutyFilter.DISTINCT> stackSupported = Set.of(
            PagerDutyFilter.DISTINCT.user_id,
            PagerDutyFilter.DISTINCT.incident_priority,
            PagerDutyFilter.DISTINCT.pd_service,
            PagerDutyFilter.DISTINCT.incident_created_at,
            PagerDutyFilter.DISTINCT.incident_resolved_at,
            PagerDutyFilter.DISTINCT.alert_resolved_at,
            PagerDutyFilter.DISTINCT.alert_created_at,
            PagerDutyFilter.DISTINCT.status,
            PagerDutyFilter.DISTINCT.alert_severity);
    private final ObjectMapper mapper;
    private final NamedParameterJdbcTemplate template;
    private PagerDutyFilterParserCommons filterParserCommons;
    private ProductsDatabaseService productsDatabaseService;
    private UserIdentityService userIdentityService;
    private PagerDutyServicesDatabaseService pagerDutyServicesDatabaseService;

    @Autowired
    public PagerDutyIncidentsDatabaseService(final ObjectMapper mapper, final DataSource dataSource) {
        super(dataSource);
        this.mapper = mapper;
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.productsDatabaseService = new ProductsDatabaseService(dataSource, mapper);
        this.filterParserCommons = new PagerDutyFilterParserCommons(productsDatabaseService);
        this.userIdentityService = new UserIdentityService(dataSource);
        this.pagerDutyServicesDatabaseService = new PagerDutyServicesDatabaseService(mapper, dataSource);
    }

    public static String getTimeZoneOffsetHours(String timezone) {
        var div = 360000;
        var tmp = TimeZone.getTimeZone(timezone).getRawOffset() / div;
        var simbol = tmp >= 0 ? "+" : "-";
        var diff = Integer.valueOf(tmp).toString();
        diff = diff.startsWith("-") ? diff.substring(1) : diff;
        diff = "0".equals(diff) ? "+00:00"
                : simbol
                + (diff.length() == 2 ? "0" : "")
                + diff.substring(0, diff.length() - 1)
                + ('5' == diff.charAt(diff.length() - 1) ? ":30" : ":00");
        return diff;
    }


    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(PagerDutyServicesDatabaseService.class);
    }

    @Override
    public String insert(String company, DbPagerDutyIncident incident) throws SQLException {
        var keyHolder = new GeneratedKeyHolder();
        var params = new MapSqlParameterSource()
                // .addValue("id", id)
                .addValue("pdServiceId", incident.getPdServiceId())
                .addValue("pdId", incident.getPdId())
                .addValue("summary", incident.getSummary())
                .addValue("urgency", incident.getUrgency())
                .addValue("priority", incident.getPriority())
                .addValue("status", incident.getStatus())
                .addValue("createdAt", incident.getCreatedAt().getEpochSecond())
                .addValue("updatedAt", incident.getUpdatedAt().getEpochSecond())
                .addValue("lastStatusAt", incident.getLastStatusAt().getEpochSecond());
        try {
            params.addValue("details", incident.getDetails() != null ? mapper.writeValueAsString(incident.getDetails()) : "{}");
        } catch (JsonProcessingException e) {
            throw new SQLException("Unable to parse the alert details", e);
        }
        // insert incident
        String sql = MessageFormat.format(INSERT_SQL_FORMAT, company, INCIDENTS_TABLE);
        var count = this.template.update(sql, params,
            keyHolder,
            new String[]{"id"});
        UUID id = null;
        if (count == 0) {
            id = template.queryForObject(
                MessageFormat.format(queryForIncidentId, company, INCIDENTS_TABLE),
                params,
                UUID.class
            );
        } else {
            id = (UUID) keyHolder.getKeys().get("id");
        }
        final UUID incidentId = id;
        // insert statuses
        if (ObjectUtils.isEmpty(incident.getStatuses())) {
            return incidentId.toString();
        }
        incident.getStatuses().forEach(status -> {
            // insert user
            var userId = insertUser(company, status.getUser(), incident.getPdServiceId());
            // insert status
            insertStatus(company, status.toBuilder()
                    .pdUserId(userId)
                    .pdIncidentId(incidentId)
                    .build());
        });
        return id.toString();
    }

    public UUID insertUser(String company, DbPagerDutyUser user, UUID pdServiceId) {
        try {
            Integer integrationId = pagerDutyServicesDatabaseService.getByPagerDutyIntegId(company, pdServiceId)
                    .orElseThrow().getIntegrationId();
            userIdentityService.insert(company, DbScmUser.builder()
                    .integrationId(String.valueOf(integrationId))
                    .displayName(user.getName())
                    .originalDisplayName(user.getName())
                    .cloudId(user.getEmail())
                    .build());
        } catch (SQLException e) {
            log.error("Error while inserting user: {}", e.getMessage(), e);
        }
        var keyHolder = new GeneratedKeyHolder();
        var params = new MapSqlParameterSource()
                .addValue("pdId", user.getPdId())
                .addValue("name", user.getName())
                .addValue("email", user.getEmail())
                .addValue("timeZone", StringUtils.firstNonBlank(user.getTimeZone(), "America/Los_Angeles"));

        int count = this.template.update(
                MessageFormat.format(INSERT_USER_SQL_FORMAT, company, USERS_TABLE_NAME),
                params,
                keyHolder,
                new String[]{"id"}
        );
        if (count == 0) {
            var id = template.queryForObject(
                    MessageFormat.format(queryForUserId, company, USERS_TABLE_NAME),
                    params,
                    UUID.class
            );
            return id;
        }
        return (UUID) keyHolder.getKeys().get("id");
    }

    public UUID insertStatus(String company, DbPagerDutyStatus status) {
        var keyHolder = new GeneratedKeyHolder();
        var params = new MapSqlParameterSource()
                .addValue("pdIncidentId", status.getPdIncidentId())
                .addValue("pdUserId", status.getPdUserId())
                .addValue("status", status.getStatus())
                .addValue("timestamp", status.getTimestamp().getEpochSecond());

        int count = this.template.update(
                MessageFormat.format(INSERT_STATUS_SQL_FORMAT, company, STATUS_TABLE_NAME),
                params,
                keyHolder,
                new String[]{"id"}
        );
        if (count == 0) {
            var id = template.queryForObject(
                    MessageFormat.format(queryForStatusId, company, STATUS_TABLE_NAME),
                    params,
                    UUID.class
            );
            return id;
        }
        return (UUID) keyHolder.getKeys().get("id");
    }

    @Override
    public Boolean update(String company, DbPagerDutyIncident t) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<DbPagerDutyIncident> get(final String company, final String id) throws SQLException {
        return get(company, UUID.fromString(id));
    }

    public Optional<DbPagerDutyIncident> get(final String company, final UUID id) throws SQLException {
        var a = list(company, QueryFilter.builder().strictMatch("id", id).build(), 0, 1, null);
        if (a != null && !CollectionUtils.isEmpty(a.getRecords())) {
            return Optional.of(a.getRecords().get(0));
        }
        return Optional.empty();
    }

    public Optional<DbPagerDutyIncident> getByPagerDutyId(String company, String id) throws SQLException {
        var a = list(company, QueryFilter.builder().strictMatch("pd_id", id).build(), 0, 1, null);
        if (a != null && !CollectionUtils.isEmpty(a.getRecords())) {
            return Optional.of(a.getRecords().get(0));
        }
        return Optional.empty();
    }

    @Override
    public DbListResponse<DbPagerDutyIncident> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return list(company, null, pageNumber, pageSize, null);
    }

    public DbListResponse<DbPagerDutyIncident> list(String company, QueryFilter filters, Integer pageNumber, Integer pageSize
            , Set<UUID> orgProductUuids)
            throws SQLException {
        var params = new MapSqlParameterSource();
        String queryStmt = getQueryStmt(company, filters, params, orgProductUuids, null, true, false);
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        String limit = MessageFormat.format("LIMIT {0} OFFSET {1}", pageSize, pageNumber * pageSize);
        log.info("sql =  {} ", queryStmt);
        var records = template.query(MessageFormat.format(queryStmt + limit, company, INCIDENTS_TABLE), params, (rs, row) -> DbPagerDutyIncident.builder()
                .id((UUID) rs.getObject("id"))
                .pdServiceId((UUID) rs.getObject("pd_service_id"))
                .pdId(rs.getString("pd_id"))
                .summary(rs.getString("summary"))
                .urgency(rs.getString("urgency"))
                .priority(rs.getString("priority"))
                .details(ParsingUtils.parseJsonObject(mapper, "details", rs.getString("details")))
                .status(rs.getString("status"))
                .createdAt(Instant.ofEpochSecond(rs.getLong("created_at")))
                .updatedAt(Instant.ofEpochSecond(rs.getLong("updated_at")))
                .lastStatusAt(Instant.ofEpochSecond(rs.getLong("last_status_at")))
                .build());
        var totalCount = records.size();
        if (totalCount == pageSize) {
            totalCount = template.queryForObject(MessageFormat.format("SELECT count(*) from ( " + queryStmt + ") as count", company, INCIDENTS_TABLE), params, Integer.class);
        }
        return DbListResponse.of(records, totalCount);
    }

    private String getQueryStmt(String company, QueryFilter filters, MapSqlParameterSource params,
                                Set<UUID> orgProductUuids, String calculation, boolean isListQuery,
                                boolean isGetTrendReport)
            throws SQLException {
        Map<Integer, Map<String, Object>> productFilters = getProductFilters(company, orgProductUuids);
        List<String> conditions = new ArrayList<>();
        if (productFilters == null || MapUtils.isEmpty(productFilters)) {
            populateConditions(company, filters, conditions, null, params);
            if (isGetTrendReport)
                return getUnionSqlForTrend(conditions);
            return getUnionSql(conditions, calculation, isListQuery);
        }
        List<String> queryList = productFilters.keySet().stream().map(integ -> {
            QueryFilter queryFilter = QueryFilter.fromRequestFilters(productFilters.get(integ));
            populateConditions(company, queryFilter, conditions, null, params);
            if (isGetTrendReport) {
                String unionSqlForTrend = getUnionSqlForTrend(conditions);
                conditions.clear();
                return unionSqlForTrend;
            }
            String unionSql = getUnionSql(conditions, calculation, isListQuery);
            conditions.clear();
            return unionSql;
        }).collect(Collectors.toList());
        return String.join(" UNION ", queryList);
    }

    private String getUnionSql(List<String> conditions, String calculation, boolean isListQuery) {
        String where = "";
        if (conditions.size() > 0) {
            conditions = conditions.stream().map(item -> item.replaceAll("'", "'''")).collect(Collectors.toList());
            where = "WHERE " + String.join(" AND ", conditions) + " ";
        }
        if (isListQuery) {
            String baseStatement = "FROM {0}.{1} c " + where;
            return "SELECT * " + baseStatement;
        }
        String baseStatement;
        String selectStatement;
        String groupByString;
        String orderByString;
        if (calculation.equalsIgnoreCase("trend")) {
            baseStatement = "SELECT c.pd_service_id, to_timestamp(created_at)::date as trend, c.{3}, count(*) count FROM {0}.{1} c "
                    + where + "GROUP BY c.pd_service_id, trend, c.{3} ORDER BY count desc ";
            selectStatement = "SELECT s.name, a.pd_service_id, ''pagerduty'' as type, a.trend, jsonb_object_agg(a.{3}, a.count) as aggregations ";
            groupByString = " GROUP BY s.name, a.pd_service_id, a.trend ";
            orderByString = " ORDER BY a.trend, s.name ";
        } else {
            baseStatement = "SELECT c.pd_service_id, c.{3}, count(*) count FROM {0}.{1} c "
                    + where + "GROUP BY c.pd_service_id, c.{3} ORDER BY count desc ";
            selectStatement = "SELECT s.name, a.pd_service_id, ''pagerduty'' as type, jsonb_object_agg(a.{3}, a.count) as aggregations ";
            groupByString = " GROUP BY s.name, a.pd_service_id ";
            orderByString = " ORDER BY s.name ";
        }
        return "SELECT * FROM (" + selectStatement
                + "FROM ( "
                + baseStatement
                + ") as a,"
                + "{0}.{2} s "
                + "WHERE s.id = a.pd_service_id "
                + groupByString + orderByString + ") final ";
    }

    public DbListResponse<Map<String, Object>> aggregate(final String company, final String pivot, String calculation,
                                                         final QueryFilter filters, final int pageNumber, Integer pageSize, Set<UUID> orgProductIdsSet) throws SQLException {
        var params = new MapSqlParameterSource();
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        String limit = MessageFormat.format("LIMIT {0} OFFSET {1}", pageSize, pageNumber * pageSize);
        String queryStmt = getQueryStmt(company, filters, params, orgProductIdsSet, calculation, false, false);
        var aggregationQuery = MessageFormat.format(
                queryStmt, company, INCIDENTS_TABLE, PagerDutyServicesDatabaseService.TABLE_NAME, pivot);
        var records = template.query(aggregationQuery + limit, params, DBPagerdutyConverters.distinctRowMapper(mapper, calculation));
        var totalCount = records.size();
        if (totalCount == pageSize) {
            totalCount = template.queryForObject(MessageFormat.format("SELECT count(*) FROM ({0}) as a", aggregationQuery), params, Integer.class);
        }
        return DbListResponse.of(records, totalCount);
    }

    public DbListResponse<Map<String, Integer>> getTrend(final String company, final QueryFilter filters, final int pageNumber,
                                                         Integer pageSize, Set<UUID> orgProductIdsSet) throws SQLException {
        var params = new MapSqlParameterSource();
        String baseStatement = getQueryStmt(company, filters, params, orgProductIdsSet, null, false, true);
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        String limit = MessageFormat.format("LIMIT {0} OFFSET {1}", pageSize, pageNumber * pageSize);
        var baseQuery = MessageFormat.format(baseStatement, company, INCIDENTS_TABLE);
        var records = template.query(baseQuery + limit, params, (rs, row) -> {
            return Map.of(
                    "key", rs.getInt("start_day"),
                    "count", rs.getInt("count")
            );
        });
        var totalCount = records.size();
        if (totalCount == pageSize) {
            totalCount = template.queryForObject(MessageFormat.format("SELECT count(*) FROM ({0}) as a", baseQuery), params, Integer.class);
        }
        return DbListResponse.of(records, totalCount);
    }

    private String getUnionSqlForTrend(List<String> conditions) {
        String where = "";
        if (conditions.size() > 0) {
            conditions = conditions.stream().map(item -> item.replaceAll("'", "'''")).collect(Collectors.toList());
            where = "WHERE " + String.join(" AND ", conditions) + " ";
        }
        return "SELECT extract(epoch from date_trunc(''day'', to_timestamp(created_at))::date)::bigint start_day, count(*) count "
                + "FROM {0}.{1} c "
                + where
                + "GROUP BY start_day "
                + "ORDER BY start_day DESC ";
    }

    /**
     * Returns a list of users with the aggregatins by day of the average incidents acknowledged by the user
     *
     * @param company
     * @param filters
     * @param orgProductIdsSet
     * @param pageNumber
     * @param pageSize
     * @return
     */
    public DbListResponse<Map<String, Object>> getAckTrend(final String company, final QueryFilter filters,
                                                           Set<UUID> orgProductIdsSet, final int pageNumber, final int pageSize) throws SQLException {
        var params = new MapSqlParameterSource();
        String queryStmt = getStmtAckTrendAndReleaseIncidents(company, filters, params, orgProductIdsSet, pageNumber, pageSize);
        List<Map<String, Object>> records = template.queryForObject(queryStmt, params, (rs, row) ->
                ParsingUtils.parseJsonList(mapper, "ack-trend-results", rs.getString("results")));
        var totalCount = records.size();
        if (totalCount == pageSize) {
            totalCount = template.queryForObject("SELECT count(*) FROM (" + queryStmt + ") as a", params, Integer.class);
        }
        return DbListResponse.of(records, totalCount);
    }

    private String getStmtAckTrendAndReleaseIncidents(String company, QueryFilter filters, MapSqlParameterSource params,
                                                      Set<UUID> orgProductUuids, int pageNumber, int pageSize)
            throws SQLException {
        List<String> conditions = new ArrayList<>();
        List<String> teamConditions = new ArrayList<>();
        Map<Integer, Map<String, Object>> productFilters = getProductFilters(company, orgProductUuids);
        if (productFilters == null || MapUtils.isEmpty(productFilters)) {
            populateConditions(company, filters, conditions, teamConditions, params);
            return getSqlAckTrend(company, conditions, teamConditions, pageNumber, pageSize);
        }
        List<String> queryList = productFilters.keySet().stream().map(integ -> {
            QueryFilter queryFilter = QueryFilter.fromRequestFilters(productFilters.get(integ));
            populateConditions(company, queryFilter, conditions, teamConditions, params);
            String unionSql = getSqlAckTrend(company, conditions, teamConditions, pageNumber, pageSize);
            conditions.clear();
            return unionSql;
        }).collect(Collectors.toList());
        return String.join(" UNION ALL ", queryList);
    }

    @Nullable
    private Map<Integer, Map<String, Object>> getProductFilters(String company, Set<UUID> orgProductUuids) throws SQLException {
        Map<Integer, Map<String, Object>> productFilters = null;
        if (orgProductUuids != null) {
            try {
                productFilters = filterParserCommons.getProductFilters(company, orgProductUuids);
            } catch (SQLException e) {
                log.error("Error while getting products...{0}" + e.getMessage(), e);
                throw e;
            }
        }
        return productFilters;
    }

    private String getSqlAckTrend(String company, List<String> conditions, List<String> teamConditions,
                                  int pageNumber, int pageSize) {
        String extraConditions = "";
        if (conditions.size() > 0) {
            extraConditions = " AND " + String.join(" AND ", conditions) + " ";
        }
        String teamConditionString = "";
        if (teamConditions.size() > 0) {
            teamConditionString = " WHERE " + String.join(" AND ", teamConditions) + " ";
        }
        String limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize, pageNumber * pageSize);
        var baseQuery = MessageFormat.format(ackTrendBaseQuery, company, extraConditions, teamConditionString);
        return "SELECT array_to_json(ARRAY(" + baseQuery + limit + ")) results";

    }

    /**
     * Returns a list of users with the total number of minutes the user worked after hours
     *
     * @param company
     * @param filters
     * @param orgProductIdsSet
     * @param pageNumber
     * @param pageSize
     * @return
     */
    @SuppressWarnings("unchecked")
    public DbListResponse<Map<String, Object>> getAfterHoursMinutes(final String company, final int from, final int to,
                                                                    final QueryFilter filters, final Set<UUID> orgProductIdsSet,
                                                                    final int pageNumber, final int pageSize)
            throws SQLException {
        var params = new MapSqlParameterSource();
        var intervals = new ArrayList<Pair<String, String>>();
        var baseQuery = getQueryStmtValues(company, filters, params, orgProductIdsSet, null, false, to, from, intervals);
        String limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize, pageNumber * pageSize);
        List<Map<String, Object>> records = template.query(baseQuery + limit, params, (rs, row) -> {
            List<Long> t = ParsingUtils.parseList(mapper, "timestamps", Long.class, rs.getString("timestamps"));
            return Map.of(
                    "user_id", rs.getString("user_id"),
                    "name", rs.getString("name"),
                    "email", StringUtils.defaultString(rs.getString("email")),
                    "time_zone", rs.getString("time_zone"),
                    "timestamps", t
            );
        });
        records = records.stream().map(item -> {
            List<Long> t = (List<Long>) item.get("timestamps");
            AtomicInteger count = new AtomicInteger(0);
            var timeZone = (String) item.get("time_zone");
            var offset = getTimeZoneOffsetHours(timeZone);

            t.forEach(timestamp -> {
                for (Pair<String, String> interval : intervals) {
                    var startExtraTime = Instant.parse(interval.getLeft() + offset).getEpochSecond();
                    var endExtraTime = Instant.parse(interval.getRight() + offset).getEpochSecond();
                    if (timestamp > startExtraTime && timestamp < endExtraTime) {
                        var extraTime = timestamp - startExtraTime;
                        count.addAndGet(Long.valueOf(extraTime).intValue());
                        break;
                    }
                }
            });
            var totalExtraHours = Duration.ofSeconds(count.get()).toMinutes();
            return Map.of(
                    "user_id", item.get("user_id"),
                    "name", item.get("name"),
                    "email", item.get("email"),
                    "after_hours_minutes", totalExtraHours);
        }).collect(Collectors.toList());
        var totalCount = records.size();
        if (totalCount == pageSize) {
            totalCount = template.queryForObject("SELECT count(*) FROM (" + baseQuery + ") as a", params, Integer.class);
        }
        return DbListResponse.of(records, totalCount);
    }

    /**
     * Returns a list of cicd jobs with the count of incidents and alerts aggregated by the time between builds for the list of cicd jobs to filter by
     *
     * @param company
     * @param filters
     * @param pageNumber
     * @param pageSize
     * @return
     */
    @SuppressWarnings("unchecked")
    public DbListResponse<Map<String, Object>> getReleaseIncidents(final String company, final QueryFilter filters,
                                                                   final int pageNumber, final int pageSize, Set<UUID> orgProductUUIDs)
            throws SQLException {
        var params = new MapSqlParameterSource();
        // get job runs for the job in the time frame cicd_job_ids
        Collection<String> jobIds = (Collection<String>) filters.getStrictMatches().getOrDefault("cicd_job_ids", Set.<String>of());
        // get lowest and highest date while accumulating time ranges
        // get incidents between the boundaries
        String queryStmt = getStmtAckTrendAndReleaseIncidents(company, filters, params, orgProductUUIDs, pageNumber, pageSize);
        List<Map<String, Object>> records = template.query(queryStmt, params, (rs, row) -> {
            var totalExtraHours = 0;
            ParsingUtils.parseJsonList(mapper, "ack-trend-results", rs.getString("results"));
            return Map.of(
                    "user_id", rs.getString("user_id"),
                    "name", rs.getString("name"),
                    "email", rs.getString("email"),
                    "after_hours_minutes", totalExtraHours
            );
        });
        var totalCount = records.size();
        if (totalCount == pageSize) {
            totalCount = template.queryForObject("SELECT count(*) FROM (" + queryStmt + ") as a", params, Integer.class);
        }
        return DbListResponse.of(records, totalCount);
    }

    /**
     * Returns the values for the filters
     *
     * @param company
     * @param field
     * @param filters
     * @param pageNumber
     * @param pageSize
     * @return
     */
    public DbListResponse<Object> getValues(final String company, String field, final QueryFilter filters,
                                            final Integer pageNumber, Integer pageSize, Set<UUID> orgProductUUIDs) throws SQLException {
        var params = new MapSqlParameterSource();
        field = field.startsWith("incident_") ? field.replace("incident_", "") : field.startsWith("alert_") ? field.replace("alert_", "") : field;
        switch (field) {
            case "priority":
            case "urgency":
            case "severity":
                if (pageSize == null || pageSize < 1) {
                    pageSize = DEFAULT_PAGE_SIZE;
                }
                var sql = getQueryStmtValues(company, filters, params, orgProductUUIDs, field, true, 0, 0, null);
                String limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize.toString(), String.valueOf(pageNumber * pageSize));
                List<Object> items = template.query(sql + limit, params, (rs, row) -> rs.getString("v"));
                var totalCount = items.size();
                if (totalCount == pageSize) {
                    totalCount = template.queryForObject("SELECT COUNT(*) FROM (" + sql + ") AS l", params, Integer.class);
                }
                return DbListResponse.of(items, totalCount);
            case "cicd_job_id":
            case "pd_service":
            case "user_id":
                if (pageSize == null || pageSize < 1) {
                    pageSize = DEFAULT_PAGE_SIZE;
                }
                String query = getQueryStmtValues(company, filters, params, orgProductUUIDs, field, true, 0, 0, null);
                limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize.toString(), String.valueOf(pageNumber * pageSize));
                items = List.copyOf(Set.copyOf(template.query(query + limit, params, (rs, row) ->
                        ParsingUtils.parseJsonObject(mapper, "name", rs.getString("v")))));
                totalCount = items.size();
                if (totalCount == pageSize) {
                    totalCount = template.queryForObject("SELECT COUNT(*) FROM (" + query + ") AS l", params, Integer.class);
                }
                return DbListResponse.of(items, totalCount);
            default:
                return null;
        }
    }

    private String getQueryStmtValues(String company, QueryFilter filters, MapSqlParameterSource params,
                                      Set<UUID> orgProductUuids, String field,
                                      boolean isValuesQuery, int to, int from,
                                      ArrayList<Pair<String, String>> intervals)
            throws SQLException {
        List<String> conditions = new ArrayList<>();
        List<String> teamConditions = new ArrayList<>();
        Map<Integer, Map<String, Object>> productFilters = getProductFilters(company, orgProductUuids);
        if (productFilters == null || MapUtils.isEmpty(productFilters)) {
            if (!isValuesQuery) {
                populateConditions(company, filters, conditions, teamConditions, params);
                return getUnionSqlForAfterHours(company, to, from, filters, conditions, teamConditions, intervals);
            }
            populateConditions(company, filters, conditions, null, params);
            return getSQLStmtValues(company, conditions, field);
        }
        List<String> queryList = productFilters.keySet().stream().map(integ -> {
            QueryFilter queryFilter = QueryFilter.fromRequestFilters(productFilters.get(integ));
            populateConditions(company, queryFilter, conditions, null, params);
            if (!isValuesQuery) {
                String unionSqlForAfterHours = getUnionSqlForAfterHours(company, to, from, filters, conditions, teamConditions, intervals);
                conditions.clear();
                return unionSqlForAfterHours;
            }
            String unionSql = getSQLStmtValues(company, conditions, field);
            conditions.clear();
            return unionSql;
        }).collect(Collectors.toList());
        return String.join(" UNION ALL ", queryList);
    }

    private String getUnionSqlForAfterHours(String company, int to, int from, QueryFilter filters, List<String> conditions, List<String> teamConditions,
                                            ArrayList<Pair<String, String>> intervals) {
        String extraConditions = "";
        String extraFrom = "";
        String teamConditionsString = "";
        if (conditions.size() > 0) {
            extraConditions = " AND " + String.join(" AND ", conditions) + " ";
            extraFrom = MessageFormat.format(", {0}.pd_incidents c", company);
        }
        if (teamConditions.size() > 0) {
            teamConditionsString = " AND " + String.join(" AND ", teamConditions) + " ";
        }
        var activeInstant = Instant.ofEpochSecond(from);
        var toInstant = Instant.ofEpochSecond(to);
        var intervalConditions = new ArrayList<String>();
        DateTimeFormatter userTimeFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"));
        var officeHours = (Map<String, String>) filters.getStrictMatches().getOrDefault("office_hours", Map.of());
        final String startShift = officeHours.getOrDefault("from", "09:00");
        final String endShift = officeHours.getOrDefault("to", "17:00");
        while (activeInstant.isBefore(toInstant)) {
            var initialDay = userTimeFormater.format(activeInstant);
            activeInstant = activeInstant.plus(Duration.ofDays(1));
            var nextDay = userTimeFormater.format(activeInstant);
            intervals.add(Pair.of(initialDay + "T" + endShift + ":00", nextDay + "T" + startShift + ":00"));
            intervalConditions.add(MessageFormat.format(afterHoursTime, initialDay, nextDay, startShift, endShift));
        }
        var timeSpans = "";
        if (intervalConditions.size() > 0) {
            timeSpans = String.join(" OR ", intervalConditions);
        }
        return MessageFormat.format(afterHoursBaseQuery, company, timeSpans, extraFrom, extraConditions, teamConditionsString);
    }

    public String getSQLStmtValues(String company, List<String> conditions, String field) {
        var BASE_FROM = " FROM {0}.{1} as c {2} {3}";
        field = field.startsWith("incident_") ? field.replace("incident_", "") : field.startsWith("alert_") ?
                field.replace("alert_", "") : field;
        String extraFrom = "";
        switch (field) {
            case "priority":
            case "urgency":
            case "severity":
                var extraConditions = "WHERE c." + field + " != '' " + (conditions.size() > 0 ? " AND " + String.join(" AND ", conditions) + " " : "");
                var table = "severity".equalsIgnoreCase(field) ? "pd_alerts" : INCIDENTS_TABLE;
                return MessageFormat.format("SELECT DISTINCT(c.{4}) AS v " + BASE_FROM, company, table, extraConditions, extraFrom, field);
            case "cicd_job_id":
            case "pd_service":
            case "user_id":
                extraConditions = "";
                table = "cicd_job_id".equalsIgnoreCase(field) ? "cicd_jobs" : "pd_service".equalsIgnoreCase(field) ? "pd_services" : "pd_users";
                var nameField = "cicd_job_id".equalsIgnoreCase(field) ? "job_full_name" : "name";
                String sql = MessageFormat.format(BASE_FROM, company, table, extraFrom, "{1}");
                sql = MessageFormat.format("SELECT c.id, c.{2} " + sql, company, extraConditions, nameField);
                return MessageFormat.format("SELECT row_to_json(l) AS v FROM ({1}) AS l ", company, sql);
        }
        return null;
    }

    public boolean isEntryUUID(String inputString) {
        try {
            UUID.fromString(inputString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void populateConditions(String company, QueryFilter filters, @NonNull List<String> conditions,
                                    List<String> teamConditions, @NonNull MapSqlParameterSource params) {
        if (filters == null || filters.getStrictMatches() == null || filters.getStrictMatches().isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : filters.getStrictMatches().entrySet()) {
            if (!allowedFilters.contains(entry.getKey())
                    || filters.getStrictMatches().get(entry.getKey()) == null
                    || ObjectUtils.isEmpty(entry.getValue())) {
                continue;
            }
            boolean isUUID = isEntryUUID(String.valueOf(entry.getValue()));
            if ("from_created".equals(entry.getKey())) {
                conditions.add("c.created_at >= :from_created");
                params.addValue("from_created", entry.getValue());
                continue;
            }
            if ("to_created".equals(entry.getKey())) {
                conditions.add("c.created_at <= :to_created");
                params.addValue("to_created", entry.getValue());
                continue;
            }
            if ("user_ids".equals(entry.getKey()) || entry.getValue().toString().contains("team")) {
                var collection = ((Collection<Object>) entry.getValue())
                        .stream()
                        .filter(ObjectUtils::isNotEmpty)
                        .map(s -> s.toString().trim())
                        .collect(Collectors.toList());
                Map<String, Object> modifiableParamsMap = new HashMap<>(params.getValues());
                TeamUtils.addUsersCondition(company, teamConditions, modifiableParamsMap, "u.email", "user_email_id",
                        false, collection, PAGERDUTY_APPLICATIONS, false);
                addParamValues(params, modifiableParamsMap);
                continue;
            }
            if (entry.getValue() instanceof Collection) {
                var collection = ((Collection<Object>) entry.getValue())
                        .stream()
                        .filter(ObjectUtils::isNotEmpty)
                        .map(s -> s.toString().trim())
                        .collect(Collectors.toSet());
                var tmp = MessageFormat.format("c.{0} = ANY({1})", entry.getKey(), "'{" + String.join(",", collection) + "}'");
                log.debug("filter: {}", tmp);
                conditions.add(tmp);
                continue;
            }
            if (entry.getValue() instanceof UUID || isUUID) {
                conditions.add(MessageFormat.format("c.{0} = :{0}::uuid", entry.getKey()));
            } else if (entry.getValue() instanceof Integer) {
                conditions.add(MessageFormat.format("c.{0} = :{0}::int", entry.getKey()));
            } else if (entry.getValue() instanceof Long) {
                conditions.add(MessageFormat.format("c.{0} = :{0}::bigint", entry.getKey()));
            } else {
                conditions.add(MessageFormat.format("c.{0} = :{0}", entry.getKey()));
            }
            params.addValue(entry.getKey(), entry.getValue().toString().trim());
        }
    }

    private void addParamValues(@NonNull MapSqlParameterSource params, Map<String, Object> copyMap) {
        copyMap.keySet().forEach(key -> params.addValue(key, copyMap.get(key)));
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String delete = "DELETE FROM {0}.{1} WHERE id = :id::uuid";
        var count = template.update(MessageFormat.format(delete, company, INCIDENTS_TABLE), Map.of("id", id));
        return count > 0;
    }

    public DbListResponse<DbPDIncident> list(String company,
                                             PagerDutyFilter filter,
                                             Map<String, SortingOrder> sortBy,
                                             OUConfiguration ouConfig,
                                             Set<UUID> orgProductIdsSet,
                                             Integer pageNumber,
                                             Integer pageSize)
            throws SQLException {
        String filterByProductSQL = "";
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (PD_SORTABLE_COLUMNS.contains(entry.getKey()))
                        return entry.getKey();
                    return "incident_updated_at";
                })
                .orElse("incident_updated_at");
        var params = new MapSqlParameterSource();
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        params.addValue("skip", pageNumber * pageSize);
        params.addValue("limit", pageSize);
        if (StringUtils.isEmpty(filterByProductSQL)) {
            filterByProductSQL = getQueryStatement(company, filter, params, orgProductIdsSet, null,
                    filter.getIssueType(), hasAlertFilters(filter, true, sortByKey), hasOfficeFilters(filter), true
                    , hasIncidentFilters(filter, sortByKey), ouConfig);
        }
        List<DbPDIncident> results = List.of();
        if (pageSize > 0) {
            String sql = "SELECT *"
                    + " FROM (" + filterByProductSQL + ") x ORDER BY " + sortByKey + " " + sortOrder
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DBPagerdutyConverters.incidentListMapper(mapper));
        }
        String countSql = "SELECT COUNT(*) FROM (" + filterByProductSQL + ") a";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbAggregationResult> stackedGroupBy(String company,
                                                              PagerDutyFilter filter,
                                                              Set<UUID> orgProductIdsSet,
                                                              List<PagerDutyFilter.DISTINCT> stacks,
                                                              OUConfiguration ouConfig,
                                                              Map<String, SortingOrder> sorting) throws SQLException {
        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        DbListResponse<DbAggregationResult> result = groupByAndCalculate(company, filter, orgProductIdsSet, ouConfig, sorting);
        log.info("[{}] PagerDuty Agg: done across '{}' - results={}", company, filter.getAcross(), result.getCount());
        if (stacks == null
                || stacks.size() == 0
                || !stackSupported.contains(stacks.get(0))) {
            return result;
        }
        if(!filter.getIssueType().equals(INCIDENT) && stacks.contains(PagerDutyFilter.DISTINCT.user_id))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user_id filter cannot be used with issue_type = alert");
        PagerDutyFilter.DISTINCT stack = stacks.get(0);
        ForkJoinPool threadPool = null;
        try {
            log.info("[{}] PagerDuty Agg: started processing stacks for '{}' across '{}' - buckets={}", company, stack, filter.getAcross(), result.getCount());
            Stream<DbAggregationResult> stream = result.getRecords().parallelStream().map(row -> {
                try {
                    log.info("[{}] PagerDuty Agg: --- currently processing stack for '{}' across '{}' - buckets={}, current='{}'", company, stack, filter.getAcross(), result.getCount(), row.getKey());
                    PagerDutyFilter newFilter;
                    final PagerDutyFilter.PagerDutyFilterBuilder newFilterBuilder = filter.toBuilder();
                    switch (filter.getAcross()) {
                        case user_id:
                            if(row.getKey() == null || row.getKey().equalsIgnoreCase("NA")) {
                                newFilter = newFilterBuilder.userNames(List.of(row.getAdditionalKey())).across(stack).build();
                            } else {
                                newFilter = newFilterBuilder.userIds(List.of(row.getKey())).across(stack).build();
                            }
                            break;
                        case pd_service:
                            newFilter = newFilterBuilder.pdServiceIds(List.of(row.getKey())).across(stack).build();
                            break;
                        case incident_priority:
                            newFilter = newFilterBuilder.incidentPriorities(List.of(row.getKey())).across(stack).build();
                            break;
                        case status:
                            newFilter = filter.getIssueType().equalsIgnoreCase(INCIDENT) ?
                                    newFilterBuilder.incidentStatuses(List.of(row.getKey())).across(stack).build()
                                    : newFilterBuilder.alertStatuses(List.of(row.getKey())).across(stack).build();
                            break;
                        case alert_severity:
                            newFilter = newFilterBuilder.alertSeverities(List.of(row.getKey())).across(stack).build();
                            break;
                        case incident_created_at:
                        case incident_resolved_at:
                        case alert_created_at:
                        case alert_resolved_at:
                            newFilter = getFilterForTrendStack(newFilterBuilder, row, filter.getAcross(), stack,
                                    MoreObjects.firstNonNull(filter.getAggInterval().toString(), "")).build();
                            break;
                        default:
                            throw new SQLException("This stack is not available for scm queries." + stack);
                    }

                    newFilter = newFilter.toBuilder().sort(Map.of(stack.toString(), SortingOrder.ASC)).build();
                    List<DbAggregationResult> currentStackResults = groupByAndCalculate(company, newFilter, orgProductIdsSet, ouConfig, sorting).getRecords();
                    return row.toBuilder().stacks(currentStackResults).build();
                } catch (SQLException e) {
                    throw new RuntimeStreamException(e);
                }
            });
            // -- collecting parallel stream with custom pool
            // (note: the toList collector preserves the encountered order)
            threadPool = new ForkJoinPool(DEFAULT_STACK_PARALLELISM);
            List<DbAggregationResult> finalList = threadPool.submit(() -> stream.collect(Collectors.toList())).join();
            log.info("[{}] Scm Agg: done processing stacks for '{}' across '{}' - buckets={}", company, stack, filter.getAcross(), result.getCount());
            return DbListResponse.of(finalList, finalList.size());
        } catch (RuntimeStreamException e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException) e.getCause();
            }
            throw new SQLException("Failed to execute stack query", e);
        } finally {
            if (threadPool != null) {
                threadPool.shutdown(); // -- Very important: threads in the pool are not GC'ed automatically.
            }
        }
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company,
                                                                   PagerDutyFilter filter,
                                                                   Set<UUID> orgProductIdsSet,
                                                                   OUConfiguration ouConfig,
                                                                   Map<String, SortingOrder> sorting) throws SQLException {
        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        String sortByKey = sorting.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (PD_SORTABLE_COLUMNS.contains(entry.getKey()))
                        return entry.getKey();
                    return "mx";
                })
                .orElse("mx");
        PagerDutyFilter.CALCULATION calculation = filter.getCalculation();
        StringBuilder orderByStringBuilder = new StringBuilder();
        StringBuffer calculationComponentStringBuffer = new StringBuffer();
        Set<String> outerSelects = new HashSet<>();
        Set<String> innerSelects = new HashSet<>();
        Set<String> groupByStrings = new HashSet<>();
        Set<String> orderByString = new HashSet<>();
        var params = new MapSqlParameterSource();
        checkForInvalidFilters(filter, sortByKey);
        parsePagerDutyCalculation(calculation, calculationComponentStringBuffer, filter.getSort(), orderByString, filter.getIssueType());
        parsePagerdutyAcrossOrStack(filter.getAcross(), filter.getAggInterval(), outerSelects, innerSelects,
                groupByStrings, sorting, orderByString, filter.getIssueType());
        String outerSelect = String.join(",", outerSelects);
        String innerSelect = String.join(",", innerSelects);
        String groupByString = String.join(",", groupByStrings);
        orderByStringBuilder.append(String.join(",", orderByString));
        String subQuery = getQueryStatement(company, filter, params, orgProductIdsSet, innerSelect, filter.getIssueType(),
                hasAlertFilters(filter, false, sortByKey), hasOfficeFilters(filter), false, hasIncidentFilters(filter, sortByKey), ouConfig);
        String orderByStringSql = StringUtils.isEmpty(orderByStringBuilder.toString())?
                getOrderBy(sorting, filter.getAcross(), calculation, filter.getIssueType()) : orderByStringBuilder.toString();
        String sql = "SELECT " + outerSelect + "," + calculationComponentStringBuffer +
                " FROM (" + subQuery + " ) a" + " GROUP BY " + groupByString + " ORDER BY " + orderByStringSql;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbAggregationResult> results = template.query(sql, params, DBPagerdutyConverters.distinctPdRowMapper(filter.getAcross(),
                filter.getAcross().name(), filter.getIssueType()));
        return DbListResponse.of(results, results.size());
    }

    private String getOrderBy(Map<String, SortingOrder> sorting, PagerDutyFilter.DISTINCT across,
                              PagerDutyFilter.CALCULATION calculation, String issueType) {
        if(MapUtils.isEmpty(sorting))
            return "mx DESC";
        String groupByField = sorting.keySet().stream().findFirst().get();
        SortingOrder sortOrder = sorting.values().stream().findFirst().get();
        if (!across.toString().equals(groupByField)) {
            if (!calculation.toString().equals(groupByField)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + groupByField);
            }
            switch (calculation) {
                case resolution_time:
                case response_time:
                    return "median " + sortOrder;
            }
        } else {
            switch(across) {
                case user_id:
                    return "user_name " + sortOrder;
                case pd_service:
                    return "service_name " + sortOrder;
                case status:
                    String issueTypePrefix =  issueType.equalsIgnoreCase(INCIDENT) ? "incident_" : "alert_";
                    return issueTypePrefix + across + " " + sortOrder;
            }
        }
        return groupByField + " " + sortOrder;
    }

    public String getQueryStatement(String company, PagerDutyFilter filter, MapSqlParameterSource params,
                                    Set<UUID> orgProductUuids, String innerSelect, String issueType,
                                    boolean hasAlertFilters, boolean hasOfficeHoursFilters, boolean isListQuery,
                                    boolean hasIncidentsFilter, OUConfiguration ouConfig)
            throws SQLException {
        Map<Integer, Map<String, Object>> productFilters = getProductFilters(company, orgProductUuids);
        Map<String, List<String>> conditions = PagerDutyConditionBuilder.createWhereClauseAndUpdateParams(company, params, filter,
                isListQuery, ouConfig, issueType);
        if (productFilters == null || MapUtils.isEmpty(productFilters)) {
            if(isListQuery)
                return getListSqlStmt(company, conditions, hasOfficeHoursFilters, hasAlertFilters);
            return issueType.equalsIgnoreCase(INCIDENT) ? sqlForPagerDutyIncidents(company, conditions, innerSelect, hasAlertFilters, filter.getAcross()) :
                    sqlForPagerDutyAlerts(company, conditions, innerSelect, hasIncidentsFilter, filter.getAcross());
        }
        List<String> queryList = productFilters.keySet().stream().map(integ -> {
            String unionSql = issueType.equalsIgnoreCase(INCIDENT) ?
                    sqlForPagerDutyIncidents(company, conditions, innerSelect, hasAlertFilters, filter.getAcross()) :
                    sqlForPagerDutyAlerts(company, conditions, innerSelect, hasIncidentsFilter, filter.getAcross());
            conditions.get(INCIDENTS_TABLE).clear();
            conditions.get(ALERTS_TABLE).clear();
            return unionSql;
        }).collect(Collectors.toList());
        return String.join(" UNION ", queryList);
    }

    private void parsePagerDutyCalculation(PagerDutyFilter.CALCULATION calculation,
                                           StringBuffer calculationComponentStringBuffer,
                                           Map<String, SortingOrder> sortBy,
                                           Set<String> orderByString,
                                           String issueType) {
        String typeString = issueType.equalsIgnoreCase(INCIDENT) ? "incident_solve_time" : "alert_solve_time";
        String pdId = issueType.equalsIgnoreCase(INCIDENT) ? "incident_pd_id" : "alert_pd_id";
        switch (calculation) {
            case resolution_time:
                calculationComponentStringBuffer.append("COUNT(DISTINCT ").append(pdId).append(") AS ct, MIN(").append(typeString).append(") AS mn, MAX(")
                        .append(typeString).append(") AS mx, ").append("PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY ").append(typeString).append(") AS median, ")
                        .append("PERCENTILE_DISC(0.9) WITHIN GROUP(ORDER BY ").append(typeString).append(") AS p90, AVG(").append(typeString).append(") AS mean");
                parseSortBy(calculation.toString(), orderByString, sortBy, "mx", false);
                break;
            case response_time:
                typeString = issueType.equalsIgnoreCase(INCIDENT) ? "incident_response_time" : "alert_response_time";
                calculationComponentStringBuffer.append("COUNT(DISTINCT ").append(pdId).append(") AS ct, MIN(").append(typeString).append(") AS mn, MAX(")
                        .append(typeString).append(") AS mx, ").append("PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY ").append(typeString).append(") AS median, ")
                        .append("PERCENTILE_DISC(0.9) WITHIN GROUP(ORDER BY ").append(typeString).append(") AS p90, AVG(").append(typeString).append(") AS mean");
                parseSortBy(calculation.toString(), orderByString, sortBy, "mx", false);
                break;
            default:
                calculationComponentStringBuffer.append("COUNT(DISTINCT ").append(pdId).append(") as ct");
                parseSortBy(calculation.toString(), orderByString, sortBy, "ct", true);
                break;
        }
    }

    public static PagerDutyFilter.PagerDutyFilterBuilder getFilterForTrendStack(PagerDutyFilter.PagerDutyFilterBuilder newFilterBuilder,
                                                                        DbAggregationResult row, PagerDutyFilter.DISTINCT across,
                                                                        PagerDutyFilter.DISTINCT stack, String aggInterval) throws SQLException {
        ImmutablePair<Long, Long> timeRange = getTimeRangeForStacks(row, aggInterval);
        switch (across) {
            case incident_created_at:
                newFilterBuilder.incidentCreatedAt(timeRange);
                break;
            case incident_resolved_at:
                newFilterBuilder.incidentResolvedAt(timeRange);
                break;
            case alert_created_at:
                newFilterBuilder.alertCreatedAt(timeRange);
                break;
            case alert_resolved_at:
                newFilterBuilder.alertResolvedAt(timeRange);
                break;
            default:
                throw new SQLException("This across option is not available trend. Provided across: " + across);
        }
        return newFilterBuilder.across(stack);
    }


    private void parsePagerdutyAcrossOrStack(PagerDutyFilter.DISTINCT acrossOrStack, AGG_INTERVAL aggInterval,
                                             Set<String> outerSelects, Set<String> innerSelects, Set<String> groupByStrings,
                                             Map<String, SortingOrder> sortBy, Set<String> orderByString, String issueType) {
        boolean sortAscending = false;
        SortingOrder sortOrder = getSortOrder(sortBy);
        switch (acrossOrStack) {
            case pd_service:
                String acrossType = "pd_service_id";
                if(!issueType.equals(INCIDENT))
                    acrossType =  "alert_service_id";
                outerSelects.add(acrossType + ",service_name");
                groupByStrings.add(acrossType + ",service_name");
                break;
            case user_id:
                outerSelects.add(acrossOrStack.name() + ", user_name");
                groupByStrings.add(acrossOrStack.name() + ", user_name");
                break;
            case alert_severity:
            case incident_priority:
                outerSelects.add(acrossOrStack.name());
                groupByStrings.add(acrossOrStack.name());
                break;
            case status:
                String typeString  = issueType.equalsIgnoreCase(INCIDENT) ? "incident_" : "alert_";
                outerSelects.add(typeString + acrossOrStack.name());
                groupByStrings.add(typeString + acrossOrStack.name());
                break;
            case incident_created_at:
                if (MapUtils.isNotEmpty(sortBy))
                    sortAscending = sortOrder.equals(SortingOrder.ASC);
                AggTimeQueryHelper.AggTimeQuery trendStartAggQuery =
                        AggTimeQueryHelper.getAggTimeQuery("incident_created_at", acrossOrStack.toString(), aggInterval != null ?
                                aggInterval.toString() : null, true, sortAscending);
                innerSelects.add(trendStartAggQuery.getHelperColumn().replaceFirst(",", ""));
                groupByStrings.add(trendStartAggQuery.getGroupBy());
                outerSelects.add(trendStartAggQuery.getSelect());
                orderByString.add(trendStartAggQuery.getOrderBy());
                break;
            case incident_resolved_at:
                if (MapUtils.isNotEmpty(sortBy))
                    sortAscending = sortOrder.equals(SortingOrder.ASC);
                AggTimeQueryHelper.AggTimeQuery trendResolvedAggQuery =
                        AggTimeQueryHelper.getAggTimeQuery("incident_resolved_at",
                                acrossOrStack.toString(), aggInterval != null ?
                                aggInterval.toString() : null, true, sortAscending);
                innerSelects.add(trendResolvedAggQuery.getHelperColumn().replaceFirst(",", ""));
                groupByStrings.add(trendResolvedAggQuery.getGroupBy());
                outerSelects.add(trendResolvedAggQuery.getSelect());
                orderByString.add(trendResolvedAggQuery.getOrderBy());
                break;
            case alert_created_at:
                if (MapUtils.isNotEmpty(sortBy))
                    sortAscending = sortOrder.equals(SortingOrder.ASC);
                AggTimeQueryHelper.AggTimeQuery aggTimeQuery =
                        AggTimeQueryHelper.getAggTimeQuery("alert_created_at", acrossOrStack.toString(), aggInterval != null ?
                                aggInterval.toString() : null, true, sortAscending);
                innerSelects.add(aggTimeQuery.getHelperColumn().replaceFirst(",", ""));
                groupByStrings.add(aggTimeQuery.getGroupBy());
                outerSelects.add(aggTimeQuery.getSelect());
                orderByString.add(aggTimeQuery.getOrderBy());

                break;
            case alert_resolved_at:
                if (MapUtils.isNotEmpty(sortBy))
                    sortAscending = sortOrder.equals(SortingOrder.ASC);
                AggTimeQueryHelper.AggTimeQuery trendAlertResolvedAggQuery =
                        AggTimeQueryHelper.getAggTimeQuery("alert_resolved_at",
                                acrossOrStack.toString(), aggInterval != null ?
                                aggInterval.toString() : null, true, sortAscending);
                innerSelects.add(trendAlertResolvedAggQuery.getHelperColumn().replaceFirst(",", ""));
                groupByStrings.add(trendAlertResolvedAggQuery.getGroupBy());
                outerSelects.add(trendAlertResolvedAggQuery.getSelect());
                orderByString.add(trendAlertResolvedAggQuery.getOrderBy());
                break;
            default:
                Validate.notNull(null, "Invalid across or stack field provided.");
        }
    }


    private SortingOrder getSortOrder(Map<String, SortingOrder> sortBy) {
        if (MapUtils.isEmpty(sortBy)) {
            return SortingOrder.DESC;
        }
        return sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> sortBy.getOrDefault(entry.getKey(), SortingOrder.DESC))
                .orElse(SortingOrder.DESC);
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        ddl.stream()
            .map(st -> MessageFormat.format(st, company, INCIDENTS_TABLE, USERS_TABLE_NAME, STATUS_TABLE_NAME))
            .forEach(template.getJdbcTemplate()::execute);
        return true;
    }

}
