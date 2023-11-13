package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.databases.services.TenantService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Map;

@RestController
@RequestMapping("/internal/v1/tenants")
public class TenantsController {
    private ObjectMapper objectMapper;
    private TenantService tenantService;

    @Autowired
    public TenantsController(@Qualifier("custom") ObjectMapper objectMapper, TenantService tenantService) {
        this.objectMapper = objectMapper;
        this.tenantService = tenantService;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createTenant(@RequestBody Tenant company) {
        return SpringUtils.deferResponse(() -> {
            String result = tenantService.insert(null, company);
            return ResponseEntity.accepted().body(Map.of("tenant_id", result));
        });
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{company}", produces = "application/json")
    public DeferredResult<ResponseEntity<Tenant>> getTenant(@PathVariable("company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(tenantService.get(null, company)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + company))));
    }

    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<DbListResponse<Tenant>>> listTenants(@RequestBody DefaultListRequest listRequest) {
        return SpringUtils.deferResponse(() -> {
            DbListResponse<Tenant> tenants = tenantService.list(null, listRequest.getPage(),
                    listRequest.getPageSize());
            return ResponseEntity.ok(tenants);
        });
    }

}
