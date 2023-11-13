package io.levelops.commons.databases.services;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.DbAWSDevToolsProjectConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsProject;
import io.levelops.commons.databases.models.filters.AWSDevToolsProjectsFilter;
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
public class AWSDevToolsProjectDatabaseService extends DatabaseService<DbAWSDevToolsProject> {

    private static final String AWS_DEV_TOOLS_PROJECTS = "awsdevtools_projects";
    private static final Set<String> SORTABLE_COLUMNS = Set.of("project_created_at, project_modified_at");

    private final NamedParameterJdbcTemplate template;

    @Autowired
    protected AWSDevToolsProjectDatabaseService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbAWSDevToolsProject project) throws SQLException {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            final int integrationId = NumberUtils.toInt(project.getIntegrationId());
            String insertProject = "INSERT INTO " + company + "." + AWS_DEV_TOOLS_PROJECTS + " (integration_id, name, arn, " +
                    "project_created_at, project_modified_at, source_type, source_location, source_version, region, created_at, " +
                    "updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (name, integration_id) DO UPDATE SET arn=EXCLUDED.arn, " +
                    "project_created_at=EXCLUDED.project_created_at, project_modified_at=EXCLUDED.project_modified_at, " +
                    "source_type=EXCLUDED.source_type, source_location=EXCLUDED.source_location, source_version=EXCLUDED.source_version, " +
                    "region=EXCLUDED.region RETURNING id";

            try (PreparedStatement projectStmt = conn.prepareStatement(insertProject, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                projectStmt.setObject(i++, integrationId);
                projectStmt.setObject(i++, project.getName());
                projectStmt.setObject(i++, project.getArn());
                projectStmt.setObject(i++, getTimestamp(project.getProjectCreatedAt()));
                projectStmt.setObject(i++, getTimestamp(project.getProjectModifiedAt()));
                projectStmt.setObject(i++, project.getSourceType());
                projectStmt.setObject(i++, project.getSourceLocation());
                projectStmt.setObject(i++, project.getSourceVersion());
                projectStmt.setObject(i++, project.getRegion());
                projectStmt.setObject(i++, getTimestamp(project.getCreatedAt()));
                projectStmt.setObject(i, getTimestamp(project.getUpdatedAt()));
                projectStmt.execute();
                return getProjectNameOrFetch(projectStmt);
            }
        }));
    }

    private String getProjectNameOrFetch(PreparedStatement insertStmt) throws SQLException {
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

    public DbListResponse<DbAWSDevToolsProject> list(String company,
                                                     AWSDevToolsProjectsFilter filter,
                                                     Map<String, SortingOrder> sortBy,
                                                     Integer pageNumber,
                                                     Integer pageSize) throws SQLException {
        Map<String, Object> params = new HashMap<>();
        final Map<String, List<String>> conditions = createWhereClauseAndUpdateParams(params, filter.getSourceTypes(),
                filter.getRegions(), filter.getIntegrationIds());
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (SORTABLE_COLUMNS.contains(entry.getKey()))
                        return entry.getKey();
                    return "project_created_at";
                })
                .orElse("project_created_at");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        String projectsWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(AWS_DEV_TOOLS_PROJECTS))) {
            projectsWhere = " WHERE " + String.join(" AND ", conditions.get(AWS_DEV_TOOLS_PROJECTS));
        }
        String fromTable = "SELECT * FROM " + company + "." + AWS_DEV_TOOLS_PROJECTS + projectsWhere;
        String query = "SELECT * FROM (" + fromTable + ") AS projects ORDER BY " + sortByKey +
                " " + sortOrder.name() + " OFFSET :skip LIMIT :limit";
        final List<DbAWSDevToolsProject> projects = template.query(query, params,
                DbAWSDevToolsProjectConverters.listRowMapper());
        String countSql = "SELECT count(*) FROM (" + fromTable + ") AS projects";
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(projects, count);
    }

    protected Map<String, List<String>> createWhereClauseAndUpdateParams(Map<String, Object> params,
                                                                         List<String> sourceTypes,
                                                                         List<String> regions,
                                                                         List<String> integrationIds) {
        List<String> projectConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(sourceTypes)) {
            projectConditions.add("source_type IN (:sourcetypes)");
            params.put("sourcetypes", sourceTypes);
        }
        if (CollectionUtils.isNotEmpty(regions)) {
            projectConditions.add("region IN (:regions)");
            params.put("regions", regions);
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            projectConditions.add("integration_id IN (:integration_ids)");
            params.put("integration_ids", integrationIds.stream()
                    .map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
        }
        return Map.of(AWS_DEV_TOOLS_PROJECTS, projectConditions);
    }

    public DbListResponse<DbAggregationResult> stackedGroupBy(String company,
                                                              AWSDevToolsProjectsFilter filter,
                                                              List<AWSDevToolsProjectsFilter.DISTINCT> stacks,
                                                              String configTableKey)
            throws SQLException {
        Set<AWSDevToolsProjectsFilter.DISTINCT> stackSupported = Set.of(
                AWSDevToolsProjectsFilter.DISTINCT.source_type,
                AWSDevToolsProjectsFilter.DISTINCT.region
        );
        DbListResponse<DbAggregationResult> result = groupByAndCalculate(company, filter, configTableKey);
        if (stacks == null
                || stacks.size() == 0
                || !stackSupported.contains(stacks.get(0))
                || !stackSupported.contains(filter.getAcross()))
            return result;
        AWSDevToolsProjectsFilter.DISTINCT stack = stacks.get(0);
        List<DbAggregationResult> dbAggregationResults = new ArrayList<>();
        for (DbAggregationResult record : result.getRecords()) {
            AWSDevToolsProjectsFilter newFilter;
            if (StringUtils.isNotEmpty(configTableKey)) {
                newFilter = filter.toBuilder().across(stack).build();
            } else {
                switch (filter.getAcross()) {
                    case source_type:
                        newFilter = filter.toBuilder().sourceTypes(List.of(record.getKey())).across(stack).build();
                        break;
                    case region:
                        newFilter = filter.toBuilder().regions(List.of(record.getKey())).across(stack).build();
                        break;
                    default:
                        throw new SQLException("This stack is not available for awsdevtools projects." + stack);
                }
            }
            dbAggregationResults.add(record.toBuilder().stacks(groupByAndCalculate(company, newFilter, null).getRecords()).build());
        }
        return DbListResponse.of(dbAggregationResults, dbAggregationResults.size());
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculate(String company,
                                                                   AWSDevToolsProjectsFilter filter,
                                                                   String configTableKey) {
        AWSDevToolsProjectsFilter.DISTINCT across = filter.getAcross();
        Validate.notNull(across, "Across must be present for group by query");
        if (StringUtils.isNotEmpty(configTableKey)) {
            across = AWSDevToolsProjectsFilter.DISTINCT.none;
        }
        final AWSDevToolsProjectsFilter.CALCULATION calculation = MoreObjects.firstNonNull(
                filter.getCalculation(), AWSDevToolsProjectsFilter.CALCULATION.project_count);
        Map<String, Object> params = new HashMap<>();
        String calculationComponent;
        String orderByString;
        String sql;
        switch (calculation) {
            case project_count:
            default:
                calculationComponent = " COUNT(id) as ct ";
                orderByString = " ct DESC ";
                break;
        }
        String groupByString;
        String selectDistinctString;
        switch (across) {
            case source_type:
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
        Map<String, List<String>> conditions = createWhereClauseAndUpdateParams(params, filter.getSourceTypes(),
                filter.getRegions(), filter.getIntegrationIds());
        String projectsWhere = "";
        if (CollectionUtils.isNotEmpty(conditions.get(AWS_DEV_TOOLS_PROJECTS))) {
            projectsWhere = " WHERE " + String.join(" AND ", conditions.get(AWS_DEV_TOOLS_PROJECTS));
        }
        String projectSql = "( SELECT * FROM " + company + "." + AWS_DEV_TOOLS_PROJECTS + projectsWhere
                + " ) AS projects) ";
        List<DbAggregationResult> dbAggregationResults;
        if (StringUtils.isNotEmpty(configTableKey)) {
            sql = "SELECT " + "'" + configTableKey + "'" + " as config_key "
                    + ", " + calculationComponent
                    + " FROM ( SELECT projects.*"
                    + " FROM " + projectSql + " AS final_table "
                    + " ORDER BY " + orderByString;
            dbAggregationResults = template.query(sql, params,
                    DbAWSDevToolsProjectConverters.aggRowMapper("config_key", calculation));
        } else {
            sql = "SELECT " + selectDistinctString
                    + ", " + calculationComponent
                    + " FROM ( SELECT projects.*"
                    + " FROM " + projectSql + " AS final_table "
                    + groupByString
                    + " ORDER BY " + orderByString;
            dbAggregationResults = template.query(sql, params,
                    DbAWSDevToolsProjectConverters.aggRowMapper(across.toString(), calculation));
        }
        return DbListResponse.of(dbAggregationResults, dbAggregationResults.size());
    }

    @Override
    public Boolean update(String company, DbAWSDevToolsProject project) throws SQLException {
        return null;
    }

    @Override
    public Optional<DbAWSDevToolsProject> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    @Override
    public DbListResponse<DbAWSDevToolsProject> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return null;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return null;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        final List<String> ddlStmts = List.of("CREATE TABLE IF NOT EXISTS " + company + "." + AWS_DEV_TOOLS_PROJECTS +
                " (" +
                " id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                " integration_id INTEGER NOT NULL REFERENCES "
                + company + ".integrations(id) ON DELETE CASCADE," +
                " name VARCHAR NOT NULL," +
                " arn VARCHAR," +
                " project_created_at TIMESTAMP WITH TIME ZONE," +
                " project_modified_at TIMESTAMP WITH TIME ZONE," +
                " source_type VARCHAR," +
                " source_location VARCHAR," +
                " source_version VARCHAR," +
                " region VARCHAR," +
                " created_at TIMESTAMP WITH TIME ZONE," +
                " updated_at TIMESTAMP WITH TIME ZONE," +
                " UNIQUE(name, integration_id)" +
                ")");
        ddlStmts.forEach(ddlStatement -> template.getJdbcTemplate().execute(ddlStatement));
        return true;
    }

    private Timestamp getTimestamp(Date date) {
        return date != null ? Timestamp.from(date.toInstant()) : null;
    }
}
