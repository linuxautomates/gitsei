package io.levelops.aggregations_shared.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import io.levelops.aggregations_shared.database.converters.JobInstanceConverters;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.database.models.DbJobDefinitionUpdate;
import io.levelops.aggregations_shared.database.models.DbJobInstanceDelete;
import io.levelops.aggregations_shared.database.models.DbJobInstanceFilter;
import io.levelops.aggregations_shared.database.models.DbJobInstanceUpdate;
import io.levelops.aggregations_shared.utils.CompressionUtils;
import io.levelops.aggregations_shared.utils.DatabaseUtils;
import io.levelops.commons.etl.models.DbJobInstance;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobStatus;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ListUtils;
import io.levelops.commons.utils.MapUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.aggregations_shared.database.EtlDatabaseConstants.ETL_SCHEMA;
import static io.levelops.aggregations_shared.database.JobDefinitionDatabaseService.JOB_DEFINITION_TABLE;

@Log4j2
@Service
public class JobInstanceDatabaseService implements DatabaseService<DbJobDefinition> {
    private static final int PAGE_SIZE = 25;
    public static final String JOB_INSTANCE_TABLE = ETL_SCHEMA + ".job_instance";
    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate template;
    private final PlatformTransactionManager transactionManager;
    private final JobDefinitionDatabaseService jobDefinitionDatabaseService;

    @Autowired
    public JobInstanceDatabaseService(ObjectMapper objectMapper, DataSource dataSource, JobDefinitionDatabaseService jobDefinitionDatabaseService) {
        this.objectMapper = objectMapper;
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.transactionManager = new DataSourceTransactionManager(dataSource);
        this.jobDefinitionDatabaseService = jobDefinitionDatabaseService;
    }

    @Override
    public NamedParameterJdbcTemplate getTemplate() {
        return template;
    }

    public JobInstanceId insert(DbJobInstance jobInstance) throws IOException {
        Validate.notNull(jobInstance.getJobDefinitionId(), "Job definition id can not be null");

        var keyHolder = new GeneratedKeyHolder();
        HashMap<String, Object> params = Maps.newHashMap();

        String fields = " instance_id, job_definition_id, worker_id, status, scheduled_start_time, start_time, " +
                "priority, attempt_max, attempt_count, timeout_in_minutes, agg_processor_name, " +
                "last_heartbeat, updated_at, metadata, progress, progress_details, payload, payload_gcs_filename, is_full, is_reprocessing, tags";

        // The instance id is a serially increasing integer sequence for each job definition id
        String instanceIdSubquery = "SELECT COALESCE(MAX(instance_id),0) + 1 from " + JOB_INSTANCE_TABLE + " WHERE job_definition_id = :job_definition_id";

        String values = " (" + instanceIdSubquery + "), :job_definition_id, :worker_id, :status::etl_job_status_t, :scheduled_start_time, :start_time, " +
                ":priority, :attempt_max, :attempt_count, :timeout_in_minutes, :agg_processor_name, " +
                ":last_heartbeat, :updated_at, :metadata::jsonb, :progress::jsonb, :progress_details::json, :payload, :payload_gcs_filename, :is_full, :is_reprocessing, :tags::VARCHAR[]";


        params.put("job_definition_id", jobInstance.getJobDefinitionId());
        params.put("worker_id", jobInstance.getWorkerId());
        params.put("status", jobInstance.getStatus().toString());
        params.put("scheduled_start_time", DatabaseUtils.toTimestamp(jobInstance.getScheduledStartTime()));
        params.put("start_time", DatabaseUtils.toTimestamp(jobInstance.getStartTime()));
        params.put("priority", jobInstance.getPriority().getPriority());
        params.put("attempt_max", jobInstance.getAttemptMax());
        params.put("attempt_count", jobInstance.getAttemptCount());
        params.put("timeout_in_minutes", jobInstance.getTimeoutInMinutes());
        params.put("agg_processor_name", jobInstance.getAggProcessorName());
        params.put("last_heartbeat", DatabaseUtils.toTimestamp(jobInstance.getLastHeartbeat()));
        params.put("updated_at", DatabaseUtils.toTimestamp(MoreObjects.firstNonNull(jobInstance.getUpdatedAt(), Instant.now())));
        params.put("metadata", ParsingUtils.serialize(objectMapper, "metadata", jobInstance.getMetadata(), "{}"));
        params.put("progress", objectMapper.writeValueAsString(MapUtils.emptyIfNull(jobInstance.getProgress())));
        params.put("progress_details", objectMapper.writeValueAsString(MapUtils.emptyIfNull(jobInstance.getProgressDetails())));
        params.put("payload", CompressionUtils.compress(objectMapper.writeValueAsString(jobInstance.getPayload())));
        params.put("payload_gcs_filename", jobInstance.getPayloadGcsFilename());
        params.put("is_full", jobInstance.getIsFull());
        params.put("is_reprocessing", jobInstance.getIsReprocessing());
        params.put("tags", DatabaseUtils.toSqlArray(jobInstance.getTags()));

        if (jobInstance.getCreatedAt() != null) {
            fields += ", created_at";
            values += ", :created_at";
            params.put("created_at", DatabaseUtils.toTimestamp(jobInstance.getCreatedAt()));
        }

        String sql = "INSERT INTO " + JOB_INSTANCE_TABLE + "(" + fields + ") \n" +
                "VALUES(" + values + ") RETURNING job_definition_id, instance_id, id";
        getTemplate().update(sql, new MapSqlParameterSource(params), keyHolder);

        return JobInstanceId.builder()
                .instanceId((Integer) keyHolder.getKeys().get("instance_Id"))
                .jobDefinitionId((UUID) keyHolder.getKeys().get("job_definition_id"))
                .build();
    }

