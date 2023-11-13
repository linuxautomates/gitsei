package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.databases.models.database.organization.OrgUserId;
import io.levelops.commons.databases.models.organization.OrgUnitDTO;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.web.util.SpringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/v1/tenants/{company}/org/units")
public class OrgUnitsController {

    private final OrgUnitHelper unitsHelper;

    @Autowired
    public OrgUnitsController(OrgUnitHelper unitsHelper) {
        this.unitsHelper = unitsHelper;
    }

    private DBOrgUnit map(final OrgUnitDTO unit) {
        var builder = DBOrgUnit.builder()
                .name(unit.getName())
                .description(unit.getDescription())
                .parentRefId(unit.getParentRefId())
                .tags(unit.getTags())
                .tagIds(unit.getTags().stream().map(Integer::parseInt).collect(Collectors.toSet()))
                .managers(unit.getManagers() == null ? Set.of() :unit.getManagers().stream()
                        .map(manager -> OrgUserId.builder()
                                .refId(Integer.valueOf(manager.getId()))
                                .email(manager.getEmail())
                                .fullName(manager.getFullName())
                                .build())
                        .collect(Collectors.toSet())
                );
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

    private DBOrgContentSection map(OrgUnitDTO.Section section) {
        OrgUnitDTO.IntegrationDetails details = null;
        Integer integrationId = null;
        if(section.getIntegrations() != null) {
            var tmp = section.getIntegrations().entrySet().iterator().next();
            integrationId = Integer.valueOf(tmp.getKey());
            details = tmp.getValue();
        }

        return DBOrgContentSection.builder()
                .dynamicUsers(section.getDynamicUserDefinition())
                .users(section.getUsers() == null ? Set.of(): section.getUsers().stream().map(Integer::valueOf).collect(Collectors.toSet()))
                .integrationId(integrationId)
                .integrationFilters(details != null ? details.getFilters() : Map.of())
                .build();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Map<String, Object>>> createUnit(@PathVariable("company") final String company, @RequestBody Set<OrgUnitDTO> units){
        return SpringUtils.deferResponse(() -> {
            var errors = new ArrayList<String>();
            var stream = units.stream().map(unit -> map(unit));
            var ids = unitsHelper.insertNewOrgUnits(company, stream);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", ids, "errors", errors));
        });
    }


}
