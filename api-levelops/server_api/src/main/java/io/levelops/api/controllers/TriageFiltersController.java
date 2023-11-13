package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.StoredFilter;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.triage.clients.StoredFiltersRESTClient;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
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

import java.util.List;

@RestController
@RequestMapping("/v1/triage_filters")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@Log4j2
@SuppressWarnings("unused")
public class TriageFiltersController {

    private final static String TYPE = "triage";
    private final StoredFiltersRESTClient restClient;

    @Autowired
    public TriageFiltersController(StoredFiltersRESTClient restClient) {
        this.restClient = restClient;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @RequestMapping(method = RequestMethod.PUT, value = "/{name}", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> upsertFilter(@RequestBody StoredFilter filter,
                                                               @PathVariable("name") String name,
                                                               @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                restClient.upsertStoredFilter(company, TYPE, filter, name)));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.GET, value = "/{name}", produces = "application/json")
    public DeferredResult<ResponseEntity<StoredFilter>> getFilter(@SessionAttribute(name = "company") String company,
                                                                  @PathVariable("name") String name) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(restClient.getStoredFilter(company, TYPE, name)));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @RequestMapping(method = RequestMethod.DELETE, value = "/{name}", produces = "application/json")
    public DeferredResult<ResponseEntity<DeleteResponse>> deleteFilter(@SessionAttribute(name = "company") String company,
                                                                       @PathVariable("name") String name) {
        return SpringUtils.deferResponse(() -> {
            var response = restClient.deleteStoredFilter(company, TYPE, name);
            return ResponseEntity.ok(response);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> bulkDeleteFilters(@SessionAttribute(name = "company") String company,
                                                                                @RequestBody List<String> names) {
        return SpringUtils.deferResponse(() -> {
            var responses = restClient.bulkDeleteStoredFilters(company, TYPE, names);
            return ResponseEntity.ok(responses);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<StoredFilter>>> filtersList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() ->
                ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(),
                        restClient.listStoredFilters(company, TYPE, filter))));
    }
}
