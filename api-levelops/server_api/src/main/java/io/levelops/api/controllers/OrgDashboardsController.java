package io.levelops.api.controllers;

import io.harness.authz.acl.client.ACLClient;
import io.harness.authz.acl.client.ACLClientException;
import io.harness.authz.acl.model.AccessCheckRequestDTO;
import io.harness.authz.acl.model.AccessCheckResponseDTO;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.PermissionCheckDTO;
import io.harness.authz.acl.model.ResourceScope;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.model.organization.OrgUnitDashboardDTO;
import io.levelops.auth.auth.authobject.AccessContext;
import io.levelops.auth.auth.config.Auth;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.dashboard.OUDashboard;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.services.DefaultConfigService;
import io.levelops.commons.databases.services.OUDashboardService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/ous")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
@Log4j2
@SuppressWarnings("unused")
public class OrgDashboardsController {
    private static final String DEFAULT_TENANT_CONFIG_NAME = "DEFAULT_DASHBOARD";
    private static final String RBAC_KEY = "rbac";

    private final DefaultConfigService configService;
    private final OUDashboardService ouDashboardService;
    private final OrgUnitsDatabaseService orgUnitsDatabaseService;
    private final UserService userService;
    private final Auth auth;

    @Autowired
    public OrgDashboardsController(OUDashboardService ouDashboardService,
                                   OrgUnitsDatabaseService orgUnitsDatabaseService,
                                   DefaultConfigService configService,
                                   UserService userService, Auth auth) {
        this.configService = configService;
        this.ouDashboardService = ouDashboardService;
        this.userService = userService;
        this.orgUnitsDatabaseService = orgUnitsDatabaseService;
        this.auth = auth;
    }

