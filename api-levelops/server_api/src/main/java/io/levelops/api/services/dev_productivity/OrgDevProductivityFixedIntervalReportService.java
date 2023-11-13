package io.levelops.api.services.dev_productivity;

import com.google.api.client.util.Lists;
import com.google.common.collect.Maps;
import io.levelops.api.model.dev_productivity.DevProductivityFixedIntervalFilter;
import io.levelops.api.model.dev_productivity.EffectiveOUs;
import io.levelops.api.model.dev_productivity.FeatureRawStat;
import io.levelops.api.model.dev_productivity.OrgDevProductivityRawStatsReport;
import io.levelops.api.services.DevProductivityParentProfileService;
import io.levelops.api.services.DevProductivityProfileService;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityParentProfile;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityResponse;
import io.levelops.commons.databases.models.database.dev_productivity.OrgDevProductivityReport;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.dev_productivity.OrgDevProductivityESReportDatabaseService;
import io.levelops.commons.databases.services.dev_productivity.OrgDevProductivityReportDatabaseService;
import io.levelops.commons.databases.services.dev_productivity.OrgDevProductivityReportV2DatabaseService;
import io.levelops.commons.databases.services.dev_productivity.OrgDevProductivityReportV3DatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.ForbiddenException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
public class OrgDevProductivityFixedIntervalReportService {
    private static final Boolean USE_ACTIVE_OUS = Boolean.TRUE;
    private final DevProductivityProfileService devProductivityProfileService;
    private final DevProductivityParentProfileService devProductivityParentProfileService;
    private final AggCacheService cacheService;
    private final DevProductivityRBACService devProductivityRBACService;
    private final OrgDevProductivityReportDatabaseService orgDevProductivityReportDatabaseService;
    private final OrgDevProductivityReportV2DatabaseService orgDevProductivityReportV2DatabaseService;
    private final OrgDevProductivityReportV3DatabaseService orgDevProductivityReportV3DatabaseService;
    private final OrgUnitHelper orgUnitHelper;
    private final OrgUnitsDatabaseService orgUnitsDatabaseService;
    private final OrgDevProductivityESReportDatabaseService orgDevProductivityESReportDatabaseService;
    private final Set<String> readDevProductivityV2OrgReportTenants;
    private final boolean readDevProductivityV2Enabled;
    private final Boolean devProdProfilesV2Enabled;
    private final Set<String> parentProfilesEnabledTenants;

    @Autowired
    public OrgDevProductivityFixedIntervalReportService(DevProductivityProfileService devProductivityProfileService, DevProductivityParentProfileService devProductivityParentProfileService, AggCacheService cacheService, DevProductivityRBACService devProductivityRBACService, OrgDevProductivityReportDatabaseService orgDevProductivityReportDatabaseService, OrgDevProductivityReportV2DatabaseService orgDevProductivityReportV2DatabaseService, OrgDevProductivityReportV3DatabaseService orgDevProductivityReportV3DatabaseService, OrgUnitHelper orgUnitHelper, OrgUnitsDatabaseService orgUnitsDatabaseService,
                                                        OrgDevProductivityESReportDatabaseService orgDevProductivityESReportDatabaseService,
                                                        @Value("${DEV_PROD_READ_V2_ORG_REPORT_TENANTS:}") String readDevProductivityV2ReportTenantsString,
                                                        @Qualifier("readDevProductivityV2Enabled") boolean readDevProductivityV2Enabled, @Value("${DEV_PROD_PROFILES_V2_ENABLED:false}") Boolean devProdProfilesV2Enabled, @Qualifier("parentProfilesEnabledTenants") Set<String> parentProfilesEnabledTenants) {
        this.devProductivityProfileService = devProductivityProfileService;
        this.devProductivityParentProfileService = devProductivityParentProfileService;
        this.cacheService = cacheService;
        this.devProductivityRBACService = devProductivityRBACService;
        this.orgDevProductivityReportDatabaseService = orgDevProductivityReportDatabaseService;
        this.orgDevProductivityReportV2DatabaseService = orgDevProductivityReportV2DatabaseService;
        this.orgDevProductivityReportV3DatabaseService = orgDevProductivityReportV3DatabaseService;
        this.orgUnitHelper = orgUnitHelper;
        this.orgUnitsDatabaseService = orgUnitsDatabaseService;
        this.orgDevProductivityESReportDatabaseService = orgDevProductivityESReportDatabaseService;
        this.readDevProductivityV2OrgReportTenants = CommaListSplitter.splitToStream(readDevProductivityV2ReportTenantsString)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        this.readDevProductivityV2Enabled = readDevProductivityV2Enabled;
        this.devProdProfilesV2Enabled = devProdProfilesV2Enabled;
        this.parentProfilesEnabledTenants = parentProfilesEnabledTenants;
    }

