package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.model.organization.OrgIdType;
import io.levelops.api.model.organization.OrgUnitDTO;
import io.levelops.api.services.SnapshotsService;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.dev_productivity.IdType;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/snapshots")
@PreAuthorize("hasAnyAuthority('ADMIN', 'AUDITOR', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
@Log4j2
public class SnapshotsController {
    private final SnapshotsService snapshotsService;

    @Autowired
    public SnapshotsController(SnapshotsService snapshotsService) {
        this.snapshotsService = snapshotsService;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.GET, value = "/users", produces = "application/json")
    public DeferredResult<ResponseEntity<OrgUserDetails>> getUserSnapshot(@SessionAttribute(name = "company") String company,
                                                                  @RequestParam(name = "user_id", required = true) String userId,
                                                                  @RequestParam(name = "user_id_type", required = true) String userIdType) {

        return SpringUtils.deferResponse(() -> {
            if(!EnumUtils.isValidEnumIgnoreCase(IdType.class, userIdType))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user_id_type is provided ");

            OrgUserDetails orgUserDetails = snapshotsService.getUserSnapshot(company, IdType.fromString(userIdType), userId);
            return ResponseEntity.ok(orgUserDetails);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.GET, value = "/orgs", produces = "application/json")
    public DeferredResult<ResponseEntity<OrgUnitDTO>> getOrgSnapshot(@SessionAttribute(name = "company") String company,
                                                                     @RequestParam(name = "org_id", required = true) String orgId,
                                                                     @RequestParam(name = "org_id_type", required = true) OrgIdType orgIdType) {
        return SpringUtils.deferResponse(() -> {
            OrgUnitDTO org = snapshotsService.getOrgSnapShot(company, orgIdType, orgId)
                    .orElseThrow(() -> new NotFoundException("Could not find snapshot for org org_id_type = " + orgIdType.toString() + " org_id = " + orgId));
            return ResponseEntity.ok(org);
        });
    }

}
