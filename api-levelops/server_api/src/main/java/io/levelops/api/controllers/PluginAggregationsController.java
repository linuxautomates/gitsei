package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.aggregations.models.JenkinsMonitoringAggDataDTO;
import io.levelops.aggregations.plugins.clients.PluginResultAggregationsClient;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.AggregationRecord;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.EnumUtils;
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

import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'AUDITOR','SUPER_ADMIN')")
@RequestMapping("/v1/plugin_aggs")
@SuppressWarnings("unused")
public class PluginAggregationsController {
    private final PluginResultAggregationsClient pluginResultAggregationsClient;

    @Autowired
    public PluginAggregationsController(PluginResultAggregationsClient pluginResultAggregationsClient) {
        this.pluginResultAggregationsClient = pluginResultAggregationsClient;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.GET, value = "/{aggregation_id}", produces = "application/json")
    public DeferredResult<ResponseEntity<JenkinsMonitoringAggDataDTO>> getPluginAgg(@SessionAttribute(name = "company") String company,
                                                                                    @PathVariable("aggregation_id") UUID aggId) {
        return SpringUtils.deferResponse(() -> {
            try {
                JenkinsMonitoringAggDataDTO jenkinsMonitoringAggDataDTO = pluginResultAggregationsClient.getById(company, aggId.toString());
                return ResponseEntity.ok(jenkinsMonitoringAggDataDTO);
            } catch (NoSuchElementException e) {
                return ResponseEntity.notFound().build();
            }
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<AggregationRecord>>> pluginAggsList(@SessionAttribute(name = "company") String company,
                                                                                               @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            String productId = filter.getFilterValue("product_id", String.class)
                    .orElse(null);
            String typeString = filter.getFilterValue("type", String.class)
                    .orElse(null);
            AggregationRecord.Type type = EnumUtils.getEnumIgnoreCase(AggregationRecord.Type.class, typeString);
            String toolType = filter.getFilterValue("tool_type", String.class)
                    .orElse(null);

            PaginatedResponse<AggregationRecord> paginatedResponse = pluginResultAggregationsClient.getPluginAggregationResultsList(company, productId, toolType);
            return ResponseEntity.ok(paginatedResponse);
        });
    }
}
