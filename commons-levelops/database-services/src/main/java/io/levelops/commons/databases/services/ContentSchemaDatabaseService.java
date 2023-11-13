package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.ContentSchemaConverters;
import io.levelops.commons.databases.models.database.ContentSchema;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.models.ContentType;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class ContentSchemaDatabaseService extends DatabaseService<ContentSchema> {

    private static final int PAGE_SIZE = 25;
    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper objectMapper;
    @VisibleForTesting
    protected boolean populateData = true;

    @Autowired
    protected ContentSchemaDatabaseService(DataSource dataSource, ObjectMapper objectMapper) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
    }

    @Override
    public String insert(String company, ContentSchema contentSchema) throws SQLException {
        Validate.notNull(contentSchema.getContentType(), "contentSchema.getContentType() cannot be null.");
        String json;
        try {
            json = objectMapper.writeValueAsString(contentSchema);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize content schema: " + contentSchema, e);
        }
        Map<String, ?> params = Map.of(
                "content_type", contentSchema.getContentType().toString(),
                "content_schema", json
        );
        String sql = "INSERT INTO " + company + ".content_schemas " +
                " (content_type, content_schema) " +
                " VALUES (:content_type, :content_schema::jsonb)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, new MapSqlParameterSource(params), keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return null;
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    public int insertBulk(String company, List<ContentSchema> contentSchemas, boolean upsert) {
        Map<String, Object> params = new HashMap<>();
        List<String> values = new ArrayList<>();
        for (ContentSchema contentSchema : contentSchemas) {
            if (contentSchema.getContentType() == null) {
                log.warn("Ignoring bulk insert of content schema missing content type: {}", contentSchema);
                continue;
            }
            String json;
            try {
                json = objectMapper.writeValueAsString(contentSchema);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize content schema", e);
                continue;
            }
            int i = values.size();
            params.putAll(Map.of(
                    "content_type_" + i, contentSchema.getContentType().toString(),
                    "content_schema_" + i, json
            ));
            values.add(MessageFormat.format("(:content_type_{0}, :content_schema_{0}::jsonb)", i));
        }

        if (values.isEmpty()) {
            log.warn("Skipping bulk insert: no valid data provided");
            return 0;
        }

        String sql = "INSERT INTO " + company + ".content_schemas " +
                " (content_type, content_schema) " +
                " VALUES " + String.join(", ", values);
        if (upsert) {
            sql += " ON CONFLICT(content_type) DO UPDATE SET " +
                    " content_schema = EXCLUDED.content_schema";
        } else {
            sql += " ON CONFLICT(type) DO NOTHING ";
        }

        return template.update(sql, new MapSqlParameterSource(params));
    }

    @Override
    public Boolean update(String company, ContentSchema t) throws SQLException {
        throw new UnsupportedOperationException("Update");
    }

    @Override
    public Optional<ContentSchema> get(String company, String id) throws SQLException {
        String sql = "SELECT * FROM " + company + ".content_schemas " +
                " WHERE id = :id::uuid " +
                " LIMIT 1 ";
        try {
            List<ContentSchema> results = template.query(sql, Map.of("id", id),
                    ContentSchemaConverters.rowMapper(objectMapper));
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get content schema for id={}", id, e);
            return Optional.empty();
        }
    }

    public Optional<ContentSchema> getByContentType(String company, ContentType contentType) throws SQLException {
        try {
            DbListResponse<ContentSchema> results = filter(company, 0, 1, List.of(contentType));
            if (results == null) {
                return Optional.empty();
            }
            return IterableUtils.getFirst(results.getRecords());
        } catch (DataAccessException e) {
            log.warn("Failed to get content schema for contentType={}", contentType, e);
            return Optional.empty();
        }
    }

    @Override
    public DbListResponse<ContentSchema> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return filter(company, pageNumber, pageSize, null);
    }

    public Stream<ContentSchema> stream(String company, List<ContentType> contentTypes) {
        return PaginationUtils.stream(0, 1, pageNumber -> filter(company, pageNumber, PAGE_SIZE, contentTypes).getRecords());
    }

    public DbListResponse<ContentSchema> filter(String company, Integer pageNumber, Integer pageSize, List<ContentType> contentTypes) {
        int limit = MoreObjects.firstNonNull(pageSize, PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        // -- type
        if (CollectionUtils.isNotEmpty(contentTypes)) {
            conditions.add("content_type::text IN (:content_types)");
            params.put("content_types", contentTypes.stream().map(ContentType::toString).collect(Collectors.toList()));
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + ".content_schemas " +
                where +
                " ORDER BY content_type ASC " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<ContentSchema> results = template.query(sql, params, ContentSchemaConverters.rowMapper(objectMapper));
        String countSql = "SELECT count(*) FROM " + company + ".content_schemas " + where;
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException("Delete");
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".content_schemas " +
                        "(" +
                        "   id             UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "   content_type   VARCHAR(128) NOT NULL UNIQUE," +
                        "   content_schema JSONB NOT NULL" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS content_schemas__content_type_idx on " + company + ".content_schemas (content_type)"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);

        if (populateData) {
            populateDefaultData(company);
        }

        return true;
    }

    protected void populateDefaultData(String company) throws SQLException {
        List<ContentSchema> contentSchemas = new ArrayList<>();
        Reflections reflections = new Reflections("db.default_data.content_schemas", new ResourcesScanner());
        Set<String> resourceList = reflections.getResources(name -> name != null && name.endsWith(".json"));
        for (String resource : resourceList) {
            log.debug("Loading resource {}", resource);
            try {
                String resourceString = ResourceUtils.getResourceAsString(resource, ContentSchemaDatabaseService.class.getClassLoader());
                var contentSchema = objectMapper.readValue(resourceString, ContentSchema.class);
                contentSchemas.add(contentSchema);
            } catch (Throwable e) {
                log.warn("Failed to load default data for content schema at {}", resource, e);
            }
        }

        int rows = insertBulk(company, contentSchemas, true);
        log.info("Inserted default data for content_schemas ({} rows)", rows);
    }
}
