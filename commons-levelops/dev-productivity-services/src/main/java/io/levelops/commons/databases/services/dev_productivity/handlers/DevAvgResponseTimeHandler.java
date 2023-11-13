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
import io.levelops.commons.utils.MapUtils;
import io.levelops.faceted_search.services.scm_service.EsScmPRsService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Component
public class DevAvgResponseTimeHandler implements DevProductivityFeatureHandler {

    private final ScmAggService scmAggService;
    private final EsScmPRsService esScmPRsService;
    private final Set<String> dbAllowedTenants;

    @Autowired
    public DevAvgResponseTimeHandler(ScmAggService scmAggService, EsScmPRsService esScmPRsService,
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
        return Set.of(DevProductivityProfile.FeatureType.PRS_AVG_APPROVAL_TIME,
                DevProductivityProfile.FeatureType.PRS_AVG_COMMENT_TIME);
    }

    @Override
    public FeatureResponse calculateFeature(String company, final Integer sectionOrder, DevProductivityProfile.Feature feature,
                                            Map<String, Object> profileSettings, DevProductivityFilter devProductivityFilter,
                                            OrgUserDetails orgUserDetails, final Map<String, Long> latestIngestedAtByIntegrationId, final TenantSCMSettings tenantSCMSettings) throws SQLException, IOException {

        ScmPrFilter scmPrFilter = getScmPrFilter(feature, profileSettings, devProductivityFilter, orgUserDetails);
        if(scmPrFilter == null) {
            return FeatureResponse.constructIntegrationsAbsentBuilder(sectionOrder, feature).build();
        }

        DbListResponse<DbAggregationResult> dbAggregationResult = null;
        Double mean = null;
        boolean useEs = FeatureHandlerUtil.useEs(company, dbAllowedTenants, devProductivityFilter);
        if(useEs) {
            dbAggregationResult = esScmPRsService.groupByAndCalculatePrs(company, scmPrFilter, false, null);
        }else{
            dbAggregationResult = scmAggService.groupByAndCalculatePrs(company, scmPrFilter, null);
        }
        if(dbAggregationResult.getTotalCount()>0) {
            mean = deriveFeatureResult(feature, dbAggregationResult, orgUserDetails);
        }

        //Do NOT remove Long.valueOf will cause NPE - https://stackoverflow.com/questions/5246776/java-weird-nullpointerexception-in-ternary-operator
        Long value = (mean != null) ? Long.valueOf(Math.round(mean)) : feature.getFeatureType().getDefaultValue(); //Initialize value or Override value

        return FeatureResponse.constructBuilder(sectionOrder, feature, value)
                .mean(mean)
                .count(value)
                .build();
    }

