package io.levelops.commons.databases.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;

import javax.sql.DataSource;
import java.sql.SQLException;

import static io.levelops.commons.databases.services.DatabaseService.LEVELOPS_INVENTORY_SCHEMA;

@Service
public class DatabaseSchemaService {

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public DatabaseSchemaService(DataSource dataSource) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @PostMapping
    public void ensureSchemaExistence(String schema) throws SQLException {
        if (!isSchemaValid(schema)) {
            return;
        }
        template.getJdbcTemplate().execute("CREATE SCHEMA IF NOT EXISTS " + schema);
    }

    private static boolean isSchemaValid(String schema) {
        if (StringUtils.isEmpty(schema)) {
            return false;
        }
        if (LEVELOPS_INVENTORY_SCHEMA.equals(schema)) {
            return true;
        }
        if (DatabaseService.SchemaType.FS_CONTROLLER_SCHEMA.getSchemaName().equals(schema)) {
            return true;
        }
        if (DatabaseService.SchemaType.ST_CONTROLLER_SCHEMA.getSchemaName().equals(schema)) {
            return true;
        }
        if (DatabaseService.SchemaType.CICD_TASK_SCHEMA.getSchemaName().equals(schema)) {
            return true;
        }
        if (DatabaseService.SchemaType.ETL_SCHEMA.getSchemaName().equals(schema)) {
            return true;
        }
        if (DatabaseService.SchemaType.TRELLIS_SCHEMA.getSchemaName().equals(schema)) {
            return true;
        }
        if (DatabaseService.SchemaType.ATLASSIAN_CONNECT_SCHEMA.getSchemaName().equals(schema)) {
            return true;
        }
        return schema.matches("[a-zA-Z0-9_-]+");
    }
}
