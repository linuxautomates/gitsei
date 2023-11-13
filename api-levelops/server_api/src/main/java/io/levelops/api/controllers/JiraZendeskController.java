package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.combined.CommitWithJira;
import io.levelops.commons.databases.models.database.combined.JiraWithGitZendesk;
import io.levelops.commons.databases.models.database.combined.ZendeskWithJira;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.filters.ZendeskTicketsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.ScmJiraZendeskService;
import io.levelops.commons.databases.services.ZendeskQueryService;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.databases.services.jira.conditions.JiraFieldConditionsBuilder;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.commons.caching.CacheHashUtils.hashDataMapOfStrings;
import static io.levelops.commons.databases.models.filters.VCS_TYPE.parseFromFilter;

import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
@RequestMapping("/v1/jira_zendesk")
@SuppressWarnings("unused")
public class JiraZendeskController {

    private final IntegrationService integService;
    private final JiraFieldConditionsBuilder jiraFieldConditionsBuilder;
    private final ScmJiraZendeskService aggService;
    private final IntegrationTrackingService integrationTrackingService;
    private final JiraFilterParser jiraFilterParser;
    private final AggCacheService aggCacheService;
    private final ObjectMapper objectMapper;
    private final ZendeskQueryService zendeskQueryService;
    private final OrgUnitHelper orgUnitHelper;


    @Autowired
    public JiraZendeskController(IntegrationService integrationService,
                                 JiraFieldConditionsBuilder jiraFieldConditionsBuilder,
                                 ScmJiraZendeskService scmJiraZdService,
                                 IntegrationTrackingService integTService,
                                 JiraFilterParser jiraFilterParser,
                                 AggCacheService aggCacheService,
                                 ObjectMapper objectMapper,
                                 ZendeskQueryService zendeskQueryService,
                                 final OrgUnitHelper orgUnitHelper) {
        this.jiraFieldConditionsBuilder = jiraFieldConditionsBuilder;
        this.aggService = scmJiraZdService;
        this.integService = integrationService;
        this.integrationTrackingService = integTService;
        this.jiraFilterParser = jiraFilterParser;
        this.aggCacheService = aggCacheService;
        this.objectMapper = objectMapper;
        this.zendeskQueryService = zendeskQueryService;
        this.orgUnitHelper = orgUnitHelper;
    }

