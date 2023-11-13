package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.Plugin;
import io.levelops.commons.databases.models.database.Tag;
import io.levelops.commons.databases.services.PluginDatabaseService;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.plugins.clients.PluginsClient;
import io.levelops.plugins.models.PluginTrigger;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@Log4j2
@RequestMapping("/v1/plugins")
@SuppressWarnings("unused")
public class PluginsController {

    private Storage storage;
    private String pluginsBucket;
    private ObjectMapper objectMapper;
    private PluginDatabaseService pluginService;
    private final PluginsClient pluginsClient;
    private final TagsService tagsService;

    @Autowired
    public PluginsController(@Value("${PLUGINS_BUCKET_NAME:levelops-plugins}") final String bucketName,
                             ObjectMapper objectMapper,
                             PluginDatabaseService pluginService,
                             PluginsClient pluginsClient,
                             Storage storage,
                             TagsService tagsService) {
        this.storage = storage;
        this.pluginsBucket = bucketName;
        this.objectMapper = objectMapper;
        this.pluginService = pluginService;
        this.pluginsClient = pluginsClient;
        this.tagsService = tagsService;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @GetMapping(path = "/{pluginid}/download", produces = "application/json")
    public DeferredResult<ResponseEntity<ByteArrayResource>> pluginDownload(
            @SessionAttribute("company") String company,
            @PathVariable("pluginid") UUID pluginId) {
        return SpringUtils.deferResponse(() -> {
            Plugin plugin = pluginService.get(company, pluginId.toString()).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Plugin with id " + pluginId + " not found."));
            if (Boolean.TRUE.equals(plugin.getCustom())) {
                // we only want non-custom plugins
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Plugin with id " + pluginId + " does not support download.");
            }
            Blob blob = null;
            if (plugin.getGcsPath() != null) {
                blob = storage.get(pluginsBucket, plugin.getGcsPath());
            }
            if (blob == null || !blob.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Plugin blob with id " + pluginId + " not found.");
            }
            ByteArrayResource resource = new ByteArrayResource(blob.getContent());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @GetMapping(path = "/{pluginid}", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> pluginDetails(
            @SessionAttribute("company") String company,
            @PathVariable("pluginid") UUID pluginId) {
        return SpringUtils.deferResponse(() -> {
            Plugin plugin = pluginService.get(company, pluginId.toString()).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Plugin with id " + pluginId + " not found."));
            return ResponseEntity.ok(
                    objectMapper.writeValueAsString(plugin));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PostMapping(path = "/{pluginid}/trigger", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> pluginTrigger(
            @SessionAttribute("company") String company,
            @PathVariable("pluginid") UUID pluginId,
            @RequestBody PluginTrigger trigger) {
        return SpringUtils.deferResponse(() -> {
            Plugin plugin = pluginService.get(company, pluginId.toString()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Plugin with id " + pluginId + " not found."));
            PluginTrigger fullPluginTrigger = trigger.toBuilder()
                .company(company)
                .pluginId(pluginId)
                .pluginType(plugin.getTool())
                .tagIds(CollectionUtils.isEmpty(trigger.getTagIds())
                    ? Collections.emptyList()
                    : trigger.getTagIds().stream()
                        .map(tagId -> tagsService.get(company, tagId).orElse(Tag.builder().name("").build()).getName())
                        .filter(Strings::isNotBlank)
                        .collect(Collectors.toList()))
                .build();
            String triggerId = pluginsClient.triggerPlugin(fullPluginTrigger);
            return ResponseEntity.accepted().body(
                    objectMapper.writeValueAsString(Map.of("trigger_id", triggerId))
                    );
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    @PostMapping(path = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> pluginsList(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            String partialName = filter.getFilterValue("partial", Map.class)
                    .map(m -> (String) m.get("name"))
                    .orElse(null);
            Boolean custom = filter.getFilterValue("custom", Boolean.class)
                    .orElse(null);
            DbListResponse<Plugin> plugins = pluginService.listByFilter(
                    company, partialName, custom, filter.getPage(), filter.getPageSize());
            return ResponseEntity.ok(
                    objectMapper.writeValueAsString(
                            PaginatedResponse.of(
                                    filter.getPage(),
                                    filter.getPageSize(),
                                    plugins)));
        });
    }
}
