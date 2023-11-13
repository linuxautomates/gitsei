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
import io.levelops.commons.databases.services.ScmQueryUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.dora.DoraCalculationUtils;
import io.levelops.commons.utils.dora.DoraQueryUtils;
import io.levelops.commons.utils.dora.DoraValidationsUtils;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
public class ScmDeployFrequencyFailureRateCalculation {

    private final NamedParameterJdbcTemplate template;

    private final ScmAggService scmAggService;

    public static final String PRS_TABLE = "scm_pullrequests";
    public static final String COMMITS_TABLE = "scm_commits";

    public static final String DEPLOY_TABLE = "deployment";
    public static final String DIRECT_MERGE_TABLE = "direct_merge";
    public static final String REVIEWS_TABLE = "scm_pullrequest_reviews";

    public static final Set<String> PR_SORTABLE_COLUMNS = Set.of("pr_updated_at", "pr_merged_at", "pr_created_at", "pr_closed_at",
            "title", "project", "assignees", "approver", "reviewer", "creator", "cycle_time", "lines_added", "lines_deleted"
            , "lines_changed", "files_ct", "repo_id", "approval_status", "review_type");

    public static final Set<String> COMMIT_SORTABLE_COLUMNS = Set.of("created_at", "committed_at", "commit_pushed_at");


    @Autowired
    public ScmDeployFrequencyFailureRateCalculation(final DataSource dataSource, ScmAggService scmAggService) {
        template = new NamedParameterJdbcTemplate(dataSource);
        this.scmAggService = scmAggService;
    }

    private List<DoraTimeSeriesDTO.TimeSeriesData> getPrBasedTimeSeriesData(String company,
                                                                            ScmPrFilter scmPrFilter,
                                                                            OUConfiguration ouConfig,
                                                                            String deploymentCriteria,
                                                                            Map<String, Map<String, List<String>>> scmFilters,
                                                                            String calculationOnField) {

        ScmPrFilter.CALCULATION calculation = scmPrFilter.getCalculation();
        Map<String, Object> params = new HashMap<>();

        String regexPatternCondition;
        String prsWhere = StringUtils.EMPTY;

        boolean needCommitsTable = checkCommitsTableFiltersJoin(scmPrFilter, calculation, false);
        boolean needCommentors = checkCommentDensityFiltersJoin(scmPrFilter, calculation, false);
        boolean needReviewedAt = checkPRReviewedJoin(scmPrFilter);
        boolean needCreators = checkCreatorsFiltersJoin(scmPrFilter, ouConfig);
        boolean needReviewers = checkReviewersFiltersJoin(scmPrFilter, calculation, false);
        boolean needApprovers = checkApproversFiltersJoin(scmPrFilter, calculation, false);

        Map<String, List<String>> prConditions = scmAggService.createPrWhereClauseAndUpdateParams(company,
                params, scmPrFilter, null, "", ouConfig);

        String startWith = "pr_merged_at";
        if("pr_merged_at".equals(calculationOnField))
            startWith = "pr_closed_at";
        final String removeCondition = startWith;
        prConditions.get(PRS_TABLE).removeIf(s->s.startsWith(removeCondition));

        if("pr_merged".equals(deploymentCriteria)) {
            prConditions.get(PRS_TABLE).add("merged");
        } else if ("pr_closed".equals(deploymentCriteria)) {
            prConditions.get(PRS_TABLE).add("pr_closed_at is not null");
        } else if ("pr_merged_closed".equals(deploymentCriteria)) {
            prConditions.get(PRS_TABLE).add("merged");
            prConditions.get(PRS_TABLE).add("pr_closed_at is not null");
        }

        if (prConditions.get(PRS_TABLE).size() > 0) {
            prsWhere = " WHERE " + String.join(" AND ", prConditions.get(PRS_TABLE));
        }

        Map<String, List<String>> deployment = DoraCalculationUtils.buildRegexMapWithNewProfile(scmFilters, params, "deploy", DEPLOY_TABLE);
        String intrCond = deployment.get(DEPLOY_TABLE) != null && deployment.get(DEPLOY_TABLE).size() > 0 ?
                String.join(" OR ", deployment.get(DEPLOY_TABLE)) : StringUtils.EMPTY;
        regexPatternCondition = StringUtils.isNotEmpty(intrCond) ? " AND ( " + intrCond + ")" : StringUtils.EMPTY;

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

        if (CollectionUtils.isNotEmpty(scmPrFilter.getRepoIds())) {
            if (prsWhere.equals("")) {
                prsWhere = " WHERE repo_ids IN (:repo_ids) ";
            } else {
                prsWhere = prsWhere + " AND  repo_ids IN (:repo_ids) ";
            }
        }

        String intermediateSQL = "SELECT id, " + calculationOnField +" as trend_interval "
                + " FROM (SELECT " + " unnest(scm_pullrequests.repo_id) AS repo_ids, " + ScmQueryUtils.PRS_SELECT
                + reviewsTableSelect + approversTableSelect + commentersSelect + creatorsSelect
                + " FROM " + company + "." + PRS_TABLE
                + creatorJoin
                + reviewerJoin
                + approverJoin
                + commenterJoin
                + commitsPRsJoin
                + prsReviewedAtJoin
                + " ) a"
                + prsWhere
                + regexPatternCondition;

        String sql = DoraQueryUtils.deploymentFrequencyInitQuery()
                + "SELECT id, trend_interval "
                + " FROM (" + intermediateSQL + " ) x "
                + DoraQueryUtils.deploymentFrequencyEndQuery() ;

        log.info("sql = {}", sql);
        log.info("params = {}", params);

        return template.query(sql, params, DoraCalculationUtils.getTimeSeries());
    }

