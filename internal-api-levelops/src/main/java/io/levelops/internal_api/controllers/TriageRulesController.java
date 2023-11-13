package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.database.TriageRule;
import io.levelops.commons.databases.services.TriageRulesService;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@RestController
@Log4j2
@RequestMapping("/internal/v1/tenants/{company}/triage_rules")
public class TriageRulesController {
    private final TriageRulesService triageRulesService;

    @Autowired
    public TriageRulesController(TriageRulesService triageRulesService) {
        this.triageRulesService = triageRulesService;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createRule(
            @PathVariable("company") String company,
            @RequestBody TriageRule rule) {
        return SpringUtils.deferResponse(() -> {
            //verify regexes
            for (String regex : rule.getRegexes())
                try {
                    Pattern.compile(regex);
                } catch (PatternSyntaxException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Regex Pattern is invalid: " + regex);
                }
            //save good regexes
            return ResponseEntity.accepted().body(Map.of("id", triageRulesService.insert(company, rule)));
        });
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{rule_id}", produces = "application/json")
    public DeferredResult<ResponseEntity<TriageRule>> getRule(@PathVariable("company") String company,
                                                              @PathVariable("rule_id") UUID ruleId) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                triageRulesService.get(company, ruleId.toString())
                        .orElseThrow(() -> new NotFoundException("Triage Rule not found: " + company))));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{rule_id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, Object>>> updateRule(@PathVariable("company") String company,
                                                                          @PathVariable("rule_id") UUID ruleId,
                                                                          @RequestBody TriageRule rule) {
        return SpringUtils.deferResponse(() -> {
            //verify regexes
            for (String regex : rule.getRegexes())
                try {
                    Pattern.compile(regex);
                } catch (PatternSyntaxException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Regex Pattern is invalid: " + regex);
                }
            //save good regexes
            return ResponseEntity.ok(
                    Map.of("ok", triageRulesService.update(company, rule.toBuilder().id(ruleId.toString()).build())));
        });
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{rule_id}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> deleteRule(@PathVariable("company") String company,
                                                                     @PathVariable("rule_id") UUID ruleId) {
        return SpringUtils.deferResponse(() -> {
            try {
                triageRulesService.delete(company, ruleId.toString());
            } catch (Exception e) {
                return ResponseEntity.ok(DeleteResponse.builder()
                        .id(ruleId.toString())
                        .success(false)
                        .error(e.getMessage())
                        .build());
            }
            return ResponseEntity.ok(DeleteResponse.builder()
                    .id(ruleId.toString())
                    .success(true)
                    .build());
        });
    }

    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> bulkDeleteRules(@PathVariable("company") String company,
                                                                              @RequestBody List<UUID> uuids) {
        final List<String> ids = uuids.stream().map(UUID::toString).collect(Collectors.toList());
        return SpringUtils.deferResponse(() -> {
            try {
                triageRulesService.bulkDelete(company, ids);
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(ids, true, null));
            } catch (Exception e) {
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(ids, false, e.getMessage()));
            }
        });
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<DbListResponse<TriageRule>>> listRules(
            @PathVariable("company") String company,
            @RequestBody DefaultListRequest listRequest) {
        Map<String, Object> partial = (Map<String, Object>) listRequest.getFilter().get("partial");
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                triageRulesService.list(company,
                        (List<String>) listRequest.getFilter().get("rule_ids"),
                        (List<String>) listRequest.getFilter().get("applications"),
                        (List<String>) listRequest.getFilter().get("owners"),
                        (String) (partial != null ? partial.get("name") : null),
                        listRequest.getPage(),
                        listRequest.getPageSize())));
    }

}
