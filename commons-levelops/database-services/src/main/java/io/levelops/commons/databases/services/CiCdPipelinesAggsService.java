package io.levelops.commons.databases.services;

import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.converters.DbCiCdPipelineConverters;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CICDJobRunDTO;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobQualifiedName;
import io.levelops.commons.databases.models.filters.CiCdJobRunParameter;
import io.levelops.commons.databases.models.filters.CiCdPipelineJobRunsFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.DbAggregationResultStacksWrapper;
import io.levelops.commons.databases.models.response.DbAggregationResultsMerger;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.parsers.CiCdFilterParserCommons;
import io.levelops.commons.databases.services.parsers.CiCdPipelineFilterParser;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.databases.utils.CriteriaUtils;
import io.levelops.commons.databases.utils.TeamUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.NumberUtils;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.databases.models.filters.CiCdUtils.getImmutablePair;
import static io.levelops.commons.databases.models.filters.CiCdUtils.getListSortBy;
import static io.levelops.commons.databases.models.filters.CiCdUtils.lowerOf;
import static io.levelops.commons.databases.models.filters.CiCdUtils.parseSortBy;
import static io.levelops.commons.databases.services.CiCdAggsService.CICD_PARTIAL_MATCH_COLUMNS;

@Log4j2
@Service
public class CiCdPipelinesAggsService extends DatabaseService<CICDJobRunDTO> {

    private static final Set<String> CICD_PIPELINE_PARTIAL_MATCH_COLUMNS = Set.of("job_normalized_full_name");
    private static final boolean LIST_FILTER = true;
    private static final boolean AGGS_FILTER = false;

    private final NamedParameterJdbcTemplate template;
    private final CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private final CiCdPipelineFilterParser ciCdPipelineFilterParser;
    private ProductsDatabaseService productsDatabaseService;
    private final CiCdFilterParserCommons ciCdFilterParserCommons;
    private final CiCdMetadataConditionBuilder ciCdMetadataConditionBuilder;
    private final CiCdPartialMatchConditionBuilder ciCdPartialMatchConditionBuilder;

    // region CSTOR
    @Autowired
    public CiCdPipelinesAggsService(DataSource dataSource, CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        this.productsDatabaseService = new ProductsDatabaseService(dataSource, DefaultObjectMapper.get());
        this.ciCdFilterParserCommons = new CiCdFilterParserCommons(productsDatabaseService);
        this.ciCdJobRunsDatabaseService = ciCdJobRunsDatabaseService;
        this.ciCdPipelineFilterParser = new CiCdPipelineFilterParser(DefaultObjectMapper.get());
        this.ciCdMetadataConditionBuilder = new CiCdMetadataConditionBuilder();
        this.ciCdPartialMatchConditionBuilder = new CiCdPartialMatchConditionBuilder();
    }
    // endregion