    public DbListResponse<DevProductivityResponse> generateReportForOrgs(Boolean disableCache, final String requestorEmail, final Map<String, List<String>> scopes, final String company, final DefaultListRequest filter, Boolean useEs) throws SQLException, NotFoundException, ForbiddenException, BadRequestException {

        DbListResponse<OrgDevProductivityReport> dbListResponse = getOrgDevProductivityReports(company, requestorEmail, scopes, filter, false, useEs);
        List<DevProductivityResponse> responses = CollectionUtils.emptyIfNull(dbListResponse.getRecords()).stream().map(r -> r.getReport()).collect(Collectors.toList());
        log.debug("responses = {}", responses);
        log.info("responses.size() = {}", responses.size());
        return DbListResponse.of(responses, dbListResponse.getTotalCount());
    }

    public List<OrgDevProductivityRawStatsReport> generateRawStatsForOrgs(Boolean disableCache, String requestorEmail, Map<String, List<String>> scopes, String company, DefaultListRequest filter, Boolean useEs) throws SQLException, NotFoundException, BadRequestException, ForbiddenException {

        List<OrgDevProductivityRawStatsReport> responses = Lists.newArrayList();
        DbListResponse<OrgDevProductivityReport> dbListResponse = getOrgDevProductivityReports(company, requestorEmail, scopes, filter, true, useEs);
        responses.addAll(dbListResponse.getRecords().stream().map(r -> buildOrgDevProductivityRawStatsResponse(r)).collect(Collectors.toList()));
        log.debug("responses = {}", responses);
        log.info("responses.size() = {}", responses.size());
        return responses;
    }

