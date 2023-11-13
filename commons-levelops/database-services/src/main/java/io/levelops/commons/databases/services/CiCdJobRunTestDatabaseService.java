package io.levelops.commons.databases.services;

import io.levelops.commons.databases.converters.DbCiCdConverters;
import io.levelops.commons.databases.models.database.CiCdJobRunTest;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobRunTestsFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.parsers.CiCdFilterParserCommons;
import io.levelops.commons.databases.services.parsers.CiCdJobRunTestFilterParser;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.databases.utils.TeamUtils;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.CiCdUtils.getListSortBy;
import static io.levelops.commons.databases.models.filters.CiCdUtils.lowerOf;
import static io.levelops.commons.databases.models.filters.CiCdUtils.parseSortBy;
import static io.levelops.commons.databases.services.CiCdAggsService.CICD_APPLICATIONS;
import static io.levelops.commons.helper.organization.OrgUnitHelper.newOUConfigForStacks;
import static java.lang.String.join;

@Log4j2
@Service
public class CiCdJobRunTestDatabaseService extends DatabaseService<CiCdJobRunTest> {

    public static final String CICD_JOB_RUNS_TESTS = "cicd_job_runs_tests";
    public static final String CICD_JOB_RUNS = "cicd_job_runs";
    public static final String CICD_JOBS = "cicd_jobs";
    public static final String CICD_INSTANCES = "cicd_instances";

    private static final Set<CiCdJobRunTestsFilter.DISTINCT> SUPPORTED_STACKS = Set.of(
            CiCdJobRunTestsFilter.DISTINCT.job_status,
            CiCdJobRunTestsFilter.DISTINCT.job_name,
            CiCdJobRunTestsFilter.DISTINCT.job_run_id,
            CiCdJobRunTestsFilter.DISTINCT.job_run_number,
            CiCdJobRunTestsFilter.DISTINCT.cicd_user_id,
            CiCdJobRunTestsFilter.DISTINCT.test_suite,
            CiCdJobRunTestsFilter.DISTINCT.test_status,
            CiCdJobRunTestsFilter.DISTINCT.instance_name,
            CiCdJobRunTestsFilter.DISTINCT.project_name
    );

    private final NamedParameterJdbcTemplate template;
    private final CiCdJobRunTestFilterParser ciCdJobRunTestFilterParser;
    private final CiCdFilterParserCommons ciCdFilterParserCommons;

