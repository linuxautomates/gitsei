package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.config_tables.clients.ConfigTableClient;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/v1/config-tables")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@SuppressWarnings("unused")
public class ConfigTableController {

    private final ConfigTableClient configTableClient;
    private final UserService userService;
    private final ActivityLogService activityLogService;

    @Autowired
    public ConfigTableController(ConfigTableClient configTableClient,
                                 UserService userService,
                                 ActivityLogService activityLogService) {
        this.configTableClient = configTableClient;
        this.userService = userService;
        this.activityLogService = activityLogService;
    }

    private void logActivity(String company, String sessionUser, ActivityLog.Action action, String msg, String id) throws SQLException {
        activityLogService.insert(company, ActivityLog.builder()
                .targetItem(id)
                .email(sessionUser)
                .targetItemType(ActivityLog.TargetItemType.CONFIG_TABLE)
                .action(action)
                .details(Collections.emptyMap())
                .body(String.format("%s Config Table: %s.", msg, id))
                .build());

    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN','ORG_ADMIN_USER')")
    public DeferredResult<ResponseEntity<ConfigTable>> get(@SessionAttribute("company") String company,
                                                           @PathVariable("id") String id,
                                                           @RequestParam(value = "expand", required = false, defaultValue = "") List<String> expand) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok().body(configTableClient.get(company, id, expand)));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PostMapping("/list")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN','ORG_ADMIN_USER')")
    public DeferredResult<ResponseEntity<PaginatedResponse<ConfigTable>>> list(@SessionAttribute("company") final String company,
                                                                               @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok().body(configTableClient.list(company, request)));
    }

    /**
     * Creates a new config table.
     * Required fields: name
     * Optional fields: schema, rows, creator
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PostMapping
    public DeferredResult<ResponseEntity<Map<String, String>>> create(@SessionAttribute("company") final String company,
                                                                      @SessionAttribute("session_user") String sessionUser,
                                                                      @RequestBody ConfigTable configTable) {
        return SpringUtils.deferResponse(() -> {
            String userId = userService.getByEmail(company, sessionUser).map(User::getId).orElse(null);
            ConfigTable configTableWithUser = configTable.toBuilder()
                    .createdBy(userId)
                    .build();
            var response = configTableClient.create(company, configTableWithUser);
            logActivity(company, sessionUser, ActivityLog.Action.CREATED, "Created", response.get("id"));
            return ResponseEntity.ok().body(response);
        });
    }

    /**
     * Creates a new config table.
     * Required fields: id
     * Optional fields: schema, rows, creator, updated_by (recommended)
     */
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @PutMapping("/{id}")
    public DeferredResult<ResponseEntity<Map<String, ?>>> update(@SessionAttribute("company") final String company,
                                                                 @SessionAttribute("session_user") String sessionUser,
                                                                 @PathVariable("id") String id,
                                                                 @RequestBody ConfigTable configTable) {
        return SpringUtils.deferResponse(() -> {
            String userId = userService.getByEmail(company, sessionUser).map(User::getId).orElse(null);
            ConfigTable configTableWithUser = configTable.toBuilder()
                    .updatedBy(userId)
                    .build();
            var response = configTableClient.update(company, id, configTableWithUser);
            logActivity(company, sessionUser, ActivityLog.Action.EDITED, "Edited", (String) response.get("id"));
            return ResponseEntity.ok().body(response);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @DeleteMapping("/{id}")
    public DeferredResult<ResponseEntity<DeleteResponse>> delete(@SessionAttribute("company") final String company,
                                                                 @SessionAttribute("session_user") String sessionUser,
                                                                 @PathVariable("id") String id) {
        return SpringUtils.deferResponse(() -> {
                var response = configTableClient.delete(company, id);
                if (BooleanUtils.isTrue(response.getSuccess())) {
                    logActivity(company, sessionUser, ActivityLog.Action.DELETED, "Deleted", response.getId());
                }
            return ResponseEntity.ok(response);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @DeleteMapping
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> bulkDelete(@SessionAttribute("company") final String company,
                                                                         @SessionAttribute("session_user") String sessionUser,
                                                                         @RequestBody List<String> ids) {
        return SpringUtils.deferResponse(() -> {
            var responses = configTableClient.bulkDelete(company, ids);
            logActivity(company, sessionUser, ActivityLog.Action.DELETED, "Deleted ids: "+ids, String.valueOf(responses));
            return ResponseEntity.ok(responses);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @GetMapping("/{id}/revisions/{version}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN','ORG_ADMIN_USER')")
    public DeferredResult<ResponseEntity<ConfigTable>> getRevision(@SessionAttribute("company") final String company,
                                                                   @PathVariable("id") String id,
                                                                   @PathVariable("version") String version) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok().body(configTableClient.getRevision(company, id, version)));
    }

}
