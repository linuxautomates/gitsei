package io.levelops.controlplane.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Maps;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.controlplane.models.DbJob;
import io.levelops.controlplane.models.DbJobConverters;
import io.levelops.controlplane.models.DbJobUpdate;
import io.levelops.controlplane.models.jsonb.JsonUtils;
import io.levelops.controlplane.utils.DatabaseUtils;
import io.levelops.ingestion.models.controlplane.JobStatus;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Log4j2
@Service
public class JobDatabaseService implements DatabaseService<DbJob> {

    private static final String JOBS_TABLE = "control_plane.jobs";
    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public JobDatabaseService(@Qualifier("controlPlaneJdbcTemplate") NamedParameterJdbcTemplate template,
                              ObjectMapper objectMapper) {
        this.template = template;
        this.objectMapper = objectMapper;
    }

    @Override
    public NamedParameterJdbcTemplate getTemplate() {
        return this.template;
    }

    public Stream<DbJob> streamJobs(int pageSize, @Nullable JobFilter jobFilter) {
        return IntStream.iterate(0, i -> i + pageSize)
                .mapToObj(i -> filterJobs(i, pageSize, jobFilter))
                .takeWhile(CollectionUtils::isNotEmpty)
                .flatMap(Collection::stream);
    }

    public List<DbJob> getJobs(int skip, int limit) {
        return filterJobs(skip, limit, null);
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = JobFilter.JobFilterBuilder.class)
    public static class JobFilter {
        List<JobStatus> statuses;
        Boolean reserved;
        String tenantId;
        List<String> integrationIds;
        Instant before;
        Instant after;
        Integer belowMaxAttemptsOrDefaultValue;
        Set<String> tags;
        List<String> controllerNames;
        List<String> excludeControllerNames;
    }

    public List<DbJob> filterJobs(int skip, int limit, @Nullable JobFilter jobFilter) {
        jobFilter = (jobFilter != null)? jobFilter : JobFilter.builder().build();

        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        List<String> conditions = new ArrayList<>();
        // --- filter by status
        if (CollectionUtils.isNotEmpty(jobFilter.getStatuses())) {
            conditions.add(generateJobStatusFilter(jobFilter.getStatuses()));
        }
        // --- filter by reserved flag
        if (jobFilter.getReserved() != null) {
            conditions.add("reserved = :reserved");
            params.put("reserved", jobFilter.getReserved());
        }
        // --- filter by tenantId and integrationIds
        if (StringUtils.isNotEmpty(jobFilter.getTenantId())) {
            conditions.add("tenant_id = :tenant_id");
            params.put("tenant_id", jobFilter.getTenantId());

            if (CollectionUtils.isNotEmpty(jobFilter.getIntegrationIds())) {
                conditions.add("integration_id IN (:integrationIds)");
                params.put("integrationIds", jobFilter.getIntegrationIds());
            }
        }
        // --- filter by timestamp
        if (jobFilter.getBefore() != null) {
            conditions.add("COALESCE(status_changed_at, created_at) < :before");
            params.put("before", DateUtils.toEpochSecond(jobFilter.getBefore()));
        }
        if (jobFilter.getAfter() != null) {
            conditions.add("COALESCE(status_changed_at, created_at) > :after");
            params.put("after", DateUtils.toEpochSecond(jobFilter.getAfter()));
        }
        // --- filter by attempts
        if (jobFilter.getBelowMaxAttemptsOrDefaultValue() != null) {
            conditions.add("COALESCE(attempt_count, 0) < COALESCE(attempt_max, :default_attempt_max)");
            params.put("default_attempt_max", jobFilter.getBelowMaxAttemptsOrDefaultValue());
        }
        // --- tags
        if (CollectionUtils.isNotEmpty(jobFilter.getTags())) {
            conditions.add("tags @> :tags::VARCHAR[]");
            params.put("tags", DatabaseUtils.toSqlArray(jobFilter.getTags()));
        }
        // --- controllers
        if (CollectionUtils.isNotEmpty(jobFilter.getControllerNames())) {
            conditions.add("controller_name in (:controller_names)");
            params.put("controller_names", jobFilter.getControllerNames());
        }
        // --- exclude controllers
        if (CollectionUtils.isNotEmpty(jobFilter.getExcludeControllerNames())) {
            conditions.add("controller_name not in (:exclude_controller_names)");
            params.put("exclude_controller_names", jobFilter.getExcludeControllerNames());
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions) + " ";

        String sql = "SELECT * FROM control_plane.jobs " +
                where +
                " OFFSET :skip " +
                " LIMIT :limit";

        return getTemplate().query(sql, params, DbJobConverters.jobRowMapper(objectMapper));
    }

    public static String generateJobStatusFilter(List<JobStatus> statuses) {
        return String.format("status IN (%s)", statuses.stream()
                .map(JobStatus::toString)
                .map(str -> String.format("'%s'::job_status_t", str))
                .collect(Collectors.joining(",")));
    }

