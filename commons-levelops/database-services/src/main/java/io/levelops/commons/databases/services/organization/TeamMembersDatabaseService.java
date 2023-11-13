package io.levelops.commons.databases.services.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.organization.DBTeamMember;
import io.levelops.commons.databases.models.database.organization.TeamMemberDTO;
import io.levelops.commons.databases.models.database.organization.TeamMemberId;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
public class TeamMembersDatabaseService extends DatabaseService<DBTeamMember> {
    public final static String TEAM_MEMBERS_TABLE_NAME = "team_members";
    public final static String TEAM_MEMBER_USERNAMES_TABLE_NAME = "team_member_usernames";
    public final static String USERS_TABLE = "integration_users";

    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper mapper;

    private final static List<String> ddl = List.of(
            "CREATE TABLE IF NOT EXISTS {0}.{1} (" + // team member
                    "    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                    "    full_name      VARCHAR(150) NOT NULL," +
                    "    email          VARCHAR(150) UNIQUE," +
                    "    created_at     BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM now())," +
                    "    updated_at     BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM now())" +
                    ");",

            "CREATE INDEX IF NOT EXISTS {1}_full_name_idx ON {0}.{1}(lower(full_name))",
            // "CREATE UNIQUE INDEX IF NOT EXISTS {1}_email_idx ON {0}.{1}(lower(email))",
            "CREATE INDEX IF NOT EXISTS {1}_created_at_idx ON {0}.{1}(created_at)",
            "CREATE INDEX IF NOT EXISTS {1}_updated_at_idx ON {0}.{1}(updated_at)",

            "CREATE TABLE IF NOT EXISTS {0}.{2} (" + // team member username
                    "    id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                    "    integration_user_id UUID NOT NULL REFERENCES {0}." + USERS_TABLE + "(id) ON DELETE CASCADE," +
                    "    team_member_id     UUID REFERENCES {0}.{1}(id) ON DELETE CASCADE," +
                    "    created_at         BIGINT NOT NULL DEFAULT EXTRACT(epoch FROM now())," +
                    "    updated_at         BIGINT" +
                    ");",
            "CREATE INDEX IF NOT EXISTS {2}_team_member_id_integration_id_idx ON {0}.{2}(team_member_id, integration_user_id)");

    //#region queries

    private final static String BASE_FROM =
            " FROM  " +
                    "    {0}.{1} tm" +
                    "    {4}" +
                    " {5} " +
                    " GROUP BY tm.id {6}";

    private final static String BASE_SELECT =
            "SELECT " +
                    "    tm.id," +
                    "    tm.full_name," +
                    "    tm.email," +
                    "    to_json(ARRAY(SELECT row_to_json(u) FROM (SELECT iu.display_name, iu.cloud_id, iu.integration_id, i.application, tmu.team_member_id FROM {0}.{2} tmu, {0}.{3} iu, {0}.integrations i " +
                    "WHERE i.id = iu.integration_id AND tm.id = tmu.team_member_id) AS u)) ids" +
                    BASE_FROM;

    private final static String INSERT_TEAM_MEMBER_SQL_FORMAT =
            "INSERT INTO {0}.{1}(full_name, email, created_at, updated_at) "
                    + "VALUES(:fullName, :email, EXTRACT(epoch FROM now()), EXTRACT(epoch FROM now())) "
                    + "ON CONFLICT (email) DO UPDATE SET full_name = EXCLUDED.full_name";
    private final static  String UPDATE_TEAM_MEMBER_SQL_FORMAT = "" +
            "UPDATE {0}.{1} as tu SET full_name = :fullName , email = :email, updated_at = EXTRACT(epoch FROM now()) WHERE id = :id::uuid";
    private final static String INSERT_TEAM_MEMBER_USERNAMES_SQL_FORMAT = "INSERT INTO {0}.{1}( integration_user_id, team_member_id, created_at, updated_at) VALUES(:integrationUserId, :teamMemberId, EXTRACT(epoch FROM now()),EXTRACT(epoch FROM now()))";
    private final static String BASE_SELECT_FROM_TEAM_USERS = "SELECT team_member_id FROM {0}.{1} as tmu WHERE {2}";
    private final static String DELETE_TEAM_MEMBERS_SQL_FORMAT =
            "DELETE FROM {0}.{1} as c WHERE {2}";

