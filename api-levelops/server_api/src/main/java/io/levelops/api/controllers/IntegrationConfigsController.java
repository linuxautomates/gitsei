package io.levelops.api.controllers;

import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.IntegrationConfig.Metadata;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ListUtils;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@RequestMapping("/v1/integration_configs")
@SuppressWarnings("unused")
public class IntegrationConfigsController {
    private static final String CUSTOM_FIELDS_CONFIG = "agg_custom_fields";
    private final IntegrationService integrationService;

    @Autowired
    public IntegrationConfigsController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<String>> configsInsert(
            @SessionAttribute(name = "company") String company,
            @RequestBody IntegrationConfig requestConfig) {
        return SpringUtils.deferResponse(() -> {
            IntegrationConfig config = requestConfig;

            //validate config entry
            Validate.notNull(config.getConfig(), "config cannot be null.");
            Validate.notBlank(config.getIntegrationId(), "integration_id cannot be null or empty.");
            Set<Map.Entry<String, List<IntegrationConfig.ConfigEntry>>> entries = config.getConfig().entrySet();
            for (Map.Entry<String, List<IntegrationConfig.ConfigEntry>> entry : entries) {
                for (IntegrationConfig.ConfigEntry configEntry : entry.getValue()) {
                    if (StringUtils.isNotEmpty(configEntry.getDelimiter())) {
                        try {
                            Pattern.compile(configEntry.getDelimiter());
                        } catch (PatternSyntaxException exception) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Invalid delimiter provided: " + configEntry.getDelimiter());
                        }
                    }
                }
            }
            IntegrationConfig existingConfig = integrationService.listConfigs(company, List.of(config.getIntegrationId()), 0, 1).getRecords().stream().findFirst().orElse(null);
            if (existingConfig != null && MapUtils.isNotEmpty(existingConfig.getConfig()) && existingConfig.getConfig().containsKey("due_date_field")) {
                //SEI-2023 : If "due_date_field" exists in config, preserve it as FE doesn't have support yet
                config.getConfig().put("due_date_field", existingConfig.getConfig().get("due_date_field"));
            }
            // SEI-2359 config updated at:
            long now = Instant.now().getEpochSecond();
            config = updateConfigUpdatedAt(now, existingConfig, config);

            integrationService.insertConfig(company, config);
            return ResponseEntity.ok("{}");
        });
    }

    private IntegrationConfig updateConfigUpdatedAt(long now, IntegrationConfig existingConfig, IntegrationConfig config) {
        Long configUpdatedAt = getNewConfigUpdatedAt(now, existingConfig, config);

        // making sure other potential fields don't get overwritten
        Metadata.MetadataBuilder metadataBuilder = config.getMetadata() != null
                ? config.getMetadata().toBuilder()
                : Metadata.builder();
        Metadata updatedMetadata = metadataBuilder
                .configUpdatedAt(configUpdatedAt)
                .build();
        return config.toBuilder()
                .metadata(updatedMetadata)
                .build();
    }

    //making public for testing
    public Long getNewConfigUpdatedAt(long now, IntegrationConfig existingConfig, IntegrationConfig config) {
        // if the version was missing, set it
        if (existingConfig == null || existingConfig.getMetadata() == null || existingConfig.getMetadata().getConfigUpdatedAt() == null) {
            return now;
        }
        // if the config has not changed, keep the old version
        boolean configHasNotChanged = config.isConfigEqualTo(existingConfig);
        if (configHasNotChanged) {
            return existingConfig.getMetadata().getConfigUpdatedAt();
        }
        // otherwise bump it to now
        return now;
    }

    @Nonnull
    private static List<IntegrationConfig.ConfigEntry> getConfigEntries(@Nonnull IntegrationConfig config, @Nonnull String key) {
        return ListUtils.emptyIfNull(MapUtils.emptyIfNull(config.getConfig()).get(key));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @PreAuthorize("hasAnyAuthority('ADMIN','PUBLIC_DASHBOARD','SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<IntegrationConfig>>> configsList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            List<String> integrationIds = (List) filter.getFilter().get("integration_ids");
            DbListResponse<IntegrationConfig> fields = integrationService.listConfigs(
                    company, integrationIds, filter.getPage(), filter.getPageSize());
            return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), fields));
        });
    }
}
