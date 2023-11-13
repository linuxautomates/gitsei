package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.kudos.DBKudos;
import io.levelops.commons.databases.models.database.kudos.DBKudosSharing;
import io.levelops.commons.databases.models.database.kudos.DBKudosWidget;
import io.levelops.commons.databases.models.database.kudos.DBKudosSharing.KudosSharingType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.ZoneId;
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
public class KudosDatabaseService extends DatabaseService<DBKudos> {
    public final static String KUDOS_TABLE_NAME = "kudos";
    public final static String KUDOS_WIDGETS_TABLE_NAME = "kudos_widgets";
    public final static String KUDOS_SHARINGS_TABLE_NAME = "kudos_sharings";
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper mapper;

    private final static List<String> ddl = List.of( 
        "CREATE TABLE IF NOT EXISTS {0}.{1} (" + // kudos
        "    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4()," + 
        "    dashboard_id                UUID NOT NULL," + 
        "    screenshot_id               UUID," + 
        "    level                       VARCHAR NOT NULL," + 
        "    author                      VARCHAR NOT NULL," + 
        "    type                        VARCHAR NOT NULL," + 
        "    icon                        VARCHAR NOT NULL," + 
        "    breadcrumbs                 VARCHAR NOT NULL," + 
        "    expiration                  TIMESTAMPTZ," + 
        "    anonymous_link              BOOLEAN NOT NULL DEFAULT false," + 
        "    body                        TEXT NOT NULL," + 
        "    include_widget_details      BOOLEAN NOT NULL DEFAULT false," + 
        "    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now()," + 
        "    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now()" + 
        ");",
        
        "CREATE UNIQUE INDEX IF NOT EXISTS {1}_anonymous_link_uniq_idx ON {0}.{1}(anonymous_link)", 
        "CREATE INDEX IF NOT EXISTS {1}_dashboard_id_idx ON {0}.{1} (dashboard_id)",
        "CREATE INDEX IF NOT EXISTS {1}_author_idx ON {0}.{1}(author)",
        "CREATE INDEX IF NOT EXISTS {1}_type_idx ON {0}.{1}(type)",
        "CREATE INDEX IF NOT EXISTS {1}_expiration_idx ON {0}.{1}(expiration)",
        "CREATE INDEX IF NOT EXISTS {1}_created_at_idx ON {0}.{1}(created_at)",
        "CREATE INDEX IF NOT EXISTS {1}_updated_at_idx ON {0}.{1}(updated_at)",

        "CREATE TABLE IF NOT EXISTS {0}.{2} (" + // kudos widget
        "    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
        "    kudos_id         UUID REFERENCES {0}.{1}(id) ON DELETE CASCADE," + // kudos
        "    widget_id        UUID REFERENCES {0}.widgets(id) ON DELETE CASCADE," + 
        "    screenshot_id    UUID," + 
        "    position         SMALLINT NOT NULL," +
        "    size             SMALLINT NOT NULL," +
        "    data             JSONB," +
        "    UNIQUE(kudos_id, widget_id)" +
        ");",

        "CREATE TABLE IF NOT EXISTS {0}.{3} (" + // kudos widget
        "    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
        "    kudos_id         UUID REFERENCES {0}.{1}(id) ON DELETE CASCADE," + // kudos
        "    type             VARCHAR(6) NOT NULL," + // slack, email
        "    target           VARCHAR(40) NOT NULL," + // slack channel or email
        "    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()" + 
        ");"
        );
    
    private final static String INSERT_KUDOS_SQL_FORMAT = "INSERT INTO {0}.{1}(id, level, dashboard_id, screenshot_id, author, type, icon, breadcrumbs, expiration, anonymous_link, body, include_widget_details, created_at, updated_at) VALUES(:id,:level,:dashboardId,:screenshotId,:author,:type,:icon,:breadcrumbs,:expiration,:anonymousLink,:body,:includeWidgetDetails,now(),now())";
    private final static String INSERT_KUDOS_WIDGETS_SQL_FORMAT = "INSERT INTO {0}.{1}(kudos_id, widget_id, screenshot_id, position, size, data) VALUES(:kudosId,:widgetId,:screenshotId,:position,:size,:data::jsonb)";
    private final static String INSERT_KUDOS_SHARINGS_SQL_FORMAT = "INSERT INTO {0}.{1}(kudos_id, type, target) VALUES(:kudosId,:type,:target)";
    private final static String BASE_SELECT = "SELECT k.*, to_json(ARRAY(SELECT row_to_json(m) FROM (SELECT * FROM {0}.{2} WHERE kudos_id = k.id) AS m)) widgets FROM {0}.{1} k {3} {4}";
    private final static String KUDOS_WIDGETS_SELECT = "SELECT to_json(ARRAY(SELECT row_to_json(m) FROM (SELECT * FROM {0}.{1} WHERE kudos_id = ''{2}'') AS m)) widgets";
    private final static String BASE_FROM = "SELECT * FROM {0}.{1} {2}";

