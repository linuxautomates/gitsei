package io.levelops.api.services.dev_productivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import io.levelops.api.services.DevProductivityParentProfileService;
import io.levelops.api.services.DevProductivityProfileService;
import io.levelops.auth.auth.config.Auth;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.TenantSCMSettings;
import io.levelops.commons.databases.models.database.dev_productivity.*;
import io.levelops.commons.databases.models.database.organization.DBOrgAccessUsers;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.OrgUserId;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.DefaultListRequestUtils;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.ScmQueryUtils;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.databases.services.dev_productivity.DevProductivityRelativeScoreService;
import io.levelops.commons.databases.services.dev_productivity.OrgUserDevProductivityProfileMappingDatabaseService;
import io.levelops.commons.databases.services.dev_productivity.engine.DevProductivityEngine;
import io.levelops.commons.databases.services.dev_productivity.OrgIdentityService;
import io.levelops.commons.databases.services.dev_productivity.engine.ScmActivitiesEngine;
import io.levelops.commons.databases.services.dev_productivity.filters.ScmActivityFilter;
import io.levelops.commons.databases.services.dev_productivity.models.ScmActivities;
import io.levelops.commons.databases.services.dev_productivity.utils.TenantSCMSettingsUtils;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.databases.services.organization.OrgAccessValidationService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.ForbiddenException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static io.levelops.api.services.dev_productivity.DevProductivityReportUtils.DEV_PRODUCTIVITY_REPORT_CACHE_TIME_UNIT;
import static io.levelops.api.services.dev_productivity.DevProductivityReportUtils.DEV_PRODUCTIVITY_REPORT_CACHE_VALUE;

@Log4j2
@Service
public class UserDevProductivityReportService {
    private final ObjectMapper mapper;
    private final DevProductivityEngine devProductivityEngine;
    private final DevProductivityProfileService devProductivityProfileService;
    private final DevProductivityParentProfileService devProductivityParentProfileService;
    private final OrgIdentityService orgIdentityService;
    private final JiraFilterParser jiraFilterParser;
    private final TenantConfigService tenantConfigService;
    private final AggCacheService cacheService;
    private final OrgAccessValidationService orgAccessValidationService;
    private final DevProductivityRelativeScoreService devProductivityRelativeScoreService;
    private final Executor userDevProductivityReportTaskExecutor;
    private final ScmActivitiesEngine scmActivitiesEngine;
    private final OrgUnitHelper orgUnitHelper;
    private final Set<String> readDevProductivityV2UserReportTenants;
    private final boolean readDevProductivityV2Enabled;
    private final Auth auth;
    private final Boolean devProdProfilesV2Enabled;
    private final Set<String> parentProfilesEnabledTenants;
    private final OrgUserDevProductivityProfileMappingDatabaseService orgUserDevProductivityProfileMappingDatabaseService;
    private final OrgUsersDatabaseService orgUsersDatabaseService;



    @Autowired
    public UserDevProductivityReportService(ObjectMapper mapper, DevProductivityEngine devProductivityEngine, DevProductivityProfileService devProductivityProfileService,
                                            DevProductivityParentProfileService devProductivityParentProfileService, OrgIdentityService orgIdentityService, JiraFilterParser jiraFilterParser, TenantConfigService tenantConfigService, AggCacheService cacheService, OrgAccessValidationService orgAccessValidationService,
                                            DevProductivityRelativeScoreService devProductivityRelativeScoreService,
                                            @Qualifier("userDevProductivityReportTaskExecutor") Executor userDevProductivityReportTaskExecutor, ScmActivitiesEngine scmActivitiesEngine, OrgUnitHelper orgUnitHelper,
                                            @Qualifier("readDevProductivityV2UserReportTenants") Set<String> readDevProductivityV2UserReportTenants,
                                            Auth auth, @Qualifier("readDevProductivityV2Enabled") boolean readDevProductivityV2Enabled, Boolean devProdProfilesV2Enabled, Set<String> parentProfilesEnabledTenants, OrgUserDevProductivityProfileMappingDatabaseService orgUserDevProductivityProfileMappingDatabaseService, OrgUsersDatabaseService orgUsersDatabaseService) {
        this.mapper = mapper;
        this.devProductivityEngine = devProductivityEngine;
        this.devProductivityProfileService = devProductivityProfileService;
        this.devProductivityParentProfileService = devProductivityParentProfileService;
        this.orgIdentityService = orgIdentityService;
        this.jiraFilterParser = jiraFilterParser;
        this.tenantConfigService = tenantConfigService;
        this.cacheService = cacheService;
        this.orgAccessValidationService = orgAccessValidationService;
        this.devProductivityRelativeScoreService = devProductivityRelativeScoreService;
        this.userDevProductivityReportTaskExecutor = userDevProductivityReportTaskExecutor;
        this.scmActivitiesEngine = scmActivitiesEngine;
        this.orgUnitHelper = orgUnitHelper;
        this.readDevProductivityV2UserReportTenants = readDevProductivityV2UserReportTenants;
        this.readDevProductivityV2Enabled = readDevProductivityV2Enabled;
        this.auth = auth;
        this.devProdProfilesV2Enabled = devProdProfilesV2Enabled;
        this.parentProfilesEnabledTenants = parentProfilesEnabledTenants;
        this.orgUserDevProductivityProfileMappingDatabaseService = orgUserDevProductivityProfileMappingDatabaseService;
        this.orgUsersDatabaseService = orgUsersDatabaseService;
    }

