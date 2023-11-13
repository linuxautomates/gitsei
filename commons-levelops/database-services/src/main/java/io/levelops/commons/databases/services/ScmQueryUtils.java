package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmContributorsFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.filters.ScmReposFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.utils.CommonUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.ingestion.models.IntegrationType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.utils.AggTimeQueryHelper.getTimeRangeForStacks;


public class ScmQueryUtils {

    private static final String USERS_TABLE = "integration_users";
    private static final String COMMIT_WORKITEM_TABLE = "scm_commit_workitem_mappings";
    private static final String PULLREQUESTS_WORKITEM_TABLE = "scm_pullrequests_workitem_mappings";
    private static final String PULLREQUESTS_JIRA_TABLE = "scm_pullrequests_jira_mappings";
    private static final String COMMIT_JIRA_TABLE = "scm_commit_jira_mappings";
    private static final List<String> COMMENTED_STATES = List.of("'commented'","'i would prefer this is not merged as is'","'this shall not be merged'","'looks good to me, but someone else must approve'","'no score'","'no vote'","'changes_requested'");
    private static final List<String> APPROVED_STATES = List.of("'approved'","'looks good to me, approved'","'approved with suggestions'");
    public static List<String> getMissingFieldsClause(Map<ScmPrFilter.MISSING_BUILTIN_FIELD, Boolean> missingBuiltinFields,
                                                            Map<String, Object> params) {
        List<String> missingFieldConditions = new ArrayList<>();
        if (MapUtils.isNotEmpty(missingBuiltinFields)) {
            missingFieldConditions.addAll(missingBuiltinFields.entrySet().stream()
                    .map(missingBuiltinField -> {
                        String clause;
                        final boolean shouldBeMissing = Boolean.TRUE.equals(missingBuiltinField.getValue());
                        switch (missingBuiltinField.getKey()) {
                            case pr_merged:
                                clause = (shouldBeMissing ? " merged = false " : " merged ");
                                break;
                            case pr_closed:
                                clause = " pr_closed_at IS " + (shouldBeMissing ? "NULL " : "NOT NULL ");
                                break;
                            default:
                                return null;
                        }
                        return clause;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        }
        return missingFieldConditions;
    }

    public enum REVIEWS_TABLE_TYPE {
        APPROVER, COMMENTER, REVIEWER
    }

    public enum REVIEW_TYPE {
        SELF_REVIEWED, PEER_REVIEWED, NOT_REVIEWED
    }

    public static final String ARRAY_UNIQ = "CREATE OR REPLACE FUNCTION anyarray_uniq(with_array anyarray)\n" +
            "\tRETURNS anyarray AS\n" +
            "$BODY$\n" +
            "\tDECLARE\n" +
            "\t\t-- The variable used to track iteration over \"with_array\".\n" +
            "\t\tloop_offset integer;\n" +
            "\n" +
            "\t\t-- The array to be returned by this function.\n" +
            "\t\treturn_array with_array%TYPE := '{}';\n" +
            "\tBEGIN\n" +
            "\t\tIF with_array IS NULL THEN\n" +
            "\t\t\treturn NULL;\n" +
            "\t\tEND IF;\n" +
            "\t\t\n" +
            "\t\tIF with_array = '{}' THEN\n" +
            "\t\t    return return_array;\n" +
            "\t\tEND IF;\n" +
            "\n" +
            "\t\t-- Iterate over each element in \"concat_array\".\n" +
            "\t\tFOR loop_offset IN ARRAY_LOWER(with_array, 1)..ARRAY_UPPER(with_array, 1) LOOP\n" +
            "\t\t\tIF with_array[loop_offset] IS NULL THEN\n" +
            "\t\t\t\tIF NOT EXISTS(\n" +
            "\t\t\t\t\tSELECT 1 \n" +
            "\t\t\t\t\tFROM UNNEST(return_array) AS s(a)\n" +
            "\t\t\t\t\tWHERE a IS NULL\n" +
            "\t\t\t\t) THEN\n" +
            "\t\t\t\t\treturn_array = ARRAY_APPEND(return_array, with_array[loop_offset]);\n" +
            "\t\t\t\tEND IF;\n" +
            "\t\t\t-- When an array contains a NULL value, ANY() returns NULL instead of FALSE...\n" +
            "\t\t\tELSEIF NOT(with_array[loop_offset] = ANY(return_array)) OR NOT(NULL IS DISTINCT FROM (with_array[loop_offset] = ANY(return_array))) THEN\n" +
            "\t\t\t\treturn_array = ARRAY_APPEND(return_array, with_array[loop_offset]);\n" +
            "\t\t\tEND IF;\n" +
            "\t\tEND LOOP;\n" +
            "\n" +
            "\tRETURN return_array;\n" +
            " END;\n" +
            "$BODY$ LANGUAGE plpgsql;";


    public static final String SCM_COMMITS_SELECT = " scm_commits.id, scm_commits.repo_id, scm_commits.vcs_type, scm_commits.project, scm_commits.integration_id," +
            " scm_commits.commit_branch, scm_commits.commit_sha, scm_commits.commit_url, scm_commits.message, scm_commits.files_ct, scm_commits.commit_pushed_at," +
            " additions, deletions, changes, coalesce(additions, 0) + coalesce(changes, 0) as loc," +
            " scm_commits.created_at, scm_commits.ingested_at, committed_at, file_types ";

    public static final String SCM_COMMIT_OUTER_SELECT = "id, commit_sha, creator, message, created_at, committed_at, commit_pushed_at, commit_branch";

    public static final String AUTHORS_SELECT =" ,COALESCE(commit_authors.display_name, author) as author," +
            " COALESCE(commit_authors.id::varchar, author_id::varchar) as author_id " ;

    public static final String COMMITTERS_SELECT = " ,COALESCE(commit_committers.display_name, committer) as committer," +
            " COALESCE(commit_committers.id::varchar, committer_id::varchar) as committer_id ";

    public static final String COMMITS_SELECT = SCM_COMMITS_SELECT + AUTHORS_SELECT + COMMITTERS_SELECT;

    public static final String PRS_SELECT =
            " scm_pullrequests.id, scm_pullrequests.id as pr_id, scm_pullrequests.repo_id," +
                    " scm_pullrequests.project, scm_pullrequests.integration_id," +
                    " scm_pullrequests.state, scm_pullrequests.number, scm_pullrequests.merge_sha, scm_pullrequests.source_branch," +
                    " scm_pullrequests.target_branch, scm_pullrequests.metadata->>'pr_link' as pr_link, merged," +
                    " scm_pullrequests.title, CASE WHEN array_length(scm_pullrequests.assignees, 1) > 0 THEN anyarray_uniq(" +
                    " scm_pullrequests.assignees) ELSE '{NONE}' END assignees, CASE WHEN array_length(scm_pullrequests.assignee_ids::varchar[], 1) > 0 THEN anyarray_uniq(" +
                    " scm_pullrequests.assignee_ids::varchar[]) ELSE '{NONE}' END assignee_ids ,scm_pullrequests.labels, scm_pullrequests.commit_shas, " +
                    " scm_pullrequests.pr_updated_at, scm_pullrequests.pr_merged_at, scm_pullrequests.pr_created_at, scm_pullrequests.pr_closed_at," +
                    " scm_pullrequests.created_at, scm_pullrequests.author_response_time, scm_pullrequests.reviewer_response_time ";

    public static final String CREATORS_SQL = ",COALESCE(pr_creators.display_name, creator) as creator," +
            " COALESCE(pr_creators.id::varchar, creator_id::varchar) as creator_id";

    public static final String COMMITS_PRS_SELECT =
            "  ,coalesce(lines_added, 0) as lines_added,\n" +
                    "  coalesce(lines_deleted, 0) as lines_deleted," +
                    " coalesce(lines_changed, 0) as lines_changed," +
                    " coalesce(lines_added, 0) + coalesce(lines_changed, 0) as loc," +
                    "  files_ct ";

    public static final String PRS_REVIEWER_COUNT =
            " ,COALESCE(array_length(anyarray_uniq(scm_pullrequests.assignees || pr_reviewers.reviewers),1),0) AS reviewer_count";

    public static final String PRS_APPROVER_COUNT = " ,COALESCE(array_length(anyarray_uniq(pr_approvers.approvers),1),0)   AS approver_count";

    public static final String REVIEW_PRS_SELECT_LIST = ",CASE WHEN array_length(pr_reviewers.reviewers || scm_pullrequests.assignees, 1)\n" +
            "> 0 THEN anyarray_uniq(pr_reviewers.reviewers || scm_pullrequests.assignees)\n" +
            "ELSE '{NONE}' END reviewer" +
            ", CASE WHEN array_length(pr_reviewers.reviewer_ids::varchar[] || scm_pullrequests.assignee_ids::varchar[], 1)" +
            "> 0 THEN anyarray_uniq(pr_reviewers.reviewer_ids::varchar[] || scm_pullrequests.assignee_ids::varchar[]) ELSE '{NONE}' END reviewer_id ";

    public static final String APPROVERS_PRS_LIST = " ,COALESCE(pr_approvers.approvers,'{NONE}')  AS approver," +
            " COALESCE(pr_approvers.approver_ids::varchar[], pr_approvers.approvers, '{NONE}') as approver_id";

    public static final String COMMENTERS_SELECT_SQL = ",pr_commenters.commenters  AS commenter, COALESCE(pr_commenters.commenter_ids::varchar[], '{NONE}')   AS commenter_id";

    public static final String RESOLUTION_TIME_SQL = ",EXTRACT(EPOCH FROM (pr_merged_at - pr_created_at)) as cycle_time";

    public static final String PR_APPROVE_TIME_SQL = ",EXTRACT(EPOCH FROM (pr_approvers.pr_approved_at - pr_created_at)) as reviewer_approve_time";

    public static final String PR_COMMENT_TIME_SQL = ",EXTRACT(EPOCH FROM (pr_commenters.pr_commented_at - pr_created_at)) as reviewer_comment_time";

    public static final String JIRA_WORKITEM_PRS_MAPPING_SELECT = ",issue_keys, workitem_ids";

    public static final String PRS_REVIEWED_AT_SELECT = ",reviews.pr_reviewed_at";

    public static String sqlForAuthorTableJoin(String company) {
        return " LEFT JOIN " + company + "." + USERS_TABLE + " commit_authors ON" +
                " commit_authors.id = scm_commits.author_id and" +
                " commit_authors.integration_id = scm_commits.integration_id ";
    }

    public static String sqlForCommitterTableJoin(String company) {
        return " LEFT JOIN " + company + "." + USERS_TABLE + " commit_committers ON " +
                " commit_committers.id = scm_commits.committer_id and" +
                " commit_committers.integration_id = scm_commits.integration_id ";
    }

    public static String sqlForCreatorTableJoin(String company) {
        return " LEFT JOIN " + company + "." + USERS_TABLE + " pr_creators " +
                " ON pr_creators.id = scm_pullrequests.creator_id and" +
                " pr_creators.integration_id = scm_pullrequests.integration_id";
    }

    public static String sqlForFileTableJoin(boolean isPrJoin) {
        if (isPrJoin) {
            return " LEFT JOIN commit_files on commit_files.commit_sha = any(scm_pullrequests.commit_shas)";
        }
        return " LEFT JOIN commit_files on commit_files.commit_sha = scm_commits.commit_sha ";
    }

    public static String sqlForCommitJiraIssuesMappingTableJoin(String company, List<String> integrationIds, String paramSuffix) {
        return getScmIssueMappingsJoinSql(company, integrationIds, paramSuffix, COMMIT_JIRA_TABLE, "commit_jira", true, false);
    }

    public static String sqlForCommitWorkItemsMappingTableJoin(String company, List<String> integrationIds, String paramSuffix) {
        return getScmIssueMappingsJoinSql(company, integrationIds, paramSuffix, COMMIT_WORKITEM_TABLE, "commit_wi", false, false);
    }

    public static String sqlForPrJiraIssuesMappingTableJoin(String company, List<String> integrationIds, String paramSuffix) {
        return getScmIssueMappingsJoinSql(company, integrationIds, paramSuffix, PULLREQUESTS_JIRA_TABLE, "pr_jira", true, true);
    }

    public static String sqlForPrWorkitemsMappingTableJoin(String company, List<String> integrationIds, String paramSuffix) {
        return getScmIssueMappingsJoinSql(company, integrationIds, paramSuffix, PULLREQUESTS_WORKITEM_TABLE, "pr_wi", false, true);
    }

    @NotNull
    private static String getScmIssueMappingsJoinSql(String company, List<String> integrationIds, String paramSuffix, String table,
                                                     String tableAlias, Boolean isJiraIssue, Boolean isPrMapping) {
        String paramSuffixString = StringUtils.isEmpty(paramSuffix) ? "" : "_" + paramSuffix;
        String whereClause = "";
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            if (isJiraIssue && !isPrMapping) {
                whereClause = "";
            } else {
                whereClause = " WHERE scm_integration_id IN (:scm_integration_ids" + paramSuffixString + ") ";
            }
        }
        String joinOnClause;
        String issueAlias;
        String select;
        String groupBy;
        if (isPrMapping) {
            select = "pr_uuid";
            groupBy = " GROUP BY pr_uuid";
            joinOnClause = " ON " + tableAlias + ".pr_uuid = scm_pullrequests.id";
        } else {
            select = "commit_sha";
            groupBy = " GROUP BY commit_sha ";
            joinOnClause = " ON " + tableAlias + ".commit_sha = scm_commits.commit_sha ";
        }
        String issueType;
        if (isJiraIssue) {
            issueType = "issue_key";
        } else {
            issueType = "workitem_id";
        }
        issueAlias = getIssueAlias(isJiraIssue, isPrMapping);
        return " LEFT OUTER JOIN ( SELECT ARRAY_AGG(" + issueType + ") AS " + issueAlias + "," + select +
                " FROM " + company + "." + table + whereClause + groupBy + ") " + tableAlias + joinOnClause;
    }

    public static String getCommitsPRsJoinForDrillDown(String company, Map<String, Object> params, ScmPrFilter filter, String paramSuffixString, String commitTblQualifier,
                                                       String regexPatternConditionForCommits) {
        Boolean useMergeShaForCommitsJoin = CommonUtils.getUseMergeShaForCommitsJoinFlag(company);
        String whereClause = "";
        List<String> commitTableConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(filter.getCommitTitles())) {
            commitTableConditions.add(commitTblQualifier + "sc.message IN (:commit_titles" + paramSuffixString + ")");
            params.put("commit_titles" + paramSuffixString, filter.getCommitTitles());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCommitTitles())) {
            commitTableConditions.add(commitTblQualifier + "sc.message NOT IN (:exclude_commit_titles" + paramSuffixString + ")");
            params.put("exclude_commit_titles" + paramSuffixString, filter.getExcludeCommitTitles());
        }
        if (MapUtils.isNotEmpty(filter.getPartialMatch())) {
            createPartialMatchFilter(filter.getPartialMatch(), commitTableConditions, Set.of("commit_title"), Set.of(), params, commitTblQualifier, true);
        }
        if (MapUtils.isNotEmpty(filter.getExcludePartialMatch())) {
            createPartialMatchFilter(filter.getExcludePartialMatch(), commitTableConditions, Set.of("commit_title"), Set.of(), params, commitTblQualifier, false);
        }
        if (commitTableConditions.size() > 0) {
            whereClause = " WHERE " + String.join(" AND ", commitTableConditions);
        }
        return useMergeShaForCommitsJoin ? getCommitsPRsJoinUsingMergeSha(company, Strings.concat(regexPatternConditionForCommits,whereClause)):
                "  LEFT JOIN (SELECT pr.id, min(committed_at) AS first_commit_time,  COALESCE(sum(additions),0) AS lines_added,\n" +
                "  COALESCE(sum(deletions),0) as lines_deleted,\n" +
                "  COALESCE(sum(changes),0) AS lines_changed, \n" +
                "  COALESCE(sum(files_ct),0) as files_ct from " + company + ".scm_pullrequests pr\n" +
                "  LEFT JOIN " + company + ".scm_commits sc on sc.commit_sha = any(pr.commit_shas) " +
                "  AND pr.integration_id = sc.integration_id " +
                regexPatternConditionForCommits + whereClause +
                "  GROUP BY pr.id) as pr_scm_commits\n" +
                "  ON scm_pullrequests.id = pr_scm_commits.id";
    }

