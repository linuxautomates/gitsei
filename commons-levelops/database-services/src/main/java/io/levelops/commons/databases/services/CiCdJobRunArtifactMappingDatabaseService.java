package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.cicd.CiCdJobRunArtifactMappingConverters;
import io.levelops.commons.databases.models.database.cicd.CiCdJobRunArtifactMapping;
import io.levelops.commons.databases.services.CicdJobRunArtifactCorrelationService.IntermediateMapping;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ListUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.dao.DataAccessException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.databases.services.CiCdJobRunArtifactMappingDatabaseService.*;

/**
 * This table is to store correlated mappings between job runs that share the same artifact(s).
 * The relation is unidirectional (1 -> 2), so we need 2 rows to represent both directions.
 * For example, if two job runs, JR1 and JR2, are found to be linked by the same artifact A,
 * then there will be 2 entries:
 * - JR1 -> JR2
 * - JR2 -> JR1
 */
@Log4j2
@Service
public class CiCdJobRunArtifactMappingDatabaseService extends FilteredDatabaseService<CiCdJobRunArtifactMapping, CicdJobRunArtifactMappingFilter> {

    public static final String TABLE_NAME = "cicd_job_run_artifact_mappings";
    private static final Integer PAGE_SIZE = 500;
    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper objectMapper;

    public CiCdJobRunArtifactMappingDatabaseService(DataSource dataSource, ObjectMapper objectMapper) {
        super(dataSource, PAGE_SIZE);
        template = new NamedParameterJdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(CiCdJobRunsDatabaseService.class);
    }

