package io.levelops.commons.tenant_management.services;

import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.tenant_management.converters.TenantIndexTypeConfigConverters;
import io.levelops.commons.tenant_management.models.TenantIndexTypeConfig;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.tenant_management.services.TenantConfigDBService.TENANT_CONFIGS_FULL_NAME;


@Log4j2
@Service
public class TenantIndexTypeConfigDBService extends DatabaseService<TenantIndexTypeConfig>  {
    private static final SchemaType FS_CONTROLLER_SCHEMA = SchemaType.FS_CONTROLLER_SCHEMA;
    private static final String TENANT_INDEX_TYPE_CONFIGS = "tenant_index_type_configs";
    public static final String TENANT_INDEX_TYPE_CONFIGS_FULL_NAME = FS_CONTROLLER_SCHEMA.getSchemaName() + "." + TENANT_INDEX_TYPE_CONFIGS;

    private static final String UPSERT_SQL = "INSERT INTO " + TENANT_INDEX_TYPE_CONFIGS_FULL_NAME + " (tenant_config_id, index_type, priority, frequency_in_mins) values (:tenant_config_id, :index_type, :priority, :frequency_in_mins) ON CONFLICT (tenant_config_id, index_type) DO NOTHING ";

    private static final String UPDATE_ENABLED_SQL = "UPDATE " + TENANT_INDEX_TYPE_CONFIGS_FULL_NAME + " SET enabled = (:enabled), updated_at = now() WHERE id = :id";
    private static final String UPDATE_FREQUENCY_SQL = "UPDATE " + TENANT_INDEX_TYPE_CONFIGS_FULL_NAME + " SET frequency_in_mins = (:frequency_in_mins), updated_at = now() WHERE id = :id";
    private static final String UPDATE_PRIORITY_SQL = "UPDATE " + TENANT_INDEX_TYPE_CONFIGS_FULL_NAME + " SET priority = (:priority), updated_at = now() WHERE id = :id";
    private static final String SELECT_SQL = "SELECT c.*, t.tenant_id from " + TENANT_INDEX_TYPE_CONFIGS_FULL_NAME + " AS c JOIN " + TENANT_CONFIGS_FULL_NAME + " AS t ON c.tenant_config_id = t.id";

    private final NamedParameterJdbcTemplate template;

