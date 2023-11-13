package io.levelops.controlplane.trigger.runnables;

import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.services.TriggeredJobService;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.ingestion.models.CreateJobRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TestTrigger implements TriggerRunnable {

    private static final String CONTROLLER_NAME = "TestController";

    private final TriggeredJobService triggeredJobService;

    @Autowired
    public TestTrigger(TriggeredJobService triggeredJobService) {
        this.triggeredJobService = triggeredJobService;
    }

    @Override
    public String getTriggerType() {
        return "test";
    }

    @Override
    public void run(DbTrigger trigger) throws Exception {
        triggeredJobService.createTriggeredJob(trigger, false, CreateJobRequest.builder()
                .controllerName(CONTROLLER_NAME)
                .query(Map.of("query", "no"))
                .build());
    }
}
