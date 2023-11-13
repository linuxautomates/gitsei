package io.levelops.commons.tenant_management.services;

import io.levelops.commons.databases.converters.DbUUIDConverter;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.tenant_management.models.TenantIndexSnapshot;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static io.levelops.commons.tenant_management.services.TenantConfigDBService.TENANT_CONFIGS_FULL_NAME;
import static io.levelops.commons.tenant_management.services.TenantIndexSnapshotDBService.TENANT_INDEX_SNAPSHOTS_FULL_NAME;
import static io.levelops.commons.tenant_management.services.TenantIndexTypeConfigDBService.TENANT_INDEX_TYPE_CONFIGS_FULL_NAME;

@Log4j2
@Service
public class TenantIndexSnapshotJobsDBService {
    private final NamedParameterJdbcTemplate template;
    private final TenantIndexSnapshotDBService tenantIndexSnapshotDBService;

    @Autowired
    public TenantIndexSnapshotJobsDBService(DataSource dataSource, TenantIndexSnapshotDBService tenantIndexSnapshotDBService) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.tenantIndexSnapshotDBService = tenantIndexSnapshotDBService;
    }

    private static final String SCHEDULE_JOBS_SELECT_SQL = "SELECT s.id FROM " +  TENANT_INDEX_SNAPSHOTS_FULL_NAME + " AS s "
            + "JOIN " + TENANT_INDEX_TYPE_CONFIGS_FULL_NAME + " AS ic ON ic.id = s.index_type_config_id "
            + "JOIN " + TENANT_CONFIGS_FULL_NAME + " AS tc ON tc.id = ic.tenant_config_id "
            + "WHERE tc.enabled=true AND ic.enabled=true AND tc.marked_for_deletion=false AND s.marked_for_deletion=false AND ( "
            + "(s.status = 'unassigned')" + " OR " //UNASSIGNED tasks
            + "(s.status = 'pending' AND ic.index_type != 'WORK_ITEMS' AND now() - COALESCE(heartbeat,status_updated_at) > interval '30 minutes')" + " OR " //non wi tasks (SCM_PR & SCM_COMMIT) stuck in pending for 30 minutes
            + "(s.status = 'pending' AND ic.index_type = 'WORK_ITEMS' AND now() - COALESCE(heartbeat,status_updated_at) > interval '2 hours')" + " OR " //wi tasks (WORK_ITEMS) stuck in pending for 2 hours
            + "(s.status = 'failure' AND failed_attempts_count < 5)" + " OR " //failed tasks having failed attempts count less than 5
            + "(s.status = 'success' AND s.ingested_at IN ('9223372036854775807', :todays_ingested_at) AND ( EXTRACT(EPOCH FROM now() - last_refreshed_at)/60 > ic.frequency_in_mins ) )" + " OR " //for successful tasks with todays ingested_at or 9223372036854775807, if last_refreshed_at is older than frequency_in_mins
            + "(s.status = 'success' AND s.ingested_at NOT IN ('9223372036854775807', :todays_ingested_at) AND (EXTRACT(EPOCH FROM (last_refresh_started_at::date)) <= s.ingested_at ) )" //for successful tasks NOT with todays ingested_at or 9223372036854775807, if last refresh for the index did not start on next day
            +" )";
    private static final String SCHEDULE_JOBS_UPDATE_SQL = "UPDATE " +  TENANT_INDEX_SNAPSHOTS_FULL_NAME + " SET status = 'scheduled', status_updated_at = now(), updated_at = now() WHERE id in ( " + SCHEDULE_JOBS_SELECT_SQL + " )";

    private static final String ASSIGN_JOBS_SELECT_SQL = "SELECT s.id as id FROM " +  TENANT_INDEX_SNAPSHOTS_FULL_NAME + " AS s "
            + "JOIN " + TENANT_INDEX_TYPE_CONFIGS_FULL_NAME + " AS ic ON ic.id = s.index_type_config_id "
            + "JOIN " + TENANT_CONFIGS_FULL_NAME + " AS tc ON tc.id = ic.tenant_config_id "
            + "WHERE tc.enabled=true AND ic.enabled=true AND tc.marked_for_deletion=false AND s.marked_for_deletion=false AND status = 'scheduled' "
            + "ORDER by s.priority, (EXTRACT(EPOCH FROM now() - COALESCE(last_refreshed_at,to_timestamp(0)))/60 - ic.frequency_in_mins) desc, s.ingested_at desc, s.updated_at desc " //First sort by priority asc, then lag desc, then ingested at desc (SCMs first then WI latest then WI older), final snapshot updated_at desc
            + "LIMIT 1";
    private static final String ASSIGN_JOBS_UPDATE_SQL = "UPDATE " +  TENANT_INDEX_SNAPSHOTS_FULL_NAME + " SET status = 'pending', status_updated_at = now(), heartbeat=null, updated_at = now() WHERE id in ( " + ASSIGN_JOBS_SELECT_SQL + " ) RETURNING id";

    private static final String SCHEDULE_JOBS_BY_TENANT_ID_INGESTED_AT_SELECT_SQL = "SELECT s.id FROM " +  TENANT_INDEX_SNAPSHOTS_FULL_NAME + " AS s "
            + "JOIN " + TENANT_INDEX_TYPE_CONFIGS_FULL_NAME + " AS ic ON ic.id = s.index_type_config_id "
            + "JOIN " + TENANT_CONFIGS_FULL_NAME + " AS tc ON tc.id = ic.tenant_config_id "
            + "WHERE tc.enabled=true AND ic.enabled=true AND tc.marked_for_deletion=false AND s.marked_for_deletion=false AND s.status NOT IN ('scheduled','pending') AND tc.tenant_id=:tenant_id AND ic.index_type='WORK_ITEMS' AND s.ingested_at=:ingested_at";
    private static final String SCHEDULE_JOBS_BY_TENANT_ID_INGESTED_AT_UPDATE_SQL = "UPDATE " +  TENANT_INDEX_SNAPSHOTS_FULL_NAME + " SET status = 'scheduled', status_updated_at = now(), updated_at = now() WHERE id in ( " + SCHEDULE_JOBS_BY_TENANT_ID_INGESTED_AT_SELECT_SQL + " )";

    public int scheduleJobs() {
        Date currentTime = new Date();
        Long todaysIngestedAt = DateUtils.truncate(currentTime, Calendar.DATE);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("todays_ingested_at", todaysIngestedAt);

        log.info("sql = " + SCHEDULE_JOBS_UPDATE_SQL); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        int updatedRows = template.update(SCHEDULE_JOBS_UPDATE_SQL, params);
        log.info("scheduleJobs updatedRows = {}", updatedRows);
        return updatedRows;
    }

    public Optional<TenantIndexSnapshot> assignJob() throws SQLException {
        Date currentTime = new Date();
        Long todaysIngestedAt = DateUtils.truncate(currentTime, Calendar.DATE);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("todays_ingested_at", todaysIngestedAt);

        log.info("sql = " + ASSIGN_JOBS_UPDATE_SQL); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        List<UUID> ids = template.query(ASSIGN_JOBS_UPDATE_SQL, params, DbUUIDConverter.uuidMapper());
        log.info("assignJob updatedRows ids = {}", ids);
        if(CollectionUtils.isEmpty(ids)) {
            return Optional.empty();
        }
        if(ids.size() > 1) {
            log.error("Assigned more than one job! ids = {}", ids);
        }
        return tenantIndexSnapshotDBService.get(null, ids.get(0).toString());
    }

    /**
     * This function is used by the integration_tracker monitoring piece.
     * @param tenantId
     * @param ingestedAt
     * @return
     */
    public int scheduleJobsByTenantIdAndIngestedAt(final String tenantId, final Long ingestedAt) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("tenant_id", tenantId);
        params.addValue("ingested_at", ingestedAt);

        log.info("sql = " + SCHEDULE_JOBS_BY_TENANT_ID_INGESTED_AT_UPDATE_SQL); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        int updatedRows = template.update(SCHEDULE_JOBS_BY_TENANT_ID_INGESTED_AT_UPDATE_SQL, params);
        log.info("scheduleJobsByTenantIdAndIngestedAt updatedRows = {}", updatedRows);
        return updatedRows;
    }
}
