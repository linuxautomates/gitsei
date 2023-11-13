package io.levelops.api.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.DbHistogramBucket;
import io.levelops.commons.databases.models.response.DbHistogramResult;
import io.levelops.commons.databases.models.response.VelocityRatingResult;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.services.velocity_productivity.services.VelocityAggsService;
import io.levelops.commons.services.velocity_productivity.services.VelocityAggsWidgetService;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/velocity")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
@Log4j2
public class VelocityAggsController {

    private static final boolean USE_INTEGRATION_PREFIX = true;

    private final VelocityAggsService velocityAggsService;
    private final VelocityAggsWidgetService velocityAggsWidgetService;
    private final IntegrationService integrationService;
    private final OrgUnitHelper ouHelper;
    private final ObjectMapper mapper;
    private final AggCacheService cacheService;
    private final Set<String> longerCacheExpiryForVelocityTenants;
    private static final Long LONGER_CACHE_EXPIRY_VALUE = 480L;
    private static final TimeUnit LONGER_CACHE_EXPIRY_UNIT= TimeUnit.MINUTES;

    @Autowired
    public VelocityAggsController(VelocityAggsService velocityAggsService, VelocityAggsWidgetService velocityAggsWidgetService, IntegrationService integrationService, final OrgUnitHelper ouHelper, ObjectMapper mapper, AggCacheService cacheService,
                                  @Value("${velocity.cache.longer_expiry_for_tenants:}") String longerCacheExpiryTenantsString) {
        this.velocityAggsService = velocityAggsService;
        this.velocityAggsWidgetService = velocityAggsWidgetService;
        this.integrationService = integrationService;
        this.ouHelper = ouHelper;
        this.mapper = mapper;
        this.cacheService = cacheService;
        this.longerCacheExpiryForVelocityTenants = CommaListSplitter.splitToStream(longerCacheExpiryTenantsString)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getVelocityAggs(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "there_is_no_precalculate", required = false, defaultValue = "false") Boolean disablePrecalculatedResult,
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            Long cacheTTLValue = getCacheTTLValue(company);
            TimeUnit cacheTTLUnit = getCacheTTLUnit(company);
            log.info("company {}, cacheTTLValue = {}",company,cacheTTLValue);
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            originalRequest.getPage(),
                            originalRequest.getPageSize(),
                            velocityAggsWidgetService.getVelocityAggs(company, originalRequest, disableCache, cacheTTLValue, cacheTTLUnit, disablePrecalculatedResult)
                            ));
                });
    }

    private Long getCacheTTLValue(String company) {
        return longerCacheExpiryForVelocityTenants.contains(company) ? LONGER_CACHE_EXPIRY_VALUE : null;
    }

    private TimeUnit getCacheTTLUnit(String company) {
        return longerCacheExpiryForVelocityTenants.contains(company) ? LONGER_CACHE_EXPIRY_UNIT : null;
    }

    private Set<IntegrationType> getCrossIntegrationTypesFromVelocityConfig(final String calculation, final VelocityConfigDTO velocityConfigDTO) {
        List<VelocityConfigDTO.Stage> allStages = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(velocityConfigDTO.getPreDevelopmentCustomStages())){
            allStages.addAll(velocityConfigDTO.getPreDevelopmentCustomStages());
        }
        if(CollectionUtils.isNotEmpty(velocityConfigDTO.getFixedStages())){
            allStages.addAll(velocityConfigDTO.getFixedStages());
        }
        if(CollectionUtils.isNotEmpty(velocityConfigDTO.getPostDevelopmentCustomStages())){
            allStages.addAll(velocityConfigDTO.getPostDevelopmentCustomStages());
        }
        Set<IntegrationType> integrationtypes = new HashSet<>();
        if("ticket_veloctity".equalsIgnoreCase(calculation) && allStages.stream().anyMatch(stage -> stage.getEvent().getType().isScmFamily())) {
            integrationtypes.addAll(IntegrationType.getSCMIntegrationTypes());
        }
        else if ("pr_velocity".equalsIgnoreCase(calculation) && allStages.stream().anyMatch(stage -> stage.getEvent().getType().hasIssueManagement())){
            integrationtypes.addAll(IntegrationType.getIssueManagementIntegrationTypes());
        }
        return integrationtypes;
    }

    private Set<IntegrationType> getIntegrationTypesFromVelocityConfig(VelocityConfigDTO velocityConfigDTO) {
        Set<IntegrationType> integrationtypes = new HashSet<>();
        List<VelocityConfigDTO.Stage> allStages = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(velocityConfigDTO.getPreDevelopmentCustomStages()))
            allStages.addAll(velocityConfigDTO.getPreDevelopmentCustomStages());
        if(CollectionUtils.isNotEmpty(velocityConfigDTO.getFixedStages()))
            allStages.addAll(velocityConfigDTO.getFixedStages());
        if(CollectionUtils.isNotEmpty(velocityConfigDTO.getPostDevelopmentCustomStages()))
            allStages.addAll(velocityConfigDTO.getPostDevelopmentCustomStages());
        Boolean hasIssueManagement = allStages.stream().anyMatch(stage -> stage.getEvent().getType().hasIssueManagement());
        Boolean isScmFamily = allStages.stream().anyMatch(stage -> stage.getEvent().getType().isScmFamily());
        Boolean isCiCdFamily = allStages.stream().anyMatch(stage -> stage.getEvent().getType().isCiCdFamily());

        if(hasIssueManagement)
            integrationtypes.addAll(IntegrationType.getIssueManagementIntegrationTypes());
        if(isScmFamily)
            integrationtypes.addAll(IntegrationType.getSCMIntegrationTypes());
        if(isCiCdFamily)
            integrationtypes.addAll(IntegrationType.getCICDIntegrationTypes());
        return integrationtypes;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'PUBLIC_DASHBOARD','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, value = "/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getVelocityValues(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "there_is_no_precalculate", required = false, defaultValue = "false") Boolean disablePrecalculatedResult,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {// OU stuff
            Long cacheTTLValue = getCacheTTLValue(company);
            TimeUnit cacheTTLUnit = getCacheTTLUnit(company);
            log.info("company {} cacheTTLValue = {} ",company,cacheTTLValue);
            DbListResponse<DbAggregationResult> dbListResponse = velocityAggsWidgetService.getVelocityValues(company, originalRequest, disableCache, cacheTTLValue, cacheTTLUnit, disablePrecalculatedResult);
            return ResponseEntity.ok().body(
                PaginatedResponse.of(
                        originalRequest.getPage(),
                        originalRequest.getPageSize(),
                        dbListResponse
                ));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/histogram", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbHistogramBucket>>> getVelocityHistogram(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "there_is_no_precalculate", required = false, defaultValue = "false") Boolean disablePrecalculatedResult,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            Long cacheTTLValue = getCacheTTLValue(company);
            TimeUnit cacheTTLUnit = getCacheTTLUnit(company);
            log.info("company {} cacheTTLValue = {} ",company,cacheTTLValue);
            return ResponseEntity.ok().body(
                PaginatedResponse.of(
                        originalRequest.getPage(),
                        originalRequest.getPageSize(),
                        velocityAggsWidgetService.getVelocityHistogram(company, originalRequest, disableCache, cacheTTLValue, cacheTTLUnit, disablePrecalculatedResult)
                ));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/histograms", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbHistogramResult>>> getVelocityHistograms(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "there_is_no_precalculate", required = false, defaultValue = "false") Boolean disablePrecalculatedResult,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            Long cacheTTLValue = getCacheTTLValue(company);
            TimeUnit cacheTTLUnit = getCacheTTLUnit(company);
            log.info("company {} cacheTTLValue = {} ",company,cacheTTLValue);
            return ResponseEntity.ok().body(
                PaginatedResponse.of(
                        originalRequest.getPage(),
                        originalRequest.getPageSize(),
                        velocityAggsWidgetService.getVelocityHistograms(company, originalRequest, disableCache, cacheTTLValue, cacheTTLUnit, disablePrecalculatedResult)
                ));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/histograms_serial", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbHistogramResult>>> getVelocityHistogramsSerial(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = OUConfiguration.builder().build();
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, "ticket_velocity".equalsIgnoreCase(originalRequest.getFilterValue("calculation", String.class).get()) ? IntegrationType.getIssueManagementIntegrationTypes() : IntegrationType.getSCMIntegrationTypes(), originalRequest, USE_INTEGRATION_PREFIX);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/velocity/histograms_serial' for the request: {}", company, originalRequest, e);
            }

            final var finalRequest = request;
            final var finalOuConfig = ouConfig;
            return ResponseEntity.ok().body(
                PaginatedResponse.of(
                        request.getPage(),
                        request.getPageSize(),
                        velocityAggsService.calculateVelocityHistogramsSerial(company, originalRequest, finalRequest, finalOuConfig)
                ));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/ratings", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<VelocityRatingResult>>> getVelocityByRatings(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "there_is_no_precalculate", required = false, defaultValue = "false") Boolean disablePrecalculatedResult,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            Long cacheTTLValue = getCacheTTLValue(company);
            TimeUnit cacheTTLUnit = getCacheTTLUnit(company);
            log.info("company {} cacheTTLValue = {} ",company,cacheTTLValue);
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            originalRequest.getPage(),
                            originalRequest.getPageSize(),
                            velocityAggsWidgetService.getVelocityByRatings(company, originalRequest, disableCache, cacheTTLValue, cacheTTLUnit, disablePrecalculatedResult)
                    ));
        });
    }
}
