package io.levelops.commons.databases.services.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.organization.DBTeam;
import io.levelops.commons.databases.models.database.organization.DBTeam.TeamMemberId;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Log4j2
public class TeamsDatabaseService extends DatabaseService<DBTeam> {
    public final static String TEAMS_TABLE_NAME = "teams";
    public final static String TEAM_MEMBERS_TABLE_NAME = "team_members";
    public final static String TEAM_MANAGERS_TABLE_NAME = "team_mangers";
    public final static String TEAM_MEMBERSHIP_TABLE_NAME = "team_membership";

    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper mapper;

    private final static List<String> ddl = List.of( 
        "CREATE TABLE IF NOT EXISTS {0}.{1} (" + // teams
        "    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
        "    name                varchar(100) UNIQUE NOT NULL," +
        "    description         varchar(100)," +
        "    parent_id           UUID REFERENCES {0}.{1}(id) ON DELETE SET NULL," +
        "    created_at          BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM now())," +
        "    updated_at          BIGINT NOT NULL" +
        ");",
        
        // "CREATE UNIQUE INDEX IF NOT EXISTS {1}_name_idx ON {0}.{1}(name)", // already provided by the constrain
        "CREATE INDEX IF NOT EXISTS {1}_created_at_idx ON {0}.{1}(created_at)",
        "CREATE INDEX IF NOT EXISTS {1}_updated_at_idx ON {0}.{1}(updated_at)",

        "CREATE TABLE IF NOT EXISTS {0}.{3} (" + // team managers
        "    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
        "    team_id            UUID REFERENCES {0}.{1}(id) ON DELETE CASCADE," +
        "    team_member_id     UUID REFERENCES {0}.{2}(id) ON DELETE CASCADE," +
        "    UNIQUE(team_id, team_member_id)" +
        ");",

        "CREATE TABLE IF NOT EXISTS {0}.{4} (" + // team_membership
        "    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
        "    team_id            UUID REFERENCES {0}.{1}(id) ON DELETE CASCADE," +
        "    team_member_id     UUID REFERENCES {0}.{2}(id) ON DELETE CASCADE," +
        "    created_at         BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM now())," +
        "    UNIQUE(team_id, team_member_id)" +
        ");");
    
    //#region queries
    
    private final static String BASE_FROM = 
        " FROM  " +
        "    {0}.{1} t" +
        "    {5}" +
        "    {6} ";
    
    private final static String BASE_SELECT = 
        "SELECT " +
        "    t.id," +
        "    t.name," +
        "    t.description," +
        "    t.parent_id," +
        "    to_json(ARRAY(SELECT row_to_json(m) FROM (SELECT tmm.team_member_id, tm.full_name, tm.email FROM {0}.{2} tmm, {0}.{4} tm WHERE t.id = tmm.team_id AND tm.id = tmm.team_member_id) AS m)) managers," +
        "    to_json(ARRAY(SELECT row_to_json(m) FROM (SELECT tmm.team_member_id, tm.full_name, tm.email FROM {0}.{3} tmm, {0}.{4} tm WHERE t.id = tmm.team_id AND tm.id = tmm.team_member_id) AS m)) members" +
        BASE_FROM;

    private final static String INSERT_TEAM_RELATIONSHIP_SQL_FORMAT = "INSERT INTO {0}.{1}(team_id, team_member_id) VALUES(:teamId, :teamMemberId) ON CONFLICT(team_id, team_member_id) DO NOTHING";
    private final static String INSERT_TEAM_SQL_FORMAT = "INSERT INTO {0}.{1}(name, description, updated_at) VALUES(:name, :description, EXTRACT(epoch FROM now()))";
    
    private final static String DELETE_TEAMS_SQL_FORMAT = 
        "DELETE FROM {0}.{1} as c WHERE {2}";

    private static final Set<String> allowedFilters = Set.of("name", "team_id", "parent_id", "manager_id", "team_member_id", "integration_id", "username", "email");
    private static final int DEFAULT_PAGE_SIZE = 10;

    //#endregion

