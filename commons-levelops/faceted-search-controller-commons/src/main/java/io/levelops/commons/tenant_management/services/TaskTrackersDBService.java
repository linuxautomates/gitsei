package io.levelops.commons.tenant_management.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.tenant_management.converters.TaskTrackersConverters;
import io.levelops.commons.tenant_management.models.TaskStatus;
import io.levelops.commons.tenant_management.models.TaskTracker;
import io.levelops.commons.tenant_management.models.TaskType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
public class TaskTrackersDBService extends DatabaseService<TaskTracker>   {
    private static final DatabaseService.SchemaType FS_CONTROLLER_SCHEMA = DatabaseService.SchemaType.FS_CONTROLLER_SCHEMA;
    private static final String TASK_TRACKERS_TABLE_NAME = "task_trackers";
    public static final String TASK_TRACKERS_FULL_NAME = FS_CONTROLLER_SCHEMA.getSchemaName() + "." + TASK_TRACKERS_TABLE_NAME;

    private static final String INSERT_SQL_FORMAT = "INSERT INTO " + TASK_TRACKERS_FULL_NAME + "(type, frequency, status, status_changed_at) VALUES (:type, :frequency, :status, now()) ON CONFLICT DO NOTHING RETURNING id";
    private static final String DELETE_SQL_FORMAT = "DELETE FROM " + TASK_TRACKERS_FULL_NAME + " WHERE id = :id";

    private static final String UPDATE_FREQUENCY_BY_TYPE_SQL_FORMAT = "UPDATE " + TASK_TRACKERS_FULL_NAME + " SET frequency = :frequency, updated_at=now() where type = :type";

    private static final String UPDATE_STATUS_BY_TYPE_SQL_FORMAT = "UPDATE " + TASK_TRACKERS_FULL_NAME + " SET status = :status, status_changed_at=now(), updated_at=now() where type = :type";
    private static final String UPDATE_STATUS_BY_TYPE_AND_TIME_IN_SECONDS_SQL_FORMAT = "UPDATE " + TASK_TRACKERS_FULL_NAME + " SET status = :status, status_changed_at=now(), updated_at=now() where type = :type and (extract(epoch from now()) - extract(epoch from status_changed_at) >= :interval)";
    private static final String UPDATE_STATUS_BY_TYPE_AND_STATUS_SQL_FORMAT = "UPDATE " + TASK_TRACKERS_FULL_NAME + " SET status = :new_status, status_changed_at=now(), updated_at=now() where type = :type and status = :current_status";

    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper objectMapper;

