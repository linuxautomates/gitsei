package io.levelops.commons.databases.services.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import io.levelops.commons.databases.converters.RunbookRunConverters;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.DBOrgUser.LoginId;
import io.levelops.commons.databases.models.database.organization.DBOrgUserCloudIdMapping;
import io.levelops.commons.databases.models.database.organization.OrgUserId;
import io.levelops.commons.databases.models.database.organization.OrgUserSchema;
import io.levelops.commons.databases.models.database.organization.OrgVersion;
import io.levelops.commons.databases.models.database.organization.OrgVersion.OrgAssetType;
import io.levelops.commons.databases.models.database.runbooks.RunbookRun;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunState;
import io.levelops.commons.databases.models.database.runbooks.RunbookVariable;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.database.scm.DbScmUser.MappingStatus;
import io.levelops.commons.databases.models.filters.UserIdentitiesFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.OUDashboardService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.utils.DatabaseUtils;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.ingestion.models.IntegrationType;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import javax.validation.constraints.NotNull;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@SuppressWarnings("unused")
@Log4j2
public class OrgUsersDatabaseService extends DatabaseService<DBOrgUser> {
    public final static String ORG_USERS_TABLE_NAME = "org_users";
    public final static int MAPPING_PAGE_SIZE = 1000;
    public final static String ORG_USERS_CLOUD_IDS_MAPPING_TABLE_NAME = "org_user_cloud_id_mapping";
    public final static String CLOUD_IDS_TABLE = "integration_users";
    public final static String OU_MEMBERSHIP_TABLE_NAME = "ou_membership";
    public final static String OU_USERS_SCHEMAS_TABLE_NAME = "org_users_schemas";
    public final static String OU_USERS_CONTRIBUTOR_ROLES_TABLE_NAME = "org_users_contributor_roles";


    private final static OrgVersion DEFAULT_VERSION = OrgVersion.builder().version(1).build();
    private final static List<String> DEFAULT_CONTRIBUTOR_ROLES = List.of("Junior Software Engineer","Senior Software Engineer","Staff Software Engineer",
            "Junior QA Engineer","Senior QA Engineer","Staff QA Engineer","Junior SRE","Senior SRE","Staff SRE");

    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper mapper;
    private final OrgVersionsDatabaseService versionsService;
    private final UserIdentityService userIdentityService;

    private final static List<String> ddl = List.of(
            "CREATE TABLE IF NOT EXISTS {0}.{1} (" + // org user
                    "    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                    "    ref_id         INTEGER NOT NULL," +
                    "    full_name      VARCHAR(150) NOT NULL," +
                    "    email          VARCHAR(150) NOT NULL," +
                    "    custom_fields  JSONB," +
                    "    versions       INTEGER[] NOT NULL," +
                    "    active         BOOL NOT NULL," +
                    "    created_at     TIMESTAMP NOT NULL DEFAULT (now() at time zone ''UTC'')," +
                    "    updated_at     TIMESTAMP NOT NULL DEFAULT (now() at time zone ''UTC'')" +
                    ");",

            "CREATE EXTENSION IF NOT EXISTS btree_gin",
            "CREATE EXTENSION IF NOT EXISTS intarray;",

            "CREATE INDEX IF NOT EXISTS {1}_ref_id_versions_idx ON {0}.{1} USING GIN (ref_id, versions)",
            // "CREATE INDEX IF NOT EXISTS {1}_ref_id_idx ON {0}.{1}(ref_id)",
            "CREATE INDEX IF NOT EXISTS {1}_full_name_idx ON {0}.{1}(lower(full_name))",
            "CREATE INDEX IF NOT EXISTS {1}_email_versions_idx ON {0}.{1}(lower(email), versions)",
            "CREATE INDEX IF NOT EXISTS {1}_versions_idx ON {0}.{1} USING GIN (versions)",
            "CREATE INDEX IF NOT EXISTS {1}_created_at_idx ON {0}.{1}(created_at)",
            "CREATE INDEX IF NOT EXISTS {1}_updated_at_idx ON {0}.{1}(updated_at)",

            "CREATE TABLE IF NOT EXISTS {0}.{2} (" + // mappings
                    "    id                        UUID NOT NULL DEFAULT uuid_generate_v4()," +
                    "    org_user_id               UUID NOT NULL REFERENCES {0}.{1}(id) ON DELETE CASCADE," +
                    "    integration_user_id       UUID REFERENCES {0}.{3}(id) ON DELETE RESTRICT," +
                    "    mapping_status            user_mapping_status_t," +
                    "    UNIQUE (org_user_id, integration_user_id)" +
                    ");",

            "CREATE TABLE IF NOT EXISTS {0}.{4} (" + // org_users_schemas" +
                    "    id            INTEGER PRIMARY KEY NOT NULL," +
                    "    schema        JSONB NOT NULL," +
                    "    created_at    TIMESTAMP NOT NULL DEFAULT (now() at time zone ''UTC'')" +
                    ");",

            "CREATE TABLE IF NOT EXISTS {0}.{5} (" + // org_users_contributor_roles" +
                    "    id                      UUID NOT NULL DEFAULT uuid_generate_v4()," +
                    "    contributor_role        varchar(150) NOT NULL," +
                    "    created_at              TIMESTAMP NOT NULL DEFAULT (now() at time zone ''UTC'')," +
                    "    UNIQUE (contributor_role)" +
                    ");"
    );

    //#region queries

    private final static String BASE_FROM =
            " FROM  \n" +
                    "    {0}.{1} u\n" +
                    "    {4}\n" +
                    " {5} \n" +
                    " GROUP BY u.id {6}\n";

