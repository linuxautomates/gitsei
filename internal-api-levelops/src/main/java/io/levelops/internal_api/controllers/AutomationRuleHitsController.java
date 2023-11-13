package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.automation_rules.AutomationRuleHit;
import io.levelops.commons.databases.models.database.automation_rules.ObjectType;
import io.levelops.commons.databases.models.filters.DefaultListRequestUtils;
import io.levelops.commons.databases.models.filters.ObjectTypeObjectId;
import io.levelops.commons.databases.services.automation_rules.AutomationRuleHitsDatabaseService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
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
import java.util.stream.Collectors;

@RestController
@Log4j2
@RequestMapping("/internal/v1/tenants/{company}/automation_rule_hits")
public class AutomationRuleHitsController {
    private AutomationRuleHitsDatabaseService automationRuleHitsDatabaseService;
    private final ObjectMapper mapper;

    @Autowired
    public AutomationRuleHitsController(AutomationRuleHitsDatabaseService automationRuleHitsDatabaseService, ObjectMapper mapper) {
        this.automationRuleHitsDatabaseService = automationRuleHitsDatabaseService;
        this.mapper = mapper;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createRule(@PathVariable("company") String company,
                                                                          @RequestBody AutomationRuleHit ruleHit) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(Map.of("id", automationRuleHitsDatabaseService.upsert(company, ruleHit))));
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> deleteRule(@PathVariable("company") String company,
                                                           @PathVariable("id") String id) {
        return SpringUtils.deferResponse(() -> {
            automationRuleHitsDatabaseService.delete(company, id);
            return ResponseEntity.ok().build();
        });
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> ruleUpdate(@PathVariable("company") String company,
                                                                          @PathVariable("id") String id,
                                                                          @RequestBody AutomationRuleHit ruleHit) {
        return SpringUtils.deferResponse(() -> {
            automationRuleHitsDatabaseService.update(company, ruleHit);
            return ResponseEntity.ok(Map.of("id", id));
        });
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<AutomationRuleHit>> getRule(@PathVariable("company") String company,
                                                                  @PathVariable("id") String id) {
        return SpringUtils.deferResponse(() -> {
            AutomationRuleHit ruleHit = automationRuleHitsDatabaseService.get(company, id)
                    .orElseThrow(() -> new NotFoundException("Could not find Automation Rule Hit with id=" + id));
            return ResponseEntity.ok(ruleHit);
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<AutomationRuleHit>>> rulesList(
            @PathVariable("company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            /*
            {
              "page": 0,
              "page_size": 100,
              "filter": {
                "object_type_object_ids": [
                  {
                    "object_type": "JIRA_ISSUE",
                    "object_id": "e1806b98-b976-4e0f-a647-243a7d2548b6"
                  }
                ]
              }
            }
             */
            log.debug("filter {}", mapper.writeValueAsString(filter));
            List<UUID> ids = CollectionUtils.emptyIfNull(DefaultListRequestUtils.getListOrDefault(filter, "ids")).stream().filter(Objects::nonNull).map(UUID::fromString).collect(Collectors.toList());
            List<ObjectType> objectTypes = CollectionUtils.emptyIfNull(DefaultListRequestUtils.getListOrDefault(filter, "object_types")).stream().map(x -> ObjectType.fromString(x)).collect(Collectors.toList());
            List<String> objectIds = DefaultListRequestUtils.getListOrDefault(filter, "object_ids");
            List<UUID> ruleIds = CollectionUtils.emptyIfNull(DefaultListRequestUtils.getListOrDefault(filter, "rule_ids")).stream().filter(Objects::nonNull).map(UUID::fromString).collect(Collectors.toList());
            List<ObjectTypeObjectId> objectTypeObjectIds = DefaultListRequestUtils.getListOfObjectOrDefault(filter, mapper, "object_type_object_ids", ObjectTypeObjectId.class);
            List<ImmutablePair<ObjectType, String>> objectTypeObjectPairs = CollectionUtils.emptyIfNull(objectTypeObjectIds).stream().map(x -> ImmutablePair.of(x.getObjectType(), x.getObjectId())).collect(Collectors.toList());


            log.debug("ids {}", ids);
            log.debug("objectTypes {}", objectTypes);
            log.debug("objectIds {}", objectIds);
            log.debug("ruleIds {}", ruleIds);
            log.debug("objectTypeObjectIds {}", objectTypeObjectIds);
            log.debug("objectTypeObjectPairs {}", objectTypeObjectPairs);

            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(),
                            automationRuleHitsDatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), ids, objectTypes, objectIds, objectTypeObjectPairs, ruleIds)));
        });
    }
}
