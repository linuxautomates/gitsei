package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.database.Component;
import io.levelops.commons.models.ComponentType;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.services.ComponentsDatabaseService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/tenants/{company}/components")
public class ComponentsController {

    private final ComponentsDatabaseService componentsDatabaseService;

    public ComponentsController(final ComponentsDatabaseService componentsDatabaseService){
        this.componentsDatabaseService = componentsDatabaseService;
    }

    /**
     * Get a specific component by its type and name.
     * @param company tenant
     * @param type the type of the component (ex. integration)
     * @param name the name of the component (ex. jira)
     * @return
     */
    @GetMapping(path = "/{type}/{name}", produces = "application/json")
    public DeferredResult<ResponseEntity<Component>> getComponent(
                    @PathVariable("company") String company,
                    @PathVariable("type") String type,
                    @PathVariable("name") String name) {
        return SpringUtils.deferResponse(() -> 
            ResponseEntity.ok(
                componentsDatabaseService.getByTypeName(company, ComponentType.fromString(type), name).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find a component '" + name + "' of '" + type + "' type.")
                )
            ));
    }

    /**
     * Gets the list of component that match the specified type.
     * 
     * @param company tenant
     * @param type the component type to query for (ex. integration)
     * @return
     */
    @GetMapping(path = "/types/{type}", produces = "application/json")
    public DeferredResult<ResponseEntity<List<Component>>> getComponentByType(
                @PathVariable("company") String company,
                @PathVariable("type") String type) {
        return SpringUtils.deferResponse(() -> {
            List<Component> components = new ArrayList<>();
            boolean hasMore = true;
            int page = 0;
            int pageSize = 100;
            do {
                var tmp = componentsDatabaseService.list(
                            company, 
                            page, 
                            pageSize,
                            QueryFilter.builder().strictMatch("type", type).build(),
                            Map.<String, SortingOrder>of()
                        );
                hasMore = ((page * pageSize) + tmp.getCount()) < tmp.getTotalCount();
                components.addAll(tmp.getRecords());
                page++;
            } while (hasMore);
            return ResponseEntity.ok(components);
        });
    }

    /**
     * Get the list of component types.
     * 
     * @param company tenant
     * @return
     */
    @GetMapping(path = "/types", produces = "application/json")
    public DeferredResult<ResponseEntity<List<ComponentType>>> getComponentTypes(@PathVariable("company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(componentsDatabaseService.getComponentTypes(company)));
    }
    
    /**
     * Gets a paginated list of components based on the request parameters.
     * 
     * @param company tenant
     * @param search request parameters
     * @return
     */
    @PostMapping(path = "/list", produces = "application/json", consumes = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Component>>> listComponents(
                @PathVariable("company") String company,
                @RequestBody DefaultListRequest search) {
        return SpringUtils.deferResponse(() ->
            ResponseEntity.ok(PaginatedResponse.of(search.getPage(), search.getPageSize(), 
                componentsDatabaseService.list(
                    company, 
                    search.getPage(),
                    search.getPageSize(),
                    QueryFilter.fromRequestFilters(search.getFilter()), 
                    SortingConverter.fromFilter(search.getSort())
                    )
                )
            ));
    }
}