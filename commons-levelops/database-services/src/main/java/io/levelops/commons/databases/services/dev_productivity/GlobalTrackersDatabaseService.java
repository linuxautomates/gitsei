package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.converters.GlobalTrackersConverters;
import io.levelops.commons.databases.models.database.dev_productivity.GlobalTracker;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
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
import java.util.*;

@Log4j2
@Service
public class GlobalTrackersDatabaseService extends DatabaseService<GlobalTracker> {

    private static final String INSERT_SQL_FORMAT = "INSERT INTO %s.global_trackers(type, frequency, status, status_changed_at) VALUES (:type, :frequency, :status, now()) ON CONFLICT DO NOTHING RETURNING id";
    private static final String DELETE_SQL_FORMAT = "DELETE FROM %s.global_trackers WHERE id = :id";

    private static final String UPDATE_FREQUENCY_BY_TYPE_SQL_FORMAT = "UPDATE %s.global_trackers SET frequency = :frequency, updated_at=now() where type = :type";

    private static final String UPDATE_STATUS_BY_TYPE_SQL_FORMAT = "UPDATE %s.global_trackers SET status = :status, status_changed_at=now(), updated_at=now() where type = :type";
    private static final String UPDATE_STATUS_BY_TYPE_AND_TIME_IN_SECONDS_SQL_FORMAT = "UPDATE %s.global_trackers SET status = :status, status_changed_at=now(), updated_at=now() where type = :type and (extract(epoch from now()) - extract(epoch from status_changed_at) >= :interval)";
    private static final String UPDATE_STATUS_BY_TYPE_AND_STATUS_SQL_FORMAT = "UPDATE %s.global_trackers SET status = :new_status, status_changed_at=now(), updated_at=now() where type = :type and status = :current_status";

    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper objectMapper;

