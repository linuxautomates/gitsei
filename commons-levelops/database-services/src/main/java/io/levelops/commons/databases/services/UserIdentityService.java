package io.levelops.commons.databases.services;

import com.google.common.collect.Lists;
import io.levelops.commons.databases.models.database.Signature;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.database.scm.DbScmUser.MappingStatus;
import io.levelops.commons.databases.models.filters.UserIdentitiesFilter;
import io.levelops.commons.databases.utils.DatabaseUtils;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.databases.converters.UserIdentityConverters.userRowMapper;

@Log4j2
@Service
public class UserIdentityService extends DatabaseService<DbScmUser> {

    private static final int PAGE_SIZE = 50;
    public final static String USER_IDS_TABLE = "integration_users";
    public static final Set<String> USERS_PARTIAL_MATCH_COLUMNS = Set.of("display_name");
    private static final Set<String> USERS_SORTABLE_COLUMNS = Set.of("created_at", "updated_at", "display_name");
    private static final Set<String> USERS_STRING_SORTABLE_COLUMNS = Set.of("display_name");

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public UserIdentityService(final DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    /**
     * Inserts or updates a user. If the user already existed and there was no change, returns null!
     * Use {@link UserIdentityService#upsert(java.lang.String, io.levelops.commons.databases.models.database.scm.DbScmUser)} if you care about the id.
     */
    @Nullable
    private String insertInternal(String company, DbScmUser dbScmUser, boolean upsert, boolean ignoreEmails) throws SQLException {
        if (StringUtils.isEmpty(dbScmUser.getIntegrationId()) || StringUtils.isEmpty(dbScmUser.getCloudId())) {
            return null;
        }
        return template.getJdbcTemplate().execute(TransactionCallback.of(connection -> {
            String onConflictSetEmails = ignoreEmails ? "" : "emails,";
            String onConflictSetEmailsValues = ignoreEmails ? "" : "EXCLUDED.emails,";
            String insertEmails = ignoreEmails ? "" : "emails,";
            String insertEmailsValues = ignoreEmails ? "" : "?,";
            List<String> onConflictFields = Lists.newArrayList("u.original_display_name", "u.display_name");
            List<String> onConflictExcludedFields = Lists.newArrayList("EXCLUDED.original_display_name", "EXCLUDED.display_name");
            if (!ignoreEmails) {
                onConflictFields.add("u.emails");
                onConflictExcludedFields.add("EXCLUDED.emails");
            }
            String onConflictClause = " WHERE (" + String.join(", ", onConflictFields) + " ) IS DISTINCT FROM (" + String.join(", ", onConflictExcludedFields) + ")";

            String upsertOnConflict = String.format(" DO UPDATE SET (display_name,original_display_name,%s updated_at) = ", onConflictSetEmails) +
                    String.format(" (EXCLUDED.display_name,EXCLUDED.original_display_name,%s EXTRACT(epoch FROM now()))", onConflictSetEmailsValues) +
                    onConflictClause;
            String noUpsertOnConflict = " DO NOTHING";
            String insertStmt = " INSERT INTO " + company + ".integration_users AS u " +
                    String.format(" (integration_id, cloud_id, display_name,original_display_name, %s updated_at, mapping_status)", insertEmails) +
                    String.format(" VALUES (?, ?, ?, ?,%s EXTRACT(epoch FROM now()), ?::user_mapping_status_t) ", insertEmailsValues) +
                    " ON CONFLICT (integration_id, cloud_id) ";

            if (upsert) {
                insertStmt += upsertOnConflict;
            } else {
                insertStmt += noUpsertOnConflict;
            }
            insertStmt += " RETURNING id";

            try (PreparedStatement statement = connection.prepareStatement(insertStmt, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                Array emailArray = dbScmUser.getEmails() == null ? null : connection.createArrayOf("varchar", dbScmUser.getEmails().toArray());
                statement.setInt(i++, NumberUtils.toInt(dbScmUser.getIntegrationId()));
                statement.setString(i++, dbScmUser.getCloudId());
                statement.setString(i++, dbScmUser.getDisplayName());
                statement.setString(i++, dbScmUser.getOriginalDisplayName());
                if (!ignoreEmails) {
                    statement.setArray(i++, emailArray);
                }
                statement.setString(i++, dbScmUser.getMappingStatus() == null ? MappingStatus.AUTO.toString() : dbScmUser.getMappingStatus().toString());
                log.debug("Query: {}", statement.toString());
                int affectedRows = statement.executeUpdate();
                if (affectedRows > 0) {
                    // get the ID back
                    try (ResultSet rs = statement.getGeneratedKeys()) {
                        if (rs.next()) {
                            return rs.getString(1);
                        }
                    }
                }
            }
            return null;
        }));
    }

    /**
     * Inserts or updates a user. If the user already existed and there was no change, returns null!
     * Use {@link UserIdentityService#upsert(java.lang.String, io.levelops.commons.databases.models.database.scm.DbScmUser)} if you care about the id.
     */
    @Nullable
    @Override
    public String insert(String company, DbScmUser dbScmUser) throws SQLException {
        return insertInternal(company, dbScmUser, true, false);
    }

    /**
     * Inserts a user. If the user already existed, returns null!
     * <p>
     * Unfortunately the insert() method above also upserts by default, so we need
     * to name this method differently.
     * TODO: refactor the insert() method to not upsert by default, and change all references to it.
     */
    public String insertNoUpsert(String company, DbScmUser dbScmUser) throws SQLException {
        return insertInternal(company, dbScmUser, false, false);
    }

    /**
     * Inserts or update a user. Always returns an id when the user exists, otherwise throws an exception.
     */
    @Nonnull
    public String upsert(String company, DbScmUser dbScmUser) throws SQLException {
        String id = insert(company, dbScmUser);
        if (id != null) {
            return id;
        }
        // if the display name did not change, the insert will do nothing and won't return an id,
        // so we need to query for the user:
        id = getUser(company, dbScmUser.getIntegrationId(), dbScmUser.getCloudId());
        if (id != null) {
            return id;
        }
        throw new SQLException(String.format("Could not upsert user for company=%s, integrationId=%s, cloud_id=%s", company, dbScmUser.getIntegrationId(), dbScmUser.getCloudId()));
    }

    /**
     * Inserts or update a user, but completely ignores the email field. This is useful because
     * in a majority of the cases, the email field is not returned from the SCM API. If we don't
     * ignore it, we will always update the email field to null which is not what we want.
     * <p>
     * Always returns an id when the user exists, otherwise throws an exception.
     */
    @Nonnull
    public String upsertIgnoreEmail(String company, DbScmUser dbScmUser) throws SQLException {
        String id = insertInternal(company, dbScmUser, true, true);
        if (id != null) {
            return id;
        }
        // if the display name did not change, the insert will do nothing and won't return an id,
        // so we need to query for the user:
        id = getUser(company, dbScmUser.getIntegrationId(), dbScmUser.getCloudId());
        if (id != null) {
            return id;
        }
        throw new SQLException(String.format("Could not upsert user for company=%s, integrationId=%s, cloud_id=%s", company, dbScmUser.getIntegrationId(), dbScmUser.getCloudId()));
    }


    /**
     * Use batch upsert when you have many records to insert and don't care about ids being returned.
     */
    private void batchUpsertInternal(String company, List<DbScmUser> dbScmUsers, boolean ignoreEmails) throws SQLException {
        String insertStmt = " INSERT INTO " + company + ".integration_users AS u " +
                " (integration_id, cloud_id, display_name,original_display_name, " + (ignoreEmails ? "" : "emails, ") + " updated_at)" +
                " VALUES (?, ?, ?, ?, " + (ignoreEmails ? "" : "?, ") + " EXTRACT(epoch FROM now())) " +
                " ON CONFLICT (integration_id, cloud_id) " +
                " DO UPDATE SET (display_name,original_display_name," + (ignoreEmails ? "" : "emails, ") + "updated_at) = " +
                " (EXCLUDED.display_name,EXCLUDED.original_display_name," + (ignoreEmails ? "" : "EXCLUDED.emails, ") + " EXTRACT(epoch FROM now()))" +
                " WHERE u.original_display_name != EXCLUDED.original_display_name or u.display_name != EXCLUDED.display_name" +
                (ignoreEmails ? "" : " OR u.emails != EXCLUDED.emails ") +
                " RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement insertUser = conn.prepareStatement(insertStmt, Statement.RETURN_GENERATED_KEYS)) {
            for (DbScmUser dbScmUser : dbScmUsers) {
                Array emailArray = dbScmUser.getEmails() == null ? null : conn.createArrayOf("varchar", dbScmUser.getEmails().toArray());
                insertUser.setInt(1, Integer.parseInt(dbScmUser.getIntegrationId()));
                insertUser.setString(2, dbScmUser.getCloudId());
                insertUser.setString(3, dbScmUser.getDisplayName());
                insertUser.setString(4, (dbScmUser.getOriginalDisplayName()));
                if (!ignoreEmails) {
                    insertUser.setArray(5, emailArray);
                }
                insertUser.addBatch();
                insertUser.clearParameters();
                insertUser.executeBatch();
            }
        }
    }

    public void batchUpsert(String company, List<DbScmUser> dbScmUsers) throws SQLException {
        batchUpsertInternal(company, dbScmUsers, false);
    }

    public void batchUpdateMappingStatus(String company, Collection<DBOrgUser.LoginId> cloudIds, MappingStatus mappingStatus) {
        String updateStmt = " UPDATE " + company + ".integration_users AS u " +
                " SET mapping_status = ?::user_mapping_status_t " +
                " WHERE u.cloud_id = ? AND u.integration_id = ?" +
                " AND u.mapping_status != ?::user_mapping_status_t";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement updateMappingStatus = conn.prepareStatement(updateStmt)) {
            for (DBOrgUser.LoginId cloudId : cloudIds) {
                updateMappingStatus.setString(1, mappingStatus.toString());
                updateMappingStatus.setString(2, cloudId.getCloudId());
                updateMappingStatus.setInt(3, cloudId.getIntegrationId());
                updateMappingStatus.setString(4, mappingStatus.toString());
                updateMappingStatus.addBatch();
                updateMappingStatus.clearParameters();
                updateMappingStatus.executeBatch();
            }
        } catch (SQLException e) {
            log.error("Error updating mapping status for company {} cloudIds {}", company, cloudIds, e);
        }
    }


