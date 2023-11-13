package io.levelops.api.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.model.TenantConfigDTO;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@RestController
@RequestMapping("/v1/configs")
public class ConfigsController {

    private final TenantConfigService configService;

    public ConfigsController(final TenantConfigService configService) {
        this.configService = configService;
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PostMapping(consumes = "application/json", produces = "application/json")
    public DeferredResult<ResponseEntity<Boolean>> update(
            @SessionAttribute("company") String company,
            @RequestBody TenantConfigDTO config) {
        return SpringUtils.deferResponse(() -> {
            TenantConfig old = configService.listByFilter(
                    company, config.getName(), 0, 1).getRecords()
                    .stream().findFirst().orElse(null);
            var newConfig = TenantConfig.builder().name(config.getName()).value(config.getValue() instanceof String ? config.getValue().toString() : DefaultObjectMapper.get().writeValueAsString(config.getValue())).build();
            if (old == null) {
                return ResponseEntity.ok(configService.insert(company, newConfig) != null);
            }
            if (StringUtils.equals(old.getValue(), newConfig.getValue())){
                return ResponseEntity.ok(Boolean.TRUE);
            }
            return ResponseEntity.ok(configService.update(company, newConfig.toBuilder().id(old.getId()).build()));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_INSIGHTS,permission = Permission.INSIGHTS_VIEW)
    @PostMapping(value = "/list", consumes = "application/json", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<TenantConfigDTO>>> list(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest listRequest) {
        return SpringUtils.deferResponse(() -> {
            var results = configService.listByFilter(company,listRequest.getFilterValue("name", String.class).orElse(null),0,100);
            return ResponseEntity.ok(PaginatedResponse.of(0, 100,results.getTotalCount(), results.getRecords().stream()
                .map(config -> {
                    if( config.getValue().startsWith("{") ){
                        try {
                            return TenantConfigDTO.builder()
                                .name(config.getName())
                                .id(config.getId())
                                .value(DefaultObjectMapper.get().readValue(config.getValue(), Map.class))
                                .build();
                        } catch (JsonProcessingException e) {
                            log.error("[{}] unable to convert potential json string value into a map: {}", company, config, e);
                            return TenantConfigDTO.builder()
                                .name(config.getName())
                                .id(config.getId())
                                .value(config.getValue())
                                .build();
                        }
                    }
                    return TenantConfigDTO.builder()
                        .name(config.getName())
                        .id(config.getId())
                        .value(config.getValue())
                        .build();
                })
                .collect(Collectors.toList())));
        });
    }

}