    private static final Set<String> allowedFilters = Set.of("id", "kudos_id", "widget_id", "type", "dashboard_id", "anonymous_link", "level", "author", "created_at", "updated_at");

    @Autowired
    public KudosDatabaseService(final ObjectMapper mapper, final DataSource dataSource) {
        super(dataSource);
        this.mapper = mapper;
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(DashboardWidgetService.class);
    }

    @Override
    public String insert(String company, DBKudos item) throws SQLException {
        var id = insertForId(company, item);
        return id != null ? id.toString() : null;
    }

    public UUID insertForId(String company, DBKudos item) throws SQLException {
        var id = item.getId() != null ? item.getId() : UUID.randomUUID();
        // insert kudos
        int count = this.template.update(
            MessageFormat.format(INSERT_KUDOS_SQL_FORMAT, company, KUDOS_TABLE_NAME),
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("level", item.getLevel())
                .addValue("dashboardId", item.getDashboardId())
                .addValue("screenshotId", item.getScreenshotId())
                .addValue("author", item.getAuthor())
                .addValue("type", item.getType().toString())
                .addValue("icon", item.getIcon())
                .addValue("breadcrumbs", item.getBreadcrumbs())
                .addValue("expiration", item.getExpiration() != null ? item.getExpiration().atZone(ZoneId.of("UTC")).toLocalDateTime() : null)
                .addValue("anonymousLink", item.getAnonymousLink())
                .addValue("body", item.getBody())
                .addValue("includeWidgetDetails", item.getIncludeWidgetDetails())
        );
        id = count == 0 ? null : id;
        if (CollectionUtils.isEmpty(item.getWidgets())) {
            return id;
        }
        // insert kudos' widgets
        insertKudosWidgets(company, id, item.getWidgets());
        // insert sharings
        insertKudosSharings(company, id, item.getSharings());

        return id;
    }

    public void insertKudosWidgets(final String company, final UUID kudosId, Set<DBKudosWidget> widgets) {
        // insert kudos
        List<SqlParameterSource> items = Lists.newArrayList();
        widgets.stream().forEach(item -> {
            var params = new MapSqlParameterSource()
                .addValue("id", item.getId())
                .addValue("kudosId", kudosId)
                .addValue("widgetId", item.getWidgetId())
                .addValue("screenshotId", item.getScreenshotId())
                .addValue("position", item.getPosition())
                .addValue("size", item.getSize());
            if (item.getData() != null) {
                try {
                    params.addValue("data", DefaultObjectMapper.get().writeValueAsString(item.getData()));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
            items.add(params);
            
        });
        int[] count = this.template.batchUpdate(
            MessageFormat.format(INSERT_KUDOS_WIDGETS_SQL_FORMAT, company, KUDOS_WIDGETS_TABLE_NAME),
            items.toArray(new SqlParameterSource[]{})
        );
    }

    public Set<DBKudosWidget> getKudosWidgets(final String company, final UUID kudosId) {
        var sql = MessageFormat.format(KUDOS_WIDGETS_SELECT, company, KUDOS_WIDGETS_TABLE_NAME, kudosId.toString());
        var results = template.query(sql, Map.of(), (rs) -> {
            if (!rs.next()) {
                return Set.<DBKudosWidget>of();
            }
            Set<DBKudosWidget> widgets = ParsingUtils.parseSet(mapper, "widgets", DBKudosWidget.class, rs.getString("widgets"));
            return widgets;
        });
        return results;
    }

    private void insertKudosSharings(final String company, final UUID kudosId, Set<DBKudosSharing> sharings) {
        // insert kudos
        List<SqlParameterSource> items = Lists.newArrayList();
        sharings.stream().forEach(item -> {
                items.add(new MapSqlParameterSource()
                    .addValue("kudosId", kudosId)
                    .addValue("type", item.getType().toString())
                    .addValue("target", item.getTarget()));
            
        });
        int[] count = this.template.batchUpdate(
            MessageFormat.format(INSERT_KUDOS_SHARINGS_SQL_FORMAT, company, KUDOS_SHARINGS_TABLE_NAME),
            items.toArray(new SqlParameterSource[]{})
        );
    }

    public List<DBKudosSharing> getKudosSharings(final String company, final UUID kudosId) {
        var sql = MessageFormat.format("SELECT * FROM {0}.kudos_sharings WHERE kudos_id = ''{1}''", company, kudosId.toString());
        List<DBKudosSharing> items = template.query(sql, (rs, row) -> {
            return DBKudosSharing.builder()
                .id((UUID) rs.getObject("id"))
                .kudosId(kudosId)
                .type(KudosSharingType.fromString(rs.getString("type")))
                .target(rs.getString("target"))
                .createdAt(rs.getTimestamp("created_at").toInstant())
                .build();
        });
        return items;
    }

    @Override
    public Boolean update(String company, DBKudos item) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Optional<DBKudos> get(String company, String param) throws SQLException {
        var results = filter(company,QueryFilter.builder().strictMatch("id", UUID.fromString(param)).build(), 0, 1);
        return results != null && results.getCount() > 0 ? Optional.of(results.getRecords().get(0)) : Optional.empty();
    }

    @Override
    public DbListResponse<DBKudos> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        var results = filter(company, QueryFilter.builder().build(), 0, 1);
        return results;
    }

    public DbListResponse<DBKudos> filter(
        final String company,
        final QueryFilter filters,
        final Integer pageNumber,
        Integer pageSize) throws SQLException {
        var params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();

        populateConditions(company, filters, conditions, params);

        var extraConditions = conditions.size() > 0 ? " WHERE " + String.join(" AND ", conditions) + " " : "";
        var extraFrom = "";

        var sql = MessageFormat.format(BASE_SELECT, company, KUDOS_TABLE_NAME, KUDOS_WIDGETS_TABLE_NAME, extraFrom, extraConditions);
        // var sql = "";
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        String limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize.toString(), String.valueOf(pageNumber*pageSize));
        List<DBKudos> items = template.query(sql + limit, params, (rs, row) -> {
            // List<Map<String, Object>> widgets = ParsingUtils.parseJsonList(mapper, "widgets", rs.getString("widgets"));
            Set<DBKudosWidget> widgets = ParsingUtils.parseSet(mapper, "widgets", DBKudosWidget.class, rs.getString("widgets"));
            // return null;
            return DBKudos.builder()
                .id((UUID) rs.getObject("id"))
                .anonymousLink(rs.getBoolean("anonymous_link"))
                .author(rs.getString("author"))
                .body(rs.getString("body"))
                .breadcrumbs(rs.getString("breadcrumbs"))
                .icon(rs.getString("icon"))
                .includeWidgetDetails(rs.getBoolean("include_widget_details"))
                .level(rs.getString("level"))
                .type(rs.getString("type"))
                .dashboardId((UUID) rs.getObject("dashboard_id"))
                .expiration(rs.getTimestamp("expiration") != null ? rs.getTimestamp("expiration").toInstant() : null)
                .createdAt(rs.getTimestamp("created_at").toInstant())
                .updatedAt(rs.getTimestamp("updated_at").toInstant())
                .build();
        });
        var total = template.queryForObject("SELECT COUNT(*) FROM (" + sql + ") as l", params, Integer.class);
        return DbListResponse.of(items, total);
    }
    
