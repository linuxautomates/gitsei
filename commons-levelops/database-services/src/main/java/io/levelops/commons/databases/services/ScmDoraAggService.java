package io.levelops.commons.databases.services;

import io.levelops.commons.databases.converters.DbScmConverters;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.models.DbListResponse;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.levelops.commons.databases.services.ScmConditionBuilder.checkFilterConfigValidity;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkApproversFiltersJoin;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkAuthors;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkCommentDensityFiltersJoin;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkCommitsTableFiltersJoin;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkCommitters;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkCreatorsFiltersJoin;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkPRReviewedJoin;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkReviewersFiltersJoin;
import static io.levelops.commons.databases.services.ScmQueryUtils.getCommitsJoinForScmDora;

@Log4j2
@Service
public class ScmDoraAggService extends DatabaseService<DbScmPullRequest> {

    public static final String PRS_TABLE = "scm_pullrequests";
    public static final String COMMITS_TABLE = "scm_commits";
    public static final String DEFECT_TABLE = "defects";
    public static final String RELEASE_TABLE = "release";
    public static final String HOTFIX_TABLE = "hotfix";
    public static final String DEPLOY_TABLE = "deployment";
    public static final String DIRECT_MERGE_TABLE = "direct_merge";
    private final NamedParameterJdbcTemplate template;
    private final ScmAggService scmAggService;

