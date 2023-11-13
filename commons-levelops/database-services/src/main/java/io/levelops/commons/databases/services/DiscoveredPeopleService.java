package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.DiscoveredPeople;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Not used right now
 */
@Log4j2
//@Service
public class DiscoveredPeopleService extends DatabaseService<DiscoveredPeople> {

//    @Autowired
    public DiscoveredPeopleService(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DiscoveredPeople person) throws SQLException {

        String SQL = "INSERT INTO " + company + ".discoveredpeople(name,email,cloudid," +
                "integrationid) VALUES(?,?,?,?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, person.getName());
            pstmt.setString(2, person.getEmail());
            pstmt.setString(3, person.getCloudId());
            pstmt.setString(4, person.getIntegrationId());

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

    /**
     * NOTE: this does not return id unless insert was successful.
     * in other words, in case of conflict, the id will NOT be returned.
     *
     * @param company
     * @param people
     * @return
     * @throws SQLException
     */
    public List<String> batchInsert(String company, List<DiscoveredPeople> people) throws SQLException {
        String SQL = "INSERT INTO " + company + ".discoveredpeople(name,email,cloudid," +
                "integrationid) VALUES(?,?,?,?) ON CONFLICT (integrationid,cloudid) DO NOTHING " +
                "RETURNING id";

        List<String> ids = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {

            int i = 0;
            for (DiscoveredPeople person : people) {
                pstmt.setString(1, person.getName());
                pstmt.setString(2, person.getEmail());
                pstmt.setString(3, person.getCloudId());
                pstmt.setString(4, person.getIntegrationId());
                pstmt.addBatch();
                pstmt.clearParameters();
                i++;
                if (i % 100 == 0) {
                    pstmt.executeBatch();
                    ResultSet rs = pstmt.getGeneratedKeys();
                    while (rs.next()) {
                        ids.add(rs.getString("id"));
                    }
                }

            }
            if (i % 100 != 0) {
                pstmt.executeBatch();
                ResultSet rs = pstmt.getGeneratedKeys();
                while (rs.next()) {
                    ids.add(rs.getString("id"));
                }
            }
        }
        return ids;
    }

    @Override
    public Boolean update(String company, DiscoveredPeople person) throws SQLException {
        String SQL = "UPDATE " + company + ".discoveredpeople SET ";
        String updates = "";
        String condition = " WHERE id = ?";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(person.getName())) {
            updates = "name = ?";
            values.add(person.getName());
        }
        if (StringUtils.isNotEmpty(person.getEmail())) {
            updates = StringUtils.isEmpty(updates) ? "email = ?" : updates + ", email = ?";
            values.add(person.getEmail());
        }
        if (StringUtils.isNotEmpty(person.getCloudId())) {
            updates = StringUtils.isEmpty(updates) ? "cloudid = ?" : updates + ", cloudid = ?";
            values.add(person.getCloudId());
        }
        if (StringUtils.isNotEmpty(person.getIntegrationId())) {
            updates = StringUtils.isEmpty(updates) ? "integrationid = ?" : updates + ", integrationid = ?";
            values.add(Integer.parseInt(person.getIntegrationId()));
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
            pstmt.setInt(values.size() + 1, Integer.parseInt(person.getId()));
            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            return affectedRows > 0;
        }
    }

    @Override
    public Optional<DiscoveredPeople> get(String company, String integrationId) {
        String SQL = "SELECT id,name,email,cloudid,integrationid,createdat FROM "
                + company + ".discoveredpeople WHERE id = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setInt(1, Integer.parseInt(integrationId));

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(DiscoveredPeople.builder()
                        .id(rs.getString("id"))
                        .name(rs.getString("name"))
                        .email(rs.getString("email"))
                        .cloudId(rs.getString("cloudid"))
                        .integrationId(rs.getString("integrationid"))
                        .createdAt(rs.getLong("createdat"))
                        .build());
            }
        } catch (SQLException ex) {
            log.error(ex);
        }
        return Optional.empty();
    }

    public DbListResponse<DiscoveredPeople> listByFilter(String company, String email, String integrationId,
                                                         String name, Integer pageNumber, Integer pageSize)
            throws SQLException {
        String criteria = " WHERE ";
        ArrayList<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(email)) {
            criteria += "email = ? ";
            values.add(email);
        }
        if (StringUtils.isNotEmpty(integrationId)) {
            criteria += (values.size() == 0) ? "integrationid = ? " : "AND integrationid = ? ";
            values.add(Integer.parseInt(integrationId));
        }
        if (StringUtils.isNotEmpty(name)) {
            criteria += (values.size() == 0) ? "name ILIKE ? " : "AND name ILIKE ? ";
            values.add(name + "%");
        }

        if (values.isEmpty()) {
            criteria = " ";
        }

        String SQL = "SELECT id,name,email,cloudid,integrationid,createdat FROM "
                + company + ".discoveredpeople" + criteria + "LIMIT " + pageSize
                + " OFFSET " + (pageNumber * pageSize);
        List<DiscoveredPeople> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(id) FROM " + company + ".discoveredpeople" + criteria;
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
                retval.add(DiscoveredPeople.builder()
                        .id(rs.getString("id"))
                        .name(rs.getString("name"))
                        .email(rs.getString("email"))
                        .cloudId(rs.getString("cloudid"))
                        .integrationId(rs.getString("integrationid"))
                        .createdAt(rs.getLong("createdat"))
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
    public DbListResponse<DiscoveredPeople> list(String company, Integer pageNumber,
                                                 Integer pageSize) throws SQLException {
        return listByFilter(company, null, null, null, pageNumber, pageSize);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".discoveredpeople WHERE id = ?";

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
        String SQL = "CREATE TABLE IF NOT EXISTS " + company + ".discoveredpeople(\n" +
                "        id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, \n" +
                "        name VARCHAR, \n" +
                "        email VARCHAR, \n" +
                "        cloudid VARCHAR NOT NULL, \n" +
                "        integrationid INTEGER REFERENCES "
                + company + ".integrations(id) ON DELETE CASCADE, \n" +
                "        createdat BIGINT DEFAULT extract(epoch from now())\n" +
                "    )";
        String sqlIndexCreation1 = "CREATE UNIQUE INDEX IF NOT EXISTS uniq_discoveredpeople_compound_idx on "
                + company + ".discoveredpeople (integrationid,cloudid)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement indexPstmt = conn.prepareStatement(sqlIndexCreation1)) {
            pstmt.execute();
            indexPstmt.execute();
            return true;
        }
    }

}