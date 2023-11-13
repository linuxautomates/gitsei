package io.levelops.commons.databases.services.organization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.dashboard.OUDashboard;
import io.levelops.commons.databases.models.database.organization.*;
import io.levelops.commons.databases.models.database.organization.OrgVersion.OrgAssetType;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.utils.DatabaseUtils;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.web.exceptions.BadRequestException;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Objects;

@Service
@Log4j2
public class OrgUnitsDatabaseService extends DatabaseService<DBOrgUnit> {
    public final static String OUS_TABLE_NAME = "ous";

    public final static String OU_CATEGORIES_TABLE_NAME = "ou_categories";
    public final static String OU_MANAGERS_TABLE_NAME = "ou_managers";
    public final static String OU_MEMBERSHIP_TABLE_NAME = "ou_membership";
    public final static String OU_CONTENT_SECTION_TABLE_NAME = "ou_content_sections";
    public final static String ORG_USERS_TABLE_NAME = "org_users";
    public final static String ORG_VERSION_COUNTER = "org_version_counter";
    public final static String OU_GROUPS_MAPPING_TABLE_NAME = "ou_groups_ous_mapping";
    public final static String OU_CATEGORY_TABLE_NAME = "ou_categories";
    public final static String OUS_DASHBOARD_MAPPING_TABLE_NAME = "ous_dashboard_mapping";
    public final static String DASHBOARDS_TABLE_NAME = "dashboards";
    public final static String PROPLEO_USERS_TABLE_NAME = "users";
    public final static String PROPLEO_USERS_MANAGED_OUS_TABLE_NAME = "user_managed_ous";
    private final String SPECIAL_CHARACTERS = "[!@#$%^&*()_+=\\[\\]{}|;':\"<>,.?-]";
    private final String ESCAPED_CHARS = "\\\\$0";


    private final NamedParameterJdbcTemplate template;
    private final TagItemDBService tagItemService;
    private final OrgUsersDatabaseService usersService;
    private final OrgVersionsDatabaseService versionsService;
    private final DashboardWidgetService dashboardWidgetService;
    private final ObjectMapper mapper;

    private final static List<String> ddl = List.of(
            "CREATE TABLE IF NOT EXISTS {0}.{1} (" + // ous
                    "    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                    "    ref_id              INTEGER NOT NULL," +
                    "    name                VARCHAR(100) NOT NULL," +
                    "    description         VARCHAR(100)," +
                    "    parent_ref_id       INTEGER," +
                    "    tag_ids             INTEGER[]," +
                    "    versions            INTEGER[] NOT NULL," +
                    "    active              BOOL NOT NULL DEFAULT false," +
                    "    path                VARCHAR(500)," +
                    "    ou_category_id      UUID REFERENCES {0}.{8}(id) ON DELETE SET NULL," +
                    "    default_dashboard_id INTEGER REFERENCES {0}.{7}(id) ON DELETE SET NULL," +
                    "    created_at          TIMESTAMP NOT NULL DEFAULT (now() at time zone ''UTC'')," +
                    "    updated_at          TIMESTAMP NOT NULL DEFAULT (now() at time zone ''UTC'')" +
                    ");",

            // "CREATE UNIQUE INDEX IF NOT EXISTS {1}_name_idx ON {0}.{1}(name)", // already provided by the constrain
            "CREATE UNIQUE INDEX IF NOT EXISTS {1}_ref_id_versions_uniq_idx ON {0}.{1}(ref_id,versions)",
            "CREATE INDEX IF NOT EXISTS {1}_ref_id_versions_idx ON {0}.{1} USING GIN (ref_id, versions)",
            "CREATE INDEX IF NOT EXISTS {1}_ref_id_active_idx ON {0}.{1}(ref_id, active)",
            "CREATE INDEX IF NOT EXISTS {1}_active_tag_ids_at_idx ON {0}.{1}(active, tag_ids)",
            "CREATE INDEX IF NOT EXISTS {1}_created_at_idx ON {0}.{1}(created_at)",
            "CREATE INDEX IF NOT EXISTS {1}_updated_at_idx ON {0}.{1}(updated_at)",
            "CREATE INDEX IF NOT EXISTS {1}_path_idx ON {0}.{1}(path)",

            "CREATE TABLE IF NOT EXISTS {0}.{3} (" + // ou managers
                    "    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                    "    ou_id            UUID REFERENCES {0}.{1}(id) ON DELETE CASCADE," + // OUs
                    "    org_user_id      INTEGER NOT NULL," + // Org_Uers - the user should be marked as deleted only
                    "    UNIQUE(ou_id, org_user_id)" +
                    ");",

            "CREATE TABLE IF NOT EXISTS {0}.{4} (" + // ou_membership
                    "    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                    "    ou_id            UUID REFERENCES {0}.{1}(id) ON DELETE CASCADE," +
                    "    org_user_id      INTEGER NOT NULL," + // org users should be marked as deleted only
                    "    UNIQUE(ou_id, org_user_id)" +
                    ");",

            // content configuration. the user values are used to create membership entries.
            "CREATE TABLE IF NOT EXISTS {0}.{5} (\n" + // ou_content_section
                    "    id                            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                    "    ou_id                         UUID NOT NULL REFERENCES {0}.{1}(id) ON DELETE CASCADE,\n" +
                    "    integration_id                INTEGER REFERENCES {0}.integrations(id) ON DELETE CASCADE,\n" +
                    "    integration_filters           JSONB,\n" +
                    "    dynamic_users_definition      JSONB,\n" +
                    "    user_ref_ids                  INTEGER[],\n" +
                    "    default_section               BOOLEAN NOT NULL DEFAULT false,\n" +
                    "    UNIQUE(ou_id, integration_id)\n" +
                    ");\n",

            "CREATE TABLE IF NOT EXISTS {0}.{6} (" + // ous_dashboard_mapping
                    "    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                    "    ou_id            UUID REFERENCES {0}.{1}(id) ON DELETE CASCADE," +
                    "    dashboard_id     INTEGER REFERENCES {0}.{7}(id) ON DELETE CASCADE," +
                    "    dashboard_order INTEGER," +
                    "    UNIQUE(ou_id, dashboard_id)" +
                    ");"
    );

    //#region queries

    private final static String BASE_FROM =
            " FROM {0}.{1} ou\n" +
                    "    LEFT JOIN (SELECT ou_id, anyarray_uniq(array_agg(dashboard_id)) as dashboard_ids" +
                    "    FROM {0}.ous_dashboard_mapping GROUP BY ou_id) as odm ON  odm.ou_id=ou.id\n" +
                    "    LEFT JOIN {0}.ou_categories ouc ON ouc.id = ou.ou_category_id" +
                    "    {6}\n" +
                    "    {7} \n";

