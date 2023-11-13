package io.levelops.internal_api.controllers;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequestMapping("/internal/v1/tenants/{company}/products")
public class ProductsController {
    private static final String ACTIVITY_LOG_TEXT = "%s Products item: %s.";
    private static final Pattern FOREIGN_KEY_VIOLATION = Pattern.compile("^.*violates foreign key constraint.*on table \\\"(?<tblname>.*)\\\".*$", Pattern.MULTILINE);
    private static final Pattern PRODUCT_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-_][a-zA-Z0-9\\-_\\s]*[a-zA-Z0-9\\-_]$");
    private static final Pattern PRODUCT_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-_]*$");
    private static final Pattern DUPLICATE_KEY_ERROR = Pattern.compile("Duplicate key exists");

    private final ProductService productService;
    private final OrgUnitCategoryDatabaseService categoryService;

    @Autowired
    public ProductsController(final ProductService productService, final OrgUnitCategoryDatabaseService categoryService){
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{productid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Product>> productDetails(@PathVariable("productid") String productId,
                                                                  @PathVariable("company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(productService.get(company, productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product with id " + productId + " not found."))));
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createProduct(@RequestBody Product product,
                                                                             @SessionAttribute(name = "session_user") String sessionUser,
                                                                             @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            try {
                validateInput(product, true);
            } catch (Exception e) {
                log.error(e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            try {
                String productId = productService.insert(company, product);
                // activityLogService.insert(company, ActivityLog.builder()
                //         .targetItem(productId)
                //         .email(sessionUser)
                //         .targetItemType(ActivityLog.TargetItemType.PRODUCT)
                //         .body(String.format(ACTIVITY_LOG_TEXT, "Created", productId))
                //         .details(Collections.singletonMap("item", product))
                //         .action(ActivityLog.Action.CREATED)
                //         .build());
                // create default categories
                var teamsCategory = OrgUnitCategory.builder()
                        .name("Teams")
                        .description("Teams")
                        .enabled(true)
                        .workspaceId(Integer.parseInt(productId))
                        .rootOuName("All Teams")
                        .createdAt(Instant.now())
                        .build();
                var sprintsCategory = OrgUnitCategory.builder()
                        .name("Sprints")
                        .description("Sprints")
                        .enabled(true)
                        .workspaceId(Integer.parseInt(productId))
                        .rootOuName("All Sprints")
                        .createdAt(Instant.now().plusSeconds(10L))
                        .build();
                var projectsCategory = OrgUnitCategory.builder()
                        .name("Projects")
                        .description("Projects")
                        .enabled(true)
                        .workspaceId(Integer.parseInt(productId))
                        .createdAt(Instant.now().plusSeconds(30L))
                        .rootOuName("All Projects")
                        .build();
                categoryService.insert(company, teamsCategory);
                categoryService.insert(company, projectsCategory);
                categoryService.insert(company, sprintsCategory);
                return ResponseEntity.ok(Map.of("id", productId));
            } catch (Exception e) {
                String msg = e.getMessage();
                Matcher matcher = DUPLICATE_KEY_ERROR.matcher(msg);
                if (matcher.find()) {
                    log.error(e.getMessage());
                    return ResponseEntity.status(HttpStatus.CONFLICT).build();
                }
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        });
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{productid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> productUpdate(@RequestBody Product product,
                                                              @SessionAttribute(name = "session_user") String sessionUser,
                                                              @PathVariable("productid") String productId,
                                                              @SessionAttribute(name = "company") String company) {
        if (StringUtils.isEmpty(product.getId())) {
            product = Product.builder()
                    .id(productId)
                    .name(product.getName())
                    .description(product.getDescription())
                    .ownerId(product.getOwnerId())
                    .integrationIds(product.getIntegrationIds())
                    .build();
        }
        final Product finalProduct = product;
        return SpringUtils.deferResponse(() -> {
            validateInput(finalProduct, false);
            var productExist = productService.get(company, productId).get();
            Set<Integer> productList=productExist.getIntegrationIds();
            productList.removeAll(finalProduct.getIntegrationIds());
            productService.update(company, finalProduct);
            if(!productList.isEmpty())
                productService.deleteOuIntegration(company,Integer.parseInt(finalProduct.getId()),productList.stream().collect(Collectors.toList()));
            // activityLogService.insert(company, ActivityLog.builder()
            //         .targetItem(productId)
            //         .email(sessionUser)
            //         .targetItemType(ActivityLog.TargetItemType.PRODUCT)
            //         .body(String.format(ACTIVITY_LOG_TEXT, "Edited", productId))
            //         .details(Collections.singletonMap("item", finalProduct))
            //         .action(ActivityLog.Action.EDITED)
            //         .build());
            // check for OUs with matching configurations for the integrations no longer in the mix
            var filters = QueryFilter.builder()
                .strictMatch("ou_category_id", Set.of())
                .build();
            // ouService.filter(company, filters, 0, 50);
            return ResponseEntity.ok().build();
        });
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Product>>> productsList(@SessionAttribute(name = "company") String company,
                                                                                   @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            String partialName = null;
            Set<Integer> productIds = null;
            Boolean bootstrapped = null;
            Boolean immutable = null;
            Boolean demo = null;
            if (filter.getFilter() != null && filter.getFilter()
                    .getOrDefault("partial", Collections.emptyMap()) != null) {
                // this partial name is looked up in the middle: %name%
                // TODO: we are supporting {"partial":{"name":{"starts":"value"}}} so we can look it up like 'value%' which is faster and more common
                // so, we need to support both.. string value and map
                var a = ((Map<String, Object>) filter.getFilter()
                .getOrDefault("partial", Collections.emptyMap()))
                .getOrDefault("name", null);
                if (a != null && a instanceof String){
                    partialName = (String) a;
                }
                else if (a != null && a instanceof Map){
                    var b = (Map<String, String>) a;
                    partialName=b.get("starts");
                }
                productIds = ((List<String>) filter.getFilter()
                            .getOrDefault("product_ids", Collections.emptyList()))
                            .stream()
                            .map(i -> Integer.parseInt(i))
                            .collect(Collectors.toSet());
                bootstrapped = filter.getFilterValue("bootstrapped", Boolean.class).orElse(null);
                immutable = filter.getFilterValue("immutable", Boolean.class).orElse(null);
                demo=filter.getFilterValue("demo", Boolean.class).orElse(null);
            }

            Map<String, Integer> updateRange = filter.getFilterValue("updated_at", Map.class).orElse(Map.of());
            Long updatedAtEnd = updateRange.get("$lt") != null ? Long.valueOf(updateRange.get("$lt")) : null;
            Long updatedAtStart = updateRange.get("$gt") != null ? Long.valueOf(updateRange.get("$gt")) : null;

            Set<Integer> integrationIds = filter.<Integer>getFilterValueAsSet("integration_id").orElse(Set.of());
            Set<String> integrationType = filter.<String>getFilterValueAsSet("integration_type").orElse(Set.of());
            Set<String> category = filter.<String>getFilterValueAsSet("category").orElse(Set.of());
            Set<String> key = filter.<String>getFilterValueAsSet("key").orElse(Set.of());
            Set<String> ownerId = filter.<String>getFilterValueAsSet("owner_id").orElse(Set.of());
            Set<String> orgIdentifier = filter.<String>getFilterValueAsSet("orgIdentifier").orElse(Set.of());
            DbListResponse<Product> products = productService.listByFilter(
                company,
                partialName,
                productIds,
                integrationIds,
                integrationType,
                category,
                key,
                orgIdentifier,
                ownerId,
                bootstrapped, immutable, updatedAtStart, updatedAtEnd, null, demo, filter.getPage(), filter.getPageSize());
            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), products));
        });
    }

    protected void validateInput(final Product product, final boolean insert) {
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace object cannot be null.");
        }
        if (StringUtils.isBlank(product.getName())) {
            if (insert) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace name cannot be null or empty or blank.");
            }
        } else {
            Matcher matcher = PRODUCT_NAME_PATTERN.matcher(product.getName().trim());
            if (!matcher.matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace name should contain only \'a-z A-Z 0-9 -_\' or space. Input: " + product.getName());
            }
        }
        if (StringUtils.isBlank(product.getKey())) {
            if (insert) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace key cannot be null or empty or blank.");
            }
        } else {
            Matcher matcher = PRODUCT_KEY_PATTERN.matcher(product.getKey().trim());
            if (!matcher.matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace key should contain only \'a-z A-Z 0-9 -_\'. Input: " + product.getKey());
            }
        }
        if (StringUtils.isBlank(product.getOwnerId())) {
            if (insert) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workspace owner cannot be null or empty or blank.");
            }
        }
        // if no integration is added, we'll let it pass as there will always be a default propelo/dummy integration
        // if(insert && CollectionUtils.isEmpty(product.getIntegrationIds())) {
        //     throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one integration is needed for a Workspace.");
        // }
        return;
    }
}
