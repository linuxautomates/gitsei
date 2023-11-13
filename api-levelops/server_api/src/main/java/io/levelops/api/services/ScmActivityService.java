package io.levelops.api.services;

import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityUserIds;
import io.levelops.commons.databases.models.database.dev_productivity.IdType;
import io.levelops.commons.databases.models.database.dev_productivity.IntegrationUserDetails;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.databases.models.database.organization.DBOrgAccessUsers;
import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.dev_productivity.OrgIdentityService;
import io.levelops.commons.databases.services.dev_productivity.engine.ScmActivitiesEngine;
import io.levelops.commons.databases.services.dev_productivity.filters.ScmActivityFilter;
import io.levelops.commons.databases.services.dev_productivity.models.ScmActivities;
import io.levelops.commons.databases.services.organization.OrgAccessValidationService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import kotlin.collections.ArrayDeque;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class ScmActivityService {
    private final OrgAccessValidationService orgAccessValidationService;
    private final OrgIdentityService orgIdentityService;
    private final OrgUnitHelper orgUnitHelper;
    private final ScmActivitiesEngine scmActivitiesEngine;
    @Autowired
    ScmActivityService(OrgAccessValidationService orgAccessValidationService, OrgIdentityService orgIdentityService, OrgUnitHelper orgUnitHelper,ScmActivitiesEngine scmActivitiesEngine){
        this.orgAccessValidationService=orgAccessValidationService;
        this.orgIdentityService=orgIdentityService;
        this.orgUnitHelper=orgUnitHelper;
        this.scmActivitiesEngine=scmActivitiesEngine;
    }
    public DbListResponse<ScmActivities> getScmData(Boolean disableCache, final String requestorEmail, final Map<String, List<String>> scopes, final String company, final DefaultListRequest filter, final boolean valueOnly) throws Exception {
        ScmActivityFilter scmActivityFilter = ScmActivityFilter.fromListRequest(filter);
        List<UUID> ouList=orgIdentityService.getOrgUnitForOrgRef(company,scmActivityFilter.getOuRefIds());
        List<OUConfiguration> ouConfig = orgUnitHelper.getOuConfigurationList(company, null, ouList);
        List<Integer> integrationList = ouConfig.get(0).getSections().stream().map(DBOrgContentSection::getIntegrationId).collect(Collectors.toList());
        List<UUID> integrationUser=List.of();
        DbListResponse<OrgUserDetails> orgUserDetailsDbListResponse=DbListResponse.of(null,0);
         if(scmActivityFilter.getAcross().equals(ScmActivityFilter.DISTINCT.integration_user)) {
             DBOrgAccessUsers dbOrgAccessUsers=orgAccessValidationService.getAllAccessUsersByOuId(company, "", ouList);
             List<UUID> orgUsersUUID=dbOrgAccessUsers.getAuthorizedUserList().stream().collect(Collectors.toList());
             DevProductivityUserIds devProductivityUserIds=DevProductivityUserIds.builder().userIdType(IdType.OU_USER_IDS).userIdList(orgUsersUUID).build();
             if(!CollectionUtils.isEmpty(devProductivityUserIds.getUserIdList())) {
                 orgUserDetailsDbListResponse = orgIdentityService.getUserIdentityForAllIntegrations(company, devProductivityUserIds, filter.getPage(), filter.getPageSize());
             }
             List<IntegrationUserDetails> integrationUserDetails= orgUserDetailsDbListResponse.getRecords().stream().map(OrgUserDetails::getIntegrationUserDetailsList).collect(Collectors.toList()).stream().flatMap(l-> l.stream()).collect(Collectors.toList())
                     .stream().filter(user->user.getIntegrationType().isScmFamily()).collect(Collectors.toList());
              integrationUser=integrationUserDetails.stream().map(IntegrationUserDetails::getIntegrationUserId).collect(Collectors.toList());
         }
       List<ScmActivities> scmActivitiesList= scmActivitiesEngine.calculateScmActivities(company,integrationUser,integrationList,scmActivityFilter.getAcross(),scmActivityFilter.getTimeRange(),null, valueOnly);
        List<ScmActivities> paginatedScmActivitiesList= scmActivitiesList.stream().skip(filter.getPage()*filter.getPageSize()).limit(filter.getPageSize()).collect(Collectors.toList());
       return DbListResponse.of(paginatedScmActivitiesList,scmActivitiesList.size());
    }
}
