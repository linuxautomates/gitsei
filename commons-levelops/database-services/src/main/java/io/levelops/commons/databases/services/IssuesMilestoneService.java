package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.databases.converters.DbIssuesMilestoneConverters;
import io.levelops.commons.databases.issue_management.DbIssuesMilestone;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.azuredevops.DbIssueSprint;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.query_criteria.WorkItemMilestoneQueryCriteria;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.Query;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Log4j2
public class IssuesMilestoneService extends DatabaseService<DbIssuesMilestone> {

    public static final String TABLE_NAME = "issue_mgmt_milestones";
    private static final String MILESTONES_WORKITEM_TABLE = "issue_mgmt_milestones_workitem_mappings";

    private static final String INSERT_SQL_FORMAT = "INSERT INTO %s.issue_mgmt_milestones (field_type, field_value, " +
            "parent_field_value, name, integration_id, project_id, state, start_date, end_date, completed_at, attributes)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?,to_json(?::jsonb))\n" +
            "ON CONFLICT(field_type, field_value, integration_id) " +
            "DO UPDATE SET (parent_field_value, name, project_id, state, start_date, end_date, completed_at, attributes) " +
            "= (EXCLUDED.parent_field_value, EXCLUDED.name, EXCLUDED.project_id, EXCLUDED.state," +
            " EXCLUDED.start_date, EXCLUDED.end_date, EXCLUDED.completed_at, EXCLUDED.attributes)\n" +
            "RETURNING id";

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public IssuesMilestoneService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public String insert(String company, DbIssuesMilestone dbIssuesMilestone) throws SQLException {
        String insertSql = String.format(INSERT_SQL_FORMAT, company);
        UUID workItemMilestoneJobId;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pstmt.setObject(++i, dbIssuesMilestone.getFieldType());
            pstmt.setObject(++i, dbIssuesMilestone.getFieldValue());
            pstmt.setObject(++i, dbIssuesMilestone.getParentFieldValue());
            pstmt.setObject(++i, dbIssuesMilestone.getName());
            pstmt.setObject(++i, dbIssuesMilestone.getIntegrationId());
            pstmt.setObject(++i, dbIssuesMilestone.getProjectId());
            pstmt.setObject(++i, dbIssuesMilestone.getState());
            pstmt.setTimestamp(++i, dbIssuesMilestone.getStartDate());
            pstmt.setTimestamp(++i, dbIssuesMilestone.getEndDate());
            pstmt.setTimestamp(++i, dbIssuesMilestone.getCompletedAt());
            try {
                pstmt.setObject(++i, DefaultObjectMapper.get().writeValueAsString(dbIssuesMilestone.getAttributes()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize attributes json. will store empty json.", e);
            }
            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            if (affectedRows <= 0) {
                throw new SQLException("Failed to create Milestone!");
            }
            // get the ID back
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("Failed to create Milestone!");
                }
                workItemMilestoneJobId = (UUID) rs.getObject(1);
                return workItemMilestoneJobId.toString();
            }
        }
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class, WorkItemsService.class);
    }

    @Override
    public Boolean update(String company, DbIssuesMilestone t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DbIssuesMilestone> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Optional<DbIssuesMilestone> getMilestone(String company, String integrationId,
                                                    String fieldType, String fieldValue) {
        Validate.notBlank(fieldType, "Missing fieldType.");
        Validate.notEmpty(fieldValue, "Missing fieldValue.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + TABLE_NAME
                + " WHERE field_type = :fieldType AND field_value = :fieldValue AND integration_id = :integrationId";
        Map<String, Object> params = Map.of("fieldType", fieldType, "fieldValue", fieldValue,
                "integrationId", NumberUtils.toInt(integrationId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbIssuesMilestone> data = template.query(sql, params, DbIssuesMilestoneConverters.milestoneRowMapper());
        return data.stream().findFirst();
    }

    public Optional<DbIssuesMilestone> getMilestoneByParentKeyAndName(String company, String integrationId,
                                                                      String fieldType, String parentKey, String name) {
        Validate.notBlank(fieldType, "Missing fieldType.");
        Validate.notEmpty(parentKey, "Missing parentKey.");
        Validate.notEmpty(name, "Missing name.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + TABLE_NAME
                + " WHERE field_type = :fieldType AND parent_field_value = :parentKey AND name = :name AND integration_id = :integrationId";
        Map<String, Object> params = Map.of("fieldType", fieldType, "parentKey", parentKey,
                "integrationId", NumberUtils.toInt(integrationId), "name", name);
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbIssuesMilestone> data = template.query(sql, params, DbIssuesMilestoneConverters.milestoneRowMapper());
        return data.stream().findFirst();
    }

    @Override
    public DbListResponse<DbIssuesMilestone> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public DbListResponse<DbIssueSprint> listByFilter(String company, WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                      Integer pageNumber, Integer pageSize) throws SQLException {
        log.info("listByFilter: API Being hit for {}, pageNumber-{} and pageSize-{}", company, pageNumber, pageSize);
        Map<String, SortingOrder> sortBy = workItemsMilestoneFilter.getSort();
        Query.QueryConditions queryConditions = WorkItemMilestoneQueryCriteria.getSelectionCriteria(workItemsMilestoneFilter, null)
                .getCriteria();
        String whereClause = (CollectionUtils.isNotEmpty(queryConditions.getConditions()))
                ? " WHERE " + String.join(" AND ", queryConditions.getConditions()) : "";
        String orderByString = "";
        if (MapUtils.isNotEmpty(sortBy)) {
            String groupByField = sortBy.keySet().stream().findFirst().get();
            SortingOrder sortOrder = sortBy.values().stream().findFirst().get();
            orderByString = " ORDER BY LOWER(" + groupByField + ") " + sortOrder + " NULLS LAST ";
        }
        if (workItemsMilestoneFilter.getSprintCount() > 0) {
            pageSize = Math.min(pageSize, workItemsMilestoneFilter.getSprintCount());
        }
        String sql = "SELECT *" +
                " FROM " + company + "." + TABLE_NAME + "" + whereClause + orderByString
                + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSQL = "SELECT COUNT(*) FROM ( SELECT w.id FROM " + company
                + "." + TABLE_NAME + " as w" + whereClause + ") as ct";
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", queryConditions.getQueryParams());
        log.info("list: Parsing DbIssuesMilestoneListResponse");
        List<DbIssuesMilestone> aggResults = template.query(sql, queryConditions.getQueryParams(), DbIssuesMilestoneConverters.milestoneRowMapper());
        Integer totalCount = template.queryForObject(countSQL, queryConditions.getQueryParams(), Integer.class);
        List<DbIssueSprint> newAggResults = aggResults.stream()
                .map(dbIssuesMilestone -> DbIssueSprint.builder()
                        .id(dbIssuesMilestone.getId())
                        .sprintId(dbIssuesMilestone.getFieldValue())
                        .name(dbIssuesMilestone.getName())
                        .parentSprint(dbIssuesMilestone.getParentFieldValue())
                        .integrationId(dbIssuesMilestone.getIntegrationId())
                        .projectId(dbIssuesMilestone.getProjectId())
                        .state(dbIssuesMilestone.getState())
                        .startDate(dbIssuesMilestone.getStartDate())
                        .endDate(dbIssuesMilestone.getEndDate())
                        .completedDate(dbIssuesMilestone.getCompletedAt()).build()).collect(Collectors.toList());
        return DbListResponse.of(newAggResults, totalCount);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of(
                "CREATE TABLE IF NOT EXISTS {0}.issue_mgmt_milestones(\n" +
                        "    id                         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    field_type                 VARCHAR NOT NULL,\n" +
                        "    field_value                VARCHAR NOT NULL,\n" +
                        "    parent_field_value         VARCHAR,\n" +
                        "    name                       VARCHAR NOT NULL,\n" +
                        "   integration_id              INTEGER NOT NULL REFERENCES " + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    project_id                 VARCHAR NOT NULL,\n" +
                        "    state                      VARCHAR, \n" +
                        "    start_date                 TIMESTAMP WITH TIME ZONE, \n" +
                        "    end_date                   TIMESTAMP WITH TIME ZONE, \n" +
                        "    completed_at               TIMESTAMP WITH TIME ZONE, \n" +
                        "    attributes                 JSONB NOT NULL DEFAULT '''{}'''::jsonb\n" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS issue_mgmt_milestones_field_type_field_value_integration_id_idx on " +
                        "{0}.issue_mgmt_milestones (field_type, field_value, integration_id)",

                // issue mgmt milestones and workItems mapping table
                "CREATE TABLE IF NOT EXISTS " + company + "." + MILESTONES_WORKITEM_TABLE + "(\n" +
                        "   id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "   workitem_id                 UUID NOT NULL REFERENCES " + company + "." + WorkItemsService.TABLE_NAME + "(id) ON DELETE CASCADE,\n" +
                        "   milestone_id                UUID NOT NULL REFERENCES " + company + "." + TABLE_NAME + "(id) ON DELETE CASCADE\n" +
                        ")"
        );
        ddl.stream()
                .map(statement -> MessageFormat.format(statement, company))
                .forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
