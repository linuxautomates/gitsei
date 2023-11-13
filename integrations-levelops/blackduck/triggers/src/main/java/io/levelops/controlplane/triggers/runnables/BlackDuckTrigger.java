package io.levelops.controlplane.triggers.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.integrations.blackduck.models.BlackDuckIterativeScanQuery;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component
public class BlackDuckTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "BlackDuckIterativeScanController";
    private static final String BLACKDUCK = "blackduck";
    private static final int FULL_SCAN_FREQ_IN_DAYS = 7;
    private static final Long PROJECTS_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(1);

    private final ObjectMapper objectMapper;
    private final DefaultCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;

    public BlackDuckTrigger(ObjectMapper objectMapper, TriggerActionService triggerActionService) {
        this.objectMapper = objectMapper;
        this.triggerActionService = triggerActionService;
        this.cursorStrategy = new DefaultCursorStrategy(TimeUnit.DAYS.toMinutes(FULL_SCAN_FREQ_IN_DAYS));
    }

    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Template iterative scan: {}", trigger);
        BlackDuckTriggerMetadata metadata = parseMetadata(trigger);
        Date now = new Date();
        DefaultCursorStrategy.DefaultCursor cursor = cursorStrategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(metadata.getLastIterativeScan())
                .lastFullScan(metadata.getLastFullScan())
                .now(now)
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());
        Date from = cursor.isPartial() ? cursor.getFrom() : null;
        Date to = cursor.getTo();
        log.debug("run: scanning projects from: {}, isFullScan: {}", from, !cursor.isPartial());
        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .tags(cursor.getTags())
                .query(BlackDuckIterativeScanQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(from)
                        .to(to)
                        .build())
                .build());
        BlackDuckTriggerMetadata updatedMetadata = BlackDuckTriggerMetadata.builder()
                .cursor(cursor.getTo())
                .lastFullScan(cursor.isPartial() ? metadata.getLastFullScan() : now)
                .lastIterativeScan(now)
                .build();
        triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
    }

    @Override
    public String getTriggerType() {
        return BLACKDUCK;
    }

    public boolean shouldFetch(Date lastRepoScan, Date now) {
        return lastRepoScan == null || lastRepoScan.toInstant().isBefore(
                now.toInstant().minus(PROJECTS_SCAN_FREQ_IN_MIN, ChronoUnit.MINUTES));
    }

    private BlackDuckTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return BlackDuckTriggerMetadata.builder().build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), BlackDuckTriggerMetadata.class);
    }


    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BlackDuckTriggerMetadata.BlackDuckTriggerMetadataBuilder.class)
    public static class BlackDuckTriggerMetadata {

        @JsonProperty("cursor")
        Date cursor;

        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_iterative_scan")
        Date lastIterativeScan;
    }
}
