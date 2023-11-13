package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.TriageRule;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.triage.clients.TriageRESTClient;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/triage_rules")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@Log4j2
@SuppressWarnings("unused")
public class TriageRulesController {
    private TriageRESTClient restClient;

    @Autowired
    public TriageRulesController(TriageRESTClient restClient) {
        this.restClient = restClient;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<String>> createRule(@RequestBody TriageRule rule,
                                                             @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(restClient.createTriageRule(company, rule)));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @RequestMapping(method = RequestMethod.DELETE, value = "/{ruleid}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> deleteRule(@PathVariable("ruleid") UUID ruleId,
                                                                     @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            var response = restClient.deleteTriageRule(company, ruleId.toString());
            return ResponseEntity.ok(response);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> bulkDeleteRules(@SessionAttribute(name = "company") String company,
                                                                              @RequestBody List<UUID> uuids) {
        return SpringUtils.deferResponse(() -> {
            final List<String> ids = uuids.stream().map(UUID::toString).collect(Collectors.toList());
            var responses = restClient.bulkDeleteTriageRules(company, ids);
            return ResponseEntity.ok(responses);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @RequestMapping(method = RequestMethod.PUT, value = "/{ruleid}", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> ruleUpdate(@RequestBody TriageRule rule,
                                                             @PathVariable("ruleid") UUID ruleId,
                                                             @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(restClient.updateTriageRule(company, ruleId.toString(), rule)));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.GET, value = "/{ruleid}", produces = "application/json")
    public DeferredResult<ResponseEntity<TriageRule>> getRule(@PathVariable("ruleid") UUID ruleid,
                                                              @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(restClient.getTriageRule(company, ruleid.toString())));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<TriageRule>>> rulesList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() ->
                ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(),
                        restClient.listTriageRules(company, filter))));
    }
}
