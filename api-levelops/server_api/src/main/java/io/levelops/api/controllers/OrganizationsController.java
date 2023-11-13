package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.Organization;
import io.levelops.commons.databases.services.OrganizationService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.exceptions.NotFoundException;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/v1/organizations")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@Log4j2
@SuppressWarnings("unused")
public class OrganizationsController {
    private ObjectMapper objectMapper;
    private OrganizationService organizationService;

    @Autowired
    public OrganizationsController(ObjectMapper objectMapper, OrganizationService organizationService) {
        this.objectMapper = objectMapper;
        this.organizationService = organizationService;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> createOrganization(@RequestBody Organization organization,
                                                                                  @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            if (StringUtils.isEmpty(organization.getName())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing name.");
            }
            return ResponseEntity.ok(Map.of("id", organizationService.insert(company, organization)));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @RequestMapping(method = RequestMethod.DELETE, value = "/{organizationid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> orgDelete(@PathVariable("organizationid") String organizationId,
                                                          @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            organizationService.delete(company, organizationId);
            return ResponseEntity.ok().build();
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @RequestMapping(method = RequestMethod.PUT, value = "/{organizationid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> orgUpdate(@RequestBody Organization organization,
                                                          @PathVariable("organizationid") String organizationId,
                                                          @SessionAttribute(name = "company") String company) {
        if (StringUtils.isEmpty(organization.getId())) {
            organization = Organization.builder().name(organization.getName()).id(organizationId).build();
        }
        final Organization orgToUpdate = organization;
        return SpringUtils.deferResponse(() -> {
            if (StringUtils.isEmpty(orgToUpdate.getName())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing name.");
            }
            organizationService.update(company, orgToUpdate);
            return ResponseEntity.ok().build();
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.GET, value = "/{organizationid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<Organization>> orgDetails(@PathVariable("organizationid") String organizationId,
                                                                   @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(organizationService.get(company, organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"))));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Organization>>> organizationsList(@SessionAttribute(name = "company") String company,
                                                                                             @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            DbListResponse<Organization> organizations =
                    organizationService.listByFilter(company,
                            filter.getFilterValue("partial", Map.class)
                                    .map(m -> (String) m.get("name"))
                                    .orElse(null),
                            filter.getPage(), filter.getPageSize());
            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), organizations));
        });
    }


}
