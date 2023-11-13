package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import io.levelops.commons.databases.converters.UserConverters;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.utils.DatabaseUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
public class AdminUserService extends DatabaseService<User> {
    private static final String UPDATE_SCOPES_SQL_FORMAT = "UPDATE %s.users SET scopes=:scopes::json, updatedat=extract(epoch from now()) where id=:id";
    private static final String _LEVELOPS = "_levelops";

    private final ObjectMapper mapper;
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public AdminUserService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        this.mapper = mapper;
        this.template = new NamedParameterJdbcTemplate(dataSource);
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
    public String insert(String company, User user) throws SQLException {
        String SQL = "INSERT INTO " + company + ".users(firstname,lastname,bcryptpassword," +
                "email,usertype,passwordauthenabled,samlauthenabled,passwordreset, mfa_enabled" + (user.getMfaEnrollmentEndAt() != null ? ", mfa_enrollment_end" : "") + ", scopes )" +
                " VALUES(?,?,?,?,?,?,?,to_json(?::json), ?" + (user.getMfaEnrollmentEndAt() != null ? ", ?" : "") + ", (to_json(?::json)))";

        String scopes = "{}";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getFirstName());
            pstmt.setString(2, (user.getLastName() != null) ? user.getLastName() : "");
            pstmt.setBytes(3, user.getBcryptPassword().getBytes());
            pstmt.setString(4, user.getEmail().toLowerCase());
            pstmt.setString(5, user.getUserType().toString());
            pstmt.setBoolean(6, user.getPasswordAuthEnabled());
            pstmt.setBoolean(7, user.getSamlAuthEnabled());
            pstmt.setString(8, mapper.writeValueAsString(user.getPasswordResetDetails()));
            pstmt.setBoolean(9, user.getMfaEnabled());
            int nextIndex = 10;
            if (user.getMfaEnrollmentEndAt() != null) {
                pstmt.setLong(nextIndex++, user.getMfaEnrollmentEndAt().getEpochSecond());
            }
            pstmt.setString(nextIndex, scopes);

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
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to write passwordreset as string", e);
        }
        return null;
    }

    @Override
    public Boolean update(String company, User user) throws SQLException {
        String SQL = "UPDATE " + company + ".users SET ";
        // String updates = "";
        String condition = " WHERE id = ?";
        List<String> updates = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(user.getFirstName())) {
            updates.add("firstname = ?");
            values.add(user.getFirstName());
        }
        if (StringUtils.isNotEmpty(user.getLastName())) {
            updates.add("lastname = ?");
            values.add(user.getLastName());
        }
        if (StringUtils.isNotEmpty(user.getBcryptPassword())) {
            updates.add("bcryptpassword = ?");
            values.add(user.getBcryptPassword().getBytes());
        }
        if (user.getUserType() != null) {
            updates.add("usertype = ?");
            values.add(user.getUserType().toString());
        }
        if (user.getPasswordAuthEnabled() != null) {
            updates.add("passwordauthenabled = ?");
            values.add(user.getPasswordAuthEnabled());
        }
        if (user.getSamlAuthEnabled() != null) {
            updates.add("samlauthenabled = ?");
            values.add(user.getSamlAuthEnabled());
        }
        if (user.getPasswordResetDetails() != null) {
            updates.add("passwordreset = to_json(?::json)");
            values.add(user.getPasswordResetDetails());
        }
        if (user.getMfaEnabled() != null) {
            updates.add("mfa_enabled = ?");
            values.add(user.getMfaEnabled());
        }
        if (user.getMfaEnrollmentEndAt() != null) {
            updates.add("mfa_enrollment_end = ?");
            values.add(user.getMfaEnrollmentEndAt().getEpochSecond());
        }
        if (user.getMfaResetAt() != null) {
            updates.add("mfa_reset_at = ?");
            values.add(user.getMfaResetAt().getEpochSecond());
        }
        //no updates
        if (values.size() == 0) {
            return false;
        }

        updates.add("updatedat = ?");
        values.add(Instant.now().getEpochSecond());

        SQL = SQL + String.join(", ", updates) + " " + condition;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.NO_GENERATED_KEYS)) {
            for (int i = 1; i <= values.size(); i++) {
                Object obj = values.get(i - 1);
                if (obj instanceof User.PasswordReset) {
                    obj = mapper.writeValueAsString(obj);
                }
                pstmt.setObject(i, obj);
            }
            pstmt.setInt(values.size() + 1, Integer.parseInt(user.getId()));
            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            return affectedRows > 0;
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to convert passwordreset to string.", e);
        }
    }

    public Boolean updateScopes(final String company, final String userId, Map<String, List<String>> scopes) throws SQLException {
        Validate.notBlank(company, "Company cannot be null or empty!");
        Validate.notBlank(userId, "User Id cannot be null or empty!");
        Validate.notNull(scopes, "Scopes cannot be null!");

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", Integer.parseInt(userId));
        try {
            params.addValue("scopes", mapper.writeValueAsString(scopes));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize scopes to JSON", e);
        }

        String updateScopesSql = String.format(UPDATE_SCOPES_SQL_FORMAT, company);

        log.info("sql = " + updateScopesSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        int updatedRows = template.update(updateScopesSql, params);
        if (updatedRows != 1) {
            throw new SQLException("Failed to update user scopes!! " + userId);
        }
        return true;
    }

    public Boolean multiUpdatePasswordOrSaml(String company,
                                             RoleType roleCriteria,
                                             Boolean setPasswordEnabled,
                                             Boolean setSamlEnabled) throws SQLException {
        String SQL = "UPDATE " + company + ".users SET ";
        String updates = "";
        String condition = "";
        List<Object> values = new ArrayList<>();
        if (roleCriteria != null) {
            condition = " WHERE usertype = '" + roleCriteria.toString() + "'";
        }
        if (setPasswordEnabled != null) {
            updates = "passwordauthenabled = ?";
            values.add(setPasswordEnabled);
        }
        if (setSamlEnabled != null) {
            updates = StringUtils.isEmpty(updates) ? "samlauthenabled = ?" : updates + ", samlauthenabled = ?";
            values.add(setSamlEnabled);
        }
        //no updates
        if (values.size() == 0) {
            return false;
        }

        updates += StringUtils.isEmpty(updates) ? "updatedat = ?" : ", updatedat = ?";
        values.add(Instant.now().getEpochSecond());
        SQL = SQL + updates + condition;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.NO_GENERATED_KEYS)) {
            for (int i = 1; i <= values.size(); i++) {
                Object obj = values.get(i - 1);
                pstmt.setObject(i, obj);
            }
            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            return affectedRows > 0;
        }
    }

    @Override
    public Optional<User> get(String company, String userId) throws SQLException {

        String SQL = "SELECT * FROM " + company + ".users WHERE id = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setInt(1, Integer.parseInt(userId));

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(UserConverters.resultSetMapper(mapper, rs));
            }
        }
        return Optional.empty();
    }

    public DbListResponse<User> listByFilters(String company, List<String> userIds, String prefix, RoleType roleType,
                                              Integer pageNumber, Integer pageSize) throws SQLException {
        return listByFilters(company, userIds, prefix, roleType, null, null, pageNumber, pageSize);
    }

    public DbListResponse<User> listByFilters(String company, List<String> userIds, String prefix, RoleType roleType,
                                              Long updatedAtStart, Long updatedAtEnd, Integer pageNumber, Integer pageSize) throws SQLException {
        pageNumber = MoreObjects.firstNonNull(pageNumber, 0);
        pageSize = MoreObjects.firstNonNull(pageSize, 25);
        List<String> criteria = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(prefix)) {
            criteria.add("email LIKE ?");
            values.add(prefix.toLowerCase() + "%");
        }
        if (roleType != null) {
            criteria.add("usertype = ?");
            values.add(roleType.toString());
        }
        if (!CollectionUtils.isEmpty(userIds)) {
            criteria.add("id = ANY(?::int[])");
            values.add(DatabaseUtils.toSqlArray(userIds));
        }
        if (updatedAtStart != null) {
            criteria.add("updatedat > ?");
            values.add(updatedAtStart);
        }
        if (updatedAtEnd != null) {
            criteria.add("updatedat < ?");
            values.add(updatedAtEnd);
        }
        String where = criteria.isEmpty() ? "" : " WHERE " + String.join(" AND ", criteria) + " ";
        String SQL = "SELECT * " +
                " FROM " + company + ".users "
                + where +
                " ORDER BY updatedat DESC" +
                " LIMIT " + pageSize +
                " OFFSET " + (pageNumber * pageSize);
        List<User> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(id) FROM " + company + ".users" + where;
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
                retval.add(UserConverters.resultSetMapper(mapper, rs));
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
    public DbListResponse<User> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return listByFilters(company, null, null, null, pageNumber, pageSize);
    }

    public Optional<User> getByEmail(String company, String email) throws SQLException {
        String SQL = "SELECT * FROM " + company + ".users WHERE email = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setString(1, email.toLowerCase());

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(UserConverters.resultSetMapper(mapper, rs));
            }
        }
        return Optional.empty();
    }

    public Optional<User> getForAuthOnly(String company, String email) throws SQLException {
        String SQL = "SELECT * FROM " + company + ".users " +
                "WHERE email = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setString(1, email.toLowerCase());

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(UserConverters.resultSetMapper(mapper, rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".users WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setLong(1, Integer.parseInt(id));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public Boolean isMultiTenantUser(String email, List<String> tenantList) throws SQLException {
        List<User> userList = getUserDetailsAcrossTenants(email, tenantList);

        if (CollectionUtils.isEmpty(userList))
            return null;

        return userList.size() > 1 ? true : false;
    }

    public boolean isValidCompanyUser(String company, String email) throws SQLException {
        Optional<User> user = getByEmail(company, email);
        return user.isPresent();
    }

    public List<User> getUserDetailsAcrossTenants(String email, List<String> tenantList) throws SQLException {
        Map<String, Object> params = Maps.newHashMap();
        params.put("email", email);
        String sql = "SELECT id,firstname,lastname,bcryptpassword,email,usertype,passwordauthenabled,samlauthenabled,passwordreset,mfa_enabled,mfa_reset_at,createdat,updatedat,mfa_enrollment_end,scopes " +
                " FROM {0}.users WHERE email IN (:email)";
        String query = String.join(" \nUNION ALL\n ", tenantList.stream().map(tenant -> MessageFormat.format(sql, tenant)).collect(Collectors.toList()));
        log.info("sql {}", query);
        log.info("params {}", params);
        return template.query(query, params, UserConverters.rowMapper(DefaultObjectMapper.get()));
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of(
                "CREATE TABLE IF NOT EXISTS " + _LEVELOPS + ".users(\n" +
                        "        id                    INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, \n" +
                        "        email                 VARCHAR(50) NOT NULL UNIQUE, \n" +
                        "        bcryptpassword        BYTEA NOT NULL, \n" +
                        "        firstname             VARCHAR(50) NOT NULL, \n" +
                        "        lastname              VARCHAR(50) NOT NULL, \n" +
                        "        passwordreset         JSONB NOT NULL,\n" +
                        "        usertype              VARCHAR NOT NULL, \n" +
                        "        samlauthenabled       BOOLEAN NOT NULL, \n" +
                        "        passwordauthenabled   BOOLEAN NOT NULL, \n" +
                        "        mfa_enabled           BOOLEAN DEFAULT false, \n" +
                        "        mfa_enrollment_end    BIGINT, \n" +
                        "        scopes                JSONB NOT NULL DEFAULT '{}'::JSONB, \n" +
                        "        mfa_reset_at          BIGINT, \n" +
                        "        metadata              JSONB NOT NULL DEFAULT '{}'::JSONB, \n" +
                        "        updatedat             BIGINT DEFAULT extract(epoch from now()),\n" +
                        "        createdat             BIGINT DEFAULT extract(epoch from now())\n" +
                        "    )",

                "CREATE INDEX IF NOT EXISTS users_updatedat_idx ON " + _LEVELOPS + ".users(updatedat)");

        ddl.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

}