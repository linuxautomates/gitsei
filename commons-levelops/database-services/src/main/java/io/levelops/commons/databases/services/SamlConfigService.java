package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.SamlConfig;
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
public class SamlConfigService extends DatabaseService<SamlConfig> {

    @Autowired
    public SamlConfigService(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public String insert(String company, SamlConfig config) throws SQLException {
        String SQL = "INSERT INTO " + company + ".samlconfig(id,enabled,idpid,idpcert,idpssourl) "
                + "VALUES(?,?,?,?,?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, Integer.parseInt(config.getId()));
            pstmt.setBoolean(2, config.getEnabled());
            pstmt.setString(3, config.getIdpId());
            pstmt.setBytes(4, config.getIdpCert().getBytes());
            pstmt.setString(5, config.getIdpSsoUrl());

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
    public Boolean update(String company, SamlConfig config) throws SQLException {
        String SQL = "UPDATE " + company + ".samlconfig SET ";
        String updates = "";
        String condition = " WHERE id = ?";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(config.getIdpId())) {
            updates = "idpid = ?";
            values.add(config.getIdpId());
        }
        if (config.getEnabled() != null) {
            updates = StringUtils.isEmpty(updates) ? "enabled = ?" : updates + ", enabled = ?";
            values.add(config.getEnabled());
        }
        if (StringUtils.isNotEmpty(config.getIdpCert())) {
            updates = StringUtils.isEmpty(updates) ? "idpcert = ?" : updates + ", idpcert = ?";
            values.add(config.getIdpCert().getBytes());
        }
        if (StringUtils.isNotEmpty(config.getIdpSsoUrl())) {
            updates = StringUtils.isEmpty(updates) ? "idpssourl = ?" : updates + ", idpssourl = ?";
            values.add(config.getIdpSsoUrl());
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
            pstmt.setInt(values.size() + 1, Integer.parseInt(config.getId()));
            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            return affectedRows > 0;
        }
    }

    @Override
    public Optional<SamlConfig> get(String company, String configId) {
        String SQL = "SELECT id,idpid,idpssourl,idpcert,enabled,createdat FROM "
                + company + ".samlconfig WHERE id = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setInt(1, Integer.parseInt(configId));

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(SamlConfig.builder()
                        .enabled(rs.getBoolean("enabled"))
                        .idpId(rs.getString("idpid"))
                        .idpSsoUrl(rs.getString("idpssourl"))
                        .idpCert(new String(rs.getBytes("idpcert")))
                        .createdAt(rs.getLong("createdat"))
                        .id(rs.getString("id"))
                        .build());
            }
        } catch (SQLException ex) {
            log.error(ex);
        }
        return Optional.empty();
    }

    @Override
    public DbListResponse<SamlConfig> list(String company, Integer pageNumber, Integer pageSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".samlconfig WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setLong(1, Integer.parseInt(id));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        //ID is integer because we dont want autoincrement in this scenario
        String SQL = "CREATE TABLE IF NOT EXISTS " + company + ".samlconfig(\n" +
                "        id INTEGER PRIMARY KEY, \n" +
                "        idpid VARCHAR NOT NULL UNIQUE, \n" +
                "        idpcert BYTEA NOT NULL, \n" +
                "        idpssourl VARCHAR NOT NULL, \n" +
                "        enabled BOOLEAN NOT NULL, \n" +
                "        createdat BIGINT DEFAULT extract(epoch from now())\n" +
                "    )";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.execute();
            return true;
        }
    }

}