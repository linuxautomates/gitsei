package io.levelops.commons.inventory;

import io.levelops.commons.databases.models.database.mappings.ProductIntegMapping;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.models.DbListResponse;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Builder
public class ProductMappingService {
    private static final int PAGE_SIZE = 100;
    private final InventoryService inventoryService;

    public ProductMappingService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public List<String> getProductIds(String company, String integrationId) {
        return PaginationUtils.stream(0, 1, page -> {
            try {
                DbListResponse<ProductIntegMapping> productMappings = inventoryService.listProducts(company, integrationId, page, PAGE_SIZE);
                if (productMappings == null || productMappings.getRecords() == null) {
                    return Collections.emptyList();
                }
                return productMappings.getRecords().stream()
                        .map(ProductIntegMapping::getProductId)
                        .collect(Collectors.toList());
            } catch (InventoryException e) {
                log.warn("Failed to retrieve product mappings for integration_id={} (page={}, pageSize={})", integrationId, page, PAGE_SIZE, e);
                return Collections.emptyList();
            }
        }).collect(Collectors.toList());
    }
}
