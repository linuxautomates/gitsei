package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.models.DbListResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class TenantService extends DatabaseService<Tenant> {

    @Autowired
    public TenantService(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public boolean isTenantSpecific() {
        return false;
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.LEVELOPS_INVENTORY_SCHEMA;
    }

    @Override
    public String insert(String schema, Tenant tenant) throws SQLException {
        String SQL = "INSERT INTO " + LEVELOPS_INVENTORY_SCHEMA + ".tenants(id,tenantname) "
                + "VALUES(?,?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, tenant.getId());
            pstmt.setString(2, tenant.getTenantName());

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
        return null;
    }

    @Override
    public Boolean update(String schema, Tenant tenant) throws SQLException {
        String SQL = "UPDATE " + LEVELOPS_INVENTORY_SCHEMA + ".tenants SET ";
        String updates = "";
        String condition = " WHERE id = ?";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(tenant.getTenantName())) {
            updates = "tenantname = ?";
            values.add(tenant.getTenantName());
        }
        SQL = SQL + updates + condition;
        //no updates
        if (values.size() == 0) {
            return false;
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.NO_GENERATED_KEYS)) {
            for (int i = 1; i <= values.size(); i++) {
                pstmt.setObject(i, values.get(i - 1));
            }
            pstmt.setString(values.size() + 1, tenant.getId());
            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            return affectedRows > 0;
        }
    }

    @Override
    public Optional<Tenant> get(String schema, String tenantId) {

        String SQL = "SELECT id,tenantname,createdat FROM " + LEVELOPS_INVENTORY_SCHEMA + ".tenants WHERE id = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setString(1, tenantId);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(Tenant.builder()
                        .id(rs.getString("id"))
                        .tenantName(rs.getString("tenantname"))
                        .createdAt(rs.getLong("createdat"))
                        .build());
            }
        } catch (SQLException ex) {
            log.error(ex);
        }
        return Optional.empty();
    }

    @Override
    public DbListResponse<Tenant> list(String schema, Integer pageNumber, Integer pageSize)
            throws SQLException {
        String SQL = "SELECT id,tenantname,createdat FROM " + LEVELOPS_INVENTORY_SCHEMA + ".tenants LIMIT "
                + pageSize + " OFFSET " + (pageNumber * pageSize);
        List<Tenant> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(id) FROM " + LEVELOPS_INVENTORY_SCHEMA + ".tenants";
        Integer totCount = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                retval.add(Tenant.builder()
                        .id(rs.getString("id"))
                        .tenantName(rs.getString("tenantname"))
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
    public Boolean delete(String schema, String id) throws SQLException {
        String SQL = "DELETE FROM " + LEVELOPS_INVENTORY_SCHEMA + ".tenants WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setString(1, id);
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean ensureTableExistence(String schema) throws SQLException {
        ensureSchemaExistence(LEVELOPS_INVENTORY_SCHEMA);

        String SQL = "CREATE TABLE IF NOT EXISTS " + LEVELOPS_INVENTORY_SCHEMA + ".tenants(\n" +
                "        id VARCHAR(50) PRIMARY KEY, \n" +
                "        tenantname VARCHAR(100) NOT NULL, \n" +
                "        createdat BIGINT DEFAULT extract(epoch from now())\n" +
                "  )";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.execute();
            return true;
        }
    }

}