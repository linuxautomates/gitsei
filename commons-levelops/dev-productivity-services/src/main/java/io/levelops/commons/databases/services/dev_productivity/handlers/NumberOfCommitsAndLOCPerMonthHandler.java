package io.levelops.commons.databases.services.dev_productivity.handlers;

import com.google.api.client.util.Lists;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.TenantSCMSettings;
import io.levelops.commons.databases.models.database.dev_productivity.*;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.ScmQueryUtils;
import io.levelops.commons.databases.services.dev_productivity.utils.FeatureHandlerUtil;
import io.levelops.commons.databases.services.dev_productivity.utils.ScmCommitUtils;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.MapUtils;
import io.levelops.faceted_search.services.scm_service.EsScmCommitsService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

import static io.levelops.commons.databases.services.dev_productivity.utils.ScmCommitUtils.getCommitFiltersBuilder;

@Log4j2
@Component
public class NumberOfCommitsAndLOCPerMonthHandler implements DevProductivityFeatureHandler {

    private static final int DEFAULT_DAYS = 90;
    private static final String DEFAULT_AGG_INTERVAL = "month";
    private final ScmAggService scmAggService;
    private final EsScmCommitsService esScmCommitsService;
    private final Set<String> dbAllowedTenants;

    @Autowired
    public NumberOfCommitsAndLOCPerMonthHandler(ScmAggService scmAggService, EsScmCommitsService esScmCommitsService,
                                                @Value("${DB_DEV_PROD_SCM:}") List<String> dbAllowedTenants) {
        this.scmAggService = scmAggService;
        this.esScmCommitsService = esScmCommitsService;
        this.dbAllowedTenants = new HashSet<>();
        if (CollectionUtils.isNotEmpty(dbAllowedTenants)) {
            this.dbAllowedTenants.addAll(dbAllowedTenants);
        }
    }

    @Override
    public Set<DevProductivityProfile.FeatureType> getSupportedFeatureTypes() {
        return Set.of(DevProductivityProfile.FeatureType.NUMBER_OF_COMMITS_PER_MONTH,
                DevProductivityProfile.FeatureType.LINES_OF_CODE_PER_MONTH);
    }

