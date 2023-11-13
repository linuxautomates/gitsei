package io.levelops.commons.databases.services;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.issue_management.DbIssueMgmtSprintMapping;
import io.levelops.commons.databases.issue_management.DbIssuesMilestone;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.levelops.commons.databases.converters.DbIssueMgmtSprintMappingConverters.mapToDbIssueMgmtSprintMapping;
import static io.levelops.commons.databases.converters.DbIssueMgmtSprintMappingConverters.sprintMappingAndMilestoneRowMapper;

@Log4j2
@Service
public class IssueMgmtSprintMappingDatabaseService extends DatabaseService<DbIssueMgmtSprintMapping> {

    private static final String ISSUE_MGMT_SPRINT_MAPPINGS = "issue_mgmt_sprint_mappings";
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public IssueMgmtSprintMappingDatabaseService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbIssueMgmtSprintMapping t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public String upsert(String company, DbIssueMgmtSprintMapping sprintMapping) {
        Validate.notBlank(sprintMapping.getIntegrationId(), "integrationId cannot be null or empty.");
        Validate.notBlank(sprintMapping.getWorkitemId(), "workitem cannot be null or empty.");
        Validate.notBlank(sprintMapping.getSprintId(), "sprintId cannot be null or empty.");
        Validate.notNull(sprintMapping.getAddedAt(), "addedAt cannot be null.");

        String sql = "INSERT INTO " + company + "." + ISSUE_MGMT_SPRINT_MAPPINGS +
                "(integration_id, workitem_id, sprint_id, added_at, removed_at, planned, delivered, outside_of_sprint, ignorable_workitem_type, story_points_planned, story_points_delivered)" +
                " VALUES " +
                "(:integration_id, :workitem_id, :sprint_id, :added_at, :removed_at, :planned, :delivered, :outside_of_sprint, :ignorable_workitem_type, :story_points_planned, :story_points_delivered)" +
                " ON CONFLICT (integration_id, workitem_id, sprint_id) " +
                " DO UPDATE SET" +
                "   added_at = GREATEST(issue_mgmt_sprint_mappings.added_at, EXCLUDED.added_at), " +
                "   removed_at = GREATEST(issue_mgmt_sprint_mappings.removed_at, EXCLUDED.removed_at), " +
                "   planned = EXCLUDED.planned, " +
                "   delivered = EXCLUDED.delivered, " +
                "   outside_of_sprint = EXCLUDED.outside_of_sprint, " +
                "   ignorable_workitem_type = EXCLUDED.ignorable_workitem_type, " +
                "   story_points_planned = EXCLUDED.story_points_planned, " +
                "   story_points_delivered = EXCLUDED.story_points_delivered " +
                " RETURNING id";

        MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("integration_id", Integer.valueOf(sprintMapping.getIntegrationId()));
        parameterSource.addValue("workitem_id", sprintMapping.getWorkitemId());
        parameterSource.addValue("sprint_id", sprintMapping.getSprintId());
        parameterSource.addValue("added_at", sprintMapping.getAddedAt());
        parameterSource.addValue("removed_at", sprintMapping.getRemovedAt());
        parameterSource.addValue("planned", MoreObjects.firstNonNull(sprintMapping.getPlanned(), false));
        parameterSource.addValue("delivered", MoreObjects.firstNonNull(sprintMapping.getDelivered(), false));
        parameterSource.addValue("outside_of_sprint", MoreObjects.firstNonNull(sprintMapping.getOutsideOfSprint(), false));
        parameterSource.addValue("ignorable_workitem_type", MoreObjects.firstNonNull(sprintMapping.getIgnorableWorkitemType(), false));
        parameterSource.addValue("story_points_planned", MoreObjects.firstNonNull(sprintMapping.getStoryPointsPlanned(), 0));
        parameterSource.addValue("story_points_delivered", MoreObjects.firstNonNull(sprintMapping.getStoryPointsDelivered(), 0));

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, parameterSource, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return null;
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    @Override
    public Boolean update(String company, DbIssueMgmtSprintMapping t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DbIssueMgmtSprintMapping> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public DbIssueMgmtSprintMapping get(String company, String integrationId, String workItemId, String sprintId) throws SQLException {
        Validate.notNull(workItemId, "Missing workitem_id.");
        Validate.notNull(integrationId, "Missing integrationId.");
        Validate.notNull(sprintId, "Missing sprint id.");
        String sql = "SELECT * FROM " + company + "." + ISSUE_MGMT_SPRINT_MAPPINGS
                + " WHERE workitem_id = ?"
                + " AND integration_id = ? "
                + " AND sprint_id = ? "
                + " LIMIT 1 ";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, workItemId);
            pstmt.setObject(2, Integer.valueOf(integrationId));
            pstmt.setObject(3, sprintId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapToDbIssueMgmtSprintMapping(rs);
            }
        }
        return null;
    }