    private static final Set<String> allowedFilters = Set.of("integration_id", "team_member_id", "email", "full_name");
    private static final int DEFAULT_PAGE_SIZE = 10;

    //#endregion

    public TeamMembersDatabaseService(final DataSource dataSource, final ObjectMapper mapper) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.mapper = mapper;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class, UserIdentityService.class);
    }

    @Override
    public String insert(String company, DBTeamMember dbTeamMember) throws SQLException {
        return upsert(company, dbTeamMember, null);
    }

    public String upsert(String company, DBTeamMember dbTeamMember, UUID integUserId) throws SQLException {
        if (integUserId == null) {
            return String.valueOf(insertTeamMember(company, dbTeamMember));
        }
        if (getId(company, integUserId).isPresent())
            return null;
        UUID teamMemberId = insertTeamMember(company, dbTeamMember);
        if (Objects.isNull(teamMemberId)) return null;
        insertUsernames(company, teamMemberId, integUserId);
        return teamMemberId.toString();
    }

    private UUID insertTeamMember(String company, DBTeamMember dbTeamMember) {
        log.info("Inserting team member - integ user mapping for company {} teamMember {}", company, dbTeamMember);
        var keyHolder = new GeneratedKeyHolder();
        int count = this.template.update(
                MessageFormat.format(INSERT_TEAM_MEMBER_SQL_FORMAT, company, TEAM_MEMBERS_TABLE_NAME),
                new MapSqlParameterSource()
                        .addValue("fullName", dbTeamMember.getFullName())
                        .addValue("email", StringUtils.isNotEmpty(dbTeamMember.getEmail()) ? dbTeamMember.getEmail().toLowerCase() : null),
                keyHolder,
                new String[]{"id"}
        );
        return count == 0 ? null : (UUID) keyHolder.getKeys().get("id");
    }

    public Integer insertUsernames(String company, final UUID teamMemberId, UUID integUserId) throws SQLException {
        var keyHolder = new GeneratedKeyHolder();
        return this.template.update(
                MessageFormat.format(INSERT_TEAM_MEMBER_USERNAMES_SQL_FORMAT, company, TEAM_MEMBER_USERNAMES_TABLE_NAME),
                new MapSqlParameterSource()
                        .addValue("teamMemberId", teamMemberId)
                        .addValue("integrationUserId", integUserId),
                keyHolder,
                new String[]{"id"}
        );
    }

    @Override
    public Boolean update(String company, DBTeamMember t) throws SQLException {
        var keyHolder = new GeneratedKeyHolder();
        int updateCount = this.template.update(
                MessageFormat.format(UPDATE_TEAM_MEMBER_SQL_FORMAT, company, TEAM_MEMBERS_TABLE_NAME),
                new MapSqlParameterSource()
                        .addValue("fullName", t.getFullName())
                        .addValue("email", t.getEmail())
                        .addValue("id", t.getId()),
                keyHolder,
                new String[]{"id"}
        );
        if(updateCount>0) {
            return true;
        }
        return false;
    }

    @Override
    public Optional<DBTeamMember> get(String company, String teamMemberId) throws SQLException {
        return get(company, UUID.fromString(teamMemberId));
    }

    public Optional<DBTeamMember> get(String company, UUID teamMemberId) throws SQLException {
        var results = filter(company, QueryFilter.builder().strictMatch("team_member_id", teamMemberId).build(), 0, 1);
        if (results.getCount() > 0) {
            return Optional.of(results.getRecords().get(0));
        }
        return Optional.empty();
    }

    public Optional<TeamMemberId> getId(String company, UUID insertedId) {
        var conditions = new ArrayList<String>();
        var params = new MapSqlParameterSource();
        var prefix = "tmu";
        processCondition(prefix, "integration_user_id", insertedId, conditions, params);
        var sql = MessageFormat.format(BASE_SELECT_FROM_TEAM_USERS, company, TEAM_MEMBER_USERNAMES_TABLE_NAME, String.join(" AND ", conditions));
        List<TeamMemberId> teamMemberIds = template.query(sql, params, teamMemberUserMapping());
        if (teamMemberIds.size() > 0) {
            return Optional.of(teamMemberIds.get(0));
        }
        return Optional.empty();
    }

    @Override
    public DbListResponse<DBTeamMember> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return filter(company, null, pageNumber, pageSize);
    }

    public DbListResponse<DBTeamMember> filter(
            final String company,
            final QueryFilter filters,
            final Integer pageNumber,
            Integer pageSize) throws SQLException {
        var params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();

        populateConditions(company, filters, conditions, params);
        var extraConditions = conditions.size() > 0 ? " WHERE " + String.join(" AND ", conditions) + " " : "";

        var extraFrom = "";
        var extraGrouping = "";

        if (extraConditions.contains("tmu")) {
            extraFrom = MessageFormat.format(", {0}.{1} tmu", company, TEAM_MEMBER_USERNAMES_TABLE_NAME);
            extraConditions += " AND tmu.team_member_id = tm.id ";
            extraGrouping = ", tmu.team_member_id";
        }

        var sql = MessageFormat.format(BASE_SELECT, company, TEAM_MEMBERS_TABLE_NAME, TEAM_MEMBER_USERNAMES_TABLE_NAME, USERS_TABLE,
                extraFrom, extraConditions, extraGrouping);
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        String limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize.toString(), String.valueOf(pageNumber * pageSize));
        List<DBTeamMember> items = template.query(sql + limit, params, (rs, row) -> {
            List<Map<String, Object>> projectsTmp = ParsingUtils.parseJsonList(mapper, "ids", rs.getString("ids"));
            return DBTeamMember.builder()
                    .id((UUID) rs.getObject("id"))
                    .email(rs.getString("email"))
                    .fullName(rs.getString("full_name"))
                    .ids(projectsTmp.stream()
                            .map(item -> DBTeamMember.LoginId.builder()
                                    .integrationId(Integer.valueOf(item.get("integration_id").toString()))
                                    .integrationType(item.get("application").toString())
                                    .username(item.get("display_name").toString())
                                    .cloudId(item.get("cloud_id"))
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

        for (Map.Entry<String, Object> entry : filters.getStrictMatches().entrySet()) {
            var item = entry.getKey();
            var values = entry.getValue();
            if (!allowedFilters.contains(item)
                    || filters.getStrictMatches().get(item) == null) {
                continue;
            }
            var prefix = "";
            switch (item) {
                case "team_member_id":
                    prefix = "tmu";
                    break;
                case "email":
                case "full_name":
                    prefix = "tm";
                    break;
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
        } else if (values instanceof Integer) {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}::int", prefix, item));
        } else if (values instanceof Long) {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}::bigint", prefix, item));
        } else {
            conditions.add(MessageFormat.format("{0}.{1} = :{1}", prefix, item));
        }
        params.addValue(item, values.toString());
    }

    public static RowMapper<TeamMemberId> teamMemberUserMapping() {
        return (rs, rowNumber) -> TeamMemberId.builder()
                .teamMemberId(rs.getString("team_member_id"))
                .build();
    }

    public static RowMapper<TeamMemberDTO> teamMemberMapping() {
        return (rs, rowNumber) -> TeamMemberDTO.builder()
                .id(UUID.fromString(rs.getString("id")))
                .fullName(rs.getString("full_name"))
                .email("email")
                .build();
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
    //     return DbListResponse.of(items, total);
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
        var sql = MessageFormat.format(DELETE_TEAM_MEMBERS_SQL_FORMAT, company, TEAM_MEMBERS_TABLE_NAME, String.join(" AND ", conditions));
        var updates = this.template.update(sql, params);
        if (ids.size() != updates) {
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
                        TEAM_MEMBERS_TABLE_NAME,
                        TEAM_MEMBER_USERNAMES_TABLE_NAME)));
        return true;
    }

}