    /**
     * Use batch upsert when you have many records to insert and don't care about ids being returned.
     * <p>
     * This completely ignores the email field. This is useful because
     * in a majority of the cases, the email field is not returned from the SCM API. If we don't
     * ignore it, we will always update the email field to null which is not what we want.
     */
    public void batchUpsertIgnoreEmail(String company, List<DbScmUser> dbScmUsers) throws SQLException {
        batchUpsertInternal(company, dbScmUsers, true);
    }


    // Commented out since this is not the correct way to do this, this creates looped dependencies
    // private void insertTeamMemberUsernames(String company, DbScmUser dbScmUser, String insertedUserId) throws SQLException {
    //     log.info("Inserting team member for company {} user {} ", company, dbScmUser);
    //     try {
    //         teamMembersDatabaseService.upsert(company,
    //                 DBTeamMember.builder()
    //                         .fullName(dbScmUser.getDisplayName())
    //                         .build(), UUID.fromString(insertedUserId));
    //     } catch (SQLException e) {
    //         log.error("Error while inserting user...{0}" + e.getMessage(), e);
    //         throw e;
    //     }
    // }

    @Override
    public Boolean update(String company, DbScmUser t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DbScmUser> get(String company, String id) throws SQLException {
        String sql = "SELECT * FROM " + company + "." + USER_IDS_TABLE +
                " WHERE id = :id::uuid " +
                " LIMIT 1 ";
        try {
            List<DbScmUser> results = template.query(sql, Map.of("id", id),
                    userRowMapper());
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get user metadata for id={}", id, e);
            return Optional.empty();
        }
    }

    @Override
    public DbListResponse<DbScmUser> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return list(company, UserIdentitiesFilter.builder().build(), pageNumber, pageSize);
    }

