package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.PluginResultDTO;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.ListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.models.JsonDiff;
import io.levelops.plugins.clients.PluginResultsClient;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@Log4j2
@SuppressWarnings("unused")
@RequestMapping("/v1/plugins/results")
public class PluginResultsController {

    private final PluginResultsClient pluginResultsClient;

    @Autowired
    public PluginResultsController(PluginResultsClient pluginResultsClient) {
        this.pluginResultsClient = pluginResultsClient;
    }

    private static PluginResultDTO addEpoch(PluginResultDTO pluginResultDTO) {
        return pluginResultDTO == null ? null : pluginResultDTO.toBuilder()
                .createdAtEpoch(DateUtils.toEpochSecond(pluginResultDTO.getCreatedAt()))
                .build();
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PostMapping(value = "/labels/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<String>>> listLabelKeys(@SessionAttribute("company") String company,
                                                                                   @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            PaginatedResponse<String> response = pluginResultsClient.listLabelKeys(company, filter);
            return ResponseEntity.accepted().body(response);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PostMapping(value = "/labels/values/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<String>>> listLabelValues(@SessionAttribute("company") String company,
                                                                                     @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            PaginatedResponse<String> response = pluginResultsClient.listLabelValues(company, filter);
            return ResponseEntity.accepted().body(response);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PostMapping(value = "/labels", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<String>>> getLabelKeys(@SessionAttribute("company") String company,
                                                                                  @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            PaginatedResponse<String> response = pluginResultsClient.getLabelKeys(company, filter);
            return ResponseEntity.accepted().body(response);
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PostMapping(value = "/labels/{key}/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<String>>> getLabelValues(@SessionAttribute("company") String company,
                                                                                    @PathVariable("key") String key,
                                                                                    @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            PaginatedResponse<String> response = pluginResultsClient.getLabelValues(company, key, filter);
            return ResponseEntity.accepted().body(response);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'AUDITOR','SUPER_ADMIN')")
    @PostMapping(value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<PluginResultDTO>>> list(@SessionAttribute("company") String company,
                                                                                   @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            PaginatedResponse<PluginResultDTO> response = pluginResultsClient.list(company, filter);
            if (response != null && response.getResponse() != null && response.getResponse().getRecords() != null) {
                response = response.toBuilder()
                        .response(ListResponse.of(IterableUtils.parseIterable(response.getResponse().getRecords(),
                                PluginResultsController::addEpoch)))
                        .build();
            }
            return ResponseEntity.accepted().body(response);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @DeleteMapping(produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> deleteBulkPluginResult(@SessionAttribute("company") String company,
                                                                                     @RequestBody List<UUID> uuids) {
        final List<String> pluginIds = uuids.stream().map(UUID::toString).collect(Collectors.toList());
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(pluginResultsClient.deleteBulkPluginResult(company, pluginIds)));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'AUDITOR','SUPER_ADMIN')")
    @GetMapping(value = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<PluginResultDTO>> getById(@SessionAttribute("company") String company,
                                                                   @PathVariable("id") UUID id) {
        return SpringUtils.deferResponse(() -> {
            PluginResultDTO response = pluginResultsClient.getById(company, id.toString());
            return ResponseEntity.accepted().body(addEpoch(response));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PutMapping(value = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> updateResults(@SessionAttribute("company") String company,
                                                                @PathVariable("id") UUID id,
                                                                @RequestBody PluginResultDTO pluginResultDTO) {
        return SpringUtils.deferResponse(() -> {
            var response = pluginResultsClient.updatePluginResult(company, pluginResultDTO.toBuilder().id(id.toString()).build());
            return ResponseEntity.accepted().body(response);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'AUDITOR','SUPER_ADMIN')")
    @GetMapping(value = "/diff", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, JsonDiff>>> diff(@SessionAttribute("company") String company,
                                                                      @RequestParam("before_id") UUID beforeId,
                                                                      @RequestParam("after_id") UUID afterId) {
        return SpringUtils.deferResponse(() -> {
            Map<String, JsonDiff> response = pluginResultsClient.diff(company, beforeId.toString(), afterId.toString());
            return ResponseEntity.accepted().body(response);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PostMapping(produces = "application/json")
    public DeferredResult<ResponseEntity<String>> createPluginResult(@SessionAttribute("company") String company,
                                                                     @RequestBody PluginResultDTO pluginResultDTO) {
        return SpringUtils.deferResponse(() -> {
            String response = pluginResultsClient.createPluginResult(company, pluginResultDTO);
            return ResponseEntity.accepted().body(response);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PostMapping(path = "/multipart", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> createPluginResultMultiPart(@SessionAttribute(name = "company") String company,
                                                                              @RequestPart("json") MultipartFile pluginResultDTOStr,
                                                                              @RequestPart(name = "result", required = false) MultipartFile resultFile) {
        return SpringUtils.deferResponse(() -> {
            String response = pluginResultsClient.createPluginResultMultipart(company, pluginResultDTOStr, resultFile);
            return ResponseEntity.accepted().body(response);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PostMapping(path = "/multipart/pre-process", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> submitPluginResultWithPreProcessing(@SessionAttribute(name = "company") String company,
                                                                              @RequestPart("json") MultipartFile jsonFile,
                                                                              @RequestPart(name = "result", required = false) MultipartFile resultFile) {
        return SpringUtils.deferResponse(() -> {
            String response = pluginResultsClient.submitPluginResultWithPreProcessing(company, jsonFile, resultFile);
            return ResponseEntity.accepted().body(response);
        });
    }

}
