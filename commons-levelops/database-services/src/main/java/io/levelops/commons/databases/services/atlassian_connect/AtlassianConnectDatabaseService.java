package io.levelops.commons.databases.services.atlassian_connect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.atlassian_connect.AtlassianConnectAppMetadataConverters;
import io.levelops.commons.databases.models.database.atlassian_connect.AtlassianConnectAppMetadata;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
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
import javax.validation.constraints.Null;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;


@Log4j2
@Service
public class AtlassianConnectDatabaseService extends DatabaseService<AtlassianConnectAppMetadata> {
    private final ObjectMapper objectMapper;
    private static final String TABLE_NAME = "atlassian_connect_metadata";
    private static final Integer PAGE_SIZE = 500;

    protected final NamedParameterJdbcTemplate template;


    @Autowired
    public AtlassianConnectDatabaseService(DataSource dataSource, ObjectMapper objectMapper) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isTenantSpecific() {
        return false;
    }

    @Override
    public SchemaType getSchemaType() {
        return SchemaType.ATLASSIAN_CONNECT_SCHEMA;
    }

    @Override
    public String insert(String company, AtlassianConnectAppMetadata m) throws SQLException {
        return insertInternal(m, false);
    }

    public String upsert(AtlassianConnectAppMetadata m) throws SQLException {
        return insertInternal(m, true);
    }