    public Optional<DbJobInstance> get(JobInstanceId id) {
        var records = filter(0, 1, DbJobInstanceFilter.builder()
                .jobInstanceIds(List.of(id))
                .build()).getRecords();
        return Optional.ofNullable(Iterables.get(records, 0, null));
    }

    public DbListResponse<DbJobInstance> filter(Integer pageNumber, Integer pageSize, @Nullable DbJobInstanceFilter filter) {
        int limit = MoreObjects.firstNonNull(pageSize, PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);
        String selectColumns = "id, job_definition_id, instance_id, worker_id, status, " +
                "scheduled_start_time, start_time, priority, attempt_max, attempt_count, " +
                "timeout_in_minutes, agg_processor_name, last_heartbeat, created_at, updated_at, " +
                "status_changed_at, metadata, progress, progress_details, payload_gcs_filename, is_full, is_reprocessing, tags";

        if (!BooleanUtils.isTrue(filter.getExcludePayload())) {
            selectColumns += ", payload";
        }

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();

        params.put("skip", skip);
        params.put("limit", limit);

        if (filter.getJobInstanceIds() != null) {
            if (filter.getJobInstanceIds().size() == 0) {
                return DbListResponse.of(List.of(), 0);
            }
            conditions.add("(job_definition_id, instance_id) in (:job_instance_ids)");
            // jdbc expects a List[Object[]] for tuples
            var compositeIds = filter.getJobInstanceIds().stream()
                    .map(id -> new Object[]{id.getJobDefinitionId(), id.getInstanceId()})
                    .collect(Collectors.toList());
            params.put("job_instance_ids", compositeIds);
        }

        if (filter.getJobDefinitionIds() != null) {
            conditions.add("job_definition_id in (:job_definition_ids)");
            params.put("job_definition_ids", filter.getJobDefinitionIds());
        }

        if (filter.getJobStatuses() != null) {
            conditions.add(generateJobStatusFilter(filter.getJobStatuses()));
        }

        if (filter.getTags() != null) {
            conditions.add("tags @> :tags::VARCHAR[]");
            params.put("tags", DatabaseUtils.toSqlArray(filter.getTags()));
        }

        if (filter.getScheduledTimeAtOrBefore() != null) {
            conditions.add("scheduled_start_time <= :scheduled_time_before");
            params.put("scheduled_time_before", Timestamp.from(filter.getScheduledTimeAtOrBefore()));
        }

        if (filter.getLastHeartbeatBefore() != null) {
            conditions.add("last_heartbeat <= :last_heartbeat");
            params.put("last_heartbeat", Timestamp.from(filter.getLastHeartbeatBefore()));
        }

        if (filter.getLastStatusChangeBefore() != null) {
            conditions.add("status_changed_at <= :status_changed_at");
            params.put("status_changed_at", Timestamp.from(filter.getLastStatusChangeBefore()));
        }

        if (filter.getIsFull() != null) {
            conditions.add("is_full = :is_full");
            params.put("is_full", filter.getIsFull());
        }

        if (BooleanUtils.isTrue(filter.getBelowMaxAttempts())) {
            conditions.add("attempt_count < attempt_max");
        }

        if (BooleanUtils.isTrue(filter.getTimedOut())) {
            conditions.add("now() - make_interval(mins => timeout_in_minutes) >= status_changed_at");
        }

        String orderBy = "";
        if (!ListUtils.isEmpty(filter.getOrderBy())) {
            orderBy = " ORDER BY " + filter.getOrderBy().stream()
                    .map(DbJobInstanceFilter.JobInstanceOrderByField::toSql)
                    .collect(Collectors.joining(","));
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT " + selectColumns + " FROM " + JOB_INSTANCE_TABLE +
                where + orderBy +
                " OFFSET :skip " +
                " LIMIT :limit ";

        log.debug("Filter SQL: " + sql);
        log.debug("Params: {}", params);

        List<DbJobInstance> results = getTemplate().query(sql, params, JobInstanceConverters.rowMapper(objectMapper));
        String countSql = "SELECT count(*) FROM " + JOB_INSTANCE_TABLE + where;
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    private static String generateJobStatusFilter(List<JobStatus> statuses) {
        return String.format("status IN (%s)", statuses.stream()
                .map(JobStatus::toString)
                .map(str -> String.format("'%s'::etl_job_status_t", str))
                .collect(Collectors.joining(",")));
    }

    public Stream<DbJobInstance> stream(@Nullable DbJobInstanceFilter filter) {
        return PaginationUtils.stream(0, 1, page -> filter(page, PAGE_SIZE, filter).getRecords());
    }

    public Boolean update(JobInstanceId id, DbJobInstanceUpdate update) throws JsonProcessingException {
        Validate.notNull(id, "job instance id can not be null");

        List<String> updates = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        if (update.getJobStatus() != null) {
            updates.add("status = :status::etl_job_status_t");
            params.put("status", update.getJobStatus().toString());
        }

        if (update.getWorkerId() != null) {
            updates.add("worker_id = :worker_id");
            params.put("worker_id", update.getWorkerId());
        }

        if (BooleanUtils.isTrue(update.getIncrementAttemptCount())) {
            updates.add("attempt_count = attempt_count + 1");
        }

        if (update.getHeartbeat() != null) {
            updates.add("last_heartbeat = :heartbeat");
            params.put("heartbeat", Timestamp.from(update.getHeartbeat()));
        }

        if (update.getProgress() != null) {
            updates.add("progress = :progress::jsonb");
            params.put("progress", objectMapper.writeValueAsString(update.getProgress()));
        }

        if (update.getProgressDetails() != null) {
            updates.add("progress_details = :progress_details::json");
            params.put("progress_details", objectMapper.writeValueAsString(update.getProgressDetails()));
        }

        if (update.getStartTime() != null) {
            updates.add("start_time = :start_time");
            params.put("start_time", Timestamp.from(update.getStartTime()));
        }
        if (update.getMetadata() != null) {
            updates.add("metadata = :metadata::jsonb");
            params.put("metadata", ParsingUtils.serialize(objectMapper, "metadata", update.getMetadata(), "{}"));
        }
        if (update.getPayloadGcsFileName() != null) {
            updates.add("payload_gcs_filename= :payload_gcs_filename");
            params.put("payload_gcs_filename", update.getPayloadGcsFileName());
        }

        if (updates.isEmpty()) {
            return true;
        }

        updates.add("updated_at = now()");

        // -- conditions
        conditions.add("job_definition_id = :job_definition_id");
        params.put("job_definition_id", id.getJobDefinitionId());
        conditions.add("instance_id = :instance_id");
        params.put("instance_id", id.getInstanceId());

        if (update.getStatusCondition() != null) {
            conditions.add("status = :old_status::etl_job_status_t");
            params.put("old_status", update.getStatusCondition().toString());
        }

        if (update.getWorkerIdCondition() != null) {
            conditions.add("worker_id = :old_worker_id");
            params.put("old_worker_id", update.getWorkerIdCondition());
        }

        String where = String.join(" AND ", conditions);

        String sql = "UPDATE " + JOB_INSTANCE_TABLE +
                " SET " + String.join(", ", updates) + " " +
                " WHERE " + where;
        log.debug("Update SQL = {}", sql);
        log.debug("Params = {} ", params);
        return template.update(sql, params) > 0;
    }

    public JobInstanceId insertAndUpdateJobDefinition(DbJobInstance jobInstance, Map<String, Object> metadataUpdate, Instant now) throws IOException {
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);

        try {
            JobInstanceId id = insert(jobInstance);
            DbJobDefinitionUpdate.DbJobDefinitionUpdateBuilder updateBuilder = DbJobDefinitionUpdate.builder()
                    .lastIterationTs(now)
                    .whereClause(DbJobDefinitionUpdate.WhereClause.builder()
                            .id(jobInstance.getJobDefinitionId())
                            .build());
            // if there is a metadata update, add it to the existing data
            if (!MapUtils.isEmpty(metadataUpdate)) {
                Map<String, Object> existingMetadata = jobDefinitionDatabaseService.get(jobInstance.getJobDefinitionId())
                        .map(DbJobDefinition::getMetadata)
                        .orElse(null);
                Map<String, Object> updatedMetadata = MapUtils.merge(existingMetadata, metadataUpdate);
                updateBuilder.metadata(updatedMetadata);
            }
            Boolean result = jobDefinitionDatabaseService.update(updateBuilder.build());
            if (!result) {
                throw new RuntimeException("Unable to update job definition with latest iteration ts");
            }
            transactionManager.commit(txStatus);
            return id;
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw e;
        }
    }

    public int delete(DbJobInstanceDelete filter) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (filter.getJobInstanceIds() != null && filter.getJobInstanceIds().size() > 0) {
            conditions.add("(job_definition_id, instance_id) in (:job_instance_ids)");
            // jdbc expects a List[Object[]] for tuples
            var compositeIds = filter.getJobInstanceIds().stream()
                    .map(id -> new Object[]{id.getJobDefinitionId(), id.getInstanceId()})
                    .collect(Collectors.toList());
            params.put("job_instance_ids", compositeIds);
        }

        if (filter.getCreatedAtBefore() != null) {
            conditions.add("created_at <= :created_at_before");
            params.put("created_at_before", DatabaseUtils.toTimestamp(filter.getCreatedAtBefore()));
        }

        String where = String.join(" AND ", conditions);
        String sql = "DELETE from " + JOB_INSTANCE_TABLE +
                " WHERE " + where;
        log.info("Delete SQL = {}", sql);
        log.info("Params = {} ", params);
        return template.update(sql, params);
    }

