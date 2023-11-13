package io.levelops.api.controllers;

import io.levelops.commons.databases.models.database.TriageRuleHit;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.triage.clients.TriageRESTClient;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping("/v1/triage_rule_results")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
@Log4j2
@SuppressWarnings("unused")
public class TriageRuleResultsController {
    private final TriageRESTClient restClient;

    @Autowired
    public TriageRuleResultsController(TriageRESTClient restClient) {
        this.restClient = restClient;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<TriageRuleHit>>> ruleResultsList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() ->
                ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(),
                        restClient.listTriageRuleResults(company, filter))));
    }
}
