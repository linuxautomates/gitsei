package io.levelops.controlplane.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.controlplane.models.DbIteration;
import io.levelops.controlplane.models.DbTriggeredJob;
import io.levelops.controlplane.models.DbTriggeredJobsConverters;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Log4j2
@Service
public class DefaultTriggeredJobDatabaseService implements TriggeredJobDatabaseService {

    private static final String TABLE = "control_plane.triggered_jobs";
    private static final int PAGE_SIZE = 50;
    private final Function<Map<String, Object>, DbTriggeredJob> dbTriggeredJobParser;
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public DefaultTriggeredJobDatabaseService(@Qualifier("controlPlaneJdbcTemplate") NamedParameterJdbcTemplate template,
                                              ObjectMapper objectMapper) {
        this.template = template;
        dbTriggeredJobParser = DbTriggeredJobsConverters.getDbTriggeredJobParser(objectMapper);
    }

    @Override
    public NamedParameterJdbcTemplate getTemplate() {
        return this.template;
    }

    @Override
    public List<DbIteration> getIterationsByTriggerId(String triggerId, int skip, int limit) {
        Validate.notBlank(triggerId, "triggerId cannot be null or empty.");

        String sql = "SELECT iteration_id, iteration_ts FROM control_plane.triggered_jobs " +
                " WHERE trigger_id = :trigger_id::uuid " +
                " GROUP BY iteration_id, iteration_ts " +
                " ORDER BY iteration_ts DESC " +
                " OFFSET :skip LIMIT :limit;";

        List<Map<String, Object>> output = getTemplate().queryForList(sql, Map.of(
                "trigger_id", triggerId,
                "skip", skip,
                "limit", limit));
        return IterableUtils.parseIterable(output, map -> DbIteration.builder()
                .iterationId(Objects.toString(map.get("iteration_id")))
                .iterationTs((Long) map.get("iteration_ts"))
                .build());
    }

    @Override
    public List<DbTriggeredJob> getTriggeredJobsByIterationId(String iterationId) {
        Validate.notBlank(iterationId, "iterationId cannot be null or empty.");

        String sql = "SELECT * from control_plane.triggered_jobs " +
                " WHERE iteration_id = :iteration_id::uuid " +
                " ORDER BY iteration_ts DESC ";

        List<Map<String, Object>> output = getTemplate().queryForList(sql, Map.of("iteration_id", iterationId));
        return IterableUtils.parseIterable(output, dbTriggeredJobParser);
    }

    @Override
    public Optional<DbTriggeredJob> getTriggeredJobByJobId(String jobId) {
        Validate.notBlank(jobId, "jobId cannot be null or empty.");

        String sql = "SELECT * from control_plane.triggered_jobs " +
                " WHERE job_id = :job_id::uuid " +
                " LIMIT 1 ";

        List<Map<String, Object>> output = getTemplate().queryForList(sql, Map.of("job_id", jobId));
        if (CollectionUtils.isEmpty(output)) {
            return Optional.empty();
        }
        return Optional.ofNullable(dbTriggeredJobParser.apply(output.get(0)));
    }

    @Override
    public Optional<DbTriggeredJob> getTriggeredJob(@Nonnull String triggerId,
                                                    @Nullable TriggeredJobFilter filter) {
        return IterableUtils.getFirst(filterTriggeredJobs(0, 1, triggerId, filter, null).getRecords());
    }


    @Override
    public Stream<DbTriggeredJob> streamTriggeredJobs(@Nonnull String triggerId,
                                                      @Nullable TriggeredJobFilter filter,
                                                      @Nullable Integer lastNIterations) {
        return IntStream.iterate(0, i -> i + PAGE_SIZE)
                .mapToObj(i -> filterTriggeredJobs(i, PAGE_SIZE, triggerId, filter, lastNIterations).getRecords())
                .takeWhile(CollectionUtils::isNotEmpty)
                .flatMap(Collection::stream);
    }

