package io.levelops.commons.databases.services;

import io.levelops.commons.databases.converters.DbSalesforceCaseConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.salesforce.DbSalesforceCase;
import io.levelops.commons.databases.models.database.salesforce.DbSalesforceCaseHistory;
import io.levelops.commons.databases.models.filters.SalesforceCaseFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
@Service
public class SalesforceCaseService extends DatabaseService<DbSalesforceCase> {

    private static final String CASES_TABLE = "salesforce_cases";
    private static final int NUM_IDLE_DAYS = 30;

    private static final Set<String> SORTABLE_COLUMNS = Set.of("bounces", "hops",
            "solve_time", "sf_created_at", "resolved_at", "sf_modified_at", "status",
            "priority", "type", "ingested_at");

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public SalesforceCaseService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        ;
    }


    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbSalesforceCase salesforceCase) throws SQLException {
        return template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            String deleteSql = "DELETE FROM " + company + "." + CASES_TABLE
                    + " WHERE case_id = ? AND integration_id = ? AND ingested_at = ?";
            //insert new case
            String sql = "INSERT INTO " + company + "." + CASES_TABLE +
                    " (case_id,integration_id,account_name,subject,contact,case_number," +
                    "creator,is_closed,is_deleted,is_escalated,origin,status,type,priority," +
                    "reason,bounces,hops,sf_created_at,sf_modified_at,resolved_at,ingested_at)" +
                    " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,to_timestamp(?))";

            try (PreparedStatement del = conn.prepareStatement(deleteSql);
                 PreparedStatement insertCase = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                del.setString(1, salesforceCase.getCaseId());
                del.setInt(2, NumberUtils.toInt(salesforceCase.getIntegrationId()));
                del.setDate(3, new Date(TimeUnit.SECONDS.toMillis(salesforceCase.getIngestedAt())));
                del.executeUpdate();

                int i = 1;
                insertCase.setObject(i++, salesforceCase.getCaseId());
                insertCase.setObject(i++, NumberUtils.toInt(salesforceCase.getIntegrationId()));
                insertCase.setObject(i++, salesforceCase.getAccountName());
                insertCase.setObject(i++, salesforceCase.getSubject());
                insertCase.setObject(i++, salesforceCase.getContact());
                insertCase.setObject(i++, salesforceCase.getCaseNumber());
                insertCase.setObject(i++, salesforceCase.getCreator());
                insertCase.setObject(i++, salesforceCase.getIsClosed());
                insertCase.setObject(i++, salesforceCase.getIsDeleted());
                insertCase.setObject(i++, salesforceCase.getIsEscalated());
                insertCase.setObject(i++, salesforceCase.getOrigin());
                insertCase.setObject(i++, salesforceCase.getStatus());
                insertCase.setObject(i++, salesforceCase.getType());
                insertCase.setObject(i++, salesforceCase.getPriority());
                insertCase.setObject(i++, salesforceCase.getReason());
                insertCase.setObject(i++, 0);
                insertCase.setObject(i++, 0);
                insertCase.setTimestamp(i++, new Timestamp(salesforceCase.getCreatedAt().toInstant().toEpochMilli()));
                insertCase.setTimestamp(i++, new Timestamp(salesforceCase.getLastModifiedAt().toInstant().toEpochMilli()));
                insertCase.setTimestamp(i++, salesforceCase.getResolvedAt() != null ?
                        new Timestamp(salesforceCase.getResolvedAt().toInstant().toEpochMilli()) : null);
                insertCase.setObject(i, salesforceCase.getIngestedAt());

                int insertedRows = insertCase.executeUpdate();
                if (insertedRows == 0)
                    throw new SQLException("Failed to insert row.");
                String insertedRowId = null;
                try (ResultSet rs = insertCase.getGeneratedKeys()) {
                    if (rs.next())
                        insertedRowId = rs.getString(1);
                }
                if (insertedRowId == null)
                    throw new SQLException("Failed to get inserted rowid.");
                return insertedRowId;
            }
        }));
    }

    public String insertCaseHistory(String company, DbSalesforceCaseHistory caseHistory, Long ingestedAt) {
        return template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            String updateSql = "UPDATE " + company + ".salesforce_cases SET hops = ? , bounces = ? ," +
                    " modified_at = ? WHERE case_id = ? AND integration_id = ? AND ingested_at = to_timestamp(?)";

            try (PreparedStatement updateCase = conn.prepareStatement(updateSql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                if (caseHistory.getHops() > 0 || caseHistory.getBounces() > 0) {
                    updateCase.setObject(i++, caseHistory.getHops());
                    updateCase.setObject(i++, caseHistory.getBounces());
                    updateCase.setObject(i++, new Timestamp(System.currentTimeMillis()));
                    updateCase.setObject(i++, caseHistory.getCaseId());
                    updateCase.setObject(i++, Integer.parseInt(caseHistory.getIntegrationId()));
                    updateCase.setObject(i, ingestedAt);

                    updateCase.addBatch();
                    updateCase.clearParameters();
                    updateCase.executeBatch();
                }

                return caseHistory.getCaseId();
            }
        }));
    }

    @Override
    public Boolean update(String company, DbSalesforceCase t) throws SQLException {
        return null;
    }

    @Override
    public Optional<DbSalesforceCase> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    public Optional<DbSalesforceCase> get(String company, String caseId, String integrationId, Long ingestedAt) {
        Validate.notNull(caseId, "Missing case id.");
        Validate.notNull(ingestedAt, "Missing ingestedAt.");
        Validate.notNull(integrationId, "Missing integrationId.");
        List<DbSalesforceCase> data = template.query(
                "SELECT * FROM " + company + "." + CASES_TABLE + " as cases"
                        + " WHERE cases.case_id = :caseId"
                        + " AND cases.ingested_at = to_timestamp(:ingestedat)"
                        + " AND cases.integration_id = :integid",
                Map.of("caseId", caseId, "integid", NumberUtils.toInt(integrationId), "ingestedat", ingestedAt),
                DbSalesforceCaseConverters.listRowMapper());
        return data.stream().findFirst();
    }

    public Long getMaxIngestedDate(String company) {
        String query = "SELECT MAX(ingested_at) AS latest_ingested_date FROM " + company + "." + CASES_TABLE;
        return template.query(query, rs -> {
            if (!rs.next())
                return null;
            return rs.getTimestamp("latest_ingested_date").toInstant().getEpochSecond();
        });
    }

    @Override
    public DbListResponse<DbSalesforceCase> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return list(company, SalesforceCaseFilter.builder().build(), Collections.emptyMap(), pageNumber, pageSize);
    }

    public DbListResponse<DbSalesforceCase> list(String company,
                                                 SalesforceCaseFilter filter,
                                                 Map<String, SortingOrder> sortBy,
                                                 Integer pageNumber,
                                                 Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        Long latestIngestedDate = filter.getIngestedAt();
        Map<String, List<String>> conditions = createWhereClauseAndUpdateParams(params, filter.getExtraCriteria(),
                filter.getCaseIds(), filter.getCaseNumbers(), filter.getPriorities(), filter.getStatuses(), filter.getContacts(),
                filter.getTypes(), filter.getIntegrationIds(), filter.getAccounts(), filter.getAge(), filter.getSFUpdatedRange(), 
                filter.getSFUpdatedRange(), latestIngestedDate);
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (SORTABLE_COLUMNS.contains(entry.getKey()))
                        return entry.getKey();
                    return "sf_created_at";
                })
                .orElse("sf_created_at");
        SortingOrder sortingOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);

        String casesWhere = "";
        if (conditions.get(CASES_TABLE).size() > 0)
            casesWhere = " WHERE " + String.join(" AND ", conditions.get(CASES_TABLE));

        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);

        List<DbSalesforceCase> results = List.of();
        String selectSfCasesSql = "SELECT * FROM " + company + "." + CASES_TABLE + " as cases"
                + casesWhere + " ORDER BY " + sortByKey + " " + sortingOrder.toString();
        if (pageSize > 0) {
            String sql = selectSfCasesSql + " OFFSET :skip LIMIT :limit";
            log.info("sql : {}", sql);
            log.info("params : {}", params);
            results = template.query(sql, params, DbSalesforceCaseConverters.listRowMapper());
        }
        String countSql = "SELECT COUNT(*) FROM " + company + "." + CASES_TABLE + " as cases" + casesWhere;
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company, SalesforceCaseFilter filter,
                                                                   String configTableKey) throws SQLException {
        SalesforceCaseFilter.CALCULATION calculation = filter.getCalculation();
        SalesforceCaseFilter.DISTINCT across = filter.getAcross();
        if (calculation == null)
            calculation = SalesforceCaseFilter.CALCULATION.case_count;
        if (StringUtils.isNotEmpty(configTableKey)) {
            across = SalesforceCaseFilter.DISTINCT.none;
        }
        Map<String, Object> params = new HashMap<>();
        Long latestIngestedDate = null;
        String sql, calculationComponent;
        String selectDistinctString, groupByString, orderByString;
        String extraColumns = null;
        Optional<String> additionalKey =  Optional.empty();
        String intervalColumn = "";
        switch (calculation) {
            case hops:
                calculationComponent =
                        "MIN(hops) AS mn,MAX(hops) AS mx,COUNT(id) AS ct,PERCENTILE_DISC(0.5) " +
                                "WITHIN GROUP(ORDER BY hops)";
                orderByString = "mx DESC";
                break;
            case bounces:
                calculationComponent =
                        "MIN(bounces) AS mn,MAX(bounces) AS mx,COUNT(id) AS ct,PERCENTILE_DISC(0.5) " +
                                "WITHIN GROUP(ORDER BY bounces)";
                orderByString = "mx DESC";
                break;
            case resolution_time:
                extraColumns = ", EXTRACT(EPOCH FROM (COALESCE(resolved_at,'now')-sf_created_at)) AS solve_time";
                calculationComponent = "MIN(solve_time) AS mn,MAX(solve_time) AS mx,COUNT(id) AS ct,PERCENTILE_DISC(0.5) " +
                        "WITHIN GROUP(ORDER BY solve_time)";
                orderByString = "mx DESC";
                break;
            default:
                orderByString = "ct DESC";
                calculationComponent = "COUNT(id) as ct";
                break;
        }
        switch (across) {
            case trend:
                AggTimeQueryHelper.AggTimeQuery ticketModAggQuery = AggTimeQueryHelper.getAggTimeQuery("ingested_at",
                        across.toString(), filter.getAggInterval() != null ? filter.getAggInterval().toString() : null, false, true);
                selectDistinctString = ticketModAggQuery.getSelect();
                intervalColumn = ticketModAggQuery.getHelperColumn();
                additionalKey = Optional.of(ticketModAggQuery.getIntervalKey());
                groupByString = ticketModAggQuery.getGroupBy();
                orderByString = ticketModAggQuery.getOrderBy();
                break;
            case none:
                groupByString = "";
                latestIngestedDate = filter.getIngestedAt();
                selectDistinctString = "";
                break;
            default:
                groupByString = across.toString();
                latestIngestedDate = filter.getIngestedAt();
                selectDistinctString = across.toString();
                break;
        }

        Map<String, List<String>> conditions = createWhereClauseAndUpdateParams(params, filter.getExtraCriteria(),
                filter.getCaseIds(), filter.getCaseNumbers(), filter.getPriorities(), filter.getStatuses(), filter.getContacts(),
                filter.getTypes(), filter.getIntegrationIds(), filter.getAccounts(), filter.getAge(), filter.getSFCreatedRange(), 
                filter.getSFUpdatedRange(), latestIngestedDate);

        String casesWhere = "";
        if (conditions.get(CASES_TABLE).size() > 0)
            casesWhere = " WHERE " + String.join(" AND ", conditions.get(CASES_TABLE));

        List<DbAggregationResult> results;

        if (StringUtils.isNotEmpty(configTableKey)) {
            sql = "SELECT " + "'" + configTableKey + "'" + " as config_key, " + calculationComponent
                    + " FROM ( SELECT *"
                    + (extraColumns != null ? extraColumns : "")
                    + intervalColumn
                    + " FROM " + company + "." + CASES_TABLE + " as cases"
                    + casesWhere
                    + " ) as finaltable";
            results = template.query(sql, params, DbSalesforceCaseConverters.distinctRowMapper(
                    "config_key", calculation, additionalKey));
        } else {
            sql = "SELECT " + selectDistinctString + "," + calculationComponent
                    + " FROM ( SELECT *"
                    + (extraColumns != null ? extraColumns : "")
                    + intervalColumn
                    + " FROM " + company + "." + CASES_TABLE + " as cases"
                    + casesWhere
                    + " ) as finaltable"
                    + " GROUP BY " + groupByString + " ORDER BY " + orderByString;
            results = template.query(sql, params, DbSalesforceCaseConverters.distinctRowMapper(
                    across.toString(), calculation, additionalKey));
        }

        return DbListResponse.of(results, results.size());
    }

    protected Map<String, List<String>> createWhereClauseAndUpdateParams(Map<String, Object> params,
                                                                         List<SalesforceCaseFilter.EXTRA_CRITERIA> extraCriteria,
                                                                         List<String> caseIds,
                                                                         List<String> caseNumbers,
                                                                         List<String> priorities,
                                                                         List<String> statuses,
                                                                         List<String> contacts,
                                                                         List<String> types,
                                                                         List<String> integrationIds,
                                                                         List<String> accounts,
                                                                         Map<String, Object> age,
                                                                         ImmutablePair<Long, Long> salesForceCaseCreatedRange,
                                                                         ImmutablePair<Long, Long> salesForceCaseUpdatedRange,
                                                                         Long ingestedAt) {
        List<String> casesTblConditions = new ArrayList<>();
        if (CollectionUtils.isEmpty(extraCriteria)) {
            extraCriteria = List.of();
        }

        if (CollectionUtils.isNotEmpty(caseIds)) {
            casesTblConditions.add("case_id IN (:caseIds)");
            params.put("caseIds", caseIds);
        }

        if (CollectionUtils.isNotEmpty(caseNumbers)) {
            casesTblConditions.add("case_number IN (:caseNumbers)");
            params.put("caseNumbers", caseNumbers);
        }

        if (CollectionUtils.isNotEmpty(priorities)) {
            casesTblConditions.add("priority IN (:priorities)");
            params.put("priorities", priorities);
        }

        if (CollectionUtils.isNotEmpty(statuses)) {
            casesTblConditions.add("status IN (:statuses)");
            params.put("statuses", statuses);
        }

        if (CollectionUtils.isNotEmpty(contacts)) {
            casesTblConditions.add("contact IN (:contacts)");
            params.put("contacts", contacts);
        }

        if (CollectionUtils.isNotEmpty(types)) {
            casesTblConditions.add("type IN (:types)");
            params.put("types", types);
        }

        if (CollectionUtils.isNotEmpty(integrationIds)) {
            casesTblConditions.add("integration_id IN (:integration_ids)");
            params.put("integration_ids",
                    integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }

        if (CollectionUtils.isNotEmpty(accounts)) {
            casesTblConditions.add("account_name IN (:accounts)");
            params.put("accounts", accounts);
        }
        if (salesForceCaseCreatedRange != null) {
            if (salesForceCaseCreatedRange.getLeft() != null) {
                casesTblConditions.add("sf_created_at > to_timestamp( :sf_created_start )");
                params.put("sf_created_start", salesForceCaseCreatedRange.getLeft());
            }
            if (salesForceCaseCreatedRange.getRight() != null) {
                casesTblConditions.add("sf_created_at < to_timestamp( :sf_created_end )");
                params.put("sf_created_end", salesForceCaseCreatedRange.getRight());
            }
        }
        if (salesForceCaseUpdatedRange != null) {
            if (salesForceCaseUpdatedRange.getLeft() != null) {
                casesTblConditions.add("sf_modified_at > to_timestamp( :sf_updated_start )");
                params.put("sf_updated_start", salesForceCaseUpdatedRange.getLeft());
            }
            if (salesForceCaseUpdatedRange.getRight() != null) {
                casesTblConditions.add("sf_modified_at < to_timestamp( :sf_updated_end )");
                params.put("sf_updated_end", salesForceCaseUpdatedRange.getRight());
            }
        }

        if(MapUtils.isNotEmpty(age)) {
            Integer caseAgeMin = age.get("$gt") != null ? Integer.valueOf(age.get("$gt").toString()) : null;
            Integer caseAgeMax = age.get("$lt") != null ? Integer.valueOf(age.get("$lt").toString()) : null;

            if(caseAgeMin != null && caseAgeMin != 0) {
                casesTblConditions.add("sf_created_at <= TIMESTAMP 'now' - interval '" + caseAgeMin + " days'");
            }

            if(caseAgeMax != null && caseAgeMax != 0) {
                casesTblConditions.add("sf_created_at >= TIMESTAMP 'now' - interval '" + caseAgeMax + " days'");
            }
        }

        if (ingestedAt != null) {
            casesTblConditions.add("ingested_at = to_timestamp(:ingested_at)");
            params.put("ingested_at", ingestedAt);
        }

        for (SalesforceCaseFilter.EXTRA_CRITERIA hygieneType : extraCriteria) {
            switch (hygieneType) {
                case idle:
                    casesTblConditions.add("sf_modified_at < TIMESTAMP 'now' - interval '" + NUM_IDLE_DAYS + " days'");
                    break;
                case no_contact:
                    casesTblConditions.add("contact = :no_contact");
                    params.put("no_contact", DbSalesforceCaseHistory.UNASSIGNED);
                    break;
            }
        }


        return Map.of(CASES_TABLE, casesTblConditions);
    }

    public int cleanUpOldData(String company, Long currentTime, Long olderThanSeconds) {
        return template.update("DELETE FROM " + company + "." + CASES_TABLE + " WHERE ingested_at < :olderThanTime",
                Map.of("olderThanTime", new Timestamp(TimeUnit.SECONDS.toMillis(currentTime - olderThanSeconds))));
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return true;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + "." + CASES_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    case_id VARCHAR NOT NULL,\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    account_name VARCHAR,\n" +
                        "    case_number VARCHAR NOT NULL,\n" +
                        "    subject VARCHAR,\n" +
                        "    contact VARCHAR,\n" +
                        "    creator VARCHAR NOT NULL,\n" +
                        "    is_closed BOOLEAN,\n" +
                        "    is_deleted BOOLEAN,\n" +
                        "    is_escalated BOOLEAN,\n" +
                        "    origin VARCHAR,\n" +
                        "    status VARCHAR NOT NULL,\n" +
                        "    type VARCHAR,\n" +
                        "    priority VARCHAR,\n" +
                        "    reason VARCHAR,\n" +
                        "    sf_created_at TIMESTAMP NOT NULL,\n" +
                        "    sf_modified_at TIMESTAMP NOT NULL,\n" +
                        "    resolved_at TIMESTAMP,\n" +
                        "    bounces INTEGER,\n" +
                        "    hops INTEGER,\n" +
                        "    ingested_at TIMESTAMP NOT NULL,\n" +
                        "    created_at TIMESTAMP DEFAULT current_timestamp,\n" +
                        "    modified_at TIMESTAMP DEFAULT current_timestamp,\n" +
                        "    UNIQUE (case_id,integration_id,ingested_at)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + CASES_TABLE + "_ingested_at_idx " +
                        "on " + company + "." + CASES_TABLE + "(ingested_at)");

        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