    @Override
    public FeatureBreakDown getBreakDown(String company, DevProductivityProfile.Feature feature, Map<String, Object> profileSettings,
                                         DevProductivityFilter devProductivityFilter, OrgUserDetails orgUserDetails, Map<String, Long> latestIngestedAtByIntegrationId,
                                         TenantSCMSettings tenantSCMSettings, Map<String, SortingOrder> sortBy, Integer pageNumber, Integer pageSize) throws SQLException, IOException {

        ScmPrFilter scmPrFilter = getScmPrFilter(feature, profileSettings, devProductivityFilter, orgUserDetails);
        List<DbScmPullRequest> scmPrs = List.of();
        long totalCount = 0;

        if(scmPrFilter != null) {
            DbListResponse<DbScmPullRequest> dbListResponse = null;
            boolean useEs = FeatureHandlerUtil.useEs(company, dbAllowedTenants, devProductivityFilter);
            if(useEs){
                dbListResponse =  esScmPRsService.list(company, scmPrFilter, sortBy, null, pageNumber, pageSize);
            }else{
                dbListResponse =  scmAggService.list(company, scmPrFilter, sortBy, null, pageNumber, pageSize);
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

    private ScmPrFilter getScmPrFilter(DevProductivityProfile.Feature feature, Map<String, Object> profileSettings, DevProductivityFilter devProductivityFilter, OrgUserDetails orgUserDetails) {

        List<String> userRowIds = getScmIntegrationUsers(orgUserDetails);

        if(CollectionUtils.isEmpty(userRowIds))
            return null;

        Map<String, List<String>> params = MapUtils.emptyIfNull(feature.getParams());
        boolean useAuthor = false;
        boolean useCommentor = false;
        boolean useApprover = true;
        ScmPrFilter.CALCULATION calculation = ScmPrFilter.CALCULATION.reviewer_approve_time;

        if (params.containsKey("use_author")) {
            useAuthor = parseBooleanParam(params, "use_author");
            if(useAuthor) {
                calculation = ScmPrFilter.CALCULATION.author_response_time;
                useApprover = false;
            }
        }

        if(feature.getFeatureType().equals(DevProductivityProfile.FeatureType.PRS_AVG_COMMENT_TIME)){
            useApprover = false;
            useCommentor = true;
            calculation = ScmPrFilter.CALCULATION.reviewer_comment_time;
        }

        List<IntegrationUserDetails> userDetails = orgUserDetails.getIntegrationUserDetailsList().stream()
                .filter(integrationUserDetails -> ScmQueryUtils.isScmIntegration(integrationUserDetails.getIntegrationType()))
                .collect(Collectors.toList());

        Pair<Map<String, Map<String, String>>, Map<String, Map<String, String>>> partialMatchPair = ScmPRUtils.checkPartialMatchConditions(profileSettings);
        ScmPrFilter.ScmPrFilterBuilder scmPrFilterBuilder = ScmPRUtils.getPrFiltersBuilder(profileSettings);

        return scmPrFilterBuilder
                .integrationIds(userDetails.stream()
                        .map(IntegrationUserDetails::getIntegrationId)
                        .map(String::valueOf)
                        .collect(Collectors.toList()))
                .creators(useAuthor ? userRowIds : null)
                .across(useAuthor ? ScmPrFilter.DISTINCT.creator : ScmPrFilter.DISTINCT.reviewer)
                .approvers(useApprover ? userRowIds : null)
                .commenters( useCommentor ? userRowIds : null)
                .partialMatch(partialMatchPair.getLeft())
                .excludePartialMatch(partialMatchPair.getRight())
                .calculation(calculation)
                .prMergedRange(devProductivityFilter.getTimeRange())
                .build();
    }

    private List<String> getScmIntegrationUsers(OrgUserDetails orgUserDetails) {

        List<IntegrationUserDetails> userDetails = orgUserDetails.getIntegrationUserDetailsList().stream()
                .filter(integrationUserDetails -> ScmQueryUtils.isScmIntegration(integrationUserDetails.getIntegrationType()))
                .collect(Collectors.toList());

        if(CollectionUtils.isEmpty(userDetails)) {
            return null;
        }

        return userDetails.stream()
                .map(IntegrationUserDetails::getIntegrationUserId)
                .map(UUID::toString)
                .collect(Collectors.toList());
    }

    private Double deriveFeatureResult(DevProductivityProfile.Feature feature,
                                       DbListResponse<DbAggregationResult> aggregationResult, OrgUserDetails orgUserDetails) {
        Double result = null;
        List<String> integrationUserids = getScmIntegrationUsers(orgUserDetails);

        result = aggregationResult.getRecords().stream()
                .filter( rs -> integrationUserids.contains(rs.getKey()) || integrationUserids.contains(rs.getAdditionalKey()))
                .mapToDouble(DbAggregationResult::getMean)
                .average()
                .orElse(0.0);

        return result;
    }

    private boolean parseBooleanParam(Map<String, List<String>> params, String userRole) {
        return Boolean.parseBoolean(params.get(userRole).get(0));
    }
}
