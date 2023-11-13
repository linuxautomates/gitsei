package io.levelops.commons.tenant_management.services;

import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.tenant_management.converters.TenantConfigConverters;
import io.levelops.commons.tenant_management.models.TenantConfig;
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
import java.util.stream.Collectors;

@Log4j2
@Service
public class TenantConfigDBService extends DatabaseService<TenantConfig>  {
    private static final SchemaType FS_CONTROLLER_SCHEMA = SchemaType.FS_CONTROLLER_SCHEMA;
    private static final String TENANT_CONFIGS_TABLE_NAME = "tenant_configs";
    public static final String TENANT_CONFIGS_FULL_NAME = FS_CONTROLLER_SCHEMA.getSchemaName() + "." + TENANT_CONFIGS_TABLE_NAME;

    String UPSERT_TENANTS_SQL = "INSERT INTO " + TENANT_CONFIGS_FULL_NAME +
            " (tenant_id) values (:tenant_id) ON CONFLICT(tenant_id) DO NOTHING ";

    String UPDATE_TENANTS_NOT_DELETED_SQL = "UPDATE " + TENANT_CONFIGS_FULL_NAME +
            " SET  marked_for_deletion = false, marked_for_deletion_at = null, updated_at = now() WHERE marked_for_deletion = true AND tenant_id IN (:tenant_id)";

    String UPDATE_TENANTS_DELETED_SQL = "UPDATE " + TENANT_CONFIGS_FULL_NAME +
            " SET  marked_for_deletion = true, marked_for_deletion_at = now(), updated_at = now() WHERE marked_for_deletion = false AND tenant_id NOT IN (:tenant_id)";

    String UPDATE_TENANT_ENABLED_SQL = "UPDATE " + TENANT_CONFIGS_FULL_NAME +
            " SET enabled = (:enabled), updated_at = now() WHERE tenant_id =:tenant_id";

    String UPDATE_TENANT_PRIORITY_SQL = "UPDATE " + TENANT_CONFIGS_FULL_NAME +
            " SET priority = (:priority), updated_at = now() WHERE tenant_id =:tenant_id";

    private final NamedParameterJdbcTemplate template;