    @Override
    public DbListResponse<DbTriggeredJob> filterTriggeredJobs(int skip, int limit,
                                                              @Nonnull String triggerId,
                                                              @Nullable TriggeredJobFilter filter,
                                                              @Nullable Integer lastNIterations) {
        Validate.notBlank(triggerId, "triggerId cannot be null or empty.");
        filter = (filter != null) ? filter : TriggeredJobFilter.builder().build();

        boolean joinWithJobsTable = false;
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        // --- trigger id
        conditions.add("trigger_id = :trigger_id::uuid");
        params.put("trigger_id", triggerId);

        // --- status
        if (CollectionUtils.isNotEmpty(filter.getStatuses())) {
            conditions.add(JobDatabaseService.generateJobStatusFilter(filter.getStatuses()));
            joinWithJobsTable = true;
        }

        // --- filter by attempts
        if (filter.getBelowMaxAttemptsOrDefaultValue() != null) {
            conditions.add("COALESCE(attempt_count, 0) < COALESCE(attempt_max, :default_attempt_max)");
            params.put("default_attempt_max", filter.getBelowMaxAttemptsOrDefaultValue());
            joinWithJobsTable = true;
        }

        // --- partial
        if (filter.getPartial() != null) {
            conditions.add("partial = :partial");
            params.put("partial", filter.getPartial());
        }

        // --- after
        if (filter.getAfterExclusive() != null) {
            conditions.add("iteration_ts > :since");
            params.put("since", filter.getAfterExclusive());
        }

        if (filter.getAfterInclusive() != null) {
            conditions.add("iteration_ts >= :since");
            params.put("since", filter.getAfterInclusive());
        }

        // --- before
        if (filter.getBeforeInclusive() != null) {
            conditions.add("iteration_ts <= :before");
            params.put("before", filter.getBeforeInclusive());
        }

        List<String> tables = new ArrayList<>();
        tables.add("control_plane.triggered_jobs as triggered");

        // join
        String jobsTableJoin = "";
        if (joinWithJobsTable) {
            jobsTableJoin = " JOIN control_plane.jobs as job ON job.id = triggered.job_id ";
        }

        if (lastNIterations != null) {
            List<Long> iterationTsList = getLastNIterationTs(triggerId, lastNIterations, filter);
            if (CollectionUtils.isNotEmpty(iterationTsList)) {
                conditions.add("iteration_ts IN (:iteration_ts_list)");
                params.put("iteration_ts_list", iterationTsList);
            }
        }
        String sql = "" +
                " SELECT triggered.* " +
                " FROM control_plane.triggered_jobs as triggered" +
                jobsTableJoin +
                " WHERE " + String.join(" AND ", conditions) +
                " ORDER BY triggered.iteration_ts DESC " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<Map<String, Object>> output = getTemplate().queryForList(sql, params);

        Integer totalCount = null;
        if (BooleanUtils.isTrue(filter.getReturnTotalCount())) {
            String countSql = "SELECT count(*) " +
                    " FROM " + String.join(", ", tables) +
                    " WHERE " + String.join(" AND ", conditions);
            totalCount = getTemplate().queryForObject(countSql, params, Integer.class);
        }

        return DbListResponse.of(IterableUtils.parseIterable(output, dbTriggeredJobParser), totalCount);
    }

    private List<Long> getLastNIterationTs(@Nonnull String triggerId,
                                           int lastNIterations,
                                           @Nonnull TriggeredJobFilter filter) {
        if (lastNIterations <= 0) {
            return Collections.emptyList();
        }
        boolean joinWithJobsTable = false;
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();

        conditions.add("trigger_id = :trigger_id::uuid");
        params.put("trigger_id", triggerId);

        if (CollectionUtils.isNotEmpty(filter.getStatuses())) {
            conditions.add(JobDatabaseService.generateJobStatusFilter(filter.getStatuses()));
            joinWithJobsTable = true;
        }

        if (filter.getPartial() != null) {
            conditions.add("partial = :partial");
            params.put("partial", filter.getPartial());
        }

        if (filter.getAfterExclusive() != null) {
            conditions.add("iteration_ts > :since");
            params.put("since", filter.getAfterExclusive());
        }

        if (filter.getBeforeInclusive() != null) {
            conditions.add("iteration_ts <= :before");
            params.put("before", filter.getBeforeInclusive());
        }

        List<String> tables = new ArrayList<>();
        tables.add("control_plane.triggered_jobs as triggered");

        if (joinWithJobsTable) {
            tables.add("control_plane.jobs as job");
            conditions.add("job.id = triggered.job_id");
        }

        params.put("limit", lastNIterations);
        String sql = "SELECT triggered.iteration_ts AS iteration_ts " +
                " FROM " + String.join(", ", tables) +
                " WHERE " + String.join(" AND ", conditions) +
                " ORDER BY iteration_ts DESC " +
                " LIMIT :limit ";
        return getTemplate().queryForList(sql, params, Long.class);
    }