    private DbListResponse<OrgDevProductivityReport> getOrgDevProductivityReports(String company, String requestorEmail, Map<String, List<String>> scopes, DefaultListRequest filter, boolean needRawReport, boolean useEs) throws ForbiddenException, SQLException, NotFoundException, BadRequestException {

        DevProductivityFixedIntervalFilter devProductivityFilter = DevProductivityFixedIntervalFilter.fromListRequest(filter);
        log.info("devProductivityFilter = {}", devProductivityFilter);

        EffectiveOUs effectiveOrgUnits = devProductivityRBACService.getEffectiveOUsWithAccess(company, scopes, requestorEmail, devProductivityFilter);
        log.info("effectiveOrgUnits = {}", effectiveOrgUnits);

        try {
            List<DevProductivityParentProfile> devProductivityParentProfiles = null;
            List<DevProductivityProfile> devProductivityProfiles = null;
            List<Integer> ouRefIds = Lists.newArrayList();
            ouRefIds.addAll(CollectionUtils.emptyIfNull(effectiveOrgUnits.getOrgRefIds()));
            List<OUConfiguration> ouConfigs = orgUnitHelper.getOuConfigurationList(company, null, effectiveOrgUnits.getOuIds());
            if (CollectionUtils.isNotEmpty(ouConfigs)) {
                ouRefIds.addAll(ouConfigs.stream().map(OUConfiguration::getOuRefId).collect(Collectors.toSet()));
            }
            List<Integer> parentRefIds = ouRefIds;
            if (filter.getFilter().get("is_immediate_child_ou") != null && filter.getFilter().get("is_immediate_child_ou").equals(true)) {
                //PROP-1871 : to show only immediate children along with root.
                ouRefIds = orgUnitsDatabaseService.getFirstLevelChildrenRefIds(company, ouRefIds).stream().collect(Collectors.toList());
                ouRefIds.addAll(parentRefIds);
            } else {
                //PROP-524 : when org report is requested, fetch the report of all children
                ouRefIds = orgUnitsDatabaseService.getAllChildrenRefIdsRecursive(company, ouRefIds).stream().collect(Collectors.toList());
            }
            if(BooleanUtils.isTrue(devProdProfilesV2Enabled) || CollectionUtils.emptyIfNull(parentProfilesEnabledTenants).contains(company)){
                devProductivityParentProfiles = DevProductivityReportUtils.getDevProductivityParentProfile(devProductivityParentProfileService, company, filter, ouRefIds);
                if (CollectionUtils.isEmpty(devProductivityParentProfiles)) {
                    throw new NotFoundException("OU is not associated with any trellis profile");
                }
            }else{
                devProductivityProfiles = DevProductivityReportUtils.getDevProductivityProfile(devProductivityProfileService, company, filter, ouRefIds);
                if (CollectionUtils.isEmpty(devProductivityProfiles)) {
                    throw new NotFoundException("OU is not associated with any trellis profile");
                }
            }

            DbListResponse<OrgDevProductivityReport> dbResponses;
            if(BooleanUtils.isTrue(devProdProfilesV2Enabled) || CollectionUtils.emptyIfNull(parentProfilesEnabledTenants).contains(company)){
                log.info("Org Report company {} using v3", company);
                dbResponses = orgDevProductivityReportV3DatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), null, null, devProductivityParentProfiles.stream().map(DevProductivityParentProfile::getId).collect(Collectors.toList()), List.of(devProductivityFilter.getReportInterval()), ouRefIds, devProductivityFilter.getSort(), needRawReport);
            } else if(readDevProductivityV2Enabled || readDevProductivityV2OrgReportTenants.contains(company)) {
                log.info("Org Report company {} using v2", company);
                dbResponses = orgDevProductivityReportV2DatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), null, null, devProductivityProfiles.stream().map(DevProductivityProfile::getId).collect(Collectors.toList()), List.of(devProductivityFilter.getReportInterval()), ouRefIds, devProductivityFilter.getSort(), needRawReport);
            } else {
                if(useEs){
                    dbResponses = orgDevProductivityESReportDatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), null, null, devProductivityProfiles.stream().map(DevProductivityProfile::getId).collect(Collectors.toList()), List.of(devProductivityFilter.getReportInterval()), ouRefIds, USE_ACTIVE_OUS, devProductivityFilter.getSort(), needRawReport);
                }else {
                    dbResponses = orgDevProductivityReportDatabaseService.listByFilter(company, filter.getPage(), filter.getPageSize(), null, null, devProductivityProfiles.stream().map(DevProductivityProfile::getId).collect(Collectors.toList()), List.of(devProductivityFilter.getReportInterval()), ouRefIds, USE_ACTIVE_OUS, devProductivityFilter.getSort(), needRawReport);
                }
            }

            List<OrgDevProductivityReport> parentList = dbResponses.getRecords().stream().filter(report -> parentRefIds.stream().anyMatch(s -> s.intValue() == report.getOuRefId().intValue())).collect(Collectors.toList());
            List<OrgDevProductivityReport> childList = dbResponses.getRecords().stream().filter(report -> parentRefIds.stream().anyMatch(s -> s.intValue() != report.getOuRefId().intValue())).collect(Collectors.toList());
            List<OrgDevProductivityReport> finalList = Lists.newArrayList();
            finalList.addAll(parentList);
            finalList.addAll(childList);

            return DbListResponse.of(finalList, dbResponses.getTotalCount());
        } catch (Exception e) {
            log.error("Exception occurred: ", e);
            throw (e);
        }
    }

    private OrgDevProductivityRawStatsReport buildOrgDevProductivityRawStatsResponse(OrgDevProductivityReport r) {
        return OrgDevProductivityRawStatsReport.builder()
                .ouId(r.getOuID())
                .name(r.getReport().getOrgName())
                .rawStats(buildRawStatsFromOrgdevProdReport(r.getReport()))
                .interval(r.getInterval()).startTime(r.getReport().getStartTime()).endTime(r.getReport().getEndTime())
                .missingUserReportsCount(r.getReport().getMissingUserReportsCount()).staleUserReportsCount(r.getReport().getStaleUserReportsCount())
                .resultTime(r.getReport().getResultTime())
                .build();
    }

    private List<FeatureRawStat> buildRawStatsFromOrgdevProdReport(DevProductivityResponse r) {
        List<FeatureRawStat> rawStats = new ArrayList<>();
        r.getSectionResponses().forEach(sr ->
                sr.getFeatureResponses().forEach(fr -> rawStats.add(FeatureRawStat.builder().name(fr.getName()).count(fr.getCount()).rating(fr.getRating()).build())));
        return rawStats;
    }
}