    @Override
    public void ensureTableExistence() throws SQLException {
        createJobStatusType();
        String sql = "CREATE TABLE IF NOT EXISTS " + JOB_INSTANCE_TABLE + " (" +
                "        id                             UUID DEFAULT uuid_generate_v4()," + // maybe remove this if it's making things confusing
                "        job_definition_id              UUID," +
                "        instance_id                    INTEGER," +
                "        worker_id                      VARCHAR(255)," +
                "        status                         etl_job_status_t NOT NULL DEFAULT 'unassigned'," +
                "        scheduled_start_time           TIMESTAMPTZ," +
                "        start_time                     TIMESTAMPTZ," +
                "        priority                       INTEGER NOT NULL DEFAULT 2," +
                "        attempt_max                    INTEGER," +
                "        attempt_count                  INTEGER," +
                "        timeout_in_minutes             INTEGER," +
                "        agg_processor_name             VARCHAR(255)," +
                "        last_heartbeat                 TIMESTAMPTZ," +
                "        created_at                     TIMESTAMPTZ NOT NULL DEFAULT now()," +
                "        updated_at                     TIMESTAMPTZ NOT NULL DEFAULT now()," +
                "        status_changed_at              TIMESTAMPTZ NOT NULL DEFAULT now()," +
                "        metadata                       JSONB," +
                "        progress                       JSONB," +
                "        progress_details               JSON," +
                "        payload                        BYTEA," +
                "        payload_gcs_filename           VARCHAR(1000)," +
                "        is_full                        BOOLEAN," +
                "        is_reprocessing                BOOLEAN," +
                "        tags                           VARCHAR(64)[] NOT NULL DEFAULT '{}'," +
                "    PRIMARY KEY(job_definition_id, instance_id)," +
                "    CONSTRAINT FK_job_definition FOREIGN KEY(job_definition_id) REFERENCES " + JOB_DEFINITION_TABLE + "(id)" +
                ")";

        log.debug("sql={}", sql);
        getTemplate().getJdbcTemplate().execute(sql);

        createStatusUpdatedAtTrigger();

        List.of(
                "CREATE INDEX IF NOT EXISTS etl_scheduler_job_status_index ON " + JOB_INSTANCE_TABLE + "(status, scheduled_start_time, priority)",
                "CREATE INDEX IF NOT EXISTS etl_scheduler_job_definition_id_index ON " + JOB_INSTANCE_TABLE + "(job_definition_id, instance_id)"
        ).forEach(getTemplate().getJdbcTemplate()::execute);

        log.info("Ensured table existence: {}", JOB_INSTANCE_TABLE);
    }

