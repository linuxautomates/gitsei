package io.levelops.commons.databases.services.dev_productivity.handlers;


import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.TenantSCMSettings;
import io.levelops.commons.databases.models.database.dev_productivity.*;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.ScmQueryUtils;
import io.levelops.commons.databases.services.dev_productivity.utils.FeatureHandlerUtil;
import io.levelops.commons.databases.services.dev_productivity.utils.ScmPRUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.MapUtils;
import io.levelops.faceted_search.services.scm_service.EsScmPRsService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

@Component
public class DevPRReviewDepthHandler implements DevProductivityFeatureHandler {

    private final ScmAggService scmAggService;
    private final EsScmPRsService esScmPRsService;
    private final Set<String> dbAllowedTenants;

    @Autowired
    public DevPRReviewDepthHandler(ScmAggService scmAggService, EsScmPRsService esScmPRsService,
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
        return Set.of(DevProductivityProfile.FeatureType.PRS_REVIEW_DEPTH);
    }

    @Override
    public FeatureResponse calculateFeature(String company, final Integer sectionOrder, DevProductivityProfile.Feature feature,
                                            Map<String, Object> profileSettings, DevProductivityFilter devProductivityFilter,
                                            OrgUserDetails orgUserDetails, final Map<String, Long> latestIngestedAtByIntegrationId,
                                            final TenantSCMSettings tenantSCMSettings) throws SQLException, IOException {

        ScmPrFilter scmPrFilter = getScmPrFilter(feature, profileSettings, devProductivityFilter, orgUserDetails, tenantSCMSettings);
        if(scmPrFilter == null) {
            return FeatureResponse.constructIntegrationsAbsentBuilder(sectionOrder, feature).build();
        }

        Double mean = null;
        DbListResponse<DbAggregationResult> dbAggregationResult = null;
        boolean useEs = FeatureHandlerUtil.useEs(company, dbAllowedTenants, devProductivityFilter);
        if(useEs){
            dbAggregationResult = esScmPRsService.groupByAndCalculatePrs(company, scmPrFilter, false, null);
        } else {
            dbAggregationResult = scmAggService.groupByAndCalculatePrs(company, scmPrFilter, null);
        }
        if(dbAggregationResult.getTotalCount()>0) {
            mean = deriveFeatureResult(feature, dbAggregationResult);
        }

        //Do NOT remove Long.valueOf will cause NPE - https://stackoverflow.com/questions/5246776/java-weird-nullpointerexception-in-ternary-operator
        Long value = (mean != null) ? Long.valueOf(Math.round(mean)) : feature.getFeatureType().getDefaultValue(); //Initialize value or Override value

        return FeatureResponse.constructBuilder(sectionOrder, feature, value)
                .count(value)
                .mean(mean)
                .build();
    }

