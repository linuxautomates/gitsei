package io.levelops.commons.databases.services.temporary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.services.queryops.QueryField;
import io.levelops.commons.databases.services.queryops.QueryGroup;
import io.levelops.commons.databases.services.temporary.model.TemporaryRow;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.commons.models.IngestionDataEntity;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.sql.DataSource;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public abstract class StreamsDefaultQueryTable<T extends IngestionDataEntity> extends QueryTable<T> {

    private final int batchSize;

    StreamsDefaultQueryTable(DataSource dataSource, String tenant, String tableName, ObjectMapper objectMapper, final int batchSize)
            throws SQLException {
        super(dataSource, tenant, tableName, objectMapper);
        this.batchSize = batchSize;
    }

    public void insertRows(Stream<T> data) throws IOException {
        final MutableInt count = new MutableInt();
        try{
            StreamUtils.forEachPage(data, batchSize, batch -> {
                try {
                    count.add(batch.size());
                    insertRows(batch);
                } catch (SQLException e) {
                    throw new RuntimeStreamException(e.getClass().getSimpleName() + " during function call:", e);
                }
            });
        } catch (RuntimeStreamException e){
            log.error("Records inserted before exception: {}", count.getValue());
            throw new IOException(e);
        }
        log.info("Records inserted: {}", count.getValue());
    }

    @Override
    public void insertRows(List<T> batch) throws SQLException {
        internalInsertRows(
            batch.stream()
                .map(entity -> {
                            try {
                                return TemporaryRow.builder()
                                        .id(entity.getId())
                                        .levelopsDataField(objectMapper.writeValueAsString(entity))
                                        .updatedAt(entity.getUpdatedAt()).build();
                            } catch (JsonProcessingException e) {
                                throw new RuntimeStreamException(e.getClass().getSimpleName() + " during function call:", e);
                            }
                        })
                .collect(Collectors.toList())
        );
    }

    protected abstract T parseDBEntity(final String dbEntity) throws JsonMappingException, JsonProcessingException;



    @Override
    public List<T> getRows(List<QueryGroup> queryGroups, Boolean useAndOperator, Integer pageNum,
            Integer pageSize) throws SQLException {
        String query = "SELECT * FROM ( SELECT DISTINCT ON (id) * FROM " + effectiveTableName + " ";
        PreparedStatement pstmt = prepareQueryStmt(query, " ) t ORDER BY updatedat DESC LIMIT "
                + pageSize + " OFFSET " + pageNum * pageSize, queryGroups, useAndOperator);
        ResultSet rs = pstmt.executeQuery();
        List<T> data = new ArrayList<>();
        while (rs.next()) {
            try {
                data.add(parseDBEntity(rs.getString("_levelops_data_field")));
            } catch (IOException e) {
                throw new SQLException("failed to parse data.");
            }
        }
        return data;
    }

    @Override
    public Integer countRows(List<QueryGroup> queryGroups, Boolean useAndOperator) throws SQLException {
        String query = "SELECT COUNT(*) FROM ( SELECT DISTINCT ON " +
                "(id) id FROM " + effectiveTableName + " ";
        PreparedStatement pstmt = prepareQueryStmt(query, " ) as counted", queryGroups, useAndOperator);
        ResultSet rs = pstmt.executeQuery();
        rs.next();
        return rs.getInt("count");
    }

    @Override
    public List<String> distinctValues(QueryField queryField) throws SQLException {
        //distinct requires a non null check op
        if (queryField.getOp() != QueryField.Operation.NON_NULL_CHECK) {
            throw new UnsupportedOperationException();
        }
        String query = "SELECT DISTINCT(" + queryField.getQueryFieldName() + ") as "
                + queryField.getFieldName() + " FROM " + effectiveTableName + " ";
        PreparedStatement pstmt = prepareQueryStmt(query, "", List.of(new QueryGroup(List.of(queryField),
                QueryGroup.GroupOperator.OR)), false);
        log.info("Getting unique values with: {}", pstmt);
        ResultSet rs = pstmt.executeQuery();
        List<String> vals = new ArrayList<>();
        while (rs.next()) {
            vals.add(rs.getString(queryField.getFieldName()));
        }
        return vals;
    }

}