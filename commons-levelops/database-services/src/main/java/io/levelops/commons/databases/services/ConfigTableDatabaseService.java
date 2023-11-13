package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.ConfigTableConverters;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Log4j2
@Service
public class ConfigTableDatabaseService extends DatabaseService<ConfigTable> {

    private static final int PAGE_SIZE = 25;
    public static final String DEFAULT_VERSION = "1";
    public static final String INCREMENT_VERSION = "increment";
    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate template;

    public enum Field {
        SCHEMA,
        ROWS,
        HISTORY
    }

    @Autowired
    public ConfigTableDatabaseService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        this.objectMapper = mapper;
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * Creates a new config table.
     * Required fields: name
     * Optional fields: schema, rows, created_by
     */
    @Override
    public String insert(String company, ConfigTable table) throws SQLException {
        Validate.notNull(table, "r cannot be null.");
        Validate.notBlank(table.getName(), "r.getName() cannot be null or empty.");

        String sql = "INSERT INTO " + company + ".config_tables " +
                "(name, schema, total_rows, rows, version, created_by)" +
                " VALUES " +
                "(:name, :schema::jsonb, :total_rows, :rows::jsonb, :version::int, :created_by)";

        try {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("name", StringUtils.truncate(StringUtils.defaultString(table.getName()), 256));
            params.addValue("schema", objectMapper.writeValueAsString(table.getSchema()));
            params.addValue("total_rows", CollectionUtils.size(table.getRows()));
            params.addValue("rows", objectMapper.writeValueAsString(MapUtils.emptyIfNull(table.getRows())));
            params.addValue("version", DEFAULT_VERSION);
            params.addValue("created_by", table.getCreatedBy());
            KeyHolder keyHolder = new GeneratedKeyHolder();
            int updatedRows = template.update(sql, params, keyHolder);
            if (updatedRows <= 0 || keyHolder.getKeys() == null) {
                return null;
            }
            return String.valueOf(keyHolder.getKeys().get("id"));
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize config table: " + table, e);
        } catch (DuplicateKeyException e) {
            return null;
        }
    }

    @Override
    public Boolean update(String company, ConfigTable table) throws SQLException {
        return updateAndReturnVersion(company, table).isPresent();
    }

    public Optional<String> updateAndReturnVersion(String company, ConfigTable table) throws SQLException {
        Validate.notBlank(table.getId(), "table.getId() cannot be null or empty.");

        List<String> updates = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", table.getId());

        try {
            // -- name
            if (table.getName() != null) {
                updates.add("name = :name");
                params.addValue("name", table.getName());
            }

            // -- schema
            if (table.getSchema() != null) {
                updates.add("schema = :schema::jsonb");
                params.addValue("schema", objectMapper.writeValueAsString(table.getSchema()));
            }

            // -- rows
            if (table.getRows() != null) {
                updates.add("rows = :rows::jsonb");
                params.addValue("rows", objectMapper.writeValueAsString(table.getRows()));
                updates.add("total_rows = :total_rows");
                params.addValue("total_rows", CollectionUtils.size(table.getRows()));
            }

            // -- version
            if (table.getVersion() != null) {
                if (table.getVersion().equals(INCREMENT_VERSION)) {
                    updates.add("version = version + 1");
                } else {
                    updates.add("version = :version");
                    params.addValue("version", Integer.valueOf(table.getVersion()));
                }
            }

            // -- history
            if (table.getHistory() != null) {
                updates.add("history = :history::jsonb");
                params.addValue("history", objectMapper.writeValueAsString(table.getHistory()));
            }

            // -- updated_by
            if (table.getUpdatedBy() != null) {
                updates.add("updated_by = :updated_by");
                params.addValue("updated_by", table.getUpdatedBy());
            }
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize data", e);
        }

        if (updates.isEmpty()) {
            return Optional.empty();
        }

        updates.add("updated_at = now()");
        String sql = "UPDATE " + company + ".config_tables " +
                "     SET " + String.join(", ", updates) + " " +
                "     WHERE id = :id::uuid" +
                "     RETURNING id, version ";

        return IterableUtils.getFirst(template.query(sql, params, (rs, rowNumber) -> rs.getString("version")));
    }

    public boolean insertRevision(String company, String id, ConfigTable.Revision revision) throws SQLException {
        Validate.notBlank(id, "id cannot be null or empty.");
        Validate.notNull(revision, "revision cannot be null.");
        Validate.notBlank(revision.getVersion(), "revision.getVersion() cannot be null or empty.");

        String sql = "UPDATE " + company + ".config_tables " +
                " SET history = history || :entry::jsonb " +
                " WHERE id = :id::uuid";

        try {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("id", id);
            params.addValue("entry", objectMapper.writeValueAsString(Map.of(revision.getVersion(), revision)));

            return template.update(sql, params) > 0;
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize revision: " + revision, e);
        }
    }

