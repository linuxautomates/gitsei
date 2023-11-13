package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import io.levelops.commons.databases.converters.UserConverters;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.databases.utils.DatabaseUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
public class UserService extends DatabaseService<User> {
    private static final List<String> CUSTOMER_SUCCESS_EMAIL = List.of("customersuccess@levelops.io", "customersuccess@propelo.ai", "sei-cs@harness.io");

    private static final String UPDATE_SCOPES_SQL_FORMAT = "UPDATE %s.users SET scopes=:scopes::json, updatedat=extract(epoch from now()) where id=:id";
    private static final String DELETE_MANAGED_OUS_BY_USER_ID_SQL_FORMAT = "DELETE FROM %s.user_managed_ous WHERE user_id = :user_id and ou_ref_id IN (:ou_ref_ids)";
    private static final String INSERT_USER_MANAGED_OUS_SQL_FORMAT = "INSERT INTO %s.user_managed_ous (ou_ref_id,user_id) "
            + "VALUES %s "
            + "ON CONFLICT(ou_ref_id,user_id) DO NOTHING";
    private static final String _LEVELOPS = "_levelops";
    private static final String SELECT_USER_CLAUSE = "SELECT array_remove(array_agg(ou_ref_id), NULL)::integer[] as ou_ref_ids,u.id as user_id, lastname, firstname, email," +
            "usertype, samlauthenabled, passwordauthenabled, mfa_enabled, mfa_enrollment_end, mfa_reset_at," +
            "bcryptpassword, passwordreset, scopes, metadata, createdat, updatedat";
    private static final String SELECT_USER_CLAUSE_WITHOUT_OUS = "SELECT u.id as user_id, lastname, firstname, email," +
            "usertype, samlauthenabled, passwordauthenabled, mfa_enabled, mfa_enrollment_end, mfa_reset_at," +
            "bcryptpassword, passwordreset, scopes, metadata, createdat, updatedat";
    private static final String USER_GROUP_BY_CLAUSE = " GROUP BY u.id";

    private final ObjectMapper mapper;
    private final NamedParameterJdbcTemplate template;
    private final PlatformTransactionManager transactionManager;