    private String insertInternal(AtlassianConnectAppMetadata m, boolean upsert) throws SQLException {
        Validate.notNull(m, "AtlassianConnectAppMetadata cannot be null.");
        Validate.notNull(m.getAtlassianClientKey(), "atlassianClientKey cannot be null.");
        Validate.notNull(m.getAtlassianBaseUrl(), "atlassianBaseUrl cannot be null.");

        String upsertOnConflict =
                "ON CONFLICT(atlassian_client_key) DO UPDATE SET " +
                        "   (installed_app_key, atlassian_base_url, product_type, description, events, enabled, atlassian_display_url, otp, updated_at) " +
                        "   = (EXCLUDED.installed_app_key, EXCLUDED.atlassian_base_url, EXCLUDED.product_type, " +
                        "        EXCLUDED.description, EXCLUDED.events, EXCLUDED.enabled, EXCLUDED.atlassian_display_url, EXCLUDED.otp, now())";

        String sql = "INSERT INTO " + getFullTableName() + " (" +
                "   atlassian_client_key," +
                "   installed_app_key," +
                "   atlassian_base_url," +
                "   atlassian_display_url," +
                "   product_type," +
                "   description," +
                "   events," +
                "   atlassian_user_account_id," +
                "   otp," +
                "   enabled" +
                ") VALUES (" +
                "   :atlassian_client_key," +
                "   :installed_app_key," +
                "   :atlassian_base_url," +
                "   :atlassian_display_url," +
                "   :product_type," +
                "   :description," +
                "   :events::jsonb," +
                "   :atlassian_user_account_id," +
                "   :otp," +
                "   :enabled" +
                ")";
        if (upsert) {
            sql += " " + upsertOnConflict;
        }
        sql += "RETURNING id";

        Map<String, Object> params = Map.of(
                "atlassian_client_key", m.getAtlassianClientKey(),
                "installed_app_key", m.getInstalledAppKey(),
                "atlassian_base_url", m.getAtlassianBaseUrl(),
                "atlassian_display_url", m.getAtlassianDisplayUrl(),
                "product_type", m.getProductType(),
                "description", m.getDescription(),
                "events", ParsingUtils.serialize(objectMapper, "events", m.getEvents(), "{}"),
                "atlassian_user_account_id", m.getAtlassianUserAccountId(),
                "enabled", m.getEnabled(),
                "otp", m.getOtp()
        );

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, new MapSqlParameterSource(params), keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to insert AtlassianConnectAppMetadata.");
        }
        return keyHolder.getKeys().get("id").toString();
    }

    @Override
    public Boolean update(String company, AtlassianConnectAppMetadata t) throws SQLException {
        if (get(company, t.getId()).isPresent()) {
            return upsert(t) != null;
        }
        return false;
    }

    @Override
    public Optional<AtlassianConnectAppMetadata> get(String company, String id) throws SQLException {
        Validate.notBlank(id, "id cannot be blank.");

        String sql = "SELECT * FROM " + getFullTableName() + " WHERE id = :id::uuid";
        try {
            List<AtlassianConnectAppMetadata> results = template.query(sql, Map.of("id", id),
                    AtlassianConnectAppMetadataConverters.rowMapper(objectMapper));
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get artifact for id={}", id, e);
            return Optional.empty();
        }
    }

    public Optional<AtlassianConnectAppMetadata> getByAtlassianClientKey(String clientKey) throws SQLException {
        return filter(0, 1, AtlassianConnectAppMetadataFilter.builder()
                .atlassianClientKeys(List.of(clientKey))
                .build()
        ).getRecords()
                .stream()
                .findFirst();
    }

    @Value
    @Builder(toBuilder = true)
    public static class AtlassianConnectAppMetadataFilter {
        @Nullable
        List<String> atlassianClientKeys;

        @Nullable
        Boolean enabled;

        @Nullable
        String otp;
    }

    public DbListResponse<AtlassianConnectAppMetadata> filter(
            Integer pageNumber,
            Integer pageSize,
            @Nullable AtlassianConnectAppMetadataFilter filter
    ) throws SQLException {
        int limit = MoreObjects.firstNonNull(pageSize, PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);
        filter = filter != null ? filter : AtlassianConnectAppMetadataFilter.builder().build();

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        // -- atlassianClientKeys
        if (CollectionUtils.isNotEmpty(filter.getAtlassianClientKeys())) {
            conditions.add("atlassian_client_key IN (:atlassian_client_keys)");
            params.put("atlassian_client_keys", filter.getAtlassianClientKeys());
        }

        // -- enabled
        if (filter.getEnabled() != null) {
            conditions.add("enabled = :enabled");
            params.put("enabled", filter.getEnabled());
        }

        // -- otp
        if (filter.getOtp() != null) {
            conditions.add("otp = :otp");
            params.put("otp", filter.getOtp());
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + getFullTableName() + where +
                " ORDER BY updated_at DESC " +
                " OFFSET :skip " +
                " LIMIT :limit ";

        List<AtlassianConnectAppMetadata> results = template.query(sql, params, AtlassianConnectAppMetadataConverters.rowMapper(objectMapper));
        return DbListResponse.of(results, null);
    }

    public Stream<AtlassianConnectAppMetadata> stream(@Nullable AtlassianConnectAppMetadataFilter filter) {
        return PaginationUtils.streamThrowingRuntime(0, 1, page -> filter(page, PAGE_SIZE, filter).getRecords());
    }

    @Override
    public DbListResponse<AtlassianConnectAppMetadata> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return filter(pageNumber, pageSize, null);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new NotImplementedException();
    }

    private String getFullTableName() {
        return getSchemaType().getSchemaName() + "." + TABLE_NAME;
    }

    @Override
    public Boolean ensureTableExistence(String schema) throws SQLException {
        ensureSchemaExistence(getSchemaType().getSchemaName());

        List<String> ddl = List.of(
                "CREATE TABLE IF NOT EXISTS " + getFullTableName() +
                        "(" +
                        "   id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "   atlassian_client_key        VARCHAR(255) UNIQUE NOT NULL," +
                        "   installed_app_key           TEXT NOT NULL," +
                        "   atlassian_base_url          TEXT NOT NULL," +
                        "   atlassian_display_url       TEXT," +
                        "   product_type                VARCHAR(64)," +
                        "   description                 TEXT," +
                        "   events                      JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "   atlassian_user_account_id   VARCHAR(255)," +
                        "   otp                         VARCHAR(255)," +
                        "   enabled                     BOOLEAN NOT NULL," +
                        "   created_at                  TIMESTAMPTZ NOT NULL DEFAULT now()," +
                        "   updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now()" +
                        ")",
                "CREATE INDEX IF NOT EXISTS atlassian_connect_otp on " + getFullTableName() + " (otp, enabled)"
        );

        ddl.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
