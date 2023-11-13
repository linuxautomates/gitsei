package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.io.levelops.scm_repo_mapping.clients.ScmRepoMappingClient;
import io.levelops.io.levelops.scm_repo_mapping.models.ScmRepoMappingResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Optional;

@RestController
@RequestMapping("/v1/scm_repo_mapping")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
@Log4j2
public class ScmRepoMappingController {
    private final ScmRepoMappingClient scmRepoMappingClient;

    @Autowired
    public ScmRepoMappingController(
            ScmRepoMappingClient scmRepoMappingClient
    ) {
        this.scmRepoMappingClient = scmRepoMappingClient;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @GetMapping("/repo_mapping")
    public DeferredResult<ResponseEntity<ScmRepoMappingResponse>> getRepoMapping(
            @SessionAttribute(name = "company") String tenantId,
            @RequestParam(value = "integration_id", required = true) String integrationId
    ) {
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.of(Optional.of(scmRepoMappingClient.getScmRepoMapping(tenantId, integrationId)));
        });
    }
}