    // region Unused
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(CiCdJobsDatabaseService.class, CiCdJobRunsDatabaseService.class);
    }

    @Override
    public String insert(String company, CICDJobRunDTO t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean update(String company, CICDJobRunDTO t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<CICDJobRunDTO> get(String company, String jobRunId) throws SQLException {
        var results = listCiCdJobRuns(company, CiCdPipelineJobRunsFilter.builder().jobRunId(jobRunId).build(), 0, 1, false, true, null);
        if (results == null || results.getCount() < 1) {
            return Optional.empty();
        }
        return Optional.of(results.getRecords().get(0));
    }

    @Override
    public DbListResponse<CICDJobRunDTO> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return null;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        return true;
    }
    // endregion

    // region Job Runs
    // region Job Runs - Aggs


    private String getSubQuery(String company, Map<String, Object> params, CiCdPipelineJobRunsFilter filter,
                               DbListResponse<DbAggregationResult> resultsWithoutStacks, Boolean fullDetails, Boolean singleJobRun,
                               Boolean isList, Boolean hasStacks,
                               final OUConfiguration ouConfig) {
        Map<Integer, Map<String, Object>> productFilters = null;
        try {
            productFilters = ciCdFilterParserCommons.getProductFilters(company, filter.getOrgProductsIds());
        } catch (SQLException throwables) {
            log.error("Error encountered while fetching product filters for company " + company, throwables);
        }
        if (productFilters == null || MapUtils.isEmpty(productFilters)) {
            List<String> conditions;
            List<String> unionConditions = null;
            if (isList) {
                conditions = createWhereClauseAndUpdateParamsJobRuns(company, filter, params, LIST_FILTER, null, true, ouConfig);
                unionConditions = createWhereClauseAndUpdateParamsJobRuns(company, filter, params, LIST_FILTER, true, null, true, ouConfig);
                if (singleJobRun && Strings.isNotBlank(filter.getJobRunId())) {
                    unionConditions.add("r.id = :union_job_run_id::uuid ");
                    params.put("union_job_run_id", filter.getJobRunId().trim());
                }
            } else {
                conditions = createWhereClauseAndUpdateParamsJobRuns(company, filter, params, AGGS_FILTER, null, isList, ouConfig);
            }
            if (hasStacks) {
                applyAcrossToStacks(filter, resultsWithoutStacks, params, conditions);
            }
            return ciCdPipelineFilterParser.getSqlStmt(company, conditions, unionConditions, fullDetails, isList);
        }
        AtomicInteger suffix = new AtomicInteger();
        Map<Integer, Map<String, Object>> integFiltersMap = productFilters;
        List<String> filterSqlStmts = CollectionUtils.emptyIfNull(productFilters.keySet()).stream()
                .map(integrationId -> {
                    CiCdPipelineJobRunsFilter newPipelineFilter = ciCdPipelineFilterParser.merge(integrationId, filter, integFiltersMap.get(integrationId));
                    List<String> conditions;
                    List<String> unionConditions = null;
                    if (isList) {
                        conditions = createWhereClauseAndUpdateParamsJobRuns(company, newPipelineFilter, params, LIST_FILTER, String.valueOf(suffix.incrementAndGet()), true, ouConfig);
                        unionConditions = createWhereClauseAndUpdateParamsJobRuns(company, newPipelineFilter, params, LIST_FILTER, true, String.valueOf(suffix.incrementAndGet()), true, ouConfig);
                        if (singleJobRun && Strings.isNotBlank(newPipelineFilter.getJobRunId())) {
                            unionConditions.add("r.id = :union_job_run_id_" + suffix + "::uuid ");
                            params.put("union_job_run_id_" + suffix, newPipelineFilter.getJobRunId().trim());
                        }
                    } else {
                        conditions = createWhereClauseAndUpdateParamsJobRuns(company, newPipelineFilter, params, AGGS_FILTER,
                                String.valueOf(suffix.incrementAndGet()), isList, ouConfig);
                    }
                    if (hasStacks) {
                        applyAcrossToStacks(newPipelineFilter, resultsWithoutStacks, params, conditions);
                    }
                    return ciCdPipelineFilterParser.getSqlStmt(company, conditions, unionConditions, fullDetails, isList);
                })
                .collect(Collectors.toList());
        return String.join(" UNION ", filterSqlStmts);
    }


    public DbListResponse<DbAggregationResult> groupByAndCalculateCiCdJobRunsWithoutStacks(String company,
                                                                                           CiCdPipelineJobRunsFilter filter,
                                                                                           Map<String, SortingOrder> sortBy,
                                                                                           boolean valuesOnly,
                                                                                           final OUConfiguration ouConfig) {
        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        CiCdPipelineJobRunsFilter.CALCULATION calculation = CiCdPipelineJobRunsFilter.getSanitizedCalculation(filter);

        StringBuilder orderByStringBuilder = new StringBuilder();
        StringBuffer calculationComponentStringBuffer = new StringBuffer();
        Set<String> outerSelects = new HashSet<>();
        Set<String> innerSelects = new HashSet<>();
        Set<String> groupByStrings = new HashSet<>();
        Set<String> orderByStrings = new HashSet<>();

        //Calculation should be parsed before across as orderBy could get overwritten in parse across
        parseCiCdJobRunsCalculation(calculation, calculationComponentStringBuffer, sortBy, orderByStrings);

        parseCiCdJobRunsAcrossOrStack(filter.getAcross(), innerSelects, outerSelects, filter.getAggInterval(), groupByStrings, sortBy, orderByStrings);

        String outerSelect = String.join(",", outerSelects);
        String innerSelect = String.join(",", innerSelects);
        String groupByString = String.join(",", groupByStrings);

        Map<String, Object> params = new HashMap<>();

        int acrossCount = (filter.getAcrossCount() != null) ? filter.getAcrossCount() : 90;
        boolean acrossTimeInterval = (filter.getAcross() == CiCdPipelineJobRunsFilter.DISTINCT.job_end) ||
                (filter.getAcross() == CiCdPipelineJobRunsFilter.DISTINCT.trend);

        String subQuery = getSubQuery(company, params, filter, null, true, false, false, false, ouConfig);
        String limit = (valuesOnly) ? "" : " LIMIT " + acrossCount;

        orderByStringBuilder.append(String.join(",", orderByStrings));

        String nullsPosition = SortingOrder.getNullsPosition(SortingOrder.fromString(orderByStringBuilder.toString().split(" ")[1]));
        String sql = "WITH RECURSIVE cte_BOM AS (" + "\n" + subQuery +
                " UNION ALL" + "\n" +
                " SELECT DISTINCT(r.id) as run_id, cte_BOM.top_job_id, j.job_full_name, r.job_run_number" + "\n" +
                " FROM " + company + ".cicd_job_runs AS r" + "\n" +
                " JOIN " + company + ".cicd_jobs AS j ON r.cicd_job_id = j.id" + "\n" +
                " LEFT OUTER JOIN " + company + ".cicd_job_run_triggers as t on t.cicd_job_run_id = r.id AND t.type = \'UpstreamCause\'" + "\n" +
                " JOIN cte_BOM ON cte_BOM.job_full_name = t.trigger_id AND cte_BOM.job_run_number = t.job_run_number" + "\n" +
                ")" + "\n" +
                (acrossTimeInterval ? " SELECT " + outerSelect + "," + calculationComponentStringBuffer.toString() + "\n" +
                        " FROM (SELECT r.*," + innerSelect + "\n" :
                        " SELECT " + innerSelect + "," + calculationComponentStringBuffer.toString() + "\n") +
                " FROM cte_BOM" + "\n" +
                " JOIN " + company + ".cicd_job_runs as r on cte_BOM.run_id = r.id" + "\n" +
                " JOIN " + company + ".cicd_jobs AS j ON cte_BOM.top_job_id = j.id" + "\n" +
                " LEFT OUTER JOIN " + company + ".cicd_instances AS i ON j.cicd_instance_id = i.id " + (acrossTimeInterval ? ") r" : "") + "\n" +
                " GROUP BY " + groupByString + " ORDER BY " + orderByStringBuilder.toString() + " NULLS " + nullsPosition +
                limit;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        List<DbAggregationResult> results = template.query(sql, params, DbCiCdPipelineConverters.distinctJobRunsAggsMapper(
                filter.getAcross(), filter.getCalculation()));
        return DbListResponse.of(results, results.size());
    }

    private void parseCiCdJobRunsAcrossOrStack(CiCdPipelineJobRunsFilter.DISTINCT acrossOrStack, Set<String> innerSelects,
                                               Set<String> outerSelects, CICD_AGG_INTERVAL aggInterval,
                                               Set<String> groupByStrings, Map<String, SortingOrder> sortBy, Set<String> orderByString) {
        boolean sortAscending = false;
        switch (acrossOrStack) {
            case job_status:
                innerSelects.add("status");
                groupByStrings.add("status");
                outerSelects.add("status");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("status"));
                break;
            case qualified_job_name:
                innerSelects.add("i.name as instance_name");
                innerSelects.add("job_name");
                outerSelects.add("job_name");
                outerSelects.add("instance_name");
                groupByStrings.add("instance_name");
                groupByStrings.add("job_name");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("job_name"));
                break;
            case cicd_job_id:
                innerSelects.add("j.id as cicd_job_id");
                innerSelects.add("i.name as instance_name");
                innerSelects.add("job_name");
                groupByStrings.add("j.id");
                groupByStrings.add("instance_name");
                groupByStrings.add("job_name");
                outerSelects.add("cicd_job_id");
                outerSelects.add("instance_name");
                outerSelects.add("job_name");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("job_name"));
                break;
            case instance_name:
                innerSelects.add("i.id as instance_id");
                innerSelects.add("i.name as instance_name");
                outerSelects.add("instance_name");
                outerSelects.add("instance_id");
                groupByStrings.add("instance_name");
                groupByStrings.add("instance_id");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("name"));
                break;
            case project_name:
            case job_name:
            case cicd_user_id:
            case job_normalized_full_name:
                innerSelects.add(acrossOrStack.toString());
                groupByStrings.add(acrossOrStack.toString());
                outerSelects.add(acrossOrStack.toString());
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf(acrossOrStack.toString()));
                break;
            case trend:
                if(MapUtils.isNotEmpty(sortBy))
                    sortAscending = sortBy.get(acrossOrStack.toString()).equals(SortingOrder.ASC);
                AggTimeQueryHelper.AggTimeQuery trendStartAggQuery =
                        AggTimeQueryHelper.getAggTimeQuery("start_time", acrossOrStack.toString(), aggInterval != null ?
                                aggInterval.toString() : null, false, sortAscending);
                innerSelects.add(trendStartAggQuery.getHelperColumn().replaceFirst(",", ""));
                groupByStrings.add(trendStartAggQuery.getGroupBy());
                outerSelects.add(trendStartAggQuery.getSelect());
                orderByString.add(trendStartAggQuery.getOrderBy());
                break;
            case job_end:
                if(MapUtils.isNotEmpty(sortBy))
                    sortAscending = sortBy.get(acrossOrStack.toString()).equals(SortingOrder.ASC);
                AggTimeQueryHelper.AggTimeQuery trendStartAggQueryJobEnd =
                        AggTimeQueryHelper.getAggTimeQuery("end_time", acrossOrStack.toString(), aggInterval != null ?
                                aggInterval.toString() : null, false, sortAscending);
                innerSelects.add(trendStartAggQueryJobEnd.getHelperColumn().replaceFirst(",", ""));
                groupByStrings.add(trendStartAggQueryJobEnd.getGroupBy());
                outerSelects.add(trendStartAggQueryJobEnd.getSelect());
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, "job_end");
                break;
            default:
                Validate.notNull(null, "Invalid across or stack field provided.");
        }
        if (MapUtils.isNotEmpty(sortBy) && orderByString.isEmpty()) {
            if (!sortBy.keySet().stream().findFirst().get().equals(acrossOrStack.toString()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field ");
        }
    }

    private void parseCiCdJobRunsCalculation(CiCdPipelineJobRunsFilter.CALCULATION calculation, StringBuffer calculationComponentStringBuffer, Map<String, SortingOrder> sortBy, Set<String> orderByString) {
        switch (calculation) {
            case duration:
                calculationComponentStringBuffer.append("min(duration) as mn, max(duration) as mx, sum(duration) as sm, count(*) as ct,"
                        + " PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY duration) as md");
                parseSortBy(calculation.toString(), orderByString, sortBy, "md", true);
                break;
            default:
                calculationComponentStringBuffer.append("COUNT(r.id) as ct");
                parseSortBy(calculation.toString(), orderByString, sortBy, "ct", true);
                break;
        }
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateCiCdJobRuns(String company,
                                                                              CiCdPipelineJobRunsFilter filter,
                                                                              Map<String, SortingOrder> sortBy,
                                                                              boolean valuesOnly,
                                                                              final OUConfiguration ouConfig) {
        return groupByAndCalculateCiCdJobRuns(company, filter.toBuilder().sortBy(sortBy).build(), valuesOnly, ouConfig);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateCiCdJobRuns(String company,
                                                                              CiCdPipelineJobRunsFilter filter,
                                                                              boolean valuesOnly,
                                                                              final OUConfiguration ouConfig) {
        Map<String, SortingOrder> sortBy = filter.getSortBy();
        DbListResponse<DbAggregationResult> resultsWithoutStacks = groupByAndCalculateCiCdJobRunsWithoutStacks(company, filter, sortBy, valuesOnly, ouConfig);
        if (CollectionUtils.isEmpty(resultsWithoutStacks.getRecords())) {
            return resultsWithoutStacks;
        }
        if (CollectionUtils.isEmpty(filter.getStacks())) {
            return resultsWithoutStacks;
        }
        if(filter.getAcross().equals(CiCdPipelineJobRunsFilter.DISTINCT.trend) ||
                filter.getAcross().equals(CiCdPipelineJobRunsFilter.DISTINCT.job_end)) {
            return groupByAndCalculateDateIntervals(company, filter, resultsWithoutStacks, ouConfig);
        }
        CiCdPipelineJobRunsFilter.CALCULATION calculation = CiCdPipelineJobRunsFilter.getSanitizedCalculation(filter);

        StringBuilder orderByStringBuilder = new StringBuilder();
        StringBuffer calculationComponentStringBuffer = new StringBuffer();
        Set<String> innerSelects = new HashSet<>();
        Set<String> outerSelects = new HashSet<>();
        Set<String> groupByStrings = new HashSet<>();
        Set<String> orderByStrings = new HashSet<>();
        //Calculation should be parsed before across as orderBy could get overwritten in parse across
        parseCiCdJobRunsCalculation(calculation, calculationComponentStringBuffer, sortBy, orderByStrings);

        parseCiCdJobRunsAcrossOrStack(filter.getAcross(), innerSelects, outerSelects, filter.getAggInterval(), groupByStrings, sortBy, orderByStrings);
        if (CollectionUtils.isNotEmpty(filter.getStacks())) {
            for (CiCdPipelineJobRunsFilter.DISTINCT stack : filter.getStacks()) {
                // For stack do not send orderBy - orderBy is overwritten only for across, not for stacks
                parseCiCdJobRunsAcrossOrStack(stack, innerSelects, outerSelects, filter.getAggInterval(), groupByStrings, sortBy, orderByStrings);
            }
        }
        boolean acrossTimeInterval = (filter.getAcross().equals(CiCdPipelineJobRunsFilter.DISTINCT.job_end))
                || (filter.getAcross().equals(CiCdPipelineJobRunsFilter.DISTINCT.trend));
        if (acrossTimeInterval && groupByStrings.contains("j.id")) {
            groupByStrings.remove("j.id");
            groupByStrings.add("cicd_job_id");
        }
        String outerSelect = String.join(",", outerSelects);
        String innerSelect = String.join(",", innerSelects);
        String groupByString = String.join(",", groupByStrings);
        orderByStringBuilder.append(String.join(",", orderByStrings));

        Map<String, Object> params = new HashMap<>();

        String nullsPosition = SortingOrder.getNullsPosition(SortingOrder.fromString(orderByStringBuilder.toString().split(" ")[1]));
        String subQuery = getSubQuery(company, params, filter, resultsWithoutStacks, true, false, false, true, ouConfig);
        String sql =
                "WITH RECURSIVE cte_BOM AS (" + "\n" + subQuery +
                        "    UNION ALL" + "\n" +
                        "        SELECT DISTINCT(r.id) as run_id, cte_BOM.top_job_id, j.job_full_name, r.job_run_number" + "\n" +
                        "        FROM " + company + ".cicd_job_runs AS r" + "\n" +
                        "        JOIN " + company + ".cicd_jobs AS j ON r.cicd_job_id = j.id" + "\n" +
                        "        LEFT OUTER JOIN " + company + ".cicd_job_run_triggers as t on t.cicd_job_run_id = r.id AND t.type = \'UpstreamCause\'" + "\n" +
                        "        JOIN cte_BOM ON cte_BOM.job_full_name = t.trigger_id AND cte_BOM.job_run_number = t.job_run_number" + "\n" +
                        ")" + "\n" +
                        (acrossTimeInterval ? " SELECT " + outerSelect + "," + calculationComponentStringBuffer + "\n" +
                                " FROM (SELECT r.id, duration, " + innerSelect + "\n" :
                                " SELECT " + innerSelect + "," + calculationComponentStringBuffer + "\n") +
                        " FROM cte_BOM" + "\n" +
                        " JOIN " + company + ".cicd_job_runs as r on cte_BOM.run_id = r.id" + "\n" +
                        " JOIN " + company + ".cicd_jobs AS j ON cte_BOM.top_job_id = j.id" + "\n" +
                        " LEFT OUTER JOIN " + company + ".cicd_instances AS i ON j.cicd_instance_id = i.id " + (acrossTimeInterval ? ") r" : "") + "\n" +
                        " GROUP BY " + groupByString + " ORDER BY " + orderByStringBuilder + " NULLS " + nullsPosition;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        List<DbAggregationResultStacksWrapper> resultsWithStacks = template.query(sql, params, DbCiCdPipelineConverters.distinctJobRunsAggsWithStacksMapper(
                filter));
        List<DbAggregationResult> results = DbAggregationResultsMerger.mergeResultsWithAndWithoutStacks(resultsWithoutStacks.getRecords(), resultsWithStacks);
        return DbListResponse.of(results, results.size());
    }

    private DbListResponse<DbAggregationResult> groupByAndCalculateDateIntervals(String company,
                                                                                 CiCdPipelineJobRunsFilter filter,
                                                                                 DbListResponse<DbAggregationResult> resultsWithoutStacks,
                                                                                 final OUConfiguration ouConfig) {
        CiCdPipelineJobRunsFilter.DISTINCT stacks = filter.getStacks().get(0);
        Stream<DbAggregationResult> dbAggregationResultStream = resultsWithoutStacks
                .getRecords()
                .parallelStream()
                .map(row -> {
                    CiCdPipelineJobRunsFilter newFilter = CiCdPipelineJobRunsFilter.builder().build();
                    CiCdPipelineJobRunsFilter.CiCdPipelineJobRunsFilterBuilder filterBuilder = filter.toBuilder();
                    switch (filter.getAcross()) {
                        case trend:
                        case job_end:
                            newFilter = parseFilterForStacks(stacks, row, filter.getAggInterval() == null ? CICD_AGG_INTERVAL.day : filter.getAggInterval(),
                                    filter.getAcross(), filterBuilder);
                            break;
                        default:
                    }
                    List<DbAggregationResult> currentStackResults = groupByAndCalculateCiCdJobRunsWithoutStacks(company, newFilter,
                            Map.of(stacks.toString(), SortingOrder.ASC), false, ouConfig).getRecords();
                    return row.toBuilder().stacks(currentStackResults).build();
                });
        List<DbAggregationResult> finalList = dbAggregationResultStream.collect(Collectors.toList());
        return DbListResponse.of(finalList, finalList.size());
    }

    private void applyAcrossToStacks(CiCdPipelineJobRunsFilter filter, DbListResponse<DbAggregationResult> resultsWithoutStacks,
                                     Map<String, Object> params, List<String> conditions) {
        switch (filter.getAcross()) {
            case cicd_job_id:
                conditions.add("j.id in (:invalues)");
                params.put("invalues", resultsWithoutStacks.getRecords().stream().map(DbAggregationResult::getCiCdJobId).map(UUID::fromString).collect(Collectors.toList()));
                break;
            case job_status:
                conditions.add("status in (:invalues)");
                params.put("invalues", resultsWithoutStacks.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()));
                break;
            case qualified_job_name:
                List<String> qualifiedJobNamesInCriterea = new ArrayList<>();
                for (int i = 0; i < resultsWithoutStacks.getRecords().size(); i++) {
                    DbAggregationResult dbAggregationResult = resultsWithoutStacks.getRecords().get(i);
                    String instanceNameKey = "in_instance_name" + i;
                    String jobNameKey = "in_job_name" + i;
                    if (dbAggregationResult.getAdditionalKey() == null) {
                        qualifiedJobNamesInCriterea.add("(i.name IS NULL AND job_name = :" + jobNameKey + ")");
                        params.put(jobNameKey, dbAggregationResult.getKey());
                    } else {
                        qualifiedJobNamesInCriterea.add("(i.name = :" + instanceNameKey + " AND job_name = :" + jobNameKey + ")");
                        params.put(instanceNameKey, dbAggregationResult.getAdditionalKey());
                        params.put(jobNameKey, dbAggregationResult.getKey());
                    }
                }
                if (CollectionUtils.isNotEmpty(qualifiedJobNamesInCriterea)) {
                    conditions.add("( " + String.join(" OR ", qualifiedJobNamesInCriterea) + " )");
                }
                break;
            case instance_name:
                conditions.add("i.name IS NOT NULL");
            case job_normalized_full_name:
                conditions.add("j.job_normalized_full_name IS NOT NULL");
            case project_name:
            case job_name:
            case cicd_user_id:
                conditions.add((filter.getAcross() != CiCdPipelineJobRunsFilter.DISTINCT.instance_name ? filter.getAcross().toString() : "i.name") + " in (:invalues)");
                params.put("invalues", resultsWithoutStacks.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()));
                break;
            default:
                Validate.notNull(null, "Invalid across field provided.");
        }
    }
    // endregion
    private CiCdPipelineJobRunsFilter parseFilterForStacks(CiCdPipelineJobRunsFilter.DISTINCT stack, DbAggregationResult row,
                                                           CICD_AGG_INTERVAL aggInterval,
                                                           CiCdPipelineJobRunsFilter.DISTINCT across,
                                                           CiCdPipelineJobRunsFilter.CiCdPipelineJobRunsFilterBuilder ciCdPipelineJobRunsFilterBuilder) {
        ImmutablePair<Long, Long> timeRange = getImmutablePair(row, aggInterval);
        switch (across) {
            case trend:
                ciCdPipelineJobRunsFilterBuilder
                        .startTimeRange(timeRange)
                        .across(stack)
                        .build();
                break;
            case job_end:
                ciCdPipelineJobRunsFilterBuilder
                        .endTimeRange(timeRange)
                        .across(stack)
                        .build();
                break;
            default:
        }
        return ciCdPipelineJobRunsFilterBuilder.build();
    }


    // region Job Runs - Where Clause

    private List<String> createWhereClauseAndUpdateParamsJobRuns(
            final String company,
            final CiCdPipelineJobRunsFilter filter,
            final Map<String, Object> params,
            final boolean listFilters, String suffix, Boolean isList,
            final OUConfiguration ouConfig) {
        return createWhereClauseAndUpdateParamsJobRuns(company, filter, params, listFilters, false, suffix, isList, ouConfig);
    }

    private List<String> createWhereClauseAndUpdateParamsJobRuns(
            final String company,
            final CiCdPipelineJobRunsFilter filter,
            final Map<String, Object> params,
            final boolean listFilters,
            final boolean skipJobIds, String suffix, Boolean isList,
            final OUConfiguration ouConfig) {
        List<String> criteria = new ArrayList<>();
        String paramSuffix = suffix == null ? "" : "_" + suffix;
        if (CollectionUtils.isNotEmpty(filter.getJobIds()) && !skipJobIds) {
            criteria.add("r.cicd_job_id = ANY('{" + filter.getJobIds().stream().filter(Strings::isNotBlank).collect(Collectors.joining(",")) + "}'::uuid[]) ");
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeJobIds())) {
            criteria.add("r.cicd_job_id != ANY('{" + filter.getExcludeJobIds().stream().filter(Strings::isNotBlank).collect(Collectors.joining(",")) + "}'::uuid[]) ");
        }
        if (StringUtils.isNotBlank(filter.getJobRunId()) && !skipJobIds) {
            criteria.add("r.id = '" + filter.getJobRunId().trim() + "'::uuid ");
        }
        if (CollectionUtils.isNotEmpty(filter.getCicdUserIds())) {
            String columnNameParam = "r.cicd_user_id" + paramSuffix;
            TeamUtils.addUsersCondition(company, criteria, params, "r.cicd_user_id",
                    columnNameParam, false, filter.getCicdUserIds(), CiCdAggsService.CICD_APPLICATIONS);
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCiCdUserIds()) || OrgUnitHelper.doesOuConfigHaveCiCdUsers(ouConfig)) {// OU: user
            var columnName = "r.cicd_user_id";
            var columnNameParam = columnName + paramSuffix;
            if (OrgUnitHelper.doesOuConfigHaveCiCdUsers(ouConfig)) {
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.getCICDIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    criteria.add(MessageFormat.format("{0} IN (SELECT cloud_id FROM ({1}) l)", columnName, usersSelect));
                }
            }
            else { 
                TeamUtils.addUsersCondition(company, criteria, params, columnName,
                        columnNameParam, false, filter.getExcludeCiCdUserIds(), CiCdAggsService.CICD_APPLICATIONS, true);
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getJobNames())) {
            criteria.add("j.job_name IN (:job_names" + paramSuffix + ")");
            params.put("job_names" + paramSuffix, filter.getJobNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeJobNames())) {
            criteria.add("j.job_name NOT IN (:excl_job_names" + paramSuffix + ")");
            params.put("excl_job_names" + paramSuffix, filter.getExcludeJobNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getProjects())) {
            criteria.add("j.project_name IN (:project_names" + paramSuffix + ")");
            params.put("project_names" + paramSuffix, filter.getProjects());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeProjects())) {
            criteria.add("j.project_name NOT IN (:excl_project_names" + paramSuffix + ")");
            params.put("excl_project_names" + paramSuffix, filter.getExcludeProjects());
        }
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            criteria.add("i.integration_id IN (:integration_ids" + paramSuffix + ")");
            params.put("integration_ids" + paramSuffix, filter.getIntegrationIds().stream()
                    .map(NumberUtils::toInteger).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getTypes())) {
            criteria.add("i.type IN (:types" + paramSuffix + ")");
            params.put("types" + paramSuffix, filter.getTypes().stream()
                    .map(CICD_TYPE::toString)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTypes())) {
            criteria.add("i.type NOT IN (:excl_types" + paramSuffix + ")");
            params.put("excl_types" + paramSuffix, filter.getExcludeTypes().stream()
                    .map(CICD_TYPE::toString)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getInstanceNames())) {
            criteria.add("i.name IN (:instance_names" + paramSuffix + ")");
            params.put("instance_names" + paramSuffix, filter.getInstanceNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeInstanceNames())) {
            criteria.add("i.name NOT IN (:excl_instance_names" + paramSuffix + ")");
            params.put("excl_instance_names" + paramSuffix, filter.getExcludeInstanceNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getJobNormalizedFullNames())) {
            criteria.add("j.job_normalized_full_name IN (:job_normalized_full_names" + paramSuffix + ")");
            params.put("job_normalized_full_names" + paramSuffix, filter.getJobNormalizedFullNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeJobNormalizedFullNames())) {
            criteria.add("j.job_normalized_full_name NOT IN (:excl_job_normalized_full_names" + paramSuffix + ")");
            params.put("excl_job_normalized_full_names" + paramSuffix, filter.getExcludeJobNormalizedFullNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getQualifiedJobNames())) {
            List<String> qualifiedJobNamesCriterea = new ArrayList<>();
            for (int i = 0; i < filter.getQualifiedJobNames().size(); i++) {
                CiCdJobQualifiedName qualifiedJobName = filter.getQualifiedJobNames().get(i);
                if (StringUtils.isBlank(qualifiedJobName.getJobName())) {
                    log.warn("qualified job name, job name is null or empty skipping it!! {}", qualifiedJobName);
                    continue;
                }
                String instanceNameKey = "instance_name" + i;
                String jobNameKey = "job_name" + i;
                if (qualifiedJobName.getInstanceName() == null) {
                    qualifiedJobNamesCriterea.add("(i.name IS NULL AND job_name = :" + jobNameKey + paramSuffix + ")");
                    params.put(jobNameKey + paramSuffix, qualifiedJobName.getJobName());
                } else {
                    qualifiedJobNamesCriterea.add("(i.name = :" + instanceNameKey + paramSuffix + " AND job_name = :" + jobNameKey + paramSuffix + ")");
                    params.put(instanceNameKey + paramSuffix, qualifiedJobName.getInstanceName());
                    params.put(jobNameKey + paramSuffix, qualifiedJobName.getJobName());
                }
            }
            if (CollectionUtils.isNotEmpty(qualifiedJobNamesCriterea)) {
                criteria.add("( " + String.join(" OR ", qualifiedJobNamesCriterea) + " )");
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeQualifiedJobNames())) {
            List<String> excludeQualifiedJobNamesCriterea = new ArrayList<>();
            for (int i = 0; i < filter.getExcludeQualifiedJobNames().size(); i++) {
                CiCdJobQualifiedName excludeQualifiedJobName = filter.getExcludeQualifiedJobNames().get(i);
                if (StringUtils.isBlank(excludeQualifiedJobName.getJobName())) {
                    log.warn("exclude qualified job name, job name is null or empty skipping it!! {}", excludeQualifiedJobName);
                    continue;
                }
                String instanceNameKey = "excl_instance_name" + i;
                String jobNameKey = "excl_job_name" + i;
                if (excludeQualifiedJobName.getInstanceName() == null) {
                    excludeQualifiedJobNamesCriterea.add("(i.name IS NULL AND job_name != :" + jobNameKey + paramSuffix + ")");
                    params.put(jobNameKey + paramSuffix, excludeQualifiedJobName.getJobName());
                } else {
                    excludeQualifiedJobNamesCriterea.add("(i.name != :" + instanceNameKey + paramSuffix + " AND job_name != :" + jobNameKey + paramSuffix + ")");
                    params.put(instanceNameKey + paramSuffix, excludeQualifiedJobName.getInstanceName());
                    params.put(jobNameKey + paramSuffix, excludeQualifiedJobName.getJobName());
                }
            }
            if (CollectionUtils.isNotEmpty(excludeQualifiedJobNamesCriterea)) {
                criteria.add("( " + String.join(" OR ", excludeQualifiedJobNamesCriterea) + " )");
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getJobStatuses())) {
            criteria.add("r.status IN (:job_statuses" + paramSuffix + ")");
            params.put("job_statuses" + paramSuffix, filter.getJobStatuses());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeJobStatuses())) {
            criteria.add("r.status NOT IN (:excl_job_statuses" + paramSuffix + ")");
            params.put("excl_job_statuses" + paramSuffix, filter.getExcludeJobStatuses());
        }
        if (CollectionUtils.isNotEmpty(filter.getParameters())) {
            List<String> parametersCriterea = new ArrayList<>();
            for (int i = 0; i < filter.getParameters().size(); i++) {
                CiCdJobRunParameter param = filter.getParameters().get(i);
                if (StringUtils.isBlank(param.getName())) {
                    log.warn("param name is null or empty skipping it!! {}", param);
                    continue;
                }
                if (CollectionUtils.isEmpty(param.getValues())) {
                    log.warn("param values is null or empty skipping it!! {}", param);
                    continue;
                }
                List<String> sanitizedValues = param.getValues().stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(sanitizedValues)) {
                    log.warn("sanitized param values is null or empty skipping it!! {}", sanitizedValues);
                    continue;
                }

                String nameKey = "name" + i;
                String valueKey = "values" + i;
                parametersCriterea.add("(p.name = :" + nameKey + paramSuffix + " AND p.value IN (:" + valueKey + paramSuffix + "))");
                params.put(nameKey + paramSuffix, param.getName());
                params.put(valueKey + paramSuffix, param.getValues());
            }
            if (CollectionUtils.isNotEmpty(parametersCriterea)) {
                criteria.add("( " + String.join(" OR ", parametersCriterea) + " )");
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getCiCdJobRunIds()) && !skipJobIds) {
            criteria.add("r.id IN (:cicd_job_run_ids" + paramSuffix + ")");
            params.put("cicd_job_run_ids" + paramSuffix, filter.getCiCdJobRunIds().stream().map(UUID::fromString).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCiCdJobRunIds())) {
            criteria.add("r.id NOT IN (:excl_cicd_job_run_ids" + paramSuffix + ")");
            params.put("excl_cicd_job_run_ids" + paramSuffix, filter.getExcludeCiCdJobRunIds().stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getParentCiCdJobIds()) && !skipJobIds) {
            criteria.add("t.trigger_id in (SELECT job_full_name FROM " + company + ".cicd_jobs WHERE id IN (:parent_cicd_job_ids" + paramSuffix + "))");
            params.put("parent_cicd_job_ids" + paramSuffix, filter.getParentCiCdJobIds().stream().map(UUID::fromString).collect(Collectors.toList()));
        } else {
            if ((listFilters) && (CollectionUtils.isNotEmpty(criteria))) {
                log.debug("We are using list filters (not agg filters) and other criterea is present not using trigger cicd_job_run_id IS NULL check");
            } else {
                criteria.add("t.cicd_job_run_id IS NULL");
            }
        }
        if (StringUtils.isNotBlank(filter.getJobNamePartial())) {
            criteria.add("j.job_name ILIKE :job_name_partial" + paramSuffix);
            params.put("job_name_partial" + paramSuffix, "%" + filter.getJobNamePartial() + "%");
        }
        if (filter.getStartTimeRange() != null) {
            ImmutablePair<Long, Long> startTimeRange = filter.getStartTimeRange();
            if (startTimeRange.getLeft() != null) {
                criteria.add("r.start_time > to_timestamp(:start_time_start" + paramSuffix + ")");
                params.put("start_time_start" + paramSuffix, startTimeRange.getLeft());
            }
            if (startTimeRange.getRight() != null) {
                criteria.add("r.start_time < to_timestamp(:start_time_end" + paramSuffix + ")");
                params.put("start_time_end" + paramSuffix, startTimeRange.getRight());
            }
        }
        if (filter.getEndTimeRange() != null) {
            ImmutablePair<Long, Long> endTimeRange = filter.getEndTimeRange();
            if (endTimeRange.getLeft() != null) {
                criteria.add("r.end_time > to_timestamp(:end_time_start" + paramSuffix + ")");
                params.put("end_time_start" + paramSuffix, endTimeRange.getLeft());
            }
            if (endTimeRange.getRight() != null) {
                criteria.add("r.end_time < to_timestamp(:end_time_end" + paramSuffix + ")");
                params.put("end_time_end" + paramSuffix, endTimeRange.getRight());
            }
        }
        if (filter.getEndTimeRange() != null) {
            ImmutablePair<Long, Long> endTimeRange = filter.getEndTimeRange();
            if (endTimeRange.getLeft() != null) {
                criteria.add("r.end_time > to_timestamp(:end_time_start" + paramSuffix + ")");
                params.put("end_time_start" + paramSuffix, endTimeRange.getLeft());
            }
            if (endTimeRange.getRight() != null) {
                criteria.add("r.end_time < to_timestamp(:end_time_end" + paramSuffix + ")");
                params.put("end_time_end" + paramSuffix, endTimeRange.getRight());
            }
        }
        Map<String, Map<String, String>> partialMatchMap = filter.getPartialMatch();
        if (MapUtils.isNotEmpty(partialMatchMap)) {
            CriteriaUtils.addPartialMatchClause(partialMatchMap, criteria, params, null, CICD_PIPELINE_PARTIAL_MATCH_COLUMNS, Collections.emptySet(), paramSuffix);
        }
        if (!isList) {
            int acrossCount = (filter.getAcrossCount() != null) ? filter.getAcrossCount() : 90;
            if (CiCdPipelineJobRunsFilter.DISTINCT.trend == filter.getAcross() ||
                    CollectionUtils.isNotEmpty(filter.getStacks()) && filter.getStacks().get(0).equals(CiCdPipelineJobRunsFilter.DISTINCT.trend)) {
                criteria.add("r.start_time >= to_timestamp(:start_time_from" + paramSuffix + ")");
                params.put("start_time_from" + paramSuffix, Instant.now().minus(acrossCount, ChronoUnit.DAYS).getEpochSecond());
            }
            if (CiCdPipelineJobRunsFilter.DISTINCT.job_normalized_full_name == filter.getAcross()) {
                criteria.add(("j.job_normalized_full_name IS NOT NULL"));
            }
        }

        ciCdMetadataConditionBuilder.prepareMetadataConditions(CiCdPipelineJobRunsFilter.parseToCiCdJobRunsFilter(filter),
                params, null, criteria);

        ciCdPartialMatchConditionBuilder.preparePartialMatchConditions(CiCdPipelineJobRunsFilter.parseToCiCdJobRunsFilter(filter),
                params, criteria, paramSuffix, CICD_PARTIAL_MATCH_COLUMNS);

        return criteria;
    }

    // endregion

    // region Job Runs - List
    public DbListResponse<CICDJobRunDTO> listCiCdJobRuns(
            final String company,
            final CiCdPipelineJobRunsFilter filter,
            final Map<String, SortingOrder> sortBy,
            final OUConfiguration ouConfig,
            final Integer pageNumber,
            final Integer pageSize) throws SQLException {
        return listCiCdJobRuns(company, filter.toBuilder().sortBy(sortBy).build(), pageNumber, pageSize, true, false, ouConfig);
    }

    /**
     * @param company
     * @param filter
     * @param pageNumber
     * @param pageSize
     * @param fullDetails  if true, no filter will be applied to the related triggers for the main job runs selected in the base query
     * @param singleJobRun if the filters contain jobId and singleJobRun is true then the result will contain only the job that matches the id but none of the related jobs in the trigger chain
     * @return
     * @throws SQLException
     */
    public DbListResponse<CICDJobRunDTO> listCiCdJobRuns(
            final String company,
            final CiCdPipelineJobRunsFilter filter,
            final Integer pageNumber,
            final Integer pageSize,
            final boolean fullDetails,
            final boolean singleJobRun,
            final OUConfiguration ouConfig) throws SQLException {
        Map<String, Object> params = new HashMap<>();
        Map<String, SortingOrder> sortBy = filter.getSortBy();
        String subQuery = getSubQuery(company, params, filter, null, fullDetails, singleJobRun, true, false, ouConfig);
        List<String> orderBy = getListSortBy(sortBy);
        String nullsPosition = SortingOrder.getNullsPosition(SortingOrder.fromString(orderBy.get(0).split(" ")[1]));
        subQuery += " ORDER BY " + String.join(",", orderBy) + " NULLS " + nullsPosition;
        log.debug("subQuery = {}", subQuery);

        String sql = subQuery + " OFFSET " + (pageNumber * pageSize) + " LIMIT " + pageSize;

        String countSQL = "SELECT COUNT(*) FROM ( " + subQuery + " ) x";
        log.debug("countSQL = {}", countSQL);

        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<CICDJobRunDTO> results = template.query(sql, params, DbCiCdPipelineConverters.jobRunsListMapper());
        List<UUID> jobRunIds = results.stream().map(CICDJobRunDTO::getId).collect(Collectors.toList());
        Map<UUID, List<CICDJobRun.JobRunParam>> jobRunParams = ciCdJobRunsDatabaseService.getJobRunParams(company, jobRunIds);
        List<CICDJobRunDTO> mergedJobRuns = mergeJobRunsAndParam(results, jobRunParams);
        int totCount = 0;
        if (results.size() > 0) {
            totCount = results.size() + (pageNumber * pageSize); // if its last page or total count is less than pageSize
            if (results.size() == pageSize) {
                totCount = template.query(countSQL, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(mergedJobRuns, totCount);
    }

    private List<CICDJobRunDTO> mergeJobRunsAndParam(final List<CICDJobRunDTO> jobRuns, final Map<UUID,
            List<CICDJobRun.JobRunParam>> jobRunParams) {
        List<CICDJobRunDTO> fullJobRuns = new ArrayList<>();
        for (CICDJobRunDTO r : jobRuns) {
            CICDJobRunDTO.CICDJobRunDTOBuilder bldr = r.toBuilder();
            if (jobRunParams.containsKey(r.getId())) {
                bldr.params(jobRunParams.get(r.getId()));
            }
            fullJobRuns.add(bldr.build());
        }
        return fullJobRuns;
    }
    // endregion
}
