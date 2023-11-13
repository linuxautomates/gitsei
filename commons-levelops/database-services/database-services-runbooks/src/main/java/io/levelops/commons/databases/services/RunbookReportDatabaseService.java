package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.RunbookReportConverters;
import io.levelops.commons.databases.models.database.runbooks.RunbookReport;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.sql.DataSource;

@Log4j2
@Service
public class RunbookReportDatabaseService extends DatabaseService<RunbookReport> {

    private static final int PAGE_SIZE = 25;
    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public RunbookReportDatabaseService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        this.objectMapper = mapper;
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public String insert(String company, RunbookReport r) throws SQLException {
        Validate.notNull(r, "r cannot be null.");
        Validate.notBlank(r.getRunbookId(), "r.getRunbookId() cannot be null or empty.");
        Validate.notBlank(r.getRunId(), "r.getRunId() cannot be null or empty.");
        Validate.notBlank(r.getGcsPath(), "r.getGcsPath() cannot be null or empty.");
        Validate.notBlank(r.getSource(), "r.getSource() cannot be null or empty.");

        String sql = "INSERT INTO " + company + ".runbook_reports " +
                "(runbook_id, run_id, source, title, gcs_path)" +
                " VALUES " +
                "(:runbook_id::uuid, :run_id::uuid, :source::text, :title::text, :gcs_path::text)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("runbook_id", r.getRunbookId());
        params.addValue("run_id", r.getRunId());
        params.addValue("source", r.getSource());
        params.addValue("title", StringUtils.truncate(StringUtils.defaultString(r.getTitle()), 256));
        params.addValue("gcs_path", r.getGcsPath());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            int updatedRows = template.update(sql, params, keyHolder);
            if (updatedRows <= 0 || keyHolder.getKeys() == null) {
                return null;
            }
        } catch (DuplicateKeyException e) {
            return null;
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    @Override
    public Boolean update(String company, RunbookReport r) throws SQLException {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Optional<RunbookReport> get(String company, String id) throws SQLException {
        String sql = "SELECT * FROM " + company + ".runbook_reports " +
                " WHERE id = :id::uuid " +
                " LIMIT 1 ";
        try {
            List<RunbookReport> results = template.query(sql, Map.of("id", id),
                    RunbookReportConverters.rowMapper(objectMapper));
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get runbook report for id={}", id, e);
            return Optional.empty();
        }
    }

    @Override
    public DbListResponse<RunbookReport> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return filter(pageNumber, pageSize, company, null, null, null, null);
    }

    public Stream<RunbookReport> stream(String company, @Nullable List<String> runbookIds, @Nullable String runId, @Nullable String source, @Nullable String partialTitle) {
        return PaginationUtils.stream(0, 1, page -> filter(page, PAGE_SIZE, company, runbookIds, runId, source, partialTitle).getRecords());
    }

    public DbListResponse<RunbookReport> filter(Integer pageNumber, Integer pageSize, String company, @Nullable List<String> runbookIds, @Nullable String runId, @Nullable String source, @Nullable String partialTitle) {
        int limit = MoreObjects.firstNonNull(pageSize, PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        if (CollectionUtils.isNotEmpty(runbookIds)) {
            conditions.add("runbook_id::text IN (:runbook_ids)");
            params.put("runbook_ids", runbookIds);
        }
        if (runId != null) {
            conditions.add("run_id = :run_id::uuid");
            params.put("run_id", runId);
        }
        if (source != null) {
            conditions.add("source = :source::text");
            params.put("source", source);
        }
        if (Strings.isNotEmpty(partialTitle)) {
            conditions.add("title LIKE :title");
            params.put("title", "%" + partialTitle + "%");
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + ".runbook_reports " +
                where +
                " ORDER BY created_at DESC " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<RunbookReport> results = template.query(sql, params, RunbookReportConverters.rowMapper(objectMapper));

        String countSql = "SELECT count(*) FROM " + company + ".runbook_reports " + where;
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String sql = "DELETE " +
                " FROM " + company + ".runbook_reports " +
                " WHERE id = :id::uuid";

        return template.update(sql, Map.of(
                "id", id
        )) > 0;
    }

    public int deleteBulkReports(String company, List<String> ids) throws SQLException {
        int affectedRows = 0;
        if (CollectionUtils.isNotEmpty(ids)) {
            Map<String, Object> params = Map.of("ids", ids.stream().map(UUID::fromString)
                    .collect(Collectors.toList()));
            String sql = "DELETE FROM " + company + ".runbook_reports WHERE id IN (:ids)";
            affectedRows = template.update(sql, params);
        }
        return affectedRows;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".runbook_reports " +
                        "(" +
                        "  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "  runbook_id  UUID NOT NULL," +
                        "  run_id      UUID NOT NULL," +
                        "  source      VARCHAR(64) NOT NULL," +
                        "  title       VARCHAR(256) NOT NULL DEFAULT ''," +
                        "  gcs_path    TEXT NOT NULL," +
                        "  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()," +
                        "  UNIQUE (runbook_id, run_id, source)" +
                        ")",

                "CREATE INDEX IF NOT EXISTS runbook_reports__runbook_id_idx on " + company + ".runbook_reports (runbook_id)",
                "CREATE INDEX IF NOT EXISTS runbook_reports__run_id_idx     on " + company + ".runbook_reports (run_id)",
                "CREATE INDEX IF NOT EXISTS runbook_reports__source_idx     on " + company + ".runbook_reports (source)"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
