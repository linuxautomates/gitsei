package io.levelops.commons.tenant_management.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.tenant_management.converters.TenantIndexSnapshotConverters;
import io.levelops.commons.tenant_management.models.JobStatus;
import io.levelops.commons.tenant_management.models.Offsets;
import io.levelops.commons.tenant_management.models.TenantIndexSnapshot;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.tenant_management.services.TenantConfigDBService.TENANT_CONFIGS_FULL_NAME;
import static io.levelops.commons.tenant_management.services.TenantIndexTypeConfigDBService.TENANT_INDEX_TYPE_CONFIGS_FULL_NAME;

@Log4j2
@Service
public class TenantIndexSnapshotDBService extends DatabaseService<TenantIndexSnapshot>  {
    private static final SchemaType FS_CONTROLLER_SCHEMA = SchemaType.FS_CONTROLLER_SCHEMA;
    private static final String TENANT_INDEX_SNAPSHOTS = "tenant_index_snapshots";
    public static final String TENANT_INDEX_SNAPSHOTS_FULL_NAME = FS_CONTROLLER_SCHEMA.getSchemaName() + "." + TENANT_INDEX_SNAPSHOTS;

    private static final String UPSERT_SQL = "INSERT INTO " + TENANT_INDEX_SNAPSHOTS_FULL_NAME + " (index_type_config_id, index_name, ingested_at, status, priority, heartbeat) values (:index_type_config_id, :index_name, :ingested_at, :status, :priority, :heartbeat) ON CONFLICT (index_type_config_id, ingested_at) DO NOTHING ";
    private static final String SELECT_SQL = "SELECT s.*, ic.index_type as index_type , tc.tenant_id as tenant_id FROM " +  TENANT_INDEX_SNAPSHOTS_FULL_NAME + " AS s "
            + "JOIN " + TENANT_INDEX_TYPE_CONFIGS_FULL_NAME + " AS ic ON ic.id = s.index_type_config_id "
            + "JOIN " + TENANT_CONFIGS_FULL_NAME + " AS tc ON tc.id = ic.tenant_config_id";

    private static final String INDEX_SNAPSHOT_INGESTED_AT_SQL = "SELECT DISTINCT ingested_at FROM %s.jira_issues "+
            " UNION "+
            " SELECT DISTINCT ingested_at FROM %s.issue_mgmt_workitems "+
            " ORDER BY ingested_at";

    String UPDATE_SNAPSHOT_NOT_DELETED_SQL = "UPDATE " + TENANT_INDEX_SNAPSHOTS_FULL_NAME +
            " SET  marked_for_deletion = false, marked_for_deletion_at = null, updated_at = now() WHERE marked_for_deletion = true AND index_type_config_id = :index_type_config_id AND ingested_at IN (:ingested_ats)";

    String UPDATE_SNAPSHOT_DELETED_SQL = "UPDATE " + TENANT_INDEX_SNAPSHOTS_FULL_NAME +
            " SET  marked_for_deletion = true, marked_for_deletion_at = now(), updated_at = now() WHERE marked_for_deletion = false AND index_type_config_id = :index_type_config_id AND ingested_at NOT IN (:ingested_ats)";

    private static final String DELETE_SQL = "DELETE FROM " + TENANT_INDEX_SNAPSHOTS_FULL_NAME + " WHERE id = :id";

    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper objectMapper;

