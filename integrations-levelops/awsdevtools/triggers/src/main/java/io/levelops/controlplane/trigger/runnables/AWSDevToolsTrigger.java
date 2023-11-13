package io.levelops.controlplane.trigger.runnables;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Splitter;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.strategies.DefaultCursorStrategy;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.integrations.awsdevtools.models.AWSDevToolsQuery;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * AWSDevTools's implementation of the {@link TriggerRunnable} for running {@link DbTrigger}
 */
@Log4j2
@Component
public class AWSDevToolsTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "AWSDevToolsController";
    private static final String AWS_DEV_TOOLS = "awsdevtools";

    private static final int FULL_SCAN_FREQ_IN_DAYS = 7;

    private final ObjectMapper objectMapper;
    private final DefaultCursorStrategy cursorStrategy;
    private final TriggerActionService triggerActionService;
    private final InventoryService inventoryService;

    public AWSDevToolsTrigger(ObjectMapper objectMapper, TriggerActionService triggerActionService, InventoryService inventoryService) {
        this.objectMapper = objectMapper;
        this.triggerActionService = triggerActionService;
        this.inventoryService = inventoryService;
        this.cursorStrategy = new DefaultCursorStrategy(TimeUnit.DAYS.toMinutes(FULL_SCAN_FREQ_IN_DAYS));
    }

    /**
     * Creates a job for the {@code trigger}. Also updates the {@link AWSDevToolsTriggerMetadata} for the next trigger
     *
     * @param trigger {@link DbTrigger} containing information about the trigger
     * @throws Exception for any exception that may occur during job creation and metadata updation.
     */
    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering Template iterative scan: {}", trigger);
        AWSDevToolsTriggerMetadata metadata = parseMetadata(trigger);
        Date now = new Date();
        DefaultCursorStrategy.DefaultCursor cursor =  cursorStrategy.getNextCursor(DefaultCursorStrategy.DefaultCursorMetadata.builder()
                .currentCursor(metadata.getLastIterativeScan())
                .lastFullScan(metadata.getLastFullScan())
                .now(now)
                .triggerCreatedAtEpoch(trigger.getCreatedAt())
                .build());
        Date from = cursor.isPartial() ? cursor.getFrom() : null;
        Date to = cursor.getTo();
        log.debug("run: scanning builds from: {}, isFullScan: {}", from, !cursor.isPartial());
        List<String> regions;
        Integration integration = inventoryService.getIntegration(trigger.getIntegrationKey());
        Map<String, Object> integrationMetadata = integration.getMetadata();
        if (integrationMetadata != null && integrationMetadata.containsKey("regions") && integrationMetadata.get("regions") != null) {
            regions = Splitter.on(",").omitEmptyStrings().trimResults().splitToList((String) integrationMetadata.get("regions"));
        } else {
            List<Regions> values = Arrays.asList(Regions.values());
            values.remove(Regions.GovCloud);
            regions = values.stream().map(Enum::toString).collect(Collectors.toList());
        }
        for (String region : regions) {
            triggerActionService.createTriggeredJob(trigger, cursor.isPartial(), CreateJobRequest.builder()
                    .controllerName(CONTROLLER_NAME)
                    .tags(cursor.getTags())
                    .query(AWSDevToolsQuery.builder()
                            .regionIntegrationKey(AWSDevToolsQuery.RegionIntegrationKey.builder()
                                    .integrationKey(trigger.getIntegrationKey())
                                    .region(region)
                                    .build())
                            .from(from)
                            .to(to)
                            .build())
                    .build());
            AWSDevToolsTriggerMetadata updatedMetadata = AWSDevToolsTriggerMetadata.builder()
                    .lastFullScan(cursor.isPartial() ? metadata.getLastFullScan() : now)
                    .lastIterativeScan(now)
                    .build();
            triggerActionService.updateTriggerMetadata(trigger.getId(), updatedMetadata);
        }
    }

    @Override
    public String getTriggerType() {
        return AWS_DEV_TOOLS;
    }

    /**
     * parses the metadata from {@link DbTrigger#getMetadata()} to {@link AWSDevToolsTriggerMetadata}
     *
     * @param trigger {@link DbTrigger} containing information about the trigger
     * @return the parsed {@link AWSDevToolsTriggerMetadata}
     */
    private AWSDevToolsTriggerMetadata parseMetadata(DbTrigger trigger) {
        if (trigger.getMetadata() == null) {
            return AWSDevToolsTriggerMetadata.builder().build();
        }
        return objectMapper.convertValue(trigger.getMetadata(), AWSDevToolsTriggerMetadata.class);
    }

    @lombok.Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = AWSDevToolsTriggerMetadata.AWSDevToolsTriggerMetadataBuilder.class)
    public static class AWSDevToolsTriggerMetadata {

        @JsonProperty("last_full_scan")
        Date lastFullScan;

        @JsonProperty("last_iterative_scan")
        Date lastIterativeScan;
    }
}
