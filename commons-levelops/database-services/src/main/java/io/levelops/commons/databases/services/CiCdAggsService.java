package io.levelops.commons.databases.services;

import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.converters.DbCiCdConverters;
import io.levelops.commons.databases.models.database.CICDJobConfigChangeDTO;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CICDJobRunDTO;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.cicd_scm.DBDummyObj;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobConfigChangesFilter;
import io.levelops.commons.databases.models.filters.CiCdJobConfigChangesFilter.DISTINCT;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.filters.CiCdJobQualifiedName;
import io.levelops.commons.databases.models.filters.CiCdJobRunParameter;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.DbAggregationResultStacksWrapper;
import io.levelops.commons.databases.models.response.DbAggregationResultsMerger;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.parsers.CiCdFilterParserCommons;
import io.levelops.commons.databases.services.parsers.CiCdJobConfigChangesFilterParser;
import io.levelops.commons.databases.services.parsers.CiCdJobRunsFilterParsers;
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
import java.util.Date;
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

@Log4j2
@Service
public class CiCdAggsService extends DatabaseService<DBDummyObj> {
    public static final Set<String> CICD_PARTIAL_MATCH_COLUMNS = Set.of("job_normalized_full_name");
    public static final List<String> CICD_APPLICATIONS = List.of("jenkins", "azure_devops", "gitlab");
    public static final String CICD_CONDITIONS = "cicd_conditions";
    public static final String TRIAGE_CONDITIONS = "triage_conditions";

    private final NamedParameterJdbcTemplate template;
    private final CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private final CiCdFilterParserCommons ciCdFilterParserCommons;
    private final CiCdJobRunsFilterParsers ciCdJobRunsFilterParsers;
    private final CiCdJobConfigChangesFilterParser ciCdJobConfigChangesFilterParser;

    private final CiCdMetadataConditionBuilder ciCdMetadataConditionBuilder;
    private final CiCdPartialMatchConditionBuilder ciCdPartialMatchConditionBuilder;
    private final CiCdStageStepConditionBuilder ciCdStageStepConditionBuilder;

