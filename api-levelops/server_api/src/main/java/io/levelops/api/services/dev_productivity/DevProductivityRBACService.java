package io.levelops.api.services.dev_productivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.api.model.dev_productivity.DevProductivityFixedIntervalFilter;
import io.levelops.api.model.dev_productivity.EffectiveOUs;
import io.levelops.auth.auth.config.Auth;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityUserIds;
import io.levelops.commons.databases.models.database.dev_productivity.IdType;
import io.levelops.commons.databases.models.database.organization.DBOrgAccessUsers;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.services.organization.OrgAccessValidationService;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.web.exceptions.ForbiddenException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Service
public class DevProductivityRBACService {
    private final OrgAccessValidationService orgAccessValidationService;
    private final Auth auth;

    @Autowired
    public DevProductivityRBACService(OrgAccessValidationService orgAccessValidationService,  Auth auth) {
        this.orgAccessValidationService = orgAccessValidationService;
        this.auth = auth;
    }

    public EffectiveOUs getEffectiveOUsWithAccess(String company, final Map<String, List<String>> scopes, String requestorEmail, DevProductivityFixedIntervalFilter devProductivityFilter) throws ForbiddenException {
        //If requestor has dev prod scope, give caller access to all requested ous, if no ous filter specified, we will give access to all
        if (MapUtils.emptyIfNull(scopes).containsKey("dev_productivity_write") || !auth.isLegacy()) {
            return EffectiveOUs.builder().ouIds(devProductivityFilter.getOuIds()).orgRefIds(devProductivityFilter.getOuRefIds()).build();
        }

        //If requestor does not have dev prod scope, get all orgs for which requestor is manager
        List<DBOrgUnit> orgUnitsAsManager = PaginationUtils.stream(0, 1, page -> {
            try {
                return orgAccessValidationService.getOrgsManagedUsingManagersEmail(company, requestorEmail, page, 1000);
            } catch (SQLException e) {
                throw new RuntimeStreamException(e);
            }
        }).collect(Collectors.toList());

        //If requestor does not have dev prod scope, get all orgs for which requestor is ou_admin
        List<DBOrgUnit> orgUnitsAsAdmin = PaginationUtils.stream(0, 1, page -> {
            try {
                return orgAccessValidationService.getOrgsManagedByAdminUsingAdminsEmail(company, requestorEmail, page, 1000);
            } catch (SQLException e) {
                throw new RuntimeStreamException(e);
            }
        }).collect(Collectors.toList());
        List<DBOrgUnit> orgUnits = Stream.concat(orgUnitsAsManager.stream(), orgUnitsAsAdmin.stream()).collect(Collectors.toList());
        log.info("orgUnits.size() = {}", CollectionUtils.size(orgUnits));

        if(CollectionUtils.isEmpty(orgUnits)) {
            String error = String.format("User %s, does not have access to Org Dev Productivity!", requestorEmail);
            throw new ForbiddenException(error);
        }

        Set<UUID> requestedOUIds = CollectionUtils.emptyIfNull(devProductivityFilter.getOuIds()).stream().collect(Collectors.toSet());
        Set<Integer> requestedOURefIds = CollectionUtils.emptyIfNull(devProductivityFilter.getOuRefIds()).stream().collect(Collectors.toSet());

        //If OU filter is not specified - we will use all mgrs org units
        //If OU filter is specified - we will filter all mgrs org units using the filter
        List<DBOrgUnit> effectiveOrgUnits = (CollectionUtils.isEmpty(requestedOUIds) && CollectionUtils.isEmpty(requestedOURefIds)) ? orgUnits :
                orgUnits.stream()
                        .filter(o -> requestedOUIds.contains(o.getId()) || requestedOURefIds.contains(o.getRefId()))
                        .collect(Collectors.toList());

        if(CollectionUtils.isEmpty(effectiveOrgUnits)) {
            String error = String.format("User %s, does not have access to Org Dev Productivity!", requestorEmail);
            throw new ForbiddenException(error);
        }
        List<UUID> effectiveUUIDs = effectiveOrgUnits.stream().map(DBOrgUnit::getId).distinct().collect(Collectors.toList());
        return EffectiveOUs.builder().ouIds(effectiveUUIDs).build();
    }

    DevProductivityUserIds getEffectiveUserIdsWithAccess(String company, final Map<String, List<String>> scopes, String requestorEmail, DevProductivityUserIds devProductivityUserIds) throws ForbiddenException {
        //If requestor has dev prod scope, give caller access to all requested ous, if no ous filter specified, we will give access to all
        if (MapUtils.emptyIfNull(scopes).containsKey("dev_productivity_write") || !auth.isLegacy()) {
            return devProductivityUserIds;
        }
        DBOrgAccessUsers dbOrgAccessUsers = null;
        try {
            dbOrgAccessUsers = orgAccessValidationService.getAllAccessUsers(company, requestorEmail, null, devProductivityUserIds.getUserIdType(), devProductivityUserIds.getUserIdList());
        } catch (SQLException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        if ((dbOrgAccessUsers== null) || (CollectionUtils.isEmpty(dbOrgAccessUsers.getAuthorizedUserList()))) {
            String error = String.format("User %s, does not have access to Users Dev Productivity!", requestorEmail);
            throw new ForbiddenException(error);
        }
        return DevProductivityUserIds.builder().userIdType(IdType.OU_USER_IDS).userIdList(CollectionUtils.emptyIfNull(dbOrgAccessUsers.getAuthorizedUserList()).stream().collect(Collectors.toList())).build();
    }
}
