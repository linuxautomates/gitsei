package io.levelops.controlplane.trigger;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.ingestion.models.CreateJobRequest;

public interface TriggerActionService {

    // TODO replace with TriggeredJobRequest model
    void createTriggeredJob(DbTrigger trigger, boolean partial, CreateJobRequest createJobRequest) throws Exception;

    <T> void updateTriggerMetadata(String triggerId, T updatedMetadata) throws JsonProcessingException;

}
