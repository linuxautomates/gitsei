package io.levelops.commons.databases.services;

import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.converters.DbCiCdScmConverters;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CICDScmJobRunDTO;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.cicd_scm.DBDummyObj;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobQualifiedName;
import io.levelops.commons.databases.models.filters.CiCdJobRunParameter;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.filters.CiCdScmFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.DbAggregationResultStacksWrapper;
import io.levelops.commons.databases.models.response.DbAggregationResultsMerger;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.parsers.CiCdFilterParserCommons;
import io.levelops.commons.databases.services.parsers.CiCdScmFilterParsers;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.databases.utils.CriteriaUtils;
import io.levelops.commons.databases.utils.TeamUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.ingestion.models.IntegrationType;
import lombok.Builder;
import lombok.Value;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.databases.models.filters.CiCdUtils.getListSortBy;
import static io.levelops.commons.databases.models.filters.CiCdUtils.lowerOf;
import static io.levelops.commons.databases.models.filters.CiCdUtils.parseSortBy;
import static io.levelops.commons.databases.services.CiCdAggsService.CICD_APPLICATIONS;
import static io.levelops.commons.databases.services.CiCdAggsService.CICD_CONDITIONS;

@Log4j2
@Service
public class CiCdScmCombinedAggsService extends DatabaseService<DBDummyObj> {

    private static final Set<String> CICD_SCM_PARTIAL_MATCH_COLUMNS = Set.of("job_normalized_full_name");
    private static final List<String> DERIVED_FIELDS = List.of("initial_commit_to_deploy_time", "lines_modified", "files_modified");

    private final NamedParameterJdbcTemplate template;
    private final CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private final CiCdFilterParserCommons ciCdFilterParserCommons;
    private final CiCdScmFilterParsers ciCdScmFilterParsers;
    private final CiCdAggsService ciCdAggsService;

    // region CSTOR
    @Autowired
    public CiCdScmCombinedAggsService(DataSource dataSource, CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        this.ciCdJobRunsDatabaseService = ciCdJobRunsDatabaseService;
        ProductsDatabaseService productsDatabaseService = new ProductsDatabaseService(dataSource, DefaultObjectMapper.get());
        this.ciCdFilterParserCommons = new CiCdFilterParserCommons(productsDatabaseService);
        this.ciCdScmFilterParsers = new CiCdScmFilterParsers(DefaultObjectMapper.get());
        ciCdAggsService = new CiCdAggsService(dataSource);
    }
    // endregion

    // region Unused
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(CiCdJobsDatabaseService.class, CiCdJobRunsDatabaseService.class, ScmAggService.class, CiCdScmMappingService.class);
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
    // endregion

    // region CiCd Scm Combined - Aggs
    private void parseCalculation(CiCdScmFilter.CALCULATION calculation, StringBuffer calculationComponentOuterSB,
                                  StringBuffer calculationComponentInnerSB, Map<String, SortingOrder> sortBy,
                                  Set<String> orderByString) {
        switch (calculation) {
            case lead_time:
                calculationComponentOuterSB.append("min(lead) as mn, max(lead) as mx, sum(lead) as sm, count(*) as ct," +
                        " PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY lead) as md");
                calculationComponentInnerSB.append("GREATEST(EXTRACT(EPOCH FROM (start_time - committed_at)), 0) as lead");
                parseSortBy(calculation.toString(), orderByString, sortBy, "md", true);
                break;
            case change_volume:
                calculationComponentOuterSB.append("sum(files_ct) as fc, sum(additions) as la, sum(deletions) as ld");
                calculationComponentInnerSB.append("files_ct,additions,deletions");
                parseSortBy(calculation.toString(), orderByString, sortBy, "fc", true);
                break;
            default:
                calculationComponentOuterSB.append("count(*) as ct");
                calculationComponentInnerSB.append("run_id");
                parseSortBy(calculation.toString(), orderByString, sortBy, "ct", true);
                break;
        }
    }


