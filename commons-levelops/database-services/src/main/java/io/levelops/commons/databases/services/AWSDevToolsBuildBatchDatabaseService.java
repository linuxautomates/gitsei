package io.levelops.commons.databases.services;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.DbAWSDevToolsBuildBatchConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsBuildBatch;
import io.levelops.commons.databases.models.filters.AWSDevToolsBuildBatchesFilter;
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
public class AWSDevToolsBuildBatchDatabaseService extends DatabaseService<DbAWSDevToolsBuildBatch> {

    private static final String AWS_DEV_TOOLS_BUILD_BATCHES = "awsdevtools_build_batches";
    private static final String AWS_DEV_TOOLS_PROJECTS = "awsdevtools_projects";
    static final Set<String> BUILD_BATCHES_SORTABLE_COLUMNS = Set.of("build_batch_started_at", "build_batch_ended_at");

    private final NamedParameterJdbcTemplate template;

    @Autowired
    protected AWSDevToolsBuildBatchDatabaseService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbAWSDevToolsBuildBatch buildBatch) throws SQLException {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            final int integrationId = NumberUtils.toInt(buildBatch.getIntegrationId());
            String insertBuildBatch = "INSERT INTO " + company + "." + AWS_DEV_TOOLS_BUILD_BATCHES + " (build_batch_id, " +
                    "integration_id, arn, build_batch_number, build_batch_started_at, build_batch_ended_at, build_batch_complete, " +
                    "last_phase, last_phase_status, status, project_name, project_arn, initiator, source_version, " +
                    "resolved_source_version, source_type, source_location, region, created_at, updated_at) " +
                    "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (build_batch_id, integration_id) DO UPDATE SET " +
                    "arn=EXCLUDED.arn, build_batch_started_at=EXCLUDED.build_batch_started_at, build_batch_ended_at=EXCLUDED.build_batch_ended_at, " +
                    "build_batch_complete=EXCLUDED.build_batch_complete, last_phase=EXCLUDED.last_phase, " +
                    "last_phase_status=EXCLUDED.last_phase_status, status=EXCLUDED.status, " +
                    "project_name=EXCLUDED.project_name, initiator=EXCLUDED.initiator, source_version=EXCLUDED.source_version, " +
                    "resolved_source_version=EXCLUDED.resolved_source_version, source_type=EXCLUDED.source_type, " +
                    "source_location=EXCLUDED.source_location, region=EXCLUDED.region RETURNING id";
            try (PreparedStatement buildBatchStmt = conn.prepareStatement(insertBuildBatch, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                buildBatchStmt.setObject(i++, buildBatch.getBuildBatchId());
                buildBatchStmt.setObject(i++, integrationId);
                buildBatchStmt.setObject(i++, buildBatch.getArn());
                buildBatchStmt.setObject(i++, buildBatch.getBuildBatchNumber());
                buildBatchStmt.setObject(i++, getTimestamp(buildBatch.getBuildBatchStartedAt()));
                buildBatchStmt.setObject(i++, getTimestamp(buildBatch.getBuildBatchEndedAt()));
                buildBatchStmt.setObject(i++, buildBatch.getBuildBatchComplete());
                buildBatchStmt.setObject(i++, buildBatch.getLastPhase());
                buildBatchStmt.setObject(i++, buildBatch.getLastPhaseStatus());
                buildBatchStmt.setObject(i++, buildBatch.getStatus());
                buildBatchStmt.setObject(i++, buildBatch.getProjectName());
                buildBatchStmt.setObject(i++, buildBatch.getProjectArn());
                buildBatchStmt.setObject(i++, buildBatch.getInitiator());
                buildBatchStmt.setObject(i++, buildBatch.getSourceVersion());
                buildBatchStmt.setObject(i++, buildBatch.getResolvedSourceVersion());
                buildBatchStmt.setObject(i++, buildBatch.getSourceType());
                buildBatchStmt.setObject(i++, buildBatch.getSourceLocation());
                buildBatchStmt.setObject(i++, buildBatch.getRegion());
                buildBatchStmt.setObject(i++, getTimestamp(buildBatch.getCreatedAt()));
                buildBatchStmt.setObject(i, getTimestamp(buildBatch.getUpdatedAt()));
                buildBatchStmt.execute();
                return getBuildBatchIdOrFetch(buildBatchStmt);
            }
        }));
    }

    private String getBuildBatchIdOrFetch(PreparedStatement insertStmt) throws SQLException {
        String id;
        try (ResultSet rs = insertStmt.getGeneratedKeys()) {
            if (rs.next()) {
                id = rs.getString(1);
            } else {
                throw new SQLException("Failed to get build batch row id");
            }
        }
        return id;
    }

    public DbListResponse<DbAWSDevToolsBuildBatch> list(String company,
                                                        AWSDevToolsBuildBatchesFilter filter,
                                                        Map<String, SortingOrder> sortBy,
                                                        Integer pageNumber,
                                                        Integer pageSize)
            throws SQLException {
        Map<String, Object> params = new HashMap<>();
        Map<String, List<String>> conditions = createWhereClauseAndUpdateParams(params, filter.getProjectNames(),
                filter.getLastPhases(), filter.getLastPhaseStatuses(), filter.getStatuses(),
                filter.getSourceTypes(), filter.getInitiators(), filter.getRegions(), filter.getIntegrationIds());
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (BUILD_BATCHES_SORTABLE_COLUMNS.contains(entry.getKey()))
                        return entry.getKey();
                    return "build_batch_started_at";
                })
                .orElse("build_batch_started_at");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        String buildBatchesWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(AWS_DEV_TOOLS_BUILD_BATCHES))) {
            buildBatchesWhere = " WHERE " + String.join(" AND ", conditions.get(AWS_DEV_TOOLS_BUILD_BATCHES));
        }
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        List<DbAWSDevToolsBuildBatch> results = List.of();
        if (pageSize > 0) {
            String sql = "SELECT * FROM " + company + "." + AWS_DEV_TOOLS_BUILD_BATCHES + buildBatchesWhere
                    + " ORDER BY " + sortByKey + " " + sortOrder.toString()
                    + " OFFSET :skip LIMIT :limit";
            results = template.query(sql, params, DbAWSDevToolsBuildBatchConverters.listRowMapper());
        }
        String countSql = "SELECT count(*) FROM " + company + "." + AWS_DEV_TOOLS_BUILD_BATCHES + buildBatchesWhere;
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company,
                                                                   AWSDevToolsBuildBatchesFilter filter,
                                                                   String configTableKey) {
        AWSDevToolsBuildBatchesFilter.DISTINCT across = filter.getAcross();
        Validate.notNull(across, "Across must be present for group by query");
        if (StringUtils.isNotEmpty(configTableKey)) {
            across = AWSDevToolsBuildBatchesFilter.DISTINCT.none;
        }
        final AWSDevToolsBuildBatchesFilter.CALCULATION calculation = MoreObjects.firstNonNull(
                filter.getCalculation(), AWSDevToolsBuildBatchesFilter.CALCULATION.build_batch_count);
        long currentTime = (new Date()).toInstant().getEpochSecond();
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
            case build_batch_count:
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
        final Map<String, List<String>> conditions = createWhereClauseAndUpdateParams(params, filter.getProjectNames(),
                filter.getLastPhases(), filter.getLastPhaseStatuses(), filter.getStatuses(),
                filter.getSourceTypes(), filter.getInitiators(), filter.getRegions(), filter.getIntegrationIds());
        String buildBatchesWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(AWS_DEV_TOOLS_BUILD_BATCHES))) {
            buildBatchesWhere = " WHERE " + String.join(" AND ", conditions.get(AWS_DEV_TOOLS_BUILD_BATCHES));
        }
        String projectsWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(AWS_DEV_TOOLS_PROJECTS))) {
            projectsWhere = " WHERE " + String.join(" AND ", conditions.get(AWS_DEV_TOOLS_PROJECTS));
        }
        String innerJoin = "(SELECT * FROM " + company
                + "." + AWS_DEV_TOOLS_BUILD_BATCHES + " " + buildBatchesWhere + " ) AS build_batches INNER JOIN "
                + " (SELECT * FROM " + company + "."
                + AWS_DEV_TOOLS_PROJECTS + " " + projectsWhere + ") AS projects on build_batches.project_arn=projects.arn) " +
                "AS final_table";
        List<DbAggregationResult> dbAggregationResults;
        if (StringUtils.isNotEmpty(configTableKey)) {
            sql = "SELECT " + "'" + configTableKey + "'" + " as config_key "
                    + ", " + calculationComponent
                    + " FROM ( SELECT build_batches.*,"
                    + "(extract(epoch FROM COALESCE(build_batch_ended_at,TO_TIMESTAMP(" + currentTime + ")))"
                    + "-extract(epoch FROM build_batch_started_at)) AS duration"
                    + " FROM " + innerJoin
                    + " ORDER BY " + orderByString;
            dbAggregationResults = template.query(sql, params,
                    DbAWSDevToolsBuildBatchConverters.aggRowMapper("config_key", calculation));
        } else {
            sql = "SELECT " + selectDistinctString
                    + ", " + calculationComponent
                    + " FROM ( SELECT build_batches.*, "
                    + "(extract(epoch FROM COALESCE(build_batch_ended_at,TO_TIMESTAMP(" + currentTime + ")))"
                    + "-extract(epoch FROM build_batch_started_at)) AS duration"
                    + " FROM " + innerJoin
                    + groupByString
                    + " ORDER BY " + orderByString;
            dbAggregationResults = template.query(sql, params,
                    DbAWSDevToolsBuildBatchConverters.aggRowMapper(across.toString(), calculation));
        }
        return DbListResponse.of(dbAggregationResults, dbAggregationResults.size());
    }

    protected Map<String, List<String>> createWhereClauseAndUpdateParams(Map<String, Object> params,
                                                                         List<String> projectNames,
                                                                         List<String> lastPhases,
                                                                         List<String> lastPhaseStatuses,
                                                                         List<String> statuses,
                                                                         List<String> sourceTypes,
                                                                         List<String> initiators,
                                                                         List<String> regions,
                                                                         List<String> integrationIds) {
        List<String> buildBatchConditions = new ArrayList<>();
        List<String> projectConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(projectNames)) {
            projectConditions.add("name IN (:projectnames)");
            buildBatchConditions.add("project_name IN (:projectnames)");
            params.put("projectnames", projectNames);
        }
        if (CollectionUtils.isNotEmpty(lastPhases)) {
            buildBatchConditions.add("last_phase IN (:lastphases)");
            params.put("lastphases", lastPhases);
        }
        if (CollectionUtils.isNotEmpty(lastPhaseStatuses)) {
            buildBatchConditions.add("last_phase_status IN (:lastphasestatuses)");
            params.put("lastphasestatuses", lastPhaseStatuses);
        }
        if (CollectionUtils.isNotEmpty(statuses)) {
            buildBatchConditions.add("status IN (:statuses)");
            params.put("statuses", statuses);
        }
        if (CollectionUtils.isNotEmpty(sourceTypes)) {
            buildBatchConditions.add("source_type IN (:sourcetypes)");
            params.put("sourcetypes", sourceTypes);
        }
        if (CollectionUtils.isNotEmpty(sourceTypes)) {
            buildBatchConditions.add("initiator IN (:initiators)");
            params.put("initiators", initiators);
        }
        if (CollectionUtils.isNotEmpty(regions)) {
            buildBatchConditions.add("region IN (:regions)");
            params.put("regions", regions);
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            buildBatchConditions.add("integration_id IN (:integration_ids)");
            params.put("integration_ids", integrationIds.stream()
                    .map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
        }
        return Map.of(AWS_DEV_TOOLS_BUILD_BATCHES, buildBatchConditions, AWS_DEV_TOOLS_PROJECTS, projectConditions);
    }

    public DbListResponse<DbAggregationResult> stackedGroupBy(String company,
                                                              AWSDevToolsBuildBatchesFilter filter,
                                                              List<AWSDevToolsBuildBatchesFilter.DISTINCT> stacks,
                                                              String configTableKey)
            throws SQLException {
        Set<AWSDevToolsBuildBatchesFilter.DISTINCT> stackSupported = Set.of(
                AWSDevToolsBuildBatchesFilter.DISTINCT.project_name,
                AWSDevToolsBuildBatchesFilter.DISTINCT.last_phase,
                AWSDevToolsBuildBatchesFilter.DISTINCT.last_phase_status,
                AWSDevToolsBuildBatchesFilter.DISTINCT.status,
                AWSDevToolsBuildBatchesFilter.DISTINCT.source_type,
                AWSDevToolsBuildBatchesFilter.DISTINCT.initiator,
                AWSDevToolsBuildBatchesFilter.DISTINCT.region
        );
        DbListResponse<DbAggregationResult> result = groupByAndCalculate(company, filter, configTableKey);
        if (stacks == null
                || stacks.size() == 0
                || !stackSupported.contains(stacks.get(0))
                || !stackSupported.contains(filter.getAcross()))
            return result;
        AWSDevToolsBuildBatchesFilter.DISTINCT stack = stacks.get(0);
        List<DbAggregationResult> finalList = new ArrayList<>();
        for (DbAggregationResult record : result.getRecords()) {
            AWSDevToolsBuildBatchesFilter newFilter;
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
                    case region:
                        newFilter = filter.toBuilder().regions(List.of(record.getKey())).across(stack).build();
                        break;
                    default:
                        throw new SQLException("This stack is not available for awsdevtools build batches." + stack);
                }
            }
            finalList.add(record.toBuilder().stacks(groupByAndCalculate(company, newFilter, null).getRecords()).build());
        }
        return DbListResponse.of(finalList, finalList.size());
    }

    @Override
    public Boolean update(String company, DbAWSDevToolsBuildBatch t) throws SQLException {
        return null;
    }

    @Override
    public Optional<DbAWSDevToolsBuildBatch> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    @Override
    public DbListResponse<DbAWSDevToolsBuildBatch> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return null;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return null;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        final List<String> ddlStmts = List.of("CREATE TABLE IF NOT EXISTS " + company + "." + AWS_DEV_TOOLS_BUILD_BATCHES +
                " (" +
                " id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                " build_batch_id VARCHAR NOT NULL," +
                " integration_id INTEGER NOT NULL REFERENCES "
                + company + ".integrations(id) ON DELETE CASCADE," +
                " arn VARCHAR NOT NULL," +
                " build_batch_number BIGINT NOT NULL," +
                " build_batch_started_at TIMESTAMP WITH TIME ZONE," +
                " build_batch_ended_at TIMESTAMP WITH TIME ZONE," +
                " build_batch_complete VARCHAR," +
                " last_phase VARCHAR," +
                " last_phase_status VARCHAR," +
                " status VARCHAR NOT NULL," +
                " project_name VARCHAR NOT NULL," +
                " project_arn VARCHAR NOT NULL," +
                " initiator VARCHAR NOT NULL," +
                " source_version VARCHAR," +
                " resolved_source_version VARCHAR," +
                " source_type VARCHAR NOT NULL," +
                " source_location VARCHAR," +
                " region VARCHAR NOT NULL," +
                " created_at DATE NOT NULL," +
                " updated_at DATE NOT NULL," +
                " UNIQUE(build_batch_id, integration_id)" +
                ")");
        ddlStmts.forEach(ddlStatement -> template.getJdbcTemplate().execute(ddlStatement));
        return true;
    }

    private Timestamp getTimestamp(Date date) {
        return date != null ? Timestamp.from(date.toInstant()) : null;
    }
}
