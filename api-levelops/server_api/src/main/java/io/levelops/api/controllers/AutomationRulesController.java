package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.automation_rules.AutomationRulesClient;
import io.levelops.commons.databases.models.database.automation_rules.AutomationRule;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/automation_rules")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@Log4j2
public class AutomationRulesController {
    private AutomationRulesClient automationRulesClient;

    @Autowired
    public AutomationRulesController(AutomationRulesClient automationRulesClient) {
        this.automationRulesClient = automationRulesClient;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_CREATE)
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createRule(@RequestBody AutomationRule rule,
                                                                          @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(Map.of("id", automationRulesClient.createAutomationRule(company, rule).getId())));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_DELETE)
    @RequestMapping(method = RequestMethod.DELETE, value = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> deleteRule(@PathVariable("id") String ruleId,
                                                                     @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(automationRulesClient.deleteAutomationRule(company, ruleId)));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_DELETE)
    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> deleteRules(@RequestBody List<String> ruleIds,
                                                                          @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() ->
                ResponseEntity.ok((automationRulesClient.deleteAutomationRules(company, ruleIds))));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_DELETE)
    @RequestMapping(method = RequestMethod.PUT, value = "/{ruleid}", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> ruleUpdate(@RequestBody AutomationRule rule,
                                                                          @PathVariable("ruleid") String ruleId,
                                                                          @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(Map.of("id", automationRulesClient.updateAutomationRule(company, ruleId, rule).getId())));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.GET, value = "/{ruleid}", produces = "application/json")
    public DeferredResult<ResponseEntity<AutomationRule>> getRule(@PathVariable("ruleid") String ruleid,
                                                                  @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(automationRulesClient.getAutomationRule(company, ruleid))
        );
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<AutomationRule>>> rulesList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            PaginatedResponse<AutomationRule> internalApiResponse = automationRulesClient.listAutomationRules(company, filter);
            log.debug("internalApiResponse = {}", internalApiResponse);
            return ResponseEntity.ok(internalApiResponse);
        });
    }
}