    @Autowired
    public UserService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        this.mapper = mapper;
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.transactionManager = new DataSourceTransactionManager(dataSource);
    }

    @Override
    public String insert(String company, User user) throws SQLException {
        String userId = null;
        String SQL = "INSERT INTO " + company + ".users(firstname,lastname,bcryptpassword," +
                "email,usertype,passwordauthenabled,samlauthenabled,passwordreset, mfa_enabled" + (user.getMfaEnrollmentEndAt() != null ? ", mfa_enrollment_end" : "") + ", scopes, metadata )" +
                " VALUES(?,?,?,?,?,?,?,to_json(?::json), ?" + (user.getMfaEnrollmentEndAt() != null ? ", ?" : "") + ", (to_json(?::json)), to_json(?::json))";

        String scopes = "{}";
        if (CUSTOMER_SUCCESS_EMAIL.contains(user.getEmail()))
            scopes = "{\"dev_productivity_write\": null}";

        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);

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
            pstmt.setObject(9, user.getMfaEnabled(), Types.BOOLEAN);

            int nextIndex = 10;
            if (user.getMfaEnrollmentEndAt() != null) {
                pstmt.setLong(nextIndex++, user.getMfaEnrollmentEndAt().getEpochSecond());
            }
            pstmt.setString(nextIndex++, scopes);
            pstmt.setString(nextIndex, mapper.writeValueAsString(
                    MoreObjects.firstNonNull(user.getMetadata(), Map.of())));

            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            if (affectedRows > 0) {
                // get the ID back
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        userId =  rs.getString(1);
                    }
                }
            }
            if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(user.getManagedOURefIds())) {
                insertUserManagedOUsBulk(company, Integer.valueOf(userId), user.getManagedOURefIds());
            }
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw new SQLException("Failed to write passwordreset as string", e);
        }
        return userId;
    }

    private int insertUserManagedOUsBulk(String company, Integer userId, List<Integer> ouRefIds) {
        Map<String, Object> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        for(Integer ouRefId : ouRefIds){
            int i = values.size();
            params.putAll(Map.of(
                    "ou_ref_id_" + i, ouRefId,
                    "dev_productivity_profile_id_" + i, userId
            ));
            values.add(MessageFormat.format("(:ou_ref_id_{0}, :dev_productivity_profile_id_{0})", i));
        }
        if (values.isEmpty()) {
            log.warn("Skipping bulk insert: no valid data provided");
            return 0;
        }
        String insertUsermanagedOUsSQL = String.format(INSERT_USER_MANAGED_OUS_SQL_FORMAT, company, String.join(", ",values));
        return template.update(insertUsermanagedOUsSQL, new MapSqlParameterSource(params));
    }

    public Boolean update(String company, User user, Boolean updateForAuthOnly) throws SQLException {
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
        if (user.getMetadata() != null) {
            updates.add("metadata = to_json(?::json)");
            try {
                values.add(mapper.writeValueAsString(user.getMetadata()));
            } catch (JsonProcessingException e) {
                values.add("'{}'");
                log.error("failed to convert query into string.");
            }
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

        TransactionStatus txStatus = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.NO_GENERATED_KEYS)) {
            for (int i = 1; i <= values.size(); i++) {
                Object obj = values.get(i - 1);
                if (obj instanceof User.PasswordReset) {
                    obj = mapper.writeValueAsString(obj);
                }
                pstmt.setObject(i, obj);
            }
            String userId = user.getId();
            pstmt.setInt(values.size() + 1, Integer.parseInt(userId));
            int affectedRows = pstmt.executeUpdate();
            if(BooleanUtils.isNotTrue(updateForAuthOnly)){
                updateUserManagedOus(company,userId,user.getManagedOURefIds());
            }
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw new SQLException("Failed to update user", e);
        }
        return true;
    }
    @Override
    public Boolean update(String company, User user) throws SQLException {
        return update(company, user, null);
    }

    public Boolean updateUserManagedOus(final String company, final String userId, List<Integer> managedOURefIds) {
        List<Integer> existingOuRefIds = getManagedOusByUserId(company, userId);
        if(ListUtils.isEqualList(existingOuRefIds,managedOURefIds))
            return true;
        List<Integer> mappingsToBeDeleted = (List<Integer>) CollectionUtils.removeAll(existingOuRefIds, managedOURefIds);
        List<Integer> mappingsToBeInserted = (List<Integer>) CollectionUtils.removeAll(managedOURefIds, existingOuRefIds);

        if(CollectionUtils.isNotEmpty(mappingsToBeDeleted)){
            deleteUserManagedOUsByUserId(company,Integer.parseInt(userId), mappingsToBeDeleted);
        }
        if(CollectionUtils.isNotEmpty(mappingsToBeInserted)){
            insertUserManagedOUsBulk(company,Integer.parseInt(userId),mappingsToBeInserted);
        }
        return true;
    }

    private static final String GET_MANAGED_OUS_BY_USER_ID_SQL_FORMAT = "SELECT ou_ref_id FROM %s.user_managed_ous WHERE user_id = :user_id";
    private List<Integer> getManagedOusByUserId(String company, String userId) {
        String getOusSql = String.format(GET_MANAGED_OUS_BY_USER_ID_SQL_FORMAT, company);
        return template.query(getOusSql, Map.of("user_id", Integer.parseInt(userId)), new RowMapper<Integer>() {
            @Override
            public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getInt("ou_ref_id");
            }
        });
    }

    private Boolean deleteUserManagedOUsByUserId(String company, Integer userId, List<Integer> managedOus) {
        String deleteSql = String.format(DELETE_MANAGED_OUS_BY_USER_ID_SQL_FORMAT, company);
        log.debug("Deleting ou-user mappings for company {} userId {} ",company,userId);
        return template.update(deleteSql, Map.of("user_id", userId, "ou_ref_ids", managedOus)) > 0;
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

        String SQL = SELECT_USER_CLAUSE +
                " FROM " + company + ".users u"+
                getManagedOUsJoinSQL(company) +
                " WHERE u.id = ? " +
                USER_GROUP_BY_CLAUSE +
                " LIMIT 1";

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
        return listByFilters(company, userIds, prefix, roleType, null, null,null, pageNumber, pageSize);
    }

    public DbListResponse<User> listByFilters(String company, List<String> userIds, String prefix, RoleType roleType,
                                              Long updatedAtStart, Long updatedAtEnd, List<String> managedOuRefIds, Integer pageNumber, Integer pageSize) throws SQLException {
        pageNumber = MoreObjects.firstNonNull(pageNumber, 0);
        pageSize = MoreObjects.firstNonNull(pageSize, 25);
        List<String> criteria = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(prefix)) {
            criteria.add("(email LIKE ? OR lower(firstname) LIKE ? OR lower(lastname) LIKE ?)");
            values.add("%" + prefix.toLowerCase() + "%");
            values.add("%" + prefix.toLowerCase() + "%");
            values.add("%" + prefix.toLowerCase() + "%");
        }
        if (roleType != null) {
            criteria.add("usertype = ?");
            values.add(roleType.toString());
        }
        if (!CollectionUtils.isEmpty(userIds)) {
            criteria.add("u.id = ANY(?::int[])");
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
        String ouRefIdCondition = "";
        if(CollectionUtils.isNotEmpty(managedOuRefIds)){
            ouRefIdCondition = " WHERE ou_ref_ids && ARRAY[?::int[]]";
            values.add(DatabaseUtils.toSqlArray(managedOuRefIds));
        }
        String where = criteria.isEmpty() ? "" : " WHERE " + String.join(" AND ", criteria) + " ";
        String SQL = SELECT_USER_CLAUSE +
                " FROM " + company + ".users u"+
                getManagedOUsJoinSQL(company) +
                where +
                USER_GROUP_BY_CLAUSE +
                " ORDER BY updatedat DESC";
        String limitCaluse =  " LIMIT " + pageSize +
                " OFFSET " + (pageNumber * pageSize);
        String selectSql = "WITH users as ("+SQL+")"+"\n"+
                "SELECT * FROM users"+ouRefIdCondition;
        String finalSQL = selectSql+limitCaluse;
        List<User> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(user_id) FROM (" + selectSql + ") AS count";
        Integer totCount = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(finalSQL);
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

    private String getManagedOUsJoinSQL(String company) {
        return " LEFT JOIN " + company + ".user_managed_ous umo ON u.id = umo.user_id ";
    }

    @Override
    public DbListResponse<User> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return listByFilters(company, null, null, null, pageNumber, pageSize);
    }

    public Optional<User> getByEmail(String company, String email) throws SQLException {
        String SQL = SELECT_USER_CLAUSE +
                " FROM " + company + ".users u"+
                getManagedOUsJoinSQL(company) +
                " WHERE email = ?" +
                USER_GROUP_BY_CLAUSE +
                " LIMIT 1";

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
        return getForAuthOnly(company, email, null);
    }

    public Optional<User> getForAuthOnly(String company, String email, Boolean tenantAdminOperation) throws SQLException {
        String SQL = (BooleanUtils.isTrue(tenantAdminOperation) ? SELECT_USER_CLAUSE_WITHOUT_OUS : SELECT_USER_CLAUSE) +
                " FROM " + company + ".users u"+
                (BooleanUtils.isTrue(tenantAdminOperation) ? "" : getManagedOUsJoinSQL(company)) +
                " WHERE email = ?" +
                USER_GROUP_BY_CLAUSE +
                " LIMIT 1";

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
        String sql = "SELECT id as user_id,firstname,lastname,bcryptpassword,email,usertype,passwordauthenabled,samlauthenabled,passwordreset,mfa_enabled,mfa_reset_at,createdat,updatedat,mfa_enrollment_end,scopes,metadata, ''{0}'' as company " +
                " FROM {0}.users WHERE email IN (:email)";
        String query = String.join(" \nUNION ALL\n ", tenantList.stream().map(tenant -> MessageFormat.format(sql, tenant)).collect(Collectors.toList()));
        log.info("sql {}", query);
        log.info("params {}", params);
        return template.query(query, params, UserConverters.rowMapper(DefaultObjectMapper.get()));
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".users(\n" +
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
                        "        metadata              JSONB NOT NULL DEFAULT '{}'::JSONB, \n" +
                        "        mfa_reset_at          BIGINT, \n" +
                        "        updatedat             BIGINT DEFAULT extract(epoch from now()),\n" +
                        "        createdat             BIGINT DEFAULT extract(epoch from now())\n" +
                        "    )",

                "CREATE INDEX IF NOT EXISTS users_updatedat_idx ON " + company + ".users(updatedat)",

                "CREATE TABLE IF NOT EXISTS " + company + ".user_managed_ous(\n" +
                        "  id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "  ou_ref_id        INTEGER NOT NULL," +
                        "  user_id INTEGER NOT NULL REFERENCES " + company + ".users(id) ON DELETE CASCADE," +
                        "  UNIQUE(ou_ref_id,user_id)" +
                        ")");

        ddl.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

}