package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.RunbookRunningNodeConverters;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunningNode;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunningNodeState;
import io.levelops.commons.databases.utils.SqlInsertQuery;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class RunbookRunningNodeDatabaseService extends DatabaseService<RunbookRunningNode> {

    private static final int PAGE_SIZE = 25;
    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public RunbookRunningNodeDatabaseService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        this.objectMapper = mapper;
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(RunbookRunDatabaseService.class);
    }

    @Override
    public String insert(String company, RunbookRunningNode n) throws SQLException {
        Validate.notNull(n, "n cannot be null.");
        Validate.notBlank(n.getRunId(), "n.getRunId() cannot be null or empty.");
        Validate.notBlank(n.getNodeId(), "n.getNodeId() cannot be null or empty.");

        String sql = SqlInsertQuery.builder(objectMapper)
                .schema(company)
                .table("runbook_running_nodes")
                .sqlField("run_id", ":run_id::uuid")
                .sqlField("node_id", ":node_id")
                .sqlField("state", ":state")
                .sqlField("triggered_by", ":triggered_by::jsonb")
                .build()
                .getSql();

        Map<String, ?> params;
        try {
            params = Map.of(
                    "run_id", n.getRunId(),
                    "node_id", n.getNodeId(),
                    "triggered_by", objectMapper.writeValueAsString(MapUtils.emptyIfNull(n.getTriggeredBy())),
                    "state", Objects.toString(n.getState(), RunbookRunningNodeState.WAITING.toString())
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize running node", e);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, new MapSqlParameterSource(params), keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return null;
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    @Override
    public Boolean update(String company, RunbookRunningNode n) throws SQLException {
        Validate.notBlank(n.getId(), "r.getId() cannot be null or empty.");

        List<String> updates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("id", n.getId());

        // -- output
        if (n.getOutput() != null) {
            updates.add("output = :output::jsonb");
            try {
                params.put("output", objectMapper.writeValueAsString(n.getOutput()));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to serialize node output", e);
            }
        }

        // -- data
        if (n.getData() != null) {
            updates.add("data = :data::jsonb");
            try {
                params.put("data", objectMapper.writeValueAsString(n.getData()));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to serialize node output", e);
            }
        }

        // -- state & state changed at
        if (n.getState() != null) {
            updates.add("state = :state");
            params.put("state", n.getState().toString());

            updates.add("state_changed_at = :state_changed_at::timestamp");
            params.put("state_changed_at", Timestamp.from(Instant.now()));
        }

        // -- triggered_by
        if (n.getTriggeredBy() != null) {
            updates.add("triggered_by = :triggered_by::jsonb");
            try {
                params.put("triggered_by", objectMapper.writeValueAsString(n.getTriggeredBy()));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to serialize node triggered_by", e);
            }
        }

        // -- has_warnings
        if (n.getHasWarnings() != null) {
            updates.add("has_warnings = :has_warnings::boolean");
            params.put("has_warnings", n.getHasWarnings());
        }

        // -- result
        if (n.getResult() != null) {
            try {
            updates.add("result = :result::jsonb");
                params.put("result", objectMapper.writeValueAsString(n.getResult()));
            } catch (JsonProcessingException e) {
                throw new SQLException("Failed to serialize running_node.result: " + n, e);
            }
        }

        if (updates.isEmpty()) {
            return true;
        }
        String sql = "UPDATE " + company + ".runbook_running_nodes " +
                " SET " + String.join(", ", updates) + " " +
                " WHERE id = :id::uuid ";
        return template.update(sql, params) > 0;
    }

    @Override
    public Optional<RunbookRunningNode> get(String company, String id) throws SQLException {
        String sql = "SELECT * FROM " + company + ".runbook_running_nodes " +
                " WHERE id = :id::uuid " +
                " LIMIT 1 ";
        try {
            List<RunbookRunningNode> results = template.query(sql, Map.of("id", id),
                    RunbookRunningNodeConverters.rowMapper(objectMapper));
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get running node for id={}", id, e);
            return Optional.empty();
        }
    }

    @Override
    public DbListResponse<RunbookRunningNode> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return filter(pageNumber, pageSize, company, null, null, null, null, null);
    }

    public Stream<RunbookRunningNode> stream(String company,
                                             @Nullable String runId,
                                             @Nullable List<String> nodeIds,
                                             @Nullable String triggeredBy,
                                             @Nullable List<RunbookRunningNodeState> states,
                                             @Nullable Boolean hasWarnings) {
        return PaginationUtils.stream(0, 1, page -> filter(page, PAGE_SIZE, company, runId, nodeIds, triggeredBy, states, hasWarnings).getRecords());
    }

    public DbListResponse<RunbookRunningNode> filter(Integer pageNumber, Integer pageSize, String company,
                                                     @Nullable String runId,
                                                     @Nullable List<String> nodeIds,
                                                     @Nullable String triggeredBy,
                                                     @Nullable List<RunbookRunningNodeState> states,
                                                     @Nullable Boolean hasWarnings) {
        int limit = MoreObjects.firstNonNull(pageSize, PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        if (runId != null) {
            conditions.add("run_id = :run_id::uuid");
            params.put("run_id", runId);
        }
        if (CollectionUtils.isNotEmpty(nodeIds)) {
            conditions.add("node_id IN (:node_ids)");
            params.put("node_ids", nodeIds);
        }
        if (triggeredBy != null) {
            conditions.add("triggered_by = :triggered_by::uuid");
            params.put("triggered_by", triggeredBy);
        }
        if (CollectionUtils.isNotEmpty(states)) {
            conditions.add("state IN (:states)");
            params.put("states", states.stream().map(RunbookRunningNodeState::toString).collect(Collectors.toList()));
        }
        if (hasWarnings != null) {
            conditions.add("has_warnings = :has_warnings::boolean");
            params.put("has_warnings", hasWarnings);
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + ".runbook_running_nodes " +
                where +
                " ORDER BY created_at " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<RunbookRunningNode> results = template.query(sql, params, RunbookRunningNodeConverters.rowMapper(objectMapper));

        String countSql = "SELECT count(*) FROM " + company + ".runbook_running_nodes " + where;
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String sql = "DELETE " +
                " FROM " + company + ".runbook_running_nodes" +
                " WHERE id = :id::uuid";

        return template.update(sql, Map.of(
                "id", id
        )) > 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".runbook_running_nodes " +
                        "(" +
                        "   id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "   run_id             UUID NOT NULL" +
                        "                         REFERENCES " + company + ".runbook_runs (id)" +
                        "                         ON DELETE CASCADE, " +
                        "   node_id            VARCHAR(32) NOT NULL," +
                        "   triggered_by       JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "   output             JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "   data               JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "   has_warnings       BOOLEAN NOT NULL DEFAULT false," +
                        "   result             JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "   state              VARCHAR(32) NOT NULL," +
                        "   state_changed_at   TIMESTAMPTZ," +
                        "   created_at         TIMESTAMPTZ NOT NULL DEFAULT now()" +
                        ")",

                "CREATE INDEX IF NOT EXISTS runbook_running_nodes__run_id_idx       on " + company + ".runbook_running_nodes (run_id)",
                "CREATE INDEX IF NOT EXISTS runbook_running_nodes__node_id_idx      on " + company + ".runbook_running_nodes (node_id)",
                "CREATE INDEX IF NOT EXISTS runbook_running_nodes__state_idx        on " + company + ".runbook_running_nodes (state)",
                "CREATE INDEX IF NOT EXISTS runbook_running_nodes__has_warnings_idx on " + company + ".runbook_running_nodes (has_warnings)"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
