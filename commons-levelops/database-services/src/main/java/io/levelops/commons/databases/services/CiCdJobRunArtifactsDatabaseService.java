package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.cicd.CiCdJobRunArtifactConverters;
import io.levelops.commons.databases.models.database.cicd.CiCdJobRunArtifact;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Table to store artifacts generated or consumed by a CICD Job Run.
 * Note that these artifacts are not shared between job runs, even if they refer to the same physical artifact.
 * For correlation between physical artifacts and jobs, see {@link CiCdJobRunArtifactMappingDatabaseService}.
 */
@Log4j2
@Service
public class CiCdJobRunArtifactsDatabaseService extends DatabaseService<CiCdJobRunArtifact> {

    public static final String TABLE_NAME = "cicd_job_run_artifacts";
    private static final String UNKNOWN_TYPE = "unknown";
    private static final int PAGE_SIZE = 100;
    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper objectMapper;

    public CiCdJobRunArtifactsDatabaseService(DataSource dataSource, ObjectMapper objectMapper) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(CiCdJobRunsDatabaseService.class);
    }

    @Override
    public String insert(String company, CiCdJobRunArtifact artifact) throws SQLException {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notNull(artifact, "artifact cannot be null.");
        Validate.notNull(artifact.getCicdJobRunId(), "artifact.getCicdJobRunId() cannot be null.");
        Validate.notNull(artifact.getName(), "artifact.getName() cannot be null.");

        String sql = "INSERT INTO " + company + "." + TABLE_NAME + " AS t " +
                " (cicd_job_run_id, input, output, type, location, name, qualifier, hash, metadata)" +
                " VALUES " +
                " (:cicd_job_run_id::UUID, :input, :output, :type, :location, :name, :qualifier, :hash, :metadata::JSONB)" +
                " ON CONFLICT (cicd_job_run_id, type, location, name, qualifier, hash)" +
                " DO UPDATE" +
                " SET " +
                "   input = EXCLUDED.input," +
                "   output = EXCLUDED.output," +
                "   metadata = EXCLUDED.metadata" +
                " WHERE" +
                " (t.input, t.output, t.metadata) " +
                " IS DISTINCT FROM " +
                " (EXCLUDED.input, EXCLUDED.output, EXCLUDED.metadata)";

        String type = StringUtils.defaultString(artifact.getType(), UNKNOWN_TYPE);
        String location = StringUtils.defaultString(artifact.getLocation());
        String qualifier = StringUtils.defaultString(artifact.getQualifier());
        String hash = StringUtils.defaultString(artifact.getHash());

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("cicd_job_run_id", artifact.getCicdJobRunId());
        params.addValue("input", artifact.getInput());
        params.addValue("output", artifact.getOutput());
        params.addValue("type", type);
        params.addValue("location", location);
        params.addValue("name", artifact.getName());
        params.addValue("qualifier", qualifier);
        params.addValue("hash", hash);
        params.addValue("metadata", ParsingUtils.serialize(DefaultObjectMapper.get(), "metadata", artifact.getMetadata(), "{}"));

        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            return get(company, artifact.getCicdJobRunId().toString(), type, location, artifact.getName(), qualifier, hash)
                    .map(CiCdJobRunArtifact::getId)
                    .orElseThrow(() -> new SQLException("Failed to get artifact id"));
        }
        return keyHolder.getKeys().get("id").toString();
    }

    /**
     * Upsert a list of artifacts for a given runId. Delete any other artifacts not in the list.
     */
    public List<String> replace(String company, String cicdJobRunId, List<CiCdJobRunArtifact> artifacts) throws SQLException {
        List<String> idsToKeep;
        try {
            idsToKeep = artifacts.stream()
                    .filter(a -> a.getCicdJobRunId() != null && cicdJobRunId.equals(a.getCicdJobRunId().toString()))
                    .map(RuntimeStreamException.wrap(a -> insert(company, a)))
                    .collect(Collectors.toList());
        } catch (RuntimeStreamException e) {
            throw new SQLException("Failed to insert artifacts for cicdJobRunId=" + cicdJobRunId, e);
        }

        List<String> idsToDelete = stream(company,
                CiCdJobRunArtifactFilter.builder()
                        .excludeIds(idsToKeep)
                        .cicdJobRunIds(List.of(cicdJobRunId))
                        .build())
                .filter(a -> a.getCicdJobRunId() != null && cicdJobRunId.equals(a.getCicdJobRunId().toString()))
                .map(CiCdJobRunArtifact::getId)
                .collect(Collectors.toList());

        idsToDelete.forEach(id -> {
            try {
                delete(company, id);
            } catch (SQLException e) {
                log.error("Failed to delete artifact id={} for cicdJobRunId={}", id, cicdJobRunId, e);
            }
        });

        return idsToKeep;
    }

    @Override
    public Boolean update(String company, CiCdJobRunArtifact t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<CiCdJobRunArtifact> get(String company, String id) throws SQLException {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(id, "id cannot be null or empty.");

        String sql = "SELECT * FROM " + company + "." + TABLE_NAME +
                " WHERE id = :id::UUID " +
                " LIMIT 1 ";
        try {
            List<CiCdJobRunArtifact> results = template.query(sql, Map.of("id", id),
                    CiCdJobRunArtifactConverters.rowMapper(objectMapper));
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get artifact for id={}", id, e);
            return Optional.empty();
        }
    }

    public Optional<CiCdJobRunArtifact> get(String company, String cicdJobRunId, @Nullable String type, @Nullable String location, String name, @Nullable String qualifier, @Nullable String hash) throws SQLException {
        Validate.notBlank(company, "company cannot be null or empty.");
        Validate.notBlank(cicdJobRunId, "cicdJobRunId cannot be null or empty.");
        Validate.notBlank(name, "name cannot be null or empty.");

        String sql = "SELECT * FROM " + company + "." + TABLE_NAME +
                " WHERE (cicd_job_run_id, type, location, name, qualifier, hash) = (:cicd_job_run_id::UUID, :type, :location, :name, :qualifier, :hash) " +
                " LIMIT 1 ";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("cicd_job_run_id", cicdJobRunId);
        params.addValue("type", StringUtils.defaultString(type, UNKNOWN_TYPE));
        params.addValue("location", StringUtils.defaultString(location));
        params.addValue("name", name);
        params.addValue("qualifier", StringUtils.defaultString(qualifier));
        params.addValue("hash", StringUtils.defaultString(hash));
        try {
            List<CiCdJobRunArtifact> results = template.query(sql, params,
                    CiCdJobRunArtifactConverters.rowMapper(objectMapper));
            return IterableUtils.getFirst(results);
        } catch (DataAccessException e) {
            log.warn("Failed to get artifact for cicd_job_run_id={} type='{}' location='{}' name='{}' qualifier='{}' hash='{}'", cicdJobRunId, type, location, name, qualifier, hash, e);
            return Optional.empty();
        }
    }

    @Value
    @Builder(toBuilder = true)
    public static class CiCdJobRunArtifactFilter {
        List<String> ids;
        List<String> excludeIds;
        List<String> cicdJobRunIds;
        List<String> types;
        List<String> locations;
        List<String> names;
        String partialName;
        List<String> qualifiers;
        List<String> hashes;
        Boolean input;
        Boolean output;

        /**
         * If True, calculate and return total count.
         */
        Boolean count;
    }

    @Override
    public DbListResponse<CiCdJobRunArtifact> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return filter(company, null, pageNumber, pageSize);
    }

    public Stream<CiCdJobRunArtifact> stream(String company, CiCdJobRunArtifactFilter filter) {
        return PaginationUtils.streamThrowingRuntime(0, 1, pageNumber -> filter(company, filter, pageNumber, null).getRecords());
    }

    public DbListResponse<CiCdJobRunArtifact> filter(String company, @Nullable CiCdJobRunArtifactFilter filter, @Nullable Integer pageNumber, @Nullable Integer pageSize) throws SQLException {
        Validate.notBlank(company, "company cannot be null or empty.");
        int limit = MoreObjects.firstNonNull(pageSize, PAGE_SIZE);
        int skip = limit * MoreObjects.firstNonNull(pageNumber, 0);
        filter = filter != null ? filter : CiCdJobRunArtifactFilter.builder().build();

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        params.put("skip", skip);
        params.put("limit", limit);

        // -- ids
        if (CollectionUtils.isNotEmpty(filter.getIds())) {
            conditions.add("id::text IN (:ids)");
            params.put("ids", filter.getIds());
        }
        // -- exclude ids
        if (CollectionUtils.isNotEmpty(filter.getExcludeIds())) {
            conditions.add("id::text NOT IN (:ids)");
            params.put("ids", filter.getExcludeIds());
        }
        // -- cicdJobRunIds
        if (CollectionUtils.isNotEmpty(filter.getCicdJobRunIds())) {
            conditions.add("cicd_job_run_id::text IN (:cicd_job_run_ids)");
            params.put("cicd_job_run_ids", filter.getCicdJobRunIds());
        }
        // -- types
        if (CollectionUtils.isNotEmpty(filter.getTypes())) {
            conditions.add("type IN (:types)");
            params.put("types", filter.getTypes());
        }
        // -- locations
        if (CollectionUtils.isNotEmpty(filter.getLocations())) {
            conditions.add("location IN (:locations)");
            params.put("locations", filter.getLocations());
        }
        // -- names
        if (CollectionUtils.isNotEmpty(filter.getNames())) {
            conditions.add("name IN (:names)");
            params.put("names", filter.getNames());
        }
        // -- partial name
        if (StringUtils.isNotEmpty(filter.getPartialName())) {
            conditions.add("name ILIKE :partial_name");
            params.put("partial_name", StringUtils.wrap(filter.getPartialName(), "%"));
        }
        // -- qualifiers
        if (CollectionUtils.isNotEmpty(filter.getQualifiers())) {
            conditions.add("qualifier IN (:qualifiers)");
            params.put("qualifiers", filter.getQualifiers());
        }
        // -- hashes
        if (CollectionUtils.isNotEmpty(filter.getHashes())) {
            conditions.add("hash IN (:hashes)");
            params.put("hashes", filter.getHashes());
        }
        // -- input
        if (filter.getInput() != null) {
            conditions.add("input = (:input)");
            params.put("input", filter.getInput());
        }
        // -- output
        if (filter.getOutput() != null) {
            conditions.add("output = (:output)");
            params.put("output", filter.getOutput());
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + "." + TABLE_NAME +
                where +
                " ORDER BY name ASC " +
                " OFFSET :skip " +
                " LIMIT :limit ";
        List<CiCdJobRunArtifact> results = template.query(sql, params, CiCdJobRunArtifactConverters.rowMapper(objectMapper));

        Integer count = null;
        if (BooleanUtils.isTrue(filter.getCount())) {
            String countSql = "SELECT count(*) FROM " + company + "." + TABLE_NAME + where;
            count = template.queryForObject(countSql, params, Integer.class);
        }
        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String sql = " DELETE " +
                " FROM " + company + "." + TABLE_NAME +
                " WHERE id = :id::UUID ";
        Map<String, Object> params = Map.of("id", id);
        return template.update(sql, params) > 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sql = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + "." + TABLE_NAME + "(" +
                        " id         UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " cicd_job_run_id UUID NOT NULL REFERENCES " + company + "." + CiCdJobRunsDatabaseService.TABLE_NAME + "(id) ON DELETE CASCADE," +
                        " input      BOOLEAN," +
                        " output     BOOLEAN," +
                        " type       TEXT NOT NULL DEFAULT '" + UNKNOWN_TYPE + "'," +
                        " location   TEXT NOT NULL DEFAULT ''," +
                        " name       TEXT NOT NULL," +
                        " qualifier  TEXT NOT NULL DEFAULT ''," +
                        " hash       TEXT NOT NULL DEFAULT ''," +
                        " metadata   JSONB NOT NULL DEFAULT '{}'::JSONB," +
                        " created_at TIMESTAMPTZ NOT NULL DEFAULT now()" +
                        ")",

                // Note: some of those fields could be missing, but making everything not null to ensure the unique constraint works
                // Any combination of these fields would identify a different artifact, so we have to put everything in the constraint.
                // Fuzzy logic to correlate jobs through artifacts will be implemented in a mapping table.
                "CREATE UNIQUE INDEX IF NOT EXISTS " + TABLE_NAME + "_job_run_id_type_name_location_qualifier_hash_idx" +
                        " ON " + company + "." + TABLE_NAME + " (cicd_job_run_id, type, location, name, qualifier, hash)",
                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_hash_idx" + " ON " + company + "." + TABLE_NAME + " (hash)",
                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_name_qualifier_location_idx" + " ON " + company + "." + TABLE_NAME + " (name, qualifier, location)"
        );
        sql.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
