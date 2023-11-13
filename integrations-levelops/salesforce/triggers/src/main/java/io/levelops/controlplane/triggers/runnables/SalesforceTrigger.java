package io.levelops.controlplane.triggers.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.integrations.salesforce.models.SalesforceIngestionQuery;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Salesforce's implementation of the {@link TriggerRunnable} for running {@link DbTrigger}
 */
@Log4j2
@Component
public class SalesforceTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "SalesforceIngestionController";
    private static final String SALESFORCE = "salesforce";

    private static final int FULL_SCAN_FREQ_IN_DAYS = 7;

    private final ObjectMapper objectMapper;
    private final DefaultCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;

    public SalesforceTrigger(ObjectMapper objectMapper, TriggerActionService triggerActionService) {
        this.objectMapper = objectMapper;
        this.cursorStrategy = new DefaultCursorStrategy(TimeUnit.DAYS.toMinutes(FULL_SCAN_FREQ_IN_DAYS));
        this.triggerActionService = triggerActionService;
    }

    /**
     * Creates a job for the {@code trigger}. Also updates the {@link SalesforceTriggerMetadata} for the next trigger
     *
     * @param trigger {@link DbTrigger} containing information about the trigger
     * @throws Exception for any exception that may occur during job creation and metadata update.
     */
    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Salesfoce iterative scan: {}", trigger);
        SalesforceTriggerMetadata metadata = parseMetadata(trigger);
        Date now = new Date();
        DefaultCursorStrategy.DefaultCursor cursor =  cursorStrategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(metadata.getLastIterativeScan())
                .lastFullScan(metadata.getLastFullScan())
                .now(now)
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());
        Date from = cursor.isPartial() ? cursor.getFrom() : null;
        Date to = cursor.getTo();
        log.debug("run: scanning from: {} to to: {}, isFullScan: {}", from, to, !cursor.isPartial());
        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .tags(cursor.getTags())
                .query(SalesforceIngestionQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(from != null ? from.getTime() : null)
                        .to(to != null ? to.getTime() : null)
                        .partial(cursor.isPartial())
                        .build())
                .build());
        SalesforceTriggerMetadata updateMetadata = SalesforceTriggerMetadata.builder()
                .lastFullScan(cursor.isPartial() ? metadata.getLastFullScan() : now)
                .lastIterativeScan(now)
                .build();
        triggerActionService.updateTriggerMetadata(trigger.getId(), updateMetadata);
    }

    /**
     * parses the metadata from {@link DbTrigger#getMetadata()} to {@link SalesforceTriggerMetadata}
     *
     * @param trigger {@link DbTrigger} containing information about the trigger
     * @return the parsed {@link SalesforceTriggerMetadata}
     */
    private SalesforceTriggerMetadata parseMetadata(DbTrigger trigger) {
        if(trigger.getMetadata() == null) {
            return SalesforceTriggerMetadata.builder().build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), SalesforceTriggerMetadata.class);
    }

    @Override
    public String getTriggerType() {
        return SALESFORCE;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SalesforceTriggerMetadata.SalesforceTriggerMetadataBuilder.class)
    public static class SalesforceTriggerMetadata {
        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_iterative_scan")
        Date lastIterativeScan;
    }
}
