package io.levelops.commons.databases.services.parsers;

import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.organization.DBOrgProduct;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.models.DbListResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class PagerDutyFilterParserCommons {

    public static final Set<String> PAGERDUTY_APPLICATION_TYPES = Set.of("pagerduty");

    private final ProductsDatabaseService productsDatabaseService;

    @Autowired
    public PagerDutyFilterParserCommons(ProductsDatabaseService productsDatabaseService) {
        this.productsDatabaseService = productsDatabaseService;
    }

    public Map<Integer, Map<String, Object>> getProductFilters(String company, Set<UUID> productIds) throws SQLException {
        DbListResponse<DBOrgProduct> productDbListResponse = productsDatabaseService.filter(company,
                QueryFilter.builder()
                        .strictMatch("product_id", productIds)
                        .strictMatch("integration_type", PAGERDUTY_APPLICATION_TYPES)
                        .build(), 0, 10);
        return productDbListResponse.getRecords()
                .stream()
                .flatMap(dbOrgProduct -> dbOrgProduct.getIntegrations().stream())
                .filter(orgProduct -> Objects.nonNull(orgProduct.getFilters()))
                .collect(Collectors.toMap(
                        DBOrgProduct.Integ::getIntegrationId,
                        DBOrgProduct.Integ::getFilters,
                        (existingInteg, newInteg) -> existingInteg
                ));
    }
}
