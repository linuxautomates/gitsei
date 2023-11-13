package io.levelops.commons.databases.services.blackduck;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.databases.converters.DbBlackDuckConvertors;
import io.levelops.commons.databases.models.database.blackduck.DbBlackDuckIssue;
import io.levelops.commons.databases.models.database.blackduck.DbBlackDuckProject;
import io.levelops.commons.databases.models.database.blackduck.DbBlackDuckProjectVersion;
import io.levelops.commons.databases.models.database.blackduck.DbBlackDuckVersion;
import io.levelops.commons.databases.models.filters.BlackDuckIssueFilter;
import io.levelops.commons.databases.models.filters.BlackDuckProjectFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.blackduck.BlackDuckUtils.insertDateRange;
import static io.levelops.commons.databases.services.blackduck.BlackDuckUtils.insertFloatRange;
import static io.levelops.commons.databases.services.blackduck.BlackDuckUtils.insertList;
import static io.levelops.commons.databases.services.blackduck.BlackDuckUtils.insertUUID;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Log4j2
@Service
public class BlackDuckDatabaseService extends DatabaseService<DbBlackDuckProject> {

    public final static String BD_PROJECTS_TABLE = "blackduck_projects";
    public final static String BD_VERSIONS_TABLE = "blackduck_versions";
    public final static String BD_ISSUES_TABLE = "blackduck_issues";


    private final NamedParameterJdbcTemplate template;