    /**
     * Fetches the Users from the DB based on userIds
     *
     * @return List of DbScmUser
     */
    public DbListResponse<DbScmUser> list(String company, List<String> userIds) {
        Validate.notEmpty(userIds, "user Ids should not be empty");

        Map<String, Object> params = new HashMap<>();
        String usersWhere = " WHERE id::text in ( :user_ids )";
        params.put("user_ids", userIds);
        String sql = "SELECT * FROM " + company + "." + USER_IDS_TABLE + usersWhere;
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<DbScmUser> results = template.query(sql, params, userRowMapper());
        return DbListResponse.of(results, results.size());
    }

    /**
     * It fetches the User by its displayNames and integrationIds from the DB
     *
     * @return List of DbScmUser
     */
    public DbListResponse<DbScmUser> listUserByDisplayNames(String
                                                                    company, List<String> integrationIds, List<String> displayNames) {
        Validate.notEmpty(integrationIds, "Integration Ids cannot be empty");
        Validate.notEmpty(displayNames, "Display names cannot be empty");

        String sql = "SELECT * FROM " + company + "." + USER_IDS_TABLE
                + " WHERE display_name IN (:display_names) AND integration_id::text IN (:integration_ids)";
        Map<String, Object> params = Map.of("display_names", displayNames,
                "integration_ids", integrationIds);
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<DbScmUser> data = template.query(sql, params, userRowMapper());
        return DbListResponse.of(data, data.size());
    }

