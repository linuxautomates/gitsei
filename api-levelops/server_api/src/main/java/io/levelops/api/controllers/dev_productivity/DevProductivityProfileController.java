package io.levelops.api.controllers.dev_productivity;

import io.levelops.api.services.DevProductivityProfileService;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/v1/dev_productivity_profiles")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@Log4j2
@SuppressWarnings("unused")
public class DevProductivityProfileController {

    private DevProductivityProfileService devProductivityProfileService;
    private final Set<String> persistDevProductivityV2EventsTenantsBlacklist;

    @Autowired
    public DevProductivityProfileController(DevProductivityProfileService devProductivityProfileService, @Qualifier("persistDevProductivityV2EventsTenantsBlacklist") Set<String> persistDevProductivityV2EventsTenantsBlacklist) {
        this.devProductivityProfileService = devProductivityProfileService;
        this.persistDevProductivityV2EventsTenantsBlacklist = persistDevProductivityV2EventsTenantsBlacklist;
    }

    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> create(@RequestBody DevProductivityProfile profile,
                                                                      @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(Map.of("id", devProductivityProfileService.create(company, profile))));
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> delete(@PathVariable("id") String configId,
                                                       @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            devProductivityProfileService.delete(company, configId);
            return ResponseEntity.ok().build();
        });
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{profileid}", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, String>>> update(@RequestBody DevProductivityProfile profile,
                                                                      @PathVariable("profileid") String profileId,
                                                                      @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            final DevProductivityProfile sanitizedProfile = (profile.getId() != null) ? profile : profile.toBuilder().id(UUID.fromString(profileId)).build();
            if(!profileId.equals(sanitizedProfile.getId().toString())){
                throw new BadRequestException("For customer " + company + " config id " + sanitizedProfile.getId().toString() + " failed to update config!");
            }
            Boolean persistV2Event = !persistDevProductivityV2EventsTenantsBlacklist.contains(company);
            return ResponseEntity.ok(Map.of("id", devProductivityProfileService.update(company, sanitizedProfile,persistV2Event)));
        });
    }

    @RequestMapping(method = RequestMethod.PATCH, value = "/{profileid}/set-default", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> setDefault(@PathVariable("profileid") String profileId,
                                                           @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            devProductivityProfileService.makeDefault(company, profileId);
            return ResponseEntity.ok().build();
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.PATCH, value = "/{profileid}/set-ou-mappings", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> updateOUMappings(@RequestBody DevProductivityProfile profile,
                                                                 @PathVariable("profileid") String profileId,
                                                                 @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            List<String> ouRefIds = profile.getAssociatedOURefIds();
            devProductivityProfileService.updateProfileOUMappings(company,profileId,ouRefIds);
            return ResponseEntity.ok().build();
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, value = "/{profileid}", produces = "application/json")
    public DeferredResult<ResponseEntity<DevProductivityProfile>> getRule(@PathVariable("profileid") String profileId,
                                                                     @SessionAttribute(name = "company") String company) {

        return SpringUtils.deferResponse(() -> {
            DevProductivityProfile rule = devProductivityProfileService.get(company, profileId)
                    .orElseThrow(() -> new NotFoundException("Could not find dev productivity profile with id=" + profileId));
            return ResponseEntity.ok(rule);
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DevProductivityProfile>>> rulesList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            var results = devProductivityProfileService.listByFilter(company, filter);
            PaginatedResponse<DevProductivityProfile> internalApiResponse = PaginatedResponse.of(filter.getPage(), filter.getPageSize(), results);
            return ResponseEntity.ok(internalApiResponse);
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/default", produces = "application/json")
    public DeferredResult<ResponseEntity<DevProductivityProfile>> createDefaultProfile(@SessionAttribute(name = "company") String company) throws Exception {
        return SpringUtils.deferResponse(() -> {
            DevProductivityProfile profile = devProductivityProfileService.getDefaultProfile(company)
                    .orElseThrow(() -> new NotFoundException("Could not find default dev productivity profile for copmpany = " + company));
            return ResponseEntity.ok(profile);
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN', 'ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.GET, value = "/default", produces = "application/json")
    public DeferredResult<ResponseEntity<DevProductivityProfile>> getDefaultProfile(@SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            DevProductivityProfile profile = devProductivityProfileService.getDefaultProfile(company)
                    .orElseThrow(() -> new NotFoundException("Could not find default dev productivity profile for copmpany = " + company));
            return ResponseEntity.ok(profile);
        });
    }
}
