package io.levelops.controlplane.trigger.runnables;

import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.services.TriggeredJobService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.ingestion.controllers.generic.BaseIntegrationQuery;
import io.levelops.ingestion.models.CreateJobRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SnykTrigger implements TriggerRunnable {
    private static final String CONTROLLER_NAME = "SnykIterativeScanController";

    private final TriggeredJobService triggeredJobService;

    @Autowired
    public SnykTrigger(TriggeredJobService triggeredJobService) {
        this.triggeredJobService = triggeredJobService;
    }

    @Override
    public String getTriggerType() {
        return "snyk";
    }

    @Override
    public void run(DbTrigger trigger) throws Exception {
        triggeredJobService.createTriggeredJob(trigger, false, CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .query(BaseIntegrationQuery.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .build())
                .build());
    }

}
