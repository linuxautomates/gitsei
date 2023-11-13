package io.levelops.commons.databases.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.cicd_scm.DBDummyObj;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
public class WorkItemFailureTriageViewService extends DatabaseService<DBDummyObj> {
    // region CSTOR
    @Autowired
    public WorkItemFailureTriageViewService(DataSource dataSource) {
        super(dataSource);
    }
    // endregion

    // region Unsupported Operations
    @Override
    public String insert(String company, DBDummyObj t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean update(String company, DBDummyObj t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DBDummyObj> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DbListResponse<DBDummyObj> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        return true;
    }
    // endregion

    //FailureTriageSlackMessage
    public List<WIFailureTriageView> getFailureTriageForWorkItem(final String company, final UUID workItemId) throws SQLException {
        String baseQueryFormat = "select distinct(jh.id) as hit_id, i.name as instance_name, i.url as instance_url, j.id as job_id, j.job_name as job_name, j.job_full_name as job_full_name, r.id as run_id, r.job_run_number, " +
                "jh.stage_id as stage_id, stg.name as stage_name, jh.rule_id as rule_id, tr.name as rule_name, jh.count, " +
                "r.start_time as run_start, stg.start_time as stage_start, stg.url as stage_url, jh.hit_content\n" +
                "from %s.jenkins_rule_hits as jh\n" +
                "join %s.workitem_cicd_mappings as m on ((m.cicd_job_run_id = jh.job_run_id AND m.cicd_job_run_stage_id IS NOT NULL AND m.cicd_job_run_stage_id = jh.stage_id) OR (m.cicd_job_run_id = jh.job_run_id AND m.cicd_job_run_stage_id IS NULL))\n" +
                "join %s.triage_rules as tr on tr.id = jh.rule_id\n" +
                "join %s.cicd_job_runs as r on r.id = jh.job_run_id\n" +
                "join %s.cicd_jobs as j on j.id = r.cicd_job_id\n" +
                "join %s.cicd_instances as i on i.id = j.cicd_instance_id\n" +
                "left outer join %s.cicd_job_run_stages as stg on stg.id = jh.stage_id\n" +
                "where m.work_item_id = ?\n" +
                "order by run_start, stage_start";
        String baseQuery = String.format(baseQueryFormat, company, company, company, company, company, company, company);
        List<WIFailureTriageView> retval = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(baseQuery)) {
            pstmt.setObject(1, workItemId);
            log.debug("Get or List Query = {}", pstmt);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String instanceName = rs.getString("instance_name");
                UUID jobId = (UUID) rs.getObject("job_id");
                String jobName = rs.getString("job_name");

                UUID runId = (UUID) rs.getObject("run_id");
                Long jobRunNumber = rs.getLong("job_run_number");

                UUID stageId =(rs.getObject("stage_id") != null) ? (UUID) rs.getObject("stage_id"): null;
                String stageName =(rs.getString("stage_name") != null) ? rs.getString("stage_name"): null;

                UUID ruleId = (UUID) rs.getObject("rule_id");
                String ruleName = rs.getString("rule_name");

                Integer hitsCount = rs.getInt("count");
                String snippet = rs.getString("hit_content");
                String stageUrl = rs.getString("stage_url");

                Instant runStartTime = DateUtils.toInstant(rs.getTimestamp("run_start"));
                Instant stageStartTime = Instant.ofEpochMilli(rs.getLong("stage_start"));

                String instanceUrl = rs.getString("instance_url");
                String jobFullName = rs.getString("job_full_name");

                WIFailureTriageView wiFailureTriageView = WIFailureTriageView.builder()
                        .instanceName(instanceName)
                        .jobId(jobId).jobName(jobName)
                        .jobRunId(runId).jobRunNumber(jobRunNumber)
                        .stageId(stageId).stageName(stageName)
                        .ruleId(ruleId).ruleName(ruleName)
                        .hitsCount(hitsCount).snippet(snippet).stageUrl(stageUrl)
                        .runStartTime(runStartTime).stageStartTime(stageStartTime)
                        .jobUrl(CiCdJobsDatabaseService.getFullUrl(company, instanceUrl, jobFullName, jobRunNumber.intValue(), "", ""))
                        .build();

                retval.add(wiFailureTriageView);
            }
        }
        return retval;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = WIFailureTriageView.WIFailureTriageViewBuilder.class)
    public static class WIFailureTriageView {
        @JsonProperty("instance_name")
        String instanceName;
        @JsonProperty("job_id")
        UUID jobId;
        @JsonProperty("job_name")
        String jobName;
        @JsonProperty("job_run_id")
        UUID jobRunId;
        @JsonProperty("job_run_number")
        Long jobRunNumber;
        @JsonProperty("job_url")
        String jobUrl;
        @JsonProperty("stage_id")
        UUID stageId;
        @JsonProperty("stage_name")
        String stageName;
        @JsonProperty("rule_id")
        UUID ruleId;
        @JsonProperty("rule_name")
        String ruleName;
        @JsonProperty("hits_count")
        Integer hitsCount;
        @JsonProperty("run_start_time")
        Instant runStartTime;
        @JsonProperty("stage_start_time")
        Instant stageStartTime;
        @JsonProperty("stage_url")
        String stageUrl;
        @JsonProperty("snippet")
        String snippet;
    }
}