    private final static String BASE_SELECT =
            "SELECT ou.id, ou.ref_id, ou.name, ou.description, ou.parent_ref_id, ou.tag_ids, ou.active, ou.versions, ou.created_at, ou.updated_at, ou.path, ou.ou_category_id, ou.default_dashboard_id, ouc.workspace_id,\n" +
                    "    to_json( array(SELECT row_to_json(wfp) from(select vc.id, vc.name from {0}.ou_profiles op inner join {0}.velocity_configs vc on vc.id = op.profile_id  where ou_ref_id = ou.ref_id ) AS wfp)) as workflow_profile,\n"+
                    "    to_json(array(SELECT row_to_json(tp) from(select dp.id, dp.name from {0}.dev_productivity_profile_ou_mappings pm inner join {0}.dev_productivity_profiles dp on dp.id = pm.dev_productivity_profile_id  where ou_ref_id = ou.ref_id ) AS tp)) as trellis_profile,\n"+
                    "    to_json(ARRAY(SELECT row_to_json(m) FROM (SELECT u.id org_user_id, u.ref_id, u.full_name, u.email FROM {0}.{2} u, {0}.{3} oum WHERE oum.ou_id = ou.id AND u.ref_id = oum.org_user_id AND u.versions @> ARRAY(SELECT version FROM {0}.org_version_counter WHERE type = 'USER' AND active = true)) AS m)) managers,\n" +
                    "    to_json(ARRAY(SELECT row_to_json(a) FROM (SELECT pu.id user_id, concat(pu.firstname,'' '',pu.lastname) full_name, pu.email FROM {0}.{8} pu, {0}.{9} oua WHERE oua.ou_ref_id = ou.ref_id AND oua.user_id = pu.id and pu.usertype IN (''ORG_ADMIN_USER'',''ADMIN'') ) AS a)) admins,\n" +
                    "    to_json(ARRAY(SELECT row_to_json(m) FROM (SELECT u.id org_user_id, u.ref_id, u.full_name, u.email FROM {0}.{2} u, {0}.{4} oum WHERE oum.ou_id = ou.id AND u.ref_id = oum.org_user_id AND u.versions @> ARRAY(SELECT version FROM {0}.org_version_counter WHERE type = 'USER' AND active = true)) AS m)) members,\n" +
                    "    to_json(ARRAY(SELECT row_to_json(m) FROM (SELECT s.*, i.application integration_type, i.name integration_name FROM {0}.{5} s LEFT JOIN {0}.integrations i ON i.id = s.integration_id WHERE s.ou_id = ou.id) AS m))::text sections,\n" +
                    "    (select count(*) from {0}.ous_dashboard_mapping ousd where ousd.ou_id=ou.id)::int no_of_dashboards," +
                    "          odm.dashboard_ids " +
                    BASE_FROM + "\n";

    private final static String INSERT_OU_RELATIONSHIP_SQL_FORMAT = "INSERT INTO {0}.{1}(ou_id, org_user_id) VALUES(:ouId, :orgUserId) ON CONFLICT(ou_id, org_user_id) DO NOTHING";
    private final static String SELECT_OUS_FOR_OU_GROUP =
            "SELECT \n" +
                    "    ou_category_id,\n" +
                    "    anyarray_uniq(array_agg(ous.ref_id)) as ou_ref_ids \n" +
                    " FROM  \n" +
                    "    {0}.{1}  \n" +
                    "WHERE ou_category_id = :ou_category_id::uuid and active = true" +
                    " GROUP BY ou_category_id";
    private final static String SELECT_CHILDREN_RECURSIVE_BY_OU_REF_IDS =
            "WITH RECURSIVE children (ref_id) AS ( \n" +
                    "    SELECT ref_id\n" +
                    " FROM  \n" +
                    "    {0}.{1}  \n" +
                    "WHERE ref_id IN (:ou_ref_ids) and active = true\n" +
                    "UNION ALL \n" +
                    "SELECT ous.ref_id \n" +
                    "FROM {0}.{1} ous \n" +
                    "JOIN children c ON c.ref_id = ous.parent_ref_id) \n" +
                    "SELECT * FROM children";
    private final static String SELECT_FIRST_LEVEL_CHILD = "SELECT ref_id FROM {0}.{1} where parent_ref_id IN(:parent_ref_id) and active = true";
    private final static String INSERT_OU_SQL_FORMAT = "INSERT INTO {0}.{1}(ref_id, name, description, parent_ref_id, tag_ids, versions,path, ou_category_id, default_dashboard_id) VALUES({2}, :name, :description, :parentRefId, :tagIds, {3}, :path, :ou_category_id, :default_dashboard_id)";
    public final static String OU_GROUPS_OUS_MAPPING = "ou_groups_ous_mapping";
    private final static String DELETE_TEAMS_SQL_FORMAT =
            "DELETE FROM {0}.{1} as c WHERE {2}";
    private final static String INSERT_OUS_DASHBOARD_MAPPING = "INSERT INTO {0}.{1}(ou_id, dashboard_id, dashboard_order) " +
            "VALUES(:ou_id, :dashboard_id, :dashboard_order) ON CONFLICT(ou_id, dashboard_id) DO NOTHING";
    private static final Set<String> allowedFilters = Set.of("name", "ou_id", "ref_id", "parent_ref_id", "manager_id", "org_user_id", "integration_id", "username", "email", "version", "active", "path", "ou_group_id", "ou_category_id", "default_dashboard_id", "dashboard_id", "workspace_id");
    private static final int DEFAULT_PAGE_SIZE = 10;

    //#endregion

