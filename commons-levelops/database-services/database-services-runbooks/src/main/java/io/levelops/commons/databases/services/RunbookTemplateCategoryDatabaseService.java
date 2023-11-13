package io.levelops.commons.databases.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.RunbookTemplateCategoryConverters;
import io.levelops.commons.databases.models.database.runbooks.RunbookTemplateCategory;
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

@Log4j2
@Service
public class RunbookTemplateCategoryDatabaseService extends FilteredDatabaseService<RunbookTemplateCategory, RunbookTemplateCategoryDatabaseService.RunbookTemplateCategoryFilter> {

    private static final String DEFAULT_CATEGORY = "LevelOps";
    private final ObjectMapper objectMapper;
    @VisibleForTesting
    protected boolean populateData = true;

    @Autowired
    public RunbookTemplateCategoryDatabaseService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        this.objectMapper = mapper;
    }

    @Override
    public String insert(String company, RunbookTemplateCategory t) throws SQLException {
        throw new UnsupportedOperationException("Only bulk insert is supported");
    }

    int insertBulk(String company, List<RunbookTemplateCategory> templates, boolean upsert) throws SQLException {
        Map<String, Object> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        for (RunbookTemplateCategory t : templates) {
            if (Strings.isEmpty(t.getName())) {
                log.warn("Ignoring bulk insert of template category missing name: {}", t);
                continue;
            }
            String metadata;
            try {
                metadata = objectMapper.writeValueAsString(MapUtils.emptyIfNull(t.getMetadata()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize template category", e);
                continue;
            }
            int i = values.size();
            params.putAll(Map.of(
                    "hidden_" + i, BooleanUtils.isTrue(t.getHidden()),
                    "name_" + i, t.getName(),
                    "description_" + i, StringUtils.defaultString(t.getDescription()),
                    "metadata_" + i, metadata
            ));
            values.add(MessageFormat.format("(:hidden_{0}, :name_{0}, :description_{0}, :metadata_{0}::jsonb)", i));
        }

        if (values.isEmpty()) {
            log.warn("Skipping bulk insert: no valid data provided");
            return 0;
        }

        String sql = "INSERT INTO " + company + ".runbook_template_categories " +
                " (hidden, name, description, metadata) " +
                " VALUES " + String.join(", ", values);
        if (upsert) {
            sql += " ON CONFLICT(name) DO UPDATE SET " +
                    " hidden = EXCLUDED.hidden, " +
                    " name = EXCLUDED.name, " +
                    " description = EXCLUDED.description, " +
                    " metadata = EXCLUDED.metadata ";
        } else {
            sql += " ON CONFLICT(name) DO NOTHING ";
        }

        return template.update(sql, new MapSqlParameterSource(params));
    }

    @Override
    public Boolean update(String company, RunbookTemplateCategory t) throws SQLException {
        throw new UnsupportedOperationException("Cannot update template category");
    }

    @Override
    public Optional<RunbookTemplateCategory> get(String company, String id) {
        String sql = "SELECT * FROM " + company + ".runbook_template_categories " +
                " WHERE id = :id::uuid " +
                " LIMIT 1 ";
        try {
            List<RunbookTemplateCategory> results = template.query(sql, Map.of("id", id),
                    RunbookTemplateCategoryConverters.rowMapper(objectMapper));
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get template category for id={}", id, e);
            return Optional.empty();
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = RunbookTemplateCategoryFilter.RunbookTemplateCategoryFilterBuilder.class)
    public static class RunbookTemplateCategoryFilter {
        @JsonProperty("ids")
        List<String> ids;
        @JsonProperty("hidden")
        Boolean hidden;
        @JsonProperty("partial_name")
        String partialName;
        @JsonProperty("name")
        String name;
        @JsonProperty("description")
        String description;
    }

    public DbListResponse<RunbookTemplateCategory> filter(Integer pageNumber, Integer pageSize, String company, RunbookTemplateCategoryFilter filter) {
        int limit = MoreObjects.firstNonNull(pageSize, FilteredDatabaseService.DEFAULT_PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);
        filter = (filter == null)?  RunbookTemplateCategoryFilter.builder().build() : filter;

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        // -- name
        if (StringUtils.isNotBlank(filter.getPartialName())) {
            conditions.add("name ILIKE :name");
            params.put("name", "%" + filter.getPartialName() + "%");
        }
        if (StringUtils.isNotBlank(filter.getName())) {
            conditions.add("name ILIKE :name");
            params.put("name", filter.getName());
        }
        // -- description
        if (StringUtils.isNotBlank(filter.getDescription())) {
            conditions.add("description ILIKE :description");
            params.put("description", "%" + filter.getDescription() + "%");
        }
        // -- ids
        if (CollectionUtils.isNotEmpty(filter.getIds())) {
            conditions.add("id::text IN (:ids)");
            params.put("ids", filter.getIds());
        }
        // -- hidden
        if (filter.getHidden() != null) {
            conditions.add("hidden = :hidden::boolean");
            params.put("hidden", filter.getHidden());
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + ".runbook_template_categories " +
                where +
                " ORDER BY name ASC " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<RunbookTemplateCategory> results = template.query(sql, params, RunbookTemplateCategoryConverters.rowMapper(objectMapper));
        String countSql = "SELECT count(*) FROM " + company + ".runbook_template_categories " + where;
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
                "CREATE TABLE IF NOT EXISTS " + company + ".runbook_template_categories " +
                        "(" +
                        "   id           UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "   hidden       BOOLEAN NOT NULL DEFAULT false," +
                        "   name         VARCHAR(64) NOT NULL UNIQUE," +
                        "   description  TEXT NOT NULL DEFAULT ''," +
                        "   metadata     JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "   created_at   TIMESTAMPTZ NOT NULL DEFAULT now()" +
                        ")"
        );
        sqlList.forEach(template.getJdbcTemplate()::execute);

        if (populateData) {
            populateDefaultData(company);
        }

        return true;
    }

    private void populateDefaultData(String company) throws SQLException {
        List<RunbookTemplateCategory> categories = new ArrayList<>();
        String resource = "db/default_data/runbook_templates/runbook_template_categories.json";
        log.debug("Loading resource {}", resource);
        try {
            String resourceString = ResourceUtils.getResourceAsString(resource, RunbookTemplateCategoryDatabaseService.class.getClassLoader());
            List<RunbookTemplateCategory> templateCategories = objectMapper.readValue(resourceString,
                    objectMapper.getTypeFactory().constructCollectionLikeType(List.class, RunbookTemplateCategory.class));
            categories.addAll(templateCategories);
        } catch (Throwable e) {
            log.warn("Failed to load default data for runbook template categories at {}", resource, e);
        }

        int rows = insertBulk(company, categories, true);
        log.info("Inserted default data for runbook template categories ({} rows)", rows);
    }

}
