package io.levelops.commons.databases.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.RunbookTemplateConverters;
import io.levelops.commons.databases.models.database.runbooks.RunbookTemplate;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Log4j2
@Service
public class RunbookTemplateDatabaseService extends FilteredDatabaseService<RunbookTemplate, RunbookTemplateDatabaseService.RunbookTemplateFilter> {

    private static final int PAGE_SIZE = 25;
    private static final String DEFAULT_CATEGORY = "LevelOps";
    private final ObjectMapper objectMapper;
    @VisibleForTesting
    protected boolean populateData = true;

    @Autowired
    public RunbookTemplateDatabaseService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        this.objectMapper = mapper;
    }

    @Override
    public String insert(String company, RunbookTemplate t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    int insertBulk(String company, List<RunbookTemplate> templates, boolean upsert) throws SQLException {
        Map<String, Object> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        for (RunbookTemplate t : templates) {
            if (Strings.isEmpty(t.getName())) {
                log.warn("Ignoring bulk insert of template missing name: {}", t);
                continue;
            }
            String metadata;
            String data;
            try {
                metadata = objectMapper.writeValueAsString(MapUtils.emptyIfNull(t.getMetadata()));
                data = objectMapper.writeValueAsString(MapUtils.emptyIfNull(t.getData()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize template", e);
                continue;
            }
            int i = values.size();
            params.putAll(Map.of(
                    "hidden_" + i, BooleanUtils.isTrue(t.getHidden()),
                    "name_" + i, t.getName(),
                    "description_" + i, StringUtils.defaultString(t.getDescription()),
                    "category_" + i, StringUtils.defaultIfBlank(t.getCategory(), DEFAULT_CATEGORY),
                    "metadata_" + i, metadata,
                    "data_" + i, data
            ));
            values.add(MessageFormat.format("(:hidden_{0}, :name_{0}, :description_{0}, :category_{0}, :metadata_{0}::jsonb, :data_{0}::jsonb)", i));
        }

        if (values.isEmpty()) {
            log.warn("Skipping bulk insert: no valid data provided");
            return 0;
        }

        String sql = "INSERT INTO " + company + ".runbook_templates " +
                " (hidden, name, description, category, metadata, data) " +
                " VALUES " + String.join(", ", values);
        if (upsert) {
            sql += " ON CONFLICT(name, category) DO UPDATE SET " +
                    " hidden = EXCLUDED.hidden, " +
                    " name = EXCLUDED.name, " +
                    " description = EXCLUDED.description, " +
                    " category = EXCLUDED.category, " +
                    " metadata = EXCLUDED.metadata, " +
                    " data = EXCLUDED.data ";
        } else {
            sql += " ON CONFLICT(name, category) DO NOTHING ";
        }

        return template.update(sql, new MapSqlParameterSource(params));
    }

    @Override
    public Boolean update(String company, RunbookTemplate t) throws SQLException {
        throw new UnsupportedOperationException("Cannot update template");
    }

    @Override
    public Optional<RunbookTemplate> get(String company, String id) {
        String sql = "SELECT * FROM " + company + ".runbook_templates " +
                " WHERE id = :id::uuid " +
                " LIMIT 1 ";
        try {
            List<RunbookTemplate> results = template.query(sql, Map.of("id", id),
                    RunbookTemplateConverters.rowMapper(objectMapper));
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get template for id={}", id, e);
            return Optional.empty();
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = RunbookTemplateFilter.RunbookTemplateFilterBuilder.class)
    public static class RunbookTemplateFilter {
        @JsonProperty("ids")
        List<String> ids;
        @JsonProperty("categories")
        List<String> categories;
        @JsonProperty("partial_name")
        String partialName;
        @JsonProperty("description")
        String description;
        @JsonProperty("hidden")
        Boolean hidden;
    }

    @Override
    public DbListResponse<RunbookTemplate> filter(Integer pageNumber, Integer pageSize, String company, RunbookTemplateFilter filter) {
        int limit = MoreObjects.firstNonNull(pageSize, PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);
        filter = filter != null? filter : RunbookTemplateFilter.builder().build();

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        // -- name
        if (filter.getPartialName() != null) {
            conditions.add("name ILIKE :name");
            params.put("name", "%" + filter.getPartialName() + "%");
        }
        // -- description
        if (filter.getDescription() != null) {
            conditions.add("description ILIKE :description");
            params.put("description", "%" + filter.getDescription() + "%");
        }
        // -- ids
        if (CollectionUtils.isNotEmpty(filter.getIds())) {
            conditions.add("id::text IN (:ids)");
            params.put("ids", filter.getIds());
        }
        // -- categories
        if (CollectionUtils.isNotEmpty(filter.getCategories())) {
            conditions.add("category IN (:categories)");
            params.put("categories", filter.getCategories());
        }
        // -- hidden
        if (filter.getHidden() != null) {
            conditions.add("hidden = :hidden::boolean");
            params.put("hidden", filter.getHidden());
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + ".runbook_templates " +
                where +
                " ORDER BY name ASC " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<RunbookTemplate> results = template.query(sql, params, RunbookTemplateConverters.rowMapper(objectMapper));
        String countSql = "SELECT count(*) FROM " + company + ".runbook_templates " + where;
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException("Cannot delete template");
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".runbook_templates " +
                        "(" +
                        "   id           UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "   hidden       BOOLEAN NOT NULL DEFAULT false," +
                        "   name         TEXT NOT NULL," +
                        "   description  TEXT NOT NULL DEFAULT ''," +
                        "   category     VARCHAR(64) NOT NULL DEFAULT ''," +
                        "   metadata     JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "   data         JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "   created_at   TIMESTAMPTZ NOT NULL DEFAULT now()," +
                        "   CONSTRAINT   runbook_templates_unique_name_category UNIQUE(name, category)" +
                        ")"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);

        if (populateData) {
            populateDefaultData(company);
        }

        return true;
    }

    private void populateDefaultData(String company) throws SQLException {
        List<RunbookTemplate> templates = new ArrayList<>();
        Reflections reflections = new Reflections("db.default_data.runbook_templates", new ResourcesScanner());
        Set<String> resourceList = reflections.getResources(name -> name != null && name.endsWith(".json") && !name.equalsIgnoreCase("runbook_template_categories.json"));
        for (String resource : resourceList) {
            log.debug("Loading resource {}", resource);
            try {
                String resourceString = ResourceUtils.getResourceAsString(resource, RunbookTemplateDatabaseService.class.getClassLoader());
                List<RunbookTemplate> runbookTemplates = objectMapper.readValue(resourceString,
                        objectMapper.getTypeFactory().constructCollectionLikeType(List.class, RunbookTemplate.class));
                templates.addAll(runbookTemplates);
            } catch (Throwable e) {
                log.warn("Failed to load default data for runbook template at {}", resource, e);
            }
        }

        int rows = insertBulk(company, templates, true);
        log.info("Inserted default data for runbook templates ({} rows)", rows);
    }

}
