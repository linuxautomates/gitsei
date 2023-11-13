package io.levelops.commons.service.dora;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.services.velocity_productivity.services.VelocityAggsService;
import io.levelops.commons.services.velocity_productivity.services.VelocityAggsWidgetService;
import io.levelops.commons.services.velocity_productivity.services.VelocityConfigsService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class LegacyLeadTimeCalculationService {

    private final VelocityAggsService velocityAggsService;
    private final ObjectMapper mapper;
    private final AggCacheService cacheService;
    private final LegacyLeadTimePrecalculatedWidgetReadService legacyLeadTimePrecalculatedWidgetReadService;
    private final VelocityAggsWidgetService velocityAggsWidgetService;

    private final VelocityConfigsService velocityConfigsService;

    @Autowired
    public LegacyLeadTimeCalculationService(VelocityAggsService velocityAggsService,
                                            ObjectMapper mapper,
                                            AggCacheService cacheService,
                                            LegacyLeadTimePrecalculatedWidgetReadService legacyLeadTimePrecalculatedWidgetReadService,
                                            VelocityConfigsService velocityConfigsService,
                                            VelocityAggsWidgetService velocityAggsWidgetService) {
        this.velocityAggsService = velocityAggsService;
        this.mapper = mapper;
        this.cacheService = cacheService;
        this.legacyLeadTimePrecalculatedWidgetReadService = legacyLeadTimePrecalculatedWidgetReadService;
        this.velocityConfigsService = velocityConfigsService;
        this.velocityAggsWidgetService = velocityAggsWidgetService;
    }


    public List<DbAggregationResult> getNewVelocityAggsForLeadTime(final String company, DefaultListRequest originalRequest, Boolean disableCache, Long cacheTTLValue, TimeUnit cacheTTLUnit, Boolean disablePrecalculatedResult) throws Exception {
        var opt = legacyLeadTimePrecalculatedWidgetReadService.getNewVelocityAggsForLeadTime(company, originalRequest, disablePrecalculatedResult);
        if (opt.isPresent()) {
            return opt.get();
        }
        VelocityConfigDTO velocityConfigDTO = getVelocityConfigByOu(company, originalRequest);
        velocityConfigDTO = velocityConfigsService.adapterLeadTimeForChanges(velocityConfigDTO);
        originalRequest = enrichRequest(velocityConfigDTO, originalRequest);

        VelocityAggsWidgetService.OUCustomization ouCustomization = velocityAggsWidgetService.getOUCustomization(company, originalRequest, "/velocity", true, velocityConfigDTO);
        return AggCacheUtils.cacheOrCallGeneric(disableCache, company,
                "/dora/lead-time",
                ouCustomization.getHash(), List.of(), mapper, cacheService, null, cacheTTLValue, cacheTTLUnit,
                () -> velocityAggsService.calculateVelocity(company, ouCustomization.getOriginalRequest(), ouCustomization.getFinalRequest(), ouCustomization.getVelocityConfigDTO(), ouCustomization.getFinalOuConfig()));
    }

    public List<DbAggregationResult> getNewVelocityAggsForMeanTime(final String company, DefaultListRequest originalRequest, Boolean disableCache, Long cacheTTLValue, TimeUnit cacheTTLUnit, Boolean disablePrecalculatedResult) throws Exception {
        var opt = legacyLeadTimePrecalculatedWidgetReadService.getNewVelocityAggsForMeanTime(company, originalRequest, disablePrecalculatedResult);
        if (opt.isPresent()) {
            return opt.get();
        }
        VelocityConfigDTO velocityConfigDTO = getVelocityConfigByOu(company, originalRequest);
        velocityConfigDTO = velocityConfigsService.adapterMeanTimeToRestore(velocityConfigDTO);
        originalRequest = enrichRequest(velocityConfigDTO, originalRequest);

        VelocityAggsWidgetService.OUCustomization ouCustomization = velocityAggsWidgetService.getOUCustomization(company, originalRequest, "/velocity", true, velocityConfigDTO);
        return AggCacheUtils.cacheOrCallGeneric(disableCache, company,
                "/dora/mean-time",
                ouCustomization.getHash(), List.of(), mapper, cacheService, null, cacheTTLValue, cacheTTLUnit,
                () -> velocityAggsService.calculateVelocity(company, ouCustomization.getOriginalRequest(), ouCustomization.getFinalRequest(), ouCustomization.getVelocityConfigDTO(), ouCustomization.getFinalOuConfig()));
    }

    public DbListResponse<DbAggregationResult> getVelocityValuesForLeadTime(final String company, DefaultListRequest originalRequest, Boolean disableCache, Long cacheTTLValue, TimeUnit cacheTTLUnit, Boolean disablePrecalculatedResult) throws Exception {
        var opt = legacyLeadTimePrecalculatedWidgetReadService.getVelocityValuesForLeadTime(company, originalRequest, disablePrecalculatedResult);
        if (opt.isPresent()) {
            return opt.get();
        }
        VelocityConfigDTO velocityConfigDTO = getVelocityConfigByOu(company, originalRequest);
        velocityConfigDTO = velocityConfigsService.adapterLeadTimeForChanges(velocityConfigDTO);
        originalRequest = enrichRequest(velocityConfigDTO, originalRequest);

        VelocityAggsWidgetService.OUCustomization ouCustomization = velocityAggsWidgetService.getOUCustomization(company, originalRequest, "/velocity/values", true, velocityConfigDTO);
        return AggCacheUtils.cacheOrCallGeneric(disableCache, company,
                "/dora/lead-time/drilldown",
                ouCustomization.getHash(), List.of(), mapper, cacheService, DbListResponse.class, cacheTTLValue, cacheTTLUnit,
                () -> velocityAggsService.calculateVelocityValues(company, ouCustomization.getOriginalRequest(), ouCustomization.getFinalRequest(), ouCustomization.getVelocityConfigDTO(), ouCustomization.getFinalOuConfig()));
    }

    public DbListResponse<DbAggregationResult> getVelocityValuesForMeanTime(final String company, DefaultListRequest originalRequest, Boolean disableCache, Long cacheTTLValue, TimeUnit cacheTTLUnit, Boolean disablePrecalculatedResult) throws Exception {
        var opt = legacyLeadTimePrecalculatedWidgetReadService.getVelocityValuesForMeanTime(company, originalRequest, disablePrecalculatedResult);
        if (opt.isPresent()) {
            return opt.get();
        }
        VelocityConfigDTO velocityConfigDTO = getVelocityConfigByOu(company, originalRequest);
        velocityConfigDTO = velocityConfigsService.adapterMeanTimeToRestore(velocityConfigDTO);
        originalRequest = enrichRequest(velocityConfigDTO, originalRequest);

        VelocityAggsWidgetService.OUCustomization ouCustomization = velocityAggsWidgetService.getOUCustomization(company, originalRequest, "/velocity/values", true, velocityConfigDTO);
        return AggCacheUtils.cacheOrCallGeneric(disableCache, company,
                "/dora/mean-time/drilldown",
                ouCustomization.getHash(), List.of(), mapper, cacheService, DbListResponse.class, cacheTTLValue, cacheTTLUnit,
                () -> velocityAggsService.calculateVelocityValues(company, ouCustomization.getOriginalRequest(), ouCustomization.getFinalRequest(), ouCustomization.getVelocityConfigDTO(), ouCustomization.getFinalOuConfig()));
    }

    private DefaultListRequest enrichRequest(VelocityConfigDTO velocityConfigDTO, DefaultListRequest request) {
        Map<String, Object> enrichedFilter = request.getFilter();
        if (velocityConfigDTO.getStartingEventIsCommitCreated() != null && velocityConfigDTO.getStartingEventIsCommitCreated()) {
            enrichedFilter.putAll(Map.of(
                    "calculation", "pr_velocity",
                    "velocity_config_id", velocityConfigDTO.getId().toString()
            ));
        } else {
            enrichedFilter.putAll(Map.of(
                    "calculation", "ticket_velocity",
                    "velocity_config_id", velocityConfigDTO.getId().toString()
            ));
        }
        request = request.toBuilder()
                .filter(enrichedFilter)
                .build();
        return request;
    }

    public VelocityConfigDTO getVelocityConfigByOu(String company, DefaultListRequest request) throws SQLException {
        if (request.getOuIds() == null || request.getOuIds().stream().findFirst().isEmpty()) {
            throw new RuntimeException("ou_id is missing in request.");
        }
        int ouRefId = request.getOuIds().stream().findFirst().get();
        Optional<VelocityConfigDTO> velocityConfigDTO = velocityConfigsService.getByOuRefId(company, ouRefId);
        if (velocityConfigDTO.isEmpty()) {
            throw new RuntimeException("Failed to get workflow profile for ou_ref_id  " + ouRefId);
        }
        return velocityConfigDTO.get();
    }
}
