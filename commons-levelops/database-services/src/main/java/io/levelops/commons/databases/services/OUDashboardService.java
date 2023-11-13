package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.dashboard.OUDashboard;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.models.DbListResponse;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class OUDashboardService extends DatabaseService<OUDashboard> {

    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate template;
    private final OrgUnitsDatabaseService orgUnitsDatabaseService;

    private static final Set<String> SORTABLE_COLUMNS = Set.of("name", "updated_at", "dashboard_order");
    private static final String DELETE_TEAMS_SQL_FORMAT = "DELETE FROM {0}.ous_dashboard_mapping oudm where {1}";
    public final static String OUS_DASHBOARD_MAPPING_TABLE_NAME = "ous_dashboard_mapping";
    public final static String OUS_TABLE_NAME = "ous";
    public final static String DASHBOARDS_TABLE_NAME = "dashboards";
    private final static String INSERT_OUS_DASHBOARD_MAPPING = "INSERT INTO {0}.{1}(ou_id, dashboard_id,dashboard_order) " +
            "VALUES(:ouId, :dashboardId, :dashboardOrder) ON CONFLICT(ou_id, dashboard_id) DO NOTHING";
    private final static List<String> ddl = List.of("CREATE TABLE IF NOT EXISTS {0}.{3} (" + // ous_dashboard_mapping
            "    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
            "    ou_id            UUID REFERENCES {0}.{1}(id) ON DELETE CASCADE," +
            "    dashboard_id     INTEGER REFERENCES {0}.{2}(id) ON DELETE CASCADE," +
            "    dashboard_order INTEGER," +
            "    UNIQUE(ou_id, dashboard_id)" +
            ");"
    );

    @Autowired
    public OUDashboardService(DataSource dataSource, ObjectMapper objectMapper, OrgUnitsDatabaseService orgUnitsDatabaseService) {
        super(dataSource);
        this.objectMapper = objectMapper;
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.orgUnitsDatabaseService = orgUnitsDatabaseService;
    }

    @Override
    public String insert(String company, OUDashboard t) throws SQLException {
        var keyHolder = new GeneratedKeyHolder();
        int count = this.template.update(
                MessageFormat.format(
                        INSERT_OUS_DASHBOARD_MAPPING,
                        company,
                        OUS_DASHBOARD_MAPPING_TABLE_NAME),
                new MapSqlParameterSource()
                        .addValue("ouId", t.getOuId())
                        .addValue("dashboardId", t.getDashboardId())
                        .addValue("dashboardOrder", t.getDashboardOrder()),
                keyHolder,
                new String[]{"id"}
        );
        var id = count == 0 ? null : (UUID) keyHolder.getKeys().get("id");
        return id == null ? null : id.toString();

    }

    @Override
    public Boolean update(String company, OUDashboard t) throws SQLException {
        return null;
    }

    public Boolean updateOuMapping(String company, List<OUDashboard> dashboardList, UUID ouId) throws SQLException {
        delete(company, ouId);
        if (CollectionUtils.isEmpty(dashboardList)) {
            log.info("Dashboard List to update is empty");
            return true;
        }
        var params = dashboardList.stream()
                .map(dashboard -> {
                    return new MapSqlParameterSource()
                            .addValue("ouId", ouId)
                            .addValue("dashboardId", dashboard.getDashboardId())
                            .addValue("dashboardOrder", dashboard.getDashboardOrder());
                })
                .filter(map -> map != null)
                .collect(Collectors.toSet()).toArray(new MapSqlParameterSource[0]);
        this.template.batchUpdate(
                MessageFormat.format(
                        INSERT_OUS_DASHBOARD_MAPPING,
                        company,
                        OUS_DASHBOARD_MAPPING_TABLE_NAME),
                params);

        return true;
    }

    @Override
    public Optional<OUDashboard> get(String company, String param) throws SQLException {
        return Optional.empty();
    }


    @Override
    public DbListResponse<OUDashboard> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return listByOuFilters(company, DashboardFilter.builder().build(), null, pageNumber, pageSize, null);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return delete(company, UUID.fromString(id));
    }

    public Boolean delete(String company, UUID id) throws SQLException {
        var conditions = new ArrayList<String>();
        var params = new MapSqlParameterSource();
        List<Object> values = new ArrayList<>();
        processCondition("oudm", "ou_id", id, conditions, params);
        var sql = MessageFormat.format(DELETE_TEAMS_SQL_FORMAT, company, String.join(" AND ", conditions));
        var updates = this.template.update(sql, params);
        return true;
    }

    public Boolean delete(String company, Set<UUID> ids) throws SQLException {
        var conditions = new ArrayList<String>();
        var params = new MapSqlParameterSource();
        List<Object> values = new ArrayList<>();
        processCondition("oudm", "ou_id", ids, conditions, params);
        var sql = MessageFormat.format(DELETE_TEAMS_SQL_FORMAT, company, String.join(" AND ", conditions));
        var updates = this.template.update(sql, params);
        if (ids.size() != updates) {
            log.warn("Request to delete {} records ended up deleting {} records (by ids)", ids.size(), updates);
        }
        return true;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(OrgUnitsDatabaseService.class, DashboardWidgetService.class);
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        ddl.forEach(item ->
                template.getJdbcTemplate()
                        .execute(MessageFormat.format(
                                item,
                                company,
                                OUS_TABLE_NAME,
                                DASHBOARDS_TABLE_NAME,
                                OUS_DASHBOARD_MAPPING_TABLE_NAME
                        )));
        return true;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = DashboardFilter.DashboardFilterBuilder.class)
    public static class DashboardFilter {
        String name;
        String exactName;
        UUID ouId;
        Boolean inherited;
        Boolean isPublic;
        Boolean isDemo;
        // If present it returns only those dashboards that this non-admin user has access to
        String rbacUserEmail;
    }

    public DbListResponse<OUDashboard> listByOuFilters(String company,
                                                       DashboardFilter filter,
                                                       DBOrgUnit dbOrgUnit,
                                                       Integer pageNumber,
                                                       Integer pageSize,
                                                       List<Map<String, Object>> sort) throws SQLException {
        String SQL = "SELECT d.id,d.name,d.metadata,ousd.dashboard_order,ousd.ou_id FROM " + company + ".dashboards d ";
        String ouMappingJoin = "INNER JOIN " + company + ".ous_dashboard_mapping ousd on d.id=ousd.dashboard_id ";
        List<String> conditions = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        if (dbOrgUnit == null) {
            dbOrgUnit = orgUnitsDatabaseService.get(company, filter.ouId).get();
        }
        boolean inherited = Boolean.TRUE.equals(filter.getInherited());
        if (inherited) {
            List<UUID> ouIds = getOuIds(company, dbOrgUnit);
            var collection = ouIds
                    .stream()
                    .filter(ObjectUtils::isNotEmpty)
                    .map(Object::toString)
                    .collect(Collectors.toSet());
            var tmp = MessageFormat.format("{0}.{1} = ANY({2})", "ousd", "ou_id", "'{" + String.join(",", collection) + "}'");
            conditions.add(tmp);
        } else {
            conditions.add("ousd.ou_id = ?");
            values.add(filter.getOuId());
        }
        if (StringUtils.isNotEmpty(filter.getName())) {
            conditions.add("d.name ILIKE ?");
            values.add("%" + filter.getName() + "%");
        }
        if (StringUtils.isNotEmpty(filter.getExactName())) {
            conditions.add("d.name = ?");
            values.add(filter.getExactName());
        }
        if (filter.getIsPublic() != null) {
            conditions.add("d.public = ?");
            values.add(filter.getIsPublic());
        }
        if (filter.getIsDemo() != null) {
            conditions.add("d.demo = ?");
            values.add(filter.getIsDemo());
        }
        if (StringUtils.isNotEmpty(filter.getRbacUserEmail())) {
            // This check is for non-admin users. We check the following:
            // 1. Is the dashboard public -> non-public dashboards are only shown to admins and queried differently
            // 1. dashboardPermissions = limited AND (rbac.allusers=true OR rbac.users has current users email as key) OR
            // 2. dashboardPermission = public (in this case all users have access to the dashboard)
            conditions.add("d.public = ?");
            values.add(true);
            conditions.add("((d.metadata->'rbac'->>'dashboardPermission'='public') OR " +
                    "( d.metadata->'rbac'->>'dashboardPermission' = 'limited' AND " +
                    "((d.metadata->'rbac'->>'allUsers')::bool = ? OR d.metadata->'rbac'->'users'->>? is not null)))");
            values.add(true);
            values.add(filter.getRbacUserEmail());
        }

        String orderByClause = getOrderByClause(sort);

        String whereClause = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        SQL = SQL + ouMappingJoin + whereClause + orderByClause + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        log.info("query {}", SQL);
        List<OUDashboard> retval = new ArrayList<>();
        String countSQL = "";
        countSQL = "SELECT COUNT(*) FROM " + company + ".dashboards d " + ouMappingJoin + whereClause;

        Integer totalCount = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {
            int i = 1;
            for (Object obj : values) {
                pstmt.setObject(i, obj);
                pstmt2.setObject(i++, obj);
            }
            log.info("Executing Query: " + pstmt);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Object displayName = objectMapper.readValue(
                        rs.getString("metadata"), Map.class).get("display_name");
                retval.add(OUDashboard.builder()
                        .dashboardId(rs.getInt("id"))
                        .name(rs.getString("name"))
                        .ouId((UUID) rs.getObject("ou_id"))
                        .dashboardOrder(rs.getInt("dashboard_order"))
                        .displayName(displayName == null ? rs.getString("name") : displayName.toString())
                        .build());
            }
            if (retval.size() > 0) {
                totalCount = retval.size() + pageNumber * pageSize;
                if (retval.size() == pageSize) {
                    rs = pstmt2.executeQuery();
                    if (rs.next()) {
                        totalCount = rs.getInt("count");
                    }
                }
            }
        } catch (SQLException | IOException e) {
            log.error("Failed to fetch data.", e);
            return DbListResponse.of(List.of(), 0);
        }
        return DbListResponse.of(retval, totalCount);
    }

    private List<UUID> getOuIds(String company, DBOrgUnit dbOrgUnit) {
        List<String> paths = getPaths(dbOrgUnit);
        UUID ouCategoryId = dbOrgUnit.getOuCategoryId() != null ? dbOrgUnit.getOuCategoryId() : null;
        if (CollectionUtils.isNotEmpty(paths)) {
            log.info("Fetching OU Ids for path : {}", paths);
            return orgUnitsDatabaseService.getOuIdsForGivenPath(company, paths, ouCategoryId);
        }
        log.info("There is no path to fetch the OU");
        return List.of();
    }

    private List<String> getPaths(DBOrgUnit dbOrgUnit) {
        String path = dbOrgUnit.getPath();
        List<String> paths = new ArrayList<>();
        paths.add(path);
        if (!StringUtils.startsWith(path, "/")) {
            path = "/" + path;
        }
        int i = StringUtils.lastIndexOf(path, "/");
        if (StringUtils.isNotEmpty(path) && i > 0) {
            do {
                path = StringUtils.substring(path, 0, i);
                paths.add(path);
                i = StringUtils.lastIndexOf(path, "/");
            } while (i != 0);
        }
        return paths;
    }

    private String getOrderByClause(List<Map<String, Object>> sort) {

        Map<String, SortingOrder> sortBy = SortingConverter.fromFilter(sort);

        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (SORTABLE_COLUMNS.contains(entry.getKey())) {
                        return entry.getKey();
                    }
                    return "name";
                })
                .orElse("name");

        String orderByClause = " ORDER BY " + sortByKey + " " + sortBy.getOrDefault(sortByKey, SortingOrder.ASC).name();

        return orderByClause;
    }

    private void processCondition(
            final String prefix,
            final String item,
            final Object values,
            final @NonNull List<String> conditions,
            final @NonNull MapSqlParameterSource params) {
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


}