package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.AccessKey;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.models.DbListResponse;
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
import java.util.UUID;

@Service
public class AccessKeyService extends DatabaseService<AccessKey> {

    @Autowired
    public AccessKeyService(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public String insert(String company, AccessKey accessKey) throws SQLException {

        String SQL = "INSERT INTO " + company + ".accesskeys(name,description,bcryptsecret," +
                "roletype) VALUES(?,?,?,?)";

        String retVal = null;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, accessKey.getName());
            pstmt.setString(2, accessKey.getDescription());
            pstmt.setBytes(3, accessKey.getBcryptSecret().getBytes());
            pstmt.setString(4, accessKey.getRoleType().toString());

            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            if (affectedRows > 0) {
                // get the ID back
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        retVal = rs.getString(1);
                    }
                }
            }
        }
        return retVal;
    }

    @Override
    public Boolean update(String company, AccessKey accessKey) {
        throw new UnsupportedOperationException("AccessKeys cannot be updated.");
    }

    @Override
    public Optional<AccessKey> get(String company, String accessKeyId) throws SQLException {
        String SQL = "SELECT id,name,description,roletype,createdat FROM "
                + company + ".accesskeys WHERE id = ?::uuid";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setObject(1, UUID.fromString(accessKeyId));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(AccessKey.builder()
                        .id(rs.getString("id"))
                        .name(rs.getString("name"))
                        .description(rs.getString("description"))
                        .roleType(RoleType.fromString(rs.getString("roletype")))
                        .createdAt(rs.getLong("createdat"))
                        .build());
            }
        }
        return Optional.empty();
    }

    public Optional<AccessKey> getForAuthOnly(String company, String accessKeyId) throws SQLException {
        String SQL = "SELECT id,name,description,bcryptsecret,roletype,createdat FROM "
                + company + ".accesskeys WHERE id = ?::uuid";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setObject(1, UUID.fromString(accessKeyId));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(AccessKey.builder()
                        .id(rs.getString("id"))
                        .name(rs.getString("name"))
                        .bcryptSecret(new String(rs.getBytes("bcryptsecret")))
                        .description(rs.getString("description"))
                        .roleType(RoleType.fromString(rs.getString("roletype")))
                        .createdAt(rs.getLong("createdat"))
                        .build());
            }
        }
        return Optional.empty();
    }

    @Override
    public DbListResponse<AccessKey> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return list(company, null, null, pageNumber, pageSize);
    }

    public DbListResponse<AccessKey> list(String company, String name, String role, Integer pageNumber,
                                          Integer pageSize) throws SQLException {
        String criteria = " WHERE";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(name)) {
            criteria += " name ILIKE ?";
            values.add(name + "%");
        }
        if (StringUtils.isNotEmpty(role)) {
            criteria += (values.size() == 0) ? " roletype LIKE ?" : " AND roletype LIKE ?";
            values.add(role.toUpperCase() + "%");
        }
        if (values.size() == 0)
            criteria = "";
        String SQL = "SELECT id,name,description,roletype,createdat FROM " + company + ".accesskeys"
                + criteria + " ORDER BY createdat DESC LIMIT " + pageSize + " OFFSET "
                + (pageNumber * pageSize);
        String countSQL = "SELECT COUNT(*) FROM " + company + ".accesskeys" + criteria;
        List<AccessKey> retval = new ArrayList<>();
        Integer totCount = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {
            for (int i = 1; i <= values.size(); i++) {
                pstmt.setObject(i, values.get(i - 1));
                pstmt2.setObject(i, values.get(i - 1));
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                retval.add(AccessKey.builder()
                        .id(rs.getString("id"))
                        .name(rs.getString("name"))
                        .description(rs.getString("description"))
                        .roleType(RoleType.fromString(rs.getString("roletype")))
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
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".accesskeys WHERE id = ?::uuid";

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
        String sql = "CREATE TABLE IF NOT EXISTS " + company + ".accesskeys(\n" +
                "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    bcryptsecret BYTEA NOT NULL,\n" +
                "    name VARCHAR NOT NULL,\n" +
                "    description VARCHAR,\n" +
                "    roletype VARCHAR NOT NULL,\n" +
                "    createdat BIGINT DEFAULT extract(epoch from now())\n" +
                ")";
        String sqlIndex1Creation = "CREATE INDEX IF NOT EXISTS accesskeys_name_idx on "
                + company + ".accesskeys(name)";
        String sqlIndex2Creation = "CREATE INDEX IF NOT EXISTS accesskeys_role_idx on "
                + company + ".accesskeys(roletype)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             PreparedStatement index1Pstmt = conn.prepareStatement(sqlIndex1Creation);
             PreparedStatement index2Pstmt = conn.prepareStatement(sqlIndex2Creation)) {
            pstmt.execute();
            index1Pstmt.execute();
            index2Pstmt.execute();
            return true;
        }
    }
}
