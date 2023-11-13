package io.levelops.commons.service.dora;

import io.levelops.commons.databases.converters.DbScmConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.dora.DoraResponseDTO;
import io.levelops.commons.databases.models.dora.DoraSingleStateDTO;
import io.levelops.commons.databases.models.dora.DoraTimeSeriesDTO;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.ScmConditionBuilder;
import io.levelops.commons.databases.services.ScmQueryUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.utils.dora.DoraCalculationUtils;
import io.levelops.commons.utils.dora.DoraQueryUtils;
import io.levelops.commons.utils.dora.DoraValidationsUtils;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.levelops.commons.databases.services.ScmQueryUtils.PRS_APPROVER_COUNT;
import static io.levelops.commons.databases.services.ScmQueryUtils.SCM_COMMIT_OUTER_SELECT;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkApproversFiltersJoin;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkAuthors;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkCommentDensityFiltersJoin;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkCommitsTableFiltersJoin;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkCommitters;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkCreatorsFiltersJoin;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkPRReviewedJoin;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkReviewersFiltersJoin;
import static io.levelops.commons.databases.services.ScmQueryUtils.getCodeChangeSql;
import static io.levelops.commons.databases.services.ScmQueryUtils.getCommitsJoinForScmDora;

@Log4j2
@Service
public class ScmDoraService {

    private final NamedParameterJdbcTemplate template;
    private final ScmAggService scmAggService;

    private final ScmDeployFrequencyFailureRateCalculation deployFrequencyFailureRateCalculation;

    public static final String PRS_TABLE = "scm_pullrequests";
    public static final String COMMITS_TABLE = "scm_commits";

    public static final String DEPLOY_TABLE = "deployment";
    public static final String DIRECT_MERGE_TABLE = "direct_merge";
    public static final String REVIEWS_TABLE = "scm_pullrequest_reviews";

    public final String DEPLOYMENT_FREQUENCY = "deployment_frequency_report";
    public final String CHANGE_FAILURE_RATE = "change_failure_rate";

    public static final Set<String> PR_SORTABLE_COLUMNS = Set.of("pr_updated_at", "pr_merged_at", "pr_created_at", "pr_closed_at",
            "title", "project", "assignees", "approver", "reviewer", "creator", "cycle_time", "lines_added", "lines_deleted"
            , "lines_changed", "files_ct", "repo_id", "approval_status", "review_type");

    public static final Set<String> COMMIT_SORTABLE_COLUMNS = Set.of("created_at", "committed_at", "commit_pushed_at");


    @Autowired
    public ScmDoraService(final DataSource dataSource, ScmAggService scmAggService, ScmDeployFrequencyFailureRateCalculation deployFrequencyFailureRateCalculation) {
        template = new NamedParameterJdbcTemplate(dataSource);
        this.scmAggService = scmAggService;
        this.deployFrequencyFailureRateCalculation = deployFrequencyFailureRateCalculation;
    }

    public DoraResponseDTO calculateNewDeploymentFrequency(String company,
                                                           ScmPrFilter scmPrFilter,
                                                           ScmCommitFilter scmCommitFilter,
                                                           OUConfiguration ouConfig,
                                                           VelocityConfigDTO velocityConfigDTO) throws BadRequestException {

        DoraValidationsUtils.validateVelocityConfig(scmPrFilter, velocityConfigDTO);
        boolean needCommitsUnionJoin = checkDirectMergeNewConfigForDeploymentFrequency(velocityConfigDTO);

        List<DoraTimeSeriesDTO.TimeSeriesData> tempTimeSeriesResult = getTimeSeriesData(company, scmPrFilter, scmCommitFilter,
                ouConfig, MapUtils.emptyIfNull(velocityConfigDTO.getDeploymentFrequency()
                .getVelocityConfigFilters().getDeploymentFrequency()
                .getScmFilters()), needCommitsUnionJoin,
                velocityConfigDTO.getDeploymentFrequency().getCalculationField().toString());

        List<DoraTimeSeriesDTO.TimeSeriesData> filledTimeSeriesResult = getFilledTimeSeriesData(scmPrFilter,
                velocityConfigDTO.getDeploymentFrequency().getCalculationField().toString(), tempTimeSeriesResult);

        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByDay = DoraCalculationUtils.convertTimeSeries("day", filledTimeSeriesResult);
        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByWeek = DoraCalculationUtils.convertTimeSeries("week", filledTimeSeriesResult);
        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByMonth = DoraCalculationUtils.convertTimeSeries("month", filledTimeSeriesResult);
        int total = CollectionUtils.emptyIfNull(filledTimeSeriesResult).stream().map(m -> m.getCount()).reduce(0,(a, b) -> a+b);
        double perDay = total/getTimeDifference(scmPrFilter, velocityConfigDTO);

        DoraTimeSeriesDTO timeSeries = DoraTimeSeriesDTO.builder().day(timeSeriesByDay).month(timeSeriesByMonth).week(timeSeriesByWeek).build();
        DoraSingleStateDTO stats = DoraSingleStateDTO.builder().totalDeployment(total).countPerDay(perDay).band(DoraCalculationUtils.calculateDeploymentFrequencyBand(perDay)).build();
        return DoraResponseDTO.builder().timeSeries(timeSeries).stats(stats).build();
    }

