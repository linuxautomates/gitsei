package io.levelops.controlplane.trigger;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.controlplane.database.TriggerDatabaseService;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.services.TriggeredJobService;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.web.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DefaultTriggerActionService implements TriggerActionService {


    private final TriggeredJobService triggeredJobService;
    private final TriggerDatabaseService triggerDatabaseService;

    @Autowired
    public DefaultTriggerActionService(TriggeredJobService triggeredJobService,
                                       TriggerDatabaseService triggerDatabaseService) {
        this.triggeredJobService = triggeredJobService;
        this.triggerDatabaseService = triggerDatabaseService;
    }

    public void createTriggeredJob(DbTrigger trigger, boolean partial, CreateJobRequest createJobRequest) throws NotFoundException, JsonProcessingException {
        triggeredJobService.createTriggeredJob(trigger, partial, createJobRequest);
    }

    public <T> void updateTriggerMetadata(String triggerId, T updatedMetadata) throws JsonProcessingException {
        triggerDatabaseService.updateTriggerMetadata(triggerId, updatedMetadata);
    }

}
