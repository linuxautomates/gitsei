package io.levelops.commons.databases.services.parsers;

import io.levelops.commons.databases.models.filters.ScmReposFilter;
import io.levelops.commons.databases.services.ScmQueryUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.parsers.ScmFilterUtils.getListOrDefault;

public class ScmReposFilterParser {

    private static final String COMMITS_TABLE = "scm_commits";
    private static final String PRS_TABLE = "scm_pullrequests";

    public ScmReposFilter merge(Integer integrationId, ScmReposFilter reqFilter, Map<String, Object> productFilter) {
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) productFilter.get("partial_match"));
        Map<String, Object> excludedFields = (Map<String, Object>) productFilter.getOrDefault("exclude", Map.of());
        return ScmReposFilter.builder()
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
                .build();
    }

    public String getSqlStmt(String company, Map<String, List<String>> conditions, ScmReposFilter scmReposFilter,
                             int paramSuffix, boolean isRepoType) {
        String commitsWhere = "";
        String prsWhere = "";
        String reposWhere = "";
        String commitAuthorTableJoin = ScmQueryUtils.sqlForAuthorTableJoin(company);
        String commitCommitterTableJoin = ScmQueryUtils.sqlForCommitterTableJoin(company);
        String pullCreatorTableJoin = ScmQueryUtils.sqlForCreatorTableJoin(company);
        String commitsSelect;
        String prsSelect;
        String creatorsSelect = ScmQueryUtils.CREATORS_SQL;
        if (conditions.get(PRS_TABLE).size() > 0)
            prsWhere = " WHERE " + String.join(" AND ", conditions.get(PRS_TABLE));
        if (conditions.get(COMMITS_TABLE).size() > 0)
            commitsWhere = " WHERE " + String.join(" AND ", conditions.get(COMMITS_TABLE));
        String commitJiraIssuesMappingTableJoin = ScmQueryUtils.sqlForCommitJiraIssuesMappingTableJoin(company, scmReposFilter.getIntegrationIds(), String.valueOf(paramSuffix));
        String commitWorkitemsMappingTableJoin = ScmQueryUtils.sqlForCommitWorkItemsMappingTableJoin(company, scmReposFilter.getIntegrationIds(), String.valueOf(paramSuffix));
        String prJiraIssuesMappingTableJoin = ScmQueryUtils.sqlForPrJiraIssuesMappingTableJoin(company, scmReposFilter.getIntegrationIds(), String.valueOf(paramSuffix));
        String prWorkitemsMappingTableJoin = ScmQueryUtils.sqlForPrWorkitemsMappingTableJoin(company, scmReposFilter.getIntegrationIds(), String.valueOf(paramSuffix));
        if(isRepoType) {
            commitsSelect =ScmQueryUtils.SCM_COMMITS_SELECT + ScmQueryUtils.AUTHORS_SELECT + ScmQueryUtils.COMMITTERS_SELECT;
            prsSelect = ScmQueryUtils.PRS_SELECT;
            if (CollectionUtils.isNotEmpty(scmReposFilter.getRepoIds())) {
                reposWhere = " WHERE cr IN (:scm_repo_ids_" + paramSuffix + ") ";
            }
            String commitsSQL = "( SELECT" + commitsSelect + "FROM " + company + "." + COMMITS_TABLE + commitAuthorTableJoin +
                    commitCommitterTableJoin + ") scm_commits ";
            String prsSQL = "( SELECT" + prsSelect + creatorsSelect + " FROM " + company + "." + PRS_TABLE + pullCreatorTableJoin + ") scm_pullrequests ";
            return "SELECT * FROM ( WITH comms AS ( SELECT COUNT(*) AS num_commits, SUM(additions) AS num_additions ,"
                    + " SUM(deletions) AS num_deletions , SUM(changes) AS num_changes, unnest(repo_id) AS cr,"
                    + " array_cat_agg(cjissues) AS cjissues, array_cat_agg(cworkitems) AS cworkitems FROM "
                    + commitsSQL
                    + commitJiraIssuesMappingTableJoin
                    + commitWorkitemsMappingTableJoin
                    + commitsWhere
                    + " GROUP BY unnest(repo_id) ), pulls AS ( SELECT COUNT(*) AS num_prs,unnest(repo_id) AS repo,"
                    + " array_cat_agg(prjissues) AS prjissues,array_cat_agg(prworkitems) AS prworkitems FROM "
                    + prsSQL
                    + prJiraIssuesMappingTableJoin
                    + prWorkitemsMappingTableJoin
                    + prsWhere
                    + " GROUP BY unnest(repo_id ) ) SELECT num_commits,num_additions,num_deletions,"
                    + "num_changes,cr,repo,num_prs,cjissues,cworkitems,prjissues,prworkitems FROM ("
                    + " SELECT * FROM comms FULL OUTER JOIN pulls ON pulls.repo = comms.cr )"
                    + " x" + reposWhere + " ) a";
        }
        String integIdCondition = "";
        if(CollectionUtils.isNotEmpty(scmReposFilter.getIntegrationIds())) {
            List<Integer> integsList = scmReposFilter.getIntegrationIds().stream()
                    .map(NumberUtils::toInt).collect(Collectors.toList());
            integIdCondition = " AND scm_files.integration_id IN (" + StringUtils.join(integsList, ",") + ")";
        }
        String fileTableJoinStmt = "WITH commit_files AS ( Select commit_sha,filetype AS file_type FROM " + company + ".scm_files scm_files, " +
                company + ".scm_file_commits scm_file_commits WHERE scm_file_commits.file_id = scm_files.id " +
                integIdCondition + " GROUP BY commit_sha, file_type)";
        commitsSelect = ScmQueryUtils.SCM_COMMITS_SELECT + ScmQueryUtils.AUTHORS_SELECT + ScmQueryUtils.COMMITTERS_SELECT;
        prsSelect = ScmQueryUtils.PRS_SELECT;
        String filesSelect = " ,commit_files.file_type ";
        String scmCommitFileTypeJoin = ScmQueryUtils.sqlForFileTableJoin(false);
        String scmPRFileTypeJoin = ScmQueryUtils.sqlForFileTableJoin(true);
        String commitsSQL = "(" + fileTableJoinStmt +  "SELECT  " + commitsSelect + filesSelect + " FROM " + company + "." + COMMITS_TABLE + commitAuthorTableJoin +
                commitCommitterTableJoin+ scmCommitFileTypeJoin + ") scm_commits ";
        String prsSQL = "(" + fileTableJoinStmt +" SELECT  " + prsSelect + creatorsSelect  + filesSelect + " FROM " + company + "." + PRS_TABLE +
                pullCreatorTableJoin + scmPRFileTypeJoin + ") scm_pullrequests ";
        return "SELECT * FROM ( WITH comms AS ( SELECT COUNT(*) AS num_commits, SUM(additions) AS num_additions,"
                + " SUM(deletions) AS num_deletions , SUM(changes) AS num_changes, file_type as file_type,"
                + " array_cat_agg(cjissues) AS cjissues, array_cat_agg(cworkitems) AS cworkitems FROM "
                + commitsSQL
                + commitJiraIssuesMappingTableJoin
                + commitWorkitemsMappingTableJoin
                + commitsWhere
                + " GROUP BY file_type ), pulls AS ( SELECT COUNT(*) AS num_prs,file_type as prsfile,"
                + " array_cat_agg(prjissues) AS prjissues,array_cat_agg(prworkitems) AS prworkitems FROM "
                + prsSQL
                + prJiraIssuesMappingTableJoin
                + prWorkitemsMappingTableJoin
                + prsWhere
                + " GROUP BY file_type ) SELECT file_type,num_commits,num_additions,num_deletions,num_changes,num_prs,"
                + " cjissues,cworkitems,prjissues,prworkitems FROM ("
                + " SELECT * FROM comms FULL OUTER JOIN pulls ON pulls.prsfile = comms.file_type )"
                + " x" + reposWhere + " ) a";
    }
}
