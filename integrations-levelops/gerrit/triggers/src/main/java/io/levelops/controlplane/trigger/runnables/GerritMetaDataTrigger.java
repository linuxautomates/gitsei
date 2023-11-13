package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.integrations.gerrit.models.GerritQuery;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component
public class GerritMetaDataTrigger implements TriggerRunnable {

    private static final String GERRIT_METADATA = "gerritMetadata";

    private static final int FULL_SCAN_FREQ_IN_DAYS = 7;
    public static final String GERRIT_INGESTION_CONTROLLER = "GerritIngestionController";

    private final ObjectMapper objectMapper;
    private final DefaultCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;

    public GerritMetaDataTrigger(ObjectMapper objectMapper, TriggerActionService triggerActionService) {
        this.objectMapper = objectMapper;
        this.cursorStrategy = new DefaultCursorStrategy(TimeUnit.DAYS.toMinutes(FULL_SCAN_FREQ_IN_DAYS));
        this.triggerActionService = triggerActionService;
    }

    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Template iterative scan: {}", trigger);
        GerritTriggerMetadata metadata = parseMetadata(trigger);
        Date now = new Date();
        DefaultCursorStrategy.DefaultCursor cursor =  cursorStrategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(metadata.getLastIterativeScan())
                .lastFullScan(metadata.getLastFullScan())
                .now(now)
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());

        Date from = cursor.isPartial() ? cursor.getFrom() : null;
        log.debug("run: scanning from: {}, isFullScan: {}", from, !cursor.isPartial());
        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(GERRIT_INGESTION_CONTROLLER)
                .tags(cursor.getTags())
                .query(GerritQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .after(from)
                        .build())
                .build());
        GerritTriggerMetadata updateMetadata = GerritTriggerMetadata.builder()
                .lastFullScan(cursor.isPartial() ? metadata.getLastFullScan() : now)
                .lastIterativeScan(now)
                .build();
        triggerActionService.updateTriggerMetadata(trigger.getId(), updateMetadata);
    }

    @Override
    public String getTriggerType() {
        return GERRIT_METADATA;
    }

    private GerritTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return GerritTriggerMetadata.builder().build();
        }

        return objectMapper.convertValue(trigger.getMetadata(), GerritTriggerMetadata.class);
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GerritTriggerMetadata.GerritTriggerMetadataBuilder.class)
    public static class GerritTriggerMetadata {
        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_iterative_scan")
        Date lastIterativeScan;
    }
}