    public GlobalTrackersDatabaseService(DataSource dataSource, ObjectMapper objectMapper) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
    }

    //region insert & insert safe
    private MapSqlParameterSource constructParameterSourceForGlobalTracker(GlobalTracker t, final UUID existingId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("type", t.getType());//type, frequency, status, start_time, end_time
        params.addValue("frequency", t.getFrequency());
        params.addValue("status", t.getStatus());
        return params;
    }

    private int insertInternal(String company, GlobalTracker t, KeyHolder keyHolder) throws SQLException {
        MapSqlParameterSource params = constructParameterSourceForGlobalTracker(t, null);
        String insertReportSql = String.format(INSERT_SQL_FORMAT, company);
        int updatedRows = template.update(insertReportSql, params, keyHolder);
        return updatedRows;
    }
    @Override
    public String insert(String company, GlobalTracker t) throws SQLException {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = insertInternal(company, t, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to insert the Global tracker");
        }
        UUID trackerId = (UUID) keyHolder.getKeys().get("id");
        return trackerId.toString();
    }
    public Optional<UUID> insertSafe(String company, GlobalTracker t) throws SQLException {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = insertInternal(company, t, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return Optional.empty();
        }
        UUID trackerId = (UUID) keyHolder.getKeys().get("id");
        return Optional.ofNullable(trackerId);
    }
    //endregion

    //region update
    @Override
    public Boolean update(String company, GlobalTracker t) throws SQLException {
        Validate.notNull(t.getId(), "Tracker Id can not be null");
        UUID trackerId = t.getId();
        Boolean updated = false;
        List<String> updates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("id", trackerId);
        boolean unsetCurrentDefaultScheme = false;
        if (t.getType() != null) {
            updates.add("type =:type");
            params.put("type", t.getType());
        }
        if (t.getFrequency() != null) {
            updates.add("frequency =:frequency");
            params.put("frequency", t.getFrequency());
        }
        if (t.getStatus() != null) {
            updates.add("status =:status");
            params.put("status", t.getStatus());
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
            String sql = "UPDATE " + company + ".global_trackers " +
                    " SET " + String.join(", ", updates) +
                    " WHERE id = :id::uuid ";

            updated = template.update(sql, params) > 0;
        } catch (Exception e) {
            log.error("Error while updating the global tracker table global_trackers" + e);
            throw e;
        }
        return updated;
    }
    //endregion

    //region update frequency
    public boolean updateFrequencyByType(final String company, final String type, final Integer frequencyInMinutes) {
        Validate.notBlank(company, "Company cannot be null or empty!");
        Validate.notBlank(type, "Type cannot be null or empty!");
        Validate.notNull(frequencyInMinutes, "Frequency cannot be null!");

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("type", type);
        params.addValue("frequency", frequencyInMinutes);

        String updateFrequencySql = String.format(UPDATE_FREQUENCY_BY_TYPE_SQL_FORMAT, company);
        int updatedRows = template.update(updateFrequencySql, params);
        return updatedRows > 0;
    }
    //endregion

    //region update status
    public boolean updateStatusByType(final String company, final String type, final String status) {
        Validate.notBlank(company, "Company cannot be null or empty!");
        Validate.notBlank(type, "Type cannot be null or empty!");
        Validate.notBlank(status, "Status cannot be null!");

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("type", type);
        params.addValue("status", status);

        String updateFrequencySql = String.format(UPDATE_STATUS_BY_TYPE_SQL_FORMAT, company);
        int updatedRows = template.update(updateFrequencySql, params);
        return updatedRows > 0;
    }
    public boolean updateStatusByTypeAndTimeInSeconds(final String company, final String type, final Integer interval, final String status) {
        Validate.notBlank(company, "Company cannot be null or empty!");
        Validate.notBlank(type, "Type cannot be null or empty!");
        Validate.notNull(interval, "Interval cannot be null or empty!");
        Validate.notBlank(status, "Status cannot be null!");

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("type", type);
        params.addValue("interval", interval);
        params.addValue("status", status);

        String updateFrequencySql = String.format(UPDATE_STATUS_BY_TYPE_AND_TIME_IN_SECONDS_SQL_FORMAT, company);
        int updatedRows = template.update(updateFrequencySql, params);
        return updatedRows > 0;
    }
    public boolean updateStatusByTypeAndStatus(final String company, final String type, final String currentStatus, final String newStatus) {
        Validate.notBlank(company, "Company cannot be null or empty!");
        Validate.notBlank(type, "Type cannot be null or empty!");
        Validate.notBlank(currentStatus, "Status cannot be null!");
        Validate.notBlank(newStatus, "Status cannot be null!");

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("type", type);
        params.addValue("current_status", currentStatus);
        params.addValue("new_status", newStatus);

        String updateFrequencySql = String.format(UPDATE_STATUS_BY_TYPE_AND_STATUS_SQL_FORMAT, company);
        int updatedRows = template.update(updateFrequencySql, params);
        return updatedRows > 0;
    }
    //endregion

    //region get
    @Override
    public Optional<GlobalTracker> get(String company, String id) throws SQLException {
        String sql = "SELECT * FROM " + company + ".global_trackers" +
                " WHERE id = :id::uuid " +
                " LIMIT 1 ";
        GlobalTracker res = null;
        try {
            List<GlobalTracker> results = template.query(sql, Map.of("id", id),
                    GlobalTrackersConverters.rowMapper(objectMapper));
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
    public DbListResponse<GlobalTracker> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        String selectSql = "SELECT * FROM " + company + ".global_trackers ORDER BY updated_at desc" + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSQL = "SELECT COUNT(*) FROM (" + selectSql + ") AS counted";

        Integer totCount = 0;
        log.debug("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        List<GlobalTracker> globalTrackerList = template.query(selectSql, GlobalTrackersConverters.rowMapper(objectMapper));
        log.debug("globalTrackersList.size() = {}", globalTrackerList.size());
        if (globalTrackerList.size() > 0) {
            //fetch sections
            totCount = globalTrackerList.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (globalTrackerList.size() == pageSize) {
                log.debug("sql = " + countSQL); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
                totCount = template.query(countSQL, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(globalTrackerList, totCount);
    }
    //endregion

    //region delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String deleteSql = String.format(DELETE_SQL_FORMAT, company);
        return template.update(deleteSql, Map.of("id", UUID.fromString(id))) > 0;
    }
    //endregion

    //region ensureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList= List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".global_trackers\n" +
                        "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    \"type\" character varying NOT NULL,\n" +
                        "    frequency integer NOT NULL,\n" +
                        "    \"status\" character varying NOT NULL,\n" +
                        "    status_changed_at TIMESTAMPTZ NOT NULL,\n" +
                        "    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),\n" +
                        "    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()\n" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS uniq_global_trackers_type_idx on " + company + ".global_trackers (type)"
        );
        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    //endregion
}