    public DoraResponseDTO calculateDeploymentFrequency(String company,
                                                        ScmPrFilter scmPrFilter,
                                                        ScmCommitFilter scmCommitFilter,
                                                        OUConfiguration ouConfig,
                                                        VelocityConfigDTO velocityConfigDTO) throws BadRequestException {
        return deployFrequencyFailureRateCalculation.calculateDeploymentFrequency(company,
                                                                                  scmPrFilter,
                                                                                  scmCommitFilter,
                                                                                  ouConfig,
                                                                                  velocityConfigDTO);
    }

    public DoraResponseDTO calculateChangeFailureRate(String company,
                                                      ScmPrFilter scmPrFilter,
                                                      ScmCommitFilter scmCommitFilter,
                                                      OUConfiguration ouConfig,
                                                      VelocityConfigDTO velocityConfigDTO) throws BadRequestException {
        return deployFrequencyFailureRateCalculation.calculateChangeFailureRate(company,
                                                                                scmPrFilter,
                                                                                scmCommitFilter,
                                                                                ouConfig,
                                                                                velocityConfigDTO);
    }

    public DbListResponse<DbScmPullRequest> getPrBasedDrillDownData(String company,
                                                                     ScmPrFilter scmPrfilter,
                                                                     Map<String, SortingOrder> sortBy,
                                                                     OUConfiguration ouConfig,
                                                                     Integer pageNumber,
                                                                     Integer pageSize,
                                                                     Map<String, Map<String, List<String>>> scmProfileFilters,
                                                                     String deploymentCriteria) {
        return deployFrequencyFailureRateCalculation.getPrBasedDrillDownData(company,
                                                                             scmPrfilter,
                                                                             sortBy,
                                                                             ouConfig,
                                                                             pageNumber,
                                                                             pageSize,
                                                                             scmProfileFilters,
                                                                             deploymentCriteria);
    }

    public DbListResponse<DbScmCommit> getCommitBasedDrillDownData(String company,
                                                                   ScmCommitFilter scmCommitFilter,
                                                                   OUConfiguration ouConfig,
                                                                   Map<String, Map<String, List<String>>> scmProfileFilters,
                                                                   Integer pageNumber,
                                                                   Integer pageSize,
                                                                   Map<String, SortingOrder> sortBy,
                                                                   String deploymentCriteria,
                                                                   String calculationField) {
        return deployFrequencyFailureRateCalculation.getCommitBasedDrillDownData(company,
                                                                                 scmCommitFilter,
                                                                                 ouConfig,
                                                                                 scmProfileFilters,
                                                                                 pageNumber,
                                                                                 pageSize,
                                                                                 sortBy,
                                                                                 deploymentCriteria,
                                                                                 calculationField);
    }





    private Double getTimeDifference(ScmPrFilter scmPrFilter,
                                     VelocityConfigDTO velocityConfig) {
        if(("pr_merged_at".equals(velocityConfig.getDeploymentFrequency().getCalculationField().toString()))) {
            return DoraCalculationUtils.getTimeDifference(scmPrFilter.getPrMergedRange());
        }
        else {
            return DoraCalculationUtils.getTimeDifference(scmPrFilter.getPrClosedRange());
        }
    }