    @Override
    public FeatureResponse calculateFeature(String company, final Integer sectionOrder, DevProductivityProfile.Feature feature,
                                            Map<String, Object> profileSettings, DevProductivityFilter devProductivityFilter,
                                            OrgUserDetails orgUserDetails, final Map<String, Long> latestIngestedAtByIntegrationId, final TenantSCMSettings tenantSCMSettings) throws SQLException, IOException {

        Map<String, List<String>> params = MapUtils.emptyIfNull(feature.getParams());
        String aggInterval = (params!= null && params.containsKey("aggInterval")) ? params.get("aggInterval").get(0).toLowerCase() : DEFAULT_AGG_INTERVAL;
        ReportIntervalType interval = devProductivityFilter.getInterval();
        Pair<Long, Long> prCommittedRange = ObjectUtils.firstNonNull(devProductivityFilter.getTimeRange(),interval != null ? interval.getIntervalTimeRange(Instant.now()).getTimeRange() : null);
        if(prCommittedRange == null) {
            long endTime = LocalDate.now().toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC);
            long startTime = LocalDate.now().minusDays(DEFAULT_DAYS).toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC);
            prCommittedRange =  Pair.of(startTime, endTime);
        }

        ScmCommitFilter scmCommitFilter = getScmCommitFilter(feature, prCommittedRange, orgUserDetails, profileSettings);
        if(scmCommitFilter == null) {
            return FeatureResponse.constructIntegrationsAbsentBuilder(sectionOrder, feature).build();
        }

        DbListResponse<DbAggregationResult> aggregationResult = null;
        Double mean = null;
        Long totalCount = null;
        boolean useEs = FeatureHandlerUtil.useEs(company, dbAllowedTenants, devProductivityFilter);
        if(useEs){
            aggregationResult = esScmCommitsService.groupByAndCalculateCommits(company, scmCommitFilter, false,null, 0, 10000);
        }else {
            aggregationResult = scmAggService.groupByAndCalculateCommits(company, scmCommitFilter, null);
        }
        if(aggregationResult.getTotalCount()>0) {

            if (feature.getFeatureType().equals(DevProductivityProfile.FeatureType.NUMBER_OF_COMMITS_PER_MONTH)) {
                totalCount = aggregationResult.getRecords().stream()
                        .map(DbAggregationResult::getCount)
                        .mapToLong(n -> n).sum();
            } else {
                totalCount = aggregationResult.getRecords().stream()
                        .map(record -> Optional.ofNullable(record.getLinesAddedCount()).orElse(0L)
                                + Optional.ofNullable(record.getLinesChangedCount()).orElse(0L))
                        .mapToLong(n -> n)
                        .sum();
            }
            Instant lowerBound = DateUtils.fromEpochSecond(prCommittedRange.getLeft());
            Instant upperBound = DateUtils.fromEpochSecond(prCommittedRange.getRight());
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
    public FeatureBreakDown getBreakDown(String company, DevProductivityProfile.Feature feature, Map<String, Object> profileSettings,
                                         DevProductivityFilter devProductivityFilter, OrgUserDetails orgUserDetails, Map<String, Long> latestIngestedAtByIntegrationId,
                                         TenantSCMSettings tenantSCMSettings, Map<String, SortingOrder> sortBy, Integer pageNumber, Integer pageSize) throws SQLException, IOException {

        ScmCommitFilter scmCommitFilter = getScmCommitFilter(feature, devProductivityFilter.getTimeRange(), orgUserDetails, profileSettings);
        List<DbScmCommit> scmCommits = Lists.newArrayList();
        long totalCount = 0;
        if(scmCommitFilter != null) {
            DbListResponse<DbScmCommit> dbListResponse = null;
            boolean useEs = FeatureHandlerUtil.useEs(company, dbAllowedTenants, devProductivityFilter);
            if(useEs){
                dbListResponse = esScmCommitsService.listCommits(company, scmCommitFilter, sortBy, null, pageNumber, pageSize);
            } else {
                dbListResponse = scmAggService.listCommits(company, scmCommitFilter, sortBy, null, pageNumber, pageSize);
            }
            scmCommits = dbListResponse.getRecords();
            totalCount = dbListResponse.getTotalCount();
        }

        return FeatureBreakDown.builder()
                .orgUserId(orgUserDetails.getOrgUserId())
                .email(orgUserDetails.getEmail())
                .fullName(orgUserDetails.getFullName())
                .name(feature.getName())
                .description(feature.getDescription())
                .breakDownType(BreakDownType.SCM_COMMITS)
                .records(scmCommits)
                .count(totalCount)
                .build();
    }

    private ScmCommitFilter getScmCommitFilter(DevProductivityProfile.Feature feature, Pair<Long, Long> prCommittedRange, OrgUserDetails orgUserDetails, Map<String, Object> profileSettings) {

        List<IntegrationUserDetails> userDetails = orgUserDetails.getIntegrationUserDetailsList().stream()
                .filter(integrationUserDetails -> ScmQueryUtils.isScmIntegration(integrationUserDetails.getIntegrationType()))
                .collect(Collectors.toList());

        if(CollectionUtils.isEmpty(userDetails))
            return null;

        Map<String, List<String>> params = MapUtils.emptyIfNull(feature.getParams());
        boolean useCommitter = false;
        boolean useAuthor = true;
        if (params.containsKey("use_committer")) {
            useCommitter = parseBooleanParam(params, "use_committer");
        }
        if (params.containsKey("use_author")) {
            useAuthor = parseBooleanParam(params, "use_author");
        }

        List<String> userRowIds = userDetails.stream()
                .map(IntegrationUserDetails::getIntegrationUserId)
                .map(UUID::toString)
                .collect(Collectors.toList());
        ScmCommitFilter.ScmCommitFilterBuilder commitFiltersBuilder = getCommitFiltersBuilder(profileSettings);
        Pair<Map<String, Map<String, String>>, Map<String, Map<String, String>>> partialMatchPair = ScmCommitUtils.checkPartialMatchConditions(profileSettings);
        return commitFiltersBuilder
                .integrationIds(userDetails.stream()
                        .map(IntegrationUserDetails::getIntegrationId)
                        .map(String::valueOf)
                        .collect(Collectors.toList()))
                .committers(useCommitter ? userRowIds : null)
                .authors(useAuthor ? userRowIds : null)
                .across(ScmCommitFilter.DISTINCT.trend)
                .aggInterval(AGG_INTERVAL.month)
                .calculation(ScmCommitFilter.CALCULATION.commit_count_only)
                .committedAtRange(ImmutablePair.of(prCommittedRange))
                .ignoreFilesJoin(true)
                .partialMatch(partialMatchPair.getLeft())
                .excludePartialMatch(partialMatchPair.getRight())
                .build();
    }

    private boolean parseBooleanParam(Map<String, List<String>> params, String userRole) {
        return Boolean.parseBoolean(params.get(userRole).get(0));
    }
}
