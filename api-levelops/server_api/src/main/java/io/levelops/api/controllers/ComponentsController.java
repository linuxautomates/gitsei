package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import io.levelops.commons.databases.models.database.Component;
import io.levelops.commons.models.ComponentType;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.triggers.clients.TriggersRESTClient;
import io.levelops.web.util.SpringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;


@RestController
@RequestMapping("/v1/components")
public class ComponentsController {
    
    private final TriggersRESTClient triggersClient;

    public ComponentsController(final TriggersRESTClient triggersClient){
        this.triggersClient = triggersClient;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.COLLECTIONS_VIEW)
    @PostMapping(value="/{type}", consumes = "application/json", produces = "application/json")
    public DeferredResult<ResponseEntity<List<Component>>> listComponentsByType(@SessionAttribute("company") String company, @PathVariable("type") ComponentType type) {
        return SpringUtils.deferResponse(() -> 
            ResponseEntity.ok(triggersClient.getComponentsByType(company, type))
        );
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @GetMapping(value="/{type}/{name}", produces = "application/json")
    public DeferredResult<ResponseEntity<Component>> getComponent(@SessionAttribute("company") String company, @PathVariable("type") ComponentType type, @PathVariable("name") String name) {
        return SpringUtils.deferResponse(() -> 
            ResponseEntity.ok(triggersClient.getComponent(company, type, name).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find a component for the type '" + type + "' and name '" + name + "'")
            ))
        );
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PostMapping(value="/types/list", consumes = "application/json", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<ComponentType>>> listComponentTypes(@SessionAttribute("company") String company, @RequestBody DefaultListRequest search) {
        return SpringUtils.deferResponse(() -> 
            ResponseEntity.ok(PaginatedResponse.of(search.getPage(), search.getPageSize(), triggersClient.getComponentTypes(company)))
        );
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PostMapping(value="/list", consumes = "application/json", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Component>>> listComponents(@SessionAttribute("company") String company, @RequestBody DefaultListRequest search) {
        return SpringUtils.deferResponse(() -> 
            ResponseEntity.ok(triggersClient.listComponents(company, search))
        );
    }
    
}