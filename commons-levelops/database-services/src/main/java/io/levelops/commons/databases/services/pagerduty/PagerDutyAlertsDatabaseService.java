package io.levelops.commons.databases.services.pagerduty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.converters.DBPagerdutyConverters;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyAlert;
import io.levelops.commons.databases.models.database.pagerduty.DbPdAlert;
import io.levelops.commons.databases.models.filters.PagerDutyFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.parsers.PagerDutyFilterParserCommons;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.pagerduty.PagerDutyUtils.checkForInvalidFilters;
import static io.levelops.commons.databases.services.pagerduty.PagerDutyUtils.hasIncidentFilters;
import static io.levelops.commons.databases.services.pagerduty.PagerDutyUtils.sqlForPagerDutyAlerts;

@Log4j2
@Service
public class PagerDutyAlertsDatabaseService extends DatabaseService<DbPagerDutyAlert> {

    public static final String TABLE_NAME = "pd_alerts";
    public static final String ALERT = "alert";
    private static final List<String> ddl = List.of(
          "CREATE TABLE IF NOT EXISTS {0}.{1} (\n"
        + "   id               uuid PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),\n"
        + "   pd_service_id    uuid REFERENCES {0}.pd_services(id) ON DELETE SET NULL ON UPDATE CASCADE,\n"
        + "   incident_id      uuid REFERENCES {0}.pd_incidents(id) ON DELETE SET NULL ON UPDATE CASCADE,\n"
        + "   pd_id            varchar(50) NOT NULL,\n"
        + "   summary          text,\n"
        + "   severity         varchar NOT NULL,\n"
        + "   details          jsonb NOT NULL DEFAULT '''{}''',\n"
        + "   status           varchar(20) NOT NULL,\n"
        + "   created_at       bigint NOT NULL DEFAULT extract(epoch from now()),\n"
        + "   updated_at       bigint NOT NULL DEFAULT extract(epoch from now()),\n"
        + "   last_status_at   bigint NOT NULL DEFAULT extract(epoch from now()),\n"
        + "   CONSTRAINT {1}_unique_id UNIQUE(pd_service_id, incident_id, pd_id)"
        + ");",
        
        "CREATE INDEX IF NOT EXISTS {1}_updated_at_idx ON {0}.{1}(updated_at)",
        
        "CREATE INDEX IF NOT EXISTS {1}_created_at_idx ON {0}.{1}(created_at)",
        
        "CREATE INDEX IF NOT EXISTS {1}_last_status_at_idx ON {0}.{1}(last_status_at)",
        
        "CREATE INDEX IF NOT EXISTS {1}_pd_service_id_idx ON {0}.{1}(pd_service_id)",
        
        "CREATE INDEX IF NOT EXISTS {1}_incident_id_idx ON {0}.{1}(incident_id)",
        
        "CREATE INDEX IF NOT EXISTS {1}_severity_idx ON {0}.{1}(severity)",
        
        "CREATE INDEX IF NOT EXISTS {1}_status_idx ON {0}.{1}(status)");

    private static final String INSERT_SQL_FORMAT = "INSERT INTO {0}.{1}(id, pd_service_id, incident_id, pd_id, summary, severity, details, status, created_at, updated_at, last_status_at) "
                                            + "VALUES(:id, :pdServiceId, :incidentId, :pdId, :summary, :severity, :details::jsonb, :status, :createdAt, :updatedAt, :lastStatusAt) "
                                            + "ON CONFLICT(pd_service_id, incident_id, pd_id) DO UPDATE SET (summary,severity,details,status,updated_at, last_status_at) = "
                                            + "(EXCLUDED.summary, EXCLUDED.severity, EXCLUDED.details::jsonb, EXCLUDED.status, EXCLUDED.updated_at, EXCLUDED.last_status_at)";

    private static final Set<String> allowedFilters = Set.of("id", "pd_service_id", "incident_id", "pd_id", "severity", "status", "created_at", "updated_at", "last_status_at");
    private static final Set<String> PD_SORTABLE_COLUMNS = Set.of("alert_severity", "alert_created_at", "alert_updated_at", "alert_last_status_at",
            "alert_resolved_at", "alert_status");
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String ALERTS_TABLE = "pd_alerts";

    private final ObjectMapper mapper;
    private final NamedParameterJdbcTemplate template;
    private PagerDutyFilterParserCommons filterParserCommons;
    private ProductsDatabaseService productsDatabaseService;

    @Autowired
    public PagerDutyAlertsDatabaseService(ObjectMapper mapper, DataSource dataSource) {
        super(dataSource);
        this.mapper = mapper;
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.productsDatabaseService = new ProductsDatabaseService(dataSource, mapper);
        this.filterParserCommons = new PagerDutyFilterParserCommons(productsDatabaseService);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(PagerDutyServicesDatabaseService.class, PagerDutyIncidentsDatabaseService.class);
    }

