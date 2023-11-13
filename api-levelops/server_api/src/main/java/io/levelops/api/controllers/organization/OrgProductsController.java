package io.levelops.api.controllers.organization;

import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.organization.DBOrgProduct;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.request.async.DeferredResult;

import java.net.URI;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/org_products")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
public class OrgProductsController {

    private final ProductsDatabaseService productsService;
    private final IntegrationService integrationService;

    @Autowired
    public OrgProductsController(final ProductsDatabaseService productsService, final IntegrationService integrationService){
        this.productsService = productsService;
        this.integrationService = integrationService;
    }

    @PostMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<String>> createProduct(@SessionAttribute(name = "company") String company, @RequestBody OrgProductsDTO request){
        return SpringUtils.deferResponse(() -> {
            var item = DBOrgProduct.builder()
                .name(request.getName())
                .description(request.getDescription())
                .integrations(request.getIntegrations().entrySet().stream()
                    .flatMap(entry -> entry.getValue().stream())
                    .map(integ -> DBOrgProduct.Integ.builder()
                        .integrationId(integ.getId())
                        .filters(integ.getFilters())
                        .build())
                    .collect(Collectors.toSet()))
                .build();
            var id = productsService.insert(company, item);
            return ResponseEntity.created(URI.create("/v1/org_products/" + id)).body(id);
        });
    }

    @GetMapping(path="/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<OrgProductsDTO>> getProduct(@SessionAttribute(name = "company") String company, @PathVariable("id") UUID id){
        return SpringUtils.deferResponse(() -> {
            var dbProduct = productsService.get(company, id);
            if (dbProduct.isEmpty()){
                throw new HttpServerErrorException(HttpStatus.NOT_FOUND, "unable to find product with id: " + id);
            }
            return ResponseEntity.ok(dbToApi(company, dbProduct.get()));
        });
    }

    @PostMapping(path="/list", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<PaginatedResponse<OrgProductsDTO>>> listProducts(
        @SessionAttribute(name = "company") String company, 
        @RequestBody DefaultListRequest request){
        return SpringUtils.deferResponse(() -> {
            var results = productsService.filter(company, QueryFilter.fromRequestFilters(request.getFilter()), request.getPage(), request.getPageSize());
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(), request.getPageSize(), results.getTotalCount(), results.getRecords().stream().map(item -> dbToApi(company, item)).collect(Collectors.toList())));
        });
    }

    private OrgProductsDTO dbToApi(final String company, final DBOrgProduct dbProduct){
        Map<String, Set<OrgProductsDTO.Integ>> integs = new HashMap<>();
        dbProduct.getIntegrations().stream().forEach(integ -> {
            var integrationOptional = integrationService.get(company, integ.getIntegrationId()+"");
            if(integrationOptional.isEmpty()){
                return;
            }
            var integration = integrationOptional.get();
            integs.compute(integration.getApplication().toLowerCase(), (String key, Set<OrgProductsDTO.Integ> map) -> {
                if(map == null){
                    map = new HashSet<>();
                }
                map.add(OrgProductsDTO.Integ.builder().id(integ.getIntegrationId()).name(integration.getName()).filters(integ.getFilters()).build());
                return map;
            });
        });

        return OrgProductsDTO.builder()
            .name(dbProduct.getName())
            .id(dbProduct.getId())
            .description(dbProduct.getDescription())
            .integrations(integs)
            .build();
    }

    @DeleteMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<String>> deleteTeams(@SessionAttribute("company") String company, @RequestBody() Set<UUID> teamIds){
            return SpringUtils.deferResponse(() -> {
                try {
                    productsService.delete(company, teamIds);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return ResponseEntity.ok("OK");
            });
    }
}
