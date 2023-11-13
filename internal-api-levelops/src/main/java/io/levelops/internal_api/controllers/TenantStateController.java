package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.TenantState;
import io.levelops.commons.databases.services.TenantStateService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.web.util.SpringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Map;

@RestController
@RequestMapping("/internal/v1/tenants/{company}/state")
public class TenantStateController {
    private ObjectMapper objectMapper;
    private TenantStateService tenantStateService;

    @Autowired
    public TenantStateController(@Qualifier("custom") ObjectMapper objectMapper, TenantStateService tenantStateService) {
        this.objectMapper = objectMapper;
        this.tenantStateService = tenantStateService;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createTenantState(@RequestBody TenantState state,
                                                                            @PathVariable(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            String result = tenantStateService.insert(company, state);
            return ResponseEntity.accepted().body(Map.of("state_id", result));
        });
    }

    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    public DeferredResult<ResponseEntity<DbListResponse<TenantState>>> getTenantStates(@PathVariable("company") String company) {
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok(tenantStateService.listByFilter(company,null,0,0,true));
        });
    }
}
