package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.RunbookNodeTemplateConverters;
import io.levelops.commons.databases.models.database.runbooks.RunbookNodeTemplate;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.util.Strings;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Log4j2
@Service
public class RunbookNodeTemplateDatabaseService extends DatabaseService<RunbookNodeTemplate> {

    private static final int PAGE_SIZE = 25;
    private static final String DEFAULT_CATEGORY = "LevelOps";
    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate template;
    @VisibleForTesting
    protected boolean populateData = true;

    @Autowired
    public RunbookNodeTemplateDatabaseService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        this.objectMapper = mapper;
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public String insert(String company, RunbookNodeTemplate t) throws SQLException {
        Validate.notBlank(t.getType(), "t.getType() cannot be null or empty.");
        Validate.notBlank(t.getNodeHandler(), "t.getNodeHandler() cannot be null or empty.");
        Validate.notBlank(t.getName(), "t.getName() cannot be null or empty.");
        String sql = "INSERT INTO " + company + ".runbook_node_templates " +
                "(type, node_handler, hidden, name, description, category, input, output, options, ui_data)" +
                " VALUES " +
                "(:type, :node_handler, :hidden, :name, :description, :category, :input::jsonb, :output::jsonb, :options::jsonb, :ui_data::jsonb)";

        Map<String, Object> params;
        try {
            params = Map.of(
                    "type", t.getType(),
                    "node_handler", t.getNodeHandler(),
                    "hidden", BooleanUtils.isTrue(t.getHidden()),
                    "name", t.getName(),
                    "description", StringUtils.defaultString(t.getDescription()),
                    "category", StringUtils.defaultIfBlank(t.getCategory(), DEFAULT_CATEGORY),
                    "input", objectMapper.writeValueAsString(MapUtils.emptyIfNull(t.getInput())),
                    "output", objectMapper.writeValueAsString(MapUtils.emptyIfNull(t.getOutput())),
                    "options", objectMapper.writeValueAsString(ListUtils.emptyIfNull(t.getOptions())),
                    "ui_data", objectMapper.writeValueAsString(MapUtils.emptyIfNull(t.getUiData()))
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize template to JSON", e);
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, new MapSqlParameterSource(params), keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return null;
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    int insertBulk(String company, List<RunbookNodeTemplate> templates, boolean upsert) throws SQLException {
        Map<String, Object> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        for (RunbookNodeTemplate t : templates) {
            if (Strings.isEmpty(t.getNodeHandler()) || Strings.isEmpty(t.getType()) || Strings.isEmpty(t.getName())) {
                log.warn("Ignoring bulk insert of template missing node_handler, type or name: {}", t);
                continue;
            }
            String input;
            String output;
            String options;
            String uiData;
            try {
                options = objectMapper.writeValueAsString(ListUtils.emptyIfNull(t.getOptions()));
                input = objectMapper.writeValueAsString(MapUtils.emptyIfNull(t.getInput()));
                output = objectMapper.writeValueAsString(MapUtils.emptyIfNull(t.getOutput()));
                uiData = objectMapper.writeValueAsString(MapUtils.emptyIfNull(t.getUiData()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize template", e);
                continue;
            }
            int i = values.size();
            params.putAll(Map.of(
                    "type_" + i, t.getType(),
                    "node_handler_" + i, t.getNodeHandler(),
                    "hidden_" + i, BooleanUtils.isTrue(t.getHidden()),
                    "name_" + i, t.getName(),
                    "description_" + i, StringUtils.defaultString(t.getDescription()),
                    "category_" + i, StringUtils.defaultIfBlank(t.getCategory(), DEFAULT_CATEGORY),
                    "input_" + i, input,
                    "output_" + i, output,
                    "options_" + i, options,
                    "ui_data_" + i, uiData
            ));
            values.add(MessageFormat.format("(:type_{0}, :node_handler_{0}, :hidden_{0}, :name_{0}, :description_{0}, :category_{0}, :input_{0}::jsonb, :output_{0}::jsonb, :options_{0}::jsonb, :ui_data_{0}::jsonb)", i));
        }

        if (values.isEmpty()) {
            log.warn("Skipping bulk insert: no valid data provided");
            return 0;
        }

        String sql = "INSERT INTO " + company + ".runbook_node_templates " +
                " (type, node_handler, hidden, name, description, category, input, output, options, ui_data) " +
                " VALUES " + String.join(", ", values);
        if (upsert) {
            sql += " ON CONFLICT(type) DO UPDATE SET " +
                    " node_handler = EXCLUDED.node_handler, " +
                    " hidden = EXCLUDED.hidden, " +
                    " name = EXCLUDED.name, " +
                    " description = EXCLUDED.description, " +
                    " category = EXCLUDED.category, " +
                    " input = EXCLUDED.input, " +
                    " output = EXCLUDED.output, " +
                    " options = EXCLUDED.options, " +
                    " ui_data = EXCLUDED.ui_data ";
        } else {
            sql += " ON CONFLICT(type) DO NOTHING ";
        }

        return template.update(sql, new MapSqlParameterSource(params));
    }

    @Override
    public Boolean update(String company, RunbookNodeTemplate t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<RunbookNodeTemplate> get(String company, String id) {
        String sql = "SELECT * FROM " + company + ".runbook_node_templates " +
                " WHERE id = :id::uuid " +
                " LIMIT 1 ";
        try {
            List<RunbookNodeTemplate> results = template.query(sql, Map.of("id", id),
                    RunbookNodeTemplateConverters.rowMapper(objectMapper));
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get node template for id={}", id, e);
            return Optional.empty();
        }
    }

    public DbListResponse<String> listCategories(String company) {
        String sql = "SELECT distinct(category) FROM " + company + ".runbook_node_templates " +
                " WHERE hidden = false " +
                " ORDER BY category ASC ";
        List<String> results = template.query(sql, Map.of(), (rs, row) -> rs.getString("category"));

        return DbListResponse.of(results, results.size());
    }

    @Override
    public DbListResponse<RunbookNodeTemplate> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return filter(pageNumber, pageSize, company, null, null, null, null, null);
    }

    public Stream<RunbookNodeTemplate> stream(String company, @Nullable List<String> types, @Nullable String name, @Nullable List<String> ids, @Nullable List<String> categories, @Nullable Boolean hidden) {
        return PaginationUtils.stream(0, 1, page -> filter(page, PAGE_SIZE, company, types, name, ids, categories, hidden).getRecords());
    }

    public DbListResponse<RunbookNodeTemplate> filter(Integer pageNumber, Integer pageSize, String company, @Nullable List<String> types, @Nullable String name, @Nullable List<String> ids, @Nullable List<String> categories, @Nullable Boolean hidden) {
        int limit = MoreObjects.firstNonNull(pageSize, PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        // -- type
        if (CollectionUtils.isNotEmpty(types)) {
            conditions.add("type::text IN (:types)");
            params.put("types", types);
        }
        // -- name
        if (name != null) {
            conditions.add("name ILIKE :name");
            params.put("name", "%" + name + "%");
        }
        // -- ids
        if (CollectionUtils.isNotEmpty(ids)) {
            conditions.add("id::text IN (:ids)");
            params.put("ids", ids);
        }
        // -- ids
        if (CollectionUtils.isNotEmpty(categories)) {
            conditions.add("category::text IN (:categories)");
            params.put("categories", categories);
        }
        // -- hidden
        if (hidden != null) {
            conditions.add("hidden = :hidden::boolean");
            params.put("hidden", hidden);
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + ".runbook_node_templates " +
                where +
                " ORDER BY name ASC " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<RunbookNodeTemplate> results = template.query(sql, params, RunbookNodeTemplateConverters.rowMapper(objectMapper));
        String countSql = "SELECT count(*) FROM " + company + ".runbook_node_templates " + where;
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String sql = "DELETE " +
                " FROM " + company + ".runbook_node_templates" +
                " WHERE id = :id::uuid";

        return template.update(sql, Map.of(
                "id", id
        )) > 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".runbook_node_templates " +
                        "(" +
                        "   id           UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "   type         VARCHAR(64) NOT NULL UNIQUE," +
                        "   node_handler VARCHAR(64) NOT NULL," +
                        "   hidden       BOOLEAN NOT NULL DEFAULT false," +
                        "   name         TEXT NOT NULL," +
                        "   description  TEXT NOT NULL DEFAULT ''," +
                        "   category     TEXT NOT NULL," +
                        "   input        JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "   output       JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "   options      JSONB NOT NULL DEFAULT '[]'::jsonb," +
                        "   ui_data      JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "   created_at   TIMESTAMPTZ NOT NULL DEFAULT now()" +
                        ")",

                "CREATE INDEX IF NOT EXISTS runbook_node_templates__type_idx on " + company + "." + "runbook_node_templates (type)",
                "CREATE INDEX IF NOT EXISTS runbook_node_templates__category_idx on " + company + "." + "runbook_node_templates (category)"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);

        if (populateData) {
            populateDefaultData(company);
        }

        return true;
    }

    private void populateDefaultData(String company) throws SQLException {
        List<RunbookNodeTemplate> templates = new ArrayList<>();
        Reflections reflections = new Reflections("db.default_data.runbook_node_templates", new ResourcesScanner());
        Set<String> resourceList = reflections.getResources(name -> name != null && name.endsWith(".json"));
        for (String resource : resourceList) {
            log.debug("Loading resource {}", resource);
            try {
                String resourceString = ResourceUtils.getResourceAsString(resource, RunbookNodeTemplateDatabaseService.class.getClassLoader());
                List<RunbookNodeTemplate> nodeTemplates = objectMapper.readValue(resourceString,
                        objectMapper.getTypeFactory().constructCollectionLikeType(List.class, RunbookNodeTemplate.class));
                templates.addAll(nodeTemplates);
            } catch (Throwable e) {
                log.warn("Failed to load default data for runbook node template at {}", resource, e);
            }
        }

        int rows = insertBulk(company, templates, true);
        log.info("Inserted default data for runbook node templates ({} rows)", rows);
    }

}
