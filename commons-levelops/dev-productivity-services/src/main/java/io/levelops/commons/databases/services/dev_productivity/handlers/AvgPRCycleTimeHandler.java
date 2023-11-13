package io.levelops.commons.databases.services.dev_productivity.handlers;

import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.TenantSCMSettings;
import io.levelops.commons.databases.models.database.dev_productivity.BreakDownType;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureBreakDown;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureResponse;
import io.levelops.commons.databases.models.database.dev_productivity.IntegrationUserDetails;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.ScmQueryUtils;
import io.levelops.commons.databases.services.dev_productivity.utils.FeatureHandlerUtil;
import io.levelops.commons.databases.services.dev_productivity.utils.ScmPRUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.faceted_search.services.scm_service.EsScmPRsService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AvgPRCycleTimeHandler implements DevProductivityFeatureHandler {

    private final ScmAggService scmAggService;
    private final EsScmPRsService esScmPRsService;
    private final Set<String> dbAllowedTenants;

    @Autowired
    public AvgPRCycleTimeHandler(ScmAggService scmAggService, EsScmPRsService esScmPRsService,
                                 @Value("${DB_DEV_PROD_SCM:}") List<String> dbAllowedTenants) {
        this.scmAggService = scmAggService;
        this.esScmPRsService = esScmPRsService;
        this.dbAllowedTenants = new HashSet<>();
        if (CollectionUtils.isNotEmpty(dbAllowedTenants)) {
            this.dbAllowedTenants.addAll(dbAllowedTenants);
        }
    }

    @Override
    public Set<DevProductivityProfile.FeatureType> getSupportedFeatureTypes() {
        return Set.of(DevProductivityProfile.FeatureType.AVG_PR_CYCLE_TIME);
    }

    @Override
    public FeatureResponse calculateFeature(String company, final Integer sectionOrder, DevProductivityProfile.Feature feature, Map<String, Object> profileSettings,
                                            DevProductivityFilter devProductivityFilter, OrgUserDetails orgUserDetails, final Map<String, Long> latestIngestedAtByIntegrationId, final TenantSCMSettings tenantSCMSettings) throws SQLException, IOException {

        ScmPrFilter scmPrFilter = getScmPrFilter(orgUserDetails, profileSettings, devProductivityFilter);
        if (scmPrFilter == null) {
            return FeatureResponse.constructIntegrationsAbsentBuilder(sectionOrder, feature).build();
        }

        Double result = null;
        Double mean = null;

        DbListResponse<DbAggregationResult> aggregationResult = null;
        boolean useEs = FeatureHandlerUtil.useEs(company, dbAllowedTenants, devProductivityFilter);
        if (useEs) {
            aggregationResult = esScmPRsService.groupByAndCalculatePrs(company, scmPrFilter, false, null);
        } else {
            aggregationResult = scmAggService.groupByAndCalculatePrs(company, scmPrFilter, null);
        }

        if (aggregationResult.getTotalCount() > 0) {
            result = deriveFeatureHandler(aggregationResult);
            mean = Double.valueOf(new DecimalFormat("#.##").format(result));
        }

        //Do NOT remove Long.valueOf will cause NPE - https://stackoverflow.com/questions/5246776/java-weird-nullpointerexception-in-ternary-operator
        Long value = (mean != null) ? Long.valueOf(Math.round(mean)) : feature.getFeatureType().getDefaultValue(); //Initialize value or Override value

        return FeatureResponse.constructBuilder(sectionOrder, feature, value)
                .mean(mean)
                .count(value)
                .build();

    }

    @Override
    public FeatureBreakDown getBreakDown(String company, DevProductivityProfile.Feature feature, Map<String, Object> profileSettings, DevProductivityFilter devProductivityFilter, OrgUserDetails orgUserDetails, Map<String, Long> latestIngestedAtByIntegrationId, TenantSCMSettings tenantSCMSettings, Map<String, SortingOrder> sortBy, Integer pageNumber, Integer pageSize) throws SQLException, IOException {

        ScmPrFilter scmPrFilter = getScmPrFilter(orgUserDetails, profileSettings, devProductivityFilter);
        List<DbScmPullRequest> scmPrs = List.of();
        long totalCount = 0;

        if (scmPrFilter != null) {
            DbListResponse<DbScmPullRequest> dbListResponse = null;
            boolean useEs = FeatureHandlerUtil.useEs(company, dbAllowedTenants, devProductivityFilter);
            if(useEs){
                dbListResponse = esScmPRsService.list(company, scmPrFilter, sortBy, null, pageNumber, pageSize);
            }else {
                dbListResponse = scmAggService.list(company, scmPrFilter, sortBy, null, pageNumber, pageSize);
            }
            scmPrs = dbListResponse.getRecords();
            totalCount = dbListResponse.getTotalCount();
        }

        return FeatureBreakDown.builder()
                .orgUserId(orgUserDetails.getOrgUserId())
                .email(orgUserDetails.getEmail())
                .fullName(orgUserDetails.getFullName())
                .name(feature.getName())
                .description(feature.getDescription())
                .breakDownType(BreakDownType.SCM_PRS)
                .records(scmPrs)
                .count(totalCount)
                .build();
    }

    private ScmPrFilter getScmPrFilter(OrgUserDetails orgUserDetails, Map<String, Object> profileSettings, DevProductivityFilter devProductivityFilter) {

        List<IntegrationUserDetails> userDetails = orgUserDetails.getIntegrationUserDetailsList().stream()
                .filter(i -> ScmQueryUtils.isScmIntegration(i.getIntegrationType()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(userDetails)) {
            return null;
        }

        Pair<Map<String, Map<String, String>>, Map<String, Map<String, String>>> partialMatchPair = ScmPRUtils.checkPartialMatchConditions(profileSettings);
        ScmPrFilter.ScmPrFilterBuilder scmPrFilterBuilder = ScmPRUtils.getPrFiltersBuilder(profileSettings);

        return scmPrFilterBuilder
                .creators(userDetails.stream()
                        .map(IntegrationUserDetails::getIntegrationUserId)
                        .map(UUID::toString)
                        .collect(Collectors.toList()))
                .prMergedRange(devProductivityFilter.getTimeRange())
                .integrationIds(userDetails.stream()
                        .map(IntegrationUserDetails::getIntegrationId)
                        .map(String::valueOf)
                        .collect(Collectors.toList()))
                .partialMatch(partialMatchPair.getLeft())
                .excludePartialMatch(partialMatchPair.getRight())
                .calculation(ScmPrFilter.CALCULATION.merge_time)
                .across(ScmPrFilter.DISTINCT.creator)
                .build();
    }

    private Double deriveFeatureHandler(DbListResponse<DbAggregationResult> aggregationResult) {
        return aggregationResult.getRecords().stream()
                .mapToDouble(DbAggregationResult::getMean)
                .average()
                .orElse(0.0);
    }
}