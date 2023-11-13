package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.database.TriageRuleHit;
import io.levelops.commons.databases.services.TriageRuleHitsService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;

@RestController
@Log4j2
@RequestMapping("/internal/v1/tenants/{company}/triage_rule_results")
public class TriageRuleHitsController {
    private final TriageRuleHitsService triageRulesHitsService;

    @Autowired
    public TriageRuleHitsController(TriageRuleHitsService triageRulesHitsService) {
        this.triageRulesHitsService = triageRulesHitsService;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createRuleHit(
            @PathVariable("company") String company,
            @RequestBody TriageRuleHit rule) {
        return SpringUtils.deferResponse(() ->
                ResponseEntity.accepted().body(Map.of("id",
                        triageRulesHitsService.insert(company, rule))));
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<DbListResponse<TriageRuleHit>>> listHits(
            @PathVariable("company") String company,
            @RequestBody DefaultListRequest listRequest) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                triageRulesHitsService.listJenkinsRuleHits(
                        company,
                        (List<String>) listRequest.getFilter().get("job_run_ids"),
                        (List<String>) listRequest.getFilter().get("stage_ids"),
                        (List<String>) listRequest.getFilter().get("step_ids"),
                        (List<String>) listRequest.getFilter().get("rule_ids"),
                        (List<String>) listRequest.getFilter().get("job_names"),
                        (List<String>) listRequest.getFilter().get("job_normalized_full_names"),
                        (List<String>) listRequest.getFilter().get("cicd_instance_ids"),
                        listRequest.getPage(),
                        listRequest.getPageSize())));
    }

}
