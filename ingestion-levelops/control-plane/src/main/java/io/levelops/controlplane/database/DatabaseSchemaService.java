package io.levelops.controlplane.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

import java.sql.SQLException;

@Service
public class DatabaseSchemaService {

    // TODO move to commons

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public DatabaseSchemaService(NamedParameterJdbcTemplate template) {
        this.template = template;
    }

    @PostMapping
    public void ensureSchemaExistence(String schema) throws SQLException {
        template.getJdbcTemplate().execute("CREATE SCHEMA IF NOT EXISTS " + schema);
    }

}
