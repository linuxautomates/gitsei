package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.TriageRuleHit;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
public class TriageRuleHitsService extends DatabaseService<TriageRuleHit> {

    public static String JENKINS_HIT_TABLE = "jenkins_rule_hits";
    public static String CICD_JOBS_TABLE = "cicd_jobs";
    public static String CICD_JOB_RUNS_TABLE = "cicd_job_runs";

    private final ObjectMapper mapper;
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public TriageRuleHitsService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        this.mapper = mapper;
    }

    public static RowMapper<TriageRuleHit> jenkinsHitRowMapper(final ObjectMapper mapper) {
        return (rs, rowNumber) -> {
            var contextString = rs.getString("context");
            Map<String, Object> context = Map.of();
            try {
                if (Strings.isNotBlank(contextString)) {
                    context = mapper.readValue(contextString,mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
                }
            } catch (JsonProcessingException e) {
                log.error("Unable to parse the context: {}", contextString, e);
            }
            return TriageRuleHit.builder()
                    .id(rs.getString("id"))
                    .jobRunId(rs.getString("job_run_id"))
                    .hitContent(rs.getString("hit_content"))
                    .ruleId(rs.getString("rule_id"))
                    .stageId(rs.getString("stage_id"))
                    .stepId(rs.getString("step_id"))
                    .count(rs.getInt("count"))
                    .type(TriageRuleHit.RuleHitType.JENKINS)
                    .createdAt(rs.getLong("created_at"))
                    .context(context)
                    .build();
        };
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(TriageRulesService.class,
                CiCdJobRunStageStepsDatabaseService.class,
                CiCdJobRunStageDatabaseService.class,
                CiCdJobRunsDatabaseService.class);
    }

    @Override
    public String insert(String company, TriageRuleHit ruleHit) throws SQLException {
        //TODO: when we have more than jenkins, check the 'type' before inserting into the right table.
        return template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            String SQL = "INSERT INTO " + company + "." + JENKINS_HIT_TABLE
                    + "(job_run_id,stage_id,step_id,rule_id,hit_content,count, context) VALUES(?,?,?,?,?,?,?::jsonb)";

            try (PreparedStatement pstmt = conn.prepareStatement(SQL,
                    Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setObject(1, UUID.fromString(ruleHit.getJobRunId()));
                pstmt.setObject(2, StringUtils.isEmpty(ruleHit.getStageId()) ? null : UUID.fromString(ruleHit.getStageId()));
                pstmt.setObject(3, StringUtils.isEmpty(ruleHit.getStepId()) ? null : UUID.fromString(ruleHit.getStepId()));
                pstmt.setObject(4, UUID.fromString(ruleHit.getRuleId()));
                pstmt.setObject(5, ruleHit.getHitContent());
                pstmt.setObject(6, ruleHit.getCount());
                try {
                    pstmt.setObject(7, MapUtils.isNotEmpty(ruleHit.getContext()) ? mapper.writeValueAsString(ruleHit.getContext()) : "{}");
				} catch (JsonProcessingException e) {
                    throw new SQLException("Unable to convert the hit context into a json string", e);
				}

                int affectedRows = pstmt.executeUpdate();
                // check the affected rows
                if (affectedRows > 0) {
                    // get the ID back
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            return rs.getString(1);
                        }
                    }
                }
            }
            throw new SQLException("ERROR. Failed to insert triage rule hit.");
        }));
    }

    @Override
    public Boolean update(String company, TriageRuleHit rule) {
        throw new UnsupportedOperationException("No updates allowed.");
    }

    @Override
    public Optional<TriageRuleHit> get(String company, String id) throws SQLException {
        var results = listJenkinsRuleHits(company, List.of(UUID.fromString(id)), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 0, 1);
        if (results == null || results.getCount() < 1) {
            return Optional.empty();
        }
        return Optional.of(results.getRecords().get(0));
    }

    @Override
    public DbListResponse<TriageRuleHit> list(String company, Integer pageNumber, Integer pageSize)
            throws SQLException {
        throw new UnsupportedOperationException("No direct list allowed.");
    }

    public DbListResponse<TriageRuleHit> listJenkinsRuleHits(String company,
                                                             List<String> jobRunIds,
                                                             List<String> stageIds,
                                                             List<String> stepIds,
                                                             List<String> ruleIds,
                                                             List<String> jobNames,
                                                             List<String> jobNormalizedFullNames,
                                                             List<String> cicdInstanceIds,
                                                             Integer pageNumber,
                                                             Integer pageSize) {
        return listJenkinsRuleHits(company, List.of(), jobRunIds, stageIds, stepIds, ruleIds, jobNames, jobNormalizedFullNames, cicdInstanceIds, pageNumber, pageSize);
    }

    public DbListResponse<TriageRuleHit> listJenkinsRuleHits(String company,
                                                             List<UUID> ids,
                                                             List<String> jobRunIds,
                                                             List<String> stageIds,
                                                             List<String> stepIds,
                                                             List<String> ruleIds,
                                                             List<String> jobNames,
                                                             List<String> jobNormalizedFullNames,
                                                             List<String> cicdInstanceIds,
                                                             Integer pageNumber,
                                                             Integer pageSize) {
        List<String> criteria = new ArrayList<>();
        List<String> jobsTableCriteria = new ArrayList<>();
        List<String> jobRunsTableCriteria = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (CollectionUtils.isNotEmpty(jobRunIds)) {
            criteria.add("job_run_id IN ( :job_run_ids )");
            jobRunsTableCriteria.add("id IN ( :job_run_ids )");
            params.put("job_run_ids", jobRunIds.stream().map(UUID::fromString).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(stageIds)) {
            criteria.add("stage_id IN ( :stage_ids )");
            params.put("stage_ids", stageIds.stream().map(UUID::fromString).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(ruleIds)) {
            criteria.add("rule_id IN ( :rule_ids )");
            params.put("rule_ids", ruleIds.stream().map(UUID::fromString).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(ids)) {
            criteria.add("id IN ( :ids )");
            params.put("ids", ids);
        }
        if (CollectionUtils.isNotEmpty(stepIds)) {
            criteria.add("step_id IN ( :step_ids )");
            params.put("step_ids", stepIds.stream().map(UUID::fromString).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(jobNames)) {
            jobsTableCriteria.add("job_name IN ( :job_names )");
            params.put("job_names", jobNames);
        }
        if (CollectionUtils.isNotEmpty(jobNormalizedFullNames)) {
            jobsTableCriteria.add("job_normalized_full_name IN ( :job_normalized_full_names )");
            params.put("job_normalized_full_names", jobNormalizedFullNames);
        }
        if (CollectionUtils.isNotEmpty(cicdInstanceIds)) {
            jobsTableCriteria.add("cicd_instance_id IN ( :cicd_instance_ids )");
            params.put("cicd_instance_ids", cicdInstanceIds.stream().map(UUID::fromString).collect(Collectors.toList()));
        }
        params.put("offset", pageNumber * pageSize);
        params.put("limit", pageSize);

        boolean needCicdJobsTable = CollectionUtils.isNotEmpty(jobNames) ||
                CollectionUtils.isNotEmpty(jobNormalizedFullNames) ||
                CollectionUtils.isNotEmpty(cicdInstanceIds);

        String jobsConditions = "";
        if (jobsTableCriteria.size() > 0)
            jobsConditions = " WHERE " + StringUtils.join(jobsTableCriteria, " AND ");

        String jobRunsConditions = "";
        if (jobRunsTableCriteria.size() > 0)
            jobRunsConditions = " WHERE " + StringUtils.join(jobRunsTableCriteria, " AND ");

        String jobsTableJoin = "";
        if (needCicdJobsTable) {
            jobsTableJoin = " INNER JOIN (SELECT job_name, job_normalized_full_name, cicd_instance_id, cicd_job_run_id "
                    + "FROM (SELECT id as cicd_job_run_id, cicd_job_id FROM " + company + "." + CICD_JOB_RUNS_TABLE + jobRunsConditions
                    + " ) jr INNER JOIN (SELECT id, job_name, job_normalized_full_name, cicd_instance_id FROM " + company + "."
                    + CICD_JOBS_TABLE + jobsConditions + ") j ON j.id = jr.cicd_job_id) jobs ON jobs.cicd_job_run_id = jht.job_run_id";
        }

        String conditions = "";
        if (criteria.size() > 0)
            conditions = " WHERE " + StringUtils.join(criteria, " AND ");
        String sql = "SELECT * FROM (SELECT * FROM " + company + "." + JENKINS_HIT_TABLE + " as jht"
                + jobsTableJoin
                + conditions
                + ") tbl  ORDER BY created_at DESC LIMIT :limit OFFSET :offset";
        String countSql = "SELECT COUNT(*) FROM ( SELECT * FROM " + company + "." + JENKINS_HIT_TABLE + " as jht"
                + jobsTableJoin
                + conditions
                + ") tbl ";
        List<TriageRuleHit> results = template.query(sql, params, jenkinsHitRowMapper(mapper));
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException("Deleting rule hits not allowed from this service.");
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + company + "." + JENKINS_HIT_TABLE + "(\n " +
                "    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    job_run_id    UUID NOT NULL REFERENCES " + company + ".cicd_job_runs(id) ON DELETE CASCADE,\n" +
                "    stage_id      UUID REFERENCES " + company + ".cicd_job_run_stages(id) ON DELETE CASCADE,\n" +
                "    step_id       UUID REFERENCES " + company + ".cicd_job_run_stage_steps(id) ON DELETE CASCADE,\n" +
                "    rule_id       UUID NOT NULL REFERENCES " + company + ".triage_rules(id) ON DELETE CASCADE,\n" +
                "    hit_content   VARCHAR NOT NULL,\n" +
                "    count         INTEGER NOT NULL,\n" +
                "    context       JSONB NOT NULL DEFAULT '{}',\n" +
                "    created_at    BIGINT DEFAULT extract(epoch from now())\n" +
                ")";
        String index1sql = "CREATE UNIQUE INDEX IF NOT EXISTS job_stage_step_rule_uniq ON "
                + company + "." + JENKINS_HIT_TABLE + "(job_run_id,stage_id,step_id,rule_id) WHERE stage_id IS NOT NULL";
        String index2sql = "CREATE UNIQUE INDEX IF NOT EXISTS job_rule_uniq ON "
                + company + "." + JENKINS_HIT_TABLE + "(job_run_id,rule_id) WHERE stage_id IS NULL";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             PreparedStatement pstmtIndex1sql = conn.prepareStatement(index1sql);
             PreparedStatement pstmtIndex2sql = conn.prepareStatement(index2sql)) {
            pstmt.execute();
            pstmtIndex1sql.execute();
            pstmtIndex2sql.execute();
            return true;
        }
    }
}
