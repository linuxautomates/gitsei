package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.JobStatus;
import io.levelops.ingestion.models.controlplane.TriggerResults;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.storage.models.StorageResult;
import io.levelops.internal_api.services.DedupeAggMessagesService;
import io.levelops.internal_api.services.MessagePubService;
import io.levelops.internal_api.services.handlers.IntegrationDataHandler;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@Log4j2
@RequestMapping("/internal/v1/tenants/{company}/integrations")
public class IntegrationDataPushController {
    private static final Set<IntegrationType> SUPPORTED_AGG_INTEGRATIONS = new HashSet<>(List.of(
            IntegrationType.JIRA, IntegrationType.GITHUB, IntegrationType.SNYK, IntegrationType.PAGERDUTY,
            IntegrationType.BITBUCKET, IntegrationType.POSTGRES, IntegrationType.SPLUNK, IntegrationType.TENABLE,
            IntegrationType.ZENDESK, IntegrationType.SALESFORCE, IntegrationType.SONARQUBE, IntegrationType.TESTRAILS,
            IntegrationType.HELIX, IntegrationType.GERRIT, IntegrationType.SNYK, IntegrationType.SONARQUBE, IntegrationType.COVERITY,
            IntegrationType.AZURE_DEVOPS, IntegrationType.CIRCLECI, IntegrationType.DRONECI, IntegrationType.HARNESSNG, IntegrationType.AWSDEVTOOLS, IntegrationType.GITLAB,
            IntegrationType.CXSAST, IntegrationType.BITBUCKET_SERVER, IntegrationType.BLACKDUCK));
    private final ObjectMapper objectMapper;
    private final IntegrationService integrationService;
    private final MessagePubService pubService;
    private final ControlPlaneService controlPlaneService;
    private final DedupeAggMessagesService dedupeAggMessagesService;
    private final Map<String, IntegrationDataHandler> dataHandlerMap = Maps.newHashMap();

    @Autowired
    public IntegrationDataPushController(MessagePubService pubService,
                                         IntegrationService integrationService,
                                         List<IntegrationDataHandler> dataHandlers,
                                         ObjectMapper objectMapper,
                                         ControlPlaneService controlPlaneService,
                                         DedupeAggMessagesService dedupeAggMessagesService) {
        this.objectMapper = objectMapper;
        this.integrationService = integrationService;
        this.pubService = pubService;
        this.controlPlaneService = controlPlaneService;
        this.dedupeAggMessagesService = dedupeAggMessagesService;
        dataHandlers.forEach(handler -> {
            if (StringUtils.isEmpty(handler.getDataType())) {
                log.info("Ignoring data handler with no data type: {}", handler.getClass().getSimpleName());
                return;
            }
            // TODO: Need a fix. Handling AZURE_DEVOPS integrationType mismatch from AzureDevopsController
            String integrationType = (IntegrationType.AZURE_DEVOPS.equals(handler.getIntegrationType()))
                    ? "azuredevops" : handler.getIntegrationType().toString();
            dataHandlerMap.put(integrationType + "_" + handler.getDataType(), handler);
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{integrationid}/push", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> consumeData(@PathVariable("company") String company,
                                                            @PathVariable("integrationid") String integrationId,
                                                            @RequestBody TriggerResults triggerResults) {
        // NOTE: triggerResults will no longer return jobs - we will have to pull them
        return SpringUtils.deferResponse(() -> {
            log.info("Received trigger for: tenant={}, integration={}, trigger={}", company, integrationId,
                    triggerResults.getTriggerId());
            handleTriggerResults(company, integrationId, triggerResults);
            return ResponseEntity.accepted().build();
        });
    }

    private void handleTriggerResults(String company, String integrationId, TriggerResults results)
            throws IOException, SQLException {
        Optional<Integration> it = integrationService.get(company, integrationId);
        if (it.isEmpty()) {
            return;
        }
        Integration itData = it.get();
        final MutableBoolean topLevelUpdated = new MutableBoolean(false);
        final MutableInt jobCount = new MutableInt(0);
        final Set<String> dedupeNoDataHandlerFound = new HashSet<>(); //Used to dedupe "no data handler found" warnings
        controlPlaneService.streamTriggerResults(10, results.getTriggerId(), false, false, true)
                .forEach(RuntimeStreamException.wrap((JobDTO job) -> {
                    jobCount.increment();
                    log.debug("handling trigger results for company={}, integ={}, triggerId={}: jobId={} #{}", company, integrationId, results.getTriggerId(), job.getId(), jobCount.intValue());
                    //each job will fetch all projects and all repos so we only need to look at the first successful one in the list
                    if (!topLevelUpdated.booleanValue() && job.getStatus() == JobStatus.SUCCESS) {
                        ListResponse<StorageResult> listResponse =
                                objectMapper.readValue(objectMapper.writeValueAsString(job.getResult()),
                                        objectMapper.getTypeFactory().constructParametricType(
                                                ListResponse.class, StorageResult.class));
                        for (StorageResult storageResult : listResponse.getRecords()) {
                            if (storageResult.getStorageMetadata() == null) {
                                continue;
                            }
                            String dataType = storageResult.getStorageMetadata().getDataType();
                            String integrationType = storageResult.getStorageMetadata().getIntegrationType();
                            IntegrationDataHandler handler = dataHandlerMap.get(integrationType + "_" + dataType.toLowerCase());
                            if (handler == null) {
                                String warning = "For integration type " + integrationType + " dataType " + dataType + " no data handler found !!!";
                                if (!dedupeNoDataHandlerFound.contains(warning)) {
                                    log.warn(warning);
                                    dedupeNoDataHandlerFound.add(warning);
                                }
                            } else {
                                if (handler.handleStorageResult(company, integrationId, storageResult)) {
                                    topLevelUpdated.setValue(true);
                                } else {
                                    log.warn("For integration type " + integrationType + " dataType " + dataType + " data handler had error storing results !!!");
                                }
                            }
                        }
                    }
                    return null;
                })::apply);
        log.info("handled trigger results for company={}, integ={}, triggerId={}: jobCount={}", company, integrationId, results.getTriggerId(), jobCount.intValue());
        IntegrationType app = IntegrationType.fromString(itData.getApplication());
        if (SUPPORTED_AGG_INTEGRATIONS.contains(app)) {
            if (dedupeAggMessagesService.shouldRunAgg(company, integrationId)) {
                pubService.publishIntegAggMessages(company, itData);
            }
        }
    }

}
