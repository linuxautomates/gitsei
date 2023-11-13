package io.levelops.api.services.dev_productivity;

import com.google.api.client.util.Lists;
import com.google.common.collect.Maps;
import io.levelops.api.model.dev_productivity.DevProductivityFixedIntervalFilter;
import io.levelops.api.model.dev_productivity.EffectiveOUs;
import io.levelops.api.model.dev_productivity.FeatureRawStat;
import io.levelops.api.model.dev_productivity.OrgUsersDevProductivityRawStatsReport;
import io.levelops.api.model.dev_productivity.UserDevProductivityRawStatsReport;
import io.levelops.api.services.DevProductivityParentProfileService;
import io.levelops.api.services.DevProductivityProfileService;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityParentProfile;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityResponse;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityUserIds;
import io.levelops.commons.databases.models.database.dev_productivity.IdType;
import io.levelops.commons.databases.models.database.dev_productivity.OrgAndUsersDevProductivityReportMappings;
import io.levelops.commons.databases.models.database.dev_productivity.OrgDevProductivityReport;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.databases.models.database.dev_productivity.UserDevProductivityReport;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.dev_productivity.OrgAndUsersDevProductivityReportMappingsDBService;
import io.levelops.commons.databases.services.dev_productivity.OrgAndUsersDevProductivityReportMappingsV2DBService;
import io.levelops.commons.databases.services.dev_productivity.OrgAndUsersDevProductivityReportMappingsV3DBService;
import io.levelops.commons.databases.services.dev_productivity.OrgDevProductivityReportV2DatabaseService;
import io.levelops.commons.databases.services.dev_productivity.OrgDevProductivityReportV3DatabaseService;
import io.levelops.commons.databases.services.dev_productivity.OrgIdentityService;
import io.levelops.commons.databases.services.dev_productivity.OrgUserDevProductivityProfileMappingDatabaseService;
import io.levelops.commons.databases.services.dev_productivity.UserDevProductivityESReportDatabaseService;
import io.levelops.commons.databases.services.dev_productivity.UserDevProductivityReportDatabaseService;
import io.levelops.commons.databases.services.dev_productivity.UserDevProductivityReportV2DatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.utils.MapUtils;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.ForbiddenException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class UserDevProductivityFixedIntervalReportService {
    private final DevProductivityProfileService devProductivityProfileService;
    private final DevProductivityParentProfileService devProductivityParentProfileService;
    private final AggCacheService cacheService;
    private final DevProductivityRBACService devProductivityRBACService;
    private final OrgIdentityService orgIdentityService;
    private final UserDevProductivityReportDatabaseService userDevProductivityReportDatabaseService;
    private final OrgAndUsersDevProductivityReportMappingsDBService orgAndUsersDevProductivityReportMappingsDBService;
    private final UserDevProductivityReportV2DatabaseService userDevProductivityReportV2DatabaseService;
    private final OrgUserDevProductivityProfileMappingDatabaseService orgUserDevProductivityProfileMappingDatabaseService;

    private final OrgAndUsersDevProductivityReportMappingsV2DBService orgAndUsersDevProductivityReportMappingsV2DBService;
    private final OrgAndUsersDevProductivityReportMappingsV3DBService orgAndUsersDevProductivityReportMappingsV3DBService;
    private final OrgDevProductivityReportV2DatabaseService orgDevProductivityReportV2DatabaseService;
    private final OrgDevProductivityReportV3DatabaseService orgDevProductivityReportV3DatabaseService;
    private final OrgUnitHelper orgUnitHelper;
    private final OrgUsersDatabaseService orgUsersDatabaseService;

    private final UserDevProductivityESReportDatabaseService userDevProductivityESReportDatabaseService;
    private final Set<String> readDevProductivityV2UserReportTenants;
    private final boolean readDevProductivityV2Enabled;
    private final Boolean devProdProfilesV2Enabled;
    private final Set<String> parentProfilesEnabledTenants;


    @Autowired
    public UserDevProductivityFixedIntervalReportService(DevProductivityProfileService devProductivityProfileService, DevProductivityParentProfileService devProductivityParentProfileService, AggCacheService cacheService, DevProductivityRBACService devProductivityRBACService, OrgIdentityService orgIdentityService, UserDevProductivityReportDatabaseService userDevProductivityReportDatabaseService, OrgAndUsersDevProductivityReportMappingsDBService orgAndUsersDevProductivityReportMappingsDBService, UserDevProductivityReportV2DatabaseService userDevProductivityReportV2DatabaseService, OrgUserDevProductivityProfileMappingDatabaseService orgUserDevProductivityProfileMappingDatabaseService, OrgAndUsersDevProductivityReportMappingsV2DBService orgAndUsersDevProductivityReportMappingsV2DBService, OrgAndUsersDevProductivityReportMappingsV3DBService orgAndUsersDevProductivityReportMappingsV3DBService, OrgDevProductivityReportV2DatabaseService orgDevProductivityReportV2DatabaseService, OrgDevProductivityReportV3DatabaseService orgDevProductivityReportV3DatabaseService, OrgUnitHelper orgUnitHelper, OrgUsersDatabaseService orgUsersDatabaseService,
                                                         UserDevProductivityESReportDatabaseService userDevProductivityESReportDatabaseService,
                                                         @Qualifier("readDevProductivityV2UserReportTenants") Set<String> readDevProductivityV2UserReportTenants,
                                                         @Qualifier("readDevProductivityV2Enabled") boolean readDevProductivityV2Enabled, @Value("${DEV_PROD_PROFILES_V2_ENABLED:false}") Boolean devProdProfilesV2Enabled, @Qualifier("parentProfilesEnabledTenants") Set<String> parentProfilesEnabledTenants) {
        this.devProductivityProfileService = devProductivityProfileService;
        this.devProductivityParentProfileService = devProductivityParentProfileService;
        this.cacheService = cacheService;
        this.devProductivityRBACService = devProductivityRBACService;
        this.orgIdentityService = orgIdentityService;
        this.userDevProductivityReportDatabaseService = userDevProductivityReportDatabaseService;
        this.orgAndUsersDevProductivityReportMappingsDBService = orgAndUsersDevProductivityReportMappingsDBService;
        this.userDevProductivityReportV2DatabaseService = userDevProductivityReportV2DatabaseService;
        this.orgUserDevProductivityProfileMappingDatabaseService = orgUserDevProductivityProfileMappingDatabaseService;
        this.orgAndUsersDevProductivityReportMappingsV2DBService = orgAndUsersDevProductivityReportMappingsV2DBService;
        this.orgAndUsersDevProductivityReportMappingsV3DBService = orgAndUsersDevProductivityReportMappingsV3DBService;
        this.orgDevProductivityReportV2DatabaseService = orgDevProductivityReportV2DatabaseService;
        this.orgDevProductivityReportV3DatabaseService = orgDevProductivityReportV3DatabaseService;
        this.orgUnitHelper = orgUnitHelper;
        this.orgUsersDatabaseService = orgUsersDatabaseService;
        this.userDevProductivityESReportDatabaseService = userDevProductivityESReportDatabaseService;
        this.readDevProductivityV2UserReportTenants = readDevProductivityV2UserReportTenants;
        this.readDevProductivityV2Enabled = readDevProductivityV2Enabled;
        this.devProdProfilesV2Enabled = devProdProfilesV2Enabled;
        this.parentProfilesEnabledTenants = parentProfilesEnabledTenants;
    }

    private void validateRequestForMultipleUsers(DevProductivityFixedIntervalFilter devProductivityFilter) throws BadRequestException {
        if(CollectionUtils.isEmpty(devProductivityFilter.getOuIds()) && CollectionUtils.isEmpty(devProductivityFilter.getOuRefIds())) {
            throw new BadRequestException("Atleast one ou_id needs to be provided! ou_ids is null or empty");
        }
    }

    private List<UUID> getOUIdsFromLatestOrgReports(final String company, List<UUID> ouIds, List<UUID> devProductivityProfileIds, List<ReportIntervalType> intervals, List<Integer> ouRefIds) throws SQLException {
        List<OrgDevProductivityReport> orgReports = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 10000, null, ouIds, devProductivityProfileIds, intervals, ouRefIds, null, false).getRecords();
        List<UUID> ouIdsFromLatestReports = CollectionUtils.emptyIfNull(orgReports).stream().map(r -> r.getOuID()).distinct().collect(Collectors.toList());
        return ouIdsFromLatestReports;
    }

    private List<UUID> getOUIdsFromLatestOrgReportsV3(final String company, List<UUID> ouIds, List<UUID> devProductivityParentProfileIds, List<ReportIntervalType> intervals, List<Integer> ouRefIds) throws SQLException {
        List<OrgDevProductivityReport> orgReports = orgDevProductivityReportV3DatabaseService.listByFilter(company, 0, 10000, null, ouIds, devProductivityParentProfileIds, intervals, ouRefIds, null, false).getRecords();
        List<UUID> ouIdsFromLatestReports = CollectionUtils.emptyIfNull(orgReports).stream().map(r -> r.getOuID()).distinct().collect(Collectors.toList());
        return ouIdsFromLatestReports;
    }


    public DbListResponse<DevProductivityResponse> generateReportForMultipleUsers(Boolean disableCache, final String requestorEmail, final Map<String, List<String>> scopes, final String company, final DefaultListRequest filter, Boolean useEs) throws SQLException, NotFoundException, BadRequestException, ForbiddenException {
        DevProductivityFixedIntervalFilter devProductivityFilter = DevProductivityFixedIntervalFilter.fromListRequest(filter);
        log.info("devProductivityFilter = {}", devProductivityFilter);
        validateRequestForMultipleUsers(devProductivityFilter);
        List<Integer> ouRefIds = devProductivityFilter.getOuRefIds();
        if(CollectionUtils.isEmpty(ouRefIds) && CollectionUtils.isNotEmpty(devProductivityFilter.getOuIds())){
            Optional<OUConfiguration> ouConfigOpt = orgUnitHelper.getOuConfiguration(company,devProductivityFilter.getOuIds().get(0));
            if(ouConfigOpt.isPresent())
                ouRefIds = List.of(ouConfigOpt.get().getOuRefId());
        }
        DbListResponse<UserDevProductivityReport> dbListResponse = null;
        List<DevProductivityResponse> responses = null;
        List<UUID> latestOUIds = null;
        DevProductivityParentProfile devProductivityParentProfile = null;
        DevProductivityProfile devProductivityProfile = null;
        EffectiveOUs effectiveOrgUnits = devProductivityRBACService.getEffectiveOUsWithAccess(company, scopes, requestorEmail, devProductivityFilter);
        log.info("effectiveOrgUnits = {}", effectiveOrgUnits);
        List<OrgAndUsersDevProductivityReportMappings> mappings = List.of();
        if(BooleanUtils.isTrue(devProdProfilesV2Enabled) || CollectionUtils.emptyIfNull(parentProfilesEnabledTenants).contains(company)){
            List<DevProductivityParentProfile> devProductivityParentProfiles = DevProductivityReportUtils.getDevProductivityParentProfile(devProductivityParentProfileService, company, filter, ouRefIds);
            if(CollectionUtils.isNotEmpty(devProductivityParentProfiles)){
                devProductivityParentProfile = devProductivityParentProfiles.get(0);
            }else{
                throw new NotFoundException("OU is not associated with any trellis profile");
            }
            log.debug("devProductivityParentProfile = {}", devProductivityParentProfile);
            log.info("User Report company {} using v3", company);
            latestOUIds = getOUIdsFromLatestOrgReportsV3(company, effectiveOrgUnits.getOuIds(), List.of(devProductivityParentProfile.getId()), List.of(devProductivityFilter.getReportInterval()), effectiveOrgUnits.getOrgRefIds());
            mappings = orgAndUsersDevProductivityReportMappingsV3DBService.listByFilter(company, 0, 10000, null, latestOUIds, List.of(devProductivityParentProfile.getId()), List.of(devProductivityFilter.getReportInterval()), null).getRecords();

        } else {
            List<DevProductivityProfile> devProductivityProfiles = DevProductivityReportUtils.getDevProductivityProfile(devProductivityProfileService, company, filter, ouRefIds);
            if(CollectionUtils.isNotEmpty(devProductivityProfiles)){
                devProductivityProfile = devProductivityProfiles.get(0);
            }else{
                throw new NotFoundException("OU is not associated with any trellis profile");
            }
            log.debug("devProductivityProfile = {}", devProductivityProfile);

            if(readDevProductivityV2Enabled || readDevProductivityV2UserReportTenants.contains(company)) {
                log.info("User Report company {} using v2", company);
                latestOUIds = getOUIdsFromLatestOrgReports(company, effectiveOrgUnits.getOuIds(), List.of(devProductivityProfile.getId()), List.of(devProductivityFilter.getReportInterval()), effectiveOrgUnits.getOrgRefIds());
                mappings = orgAndUsersDevProductivityReportMappingsV2DBService.listByFilter(company, 0, 10000, null, latestOUIds, List.of(devProductivityProfile.getId()), List.of(devProductivityFilter.getReportInterval()), null).getRecords();
            } else {
                mappings = orgAndUsersDevProductivityReportMappingsDBService.listByFilter(company, 0, 10000, null, effectiveOrgUnits.getOuIds(), List.of(devProductivityProfile.getId()), List.of(devProductivityFilter.getReportInterval()), effectiveOrgUnits.getOrgRefIds()).getRecords();
            }
        }
        List<UUID> orgUserIds = mappings.stream().map(m -> m.getOrgUserIds()).flatMap(Collection::stream).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(orgUserIds)) {
            log.info("mappings is empty, returning empty response!");
            return DbListResponse.of(List.of(), 0);
        }
        if(BooleanUtils.isTrue(devProdProfilesV2Enabled) || CollectionUtils.emptyIfNull(parentProfilesEnabledTenants).contains(company)){
            log.info("User Report company {} using parent profiles", company);
            List<UUID> devProductivityProfileIds = devProductivityParentProfile.getSubProfiles().stream().map(DevProductivityProfile::getId).collect(Collectors.toList());
            dbListResponse = userDevProductivityReportV2DatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), null, orgUserIds, devProductivityProfileIds, List.of(devProductivityFilter.getReportInterval()), devProductivityFilter.getSort(), false, null);
        }else if(readDevProductivityV2Enabled || readDevProductivityV2UserReportTenants.contains(company)) {
            log.info("User Report company {} using v2", company);
            dbListResponse = userDevProductivityReportV2DatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), null, orgUserIds, List.of(devProductivityProfile.getId()), List.of(devProductivityFilter.getReportInterval()), devProductivityFilter.getSort(), false, null);
        } else {
            if(useEs){
                dbListResponse = userDevProductivityESReportDatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), null, orgUserIds, List.of(devProductivityProfile.getId()), List.of(devProductivityFilter.getReportInterval()), devProductivityFilter.getSort(), false);
            }else{
                dbListResponse = userDevProductivityReportDatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), null, orgUserIds, List.of(devProductivityProfile.getId()), List.of(devProductivityFilter.getReportInterval()), devProductivityFilter.getSort(), false);
            }
        }
        responses = CollectionUtils.emptyIfNull(dbListResponse.getRecords()).stream().map(r -> r.getReport()).collect(Collectors.toList());
        responses = responses.stream().map(r -> {
            if(MapUtils.isEmpty(r.getCustomFields())){
                r = r.toBuilder().customFields(getOuAttributesFromOrgUserId(company,r.getOrgUserId())).build();
            }
            return r;
        }).collect(Collectors.toList());
        log.debug("responses = {}", responses);
        log.info("responses.size() = {}", responses.size());
        return DbListResponse.of(responses, dbListResponse.getTotalCount());
    }

    public List<OrgUsersDevProductivityRawStatsReport> generateRawStatsForUsers(Boolean disableCache, final String requestorEmail, final Map<String, List<String>> scopes, final String company, final DefaultListRequest filter, Boolean useEs) throws SQLException, NotFoundException, BadRequestException, ForbiddenException {
        DevProductivityFixedIntervalFilter devProductivityFilter = DevProductivityFixedIntervalFilter.fromListRequest(filter);
        log.info("devProductivityFilter = {}", devProductivityFilter);
        validateRequestForMultipleUsers(devProductivityFilter);

        EffectiveOUs effectiveOrgUnits = devProductivityRBACService.getEffectiveOUsWithAccess(company, scopes, requestorEmail, devProductivityFilter);
        log.info("effectiveOrgUnits = {}", effectiveOrgUnits);

        List<OrgUsersDevProductivityRawStatsReport> responses = Lists.newArrayList();
        List<DevProductivityParentProfile> devProductivityParentProfiles = null;
        List<DevProductivityProfile> devProductivityProfiles = null;
        List<OrgAndUsersDevProductivityReportMappings> mappings = null;
        if(CollectionUtils.isNotEmpty(effectiveOrgUnits.getOrgRefIds())){
            for(Integer ouRefId : effectiveOrgUnits.getOrgRefIds()){
                if(BooleanUtils.isTrue(devProdProfilesV2Enabled) || CollectionUtils.emptyIfNull(parentProfilesEnabledTenants).contains(company)){
                    devProductivityParentProfiles = DevProductivityReportUtils.getDevProductivityParentProfile(devProductivityParentProfileService, company, filter, List.of(ouRefId));
                    if(CollectionUtils.isEmpty(devProductivityParentProfiles)){
                        log.warn("Company {} OU {} is not associated with any trellis profile ",company,ouRefId);
                        continue;
                    }
                    log.info("OU User mapping company {} using v3", company);
                    List<UUID> latestOUIds = getOUIdsFromLatestOrgReportsV3(company, null, devProductivityParentProfiles.stream().map(DevProductivityParentProfile::getId).collect(Collectors.toList()), List.of(devProductivityFilter.getReportInterval()), List.of(ouRefId));
                    mappings = orgAndUsersDevProductivityReportMappingsV3DBService.listByFilter(company, 0, 10000, null, latestOUIds, devProductivityParentProfiles.stream().map(DevProductivityParentProfile::getId).collect(Collectors.toList()), List.of(devProductivityFilter.getReportInterval()), null).getRecords();
                } else {
                    devProductivityProfiles = DevProductivityReportUtils.getDevProductivityProfile(devProductivityProfileService, company, filter, List.of(ouRefId));
                    if(CollectionUtils.isEmpty(devProductivityProfiles)){
                        log.warn("Company {} OU {} is not associated with any trellis profile ",company,ouRefId);
                        continue;
                    }
                    if(readDevProductivityV2Enabled || readDevProductivityV2UserReportTenants.contains(company)) {
                        log.info("User Report company {} using v2", company);
                        List<UUID> latestOUIds = getOUIdsFromLatestOrgReports(company, null, devProductivityProfiles.stream().map(DevProductivityProfile::getId).collect(Collectors.toList()), List.of(devProductivityFilter.getReportInterval()), List.of(ouRefId));
                        mappings = orgAndUsersDevProductivityReportMappingsV2DBService.listByFilter(company, 0, 10000, null, latestOUIds, devProductivityProfiles.stream().map(DevProductivityProfile::getId).collect(Collectors.toList()), List.of(devProductivityFilter.getReportInterval()), null).getRecords();
                    } else {
                        mappings = orgAndUsersDevProductivityReportMappingsDBService.listByFilter(company, 0, 10000, null, null, devProductivityProfiles.stream().map(DevProductivityProfile::getId).collect(Collectors.toList()), List.of(devProductivityFilter.getReportInterval()), List.of(ouRefId)).getRecords();
                    }
                }

                List<UUID> orgUserIds = mappings.stream().map(m -> m.getOrgUserIds()).flatMap(Collection::stream).collect(Collectors.toList());
                if(CollectionUtils.isEmpty(orgUserIds)) {
                    log.info("mappings is empty, returning empty response!");
                    return responses;
                }
                DbListResponse<UserDevProductivityReport> devProdResponse;
                if(BooleanUtils.isTrue(devProdProfilesV2Enabled) || CollectionUtils.emptyIfNull(parentProfilesEnabledTenants).contains(company)){
                    log.info("User Report company {} using parent profiles", company);
                    List<UUID> devProductivityProfileIds = devProductivityParentProfiles.get(0).getSubProfiles().stream().map(DevProductivityProfile::getId).collect(Collectors.toList());
                    devProdResponse = userDevProductivityReportV2DatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), null, orgUserIds, devProductivityProfileIds, List.of(devProductivityFilter.getReportInterval()), devProductivityFilter.getSort(), false, null);
                }else if(readDevProductivityV2Enabled || readDevProductivityV2UserReportTenants.contains(company)) {
                    log.info("User Report company {} using v2", company);
                    devProdResponse = userDevProductivityReportV2DatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), null, orgUserIds, devProductivityProfiles.stream().map(DevProductivityProfile::getId).collect(Collectors.toList()), List.of(devProductivityFilter.getReportInterval()), devProductivityFilter.getSort(), true, null);
                } else {
                    if(useEs){
                        devProdResponse = userDevProductivityESReportDatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), null, orgUserIds, devProductivityProfiles.stream().map(DevProductivityProfile::getId).collect(Collectors.toList()), List.of(devProductivityFilter.getReportInterval()), devProductivityFilter.getSort(), true);
                    }else {
                        devProdResponse = userDevProductivityReportDatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), null, orgUserIds, devProductivityProfiles.stream().map(DevProductivityProfile::getId).collect(Collectors.toList()), List.of(devProductivityFilter.getReportInterval()), devProductivityFilter.getSort(), true);
                    }
                }
                responses.add(buildDevProdRawStatsResponse(company,null,ouRefId,devProductivityFilter.getReportInterval(),devProdResponse));
            }
        }else if(CollectionUtils.isNotEmpty(effectiveOrgUnits.getOuIds())){
            for(UUID ouId : effectiveOrgUnits.getOuIds()){
                List<Integer> ouRefIds = null;
                Optional<OUConfiguration> ouConfigOpt = orgUnitHelper.getOuConfiguration(company,ouId);
                if(ouConfigOpt.isPresent())
                    ouRefIds = List.of(ouConfigOpt.get().getOuRefId());
                devProductivityProfiles = DevProductivityReportUtils.getDevProductivityProfile(devProductivityProfileService, company, filter, ouRefIds);
                if(CollectionUtils.isEmpty(devProductivityProfiles)){
                    log.warn("Company {} OU {} is not associated with any trellis profile ",company,ouId);
                    continue;
                }
                if(readDevProductivityV2Enabled || readDevProductivityV2UserReportTenants.contains(company)) {
                    log.info("User Report company {} using v2", company);
                    List<UUID> latestOUIds = getOUIdsFromLatestOrgReports(company, List.of(ouId), devProductivityProfiles.stream().map(DevProductivityProfile::getId).collect(Collectors.toList()), List.of(devProductivityFilter.getReportInterval()), null);
                    mappings = orgAndUsersDevProductivityReportMappingsV2DBService.listByFilter(company, 0, 10000, null, latestOUIds, devProductivityProfiles.stream().map(DevProductivityProfile::getId).collect(Collectors.toList()), List.of(devProductivityFilter.getReportInterval()), null).getRecords();
                } else {
                    mappings = orgAndUsersDevProductivityReportMappingsDBService.listByFilter(company, 0, 10000, null, List.of(ouId), devProductivityProfiles.stream().map(DevProductivityProfile::getId).collect(Collectors.toList()), List.of(devProductivityFilter.getReportInterval()), null).getRecords();
                }
                List<UUID> orgUserIds = mappings.stream().map(m -> m.getOrgUserIds()).flatMap(Collection::stream).collect(Collectors.toList());
                if(CollectionUtils.isEmpty(orgUserIds)) {
                    log.info("mappings is empty, returning empty response!");
                    return responses;
                }
                DbListResponse<UserDevProductivityReport> devProdResponse;
                if(readDevProductivityV2Enabled || readDevProductivityV2UserReportTenants.contains(company)) {
                    log.info("User Report company {} using v2", company);
                    devProdResponse = userDevProductivityReportV2DatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), null, orgUserIds, devProductivityProfiles.stream().map(DevProductivityProfile::getId).collect(Collectors.toList()), List.of(devProductivityFilter.getReportInterval()), devProductivityFilter.getSort(), true, null);
                } else {
                    if(useEs){
                        devProdResponse = userDevProductivityESReportDatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), null, orgUserIds, devProductivityProfiles.stream().map(DevProductivityProfile::getId).collect(Collectors.toList()), List.of(devProductivityFilter.getReportInterval()), devProductivityFilter.getSort(), true);
                    }else {
                        devProdResponse = userDevProductivityReportDatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), null, orgUserIds, devProductivityProfiles.stream().map(DevProductivityProfile::getId).collect(Collectors.toList()), List.of(devProductivityFilter.getReportInterval()), devProductivityFilter.getSort(), true);
                    }
                }
                responses.add(buildDevProdRawStatsResponse(company,ouId,null,devProductivityFilter.getReportInterval(),devProdResponse));
            }
        }
        else
            throw new BadRequestException("The request does not contain any ouids or ouRefIds and the requestor doesn't have access to any OUs");
        return responses;
    }

    private OrgUsersDevProductivityRawStatsReport buildDevProdRawStatsResponse(String company, UUID ouId, Integer ouRefId, ReportIntervalType reportInterval, DbListResponse<UserDevProductivityReport> devProdResponse) {
        return OrgUsersDevProductivityRawStatsReport.builder()
                .ouId(ouId)
                .interval(reportInterval)
                .records(buildUserDevProdRawStatsFromUserDevProdReport(company,devProdResponse.getRecords()))
                .build();
    }

    private List<UserDevProductivityRawStatsReport> buildUserDevProdRawStatsFromUserDevProdReport(String company, List<UserDevProductivityReport> records) {
        return records.stream().map(r -> UserDevProductivityRawStatsReport.builder()
                .orgUserId(r.getOrgUserId())
                .fullName(r.getReport().getFullName())
                .ouAttributes(getOuAttributesFromOrgUserId(company,r.getOrgUserId()))
                .rawStats(buildRawStatsFromDevProdReport(r.getReport()))
                .interval(r.getReport().getInterval()).startTime(r.getReport().getStartTime()).endTime(r.getReport().getEndTime())
                .incomplete(r.getReport().getIncomplete()).missingFeatures(r.getReport().getMissingFeatures())
                .resultTime(r.getReport().getResultTime())
                .build()).collect(Collectors.toList());
    }

    private Map<String, Object> getOuAttributesFromOrgUserId(String company, UUID orgUserId) {
        try {
            return orgUsersDatabaseService.get(company,orgUserId).get().getCustomFields();
        } catch (SQLException e) {
            log.error("Error while fetching attributes for company = {}, orgUserId = {}",company,orgUserId,e);
        }
        return null;
    }

    private List<FeatureRawStat> buildRawStatsFromDevProdReport(DevProductivityResponse report) {
        List<FeatureRawStat> rawStats = new ArrayList<>();
        report.getSectionResponses().forEach(sr -> sr.getFeatureResponses().forEach(fr -> rawStats.add(FeatureRawStat.builder().name(fr.getName()).count(fr.getCount()).rating(fr.getRating()).build())));
        return rawStats;
    }

    private DevProductivityUserIds validateRequestForSingleUser(final String userIdTypeString, final String userId) throws BadRequestException {
        if(StringUtils.isBlank(userIdTypeString) || StringUtils.isBlank(userId)) {
            throw new BadRequestException("User Id Type & User Id cannot be blank!");
        }
        IdType userIdType = IdType.fromString(userIdTypeString);
        if(userIdType == null) {
            String error = String.format("User Id Type %s is not supported!", userIdTypeString);
            throw new BadRequestException(error);
        }
        return DevProductivityUserIds.builder().userIdType(userIdType).userIdList(List.of(UUID.fromString(userId))).build();
    }

    private Optional<UUID> convertDevProdUserIdsToOrgUserIds(final String company, DevProductivityUserIds effectiveDevProdUserIds) {
        if(effectiveDevProdUserIds.getUserIdType() == IdType.OU_USER_IDS) {
            return Optional.ofNullable(effectiveDevProdUserIds.getUserIdList().get(0));
        }
        List<OrgUserDetails> orgUserDetailsList = orgIdentityService.getUserIdentityForAllIntegrations(company, effectiveDevProdUserIds, 0, 1000).getRecords();
        log.info("orgUserDetailsList = {}", orgUserDetailsList);
        if(CollectionUtils.isEmpty(orgUserDetailsList)) {
            return Optional.empty();
        }
        return Optional.ofNullable(orgUserDetailsList.get(0).getOrgUserId());
    }

    public DevProductivityResponse generateReportForSingleUser(Boolean disableCache, final String requestorEmail, final Map<String, List<String>> scopes, final String company, final String userIdType, final String userId, final DefaultListRequest filter, Boolean useEs) throws SQLException, NotFoundException, BadRequestException, ForbiddenException {
        DevProductivityFixedIntervalFilter devProductivityFilter = DevProductivityFixedIntervalFilter.fromListRequest(filter);
        log.info("devProductivityFilter = {}", devProductivityFilter);

        DevProductivityUserIds devProductivityUserIds = validateRequestForSingleUser(userIdType, userId);
        log.info("devProductivityUserIds = {}", devProductivityUserIds);

        DevProductivityUserIds effectiveDevProdUserIds = devProductivityRBACService.getEffectiveUserIdsWithAccess(company, scopes, requestorEmail, devProductivityUserIds);

        Optional<UUID> optionalOrgUserId = convertDevProdUserIdsToOrgUserIds(company, effectiveDevProdUserIds);
        if(optionalOrgUserId.isEmpty()) {
            String error = String.format("No User Dev Productivity Report found for user type %s and user id %s!", userIdType, userId);
            throw new NotFoundException(error);
        }

        DevProductivityParentProfile devProductivityParentProfile = null;
        DevProductivityProfile devProductivityProfile = null;
        if(BooleanUtils.isTrue(devProdProfilesV2Enabled) || CollectionUtils.emptyIfNull(parentProfilesEnabledTenants).contains(company)){
            devProductivityParentProfile = DevProductivityReportUtils.getDevProductivityParentProfile(devProductivityParentProfileService, company, filter, devProductivityFilter.getOuRefIds()).get(0);
            log.debug("devProductivityParentProfile = {}", devProductivityParentProfile);
        } else{
            devProductivityProfile = DevProductivityReportUtils.getDevProductivityProfile(devProductivityProfileService, company, filter, devProductivityFilter.getOuRefIds()).stream().findFirst().orElse(null);
            log.debug("devProductivityProfile = {}", devProductivityProfile);
        }

        DbListResponse<UserDevProductivityReport> dbListResponse = null;
        if(BooleanUtils.isTrue(devProdProfilesV2Enabled) || CollectionUtils.emptyIfNull(parentProfilesEnabledTenants).contains(company)){
            log.info("User Report company {} using parent profiles", company);
            dbListResponse = userDevProductivityReportV2DatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), null, List.of(optionalOrgUserId.get()), devProductivityParentProfile.getSubProfiles().stream().map(DevProductivityProfile::getId).collect(Collectors.toList()), List.of(devProductivityFilter.getReportInterval()), devProductivityFilter.getSort(), false, null);
        } else if(readDevProductivityV2Enabled || readDevProductivityV2UserReportTenants.contains(company)) {
            log.info("User Report company {} using v2", company);
            dbListResponse = userDevProductivityReportV2DatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), null, List.of(optionalOrgUserId.get()), List.of(devProductivityProfile.getId()), List.of(devProductivityFilter.getReportInterval()), devProductivityFilter.getSort(), false, null);
        } else {
            if(useEs){
                dbListResponse = userDevProductivityESReportDatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), null, List.of(optionalOrgUserId.get()), List.of(devProductivityProfile.getId()), List.of(devProductivityFilter.getReportInterval()), devProductivityFilter.getSort(), false);
            }else {
                dbListResponse = userDevProductivityReportDatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), null, List.of(optionalOrgUserId.get()), List.of(devProductivityProfile.getId()), List.of(devProductivityFilter.getReportInterval()), devProductivityFilter.getSort(), false);
            }
        }
        List<DevProductivityResponse> responses = CollectionUtils.emptyIfNull(dbListResponse.getRecords()).stream().map(r -> r.getReport()).collect(Collectors.toList());
        log.debug("responses = {}", responses);
        log.info("responses.size() = {}", responses.size());
        if(CollectionUtils.isEmpty(responses)) {
            String error = String.format("No User Dev Productivity Report found for user type %s and user id %s!", userIdType, userId);
            throw new NotFoundException(error);
        }
        return responses.get(0);
    }

}
