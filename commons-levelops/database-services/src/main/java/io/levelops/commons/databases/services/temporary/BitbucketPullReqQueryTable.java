package io.levelops.commons.databases.services.temporary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.temporary.TempGitPullRequest;
import io.levelops.commons.databases.services.queryops.QueryField;
import io.levelops.commons.databases.services.queryops.QueryGroup;
import io.levelops.commons.databases.services.temporary.model.TemporaryRow;
import lombok.extern.log4j.Log4j2;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Log4j2
public class BitbucketPullReqQueryTable extends QueryTable<TempGitPullRequest> {
    private static String TBL_NAME = "bitbucketpr_qtbl";

    public BitbucketPullReqQueryTable(PGSimpleDataSource pgSimpleDataSource, String tenant,
                                String tblName, ObjectMapper objectMapper)
            throws SQLException {
        super(pgSimpleDataSource, tenant, tblName, objectMapper);
    }

    public BitbucketPullReqQueryTable(PGSimpleDataSource pgSimpleDataSource, String tenant,
                                ObjectMapper objectMapper)
            throws SQLException {
        super(pgSimpleDataSource, tenant, TBL_NAME, objectMapper);
    }

    @Override
    public void insertRows(List<TempGitPullRequest> prs) throws SQLException {
        List<TemporaryRow> data = prs.stream()
                .map(pr -> {
                    try {
                        return TemporaryRow.builder().id(pr.getId())
                                .levelopsDataField(objectMapper.writeValueAsString(pr))
                                .updatedAt(pr.getUpdatedDate())
                                .build();
                    } catch (JsonProcessingException e) {
                        log.warn("Error inserting in temptable.", e);
                    }
                    return null;
                })
                .takeWhile(Objects::nonNull)
                .collect(Collectors.toList());
        if (data.size() != prs.size()) {
            throw new SQLException("Failed to convert some commits to string for insertion.");
        }
        internalInsertRows(data);
    }

    @Override
    public List<TempGitPullRequest> getRows(List<QueryGroup> groups, Boolean useAndOperator, Integer pageNum,
                                            Integer pageSize)
            throws SQLException {
        String query = "SELECT * FROM ( SELECT DISTINCT ON (id) * FROM " + effectiveTableName + " ";
        PreparedStatement pstmt = prepareQueryStmt(query, " ) t ORDER BY updatedat DESC LIMIT "
                + pageSize + " OFFSET " + pageNum * pageSize, groups, useAndOperator);
        ResultSet rs = pstmt.executeQuery();
        List<TempGitPullRequest> data = new ArrayList<>();
        while (rs.next()) {
            try {
                data.add(objectMapper.readValue(rs.getString("_levelops_data_field"),
                        TempGitPullRequest.class));
            } catch (IOException e) {
                throw new SQLException("failed to parse data.");
            }
        }
        return data;
    }

    @Override
    public Integer countRows(List<QueryGroup> groups, Boolean useAndOperator) throws SQLException {
        String query = "SELECT COUNT(*) FROM ( SELECT DISTINCT ON " +
                "(id) id FROM " + effectiveTableName + " ";
        PreparedStatement pstmt = prepareQueryStmt(query, " ) as counted", groups, useAndOperator);
        ResultSet rs = pstmt.executeQuery();
        rs.next();
        return rs.getInt("count");
    }

    @Override
    public List<String> distinctValues(QueryField field) throws SQLException {
        //distinct requires a non null check op
        if (field.getOp() != QueryField.Operation.NON_NULL_CHECK) {
            throw new UnsupportedOperationException();
        }
        String query = "SELECT DISTINCT(" + field.getQueryFieldName() + ") as "
                + field.getFieldName() + " FROM " + effectiveTableName + " ";
        PreparedStatement pstmt = prepareQueryStmt(query, "", List.of(new QueryGroup(List.of(field),
                QueryGroup.GroupOperator.OR)), false);
        ResultSet rs = pstmt.executeQuery();
        List<String> vals = new ArrayList<>();
        while (rs.next()) {
            vals.add(rs.getString(field.getFieldName()));
        }
        return vals;
    }
}
