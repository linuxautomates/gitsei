package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.databases.issue_management.DbProject;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Log4j2
public class IssueMgmtProjectService extends DatabaseService<DbProject> {

    public static final String TABLE_NAME = "issue_mgmt_project";

    private static final String INSERT_SQL_FORMAT = "INSERT INTO %s.issue_mgmt_project (project_id, name, " +
            "integration_id, project_key, components, is_private, lead_user_id, attributes)"
            + " VALUES(?,?,?,?,?,?,?,to_json(?::jsonb))\n" +
            "ON CONFLICT(project_id, integration_id) " +
            "DO UPDATE SET (project_key, components, is_private, lead_user_id, attributes) = " +
            "(EXCLUDED.project_key, EXCLUDED.components, EXCLUDED.is_private, EXCLUDED.lead_user_id, EXCLUDED.attributes)\n" +
            "RETURNING id";


    private final NamedParameterJdbcTemplate template;

    @Autowired
    public IssueMgmtProjectService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public String insert(String company, DbProject dbProject) throws SQLException {
        String insertSql = String.format(INSERT_SQL_FORMAT, company);
        UUID workItemHistoryJobId;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pstmt.setObject(++i, dbProject.getProjectId());
            pstmt.setObject(++i, dbProject.getName());
            pstmt.setObject(++i, dbProject.getIntegrationId());
            pstmt.setObject(++i, dbProject.getProjectKey());
            pstmt.setObject(++i, dbProject.getComponents());
            pstmt.setObject(++i, dbProject.getIsPrivate());
            pstmt.setObject(++i, dbProject.getLeadUserId());
            try {
                pstmt.setObject(++i, DefaultObjectMapper.get().writeValueAsString(dbProject.getAttributes()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize attributes json. will store empty json.", e);
            }
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows <= 0) {
                throw new SQLException("Failed to create issue management project!");
            }
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("Failed to create issue management project!");
                }
                workItemHistoryJobId = (UUID) rs.getObject(1);
                return workItemHistoryJobId.toString();
            }
        }
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public Boolean update(String company, DbProject t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DbProject> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DbListResponse<DbProject> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of(
                "CREATE TABLE IF NOT EXISTS {0}.issue_mgmt_project(\n" +
                        "    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    project_id              VARCHAR NOT NULL,\n" +
                        "    name                    VARCHAR NOT NULL,\n" +
                        "    integration_id          INTEGER NOT NULL REFERENCES " + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    project_key             VARCHAR,\n" +
                        "    components              VARCHAR[],\n" +
                        "    is_private              BOOLEAN,\n" +
                        "    lead_user_id            VARCHAR, \n" +
                        "    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(), \n" +
                        "    attributes              JSONB NOT NULL DEFAULT '''{}'''::jsonb\n" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS issue_mgmt_project_project_id_integration_id_idx on" +
                        " {0}.issue_mgmt_project (project_id, integration_id)"
        );
        ddl.stream()
                .map(statement -> MessageFormat.format(statement, company))
                .forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
