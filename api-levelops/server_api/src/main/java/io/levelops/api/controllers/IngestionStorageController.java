package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.integrations.storage.models.StorageData;
import io.levelops.integrations.storage.models.StorageResult;
import io.levelops.services.GcsStorageService;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.ForbiddenException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping("/v1/ingestion/storage")
@PreAuthorize("hasAuthority('INGESTION')")
@Log4j2
@SuppressWarnings("unused")
public class IngestionStorageController {

    private final boolean validateIntegrationId;
    private final GcsStorageService gcsStorageService;
    private final InventoryService inventoryService;

    @Autowired
    public IngestionStorageController(GcsStorageService gcsStorageService,
                                      InventoryService inventoryService,
                                      @Value("${INGESTION_STORAGE_VALIDATE_INTEGRATION_ID:false}") boolean validateIntegrationId) {
        this.gcsStorageService = gcsStorageService;
        this.inventoryService = inventoryService;
        this.validateIntegrationId = validateIntegrationId;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PostMapping(path = "/push", produces = "application/json")
    public DeferredResult<ResponseEntity<StorageResult>> push(@SessionAttribute("company") String company,
                                                              @RequestBody StorageData storageData) throws BadRequestException, ForbiddenException {
        IntegrationKey integrationKey = storageData.getIntegrationKey();
        BadRequestException.checkNotNull(integrationKey, "integration_key is required");
        ForbiddenException.check(company.equals(integrationKey.getTenantId()), "invalid tenant");
        BadRequestException.check(Strings.isNotBlank(storageData.getJobId()), "invalid job_id");
        BadRequestException.check(Strings.isNotBlank(storageData.getRelativePath()), "invalid relative_path");
        if (validateIntegrationId) {
            try {
                inventoryService.getIntegration(integrationKey);
            } catch (InventoryException e) {
                throw new ForbiddenException("invalid integration id");
            }
        }

        return SpringUtils.deferResponse(() -> ResponseEntity.ok(gcsStorageService.pushOne(storageData)));
    }

}
