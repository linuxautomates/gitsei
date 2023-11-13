package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.exceptions.ServerApiException;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.mappings.ProductIntegMapping;
import io.levelops.commons.databases.services.ProductIntegMappingService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.webjars.NotFoundException;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@RequestMapping("/v1/mappings")
@SuppressWarnings("unused")
public class ProductIntegMappingsController {
    private ProductIntegMappingService productIntegMappingService;

    @Autowired
    public ProductIntegMappingsController(ObjectMapper objectMapper, ProductIntegMappingService productIntegMappingService) {
        this.productIntegMappingService = productIntegMappingService;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<String>> createStageMapping(@RequestBody ProductIntegMapping map,
                                                                     @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            if (map.getMappings() == null || map.getIntegrationId() == null || map.getProductId() == null) {
                throw new ServerApiException(HttpStatus.BAD_REQUEST, "mappings, integrationid or productid are not valid.");
            }
            return ResponseEntity.ok(productIntegMappingService.insert(company, map));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @RequestMapping(method = RequestMethod.DELETE, value = "/{mappingid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> deleteMapping(@PathVariable("mappingid") String mappingId,
                                                              @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            productIntegMappingService.delete(company, mappingId);
            return ResponseEntity.ok().build();
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @RequestMapping(method = RequestMethod.PUT, value = "/{mappingid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Boolean>> mappingUpdate(@RequestBody ProductIntegMapping integMapping,
                                                                 @PathVariable("mappingid") String mappingId,
                                                                 @SessionAttribute(name = "company") String company) {
        //we only care about update of mapping field. but also we need id in case it doesnt exist.
        if (StringUtils.isEmpty(integMapping.getMappings())) {
            integMapping = ProductIntegMapping.builder()
                    .id(mappingId)
                    .mappings(integMapping.getMappings())
                    .build();
        }
        final ProductIntegMapping finalMapping = integMapping;
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(productIntegMappingService.update(company, finalMapping)));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.GET, value = "/{mappingid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<ProductIntegMapping>> mappingGet(@PathVariable("mappingid") String mappingId,
                                                                          @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(productIntegMappingService.get(company, mappingId)
                .orElseThrow(() -> new NotFoundException("Mapping not found"))));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<ProductIntegMapping>>> stagesList(@SessionAttribute(name = "company") String company,
                                                                                             @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            String productId = (String) filter.getFilter().get("product_id");
            String integId = (String) filter.getFilter().get("integration_id");
            DbListResponse<ProductIntegMapping> mappings = productIntegMappingService.listByFilter(company,
                    productId, integId, filter.getPage(), filter.getPageSize());
            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), mappings));
        });
    }
}