    // region CSTOR
    public TenantIndexTypeConfigDBService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }
    // endregion

    // region isTenantSpecific & SchemaType
    @Override
    public boolean isTenantSpecific() {
        return false;
    }
    @Override
    public SchemaType getSchemaType() {
        return FS_CONTROLLER_SCHEMA;
    }
    // endregion
    
    // region References
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(TenantConfigDBService.class);
    }
    // endregion

    // region insert
    @Override
    public String insert(String company, TenantIndexTypeConfig t) throws SQLException {
        var keyHolder = new GeneratedKeyHolder();
        int count = this.template.update(
                UPSERT_SQL,
                new MapSqlParameterSource()
                        .addValue("tenant_config_id", t.getTenantConfigId())
                        .addValue("index_type", t.getIndexType().toString())
                        .addValue("priority", t.getPriority())
                        .addValue("frequency_in_mins", t.getFrequencyInMins()),
                keyHolder,
                new String[]{"id"}
        );
        return count == 0 ? null : keyHolder.getKeys().get("id").toString();
    }
    // endregion

    //region NOT IMPLEMENTED
    @Override
    public Boolean update(String company, TenantIndexTypeConfig t) throws SQLException {
        throw new NotImplementedException();
    }
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new NotImplementedException();
    }
    // endregion

    //region Get and List
    @Override
    public Optional<TenantIndexTypeConfig> get(String company, String id) throws SQLException {
        var results = getBatch(0, 10, Collections.singletonList(UUID.fromString(id)), null, null, null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }
    @Override
    public DbListResponse<TenantIndexTypeConfig> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(pageNumber, pageSize, null, null, null, null);
    }
    public DbListResponse<TenantIndexTypeConfig> listByFilter(Integer pageNumber, Integer pageSize, List<UUID> ids, List<String> tenantIds, Boolean configEnabled, Boolean tenantEnabled) throws SQLException {
        return getBatch(pageNumber, pageSize, ids, tenantIds, configEnabled, tenantEnabled);
    }

    private DbListResponse<TenantIndexTypeConfig> getBatch(Integer pageNumber, Integer pageSize, List<UUID> ids, List<String> tenantIds, Boolean configEnabled, Boolean tenantEnabled) throws SQLException {
        List<String> criterias = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        if(CollectionUtils.isNotEmpty(ids)) {
            criterias.add("c.id in (:ids)");
            params.put("ids", ids);
        }
        if(CollectionUtils.isNotEmpty(tenantIds)) {
            criterias.add("t.tenant_id in (:tenant_ids)");
            params.put("tenant_ids", tenantIds);
        }

        if(configEnabled != null) {
            criterias.add("c.enabled = :config_enabled");
            params.put("config_enabled", configEnabled);
        }
        if(tenantEnabled != null) {
            criterias.add("t.enabled = :tenant_enabled");
            params.put("tenant_enabled", tenantEnabled);
        }

        String selectSqlBase = SELECT_SQL;
        String criteria = "";
        if(CollectionUtils.isNotEmpty(criterias)) {
            criteria = " WHERE " + String.join(" AND ", criterias);
        }

        List<String> sortBy = new ArrayList<>();
        sortBy.add("c.updated_at DESC");
        String orderBy = " ORDER BY " + String.join(",", sortBy);

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<TenantIndexTypeConfig> results = template.query(selectSql, params, TenantIndexTypeConfigConverters.mapTenantIndexTypeConfig());

        Integer totCount = 0;
        if (results.size() > 0) {
            totCount = results.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (results.size() == pageSize) {
                totCount = template.query(countSql, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(results, totCount);
    }
    // endregion

    //region Upsert
    public void batchUpsert(List<TenantIndexTypeConfig> tenantIndexTypeConfigs) {
        if(CollectionUtils.isEmpty(tenantIndexTypeConfigs)) {
            return;
        }
        List<SqlParameterSource> params = tenantIndexTypeConfigs.stream()
                .map(t -> new MapSqlParameterSource()
                        .addValue("tenant_config_id", t.getTenantConfigId())
                        .addValue("index_type", t.getIndexType().toString())
                        .addValue("priority", t.getPriority())
                        .addValue("frequency_in_mins", t.getFrequencyInMins())
                )
                .collect(Collectors.toList());

        int[] count = this.template.batchUpdate(UPSERT_SQL,params.toArray(new SqlParameterSource[]{}));
    }
    // endregion

    //region Update
    public void updateTenantIndexTypeConfigEnable(UUID id, Boolean enabled) {
        if(enabled == null) {
            return;
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("enabled", enabled);
        this.template.update(UPDATE_ENABLED_SQL,params);
    }

    public void updateTenantIndexTypeConfigPriority(UUID id, Integer priority) {
        if(priority == null) {
            return;
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("priority", priority);
        this.template.update(UPDATE_PRIORITY_SQL,params);
    }

    public void updateTenantIndexTypeConfigFrequency(UUID id, Integer frequencyInMins) {
        if(frequencyInMins == null) {
            return;
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("id", id);
        params.put("frequency_in_mins", frequencyInMins);
        this.template.update(UPDATE_FREQUENCY_SQL,params);
    }
    // endregion

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {

        List<String> ddl = List.of(
                "CREATE TABLE IF NOT EXISTS " + TENANT_INDEX_TYPE_CONFIGS_FULL_NAME +
                        "(\n" +
                        "    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    tenant_config_id BIGINT NOT NULL REFERENCES  "+ TENANT_CONFIGS_FULL_NAME + " (id) ON DELETE CASCADE, " +
                        "    index_type VARCHAR NOT NULL,\n" +
                        "    enabled BOOLEAN NOT NULL DEFAULT true,\n" +
                        "    priority INTEGER NOT NULL,\n" +
                        "    frequency_in_mins INTEGER NOT NULL,\n" +
                        "    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "    CONSTRAINT tenant_index_type_configs_valid_priority check (priority >= 0 AND priority <= 5)"+
                        ");\n",
                "CREATE UNIQUE INDEX IF NOT EXISTS " + TENANT_INDEX_TYPE_CONFIGS + "_tenant_config_id_index_type_uniq_idx ON " + TENANT_INDEX_TYPE_CONFIGS_FULL_NAME +"(tenant_config_id, index_type)"
        );
        ddl.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
