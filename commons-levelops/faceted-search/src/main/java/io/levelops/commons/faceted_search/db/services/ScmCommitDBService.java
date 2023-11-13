package io.levelops.commons.faceted_search.db.services;

import io.levelops.commons.databases.converters.DbScmConverters;
import io.levelops.commons.databases.models.database.GitTechnology;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFileCommitDetails;
import io.levelops.commons.databases.models.database.scm.DbScmTag;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.faceted_search.db.converters.ScmPRConverter;
import io.levelops.commons.faceted_search.db.models.ScmCommitterPrDetails;
import io.levelops.commons.faceted_search.db.models.ScmPROrCommitJiraWIMapping;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.converters.DbScmConverters.getIssues;
import static io.levelops.commons.databases.services.ScmAggService.COMMITS_TABLE;
import static io.levelops.commons.databases.services.ScmAggService.COMMIT_JIRA_TABLE;
import static io.levelops.commons.databases.services.ScmAggService.COMMIT_WORKITEM_TABLE;
import static io.levelops.commons.databases.services.ScmQueryUtils.getSqlForCommitJiraMappingTable;
import static io.levelops.commons.databases.services.ScmQueryUtils.getSqlForCommitWorkItemMappingTable;
import static io.levelops.commons.databases.services.ScmQueryUtils.getSqlForPRJiraMappingTable;
import static io.levelops.commons.databases.services.ScmQueryUtils.getSqlForPRWorkItemMappingTable;

@Log4j2
@Service
public class ScmCommitDBService {
    private static final Integer CHUNK_SIZE = 1000;
    private static final String COMMITS_LIST_BY_JIRA_ISSUE_KEY = "SELECT DISTINCT(c.id) as id, c.committed_at, ARRAY_AGG(issue_key) as workitem_ids FROM %s." + COMMITS_TABLE + " AS c JOIN %s." + COMMIT_JIRA_TABLE + " AS m ON c.integration_id=m.scm_integ_id AND c.commit_sha=m.commit_sha ";
    private static final String COMMITS_LIST_BY_WORKITEM_ID = "SELECT DISTINCT(c.id) as id, c.committed_at, ARRAY_AGG(workitem_id) as workitem_ids FROM %s." + COMMITS_TABLE + " AS c JOIN %s." + COMMIT_WORKITEM_TABLE + " AS m ON c.integration_id=m.scm_integration_id AND c.commit_sha=m.commit_sha ";
    private static final String COMMITS_LIST_BY_ID = "SELECT * FROM %s." + COMMITS_TABLE + " AS c WHERE id in (:ids) ORDER BY c.committed_at";

    private static final String TAG_COMMITS_LIST = "SELECT * FROM %s.scm_tags AS st JOIN %s." + COMMITS_TABLE + " AS c ON c.integration_id = st.integration_id AND c.commit_sha = st.commit_sha AND c.commit_sha IN (:commit_sha) ORDER BY c.commit_sha";
    private static final String FILE_COMMITS_LIST = "SELECT fc.*, sf.integration_id, sf.project, sf.repo_id, " +
            " sf.filename, sf.filetype, sf.created_at as file_created_at  FROM %s.scm_file_commits AS fc " +
            " INNER JOIN %s.scm_files sf on fc.file_id = sf.id "+
            " INNER JOIN %s.scm_commits sc ON fc.commit_sha = sc.commit_sha and sf.integration_id = sc.integration_id "+
            " WHERE fc.commit_sha IN (:commit_sha) ORDER BY fc.commit_sha";

    private static final String SCM_TECH_LIST = "SELECT * FROM %s.gittechnologies AS gt WHERE gt.repo_id IN (:repo_id) ORDER BY gt.repo_id";

    private final NamedParameterJdbcTemplate template;
    private final ScmAggService scmAggService;
    private final ScmPRDBService scmPRDBService;

    @Autowired
    public ScmCommitDBService(final DataSource dataSource, ScmAggService scmAggService, ScmPRDBService scmPRDBService) {
        //super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        this.scmAggService = scmAggService;
        this.scmPRDBService = scmPRDBService;
    }

