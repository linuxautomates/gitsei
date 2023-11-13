package io.levelops.commons.databases.services;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.DbAWSDevToolsBuildConverters;
import io.levelops.commons.databases.converters.DbAWSDevToolsTestcaseConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsBuild;
import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsReport;
import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsTestcase;
import io.levelops.commons.databases.models.filters.AWSDevToolsBuildsFilter;
import io.levelops.commons.databases.models.filters.AWSDevToolsTestcasesFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
public class AWSDevToolsBuildDatabaseService extends DatabaseService<DbAWSDevToolsBuild> {

    private static final String AWS_DEV_TOOLS_BUILDS = "awsdevtools_builds";
    private static final String AWS_DEV_TOOLS_REPORTS = "awsdevtools_reports";
    private static final String AWS_DEV_TOOLS_TESTCASES = "awsdevtools_testcases";
    private static final String AWS_DEV_TOOLS_PROJECTS = "awsdevtools_projects";
    static final Set<String> BUILD_SORTABLE_COLUMNS = Set.of("build_started_at", "build_ended_at");

    private final NamedParameterJdbcTemplate template;

    @Autowired
    protected AWSDevToolsBuildDatabaseService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbAWSDevToolsBuild build) throws SQLException {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            final int integrationId = NumberUtils.toInt(build.getIntegrationId());
            String insertBuild = "INSERT INTO " + company + "." + AWS_DEV_TOOLS_BUILDS + " (build_id, integration_id, arn, " +
                    "build_number, build_started_at, build_ended_at, last_phase, last_phase_status, status, build_complete, " +
                    "project_name, project_arn, initiator, build_batch_arn, source_type, source_location, resolved_source_version, region, " +
                    "created_at, updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (build_id, integration_id) " +
                    "DO UPDATE SET arn=EXCLUDED.arn, build_started_at=EXCLUDED.build_started_at, build_ended_at=EXCLUDED.build_ended_at, " +
                    "last_phase=EXCLUDED.last_phase, last_phase_status=EXCLUDED.last_phase_status, status=EXCLUDED.status, " +
                    "build_complete=EXCLUDED.build_complete, project_name=EXCLUDED.project_name, initiator=EXCLUDED.initiator, " +
                    "build_batch_arn=EXCLUDED.build_batch_arn, source_type=EXCLUDED.source_type, source_location=EXCLUDED.source_location, " +
                    "resolved_source_version=EXCLUDED.resolved_source_version, region=EXCLUDED.region RETURNING id";
            String insertReport = "INSERT INTO " + company + "." + AWS_DEV_TOOLS_REPORTS + " (arn, integration_id, execution_id, build_id, " +
                    "report_type, report_group_arn, report_group_name, status, duration, report_created_at, report_expired_at, " +
                    "created_at, updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (arn, integration_id) DO UPDATE SET " +
                    "execution_id=EXCLUDED.execution_id, report_type=EXCLUDED.report_type, report_group_arn=EXCLUDED.report_group_arn, " +
                    "report_group_name=EXCLUDED.report_group_name, status=EXCLUDED.status, duration=EXCLUDED.duration, " +
                    "report_created_at=EXCLUDED.report_created_at, report_expired_at=EXCLUDED.report_expired_at RETURNING id";
            String deleteExistingTestcase = "DELETE FROM " + company + "." + AWS_DEV_TOOLS_TESTCASES + " WHERE report_id=?";
            String insertTestcase = "INSERT INTO " + company + "." + AWS_DEV_TOOLS_TESTCASES + " (report_id, name, report_arn, prefix, " +
                    "status, duration, expired, created_at, updated_at) VALUES(?,?,?,?,?,?,?,?,?) ON CONFLICT (report_id, name) " +
                    "DO UPDATE SET report_arn=EXCLUDED.report_arn, prefix=EXCLUDED.prefix, status=EXCLUDED.status, " +
                    "duration=EXCLUDED.duration, expired=EXCLUDED.expired RETURNING id";
            try (PreparedStatement buildStmt = conn.prepareStatement(insertBuild, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement reportStmt = conn.prepareStatement(insertReport, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement deleteTestcaseStmt = conn.prepareStatement(deleteExistingTestcase);
                 PreparedStatement testcaseStmt = conn.prepareStatement(insertTestcase, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                buildStmt.setObject(i++, build.getBuildId());
                buildStmt.setObject(i++, integrationId);
                buildStmt.setObject(i++, build.getArn());
                buildStmt.setObject(i++, build.getBuildNumber());
                buildStmt.setObject(i++, getTimestamp(build.getBuildStartedAt()));
                buildStmt.setObject(i++, getTimestamp(build.getBuildEndedAt()));
                buildStmt.setObject(i++, build.getLastPhase());
                buildStmt.setObject(i++, build.getLastPhaseStatus());
                buildStmt.setObject(i++, build.getStatus());
                buildStmt.setObject(i++, build.getBuildComplete());
                buildStmt.setObject(i++, build.getProjectName());
                buildStmt.setObject(i++, build.getProjectArn());
                buildStmt.setObject(i++, build.getInitiator());
                buildStmt.setObject(i++, build.getBuildBatchArn());
                buildStmt.setObject(i++, build.getSourceType());
                buildStmt.setObject(i++, build.getSourceLocation());
                buildStmt.setObject(i++, build.getResolvedSourceVersion());
                buildStmt.setObject(i++, build.getRegion());
                buildStmt.setObject(i++, getTimestamp(build.getCreatedAt()));
                buildStmt.setObject(i, getTimestamp(build.getUpdatedAt()));
                buildStmt.execute();
                String buildId = getBuildIdOrFetch(buildStmt);
                final UUID buildUuid = UUID.fromString(buildId);
                if (CollectionUtils.isNotEmpty(build.getReports())) {
                    for (DbAWSDevToolsReport report : build.getReports()) {
                        int j = 1;
                        reportStmt.setObject(j++, report.getArn());
                        reportStmt.setObject(j++, integrationId);
                        reportStmt.setObject(j++, report.getExecutionId());
                        reportStmt.setObject(j++, buildUuid);
                        reportStmt.setObject(j++, report.getReportType());
                        reportStmt.setObject(j++, report.getReportGroupArn());
                        reportStmt.setObject(j++, report.getReportGroupName());
                        reportStmt.setObject(j++, report.getStatus());
                        reportStmt.setObject(j++, report.getDuration());
                        reportStmt.setObject(j++, getTimestamp(report.getReportCreatedAt()));
                        reportStmt.setObject(j++, getTimestamp(report.getReportExpiredAt()));
                        reportStmt.setObject(j++, getTimestamp(report.getCreatedAt()));
                        reportStmt.setObject(j, getTimestamp(report.getUpdatedAt()));
                        reportStmt.executeUpdate();

                        final String reportId = getReportIdOrFetch(reportStmt);
                        final UUID reportUuid = UUID.fromString(reportId);
                        if (CollectionUtils.isNotEmpty(report.getTestcases())) {
                            deleteTestcaseStmt.setObject(1, reportUuid);
                            deleteTestcaseStmt.execute();
                            for (DbAWSDevToolsTestcase testcase : report.getTestcases()) {
                                int k = 1;
                                testcaseStmt.setObject(k++, reportUuid);
                                testcaseStmt.setObject(k++, testcase.getName());
                                testcaseStmt.setObject(k++, testcase.getReportArn());
                                testcaseStmt.setObject(k++, testcase.getPrefix());
                                testcaseStmt.setObject(k++, testcase.getStatus());
                                testcaseStmt.setObject(k++, testcase.getDuration());
                                testcaseStmt.setObject(k++, getTimestamp(testcase.getExpired()));
                                testcaseStmt.setObject(k++, getTimestamp(testcase.getCreatedAt()));
                                testcaseStmt.setObject(k, getTimestamp(testcase.getUpdatedAt()));
                                testcaseStmt.execute();
                            }
                        }
                    }
                }
                return buildId;
            }
        }));
    }

    private String getBuildIdOrFetch(PreparedStatement insertStmt) throws SQLException {
        String id;
        try (ResultSet rs = insertStmt.getGeneratedKeys()) {
            if (rs.next()) {
                id = rs.getString(1);
            } else {
                throw new SQLException("Failed to get build row id");
            }
        }
        return id;
    }

    private String getReportIdOrFetch(PreparedStatement insertStmt) throws SQLException {
        String name;
        try (ResultSet rs = insertStmt.getGeneratedKeys()) {
            if (rs.next()) {
                name = rs.getString(1);
            } else {
                throw new SQLException("Failed to get project row id");
            }
        }
        return name;
    }

    public DbListResponse<DbAWSDevToolsBuild> listBuilds(String company,
                                                         AWSDevToolsBuildsFilter filter,
                                                         Map<String, SortingOrder> sortBy,
                                                         Integer pageNumber,
                                                         Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        Map<String, List<String>> conditions = createBuildsWhereClauseAndUpdateParams(params, filter.getProjectNames(),
                filter.getLastPhases(), filter.getLastPhaseStatuses(), filter.getStatuses(),
                filter.getSourceTypes(), filter.getInitiators(), filter.getBuildBatchArns(), filter.getRegions(), filter.getIntegrationIds());
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (BUILD_SORTABLE_COLUMNS.contains(entry.getKey()))
                        return entry.getKey();
                    return "build_started_at";
                })
                .orElse("build_started_at");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        String buildWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(AWS_DEV_TOOLS_BUILDS))) {
            buildWhere = " WHERE " + String.join(" AND ", conditions.get(AWS_DEV_TOOLS_BUILDS));
        }
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        List<DbAWSDevToolsBuild> results = List.of();
        if (pageSize > 0) {
            String sql = "SELECT * FROM " + company + "." + AWS_DEV_TOOLS_BUILDS + buildWhere
                    + " ORDER BY " + sortByKey + " " + sortOrder.toString()
                    + " OFFSET :skip LIMIT :limit";
            results = template.query(sql, params, DbAWSDevToolsBuildConverters.listRowMapper());
        }
        String countSql = "SELECT count(*) FROM " + company + "." + AWS_DEV_TOOLS_BUILDS + buildWhere;
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateBuild(String company,
                                                                        AWSDevToolsBuildsFilter filter,
                                                                        String configTableKey) {
        AWSDevToolsBuildsFilter.DISTINCT across = filter.getAcross();
        Validate.notNull(across, "Across must be present for group by query");
        if (StringUtils.isNotEmpty(configTableKey)) {
            across = AWSDevToolsBuildsFilter.DISTINCT.none;
        }
        final AWSDevToolsBuildsFilter.CALCULATION calculation = MoreObjects.firstNonNull(
                filter.getCalculation(), AWSDevToolsBuildsFilter.CALCULATION.build_count);
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<String, Object> params = new HashMap<>();
        String calculationComponent;
        String orderByString;
        String sql;
        switch (calculation) {
            case duration:
                calculationComponent = " MIN(duration) as mn, MAX(duration) as mx, COUNT(id) as ct, PERCENTILE_DISC(0.5) " +
                        "WITHIN GROUP(ORDER BY duration)";
                orderByString = " mx DESC ";
                break;
            case build_count:
            default:
                calculationComponent = " COUNT(id) as ct ";
                orderByString = " ct DESC ";
                break;
        }
        String groupByString;
        String selectDistinctString;
        switch (across) {
            case project_name:
            case last_phase:
            case last_phase_status:
            case status:
            case source_type:
            case initiator:
            case region:
            case build_batch_arn:
                groupByString = " GROUP BY " + across.name();
                selectDistinctString = filter.getAcross().toString();
                break;
            case none:
                groupByString = "";
                selectDistinctString = "";
                break;
            default:
                throw new IllegalStateException("Unsupported across: " + across);
        }
        final Map<String, List<String>> conditions = createBuildsWhereClauseAndUpdateParams(params, filter.getProjectNames(),
                filter.getLastPhases(), filter.getLastPhaseStatuses(), filter.getStatuses(),
                filter.getSourceTypes(), filter.getInitiators(), filter.getBuildBatchArns(), filter.getRegions(), filter.getIntegrationIds());
        String buildsWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(AWS_DEV_TOOLS_BUILDS))) {
            buildsWhere = " WHERE " + String.join(" AND ", conditions.get(AWS_DEV_TOOLS_BUILDS));
        }
        String projectsWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(AWS_DEV_TOOLS_PROJECTS))) {
            projectsWhere = " WHERE " + String.join(" AND ", conditions.get(AWS_DEV_TOOLS_PROJECTS));
        }
        String innerJoin = "(SELECT * FROM " + company
                + "." + AWS_DEV_TOOLS_BUILDS + " " + buildsWhere + " ) AS builds INNER JOIN "
                + " (SELECT * FROM " + company + "."
                + AWS_DEV_TOOLS_PROJECTS + " " + projectsWhere + " ) AS projects on builds.project_arn=projects.arn) " +
                "AS final_table";
        List<DbAggregationResult> dbAggregationResults;
        if (StringUtils.isNotEmpty(configTableKey)) {
            sql = "SELECT " + "'" + configTableKey + "'" + " as config_key "
                    + ", " + calculationComponent
                    + " FROM ( SELECT builds.*,"
                    + "(extract(epoch FROM COALESCE(build_ended_at,TO_TIMESTAMP(" + currentTime + ")))"
                    + "-extract(epoch FROM build_started_at)) AS duration"
                    + " FROM " + innerJoin
                    + " ORDER BY " + orderByString;
            dbAggregationResults = template.query(sql, params,
                    DbAWSDevToolsBuildConverters.aggRowMapper("config_key", calculation));
        } else {
            sql = "SELECT " + selectDistinctString
                    + ", " + calculationComponent
                    + " FROM ( SELECT builds.*,"
                    + "(extract(epoch FROM COALESCE(build_ended_at,TO_TIMESTAMP(" + currentTime + ")))"
                    + "-extract(epoch FROM build_started_at)) AS duration"
                    + " FROM " + innerJoin
                    + groupByString
                    + " ORDER BY " + orderByString;
            dbAggregationResults = template.query(sql, params,
                    DbAWSDevToolsBuildConverters.aggRowMapper(across.toString(), calculation));

        }
        return DbListResponse.of(dbAggregationResults, dbAggregationResults.size());
    }

    protected Map<String, List<String>> createBuildsWhereClauseAndUpdateParams(Map<String, Object> params,
                                                                               List<String> projectNames,
                                                                               List<String> lastPhases,
                                                                               List<String> lastPhaseStatuses,
                                                                               List<String> statuses,
                                                                               List<String> sourceTypes,
                                                                               List<String> initiators,
                                                                               List<String> buildBatches,
                                                                               List<String> regions,
                                                                               List<String> integrationIds) {
        List<String> buildConditions = new ArrayList<>();
        List<String> projectConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(projectNames)) {
            projectConditions.add("name IN (:projectnames)");
            buildConditions.add("project_name IN (:projectnames)");
            params.put("projectnames", projectNames);
        }
        if (CollectionUtils.isNotEmpty(lastPhases)) {
            buildConditions.add("last_phase IN (:lastphases)");
            params.put("lastphases", lastPhases);
        }
        if (CollectionUtils.isNotEmpty(lastPhaseStatuses)) {
            buildConditions.add("last_phase_status IN (:lastphasestatuses)");
            params.put("lastphasestatuses", lastPhaseStatuses);
        }
        if (CollectionUtils.isNotEmpty(statuses)) {
            buildConditions.add("status IN (:statuses)");
            params.put("statuses", statuses);
        }
        if (CollectionUtils.isNotEmpty(sourceTypes)) {
            buildConditions.add("source_type IN (:sourcetypes)");
            params.put("sourcetypes", sourceTypes);
        }
        if (CollectionUtils.isNotEmpty(sourceTypes)) {
            buildConditions.add("initiator IN (:initiators)");
            params.put("initiators", initiators);
        }
        if (CollectionUtils.isNotEmpty(buildBatches)) {
            buildConditions.add("build_batch_arn IN (:buildbatches)");
            params.put("buildbatches", buildBatches);
        }
        if (CollectionUtils.isNotEmpty(regions)) {
            buildConditions.add("region IN (:regions)");
            params.put("regions", regions);
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            buildConditions.add("integration_id IN (:integration_ids)");
            params.put("integration_ids", integrationIds.stream()
                    .map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
        }
        return Map.of(AWS_DEV_TOOLS_BUILDS, buildConditions, AWS_DEV_TOOLS_PROJECTS, projectConditions);
    }

    public DbListResponse<DbAggregationResult> buildStackedGroupBy(String company,
                                                                   AWSDevToolsBuildsFilter filter,
                                                                   List<AWSDevToolsBuildsFilter.DISTINCT> stacks,
                                                                   String configTableKey)
            throws SQLException {
        Set<AWSDevToolsBuildsFilter.DISTINCT> stackSupported = Set.of(
                AWSDevToolsBuildsFilter.DISTINCT.project_name,
                AWSDevToolsBuildsFilter.DISTINCT.last_phase,
                AWSDevToolsBuildsFilter.DISTINCT.last_phase_status,
                AWSDevToolsBuildsFilter.DISTINCT.status,
                AWSDevToolsBuildsFilter.DISTINCT.source_type,
                AWSDevToolsBuildsFilter.DISTINCT.initiator,
                AWSDevToolsBuildsFilter.DISTINCT.region,
                AWSDevToolsBuildsFilter.DISTINCT.build_batch_arn
        );
        DbListResponse<DbAggregationResult> result = groupByAndCalculateBuild(company, filter, null);
        if (stacks == null
                || stacks.size() == 0
                || !stackSupported.contains(stacks.get(0))
                || !stackSupported.contains(filter.getAcross()))
            return result;
        AWSDevToolsBuildsFilter.DISTINCT stack = stacks.get(0);
        List<DbAggregationResult> dbAggregationResults = new ArrayList<>();
        for (DbAggregationResult record : result.getRecords()) {
            AWSDevToolsBuildsFilter newFilter;
            if (StringUtils.isNotEmpty(configTableKey)) {
                newFilter = filter.toBuilder().across(stack).build();
            } else {
                switch (filter.getAcross()) {
                    case project_name:
                        newFilter = filter.toBuilder().projectNames(List.of(record.getKey())).across(stack).build();
                        break;
                    case last_phase:
                        newFilter = filter.toBuilder().lastPhases(List.of(record.getKey())).across(stack).build();
                        break;
                    case last_phase_status:
                        newFilter = filter.toBuilder().lastPhaseStatuses(List.of(record.getKey())).across(stack).build();
                        break;
                    case status:
                        newFilter = filter.toBuilder().statuses(List.of(record.getKey())).across(stack).build();
                        break;
                    case source_type:
                        newFilter = filter.toBuilder().sourceTypes(List.of(record.getKey())).across(stack).build();
                        break;
                    case initiator:
                        newFilter = filter.toBuilder().initiators(List.of(record.getKey())).across(stack).build();
                        break;
                    case build_batch_arn:
                        newFilter = filter.toBuilder().buildBatchArns(List.of(record.getKey())).across(stack).build();
                        break;
                    case region:
                        newFilter = filter.toBuilder().regions(List.of(record.getKey())).across(stack).build();
                        break;
                    default:
                        throw new SQLException("This stack is not available for awsdevtools builds." + stack);
                }
            }
            dbAggregationResults.add(record.toBuilder().stacks(groupByAndCalculateBuild(company, newFilter, null).getRecords()).build());
        }
        return DbListResponse.of(dbAggregationResults, dbAggregationResults.size());
    }

    public DbListResponse<DbAWSDevToolsTestcase> listTestcases(String company,
                                                               AWSDevToolsTestcasesFilter filter,
                                                               Integer pageNumber,
                                                               Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        Map<String, List<String>> conditions = createTestcasesWhereClauseAndUpdateParams(params, filter.getStatuses(),
                filter.getReportArns(), filter.getRegions(), filter.getInitiators(), filter.getSourceTypes(),
                filter.getProjectNames(), filter.getBuildBatchArns());
        String testcaseWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(AWS_DEV_TOOLS_TESTCASES))) {
            testcaseWhere = " WHERE " + String.join(" AND ", conditions.get(AWS_DEV_TOOLS_TESTCASES));
        }
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        List<DbAWSDevToolsTestcase> results = List.of();
        if (pageSize > 0) {
            String sql = "SELECT * FROM " + company + "." + AWS_DEV_TOOLS_TESTCASES + testcaseWhere
                    + " OFFSET :skip LIMIT :limit";
            results = template.query(sql, params, DbAWSDevToolsTestcaseConverters.listRowMapper());
        }
        String countSql = "SELECT count(*) FROM " + company + "." + AWS_DEV_TOOLS_TESTCASES + testcaseWhere;
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateTestcase(String company,
                                                                           AWSDevToolsTestcasesFilter filter,
                                                                           String configTableKey) {
        AWSDevToolsTestcasesFilter.DISTINCT across = filter.getAcross();
        Validate.notNull(across, "Across cant be missing for groupBy query.");
        if (StringUtils.isNotEmpty(configTableKey)) {
            across = AWSDevToolsTestcasesFilter.DISTINCT.none;
        }
        final AWSDevToolsTestcasesFilter.CALCULATION calculation = MoreObjects.firstNonNull(
                filter.getCalculation(), AWSDevToolsTestcasesFilter.CALCULATION.testcase_count);
        Map<String, Object> params = new HashMap<>();
        String calculationComponent;
        String orderByString;
        String sql;
        switch (calculation) {
            case duration:
                calculationComponent = " MIN(duration) as mn, MAX(duration) as mx, COUNT(id) as ct, " +
                        "PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY duration)";
                orderByString = " mx DESC ";
                break;
            case testcase_count:
            default:
                calculationComponent = " COUNT(id) as ct ";
                orderByString = " ct DESC ";
                break;
        }
        String groupByString;
        String selectDistinctString;
        switch (across) {
            case report_arn:
            case status:
            case region:
            case project_name:
            case source_type:
            case initiator:
            case build_batch_arn:
                groupByString = " GROUP BY " + across.name();
                selectDistinctString = filter.getAcross().toString();
                break;
            case none:
                groupByString = "";
                selectDistinctString = "";
                break;
            default:
                throw new IllegalStateException("Unsupported across: " + across);
        }
        Map<String, List<String>> conditions = createTestcasesWhereClauseAndUpdateParams(params, filter.getStatuses(),
                filter.getReportArns(), filter.getRegions(), filter.getInitiators(), filter.getSourceTypes(), filter.getProjectNames(),
                filter.getBuildBatchArns());
        String testcasesWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(AWS_DEV_TOOLS_TESTCASES))) {
            testcasesWhere = " WHERE " + String.join(" AND ", conditions.get(AWS_DEV_TOOLS_TESTCASES));
        }
        String buildsWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(AWS_DEV_TOOLS_BUILDS))) {
            buildsWhere = " WHERE " + String.join(" AND ", conditions.get(AWS_DEV_TOOLS_BUILDS));
        }
        String innerJoin = "(SELECT * FROM " + company + "." + AWS_DEV_TOOLS_BUILDS + " " + buildsWhere + ") AS builds"
                + " INNER JOIN (SELECT * FROM " + company + "." + AWS_DEV_TOOLS_REPORTS + " ) AS reports"
                + " ON reports.execution_id = builds.arn INNER JOIN"
                + " (SELECT * FROM " + company + "." + AWS_DEV_TOOLS_TESTCASES + " " + testcasesWhere + ") AS testcases"
                + " ON testcases.report_arn = reports.arn";
        List<DbAggregationResult> dbAggregationResults;
        if (StringUtils.isNotEmpty(configTableKey)) {
            sql = "SELECT " + "'" + configTableKey + "'" + " as config_key "
                    + ", " + calculationComponent
                    + " FROM ( SELECT testcases.*, builds.project_name, builds.source_type,"
                    + " builds.initiator, builds.build_batch_arn, builds.region "
                    + " FROM " + innerJoin + ") AS final_table "
                    + " ORDER BY " + orderByString;
            dbAggregationResults = template.query(sql, params,
                    DbAWSDevToolsTestcaseConverters.aggRowMapper("config_key", calculation));
        } else {
            sql = "SELECT " + selectDistinctString
                    + ", " + calculationComponent
                    + " FROM ( SELECT testcases.*, builds.project_name, builds.source_type,"
                    + " builds.initiator, builds.build_batch_arn, builds.region "
                    + " FROM " + innerJoin + ") AS final_table "
                    + groupByString
                    + " ORDER BY " + orderByString;
            dbAggregationResults = template.query(sql, params,
                    DbAWSDevToolsTestcaseConverters.aggRowMapper(across.toString(), calculation));
        }
        return DbListResponse.of(dbAggregationResults, dbAggregationResults.size());
    }

    protected Map<String, List<String>> createTestcasesWhereClauseAndUpdateParams(Map<String, Object> params,
                                                                                  List<String> statuses,
                                                                                  List<String> reportArns,
                                                                                  List<String> regions,
                                                                                  List<String> initiators,
                                                                                  List<String> sourceTypes,
                                                                                  List<String> projectNames,
                                                                                  List<String> buildBatches) {
        List<String> testcasesConditions = new ArrayList<>();
        List<String> buildConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(statuses)) {
            testcasesConditions.add("status IN (:statuses)");
            params.put("statuses", statuses);
        }
        if (CollectionUtils.isNotEmpty(reportArns)) {
            testcasesConditions.add("report_arn IN (:reportarns)");
            params.put("reportarns", reportArns);
        }
        if (CollectionUtils.isNotEmpty(regions)) {
            buildConditions.add("region IN (:regions)");
            params.put("regions", regions);
        }
        if (CollectionUtils.isNotEmpty(initiators)) {
            buildConditions.add("initiator IN (:initiators)");
            params.put("initiators", initiators);
        }
        if (CollectionUtils.isNotEmpty(initiators)) {
            buildConditions.add("source_type IN (:source_types)");
            params.put("source_types", sourceTypes);
        }
        if (CollectionUtils.isNotEmpty(projectNames)) {
            buildConditions.add("project_name IN (:project_names)");
            params.put("project_names", projectNames);
        }
        if (CollectionUtils.isNotEmpty(buildBatches)) {
            buildConditions.add("build_batch_arn IN (:build_batches)");
            params.put("build_batches", buildBatches);
        }
        return Map.of(AWS_DEV_TOOLS_TESTCASES, testcasesConditions, AWS_DEV_TOOLS_BUILDS, buildConditions);
    }

    public DbListResponse<DbAggregationResult> testcaseStackedGroupBy(String company,
                                                                      AWSDevToolsTestcasesFilter filter,
                                                                      List<AWSDevToolsTestcasesFilter.DISTINCT> stacks,
                                                                      String configTableKey)
            throws SQLException {
        Set<AWSDevToolsTestcasesFilter.DISTINCT> stackSupported = Set.of(
                AWSDevToolsTestcasesFilter.DISTINCT.status,
                AWSDevToolsTestcasesFilter.DISTINCT.report_arn,
                AWSDevToolsTestcasesFilter.DISTINCT.region,
                AWSDevToolsTestcasesFilter.DISTINCT.project_name,
                AWSDevToolsTestcasesFilter.DISTINCT.source_type,
                AWSDevToolsTestcasesFilter.DISTINCT.initiator,
                AWSDevToolsTestcasesFilter.DISTINCT.build_batch_arn
        );
        DbListResponse<DbAggregationResult> result = groupByAndCalculateTestcase(company, filter, configTableKey);
        if (stacks == null
                || stacks.size() == 0
                || !stackSupported.contains(stacks.get(0))
                || !stackSupported.contains(filter.getAcross()))
            return result;
        AWSDevToolsTestcasesFilter.DISTINCT stack = stacks.get(0);
        List<DbAggregationResult> dbAggregationResults = new ArrayList<>();
        for (DbAggregationResult record : result.getRecords()) {
            AWSDevToolsTestcasesFilter newFilter;
            if (StringUtils.isNotEmpty(configTableKey)) {
                newFilter = filter.toBuilder().across(stack).build();
            } else {
                switch (filter.getAcross()) {
                    case status:
                        newFilter = filter.toBuilder().statuses(List.of(record.getKey())).across(stack).build();
                        break;
                    case report_arn:
                        newFilter = filter.toBuilder().reportArns(List.of(record.getKey())).across(stack).build();
                        break;
                    case region:
                        newFilter = filter.toBuilder().regions(List.of(record.getKey())).across(stack).build();
                        break;
                    case project_name:
                        newFilter = filter.toBuilder().projectNames(List.of(record.getKey())).across(stack).build();
                        break;
                    case source_type:
                        newFilter = filter.toBuilder().sourceTypes(List.of(record.getKey())).across(stack).build();
                        break;
                    case initiator:
                        newFilter = filter.toBuilder().initiators(List.of(record.getKey())).across(stack).build();
                        break;
                    case build_batch_arn:
                        newFilter = filter.toBuilder().buildBatchArns(List.of(record.getKey())).across(stack).build();
                        break;
                    default:
                        throw new SQLException("This stack is not available for awsdevtools testcases." + stack);
                }
            }
            dbAggregationResults.add(record.toBuilder().stacks(groupByAndCalculateTestcase(company, newFilter, null).getRecords()).build());
        }
        return DbListResponse.of(dbAggregationResults, dbAggregationResults.size());
    }

    @Override
    public Boolean update(String company, DbAWSDevToolsBuild build) throws SQLException {
        return null;
    }

    @Override
    public Optional<DbAWSDevToolsBuild> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    @Override
    public DbListResponse<DbAWSDevToolsBuild> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return null;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return null;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        final List<String> ddlStmts = List.of("CREATE TABLE IF NOT EXISTS " + company + "." + AWS_DEV_TOOLS_BUILDS +
                        " (" +
                        " id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " build_id VARCHAR NOT NULL," +
                        " integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE," +
                        " arn VARCHAR NOT NULL," +
                        " build_number BIGINT NOT NULL," +
                        " build_started_at TIMESTAMP WITH TIME ZONE," +
                        " build_ended_at TIMESTAMP WITH TIME ZONE," +
                        " last_phase VARCHAR," +
                        " last_phase_status VARCHAR," +
                        " status VARCHAR NOT NULL," +
                        " build_complete VARCHAR," +
                        " project_name VARCHAR NOT NULL," +
                        " project_arn VARCHAR NOT NULL," +
                        " initiator VARCHAR NOT NULL," +
                        " build_batch_arn VARCHAR," +
                        " source_type VARCHAR NOT NULL," +
                        " source_location VARCHAR," +
                        " resolved_source_version VARCHAR," +
                        " region VARCHAR NOT NULL," +
                        " created_at DATE NOT NULL," +
                        " updated_at DATE NOT NULL," +
                        " UNIQUE(build_id, integration_id)" +
                        ")",
                "CREATE TABLE IF NOT EXISTS " + company + "." + AWS_DEV_TOOLS_REPORTS +
                        " (" +
                        " id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " arn VARCHAR NOT NULL," +
                        " integration_id INTEGER NOT NULL REFERENCES " +
                        company + ".integrations(id) ON DELETE CASCADE," +
                        " execution_id VARCHAR NOT NULL, " +
                        " build_id UUID NOT NULL REFERENCES " +
                        company + "." + AWS_DEV_TOOLS_BUILDS + "(id) ON DELETE CASCADE," +
                        " report_type VARCHAR NOT NULL," +
                        " report_group_arn VARCHAR," +
                        " report_group_name VARCHAR," +
                        " status VARCHAR," +
                        " duration BIGINT," +
                        " report_created_at TIMESTAMP WITH TIME ZONE," +
                        " report_expired_at TIMESTAMP WITH TIME ZONE," +
                        " created_at TIMESTAMP WITH TIME ZONE," +
                        " updated_at TIMESTAMP WITH TIME ZONE," +
                        " UNIQUE(arn, integration_id)" +
                        ")",
                "CREATE TABLE IF NOT EXISTS " + company + "." + AWS_DEV_TOOLS_TESTCASES +
                        " (" +
                        " id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " name VARCHAR NOT NULL," +
                        " report_id UUID NOT NULL REFERENCES " +
                        company + "." + AWS_DEV_TOOLS_REPORTS + "(id) ON DELETE CASCADE," +
                        " report_arn VARCHAR NOT NULL, " +
                        " prefix VARCHAR," +
                        " status VARCHAR  NOT NULL," +
                        " duration BIGINT," +
                        " expired TIMESTAMP WITH TIME ZONE," +
                        " created_at TIMESTAMP WITH TIME ZONE," +
                        " updated_at TIMESTAMP WITH TIME ZONE," +
                        " UNIQUE(name, report_id)" +
                        ")"
        );
        ddlStmts.forEach(ddlStatement -> template.getJdbcTemplate().execute(ddlStatement));
        return true;
    }

    private Timestamp getTimestamp(Date date) {
        return date != null ? Timestamp.from(date.toInstant()) : null;
    }
}
