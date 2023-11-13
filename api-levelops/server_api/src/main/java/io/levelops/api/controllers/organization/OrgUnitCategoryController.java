package io.levelops.api.controllers.organization;

import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityParentProfile;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.services.dev_productivity.DevProductivityParentProfileDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@RestController
@RequestMapping(value = {"/v1/org/groups", "v1/org/categories"})
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
public class OrgUnitCategoryController {

    private final OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    private final Boolean devProdProfilesV2Enabled;
    private final Set<String> parentProfilesEnabledTenants;
    private final DevProductivityParentProfileDatabaseService devProductivityParentProfileDatabaseService;

    @Autowired
    public OrgUnitCategoryController(final OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService, @Value("${DEV_PROD_PROFILES_V2_ENABLED:false}") Boolean devProdProfilesV2Enabled, @Qualifier("parentProfilesEnabledTenants") Set<String> parentProfilesEnabledTenants, DevProductivityParentProfileDatabaseService devProductivityParentProfileDatabaseService) {
        this.orgUnitCategoryDatabaseService = orgUnitCategoryDatabaseService;
        this.devProdProfilesV2Enabled = devProdProfilesV2Enabled;
        this.parentProfilesEnabledTenants = parentProfilesEnabledTenants;
        this.devProductivityParentProfileDatabaseService = devProductivityParentProfileDatabaseService;
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'AUDITOR', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @PostMapping(path = "/list", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<PaginatedResponse<OrgUnitCategory>>> listUnits(@SessionAttribute(name = "company") final String company,
                                                                                     @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            var filters = QueryFilter.fromRequestFilters(request.getFilter());
            var listResponse = orgUnitCategoryDatabaseService.filter(company, filters, request.getPage(), request.getPageSize());
            return ResponseEntity.ok(PaginatedResponse.of(
                    request.getPage(),
                    request.getPageSize(),
                    listResponse.getTotalCount(),
                    listResponse.getRecords()));
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<OrgUnitCategory>> createOUGroup(@SessionAttribute(name = "company") String company,
                                                                      @RequestBody OrgUnitCategory orgUnitCategory) {
        return SpringUtils.deferResponse(() -> {
            if (StringUtils.isEmpty(orgUnitCategory.getRootOuName())){
                throw new BadRequestException("The root OU name is required");
            }
            if (StringUtils.isEmpty(orgUnitCategory.getName())){
                throw new BadRequestException("The category name is required");
            }
            if (orgUnitCategory.getWorkspaceId() < 1){
                throw new BadRequestException("The workspace id is required and must be valid");
            }
            OrgUnitCategory unitCategory = orgUnitCategoryDatabaseService.insertReturningOUGroup(company, orgUnitCategory);
            if(BooleanUtils.isTrue(devProdProfilesV2Enabled) || CollectionUtils.emptyIfNull(parentProfilesEnabledTenants).contains(company)){
                mapRootOUsToCentralTrellisProfile(company, List.of(String.valueOf(unitCategory.getId())));
            }
            return ResponseEntity.accepted().body(unitCategory);
        });
    }

    private void mapRootOUsToCentralTrellisProfile(final String company,List<String> categoryIds) {
        Set<String> rootOuRefIds = null;
        DevProductivityParentProfile centralProfile = null;
        try {
            rootOuRefIds = orgUnitCategoryDatabaseService.filter(company,QueryFilter.builder().strictMatches(Map.of("id",categoryIds)).build(),0,10).getRecords()
                    .stream().map(OrgUnitCategory::getRootOuRefId).map(String::valueOf).collect(Collectors.toSet());
            centralProfile =devProductivityParentProfileDatabaseService.getDefaultDevProductivityProfile(company).get();
            if(centralProfile != null && CollectionUtils.isNotEmpty(rootOuRefIds))
                devProductivityParentProfileDatabaseService.updateProfileOUMappings(company,centralProfile.getId(), rootOuRefIds);
        } catch (SQLException e) {
            log.error("Unable to update root OU -> central profile mapping ",e);
        }
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.PUT, path ="/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<OrgUnitCategory>> updateOUGroup(@SessionAttribute(name = "company") String company,
                                                                         @PathVariable ("id") UUID groupId,
                                                                         @RequestBody OrgUnitCategory orgUnitCategory) throws BadRequestException {
        if(groupId == null || StringUtils.isEmpty(groupId.toString())) {
            throw new BadRequestException("The Group Id is required to update the OU Group");
        }
        orgUnitCategory = OrgUnitCategory.builder()
                .id(groupId)
                .isPredefined(orgUnitCategory.getIsPredefined())
                .name(orgUnitCategory.getName())
                .description(orgUnitCategory.getDescription())
                .workspaceId(orgUnitCategory.getWorkspaceId())
                .enabled(orgUnitCategory.getEnabled())
                .build();
        final OrgUnitCategory finalOrgUnitCategory = orgUnitCategory;
        return SpringUtils.deferResponse(() -> {
            OrgUnitCategory dbOrgUnitCategory = orgUnitCategoryDatabaseService.updateReturningOrgGroup(company, finalOrgUnitCategory);
            return ResponseEntity.accepted().body(dbOrgUnitCategory);
        });
    }

    /**
     * GET - Retrieves a ougroup object.
     *
     * @param company
     * @param id
     * @return
     */
    @PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<OrgUnitCategory>> getOuGroupById(@SessionAttribute("company") final String company,
                                                                          @PathVariable("id") final UUID id) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(orgUnitCategoryDatabaseService.get(company, id.toString()).orElseThrow(() ->
                new NotFoundException("Couldn't find the requested best practice with id '" + id + "'"))));
    }


    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> OuGroupsBulkDelete(
            @SessionAttribute("company") final String company,
            @RequestBody final List<String> ids) {
        return SpringUtils.deferResponse(() -> {
            try {
                List<String> filteredIds = ids.stream()
                        .map(UUID::fromString)
                        .map(UUID::toString)
                        .collect(Collectors.toList());
                orgUnitCategoryDatabaseService.bulkDelete(company, filteredIds);
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(ids, true, null));
            } catch (Exception e) {
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(ids, false, e.getMessage()));
            }
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.DELETE, path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> OuGroupDelete(
            @SessionAttribute("company") final String company,
            @PathVariable final String id) {
        return SpringUtils.deferResponse(() -> {
            try {
                orgUnitCategoryDatabaseService.bulkDelete(company, List.of(id));
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(List.of(id), true, null));
            } catch (Exception e) {
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(List.of(id), false, e.getMessage()));
            }
        });
    }

}
