package io.levelops.commons.databases.services.pagerduty;

import io.levelops.commons.models.DbListResponse;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import io.levelops.commons.databases.models.database.pagerduty.DbPagerDutyIncident;
import io.levelops.commons.databases.services.DatabaseService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

public class PagerDutyEscalationPoliciesDatabaseService extends DatabaseService<DbPagerDutyIncident> {

    public static final String TABLE_NAME = "pd_escalation_policies";
    private final NamedParameterJdbcTemplate template;
    private final List<String> ddl = List.of("");


    private final String INSERT_SQL_FORMAT = "INSERT INTO {0}.{1}(id, pd_service_id, ...) "
                                            + "VALUES(:id, :pdServiceId, ...) "
                                            + "ON CONFLICT(pd_service_id, ) DO NOTHING";

    protected PagerDutyEscalationPoliciesDatabaseService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public String insert(String company, DbPagerDutyIncident incident) throws SQLException {
        String insertSql = String.format(INSERT_SQL_FORMAT, company);
        UUID cicdJobRunStageStepId;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, incident.getPdServiceId());

            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            if (affectedRows <= 0) {
                throw new SQLException("Failed to create cicd job!");
            }
            // get the ID back
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("Failed to create cicd job!");
                }
                cicdJobRunStageStepId = (UUID) rs.getObject(1);
                return cicdJobRunStageStepId.toString();
            }
        }
    }

    @Override
    public Boolean update(String company, DbPagerDutyIncident t) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<DbPagerDutyIncident> get(String company, String param) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DbListResponse<DbPagerDutyIncident> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        ddl.stream()
            .map(st -> MessageFormat.format(st, company, TABLE_NAME))
            .forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    
}
