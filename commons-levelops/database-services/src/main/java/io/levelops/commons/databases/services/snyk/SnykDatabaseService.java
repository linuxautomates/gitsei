package io.levelops.commons.databases.services.snyk;

import io.levelops.commons.databases.converters.DbSnykIssueConverters;
import io.levelops.commons.databases.converters.DbSnykPatchConverter;
import io.levelops.commons.databases.models.database.snyk.DbSnykIssue;
import io.levelops.commons.databases.models.database.snyk.DbSnykIssues;
import io.levelops.commons.databases.models.filters.SnykIssuesFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.integrations.snyk.models.SnykIssues;
import io.levelops.integrations.snyk.models.SnykLicenseIssue;
import io.levelops.integrations.snyk.models.SnykVulnerability;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * A {@link DatabaseService} implementation for Snyk. It is responsible for creating and executing SQL statements.
 */

@Log4j2
@Service
public class SnykDatabaseService extends DatabaseService<DbSnykIssue> {

    private static final String SNYK_ISSUES = "snyk_issues";
    private static final String SNYK_ISSUES_PATCH = "snyk_issues_patches";
    private static final int BATCH_LIMIT = 100;

    private static final String INSERT_QUERY = "INSERT INTO {0}." + SNYK_ISSUES 
        + " ( integration_id, org_id, org , project_id ,issue_id ,project ,url ,title ,type ,package ,version ,severity ,language ,package_manager ,ignored ,patched ,exploit_maturity ,upgradable ,patchable ,pinnable ,cvssv3 ,cvssv_score ,disclosure_time ,publication_time ,ingested_at) " 
        + "VALUES (:integrationId, :orgId, :org, :projectId, :issueId, :project, :url, :title, :type, :package, :version, :severity, :language, :packageManager, :ignored, :patched, :exploitMaturity, :upgradable, :patchable, :pinnable, :cvssv3, :cvssvScore, :disclosureTime, :publicationTime, :ingestedAt) " 
        + "ON CONFLICT (integration_id, org_id, project_id, issue_id, ingested_at) "
        + "DO UPDATE SET project=EXCLUDED.project, url=EXCLUDED.url, title=EXCLUDED.title, type=EXCLUDED.type, package=EXCLUDED.package, version=EXCLUDED.version, severity=EXCLUDED.severity, language=EXCLUDED.language, package_manager=EXCLUDED.package_manager, ignored=EXCLUDED.ignored, " 
        + "patched=EXCLUDED.patched, exploit_maturity=EXCLUDED.exploit_maturity, upgradable=EXCLUDED.upgradable, patchable=EXCLUDED.patchable, pinnable=EXCLUDED.pinnable, cvssv3=EXCLUDED.cvssv3, cvssv_score=EXCLUDED.cvssv_score, disclosure_time=EXCLUDED.disclosure_time, " 
        + "publication_time=EXCLUDED.publication_time, org=EXCLUDED.org";
    
    private static final String INSERT_PATCH_QUERY = "INSERT INTO {0}." + SNYK_ISSUES_PATCH 
        + " (issue_id, patch_id, version, modification_time) " 
        + "VALUES (:issueId, :patchId, :version, :modificationTime) " 
        + "ON CONFLICT (issue_id, patch_id) DO UPDATE SET patch_id=EXCLUDED.patch_id, version=EXCLUDED.version, modification_time=EXCLUDED.modification_time";

    private static final Set<SnykIssuesFilter.Distinct> SUPPORTED_STACKS = Set.of(
            SnykIssuesFilter.Distinct.org,
            SnykIssuesFilter.Distinct.project,
            SnykIssuesFilter.Distinct.title,
            SnykIssuesFilter.Distinct.type,
            SnykIssuesFilter.Distinct.severity,
            SnykIssuesFilter.Distinct.language,
            SnykIssuesFilter.Distinct.package_name,
            SnykIssuesFilter.Distinct.cvssv3,
            SnykIssuesFilter.Distinct.package_manager,
            SnykIssuesFilter.Distinct.exploit_maturity,
            SnykIssuesFilter.Distinct.upgradable,
            SnykIssuesFilter.Distinct.patchable,
            SnykIssuesFilter.Distinct.pinnable,
            SnykIssuesFilter.Distinct.ignored,
            SnykIssuesFilter.Distinct.patched
    );

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public SnykDatabaseService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public Boolean update(String company, DbSnykIssue t) throws SQLException {
        return null;
    }

