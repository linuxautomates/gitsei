package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.RunbookRunConverters;
import io.levelops.commons.databases.models.database.runbooks.RunbookRun;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunState;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class RunbookRunDatabaseService extends DatabaseService<RunbookRun> {

    private static final Integer PAGE_SIZE = 25;
    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public RunbookRunDatabaseService(DataSource ds, ObjectMapper objectMapper) {
        super(ds);
        this.objectMapper = objectMapper;
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(RunbookDatabaseService.class);
    }

    @Override
    public String insert(String company, RunbookRun r) throws SQLException {
        Validate.notBlank(r.getRunbookId(), "r.getId() cannot be null or empty.");
        Validate.notNull(r.getTriggerType(), "r.getTriggerType() cannot be null.");

        String sql = "INSERT INTO " + company + ".runbook_runs " +
                "(runbook_id, trigger_type, args, state)" +
                " VALUES " +
                "(:runbook_id::uuid, :trigger_type, :args::jsonb, :state)";

        Map<String, Object> params = null;
        try {
            params = Map.of(
                    "runbook_id", r.getRunbookId(),
                    "trigger_type", r.getTriggerType(),
                    "args", objectMapper.writeValueAsString(MapUtils.emptyIfNull(r.getArgs())),
                    "state", Objects.toString(r.getState(), RunbookRunState.RUNNING.toString())
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize runbook run", e);
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, new MapSqlParameterSource(params), keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return null;
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    @Override
    public Boolean update(String company, RunbookRun r) throws SQLException {
        Validate.notBlank(r.getId(), "r.getId() cannot be null or empty.");

        List<String> updates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("id", r.getId());


        // -- state & state changed at
        if (r.getState() != null) {
            updates.add("state = :state");
            params.put("state", r.getState().toString());

            updates.add("state_changed_at = :state_changed_at::timestamp");
            params.put("state_changed_at", Timestamp.from(Instant.now()));
        }

        // -- result
        if (r.getResult() != null) {
            try {
                updates.add("result = :result::jsonb");
                params.put("result", objectMapper.writeValueAsString(r.getResult()));
            } catch (JsonProcessingException e) {
                throw new SQLException("Failed to serialize run.result: " + r);
            }
        }

        // -- has_warnings
        if (r.getHasWarnings() != null) {
            updates.add("has_warnings = :has_warnings::boolean");
            params.put("has_warnings", r.getHasWarnings());
        }

        if (updates.isEmpty()) {
            return true;
        }
        String sql = "UPDATE " + company + ".runbook_runs " +
                " SET " + String.join(", ", updates) + " " +
                " WHERE id = :id::uuid ";
        return template.update(sql, params) > 0;
    }

    @Override
    public Optional<RunbookRun> get(String company, String id) throws SQLException {
        String sql = "SELECT run.*, rb.permanent_id FROM " + company + ".runbook_runs as run " +
                " LEFT JOIN " + company + ".runbooks AS rb ON rb.id = run.runbook_id " +
                " WHERE run.id = :id::uuid " +
                " LIMIT 1 ";
        try {
            List<RunbookRun> results = template.query(sql, Map.of("id", id),
                    RunbookRunConverters.rowMapper(objectMapper));
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get run for id={}", id, e);
            return Optional.empty();
        }
    }

    @Override
    public DbListResponse<RunbookRun> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return filter(pageNumber, pageSize, company, null, null, null);
    }

    public Stream<RunbookRun> stream(String company, @Nullable String runbookId, @Nullable RunbookRunState state, @Nullable Boolean hasWarnings) {
        return PaginationUtils.stream(0, 1, page -> filter(page, PAGE_SIZE, company, runbookId, state, hasWarnings).getRecords());
    }

    public DbListResponse<RunbookRun> filter(Integer pageNumber, Integer pageSize, String company, @Nullable String runbookId, @Nullable RunbookRunState state, @Nullable Boolean hasWarnings) {
        return filter(pageNumber, pageSize, company, runbookId, state, hasWarnings, null, null);
    }

    public DbListResponse<RunbookRun> filter(Integer pageNumber, Integer pageSize, String company, @Nullable String runbookId, @Nullable RunbookRunState state, @Nullable Boolean hasWarnings, @Nullable Long updatedAtStart, @Nullable Long updatedAtEnd) {
        int limit = MoreObjects.firstNonNull(pageSize, PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        if (runbookId != null) {
            conditions.add("run.runbook_id = :runbook_id::uuid");
            params.put("runbook_id", runbookId);
        }
        if (state != null) {
            conditions.add("run.state = :state");
            params.put("state", state.toString());
        }
        if (hasWarnings != null) {
            conditions.add("run.has_warnings = :has_warnings::boolean");
            params.put("has_warnings", hasWarnings);
        }
        if (updatedAtStart != null) {
            conditions.add("run.state_changed_at > to_timestamp(::updatedAtStart)");
            params.put("updatedAtStart", updatedAtStart);
        }
        if (updatedAtEnd != null) {
            conditions.add("run.state_changed_at < to_timestamp(::updatedAtEnd)");
            params.put("updatedAtEnd", updatedAtEnd);
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT run.*, rb.permanent_id FROM " + company + ".runbook_runs AS run " +
                " LEFT JOIN " + company + ".runbooks AS rb ON rb.id = run.runbook_id " +
                where +
                " ORDER BY COALESCE(run.state_changed_at, run.created_at) DESC " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<RunbookRun> results = template.query(sql, params, RunbookRunConverters.rowMapper(objectMapper));

        String countSql = "SELECT count(*) FROM " + company + ".runbook_runs AS run " + where;
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String sql = "DELETE " +
                " FROM " + company + ".runbook_runs" +
                " WHERE id = :id::uuid";

        return template.update(sql, Map.of(
                "id", id
        )) > 0;
    }

    public int bulkDelete(String company, List<String> ids) throws SQLException {
        int rowsDeleted = 0;
        if (CollectionUtils.isNotEmpty(ids)) {
            Map<String, Object> params = Map.of("ids", ids.stream().map(UUID::fromString)
                    .collect(Collectors.toList()));
            String SQL = "DELETE FROM " + company + ".runbook_runs WHERE id IN (:ids)";
            rowsDeleted = template.update(SQL, params);
        }
        return rowsDeleted;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".runbook_runs " +
                        "(" +
                        "   id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "   runbook_id         UUID NOT NULL" +
                        "                         REFERENCES " + company + ".runbooks (id)" +
                        "                         ON DELETE CASCADE, " +
                        "   trigger_type       VARCHAR(64) NOT NULL," +
                        "   args               JSONB NOT NULL," +
                        "   has_warnings       BOOLEAN NOT NULL DEFAULT false," +
                        "   result             JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "   state              VARCHAR(32) NOT NULL," +
                        "   state_changed_at   TIMESTAMPTZ," +
                        "   created_at         TIMESTAMPTZ NOT NULL DEFAULT now()" +
                        ")",

                "CREATE INDEX IF NOT EXISTS runbook_runs__runbook_id_idx   on " + company + "." + "runbook_runs (runbook_id)",

                "CREATE INDEX IF NOT EXISTS runbook_runs__state_idx        on " + company + "." + "runbook_runs (state)",

                "CREATE INDEX IF NOT EXISTS runbook_runs__trigger_type_idx on " + company + "." + "runbook_runs (trigger_type)",

                "CREATE INDEX IF NOT EXISTS runbook_runs_state_changed_at_idx on " + company + "." + "runbook_runs (state_changed_at)"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

}
