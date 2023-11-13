package io.levelops.commons.databases.services.dev_productivity.handlers;

import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.TenantSCMSettings;
import io.levelops.commons.databases.models.database.dev_productivity.*;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.ScmQueryUtils;
import io.levelops.commons.databases.services.dev_productivity.utils.FeatureHandlerUtil;
import io.levelops.commons.databases.services.dev_productivity.utils.ScmPRUtils;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.MapUtils;
import io.levelops.faceted_search.services.scm_service.EsScmPRsService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;

import java.util.stream.Collectors;

@Log4j2
@Component
public class NumberOfPRsPerMonthHandler implements DevProductivityFeatureHandler {

    private static final int DEFAULT_DAYS = 90;
    private static final String DEFAULT_AGG_INTERVAL = "month";

    private final ScmAggService scmAggService;
    private final EsScmPRsService esScmPRsService;
    private final Set<String> dbAllowedTenants;

    @Autowired
    public NumberOfPRsPerMonthHandler(ScmAggService scmAggService, EsScmPRsService esScmPRsService,
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
        return Set.of(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_PER_MONTH,
                DevProductivityProfile.FeatureType.NUMBER_OF_PRS_APPROVED_PER_MONTH,
                DevProductivityProfile.FeatureType.NUMBER_OF_PRS_COMMENTED_ON_PER_MONTH);
    }

