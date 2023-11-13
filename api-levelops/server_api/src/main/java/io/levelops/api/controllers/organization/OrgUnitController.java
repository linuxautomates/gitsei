package io.levelops.api.controllers.organization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.authz.acl.client.ACLClient;
import io.harness.authz.acl.client.ACLClientException;
import io.harness.authz.acl.model.AccessCheckRequestDTO;
import io.harness.authz.acl.model.AccessCheckResponseDTO;
import io.harness.authz.acl.model.AccessControlDTO;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.PermissionCheckDTO;
import io.harness.authz.acl.model.ResourceScope;
import io.harness.authz.acl.model.ResourceType;
import io.harness.authz.acl.model.VaildPermissions;
import io.levelops.api.converters.organization.OUConverter;
import io.levelops.api.model.organization.OrgUnitDTO;
import io.levelops.api.model.organization.OrgUnitDTO.IntegrationDetails;
import io.levelops.api.model.organization.OrgUnitDTO.Section;
import io.levelops.auth.auth.authobject.AccessContext;
import io.levelops.auth.auth.config.Auth;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUnitInfo;
import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.OrgUserId;
import io.levelops.commons.databases.models.database.organization.OrgVersion;
import io.levelops.commons.databases.services.dev_productivity.OrgIdentityService;
import io.levelops.commons.databases.services.dev_productivity.services.DevProdTaskReschedulingService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import io.propelo.trellis_framework.client.TrellisAPIControllerClient;
import io.propelo.trellis_framework.client.exception.TrellisControllerClientException;
import io.propelo.trellis_framework.models.events.Event;
import io.propelo.trellis_framework.models.jobs.JobStatus;
import io.propelo.trellis_framework.models.events.EventType;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Log4j2
@RestController
@RequestMapping("/v1/org/units")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
public class OrgUnitController {
    private final OrgUnitHelper unitsHelper;
    private final OrgUnitsDatabaseService unitsService;
    private final OrgIdentityService orgIdentityService;
    private final DevProdTaskReschedulingService devProdTaskReschedulingService;
    private final TrellisAPIControllerClient trellisAPIControllerClient;
    private final Set<String> persistDevProductivityV2EventsTenantsBlacklist;
    private final Auth auth;

    @Autowired
    public OrgUnitController(final OrgUnitHelper unitsHelper, final OrgUnitsDatabaseService unitsService, OrgIdentityService orgIdentityService, DevProdTaskReschedulingService devProdTaskReschedulingService, TrellisAPIControllerClient trellisAPIControllerClient,
                             @Qualifier("persistDevProductivityV2EventsTenantsBlacklist") Set<String> persistDevProductivityV2EventsTenantsBlacklist, Auth auth) {
        this.unitsHelper = unitsHelper;
        this.unitsService = unitsService;
        this.orgIdentityService = orgIdentityService;
        this.devProdTaskReschedulingService = devProdTaskReschedulingService;
        this.trellisAPIControllerClient = trellisAPIControllerClient;
        this.persistDevProductivityV2EventsTenantsBlacklist = persistDevProductivityV2EventsTenantsBlacklist;
        this.auth = auth;
    }

