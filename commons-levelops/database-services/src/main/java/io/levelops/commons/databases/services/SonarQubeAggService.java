package io.levelops.commons.databases.services;


import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.DBSonarAggConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.SonarQubePrFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
public class SonarQubeAggService {

    public static final String SONAR_PULL_REQUESTS = "sonarqube_project_pull_requests";
    public static final String SONAR_PROJECTS = "sonarqube_projects";
    public static final String SCM_PULL_REQUESTS = "scm_pullrequests";
    public static final String SCM_COMMITS = "scm_commits";
    public static final String FINAL_TABLE_CONDITION = "final_table_condition";

    private static final Set<String> SORTABLE_COLUMNS = Set.of("created_at", "committed_at", "lines_added", "lines_deleted", "lines_changed", "bugs", "code_smells", "vulnerabilities");

    private final DataSource dataSource;
    private NamedParameterJdbcTemplate template;

    @Autowired
    public SonarQubeAggService(DataSource dataSource){
        this.dataSource = dataSource;
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculatePrs(String company, SonarQubePrFilter filter) throws SQLException {

        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        SonarQubePrFilter.CALCULATION calculation = filter.getCalculation();
        Map<String, Object> params = new HashMap<>();
        String filterByProductSQL = "";
        String calculationComponent;
        String finalWhere = "";
        String intervalColumn = "";
        String groupByString= " GROUP BY ";
        String selectDistinctString ="";
        boolean sortAscending = true;
        SonarQubePrFilter.DISTINCT DISTINCT = filter.getAcross();
        Map<String, List<String>> conditions;

        if (calculation == null)
            calculation = SonarQubePrFilter.CALCULATION.count;

        conditions = createPrWhereClauseAndUpdateParams(company, params, filter, null, "");

        switch (calculation) {
            case count:
                calculationComponent = ",COUNT(*) as ct," +
                        "Sum(bugs) AS bugs," +
                        "Sum(vulnerabilities) as vulnerabilities," +
                        "Sum(code_smells) as code_smells, " +
                        "sum(lines_added) as lines_added,sum(lines_deleted) as lines_deleted," +
                        "sum(lines_changed) as lines_changed,ROUND(avg(lines_changed),5) as avg_lines_changed," +
                        "sum(files_ct) as total_files_changed,ROUND(avg(files_ct),5)  as avg_files_changed," +
                        "PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY lines_changed) AS median_lines_changed,"
                        + "PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY files_ct) AS median_files_changed";
                break;
            default:
                throw new SQLException("Invalid calculation field provided for this agg.");
        }

        switch (DISTINCT){
            case committed_at:
            case pr_created:
                String columnName = "unnest ( scm_pr_created_at )";
                if(SonarQubePrFilter.DISTINCT.committed_at.equals(DISTINCT))
                    columnName = "unnest ( scm_committed_at )";

                AggTimeQueryHelper.AggTimeQuery ticketModAggQuery = AggTimeQueryHelper.getAggTimeQuery
                        (columnName, DISTINCT.toString(), filter.getAggInterval() != null ?
                                filter.getAggInterval().toString() : null, false, sortAscending);
                selectDistinctString = ticketModAggQuery.getSelect();
                intervalColumn = ticketModAggQuery.getHelperColumn().replaceFirst(",", "") + ",";
                groupByString += ticketModAggQuery.getGroupBy();
                break;

            default:
                Validate.notNull(null, "Invalid across field provided.");
                return DbListResponse.of(List.of(), 0);
        }

        if(conditions.get(FINAL_TABLE_CONDITION).size()>0 ) {
            finalWhere = " WHERE " + String.join(" AND ", conditions.get(FINAL_TABLE_CONDITION));
        }

        String sonarPrAnd = "";
        if( conditions.get(SONAR_PULL_REQUESTS).size()>0  ) {
            sonarPrAnd = " AND " + String.join(" AND ", conditions.get(SONAR_PULL_REQUESTS));
        }
        String sonarProjectAnd = "";
        if(conditions.get(SONAR_PROJECTS).size() > 0)
            sonarProjectAnd = " AND " + String.join(" AND ", conditions.get(SONAR_PROJECTS));


        String scmPrAnd = "";
        if( conditions.get(SCM_PULL_REQUESTS).size()>0  ) {
            scmPrAnd =  " AND " +String.join(" AND ", conditions.get(SCM_PULL_REQUESTS));
        }

        String joinIssues = " INNER JOIN ( SELECT pr.id, COALESCE(sum(bugs),0) AS bugs, COALESCE(sum(vulnerabilities),0) AS vulnerabilities, " +
                "COALESCE(sum(code_smells),0)  AS code_smells" +
                " FROM "+company+"."+SONAR_PULL_REQUESTS + " sonar_pr"+
                " INNER JOIN "+company+"."+SONAR_PROJECTS+ " sp"+
                " ON sp.id = sonar_pr.project_id"+
                 sonarProjectAnd +
                " LEFT JOIN "+company+"."+SCM_PULL_REQUESTS+" pr"+
                " ON pr.source_branch= sonar_pr.branch" +
                " AND pr.number = sonar_pr.key " +
                 sonarPrAnd +
                 scmPrAnd +
                " GROUP BY  pr.id) AS sonar_pr" +
                " ON pr.id = sonar_pr.id";

        String joinCommits = " LEFT JOIN (" +
                " SELECT pr.id," +
                " committer, committer_id, sc.created_at as commit_created_at,"+
                " COALESCE(sum(additions),0) AS lines_added," +
                " COALESCE(sum(deletions),0) AS lines_deleted," +
                " COALESCE(sum(changes),0)   AS lines_changed," +
                " COALESCE(sum(files_ct),0)  AS files_ct" +
                " FROM "+company+"."+SCM_PULL_REQUESTS+" pr" +
                " LEFT JOIN "+company+"."+SCM_COMMITS+" sc" +
                " ON sc.commit_sha = ANY(pr.commit_shas)" +
                " AND pr.integration_id = sc.integration_id " +
                 scmPrAnd +
                " GROUP BY  pr.id, committer, committer_id, sc.created_at) AS pr_scm_commits" +
                " ON pr.id = pr_scm_commits.id";

        String joinPrCreated = " LEFT JOIN ( SELECT array_agg(pr_created_at) AS scm_pr_created_at, id " +
                " FROM "+company+"."+SCM_PULL_REQUESTS +
                " GROUP BY id ) AS spr " +
                " ON pr.id = spr.id ";

        String joinCommitCreated = " LEFT JOIN ( SELECT array_agg(committed_at) AS scm_committed_at, commit_sha " +
                " FROM "+company+"."+SCM_COMMITS +
                " GROUP BY commit_sha ) AS scm " +
                " ON scm.commit_sha = ANY ( pr.commit_shas ) ) a";

        filterByProductSQL = "SELECT * FROM ( SELECT "+intervalColumn + " creator, creator_id, pr.created_at, committer, committer_id, commit_created_at, bugs, code_smells,  vulnerabilities, lines_added, lines_deleted, lines_changed, files_ct " +
                " FROM " + company+ "." +SCM_PULL_REQUESTS + " pr "
                + joinIssues
                + joinCommits
                + joinPrCreated
                + joinCommitCreated
                + finalWhere;

        String query =  "SELECT "+ selectDistinctString + calculationComponent
                + " FROM ("+filterByProductSQL+") y "
                + groupByString;

        log.info("query is {}", query);
        log.info("params {}", params);

        List<DbAggregationResult> results = template.query(query, params, DBSonarAggConverters.rowMapper());

        return DbListResponse.of(results, results.size());
    }

    public DbListResponse<DbAggregationResult> list(String company, SonarQubePrFilter filter, Map<String, SortingOrder> sortBy,
                                                    Integer pageNumber,
                                                    Integer pageSize) throws SQLException {

        sortBy = MapUtils.emptyIfNull(sortBy);
        pageNumber = MoreObjects.firstNonNull(pageNumber, 0);
        pageSize = MoreObjects.firstNonNull(pageSize, 25);

        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (SORTABLE_COLUMNS.contains(entry.getKey())) {
                        return entry.getKey();
                    }
                    return "created_at";
                })
                .orElse("created_at");

        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);

        Map<String, Object> params = new HashMap<>();
        String query = "";
        String finalWhere = "";
        Map<String, List<String>> conditions;

        params.put("offset", pageNumber * pageSize);
        params.put("limit", pageSize);

        conditions = createPrWhereClauseAndUpdateParams(company, params, filter, null, "");

        if(conditions.get(FINAL_TABLE_CONDITION).size()>0 ) {
            finalWhere = " WHERE " + String.join(" AND ", conditions.get(FINAL_TABLE_CONDITION));
        }

        String sonarPrAnd = "";
        if( conditions.get(SONAR_PULL_REQUESTS).size()>0  ) {
            sonarPrAnd = " AND " + String.join(" AND ", conditions.get(SONAR_PULL_REQUESTS));
        }
        String sonarProjectAnd = "";
        if(conditions.get(SONAR_PROJECTS).size() > 0)
            sonarProjectAnd = " AND " + String.join(" AND ", conditions.get(SONAR_PROJECTS));


        String scmPrAnd = "";
        if( conditions.get(SCM_PULL_REQUESTS).size()>0  ) {
            scmPrAnd =  " AND " +String.join(" AND ", conditions.get(SCM_PULL_REQUESTS));
        }

        String joinIssues = " INNER JOIN ( SELECT pr.id, COALESCE(sum(bugs),0) AS bugs, COALESCE(sum(vulnerabilities),0) AS vulnerabilities, " +
                "COALESCE(sum(code_smells),0)  AS code_smells" +
                " FROM "+company+"."+SONAR_PULL_REQUESTS + " sonar_pr"+
                " INNER JOIN "+company+"."+SONAR_PROJECTS+ " sp"+
                " ON sp.id = sonar_pr.project_id"+
                sonarProjectAnd +
                " LEFT JOIN "+company+"."+SCM_PULL_REQUESTS+" pr"+
                " ON pr.source_branch= sonar_pr.branch" +
                " AND pr.number = sonar_pr.key " +
                sonarPrAnd +
                scmPrAnd +
                " GROUP BY  pr.id) AS sonar_pr" +
                " ON pr.id = sonar_pr.id";

        String joinCommits = " LEFT JOIN (" +
                " SELECT pr.id," +
                " committer, committer_id, sc.created_at as commit_created_at,"+
                " COALESCE(sum(additions),0) AS lines_added," +
                " COALESCE(sum(deletions),0) AS lines_deleted," +
                " COALESCE(sum(changes),0)   AS lines_changed," +
                " COALESCE(sum(files_ct),0)  AS files_ct" +
                " FROM "+company+"."+SCM_PULL_REQUESTS+" pr" +
                " LEFT JOIN "+company+"."+SCM_COMMITS+" sc" +
                " ON sc.commit_sha = ANY(pr.commit_shas)" +
                " AND pr.integration_id = sc.integration_id " +
                scmPrAnd +
                " GROUP BY  pr.id, committer, committer_id, sc.created_at) AS pr_scm_commits" +
                " ON pr.id = pr_scm_commits.id";

        String joinPrCreated = " LEFT JOIN ( SELECT array_agg(pr_created_at) AS scm_pr_created_at, id " +
                " FROM "+company+"."+SCM_PULL_REQUESTS +
                " GROUP BY id ) AS spr " +
                " ON pr.id = spr.id ";

        String joinCommitCreated = " LEFT JOIN ( SELECT array_agg(committed_at) AS scm_committed_at, commit_sha " +
                " FROM "+company+"."+SCM_COMMITS +
                " GROUP BY commit_sha ) AS scm " +
                " ON scm.commit_sha = ANY ( pr.commit_shas ) ) a";

        query = "SELECT * FROM ( SELECT creator, creator_id, pr.created_at,pr.repo_id, pr.title, pr.source_branch, pr.number, committer, committer_id, commit_created_at, bugs, code_smells,  vulnerabilities, lines_added, lines_deleted, lines_changed, files_ct " +
                " FROM " + company+ "." +SCM_PULL_REQUESTS + " pr "
                + joinIssues
                + joinCommits
                + joinPrCreated
                + joinCommitCreated
                + finalWhere
                + " ORDER BY " + sortByKey + " " + sortOrder.toString();

        List<DbAggregationResult> results = List.of();

        String sql = query + " OFFSET :offset LIMIT :limit";
        results =  template.query(sql, params, DBSonarAggConverters.rowMapperBreakDown());
        log.info("sql : {}",query);
        log.info("params : {}",params);

        String countSql = "SELECT COUNT(*) FROM (" + query + ") as x ";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    private Map<String, List<String>> createPrWhereClauseAndUpdateParams(String company, Map<String, Object> params, SonarQubePrFilter filter, String paramSuffix,
                                                                         String prTblQualifier) {

        String paramSuffixString = StringUtils.isEmpty(paramSuffix) ? "" : "_" + paramSuffix;

        List<String> sonarPrTableConditions = new ArrayList<>();
        List<String> scmPrTableConditions = new ArrayList<>();
        List<String> sonarProjectTableConditions = new ArrayList<>();
        List<String> finalTableCondition = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            scmPrTableConditions.add(prTblQualifier + "pr.integration_id in ( :integration_id" + paramSuffixString + " )");
            sonarProjectTableConditions.add(prTblQualifier + "sp.integration_id in ( :integration_id" + paramSuffixString + " )");
            params.put("integration_id" + paramSuffixString, filter.getIntegrationIds().stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getCreators())) {
            finalTableCondition.add(prTblQualifier + "creator in ( :creator" + paramSuffixString + " )");
            params.put("creator" + paramSuffixString, filter.getCreators());
        }
        if (CollectionUtils.isNotEmpty(filter.getCreatorIds())) {
            finalTableCondition.add(prTblQualifier + "creator_id in ( :creator_id" + paramSuffixString + " )");
            params.put("creator_id" + paramSuffixString, filter.getCreatorIds());
        }
        if (CollectionUtils.isNotEmpty(filter.getKeys())) {
            sonarPrTableConditions.add(prTblQualifier + "key in ( :key" + paramSuffixString + " )");
            params.put("key" + paramSuffixString, filter.getKeys());
        }
        if (CollectionUtils.isNotEmpty(filter.getBranches())) {
            sonarPrTableConditions.add(prTblQualifier + "branch in ( :branch" + paramSuffixString + " )");
            params.put("branch" + paramSuffixString, filter.getBranches());
        }
        if (CollectionUtils.isNotEmpty(filter.getTitles())) {
            sonarPrTableConditions.add(prTblQualifier + "title in ( :title" + paramSuffixString + " )");
            params.put("title" + paramSuffixString, filter.getTitles());
        }
        if (CollectionUtils.isNotEmpty(filter.getCommitter())) {
            finalTableCondition.add(prTblQualifier + "committer in ( :committer" + paramSuffixString + " )");
            params.put("committer" + paramSuffixString, filter.getCommitter());
        }
        if (CollectionUtils.isNotEmpty(filter.getCommitterIds())) {
            finalTableCondition.add(prTblQualifier + "committer_id in ( :committer_id" + paramSuffixString + " )");
            params.put("committer_id" + paramSuffixString, filter.getCommitterIds());
        }

        ImmutablePair<Long, Long> committedRange = filter.getCommitCreatedRange();
        if (committedRange != null) {
            if (committedRange.getLeft() != null) {
                finalTableCondition.add(prTblQualifier + "commit_created_at > :" + paramSuffixString +"committed_start");
                params.put("committed_start"+paramSuffixString, committedRange.getLeft());
            }
            if (committedRange.getRight() != null) {
                finalTableCondition.add(prTblQualifier + "commit_created_at < :" + paramSuffixString +"committed_end");
                params.put("committed_end"+paramSuffixString, committedRange.getRight());
            }
        }
        ImmutablePair<Long, Long> prCreatedRange = filter.getPrCreatedRange();
        if (prCreatedRange != null) {
            if (prCreatedRange.getLeft() != null) {
                finalTableCondition.add(prTblQualifier + "created_at > :" + paramSuffixString +"pr_created_start ");
                params.put("pr_created_start", prCreatedRange.getLeft());
            }
            if (prCreatedRange.getRight() != null) {
                finalTableCondition.add(prTblQualifier + "created_at < :" + paramSuffixString +"pr_created_end ");
                params.put("pr_created_end", prCreatedRange.getRight());
            }
        }
        return Map.of(SONAR_PULL_REQUESTS, sonarPrTableConditions,
                SCM_PULL_REQUESTS, scmPrTableConditions,
                SONAR_PROJECTS, sonarProjectTableConditions,
                FINAL_TABLE_CONDITION, finalTableCondition);
    }
}
