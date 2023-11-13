package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.RunbookReportSectionConverters;
import io.levelops.commons.databases.models.database.runbooks.RunbookReportSection;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Log4j2
@Service
public class RunbookReportSectionDatabaseService extends DatabaseService<RunbookReportSection> {

    private static final int PAGE_SIZE = 25;
    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public RunbookReportSectionDatabaseService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        this.objectMapper = mapper;
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(RunbookReportDatabaseService.class);
    }

    @Override
    public String insert(String company, RunbookReportSection r) throws SQLException {
        String sql = "INSERT INTO " + company + ".runbook_report_sections " +
                "(source, report_id, gcs_path, page_count, page_size, total_count, title, metadata)" +
                " VALUES " +
                "(:source::text, :report_id::uuid, :gcs_path::text, :page_count::integer, :page_size::integer, :total_count::integer, :title::text, :metadata::jsonb)";

        Map<String, Object> params = null;
        try {
            params = Map.of(
                    "source", r.getSource(),
                    "report_id", r.getReportId(),
                    "gcs_path", r.getGcsPath(),
                    "page_count", r.getPageCount(),
                    "page_size", r.getPageSize(),
                    "total_count", r.getTotalCount(),
                    "title", StringUtils.defaultString(StringUtils.truncate(r.getTitle(), 256)),
                    "metadata", r.getMetadata() != null ? objectMapper.writeValueAsString(r.getMetadata()) : "{}");
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize report section", e);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            int updatedRows = template.update(sql, new MapSqlParameterSource(params), keyHolder);
            if (updatedRows <= 0 || keyHolder.getKeys() == null) {
                return null;
            }
        } catch (DuplicateKeyException e) {
            return null;
        }
        return String.valueOf(keyHolder.getKeys().get("id"));
    }

    @Override
    public Boolean update(String company, RunbookReportSection r) throws SQLException {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Optional<RunbookReportSection> get(String company, String id) throws SQLException {
        String sql = "SELECT * FROM " + company + ".runbook_report_sections " +
                " WHERE id = :id::uuid " +
                " LIMIT 1 ";
        try {
            List<RunbookReportSection> results = template.query(sql, Map.of("id", id),
                    RunbookReportSectionConverters.rowMapper(objectMapper));
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get runbook report section for id={}", id, e);
            return Optional.empty();
        }
    }

    @Override
    public DbListResponse<RunbookReportSection> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return filter(pageNumber, pageSize, company, null, null);
    }

    public Stream<RunbookReportSection> stream(String company, @Nullable String source, @Nullable String reportId) {
        return PaginationUtils.stream(0, 1, page -> filter(page, PAGE_SIZE, company, source, reportId).getRecords());
    }

    public DbListResponse<RunbookReportSection> filter(Integer pageNumber, Integer pageSize, String company, @Nullable String source, @Nullable String reportId) {
        int limit = MoreObjects.firstNonNull(pageSize, PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        if (source != null) {
            conditions.add("source = :source::text");
            params.put("source", source);
        }
        if (reportId != null) {
            conditions.add("report_id = :report_id::uuid");
            params.put("report_id", reportId);
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + ".runbook_report_sections " +
                where +
                " ORDER BY created_at DESC " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<RunbookReportSection> results = template.query(sql, params, RunbookReportSectionConverters.rowMapper(objectMapper));

        String countSql = "SELECT count(*) FROM " + company + ".runbook_report_sections " + where;
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String sql = "DELETE " +
                " FROM " + company + ".runbook_report_sections " +
                " WHERE id = :id::uuid";

        return template.update(sql, Map.of(
                "id", id
        )) > 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".runbook_report_sections " +
                        "(" +
                        "  id          UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "  source      VARCHAR(64) NOT NULL," +
                        "  report_id   UUID NOT NULL" +
                        "                  REFERENCES " + company + ".runbook_reports(id)" +
                        "                  ON DELETE CASCADE," +
                        "  gcs_path    TEXT NOT NULL," +
                        "  page_count  INTEGER NOT NULL," +
                        "  page_size   INTEGER NOT NULL," +
                        "  total_count INTEGER NOT NULL," +
                        "  title       VARCHAR(256) NOT NULL," +
                        "  metadata    JSONB NOT NULL DEFAULT '{}'::JSONB," +
                        "  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()," +
                        "  UNIQUE (source, report_id) " +
                        ")",

                "CREATE INDEX IF NOT EXISTS runbook_report_sections__source_idx    on " + company + ".runbook_report_sections (source)",
                "CREATE INDEX IF NOT EXISTS runbook_report_sections__report_id_idx on " + company + ".runbook_report_sections (report_id)"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
