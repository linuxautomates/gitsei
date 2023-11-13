package io.levelops.commons.databases.services;

import io.levelops.commons.databases.converters.SonarQubeProjectConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeBranch;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeCoverage;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeMeasure;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeProject;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubePullRequest;
import io.levelops.commons.databases.models.filters.SonarQubeMetricFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ListUtils;
import io.levelops.integrations.sonarqube.models.Analyse;
import io.levelops.integrations.sonarqube.models.Event;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Class to perform Database operations like insert, group by, select queries etc on sonarqube_projects,sonarqube
 * _project_analyses and sonarqube_project_pull_requests tables.
 */

@Log4j2
@Service
public class SonarQubeProjectService extends DatabaseService<DbSonarQubeProject> {

    private static final String SONARQUBE_PROJECTS = "sonarqube_projects";
    private static final String SONARQUBE_PROJECT_PULL_REQUESTS = "sonarqube_project_pull_requests";
    private static final String SONARQUBE_PROJECTS_ANALYSES = "sonarqube_project_analyses";
    private static final String SONARQUBE_METRICS = "sonarqube_metrics";
    private static final String SONARQUBE_PROJECT_BRANCHES = "sonarqube_project_branches";
    private static final int ANALYSES_BATCH_SIZE = 500;
    private static final int MEASURES_BATCH_SIZE = 10000;

    private static final List<String> NUMERIC_METRICS = List.of("PERCENT", "MILLISEC", "FLOAT", "WORK_DUR", "INT", "RATING");

    private static final Set<SonarQubeMetricFilter.DISTINCT> SUPPORTED_STACKS = Set.of(
            SonarQubeMetricFilter.DISTINCT.organization,
            SonarQubeMetricFilter.DISTINCT.project,
            SonarQubeMetricFilter.DISTINCT.visibility,
            SonarQubeMetricFilter.DISTINCT.pull_request,
            SonarQubeMetricFilter.DISTINCT.pr_branch,
            SonarQubeMetricFilter.DISTINCT.pr_target_branch,
            SonarQubeMetricFilter.DISTINCT.pr_base_branch);

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public SonarQubeProjectService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * Upsert a project and all of its associated components.
     * </p>
     * Warning: This is mostly for backward compatibility.
     * Each object can also be inserted separately, including the project itself. {@code insertProject()}
     * To handle errors individually, do not call this method directly.
     */
    @Override
    public String insert(String company, DbSonarQubeProject project) throws SQLException {
        String projectId = insertProject(company, project);

        List<DbSonarQubeMeasure> measures = new ArrayList<>(DbSonarQubeMeasure.addParentIdToBatch(projectId, project.getMeasures()));

        for (DbSonarQubeBranch branch : ListUtils.emptyIfNull(project.getBranches())) {
            String branchId = insertBranch(company, branch.toBuilder()
                    .projectId(projectId)
                    .build());
            measures.addAll(DbSonarQubeMeasure.addParentIdToBatch(branchId, branch.getMeasures()));
        }

        for (DbSonarQubePullRequest pr : ListUtils.emptyIfNull(project.getPullRequests())) {
            String prId = insertPR(company, pr.toBuilder()
                    .projectId(projectId)
                    .build());
            measures.addAll(DbSonarQubeMeasure.addParentIdToBatch(prId, pr.getMeasures()));
        }

        batchInsertAnalyses(company, projectId, project.getAnalyses());
        batchInsertMeasures(company, measures);

        return projectId;
    }