    @Override
    public Optional<DbSnykIssue> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return null;
    }

    @Override
    public DbListResponse<DbSnykIssue> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return null;
    }

    @Override
    public String insert(String company, DbSnykIssue dbIssue) {
        int integrationId = NumberUtils.toInt(dbIssue.getIntegrationId());
        String insertIssueQuery = MessageFormat.format(INSERT_QUERY,company);
        var keyHolder = new GeneratedKeyHolder();
        template.update(
            insertIssueQuery, 
            new MapSqlParameterSource()
                .addValue("integrationId", integrationId)
                .addValue("orgId", dbIssue.getOrgId())
                .addValue("org", dbIssue.getOrg())
                .addValue("projectId", dbIssue.getProjectId())
                .addValue("issueId", dbIssue.getIssueId())
                .addValue("project", Optional.ofNullable(dbIssue.getProjectName()).orElse(dbIssue.getProjectId()))
                .addValue("url", dbIssue.getUrl())
                .addValue("title", dbIssue.getTitle())
                .addValue("type", dbIssue.getType())
                .addValue("package", dbIssue.getPackageName())
                .addValue("version", dbIssue.getVersion())
                .addValue("severity", dbIssue.getSeverity())
                .addValue("language", dbIssue.getLanguage())
                .addValue("packageManager", dbIssue.getPackageManager())
                .addValue("ignored", Optional.ofNullable(dbIssue.getIgnored()).orElse(false))
                .addValue("patched", Optional.ofNullable(dbIssue.getPatched()).orElse(false))
                .addValue("exploitMaturity", dbIssue.getExploitMaturity())
                .addValue("upgradable", dbIssue.getUpgradable())
                .addValue("patchable", dbIssue.getPatchable())
                .addValue("pinnable", dbIssue.getPinnable())
                .addValue("cvssv3", dbIssue.getCvssv3())
                .addValue("cvssvScore", dbIssue.getCvssScore())
                .addValue("disclosureTime", getTimestamp(dbIssue.getDisclosureTime()))
                .addValue("publicationTime", getTimestamp(dbIssue.getPublicationTime()))
                .addValue("ingestedAt", getTimestamp(dbIssue.getIngestedAt())),
            keyHolder,
            new String[]{"id"}
            );
        var issueId = keyHolder.getKeys().get("id").toString();
        if (issueId == null){
            log.error("Failed to get inserted row id.");
            return null;
        }
        UUID issueUuid = UUID.fromString(issueId);
        var batchParams = new ArrayList<MapSqlParameterSource>();
        String insertIssuePatchQuery = MessageFormat.format(INSERT_PATCH_QUERY, company);
        for (SnykVulnerability.Patch patch: CollectionUtils.emptyIfNull(dbIssue.getPatches())) {
            batchParams.add(getPatchParams(patch, issueUuid));
            if (batchParams.size() == BATCH_LIMIT) {
                template.batchUpdate(insertIssuePatchQuery, batchParams.toArray(new MapSqlParameterSource[0]));
                batchParams = new ArrayList<MapSqlParameterSource>();
            }
        }
        if (batchParams.size() > 0) {
            template.batchUpdate(insertIssuePatchQuery, batchParams.toArray(new MapSqlParameterSource[0]));
        }
        return issueId;
    }

    public void batchInsert(String company, DbSnykIssues dbIssues) {
        int integrationId = NumberUtils.toInt(dbIssues.getIntegrationId());
        String insertIssueQuery = MessageFormat.format(INSERT_QUERY,company);
        List<String> issueIds = new LinkedList<>();
        Optional<SnykIssues.Issues> issues = Optional.ofNullable(dbIssues.getIssues());

        if (issues.isEmpty())
            return;

        var batchParams = new ArrayList<MapSqlParameterSource>();
        for (SnykVulnerability vulnerability: CollectionUtils.emptyIfNull(issues.get().getVulnerabilities())) {
            batchParams.add(getVulnerabilityParams(dbIssues, vulnerability, integrationId));
            if (batchParams.size() == BATCH_LIMIT) {
                template.batchUpdate(insertIssueQuery, batchParams.toArray(new MapSqlParameterSource[0]));
                batchParams = new ArrayList<MapSqlParameterSource>();
                // fillIssueIdsFromPreparedStatement(insertIssueStmt, issueIds);
            }
        }
        if (batchParams.size() > 0) {
            template.batchUpdate(insertIssueQuery, batchParams.toArray(new MapSqlParameterSource[0]));
            batchParams = new ArrayList<MapSqlParameterSource>();
            // fillIssueIdsFromPreparedStatement(insertIssueStmt, issueIds);
        }

        Iterator<String> vulnerabilityIdsIterator = issueIds.iterator();
        Iterator<SnykVulnerability> vulnerabilityIterator = CollectionUtils.emptyIfNull(issues.get().getVulnerabilities()).iterator();
        String insertIssuePatchQuery = MessageFormat.format(INSERT_PATCH_QUERY, company);
        while (vulnerabilityIdsIterator.hasNext() && vulnerabilityIterator.hasNext()) {
            UUID issueUuid = UUID.fromString(vulnerabilityIdsIterator.next());
            SnykVulnerability issue = vulnerabilityIterator.next();
            for (SnykVulnerability.Patch patch: CollectionUtils.emptyIfNull(issue.getPatches())) {
                batchParams.add(getPatchParams(patch, issueUuid));
                if (batchParams.size() == BATCH_LIMIT) {
                    template.batchUpdate(insertIssuePatchQuery, batchParams.toArray(new MapSqlParameterSource[0]));
                    batchParams = new ArrayList<MapSqlParameterSource>();
                }
            }
        }
        if (batchParams.size() > 0) {
            template.batchUpdate(insertIssuePatchQuery, batchParams.toArray(new MapSqlParameterSource[0]));
        }

        batchParams = new ArrayList<MapSqlParameterSource>();
        for (SnykLicenseIssue licenseIssue: CollectionUtils.emptyIfNull(issues.get().getLicenses())) {
            batchParams.add(getLicenseParams(dbIssues, licenseIssue, integrationId));
            if (batchParams.size() == BATCH_LIMIT) {
                template.batchUpdate(insertIssueQuery, batchParams.toArray(new MapSqlParameterSource[0]));
                batchParams = new ArrayList<MapSqlParameterSource>();
                // fillIssueIdsFromPreparedStatement(insertIssueStmt, issueIds);
            }
        }
        if (batchParams.size() > 0) {
            template.batchUpdate(insertIssueQuery, batchParams.toArray(new MapSqlParameterSource[0]));
            // fillIssueIdsFromPreparedStatement(insertIssueStmt, issueIds);
        }
    }

    public DbListResponse<DbSnykIssue> listIssues(String company, SnykIssuesFilter filter, Integer pageNumber, Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        List<String> conditions = createWhereClauseAndUpdateParams(params, filter, "");
        setPagingParams(params, pageNumber, pageSize);
        String limitClause = " OFFSET :offset LIMIT :limit";
        String whereClause = getWhereClause(conditions);
        String selectQuery = "SELECT * FROM " + company + "." + SNYK_ISSUES + " ";
        String query = selectQuery + whereClause;
        String sqlQuery = query + limitClause;
        List<DbSnykIssue> issues = template.query(sqlQuery, params, DbSnykIssueConverters.listRowMapper());
        String countSql = "SELECT COUNT(*) FROM (" + query + ") AS x";
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(issues, count);
    }

    public DbListResponse<SnykVulnerability.Patch> listPatches(String company, SnykIssuesFilter filter, Integer pageNumber, Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        List<String> conditions = createWhereClauseAndUpdateParams(params, filter, "issue");
        setPagingParams(params, pageNumber, pageSize);
        String limitClause = " OFFSET :offset LIMIT :limit";
        String whereClause = getWhereClause(conditions);
        String selectQuery = "SELECT patch.* FROM " + company + "." + SNYK_ISSUES_PATCH + " AS patch JOIN " + company + "." + SNYK_ISSUES +
                " AS issue ON patch.issue_id = issue.id " + whereClause;
        String sqlQuery = selectQuery + limitClause;
        List<SnykVulnerability.Patch> issues = template.query(sqlQuery, params, DbSnykPatchConverter.listRowMapper());
        String countSql = "SELECT COUNT(*) FROM (" + selectQuery + ") AS x";
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(issues, count);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateIssues(String company, SnykIssuesFilter filter,
                                                                         String configTableKey) {
        final SnykIssuesFilter.Calculation calculation = filter.getCalculation();
        SnykIssuesFilter.Distinct distinct = filter.getAcross();
        if (StringUtils.isNotEmpty(configTableKey))
            distinct = SnykIssuesFilter.Distinct.none;
        Map<String, Object> params = new HashMap<>();
        List<String> conditions = createWhereClauseAndUpdateParams(params, filter, "");
        String aggColumn, orderBySql, aggName;
        boolean needPatchesTable = false;
        switch (calculation) {
            case scores:
                aggName = "cvssv_score";
                aggColumn = " MIN(" + aggName + ") AS mn, MAX(" + aggName + ") as mx, COUNT(id) AS ct, " +
                        "PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY " + aggName + "), AVG(" + aggName + ") AS mean";
                orderBySql = " ORDER BY mx DESC ";
                break;
            case total_patches:
                aggColumn = " COUNT(id) AS ct ";
                orderBySql = " ORDER BY ct DESC ";
                needPatchesTable = true;
                break;
            default:
                aggColumn = " COUNT(id) AS ct ";
                orderBySql = " ORDER BY ct DESC ";
                break;
        }
        String groupByKey;
        switch (distinct) {
            case trend:
                groupByKey = "ingested_at";
                break;
            case package_name:
                groupByKey = "package";
                break;
            default:
                groupByKey = distinct.name();
        }
        String groupBySql = distinct.equals(SnykIssuesFilter.Distinct.none) ? "" : " GROUP BY " + groupByKey;
        String whereClause = getWhereClause(conditions);
        String fromTable;
        String issueTable = " (SELECT * FROM " + company + "." + SNYK_ISSUES + whereClause + ") AS issues ";
        if (needPatchesTable) {
            String joinTable = issueTable + " JOIN " + company + "." + SNYK_ISSUES_PATCH + " AS patches ON issues.id = patches.issue_id";
            fromTable = " FROM (SELECT issues.* FROM " + joinTable + ") AS x ";
        }
        else
            fromTable = " FROM " + issueTable;
        List<DbAggregationResult> aggregationResults;
        if (StringUtils.isNotEmpty(configTableKey)) {
            String query = "SELECT '" + configTableKey + "' AS config_key, " + aggColumn + fromTable + orderBySql;
            aggregationResults = template.query(query, params, DbSnykIssueConverters.aggRowMapper("config_key", calculation));
        }
        else {
            String query = "SELECT " + groupByKey + ", " + aggColumn + fromTable + groupBySql + orderBySql;
            aggregationResults = template.query(query, params, DbSnykIssueConverters.aggRowMapper(groupByKey, calculation));
        }
        return DbListResponse.of(aggregationResults, aggregationResults.size());
    }

    public DbListResponse<DbAggregationResult> stackedGroupByIssues(String company,
                                                                    SnykIssuesFilter filter,
                                                                    List<SnykIssuesFilter.Distinct> stacks,
                                                                    String configTableKey)
            throws SQLException {
        final DbListResponse<DbAggregationResult> result = groupByAndCalculateIssues(company, filter, configTableKey);
        if (stacks == null
                || stacks.size() == 0
                || !SUPPORTED_STACKS.contains(stacks.get(0))
                || !SUPPORTED_STACKS.contains(filter.getAcross()))
            return result;
        final SnykIssuesFilter.Distinct stack = stacks.get(0);
        List<DbAggregationResult> finalList = new ArrayList<>();
        for (DbAggregationResult record : result.getRecords()) {
            SnykIssuesFilter stackFilter;
            if (StringUtils.isNotEmpty(configTableKey)) {
                stackFilter = filter.toBuilder().across(stack).build();
            } else {
                switch (filter.getAcross()) {
                    case org:
                        stackFilter = filter.toBuilder().orgs(List.of(record.getKey())).across(stack).build();
                        break;
                    case project:
                        stackFilter = filter.toBuilder().projects(List.of(record.getKey())).across(stack).build();
                        break;
                    case title:
                        stackFilter = filter.toBuilder().titles(List.of(record.getKey())).across(stack).build();
                        break;
                    case type:
                        stackFilter = filter.toBuilder().types(List.of(record.getKey())).across(stack).build();
                        break;
                    case severity:
                        stackFilter = filter.toBuilder().severities(List.of(record.getKey())).across(stack).build();
                        break;
                    case language:
                        stackFilter = filter.toBuilder().languages(List.of(record.getKey())).across(stack).build();
                        break;
                    case package_manager:
                        stackFilter = filter.toBuilder().packageManagers(List.of(record.getKey())).across(stack).build();
                        break;
                    case exploit_maturity:
                        stackFilter = filter.toBuilder().exploitMaturities(List.of(record.getKey())).across(stack).build();
                        break;
                    case package_name:
                        stackFilter = filter.toBuilder().packageNames(List.of(record.getKey())).across(stack).build();
                        break;
                    case cvssv3:
                        stackFilter = filter.toBuilder().cvssv3(List.of(record.getKey())).across(stack).build();
                        break;
                    default:
                        throw new SQLException("This stack is not available for CircleCI builds." + stack);
                }
            }
            finalList.add(record.toBuilder().stacks(groupByAndCalculateIssues(company, stackFilter, null)
                    .getRecords()).build());
        }
        return DbListResponse.of(finalList, finalList.size());
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddlStmts = List.of("CREATE TABLE IF NOT EXISTS " + company + "." + SNYK_ISSUES +
                        " (id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " integration_id INTEGER NOT NULL REFERENCES " + company + ".integrations(id) ON DELETE CASCADE," +
                        " org_id VARCHAR NOT NULL, " +
                        " org VARCHAR," +
                        " project_id VARCHAR NOT NULL, " +
                        " issue_id VARCHAR, " +
                        " project VARCHAR NOT NULL, " +
                        " url VARCHAR, " +
                        " title VARCHAR, " +
                        " type VARCHAR, " +
                        " package VARCHAR, " +
                        " version VARCHAR, " +
                        " severity VARCHAR, " +
                        " language VARCHAR, " +
                        " package_manager VARCHAR, " +
                        " ignored BOOLEAN, " +
                        " patched BOOLEAN, " +
                        " exploit_maturity VARCHAR, " +
                        " upgradable BOOLEAN, " +
                        " patchable BOOLEAN, " +
                        " pinnable BOOLEAN, " +
                        " cvssv3 VARCHAR, " +
                        " cvssv_score NUMERIC, " +
                        " disclosure_time TIMESTAMP WITH TIME ZONE, " +
                        " publication_time TIMESTAMP WITH TIME ZONE, " +
                        " ingested_at TIMESTAMP WITH TIME ZONE, " +
                        " UNIQUE(integration_id, org_id, project_id, issue_id, ingested_at))",

                " CREATE INDEX IF NOT EXISTS " + SNYK_ISSUES + "_integration_id_build_num_compound_idx on "
                        + company + "." + SNYK_ISSUES + " (integration_id, org_id, project_id)",

                "CREATE TABLE IF NOT EXISTS " + company + "." + SNYK_ISSUES_PATCH +
                        " (id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " issue_id UUID NOT NULL REFERENCES " + company + "." + SNYK_ISSUES + "(id) ON DELETE CASCADE," +
                        " patch_id VARCHAR," +
                        " version VARCHAR," +
                        " modification_time TIMESTAMP WITH TIME ZONE," +
                        " UNIQUE(issue_id, patch_id))",

                " CREATE INDEX IF NOT EXISTS " + SNYK_ISSUES_PATCH + "_integration_id_build_num_compound_idx on "
                        + company + "." + SNYK_ISSUES_PATCH + " (issue_id)"
        );
        ddlStmts.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

    private String getWhereClause(List<String> conditions) {
        if (!conditions.isEmpty())
            return " WHERE " + String.join(" AND ", conditions);
        return EMPTY;
    }

    private void setPagingParams(Map<String, Object> params, Integer pageNumber, Integer pageSize) {
        params.put("offset", pageNumber * pageSize);
        params.put("limit", pageSize);
    }

    private <T> void insertList(Map<String, Object> params, List<String> conditions, String columnName, String key,
                                List<T> value) {
        if (CollectionUtils.isNotEmpty(value)) {
            conditions.add(columnName + " IN (:" + key + ")");
            params.put(key, value);
        }
    }

    private void insertBoolean(Map<String, Object> params, List<String> conditions, String columnName, String key,
                               String value) {
        if (StringUtils.isNotEmpty(value)) {
            conditions.add(columnName + " = :" + key);
            params.put(key, BooleanUtils.toBoolean(value));
        }
    }

    private void insertFloatRange(Map<String, Object> params, List<String> conditions, String columnName,String key,
                                  Map<String, String> value) {
        if (MapUtils.isNotEmpty(value)) {
            if (value.get("$gt") != null) {
                conditions.add(columnName + " > :gt_" + key);
                params.put("gt_" + key, NumberUtils.toFloat(value.get("$gt")));
            }
            if (value.get("$lt") != null) {
                conditions.add(columnName + " < :lt_" + key);
                params.put("lt_" + key, NumberUtils.toFloat(value.get("$lt")));
            }
        }
    }

    private void insertDateRange(Map<String, Object> params, List<String> conditions, String columnName, String key,
                                 Map<String, String> value) {
        if (MapUtils.isNotEmpty(value)) {
            if (value.get("$gt") != null) {
                conditions.add(columnName + " > :gt_" + key);
                params.put("gt_" + key, getTimeStamp(NumberUtils.toLong(value.get("$gt"))));
            }
            if (value.get("$lt") != null) {
                conditions.add(columnName + " < :lt_" + key);
                params.put("lt_" + key, getTimeStamp(NumberUtils.toLong(value.get("$lt"))));
            }
        }
    }

    private List<String> createWhereClauseAndUpdateParams(Map<String, Object> params, SnykIssuesFilter filter, String tableAlias) {
        tableAlias = StringUtils.isEmpty(tableAlias) ? "": tableAlias + ".";
        List<String> issuesConditions = new LinkedList<>();
        insertList(params, issuesConditions, "integration_id", "integration_ids",
                Optional.ofNullable(filter.getIntegrationIds())
                        .orElse(List.of()).stream()
                        .map(NumberUtils::toInt)
                        .collect(Collectors.toList()));
        insertList(params, issuesConditions,tableAlias + "org", "orgs", filter.getOrgs());
        insertList(params, issuesConditions,tableAlias + "project", "projects", filter.getProjects());
        insertList(params, issuesConditions,tableAlias + "title", "titles", filter.getTitles());
        insertList(params, issuesConditions,tableAlias + "type", "types", filter.getTypes());
        insertList(params, issuesConditions,tableAlias + "severity", "severities", filter.getSeverities());
        insertList(params, issuesConditions,tableAlias + "language", "languages", filter.getLanguages());
        insertList(params, issuesConditions,tableAlias + "version", "versions", filter.getVersions());
        insertList(params, issuesConditions,tableAlias + "package", "packages", filter.getPackageNames());
        insertList(params, issuesConditions,tableAlias + "cvssv3", "cvssv3", filter.getCvssv3());
        insertList(params, issuesConditions,tableAlias + "package_manager", "package_managers",
                filter.getPackageManagers());
        insertList(params, issuesConditions,"exploit_maturity", "exploit_maturities",
                filter.getExploitMaturities());
        insertBoolean(params, issuesConditions, tableAlias + "upgradable", "upgradable", filter.getUpgradable());
        insertBoolean(params, issuesConditions, tableAlias + "patchable", "patchable", filter.getPatchable());
        insertBoolean(params, issuesConditions,tableAlias + "pinnable", "pinnable", filter.getPinnable());
        insertBoolean(params, issuesConditions,tableAlias + "ignored", "ignored", filter.getIgnored());
        insertBoolean(params, issuesConditions,tableAlias + "patched", "patched", filter.getPatched());
        insertFloatRange(params, issuesConditions,tableAlias + "cvssv_score", "score", filter.getScoreRange());
        insertDateRange(params, issuesConditions, tableAlias + "disclosure_time", "disclosure_time",
                filter.getDisclosureDateRange());
        insertDateRange(params, issuesConditions, tableAlias + "publication_time", "publication_time",
                filter.getPublicationDateRange());
        return issuesConditions;
    }

    private void fillIssueIdsFromPreparedStatement(PreparedStatement statement, List<String> issueIds) throws SQLException{
        try (ResultSet rs = statement.getGeneratedKeys()) {
            while(rs.next())
                issueIds.add(rs.getString(1));
        }
    }

    private MapSqlParameterSource getPatchParams(SnykVulnerability.Patch patch, UUID issueUuid) {
        return new MapSqlParameterSource()
            .addValue("issueId", issueUuid)
            .addValue("patchId", patch.getId())
            .addValue("version", patch.getVersion())
            .addValue("modificationTime", getTimestamp(patch.getModificationTime()));
    }

    private MapSqlParameterSource getLicenseParams(DbSnykIssues dbIssues, SnykLicenseIssue license, Integer integrationId) {
        return new MapSqlParameterSource()
            .addValue("integrationId", integrationId)
            .addValue("orgId", dbIssues.getOrgId())
            .addValue("org", dbIssues.getOrg())
            .addValue("projectId", dbIssues.getProjectId())
            .addValue("issueId", license.getId())
            .addValue("project", Optional.ofNullable(dbIssues.getProjectName()).orElse(dbIssues.getProjectId()))
            .addValue("url", license.getUrl())
            .addValue("title", license.getTitle())
            .addValue("type", license.getType())
            .addValue("package", license.getPackageName())
            .addValue("version", license.getVersion())
            .addValue("severity", license.getSeverity())
            .addValue("language", license.getLanguage())
            .addValue("packageManager", license.getPackageManager())
            .addValue("ignored", Optional.ofNullable(license.getIgnored()).orElse(false))
            .addValue("patched", Optional.ofNullable(license.getPatched()).orElse(false))
            .addValue("exploitMaturity", null)
            .addValue("upgradable", null)
            .addValue("patchable", null)
            .addValue("pinnable", null)
            .addValue("cvssv3", null)
            .addValue("cvssvScore", null)
            .addValue("disclosureTime", null)
            .addValue("publicationTime", null)
            .addValue("ingestedAt", getTimestamp(dbIssues.getIngestedAt()));
    }

    private MapSqlParameterSource getVulnerabilityParams(DbSnykIssues dbIssues, SnykVulnerability vulnerability, Integer integrationId){
        return new MapSqlParameterSource()
            .addValue("integrationId", integrationId)
            .addValue("orgId", dbIssues.getOrgId())
            .addValue("org", dbIssues.getOrg())
            .addValue("projectId", dbIssues.getProjectId())
            .addValue("issueId", vulnerability.getId())
            .addValue("project", Optional.ofNullable(dbIssues.getProjectName()).orElse(dbIssues.getProjectId()))
            .addValue("url", vulnerability.getUrl())
            .addValue("title", vulnerability.getTitle())
            .addValue("type", vulnerability.getType())
            .addValue("package", vulnerability.getPackageName())
            .addValue("version", vulnerability.getVersion())
            .addValue("severity", vulnerability.getSeverity())
            .addValue("language", vulnerability.getLanguage())
            .addValue("packageManager", vulnerability.getPackageManager())
            .addValue("ignored", Optional.ofNullable(vulnerability.getIgnored()).orElse(false))
            .addValue("patched", Optional.ofNullable(vulnerability.getPatched()).orElse(false))
            .addValue("exploitMaturity", vulnerability.getExploitMaturity())
            .addValue("upgradable", vulnerability.getUpgradable())
            .addValue("patchable", vulnerability.getPatchable())
            .addValue("pinnable", vulnerability.getPinnable())
            .addValue("cvssv3", vulnerability.getCvssv3())
            .addValue("cvssvScore", vulnerability.getCvssScore())
            .addValue("disclosureTime", getTimestamp(vulnerability.getDisclosureTime()))
            .addValue("publicationTime", getTimestamp(vulnerability.getPublicationTime()))
            .addValue("ingestedAt", getTimestamp(dbIssues.getIngestedAt()));
    }

    private Timestamp getTimestamp(Date date) {
        return date != null ? Timestamp.from(date.toInstant()) : null;
    }

    private Timestamp getTimeStamp(Long date) {
        return new Timestamp(date);
    }
}
