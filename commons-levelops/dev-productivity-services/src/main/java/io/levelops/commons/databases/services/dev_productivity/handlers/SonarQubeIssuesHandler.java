package io.levelops.commons.databases.services.dev_productivity.handlers;


import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.TenantSCMSettings;
import io.levelops.commons.databases.models.database.dev_productivity.*;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.models.filters.SonarQubePrFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.ScmQueryUtils;
import io.levelops.commons.databases.services.SonarQubeAggService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.MapUtils;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Component
public class SonarQubeIssuesHandler implements DevProductivityFeatureHandler{

    private final SonarQubeAggService sonarQubeAggService;
    private static final int DEFAULT_DAYS = 90;
    private int lines = 100;

    @Autowired
    public SonarQubeIssuesHandler(SonarQubeAggService sonarQubeAggService){
        this.sonarQubeAggService = sonarQubeAggService;
    }


    @Override
    public Set<DevProductivityProfile.FeatureType> getSupportedFeatureTypes() {
        return Set.of(DevProductivityProfile.FeatureType.SONAR_BUG_ISSUES_PER_HUNDERD_LINES_OF_CODE,
                DevProductivityProfile.FeatureType.SONAR_VULNERABILITY_ISSUES_PER_HUNDERD_LINES_OF_CODE,
                DevProductivityProfile.FeatureType.SONAR_CODE_SMELLS_ISSUES_PER_HUNDERD_LINES_OF_CODE);
    }

    @Override
    public FeatureResponse calculateFeature(String company, Integer sectionOrder, DevProductivityProfile.Feature feature, Map<String, Object> profileSettings,
                                            DevProductivityFilter devProductivityFilter, OrgUserDetails orgUserDetails, Map<String, Long> latestIngestedAtByIntegrationId,
                                            TenantSCMSettings tenantSCMSettings) throws SQLException, IOException {

        SonarQubePrFilter sonarQubePrFilter = getSonarQubePrFilter(feature, devProductivityFilter, orgUserDetails);
        if(sonarQubePrFilter == null) {
            return FeatureResponse.constructIntegrationsAbsentBuilder(sectionOrder, feature).build();
        }

        List<DbAggregationResult> aggResult = sonarQubeAggService.groupByAndCalculatePrs(company, sonarQubePrFilter).getRecords();

        Double mean = CollectionUtils.isEmpty(aggResult) ? null : getIssuesPerNLines(aggResult, feature, lines);
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

        SonarQubePrFilter sonarQubePrFilter = getSonarQubePrFilter(feature, devProductivityFilter, orgUserDetails);
        List<DbAggregationResult> aggResult = List.of();
        long totalCount = 0;

        if(sonarQubePrFilter != null) {
            DbListResponse<DbAggregationResult> dbListResponse = sonarQubeAggService.list(company, sonarQubePrFilter, sortBy, pageNumber, pageSize);
            aggResult = dbListResponse.getRecords();
            totalCount = dbListResponse.getTotalCount();
        }

        return FeatureBreakDown.builder()
                .orgUserId(orgUserDetails.getOrgUserId())
                .email(orgUserDetails.getEmail())
                .fullName(orgUserDetails.getFullName())
                .name(feature.getName())
                .description(feature.getDescription())
                .breakDownType(BreakDownType.SONAR_ISSUES)
                .records(aggResult)
                .count(totalCount)
                .build();
    }

    private SonarQubePrFilter getSonarQubePrFilter(DevProductivityProfile.Feature feature, DevProductivityFilter devProductivityFilter, OrgUserDetails orgUserDetails) {

        List<IntegrationUserDetails> scmIntegrations = orgUserDetails.getIntegrationUserDetailsList().stream()
                .filter(integrationUserDetails -> ScmQueryUtils.isScmIntegration(integrationUserDetails.getIntegrationType()))
                .collect(Collectors.toList());
        List<IntegrationUserDetails> sonarIntegrations = orgUserDetails.getIntegrationUserDetailsList().stream()
                .filter(integrationUserDetails -> IntegrationType.SONARQUBE.equals(integrationUserDetails.getIntegrationType()))
                .collect(Collectors.toList());

        if(CollectionUtils.isEmpty(scmIntegrations) || CollectionUtils.isEmpty(sonarIntegrations))
                return null;

        Map<String, List<String>> params = MapUtils.emptyIfNull(feature.getParams());

        String interval = "month";
        lines = 100;
        SonarQubePrFilter.DISTINCT across = SonarQubePrFilter.DISTINCT.committed_at;

        if(params!= null && params.containsKey("aggInterval"))
            interval = params.get("aggInterval").get(0);

        if(params!= null && params.containsKey("across")) {

            if(!EnumUtils.isValidEnumIgnoreCase(SonarQubePrFilter.DISTINCT.class, params.get("across").get(0)))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, " Unknown across value provided ");

            across =  SonarQubePrFilter.DISTINCT.fromString(params.get("across").get(0));
        }

