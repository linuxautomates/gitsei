package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.services.StateService;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.State;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
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

import java.util.Collections;
import java.util.Map;

@RestController
@Log4j2
@RequestMapping("/v1/states")
@SuppressWarnings("unused")
public class StatesController {
    private final StateService stateService;

    @Autowired
    public StatesController(StateService stateService) {
        this.stateService = stateService;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createState(@RequestBody State state,
                                                                           @SessionAttribute(name = "session_user") String sessionUser,
                                                                           @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            String stateId = stateService.createState(company, sessionUser, state);
            return ResponseEntity.ok().body(Map.of("id", stateId));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.DELETE, value = "/{stateid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> stateDelete(@PathVariable("stateid") String stateId,
                                                            @SessionAttribute(name = "session_user") String sessionUser,
                                                            @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            Boolean deleted = stateService.deleteState(company, sessionUser, stateId);
            if (BooleanUtils.isTrue(deleted)) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.PUT, value = "/{stateid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> stateUpdate(@RequestBody State state,
                                                                           @SessionAttribute(name = "session_user") String sessionUser,
                                                                           @PathVariable("stateid") String stateId,
                                                                           @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            stateService.updateState(company, sessionUser, stateId, state);
            return ResponseEntity.ok(Map.of("id", stateId));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.GET, value = "/{stateid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<State>> stateDetails(@PathVariable("stateid") String stateId,
                                                              @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok(
                    stateService.getState(company, stateId).orElseThrow(() -> new NotFoundException("State not found: " + stateId)));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','ASSIGNED_ISSUES_USER', 'PUBLIC_DASHBOARD','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<State>>> statesList(@SessionAttribute(name = "company") String company,
                                                                               @RequestBody DefaultListRequest listRequest) {
        return SpringUtils.deferResponse(() -> {
            String partialName = (String) ((Map<String, Object>) listRequest.getFilter()
                    .getOrDefault("partial", Collections.emptyMap()))
                    .getOrDefault("name", null);
            PaginatedResponse<State> paginatedResponse = stateService.getStatesList(company, partialName, listRequest.getPage(), listRequest.getPageSize());
            return ResponseEntity.ok().body(paginatedResponse);
        });
    }
}