    public DoraResponseDTO calculateNewChangeFailureRate( String company,
                                                          ScmPrFilter scmPrFilter,
                                                          ScmCommitFilter scmCommitFilter,
                                                          OUConfiguration ouConfig,
                                                          VelocityConfigDTO velocityConfigDTO) throws BadRequestException {

        DoraValidationsUtils.validateVelocityConfig(scmPrFilter, velocityConfigDTO);
        DoraSingleStateDTO.DoraSingleStateDTOBuilder stats = DoraSingleStateDTO.builder()
                                                            .isAbsolute(velocityConfigDTO.getChangeFailureRate().getIsAbsoulte());

        boolean needCommitsUnionFailedDeploymentJoin = checkDirectMergeNewConfigForFailedDeployment(velocityConfigDTO);
        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByDayFailedDeployment = getTimeSeriesData(company,
                scmPrFilter, scmCommitFilter, ouConfig,
                MapUtils.emptyIfNull(velocityConfigDTO.getChangeFailureRate()
                        .getVelocityConfigFilters().getFailedDeployment()
                        .getScmFilters()), needCommitsUnionFailedDeploymentJoin,
                velocityConfigDTO.getChangeFailureRate().getCalculationField().toString());
        int totalFailed = CollectionUtils.emptyIfNull(timeSeriesByDayFailedDeployment).stream().map(m -> m.getCount()).reduce(0,(a, b) -> a+b);

        List<DoraTimeSeriesDTO.TimeSeriesData> filledTimeSeriesByDayFailedDeployment = getFilledTimeSeriesData(scmPrFilter,
                velocityConfigDTO.getChangeFailureRate().getCalculationField().toString(),
                timeSeriesByDayFailedDeployment);

        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByDay = DoraCalculationUtils.convertTimeSeries("day", filledTimeSeriesByDayFailedDeployment);
        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByWeek = DoraCalculationUtils.convertTimeSeries("week", filledTimeSeriesByDayFailedDeployment);
        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByMonth = DoraCalculationUtils.convertTimeSeries("month", filledTimeSeriesByDayFailedDeployment);

        DoraTimeSeriesDTO timeSeries = DoraTimeSeriesDTO.builder().day(timeSeriesByDay).month(timeSeriesByMonth).week(timeSeriesByWeek).build();
        stats.totalDeployment(totalFailed);

        if(velocityConfigDTO.getChangeFailureRate().getIsAbsoulte() != null
                && !velocityConfigDTO.getChangeFailureRate().getIsAbsoulte()) {

            boolean needCommitsUnionTotalDeploymentJoin = checkDirectMergeNewConfigForTotalDeployment(velocityConfigDTO);
            List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByDayTotalDeployment = getTimeSeriesData(company, scmPrFilter,
                    scmCommitFilter, ouConfig,
                    MapUtils.emptyIfNull(velocityConfigDTO.getChangeFailureRate()
                            .getVelocityConfigFilters().getTotalDeployment()
                            .getScmFilters()), needCommitsUnionTotalDeploymentJoin,
                    velocityConfigDTO.getChangeFailureRate().getCalculationField().toString());

            int totalDeployment = CollectionUtils.emptyIfNull(timeSeriesByDayTotalDeployment).stream().map(m -> m.getCount()).reduce(0,(a, b) -> a+b);
            if(totalFailed > 0 && totalDeployment == 0)
                throw new RuntimeException("Invalid configuration for Change Failure Rate of WorkFlow Profile: "+ velocityConfigDTO.getName());

            double failureRate = 0;
            if(totalFailed == 0 )
                failureRate = 0;
            else
                failureRate = Double.valueOf(totalFailed)*100/totalDeployment;

            stats.failureRate(failureRate).totalDeployment(totalDeployment)
                    .band(DoraCalculationUtils.calculateChangeFailureRateBand(failureRate));
        }
        return DoraResponseDTO.builder().timeSeries(timeSeries).stats(stats.build()).build();
    }

    @NotNull
    private List<DoraTimeSeriesDTO.TimeSeriesData> getFilledTimeSeriesData(ScmPrFilter scmPrFilter, String calculationField, List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByDayFailedDeployment) {

        Long startTime = 0L;
        Long endTime = 0L;
        if ("pr_closed_at".equals(calculationField)) {
            startTime = scmPrFilter.getPrClosedRange().getLeft();
            endTime = scmPrFilter.getPrClosedRange().getRight();
        } else {
            startTime = scmPrFilter.getPrMergedRange().getLeft();
            endTime = scmPrFilter.getPrMergedRange().getRight();
        }

        List<DoraTimeSeriesDTO.TimeSeriesData> filledTimeSeriesByDay =
                DoraCalculationUtils.fillRemainingDates(startTime,
                                                        endTime,
                                                        timeSeriesByDayFailedDeployment);
        return filledTimeSeriesByDay;
    }

