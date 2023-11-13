package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.utils.MapUtils;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.models.DbTriggerSettings;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.IterativeBackwardScanCursorStrategy;
import io.levelops.controlplane.trigger.strategies.IterativeBackwardScanCursorStrategy.IterativeBackwardScanCursor;
import io.levelops.controlplane.trigger.strategies.IterativeBackwardScanCursorStrategy.IterativeBackwardScanCursorMetadata;
import io.levelops.ingestion.integrations.jira.models.JiraIterativeScanQuery;
import io.levelops.ingestion.models.CreateJobRequest;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component
public class JiraTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "JiraIterativeScanController";
    private static final long DEFAULT_FULL_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(7);
    private static final long DEFAULT_ONBOARDING_SPAN_IN_DAYS = 365;
    private static final long DEFAULT_BACKWARD_SCAN_SUB_JOB_SPAN_IN_MIN = TimeUnit.DAYS.toMinutes(31);
    private static final long DEFAULT_PROJECT_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(1);
    private static final long DEFAULT_SPRINT_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(1);

    private final ObjectMapper objectMapper;
    private final TriggerActionService triggerActionService;
    private final Long projectScanFreqInMin;
    private final Long sprintScanFreqInMin;

    private final Long fullScanFreqInMin;
    private final Long onboardingSpanInDays;
    private final Long backwardScanSubJobSpanInMin;

    @Autowired
    public JiraTrigger(ObjectMapper objectMapper,
                       TriggerActionService triggerActionService,
                       @Value("${JIRA__FULL_SCAN_FREQ_IN_MIN:}") Long fullScanFreqInMin,
                       @Value("${JIRA__ONBOARDING_SPAN_IN_DAYS:}") Long onboardingSpanInDays,
                       @Value("${JIRA__BACKWARD_SCAN_SUB_JOB_SPAN_IN_MIN:}") Long backwardScanSubJobSpanInMin,
                       @Value("${JIRA__PROJECT_SCAN_FREQ_IN_MIN:}") Long projectScanFreqInMin,
                       @Value("${JIRA__SPRINT_SCAN_FREQ_IN_MIN:}") Long sprintScanFreqInMin) {
        this.objectMapper = objectMapper;
        this.triggerActionService = triggerActionService;
        this.projectScanFreqInMin = ObjectUtils.firstNonNull(projectScanFreqInMin, DEFAULT_PROJECT_SCAN_FREQ_IN_MIN);
        this.sprintScanFreqInMin = ObjectUtils.firstNonNull(sprintScanFreqInMin, DEFAULT_SPRINT_SCAN_FREQ_IN_MIN);

        this.fullScanFreqInMin = ObjectUtils.firstNonNull(fullScanFreqInMin, DEFAULT_FULL_SCAN_FREQ_IN_MIN);
        this.onboardingSpanInDays = ObjectUtils.firstNonNull(onboardingSpanInDays, DEFAULT_ONBOARDING_SPAN_IN_DAYS);
        this.backwardScanSubJobSpanInMin = ObjectUtils.firstNonNull(backwardScanSubJobSpanInMin, DEFAULT_BACKWARD_SCAN_SUB_JOB_SPAN_IN_MIN);
        log.info("Configured Jira Trigger: fullScanFreqInMin={}, onboardingSpanInDays={}, backwardScanSubJobSpanInMin={}, projectScanFreqInMin={}, sprintScanFreqInMin={}",
                this.fullScanFreqInMin, this.onboardingSpanInDays, this.backwardScanSubJobSpanInMin, this.projectScanFreqInMin, this.sprintScanFreqInMin);
    }

    @Override
    public String getTriggerType() {
        return "jira";
    }

    private JiraTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return JiraTriggerMetadata.builder()
                    .build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), JiraTriggerMetadata.class);
    }

    private IterativeBackwardScanCursorStrategy getCursorStrategy(DbTrigger trigger) {
        Long currentBackwardScanSubJobSpanInMin = backwardScanSubJobSpanInMin;
        Long currentOnboardingSpanInDays = onboardingSpanInDays;
        DbTriggerSettings settings = trigger.getSettings();
        if (settings != null) {
            if (settings.getBackwardScanSubjobSpanInMinutes() != null) {
                currentBackwardScanSubJobSpanInMin = settings.getBackwardScanSubjobSpanInMinutes();
                log.debug("Backward scan sub job span for trigger {} set to {}", trigger.getId(), currentBackwardScanSubJobSpanInMin);
            }
            if (settings.getOnboardingSpanInDays() != null) {
                currentOnboardingSpanInDays = settings.getOnboardingSpanInDays();
                log.debug("Onboarding span for trigger {} set to {}", trigger.getId(), currentOnboardingSpanInDays);
            }
        }
        log.debug("Configured Jira Trigger {}: fullScanFreqInMin={}, onboardingSpanInDays={}, backwardScanSubJobSpanInMin={}, projectScanFreqInMin={}, sprintScanFreqInMin={}",
                trigger.getId(), this.fullScanFreqInMin, currentOnboardingSpanInDays, currentBackwardScanSubJobSpanInMin, this.projectScanFreqInMin, this.sprintScanFreqInMin);
        return new IterativeBackwardScanCursorStrategy(fullScanFreqInMin, currentOnboardingSpanInDays, currentBackwardScanSubJobSpanInMin);
    }

    @Override
    public void run(DbTrigger trigger) throws Exception {
        JiraTriggerMetadata metadata = parseMetadata(trigger);

        Date now = new Date();
        IterativeBackwardScanCursorStrategy cursorStrategy = getCursorStrategy(trigger);
        IterativeBackwardScanCursor cursor = cursorStrategy.getNextCursor(IterativeBackwardScanCursorMetadata.builder()
                .currentForwardCursor(metadata.getCursor())
                .currentBackwardCursor(metadata.getBackwardCursor())
                .lastScanType(metadata.getLastScanType())
                .lastFullScan(metadata.getLastFullScan())
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .now(now)
                .build());
        boolean shouldFetchProjects = shouldFetchProjects(metadata.getLastProjectScan(), now);
        boolean shouldFetchSprints = shouldFetchSprints(metadata.getLastSprintScan(), now);
        Integer issuesPageSize = getPageSize(trigger, "jira_issues");

        log.debug("Triggering Jira iterative scan: shouldFetchProjects={} ({} - {}), shouldFetchSprints={} ({} - {})",
                shouldFetchProjects, metadata.getLastProjectScan(), now, shouldFetchSprints, metadata.getLastSprintScan(), now);

        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .tags(cursor.getTags())
                .query(JiraIterativeScanQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(cursor.getFrom())
                        .to(cursor.getTo())
                        .fetchProjects(shouldFetchProjects)
                        .fetchSprints(shouldFetchSprints)
                        .issuesPageSize(issuesPageSize)
                        .build())
                .build());

        JiraTriggerMetadata updatedMetadata = JiraTriggerMetadata.builder()
                .cursor(cursor.getForwardCursor())
                .backwardCursor(cursor.getBackwardCursor())
                .lastScanType(cursor.getScanType())
                .lastFullScan(cursor.getLastFullScan())
                .lastProjectScan(shouldFetchProjects ? now : metadata.getLastProjectScan())
                .lastSprintScan(shouldFetchSprints ? now :metadata.getLastSprintScan())
                .build();

        triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
    }

    public Integer getPageSize(DbTrigger trigger, String key) {
        if (trigger == null || trigger.getSettings() == null) {
            return null;
        }
        DbTriggerSettings settings = trigger.getSettings();
        return MapUtils.emptyIfNull(settings.getDataSourcePageSizes()).get(key);
    }

    public boolean shouldFetchProjects(Date lastProjectScan, Date now) {
        return lastProjectScan == null || lastProjectScan.toInstant().isBefore(
                now.toInstant().minus(projectScanFreqInMin, ChronoUnit.MINUTES));
    }

    public boolean shouldFetchSprints(Date lastSprintScan, Date now) {
        return lastSprintScan == null || lastSprintScan.toInstant().isBefore(
                now.toInstant().minus(sprintScanFreqInMin, ChronoUnit.MINUTES));
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = JiraTriggerMetadata.JiraTriggerMetadataBuilder.class)
    public static class JiraTriggerMetadata {

        @JsonProperty("cursor")
        Date cursor;

        @JsonProperty("backward_cursor")
        Date backwardCursor;

        @JsonProperty("last_scan_type")
        String lastScanType;

        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_project_scan")
        Date lastProjectScan;

        @JsonProperty("last_sprint_scan")
        Date lastSprintScan;
    }
}
