package io.levelops.controlplane.triggers.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.integrations.tenable.models.TenableScanQuery;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Tenable's implementation of the {@link TriggerRunnable} for running {@link DbTrigger}
 */
@Log4j2
@Component
public class TenableTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "TenableIngestionController";
    private static final String TENABLE = "tenable";

    private static final int FULL_SCAN_FREQ_IN_DAYS = 7;

    private final ObjectMapper objectMapper;
    private final DefaultCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;

    public TenableTrigger(ObjectMapper objectMapper, TriggerActionService triggerActionService) {
        this.objectMapper = objectMapper;
        this.cursorStrategy = new DefaultCursorStrategy(TimeUnit.DAYS.toMinutes(FULL_SCAN_FREQ_IN_DAYS));
        this.triggerActionService = triggerActionService;
    }

    /**
     * Creates a job for the {@code trigger}. Also updates the {@link TenableTriggerMetadata} for the next trigger
     *
     * @param trigger {@link DbTrigger} containing information about the trigger
     * @throws Exception for any exception that may occur during job creation and metadata update.
     */
    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Template iterative scan: {}", trigger);
        TenableTriggerMetadata metadata = parseMetadata(trigger);
        Date now = new Date();
        DefaultCursorStrategy.DefaultCursor cursor =  cursorStrategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(metadata.getLastIterativeScan())
                .lastFullScan(metadata.getLastFullScan())
                .now(now)
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());
        Date from = cursor.isPartial() ? cursor.getFrom() : null;
        Long since;
        if(from != null) {
            since = TimeUnit.MILLISECONDS.toSeconds(from.getTime());
        } else {
            since = null;
        }
        log.debug("run: scanning from: {}, isFullScan: {}", from, !cursor.isPartial());
        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .tags(cursor.getTags())
                .query(TenableScanQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .since(since)
                        .partial(cursor.isPartial())
                        .build())
                .build());
        TenableTriggerMetadata updateMetadata = TenableTriggerMetadata.builder()
                .lastFullScan(cursor.isPartial() ? metadata.getLastFullScan() : now)
                .lastIterativeScan(now)
                .build();
        triggerActionService.updateTriggerMetadata(trigger.getId(), updateMetadata);
    }

    @Override
    public String getTriggerType() {
        return TENABLE;
    }

    /**
     * parses the metadata from {@link DbTrigger#getMetadata()} to {@link TenableTriggerMetadata}
     *
     * @param trigger {@link DbTrigger} containing information about the trigger
     * @return the parsed {@link TenableTriggerMetadata}
     */
    private TenableTriggerMetadata parseMetadata(DbTrigger trigger) {
        if(trigger.getMetadata() == null) {
            return TenableTriggerMetadata.builder().build();
        }

        return objectMapper.convertValue(trigger.getMetadata(), TenableTriggerMetadata.class);
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = TenableTriggerMetadata.TenableTriggerMetadataBuilder.class)
    public static class TenableTriggerMetadata {
        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_iterative_scan")
        Date lastIterativeScan;
    }
}
