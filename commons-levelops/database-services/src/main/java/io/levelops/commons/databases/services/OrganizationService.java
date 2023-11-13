package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Organization;
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
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class OrganizationService extends DatabaseService<Organization> {

    @Autowired
    public OrganizationService(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public String insert(String company, Organization organization) throws SQLException {

        String SQL = "INSERT INTO " + company + ".organizations(name,contact) VALUES(?,?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, organization.getName());
            if (StringUtils.isEmpty(organization.getContact())) {
                pstmt.setNull(2, Types.VARCHAR);
            } else {
                pstmt.setString(2, organization.getContact());
            }

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
    public Boolean update(String company, Organization organization) throws SQLException {
        String SQL = "UPDATE " + company + ".organizations SET ";
        String updates = "";
        String condition = " WHERE id = ?";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(organization.getName())) {
            updates = "name = ?";
            values.add(organization.getName());
        }
        if (StringUtils.isNotEmpty(organization.getContact())) {
            updates = StringUtils.isEmpty(updates) ? "contact = ?" : updates + ", contact = ?";
            values.add(organization.getContact());
        }
        SQL = SQL + updates + condition;
        //no updates
        if (values.size() == 0) {
            return false;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 1; i <= values.size(); i++) {
                pstmt.setObject(i, values.get(i - 1));
            }
            pstmt.setInt(values.size() + 1, Integer.parseInt(organization.getId()));
            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            return affectedRows > 0;
        }
    }

    @Override
    public Optional<Organization> get(String company, String organizationId) {
        String SQL = "SELECT id,name,contact FROM " + company + ".organizations WHERE id = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setInt(1, Integer.parseInt(organizationId));

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(Organization.builder()
                        .id(rs.getString("id"))
                        .name(rs.getString("name"))
                        .contact(rs.getString("contact"))
                        .build());
            }
        } catch (SQLException ex) {
            log.error(ex);
        }
        return Optional.empty();
    }

    public DbListResponse<Organization> listByFilter(String company, String name, Integer pageNumber,
                                                     Integer pageSize) throws SQLException {
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(name)) {
            criteria += "org.name ILIKE ?";
            values.add(name + "%");
        }
        if (values.size() == 0) {
            criteria = " ";
        }
        String SQL = "SELECT org.id,org.name,org.contact,(SELECT COUNT(t.Id) FROM "
                + company + ".teams t WHERE org.id = t.organizationid) as tcount FROM "
                + company + ".organizations org " + criteria + "ORDER BY tcount DESC LIMIT "
                + pageSize + " OFFSET " + (pageNumber * pageSize);
        List<Organization> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(*) FROM " + company + ".organizations org" + criteria;
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
                retval.add(Organization.builder()
                        .id(rs.getString("id"))
                        .name(rs.getString("name"))
                        .contact(rs.getString("contact"))
                        .teamsCount(rs.getInt("tcount"))
                        .build());
            }
            if (retval.size() > 0) {
                totCount = retval.size() + pageNumber*pageSize; // if its last page or total count is less than pageSize
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
    public DbListResponse<Organization> list(String company, Integer pageNumber,
                                             Integer pageSize) throws SQLException {
        return listByFilter(company, null, pageNumber, pageSize);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".organizations WHERE id = ?";

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
        String SQL = "CREATE TABLE IF NOT EXISTS " + company + ".organizations(\n" +
                "        id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, \n" +
                "        name VARCHAR(50) NOT NULL, \n" +
                "        contact VARCHAR(50), \n" +
                "        createdat BIGINT DEFAULT extract(epoch from now())\n" +
                "    )";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.execute();
            return true;
        }
    }

}