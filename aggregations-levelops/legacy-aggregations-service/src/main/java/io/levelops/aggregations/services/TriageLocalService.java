package io.levelops.aggregations.services;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.common.base.MoreObjects;
import io.levelops.cicd.services.CiCdService;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.TriageRule;
import io.levelops.commons.databases.models.database.TriageRuleHit;
import io.levelops.commons.databases.models.database.TriageRuleHit.RuleHitType;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.PathSegment;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.TriageRuleHitsService;
import io.levelops.commons.databases.services.TriageRulesService;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.regex.RegexService;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.models.EventsClientException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class TriageLocalService {

    private static final String DEFAULT_JOB_NAME = "Unknown Job";
    private static final String DEFAULT_INSTANCE_NAME = "Unknown Instance";
    private final static List<String> FAILED_STATES = List.of("ABORTED", "FAILURE");
    private final Storage storage;
    private final TriageRulesService triageRulesService;
    private final TriageRuleHitsService triageRuleHitsService;
    private final RegexService regexService;
    private final String jenkinsLogsBucket;
    private final EventsClient eventsClient;
    private final CiCdJobRunStageDatabaseService jobRunStageDatabaseService;
    private final RedisConnectionFactory redisConnectionFactory;
    private final CiCdService ciCdService;

    @Autowired
    public TriageLocalService(
            @Value("${CICD_JOB_RUNS_DETAILS_BUCKET}") final String jenkinsLogsBucket,
            final TriageRulesService triageRulesService,
            final TriageRuleHitsService triageRuleHitsService,
            final RegexService regexService,
            final Storage storage,
            final EventsClient eventsClient,
            final CiCdJobRunStageDatabaseService jobRunStageDatabaseService,
            final RedisConnectionFactory redisConnectionFactory,
            CiCdService ciCdService) {
        this.triageRulesService = triageRulesService;
        this.triageRuleHitsService = triageRuleHitsService;
        this.regexService = regexService;
        this.storage = storage;
        this.jenkinsLogsBucket = jenkinsLogsBucket;
        this.eventsClient = eventsClient;
        this.jobRunStageDatabaseService = jobRunStageDatabaseService;
        this.redisConnectionFactory = redisConnectionFactory;
        this.ciCdService = ciCdService;
    }

    public void analyzeJenkinsGCSLogs(
            @Nonnull final String company,
            @Nonnull final String instanceId,
            @Nonnull final String instanceName,
            @Nonnull final String jobId,
            @Nonnull final String jobRunId,
            @Nonnull final String jobName,
            @Nonnull final String jobStatus,
            final String stageId,
            final String stepId,
            @Nonnull final String gcsLogsLocation,
            @Nonnull final String logBucket,
            final String url) throws IOException {
        log.debug("analyzing: {}", gcsLogsLocation);
        String logsContents = new String(storage.readAllBytes(BlobId.of(MoreObjects.firstNonNull(logBucket, jenkinsLogsBucket), gcsLogsLocation)), StandardCharsets.UTF_8);
        analyzeJenkinsLogsContents(company, instanceId, instanceName, jobId, jobRunId, jobName, jobStatus, stageId, stepId, logsContents, url);
    }

    public void analyzeJenkinsLogsContents(
            @Nonnull final String company,
            @Nonnull final String instanceId,
            @Nonnull final String instanceName,
            @Nonnull final String jobId,
            @Nonnull final String jobRunId,
            @Nonnull final String jobName,
            @Nonnull final String jobStatus,
            final String stageId,
            final String stepId,
            @Nonnull final String logsContents,
            final String url) throws IOException {
        try {
            long numberOfRulesHit = getRulesForJob(company, jobId)
                    .filter(rule -> rule != null)
                    .map(rule -> {
                        log.debug("Running regex: name={}, regex={}", rule.getName(), rule.getRegexes());
                        var ruleHit = regexService.findRegexHits(new HashSet<>(rule.getRegexes()), logsContents);
                        if (ruleHit == null || ruleHit.getTotalMatches() == null || ruleHit.getTotalMatches() < 1) {
                            log.debug("no match..");
                            return null;
                        }
                        try {
                            log.debug("Insering match results...");
                            triageRuleHitsService.insert(company, TriageRuleHit.builder()
                                    .count(ruleHit.getTotalMatches())
                                    .hitContent(ruleHit.getFirstHitContext())
                                    .context(Map.of(
                                            "line", ruleHit.getFirstHitLineNumber(),
                                            "step", StringUtils.defaultString(stepId)
                                    ))
                                    .jobRunId(jobRunId)
                                    .ruleId(rule.getId())
                                    .stageId(stageId)
                                    .stepId(stepId)
                                    .type(RuleHitType.JENKINS)
                                    .build()
                            );
                        } catch (SQLException e) {
                            log.error("Unable to persist rule results: {}", ruleHit, e);
                        }
                        return ruleHit;
                    })
                    .filter(Objects::nonNull)
                    .count();
            if (numberOfRulesHit > 0) {
                log.info("Jenkins job id={} has hit {} triage rules; emitting jenkins_triage_rules_hit event", jobId, numberOfRulesHit);
                emitJenkinsTriageRulesHitEvent(company, instanceName, jobId, jobName, jobRunId, stageId, url);
            }
            if (isTheLastMessage(company, instanceId, jobRunId)) {
                EventType event = EventType.JENKINS_JOB_RUN_COMPLETED;
                if (FAILED_STATES.stream().anyMatch(state -> state.equalsIgnoreCase(jobStatus))) {
                    event = EventType.JENKINS_JOB_RUN_FAILED;
                }
                eventsClient.emitEvent(company, event, Map.of(
                        "id", jobId,
                        "job_name", jobName,
                        "job_run_id", jobRunId,
                        "instance_id", instanceId,
                        "instance_name", StringUtils.defaultString(instanceName),
                        "parent_job_names", getParentJobNames(company, jobRunId, null)
                ));
            }
        } catch (RuntimeStreamException e) {
            throw new IOException(String.format("[%s] Unable to get triage rules for the job: %s", company, jobRunId), e);
        } catch (EventsClientException e) {
            log.error("Unable to send the event for: instanceId={}, jobId={}, jobName={}, runId={}, jobStatus={}", instanceId, jobId, jobName, jobRunId, jobStatus, e);
        }
    }

    /**
     * Distributed check
     *
     * @param company
     * @param instanceId
     * @param jobRunId
     * @return
     */
    private boolean isTheLastMessage(String company, String instanceId, String jobRunId) {
        var result = false;
        var id = String.format("%s_%s", company, (instanceId + jobRunId));
        try (var redis = redisConnectionFactory.getConnection()) {
            var key = id.getBytes();
            var count = redis.incrBy(key, -1L);
            result = count < 1;
            if (!result) {
                return false;
            }
            log.debug("No more pending jobs for '{}'. deleting...", id);
            redis.del(key);
            return true;
        } catch (Exception e) {
            log.warn("Exception while checking for the last message in redis for the key {}...", id, e);
            return result;
        }
    }

    private void emitJenkinsTriageRulesHitEvent(String company, String instanceName, String jobId,
                                                final String jobName, String jobRunId,
                                                @Nullable String stageId, final String url) {
        String stageName = null;
        if (StringUtils.isNotEmpty(stageId)) {
            try {
                stageName = jobRunStageDatabaseService.get(company, stageId).map(JobRunStage::getName).orElse("");
            } catch (SQLException e) {
                log.warn("Failed to retrieve metadata for jenkins_triage_rules_hit event for job_id={} (still sending event...)", jobId, e);
            }
        }

        try {
            eventsClient.emitEvent(company, EventType.JENKINS_TRIAGE_RULES_MATCHED, Map.of(
                    "job_run_id", StringUtils.defaultString(jobRunId),
                    "stage_id", StringUtils.defaultString(stageId),
                    "instance_name", StringUtils.defaultString(instanceName, DEFAULT_INSTANCE_NAME),
                    "job_name", StringUtils.defaultString(jobName, DEFAULT_JOB_NAME),
                    "jenkins_url", StringUtils.defaultString(url),
                    "stage_name", StringUtils.defaultString(stageName), // can be empty
                    "parent_job_names", getParentJobNames(company, jobRunId, stageId)
            ));
        } catch (EventsClientException e) {
            log.error("Failed to emit jenkins_triage_rules_hit event for job_id={}", jobId, e);
        }
    }

    private List<String> getParentJobNames(String company, String jobRunId, @Nullable String stageId) {
        Set<PathSegment> fullPath = null;
        if (StringUtils.isNotBlank(stageId)) {
            try {
                fullPath = ciCdService.getJobStageFullPath(company, stageId, false);
            } catch (Exception e) {
                log.warn("Failed to generate full path for jobRunId={}, stageId={}", jobRunId, stageId, e);
            }
        } else {
            try {
                fullPath = ciCdService.getJobRunFullPath(company, jobRunId, false);
            } catch (Exception e) {
                log.warn("Failed to generate full path for jobRunId={}", jobRunId, e);
            }
        }
        if (fullPath == null) {
            return Collections.emptyList();
        }
        return PathSegment.toStream(fullPath, false)
                .map(PathSegment::getName)
                .collect(Collectors.toList());
    }

    public Stream<TriageRule> getRulesForJob(final String company, final String jobId) throws RuntimeStreamException {
        return PaginationUtils.stream(0, 1, RuntimeStreamException.wrap(page ->
                triageRulesService.list(company, null, null, null, null,
                        page, 200).getRecords()));
    }
}