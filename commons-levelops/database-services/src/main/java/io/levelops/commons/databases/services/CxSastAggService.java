package io.levelops.commons.databases.services;

import io.levelops.commons.databases.converters.DbCxSastConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastIssue;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastProject;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastQuery;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastScan;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CxSastIssueFilter;
import io.levelops.commons.databases.models.filters.CxSastScanFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.models.DbListResponse;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class CxSastAggService extends DatabaseService<DbCxSastProject> {

    private static final String PROJECTS_TABLE = "checkmarx_sast_projects";
    private static final String SCANS_TABLE = "checkmarx_sast_scans";
    private static final String QUERIES_TABLE = "checkmarx_sast_queries";
    private static final String ISSUES_TABLE = "checkmarx_sast_issues";

    private static final Set<String> ISSUES_SORTABLE_COLUMNS = Set.of("severity", "status", "scan_id");
    private static final Set<String> SCANS_SORTABLE_COLUMNS = Set.of("scan_path", "initiator_name", "scan_type",
            "status", "scan_id", "project_name", "scan_risk");
    private static final Set<String> FILES_SORTABLE_COLUMNS = Set.of("node_id");

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public CxSastAggService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public String insert(String company, DbCxSastProject project) throws SQLException {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            final int integrationId = NumberUtils.toInt(project.getIntegrationId());
            String insertProject = "INSERT INTO " + company + "." + PROJECTS_TABLE + " (integration_id, " +
                    "project_id, team_id, name, is_public) VALUES(?,?,?,?,?) " +
                    "ON CONFLICT(project_id,team_id,integration_id) " +
                    "DO NOTHING ";
            try (PreparedStatement projectStmt = conn.prepareStatement(insertProject,
                    Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                projectStmt.setObject(i++, integrationId);
                projectStmt.setObject(i++, project.getProjectId());
                projectStmt.setObject(i++, project.getTeamId());
                projectStmt.setObject(i++, project.getName());
                projectStmt.setObject(i, project.getIsPublic());
                int affectedRows = projectStmt.executeUpdate();
                if (affectedRows > 0) {
                    // get the ID back
                    try (ResultSet rs = projectStmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            return rs.getString(1);
                        }
                    }
                }
            }
            return null;
        }));
    }

    public String insertScan(String company, DbCxSastScan scan) {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            final int integrationId = NumberUtils.toInt(scan.getIntegrationId());
            String insertScan = "INSERT INTO " + company + "." + SCANS_TABLE + " (integration_id, " +
                    " scan_id, project_id, scan_risk, status, scan_type, scan_started_at, scan_finished_at," +
                    " scan_path, languages, owner, initiator_name, is_public) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                    " ON CONFLICT(scan_id, project_id, integration_id) " +
                    " DO UPDATE SET scan_started_at=EXCLUDED.scan_started_at," +
                    " scan_finished_at=EXCLUDED.scan_finished_at " +
                    " RETURNING id";
            String uuid;
            Optional<String> projectId = getProjectId(company, scan.getProjectId(), integrationId);
            if (projectId.isPresent()) {
                uuid = projectId.get();
            } else {
                throw new SQLException("Failed to get project row id");
            }
            try (PreparedStatement scanStmt = conn.prepareStatement(insertScan, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                scanStmt.setObject(i++, integrationId);
                scanStmt.setObject(i++, scan.getScanId());
                scanStmt.setObject(i++, UUID.fromString(uuid));
                scanStmt.setObject(i++, scan.getScanRisk());
                scanStmt.setObject(i++, scan.getStatus());
                scanStmt.setObject(i++, scan.getScanType());
                scanStmt.setObject(i++, getTimestamp(scan.getScanStartedAt()));
                scanStmt.setObject(i++, getTimestamp(scan.getScanFinishedAt()));
                scanStmt.setObject(i++, scan.getScanPath());
                scanStmt.setObject(i++, conn.createArrayOf("varchar",
                        CollectionUtils.emptyIfNull(scan.getLanguages()).toArray()));
                scanStmt.setObject(i++, scan.getOwner());
                scanStmt.setObject(i++, scan.getInitiatorName());
                scanStmt.setObject(i, scan.isPublic());
                int affectedRows = scanStmt.executeUpdate();
                if (affectedRows > 0) {
                    // get the ID back
                    try (ResultSet rs = scanStmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            return rs.getString(1);
                        }
                    }
                }
            }
            return null;
        }));
    }

    public String insertQuery(String company, DbCxSastQuery query, String scanId, String projectId) {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            final int integrationId = NumberUtils.toInt(query.getIntegrationId());
            String insertQuery = "INSERT INTO " + company + "." + QUERIES_TABLE + " (integration_id, " +
                    " scan_id, query_id, cwe_id, name, group_name, categories, severity, language," +
                    " language_hash, language_change_date, " +
                    " severity_index) VALUES(?,?,?,?,?,?,?,?,?,?,?,?) " +
                    " ON CONFLICT(query_id, scan_id, integration_id) DO NOTHING ";
            String insertIssue = "INSERT INTO " + company + "." + ISSUES_TABLE +
                    " (integration_id,query_id,node_id,file_name,status," +
                    "  line_number,column_number,false_positive,severity,assignee,state,detection_date,ingested_at) " +
                    " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(node_id, query_id, integration_id) " +
                    " DO NOTHING";
            Optional<String> projectId1 = getProjectId(company, projectId, integrationId);
            Optional<String> scanId1 = getScanId(company, scanId,
                    projectId1.get(), integrationId);
            try (PreparedStatement queryStmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement issueStmt = conn.prepareStatement(insertIssue, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                queryStmt.setObject(i++, integrationId);
                queryStmt.setObject(i++, UUID.fromString(scanId1.get()));
                queryStmt.setObject(i++, NumberUtils.toInt(query.getQueryId()));
                queryStmt.setObject(i++, NumberUtils.toInt(query.getCweId()));
                queryStmt.setObject(i++, query.getName());
                queryStmt.setObject(i++, query.getGroup());
                queryStmt.setObject(i++, conn.createArrayOf("varchar",
                        CollectionUtils.emptyIfNull(query.getCategories()).toArray()));
                queryStmt.setObject(i++, query.getSeverity());
                queryStmt.setObject(i++, query.getLanguage());
                queryStmt.setObject(i++, query.getLanguageHash());
                queryStmt.setObject(i++, getTimestamp(query.getLanguageChangeDate()));
                queryStmt.setObject(i, query.getSeverityIndex());
                queryStmt.executeUpdate();
                String queryInsertedRowId;
                try (ResultSet rs = queryStmt.getGeneratedKeys()) {
                    if (rs.next())
                        queryInsertedRowId = rs.getString(1);
                    else {
                        Optional<String> idOpt = getQuery(company, query.getQueryId(), scanId1.get(),
                                integrationId);
                        if (idOpt.isPresent()) {
                            queryInsertedRowId = idOpt.get();
                        } else {
                            throw new SQLException("Failed to get query row id");
                        }
                    }
                }
                if (queryInsertedRowId == null)
                    throw new SQLException("Failed to get inserted rowid.");
                if (CollectionUtils.isNotEmpty(query.getIssues())) {
                    for (DbCxSastIssue issue : query.getIssues()) {
                        i = 1;
                        issueStmt.setObject(i++, integrationId);
                        issueStmt.setObject(i++, UUID.fromString(queryInsertedRowId));
                        issueStmt.setObject(i++, issue.getNodeId());
                        issueStmt.setObject(i++, issue.getFileName());
                        issueStmt.setObject(i++, issue.getStatus());
                        issueStmt.setObject(i++, issue.getLine());
                        issueStmt.setObject(i++, issue.getColumn());
                        issueStmt.setObject(i++, issue.isFalsePositive());
                        issueStmt.setObject(i++, issue.getSeverity());
                        issueStmt.setObject(i++, issue.getAssignee());
                        issueStmt.setObject(i++, NumberUtils.toInt(issue.getState()));
                        issueStmt.setObject(i++, convertJavaDateToSqlDate(issue.getDetectionDate()));
                        issueStmt.setObject(i, convertJavaDateToSqlDate(issue.getIngestedAt()));
                        issueStmt.addBatch();
                        issueStmt.clearParameters();
                    }
                    issueStmt.executeBatch();
                }
                return queryInsertedRowId;
            }
        }));
    }

    private Optional<String> getProjectId(String company, String projectId, int integrationId) {
        String query = "SELECT id FROM " + company + "." + PROJECTS_TABLE + " WHERE " +
                " integration_id = :integration_id AND project_id = :project_id ";
        final Map<String, Object> params = Map.of(
                "integration_id", integrationId,
                "project_id", projectId);
        return Optional.ofNullable(template.query(query, params,
                rs -> rs.next() ? rs.getString("id") : null));
    }

    private Optional<String> getScanId(String company, String scanId, String projectId, int integrationId) {
        String query = "SELECT id FROM " + company + "." + SCANS_TABLE + " WHERE " +
                " integration_id = :integration_id AND project_id = :project_id AND scan_id = :scan_id";
        final Map<String, Object> params = Map.of(
                "integration_id", integrationId,
                "project_id", UUID.fromString(projectId),
                "scan_id", scanId);
        return Optional.ofNullable(template.query(query, params,
                rs -> rs.next() ? rs.getString("id") : null));
    }

    public Optional<String> getQuery(String company, String queryId, String scanId,
                                     int integrationId) {
        UUID uuid = UUID.fromString(scanId);
        Validate.notBlank(queryId, "Missing query_id");
        Validate.notBlank(scanId, "Missing scan_id.");
        return Optional.ofNullable(template.query(
                "SELECT * FROM " + company + "." + QUERIES_TABLE
                        + " WHERE query_id = :query_id AND integration_id = :integid AND scan_id = :scan_id",
                Map.of("query_id", NumberUtils.toInt(queryId), "integid", integrationId, "scan_id", uuid),
                rs -> rs.next() ? rs.getString("id") : null));
    }

    public Optional<String> getIssue(String company, int integrationId, String queryId, String nodeId) {
        UUID uuid = UUID.fromString(queryId);
        Validate.notBlank(queryId, "Missing query_id");
        Validate.notBlank(nodeId, "Missing node_id.");
        return Optional.ofNullable(template.query(
                "SELECT * FROM " + company + "." + ISSUES_TABLE
                        + " WHERE integration_id = :integ_id AND query_id = :query_id AND node_id = :node_id",
                Map.of("integ_id", integrationId, "query_id", uuid, "node_id", nodeId),
                rs -> rs.next() ? rs.getString("id") : null));
    }

    public DbListResponse<DbCxSastIssue> listIssues(String company,
                                                    CxSastIssueFilter filter,
                                                    Map<String, SortingOrder> sortBy,
                                                    Integer pageNumber,
                                                    Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        CxSastIssueCriteria conditions = createIssueWhereClauseAndUpdateParams(params,
                filter.getIntegrationIds(), filter.getAssignees(), filter.getFalsePositive(), filter.getScanIds(),
                filter.getSeverities(), filter.getStates(), filter.getStatuses(), filter.getProjects(),
                filter.getLanguages(), filter.getCategories(), filter.getIssueNames(), filter.getIssueGroups(),
                filter.getFiles());
        String sortByKey = getSortByKey(sortBy, ISSUES_SORTABLE_COLUMNS, "status");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        String issuesWhere = (CollectionUtils.isEmpty(conditions.getIssueCriteria())) ? ""
                : " WHERE " + String.join(" AND ", conditions.getIssueCriteria());
        String finalJoin = getFinalJoin(company, conditions);
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        List<DbCxSastIssue> results = List.of();
        String tempSQL = "FROM ( SELECT issues.* FROM " + company + "." + ISSUES_TABLE + " as issues "
                + finalJoin + issuesWhere + " ) foo ";
        if (pageSize > 0) {
            String sql = " SELECT * " + tempSQL + " ORDER BY " + sortByKey + " " + sortOrder.toString()
                    + " OFFSET :skip LIMIT :limit";
            results = template.query(sql, params, DbCxSastConverters.issueRowMapper());
        }
        String countSql = "SELECT count(*) " + tempSQL;
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbCxSastIssue> listFiles(String company,
                                                   CxSastIssueFilter filter,
                                                   Map<String, SortingOrder> sortBy,
                                                   Integer pageNumber,
                                                   Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        CxSastIssueCriteria conditions = createIssueWhereClauseAndUpdateParams(params,
                filter.getIntegrationIds(), filter.getAssignees(), filter.getFalsePositive(), filter.getScanIds(),
                filter.getSeverities(), filter.getStates(), filter.getStatuses(), filter.getProjects(),
                filter.getLanguages(), filter.getCategories(), filter.getIssueNames(), filter.getIssueGroups(),
                filter.getFiles());
        String sortByKey = getSortByKey(sortBy, FILES_SORTABLE_COLUMNS, "node_id");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        String issuesWhere = (CollectionUtils.isEmpty(conditions.getIssueCriteria())) ? ""
                : " WHERE " + String.join(" AND ", conditions.getIssueCriteria());
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        String finalJoin = getFinalJoin(company, conditions);
        String issueSelect = " node_id, file_name, line_number, column_number ";
        List<DbCxSastIssue> results = List.of();
        String tempSQL = "FROM ( SELECT " + issueSelect + " FROM " + company + "." + ISSUES_TABLE
                + " issues " + finalJoin + issuesWhere + " ) foo ";
        if (pageSize > 0) {
            String sql = "SELECT * " + tempSQL + " ORDER BY " + sortByKey + " " + sortOrder.toString()
                    + " OFFSET :skip LIMIT :limit";
            results = template.query(sql, params, DbCxSastConverters.fileRowMapper());
        }
        String countSql = "SELECT count(*) " + tempSQL;
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateIssue(String company,
                                                                        CxSastIssueFilter filter,
                                                                        String configTableKey) {
        CxSastIssueFilter.DISTINCT across = filter.getAcross();
        Validate.notNull(across, "Across cant be missing for groupBy query.");
        Map<String, Object> params = new HashMap<>();
        if (StringUtils.isNotEmpty(configTableKey)) {
            across = CxSastIssueFilter.DISTINCT.none;
        }
        String calculationComponent, selectDistinctString, groupByString, orderByString, key;
        orderByString = "ct DESC";
        calculationComponent = " COUNT(*) as ct ";
        String intervalColumn = "";
        AGG_INTERVAL aggInterval = filter.getAggInterval();
        if (aggInterval == null) {
            aggInterval = AGG_INTERVAL.day;
        }
        Optional<String> additionalKey = Optional.empty();
        switch (filter.getAcross()) {
            case none:
                groupByString = "";
                selectDistinctString = "";
                key = "none";
                break;
            case trend:
                AggTimeQueryHelper.AggTimeQuery issueModAggQuery = AggTimeQueryHelper
                        .getAggTimeQuery("ingested_at", filter.getAcross().toString(),
                                aggInterval.toString(), false);
                intervalColumn = issueModAggQuery.getHelperColumn();
                groupByString = " GROUP BY " + issueModAggQuery.getGroupBy();
                orderByString = issueModAggQuery.getOrderBy();
                selectDistinctString = issueModAggQuery.getSelect();
                additionalKey = Optional.of(issueModAggQuery.getIntervalKey());
                key = "trend";
                break;
            case file:
                groupByString = "GROUP BY file_name";
                selectDistinctString = "file_name";
                key = "file_name";
                break;
            case project:
                groupByString = "GROUP BY project_name";
                selectDistinctString = "project_name";
                key = "project_name";
                break;
            case issue_group:
                groupByString = "GROUP BY group_name";
                selectDistinctString = "group_name";
                key = "group_name";
                break;
            default:
                groupByString = " GROUP BY " + across;
                selectDistinctString = across.toString();
                key = across.toString();
                break;
        }
        CxSastIssueCriteria conditions = createIssueWhereClauseAndUpdateParams(params,
                filter.getIntegrationIds(), filter.getAssignees(), filter.getFalsePositive(), filter.getScanIds(),
                filter.getSeverities(), filter.getStates(), filter.getStatuses(), filter.getProjects(),
                filter.getLanguages(), filter.getCategories(), filter.getIssueNames(), filter.getIssueGroups(),
                filter.getFiles());
        String issuesWhere = (CollectionUtils.isEmpty(conditions.getIssueCriteria())) ? ""
                : " WHERE " + String.join(" AND ", conditions.getIssueCriteria());
        String finalJoin = getFinalJoin(company, conditions);
        final List<DbAggregationResult> dbAggregationResults;
        String limitString = "";
        Integer acrossLimit = filter.getAcrossLimit();
        if (acrossLimit != null && acrossLimit > 0) {
            limitString = " LIMIT " + acrossLimit;
        }
        if (StringUtils.isNotEmpty(configTableKey)) {
            String sql = "SELECT '" + configTableKey + "' AS config_key" + "," + calculationComponent
                    + " FROM ( SELECT * FROM " + company + "." + ISSUES_TABLE + " issues "
                    + finalJoin + issuesWhere + " ) foo "
                    + groupByString + " ORDER BY " + orderByString + limitString;
            dbAggregationResults = template.query(sql, params,
                    DbCxSastConverters.distinctIssueRowMapper("config_key", additionalKey));
        } else {
            String sql = "SELECT " + (StringUtils.isNotEmpty(selectDistinctString) ? selectDistinctString + "," : "") + calculationComponent
                    + " FROM ( SELECT * " + intervalColumn + " FROM " + company + "." + ISSUES_TABLE + " issues "
                    + finalJoin + issuesWhere + " ) foo "
                    + groupByString + " ORDER BY " + orderByString + limitString;
            dbAggregationResults = template.query(sql, params,
                    DbCxSastConverters.distinctIssueRowMapper(key, additionalKey));
        }
        return DbListResponse.of(dbAggregationResults, dbAggregationResults.size());
    }

    @NotNull
    private String getFinalJoin(String company, CxSastIssueCriteria conditions) {
        String queriesWhere = (CollectionUtils.isEmpty(conditions.getQueryCriteria())) ? ""
                : " WHERE " + String.join(" AND ", conditions.getQueryCriteria());
        String scansWhere = (CollectionUtils.isEmpty(conditions.getScanCriteria())) ? ""
                : " WHERE " + String.join(" AND ", conditions.getScanCriteria());
        String queryTableJoin = " INNER JOIN ( SELECT id as query_id,name as issue_name, group_name, language,"
                + " scan_id, categories FROM " + company + "." + QUERIES_TABLE + queriesWhere + " ) queries"
                + " ON queries.query_id=issues.query_id";
        String scanTableJoin = " INNER JOIN ( SELECT id as scan_id,project_id FROM " + company
                + "." + SCANS_TABLE + scansWhere + " ) scans ON scans.scan_id=queries.scan_id ";
        String projectTableJoin = getProjectTableJoin(company, conditions);
        return queryTableJoin + scanTableJoin + projectTableJoin;

    }

    @NotNull
    private String getProjectTableJoin(String company, CxSastIssueCriteria conditions) {
        String projectsWhere = (CollectionUtils.isEmpty(conditions.getProjectCriteria())) ? ""
                : " WHERE " + String.join(" AND ", conditions.getProjectCriteria());
        return " INNER JOIN ( SELECT name as project_name, id as project_id FROM "
                + company + "." + PROJECTS_TABLE + projectsWhere + " ) projects"
                + " ON projects.project_id=scans.project_id ";
    }

    @Value
    @Builder(toBuilder = true)
    public static class CxSastIssueCriteria {
        List<String> projectCriteria;
        List<String> scanCriteria;
        List<String> queryCriteria;
        List<String> issueCriteria;
    }

    @Value
    @Builder(toBuilder = true)
    public static class CxSastScanCriteria {
        List<String> projectCriteria;
        List<String> scanCriteria;
    }

    private CxSastIssueCriteria createIssueWhereClauseAndUpdateParams(Map<String, Object> params,
                                                                      List<String> integrationIds,
                                                                      List<String> assignees,
                                                                      Boolean falsePositive,
                                                                      List<String> scanIds,
                                                                      List<String> severities,
                                                                      List<String> states,
                                                                      List<String> statuses,
                                                                      List<String> projects,
                                                                      List<String> languages,
                                                                      List<String> categories,
                                                                      List<String> issueNames,
                                                                      List<String> issueGroups,
                                                                      List<String> files) {
        List<String> issueTableConditions = new ArrayList<>();
        List<String> scanTableConditions = new ArrayList<>();
        List<String> queryTableConditions = new ArrayList<>();
        List<String> projectTableConditions = new ArrayList<>();
        if (falsePositive != null) {
            issueTableConditions.add("false_positive IN (:false_positive)");
            params.put("false_positive", falsePositive);
        }
        if (CollectionUtils.isNotEmpty(assignees)) {
            issueTableConditions.add("assignee IN (:assignees)");
            params.put("assignees", assignees);
        }
        if (CollectionUtils.isNotEmpty(severities)) {
            issueTableConditions.add("severity IN (:severities)");
            params.put("severities", severities);
        }
        if (CollectionUtils.isNotEmpty(statuses)) {
            issueTableConditions.add("status IN (:statuses)");
            params.put("statuses", statuses);
        }
        if (CollectionUtils.isNotEmpty(scanIds)) {
            scanTableConditions.add("scan_id IN (:scan_ids)");
            params.put("scan_ids", scanIds);
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            issueTableConditions.add("integration_id IN (:integration_ids)");
            projectTableConditions.add("integration_id IN (:integration_ids)");
            scanTableConditions.add("integration_id IN (:integration_ids)");
            queryTableConditions.add("integration_id IN (:integration_ids)");
            params.put("integration_ids",
                    integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(states)) {
            issueTableConditions.add("state IN (:states)");
            params.put("states", states.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(languages)) {
            queryTableConditions.add("language IN (:languages)");
            params.put("languages", languages);
        }
        if (CollectionUtils.isNotEmpty(projects)) {
            projectTableConditions.add("name IN (:projects)");
            params.put("projects", projects);
        }
        if (CollectionUtils.isNotEmpty(categories)) {
            queryTableConditions.add("categories && ARRAY[ :categories ]");
            params.put("categories", categories);
        }
        if (CollectionUtils.isNotEmpty(issueNames)) {
            queryTableConditions.add("name IN (:issueNames)");
            params.put("issueNames", issueNames);
        }
        if (CollectionUtils.isNotEmpty(issueGroups)) {
            queryTableConditions.add("group_name IN (:issueGroups)");
            params.put("issueGroups", issueGroups);
        }
        if (CollectionUtils.isNotEmpty(files)) {
            issueTableConditions.add("file_name IN (:files)");
            params.put("files", files);
        }
        return CxSastIssueCriteria.builder()
                .issueCriteria(issueTableConditions)
                .queryCriteria(queryTableConditions)
                .scanCriteria(scanTableConditions)
                .projectCriteria(projectTableConditions)
                .build();
    }

    public DbListResponse<DbCxSastScan> listScans(String company,
                                                  CxSastScanFilter filter,
                                                  Map<String, SortingOrder> sortBy,
                                                  Integer pageNumber,
                                                  Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        CxSastScanCriteria conditions = createScanWhereClauseAndUpdateParams(params,
                filter.getIntegrationIds(), filter.getInitiatorNames(), filter.getIsPublic(), filter.getScanIds(),
                filter.getLanguages(), filter.getOwners(), filter.getProjectNames(),
                filter.getScanPaths(), filter.getScanTypes(), filter.getStatuses());
        String sortByKey = getSortByKey(sortBy, SCANS_SORTABLE_COLUMNS, "status");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        String scansWhere = (CollectionUtils.isEmpty(conditions.getScanCriteria())) ? ""
                : " WHERE " + String.join(" AND ", conditions.getScanCriteria());
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        List<DbCxSastScan> results = List.of();
        String projectsWhere = (CollectionUtils.isEmpty(conditions.getProjectCriteria())) ? ""
                : " WHERE " + String.join(" AND ", conditions.getProjectCriteria());
        String projectTableJoin = " INNER JOIN ( SELECT name as project_name, id as project_id FROM "
                + company + "." + PROJECTS_TABLE + projectsWhere + " ) projects"
                + " ON projects.project_id=scans.project_id ";
        String tempSQL = "FROM ( SELECT scans.* FROM " + company + "." + SCANS_TABLE + " scans "
                + projectTableJoin + scansWhere + " ) foo";
        if (pageSize > 0) {
            String sql = "SELECT * " + tempSQL + " ORDER BY " + sortByKey + " " + sortOrder.toString()
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbCxSastConverters.scanRowMapper());
        }

        String countSql = "SELECT count(*) " + tempSQL;
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    private String getSortByKey(Map<String, SortingOrder> sortBy, Set<String> sortableColumn, String defaultKey) {
        return sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (sortableColumn.contains(entry.getKey()))
                        return entry.getKey();
                    return defaultKey;
                })
                .orElse(defaultKey);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateScan(String company,
                                                                       CxSastScanFilter filter,
                                                                       String configTableKey) {
        CxSastScanFilter.DISTINCT across = filter.getAcross();
        Validate.notNull(across, "Across cant be missing for groupBy query.");
        Map<String, Object> params = new HashMap<>();
        if (StringUtils.isNotEmpty(configTableKey)) {
            across = CxSastScanFilter.DISTINCT.none;
        }
        String calculationComponent, selectDistinctString, groupByString, orderByString, key;
        orderByString = "ct DESC";
        calculationComponent = "COUNT(*) as ct";
        switch (across) {
            case none:
                groupByString = "";
                selectDistinctString = "";
                key = "none";
                break;
            case language:
                groupByString = " GROUP BY " + across;
                selectDistinctString = "UNNEST(" + across + "s) AS " + across;
                key = across.toString();
                break;
            default:
                groupByString = " GROUP BY " + across;
                selectDistinctString = across.toString();
                key = across.toString();
                break;
        }
        CxSastScanCriteria conditions = createScanWhereClauseAndUpdateParams(params,
                filter.getIntegrationIds(), filter.getInitiatorNames(), filter.getIsPublic(), filter.getScanIds(),
                filter.getLanguages(), filter.getOwners(), filter.getProjectNames(),
                filter.getScanPaths(), filter.getScanTypes(), filter.getStatuses());
        String scansWhere = (CollectionUtils.isEmpty(conditions.getScanCriteria())) ? ""
                : " WHERE " + String.join(" AND ", conditions.getScanCriteria());
        String projectsWhere = (CollectionUtils.isEmpty(conditions.getProjectCriteria())) ? ""
                : " WHERE " + String.join(" AND ", conditions.getProjectCriteria());
        String projectTableJoin = " INNER JOIN ( SELECT name as project_name, id as project_id FROM "
                + company + "." + PROJECTS_TABLE + projectsWhere + " ) projects"
                + " ON projects.project_id=scans.project_id ";
        String limitString = "";
        Integer acrossLimit = filter.getAcrossLimit();
        if (acrossLimit != null && acrossLimit > 0) {
            limitString = " LIMIT " + acrossLimit;
        }
        String tempSQL = " FROM ( SELECT * FROM " + company + "." + SCANS_TABLE + " as scans " + projectTableJoin
                + scansWhere + " ) foo " + groupByString + " ORDER BY " + orderByString + limitString;
        final List<DbAggregationResult> dbAggregationResults;
        if (StringUtils.isNotEmpty(configTableKey)) {
            String sql = "SELECT '" + configTableKey + "' AS config_key" + "," + calculationComponent + tempSQL;
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            dbAggregationResults = template.query(sql, params,
                    DbCxSastConverters.distinctScanRowMapper("config_key"));
        } else {
            String sql = "SELECT " + (StringUtils.isNotEmpty(selectDistinctString) ? selectDistinctString + "," : "")
                    + calculationComponent + tempSQL;
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            dbAggregationResults = template.query(sql, params, DbCxSastConverters.distinctScanRowMapper(key));
        }
        return DbListResponse.of(dbAggregationResults, dbAggregationResults.size());
    }


    private CxSastScanCriteria createScanWhereClauseAndUpdateParams(Map<String, Object> params,
                                                                    List<String> integrationIds,
                                                                    List<String> initiatorNames,
                                                                    Boolean isPublic,
                                                                    List<String> scanIds,
                                                                    List<String> languages,
                                                                    List<String> owners,
                                                                    List<String> projectNames,
                                                                    List<String> scanPaths,
                                                                    List<String> scanTypes,
                                                                    List<String> statuses) {

        List<String> scanTableConditions = new ArrayList<>();
        List<String> projectTableConditions = new ArrayList<>();
        if (isPublic != null) {
            scanTableConditions.add("is_public IN (:is_public)");
            params.put("is_public", isPublic);
        }
        if (CollectionUtils.isNotEmpty(initiatorNames)) {
            scanTableConditions.add("initiator_name IN (:initiator_names)");
            params.put("initiator_names", initiatorNames);
        }
        if (CollectionUtils.isNotEmpty(languages)) {
            scanTableConditions.add("languages && ARRAY[ :languages ]");
            params.put("languages", languages);
        }
        if (CollectionUtils.isNotEmpty(owners)) {
            scanTableConditions.add("owner IN (:owners)");
            params.put("owners", owners);
        }
        if (CollectionUtils.isNotEmpty(statuses)) {
            scanTableConditions.add("status IN (:statuses)");
            params.put("statuses", statuses);
        }
        if (CollectionUtils.isNotEmpty(scanIds)) {
            scanTableConditions.add("scan_id IN (:scan_ids)");
            params.put("scan_ids", scanIds);
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            scanTableConditions.add("integration_id IN (:integration_ids)");
            projectTableConditions.add("integration_id IN (:integration_ids)");
            params.put("integration_ids",
                    integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(projectNames)) {
            projectTableConditions.add("name IN (:project_names)");
            params.put("project_names", projectNames);
        }
        if (CollectionUtils.isNotEmpty(scanPaths)) {
            scanTableConditions.add("scan_path IN (:scan_paths)");
            params.put("scan_paths", scanPaths);
        }
        if (CollectionUtils.isNotEmpty(scanTypes)) {
            scanTableConditions.add("scan_type IN (:scan_types)");
            params.put("scan_types", scanTypes);
        }
        return CxSastScanCriteria.builder()
                .projectCriteria(projectTableConditions)
                .scanCriteria(scanTableConditions)
                .build();
    }

    public DbListResponse<DbAggregationResult> stackedGroupByIssue(String company,
                                                                   CxSastIssueFilter filter,
                                                                   List<CxSastIssueFilter.DISTINCT> stacks,
                                                                   String configTableKey)
            throws SQLException {
        Set<CxSastIssueFilter.DISTINCT> stackSupported = Set.of(
                CxSastIssueFilter.DISTINCT.assignee,
                CxSastIssueFilter.DISTINCT.severity,
                CxSastIssueFilter.DISTINCT.state,
                CxSastIssueFilter.DISTINCT.status,
                CxSastIssueFilter.DISTINCT.file,
                CxSastIssueFilter.DISTINCT.issue_name,
                CxSastIssueFilter.DISTINCT.project,
                CxSastIssueFilter.DISTINCT.language,
                CxSastIssueFilter.DISTINCT.issue_group
        );
        DbListResponse<DbAggregationResult> result = groupByAndCalculateIssue(company, filter, configTableKey);
        if (stacks == null
                || stacks.size() == 0
                || !stackSupported.contains(stacks.get(0))
                || !stackSupported.contains(filter.getAcross()))
            return result;
        CxSastIssueFilter.DISTINCT stack = stacks.get(0);
        List<DbAggregationResult> finalList = new ArrayList<>();
        for (DbAggregationResult record : result.getRecords()) {
            CxSastIssueFilter newFilter;
            if (StringUtils.isNotEmpty(configTableKey)) {
                newFilter = filter.toBuilder().across(stack).build();
            } else {
                switch (filter.getAcross()) {
                    case assignee:
                        newFilter = filter.toBuilder().assignees(List.of(record.getKey())).across(stack).build();
                        break;
                    case severity:
                        newFilter = filter.toBuilder().severities(List.of(record.getKey())).across(stack).build();
                        break;
                    case state:
                        newFilter = filter.toBuilder().states(List.of(record.getKey())).across(stack).build();
                        break;
                    case status:
                        newFilter = filter.toBuilder().statuses(List.of(record.getKey())).across(stack).build();
                        break;
                    case file:
                        newFilter = filter.toBuilder().files(List.of(record.getKey())).across(stack).build();
                        break;
                    case issue_name:
                        newFilter = filter.toBuilder().issueNames(List.of(record.getKey())).across(stack).build();
                        break;
                    case project:
                        newFilter = filter.toBuilder().projects(List.of(record.getKey())).across(stack).build();
                        break;
                    case language:
                        newFilter = filter.toBuilder().languages(List.of(record.getKey())).across(stack).build();
                        break;
                    case issue_group:
                        newFilter = filter.toBuilder().issueGroups(List.of(record.getKey())).across(stack).build();
                        break;
                    default:
                        throw new SQLException("This stack is not available for cxsast issues." + stack);
                }
            }
            finalList.add(record.toBuilder().stacks(groupByAndCalculateIssue(company, newFilter,
                    null).getRecords()).build());
        }
        return DbListResponse.of(finalList, finalList.size());
    }

    public DbListResponse<DbAggregationResult> stackedGroupByScan(String company,
                                                                  CxSastScanFilter filter,
                                                                  List<CxSastScanFilter.DISTINCT> stacks,
                                                                  String configTableKey)
            throws SQLException {
        Set<CxSastScanFilter.DISTINCT> stackSupported = Set.of(
                CxSastScanFilter.DISTINCT.initiator_name,
                CxSastScanFilter.DISTINCT.scan_type,
                CxSastScanFilter.DISTINCT.language,
                CxSastScanFilter.DISTINCT.owner,
                CxSastScanFilter.DISTINCT.scan_path
        );
        DbListResponse<DbAggregationResult> result = groupByAndCalculateScan(company, filter, configTableKey);
        if (stacks == null
                || stacks.size() == 0
                || !stackSupported.contains(stacks.get(0))
                || !stackSupported.contains(filter.getAcross()))
            return result;
        CxSastScanFilter.DISTINCT stack = stacks.get(0);
        List<DbAggregationResult> finalList = new ArrayList<>();
        for (DbAggregationResult record : result.getRecords()) {
            CxSastScanFilter newFilter;
            if (StringUtils.isNotEmpty(configTableKey)) {
                newFilter = filter.toBuilder().across(stack).build();
            } else {
                switch (filter.getAcross()) {
                    case owner:
                        newFilter = filter.toBuilder().owners(List.of(record.getKey())).across(stack).build();
                        break;
                    case scan_path:
                        newFilter = filter.toBuilder().scanPaths(List.of(record.getKey())).across(stack).build();
                        break;
                    case scan_type:
                        newFilter = filter.toBuilder().scanTypes(List.of(record.getKey())).across(stack).build();
                        break;
                    case language:
                        newFilter = filter.toBuilder().languages(List.of(record.getKey())).across(stack).build();
                        break;
                    case initiator_name:
                        newFilter = filter.toBuilder().initiatorNames(List.of(record.getKey())).across(stack).build();
                        break;
                    default:
                        throw new SQLException("This stack is not available for cxsast scans." + stack);
                }
            }
            finalList.add(record.toBuilder().stacks(groupByAndCalculateScan(company, newFilter,
                    null).getRecords()).build());
        }
        return DbListResponse.of(finalList, finalList.size());
    }

    @Override
    public Boolean update(String company, DbCxSastProject t) throws SQLException {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Optional<DbCxSastProject> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    @Override
    public DbListResponse<DbCxSastProject> list(String company, Integer pageNumber, Integer pageSize) throws
            SQLException {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddlStmts = List.of(
                " CREATE TABLE IF NOT EXISTS " + company + "." + PROJECTS_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES " +
                        company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    project_id VARCHAR NOT NULL,\n" +
                        "    team_id VARCHAR NOT NULL,\n" +
                        "    name VARCHAR NOT NULL,\n" +
                        "    is_public BOOLEAN NOT NULL,\n" +
                        "    UNIQUE (project_id,team_id,integration_id)\n" +
                        ")",
                " CREATE INDEX IF NOT EXISTS " + PROJECTS_TABLE + "_project_id_team_id_compound_idx ON " +
                        company + "." + PROJECTS_TABLE + " (project_id, team_id)",
                " CREATE TABLE IF NOT EXISTS " + company + "." + SCANS_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES " +
                        company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    scan_id VARCHAR NOT NULL,\n " +
                        "    project_id UUID NOT NULL REFERENCES " +
                        company + "." + PROJECTS_TABLE + "(id) ON DELETE CASCADE,\n " +
                        "    scan_risk INTEGER NOT NULL,\n " +
                        "    status VARCHAR NOT NULL,\n " +
                        "    scan_type VARCHAR NOT NULL,\n " +
                        "    scan_started_at TIMESTAMP,\n " +
                        "    scan_finished_at TIMESTAMP,\n " +
                        "    scan_path VARCHAR NOT NULL,\n " +
                        "    languages VARCHAR[] NOT NULL,\n " +
                        "    owner VARCHAR NOT NULL,\n " +
                        "    initiator_name VARCHAR NOT NULL,\n " +
                        "    is_public BOOLEAN NOT NUll,\n " +
                        "    UNIQUE (scan_id, project_id, integration_id)\n" +
                        ")",
                " CREATE INDEX IF NOT EXISTS " + SCANS_TABLE + "_scan_id_project_id_compound_idx ON " +
                        company + "." + SCANS_TABLE + " (scan_id, project_id)",
                " CREATE TABLE IF NOT EXISTS " + company + "." + QUERIES_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n " +
                        "    integration_id INTEGER NOT NULL REFERENCES " +
                        company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    scan_id UUID NOT NULL REFERENCES " +
                        company + "." + SCANS_TABLE + "(id) ON DELETE CASCADE,\n " +
                        "    query_id INTEGER NOT NULL,\n " +
                        "    cwe_id INTEGER NOT NULL,\n " +
                        "    name VARCHAR NOT NULL,\n " +
                        "    group_name VARCHAR NOT NULL,\n " +
                        "    categories VARCHAR[],\n " +
                        "    severity VARCHAR NOT NULL,\n " +
                        "    language VARCHAR,\n " +
                        "    language_hash VARCHAR,\n " +
                        "    language_change_date TIMESTAMP,\n " +
                        "    severity_index VARCHAR,\n " +
                        "    UNIQUE (query_id, scan_id, integration_id)\n" +
                        ")",
                " CREATE INDEX IF NOT EXISTS " + QUERIES_TABLE + "_query_id_scan_id_compound_idx ON " +
                        company + "." + QUERIES_TABLE + " (query_id, scan_id)",
                " CREATE TABLE IF NOT EXISTS " + company + "." + ISSUES_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n " +
                        "    integration_id INTEGER NOT NULL REFERENCES " +
                        company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    query_id UUID NOT NULL REFERENCES " +
                        company + "." + QUERIES_TABLE + "(id) ON DELETE CASCADE,\n " +
                        "    node_id VARCHAR NOT NULL,\n " +
                        "    file_name VARCHAR NOT NULL,\n " +
                        "    status VARCHAR NOT NULL,\n " +
                        "    line_number INTEGER NOT NULL,\n " +
                        "    column_number INTEGER NOT NULL,\n " +
                        "    false_positive BOOLEAN NOT NULL,\n " +
                        "    severity VARCHAR NOT NULL,\n " +
                        "    assignee VARCHAR,\n " +
                        "    state INTEGER NOT NULL,\n " +
                        "    detection_date TIMESTAMP,\n " +
                        "    ingested_at DATE NOT NULL,\n " +
                        "    UNIQUE (node_id, query_id, integration_id)\n" +
                        ")",
                " CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_node_id_query_id_compound_idx ON " +
                        company + "." + ISSUES_TABLE + " (node_id, query_id)"
        );
        ddlStmts.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

    private Timestamp getTimestamp(Long date) {
        return date != null ? (new Timestamp(date)) : null;
    }

    private Timestamp convertJavaDateToSqlDate(java.util.Date date) {
        return date != null ? Timestamp.from(date.toInstant()) : null;
    }
}