    @PostMapping(path = "/single", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Map<String, Object>>> createUnit(@SessionAttribute(name = "company") final String company, @RequestBody OrgUnitDTO unit) {
        return SpringUtils.deferResponse(() -> {
            var ids = unitsHelper.insertNewOrgUnits(company, Set.of(map(unit)).stream());
            reSchdeuleOUOrgUserMappings(company);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", ids, "errors", ""));
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.GET, value = "{workspaceId}/integrationid/{integrationid}", produces = "application/json")
    public DeferredResult<ResponseEntity<List<OrgUnitInfo>>> integrationDetails(@PathVariable("integrationid") String integrationId,
                                                                                @PathVariable("workspaceId") Integer workspaceId,
                                                                                @SessionAttribute(name = "company") String company) {
        List<Integer> integrationsList = List.of(Integer.parseInt(integrationId));
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(orgIdentityService.getOrgUnitsForIntegration(company, integrationsList, workspaceId)));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, value = "{workspaceId}/integration/list", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<Object, List<OrgUnitInfo>>>> integrationOUDetails(@RequestBody List<Integer> integrationsList,
                                                                                               @PathVariable("workspaceId") Integer workspaceId,
                                                                                               @SessionAttribute(name = "company") String company) {

        Map<Object, List<OrgUnitInfo>> map = orgIdentityService.getOrgUnitsForIntegration(company, integrationsList, workspaceId).stream().collect(Collectors.groupingBy(OrgUnitInfo::getIntegrationId));
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(map));
    }

    private DBOrgUnit map(final OrgUnitDTO unit) {
        var builder = DBOrgUnit.builder()
                .name(unit.getName())
                .description(unit.getDescription())
                .parentRefId(unit.getParentRefId())
                .tags(unit.getTags())
                .tagIds(unit.getTags().stream().map(Integer::parseInt).collect(Collectors.toSet()))
                .ouCategoryId(unit.getOuCategoryId())
                .noOfDashboards(unit.getNoOfDashboards())
                .path(unit.getPath())
                .managers(unit.getManagers() == null ? Set.of() : unit.getManagers().stream()
                        .map(manager -> OrgUserId.builder()
                                .refId(Integer.valueOf(manager.getId()))
                                .email(manager.getEmail())
                                .fullName(manager.getFullName())
                                .build())
                        .collect(Collectors.toSet())
                )
                .defaultDashboardId(unit.getDefaultDashboardId());
        Set<DBOrgContentSection> sections = unit.getSections() == null ? Set.of() : unit.getSections().stream()
                .map(section -> map(section))
                .collect(Collectors.toSet());
        if (unit.getDefaultSection() != null) {
            sections = new HashSet<DBOrgContentSection>(sections);
            sections.add(map(unit.getDefaultSection()).toBuilder().defaultSection(true).build());
        }
        builder.sections(sections);
        if (StringUtils.isNotBlank(unit.getId())) {
            builder.refId(Integer.valueOf(unit.getId()));
        }
        return builder.build();
    }

    private DBOrgContentSection map(Section section) {
        IntegrationDetails details = null;
        Integer integrationId = null;
        if (section.getIntegrations() != null) {
            var tmp = section.getIntegrations().entrySet().iterator().next();
            integrationId = Integer.valueOf(tmp.getKey());
            details = tmp.getValue();
        }

        return DBOrgContentSection.builder()
                .dynamicUsers(section.getDynamicUserDefinition())
                .users(section.getUsers() == null ? Set.of() : section.getUsers().stream().map(Integer::valueOf).collect(Collectors.toSet()))
                .integrationId(integrationId)
                .integrationFilters(details != null ? details.getFilters() : Map.of())
                .build();
    }

    private void reSchdeuleOUOrgUserMappings(final String company) {
        log.info("Rescheduling OU Org User Mappings for company {} starting, trigger OU Create or Update", company);
        boolean scheduled = devProdTaskReschedulingService.reScheduleOUOrgUserMappingsForOneTenant(company);
        log.info("Rescheduling OU Org User Mappings for company {} completed success {}, trigger OU Create or Update", company, scheduled);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Map<String, Object>>> createUnit(@SessionAttribute(name = "company") final String company, @RequestBody Set<OrgUnitDTO> units) {
        return SpringUtils.deferResponse(() -> {
            var errors = new ArrayList<String>();
            var stream = units.stream().map(this::map);
            var ids = unitsHelper.insertNewOrgUnits(company, stream);
            reSchdeuleOUOrgUserMappings(company);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", ids, "errors", errors));
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<OrgUnitDTO>> getUnit(@SessionAttribute(name = "company") final String company, @PathVariable final Integer id, @RequestParam(name = "version", required = false) Integer version) {
        return SpringUtils.deferResponse(() -> {
            var dbUnit = unitsService.get(company, id, version).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OU not found"));
            return ResponseEntity.ok(OUConverter.map(dbUnit));
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'AUDITOR', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @PostMapping(path = "/parentList", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<PaginatedResponse<OrgUnitDTO>>> getParentList(@SessionAttribute(name = "company") final String company, @RequestBody DefaultListRequest request, @RequestParam(name = "version", defaultValue = "0") Integer version) {
        return SpringUtils.deferResponse(() -> {
            var filters = QueryFilter.fromRequestFilters(request.getFilter());
            var listResponse = unitsService.filter(company, filters, request.getPage(), request.getPageSize());
            if(listResponse.getRecords().size()>0){
                DBOrgUnit dbOrgUnit=listResponse.getRecords().get(0);
                String pathBuilder=dbOrgUnit.getPath();
                int i = pathBuilder.lastIndexOf("/");
                List<String> paths=new ArrayList<String>();
                while(i!=0) {
                    String newPath=pathBuilder.substring(0, i);
                    i=newPath.lastIndexOf("/");
                    paths.add(newPath);
                }
                listResponse = unitsService.filter(company, QueryFilter
                        .fromRequestFilters(Map.of("ou_category_id",dbOrgUnit.getOuCategoryId(),"path",paths)), request.getPage(), request.getPageSize());

            }
            return ResponseEntity.ok(PaginatedResponse.of(
                    request.getPage(),
                    request.getPageSize(),
                    listResponse.getTotalCount(),
                    listResponse.getRecords().stream()
                            .map(item -> OUConverter.map(item))
                            .collect(Collectors.toList())));
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'AUDITOR', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @PostMapping(path = "/list", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaginatedResponse<OrgUnitDTO>> listUnits(@SessionAttribute(name = "company") final String company, @RequestBody DefaultListRequest request, @RequestParam(name = "version", defaultValue = "0") Integer version,
                                                                   @SessionAttribute(name = "accessContext") AccessContext accessContext) throws ACLClientException, SQLException {
        var filters = QueryFilter.fromRequestFilters(request.getFilter());
        var listResponse = unitsService.filter(company, filters, request.getPage(), request.getPageSize());
        if (!auth.isLegacy()) {
            listResponse = getOusWithAccessControlDetails(company, listResponse, accessContext);
        }
        return ResponseEntity.ok(PaginatedResponse.of(
                request.getPage(),
                request.getPageSize(),
                listResponse.getTotalCount(),
                listResponse.getRecords().stream()
                        .map(item -> OUConverter.map(item))
                        .collect(Collectors.toList())));
    }

    private DbListResponse<DBOrgUnit> getOusWithAccessControlDetails(String company, DbListResponse<DBOrgUnit> listResponse, AccessContext accessContext) throws ACLClientException {
        List<PermissionCheckDTO> permissionCheckDTOList = new ArrayList();
        Map<String, DBOrgUnit> dbOrgUnitMap = new HashMap<>();
        List<DBOrgUnit> dbOrgUnitList = new ArrayList<>();

        ResourceScope resourceScope = accessContext.getResourceScope();
        log.info("Org: will look for resorcescope {}", resourceScope);
        for (DBOrgUnit dbOrgUnit : listResponse.getRecords()) {
            for (Permission permission : Permission.getPermissionListForResource(ResourceType.SEI_COLLECTIONS)) {
                PermissionCheckDTO permissionCheckDTO = PermissionCheckDTO.builder()
                        .resourceScope(resourceScope)
                        .resourceType(ResourceType.SEI_COLLECTIONS.name())
                        .resourceIdentifier(String.valueOf(dbOrgUnit.getRefId()))
                        .permission(permission.getPermission())
                        .build();
                permissionCheckDTOList.add(permissionCheckDTO);
            }
            dbOrgUnitMap.put(String.valueOf(dbOrgUnit.getRefId()), dbOrgUnit);
        }

        log.info("Org: will look for permissions {}", permissionCheckDTOList);
        AccessCheckRequestDTO accessCheckRequestDTO = AccessCheckRequestDTO.builder()
                .principal(accessContext.getPrincipal())
                .permissions(permissionCheckDTOList)
                .build();

        ACLClient aclClient = accessContext.getAclClient();
        AccessCheckResponseDTO response = aclClient.checkAccess(accessCheckRequestDTO);

        for (Map.Entry<String, DBOrgUnit> entry : dbOrgUnitMap.entrySet()) {
            String ouRefId = entry.getKey();
            DBOrgUnit orgUnit = entry.getValue();
            VaildPermissions.VaildPermissionsBuilder vaildPermissionsBuilder = VaildPermissions.builder();
            List<AccessControlDTO> accessControlDTOS = response.getAccessCheckDataResponse().getAccessControlList().stream().filter(accessControlDTO -> accessControlDTO.getResourceIdentifier().equals(ouRefId))
                    .collect(Collectors.toList());
            accessControlDTOS.forEach(accessControlDTO -> {
                if(Permission.COLLECTIONS_CREATE.getPermission().equals(accessControlDTO.getPermission())){
                    vaildPermissionsBuilder.create(accessControlDTO.isPermitted());
                }else if(Permission.COLLECTIONS_EDIT.getPermission().equals( accessControlDTO.getPermission())){
                    vaildPermissionsBuilder.edit(accessControlDTO.isPermitted());
                }else if(Permission.COLLECTIONS_VIEW.getPermission().equals(accessControlDTO.getPermission())){
                    vaildPermissionsBuilder.view(accessControlDTO.isPermitted());
                }else if(Permission.COLLECTIONS_DELETE.getPermission().equals(accessControlDTO.getPermission())){
                    vaildPermissionsBuilder.delete(accessControlDTO.isPermitted());
                }
            });
            orgUnit = orgUnit.toBuilder()
                    .vaildPermissions(vaildPermissionsBuilder.build())
                    .build();

            dbOrgUnitList.add(orgUnit);
        }
        log.info("Original list size is {}, List size with access control details is {}", listResponse.getTotalCount(), dbOrgUnitList.size());
        return DbListResponse.of(dbOrgUnitList, dbOrgUnitList.size());
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<String>> updateUnits(@SessionAttribute(name = "company") final String company, @RequestBody Set<OrgUnitDTO> units) {
        return SpringUtils.deferResponse(() -> {
            if (units.stream().filter(unit -> StringUtils.isBlank(unit.getId())).findFirst().isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "at least one of the org units is missing the id. Without the id we don't know which org unit to update.");
            }

            unitsHelper.updateUnits(company, units.stream().map(item -> map(item)));
            //Persist OU_Change event with metadata. Processing to be done in event processor
            log.info("persistDevProductivityV2EventsTenantsBlacklist = {} ",persistDevProductivityV2EventsTenantsBlacklist);
            if (! persistDevProductivityV2EventsTenantsBlacklist.contains(company)) {
                log.info("Trellis persist V2 event = true, company {}", company);
                units.stream().forEach(u -> {
                    try {
                        trellisAPIControllerClient.createEvent(Event.builder()
                                .tenantId(company)
                                .eventType(EventType.OU_CHANGE)
                                .status(JobStatus.SCHEDULED)
                                .data(Map.of("ou_ref_id",u.getId()))
                                .build());
                    } catch (TrellisControllerClientException e) {
                        log.error("Error creating OU_CHANGE event for ou_ref_id {} ",u.getId());
                    }
                });
            } else {
                log.info("Trellis persist V2 event = false, company {}", company);
            }

            //TODO : This step needs to be discontinued once the trellis optimization is stabilized
            reSchdeuleOUOrgUserMappings(company);
            return ResponseEntity.ok("ok");
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<String>> deleteUnits(@SessionAttribute(name = "company") final String company, @RequestBody Set<String> ids) {
        return SpringUtils.deferResponse(() -> {
            unitsHelper.deleteUnits(company, ids.stream().map(item -> Integer.valueOf(item)).collect(Collectors.toSet()));
            return ResponseEntity.ok("ok");
        });
    }

    // @PostMapping(path = "/import", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    // public void importUnits(@SessionAttribute(name = "company") final String company, @RequestBody Set<String> users){

    // }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @PostMapping(path = "/values", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, Object>>>> getValues(
            @SessionAttribute(name = "company") final String company,
            @RequestBody DefaultListRequest request,
            @RequestParam(name = "version", required = false) Integer version) {
        return SpringUtils.deferResponse(() -> {
            QueryFilter filters = QueryFilter.fromRequestFilters(request.getFilter());
            if (version != null) {
                filters = filters.toBuilder().strictMatch("version", version).build();
            }
            final var f = filters;
            var results = new ArrayList<Map<String, Object>>();
            var count = new AtomicInteger(0);
            request.getFields().stream().forEach(field -> {
                var listResponse = unitsService.getValues(company, field, f, request.getPage(), request.getPageSize());
                results.add(Map.of(field, listResponse));
                if (count.get() < listResponse.getTotalCount()) {
                    count.set(listResponse.getTotalCount());
                }
            });
            return ResponseEntity.ok(PaginatedResponse.of(
                    request.getPage(),
                    request.getPageSize(),
                    count.get(),
                    results));
        });
    }

    @GetMapping(path = "/versions", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Set<OrgVersion>>> getVersions(
            @SessionAttribute(name = "company") final String company,
            @RequestParam(name = "org_id") final Integer refId,
            @RequestParam(name = "page_size", defaultValue = "50") Integer pageSize,
            @RequestParam(name = "page", defaultValue = "0") Integer page) {
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok(unitsService.getVersions(company, refId));
        });
    }

    @PostMapping(path = "/versions", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public void setActiveVersion(
            @SessionAttribute(name = "company") final String company,
            @RequestBody final OrgUnitActivation request) {
        try {
            unitsHelper.activateVersion(company, request.getRefId(), request.getVersion());
        } catch (SQLException e) {
            log.error("[{}] unable to activate the reqeusted version: {}", company, request, e);
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = OrgUnitActivation.OrgUnitActivationBuilder.class)
    public static class OrgUnitActivation {
        @JsonProperty("ou_id")
        Integer refId;
        @JsonProperty("active_version")
        Integer version;
        @JsonProperty("all_ous")
        Boolean allOUs;
    }
}
