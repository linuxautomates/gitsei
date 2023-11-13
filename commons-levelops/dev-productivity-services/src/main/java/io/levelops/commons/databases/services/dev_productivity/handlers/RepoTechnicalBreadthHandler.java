package io.levelops.commons.databases.services.dev_productivity.handlers;

import com.google.api.client.util.Sets;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.TenantSCMSettings;
import io.levelops.commons.databases.models.database.dev_productivity.*;
import io.levelops.commons.databases.models.database.scm.DbScmContributorAgg;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.models.filters.ScmContributorsFilter;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.ScmQueryUtils;
import io.levelops.commons.databases.services.dev_productivity.utils.FeatureHandlerUtil;
import io.levelops.commons.databases.services.dev_productivity.utils.ScmCommitUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.MapUtils;
import io.levelops.faceted_search.services.scm_service.EsScmCommitsService;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.dev_productivity.utils.ScmCommitUtils.getRangeImmutablePair;

@Log4j2
@Component
public class RepoTechnicalBreadthHandler implements DevProductivityFeatureHandler {

    private final ScmAggService scmAggService;
    private final EsScmCommitsService esScmCommitsService;
    private final Set<String> dbAllowedTenants;

    @Autowired
    public RepoTechnicalBreadthHandler(ScmAggService scmAggService, EsScmCommitsService esScmCommitsService,
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
        return Set.of(DevProductivityProfile.FeatureType.REPO_BREADTH,
                DevProductivityProfile.FeatureType.TECHNICAL_BREADTH);
    }