    private List<DoraTimeSeriesDTO.TimeSeriesData> getCommitBasedTimeSeriesData(String company,
                                                                                ScmCommitFilter scmCommitFilter,
                                                                                OUConfiguration ouConfig,
                                                                                String deploymentCriteria,
                                                                                Map<String, Map<String, List<String>>> scmProfileFilters,
                                                                                String calculationOnField) {

        Map<String, Object> params = new HashMap<>();

        String regexPatternConditionForCommits = StringUtils.EMPTY;
        String commitsWhere = StringUtils.EMPTY;

        boolean needCommitters = checkCommitters(scmCommitFilter, ouConfig);
        boolean needAuthors = checkAuthors(scmCommitFilter, ouConfig);

        Map<String, List<String>> commitConditions = scmAggService.createCommitsWhereClauseAndUpdateParams(company,
                params, scmCommitFilter, null, "", false, ouConfig);

        if("commit_merged_to_branch".equals(deploymentCriteria)
                || "commit_merged_to_branch_with_tag".equals(deploymentCriteria)) {
            commitConditions.get(COMMITS_TABLE).add("direct_merge");
        }

        if (commitConditions.get(COMMITS_TABLE).size() > 0) {
            String startWith = "committed_at";
            if("committed_at".equals(calculationOnField))
                startWith = "commit_pushed_at";

            final String removeCondition = startWith;
            commitConditions.get(COMMITS_TABLE).removeIf(s->s.startsWith(removeCondition));
            commitsWhere = " WHERE " + String.join(" AND ", commitConditions.get(COMMITS_TABLE));

            if("commit_pushed_at".equals(calculationOnField))
                commitsWhere = commitsWhere + DoraCalculationUtils.addPushedAtCondition("", scmCommitFilter);
        }
        else {
            commitsWhere = " WHERE " + DoraCalculationUtils.addPushedAtCondition("", scmCommitFilter);
        }

        Map<String, List<String>> deployment = DoraCalculationUtils.buildRegexMapWithNewProfile(scmProfileFilters, params, "deploy", DEPLOY_TABLE);

        String intrCondForCommits = deployment.get(DIRECT_MERGE_TABLE) != null && deployment.get(DIRECT_MERGE_TABLE).size() > 0 ?
                String.join(" OR ", deployment.get(DIRECT_MERGE_TABLE)) : StringUtils.EMPTY;
        regexPatternConditionForCommits = StringUtils.isNotEmpty(intrCondForCommits) ? " AND ( " + intrCondForCommits + ")" : StringUtils.EMPTY;

        String committersSelect = needCommitters ? ScmQueryUtils.COMMITTERS_SELECT : StringUtils.EMPTY;
        String authorsSelect = needAuthors ? ScmQueryUtils.AUTHORS_SELECT : StringUtils.EMPTY;
        String authorTableJoin = needAuthors ? ScmQueryUtils.sqlForAuthorTableJoin(company) : StringUtils.EMPTY;
        String committerTableJoin = needCommitters ? ScmQueryUtils.sqlForCommitterTableJoin(company) : StringUtils.EMPTY;

        if (CollectionUtils.isNotEmpty(scmCommitFilter.getRepoIds())) {
            if (commitsWhere.equals("")) {
                commitsWhere = " WHERE repo_ids IN (:repo_ids) ";
            } else {
                commitsWhere = commitsWhere + " AND  repo_ids IN (:repo_ids) ";
            }
        }

        boolean isTagsJoinRequired = DoraCalculationUtils.isTagsJoinRequired(scmProfileFilters);
        String commitsSelect = ScmQueryUtils.SCM_COMMITS_SELECT + ", committer as creator, committer_id::varchar as creator_id";

        String commitsStmt = "SELECT id, " + calculationOnField +" as trend_interval "
                + "  FROM (SELECT unnest(scm_commits.repo_id) AS repo_ids, direct_merge, "
                + commitsSelect + committersSelect + authorsSelect
                + (isTagsJoinRequired ? ", tags ":StringUtils.EMPTY)
                + " FROM " + company + "." + COMMITS_TABLE
                + (isTagsJoinRequired ?
                DoraQueryUtils.getTagTableJoinWithCommitTable(company, scmCommitFilter.getIntegrationIds()):StringUtils.EMPTY)
                + committerTableJoin
                + authorTableJoin
                + " ) b" + commitsWhere + regexPatternConditionForCommits;

        String sql = DoraQueryUtils.deploymentFrequencyInitQuery()
                + "SELECT id, trend_interval "
                + " FROM (" + commitsStmt + " ) x "
                + DoraQueryUtils.deploymentFrequencyEndQuery() ;

        log.info("sql = {}", sql);
        log.info("params = {}", params);

        return template.query(sql, params, DoraCalculationUtils.getTimeSeries());
    }