    // region CSTOR
    @Autowired
    public CiCdAggsService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        ProductsDatabaseService productsDatabaseService = new ProductsDatabaseService(dataSource, DefaultObjectMapper.get());
        this.ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        this.ciCdFilterParserCommons = new CiCdFilterParserCommons(productsDatabaseService);
        this.ciCdJobRunsFilterParsers = new CiCdJobRunsFilterParsers(DefaultObjectMapper.get());
        this.ciCdJobConfigChangesFilterParser = new CiCdJobConfigChangesFilterParser(DefaultObjectMapper.get());
        this.ciCdMetadataConditionBuilder = new CiCdMetadataConditionBuilder();
        this.ciCdPartialMatchConditionBuilder = new CiCdPartialMatchConditionBuilder();
        this.ciCdStageStepConditionBuilder = new CiCdStageStepConditionBuilder();
    }
    // endregion

    // region Unused
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(CiCdJobsDatabaseService.class, CiCdJobRunsDatabaseService.class);
    }

    @Override
    public String insert(String company, DBDummyObj t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean update(String company, DBDummyObj t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DBDummyObj> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DbListResponse<DBDummyObj> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
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

    private String getSubQuery(String company, Map<String, Object> params, CiCdJobConfigChangesFilter filter,
                               String innerSelect, DbListResponse<DbAggregationResult> resultsWithoutStacks,
                               Boolean isList, Boolean hasStacks, OUConfiguration ouConfig) {
        Map<Integer, Map<String, Object>> productFilters = null;
        try {
            productFilters = ciCdFilterParserCommons.getProductFilters(company, filter.getOrgProductsIds());
        } catch (SQLException throwables) {
            log.error("Error encountered while fetching product filters for company " + company, throwables);
        }
        if (productFilters == null || MapUtils.isEmpty(productFilters)) {
            List<String> conditions = createWhereClauseAndUpdateParamsJobConfigChanges(company, filter, params, null, isList, ouConfig);
            if (hasStacks) {
                applyAcrossToStacks(filter, resultsWithoutStacks, params, conditions);
            }
            return ciCdJobConfigChangesFilterParser.getSqlStmt(company, conditions, innerSelect, isList);
        }
        AtomicInteger suffix = new AtomicInteger();
        Map<Integer, Map<String, Object>> integFiltersMap = productFilters;
        List<String> filterSqlStmts = CollectionUtils.emptyIfNull(productFilters.keySet()).stream()
                .map(integrationId -> {
                    CiCdJobConfigChangesFilter newJobConfigFilter = ciCdJobConfigChangesFilterParser.merge(integrationId, filter, integFiltersMap.get(integrationId));
                    List<String> conditions = createWhereClauseAndUpdateParamsJobConfigChanges(company, newJobConfigFilter, params,
                            String.valueOf(suffix.incrementAndGet()), isList, ouConfig);
                    if (hasStacks) {
                        applyAcrossToStacks(newJobConfigFilter, resultsWithoutStacks, params, conditions);
                    }
                    return ciCdJobConfigChangesFilterParser.getSqlStmt(company, conditions, innerSelect, isList);
                })
                .collect(Collectors.toList());
        return String.join(" UNION ", filterSqlStmts);
    }
    // endregion

    // region Job Config Changes - Aggs
    public DbListResponse<DbAggregationResult> groupByAndCalculateCiCdJobConfigChangesWithoutStacks(String company,
                                                                                                    CiCdJobConfigChangesFilter filter,
                                                                                                    Map<String, SortingOrder> sortBy,
                                                                                                    boolean valuesOnly,
                                                                                                    OUConfiguration ouConfig) {
        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        StringBuilder orderByStringBuilder = new StringBuilder();
        StringBuffer calculationComponentStringBuffer = new StringBuffer();
        Set<String> innerSelects = new HashSet<>();
        Set<String> groupByStrings = new HashSet<>();
        Set<String> orderByStrings = new HashSet<>();

        CiCdJobConfigChangesFilter.CALCULATION calculation = CiCdJobConfigChangesFilter.getSanitizedCalculation(filter);

        parseCiCdJobConfigChangesCalculation(calculation, calculationComponentStringBuffer, sortBy, orderByStrings);

        parseCiCdJobConfigChangesAcrossOrStack(filter.getAcross(), innerSelects, groupByStrings, sortBy, orderByStrings);

        String innerSelect = String.join(",", innerSelects);
        String groupByString = String.join(",", groupByStrings);

        orderByStringBuilder.append(String.join(",", orderByStrings));

        Map<String, Object> params = new HashMap<>();

        int acrossCount = (filter.getAcrossCount() != null) ? filter.getAcrossCount() : 90;
        String limit = (valuesOnly) ? "" : " LIMIT " + acrossCount;

        String nullsPosition = SortingOrder.getNullsPosition(SortingOrder.fromString(orderByStringBuilder.toString().split(" ")[1]));
        String subQuery = getSubQuery(company, params, filter, innerSelect,
                null, false, false, ouConfig);
        String sql = "SELECT " + groupByString + "," + calculationComponentStringBuffer.toString() + " FROM (" + subQuery
                + ") final  GROUP BY " + groupByString + " ORDER BY " + orderByStringBuilder.toString() + " NULLS " + nullsPosition
                + limit;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        List<DbAggregationResult> results = template.query(sql, params, DbCiCdConverters.distinctJobConfigChangesAggsMapper(
                filter.getAcross(), filter.getCalculation()));
        return DbListResponse.of(results, results.size());
    }

    private void parseCiCdJobConfigChangesCalculation(CiCdJobConfigChangesFilter.CALCULATION calculation,
                                                      StringBuffer calculationComponentStringBuffer,
                                                      Map<String, SortingOrder> sortBy, Set<String> orderByStrings) {
        switch (calculation) {
            case count:
                calculationComponentStringBuffer.append("COUNT(id) as ct");
                parseSortBy(calculation.toString(), orderByStrings, sortBy, "ct", true);
                break;
            default:
                Validate.notNull(null, "Invalid calculation field provided.");
        }
    }

    private void parseCiCdJobConfigChangesAcrossOrStack(CiCdJobConfigChangesFilter.DISTINCT acrossOrStack, Set<String> innerSelects, Set<String> groupByStrings, Map<String, SortingOrder> sortBy, Set<String> orderByString) {
        switch (acrossOrStack) {
            case qualified_job_name:
                innerSelects.add("i.name as instance_name");
                innerSelects.add("job_name");
                groupByStrings.add("instance_name");
                groupByStrings.add("job_name");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("job_name"));
                break;
            case project_name:
            case job_name:
            case cicd_user_id:
                innerSelects.add(acrossOrStack.toString());
                groupByStrings.add(acrossOrStack.toString());
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf(acrossOrStack.toString()));
                break;
            case instance_name:
                innerSelects.add("i.name as instance_name");
                groupByStrings.add("instance_name");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("instance_name"));
                break;
            case trend:
                innerSelects.add("change_time::date as trend");
                groupByStrings.add("trend");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, "trend");
                break;
            default:
                Validate.notNull(null, "Invalid across field provided.");
        }
        if(MapUtils.isNotEmpty(sortBy) && orderByString.isEmpty()){
            if(!sortBy.keySet().stream().findFirst().get().equals(acrossOrStack.toString()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field ");
        }
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateCiCdJobConfigChanges(String company,
                                                                                       CiCdJobConfigChangesFilter filter,
                                                                                       Map<String, SortingOrder> sortBy,
                                                                                       boolean valuesOnly,
                                                                                       OUConfiguration ouConfig) {
        return groupByAndCalculateCiCdJobConfigChanges(company, filter.toBuilder().sortBy(sortBy).build(), valuesOnly, ouConfig);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateCiCdJobConfigChanges(String company,
                                                                                       CiCdJobConfigChangesFilter filter,
                                                                                       boolean valuesOnly,
                                                                                       OUConfiguration ouConfig) {
        Map<String, SortingOrder> sortBy = filter.getSortBy();
        DbListResponse<DbAggregationResult> resultsWithoutStacks = groupByAndCalculateCiCdJobConfigChangesWithoutStacks(company, filter, sortBy, valuesOnly, ouConfig);
        if (CollectionUtils.isEmpty(resultsWithoutStacks.getRecords())) {
            return resultsWithoutStacks;
        }

        if (CollectionUtils.isEmpty(filter.getStacks())) {
            return resultsWithoutStacks;
        }

        StringBuilder orderByStringBuilder = new StringBuilder();
        StringBuffer calculationComponentStringBuffer = new StringBuffer();
        Set<String> innerSelects = new HashSet<>();
        Set<String> groupByStrings = new HashSet<>();
        Set<String> orderByStrings = new HashSet<>();
        CiCdJobConfigChangesFilter.CALCULATION calculation = CiCdJobConfigChangesFilter.getSanitizedCalculation(filter);
        parseCiCdJobConfigChangesCalculation(calculation, calculationComponentStringBuffer, sortBy, orderByStrings);

        parseCiCdJobConfigChangesAcrossOrStack(filter.getAcross(), innerSelects, groupByStrings, sortBy, orderByStrings);
        if (CollectionUtils.isNotEmpty(filter.getStacks())) {
            for (CiCdJobConfigChangesFilter.DISTINCT stack : filter.getStacks()) {
                parseCiCdJobConfigChangesAcrossOrStack(stack, innerSelects, groupByStrings, sortBy, orderByStrings);
            }
        }
        String innerSelect = String.join(",", innerSelects);
        String groupByString = String.join(",", groupByStrings);
        orderByStringBuilder.append(String.join(",", orderByStrings));

        Map<String, Object> params = new HashMap<>();

        String nullsPosition = SortingOrder.getNullsPosition(SortingOrder.fromString(orderByStringBuilder.toString().split(" ")[1]));
        String subQuery = getSubQuery(company, params, filter, innerSelect,
                resultsWithoutStacks, false, true, ouConfig);
        String sql = "SELECT " + groupByString + "," + calculationComponentStringBuffer.toString() + " FROM (" + subQuery
                + ") final  GROUP BY " + groupByString + " ORDER BY " + orderByStringBuilder.toString() + " NULLS " + nullsPosition;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        List<DbAggregationResultStacksWrapper> resultsWithStacks = template.query(sql, params, DbCiCdConverters.distinctJobConfigChangesAggsWithStacksMapper(
                filter));
        List<DbAggregationResult> results = DbAggregationResultsMerger.mergeResultsWithAndWithoutStacks(resultsWithoutStacks.getRecords(), resultsWithStacks);
        return DbListResponse.of(results, results.size());
    }

    private void applyAcrossToStacks(CiCdJobConfigChangesFilter filter, DbListResponse<DbAggregationResult> resultsWithoutStacks, Map<String, Object> params, List<String> conditions) {
        switch (filter.getAcross()) {
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
            case project_name:
            case job_name:
            case cicd_user_id:
            case instance_name:
                conditions.add((filter.getAcross() != DISTINCT.instance_name ? filter.getAcross().toString() : "i.name") + " in (:invalues)");
                params.put("invalues", resultsWithoutStacks.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()));
                break;
            case trend:
                conditions.add("change_time::date in (:invalues::date)");
                params.put("invalues", resultsWithoutStacks.getRecords().stream().map(DbAggregationResult::getKey).map(Long::parseLong).map(Instant::ofEpochSecond).map(x -> new Date(x.toEpochMilli())).collect(Collectors.toList()));
                break;
            default:
                Validate.notNull(null, "Invalid across field provided.");
        }
    }

    private List<String> createWhereClauseAndUpdateParamsJobConfigChanges(String company, CiCdJobConfigChangesFilter filter,
                                                                          Map<String, Object> params,
                                                                          String suffix, Boolean isList,
                                                                          OUConfiguration ouConfig) {
        List<String> criterea = new ArrayList<>();
        String paramSuffix = suffix == null ? "" : "_" + suffix;
        if (CollectionUtils.isNotEmpty(filter.getCicdUserIds()) || OrgUnitHelper.doesOuConfigHaveCiCdUsers(ouConfig)) { // OU: user
            var columnName = "c.cicd_user_id" + paramSuffix;
            var columnNameParam = columnName + paramSuffix;
            if(OrgUnitHelper.doesOuConfigHaveCiCdUsers(ouConfig)){
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.getCICDIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    criterea.add(MessageFormat.format("{0} IN (SELECT cloud_id FROM ({1}) l)", columnName, usersSelect));
                }
            }
            else if(CollectionUtils.isNotEmpty(filter.getCicdUserIds())){
                TeamUtils.addUsersCondition(company, criterea, params, "c.cicd_user_id", columnNameParam, false, filter.getCicdUserIds(), CICD_APPLICATIONS);
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCiCdUserIds())) {
            String columnNameParam = "c.cicd_user_id" + paramSuffix;
            TeamUtils.addUsersCondition(company, criterea, params, "c.cicd_user_id",
                    columnNameParam, false, filter.getExcludeCiCdUserIds(), CICD_APPLICATIONS, true);
        }
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            criterea.add("i.integration_id IN (:integration_ids" + paramSuffix + ")");
            params.put("integration_ids" + paramSuffix, filter.getIntegrationIds().stream()
                    .map(NumberUtils::toInteger).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getTypes())) {
            criterea.add("i.type IN (:types" + paramSuffix + ")");
            params.put("types" + paramSuffix, filter.getTypes().stream()
                    .map(CICD_TYPE::toString)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTypes())) {
            criterea.add("i.type NOT IN (:excl_types" + paramSuffix + ")");
            params.put("excl_types" + paramSuffix, filter.getExcludeTypes().stream()
                    .map(CICD_TYPE::toString)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getJobNames())) {
            criterea.add("j.job_name IN (:job_names" + paramSuffix + ")");
            params.put("job_names" + paramSuffix, filter.getJobNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeJobNames())) {
            criterea.add("j.job_name NOT IN (:excl_job_names" + paramSuffix + ")");
            params.put("excl_job_names" + paramSuffix, filter.getExcludeJobNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getProjects())) {
            criterea.add("j.project_name IN (:project_names" + paramSuffix + ")");
            params.put("project_names" + paramSuffix, filter.getProjects());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeProjects())) {
            criterea.add("j.project_name NOT IN (:excl_project_names" + paramSuffix + ")");
            params.put("excl_project_names" + paramSuffix, filter.getExcludeProjects());
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
                criterea.add("( " + String.join(" OR ", qualifiedJobNamesCriterea) + " )");
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
                criterea.add("( " + String.join(" OR ", excludeQualifiedJobNamesCriterea) + " )");
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getInstanceNames())) {
            criterea.add("i.name IN (:instance_names" + paramSuffix + ")");
            params.put("instance_names" + paramSuffix, filter.getInstanceNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeInstanceNames())) {
            criterea.add("i.name NOT IN (:excl_instance_names" + paramSuffix + ")");
            params.put("excl_instance_names" + paramSuffix, filter.getExcludeInstanceNames());
        }
        if (filter.getChangeStartTime() != null) {
            criterea.add("c.change_time >= TO_TIMESTAMP(" + filter.getChangeStartTime() + paramSuffix + ")");
        }
        if (filter.getChangeEndTime() != null) {
            criterea.add("c.change_time <= TO_TIMESTAMP(" + filter.getChangeEndTime() + paramSuffix + ")");
        }
        if (filter.getStacks() == null || CollectionUtils.isEmpty(filter.getStacks()) || !isList) {
            int acrossCount = (filter.getAcrossCount() != null) ? filter.getAcrossCount() : 90;
            if (CiCdJobConfigChangesFilter.DISTINCT.trend == filter.getAcross() && filter.getChangeEndTime() == null
            && filter.getChangeStartTime() == null) {
                criterea.add("change_time >= to_timestamp(:change_time_from)");
                params.put("change_time_from", Instant.now().minus(acrossCount, ChronoUnit.DAYS).getEpochSecond());
            }
            if (CiCdJobConfigChangesFilter.DISTINCT.instance_name == filter.getAcross()) {
                criterea.add("i.name IS NOT NULL");
            }
        }

        return criterea;
    }
    // endregion

    public DbListResponse<CICDJobConfigChangeDTO> listCiCdJobConfigChanges(String company, CiCdJobConfigChangesFilter filter, OUConfiguration ouConfig,
                                                                           Integer pageNumber, Integer pageSize) {
        Map<String, Object> params = new HashMap<>();

        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        List<String> orderBy = getListSortBy(filter.getSortBy(), "change_time");
        String nullsPosition = SortingOrder.getNullsPosition(SortingOrder.fromString(orderBy.get(0).split(" ")[1]));
        String subQuery = getSubQuery(company, params, filter, null, null, true, false, ouConfig);
        String sqlBase = "SELECT * FROM (" + subQuery + " ) final ORDER BY " + String.join(",", orderBy) + " NULLS " + nullsPosition;

        String sql = sqlBase + " OFFSET :skip LIMIT :limit";

        String countSQL = "SELECT COUNT(*) FROM ( " + sqlBase + " ) x";
        log.debug("countSQL = {}", countSQL);

        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<CICDJobConfigChangeDTO> results = template.query(sql, params, DbCiCdConverters.jobConfigChangesListMapper());
        int totCount = 0;
        if (results.size() > 0) {
            totCount = results.size() + (pageNumber * pageSize); // if its last page or total count is less than pageSize
            if (results.size() == pageSize) {
                totCount = template.query(countSQL, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(results, totCount);
    }
    // endregion

    // region Job Runs - Aggs
    public DbListResponse<DbAggregationResult> groupByAndCalculateCiCdJobRunsWithoutStacks(String company,
                                                                                           CiCdJobRunsFilter filter,
                                                                                           Map<String, SortingOrder> sortBy,
                                                                                           boolean valuesOnly,
                                                                                           OUConfiguration ouConfig) {
        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        CiCdJobRunsFilter.CALCULATION calculation = CiCdJobRunsFilter.getSanitizedCalculation(filter);

        StringBuilder orderByStringBuilder = new StringBuilder();
        StringBuffer calculationComponentStringBuffer = new StringBuffer();
        Set<String> outerSelects = new HashSet<>();
        Set<String> innerSelects = new HashSet<>();
        Set<String> groupByStrings = new HashSet<>();
        Set<String> orderByString = new HashSet<>();

        //Calculation should be parsed before across as orderBy could get overwritten in parse across
        parseCiCdJobRunsCalculation(calculation, filter.getAcross(), filter.getStacks(), calculationComponentStringBuffer, sortBy, orderByString);
        parseCiCdJobRunsAcrossOrStack(filter.getAcross(), filter.getAggInterval(), outerSelects, innerSelects, groupByStrings, sortBy, orderByString);

        if(CollectionUtils.isNotEmpty(filter.getStacks()) && filter.getStacks().contains(CiCdJobRunsFilter.DISTINCT.triage_rule))
            innerSelects.addAll(List.of("triage_rule", "triage_rule_id"));

        String outerSelect = String.join(",", outerSelects);
        String innerSelect = String.join(",", innerSelects);
        String groupByString = String.join(",", groupByStrings);
        orderByStringBuilder.append(String.join(",", orderByString));

        Map<String, Object> params = new HashMap<>();
        int acrossCount = (filter.getAcrossCount() != null) ? filter.getAcrossCount() : 90;
        String limit = (valuesOnly) ? "" : " LIMIT " + acrossCount;
        String nullsPosition = SortingOrder.getNullsPosition(SortingOrder.fromString(orderByStringBuilder.toString().split(" ")[1]));
        String subQuery = getSubQuery(company, params, filter, innerSelect, null, false, false, ouConfig);
        String sql = "SELECT " + outerSelect + "," + calculationComponentStringBuffer +
                " FROM (" + subQuery + " ) a" + " GROUP BY " + groupByString + " ORDER BY " + orderByStringBuilder +" NULLS "+ nullsPosition + limit;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        List<DbAggregationResult> results = template.query(sql, params, DbCiCdConverters.distinctJobRunsAggsMapper(
                filter.getAcross(), filter.getCalculation()));
        return DbListResponse.of(results, results.size());
    }

    private void parseCiCdJobRunsAcrossOrStack(CiCdJobRunsFilter.DISTINCT acrossOrStack, CICD_AGG_INTERVAL aggInterval,
                                               Set<String> outerSelects, Set<String> innerSelects, Set<String> groupByStrings,
                                               Map<String, SortingOrder> sortBy, Set<String> orderByString) {
        boolean sortAscending = false;
        switch (acrossOrStack) {
            case job_status:
                outerSelects.add("status");
                innerSelects.add("status");
                groupByStrings.add("status");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("status"));
                break;
            case service:
                outerSelects.add("service");
                innerSelects.add("jsonb_array_elements_text(r.metadata->'service_ids') as service");
                groupByStrings.add("service");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("service"));
                break;
            case infrastructure:
                outerSelects.add("infrastructure");
                innerSelects.add("jsonb_array_elements_text(r.metadata->'infra_ids') as infrastructure");
                groupByStrings.add("infrastructure");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("infrastructure"));
                break;
            case environment:
                outerSelects.add("environment");
                innerSelects.add("jsonb_array_elements_text(r.metadata->'env_ids') as environment");
                groupByStrings.add("environment");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("environment"));
                break;
            case deployment_type:
                outerSelects.add("deployment_type");
                innerSelects.add("jsonb_array_elements_text(r.metadata->'service_types') as deployment_type");
                groupByStrings.add("deployment_type");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("deployment_type"));
                break;
            case repository:
                outerSelects.add("repository");
                innerSelects.add("r.metadata->>'repo_url' as repository");
                groupByStrings.add("repository");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("repository"));
                break;
            case branch:
                outerSelects.add("branch");
                innerSelects.add("r.metadata->>'branch' as branch");
                groupByStrings.add("branch");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("branch"));
                break;
            case tag:
                outerSelects.add("tag");
                innerSelects.add("jsonb_array_elements_text(r.metadata->'tags') as tag");
                groupByStrings.add("tag");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("tag"));
                break;
            case rollback:
                outerSelects.add("rollback");
                innerSelects.add("r.metadata->>'rollback' as rollback");
                groupByStrings.add("rollback");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("rollback"));
                break;
            case qualified_job_name:
                outerSelects.add("instance_name");
                outerSelects.add("job_name");
                innerSelects.add("i.name as instance_name");
                innerSelects.add("job_name");
                groupByStrings.add("instance_name");
                groupByStrings.add("job_name");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("job_name"));
                break;
            case instance_name:
                outerSelects.add("instance_name");
                innerSelects.add("i.name as instance_name");
                groupByStrings.add("instance_name");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("instance_name"));
                break;
            case cicd_job_id:
                outerSelects.addAll(List.of("cicd_job_id", "instance_name", "job_name"));
                innerSelects.addAll(List.of("r.cicd_job_id as cicd_job_id", "i.name as instance_name", "job_name"));
                groupByStrings.addAll(List.of("cicd_job_id", "instance_name", "job_name"));
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("job_name"));
                break;
            case job_normalized_full_name:
                outerSelects.addAll(List.of(acrossOrStack.toString(), "(array_agg(cicd_job_id))[1]  as cicd_job_id"));
                innerSelects.addAll(List.of(acrossOrStack.toString(),"j.id as cicd_job_id"));
                groupByStrings.addAll(List.of(acrossOrStack.toString()));
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf(acrossOrStack.name()));
                break;
            case triage_rule:
                outerSelects.addAll(List.of("triage_rule", "triage_rule_id"));
                innerSelects.addAll(List.of("triage_rule", "triage_rule_id"));
                groupByStrings.addAll(List.of("triage_rule", "triage_rule_id"));
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("triage_rule"));
                break;
            case project_name:
            case job_name:
            case cicd_user_id:
                outerSelects.add(acrossOrStack.toString());
                innerSelects.add(acrossOrStack.toString());
                groupByStrings.add(acrossOrStack.toString());
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf(acrossOrStack.name()));
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
                AggTimeQueryHelper.AggTimeQuery trendAggQuery =
                        AggTimeQueryHelper.getAggTimeQuery("end_time", acrossOrStack.toString(), aggInterval != null ? aggInterval.toString() : null, false);
                innerSelects.add(trendAggQuery.getHelperColumn().replaceFirst(",", ""));
                groupByStrings.add(trendAggQuery.getGroupBy());
                outerSelects.add(trendAggQuery.getSelect());
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, acrossOrStack.name());
                break;
            case stage_name:
                outerSelects.add("stage_name");
                innerSelects.add("cicd_job_run_stage.name as stage_name");
                groupByStrings.add("stage_name");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("stage_name"));
                break;
            case step_name:
                outerSelects.add("step_name");
                innerSelects.add("display_name as step_name");
                groupByStrings.add("step_name");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("step_name"));
                break;
            case stage_status:
                outerSelects.add("stage_status");
                innerSelects.add("cicd_job_run_stage.result as stage_status");
                groupByStrings.add("stage_status");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("stage_status"));
                break;
            case step_status:
                outerSelects.add("step_status");
                innerSelects.add("cicd_job_run_stage_steps.result as step_status");
                groupByStrings.add("step_status");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("step_status"));
                break;
            default:
                Validate.notNull(null, "Invalid across or stack field provided.");
        }
        if (MapUtils.isNotEmpty(sortBy) && orderByString.isEmpty()) {
            if (!sortBy.keySet().stream().findFirst().get().equals(acrossOrStack.toString()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field ");
        }
    }

    private void parseCiCdJobRunsCalculation(CiCdJobRunsFilter.CALCULATION calculation, CiCdJobRunsFilter.DISTINCT across, List<CiCdJobRunsFilter.DISTINCT> stacks, StringBuffer calculationComponentStringBuffer, Map<String, SortingOrder> sortBy, Set<String> orderByString) {
        switch (calculation) {
            case duration:
                calculationComponentStringBuffer.append("min(duration) as mn, max(duration) as mx, sum(duration) as sm, count(*) as ct,"
                        + " PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY duration) as md");
                parseSortBy(calculation.toString(), orderByString, sortBy, "md", true);
                break;
            default:
                if (across == CiCdJobRunsFilter.DISTINCT.triage_rule && CollectionUtils.isEmpty(stacks))
                    calculationComponentStringBuffer.append("COUNT(id) - 1 as ct");
                else
                    calculationComponentStringBuffer.append("COUNT(id) as ct");
                parseSortBy(calculation.toString(), orderByString, sortBy, "ct", true);
                break;
        }
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateCiCdJobRuns(String company,
                                                                              CiCdJobRunsFilter filter,
                                                                              Map<String, SortingOrder> sortBy,
                                                                              boolean valuesOnly) throws SQLException {
        return groupByAndCalculateCiCdJobRuns(company, filter.toBuilder().sortBy(sortBy).build(), valuesOnly, null);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateCiCdJobRuns(String company,
                                                                              CiCdJobRunsFilter filter,
                                                                              Map<String, SortingOrder> sortBy,
                                                                              boolean valuesOnly,
                                                                              OUConfiguration ouConfig) throws SQLException {
        return groupByAndCalculateCiCdJobRuns(company, filter.toBuilder().sortBy(sortBy).build(), valuesOnly, ouConfig);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateCiCdJobRuns(String company,
                                                                              CiCdJobRunsFilter filter,
                                                                              boolean valuesOnly) throws SQLException {
        return groupByAndCalculateCiCdJobRuns(company, filter, valuesOnly, null);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateCiCdJobRuns(String company,
                                                                              CiCdJobRunsFilter filter,
                                                                              boolean valuesOnly,
                                                                              OUConfiguration ouConfig) throws SQLException {
        Map<String, SortingOrder> sortBy = filter.getSortBy();
        DbListResponse<DbAggregationResult> resultsWithoutStacks = groupByAndCalculateCiCdJobRunsWithoutStacks(company, filter, sortBy, valuesOnly, ouConfig);
        if (CollectionUtils.isEmpty(resultsWithoutStacks.getRecords())) {
            return resultsWithoutStacks;
        }
        if (CollectionUtils.isEmpty(filter.getStacks())) {
            return resultsWithoutStacks;
        }
        if(filter.getAcross().equals(CiCdJobRunsFilter.DISTINCT.trend) ||
                filter.getAcross().equals(CiCdJobRunsFilter.DISTINCT.job_end)) {
            return groupByAndCalculateDateIntervals(company, filter, resultsWithoutStacks, ouConfig);
        }
        CiCdJobRunsFilter.CALCULATION calculation = CiCdJobRunsFilter.getSanitizedCalculation(filter);

        StringBuilder orderByStringBuilder = new StringBuilder();
        StringBuffer calculationComponentStringBuffer = new StringBuffer();
        Set<String> outerSelects = new HashSet<>();
        Set<String> innerSelects = new HashSet<>();
        Set<String> groupByStrings = new HashSet<>();
        Set<String> orderByString = new HashSet<>();

        //Calculation should be parsed before across as orderBy could get overwritten in parse across
        parseCiCdJobRunsCalculation(calculation, filter.getAcross(), filter.getStacks(), calculationComponentStringBuffer, sortBy, orderByString);

        parseCiCdJobRunsAcrossOrStack(filter.getAcross(), filter.getAggInterval(), outerSelects, innerSelects, groupByStrings, sortBy, orderByString);
        if (CollectionUtils.isNotEmpty(filter.getStacks())) {
            for (CiCdJobRunsFilter.DISTINCT stack : filter.getStacks()) {
                // For stack do not send orderBy - orderBy is overwritten only for across, not for stacks
                parseCiCdJobRunsAcrossOrStack(stack, filter.getAggInterval(), outerSelects, innerSelects, groupByStrings, sortBy, orderByString);
            }
        }

        String outerSelect = String.join(",", outerSelects);
        String innerSelect = String.join(",", innerSelects);
        String groupByString = String.join(",", groupByStrings);
        orderByStringBuilder.append(String.join(",", orderByString));

        Map<String, Object> params = new HashMap<>();
        String nullsPosition = SortingOrder.getNullsPosition(SortingOrder.fromString(orderByStringBuilder.toString().split(" ")[1]));
        String subQuery = getSubQuery(company, params, filter, innerSelect, resultsWithoutStacks, false, true, ouConfig);
        String sql = "SELECT " + outerSelect + "," + calculationComponentStringBuffer.toString() + " FROM (" + subQuery + " ) a"
                + " GROUP BY " + groupByString + " ORDER BY " + orderByStringBuilder.toString() + " NULLS " + nullsPosition;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        List<DbAggregationResultStacksWrapper> resultsWithStacks = template.query(sql, params, DbCiCdConverters.distinctJobRunsAggsWithStacksMapper(
                filter));
        List<DbAggregationResult> results = DbAggregationResultsMerger.mergeResultsWithAndWithoutStacks(resultsWithoutStacks.getRecords(), resultsWithStacks);
        return DbListResponse.of(results, results.size());
    }

    private DbListResponse<DbAggregationResult> groupByAndCalculateDateIntervals(String company,
                                                                                 CiCdJobRunsFilter filter,
                                                                                 DbListResponse<DbAggregationResult> resultsWithoutStacks,
                                                                                 OUConfiguration ouConfig) {
        CiCdJobRunsFilter.DISTINCT stacks = filter.getStacks().get(0);
        Stream<DbAggregationResult> dbAggregationResultStream = resultsWithoutStacks
                .getRecords()
                .parallelStream()
                .map(row -> {
                    CiCdJobRunsFilter newFilter = CiCdJobRunsFilter.builder().build();
                    CiCdJobRunsFilter.CiCdJobRunsFilterBuilder filterBuilder = filter.toBuilder();
                    switch (filter.getAcross()) {
                        case trend:
                        case job_end:
                            newFilter = parseFiltersForStacks(stacks, row, filter.getAggInterval() == null ? CICD_AGG_INTERVAL.day : filter.getAggInterval(),
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

    private void applyAcrossToStacks(CiCdJobRunsFilter filter,
                                     DbListResponse<DbAggregationResult> resultsWithoutStacks,
                                     Map<String, Object> params, List<String> conditions) {
        switch (filter.getAcross()) {
            case cicd_job_id:
                conditions.add("cicd_job_id in (:invalues)");
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
                conditions.add((filter.getAcross() != CiCdJobRunsFilter.DISTINCT.instance_name ? filter.getAcross().toString() : "i.name") + " in (:invalues)");
                params.put("invalues", resultsWithoutStacks.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()));
                break;
            case triage_rule:
                conditions.add("jen.triage_rule in (:invalues)");
                params.put("invalues", resultsWithoutStacks.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()));
                break;
            default:
                Validate.notNull(null, "Invalid across field provided.");
        }
    }
    // endregion

    private CiCdJobRunsFilter parseFiltersForStacks(CiCdJobRunsFilter.DISTINCT stack, DbAggregationResult row,
                                                    CICD_AGG_INTERVAL aggInterval,
                                                    CiCdJobRunsFilter.DISTINCT across,
                                                    CiCdJobRunsFilter.CiCdJobRunsFilterBuilder ciCdJobRunsFilterBuilder) {
        ImmutablePair<Long, Long> timeRange = getImmutablePair(row, aggInterval);
        switch (across) {
            case trend:
                ciCdJobRunsFilterBuilder
                        .startTimeRange(timeRange)
                        .across(stack)
                        .build();
                break;
            case job_end:
                ciCdJobRunsFilterBuilder
                        .endTimeRange(timeRange)
                        .across(stack)
                        .build();
                break;
        default:
        }
        return ciCdJobRunsFilterBuilder.build();
    }

    public Map<String, List<String>> createWhereClauseAndUpdateParamsJobRuns(String company, CiCdJobRunsFilter filter,
                                                                             Map<String, Object> params, String suffix, Boolean isList,
                                                                             OUConfiguration ouConfig) {
        List<String> criterias = new ArrayList<>();
        List<String> triageCriterias = new ArrayList<>();
        String paramSuffix = suffix == null ? "" : "_" + suffix;
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            criterias.add("i.integration_id IN (:integration_ids" + paramSuffix + ")");
            params.put("integration_ids" + paramSuffix, filter.getIntegrationIds().stream()
                    .map(NumberUtils::toInteger).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getCicdUserIds()) || OrgUnitHelper.doesOuConfigHaveCiCdUsers(ouConfig)) { // OU: user
            var columnName = "r.cicd_user_id" + paramSuffix;
            var columnNameParam = columnName + paramSuffix;
            if(OrgUnitHelper.doesOuConfigHaveCiCdUsers(ouConfig)){
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params,
                        IntegrationType.getCICDIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    criterias.add(MessageFormat.format("{0} IN (SELECT cloud_id FROM ({1}) l)", "r.cicd_user_id", usersSelect));
                }
            }
            else if(CollectionUtils.isNotEmpty(filter.getCicdUserIds())){
                TeamUtils.addUsersCondition(company, criterias, params, "r.cicd_user_id", columnNameParam, false, filter.getCicdUserIds(), CICD_APPLICATIONS);
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCiCdUserIds())) {
            String columnNameParam = "r.cicd_user_id" + paramSuffix;
            TeamUtils.addUsersCondition(company, criterias, params, "r.cicd_user_id",
                    columnNameParam, false, filter.getExcludeCiCdUserIds(), CICD_APPLICATIONS, true);
        }
        if (CollectionUtils.isNotEmpty(filter.getTypes())) {
            criterias.add("i.type IN (:types" + paramSuffix + ")");
            params.put("types" + paramSuffix, filter.getTypes().stream()
                    .map(CICD_TYPE::toString)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTypes())) {
            criterias.add("i.type NOT IN (:excl_types" + paramSuffix + ")");
            params.put("excl_types" + paramSuffix, filter.getExcludeTypes().stream()
                    .map(CICD_TYPE::toString)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getProjects())) {
            criterias.add("j.project_name IN (:projects" + paramSuffix + ")");
            params.put("projects" + paramSuffix, filter.getProjects());
        }
        if (CollectionUtils.isNotEmpty(filter.getTriageRuleNames())) {
            criterias.add("jen.triage_rule IN (:triage_rules" + paramSuffix + ")");
            triageCriterias.add("name IN (:triage_rules" + paramSuffix + ")");
            params.put("triage_rules" + paramSuffix, filter.getTriageRuleNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getTriageRuleIds())) {
            criterias.add("jen.triage_rule_id IN (:triage_rule_ids" + paramSuffix + ")");
            triageCriterias.add("t.id IN (:triage_rule_ids" + paramSuffix + ")");
            params.put("triage_rule_ids" + paramSuffix, filter.getTriageRuleIds());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTriageRuleNames())) {
            criterias.add("jen.triage_rule NOT IN (:excl_triage_rules" + paramSuffix + ")");
            triageCriterias.add("name NOT IN (:excl_triage_rules" + paramSuffix + ")");
            params.put("excl_triage_rules" + paramSuffix, filter.getExcludeTriageRuleNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTriageRuleIds())) {
            criterias.add("jen.triage_rule_id NOT IN (:excl_triage_rule_ids" + paramSuffix + ")");
            triageCriterias.add("t.id NOT IN (:excl_triage_rule_ids" + paramSuffix + ")");
            params.put("excl_triage_rule_ids" + paramSuffix, filter.getExcludeTriageRuleIds());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeProjects())) {
            criterias.add("j.project_name NOT IN (:excl_projects" + paramSuffix + ")");
            params.put("excl_projects" + paramSuffix, filter.getExcludeProjects());
        }
        if (CollectionUtils.isNotEmpty(filter.getJobNames())) {
            criterias.add("j.job_name IN (:job_names" + paramSuffix + ")");
            params.put("job_names" + paramSuffix, filter.getJobNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeJobNames())) {
            criterias.add("j.job_name NOT IN (:excl_job_names" + paramSuffix + ")");
            params.put("excl_job_names" + paramSuffix, filter.getExcludeJobNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getJobNormalizedFullNames())) {
            criterias.add("j.job_normalized_full_name IN (:job_normalized_full_names" + paramSuffix + ")");
            params.put("job_normalized_full_names" + paramSuffix, filter.getJobNormalizedFullNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeJobNormalizedFullNames())) {
            criterias.add("j.job_normalized_full_name NOT IN (:excl_job_normalized_full_names" + paramSuffix + ")");
            params.put("excl_job_normalized_full_names" + paramSuffix, filter.getExcludeJobNormalizedFullNames());
        }
        if (filter.getStartTimeRange() != null) {
            ImmutablePair<Long, Long> startTimeRange = filter.getStartTimeRange();
            if (startTimeRange.getLeft() != null) {
                criterias.add("r.start_time >= TO_TIMESTAMP(:start_time_start" + paramSuffix + ")");
                params.put("start_time_start" + paramSuffix, startTimeRange.getLeft());
            }
            if (startTimeRange.getRight() != null) {
                criterias.add("r.start_time <= TO_TIMESTAMP(:start_time_end" + paramSuffix + ")");
                params.put("start_time_end" + paramSuffix, startTimeRange.getRight());
            }
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
                criterias.add("( " + String.join(" OR ", qualifiedJobNamesCriterea) + " )");
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
                criterias.add("( " + String.join(" OR ", excludeQualifiedJobNamesCriterea) + " )");
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getJobStatuses())) {
            criterias.add("r.status IN (:job_statuses" + paramSuffix + ")");
            params.put("job_statuses" + paramSuffix, filter.getJobStatuses());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeJobStatuses())) {
            criterias.add("r.status NOT IN (:excl_job_statuses" + paramSuffix + ")");
            params.put("excl_job_statuses" + paramSuffix, filter.getExcludeJobStatuses());
        }
        if (CollectionUtils.isNotEmpty(filter.getInstanceNames())) {
            criterias.add("i.name IN (:instance_names" + paramSuffix + ")");
            params.put("instance_names" + paramSuffix, filter.getInstanceNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeInstanceNames())) {
            criterias.add("i.name NOT IN (:excl_instance_names" + paramSuffix + ")");
            params.put("excl_instance_names" + paramSuffix, filter.getExcludeInstanceNames());
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
                criterias.add("( " + String.join(" OR ", parametersCriterea) + " )");
            }
        }
        if(filter.getIsCiJob() != null && filter.getIsCdJob() != null){
            criterias.add("(r.cd = :is_cd" + paramSuffix + " OR r.ci = :is_ci" + paramSuffix + ")");
            params.put("is_cd" + paramSuffix, filter.getIsCdJob());
            params.put("is_ci" + paramSuffix, filter.getIsCiJob());
        } else if (filter.getIsCiJob() != null){
            criterias.add("r.ci = :is_ci" + paramSuffix);
            params.put("is_ci" + paramSuffix, filter.getIsCiJob());
        } else if (filter.getIsCdJob() != null) {
            criterias.add("r.cd = :is_cd" + paramSuffix);
            params.put("is_cd" + paramSuffix, filter.getIsCdJob());
        }
        if (filter.getEndTimeRange() != null) {
            ImmutablePair<Long, Long> endTimeRange = filter.getEndTimeRange();
            if (endTimeRange.getLeft() != null) {
                criterias.add("r.end_time >= to_timestamp(:end_time_start" + paramSuffix + ")");
                params.put("end_time_start" + paramSuffix, endTimeRange.getLeft());
            }
            if (endTimeRange.getRight() != null) {
                criterias.add("r.end_time <= to_timestamp(:end_time_end" + paramSuffix + ")");
                params.put("end_time_end" + paramSuffix, endTimeRange.getRight());
            }
        }
        Map<String, Map<String, String>> partialMatchMap = filter.getPartialMatch();
        if (MapUtils.isNotEmpty(partialMatchMap)) {
            CriteriaUtils.addPartialMatchClause(partialMatchMap, criterias, params, null, CICD_PARTIAL_MATCH_COLUMNS, Collections.emptySet(), paramSuffix);
        }
        int acrossCount = (filter.getAcrossCount() != null) ? filter.getAcrossCount() : 90;
        if (isList && (CiCdJobRunsFilter.DISTINCT.trend == filter.getAcross() ||
                CollectionUtils.isNotEmpty(filter.getStacks())
                        && filter.getStacks().get(0).equals(CiCdJobRunsFilter.DISTINCT.trend))
                && filter.getEndTimeRange() !=  null && filter.getStartTimeRange() != null) {
            criterias.add("r.start_time >= to_timestamp(:start_time_from" + paramSuffix + ")");
            params.put("start_time_from" + paramSuffix, Instant.now().minus(acrossCount, ChronoUnit.DAYS).getEpochSecond());
        }
        if (CiCdJobRunsFilter.DISTINCT.instance_name == filter.getAcross()) {
            criterias.add("i.name IS NOT NULL");
        }
        if (CiCdJobRunsFilter.DISTINCT.job_normalized_full_name == filter.getAcross()) {
            criterias.add("j.job_normalized_full_name IS NOT NULL");
        }
        if(CiCdJobRunsFilter.DISTINCT.triage_rule == filter.getAcross() ||
                (CollectionUtils.isNotEmpty(filter.getStacks()) && filter.getStacks().contains(CiCdJobRunsFilter.DISTINCT.triage_rule))){
            criterias.add("triage_rule IS NOT NULL");
        }

        ciCdMetadataConditionBuilder.prepareMetadataConditions(filter, params, null, criterias);

        ciCdPartialMatchConditionBuilder.preparePartialMatchConditions(filter, params, criterias, paramSuffix, CICD_PARTIAL_MATCH_COLUMNS);

        ciCdStageStepConditionBuilder.prepareCiCdStageStepConditions(filter, params, criterias, paramSuffix, company);

        return Map.of(CICD_CONDITIONS, criterias,
                TRIAGE_CONDITIONS, triageCriterias);
    }

    // endregion

    // region Job Runs - List

    public DbListResponse<CICDJobRunDTO> listCiCdJobRuns(String company, CiCdJobRunsFilter filter, Map<String, SortingOrder> sortBy,
                                                         Integer pageNumber, Integer pageSize) throws SQLException {
        return listCiCdJobRuns(company, filter.toBuilder().sortBy(sortBy).build(), pageNumber, pageSize);
    }

    public DbListResponse<CICDJobRunDTO> listCiCdJobRuns(String company, CiCdJobRunsFilter filter,
                                                         Integer pageNumber, Integer pageSize) throws SQLException {
        return listCiCdJobRuns(company, filter, pageNumber, pageSize, null);
    }

    public DbListResponse<CICDJobRunDTO> listCiCdJobRuns(String company, CiCdJobRunsFilter filter,
                                                         Integer pageNumber, Integer pageSize,
                                                         OUConfiguration ouConfig) throws SQLException {
        Map<String, Object> params = new HashMap<>();
        String subQuery = getSubQuery(company, params, filter, null, null, true, false, ouConfig);
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        List<String> orderBy = getListSortBy(filter.getSortBy());
        String nullsPosition = SortingOrder.getNullsPosition(SortingOrder.fromString(orderBy.get(0).split(" ")[1]));
        String sql = "SELECT * FROM ( " + subQuery + ") a ORDER BY " + String.join(",", orderBy) + " NULLS " + nullsPosition + " OFFSET :skip LIMIT :limit";
        String countSQL = "SELECT COUNT(*) FROM ( " + subQuery + " ) x";
        log.debug("countSQL = {}", countSQL);
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<CICDJobRunDTO> results = template.query(sql, params, DbCiCdConverters.jobRunsListMapper());
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

    public DbListResponse<CICDJobRunDTO> listCiCdJobRunsForDora(String company, CiCdJobRunsFilter filter,
                                                         Integer pageNumber, Integer pageSize,
                                                         OUConfiguration ouConfig,
                                                                List<String> jobIds) throws SQLException {
        Map<String, Object> params = new HashMap<>();
        String subQuery = getSubQuery(company, params, filter, null, null, true, false, ouConfig);
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        if (CollectionUtils.isNotEmpty(jobIds)) {
            params.put("cicd_job_ids", jobIds.stream().map(UUID::fromString).collect(Collectors.toList()));
            subQuery = subQuery +" AND cicd_job_id IN (:cicd_job_ids)";
        }
        List<String> orderBy = getListSortBy(filter.getSortBy());

        String nullsPosition = SortingOrder.getNullsPosition(SortingOrder.fromString(orderBy.get(0).split(" ")[1]));
        String sql = "SELECT * FROM ( " + subQuery + ") a ORDER BY " + String.join(",", orderBy) + " NULLS " + nullsPosition + " OFFSET :skip LIMIT :limit";
        String countSQL = "SELECT COUNT(*) FROM ( " + subQuery + " ) x";
        log.debug("countSQL = {}", countSQL);
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<CICDJobRunDTO> results = template.query(sql, params, DbCiCdConverters.doraJobRunsListMapper());
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

    private String getSubQuery(String company, Map<String, Object> params, CiCdJobRunsFilter filter, String innerSelect,
                               DbListResponse<DbAggregationResult> resultsWithoutStacks,
                               Boolean isList, Boolean hasStacks,
                               OUConfiguration ouConfig) {
        Map<Integer, Map<String, Object>> productFilters = null;
        try {
            productFilters = ciCdFilterParserCommons.getProductFilters(company, filter.getOrgProductsIds());
        } catch (SQLException throwables) {
            log.error("Error encountered while fetching product filters for company " + company, throwables);
        }
        if (productFilters == null || MapUtils.isEmpty(productFilters)) {
            Map<String, List<String>> conditions = createWhereClauseAndUpdateParamsJobRuns(company, filter, params, null, isList, ouConfig);
            if (hasStacks) {
                applyAcrossToStacks(filter, resultsWithoutStacks, params, conditions.get(CICD_CONDITIONS));
            }
            boolean useAllTriage = filter.getAcross() == CiCdJobRunsFilter.DISTINCT.triage_rule;
            return ciCdJobRunsFilterParsers.getSqlStmt(company, conditions, innerSelect, isList, filter, useAllTriage);
        } else {
            AtomicInteger suffix = new AtomicInteger();
            Map<Integer, Map<String, Object>> finalProductFilters = productFilters;
            List<String> filterSqlStmts = MapUtils.emptyIfNull(productFilters).keySet().stream()
                    .map(integrationId -> {
                        CiCdJobRunsFilter newJobRunsFilter = ciCdJobRunsFilterParsers.merge(integrationId, filter, finalProductFilters.get(integrationId));
                        Map<String, List<String>> conditions = createWhereClauseAndUpdateParamsJobRuns(company, newJobRunsFilter, params,
                                String.valueOf(suffix.incrementAndGet()), isList, ouConfig);
                        if (hasStacks) {
                            applyAcrossToStacks(newJobRunsFilter, resultsWithoutStacks, params, conditions.get(CICD_CONDITIONS));
                        }
                        return ciCdJobRunsFilterParsers.getSqlStmt(company, conditions, innerSelect, isList, newJobRunsFilter, false);
                    })
                    .collect(Collectors.toList());
            return String.join(" UNION ", filterSqlStmts);
        }
    }

    private List<CICDJobRunDTO> mergeJobRunsAndParam(final List<CICDJobRunDTO> jobRuns, final Map<UUID, List<CICDJobRun.JobRunParam>> jobRunParams) {
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
