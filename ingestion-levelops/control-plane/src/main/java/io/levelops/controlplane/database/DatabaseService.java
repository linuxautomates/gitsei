package io.levelops.controlplane.database;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.SQLException;

public interface DatabaseService<V> {
    public NamedParameterJdbcTemplate getTemplate();
    public void ensureTableExistence() throws SQLException;
}