    // region CSTOR
    public TaskTrackersDBService(DataSource dataSource, ObjectMapper objectMapper) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
    }
    // endregion

    // region isTenantSpecific and References
    @Override
    public boolean isTenantSpecific() {
        return false;
    }
    @Override
    public DatabaseService.SchemaType getSchemaType() {
        return FS_CONTROLLER_SCHEMA;
    }
    // endregion


    //region insert & insert safe
    private MapSqlParameterSource constructParameterSourceForGlobalTracker(TaskTracker t, final UUID existingId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("type", t.getType().toString());//type, frequency, status, start_time, end_time
        params.addValue("frequency", t.getFrequency());
        params.addValue("status", t.getStatus().toString());
        return params;
    }

    private int insertInternal(TaskTracker t, KeyHolder keyHolder) throws SQLException {
        MapSqlParameterSource params = constructParameterSourceForGlobalTracker(t, null);
        int updatedRows = template.update(INSERT_SQL_FORMAT, params, keyHolder);
        return updatedRows;
    }
    @Override
    public String insert(String company, TaskTracker t) throws SQLException {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = insertInternal(t, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to insert the Global tracker");
        }
        UUID trackerId = (UUID) keyHolder.getKeys().get("id");
        return trackerId.toString();
    }
    public Optional<UUID> insertSafe(TaskTracker t) throws SQLException {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = insertInternal(t, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return Optional.empty();
        }
        UUID trackerId = (UUID) keyHolder.getKeys().get("id");
        return Optional.ofNullable(trackerId);
    }
    //endregion

    //region update
    @Override
    public Boolean update(String company, TaskTracker t) throws SQLException {
        Validate.notNull(t.getId(), "Tracker Id can not be null");
        UUID trackerId = t.getId();
        Boolean updated = false;
        List<String> updates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("id", trackerId);
        boolean unsetCurrentDefaultScheme = false;
        if (t.getType() != null) {
            updates.add("type =:type");
            params.put("type", t.getType().toString());
        }
        if (t.getFrequency() != null) {
            updates.add("frequency =:frequency");
            params.put("frequency", t.getFrequency());
        }
        if (t.getStatus() != null) {
            updates.add("status =:status");
            params.put("status", t.getStatus().toString());
        }
        if (t.getStatusChangedAt() != null) {
            updates.add("status_changed_at =:status_changed_at");
            params.put("status_changed_at", Timestamp.from(t.getStatusChangedAt()));
        }
        if (updates.isEmpty()) {
            return true;
        }
        try {
            updates.add("updated_at = now()");
            String sql = "UPDATE " + TASK_TRACKERS_FULL_NAME +
                    " SET " + String.join(", ", updates) +
                    " WHERE id = :id::uuid ";

            updated = template.update(sql, params) > 0;
        } catch (Exception e) {
            log.error("Error while updating the task trackers table", e);
            throw e;
        }
        return updated;
    }
    //endregion

    //region update frequency
    public boolean updateFrequencyByType(final TaskType type, final Integer frequencyInMinutes) {
        Validate.notNull(type, "Type cannot be null or empty!");
        Validate.notNull(frequencyInMinutes, "Frequency cannot be null!");

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("type", type.toString());
        params.addValue("frequency", frequencyInMinutes);

        int updatedRows = template.update(UPDATE_FREQUENCY_BY_TYPE_SQL_FORMAT, params);
        return updatedRows > 0;
    }
    //endregion

    //region update status
    public boolean updateStatusByType(final TaskType type, final TaskStatus status) {
        Validate.notNull(type, "Type cannot be null or empty!");
        Validate.notNull(status, "Status cannot be null!");

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("type", type.toString());
        params.addValue("status", status.toString());

        int updatedRows = template.update(UPDATE_STATUS_BY_TYPE_SQL_FORMAT, params);
        return updatedRows > 0;
    }
    public boolean updateStatusByTypeAndTimeInSeconds(final TaskType type, final Integer interval, final TaskStatus status) {
        Validate.notNull(type, "Type cannot be null or empty!");
        Validate.notNull(interval, "Interval cannot be null or empty!");
        Validate.notNull(status, "Status cannot be null!");

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("type", type.toString());
        params.addValue("interval", interval);
        params.addValue("status", status.toString());

        int updatedRows = template.update(UPDATE_STATUS_BY_TYPE_AND_TIME_IN_SECONDS_SQL_FORMAT, params);
        return updatedRows > 0;
    }
    public boolean updateStatusByTypeAndStatus(final TaskType type, final TaskStatus currentStatus, final TaskStatus newStatus) {
        Validate.notNull(type, "Type cannot be null or empty!");
        Validate.notNull(currentStatus, "Status cannot be null!");
        Validate.notNull(newStatus, "Status cannot be null!");

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("type", type.toString());
        params.addValue("current_status", currentStatus.toString());
        params.addValue("new_status", newStatus.toString());

        int updatedRows = template.update(UPDATE_STATUS_BY_TYPE_AND_STATUS_SQL_FORMAT, params);
        return updatedRows > 0;
    }
    //endregion

    //region get
    @Override
    public Optional<TaskTracker> get(String company, String id) throws SQLException {
        String sql = "SELECT * FROM " + TASK_TRACKERS_FULL_NAME +
                " WHERE id = :id::uuid " +
                " LIMIT 1 ";
        TaskTracker res = null;
        try {
            List<TaskTracker> results = template.query(sql, Map.of("id", id),
                    TaskTrackersConverters.rowMapper(objectMapper));
            res = IterableUtils.getFirst(results).orElse(null);
        } catch (DataAccessException e) {
            log.warn("Failed to get data for Global tracker for id={}", id, e);
            return Optional.empty();
        }
        return Optional.ofNullable(res);
    }
    //endregion

    //region list
    @Override
    public DbListResponse<TaskTracker> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        String selectSql = "SELECT * FROM " + TASK_TRACKERS_FULL_NAME + " ORDER BY updated_at desc" + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSQL = "SELECT COUNT(*) FROM (" + selectSql + ") AS counted";

        Integer totCount = 0;
        log.debug("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        List<TaskTracker> taskTrackerList = template.query(selectSql, TaskTrackersConverters.rowMapper(objectMapper));
        log.debug("globalTrackersList.size() = {}", taskTrackerList.size());
        if (taskTrackerList.size() > 0) {
            //fetch sections
            totCount = taskTrackerList.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (taskTrackerList.size() == pageSize) {
                log.debug("sql = " + countSQL); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
                totCount = template.query(countSQL, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(taskTrackerList, totCount);
    }
    //endregion

    //region delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return template.update(DELETE_SQL_FORMAT, Map.of("id", UUID.fromString(id))) > 0;
    }
    //endregion

    //region ensureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList= List.of(
                "CREATE TABLE IF NOT EXISTS " + TASK_TRACKERS_FULL_NAME + "\n" +
                        "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    \"type\" character varying NOT NULL,\n" +
                        "    frequency integer NOT NULL,\n" +
                        "    \"status\" character varying NOT NULL,\n" +
                        "    status_changed_at TIMESTAMPTZ NOT NULL,\n" +
                        "    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()\n" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS uniq__levelops_faceted_search_controller_task_trackers_type_idx on " + TASK_TRACKERS_FULL_NAME + " (type)"
        );
        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    //endregion
}
