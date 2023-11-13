package io.levelops.commons.databases.services;

import io.levelops.commons.database.ArrayWrapper;
import io.levelops.commons.database.DBUtils;
import io.levelops.commons.databases.models.database.IntegrationAgg;
import io.levelops.commons.models.DbListResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class IntegrationAggService extends DatabaseService<IntegrationAgg> {

    @Autowired
    public IntegrationAggService(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(ProductService.class);
    }

    @Override
    public String insert(String company, IntegrationAgg aggregation) throws SQLException {

        String SQL = "INSERT INTO " + company + ".integaggs(id,successful," +
                "version,integids,type,gcspath) VALUES(?::uuid,?,?,?,?,?)";

        UUID id = (!StringUtils.isEmpty(aggregation.getId())) ? UUID.fromString(aggregation.getId()) : UUID.randomUUID();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {
            Array integIds = conn.createArrayOf("int", aggregation.getIntegrationIds()
                    .stream().map(Integer::parseInt).toArray());

            pstmt.setObject(1, id);
            pstmt.setBoolean(2, aggregation.getSuccessful());
            pstmt.setString(3, aggregation.getVersion());
            pstmt.setArray(4, integIds);
            pstmt.setString(5, aggregation.getType().toString());
            pstmt.setString(6, aggregation.getGcsPath());

            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            if (affectedRows > 0) {
                // get the ID back
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                }
            }
        }
        return id.toString();
    }

    @Override
    public Boolean update(String company, IntegrationAgg integrationAgg) throws SQLException {
        String SQL = "UPDATE " + company + ".integaggs SET ";
        String updates = "";
        String condition = " WHERE id = ?::uuid";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(integrationAgg.getVersion())) {
            updates = "version = ?";
            values.add(integrationAgg.getVersion());
        }
        //no updates
        if (values.size() == 0) {
            return false;
        }

        updates += ", updatedat = ?";
        values.add(Instant.now().getEpochSecond());

        SQL = SQL + updates + condition;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 1; i <= values.size(); i++) {
                pstmt.setObject(i, values.get(i - 1));
            }
            pstmt.setObject(values.size() + 1, UUID.fromString(integrationAgg.getId()));
            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            return affectedRows > 0;
        }
    }

    @Override
    public Optional<IntegrationAgg> get(String company, String integrationId) throws SQLException {
        String SQL = "SELECT id,successful,version,integids,type,gcspath,updatedat," +
                "createdat FROM " + company + ".integaggs WHERE id = ?::uuid LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setObject(1, UUID.fromString(integrationId));

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                List<String> integIds = Arrays.stream((Integer[]) rs.getArray("integids").getArray())
                        .map(Object::toString)
                        .collect(Collectors.toList());
                return Optional.of(IntegrationAgg.builder()
                        .id(rs.getString("id"))
                        .successful(rs.getBoolean("successful"))
                        .version(rs.getString("version"))
                        .integrationIds(integIds)
                        .gcsPath(rs.getString("gcspath"))
                        .type(IntegrationAgg.AnalyticType.fromString(
                                rs.getString("type")))
                        .updatedAt(rs.getLong("updatedat"))
                        .createdAt(rs.getLong("createdat"))
                        .build());
            }
        }
        return Optional.empty();
    }

    //default sorted by createdat
    public DbListResponse<IntegrationAgg> listByFilter(String company,
                                                       List<IntegrationAgg.AnalyticType> types,
                                                       List<String> integrationIds,
                                                       Boolean successful,
                                                       Integer pageNumber,
                                                       Integer pageSize)
            throws SQLException {
        String criteria = " WHERE ";
        ArrayList<Object> values = new ArrayList<>();
        if (successful != null) {
            criteria += (values.size() == 0) ? "successful = ? " : "AND successful = ? ";
            values.add(successful);
        }
        if (CollectionUtils.isNotEmpty(types)) {
            criteria += (values.size() == 0) ? "type = ANY(?::varchar[]) " : "AND type = ANY(?::varchar[]) ";
            values.add(new ArrayWrapper<>(
                    "varchar",
                    types.stream()
                            .map(IntegrationAgg.AnalyticType::toString)
                            .collect(Collectors.toList())));
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            criteria += (values.size() == 0) ? "integids @> ? " : "AND integids @> ? ";
            values.add(new ArrayWrapper<>(
                    "int",
                    integrationIds.stream().map(Integer::parseInt).collect(Collectors.toList())));
        }

        if (values.isEmpty()) {
            criteria = "";
        }

        String SQL = "SELECT id,successful,version,integids,type,gcspath,updatedat," +
                "createdat FROM " + company + ".integaggs " + criteria + " ORDER BY " +
                "createdat DESC LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        List<IntegrationAgg> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(id) FROM " + company + ".integaggs" + criteria;
        Integer totCount = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {
            for (int i = 1; i <= values.size(); i++) {
                Object obj = DBUtils.processArrayValues(conn, values.get(i - 1));
                pstmt.setObject(i, obj);
                pstmt2.setObject(i, obj);
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                List<String> integIds = Arrays.stream((Integer[]) rs.getArray("integids").getArray())
                        .map(Object::toString)
                        .collect(Collectors.toList());
                retval.add(IntegrationAgg.builder()
                        .id(rs.getString("id"))
                        .successful(rs.getBoolean("successful"))
                        .version(rs.getString("version"))
                        .integrationIds(integIds)
                        .type(IntegrationAgg.AnalyticType.fromString(rs.getString("type")))
                        .gcsPath(rs.getString("gcspath"))
                        .updatedAt(rs.getLong("updatedat"))
                        .createdAt(rs.getLong("createdat"))
                        .build());
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

    @Override
    public DbListResponse<IntegrationAgg> list(String company, Integer pageNumber,
                                               Integer pageSize) throws SQLException {
        return listByFilter(company, null, null, null, pageNumber, pageSize);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".integaggs WHERE id = ?::uuid";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setObject(1, UUID.fromString(id));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        String SQL = "CREATE TABLE IF NOT EXISTS " + company + ".integaggs(\n" +
                "        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), \n" +
                "        successful BOOLEAN NOT NULL, \n" +
                "        version VARCHAR NOT NULL, \n" +
                "        integids INTEGER[] NOT NULL, \n" +
                "        type VARCHAR NOT NULL, \n" +
                "        gcspath VARCHAR NOT NULL, \n" +
                "        updatedat BIGINT DEFAULT extract(epoch from now()),\n" +
                "        createdat BIGINT DEFAULT extract(epoch from now())\n" +
                "    )";
        String sqlIndexCreation1 = "CREATE INDEX IF NOT EXISTS integaggs_compound_idx on "
                + company + ".integaggs (successful,type)";
        String sqlIndexCreation2 = "CREATE INDEX IF NOT EXISTS integaggs_createdatdesc_idx on "
                + company + ".integaggs (createdat DESC)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement index1Pstmt = conn.prepareStatement(sqlIndexCreation1);
             PreparedStatement index2Pstmt = conn.prepareStatement(sqlIndexCreation2)) {
            pstmt.execute();
            index1Pstmt.execute();
            index2Pstmt.execute();
            return true;
        }
    }
}