    public OrgUnitsDatabaseService(final DataSource dataSource, final ObjectMapper mapper,
                                   final TagItemDBService tagItemService,
                                   final OrgUsersDatabaseService usersService,
                                   final OrgVersionsDatabaseService versionsService,
                                   DashboardWidgetService dashboardWidgetService) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.mapper = mapper;
        this.tagItemService = tagItemService;
        this.usersService = usersService;
        this.versionsService = versionsService;
        this.dashboardWidgetService = dashboardWidgetService;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(OrgUsersDatabaseService.class, DashboardWidgetService.class, OrgUnitCategoryDatabaseService.class);
    }

    /**
     * Will generate a new version of the OU, either a new record or a new entry in the versions array
     * <p>
     * param company
     * param item The OU to be inserted
     */
    @Override
    public String insert(final String company, final DBOrgUnit item) throws SQLException {
        return insertForId(company, item).getRight().toString();
    }

    public Pair<UUID, Integer> insertForId(String company, DBOrgUnit item) throws SQLException {
        Validate.notBlank(item.getName(), "item.getName() cannot be null or empty.");
        // Get the next version - in query
        // process tags
        // insert OU record
        var refId = MessageFormat.format("(SELECT COALESCE(MAX(ref_id)+1,1) FROM {0}.{1})", company, OUS_TABLE_NAME);
        var versions = "'{1}'::integer[]";
        if (item.getRefId() > 0) {
            refId = String.valueOf(item.getRefId());
            versions = MessageFormat.format("ARRAY[(SELECT MAX(versions)+1 FROM (SELECT unnest(versions) versions FROM {0}.{1} WHERE ref_id = {2}) AS l)]", company, OUS_TABLE_NAME, String.valueOf(item.getRefId()));
        }
        var keyHolder = new GeneratedKeyHolder();

        Array tagIds = null;
        if (item.getTagIds() != null) {
            try (var conn = this.template.getJdbcTemplate().getDataSource().getConnection()) {
                tagIds = conn.createArrayOf("integer", item.getTagIds().toArray());
            }
        }
        Integer parentId = item.getParentRefId() != null ? item.getParentRefId() : null;
        Optional<DBOrgUnit> ouFromParentId = parentId != null ? get(company, parentId) : Optional.empty();
        String parentPath = StringUtils.EMPTY;
        Integer parentRefId = null;
        UUID parentOuCategoryId = null;
        UUID insertedOuCategoryId = item.getOuCategoryId() != null ? item.getOuCategoryId() : null;
        if (ouFromParentId.isPresent()) {
            parentPath = StringUtils.isNotEmpty(ouFromParentId.get().getPath()) ? ouFromParentId.get().getPath() : "/" + ouFromParentId.get().getName();
            parentRefId = ouFromParentId.get().getRefId();
            parentOuCategoryId = ouFromParentId.get().getOuCategoryId() != null ? ouFromParentId.get().getOuCategoryId() : null;

        }
        if (parentOuCategoryId != null && insertedOuCategoryId != null && !parentOuCategoryId.toString().equalsIgnoreCase(insertedOuCategoryId.toString())) {
            throwException(new BadRequestException("Parent and Child Org Units should belong to the same Org Unit Category"));
        }
        String path = StringUtils.isNotEmpty(parentPath) ? parentPath + "/" + item.getName() : "/" + item.getName();
        int count = this.template.update(
                MessageFormat.format(INSERT_OU_SQL_FORMAT, company, OUS_TABLE_NAME, refId, versions),
                new MapSqlParameterSource()
                        .addValue("name", item.getName())
                        .addValue("description", item.getDescription())
                        .addValue("parentRefId", parentRefId)
                        .addValue("tagIds", tagIds)
                        .addValue("path", path)
                        .addValue("ou_category_id", parentOuCategoryId == null ? insertedOuCategoryId : parentOuCategoryId)
                        .addValue("default_dashboard_id", (item.getDefaultDashboardId() != null && item.getDefaultDashboardId() != 0) ?
                                item.getDefaultDashboardId() : null),
                keyHolder,
                new String[]{"id", "ref_id"}
        );
        var id = count == 0 ? null : Pair.of((UUID) keyHolder.getKeys().get("id"), (Integer) keyHolder.getKeys().get("ref_id"));
        //insert into group mapping table
        if (id != null && id.getLeft() != null && id.getRight() != null) {
            //insert dashboard mapping
            // insertGroupDashBoardMapping(company, id.getLeft(), item.getDashboardIds());
            // insert content sections
            insertContentSections(company, id.getLeft(), item.getSections());
            // insert managers
            insertRelationship(company, id.getLeft(), item.getManagers(), OU_MANAGERS_TABLE_NAME);
            if (CollectionUtils.isEmpty(item.getSections())) {
                return id;
            }
        }
        // get static users ou-membership
        var staticUsers = item.getSections().stream()
                .filter(section -> CollectionUtils.isNotEmpty(section.getUsers()))
                .flatMap(section -> section.getUsers().stream())
                .map(rId -> {
                    try {
                        return usersService.get(company, rId);
                    } catch (SQLException e) {
                        log.error("[{}] Error getting active version of user with ref id '{}'", company, rId, e);
                        return Optional.<DBOrgUser>empty();
                    }
                })
                .filter(Optional::isPresent)
                .<OrgUserId>map(Optional::get);

        // get dynamic users ou-membership
        var dynamicUsers = item.getSections().stream()
                .filter(section -> MapUtils.isNotEmpty(section.getDynamicUsers()))
                .map(section -> QueryFilter.fromRequestFilters(section.getDynamicUsers()))
                .flatMap(filters -> {
                    try {
                        return usersService.filter(company, filters, 0, 100).getRecords().stream();
                    } catch (SQLException e) {
                        log.error("[{}] Error getting active version of user with ref id '{}'", company, filters, e);
                        return Stream.<DBOrgUser>empty();
                    }
                });

        var users = Stream.concat(staticUsers, dynamicUsers).collect(Collectors.toSet());
        // insert all users ou-membership
        if (id != null) {
            insertRelationship(company, id.getLeft(), users, OU_MEMBERSHIP_TABLE_NAME);
        }
        return id;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void throwException(Throwable t) throws T {
        throw (T) t;
    }

    private void insertContentSections(String company, UUID ouId, Set<DBOrgContentSection> sections) {
        if (CollectionUtils.isEmpty(sections)) {
            return;
        }
        var params = sections.stream()
                .map(section -> {
                    try (var conn = this.template.getJdbcTemplate().getDataSource().getConnection();) {
                        return new MapSqlParameterSource()
                                .addValue("ouId", ouId)
                                .addValue("integrationId", section.getIntegrationId())
                                .addValue("integrationFilters", ParsingUtils.serializeOrThrow(mapper, section.getIntegrationFilters(), null))
                                .addValue("dynamicUsersDefinition", ParsingUtils.serializeOrThrow(mapper, section.getDynamicUsers(), null))
                                .addValue("userRefIds", conn.createArrayOf("integer", SetUtils.emptyIfNull(section.getUsers()).toArray(new Integer[0])))
                                .addValue("default", section.getDefaultSection());
                    } catch (JsonProcessingException | SQLException e) {
                        log.error("[{}] Unable to persist section: {}", company, section, e);
                        return null;
                    }
                })
                .filter(map -> map != null)
                .collect(Collectors.toSet()).toArray(new MapSqlParameterSource[0]);
        this.template.batchUpdate(
                MessageFormat.format(
                        "INSERT INTO {0}.{1}(ou_id, integration_id, integration_filters, dynamic_users_definition, user_ref_ids, default_section) VALUES(:ouId, :integrationId, :integrationFilters::jsonb, :dynamicUsersDefinition::jsonb, :userRefIds::integer[], :default)",
                        company,
                        OU_CONTENT_SECTION_TABLE_NAME),
                params);
    }

    private void insertRelationship(final String company, final UUID ouId, final Set<OrgUserId> members, final String type) throws SQLException {
        if (CollectionUtils.isEmpty(members)) {
            return;
        }
        var params = members.stream().map(memberId -> new MapSqlParameterSource()
                        .addValue("ouId", ouId)
                        .addValue("orgUserId", memberId.getRefId()))
                .collect(Collectors.toSet()).toArray(new MapSqlParameterSource[0]);

        this.template.batchUpdate(MessageFormat.format(INSERT_OU_RELATIONSHIP_SQL_FORMAT, company, type), params);
    }

    private void insertGroupDashBoardMapping(final String company, final UUID ouId, final Set<Integer> dashBoards) throws SQLException {
        if (CollectionUtils.isEmpty(dashBoards)) {
            return;
        }
        var params = dashBoards.stream().map(id -> new MapSqlParameterSource()
                        .addValue("ou_id", ouId)
                        .addValue("dashboard_id", id)
                        .addValue("dashboard_order", null))
                .collect(Collectors.toSet()).toArray(new MapSqlParameterSource[0]);

        this.template.batchUpdate(MessageFormat.format(INSERT_OUS_DASHBOARD_MAPPING, company, OUS_DASHBOARD_MAPPING_TABLE_NAME), params);
    }


    /**
     * insert method for ou and dashboard mappings on changing the version of OU
     */
    public void insertOuDashboardMappings(String company, Pair<UUID, Integer> newOuId, UUID oldOuId) throws SQLException {
        List<OUDashboard> dashboardsIdsForOuId = getDashboardsIdsForOuId(company, oldOuId).getRecords();
        if (CollectionUtils.isEmpty(dashboardsIdsForOuId))
            return;
        List<OrgUnitDashboardMapping> orgUnitDashboardMappings = dashboardsIdsForOuId.stream()
                .map(dashboard -> OrgUnitDashboardMapping.builder()
                        .orgUnitId(newOuId.getLeft())
                        .dashboardId(dashboard.getDashboardId())
                        .dashboardOrder(dashboard.getDashboardOrder())
                        .build())
                .collect(Collectors.toList());
        batchInsertOuDashboardMappings(company, orgUnitDashboardMappings);
    }

    private void batchInsertOuDashboardMappings(String company, List<OrgUnitDashboardMapping> orgUnitDashboardMappings) {
        if (CollectionUtils.isEmpty(orgUnitDashboardMappings)) {
            return;
        }
        List<SqlParameterSource> parameterSources = orgUnitDashboardMappings.stream()
                .map(this::constructOUDashboardMappingParameterSource).collect(Collectors.toList());
        String insertOuDashboardMappingSql = MessageFormat.format(INSERT_OUS_DASHBOARD_MAPPING, company, OUS_DASHBOARD_MAPPING_TABLE_NAME);
        int[] updateCounts = template.batchUpdate(insertOuDashboardMappingSql, parameterSources.toArray(new SqlParameterSource[0]));
        log.debug("Insert OU Dashboard Mapping response {}", updateCounts);
    }

    private SqlParameterSource constructOUDashboardMappingParameterSource(OrgUnitDashboardMapping orgUnitDashboardMapping) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ou_id", orgUnitDashboardMapping.getOrgUnitId());
        params.addValue("dashboard_id", orgUnitDashboardMapping.getDashboardId());
        params.addValue("dashboard_order", orgUnitDashboardMapping.getDashboardOrder());
        return params;
    }

    /**
     * insert method for ou and dashboard mapping
     */
    public String insertOuDashboardMapping(String company, OrgUnitDashboardMapping orgUnitDashboardMapping) {
        var keyHolder = new GeneratedKeyHolder();
        int count = this.template.update(
                MessageFormat.format(INSERT_OUS_DASHBOARD_MAPPING, company, OUS_DASHBOARD_MAPPING_TABLE_NAME),
                new MapSqlParameterSource()
                        .addValue("ou_id", orgUnitDashboardMapping.getOrgUnitId())
                        .addValue("dashboard_id", orgUnitDashboardMapping.getDashboardId())
                        .addValue("dashboard_order", orgUnitDashboardMapping.getDashboardOrder()),
                keyHolder,
                new String[]{"ou_id", "dashboard_id"}
        );
        var id = count == 0 ? null : Pair.of((UUID) keyHolder.getKeys().get("ou_id"), (Integer) keyHolder.getKeys().get("dashboard_id"));
        if (id != null) {
            return id.getLeft().toString();
        }
        return null;
    }

    @Override
    public Boolean update(String company, DBOrgUnit unit) throws SQLException {
        throw new NotImplementedException();
    }

    public Boolean update(String company, UUID id, boolean active) throws SQLException {
        this.template.update(MessageFormat.format("UPDATE {0}.{1} SET active = :active, updated_at = (now() at time zone ''UTC'') WHERE id = :id", company, OUS_TABLE_NAME), new MapSqlParameterSource().addValue("active", active).addValue("id", id));
        return true;
    }

    public Boolean updateNamePathForOu(String company, UUID id, String name) {
        if (StringUtils.isEmpty(name))
            return false;
        this.template.update(MessageFormat.format("UPDATE {0}.{1} SET  path = regexp_replace(path, :name, (select name from {0}.{1} where id = :id)) ,updated_at = (now() at time zone ''UTC'')", company, OUS_TABLE_NAME),
                new MapSqlParameterSource().addValue("id", id).addValue("name", name));
        return true;
    }

    @Override
    public Optional<DBOrgUnit> get(String company, String ouId) throws SQLException {
        return get(company, UUID.fromString(ouId));
    }

    public Optional<DBOrgUnit> get(String company, UUID ouId) throws SQLException {
        if (ouId == null) {
            return Optional.empty();
        }
        var results = filter(company, QueryFilter.builder().strictMatch("ou_id", ouId).build(), 0, 1);
        if (results.getTotalCount() > 0) {
            return Optional.of(results.getRecords().get(0));
        }
        return Optional.empty();
    }

    public Optional<DBOrgUnit> get(String company, Integer refId) throws SQLException {
        return get(company, refId, null);
    }

    public Optional<DBOrgUnit> get(String company, Integer refId, final Boolean active) throws SQLException {
        return get(company, refId, null, active);
    }

    public Optional<DBOrgUnit> get(final String company, final int refId, final Integer version) throws SQLException {
        return get(company, refId, version, null);
    }

    public Optional<DBOrgUnit> get(final String company, final int refId, final Integer version, final Boolean active) throws SQLException {
        var filterBuilder = QueryFilter.builder().strictMatch("ref_id", refId);
        if (version != null) {
            filterBuilder.strictMatch("version", version);
        }
        if (active != null) {
            filterBuilder.strictMatch("active", active);
        }
        var results = filter(company, filterBuilder.build(), 0, 1);
        if (results.getTotalCount() > 0) {
            return Optional.of(results.getRecords().get(0));
        }
        return Optional.empty();
    }

    /**
     * For a given ouId, retrieves the list of dashboards related.
     */
    public DbListResponse<OUDashboard> getDashboardsIdsForOuId(String company, UUID ouId) {
        String SQL = "SELECT d.id,d.name,ousd.dashboard_order,ousd.ou_id FROM " + company + ".dashboards d ";
        String ouMappingJoin = "INNER JOIN " + company + ".ous_dashboard_mapping ousd on d.id=ousd.dashboard_id ";
        String whereClause = " WHERE ousd.ou_id = :ou_id::uuid";
        SQL = SQL + ouMappingJoin + whereClause;
        Map<String, Object> params = Map.of("ou_id", ouId);
        log.info("query {}", SQL);
        log.info("params {}", params);
        List<OUDashboard> ouDashboards = template.query(SQL, params, (rs, rowNumber) -> OUDashboard.builder()
                .dashboardId(rs.getInt("id"))
                .name(rs.getString("name"))
                .ouId((UUID) rs.getObject("ou_id"))
                .dashboardOrder(rs.getInt("dashboard_order"))
                .build());
        return DbListResponse.of(ouDashboards, ouDashboards.size());
    }

    /**
     * For a given ou paths, retrieves the list of related Ou Ids.
     */
    public List<UUID> getOuIdsForGivenPath(String company, List<String> paths, UUID ouCategoryId) {
        String selectSql = "SELECT ou.id\n" +
                "FROM {0}.{1} ou\n" +
                "WHERE active = true\n" +
                "AND path IN (:paths)\n" +
                "AND ou_category_id = :ou_category_id";
        var sql = MessageFormat.format(selectSql, company, OUS_TABLE_NAME);
        var params = new MapSqlParameterSource();
        log.info("SQL for getting Ou Ids for a Given Path " + sql);
        params.addValue("paths", paths);
        if (ouCategoryId != null)
            params.addValue("ou_category_id", ouCategoryId);
        return template.query(sql, params, (rs, row) -> (UUID) rs.getObject("id"));
    }

    private static boolean columnPresent(ResultSet rs, String column) {
        boolean isColumnPresent = false;
        try {
            rs.findColumn(column);
            if (ObjectUtils.isNotEmpty(rs.getObject(column))) {
                isColumnPresent = true;
            }
        } catch (SQLException e) {
            isColumnPresent = false;
        }
        return isColumnPresent;
    }


    @Override
    public DbListResponse<DBOrgUnit> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return filter(company, null, pageNumber, pageSize);
    }

    public List<UUID> getActiveMembersByOuId(final String company,
                                             final UUID ouId,
                                             final Integer pageNumber,
                                             Integer pageSize) {
        String selectSql = "SELECT U.id\n" +
                "FROM {0}.{1} U,\n" +
                "{0}.{2} OUM WHERE OUM.OU_ID = :ou_id AND U.REF_ID = OUM.ORG_USER_ID AND U.ACTIVE = 'true' AND U.VERSIONS @> ARRAY\n" +
                "(SELECT VERSION\n" +
                "FROM {0}.{3}\n" +
                "WHERE TYPE = 'USER'\n" +
                "AND ACTIVE = TRUE)";

        var sql = MessageFormat.format(selectSql, company, ORG_USERS_TABLE_NAME, OU_MEMBERSHIP_TABLE_NAME, ORG_VERSION_COUNTER);
        sql = sql.replaceAll("WHERE TYPE = USER", "WHERE TYPE = 'USER'");
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        String orderBy = " ORDER BY U.REF_ID";
        String limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize.toString(), String.valueOf(pageNumber * pageSize));
        var params = new MapSqlParameterSource();
        params.addValue("ou_id", ouId);
        List<UUID> orgUserIds = template.query(sql + orderBy + limit, params, (rs, row) -> {
            return (UUID) rs.getObject("id");
        });
        return orgUserIds;
    }

    public DbListResponse<DBOrgUnit> filter(
            final String company,
            final QueryFilter filters,
            final Integer pageNumber,
            Integer pageSize) throws SQLException {
        var params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();

        populateConditions(company, filters, conditions, params);

        var extraConditions = conditions.size() > 0 ? " WHERE " + String.join(" AND ", conditions) + " " : "";
        var extraFrom = "";

        var sqlBase = MessageFormat.format(BASE_SELECT, company, OUS_TABLE_NAME, ORG_USERS_TABLE_NAME, OU_MANAGERS_TABLE_NAME, OU_MEMBERSHIP_TABLE_NAME, OU_CONTENT_SECTION_TABLE_NAME, extraFrom, extraConditions, PROPLEO_USERS_TABLE_NAME, PROPLEO_USERS_MANAGED_OUS_TABLE_NAME);
        sqlBase = sqlBase.replaceAll("WHERE type = USER AND active = true"," WHERE type = 'USER' AND active = true ");

        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        String limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize.toString(), String.valueOf(pageNumber * pageSize));
        String sql = sqlBase + limit;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DBOrgUnit> items = template.query(sql, params, (rs, row) -> {
            List<Map<String, Object>> managers = ParsingUtils.parseJsonList(mapper, "managers", rs.getString("managers"));
            List<Map<String, Object>> admins = ParsingUtils.parseJsonList(mapper, "admins", rs.getString("admins"));
            List<Map<String, Object>> members = ParsingUtils.parseJsonList(mapper, "members", rs.getString("members"));
            Set<DBOrgContentSection> sections = ParsingUtils.parseSet(mapper, "sections", DBOrgContentSection.class, rs.getString("sections"));
            Set<Integer> tagIds = ParsingUtils.parseSet("tag_ids", Integer.class, rs.getArray("tag_ids"));
            List<Map<String,Object>> workflowProfile= ParsingUtils.parseJsonList(mapper, "workFlowProfile", rs.getString("workflow_profile"));
            List<Map<String,Object>> trellisProfile= ParsingUtils.parseJsonList(mapper, "trellisProfile", rs.getString("trellis_profile"));

            return DBOrgUnit.builder()
                    .id((UUID) rs.getObject("id"))
                    .refId(rs.getInt("ref_id"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .parentRefId(columnPresent(rs, "parent_ref_id") ? rs.getInt("parent_ref_id") : null)
                    .tags(null)
                    .tagIds(tagIds)
                    .versions(DatabaseUtils.fromSqlArray(rs.getArray("versions"), Integer.class).collect(Collectors.toSet()))
                    .active(rs.getBoolean("active"))
                    .path(rs.getString("path"))
                    .ouCategoryId(columnPresent(rs, "ou_category_id") ? (UUID) rs.getObject("ou_category_id") : null)
                    .noOfDashboards(rs.getInt("no_of_dashboards"))
                    .createdAt(rs.getTimestamp("created_at").toInstant())
                    .updatedAt(rs.getTimestamp("updated_at").toInstant())
                    .defaultDashboardId(rs.getInt("default_dashboard_id"))
                    .workspaceId(rs.getInt("workspace_id"))
                    .managers(managers.stream()
                            .map(item -> {
                                return OrgUserId.builder()
                                        .id(UUID.fromString(item.get("org_user_id").toString()))
                                        .refId((Integer.valueOf(item.get("ref_id").toString())))
                                        .fullName(item.get("full_name").toString())
                                        .email(item.get("email").toString())
                                        .build();
                            })
                            .collect(Collectors.toSet()))
                    .admins(admins.stream()
                            .map(item -> {
                                return PropeloUserId.builder()
                                        .userId((Integer.valueOf(item.get("user_id").toString())))
                                        .fullName(item.get("full_name").toString())
                                        .email(item.get("email").toString())
                                        .build();
                            })
                            .collect(Collectors.toSet()))
                    .sections(sections)
                    // .members(members.stream()
                    //     .map(item -> OrgUserId.builder()
                    //         .id(UUID.fromString(item.get("team_member_id").toString()))
                    //         .fullName(item.get("full_name").toString())
                    //         .email(item.get("email").toString())
                    //         .build())
                    //     .collect(Collectors.toSet()))
                    .workflowProfileId(CollectionUtils.isNotEmpty(workflowProfile) ? UUID.fromString(workflowProfile.get(0).get("id").toString()) : null)
                    .trellisProfileId(CollectionUtils.isNotEmpty(trellisProfile) ? UUID.fromString(trellisProfile.get(0).get("id").toString()) : null)
                    .workflowProfileName(CollectionUtils.isNotEmpty(workflowProfile) ? workflowProfile.get(0).get("name").toString() : null)
                    .trellisProfileName(CollectionUtils.isNotEmpty(trellisProfile) ? trellisProfile.get(0).get("name").toString() : null)
                    .build();
        });
        Integer total = null;
        if (filters == null || BooleanUtils.isNotFalse(filters.getCount())) { // true by default for backward compatibility
            String countSQL = "SELECT COUNT(*) FROM (" + sqlBase + ") as l";
            log.info("sql = " + countSQL); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            total = template.queryForObject(countSQL, params, Integer.class);
        }
        return DbListResponse.of(items, total);
    }

    public DbListResponse<Map<String, Object>> getValues(final String company, String field, final QueryFilter filters, final Integer pageNumber, Integer pageSize) {
        var params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        switch (field) {
            case "name":
            case "path":
                populateConditions(company, filters, conditions, params);

                var extraConditions = conditions.size() > 0 ? " WHERE " + String.join(" AND ", conditions) + " " : "";

                var prefix = "ou";
                var sql = MessageFormat.format("SELECT DISTINCT({2}.{3}) AS v " + BASE_FROM, company, OUS_TABLE_NAME, prefix, field, "", "", "", extraConditions, OU_GROUPS_MAPPING_TABLE_NAME);
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
                return DbListResponse.of(List.of(), 0);
        }
    }

    private void populateConditions(
            final String company,
            final QueryFilter filters,
            final @NonNull List<String> conditions,
            final @NonNull MapSqlParameterSource params) {
        if (filters != null && MapUtils.isNotEmpty(filters.getPartialMatches()) && filters.getPartialMatches().containsKey("name")) {
            conditions.add(MessageFormat.format("ou.name ILIKE ''%{0}%''", filters.getPartialMatches().get("name")));
        }
        if (filters != null && MapUtils.isNotEmpty(filters.getPartialMatches()) && filters.getPartialMatches().containsKey("path")) {
            conditions.add(MessageFormat.format("ou.path ILIKE ''%{0}%''", filters.getPartialMatches().get("path")));
        }
        if (filters == null || MapUtils.isEmpty(filters.getStrictMatches()) || !(filters.getStrictMatches().containsKey("active")
                || filters.getStrictMatches().containsKey("version") || filters.getStrictMatches().containsKey("ou_id"))) {
            conditions.add("active = true");
        }
        if (filters == null || filters.getStrictMatches() == null || filters.getStrictMatches().isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : filters.getStrictMatches().entrySet()) {
            var item = entry.getKey();
            var values = entry.getValue();
            if (!allowedFilters.contains(item)
                    || (filters.getStrictMatches().get(item) == null &&
                    !item.equalsIgnoreCase("parent_ref_id"))) {
                continue;
            }
            var prefix = "";
            switch (item) {
                case "ou_id":
                    prefix = "ou";
                    processCondition(prefix, "id", values, conditions, params);
                    continue;
                case "name":
                case "ref_id":
                case "version":
                case "path":
                    prefix = "ou";
                    break;
                case "active":
                    prefix = "ou";
                    processCondition(prefix, "active", values, conditions, params);
                    continue;
                case "parent_ref_id":
                    prefix = "ou";
                    processCondition(prefix, "parent_ref_id", values, conditions, params);
                    continue;
                case "manager_id":
                    prefix = "ouma";
                    processCondition(prefix, "org_user_id", values, conditions, params);
                    continue;
                case "org_user_id":
                    prefix = "oume";
                    processCondition(prefix, "org_user_id", values, conditions, params);
                    continue;
                case "integration_id":
                case "username":
                    prefix = "iu";
                    break;
                case "email":
                    prefix = "u";
                    break;
                case "default_dashboard_id":
                    prefix = "ou";
                    processCondition(prefix, "default_dashboard_id", values, conditions, params);
                    continue;
                case "ou_category_id":
                    prefix = "ou";
                    processCondition(prefix, "ou_category_id", values, conditions, params);
                    continue;
                case "dashboard_id":
                    prefix = "odm";
                    processCondition(prefix, "dashboard_id", values, conditions, params);
                    continue;
                case "workspace_id":
                    prefix = "ouc";
                    processCondition(prefix, "workspace_id", values, conditions, params);
                    continue;
                default:
                    continue;
            }
            processCondition(prefix, item, values, conditions, params);
        }
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
            try (var conn = this.template.getJdbcTemplate().getDataSource().getConnection()) {
                params.addValue("version", conn.createArrayOf("integer", new Integer[]{(Integer) values}));
            } catch (SQLException e) {
                log.error(e);
            }
            return;
        }
        if (item.equalsIgnoreCase("dashboard_id")) {
            conditions.add(MessageFormat.format(":{1}::integer =  ANY({0}.dashboard_ids)", prefix, item));
            params.addValue(item, values.toString());
            return;
        }
        if (item.equalsIgnoreCase("parent_ref_id")) {
            if (values == null) {
                conditions.add(MessageFormat.format("{0}.{1} IS NULL", prefix, item));
                return;
            }
            conditions.add(MessageFormat.format("{0}.{1} = :{1}::int", prefix, item));
            params.addValue(item, values.toString());
            return;
        }
        if (values instanceof Collection) {
            addCollectionCond(prefix, item, (Collection<Object>) values, conditions);
            return;
        }
        if (values instanceof UUID || item.equals("id")) {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}::uuid", prefix, item));
        } else if (values instanceof Integer) {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}::int", prefix, item));
        } else if (values instanceof Long) {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}::bigint", prefix, item));
        } else if (values instanceof Boolean) {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}::boolean", prefix, item));
        } else {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}", prefix, item));
        }
        params.addValue(item, values.toString());
    }


    private void addCollectionCond(String prefix, String item, Collection<Object> values, @NonNull List<String> conditions) {
        var collection = values
                .stream()
                .filter(ObjectUtils::isNotEmpty)
                .map(Object::toString)
                .collect(Collectors.toSet());
        var tmp = MessageFormat.format("{0}.{1} = ANY({2})", prefix, item, "'{" + String.join(",", collection) + "}'");
        log.debug("filter: {}", tmp);
        conditions.add(tmp);
    }


    /**
     * For a given ou_group_id, retrieves the list of org units related.
     */
    public DbListResponse<DBOrgUnit> getOusForGroupId(String company, UUID ouGroupId) {
        List<DBOrgUnit> dbOrgUnits = new ArrayList<>();
        var sqlBase = MessageFormat.format(SELECT_OUS_FOR_OU_GROUP, company, OUS_TABLE_NAME, ouGroupId.toString());
        List<List<Integer>> query = template.query(sqlBase, new MapSqlParameterSource().addValue("ou_category_id", ouGroupId), (rs, row) ->
        {
            List<Integer> ouIds = columnPresent(rs, "ou_ref_ids") ? Arrays.stream(((Integer[]) rs.getArray("ou_ref_ids").getArray()))
                    .collect(Collectors.toList()) : List.of();
            return CollectionUtils.isNotEmpty(ouIds) ? ouIds : null;
        });
        if (CollectionUtils.isNotEmpty(query) && query.get(0) != null) {
            List<Integer> ouIds = query.get(0);
            ouIds.forEach(ouId -> {
                try {
                    dbOrgUnits.add(get(company, ouId).orElse(DBOrgUnit.builder().build()));
                } catch (SQLException e) {
                    log.info("SQL Exception thrown at OrgUnitGroupsDatabaseService " + e);
                }
            });
        }

        return DbListResponse.of(dbOrgUnits, dbOrgUnits.size());
    }

    public Set<Integer> getAllChildrenRefIdsRecursive(String company, List<Integer> ouRefIds) {
        var sqlBase = MessageFormat.format(SELECT_CHILDREN_RECURSIVE_BY_OU_REF_IDS, company, OUS_TABLE_NAME, ouRefIds);
        List<Integer> children = template.query(sqlBase, new MapSqlParameterSource().addValue("ou_ref_ids", ouRefIds), (rs, row) ->
        {
            return columnPresent(rs, "ref_id") ? rs.getInt("ref_id") : null;
        });
        return children.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return delete(company, UUID.fromString(id));
    }

    public Boolean delete(String company, UUID id) throws SQLException {
        return delete(company, Set.of(id));
    }

    public Boolean delete(String company, Set<UUID> ids) throws SQLException {
        var conditions = new ArrayList<String>();
        var params = new MapSqlParameterSource();
        processCondition("c", "id", ids, conditions, params);
        var sql = MessageFormat.format(DELETE_TEAMS_SQL_FORMAT, company, OUS_TABLE_NAME, String.join(" AND ", conditions));
        var updates = this.template.update(sql, params);
        if (ids.size() != updates) {
            log.warn("Request to delete {} records ended up deleting {} records (by ids)", ids.size(), updates);
        }
        return true;
    }

    public Set<Integer> getFirstLevelChildrenRefIds(String company, List<Integer> ouRefIds) {
        var sqlBase = MessageFormat.format(SELECT_FIRST_LEVEL_CHILD, company, OUS_TABLE_NAME, ouRefIds);
        List<Integer> children = template.query(sqlBase, new MapSqlParameterSource().addValue("parent_ref_id", ouRefIds), (rs, row) ->
        {
            return columnPresent(rs, "ref_id") ? rs.getInt("ref_id") : null;
        });
        return children.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public String updatePathAndOuCategoryId(String company,
                                            String updatedPath,
                                            String existingPath,
                                            int refId,
                                            boolean isUpdateOperation,
                                            UUID ouCategoryId,
                                            boolean hasCategoryChanged, Integer workspaceId) {
        return this.template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            int i = 0;
            String refIdCond = isUpdateOperation ? " AND ref_id != ? " : StringUtils.EMPTY;
            String tempPath = existingPath;
            if(existingPath.chars().mapToObj(c -> (char) c).anyMatch(c -> SPECIAL_CHARACTERS.indexOf(c) >= 0)){
                tempPath = existingPath.replaceAll(SPECIAL_CHARACTERS, ESCAPED_CHARS);
            }
            String existingPath_ = tempPath;
            String categoryIdCondition = (!hasCategoryChanged && isUpdateOperation) ? "AND ou_category_id = ?" : " AND ou_category_id IN (SELECT id FROM {0}.{2} WHERE workspace_id = ? ) ";
            String updatePathSql = MessageFormat.format("UPDATE {0}.{1} SET path = regexp_replace(path, ?, ?), updated_at = (now() at time zone ''UTC'') WHERE  active = 'true' " + categoryIdCondition + " " + refIdCond, company, OUS_TABLE_NAME, OU_CATEGORIES_TABLE_NAME);
            String updateCategoryIdSql = MessageFormat.format("UPDATE {0}.{1} SET ou_category_id = ?, updated_at = (now() at time zone ''UTC'') " +
                    "WHERE path LIKE ? AND ou_category_id IN (SELECT id FROM {0}.{2} WHERE workspace_id = ? )", company, OUS_TABLE_NAME, OU_CATEGORIES_TABLE_NAME);
            String intrSql = isUpdateOperation ? "(SELECT parent_ref_id from {0}.{1} where ref_id = ? and versions = (select MAX(versions) from {0}.{1} where ref_id = ?))" : "null";
            String whereCondStrForRefId = isUpdateOperation ? "WHERE ref_id = ?" : "WHERE parent_ref_id = ?";
            String updateParentRefIdSql = MessageFormat.format("UPDATE {0}.{1} SET parent_ref_id = " + intrSql + ", updated_at = (now() at time zone ''UTC'') " + whereCondStrForRefId, company, OUS_TABLE_NAME);
            try (PreparedStatement updatePath = conn.prepareStatement(updatePathSql, Statement.RETURN_GENERATED_KEYS)) {
                // This is not a great implementation. Ideally we should be traversing through the entire tree and
                // appropriately setting the path. This works for now, but there may be subtle edge cases.
                updatePath.setObject(++i, existingPath_);
                updatePath.setObject(++i, updatedPath);
                if (!hasCategoryChanged && isUpdateOperation)
                    updatePath.setObject(++i, ouCategoryId);
                else
                    updatePath.setObject(++i, workspaceId);
                if (isUpdateOperation)
                    updatePath.setObject(++i, refId);
                updatePath.executeUpdate();
            }
            try (PreparedStatement updateParentRefId = conn.prepareStatement(updateParentRefIdSql, Statement.RETURN_GENERATED_KEYS)) {
                i = 0;
                updateParentRefId.setObject(++i, refId);
                if (isUpdateOperation) {
                    updateParentRefId.setObject(++i, refId);
                    updateParentRefId.setObject(++i, refId);
                }
                updateParentRefId.executeUpdate();
            }
            if (isUpdateOperation && hasCategoryChanged) {
                try (PreparedStatement updateCategoryId = conn.prepareStatement(updateCategoryIdSql, Statement.RETURN_GENERATED_KEYS)) {
                    i = 0;
                    updateCategoryId.setObject(++i, ouCategoryId);
                    updateCategoryId.setObject(++i, "%" + updatedPath + "%");
                    updateCategoryId.setObject(++i, workspaceId);
                    updateCategoryId.executeUpdate();
                }
            }
            return null;
        }));
    }

    public void upgradeUnitVersion(final String company, final Integer refId) throws SQLException {
        // upgradeUnitVersion(company, versionId, Set.of());
    }

    public void upgradeUnitVersion(final String company, final UUID versionId) throws SQLException {
        upgradeUnitVersion(company, versionId, Set.of());
    }

    public void upgradeUnitVersion(final String company, final UUID versionId, Set<Integer> exclusions) throws SQLException {
        var newVersion = versionsService.get(company, versionId).get();
        var currentVersion = versionsService.getActive(company, OrgAssetType.USER).get();
        upgradeUnitVersion(company, newVersion.getVersion(), currentVersion.getVersion(), exclusions);
    }

    public void upgradeUnitVersion(final String company, final Integer newVersion, final Integer currentVersion) throws SQLException {
        upgradeUnitVersion(company, newVersion, currentVersion, Set.of());
    }

    public void upgradeUnitVersion(final String company, final Integer newVersion, final Integer currentVersion, Set<Integer> exclusions) throws SQLException {
        var exclusionsQ = CollectionUtils.isEmpty(exclusions) ? "" : "AND ref_id NOT IN (:excludedRefIds)";
        try (var conn = this.template.getJdbcTemplate().getDataSource().getConnection();) {
            this.template.update(
                    MessageFormat.format("UPDATE {0}.{1} SET versions = versions || :newVersion, updated_at = (now() at time zone ''UTC'') WHERE versions @> :currentVersion {2}", company, ORG_USERS_TABLE_NAME, exclusionsQ),
                    new MapSqlParameterSource().addValue("newVersion", newVersion)
                            .addValue("currentVersion", conn.createArrayOf("integer", new Integer[]{currentVersion}))
                            .addValue("excludedRefIds", exclusions));
        }
    }

    public Set<OrgVersion> getVersions(final String company, final Integer refId) {
        var results = this.template.query(
                MessageFormat.format("SELECT id, UNNEST(versions) v, created_at, updated_at, active FROM {0}.{1} WHERE ref_id = :refId;", company, OUS_TABLE_NAME),
                new MapSqlParameterSource().addValue("refId", refId),
                (rs, row) -> {
                    return OrgVersion.builder()
                            .id((UUID) rs.getObject("id"))
                            .version(rs.getInt("v"))
                            .createdAt(rs.getTimestamp("created_at").toInstant())
                            .updatedAt(rs.getTimestamp("updated_at").toInstant())
                            .active(rs.getBoolean("active"))
                            .build();
                });
        return Set.copyOf(results);
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        ddl.forEach(item ->
                template.getJdbcTemplate()
                        .execute(MessageFormat.format(
                                item,
                                company,
                                OUS_TABLE_NAME,
                                ORG_USERS_TABLE_NAME,
                                OU_MANAGERS_TABLE_NAME,
                                OU_MEMBERSHIP_TABLE_NAME,
                                OU_CONTENT_SECTION_TABLE_NAME,
                                OUS_DASHBOARD_MAPPING_TABLE_NAME,
                                DASHBOARDS_TABLE_NAME,
                                OU_CATEGORY_TABLE_NAME
                        )));
        return true;
    }

}