    public DoraResponseDTO calculateDeploymentFrequency(String company,
                                                        ScmPrFilter scmPrFilter,
                                                        ScmCommitFilter scmCommitFilter,
                                                        OUConfiguration ouConfig,
                                                        VelocityConfigDTO velocityConfigDTO) throws BadRequestException {

        DoraValidationsUtils.validateVelocityConfig(scmPrFilter, velocityConfigDTO);
        String deploymentRoute =  velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getDeploymentRoute().toString();
        String deploymentCriteria = velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getDeploymentCriteria().toString();
        String calculationField = velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getCalculationField().toString();
        List<DoraTimeSeriesDTO.TimeSeriesData> tempTimeSeriesResult;
        Map<String, Map<String, List<String>>> scmFilters = velocityConfigDTO.getDeploymentFrequency()
                .getVelocityConfigFilters().getDeploymentFrequency().getScmFilters();

        Long startTime = 0L;
        Long endTime = 0L;
        ImmutablePair<Long, Long> range;
        if("pr".equals(deploymentRoute)) {
            tempTimeSeriesResult = getPrBasedTimeSeriesData(company, scmPrFilter, ouConfig,
                    deploymentCriteria, scmFilters, calculationField);
            if ("pr_closed_at".equals(calculationField)) {
                startTime = scmPrFilter.getPrClosedRange().getLeft();
                endTime = scmPrFilter.getPrClosedRange().getRight();
                range = scmPrFilter.getPrClosedRange();
            } else  {
                startTime = scmPrFilter.getPrMergedRange().getLeft();
                endTime = scmPrFilter.getPrMergedRange().getRight();
                range = scmPrFilter.getPrMergedRange();
            }
        } else if ("commit".equals(deploymentRoute)) {
            tempTimeSeriesResult = getCommitBasedTimeSeriesData(company, scmCommitFilter, ouConfig,
                    deploymentCriteria, scmFilters, calculationField);
            if ("commit_pushed_at".equals(calculationField)) {
                startTime = scmCommitFilter.getCommitPushedAtRange().getLeft();
                endTime = scmCommitFilter.getCommitPushedAtRange().getRight();
                range = scmCommitFilter.getCommitPushedAtRange();
            } else  {
                startTime = scmCommitFilter.getCommittedAtRange().getLeft();
                endTime = scmCommitFilter.getCommittedAtRange().getRight();
                range = scmCommitFilter.getCommittedAtRange();
            }
        } else {
            throw new RuntimeException("Invalid Profile");
        }

        List<DoraTimeSeriesDTO.TimeSeriesData> filledTimeSeriesResult =
                DoraCalculationUtils.fillRemainingDates(startTime,
                        endTime,
                        tempTimeSeriesResult);

        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByDay = DoraCalculationUtils.convertTimeSeries("day", filledTimeSeriesResult);
        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByWeek = DoraCalculationUtils.convertTimeSeries("week", filledTimeSeriesResult);
        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByMonth = DoraCalculationUtils.convertTimeSeries("month", filledTimeSeriesResult);
        int total = CollectionUtils.emptyIfNull(filledTimeSeriesResult).stream().map(m -> m.getCount()).reduce(0,(a, b) -> a+b);
        double perDay = total/DoraCalculationUtils.getTimeDifference(range);
        DoraTimeSeriesDTO timeSeries = DoraTimeSeriesDTO.builder().day(timeSeriesByDay).month(timeSeriesByMonth).week(timeSeriesByWeek).build();
        DoraSingleStateDTO stats = DoraSingleStateDTO.builder().totalDeployment(total).countPerDay(perDay).band(DoraCalculationUtils.calculateDeploymentFrequencyBand(perDay)).build();
        return DoraResponseDTO.builder().timeSeries(timeSeries).stats(stats).build();
    }

