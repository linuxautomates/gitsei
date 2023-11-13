package io.levelops.api.services.dev_productivity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import io.levelops.api.services.DevProductivityProfileService;
import io.levelops.auth.auth.config.Auth;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.TenantSCMSettings;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityResponse;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityUserIds;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureResponse;
import io.levelops.commons.databases.models.database.dev_productivity.IdType;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.databases.models.database.dev_productivity.SectionResponse;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.databases.services.dev_productivity.OrgIdentityService;
import io.levelops.commons.databases.services.dev_productivity.engine.DevProductivityEngine;
import io.levelops.commons.databases.services.dev_productivity.utils.TenantSCMSettingsUtils;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.databases.services.organization.OrgAccessValidationService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import static io.levelops.api.converters.DefaultListRequestUtils.getListOrDefault;
import static io.levelops.api.services.dev_productivity.DevProductivityReportUtils.DEV_PRODUCTIVITY_REPORT_CACHE_TIME_UNIT;
import static io.levelops.api.services.dev_productivity.DevProductivityReportUtils.DEV_PRODUCTIVITY_REPORT_CACHE_VALUE;

@Log4j2
@Service
public class OrgDevProductivityReportService {
    private final ObjectMapper mapper;
    private final DevProductivityEngine devProductivityEngine;
    private final DevProductivityProfileService devProductivityProfileService;
    private final OrgIdentityService orgIdentityService;
    private final JiraFilterParser jiraFilterParser;
    private final TenantConfigService tenantConfigService;
    private final OrgUnitsDatabaseService orgUnitsDatabaseService;
    private final AggCacheService cacheService;
    private final UserDevProductivityReportService userDevProductivityReportService;
    private final OrgAccessValidationService orgAccessValidationService;
    private final Executor orgDevProductivityReportTaskExecutor;
    private final Auth auth;

    @Autowired
    public OrgDevProductivityReportService(ObjectMapper mapper, DevProductivityEngine devProductivityEngine, DevProductivityProfileService devProductivityProfileService, OrgIdentityService orgIdentityService, JiraFilterParser jiraFilterParser, TenantConfigService tenantConfigService, OrgUnitsDatabaseService orgUnitsDatabaseService, AggCacheService cacheService, UserDevProductivityReportService userDevProductivityReportService, OrgAccessValidationService orgAccessValidationService,
                                           @Qualifier("orgDevProductivityReportTaskExecutor") Executor orgDevProductivityReportTaskExecutor, Auth auth) {
        this.mapper = mapper;
        this.devProductivityEngine = devProductivityEngine;
        this.devProductivityProfileService = devProductivityProfileService;
        this.orgIdentityService = orgIdentityService;
        this.jiraFilterParser = jiraFilterParser;
        this.tenantConfigService = tenantConfigService;
        this.orgUnitsDatabaseService = orgUnitsDatabaseService;
        this.cacheService = cacheService;
        this.userDevProductivityReportService = userDevProductivityReportService;
        this.orgAccessValidationService = orgAccessValidationService;
        this.orgDevProductivityReportTaskExecutor = orgDevProductivityReportTaskExecutor;
        this.auth = auth;
    }

