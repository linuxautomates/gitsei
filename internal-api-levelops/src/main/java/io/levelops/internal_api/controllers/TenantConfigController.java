package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.web.util.SpringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping("/internal/v1/tenant_config")
public class TenantConfigController {
    private final TenantConfigService tenantConfigService;

    @Autowired
    public TenantConfigController(@Qualifier("custom") ObjectMapper objectMapper, TenantConfigService tenantService) {
        this.tenantConfigService = tenantService;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{company}", produces = "application/json")
    public DeferredResult<ResponseEntity<DbListResponse<TenantConfig>>> getConfig(
            @PathVariable("company") String company,
            @RequestParam(name = "config_key") String configKey) {
        return SpringUtils.deferResponse(() -> {
            var dbListResponse= tenantConfigService.listByFilter(company, configKey, 0, 1);
            return ResponseEntity.ok(dbListResponse);
        });
    }
}