    // region CSTOR
    public TenantIndexSnapshotDBService(DataSource dataSource, ObjectMapper objectMapper) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
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
        return Set.of(TenantIndexTypeConfigDBService.class);
    }
    // endregion

    //region NOT IMPLEMENTED
    @Override
    public String insert(String company, TenantIndexSnapshot t) throws SQLException {
        throw new NotImplementedException();
    }

    @Override
    public Boolean update(String company, TenantIndexSnapshot t) throws SQLException {
        throw new NotImplementedException();
    }
    //endregion

    //region Delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return template.update(DELETE_SQL, Map.of("id", UUID.fromString(id))) > 0;
    }
    //endregion

    //region Get and List
    @Override
    public Optional<TenantIndexSnapshot> get(String company, String id) throws SQLException {
        var results = getBatch(0, 10, Collections.singletonList(UUID.fromString(id)), null, null, null, null, null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }

    @Override
    public DbListResponse<TenantIndexSnapshot> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(0, 10, null, null, null, null, null, null);
    }

    public DbListResponse<TenantIndexSnapshot> listByFilter(Integer pageNumber, Integer pageSize, List<UUID> ids, List<String> tenantIds, Boolean tenantEnabled, Boolean tenantIndexTypeEnabled, Boolean markedForDeletion, Instant markedForDeletionBefore) throws SQLException {
        return getBatch(pageNumber, pageSize, ids, tenantIds, tenantEnabled, tenantIndexTypeEnabled, markedForDeletion, markedForDeletionBefore);
    }

    private DbListResponse<TenantIndexSnapshot> getBatch(Integer pageNumber, Integer pageSize, List<UUID> ids, List<String> tenantIds, Boolean tenantEnabled, Boolean tenantIndexTypeEnabled, Boolean markedForDeletion, Instant markedForDeletionBefore) throws SQLException {
        List<String> criterias = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        if(CollectionUtils.isNotEmpty(ids)) {
            criterias.add("s.id in (:ids)");
            params.put("ids", ids);
        }
        if(CollectionUtils.isNotEmpty(tenantIds)) {
            criterias.add("tc.tenant_id in (:tenant_ids)");
            params.put("tenant_ids", tenantIds);
        }

        if(tenantEnabled != null) {
            criterias.add("tc.enabled = :tenant_enabled");
            params.put("tenant_enabled", tenantEnabled);
        }
        if(tenantIndexTypeEnabled != null) {
            criterias.add("ic.enabled = :tenant_index_type_config_enabled");
            params.put("tenant_index_type_config_enabled", tenantIndexTypeEnabled);
        }
        if(markedForDeletion != null) {
            criterias.add("s.marked_for_deletion = :marked_for_deletion");
            params.put("marked_for_deletion", markedForDeletion);
        }
        if(markedForDeletionBefore != null) {
            criterias.add("s.marked_for_deletion_at < :marked_for_deletion_at_before");
            params.put("marked_for_deletion_at_before", Timestamp.from(markedForDeletionBefore));
        }

        String selectSqlBase = SELECT_SQL;
        String criteria = "";
        if(CollectionUtils.isNotEmpty(criterias)) {
            criteria = " WHERE " + String.join(" AND ", criterias);
        }

        List<String> sortBy = new ArrayList<>();
        sortBy.add("s.updated_at DESC");
        String orderBy = " ORDER BY " + String.join(",", sortBy);

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<TenantIndexSnapshot> results = template.query(selectSql, params, TenantIndexSnapshotConverters.mapTenantTenantIndexSnapshot(objectMapper));

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

    //region Upsert
    public void batchUpsert(List<TenantIndexSnapshot> tenantIndexSnapshots) {
        if(CollectionUtils.isEmpty(tenantIndexSnapshots)) {
            return;
        }
        List<SqlParameterSource> params = tenantIndexSnapshots.stream()
                .map(t -> new MapSqlParameterSource()
                        .addValue("index_type_config_id", t.getIndexTypeConfigId())
                        .addValue("index_name", t.getIndexName())
                        .addValue("ingested_at", t.getIngestedAt())
                        .addValue("status", t.getStatus().toString())
                        .addValue("priority", t.getPriority())
                        .addValue("heartbeat", (t.getHeartbeat() == null)? null : Timestamp.from(t.getHeartbeat()) )
                )
                .collect(Collectors.toList());

        int[] count = this.template.batchUpdate(UPSERT_SQL,params.toArray(new SqlParameterSource[]{}));
    }

    public int markSnapshotsAsNotDeleted(UUID indexTypeConfigId, List<Long> existingIngestedAts) {
        if(CollectionUtils.isEmpty(existingIngestedAts)) {
            return 0;
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("index_type_config_id", indexTypeConfigId);
        params.put("ingested_ats", existingIngestedAts);
        int updatedCount = this.template.update(UPDATE_SNAPSHOT_NOT_DELETED_SQL,params);
        log.debug("updatedCount = {}", updatedCount);
        return updatedCount;
    }

    public int markSnapshotsAsDeleted(UUID indexTypeConfigId, List<Long> existingIngestedAts) {
        if(CollectionUtils.isEmpty(existingIngestedAts)) {
            return 0;
        }
        HashMap<String, Object> params = new HashMap<>();
        params.put("index_type_config_id", indexTypeConfigId);
        params.put("ingested_ats", existingIngestedAts);
        int updatedCount = this.template.update(UPDATE_SNAPSHOT_DELETED_SQL,params);
        log.debug("updatedCount = {}", updatedCount);
        return updatedCount;
    }
    // endregion

    //region Update Index Snapshot Status
    public Integer updateIndexSnapshotStatus(UUID id, JobStatus status, Instant lastRefreshStartedAt){
        if(id == null || status  == null){
            return 0;
        }

        List<String> updates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);

        params.put("status", status.toString());
        updates.add("status = :status");
        updates.add("status_updated_at = now()");

        if(status == JobStatus.FAILURE) {
            updates.add("failed_attempts_count = failed_attempts_count +1");
        } else if (status == JobStatus.SUCCESS) {
            updates.add("failed_attempts_count = 0");

            params.put("last_refresh_started_at", Timestamp.from(lastRefreshStartedAt));
            updates.add("last_refresh_started_at = :last_refresh_started_at");
            updates.add("last_refreshed_at = now()");

            updates.add("index_exist = true");
        }

        String updateString = " SET " + String.join(" , ", updates);

        String sql = "UPDATE "+ TENANT_INDEX_SNAPSHOTS_FULL_NAME +
                updateString +
                " WHERE id = :id ";

        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        return template.update(sql, params);
    }
    // endregion

    //region Update Index Snapshot latest_offset
    public Integer updateIndexSnapshotLatestOffsets(UUID id, Offsets latestOffsets){
        if(id == null || latestOffsets  == null){
            return 0;
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        try {
            params.addValue("latest_offsets", objectMapper.writeValueAsString(latestOffsets));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize dev productivity report to JSON", e);
        }
        //params.put("latest_offset", latestOffset);

        String sql = "UPDATE "+ TENANT_INDEX_SNAPSHOTS_FULL_NAME +
                " SET heartbeat=now(), latest_offsets=:latest_offsets::jsonb, updated_at=now() " +
                " WHERE status = 'pending' AND id = :id ";

        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        return template.update(sql, params);
    }
    // endregion


    //region insert
    public String insertTenantIndexSnapshot(TenantIndexSnapshot tenantIndexSnapshot) {

        String insertTenantSnapshotSql =   "INSERT INTO "+ TENANT_INDEX_SNAPSHOTS_FULL_NAME +
                " (index_type_config_id, index_name, ingested_at, index_exist, status, priority, last_refresh_started_at, last_refreshed_at, latest_offsets, heartbeat) VALUES "+
                " (:indexTypeConfigId, :indexName, :ingestedAt, :indexExist, :status, :priority, :last_refresh_started_at, :last_refreshed_at, :latest_offsets::jsonb, :heartbeat) "+
                " ON CONFLICT (index_type_config_id, ingested_at) \n" +
                " DO UPDATE SET (ingested_at, index_exist, status, priority, last_refresh_started_at, last_refreshed_at, latest_offsets, heartbeat)=(EXCLUDED.ingested_at, EXCLUDED.index_exist, EXCLUDED.status, EXCLUDED.priority, EXCLUDED.last_refresh_started_at, EXCLUDED.last_refreshed_at, EXCLUDED.latest_offsets, EXCLUDED.heartbeat)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("indexTypeConfigId", tenantIndexSnapshot.getIndexTypeConfigId());
        params.addValue("indexName", tenantIndexSnapshot.getIndexName());
        params.addValue("ingestedAt", tenantIndexSnapshot.getIngestedAt());
        params.addValue("indexExist", BooleanUtils.toBooleanDefaultIfNull(tenantIndexSnapshot.getIndexExist(), false));
        params.addValue("status", tenantIndexSnapshot.getStatus().toString());
        params.addValue("priority", tenantIndexSnapshot.getPriority());
        params.addValue("last_refresh_started_at", (tenantIndexSnapshot.getLastRefreshStartedAt() == null)? null : Timestamp.from(tenantIndexSnapshot.getLastRefreshStartedAt()));
        params.addValue("last_refreshed_at", (tenantIndexSnapshot.getLastRefreshedAt() == null)? null : Timestamp.from(tenantIndexSnapshot.getLastRefreshedAt()));
        params.addValue("heartbeat", (tenantIndexSnapshot.getHeartbeat() == null)? null : Timestamp.from(tenantIndexSnapshot.getHeartbeat()));
        try {
            if(tenantIndexSnapshot.getLatestOffsets() != null) {
                params.addValue("latest_offsets", objectMapper.writeValueAsString(tenantIndexSnapshot.getLatestOffsets()));
            } else {
                params.addValue("latest_offsets", "{}");
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize latest offsets to JSON", e);
        }

        log.info("sql = " + insertTenantSnapshotSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(insertTenantSnapshotSql, params, keyHolder);
        log.info("updated rows are "+updatedRows);
        return updatedRows == 0 ? null : keyHolder.getKeys().get("id").toString();
    }
    // endregion

    public List<Long> listIngestedAtForSnapshots(String company){
        String sql = String.format(INDEX_SNAPSHOT_INGESTED_AT_SQL, company, company);
        log.info("sql = " +  sql);
        return template.queryForList(sql, Map.of(), Long.class);
    }

    //region ensureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {

        List<String> ddl = List.of(
                "CREATE TABLE IF NOT EXISTS " + TENANT_INDEX_SNAPSHOTS_FULL_NAME + "(\n" +
                        "    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    index_type_config_id UUID NOT NULL REFERENCES  "+ TENANT_INDEX_TYPE_CONFIGS_FULL_NAME + " (id) ON DELETE CASCADE, " +
                        "    ingested_at bigint NOT NULL DEFAULT 9223372036854775807,\n" +
                        "    index_name VARCHAR NOT NULL,\n" +
                        "    index_exist BOOLEAN NOT NULL DEFAULT false,\n" +
                        "    priority INTEGER NOT NULL,\n" +
                        "    status VARCHAR NOT NULL,\n" +
                        "    status_updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "    last_refresh_started_at TIMESTAMPTZ,\n" +
                        "    last_refreshed_at TIMESTAMPTZ,\n" +
                        "    latest_offsets JSONB NOT NULL DEFAULT '{}'::jsonb,\n" +
                        "    heartbeat TIMESTAMPTZ,\n" +
                        "    failed_attempts_count INTEGER NOT NULL DEFAULT 0,\n" +
                        "    marked_for_deletion BOOLEAN NOT NULL DEFAULT false,\n" +
                        "    marked_for_deletion_at TIMESTAMPTZ,\n" +
                        "    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "    CONSTRAINT tenant_index_snapshots_valid_priority check (priority >= 0 AND priority <= 5)"+
                        ");\n",
                "CREATE UNIQUE INDEX IF NOT EXISTS " + TENANT_INDEX_SNAPSHOTS + "_index_type_config_id_ingested_at_uniq_idx ON " + TENANT_INDEX_SNAPSHOTS_FULL_NAME +"(index_type_config_id, ingested_at)"
        );

        ddl.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    // endregion
}