    private final static String BASE_SELECT =
            "SELECT \n" +
                    "    u.id,\n" +
                    "    u.ref_id,\n" +
                    "    u.full_name,\n" +
                    "    u.email,\n" +
                    "    u.custom_fields,\n" +
                    "    u.versions,\n" +
                    "    u.active,\n" +
                    "    u.created_at,\n" +
                    "    u.updated_at,\n" +
                    "    to_jsonb(ARRAY(SELECT row_to_json(ut) FROM (SELECT iu.display_name, iu.cloud_id, iu.integration_id, i.application, ucm.org_user_id FROM {0}.{2} ucm, {0}.{3} iu, {0}.integrations i " +
                    "WHERE u.id = ucm.org_user_id AND iu.id = ucm.integration_user_id AND i.id = iu.integration_id) AS ut)) ids\n" +
                    BASE_FROM;

    private final static String INSERT_ORG_USER_SQL_FORMAT =
            "INSERT INTO {0}.{1}(ref_id, full_name, email, custom_fields, versions, active) "
                    + "VALUES(COALESCE((SELECT ref_id FROM {0}.{1} WHERE email = :email or ref_id = :refId LIMIT 1), (SELECT MAX(ref_id)+1 FROM {0}.{1}), 1), :fullName, :email, :customFields::jsonb, :versions::integer[], :active) ";
    // + "ON CONFLICT (ref_id, versions) DO UPDATE SET full_name = EXCLUDED.full_name, email = EXCLUDED.email, custom_fields = EXCLUDED.custom_fields, active = EXCLUDED.active, updated_at = EXTRACT(epoch FROM now())";
    private final static String UPDATE_USER_VERSIONS_SQL_FORMAT = "UPDATE {0}.{1} SET versions = :versions, updated_at = EXTRACT(epoch FROM now()) WHERE id = :id::uuid";
    private final static String UPSERT_INTEGRATION_USERNAMES_SQL_FORMAT = "INSERT INTO {0}.{1}(integration_id, cloud_id, display_name,original_display_name) VALUES(:integrationId, :cloudId, :cloudId,:cloudId) ON CONFLICT (integration_id, cloud_id) DO NOTHING";
    private final static String INSERT_ORG_USER_CLOUD_IDS_MAPPINGS_SQL_FORMAT = "INSERT INTO {0}.{1}(org_user_id, integration_user_id, mapping_status) VALUES(:orgUserId, :integrationUserId, :mappingStatus::user_mapping_status_t) ON CONFLICT(org_user_id, integration_user_id) DO NOTHING";
    private final static String BASE_SELECT_FROM_ORG_USERS = "SELECT org_user_id FROM {0}.{1} as oum WHERE {2}";

    private final static String GET_VERSIONED_ORG_USER_SQL_FORMAT = "SELECT id, ref_id, email, full_name FROM {0}.{1} WHERE ref_id = :refId AND versions @> :versions LIMIT 1;";

    private final static String DELETE_ORG_USERS_SQL_FORMAT =
            "DELETE FROM {0}.{1} as c WHERE {2}";

    private static final Set<String> allowedFilters = Set.of("integration_id", "org_user_id", "ref_id", "email", "full_name", "custom_fields", "version");
    private static final int DEFAULT_PAGE_SIZE = 10;

    //#endregion