    public String insertProject(String company, DbSonarQubeProject project) throws SQLException {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(project.getIntegrationId(), "project.getIntegrationId() cannot be null or empty.");
        Validate.notBlank(project.getKey(), "project.getKey() cannot be null or empty.");

        String insertProject = "INSERT INTO " + company + "." + SONARQUBE_PROJECTS + " AS t " +
                " (integration_id, organization, key, name, visibility, last_analysis_date, revision)" +
                " VALUES (:integration_id, :organization, :key, :name, :visibility, :last_analysis_date, :revision) " +
                " ON CONFLICT (key, integration_id) DO UPDATE SET " +
                "   visibility = EXCLUDED.visibility, " +
                "   organization = EXCLUDED.organization, " +
                "   last_analysis_date = EXCLUDED.last_analysis_date, " +
                "   name = EXCLUDED.name, " +
                "   revision = EXCLUDED.revision " +
                " WHERE (t.visibility, t.organization, t.last_analysis_date, t.name, t.revision)" +
                " IS DISTINCT FROM (EXCLUDED.visibility, EXCLUDED.organization, EXCLUDED.last_analysis_date, EXCLUDED.name, EXCLUDED.revision)" +
                " RETURNING id";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("integration_id", NumberUtils.toInt(project.getIntegrationId()));
        params.addValue("organization", project.getOrganization());
        params.addValue("key", project.getKey());
        params.addValue("name", project.getName());
        params.addValue("visibility", project.getVisibility());
        params.addValue("last_analysis_date", DateUtils.toTimestamp(project.getLastAnalysisDate()));
        params.addValue("revision", project.getRevision());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(insertProject, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return this.getProjectId(company, project.getKey(), project.getIntegrationId())
                    .orElseThrow(() -> new SQLException("Failed to get project id"));
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    public String insertBranch(String company, DbSonarQubeBranch branch) throws SQLException {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(branch.getProjectId(), "branch.getProjectId() cannot be null or empty.");
        Validate.notBlank(branch.getName(), "branch.getName() cannot be null or empty.");

        String insertBranch = "INSERT INTO " + company + "." + SONARQUBE_PROJECT_BRANCHES + " AS t " +
                " (project_id, name, quality_gate_status, bugs, vulnerabilities, code_smells, analysis_date) " +
                " VALUES (:project_id, :name, :quality_gate_status, :bugs, :vulnerabilities, :code_smells, :analysis_date) " +
                " ON CONFLICT (project_id, name) DO UPDATE SET " +
                "   quality_gate_status = EXCLUDED.quality_gate_status," +
                "   bugs = EXCLUDED.bugs," +
                "   vulnerabilities = EXCLUDED.vulnerabilities," +
                "   code_smells = EXCLUDED.code_smells," +
                "   analysis_date = EXCLUDED.analysis_date " +
                " WHERE (t.quality_gate_status, t.bugs, t.vulnerabilities, t.code_smells, t.analysis_date)" +
                " IS DISTINCT FROM (EXCLUDED.quality_gate_status, EXCLUDED.bugs, EXCLUDED.vulnerabilities, EXCLUDED.code_smells, EXCLUDED.analysis_date)" +
                " RETURNING id";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("project_id", UUID.fromString(branch.getProjectId()));
        params.addValue("name", branch.getName());
        params.addValue("quality_gate_status", branch.getQualityGateStatus());
        params.addValue("bugs", branch.getBugs());
        params.addValue("vulnerabilities", branch.getVulnerabilities());
        params.addValue("code_smells", branch.getCodeSmells());
        params.addValue("analysis_date", DateUtils.toTimestamp(branch.getAnalysisDate()));

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(insertBranch, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return this.getBranchId(company, branch.getName(), branch.getProjectId())
                    .orElseThrow(() -> new SQLException("Failed to get branch id"));
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    public String insertPR(String company, DbSonarQubePullRequest pr) throws SQLException {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(pr.getKey(), "pr.getKey() cannot be null or empty.");
        Validate.notBlank(pr.getProjectId(), "pr.getProjectId() cannot be null or empty.");

        String insertPR = "INSERT INTO " + company + "." + SONARQUBE_PROJECT_PULL_REQUESTS + " AS t " +
                " (project_id, key, title, branch, base_branch, target_branch, quality_gate_status, bugs, vulnerabilities, code_smells, analysis_date, url) " +
                " VALUES (:project_id, :key, :title, :branch, :base_branch, :target_branch, :quality_gate_status, :bugs, :vulnerabilities, :code_smells, :analysis_date, :url) " +
                " ON CONFLICT (key, project_id) DO UPDATE SET" +
                "   title = EXCLUDED.title," +
                "   branch = EXCLUDED.branch, " +
                "   base_branch = EXCLUDED.base_branch," +
                "   target_branch = EXCLUDED.target_branch," +
                "   quality_gate_status = EXCLUDED.quality_gate_status, " +
                "   bugs = EXCLUDED.bugs," +
                "   vulnerabilities = EXCLUDED.vulnerabilities," +
                "   code_smells = EXCLUDED.code_smells," +
                "   analysis_date = EXCLUDED.analysis_date, " +
                "   url = EXCLUDED.url" +
                " WHERE (t.title, t.branch, t.base_branch, t.target_branch, t.quality_gate_status, t.bugs, t.vulnerabilities, t.code_smells, t.analysis_date, t.url)" +
                " IS DISTINCT FROM (EXCLUDED.title, EXCLUDED.branch, EXCLUDED.base_branch, EXCLUDED.target_branch, EXCLUDED.quality_gate_status, EXCLUDED.bugs, EXCLUDED.vulnerabilities, EXCLUDED.code_smells, EXCLUDED.analysis_date, EXCLUDED.url)" +
                " RETURNING id";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("project_id", UUID.fromString(pr.getProjectId()));
        params.addValue("key", pr.getKey());
        params.addValue("title", pr.getTitle());
        params.addValue("branch", pr.getBranch());
        params.addValue("base_branch", pr.getBaseBranch());
        params.addValue("target_branch", pr.getTargetBranch());
        params.addValue("quality_gate_status", pr.getQualityGateStatus());
        params.addValue("bugs", pr.getBugs());
        params.addValue("vulnerabilities", pr.getVulnerabilities());
        params.addValue("code_smells", pr.getCodeSmells());
        params.addValue("analysis_date", DateUtils.toTimestamp(pr.getAnalysisDate()));
        params.addValue("url", pr.getUrl());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(insertPR, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return this.getPRId(company, pr.getKey(), pr.getProjectId())
                    .orElseThrow(() -> new SQLException("Failed to get PR id"));
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    public void batchInsertAnalyses(String company, String projectId, @Nullable List<Analyse> analyses) {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(projectId, "projectId cannot be null or empty.");

        String insertAnalysis = "INSERT INTO " + company + "." + SONARQUBE_PROJECTS_ANALYSES + " AS t " +
                " (project_id, key, time, project_version, build_version, revision, quality_gate, quality_profile) " +
                " VALUES (:project_id, :key, :time, :project_version, :build_version, :revision, :quality_gate, :quality_profile)" +
                " ON CONFLICT (key, project_id) DO UPDATE SET " +
                "   project_version = EXCLUDED.project_version," +
                "   time = EXCLUDED.time," +
                "   build_version = EXCLUDED.build_version," +
                "   revision = EXCLUDED.revision, " +
                "   quality_gate = EXCLUDED.quality_gate, " +
                "   quality_profile = EXCLUDED.quality_profile " +
                " WHERE (t.time, t.project_version, t.build_version, t.revision, t.quality_gate, t.quality_profile)" +
                " IS DISTINCT FROM (EXCLUDED.time, EXCLUDED.project_version, EXCLUDED.build_version, EXCLUDED.revision, EXCLUDED.quality_gate, EXCLUDED.quality_profile)";

        List<MapSqlParameterSource> batchParams = new ArrayList<>();
        for (Analyse analyse : ListUtils.emptyIfNull(analyses)) {
            if (analyse.getKey() == null) {
                continue;
            }
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("project_id", UUID.fromString(projectId));
            params.addValue("key", analyse.getKey());
            params.addValue("project_version", analyse.getProjectVersion());
            params.addValue("time", DateUtils.toTimestamp(analyse.getDate()));
            params.addValue("build_version", analyse.getBuildString());
            params.addValue("revision", analyse.getRevision());
            params.addValue("quality_gate", getQualityGate(analyse.getEvents()));
            params.addValue("quality_profile", getQualityProfile(analyse.getEvents()));
            batchParams.add(params);

            if (batchParams.size() >= ANALYSES_BATCH_SIZE) {
                template.batchUpdate(insertAnalysis, batchParams.toArray(new MapSqlParameterSource[0]));
                batchParams.clear();
            }
        }

        if (!batchParams.isEmpty()) {
            template.batchUpdate(insertAnalysis, batchParams.toArray(new MapSqlParameterSource[0]));
        }
    }

    public void batchInsertMeasures(String company, @Nullable List<DbSonarQubeMeasure> measures) {
        Validate.notBlank(company, "company cannot be null or empty.");

        String insertMetric = "INSERT INTO " + company + "." + SONARQUBE_METRICS + " AS t " +
                " (name, value, dtype, parent_id, ingested_at) " +
                " VALUES (:name, :value, :dtype, :parent_id::uuid, :ingested_at)  " +
                " ON CONFLICT (name, parent_id, ingested_at) DO UPDATE SET " +
                "   value = EXCLUDED.value," +
                "   dtype = EXCLUDED.dtype " +
                " WHERE (t.value, t.dtype)" +
                " IS DISTINCT FROM (EXCLUDED.value, EXCLUDED.dtype)";

        ArrayList<MapSqlParameterSource> batchParams = new ArrayList<>();
        for (DbSonarQubeMeasure measure : ListUtils.emptyIfNull(measures)) {
            if (ObjectUtils.anyNull(measure.getName(), measure.getParentId(), measure.getIngestedAt())) {
                continue;
            }
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("name", measure.getName());
            params.addValue("value", measure.getValue());
            params.addValue("dtype", measure.getDataType());
            params.addValue("parent_id", measure.getParentId());
            params.addValue("ingested_at", DateUtils.toTimestamp(measure.getIngestedAt()));
            batchParams.add(params);

            if (batchParams.size() >= MEASURES_BATCH_SIZE) {
                template.batchUpdate(insertMetric, batchParams.toArray(new MapSqlParameterSource[0]));
                batchParams.clear();
            }
        }

        if (!batchParams.isEmpty()) {
            template.batchUpdate(insertMetric, batchParams.toArray(new MapSqlParameterSource[0]));
        }
    }

    public DbListResponse<DbSonarQubeMeasure> listMetrics(String company,
                                                          SonarQubeMetricFilter filter,
                                                          SortingOrder sortOrder,
                                                          Integer pageNumber,
                                                          Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        final Long latestIngestedDate = filter.getIngestedAt();
        final Map<String, List<String>> conditions = createWhereClauseAndUpdateParams(params, filter.getScope(),
                filter.getIntegrationIds(), filter.getOrganizations(), filter.getProjects(), filter.getVisibilities(),
                filter.getMetrics(), filter.getDtypes(), filter.getPullRequests(), filter.getPrBranches(),
                filter.getPrTargetBranches(), filter.getPrBaseBranches(), filter.getBranches(), filter.getComplexityScore(), latestIngestedDate);
        params.put("offset", pageNumber * pageSize);
        params.put("limit", pageSize);
        String projectsWhere = "";
        if (conditions.get(SONARQUBE_PROJECTS).size() > 0) {
            projectsWhere = " WHERE " + String.join(" AND ", conditions.get(SONARQUBE_PROJECTS));
        }
        String projectsTable = "WITH sonar_projects AS (SELECT * FROM " + company + "." + SONARQUBE_PROJECTS +
                projectsWhere + ")";
        String parentJoin;
        switch (filter.getScope()) {
            case pull_request:
                String prWhereClause = "";
                if (conditions.get(SONARQUBE_PROJECT_PULL_REQUESTS).size() > 0) {
                    prWhereClause = " WHERE " + String.join(" AND ", conditions.get(SONARQUBE_PROJECT_PULL_REQUESTS));
                }
                parentJoin = " INNER JOIN (SELECT prs.*,sonar_projects.key repo FROM (SELECT id, title AS parent,project_id FROM " + company + "."
                        + SONARQUBE_PROJECT_PULL_REQUESTS + prWhereClause + ") prs INNER JOIN sonar_projects "
                        + "ON prs.project_id=sonar_projects.id) parents ON metrics.parent_id=parents.id";
                break;
            case branch:
                String branchWhereClause = "";
                if (conditions.get(SONARQUBE_PROJECT_BRANCHES).size() > 0) {
                    branchWhereClause = " WHERE " + String.join(" AND ", conditions.get(SONARQUBE_PROJECT_BRANCHES));
                }
                parentJoin = " INNER JOIN (SELECT branches.*,sonar_projects.key repo FROM (SELECT id, name AS parent,project_id FROM " + company + "."
                        + SONARQUBE_PROJECT_BRANCHES + branchWhereClause + ") branches INNER JOIN sonar_projects"
                        + " ON branches.project_id=sonar_projects.id) parents ON metrics.parent_id=parents.id";
                break;
            case repo:
            default:
                parentJoin = " INNER JOIN (SELECT id, key AS parent, key AS repo FROM sonar_projects) parents"
                        + " ON metrics.parent_id=parents.id";
                break;
        }
        String metricsWhere = "";
        if (conditions.get(SONARQUBE_METRICS).size() > 0) {
            metricsWhere = " WHERE " + String.join(" AND ", conditions.get(SONARQUBE_METRICS));
        }
        String query = projectsTable + "SELECT * FROM (SELECT * FROM " + company + "." + SONARQUBE_METRICS
                + metricsWhere + ") metrics " + parentJoin + " ORDER BY value " + sortOrder.name()
                + " OFFSET :offset LIMIT :limit";
        final List<DbSonarQubeMeasure> measures = template.query(query, params,
                SonarQubeProjectConverters.listMeasureMapper());
        String countSql = projectsTable + "SELECT COUNT(*) FROM (SELECT * FROM " + company + "." + SONARQUBE_METRICS
                + metricsWhere + ") metrics " + parentJoin;
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(measures, count);
    }


    public List<ImmutablePair<String, String>> getMetricsWithDataTypes(String company,
                                                                       SonarQubeMetricFilter filter) {
        Map<String, Object> params = new HashMap<>();
        final Long latestIngestedDate = filter.getIngestedAt();
        final Map<String, List<String>> conditions = createWhereClauseAndUpdateParams(params, filter.getScope(),
                filter.getIntegrationIds(), filter.getOrganizations(), filter.getProjects(), filter.getVisibilities(),
                filter.getMetrics(), filter.getDtypes(), filter.getPullRequests(), filter.getPrBranches(),
                filter.getPrTargetBranches(), filter.getPrBaseBranches(), filter.getBranches(), filter.getComplexityScore(), latestIngestedDate);
        String projectsWhere = "";
        if (conditions.get(SONARQUBE_PROJECTS).size() > 0) {
            projectsWhere = " WHERE " + String.join(" AND ", conditions.get(SONARQUBE_PROJECTS));
        }
        String projectsTable = "WITH sonar_projects AS (SELECT * FROM " + company + "." + SONARQUBE_PROJECTS +
                projectsWhere + ")";
        String parentJoin;
        switch (filter.getScope()) {
            case pull_request:
                String prWhereClause = "";
                if (conditions.get(SONARQUBE_PROJECT_PULL_REQUESTS).size() > 0) {
                    prWhereClause = " WHERE " + String.join(" AND ", conditions.get(SONARQUBE_PROJECT_PULL_REQUESTS));
                }
                parentJoin = " INNER JOIN (SELECT prs.* FROM (SELECT id, title AS parent,project_id FROM " + company + "."
                        + SONARQUBE_PROJECT_PULL_REQUESTS + prWhereClause + ") prs INNER JOIN sonar_projects "
                        + "ON prs.project_id=sonar_projects.id) parents ON metrics.parent_id=parents.id";
                break;
            case branch:
                String branchWhereClause = "";
                if (conditions.get(SONARQUBE_PROJECT_BRANCHES).size() > 0) {
                    branchWhereClause = " WHERE " + String.join(" AND ", conditions.get(SONARQUBE_PROJECT_BRANCHES));
                }
                parentJoin = " INNER JOIN (SELECT branches.* FROM (SELECT id, name AS parent,project_id FROM " + company + "."
                        + SONARQUBE_PROJECT_BRANCHES + branchWhereClause + ") branches INNER JOIN sonar_projects"
                        + " ON branches.project_id=sonar_projects.id) parents ON metrics.parent_id=parents.id";
                break;
            case repo:
            default:
                parentJoin = " INNER JOIN (SELECT id, key AS parent FROM sonar_projects) parents"
                        + " ON metrics.parent_id=parents.id";
                break;
        }
        String metricsWhere = "";
        if (conditions.get(SONARQUBE_METRICS).size() > 0) {
            metricsWhere = " WHERE " + String.join(" AND ", conditions.get(SONARQUBE_METRICS));
        }
        String query = projectsTable + "SELECT DISTINCT metrics.name, metrics.dtype FROM (SELECT * FROM "
                + company + "." + SONARQUBE_METRICS + metricsWhere + ") metrics " + parentJoin;
        return template.query(query, params, (rs, rowNum) ->
                ImmutablePair.of(rs.getString("name"), rs.getString("dtype")));
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company,
                                                                   SonarQubeMetricFilter filter,
                                                                   boolean values) {
        if (!values) {
            if (CollectionUtils.isEmpty(filter.getMetrics())) {
                filter = filter.toBuilder().dtypes(NUMERIC_METRICS).build();
            } else {
                List<ImmutablePair<String, String>> nonNumMetrics = CollectionUtils.emptyIfNull(
                                getMetricsWithDataTypes(company, filter))
                        .stream()
                        .distinct()
                        .filter(pair -> !NUMERIC_METRICS.contains(pair.getValue()))
                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(nonNumMetrics)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot perform agg on non numeric metrics: " +
                            nonNumMetrics.stream().collect(Collectors.toMap(Pair::getKey, Pair::getValue)));
                }
            }
        }
        Map<String, Object> params = new HashMap<>();
        Long latestIngestedDate = filter.getIngestedAt();
        String aggSql;
        String orderBySql;
        if (values) {
            aggSql = "COUNT(metrics.id) as ct";
            orderBySql = " ct DESC ";
        } else {
            aggSql = "MIN(num_val) AS mn,MAX(num_val) AS mx,COUNT(metrics.id) AS ct, SUM(num_val) as sum," +
                    " PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY num_val)";
            orderBySql = " mx DESC ";
        }
        String groupByString;
        String key;
        String additionalKey = "metric";
        switch (filter.getDISTINCT()) {
            case organization:
                groupByString = "organization, metric";
                key = "organization";
                break;
            case project:
            case pull_request:
                groupByString = "key, metric";
                key = "key";
                break;
            case visibility:
                groupByString = "visibility, metric";
                key = "visibility";
                break;
            case pr_branch:
            case branch:
                groupByString = "branch, metric";
                key = "branch";
                break;
            case pr_target_branch:
                groupByString = "target_branch, metric";
                key = "target_branch";
                break;
            case pr_base_branch:
                groupByString = "base_branch, metric";
                key = "base_branch";
                break;
            case metric:
                key = "metric";
                if (values) {
                    groupByString = "metric,dtype";
                    additionalKey = "dtype";
                } else {
                    groupByString = "metric";
                    additionalKey = null;
                }
                break;
            case trend:
                groupByString = "trend, metric";
                key = "trend";
                orderBySql = " trend ASC ";
                latestIngestedDate = null;
                break;
            default:
                throw new IllegalStateException("Unsupported across: " + filter.getDISTINCT());
        }
        final Map<String, List<String>> conditions = createWhereClauseAndUpdateParams(params, filter.getScope(),
                filter.getIntegrationIds(), filter.getOrganizations(), filter.getProjects(), filter.getVisibilities(),
                filter.getMetrics(), filter.getDtypes(), filter.getPullRequests(), filter.getPrBranches(),
                filter.getPrTargetBranches(), filter.getPrBaseBranches(), filter.getBranches(), filter.getComplexityScore(), latestIngestedDate);
        String projectsWhere = "";
        if (conditions.get(SONARQUBE_PROJECTS).size() > 0) {
            projectsWhere = " WHERE " + String.join(" AND ", conditions.get(SONARQUBE_PROJECTS));
        }
        String projectsTable = "WITH sonar_projects AS (SELECT * FROM " + company + "." + SONARQUBE_PROJECTS +
                projectsWhere + ")";
        String parentJoin;
        switch (filter.getScope()) {
            case pull_request:
                String prWhereClause = "";
                if (conditions.get(SONARQUBE_PROJECT_PULL_REQUESTS).size() > 0) {
                    prWhereClause = " WHERE " + String.join(" AND ", conditions.get(SONARQUBE_PROJECT_PULL_REQUESTS));
                }
                parentJoin = " INNER JOIN (SELECT prs.* FROM (SELECT * FROM " + company + "."
                        + SONARQUBE_PROJECT_PULL_REQUESTS + prWhereClause + ") prs INNER JOIN sonar_projects "
                        + "ON prs.project_id=sonar_projects.id) parents ON metrics.parent_id=parents.id";
                break;
            case branch:
                String branchWhereClause = "";
                if (conditions.get(SONARQUBE_PROJECT_BRANCHES).size() > 0) {
                    branchWhereClause = " WHERE " + String.join(" AND ", conditions.get(SONARQUBE_PROJECT_BRANCHES));
                }
                parentJoin = " INNER JOIN (SELECT branches.*, branches.name branch FROM (SELECT * FROM " + company + "."
                        + SONARQUBE_PROJECT_BRANCHES + branchWhereClause + ") branches INNER JOIN sonar_projects"
                        + " ON branches.project_id=sonar_projects.id) parents ON metrics.parent_id=parents.id";
                break;
            case repo:
            default:
                parentJoin = " INNER JOIN (SELECT * FROM sonar_projects) parents ON metrics.parent_id=parents.id";
                break;
        }
        String metricsWhere = "";
        if (conditions.get(SONARQUBE_METRICS).size() > 0) {
            metricsWhere = " WHERE " + String.join(" AND ", conditions.get(SONARQUBE_METRICS));
        }
        String valueCol = values ? "" : ",value::NUMERIC AS num_val";
        String query = projectsTable + " SELECT " + key + (additionalKey != null ? ("," + additionalKey) : "") + ","
                + aggSql + " FROM (SELECT name AS metric" + valueCol
                + ",EXTRACT(EPOCH FROM ingested_at)::text AS trend,* FROM "
                + company + "." + SONARQUBE_METRICS + metricsWhere + ") metrics " + parentJoin + " GROUP BY " +
                groupByString + " ORDER BY " + orderBySql;
        final List<DbAggregationResult> results = template.query(query, params,
                SonarQubeProjectConverters.aggMeasureMapper(values, key, additionalKey));
        return DbListResponse.of(results, results.size());
    }

    public DbListResponse<DbAggregationResult> stackedGroupBy(
            String company,
            SonarQubeMetricFilter filter,
            List<SonarQubeMetricFilter.DISTINCT> stacks) throws SQLException {
        DbListResponse<DbAggregationResult> result = groupByAndCalculate(company, filter, false);
        if (stacks == null
                || stacks.size() == 0
                || !SUPPORTED_STACKS.contains(stacks.get(0))
                || !SUPPORTED_STACKS.contains(filter.getDISTINCT())) {
            return result;
        }
        SonarQubeMetricFilter.DISTINCT stack = stacks.get(0);
        List<DbAggregationResult> finalList = new ArrayList<>();
        for (DbAggregationResult row : result.getRecords()) {
            SonarQubeMetricFilter stackFilter = null;
            switch (filter.getDISTINCT()) {
                case organization:
                    stackFilter = filter.toBuilder().organizations(List.of(row.getKey()))
                            .metrics(Optional.ofNullable(row.getAdditionalKey()).map(List::of).orElse(List.of()))
                            .DISTINCT(stack).build();
                    break;
                case project:
                    stackFilter = filter.toBuilder().projects(List.of(row.getKey()))
                            .metrics(Optional.ofNullable(row.getAdditionalKey()).map(List::of).orElse(List.of()))
                            .DISTINCT(stack).build();
                    break;
                case visibility:
                    stackFilter = filter.toBuilder().visibilities(List.of(row.getKey()))
                            .metrics(Optional.ofNullable(row.getAdditionalKey()).map(List::of).orElse(List.of()))
                            .DISTINCT(stack).build();
                    break;
                case pull_request:
                    if (SonarQubeMetricFilter.SCOPE.pull_request.equals(filter.getScope())) {
                        stackFilter = filter.toBuilder().pullRequests(List.of(row.getKey()))
                                .metrics(Optional.ofNullable(row.getAdditionalKey()).map(List::of).orElse(List.of()))
                                .DISTINCT(stack).build();
                    }
                    break;
                case pr_branch:
                    if (SonarQubeMetricFilter.SCOPE.pull_request.equals(filter.getScope())) {
                        stackFilter = filter.toBuilder().prBranches(List.of(row.getKey()))
                                .metrics(Optional.ofNullable(row.getAdditionalKey()).map(List::of).orElse(List.of()))
                                .DISTINCT(stack).build();
                    }
                    break;
                case pr_target_branch:
                    if (SonarQubeMetricFilter.SCOPE.pull_request.equals(filter.getScope())) {
                        stackFilter = filter.toBuilder().prTargetBranches(List.of(row.getKey()))
                                .metrics(Optional.ofNullable(row.getAdditionalKey()).map(List::of).orElse(List.of()))
                                .DISTINCT(stack).build();
                    }
                    break;
                case pr_base_branch:
                    if (SonarQubeMetricFilter.SCOPE.pull_request.equals(filter.getScope())) {
                        stackFilter = filter.toBuilder().prBaseBranches(List.of(row.getKey()))
                                .metrics(Optional.ofNullable(row.getAdditionalKey()).map(List::of).orElse(List.of()))
                                .DISTINCT(stack).build();
                    }
                    break;
                default:
                    throw new SQLException("This stack is not available for sonarqube metrics: " + stack);
            }
            finalList.add(row.toBuilder()
                    .stacks(stackFilter != null ? groupByAndCalculate(company, stackFilter, false).getRecords()
                            : List.of())
                    .build());
        }
        return DbListResponse.of(finalList, finalList.size());
    }

    public int cleanUpOldData(String company, Long fromEpochSeconds, Long olderThanSeconds) {
        String deleteSql = "DELETE FROM " + company + "." + SONARQUBE_METRICS + " WHERE ingested_at < :date";
        Map<String, Timestamp> params = Map.of("date", DateUtils.fromEpochSecondToTimestamp(fromEpochSeconds - olderThanSeconds));
        log.debug("sql = " + deleteSql);
        log.debug("params = {}", params);
        return template.update(deleteSql, params);
    }

    protected Map<String, List<String>> createWhereClauseAndUpdateParams(Map<String, Object> params,
                                                                         SonarQubeMetricFilter.SCOPE scope,
                                                                         List<String> integrationIds,
                                                                         List<String> organizations,
                                                                         List<String> projects,
                                                                         List<String> visibilities,
                                                                         List<String> metrics,
                                                                         List<String> dtypes,
                                                                         List<String> pullRequests,
                                                                         List<String> prBranches,
                                                                         List<String> prTargetBranches,
                                                                         List<String> prBaseBranches,
                                                                         List<String> branches,
                                                                         Map<String, String> complexityScore,
                                                                         Long ingestedAt) {
        List<String> projectConditions = new ArrayList<>();
        List<String> branchConditions = new ArrayList<>();
        List<String> pRConditions = new ArrayList<>();
        List<String> metricConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            projectConditions.add("integration_id IN (:integration_ids)");
            params.put("integration_ids", integrationIds.stream()
                    .map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(visibilities)) {
            projectConditions.add("visibility IN (:visibilities)");
            params.put("visibilities", visibilities);
        }
        if (CollectionUtils.isNotEmpty(organizations)) {
            projectConditions.add("organization IN (:organizations)");
            params.put("organizations", organizations);
        }
        if (CollectionUtils.isNotEmpty(projects)) {
            projectConditions.add("key IN (:projects)");
            params.put("projects", projects);
        }
        if (SonarQubeMetricFilter.SCOPE.pull_request.equals(scope)) {
            if (CollectionUtils.isNotEmpty(pullRequests)) {
                pRConditions.add("key IN (:pull_requests)");
                params.put("pull_requests", pullRequests);
            }
            if (CollectionUtils.isNotEmpty(prBranches)) {
                pRConditions.add("branch IN (:pr_branches)");
                params.put("pr_branches", prBranches);
            }
            if (CollectionUtils.isNotEmpty(prTargetBranches)) {
                pRConditions.add("target_branch IN (:target_branches)");
                params.put("target_branches", prTargetBranches);
            }
            if (CollectionUtils.isNotEmpty(prBaseBranches)) {
                pRConditions.add("base_branch IN (:base_branches)");
                params.put("base_branches", prBaseBranches);
            }
        }
        if (SonarQubeMetricFilter.SCOPE.branch.equals(scope) && CollectionUtils.isNotEmpty(branches)) {
            branchConditions.add("name IN (:branch_names)");
            params.put("branch_names", branches);
        }
        if (CollectionUtils.isNotEmpty(metrics)) {
            metricConditions.add("name IN (:metrics)");
            params.put("metrics", metrics);
            if (metrics.contains("complexity") || metrics.contains("cognitive_complexity")) {
                if (MapUtils.isNotEmpty(complexityScore)) {
                    String gt = complexityScore.get("$gt");
                    if (gt != null) {
                        metricConditions.add("value::NUMERIC > :complexity_score_gt");
                        params.put("complexity_score_gt", NumberUtils.toInt(gt));
                    }
                    String lt = complexityScore.get("$lt");
                    if (lt != null) {
                        metricConditions.add("value::NUMERIC < :complexity_score_lt");
                        params.put("complexity_score_lt", NumberUtils.toInt(lt));
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(dtypes)) {
            metricConditions.add("dtype IN (:dtypes)");
            params.put("dtypes", dtypes);
        }
        if (ingestedAt != null) {
            metricConditions.add("ingested_at = to_timestamp(:metric_ingested_at)");
            params.put("metric_ingested_at", ingestedAt);
        }
        return Map.of(
                SONARQUBE_PROJECTS, projectConditions,
                SONARQUBE_PROJECT_BRANCHES, branchConditions,
                SONARQUBE_PROJECT_PULL_REQUESTS, pRConditions,
                SONARQUBE_METRICS, metricConditions);
    }

    public DbListResponse<DbSonarQubeCoverage> listCoverage(String company,
                                                            SonarQubeMetricFilter filter,
                                                            Integer pageNumber,
                                                            Integer pageSize) {

        Map<String, Object> params = new HashMap<>();
        final Long latestIngestedDate = filter.getIngestedAt();
        final Map<String, List<String>> conditions = createWhereClauseAndUpdateParams(params, filter.getScope(),
                filter.getIntegrationIds(), filter.getOrganizations(), filter.getProjects(), filter.getVisibilities(),
                filter.getMetrics(), filter.getDtypes(), filter.getPullRequests(), filter.getPrBranches(),
                filter.getPrTargetBranches(), filter.getPrBaseBranches(), filter.getBranches(), filter.getComplexityScore(), latestIngestedDate);

        params.put("offset", pageNumber * pageSize);
        params.put("limit", pageSize);

        List<String> coverageParamList = List.of("coverage", "line_coverage", "lines", "uncovered_lines", "conditions_to_cover", "uncovered_conditions",
                "new_coverage", "new_line_coverage", "new_lines", "new_uncovered_lines", "new_conditions_to_cover", "new_uncovered_conditions");

        String coverageNames = coverageParamList.stream()
                .map(val -> "'" + val + "'")
                .collect(Collectors.joining(","));
        String coverageValues = coverageParamList.stream()
                .map(val -> "('" + val + "')")
                .collect(Collectors.joining(","));
        String coverageDataTypes = coverageParamList.stream()
                .map(val -> val + " character varying")
                .collect(Collectors.joining(","));

        String projectsWhere = "";

        if (conditions.get(SONARQUBE_PROJECTS).size() > 0) {
            projectsWhere = " WHERE " + String.join(" AND ", conditions.get(SONARQUBE_PROJECTS));
        }

        String projectsTable = "WITH sonar_projects AS (SELECT * FROM " + company + "." + SONARQUBE_PROJECTS +
                projectsWhere + ")";

        String parentJoin = "  INNER JOIN (SELECT id, key AS parent, key AS repo FROM sonar_projects) parents"
                + " ON metrics.parent_id=parents.id";

        String fromCrossTab = " ( $$"
                + " SELECT parent_id, name, value FROM "
                + " ( SELECT CASE  "
                + " WHEN metrics.name IN (" + coverageNames + ") THEN metrics.name::text  \n"
                + " END AS name, value, parent_id "
                + " FROM  " + company + "." + SONARQUBE_METRICS + " metrics "
                + " INNER JOIN " + company + "." + SONARQUBE_PROJECTS + " parents ON metrics.parent_id=parents.id "
                + " ) \"t\" "
                + " $$,"
                + " $$ "
                + "SELECT name FROM (values " + coverageValues + " ) t(name) "
                + " $$ ) metrics "
                + " (parent_id uuid, " + coverageDataTypes + " )";

        String selectDerivedParams = " CASE WHEN ( lines IS NULL OR uncovered_lines IS NULL ) "
                + " THEN NULL "
                + " ELSE ( CAST  (lines AS integer) - CAST  (uncovered_lines AS integer)) END AS covered_lines,"
                + " CASE WHEN ( conditions_to_cover IS NULL OR uncovered_conditions IS NULL ) "
                + " THEN NULL "
                + " ELSE ( CAST  (conditions_to_cover AS integer) - CAST  (uncovered_conditions AS integer)) END AS covered_conditions, "
                + " CASE WHEN ( new_lines IS NULL OR new_uncovered_lines IS NULL ) "
                + " THEN NULL "
                + " ELSE ( CAST  (new_lines AS integer) - CAST  (new_uncovered_lines AS integer)) END AS new_covered_lines,"
                + " CASE WHEN ( new_conditions_to_cover IS NULL OR new_uncovered_conditions IS NULL ) "
                + " THEN NULL "
                + " ELSE ( CAST  (new_conditions_to_cover AS integer) - CAST  (new_uncovered_conditions AS integer)) END AS new_covered_conditions ";

        String query = projectsTable + " SELECT * , "
                + selectDerivedParams
                + " FROM crosstab "
                + fromCrossTab
                + parentJoin;

        List<DbSonarQubeCoverage> coverage = List.of();

        String sql = query + " OFFSET :offset LIMIT :limit";

        coverage = template.query(sql, params,
                SonarQubeProjectConverters.listCoverageMapper());
        log.info("sql : {}", query);
        log.info("params : {}", params);

        String countSql = "SELECT COUNT(*) FROM (" + query + ") as x ";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(coverage, count);
    }

    private String getQualityGate(List<Event> events) {
        return convertFirstEventToString(events, "QUALITY_GATE");
    }

    private String getQualityProfile(List<Event> events) {
        return convertFirstEventToString(events, "QUALITY_PROFILE");
    }

    private String convertFirstEventToString(List<Event> events, String category) {
        return events.stream()
                .filter(x -> x.getCategory().equalsIgnoreCase(category))
                .findFirst()
                .map(e -> StringUtils.defaultString(e.getName(), " ") +
                        "<SP>" +
                        StringUtils.defaultString(e.getDescription(), " "))
                .orElse("");
    }

    protected Optional<String> getProjectId(String company, String key, String integrationId) {
        String query = "SELECT id FROM " + company + "." + SONARQUBE_PROJECTS + " where key=:key AND integration_id=:integration_id;";
        Map<String, Object> params = Map.of(
                "key", key,
                "integration_id", NumberUtils.toInt(integrationId)
        );
        return Optional.ofNullable(this.template.query(query, params, SonarQubeProjectConverters.idMapper()));
    }

    protected Optional<String> getPRId(String company, String key, String projectId) {
        String query = "SELECT id FROM " + company + "." + SONARQUBE_PROJECT_PULL_REQUESTS +
                " WHERE project_id=:project_id::uuid AND key=:key";
        return Optional.ofNullable(template.query(query, Map.of("key", key, "project_id", projectId),
                SonarQubeProjectConverters.idMapper()));
    }

    protected Optional<String> getBranchId(String company, String branchName, String projectId) {
        String query = "SELECT id FROM " + company + "." + SONARQUBE_PROJECT_BRANCHES +
                " WHERE project_id=:project_id::uuid AND name=:name";
        return Optional.ofNullable(template.query(query, Map.of("name", branchName, "project_id", projectId),
                SonarQubeProjectConverters.idMapper()));
    }

    @Override
    public Boolean update(String company, DbSonarQubeProject t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DbSonarQubeProject> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException("Not Supported");
    }

    public Optional<DbSonarQubeProject> get(String company, String key, String integrationId) {
        String query = "SELECT * FROM " + company + "." + SONARQUBE_PROJECTS + " " +
                "WHERE key=:key and integration_id=:integration_id";
        Map<String, Object> params = Map.of(
                "key", key,
                "integration_id", NumberUtils.toInt(integrationId)
        );
        return Optional.ofNullable(template.query(query, params, SonarQubeProjectConverters.rowMapper()));
    }

    @Override
    public DbListResponse<DbSonarQubeProject> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        createSonarQubeProjectsTable(company);
        createSonarQubePRTable(company);
        createSonarQubeAnalysesTable(company);
        createSonarQubeTables(company);
        return true;
    }

    private void createSonarQubeTables(String company) {
        List<String> ddlStmts = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + "." + SONARQUBE_METRICS +
                        " (id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " name VARCHAR NOT NULL," +
                        " value VARCHAR NOT NULL," +
                        " dtype VARCHAR NOT NULL," +
                        " parent_id UUID NOT NULL," +
                        " ingested_at DATE NOT NULL," +
                        " UNIQUE(name, parent_id, ingested_at)" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + SONARQUBE_METRICS + "_compound_parent_idx ON " +
                        company + "." + SONARQUBE_METRICS + " (name, parent_id)",
                "CREATE TABLE IF NOT EXISTS " + company + "." + SONARQUBE_PROJECT_BRANCHES +
                        " (id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " project_id UUID NOT NULL REFERENCES " + company + "." + SONARQUBE_PROJECTS +
                        "(id) ON DELETE CASCADE," +
                        "name VARCHAR NOT NULL," +
                        "quality_gate_status VARCHAR," +
                        "bugs INTEGER," +
                        "vulnerabilities INTEGER," +
                        "code_smells INTEGER," +
                        "analysis_date TIMESTAMP WITH TIME ZONE," +
                        "UNIQUE(name, project_id)" +
                        ")");
        ddlStmts.forEach(template.getJdbcTemplate()::execute);
    }

    private void createSonarQubeAnalysesTable(String company) {
        String ddlStatement = "CREATE TABLE IF NOT EXISTS " + company + "." + SONARQUBE_PROJECTS_ANALYSES + " (" +
                " id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                " project_id UUID NOT NULL REFERENCES " + company + "." + SONARQUBE_PROJECTS + "(id) ON DELETE CASCADE, " +
                " key varchar," +
                " time TIMESTAMP WITH TIME ZONE," +
                " project_version varchar," +
                " build_version varchar," +
                " revision varchar," +
                " quality_gate varchar," +
                " quality_profile varchar, " +
                " UNIQUE (key,project_id)" +
                ");";
        template.getJdbcTemplate().execute(ddlStatement);
    }

    private void createSonarQubeProjectsTable(String company) {
        String ddlStatement = "CREATE TABLE IF NOT EXISTS " + company + "." + SONARQUBE_PROJECTS + " (" +
                " id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                " integration_id INTEGER NOT NULL REFERENCES " + company + ".integrations(id) ON DELETE CASCADE," +
                " organization VARCHAR," +
                " key VARCHAR NOT NULL," +
                " name VARCHAR," +
                " visibility VARCHAR," +
                " last_analysis_date TIMESTAMP WITH TIME ZONE," +
                " revision VARCHAR," +
                " UNIQUE (key,integration_id)" +
                ");";
        String createIndex = "CREATE INDEX IF NOT EXISTS projects_integration_id_key on " + company + "." + SONARQUBE_PROJECTS + "(key,integration_id);";
        template.getJdbcTemplate().execute(ddlStatement);
        template.getJdbcTemplate().execute(createIndex);
    }

    private void createSonarQubePRTable(String company) {
        String ddlStatement =
                "CREATE TABLE IF NOT EXISTS " + company + "." + SONARQUBE_PROJECT_PULL_REQUESTS + " (" +
                        " id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " project_id UUID NOT NULL REFERENCES " + company + "." + SONARQUBE_PROJECTS +
                        " (id) ON DELETE CASCADE," +
                        " key VARCHAR," +
                        " title VARCHAR," +
                        " branch VARCHAR," +
                        " base_branch VARCHAR," +
                        " target_branch VARCHAR," +
                        " quality_gate_status VARCHAR," +
                        " bugs INTEGER," +
                        " vulnerabilities INTEGER," +
                        " code_smells INTEGER," +
                        " analysis_date TIMESTAMP WITH TIME ZONE," +
                        " url VARCHAR," +
                        " UNIQUE (key,project_id)" +
                        ");";

        template.getJdbcTemplate().execute(ddlStatement);
    }

}