    private List<DbScmCommit> getByIds(final String company, List<UUID> commitIds) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ids", commitIds);
        String selectSql = String.format(COMMITS_LIST_BY_ID, company);
        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbScmCommit> commits = template.query(selectSql, params, DbScmConverters.commitRowMapper());
        return commits;
    }

    private Map<UUID, DbScmCommit> fetchCommitsByIds(String company, List<UUID> commitIds) throws SQLException {
        Map<UUID, DbScmCommit> commitsMap = new HashMap<>();
        for (List<UUID> currentPRIds : ListUtils.partition(commitIds, CHUNK_SIZE)) {
            List<DbScmCommit> scmCommits = getByIds(company, currentPRIds);
            CollectionUtils.emptyIfNull(scmCommits).stream()
                    .forEach(c -> commitsMap.put(UUID.fromString(c.getId()), c));
        }
        return commitsMap;
    }

    private Map<String, List<DbScmCommit>> listByJiraIssuesOrWorkItems(String company, Set<String> jiraIssueKeysOrWorkItemIds, Set<String> commitShas, boolean jiraIssues) throws SQLException {

        List<ScmPROrCommitJiraWIMapping> scmCommitJiraWIMappings = getScmCommitJiraWIMappings(company, jiraIssueKeysOrWorkItemIds, commitShas, jiraIssues);
        List<UUID> commitUUIDs = CollectionUtils.emptyIfNull(scmCommitJiraWIMappings).stream().map(ScmPROrCommitJiraWIMapping::getPrOrCommitId).collect(Collectors.toList());
        Map<UUID, DbScmCommit> commitsMap = fetchCommitsByIds(company, commitUUIDs);

        Map<String, List<DbScmCommit>> issueKeyToCommitIdMap = new HashMap<>();
        CollectionUtils.emptyIfNull(scmCommitJiraWIMappings).stream()
                .filter(x -> commitsMap.containsKey(x.getPrOrCommitId()))
                .forEach(m -> {
                    CollectionUtils.emptyIfNull(m.getWorkItemIds()).stream()
                            .forEach(issueKey -> {
                                issueKeyToCommitIdMap.computeIfAbsent(issueKey, k -> new ArrayList<>()).add(commitsMap.get(m.getPrOrCommitId()));
                                Collections.sort(issueKeyToCommitIdMap.get(issueKey), (a, b) -> (int) (a.getCommittedAt() - b.getCommittedAt()));
                            });
                });

        return issueKeyToCommitIdMap;
    }

    public List<ScmPROrCommitJiraWIMapping> getScmCommitJiraWIMappings(String company, Set<String> jiraIssueKeysOrWorkItemIds, Set<String> commitShas, boolean jiraIssues) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        String groupBy = " GROUP BY c.id ";
        String orderBy = " ORDER BY c.committed_at";
        if (CollectionUtils.isNotEmpty(jiraIssueKeysOrWorkItemIds)) {
            if (jiraIssues)
                conditions.add("m.issue_key in (:workitem_ids)");
            else
                conditions.add("m.workitem_id in (:workitem_ids)");
            params.addValue("workitem_ids", jiraIssueKeysOrWorkItemIds);
        }
        if (CollectionUtils.isNotEmpty(commitShas)) {
            conditions.add("c.commit_sha in (:commit_shas)");
            params.addValue("commit_shas", commitShas);
        }
        String whereClause = "";
        if (CollectionUtils.isNotEmpty(conditions))
            whereClause = " WHERE " + String.join(" OR ", conditions);

        String selectSql = String.format((jiraIssues) ? COMMITS_LIST_BY_JIRA_ISSUE_KEY : COMMITS_LIST_BY_WORKITEM_ID, company, company)
                + whereClause
                + groupBy
                + orderBy;

        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        return template.query(selectSql, params, ScmPRConverter.prJiraWIMapper());
    }

    public Map<String, List<DbScmCommit>> listByJiraTickets(String company, Set<String> jiraIssueKeys, Set<String> commitShas) throws SQLException {
        return listByJiraIssuesOrWorkItems(company, jiraIssueKeys, commitShas, true);
    }

    public Map<String, List<DbScmCommit>> listByWorkItems(String company, Set<String> workItemIds, Set<String> commitShas) throws SQLException {
        return listByJiraIssuesOrWorkItems(company, workItemIds, commitShas, false);
    }

    public Map<String, DbScmCommit> listCommit(String company, List<String> commitShaList) {

        List<DbScmCommit> list = listCommit(company, ScmCommitFilter.builder().build(), commitShaList,0, 10000);
        Map<String, DbScmCommit> commitMap = new HashMap<>();
        CollectionUtils.emptyIfNull(list).stream()
                .forEach(c -> {
                    commitMap.put(c.getCommitSha(), c);
                });

        return commitMap;
    }

    public List<DbScmCommit> listCommit(String company, ScmCommitFilter filter, List<String> commitShaList, int pageNumber, Integer pageSize) {
        String query = "SELECT * FROM " + company + "." + COMMITS_TABLE;
        String limit = " offset :offset limit :limit";
        String whereClause = "";

        List<String> commitsTableConditions = new ArrayList<>();
        ImmutablePair<Long, Long> committedAtRange = filter.getCommittedAtRange();
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("offset", pageNumber * pageSize);
        params.addValue("limit", pageSize);

        if (committedAtRange != null) {
            if (committedAtRange.getLeft() != null) {
                commitsTableConditions.add("committed_at > TO_TIMESTAMP(" + committedAtRange.getLeft() + ")");
            }
            if (committedAtRange.getRight() != null) {
                commitsTableConditions.add("committed_at < TO_TIMESTAMP(" + committedAtRange.getRight() + ")");
            }
        }

        if (CollectionUtils.isNotEmpty(commitShaList)) {
            commitsTableConditions.add("commit_sha IN (:commit_sha)");
            params.addValue("commit_sha", commitShaList);
        }

        if (CollectionUtils.isNotEmpty(commitsTableConditions)) {
            whereClause = " WHERE " + String.join(" AND ", commitsTableConditions);
        }

        String selectSql = query + whereClause + limit;

        log.info("commits sql = " + selectSql);
        log.info("commits params = {}", params);

        return template.query(selectSql, params, DbScmConverters.commitESRowMapper());
    }

    public Map<String, List<DbScmTag>> getScmTags(String company, List<DbScmCommit> commitList) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("commit_sha", commitList.stream().map(c -> c.getCommitSha()).collect(Collectors.toList()));
        String selectSql = String.format(TAG_COMMITS_LIST, company, company);

        log.info("tags sql = " + selectSql);
        log.info("tags params = {}", params);

        List<DbScmTag> filesList = template.query(selectSql, params, DbScmConverters.mapScmTag());
        Map<String, List<DbScmTag>> commitScmTags = new HashMap<>();
        CollectionUtils.emptyIfNull(filesList).stream()
                .forEach(c -> {
                    List<DbScmTag> list = commitScmTags.getOrDefault(c.getCommitSha(), new ArrayList<DbScmTag>());
                    list.add(c);
                    commitScmTags.put(c.getCommitSha(), list);
                });

        return commitScmTags;
    }

    public Map<String, List<DbScmFileCommitDetails>> getFileCommits(String company, List<DbScmCommit> commitList) {

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("commit_sha", commitList.stream().map(c -> c.getCommitSha()).collect(Collectors.toList()));
        String selectSql = String.format(FILE_COMMITS_LIST, company, company, company);

        log.info("file commit sql = " + selectSql);

        List<DbScmFileCommitDetails> filesList = template.query(selectSql, params, DbScmConverters.filesCommitDetailsRowMapper());
        Map<String, List<DbScmFileCommitDetails>> fileCommitsMap = new HashMap<>();
        CollectionUtils.emptyIfNull(filesList).stream()
                .forEach(c -> {
                    List<DbScmFileCommitDetails> list = fileCommitsMap.getOrDefault(c.getCommitSha()+"_"+c.getIntegrationId(), new ArrayList<DbScmFileCommitDetails>());
                    list.add(c);
                    fileCommitsMap.put(c.getCommitSha()+"_"+c.getIntegrationId(), list);
                });
        return fileCommitsMap;
    }


    public Map<String, List<GitTechnology>> getScmTechnologies(String company, List<String> repoIds) {

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("repo_id",repoIds);
        String selectSql = String.format(SCM_TECH_LIST, company);

        log.info("technology sql = " + selectSql);

        List<GitTechnology> technologyList = template.query(selectSql, params, DbScmConverters.mapScmTech());
        Map<String, List<GitTechnology>> techMap = new HashMap<>();
        CollectionUtils.emptyIfNull(technologyList).stream()
                .forEach(c -> {
                    List<GitTechnology> list = techMap.getOrDefault(c.getRepoId()+"_"+c.getIntegrationId(), new ArrayList<GitTechnology>());
                    list.add(c);
                    techMap.put(c.getRepoId()+"_"+c.getIntegrationId(), list);
                });
        return techMap;
    }

    public List<ScmPROrCommitJiraWIMapping> getWorkItemsForCommits(String company, Set<String> jiraIssueKeysOrWorkItemIds, Set<String> commitShas) {

        List<ScmPROrCommitJiraWIMapping> jiraMappings = getScmCommitJiraWIMappings(company, jiraIssueKeysOrWorkItemIds, commitShas, true);
        List<ScmPROrCommitJiraWIMapping> wiMappings = getScmCommitJiraWIMappings(company, jiraIssueKeysOrWorkItemIds, commitShas, false);
        wiMappings.addAll(jiraMappings);
        return wiMappings;
    }

    public Map<String, List<String>> getWorkItemsFromPrs(String company, List<String> commitList){

        Map<String, List<String>> result = new HashMap<>();
        Map<String, Set<String>> prCommitMap = getPRList(company, commitList);
        List<ScmPROrCommitJiraWIMapping> jiraMappings = scmPRDBService.getScmPRJiraWIMappings(company, null, null, prCommitMap.keySet(), true);
        List<ScmPROrCommitJiraWIMapping> wiMappings = scmPRDBService.getScmPRJiraWIMappings(company, null, null, prCommitMap.keySet(), false);
        wiMappings.addAll(jiraMappings);

        wiMappings.forEach( wi -> {
            if(prCommitMap.containsKey(wi.getPrOrCommitId().toString())){
                prCommitMap.get(wi.getPrOrCommitId().toString()).forEach( c -> {
                    result.put(c, wi.getWorkItemIds());
                });
            }
        });

        return result;
    }

    private Map<String, Set<String>> getPRList(String company, List<String> commitList) {

        String sql = "select scm_commit_id, scm_pullrequest_id from "+company+".scm_commit_pullrequest_mappings "+
                "where scm_commit_id in (:commit_ids)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("commit_ids",commitList.stream().map(c -> UUID.fromString(c)).collect(Collectors.toList()));
        log.info(" pr id sql = " + sql);

        Map<String, Set<String>> prMap = new HashMap<>();
        template.query(sql, params, (ResultSetExtractor<Void>) rs -> {
            while (rs.next()){
                String commit = rs.getString("scm_commit_id");
                String pr = rs.getString("scm_pullrequest_id");
                Set<String> list = prMap.getOrDefault(pr, new HashSet<String>());
                list.add(commit);
                prMap.put(pr, list);
            }
            return null;
        });

        return prMap;
    }

    public Map<String, Set<String>> getCommitPRMap(String company, List<String> commitList) {

        String sql = "select scm_commit_id, scm_pullrequest_id from "+company+".scm_commit_pullrequest_mappings "+
                "where scm_commit_id in (:commit_ids)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("commit_ids",commitList.stream().map(c -> UUID.fromString(c)).collect(Collectors.toList()));
        log.info("commit pr map sql = " + sql);

        Map<String, Set<String>> commitMap = new HashMap<>();
        template.query(sql, params, (ResultSetExtractor<Void>) rs -> {
            while (rs.next()){
                String commit = rs.getString("scm_commit_id");
                String pr = rs.getString("scm_pullrequest_id");
                Set<String> list = commitMap.getOrDefault(commit, new HashSet<String>());
                list.add(pr);
                commitMap.put(commit, list);
            }
            return null;
        });

        return commitMap;
    }

    public Map<String, ScmCommitterPrDetails> getCommitterPRMap(String company, List<String> commitList) {

        String sql = "SELECT scm_commits.id AS commit_id,\n" +
                "       scm_pullrequests.id AS pr_id,\n" +
                "       Array_cat_agg(issue_keys)   AS prjissues,\n" +
                "       Array_cat_agg(workitem_ids) AS prworkitems,\n" +
                "       Array_cat_agg(cjissues) AS cjissues,\n" +
                "       Array_cat_agg(cworkitems) AS cworkitems\n" +
                " FROM "+company+".scm_commits INNER JOIN "+company+".scm_pullrequests \n" +
                " ON commit_sha = ANY ( commit_shas ) and scm_commits.committer_id = scm_pullrequests.creator_id  AND scm_commits.id in (:commit_ids) "
                + getSqlForPRJiraMappingTable(company) + getSqlForPRWorkItemMappingTable(company)
                + getSqlForCommitJiraMappingTable(company) + getSqlForCommitWorkItemMappingTable(company)
                + " GROUP  BY scm_commits.id, scm_pullrequests.id ORDER BY scm_commits.id";

        Map<String, ScmCommitterPrDetails> map = new HashMap<>();
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("commit_ids",commitList.stream().map(c -> UUID.fromString(c)).collect(Collectors.toList()));
        log.info("committer pr map sql = " + sql);
        template.query(sql, params, (ResultSetExtractor<Void>) rs -> {
            while (rs.next()){

                List<String> workitems = getIssues(rs, "cworkitems", "prworkitems");
                List<String> jiraIssues = getIssues(rs, "cjissues", "prjissues");
                String commit = rs.getString("commit_id");
                String pr = rs.getString("pr_id");
                ScmCommitterPrDetails prDetails;
                if(map.containsKey(commit)){
                    prDetails = map.get(commit);
                    List<String> prs = prDetails.getPrIds();
                    List<String> jiras = prDetails.getJiraIssues();
                    List<String> wis = prDetails.getWorkItems();
                    prs.add(pr);
                    jiras.addAll(jiraIssues);
                    wis.addAll(workitems);
                    prDetails = prDetails.toBuilder()
                            .prIds(prs)
                            .workItems(wis)
                            .jiraIssues(jiras)
                            .build();
                }else{
                    List<String> prs = new ArrayList<>();
                    prs.add(pr);
                    prDetails = ScmCommitterPrDetails.builder()
                            .commitId(commit)
                            .prIds(prs)
                            .workItems(workitems)
                            .jiraIssues(jiraIssues)
                            .build();
                }
              map.put(commit, prDetails);
            }
            return null;
        });

        return map;
    }

    public Map<String, DbScmUser> getIntegrationUsers(String company, List<UUID> userIdList) {

        String sql = "SELECT * FROM " + company + ".integration_users";
        String whereClause = CollectionUtils.isEmpty(userIdList) ? StringUtils.EMPTY : " WHERE id in (:userIdList) ";
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (CollectionUtils.isNotEmpty(userIdList)) {
            params.addValue("userIdList", userIdList);
        }

        sql += whereClause;

        log.info("integration user sql = " + sql);
        log.info("integration user params = " + params);
        List<DbScmUser> list = template.query(sql, params, DbScmConverters.userRowMapper());

        Map<String, DbScmUser> map = new HashMap<>();
        CollectionUtils.emptyIfNull(list).stream()
                .forEach(c -> map.put(c.getId(), c));

        return map;
    }
}
