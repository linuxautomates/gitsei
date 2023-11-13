package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.TriggerSchema;
import io.levelops.commons.databases.models.database.TriggerType;
import io.levelops.commons.databases.services.TriggerSchemasDatabaseService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Log4j2
@RestController
@RequestMapping("/v1/tenants/{company}/triggers/playbooks")
public class TriggerSchemasController {
   
    private final TriggerSchemasDatabaseService triggerSchemasDatabaseService;
    
    public TriggerSchemasController(final TriggerSchemasDatabaseService triggerSchemasDatabaseService){
        this.triggerSchemasDatabaseService = triggerSchemasDatabaseService;
    }

    /**
    * Get a specific trigger schema definition.
    *
    * @param company tenant
    * @param triggerType trigget type
    * @return
    */
    @GetMapping(path = "/schemas/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<TriggerSchema>> getTriggerSchema(
        @PathVariable("company") final String company, 
        @PathVariable("type") final String triggerType) {
        log.info("[{}] Getting Type {}", company, triggerType);
        return SpringUtils.deferResponse(() -> 
            ResponseEntity.ok(triggerSchemasDatabaseService.get(company, triggerType).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "")
            ))
        );
    }

    /**
     * Get a paginated list of trigger schema deffinition.
     * 
     * @param company tenant
     * @param search query options
     * @return
     */
    @PostMapping(path = "/schemas/list", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<PaginatedResponse<TriggerSchema>>> listTriggerSchemas(
        @PathVariable("company") final String company, 
        @RequestBody final DefaultListRequest search) {
        return SpringUtils.deferResponse(() -> 
            ResponseEntity.ok(
                PaginatedResponse.of(
                    search.getPage(), 
                    search.getPageSize(),
                    triggerSchemasDatabaseService.list(
                        company,
                        search.getPage(),
                        search.getPageSize(),
                        QueryFilter.fromRequestFilters(search.getFilter()),
                        SortingConverter.fromFilter(search.getSort())))
        ));
    }

    /**
     * Get a paginated list of trigger schema deffinition.
     * 
     * @param company tenant
     * @return
     */
    @PostMapping(path = "/types", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<List<TriggerType>>> getTriggerTypes(
        @PathVariable("company") final String company) {
        return SpringUtils.deferResponse(() -> 
            ResponseEntity.ok(triggerSchemasDatabaseService.getTriggerTypes(company)));
    }
   
}