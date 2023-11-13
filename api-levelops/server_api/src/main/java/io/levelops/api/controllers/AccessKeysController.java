package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.converters.AccessKeyRequestConverter;
import io.levelops.api.exceptions.ServerApiException;
import io.levelops.api.requests.AccessKeyRequest;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.AccessKey;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.services.AccessKeyService;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.models.ApiKeyToken;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/apikeys")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
@Log4j2
@SuppressWarnings("unused")
public class AccessKeysController {
    private static final String ACTIVITY_LOG_TEXT = "API Access Key %s: %s.";

    private final AccessKeyService accessKeyService;
    private final ActivityLogService activityLogService;
    private final AccessKeyRequestConverter converter;
    private final ObjectMapper objectMapper;

    @Autowired
    public AccessKeysController(final AccessKeyService accessKeyService, final ActivityLogService logService,
                                final AccessKeyRequestConverter requestConverter, final ObjectMapper objectMapper) {
        this.accessKeyService = accessKeyService;
        this.activityLogService = logService;
        this.converter = requestConverter;
        this.objectMapper = objectMapper;
    }

    /**
     * POST - Creates an accesskey.
     *
     * @param company
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<String>> createAccessKey(@SessionAttribute("company") final String company,
                                                                  @SessionAttribute("session_user") final String sessionUser,
                                                                  @RequestBody final AccessKeyRequest request) {
        return SpringUtils.deferResponse(() -> {
            ImmutablePair<AccessKey, String> accessKeyAndSecret = converter.convertToAccessKey(request);
            String uuid = accessKeyService.insert(company, accessKeyAndSecret.getLeft());
            ApiKeyToken token = new ApiKeyToken(accessKeyAndSecret.getRight(), uuid, company);
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(uuid)
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.API_ACCESS_KEY)
                    .body(String.format(ACTIVITY_LOG_TEXT, ActivityLog.Action.CREATED, uuid))
                    .details(Collections.emptyMap())
                    .action(ActivityLog.Action.CREATED)
                    .build());
            return ResponseEntity.ok("{\"id\":\"" + uuid + "\",\"key\":\""
                    + new String(Base64.getEncoder().encode(objectMapper.writeValueAsBytes(token))) + "\"}");
        });
    }

    /**
     * GET - Retrieves an accesskey.
     *
     * @param company
     * @param id
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_CREATE)
    @RequestMapping(method = RequestMethod.GET, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<AccessKey>> getAccessKey(@SessionAttribute("company") final String company,
                                                                  @PathVariable("id") final UUID id) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(
                accessKeyService.get(company, id.toString()).orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "AccessKey with id '" + id + "' not found."))));
    }

    /**
     * DELETE - Deletes an AccessKey.
     *
     * @param company
     * @param id
     * @return
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_CREATE)
    @RequestMapping(method = RequestMethod.DELETE, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> accessKeyDelete(@SessionAttribute("company") final String company,
                                                                  @SessionAttribute("session_user") final String sessionUser,
                                                                  @PathVariable("id") final UUID id) {
        return SpringUtils.deferResponse(() -> {
            if (!accessKeyService.delete(company, id.toString()))
                throw new ServerApiException(HttpStatus.BAD_REQUEST, "Could not find accesskey to delete.");
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(id.toString())
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.API_ACCESS_KEY)
                    .body(String.format(ACTIVITY_LOG_TEXT, ActivityLog.Action.DELETED, id))
                    .details(Collections.emptyMap())
                    .action(ActivityLog.Action.DELETED)
                    .build());
            return ResponseEntity.ok().build();
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @RequestMapping(method = RequestMethod.POST, path = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> accessKeysList(@SessionAttribute("company") final String company,
                                                                 @RequestBody final DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            String name = filter.getFilterValue("partial", Map.class)
                    .map(m -> (String) m.get("name"))
                    .orElse(null);
            String role = filter.getFilterValue("partial", Map.class)
                    .map(m -> (String) m.get("role"))
                    .orElse(null);
            DbListResponse<AccessKey> results = accessKeyService.list(company, name, role,
                    filter.getPage(), filter.getPageSize());
            if (results == null || results.getCount() < 1) {
                return ResponseEntity.ok(objectMapper.writeValueAsString(PaginatedResponse.of(filter.getPage(),
                        filter.getPageSize(), 0, Collections.emptyList())));
            }
            return ResponseEntity.ok(
                    objectMapper.writeValueAsString(PaginatedResponse.of(
                            filter.getPage(),
                            filter.getPageSize(),
                            results.getTotalCount(),
                            results.getRecords())));
        });
    }
}