    private boolean validateRequestorsAccessToOrg(final String requestorEmail, final Map<String, List<String>> scopes, final String company, final UUID orgId) {
        if (MapUtils.emptyIfNull(scopes).containsKey("dev_productivity_write") || !auth.isLegacy()) {
            log.info("requestorEmail {} has required role, granting access!", requestorEmail);
            return true;
        }
        try {
            boolean validAceess = orgAccessValidationService.validateAccess(company, requestorEmail, null, orgId);
            log.info("requestorEmail {} membership access is {}", requestorEmail, validAceess);
            return validAceess;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private CompletableFuture<DevProductivityResponse> generateReportForOrg(final Boolean disableCache, final String requestorEmail, final Map<String, List<String>> scopes, final String company, final DevProductivityProfile devProductivityProfile, final DevProductivityFilter devProductivityFilter,
                                                                            final Map<String, Long> latestIngestedAtByIntegrationId, final TenantSCMSettings tenantSCMSettings, final UUID orgId) {
        return CompletableFuture.supplyAsync(() -> {
            boolean validAccess = validateRequestorsAccessToOrg(requestorEmail, scopes, company, orgId);
            if (!validAccess) {
                return DevProductivityResponse.builder().orgId(orgId).build();
            }

            List<Object> data = List.of(company, devProductivityProfile.getId(), devProductivityFilter, latestIngestedAtByIntegrationId, tenantSCMSettings, orgId);
            String hash = null;
            try {
                hash = Hashing.sha256().hashBytes(mapper.writeValueAsBytes(data)).toString();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            log.info("hash = {}", hash);
            try {
                return AggCacheUtils.cacheOrCallGeneric(disableCache, company, "/dev_productivity/org_reports_", hash, List.of(), mapper, cacheService, DevProductivityResponse.class, DEV_PRODUCTIVITY_REPORT_CACHE_VALUE, DEV_PRODUCTIVITY_REPORT_CACHE_TIME_UNIT,
                        () -> {
                            List<UUID> orgUserIds = orgAccessValidationService.getAllAccessUsersByOuId(company, null, List.of(orgId)).getAuthorizedUserList().stream().collect(Collectors.toList());
                            log.info("orgUserIds = {}", orgUserIds);

                            DevProductivityUserIds devProductivityUserIds = DevProductivityUserIds.builder().userIdType(IdType.OU_USER_IDS).userIdList(orgUserIds).build();
                            log.info("devProductivityUserIds = {}", devProductivityUserIds);

                            List<OrgUserDetails> orgUserDetailsList = orgIdentityService.getUserIdentityForAllIntegrations(company, devProductivityUserIds, 0, 1000).getRecords();
                            log.info("orgUserDetailsList = {}", orgUserDetailsList);

                            List<CompletableFuture<DevProductivityResponse>> futures = CollectionUtils.emptyIfNull(orgUserDetailsList).stream()
                                    .map(o -> userDevProductivityReportService.calculateDevProductivitySingleUser(disableCache, company, devProductivityProfile, devProductivityFilter, latestIngestedAtByIntegrationId, tenantSCMSettings, o))
                                    .collect(Collectors.toList());
                            List<DevProductivityResponse> orgUserResponses = futures.stream()
                                    .map(CompletableFuture::join)
                                    .collect(Collectors.toList());
                            log.info("orgUserResponses = {}", orgUserResponses);
                            Map<Integer, Map<Integer, List<FeatureResponse>>> resultsMap = new HashMap<>();
                            for (DevProductivityResponse orgUserResponse : orgUserResponses) {
                                if (CollectionUtils.isEmpty(orgUserResponse.getSectionResponses())) {
                                    continue;
                                }
                                for (SectionResponse sc : orgUserResponse.getSectionResponses()) {
                                    if (CollectionUtils.isEmpty(sc.getFeatureResponses())) {
                                        continue;
                                    }
                                    for (FeatureResponse fr : sc.getFeatureResponses()) {
                                        resultsMap.putIfAbsent(sc.getOrder(), new HashMap<>());
                                        resultsMap.get(sc.getOrder()).putIfAbsent(fr.getOrder(), new ArrayList<>());
                                        resultsMap.get(sc.getOrder()).get(fr.getOrder()).add(fr);
                                    }
                                }
                            }
                            DevProductivityResponse devProductivityResponse = DevProductivityEngine.ResponseHelper.buildResponseFromPartialFeatureResponses(devProductivityProfile, resultsMap, null);
                            return devProductivityResponse;
                        }
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, orgDevProductivityReportTaskExecutor);

    }

    public DbListResponse<DevProductivityResponse> generateReportForOrgs(Boolean disableCache, final String requestorEmail, final Map<String, List<String>> scopes, final String company, final DefaultListRequest filter) throws SQLException, NotFoundException, BadRequestException {
        DevProductivityFilter devProductivityFilter = DevProductivityFilter.fromListRequest(filter);
        log.info("devProductivityFilter = {}", devProductivityFilter);
        DevProductivityProfile devProductivityProfile = DevProductivityReportUtils.getDevProductivityProfile(devProductivityProfileService, company, filter, null).get(0);
        log.info("devProductivityProfile = {}", devProductivityProfile);

        final Map<String, Long> latestIngestedAtByIntegrationId = jiraFilterParser.getIngestedAt(company, List.of(IntegrationType.JIRA, IntegrationType.AZURE_DEVOPS), DefaultListRequest.builder().build()).getLatestIngestedAtByIntegrationId();
        log.info("latestIngestedAtByIntegrationId = {}", latestIngestedAtByIntegrationId);
        final TenantSCMSettings tenantSCMSettings = TenantSCMSettingsUtils.getTenantSCMSettings(tenantConfigService, mapper, company);
        log.info("tenantSCMSettings = {}", tenantSCMSettings);

        List<UUID> orgIds = CollectionUtils.emptyIfNull(getListOrDefault(filter.getFilter(), "ou_ids")).stream().map(UUID::fromString).collect(Collectors.toList());
        log.info("orgIds = {}", orgIds);

        List<CompletableFuture<DevProductivityResponse>> futures = CollectionUtils.emptyIfNull(orgIds).stream()
                .map(o -> generateReportForOrg(disableCache, requestorEmail, scopes, company, devProductivityProfile, devProductivityFilter, latestIngestedAtByIntegrationId, tenantSCMSettings, o))
                .collect(Collectors.toList());
        List<DevProductivityResponse> responses = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        log.info("responses = {}", responses);
        return DbListResponse.of(responses, responses.size());
    }


    public DbListResponse<DevProductivityResponse> generateReportForOrgUsers(Boolean disableCache, final String requestorEmail, final Map<String, List<String>> scopes, final String company, final String orgUUID, final DefaultListRequest filter) throws SQLException, NotFoundException, JsonProcessingException, BadRequestException {
        UUID orgId = UUID.fromString(orgUUID);
        boolean validAccess = validateRequestorsAccessToOrg(requestorEmail, scopes, company, orgId);
        if (!validAccess) {
            return DbListResponse.of(List.of(), 0);
        }

        DevProductivityFilter devProductivityFilter = DevProductivityFilter.fromListRequest(filter);
        log.info("devProductivityFilter = {}", devProductivityFilter);
        DevProductivityProfile devProductivityProfile = DevProductivityReportUtils.getDevProductivityProfile(devProductivityProfileService, company, filter, null).get(0);
        log.info("devProductivityProfile = {}", devProductivityProfile);

        final Map<String, Long> latestIngestedAtByIntegrationId = jiraFilterParser.getIngestedAt(company, List.of(IntegrationType.JIRA, IntegrationType.AZURE_DEVOPS), DefaultListRequest.builder().build()).getLatestIngestedAtByIntegrationId();
        log.info("latestIngestedAtByIntegrationId = {}", latestIngestedAtByIntegrationId);
        final TenantSCMSettings tenantSCMSettings = TenantSCMSettingsUtils.getTenantSCMSettings(tenantConfigService, mapper, company);
        log.info("tenantSCMSettings = {}", tenantSCMSettings);

        List<UUID> orgUserIds = orgAccessValidationService.getAllAccessUsersByOuId(company, null, List.of(UUID.fromString(orgUUID))).getAuthorizedUserList().stream().collect(Collectors.toList());
        log.info("orgUserIds = {}", orgUserIds);

        DevProductivityUserIds devProductivityUserIds = DevProductivityUserIds.builder().userIdType(IdType.OU_USER_IDS).userIdList(orgUserIds).build();
        log.info("devProductivityUserIds = {}", devProductivityUserIds);

        List<OrgUserDetails> orgUserDetailsList = orgIdentityService.getUserIdentityForAllIntegrations(company, devProductivityUserIds, 0, 1000).getRecords();
        log.info("orgUserDetailsList = {}", orgUserDetailsList);

        List<CompletableFuture<DevProductivityResponse>> futures = CollectionUtils.emptyIfNull(orgUserDetailsList).stream()
                .map(o -> userDevProductivityReportService.calculateDevProductivitySingleUser(disableCache, company, devProductivityProfile, devProductivityFilter, latestIngestedAtByIntegrationId, tenantSCMSettings, o))
                .collect(Collectors.toList());
        List<DevProductivityResponse> orgUserResponses = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        log.info("orgUserResponses = {}", orgUserResponses);
        return DbListResponse.of(orgUserResponses, orgUserResponses.size());
    }
}
