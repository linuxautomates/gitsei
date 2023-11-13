package io.levelops.commons.databases.services.dev_productivity.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.TenantSCMSettings;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.database.dev_productivity.*;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.WorkItemsService;
import io.levelops.commons.databases.services.dev_productivity.JiraFeatureHandlerService;
import io.levelops.commons.databases.services.dev_productivity.handlers.services.EsIssueMgmtFeatureHandlerService;
import io.levelops.commons.databases.services.dev_productivity.utils.FeatureHandlerUtil;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.databases.utils.IssueMgmtUtil;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.MapUtils;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Component
public class IssueManagementFeatureHandler implements DevProductivityFeatureHandler {

    private static final int DEFAULT_DAYS = 90;
    private static final String DEFAULT_AGG_INTERVAL = "month";
    private static final List<String> DEFAULT_DONE_STATUS_CATEGORIES = List.of("DONE","COMPLETED","RESOLVED","Done","Completed","Resolved");
    private static final List<String> DEFAULT_CRITICAL_PRIORITIES = List.of("HIGH","HIGHEST","High","Highest");
    private static final List<String> DEFAULT_BUG_ISSUE_TYPES = List.of("BUG","Bug");
    private final JiraIssueService jiraIssueService;
    private final WorkItemsService workItemsService;
    private final TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    private final JiraFilterParser jiraFilterParser;
    private final JiraFeatureHandlerService jiraFeatureHandlerService;
    private final EsIssueMgmtFeatureHandlerService esIssueMgmtFeatureHandlerService;
    private final Set<String> dbAllowedTenants;
    private final Set<String> speedUpTenantList;

    @Override
    public Set<DevProductivityProfile.FeatureType> getSupportedFeatureTypes() {
        return Set.of(DevProductivityProfile.FeatureType.NUMBER_OF_BUGS_FIXED_PER_MONTH,
                DevProductivityProfile.FeatureType.NUMBER_OF_STORIES_RESOLVED_PER_MONTH,
                DevProductivityProfile.FeatureType.NUMBER_OF_CRITICAL_BUGS_RESOLVED_PER_MONTH,
                DevProductivityProfile.FeatureType.NUMBER_OF_CRITICAL_STORIES_RESOLVED_PER_MONTH,
                DevProductivityProfile.FeatureType.NUMBER_OF_STORY_POINTS_DELIVERED_PER_MONTH,
                DevProductivityProfile.FeatureType.AVG_ISSUE_RESOLUTION_TIME);
    }

    @Autowired
    public IssueManagementFeatureHandler(JiraIssueService jiraIssueService,
                                         WorkItemsService workItemsService,
                                         JiraFilterParser jiraFilterParser,
                                         TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService,
                                         JiraFeatureHandlerService jiraFeatureHandlerService,
                                         EsIssueMgmtFeatureHandlerService esIssueMgmtFeatureHandlerService,
                                         @Value("${DB_DEV_PROD_WI:}") List<String> dbAllowedTenants,
                                         @Value("${OPTIMIZED_DRILLDOWN_TENANTS:}") List<String> speedUpTenantList) {
        this.jiraIssueService = jiraIssueService;
        this.workItemsService = workItemsService;
        this.jiraFilterParser = jiraFilterParser;
        this.ticketCategorizationSchemeDatabaseService = ticketCategorizationSchemeDatabaseService;
        this.jiraFeatureHandlerService = jiraFeatureHandlerService;
        this.esIssueMgmtFeatureHandlerService = esIssueMgmtFeatureHandlerService;
        this.dbAllowedTenants = new HashSet<>();
        if (CollectionUtils.isNotEmpty(dbAllowedTenants)) {
            this.dbAllowedTenants.addAll(dbAllowedTenants);
        }
        this.speedUpTenantList = new HashSet<>();
        if(CollectionUtils.isNotEmpty(speedUpTenantList)){
            this.speedUpTenantList.addAll(speedUpTenantList);
        }

    }

