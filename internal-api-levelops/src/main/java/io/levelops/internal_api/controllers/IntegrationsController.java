package io.levelops.internal_api.controllers;

import io.harness.atlassian_connect.exceptions.AtlassianConnectServiceClientException;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.mappings.ProductIntegMapping;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ProductIntegMappingService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.inventory.SecretsManagerServiceClient;
import io.levelops.commons.inventory.exceptions.SecretsManagerServiceClientException;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.internal_api.services.IntegrationSecretsService;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Log4j2
@RequestMapping("/internal/v1/tenants/{company}/integrations")
public class IntegrationsController {

    private IntegrationService integrationService;
    private TagItemDBService tagItemDBService;
    private final ProductIntegMappingService productIntegMappingService;
    private final IntegrationSecretsService integrationSecretsService;

    @Autowired
    public IntegrationsController(IntegrationService service, TagItemDBService tagItemDBService,
                                  ProductIntegMappingService productIntegMappingService,
                                  IntegrationSecretsService integrationSecretsService) {
        this.integrationService = service;
        this.tagItemDBService = tagItemDBService;
        this.productIntegMappingService = productIntegMappingService;
        this.integrationSecretsService = integrationSecretsService;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    @SuppressWarnings({"rawtypes"})
    public DeferredResult<ResponseEntity<Map>> createIntegration(@PathVariable("company") String company,
                                                                 @RequestBody Integration integration) {
        return SpringUtils.deferResponse(() -> {
            String integrationId = integrationService.insert(company, integration);
            insertTagItemIds(company, integrationId, integration.getTags());
            return ResponseEntity.accepted().body(Map.of("integration_id", integrationId));
        });
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{integrationid:[0-9]+}", produces = "application/json")
    @SuppressWarnings({"rawtypes"})
    public DeferredResult<ResponseEntity<Map>> updateIntegration(@PathVariable("company") String company,
                                                                 @PathVariable("integrationid") String integrationId,
                                                                 @RequestBody Integration integration) {
        if (StringUtils.isEmpty(integration.getId())) {
            integration = integration.toBuilder()
                    .id(integrationId)
                    .build();
        }
        final Integration integrationToUpdate = integration;
        return SpringUtils.deferResponse(() -> {
            integrationService.update(company, integrationToUpdate);
            tagItemDBService.deleteTagsForItem(company,
                    TagItemMapping.TagItemType.INTEGRATION.toString(), integrationId);
            insertTagItemIds(company, integrationId, integrationToUpdate.getTags());
            return ResponseEntity.accepted().body(Map.of("integration_id", integrationId));
        });
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{integrationid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> deleteIntegration(@PathVariable("company") String company,
                                                                  @PathVariable("integrationid") String integrationId) {
        return SpringUtils.deferResponse(() -> {
            Integration integration = integrationService.get(company, integrationId)
                    .orElseThrow(() -> new NotFoundException("Could not find integration id=" + integrationId + " for company=" + company));

            // if there are other integrations which have their credentials linked this one, remap them
            remapLinkedCredentials(company, integration);

            // clean up secrets
            try {
                integrationSecretsService.delete(company, integrationId);
            } catch (Exception e) {
                log.warn("Failed to delete integration secrets for company={}, integration={}", company, integrationId);
            }

            // remove integration
            Boolean deleted = integrationService.delete(company, integrationId);
            if (BooleanUtils.isTrue(deleted)) {
                tagItemDBService.deleteTagsForItem(company, TagItemMapping.TagItemType.INTEGRATION.toString(), integrationId);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        });
    }

    private void remapLinkedCredentials(String company, Integration integration) throws SQLException, SecretsManagerServiceClientException, NotFoundException, BadRequestException, AtlassianConnectServiceClientException {
        String integrationId = integration.getId();

        // list all integrations that are linked to this one (children)
        List<String> childIntegrationIds = integrationService.stream(company, null, false, null, null, null, null, null, null, integrationId)
                .map(Integration::getId)
                .collect(Collectors.toList());

        if (childIntegrationIds.isEmpty()) {
            return;
        }

        // arbitrarily pick a new parent
        String newParentId = childIntegrationIds.get(0);

        log.info("Found {} linked integrations for company={}, integration={}; remapping to new parent id={}", childIntegrationIds.size(), company, integrationId, newParentId);

        // unlink then copy current credentials to new parent
        integrationService.update(company, Integration.builder()
                .id(newParentId)
                .linkedCredentials(IntegrationService.UNLINK_CREDENTIALS)
                .authentication(integration.getAuthentication())
                .build());
        integrationSecretsService.copy(company, SecretsManagerServiceClient.DEFAULT_CONFIG_ID, integrationId, newParentId);

        // relink all following integrations
        for (int i = 1 ; i < childIntegrationIds.size(); ++i) {
            integrationService.update(company, Integration.builder()
                    .id(childIntegrationIds.get(i))
                    .linkedCredentials(newParentId)
                    .build());
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, Object>>> deleteIntegrations(@PathVariable("company") String company,
                                                                                  @RequestBody List<String> integrationIds) {
        return SpringUtils.deferResponse(() -> {
            integrationIds.forEach(integrationId -> {
                try {
                    integrationSecretsService.delete(company, integrationId);
                } catch (Exception e) {
                    log.warn("Failed to delete integration secrets for company={}, integration={}", company, integrationId);
                }
            });
            int rowsDeleted = integrationService.bulkDelete(company, integrationIds);
            if (rowsDeleted > 0) {
                tagItemDBService.bulkDeleteTagsForItems(company,
                        TagItemMapping.TagItemType.INTEGRATION.toString(), integrationIds);
            }
            return ResponseEntity.ok(Map.of("deleted_rows", rowsDeleted,
                    "success", (org.apache.commons.collections4.CollectionUtils.size(integrationIds) == rowsDeleted)));
        });
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{integrationid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Integration>> getIntegration(@PathVariable("company") String company,
                                                                      @PathVariable("integrationid") String integrationId) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok()
                .body(integrationService.get(company, integrationId)
                        .orElseThrow(() -> new NotFoundException("Could not find integration id=" + integrationId + " for company=" + company))));
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<DbListResponse<Integration>>> listIntegrations(@PathVariable("company") String company,
                                                                                        @RequestBody DefaultListRequest listRequest) {
        return SpringUtils.deferResponse(() -> {
            String name = listRequest.getFilterValueAsMap("partial").map(m -> (String) m.get("name")).orElse(null);
            String application = listRequest.getFilterValue("application", String.class).orElse(null);
            List<String> applications = listRequest.<String>getFilterValueAsList("applications").orElse(null);
            if (CollectionUtils.isEmpty(applications) && !StringUtils.isEmpty(application)) {
                applications = List.of(application);
            }
            Boolean satellite = listRequest.getFilterValue("satellite", Boolean.class).orElse(null);
            List<String> tagIdsString = listRequest.<String>getFilterValueAsList("tag_ids")
                    .orElse(Collections.emptyList());
            List<Integer> tagIds = tagIdsString.stream()
                    .map(i -> NumberUtils.toInt(i, -1))
                    .filter(i -> i > 0)
                    .collect(Collectors.toList());
            List<String> integIdsString = listRequest.<String>getFilterValueAsList("integration_ids")
                    .orElse(Collections.emptyList());
            List<Integer> integIds = integIdsString.stream()
                    .map(i -> NumberUtils.toInt(i, -1))
                    .filter(i -> i > 0)
                    .collect(Collectors.toList());
            String linkedCredentialsIntegrationId = listRequest.getFilterValue("linked_credentials", String.class).orElse(null);
            
            Map<String, Integer> updateRange = listRequest.getFilterValue("updated_at", Map.class).orElse(Map.of());
            Long updatedAtEnd = updateRange.get("$lt") != null ? Long.valueOf(updateRange.get("$lt")) : null;
            Long updatedAtStart = updateRange.get("$gt") != null ? Long.valueOf(updateRange.get("$gt")) : null;

            DbListResponse<Integration> integrations = integrationService.listByFilter(
                    company,
                    name,
                    false,
                    applications,
                    satellite,
                    integIds,
                    tagIds,
                    updatedAtStart,
                    updatedAtEnd,
                    linkedCredentialsIntegrationId,
                    listRequest.getPage(),
                    listRequest.getPageSize());
            return ResponseEntity.ok(integrations);
        });
    }

    @PostMapping(value = "/configs/list", produces = "application/json")
    public DeferredResult<ResponseEntity<DbListResponse<IntegrationConfig>>> listConfigs(@PathVariable("company") String company,
                                                                                         @RequestBody DefaultListRequest listRequest) {
        List<String> integrationIds = listRequest.<String>getFilterValueAsList("integration_ids").orElse(null);
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                integrationService.listConfigs(company, integrationIds, listRequest.getPage(), listRequest.getPageSize())));
    }

    @PostMapping(value = "/{integrationId:[0-9]+}/products/list", produces = "application/json")
    public DeferredResult<ResponseEntity<DbListResponse<ProductIntegMapping>>> listProducts(@PathVariable("company") String company,
                                                                                            @PathVariable("integrationId") String integrationId,
                                                                                            @RequestBody DefaultListRequest listRequest) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                productIntegMappingService.listByFilter(company, null, integrationId, listRequest.getPage(), listRequest.getPageSize())));
    }

    private void insertTagItemIds(final String company, final String integrationId,
                                  final List<String> tags) throws SQLException {
        if (CollectionUtils.isEmpty(tags)) {
            return;
        }
        List<TagItemMapping> tagBPMappings = tags.stream()
                .map(tag -> TagItemMapping.builder()
                        .tagId(tag)
                        .itemId(integrationId)
                        .tagItemType(TagItemMapping.TagItemType.INTEGRATION)
                        .build())
                .collect(Collectors.toList());
        tagItemDBService.batchInsert(company, tagBPMappings);
    }

}