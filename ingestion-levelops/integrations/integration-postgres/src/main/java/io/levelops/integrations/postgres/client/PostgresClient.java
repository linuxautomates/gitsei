package io.levelops.integrations.postgres.client;

import com.fasterxml.jackson.annotation.*;
import io.levelops.commons.functional.UncheckedCloseable;
import lombok.Builder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PostgresClient {
    @JsonProperty("db_template")
    private final JdbcTemplate dbTemplate;

    public PostgresClient(JdbcTemplate dbTemplate) {
        this.dbTemplate = dbTemplate;
    }

    public Stream<Row> executeQueryStreamResults(String query) throws SQLException {
        UncheckedCloseable close=null;
        try {
            Connection connection = dbTemplate.getDataSource().getConnection();
            close=UncheckedCloseable.wrap(connection);
            PreparedStatement pSt = connection.prepareStatement(query);
            close=close.nest(pSt);
            connection.setAutoCommit(false);
            pSt.setFetchSize(5000);
            ResultSet resultSet = pSt.executeQuery();
            close=close.nest(resultSet);
            return StreamSupport.stream(new Spliterators.AbstractSpliterator<Row>(
                    Long.MAX_VALUE,Spliterator.ORDERED) {
                @Override
                public boolean tryAdvance(Consumer<? super Row> action) {
                    try {
                        if(!resultSet.next()) return false;
                        action.accept(createRowRecord(resultSet));
                        return true;
                    } catch(SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }, false).onClose(close);
        } catch(SQLException sqlEx) {
            if(close!=null)
                try { close.close(); } catch(Exception ex) { sqlEx.addSuppressed(ex); }
            throw sqlEx;
        }
    }

    public List<Row> executeQuery(final String query){
        List<Row> resultData = this.dbTemplate.query(query, (rs, rowNum) -> createRowRecord(rs));
        return resultData;
    }

    @lombok.Data
    @Builder
    public static final class Row{
        @JsonProperty("columns")
        private final List<Column> columns;
    }

    @lombok.Data
    @Builder
    public static final class Column {
        @JsonProperty("column_name")
        private final String columnName;
        @JsonProperty("value")
        private final String value;
    }

    private Row createRowRecord(ResultSet rs) throws SQLException {
        final ResultSetMetaData meta = rs.getMetaData();
        final int columnCount = meta.getColumnCount();
        final List<Column> columns = new ArrayList<Column>();
        for (int column = 1; column <= columnCount; ++column){
            String columnName = meta.getColumnName(column);
            String value = String.valueOf(rs.getObject(column));
            Column data = Column.builder().columnName(columnName).value(value).build();
            columns.add(data);
        }
        return Row.builder().columns(columns).build();
    }
}