    @SuppressWarnings("unchecked")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, value = "/{ou_id}/dashboards/list", produces = "application/json")
    public ResponseEntity<PaginatedResponse<OUDashboard>> dashboardsList(
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String sessionUser,
            @SessionAttribute(name = "user_type") String userType,
            @PathVariable("ou_id") String ouId,
            @RequestBody DefaultListRequest filter,
            @SessionAttribute(name = "accessContext") AccessContext accessContext) throws ACLClientException, SQLException, NotFoundException {
        // returning object of Type ResponseEntity<PaginatedResponse<OUDashboard>> since this API is being called from harness platform
        // and it expect response in this format instead of deferResponse which returns generic object
        Map<String, Object> partial = (Map<String, Object>) filter.getFilter().get("partial");
        Boolean inherited = (Boolean) filter.getFilter().get("inherited");
        if (inherited == null) {
            inherited = false;
        }
        boolean isRbacFilterEnabled = filter.getFilter().get("has_rbac_access") != null;
        String rbacFilterEmail = null;

        if (isRbacFilterEnabled && (userType.equals(RoleType.PUBLIC_DASHBOARD.toString()) || userType.equals(RoleType.ORG_ADMIN_USER.toString()))) {
            log.info("PUBLIC_DASHBOARD user type, setting rbacFilterEmail");
            rbacFilterEmail = sessionUser;
        } else if (isRbacFilterEnabled){
            log.info("Admin or equivalent user type, setting rbacFilterEmail to null");
        }

        OUDashboardService.DashboardFilter dashboardFilter = OUDashboardService.DashboardFilter.builder().name((String) (partial != null ?
                        partial.getOrDefault("name", null) : null))
                .exactName(filter.getFilter().get("exact_name") != null ? (String) filter.getFilter().get("exact_name") : null)
                .ouId(UUID.fromString(ouId))
                .isPublic((Boolean) filter.getFilter().get("public"))
                .inherited(inherited)
                .rbacUserEmail(rbacFilterEmail)
                .build();
        Optional<DBOrgUnit> optionalDBOrgUnit = orgUnitsDatabaseService.get(company, ouId);
        if (optionalDBOrgUnit.isEmpty())
            throw new NotFoundException("Invalid OU Id.");
        DbListResponse<OUDashboard> dashboards = ouDashboardService.listByOuFilters(
                company,
                dashboardFilter,
                optionalDBOrgUnit.get(),
                filter.getPage(),
                filter.getPageSize(),
                filter.getSort());
        String defaultId = configService.getDefaultId(company, DEFAULT_TENANT_CONFIG_NAME);

        if(!auth.isLegacy()) {
            dashboards = getDashboradsWithAccessControlDetails(company, dashboards, accessContext);
        }

        return ResponseEntity.ok(
                PaginatedResponse.of(
                        filter.getPage(),
                        filter.getPageSize(),
                        dashboards.getTotalCount(),
                        ListUtils.emptyIfNull(dashboards.getRecords())));
    }

    private DbListResponse<OUDashboard> getDashboradsWithAccessControlDetails(String company, DbListResponse<OUDashboard> dashboards, AccessContext accessContext) throws ACLClientException {

        List<PermissionCheckDTO> permissionCheckDTOList = new ArrayList();
        ResourceScope resourceScope = accessContext.getResourceScope();
        log.info("Insights: will look for resorcescope {}", resourceScope);
        for(OUDashboard ouDashboard : dashboards.getRecords()){
            PermissionCheckDTO permission = PermissionCheckDTO.builder()
                    .resourceScope(resourceScope)
                    .resourceType(ResourceType.SEI_INSIGHTS.name())
                    .resourceIdentifier(String.valueOf(ouDashboard.getDashboardId()))
                    .permission(Permission.INSIGHTS_VIEW.getPermission())
                    .build();
            permissionCheckDTOList.add(permission);
        }
        log.info("Insights: will look for permissions {}", permissionCheckDTOList);
        AccessCheckRequestDTO accessCheckRequestDTO = AccessCheckRequestDTO.builder()
                .principal(accessContext.getPrincipal())
                .permissions(permissionCheckDTOList)
                .build();

        ACLClient aclClient = accessContext.getAclClient();

        AccessCheckResponseDTO response = aclClient.checkAccess(accessCheckRequestDTO);

        Set<String> permittedList = response.getAccessCheckDataResponse().getAccessControlList().stream().filter(f  -> f.isPermitted()).map(a -> a.getResourceIdentifier()).collect(Collectors.toSet());
        List<OUDashboard> dashboards2 = dashboards.getRecords().stream().filter(d -> permittedList.contains(String.valueOf(d.getDashboardId()))).collect(Collectors.toList());
        log.info("Original list size is {}, List size with access control details is {}", dashboards.getTotalCount(), dashboards2.size());
        return DbListResponse.of(dashboards2, dashboards2.size());
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_EDIT)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.PUT, value = "/{ou_id}/dashboards/{dashboard_id}", produces = "application/json")
    public Object dashUpdate(@RequestBody OrgUnitDashboardDTO ouDashboardDto,
                             @PathVariable("dashboard_id") Integer dashboardId,
                             @PathVariable("ou_id") String ouId,
                             @SessionAttribute(name = "company") String company,
                             @SessionAttribute("session_user") final String sessionUser) {


        OUDashboard ouDashboard = OUDashboard.builder().ouId(UUID.fromString(ouId)).dashboardId(dashboardId).dashboardOrder(ouDashboardDto.getDashboardOrder()).build();

        return SpringUtils.deferResponse(() -> {
            ouDashboardService.insert(company, ouDashboard);

            return ResponseEntity.ok().build();
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_EDIT)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.PUT, value = "/{ou_id}/dashboards", produces = "application/json")
    public Object updateDashboard(@RequestBody List<OrgUnitDashboardDTO> ouDashboardDtoList,
                                  @PathVariable("ou_id") String ouId,
                                  @SessionAttribute(name = "company") String company,
                                  @SessionAttribute("session_user") final String sessionUser) {

        List<OUDashboard> dashboardList = ouDashboardDtoList.stream().map(dashboard -> map(dashboard)).collect(Collectors.toList());
        return SpringUtils.deferResponse(() -> {
            ouDashboardService.updateOuMapping(company, dashboardList, UUID.fromString(ouId));

            return ResponseEntity.ok().build();
        });
    }

    private OUDashboard map(OrgUnitDashboardDTO ouDashboardDto) {
        OUDashboard ouDashboard = OUDashboard.builder().ouId(ouDashboardDto.getOuId()).dashboardId(ouDashboardDto.getDashboardId()).dashboardOrder(ouDashboardDto.getDashboardOrder()).build();
        return ouDashboard;
    }


}