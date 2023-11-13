package io.levelops.api.controllers;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;


import io.levelops.commons.databases.models.database.EventType;
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
@RequestMapping("/v1/events")
public class EventsController {
    
    private TriggersRESTClient triggersClient;

    public EventsController(final TriggersRESTClient triggersRESTClient){
        this.triggersClient = triggersRESTClient;
    }

    @GetMapping(value="/types/{type}", produces = "application/json")
    public DeferredResult<ResponseEntity<EventType>> getEventType(@SessionAttribute("company") String company, @PathVariable("type") EventType eventType) {
        return SpringUtils.deferResponse(() -> 
            ResponseEntity.ok(triggersClient.getEventType(company, eventType).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find the event type: " + eventType)
            ))
        );
    }

    @PostMapping(value="/types/list", consumes = "application/json", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<EventType>>> listEventTypes(@SessionAttribute("company") String company, @RequestBody DefaultListRequest search) {
        return SpringUtils.deferResponse(() -> 
            ResponseEntity.ok(triggersClient.listEventTypes(company, search))
        );
    }
    
}