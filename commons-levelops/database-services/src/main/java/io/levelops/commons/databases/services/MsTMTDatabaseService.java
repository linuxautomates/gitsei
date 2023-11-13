package io.levelops.commons.databases.services;

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

import javax.sql.DataSource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.plugins.MsTmtVulnerability;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class MsTMTDatabaseService extends DatabaseService<MsTmtVulnerability> {
    public final static String REPORTS_TABLE_NAME = "ms_tmt_reports";
    public final static String ISSUES_TABLE_NAME = "ms_tmt_issues";

    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper mapper;

    private final static List<String> ddl = List.of( 
        "CREATE TABLE IF NOT EXISTS {0}.{1} (" +
        "    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
        "    ingested_at         bigint," +
        "    created_at          varchar(100)," +
        "    model               varchar(200)," +
        "    owner               varchar(200)," +
        "    plugin_result_id    UUID NOT NULL REFERENCES {0}.{3}(id) ON DELETE CASCADE" +
        ");",
        
        "CREATE INDEX IF NOT EXISTS {1}_ingested_at ON {0}.{1}(ingested_at)",
        
        "CREATE INDEX IF NOT EXISTS {1}_created_at ON {0}.{1}(lower(created_at))",

        "CREATE INDEX IF NOT EXISTS {1}_model ON {0}.{1}(lower(model))",

        "CREATE TABLE IF NOT EXISTS {0}.{2} (" +
        "    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
        "    category       VARCHAR(80) NOT NULL," +
        "    priority       VARCHAR(50) NOT NULL," +
        "    name           VARCHAR(200) NOT NULL," +
        "    state          VARCHAR(50)," +
        "    description    TEXT," +
        "    extra_data     JSON," +
        "    report_id      UUID NOT NULL REFERENCES {0}.{1} (id) ON DELETE CASCADE" +
        ");",
        
        "CREATE INDEX IF NOT EXISTS {2}_category ON {0}.{2}(lower(category))",
        
        "CREATE INDEX IF NOT EXISTS {2}_priority ON {0}.{2}(lower(priority))",
        
        "CREATE INDEX IF NOT EXISTS {2}_state ON {0}.{2}(lower(state))");
    
    private final static String SELECT_REPORT_ID = 
        "SELECT id " + 
        "FROM " + 
            "{0}.{1} " + 
        "WHERE " +
            "model = :model AND " +
            "owner = :owner AND " +
            "created_at = :createdAt AND " +
            "ingested_at = :ingestedAt ";
    
    private final static String BASE_FROM = 
        " FROM  " +
        "    {0}.{2} i," +
        "    {0}.{1} r" +
        "    {3}" +
        " WHERE " +
        "    i.report_id = r.id {4} ";
    
    private final static String BASE_SELECT = 
        "SELECT " +
        "    i.id," +
        "    i.category," +
        "    i.priority," +
        "    i.name," +
        "    i.state," +
        "    i.description," +
        "    i.extra_data," +
        "    r.id report_id," +
        "    r.model," +
        "    r.owner," +
        "    r.created_at," +
        "    r.ingested_at," +
        "    to_json(ARRAY(SELECT row_to_json(p) FROM (SELECT p.id,p.name FROM {0}.products p, {0}.component_product_mappings m WHERE m.component_type = ''plugin_result'' AND m.component_id = r.plugin_result_id AND p.id = m.product_id) AS p)) projects," +
        "    to_json(ARRAY(SELECT row_to_json(y) FROM (SELECT ta.id, ta.name FROM {0}.tagitems t, {0}.tags ta WHERE t.itemtype = ''PLUGIN_RESULT'' AND t.itemid = r.plugin_result_id::text AND ta.id = t.tagid)AS y)) tags" +
        BASE_FROM;
    
    private final static String SELECT_PLUGIN_RESULT_IDS =
        "SELECT" +
        "    r.plugin_result_id " +
        "FROM " +
        "    {0}.component_product_mappings m," +
        "    {0}.{1} r" +
        "    {2} " +
        "WHERE " +
        "    {3}" +
        "    m.component_type = ''plugin_result'' " +
        "    AND m.component_id = r.plugin_result_id " +
        "    {4}" +
        "    {5}";

    private final static String INSERT_REPORT_SQL_FORMAT = "INSERT INTO {0}.ms_tmt_reports(id, model, owner, created_at, ingested_at, plugin_result_id) VALUES(:id, :model, :owner, :createdAt, :ingestedAt, :pluginResultId)";
    private final static String INSERT_ISSUE_SQL_FORMAT = "INSERT INTO {0}.ms_tmt_issues(id, name, category, priority, state, description, extra_data, report_id) VALUES(:id, :name, :category, :priority, :state, :description, :extra_data::jsonb, :reportId::uuid)";

    private static final Set<String> allowedFilters = Set.of("n_last_reports", "ingested_at", "category", "priority", "model", "project", "tag");
    private static final int DEFAULT_PAGE_SIZE = 10;

    protected MsTMTDatabaseService(final DataSource dataSource, final ObjectMapper mapper) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.mapper = mapper;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(PluginResultsDatabaseService.class);
    }

    @Override
    public String insert(String company, MsTmtVulnerability item) throws SQLException {
        UUID id = item.getId() != null ? item.getId() : UUID.randomUUID();

        var reportId = insertReport(company, item);

        var params = new MapSqlParameterSource()
                    .addValue("id", id)
                    .addValue("name", item.getName())
                    .addValue("category", item.getCategory().toLowerCase())
                    .addValue("priority", item.getPriority().toLowerCase())
                    .addValue("state", item.getState().toLowerCase())
                    .addValue("description", StringUtils.defaultString(item.getDescription()))
                    .addValue("reportId", reportId);

        try {
            params.addValue("extra_data", item.getExtraData() != null ? mapper.writeValueAsString(item.getExtraData()) : "{}");
        } catch (JsonProcessingException e) {
            throw new SQLException("Unable to parse the ms tmt extra data", e);
        }

        this.template.update(MessageFormat.format(INSERT_ISSUE_SQL_FORMAT, company, ISSUES_TABLE_NAME), params);
        return id.toString();
    }

    private String insertReport(String company, MsTmtVulnerability item) throws SQLException {
        var params = new MapSqlParameterSource()
                    .addValue("model", item.getModel())
                    .addValue("owner", item.getOwner())
                    .addValue("createdAt", item.getCreatedAt())
                    .addValue("ingestedAt", item.getIngestedAt())
                    .addValue("pluginResultId", item.getPluginResultId());
    
        var query = MessageFormat.format(SELECT_REPORT_ID, company, REPORTS_TABLE_NAME);
        try {
            UUID id = template.queryForObject(query, params, UUID.class);
            return id.toString();
        }
        catch (EmptyResultDataAccessException e){
            log.debug("No report found for {}, inserting a new record... ", params);
        }

        // TODO: address possible race condition...
        UUID id = item.getId() != null ? item.getId() : UUID.randomUUID();
        params.addValue("id", id);

        this.template.update(MessageFormat.format(INSERT_REPORT_SQL_FORMAT, company, REPORTS_TABLE_NAME), params);
        return id.toString();
    }

    @Override
    public Boolean update(String company, MsTmtVulnerability t) throws SQLException {
        throw new NotImplementedException();
    }

    @Override
    public Optional<MsTmtVulnerability> get(String company, String param) throws SQLException {
        throw new NotImplementedException();
    }

    @Override
    public DbListResponse<MsTmtVulnerability> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        return filter(company, null, pageNumber, pageSize);
    }

    public DbListResponse<MsTmtVulnerability> filter(
        final String company,
        final QueryFilter filters,
        final Integer pageNumber,
        Integer pageSize) throws SQLException {
        var params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();

        populateConditions(company, filters, conditions, params);

        var extraConditions = conditions.size() > 0 ? " AND " + String.join(" AND ", conditions) + " " : "";

        var sql = MessageFormat.format(BASE_SELECT, company, REPORTS_TABLE_NAME, ISSUES_TABLE_NAME, "", extraConditions);
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        String limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize.toString(), String.valueOf(pageNumber*pageSize));
        List<MsTmtVulnerability> items = template.query(sql + limit, params, (rs, row) -> {
            List<Map<String, Object>> projectsTmp = ParsingUtils.parseJsonList(mapper, "projects", rs.getString("projects"));
            List<Map<String, Object>> tagsTmp = ParsingUtils.parseJsonList(mapper, "tags", rs.getString("tags"));
            return MsTmtVulnerability.builder()
                .category(rs.getString("category"))
                .priority(rs.getString("priority"))
                .name(rs.getString("name"))
                .owner(rs.getString("owner"))
                .reportId((UUID)rs.getObject("report_id"))
                .state(rs.getString("state"))
                .description(rs.getString("description"))
                .createdAt(rs.getString("created_at"))
                .ingestedAt(rs.getInt("ingested_at"))
                .projects(List.copyOf(projectsTmp.stream()
                    .map(item -> Map.of(item.get("id").toString(), (String) item.get("name")))
                    .collect(Collectors.toSet())))
                .tags(List.copyOf(tagsTmp.stream()
                    .map(item -> Map.of(item.get("id").toString(), (String) item.get("name")))
                    .collect(Collectors.toSet())))
                .extraData(ParsingUtils.parseJsonObject(mapper, "extra_data", rs.getString("extra_data")))
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
                || filters.getStrictMatches().get(item) == null
                || "n_last_reports".equalsIgnoreCase(item)
                || "project".equalsIgnoreCase(item)
                || "tag".equalsIgnoreCase(item)) {
                continue;
            }
            var prefix = "ingested_at".equalsIgnoreCase(item) || "model".equalsIgnoreCase(item) ? "r" : "i";
            processCondition(prefix, item, values, conditions, params);
        }

        var projects = filters.getStrictMatches().getOrDefault("project", Set.of());
        var tags = filters.getStrictMatches().getOrDefault("tag", Set.of());
        var lastNReports = filters.getStrictMatches().getOrDefault("n_last_reports", null);
        if (ObjectUtils.isNotEmpty(projects) || ObjectUtils.isNotEmpty(tags) || lastNReports instanceof Integer) {
            try {
                var values = getPluginResultIds(company, projects, tags, lastNReports);
                processCondition("r", "plugin_result_id", values, conditions, params);
            }
            catch(Exception e){
                log.error("Unable to get report ids from: projects={}, tags={}, n_last_reports={}", projects, tags, lastNReports, e);
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

    private Set<String> getPluginResultIds(final String company, final Object projects, final Object tags, final Object lastNReports) {
        List<String> conditions = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();
        if(ObjectUtils.isNotEmpty(projects)) {
            processCondition("m", "product_id", projects, conditions, params);
        }
        var queryTags = "LEFT OUTER JOIN {0}.tagitems ti ON ti.itemid = r.plugin_result_id::text AND ti.itemtype = ''PLUGIN_RESULT''";
        var tagsParams = "";
        if(ObjectUtils.isNotEmpty(tags)) {
            processCondition("ti", "tagid", tags, conditions, params);
            queryTags = ", {0}.tagitems ti ";
            tagsParams = "ti.itemid = r.plugin_result_id::text AND ti.itemtype = 'PLUGIN_RESULT' AND ";
        }
        queryTags = MessageFormat.format(queryTags, company);
        var extraConditions = conditions.size() > 0 ? " AND " + String.join(" AND ", conditions) + " " : "";
        var orderAndLimit = "";
        if (lastNReports instanceof Integer) {
            orderAndLimit += " ORDER BY r.ingested_at DESC LIMIT " + lastNReports;
        }
        var query = MessageFormat.format(SELECT_PLUGIN_RESULT_IDS, company, REPORTS_TABLE_NAME, queryTags, tagsParams, extraConditions, orderAndLimit);
        log.debug("query: {}", query);
        return template.queryForList(query, params, UUID.class).stream().map(UUID::toString).collect(Collectors.toSet());
    }

    public DbListResponse<Map<String, Object>> getValues(final String company, final String field, final QueryFilter filters, final Integer pageNumber, Integer pageSize){
        var params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        switch(field) {
            case "priority":
            case "category":
            case "model":
                populateConditions(company, filters, conditions, params);

                var extraConditions = conditions.size() > 0 ? " AND " + String.join(" AND ", conditions) + " " : "";

                var prefix = "model".equalsIgnoreCase(field) ? "r" : "i";
                var sql = MessageFormat.format("SELECT DISTINCT({5}.{6}) AS v " + BASE_FROM, company, REPORTS_TABLE_NAME, ISSUES_TABLE_NAME, "", extraConditions, prefix, field);
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
            case "project":
            case "tag":
                var extraFrom = "project".equalsIgnoreCase(field) ? ", {0}.products c, {0}.component_product_mappings m" : ", {0}.tags c, {0}.tagitems ti";
                populateConditions(company, filters, conditions, params);
                extraConditions = "project".equalsIgnoreCase(field) 
                    ? "AND m.component_id = r.plugin_result_id AND c.id = m.product_id" 
                    : "AND ti.itemid = r.plugin_result_id::text AND c.id = ti.tagid";
                extraConditions += conditions.size() > 0 ? " AND " + String.join(" AND ", conditions) + " " : "";
                sql = MessageFormat.format(BASE_FROM, company, REPORTS_TABLE_NAME, ISSUES_TABLE_NAME, extraFrom, "{1}");
                sql = MessageFormat.format("SELECT c.id, c.name " + sql, company, extraConditions);
                var query = MessageFormat.format("SELECT row_to_json(l) AS v FROM ({1}) AS l ", company, sql);

                if (pageSize == null || pageSize < 1) {
                    pageSize = DEFAULT_PAGE_SIZE;
                }
                limit = MessageFormat.format(" LIMIT {0} OFFSET {1}", pageSize.toString(), String.valueOf(pageNumber*pageSize));
                items = List.copyOf(Set.copyOf(template.query(query + limit, params, (rs, row) -> {
                    var vals = ParsingUtils.parseJsonObject(mapper, "name", rs.getString("v"));
                    return Map.of("key", vals.get("id"), "value", vals.get("name"));
                })));
                totalCount = items.size();
                if (totalCount == pageSize) {
                    totalCount = template.queryForObject("SELECT COUNT(*) FROM (" + sql + ") AS l", params, Integer.class);
                }
                return DbListResponse.of(items, totalCount);
            default:
                return null;
        }
        // return DbListResponse.of(items, total);
    }

    public DbListResponse<Map<String, Object>> aggregate(final String company, final String pivot, final QueryFilter filters, final int pageNumber, Integer pageSize) {
        var params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        populateConditions(company, filters, conditions, params);
        String extraConditions = "";
        if (conditions.size() > 0) {
            extraConditions = "AND " + String.join(" AND ", conditions) + " ";
        }
        var extraFrom = "";
        var field = "";
        if ("category".equalsIgnoreCase(pivot) || "priority".equalsIgnoreCase(pivot)) {
            field = "i." + pivot;
        }
        else if ("project".equalsIgnoreCase(pivot)){
            extraFrom = MessageFormat.format(", {0}.products p, {0}.component_product_mappings m", company);
            extraConditions = " AND p.id = m.product_id AND r.plugin_result_id = m.component_id AND m.component_type = 'plugin_result' " + extraConditions;
            field = "p.name";
        }
        else if ("tag".equalsIgnoreCase(pivot)){
            extraFrom = MessageFormat.format(", {0}.tags t, {0}.tagitems ti", company);
            extraConditions = " AND t.id = ti.tagid AND r.plugin_result_id::text = ti.itemid AND ti.itemtype = 'PLUGIN_RESULT' " + extraConditions;
            field = "t.name";
        }
        else {
            return DbListResponse.of(List.of(), 0);
        }
        var sql = MessageFormat.format(
            "SELECT " + 
                "{5} as key, " + 
                "COUNT(*) as count" + 
            BASE_FROM + 
            "GROUP BY " +
                "{5} ",
            company,
            REPORTS_TABLE_NAME,
            ISSUES_TABLE_NAME,
            extraFrom,
            extraConditions,
            field);
        
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        String limit = MessageFormat.format("LIMIT {0} OFFSET {1}", pageSize.toString(), String.valueOf(pageNumber*pageSize));

        var records = template.query(
            "SELECT " + 
            "   row_to_json(l) as aggregations " + 
            "FROM (" + sql + limit +") as l", params, (rs, row) -> {
            var aggregations = ParsingUtils.parseMap(mapper, "aggregations", String.class, Object.class, rs.getString("aggregations"));
            return aggregations;
        });
        var totalCount = records.size();
        if (totalCount == pageSize) {
            totalCount = template.queryForObject(MessageFormat.format("SELECT count(*) FROM ({0}) as a", sql), params, Integer.class);
        }
        return DbListResponse.of(records, totalCount);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new NotImplementedException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        ddl.forEach(item -> template.getJdbcTemplate()
            .execute(MessageFormat.format(
                item,
                company,
                REPORTS_TABLE_NAME,
                ISSUES_TABLE_NAME,
                PluginResultsDatabaseService.RESULTS_TABLE)));
        return true;
    }
    
}
