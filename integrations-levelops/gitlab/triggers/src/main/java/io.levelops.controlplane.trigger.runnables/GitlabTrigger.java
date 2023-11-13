package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.integrations.gitlab.models.GitlabIterativeScanQuery;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Gitlab's implementation of the {@link TriggerRunnable} for running {@link DbTrigger}
 */
@Log4j2
@Component
public class GitlabTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "GitlabIterativeScanController";
    private static final String GITLAB = "gitlab";
    private static final int FULL_SCAN_FREQ_IN_DAYS = 14;
    private static final long ONBOARDING_SPAN_IN_DAYS = 14;
    private static final Long GROUP_SCAN_FREQ_IN_DAYS = TimeUnit.DAYS.toDays(1);
    private static final Long PROJECTS_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(1);

    private final ObjectMapper objectMapper;
    private final DefaultCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;

    public GitlabTrigger(ObjectMapper objectMapper, TriggerActionService triggerActionService) {
        this.objectMapper = objectMapper;
        this.triggerActionService = triggerActionService;
        this.cursorStrategy = new DefaultCursorStrategy(TimeUnit.DAYS.toMinutes(FULL_SCAN_FREQ_IN_DAYS), ONBOARDING_SPAN_IN_DAYS);
    }

    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Template iterative scan: {}", trigger);
        GitlabTriggerMetadata metadata = parseMetadata(trigger);
        Date now = new Date();
        DefaultCursorStrategy.DefaultCursor cursor =  cursorStrategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(metadata.getLastIterativeScan())
                .lastFullScan(metadata.getLastFullScan())
                .now(now)
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());
        Date from = cursor.getFrom();
        Date to = cursor.getTo();
        log.debug("run: scanning tickets from: {}, isFullScan: {}", from, !cursor.isPartial());
        boolean shouldFetchGroups = shouldFetchGroups(metadata.getLastIterativeScan(), now);
        boolean shouldFetchProjects = shouldFetchProjects(metadata.getLastIterativeScan(), now);
        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .tags(cursor.getTags())
                .query(GitlabIterativeScanQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(from)
                        .to(to)
                        .shouldFetchGroups(shouldFetchGroups)
                        .shouldFetchProjects(shouldFetchProjects)
                        .shouldFetchAllUsers(!cursor.isPartial())
                        .build())
                .build());
        GitlabTriggerMetadata updatedMetadata = GitlabTriggerMetadata.builder()
                .cursor(cursor.getTo())
                .lastFullScan(cursor.isPartial() ? metadata.getLastFullScan() : now)
                .lastIterativeScan(now)
                .build();
        triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
    }

    public boolean shouldFetchGroups(Date lastIterativeScan, Date now) {
        return lastIterativeScan == null || lastIterativeScan.toInstant().isBefore(
                now.toInstant().minus(GROUP_SCAN_FREQ_IN_DAYS, ChronoUnit.DAYS));
    }

    public boolean shouldFetchProjects(Date lastRepoScan, Date now) {
        return lastRepoScan == null || lastRepoScan.toInstant().isBefore(
                now.toInstant().minus(PROJECTS_SCAN_FREQ_IN_MIN, ChronoUnit.MINUTES));
    }

    private GitlabTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return GitlabTriggerMetadata.builder().build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), GitlabTriggerMetadata.class);
    }

    @Override
    public String getTriggerType() {
        return GITLAB;
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GitlabTriggerMetadata.GitlabTriggerMetadataBuilder.class)
    public static class GitlabTriggerMetadata {

        @JsonProperty("cursor")
        Date cursor;

        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_iterative_scan")
        Date lastIterativeScan;
    }
}