    @Autowired
    public CiCdJobRunTestDatabaseService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        ProductsDatabaseService productsDatabaseService = new ProductsDatabaseService(dataSource, DefaultObjectMapper.get());
        this.ciCdFilterParserCommons = new CiCdFilterParserCommons(productsDatabaseService);
        this.ciCdJobRunTestFilterParser = new CiCdJobRunTestFilterParser();
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(CiCdJobRunsDatabaseService.class);
    }

    public List<String> batchInsert(String company, List<CiCdJobRunTest> tests) {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            String insertSql = "INSERT INTO " + company + "." + CICD_JOB_RUNS_TESTS + "(cicd_job_run_id, test_suite, " +
                    "test_name, status, duration, error_details, error_stacktrace)" +
                    " VALUES(?,?,?,?,?,?,?) ON CONFLICT(cicd_job_run_id, test_name) DO NOTHING RETURNING id";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                List<String> ids = new ArrayList<>();
                int batchSize = 0;
                for (CiCdJobRunTest test : tests) {
                    int paramIndex = 1;
                    insertStmt.setObject(paramIndex++, UUID.fromString(test.getCicdJobRunId()));
                    insertStmt.setObject(paramIndex++, test.getTestSuite());
                    insertStmt.setObject(paramIndex++, test.getTestName());
                    insertStmt.setObject(paramIndex++, test.getStatus().name());
                    insertStmt.setObject(paramIndex++, test.getDuration());
                    insertStmt.setObject(paramIndex++, test.getErrorDetails());
                    insertStmt.setObject(paramIndex, test.getErrorStackTrace());
                    insertStmt.addBatch();
                    insertStmt.clearParameters();
                    batchSize++;
                    if (batchSize % 100 == 0) {
                        insertStmt.executeBatch();
                        try (ResultSet rs = insertStmt.getGeneratedKeys()) {
                            while (rs.next()) {
                                ids.add(rs.getString("id"));
                            }
                        }
                    }
                }
                if (batchSize % 100 != 0) {
                    insertStmt.executeBatch();
                    try (ResultSet rs = insertStmt.getGeneratedKeys()) {
                        while (rs.next()) {
                            ids.add(rs.getString("id"));
                        }
                    }
                }
                return ids;
            }
        }));
    }

    @Override
    public String insert(String company, CiCdJobRunTest test) throws SQLException {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            String insertSql = "INSERT INTO " + company + "." + CICD_JOB_RUNS_TESTS + "(cicd_job_run_id, test_suite, " +
                    "test_name, status, duration, error_details, error_stacktrace)" +
                    " VALUES(?,?,?,?,?,?,?) ON CONFLICT(cicd_job_run_id, test_name) DO NOTHING";

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertStmt.setObject(i++, UUID.fromString(test.getCicdJobRunId()));
                insertStmt.setObject(i++, test.getTestSuite());
                insertStmt.setObject(i++, test.getTestName());
                insertStmt.setObject(i++, test.getStatus().name());
                insertStmt.setObject(i++, test.getDuration());
                insertStmt.setObject(i++, test.getErrorDetails());
                insertStmt.setObject(i, test.getErrorStackTrace());
                insertStmt.executeUpdate();
                String id;
                try (ResultSet rs = insertStmt.getGeneratedKeys()) {
                    if (rs.next())
                        id = rs.getString(1);
                    else {
                        throw new SQLException("Failed to get inserted row id");
                    }
                }
                return id;
            }
        }));
    }

    @Override
    public Boolean update(String company, CiCdJobRunTest t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<CiCdJobRunTest> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    @Override
    public DbListResponse<CiCdJobRunTest> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        throw new UnsupportedOperationException();
    }

    private String getSubQuery(String company, Map<String, Object> params, CiCdJobRunTestsFilter filter, String innerSelect,
                               Boolean isList, OUConfiguration ouConfig) {
        Map<Integer, Map<String, Object>> productFilters = null;
        try {
            productFilters = ciCdFilterParserCommons.getProductFilters(company, filter.getOrgProductsIds());
        } catch (SQLException throwables) {
            log.error("Error encountered while fetching product filters for company " + company, throwables);
        }
        if (productFilters == null || MapUtils.isEmpty(productFilters)) {
            List<String> conditions = createWhereClauseAndUpdateParams(company, params, filter, null, ouConfig);
            return ciCdJobRunTestFilterParser.getSqlStmt(company, conditions, innerSelect, isList, filter);
        } else {
            AtomicInteger suffix = new AtomicInteger();
            Map<Integer, Map<String, Object>> integFiltersMap = productFilters;
            List<String> filterSqlStmts = CollectionUtils.emptyIfNull(productFilters.keySet()).stream()
                    .map(integrationId -> {
                        CiCdJobRunTestsFilter newJobConfigFilter = ciCdJobRunTestFilterParser.merge(integrationId, filter, integFiltersMap.get(integrationId));
                        List<String> conditions = createWhereClauseAndUpdateParams(company, params, newJobConfigFilter,
                                String.valueOf(suffix.incrementAndGet()), ouConfig);
                        return ciCdJobRunTestFilterParser.getSqlStmt(company, conditions, innerSelect, isList, newJobConfigFilter);
                    })
                    .collect(Collectors.toList());
            return join(" UNION ", filterSqlStmts);
        }
    }


    public DbListResponse<CiCdJobRunTest> list(String company,
                                               CiCdJobRunTestsFilter filter,
                                               Map<String, SortingOrder> sortBy,
                                               Integer pageNumber,
                                               Integer pageSize) {
        return list(company, filter.toBuilder().sortBy(sortBy).build(), pageNumber, pageSize);
    }
    public DbListResponse<CiCdJobRunTest> list(String company,
                                               CiCdJobRunTestsFilter filter,
                                               Integer pageNumber,
                                               Integer pageSize) {
        return list(company, filter, pageNumber, pageSize, null);
    }

    public DbListResponse<CiCdJobRunTest> list(String company,
                                               CiCdJobRunTestsFilter filter,
                                               Integer pageNumber,
                                               Integer pageSize,
                                               OUConfiguration ouConfig) {
        Map<String, Object> params = new HashMap<>();
        params.put("offset", pageNumber * pageSize);
        params.put("limit", pageSize);
        String limitClause = " OFFSET :offset LIMIT :limit";
        List<String> orderBy = getListSortBy(filter.getSortBy());
        String nullsPosition = SortingOrder.getNullsPosition(SortingOrder.fromString(orderBy.get(0).split(" ")[1]));
        String subQuery = getSubQuery(company, params, filter, null, true, ouConfig);
        String query = "SELECT * FROM ( " + subQuery + ") final ORDER BY " + String.join(",", orderBy) + " NULLS " + nullsPosition + " " + limitClause;
        log.info("sql {}", query);
        final List<CiCdJobRunTest> tests = template.query(query, params, DbCiCdConverters.jobRunTestsListMapper());
        String countQuery = "SELECT count(*) FROM (" + subQuery + ") final ";
        log.info("count sql {}", countQuery);
        final Integer count = template.queryForObject(countQuery, params, Integer.class);
        return DbListResponse.of(tests, count);
    }

    public DbListResponse<DbAggregationResult> stackedGroupBy(String company, CiCdJobRunTestsFilter filter,
                                                              List<CiCdJobRunTestsFilter.DISTINCT> stacks, Map<String, SortingOrder> sortBy,
                                                              OUConfiguration ouConfig) throws SQLException {
        return stackedGroupBy(company, filter.toBuilder().sortBy(sortBy).build(), stacks, ouConfig);
    }

    public DbListResponse<DbAggregationResult> stackedGroupBy(String company, CiCdJobRunTestsFilter filter,
                                                              List<CiCdJobRunTestsFilter.DISTINCT> stacks) throws SQLException {
        return stackedGroupBy(company, filter, stacks, null);
    }

    public DbListResponse<DbAggregationResult> stackedGroupBy(String company, CiCdJobRunTestsFilter filter,
                                                              List<CiCdJobRunTestsFilter.DISTINCT> stacks,
                                                              OUConfiguration ouConfig) throws SQLException {
        DbListResponse<DbAggregationResult> result = groupByAndCalculate(company, filter, filter.getSortBy(), ouConfig);
        if (stacks == null
                || stacks.size() == 0
                || !SUPPORTED_STACKS.contains(stacks.get(0))
                || !SUPPORTED_STACKS.contains(filter.getDISTINCT()))
            return result;
        CiCdJobRunTestsFilter.DISTINCT stack = stacks.get(0);
        OUConfiguration ouConfigForStacks = ouConfig;
        List<DbAggregationResult> finalList = new ArrayList<>();
        CiCdJobRunTestsFilter newFilter;
        for (DbAggregationResult row : result.getRecords()) {
            switch (filter.getDISTINCT()) {
                case job_name:
                    newFilter = filter.toBuilder().DISTINCT(stack).jobNames(List.of(row.getKey())).build();
                    break;
                case job_status:
                    newFilter = filter.toBuilder().DISTINCT(stack).jobStatuses(List.of(row.getKey())).build();
                    break;
                case job_run_id:
                    newFilter = filter.toBuilder().DISTINCT(stack).jobRunIds(List.of(row.getKey())).build();
                    break;
                case job_run_number:
                    newFilter = filter.toBuilder().DISTINCT(stack).jobRunNumbers(List.of(row.getKey())).build();
                    break;
                case cicd_user_id:
                    newFilter = filter.toBuilder().DISTINCT(stack).cicdUserIds(List.of(row.getKey())).build();
                    ouConfigForStacks = newOUConfigForStacks(ouConfig, "cicd_user_ids");
                    break;
                case test_suite:
                    newFilter = filter.toBuilder().DISTINCT(stack).testSuites(List.of(row.getKey())).build();
                    break;
                case test_status:
                    newFilter = filter.toBuilder().DISTINCT(stack).testStatuses(List.of(row.getKey())).build();
                    break;
                case instance_name:
                    newFilter = filter.toBuilder().DISTINCT(stack).instanceNames(List.of(row.getKey())).build();
                    break;
                case project_name:
                    newFilter = filter.toBuilder().DISTINCT(stack).projects(List.of(row.getKey())).build();
                    break;
                default:
                    throw new UnsupportedOperationException("Stack is not supported for:" + stack);
            }
            newFilter = newFilter.toBuilder().sortBy(Map.of(stack.name(), SortingOrder.ASC)).build();
            finalList.add(row.toBuilder().stacks(groupByAndCalculate(company, newFilter, ouConfigForStacks).getRecords()).build());
        }
        return DbListResponse.of(finalList, finalList.size());
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company,
                                                                   CiCdJobRunTestsFilter filter,
                                                                   Map<String, SortingOrder> sortBy) throws SQLException {
        return groupByAndCalculate(company, filter.toBuilder().sortBy(sortBy).build());
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company,
                                                                   CiCdJobRunTestsFilter filter,
                                                                   Map<String, SortingOrder> sortBy,
                                                                   OUConfiguration ouConfig) throws SQLException {
        return groupByAndCalculate(company, filter.toBuilder().sortBy(sortBy).build(), ouConfig);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company,
                                                                   CiCdJobRunTestsFilter filter) throws SQLException {
        return groupByAndCalculate(company, filter, (OUConfiguration) null);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company,
                                                                   CiCdJobRunTestsFilter filter,
                                                                   OUConfiguration ouConfig) throws SQLException {
        Map<String, Object> params = new HashMap<>();
        final CiCdJobRunTestsFilter.DISTINCT DISTINCT = filter.getDISTINCT();
        String key;
        String groupBySql;
        String orderBySql = "";
        String innerSelect = "";
        Set<String> orderByStrings = new HashSet<>();
        Map<String, SortingOrder> sortBy = filter.getSortBy();
        switch (DISTINCT) {
            case job_status:
            case job_name:
            case cicd_user_id:
            case test_suite:
                key = DISTINCT.name();
                groupBySql = " GROUP BY " + DISTINCT.name();
                parseSortBy(DISTINCT.toString(), orderByStrings, sortBy, lowerOf(key));
                break;
            case job_run_number:
                key = DISTINCT.name();
                groupBySql = " GROUP BY " + DISTINCT.name();
                parseSortBy(DISTINCT.toString(), orderByStrings, sortBy, key);
                break;
            case instance_name:
                key = "name";
                groupBySql = " GROUP BY name";
                parseSortBy(DISTINCT.toString(), orderByStrings, sortBy, lowerOf(key));
                break;
            case job_run_id:
                key = "cicd_job_run_id";
                groupBySql = " GROUP BY cicd_job_run_id";
                parseSortBy(DISTINCT.toString(), orderByStrings, sortBy, key);
                break;
            case test_status:
                key = "status";
                groupBySql = " GROUP BY status";
                parseSortBy(DISTINCT.toString(), orderByStrings, sortBy, lowerOf(key));
                break;
            case trend:
                key = "trend";
                groupBySql = " GROUP BY trend";
                parseSortBy(DISTINCT.toString(), orderByStrings, sortBy, key);
                break;
            case project_name:
                key = DISTINCT.toString();
                groupBySql = "GROUP BY " + DISTINCT.toString();
                parseSortBy(DISTINCT.toString(), orderByStrings, sortBy, lowerOf(key));
                break;
            case job_end:
                AggTimeQueryHelper.AggTimeQuery trendAggQuery =
                        AggTimeQueryHelper.getAggTimeQuery("end_time", DISTINCT.toString(), filter.getAggInterval() != null ? filter.getAggInterval().toString() : null, false);
                innerSelect = trendAggQuery.getHelperColumn().replaceFirst(",", "");
                groupBySql = "GROUP BY " + trendAggQuery.getGroupBy();
                key = trendAggQuery.getSelect();
                parseSortBy(DISTINCT.toString(), orderByStrings, sortBy, "job_end");
                break;
            default:
                throw new SQLException("Unsupported across: " + DISTINCT.toString());
        }
        String aggSql;
        switch (filter.getCALCULATION()) {
            case duration:
                aggSql = " MIN(duration) as mn, MAX(duration) as mx, COUNT(id) as ct," +
                        " PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY duration) as md";
                parseSortBy(filter.getCALCULATION().toString(), orderByStrings, sortBy, "md", true);
                break;
            case count:
            default:
                aggSql = "COUNT(id) as ct";
                parseSortBy(filter.getCALCULATION().toString(), orderByStrings, sortBy, "ct", true);
                break;
        }
        if (MapUtils.isNotEmpty(sortBy) && orderByStrings.isEmpty()) {
            if (!sortBy.keySet().stream().findFirst().get().equals(DISTINCT.toString()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field ");
        }
        String subQuery = getSubQuery(company, params, filter, innerSelect, false, ouConfig);
        orderBySql = String.join(",", orderByStrings);
        String nullsPosition = SortingOrder.getNullsPosition(SortingOrder.fromString(orderBySql.split(" ")[1]));
        String aggQuery = "SELECT " + aggSql + "," + key + " FROM (" + subQuery + ") final_table " +
                groupBySql + " ORDER BY " + orderBySql + " NULLS " + nullsPosition;
        log.info("sql {}", aggQuery);
        final List<DbAggregationResult> aggregationResults = template.query(aggQuery, params,
                DbCiCdConverters.cicdTestsAggRowMapper(filter.getDISTINCT() == CiCdJobRunTestsFilter.DISTINCT.job_end ? filter.getDISTINCT().toString() : key, filter.getCALCULATION()));
        return DbListResponse.of(aggregationResults, aggregationResults.size());
    }

    protected List<String> createWhereClauseAndUpdateParams(String company, Map<String, Object> params,
                                                            CiCdJobRunTestsFilter filter, String suffix,
                                                            OUConfiguration ouConfig) {
        String paramSuffix = suffix == null ? "" : "_" + suffix;
        List<String> conditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(filter.getCicdUserIds()) || OrgUnitHelper.doesOuConfigHaveCiCdUsers(ouConfig)) { // OU: user
            var columnName = "r.cicd_user_id" + paramSuffix;
            var columnNameParam = columnName + paramSuffix;
            if(OrgUnitHelper.doesOuConfigHaveCiCdUsers(ouConfig)){
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(
                        company, ouConfig, params, IntegrationType.getCICDIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    conditions.add(MessageFormat.format("{0} IN (SELECT cloud_id FROM ({1}) l)", columnName, usersSelect));
                }
            }
            else if(CollectionUtils.isNotEmpty(filter.getCicdUserIds())){
                TeamUtils.addUsersCondition(company, conditions, params, "r.cicd_user_id",
                        columnNameParam, false, filter.getCicdUserIds(), CICD_APPLICATIONS);
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCiCdUserIds())) {
            String columnNameParam = "r.cicd_user_id" + paramSuffix;
            TeamUtils.addUsersCondition(company, conditions, params, "r.cicd_user_id",
                    columnNameParam, false, filter.getExcludeCiCdUserIds(), CICD_APPLICATIONS, true);
        }
        if (filter.getStartTimeRange() != null) {
            ImmutablePair<Long, Long> startTimeRange = filter.getStartTimeRange();
            if (startTimeRange.getLeft() != null) {
                conditions.add("r.start_time > TO_TIMESTAMP(:start_time_start" + paramSuffix + ")");
                params.put("start_time_start" + paramSuffix, startTimeRange.getLeft());
            }
            if (startTimeRange.getRight() != null) {
                conditions.add("r.start_time < TO_TIMESTAMP(:start_time_end" + paramSuffix + ")");
                params.put("start_time_end" + paramSuffix, startTimeRange.getRight());
            }
        }

        if (filter.getEndTimeRange() != null) {
            ImmutablePair<Long, Long> endTimeRange = filter.getEndTimeRange();
            if (endTimeRange.getLeft() != null) {
                conditions.add("r.end_time > to_timestamp(:end_time_start" + paramSuffix + ")");
                params.put("end_time_start" + paramSuffix, endTimeRange.getLeft());
            }
            if (endTimeRange.getRight() != null) {
                conditions.add("r.end_time < to_timestamp(:end_time_end" + paramSuffix + ")");
                params.put("end_time_end" + paramSuffix, endTimeRange.getRight());
            }
        }

        if (CollectionUtils.isNotEmpty(filter.getJobNames())) {
            conditions.add("j.job_name IN (:job_names" + paramSuffix + ")");
            params.put("job_names" + paramSuffix, filter.getJobNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeJobNames())) {
            conditions.add("j.job_name NOT IN (:excl_job_names" + paramSuffix + ")");
            params.put("excl_job_names" + paramSuffix, filter.getExcludeJobNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getProjects())) {
            conditions.add("j.project_name IN (:project_names" + paramSuffix + ")");
            params.put("project_names" + paramSuffix, filter.getProjects());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeProjects())) {
            conditions.add("j.project_name NOT IN (:excl_project_names" + paramSuffix + ")");
            params.put("excl_project_names" + paramSuffix, filter.getExcludeProjects());
        }
        if (CollectionUtils.isNotEmpty(filter.getTypes())) {
            conditions.add("i.type IN (:types" + paramSuffix + ")");
            params.put("types" + paramSuffix, filter.getTypes().stream()
                    .map(CICD_TYPE::toString)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTypes())) {
            conditions.add("i.type NOT IN (:excl_types" + paramSuffix + ")");
            params.put("excl_types" + paramSuffix, filter.getExcludeTypes().stream()
                    .map(CICD_TYPE::toString)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            conditions.add("i.integration_id IN (:integration_ids" + paramSuffix + ")");
            params.put("integration_ids" + paramSuffix, filter.getIntegrationIds().stream()
                    .map(io.levelops.commons.utils.NumberUtils::toInteger).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getJobStatuses())) {
            conditions.add("r.status IN (:job_statuses" + paramSuffix + ")");
            params.put("job_statuses" + paramSuffix, filter.getJobStatuses());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeJobStatuses())) {
            conditions.add("r.status NOT IN (:excl_job_statuses" + paramSuffix + ")");
            params.put("excl_job_statuses" + paramSuffix, filter.getExcludeJobStatuses());
        }
        if (CollectionUtils.isNotEmpty(filter.getTestStatuses())) {
            conditions.add("t.status IN (:test_statuses" + paramSuffix + ")");
            params.put("test_statuses" + paramSuffix, filter.getTestStatuses());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTestStatuses())) {
            conditions.add("t.status NOT IN (:excl_test_statuses" + paramSuffix + ")");
            params.put("excl_test_statuses" + paramSuffix, filter.getExcludeTestStatuses());
        }
        if (CollectionUtils.isNotEmpty(filter.getTestSuites())) {
            conditions.add("t.test_suite IN (:test_suites" + paramSuffix + ")");
            params.put("test_suites" + paramSuffix, filter.getTestSuites());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTestSuites())) {
            conditions.add("t.test_suite NOT IN (:excl_test_suites" + paramSuffix + ")");
            params.put("excl_test_suites" + paramSuffix, filter.getExcludeTestSuites());
        }
        if (CollectionUtils.isNotEmpty(filter.getJobRunNumbers())) {
            conditions.add("r.job_run_number IN (:job_run_numbers" + paramSuffix + ")");
            params.put("job_run_numbers" + paramSuffix, filter.getJobRunNumbers().stream()
                    .map(NumberUtils::toLong)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeJobRunNumbers())) {
            conditions.add("r.job_run_number NOT IN (:excl_job_run_numbers" + paramSuffix + ")");
            params.put("excl_job_run_numbers" + paramSuffix, filter.getExcludeJobRunNumbers().stream()
                    .map(NumberUtils::toLong)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getJobRunIds())) {
            conditions.add("r.id IN (:job_run_ids" + paramSuffix + ")");
            params.put("job_run_ids" + paramSuffix, filter.getJobRunIds().stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeJobRunIds())) {
            conditions.add("r.id NOT IN (:excl_job_run_ids" + paramSuffix + ")");
            params.put("excl_job_run_ids" + paramSuffix, filter.getExcludeJobRunIds().stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getInstanceNames())) {
            conditions.add("i.name IN (:instance_names" + paramSuffix + ")");
            params.put("instance_names" + paramSuffix, filter.getInstanceNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeInstanceNames())) {
            conditions.add("i.name NOT IN (:excl_instance_names" + paramSuffix + ")");
            params.put("excl_instance_names" + paramSuffix, filter.getExcludeInstanceNames());
        }
        if (filter.getDISTINCT() != null && filter.getDISTINCT().equals(CiCdJobRunTestsFilter.DISTINCT.trend)) {
            conditions.add("r.start_time IS NOT NULL");
        }
        return conditions;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        final List<String> ddlStmts = List.of("CREATE TABLE IF NOT EXISTS " + company + "." + CICD_JOB_RUNS_TESTS +
                        " (id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " cicd_job_run_id UUID NOT NULL REFERENCES " +
                        company + "." + CICD_JOB_RUNS + "(id) ON DELETE CASCADE," +
                        " test_suite VARCHAR NOT NULL," +
                        " test_name VARCHAR NOT NULL," +
                        " status VARCHAR," +
                        " duration NUMERIC," +
                        " error_details VARCHAR," +
                        " error_stacktrace VARCHAR," +
                        " UNIQUE(cicd_job_run_id, test_name))",
                "CREATE INDEX IF NOT EXISTS " + CICD_JOB_RUNS_TESTS + "_job_run_id_test_name_compound_idx" +
                        " on " + company + "." + CICD_JOB_RUNS_TESTS + " (cicd_job_run_id, test_name)"
        );
        ddlStmts.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
