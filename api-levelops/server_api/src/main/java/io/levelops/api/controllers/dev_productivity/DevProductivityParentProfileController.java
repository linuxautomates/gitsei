package io.levelops.api.controllers.dev_productivity;

import io.levelops.api.model.dev_productivity.CopyDevProductivityParentProfileRequest;
import io.levelops.api.services.DevProductivityParentProfileService;
import io.levelops.api.services.DevProductivityProfileService;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityParentProfile;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/v1/dev_productivity_parent_profiles")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@Log4j2
@SuppressWarnings("unused")
public class DevProductivityParentProfileController {

    private DevProductivityParentProfileService devProductivityParentProfileService;
    private final Set<String> persistDevProductivityV2EventsTenantsBlacklist;

    @Autowired
    public DevProductivityParentProfileController(DevProductivityParentProfileService devProductivityParentProfileService, @Qualifier("persistDevProductivityV2EventsTenantsBlacklist") Set<String> persistDevProductivityV2EventsTenantsBlacklist) {
        this.devProductivityParentProfileService = devProductivityParentProfileService;
        this.persistDevProductivityV2EventsTenantsBlacklist = persistDevProductivityV2EventsTenantsBlacklist;
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{profileid}", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> update(@RequestBody DevProductivityParentProfile profile,
                                                                      @PathVariable("profileid") String profileId,
                                                                      @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            final DevProductivityParentProfile sanitizedProfile = (profile.getId() != null) ? profile : profile.toBuilder().id(UUID.fromString(profileId)).build();
            if(!profileId.equals(sanitizedProfile.getId().toString())){
                throw new BadRequestException("For customer " + company + " config id " + sanitizedProfile.getId().toString() + " failed to update config!");
            }
            Boolean persistV2Event = !persistDevProductivityV2EventsTenantsBlacklist.contains(company);
            return ResponseEntity.ok(Map.of("id", devProductivityParentProfileService.update(company, sanitizedProfile,persistV2Event)));
        });
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/copy", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, Boolean>>> copy(@RequestBody CopyDevProductivityParentProfileRequest request,
                                                                      @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            DevProductivityParentProfile profile = request.getParentProfile();
            List<String> targetOUs = request.getTargetOuRefIds();
            Boolean persistV2Event = !persistDevProductivityV2EventsTenantsBlacklist.contains(company);
            return ResponseEntity.ok(Map.of("status", devProductivityParentProfileService.copy(company, profile, targetOUs)));
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.PATCH, value = "/{ouRefId}/enable-trellis", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, Boolean>>> enableTrellis(@PathVariable("ouRefId") String ouRefId,
                                                                                   @SessionAttribute(name = "company") String company) {

        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok(Map.of("status",devProductivityParentProfileService.enableTrellisForOU(company, ouRefId)));
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.PATCH, value = "/{ouRefId}/disable-trellis", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, Boolean>>> disableTrellis(@PathVariable("ouRefId") String ouRefId,
                                                                              @SessionAttribute(name = "company") String company) {

        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok(Map.of("status",devProductivityParentProfileService.disableTrellisForOU(company, ouRefId)));
        });
    }


    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, value = "/{profileid}", produces = "application/json")
    public DeferredResult<ResponseEntity<DevProductivityParentProfile>> getProfile(@PathVariable("profileid") String profileId,
                                                                     @SessionAttribute(name = "company") String company) {

        return SpringUtils.deferResponse(() -> {
            DevProductivityParentProfile rule = devProductivityParentProfileService.get(company, profileId)
                    .orElseThrow(() -> new NotFoundException("Could not find dev productivity profile with id=" + profileId));
            return ResponseEntity.ok(rule);
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DevProductivityParentProfile>>> profilesList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            var results = devProductivityParentProfileService.listByFilter(company, filter);
            PaginatedResponse<DevProductivityParentProfile> internalApiResponse = PaginatedResponse.of(filter.getPage(), filter.getPageSize(), results);
            return ResponseEntity.ok(internalApiResponse);
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/default", produces = "application/json")
    public DeferredResult<ResponseEntity<DevProductivityParentProfile>> createDefaultProfile(@SessionAttribute(name = "company") String company) throws Exception {
        return SpringUtils.deferResponse(() -> {
            DevProductivityParentProfile profile = devProductivityParentProfileService.createDefaultProfile(company)
                    .orElseThrow(() -> new NotFoundException("Could not find default dev productivity profile for copmpany = " + company));
            return ResponseEntity.ok(profile);
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, value = "/default", produces = "application/json")
    public DeferredResult<ResponseEntity<DevProductivityParentProfile>> getDefaultProfile(@SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            DevProductivityParentProfile profile = devProductivityParentProfileService.getDefaultProfile(company)
                    .orElseThrow(() -> new NotFoundException("Could not find default dev productivity parent profile for copmpany = " + company));
            return ResponseEntity.ok(profile);
        });
    }
}