    @Override
    public FeatureResponse calculateFeature(String company, final Integer sectionOrder, DevProductivityProfile.Feature feature,
                                            Map<String, Object> profileSettings, DevProductivityFilter devProductivityFilter,
                                            OrgUserDetails orgUserDetails, final Map<String, Long> latestIngestedAtByIntegrationId, final TenantSCMSettings tenantSCMSettings) throws SQLException, IOException {
        Map<String, List<String>> params = MapUtils.emptyIfNull(feature.getParams());
        ReportIntervalType interval = devProductivityFilter.getInterval();
        String aggInterval = (params!= null && params.containsKey("aggInterval")) ? params.get("aggInterval").get(0).toLowerCase() : DEFAULT_AGG_INTERVAL;
        Pair<Long, Long> prMergedRange = ObjectUtils.firstNonNull(devProductivityFilter.getTimeRange(),interval != null ? interval.getIntervalTimeRange(Instant.now()).getTimeRange() : null);
        if(prMergedRange == null) {
            long endTime = LocalDate.now().toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC);
            long startTime = LocalDate.now().minusDays(DEFAULT_DAYS).toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC);
            prMergedRange =  Pair.of(startTime, endTime);
        }

        ScmPrFilter scmPrFilter = getScmPrFilter(feature, profileSettings, prMergedRange, orgUserDetails);
        if(scmPrFilter == null) {
            return FeatureResponse.constructIntegrationsAbsentBuilder(sectionOrder, feature).build();
        }

        DbListResponse<DbAggregationResult> aggregationResult = null;
        Double mean = null;
        Long totalCount = null;
        boolean useEs = FeatureHandlerUtil.useEs(company, dbAllowedTenants, devProductivityFilter);
        if(useEs){
            aggregationResult = esScmPRsService.groupByAndCalculatePrs(company, scmPrFilter, false, null);
        } else {
            aggregationResult = scmAggService.groupByAndCalculatePrs(company, scmPrFilter, null);
        }

        if(aggregationResult.getTotalCount()>0) {
            totalCount = aggregationResult.getRecords().stream()
                    .map(DbAggregationResult::getCount)
                    .mapToLong(Long::longValue)
                    .sum();
            Instant lowerBound = DateUtils.fromEpochSecond(prMergedRange.getLeft());
            Instant upperBound = DateUtils.fromEpochSecond(prMergedRange.getRight());
            List<ImmutablePair<Long, Long>> timePartition = FeatureHandlerUtil.getTimePartitionByInterval(aggInterval,lowerBound,upperBound);
            mean = new BigDecimal((totalCount*1.0)/timePartition.size()).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        //Do NOT remove Long.valueOf will cause NPE - https://stackoverflow.com/questions/5246776/java-weird-nullpointerexception-in-ternary-operator
        Long value = (mean != null) ? Long.valueOf(Math.round(mean)) : feature.getFeatureType().getDefaultValue(); //Initialize value or Override value

        return FeatureResponse.constructBuilder(sectionOrder, feature, value, interval)
                .count(totalCount)
                .mean(mean)
                .build();
    }

    @Override
    public FeatureBreakDown getBreakDown(String company, DevProductivityProfile.Feature feature, Map<String, Object> profileSettings, DevProductivityFilter devProductivityFilter, OrgUserDetails orgUserDetails, Map<String, Long> latestIngestedAtByIntegrationId, TenantSCMSettings tenantSCMSettings, Map<String, SortingOrder> sortBy, Integer pageNumber, Integer pageSize) throws SQLException, IOException {

        ScmPrFilter scmPrFilter = getScmPrFilter(feature, profileSettings, devProductivityFilter.getTimeRange(), orgUserDetails);
        List<DbScmPullRequest> scmPrs = List.of();
        long totalCount = 0;

        if(scmPrFilter !=null) {
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

    private ScmPrFilter getScmPrFilter(DevProductivityProfile.Feature feature, Map<String, Object> settings, Pair<Long, Long> prMergedRange, OrgUserDetails orgUserDetails) {

        List<IntegrationUserDetails> userDetails = orgUserDetails.getIntegrationUserDetailsList().stream()
                .filter(integrationUserDetails -> ScmQueryUtils.isScmIntegration(integrationUserDetails.getIntegrationType()))
                .collect(Collectors.toList());

        if(CollectionUtils.isEmpty(userDetails))
            return null;

        Map<String, List<String>> params = MapUtils.emptyIfNull(feature.getParams());
        boolean useCreator = true;
        boolean useAssignee = false;
        boolean useReviewer = false;
        boolean useApprover = false;
        boolean useCommenter = false;
        String interval = "month";
        if (params.containsKey("use_creator")) {
            useCreator = parseBooleanParam(params, "use_creator");
        }
        if (params.containsKey("use_assignee")) {
            useAssignee = parseBooleanParam(params, "use_assignee");
        }
        if (params.containsKey("use_reviewer")) {
            useReviewer = parseBooleanParam(params, "use_reviewer");
        }
        if (params.containsKey("use_approver")) {
            useApprover = parseBooleanParam(params, "use_approver");
        }
        if (params.containsKey("use_commenter")) {
            useCommenter = parseBooleanParam(params, "use_commenter");
        }
        if (params.containsKey("aggInterval")) {
            interval = params.get("aggInterval").get(0);
            if(EnumUtils.isValidEnumIgnoreCase(AGG_INTERVAL.class,interval))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, " Invalid interval provided, please provide valid interval ");
        }

        if(feature.getFeatureType().equals(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_COMMENTED_ON_PER_MONTH)) {
            useCommenter = true;
            useCreator = false;
        }else if(feature.getFeatureType().equals(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_APPROVED_PER_MONTH)) {
            useApprover = true;
            useCreator = false;
        }

        List<String> userRowIds = userDetails.stream()
                .map(IntegrationUserDetails::getIntegrationUserId)
                .map(UUID::toString)
                .collect(Collectors.toList());
        List<String> userCloudIds = userDetails.stream()
                .map(IntegrationUserDetails::getCloudId).collect(Collectors.toList());

        Pair<Map<String, Map<String, String>>, Map<String, Map<String, String>>> partialMatchPair = ScmPRUtils.checkPartialMatchConditions(settings);
        ScmPrFilter.ScmPrFilterBuilder scmPrFilterBuilder = ScmPRUtils.getPrFiltersBuilder(settings);

        return scmPrFilterBuilder
                .integrationIds(userDetails.stream()
                        .map(IntegrationUserDetails::getIntegrationId)
                        .map(String::valueOf)
                        .collect(Collectors.toList()))
                .creators(useCreator ? userRowIds : null)
                .assignees(useAssignee ? userRowIds : null)
                .reviewers(useReviewer ? userRowIds : null)
                .approvers(useApprover ? userRowIds : null)
                .commenters(useCommenter ? userRowIds : null)
                .partialMatch(partialMatchPair.getLeft())
                .excludePartialMatch(partialMatchPair.getRight())
                .across(ScmPrFilter.DISTINCT.pr_merged)
                .aggInterval(AGG_INTERVAL.fromString(interval))
                .calculation(ScmPrFilter.CALCULATION.count)
                .prMergedRange(ImmutablePair.of(prMergedRange))
                .build();
    }

    private boolean parseBooleanParam(Map<String, List<String>> params, String userRole) {
        return Boolean.parseBoolean(params.get(userRole).get(0));
    }
}
