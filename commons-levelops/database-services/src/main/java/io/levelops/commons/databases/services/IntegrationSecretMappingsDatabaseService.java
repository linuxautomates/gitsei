package io.levelops.commons.databases.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.IntegrationSecretMappingConverters;
import io.levelops.commons.databases.models.database.IntegrationSecretMapping;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Log4j2
@Service
public class IntegrationSecretMappingsDatabaseService extends FilteredDatabaseService<IntegrationSecretMapping, IntegrationSecretMappingsDatabaseService.Filter> {

    public IntegrationSecretMappingsDatabaseService(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, IntegrationSecretMapping mapping) throws SQLException {
        Validate.notBlank(mapping.getIntegrationId(), "mapping.getIntegrationId() cannot be null or empty.");
        Validate.notBlank(mapping.getName(), "mapping.getName() cannot be null or empty.");
        Validate.notBlank(mapping.getSmConfigId(), "mapping.getSmConfigId() cannot be null or empty.");
        Validate.notBlank(mapping.getSmKey(), "mapping.getSmKey() cannot be null or empty.");

        String sql = "INSERT INTO " + company + ".integration_secret_mappings " +
                "(integration_id, name, sm_config_id, sm_key)" +
                " VALUES " +
                "(:integration_id::INTEGER, :name, :sm_config_id, :sm_key)";

        Map<String, ?> params = Map.of(
                "integration_id", mapping.getIntegrationId(),
                "name", mapping.getName(),
                "sm_config_id", mapping.getSmConfigId(),
                "sm_key", mapping.getSmKey());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, new MapSqlParameterSource(params), keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return null;
        }
        return keyHolder.getKeys().get("id").toString();
    }

    @Override
    public Boolean update(String company, IntegrationSecretMapping mapping) throws SQLException {
        Validate.notBlank(mapping.getId(), "mapping.getId() cannot be null or empty.");

        List<String> updates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("id", mapping.getId());

        // -- name
        if (mapping.getName() != null) {
            updates.add("name = :name");
            params.put("name", mapping.getName());
        }

        // -- integration id
        if (mapping.getIntegrationId() != null) {
            updates.add("integration_id = :integration_id::INTEGER");
            params.put("integration_id", mapping.getIntegrationId());
        }

        // -- sm config id
        if (mapping.getSmConfigId() != null) {
            updates.add("sm_config_id = :sm_config_id");
            params.put("sm_config_id", mapping.getSmConfigId());
        }

        // -- sm key
        if (mapping.getSmConfigId() != null) {
            updates.add("sm_key = :sm_key");
            params.put("sm_key", mapping.getSmKey());
        }

        if (updates.isEmpty()) {
            return true;
        }

        updates.add("updated_at = now()");
        String sql = "UPDATE " + company + ".integration_secret_mappings " +
                " SET " + String.join(", ", updates) + " " +
                " WHERE id = :id::uuid ";
        return template.update(sql, params) > 0;
    }

    @Override
    public Optional<IntegrationSecretMapping> get(String company, String id) throws SQLException {
        String sql = "SELECT * FROM " + company + ".integration_secret_mappings " +
                " WHERE id = :id::uuid " +
                " LIMIT 1 ";
        try {
            List<IntegrationSecretMapping> results = template.query(sql, Map.of("id", id),
                    IntegrationSecretMappingConverters.rowMapper());
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get integration secret mapping for id={}", id, e);
            return Optional.empty();
        }
    }


    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Filter.FilterBuilder.class)
    public static class Filter {
        @JsonProperty("integration_id")
        String integrationId;
        @JsonProperty("sm_config_id")
        String smConfigId;
    }

    @Override
    public DbListResponse<IntegrationSecretMapping> filter(Integer pageNumber, Integer pageSize, String company, @Nullable Filter filter) {
        int limit = MoreObjects.firstNonNull(pageSize, 25);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);
        filter = filter != null ? filter : Filter.builder().build();

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        if (filter.getIntegrationId() != null) {
            conditions.add("integration_id = :integration_id::INTEGER");
            params.put("integration_id", filter.getIntegrationId());
        }
        if (filter.getSmConfigId() != null) {
            conditions.add("sm_config_id = :sm_config_id");
            params.put("sm_config_id", filter.getSmConfigId());
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + ".integration_secret_mappings " +
                where +
                " ORDER BY updated_at DESC " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<IntegrationSecretMapping> results = template.query(sql, params, IntegrationSecretMappingConverters.rowMapper());

        String countSql = "SELECT count(*) FROM " + company + ".integration_secret_mappings " + where;
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String sql = "DELETE FROM " + company + ".integration_secret_mappings " +
                " WHERE id = :id::uuid";

        return template.update(sql, Map.of(
                "id", id
        )) > 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".integration_secret_mappings " +
                        "(" +
                        "  id              UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "  integration_id  INTEGER NOT NULL  " +
                        "                     REFERENCES " + company + ".integrations(id) " +
                        "                     ON DELETE CASCADE," +
                        "  name             VARCHAR(50) NOT NULL ," +
                        "  sm_config_id     VARCHAR(50) NOT NULL," +
                        "  sm_key           TEXT NOT NULL," +
                        "  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()," +
                        "  created_at       TIMESTAMPTZ NOT NULL DEFAULT now()," +
                        "  CONSTRAINT       unique_name UNIQUE(integration_id, name)" +
                        ")",

                "CREATE INDEX IF NOT EXISTS integration_secret_mappings__integration_id_idx on " + company + ".integration_secret_mappings (integration_id)"
        );
        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

}
