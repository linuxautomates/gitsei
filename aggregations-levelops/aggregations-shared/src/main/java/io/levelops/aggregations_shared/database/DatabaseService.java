package io.levelops.aggregations_shared.database;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.SQLException;

public interface DatabaseService<V> {
    NamedParameterJdbcTemplate getTemplate();
    void ensureTableExistence() throws SQLException;
}