    private void parseAcrossOrStack(CiCdScmFilter.DISTINCT acrossOrStack, CICD_AGG_INTERVAL aggInterval,
                                    Set<String> outerSelects, Set<String> innerSelects,
                                    Set<String> groupByStrings, Map<String, SortingOrder> sortBy, Set<String> orderByString) {
        switch (acrossOrStack) {
            case job_status:
                outerSelects.add("status");
                innerSelects.add("status");
                groupByStrings.add("status");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("status"));
                break;
            case author:
                outerSelects.add(acrossOrStack.toString());
                innerSelects.add("j.updated_at"); //Dummy field selected to mainly keep query create formatting easy
                groupByStrings.add(acrossOrStack.toString());
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf(acrossOrStack.toString()));
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
            case project_name:
            case job_name:
            case cicd_user_id:
            case job_normalized_full_name:
                outerSelects.add(acrossOrStack.toString());
                innerSelects.add(acrossOrStack.toString());
                groupByStrings.add(acrossOrStack.toString());
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf(acrossOrStack.toString()));
                break;
            case instance_name:
                outerSelects.add("instance_name");
                innerSelects.add("i.name as instance_name");
                groupByStrings.add("instance_name");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("instance_name"));
                break;
            case repo:
                outerSelects.add("unnest(repo_id) AS repo_ids");
                groupByStrings.add("repo_ids");
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, lowerOf("repo_ids"));
                break;
            case trend:
                boolean sortAscending = false;
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
                parseSortBy(acrossOrStack.toString(), orderByString, sortBy, "job_end");
                break;
            default:
                Validate.notNull(null, "Invalid across field provided.");
        }
        if (MapUtils.isNotEmpty(sortBy) && orderByString.isEmpty()) {
            if (!sortBy.keySet().stream().findFirst().get().equals(acrossOrStack.toString()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field ");
        }
    }

    private String getSubQuery(String company, Map<String, Object> params, CiCdScmFilter filter, String innerSelect,
                               DbListResponse<DbAggregationResult> resultsWithoutStacks, Boolean hasStacks,
                               Boolean isList,
                               OUConfiguration ouConfig) {
        if (filter.getOrgProductsIds() == null || CollectionUtils.isEmpty(filter.getOrgProductsIds())) {
            CicdScmSqlCriteria conditions = createWhereClauseAndUpdateParams(company, filter, params, isList, null, ouConfig);
            if (hasStacks) {
                applyAcrossForStacks(filter, resultsWithoutStacks, params, conditions);
            }
            return ciCdScmFilterParsers.getSqlStmt(company, conditions, innerSelect, isList);
        }
        Map<Integer, Map<String, Object>> productFilters = null;
        try {
            productFilters = ciCdFilterParserCommons.getProductFilters(company, filter.getOrgProductsIds());
        } catch (SQLException throwables) {
            log.error("Error encountered while fetching product filters for company " + company, throwables);
        }
        AtomicInteger suffix = new AtomicInteger();
        Map<Integer, Map<String, Object>> integFiltersMap = productFilters;
        List<String> filterSqlStmts = CollectionUtils.emptyIfNull(productFilters.keySet()).stream()
                .map(integrationId -> {
                    CiCdScmFilter newCiCdScmFilter = ciCdScmFilterParsers.merge(integrationId, filter, integFiltersMap.get(integrationId));
                    CicdScmSqlCriteria conditions = createWhereClauseAndUpdateParams(company, newCiCdScmFilter, params, isList,
                            String.valueOf(suffix.getAndIncrement()), ouConfig);
                    if (hasStacks) {
                        applyAcrossForStacks(newCiCdScmFilter, resultsWithoutStacks, params, conditions);
                    }
                    return ciCdScmFilterParsers.getSqlStmt(company, conditions, innerSelect, isList);
                }).collect(Collectors.toList());
        return String.join(" UNION ", filterSqlStmts);

    }

    public DbListResponse<DbAggregationResult> computeDeployJobChangeVolume(String company,
                                                                            CiCdJobRunsFilter deployJobFilter,
                                                                            CiCdScmFilter buildJobFilter,
                                                                            CiCdScmFilter.DISTINCT across,
                                                                            CICD_AGG_INTERVAL interval,
                                                                            OUConfiguration ouConfig) {
        Map<String, Object> params = new HashMap<>();

        List<String> deployConditions = ciCdAggsService.createWhereClauseAndUpdateParamsJobRuns(company, deployJobFilter, params, "deploy", false, ouConfig).get(CICD_CONDITIONS);
        CicdScmSqlCriteria buildConditions = createWhereClauseAndUpdateParams(company, buildJobFilter, params, false, "build", ouConfig);

        String buildCicdWhereClause = (CollectionUtils.isEmpty(buildConditions.getCicdCriteria())) ? ""
                : " WHERE " + String.join(" AND ", buildConditions.getCicdCriteria());
        String buildScmWhereClause = (CollectionUtils.isEmpty(buildConditions.getScmCriteria())) ? ""
                : " WHERE " + String.join(" AND ", buildConditions.getScmCriteria());

        String deployCicdWhereClause = (CollectionUtils.isEmpty(deployConditions)) ? ""
                : " AND " + String.join(" AND ", deployConditions);

        StringBuffer calculationComponentOuterSB = new StringBuffer();
        StringBuffer calculationComponentInnerSB = new StringBuffer();
        Set<String> orderByStrings = new HashSet<>();
        parseCalculation(CiCdScmFilter.CALCULATION.change_volume, calculationComponentOuterSB, calculationComponentInnerSB, Map.of(), orderByStrings);

        Set<String> outerSelects = new HashSet<>();
        Set<String> innerSelects = new HashSet<>();
        Set<String> groupByStrings = new HashSet<>();
        parseAcrossOrStack(across, interval, outerSelects, innerSelects, groupByStrings, Map.of(), orderByStrings);
        String outerSelect = String.join(",", outerSelects);
        String innerSelect = String.join(",", innerSelects);
        String groupByString = String.join(",", groupByStrings);
        String selectJobEnd = "job_end_interval";

        String deploySql = "SELECT distinct deploy_run_id, " + calculationComponentInnerSB +
                ", changes,count(*) over (partition by deploy_run_id) as build_job_runs_count," + selectJobEnd + " FROM " +
                "(SELECT r.id as deploy_run_id,integration_id as deploy_integ_id" +
                ",job_name as deploy_job_name, start_time as deploy_start_time," +
                " LAG(start_time)  over (order by start_time) as span_start, " + innerSelect + " from "
                + company + ".cicd_jobs j, " + company + ".cicd_job_runs r, " +
                company + ".cicd_instances i where i.id = j.cicd_instance_id and" +
                " j.id = r.cicd_job_id " + deployCicdWhereClause + " ) as deploy_job_runs";

        String buildSql = "( SELECT * FROM (SELECT  r.id as run_id,start_time, j.job_name, cicd_job_id, job_run_number, status," +
                " end_time, duration, cicd_user_id, i.name as instance_name, i.id as instance_guid, " +
                " project_name, job_normalized_full_name, i.integration_id as cicd_integration_id, scm_url, scm_commit_ids" +
                " FROM " + company + ".cicd_job_runs as r\n" +
                " JOIN " + company + ".cicd_jobs as j on r.cicd_job_id = j.id \n" +
                " LEFT OUTER JOIN " + company + ".cicd_instances as i on j.cicd_instance_id = i.id\n" + buildCicdWhereClause + ") a " +
                "join " + company + ".cicd_scm_mapping as m on m.cicd_job_run_id = a.run_id\n" +
                "join " + company + ".scm_commits as c on c.id = m.commit_id " +
                buildScmWhereClause + ") as build_job_runs ";

        String sql = deploySql + " cross join " + buildSql + " WHERE deploy_job_runs.span_start is not null and " +
                " build_job_runs.end_time between deploy_job_runs.span_start and deploy_job_runs.deploy_start_time";
        String finalSql = "select " + outerSelect + "," + calculationComponentOuterSB + ", Sum(changes) as change_ct,sum(build_job_runs_count) as build_job_runs_count, count(DISTINCT deploy_run_id) as deploy_job_runs_count" +
                " from ( " + sql + " ) final group by " + groupByString;

        log.info("sql = " + finalSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        List<DbAggregationResult> results = template.query(finalSql, params, DbCiCdScmConverters.mapAggregationsResults(
                CiCdScmFilter.DISTINCT.job_end, CiCdScmFilter.CALCULATION.change_volume));
        return DbListResponse.of(results, results.size());
    }

    public DbListResponse<CICDScmJobRunDTO> computeDeployJobChangeVolumeDrillDown(String company,
                                                                                  CiCdJobRunsFilter deployJobFilter,
                                                                                  CiCdScmFilter buildJobFilter,
                                                                                  ImmutablePair<Long, Long> deployEndTimeRangePair,
                                                                                  Integer pageNumber, Integer pageSize,
                                                                                  OUConfiguration ouConfig) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        List<String> orderBy = getListSortBy(deployJobFilter.getSortBy());
        String nullsPosition = SortingOrder.getNullsPosition(SortingOrder.fromString(orderBy.get(0).split(" ")[1]));
        List<String> deployConditions = ciCdAggsService.createWhereClauseAndUpdateParamsJobRuns(company, deployJobFilter, params, "deploy", false, ouConfig).get(CICD_CONDITIONS);
        CicdScmSqlCriteria buildConditions = createWhereClauseAndUpdateParams(company, buildJobFilter, params, false, "build", ouConfig);

        String buildCicdWhereClause = (CollectionUtils.isEmpty(buildConditions.getCicdCriteria())) ? ""
                : " WHERE " + String.join(" AND ", buildConditions.getCicdCriteria());
        String buildScmWhereClause = (CollectionUtils.isEmpty(buildConditions.getScmCriteria())) ? ""
                : " WHERE " + String.join(" AND ", buildConditions.getScmCriteria());

        String deployCicdWhereClause = (CollectionUtils.isEmpty(deployConditions)) ? ""
                : " AND " + String.join(" AND ", deployConditions);

        String deploySql = "SELECT r.id AS deploy_run_id, integration_id AS deploy_integ_id, job_name AS deploy_job_name, " +
                " start_time AS deploy_start_time, Lag(start_time) OVER ( ORDER BY start_time) AS span_start, start_time, " +
                " j.job_name, cicd_job_id, job_run_number, status, end_time as deploy_end_time, duration, cicd_user_id, " +
                " i.NAME AS instance_name, i.id AS instance_guid," +
                " i.type AS cicd_instance_type, project_name, job_normalized_full_name " + " from "
                + company + ".cicd_jobs j, " + company + ".cicd_job_runs r, " +
                company + ".cicd_instances i where i.id = j.cicd_instance_id and" +
                " j.id = r.cicd_job_id " + deployCicdWhereClause + " ) as deploy_job_runs";

        String buildSql = "( SELECT * FROM (SELECT r.id AS run_id, i.integration_id AS cicd_integration_id, scm_url, " +
                " end_time, scm_commit_ids FROM " + company + ".cicd_job_runs as r\n" +
                " JOIN " + company + ".cicd_jobs as j on r.cicd_job_id = j.id \n" +
                " LEFT OUTER JOIN " + company + ".cicd_instances as i on j.cicd_instance_id = i.id\n" + buildCicdWhereClause + ") a " +
                " join " + company + ".cicd_scm_mapping as m on m.cicd_job_run_id = a.run_id\n" +
                " join " + company + ".scm_commits as c on c.id = m.commit_id " +
                buildScmWhereClause + ") as build_job_runs ";
        String deployTimeRangeSql = addDeployTimeRangeFilter(deployEndTimeRangePair);
        String baseSql = "SELECT * FROM( SELECT DISTINCT deploy_run_id, run_id as build_run_id, deploy_integ_id, deploy_job_name, " +
                " deploy_start_time, cicd_job_id, " +
                " job_run_number, status, deploy_end_time, duration, cicd_user_id, " +
                " instance_name, instance_guid, project_name, job_normalized_full_name, " +
                " cicd_instance_type, scm_url, scm_commit_ids " +
                " FROM (  " + deploySql + " CROSS JOIN " + buildSql + " WHERE deploy_job_runs.span_start is not null and " +
                " build_job_runs.end_time between deploy_job_runs.span_start and deploy_job_runs.deploy_start_time" +
                (StringUtils.isNotEmpty(deployTimeRangeSql) ? " AND " + deployTimeRangeSql : StringUtils.EMPTY) +
                ") x ";

        String finalSql = "SELECT *  FROM ( " + baseSql + " ) final ORDER BY " + orderBy.get(0)
                + " NULLS " + nullsPosition;
        List<CICDScmJobRunDTO> results = List.of();
        if (pageSize > 0) {
            log.info("sql = " + finalSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(finalSql, params, DbCiCdScmConverters.codeVsDeployListRowMapper());
        }
        List<CICDScmJobRunDTO> partialResults = results;
        String subQuery = getSubQuery(company, params, buildJobFilter, null, null, false, true, ouConfig);
        Set<UUID> deployRunIds = results.stream().map(CICDScmJobRunDTO::getId).collect(Collectors.toSet());
        List<CICDScmJobRunDTO> finalDeployJobsList = new ArrayList<>();
        deployRunIds.forEach(deployRunId -> {
            List<UUID> buildIds = partialResults.stream().filter(row -> row.getId().equals(deployRunId))
                    .map(CICDScmJobRunDTO::getBuildId).collect(Collectors.toList());
            Map<UUID, List<CICDJobRun.JobRunParam>> jobRunParams = Map.of();
            try {
                jobRunParams = ciCdJobRunsDatabaseService.getJobRunParams(company, buildIds);
            } catch (SQLException e) {
                log.error("Error in getting job run params: " + e);
            }
            List<CICDScmJobRunDTO.ScmCommit> scmCommitDetails = getJobRunCommits(buildIds, subQuery, params);
            mergeResults(partialResults, deployRunId, jobRunParams, scmCommitDetails, buildIds, finalDeployJobsList);
        });
        List<CICDScmJobRunDTO> paginatedDeploysResponse = getPaginatedResponse(finalDeployJobsList, pageNumber, pageSize);
        return DbListResponse.of(paginatedDeploysResponse, finalDeployJobsList.size());

    }

    private List<CICDScmJobRunDTO> getPaginatedResponse(List<CICDScmJobRunDTO> finalDeployJobsList, Integer pageNumber, Integer pageSize) {
        if (finalDeployJobsList == null)
            return Collections.emptyList();
        if (pageSize == null || pageSize == 0) {
            pageSize = finalDeployJobsList.size();
        }
        if (finalDeployJobsList.size() > 0) {
            int fromIndex = pageNumber * pageSize;
            return finalDeployJobsList.subList(fromIndex, Math.min(++pageNumber * pageSize, finalDeployJobsList.size()));
        }
        return finalDeployJobsList;
    }

    private void mergeResults(List<CICDScmJobRunDTO> partialResults,
                              UUID deployRunId,
                              Map<UUID, List<CICDJobRun.JobRunParam>> jobRunParams,
                              List<CICDScmJobRunDTO.ScmCommit> scmCommitDetails,
                              List<UUID> buildIds,
                              List<CICDScmJobRunDTO> finalDeployJobs) {
        CICDScmJobRunDTO cicdScmJobRunDTO = partialResults.stream().filter(row -> row.getId().equals(deployRunId))
                .findFirst().orElseThrow();
        Set<CICDScmJobRunDTO.ScmCommit> existingCommits = new HashSet<>(CollectionUtils.isNotEmpty(cicdScmJobRunDTO.getCommits()) ?
                cicdScmJobRunDTO.getCommits() : Set.of());
        existingCommits.addAll(scmCommitDetails);
        CICDScmJobRunDTO.CICDScmJobRunDTOBuilder ciCdScmJobRunDTOBuilder = cicdScmJobRunDTO.toBuilder();
        if (jobRunParams.containsKey(deployRunId)) {
            ciCdScmJobRunDTOBuilder.params(jobRunParams.get(deployRunId));
        }
        ciCdScmJobRunDTOBuilder.commits(new ArrayList<>(existingCommits));
        ciCdScmJobRunDTOBuilder.buildRunIds(buildIds);
        finalDeployJobs.add(ciCdScmJobRunDTOBuilder.build());
    }

    private String addDeployTimeRangeFilter(ImmutablePair<Long, Long> deployEndTimeRangePair) {
        StringBuilder deployTimeRangeCondition = new StringBuilder();
        if (deployEndTimeRangePair != null) {
            if (deployEndTimeRangePair.getLeft() != null) {
                deployTimeRangeCondition.append(" deploy_end_time > to_timestamp(").append(deployEndTimeRangePair.getLeft()).append(")");
            }
            if (deployEndTimeRangePair.getRight() != null) {
                deployTimeRangeCondition.append("AND deploy_end_time < to_timestamp(").append(deployEndTimeRangePair.getRight()).append(")");
            }
        }
        return deployTimeRangeCondition.toString();
    }


    public DbListResponse<DbAggregationResult> groupByAndCalculateWithoutStacks(String company,
                                                                                CiCdScmFilter filter,
                                                                                Map<String, SortingOrder> sortBy,
                                                                                boolean valuesOnly,
                                                                                OUConfiguration ouConfig) {
        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");

        CiCdScmFilter.CALCULATION calculation = CiCdScmFilter.getSanitizedCalculation(filter);
        StringBuilder orderBySB = new StringBuilder();
        StringBuffer calculationComponentOuterSB = new StringBuffer();
        StringBuffer calculationComponentInnerSB = new StringBuffer();
        Set<String> orderByStrings = new HashSet<>();
        parseCalculation(calculation, calculationComponentOuterSB, calculationComponentInnerSB, sortBy, orderByStrings);
        Set<String> outerSelects = new HashSet<>();
        Set<String> innerSelects = new HashSet<>();
        Set<String> groupByStrings = new HashSet<>();
        parseAcrossOrStack(filter.getAcross(), filter.getAggInterval(), outerSelects, innerSelects, groupByStrings, sortBy, orderByStrings);
        String outerSelect = String.join(",", outerSelects);
        String innerSelect = String.join(",", innerSelects);
        String groupByString = String.join(",", groupByStrings);
        String selectJobEnd = "job_end_interval";
        Map<String, Object> params = new HashMap<>();
        int acrossCount = (filter.getAcrossCount() != null) ? filter.getAcrossCount() : 90;

        String limit = (valuesOnly) ? "" : " LIMIT " + acrossCount;

        boolean isRepo = filter.getAcross() == CiCdScmFilter.DISTINCT.repo;
        String selectRepo = "repo_ids";

        String subQuery = getSubQuery(company, params, filter, innerSelect, null, false, false, ouConfig);
        if(filter.getAcross() == CiCdScmFilter.DISTINCT.trend) {
            selectJobEnd = "trend_interval";
        }
        orderBySB.append(String.join(",", orderByStrings));
        String nullsPosition = SortingOrder.getNullsPosition(SortingOrder.fromString(orderBySB.toString().split(" ")[1]));
        String sql = "SELECT " + (isRepo ? selectRepo : outerSelect) + "," + calculationComponentOuterSB.toString() + " FROM (" +
                "SELECT " + (filter.getAcross() == CiCdScmFilter.DISTINCT.trend ||
                filter.getAcross() == CiCdScmFilter.DISTINCT.job_end
                ? selectJobEnd : outerSelect) + "," +
                calculationComponentInnerSB +
                " FROM (" + subQuery + " ) final " +
                ") b" + " GROUP BY " + groupByString + " ORDER BY " + orderBySB + " NULLS " + nullsPosition +
                limit;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        List<DbAggregationResult> results = template.query(sql, params, DbCiCdScmConverters.mapAggregationsResults(
                filter.getAcross(), filter.getCalculation()));
        return DbListResponse.of(results, results.size());
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company,
                                                                   CiCdScmFilter filter,
                                                                   Map<String, SortingOrder> sortBy,
                                                                   boolean valuesOnly) {
        return groupByAndCalculate(company, filter.toBuilder().sortBy(sortBy).build(), valuesOnly);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company,
                                                                   CiCdScmFilter filter,
                                                                   boolean valuesOnly) {
        return groupByAndCalculate(company, filter, valuesOnly, null);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company,
                                                                   CiCdScmFilter filter,
                                                                   boolean valuesOnly,
                                                                   OUConfiguration ouConfig) {
        Map<String, SortingOrder> sortBy = filter.getSortBy();
        DbListResponse<DbAggregationResult> resultsWithoutStacks = groupByAndCalculateWithoutStacks(company, filter, sortBy, valuesOnly, ouConfig);
        if (CollectionUtils.isEmpty(resultsWithoutStacks.getRecords())) {
            return resultsWithoutStacks;
        }
        if (CollectionUtils.isEmpty(filter.getStacks())) {
            return resultsWithoutStacks;
        }
        if(filter.getAcross().equals(CiCdScmFilter.DISTINCT.trend) ||
                filter.getAcross().equals(CiCdScmFilter.DISTINCT.job_end)) {
            return groupByAndCalculateDateIntervals(company, filter, resultsWithoutStacks, ouConfig);
        }
        CiCdScmFilter.CALCULATION calculation = CiCdScmFilter.getSanitizedCalculation(filter);
        StringBuilder orderBySB = new StringBuilder();
        StringBuffer calculationComponentOuterSB = new StringBuffer();
        StringBuffer calculationComponentInnerSB = new StringBuffer();
        Set<String> orderByStrings = new HashSet<>();
        parseCalculation(calculation, calculationComponentOuterSB, calculationComponentInnerSB, sortBy, orderByStrings);

        Set<String> outerSelects = new HashSet<>();
        Set<String> innerSelects = new HashSet<>();
        Set<String> groupByStrings = new HashSet<>();
        parseAcrossOrStack(filter.getAcross(), filter.getAggInterval(), outerSelects, innerSelects, groupByStrings, sortBy, orderByStrings);
        if (CollectionUtils.isNotEmpty(filter.getStacks())) {
            for (CiCdScmFilter.DISTINCT stack : filter.getStacks()) {
                parseAcrossOrStack(stack, filter.getAggInterval(), outerSelects, innerSelects, groupByStrings, sortBy, orderByStrings);
            }
        }
        String outerSelect = String.join(",", outerSelects);
        String innerSelect = String.join(",", innerSelects);
        String groupByString = String.join(",", groupByStrings);
        orderBySB.append(String.join(",", orderByStrings));
        Map<String, Object> params = new HashMap<>();
        String nullsPosition = SortingOrder.getNullsPosition(SortingOrder.fromString(orderBySB.toString().split(" ")[1]));
        String subQuery = getSubQuery(company, params, filter, innerSelect, resultsWithoutStacks, true, false, ouConfig);
        String sql = "SELECT " + outerSelect + "," + calculationComponentOuterSB.toString() + " FROM (" +
                "SELECT " + (filter.getStacks().contains(CiCdScmFilter.DISTINCT.job_end) ? "job_end_interval," : "") + outerSelect + "," + calculationComponentInnerSB.toString() +
                " FROM (" + subQuery + " ) final " +
                ") b" + " GROUP BY " + groupByString + " ORDER BY " + orderBySB.toString() + " NULLS " + nullsPosition;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        List<DbAggregationResultStacksWrapper> resultsWithStacks = template.query(sql, params, DbCiCdScmConverters.mapAggregationsResultsWithStacksMapper(
                filter));
        List<DbAggregationResult> results = DbAggregationResultsMerger.mergeResultsWithAndWithoutStacks(resultsWithoutStacks.getRecords(), resultsWithStacks);
        return DbListResponse.of(results, results.size());
    }

    private DbListResponse<DbAggregationResult> groupByAndCalculateDateIntervals(String company,
                                                                                 CiCdScmFilter filter,
                                                                                 DbListResponse<DbAggregationResult> resultsWithoutStacks,
                                                                                 OUConfiguration ouConfig) {
        CiCdScmFilter.DISTINCT stacks = filter.getStacks().get(0);
        Stream<DbAggregationResult> dbAggregationResultStream = resultsWithoutStacks
                .getRecords()
                .parallelStream()
                .map(row -> {
                    CiCdScmFilter newFilter = CiCdScmFilter.builder().build();
                    CiCdScmFilter.CiCdScmFilterBuilder filterBuilder = filter.toBuilder();
                    switch (filter.getAcross()) {
                        case trend:
                        case job_end:
                            newFilter = fillConditionsForStacks(stacks, row,
                                    filter.getAggInterval() == null ? CICD_AGG_INTERVAL.day : filter.getAggInterval()
                                    , filterBuilder);
                            break;
                        default:
                    }
                    List<DbAggregationResult> currentStackResults = groupByAndCalculateWithoutStacks(company, newFilter,
                            Map.of(stacks.toString(), SortingOrder.ASC), false, ouConfig).getRecords();
                    return row.toBuilder().stacks(currentStackResults).build();
                });
        List<DbAggregationResult> finalList = dbAggregationResultStream.collect(Collectors.toList());
        return DbListResponse.of(finalList, finalList.size());
    }

    private void applyAcrossForStacks(CiCdScmFilter filter, DbListResponse<DbAggregationResult> resultsWithoutStacks,
                                      Map<String, Object> params, CicdScmSqlCriteria conditions) {
        switch (filter.getAcross()) {
            case author:
                conditions.getScmCriteria().add(filter.getAcross() + " in (:invalues)");
                params.put("invalues", resultsWithoutStacks.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()));
                break;
            case job_status:
                conditions.getCicdCriteria().add("status in (:invalues)");
                params.put("invalues", resultsWithoutStacks.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()));
                break;
            case project_name:
                conditions.getCicdCriteria().add("project_name in (:invalues)");
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
                    conditions.getCicdCriteria().add("( " + String.join(" OR ", qualifiedJobNamesInCriterea) + " )");
                }
                break;
            case job_name:
            case cicd_user_id:
            case job_normalized_full_name:
                conditions.getCicdCriteria().add(filter.getAcross() + " in (:invalues)");
                params.put("invalues", resultsWithoutStacks.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()));
                break;
            case instance_name:
                conditions.getCicdCriteria().add((filter.getAcross() != CiCdScmFilter.DISTINCT.instance_name ? filter.getAcross().toString() : "i.name") + " in (:invalues)");
                params.put("invalues", resultsWithoutStacks.getRecords().stream().map(DbAggregationResult::getKey).collect(Collectors.toList()));
                break;
            default:
                Validate.notNull(null, "Invalid across field provided.");
        }
    }


    private CiCdScmFilter fillConditionsForStacks(CiCdScmFilter.DISTINCT stack, DbAggregationResult row,
                                                  CICD_AGG_INTERVAL aggInterval,
                                                  CiCdScmFilter.CiCdScmFilterBuilder ciCdScmFilterBuilder) {
        Calendar cal = Calendar.getInstance();
        long startTimeInSeconds = Long.parseLong(row.getKey());
        cal.setTimeInMillis(TimeUnit.SECONDS.toMillis(startTimeInSeconds));
        if(aggInterval.equals(CICD_AGG_INTERVAL.month))
            cal.add(Calendar.MONTH, 1);
        else if (aggInterval.equals(CICD_AGG_INTERVAL.day))
            cal.add(Calendar.DATE, 1);
        else if (aggInterval.equals(CICD_AGG_INTERVAL.year))
            cal.add(Calendar.YEAR, 1);
        else if (aggInterval.equals(CICD_AGG_INTERVAL.quarter))
            cal.add(Calendar.MONTH, 3);
        else
            cal.add(Calendar.DATE, 7);
        long endTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(cal.getTimeInMillis());
        return ciCdScmFilterBuilder
                .jobStartTime(startTimeInSeconds)
                .jobEndTime(endTimeInSeconds)
                .across(stack)
                .build();
    }

    @Value
    @Builder(toBuilder = true)
    public static class CicdScmSqlCriteria {
        boolean usingParamsTable;
        List<String> cicdCriteria;
        List<String> scmCriteria;
    }

    private CicdScmSqlCriteria createWhereClauseAndUpdateParams(String company, CiCdScmFilter filter, Map<String, Object> params,
                                                                Boolean isList, String suffix, OUConfiguration ouConfig) {
        List<String> cicdCriterea = new ArrayList<>();
        boolean usingParamsTable = false;
        String paramSuffix = suffix == null ? "" : "_" + suffix;
        if (CollectionUtils.isNotEmpty(filter.getCicdUserIds()) || OrgUnitHelper.doesOuConfigHaveCiCdUsers(ouConfig)) { // OU: user
            var columnName = "r.cicd_user_id" + paramSuffix;
            var columnNameParam = columnName + paramSuffix;
            if(OrgUnitHelper.doesOuConfigHaveCiCdUsers(ouConfig)){
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params,
                        IntegrationType.getCICDIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    cicdCriterea.add(MessageFormat.format("{0} IN (SELECT cloud_id FROM ({1}) l)", "r.cicd_user_id", usersSelect));
                }
            }
            else if(CollectionUtils.isNotEmpty(filter.getCicdUserIds())){
                TeamUtils.addUsersCondition(company, cicdCriterea, params, "r.cicd_user_id", columnNameParam, false, filter.getCicdUserIds(), CICD_APPLICATIONS);
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCiCdUserIds())) {
            String columnNameParam = "r.cicd_user_id" + paramSuffix;
            TeamUtils.addUsersCondition(company, cicdCriterea, params, "r.cicd_user_id",
                    columnNameParam, false, filter.getExcludeCiCdUserIds(), CICD_APPLICATIONS, true);
        }
        if (CollectionUtils.isNotEmpty(filter.getJobNames())) {
            cicdCriterea.add("j.job_name IN (:job_names" + paramSuffix + ")");
            params.put("job_names" + paramSuffix, filter.getJobNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeJobNames())) {
            cicdCriterea.add("j.job_name NOT IN (:excl_job_names" + paramSuffix + ")");
            params.put("excl_job_names" + paramSuffix, filter.getExcludeJobNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getProjects())) {
            cicdCriterea.add("j.project_name IN (:project_names" + paramSuffix + ")");
            params.put("project_names" + paramSuffix, filter.getProjects());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeProjects())) {
            cicdCriterea.add("j.project_name NOT IN (:excl_project_names" + paramSuffix + ")");
            params.put("excl_project_names" + paramSuffix, filter.getExcludeProjects());
        }
        if (CollectionUtils.isNotEmpty(filter.getJobNormalizedFullNames())) {
            cicdCriterea.add("j.job_normalized_full_name IN (:job_normalized_full_names" + paramSuffix + ")");
            params.put("job_normalized_full_names" + paramSuffix, filter.getJobNormalizedFullNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeJobNormalizedFullNames())) {
            cicdCriterea.add("j.job_normalized_full_name NOT IN (:excl_job_normalized_full_names" + paramSuffix + ")");
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
                    params.put(instanceNameKey, qualifiedJobName.getInstanceName());
                    params.put(jobNameKey + paramSuffix, qualifiedJobName.getJobName());
                }
            }
            if (CollectionUtils.isNotEmpty(qualifiedJobNamesCriterea)) {
                cicdCriterea.add("( " + String.join(" OR ", qualifiedJobNamesCriterea) + " )");
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
                    params.put(instanceNameKey, excludeQualifiedJobName.getInstanceName());
                    params.put(jobNameKey + paramSuffix, excludeQualifiedJobName.getJobName());
                }
            }
            if (CollectionUtils.isNotEmpty(excludeQualifiedJobNamesCriterea)) {
                cicdCriterea.add("( " + String.join(" OR ", excludeQualifiedJobNamesCriterea) + " )");
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getJobStatuses())) {
            cicdCriterea.add("r.status IN (:job_statuses" + paramSuffix + ")");
            params.put("job_statuses" + paramSuffix, filter.getJobStatuses());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeJobStatuses())) {
            cicdCriterea.add("r.status NOT IN (:excl_job_statuses" + paramSuffix + ")");
            params.put("excl_job_statuses" + paramSuffix, filter.getExcludeJobStatuses());
        }
        if (CollectionUtils.isNotEmpty(filter.getInstanceNames())) {
            cicdCriterea.add("i.name IN (:instance_names" + paramSuffix + ")");
            params.put("instance_names" + paramSuffix, filter.getInstanceNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeInstanceNames())) {
            cicdCriterea.add("i.name NOT IN (:excl_instance_names" + paramSuffix + ")");
            params.put("excl_instance_names" + paramSuffix, filter.getExcludeInstanceNames());
        }
        if (filter.getJobStartTime() != null) {
            cicdCriterea.add("r.start_time >= TO_TIMESTAMP(:start_time_gt" + paramSuffix + ")");
            params.put("start_time_gt" + paramSuffix, filter.getJobStartTime());
        }
        if (filter.getJobEndTime() != null) {
            cicdCriterea.add("r.start_time <= TO_TIMESTAMP(:start_time_lt" + paramSuffix + ")");
            params.put("start_time_lt" + paramSuffix, filter.getJobEndTime());
        }
        if (filter.getEndTimeRange() != null) {
            ImmutablePair<Long, Long> endTimeRange = filter.getEndTimeRange();
            if (endTimeRange.getLeft() != null) {
                cicdCriterea.add("r.end_time > to_timestamp(:end_time_start" + paramSuffix + ")");
                params.put("end_time_start" + paramSuffix, endTimeRange.getLeft());
            }
            if (endTimeRange.getRight() != null) {
                cicdCriterea.add("r.end_time < to_timestamp(:end_time_end" + paramSuffix + ")");
                params.put("end_time_end" + paramSuffix, endTimeRange.getRight());
            }
        }
        if (filter.getStartTimeRange() != null) {
            ImmutablePair<Long, Long> startTimeRange = filter.getStartTimeRange();
            if (startTimeRange.getLeft() != null) {
                cicdCriterea.add("r.start_time > to_timestamp(:start_time_start" + paramSuffix + ")");
                params.put("start_time_start" + paramSuffix, startTimeRange.getLeft());
            }
            if (startTimeRange.getRight() != null) {
                cicdCriterea.add("r.start_time < to_timestamp(:start_time_end" + paramSuffix + ")");
                params.put("start_time_end" + paramSuffix, startTimeRange.getRight());
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getTypes())) {
            cicdCriterea.add("i.type IN (:types" + paramSuffix + ")");
            params.put("types" + paramSuffix, filter.getTypes().stream()
                    .map(CICD_TYPE::toString)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTypes())) {
            cicdCriterea.add("i.type NOT IN (:excl_types" + paramSuffix + ")");
            params.put("excl_types" + paramSuffix, filter.getExcludeTypes().stream()
                    .map(CICD_TYPE::toString)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getCicdIntegrationIds())) {
            cicdCriterea.add("i.integration_id IN (:cicd_integration_ids" + paramSuffix + ")");
            params.put("cicd_integration_ids" + paramSuffix, filter.getCicdIntegrationIds().stream().map(Integer::parseInt).collect(Collectors.toList()));
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
                usingParamsTable = true;
            }
            if (CollectionUtils.isNotEmpty(parametersCriterea))
                cicdCriterea.add("( " + String.join(" OR ", parametersCriterea) + " )");
        }

        Map<String, Map<String, String>> partialMatchMap = filter.getPartialMatch();
        if (MapUtils.isNotEmpty(partialMatchMap)) {
            CriteriaUtils.addPartialMatchClause(partialMatchMap, cicdCriterea, params, null, CICD_SCM_PARTIAL_MATCH_COLUMNS, Collections.emptySet(), paramSuffix);
        }

        List<String> scmCriterea = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            scmCriterea.add("c.integration_id IN (:integration_ids" + paramSuffix + ")");
            params.put("integration_ids" + paramSuffix, filter.getIntegrationIds().stream().map(Integer::parseInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getAuthors()) || OrgUnitHelper.doesOuConfigHaveCiCdAuthors(ouConfig)) { // OU: user
            var columnName = "c.author" + paramSuffix;
            var columnNameParam = columnName + paramSuffix;
            if(OrgUnitHelper.doesOuConfigHaveCiCdAuthors(ouConfig)){
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.getCICDIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    scmCriterea.add(MessageFormat.format("{0} IN (SELECT cloud_id FROM ({1}) l)", "c.author", usersSelect));
                }
            }
            else if(CollectionUtils.isNotEmpty(filter.getAuthors())){
                TeamUtils.addUsersCondition(company, scmCriterea, params, "c.author", columnNameParam, false, filter.getAuthors(), CICD_APPLICATIONS);
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeAuthors())) {
            scmCriterea.add("c.author NOT IN (:excl_authors" + paramSuffix + ")");
            params.put("excl_authors" + paramSuffix, filter.getExcludeAuthors());
        }
        if (CollectionUtils.isNotEmpty(filter.getRepos())) {
            scmCriterea.add("c.repo_id && ARRAY[ :repos" + paramSuffix + " ]::varchar[]");
            params.put("repos" + paramSuffix, filter.getRepos());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeRepos())) {
            scmCriterea.add("NOT c.repo_id && ARRAY[ :excl_repos" + paramSuffix + " ]::varchar[]");
            params.put("excl_repos" + paramSuffix, filter.getExcludeRepos());
        }
        if (!isList) {
            int acrossCount = (filter.getAcrossCount() != null) ? filter.getAcrossCount() : 90;
            if (CiCdScmFilter.DISTINCT.trend == filter.getAcross() ||
                    CollectionUtils.isNotEmpty(filter.getStacks()) &&
                            filter.getStacks().get(0).equals(CiCdScmFilter.DISTINCT.trend)
                            && filter.getEndTimeRange() !=  null && filter.getStartTimeRange() != null) {
                cicdCriterea.add("r.start_time >= to_timestamp(:start_time_from" + paramSuffix + ")");
                params.put("start_time_from" + paramSuffix, Instant.now().minus(acrossCount, ChronoUnit.DAYS).getEpochSecond());
            }
            if (CiCdScmFilter.DISTINCT.job_normalized_full_name == filter.getAcross()) {
                cicdCriterea.add(("j.job_normalized_full_name IS NOT NULL"));
            }
        }
        return CicdScmSqlCriteria.builder()
                .usingParamsTable(usingParamsTable)
                .cicdCriteria(cicdCriterea)
                .scmCriteria(scmCriterea)
                .build();
    }

    public DbListResponse<CICDScmJobRunDTO> listCiCdScmCombinedData(String company,
                                                                    CiCdScmFilter filter,
                                                                    Map<String, SortingOrder> sortBy,
                                                                    Integer pageNumber, Integer pageSize) throws SQLException {
        return listCiCdScmCombinedData(company, filter.toBuilder().sortBy(sortBy).build(), pageNumber, pageSize);
    }

    public DbListResponse<CICDScmJobRunDTO> listCiCdScmCombinedData(String company,
                                                                    CiCdScmFilter filter,
                                                                    Integer pageNumber, Integer pageSize) throws SQLException {
        return listCiCdScmCombinedData(company, filter, pageNumber, pageSize, null);
    }


    public DbListResponse<CICDScmJobRunDTO> listCiCdScmCombinedData(String company,
                                                                    CiCdScmFilter filter,
                                                                    Integer pageNumber, Integer pageSize,
                                                                    OUConfiguration ouConfig) throws SQLException {
        Map<String, Object> params = new HashMap<>();
        String subQuery = getSubQuery(company, params, filter, null,
                null, false, true, ouConfig);

        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        List<String> orderBy = getListSortBy(filter.getSortBy());
        String sortColumn = orderBy.get(0).split(" ")[0];
        String nullsPosition = SortingOrder.getNullsPosition(SortingOrder.fromString(orderBy.get(0).split(" ")[1]));
        boolean isDerivedField = DERIVED_FIELDS.contains(sortColumn);
        String sqlBase = "SELECT run_id, SUM(additions+deletions) as lines_modified, SUM(files_ct) AS files_modified, " +
                "MAX(EXTRACT(EPOCH FROM (start_time - committed_at))::int) as initial_commit_to_deploy_time " +
                "FROM (" + subQuery + "  ) final GROUP BY run_id" +
                (isDerivedField ? " ORDER BY " + orderBy.get(0) :
                        "," + sortColumn + " ORDER BY " + orderBy.get(0)) + " NULLS " + nullsPosition;

        String sql = sqlBase + " OFFSET :skip LIMIT :limit";
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'

        String countSQL = "SELECT COUNT(*) FROM ( " + sqlBase + " ) x";
        log.info("countSQL = {}", countSQL);

        List<CICDScmJobRunDTO> partialCiCdScmJobRun = template.query(sql, params, DbCiCdScmConverters.mapAggListResults());
        List<UUID> jobRunIds = partialCiCdScmJobRun.stream().map(CICDScmJobRunDTO::getId).collect(Collectors.toList());
        Map<UUID, List<CICDJobRun.JobRunParam>> jobRunParams = ciCdJobRunsDatabaseService.getJobRunParams(company, jobRunIds);
        Map<UUID, List<CICDScmJobRunDTO>> cicdScmJobRun = getJobRunDetails(jobRunIds, subQuery, params);

        List<CICDScmJobRunDTO> results = mergeJobRunsParamsAndCommits(partialCiCdScmJobRun, jobRunParams, cicdScmJobRun);

        int totCount = 0;
        if (results.size() > 0) {
            totCount = results.size() + (pageNumber * pageSize); // if its last page or total count is less than pageSize
            if (results.size() == pageSize) {
                totCount = template.query(countSQL, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(results, totCount);
    }
    private List<CICDScmJobRunDTO.ScmCommit> getJobRunCommits(List<UUID> jobRunIds, String subQuery, Map<String, Object> params) {
        if (CollectionUtils.isEmpty(jobRunIds)) {
            return List.of();
        }
        String s = "SELECT * FROM (" + subQuery + ") final WHERE run_id IN (:run_ids) ";
        params.put("run_ids", jobRunIds);
        return template.query(s, params, DbCiCdScmConverters.mapScmCommits1());
    }

    private Map<UUID, List<CICDScmJobRunDTO>> getJobRunDetails(List<UUID> jobRunIds, String subQuery, Map<String, Object> params) {
        if (CollectionUtils.isEmpty(jobRunIds)) {
            return Collections.emptyMap();
        }
        String s = "SELECT * FROM (" + subQuery + ") final WHERE run_id IN (:run_ids) ";
        params.put("run_ids", jobRunIds);
        List<Map.Entry<UUID, CICDScmJobRunDTO>> partialCiCdScmJobRun = template.query(s, params, DbCiCdScmConverters.mapListResults());
        List<Map.Entry<UUID, CICDScmJobRunDTO.ScmCommit>> scmCommitsList = template.query(s, params, DbCiCdScmConverters.mapScmCommits());
        Map<UUID, List<CICDScmJobRunDTO.ScmCommit>> scmCommits = new HashMap<>();
        Map<UUID, List<CICDScmJobRunDTO>> jobRunDTOMap = partialCiCdScmJobRun.stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.groupingBy(CICDScmJobRunDTO::getId, Collectors.toList()));
        for (Map.Entry<UUID, CICDScmJobRunDTO.ScmCommit> currentCommit : scmCommitsList) {
            if (!scmCommits.containsKey(currentCommit.getKey())) {
                scmCommits.put(currentCommit.getKey(), new ArrayList<>());
            }
            scmCommits.get(currentCommit.getKey()).add(currentCommit.getValue());
        }
        jobRunDTOMap.keySet()
                .forEach(cicdScmJobRunDTO -> {
                    var jobRunDTO = jobRunDTOMap.get(cicdScmJobRunDTO);
                    var commitsDTO = scmCommits.get(cicdScmJobRunDTO);
                    jobRunDTOMap.put(cicdScmJobRunDTO,
                            jobRunDTO.stream()
                                    .map(jobRun -> jobRun.toBuilder()
                                            .commits(commitsDTO)
                                            .build()).collect(Collectors.toList()));
                });
        return jobRunDTOMap;
    }

    private List<CICDScmJobRunDTO> mergeJobRunsParamsAndCommits(final List<CICDScmJobRunDTO> jobRuns,
                                                                final Map<UUID, List<CICDJobRun.JobRunParam>> jobRunParams,
                                                                Map<UUID, List<CICDScmJobRunDTO>> cicdScmjobRuns) {
        List<CICDScmJobRunDTO> fullJobRuns = new ArrayList<>();
        for (CICDScmJobRunDTO sortedJobRun : jobRuns) {
            CICDScmJobRunDTO.CICDScmJobRunDTOBuilder bldr = sortedJobRun.toBuilder();
            if (jobRunParams.containsKey(sortedJobRun.getId())) {
                bldr.params(jobRunParams.get(sortedJobRun.getId()));
            }
            for (var jobRunDetails : cicdScmjobRuns.get(sortedJobRun.getId())) {
                bldr.cicdIntegrationId(jobRunDetails.getCicdIntegrationId());
                bldr.scmIntegrationId(jobRunDetails.getScmIntegrationId());
                bldr.cicdInstanceGuid(jobRunDetails.getCicdInstanceGuid());
                bldr.cicdInstanceName(jobRunDetails.getCicdInstanceName());
                bldr.cicdJobId(jobRunDetails.getCicdJobId());
                bldr.cicdUserId(jobRunDetails.getCicdUserId());
                bldr.duration(jobRunDetails.getDuration());
                bldr.startTime(jobRunDetails.getStartTime());
                bldr.endTime(jobRunDetails.getEndTime());
                bldr.status(jobRunDetails.getStatus());
                bldr.jobName(jobRunDetails.getJobName());
                bldr.jobNormalizedFullName(jobRunDetails.getJobNormalizedFullName());
                bldr.jobRunNumber(jobRunDetails.getJobRunNumber());
                bldr.scmUrl(jobRunDetails.getScmUrl());
                bldr.commits(jobRunDetails.getCommits());
            }
            fullJobRuns.add(bldr.build());
        }
        return fullJobRuns;
    }
    // endregion
}