    @Override
    public String insert(String company, DbPagerDutyAlert alert) throws SQLException {
        UUID id = alert.getId() != null ? alert.getId() : UUID.randomUUID();


        var params = new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("pdServiceId", alert.getPdServiceId())
                    .addValue("pdId", alert.getPdId())
                    .addValue("incidentId", alert.getIncidentId())
                    .addValue("summary", StringUtils.defaultString(alert.getSummary()))
                    .addValue("severity", StringUtils.defaultString(alert.getSeverity()))
                    .addValue("status", StringUtils.defaultString(alert.getStatus()))
                    .addValue("createdAt", alert.getCreatedAt().getEpochSecond())
                    .addValue("updatedAt", alert.getUpdatedAt().getEpochSecond())
                    .addValue("lastStatusAt", alert.getLastStatusAt().getEpochSecond());

        try {
            params.addValue("details", alert.getDetails() != null ? mapper.writeValueAsString(alert.getDetails()) : "{}");
        } catch (JsonProcessingException e) {
            throw new SQLException("Unable to parse the alert details", e);
        }

        this.template.update(MessageFormat.format(INSERT_SQL_FORMAT, company,TABLE_NAME), params);
        return id.toString();
    }

    @Override
    public Boolean update(String company, DbPagerDutyAlert t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DbPagerDutyAlert> get(String company, String id) throws SQLException {
        var a = list(company, QueryFilter.builder().strictMatch("id", UUID.fromString(id)).build(), 0, 1, null);
        if (a != null && !CollectionUtils.isEmpty(a.getRecords())) {
            return Optional.of(a.getRecords().get(0));
        }
        return Optional.empty();
    }

    public Optional<DbPagerDutyAlert> getByPagerDutyId(String company, String id) throws SQLException {
        var a = list(company, QueryFilter.builder().strictMatch("pd_id", id).build(), 0, 1, null);
        if (a != null && !CollectionUtils.isEmpty(a.getRecords())) {
            return Optional.of(a.getRecords().get(0));
        }
        return Optional.empty();
    }

    @Override
    public DbListResponse<DbPagerDutyAlert> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return list(company, null, pageNumber, pageSize, null);
    }

    public DbListResponse<DbPagerDutyAlert> list(String company, QueryFilter filters, Integer pageNumber, Integer pageSize, Set<UUID> orgProductUUIDs)
            throws SQLException {
        var params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        String queryStmt = getQueryStmt(company, filters, conditions, params, null , orgProductUUIDs, true);
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        String limit = MessageFormat.format("LIMIT {0} OFFSET {1}", pageSize, pageNumber*pageSize) ;
        var records = template.query(MessageFormat.format(queryStmt  + limit, company, TABLE_NAME), params, (rs, row) -> DbPagerDutyAlert.builder()
            .id((UUID)rs.getObject("id"))
            .pdServiceId((UUID) rs.getObject("pd_service_id"))
            .incidentId((UUID) rs.getObject("incident_id"))
            .summary(rs.getString("summary"))
            .severity(rs.getString("severity"))
            .details(ParsingUtils.parseJsonObject(mapper, "details", rs.getString("details")))
            .status(rs.getString("status"))
            .createdAt(Instant.ofEpochSecond(rs.getLong("created_at")))
            .updatedAt(Instant.ofEpochSecond(rs.getLong("updated_at")))
            .lastStatusAt(Instant.ofEpochSecond(rs.getLong("last_status_at")))
            .build());
        var totalCount = records.size();
        if (totalCount == pageSize) {
            totalCount = template.queryForObject(MessageFormat.format("SELECT count(*) from ( " + queryStmt + ") as count", company, TABLE_NAME), params, Integer.class);
        }
        return DbListResponse.of(records, totalCount);
    }