    public TeamsDatabaseService(final DataSource dataSource, final ObjectMapper mapper) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.mapper = mapper;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(TeamMembersDatabaseService.class);
    }

    @Override
    public String insert(String company, DBTeam item) throws SQLException {
        var keyHolder = new GeneratedKeyHolder();

        // insert team
        int count = this.template.update(
            MessageFormat.format(INSERT_TEAM_SQL_FORMAT, company, TEAMS_TABLE_NAME),
            new MapSqlParameterSource()
                .addValue("name", item.getName())
                .addValue("description", item.getDescription()),
            keyHolder,
            new String[]{"id"}
        );
        var id = count == 0 ? null : (UUID) keyHolder.getKeys().get("id");
        // insert managers
        insertRelationship(company, id, item.getManagers(), TEAM_MANAGERS_TABLE_NAME);
        // insert members
        insertRelationship(company, id, item.getMembers(), TEAM_MEMBERSHIP_TABLE_NAME);

        return id.toString();
    }

    private void insertRelationship(final String company, final UUID teamId, final Set<TeamMemberId> members, final String type) throws SQLException {
        var params = members.stream().map(memberId -> new MapSqlParameterSource()
                    .addValue("teamId", teamId)
                    .addValue("teamMemberId", memberId.getId()))
                    .collect(Collectors.toSet()).toArray(new MapSqlParameterSource[0]);

        this.template.batchUpdate(MessageFormat.format(INSERT_TEAM_RELATIONSHIP_SQL_FORMAT, company, type), params);
    }

    @Override
    public Boolean update(String company, DBTeam t) throws SQLException {
        throw new NotImplementedException();
    }

    @Override
    public Optional<DBTeam> get(String company, String teamId) throws SQLException {
        return get(company, UUID.fromString(teamId));
    }

    public Optional<DBTeam> get(String company, UUID teamId) throws SQLException {
        var results = filter(company, QueryFilter.builder().strictMatch("team_id", teamId).build(), 0, 1);
        if(results.getTotalCount()> 0){
            return Optional.of(results.getRecords().get(0));
        }
        return Optional.empty();
    }

    @Override
    public DbListResponse<DBTeam> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return filter(company, null, pageNumber, pageSize);
    }

    public DbListResponse<DBTeam> filter(
        final String company,
        final QueryFilter filters,
        final Integer pageNumber,
        Integer pageSize) throws SQLException {
        var params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();

        populateConditions(company, filters, conditions, params);

        var extraConditions = conditions.size() > 0 ? " WHERE " + String.join(" AND ", conditions) + " " : "";
        var extraFrom = "";

        var sql = MessageFormat.format(BASE_SELECT, company, TEAMS_TABLE_NAME, TEAM_MANAGERS_TABLE_NAME, TEAM_MEMBERSHIP_TABLE_NAME, TEAM_MEMBERS_TABLE_NAME, extraFrom, extraConditions);
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        String limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize.toString(), String.valueOf(pageNumber*pageSize));
        List<DBTeam> items = template.query(sql + limit, params, (rs, row) -> {
            List<Map<String, Object>> managers = ParsingUtils.parseJsonList(mapper, "managers", rs.getString("managers"));
            List<Map<String, Object>> members = ParsingUtils.parseJsonList(mapper, "members", rs.getString("members"));
            return DBTeam.builder()
                .id((UUID) rs.getObject("id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .managers(managers.stream()
                    .map(item -> TeamMemberId.builder()
                        .id(UUID.fromString(item.get("team_member_id").toString()))
                        .fullName(item.get("full_name").toString())
                        .email(item.get("email").toString())
                        .build())
                    .collect(Collectors.toSet()))
                .members(members.stream()
                    .map(item -> TeamMemberId.builder()
                        .id(UUID.fromString(item.get("team_member_id").toString()))
                        .fullName(item.get("full_name").toString())
                        .email(item.get("email").toString())
                        .build())
                    .collect(Collectors.toSet()))
                .build();
        });
        var total = template.queryForObject("SELECT COUNT(*) FROM (" + sql + ") as l", params, Integer.class);
        return DbListResponse.of(items, total);
    }
    
    private void populateConditions(
        final String company,
        final QueryFilter filters,
        final @NonNull List<String> conditions,
        final @NonNull MapSqlParameterSource params) {
        if (filters == null || filters.getStrictMatches() == null || filters.getStrictMatches().isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry: filters.getStrictMatches().entrySet()) {
            var item = entry.getKey();
            var values = entry.getValue();
            if (!allowedFilters.contains(item) 
                || filters.getStrictMatches().get(item) == null) {
                continue;
            }
            var prefix = "";
            switch(item){
                case "team_id":
                    prefix = "t";
                    processCondition(prefix, "id", values, conditions, params);
                    continue;
                case "name":
                case "parent_id":
                    prefix = "t";
                    break;
                case "manager_id":
                    prefix = "tma";
                    processCondition(prefix, "team_member_id", values, conditions, params);
                    continue;
                case "team_member_id":
                    prefix = "tme";
                    processCondition(prefix, "team_member_id", values, conditions, params);
                    continue;
                case "integration_id":
                    prefix = "tmu";
                    break;
                case "username":
                    prefix = "tmu";
                    break;
                case "email":
                    prefix = "tm";
                    break;
                default:
                    continue;
            }
            processCondition(prefix, item, values, conditions, params);
        }

        // var projects = filters.getStrictMatches().getOrDefault("project", Set.of());
        // var tags = filters.getStrictMatches().getOrDefault("tag", Set.of());
        // var lastNReports = filters.getStrictMatches().getOrDefault("n_last_reports", null);
        // if (ObjectUtils.isNotEmpty(projects) || ObjectUtils.isNotEmpty(tags) || lastNReports instanceof Integer) {
        //     try {
        //         var values = getPluginResultIds(company, projects, tags, lastNReports);
        //         processCondition("r", "plugin_result_id", values, conditions, params);
        //     }
        //     catch(Exception e){
        //         log.error("Unable to get report ids from: projects={}, tags={}, n_last_reports={}", projects, tags, lastNReports, e);
        //     }
        // }
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
        if (values instanceof Collection) {
            var collection = ((Collection<Object>) values)
                        .stream()
                        .filter(ObjectUtils::isNotEmpty)
                        .map(Object::toString)
                        .map(s -> s.toLowerCase())
                        .collect(Collectors.toSet());
            var tmp = MessageFormat.format("{0}.{1} = ANY({2})", prefix, item, "'{" + String.join(",", collection) + "}'");
            log.debug("filter: {}", tmp);
            conditions.add(tmp);
            return;
        }
        if (values instanceof UUID) {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}::uuid", prefix, item));
        }
        else if (values instanceof Integer) {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}::int", prefix, item));
        }
        else if (values instanceof Long) {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}::bigint", prefix, item));
        }
        else {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}", prefix, item));
        }
        params.addValue(item, values.toString());
    }

    // public DbListResponse<Map<String, Object>> getValues(final String company, final String field, final QueryFilter filters, final Integer pageNumber, Integer pageSize){
    //     var params = new MapSqlParameterSource();
    //     List<String> conditions = new ArrayList<>();
    //     switch(field) {
    //         case "priority":
    //         case "category":
    //         case "model":
    //             populateConditions(company, filters, conditions, params);

    //             var extraConditions = conditions.size() > 0 ? " AND " + String.join(" AND ", conditions) + " " : "";

    //             var prefix = "model".equalsIgnoreCase(field) ? "r" : "i";
    //             var sql = MessageFormat.format("SELECT DISTINCT({5}.{6}) AS v " + BASE_FROM, company, TEAMS_TABLE_NAME, TEAM_MEMBERS_TABLE_NAME, "", extraConditions, prefix, field);
    //             if (pageSize == null || pageSize < 1) {
    //                 pageSize = DEFAULT_PAGE_SIZE;
    //             }
    //             String limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize.toString(), String.valueOf(pageNumber*pageSize));
    //             List<Map<String, Object>> items = template.query(sql + limit, params, (rs, row) -> {
    //                 return Map.of("key", rs.getObject("v"));
    //             });
    //             var totalCount = items.size();
    //             if (totalCount == pageSize) {
    //                 totalCount = template.queryForObject("SELECT COUNT(*) FROM (" + sql + ") AS l", params, Integer.class);
    //             }
    //             return DbListResponse.of(items, totalCount);
    //         case "project":
    //         case "tag":
    //             var extraFrom = "project".equalsIgnoreCase(field) ? ", {0}.products c, {0}.component_product_mappings m" : ", {0}.tags c, {0}.tagitems ti";
    //             populateConditions(company, filters, conditions, params);
    //             extraConditions = "project".equalsIgnoreCase(field) 
    //                 ? "AND m.component_id = r.plugin_result_id AND c.id = m.product_id" 
    //                 : "AND ti.itemid = r.plugin_result_id::text AND c.id = ti.tagid";
    //             extraConditions += conditions.size() > 0 ? " AND " + String.join(" AND ", conditions) + " " : "";
    //             sql = MessageFormat.format(BASE_FROM, company, TEAMS_TABLE_NAME, TEAM_MEMBERS_TABLE_NAME, extraFrom, "{1}");
    //             sql = MessageFormat.format("SELECT c.id, c.name " + sql, company, extraConditions);
    //             var query = MessageFormat.format("SELECT row_to_json(l) AS v FROM ({1}) AS l ", company, sql);

    //             if (pageSize == null || pageSize < 1) {
    //                 pageSize = DEFAULT_PAGE_SIZE;
    //             }
    //             limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize.toString(), String.valueOf(pageNumber*pageSize));
    //             items = List.copyOf(Set.copyOf(template.query(query + limit, params, (rs, row) -> {
    //                 var vals = ParsingUtils.parseJsonObject(mapper, "name", rs.getString("v"));
    //                 return Map.of("key", vals.get("id"), "value", vals.get("name"));
    //             })));
    //             totalCount = items.size();
    //             if (totalCount == pageSize) {
    //                 totalCount = template.queryForObject("SELECT COUNT(*) FROM (" + sql + ") AS l", params, Integer.class);
    //             }
    //             return DbListResponse.of(items, totalCount);
    //         default:
    //             return null;
    //     }
    //     // return DbListResponse.of(items, total);
    // }

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
        var sql = MessageFormat.format(DELETE_TEAMS_SQL_FORMAT, company, TEAMS_TABLE_NAME, String.join(" AND ", conditions));
        var updates = this.template.update(sql, params);
        if(ids.size() != updates){
            log.warn("Request to delete {} records ended up deleting {} records (by ids)", ids.size(), updates);
        }
        return true;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        ddl.forEach(item -> template.getJdbcTemplate()
            .execute(MessageFormat.format(
                item,
                company,
                TEAMS_TABLE_NAME,
                TEAM_MEMBERS_TABLE_NAME,
                TEAM_MANAGERS_TABLE_NAME,
                TEAM_MEMBERSHIP_TABLE_NAME)));
        return true;
    }
    
}
