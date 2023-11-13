package io.levelops.commons.services.velocity_productivity.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.util.MapUtilsForRESTControllers;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.DbHistogramBucket;
import io.levelops.commons.databases.models.response.DbHistogramResult;
import io.levelops.commons.databases.models.response.VelocityRatingResult;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.NotFoundException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
@Service
public class VelocityAggsWidgetService {
    private static final boolean USE_INTEGRATION_PREFIX = true;

    private final VelocityAggsService velocityAggsService;
    private final OrgUnitHelper ouHelper;
    private final ObjectMapper mapper;
    private final AggCacheService cacheService;
    private final VelocityAggsPrecalculatedWidgetReadService velocityAggsPrecalculatedWidgetReadService;

    @Autowired
    public VelocityAggsWidgetService(VelocityAggsService velocityAggsService, OrgUnitHelper ouHelper, ObjectMapper mapper, AggCacheService cacheService, VelocityAggsPrecalculatedWidgetReadService velocityAggsPrecalculatedWidgetReadService) {
        this.velocityAggsService = velocityAggsService;
        this.ouHelper = ouHelper;
        this.mapper = mapper;
        this.cacheService = cacheService;
        this.velocityAggsPrecalculatedWidgetReadService = velocityAggsPrecalculatedWidgetReadService;
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

    public List<DbAggregationResult> getVelocityAggs(final String company, DefaultListRequest originalRequest, Boolean disableCache, Long cacheTTLValue, TimeUnit cacheTTLUnit, Boolean disablePrecalculatedResult) throws Exception {
        var opt = velocityAggsPrecalculatedWidgetReadService.getVelocityAggs(company, originalRequest, disablePrecalculatedResult);
        if(opt.isPresent()) {
            return opt.get();
        }
        OUCustomization ouCustomization = getOUCustomization(company, originalRequest, "/velocity", true);
        return AggCacheUtils.cacheOrCallGeneric(disableCache, company,
                "/velocity/agg_",
                ouCustomization.getHash(), List.of(), mapper, cacheService, null, cacheTTLValue, cacheTTLUnit,
                () -> velocityAggsService.calculateVelocity(company, ouCustomization.getOriginalRequest(), ouCustomization.getFinalRequest(), ouCustomization.getVelocityConfigDTO(), ouCustomization.getFinalOuConfig()));
    }

    public DbListResponse<DbAggregationResult> getVelocityValues(final String company, DefaultListRequest originalRequest, Boolean disableCache, Long cacheTTLValue, TimeUnit cacheTTLUnit, Boolean disablePrecalculatedResult) throws Exception {
        var opt = velocityAggsPrecalculatedWidgetReadService.getVelocityValues(company, originalRequest, disablePrecalculatedResult);
        if(opt.isPresent()) {
            return opt.get();
        }
        OUCustomization ouCustomization = getOUCustomization(company, originalRequest, "/velocity/values", true);
        return AggCacheUtils.cacheOrCallGeneric(disableCache, company,
                "/velocity/aggs_values_",
                ouCustomization.getHash(), List.of(), mapper, cacheService, DbListResponse.class, cacheTTLValue, cacheTTLUnit,
                () -> velocityAggsService.calculateVelocityValues(company, ouCustomization.getOriginalRequest(), ouCustomization.getFinalRequest(), ouCustomization.getVelocityConfigDTO(), ouCustomization.getFinalOuConfig()));
    }

    public DbListResponse<DbHistogramBucket> getVelocityHistogram(final String company, DefaultListRequest originalRequest, Boolean disableCache, Long cacheTTLValue, TimeUnit cacheTTLUnit, Boolean disablePrecalculatedResult) throws Exception {
        var opt = velocityAggsPrecalculatedWidgetReadService.getVelocityHistogram(company, originalRequest, disablePrecalculatedResult);
        if(opt.isPresent()) {
            return opt.get();
        }
        OUCustomization ouCustomization = getOUCustomization(company, originalRequest, "/velocity/histogram", false);
        return AggCacheUtils.cacheOrCallGeneric(disableCache, company,
                "/velocity/aggs_histogram_",
                ouCustomization.getHash(), List.of(), mapper, cacheService, null, cacheTTLValue, cacheTTLUnit,
                () -> velocityAggsService.calculateVelocityHistogram(company, ouCustomization.getOriginalRequest(), ouCustomization.getFinalRequest(), ouCustomization.getVelocityConfigDTO(), ouCustomization.getFinalOuConfig()));
    }

    public DbListResponse<DbHistogramResult> getVelocityHistograms(final String company, DefaultListRequest originalRequest, Boolean disableCache, Long cacheTTLValue, TimeUnit cacheTTLUnit, Boolean disablePrecalculatedResult) throws Exception {
        var opt = velocityAggsPrecalculatedWidgetReadService.getVelocityHistograms(company, originalRequest, disablePrecalculatedResult);
        if(opt.isPresent()) {
            return opt.get();
        }
        OUCustomization ouCustomization = getOUCustomization(company, originalRequest, "/velocity/histograms", false);
        return AggCacheUtils.cacheOrCallGeneric(disableCache, company,
                "/velocity/aggs_histograms_",
                ouCustomization.getHash(), List.of(), mapper, cacheService, null, cacheTTLValue, cacheTTLUnit,
                () -> velocityAggsService.calculateVelocityHistograms(company, ouCustomization.getOriginalRequest(), ouCustomization.getFinalRequest(), ouCustomization.getVelocityConfigDTO(), ouCustomization.getFinalOuConfig()));
    }

    public DbListResponse<VelocityRatingResult> getVelocityByRatings(final String company, DefaultListRequest originalRequest, Boolean disableCache, Long cacheTTLValue, TimeUnit cacheTTLUnit, Boolean disablePrecalculatedResult) throws Exception {
        var opt = velocityAggsPrecalculatedWidgetReadService.getVelocityByRatings(company, originalRequest, disablePrecalculatedResult);
        if(opt.isPresent()) {
            return opt.get();
        }
        OUCustomization ouCustomization = getOUCustomization(company, originalRequest, "/velocity/ratings", false);
        return AggCacheUtils.cacheOrCallGeneric(disableCache, company,
                "/velocity/aggs_ratings_",
                ouCustomization.getHash(), List.of(), mapper, cacheService, null, cacheTTLValue, cacheTTLUnit,
                () -> velocityAggsService.calculateVelocityRatings(company, ouCustomization.getOriginalRequest(), ouCustomization.getFinalRequest(), ouCustomization.getVelocityConfigDTO(), ouCustomization.getFinalOuConfig()));
    }

    public OUCustomization getOUCustomization(final String company, DefaultListRequest originalRequest, String api, boolean ouAdditional) throws SQLException, NotFoundException, JsonProcessingException {
        return getOUCustomization(company, originalRequest, api, ouAdditional, null);
    }

    public OUCustomization getOUCustomization(final String company, DefaultListRequest originalRequest, String api, boolean ouAdditional, VelocityConfigDTO velocityConfigDTO) throws SQLException, NotFoundException, JsonProcessingException {
        // OU stuff
        var request = originalRequest;
        log.info("originalRequest before = {}", originalRequest);
        OUConfiguration ouConfig = OUConfiguration.builder().build();
        velocityConfigDTO = velocityConfigDTO != null ? velocityConfigDTO : velocityAggsService.getVelocityConfig(company, originalRequest);
        log.info("velocityConfigDTO = {}", velocityConfigDTO);
        var calculation = originalRequest.getFilterValue("calculation", String.class).get();
        try {
            // we get the OU filters only for the type of integration that correspond to the event originator... for ticket_velocity Issue Management and for pr_velocity SCM
            ouConfig = ouHelper.getOuConfigurationFromRequest(company, "ticket_velocity".equalsIgnoreCase(calculation) ? IntegrationType.getIssueManagementIntegrationTypes() : IntegrationType.getSCMIntegrationTypes(), originalRequest, USE_INTEGRATION_PREFIX);
            request = ouConfig.getRequest();

            if(ouAdditional) {
                // if there was an OU, then do the special handling
                if (ouConfig.getOuId() != null) {
                    // if there are cross integrations then we add them. Lookup in the profile to see if other integration types are expected in the stages.
                    Set<IntegrationType> crossIntegrationTypes = getCrossIntegrationTypesFromVelocityConfig(calculation, velocityConfigDTO);
                    // if the dashboard is sending integration ids get them to complement the OU config.
                    var integrationIds = new HashSet<>((MapUtilsForRESTControllers.getListOrDefault(originalRequest.getFilter(), "integration_ids")).stream().map(item -> Integer.valueOf(item)).collect(Collectors.toSet()));
                    var ouIntegrationIds = new HashSet<>((MapUtilsForRESTControllers.getListOrDefault(request.getFilter(), "integration_ids")).stream().map(item -> Integer.valueOf(item)).collect(Collectors.toSet()));
                    if (CollectionUtils.isNotEmpty(integrationIds)) {
                        // remove the ou integration as those will already be included
                        integrationIds.removeAll(ouIntegrationIds);
                        // check if there are cross-integrations... for ticket_velocity look for SCM integrations. for pr_velocity look for Issue Management integrations.
                        if (CollectionUtils.isNotEmpty(integrationIds)) {
                            // just add back the integraiton ids not in the OU config to the request... alternatively we could check if the remaining integrations from the original request are of the needed type and if not we could still go and lookup the right type in the db... for now no...
                            var newIntegrationIds = new HashSet<>(ouConfig.getIntegrationIds());
                            newIntegrationIds.addAll(integrationIds);
                            var newFilters = request.getFilter();
                            newFilters.put("integration_ids", newIntegrationIds);
                            request = request.toBuilder().filter(newFilters).build();
                            ouConfig = ouConfig.toBuilder().integrationIds(newIntegrationIds).request(request).build();
                            log.info("Modified OUConfig with integration_ids (from dashboard): {}", ouConfig);
                        }
                    }
                    // if the dashboard is not sending integration get the complemental integration ids from the db. first try to get the integration from the OUHelper in case that there is a specif integration of the type already defined in the OU.
                    else if (CollectionUtils.isNotEmpty(crossIntegrationTypes)) {
                        // get the oposite integrationTypes
                        var tmpOuConfig = ouHelper.getOuConfigurationFromRequest(company, "ticket_velocity".equalsIgnoreCase(calculation) ? IntegrationType.getSCMIntegrationTypes() : IntegrationType.getIssueManagementIntegrationTypes(), originalRequest, USE_INTEGRATION_PREFIX);
                        if (CollectionUtils.isNotEmpty(tmpOuConfig.getIntegrationIds())) {
                            // merge the integration ids from the main OUConfig (originator) with the cross integrations ones.
                            var newIntegrationIds = new HashSet<>(ouConfig.getIntegrationIds());
                            newIntegrationIds.addAll(tmpOuConfig.getIntegrationIds());
                            var newFilters = request.getFilter();
                            newFilters.put("integration_ids", newIntegrationIds);
                            request = request.toBuilder().filter(newFilters).build();
                            ouConfig = ouConfig.toBuilder().integrationIds(newIntegrationIds).request(request).build();
                            log.info("Modified OUConfig with integration_ids (from OU cross integrations): {}", ouConfig);
                        }
                    }

                    var configBuilder = ouConfig.toBuilder();
                    // if there are user selection... aparently it needs to apply only to the originator and not to the entire chain LEV-4493
                    if (ouConfig.hasUsersSelection()) {
                        if ("ticket_velocity".equalsIgnoreCase(calculation)) {
                            configBuilder.githubFields(Set.of("none"));
                            configBuilder.gitlabFields(Set.of("none"));
                            configBuilder.helixFields(Set.of("none"));
                            configBuilder.bitbucketFields(Set.of("none"));
                            configBuilder.bitbucketServerFields(Set.of("none"));
                            configBuilder.scmFields(Set.of("none"));

                            configBuilder.jiraFields(Set.of("assignee"));
                            configBuilder.adoFields(Set.of("assignee"));
                        } else {
                            configBuilder.cicdFields(Set.of("none"));
                            configBuilder.pagerDutyFields(Set.of("none"));
                            configBuilder.jiraFields(Set.of("none"));
                            configBuilder.adoFields(Set.of("none"));

                            configBuilder.githubFields(Set.of("author"));
                            configBuilder.gitlabFields(Set.of("author"));
                            configBuilder.helixFields(Set.of("author"));
                            configBuilder.bitbucketFields(Set.of("author"));
                            configBuilder.bitbucketServerFields(Set.of("author"));
                            configBuilder.scmFields(Set.of("author"));
                        }
                    }
                    ouConfig = configBuilder.build();
                }
            }
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/velocity' for the request: {}", company, originalRequest, e);
        }

        final var finalRequest = request;
        final var finalOuConfig = ouConfig;
        List<Object> data = List.of(company, finalRequest, velocityConfigDTO, finalOuConfig);
        //Do not enable this. ouConfig.hashCode() returns different hash code on different containers.
        //String hash = Hashing.sha256().hashBytes(mapper.writeValueAsBytes(data)).toString() + ouConfig.hashCode();
        String hash = Hashing.sha256().hashBytes(mapper.writeValueAsBytes(data)).toString();
        log.info("hash = {}", hash);
        log.info("originalRequest = {}", originalRequest);
        log.info("finalRequest = {}", finalRequest);
        return OUCustomization.builder()
                .originalRequest(originalRequest)
                .finalRequest(finalRequest)
                .velocityConfigDTO(velocityConfigDTO)
                .finalOuConfig(finalOuConfig)
                .hash(hash)
                .build();
    }

    @Value
    @Builder(toBuilder = true)
    public static class OUCustomization {
        private final DefaultListRequest originalRequest;
        private final DefaultListRequest finalRequest;
        private final VelocityConfigDTO velocityConfigDTO;
        private final OUConfiguration finalOuConfig;
        private final String hash;
    }
}
