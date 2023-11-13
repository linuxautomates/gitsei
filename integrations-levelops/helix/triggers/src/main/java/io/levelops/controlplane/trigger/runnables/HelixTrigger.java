package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.integrations.helix_swarm.models.HelixSwarmQuery;
import io.levelops.integrations.helixcore.models.HelixCoreIterativeQuery;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component
public class HelixTrigger implements TriggerRunnable {

    private static final String HELIX_CORE_CONTROLLER_NAME = "HelixCoreIterativeScanController";
    private static final String HELIX_SWARM_CONTROLLER_NAME = "HelixSwarmReviewController";
    private static final String HELIX = "helix";

    private static final int FULL_SCAN_FREQ_IN_DAYS = 7;
    private static final long DEPOT_SCAN_FREQ_IN_MIN = TimeUnit.DAYS.toMinutes(1);

    private final ObjectMapper objectMapper;
    private final DefaultCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;
    private final InventoryService inventoryService;

    public HelixTrigger(ObjectMapper objectMapper, TriggerActionService triggerActionService,
                        InventoryService inventoryService) {
        this.objectMapper = objectMapper;
        this.cursorStrategy = new DefaultCursorStrategy(TimeUnit.DAYS.toMinutes(FULL_SCAN_FREQ_IN_DAYS));
        this.triggerActionService = triggerActionService;
        this.inventoryService = inventoryService;
    }

    /**
     * Creates a job for the {@code trigger}. Also updates the {@link HelixTriggerMetadata} for the next trigger
     *
     * @param trigger {@link io.levelops.controlplane.models.DbTrigger} containing information about the trigger
     * @throws Exception for any exception that may occur during job creation and metadata updation
     */
    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Template iterative scan: {}", trigger);
        HelixTriggerMetadata metadata = parseMetadata(trigger);
        Date now = new Date();
        DefaultCursorStrategy.DefaultCursor cursor = cursorStrategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(metadata.getLastIterativeScan())
                .lastFullScan(metadata.getLastFullScan())
                .now(now)
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());
        log.debug("run: scanning entities from: {}, isFullScan: {}", cursor.getFrom(), !cursor.isPartial());
        boolean shouldFetchDepots = shouldFetchDepots(metadata.getLastDepotScan(), now);
        Date from = cursor.isPartial() ? cursor.getFrom() : null;
        triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                .controllerName(HELIX_CORE_CONTROLLER_NAME)
                .tags(cursor.getTags())
                .query(HelixCoreIterativeQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .from(from)
                        .to(cursor.getTo())
                        .fetchDepots(shouldFetchDepots)
                        .build())
                .build());
        boolean needHelixSwarm = needHelixSwarm(trigger);
        if (needHelixSwarm)
            triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                    .controllerName(HELIX_SWARM_CONTROLLER_NAME)
                    .tags(cursor.getTags())
                    .query(HelixSwarmQuery.builder()
                            .integrationKey(trigger.getIntegrationKey())
                            .from(from)
                            .to(cursor.getTo())
                            .build())
                    .build());
        HelixTriggerMetadata updatedMetadata = HelixTriggerMetadata.builder()
                .cursor(cursor.getTo())
                .lastFullScan(cursor.isPartial() ? metadata.getLastFullScan() : now)
                .lastIterativeScan(now)
                .lastDepotScan(shouldFetchDepots ? now : metadata.getLastDepotScan())
                .build();
        triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
    }

    private boolean needHelixSwarm(DbTrigger trigger) throws InventoryException {
        Integration helixIntegration = inventoryService.getIntegration(trigger.getTenantId(),
                trigger.getIntegrationId());
        boolean result = false;
        boolean needHelixSwarm = false;
        if (helixIntegration.getMetadata().get("helix_swarm_url") != null) {
            if (StringUtils.isNotEmpty(String.valueOf(helixIntegration.getMetadata().get("helix_swarm_url"))))
                needHelixSwarm = true;
            result = needHelixSwarm;
        }
        return result;
    }

    public boolean shouldFetchDepots(Date lastDepotScan, Date now) {
        return lastDepotScan == null || lastDepotScan.toInstant().isBefore(
                now.toInstant().minus(DEPOT_SCAN_FREQ_IN_MIN, ChronoUnit.MINUTES));
    }

    private HelixTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return HelixTriggerMetadata.builder().build();
        }

        return objectMapper.convertValue(trigger.getMetadata(), HelixTriggerMetadata.class);
    }

    @Override
    public String getTriggerType() {
        return HELIX;
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = HelixTriggerMetadata.HelixTriggerMetadataBuilder.class)
    public static class HelixTriggerMetadata {

        @JsonProperty("cursor")
        Date cursor;

        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_iterative_scan")
        Date lastIterativeScan;

        @JsonProperty("last_depot_scan")
        Date lastDepotScan;
    }
}
