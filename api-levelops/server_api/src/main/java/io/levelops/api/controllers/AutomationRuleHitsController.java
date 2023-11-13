package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.automation_rules.AutomationRuleHitsClient;
import io.levelops.commons.databases.models.database.automation_rules.AutomationRuleHit;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
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

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/automation_rule_hits")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@Log4j2
@SuppressWarnings("unused")
public class AutomationRuleHitsController {
    private AutomationRuleHitsClient automationRuleHitsClient;

    @Autowired
    public AutomationRuleHitsController(AutomationRuleHitsClient automationRuleHitsClient) {
        this.automationRuleHitsClient = automationRuleHitsClient;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_CREATE)
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createRule(@RequestBody AutomationRuleHit ruleHit,
                                                                          @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(Map.of("id", automationRuleHitsClient.createAutomationRuleHit(company, ruleHit).getId())));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_DELETE)
    @RequestMapping(method = RequestMethod.DELETE, value = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> deleteRule(@PathVariable("id") UUID id,
                                                           @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            automationRuleHitsClient.deleteAutomationRuleHit(company, id.toString());
            return ResponseEntity.ok().build();
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_EDIT)
    @RequestMapping(method = RequestMethod.PUT, value = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> ruleHitUpdate(@RequestBody AutomationRuleHit ruleHit,
                                                                          @PathVariable("id") UUID id,
                                                                          @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(Map.of("id", automationRuleHitsClient.updateAutomationRuleHit(company, id.toString(), ruleHit).getId())));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.GET, value = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<AutomationRuleHit>> getRule(@PathVariable("id") UUID id,
                                                                  @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(automationRuleHitsClient.getAutomationRuleHit(company, id.toString()))
        );
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<AutomationRuleHit>>> ruleHitsList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() ->
                ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(),
                        automationRuleHitsClient.listAutomationRuleHits(company, filter))));
    }
}