    @Override
    public FeatureBreakDown getBreakDown(String company, DevProductivityProfile.Feature feature, Map<String, Object> profileSettings,
                                         DevProductivityFilter devProductivityFilter, OrgUserDetails orgUserDetails, Map<String, Long> latestIngestedAtByIntegrationId,
                                         TenantSCMSettings tenantSCMSettings, Map<String, SortingOrder> sortBy, Integer pageNumber, Integer pageSize) throws SQLException, IOException {

        ScmPrFilter scmPrFilter = getScmPrFilter(feature, profileSettings, devProductivityFilter, orgUserDetails, tenantSCMSettings);
        List<DbScmPullRequest> scmPrs = List.of();
        long totalCount = 0;

        if(scmPrFilter != null) {
            DbListResponse<DbScmPullRequest> dbListResponse = null;
            boolean useEs = FeatureHandlerUtil.useEs(company, dbAllowedTenants, devProductivityFilter);
            if(useEs){
                dbListResponse = esScmPRsService.list(company, scmPrFilter, sortBy, null, pageNumber, pageSize);
            } else {
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

    private ScmPrFilter getScmPrFilter(DevProductivityProfile.Feature feature, Map<String, Object> profileSettings, DevProductivityFilter devProductivityFilter, OrgUserDetails orgUserDetails, TenantSCMSettings tenantSCMSettings) {

        List<IntegrationUserDetails> userDetails = orgUserDetails.getIntegrationUserDetailsList().stream()
                .filter(integrationUserDetails -> ScmQueryUtils.isScmIntegration(integrationUserDetails.getIntegrationType()))
                .collect(Collectors.toList());

        if(CollectionUtils.isEmpty(userDetails))
            return null;

        Map<String, List<String>> params = MapUtils.emptyIfNull(feature.getParams());
        boolean isApprover = false;
        Integer shallowCommentDensityConfig = tenantSCMSettings.getCommentDensitySmall();
        Integer goodCommentDensityConfig = tenantSCMSettings.getCommentDensityMedium();
        if (params.containsKey("use_approver")) {
            isApprover = parseBooleanParam(params, "use_approver");
        }

        List<String> userRowIds = userDetails.stream()
                .map(IntegrationUserDetails::getIntegrationUserId)
                .map(UUID::toString)
                .collect(Collectors.toList());

        Pair<Map<String, Map<String, String>>, Map<String, Map<String, String>>> partialMatchPair = ScmPRUtils.checkPartialMatchConditions(profileSettings);
        ScmPrFilter.ScmPrFilterBuilder scmPrFilterBuilder = ScmPRUtils.getPrFiltersBuilder(profileSettings);

        return scmPrFilterBuilder
                .prCreatedRange(devProductivityFilter.getTimeRange())
                .integrationIds(userDetails.stream()
                        .map(IntegrationUserDetails::getIntegrationId)
                        .map(String::valueOf)
                        .collect(Collectors.toList()))
                .reviewers(!isApprover ? userRowIds : null)
                .approvers(isApprover ? userRowIds : null)
                .partialMatch(partialMatchPair.getLeft())
                .excludePartialMatch(partialMatchPair.getRight())
                .commentDensitySizeConfig(shallowCommentDensityConfig != null &&
                        goodCommentDensityConfig != null ? Map.of("shallow", String.valueOf(shallowCommentDensityConfig),
                        "good", String.valueOf(goodCommentDensityConfig)) : null)
                .across(ScmPrFilter.DISTINCT.comment_density)
                .build();
    }

    private Double deriveFeatureResult(DevProductivityProfile.Feature feature,
                                       DbListResponse<DbAggregationResult> aggregationResult) {
        double result;
        if (feature.getFeatureType().equals(DevProductivityProfile.FeatureType.PRS_REVIEW_DEPTH)) {
            Optional<Double> totalPrs = getSum(aggregationResult, DbAggregationResult::getCount);
            Optional<Double> totalComments = getSum(aggregationResult, DbAggregationResult::getTotalComments);
            result = totalComments.orElse(0.0) / totalPrs.orElse(0.0);
        } else {
            result = aggregationResult.getRecords().stream()
                    .map(record -> Optional.ofNullable(record.getLinesAddedCount()).orElse(0L)
                            + Optional.ofNullable(record.getLinesChangedCount()).orElse(0L))
                    .mapToLong(n -> n)
                    .average()
                    .orElse(0.0);
        }
        return result;
    }

    private boolean parseBooleanParam(Map<String, List<String>> params, String userRole) {
        return Boolean.parseBoolean(params.get(userRole).get(0));
    }

    private String parseStringParam(Map<String, List<String>> params, String userRole) {
        return params.get(userRole).get(0);
    }

    private Optional<Double> getSum(DbListResponse<DbAggregationResult> aggregationResult,
                                    ToDoubleFunction<DbAggregationResult> mapperFn) {
        return Optional.of(aggregationResult.getRecords()
                .stream()
                .mapToDouble(mapperFn)
                .sum());
    }
}
