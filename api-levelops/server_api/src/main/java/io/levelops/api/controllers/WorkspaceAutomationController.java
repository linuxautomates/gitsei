package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.model.workspace_automation.CloneRequest;
import io.levelops.api.services.WorkspaceBulkOperationService;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Map;

@Log4j2
@RestController
@RequestMapping("/v1/workspace/automation")
public class WorkspaceAutomationController {
    private final WorkspaceBulkOperationService workspaceBulkOperationService;

    @Autowired
    public WorkspaceAutomationController(WorkspaceBulkOperationService workspaceBulkOperationService) {
        this.workspaceBulkOperationService = workspaceBulkOperationService;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN','TENANT_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, produces = "application/json", path = "/cloneworkspace")
    public DeferredResult<ResponseEntity<Map<String, String>>> cloneWorkspace(
            @SessionAttribute(name = "company") final String company,
            @SessionAttribute(name = "session_user") String sessionUser,
            @RequestBody CloneRequest cloneRequest) {
        return SpringUtils.deferResponse(() -> {
            log.info("Clone request: " + cloneRequest);
            String newWorkspaceId = workspaceBulkOperationService.cloneWorkspace(
                    cloneRequest.getOriginalWorkspaceId(),
                    cloneRequest.getClonedWorkspaceName(),
                    cloneRequest.getCloneWorkspaceKeyName(),
                    company,
                    sessionUser);
            return ResponseEntity.ok(Map.of("id", newWorkspaceId));
        });
    }
}
