package io.levelops.commons.databases.services.parsers;

import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.services.parsers.ScmFilterUtils.getListOrDefault;

public class ScmFilesFilterParser {

    private static final String FILES_TABLE = "scm_files";
    private static final String FILE_COMMITS_TABLE = "scm_file_commits";

    public ScmFilesFilter merge(Integer integrationId, ScmFilesFilter reqFilter, Map<String, Object> productFilter) {
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) productFilter.get("partial_match"));
        Map<String, Object> excludedFields = (Map<String, Object>) productFilter.getOrDefault("exclude", Map.of());
        return ScmFilesFilter.builder()
                .across(reqFilter.getAcross())
                .calculation(reqFilter.getCalculation())
                .integrationIds(List.of(String.valueOf(integrationId)))
                .repoIds(getListOrDefault(productFilter, "repo_ids"))
                .projects(getListOrDefault(productFilter, "projects"))
                .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                .excludeProjects(getListOrDefault(excludedFields, "projects"))
                .filename((String) productFilter.getOrDefault("filename", null))
                .module((String) productFilter.getOrDefault("module", null))
                .listFiles((Boolean) productFilter.getOrDefault("list_files", true))
                .commitStartTime(reqFilter.getCommitStartTime())
                .commitEndTime(reqFilter.getCommitEndTime())
                .partialMatch(partialMatchMap)
                .build();
    }

    public String getSqlStmtForModules(String company, Map<String, List<String>> conditions, ScmFilesFilter scmFilesFilter) {
        String filesWhere = "";
        String fileCondition = "";
        String fileCommitsWhere = " WHERE file_id = files.id ";
        String path;
        if (conditions.get(FILES_TABLE).size() > 0)
            filesWhere = " WHERE " + String.join(" AND ", conditions.get(FILES_TABLE));
        if (conditions.get(FILE_COMMITS_TABLE).size() > 0)
            fileCommitsWhere = fileCommitsWhere + " AND " + String.join(" AND ", conditions.get(FILE_COMMITS_TABLE));
        path = StringUtils.isEmpty(scmFilesFilter.getModule())
                ? "filename" : "substring(filename from " + (scmFilesFilter.getModule().length() + 2) + ")";
        if (!scmFilesFilter.getListFiles()) {
            fileCondition = " where position('/' IN " + path + " ) > 0 ";
        }
        return "SELECT * FROM (" + "SELECT split_part(" + path + ", '/', 1) as root_module, repo_id, project, SUM(num_commits) " +
                "as no_of_commits FROM ( " + " SELECT files.*,"
                + "(SELECT COUNT(*) FROM " + company + ".scm_file_commits " + fileCommitsWhere + " ) AS num_commits,"
                + "(SELECT SUM(change) FROM " + company + ".scm_file_commits " + fileCommitsWhere + " ) AS changes,"
                + "(SELECT SUM(addition) FROM " + company + ".scm_file_commits " + fileCommitsWhere + " ) AS additions,"
                + "(SELECT SUM(deletion) FROM " + company + ".scm_file_commits " + fileCommitsWhere + " ) AS deletions"
                + " FROM " + company + "." + FILES_TABLE + " AS files"
                + filesWhere + " ) s_files" + fileCondition + " GROUP BY repo_id, project, root_module" + ") a ";
    }

    public String getSqlStmt(String company, Map<String, List<String>> conditions, boolean isListQuery) {
        String filesWhere = "";
        String fileCommitsWhere = "";
        if (isListQuery) {
            fileCommitsWhere = " WHERE file_id = files.id ";
            if (conditions.get(FILES_TABLE).size() > 0)
                filesWhere = " WHERE " + String.join(" AND ", conditions.get(FILES_TABLE));
            if (conditions.get(FILE_COMMITS_TABLE).size() > 0)
                fileCommitsWhere = fileCommitsWhere + " AND " + String.join(" AND ", conditions.get(FILE_COMMITS_TABLE));
            return "SELECT * FROM (SELECT files.*,"
                    + "(SELECT COUNT(*) FROM " + company + ".scm_file_commits " + fileCommitsWhere + " ) AS num_commits,"
                    + "(SELECT SUM(change) FROM " + company + ".scm_file_commits " + fileCommitsWhere + " ) AS changes,"
                    + "(SELECT SUM(addition) FROM " + company + ".scm_file_commits " + fileCommitsWhere + " ) AS additions,"
                    + "(SELECT SUM(deletion) FROM " + company + ".scm_file_commits " + fileCommitsWhere + " ) AS deletions"
                    + " FROM " + company + "." + FILES_TABLE + " AS files"
                    + filesWhere + ") a";
        }
        if (conditions.get(FILES_TABLE).size() > 0)
            filesWhere = " WHERE " + String.join(" AND ", conditions.get(FILES_TABLE));
        if (conditions.get(FILE_COMMITS_TABLE).size() > 0)
            fileCommitsWhere = " WHERE " + String.join(" AND ", conditions.get(FILE_COMMITS_TABLE));
        return "SELECT * FROM (SELECT files.id,files.repo_id,files.project,commits.* FROM "
                + company + "." + FILES_TABLE + " AS files"
                + " INNER JOIN ( SELECT COUNT(*) as num_commits,file_id FROM "
                + company + "." + FILE_COMMITS_TABLE
                + fileCommitsWhere + " GROUP BY file_id ) AS commits"
                + " ON files.id = commits.file_id "
                + filesWhere + ") a";
    }

}
