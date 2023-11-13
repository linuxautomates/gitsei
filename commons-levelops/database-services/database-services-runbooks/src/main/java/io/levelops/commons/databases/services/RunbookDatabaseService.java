package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.RunbookConverters;
import io.levelops.commons.databases.models.database.TriggerType;
import io.levelops.commons.databases.models.database.runbooks.Runbook;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.util.Strings;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Log4j2
@Service
public class RunbookDatabaseService extends DatabaseService<Runbook> {

    private static final int PAGE_SIZE = 25;
    private final ObjectMapper objectMapper;
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public RunbookDatabaseService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        this.objectMapper = mapper;
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public String insert(String company, Runbook r) throws SQLException {
        return insertAndReturnIds(company, r)
                .map(InsertResult::getId)
                .orElse(null);
    }

    @Value
    @Builder(toBuilder = true)
    public static class InsertResult {
        String id;
        String permanentId;
    }

    public Optional<InsertResult> insertAndReturnIds(String company, Runbook r) throws SQLException {
        Validate.notBlank(r.getName(), "r.getName() cannot be null or empty.");
        Validate.notNull(r.getTriggerType(), "r.getTriggerType() cannot be null.");

        String sql = "INSERT INTO " + company + ".runbooks " +
                "(name, description, enabled, previous_id, permanent_id, trigger_type, trigger_template_type, trigger_data, input, nodes, ui_data, settings)" +
                " VALUES " +
                "(:name, :description, :enabled, :previous_id::uuid, :permanent_id::uuid, :trigger_type, :trigger_template_type, :trigger_data::jsonb, :input::jsonb, :nodes::jsonb, :ui_data::jsonb, :settings::jsonb)";

        MapSqlParameterSource params = new MapSqlParameterSource();
        try {
            params.addValue("name", r.getName());
            params.addValue("description", StringUtils.defaultString(r.getDescription()));
            params.addValue("enabled", BooleanUtils.toBooleanDefaultIfNull(r.getEnabled(), true));
            params.addValue("previous_id", r.getPreviousId()); // nullable
            params.addValue("permanent_id", StringUtils.isNotBlank(r.getPermanentId()) ? r.getPermanentId() : UUID.randomUUID());
            params.addValue("trigger_type", r.getTriggerType().toString());
            params.addValue("trigger_template_type", StringUtils.defaultString(r.getTriggerTemplateType()));
            params.addValue("trigger_data", objectMapper.writeValueAsString(MapUtils.emptyIfNull(r.getTriggerData())));
            params.addValue("input", objectMapper.writeValueAsString(MapUtils.emptyIfNull(r.getInput())));
            params.addValue("nodes", objectMapper.writeValueAsString(MapUtils.emptyIfNull(r.getNodes())));
            params.addValue("ui_data", objectMapper.writeValueAsString(MapUtils.emptyIfNull(r.getUiData())));
            params.addValue("settings", objectMapper.writeValueAsString(r.getSettings()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize runbook nodes", e);
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return Optional.empty();
        }
        UUID id = (UUID) keyHolder.getKeys().get("id");
        UUID permanentId = (UUID) keyHolder.getKeys().get("permanent_id");
        return Optional.of(InsertResult.builder()
                .id(id != null? id.toString() : null)
                .permanentId(permanentId != null? permanentId.toString() : null)
                .build());
    }

    @Override
    public Boolean update(String company, Runbook r) throws SQLException {
        Validate.notBlank(r.getId(), "r.getId() cannot be null or empty.");

        List<String> updates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("id", r.getId());

        try {
            // -- name
            if (r.getName() != null) {
                updates.add("name = :name");
                params.put("name", r.getName());
            }

            // -- description
            if (r.getDescription() != null) {
                updates.add("description = :description");
                params.put("description", r.getDescription());
            }

            // -- enabled
            if (r.getEnabled() != null) {
                updates.add("enabled = :enabled");
                params.put("enabled", r.getEnabled());
            }

            // -- trigger_type
            if (r.getTriggerType() != null) {
                updates.add("trigger_type = :trigger_type");
                params.put("trigger_type", r.getTriggerType().toString());
            }

            // -- trigger_type
            if (r.getTriggerTemplateType() != null) {
                updates.add("trigger_template_type = :trigger_template_type");
                params.put("trigger_template_type", r.getTriggerTemplateType());
            }

            // -- trigger_data
            if (r.getTriggerData() != null) {
                updates.add("trigger_data = :trigger_data::jsonb");
                params.put("trigger_data", objectMapper.writeValueAsString(r.getTriggerData()));
            }

            // -- ui_data
            if (r.getUiData() != null) {
                updates.add("ui_data = :ui_data::jsonb");
                params.put("ui_data", objectMapper.writeValueAsString(r.getUiData()));
            }

            // -- settings
            if (r.getSettings() != null) {
                updates.add("settings = :settings::jsonb");
                params.put("settings", objectMapper.writeValueAsString(r.getSettings()));
            }

            // -- last_run_at
            if (r.getLastRunAt() != null) {
                updates.add("last_run_at = :last_run_at::timestamp");
                params.put("last_run_at", Timestamp.from(r.getLastRunAt()));
            }
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize data", e);
        }

        if (updates.isEmpty()) {
            return true;
        }

        updates.add("updated_at = now()");
        String sql = "UPDATE " + company + ".runbooks " +
                " SET " + String.join(", ", updates) + " " +
                " WHERE id = :id::uuid ";
        return template.update(sql, params) > 0;
    }

    @Override
    public Optional<Runbook> get(String company, String id) throws SQLException {
        String sql = "SELECT * FROM " + company + ".runbooks " +
                " WHERE id = :id::uuid " +
                " LIMIT 1 ";
        try {
            List<Runbook> results = template.query(sql, Map.of("id", id),
                    RunbookConverters.rowMapper(objectMapper));
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get runbook for id={}", id, e);
            return Optional.empty();
        }
    }

    public Optional<Runbook> getLatestByPermanentId(String company, String permanentId) throws SQLException {
        String sql = "SELECT * FROM " + company + ".runbooks as r1" +
                " WHERE permanent_id = :permanent_id::uuid " +
                " AND (SELECT count(*) FROM " + company +  ".runbooks as r2 WHERE r2.previous_id = r1.id) = 0" +
                " LIMIT 1 ";
        try {
            List<Runbook> results = template.query(sql, Map.of("permanent_id", permanentId),
                    RunbookConverters.rowMapper(objectMapper));
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get latest runbook for permanentId={}", permanentId, e);
            return Optional.empty();
        }
    }


    @Override
    public DbListResponse<Runbook> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return filter(pageNumber, pageSize, company, null, null, null, null, null, null, null, null);
    }

    public Stream<Runbook> stream(String company, @Nullable String name, @Nullable Boolean enabled, @Nullable TriggerType triggerType, @Nullable String triggerTemplateType, @Nullable List<String> runbookIds, @Nullable Boolean onlyLatestRevision, @Nullable String previousId, @Nullable List<String> permanentIds) {
        return PaginationUtils.stream(0, 1, page -> filter(page, PAGE_SIZE, company, name, enabled, triggerType, triggerTemplateType, runbookIds, onlyLatestRevision, previousId, permanentIds).getRecords());
    }

    public DbListResponse<Runbook> filter(Integer pageNumber, Integer pageSize, String company, @Nullable String name, @Nullable Boolean enabled, @Nullable TriggerType triggerType, @Nullable String triggerTemplateType, @Nullable List<String> runbookIds, @Nullable Boolean onlyLatestRevision, @Nullable String previousId, @Nullable List<String> permanentIds) {
        int limit = MoreObjects.firstNonNull(pageSize, PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        if (name != null) {
            conditions.add("name ILIKE :name");
            params.put("name", "%" + name + "%");
        }
        if (enabled != null) {
            conditions.add("enabled = :enabled");
            params.put("enabled", enabled);
        }
        if (triggerType != null) {
            conditions.add("trigger_type = :trigger_type");
            params.put("trigger_type", triggerType.toString());
        }
        if (Strings.isNotEmpty(triggerTemplateType)) {
            conditions.add("trigger_template_type = :trigger_template_type");
            params.put("trigger_template_type", triggerTemplateType);
        }
        if (CollectionUtils.isNotEmpty(runbookIds)) {
            conditions.add("id::text IN (:runbook_ids)");
            params.put("runbook_ids", runbookIds);
        }
        if (BooleanUtils.isTrue(onlyLatestRevision)) {
            conditions.add("(SELECT count(*) FROM " + company +  ".runbooks as r2 WHERE r2.previous_id = r1.id) = 0");
        }
        if (Strings.isNotEmpty(previousId)) {
            conditions.add("previous_id = :previous_id::uuid");
            params.put("previous_id", previousId);
        }
        if (CollectionUtils.isNotEmpty(permanentIds)) {
            conditions.add("permanent_id::text IN (:permanent_ids)");
            params.put("permanent_ids", permanentIds);
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + ".runbooks AS r1 " +
                where +
                " ORDER BY updated_at DESC " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<Runbook> results = template.query(sql, params, RunbookConverters.rowMapper(objectMapper));

        String countSql = "SELECT count(*) FROM " + company + ".runbooks as r1 " + where;
        Integer count = template.queryForObject(countSql, params, Integer.class);

        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String sql = "DELETE " +
                " FROM " + company + ".runbooks " +
                " WHERE id = :id::uuid";

        return template.update(sql, Map.of(
                "id", id
        )) > 0;
    }

    public List<String> listNewerRevisions(String company, String id) {
        String sql = MessageFormat.format("WITH RECURSIVE revisions AS ( " +
                "	SELECT " +
                "		id, " +
                "		name, " +
                "		previous_id " +
                "	FROM " +
                "		{0}.runbooks " +
                "	WHERE " +
                "		id = :id::uuid  " +
                "	UNION " +
                "		SELECT " +
                "			next.id, " +
                "			next.name, " +
                "			next.previous_id " +
                "		FROM " +
                "			{0}.runbooks next " +
                "		INNER JOIN revisions curr ON next.previous_id = curr.id " +
                ") SELECT " +
                "	id " +
                "FROM " +
                "	revisions;", company);
        return template.queryForList(sql, Map.of("id", id), String.class);
    }

    public String getLatestRevision(String company, String id) {
        String sql = MessageFormat.format("WITH RECURSIVE revisions (id, name, previous_id, level) AS ( " +
                "	SELECT " +
                "		id, " +
                "		name, " +
                "		previous_id," +
                "       0::int " +
                "	FROM " +
                "		{0}.runbooks " +
                "	WHERE " +
                "		id = :id::uuid  " +
                "	UNION " +
                "		SELECT " +
                "			next.id, " +
                "			next.name, " +
                "			next.previous_id," +
                "           level + 1 " +
                "		FROM " +
                "			{0}.runbooks next " +
                "		INNER JOIN revisions curr ON next.previous_id = curr.id " +
                ")" +
                " SELECT id FROM revisions " +
                " ORDER BY level DESC" +
                " LIMIT 1;", company);
        return template.queryForObject(sql, Map.of("id", id), String.class);
    }

    public List<String> listPreviousRevisions(String company, String id) {
        String sql = MessageFormat.format("WITH RECURSIVE revisions AS ( " +
                    "	SELECT " +
                    "		id, " +
                    "		name, " +
                    "		previous_id " +
                    "	FROM " +
                    "		{0}.runbooks " +
                    "	WHERE " +
                    "		id = :id::uuid  " +
                    "	UNION " +
                    "		SELECT " +
                    "			prev.id, " +
                    "			prev.name, " +
                    "			prev.previous_id " +
                    "		FROM " +
                    "			{0}.runbooks prev " +
                    "		INNER JOIN revisions curr ON curr.previous_id = prev.id " +
                    ") SELECT " +
                    "	id " +
                    "FROM " +
                    "	revisions;", company);
        return template.queryForList(sql, Map.of("id", id), String.class);
    }

    public boolean deletePreviousRevisions(String company, String upToId, boolean inclusive) throws SQLException {
        List<String> revisionIds = listPreviousRevisions(company, upToId);
        if (!inclusive) {
            revisionIds.remove(upToId);
        }

        if (revisionIds.isEmpty()) {
            return false;
        }

        String sql = "DELETE " +
                " FROM " + company + ".runbooks " +
                " WHERE id::text IN (:ids)";

        return template.update(sql, Map.of(
                "ids", revisionIds
        )) > 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".runbooks " +
                        "(" +
                        "  id                 UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        "  name               TEXT NOT NULL," +
                        "  description        TEXT NOT NULL," +
                        "  enabled            BOOLEAN NOT NULL DEFAULT TRUE," +
                        "  previous_id        UUID," +
                        "  permanent_id       UUID NOT NULL DEFAULT uuid_generate_v4()," +
                        "  trigger_type       VARCHAR(64) NOT NULL," +
                        "  trigger_template_type VARCHAR(64) NOT NULL," +
                        "  trigger_data       JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "  last_run_at        TIMESTAMPTZ," +
                        "  input              JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "  nodes              JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "  ui_data            JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "  settings           JSONB NOT NULL DEFAULT '{}'::jsonb," +
                        "  updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()," +
                        "  created_at         TIMESTAMPTZ NOT NULL DEFAULT now()" +
                        ")",

                "CREATE INDEX IF NOT EXISTS runbooks__enabled_idx      on " + company + ".runbooks (enabled)",
                "CREATE INDEX IF NOT EXISTS runbooks__trigger_type_idx on " + company + ".runbooks (trigger_type)",
                "CREATE INDEX IF NOT EXISTS runbooks__trigger_template_type_idx on " + company + ".runbooks (trigger_template_type)",
                "CREATE INDEX IF NOT EXISTS runbooks__previous_id_idx on " + company + ".runbooks (previous_id)",
                "CREATE INDEX IF NOT EXISTS runbooks__permanent_id_idx on " + company + ".runbooks (permanent_id)"
        );
        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

    public boolean isCountMaxedOut(String company, Map<String, String> entitlementsConfig){

        long maxCount = Long.valueOf(entitlementsConfig.get("PROPELS_COUNT"));
        String sql = "SELECT count(*) FROM "+company+".runbooks";
        long count = template.queryForObject(sql, Map.of(), Integer.class);

        return count >= maxCount;
    }
}
