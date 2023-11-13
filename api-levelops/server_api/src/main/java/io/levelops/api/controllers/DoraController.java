package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.services.DoraService;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.dora.DoraDrillDownDTO;
import io.levelops.commons.databases.models.dora.DoraResponseDTO;
import io.levelops.commons.databases.models.dora.DoraSingleStateDTO;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.service.dora.LegacyLeadTimeCalculationService;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@Log4j2
@RequestMapping("/v1/dora")
public class DoraController {

    private final DoraService doraService;
    private final LegacyLeadTimeCalculationService legacyLeadTimeCalculation;
    private final Set<String> longerCacheExpiryForVelocityTenants;

    private static final Long LONGER_CACHE_EXPIRY_VALUE = 480L;
    private static final TimeUnit LONGER_CACHE_EXPIRY_UNIT = TimeUnit.MINUTES;


    @Autowired
    public DoraController(DoraService doraService, LegacyLeadTimeCalculationService velocityAggsWidgetService,
                          @Value("${velocity.cache.longer_expiry_for_tenants:}") String longerCacheExpiryTenantsString) {
        this.doraService = doraService;
        this.legacyLeadTimeCalculation = velocityAggsWidgetService;
        this.longerCacheExpiryForVelocityTenants = CommaListSplitter.splitToStream(longerCacheExpiryTenantsString)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/drilldown/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DoraDrillDownDTO>>> getList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) throws BadRequestException {
        return SpringUtils.deferResponse(() -> {
            VelocityConfigDTO velocityConfigDTO = doraService.getVelocityConfigByOu(company, originalRequest);
            DbListResponse<DoraDrillDownDTO> doraResponseDTO = doraService.getList(disableCache, company, originalRequest, velocityConfigDTO);
            return ResponseEntity.ok(
                    PaginatedResponse.of(originalRequest.getPage(),
                            originalRequest.getPageSize(),doraResponseDTO));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "drilldown/scm-commits/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DoraDrillDownDTO>>> getScmCommitList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest
    ) {
        return SpringUtils.deferResponse(() -> {
            VelocityConfigDTO velocityConfigDTO = doraService.getVelocityConfigByOu(company, originalRequest);
            DbListResponse<DoraDrillDownDTO> doraResponseDTO = doraService.getScmCommitList(disableCache, company, originalRequest, velocityConfigDTO);
            return ResponseEntity.ok(
                    PaginatedResponse.of(originalRequest.getPage(),
                            originalRequest.getPageSize(), doraResponseDTO));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/deployment_frequency", produces = "application/json")
    public DeferredResult<ResponseEntity<DoraResponseDTO>> calculateDeploymentFrequency(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            VelocityConfigDTO velocityConfigDTO = doraService.getVelocityConfigByOu(company, originalRequest);
            String integrationType = getIntegrationTypeAndModifyRequestForDF(originalRequest, velocityConfigDTO);
            DoraResponseDTO doraResponseDTO = DoraResponseDTO.builder().build();
            switch (integrationType) {
                case ("SCM"):
                    doraResponseDTO = doraService.generateDeploymentFrequencyForSCM(company, originalRequest, disableCache, velocityConfigDTO);
                    break;
                case ("IM"):
                    doraResponseDTO = doraService.generateDFCountForIM(company, originalRequest, velocityConfigDTO);
                    break;
                case ("CICD"):
                    doraResponseDTO = doraService.generateDFCountForCICD(company, originalRequest, velocityConfigDTO);
                    break;
            }

            DoraSingleStateDTO stats = doraResponseDTO.getStats();
            Double countPerDay = stats.getCountPerDay();
            stats = stats.toBuilder().countPerMonth(countPerDay*30).countPerWeek(countPerDay*7).build();
            doraResponseDTO = doraResponseDTO.toBuilder().stats(stats).build();
            return ResponseEntity.ok(doraResponseDTO);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/change_failure_rate", produces = "application/json")
    public DeferredResult<ResponseEntity<DoraResponseDTO>> calculateChangeFailureRate(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            VelocityConfigDTO velocityConfigDTO = doraService.getVelocityConfigByOu(company, originalRequest);
            String integrationType = getIntegrationTypeAndModifyRequestForCFR(originalRequest, velocityConfigDTO);
            DoraResponseDTO doraResponseDTO = DoraResponseDTO.builder().build();
            switch (integrationType) {
                case ("SCM"):
                    doraResponseDTO = doraService.generateChangeFailureRateForSCM(company, originalRequest, disableCache, velocityConfigDTO);
                    break;
                case ("IM"):
                    doraResponseDTO = doraService.generateCFRCountForIM(company, originalRequest, velocityConfigDTO);
                    break;
                case ("CICD"):
                    doraResponseDTO = doraService.generateCFRCountforCICD(company, originalRequest, velocityConfigDTO);
                    break;
                default:
            }
            return ResponseEntity.ok(doraResponseDTO);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, value = "/lead-time", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getVelocityAggsForLeadTime(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "there_is_no_precalculate", required = false, defaultValue = "false") Boolean disablePrecalculatedResult,
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            Long cacheTTLValue = getCacheTTLValue(company);
            TimeUnit cacheTTLUnit = getCacheTTLUnit(company);
            log.info("company {}, cacheTTLValue = {}", company, cacheTTLValue);
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            originalRequest.getPage(),
                            originalRequest.getPageSize(),
                            legacyLeadTimeCalculation.getNewVelocityAggsForLeadTime(
                                    company, originalRequest, disableCache, cacheTTLValue, cacheTTLUnit, disablePrecalculatedResult
                            )
                    ));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN','PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, value = "/mean-time", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getVelocityAggsForMeanTime(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "there_is_no_precalculate", required = false, defaultValue = "false") Boolean disablePrecalculatedResult,
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            Long cacheTTLValue = getCacheTTLValue(company);
            TimeUnit cacheTTLUnit = getCacheTTLUnit(company);
            log.info("company {}, cacheTTLValue = {}", company, cacheTTLValue);
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            originalRequest.getPage(),
                            originalRequest.getPageSize(),
                            legacyLeadTimeCalculation.getNewVelocityAggsForMeanTime(
                                    company, originalRequest, disableCache, cacheTTLValue, cacheTTLUnit, disablePrecalculatedResult
                            )
                    ));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'PUBLIC_DASHBOARD','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, value = "/lead-time/drilldown", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getVelocityValuesForLeadTime(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "there_is_no_precalculate", required = false, defaultValue = "false") Boolean disablePrecalculatedResult,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {// OU stuff
            Long cacheTTLValue = getCacheTTLValue(company);
            TimeUnit cacheTTLUnit = getCacheTTLUnit(company);
            log.info("company {} cacheTTLValue = {} ", company, cacheTTLValue);
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            originalRequest.getPage(),
                            originalRequest.getPageSize(),
                            legacyLeadTimeCalculation.getVelocityValuesForLeadTime(
                                    company, originalRequest, disableCache, cacheTTLValue, cacheTTLUnit, disablePrecalculatedResult
                            )
                    ));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'PUBLIC_DASHBOARD','ORG_ADMIN_USER')")
    @RequestMapping(method = RequestMethod.POST, value = "/mean-time/drilldown", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getVelocityValuesForMeanTime(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "there_is_no_precalculate", required = false, defaultValue = "false") Boolean disablePrecalculatedResult,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {// OU stuff
            Long cacheTTLValue = getCacheTTLValue(company);
            TimeUnit cacheTTLUnit = getCacheTTLUnit(company);
            log.info("company {} cacheTTLValue = {} ", company, cacheTTLValue);
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            originalRequest.getPage(),
                            originalRequest.getPageSize(),
                            legacyLeadTimeCalculation.getVelocityValuesForMeanTime(
                                    company, originalRequest, disableCache, cacheTTLValue, cacheTTLUnit, disablePrecalculatedResult
                            )
                    ));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/cicd-job-params", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, List<String>>>> getCicdJobParams(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        log.debug("filter = {}", originalRequest);
        return SpringUtils.deferResponse(() -> {
            Map<String, Object> filters = originalRequest.getFilter();
            List<String> jobIds = (List<String>) filters.get("cicd_job_ids");
            return ResponseEntity.ok(doraService.getCicdJobParams(company, jobIds));
        });
    }

    private String getIntegrationTypeAndModifyRequestForDF(DefaultListRequest originalRequest,
                                                           VelocityConfigDTO velocityConfigDTO) {

        Map<String, Object> filter = originalRequest.getFilter() != null ? originalRequest.getFilter() : new HashMap<>();
        String integrationType = velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getIntegrationType();

        filter.put(
                velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getCalculationField().toString(),
                filter.get("time_range")
        );
        List<Integer> integrationIds = velocityConfigDTO.getDeploymentFrequency().getIntegrationIds();
        filter.put("integration_ids", integrationIds);

        originalRequest.toBuilder().filter(filter);
        return integrationType;
    }

    private String getIntegrationTypeAndModifyRequestForCFR(DefaultListRequest originalRequest,
                                                            VelocityConfigDTO velocityConfigDTO) {

        Map<String, Object> filter = originalRequest.getFilter() != null ? originalRequest.getFilter() : new HashMap<>();
        String integrationType = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getIntegrationType();

        filter.put(
                velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment()
                        .getCalculationField().toString(),
                filter.get("time_range")
        );
        if (velocityConfigDTO.getChangeFailureRate().getIsAbsoulte() != null
                && !velocityConfigDTO.getChangeFailureRate().getIsAbsoulte()) {
            filter.put(
                    velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment()
                            .getCalculationField().toString(),
                    filter.get("time_range")
            );
        }
        List<Integer> integrationIds = velocityConfigDTO.getChangeFailureRate().getIntegrationIds();
        filter.put("integration_ids", integrationIds);

        originalRequest.toBuilder().filter(filter);
        return integrationType;
    }

    private Long getCacheTTLValue(String company) {
        return longerCacheExpiryForVelocityTenants.contains(company) ? LONGER_CACHE_EXPIRY_VALUE : null;
    }

    private TimeUnit getCacheTTLUnit(String company) {
        return longerCacheExpiryForVelocityTenants.contains(company) ? LONGER_CACHE_EXPIRY_UNIT : null;
    }
}