    public CompletableFuture<DevProductivityResponse> calculateDevProductivitySingleUser(Boolean disableCache, final String company, final DevProductivityProfile devProductivityProfile, final DevProductivityFilter devProductivityFilter,
                                                                                         final Map<String, Long> latestIngestedAtByIntegrationId, final TenantSCMSettings tenantSCMSettings, final OrgUserDetails orgUserDetails) {
        return CompletableFuture.supplyAsync(() -> {
            List<DevProductivityProfile.Feature> enabledFeatureList = devProductivityProfile.getSections().stream()
                    .filter(section -> section.getEnabled()).flatMap(section -> section.getFeatures().stream())
                    .filter(feature -> feature.getEnabled()).collect(Collectors.toList());

            List<Object> data = List.of(company, devProductivityProfile.getId(), enabledFeatureList, devProductivityFilter, orgUserDetails, latestIngestedAtByIntegrationId, tenantSCMSettings);
            String hash = null;
            try {
                hash = Hashing.sha256().hashBytes(mapper.writeValueAsBytes(data)).toString();
            } catch (JsonProcessingException e) {
                log.error("JsonProcessingException !", e);
                return null;
            }
            log.debug("hash = {}", hash);
            try {
                return AggCacheUtils.cacheOrCallGeneric(disableCache, company, "/dev_productivity/user_reports_", hash, List.of(), mapper, cacheService, DevProductivityResponse.class, DEV_PRODUCTIVITY_REPORT_CACHE_VALUE, DEV_PRODUCTIVITY_REPORT_CACHE_TIME_UNIT,
                        () -> devProductivityEngine.calculateDevProductivity(company, devProductivityProfile, devProductivityFilter, orgUserDetails, latestIngestedAtByIntegrationId, tenantSCMSettings, null)
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, userDevProductivityReportTaskExecutor);
    }

    private DevProductivityUserIds getUserIdsWithAccess(final String requestorEmail, final Map<String, List<String>> scopes, final String company, final DevProductivityUserIds devProductivityUserIds) {
        if (MapUtils.emptyIfNull(scopes).containsKey("dev_productivity_write") || !auth.isLegacy()) {
            return devProductivityUserIds;
        }
        try {
            DBOrgAccessUsers dbOrgAccessUsers = orgAccessValidationService.getAllAccessUsers(company, requestorEmail, null, devProductivityUserIds.getUserIdType(), devProductivityUserIds.getUserIdList());
            return DevProductivityUserIds.builder().userIdType(IdType.OU_USER_IDS).userIdList(CollectionUtils.emptyIfNull(dbOrgAccessUsers.getAuthorizedUserList()).stream().collect(Collectors.toList())).build();
        } catch (SQLException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public DbListResponse<DevProductivityResponse> generateReportForUsers(Boolean disableCache, final String requestorEmail, final Map<String, List<String>> scopes, final String company, final DefaultListRequest filter) throws SQLException, NotFoundException, ForbiddenException, BadRequestException {
        DevProductivityFilter devProductivityFilter = DevProductivityFilter.fromListRequest(filter);
        log.debug("devProductivityFilter = {}", devProductivityFilter);
        List<DevProductivityProfile> devProductivityProfiles = DevProductivityReportUtils.getDevProductivityProfile(devProductivityProfileService, company, filter, null);
        DevProductivityProfile devProductivityProfile = devProductivityProfiles.isEmpty() ? null : devProductivityProfiles.get(0);
        if(devProductivityProfile == null){
            throw new NotFoundException("User is not associated with any trellis profile");
        }
        log.debug("devProductivityProfile = {}", devProductivityProfile);
        DevProductivityUserIds devProductivityUserIds = DevProductivityUserIds.fromListRequest(filter);
        log.debug("devProductivityUserIds = {}", devProductivityUserIds);
        final Map<String, Long> latestIngestedAtByIntegrationId = jiraFilterParser.getIngestedAt(company, List.of(IntegrationType.JIRA, IntegrationType.AZURE_DEVOPS), DefaultListRequest.builder().build()).getLatestIngestedAtByIntegrationId();
        log.debug("latestIngestedAtByIntegrationId = {}", latestIngestedAtByIntegrationId);
        final TenantSCMSettings tenantSCMSettings = TenantSCMSettingsUtils.getTenantSCMSettings(tenantConfigService, mapper, company);
        log.debug("tenantSCMSettings = {}", tenantSCMSettings);

        DevProductivityUserIds effectiveDevProductivityUserIds = getUserIdsWithAccess(requestorEmail, scopes, company, devProductivityUserIds);
        log.debug("effectiveDevProductivityUserIds = {}", effectiveDevProductivityUserIds);

        if (CollectionUtils.isEmpty(effectiveDevProductivityUserIds.getUserIdList())) {
            log.info("effectiveDevProductivityUserIds is empty, returning empty response!");
            throw new ForbiddenException("User " + requestorEmail + " does not have authority to see dev-productivity score for all the users in the request!");
        }

        List<OrgUserDetails> orgUserDetailsList = orgIdentityService.getUserIdentityForAllIntegrations(company, effectiveDevProductivityUserIds, 0, 1000).getRecords();
        log.debug("orgUserDetailsList = {}", orgUserDetailsList);

        List<CompletableFuture<DevProductivityResponse>> futures = CollectionUtils.emptyIfNull(orgUserDetailsList).stream()
                .map(o -> calculateDevProductivitySingleUser(disableCache, company, devProductivityProfile, devProductivityFilter, latestIngestedAtByIntegrationId, tenantSCMSettings, o))
                .collect(Collectors.toList());
        List<DevProductivityResponse> responses = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        log.debug("responses = {}", responses);
        return DbListResponse.of(responses, responses.size());
    }

    public DbListResponse<FeatureBreakDown> getFeatureDetails(final String company, final DefaultListRequest filter, String forceSource) throws SQLException, NotFoundException, BadRequestException {

        DevProductivityFilter devProductivityFilter = DevProductivityFilter.fromListRequest(filter).toBuilder()
                .forceSource(forceSource)
                .build();
        log.debug("devProductivityFilter for feature details = {}", devProductivityFilter);
        DevProductivityProfile devProductivityProfile = DevProductivityReportUtils.getDevProductivityProfile(devProductivityProfileService, company, filter,null).get(0);
        log.debug("devProductivityProfile for feature details = {}", devProductivityProfile);
        DevProductivityUserIds devProductivityUserIds = DevProductivityUserIds.fromListRequest(filter);
        log.debug("devProductivityUserIds for feature details = {}", devProductivityUserIds);
        final Map<String, Long> latestIngestedAtByIntegrationId = jiraFilterParser.getIngestedAt(company, List.of(IntegrationType.JIRA, IntegrationType.AZURE_DEVOPS), DefaultListRequest.builder().build()).getLatestIngestedAtByIntegrationId();
        log.debug("latestIngestedAtByIntegrationId for feature details = {}", latestIngestedAtByIntegrationId);
        final TenantSCMSettings tenantSCMSettings = TenantSCMSettingsUtils.getTenantSCMSettings(tenantConfigService, mapper, company);
        log.debug("tenantSCMSettings for feature details = {}", tenantSCMSettings);
        List<OrgUserDetails> orgUserDetailsList = orgIdentityService.getUserIdentityForAllIntegrations(company, devProductivityUserIds, 0, 1000).getRecords();
        log.debug("orgUserDetailsList for feature details = {}", orgUserDetailsList);
        String featureName = (String) filter.getFilter().get("feature_name");
        log.debug("Feature Name is {}", featureName);

        String intervalStr = (String) filter.getFilter().get("interval");
        ReportIntervalType interval = StringUtils.isNotEmpty(intervalStr) ? ReportIntervalType.fromString(intervalStr) : ReportIntervalType.LAST_MONTH;

        DevProductivityProfile.Feature feature = devProductivityProfile.getSections().stream()
                .flatMap(section -> section.getFeatures().stream())
                .filter(feat -> feat.getName().equalsIgnoreCase(featureName)
                        || feat.getFeatureType().getDisplayTextForTimeInterval(interval).equalsIgnoreCase(featureName))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dev Productivity profile does not have feature with the name " + featureName));

        FeatureBreakDown featureBreakDown = devProductivityEngine.getFeatureBreakDown(company, feature, devProductivityProfile, devProductivityFilter, orgUserDetailsList.get(0), latestIngestedAtByIntegrationId,
                tenantSCMSettings, devProductivityProfile.getEffortInvestmentProfileId(),
                SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), Collections.emptyList())),
                filter.getPage(),
                filter.getPageSize());

        return DbListResponse.of(List.of(featureBreakDown), featureBreakDown.getCount().intValue());
    }

    public DbListResponse<ScmActivities> getSCMActivity(String company, String requestorEmail, Map<String, List<String>> scopes, DefaultListRequest filter, final boolean valueOnly) throws Exception {
        ScmActivityFilter scmActivityFilter = ScmActivityFilter.fromListRequest(filter);
        log.debug("scmActivityFilter = {}", scmActivityFilter);
        DevProductivityProfile devProductivityProfile = DevProductivityReportUtils.getDevProductivityProfile(devProductivityProfileService, company, filter, null).get(0);
        log.debug("devProductivityProfile = {}", devProductivityProfile);

        DevProductivityUserIds devProductivityUserIds = DevProductivityUserIds.builder().userIdType(scmActivityFilter.getUserIdType()).userIdList(List.of(scmActivityFilter.getUserId())).build();
        log.debug("devProductivityUserIds = {}", devProductivityUserIds);
        final Map<String, Long> latestIngestedAtByIntegrationId = jiraFilterParser.getIngestedAt(company, List.of(IntegrationType.JIRA), DefaultListRequest.builder().build()).getLatestIngestedAtByIntegrationId();
        log.debug("latestIngestedAtByIntegrationId = {}", latestIngestedAtByIntegrationId);
        final TenantSCMSettings tenantSCMSettings = TenantSCMSettingsUtils.getTenantSCMSettings(tenantConfigService, mapper, company);
        log.debug("tenantSCMSettings = {}", tenantSCMSettings);

        DevProductivityUserIds effectiveDevProductivityUserIds = getUserIdsWithAccess(requestorEmail, scopes, company, devProductivityUserIds);
        log.debug("effectiveDevProductivityUserIds = {}", effectiveDevProductivityUserIds);

        if (CollectionUtils.isEmpty(effectiveDevProductivityUserIds.getUserIdList())) {
            log.info("effectiveDevProductivityUserIds is empty, returning empty response!");
            throw new ForbiddenException("User " + requestorEmail + " does not have authority to see dev-productivity score for all the users in the request!");
        }

        List<OrgUserDetails> orgUserDetailsList = orgIdentityService.getUserIdentityForAllIntegrations(company, effectiveDevProductivityUserIds, 0, 1000).getRecords();
        log.debug("orgUserDetailsList = {}", orgUserDetailsList);
        if (orgUserDetailsList == null || CollectionUtils.isEmpty(orgUserDetailsList)) {
            log.info("orgUserDetailsList is empty, returning empty response!");
            throw new NotFoundException("orgUserDetailsList can not be null or empty");
        }
        OrgUserDetails orgUserDetails = orgUserDetailsList.get(0);
        List<IntegrationUserDetails> integrationUserDetails = CollectionUtils.emptyIfNull(orgUserDetails.getIntegrationUserDetailsList()).stream()
                .filter(i -> ScmQueryUtils.isScmIntegration(i.getIntegrationType()))
                .collect(Collectors.toList());

        log.debug("integrationUserDetails = {}", integrationUserDetails);

        List<ScmActivities> scmActivitiesList = (CollectionUtils.isEmpty(integrationUserDetails)) ? Collections.EMPTY_LIST
                : scmActivitiesEngine.calculateScmActivities(company,
                integrationUserDetails.stream().map(d -> d.getIntegrationUserId()).collect(Collectors.toList()),
                integrationUserDetails.stream().map(d -> d.getIntegrationId()).collect(Collectors.toList()),
                scmActivityFilter.getAcross(), scmActivityFilter.getTimeRange(), null, valueOnly);

        log.debug("scmActivitiesList = {}", DefaultObjectMapper.get().writeValueAsString(scmActivitiesList));

        ScmActivities mergedScmActivities = ScmActivities.mergeScmActivities(scmActivitiesList);
        log.debug("mergedScmActivities = {}", mergedScmActivities);
        mergedScmActivities = mergedScmActivities.toBuilder()
                .orgUserId(orgUserDetails.getOrgUserId())
                .fullName(orgUserDetails.getFullName())
                .email(orgUserDetails.getEmail())
                .build();

        DbListResponse<ScmActivities> dbListResponse = DbListResponse.of(List.of(mergedScmActivities), 1);
        return dbListResponse;
    }

    public DbListResponse<RelativeScore> getRelativeScore(String company, String requestorEmail, Map<String, List<String>> scopes, DefaultListRequest filter, Boolean useES) throws SQLException, BadRequestException, ForbiddenException, NotFoundException {

        Map<UUID,List<UUID>> ouOrOrgUserIdDevProdProfilesMap = Maps.newHashMap();
        final UUID devProductivityProfileId = getRequestParamValueOrNull(filter.getFilter(), "dev_productivity_profile_id");
        DevProductivityReportRequest reportRequest = DevProductivityReportRequest.fromListRequest(filter);
        AGG_INTERVAL intervalType = AGG_INTERVAL.fromString(filter.getFilterValue("agg_interval", String.class).get());
        List<UUID> ouOrIntegrationUserIds = reportRequest.getDevProductivityUserIds().stream().filter(d -> d.getIdType() == IdType.OU_USER_IDS || d.getIdType() == IdType.INTEGRATION_USER_IDS)
                .map(DevProductivityUserIds::getId)
                .collect(Collectors.toList());
        List<Integer> ouRefIds = CollectionUtils.emptyIfNull(DefaultListRequestUtils.getListOrDefault(filter,"ou_ref_ids")).stream().filter(Objects::nonNull).map(Integer::valueOf).collect(Collectors.toList());
        if(CollectionUtils.isNotEmpty(ouOrIntegrationUserIds)){
            if(devProductivityProfileId != null) {
                ouOrIntegrationUserIds.forEach(ouOrIntegrationUserId -> ouOrOrgUserIdDevProdProfilesMap.put(ouOrIntegrationUserId, List.of(devProductivityProfileId)));
            } else if(BooleanUtils.isTrue(devProdProfilesV2Enabled) || CollectionUtils.emptyIfNull(parentProfilesEnabledTenants).contains(company)){
                List<DevProductivityParentProfile> devProductivityParentProfiles = DevProductivityReportUtils.getDevProductivityParentProfile(devProductivityParentProfileService, company, filter, ouRefIds);
                if(CollectionUtils.isEmpty(devProductivityParentProfiles)){
                    throw new NotFoundException("OU is not associated with any trellis profile");
                }
                log.info("User Report company {} using v3", company);
                List<UUID> orgUserIds = convertDevProdUserIdsToOrgUserIds(company,reportRequest.getDevProductivityUserIds());
                Map<Integer, UUID> orgUserRefIdOrgUserIdMap = orgUsersDatabaseService.filter(company, QueryFilter.builder().strictMatch("org_user_id",orgUserIds).build(),0,1000).getRecords().stream().collect(Collectors.toMap(DBOrgUser::getRefId, DBOrgUser::getId));
                List<OrgUserDevProdProfileMappings> mappings = orgUserDevProductivityProfileMappingDatabaseService.listByFilter(company,0,1000,null,orgUserRefIdOrgUserIdMap.keySet().stream().collect(Collectors.toList()),devProductivityParentProfiles.stream().map(DevProductivityParentProfile::getId).collect(Collectors.toList()), null).getRecords();
                ouOrOrgUserIdDevProdProfilesMap.putAll(mappings.stream().collect(Collectors.groupingBy(m -> orgUserRefIdOrgUserIdMap.get(m.getOrgUserRefId()),Collectors.mapping(OrgUserDevProdProfileMappings::getDevProductivityProfileId, Collectors.toList()))));
            }else {
                List<DevProductivityProfile> devProductivityProfiles = DevProductivityReportUtils.getDevProductivityProfile(devProductivityProfileService, company, filter, ouRefIds);
                if(CollectionUtils.isEmpty(devProductivityProfiles)){
                    throw new BadRequestException("No trellis profile is associated with OU");
                }else{
                    ouOrIntegrationUserIds.forEach(ouOrIntegrationUserId -> ouOrOrgUserIdDevProdProfilesMap.put(ouOrIntegrationUserId, devProductivityProfiles.stream().map(DevProductivityProfile::getId).collect(Collectors.toList())));
                }
            }
        }
        if(containsOrgIds(reportRequest)){
            List<UUID> ouIds = reportRequest.getDevProductivityUserIds().stream().filter(d -> d.getIdType() == IdType.ORG_IDS)
                            .map(DevProductivityUserIds::getOrgIds)
                            .flatMap(List::stream)
                            .collect(Collectors.toList());
            UUID associatedProfileId = null;
            for(UUID ouId : ouIds){
                Optional<OUConfiguration> ouConfigOpt = orgUnitHelper.getOuConfiguration(company,ouId);
                Integer ouRefId = ouConfigOpt.isPresent() ? ouConfigOpt.get().getOuRefId() : null;
                if(BooleanUtils.isTrue(devProdProfilesV2Enabled) || CollectionUtils.emptyIfNull(parentProfilesEnabledTenants).contains(company)){
                    List<DevProductivityParentProfile> devProductivityParentProfiles = DevProductivityReportUtils.getDevProductivityParentProfile(devProductivityParentProfileService, company, filter, ouRefIds);
                    if(CollectionUtils.isEmpty(devProductivityParentProfiles)){
                        log.warn("OU " + ouId +" is not associated with any trellis profile");
                        continue;
                    }
                    ouOrOrgUserIdDevProdProfilesMap.put(ouId,List.of(devProductivityParentProfiles.get(0).getId()));
                } else{
                    List<DevProductivityProfile> devProductivityProfiles = DevProductivityReportUtils.getDevProductivityProfile(devProductivityProfileService, company, filter, List.of(ouRefId));
                    DevProductivityProfile devProductivityProfile = devProductivityProfiles.isEmpty() ? null : devProductivityProfiles.get(0);
                    log.debug("devProductivityProfile = {}", devProductivityProfile);
                    associatedProfileId = devProductivityProfile != null ? devProductivityProfile.getId() : null;
                    if(associatedProfileId == null){
                        log.warn("OU " + ouId +" is not associated with any trellis profile");
                        continue;
                    }
                    ouOrOrgUserIdDevProdProfilesMap.put(ouId,List.of(associatedProfileId));
                }

            }
        }
        if (!isAuthorizedToViewAllUserScores(company, requestorEmail, scopes, reportRequest)) {
            log.info("User {} does not have authority to see dev-productivity score for all the users in the request!", requestorEmail);
            throw new ForbiddenException("User " + requestorEmail + " does not have authority to see dev-productivity score for all the users and/or org in the request!");
        }
        Boolean noComparison = filter.getFilterValue("no_comparison",Boolean.class).orElse(false);
        if (readDevProductivityV2Enabled || readDevProductivityV2UserReportTenants.contains(company)) {
            log.info("Relative Score Report company {} using v2", company);
            return devProductivityRelativeScoreService.listByFilterV2(company, ouOrOrgUserIdDevProdProfilesMap, intervalType, reportRequest.getDevProductivityUserIds(), noComparison, useES, filter.getPage(), filter.getPageSize());
        } else {
            return devProductivityRelativeScoreService.listByFilter(company, ouOrOrgUserIdDevProdProfilesMap, intervalType, reportRequest.getDevProductivityUserIds(), noComparison, useES, filter.getPage(), filter.getPageSize());
        }
    }

    private boolean containsOrgIds(DevProductivityReportRequest reportRequest) {
        if(CollectionUtils.isNotEmpty(reportRequest.getDevProductivityUserIds())){
            for(DevProductivityUserIds userId : reportRequest.getDevProductivityUserIds()){
                if(userId.getIdType().equals(IdType.ORG_IDS))
                    return true;
            }
        }
        return false;
    }

    private boolean isAuthorizedToViewAllUserScores(String company, String requestorEmail, Map<String, List<String>> scopes, DevProductivityReportRequest reportRequest) throws SQLException {

        if (MapUtils.emptyIfNull(scopes).containsKey("dev_productivity_write") || !auth.isLegacy()) {
            return true;
        }

        List<UUID> allOrgAndIntegrationUsers = reportRequest.getDevProductivityUserIds().stream().filter( user -> user.getIdType().equals(IdType.OU_USER_IDS)
                || user.getIdType().equals(IdType.INTEGRATION_USER_IDS)).map(DevProductivityUserIds::getId).collect(Collectors.toList());
        List<UUID> orgUserIdList = reportRequest.getDevProductivityUserIds().stream().filter( user -> user.getIdType().equals(IdType.OU_USER_IDS))
                .map(DevProductivityUserIds::getId).collect(Collectors.toList());
        List<UUID> integrationUserIdList = reportRequest.getDevProductivityUserIds().stream().filter( user -> user.getIdType().equals(IdType.INTEGRATION_USER_IDS))
                .map(DevProductivityUserIds::getId).collect(Collectors.toList());
        List<UUID> ouIdList1 = reportRequest.getDevProductivityUserIds().stream().filter( user -> user.getIdType().equals(IdType.ORG_IDS))
                .map(DevProductivityUserIds::getId).filter(Objects::nonNull).collect(Collectors.toList());
        List<UUID> ouIdList2 = reportRequest.getDevProductivityUserIds().stream().filter( user -> user.getIdType().equals(IdType.ORG_IDS))
                .filter(x -> CollectionUtils.isNotEmpty(x.getOrgIds())).map(DevProductivityUserIds::getOrgIds).flatMap(List::stream).collect(Collectors.toList());
        List<UUID> ouIdList = new ArrayList<>();
        ouIdList.addAll(ouIdList1);
        ouIdList.addAll(ouIdList2);
        ouIdList = ouIdList.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());

        int authorizedCount = 0;
        if (CollectionUtils.isNotEmpty(orgUserIdList)) {
            DevProductivityUserIds orgUserIds = DevProductivityUserIds.builder()
                    .userIdType(IdType.OU_USER_IDS)
                    .userIdList(orgUserIdList)
                    .build();
            DevProductivityUserIds effectiveDevProductivityUserIds = getUserIdsWithAccess(requestorEmail, scopes, company, orgUserIds);
            authorizedCount += effectiveDevProductivityUserIds.getUserIdList().size();
        }

        if (CollectionUtils.isNotEmpty(integrationUserIdList)) {
            DevProductivityUserIds integrationUserIds = DevProductivityUserIds.builder()
                    .userIdType(IdType.INTEGRATION_USER_IDS)
                    .userIdList(integrationUserIdList)
                    .build();
            DevProductivityUserIds effectiveDevProductivityUserIds = getUserIdsWithAccess(requestorEmail, scopes, company, integrationUserIds);
            authorizedCount += effectiveDevProductivityUserIds.getUserIdList().size();
        }

        List<DBOrgUnit> managerAccessibleOU = orgAccessValidationService.getOrgsManagedUsingManagersEmail(company, requestorEmail, 0, 100);

        List<UUID> authuorizedOUList = managerAccessibleOU.stream().map(orgUnit -> orgUnit.getId()).collect(Collectors.toList());

        return authorizedCount == allOrgAndIntegrationUsers.size() && authuorizedOUList.containsAll(ouIdList);
    }

    private UUID getRequestParamValueOrNull(Map<String, Object> filter, String requestParam) {
        return filter.get(requestParam) == null ? null : UUID.fromString((String) filter.get(requestParam));
    }

    private List<UUID> convertDevProdUserIdsToOrgUserIds(final String company, List<DevProductivityUserIds> effectiveDevProdUserIds) {
        List<UUID> orgUserIds = new ArrayList<>();
        effectiveDevProdUserIds.forEach(d -> {
            if(d.getIdType() == IdType.OU_USER_IDS) {
                orgUserIds.add(d.getId());
            } else{
                List<OrgUserDetails> orgUserDetailsList = orgIdentityService.getUserIdentityForAllIntegrations(company, d, 0, 1000).getRecords();
                orgUserIds.addAll(orgUserDetailsList.stream().map(OrgUserDetails::getOrgUserId).collect(Collectors.toList()));
            }
        });
        return orgUserIds;
    }
}
