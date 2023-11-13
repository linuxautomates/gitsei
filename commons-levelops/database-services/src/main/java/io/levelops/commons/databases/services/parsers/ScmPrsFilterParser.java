package io.levelops.commons.databases.services.parsers;

import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.services.ScmQueryUtils;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.services.ScmQueryUtils.PRS_APPROVER_COUNT;
import static io.levelops.commons.databases.services.ScmQueryUtils.getCodeChangeSql;
import static io.levelops.commons.databases.services.ScmQueryUtils.getFilesChangeSql;
import static io.levelops.commons.databases.services.parsers.ScmFilterUtils.getListOrDefault;

public class ScmPrsFilterParser {

    private static final String REVIEWS_TABLE = "scm_pullrequest_reviews";
    private static final String PRS_TABLE = "scm_pullrequests";

    public ScmPrFilter merge(Integer integrationId, ScmPrFilter reqFilter, Map<String, Object> productFilter) {
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) productFilter.get("partial_match"));
        Map<String, Object> excludedFields = (Map<String, Object>) productFilter.getOrDefault("exclude", Map.of());
        return ScmPrFilter.builder()
                .calculation(reqFilter.getCalculation())
                .across(reqFilter.getAcross())
                .prCreatedRange(reqFilter.getPrCreatedRange())
                .prMergedRange(reqFilter.getPrMergedRange())
                .labels(getListOrDefault(productFilter, "labels"))
                .states(getListOrDefault(productFilter, "states"))
                .repoIds(getListOrDefault(productFilter, "repo_ids"))
                .sourceBranches(getListOrDefault(productFilter, "source_branches"))
                .targetBranches(getListOrDefault(productFilter, "target_branches"))
                .projects(getListOrDefault(productFilter, "projects"))
                .creators(getListOrDefault(productFilter, "creators"))
                .reviewers(getListOrDefault(productFilter, "reviewers"))
                .assignees(getListOrDefault(productFilter, "assignees"))
                .excludeLabels(getListOrDefault(excludedFields, "labels"))
                .excludeStates(getListOrDefault(excludedFields, "states"))
                .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                .excludeSourceBranches(getListOrDefault(excludedFields, "source_branches"))
                .excludeTargetBranches(getListOrDefault(excludedFields, "target_branches"))
                .excludeProjects(getListOrDefault(excludedFields, "projects"))
                .excludeCreators(getListOrDefault(excludedFields, "creators"))
                .excludeReviewers(getListOrDefault(excludedFields, "reviewers"))
                .excludeAssignees(getListOrDefault(excludedFields, "assignees"))
                .integrationIds(List.of(String.valueOf(integrationId)))
                .partialMatch(partialMatchMap)
                .build();
    }

    public String getSqlStmt(String company, Map<String, List<String>> conditions, ScmPrFilter scmPrFilter) {
        String reviewsWhere = "";
        String prsWhere = "";
        String reviewJoin = "";
        boolean needReviewStuff = CollectionUtils.isNotEmpty(scmPrFilter.getReviewers())
                || CollectionUtils.isNotEmpty(scmPrFilter.getExcludeReviewers());
        if (conditions.get(REVIEWS_TABLE).size() > 0)
            reviewsWhere = " WHERE " + String.join(" AND ", conditions.get(REVIEWS_TABLE));
        if (conditions.get(PRS_TABLE).size() > 0)
            prsWhere = " WHERE " + String.join(" AND ", conditions.get(PRS_TABLE));
        if (needReviewStuff) {
            reviewJoin = " INNER JOIN ( SELECT reviewer,reviewed_at,pr_id FROM "
                    + company + "." + REVIEWS_TABLE + reviewsWhere
                    + " ) AS reviews ON prs.id = reviews.pr_id ";
        }
        String prsSelect = ScmQueryUtils.PRS_SELECT;
        String creatorsSelect = ScmQueryUtils.CREATORS_SQL;
        String creatorJoin = ScmQueryUtils.sqlForCreatorTableJoin(company);
        return "SELECT * FROM ( SELECT " + prsSelect + creatorsSelect + " FROM "
                + company + "." + PRS_TABLE
                + (needReviewStuff ? reviewJoin : "")
                + creatorJoin
                + (needReviewStuff ? " GROUP BY scm_pullrequests.id,pr_creators.display_name,pr_creators.id" : "")
                + " ) a " + prsWhere;
    }

    public String getSqlStmtForGroupByDuration(String company, Map<String, List<String>> conditions, ScmPrFilter scmPrFilter,
                                               int paramSuffix) {
        String prsWhere = "";
        ScmPrFilter.CALCULATION calculation;
        calculation = scmPrFilter.getCalculation() == null ? ScmPrFilter.CALCULATION.merge_time : scmPrFilter.getCalculation();
        String intervalColumn = "";
        AggTimeQueryHelper.AggTimeQuery prsModAggQuery;
        AGG_INTERVAL aggInterval = scmPrFilter.getAggInterval() == null ? AGG_INTERVAL.day : scmPrFilter.getAggInterval();
        switch (calculation) {
            case merge_time:
            case first_review_to_merge_time:
                conditions.get(PRS_TABLE).add("merged");
                break;
            case first_review_time:
                break;
            default:
        }
        ScmPrFilter.DISTINCT DISTINCT = scmPrFilter.getAcross();
        switch (DISTINCT) {
            case pr_merged:
                conditions.get(PRS_TABLE).add("merged");
            case pr_created:
            case pr_updated:
                prsModAggQuery = AggTimeQueryHelper.getAggTimeQuery
                        (DISTINCT + "_at", DISTINCT.toString(), aggInterval.toString(), false);
                intervalColumn = prsModAggQuery.getHelperColumn().replaceFirst(",", "") + ",";
            default:
        }
        if (conditions.get(PRS_TABLE).size() > 0)
            prsWhere = " WHERE " + String.join(" AND ", conditions.get(PRS_TABLE));
        if (CollectionUtils.isNotEmpty(scmPrFilter.getRepoIds())) {
            if (prsWhere.equals(""))
                prsWhere = " WHERE repo_ids IN (:repo_ids_" + paramSuffix + ") ";
            else
                prsWhere = prsWhere + " AND  repo_ids IN (:repo_ids_" + paramSuffix + ") ";
        }
        String creatorJoin = ScmQueryUtils.sqlForCreatorTableJoin(company);
        String prsSelect = ScmQueryUtils.PRS_SELECT;
        String creatorsSelect = ScmQueryUtils.CREATORS_SQL;
        String reviewerJoin = ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.REVIEWER);
        String approverJoin  =  ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.APPROVER);
        String commenterJoin = ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.COMMENTER);
        String commitsPRsJoin = ScmQueryUtils.getCommitsPRsJoin(company, Map.of(), ScmPrFilter.builder().build(), "", "");
        String prJiraMappingJoin  = ScmQueryUtils.getSqlForPRJiraMappingTable(company);
        String prWorkItemIdsMappingJoin = ScmQueryUtils.getSqlForPRWorkItemMappingTable(company);
        String prsReviewedAtJoin = ScmQueryUtils.getSqlForPRReviewedAtJoin(company);
        String approvalStatusSelect = ScmQueryUtils.getApprovalStatusSqlStmt();
        String codeChangeSql = getCodeChangeSql(scmPrFilter.getCodeChangeSizeConfig(), true,scmPrFilter.getCodeChangeUnit());
        String fileChangeSql = getFilesChangeSql(scmPrFilter.getCodeChangeSizeConfig());
        String commentDensitySql = ScmQueryUtils.getCommentDensitySql(scmPrFilter);
        String issueKeysSql = ScmQueryUtils.getIssueKeysSql();
        if (calculation == ScmPrFilter.CALCULATION.first_review_time) {
            return "SELECT * FROM ( SELECT " + intervalColumn + " unnest(repo_id) AS repo_ids," + prsSelect + creatorsSelect
                    + approvalStatusSelect + codeChangeSql + fileChangeSql + commentDensitySql + issueKeysSql + ScmQueryUtils.RESOLUTION_TIME_SQL
                    + ScmQueryUtils.REVIEW_PRS_SELECT_LIST + ScmQueryUtils.COMMITS_PRS_SELECT + ScmQueryUtils.getReviewType() + ScmQueryUtils.JIRA_WORKITEM_PRS_MAPPING_SELECT +
                    ScmQueryUtils.PRS_REVIEWER_COUNT + PRS_APPROVER_COUNT
                    + " ,EXTRACT(EPOCH FROM (reviews.pr_reviewed_at - scm_pullrequests.pr_created_at)) as calc FROM  "
                    + company + "." + PRS_TABLE + " INNER JOIN ( SELECT MIN(reviewed_at) as pr_reviewed_at,pr_id FROM "
                    + company + "." + REVIEWS_TABLE  + " GROUP BY pr_id ) AS reviews ON scm_pullrequests.id = reviews.pr_id"
                    + creatorJoin + reviewerJoin + approverJoin + commenterJoin + commitsPRsJoin + prJiraMappingJoin + prWorkItemIdsMappingJoin
                    + " ) a" + prsWhere;
        }
        if (calculation == ScmPrFilter.CALCULATION.first_review_to_merge_time) {
            return "SELECT * FROM (SELECT " + intervalColumn + " unnest(repo_id) AS repo_ids," + prsSelect + creatorsSelect
                    + approvalStatusSelect + codeChangeSql + fileChangeSql + commentDensitySql + issueKeysSql + ScmQueryUtils.RESOLUTION_TIME_SQL
                    + ScmQueryUtils.REVIEW_PRS_SELECT_LIST + ScmQueryUtils.COMMITS_PRS_SELECT + ScmQueryUtils.getReviewType() + ScmQueryUtils.JIRA_WORKITEM_PRS_MAPPING_SELECT +
                    ScmQueryUtils.PRS_REVIEWER_COUNT + PRS_APPROVER_COUNT
                    + " ,EXTRACT(EPOCH FROM (scm_pullrequests.pr_merged_at - reviews.pr_reviewed_at)) as calc FROM "
                    + company + "." + PRS_TABLE + " INNER JOIN ( SELECT MIN(reviewed_at) as pr_reviewed_at,pr_id FROM "
                    + company + "." + REVIEWS_TABLE  + " GROUP BY pr_id ) AS reviews ON scm_pullrequests.id = reviews.pr_id"
                    + creatorJoin + reviewerJoin + approverJoin + commenterJoin + commitsPRsJoin + prJiraMappingJoin + prWorkItemIdsMappingJoin
                    + " ) a" + prsWhere;
        }
        return "SELECT * FROM ( SELECT " + intervalColumn + " unnest(repo_id) AS repo_ids, " + prsSelect + creatorsSelect
                + approvalStatusSelect + codeChangeSql + fileChangeSql + commentDensitySql + issueKeysSql + ScmQueryUtils.RESOLUTION_TIME_SQL
                + ScmQueryUtils.REVIEW_PRS_SELECT_LIST + ScmQueryUtils.COMMITS_PRS_SELECT + ScmQueryUtils.getReviewType() + ScmQueryUtils.JIRA_WORKITEM_PRS_MAPPING_SELECT +
                ScmQueryUtils.PRS_REVIEWER_COUNT + PRS_APPROVER_COUNT
                + " ,EXTRACT(EPOCH FROM (pr_merged_at - pr_created_at)) as calc"
                + " FROM " + company + "." + PRS_TABLE
                + creatorJoin + reviewerJoin + approverJoin + commenterJoin + commitsPRsJoin + prJiraMappingJoin + prWorkItemIdsMappingJoin
                + prsReviewedAtJoin
                + " ) a" + prsWhere;

    }

    public String getSqlStmtForGroupByCount(String company, Map<String, List<String>> conditions, ScmPrFilter scmPrFilter,
                                            boolean isAcrossTechnology, int paramPrefixVar) {
        String prsWhere = "";
        String intervalColumn = "";
        String creatorJoin = ScmQueryUtils.sqlForCreatorTableJoin(company);
        String prsSelect = ScmQueryUtils.PRS_SELECT;
        String creatorsSelect = ScmQueryUtils.CREATORS_SQL;
        String reviewerJoin = ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.REVIEWER);
        String approverJoin  =  ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.APPROVER);
        String commenterJoin = ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.COMMENTER);
        String commitsPRsJoin = ScmQueryUtils.getCommitsPRsJoin(company, Map.of(), ScmPrFilter.builder().build(), "", "");
        String prJiraMappingJoin  = ScmQueryUtils.getSqlForPRJiraMappingTable(company);
        String prWorkItemIdsMappingJoin = ScmQueryUtils.getSqlForPRWorkItemMappingTable(company);
        String prsReviewedAtJoin = ScmQueryUtils.getSqlForPRReviewedAtJoin(company);
        String approvalStatusSelect = ScmQueryUtils.getApprovalStatusSqlStmt();
        String codeChangeSql = ScmQueryUtils.getCodeChangeSql(scmPrFilter.getCodeChangeSizeConfig(), true,scmPrFilter.getCodeChangeUnit());
        String fileChangeSql = ScmQueryUtils.getFilesChangeSql(scmPrFilter.getCodeChangeSizeConfig());
        String commentDensitySql =  ScmQueryUtils.getCommentDensitySql(scmPrFilter);
        String issueKeysSql = ScmQueryUtils.getIssueKeysSql();
        String reviewsTableSelect = ScmQueryUtils.REVIEW_PRS_SELECT_LIST ;
        String commentorsSelect = ScmQueryUtils.COMMENTERS_SELECT_SQL;
        String commitsTableSelect = ScmQueryUtils.COMMITS_PRS_SELECT;
        String jiraWorkItemSelect = ScmQueryUtils.JIRA_WORKITEM_PRS_MAPPING_SELECT;
        String prReviewerCountSelect = ScmQueryUtils.PRS_REVIEWER_COUNT;
        String prApproverCountSelect = PRS_APPROVER_COUNT;
        AggTimeQueryHelper.AggTimeQuery prsModAggQuery;
        AGG_INTERVAL aggInterval = scmPrFilter.getAggInterval() == null ? AGG_INTERVAL.day : scmPrFilter.getAggInterval();
        switch (scmPrFilter.getAcross()) {
            case pr_merged:
                conditions.get(PRS_TABLE).add("merged = true");
            case pr_updated:
            case pr_created:
                prsModAggQuery = AggTimeQueryHelper.getAggTimeQuery
                        (scmPrFilter.getAcross() + "_at", scmPrFilter.getAcross().toString(), aggInterval.toString()
                                , false);
                intervalColumn = prsModAggQuery.getHelperColumn().replaceFirst(",", "") + ",";
                break;
            default:
        }
        if (conditions.get(PRS_TABLE).size() > 0)
            prsWhere = " WHERE " + String.join(" AND ", conditions.get(PRS_TABLE));
        if (CollectionUtils.isNotEmpty(scmPrFilter.getRepoIds())) {
            if (prsWhere.equals(""))
                prsWhere = " WHERE repo_ids IN (:repo_ids_" + paramPrefixVar + ") ";
            else
                prsWhere = prsWhere + " AND  repo_ids IN (:repo_ids_" + paramPrefixVar + ") ";
        }
        if (!isAcrossTechnology)
            return "SELECT *  FROM (SELECT " + intervalColumn + " unnest(scm_pullrequests.repo_id) AS repo_ids, " + prsSelect + creatorsSelect
                    + commitsTableSelect + reviewsTableSelect + commentorsSelect + prReviewerCountSelect + prApproverCountSelect + jiraWorkItemSelect
                    + approvalStatusSelect + codeChangeSql + fileChangeSql + commentDensitySql + issueKeysSql
                    + " FROM " + company + "." + PRS_TABLE
                    + creatorJoin
                    + reviewerJoin
                    + approverJoin
                    + commenterJoin
                    + commitsPRsJoin
                    + prJiraMappingJoin
                    + prWorkItemIdsMappingJoin
                    + prsReviewedAtJoin
                    + " ) a" + prsWhere;
        return "SELECT * FROM ( SELECT * FROM ( SELECT unnest(scm_pullrequests.repo_id) AS repo_ids,"
                + prsSelect + creatorsSelect  + commitsTableSelect + reviewsTableSelect + prReviewerCountSelect + jiraWorkItemSelect +
                approvalStatusSelect +
                " FROM " + company + "." + PRS_TABLE
                + creatorJoin
                + reviewerJoin
                + approverJoin
                + commenterJoin
                + commitsPRsJoin
                + prJiraMappingJoin
                + prWorkItemIdsMappingJoin
                + prsReviewedAtJoin
                + " ) pr INNER JOIN ("
                + " SELECT name as technology,repo_id as tr_id,integration_id as ti_id FROM "
                + company + ".gittechnologies ) x ON x.tr_id = pr.repo_ids AND pr.integration_id = x.ti_id"
                + " ) a" + prsWhere;
    }
}