    public OrgUsersDatabaseService(
            final DataSource dataSource,
            final ObjectMapper mapper,
            final OrgVersionsDatabaseService versionsService,
            final UserIdentityService userIdentityService) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.mapper = mapper;
        this.versionsService = versionsService;
        this.userIdentityService = userIdentityService;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class, UserIdentityService.class, OrgVersionsDatabaseService.class);
    }

    /**
     * Returns the user id which can be used to retrieve any version of the user
     */
    @Override
    public String insert(String company, DBOrgUser dbOuUser) throws SQLException {
        return String.valueOf(upsert(company, dbOuUser).getRefId());
    }

    public OrgUserId upsert(String company, @NotNull DBOrgUser dbOuUser) throws SQLException {
        return upsertInternal(company, dbOuUser, MappingStatus.MANUAL, null);
    }

    public OrgUserId upsertAuto(String company, @NotNull DBOrgUser dbOuUser, Set<Integer> currentVersions) throws SQLException {
        return upsertInternal(company, dbOuUser, MappingStatus.AUTO, currentVersions);
    }

    public OrgUserId upsertInternal(String company, @NotNull DBOrgUser dbOuUser, MappingStatus mappingStatus, Set<Integer> currentVersions) throws SQLException {
        // new versions only
        // if no cloudIds, return after inserting user
        OrgUserId orgUserId = null;
        if (currentVersions != null) {
            orgUserId = insertOrgUser(company, dbOuUser, currentVersions);
        } else {
            orgUserId = insertOrgUser(company, dbOuUser);
        }
        if (CollectionUtils.isEmpty(dbOuUser.getIds())) {
            return orgUserId;
        }
        if (Objects.isNull(orgUserId)) {
            return null;
        }

        linkCloudIds(company, orgUserId.getId(), dbOuUser.getIds(), mappingStatus);
        return orgUserId;
    }

    public void upgradeUsersVersion(final String company, final UUID versionId) throws SQLException {
        upgradeUsersVersion(company, versionId, Set.of());
    }

    public void upgradeUsersVersion(final String company, final UUID versionId, Set<Integer> exclusions) throws SQLException {
        var newVersion = versionsService.get(company, versionId).get();
        var currentVersion = versionsService.getActive(company, OrgAssetType.USER).get();
        upgradeUsersVersion(company, newVersion.getVersion(), currentVersion.getVersion(), exclusions);
    }

    public void upgradeUsersVersion(final String company, final Integer newVersion, final Integer currentVersion) throws SQLException {
        upgradeUsersVersion(company, newVersion, currentVersion, Set.of());
    }

    public void upgradeUsersVersion(final String company, final Integer newVersion, final Integer currentVersion, Set<Integer> exclusions) throws SQLException {
        var exclusionsQ = CollectionUtils.isEmpty(exclusions) ? "" : "AND ref_id NOT IN (:excludedRefIds)";
        try (var conn = this.template.getJdbcTemplate().getDataSource().getConnection();) {
            this.template.update(
                    MessageFormat.format("UPDATE {0}.{1} SET versions = versions || :newVersion, updated_at = (now() at time zone ''UTC'') WHERE versions @> :currentVersion {2}", company, ORG_USERS_TABLE_NAME, exclusionsQ),
                    new MapSqlParameterSource().addValue("newVersion", newVersion)
                            .addValue("currentVersion", conn.createArrayOf("integer", new Integer[]{currentVersion}))
                            .addValue("excludedRefIds", exclusions));
        }
    }

    private OrgUserId insertOrgUser(String company, DBOrgUser dbOrgUser) throws SQLException {
        // get the latest version for the users directory.
        // the users directory version is incremented before the inserts happen so,
        // in user insert we can always use the latest version
        var currentVersions = Set.of(versionsService.getLatest(company, OrgAssetType.USER).orElse(DEFAULT_VERSION).getVersion());
        return insertOrgUserInternal(company, dbOrgUser, currentVersions);
    }

    private OrgUserId insertOrgUser(String company, DBOrgUser dbOrgUser, Set<Integer> currentVersions) throws SQLException {
        Validate.notEmpty(currentVersions, "currentVersions cannot be empty");
        return insertOrgUserInternal(company, dbOrgUser, currentVersions);
    }

    private OrgUserId insertOrgUserInternal(String company, DBOrgUser dbOrgUser, Set<Integer> currentVersions) throws SQLException {
        log.info("[{}] Inserting ou user: {}", company, dbOrgUser);
        // In case user with email or refId already exists, get the ref_id
        var refId = (Integer) null;
        try {
            refId = this.template.queryForObject(MessageFormat.format(
                            "SELECT ref_id FROM {0}.{1} WHERE email = :email or ref_id = :ref_id LIMIT 1;", company, ORG_USERS_TABLE_NAME),
                    new MapSqlParameterSource().addValue("email", dbOrgUser.getEmail().toLowerCase())
                            .addValue("ref_id",dbOrgUser.getRefId()),
                    Integer.class);
        } catch (EmptyResultDataAccessException e) {
            log.debug("[{}] No user found with email '{}'", company, dbOrgUser.getEmail());
        }
        // if the email already exists we can use the ref_id to check if the current versions record already exists
        if (refId != null) {
            UUID id = null;
            try (var conn = this.template.getJdbcTemplate().getDataSource().getConnection();) {
                return template.queryForObject(
                        MessageFormat.format(GET_VERSIONED_ORG_USER_SQL_FORMAT, company, ORG_USERS_TABLE_NAME),
                        new MapSqlParameterSource().addValue("refId", refId).addValue("versions", conn.createArrayOf("integer", currentVersions.toArray())),
                        orgUserIdRowMapper());
            }
            // if no record found an EmptyResultDataAccessException is trown
            catch (EmptyResultDataAccessException e) {
                log.debug("[{}] No user found with email '{}' and versions '{}'", company, dbOrgUser.getEmail(), currentVersions);
            }
        }
        int count = 0;
        var keyHolder = new GeneratedKeyHolder();
        var customFields = "{}";
        if (MapUtils.isNotEmpty(dbOrgUser.getCustomFields())) {
            try {
                customFields = mapper.writeValueAsString(dbOrgUser.getCustomFields());
            } catch (JsonProcessingException e) {
                log.error("[{}] Error converting to json the custom fields for user '{}': {}", company, dbOrgUser.getEmail(), dbOrgUser.getCustomFields(), e);
            }
        }
        // If a user exists with email or ref-id, insert a new row with same ref_id
        // If a user doesn't exist with email or ref-id, insert a new row altogether
        try (var conn = this.template.getJdbcTemplate().getDataSource().getConnection();) {
            count = this.template.update(
                    MessageFormat.format(INSERT_ORG_USER_SQL_FORMAT, company, ORG_USERS_TABLE_NAME),
                    new MapSqlParameterSource()
                                .addValue("fullName", dbOrgUser.getFullName())
                                .addValue("email", dbOrgUser.getEmail().toLowerCase())
                                .addValue("refId", refId != null ? refId : 0)
                                .addValue("customFields", customFields)
                                .addValue("versions", conn.createArrayOf("integer", currentVersions.toArray()))
                                .addValue("active", dbOrgUser.isActive()),
                    keyHolder,
                    new String[]{"id", "full_name", "ref_id", "email"}
                );
            return count > 0
                    ? OrgUserId.builder()
                    .id(UUID.fromString(keyHolder.getKeys().get("id").toString()))
                    .refId((Integer) keyHolder.getKeys().get("ref_id"))
                    .build()
                    : template.queryForObject(
                    MessageFormat.format(GET_VERSIONED_ORG_USER_SQL_FORMAT, company, ORG_USERS_TABLE_NAME),
                    new MapSqlParameterSource().addValue("refId", dbOrgUser.getRefId()).addValue("versions", conn.createArrayOf("integer", currentVersions.toArray())),
                    orgUserIdRowMapper());
        }
    }

    private RowMapper<OrgUserId> orgUserIdRowMapper() {
        return (rs, row) -> OrgUserId.builder()
                .id((UUID) rs.getObject("id"))
                .refId(rs.getInt("ref_id"))
                .email(rs.getString("email"))
                .fullName(rs.getString("full_name"))
                .build();
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = OrgUserCloudIdMappingFilter.OrgUserCloudIdMappingFilterBuilder.class)
    public static class OrgUserCloudIdMappingFilter {
        @JsonProperty("org_user_ids")
        List<UUID> orgUserIds;

        @JsonProperty("integration_user_ids")
        List<UUID> integrationUserIds;

        @JsonProperty("mapping_status")
        MappingStatus mappingStatus;
    }

    public static RowMapper<DBOrgUserCloudIdMapping> orgUserCloudIdMappingRowMapper() {
        return (rs, rowNumber) -> {
            MappingStatus mappingStatus = null;
            if (rs.getString("mapping_status") != null) {
                mappingStatus = MappingStatus.fromString(rs.getString("mapping_status"));
            }
            return DBOrgUserCloudIdMapping.builder()
                    .id(UUID.fromString(rs.getString("id")))
                    .orgUserId(UUID.fromString(rs.getString("org_user_id")))
                    .integrationUserId(UUID.fromString(rs.getString("integration_user_id")))
                    .mappingStatus(mappingStatus)
                    .build();
        };
    }

    public Stream<DBOrgUserCloudIdMapping> streamOrgUserCloudIdMappings(String company, OrgUserCloudIdMappingFilter filter) {
        return PaginationUtils.streamThrowingRuntime(0, 1, pageNumber ->
                filterOrgUserCloudIdMappings(company, filter, pageNumber, DEFAULT_PAGE_SIZE).getRecords());
    }

    public DbListResponse<DBOrgUserCloudIdMapping> filterOrgUserCloudIdMappings(
            final String company,
            final OrgUserCloudIdMappingFilter filter,
            final Integer pageNumber,
            final Integer pageSize
    ) throws SQLException {
        int limit = MoreObjects.firstNonNull(pageSize, MAPPING_PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        if (filter.getIntegrationUserIds() != null) {
            conditions.add("integration_user_id in (:integration_user_ids)");
            params.put("integration_user_ids", filter.getIntegrationUserIds());
        }

        if (filter.getOrgUserIds() != null) {
            conditions.add("org_user_id in (:org_user_ids)");
            params.put("org_user_ids", filter.getOrgUserIds());
        }

        if (filter.getMappingStatus() != null) {
            conditions.add("mapping_status = :mapping_status::user_mapping_status_t");
            params.put("mapping_status", filter.getMappingStatus().toString());
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + "." + ORG_USERS_CLOUD_IDS_MAPPING_TABLE_NAME + where +
                " ORDER BY id" +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<DBOrgUserCloudIdMapping> results = template.query(sql, params, orgUserCloudIdMappingRowMapper());

        return DbListResponse.of(results, null);
    }


    public Boolean linkCloudIds(String company, final UUID orgUserId, Set<LoginId> loginIds, MappingStatus mappingStatus) throws SQLException {
        log.debug("[{}] mapping login ids for ou user '{}': {}", company, orgUserId, loginIds);
        // inset cloud id if not present
        var keyHolder = new GeneratedKeyHolder();
        var mappingValues = new MapSqlParameterSource[loginIds.size()];
        var index = 0;

        // This whole flow rests on the assumption that the user version is incremented but not activated. So we can
        // get the previous user version from the DB
        Optional<OrgVersion> currentVersion = versionsService.getActive(company, OrgAssetType.USER);
        Optional<DBOrgUser> orgUser = get(company, orgUserId);
        // Create a new set of loginIds with the username and integrationType removed. This is so that we can compare
        // the set of LoginIds that we create (oldLoginIds) with this set. Not filling out this extra info saves a DB
        // call
        Set<LoginId> newLoginIds = loginIds.stream().map(loginId -> loginId.toBuilder()
                        .username(null)
                        .integrationType(null)
                        .build())
                .collect(Collectors.toSet());
        Set<LoginId> addedCloudIds = new HashSet<>();
        Set<LoginId> deletedCloudIds = new HashSet<>();
        Map<UUID, DBOrgUserCloudIdMapping> oldOrgUserCloudIdMappings = Map.of();

        // Get the diff between the current and previous version mappings. We will only apply the new mappingStatus
        // to the difference
        if (currentVersion.isPresent() && orgUser.isPresent()) {
            Optional<DBOrgUser> previousOrgUserVersion = get(company, orgUser.get().getRefId(), currentVersion.get().getVersion());
            if (previousOrgUserVersion.isPresent()) {
                oldOrgUserCloudIdMappings = streamOrgUserCloudIdMappings(
                        company,
                        OrgUserCloudIdMappingFilter.builder()
                                .orgUserIds(List.of(previousOrgUserVersion.get().getId()))
                                .build()
                ).collect(Collectors.toMap(DBOrgUserCloudIdMapping::getIntegrationUserId, x -> x));

                List<UUID> oldIntegrationUserIds = oldOrgUserCloudIdMappings.values().stream().map(DBOrgUserCloudIdMapping::getIntegrationUserId)
                        .collect(Collectors.toList());
                Set<LoginId> oldLoginIds = Set.of();
                if (!oldIntegrationUserIds.isEmpty()) {
                    oldLoginIds = userIdentityService
                            .list(company, oldIntegrationUserIds.stream().map(UUID::toString).collect(Collectors.toList()))
                            .getRecords()
                            .stream()
                            .map(user -> {
                                return LoginId.builder()
                                        .cloudId(user.getCloudId())
                                        .integrationId(Integer.parseInt(user.getIntegrationId()))
                                        .build();
                            })
                            .collect(Collectors.toSet());
                }
                addedCloudIds = Sets.difference(newLoginIds, oldLoginIds);
                deletedCloudIds = Sets.difference(oldLoginIds, newLoginIds);
            }
        }

        for (LoginId loginId : newLoginIds) {
            if (StringUtils.isBlank(loginId.getCloudId())) {
                log.warn("Skipping linking of orgUserId={}, cloudId was null: {}", orgUserId, loginId);
                continue;
            }
            var count = this.template.update(
                    MessageFormat.format(UPSERT_INTEGRATION_USERNAMES_SQL_FORMAT, company, CLOUD_IDS_TABLE),
                    new MapSqlParameterSource()
                            .addValue("integrationId", loginId.getIntegrationId())
                            .addValue("cloudId", loginId.getCloudId()),
                    keyHolder,
                    new String[]{"id"}
            );
            var integrationUserId = count > 0
                    ? (UUID) keyHolder.getKeys().get("id")
                    : this.template.queryForObject(
                    MessageFormat.format("SELECT id FROM {0}.{1} WHERE integration_id = :integrationId AND cloud_id = :cloudId", company, CLOUD_IDS_TABLE),
                    new MapSqlParameterSource().addValue("integrationId", loginId.getIntegrationId()).addValue("cloudId", loginId.getCloudId()),
                    UUID.class);
            MappingStatus finalStatus = mappingStatus;
            // If this mapping existed before, then we retain the mapping status
            if (oldOrgUserCloudIdMappings.containsKey(integrationUserId)) {
                MappingStatus status = oldOrgUserCloudIdMappings.get(integrationUserId).getMappingStatus();
                if(status != null){
                    finalStatus = status;
                }
            }
            mappingValues[index++] = new MapSqlParameterSource()
                    .addValue("orgUserId", orgUserId)
                    .addValue("integrationUserId", integrationUserId)
                    .addValue("mappingStatus", finalStatus.toString());
        }

        // insert relationship
        log.debug("[{}] mapping login ids for ou user '{}': {}", company, orgUserId, loginIds);
        var results = this.template.batchUpdate(
                MessageFormat.format(INSERT_ORG_USER_CLOUD_IDS_MAPPINGS_SQL_FORMAT, company, ORG_USERS_CLOUD_IDS_MAPPING_TABLE_NAME),
                mappingValues
        );
        Set<LoginId> cloudIdsToMarkAsOverridden = Sets.union(addedCloudIds, deletedCloudIds);
        if (CollectionUtils.isNotEmpty(cloudIdsToMarkAsOverridden)) {
            log.info("Cloud ids to mark as overridden: {}", cloudIdsToMarkAsOverridden);
        }
        userIdentityService.batchUpdateMappingStatus(company, cloudIdsToMarkAsOverridden, mappingStatus);

        return results != null;
    }


    @Override
    public Boolean update(String company, DBOrgUser orgUser) throws SQLException {
        int updateCount = this.template.update(
                MessageFormat.format(UPDATE_USER_VERSIONS_SQL_FORMAT, company, ORG_USERS_TABLE_NAME),
                new MapSqlParameterSource()
                        .addValue("versions", orgUser.getVersions())
                        .addValue("id", orgUser.getId())
        );
        return updateCount > 0;
    }

    @Override
    public Optional<DBOrgUser> get(String company, String orgUserId) throws SQLException {
        return get(company, UUID.fromString(orgUserId));
    }

    public Optional<DBOrgUser> get(String company, UUID orgUserId) throws SQLException {
        var results = filter(company, QueryFilter.builder().strictMatch("org_user_id", orgUserId).build(), 0, 1);
        if (results.getCount() > 0) {
            return Optional.of(results.getRecords().get(0));
        }
        return Optional.empty();
    }

    /**
     * Retrieves the active version of the user identified by user id (not db id).
     */
    public Optional<DBOrgUser> get(final String company, final int refId) throws SQLException {
        return get(company, refId, null);
    }

    /**
     * Retrieves the specified version of the user identified by user id (not db id).
     * If the version is not specified, the active version will be used.
     */
    public Optional<DBOrgUser> get(final String company, final int refId, final Integer version) throws SQLException {
        var filterBuilder = QueryFilter.builder().strictMatch("ref_id", refId);
        if (version != null && version > 0) {
            filterBuilder.strictMatch("version", version);
        }
        var results = filter(company, filterBuilder.build(), 0, 1);
        if (results.getCount() > 0) {
            return Optional.of(results.getRecords().get(0));
        }
        return Optional.empty();
    }

    public Optional<DBOrgUser> getByUser(final String company, final String user) throws SQLException {
        var currentVersions = Set.of(versionsService.getLatest(company, OrgAssetType.USER).orElse(DEFAULT_VERSION).getVersion());
        var filterBuilder = QueryFilter.builder().strictMatch("full_name", user).strictMatch("versions", currentVersions);
        var results = filter(company, filterBuilder.build(), 0, 1);
        if (results.getCount() > 0) {
            return Optional.of(results.getRecords().get(0));
        }
        return Optional.empty();
    }

    private static final boolean SHOW_NON_USERS = true;

    @Override
    public DbListResponse<DBOrgUser> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return filter(company, null, pageNumber, pageSize, SHOW_NON_USERS);
    }

    public Stream<DBOrgUser> stream(
            final String company,
            QueryFilter filters,
            Integer pageSize) {
        return PaginationUtils.streamThrowingRuntime(0, 1, page -> {
            return filter(company, filters, page, pageSize).getRecords();
        });
    }


    public DbListResponse<DBOrgUser> filter(
            final String company,
            QueryFilter filters,
            final Integer pageNumber,
            Integer pageSize) throws SQLException {
        return filter(company, filters, pageNumber, pageSize, false);
    }

    public DbListResponse<DBOrgUser> filter(
            final String company,
            QueryFilter filters,
            final Integer pageNumber,
            Integer pageSize,
            final Boolean includeDanglingLogins) throws SQLException {
        var params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();

        populateConditions(company, filters, conditions, params);
        var extraConditions = conditions.size() > 0 ? " WHERE " + String.join(" AND ", conditions) + " " : "";

        var extraFrom = "";
        var extraGrouping = "";

        if (extraConditions.contains("ocm")) {
            extraFrom = MessageFormat.format(", {0}.{1} oum", company, OU_MEMBERSHIP_TABLE_NAME);
            extraConditions += " AND ocm.org_user_id = u.id ";
            extraGrouping = ", ocm.org_user_id";
        }

        if (extraConditions.contains(" iu.")) {
            extraFrom += MessageFormat.format(", {0}.integrations i, {0}.{1} ocm, {0}.{2} iu", company, ORG_USERS_CLOUD_IDS_MAPPING_TABLE_NAME, CLOUD_IDS_TABLE);
            extraConditions += " AND ocm.org_user_id = u.id AND ocm.integration_user_id = iu.id AND iu.integration_id = i.id";
            // extraGrouping = ", oum.org_user_id";
        }

        var querySelect = MessageFormat.format(BASE_SELECT, company, ORG_USERS_TABLE_NAME, ORG_USERS_CLOUD_IDS_MAPPING_TABLE_NAME, CLOUD_IDS_TABLE,
                extraFrom, extraConditions, extraGrouping);
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        StringBuilder danglingWhere = new StringBuilder();
        if (filters != null) {
            if (filters.getPartialMatches() instanceof Map &&
                    filters.getPartialMatches().get("full_name") instanceof String) {
                danglingWhere.append(String.format(" AND iu.display_name ILIKE '%%%s%%' ", String.valueOf(filters.getPartialMatches().get("full_name")).trim()));
            }
            if (filters.getStrictMatches() instanceof Map
                    && filters.getStrictMatches().get("full_name") instanceof List) {
                List<String> fullNames = (List) filters.getStrictMatches().get("full_name");
                String fullNamesString = String.join(",", fullNames);
                danglingWhere.append(String.format(" AND iu.display_name = ANY('{%s}') ", fullNamesString));
            }
        }

        var union = "";
        var orderBy = "\n ORDER BY ref_id, created_at DESC";
        var danglingSelect = "SELECT \n"
                + "   null::uuid as id,\n"
                + "   null::integer as ref_id,\n"
                + "   iu.display_name as full_name,\n"
                + "   '' email, '{}'::jsonb custom_fields,\n"
                + "   ARRAY[0]::integer[] versions,\n"
                + "   false as active,\n"
                + "   to_timestamp(iu.created_at) as created_at,\n"
                + "   to_timestamp(iu.updated_at) as updated_at,\n"
                + "   to_jsonb(ARRAY(SELECT row_to_json(tmp) FROM (SELECT iu.display_name, iu.cloud_id, iu.integration_id, i.application, null::uuid as org_user_id) as tmp )) ids  \n";
        // + "   '[]'::jsonb ids \n";
        var currentActiveVersion = versionsService.getActive(company, OrgAssetType.USER).orElseGet(() -> {
            log.error("Active user version not found for company {}, returning default version", company);
            return DEFAULT_VERSION;
        });
        var danglingFrom = MessageFormat.format(
                "FROM\n"
                        + "   {0}.integration_users iu, \n"
                        + "   {0}.integrations i \n"
                        + "WHERE\n"
                        + "   iu.id NOT IN (SELECT integration_user_id FROM {0}.org_user_cloud_id_mapping \n" +
                        "          where org_user_id in (\n" +
                        "               select id from {0}.org_users where " + currentActiveVersion.getVersion() + " = ANY(versions)\n" +
                        "       )) \n"
                        + "   AND iu.integration_id = i.id \n", company);

        if (includeDanglingLogins && isIntegrationTableApplicable(filters)) {
            union = "\n UNION \n" + danglingSelect + danglingFrom + danglingWhere;
        }
        String limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize.toString(), String.valueOf(pageNumber * pageSize));
        querySelect = querySelect + union;
        var recordsSelect = querySelect + orderBy + limit;
        log.debug(recordsSelect);
        List<DBOrgUser> items = template.query(recordsSelect, params, (rs, row) -> {
            List<Map<String, Object>> projectsTmp = ParsingUtils.parseJsonList(mapper, "ids", rs.getString("ids"));
            return DBOrgUser.builder()
                    .id((UUID) rs.getObject("id"))
                    .refId(rs.getInt("ref_id"))
                    .email(rs.getString("email"))
                    .fullName(rs.getString("full_name"))
                    .customFields(ParsingUtils.parseJsonObject(mapper, "custom_fields", rs.getString("custom_fields")))
                    .versions(ParsingUtils.parseSet("versions", Integer.class, rs.getArray("versions")))
                    .active(rs.getBoolean("active"))
                    .createdAt(rs.getTimestamp("created_at").toInstant())
                    .updatedAt(rs.getTimestamp("updated_at").toInstant())
                    .ids(projectsTmp.stream()
                            .map(item -> DBOrgUser.LoginId.builder()
                                    .integrationId(Integer.valueOf(item.get("integration_id").toString()))
                                    .integrationType(item.get("application").toString())
                                    .username(item.get("display_name").toString())
                                    .cloudId(item.get("cloud_id").toString())
                                    .build())
                            .collect(Collectors.toSet()))
                    // .teams(teams)
                    .build();
        });
        var total = template.queryForObject("SELECT COUNT(*) FROM (" + querySelect + ") as l", params, Integer.class);
        var totalDangling = template.queryForObject("SELECT COUNT(*) " + danglingFrom, params, Integer.class);
        return DbListResponse.of(items, total, Map.of("dangling", totalDangling));
    }
    private static boolean isIntegrationTableApplicable(QueryFilter filters) {
        return (filters == null ||
                !(filters.getStrictMatches().containsKey("email") ||
                        (filters.getPartialMatches() != null &&
                                !filters.getPartialMatches().isEmpty() &&
                                filters.getPartialMatches().keySet().stream().anyMatch(x -> x.startsWith("custom_field_")))
                ));
    }

    private void populateConditions(
            final String company,
            QueryFilter filters,
            final @NonNull List<String> conditions,
            final @NonNull MapSqlParameterSource params) throws SQLException {

        // if no version is specified we will use the current active one
        // aslong as we are not filtering by org user id (which refers to a specific entry for the user and versions)
        if (filters == null || MapUtils.isEmpty(filters.getStrictMatches()) || (!filters.getStrictMatches().containsKey("version") && !filters.getStrictMatches().containsKey("or_user_id"))) {
            if (filters == null) {
                filters = QueryFilter.builder().build();
            }
            var optionalVersion = versionsService.getActive(company, OrgAssetType.USER);
            if (optionalVersion.isPresent()) {
                filters = filters.toBuilder().strictMatch("version", optionalVersion.get().getVersion()).build();
            }
        }
        if (filters != null && MapUtils.isNotEmpty(filters.getPartialMatches()) && filters.getPartialMatches().containsKey("full_name")) {
            conditions.add(MessageFormat.format("u.full_name ILIKE ''%{0}%''", filters.getPartialMatches().get("full_name")));
        }
        if (filters == null || filters.getStrictMatches() == null || filters.getStrictMatches().isEmpty()) {
            return;
        }

        if (filters.getStrictMatches().containsKey("org_user_id")) {
            processCondition("u", "id", filters.getStrictMatches().get("org_user_id"), conditions, params);
            return;
        }
        for (Map.Entry<String, Object> entry : filters.getStrictMatches().entrySet()) {
            var item = entry.getKey();
            var values = entry.getValue();
            if (!(allowedFilters.contains(item) || item.startsWith("custom_field_"))
                    || filters.getStrictMatches().get(item) == null) {
                continue;
            }
            var prefix = "";
            switch (item) {
                case "ref_id":
                case "email":
                case "full_name":
                case "version":
                    prefix = "u";
                    break;
                case "integration_id":
                    prefix = "iu";
                    break;
                // case "custom_fields":
                //     processCondition(prefix, item, values, conditions, params);
                // continue;
                default:
                    continue;
            }
            processCondition(prefix, item, values, conditions, params);
        }
        //for custom filters
        if (filters.getPartialMatches() != null)
            filters.getPartialMatches().entrySet().stream().filter(etr -> etr.getKey()
                    .startsWith("custom_field_")).forEach(x -> {
                var item = x.getKey();
                var values = x.getValue();
                var prefix = "u";
                processCondition(prefix, item, values, conditions, params);
            });
    }

    @SuppressWarnings("unchecked")
    private void processCondition(
            final String prefix,
            final String item,
            final Object values,
            final @NonNull List<String> conditions,
            final @NonNull MapSqlParameterSource params) {
        if (item.equalsIgnoreCase("ingested_at")) {
            Map<String, String> range = (Map<String, String>) values;
            String gt = range.get("$gt");
            if (gt != null) {
                conditions.add(MessageFormat.format("{0}.{1} > :ingested_at_gt", prefix, item));
                params.addValue("ingested_at_gt", NumberUtils.toInt(gt));
            }
            String lt = range.get("$lt");
            if (lt != null) {
                conditions.add(MessageFormat.format("{0}.{1} < :ingested_at_lt", prefix, item));
                params.addValue("ingested_at_lt", NumberUtils.toInt(lt));
            }
            return;
        }
        if (item.equalsIgnoreCase("version")) {
            conditions.add(MessageFormat.format("{0}.versions @> :version", prefix, item));
            try (var conn = this.template.getJdbcTemplate().getDataSource().getConnection();) {
                params.addValue("version", conn.createArrayOf("integer", new Integer[]{(Integer) values}));
            } catch (SQLException e) {
                log.error(e);
            }
            return;
        }
        if (item.startsWith("custom_field_")) {
            var field = item.substring(0, 13);
            var customField=item.substring(field.length());
            conditions.add(MessageFormat.format("{0}.custom_fields ->> ''{1}'' ILIKE :{2}", prefix, customField, item));
            params.addValue(item, MessageFormat.format("%{0}%", values));
            return;
        }
        if (values instanceof Collection) {
            var collection = ((Collection<Object>) values)
                    .stream()
                    .filter(ObjectUtils::isNotEmpty)
                    .map(Object::toString)
                    .collect(Collectors.toSet());
            var tmp = MessageFormat.format("{0}.{1} = ANY({2})", prefix, item, "'{" + String.join(",", collection) + "}'");
            log.debug("filter: {}", tmp);
            conditions.add(tmp);
            return;
        }
        if (values instanceof UUID) {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}::uuid", prefix, item));
        } else if (values instanceof Integer) {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}::int", prefix, item));
        } else if (values instanceof Long) {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}::bigint", prefix, item));
        } else {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}", prefix, item));
        }
        params.addValue(item, values.toString());
    }

    public DbListResponse<Map<String, Object>> getValues(final String company, String field, final QueryFilter filters, final Integer pageNumber, Integer pageSize) throws SQLException {
        var params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        switch (field) {
            case "email":
            case "full_name":
                populateConditions(company, filters, conditions, params);

                var extraConditions = conditions.size() > 0 ? " WHERE " + String.join(" AND ", conditions) + " " : "";

                var prefix = "u";
                var sql = MessageFormat.format("SELECT DISTINCT({7}.{8}) AS v " + BASE_FROM, company, ORG_USERS_TABLE_NAME, ORG_USERS_CLOUD_IDS_MAPPING_TABLE_NAME, CLOUD_IDS_TABLE, "", extraConditions, "", prefix, field);
                if (pageSize == null || pageSize < 1) {
                    pageSize = DEFAULT_PAGE_SIZE;
                }
                String limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize.toString(), String.valueOf(pageNumber * pageSize));
                List<Map<String, Object>> items = template.query(sql + limit, params, (rs, row) -> {
                    return Map.of("key", rs.getObject("v"));
                });
                var totalCount = items.size();
                if (totalCount == pageSize) {
                    totalCount = template.queryForObject("SELECT COUNT(*) FROM (" + sql + ") AS l", params, Integer.class);
                }
                return DbListResponse.of(items, totalCount);
            default:
                if (field.startsWith("custom_field_")) {
                    field = field.substring(13);
                    populateConditions(company, filters, conditions, params);

                    extraConditions = conditions.size() > 0 ? " WHERE " + String.join(" AND ", conditions) + " " : "";

                    prefix = "u";
                    sql = MessageFormat.format("SELECT * FROM (SELECT DISTINCT({7}.custom_fields->>''{8}'') AS v " + BASE_FROM + " ) AS l WHERE v IS NOT NULL", company, ORG_USERS_TABLE_NAME, ORG_USERS_CLOUD_IDS_MAPPING_TABLE_NAME, CLOUD_IDS_TABLE, "", extraConditions, "", prefix, field);
                    if (pageSize == null || pageSize < 1) {
                        pageSize = DEFAULT_PAGE_SIZE;
                    }
                    limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize.toString(), String.valueOf(pageNumber * pageSize));
                    items = template.query("SELECT * FROM (" + sql + ") AS l WHERE v IS NOT NULL" + limit, params, (rs, row) -> {
                        return Map.of("key", rs.getObject("v"));
                    });
                    totalCount = items.size();
                    if (totalCount == pageSize) {
                        totalCount = template.queryForObject("SELECT COUNT(*) FROM (" + sql + ") AS l", params, Integer.class);
                    }
                    return DbListResponse.of(items, totalCount);
                }
                return null;
        }
    }

    public List<String> getOuUsers(final String company, final OUConfiguration ouConfig, IntegrationType integrationType) {
        Map<String, Object> params = new HashMap<>();
        String usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, integrationType);
        log.info("getOuUsers usersSelect = {}", usersSelect);
        if (StringUtils.isNotBlank(usersSelect)) {
            String sql = MessageFormat.format("SELECT id FROM ({0}) l", usersSelect);
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            return template.query(sql, params, ouUsersRowMapper());
        }
        return List.of();
    }

    public List<String> getOuUsersDisplayNames(final String company, final OUConfiguration ouConfig, IntegrationType integrationType) {
        Map<String, Object> params = new HashMap<>();
        String usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, integrationType);
        log.info("getOuUsers usersSelect = {}", usersSelect);
        if (StringUtils.isNotBlank(usersSelect)) {
            String sql = MessageFormat.format("SELECT display_name FROM ({0}) l", usersSelect);
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            return template.query(sql, params, ouUsersDisplayNameRowMapper());
        }
        return List.of();
    }

    private RowMapper<String> ouUsersRowMapper() {
        return (rs, row) -> String.valueOf(rs.getObject("id"));
    }

    private RowMapper<String> ouUsersDisplayNameRowMapper() {
        return (rs, row) -> rs.getString("display_name");
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new SQLException("No deletions allowed, use UserHelper for logical delete");
    }

    public Integer insertUsersSchema(final String company, final Set<OrgUserSchema.Field> fields) throws DataAccessException, JsonProcessingException {
        var keyHolder = new GeneratedKeyHolder();
        var count = this.template.update(
                MessageFormat.format("INSERT INTO {0}.{1} (id, schema) VALUES((SELECT COALESCE((SELECT MAX(id)+1 FROM {0}.{1}), 1)), :schema::jsonb)", company, OU_USERS_SCHEMAS_TABLE_NAME),
                new MapSqlParameterSource().addValue("schema", mapper.writeValueAsString(fields)),
                keyHolder,
                new String[]{"id"}
        );
        return count > 0
                ? (Integer) keyHolder.getKeys().get("id")
                : null;
    }

    public Optional<OrgUserSchema> getUsersSchemas(final String company, final Integer version) {
        var condition = version != null ? "WHERE id = :version" : "ORDER BY id DESC LIMIT 1";
        try {
            var results = this.template.queryForObject(
                    MessageFormat.format("SELECT * FROM {0}.{1} {2}", company, OU_USERS_SCHEMAS_TABLE_NAME, condition),
                    new MapSqlParameterSource().addValue("version", version),
                    orgUserSchemaMapper()
            );
            return Optional.of(results);
        } catch (EmptyResultDataAccessException e) {
            log.debug("[{}] No schema found: version='{}'", company, version);
            return Optional.empty();
        }
    }

    private RowMapper<OrgUserSchema> orgUserSchemaMapper() {
        return (rs, row) -> {
            List<OrgUserSchema.Field> fields = ParsingUtils.parseList(mapper, "schema", OrgUserSchema.Field.class, rs.getString("schema"));
            return OrgUserSchema.builder()
                    .version(rs.getInt("id"))
                    .createdAt(rs.getTimestamp("created_at").toInstant())
                    .fields(Set.copyOf(fields))
                    .build();
        };
    }

    public Optional<Set<OrgUserSchema>> listUsersSchemas(final String company) {
        try {
            var results = this.template.query(
                    MessageFormat.format("SELECT * FROM {0}.{1}", company, OU_USERS_SCHEMAS_TABLE_NAME),
                    new MapSqlParameterSource(),
                    orgUserSchemaMapper()
            );
            return Optional.of(Set.copyOf(results));
        } catch (EmptyResultDataAccessException e) {
            log.debug("[{}] No schema found", company);
            return Optional.empty();
        }
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        DatabaseUtils.createEnumType(template.getJdbcTemplate(),
                "user_mapping_status_t", MappingStatus.class);
        ddl.forEach(item -> template.getJdbcTemplate()
                .execute(MessageFormat.format(
                        item,
                        company,
                        ORG_USERS_TABLE_NAME,
                        ORG_USERS_CLOUD_IDS_MAPPING_TABLE_NAME,
                        CLOUD_IDS_TABLE,
                        OU_USERS_SCHEMAS_TABLE_NAME,
                        OU_USERS_CONTRIBUTOR_ROLES_TABLE_NAME)));
        insertDefaultContributorRoles(company);
        return true;
    }

    private static final String INSERT_CONTRIBUTOR_ROLES_SQL_FORMAT = "INSERT INTO %s.org_users_contributor_roles (contributor_role) "
            + "VALUES %s "
            + "ON CONFLICT(contributor_role) DO NOTHING";
    private int insertDefaultContributorRoles(final String company) {
        Map<String, Object> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        for(String contributorRole : DEFAULT_CONTRIBUTOR_ROLES){
            int i = values.size();
            params.putAll(Map.of(
                    "contributor_role_" + i, contributorRole
            ));
            values.add(MessageFormat.format("(:contributor_role_{0})", i));
        }
        if (values.isEmpty()) {
            log.warn("Skipping bulk insert: no valid data provided");
            return 0;
        }
        String insertContributorRolesSQL = String.format(INSERT_CONTRIBUTOR_ROLES_SQL_FORMAT, company, String.join(", ",values));
        return template.update(insertContributorRolesSQL, new MapSqlParameterSource(params));
    }

    private static final String GET_CONTRIBUTOR_ROLES_SQL_FORMAT = "SELECT * FROM %s.org_users_contributor_roles";
    public List<String> getAllContributorRoles(final String company){
        String getContributorRolesSQL = String.format(GET_CONTRIBUTOR_ROLES_SQL_FORMAT, company);
        List<String> result = template.query(getContributorRolesSQL,((rs, rowNum) -> rs.getString("contributor_role")));
        return result;
    }

}
