package io.levelops.commons.databases.models.database.cicd;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = FailureTriageSlackMessage.FailureTriageSlackMessageBuilder.class)
public class FailureTriageSlackMessage {
    @JsonProperty("hide_change_ticket_status")
    private final Boolean hideChangeTicketStatus;
    @JsonProperty("hide_change_ticket_assignee")
    private final Boolean hideChangeTicketAssignee;

    @JsonProperty("job_runs")
    private final List<JobRun> jobRuns;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = JobRun.JobRunBuilder.class)
    public static class JobRun {
        @JsonProperty("job_run_id")
        private final UUID jobRunId;
        @JsonProperty("job_name")
        private final String jobName;
        @JsonProperty("job_run_number")
        private final Long jobRunNumber;
        @JsonProperty("jenkins_instance_name")
        private final String jenkinsInstanceName;
        @JsonProperty("jenkins_url")
        private final String jenkinsUrl;
        @JsonProperty("level_ops_url")
        private final String levelOpsUrl;

        @JsonProperty("rule_hits")
        List<RuleHit> ruleHits;

        @JsonProperty("stages")
        private final List<Stage> stages;

        @JsonProperty("run_start_time")
        Instant runStartTime;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = JobRun.JobRunBuilder.class)
    public static class RuleHit {
        @JsonProperty("rule_id")
        private final UUID ruleId;
        @JsonProperty("rule")
        private final String rule;
        @JsonProperty("matches_count")
        private final Integer matchesCount;
        @JsonProperty("snippet")
        private final String snippet;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Stage.StageBuilder.class)
    public static class Stage {
        @JsonProperty("stage_id")
        private final UUID stageId;
        @JsonProperty("stage_name")
        private final String stageName;
        @JsonProperty("stage_jenkins_url")
        private final String stageJenkinsUrl;

        @JsonProperty("rule_hits")
        List<RuleHit> ruleHits;

        @JsonProperty("stage_start_time")
        Instant stageStartTime;
    }
}
