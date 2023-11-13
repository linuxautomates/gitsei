package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.dashboard.CloneDashboardRequest;
import io.levelops.commons.databases.models.database.dashboard.CloneDashboardResponse;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.databases.models.database.mappings.ProductIntegMapping;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
public class CannedDashboardService {

    private final DashboardWidgetService dashboardWidgetService;
    private final ProductService prodService;
    private final IntegrationService integrationService;
    private final ProductIntegMappingService productIntegMappingService;
    private final UserService userService;
    private static final Random random = new Random();

    @Autowired
    public CannedDashboardService(DashboardWidgetService dashService,
                                  ProductService prodService,
                                  IntegrationService integrationService,
                                  ProductIntegMappingService productIntegMappingService,
                                  UserService userService) {
        this.dashboardWidgetService = dashService;
        this.prodService = prodService;
        this.integrationService = integrationService;
        this.productIntegMappingService = productIntegMappingService;
        this.userService = userService;
    }

    public CloneDashboardResponse cloneDashboard(CloneDashboardRequest cloneDashboardRequest) throws SQLException, BadRequestException {
        Optional<Dashboard> optDashboard = getSourceDashboard(cloneDashboardRequest);
        if (optDashboard.isPresent()) {
            Dashboard dashboard = sanitizeAndReConfigureDashboard(optDashboard.get(), cloneDashboardRequest);
            log.debug("source dashboard = {}", dashboard);
            String dashboardId = dashboardWidgetService.insert(cloneDashboardRequest.getDestinationTenant(), dashboard);
            return CloneDashboardResponse.builder()
                    .dashboardId(dashboardId)
                    .dashboardName(dashboard.getName())
                    .destinationTenant(cloneDashboardRequest.getDestinationTenant())
                    .build();
        } else {
            throw new BadRequestException("Dashboard not found " + cloneDashboardRequest.getDashboardId());
        }
    }

    private Optional<Dashboard> getSourceDashboard(CloneDashboardRequest cloneDashboardRequest) throws SQLException {
        Optional<Dashboard> optDashboard = Optional.empty();
        if (StringUtils.isNotEmpty(cloneDashboardRequest.getDashboardName())) {
            DbListResponse<Dashboard> dashboardDbListResponse = dashboardWidgetService.listByFilters(cloneDashboardRequest.getSourceTenant(),
                    null, null, cloneDashboardRequest.getDashboardName(), null, null, 0, 1, null);
            if (dashboardDbListResponse.getCount() > 0)
                optDashboard = dashboardWidgetService.get(
                        cloneDashboardRequest.getSourceTenant(), dashboardDbListResponse.getRecords().get(0).getId());
        } else {
            optDashboard = dashboardWidgetService.get(
                    cloneDashboardRequest.getSourceTenant(), cloneDashboardRequest.getDashboardId());
        }
        return optDashboard;
    }

    private Dashboard sanitizeAndReConfigureDashboard(Dashboard dashboard,
                                                      CloneDashboardRequest request) throws SQLException, BadRequestException {
        Dashboard.DashboardBuilder builder = dashboard.toBuilder();
        builder.name(generateName(dashboard.getName(), request));
        if (request.getProductId() != null && request.getIntegrationIds() != null) {
            Optional<Product> product = prodService.get(request.getDestinationTenant(), request.getProductId());
            if (product.isPresent()) {
                final List<Integer> integIds = request.getIntegrationIds().stream().map(Integer::parseInt).collect(Collectors.toList());
                final DbListResponse<Integration> integsList = integrationService.listByFilter(request.getDestinationTenant(),
                        null, null, null, integIds, null, 0, 1000);
                List<String> validIntegs = integsList.getRecords().stream().map(Integration::getId).collect(Collectors.toList());
                if (validIntegs.isEmpty()) {
                    throw new BadRequestException("Integration ids " + request.getIntegrationIds() + " not found for product id " + request.getProductId());
                }
                builder.query(Map.of("product_id", request.getProductId(), "integration_ids", validIntegs));
            } else {
                throw new BadRequestException("Product " + request.getProductId() + " does not exist");
            }
            builder.ownerId(getOwnerId(request));
        } else {
            List<Product> products = prodService.getSystemImmutableProducts(request.getDestinationTenant());
            if (products.isEmpty()) {
                throw new BadRequestException("default product not found for " + request.getDestinationTenant());
            }
            Product defaultProduct = products.get(0);
            DbListResponse<ProductIntegMapping> productIntegMappingDbListResponse = productIntegMappingService.listByFilter(
                    request.getDestinationTenant(), defaultProduct.getId(), null, 0, 10000);
            Set<String> integs = productIntegMappingDbListResponse.getRecords()
                    .stream()
                    .map(ProductIntegMapping::getIntegrationId)
                    .collect(Collectors.toSet());
            builder.query(Map.of("product_id", defaultProduct.getId(), "integration_ids", integs));
            if (request.getOwnerId() != null)
                builder.ownerId(getOwnerId(request));
            else
                builder.ownerId(defaultProduct.getOwnerId());
        }
        List<Widget> widgets = sanitizeAndReConfigureWidgets(dashboard);
        log.debug("source widgets = {}", widgets);
        builder.widgets(widgets);
        log.debug(" dashboard = {}", builder);
        return builder.build();
    }

    private List<Widget> sanitizeAndReConfigureWidgets(Dashboard dashboard) {
        List<Widget> widgets = dashboard.getWidgets()
                .stream()
                .map(widget -> {
                    Map queryMap = DefaultObjectMapper.get().convertValue(widget.getQuery(), Map.class);
                    queryMap.remove("custom_fields");
                    return widget.toBuilder().id(null).query(queryMap).build();
                }).collect(Collectors.toList());
        return widgets;
    }

    private String generateName(String name, CloneDashboardRequest cloneDashboardRequest) throws SQLException {
        if (StringUtils.isNotEmpty(cloneDashboardRequest.getDashboardName()))
            name = cloneDashboardRequest.getDashboardName();
        DbListResponse<Dashboard> dashboardDbListResponse = dashboardWidgetService.listByFilters(
                cloneDashboardRequest.getDestinationTenant(), null, null, name, null, null, 0, 1, null);
        if (dashboardDbListResponse.getCount() > 0) {
            name = name + "-" + random.nextInt(1000);
            return name;
        }
        return name;
    }

    private String getOwnerId(CloneDashboardRequest request) throws BadRequestException, SQLException {
        if (request.getOwnerId() != null) {
            Optional<User> userDetails = userService.get(request.getDestinationTenant(), request.getOwnerId());
            if (userDetails.isPresent())
                return request.getOwnerId();
            else
                throw new BadRequestException("owner id not found in " + request.getDestinationTenant());
        } else {
            Optional<Product> product = prodService.get(request.getDestinationTenant(), request.getProductId());
            if (product.isEmpty())
                throw new BadRequestException("Owner id for this " + request.getProductId() + " not found");
            return product.get().getOwnerId();
        }
    }
}