    private Long getIngestedAt(String company, IntegrationType type, DefaultListRequest filter)
            throws SQLException {
        Integration integ = integService.listByFilter(company,
                null,
                List.of(type.toString()),
                null,
                getListOrDefault(filter.getFilter(), "integration_ids").stream()
                        .map(x -> NumberUtils.toInt(x))
                        .collect(Collectors.toList()),
                List.of(),
                0,
                1)
                .getRecords().stream().findFirst().orElse(null);
        Long ingestedAt = DateUtils.truncate(new Date(), Calendar.DATE);
        if (integ != null)
            ingestedAt = integrationTrackingService.get(company, integ.getId())
                    .orElse(IntegrationTracker.builder().latestIngestedAt(ingestedAt).build())
                    .getLatestIngestedAt();
        return ingestedAt;
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/agg", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> filesList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JIRA, IntegrationType.ZENDESK), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_zendesk' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Map<String, String> committedRange = request.getFilterValue("zendesk_created_at", Map.class)
                    .orElse(Map.of());
            Boolean withCommits = Boolean.TRUE.equals(request.getFilter().getOrDefault("jira_has_commit",
                    true));
            Boolean getCommits = Boolean.TRUE.equals(request.getFilter().getOrDefault("jira_get_commit",
                    false));
            Long zdStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
            Long zdEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;

            Map<String, Map<String, String>> fieldSizeMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("jira_field_size"));
            validateFieldSizeFilter(company, integrationIds, fieldSizeMap);

            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null,
                    null, null, true);
            final Map<String, Object> excludedFields = request.<String, Object>getFilterValueAsMap("exclude").orElse(Map.of());
            ZendeskTicketsFilter zdFilter = ZendeskTicketsFilter.builder()
                    .brands(getListOrDefault(request.getFilter(), "zendesk_brands"))
                    .extraCriteria(getListOrDefault(request.getFilter(), "zendesk_hygiene_types")
                            .stream()
                            .map(String::valueOf)
                            .map(ZendeskTicketsFilter.EXTRA_CRITERIA::fromString)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .types(getListOrDefault(request.getFilter(), "zendesk_types"))
                    .ingestedAt(getIngestedAt(company, IntegrationType.ZENDESK, request))
                    .integrationIds(integrationIds)
                    .priorities(getListOrDefault(request.getFilter(), "zendesk_priorities"))
                    .statuses(getListOrDefault(request.getFilter(), "zendesk_statuses"))
                    .organizations(getListOrDefault(request.getFilter(), "zendesk_organizations"))
                    .requesterEmails(getListOrDefault(request.getFilter(), "zendesk_requesters"))
                    .submitterEmails(getListOrDefault(request.getFilter(), "zendesk_submitters"))
                    .assigneeEmails(getListOrDefault(request.getFilter(), "zendesk_assignees"))
                    .age(request.<String, Object>getFilterValueAsMap("zendesk_age").orElse(Map.of()))
                    .ticketCreatedStart(zdStart)
                    .ticketCreatedEnd(zdEnd)
                    .customAcross((String) request.getFilter().get("zendesk_custom_across"))
                    .customStacks(getListOrDefault(request.getFilter(), "zendesk_custom_stacks"))
                    .customFields((Map<String, List<String>>) request.getFilter().get("zendesk_custom_fields"))
                    .excludeCustomFields(MapUtils.emptyIfNull((Map<String, List<String>>) excludedFields.get("zendesk_custom_fields")))
                    .build();
            DbListResponse<DbAggregationResult> aggResult;
            String across = ObjectUtils.firstNonNull(request.getAcross(), "trend");
            Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            var finalOuConfig = ouConfig;
            if ("jira_status".equals(across)) {
                aggResult = AggCacheUtils.cacheOrCall(disableCache, company,
                        "/jira_zendesk/agg_jira_status_" + (withCommits || getCommits) + "_" + getCommits
                                + "_" + getHash("sort", sorting),
                        jiraFilter.generateCacheHash() + "_" + zdFilter.generateCacheHash(),
                        integrationIds, objectMapper, aggCacheService,
                        () -> aggService.groupJiraTickets(
                                company,
                                jiraFilter,
                                zdFilter,
                                integrationIds,
                                withCommits || getCommits, //if get commits is true, this should be true else rely on withcommits
                                getCommits, sorting, finalOuConfig));
            } else {
                aggResult = AggCacheUtils.cacheOrCall(disableCache, company,
                        "/jira_zendesk/agg_" + across + "_" + getHash("sort", sorting),
                        jiraFilter.generateCacheHash() + "_" + zdFilter.generateCacheHash(),
                        integrationIds, objectMapper, aggCacheService,
                        () -> aggService.groupZendeskTicketsWithJiraLink(
                                company,
                                jiraFilter,
                                zdFilter, sorting, finalOuConfig));
            }
            return ResponseEntity.ok(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            aggResult));
        });
    }
    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/escalation_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getTicketEscalationTimeReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JIRA, IntegrationType.ZENDESK), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_zendesk/escalation_time_report' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Map<String, String> committedRange = request.getFilterValue("zendesk_created_at", Map.class)
                    .orElse(Map.of());
            Long zdStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
            Long zdEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;

            Map<String, Map<String, String>> fieldSizeMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("jira_field_size"));
            validateFieldSizeFilter(company, integrationIds, fieldSizeMap);

            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null, null, null, true);
            final Map<String, Object> excludedFields = request.<String, Object>getFilterValueAsMap("exclude").orElse(Map.of());
            ZendeskTicketsFilter zdFilter = ZendeskTicketsFilter.builder()
                    .brands(getListOrDefault(request.getFilter(), "zendesk_brands"))
                    .extraCriteria(getListOrDefault(request.getFilter(), "zendesk_hygiene_types")
                            .stream()
                            .map(String::valueOf)
                            .map(ZendeskTicketsFilter.EXTRA_CRITERIA::fromString)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .types(getListOrDefault(request.getFilter(), "zendesk_types"))
                    .ingestedAt(getIngestedAt(company, IntegrationType.ZENDESK, request))
                    .integrationIds(integrationIds)
                    .priorities(getListOrDefault(request.getFilter(), "zendesk_priorities"))
                    .statuses(getListOrDefault(request.getFilter(), "zendesk_statuses"))
                    .organizations(getListOrDefault(request.getFilter(), "zendesk_organizations"))
                    .requesterEmails(getListOrDefault(request.getFilter(), "zendesk_requesters"))
                    .submitterEmails(getListOrDefault(request.getFilter(), "zendesk_submitters"))
                    .assigneeEmails(getListOrDefault(request.getFilter(), "zendesk_assignees"))
                    .customAcross((String) request.getFilter().get("zendesk_custom_across"))
                    .customStacks(getListOrDefault(request.getFilter(), "zendesk_custom_stacks"))
                    .customFields((Map<String, List<String>>) request.getFilter().get("zendesk_custom_fields"))
                    .excludeCustomFields((Map<String, List<String>>) excludedFields.get("zendesk_custom_fields"))
                    .ticketCreatedStart(zdStart)
                    .ticketCreatedEnd(zdEnd)
                    .DISTINCT(MoreObjects.firstNonNull(ZendeskTicketsFilter.DISTINCT.fromString(request.getAcross()),
                            ZendeskTicketsFilter.DISTINCT.assignee))
                    .build();
            Map<String, SortingOrder> sorting =
                    SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            var finalOuConfig = ouConfig;
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(), request.getPageSize(),
                    AggCacheUtils.cacheOrCall(disableCache, company,
                            "/jira_zendesk/escalation" + "_" + getHash("sort", sorting),
                            jiraFilter.generateCacheHash() + "_" + zdFilter.generateCacheHash(),
                            integrationIds, objectMapper, aggCacheService,
                            () -> aggService.getZendeskEscalationTimeReport(company, jiraFilter, zdFilter, sorting, finalOuConfig))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/files_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getEscalatedFileReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JIRA, IntegrationType.ZENDESK), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_zendesk/files_report' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");

            Map<String, String> committedRange = request.getFilterValue("committed_at", Map.class)
                    .orElse(Map.of());
            Long commitStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
            Long commitEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;

            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            Map<String, Map<String, String>> scmFilePartialmatch = partialMatchMap.entrySet().stream()
                    .filter(partialField -> partialField.getKey().startsWith("scm_file_"))
                    .collect(Collectors.toMap(
                            stringMapEntry -> stringMapEntry.getKey().replaceFirst("^scm_file_", ""),
                            Map.Entry::getValue));

            Map<String, String> zdCommittedRange = request.getFilterValue("zendesk_created_at", Map.class)
                    .orElse(Map.of());
            Long zdStart = zdCommittedRange.get("$gt") != null ? Long.valueOf(zdCommittedRange.get("$gt")) : null;
            Long zdEnd = zdCommittedRange.get("$lt") != null ? Long.valueOf(zdCommittedRange.get("$lt")) : null;

            final Map<String, Object> excludedFields = request.<String, Object>getFilterValueAsMap("exclude").orElse(Map.of());
            ScmFilesFilter scmFilesFilter = ScmFilesFilter.builder()
                    .integrationIds(integrationIds)
                    .repoIds(getListOrDefault(request.getFilter(), "repo_ids"))
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                    .excludeProjects(getListOrDefault(excludedFields, "projects"))
                    .filename((String) request.getFilter().getOrDefault("filename", null))
                    .module((String) request.getFilter().getOrDefault("module", null))
                    .listFiles((Boolean) request.getFilter().getOrDefault("list_files", true))
                    .partialMatch(scmFilePartialmatch)
                    .commitStartTime(commitStart)
                    .commitEndTime(commitEnd)
                    .build();

            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null, null, null, true);
            ZendeskTicketsFilter zdFilter = ZendeskTicketsFilter.builder()
                    .brands(getListOrDefault(request.getFilter(), "zendesk_brands"))
                    .extraCriteria(getListOrDefault(request.getFilter(), "zendesk_hygiene_types")
                            .stream()
                            .map(String::valueOf)
                            .map(ZendeskTicketsFilter.EXTRA_CRITERIA::fromString)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .types(getListOrDefault(request.getFilter(), "zendesk_types"))
                    .ingestedAt(getIngestedAt(company, IntegrationType.ZENDESK, request))
                    .integrationIds(integrationIds)
                    .priorities(getListOrDefault(request.getFilter(), "zendesk_priorities"))
                    .statuses(getListOrDefault(request.getFilter(), "zendesk_statuses"))
                    .organizations(getListOrDefault(request.getFilter(), "zendesk_organizations"))
                    .requesterEmails(getListOrDefault(request.getFilter(), "zendesk_requesters"))
                    .submitterEmails(getListOrDefault(request.getFilter(), "zendesk_submitters"))
                    .assigneeEmails(getListOrDefault(request.getFilter(), "zendesk_assignees"))
                    .ticketCreatedStart(zdStart)
                    .ticketCreatedEnd(zdEnd)
                    .customAcross((String) request.getFilter().get("zendesk_custom_across"))
                    .customStacks(getListOrDefault(request.getFilter(), "zendesk_custom_stacks"))
                    .customFields((Map<String, List<String>>) request.getFilter().get("zendesk_custom_fields"))
                    .excludeCustomFields((Map<String, List<String>>) excludedFields.get("zendesk_custom_fields"))
                    .DISTINCT(ZendeskTicketsFilter.DISTINCT.fromString(request.getAcross()))
                    .build();
            Map<String, SortingOrder> sorting =
                    SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            var finalOuConfig = ouConfig;
            return ResponseEntity.ok(
                    PaginatedResponse.of(request.getPage(), request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/jira_zendesk/escalation_files" + "_" + getHash("sort", sorting),
                                    scmFilesFilter.generateCacheHash() + jiraFilter.generateCacheHash() + "_" + zdFilter.generateCacheHash(),
                                    integrationIds, objectMapper, aggCacheService,
                                    () -> aggService.getZendeskEscalationFileReport(company, scmFilesFilter, jiraFilter, zdFilter, sorting, finalOuConfig))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/resolved_tickets_trend", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getResolvedTicketsTrend(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JIRA, IntegrationType.ZENDESK), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_zendesk/resolved_tickets_trend' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Map<String, Map<String, String>> fieldSizeMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("jira_field_size"));
            validateFieldSizeFilter(company, integrationIds, fieldSizeMap);
            Map<String, String> committedRange = request.getFilterValue("zendesk_created_at", Map.class)
                    .orElse(Map.of());
            Long zdStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : Instant.now().minus(Duration.ofDays(7)).getEpochSecond();
            Long zdEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : Instant.now().getEpochSecond();
            Boolean withCommits = Boolean.TRUE.equals(request.getFilter().getOrDefault("jira_has_commit",
                    true));
            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null, null, null, true);
            final Map<String, Object> excludedFields = request.<String, Object>getFilterValueAsMap("exclude").orElse(Map.of());
            ZendeskTicketsFilter zdFilter = ZendeskTicketsFilter.builder()
                    .brands(getListOrDefault(request.getFilter(), "zendesk_brands"))
                    .extraCriteria(getListOrDefault(request.getFilter(), "zendesk_hygiene_types")
                            .stream()
                            .map(String::valueOf)
                            .map(ZendeskTicketsFilter.EXTRA_CRITERIA::fromString)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .types(getListOrDefault(request.getFilter(), "zendesk_types"))
                    .ingestedAt(getIngestedAt(company, IntegrationType.ZENDESK, request))
                    .integrationIds(integrationIds)
                    .priorities(getListOrDefault(request.getFilter(), "zendesk_priorities"))
                    .statuses(getListOrDefault(request.getFilter(), "zendesk_statuses"))
                    .organizations(getListOrDefault(request.getFilter(), "zendesk_organizations"))
                    .requesterEmails(getListOrDefault(request.getFilter(), "zendesk_requesters"))
                    .submitterEmails(getListOrDefault(request.getFilter(), "zendesk_submitters"))
                    .assigneeEmails(getListOrDefault(request.getFilter(), "zendesk_assignees"))
                    .customAcross((String) request.getFilter().get("zendesk_custom_across"))
                    .customStacks(getListOrDefault(request.getFilter(), "zendesk_custom_stacks"))
                    .customFields((Map<String, List<String>>) request.getFilter().get("zendesk_custom_fields"))
                    .excludeCustomFields((Map<String, List<String>>) excludedFields.get("zendesk_custom_fields"))
                    .age(request.<String, Object>getFilterValueAsMap("zendesk_age").orElse(Map.of()))
                    .ticketCreatedStart(zdStart)
                    .ticketCreatedEnd(zdEnd)
                    .build();
            Map<String, SortingOrder> sorting =
                    SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            var finalOuConfig = ouConfig;
            return ResponseEntity.ok(PaginatedResponse.of(
                    request.getPage(),
                    request.getPageSize(),
                    AggCacheUtils.cacheOrCall(disableCache, company,
                            "/jira_zendesk/resolved_tickets_" + withCommits + "_" + getHash("sort", sorting),
                            jiraFilter.generateCacheHash() + "_" + zdFilter.generateCacheHash(),
                            integrationIds, objectMapper, aggCacheService,
                            () -> aggService.resolvedTicketsTrendReport(
                                    company, jiraFilter, zdFilter, integrationIds, withCommits, sorting, finalOuConfig))
            ));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list_commit", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<CommitWithJira>>> listCommits(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JIRA, IntegrationType.ZENDESK), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_zendesk/list_commit' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Map<String, String> committedRange = request.getFilterValue("zendesk_created_at", Map.class)
                    .orElse(Map.of());
            Long zdStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
            Long zdEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;

            Map<String, Map<String, String>> fieldSizeMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("jira_field_size"));
            validateFieldSizeFilter(company, integrationIds, fieldSizeMap);

            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null,
                    null, null, true);
            final Map<String, Object> excludedFields = request.<String, Object>getFilterValueAsMap("exclude").orElse(Map.of());
            ZendeskTicketsFilter zdFilter = ZendeskTicketsFilter.builder()
                    .brands(getListOrDefault(request.getFilter(), "zendesk_brands"))
                    .extraCriteria(getListOrDefault(request.getFilter(), "zendesk_hygiene_types")
                            .stream()
                            .map(String::valueOf)
                            .map(ZendeskTicketsFilter.EXTRA_CRITERIA::fromString)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .types(getListOrDefault(request.getFilter(), "zendesk_types"))
                    .ingestedAt(getIngestedAt(company, IntegrationType.ZENDESK, request))
                    .integrationIds(integrationIds)
                    .priorities(getListOrDefault(request.getFilter(), "zendesk_priorities"))
                    .statuses(getListOrDefault(request.getFilter(), "zendesk_statuses"))
                    .organizations(getListOrDefault(request.getFilter(), "zendesk_organizations"))
                    .requesterEmails(getListOrDefault(request.getFilter(), "zendesk_requesters"))
                    .submitterEmails(getListOrDefault(request.getFilter(), "zendesk_submitters"))
                    .assigneeEmails(getListOrDefault(request.getFilter(), "zendesk_assignees"))
                    .age(request.<String, Object>getFilterValueAsMap("zendesk_age").orElse(Map.of()))
                    .customAcross((String) request.getFilter().get("zendesk_custom_across"))
                    .customStacks(getListOrDefault(request.getFilter(), "zendesk_custom_stacks"))
                    .customFields((Map<String, List<String>>) request.getFilter().get("zendesk_custom_fields"))
                    .excludeCustomFields((Map<String, List<String>>) excludedFields.get("zendesk_custom_fields"))
                    .ticketCreatedStart(zdStart)
                    .ticketCreatedEnd(zdEnd)
                    .build();
            Map<String, SortingOrder> sorting =
                    SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            var finalOuConfig = ouConfig;
            var page = request.getPage();
            var pageSize = request.getPageSize();
            return ResponseEntity.ok(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/jira_zendesk/list_commit_" + request.getPage()
                                            + "_" + request.getPageSize() + "_" + getHash("sort", sorting),
                                    jiraFilter.generateCacheHash() + "_" + zdFilter.generateCacheHash(),
                                    integrationIds, objectMapper, aggCacheService,
                                    () -> aggService.listCommits(company,
                                            jiraFilter,
                                            zdFilter,
                                            integrationIds,
                                            page,
                                            pageSize,
                                            sorting,
                                            finalOuConfig))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list_zendesk", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<ZendeskWithJira>>> listZendeskTickets(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JIRA, IntegrationType.ZENDESK), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_zendesk/list_zendesk' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Map<String, String> committedRange = request.getFilterValue("zendesk_created_at", Map.class)
                    .orElse(Map.of());
            Long zdStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
            Long zdEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;

            Map<String, Map<String, String>> fieldSizeMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("jira_field_size"));
            validateFieldSizeFilter(company, integrationIds, fieldSizeMap);

            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null,
                    null, null, true);

            final Map<String, Object> excludedFields = request.<String, Object>getFilterValueAsMap("exclude").orElse(Map.of());
            ZendeskTicketsFilter zdFilter = ZendeskTicketsFilter.builder()
                    .brands(getListOrDefault(request.getFilter(), "zendesk_brands"))
                    .extraCriteria(getListOrDefault(request.getFilter(), "zendesk_hygiene_types")
                            .stream()
                            .map(String::valueOf)
                            .map(ZendeskTicketsFilter.EXTRA_CRITERIA::fromString)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .types(getListOrDefault(request.getFilter(), "zendesk_types"))
                    .ingestedAt(getIngestedAt(company, IntegrationType.ZENDESK, request))
                    .integrationIds(integrationIds)
                    .priorities(getListOrDefault(request.getFilter(), "zendesk_priorities"))
                    .statuses(getListOrDefault(request.getFilter(), "zendesk_statuses"))
                    .organizations(getListOrDefault(request.getFilter(), "zendesk_organizations"))
                    .requesterEmails(getListOrDefault(request.getFilter(), "zendesk_requesters"))
                    .submitterEmails(getListOrDefault(request.getFilter(), "zendesk_submitters"))
                    .assigneeEmails(getListOrDefault(request.getFilter(), "zendesk_assignees"))
                    .customAcross((String) request.getFilter().get("zendesk_custom_across"))
                    .customStacks(getListOrDefault(request.getFilter(), "zendesk_custom_stacks"))
                    .customFields((Map<String, List<String>>) request.getFilter().get("zendesk_custom_fields"))
                    .excludeCustomFields((Map<String, List<String>>) excludedFields.get("zendesk_custom_fields"))
                    .age(request.<String, Object>getFilterValueAsMap("zendesk_age").orElse(Map.of()))
                    .ticketCreatedStart(zdStart)
                    .ticketCreatedEnd(zdEnd)
                    .build();
            Map<String, SortingOrder> sorting =
                    SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            var finalOuConfig = ouConfig;
            var page = request.getPage();
            var pageSize = request.getPageSize();
            if (sorting.containsKey("escalation_time")) {
                return ResponseEntity.ok(
                        PaginatedResponse.of(
                                page,
                                pageSize,
                                AggCacheUtils.cacheOrCall(disableCache, company,
                                        "/jira_zendesk/list_zendesktickets_escalation_" + sorting.get("escalation_time")
                                                + "_" + page + "_" + pageSize + "_" + getHash("sort", sorting),
                                        jiraFilter.generateCacheHash() + "_" + zdFilter.generateCacheHash(),
                                        integrationIds, objectMapper, aggCacheService,
                                        () -> zendeskQueryService.listZendeskTicketsWithEscalationTime(company,
                                                jiraFilter,
                                                zdFilter,
                                                page,
                                                pageSize,
                                                sorting,
                                                finalOuConfig))));
            } else {
                return ResponseEntity.ok(
                        PaginatedResponse.of(
                                page,
                                pageSize,
                                AggCacheUtils.cacheOrCall(disableCache, company,
                                        "/jira_zendesk/list_zendesktickets_" + page
                                                + "_" + pageSize + "_" + getHash("sort", sorting),
                                        jiraFilter.generateCacheHash() + "_" + zdFilter.generateCacheHash(),
                                        integrationIds, objectMapper, aggCacheService,
                                        () -> zendeskQueryService.listZendeskTickets(company,
                                                jiraFilter,
                                                zdFilter,
                                                page,
                                                pageSize,
                                                sorting,
                                                finalOuConfig))));
            }
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/files/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbScmFile>>> scmFilesList(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JIRA, IntegrationType.ZENDESK), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_zendesk/files/list' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Map<String, String> committedRange = request.getFilterValue("committed_at", Map.class)
                    .orElse(Map.of());
            Long commitStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
            Long commitEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;

            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            Map<String, Map<String, String>> scmFilePartialmatch = partialMatchMap.entrySet().stream()
                    .filter(partialField -> partialField.getKey().startsWith("scm_file_"))
                    .collect(Collectors.toMap(
                            stringMapEntry -> stringMapEntry.getKey().replaceFirst("^scm_file_", ""),
                            Map.Entry::getValue));

            Long zdStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
            Long zdEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;

            Map<String, Map<String, String>> fieldSizeMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("jira_field_size"));
            validateFieldSizeFilter(company, integrationIds, fieldSizeMap);
            final Map<String, Object> excludedFields = request.<String, Object>getFilterValueAsMap("exclude").orElse(Map.of());
            ScmFilesFilter scmFilter = ScmFilesFilter.builder()
                    .integrationIds(integrationIds)
                    .repoIds(getListOrDefault(request.getFilter(), "repo_ids"))
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                    .excludeProjects(getListOrDefault(excludedFields, "projects"))
                    .filename((String) request.getFilter().getOrDefault("filename", null))
                    .module((String) request.getFilter().getOrDefault("module", null))
                    .partialMatch(scmFilePartialmatch)
                    .commitStartTime(commitStart)
                    .commitEndTime(commitEnd)
                    .build();
            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null,
                    null, null, true);
            ZendeskTicketsFilter zdFilter = ZendeskTicketsFilter.builder()
                    .brands(getListOrDefault(request.getFilter(), "zendesk_brands"))
                    .extraCriteria(getListOrDefault(request.getFilter(), "zendesk_hygiene_types")
                            .stream()
                            .map(String::valueOf)
                            .map(ZendeskTicketsFilter.EXTRA_CRITERIA::fromString)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .types(getListOrDefault(request.getFilter(), "zendesk_types"))
                    .ingestedAt(getIngestedAt(company, IntegrationType.ZENDESK, request))
                    .integrationIds(integrationIds)
                    .priorities(getListOrDefault(request.getFilter(), "zendesk_priorities"))
                    .statuses(getListOrDefault(request.getFilter(), "zendesk_statuses"))
                    .organizations(getListOrDefault(request.getFilter(), "zendesk_organizations"))
                    .requesterEmails(getListOrDefault(request.getFilter(), "zendesk_requesters"))
                    .submitterEmails(getListOrDefault(request.getFilter(), "zendesk_submitters"))
                    .assigneeEmails(getListOrDefault(request.getFilter(), "zendesk_assignees"))
                    .customAcross((String) request.getFilter().get("zendesk_custom_across"))
                    .customStacks(getListOrDefault(request.getFilter(), "zendesk_custom_stacks"))
                    .customFields((Map<String, List<String>>) request.getFilter().get("zendesk_custom_fields"))
                    .excludeCustomFields((Map<String, List<String>>) excludedFields.get("zendesk_custom_fields"))
                    .age(request.<String, Object>getFilterValueAsMap("zendesk_age").orElse(Map.of()))
                    .ticketCreatedStart(zdStart)
                    .ticketCreatedEnd(zdEnd)
                    .build();
            Map<String, SortingOrder> sorting =
                    SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            var finalOuConfig = ouConfig;
            var page = request.getPage();
            var pageSize = request.getPageSize();
            return ResponseEntity.ok(
                    PaginatedResponse.of(page,
                            pageSize,
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/jira_zendesk/list_scm_files_" + page + "_" + pageSize
                                            + "_" + getHash("sort", sorting),
                                    scmFilter.generateCacheHash() + "_" + jiraFilter.generateCacheHash() + "_" + zdFilter.generateCacheHash(),
                                    integrationIds, objectMapper, aggCacheService,
                                    () -> zendeskQueryService.listEscalatedFiles(company,
                                            scmFilter,
                                            jiraFilter,
                                            zdFilter,
                                            page,
                                            pageSize,
                                            sorting,
                                            finalOuConfig))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list_jira", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<JiraWithGitZendesk>>> listJiraTickets(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JIRA, IntegrationType.ZENDESK), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_zendesk/list_jira' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Map<String, String> committedRange = request.getFilterValue("zendesk_created_at", Map.class)
                    .orElse(Map.of());
            Boolean withCommits = Boolean.TRUE.equals(request.getFilter().getOrDefault("jira_has_commit",
                    true));
            Long zdStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
            Long zdEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;

            Map<String, Map<String, String>> fieldSizeMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("jira_field_size"));
            validateFieldSizeFilter(company, integrationIds, fieldSizeMap);

            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null,
                    null, null, true);

            final Map<String, Object> excludedFields = request.<String, Object>getFilterValueAsMap("exclude").orElse(Map.of());
            ZendeskTicketsFilter zdFilter = ZendeskTicketsFilter.builder()
                    .brands(getListOrDefault(request.getFilter(), "zendesk_brands"))
                    .extraCriteria(getListOrDefault(request.getFilter(), "zendesk_hygiene_types")
                            .stream()
                            .map(String::valueOf)
                            .map(ZendeskTicketsFilter.EXTRA_CRITERIA::fromString)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .types(getListOrDefault(request.getFilter(), "zendesk_types"))
                    .ingestedAt(getIngestedAt(company, IntegrationType.ZENDESK, request))
                    .integrationIds(integrationIds)
                    .priorities(getListOrDefault(request.getFilter(), "zendesk_priorities"))
                    .statuses(getListOrDefault(request.getFilter(), "zendesk_statuses"))
                    .organizations(getListOrDefault(request.getFilter(), "zendesk_organizations"))
                    .requesterEmails(getListOrDefault(request.getFilter(), "zendesk_requesters"))
                    .submitterEmails(getListOrDefault(request.getFilter(), "zendesk_submitters"))
                    .assigneeEmails(getListOrDefault(request.getFilter(), "zendesk_assignees"))
                    .customAcross((String) request.getFilter().get("zendesk_custom_across"))
                    .customStacks(getListOrDefault(request.getFilter(), "zendesk_custom_stacks"))
                    .customFields((Map<String, List<String>>) request.getFilter().get("zendesk_custom_fields"))
                    .excludeCustomFields((Map<String, List<String>>) excludedFields.get("zendesk_custom_fields"))
                    .age(request.<String, Object>getFilterValueAsMap("zendesk_age").orElse(Map.of()))
                    .ticketCreatedStart(zdStart)
                    .ticketCreatedEnd(zdEnd)
                    .build();
            Map<String, SortingOrder> sorting =
                    SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            var finalOuConfig = ouConfig;
            var page = request.getPage();
            var pageSize = request.getPageSize();
            return ResponseEntity.ok(
                    PaginatedResponse.of(
                            page,
                            pageSize,
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/jira_zendesk/list_jira_" + page + "_" + pageSize
                                            + "_" + withCommits + "_" + getHash("sort", sorting),
                                    jiraFilter.generateCacheHash() + "_" + zdFilter.generateCacheHash(),
                                    integrationIds, objectMapper, aggCacheService,
                                    () -> aggService.listJiraTickets(company,
                                            jiraFilter,
                                            zdFilter,
                                            integrationIds,
                                            withCommits,
                                            page,
                                            pageSize,
                                            sorting,
                                            finalOuConfig))));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/top_committers", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> topCommitters(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JIRA, IntegrationType.ZENDESK), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_zendesk/top_committers' for the request: {}", company, originalRequest, e);
            }
            List<String> integrationIds = getListOrDefault(request.getFilter(), "integration_ids");
            if (CollectionUtils.isEmpty(integrationIds))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This endpoint must have integration_ids.");
            Map<String, String> committedRange = request.getFilterValue("zendesk_created_at", Map.class)
                    .orElse(Map.of());
            Long zdStart = committedRange.get("$gt") != null ? Long.valueOf(committedRange.get("$gt")) : null;
            Long zdEnd = committedRange.get("$lt") != null ? Long.valueOf(committedRange.get("$lt")) : null;

            Map<String, Map<String, String>> fieldSizeMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("jira_field_size"));
            validateFieldSizeFilter(company, integrationIds, fieldSizeMap);

            JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null,
                    null, null, true);
            final Map<String, Object> excludedFields = request.<String, Object>getFilterValueAsMap("exclude").orElse(Map.of());
            ZendeskTicketsFilter zdFilter = ZendeskTicketsFilter.builder()
                    .brands(getListOrDefault(request.getFilter(), "zendesk_brands"))
                    .extraCriteria(getListOrDefault(request.getFilter(), "zendesk_hygiene_types")
                            .stream()
                            .map(String::valueOf)
                            .map(ZendeskTicketsFilter.EXTRA_CRITERIA::fromString)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .types(getListOrDefault(request.getFilter(), "zendesk_types"))
                    .ingestedAt(getIngestedAt(company, IntegrationType.ZENDESK, request))
                    .integrationIds(integrationIds)
                    .priorities(getListOrDefault(request.getFilter(), "zendesk_priorities"))
                    .statuses(getListOrDefault(request.getFilter(), "zendesk_statuses"))
                    .organizations(getListOrDefault(request.getFilter(), "zendesk_organizations"))
                    .requesterEmails(getListOrDefault(request.getFilter(), "zendesk_requesters"))
                    .submitterEmails(getListOrDefault(request.getFilter(), "zendesk_submitters"))
                    .assigneeEmails(getListOrDefault(request.getFilter(), "zendesk_assignees"))
                    .customAcross((String) request.getFilter().get("zendesk_custom_across"))
                    .customStacks(getListOrDefault(request.getFilter(), "zendesk_custom_stacks"))
                    .customFields((Map<String, List<String>>) request.getFilter().get("zendesk_custom_fields"))
                    .excludeCustomFields((Map<String, List<String>>) excludedFields.get("zendesk_custom_fields"))
                    .ticketCreatedStart(zdStart)
                    .ticketCreatedEnd(zdEnd)
                    .build();
            ScmCommitFilter scmCommitFilter = ScmCommitFilter.builder()
                    .repoIds(getListOrDefault(request.getFilter(), "repo_ids"))
                    .vcsTypes(parseFromFilter(request.getFilter()))
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .authors(getListOrDefault(request.getFilter(), "authors"))
                    .committers(getListOrDefault(request.getFilter(), "committers"))
                    .commitShas(getListOrDefault(request.getFilter(), "commit_shas"))
                    .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                    .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                    .excludeProjects(getListOrDefault(excludedFields, "projects"))
                    .excludeAuthors(getListOrDefault(excludedFields, "authors"))
                    .excludeCommitters(getListOrDefault(excludedFields, "committers"))
                    .excludeCommitShas(getListOrDefault(excludedFields, "commit_shas"))
                    .build();
            ScmFilesFilter scmFilesFilter = ScmFilesFilter.builder()
                    .repoIds(getListOrDefault(request.getFilter(), "repo_ids"))
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                    .excludeRepoIds(getListOrDefault(excludedFields, "repo_ids"))
                    .excludeProjects(getListOrDefault(excludedFields, "projects"))
                    .filename((String) request.getFilter().getOrDefault("filename", null))
                    .commitStartTime(zdStart)
                    .commitEndTime(zdEnd)
                    .build();
            Map<String, SortingOrder> sorting =
                    SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
            var finalOuConfig = ouConfig;
            return ResponseEntity.ok(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/jira_zendesk/get_top_committers_" + request.getPage()
                                            + "_" + request.getPageSize() + "_" + getHash("sort", sorting),
                                    jiraFilter.generateCacheHash() + "_" + zdFilter.generateCacheHash() + "_"
                                            + scmCommitFilter.generateCacheHash() + "_" + scmFilesFilter.generateCacheHash(),
                                    integrationIds, objectMapper, aggCacheService,
                                    () -> aggService.getTopCommitters(
                                            company,
                                            jiraFilter,
                                            zdFilter,
                                            scmCommitFilter,
                                            scmFilesFilter,
                                            integrationIds,
                                            sorting,
                                            finalOuConfig))));
        });
    }

    public void validateFieldSizeFilter(String company, List<String> integrationIds, Map<String, Map<String, String>> fieldSizes) {
        if (MapUtils.isEmpty(fieldSizes))
            return;
        ArrayList<String> fieldSizeKeys = new ArrayList<>(fieldSizes.keySet());
        try {
            String unknownField = jiraFieldConditionsBuilder.checkFieldsPresent(company, integrationIds, fieldSizeKeys);
            if (unknownField != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, unknownField + " is not valid field for size based filter");
            }
        } catch (SQLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "error while fetching list of jira fields");
        }
    }

    public StringBuilder getHash(String fieldName, Map<String, ?> map) {
        StringBuilder dataToHash = new StringBuilder();
        hashDataMapOfStrings(dataToHash, fieldName, map);
        return dataToHash;
    }
}