    private String getQueryStmt(String company, QueryFilter filters, List<String> conditions, MapSqlParameterSource params,
                                String calculation, Set<UUID> orgProductUuids, boolean isListQuery)
            throws SQLException {
        Map<Integer, Map<String, Object>> productFilters = getProductFilters(company, orgProductUuids);
        if (productFilters == null || MapUtils.isEmpty(productFilters)) {
            populateConditions(filters, conditions, params);
            return getUnionSql(conditions, calculation, isListQuery);
        }
        Map<Integer, Map<String, Object>> finalProductFilters = productFilters;
        List<String> queryList = productFilters.keySet().stream().map(integ -> {
            QueryFilter queryFilter = QueryFilter.fromRequestFilters(finalProductFilters.get(integ));
            populateConditions(queryFilter, conditions, params);
            String unionSql = getUnionSql(conditions, calculation, isListQuery);
            conditions.clear();
            return unionSql;
        }).collect(Collectors.toList());
        return String.join(" UNION ", queryList);
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

    private String getUnionSql(List<String> conditions, String calculation, boolean isListQuery) {
        String where = "";
        if (conditions.size() > 0) {
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
        return "SELECT * FROM ( "+ selectStatement
                + "FROM ( "
                + baseStatement
                + ") as a,"
                + "{0}.{2} s "
                + "WHERE s.id = a.pd_service_id "
                + groupByString + orderByString + ") final ";
    }

    public DbListResponse<Map<String, Object>> aggregate(final String company, final String pivot, final String calculation,
                                                         final QueryFilter filters, final int pageNumber, Integer pageSize,
                                                         Set<UUID> orgProductIdsSet)
            throws SQLException {
        var params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        String limit = MessageFormat.format("LIMIT {0} OFFSET {1}", pageSize, pageNumber * pageSize);
        String queryStmt = getQueryStmt(company, filters, conditions, params, calculation, orgProductIdsSet, false);
        var aggregationQuery = MessageFormat.format(
                queryStmt, company, TABLE_NAME, PagerDutyServicesDatabaseService.TABLE_NAME, pivot);
        var records = template.query(aggregationQuery + limit, params, DBPagerdutyConverters.distinctRowMapper(mapper, calculation));
        var totalCount = records.size();
        if (totalCount == pageSize) {
            totalCount = template.queryForObject(MessageFormat.format("SELECT count(*) FROM ({0}) as a", aggregationQuery), params, Integer.class);
        }
        return DbListResponse.of(records, totalCount);
    }

    @SuppressWarnings("unchecked")
    private void populateConditions(QueryFilter filters, @NonNull List<String> conditions, @NonNull MapSqlParameterSource params) {
        if (filters == null || filters.getStrictMatches() == null || filters.getStrictMatches().isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry: filters.getStrictMatches().entrySet()) {
            if (!allowedFilters.contains(entry.getKey()) || filters.getStrictMatches().get(entry.getKey()) == null) {
                continue;
            }
            boolean isUUID = isEntryUUID(String.valueOf(entry.getValue()));
            if (entry.getValue() instanceof Collection) {
                var collection = ((Collection<String>) entry.getValue())
                            .stream()
                            .filter(Strings::isNotBlank)
                            .map(s -> s.toUpperCase())
                            .collect(Collectors.toSet());
                var tmp = MessageFormat.format("c.{0} = ANY({1})", entry.getKey(), "'''{" + String.join(",", collection) + "}'''");
                log.debug("filter: {}", tmp);
                conditions.add(tmp);
                continue;
            }
            if (entry.getValue() instanceof UUID || isUUID) {
                conditions.add(MessageFormat.format("c.{0} = :{0}::uuid", entry.getKey()));
            }
            else {
                conditions.add(MessageFormat.format("c.{0} = :{0}", entry.getKey()));
            }
            params.addValue(entry.getKey(), entry.getValue().toString());
        }
    }
    public DbListResponse<DbPdAlert> list(String company,
                                          PagerDutyFilter filter,
                                          Map<String, SortingOrder> sortBy,
                                          Set<UUID> orgProductIdsSet,
                                          Integer pageNumber,
                                          Integer pageSize,
                                          OUConfiguration ouConfig)
            throws SQLException {
        String filterByProductSQL = "";
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (PD_SORTABLE_COLUMNS.contains(entry.getKey()))
                        return entry.getKey();
                    return "alert_updated_at";
                })
                .orElse("alert_updated_at");
        checkForInvalidFilters(filter, sortByKey);
        var params = new MapSqlParameterSource();
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        params.addValue("skip", pageNumber * pageSize);
        params.addValue("limit", pageSize);
        if (StringUtils.isEmpty(filterByProductSQL)) {
            filterByProductSQL = getQueryStatement(company, filter, params, orgProductIdsSet,
                    hasIncidentFilters(filter, sortByKey), ouConfig);
        }
        List<DbPdAlert> results = List.of();
        if (pageSize > 0) {
            String sql = "SELECT * "
                    + " FROM (" + filterByProductSQL + ") x ORDER BY " + sortByKey + " " + sortOrder
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DBPagerdutyConverters.alertListMapper(mapper));
        }
        String countSql = "SELECT COUNT(*) FROM (" + filterByProductSQL + ") a";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    public String getQueryStatement(String company, PagerDutyFilter filter, MapSqlParameterSource params,
                                    Set<UUID> orgProductUuids, boolean hasIncidentsFilter, OUConfiguration ouConfig) throws SQLException {
        Map<Integer, Map<String, Object>> productFilters = getProductFilters(company, orgProductUuids);
        Map<String, List<String>> conditions = PagerDutyConditionBuilder.createWhereClauseAndUpdateParams
                (company, params, filter, false, ouConfig, filter.getIssueType());
        if (productFilters == null || MapUtils.isEmpty(productFilters)) {
            return sqlForPagerDutyAlerts(company, conditions, null, hasIncidentsFilter, filter.getAcross());
        }
        List<String> queryList = productFilters.keySet().stream().map(integ -> {
            String unionSql = sqlForPagerDutyAlerts(company, conditions, null, hasIncidentsFilter, filter.getAcross());
            conditions.get(ALERTS_TABLE).clear();
            return unionSql;
        }).collect(Collectors.toList());
        return String.join(" UNION ", queryList);
    }

    public boolean isEntryUUID(String inputString) {
        try {
            UUID.fromString(inputString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String delete = "DELETE FROM {0}.{1} WHERE id = :id::uuid";
        var count = template.update(MessageFormat.format(delete, company, TABLE_NAME), Map.of("id", id));
        return count > 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        ddl.stream()
            .map(st -> MessageFormat.format(st, company, TABLE_NAME))
            .forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    
}
