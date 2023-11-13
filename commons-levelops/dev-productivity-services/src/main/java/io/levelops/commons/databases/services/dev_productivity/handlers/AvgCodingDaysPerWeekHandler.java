package io.levelops.commons.databases.services.dev_productivity.handlers;

import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.TenantSCMSettings;
import io.levelops.commons.databases.models.database.dev_productivity.BreakDownType;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureBreakDown;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureResponse;
import io.levelops.commons.databases.models.database.dev_productivity.IntegrationUserDetails;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.ScmQueryUtils;
import io.levelops.commons.databases.services.dev_productivity.utils.FeatureHandlerUtil;
import io.levelops.commons.databases.services.dev_productivity.utils.MeanUtils;
import io.levelops.commons.databases.services.dev_productivity.utils.ScmCommitUtils;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.faceted_search.services.scm_service.EsScmCommitsService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.dev_productivity.utils.ScmCommitUtils.getCommitFiltersBuilder;

@Log4j2
@Component
public class AvgCodingDaysPerWeekHandler implements DevProductivityFeatureHandler {

    private final ScmAggService scmAggService;
    private EsScmCommitsService esScmCommitsService;
    private static final String DEFAULT_AGG_INTERVAL = "week";
    private final Set<String> dbAllowedTenants;

    @Override
    public Set<DevProductivityProfile.FeatureType> getSupportedFeatureTypes() {
        return Set.of(DevProductivityProfile.FeatureType.AVG_CODING_DAYS_PER_WEEK);
    }

    @Autowired
    public AvgCodingDaysPerWeekHandler(ScmAggService scmAggService, EsScmCommitsService esScmCommitsService,
                                       @Value("${DB_DEV_PROD_SCM:}") List<String> dbAllowedTenants) {
        this.scmAggService = scmAggService;
        this.esScmCommitsService = esScmCommitsService;
        this.dbAllowedTenants = new HashSet<>();
        if (CollectionUtils.isNotEmpty(dbAllowedTenants)) {
            this.dbAllowedTenants.addAll(dbAllowedTenants);
        }
    }

    @Override
    public FeatureResponse calculateFeature(String company, final Integer sectionOrder, DevProductivityProfile.Feature feature,
                                            Map<String, Object> profileSettings, DevProductivityFilter devProductivityFilter,
                                            OrgUserDetails orgUserDetails, final Map<String, Long> latestIngestedAtByIntegrationId, final TenantSCMSettings tenantSCMSettings) throws SQLException, IOException {

        ScmCommitFilter scmCommitFilter = getScmCommitFilter(orgUserDetails, devProductivityFilter, profileSettings);
        if (scmCommitFilter == null) {
            return FeatureResponse.constructIntegrationsAbsentBuilder(sectionOrder, feature).build();
        }

        DbListResponse<DbAggregationResult> aggregationResult = null;
        Double mean = null;
        //changed as part of LEV-4516
       boolean useEs = FeatureHandlerUtil.useEs(company, dbAllowedTenants, devProductivityFilter);
       if(useEs) {
           aggregationResult = esScmCommitsService.groupByAndCalculateCodingDays(company, scmCommitFilter, null);
       }else{
           aggregationResult = scmAggService.groupByAndCalculateCodingDays(company, scmCommitFilter, null);
       }

        if (aggregationResult.getTotalCount() > 0) {
            mean = deriveFeatureResult(aggregationResult);
        }

        // we need to convert to second first, then we round the value up to a long
        if(mean != null){
            mean = Double.valueOf(String.format("%.2f",mean));
        }
        Double meanSecondsDouble = MeanUtils.convertMeanDaysToSecondsOrDefault(feature, mean);
        Long meanSecondsLong =  MeanUtils.roundMeanDoubleUpToLong(meanSecondsDouble);
        Pair<Long,Long> committedAtRange = scmCommitFilter.getCommittedAtRange();
        Instant lowerBound = DateUtils.fromEpochSecond(committedAtRange.getLeft());
        Instant upperBound = DateUtils.fromEpochSecond(committedAtRange.getRight());
        long numDays = Duration.between(lowerBound,upperBound).toDays();
        long numWeeks = (numDays >= 0 && numDays <= 7) ? 1 : numDays / 7;
        Long count = meanSecondsLong * numWeeks;
        return FeatureResponse.constructBuilder(sectionOrder, feature, meanSecondsLong)
                .mean(meanSecondsDouble)
                .count(count)
                .build();
    }

