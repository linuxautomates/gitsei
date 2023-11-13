package io.levelops.api.converters.organization;

import io.levelops.api.model.organization.OrgUnitDTO;
import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.PropeloUserId;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class OUConverter {
    private static OrgUnitDTO.Section map(DBOrgContentSection section) {
        return OrgUnitDTO.Section.builder()
                .id(section.getId().toString())
                .integrations(section.getIntegrationId() == null ? null: Map.of(
                        section.getIntegrationId().toString(),
                        OrgUnitDTO.IntegrationDetails.builder()
                                .filters(section.getIntegrationFilters())
                                .name(section.getIntegrationName())
                                .type(section.getIntegrationType().toString())
                                .build()
                ))
                .dynamicUserDefinition(section.getDynamicUsers())
                .users(CollectionUtils.isEmpty(section.getUsers()) ? Set.of() : section.getUsers().stream().map(tmp -> tmp.toString()).collect(Collectors.toSet()))
                .build();
    }

    public static OrgUnitDTO map(final DBOrgUnit dbUnit){
        return OrgUnitDTO.builder()
                .id(String.valueOf(dbUnit.getRefId()))
                .ouId(dbUnit.getId())
                .name(dbUnit.getName())
                .description(dbUnit.getDescription())
                .version(dbUnit.getVersions().stream().max((v,b) -> v < b ? b : v).get().toString())
                .ouCategoryId(dbUnit.getOuCategoryId())
                .path(dbUnit.getPath())
                .noOfDashboards(dbUnit.getNoOfDashboards())
                .workspaceId(dbUnit.getWorkspaceId())
                .managers(dbUnit.getManagers().stream()
                        .map(id -> io.levelops.api.model.organization.OrgUnitDTO.OrgUserId.builder()
                                .id(String.valueOf(id.getRefId()))
                                .fullName(id.getFullName())
                                .email(id.getEmail())
                                .build())
                        .collect(Collectors.toSet()))
                .admins(dbUnit.getAdmins().stream()
                        .map(id -> io.levelops.api.model.organization.OrgUnitDTO.OrgUserId.builder()
                                .id(String.valueOf(id.getUserId()))
                                .fullName(id.getFullName())
                                .email(id.getEmail())
                                .build())
                        .collect(Collectors.toSet()))
                .sections(CollectionUtils.isEmpty(dbUnit.getSections()) ? Set.of() : dbUnit.getSections().stream()
                        .filter(s -> !s.getDefaultSection())
                        .map(section -> map(section))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .defaultSection(CollectionUtils.isEmpty(dbUnit.getSections()) ? null : dbUnit.getSections().stream()
                        .filter(DBOrgContentSection::getDefaultSection)
                        .map(section -> map(section))
                        .findFirst()
                        .orElse(null)
                )
                .defaultDashboardId(dbUnit.getDefaultDashboardId())
                .tags(dbUnit.getTagIds().stream().map(Object::toString).collect(Collectors.toSet()))
                .parentRefId(dbUnit.getParentRefId())
                .createdAt(dbUnit.getCreatedAt())
                .workflowProfileId(dbUnit.getWorkflowProfileId())
                .workflowProfileName(dbUnit.getWorkflowProfileName())
                .trellisProfileId(dbUnit.getTrellisProfileId())
                .trellisProfileName(dbUnit.getTrellisProfileName())
                .validPermissions(dbUnit.getVaildPermissions())
                .build();
    }
}