    public DbListResponse<DbScmPullRequest> getPrBasedDrillDownData(String company,
                                                                     ScmPrFilter filter,
                                                                     Map<String, SortingOrder> sortBy,
                                                                     OUConfiguration ouConfig,
                                                                     Integer pageNumber,
                                                                     Integer pageSize,
                                                                     Map<String, Map<String, List<String>>> scmProfileFilters,
                                                                     String deploymentCriteria) {

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

        Map<String, List<String>> conditions = scmAggService.createPrWhereClauseAndUpdateParams(company, params, filter, null, "", ouConfig, true);
        if("pr_merged".equals(deploymentCriteria)) {
            conditions.get(PRS_TABLE).add("merged");
        } else if ("pr_closed".equals(deploymentCriteria)) {
            conditions.get(PRS_TABLE).add("pr_closed_at is not null");
        } else if ("pr_merged_closed".equals(deploymentCriteria)) {
            conditions.get(PRS_TABLE).add("merged");
            conditions.get(PRS_TABLE).add("pr_closed_at is not null");
        }

        prsWhere = " WHERE " + String.join(" AND ", conditions.get(PRS_TABLE));

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

        Map<String, List<String>> deployment = DoraCalculationUtils.buildRegexMapWithNewProfile(scmProfileFilters, params, "deploy", DEPLOY_TABLE);
        params.put("integration_ids", filter.getIntegrationIds().stream().map(s -> Integer.parseInt(s)).collect(Collectors.toList()));

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
                    + ScmQueryUtils.RESOLUTION_TIME_SQL +  " FROM "
                    + company + "." + PRS_TABLE
                    + reviewSQL
                    + creatorJoin
                    + reviewerJoin
                    + approverJoin
                    + commenterJoin
                    + commitsPRsJoin
                    + prJiraMappingJoin
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

    public DbListResponse<DbScmCommit> getCommitBasedDrillDownData(String company,
                                                                   ScmCommitFilter scmCommitFilter,
                                                                   OUConfiguration ouConfig,
                                                                   Map<String, Map<String, List<String>>> scmProfileFilters,
                                                                   Integer pageNumber,
                                                                   Integer pageSize,
                                                                   Map<String, SortingOrder> sortBy,
                                                                   String deploymentCriteria,
                                                                   String calculationField) {
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

        Map<String, List<String>> deployment = DoraCalculationUtils.buildRegexMapWithNewProfile(scmProfileFilters, params, "deploy", DEPLOY_TABLE);
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

        if("commit_merged_to_branch".equals(deploymentCriteria)
                || "commit_merged_to_branch_with_tag".equals(deploymentCriteria)) {
            commitConditions.get(COMMITS_TABLE).add("direct_merge");
        }

        if (commitConditions.get(COMMITS_TABLE).size() > 0) {
            commitsWhere = " WHERE " + String.join(" AND ", commitConditions.get(COMMITS_TABLE));
            if("commit_pushed_at".equals(calculationField))
                commitsWhere = commitsWhere + DoraCalculationUtils.addPushedAtCondition("", scmCommitFilter);
        }
        else {
            commitsWhere = " WHERE " + DoraCalculationUtils.addPushedAtCondition("", scmCommitFilter);
        }

        if (CollectionUtils.isNotEmpty(scmCommitFilter.getRepoIds())) {
            if (commitsWhere.equals("")) {
                commitsWhere = " WHERE repo_ids IN (:repo_ids) ";
            } else {
                commitsWhere = commitsWhere + " AND  repo_ids IN (:repo_ids) ";
            }
        }

        boolean isTagsJoinRequired = DoraCalculationUtils.isTagsJoinRequired(scmProfileFilters);

        String finalSql = "SELECT " + SCM_COMMIT_OUTER_SELECT
                + "  FROM (SELECT unnest(scm_commits.repo_id) AS repo_ids, direct_merge, "
                + commitsSelect + committersSelect + authorsSelect
                + (isTagsJoinRequired ? ", tags ":StringUtils.EMPTY)
                + " FROM " + company + "." + COMMITS_TABLE
                + (isTagsJoinRequired ?
                DoraQueryUtils.getTagTableJoinWithCommitTable(company, scmCommitFilter.getIntegrationIds()):StringUtils.EMPTY)
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

    public DoraResponseDTO calculateChangeFailureRate( String company,
                                                       ScmPrFilter scmPrFilter,
                                                       ScmCommitFilter scmCommitFilter,
                                                       OUConfiguration ouConfig,
                                                       VelocityConfigDTO velocityConfigDTO) throws BadRequestException {
        DoraValidationsUtils.validateVelocityConfig(scmPrFilter, velocityConfigDTO);
        boolean isAbsolute = velocityConfigDTO.getChangeFailureRate().getIsAbsoulte();

        List<DoraTimeSeriesDTO.TimeSeriesData> filledTimeSeriesByDayFailedDeployment =
                calculateFailedDeployment(company, scmPrFilter, scmCommitFilter, ouConfig, velocityConfigDTO);

        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByDay = DoraCalculationUtils.convertTimeSeries("day", filledTimeSeriesByDayFailedDeployment);
        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByWeek = DoraCalculationUtils.convertTimeSeries("week", filledTimeSeriesByDayFailedDeployment);
        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByMonth = DoraCalculationUtils.convertTimeSeries("month", filledTimeSeriesByDayFailedDeployment);
        DoraSingleStateDTO.DoraSingleStateDTOBuilder stats = DoraSingleStateDTO.builder().isAbsolute(isAbsolute);
        int totalFailed = CollectionUtils.emptyIfNull(filledTimeSeriesByDayFailedDeployment).stream().map(m -> m.getCount()).reduce(0,(a, b) -> a+b);
        stats.totalDeployment(totalFailed);
        DoraTimeSeriesDTO timeSeries = DoraTimeSeriesDTO.builder().day(timeSeriesByDay).month(timeSeriesByMonth).week(timeSeriesByWeek).build();

        if(velocityConfigDTO.getChangeFailureRate().getIsAbsoulte() != null
                && !velocityConfigDTO.getChangeFailureRate().getIsAbsoulte()) {

            List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByDayTotalDeployment =
                    calculateTotalDeployment(company, scmPrFilter, scmCommitFilter, ouConfig, velocityConfigDTO);

            int totalDeployment = CollectionUtils.emptyIfNull(timeSeriesByDayTotalDeployment).stream().map(m -> m.getCount()).reduce(0,(a, b) -> a+b);
            if(totalDeployment == 0)
                throw new RuntimeException("Invalid configuration for Change Failure Rate of WorkFlow Profile: "+ velocityConfigDTO.getName());

            double failureRate = Double.valueOf(totalFailed)*100/totalDeployment;

            stats.failureRate(failureRate).totalDeployment(totalDeployment)
                    .band(DoraCalculationUtils.calculateChangeFailureRateBand(failureRate));

        }
        return DoraResponseDTO.builder().timeSeries(timeSeries).stats(stats.build()).build();
    }

    private List<DoraTimeSeriesDTO.TimeSeriesData> calculateFailedDeployment(String company,
                                                                              ScmPrFilter scmPrFilter,
                                                                              ScmCommitFilter scmCommitFilter,
                                                                              OUConfiguration ouConfig,
                                                                              VelocityConfigDTO velocityConfigDTO) {

        String deploymentRoute =  velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters()
                .getFailedDeployment().getDeploymentRoute().toString();
        String deploymentCriteria = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters()
                .getFailedDeployment().getDeploymentCriteria().toString();
        String calculationField = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters()
                .getFailedDeployment().getCalculationField().toString();
        Map<String, Map<String, List<String>>> scmFilters = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters()
                .getFailedDeployment().getScmFilters();

        List<DoraTimeSeriesDTO.TimeSeriesData> tempTimeSeriesResult;
        Long startTime = 0L;
        Long endTime = 0L;
        if("pr".equals(deploymentRoute)) {
            tempTimeSeriesResult = getPrBasedTimeSeriesData(company, scmPrFilter, ouConfig,
                    deploymentCriteria, scmFilters, calculationField);
            if ("pr_closed_at".equals(calculationField)) {
                startTime = scmPrFilter.getPrClosedRange().getLeft();
                endTime = scmPrFilter.getPrClosedRange().getRight();
            } else  {
                startTime = scmPrFilter.getPrMergedRange().getLeft();
                endTime = scmPrFilter.getPrMergedRange().getRight();
            }
        } else if ("commit".equals(deploymentRoute)) {
            tempTimeSeriesResult = getCommitBasedTimeSeriesData(company, scmCommitFilter, ouConfig,
                    deploymentCriteria, scmFilters, calculationField);
            if ("commit_pushed_at".equals(calculationField)) {
                startTime = scmCommitFilter.getCommitPushedAtRange().getLeft();
                endTime = scmCommitFilter.getCommitPushedAtRange().getRight();
            } else  {
                startTime = scmCommitFilter.getCommittedAtRange().getLeft();
                endTime = scmCommitFilter.getCommittedAtRange().getRight();
            }
        } else {
            throw new RuntimeException("Invalid Profile");
        }
       return DoraCalculationUtils.fillRemainingDates(startTime, endTime, tempTimeSeriesResult);
    }

    private List<DoraTimeSeriesDTO.TimeSeriesData> calculateTotalDeployment(String company,
                                                                            ScmPrFilter scmPrFilter,
                                                                            ScmCommitFilter scmCommitFilter,
                                                                            OUConfiguration ouConfig,
                                                                            VelocityConfigDTO velocityConfigDTO) {
        String deploymentRoute =  velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters()
                .getTotalDeployment().getDeploymentRoute().toString();
        String deploymentCriteria = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters()
                .getTotalDeployment().getDeploymentCriteria().toString();
        String calculationField = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters()
                .getTotalDeployment().getCalculationField().toString();
        Map<String, Map<String, List<String>>> scmFilters = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters()
                .getTotalDeployment().getScmFilters();

        if("pr".equals(deploymentRoute)) {
            return getPrBasedTimeSeriesData(company, scmPrFilter, ouConfig,
                    deploymentCriteria, scmFilters, calculationField);
        } else if ("commit".equals(deploymentRoute)) {
            return getCommitBasedTimeSeriesData(company, scmCommitFilter, ouConfig,
                    deploymentCriteria, scmFilters, calculationField);
        } else {
            throw new RuntimeException("Invalid Profile");
        }
    }

}