    @Override
    public String insert(String company, CiCdJobRunArtifactMapping m) throws SQLException {
        Validate.notNull(m, "m cannot be null.");
        Validate.notNull(m.getCicdJobRunId1(), "m.getCicdJobRunId1() cannot be null.");
        Validate.notNull(m.getCicdJobRunId2(), "m.getCicdJobRunId2() cannot be null.");

        String sql = "INSERT INTO ${company}.cicd_job_run_artifact_mappings AS m " +
                " (cicd_job_run_id1, cicd_job_run_id2)" +
                " VALUES " +
                " (:cicd_job_run_id1::UUID, :cicd_job_run_id2::UUID)" +
                " ON CONFLICT (cicd_job_run_id1, cicd_job_run_id2)" +
                " DO NOTHING ";
        sql = StringSubstitutor.replace(sql, Map.of("company", company));
        Map<String, Object> params = Map.of(
                "cicd_job_run_id1", m.getCicdJobRunId1(),
                "cicd_job_run_id2", m.getCicdJobRunId2()
        );

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, new MapSqlParameterSource(params), keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return get(company, m.getCicdJobRunId1(), m.getCicdJobRunId2())
                    .map(CiCdJobRunArtifactMapping::getId)
                    .orElseThrow(() -> new SQLException("Failed to get artifact id"));
        }
        return keyHolder.getKeys().get("id").toString();
    }

    public void replace(String company, IntermediateMapping intermediateMapping) {
        bulkReplace(company, List.of(intermediateMapping));
    }

    protected List<IntermediateMapping> consolidateMappings(List<IntermediateMapping> mappingsIn) {
        Map<UUID, Set<UUID>> mappingMap = new HashMap<>();
        for (IntermediateMapping mapping : mappingsIn) {
            if (mapping.getRunId1() == null || CollectionUtils.isEmpty(mapping.getRunIds())) {
                continue;
            }
            mappingMap.putIfAbsent(mapping.getRunId1(), new HashSet<>());
            Set<UUID> currentSet = mappingMap.get(mapping.getRunId1());
            currentSet.addAll(mapping.getRunIds());
        }

        return mappingMap.keySet().stream().map(runId1 -> {
            Set<UUID> runIds = mappingMap.get(runId1);
            return IntermediateMapping.builder()
                    .runId1(runId1)
                    .runIds(runIds)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * For each intermediate mapping, { runId1 -> [ runId2_a, runId2_b, runId2_c, ... ] },
     * normalize and insert each final mapping: { runId1 -> runId2_a }, { run_Id1 -> runId2_b }, etc.
     * then delete all pre-existing mappings not in the list.
     */
    public void bulkReplace(String company, List<IntermediateMapping> intermediateMappings) {
        if (ListUtils.isEmpty(intermediateMappings)) {
            return;
        }
        // Consolidate mappings so that the delete below works correctly even if there are
        // multiple mappings with the same runId1
        intermediateMappings = consolidateMappings(intermediateMappings);

        String insertSql = "INSERT INTO ${company}.cicd_job_run_artifact_mappings AS t " +
                " (cicd_job_run_id1, cicd_job_run_id2)" +
                " VALUES " +
                " (:cicd_job_run_id1::UUID, :cicd_job_run_id2::UUID)" +
                " ON CONFLICT (cicd_job_run_id1, cicd_job_run_id2)" +
                " DO NOTHING ";
        insertSql = StringSubstitutor.replace(insertSql, Map.of("company", company));
        MapSqlParameterSource[] insertBulkParams = intermediateMappings.stream()
                .filter(im -> im.getRunId1() != null)
                .flatMap(intermediateMapping -> SetUtils.emptyIfNull(intermediateMapping.getRunIds()).stream()
                        .filter(Objects::nonNull)
                        .map(runId2 -> new MapSqlParameterSource(Map.of(
                                "cicd_job_run_id1", intermediateMapping.getRunId1(),
                                "cicd_job_run_id2", runId2
                        ))))
                .toArray(MapSqlParameterSource[]::new);

        String deleteSql = "DELETE FROM ${company}.cicd_job_run_artifact_mappings " +
                " WHERE cicd_job_run_id1 = :cicd_job_run_id1::UUID" +
                " AND NOT cicd_job_run_id2 = ANY (:cicd_job_run_id2_array) ";
        deleteSql = StringSubstitutor.replace(deleteSql, Map.of("company", company));
        MapSqlParameterSource[] deleteBulkParams = intermediateMappings.stream()
                .filter(im -> im.getRunId1() != null)
                .filter(im -> CollectionUtils.isNotEmpty(im.getRunIds()))
                .map(intermediateMapping -> new MapSqlParameterSource(Map.of(
                        "cicd_job_run_id1", intermediateMapping.getRunId1(),
                        "cicd_job_run_id2_array", intermediateMapping.getRunIds().toArray(new UUID[0]) // PROP-3631 very important to use arrays and not lists!
                )))
                .toArray(MapSqlParameterSource[]::new);

        if (log.isDebugEnabled()) {
            log.debug("insertSql = " + insertSql);
            log.debug("insertParams = " + DefaultObjectMapper.writeAsPrettyJson(insertBulkParams));
            log.debug("deleteSql = " + deleteSql);
            log.debug("deleteParams = " + DefaultObjectMapper.writeAsPrettyJson(deleteBulkParams));
        }

        template.batchUpdate(insertSql, insertBulkParams);
        template.batchUpdate(deleteSql, deleteBulkParams);
    }

    @Override
    public Boolean update(String company, CiCdJobRunArtifactMapping t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<CiCdJobRunArtifactMapping> get(String company, String id) throws SQLException {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(id, "id cannot be null or empty.");

        String sql = "SELECT * FROM " + company + "." + TABLE_NAME +
                " WHERE id = :id::UUID " +
                " LIMIT 1 ";

        try {
            List<CiCdJobRunArtifactMapping> results = template.query(sql, Map.of("id", id),
                    CiCdJobRunArtifactMappingConverters.rowMapper(objectMapper));
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get artifact for id={}", id, e);
            return Optional.empty();
        }
    }

    public Optional<CiCdJobRunArtifactMapping> get(String company, UUID cicdJobRunId1, UUID cicdJobRunId2) throws SQLException {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notNull(cicdJobRunId1, "cicdJobRunId1 cannot be null.");
        Validate.notNull(cicdJobRunId2, "cicdJobRunId2 cannot be null.");

        String sql = "SELECT * FROM " + company + "." + TABLE_NAME +
                " WHERE cicd_job_run_id1 = :cicdJobRunId1::UUID " +
                " AND cicd_job_run_id2 = :cicdJobRunId2::UUID " +
                " LIMIT 1 ";
        Map<String, Object> params = Map.of(
                "cicdJobRunId1", cicdJobRunId1,
                "cicdJobRunId2", cicdJobRunId2
        );
        try {
            List<CiCdJobRunArtifactMapping> results = template.query(sql, new MapSqlParameterSource(params),
                    CiCdJobRunArtifactMappingConverters.rowMapper(objectMapper));
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get artifact mapping for cicd_job_run_id1={}, cicd_job_run_id2={}", cicdJobRunId1, cicdJobRunId2, e);
            return Optional.empty();
        }
    }

    @Value
    @Builder(toBuilder = true)
    public static class CicdJobRunArtifactMappingFilter {
        List<String> cicdJobRunId1List;
        List<String> cicdJobRunId2List;
    }

    @Override
    public DbListResponse<CiCdJobRunArtifactMapping> filter(Integer pageNumber, Integer pageSize, String company, @Nullable CicdJobRunArtifactMappingFilter filter) {
        Validate.notBlank(company, "company cannot be null or empty.");
        int limit = MoreObjects.firstNonNull(pageSize, PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);
        filter = filter != null ? filter : CicdJobRunArtifactMappingFilter.builder().build();

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        // -- id1
        if (CollectionUtils.isNotEmpty(filter.getCicdJobRunId1List())) {
            conditions.add("cicd_job_run_id1::text IN (:cicd_job_run_id1)");
            params.put("cicd_job_run_id1", filter.getCicdJobRunId1List());
        }
        // -- id2
        if (CollectionUtils.isNotEmpty(filter.getCicdJobRunId2List())) {
            conditions.add("cicd_job_run_id2::text IN (:cicd_job_run_id2)");
            params.put("cicd_job_run_id2", filter.getCicdJobRunId2List());
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM ${company}.cicd_job_run_artifact_mappings " +
                where +
                " OFFSET :skip " +
                " LIMIT :limit ";
        sql = StringSubstitutor.replace(sql, Map.of("company", company));
        List<CiCdJobRunArtifactMapping> results = template.query(sql, params, CiCdJobRunArtifactMappingConverters.rowMapper(objectMapper));

        return DbListResponse.of(results, null);
    }

    public void bulkDeleteByRunId1(String company, Stream<String> runIds) throws SQLException {
        String sql = " DELETE FROM ${company}.cicd_job_run_artifact_mappings " +
                " WHERE cicd_job_run_id1 = :id::UUID ";
        sql = StringSubstitutor.replace(sql, Map.of("company", company));
        MapSqlParameterSource[] batchParams = runIds.map(id -> new MapSqlParameterSource("id", id))
                .toArray(MapSqlParameterSource[]::new);
        template.batchUpdate(sql, batchParams);
    }

    public void bulkDelete(String company, Stream<String> ids) throws SQLException {
        String sql = " DELETE FROM ${company}.cicd_job_run_artifact_mappings " +
                " WHERE id = :id::UUID ";
        sql = StringSubstitutor.replace(sql, Map.of("company", company));
        MapSqlParameterSource[] batchParams = ids.map(id -> new MapSqlParameterSource("id", id))
                .toArray(MapSqlParameterSource[]::new);
        template.batchUpdate(sql, batchParams);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        bulkDelete(company, Stream.of(id));
        return true;
    }

    public Boolean delete(String company, String cicdJobRunId1, String cicdJobRunId2) throws SQLException {
        String sql = " DELETE FROM ${company}.cicd_job_run_artifact_mappings " +
                " WHERE " +
                "     cicd_job_run_id1 = :cicdJobRunId1::UUID " +
                " AND cicd_job_run_id2 = :cicdJobRunId2::UUID ";
        sql = StringSubstitutor.replace(sql, Map.of("company", company));
        Map<String, Object> params = Map.of(
                "cicdJobRunId1", cicdJobRunId1,
                "cicdJobRunId2", cicdJobRunId2
        );
        return template.update(sql, params) > 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        Stream.of("CREATE TABLE IF NOT EXISTS ${company}.cicd_job_run_artifact_mappings(" +
                                " id               UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                                " cicd_job_run_id1 UUID NOT NULL REFERENCES " + company + "." + CiCdJobRunsDatabaseService.TABLE_NAME + "(id) ON DELETE CASCADE," +
                                " cicd_job_run_id2 UUID NOT NULL REFERENCES " + company + "." + CiCdJobRunsDatabaseService.TABLE_NAME + "(id) ON DELETE CASCADE," +
                                " created_at       TIMESTAMPTZ NOT NULL DEFAULT now()" +
                                ")",
                        "CREATE UNIQUE INDEX IF NOT EXISTS cicd_job_run_artifact_mappings__cicd_job_run_ids_idx " +
                                " ON ${company}.cicd_job_run_artifact_mappings (cicd_job_run_id1, cicd_job_run_id2) "
                )
                .map(sql -> StringSubstitutor.replace(sql, Map.of("company", company)))
                .forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
