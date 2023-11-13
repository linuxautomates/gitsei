package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.integrations.zendesk.models.ZendeskTicketQuery;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Zendesk's implementation of the {@link TriggerRunnable} for running {@link DbTrigger}
 */
@Log4j2
@Component
public class ZendeskTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "ZendeskController";
    private static final String ZENDESK = "zendesk";

    private static final int FULL_SCAN_FREQ_IN_DAYS = 7;

    private final ObjectMapper objectMapper;
    private final DefaultCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;

    public ZendeskTrigger(ObjectMapper objectMapper, TriggerActionService triggerActionService) {
        this.objectMapper = objectMapper;
        this.triggerActionService = triggerActionService;
        this.cursorStrategy = new DefaultCursorStrategy(TimeUnit.DAYS.toMinutes(FULL_SCAN_FREQ_IN_DAYS));
    }


    /**
     * Creates a job for the {@code trigger}. Also updates the {@link ZendeskTriggerMetadata} for the next trigger
     *
     * @param trigger {@link DbTrigger} containing information about the trigger
     * @throws Exception for any exception that may occur during job creation and metadata updation
     */
    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Template iterative scan: {}", trigger);
        ZendeskTriggerMetadata metadata = parseMetadata(trigger);
        Date now = new Date();
        DefaultCursorStrategy.DefaultCursor cursor =  cursorStrategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(metadata.getLastIterativeScan())
                .lastFullScan(metadata.getLastFullScan())
                .now(now)
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());
        Date from = cursor.isPartial() ? cursor.getFrom() : null;
        log.debug("run: scanning tickets from: {}, isFullScan: {}", from, !cursor.isPartial());
        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .tags(cursor.getTags())
                .query(ZendeskTicketQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(from)
                        .build())
                .build());
        ZendeskTriggerMetadata updatedMetadata = ZendeskTriggerMetadata.builder()
                .lastFullScan(cursor.isPartial() ? metadata.getLastFullScan() : now)
                .lastIterativeScan(now)
                .build();
        triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
    }

    /**
     * parses the metadata from {@link DbTrigger#getMetadata()} to {@link ZendeskTriggerMetadata}
     *
     * @param trigger {@link DbTrigger} containing information about the trigger
     * @return the parsed {@link ZendeskTriggerMetadata}
     */
    private ZendeskTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return ZendeskTriggerMetadata.builder().build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), ZendeskTriggerMetadata.class);
    }

    @Override
    public String getTriggerType() {
        return ZENDESK;
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ZendeskTriggerMetadata.ZendeskTriggerMetadataBuilder.class)
    public static class ZendeskTriggerMetadata {

        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_iterative_scan")
        Date lastIterativeScan;
    }
}