    public Stream<DbScmUser> stream(String company, UserIdentitiesFilter filter) {
        return PaginationUtils.stream(0, 1, RuntimeStreamException.wrap(page -> list(company, filter, page, PAGE_SIZE).getRecords()));
    }

    public DbListResponse<DbScmUser> list(String company, UserIdentitiesFilter filter,
                                          Integer pageNumber, Integer pageSize) throws SQLException {
        Map<String, Object> params = new HashMap<>();
        String usersWhere = "";
        Map<String, SortingOrder> sortBy = MapUtils.emptyIfNull(filter.getSort());
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (USERS_SORTABLE_COLUMNS.contains(entry.getKey())) {
                        return entry.getKey();
                    }
                    return "updated_at";
                })
                .orElse("updated_at");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        if (USERS_STRING_SORTABLE_COLUMNS.contains(sortByKey)) {
            sortByKey = "lower(" + sortByKey + ")";
        }
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        Map<String, List<String>> conditions = createUsersWhereClauseAndUpdateParams(params, filter);
        if (conditions.get(USER_IDS_TABLE).size() > 0) {
            usersWhere = " WHERE " + String.join(" AND ", conditions.get(USER_IDS_TABLE));
        }
        List<DbScmUser> results = List.of();
        if (pageSize > 0) {
            String sql = "SELECT * FROM " + company + "." + USER_IDS_TABLE + usersWhere
                    + " ORDER BY " + sortByKey + " " + sortOrder + ", id" // The id is used as a tie breaker to ensure consistent pagination
                    + " OFFSET :skip LIMIT :limit";
            log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.debug("params = {}", params);
            results = template.query(sql, params, userRowMapper());
        }
        String countSql = "SELECT COUNT(*) FROM " + company + "." + USER_IDS_TABLE + usersWhere;
        log.debug("countSql = {}", countSql);
        log.debug("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    private Map<String, List<String>> createUsersWhereClauseAndUpdateParams(Map<String, Object> params,
                                                                            UserIdentitiesFilter filter) {
        List<String> userIdTableConditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            userIdTableConditions.add("integration_id IN (:integration_ids)");
            params.put("integration_ids", filter.getIntegrationIds().stream()
                    .map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getCloudIds())) {
            userIdTableConditions.add("cloud_id IN (:cloud_ids)");
            params.put("cloud_ids", filter.getCloudIds());
        }
        if (MapUtils.isNotEmpty(filter.getPartialMatch())) {
            createPartialMatchFilter(filter.getPartialMatch(), userIdTableConditions, params);
        }
        if (filter.getUsersCreatedRange() != null) {
            if (filter.getUsersCreatedRange().getLeft() != null) {
                userIdTableConditions.add("created_at > " + filter.getUsersCreatedRange().getLeft());
            }
            if (filter.getUsersCreatedRange().getRight() != null) {
                userIdTableConditions.add("created_at < " + filter.getUsersCreatedRange().getRight());
            }
        }
        if (filter.getUsersUpdatedRange() != null) {
            if (filter.getUsersUpdatedRange().getLeft() != null) {
                userIdTableConditions.add("updated_at > " + filter.getUsersUpdatedRange().getLeft());
            }
            if (filter.getUsersUpdatedRange().getRight() != null) {
                userIdTableConditions.add("updated_at < " + filter.getUsersUpdatedRange().getRight());
            }
        }
        if (BooleanUtils.isTrue(filter.getEmptyEmails())) {
            userIdTableConditions.add("(emails = '{}' or emails is null)");
        } else if (BooleanUtils.isFalse(filter.getEmptyEmails())) {
            userIdTableConditions.add("(emails != '{}' and emails is not null)");
        }
        if (filter.getMappingStatus() != null) {
            userIdTableConditions.add("mapping_status = :mapping_status::user_mapping_status_t");
            params.put("mapping_status", filter.getMappingStatus().toString());
        }
        return Map.of(USER_IDS_TABLE, userIdTableConditions);
    }

    private void createPartialMatchFilter(Map<String, Map<String, String>> partialMatchMap,
                                          List<String> userIdTableConditions,
                                          Map<String, Object> params) {
        for (Map.Entry<String, Map<String, String>> partialMatchEntry : partialMatchMap.entrySet()) {
            String key = partialMatchEntry.getKey();
            Map<String, String> value = partialMatchEntry.getValue();
            String begins = value.get("$begins");
            String ends = value.get("$ends");
            String contains = value.get("$contains");
            if (begins != null || ends != null || contains != null) {
                if (USERS_PARTIAL_MATCH_COLUMNS.contains(key)) {
                    createPartialMatchCondition(userIdTableConditions, params, key, begins, ends, contains);
                }
            }
        }
    }

    private void createPartialMatchCondition(List<String> userIdTableConditions, Map<String, Object> params,
                                             String key, String begins, String ends, String contains) {
        if (begins != null) {
            String beginsCondition = key + " SIMILAR TO :" + key + "_begins ";
            params.put(key + "_begins", begins + "%");
            userIdTableConditions.add(beginsCondition);
        }
        if (ends != null) {
            String endsCondition = key + " SIMILAR TO :" + key + "_ends ";
            params.put(key + "_ends", "%" + ends);
            userIdTableConditions.add(endsCondition);
        }
        if (contains != null) {
            String containsCondition = key + " SIMILAR TO :" + key + "_contains ";
            params.put(key + "_contains", "%" + contains + "%");
            userIdTableConditions.add(containsCondition);
        }
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public Integer getSequenceNumberForExtUser(String company) {
        Integer seqNum = 0;
        String selectSequenceSql = "SELECT nextval('" + company + ".sequence_num_ext_user_id')";
        try (Connection conn = dataSource.getConnection(); PreparedStatement selectSequencePstmt = conn.prepareStatement(selectSequenceSql)) {
            ResultSet rs = selectSequencePstmt.executeQuery();
            while (rs.next()) {
                seqNum = rs.getInt("nextval");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return seqNum;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        DatabaseUtils.createEnumType(template.getJdbcTemplate(),
                "user_mapping_status_t", MappingStatus.class);

        List<String> integUserSql = List.of("CREATE TABLE IF NOT EXISTS " + company + "." + USER_IDS_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    cloud_id               VARCHAR NOT NULL,\n" +
                        "    display_name           VARCHAR NOT NULL,\n" +
                        "    original_display_name  VARCHAR NOT NULL,\n" +
                        "    emails                 VARCHAR[],\n" +
                        "    mapping_status         user_mapping_status_t," +
                        "    created_at             BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM now())," +
                        "    updated_at             BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM now())," +
                        "    UNIQUE (integration_id, cloud_id)\n" +
                        ")",

                "CREATE INDEX IF NOT EXISTS " + USER_IDS_TABLE + "_compound_idx on "
                        + company + "." + USER_IDS_TABLE + "(integration_id,cloud_id,original_display_name)",
                "CREATE INDEX IF NOT EXISTS " + USER_IDS_TABLE + "_compound_idx_emails on "
                        + company + "." + USER_IDS_TABLE + "(integration_id,emails)",

                "CREATE SEQUENCE IF NOT EXISTS " + company + ".sequence_num_ext_user_id"
        );
        integUserSql.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

    public String getUser(String company, String integrationId, String cloudId) {
        Validate.notBlank(cloudId, "Missing cloud_id.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + USER_IDS_TABLE
                + " WHERE cloud_id = :cloud_id AND integration_id = :integid";
        Map<String, Object> params = Map.of("cloud_id", cloudId,
                "integid", NumberUtils.toInt(integrationId));
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<DbScmUser> data = template.query(sql, params, userRowMapper());
        if (data.size() > 0) {
            return data.stream().findFirst().get().getId();
        }
        log.debug("Could not find user with cloud_id = " + cloudId + ", integration_id = " + integrationId);
        return null;
    }

    public Optional<DbScmUser> getUserByCloudId(String company, String integrationId, String cloudIdCaseInsensetive) {
        Validate.notBlank(integrationId, "Missing integrationId.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        Validate.notBlank(cloudIdCaseInsensetive, "Missing cloud_id.");

        String sql = "SELECT * FROM " + company + "." + USER_IDS_TABLE
                + " WHERE cloud_id ILIKE :cloud_id AND integration_id = :integid";
        Map<String, Object> params = Map.of("cloud_id", cloudIdCaseInsensetive,
                "integid", NumberUtils.toInt(integrationId));
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<DbScmUser> data = template.query(sql, params, userRowMapper());
        return CollectionUtils.isNotEmpty(data) ? Optional.ofNullable(data.get(0)) : Optional.empty();
    }

    public Optional<String> getUserByDisplayName(String company, String integrationId, String displayName) {
        Validate.notBlank(displayName, "Missing displayName.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + USER_IDS_TABLE
                + " WHERE display_name = :displayName AND integration_id = :integrationId";
        Map<String, Object> params = Map.of("displayName", displayName,
                "integrationId", NumberUtils.toInt(integrationId));
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<DbScmUser> data = template.query(sql, params, userRowMapper());
        if (data.size() > 0) {
            return Optional.of(data.stream().findFirst().get().getId());
        }
        log.debug("Could not find user with display_name = " + displayName + ", integration_id = " + integrationId);
        return Optional.empty();
    }

    public Optional<String> getUserByOriginalDisplayName(String company, String integrationId, String
            originalDisplayName) {
        Validate.notBlank(originalDisplayName, "Missing original displayName.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + USER_IDS_TABLE
                + " WHERE original_display_name = :originalDisplayName AND integration_id = :integrationId";
        Map<String, Object> params = Map.of("originalDisplayName", originalDisplayName,
                "integrationId", NumberUtils.toInt(integrationId));
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<DbScmUser> data = template.query(sql, params, userRowMapper());
        if (data.size() > 0) {
            return Optional.of(data.stream().findFirst().get().getId());
        }
        log.debug("Could not find user with display_name = " + originalDisplayName + ", integration_id = " + integrationId);
        return Optional.empty();
    }

    public Optional<DbScmUser> getUserByEmail(String company, String integrationId, String email) {
        Validate.notBlank(email, "Missing email");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + USER_IDS_TABLE
                + " WHERE :email=ANY(emails) AND integration_id = :integrationId";
        Map<String, Object> params = Map.of("email", email, "integrationId", NumberUtils.toInt(integrationId));
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<DbScmUser> data = template.query(sql, params, userRowMapper());
        if (data.size() > 0) {
            return data.stream().findFirst();
        }
        log.debug("Could not find user with email = " + email + ", integration_id = " + integrationId);
        return Optional.empty();
    }
}