    public DbListResponse<Map<String, Object>> getValues(final String company, String field, final QueryFilter filters, final Integer pageNumber, Integer pageSize){
        var params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        switch(field) {
            case "name":
                populateConditions(company, filters, conditions, params);

                var extraConditions = conditions.size() > 0 ? " WHERE " + String.join(" AND ", conditions) + " " : "";

                var prefix = "ou";
                var sql = MessageFormat.format("SELECT DISTINCT({2}.{3}) AS v " + BASE_FROM, company, KUDOS_TABLE_NAME, prefix, field, "", "", "", extraConditions);
                if (pageSize == null || pageSize < 1) {
                    pageSize = DEFAULT_PAGE_SIZE;
                }
                String limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize.toString(), String.valueOf(pageNumber*pageSize));
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
        // if (filters != null && MapUtils.isNotEmpty(filters.getPartialMatches()) && filters.getPartialMatches().containsKey("name")) {
        //     conditions.add(MessageFormat.format("ou.name ILIKE ''%{0}%''", filters.getPartialMatches().get("name")));
        // }
        // if(filters == null || MapUtils.isEmpty(filters.getStrictMatches()) || !(filters.getStrictMatches().containsKey("active") || filters.getStrictMatches().containsKey("version") || filters.getStrictMatches().containsKey("ou_id"))) {
        //     conditions.add("active = true");
        // }
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
                case "id":
                case "name":
                case "type":
                case "author":
                case "level":
                    prefix = "k";
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
        if (item.equalsIgnoreCase("version")) {
            conditions.add(MessageFormat.format("{0}.versions @> :version", prefix, item));
            try (var conn = this.template.getJdbcTemplate().getDataSource().getConnection();) {
                params.addValue("version", conn.createArrayOf("integer", new Integer[]{(Integer) values}));
            } catch (SQLException e) {
                log.error(e);
            }
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

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        ddl.forEach(item -> 
        template.getJdbcTemplate()
            .execute(MessageFormat.format(
                item,
                company,
                KUDOS_TABLE_NAME,
                KUDOS_WIDGETS_TABLE_NAME,
                KUDOS_SHARINGS_TABLE_NAME)));
        return true;
    }
    
}