    private void createStatusUpdatedAtTrigger() {
//        getTemplate().getJdbcTemplate().execute("DROP function IF EXISTS trigger_set_status_updated_at;");
        String functionSql =
                "CREATE OR REPLACE FUNCTION trigger_set_status_updated_at() " +
                        "RETURNS TRIGGER AS $$ " +
                        "BEGIN " +
                        "NEW.status_changed_at = NOW(); " +
                        "RETURN NEW; " +
                        "END; " +
                        "$$ LANGUAGE plpgsql; ";

        String triggerSql =
                "CREATE TRIGGER update_status_time " +
                        "BEFORE UPDATE ON " + JOB_INSTANCE_TABLE + " " +
                        "FOR EACH ROW " +
                        "WHEN (OLD.status IS DISTINCT FROM NEW.status AND old.status_changed_at IS NOT DISTINCT FROM NEW.status_changed_at) " +
                        "EXECUTE FUNCTION trigger_set_status_updated_at(); ";

        try {
            getTemplate().getJdbcTemplate().execute(functionSql);
            getTemplate().getJdbcTemplate().execute(triggerSql);
        } catch (BadSqlGrammarException e) {
            if (e.getMessage().contains("ERROR: trigger \"update_status_time\" for relation \"job_instance\" already exists")) {
                log.info("update_status_time trigger already exists");
                return;
            }
            log.error("Failed to create status changed at trigger", e);
        }
        log.info("Created update_status_time trigger");
    }

    private void createJobStatusType() {
        try {
//        getTemplate().getJdbcTemplate().execute("DROP TYPE IF EXISTS etl_job_status_t;");
            String sql = String.format("CREATE TYPE etl_job_status_t AS ENUM (%s)",
                    Stream.of(JobStatus.values())
                            .map(JobStatus::toString)
                            .map(String::toLowerCase)
                            .map(s -> StringUtils.wrap(s, "'"))
                            .collect(Collectors.joining(", ")));
            log.debug("sql={}", sql);
            getTemplate().getJdbcTemplate().execute(sql);
            log.info("Created etl_job_status_t enum type");
        } catch (DataAccessException e) {
            if (e.getMessage().contains("ERROR: type \"etl_job_status_t\" already exists")) {
                log.info("job_status_t enum type already exists");
                return;
            }
            log.error("Failed to create job_status_t enum type", e);
        }
    }
}