    @Override
    public FeatureBreakDown getBreakDown(String company, DevProductivityProfile.Feature feature, Map<String, Object> profileSettings, DevProductivityFilter devProductivityFilter, OrgUserDetails orgUserDetails, Map<String, Long> latestIngestedAtByIntegrationId, TenantSCMSettings tenantSCMSettings, Map<String, SortingOrder> sortBy, Integer pageNumber, Integer pageSize) throws SQLException, IOException {

        ScmCommitFilter scmCommitFilter = getScmCommitFilter(orgUserDetails, devProductivityFilter, profileSettings);
        List<DbScmCommit> scmCommits = List.of();
        long totalCount = 0;
        if (scmCommitFilter != null) {
            DbListResponse<DbScmCommit> dbListResponse = null;
            boolean useEs = FeatureHandlerUtil.useEs(company, dbAllowedTenants, devProductivityFilter);
            if(useEs){
                dbListResponse = esScmCommitsService.listCommits(company, scmCommitFilter, sortBy, null, pageNumber, pageSize);
            }else {
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

    private ScmCommitFilter getScmCommitFilter(OrgUserDetails orgUserDetails, DevProductivityFilter devProductivityFilter, Map<String, Object> profileSettings) {
        ImmutablePair<Long, Long> committedRange;
        if (devProductivityFilter.getTimeRange() != null) {
            committedRange = ImmutablePair.of(devProductivityFilter.getTimeRange().getLeft(),
                    devProductivityFilter.getTimeRange().getRight());
        } else {
            long start = Instant.now().minus(90, ChronoUnit.DAYS).toEpochMilli();
            committedRange = ImmutablePair.of(start, Instant.now().toEpochMilli());
        }


        List<IntegrationUserDetails> userDetails = orgUserDetails.getIntegrationUserDetailsList().stream()
                .filter(i -> ScmQueryUtils.isScmIntegration(i.getIntegrationType()))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(userDetails)) {
            return null;
        }

        List<String> integrationUserIds = userDetails.stream()
                .map(IntegrationUserDetails::getIntegrationUserId)
                .map(UUID::toString)
                .collect(Collectors.toList());

        boolean useAuthor = true;
        boolean useCommitter = false;

        ScmCommitFilter.ScmCommitFilterBuilder commitFiltersBuilder = getCommitFiltersBuilder(profileSettings);
        Pair<Map<String, Map<String, String>>, Map<String, Map<String, String>>> partialMatchPair = ScmCommitUtils.checkPartialMatchConditions(profileSettings);
        return commitFiltersBuilder
                .committers(useCommitter ? integrationUserIds : null)
                .authors(useAuthor ? integrationUserIds : null)
                .integrationIds(userDetails.stream()
                        .map(IntegrationUserDetails::getIntegrationId)
                        .map(String::valueOf)
                        .collect(Collectors.toList()))
                .committedAtRange(committedRange)
                .calculation(ScmCommitFilter.CALCULATION.commit_days)
                .aggInterval(AGG_INTERVAL.week)
                .across(ScmCommitFilter.DISTINCT.trend)
                .ignoreFilesJoin(true)
                .partialMatch(partialMatchPair.getLeft())
                .excludePartialMatch(partialMatchPair.getRight())
                .build();
    }

    private Double deriveFeatureResult(DbListResponse<DbAggregationResult> aggregationResult) {
        return aggregationResult.getRecords().stream()
                .mapToDouble(DbAggregationResult::getMean)
                .sum();
    }
}
