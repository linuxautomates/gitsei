package io.levelops.controlplane.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.controlplane.database.TriggerDatabaseService;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.models.EnableHistoricalTriggerRequest;
import io.levelops.controlplane.services.TriggeredJobService;
import io.levelops.controlplane.trigger.runnables.GithubTrigger;
import io.levelops.ingestion.models.controlplane.CreateTriggerRequest;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Log4j2
@RestController
@RequestMapping("/v1/triggers")
public class TriggerController {

    private static final int PAGE_SIZE = 25;
    private final ObjectMapper objectMapper;
    private final TriggerDatabaseService triggerDatabaseService;
    private final TriggeredJobService triggeredJobService;

    @Autowired
    public TriggerController(ObjectMapper objectMapper,
                             TriggerDatabaseService triggerDatabaseService,
                             TriggeredJobService triggeredJobService) {
        this.objectMapper = objectMapper;
        this.triggerDatabaseService = triggerDatabaseService;
        this.triggeredJobService = triggeredJobService;
    }

    @GetMapping("/{triggerId}")
    public DbTrigger getTrigger(@RequestParam("trigger_id") String triggerId) throws NotFoundException {
        return triggerDatabaseService.getTriggerById(triggerId)
                .orElseThrow(() -> new NotFoundException("Could not find trigger with id=" + triggerId));
    }

    @GetMapping
    public PaginatedResponse<DbTrigger> getTriggers(@RequestParam(value = "page", required = false, defaultValue = "0") Integer pageNumber,
                                                    @RequestParam(value = "trigger_type", required = false) String triggerType,
                                                    @RequestParam(value = "tenant_id", required = false) String tenantId,
                                                    @RequestParam(value = "integration_id", required = false) String integrationId) {
        return PaginatedResponse.of(pageNumber, PAGE_SIZE,
                triggerDatabaseService.filter(pageNumber * PAGE_SIZE, PAGE_SIZE, tenantId, integrationId, triggerType));
    }

    @PostMapping
    public Map<String, Object> createTrigger(@RequestBody CreateTriggerRequest request) throws JsonProcessingException {
        Validate.notNull(request, "request cannot be null.");
        Validate.notNull(request.getFrequency(), "request.getFrequency() cannot be null.");
        UUID id = UUID.randomUUID();
        String tenantId = null;
        String integrationId = null;
        if (request.getIntegrationKey() != null) {
            tenantId = request.getIntegrationKey().getTenantId();
            integrationId = request.getIntegrationKey().getIntegrationId();
        }
        String metadata = request.getMetadata() != null ? objectMapper.writeValueAsString(request.getMetadata()) : null;
        triggerDatabaseService.createTrigger(id,
                tenantId,
                integrationId,
                request.getReserved(),
                request.getTriggerType(),
                request.getFrequency(),
                metadata,
                request.getCallbackUrl(),
                null);
        return Map.of("id", id);
    }

    @PutMapping("/{triggerId}/frequency")
    public void updateTriggerFrequency(@PathVariable("triggerId") String triggerId,
                                       @RequestParam(value = "frequency") Integer frequency) throws NotFoundException {
        if (!triggerDatabaseService.updateTriggerFrequency(triggerId, frequency)) {
            throw new NotFoundException("Could not update trigger with id=" + triggerId);
        }
        log.info("Updated frequency of trigger id={} to {} min", triggerId, frequency);
    }

    @PutMapping(path = "/{triggerId}/metadata", consumes = "application/json")
    public void updateTriggerMetadata(@PathVariable("triggerId") String triggerId,
                                      @RequestBody String metadata) throws NotFoundException, JsonProcessingException {
        if (!triggerDatabaseService.updateTriggerMetadata(triggerId, metadata)) {
            throw new NotFoundException("Could not update trigger with id=" + triggerId);
        }
        log.info("Updated metadata of trigger id={}", triggerId);
    }

    @PutMapping(path = "/{triggerId}/enableHistoricalTrigger", consumes = "application/json")
    public void enableHistoricalTrigger(@PathVariable("triggerId") String triggerId,
                                              @RequestBody EnableHistoricalTriggerRequest enableRequest) throws NotFoundException, JsonProcessingException {
        var trigger = triggerDatabaseService.getTriggerById(triggerId).get();
        // this is only available for github as of now
        if (!trigger.getType().equals("github")) {
            throw new NotFoundException("Trigger is not a github type. id=" + triggerId);
        }
        GithubTrigger.GithubTriggerMetadata githubMetadata = objectMapper.convertValue(trigger.getMetadata(), GithubTrigger.GithubTriggerMetadata.class);
        var updatedMetadata = githubMetadata.toBuilder()
                .shouldStartFetchingHistory(true)
                .shouldFetchHistory(true)
                .historicalSpanInDays(enableRequest.getHistoricalSpanInDays())
                .historicalSubJobSpanInMin(enableRequest.getHistoricalSubJobSpanInMin())
                .historicalSuccesiveBackwardScanCount(enableRequest.getHistoricalSuccessiveBackwardScanCount())
                .build();

        if(!triggerDatabaseService.updateTriggerMetadata(triggerId, updatedMetadata)){
            throw new RuntimeException("Unable to update trigger");
        }

        log.info("Enabled historical strategy for trigger id={}. UpdatedMetadata = {}", triggerId, updatedMetadata);
    }

    @DeleteMapping
    public Map<String, Object> deleteTriggers(@RequestParam("tenant_id") String tenantId, @RequestParam("integration_id") String integrationId) {
        List<DbTrigger> triggersByIntegration = triggerDatabaseService.getTriggersByIntegration(IntegrationKey.builder()
                .integrationId(integrationId)
                .tenantId(tenantId)
                .build());
        triggersByIntegration.stream()
                .map(DbTrigger::getId)
                .forEach(triggeredJobService::cleanUpTriggeredJobs);
        int deleted = triggerDatabaseService.deleteTriggers(tenantId, integrationId);
        return Map.of("deleted", deleted);
    }

    @DeleteMapping("/{triggerId}")
    public Map<String, Object> deleteTrigger(@RequestParam("trigger_id") String triggerId) {
        triggeredJobService.cleanUpTriggeredJobs(triggerId);
        boolean deleted = triggerDatabaseService.deleteTrigger(triggerId);
        return Map.of("deleted", deleted);
    }

}
