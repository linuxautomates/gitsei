package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import io.levelops.commons.databases.models.database.Id;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.workitems.CreateSnippetWorkitemRequestWithText;
import io.levelops.commons.databases.models.filters.WorkItemFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.events.clients.EventsClient;
import io.levelops.internal_api.services.WorkItemService;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@RestController
@RequestMapping("/internal/v1/tenants/{company}/workitems")
public class WorkItemsController {

    private final String UUID_PATTERN = "[0-9A-fa-f]{8}-[0-9A-Fa-f]{4}-4[0-9A-Fa-f]{3}-[89ABab][0-9A-Fa-f]{3}-[0-9A-Fa-f]{12}";

    private final Storage storage;
    private final ObjectMapper objectMapper;
    private final EventsClient eventsClient;
    private final WorkItemService workItemService;

    /**
     * Controller for work items.
     *
     * @param workItemService workItems service
     * @param storage         storage service
     * @param objectMapper    serialization object
     * @param eventsClient    events client
     */
    @Autowired
    public WorkItemsController(WorkItemService workItemService, Storage storage,
                               ObjectMapper objectMapper, EventsClient eventsClient) {
        this.workItemService = workItemService;
        this.storage = storage;
        this.objectMapper = objectMapper;
        this.eventsClient = eventsClient;
    }

