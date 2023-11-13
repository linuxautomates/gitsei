package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.AiReportConverters;
import io.levelops.commons.databases.models.database.AiReport;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Log4j2
@Service
public class AiReportDatabaseService extends FilteredDatabaseService<AiReport, AiReportDatabaseService.AiReportFilter> {

    private static final int PAGE_SIZE = 25;
    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public AiReportDatabaseService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        this.objectMapper = mapper;
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public String insert(String company, AiReport r) throws SQLException {
        Validate.notBlank(r.getType(), "r.getType() cannot be null or empty.");
        Validate.notBlank(r.getKey(), "r.getKey() cannot be null or empty.");

        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();
        List<String> onConflict = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();

        columns.add("type");
        values.add(":type");
        params.addValue("type", r.getType());

        columns.add("key");
        values.add(":key");
        params.addValue("key", r.getKey());

        try {
            if (r.getData() != null) {
                columns.add("data");
                values.add(":data::json");
                params.addValue("data", objectMapper.writeValueAsString(r.getData()));

                columns.add("data_updated_at");
                values.add("now()");

                onConflict.add("data = EXCLUDED.data");
                onConflict.add("data_updated_at = EXCLUDED.data_updated_at");
            }
            if (r.getError() != null) {
                columns.add("error");
                values.add(":error::json");
                params.addValue("error", objectMapper.writeValueAsString(r.getError()));

                columns.add("error_updated_at");
                values.add("now()");

                onConflict.add("error = EXCLUDED.error");
                onConflict.add("error_updated_at = EXCLUDED.error_updated_at");
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize to JSON", e);
        }

        String sql = "INSERT INTO " + company + ".ai_reports as r " +
                " (" + String.join(", ", columns) + ") " +
                " VALUES " +
                " (" + String.join(", ", values) + ") " +
                " ON CONFLICT (type, key) " +
                " DO UPDATE SET " + String.join(", ", onConflict);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return null;
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    @Override
    public Boolean update(String company, AiReport r) throws SQLException {
        Validate.notBlank(company, "company cannot be null or empty.");

        List<String> updates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        String whereCondition;
        if (r.getId() != null) {
            whereCondition = "id = :id::uuid";
            params.put("id", r.getId());
        } else {
            Validate.notBlank(r.getType(), "r.getType() cannot be null or empty.");
            Validate.notBlank(r.getKey(), "r.getKey() cannot be null or empty.");
            whereCondition = "(type = :type AND key = :key)";
            params.put("type", r.getType());
            params.put("key", r.getKey());
        }

        // -- data
        if (r.getData() != null) {
            updates.add("data = :data::json");
            try {
                params.put("data", objectMapper.writeValueAsString(r.getData()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse data", e);
            }
            updates.add("data_updated_at = now()");
        }

        // -- error
        if (r.getError() != null) {
            updates.add("error = :error::json");
            try {
                params.put("error", objectMapper.writeValueAsString(r.getError()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse data", e);
            }
            updates.add("error_updated_at = now()");
        }

        if (updates.isEmpty()) {
            return true;
        }
        String sql = "UPDATE " + company + ".ai_reports " +
                " SET " + String.join(", ", updates) + " " +
                " WHERE " + whereCondition;
        return template.update(sql, params) > 0;
    }

    @Override
    public Optional<AiReport> get(String company, String id) {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(id, "id cannot be null or empty.");

        String sql = "SELECT * FROM " + company + ".ai_reports " +
                " WHERE id = :id::uuid " +
                " LIMIT 1 ";
        try {
            List<AiReport> results = template.query(sql,
                    Map.of("id", id),
                    AiReportConverters.rowMapper(objectMapper));
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get AI report for id={}", id, e);
            return Optional.empty();
        }
    }

    public Optional<AiReport> get(String company, AiReport.TypeKeyIdentifier identifier) {
        return get(company, identifier.getType(), identifier.getKey());
    }

    public Optional<AiReport> get(String company, String type, String key) {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(type, "type cannot be null or empty.");
        Validate.notBlank(key, "key cannot be null or empty.");

        String sql = "SELECT * FROM " + company + ".ai_reports " +
                " WHERE type = :type and key=:key " +
                " LIMIT 1 ";
        try {
            List<AiReport> results = template.query(sql,
                    Map.of(
                            "type", type,
                            "key", key),
                    AiReportConverters.rowMapper(objectMapper));
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get AI report for type={} and key={}", type, key, e);
            return Optional.empty();
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = AiReportFilter.AiReportFilterBuilder.class)
    public static class AiReportFilter {
        boolean count;
        List<String> types;
        List<String> keys;
        List<AiReport.TypeKeyIdentifier> typeKeyIdentifiers;
    }

    @Override
    public DbListResponse<AiReport> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return filter(pageNumber, pageSize, company, null);
    }

    @Override
    public DbListResponse<AiReport> filter(Integer pageNumber, Integer pageSize, String company, @Nullable AiReportFilter filter) {
        int limit = MoreObjects.firstNonNull(pageSize, PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);
        filter = filter != null ? filter : AiReportFilter.builder().build();

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        // -- type
        if (CollectionUtils.isNotEmpty(filter.getTypes())) {
            conditions.add("type IN (:types)");
            params.put("types", filter.getTypes());
        }

        // -- key
        if (CollectionUtils.isNotEmpty(filter.getKeys())) {
            conditions.add("key IN (:keys)");
            params.put("keys", filter.getKeys());
        }

        // -- types and keys
        if (CollectionUtils.isNotEmpty(filter.getTypeKeyIdentifiers())) {
            List<String> orConditions = new ArrayList<>();
            for (int i = 0; i < filter.getTypeKeyIdentifiers().size(); ++i) {
                orConditions.add("(type = :type_" + i + " AND key = :key_" + i + ")");

                AiReport.TypeKeyIdentifier identifier = filter.getTypeKeyIdentifiers().get(i);
                params.put("type_" + i, identifier.getType());
                params.put("key_" + i, identifier.getKey());
            }
            conditions.add("( " + String.join(" OR ", orConditions) + " )");
        }


        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + ".ai_reports " +
                where +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<AiReport> results = template.query(sql, params, AiReportConverters.rowMapper(objectMapper));

        Integer count = null;
        if (filter.isCount()) {
            String countSql = "SELECT count(*) FROM " + company + ".ai_reports " + where;
            count = template.queryForObject(countSql, params, Integer.class);
        }
        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String sql = "DELETE " +
                " FROM " + company + ".ai_reports" +
                " WHERE id = :id::uuid";

        return template.update(sql, Map.of(
                "id", id
        )) > 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".ai_reports " +
                        "(" +
                        "   id               UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "   type             VARCHAR(64) NOT NULL," +
                        "   key              VARCHAR(64) NOT NULL," +
                        "   data             JSON NOT NULL DEFAULT '{}'::json," +
                        "   error            JSON NOT NULL DEFAULT '{}'::json," +
                        "   data_updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()," +
                        "   error_updated_at TIMESTAMPTZ NOT NULL DEFAULT now()" +
                        ")",

                "CREATE INDEX IF NOT EXISTS ai_reports__type_idx ON " + company + "." + "ai_reports (type)",
                "CREATE UNIQUE INDEX IF NOT EXISTS ai_reports__type_key_idx ON " + company + "." + "ai_reports (type, key)"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);

        return true;
    }

}
