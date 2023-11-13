package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.DashboardReport;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class DashboardWidgetService extends DatabaseService<Dashboard> {

    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate template;

    private static final Set<String> SORTABLE_COLUMNS = Set.of("name", "email", "updated_at");

    @Autowired
    public DashboardWidgetService(DataSource dataSource, ObjectMapper objectMapper) {
        super(dataSource);
        this.objectMapper = objectMapper;
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(UserService.class);
    }

    @Override
    public String insert(String company, Dashboard dashboard) throws SQLException {


        String retVal = null;
        String dashboardSQL = "INSERT INTO " + company + ".dashboards(name,demo,type,owner_id,query,metadata,public) " +
                "VALUES(?,?,?,?,to_json(?::json),to_json(?::json), ?)";
        String widgetSQL = "INSERT INTO " + company + ".widgets(id,name,type,dashboardid,query,metadata,display_info) " +
                "VALUES(?,?,?,?,to_json(?::json),to_json(?::json),to_json(?::json))";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(dashboardSQL,
                     Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmt1 = conn.prepareStatement(widgetSQL,
                     Statement.RETURN_GENERATED_KEYS)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                pstmt.setString(1, dashboard.getName());
                pstmt.setBoolean(2, dashboard.getDemo());
                pstmt.setString(3, dashboard.getType());
                pstmt.setObject(4, io.levelops.commons.utils.NumberUtils.toInteger(dashboard.getOwnerId()));
                pstmt.setString(5, objectMapper.writeValueAsString(
                        MoreObjects.firstNonNull(dashboard.getQuery(), Map.of())));
                pstmt.setString(6, objectMapper.writeValueAsString(
                        MoreObjects.firstNonNull(dashboard.getMetadata(), Map.of())));
                pstmt.setBoolean(7, dashboard.isPublic());
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        retVal = rs.getString(1);
                    }
                }
                if (CollectionUtils.isNotEmpty(dashboard.getWidgets())) {
                    for (Widget widget : dashboard.getWidgets()) {
                        UUID widgetId = (StringUtils.isNotEmpty(widget.getId())) ?
                                UUID.fromString(widget.getId()) : UUID.randomUUID();
                        pstmt1.setObject(1, widgetId);
                        pstmt1.setString(2, widget.getName());
                        pstmt1.setString(3, StringUtils.isEmpty(widget.getType()) ? "" : widget.getType());
                        pstmt1.setInt(4, NumberUtils.toInt(retVal));
                        pstmt1.setString(5,
                                objectMapper.writeValueAsString(
                                        MoreObjects.firstNonNull(widget.getQuery(), Map.of())));
                        pstmt1.setString(6,
                                objectMapper.writeValueAsString(
                                        MoreObjects.firstNonNull(widget.getMetadata(), Map.of())));
                        pstmt1.setString(7,
                                objectMapper.writeValueAsString(
                                        MoreObjects.firstNonNull(widget.getDisplayInfo(), Map.of())));
                        pstmt1.addBatch();
                        pstmt1.clearParameters();
                    }
                    pstmt1.executeBatch();
                }
                conn.commit();
            } catch (JsonProcessingException e) {
                conn.rollback();
                throw new SQLException("Failed to insert dashboard.", e);
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
        return retVal;
    }

    public String insertReport(String company, DashboardReport report) throws SQLException {

        String retVal = null;
        String SQL = "INSERT INTO " + company + ".dashboard_reports(name,file_id," +
                "created_by,dashboard_id) VALUES(?,?,?,?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL,
                     Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, report.getName());
            pstmt.setString(2, report.getFileId());
            pstmt.setInt(3, NumberUtils.toInt(report.getCreatedBy()));
            pstmt.setInt(4, NumberUtils.toInt(report.getDashboardId()));
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    retVal = rs.getString(1);
                }
            }
        }
        return retVal;
    }

    @Override
    public Boolean update(String company, Dashboard dashboard) throws SQLException {
        List<String> updateFields = new ArrayList<>();
        String condition = " WHERE id = ?";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(dashboard.getName())) {
            updateFields.add("name = ?");
            values.add(dashboard.getName());
        }
        if (StringUtils.isNotEmpty(dashboard.getType())) {
            updateFields.add("type = ?");
            values.add(dashboard.getType());
        }
        if (StringUtils.isNotEmpty(dashboard.getOwnerId())) {
            updateFields.add("owner_id = ?");
            values.add(NumberUtils.toInt(dashboard.getOwnerId()));
        }
        if (dashboard.getQuery() != null) {
            updateFields.add("query = to_json(?::json)");
            try {
                values.add(objectMapper.writeValueAsString(dashboard.getQuery()));
            } catch (JsonProcessingException e) {
                throw new SQLException("failed to convert query into string.");
            }
        }
        if (dashboard.getMetadata() != null) {
            updateFields.add("metadata = to_json(?::json)");
            try {
                values.add(objectMapper.writeValueAsString(dashboard.getMetadata()));
            } catch (JsonProcessingException e) {
                throw new SQLException("failed to convert metadata into string.");
            }
        }
        updateFields.add("public = ?");
        values.add(dashboard.isPublic());

        updateFields.add("updated_at = ?");
        values.add((new Date()).toInstant().getEpochSecond());

        var updates = updateFields.size() < 1 ? "" : String.join(", ", updateFields);
        boolean retVal;

        var updateDashboardSQL = "UPDATE " + company + ".dashboards SET " + updates + condition;

        String deleteWidgetSQL = "DELETE FROM " + company + ".widgets WHERE dashboardid = ?";
        String insertWidgetSQL = "INSERT INTO " + company + ".widgets(id,name,type,dashboardid,query,metadata,display_info) " +
                "VALUES(?,?,?,?,to_json(?::json),to_json(?::json),to_json(?::json))";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateDashboardSQL,
                     Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmt2 = conn.prepareStatement(deleteWidgetSQL,
                     Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmt3 = conn.prepareStatement(insertWidgetSQL,
                     Statement.RETURN_GENERATED_KEYS)) {
            //to do this in one transaction so that we dont drop widgets and fail to reinsert them.
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                int i = 1;
                for (Object obj : values) {
                    pstmt.setObject(i++, obj);
                }
                pstmt.setObject(i, NumberUtils.toInt(dashboard.getId()));
                pstmt.executeUpdate();
                pstmt2.setObject(1, NumberUtils.toInt(dashboard.getId()));
                pstmt2.executeUpdate();
                if (CollectionUtils.isNotEmpty(dashboard.getWidgets())) {
                    for (Widget widget : dashboard.getWidgets()) {
                        UUID widgetId = (StringUtils.isNotEmpty(widget.getId())) ?
                                UUID.fromString(widget.getId()) : UUID.randomUUID();
                        pstmt3.setObject(1, widgetId);
                        pstmt3.setString(2, widget.getName());
                        pstmt3.setString(3, StringUtils.isEmpty(
                                widget.getType()) ? "" : widget.getType());
                        pstmt3.setInt(4, Integer.parseInt(dashboard.getId()));
                        pstmt3.setString(5,
                                objectMapper.writeValueAsString(MoreObjects.firstNonNull(
                                        widget.getQuery(), Map.of())));
                        pstmt3.setString(6,
                                objectMapper.writeValueAsString(MoreObjects.firstNonNull(
                                        widget.getMetadata(), Map.of())));
                        pstmt3.setString(7,
                                objectMapper.writeValueAsString(MoreObjects.firstNonNull(
                                        widget.getDisplayInfo(), Map.of())));
                        pstmt3.addBatch();
                        pstmt3.clearParameters();
                    }
                    pstmt3.executeBatch();
                }
                conn.commit();
                retVal = true;
            } catch (JsonProcessingException e) {
                conn.rollback();
                throw new SQLException("Failed to convert widget into string in dashboardservice.");
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
        return retVal;
    }

    @Override
    public Optional<Dashboard> get(String company, String dashboardId) {
        String SQL = "SELECT id,name,demo,type,query,metadata,public,owner_id,createdat FROM "
                + company + ".dashboards WHERE id = ? LIMIT 1";

        String SQL2 = "SELECT id,name,dashboardid,metadata,type,query,display_info,precalculate,precalculate_frequency_in_mins FROM " + company
                + ".widgets WHERE dashboardid = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(SQL2)) {

            pstmt.setLong(1, Integer.parseInt(dashboardId));
            pstmt2.setLong(1, Integer.parseInt(dashboardId));

            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                return Optional.empty();
            }
            Dashboard.DashboardBuilder builder = Dashboard.builder()
                    .id(rs.getString("id"))
                    .name(rs.getString("name"))
                    .demo(rs.getBoolean("demo"))
                    .type(rs.getString("type"))
                    .ownerId(rs.getString("owner_id"))
                    .query(objectMapper.readValue(
                            rs.getString("query"), Map.class))
                    .metadata(objectMapper.readValue(
                            rs.getString("metadata"), Map.class))
                    .isPublic(rs.getBoolean("public"))
                    .createdAt(rs.getLong("createdat"));

            List<Widget> widgets = new ArrayList<>();
            rs = pstmt2.executeQuery();
            while (rs.next()) {
                widgets.add(Widget.builder()
                        .id(rs.getString("id"))
                        .name(rs.getString("name"))
                        .type(rs.getString("type"))
                        .dashboardId(rs.getString("dashboardid"))
                        .query(objectMapper.readValue(
                                rs.getString("query"), Map.class))
                        .metadata(objectMapper.readValue(
                                rs.getString("metadata"), Map.class))
                        .displayInfo(objectMapper.readValue(
                                rs.getString("display_info"), Map.class))
                        .precalculate(rs.getBoolean("precalculate"))
                        .precalculateFrequencyInMins(rs.getInt("precalculate_frequency_in_mins"))
                        .build());
            }
            builder.widgets(widgets);
            return Optional.of(builder.build());
        } catch (SQLException | IOException e) {
            log.error("Failed to fetch data.", e);
            return Optional.empty();
        }
    }

    @Override
    public DbListResponse<Dashboard> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return listByFilters(company, DashboardFilter.builder().build(), pageNumber, pageSize, null);
    }

    /**
     * @deprecated use this method instead {@link io.levelops.commons.databases.services.DashboardWidgetService#listByFilters(java.lang.String, io.levelops.commons.databases.services.DashboardWidgetService.DashboardFilter, java.lang.Integer, java.lang.Integer, java.util.List<java.util.Map<java.lang.String,java.lang.Object>>)}
     */
    @Deprecated
    public DbListResponse<Dashboard> listByFilters(String company,
                                                   String type,
                                                   String ownerId,
                                                   String name,
                                                   Boolean isPublic,
                                                   Integer workSpaceId,
                                                   Integer pageNumber,
                                                   Integer pageSize,
                                                   List<Map<String, Object>> sort) throws SQLException {
        return this.listByFilters(company,
                DashboardFilter.builder()
                        .type(type)
                        .ownerId(ownerId)
                        .name(name)
                        .isPublic(isPublic)
                        .workspaceId(workSpaceId)
                        .build(),
                pageNumber, pageSize, sort);
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = DashboardFilter.DashboardFilterBuilder.class)
    public static class DashboardFilter {
        String type;
        String ownerId;
        String name;
        String exactName;
        Boolean isPublic;
        Boolean isDemo;
        Integer workspaceId;
        // If present it returns only those dashboards that this non-admin user has access to
        String rbacUserEmail;
        List<String> ids;
    }

    public DbListResponse<Dashboard> listByFilters(String company,
                                                   DashboardFilter filter,
                                                   Integer pageNumber,
                                                   Integer pageSize,
                                                   List<Map<String, Object>> sort) throws SQLException {
        String SQL = "SELECT DISTINCT d.id,d.name,d.demo,d.type,d.metadata,d.public,d.query,d.owner_id,d.createdat,u.firstname,u.lastname, u.email, d.updated_at FROM " + company + ".dashboards d ";
        String workspaceJoinSQL = " LEFT JOIN " + company + ".ous_dashboard_mapping dm on dm.dashboard_id=d.id  LEFT JOIN " + company + ".ous ou on ou.id=dm.ou_id LEFT JOIN " + company + ".ou_categories oc on ou.ou_category_id=oc.id";
        String userJoinSQL = " LEFT JOIN " + company + ".users u ON d.owner_id = u.id ";
        List<String> conditions = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(filter.getType())) {
            conditions.add("d.type = ?");
            values.add(filter.getType());
        }
        if (StringUtils.isNotEmpty(filter.getOwnerId())) {
            conditions.add("d.owner_id = ?");
            values.add(NumberUtils.toInt(filter.getOwnerId()));
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
        if (filter.getWorkspaceId() != null && filter.getWorkspaceId() != 0) {
            conditions.add("workspace_id = ?");
            values.add(filter.getWorkspaceId());
        }
        if (CollectionUtils.isNotEmpty(filter.getIds())) {
            conditions.add("d.id in ("+filter.getIds().stream().map(id -> "?").collect(Collectors.joining(", "))+")");
            filter.getIds().forEach(id -> values.add(Integer.valueOf(id)));
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

        SQL = SQL + workspaceJoinSQL + userJoinSQL + whereClause + orderByClause + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        List<Dashboard> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(DISTINCT d.id) FROM " + company + ".dashboards d " + workspaceJoinSQL + userJoinSQL + whereClause;
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
                retval.add(Dashboard.builder()
                        .type(rs.getString("type"))
                        .id(rs.getString("id"))
                        .demo(rs.getBoolean("demo"))
                        .name(rs.getString("name"))
                        .metadata(objectMapper.readValue(
                                rs.getString("metadata"), Map.class))
                        .query(objectMapper.readValue(
                                rs.getString("query"), Map.class))
                        .ownerId(rs.getString("owner_id"))
                        .isPublic(rs.getBoolean("public"))
                        .createdAt(rs.getLong("createdat"))
                        .firstName(rs.getString("firstname"))
                        .lastName(rs.getString("lastname"))
                        .email(rs.getString("email"))
                        .updatedAt(rs.getLong("updated_at"))
                        .build());
            }
            if (retval.size() > 0) {
                totalCount = retval.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
                if (retval.size() == pageSize) {
                    rs = pstmt2.executeQuery();
                    if (rs.next()) {
                        totalCount = rs.getInt("count");
                    }
                }
            }
        } catch (JsonProcessingException e) {
            throw new SQLException("Couldnt parse query field for dashboard or widget.");
        }
        return DbListResponse.of(retval, totalCount);
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

    public DbListResponse<DashboardReport> listReports(String company,
                                                       String name,
                                                       String dashboardId,
                                                       String createdBy,
                                                       Integer pageNumber,
                                                       Integer pageSize)
            throws SQLException {
        String SQL = "SELECT id,name,file_id,created_by,dashboard_id,created_at FROM "
                + company + ".dashboard_reports";
        String condition = "";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(name)) {
            condition = " WHERE name ILIKE ?";
            values.add("%" + name + "%");
        }
        if (StringUtils.isNotEmpty(dashboardId)) {
            condition = StringUtils.isEmpty(condition) ? " WHERE dashboard_id = ?" : condition + " AND dashboard_id = ?";
            values.add(NumberUtils.toInt(dashboardId));
        }
        if (StringUtils.isNotEmpty(createdBy)) {
            condition = StringUtils.isEmpty(condition) ? " WHERE created_by = ?" : condition + " AND created_by = ?";
            values.add(NumberUtils.toInt(createdBy));
        }
        SQL = SQL + condition + " ORDER BY created_at DESC LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        List<DashboardReport> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(*) FROM " + company + ".dashboard_reports" + condition;
        Integer totalCount = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {
            int i = 1;
            for (Object obj : values) {
                pstmt.setObject(i, obj);
                pstmt2.setObject(i++, obj);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    retval.add(DashboardReport.builder()
                            .name(rs.getString("name"))
                            .id(rs.getString("id"))
                            .fileId(rs.getString("file_id"))
                            .dashboardId(rs.getString("dashboard_id"))
                            .createdBy(rs.getString("created_by"))
                            .createdAt(rs.getLong("created_at"))
                            .build());
                }
            }
            if (retval.size() > 0) {
                totalCount = retval.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
                if (retval.size() == pageSize) {
                    try (ResultSet rs = pstmt2.executeQuery()) {
                        if (rs.next()) {
                            totalCount = rs.getInt("count");
                        }
                    }
                }
            }
        }
        return DbListResponse.of(retval, totalCount);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".dashboards WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setLong(1, Integer.parseInt(id));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    public int bulkDelete(String company, List<String> dashboardsIds) {

        if (CollectionUtils.isNotEmpty(dashboardsIds)) {
            Map<String, Object> params = Map.of("ids", dashboardsIds.stream().map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
            String SQL = "DELETE FROM " + company + ".dashboards WHERE id IN (:ids)";
            return template.update(SQL, params);
        }
        return 0;
    }

    public Boolean deleteReport(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".dashboard_reports WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setLong(1, Integer.parseInt(id));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    public int deleteBulkReport(String company, List<String> ids) throws SQLException {
        int affectedRows = 0;
        if (CollectionUtils.isNotEmpty(ids)) {
            Map<String, Object> params = Map.of("ids", ids.stream().map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
            String sql = "DELETE FROM " + company + ".dashboard_reports WHERE id IN (:ids)";
            affectedRows = template.update(sql, params);
        }
        return affectedRows;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".dashboards(\n" +
                        "        id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, \n" +
                        "        name VARCHAR(100) NOT NULL, \n" +
                        "        owner_id INTEGER REFERENCES " + company + ".users(id) ON DELETE SET NULL," +
                        "        demo BOOLEAN NOT NULL DEFAULT false, \n" +
                        "        query JSONB, \n" +
                        "        metadata JSONB, \n" +
                        "        public BOOLEAN NOT NULL DEFAULT false, \n" +
                        "        type VARCHAR(50) NOT NULL, \n" +
                        "        createdat BIGINT DEFAULT extract(epoch from now()), \n" +
                        "        updated_at BIGINT DEFAULT extract(epoch from now()) \n" +
                        ");",

                "CREATE TABLE IF NOT EXISTS " + company + ".widgets(\n" +
                        "        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), \n" +
                        "        name VARCHAR(100) NOT NULL, \n" +
                        "        type VARCHAR(50) NOT NULL, \n" +
                        "        query JSONB, \n" +
                        "        metadata JSONB, \n" +
                        "        display_info JSONB, \n" +
                        "        dashboardid INTEGER NOT NULL REFERENCES " + company + ".dashboards(id) ON DELETE CASCADE, \n" +
                        "        precalculate BOOLEAN NOT NULL DEFAULT false,\n" +
                        "        precalculate_frequency_in_mins INTEGER NOT NULL DEFAULT 0,\n" +
                        "        createdat BIGINT DEFAULT extract(epoch from now())\n" +
                        "    )",

                "CREATE TABLE IF NOT EXISTS " + company + ".dashboard_reports(\n" +
                        "        id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, \n" +
                        "        name VARCHAR(100) NOT NULL, \n" +
                        "        created_by INTEGER REFERENCES " + company + ".users(id) ON DELETE SET NULL," +
                        "        file_id VARCHAR NOT NULL, \n" +
                        "        dashboard_id INTEGER NOT NULL REFERENCES " + company + ".dashboards(id) ON DELETE CASCADE, \n" +
                        "        created_at BIGINT DEFAULT extract(epoch from now())\n" +
                        ");",

                "CREATE INDEX IF NOT EXISTS widgets_dashboardid_idx on " + company + ".widgets (dashboardid)");
        ddl.forEach(statement -> template.getJdbcTemplate().execute(statement));

        populateDemoDashboard(company);

        return true;
    }

    public void populateDemoDashboard(String company) {
        String resourceString = null;
        try {
            resourceString = ResourceUtils.getResourceAsString("db/default_data/dashboards/demo_dashboard.json", DashboardWidgetService.class.getClassLoader());
            var dashboard = objectMapper.readValue(resourceString, Dashboard.class);
            populateDashboardIdempotent(company, dashboard);
        } catch (IOException e) {
            log.warn("Failed to populate demo dashboard", e);
        }
    }

    public void populateDashboardIdempotent(String company, Dashboard dashboard) throws JsonProcessingException {
        String metadata = objectMapper.writeValueAsString(dashboard.getMetadata());

        String sql = "INSERT INTO " + company + ".dashboards (name,demo,type,owner_id,query,metadata,public)\n" +
                "SELECT :name, :demo, 'dashboard', null, '{}'::json, :metadata::json, false\n" +
                "WHERE NOT EXISTS (\n" +
                "    SELECT 1 FROM " + company + ".dashboards WHERE name = :name\n" +
                ");";

        template.update(sql, Map.of(
                "name", dashboard.getName(),
                "demo", dashboard.getDemo(),
                "metadata", metadata));
    }

    public String insertWidget(String company, Widget widget, String dashboardId) throws SQLException {
        String retVal = null;
        String widgetSQL = "INSERT INTO " + company + ".widgets(id,name,type,dashboardid,query,metadata,display_info) " +
                "VALUES(?,?,?,?,to_json(?::json),to_json(?::json),to_json(?::json))";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt1 = conn.prepareStatement(widgetSQL,
                     Statement.RETURN_GENERATED_KEYS)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                UUID widgetId = (StringUtils.isNotEmpty(widget.getId())) ?
                        UUID.fromString(widget.getId()) : UUID.randomUUID();
                pstmt1.setObject(1, widgetId);
                pstmt1.setString(2, widget.getName());
                pstmt1.setString(3, StringUtils.isEmpty(widget.getType()) ? "" : widget.getType());
                pstmt1.setInt(4, NumberUtils.toInt(dashboardId));
                pstmt1.setString(5,
                        objectMapper.writeValueAsString(
                                MoreObjects.firstNonNull(widget.getQuery(), Map.of())));
                pstmt1.setString(6,
                        objectMapper.writeValueAsString(
                                MoreObjects.firstNonNull(widget.getMetadata(), Map.of())));
                pstmt1.setString(7,
                        objectMapper.writeValueAsString(
                                MoreObjects.firstNonNull(widget.getDisplayInfo(), Map.of())));
                pstmt1.executeUpdate();
                try (ResultSet rs = pstmt1.getGeneratedKeys()) {
                    if (rs.next()) {
                        retVal = rs.getString("id");
                    }
                }
                conn.commit();
            } catch (JsonProcessingException e) {
                conn.rollback();
                throw new SQLException("Failed to insertWidget widget.", e);
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
        return retVal;
    }

    public Boolean updateWidget(String company, Widget widget) throws SQLException {
        List<String> updateFields = new ArrayList<>();
        String condition = " WHERE id = ?";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(widget.getName())) {
            updateFields.add("name = ?");
            values.add(widget.getName());
        }
        if (StringUtils.isNotEmpty(widget.getType())) {
            updateFields.add("type = ?");
            values.add(widget.getType());
        }
        if (StringUtils.isNotEmpty(widget.getDashboardId())) {
            updateFields.add("dashboardid = ?");
            values.add(NumberUtils.toInt(widget.getDashboardId()));
        }
        if (widget.getQuery() != null) {
            updateFields.add("query = to_json(?::json)");
            try {
                values.add(objectMapper.writeValueAsString(widget.getQuery()));
            } catch (JsonProcessingException e) {
                throw new SQLException("failed to convert query into string.");
            }
        }
        if (widget.getMetadata() != null) {
            updateFields.add("metadata = to_json(?::json)");
            try {
                values.add(objectMapper.writeValueAsString(widget.getMetadata()));
            } catch (JsonProcessingException e) {
                throw new SQLException("failed to convert metadata into string.");
            }
        }
        if (widget.getDisplayInfo() != null) {
            updateFields.add("display_info = to_json(?::json)");
            try {
                values.add(objectMapper.writeValueAsString(widget.getDisplayInfo()));
            } catch (JsonProcessingException e) {
                throw new SQLException("failed to convert display_info into string.");
            }
        }
        var updates = updateFields.size() < 1 ? "" : String.join(", ", updateFields);
        boolean retVal;
        var updateWidgetSQL = "UPDATE " + company + ".widgets SET " + updates + condition;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateWidgetSQL,
                     Statement.RETURN_GENERATED_KEYS)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                int i = 1;
                for (Object obj : values) {
                    pstmt.setObject(i++, obj);
                }
                pstmt.setObject(i, UUID.fromString(widget.getId()));
                pstmt.executeUpdate();
                conn.commit();
                retVal = true;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
        return retVal;
    }

    public DbListResponse<Widget> listByFilters(String company,
                                                String dashboardId,
                                                Integer pageNumber,
                                                Integer pageSize) throws SQLException {
        String SQL = "SELECT id,dashboardid,name,type,metadata,query,display_info FROM " + company + ".widgets";
        String condition = "";
        if (StringUtils.isNotEmpty(dashboardId)) {
            condition = StringUtils.isEmpty(condition) ? " WHERE dashboardid = " + NumberUtils.toInt(dashboardId) : condition + " AND dashboardid = " + NumberUtils.toInt(dashboardId);
        }
        SQL = SQL + condition + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        List<Widget> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(*) FROM " + company + ".widgets" + condition;
        int totalCount = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(countSQL)) {
            ResultSet rs;
            if (pageSize > 0) {
                retval = template.query(SQL, listRowMapper());
                totalCount = retval.size() + pageNumber * pageSize;
                if (retval.size() == pageSize) {
                    rs = pstmt.executeQuery();
                    if (rs.next()) {
                        totalCount = rs.getInt("count");
                    }
                }
            }
        }
        return DbListResponse.of(retval, totalCount);
    }

    public DbListResponse<Widget> listWidgetsByFilters(String company,
                                                       List<UUID> widgetIds,
                                                       List<String> widgetTypes,
                                                       Boolean precalculate,
                                                       Integer pageNumber,
                                                       Integer pageSize) throws SQLException {
        List<String> criterias = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        if (CollectionUtils.isNotEmpty(widgetIds)) {
            criterias.add("id in (:ids)");
            params.put("ids", widgetIds);
        }
        if (CollectionUtils.isNotEmpty(widgetTypes)) {
            criterias.add("type in (:types)");
            params.put("types", widgetTypes);
        }
        if (precalculate != null) {
            criterias.add("precalculate = :precalculate");
            params.put("precalculate", precalculate);
        }

        String selectSqlBase = "SELECT * FROM " + company + ".widgets";
        String criteria = "";
        if (CollectionUtils.isNotEmpty(criterias)) {
            criteria = " WHERE " + String.join(" AND ", criterias);
        }

        List<String> sortBy = new ArrayList<>();
        sortBy.add("createdat DESC");
        String orderBy = " ORDER BY " + String.join(",", sortBy);

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" + selectSqlBase + criteria + ") AS counted";

        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<Widget> results = template.query(selectSql, params, listRowMapper());

        Integer totCount = 0;
        if (results.size() > 0) {
            totCount = results.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (results.size() == pageSize) {
                totCount = template.query(countSql, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(results, totCount);
    }

    public void enableWidgetPrecalculation(String company, List<UUID> widgetIds, int precalculateFrequencyInMins) {
        String sql = "UPDATE " + company + ".widgets SET precalculate=true, precalculate_frequency_in_mins=:precalculate_frequency_in_mins where id in (:ids)";
        HashMap<String, Object> params = new HashMap<>();
        params.put("ids", widgetIds);
        params.put("precalculate_frequency_in_mins", precalculateFrequencyInMins);
        this.template.update(sql, params);
    }

    public Optional<Widget> getWidget(String company, String widgetId, String dashboardId) throws SQLException {
        String SQL = "SELECT id,name,dashboardid,metadata,type,query,display_info,precalculate,precalculate_frequency_in_mins FROM " + company
                + ".widgets WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);) {
            pstmt.setObject(1, UUID.fromString(widgetId));
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                return Optional.empty();
            }
            Widget.WidgetBuilder builder = Widget.builder()
                    .id(rs.getString("id"))
                    .name(rs.getString("name"))
                    .type(rs.getString("type"))
                    .dashboardId(rs.getString("dashboardid"))
                    .query(objectMapper.readValue(
                            rs.getString("query"), Map.class))
                    .metadata(objectMapper.readValue(
                            rs.getString("metadata"), Map.class))
                    .displayInfo(objectMapper.readValue(
                            rs.getString("display_info"), Map.class))
                    .precalculate(rs.getBoolean("precalculate"))
                    .precalculateFrequencyInMins(rs.getInt("precalculate_frequency_in_mins"));
            return Optional.of(builder.build());
        } catch (SQLException | IOException e) {
            log.error("Failed to fetch data with ID: " + widgetId, e);
            return Optional.empty();
        }
    }

    public Boolean deleteWidget(String company, String widgetId, String dashboardId) throws SQLException {
        String SQL = "DELETE FROM " + company + ".widgets WHERE (id = ? AND dashboardid = ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            UUID uuid = UUID.fromString(widgetId);
            pstmt.setObject(1, uuid);
            pstmt.setLong(2, Integer.parseInt(dashboardId));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    public int bulkWidgetsDelete(String company, List<String> widgetsIds) {
        if (CollectionUtils.isNotEmpty(widgetsIds)) {
            Map<String, Object> params = Map.of("ids", widgetsIds.stream().map(UUID::fromString)
                    .collect(Collectors.toList()));
            String SQL = "DELETE FROM " + company + ".widgets WHERE id IN (:ids)";
            return template.update(SQL, params);
        }
        return 0;
    }

    private RowMapper<Widget> listRowMapper() {
        return (rs, rowNumber) -> {
            try {
                return rowMapperBuilder(rs);
            } catch (JsonProcessingException e) {
                log.error("listRowMapper: unable to convert DBRecord into widget: " + e.getMessage(), e);
            }
            return null;
        };
    }

    private Widget rowMapperBuilder(ResultSet rs) throws SQLException, JsonProcessingException {
        return Widget.builder()
                .type(rs.getString("type"))
                .id(rs.getString("id"))
                .name(rs.getString("name"))
                .metadata(objectMapper.readValue(
                        rs.getString("metadata"), Map.class))
                .query(objectMapper.readValue(
                        rs.getString("query"), Map.class))
                .displayInfo(objectMapper.readValue(
                        rs.getString("display_info"), Map.class))
                .dashboardId(rs.getString("dashboardid"))
                .precalculate(rs.getBoolean("precalculate"))
                .precalculateFrequencyInMins(rs.getInt("precalculate_frequency_in_mins"))
                .build();
    }

    public Integer createDemoDashboards(String company, String type) {
        String resourceDemoDashboardData = null;
        Integer dashboardId = null;
        try {
            resourceDemoDashboardData = ResourceUtils.getResourceAsString("db/default_data/dashboards/" + type + "_demo_dashboard.json", DashboardWidgetService.class.getClassLoader());
            var demoDashboard = objectMapper.readValue(resourceDemoDashboardData, Dashboard.class);
            dashboardId = insertUpdateDashboardName(company, demoDashboard);
        } catch (IOException e) {
            log.warn("Failed to populate demo dashboard", e);
        }
        return dashboardId;
    }

    public Integer insertDashBoard(String company, Dashboard dashboard) throws JsonProcessingException {
        String metadata = objectMapper.writeValueAsString(dashboard.getMetadata());
        var keyHolder = new GeneratedKeyHolder();
        String sql = "INSERT INTO " + company + ".dashboards (name,demo,type,owner_id,query,metadata,public)\n" +
                "SELECT :name, :demo, 'dashboard', null, '{}'::json, :metadata::json, false\n" +
                "WHERE NOT EXISTS (\n" +
                "    SELECT 1 FROM " + company + ".dashboards WHERE name = :name\n" +
                ");";

        int count = template.update(sql, new MapSqlParameterSource().addValue(
                "name", dashboard.getName()).addValue(
                "demo", dashboard.getDemo()).addValue(
                "metadata", metadata), keyHolder);
        var id = count == 0 ? null : (Integer) keyHolder.getKeys().get("id");
        return id;
    }

    public Integer insertUpdateDashboardName(String company, Dashboard dashboard) throws JsonProcessingException {
        String metadata = objectMapper.writeValueAsString(dashboard.getMetadata());
        var keyHolder = new GeneratedKeyHolder();
        String sql = "INSERT INTO " + company + ".dashboards (name,demo,type,owner_id,query,metadata,public) VALUES( :name, :demo, 'dashboard', null, '{}'::json, :metadata::json, false );";

        int count = template.update(sql, new MapSqlParameterSource().addValue(
                "name", dashboard.getName()).addValue(
                "demo", dashboard.getDemo()).addValue(
                "metadata", metadata), keyHolder);
        var id = count == 0 ? null : (Integer) keyHolder.getKeys().get("id");
        String updateSql = "UPDATE " + company + ".dashboards SET name=:name where id = :id";
        template.update(updateSql, new MapSqlParameterSource().addValue(
                "name", dashboard.getName() + "_" + id).addValue("id", id));

        return id;
    }
}