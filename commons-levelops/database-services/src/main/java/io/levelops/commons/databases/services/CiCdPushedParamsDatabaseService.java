package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.converters.cicd.CiCdPushedConverters;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.cicd.DbCiCdPushedJobRunParam;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
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
public class CiCdPushedParamsDatabaseService extends DatabaseService<DbCiCdPushedJobRunParam>{

    private final NamedParameterJdbcTemplate template;
    private static final int BATCH_SIZE = 100;
    private static final String CICD_PUSHED_PARAM_TABLE = "cicd_pushed_params";
    private static final String CICD_PUSHED_PARAMS_INSERT_SQL = "INSERT INTO %s." + CICD_PUSHED_PARAM_TABLE
            + " (job_run_number, job_name, repository, integration_id, job_run_params, created_at)"
            + " VALUES (:job_run_number,:job_name,:repository,:integration_id,:job_run_params::jsonb,now()) RETURNING id";
    private static final String CICD_JOB_RUN_PARAMS_INSERT_SQL = "INSERT INTO %s.cicd_job_run_params"
            + "(cicd_job_run_id,type,name,value) VALUES(?,?,?,?) RETURNING id";

    private ObjectMapper mapper;

    public CiCdPushedParamsDatabaseService(DataSource dataSource, final ObjectMapper mapper) {
        super(dataSource);
        this.mapper = mapper;
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public String insert(String company, DbCiCdPushedJobRunParam t) throws SQLException {
        throw new NotImplementedException("insert not implemented!");
    }

    public String insertPushedJobRunParams(String company, DbCiCdPushedJobRunParam dbCiCdPushedJobRunParam) throws SQLException {
        Validate.notBlank(company, "company cannot be null or empty.");
        String sql = String.format(CICD_PUSHED_PARAMS_INSERT_SQL, company);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("job_run_number", dbCiCdPushedJobRunParam.getJobRunNumber());
        params.addValue("job_name", dbCiCdPushedJobRunParam.getJobName());
        params.addValue("repository", dbCiCdPushedJobRunParam.getRepository());
        params.addValue("integration_id", Integer.parseInt(dbCiCdPushedJobRunParam.getIntegrationId()));
        params.addValue("job_run_params", ParsingUtils.serialize(mapper, "job_run_params", dbCiCdPushedJobRunParam.getJobRunParams(), "{}"));
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(sql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to insert pushed job run param record! " + dbCiCdPushedJobRunParam);
        }
        UUID id = (UUID) keyHolder.getKeys().get("id");
        return id.toString();
    }

    @Override
    public Boolean update(String company, DbCiCdPushedJobRunParam t) throws SQLException {
        throw new NotImplementedException("update not implemented!");
    }

    @Override
    public Optional<DbCiCdPushedJobRunParam> get(String company, String param) throws SQLException {
        throw new NotImplementedException("get not implemented!");
    }

    @Override
    public DbListResponse<DbCiCdPushedJobRunParam> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        throw new NotImplementedException("list not implemented!");
    }

    public List<DbCiCdPushedJobRunParam> filterPushedParams(String company, List<String> jobNames, List<Long> jobRunNumbers, List<String> repositories, List<Integer> integrationIds) {
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
        String sql = "SELECT * FROM " + company + "." + CICD_PUSHED_PARAM_TABLE + where;
        List<DbCiCdPushedJobRunParam> results = template.query(sql, params, CiCdPushedConverters.pushedParamsRowMapper(mapper));

        return results;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new NotImplementedException("delete not implemented!");
    }

    public int cleanUpPushedParamsData(String company, List<UUID> paramIds) {
        if (CollectionUtils.isEmpty(paramIds)) {
            return 0;
        }
        return template.update("DELETE FROM " + company + "." + CICD_PUSHED_PARAM_TABLE +
                " WHERE id IN (:param_ids)", Map.of("param_ids", paramIds));
    }

    public List<String> insertCiCdJobRunParamsFromPushedParams(String company, String integrationId, UUID ciCdJobRunId, String jobName, Long jobRunNumber, String repository) {
        List<DbCiCdPushedJobRunParam> dbCiCdPushedJobRunParams = filterPushedParams(company, List.of(jobName),
                List.of(jobRunNumber), List.of(repository), List.of(Integer.valueOf(integrationId)));
        if(CollectionUtils.isEmpty(dbCiCdPushedJobRunParams)) {
            log.info("No params found for tenant : " + company + "integration_id : " + integrationId +" for /" + repository + "/" + jobName + "/" + jobRunNumber);
            return List.of();
        }

        List<String> paramIds = dbCiCdPushedJobRunParams.stream().map(DbCiCdPushedJobRunParam::getId).collect(Collectors.toList());
        List<CICDJobRun.JobRunParam> jobRunParams = new ArrayList<>();
        for (var dbCiCdPushedJobRunParam : dbCiCdPushedJobRunParams) {
            for (var param : CollectionUtils.emptyIfNull(dbCiCdPushedJobRunParam.getJobRunParams())) {
                jobRunParams.add(CICDJobRun.JobRunParam.builder()
                        .type(param.getType())
                        .name(param.getName())
                        .value(param.getValue())
                        .build());
            }
        }

        insertCiCdJobRunParams(company, ciCdJobRunId, jobRunParams);
        return paramIds;
    }

    public void insertCiCdJobRunParams(String company, UUID ciCdJobRunId, List<CICDJobRun.JobRunParam> jobRunParams) {
        Validate.notBlank(company, "company cannot be null or empty.");

        String sql = String.format(CICD_JOB_RUN_PARAMS_INSERT_SQL, company);

        try(Connection conn = dataSource.getConnection();
            PreparedStatement insertCiCdJobRunParamsPstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int count = 0;

            for (CICDJobRun.JobRunParam jobRunParam: jobRunParams) {
                int idx = 1;
                insertCiCdJobRunParamsPstmt.setObject(idx++, ciCdJobRunId);
                insertCiCdJobRunParamsPstmt.setObject(idx++, jobRunParam.getType());
                insertCiCdJobRunParamsPstmt.setObject(idx++, jobRunParam.getName());
                insertCiCdJobRunParamsPstmt.setObject(idx, jobRunParam.getValue());
                count++;
                insertCiCdJobRunParamsPstmt.addBatch();
                if (count % BATCH_SIZE == 0) {
                    insertCiCdJobRunParamsPstmt.executeBatch();
                }
            }
            if (count % BATCH_SIZE != 0) {
                insertCiCdJobRunParamsPstmt.executeBatch();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert params to cicd_job_run_params: " + e);
        }
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sql = List.of("CREATE TABLE IF NOT EXISTS " + company +"." + CICD_PUSHED_PARAM_TABLE + " (" +
                " id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                " job_run_number    BIGINT NOT NULL," +
                " job_name          VARCHAR NOT NULL," +
                " repository        VARCHAR NOT NULL," +
                " integration_id    INTEGER NOT NULL REFERENCES "
                + company + ".integrations(id) ON DELETE CASCADE," +
                " job_run_params    JSONB NOT NULL," +
                " created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()" +
                ")"
        );
        sql.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
