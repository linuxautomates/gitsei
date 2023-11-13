package io.levelops.commons.databases.services;

import io.levelops.commons.database.ArrayWrapper;
import io.levelops.commons.database.DBUtils;
import io.levelops.commons.databases.models.database.AggregationRecord;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class AggregationsDatabaseService extends DatabaseService<AggregationRecord> {
    private static final String INSERT_SQL_FORMAT = "INSERT INTO %s.aggregations (version,successful,type,tool_type,gcspath,id) VALUES(?,?,?,?,?,?) RETURNING id";
    private static final String UPDATE_SQL_FORMAT = "UPDATE %s.aggregations SET version = ?, successful = ?, type = ?, tool_type = ?, gcspath = ?, updated_at = now() WHERE id = ?";
    private static final String DELETE_SQL_FORMAT = "DELETE FROM %s.aggregations WHERE id = ?";

    private static final String AGGREGATIONS_PRODUCT_MAPPINGS_DELETE_SQL = "DELETE FROM %s.aggregation_product_mappings WHERE aggregation_id = ?";
    private static final String AGGREGATIONS_PRODUCT_MAPPINGS_INSERT_SQL = "INSERT INTO %s.aggregation_product_mappings(aggregation_id,product_id) VALUES(?,?) ON CONFLICT(aggregation_id,product_id) DO NOTHING";


    //"SELECT "
    @Autowired
    public AggregationsDatabaseService(DataSource dataSource) {
        super(dataSource);
    }

    // region get references
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(ProductService.class);
    }
    // endregion

    //region Insert Aggregation Product Mapping
    private void insertAggregationProductIds(PreparedStatement pstmt, UUID aggregationId, AggregationRecord aggregationRecord) throws SQLException {
        if (CollectionUtils.isEmpty(aggregationRecord.getProductIds())) {
            return;
        }
        for (Integer productId : aggregationRecord.getProductIds()) {
            pstmt.setObject(1, aggregationId);
            pstmt.setInt(2, productId);
            pstmt.addBatch();
            pstmt.clearParameters();
        }
        pstmt.executeBatch();
    }
    //endregion

    //region Insert
    @Override
    public String insert(String company, AggregationRecord t) throws SQLException {
        String insertSql = String.format(INSERT_SQL_FORMAT, company);
        String aggregationsProductMappingsInsertSql = String.format(AGGREGATIONS_PRODUCT_MAPPINGS_INSERT_SQL, company);
        UUID aggregationRecordId;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement aggregationsProductMappingsInsertPstmt = conn.prepareStatement(aggregationsProductMappingsInsertSql)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                pstmt.setString(1, t.getVersion());
                pstmt.setBoolean(2, t.getSuccessful());
                pstmt.setString(3, t.getType().toString());
                pstmt.setString(4, t.getToolType());
                pstmt.setString(5, t.getGcsPath());
                pstmt.setObject(6, UUID.fromString(t.getId()));

                int affectedRows = pstmt.executeUpdate();
                // check the affected rows
                if (affectedRows <= 0) {
                    throw new SQLException("Failed to create AggregationRecord!");
                }
                // get the ID back
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("Failed to create AggregationRecord!");
                    }
                    aggregationRecordId = (UUID) rs.getObject(1);
                }
                insertAggregationProductIds(aggregationsProductMappingsInsertPstmt, aggregationRecordId, t);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
            return aggregationRecordId.toString();
        }
    }
    //endregion

    //region Update
    @Override
    public Boolean update(String company, AggregationRecord t) throws SQLException {
        String updateSql = String.format(UPDATE_SQL_FORMAT, company);
        String aggregationsProductMappingsDeleteSql = String.format(AGGREGATIONS_PRODUCT_MAPPINGS_DELETE_SQL, company);
        String aggregationsProductMappingsInsertSql = String.format(AGGREGATIONS_PRODUCT_MAPPINGS_INSERT_SQL, company);
        boolean success = true;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql);
             PreparedStatement aggregationsProductMappingsDeletePstmt = conn.prepareStatement(aggregationsProductMappingsDeleteSql);
             PreparedStatement aggregationsProductMappingsInsertPstmt = conn.prepareStatement(aggregationsProductMappingsInsertSql)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            UUID aggregationRecordId = UUID.fromString(t.getId());
            try {
                pstmt.setString(1, t.getVersion());
                pstmt.setBoolean(2, t.getSuccessful());
                pstmt.setString(3, t.getType().toString());
                pstmt.setString(4, t.getToolType());
                pstmt.setString(5, t.getGcsPath());
                pstmt.setObject(6, aggregationRecordId);

                int affectedRows = pstmt.executeUpdate();
                success = (affectedRows > 0);

                if(CollectionUtils.isNotEmpty(t.getProductIds())){
                    aggregationsProductMappingsDeletePstmt.setObject(1, aggregationRecordId);
                    aggregationsProductMappingsDeletePstmt.executeUpdate();
                    insertAggregationProductIds(aggregationsProductMappingsInsertPstmt, aggregationRecordId, t);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
            return success;
        }
    }
    //endregion

    //region Get
    @Override
    public Optional<AggregationRecord> get(String company, String id) throws SQLException {
        var results = getBatch(company, 0, 10, Collections.singletonList(UUID.fromString(id)),null,null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }
    //endregion

    private String formatCriterea(String criterea, List<Object> values, String newCriterea){
        String result = criterea + ((values.size() ==0) ? "" : "AND ");
        result += newCriterea;
        return result;
    }

    DbListResponse<AggregationRecord> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<AggregationRecord.Type> types, List<String> toolTypes) throws SQLException {
        //String selectSqlBase = "SELECT * FROM " + company + ".aggregations";
        String selectSqlBase = "SELECT a.id,a.version,a.successful,a.type,a.tool_type,a.gcspath,a.created_at,a.updated_at, \n" +
                "product_ids \n" +
                "FROM " + company + ".aggregations as a \n" +
                "LEFT OUTER JOIN ( \n" +
                "SELECT p.aggregation_id::uuid, array_remove(array_agg(p.product_id), NULL)::integer[] as product_ids FROM " + company + ".aggregation_product_mappings as p \n" +
                "GROUP BY p.aggregation_id ) as p ON p.aggregation_id = a.id \n";


        String orderBy = " ORDER BY created_at DESC ";
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ids)) {
            criteria = formatCriterea(criteria, values, "id = ANY(?::uuid[]) ");
            values.add(new ArrayWrapper<>("uuid", ids));
        }
        if (CollectionUtils.isNotEmpty(types)) {
            criteria = formatCriterea(criteria, values, "type = ANY(?::varchar[]) ");
            values.add(new ArrayWrapper<>("varchar", types.stream().map(AggregationRecord.Type::toString).collect(Collectors.toList())));
        }
        if (CollectionUtils.isNotEmpty(toolTypes)) {
            criteria = formatCriterea(criteria, values, "tool_type = ANY(?::varchar[]) ");
            values.add(new ArrayWrapper<>("varchar", toolTypes));
        }
        criteria = CollectionUtils.isEmpty(values) ? "" : criteria;

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        List<AggregationRecord> retval = new ArrayList<>();
        Integer totCount = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql);
             PreparedStatement pstmt2 = conn.prepareStatement(countSql)) {
            for (int i = 0; i < values.size(); i++) {
                Object obj = DBUtils.processArrayValues(conn, values.get(i));
                pstmt.setObject(i + 1, obj);
                pstmt2.setObject(i + 1, obj);
            }
            log.debug("Get or List Query = {}", pstmt);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                UUID id = (UUID) rs.getObject("id");
                String version = rs.getString("version");
                Boolean successful = rs.getBoolean("successful");
                String type = rs.getString("type");
                String toolType = rs.getString("tool_type");
                String gcspath = rs.getString("gcspath");
                Instant createdAt = DateUtils.toInstant(rs.getTimestamp("created_at"));
                Instant updatedAt = DateUtils.toInstant(rs.getTimestamp("updated_at"));
                Integer[] productIds = rs.getArray("product_ids") != null
                        ? (Integer[]) rs.getArray("product_ids").getArray()
                        : new Integer[0];

                AggregationRecord aggregationRecord = AggregationRecord.builder()
                        .id(id.toString())
                        .version(version)
                        .successful(successful)
                        .type(EnumUtils.getEnumIgnoreCase(AggregationRecord.Type.class, type))
                        .toolType(toolType)
                        .gcsPath(gcspath)
                        .productIds(productIds.length > 0 ? Arrays.asList(productIds) : Collections.emptyList())
                        .createdAt(createdAt)
                        .updatedAt(updatedAt)
                        .build();

                retval.add(aggregationRecord);
            }
            if (retval.size() > 0) {
                totCount = retval.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
                if (retval.size() == pageSize) {
                    rs = pstmt2.executeQuery();
                    if (rs.next()) {
                        totCount = rs.getInt("count");
                    }
                }
            }
        }
        return DbListResponse.of(retval, totCount);
    }

    //region List
    @Override
    public DbListResponse<AggregationRecord> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(company, pageNumber, pageSize, null,null,null);
    }
    public DbListResponse<AggregationRecord> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<AggregationRecord.Type> types, List<String> toolTypes) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids,types,toolTypes);
    }
    //endregion

    //region Delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String deleteSql = String.format(DELETE_SQL_FORMAT, company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setObject(1, UUID.fromString(id));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }
    //endregion

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlStatements = new ArrayList<>();
        String sqlStatement = "CREATE TABLE IF NOT EXISTS " + company + ".aggregations( \n" +
                "        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), \n" +
                "        version VARCHAR NOT NULL, \n" +
                "        successful BOOLEAN NOT NULL, \n" +
                "        type VARCHAR NOT NULL, \n" +
                "        tool_type VARCHAR, \n" +
                "        gcspath VARCHAR NOT NULL, \n" +
                "        updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(), \n" +
                "        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now() \n" +
                "    )";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE INDEX IF NOT EXISTS aggregations_type_idx on "+ company + ".aggregations (type)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE INDEX IF NOT EXISTS aggregations_tool_type_idx on "+ company + ".aggregations (tool_type)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE INDEX IF NOT EXISTS aggregations_created_at_desc_idx on " + company + ".aggregations (created_at DESC)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE TABLE IF NOT EXISTS " + company + ".aggregation_product_mappings( \n" +
                "        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), \n" +
                "        aggregation_id UUID NOT NULL REFERENCES " + company + ".aggregations(id) ON DELETE CASCADE,\n" +
                "        product_id INTEGER NOT NULL REFERENCES " + company + ".products(id) ON DELETE CASCADE\n" +
                "    )";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE UNIQUE INDEX IF NOT EXISTS uniq_aggregation_product_mappings_compound_idx on " + company + ".aggregation_product_mappings (aggregation_id,product_id)";
        sqlStatements.add(sqlStatement);

        try (Connection conn = dataSource.getConnection()) {
            for (String currentSql : sqlStatements) {
                try (PreparedStatement pstmt = conn.prepareStatement(currentSql)) {
                    pstmt.execute();
                }
            }
            return true;
        }
    }
}
