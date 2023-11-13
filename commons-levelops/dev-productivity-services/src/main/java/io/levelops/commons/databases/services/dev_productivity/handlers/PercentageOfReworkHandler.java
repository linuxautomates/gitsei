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
import io.levelops.commons.databases.services.dev_productivity.utils.ScmCommitUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.MapUtils;
import io.levelops.faceted_search.services.scm_service.EsScmCommitsService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
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
public class PercentageOfReworkHandler implements DevProductivityFeatureHandler {

    private final ScmAggService scmAggService;
    private final EsScmCommitsService esScmCommitsService;
    private final Set<String> dbAllowedTenants;

    @Autowired
    public PercentageOfReworkHandler(ScmAggService scmAggService, EsScmCommitsService esScmCommitsService,
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
        return Set.of(DevProductivityProfile.FeatureType.PERCENTAGE_OF_REWORK,
                DevProductivityProfile.FeatureType.PERCENTAGE_OF_LEGACY_REWORK);
    }

    @Override
    public FeatureResponse calculateFeature(String company,
                                            final Integer sectionOrder,
                                            DevProductivityProfile.Feature feature,
                                            Map<String, Object> profileSettings,
                                            DevProductivityFilter devProductivityFilter,
                                            OrgUserDetails orgUserDetails,
                                            final Map<String, Long> latestIngestedAtByIntegrationId,
                                            final TenantSCMSettings tenantSCMSettings) throws SQLException, IOException {

        ScmCommitFilter scmCommitFilter = getScmCommitFilter(feature, devProductivityFilter, orgUserDetails, tenantSCMSettings, profileSettings);
        if(scmCommitFilter == null) {
            return FeatureResponse.constructIntegrationsAbsentBuilder(sectionOrder, feature).build();
        }

        DbListResponse<DbAggregationResult> aggregationResult = null;
        Double mean = null;
        boolean useEs = FeatureHandlerUtil.useEs(company, dbAllowedTenants, devProductivityFilter);
        if(useEs){
            aggregationResult = esScmCommitsService.groupByAndCalculateCommits(company, scmCommitFilter, false, null, 0, 1000);
        } else {
            aggregationResult = scmAggService.groupByAndCalculateCommits(company, scmCommitFilter, null);
        }
        if(aggregationResult.getTotalCount()>0) {
            mean = deriveFeatureResult(feature, aggregationResult);
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

        ScmCommitFilter scmCommitFilter = getScmCommitFilter(feature, devProductivityFilter, orgUserDetails, tenantSCMSettings, profileSettings);
        List<DbScmCommit> scmCommits = List.of();
        long totalCount = 0;

        if(scmCommitFilter != null) {
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

    private ScmCommitFilter getScmCommitFilter(DevProductivityProfile.Feature feature, DevProductivityFilter devProductivityFilter, OrgUserDetails orgUserDetails, TenantSCMSettings tenantSCMSettings, Map<String, Object> profileSettings) {

        List<IntegrationUserDetails> userDetails = orgUserDetails.getIntegrationUserDetailsList().stream()
                .filter(integrationUserDetails -> ScmQueryUtils.isScmIntegration(integrationUserDetails.getIntegrationType()))
                .collect(Collectors.toList());

        if(CollectionUtils.isEmpty(userDetails))
            return null;

        Map<String, List<String>> params = MapUtils.emptyIfNull(feature.getParams());
        boolean useCommitter = false;
        boolean useAuthor = true;
        Long legacyUpdateIntervalConfig = tenantSCMSettings.getLegacyUpdateIntervalConfig();
        if (legacyUpdateIntervalConfig == null) {
            legacyUpdateIntervalConfig = Instant.now().minus(30, ChronoUnit.DAYS).getEpochSecond();
        }
        if (params.containsKey("use_committer")) {
            useCommitter = parseBooleanParam(params, "use_committer");
        }
        if (params.containsKey("use_author")) {
            useAuthor = parseBooleanParam(params, "use_author");
            useCommitter = false;
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
                .across(useCommitter ? ScmCommitFilter.DISTINCT.committer : ScmCommitFilter.DISTINCT.author)
                .aggInterval(AGG_INTERVAL.month)
                .calculation(ScmCommitFilter.CALCULATION.count)
                .committedAtRange(devProductivityFilter.getTimeRange())
                .legacyCodeConfig(legacyUpdateIntervalConfig)
                .partialMatch(partialMatchPair.getLeft())
                .excludePartialMatch(partialMatchPair.getRight())
                .build();
    }

    private Double deriveFeatureResult(DevProductivityProfile.Feature feature,
                                       DbListResponse<DbAggregationResult> aggregationResult) {
        Double result = null;
        if (feature.getFeatureType().equals(DevProductivityProfile.FeatureType.PERCENTAGE_OF_REWORK)) {
            result = aggregationResult.getRecords().stream()
                    .map(DbAggregationResult::getPctRefactoredLines)
                    .mapToDouble(n -> n)
                    .average()
                    .orElse(0.0);
        } else {
            result = aggregationResult.getRecords().stream()
                    .map(DbAggregationResult::getPctLegacyRefactoredLines)
                    .mapToDouble(n -> n)
                    .average()
                    .orElse(0.0);
        }
        return result;
    }

    private boolean parseBooleanParam(Map<String, List<String>> params, String userRole) {
        return Boolean.parseBoolean(params.get(userRole).get(0));
    }
}