    @NotNull
    private List<DoraTimeSeriesDTO.TimeSeriesData> getTimeSeriesData(String company,
                                                                     ScmPrFilter scmPrFilter,
                                                                     ScmCommitFilter scmCommitFilter,
                                                                     OUConfiguration ouConfig,
                                                                     Map<String, Map<String, List<String>>> scmFilters,
                                                                     boolean needCommitsUnionJoin, String calculationOnField) {

        ScmPrFilter.CALCULATION calculation = scmPrFilter.getCalculation();
        Map<String, Object> params = new HashMap<>();

        String regexPatternCondition, regexPatternConditionForCommits;
        String prsWhere = StringUtils.EMPTY;
        String commitsWhere = StringUtils.EMPTY;

        boolean needCommitsTable = checkCommitsTableFiltersJoin(scmPrFilter, calculation, false);
        boolean needCommentors = checkCommentDensityFiltersJoin(scmPrFilter, calculation, false);
        boolean needReviewedAt = checkPRReviewedJoin(scmPrFilter);
        boolean needCreators = checkCreatorsFiltersJoin(scmPrFilter, ouConfig);
        boolean needCommitters = checkCommitters(scmCommitFilter, ouConfig);
        boolean needAuthors = checkAuthors(scmCommitFilter, ouConfig);
        boolean needReviewers = checkReviewersFiltersJoin(scmPrFilter, calculation, false);
        boolean needApprovers = checkApproversFiltersJoin(scmPrFilter, calculation, false);

        Map<String, List<String>> prConditions = scmAggService.createPrWhereClauseAndUpdateParams(company,
                params, scmPrFilter, null, "", ouConfig);
        Map<String, List<String>> commitConditions = scmAggService.createCommitsWhereClauseAndUpdateParams(company,
                params, scmCommitFilter, null, "", false, ouConfig);

        prConditions.get(PRS_TABLE).add("merged");
        commitConditions.get(COMMITS_TABLE).add("direct_merge");
        String key = "band";
        if (prConditions.get(PRS_TABLE).size() > 0) {
            prsWhere = " WHERE " + String.join(" AND ", prConditions.get(PRS_TABLE));
        }
        if (commitConditions.get(COMMITS_TABLE).size() > 0) {
            commitsWhere = " WHERE " + String.join(" AND ", commitConditions.get(COMMITS_TABLE));
            commitsWhere = commitsWhere + addPushedAtCondition("", scmCommitFilter);
        }

        Map<String, List<String>> deployment = buildRegexMapWithNewProfile(scmFilters, params, "deploy", DEPLOY_TABLE);

        String intrCond = deployment.get(DEPLOY_TABLE) != null && deployment.get(DEPLOY_TABLE).size() > 0 ?
                String.join(" OR ", deployment.get(DEPLOY_TABLE)) : StringUtils.EMPTY;
        regexPatternCondition = StringUtils.isNotEmpty(intrCond) ? " AND ( " + intrCond + ")" : StringUtils.EMPTY;
        String intrCondForCommits = deployment.get(DIRECT_MERGE_TABLE) != null && deployment.get(DIRECT_MERGE_TABLE).size() > 0 ?
                String.join(" OR ", deployment.get(DIRECT_MERGE_TABLE)) : StringUtils.EMPTY;
        regexPatternConditionForCommits = StringUtils.isNotEmpty(intrCondForCommits) ? " AND ( " + intrCondForCommits + ")" : StringUtils.EMPTY;

        String creatorJoin = needCreators ? ScmQueryUtils.sqlForCreatorTableJoin(company) : StringUtils.EMPTY;
        String reviewerJoin = needReviewers ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.REVIEWER) : StringUtils.EMPTY;
        String approverJoin = needApprovers ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.APPROVER) : StringUtils.EMPTY;
        String commenterJoin = needCommentors ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.COMMENTER) : StringUtils.EMPTY;
        String commitsPRsJoin = needCommitsTable ? getCommitsJoinForScmDora(company, scmPrFilter.getIntegrationIds()) : StringUtils.EMPTY;
        String prsReviewedAtJoin = needReviewedAt ? ScmQueryUtils.getSqlForPRReviewedAtJoin(company) : StringUtils.EMPTY;
        String reviewsTableSelect = needReviewers ? ScmQueryUtils.REVIEW_PRS_SELECT_LIST : StringUtils.EMPTY;
        String approversTableSelect = needApprovers ? ScmQueryUtils.APPROVERS_PRS_LIST : StringUtils.EMPTY;
        String commentersSelect = needCommentors ? ScmQueryUtils.COMMENTERS_SELECT_SQL : StringUtils.EMPTY;
        String creatorsSelect = needCreators ? ScmQueryUtils.CREATORS_SQL : StringUtils.EMPTY;
        String committersSelect = needCommitters ? ScmQueryUtils.COMMITTERS_SELECT : StringUtils.EMPTY;
        String authorsSelect = needAuthors ? ScmQueryUtils.AUTHORS_SELECT : StringUtils.EMPTY;
        String authorTableJoin = needAuthors ? ScmQueryUtils.sqlForAuthorTableJoin(company) : StringUtils.EMPTY;
        String committerTableJoin = needCommitters ? ScmQueryUtils.sqlForCommitterTableJoin(company) : StringUtils.EMPTY;

        boolean isTagsJoinRequired = isTagsJoinRequired(scmFilters);
        String tagsTableJoin = ScmQueryUtils.getTagsTableJoin(company, scmPrFilter.getIntegrationIds());

        if (CollectionUtils.isNotEmpty(scmPrFilter.getRepoIds())) {
            if (prsWhere.equals("")) {
                prsWhere = " WHERE repo_ids IN (:repo_ids) ";
            } else {
                prsWhere = prsWhere + " AND  repo_ids IN (:repo_ids) ";
            }
        }
        if (CollectionUtils.isNotEmpty(scmCommitFilter.getRepoIds())) {
            if (commitsWhere.equals("")) {
                commitsWhere = " WHERE repo_ids IN (:repo_ids) ";
            } else {
                commitsWhere = commitsWhere + " AND  repo_ids IN (:repo_ids) ";
            }
        }
        String prsSelect = ScmQueryUtils.PRS_SELECT;
        String commitsSelect = ScmQueryUtils.SCM_COMMITS_SELECT + ", committer as creator, committer_id::varchar as creator_id";

        String commitsUnionStmt = needCommitsUnionJoin ? " UNION SELECT id, commit_pushed_at as trend_interval"
                + "  FROM (SELECT unnest(scm_commits.repo_id) AS repo_ids, direct_merge, "
                + commitsSelect + committersSelect + authorsSelect
                + " FROM " + company + "." + COMMITS_TABLE
                + committerTableJoin
                + authorTableJoin
                + " ) b" + commitsWhere + regexPatternConditionForCommits : StringUtils.EMPTY;

        String intermediateSQL = "SELECT id, " + calculationOnField +" as trend_interval "
                + " FROM (SELECT " + " unnest(scm_pullrequests.repo_id) AS repo_ids, " + prsSelect;
        if(isTagsJoinRequired)
            intermediateSQL = intermediateSQL + ", tags";

        intermediateSQL= intermediateSQL + reviewsTableSelect + approversTableSelect + commentersSelect + creatorsSelect
                + " FROM " + company + "." + PRS_TABLE
                + creatorJoin
                + reviewerJoin
                + approverJoin
                + commenterJoin
                + commitsPRsJoin;

        if(isTagsJoinRequired)
                    intermediateSQL = intermediateSQL + tagsTableJoin;

        intermediateSQL = intermediateSQL + prsReviewedAtJoin
                + " ) a"
                + prsWhere
                + regexPatternCondition
                + commitsUnionStmt;

        String sql = DoraQueryUtils.deploymentFrequencyInitQuery()
                + "SELECT id, trend_interval "
                + " FROM (" + intermediateSQL + " ) x "
                + DoraQueryUtils.deploymentFrequencyEndQuery() ;

        log.info("sql = {}", sql);
        log.info("params = {}", params);

        return template.query(sql, params, DoraCalculationUtils.getTimeSeries());
    }

    public DbListResponse<DbScmPullRequest> getDrillDownData(String company,
                                                 ScmPrFilter filter,
                                                 Map<String, SortingOrder> sortBy,
                                                 OUConfiguration ouConfig,
                                                 Integer pageNumber,
                                                 Integer pageSize,
                                                 VelocityConfigDTO velocityConfigDTO,
                                                 DefaultListRequest originalRequest)
            throws SQLException {
        Map<String, Object> params = new HashMap<>();
        String regexPatternCondition, regexPatternConditionForCommits;

        String prsWhere = "";
        String filterByProductSQL = "";
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (PR_SORTABLE_COLUMNS.contains(entry.getKey())) {
                        return entry.getKey();
                    }
                    return "pr_updated_at";
                })
                .orElse("pr_updated_at");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        if (CollectionUtils.isNotEmpty(filter.getOrgProductIds())) {
            filterByProductSQL = scmAggService.getUnionSqlForPrsList(company, filter, params, ouConfig);
        }

        Map<String, List<String>> conditions = scmAggService.createPrWhereClauseAndUpdateParams(company, params, filter, null, "", ouConfig, true);
        if (conditions.get(PRS_TABLE).size() > 0) {
            conditions.get(PRS_TABLE).add("merged");
            prsWhere = " WHERE " + String.join(" AND ", conditions.get(PRS_TABLE));
        }

        boolean needCommentors = checkCommentDensityFiltersJoin(filter, filter.getCalculation(), false);
        boolean needCreators = checkCreatorsFiltersJoin(filter, ouConfig);
        boolean needReviewers = checkReviewersFiltersJoin(filter, filter.getCalculation(), false);
        boolean needApprovers = checkApproversFiltersJoin(filter, filter.getCalculation(), false);


        String prsSelect = ScmQueryUtils.PRS_SELECT;
        String creatorsSelect = ScmQueryUtils.CREATORS_SQL;
        String creatorJoin = needCreators ? ScmQueryUtils.sqlForCreatorTableJoin(company) : StringUtils.EMPTY;
        String reviewerJoin = needReviewers ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.REVIEWER) : StringUtils.EMPTY;
        String approverJoin = needApprovers ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.APPROVER) : StringUtils.EMPTY;
        String commenterJoin = needCommentors ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.COMMENTER) : StringUtils.EMPTY;
        String prJiraMappingJoin = ScmQueryUtils.getSqlForPRJiraMappingTable(company);
        String prWorkItemIdsMappingJoin = ScmQueryUtils.getSqlForPRWorkItemMappingTable(company);
        String prsReviewedAtJoin =  needReviewers ? ScmQueryUtils.getSqlForPRReviewedAtJoin(company) : StringUtils.EMPTY;
        String approvalStatusSelect = needApprovers ? ScmQueryUtils.getApprovalStatusSqlStmt() : StringUtils.EMPTY;
        String codeChangeSql = getCodeChangeSql(filter.getCodeChangeSizeConfig(), true,filter.getCodeChangeUnit());
        String commentDensitySql = needCommentors ? ScmQueryUtils.getCommentDensitySql(filter) : StringUtils.EMPTY;
        String collaborationStateSql = needCommentors ? ScmQueryUtils.getCollaborationStateSql() : StringUtils.EMPTY;
        String issueKeysSql = ScmQueryUtils.getIssueKeysSql();
        String jiraWorkitemPrsMappingSelect = ScmQueryUtils.JIRA_WORKITEM_PRS_MAPPING_SELECT;
        String prApprovalTimeSql = needApprovers ? ScmQueryUtils.PR_APPROVE_TIME_SQL : StringUtils.EMPTY;
        String prCommentTimeSql =  needCommentors ? ScmQueryUtils.PR_COMMENT_TIME_SQL : StringUtils.EMPTY;
        String tagsTableJoin = ScmQueryUtils.getTagsTableJoin(company, filter.getIntegrationIds());
        Map<String, List<String>> deployment = null;

        boolean isTagsJoinRequired = false;
        if (originalRequest.getWidget().equals(DEPLOYMENT_FREQUENCY)) {
            Map<String, Map<String, List<String>>> profileFilter = velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getScmFilters();
            deployment = buildRegexMapWithNewProfile(profileFilter, params, "deploy", DEPLOY_TABLE);
            params.put("integration_ids", Arrays.asList(velocityConfigDTO.getDeploymentFrequency().getIntegrationId()));
            isTagsJoinRequired = isTagsJoinRequired(profileFilter);
        } else if (originalRequest.getWidget().equals(CHANGE_FAILURE_RATE)) {
            Map<String, Map<String, List<String>>> profileFilter = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getScmFilters();
            deployment = buildRegexMapWithNewProfile(profileFilter, params, "deploy", DEPLOY_TABLE);
            params.put("integration_ids", Arrays.asList(velocityConfigDTO.getChangeFailureRate().getIntegrationId()));
            isTagsJoinRequired = isTagsJoinRequired(profileFilter);
        }

        String intrCond = deployment.get(DEPLOY_TABLE) != null && deployment.get(DEPLOY_TABLE).size() > 0 ?
                String.join(" OR ", deployment.get(DEPLOY_TABLE)) : StringUtils.EMPTY;
        regexPatternCondition = StringUtils.isNotEmpty(intrCond) ? " AND ( " + intrCond + ")" : StringUtils.EMPTY;
        String intrCondForCommits = deployment.get(DIRECT_MERGE_TABLE) != null && deployment.get(DIRECT_MERGE_TABLE).size() > 0 ?
                String.join(" OR ", deployment.get(DIRECT_MERGE_TABLE)) : StringUtils.EMPTY;
        regexPatternConditionForCommits = StringUtils.isNotEmpty(intrCondForCommits) ? " AND ( " + intrCondForCommits + ")" : StringUtils.EMPTY;
        String commitsPRsJoin = ScmQueryUtils.getCommitsPRsJoinForDrillDown(company, params, filter, "", "", regexPatternConditionForCommits);

        creatorsSelect = needCreators ? creatorsSelect : " ,creator ";
        String reviewSQL = (needReviewers ? (filter.getHasComments() != null && filter.getHasComments() ? " INNER JOIN ( SELECT MIN(reviewed_at) as pr_reviewed_at,pr_id FROM "
                + company + "." + REVIEWS_TABLE + " GROUP BY pr_id ) AS first_reviews ON scm_pullrequests.id = first_reviews.pr_id" : StringUtils.EMPTY)
                : StringUtils.EMPTY);
        String reviewSelfReviewAndReviewCount = needReviewers ? (ScmQueryUtils.getReviewType() + ScmQueryUtils.PRS_REVIEWER_COUNT + PRS_APPROVER_COUNT) : StringUtils.EMPTY;

        if (StringUtils.isEmpty(filterByProductSQL)) {
            filterByProductSQL = "SELECT * FROM ( SELECT " + prsSelect + creatorsSelect
                    + ScmQueryUtils.COMMITS_PRS_SELECT + (needReviewers ? ScmQueryUtils.REVIEW_PRS_SELECT_LIST : StringUtils.EMPTY)
                    + (needApprovers ? ScmQueryUtils.APPROVERS_PRS_LIST : StringUtils.EMPTY) + (needCommentors ? ScmQueryUtils.COMMENTERS_SELECT_SQL : StringUtils.EMPTY)
                    + jiraWorkitemPrsMappingSelect + (needReviewers ? ScmQueryUtils.PRS_REVIEWED_AT_SELECT : StringUtils.EMPTY)
                    + prApprovalTimeSql + prCommentTimeSql
                    + approvalStatusSelect + codeChangeSql + collaborationStateSql + reviewSelfReviewAndReviewCount
                    + commentDensitySql + issueKeysSql
                    + ScmQueryUtils.RESOLUTION_TIME_SQL + (isTagsJoinRequired ? ",tags FROM " : " FROM ")
                    + company + "." + PRS_TABLE
                    + reviewSQL
                    + creatorJoin
                    + reviewerJoin
                    + approverJoin
                    + commenterJoin
                    + commitsPRsJoin;

            if (isTagsJoinRequired) {
                filterByProductSQL += tagsTableJoin;
            }

            filterByProductSQL += prJiraMappingJoin
                    + prWorkItemIdsMappingJoin
                    + prsReviewedAtJoin
                    + " ) a " + prsWhere + regexPatternCondition;
        }

        List<DbScmPullRequest> results = List.of();
        if (pageSize > 0) {
            String sql = "SELECT *"
                    + " FROM (" + filterByProductSQL + ") x ORDER BY " + sortByKey + " " + sortOrder
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbScmConverters.prRowMapper());
        }
        String countSql = "SELECT COUNT(*) FROM (" + filterByProductSQL + ") a";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbScmCommit> getScmCommitDrillDownData(String company,
                                                                 ScmCommitFilter scmCommitFilter,
                                                                 OUConfiguration ouConfig,
                                                                 Map<String, Map<String, List<String>>> scmFilters,
                                                                 Integer pageNumber,
                                                                 Integer pageSize,
                                                                 Map<String, SortingOrder> sortBy) {
        Map<String, Object> params = new HashMap<>();
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (COMMIT_SORTABLE_COLUMNS.contains(entry.getKey())) {
                        return entry.getKey();
                    }
                    return "commit_pushed_at";
                })
                .orElse("commit_pushed_at");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        String commitsWhere = StringUtils.EMPTY;

        Map<String, List<String>> deployment = buildRegexMapWithNewProfile(scmFilters, params, "deploy", DEPLOY_TABLE);
        String intrCondForCommits = deployment.get(DIRECT_MERGE_TABLE) != null && deployment.get(DIRECT_MERGE_TABLE).size() > 0 ?
                String.join(" OR ", deployment.get(DIRECT_MERGE_TABLE)) : StringUtils.EMPTY;
        String regexPatternConditionForCommits = StringUtils.isNotEmpty(intrCondForCommits) ? " AND ( " + intrCondForCommits + ")" : StringUtils.EMPTY;

        boolean needCommitters = checkCommitters(scmCommitFilter, ouConfig);
        String committerTableJoin = needCommitters ? ScmQueryUtils.sqlForCommitterTableJoin(company) : StringUtils.EMPTY;
        String committersSelect = needCommitters ? ScmQueryUtils.COMMITTERS_SELECT : StringUtils.EMPTY;

        boolean needAuthors = checkAuthors(scmCommitFilter, ouConfig);
        String authorTableJoin = needAuthors ? ScmQueryUtils.sqlForAuthorTableJoin(company) : StringUtils.EMPTY;
        String authorsSelect = needAuthors ? ScmQueryUtils.AUTHORS_SELECT : StringUtils.EMPTY;

        Map<String, List<String>> commitConditions = scmAggService.createCommitsWhereClauseAndUpdateParams(company,
                params, scmCommitFilter, null, "", false, ouConfig);

        String commitsSelect = ScmQueryUtils.SCM_COMMITS_SELECT + ", committer as creator, committer_id::varchar as creator_id";

        commitConditions.get(COMMITS_TABLE).add("direct_merge");
        if (commitConditions.get(COMMITS_TABLE).size() > 0) {
            commitsWhere = " WHERE " + String.join(" AND ", commitConditions.get(COMMITS_TABLE));
            commitsWhere = commitsWhere + addPushedAtCondition("", scmCommitFilter);
        }
        if (CollectionUtils.isNotEmpty(scmCommitFilter.getRepoIds())) {
            if (commitsWhere.equals("")) {
                commitsWhere = " WHERE repo_ids IN (:repo_ids) ";
            } else {
                commitsWhere = commitsWhere + " AND  repo_ids IN (:repo_ids) ";
            }
        }

        String finalSql = "SELECT " + SCM_COMMIT_OUTER_SELECT
                + "  FROM (SELECT unnest(scm_commits.repo_id) AS repo_id, direct_merge, "
                + commitsSelect + committersSelect + authorsSelect
                + " FROM " + company + "." + COMMITS_TABLE
                + committerTableJoin
                + authorTableJoin
                + " ) b" + commitsWhere + regexPatternConditionForCommits;

        List<DbScmCommit> results = List.of();
        if (pageSize > 0) {
            String sql = "SELECT *"
                    + " FROM (" + finalSql + ") x ORDER BY " + sortByKey + " " + sortOrder
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, (rs, rowNumber) -> {
                return DbScmCommit.builder()
                        .id(rs.getString("id"))
                        .commitSha(rs.getString("commit_sha"))
                        .committer(rs.getString("creator"))
                        .message(rs.getString("message"))
                        .createdAt(rs.getLong("created_at"))
                        .committedAt(rs.getTimestamp("committed_at").toInstant().getEpochSecond())
                        .commitPushedAt(rs.getTimestamp("commit_pushed_at").toInstant().getEpochSecond())
                        .branch(rs.getString("commit_branch"))
                        .build();
            });
        }
        String countSql = "SELECT COUNT(*) FROM (" + finalSql + ") a";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    private boolean checkDirectMergeNewConfigForDeploymentFrequency(VelocityConfigDTO velocityConfigDTO) {
        return velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getScmFilters() != null
                && velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getScmFilters().get("commit_branch") != null
                && velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getScmFilters().get("commit_branch").size() > 0;
    }

    private boolean checkDirectMergeNewConfigForFailedDeployment(VelocityConfigDTO velocityConfigDTO) {
        return velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getScmFilters()!=null
                && velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getScmFilters().get("commit_branch") != null
                && velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getScmFilters().get("commit_branch").size() > 0;
        }

    private boolean checkDirectMergeNewConfigForTotalDeployment(VelocityConfigDTO velocityConfigDTO) {
        return velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().getScmFilters()!=null
                && velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().getScmFilters().get("commit_branch") != null
                && velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().getScmFilters().get("commit_branch").size() > 0;
         }

    private Map<String, List<String>> buildRegexMapWithNewProfile(Map<String, Map<String, List<String>>> velocityConfigDTO,
                                                                  Map<String, Object> params,
                                                                  String suffix,
                                                                  String tableName) {
        List<String> doraConditions = new ArrayList<>();
        if (velocityConfigDTO.size() == 0)
            return Map.of();
        return ScmConditionBuilder.buildNewRegexPatternMap(params, velocityConfigDTO, doraConditions, tableName, suffix);
    }

    private boolean isTagsJoinRequired(Map<String, Map<String, List<String>>> velocityConfigDTO) {

        for (Map.Entry<String, Map<String, List<String>>> partialMatchEntry : velocityConfigDTO.entrySet()) {
            String field = partialMatchEntry.getKey();
            Map<String, List<String>> matchEntryValue = partialMatchEntry.getValue();
            if ("tags".equals(field))
                return true;
        }
        return false;
    }

    private String addPushedAtCondition(String commitTblQualifier, ScmCommitFilter commitFilter) {

        String pushedAtPrCondition = StringUtils.EMPTY;
        ImmutablePair<Long, Long> commitPushedAtRange = commitFilter.getCommitPushedAtRange();
        if (commitPushedAtRange != null) {
            if (commitPushedAtRange.getLeft() != null) {
                pushedAtPrCondition = pushedAtPrCondition.concat("  AND " + commitTblQualifier + "commit_pushed_at > TO_TIMESTAMP(" + commitPushedAtRange.getLeft() + ")");
            }
            if (commitPushedAtRange.getRight() != null) {
                pushedAtPrCondition = pushedAtPrCondition.concat("  AND " + commitTblQualifier + "commit_pushed_at < TO_TIMESTAMP(" + commitPushedAtRange.getRight() + ")");
            }
        }
        return pushedAtPrCondition;
    }


}