    // region CSTOR
    public TenantConfigDBService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }
    // endregion

    // region isTenantSpecific
    @Override
    public boolean isTenantSpecific() {
        return false;
    }
    // endregion

    // region References
    @Override
    public SchemaType getSchemaType() {
        return FS_CONTROLLER_SCHEMA;
    }
    // endregion

    //region Upsert Flow
    public void batchUpsert(List<String> tenantIds) {
        if(CollectionUtils.isEmpty(tenantIds)) {
            return;
        }
        List<SqlParameterSource> params = tenantIds.stream()
                .map(t -> new MapSqlParameterSource().addValue("tenant_id", t))
                .collect(Collectors.toList());

        int[] count = this.template.batchUpdate(UPSERT_TENANTS_SQL,params.toArray(new SqlParameterSource[]{}));
    }

    public void markTenantsAsNotDeleted(List<String> existingTenantIds) {
        if(CollectionUtils.isEmpty(existingTenantIds)) {
            return;
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("tenant_id", existingTenantIds);
        this.template.update(UPDATE_TENANTS_NOT_DELETED_SQL,params);
    }

    public void markNonExistingTenantsAsDeleted(List<String> existingTenantIds) {
        if(CollectionUtils.isEmpty(existingTenantIds)) {
            return;
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("tenant_id", existingTenantIds);
        this.template.update(UPDATE_TENANTS_DELETED_SQL,params);
    }
    //endregion

    //region Updates
    public void updateTenantEnable(String tenantId, Boolean enabled) {
        if(enabled == null) {
            return;
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("tenant_id", tenantId);
        params.put("enabled", enabled);
        this.template.update(UPDATE_TENANT_ENABLED_SQL,params);
    }

    public void updateTenantPriority(String tenantId, Integer priority) {
        if(priority == null) {
            return;
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("tenant_id", tenantId);
        params.put("priority", priority);
        this.template.update(UPDATE_TENANT_PRIORITY_SQL,params);
    }
    //endregion

    //region Insert
    @Override
    public String insert(String company, TenantConfig t) throws SQLException {
        var keyHolder = new GeneratedKeyHolder();
        int count = this.template.update(
                UPSERT_TENANTS_SQL,
                new MapSqlParameterSource()
                        .addValue("tenant_id", t.getTenantId()),
                keyHolder,
                new String[]{"id"}
        );
        return count == 0 ? null : keyHolder.getKeys().get("id").toString();
    }
    //endregion

    //region Get and List
    @Override
    public Optional<TenantConfig> get(String company, String id) throws SQLException {
        var results = getBatch(0, 10, Collections.singletonList(Long.parseLong(id)), null, null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }

    @Override
    public DbListResponse<TenantConfig> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(pageNumber, pageSize, null, null, null);
    }

    public DbListResponse<TenantConfig> listByFilter(Integer pageNumber, Integer pageSize, List<Long> ids, List<String> tenantIds, Boolean enabled) throws SQLException {
        return getBatch(pageNumber, pageSize, ids, tenantIds, enabled);
    }
    
    private DbListResponse<TenantConfig> getBatch(Integer pageNumber, Integer pageSize, List<Long> ids, List<String> tenantIds, Boolean enabled) throws SQLException {
        List<String> criterias = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        if(CollectionUtils.isNotEmpty(ids)) {
            criterias.add("id in (:ids)");
            params.put("ids", ids);
        }
        if(CollectionUtils.isNotEmpty(tenantIds)) {
            criterias.add("tenant_id in (:tenant_ids)");
            params.put("tenant_ids", tenantIds);
        }

        if(enabled != null) {
            criterias.add("enabled = :enabled");
            params.put("enabled", enabled);
        }

        String selectSqlBase = "SELECT * FROM " + TENANT_CONFIGS_FULL_NAME;
        String criteria = "";
        if(CollectionUtils.isNotEmpty(criterias)) {
            criteria = " WHERE " + String.join(" AND ", criterias);
        }

        List<String> sortBy = new ArrayList<>();
        sortBy.add("updated_at DESC");
        String orderBy = " ORDER BY " + String.join(",", sortBy);

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<TenantConfig> results = template.query(selectSql, params, TenantConfigConverters.mapTenantConfig());

        Integer totCount = 0;
        if (results.size() > 0) {
            totCount = results.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (results.size() == pageSize) {
                totCount = template.query(countSql, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(results, totCount);
    }
    //endregion

    //region NOT IMPLEMENTED
    @Override
    public Boolean update(String company, TenantConfig t) throws SQLException {
        return null;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
            throw new NotImplementedException();
    }
    //endregion

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {

        List<String> ddl = List.of(
                "CREATE TABLE IF NOT EXISTS " + TENANT_CONFIGS_FULL_NAME + "(\n" +
                        "    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,\n" +
                        "    tenant_id VARCHAR NOT NULL,\n" +
                        "    enabled BOOLEAN NOT NULL DEFAULT true,\n" +
                        "    priority INTEGER NOT NULL DEFAULT 3,\n" +
                        "    marked_for_deletion BOOLEAN NOT NULL DEFAULT false,\n" +
                        "    marked_for_deletion_at TIMESTAMPTZ,\n" +
                        "    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "    CONSTRAINT tenants_valid_priority check (priority >= 0 AND priority <= 5)"+
                ");\n",
                "CREATE UNIQUE INDEX IF NOT EXISTS " + TENANT_CONFIGS_TABLE_NAME + "_tenant_id_uniq_idx ON " + TENANT_CONFIGS_FULL_NAME +"(tenant_id)"
        );

        ddl.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