    public Optional<DbJob> getJobById(String jobId) {
        String sql = "SELECT * FROM control_plane.jobs " +
                "WHERE id = :id::uuid";
        List<DbJob> output = getTemplate().query(sql, Map.of("id", jobId), DbJobConverters.jobRowMapper(objectMapper));
        return IterableUtils.getFirst(output);
    }

    public Optional<DbJob> getJobMetadataById(String jobId, boolean includeQuery, boolean includeResult) {
        String columns = "id,agent_id,tenant_id,integration_id,reserved,tags,status,level,parent_id,attempt_count,attempt_max,callback_url,controller_name,created_at,status_changed_at";
        if (includeQuery) {
            columns += ",query";
        }
        if (includeResult) {
            columns += ",result,error,failures,intermediate_state";
        }
        String sql = "SELECT " + columns + " FROM control_plane.jobs " +
                "WHERE id = :id::uuid";
        List<DbJob> output = getTemplate().query(sql, Map.of("id", jobId), DbJobConverters.jobRowMapper(objectMapper, includeQuery, includeResult));
        return IterableUtils.getFirst(output);
    }

    public void createJob(@Nonnull String jobId,
                          @Nonnull String controllerName,
                          @Nonnull String query,
                          @Nullable String tenantId,
                          @Nullable String integrationId,
                          @Nullable Boolean reserved,
                          @Nullable String callbackUrl,
                          @Nullable Set<String> tags) {
        Validate.notBlank(jobId, "jobId cannot be null or empty.");
        Validate.notBlank(controllerName, "controllerName cannot be null or empty.");
        Validate.notNull(query, "query cannot be null.");
        String sql = "INSERT INTO control_plane.jobs (id, controller_name, query, tenant_id, integration_id, reserved, callback_url, tags) " +
                "VALUES (:id::UUID, :controller_name, to_jsonb(:query::JSONB), :tenant_id, :integration_id, :reserved, :callback_url, :tags::VARCHAR[])";
        // TODO error handling (e.g. PSQLException 'already exists')
        getTemplate().update(sql, Map.of(
                "id", jobId,
                "controller_name", controllerName,
                "query", query,
                "tenant_id", StringUtils.defaultString(tenantId),
                "integration_id", StringUtils.defaultString(integrationId),
                "reserved", BooleanUtils.toBooleanDefaultIfNull(reserved, false),
                "callback_url", StringUtils.defaultString(callbackUrl),
                "tags", DatabaseUtils.toSqlArray(tags)));
    }

    private void insertJob(DbJob job) {
        String sql = "INSERT INTO control_plane.jobs(id, agent_id, status, level, parent_id, attempt_count, attempt_max, query, tenant_id, integration_id, reserved, callback_url, controller_name, created_at) \n" +
                "VALUES(:id::uuid, :agent_id, :status::job_status_t, :level, :parent_id, :attempt_count, :attempt_max, to_json(:query::json), :tenant_id, :integration_id, :reserved, :callback_url, :controller_name, :created_at)";

        HashMap<String, Object> params = Maps.newHashMap();
        params.put("id", job.getId());
        params.put("agent_id", job.getAgentId());
        params.put("status", job.getStatus());
        params.put("level", job.getLevel());
        params.put("parent_id", job.getParentId());
        params.put("attempt_count", job.getAttemptCount());
        params.put("attempt_max", job.getAttemptMax());
        params.put("query", job.getQuery());
        params.put("tenant_id", job.getTenantId());
        params.put("integration_id", job.getIntegrationId());
        params.put("reserved", job.getReserved());
        params.put("callback_url", job.getCallbackUrl());
        params.put("controller_name", job.getControllerName());
        params.put("created_at", job.getCreatedAt());
        getTemplate().update(sql, params);
    }


    // region updates