    public boolean deleteRevisions(String company, String id, List<String> versions) throws SQLException {
        Validate.notBlank(id, "id cannot be null or empty.");
        if (CollectionUtils.isEmpty(versions)) {
            return true;
        }

        String sql = "UPDATE " + company + ".config_tables " +
                " SET history = history - '" + String.join("' - '", versions) + "'" +
                " WHERE id = :id::uuid";

        return template.update(sql, Map.of("id", id)) > 0;
    }

    @Override
    public Optional<ConfigTable> get(String company, String id) throws SQLException {
        return get(company, id, EnumSet.allOf(Field.class));
    }

    public Optional<ConfigTable> get(String company, String id, EnumSet<Field> expand) throws SQLException {
        String sql = "SELECT " + getFields(expand) + " FROM " + company + ".config_tables " +
                " WHERE id = :id::uuid " +
                " LIMIT 1 ";
        try {
            List<ConfigTable> results = template.query(sql, Map.of("id", id),
                    ConfigTableConverters.rowMapper(objectMapper, expand));
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get config table for id={}", id, e);
            return Optional.empty();
        }
    }

    @Override
    public DbListResponse<ConfigTable> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return filter(pageNumber, pageSize, company, null, null, null, EnumSet.noneOf(Field.class));
    }

    public Stream<ConfigTable> stream(String company, @Nullable List<String> ids, @Nullable String exactName, @Nullable String partialName, EnumSet<Field> expand) {
        return PaginationUtils.stream(0, 1, page -> filter(page, PAGE_SIZE, company, ids, exactName, partialName, expand).getRecords());
    }

    public DbListResponse<ConfigTable> filter(Integer pageNumber, Integer pageSize, String company, @Nullable List<String> ids, @Nullable String exactName, @Nullable String partialName, EnumSet<Field> expand) {
        int limit = MoreObjects.firstNonNull(pageSize, PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        // -- ids
        if (CollectionUtils.isNotEmpty(ids)) {
            conditions.add("id::text IN (:ids)");
            params.put("ids", ids);
        }

        // -- name
        if (Strings.isNotEmpty(exactName)) {
            conditions.add("name ILIKE :name");
            params.put("name", exactName);
        } else if (Strings.isNotEmpty(partialName)) {
            conditions.add("name ILIKE :name");
            params.put("name", "%" + partialName + "%");
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT " + getFields(expand) + " FROM " + company + ".config_tables " +
                where +
                " ORDER BY COALESCE(updated_at, created_at) DESC " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<ConfigTable> results = template.query(sql, params, ConfigTableConverters.rowMapper(objectMapper, expand));

        String countSql = "SELECT count(id) FROM " + company + ".config_tables " + where;
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String sql = "DELETE " +
                " FROM " + company + ".config_tables " +
                " WHERE id = :id::uuid";

        return template.update(sql, Map.of(
                "id", id
        )) > 0;
    }

    public static EnumSet<Field> getExpand(boolean expandSchema, boolean expandRows, boolean expandHistory) {
        EnumSet<Field> expand = EnumSet.noneOf(Field.class);
        if (expandSchema) {
            expand.add(Field.SCHEMA);
        }
        if (expandRows) {
            expand.add(Field.ROWS);
        }
        if (expandHistory) {
            expand.add(Field.HISTORY);
        }
        return expand;
    }

    private static String getFields(EnumSet<Field> expand) {
        return getFields("", expand);
    }

    private static String getFields(String prefix, EnumSet<Field> expand) {
        String fields = "{0}id,{0}name,{0}total_rows,{0}version,{0}updated_by,{0}created_by,{0}updated_at,{0}created_at";
        if (expand.contains(Field.SCHEMA)) {
            fields += ",{0}schema";
        }
        if (expand.contains(Field.ROWS)) {
            fields += ",{0}rows";
        }
        if (expand.contains(Field.HISTORY)) {
            fields += ",{0}history";
        }
        return MessageFormat.format(fields, prefix);
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".config_tables " +
                        "(" +
                        "  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "  name        VARCHAR(256) NOT NULL," +
                        "  schema      JSONB NOT NULL DEFAULT '{}'::JSONB," +
                        "  total_rows  INTEGER NOT NULL DEFAULT 0," +
                        "  rows        JSONB NOT NULL DEFAULT '{}'::JSONB," +
                        "  version     INTEGER NOT NULL," +
                        "  history     JSONB NOT NULL DEFAULT '{}'::JSONB," +
                        "  created_by  TEXT," +
                        "  updated_by  TEXT," +
                        "  updated_at  TIMESTAMPTZ," +
                        "  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()," +
                        "  UNIQUE (name)" +
                        ")",

                "CREATE INDEX IF NOT EXISTS config_tables__name_idx on " + company + ".config_tables (name)"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
