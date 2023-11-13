package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.ScmDoraAggService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.services.velocity_productivity.services.VelocityAggsService;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
@RequestMapping("/v1/scm/dora")
public class ScmDoraAggsController {

    private final ScmDoraAggService scmDoraAggService;
    private final OrgUnitHelper orgUnitHelper;
    private final VelocityAggsService velocityAggsService;
    private final ObjectMapper mapper;
    private final AggCacheService aggCacheService;

    @Autowired
    public ScmDoraAggsController(ScmDoraAggService scmDoraAggService,
                                 VelocityAggsService velocityAggsService,
                                 final OrgUnitHelper orgUnitHelper,
                                 ObjectMapper objectMapper,
                                 AggCacheService aggCacheService) {
        this.scmDoraAggService = scmDoraAggService;
        this.orgUnitHelper = orgUnitHelper;
        this.velocityAggsService = velocityAggsService;
        this.mapper = objectMapper;
        this.aggCacheService = aggCacheService;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/deployment_frequency", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getDeploymentFrequencyReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/dora/deployment_frequency' for the request: {}", company, originalRequest, e);
            }
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            final var finalOuConfig = ouConfig;
            VelocityConfigDTO velocityConfigDTO = velocityAggsService.getVelocityConfig(company, originalRequest);
            ScmPrFilter scmPrFilter = ScmPrFilter.fromDefaultListRequest(request, MoreObjects.firstNonNull(ScmPrFilter.DISTINCT.fromString(request.getAcross()), ScmPrFilter.DISTINCT.none), ScmPrFilter.CALCULATION.deployment_frequency);
            ScmCommitFilter scmCommitFilter = ScmCommitFilter.fromDefaultListRequest(originalRequest, null, null, Map.of());
            List<DbAggregationResult> aggregationRecords = AggCacheUtils.cacheOrCall(disableCache, company,
                    "/scm/dora/deployment_frequency",
                    scmPrFilter.generateCacheRawString() + finalOuConfig.hashCode() + velocityConfigDTO.getId(), scmPrFilter.getIntegrationIds(), mapper, aggCacheService,
                    () -> scmDoraAggService.generateDeploymentFrequency(company, scmPrFilter, scmCommitFilter, finalOuConfig, velocityConfigDTO))
                    .getRecords();
            return ResponseEntity.ok(PaginatedResponse.of(page, pageSize, aggregationRecords));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/failure_rate", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getFailureRateReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/dora/failure_rate' for the request: {}", company, originalRequest, e);
            }
            VelocityConfigDTO velocityConfigDTO = velocityAggsService.getVelocityConfig(company, originalRequest);
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            final var finalOuConfig = ouConfig;
            ScmPrFilter scmPrFilter = ScmPrFilter.fromDefaultListRequest(request, MoreObjects.firstNonNull(ScmPrFilter.DISTINCT.fromString(request.getAcross()), ScmPrFilter.DISTINCT.none)
                    , ScmPrFilter.CALCULATION.failure_rate);
            ScmCommitFilter scmCommitFilter = ScmCommitFilter.fromDefaultListRequest(originalRequest, null, null, Map.of());
            List<DbAggregationResult> aggregationRecords = AggCacheUtils.cacheOrCall(disableCache, company,
                    "/scm/dora/failure_rate",
                    scmPrFilter.generateCacheRawString() + finalOuConfig.hashCode() + velocityConfigDTO.getId(), scmPrFilter.getIntegrationIds(), mapper, aggCacheService,
                    () -> scmDoraAggService.generateFailureRateReports(company, scmPrFilter, scmCommitFilter, finalOuConfig, velocityConfigDTO))
                    .getRecords();
            return ResponseEntity.ok(PaginatedResponse.of(page, pageSize, aggregationRecords));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/lead_time", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getLeadTimeReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/dora/lead_time' for the request: {}", company, originalRequest, e);
            }
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            final var finalOuConfig = ouConfig;
            VelocityConfigDTO velocityConfigDTO = velocityAggsService.getVelocityConfig(company, originalRequest);
            ScmPrFilter scmPrFilter = ScmPrFilter.fromDefaultListRequest(request, MoreObjects.firstNonNull(ScmPrFilter.DISTINCT.fromString(request.getAcross()),
                    ScmPrFilter.DISTINCT.none), ScmPrFilter.CALCULATION.lead_time_for_changes);
            List<DbAggregationResult> aggregationRecords = AggCacheUtils.cacheOrCall(disableCache, company,
                    "/scm/dora/lead_time",
                    scmPrFilter.generateCacheRawString() + finalOuConfig.hashCode() + velocityConfigDTO.getId(), scmPrFilter.getIntegrationIds(), mapper, aggCacheService,
                    () -> scmDoraAggService.generateLeadTimeAndMTTRReport(company, scmPrFilter, finalOuConfig, velocityConfigDTO))
                    .getRecords();
            return ResponseEntity.ok(PaginatedResponse.of(page, pageSize, aggregationRecords));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/mean_time_to_recover", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getMeanTimeToRecover(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/scm/dora/mean_time_to_recover' for the request: {}", company, originalRequest, e);
            }
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            final var finalOuConfig = ouConfig;

            VelocityConfigDTO velocityConfigDTO = velocityAggsService.getVelocityConfig(company, originalRequest);
            ScmPrFilter scmPrFilter = ScmPrFilter.fromDefaultListRequest(request, MoreObjects.firstNonNull(ScmPrFilter.DISTINCT.fromString(request.getAcross()), ScmPrFilter.DISTINCT.none), ScmPrFilter.CALCULATION.mean_time_to_recover);
            List<DbAggregationResult> aggregationRecords = AggCacheUtils.cacheOrCall(disableCache, company,
                    "/scm/dora/mean_time_to_recover",
                    scmPrFilter.generateCacheRawString() + finalOuConfig.hashCode() + velocityConfigDTO.getId(),
                    scmPrFilter.getIntegrationIds(), mapper, aggCacheService,
                    () -> scmDoraAggService.generateLeadTimeAndMTTRReport(company, scmPrFilter, finalOuConfig, velocityConfigDTO))
                    .getRecords();
            return ResponseEntity.ok(PaginatedResponse.of(page, pageSize, aggregationRecords));
        });
    }
}