    @Autowired
    public ScmDoraAggService(final DataSource dataSource, ScmAggService scmAggService) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        this.scmAggService = scmAggService;
    }

    @Override
    public String insert(String company, DbScmPullRequest t) throws SQLException {
        return null;
    }

    @Override
    public Boolean update(String company, DbScmPullRequest t) throws SQLException {
        return null;
    }

    @Override
    public Optional<DbScmPullRequest> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    @Override
    public DbListResponse<DbScmPullRequest> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return null;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return null;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        return null;
    }

    public DbListResponse<DbAggregationResult> generateDeploymentFrequency(String company,
                                                                           ScmPrFilter scmPrFilter,
                                                                           ScmCommitFilter scmCommitFilter,
                                                                           OUConfiguration ouConfig,
                                                                           VelocityConfigDTO velocityConfigDTO) throws BadRequestException {
        checkFilterConfigValidity(scmPrFilter, velocityConfigDTO);
        ScmPrFilter.DISTINCT DISTINCT = scmPrFilter.getAcross();
        Validate.notNull(DISTINCT, "Across cant be missing for groupby query.");
        ScmPrFilter.CALCULATION calculation = scmPrFilter.getCalculation();
        Map<String, Object> params = new HashMap<>();
        String selectDistinctString = "";
        String groupByString = "";
        String outerSelectDistinctString = "";
        String innerSelectDistinctString = "";
        String intermediateSQL;
        String regexPatternCondition, regexPatternConditionForCommits;
        String prsWhere = StringUtils.EMPTY;
        String commitsWhere = StringUtils.EMPTY;
        String doraMetricCalculationSql, internalCalculationComponent, resultCalculationComponent;
        Map<String, List<String>> prConditions;
        Map<String, List<String>> commitConditions;
        boolean needCommitsTable = checkCommitsTableFiltersJoin(scmPrFilter, calculation, false);
        boolean needCommentors = checkCommentDensityFiltersJoin(scmPrFilter, calculation, false);
        boolean needReviewedAt = checkPRReviewedJoin(scmPrFilter);
        boolean needCreators = checkCreatorsFiltersJoin(scmPrFilter, ouConfig);
        boolean needCommitters = checkCommitters(scmCommitFilter, ouConfig);
        boolean needAuthors = checkAuthors(scmCommitFilter, ouConfig);
        boolean needReviewers = checkReviewersFiltersJoin(scmPrFilter, calculation, false);
        boolean needApprovers = checkApproversFiltersJoin(scmPrFilter, calculation, false);

        Long lowerTimeLimit = new Date().toInstant().minus(30, ChronoUnit.DAYS).getEpochSecond();
        Long upperTimeLimit = new Date().toInstant().getEpochSecond();
        if(scmPrFilter.getPrMergedRange() == null) {
            scmPrFilter = scmPrFilter.toBuilder().prMergedRange(ImmutablePair.of(lowerTimeLimit, upperTimeLimit)).build();
        }
        if(scmCommitFilter.getCommittedAtRange() == null){
            scmCommitFilter = scmCommitFilter.toBuilder().committedAtRange(ImmutablePair.of(lowerTimeLimit, upperTimeLimit)).build();
        }
        
        prConditions = scmAggService.createPrWhereClauseAndUpdateParams(company, params, scmPrFilter, null, "", ouConfig);
        commitConditions = scmAggService.createCommitsWhereClauseAndUpdateParams(company, params, scmCommitFilter, null, "", false, ouConfig);
        long numDays = (scmPrFilter.getPrMergedRange().getRight() - scmPrFilter.getPrMergedRange().getLeft())/86400 + 1;
        boolean needCommitsUnionJoin = checkDirectMergeConfigForDeploymentFrequency(velocityConfigDTO);
        prConditions.get(PRS_TABLE).add("merged");
        commitConditions.get(COMMITS_TABLE).add("direct_merge");
        String key = "band";
        if (prConditions.get(PRS_TABLE).size() > 0) {
            prsWhere = " WHERE " + String.join(" AND ", prConditions.get(PRS_TABLE));
        }
        if (commitConditions.get(COMMITS_TABLE).size() > 0) {
            commitsWhere = " WHERE " + String.join(" AND ", commitConditions.get(COMMITS_TABLE));
        }
        doraMetricCalculationSql = ScmQueryUtils.getDeploymentFrequencySql()
                + ", CASE WHEN deployment_frequency IS NULL THEN 0 ELSE deployment_frequency END deployment_frequency, ct";
        resultCalculationComponent = "ct/"+numDays+"::float AS deployment_frequency, ct";
        internalCalculationComponent = " Count(DISTINCT id) AS ct";
        Map<String, List<String>> deployment = buildRegexMapWithProfile(MapUtils.emptyIfNull(velocityConfigDTO.getScmConfig().getDeployment()), params,
                "deploy", DEPLOY_TABLE);
        String intrCond = deployment.get(DEPLOY_TABLE) != null && deployment.get(DEPLOY_TABLE).size() > 0 ?
                String.join(" OR ", deployment.get(DEPLOY_TABLE)) : StringUtils.EMPTY;
        regexPatternCondition = StringUtils.isNotEmpty(intrCond) ? " AND ( " + intrCond + ")" : StringUtils.EMPTY;
        String intrCondForCommits = deployment.get(DIRECT_MERGE_TABLE) != null && deployment.get(DIRECT_MERGE_TABLE).size() > 0 ?
                String.join(" OR ", deployment.get(DIRECT_MERGE_TABLE)) : StringUtils.EMPTY;
        regexPatternConditionForCommits = StringUtils.isNotEmpty(intrCondForCommits) ? " AND ( " + intrCondForCommits + ")" : StringUtils.EMPTY;
        switch (DISTINCT) {
            case project:
                selectDistinctString = DISTINCT.toString();
                groupByString = DISTINCT.toString();
                outerSelectDistinctString = DISTINCT.toString();
                innerSelectDistinctString = outerSelectDistinctString;
                break;
            case repo_id:
                groupByString = "repo_ids";
                selectDistinctString = "repo_ids";
                outerSelectDistinctString = groupByString;
                innerSelectDistinctString = outerSelectDistinctString;
                break;
            case creator:
                needCreators = true;
                groupByString = "creator_id, " + scmPrFilter.getAcross().name();
                selectDistinctString = "creator_id, " + scmPrFilter.getAcross().name();
                outerSelectDistinctString = selectDistinctString;
                innerSelectDistinctString = outerSelectDistinctString;
                break;
            case none:
                groupByString = "";
                selectDistinctString = "";
                outerSelectDistinctString = "";
                innerSelectDistinctString = "";
                break;
        }
        String creatorJoin = needCreators ? ScmQueryUtils.sqlForCreatorTableJoin(company) : StringUtils.EMPTY;
        String reviewerJoin = needReviewers ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.REVIEWER) : StringUtils.EMPTY;
        String approverJoin = needApprovers ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.APPROVER) : StringUtils.EMPTY;
        String commenterJoin = needCommentors ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.COMMENTER) : StringUtils.EMPTY;
        String commitsPRsJoin = needCommitsTable ? getCommitsJoinForScmDora(company, scmPrFilter.getIntegrationIds()) : StringUtils.EMPTY;
        String prsReviewedAtJoin = needReviewedAt ? ScmQueryUtils.getSqlForPRReviewedAtJoin(company) : StringUtils.EMPTY;
        String tagsTableJoin = ScmQueryUtils.getTagsTableJoin(company, scmPrFilter.getIntegrationIds());
        String reviewsTableSelect = needReviewers ? ScmQueryUtils.REVIEW_PRS_SELECT_LIST : StringUtils.EMPTY;
        String approversTableSelect = needApprovers ? ScmQueryUtils.APPROVERS_PRS_LIST : StringUtils.EMPTY;
        String commentersSelect = needCommentors ? ScmQueryUtils.COMMENTERS_SELECT_SQL : StringUtils.EMPTY;
        String creatorsSelect = needCreators ? ScmQueryUtils.CREATORS_SQL : StringUtils.EMPTY;
        String committersSelect = needCommitters ? ScmQueryUtils.COMMITTERS_SELECT : StringUtils.EMPTY;
        String authorsSelect = needAuthors ? ScmQueryUtils.AUTHORS_SELECT : StringUtils.EMPTY;
        String authorTableJoin = needAuthors ? ScmQueryUtils.sqlForAuthorTableJoin(company) : StringUtils.EMPTY;
        String committerTableJoin = needCommitters ? ScmQueryUtils.sqlForCommitterTableJoin(company) : StringUtils.EMPTY;
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

        String commitsUnionStmt = needCommitsUnionJoin ? " UNION SELECT id" + (StringUtils.isNotEmpty(selectDistinctString) ? ", " + selectDistinctString : "")
                + "  FROM (SELECT unnest(scm_commits.repo_id) AS repo_ids, direct_merge, "
                + commitsSelect + committersSelect + authorsSelect
                + " FROM " + company + "." + COMMITS_TABLE
                + committerTableJoin
                + authorTableJoin
                + " ) b" + commitsWhere + regexPatternConditionForCommits : StringUtils.EMPTY;

        intermediateSQL = "SELECT id" + (StringUtils.isNotEmpty(selectDistinctString) ? ", " + selectDistinctString : "")
                + " FROM (SELECT " + " unnest(scm_pullrequests.repo_id) AS repo_ids, " + prsSelect
                + ", tags" + reviewsTableSelect + approversTableSelect + commentersSelect + creatorsSelect
                + " FROM " + company + "." + PRS_TABLE
                + creatorJoin
                + reviewerJoin
                + approverJoin
                + commenterJoin
                + commitsPRsJoin
                + tagsTableJoin
                + prsReviewedAtJoin
                + " ) a"
                + prsWhere
                + regexPatternCondition
                + commitsUnionStmt;

        String sql = "SELECT " + doraMetricCalculationSql + (StringUtils.isNotEmpty(outerSelectDistinctString) ? "," + outerSelectDistinctString : "") + " FROM ("
                + "SELECT " + resultCalculationComponent + (StringUtils.isNotEmpty(innerSelectDistinctString) ? "," + innerSelectDistinctString : "") + " FROM ("
                + "SELECT " + internalCalculationComponent + (StringUtils.isNotEmpty(selectDistinctString) ? "," + selectDistinctString : "")
                + " FROM (" + intermediateSQL + " ) x " + (StringUtils.isNotEmpty(groupByString) ? " GROUP BY "
                + groupByString : "") + ") y ) z";
        log.info("sql = {}", sql);
        log.info("params = {}", params);
        List<DbAggregationResult> results = template.query(sql, params, DbScmConverters.doraMetricsRowMapper(key, scmPrFilter.getAcross()));
        return DbListResponse.of(results, results.size());
    }

    public DbListResponse<DbAggregationResult> generateFailureRateReports(String company,
                                                                          ScmPrFilter scmPrFilter,
                                                                          ScmCommitFilter scmCommitFilter,
                                                                          OUConfiguration ouConfig,
                                                                          VelocityConfigDTO velocityConfigDTO) throws SQLException, BadRequestException {
        checkFilterConfigValidity(scmPrFilter, velocityConfigDTO);
        ScmPrFilter.DISTINCT DISTINCT = scmPrFilter.getAcross();
        Validate.notNull(DISTINCT, "Across cant be missing for groupby query.");
        ScmPrFilter.CALCULATION calculation = scmPrFilter.getCalculation();
        Map<String, Object> params = new HashMap<>();
        Map<String, List<String>> hotfix;
        Map<String, List<String>> release;
        String selectDistinctString = "";
        String groupByString = "";
        String outerSelectDistinctString = "";
        String innerSelectDistinctString = "";
        String intermediateSQL;
        String regexPatternCondition = StringUtils.EMPTY;
        String prsWhere = StringUtils.EMPTY;
        String commitsWhere = StringUtils.EMPTY;
        String doraMetricCalculationSql, internalCalculationComponentForPrs, internalCalculationCompForCommits, innerCalculation, resultCalculationComponent;
        Map<String, List<String>> prConditions;
        Map<String, List<String>> commitConditions;
        boolean needCommitsTable = checkCommitsTableFiltersJoin(scmPrFilter, calculation, false);
        boolean needCommentors = checkCommentDensityFiltersJoin(scmPrFilter, calculation, false);
        boolean needReviewedAt = checkPRReviewedJoin(scmPrFilter);
        boolean needCreators = checkCreatorsFiltersJoin(scmPrFilter, ouConfig);
        boolean needCommitters = checkCommitters(scmCommitFilter, ouConfig);
        boolean needAuthors = checkAuthors(scmCommitFilter, ouConfig);
        boolean needReviewers = checkReviewersFiltersJoin(scmPrFilter, calculation, false);
        boolean needApprovers = checkApproversFiltersJoin(scmPrFilter, calculation, false);
        prConditions = scmAggService.createPrWhereClauseAndUpdateParams(company, params, scmPrFilter, null, "", ouConfig);
        commitConditions = scmAggService.createCommitsWhereClauseAndUpdateParams(company, params, scmCommitFilter, null, "", false, ouConfig);
        boolean needCommitsUnionJoin = checkDirectMergeConfigForFailureRate(velocityConfigDTO);
        prConditions.get(PRS_TABLE).add("merged");
        commitConditions.get(COMMITS_TABLE).add("direct_merge");
        String key = "band";
        if (prConditions.get(PRS_TABLE).size() > 0) {
            prsWhere = " WHERE " + String.join(" AND ", prConditions.get(PRS_TABLE));
        }
        if (commitConditions.get(COMMITS_TABLE).size() > 0) {
            commitsWhere = " WHERE " + String.join(" AND ", commitConditions.get(COMMITS_TABLE));
        }

        doraMetricCalculationSql = ScmQueryUtils.getFailureRateSql()
                + ", CASE WHEN failure_rate IS NULL THEN 0 ELSE failure_rate END failure_rate, ct";
        resultCalculationComponent = "num_hfs * 100.0/ NULLIF(num_hfs + num_releases,0)::float AS failure_rate, (num_hfs + num_releases) as ct";
        hotfix = buildRegexMapWithProfile(MapUtils.emptyIfNull(velocityConfigDTO.getScmConfig().getHotfix()),
                params, "hotfix", HOTFIX_TABLE);
        release = buildRegexMapWithProfile(MapUtils.emptyIfNull(velocityConfigDTO.getScmConfig().getRelease()),
                params, "release", RELEASE_TABLE);
        String interHotfixStr = String.join(" OR ", hotfix.get(HOTFIX_TABLE));
        String intrReleaseStr = String.join(" OR ", release.get(RELEASE_TABLE));
        boolean isHFEmpty = StringUtils.isEmpty(interHotfixStr);
        boolean isReleaseEmpty = StringUtils.isEmpty(intrReleaseStr);
        internalCalculationComponentForPrs = "Count(DISTINCT id)             AS ct,\n" +
                (isHFEmpty ? "0" : "Count(*) filter (WHERE " + interHotfixStr + ")" ) + " AS num_hfs,\n" +
                (isReleaseEmpty ? "0" : "Count(*) filter (WHERE " + intrReleaseStr + ") " ) + "  AS num_releases";
        isHFEmpty = CollectionUtils.isEmpty(hotfix.get(DIRECT_MERGE_TABLE));
        isReleaseEmpty = CollectionUtils.isEmpty(release.get(DIRECT_MERGE_TABLE));
        internalCalculationCompForCommits = "Count(DISTINCT id)             AS ct,\n" +
                (isHFEmpty ? "0" : "Count(*) filter (WHERE  " + String.join(" OR ", hotfix.get(DIRECT_MERGE_TABLE)) + ")" ) +" AS num_hfs,\n" +
                (isReleaseEmpty ? "0" : "count(*) filter (WHERE " + String.join(" OR ", release.get(DIRECT_MERGE_TABLE)) + ")" )+" AS num_releases";
        innerCalculation = "sum(num_hfs) AS num_hfs, sum(num_releases) AS num_releases, sum(ct) AS ct";

        switch (DISTINCT) {
            case project:
                selectDistinctString = DISTINCT.toString();
                groupByString = DISTINCT.toString();
                outerSelectDistinctString = DISTINCT.toString();
                innerSelectDistinctString = outerSelectDistinctString;
                break;
            case repo_id:
                groupByString = "repo_ids";
                selectDistinctString = "repo_ids";
                outerSelectDistinctString = groupByString;
                innerSelectDistinctString = outerSelectDistinctString;
                break;
            case creator:
                needCreators = true;
                groupByString = "creator_id, " + scmPrFilter.getAcross().name();
                selectDistinctString = "creator_id, " + scmPrFilter.getAcross().name();
                outerSelectDistinctString = selectDistinctString;
                innerSelectDistinctString = outerSelectDistinctString;
                break;
            case none:
                groupByString = "";
                selectDistinctString = "";
                outerSelectDistinctString = "";
                innerSelectDistinctString = "";
                break;
        }
        String creatorJoin = needCreators ? ScmQueryUtils.sqlForCreatorTableJoin(company) : StringUtils.EMPTY;
        String reviewerJoin = needReviewers ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.REVIEWER) : StringUtils.EMPTY;
        String approverJoin = needApprovers ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.APPROVER) : StringUtils.EMPTY;
        String commenterJoin = needCommentors ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.COMMENTER) : StringUtils.EMPTY;
        String commitsPRsJoin = needCommitsTable ? getCommitsJoinForScmDora(company, scmPrFilter.getIntegrationIds()) : StringUtils.EMPTY;
        String prsReviewedAtJoin = needReviewedAt ? ScmQueryUtils.getSqlForPRReviewedAtJoin(company) : StringUtils.EMPTY;
        String tagsTableJoin = ScmQueryUtils.getTagsTableJoin(company, scmPrFilter.getIntegrationIds());
        String reviewsTableSelect = needReviewers ? ScmQueryUtils.REVIEW_PRS_SELECT_LIST : StringUtils.EMPTY;
        String approversTableSelect = needApprovers ? ScmQueryUtils.APPROVERS_PRS_LIST : StringUtils.EMPTY;
        String commentersSelect = needCommentors ? ScmQueryUtils.COMMENTERS_SELECT_SQL : StringUtils.EMPTY;
        String creatorsSelect = needCreators ? ScmQueryUtils.CREATORS_SQL : StringUtils.EMPTY;
        String committersSelect = needCommitters ? ScmQueryUtils.COMMITTERS_SELECT : StringUtils.EMPTY;
        String authorsSelect = needAuthors ? ScmQueryUtils.AUTHORS_SELECT : StringUtils.EMPTY;
        String authorTableJoin = needAuthors ? ScmQueryUtils.sqlForAuthorTableJoin(company) : StringUtils.EMPTY;
        String committerTableJoin = needCommitters ? ScmQueryUtils.sqlForCommitterTableJoin(company) : StringUtils.EMPTY;
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

        String commitsUnionStmt = needCommitsUnionJoin ? " UNION SELECT " + internalCalculationCompForCommits + (StringUtils.isNotEmpty(innerSelectDistinctString) ? "," + innerSelectDistinctString : "")
                + " FROM (SELECT unnest(scm_commits.repo_id) AS repo_ids, direct_merge, "
                + commitsSelect + committersSelect + authorsSelect
                + " FROM " + company + "." + COMMITS_TABLE
                + committerTableJoin
                + authorTableJoin
                + " ) b" + commitsWhere
                + (StringUtils.isNotEmpty(groupByString) ? " GROUP BY " + groupByString : "") : StringUtils.EMPTY;

        intermediateSQL = "SELECT " + internalCalculationComponentForPrs + (StringUtils.isNotEmpty(innerSelectDistinctString) ? "," + innerSelectDistinctString : "")
                + " FROM (SELECT " + " unnest(scm_pullrequests.repo_id) AS repo_ids, " + prsSelect
                + ", tags" + reviewsTableSelect + approversTableSelect + commentersSelect + creatorsSelect
                + " FROM " + company + "." + PRS_TABLE
                + creatorJoin
                + reviewerJoin
                + approverJoin
                + commenterJoin
                + commitsPRsJoin
                + tagsTableJoin
                + prsReviewedAtJoin
                + " ) a"
                + prsWhere
                + regexPatternCondition
                + (StringUtils.isNotEmpty(groupByString) ? " GROUP BY " + groupByString : "")
                + commitsUnionStmt;

        String sql = "SELECT " + doraMetricCalculationSql + (StringUtils.isNotEmpty(outerSelectDistinctString) ? "," + outerSelectDistinctString : "") + " FROM ("
                + "SELECT " + resultCalculationComponent + (StringUtils.isNotEmpty(innerSelectDistinctString) ? "," + innerSelectDistinctString : "") + " FROM ("
                + "SELECT " + innerCalculation + (StringUtils.isNotEmpty(selectDistinctString) ? "," + selectDistinctString : "")
                + " FROM (" + intermediateSQL + " ) x " + (StringUtils.isNotEmpty(groupByString) ? " GROUP BY " + groupByString : "") + ") y ) z";
        log.info("sql = {}", sql);
        log.info("params = {}", params);
        List<DbAggregationResult> results = template.query(sql, params, DbScmConverters.doraMetricsRowMapper(key, scmPrFilter.getAcross()));
        return DbListResponse.of(results, results.size());
    }

    private boolean checkDirectMergeConfigForFailureRate(VelocityConfigDTO velocityConfigDTO) {
        return  velocityConfigDTO.getScmConfig().getHotfix() != null
                && velocityConfigDTO.getScmConfig().getRelease() != null
                && velocityConfigDTO.getScmConfig().getHotfix().get(VelocityConfigDTO.ScmConfig.Field.commit_branch) != null
                && velocityConfigDTO.getScmConfig().getHotfix().get(VelocityConfigDTO.ScmConfig.Field.commit_branch).size() > 0
                && velocityConfigDTO.getScmConfig().getRelease().get(VelocityConfigDTO.ScmConfig.Field.commit_branch) != null
                && velocityConfigDTO.getScmConfig().getRelease().get(VelocityConfigDTO.ScmConfig.Field.commit_branch).size() > 0;
    }

    private boolean checkDirectMergeConfigForDeploymentFrequency(VelocityConfigDTO velocityConfigDTO) {
        return velocityConfigDTO.getScmConfig().getDeployment() != null &&
                velocityConfigDTO.getScmConfig().getDeployment().get(VelocityConfigDTO.ScmConfig.Field.commit_branch) != null &&
                velocityConfigDTO.getScmConfig().getDeployment().get(VelocityConfigDTO.ScmConfig.Field.commit_branch).size() > 0;
    }

    public DbListResponse<DbAggregationResult> generateLeadTimeAndMTTRReport(String company, ScmPrFilter filter,
                                                                             OUConfiguration ouConfig,
                                                                             VelocityConfigDTO velocityConfigDTO) throws SQLException, BadRequestException {
        checkFilterConfigValidity(filter, velocityConfigDTO);
        ScmPrFilter.DISTINCT DISTINCT = filter.getAcross();
        Validate.notNull(DISTINCT, "Across cant be missing for groupby query.");
        Map<String, List<String>> conditions;
        Map<String, List<String>> defect;
        Map<String, List<String>> release;
        String regexPatternCondition;
        String intrDefCond;
        String prsWhere = StringUtils.EMPTY;
        String calculationComponent;
        String doraMetricString;
        String key;
        String selectDistinctString = "";
        String outerSelectDistinctString = "";
        String groupByString = "";
        String orderByString = "";
        ScmPrFilter.CALCULATION calculation = filter.getCalculation();
        Map<String, Object> params = new HashMap<>();
        conditions = scmAggService.createPrWhereClauseAndUpdateParams(company, params, filter, null, StringUtils.EMPTY, ouConfig);
        boolean needCommentors = checkCommentDensityFiltersJoin(filter, calculation, false);
        boolean needReviewers = checkReviewersFiltersJoin(filter, calculation, false);
        boolean needApprovers = checkApproversFiltersJoin(filter, calculation, false);
        String creatorJoin = ScmQueryUtils.sqlForCreatorTableJoin(company);
        String reviewerJoin = needReviewers ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.REVIEWER) : StringUtils.EMPTY;
        String approverJoin = needApprovers ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.APPROVER) : StringUtils.EMPTY;
        String commenterJoin = needCommentors ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.COMMENTER) : StringUtils.EMPTY;
        String tagsTableJoin = ScmQueryUtils.getTagsTableJoin(company, filter.getIntegrationIds());
        String commitsPRsJoin = getCommitsJoinForScmDora(company, filter.getIntegrationIds());
        String reviewsTableSelect = needReviewers ? ScmQueryUtils.REVIEW_PRS_SELECT_LIST : StringUtils.EMPTY;
        String approversTableSelect = needApprovers ? ScmQueryUtils.APPROVERS_PRS_LIST : StringUtils.EMPTY;
        String commentersSelect = needCommentors ? ScmQueryUtils.COMMENTERS_SELECT_SQL : StringUtils.EMPTY;
        String prsSelect = ScmQueryUtils.PRS_SELECT;
        String creatorsSelect = ScmQueryUtils.CREATORS_SQL;
        String leadTimeSelect = ",GREATEST(EXTRACT(EPOCH FROM (pr_merged_at - first_commit_time)), 0) AS lead_time";
        boolean isLeadTimeMetric = calculation.equals(ScmPrFilter.CALCULATION.lead_time_for_changes);
        conditions.get(PRS_TABLE).add("merged");
        switch (calculation) {
            case mean_time_to_recover:
                calculationComponent = " avg (lead_time) as lead_time_avg,\n" + " count(DISTINCT pr_id) as count_of_prs ";
                key = "mean_time_to_resolve_metric";
                defect = buildRegexMapWithProfile(MapUtils.emptyIfNull(velocityConfigDTO.getScmConfig().getDefect()), params,
                        "defect", DEFECT_TABLE);
                intrDefCond = defect.get(DEFECT_TABLE) != null &&
                        defect.get(DEFECT_TABLE).size() > 0 ? String.join(" OR ", defect.get(DEFECT_TABLE)) :
                        StringUtils.EMPTY;
                regexPatternCondition = StringUtils.isNotEmpty(intrDefCond) ? " AND (" + intrDefCond + ")" : StringUtils.EMPTY;
                break;
            case lead_time_for_changes:
                calculationComponent = " avg (lead_time) as lead_time_avg,\n" + " count(DISTINCT pr_id) as count_of_prs ";
                key = "lead_time_for_changes_metric";
                release = buildRegexMapWithProfile(MapUtils.emptyIfNull(velocityConfigDTO.getScmConfig().getRelease()), params,
                        "release", RELEASE_TABLE);
                intrDefCond = release.get(RELEASE_TABLE) != null &&
                        release.get(RELEASE_TABLE).size() > 0 ? String.join(" OR ", release.get(RELEASE_TABLE)) :
                        StringUtils.EMPTY;
                regexPatternCondition = StringUtils.isNotEmpty(intrDefCond) ? " AND (" + intrDefCond + ")" : StringUtils.EMPTY;
                break;
            default:
                throw new SQLException("Invalid calculation field provided for this agg.");
        }
        switch (DISTINCT) {
            case project:
                selectDistinctString = DISTINCT.toString();
                groupByString = DISTINCT.toString();
                orderByString = DISTINCT.toString();
                outerSelectDistinctString = DISTINCT.toString();
                break;
            case repo_id:
                groupByString = "repo_ids";
                selectDistinctString = "repo_ids";
                orderByString = "repo_ids";
                outerSelectDistinctString = groupByString;
                break;
            case assignee:
                groupByString = "assignee_id, " + filter.getAcross().name();
                selectDistinctString = "UNNEST(assignee_ids) AS assignee_id, UNNEST(" + filter.getAcross() + "s) AS " + filter.getAcross();
                outerSelectDistinctString = "assignee_id, assignee";
                break;
            case creator:
                groupByString = "creator_id, " + filter.getAcross().name();
                selectDistinctString = "creator_id, " + filter.getAcross().name();
                outerSelectDistinctString = selectDistinctString;
                break;
            case none:
                groupByString = "";
                selectDistinctString = "";
                outerSelectDistinctString = "";
                break;
        }
        if (conditions.get(PRS_TABLE).size() > 0) {
            prsWhere = " WHERE " + String.join(" AND ", conditions.get(PRS_TABLE));
        }
        List<DbAggregationResult> results;
        doraMetricString = " CASE " +
                "         WHEN lead_time_avg IS NULL THEN 'ELITE' "+
                "         WHEN lead_time_avg <=  " + (isLeadTimeMetric ? "86400" : "3600") + " THEN 'ELITE'\n" +
                "         WHEN " + (isLeadTimeMetric ? "lead_time_avg > 86400 AND lead_time_avg <= 604800" : "lead_time_avg <= 86400") + "  THEN 'HIGH'\n" +
                "         WHEN " + (isLeadTimeMetric ? "lead_time_avg > 604800 AND lead_time_avg <= 2419200" : "lead_time_avg <= 604800") + " THEN 'MEDIUM' \n" +
                "         WHEN lead_time_avg >  " + (isLeadTimeMetric ? "2419200" : "604800") + " THEN 'LOW'\n" +
                "    END  " + key + ", " +
                " CASE WHEN lead_time_avg IS NULL THEN 0 ELSE lead_time_avg END " + (isLeadTimeMetric ? "lead_time" : "recover_time") + "," +
                " count_of_prs as ct " + (StringUtils.isNotEmpty(outerSelectDistinctString) ? "," +
                outerSelectDistinctString : "");
        String sql = "SELECT " + doraMetricString + " FROM ( SELECT " + calculationComponent +
                (StringUtils.isNotEmpty(selectDistinctString) ? "," + selectDistinctString : "")
                + "  FROM (SELECT unnest(scm_pullrequests.repo_id) AS repo_ids, " +
                prsSelect + creatorsSelect + ", tags"
                + reviewsTableSelect + approversTableSelect + commentersSelect + leadTimeSelect
                + " FROM " + company + "." + PRS_TABLE
                + creatorJoin
                + reviewerJoin
                + approverJoin
                + commenterJoin
                + commitsPRsJoin
                + tagsTableJoin
                + " ) a" + prsWhere + regexPatternCondition + (StringUtils.isNotEmpty(groupByString) ? " GROUP BY "
                + groupByString : "") + " ORDER BY " + (StringUtils.isEmpty(orderByString) ? "lead_time_avg" : orderByString) + ") y ";
        log.info("sql = {}", sql);
        log.info("params = {}", params);
        results = template.query(sql, params, DbScmConverters.doraMetricsRowMapper(key, filter.getAcross()));
        return DbListResponse.of(results, results.size());
    }

    @NotNull
    private Map<String, List<String>> buildRegexMapWithProfile(Map<VelocityConfigDTO.ScmConfig.Field, Map<String, List<String>>> velocityConfigDTO,
                                                               Map<String, Object> params,
                                                               String suffix,
                                                               String tableName) {
        List<String> doraConditions = new ArrayList<>();
        if (velocityConfigDTO.size() == 0)
            return Map.of();
        return ScmConditionBuilder.buildRegexPatternMap(params, velocityConfigDTO, doraConditions, tableName, suffix);
    }
}
