package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.testrails.DbTestRailsMilestone;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsProject;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Date;
import java.util.*;

@Log4j2
@Service
public class TestRailsProjectDatabaseService extends DatabaseService<DbTestRailsProject> {

    private static final String TESTRAILS_PROJECTS = "testrails_projects";
    private static final String TESTRAILS_MILESTONES = "testrails_milestones";

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public TestRailsProjectDatabaseService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbTestRailsProject project) throws SQLException {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            final int integrationId = NumberUtils.toInt(project.getIntegrationId());
            String insertProject = "INSERT INTO " + company + "." + TESTRAILS_PROJECTS + " (project_id, integration_id, " +
                    "name, completed_at, description, is_completed, url) VALUES(?,?,?,?,?,?,?) ON CONFLICT " +
                    "(project_id, integration_id) DO UPDATE SET name=EXCLUDED.name, completed_at=EXCLUDED.completed_at, " +
                    "description=EXCLUDED.description, is_completed=EXCLUDED.is_completed, url=EXCLUDED.url";
            String insertMilestone = "INSERT INTO " + company + "." + TESTRAILS_MILESTONES + " (integration_id, " +
                    "milestone_id, name, project_id, parent_id, url, description, refs, start_on, due_on, started_on, " +
                    "completed_on, is_started, is_completed) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT " +
                    "(milestone_id, integration_id) DO UPDATE SET name=EXCLUDED.name, project_id=EXCLUDED.project_id, " +
                    "parent_id=EXCLUDED.parent_id, url=EXCLUDED.url, description=EXCLUDED.description, refs=EXCLUDED.refs" +
                    ", start_on=EXCLUDED.start_on, due_on=EXCLUDED.due_on, started_on=EXCLUDED.started_on, " +
                    "completed_on=EXCLUDED.completed_on, is_started=EXCLUDED.is_started, " +
                    "is_completed=EXCLUDED.is_completed";
            try (PreparedStatement projectStmt = conn.prepareStatement(insertProject, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement milestoneStmt = conn.prepareStatement(insertMilestone, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                projectStmt.setObject(i++, project.getProjectId());
                projectStmt.setObject(i++, integrationId);
                projectStmt.setObject(i++, project.getName());
                projectStmt.setObject(i++, getTimestamp(project.getCompletedOn()));
                projectStmt.setObject(i++, project.getDescription());
                projectStmt.setObject(i++, project.getIsCompleted());
                projectStmt.setObject(i, project.getUrl());
                projectStmt.executeUpdate();
                String projectId = getProjectIdOrFetch(projectStmt, company, project.getProjectId(), integrationId);
                final UUID projectUuid = UUID.fromString(projectId);

                if (CollectionUtils.isNotEmpty(project.getMilestones())) {
                    int count = 0;
                    for (DbTestRailsMilestone milestone : project.getMilestones()) {
                        int index = 1;
                        milestoneStmt.setObject(index++, integrationId);
                        milestoneStmt.setObject(index++, milestone.getMilestoneId());
                        milestoneStmt.setObject(index++, milestone.getName());
                        milestoneStmt.setObject(index++, projectUuid);
                        milestoneStmt.setObject(index++, milestone.getParentId());
                        milestoneStmt.setObject(index++, milestone.getUrl());
                        milestoneStmt.setObject(index++, milestone.getDescription());
                        milestoneStmt.setObject(index++, milestone.getRefs());
                        milestoneStmt.setObject(index++, getTimestamp(milestone.getStartOn()));
                        milestoneStmt.setObject(index++, getTimestamp(milestone.getDueOn()));
                        milestoneStmt.setObject(index++, getTimestamp(milestone.getStartedOn()));
                        milestoneStmt.setObject(index++, getTimestamp(milestone.getCompletedOn()));
                        milestoneStmt.setObject(index++, milestone.getIsStarted());
                        milestoneStmt.setObject(index, milestone.getIsCompleted());
                        milestoneStmt.addBatch();
                        count++;
                        if (count % 100 == 0) {
                            milestoneStmt.executeBatch();
                        }
                    }
                    if (count % 100 != 0) {
                        milestoneStmt.executeBatch();
                    }
                }
                return projectId;
            }
        }));
    }

    private String getProjectIdOrFetch(PreparedStatement insertStmt, String company, int projectId,
                                       int integrationId) throws SQLException {
        String id;
        try (ResultSet rs = insertStmt.getGeneratedKeys()) {
            if (rs.next())
                id = rs.getString(1);
            else {
                final Optional<String> idOpt = getProjectId(company, projectId, integrationId);
                if (idOpt.isPresent()) {
                    id = idOpt.get();
                } else {
                    throw new SQLException("Failed to get project row id");
                }
            }
        }
        return id;
    }

    private Optional<String> getProjectId(String company, int projectId, int integrationId) {
        String query = "SELECT id FROM " + company + "." + TESTRAILS_PROJECTS + " WHERE " +
                " integration_id = :integration_id AND project_id = :project_id";
        final Map<String, Object> params = Map.of(
                "integration_id", integrationId,
                "project_id", projectId);
        return Optional.ofNullable(template.query(query, params,
                rs -> rs.next() ? rs.getString("id") : null));
    }

    @Override
    public Boolean update(String company, DbTestRailsProject t) throws SQLException {
        throw new UnsupportedOperationException("Upsert is implemented in insert");
    }

    @Override
    public Optional<DbTestRailsProject> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    @Override
    public DbListResponse<DbTestRailsProject> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return null;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return false;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        final List<String> ddlStmts = List.of("CREATE TABLE IF NOT EXISTS " + company + "." + TESTRAILS_PROJECTS +
                        " (" +
                        " id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " project_id INTEGER NOT NULL," +
                        " integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE," +
                        " name VARCHAR," +
                        " completed_at TIMESTAMP WITH TIME ZONE," +
                        " description VARCHAR," +
                        " is_completed BOOLEAN NOT NULL DEFAULT FALSE," +
                        " url VARCHAR," +
                        " UNIQUE(project_id, integration_id)" +
                        ")",
                "CREATE TABLE IF NOT EXISTS " + company + "." + TESTRAILS_MILESTONES +
                        " (" +
                        " id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE," +
                        " milestone_id INTEGER NOT NULL," +
                        " name VARCHAR," +
                        " project_id UUID NOT NULL REFERENCES " +
                        company + "." + TESTRAILS_PROJECTS + "(id) ON DELETE CASCADE," +
                        " parent_id INTEGER," +
                        " url VARCHAR," +
                        " description VARCHAR," +
                        " refs VARCHAR," +
                        " start_on TIMESTAMP WITH TIME ZONE," +
                        " due_on TIMESTAMP WITH TIME ZONE," +
                        " started_on TIMESTAMP WITH TIME ZONE," +
                        " completed_on TIMESTAMP WITH TIME ZONE," +
                        " is_started BOOLEAN NOT NULL DEFAULT FALSE," +
                        " is_completed BOOLEAN NOT NULL DEFAULT FALSE," +
                        " UNIQUE(milestone_id, integration_id)" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + TESTRAILS_MILESTONES + "_integration_id_milestone_id_cmpd_idx ON " + company + "." + TESTRAILS_MILESTONES + "(integration_id, milestone_id)",
                "CREATE INDEX IF NOT EXISTS " + TESTRAILS_MILESTONES + "_name_idx on " + company + "." + TESTRAILS_MILESTONES + "(name)",
                "CREATE INDEX IF NOT EXISTS " + TESTRAILS_PROJECTS + "_name_idx on " + company + "." + TESTRAILS_PROJECTS + "(name)",
                "CREATE INDEX IF NOT EXISTS " + TESTRAILS_PROJECTS + "_integration_id_project_id_cmpd_idx ON " + company + "." + TESTRAILS_PROJECTS + "(integration_id, project_id)");
        ddlStmts.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

    private Timestamp getTimestamp(Date date) {
        return date != null ? Timestamp.from(date.toInstant()) : null;
    }
}
