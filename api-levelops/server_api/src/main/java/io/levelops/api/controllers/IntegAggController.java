package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.aggregations.plugins.clients.IntegrationAggregationsClient;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.IntegrationAgg;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
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
import java.util.NoSuchElementException;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'AUDITOR', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
@RequestMapping("/v1/product_aggs")
@SuppressWarnings("unused")
public class IntegAggController {
    private final IntegrationAggregationsClient integrationAggregationsClient;

    @Autowired
    public IntegAggController(IntegrationAggregationsClient integrationAggregationsClient) {
        this.integrationAggregationsClient = integrationAggregationsClient;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.GET, value = "/{aggregationid}", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> getIntegrationAgg(@PathVariable("aggregationid") String aggId,
                                                                    @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            try {
                String integrationAgg = integrationAggregationsClient.getById(company, aggId);
                return ResponseEntity.ok(integrationAgg);
            } catch (NoSuchElementException e) {
                return ResponseEntity.notFound().build();
            }
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<IntegrationAgg>>> integrationAggsList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            String productId = filter.getFilterValue("product_id", String.class)
                    .orElse(null);
            List<String> integrationIds = (List<String>) filter.getFilter().get("integration_ids");
            List<String> integrationTypes = (List<String>)filter.getFilter().get("integration_types");
            if(integrationTypes == null)
                integrationTypes = List.of();
            PaginatedResponse<IntegrationAgg> paginatedResponse = integrationAggregationsClient
                    .getIntegrationAggregationResultsList(company, productId, integrationIds, integrationTypes, filter.getPage(), filter.getPageSize());
            return ResponseEntity.ok(paginatedResponse);
        });
    }
}
