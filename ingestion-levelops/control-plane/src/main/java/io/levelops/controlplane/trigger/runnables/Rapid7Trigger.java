package io.levelops.controlplane.trigger.runnables;

import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.services.TriggeredJobService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.ingestion.integrations.rapid7.Rapid7Query;
import io.levelops.ingestion.models.CreateJobRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Rapid7Trigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "Rapid7Controller";

    private final TriggeredJobService triggeredJobService;

    @Autowired
    public Rapid7Trigger(TriggeredJobService triggeredJobService) {
        this.triggeredJobService = triggeredJobService;
    }

    @Override
    public String getTriggerType() {
        return "rapid7";
    }

    @Override
    public void run(DbTrigger trigger) throws Exception {
        triggeredJobService.createTriggeredJob(trigger, false, CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .query(Rapid7Query.builder()
                        .integrationKey(trigger.getIntegrationKey())
                        .build())
                .build());
    }

}
