package io.levelops.commons.databases.services;

import io.levelops.commons.databases.converters.CiCdPreProcessTaskConverters;
import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.models.database.CiCDPreProcessTask;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
public class CiCdPreProcessTaskService extends DatabaseService<CiCDPreProcessTask> {
    private static final DatabaseService.SchemaType CICD_TASK_SCHEMA = DatabaseService.SchemaType.CICD_TASK_SCHEMA;
    private static final String CICD_PRE_PROCESS_TASK = "task";
    public static final String CICD_PRE_PROCESS_TASK_FULL_NAME = CICD_TASK_SCHEMA.getSchemaName() + "." + CICD_PRE_PROCESS_TASK;

    private static final String INSERT_SQL = "INSERT INTO " + CICD_PRE_PROCESS_TASK_FULL_NAME + " (tenant_id, status, metadata, attempts_count) values (:tenant_id, :status, :metadata, :attempts_count) RETURNING id";
    private static final String SELECT_SQL = "SELECT * FROM " + CICD_PRE_PROCESS_TASK_FULL_NAME;

    private static final String DELETE_SQL = "DELETE FROM " + CICD_PRE_PROCESS_TASK_FULL_NAME + " WHERE id = :id";
    private static final String UPDATE_STATUS_SQL = "UPDATE " + CICD_PRE_PROCESS_TASK_FULL_NAME + " SET status = :status, status_changed_at = (now() at time zone 'UTC'), updated_at = (now() at time zone 'UTC') where id=:id";
    private static final String UPDATE_ATTEMPT_COUNT_SQL = "UPDATE " + CICD_PRE_PROCESS_TASK_FULL_NAME + " SET attempts_count = :attempts_count, updated_at = (now() at time zone 'UTC') where id=:id";
    private static final String UPDATE_STATUS_ATTEMPT_COUNT_SQL = "UPDATE " + CICD_PRE_PROCESS_TASK_FULL_NAME + " SET status = :status, attempts_count = attempts_count+1, updated_at = (now() at time zone 'UTC') where id=:id";
    private final NamedParameterJdbcTemplate template;

    // region CSTOR
    public CiCdPreProcessTaskService(DataSource dataSource) {
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
    public DatabaseService.SchemaType getSchemaType() {
        return CICD_TASK_SCHEMA;
    }
    // endregion

    //region NOT IMPLEMENTED
    @Override
    public Boolean update(String company, CiCDPreProcessTask t) throws SQLException {
        throw new NotImplementedException();
    }
    //endregion

    public Boolean updateStatus(String company, String status, UUID id) {
        template.update(UPDATE_STATUS_SQL, new MapSqlParameterSource().addValue("id", id).addValue("status", status));
        return true;
    }
    public Boolean updateStatusAndAttemptCount(String company, String status, UUID id) {
        template.update(UPDATE_STATUS_ATTEMPT_COUNT_SQL, new MapSqlParameterSource().addValue("id", id).addValue("status", status));
        return true;
    }

    public Boolean updateAttemptsCount(String company, Integer attemptCount, UUID id) {
        template.update(UPDATE_ATTEMPT_COUNT_SQL, new MapSqlParameterSource().addValue("id", id).addValue("attempts_count", attemptCount));
        return true;
    }

    //region Delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return template.update(DELETE_SQL, Map.of("id", UUID.fromString(id))) > 0;
    }
    //endregion

    //region Get and List
    @Override
    public Optional<CiCDPreProcessTask> get(String company, String id) throws SQLException {
        var results = getBatch(0, 10, Collections.singletonList(UUID.fromString(id)), company).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }

    @Override
    public DbListResponse<CiCDPreProcessTask> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(0, 10, null, null);
    }

    public DbListResponse<CiCDPreProcessTask> listByFilter(Integer pageNumber, Integer pageSize, List<UUID> ids, String tenantId) throws SQLException {
        return getBatch(pageNumber, pageSize, ids, tenantId);
    }

    private DbListResponse<CiCDPreProcessTask> getBatch(Integer pageNumber, Integer pageSize, List<UUID> ids, String tenantId) throws SQLException {
        List<String> criterias = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        if (CollectionUtils.isNotEmpty(ids)) {
            criterias.add("id in (:ids)");
            params.put("ids", ids);
        }
        if (tenantId != null) {
            criterias.add("tenant_id = (:tenant_id)");
            params.put("tenant_id", tenantId);
        }

        String selectSqlBase = SELECT_SQL;
        String criteria = "";
        if (CollectionUtils.isNotEmpty(criterias)) {
            criteria = " WHERE " + String.join(" AND ", criterias);
        }

        List<String> sortBy = new ArrayList<>();
        sortBy.add("updated_at DESC");
        String orderBy = " ORDER BY " + String.join(",", sortBy);

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" + selectSqlBase + criteria + ") AS counted";

        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<CiCDPreProcessTask> results = template.query(selectSql, params, CiCdPreProcessTaskConverters.mapCiCdPreProcessTask());

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

    //region Insert
    @Override
    public String insert(String company, CiCDPreProcessTask ciCDPreProcessTask) throws SQLException {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("tenant_id", ciCDPreProcessTask.getTenantId());
        params.addValue("metadata", ciCDPreProcessTask.getMetaData());
        params.addValue("status", ciCDPreProcessTask.getStatus());
        params.addValue("attempts_count", ciCDPreProcessTask.getAttemptCount());

        log.info("sql = " + INSERT_SQL); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(INSERT_SQL, params, keyHolder);
        log.info("updated rows are " + updatedRows);
        return updatedRows == 0 ? null : keyHolder.getKeys().get("id").toString();
    }
    //endregion

    //region Upsert
    public void batchInsert(List<CiCDPreProcessTask> ciCDPreProcessTasks) {
        if (CollectionUtils.isEmpty(ciCDPreProcessTasks)) {
            return;
        }
        List<SqlParameterSource> params = ciCDPreProcessTasks.stream()
                .map(t -> new MapSqlParameterSource()
                        .addValue("tenant_id", t.getTenantId())
                        .addValue("metadata", t.getMetaData())
                        .addValue("status", t.getStatus())
                        .addValue("attempts_count", t.getAttemptCount())
                )
                .collect(Collectors.toList());

        int[] count = this.template.batchUpdate(INSERT_SQL, params.toArray(new SqlParameterSource[]{}));
    }

    //region ensureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {

        List<String> ddl = List.of(
                "CREATE TABLE IF NOT EXISTS " + CICD_PRE_PROCESS_TASK_FULL_NAME + "(\n" +
                        "    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    tenant_id VARCHAR NOT NULL,\n " +
                        "    status VARCHAR NOT NULL,\n" +
                        "    metadata VARCHAR NOT NULL,\n" +
                        "    attempts_count INTEGER NOT NULL,\n" +
                        "    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "    status_changed_at TIMESTAMPTZ NOT NULL DEFAULT now()\n" +
                        ");\n"
        );

        ddl.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}

