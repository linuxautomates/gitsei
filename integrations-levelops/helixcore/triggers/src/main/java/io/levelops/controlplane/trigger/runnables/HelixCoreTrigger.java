package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.integrations.helixcore.models.HelixCoreIterativeQuery;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component
public class HelixCoreTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "HelixCoreIterativeScanController";
    private static final String HELIX_CORE = "helixcore";

    private static final int FULL_SCAN_FREQ_IN_DAYS = 7;

    private final ObjectMapper objectMapper;
    private final DefaultCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;

    public HelixCoreTrigger(ObjectMapper objectMapper, TriggerActionService triggerActionService) {
        this.objectMapper = objectMapper;
        this.cursorStrategy = new DefaultCursorStrategy(TimeUnit.DAYS.toMinutes(FULL_SCAN_FREQ_IN_DAYS));
        this.triggerActionService = triggerActionService;
    }

    /**
     * Creates a job for the {@code trigger}. Also updates the {@link HelixcoreTriggerMetadata} for the next trigger
     *
     * @param trigger {@link DbTrigger} containing information about the trigger
     * @throws Exception for any exception that may occur during job creation and metadata updation
     */
    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Template iterative scan: {}", trigger);
        HelixcoreTriggerMetadata metadata = parseMetadata(trigger);
        Date now = new Date();
        DefaultCursorStrategy.DefaultCursor cursor =  cursorStrategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(metadata.getLastIterativeScan())
                .lastFullScan(metadata.getLastFullScan())
                .now(now)
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());
        log.debug("run: scanning entities from: {}, isFullScan: {}", cursor.getFrom(), !cursor.isPartial());
        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder().tags(cursor.getTags())
                .controllerName(CONTROLLER_NAME)
                .query(HelixCoreIterativeQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(cursor.getFrom())
                        .to(cursor.getTo())
                        .build())
                .build());
        HelixcoreTriggerMetadata updatedMetadata = HelixcoreTriggerMetadata.builder()
                .cursor(cursor.getTo())
                .lastFullScan(cursor.isPartial() ? metadata.getLastFullScan() : now)
                .lastIterativeScan(now)
                .build();
        triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
    }

    private HelixcoreTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return HelixcoreTriggerMetadata.builder().build();
        }

        return objectMapper.convertValue(trigger.getMetadata(), HelixcoreTriggerMetadata.class);
    }

    @Override
    public String getTriggerType() {
        return HELIX_CORE;
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = HelixcoreTriggerMetadata.HelixcoreTriggerMetadataBuilder.class)
    public static class HelixcoreTriggerMetadata {

        @JsonProperty("cursor")
        Date cursor;

        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_iterative_scan")
        Date lastIterativeScan;

    }
}
