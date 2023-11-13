package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.database.StoredFilter;
import io.levelops.commons.databases.services.StoredFiltersService;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Log4j2
@RequestMapping("/internal/v1/tenants/{company}/filters/{type}")
public class StoredFiltersController {

    private final StoredFiltersService storedFiltersService;

    @Autowired
    public StoredFiltersController(StoredFiltersService storedFiltersService) {
        this.storedFiltersService = storedFiltersService;
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{name}", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> upsertFilter(
            @PathVariable("company") String company,
            @PathVariable("name") String name,
            @PathVariable("type") String type,
            @RequestBody StoredFilter filter) {
        return SpringUtils.deferResponse(
                () -> ResponseEntity.accepted().body(Map.of("id",
                        storedFiltersService.insert(company, filter.toBuilder().name(name).type(type).build()))));
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{name}", produces = "application/json")
    public DeferredResult<ResponseEntity<StoredFilter>> getFilter(@PathVariable("company") String company,
                                                                  @PathVariable("type") String type,
                                                                  @PathVariable("name") String name) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                storedFiltersService.get(company, type, name)
                        .orElse(StoredFilter.builder().name(name).filter(Collections.EMPTY_MAP).build())));
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{name}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> deleteFilter(@PathVariable("company") String company,
                                                                       @PathVariable("type") String type,
                                                                       @PathVariable("name") String name) {
        return SpringUtils.deferResponse(() -> {
            try {
                storedFiltersService.delete(company, type, name);
            } catch (Exception e) {
                return ResponseEntity.ok(DeleteResponse.builder()
                        .id(name)
                        .success(false)
                        .error(e.getMessage())
                        .build());
            }
            return ResponseEntity.ok(DeleteResponse.builder()
                    .id(name)
                    .success(true)
                    .build());
        });
    }

    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> bulkDeleteRules(@PathVariable("company") String company,
                                                                              @PathVariable("type") String type,
                                                                              @RequestBody List<String> names) {
        return SpringUtils.deferResponse(() -> {
            try {
                storedFiltersService.bulkDelete(company, type, names);
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(names, true, null));
            } catch (Exception e) {
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(names, false, e.getMessage()));
            }
        });
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<DbListResponse<StoredFilter>>> listTriageFilters(@PathVariable("company") String company,
                                                                                          @PathVariable("type") String type,
                                                                                          @RequestBody DefaultListRequest listRequest) {
        Map<String, Map<String, String>> partialMatch = (Map<String, Map<String, String>>) listRequest.getFilter().get("partial_match");
        validatePartialMatchFilter(company, partialMatch);
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                storedFiltersService.list(company,
                        List.of(type), (List<String>) listRequest.getFilter().get("names"),
                        (Boolean) listRequest.getFilter().get("is_default"),
                        partialMatch,
                        listRequest.getPage(),
                        listRequest.getPageSize())));
    }

    private void validatePartialMatchFilter(String company, Map<String, Map<String, String>> partialMatchMap) {
        if (MapUtils.isEmpty(partialMatchMap)) {
            return;
        }
        ArrayList<String> partialMatchKeys = new ArrayList<>(partialMatchMap.keySet());
        List<String> invalidPartialMatchKeys = partialMatchKeys.stream()
                .filter(key -> !StoredFiltersService.PARTIAL_MATCH_COLUMNS.contains(key))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(invalidPartialMatchKeys)) {
            log.warn("Company - " + company + ": " + String.join(",", invalidPartialMatchKeys)
                    + " are not valid fields for triageFilters partial match based filter");
        }
    }
}