package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.services.EventTypesDatabaseService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/v1/tenants/{company}/events")
public class EventsController {

    private final EventTypesDatabaseService eventTypeDBService;

    public EventsController(final EventTypesDatabaseService eventTypeDBService){
        this.eventTypeDBService = eventTypeDBService;
    }

    /**
     * Retrieves an event type with all its metadata.
     * 
     * @param company tenat
     * @param eventType the type to retrieve
     * @return
     */
    @GetMapping(path = "/types/{type}", produces = "application/json")
    public DeferredResult<ResponseEntity<EventType>> getEventType(
                    @PathVariable("company") String company, 
                    @PathVariable("type") String eventType) {
        return SpringUtils.deferResponse(() -> 
            ResponseEntity.ok(eventTypeDBService.get(company, eventType).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find the event type: " + eventType)
            ))
        );
    }

    /**
     * Retrieves an event type with all its metadata.
     * 
     * @param company tenat
     * @param search the search criteria
     * @return
     */
    @PostMapping(path = "/types/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<EventType>>> listEventTypes(
                    @PathVariable("company") String company, 
                    @RequestBody DefaultListRequest search) {
        return SpringUtils.deferResponse(() -> 
            ResponseEntity.ok(PaginatedResponse.of(search.getPage(), search.getPageSize(), eventTypeDBService.list(
                company, 
                search.getPage(), 
                search.getPageSize(), 
                QueryFilter.fromRequestFilters(search.getFilter()), 
                SortingConverter.fromFilter(search.getSort()))
            )
        ));
    }
    
}