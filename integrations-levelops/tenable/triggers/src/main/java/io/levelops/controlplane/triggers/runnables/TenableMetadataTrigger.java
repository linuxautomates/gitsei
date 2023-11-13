package io.levelops.controlplane.triggers.runnables;

import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerActionService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.integrations.tenable.models.TenableMetadataQuery;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

/**
 * Tenable's implementation of the {@link TriggerRunnable} for running {@link DbTrigger} for metadata ingestion controller
 */
@Log4j2
@Component
public class TenableMetadataTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "TenableMetadataIngestionController";
    private static final String TENABLE_INGESTION = "tenable_ingestion";

    private final TriggerActionService triggerActionService;

    public TenableMetadataTrigger(TriggerActionService triggerActionService) {
        this.triggerActionService = triggerActionService;
    }

    /**
     * Creates a job for the {@code trigger}.
     *
     * @param trigger {@link DbTrigger} containing information about the trigger
     * @throws Exception for any exception that may occur during job creation and metadata update
     */
    @Override
    public void run(DbTrigger trigger) throws Exception {
        log.debug("Triggering metadata ingestion scan: {}", trigger);
        triggerActionService.createTriggeredJob(trigger, false, CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .query(TenableMetadataQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .build())
                .build());
    }

    @Override
    public String getTriggerType() {
        return TENABLE_INGESTION;
    }
}