    public static String getCommitsPRsJoin(String company, Map<String, Object> params, ScmPrFilter filter, String paramSuffixString, String commitTblQualifier) {
        Boolean useMergeShaForCommitsJoin = CommonUtils.getUseMergeShaForCommitsJoinFlag(company);
        String whereClause = "";
        List<String> commitTableConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(filter.getCommitTitles())) {
            commitTableConditions.add(commitTblQualifier + "sc.message IN (:commit_titles" + paramSuffixString + ")");
            params.put("commit_titles" + paramSuffixString, filter.getCommitTitles());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCommitTitles())) {
            commitTableConditions.add(commitTblQualifier + "sc.message NOT IN (:exclude_commit_titles" + paramSuffixString + ")");
            params.put("exclude_commit_titles" + paramSuffixString, filter.getExcludeCommitTitles());
        }
        if (MapUtils.isNotEmpty(filter.getPartialMatch())) {
            createPartialMatchFilter(filter.getPartialMatch(), commitTableConditions, Set.of("commit_title"), Set.of(), params, commitTblQualifier, true);
        }
        if (MapUtils.isNotEmpty(filter.getExcludePartialMatch())) {
            createPartialMatchFilter(filter.getExcludePartialMatch(), commitTableConditions, Set.of("commit_title"), Set.of(), params, commitTblQualifier, false);
        }
        if (commitTableConditions.size() > 0) {
            whereClause = " WHERE " + String.join(" AND ", commitTableConditions);
        }
        return useMergeShaForCommitsJoin ? getCommitsPRsJoinUsingMergeSha(company,whereClause):
                "  LEFT JOIN (SELECT pr.id, min(committed_at) AS first_commit_time,  COALESCE(sum(additions),0) AS lines_added,\n" +
                "  COALESCE(sum(deletions),0) as lines_deleted,\n" +
                "  COALESCE(sum(changes),0) AS lines_changed, \n" +
                "  COALESCE(sum(files_ct),0) as files_ct from " + company + ".scm_pullrequests pr\n" +
                "  LEFT JOIN " + company + ".scm_commits sc on sc.commit_sha = any(pr.commit_shas) " +
                "  AND pr.integration_id = sc.integration_id " + whereClause +
                "  GROUP BY pr.id) as pr_scm_commits\n" +
                "  ON scm_pullrequests.id = pr_scm_commits.id";
    }

    private static String getCommitsPRsJoinUsingMergeSha(String company,String whereClause)
    {
        return "  LEFT JOIN (SELECT pr.id, min(committed_at) AS first_commit_time,  COALESCE(sum(additions),0) AS lines_added,\n" +
                "  COALESCE(sum(deletions),0) as lines_deleted,\n" +
                "  COALESCE(sum(changes),0) AS lines_changed, \n" +
                "  COALESCE(sum(files_ct),0) as files_ct from "+
                " (select pr_view.id, pr_view.integration_id, CASE WHEN pr_view.merge_sha IS NOT NULL THEN STRING_TO_ARRAY(pr_view.merge_sha,',') "+
                " ELSE pr_view.commit_shas END custom_merge_sha from "+ company + ".scm_pullrequests pr_view) pr"+
                "  LEFT JOIN " + company + ".scm_commits sc on sc.commit_sha = ANY(pr.custom_merge_sha)"+
                "  AND pr.integration_id = sc.integration_id " + whereClause +
                "  GROUP BY pr.id) as pr_scm_commits\n" +
                "  ON scm_pullrequests.id = pr_scm_commits.id";
    }



    public static String getCommitsJoinForScmDora(String company, List<String> integrationIds) {
        String whereCond = "";
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            whereCond = " AND sc.integration_id IN (:integration_ids)";
        }
        return "  LEFT JOIN (SELECT pr.id, min(committed_at) AS first_commit_time" +
                " from " + company + ".scm_pullrequests pr\n" +
                "  LEFT JOIN " + company + ".scm_commits sc on sc.commit_sha = any(pr.commit_shas) " +
                "  AND pr.integration_id = sc.integration_id " +
                whereCond +
                "  GROUP BY pr.id) as pr_scm_commits\n" +
                "  ON scm_pullrequests.id = pr_scm_commits.id";
    }

    public static SortingOrder getScmSortOrder(Map<String, SortingOrder> sortBy) {
        if (MapUtils.isEmpty(sortBy)) {
            return SortingOrder.DESC;
        }
        return sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> sortBy.getOrDefault(entry.getKey(), SortingOrder.DESC))
                .orElse(SortingOrder.DESC);
    }

    public static String getSqlForReviewTableJoin(String company, REVIEWS_TABLE_TYPE reviewType) {
        String joinType = "";
        String userIds = "";
        String whereCondition = "";
        String finalTableAlais = "";
        String reviewedAt = "";
        switch (reviewType) {
            case APPROVER:
                whereCondition = " WHERE lower(state) IN (" + StringUtils.join(APPROVED_STATES,",") + ")";
                joinType = "approvers,";
                userIds = "approver_ids,";
                reviewedAt = "pr_approved_at";
                finalTableAlais = "pr_approvers";
                break;
            case REVIEWER:
                joinType = "reviewers,";
                userIds = "reviewer_ids,";
                reviewedAt = "min_pr_reviewed_at";
                finalTableAlais = "pr_reviewers";
                break;
            case COMMENTER:
                whereCondition = " WHERE lower(state) IN ("+ StringUtils.join(COMMENTED_STATES,",")+")";
                joinType = "commenters,";
                userIds = "commenter_ids,";
                reviewedAt = "pr_commented_at";
                finalTableAlais = "pr_commenters";
                break;
        }
        return "  LEFT JOIN (SELECT  array_agg(reviewer) as " + joinType +
                " array_agg(reviewer_id) as " + userIds + " pr_id, MIN (reviewed_at) as " + reviewedAt +
                " FROM " + company + ".scm_pullrequest_reviews\n" +
                whereCondition + " group by pr_id \n" +
                " ) AS " + finalTableAlais + " ON scm_pullrequests.id = " + finalTableAlais + ".pr_id";
    }

    public static String getSqlForPRJiraMappingTable(String company) {
        return " LEFT JOIN (SELECT pr_uuid, array_agg(issue_key) as issue_keys from " + company + ".scm_pullrequests_jira_mappings  " +
                " GROUP BY pr_uuid) as scm_pullrequests_jira_mapping\n" +
                " ON scm_pullrequests.id = scm_pullrequests_jira_mapping.pr_uuid";
    }

    public static String getSqlForPRWorkItemMappingTable(String company) {
        return " LEFT JOIN (Select pr_uuid, array_agg(workitem_id) as workitem_ids from " + company + ".scm_pullrequests_workitem_mappings  " +
                " GROUP BY pr_uuid) as scm_pullrequests_workitem_mappings\n" +
                " ON scm_pullrequests.id = scm_pullrequests_workitem_mappings.pr_uuid";
    }

    public static String getSqlForCommitJiraMappingTable(String company) {
        return " LEFT JOIN (Select Array_agg(issue_key) AS cjissues, commit_sha from " + company + ".scm_commit_jira_mappings  " +
                " GROUP BY commit_sha) as commit_jira\n" +
                "  ON commit_jira.commit_sha = scm_commits.commit_sha";
    }

    public static String getSqlForCommitWorkItemMappingTable(String company) {
        return " LEFT JOIN (Select Array_agg(workitem_id) AS cworkitems, commit_sha from " + company + ".scm_commit_workitem_mappings  " +
                " GROUP BY commit_sha) as commit_wi " +
                " ON commit_wi.commit_sha = scm_commits.commit_sha";
    }

    public static String getSqlForPRReviewedAtJoin(String company) {
        return " LEFT JOIN (SELECT array_agg(reviewed_at) as pr_reviewed_at,\n" +
                " pr_id  FROM " + company + ".scm_pullrequest_reviews group by pr_id ) AS reviews  " +
                " ON  scm_pullrequests.id = reviews.pr_id ";
    }

    public static String getTagsTableJoin(String company, List<String> integrationIds) {
        String whereCond = "";
        if(CollectionUtils.isNotEmpty(integrationIds)) {
            whereCond = " AND st.integration_id IN (:integration_ids)";
        }
        return " LEFT JOIN\n" +
                " (\n" +
                "     SELECT    pr.id,\n" +
                "               anyarray_uniq(array_agg(tag)) as tags\n" +
                "     FROM      " + company + ".scm_pullrequests pr\n" +
                "     LEFT JOIN " + company + ".scm_tags st\n" +
                "     ON        st.commit_sha = ANY (pr.commit_shas)\n" +
                "     AND       st.integration_id = pr.integration_id\n" +
                 whereCond +
                "     GROUP BY  pr.id ) AS scm_pullrequests_tags\n" +
                "  ON        scm_pullrequests.id = scm_pullrequests_tags.id\n";
    }

    public static String getApprovalStatusSqlStmt() {
        return ", CASE " +
                "  WHEN ARRAY[scm_pullrequests.creator]::varchar[] = pr_approvers.approvers THEN 'self approved'\n" +
                "  WHEN scm_pullrequests.creator != any (pr_approvers.approvers) THEN 'peer approved'\n" +
                "  WHEN array_length(pr_approvers.approvers, 1) = 0 AND scm_pullrequests.merged = 't' THEN 'merged without approval'\n" +
                "  WHEN scm_pullrequests.pr_closed_at IS NOT NULL AND scm_pullrequests.merged = 'f' THEN 'rejected'\n" +
                "  WHEN scm_pullrequests.pr_merged_at is NULL AND scm_pullrequests.merged = 'f' THEN 'pending review'" +
                " ELSE 'not reviewed' " +
                " END approval_status";
    }

    public static String getReviewType() {
        // NOTE: LEV-5226
        // We used to check if the creator is either a reviewer OR an assignee - but this doesn't make sense for most of the apps.
        // If this becomes an issue for a given app, we should address it as an edge case (maybe add an app-specific case) and
        // not change the main logic.
        return " ,CASE " +
                " WHEN scm_pullrequests.creator = ANY(pr_reviewers.reviewers) " +
                " THEN '" + REVIEW_TYPE.SELF_REVIEWED + "'" +
                " WHEN coalesce(array_length(pr_reviewers.reviewers,1), 0) > 0 " +
                " THEN '" + REVIEW_TYPE.PEER_REVIEWED + "' " +
                " ELSE '" + REVIEW_TYPE.NOT_REVIEWED + "' " +
                " END review_type ";
    }

    @NotNull
    private static String getIssueAlias(Boolean isJiraIssue, Boolean isPrMapping) {
        String issueAlias;
        if (isPrMapping) {
            if (isJiraIssue) {
                issueAlias = "prjissues";
            } else {
                issueAlias = "prworkitems";
            }
        } else {
            if (isJiraIssue) {
                issueAlias = "cjissues";
            } else {
                issueAlias = "cworkitems";
            }
        }
        return issueAlias;
    }

    public static String getCollaborationStateSql() {
        return ",CASE \n" +
                " WHEN pr_approvers.approvers IS NULL  AND   pr_commenters.commenters IS NULL THEN 'unapproved'\n" +
                " WHEN pr_approvers.approvers IS NULL AND pr_commenters.commenters && scm_pullrequests.assignees = 't' THEN 'unapproved'\n" +
                " WHEN pr_approvers.approvers IS NULL  AND pr_commenters.commenters && scm_pullrequests.assignees = 'f' THEN 'unapproved'\n" +
                " WHEN pr_approvers.approvers IS NULL  AND  scm_pullrequests.creator = ANY(pr_commenters.commenters) THEN 'unapproved'\n" +
                " WHEN scm_pullrequests.creator = ANY (pr_approvers.approvers)  AND  pr_commenters.commenters IS NULL THEN 'self-approved'\n" +
                " WHEN scm_pullrequests.creator = ANY (pr_approvers.approvers)  AND  scm_pullrequests.creator = ANY(pr_commenters.commenters) THEN 'self-approved'\n" +
                " WHEN scm_pullrequests.creator = ANY (pr_approvers.approvers)  AND  pr_commenters.commenters && scm_pullrequests.assignees = 't' THEN 'self-approved-with-review'\n" +
                " WHEN scm_pullrequests.creator = ANY (pr_approvers.approvers)  AND  pr_commenters.commenters && scm_pullrequests.assignees = 'f' THEN 'self-approved-with-review'\n" +
                " WHEN pr_approvers.approvers && scm_pullrequests.assignees = 't' AND   pr_commenters.commenters IS NULL THEN 'assigned-peer-approved'\n" +
                " WHEN pr_approvers.approvers && scm_pullrequests.assignees = 't' AND  scm_pullrequests.creator = ANY(pr_commenters.commenters) THEN 'assigned-peer-approved'\n" +
                " WHEN pr_approvers.approvers && scm_pullrequests.assignees = 't' AND  pr_commenters.commenters && scm_pullrequests.assignees = 'f' THEN 'assigned-peer-approved'\n" +
                " WHEN pr_approvers.approvers && scm_pullrequests.assignees = 't' AND  pr_commenters.commenters && scm_pullrequests.assignees = 't' THEN 'assigned-peer-approved'\n" +
                " WHEN pr_approvers.approvers && scm_pullrequests.assignees = 'f' AND   pr_commenters.commenters IS NULL THEN 'unassigned-peer-approved'\n" +
                " WHEN pr_approvers.approvers && scm_pullrequests.assignees = 'f' AND  scm_pullrequests.creator= ANY(pr_commenters.commenters) THEN 'unassigned-peer-approved'\n" +
                " WHEN pr_approvers.approvers && scm_pullrequests.assignees = 'f' AND  pr_commenters.commenters && scm_pullrequests.assignees = 'f' THEN 'unassigned-peer-approved'\n" +
                " WHEN pr_approvers.approvers && scm_pullrequests.assignees = 'f' AND   pr_commenters.commenters && scm_pullrequests.assignees = 't' THEN 'unassigned-peer-approved'\n" +
                "END collab_state";
    }

    public static String getCodeChangeSql(Map<String, String> codeChangeSizeConfig, String columnName) {
        Map<String, String> codeChangeSizeConfigMAp = MapUtils.emptyIfNull(codeChangeSizeConfig);
        return ", CASE " +
                " WHEN " + columnName + " IS NULL OR " + columnName + " <= " + codeChangeSizeConfigMAp.getOrDefault("small", "50")
                + " THEN 'small'" +
                " WHEN " + columnName + " > " + codeChangeSizeConfigMAp.getOrDefault("small", "50")
                + " AND  " + columnName + " <= " + codeChangeSizeConfigMAp.getOrDefault("medium", "150")
                + " THEN 'medium'" +
                " WHEN " + columnName + " > " + codeChangeSizeConfigMAp.getOrDefault("medium", "150")
                + " THEN 'large'"
                + " END code_change";
    }

    public static String getCodeChangeSql(Map<String, String> codeChangeSizeConfig, Boolean isPr,String codeChangeUnit) {
        String codeChangeColumn = (codeChangeUnit!=null && codeChangeUnit.equals("files")) ? "files_ct" : "lines_changed";

        if (isPr)
            return getCodeChangeSql(codeChangeSizeConfig, codeChangeColumn);
        else
            return getCodeChangeSql(codeChangeSizeConfig, "(additions+deletions+changes)");
    }

    public static String getFilesChangeSql(Map<String, String> codeChangeSizeConfig) {
        Map<String, String> codeChangeSizeConfigMAp = MapUtils.emptyIfNull(codeChangeSizeConfig);
        return ", CASE " +
                " WHEN files_ct IS NULL OR files_ct <= " + codeChangeSizeConfigMAp.getOrDefault("small", "50")
                + " THEN 'small'" +
                " WHEN files_ct > " + codeChangeSizeConfigMAp.getOrDefault("small", "50")
                + " AND files_ct <= " + codeChangeSizeConfigMAp.getOrDefault("medium", "150")
                + " THEN 'medium'" +
                " WHEN files_ct > " + codeChangeSizeConfigMAp.getOrDefault("medium", "150")
                + " THEN 'large'" + "\n"
                + " END files_changed  ";
    }

    public static String getDeploymentFrequencySql() {
        return "CASE\n" +
                "        WHEN deployment_frequency >= 1 THEN 'ELITE'\n" +
                "        WHEN deployment_frequency < 1\n" +
                "        AND    deployment_frequency >= 0.1428 THEN 'HIGH'\n" +
                "        WHEN deployment_frequency < 0.1428\n" +
                "        AND    deployment_frequency >= 0.0333 THEN 'MEDIUM'\n" +
                "        WHEN deployment_frequency < 0.0333\n" +
                "        OR deployment_frequency IS NULL THEN 'LOW'\n" +
                "END band";
    }

    public static String getFailureRateSql() {
        return "CASE\n" +
                "        WHEN failure_rate IS NULL\n" +
                "        OR     failure_rate <= 15.0 THEN 'ELITE'\n" +
                "        WHEN failure_rate > 15.0\n" +
                "        AND    failure_rate <= 30.0 THEN 'HIGH'\n" +
                "        WHEN failure_rate > 30.0\n" +
                "        AND    failure_rate <= 45.0 THEN 'MEDIUM'\n" +
                "        WHEN failure_rate > 45.0 THEN 'LOW'\n" +
                "END band";
    }

    public static String getCommentDensitySql(ScmPrFilter filter) {
        Map<String, String> commentDensitySizeConfigMap = MapUtils.emptyIfNull(filter.getCommentDensitySizeConfig());
        String diffFilter = "COALESCE(Array_length(pr_commenters.commenters ,1),0)";
        return ", CASE " +
                " WHEN " + diffFilter + " <= " + commentDensitySizeConfigMap.getOrDefault("shallow", "50")
                + " THEN 'shallow'" +
                " WHEN " + diffFilter + " > " + commentDensitySizeConfigMap.getOrDefault("shallow", "50")
                + " AND " + diffFilter + " <= " + commentDensitySizeConfigMap.getOrDefault("good", "150")
                + " THEN 'good'" +
                " WHEN " + diffFilter + " > " + commentDensitySizeConfigMap.getOrDefault("good", "150")
                + " THEN 'heavy'" + "\n"
                + " END comment_density  ";
    }

    public static String getIssueKeysSql() {
        return " ,CASE\n" +
                " WHEN COALESCE(array_length(issue_keys, 1),array_length(workitem_ids,1),0) > 0 THEN 1 \n" +
                " ELSE 0\n" +
                " END has_issue_keys ";
    }

    public static String getScmCommitMetricsQuery(String company, String integIdCondition) {
        return " LEFT JOIN ( SELECT   commit_sha,\n" +
                "                       tot_addition,\n" +
                "                       tot_deletion,\n" +
                "                       tot_change," +
                "                       tot_change+tot_deletion+tot_addition as lines_changed,\n" +
                "                       total_legacy_code_lines as total_legacy_code_lines,\n" +
                "                       total_refactored_code_lines as total_refactored_code_lines,\n" +
                "         (total_legacy_code_lines     * 100.0/nullif((tot_addition+tot_deletion+tot_change),0)) AS pct_legacy_refactored_lines,\n" +
                "         (total_refactored_code_lines * 100.0/nullif((tot_addition+tot_deletion+tot_change),0)) AS pct_refactored_lines,\n" +
                "         (total_new_lines             * 100.0/nullif((tot_addition+tot_deletion+tot_change),0)) AS pct_new_lines\n" +
                " FROM     (\n" +
                "                    SELECT     commit_sha,\n" +
                "                               Array_agg(filetype) OVER (partition BY commit_sha) AS file_types,\n" +
                "                               Sum(addition) OVER (partition BY commit_sha)       AS tot_addition ,\n" +
                "                               Sum(deletion) OVER (partition BY commit_sha)       AS tot_deletion,\n" +
                "                               Sum(change) OVER (partition BY commit_sha)         AS tot_change,\n" +
                "                               Sum(addition+deletion+change) filter (WHERE previous_committed_at IS NOT NULL\n" +
                "                    AND        previous_committed_at < to_timestamp( :metric_previous_committed_at )) OVER (partition BY commit_sha) AS total_legacy_code_lines,\n" +
                "                               sum(addition+deletion+change) filter (WHERE previous_committed_at IS NOT NULL\n" +
                "                    AND        previous_committed_at > to_timestamp( :metric_previous_committed_at )) OVER (partition BY commit_sha ) AS total_refactored_code_lines ,\n" +
                "                               sum(addition+deletion+change) filter (WHERE previous_committed_at IS NULL) OVER (partition BY commit_sha ) AS total_new_lines\n" +
                "                    FROM       " + company + ".scm_files scm_files\n" +
                "                    INNER JOIN " + company + ".scm_file_commits scm_file_commits\n" +
                "                    ON         scm_file_commits.file_id = scm_files.id " + integIdCondition + ") z \n" +
                " GROUP BY commit_sha,\n" +
                "         file_types,\n" +
                "         tot_addition,\n" +
                "         tot_deletion,\n" +
                "         tot_change," +
                "         lines_changed,\n" +
                "         total_legacy_code_lines,\n" +
                "         total_refactored_code_lines,\n" +
                "         total_new_lines ) commit_files ON commit_files.commit_sha = scm_commits.commit_sha";
    }

    public static String getFileCommitsSelect() {
        return ",addition,deletion,change,previous_committed_at,commit_files.commit_sha as files_commit_sha,lines_changed,file_id," +
                "case when previous_committed_at IS NOT NULL\n" +
                "  AND   previous_committed_at < to_timestamp( :metric_previous_committed_at ) then 'legacy_refactored_lines'   " +
                "  when        previous_committed_at IS NOT NULL\n" +
                "  AND   previous_committed_at > to_timestamp( :metric_previous_committed_at ) then 'refactored_lines' \n" +
                "  when previous_committed_at IS NULL then 'new_lines' when lines_changed is null then 'null' end as code_category";
    }

    public static String getTechnologyJoinSqlStmt(String company) {
        return " INNER JOIN ("
                + " SELECT name as technology,repo_id as tr_id,integration_id as ti_id FROM "
                + company + ".gittechnologies ) x ON x.tr_id = ANY(scm_commits.repo_id) AND scm_commits.integration_id = x.ti_id ";
    }

    public static ScmPrFilter.ScmPrFilterBuilder getFilterForTrendStack(ScmPrFilter.ScmPrFilterBuilder newFilterBuilder,
                                                                        DbAggregationResult row, ScmPrFilter.DISTINCT across,
                                                                        ScmPrFilter.DISTINCT stack, String aggInterval) throws SQLException {
        if(aggInterval.equals(AGG_INTERVAL.day_of_week.toString())){

            switch (across) {
                case pr_created:
                    newFilterBuilder.prCreatedDaysOfWeek(List.of(row.getKey()));
                    break;
                case pr_closed:
                    newFilterBuilder.prClosedDaysOfWeek(List.of(row.getKey()));
                    break;
                case pr_merged:
                    newFilterBuilder.prMergedDaysOfWeek(List.of(row.getKey()));
                    break;
                default:
                    throw new SQLException("This across option is not available trend. Provided across: " + across);
            }

            return newFilterBuilder.across(stack);
        }

        ImmutablePair<Long, Long> timeRange = getTimeRangeForStacks(row, aggInterval);
        switch (across) {
            case pr_created:
                newFilterBuilder.prCreatedRange(timeRange);
                break;
            case pr_closed:
                newFilterBuilder.prClosedRange(timeRange);
                break;
            case pr_merged:
                newFilterBuilder.prMergedRange(timeRange);
                break;
            default:
                throw new SQLException("This across option is not available trend. Provided across: " + across);
        }
        return newFilterBuilder.across(stack);
    }

    public static ScmCommitFilter.ScmCommitFilterBuilder getCommitsFilterForTrendStack(ScmCommitFilter.ScmCommitFilterBuilder newFilterBuilder,
                                                                                       DbAggregationResult row, ScmCommitFilter.DISTINCT across,
                                                                                       ScmCommitFilter.DISTINCT stack, String aggInterval) throws SQLException {

        if(aggInterval.equals(AGG_INTERVAL.day_of_week.toString()) && across == ScmCommitFilter.DISTINCT.trend){
            newFilterBuilder.daysOfWeek(List.of(row.getKey()));
            return newFilterBuilder.across(stack);
        }
        ImmutablePair<Long, Long> timeRange = getTimeRangeForStacks(row, aggInterval);
        if (across == ScmCommitFilter.DISTINCT.trend) {
            newFilterBuilder.committedAtRange(timeRange);
        } else {
            throw new SQLException("This across option is not available trend. Provided across: " + across);
        }
        return newFilterBuilder.across(stack);
    }

    public static ScmPrFilter.ScmPrFilterBuilder getFilterWithConfig(ScmPrFilter.ScmPrFilterBuilder newFilterBuilder,
                                                                     ScmPrFilter filter) {
        return newFilterBuilder
                .codeChangeSizeConfig(filter.getCodeChangeSizeConfig())
                .codeChangeUnit(filter.getCodeChangeUnit())
                .commentDensitySizeConfig(filter.getCommentDensitySizeConfig());
    }

    public static boolean isScmIntegration(IntegrationType type) {
        return (type == null) ? false : type.isScmFamily();
    }

    public static boolean checkCommitsTableFiltersJoin(ScmPrFilter filter, ScmPrFilter.CALCULATION calculation, boolean valuesOnly) {
        return CollectionUtils.isNotEmpty(filter.getCodeChanges()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeCodeChanges())
                || filter.getAcross() == ScmPrFilter.DISTINCT.code_change
                || (calculation == ScmPrFilter.CALCULATION.count && !valuesOnly) ;
    }

    public static boolean checkCommentDensityFiltersJoin(ScmPrFilter filter, ScmPrFilter.CALCULATION calculation, boolean valuesOnly) {
        return CollectionUtils.isNotEmpty(filter.getCommentDensities()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeCommentDensities()) ||
                CollectionUtils.isNotEmpty(filter.getCollabStates()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeCollabStates()) ||
                CollectionUtils.isNotEmpty(filter.getCommenters()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeCommenters()) ||
                filter.getAcross() == ScmPrFilter.DISTINCT.comment_density ||
                (calculation == ScmPrFilter.CALCULATION.count && !valuesOnly);
    }

    public static boolean checkPRReviewedJoin(ScmPrFilter filter) {
        return filter.getAcross() == ScmPrFilter.DISTINCT.pr_reviewed;
    }

    public static boolean checkCreatorsFiltersJoin(ScmPrFilter filter, OUConfiguration ouConfig) {
        return CollectionUtils.isNotEmpty(filter.getCreators()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeCreators()) ||
                (filter.getPartialMatch() != null && MapUtils.size(filter.getPartialMatch().get("creator")) > 0) ||
                OrgUnitHelper.doesOUConfigHavePRCreator(ouConfig);
    }

    public static boolean checkReviewersFiltersJoin(ScmPrFilter filter, ScmPrFilter.CALCULATION calculation, boolean valuesOnly) {
        return CollectionUtils.isNotEmpty(filter.getExcludeReviewers()) ||
                CollectionUtils.isNotEmpty(filter.getReviewers()) ||
                CollectionUtils.isNotEmpty(filter.getReviewTypes()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeReviewTypes()) ||
                CollectionUtils.isNotEmpty(filter.getApprovalStatuses()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeApprovalStatuses()) ||
                (filter.getReviewerCount() != null && filter.getReviewerCount().getLeft() != null && filter.getReviewerCount().getRight() != null) ||
                (calculation == ScmPrFilter.CALCULATION.count && !valuesOnly) || (filter.getPartialMatch() != null
                && MapUtils.size(filter.getPartialMatch().get("reviewer")) > 0);
    }

    public static boolean checkApproversFiltersJoin(ScmPrFilter filter, ScmPrFilter.CALCULATION calculation, boolean valuesOnly) {
        return CollectionUtils.isNotEmpty(filter.getApprovalStatuses()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeApprovalStatuses()) ||
                CollectionUtils.isNotEmpty(filter.getCollabStates()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeCollabStates()) ||
                (filter.getApproverCount() != null && filter.getApproverCount().getLeft() != null && filter.getApproverCount().getRight() != null)
                || CollectionUtils.isNotEmpty(filter.getApprovers()) || CollectionUtils.isNotEmpty(filter.getExcludeApprovers()) ||
                (calculation == ScmPrFilter.CALCULATION.count && !valuesOnly) || (filter.getPartialMatch() != null
                && MapUtils.size(filter.getPartialMatch().get("approver")) > 0);
    }

    public static boolean checkApprovalStatus(ScmPrFilter filter) {
        return CollectionUtils.isNotEmpty(filter.getApprovalStatuses()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeApprovalStatuses());
    }

    public static boolean checkCollaborationState(ScmPrFilter filter) {
        return CollectionUtils.isNotEmpty(filter.getCollabStates()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeCollabStates()) ||
                filter.getAcross() == ScmPrFilter.DISTINCT.collab_state;
    }


    public static boolean checkReviewTypeFilters(ScmPrFilter filter) {
        return CollectionUtils.isNotEmpty(filter.getReviewTypes()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeReviewTypes());
    }


    public static boolean checkCommitters(ScmCommitFilter filter,  OUConfiguration ouConfig) {
        return CollectionUtils.isNotEmpty(filter.getCommitters()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeCommitters())|| (filter.getPartialMatch() != null
                && MapUtils.size(filter.getPartialMatch().get("committer")) > 0) ||
                OrgUnitHelper.doesOuConfigHaveCommitCommitters(ouConfig);
    }

    public static boolean checkAuthors(ScmCommitFilter filter, OUConfiguration ouConfig) {
        return CollectionUtils.isNotEmpty(filter.getAuthors()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeAuthors()) || (filter.getPartialMatch() != null
                && MapUtils.size(filter.getPartialMatch().get("author")) > 0) ||
                OrgUnitHelper.doesOuConfigHaveCommitAuthors(ouConfig);
    }

    public static boolean checkCommitters(ScmReposFilter filter, OUConfiguration ouConfig) {
        return CollectionUtils.isNotEmpty(filter.getCommitters()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeCommitters())|| (filter.getPartialMatch() != null
                && MapUtils.size(filter.getPartialMatch().get("committer")) > 0) ||
                OrgUnitHelper.doesOuConfigHaveCommitCommitters(ouConfig);
    }

    public static boolean checkAuthors(ScmReposFilter filter, OUConfiguration ouConfig) {
        return CollectionUtils.isNotEmpty(filter.getAuthors()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeAuthors()) || (filter.getPartialMatch() != null
                && MapUtils.size(filter.getPartialMatch().get("author")) > 0) ||
                OrgUnitHelper.doesOuConfigHaveCommitAuthors(ouConfig);
    }

    public static boolean checkFileTableFilters(ScmCommitFilter filter) {
        return CollectionUtils.isNotEmpty(filter.getExcludeFileTypes()) ||
                CollectionUtils.isNotEmpty(filter.getFileTypes()) ||
                CollectionUtils.isNotEmpty(filter.getCodeChanges());
    }
    public static boolean checkAuthorsFilters(ScmContributorsFilter filter, OUConfiguration ouConfig) {
        return CollectionUtils.isNotEmpty(filter.getAuthors()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeAuthors()) ||
                filter.getAcross() == ScmContributorsFilter.DISTINCT.author || (filter.getPartialMatch() != null
                && MapUtils.size(filter.getPartialMatch().get("author")) > 0)||
                OrgUnitHelper.doesOuConfigHaveCommitAuthors(ouConfig);
    }

    public static boolean checkCommittersFilters(ScmContributorsFilter filter, OUConfiguration ouConfig) {
        return CollectionUtils.isNotEmpty(filter.getCommitters()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeCommitters()) ||
                filter.getAcross() != ScmContributorsFilter.DISTINCT.author || (filter.getPartialMatch() != null
                && MapUtils.size(filter.getPartialMatch().get("committer")) > 0) ||
                OrgUnitHelper.doesOuConfigHaveCommitCommitters(ouConfig);
    }

    public static boolean checkTechnologiesFilter(ScmCommitFilter filter) {
        return CollectionUtils.isNotEmpty(filter.getTechnologies()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeTechnologies()) ||
                filter.getAcross() == ScmCommitFilter.DISTINCT.technology;
    }

    /*
    partialMatchColumns & partialMatchArrayColumns should be Non Null
     */

    public static void createPartialMatchFilter(Map<String, Map<String, String>> partialMatchMap, List<String> fileTableConditions,
                                          Set<String> partialMatchColumns, Set<String> partialMatchArrayColumns,
                                          Map<String, Object> params, String tableQualifier, boolean includePartialMatch) {
        for (Map.Entry<String, Map<String, String>> partialMatchEntry : partialMatchMap.entrySet()) {
            String key = partialMatchEntry.getKey();
            Map<String, String> value = partialMatchEntry.getValue();

            String begins = value.get("$begins");
            String ends = value.get("$ends");
            String contains = value.get("$contains");
            String regex = value.get("$regex");

            if (begins != null || ends != null || contains != null || regex != null) {
                if (partialMatchColumns.contains(key)) {
                    createPartialMatchCondition(fileTableConditions, params, key, begins, ends, contains, regex, tableQualifier, includePartialMatch);
                } else if (partialMatchArrayColumns.contains(key)) {
                    createPartialMatchConditionArray(fileTableConditions, params, key, begins, ends, contains, regex, tableQualifier, includePartialMatch);
                }
            }
        }
    }

    public static void createPartialMatchConditionArray(List<String> conditions, Map<String, Object> params, String key,
                                                        String begins, String ends, String contains, String regex, String prTblQualifier,
                                                        boolean includePartialMatch) {
        key = prTblQualifier + key;
        String includePartialMatchCond = includePartialMatch ? StringUtils.EMPTY : "NOT ";
        String varForBegins = includePartialMatch ? key + "_begins" : key + "_NOT_begins";
        if (begins != null) {
            String beginsCondition = includePartialMatchCond + "exists (select 1 from unnest (" + key + ") as k where k SIMILAR TO :" + varForBegins + " )";
            params.put(varForBegins, begins + "%");
            conditions.add(beginsCondition);
        }
        String varForEnds = includePartialMatch ? key + "_ends" : key + "_NOT_ends";
        if (ends != null) {
            String endsCondition = includePartialMatchCond + "exists (select 1 from unnest (" + key + ") as k where k SIMILAR TO :" + varForEnds + " )";
            params.put(varForEnds, "%" + ends);
            conditions.add(endsCondition);
        }
        String varForContains = includePartialMatch ? key + "_contains" : key + "_NOT_contains";
        if (contains != null) {
            String containsCondition = includePartialMatchCond + "exists (select 1 from unnest (" + key + ") as k where k SIMILAR TO :" + varForContains + " )";
            params.put(varForContains, "%" + contains + "%");
            conditions.add(containsCondition);
        }
        String varForRegex = includePartialMatch ? key + "_regex" : key + "_NOT_regex";
        if (regex != null) {
            String regexCondition = includePartialMatchCond + "exists (select 1 from unnest (" + key + ") as k where k ~ :" + varForRegex + " )";
            params.put(varForRegex, regex);
            conditions.add(regexCondition);
        }
    }

    public static void createPartialMatchCondition(List<String> fileTableConditions, Map<String, Object> params, String key,
                                                   String begins, String ends, String contains, String regex, String prTblQualifier,
                                                   boolean includePartialMatch) {
        key = prTblQualifier + key;
        String includePartialMatchCond = includePartialMatch ? StringUtils.EMPTY : "NOT";
        key = key.equalsIgnoreCase(prTblQualifier + "commit_title") ? "message" : key;
        String varForBegins = includePartialMatch ? key + "_begins" : key + "_NOT_begins";
        if (begins != null) {
            String beingsCondition = key + " " + includePartialMatchCond + " SIMILAR TO :" + varForBegins;
            params.put(varForBegins, begins + "%");
            fileTableConditions.add(beingsCondition);
        }
        String varForEnds = includePartialMatch ? key + "_ends" : key + "_NOT_ends";
        if (ends != null) {
            String endsCondition = key + " " + includePartialMatchCond + " SIMILAR TO :" + varForEnds;
            params.put(varForEnds, "%" + ends);
            fileTableConditions.add(endsCondition);
        }
        String varForContains = includePartialMatch ? key + "_contains" : key + "_NOT_contains";
        if (contains != null) {
            String containsCondition = key + " " + includePartialMatchCond + " SIMILAR TO :" + varForContains;
            params.put(varForContains, "%" + contains + "%");
            fileTableConditions.add(containsCondition);
        }
        String varForRegex = includePartialMatch ? key + "_regex" : key + "_NOT_regex";
        if (regex != null) {
            includePartialMatchCond = includePartialMatch ? " ~ :" : " !~ :";
            String regexCondition = key + includePartialMatchCond + varForRegex;
            params.put(varForRegex, regex);
            fileTableConditions.add(regexCondition);
        }
    }

}
