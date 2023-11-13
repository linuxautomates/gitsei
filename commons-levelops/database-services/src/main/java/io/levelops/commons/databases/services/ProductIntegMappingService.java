package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.mappings.ProductIntegMapping;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.MapUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Log4j2
@Service
public class ProductIntegMappingService extends DatabaseService<ProductIntegMapping> {

    private final ObjectMapper objectMapper;

    @Autowired
    public ProductIntegMappingService(DataSource dataSource, ObjectMapper objectMapper) {
        super(dataSource);
        this.objectMapper = objectMapper;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class, ProductService.class);
    }

    @Override
    public String insert(String company, ProductIntegMapping mapping) throws SQLException {

        String SQL = "INSERT INTO " + company + ".productintegmappings(productid," +
                "integrationid,mappings) VALUES(?,?,to_json(?::json))";
        String retVal = null;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, Integer.parseInt(mapping.getProductId()));
            pstmt.setInt(2, Integer.parseInt(mapping.getIntegrationId()));
            pstmt.setString(3, StringUtils.defaultIfBlank(
                    objectMapper.writeValueAsString(MapUtils.emptyIfNull(mapping.getMappings())),
                    "{}"));
            pstmt.executeUpdate();
            // get the ID back
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    retVal = rs.getString(1);
                }
            }
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to convert mappings to string.", e);
        }
        return retVal;
    }

    @Override
    public Boolean update(String company, ProductIntegMapping stage) throws SQLException {
        String SQL = "UPDATE " + company + ".productintegmappings SET ";
        String updates = "";
        String condition = " WHERE id = ?";
        List<Object> values = new ArrayList<>();
        updates = "mappings = to_json(?::json)";
        if (stage.getMappings() != null) {
            values.add(stage.getMappings());
        } else {
            values.add(Map.of());
        }
        //we dont allow updates to integrationid and productid

        //nothing to update.
        if (values.size() == 0) {
            return false;
        }

        updates += StringUtils.isEmpty(updates) ? "updatedat = ?" : ", updatedat = ?";
        values.add(Instant.now().getEpochSecond());

        SQL = SQL + updates + condition;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 1; i <= values.size(); i++) {
                Object obj = values.get(i - 1);
                if (obj instanceof Map) {
                    pstmt.setObject(i, objectMapper.writeValueAsString(obj));
                    continue;
                }
                pstmt.setObject(i, values.get(i - 1));
            }
            pstmt.setInt(values.size() + 1,
                    Integer.parseInt(stage.getId()));
            pstmt.executeUpdate();
            return true;
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to convert mappings to string.", e);
        }
    }

    @Override
    public Optional<ProductIntegMapping> get(String company, String mappingId) {
        throw new UnsupportedOperationException();
    }

    public DbListResponse<ProductIntegMapping> listByFilter(String company, String productId, String integrationId,
                                                            Integer pageNumber, Integer pageSize)
            throws SQLException {
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(productId)) {
            criteria += "productid = ? ";
            values.add(Integer.parseInt(productId));
        }
        if (StringUtils.isNotEmpty(integrationId)) {
            criteria += (values.size() == 0) ? "integrationid = ? " : "AND integrationid = ? ";
            values.add(Integer.parseInt(integrationId));
        }
        if (values.size() == 0) {
            criteria = "";
        }
        String SQL = "SELECT id,integrationid,productid,mappings,updatedat," +
                "createdat FROM " + company + ".productintegmappings " + criteria
                + "LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        List<ProductIntegMapping> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(*) FROM ( SELECT id,integrationid,productid FROM "
                + company + ".productintegmappings " + criteria + ") AS d";
        Integer totCount = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {
            int i = 1;
            for (Object obj : values) {
                pstmt.setObject(i, obj);
                pstmt2.setObject(i++, obj);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                retval.add(ProductIntegMapping.builder()
                        .id(rs.getString("id"))
                        .integrationId(rs.getString("integrationid"))
                        .productId(rs.getString("productid"))
                        .mappings(ParsingUtils.parseJsonObject(objectMapper, "mappings", rs.getString("mappings")))
                        .createdAt(rs.getLong("createdat"))
                        .updatedAt(rs.getLong("updatedat"))
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
    public DbListResponse<ProductIntegMapping> list(String company, Integer pageNumber,
                                                    Integer pageSize) throws SQLException {
        return listByFilter(company, null, null, pageNumber, pageSize);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".productintegmappings WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setInt(1, Integer.parseInt(id));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        String SQL1 = "CREATE TABLE IF NOT EXISTS " + company + ".productintegmappings(\n" +
                "        id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, \n" +
                "        productid INTEGER NOT NULL REFERENCES "
                + company + ".products(id) ON DELETE CASCADE, \n" +
                "        integrationid INTEGER NOT NULL REFERENCES "
                + company + ".integrations(id) ON DELETE CASCADE, \n" +
                "        mappings JSONB, \n" +
                "        updatedat BIGINT DEFAULT extract(epoch from now()),\n" +
                "        createdat BIGINT DEFAULT extract(epoch from now())\n" +
                "    )";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt1 = conn.prepareStatement(SQL1)) {
            pstmt1.execute();
            return true;
        }

    }

}