    @Override
    public FeatureResponse calculateFeature(String company, final Integer sectionOrder, DevProductivityProfile.Feature feature,
                                            Map<String, Object> profileSettings, DevProductivityFilter devProductivityFilter,
                                            OrgUserDetails orgUserDetails, final Map<String, Long> latestIngestedAtByIntegrationId, final TenantSCMSettings tenantSCMSettings) throws SQLException, IOException {

        ScmContributorsFilter scmContributorsFilter = getScmContributionFilter(feature, devProductivityFilter, orgUserDetails, profileSettings);
        if(scmContributorsFilter == null) {
            return FeatureResponse.constructIntegrationsAbsentBuilder(sectionOrder, feature).build();
        }
        DbListResponse<DbScmContributorAgg> resultList = null;
        Double mean = null;
        boolean useEs = FeatureHandlerUtil.useEs(company, dbAllowedTenants, devProductivityFilter);
       if(useEs){
           resultList = esScmCommitsService.list(company, scmContributorsFilter, Map.of(), null, 0, 100);
       } else {
           resultList = scmAggService.list(company, scmContributorsFilter, Map.of(), null, 0, 100);
       }
        if(resultList.getTotalCount()>0) {
            mean = deriveFeatureResult(feature, resultList);
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

       ScmContributorsFilter scmContributorsFilter = getScmContributionFilter(feature, devProductivityFilter, orgUserDetails, profileSettings);
       List<DbScmContributorAgg> scmContributorAggs = List.of();
       long totalCount = 0;

       if(scmContributorsFilter != null) {
           DbListResponse<DbScmContributorAgg> dbListResponse = null;
           boolean useEs = FeatureHandlerUtil.useEs(company, dbAllowedTenants, devProductivityFilter);
           if (useEs){
               dbListResponse = esScmCommitsService.list(company, scmContributorsFilter, sortBy, null, pageNumber, pageSize);
           }else {
               dbListResponse = scmAggService.list(company, scmContributorsFilter, sortBy, null, pageNumber, pageSize);
           }
           scmContributorAggs = dbListResponse.getRecords();
           totalCount = dbListResponse.getTotalCount();
       }

       return FeatureBreakDown.builder()
                .orgUserId(orgUserDetails.getOrgUserId())
                .email(orgUserDetails.getEmail())
                .fullName(orgUserDetails.getFullName())
                .name(feature.getName())
                .description(feature.getDescription())
                .breakDownType(BreakDownType.SCM_CONTRIBUTIONS)
                .records(scmContributorAggs)
                .count(totalCount)
                .build();
    }

    private ScmContributorsFilter getScmContributionFilter(DevProductivityProfile.Feature feature, DevProductivityFilter devProductivityFilter, OrgUserDetails orgUserDetails, Map<String, Object> profileSettings) {

        if (CollectionUtils.isEmpty(getIntegrationIds(orgUserDetails)))
            return null;

        Map<String, List<String>> params = MapUtils.emptyIfNull(feature.getParams());
        boolean useCommitters = false;
        boolean useAuthors = true;
        boolean useTimeline = false;
        Long currentDate = null;
        Long prevYearDate = null;

        if(devProductivityFilter.getTimeRange() == null) {
            useTimeline = true;
            Calendar calendar = Calendar.getInstance();
            currentDate = calendar.toInstant().getEpochSecond();
            calendar.add(Calendar.YEAR, -2);
             prevYearDate = calendar.toInstant().getEpochSecond();
        }

        if (params.containsKey("use_committers")) {
            useCommitters = parseBooleanParam(params, "use_committers");
        }
        if (params.containsKey("user_authors")) {
            useAuthors = parseBooleanParam(params, "user_authors");
            useCommitters = false;
        }

        List<String> userRowIds = orgUserDetails.getIntegrationUserDetailsList().stream()
                .filter(integrationUserDetails -> ScmQueryUtils.isScmIntegration(integrationUserDetails.getIntegrationType()))
                .map(IntegrationUserDetails::getIntegrationUserId)
                .map(UUID::toString)
                .collect(Collectors.toList());
        Pair<Map<String, Map<String, String>>, Map<String, Map<String, String>>> partialMatchPair = ScmCommitUtils.checkPartialMatchConditions(profileSettings);
        return getContributorsFiltersBuilder(profileSettings)
                .integrationIds(getIntegrationIds(orgUserDetails))
                .committers(useCommitters ? userRowIds : null)
                .authors(useAuthors ? userRowIds : null)
                .includeIssues(false)
                .across(useCommitters ? ScmContributorsFilter.DISTINCT.committer : ScmContributorsFilter.DISTINCT.author)
                .dataTimeRange(useTimeline ? ImmutablePair.of(prevYearDate, currentDate) : devProductivityFilter.getTimeRange())
                .partialMatch(partialMatchPair.getLeft())
                .excludePartialMatch(partialMatchPair.getRight())
                .build();
    }

    private Double deriveFeatureResult(DevProductivityProfile.Feature feature,
                                       DbListResponse<DbScmContributorAgg> dbListResponse) {

        Set<String> set = Sets.newHashSet();
        dbListResponse.getRecords().stream()
                .flatMap((feature.getFeatureType().equals(DevProductivityProfile.FeatureType.REPO_BREADTH)) ?
                        record -> record.getRepoBreadth().stream() : record ->  record.getTechBreadth().stream())
                .forEach(  item -> { set.add(item); });
        return Double.valueOf(set.size());

    }

    private List<String> getIntegrationIds(OrgUserDetails orgUserDetails) {
        return orgUserDetails.getIntegrationUserDetailsList().stream()
                .filter(integrationUserDetails -> ScmQueryUtils.isScmIntegration(integrationUserDetails.getIntegrationType()))
                .map(IntegrationUserDetails::getIntegrationId)
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    private boolean parseBooleanParam(Map<String, List<String>> params, String userRole) {
        return Boolean.parseBoolean(params.get(userRole).get(0));
    }

    public static ScmContributorsFilter.ScmContributorsFilterBuilder getContributorsFiltersBuilder(Map<String, Object> profileSettings) {
        ImmutablePair<Long, Long> loc = null;
        try {
            loc = profileSettings.get("loc") != null ?
                    getRangeImmutablePair("loc", (Map<String, Object>) profileSettings.get("loc")) : ImmutablePair.nullPair();
        } catch (BadRequestException e) {
            log.error("Failed to parse loc range filter" + e);
        }
        Map<String, Object> excludedFields = org.apache.commons.collections4.MapUtils.emptyIfNull((Map<String, Object>) profileSettings.get("exclude"));
        ImmutablePair<Long, Long> excludeLoc = ImmutablePair.nullPair();
        if (org.apache.commons.collections4.MapUtils.isNotEmpty(excludedFields)) {
            try {
                excludeLoc = excludedFields.get("loc") != null ?
                        getRangeImmutablePair("loc", (Map<String, Object>) excludedFields.get("loc")) : ImmutablePair.nullPair();
            } catch (BadRequestException e) {
                log.error("Failed to parse exclude loc range filter" + e);
            }
        }
        return ScmContributorsFilter.builder()
                .locRange(loc)
                .excludeLocRange(excludeLoc);
    }
}
