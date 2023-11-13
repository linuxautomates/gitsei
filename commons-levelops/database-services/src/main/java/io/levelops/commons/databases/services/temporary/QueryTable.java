package io.levelops.commons.databases.services.temporary;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.services.queryops.QueryField;
import io.levelops.commons.databases.services.queryops.QueryGroup;
import io.levelops.commons.databases.services.temporary.model.TemporaryRow;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class QueryTable<T> implements AutoCloseable {

    protected Connection connection;
    String effectiveTableName;
    ObjectMapper objectMapper;

    QueryTable(DataSource dataSource, String tenant, String tableName, ObjectMapper objectMapper) throws SQLException {
        this.connection = dataSource.getConnection();
        this.effectiveTableName = tenant + "_" + tableName;
        this.objectMapper = objectMapper;
    }

    public void createTempTable() throws SQLException {
        String tableDefinition = "CREATE TEMP TABLE IF NOT EXISTS " + effectiveTableName + "(\n" +
                "        id VARCHAR PRIMARY KEY, \n" +
                "        _levelops_data_field JSONB NOT NULL, \n" +
                "        updatedat BIGINT NOT NULL \n" +
                "    )";
        connection.prepareStatement(tableDefinition).execute();
    }

    void internalInsertRows(List<TemporaryRow> rows) throws SQLException {
        PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO " + effectiveTableName + "(id,_levelops_data_field,updatedat) " +
                        "VALUES (?,to_json(?::json),?) ON CONFLICT (id) DO UPDATE SET " +
                        "(_levelops_data_field,updatedat) = (EXCLUDED._levelops_data_field," +
                        "EXCLUDED.updatedat) WHERE " + effectiveTableName + ".updatedat < EXCLUDED.updatedat");
        for (TemporaryRow row : rows) {
            pstmt.setString(1, row.getId());
            pstmt.setString(2, row.getLevelopsDataField());
            pstmt.setLong(3, row.getUpdatedAt());
            pstmt.addBatch();
            pstmt.clearParameters();
        }
        pstmt.executeBatch();
    }

    PreparedStatement prepareQueryStmt(String queryPrefix, String querySuffix, List<QueryGroup> queryGroups,
                                       boolean and) throws SQLException {
        StringBuilder queryStmt = new StringBuilder(queryPrefix);
        // this is to prevent creation of duplicate join criteria
        Set<String> joinDeDupe = new HashSet<>();
        StringBuilder criteriaStatement = new StringBuilder();
        List<Object> values = new ArrayList<>();
        boolean firstGlobalCriteria = true;
        for (QueryGroup group : queryGroups) {
            if (!firstGlobalCriteria) {
                criteriaStatement.append(and ? " AND " : " OR ");
                criteriaStatement.append(" ( ");
            }
            boolean firstGroupCriteria = true;
            for (QueryField entry : group.getQueryFields()) {
                //build the join portion of the query
                if (StringUtils.isNotEmpty(entry.getFullJoin())
                        && !joinDeDupe.contains(entry.getFullJoin())) {
                    queryStmt.append(entry.getFullJoin()).append(" ");
                    joinDeDupe.add(entry.getFullJoin());
                }
                //build the query criteria. this portion has to be in here cuz for the first one there is no operator
                if (firstGlobalCriteria) {
                    criteriaStatement.append("WHERE ( ");
                    firstGlobalCriteria = false;
                }

                if (firstGroupCriteria)
                    firstGroupCriteria = false;
                else
                    criteriaStatement.append(group.getGroupOperator().getOp());

                criteriaStatement.append(entry.getQueryWithCriteria());
                values.add(entry.getValueForQuery());
            }
            criteriaStatement.append(" ) ");
        }
        //now merge the join with the criteria
        queryStmt.append(criteriaStatement.toString());
        //finally append the suffix
        if (!StringUtils.isEmpty(querySuffix)) {
            queryStmt.append(querySuffix);
        }
        //prepare the statement
        PreparedStatement pstmt = connection.prepareStatement(queryStmt.toString());
        int i = 1;
        for (Object value : values) {
            if (value != null)
                pstmt.setObject(i++, value);
        }
        return pstmt;
    }

    abstract public void insertRows(List<T> data)
            throws SQLException;

    abstract public List<T> getRows(List<QueryGroup> queryGroups, Boolean useAndOperator,
                                    Integer pageNum, Integer pageSize)
            throws SQLException;

    abstract public Integer countRows(List<QueryGroup> queryGroups, Boolean useAndOperator)
            throws SQLException;

    abstract public List<String> distinctValues(QueryField queryField)
            throws SQLException;

    public void close() throws SQLException {
        connection.close();
    }
}
