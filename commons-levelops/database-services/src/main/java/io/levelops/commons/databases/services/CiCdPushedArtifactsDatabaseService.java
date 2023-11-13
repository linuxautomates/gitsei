package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.converters.cicd.CiCdPushedConverters;
import io.levelops.commons.databases.models.database.cicd.CiCdJobRunArtifact;
import io.levelops.commons.databases.models.database.cicd.DbCiCdPushedArtifact;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class CiCdPushedArtifactsDatabaseService extends DatabaseService<DbCiCdPushedArtifact> {

    private static final String CICD_PUSHED_ARTIFACTS_TABLE = "cicd_pushed_artifacts";
    private static final String CICD_PUSHED_ARTIFACTS_INSERT_SQL = "INSERT INTO %s." + CICD_PUSHED_ARTIFACTS_TABLE
            + " (job_run_number, job_name, repository, integration_id, artifacts, created_at)"
            + " VALUES (:job_run_number,:job_name,:repository,:integration_id,:artifacts::jsonb,now()) RETURNING id";
    private static final int BATCH_SIZE = 100;
    private static final String UNKNOWN_TYPE = "unknown";
    private final CiCdJobRunArtifactsDatabaseService ciCdJobRunArtifactsDatabaseService;
    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper mapper;

    public CiCdPushedArtifactsDatabaseService(DataSource dataSource,
                                              final ObjectMapper mapper,
                                              CiCdJobRunArtifactsDatabaseService ciCdJobRunArtifactsDatabaseService) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        this.mapper = mapper;
        this.ciCdJobRunArtifactsDatabaseService = ciCdJobRunArtifactsDatabaseService;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbCiCdPushedArtifact dbCiCdPushedArtifact){
        throw new NotImplementedException("insert not implemented!");
    }

    public String insertPushedArtifacts(String company, DbCiCdPushedArtifact dbCiCdPushedArtifact) throws SQLException {
        Validate.notBlank(company, "company cannot be null or empty.");

        String sql = String.format(CICD_PUSHED_ARTIFACTS_INSERT_SQL, company);
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("job_run_number", dbCiCdPushedArtifact.getJobRunNumber());
        params.addValue("job_name", dbCiCdPushedArtifact.getJobName());
        params.addValue("repository", dbCiCdPushedArtifact.getRepository());
        params.addValue("integration_id", Integer.parseInt(dbCiCdPushedArtifact.getIntegrationId()));
        params.addValue("artifacts", ParsingUtils.serialize(mapper, "artifacts", dbCiCdPushedArtifact.getArtifacts(), "{}"));
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to insert pushed artifact record! " + dbCiCdPushedArtifact);
        }
        UUID id = (UUID) keyHolder.getKeys().get("id");
        return id.toString();
    }

    @Override
    public Boolean update(String company, DbCiCdPushedArtifact t) throws SQLException {
        throw new NotImplementedException("update not implemented!");
    }

    @Override
    public Optional<DbCiCdPushedArtifact> get(String company, String param) throws SQLException {
        throw new NotImplementedException("get not implemented!");
    }

    @Override
    public DbListResponse<DbCiCdPushedArtifact> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        throw new NotImplementedException("list not implemented!");
    }

    public List<DbCiCdPushedArtifact> filterPushedArtifacts(String company, List<String> jobNames, List<Long> jobRunNumbers, List<String> repositories, List<Integer> integrationIds) {
        Validate.notBlank(company, "company cannot be null or empty.");

        List<String> conditions = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();

        if (CollectionUtils.isNotEmpty(jobRunNumbers)) {
            conditions.add("job_run_number IN (:job_run_numbers)");
            params.put("job_run_numbers", jobRunNumbers);
        }
        if (CollectionUtils.isNotEmpty(jobNames)) {
            conditions.add("job_name IN (:job_names)");
            params.put("job_names", jobNames);
        }
        if (CollectionUtils.isNotEmpty(repositories)) {
            conditions.add("repository IN (:repositories)");
            params.put("repositories", repositories);
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            conditions.add("integration_id IN (:integration_ids)");
            params.put("integration_ids", integrationIds);
        }

        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        String sql = "SELECT * FROM " + company + "." + CICD_PUSHED_ARTIFACTS_TABLE + where;
        List<DbCiCdPushedArtifact> artifacts = template.query(sql, params, CiCdPushedConverters.pushedArtifactsRowMapper(mapper));
        return artifacts;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new NotImplementedException("delete not implemented!");
    }

    public int cleanUpPushedArtifactsData(String company, List<UUID> artifactIds) {
        if (CollectionUtils.isEmpty(artifactIds)) {
            return 0;
        }
        return template.update("DELETE FROM " + company + "." + CICD_PUSHED_ARTIFACTS_TABLE +
                " WHERE id IN (:artifact_ids)", Map.of("artifact_ids", artifactIds));
    }

    public List<String> insertCiCdJobRunArtifactsFromPushedArtifacts(String company, String integrationId, UUID ciCdJobRunId, String jobName, Long jobRunNumber, String repository, List<String> pushedArtifactIds) throws SQLException {
        List<DbCiCdPushedArtifact> pushedArtifacts = filterPushedArtifacts(company, List.of(jobName),
                List.of(jobRunNumber), List.of(repository), List.of(Integer.valueOf(integrationId)));
        if(CollectionUtils.isEmpty(pushedArtifacts)) {
            log.info("No pushed_artifacts found for tenant: " + company + " integration_id : " + integrationId +
                    " for " + repository + "/" + jobName + "/" + jobRunNumber);
            return List.of();
        }

        List<CiCdJobRunArtifact> artifacts = new ArrayList<>();
        pushedArtifactIds.addAll(pushedArtifacts.stream().map(DbCiCdPushedArtifact::getId).collect(Collectors.toList()));
        for (var dbCiCdPushedArtifact : pushedArtifacts) {
            for (var artifact : CollectionUtils.emptyIfNull(dbCiCdPushedArtifact.getArtifacts())) {
                CiCdJobRunArtifact ciCdJobRunArtifact = CiCdJobRunArtifact.builder()
                        .cicdJobRunId(ciCdJobRunId)
                        .input(Boolean.TRUE)
                        .output(Boolean.FALSE)
                        .location(artifact.getLocation())
                        .name(artifact.getName())
                        .qualifier(artifact.getTag())
                        .hash(artifact.getDigest())
                        .type(artifact.getType())
                        .build();
                artifacts.add(ciCdJobRunArtifact);
            }
        }
        return ciCdJobRunArtifactsDatabaseService.replace(company, ciCdJobRunId.toString(), artifacts);
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sql = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + "." + CICD_PUSHED_ARTIFACTS_TABLE + " (" +
                        " id                UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " job_run_number    BIGINT NOT NULL," +
                        " job_name          VARCHAR NOT NULL," +
                        " repository        VARCHAR NOT NULL," +
                        " integration_id    INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE," +
                        " artifacts         JSONB NOT NULL," +
                        " created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()" +
                        ")"
        );
        sql.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
