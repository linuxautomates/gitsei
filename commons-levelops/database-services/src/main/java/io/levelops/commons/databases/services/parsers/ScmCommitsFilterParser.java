package io.levelops.commons.databases.services.parsers;


import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.services.ScmQueryUtils;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.VCS_TYPE.parseFromFilter;
import static io.levelops.commons.databases.services.ScmQueryUtils.*;
import static io.levelops.commons.databases.services.ScmQueryUtils.getFilesChangeSql;
import static io.levelops.commons.databases.services.parsers.ScmFilterUtils.getListOrDefault;

public class ScmCommitsFilterParser {

    private static final String COMMITS_TABLE = "scm_commits";

    @SuppressWarnings("unchecked")
    public ScmCommitFilter merge(Integer integrationId, ScmCommitFilter reqFilter, Map<String, Object> productFilter) {
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) productFilter.get("partial_match"));
        Map<String, Object> excludedFields = (Map<String, Object>) productFilter.getOrDefault("exclude", Map.of());
        Map<String, Map<String, String>> excludePartialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) excludedFields.get("partial_match"));
        return ScmCommitFilter.builder()
                .committedAtRange(reqFilter.getCommittedAtRange())
                .createdAtRange(reqFilter.getCreatedAtRange())
                .calculation(reqFilter.getCalculation())
                .across(reqFilter.getAcross())
                .committers(getListOrDefault(productFilter, "committers"))
                .fileTypes(getListOrDefault(productFilter, "file_types"))
                .repoIds(getListOrDefault(productFilter, "repo_ids"))
                .vcsTypes(parseFromFilter(productFilter))
                .projects(getListOrDefault(productFilter, "projects"))
                .authors(getListOrDefault(productFilter, "authors"))
                .commitShas(getListOrDefault(productFilter, "commit_shas"))
                .excludeCommitters(getListOrDefault(excludedFields, "committers"))
                .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                .excludeFileTypes(getListOrDefault(productFilter, "file_types"))
                .excludeProjects(getListOrDefault(excludedFields, "projects"))
                .excludeAuthors(getListOrDefault(excludedFields, "authors"))
                .excludeCommitShas(getListOrDefault(excludedFields, "commit_shas"))
                .integrationIds(List.of(String.valueOf(integrationId)))
                .partialMatch(partialMatchMap)
                .excludePartialMatch(excludePartialMatchMap)
                .build();
    }

    public String getSqlStmt(String company, Map<String, List<String>> conditions, boolean isListQuery,
                             ScmCommitFilter scmCommitFilter, int paramSuffix) {
        String commitsWhere = "";
        String sql;
        String authorTableJoin = ScmQueryUtils.sqlForAuthorTableJoin(company);
        String committerTableJoin = ScmQueryUtils.sqlForCommitterTableJoin(company);
        String commitsSelect = ScmQueryUtils.SCM_COMMITS_SELECT
                + ",committer, committer_id::varchar as committer_id"
                + ",author, author_id::varchar as author_id";
        String intervalColumn = "";
        String integIdCondition = "";
        AggTimeQueryHelper.AggTimeQuery commitModAggQuery;
        AGG_INTERVAL aggInterval = scmCommitFilter.getAggInterval() == null ? AGG_INTERVAL.day : scmCommitFilter.getAggInterval();
        if (conditions.get(COMMITS_TABLE).size() > 0)
            commitsWhere = " WHERE " + String.join(" AND ", conditions.get(COMMITS_TABLE));
        if(CollectionUtils.isNotEmpty(scmCommitFilter.getIntegrationIds())) {
            List<Integer> integsList = scmCommitFilter.getIntegrationIds().stream()
                    .map(NumberUtils::toInt).collect(Collectors.toList());
            integIdCondition = " AND scm_files.integration_id IN (" + StringUtils.join(integsList, ",") + ")";
        }
        String fileTableJoinStmt = getScmCommitMetricsQuery(company, integIdCondition);
        String codeChangeSelect = getCodeChangeSql(scmCommitFilter.getCodeChangeSizeConfig(), false,scmCommitFilter.getCodeChangeUnit()) + getFilesChangeSql(scmCommitFilter.getCodeChangeSizeConfig());
        if (isListQuery)
            return  " SELECT * FROM ( SELECT "  + commitsSelect +
                    " ,scm_commits.file_types,tot_addition,tot_deletion,tot_change,pct_legacy_refactored_lines,pct_refactored_lines,pct_new_lines, total_legacy_code_lines, total_refactored_code_lines" +
                    codeChangeSelect + " FROM " + company + "." + COMMITS_TABLE +
                    fileTableJoinStmt + " ) fin " + commitsWhere;
        else {
            fileTableJoinStmt = " LEFT JOIN ( Select commit_sha,filetype AS file_type,addition,addition+deletion+change as lines_changed," +
                    "deletion,change,previous_committed_at,file_id,integration_id AS file_integ_id FROM " + company + ".scm_files scm_files INNER JOIN " +
                    company + ".scm_file_commits scm_file_commits ON scm_file_commits.file_id = scm_files.id " +
                    integIdCondition + " ) commit_files on commit_files.commit_sha = scm_commits.commit_sha AND commit_files.file_integ_id = scm_commits.integration_id";
            ScmCommitFilter.DISTINCT DISTINCT = scmCommitFilter.getAcross();
            switch (DISTINCT) {
                case trend:
                    commitModAggQuery = AggTimeQueryHelper.getAggTimeQuery
                            ("committed_at", DISTINCT.toString(), aggInterval.toString(), false);
                    intervalColumn = commitModAggQuery.getHelperColumn().replaceFirst(",", "") + ",";
                    break;
                default:
            }
            if (CollectionUtils.isNotEmpty(scmCommitFilter.getRepoIds())) {
                if (commitsWhere.equals(""))
                    commitsWhere = " WHERE repo_ids IN (:repo_ids_" + paramSuffix + ") ";
                else
                    commitsWhere = commitsWhere + " AND repo_ids IN (:repo_ids_" + paramSuffix + ") ";
            }
            String fileCommitsSelect = getFileCommitsSelect();
            fileCommitsSelect += getCodeChangeSql(scmCommitFilter.getCodeChangeSizeConfig(), false,scmCommitFilter.getCodeChangeUnit());
            fileCommitsSelect += getFilesChangeSql(scmCommitFilter.getCodeChangeSizeConfig());
            sql = "SELECT * FROM ( SELECT " + intervalColumn + " unnest(scm_commits.repo_id) as repo_ids, " + commitsSelect
                    + fileCommitsSelect + " FROM " + company + "."
                    + COMMITS_TABLE + authorTableJoin + committerTableJoin + fileTableJoinStmt  + " ) a " + commitsWhere;
            if (scmCommitFilter.getAcross().equals(ScmCommitFilter.DISTINCT.technology))
                sql = "SELECT * FROM ( SELECT * FROM ( SELECT unnest(repo_id) as repo_ids, " + commitsSelect
                        + " FROM " + company + "." + COMMITS_TABLE + authorTableJoin + committerTableJoin
                        + " ) c INNER JOIN ("
                        + " SELECT name as technology,repo_id as tr_id,integration_id as ti_id FROM "
                        + company + ".gittechnologies ) x ON x.tr_id = c.repo_ids AND c.integration_id = x.ti_id"
                        + " ) a" + commitsWhere;
        }
        return sql;
    }
}