        if(params!= null && params.containsKey("lines")) {
            try {
                lines = Integer.valueOf(params.get("lines").get(0));
            }catch(NumberFormatException e){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, " Non-number value provided for lines ");
            }
        }
        Pair<Long, Long> resolutionTimeRange = devProductivityFilter.getTimeRange();

        if(resolutionTimeRange == null) {
            long endTime = LocalDate.now().toEpochDay();
            long startTime = LocalDate.now().minusDays(DEFAULT_DAYS).toEpochDay();
            resolutionTimeRange =  Pair.of(startTime, endTime);
        }

        List<String> cloudIdList = orgUserDetails.getIntegrationUserDetailsList().stream().filter(i -> IntegrationType.getSCMIntegrationTypes().contains(i.getIntegrationType())).map(i -> i.getCloudId()).collect(Collectors.toList());
        List<UUID> integrationIdList = orgUserDetails.getIntegrationUserDetailsList().stream().filter(i -> IntegrationType.getSCMIntegrationTypes().contains(i.getIntegrationType())).map(i -> i.getIntegrationUserId()).collect(Collectors.toList());
        return SonarQubePrFilter.builder()
                .integrationIds(orgUserDetails.getIntegrationUserDetailsList().stream().filter(i -> IntegrationType.getSCMIntegrationTypes().contains(i.getIntegrationType()) || IntegrationType.SONARQUBE.equals(i.getIntegrationType())).map(i -> i.getIntegrationId()).map(String::valueOf).collect(Collectors.toList()))
                .committerIds(across.equals(SonarQubePrFilter.DISTINCT.committed_at)?integrationIdList:null)
                .creatorIds(across.equals(SonarQubePrFilter.DISTINCT.pr_created)?integrationIdList:null)
                .creators(across.equals(SonarQubePrFilter.DISTINCT.pr_created)?cloudIdList:null)
                .commitCreatedRange(across.equals(SonarQubePrFilter.DISTINCT.committed_at)?ImmutablePair.of(resolutionTimeRange):null)
                .prCreatedRange(across.equals(SonarQubePrFilter.DISTINCT.pr_created)?ImmutablePair.of(resolutionTimeRange):null)
                .calculation(SonarQubePrFilter.CALCULATION.count)
                .aggInterval(AGG_INTERVAL.fromString(interval))
                .across(across)
                .build();
    }

    private Double getIssuesPerNLines(List<DbAggregationResult> aggResult, DevProductivityProfile.Feature feature, int lines) {

        long totalLines = aggResult.stream()
                .map(r -> r.getLinesAddedCount())
                .mapToLong(Long::longValue)
                .sum();

        Double issues = 0d;
        if (feature.getFeatureType().equals(DevProductivityProfile.FeatureType.SONAR_BUG_ISSUES_PER_HUNDERD_LINES_OF_CODE)) {
            issues = aggResult.stream()
                    .map(r -> Double.valueOf(r.getBugs()))
                    .mapToDouble(Double::doubleValue)
                    .sum();
        } else if (feature.getFeatureType().equals(DevProductivityProfile.FeatureType.SONAR_VULNERABILITY_ISSUES_PER_HUNDERD_LINES_OF_CODE)) {
            issues = aggResult.stream()
                    .map(r -> Double.valueOf(r.getVulnerabilities()))
                    .mapToDouble(Double::doubleValue)
                    .sum();
        } else if (feature.getFeatureType().equals(DevProductivityProfile.FeatureType.SONAR_CODE_SMELLS_ISSUES_PER_HUNDERD_LINES_OF_CODE)) {
            issues = aggResult.stream()
                    .map(r -> Double.valueOf(r.getCodeSmells()))
                    .mapToDouble(Double::doubleValue)
                    .sum();
        }

        return new BigDecimal((issues * lines) / totalLines).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