    @Override
    public FeatureResponse calculateFeature(String company, Integer sectionOrder, DevProductivityProfile.Feature feature, Map<String, Object> profileSettings,
                                            DevProductivityFilter devProductivityFilter, OrgUserDetails orgUserDetails, Map<String, Long> latestIngestedAtByIntegrationId,
                                            TenantSCMSettings tenantSCMSettings) throws SQLException, IOException {
        Map<String, List<String>> params = MapUtils.emptyIfNull(feature.getParams());
        String aggInterval = (params!= null && params.containsKey("aggInterval")) ? params.get("aggInterval").get(0).toLowerCase() : DEFAULT_AGG_INTERVAL;
        ReportIntervalType interval = devProductivityFilter.getInterval();
        Pair<Long, Long> resolutionTimeRange = ObjectUtils.firstNonNull(devProductivityFilter.getTimeRange(), interval != null ? interval.getIntervalTimeRange(Instant.now()).getTimeRange() : null);
        List<String> developmentStages = (List<String>) profileSettings.get("development_stages");
        if(resolutionTimeRange == null) {
            long endTime = LocalDate.now().toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC);
            long startTime = LocalDate.now().minusDays(DEFAULT_DAYS).toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC);
            resolutionTimeRange =  Pair.of(startTime, endTime);
        }
        List<DbAggregationResult> aggregationResult = null;
        Object issueMgmtFilter = createIssueMgmtFilter(company,feature,orgUserDetails,latestIngestedAtByIntegrationId,aggInterval,resolutionTimeRange);
        ObjectMapper MAPPER = DefaultObjectMapper.get();

        try {
            log.info("Dev Prod Feature - IssueMgmt - company {}, feature {}, devProductivityFilter {}, orgUserDetails {}, latestIngestedAtByIntegrationId {}, aggInterval {}, resolutionTimeRange {}, issueMgmtFilter {}",
                 company, MAPPER.writeValueAsString(feature), MAPPER.writeValueAsString(devProductivityFilter), MAPPER.writeValueAsString(orgUserDetails), MAPPER.writeValueAsString(latestIngestedAtByIntegrationId), aggInterval, resolutionTimeRange, MAPPER.writeValueAsString(issueMgmtFilter));
        } catch (Exception e) {
            log.error("Exception!!", e);
        }

        if(issueMgmtFilter == null) {
            return FeatureResponse.constructIntegrationsAbsentBuilder(sectionOrder, feature).build();
        }
        if (issueMgmtFilter instanceof JiraIssuesFilter) {
            JiraIssuesFilter jiraIssuesFilter = (JiraIssuesFilter) issueMgmtFilter;
            if (useEs(company, feature, devProductivityFilter)) {
                aggregationResult = esIssueMgmtFeatureHandlerService.getJiraFeatureResponse(company, jiraIssuesFilter, developmentStages, feature.getFeatureType());
            } else {
                aggregationResult = jiraFeatureHandlerService.getJiraFeatureResponse(company, jiraIssuesFilter, "", developmentStages, feature.getFeatureType());
            }
        } else if (issueMgmtFilter instanceof WorkItemsFilter) {
            WorkItemsFilter workItemsFilter = (WorkItemsFilter) issueMgmtFilter;
            if (useEs(company, feature, devProductivityFilter)) {
                aggregationResult = esIssueMgmtFeatureHandlerService.getWorkItemFeatureResponse(company, workItemsFilter, List.of(), feature.getFeatureType());
            } else {
                aggregationResult = workItemsService.getWorkItemsReport(company, workItemsFilter, WorkItemsMilestoneFilter.builder().build(), WorkItemsFilter.DISTINCT.workitem_resolved_at, false, null).getRecords();
            }
        }

        Double mean = null;
        Long totalCount = null;

        if(aggregationResult != null && aggregationResult.size() >0 ) {
            if(feature.getFeatureType() == DevProductivityProfile.FeatureType.NUMBER_OF_STORY_POINTS_DELIVERED_PER_MONTH){
                totalCount = aggregationResult.stream()
                        .map(r -> r.getTotalStoryPoints())
                        .filter(Objects::nonNull)
                        .mapToLong(Long::longValue)
                        .sum();
            }else if(feature.getFeatureType() == DevProductivityProfile.FeatureType.AVG_ISSUE_RESOLUTION_TIME){
                totalCount = aggregationResult.stream()
                        .map(r -> r.getTimeSpentPerTicket())
                        .filter(Objects::nonNull)
                        .mapToLong(Long::longValue)
                        .sum();
            }else{
                totalCount = aggregationResult.stream()
                        .map(r -> r.getTotalTickets())
                        .filter(Objects::nonNull)
                        .mapToLong(Long::longValue)
                        .sum();
            }

            Instant lowerBound = DateUtils.fromEpochSecond(resolutionTimeRange.getLeft());
            Instant upperBound = DateUtils.fromEpochSecond(resolutionTimeRange.getRight());
            List<ImmutablePair<Long, Long>> timePartition = FeatureHandlerUtil.getTimePartitionByInterval(aggInterval, lowerBound, upperBound);
            mean = feature.getFeatureType() == DevProductivityProfile.FeatureType.AVG_ISSUE_RESOLUTION_TIME ? totalCount * 1.0
                                                :  new BigDecimal((totalCount*1.0)/timePartition.size()).setScale(2, RoundingMode.HALF_UP).doubleValue();
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
        log.info("check if optimized drill-down queries is required for {}", company);
        boolean isSpeedUp = false;
        if(speedUpTenantList.contains(company)){
            isSpeedUp = true;
            log.info("will use optimized drill-down queries for {}", company);
        }

        Map<String, List<String>> params = MapUtils.emptyIfNull(feature.getParams());
        String aggInterval = (params!= null && params.containsKey("aggInterval")) ? params.get("aggInterval").get(0).toLowerCase() : DEFAULT_AGG_INTERVAL;
        Pair<Long, Long> resolutionTimeRange = devProductivityFilter.getTimeRange();
        if(resolutionTimeRange == null) {
            long endTime = LocalDate.now().toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC);
            long startTime = LocalDate.now().minusDays(DEFAULT_DAYS).toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC);
            resolutionTimeRange =  Pair.of(startTime, endTime);
        }
        List<String> developmentStages = (List<String>) profileSettings.get("development_stages");
        Object issueMgmtFilter = createIssueMgmtFilter(company,feature,orgUserDetails,latestIngestedAtByIntegrationId,aggInterval,resolutionTimeRange);

        ObjectMapper MAPPER = DefaultObjectMapper.get();
        try {
            log.info("Dev Prod Drilldown - IssueMgmt - company {}, feature {}, devProductivityFilter {}, orgUserDetails {}, latestIngestedAtByIntegrationId {}, aggInterval {}, resolutionTimeRange {}, issueMgmtFilter {}",
                    company, MAPPER.writeValueAsString(feature), MAPPER.writeValueAsString(devProductivityFilter), MAPPER.writeValueAsString(orgUserDetails), MAPPER.writeValueAsString(latestIngestedAtByIntegrationId), aggInterval, resolutionTimeRange, MAPPER.writeValueAsString(issueMgmtFilter));
        } catch (Exception e) {
            log.error("Exception!!", e);
        }

        List<DbJiraIssue> jiraIssues = List.of();
        List<DbWorkItem> workItems = List.of();
        long totalCount = 0;
        if (issueMgmtFilter instanceof JiraIssuesFilter) {
            JiraIssuesFilter jiraIssuesFilter = (JiraIssuesFilter) issueMgmtFilter;
            DbListResponse<DbJiraIssue> dbListResponse;
            if (useEs(company, feature, devProductivityFilter)) {
                dbListResponse = esIssueMgmtFeatureHandlerService.getJiraFeatureBreakDown(company, jiraIssuesFilter, developmentStages, feature.getFeatureType(), sortBy, pageNumber, pageSize);
            } else {
                dbListResponse = jiraFeatureHandlerService.getFeatureBreakDown(company, jiraIssuesFilter, "", developmentStages, sortBy, feature.getFeatureType(), isSpeedUp, pageNumber, pageSize);
            }
            jiraIssues = dbListResponse.getRecords();
            totalCount = dbListResponse.getTotalCount();
        } else if (issueMgmtFilter instanceof WorkItemsFilter){
            WorkItemsFilter workItemsFilter = (WorkItemsFilter) issueMgmtFilter;
            DbListResponse<DbWorkItem> dbListResponse;
            if (useEs(company, feature, devProductivityFilter)) {
                dbListResponse = esIssueMgmtFeatureHandlerService.getWorkItemFeatureBreakDown(company, workItemsFilter, List.of(), feature.getFeatureType(), sortBy, pageNumber, pageSize);
            } else {
                dbListResponse = workItemsService.listByFilter(company, workItemsFilter,WorkItemsMilestoneFilter.builder().build(),null, pageNumber, pageSize);
            }
            workItems = dbListResponse.getRecords();
            totalCount = dbListResponse.getTotalCount();

        }
        return FeatureBreakDown.builder()
                .orgUserId(orgUserDetails.getOrgUserId())
                .email(orgUserDetails.getEmail())
                .fullName(orgUserDetails.getFullName())
                .name(feature.getName())
                .description(feature.getDescription())
                .breakDownType(issueMgmtFilter instanceof JiraIssuesFilter ? BreakDownType.JIRA_ISSUES : BreakDownType.WORKITEM_ISSUES)
                .records(issueMgmtFilter instanceof JiraIssuesFilter ? jiraIssues : workItems)
                .count(totalCount)
                .build();
    }

    private Object createIssueMgmtFilter(String company, DevProductivityProfile.Feature feature, OrgUserDetails orgUserDetails, Map<String, Long> latestIngestedAtByIntegrationId, String aggInterval, Pair<Long,Long> resolutionTimeRange){
        Map<String, List<String>> params = MapUtils.emptyIfNull(feature.getParams());
        List<String> doneStatusCategories = (params!= null && params.containsKey("doneStatusCategories")) ? params.get("doneStatusCategories") : DEFAULT_DONE_STATUS_CATEGORIES;
        List<String> criticalPriorities = (params!= null && params.containsKey("criticalPriorities")) ? params.get("criticalPriorities") : DEFAULT_CRITICAL_PRIORITIES;
        List<String> bugIssueTypes = (params!= null && params.containsKey("bugIssueTypes")) ? params.get("bugIssueTypes") : DEFAULT_BUG_ISSUE_TYPES;
        boolean useIssues = false;
        if (params.containsKey("use_issues")) {
            useIssues = parseBooleanParam(params, "use_issues");
        }

        List<IntegrationUserDetails> jiraIntegrations =  orgUserDetails.getIntegrationUserDetailsList().stream().filter(i -> IntegrationType.JIRA.equals(i.getIntegrationType())).collect(Collectors.toList());
        List<IntegrationUserDetails> adoIntegrations =  orgUserDetails.getIntegrationUserDetailsList().stream().
                filter(i -> IntegrationType.AZURE_DEVOPS.equals(i.getIntegrationType())).collect(Collectors.toList());
        if(CollectionUtils.isNotEmpty(jiraIntegrations))
            return getJiraIssueFilter(company,feature,jiraIntegrations,latestIngestedAtByIntegrationId,resolutionTimeRange,aggInterval,doneStatusCategories,criticalPriorities,bugIssueTypes,useIssues);
        else if(CollectionUtils.isNotEmpty(adoIntegrations))
            return getWorkItemfilter(company,feature,adoIntegrations,latestIngestedAtByIntegrationId,resolutionTimeRange,aggInterval,doneStatusCategories,criticalPriorities,bugIssueTypes,useIssues);
        return null;
    }

    private boolean parseBooleanParam(Map<String, List<String>> params, String key) {
        return Boolean.parseBoolean(params.get(key).get(0));
    }

    private WorkItemsFilter getWorkItemfilter(String company, DevProductivityProfile.Feature feature, List<IntegrationUserDetails> adoIntegrations, Map<String, Long> latestIngestedAtByIntegrationId,
                                              Pair<Long, Long> resolutionTimeRange, String aggInterval, List<String> doneStatusCategories, List<String> criticalPriorities, List<String> bugIssueTypes, boolean useIssues) {

        Map<String, Long> adIngestedAtByIntegrationId = latestIngestedAtByIntegrationId.entrySet()
                .stream()
                .filter(e -> (adoIntegrations.stream().map(i -> i.getIntegrationId()).map(String::valueOf).collect(Collectors.toList()).contains(e.getKey())))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        Long weekOlderDate = LocalDate.now().minusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant().getEpochSecond();
        //Getting oldest available ingested_at from last 7 days
        Optional<Long> ingestedAt = adIngestedAtByIntegrationId.values().stream()
                    .filter(v -> v >= weekOlderDate )
                    .min(Long::compare);


        WorkItemsFilter.WorkItemsFilterBuilder workItemFilterBuilder = WorkItemsFilter.builder()
                .assignees(adoIntegrations.stream().map(IntegrationUserDetails::getIntegrationUserId)
                        .filter(Objects::nonNull).map(UUID::toString).collect(Collectors.toList()))
                //if oldest ingestAt for last 7 days is not available, will use latest available ingested_at
                .ingestedAt(ingestedAt.isPresent() ? ingestedAt.get() : adIngestedAtByIntegrationId.values().stream().max(Comparator.comparing(Long::valueOf)).get())
                .ingestedAtByIntegrationId(adIngestedAtByIntegrationId)
                .integrationIds(adoIntegrations.stream().map(i -> i.getIntegrationId()).map(String::valueOf).collect(Collectors.toList()))
                .workItemResolvedRange(ImmutablePair.of(resolutionTimeRange))
                .aggInterval(AGG_INTERVAL.fromString(aggInterval).name())
                .across(WorkItemsFilter.DISTINCT.workitem_resolved_at)
                .calculation(WorkItemsFilter.CALCULATION.issue_count)
                .statusCategories(doneStatusCategories);
        switch(feature.getFeatureType()){
            case NUMBER_OF_BUGS_FIXED_PER_MONTH:
                workItemFilterBuilder = workItemFilterBuilder.workItemTypes(bugIssueTypes);
                break;
            case NUMBER_OF_STORIES_RESOLVED_PER_MONTH:
                workItemFilterBuilder = workItemFilterBuilder.excludeWorkItemTypes(bugIssueTypes);
                break;
            case AVG_ISSUE_RESOLUTION_TIME:
                if(useIssues)
                    workItemFilterBuilder = workItemFilterBuilder.workItemTypes(bugIssueTypes);
                else
                    workItemFilterBuilder = workItemFilterBuilder.excludeWorkItemTypes(bugIssueTypes);
                break;
            case NUMBER_OF_CRITICAL_BUGS_RESOLVED_PER_MONTH:
            case NUMBER_OF_CRITICAL_STORIES_RESOLVED_PER_MONTH:
                if(CollectionUtils.isNotEmpty(feature.getTicketCategories())){
                    try {
                        List<TicketCategorizationScheme.TicketCategorization> categories  = feature.getTicketCategories().stream().map(categoryId -> {
                            return ticketCategorizationSchemeDatabaseService.getCategoryById(company,categoryId.toString()).orElse(null);
                        }).filter(Objects::nonNull).collect(Collectors.toList());
                        List<WorkItemsFilter.TicketCategorizationFilter> categorizationFilters = IssueMgmtUtil.generateTicketCategorizationFilters(company,categories);
                        workItemFilterBuilder = workItemFilterBuilder.ticketCategorizationFilters(categorizationFilters)
                                .ticketCategories(categories.stream().map(TicketCategorizationScheme.TicketCategorization::getName).collect(Collectors.toList()));
                    } catch (BadRequestException e) {
                        log.error("Could not create work item categories filter",e);
                    }
                }else
                    return null;
        }
        return workItemFilterBuilder.build();
    }

    private JiraIssuesFilter getJiraIssueFilter(String company, DevProductivityProfile.Feature feature, List<IntegrationUserDetails> jiraIntegrations, Map<String, Long> latestIngestedAtByIntegrationId,
                                                Pair<Long, Long> resolutionTimeRange, String aggInterval, List<String> doneStatusCategories, List<String> criticalPriorities, List<String> bugIssueTypes, boolean useIssues) {

        Map<String, Long> jiraIngestedAtByIntegrationId = latestIngestedAtByIntegrationId.entrySet()
                .stream()
                .filter(e -> (jiraIntegrations.stream().map(i -> i.getIntegrationId()).map(String::valueOf).collect(Collectors.toList()).contains(e.getKey())))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));


        Long weekOlderDate = LocalDate.now().minusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant().getEpochSecond();
        //Getting oldest available ingested_at from last 7 days
        Optional<Long> ingestedAt = jiraIngestedAtByIntegrationId.values().stream()
                    .filter(v -> v >= weekOlderDate )
                    .min(Long::compare);


        JiraIssuesFilter.JiraIssuesFilterBuilder jiraIssuesFilterBuilder = JiraIssuesFilter.builder()
                .assignees(jiraIntegrations.stream().map(IntegrationUserDetails::getIntegrationUserId)
                        .filter(Objects::nonNull).map(UUID::toString).collect(Collectors.toList()))
                //if oldest ingestAt for last 7 days is not available, will use latest available ingested_at
                .ingestedAt(ingestedAt.isPresent() ? ingestedAt.get() : jiraIngestedAtByIntegrationId.values().stream().max(Comparator.comparing(Long::valueOf)).get())
                .integrationIds(jiraIntegrations.stream().map(i -> i.getIntegrationId()).map(String::valueOf).collect(Collectors.toList()))
                .issueResolutionRange(ImmutablePair.of(resolutionTimeRange))
                .ingestedAtByIntegrationId(jiraIngestedAtByIntegrationId)
                .aggInterval(AGG_INTERVAL.fromString(aggInterval).name())
                .across(JiraIssuesFilter.DISTINCT.issue_resolved)
                .calculation(JiraIssuesFilter.CALCULATION.ticket_count)
                .statusCategories(doneStatusCategories);
        switch(feature.getFeatureType()){
            case NUMBER_OF_BUGS_FIXED_PER_MONTH:
                jiraIssuesFilterBuilder = jiraIssuesFilterBuilder.issueTypes(bugIssueTypes);
                break;
            case NUMBER_OF_STORIES_RESOLVED_PER_MONTH:
                jiraIssuesFilterBuilder = jiraIssuesFilterBuilder.excludeIssueTypes(bugIssueTypes);
                break;
            case AVG_ISSUE_RESOLUTION_TIME:
                if(useIssues)
                    jiraIssuesFilterBuilder = jiraIssuesFilterBuilder.issueTypes(bugIssueTypes);
                else
                    jiraIssuesFilterBuilder = jiraIssuesFilterBuilder.excludeIssueTypes(bugIssueTypes);
                break;
            case NUMBER_OF_CRITICAL_BUGS_RESOLVED_PER_MONTH:
            case NUMBER_OF_CRITICAL_STORIES_RESOLVED_PER_MONTH:
                if(CollectionUtils.isNotEmpty(feature.getTicketCategories())){
                    try {
                        List<TicketCategorizationScheme.TicketCategorization> categories  = feature.getTicketCategories().stream().map(categoryId -> {
                            return ticketCategorizationSchemeDatabaseService.getCategoryById(company,categoryId.toString()).orElse(null);
                        }).filter(Objects::nonNull).collect(Collectors.toList());
                        List<JiraIssuesFilter.TicketCategorizationFilter> categorizationFilters = jiraFilterParser.generateTicketCategorizationFilters(company,categories);
                        jiraIssuesFilterBuilder = jiraIssuesFilterBuilder.ticketCategorizationFilters(categorizationFilters)
                                .ticketCategories(categories.stream().map(TicketCategorizationScheme.TicketCategorization::getName).collect(Collectors.toList()));
                    } catch (BadRequestException e) {
                        log.error("Could not create jira categories filter",e);
                    }
                }else
                    return null;
        }
        return jiraIssuesFilterBuilder.build();
    }

    private boolean useEs(String company, DevProductivityProfile.Feature feature, DevProductivityFilter devProductivityFilter) {
        boolean useEs = FeatureHandlerUtil.useEs(company, dbAllowedTenants, devProductivityFilter);
        return useEs;
    }
}
