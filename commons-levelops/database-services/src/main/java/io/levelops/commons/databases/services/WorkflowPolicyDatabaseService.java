package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.workflow.models.WorkflowPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Optional;

@Service
public class WorkflowPolicyDatabaseService extends DatabaseService<WorkflowPolicy> {

    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public WorkflowPolicyDatabaseService(DataSource dataSource, ObjectMapper objectMapper) {
        super(dataSource);
        this.objectMapper = objectMapper;
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public String insert(String company, WorkflowPolicy t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean update(String company, WorkflowPolicy t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<WorkflowPolicy> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DbListResponse<WorkflowPolicy> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        // TODO
        return true;
    }
}