    public BlackDuckDatabaseService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbBlackDuckProject project) throws SQLException {
        return template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            String projectSql = "INSERT INTO " + company + "." + BD_PROJECTS_TABLE +
                    " (integration_id,name,description,attributes,project_created_at,project_updated_at,updated_at) " +
                    " VALUES (?,?,?,to_json(?::json),?,?,EXTRACT(epoch FROM now())) ON CONFLICT (integration_id,name)" +
                    " DO UPDATE SET (description,attributes,project_created_at,project_updated_at,updated_at) " +
                    " = (EXCLUDED.description,EXCLUDED.attributes,EXCLUDED.project_created_at,EXCLUDED.project_updated_at," +
                    " trunc(extract(epoch from now()))" +
                    " ) RETURNING id";

            try (PreparedStatement insertProject = conn.prepareStatement(projectSql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 0;
                insertProject.setObject(++i, NumberUtils.toInt(project.getIntegrationId()));
                insertProject.setObject(++i, project.getName());
                insertProject.setObject(++i, project.getDescription());
                try {
                    insertProject.setObject(++i, DefaultObjectMapper.get().writeValueAsString(project.getAttributes()));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize attributes json. will store empty json.", e);
                    insertProject.setObject(i - 1, "{}");
                }
                insertProject.setObject(++i, getTimestamp(project.getProjCreatedAt()));
                insertProject.setObject(++i, getTimestamp(project.getProjUpdatedAt()));
                int insertedRows = insertProject.executeUpdate();
                if (insertedRows == 0)
                    throw new SQLException("Failed to insert project.");
                String insertedRowId = null;
                try (ResultSet rs = insertProject.getGeneratedKeys()) {
                    if (rs.next())
                        insertedRowId = rs.getString(1);
                }
                if (insertedRowId == null)
                    throw new SQLException("Failed to get inserted project's Id.");
                return insertedRowId;
            }
        }));
    }

    public String insertVersions(String company, DbBlackDuckVersion version) {
        return template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            String versionSql = "INSERT INTO " + company + "." + BD_VERSIONS_TABLE +
                    " (project_id,name,release_date,source,attributes,security_risks,operational_risks,license_risks,version_created_at," +
                    " updated_at)  VALUES (?,?,?,?,to_json(?::json),to_json(?::json),to_json(?::json),to_json(?::json)" +
                    " ,?,EXTRACT(epoch FROM now())) ON CONFLICT (project_id,name)" +
                    " DO UPDATE SET (release_date,source,attributes,security_risks,operational_risks,license_risks," +
                    " version_created_at,updated_at) " +
                    " = (EXCLUDED.release_date,EXCLUDED.source,EXCLUDED.attributes,EXCLUDED.security_risks," +
                    " EXCLUDED.operational_risks,EXCLUDED.license_risks,EXCLUDED.version_created_at," +
                    " trunc(extract(epoch from now())) ) RETURNING id";
            try (PreparedStatement insertVersion = conn.prepareStatement(versionSql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 0;
                insertVersion.setObject(++i, UUID.fromString(version.getProjectId()));
                insertVersion.setObject(++i, version.getVersionName());
                insertVersion.setObject(++i, getTimestamp(version.getReleaseDate()));
                insertVersion.setObject(++i, version.getSource());
                try {
                    insertVersion.setObject(++i, DefaultObjectMapper.get().writeValueAsString(version.getVersionAttributes()));
                    insertVersion.setObject(++i, DefaultObjectMapper.get().writeValueAsString(version.getSecurityRiskProfile()));
                    insertVersion.setObject(++i, DefaultObjectMapper.get().writeValueAsString(version.getOperationalRiskProfile()));
                    insertVersion.setObject(++i, DefaultObjectMapper.get().writeValueAsString(version.getLicenseRiskProfile()));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize json. will store empty json.", e);
                    insertVersion.setObject(i - 4, "{}");
                }
                insertVersion.setObject(++i, getTimestamp(version.getVersionCreatedAt()));
                int insertedRows = insertVersion.executeUpdate();
                if (insertedRows == 0)
                    throw new SQLException("Failed to insert version.");
                String insertedRowId = null;
                try (ResultSet rs = insertVersion.getGeneratedKeys()) {
                    if (rs.next())
                        insertedRowId = rs.getString(1);
                }
                if (insertedRowId == null)
                    throw new SQLException("Failed to get inserted version's Id.");
                return insertedRowId;
            }
        }));
    }

    public String insertIssues(String company, DbBlackDuckIssue issue) {
        return template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            String versionSql = "INSERT INTO " + company + "." + BD_ISSUES_TABLE +
                    "(version_id,description,component_name,component_version_name,vulnerability_name,vulnerability_published_at" +
                    ",vulnerability_updated_at,base_score,overall_score,exploitability_subscore,impact_subscore,source," +
                    "severity,remediation_status,cwe_id,bdsa_tags,related_vulnerability,remediation_created_at,remediation_updated_at,updated_at)" +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,EXTRACT(epoch FROM now())) ON CONFLICT (version_id,vulnerability_name)" +
                    " DO UPDATE SET (description,component_name,component_version_name,vulnerability_published_at, vulnerability_updated_at" +
                    ",base_score, overall_score,exploitability_subscore,impact_subscore,source,severity,remediation_status" +
                    ",cwe_id,bdsa_tags,related_vulnerability,remediation_created_at,remediation_updated_at,updated_at) " +
                    " = (EXCLUDED.description,EXCLUDED.component_name,EXCLUDED.component_version_name,EXCLUDED.vulnerability_published_at, " +
                    "EXCLUDED.vulnerability_updated_at,EXCLUDED.base_score, EXCLUDED.overall_score,EXCLUDED.exploitability_subscore,EXCLUDED.impact_subscore," +
                    "EXCLUDED.source,EXCLUDED.severity,EXCLUDED.remediation_status,EXCLUDED.cwe_id,EXCLUDED.bdsa_tags,EXCLUDED.related_vulnerability," +
                    "EXCLUDED.remediation_created_at,EXCLUDED.remediation_updated_at,trunc(extract(epoch from now()))" +
                    " ) RETURNING id";
            try (PreparedStatement insertIssue = conn.prepareStatement(versionSql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 0;
                insertIssue.setObject(++i, UUID.fromString(issue.getVersionId()));
                insertIssue.setObject(++i, issue.getDescription());
                insertIssue.setObject(++i, issue.getComponentName());
                insertIssue.setObject(++i, issue.getComponentVersionName());
                insertIssue.setObject(++i, issue.getVulnerabilityName());
                insertIssue.setObject(++i, getTimestamp(issue.getVulnerabilityPublishedAt()));
                insertIssue.setObject(++i, getTimestamp(issue.getVulnerabilityUpdatedAt()));
                insertIssue.setObject(++i, issue.getBaseScore());
                insertIssue.setObject(++i, issue.getOverallScore());
                insertIssue.setObject(++i, issue.getExploitabilitySubScore());
                insertIssue.setObject(++i, issue.getImpactSubScore());
                insertIssue.setObject(++i, issue.getSource());
                insertIssue.setObject(++i, issue.getSeverity());
                insertIssue.setObject(++i, issue.getRemediationStatus());
                insertIssue.setObject(++i, issue.getCwdId());
                insertIssue.setObject(++i, issue.getBdsaTags().toArray(new String[0]));
                insertIssue.setObject(++i, issue.getRelatedVulnerability());
                insertIssue.setObject(++i, getTimestamp(issue.getRemediationCreatedAt()));
                insertIssue.setObject(++i, getTimestamp(issue.getRemediationUpdatedAt()));
                int insertedRows = insertIssue.executeUpdate();
                if (insertedRows == 0)
                    throw new SQLException("Failed to insert project.");
                String insertedRowId = null;
                try (ResultSet rs = insertIssue.getGeneratedKeys()) {
                    if (rs.next())
                        insertedRowId = rs.getString(1);
                }
                if (insertedRowId == null)
                    throw new SQLException("Failed to get inserted project's Id.");
                return insertedRowId;
            }
        }));
    }

    public Optional<DbBlackDuckProjectVersion> getProject(String company, String name, String integrationId) {
        Validate.notBlank(name, "Missing name.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + BD_PROJECTS_TABLE
                + " WHERE name = :name AND integration_id = :integid";
        Map<String, Object> params = Map.of("name", name,
                "integid", NumberUtils.toInt(integrationId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbBlackDuckProjectVersion> data = template.query(sql, params, DbBlackDuckConvertors.projectRowMapper());
        return data.stream().findFirst();
    }

    @Override
    public Boolean update(String company, DbBlackDuckProject t) throws SQLException {
        return null;
    }

    @Override
    public Optional<DbBlackDuckProject> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    @Override
    public DbListResponse<DbBlackDuckProject> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return null;
    }

    public DbListResponse<DbBlackDuckProjectVersion> listProjects(String company, BlackDuckProjectFilter projectFilter,
                                                                  Integer pageNumber, Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        List<String> projectConditions = createProjectWhereClauseAndUpdateParams(params, projectFilter, "proj");
        List<String> versionConditions = createVersionWhereClauseAndUpdateParams(params, projectFilter, "ver");
        setPagingParams(params, pageNumber, pageSize);
        String limitClause = " OFFSET :offset LIMIT :limit";
        String whereProjectClause = getWhereClause(projectConditions);
        String whereVersionClause = getWhereClause(versionConditions);
        String versionsTableJoinStmt = " INNER JOIN " + company + "." + BD_VERSIONS_TABLE + " as ver ON proj.id = ver.project_id ";
        String selectQuery = "SELECT *, proj.name as proj_name, proj.attributes as proj_attributes, ver.attributes as ver_attributes" +
                " FROM " + company + "." + BD_PROJECTS_TABLE + " as proj " + versionsTableJoinStmt;
        String sqlQuery = selectQuery + whereProjectClause + whereVersionClause + limitClause;
        List<DbBlackDuckProjectVersion> issues = template.query(sqlQuery, params, DbBlackDuckConvertors.projectRowMapper());
        String countSql = "SELECT COUNT(*) FROM (" + sqlQuery + ") AS x";
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(issues, count);

    }

    public DbListResponse<DbBlackDuckIssue> listIssues(String company,
                                                       BlackDuckIssueFilter issueFilter,
                                                       int pageNumber,
                                                       int pageSize) {
        Map<String, Object> params = new HashMap<>();
        List<String> conditions = createIssuesWhereClauseAndUpdateParams(params, issueFilter, null);
        setPagingParams(params, pageNumber, pageSize);
        String limitClause = " OFFSET :offset LIMIT :limit";
        String whereClause = getWhereClause(conditions);
        String selectQuery = "SELECT * FROM " + company + "." + BD_ISSUES_TABLE + " ";
        String query = selectQuery + whereClause;
        String sqlQuery = query + limitClause;
        log.info("sql : {}", sqlQuery);
        log.info("params : {}", params);
        List<DbBlackDuckIssue> issues = template.query(sqlQuery, params, DbBlackDuckConvertors.issueRowMapper());
        String countSql = "SELECT COUNT(*) FROM (" + query + ") AS x";
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(issues, count);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateIssues(String company, BlackDuckIssueFilter filter,
                                                                         String configTableKey) {
        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        BlackDuckIssueFilter.CALCULATION calculation = filter.getCalculation();
        BlackDuckIssueFilter.DISTINCT distinct = filter.getAcross();
        if (StringUtils.isNotEmpty(configTableKey))
            distinct = BlackDuckIssueFilter.DISTINCT.none;
        if (calculation == null) {
            calculation = BlackDuckIssueFilter.CALCULATION.total_issues;
        }
        String aggColumn, orderBySql, aggName;
        switch (calculation) {
            case overall_score:
                aggName = "overall_score";
                aggColumn = " MIN(" + aggName + ") AS mn, MAX(" + aggName + ") as mx, COUNT(*) AS ct, " +
                        "PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY " + aggName + "), AVG(" + aggName + ") AS mean";
                orderBySql = " ORDER BY mx DESC ";
                break;
            case total_issues:
            case count:
            default:
                aggColumn = " COUNT(issues.id) AS ct ";
                orderBySql = " ORDER BY ct DESC ";
                break;
        }
        String groupByKey;
        switch (distinct) {
            case vulnerability:
                groupByKey = "vulnerability_name";
                break;
            case severity:
                groupByKey = "severity";
                break;
            case component:
                groupByKey = "component_name";
                break;
            case cweId:
                groupByKey = "cwe_id";
                break;
            case vulnerability_created_at:
                groupByKey = "vulnerability_published_at";
                break;
            default:
                groupByKey = distinct.name();
        }
        Map<String, Object> params = new HashMap<>();
        List<String> issueConditions = createIssuesWhereClauseAndUpdateParams(params, filter, null);
        String issueWhereClause = getWhereClause(issueConditions);
        Boolean isVersionsJoinRequired = isJoinRequiredWithVersions(filter);
        String versionsWhereClause = "";
        if(isVersionsJoinRequired){
            List<String> versionConditions = createVersionWhereClauseFromIssuesFilter(params, filter, null);
            versionsWhereClause = getWhereClause(versionConditions);
        }
        Boolean isProjectsJoinRequired = isJoinRequiredWithProjects(filter);
        String projectsWhereClause = "";
        if(isProjectsJoinRequired){
            List<String>  projectConditions = createProjectWhereClauseFromIssuesFilter(params, filter, null);
            projectsWhereClause = getWhereClause(projectConditions);
        }
        String groupBySql = distinct.equals(BlackDuckIssueFilter.DISTINCT.none) ? "" : " GROUP BY " + groupByKey;
        String fromIssueTable = " FROM (SELECT * FROM " + company + "." + BD_ISSUES_TABLE + issueWhereClause + ") AS issues ";
        String joinVersionTable = isVersionsJoinRequired ? getVersionJoinStmt(company,versionsWhereClause) : "";
        String joinProjectTable = isProjectsJoinRequired ? getProectJoinStmt(company,projectsWhereClause) : "";
        String groupByCol = groupByKey.equals("phase") ? "versions.attributes->>'phase' as phase": groupByKey;
        String query = "SELECT " + groupByCol + ", " + aggColumn + fromIssueTable + joinVersionTable + joinProjectTable + groupBySql + orderBySql;
        List<DbAggregationResult> aggregationResults = template.query(query, params, DbBlackDuckConvertors.aggRowMapper(groupByKey, calculation));
        return DbListResponse.of(aggregationResults, aggregationResults.size());
    }

    private Boolean isJoinRequiredWithVersions(BlackDuckIssueFilter filter) {
        BlackDuckIssueFilter.DISTINCT distinct = filter.getAcross();
        return  isJoinRequiredWithProjects(filter) ||
                distinct.equals(BlackDuckIssueFilter.DISTINCT.version) ||
                distinct.equals(BlackDuckIssueFilter.DISTINCT.phase)||
                (filter.getVersions() != null && CollectionUtils.isNotEmpty(filter.getVersions())) ||
                (filter.getPhases() != null && CollectionUtils.isNotEmpty(filter.getPhases()));

    }

    private Boolean isJoinRequiredWithProjects(BlackDuckIssueFilter filter) {
        BlackDuckIssueFilter.DISTINCT distinct = filter.getAcross();
        return  distinct.equals(BlackDuckIssueFilter.DISTINCT.project) ||
                (filter.getProjects() != null && CollectionUtils.isNotEmpty(filter.getProjects()));
    }

    private String getProectJoinStmt(String company, String projectsWhereClause) {
        return " INNER JOIN ( SELECT name as project, * FROM " + company + "." +BD_PROJECTS_TABLE + projectsWhereClause + ") AS projects ON versions.project_id = projects.id";
    }

    private String getVersionJoinStmt(String company, String versionsWhereClause) {
        return " INNER JOIN ( SELECT name as version, * FROM " + company + "." +BD_VERSIONS_TABLE + versionsWhereClause + ") AS versions ON issues.version_id = versions.id";
    }


    private List<String> createIssuesWhereClauseAndUpdateParams(Map<String, Object> params,
                                                          BlackDuckIssueFilter filter,
                                                          String tableAlias) {
        tableAlias = StringUtils.isEmpty(tableAlias) ? "" : tableAlias + ".";
        List<String> issuesConditions = new LinkedList<>();
        insertUUID(params, issuesConditions, tableAlias + "version_id", "version_id", filter.getVersionIds());
        insertList(params, issuesConditions, tableAlias + "component_name", "component_name", filter.getComponentNames());
        insertList(params, issuesConditions, tableAlias + "component_version_name", "component_version_name", filter.getComponentVersionNames());
        insertList(params, issuesConditions, tableAlias + "vulnerability_name", "vulnerability_name", filter.getVulnerabilities());
        insertDateRange(issuesConditions, tableAlias + "vulnerability_published_at", filter.getVulnerabilityPublishedAtRange());
        insertDateRange(issuesConditions, tableAlias + "vulnerability_published_at", filter.getVulnerabilityUpdatedAtRange());
        insertFloatRange(params, issuesConditions, tableAlias + "base_score", "base_score", filter.getBaseScoreRange());
        insertFloatRange(params, issuesConditions, tableAlias + "overall_score", "overall_score", filter.getOverallScoreRange());
        insertFloatRange(params, issuesConditions, tableAlias + "exploitability_subscore", "exploitability_score", filter.getExploitabilitySubScoreRange());
        insertFloatRange(params, issuesConditions, tableAlias + "impact_subscore", "impact_score", filter.getImpactSubScoreRange());
        insertList(params, issuesConditions, tableAlias + "source", "source", filter.getSources());
        insertList(params, issuesConditions, tableAlias + "severity", "severity", filter.getSeverities());
        insertList(params, issuesConditions, tableAlias + "remediation_status", "remediation_status", filter.getRemediationStatuses());
        insertList(params, issuesConditions, tableAlias + "cwe_id", "cwe_id", filter.getCweIds());
        insertDateRange(issuesConditions, tableAlias + "remediation_created_at", filter.getRemediationCreatedAtRange());
        insertDateRange(issuesConditions, tableAlias + "remediation_updated_at", filter.getVulnerabilityUpdatedAtRange());
        return issuesConditions;
    }

    private List<String> createProjectWhereClauseAndUpdateParams(Map<String, Object> params,
                                                                 BlackDuckProjectFilter filter,
                                                                 String tableAlias) {
        tableAlias = StringUtils.isEmpty(tableAlias) ? "" : tableAlias + ".";
        List<String> projectConditions = new LinkedList<>();
        insertList(params, projectConditions, "integration_id", "integration_ids",
                Optional.ofNullable(filter.getIntegrationIds())
                        .orElse(List.of()).stream()
                        .map(NumberUtils::toInt)
                        .collect(Collectors.toList()));
        insertList(params, projectConditions, tableAlias + "name", "name", filter.getProjects());
        insertDateRange(projectConditions, tableAlias + "project_created_at", filter.getProjectCreatedRange());
        insertDateRange(projectConditions, tableAlias + "project_updated_at", filter.getProjectUpdatedRange());
        return projectConditions;
    }

    private List<String> createVersionWhereClauseAndUpdateParams(Map<String, Object> params,
                                                                 BlackDuckProjectFilter filter,
                                                                 String tableAlias) {
        tableAlias = StringUtils.isEmpty(tableAlias) ? "" : tableAlias + ".";
        List<String> versionConditions = new LinkedList<>();
        insertList(params, versionConditions, tableAlias + "name", "name", filter.getVersions());
        insertList(params, versionConditions, tableAlias + "source", "component_name", filter.getSources());
        insertDateRange(versionConditions, tableAlias + "version_created_at", filter.getVersionCreatedRange());
        insertDateRange(versionConditions, tableAlias + "released_date", filter.getVersionReleasedOnRange());
        return versionConditions;
    }

    private List<String> createVersionWhereClauseFromIssuesFilter(Map<String, Object> params, BlackDuckIssueFilter filter, String tableAlias) {
        tableAlias = StringUtils.isEmpty(tableAlias) ? "" : tableAlias + ".";
        List<String> versionConditions = new LinkedList<>();
        insertList(params, versionConditions, tableAlias + "name", "name", filter.getVersions());
        insertList(params, versionConditions, tableAlias + "phase", "name", filter.getPhases());
        return versionConditions;
    }

    private List<String> createProjectWhereClauseFromIssuesFilter(Map<String, Object> params, BlackDuckIssueFilter filter, String tableAlias) {
        tableAlias = StringUtils.isEmpty(tableAlias) ? "" : tableAlias + ".";
        List<String> projectConditions = new LinkedList<>();
        insertList(params, projectConditions, "integration_id", "integration_ids",
                Optional.ofNullable(filter.getIntegrationIds())
                        .orElse(List.of()).stream()
                        .map(NumberUtils::toInt)
                        .collect(Collectors.toList()));
        insertList(params, projectConditions, tableAlias + "name", "name", filter.getProjects());
        return projectConditions;
    }


    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return null;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + "." + BD_PROJECTS_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    name VARCHAR NOT NULL,\n" +
                        "    description VARCHAR,\n" +
                        "    attributes JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "    project_created_at TIMESTAMP,\n" +
                        "    project_updated_at TIMESTAMP,\n" +
                        "    created_at BIGINT DEFAULT extract(epoch from now()),\n" +
                        "    updated_at BIGINT DEFAULT extract(epoch from now()),\n" +
                        "    UNIQUE (name, integration_id)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + BD_PROJECTS_TABLE + "_bd_id_project_integration_compound_idx " +
                        "on " + company + "." + BD_PROJECTS_TABLE + "(name,integration_id)",
                "CREATE TABLE IF NOT EXISTS " + company + "." + BD_VERSIONS_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    project_id UUID NOT NULL REFERENCES "
                        + company + "." + BD_PROJECTS_TABLE + "(id) ON DELETE CASCADE,\n" +
                        "    name VARCHAR NOT NULL,\n" +
                        "    release_date TIMESTAMP,\n" +
                        "    source VARCHAR NOT NULL,\n" +
                        "    attributes JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "    security_risks JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "    operational_risks JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "    license_risks JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "    version_created_at TIMESTAMP,\n" +
                        "    created_at BIGINT DEFAULT extract(epoch from now()),\n" +
                        "    updated_at BIGINT DEFAULT extract(epoch from now())," +
                        "    UNIQUE (name, project_id)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + BD_VERSIONS_TABLE + "_bd_version_name_compound_idx " +
                        "on " + company + "." + BD_VERSIONS_TABLE + "(project_id,name)",
                "CREATE TABLE IF NOT EXISTS " + company + "." + BD_ISSUES_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    version_id UUID NOT NULL REFERENCES \n"
                        + company + "." + BD_VERSIONS_TABLE + "(id) ON DELETE CASCADE,\n" +
                        "    description VARCHAR NOT NULL,\n" +
                        "    component_name VARCHAR NOT NULL,\n" +
                        "    component_version_name VARCHAR NOT NULL,\n" +
                        "    vulnerability_name VARCHAR NOT NULL,\n" +
                        "    vulnerability_published_at TIMESTAMP,\n" +
                        "    vulnerability_updated_at TIMESTAMP,\n" +
                        "    base_score DEC(5,2) NOT NULL,\n" +
                        "    overall_score DEC(5,2) NOT NULL,\n" +
                        "    exploitability_subscore DEC(5,2) NOT NULL,\n" +
                        "    impact_subscore DEC(5,2) NOT NULL,\n" +
                        "    source VARCHAR NOT NULL,\n" +
                        "    severity VARCHAR NOT NULL,\n" +
                        "    remediation_status VARCHAR,\n" +
                        "    cwe_id VARCHAR,\n" +
                        "    bdsa_tags VARCHAR[],\n" +
                        "    related_vulnerability VARCHAR,\n" +
                        "    remediation_created_at TIMESTAMP,\n" +
                        "    remediation_updated_at TIMESTAMP,\n" +
                        "    created_at BIGINT DEFAULT extract(epoch from now()),\n" +
                        "    updated_at BIGINT DEFAULT extract(epoch from now())," +
                        "    UNIQUE (version_id,vulnerability_name)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + BD_ISSUES_TABLE + "_issue_project_compound_idx " +
                        "on " + company + "." + BD_ISSUES_TABLE + "(version_id,vulnerability_name)"
        );
        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

    private Timestamp getTimestamp(Date date) {
        return date != null ? Timestamp.from(date.toInstant()) : null;
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


}
