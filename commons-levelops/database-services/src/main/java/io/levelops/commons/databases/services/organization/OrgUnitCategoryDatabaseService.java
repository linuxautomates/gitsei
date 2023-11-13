package io.levelops.commons.databases.services.organization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityUserIds;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.utils.DatabaseUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class OrgUnitCategoryDatabaseService extends DatabaseService<OrgUnitCategory> {

    private final NamedParameterJdbcTemplate template;
    private final OrgUnitHelper unitsHelper;
    private final ObjectMapper objectMapper;

    public final static String OUS_CATEGORY_TABLE_NAME = "ou_categories";
    public final static String WORKSPACES_TABLE_NAME = "products";
    public final static String OUS_TABLE_NAME = "ous";
    private static final Set<String> allowedFilters = Set.of("name", "id", "is_predefined", "enabled", "workspace_id");
    private static final int DEFAULT_PAGE_SIZE = 10;
    private final static List<String> ddl = List.of(
            "CREATE TABLE IF NOT EXISTS {0}.{1} (" + // ou_category
                    "    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                    "    name                VARCHAR(100) NOT NULL," +
                    "    description         VARCHAR(100)," +
                    "    is_predefined       boolean NOT NULL DEFAULT false ," +
                    "    enabled             boolean NOT NULL DEFAULT false ," +
                    "    workspace_id        INTEGER REFERENCES {0}.{2}(id) ON DELETE SET NULL," +
                    "    created_at          TIMESTAMP NOT NULL DEFAULT (now() at time zone ''UTC'')," +
                    "    updated_at          TIMESTAMP NOT NULL DEFAULT (now() at time zone ''UTC'')" +
                    ");",
            "CREATE UNIQUE INDEX IF NOT EXISTS {1}_name_workspace_id_category_idx ON {0}.{1}(name, workspace_id)"

    );
    private final static String INSERT_OU_GROUP = "INSERT INTO {0}.{1}(name, description, is_predefined, enabled, workspace_id, created_at, updated_at) " +
            "VALUES(:name, :description, :is_predefined, :enabled, :workspace_id,:created_at, now())" +
            " ON CONFLICT (name, workspace_id) DO UPDATE SET (description, is_predefined, enabled, updated_at)=" +
            "(EXCLUDED.description, EXCLUDED.is_predefined, EXCLUDED.enabled, now()) RETURNING *, (xmax=0) AS inserted";
    private final static String UPDATE_OU_GROUP = "UPDATE {0}.{1} " +
            " SET name = :name, description= :description, is_predefined = :is_predefined, enabled = :enabled, workspace_id  = :workspace_id, " +
            " updated_at = now() where id = :id::uuid";

    private final static String DELETE_OU_GROUPS_SQL_FORMAT =
            "DELETE FROM {0}.{1} as c WHERE {2}";

    private final static String SELECT_OU_COUNT_OU_GROUP =
            "(SELECT \n" +
                    "    ou_category_id,\n" +
                    "    anyarray_uniq(array_agg(ous.ref_id)) as ou_ref_ids \n" +
                  //  "    array_length(anyarray_uniq(array_agg(ous.ref_id)),1) as ou_ids_count \n" +
                    " FROM  \n" +
                    "    {0}.{3}  \n" +
                    " WHERE active = true" +
                    " GROUP BY ou_category_id) as a ON a.ou_category_id = ou_category.id";

    private final static String BASE_SELECT =
            "SELECT \n" +
                    "    ou_category.id," +
                    "    ou_category.name,\n" +
                    "    ou_category.description,\n" +
                    "    ou_category.is_predefined,\n" +
                    "    ou_category.enabled,\n" +
                    "    ou_category.workspace_id,\n" +
                    "    ou_category.created_at,\n" +
                    "    ou_category.updated_at,\n" +
                    "    b.root_ou_ref_id, b.root_ou_id," +
                    "    ou_ref_ids,\n" +
                    "    coalesce(array_length(ou_ref_ids,1),0) as count_of_ous\n" +
                    " FROM  \n" +
                    "    {0}.{1} ou_category \n" +
                    " LEFT JOIN (SELECT id root_ou_id, ref_id root_ou_ref_id, ou_category_id FROM {0}.{3} ous WHERE ous.active = true AND ous.parent_ref_id IS NULL) as b ON b.ou_category_id = ou_category.id \n" +
                    " LEFT JOIN " + SELECT_OU_COUNT_OU_GROUP +
                    " {2} ";
    private final static String SELECT_BY_DASHBOARD = "SELECT DISTINCT oc.workspace_id,oc.id,oc.name,oc.description,oc.is_predefined,oc.enabled,oc.created_at,oc.updated_at FROM {0}.dashboards d \n" +
            "LEFT JOIN {0}.ous_dashboard_mapping dm on dm.dashboard_id=d.id  LEFT JOIN {0}.ous ou on ou.id=dm.ou_id LEFT JOIN {0}.ou_categories oc on ou.ou_category_id=oc.id LEFT JOIN {0}.users u ON d.owner_id = u.id where workspace_id is not null and  workspace_id= :workspace_id and d.id= :dashboard_id";

    @Autowired
    public OrgUnitCategoryDatabaseService(final DataSource dataSource, OrgUnitHelper unitsHelper, ObjectMapper objectMapper) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.unitsHelper = unitsHelper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(WorkspaceDatabaseService.class, ProductService.class);
    }

    @Override
    public String insert(String company, OrgUnitCategory orgUnitCategory) throws SQLException {
        Map<String, Object> keyHolder = new HashMap<>();
        boolean inserted = Boolean.TRUE.equals(this.template.queryForObject(
                MessageFormat.format(INSERT_OU_GROUP, company, OUS_CATEGORY_TABLE_NAME),
                new MapSqlParameterSource()
                        .addValue("name", orgUnitCategory.getName())
                        .addValue("description", orgUnitCategory.getDescription())
                        .addValue("is_predefined", orgUnitCategory.getIsPredefined() != null ? orgUnitCategory.getIsPredefined() : false)
                        .addValue("enabled", orgUnitCategory.getEnabled() != null ? orgUnitCategory.getEnabled() : false)
                        .addValue("workspace_id", (orgUnitCategory.getWorkspaceId() != null && orgUnitCategory.getWorkspaceId() > 0) ? orgUnitCategory.getWorkspaceId() : null)
                        .addValue("created_at", Timestamp.from(orgUnitCategory.getCreatedAt() != null ? orgUnitCategory.getCreatedAt() : Instant.now())),
                (rs, rowNum) -> {
                    keyHolder.put("id", rs.getObject("id"));
                    keyHolder.put("name", rs.getString("name"));
                    return rs.getBoolean("inserted");
                }));
        var id = Pair.of((UUID) keyHolder.get("id"), (String) keyHolder.get("name"));
        if (inserted && orgUnitCategory.getRootOuName() != null && StringUtils.isNotEmpty(orgUnitCategory.getRootOuName())) {
            unitsHelper.insertNewOrgUnits(company, Stream.of(DBOrgUnit.builder()
                    .name(orgUnitCategory.getRootOuName())
                    .ouCategoryId(id.getLeft() != null ? id.getLeft() : null)
                    .build()));
        }
        if (id != null) {
            return id.getLeft().toString();
        }
        return null;
    }

    public OrgUnitCategory insertReturningOUGroup(String company, OrgUnitCategory orgUnitCategory) {
        Map<String, Object> keyHolder = new HashMap<>();
        boolean inserted = Boolean.TRUE.equals(this.template.queryForObject(
                MessageFormat.format(INSERT_OU_GROUP, company, OUS_CATEGORY_TABLE_NAME),
                new MapSqlParameterSource()
                        .addValue("name", orgUnitCategory.getName())
                        .addValue("description", orgUnitCategory.getDescription())
                        .addValue("is_predefined", orgUnitCategory.getIsPredefined() != null ? orgUnitCategory.getIsPredefined() : false)
                        .addValue("enabled", orgUnitCategory.getEnabled() != null ? orgUnitCategory.getEnabled() : false)
                        .addValue("workspace_id", (orgUnitCategory.getWorkspaceId() != null && orgUnitCategory.getWorkspaceId() > 0) ? orgUnitCategory.getWorkspaceId() : null)
                        .addValue("created_at", Timestamp.from(orgUnitCategory.getCreatedAt() != null ? orgUnitCategory.getCreatedAt() : Instant.now())),
                (rs, rowNum) -> {
                    keyHolder.put("id", rs.getObject("id"));
                    keyHolder.put("name", rs.getString("name"));
                    keyHolder.put("description", rs.getString("description"));
                    keyHolder.put("is_predefined", rs.getBoolean("is_predefined"));
                    keyHolder.put("enabled", rs.getBoolean("enabled"));
                    keyHolder.put("workspace_id", rs.getInt("workspace_id"));
                    return rs.getBoolean("inserted");
                }));
        Set<Integer> newOrgUnits = Set.of();
        OrgUnitCategory.OrgUnitCategoryBuilder orgUnitCategoryBuilder = OrgUnitCategory.builder()
                .id((UUID) keyHolder.get("id"))
                .name((String) keyHolder.get("name"))
                .description((String) keyHolder.get("description"))
                .isPredefined((Boolean) keyHolder.get("is_predefined"))
                .workspaceId((Integer) keyHolder.get("workspace_id"))
                .enabled((Boolean) keyHolder.get("enabled"));
        if (inserted && orgUnitCategory.getRootOuName() != null && StringUtils.isNotEmpty(orgUnitCategory.getRootOuName())) {
            newOrgUnits = unitsHelper.insertNewOrgUnits(company, Stream.of(DBOrgUnit.builder()
                    .name(orgUnitCategory.getRootOuName())
                    .ouCategoryId((orgUnitCategoryBuilder.build().getId() != null && orgUnitCategoryBuilder.build().getId() != null) ?
                            orgUnitCategoryBuilder.build().getId() : null)
                    .build()));
        }
        return keyHolder.get("id") == null ? null : orgUnitCategoryBuilder
                .rootOuName((CollectionUtils.isNotEmpty(newOrgUnits)) ? newOrgUnits.iterator().next().toString() : null)
                .build();
    }


    @Override
    public Boolean update(String company, OrgUnitCategory t) throws SQLException {
        return StringUtils.isNotEmpty(insert(company, t));
    }

    public OrgUnitCategory updateReturningOrgGroup(String company, OrgUnitCategory unitGroup) throws SQLException {
        Optional<OrgUnitCategory> dbOrgGroup = get(company, unitGroup.getId());
        if (dbOrgGroup.isPresent()) {
            var keyHolder = new GeneratedKeyHolder();
            int count = this.template.update(
                    MessageFormat.format(UPDATE_OU_GROUP, company, OUS_CATEGORY_TABLE_NAME),
                    new MapSqlParameterSource()
                            .addValue("name", unitGroup.getName() != null ? unitGroup.getName() : dbOrgGroup.get().getName())
                            .addValue("description", unitGroup.getDescription() != null ? unitGroup.getDescription() : dbOrgGroup.get().getDescription())
                            .addValue("is_predefined", unitGroup.getIsPredefined() != null ? unitGroup.getIsPredefined() : dbOrgGroup.get().getIsPredefined())
                            .addValue("enabled", unitGroup.getEnabled() != null ? unitGroup.getEnabled() : dbOrgGroup.get().getEnabled())
                            .addValue("workspace_id", unitGroup.getWorkspaceId())
                            .addValue("id", unitGroup.getId()),
                    keyHolder,
                    new String[]{"id", "name", "description", "is_predefined", "enabled", "workspace_id"}
            );
            var orgUnitGroup = count == 0 ? null : OrgUnitCategory.builder()
                    .id((UUID) keyHolder.getKeys().get("id"))
                    .name((String) keyHolder.getKeys().get("name"))
                    .description((String) keyHolder.getKeys().get("description"))
                    .isPredefined((Boolean) keyHolder.getKeys().get("is_predefined"))
                    .workspaceId((Integer) keyHolder.getKeys().get("workspace_id"))
                    .enabled((Boolean) keyHolder.getKeys().get("enabled"))
                    .build();
            if (orgUnitGroup != null) {
                return orgUnitGroup;
            }
        }
        log.info("Org Unit Group not present");
        return null;
    }


    @Override
    public Optional<OrgUnitCategory> get(String company, String ouGroupId) throws SQLException {
        return get(company, UUID.fromString(ouGroupId));
    }

    public Optional<OrgUnitCategory> get(String company, UUID ouGroupId) throws SQLException {
        var results = filter(company, QueryFilter.builder().strictMatch("id", ouGroupId).build(), 0, 1);
        if (results.getTotalCount() > 0) {
            return Optional.of(results.getRecords().get(0));
        }
        return Optional.empty();
    }

    public Optional<OrgUnitCategory> getByName(String company, String groupName) throws SQLException {
        var results = filter(company, QueryFilter.builder().strictMatch("name", groupName).build(), 0, 1);
        if (results.getTotalCount() > 0) {
            return Optional.of(results.getRecords().get(0));
        }
        return Optional.empty();
    }

    public DbListResponse<OrgUnitCategory> filterByDashboard(
            final String company,
            final Integer workspaceId,
            final Integer dashboardId,
            final QueryFilter filters,
            final Integer pageNumber,
            Integer pageSize) throws SQLException {
        var params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        params.addValue("workspace_id", workspaceId);
        params.addValue("dashboard_id", dashboardId);
        if(filters.getStrictMatches().get("enabled")!=null) {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}", "oc", "enabled"));
            params.addValue("enabled", filters.getStrictMatches().get("enabled"));
        }
        var extraConditions = conditions.size() > 0 ? " AND " + String.join(" AND ", conditions) + " " : "";
        var sqlBase = MessageFormat.format(SELECT_BY_DASHBOARD, company);
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        String limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize.toString(), String.valueOf(pageNumber * pageSize));
        String sql = sqlBase + extraConditions + " ORDER BY created_at ASC " + limit;
        log.info("sql = " + sql);
        log.info("params = {}", params);
        List<OrgUnitCategory> items = template.query(sql, params, (rs, row) -> OrgUnitCategory.builder()
                .id((UUID) rs.getObject("id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .isPredefined(rs.getBoolean("is_predefined"))
                .enabled(rs.getBoolean("enabled"))
                .workspaceId(rs.getInt("workspace_id"))
                .createdAt(rs.getTimestamp("created_at").toInstant())
                .updatedAt(rs.getTimestamp("updated_at").toInstant())
                .build());
        String countSQL = "SELECT COUNT(*) FROM (" + sql + ") as l";
        log.info("sql = " + countSQL);
        log.info("params = ", params);
        var total = template.queryForObject(countSQL, params, Integer.class);
        return DbListResponse.of(items, total);
    }

    public DbListResponse<OrgUnitCategory> filter(
            final String company,
            final QueryFilter filters,
            final Integer pageNumber,
            Integer pageSize) throws SQLException {
        var params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();

        populateConditions(company, filters, conditions, params);

        var extraConditions = conditions.size() > 0 ? " WHERE " + String.join(" AND ", conditions) + " " : "";

        var sqlBase = MessageFormat.format(BASE_SELECT, company, OUS_CATEGORY_TABLE_NAME, extraConditions, OUS_TABLE_NAME);
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        String limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize.toString(), String.valueOf(pageNumber * pageSize));
        String sql = sqlBase + " ORDER BY created_at ASC " + limit;
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<OrgUnitCategory> items = template.query(sql, params, (rs, row) -> OrgUnitCategory.builder()
                .id((UUID) rs.getObject("id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .isPredefined(rs.getBoolean("is_predefined"))
                .enabled(rs.getBoolean("enabled"))
                .workspaceId(rs.getInt("workspace_id"))
                .createdAt(rs.getTimestamp("created_at").toInstant())
                .updatedAt(rs.getTimestamp("updated_at").toInstant())
                .rootOuId((UUID) rs.getObject("root_ou_id"))
                .rootOuRefId(rs.getInt("root_ou_ref_id"))
                .ouRefIds(DatabaseUtils.fromSqlArray(rs.getArray("ou_ref_ids"), Integer.class).collect(Collectors.toList()))
                .ousCount(rs.getInt("count_of_ous"))
                .build());
        String countSQL = "SELECT COUNT(*) FROM (" + sql + ") as l";
        log.info("sql = " + countSQL); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        var total = template.queryForObject(countSQL, params, Integer.class);
        return DbListResponse.of(items, total);
    }

    @Override
    public DbListResponse<OrgUnitCategory> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return filter(company, null, pageNumber, pageSize);
    }

    public DbListResponse<OrgUnitCategory> list(String company, QueryFilter queryFilter, Integer pageNumber, Integer pageSize) throws SQLException {
        return filter(company, queryFilter, pageNumber, pageSize);
    }

    public Map<String,List<String>> getWorkspaceOuRefIdMappingsByOuRefIds(String company, List<Integer> ouRefIds) throws SQLException {
        String sql = "select workspace_id,anyarray_uniq(array_agg(ous.ref_id)::varchar[]) as ou_ids  from "+company+".products p\n" +
                "LEFT JOIN "+company+".ou_categories oc ON p.id = oc.workspace_id\n" +
                "LEFT JOIN "+company+".ous ous ON ous.ou_category_id = oc.id\n" +
                "WHERE ous.ref_id IN (:ou_ref_ids)\n" +
                "GROUP BY workspace_id";
        var params = new MapSqlParameterSource();
        params.addValue("ou_ref_ids", ouRefIds);
        Map<String,List<String>> workSpaceOuMappings = template.query(sql, params, new ResultSetExtractor<Map<String, List<String>>>() {
            @Override
            public Map<String, List<String>> extractData(ResultSet rs) throws SQLException, DataAccessException {
                Map<String,List<String>> result = new HashMap<>();
                while(rs.next()){
                    result.put(rs.getString("workspace_id"), DatabaseUtils.fromSqlArray(rs.getArray("ou_ids"), String.class).collect(Collectors.toList()));
                }
                return result;
            }
        });
        return workSpaceOuMappings;
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
        var sql = MessageFormat.format(DELETE_OU_GROUPS_SQL_FORMAT, company, OUS_CATEGORY_TABLE_NAME, String.join(" AND ", conditions));
        var updates = this.template.update(sql, params);
        if (ids.size() != updates) {
            log.warn("Request to delete {} records ended up deleting {} records (by ids)", ids.size(), updates);
        }
        return true;
    }

    public int bulkDelete(String company, List<String> ids) throws SQLException {
        if (CollectionUtils.isNotEmpty(ids)) {
            String SQL = "DELETE FROM " + company + ".ou_categories WHERE id IN (:ids) ";
            Map<String, Object> params1 = Map.of("ids", ids.stream().map(UUID::fromString).collect(Collectors.toList()));
            int rowsDeleted = template.update(SQL, params1);
            if (rowsDeleted > 0) {
                return rowsDeleted;
            }
        }
        return 0;
    }

    private void populateConditions(
            final String company,
            final QueryFilter filters,
            final @NonNull List<String> conditions,
            final @NonNull MapSqlParameterSource params) {
        if (filters != null && MapUtils.isNotEmpty(filters.getPartialMatches()) && filters.getPartialMatches().containsKey("name")) {
            conditions.add(MessageFormat.format("ou_group.name ILIKE ''%{0}%''", filters.getPartialMatches().get("name")));
        }
        if (filters != null && MapUtils.isNotEmpty(filters.getPartialMatches()) && filters.getPartialMatches().containsKey("group_category")) {
            conditions.add(MessageFormat.format("ou_group.group_category ILIKE ''%{0}%''", filters.getPartialMatches().get("group_category")));
        }
        if (filters == null || filters.getStrictMatches() == null || filters.getStrictMatches().isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : filters.getStrictMatches().entrySet()) {
            var item = entry.getKey();
            var values = entry.getValue();
            if (!allowedFilters.contains(item)
                    || filters.getStrictMatches().get(item) == null) {
                continue;
            }
            switch (item) {
                case "name":
                    processCondition("ou_category", "name", values, conditions, params);
                    continue;
                case "id":
                    processCondition("ou_category", "id", values, conditions, params);
                    continue;
                default:
                    processCondition("ou_category", item, values, conditions, params);
            }

        }
    }

    @SuppressWarnings("unchecked")
    private void processCondition(
            final String prefix,
            final String item,
            final Object values,
            final @NonNull List<String> conditions,
            final @NonNull MapSqlParameterSource params) {
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
        } else if (values instanceof Boolean) {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}::boolean", prefix, item));
        } else {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}", prefix, item));
        }
        params.addValue(item, values.toString());
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        ddl.forEach(item ->
                template.getJdbcTemplate()
                        .execute(MessageFormat.format(
                                item,
                                company,
                                OUS_CATEGORY_TABLE_NAME,
                                WORKSPACES_TABLE_NAME
                        )));
        return true;
    }
}