    /**
     * POST - Creates a workitems object.
     *
     * @param company  the tenenat
     * @param workItem the work item to be persisted
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<WorkItem>> createWorkItem(@PathVariable("company") final String company,
                                                                   @Valid @RequestBody WorkItem workItem) {
        return SpringUtils.deferResponse(() -> {
            WorkItem createdWi = workItemService.createWorkItem(company, workItem);
            return ResponseEntity.ok().body(createdWi);
        });
    }

    @RequestMapping(method = RequestMethod.POST, path = "/snippet_workitems/multipart", produces = "application/json")
    public DeferredResult<ResponseEntity<WorkItem>> createSnippetWorkItemMultipart(@PathVariable(name = "company") String company,
                                                                                   @RequestPart("json") MultipartFile createSnippetWorkItemRequest,
                                                                                   @RequestPart(name = "file") MultipartFile snippetFile) {
        return SpringUtils.deferResponse(() -> {
            WorkItem createdWi = workItemService.createSnippetWorkItemMultipart(company, createSnippetWorkItemRequest, snippetFile);
            return ResponseEntity.ok().body(createdWi);
        });
    }

    @RequestMapping(method = RequestMethod.POST, path = "/snippet_workitems", produces = "application/json")
    public DeferredResult<ResponseEntity<WorkItem>> createSnippetWorkItem(@PathVariable(name = "company") String company,
                                                                          @Valid @RequestBody CreateSnippetWorkitemRequestWithText request) {
        return SpringUtils.deferResponse(() -> {
            log.debug("createSnippetWorkitemRequestWithText = {}", request);
            WorkItem createdWi = workItemService.createSnippetWorkItem(company, request);
            return ResponseEntity.ok().body(createdWi);
        });
    }

    /**
     * GET - Retrieves a workitem object.
     *
     * @param company the tenenat
     * @param id      the id of the work item
     * @return
     */
    @GetMapping(path = "/{id:" + UUID_PATTERN + "}", produces = "application/json")
    public DeferredResult<ResponseEntity<WorkItem>> workitemsDetails(@PathVariable("company") final String company,
                                                                     @PathVariable("id") final UUID id) {
        log.debug("id = {}", id);
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(workItemService.getWorkItem(company, id)
                .orElseThrow(() -> new NotFoundException("WorkItem not found. id : " + id))));
    }

    /**
     * GET by Vanity Id - Retrieves a workitem object.
     *
     * @param company  the tenenat
     * @param vanityId the vanity id value
     * @return
     */
    @RequestMapping(method = RequestMethod.GET, path = "/vanity-id/{vanity-id}", produces = "application/json")
    public DeferredResult<ResponseEntity<WorkItem>> workitemsDetailsByVanityId(@PathVariable("company") final String company,
                                                                               @PathVariable("vanity-id") final String vanityId) {
        log.debug("vanityId = {}", vanityId);
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(workItemService.getWorkItemByVanityId(company, vanityId)
                .orElseThrow(() -> new NotFoundException("WorkItem not found. vanity-id : " + vanityId))));
    }

    /**
     * PUT - Updates a workitem object.
     *
     * @param company the tenenat
     * @return
     */
    @PutMapping(path = "/{id:" + UUID_PATTERN + "}", produces = "application/json")
    public DeferredResult<ResponseEntity<Id>> updateWorkItem(@PathVariable("company") final String company,
                                                             @PathVariable("id") final UUID id,
                                                             @RequestBody final WorkItem workitem) {
        return SpringUtils.deferResponse(() -> {
            Boolean result = workItemService.updateWorkItem(company, id, workitem);
            if (BooleanUtils.isTrue(result)) {
                return ResponseEntity.accepted().body(Id.from(id.toString()));
            } else {
                return ResponseEntity.notFound().build();
            }
        });
    }

    /**
     * PATCH - Updates the product id associated to a workitem object.
     *
     * @param company the tenenat
     * @return
     */
    @PatchMapping(path = "/{id:" + UUID_PATTERN + "}/product/{product_id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Id>> workItemChangeProduct(@PathVariable("company") final String company,
                                                                    @PathVariable("id") final UUID id,
                                                                    @PathVariable("product_id") final String newProductId) {
        return SpringUtils.deferResponse(() -> {
            workItemService.changeProductId(company, id, newProductId);
            return ResponseEntity.accepted().body(Id.from(id.toString()));
        });
    }

    /**
     * PATCH - Updates the parent id of a workitem object.
     *
     * @param company the tenenat
     * @return
     */
    @PatchMapping(path = "/{id:" + UUID_PATTERN + "}/parent/{parent_id:" + UUID_PATTERN + "}", produces = "application/json")
    public DeferredResult<ResponseEntity<Id>> workItemChangeParentId(@PathVariable("company") final String company,
                                                                     @PathVariable("id") final UUID id,
                                                                     @PathVariable("parent_id") final UUID newParentId) {
        return SpringUtils.deferResponse(() -> {
            workItemService.changeParentId(company, id, newParentId);
            return ResponseEntity.accepted().body(Id.from(id.toString()));
        });
    }

    /**
     * PATCH - Updates the product id associated to a workitem object.
     *
     * @param company the tenenat
     * @return
     */
    @PatchMapping(path = "/{id:" + UUID_PATTERN + "}/state/{state_name}", produces = "application/json")
    public DeferredResult<ResponseEntity<Id>> workItemChangeState(@PathVariable("company") final String company,
                                                                  @PathVariable("id") final UUID id,
                                                                  @PathVariable("state_name") final String stateName) {
        return SpringUtils.deferResponse(() -> {
            workItemService.changeState(company, id, stateName);
            return ResponseEntity.accepted().body(Id.from(id.toString()));
        });
    }

    /**
     * DELETE - Deletes a workitem object.
     *
     * @param company the tenenat
     * @param id      the id of the work item to be deleted
     * @return
     */
    @DeleteMapping(path = "/{id:" + UUID_PATTERN + "}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> workItemsDelete(@PathVariable("company") final String company,
                                                                          @PathVariable("id") final UUID id) {
        return SpringUtils.deferResponse(() -> {
            // using role admin for the time being since it is an internal action... not validated by user type.. at this time.
            try {
                workItemService.deleteWorkItem(company, "", RoleType.ADMIN, id);
            }
            catch (NotFoundException | UnsupportedOperationException e) {
                return ResponseEntity.ok(DeleteResponse.builder()
                        .id(id.toString())
                        .success(false)
                        .error(e.getMessage())
                        .build());
            }
            return ResponseEntity.ok(DeleteResponse.builder()
                    .id(id.toString())
                    .success(true)
                    .build());
        });
    }

    @DeleteMapping(produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> workItemsBulkDelete(@PathVariable("company") final String company,
                                                                                  @RequestBody List<UUID> ids) {
        return SpringUtils.deferResponse(() -> {
            List<DeleteResponse> records = workItemService.bulkDeleteWorkItems(company, "", RoleType.ADMIN, ids);
                return ResponseEntity.ok(BulkDeleteResponse.of(records));
        });
    }
    /**
     * Gets a paginated list of work items.
     *
     * @param company the tenenat
     * @param filter  the seatch criteria
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, path = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<WorkItem>>> workItemsList(@PathVariable("company") final String company,
                                                                                     @RequestBody final DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            DbListResponse<WorkItem> results = workItemService.listWorkItems(company, filter);
            PaginatedResponse<WorkItem> response = PaginatedResponse.of(filter.getPage(),
                    filter.getPageSize(), results);

            return ResponseEntity.ok().body(response);
        });
    }
    // endregion

    @PostMapping("/aggregate")
    public DeferredResult<ResponseEntity<DbListResponse<DbAggregationResult>>> stackedAggregate(@PathVariable("company") String company,
                                                                                                @RequestParam("calculation") String calculation,
                                                                                                @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            WorkItemFilter questionnaireFilter = (request.getFilter() != null)
                    ? objectMapper.convertValue(request.getFilter(), WorkItemFilter.class)
                    : WorkItemFilter.builder().build();
            var stacks = ListUtils.emptyIfNull(request.getStacks()).stream()
                    .map(WorkItemFilter.Distinct::valueOf)
                    .collect(Collectors.toList());
            questionnaireFilter = questionnaireFilter.toBuilder()
                    .across(WorkItemFilter.Distinct.fromString(request.getAcross()))
                    .calculation(WorkItemFilter.Calculation.fromString(calculation))
                    .build();
            return ResponseEntity.ok(workItemService.stackedAggregate(company, questionnaireFilter, stacks));
        });
    }
}