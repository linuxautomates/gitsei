package io.levelops.commons.databases.services;

import io.levelops.commons.databases.converters.DbWorkItemConverters;
import io.levelops.commons.databases.issue_management.DbWorkItemPrioritySLA;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Log4j2
public class WorkItemsPrioritySLAService extends DatabaseService<DbWorkItemPrioritySLA> {


    public static final String TABLE_NAME = "issue_mgmt_priorities_sla";
    //upsert
    private static final String INSERT_SQL_FORMAT = "INSERT INTO %s." + TABLE_NAME +
            " (priority, workitem_type, project, integration_id) VALUES (?,?,?,?) ON CONFLICT " +
            " (priority, workitem_type, project, integration_id) DO NOTHING";
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public WorkItemsPrioritySLAService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }


    @Override
    public String insert(String company, DbWorkItemPrioritySLA dbWorkItemPrioritySLA) throws SQLException {
        String insertSql = String.format(INSERT_SQL_FORMAT, company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pstmt.setObject(++i, dbWorkItemPrioritySLA.getPriority());
            pstmt.setObject(++i, dbWorkItemPrioritySLA.getWorkitemType());
            pstmt.setObject(++i, dbWorkItemPrioritySLA.getProject());
            pstmt.setObject(++i, Integer.valueOf(dbWorkItemPrioritySLA.getIntegrationId()));
            return String.valueOf(pstmt.executeUpdate());
        }
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public Boolean update(String company, DbWorkItemPrioritySLA t) throws SQLException {
        return null;
    }

    @Override
    public Optional<DbWorkItemPrioritySLA> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    @Override
    public DbListResponse<DbWorkItemPrioritySLA> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return listPrioritiesSla(company, null, null, null, null, pageNumber, pageSize);
    }

    public DbListResponse<DbWorkItemPrioritySLA> listPrioritiesSla(String company,
                                                                   List<String> integrationIds,
                                                                   List<String> projects,
                                                                   List<String> workItemTypes,
                                                                   List<String> priorities,
                                                                   Integer pageNumber,
                                                                   Integer pageSize) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        createWhereConditionsAndUpdateParams(integrationIds, projects, workItemTypes, priorities, conditions, params);
        String where = "";
        if (!conditions.isEmpty()) {
            where = " WHERE " + String.join(" AND ", conditions);
        }
        String sortLimit = " ORDER BY resp_sla DESC, solve_sla DESC OFFSET :skip LIMIT :limit";
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        String sql = "SELECT * FROM " + company + "." + TABLE_NAME + where;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbWorkItemPrioritySLA> results = template.query(
                sql + sortLimit, params, DbWorkItemConverters.prioritySLARowMapper());
        String countSql = "SELECT COUNT(*) FROM ( " + sql + " ) as i";
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }


    /**
     * It fetches the prioritySLA data from the DB based on integrationIds, projects, workItemTypes and priorities
     *
     * @return List of DbWorkItemPrioritySLA
     */
    public DbListResponse<DbWorkItemPrioritySLA> listPrioritySla(String company,
                                                                 List<String> integrationIds,
                                                                 List<String> projects,
                                                                 List<String> workItemTypes,
                                                                 List<String> priorities) {
        Validate.notEmpty(integrationIds, "Integration ids cannot be empty");
        Validate.notEmpty(projects, "Projects cannot be empty");
        Validate.notEmpty(workItemTypes, "WorkItem types cannot be empty");
        Validate.notEmpty(priorities, "Priorities cannot be empty");

        String sql = "SELECT * FROM " + company + "." + TABLE_NAME +
                " WHERE integration_id::text IN (:integrationIds)" +
                " AND project IN (:projects) AND workitem_type IN (:workItemTypes) AND priority IN (:priorities)";
        Map<String, Object> params = new HashMap<>();
        params.put("integrationIds", integrationIds);
        params.put("projects", projects);
        params.put("workItemTypes", workItemTypes);
        params.put("priorities", priorities);
        log.info("sql = " + sql);
        log.info("params: {}", params);

        List<DbWorkItemPrioritySLA> results = template.query(sql, params, DbWorkItemConverters.prioritySLARowMapper());

        return DbListResponse.of(results, results.size());
    }

    public Boolean updatePrioritySla(String company, DbWorkItemPrioritySLA prioritySla) {
        return bulkUpdatePrioritySla(
                company, List.of(prioritySla.getId()), null, null,
                null, null, prioritySla.getRespSla(), prioritySla.getSolveSla()
        ) == 1;
    }

    public Integer bulkUpdatePrioritySla(String company,
                                         List<String> ids,
                                         List<String> integrationIds,
                                         List<String> projects,
                                         List<String> workItemTypes,
                                         List<String> priorities,
                                         Long respSla,
                                         Long solveSla) {
        List<String> updates = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (Objects.nonNull(respSla)) {
            updates.add("resp_sla = :respSla");
            params.put("respSla", respSla);
        }
        if (Objects.nonNull(solveSla)) {
            updates.add("solve_sla = :solveSla");
            params.put("solveSla", solveSla);
        }
        if (updates.isEmpty()) {
            return 0;
        }
        if (CollectionUtils.isNotEmpty(ids)) {
            conditions.add("id IN (:ids)");
            params.put("ids", ids.stream().map(UUID::fromString).collect(Collectors.toList()));
        }
        createWhereConditionsAndUpdateParams(integrationIds, projects, workItemTypes, priorities, conditions, params);
        String whereClause = "";
        if (!conditions.isEmpty()) {
            whereClause = " WHERE " + String.join(" AND ", conditions);
        }
        String sql = "UPDATE " + company + "." + TABLE_NAME + " SET "
                + String.join(", ", updates) + whereClause;
        return template.update(sql, params);
    }

    private void createWhereConditionsAndUpdateParams(List<String> integrationIds, List<String> projects,
                                                      List<String> workItemTypes, List<String> priorities,
                                                      List<String> conditions, Map<String, Object> params) {
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            conditions.add("integration_id IN (:integrationIds)");
            params.put("integrationIds",
                    integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(projects)) {
            conditions.add("project IN (:projects)");
            params.put("projects", projects);
        }
        if (CollectionUtils.isNotEmpty(workItemTypes)) {
            conditions.add("workitem_type IN (:workItemTypes)");
            params.put("workItemTypes", workItemTypes);
        }
        if (CollectionUtils.isNotEmpty(priorities)) {
            conditions.add("priority IN (:priorities)");
            params.put("priorities", priorities);
        }
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return null;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of("CREATE TABLE IF NOT EXISTS {0}." + TABLE_NAME + "(\n" +
                "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    priority VARCHAR NOT NULL,\n" +
                "    project VARCHAR NOT NULL,\n" +
                "    workitem_type VARCHAR NOT NULL,\n" +
                "    resp_sla BIGINT NOT NULL DEFAULT 86400,\n" +
                "    solve_sla BIGINT NOT NULL DEFAULT 86400,\n" +
                "    integration_id INTEGER NOT NULL REFERENCES {0}.integrations(id) ON DELETE CASCADE,\n" +
                "    UNIQUE (priority,workitem_type,project,integration_id)\n" +
                ")"
        );
        ddl.stream().map(statement -> MessageFormat.format(statement, company)).forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}

