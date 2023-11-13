package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.integrations.coverity.models.CoverityIterativeScanQuery;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Coverity's implementation of the {@link TriggerRunnable} for running {@link DbTrigger}
 */
@Log4j2
@Component
public class CoverityTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "CoverityIterativeScanController";
    private static final String COVERITY = "coverity";
    private static final int FULL_SCAN_FREQ_IN_DAYS = 30;

    private final ObjectMapper objectMapper;
    private final DefaultCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;

    public CoverityTrigger(ObjectMapper objectMapper, TriggerActionService triggerActionService) {
        this.objectMapper = objectMapper;
        this.triggerActionService = triggerActionService;
        this.cursorStrategy = new DefaultCursorStrategy(TimeUnit.DAYS.toMinutes(FULL_SCAN_FREQ_IN_DAYS));
    }

    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Template iterative scan: {}", trigger);
        CoverityTriggerMetadata metadata = parseMetadata(trigger);
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
                .query(CoverityIterativeScanQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(from)
                        .to(to)
                        .build())
                .build());
        CoverityTriggerMetadata updatedMetadata = CoverityTriggerMetadata.builder()
                .cursor(cursor.getTo())
                .lastFullScan(cursor.isPartial() ? metadata.getLastFullScan() : now)
                .lastIterativeScan(now)
                .build();
        triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
    }

    @Override
    public String getTriggerType() {
        return COVERITY;
    }

    private CoverityTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return CoverityTriggerMetadata.builder().build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), CoverityTriggerMetadata.class);
    }


    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CoverityTriggerMetadata.CoverityTriggerMetadataBuilder.class)
    public static class CoverityTriggerMetadata {

        @JsonProperty("cursor")
        Date cursor;

        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_iterative_scan")
        Date lastIterativeScan;
    }
}
