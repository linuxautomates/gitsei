package io.levelops.controlplane.controllers;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.controlplane.database.TriggerDatabaseService;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.services.TriggerResultService;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.models.controlplane.TriggerResults;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@RestController
@RequestMapping("/v1/triggers")
public class TriggerResultsController {

    private final TriggerDatabaseService triggerDatabaseService;
    private final TriggerResultService triggerResultService;

    @Autowired
    public TriggerResultsController(final TriggerDatabaseService triggerDatabaseService,
                                    final TriggerResultService triggerResultService) {
        this.triggerDatabaseService = triggerDatabaseService;
        this.triggerResultService = triggerResultService;
    }

    @GetMapping("/{triggerId}/iterations/{iterationId}/results")
    public TriggerResults getTriggerResultsForIteration(@PathVariable("triggerId") String triggerId,
                                                        @PathVariable("iterationId") String iterationId) throws NotFoundException {
        Validate.notBlank(triggerId, "triggerId cannot be null or empty.");
        Validate.notBlank(iterationId, "iterationId cannot be null or empty.");
        DbTrigger dbTrigger = triggerDatabaseService.getTriggerById(triggerId)
                .orElseThrow(() -> new NotFoundException("Could not find trigger with id=" + triggerId));
        return triggerResultService.retrieveTriggerResultsForIteration(dbTrigger, iterationId, true);
    }

    @PostMapping("/{triggerId}/results/latest/list")
    public TriggerResults listLatestTriggerResults(@PathVariable("triggerId") String triggerId,
                                                   @RequestParam(value = "partial", required = false, defaultValue = "false") boolean partial,
                                                   @RequestParam(value = "allow_empty_results", required = false, defaultValue = "false") boolean allowEmptyResults,
                                                   @RequestParam(value = "only_successful_results", required = false, defaultValue = "true") boolean onlySuccessfulResults,
                                                   @RequestParam(value = "include_job_result_field", required = false, defaultValue = "true") boolean includeJobResultField,
                                                   @RequestBody DefaultListRequest request) throws NotFoundException {
        DbTrigger trigger = triggerDatabaseService.getTriggerById(triggerId)
                .orElseThrow(() -> new NotFoundException("Could not find trigger with id=" + triggerId));

        return triggerResultService.retrieveLatestTriggerResults(request.getPage(), request.getPageSize(), trigger, partial, allowEmptyResults, onlySuccessfulResults, includeJobResultField);
    }

    @GetMapping("/{triggerId}/results/latest")
    public TriggerResults getLatestTriggerResults(@PathVariable("triggerId") String triggerId,
                                                  @RequestParam(value = "partial", required = false, defaultValue = "false") boolean partial,
                                                  @RequestParam(value = "allow_empty_results", required = false, defaultValue = "false") boolean allowEmptyResults,
                                                  @RequestParam(value = "only_successful_results", required = false, defaultValue = "true") boolean onlySuccessfulResults,
                                                  @RequestParam(value = "include_job_result_field", required = false, defaultValue = "true") boolean includeJobResultField) throws NotFoundException {
        DbTrigger trigger = triggerDatabaseService.getTriggerById(triggerId)
                .orElseThrow(() -> new NotFoundException("Could not find trigger with id=" + triggerId));

        return triggerResultService.retrieveLatestTriggerResults(trigger, partial, allowEmptyResults, onlySuccessfulResults, includeJobResultField);
    }

    @GetMapping("/{triggerId}/results")
    public TriggerResults getTriggerResultsSince(@PathVariable("triggerId") String triggerId,
                                                 @RequestParam(value = "iteration_id", required = false) String iterationId,
                                                 @RequestParam(value = "partial", required = false, defaultValue = "false") boolean partial,
                                                 @RequestParam(value = "allow_empty_results", required = false, defaultValue = "false") boolean allowEmptyResults,
                                                 @RequestParam(value = "only_successful_results", required = false, defaultValue = "true") boolean onlySuccessfulResults,
                                                 @RequestParam(value = "last_n_jobs", required = false, defaultValue = "0") Integer lastNjobs,
                                                 @RequestParam(value = "after", required = false, defaultValue = "0") Long after,
                                                 @RequestParam(value = "before", required = false, defaultValue = "0") Long before) throws NotFoundException {
        if (Strings.isBlank(iterationId) && (lastNjobs == null || lastNjobs < 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "iteration id or last n jobs must be specified.");
        }
        DbTrigger trigger = triggerDatabaseService.getTriggerById(triggerId)
                .orElseThrow(() -> new NotFoundException("Could not find trigger with id=" + triggerId));
        if (lastNjobs != null && lastNjobs > 0) {
            return triggerResultService.retrieveTriggerResults(trigger, lastNjobs, partial, allowEmptyResults, onlySuccessfulResults, after, before, true);
        }
        return triggerResultService.retrieveTriggerResults(trigger, iterationId, partial, allowEmptyResults, onlySuccessfulResults, true);
    }

    @GetMapping("{triggerId}/results/callback")
    public Map<String, Object> triggerCallback(@PathVariable("triggerId") String triggerId,
                                               @RequestParam("iteration_id") String iterationId) throws NotFoundException {
        DbTrigger dbTrigger = triggerDatabaseService.getTriggerById(triggerId)
                .orElseThrow(() -> new NotFoundException("Could not find Trigger with id=" + triggerId));
        boolean status = triggerResultService.triggerCallback(dbTrigger, iterationId);
        return Map.of(
                "job_id", triggerId,
                "status", status);
    }

    @GetMapping("/all/results/latest")
    public MultipleTriggerResults getAllTriggerResults(@RequestParam("integration_id") String integrationId,
                                                       @RequestParam("tenant_id") String tenantId,
                                                       @RequestParam(value = "trigger_type", required = false) String triggerType,
                                                       @RequestParam(value = "partial", required = false, defaultValue = "false") boolean partial,
                                                       @RequestParam(value = "allow_empty_results", required = false, defaultValue = "false") boolean allowEmptyResults,
                                                       @RequestParam(value = "only_successful_results", required = false, defaultValue = "true") boolean onlySuccessfulResults) {
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(tenantId)
                .integrationId(integrationId)
                .build();
        List<DbTrigger> triggers;
        if (StringUtils.isEmpty(triggerType)) {
            triggers = triggerDatabaseService.getTriggersByIntegration(integrationKey);
        } else {
            triggers = triggerDatabaseService.getTriggersByIntegrationAndType(integrationKey, triggerType);
        }
        return MultipleTriggerResults.builder()
                .integrationId(integrationId)
                .tenantId(tenantId)
                .partial(Boolean.TRUE.equals(partial))
                .triggerResults(triggers.stream()
                        .map(trigger -> triggerResultService.retrieveLatestTriggerResults(trigger, partial, allowEmptyResults, onlySuccessfulResults, true))
                        .collect(Collectors.toList()))
                .build();
    }

    @GetMapping("{triggerId}/results/latest/callback")
    public Map<String, Object> triggerCallbackForLatestResults(@PathVariable("triggerId") String triggerId) throws NotFoundException {
        DbTrigger dbTrigger = triggerDatabaseService.getTriggerById(triggerId)
                .orElseThrow(() -> new NotFoundException("Could not find Trigger with id=" + triggerId));
        boolean status = triggerResultService.triggerCallbackForLatestResults(dbTrigger);
        return Map.of(
                "job_id", triggerId,
                "status", status);
    }

}
