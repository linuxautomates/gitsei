package io.levelops.commons.databases.services.parsers;

import io.levelops.commons.databases.models.filters.ScmContributorsFilter;
import io.levelops.commons.databases.services.ScmQueryUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.services.parsers.ScmFilterUtils.getListOrDefault;

public class ScmContributorsFilterParser {

    private static final String PRS_TABLE = "scm_pullrequests";
    private static final String COMMITS_TABLE = "scm_commits";

    public ScmContributorsFilter merge(Integer integrationId, ScmContributorsFilter reqFilter, Map<String, Object> productFilter) {
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) productFilter.get("partial_match"));
        Map<String, Object> excludedFields = (Map<String, Object>) productFilter.getOrDefault("exclude", Map.of());
        Map<String, Map<String, String>> excludePartialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) excludedFields.get("partial_match"));
        return ScmContributorsFilter.builder()
                .across(reqFilter.getAcross())
                .dataTimeRange(reqFilter.getDataTimeRange())
                .repoIds(getListOrDefault(productFilter, "repo_ids"))
                .projects(getListOrDefault(productFilter, "projects"))
                .committers(getListOrDefault(productFilter, "committers"))
                .authors(getListOrDefault(productFilter, "authors"))
                .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                .excludeProjects(getListOrDefault(excludedFields, "projects"))
                .excludeCommitters(getListOrDefault(excludedFields, "committers"))
                .excludeAuthors(getListOrDefault(excludedFields, "authors"))
                .integrationIds(List.of(String.valueOf(integrationId)))
                .partialMatch(partialMatchMap)
                .excludePartialMatch(excludePartialMatchMap)
                .build();
    }

    public String getSqlStmt(String company, Map<String, List<String>> conditions, ScmContributorsFilter scmContributorsFilter, int paramSuffix) {
        String integIdCondition = "";
        String commitsSelectAndGroupBy = (ScmContributorsFilter.DISTINCT.author == scmContributorsFilter.getAcross()) ?
                "author, author_id" : "committer, committer_id";
        String commitsSelectDistinctString = (ScmContributorsFilter.DISTINCT.author == scmContributorsFilter.getAcross()) ?
                "author" : "committer";
        String prsWhere = CollectionUtils.isEmpty(conditions.get(PRS_TABLE)) ? "" :
                " WHERE " + String.join(" AND ", conditions.get(PRS_TABLE));
        String commitsWhere = CollectionUtils.isEmpty(conditions.get(COMMITS_TABLE)) ? "" :
                " WHERE " + String.join(" AND ", conditions.get(COMMITS_TABLE));
        String commitAuthorTableJoin = ScmQueryUtils.sqlForAuthorTableJoin(company);
        String commitCommitterTableJoin = ScmQueryUtils.sqlForCommitterTableJoin(company);
        String pullCreatorTableJoin = ScmQueryUtils.sqlForCreatorTableJoin(company);
        String commitsSelect = ScmQueryUtils.SCM_COMMITS_SELECT + ScmQueryUtils.AUTHORS_SELECT + ScmQueryUtils.COMMITTERS_SELECT;
        String prsSelect = ScmQueryUtils.PRS_SELECT;
        String creatorsSelect = ScmQueryUtils.CREATORS_SQL;
        String fileTableJoin = ScmQueryUtils.sqlForFileTableJoin(false);
        if (CollectionUtils.isNotEmpty(scmContributorsFilter.getIntegrationIds()))
            integIdCondition = " WHERE scm_files.integration_id IN (:scm_integration_ids_1)";
        String fileTableJoinStmt = "WITH commit_files AS ( Select commit_sha,array_agg(DISTINCT(filetype)) AS file_type FROM " + company + ".scm_files scm_files INNER JOIN " +
                company + ".scm_file_commits scm_file_commits ON scm_file_commits.file_id = scm_files.id " +
                integIdCondition + " GROUP BY commit_sha)";
        String commitsSQL = "(" + fileTableJoinStmt + " SELECT "+ commitsSelect + ",commit_files.file_type FROM " + company + "." + COMMITS_TABLE + commitAuthorTableJoin +
                commitCommitterTableJoin + fileTableJoin + ")scm_commits ";
        String prsSQL = "( SELECT" + prsSelect + creatorsSelect + " FROM " + company + "." + PRS_TABLE + pullCreatorTableJoin + ") scm_pullrequests ";
        String commitJiraIssuesMappingTableJoin = ScmQueryUtils.sqlForCommitJiraIssuesMappingTableJoin(company, scmContributorsFilter.getIntegrationIds(), String.valueOf(paramSuffix));
        String commitWorkitemsMappingTableJoin = ScmQueryUtils.sqlForCommitWorkItemsMappingTableJoin(company, scmContributorsFilter.getIntegrationIds(), String.valueOf(paramSuffix));
        String prJiraIssuesMappingTableJoin = ScmQueryUtils.sqlForPrJiraIssuesMappingTableJoin(company, scmContributorsFilter.getIntegrationIds(), String.valueOf(paramSuffix));
        String prWorkitemsMappingTableJoin = ScmQueryUtils.sqlForPrWorkitemsMappingTableJoin(company, scmContributorsFilter.getIntegrationIds(), String.valueOf(paramSuffix));

        return  "SELECT *,array(SELECT DISTINCT unnest (files)) AS file_types,Array_length(array(SELECT DISTINCT unnest (repos)),1) AS num_repos FROM ( WITH comms AS ( "
                + "SELECT array_cat_agg(file_type) AS files,array_cat_agg(repo_id)AS repos,COUNT(*) AS num_commits, SUM(additions) AS num_additions,"
                + " SUM(deletions) AS num_deletions , SUM(changes) AS num_changes, array_cat_agg(cjissues) AS cjissues, array_cat_agg(cworkitems) AS cworkitems, "
                + commitsSelectAndGroupBy + " FROM "
                + commitsSQL
                + commitJiraIssuesMappingTableJoin
                + commitWorkitemsMappingTableJoin
                + commitsWhere
                + " GROUP BY " + commitsSelectAndGroupBy + " ), pulls AS ( SELECT COUNT(*) AS num_prs,"
                + " array_cat_agg(prjissues) AS prjissues,array_cat_agg(prworkitems) AS prworkitems, creator,creator_id FROM "
                + prsSQL
                + prJiraIssuesMappingTableJoin
                + prWorkitemsMappingTableJoin
                + prsWhere
                + " GROUP BY creator,creator_id ) SELECT files,repos,num_commits,num_additions,num_deletions,"
                + "num_changes,creator,creator_id,num_prs,prjissues,prworkitems,cjissues, cworkitems," + commitsSelectDistinctString +
                "," + ((ScmContributorsFilter.DISTINCT.author == scmContributorsFilter.getAcross()) ? "author_id" : "committer_id") + " FROM ("
                + " SELECT * FROM comms LEFT JOIN pulls ON comms." + ((ScmContributorsFilter.DISTINCT.author == scmContributorsFilter.getAcross()) ? "author_id" : "committer_id")
                + "= pulls.creator_id )"
                + " x ) a";
    }
}
