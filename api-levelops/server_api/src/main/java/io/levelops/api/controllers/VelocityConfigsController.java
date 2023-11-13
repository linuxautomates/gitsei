package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.services.velocity.OrgProfileDatabaseService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.services.velocity_productivity.services.VelocityConfigsService;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
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

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/velocity_configs")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@Log4j2
public class VelocityConfigsController {
    private VelocityConfigsService velocityConfigsService;
    private OrgProfileDatabaseService orgProfileDatabaseService;

    @Autowired
    public VelocityConfigsController(VelocityConfigsService velocityConfigsService, OrgProfileDatabaseService orgProfileDatabaseService) {
        this.velocityConfigsService = velocityConfigsService;
        this.orgProfileDatabaseService = orgProfileDatabaseService;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> create(@RequestBody VelocityConfigDTO config,
                                                                          @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(Map.of("id", velocityConfigsService.create(company, config))));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @RequestMapping(method = RequestMethod.DELETE, value = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> delete(@PathVariable("id") String configId,
                                                           @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            velocityConfigsService.delete(company, configId);
            return ResponseEntity.ok().build();
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @RequestMapping(method = RequestMethod.PUT, value = "/{configid}", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> update(@RequestBody VelocityConfigDTO config,
                                                                          @PathVariable("configid") String configId,
                                                                          @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            final VelocityConfigDTO sanitizedConfig = (config.getId() != null) ? config : config.toBuilder().id(UUID.fromString(configId)).build();
            if(!configId.equals(sanitizedConfig.getId().toString())){
                throw new BadRequestException("For customer " + company + " config id " + sanitizedConfig.getId().toString() + " failed to update config!");
            }
            return ResponseEntity.ok(Map.of("id", velocityConfigsService.update(company, sanitizedConfig)));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @RequestMapping(method = RequestMethod.PATCH, value = "/{configid}/set-default", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> setDefault(@PathVariable("configid") String configid,
                                                       @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            velocityConfigsService.setDefault(company, configid);
            return ResponseEntity.ok().build();
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.PATCH, value = "/{configid}/set-ou-mappings", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> updateOUMappings(@RequestBody VelocityConfigDTO config,
                                                                 @PathVariable("configid") String configid,
                                                           @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            orgProfileDatabaseService.updateProfileOUMappings(company,UUID.fromString(configid), config.getAssociatedOURefIds());
            return ResponseEntity.ok().build();
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, value = "/{configid}", produces = "application/json")
    public DeferredResult<ResponseEntity<VelocityConfigDTO>> getRule(@PathVariable("configid") String configid,
                                                                  @SessionAttribute(name = "company") String company) {

        return SpringUtils.deferResponse(() -> {
            VelocityConfigDTO rule = velocityConfigsService.get(company, configid)
                    .orElseThrow(() -> new NotFoundException("Could not find Velocity Config with id=" + configid));
            return ResponseEntity.ok(rule);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_INSIGHTS,permission = Permission.INSIGHTS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, value = "/ou/{ou_ref_id}", produces = "application/json")
    public DeferredResult<ResponseEntity<VelocityConfigDTO>> getByOuRefId(@PathVariable("ou_ref_id") int ouRefId,
                                                                     @SessionAttribute(name = "company") String company) {

        return SpringUtils.deferResponse(() -> {
            VelocityConfigDTO velocityConfigDTO = velocityConfigsService.getByOuRefId(company, ouRefId)
                    .orElseThrow(() -> new NotFoundException("Could not find Velocity Config with ou_ref_id=" + ouRefId));
            return ResponseEntity.ok(velocityConfigDTO);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<VelocityConfigDTO>>> rulesList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            PaginatedResponse<VelocityConfigDTO> internalApiResponse = PaginatedResponse.of(filter.getPage(), filter.getPageSize(), velocityConfigsService.listByFilter(company, filter));
            log.info("internalApiResponse = {}", internalApiResponse);
            return ResponseEntity.ok(internalApiResponse);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, value = "/base-config-template", produces = "application/json")
    public DeferredResult<ResponseEntity<VelocityConfigDTO>> getRule(@SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            VelocityConfigDTO rule = velocityConfigsService.getBaseConfigTemplate();
            return ResponseEntity.ok(rule);
        });
    }
}