    /**
     * Update a job.
     * @return true if the job was changed.
     */
    public boolean updateJob(String jobId, DbJobUpdate jobUpdate) {
        Validate.notBlank(jobId, "jobId cannot be null or empty.");
        Validate.notNull(jobUpdate, "update cannot be null.");

        List<String> updates = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        // -- id
        conditions.add("id = :id::uuid");
        params.put("id", jobId);

        // -- status condition
        if (jobUpdate.getStatusCondition() != null) {
            conditions.add("status = :status_condition::job_status_t");
            params.put("status_condition", jobUpdate.getStatusCondition().toString());
        }

        // -- status
        if (jobUpdate.getStatus() != null) {
            updates.add("status = :status::job_status_t");
            updates.add("status_changed_at = (CASE WHEN status != :status::job_status_t THEN extract(epoch from now()) ELSE status_changed_at END)");
            params.put("status", jobUpdate.getStatus().toString());
        }

        // -- agentId
        if (jobUpdate.getAgentId() != null) {
            updates.add("agent_id = :agent_id::uuid");
            params.put("agent_id", jobUpdate.getAgentId());
        }

        // -- attemptCount
        if (jobUpdate.getAttemptCount() != null) {
            updates.add("attempt_count = :attempt_count::INTEGER");
            params.put("attempt_count", jobUpdate.getAttemptCount());
        } else if (BooleanUtils.isTrue(jobUpdate.getIncrementAttemptCount())) {
            updates.add("attempt_count = coalesce(attempt_count, 0) + 1");
        }

        // -- result
        if (jobUpdate.getResult() != null) {
            updates.add("result = :result::JSON");
            params.put("result", JsonUtils.serializeJson(objectMapper, jobUpdate.getResult(), "", "result"));
        }

        // -- error
        if (jobUpdate.getError() != null) {
            updates.add("error = :error::JSON");
            params.put("error", JsonUtils.serializeJson(objectMapper, jobUpdate.getError(), "", "error"));
        }

        // -- intermediate_state
        if (jobUpdate.getIntermediateState() != null) {
            updates.add("intermediate_state = :intermediate_state::JSON");
            params.put("intermediate_state", JsonUtils.serializeJson(objectMapper, jobUpdate.getIntermediateState(), "", "intermediate_state"));
        }

        // -- ingestion failures
        if (jobUpdate.getIngestionFailures() != null) {
            updates.add("failures = :failures::JSON");
            params.put("failures", JsonUtils.serializeJson(objectMapper, jobUpdate.getIngestionFailures(), "", "failures"));
        }

        if (updates.isEmpty()) {
            return false;
        }

        String sql = "UPDATE control_plane.jobs\n" +
                "SET " + String.join(", ", updates) + "\n" +
                "WHERE " + String.join(" AND ", conditions);
        return getTemplate().update(sql, params) > 0;
    }

    // endregion

    // region --- setup ---

    @Override
    public void ensureTableExistence() {
        createJobStatusType();

        String sql = "CREATE TABLE IF NOT EXISTS control_plane.jobs (" +
                "        id                 UUID PRIMARY KEY," +
                "        agent_id           UUID," +
                "        tenant_id          VARCHAR(255)," +
                "        integration_id     VARCHAR(255)," +
                "        reserved           BOOLEAN NOT NULL DEFAULT false," +
                "        tags               VARCHAR(64)[] NOT NULL DEFAULT '{}'," +
                "        status             job_status_t NOT NULL DEFAULT 'unassigned'," +
                "        level              INTEGER NOT NULL DEFAULT 0," +
                "        parent_id          UUID," +
                "        attempt_count      INTEGER," +
                "        attempt_max        INTEGER," +
                "        query              JSONB," +
                "        callback_url       VARCHAR(255)," +
                "        controller_name    VARCHAR(255)," +
                "        created_at         BIGINT NOT NULL DEFAULT extract(epoch from now())," +
                "        status_changed_at  BIGINT," +
                "        result             JSON," +
                "        intermediate_state JSON," +
                "        error              JSON," + // critical error
                "        failures           JSON" + // warnings
                ")";


        log.debug("sql={}", sql);
        getTemplate().getJdbcTemplate().execute(sql);

        List.of(
                "CREATE INDEX IF NOT EXISTS control_plane_jobs_status_index ON control_plane.jobs(status)",
                "CREATE INDEX IF NOT EXISTS control_plane_jobs_tenant_id_index ON control_plane.jobs(tenant_id)",
                "CREATE INDEX IF NOT EXISTS control_plane_jobs_integration_id_index ON control_plane.jobs(integration_id)",
                "CREATE INDEX IF NOT EXISTS control_plane_jobs_reserved_index ON control_plane.jobs(reserved)",
                "CREATE INDEX IF NOT EXISTS control_plane_jobs_tags_index ON control_plane.jobs using GIN(tags)"
        ).forEach(getTemplate().getJdbcTemplate()::execute);

        log.info("Ensured table existence: control_plane.jobs");
    }

    private void createJobStatusType() {
        try {
//        getTemplate().getJdbcTemplate().execute("DROP TYPE IF EXISTS job_status_t;");
            String sql = String.format("CREATE TYPE job_status_t AS ENUM (%s)",
                    Stream.of(JobStatus.values())
                            .map(JobStatus::toString)
                            .map(String::toLowerCase)
                            .map(s -> StringUtils.wrap(s, "'"))
                            .collect(Collectors.joining(", ")));
            log.debug("sql={}", sql);
            getTemplate().getJdbcTemplate().execute(sql);
            log.info("Created job_status_t enum type");
        } catch (DataAccessException e) {
            if (e.getMessage().contains("ERROR: type \"job_status_t\" already exists")) {
                log.info("job_status_t enum type already exists");
                return;
            }
            log.error("Failed to create job_status_t enum type", e);
        }
    }

    // endregion
}
