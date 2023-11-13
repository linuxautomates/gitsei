package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.exceptions.ServerApiException;
import io.levelops.api.services.WorkflowUiDataService;
import io.levelops.commons.databases.services.PluginDatabaseService;
import io.levelops.commons.databases.services.WorkflowDatabaseService;
import io.levelops.commons.databases.services.WorkflowPolicyDatabaseService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.util.SpringUtils;
import io.levelops.workflow.converters.WorkflowUiDataParser;
import io.levelops.workflow.converters.WorkflowUiDataParser.ParsedWorkflowData;
import io.levelops.workflow.models.Workflow;
import io.levelops.workflow.models.WorkflowPolicy;
import io.levelops.workflow.models.ui.WorkflowUiData;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Log4j2
@RequestMapping("/v1/workflows")
@SuppressWarnings("unused")
public class WorkflowController {

    private final WorkflowUiDataService workflowUiDataService;
    private ObjectMapper objectMapper;
    private final WorkflowDatabaseService workflowDatabaseService;
    private final WorkflowPolicyDatabaseService workflowPolicyDatabaseService;
    private final WorkflowUiDataParser workflowUiDataParser;
    private PluginDatabaseService pluginService;

    @Autowired
    public WorkflowController(ObjectMapper objectMapper,
                              WorkflowUiDataService workflowUiDataService,
                              WorkflowDatabaseService workflowDatabaseService,
                              WorkflowPolicyDatabaseService workflowPolicyDatabaseService,
                              WorkflowUiDataParser workflowUiDataParser) {
        this.objectMapper = objectMapper;
        this.workflowUiDataService = workflowUiDataService;
        this.workflowDatabaseService = workflowDatabaseService;
        this.workflowPolicyDatabaseService = workflowPolicyDatabaseService;
        this.workflowUiDataParser = workflowUiDataParser;
    }

    public DeferredResult<ResponseEntity<String>> createWorkflow(@SessionAttribute("company") String company,
                                                                 @RequestBody String rawWorkflowUiData) {
        return SpringUtils.deferResponse(() -> {
            // parse UI data into Backend data
            WorkflowUiData workflowUiData = objectMapper.readValue(rawWorkflowUiData, WorkflowUiData.class);
            ParsedWorkflowData parsed = workflowUiDataParser.parse(workflowUiData);
            Workflow workflow = parsed.getWorkflow();

            if (workflowDatabaseService.get(company, workflow.getId()).isPresent()) {
                throw new ServerApiException(HttpStatus.FORBIDDEN, "Workflow already exists");
            }

            // upload UI data to GCS
            String gcsPath = workflowUiDataService.uploadData(company, workflow.getId(), rawWorkflowUiData);
            workflow = workflow.toBuilder()
                    .uiData(gcsPath)
                    .build();

            // save to DB
            try {
                workflowDatabaseService.insertAndReturnId(company, workflow)
                        .orElseThrow(() -> new ServerApiException("Cannot create Workflow: already exists"));
                for (WorkflowPolicy policy : parsed.getPolicies()) {
                    workflowPolicyDatabaseService.insertAndReturnId(company, policy)
                            .orElseThrow(() -> new ServerApiException("Cannot create Workflow: policy already exists"));
                }
            } catch (Exception e) {
                log.warn("Failed to save workflow data to DB; deleting from GCS");
                workflowUiDataService.deleteResultsFromGcs(gcsPath);
                throw e;
            }

            return ResponseEntity.ok()
                    .body("{\"id\": \"" + workflow.getId() + "\"}");
        });
    }

    @GetMapping(path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Workflow>> getWorkflow(@SessionAttribute("company") String company,
                                                                  @PathVariable("id") String id) {
        return SpringUtils.deferResponse(() -> {
            Workflow workflow = workflowDatabaseService.get(company, id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow with id " + id + " not found."))
                    .toBuilder()
                    .uiData(workflowUiDataService.downloadData(company, id))
                    .build();
            return ResponseEntity.ok().body(workflow);
        });
    }

    @PostMapping(path = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> listWorkflows(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest filter) {
        throw new UnsupportedOperationException();
    }

    @PostMapping(path = "/policies/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> getPolicy(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest filter) {
        throw new UnsupportedOperationException();
    }

    @PostMapping(path = "/policies/list", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> listPolicies(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest filter) {
        throw new UnsupportedOperationException();
    }
}