    public boolean createTriggeredJob(String jobId, String triggerId, String iterationId, Long iterationTs, boolean partial) {
        Validate.notBlank(jobId, "jobId cannot be null or empty.");
        Validate.notBlank(triggerId, "triggerId cannot be null or empty.");
        Validate.notBlank(iterationId, "iterationId cannot be null or empty.");
        Validate.notNull(iterationTs, "iterationTs cannot be null.");

        String sql = "INSERT INTO control_plane.triggered_jobs(job_id, trigger_id, iteration_id, iteration_ts, partial) \n" +
                "VALUES(:job_id::uuid, :trigger_id::uuid, :iteration_id::uuid, :iteration_ts, :partial)";

        return getTemplate().update(sql, Map.of(
                "job_id", jobId,
                "trigger_id", triggerId,
                "iteration_id", iterationId,
                "iteration_ts", iterationTs,
                "partial", partial)) > 0;
    }

    public int deleteTriggeredJobs(String triggerId) {
        Validate.notBlank(triggerId, "triggerId cannot be null or empty.");

        String sql = "DELETE FROM control_plane.triggered_jobs " +
                "WHERE trigger_id = :trigger_id::uuid";
        return getTemplate().update(sql, Map.of("trigger_id", triggerId));
    }


    // region --- setup ---

    @Override
    public void ensureTableExistence() {

        String sql = "CREATE TABLE IF NOT EXISTS control_plane.triggered_jobs (" +
                "        job_id          UUID PRIMARY KEY," +
                "        trigger_id      UUID NOT NULL," +
                "        iteration_id    UUID NOT NULL," +
                "        iteration_ts    BIGINT NOT NULL," +
                "        partial         BOOLEAN NOT NULL DEFAULT false," +
                "        created_at      BIGINT NOT NULL DEFAULT extract(epoch from now())" +
                ")";

        log.debug("sql={}", sql);
        getTemplate().getJdbcTemplate().execute(sql);

        String triggerIdIndexSql = "CREATE INDEX IF NOT EXISTS control_plane_triggered_jobs_trigger_id_index ON control_plane.triggered_jobs(trigger_id)";
        log.debug("sql={}", triggerIdIndexSql);
        getTemplate().getJdbcTemplate().execute(triggerIdIndexSql);

        String iterationIdIndexSql = "CREATE INDEX IF NOT EXISTS control_plane_triggered_jobs_iteration_id_index ON control_plane.triggered_jobs(iteration_id)";
        log.debug("sql={}", iterationIdIndexSql);
        getTemplate().getJdbcTemplate().execute(iterationIdIndexSql);

        String iterationTsIndexSql = "CREATE INDEX IF NOT EXISTS control_plane_triggered_jobs_iteration_ts_index ON control_plane.triggered_jobs(iteration_ts)";
        log.debug("sql={}", iterationTsIndexSql);
        getTemplate().getJdbcTemplate().execute(iterationTsIndexSql);

        String partialIndexSql = "CREATE INDEX IF NOT EXISTS control_plane_triggered_jobs_partial_index ON control_plane.triggered_jobs(partial)";
        log.debug("sql={}", partialIndexSql);
        getTemplate().getJdbcTemplate().execute(partialIndexSql);

        log.info("Ensured table existence: control_plane.triggered_jobs");
    }

    // endregion
}