    @Override
    public DbListResponse<DbIssueMgmtSprintMapping> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Fetches the sprintMapping data from the db based on workItemsIds and integrationIds
     *
     * @return List of DbIssueMgmtSprintMapping
     * @throws SQLException
     */
    public DbListResponse<Pair<DbIssueMgmtSprintMapping, DbIssuesMilestone>> list(String company, List<String> workItemIds,
                                                         List<String> integrationIds) throws SQLException {
        Validate.notEmpty(workItemIds, "Missing workitem Ids, It should not be empty.");
        Validate.notEmpty(integrationIds, "Missing integration Ids, It should not be empty");

        String sql = "select * from ( " +
                " select  id as sprint_mapping_id, " +
                "        integration_id as sprint_mapping_integration_id, " +
                "        workitem_id as sprint_mapping_workitem_id, " +
                "        sprint_id as sprint_mapping_sprint_id, " +
                "        added_at as sprint_mapping_added_at, " +
                "        removed_at as sprint_mapping_removed_at, " +
                "        planned as sprint_mapping_planned, " +
                "        delivered as sprint_mapping_delivered, " +
                "        outside_of_sprint as sprint_mapping_outside_of_sprint, " +
                "        ignorable_workitem_type as sprint_mapping_ignorable_workitem_type, " +
                "        story_points_planned as sprint_mapping_story_points_planned, " +
                "        story_points_delivered as sprint_mapping_story_points_delivered, " +
                "        created_at as sprint_mapping_created_at, " +
                "        milestone_id, " +
                "        milestone_field_type, " +
                "        milestone_field_value, " +
                "        milestone_parent_field_value, " +
                "        milestone_name, " +
                "        milestone_integration_id, " +
                "        milestone_project_id, " +
                "        milestone_state, " +
                "        milestone_start_date, " +
                "        milestone_end_date, " +
                "        milestone_completed_at, " +
                "        milestone_attributes " +
                " from " + company + ".issue_mgmt_sprint_mappings as sm " +
                " inner join ( " +
                "    select  id as milestone_id, " +
                "            field_type as milestone_field_type, " +
                "            field_value as milestone_field_value, " +
                "            parent_field_value as milestone_parent_field_value, " +
                "            name as milestone_name, " +
                "            integration_id as milestone_integration_id, " +
                "            project_id as milestone_project_id, " +
                "            state as milestone_state, " +
                "            start_date as milestone_start_date, " +
                "            end_date as milestone_end_date, " +
                "            completed_at as milestone_completed_at, " +
                "            attributes as milestone_attributes " +
                "    from " + company + ".issue_mgmt_milestones " +
                "    where field_type in ('sprint') " +
                " ) milestone ";
        String joiningCondition = " ON  sm.integration_id = milestone.milestone_integration_id " +
                " AND sm.sprint_id = milestone.milestone_parent_field_value || '\\' || milestone.milestone_name " +
                ") smmil";
        String whereCondition = " WHERE sprint_mapping_workitem_id IN ( :workitem_ids ) " +
                " AND sprint_mapping_integration_id::text IN ( :integration_ids ) ";
        Map<String, Object> params = new HashMap<>();
        params.put("workitem_ids", workItemIds);
        params.put("integration_ids", integrationIds);
        sql += joiningCondition + whereCondition;
        log.info("sql = " + sql);
        log.info("params = {}", params);
        List<Pair<DbIssueMgmtSprintMapping, DbIssuesMilestone>> result = template.query(sql, params, sprintMappingAndMilestoneRowMapper());
        return DbListResponse.of(result, result.size());
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + "." + ISSUE_MGMT_SPRINT_MAPPINGS + " (" +
                        "    id                        UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "    integration_id            INTEGER NOT NULL " +
                        "REFERENCES " + company + ".integrations(id) ON DELETE CASCADE," +
                        "    workitem_id               VARCHAR NOT NULL," +
                        "    sprint_id                 VARCHAR NOT NULL," +
                        "    added_at                  BIGINT NOT NULL," +
                        "    planned                   BOOLEAN NOT NULL," +
                        "    delivered                 BOOLEAN NOT NULL," +
                        "    outside_of_sprint         BOOLEAN NOT NULL," +
                        "    ignorable_workitem_type   BOOLEAN NOT NULL," +
                        "    story_points_planned      INTEGER NOT NULL," +
                        "    story_points_delivered    INTEGER NOT NULL," +
                        "    created_at                TIMESTAMPTZ NOT NULL DEFAULT now()," +
                        "    removed_at                BIGINT," +
                        "    UNIQUE (integration_id, sprint_id, workitem_id)" +
                        ")"
        );
        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
