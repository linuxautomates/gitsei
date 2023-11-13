package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.database.automation_rules.AutomationRule;
import io.levelops.commons.databases.models.database.automation_rules.Criterea;
import io.levelops.commons.databases.models.database.automation_rules.ObjectType;
import io.levelops.commons.databases.models.filters.DefaultListRequestUtils;
import io.levelops.commons.databases.services.automation_rules.AutomationRulesDatabaseService;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@RestController
@Log4j2
@RequestMapping("/internal/v1/tenants/{company}/automation_rules")
public class AutomationRulesController {
    private AutomationRulesDatabaseService automationRulesDatabaseService;

    @Autowired
    public AutomationRulesController(AutomationRulesDatabaseService automationRulesDatabaseService) {
        this.automationRulesDatabaseService = automationRulesDatabaseService;
    }

    private void validate(AutomationRule rule) throws BadRequestException {
        if (StringUtils.isBlank(rule.getName())) {
            throw new BadRequestException(String.format("Rule name cannot be blank!"));
        }
        if(CollectionUtils.isEmpty(rule.getCritereas())) {
            throw new BadRequestException(String.format("Rule %s should have atleast one criterea", rule.getName()));
        }
        if(rule.getObjectType() == null) {
            throw new BadRequestException(String.format("Rule object type cannot be blank!"));
        }
        for(Criterea c : rule.getCritereas()) {
            if(CollectionUtils.isEmpty(c.getRegexes())) {
                throw new BadRequestException(String.format("Criterea needs to have atleast one regex %s", c.toString()));
            }
            for(String regex : c.getRegexes()) {
                if(StringUtils.isBlank(regex)) {
                    throw new BadRequestException(String.format("Criterea regex should not be blank %s", c.toString()));
                }
                try {
                    Pattern.compile(regex);
                } catch (PatternSyntaxException e) {
                    throw new BadRequestException(String.format("Criterea regex %s is not valid. Error %s", regex, e.getMessage()));
                }
            }
        }
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createRule(@PathVariable("company") String company,
                                                                          @RequestBody AutomationRule rule) {
        return SpringUtils.deferResponse(() -> {
            validate(rule);
            return ResponseEntity.ok(Map.of("id", automationRulesDatabaseService.insert(company, rule)));
        });
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> deleteRule(@PathVariable("company") String company,
                                                                     @PathVariable("id") String id) {
        return SpringUtils.deferResponse(() -> {
            try {
                automationRulesDatabaseService.delete(company, id);
            } catch (Exception e) {
                return ResponseEntity.ok(DeleteResponse.builder().id(id).success(false).error(e.getMessage()).build());
            }
            return ResponseEntity.ok(DeleteResponse.builder().id(id).success(true).build());
        });
    }

    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> deleteRules(@PathVariable("company") String company,
                                                                          @RequestBody List<String> ids) {
        return SpringUtils.deferResponse(() -> {
            try {
                automationRulesDatabaseService.bulkDelete(company, ids);
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(ids, true, null));
            } catch (Exception e) {
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(ids, false, e.getMessage()));
            }
        });
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> ruleUpdate(@PathVariable("company") String company,
                                                                          @PathVariable("id") String id,
                                                                          @RequestBody AutomationRule rule) {
        return SpringUtils.deferResponse(() -> {
            validate(rule);
            automationRulesDatabaseService.update(company, rule);
            return ResponseEntity.ok(Map.of("id", id));
        });
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<AutomationRule>> getRule(@PathVariable("company") String company,
                                                                  @PathVariable("id") String id) {
        return SpringUtils.deferResponse(() -> {
            AutomationRule rule = automationRulesDatabaseService.get(company, id)
                    .orElseThrow(() -> new NotFoundException("Could not find Automation Rule with id=" + id));
            return ResponseEntity.ok(rule);
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<AutomationRule>>> rulesList(
            @PathVariable("company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            List<UUID> ids = CollectionUtils.emptyIfNull(DefaultListRequestUtils.getListOrDefault(filter, "ids")).stream().filter(Objects::nonNull).map(UUID::fromString).collect(Collectors.toList());
            List<ObjectType> objectTypes = CollectionUtils.emptyIfNull(DefaultListRequestUtils.getListOrDefault(filter, "object_types")).stream().map(x -> ObjectType.fromString(x)).collect(Collectors.toList());
            String namePartial = filter.getFilterValue("partial", Map.class).map(map -> (String) map.get("name")).orElse(null);

            log.debug("ids {}", ids);
            log.debug("objectTypes {}", objectTypes);
            log.debug("namePartial {}", namePartial);
            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(),
                    automationRulesDatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), ids, objectTypes, namePartial)));
        });
    }
}
