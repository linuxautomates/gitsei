package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.integrations.checkmarx.sources.cxsca.CxScaProjectDataSource;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Log4j2
public class CxScaScanTrigger implements TriggerRunnable {
    private static final String SCAN_CONTROLLER_NAME = "CxScaScanController";

    private static final String CXSCASCAN = "cxscascan";
    private static final int FULL_SCAN_FREQ_IN_DAYS = 7;

    private final ObjectMapper objectMapper;
    private final DefaultCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;

    CxScaScanTrigger(ObjectMapper objectMapper, TriggerActionService triggerActionService) {
        this.objectMapper = objectMapper;
        this.triggerActionService = triggerActionService;
        this.cursorStrategy = new DefaultCursorStrategy(TimeUnit.DAYS.toMinutes(FULL_SCAN_FREQ_IN_DAYS));
    }

    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Template iterative scan: {}", trigger);
        CxScaTriggerMetadata metadata = parseMetadata(trigger);
        Date now = new Date();
        DefaultCursorStrategy.DefaultCursor cursor =  cursorStrategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(metadata.getLastIterativeScan())
                .lastFullScan(metadata.getLastFullScan())
                .now(now)
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());
        Date from = cursor.isPartial() ? cursor.getFrom() : null;
        log.debug("run: scanning  from: {}, isFullScan: {}", from, !cursor.isPartial());
        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(SCAN_CONTROLLER_NAME)
                .tags(cursor.getTags())
                .query(CxScaProjectDataSource.CxScaProjectQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .build())
                .build());
        CxScaTriggerMetadata updatedMetadata = CxScaTriggerMetadata.builder()
                .lastFullScan(cursor.isPartial() ? metadata.getLastFullScan() : now)
                .lastIterativeScan(now)
                .build();
        triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
    }


    @Override
    public String getTriggerType() {
        return CXSCASCAN;
    }

    private CxScaTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return CxScaTriggerMetadata.builder().build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), CxScaTriggerMetadata.class);
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CxScaTriggerMetadata.CxScaTriggerMetadataBuilder.class)
    public static class CxScaTriggerMetadata {

        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_iterative_scan")
        Date lastIterativeScan;
    }
}

