package io.levelops.commons.databases.services.business_alignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.caching.CacheHashUtils;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.services.business_alignment.es.services.BaJiraAggsESService;
import io.levelops.commons.services.business_alignment.models.BaJiraOptions;
import io.levelops.commons.services.business_alignment.models.Calculation;
import io.levelops.commons.services.business_alignment.models.JiraAcross;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Set;

@Log4j2
@Service
public class BaJiraAggsWidgetService {
    private final BaJiraAggsDatabaseService jiraBaAggsDatabaseService;
    private final JiraFilterParser jiraFilterParser;
    private final OrgUnitHelper orgUnitHelper;
    private final AggCacheService cacheService;
    private final ObjectMapper mapper;
    private final BaJiraAggsPrecalculatedWidgetReadService baJiraAggsPrecalculatedWidgetReadService;
    private final BaJiraAggsESService baJiraAggsESService;
    private final Set<String> baReadTicketCountESTenants;
    private final Set<String> baReadStoryPointsESTenants;
    private final Set<String> baReadTicketTimeESTenants;

    @Autowired
    public BaJiraAggsWidgetService(BaJiraAggsDatabaseService jiraBaAggsDatabaseService, JiraFilterParser jiraFilterParser, OrgUnitHelper orgUnitHelper, AggCacheService cacheService, ObjectMapper mapper, BaJiraAggsPrecalculatedWidgetReadService baJiraAggsPrecalculatedWidgetReadService,
                                   BaJiraAggsESService baJiraAggsESService,
                                   @Qualifier("baReadTicketCountESTenants") Set<String> baReadTicketCountESTenants,
                                   @Qualifier("baReadStoryPointsESTenants") Set<String> baReadStoryPointsESTenants,
                                   @Qualifier("baReadTicketTimeESTenants") Set<String> baReadTicketTimeESTenants) {
        this.jiraBaAggsDatabaseService = jiraBaAggsDatabaseService;
        this.jiraFilterParser = jiraFilterParser;
        this.orgUnitHelper = orgUnitHelper;
        this.cacheService = cacheService;
        this.mapper = mapper;
        this.baJiraAggsPrecalculatedWidgetReadService = baJiraAggsPrecalculatedWidgetReadService;
        this.baJiraAggsESService = baJiraAggsESService;
        this.baReadTicketCountESTenants = baReadTicketCountESTenants;
        this.baReadStoryPointsESTenants = baReadStoryPointsESTenants;
        this.baReadTicketTimeESTenants = baReadTicketTimeESTenants;
    }

    private static JiraAcross parseJiraAcross(DefaultListRequest request) {
        return MoreObjects.firstNonNull(
                JiraAcross.fromString(request.getAcross()),
                JiraAcross.TICKET_CATEGORY);
    }
    private static BaJiraScmAggsQueryBuilder.ScmAcross parseScmAcross(DefaultListRequest request) {
        return MoreObjects.firstNonNull(
                BaJiraScmAggsQueryBuilder.ScmAcross.fromString(request.getAcross()),
                BaJiraScmAggsQueryBuilder.ScmAcross.TICKET_CATEGORY);
    }

    private static final Boolean SHOULD_USE_ES = true;
    private static final Boolean SHOULD_USE_DB = false;


    private static boolean shouldUseEs(String forceSource, Set<String> esReadTenants, String company) {
        if ("es".equals(forceSource)) {
            return SHOULD_USE_ES;
        } else if ("db".equals(forceSource)) {
            return SHOULD_USE_DB;
        }
        return esReadTenants.contains(company);
    }

    public PaginatedResponse<DbAggregationResult> calculateTicketCountFTE(String company, DefaultListRequest originalRequest,Boolean disableCache, Boolean disablePrecalculatedResult, String forceSource) throws Exception {
        var opt = baJiraAggsPrecalculatedWidgetReadService.calculateTicketCountFTE(company, originalRequest, disablePrecalculatedResult);
        if(opt.isPresent()) {
            return opt.get();
        }

        var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
        var request = ouConfig.getRequest();
        JiraAcross across = parseJiraAcross(request);
        final var page = request.getPage();
        final var pageSize = request.getPageSize();
        BaJiraOptions baOptions = BaJiraOptions.fromDefaultListRequest(request);
        log.info("Serving ba/jira/ticket_count_fte across {} for {}", across, company);
        JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null, null, request.getAggInterval(), false);
        //log.info("TicketCategorizationFilters = {}", DefaultObjectMapper.get().writeValueAsString(jiraFilter.getTicketCategorizationFilters()));
        boolean useEs = shouldUseEs(forceSource, baReadTicketCountESTenants, company);
        log.info("calculateTicketCountFTE company {}, useEs {}", company, useEs);

        return PaginatedResponse.of(
                request.getPage(),
                request.getPageSize(),
                AggCacheUtils.cacheOrCall(disableCache, company,
                        "/ba/jira/ticket_count_fte_" + across,
                        CacheHashUtils.combineCacheHashes(jiraFilter.generateCacheHash(), baOptions.generateCacheHash(), (ouConfig.getOuId() != null ? ouConfig.getOuId().toString() : "")), jiraFilter.getIntegrationIds(), mapper, cacheService,
                        () ->  (useEs) ? baJiraAggsESService.doCalculateIssueFTE(company, across, Calculation.TICKET_COUNT, jiraFilter, ouConfig,baOptions, page, pageSize)
                                : jiraBaAggsDatabaseService.calculateTicketCountFTE(company, across, jiraFilter, baOptions, ouConfig, page, pageSize))
        );
    }

    public PaginatedResponse<DbAggregationResult> calculateStoryPointsFTE(String company, DefaultListRequest originalRequest,Boolean disableCache, Boolean disablePrecalculatedResult, String forceSource) throws Exception {
        var opt = baJiraAggsPrecalculatedWidgetReadService.calculateStoryPointsFTE(company, originalRequest, disablePrecalculatedResult);
        if(opt.isPresent()) {
            return opt.get();
        }

        var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
        var request = ouConfig.getRequest();
        JiraAcross across = parseJiraAcross(request);
        final var page = request.getPage();
        final var pageSize = request.getPageSize();
        BaJiraOptions baOptions = BaJiraOptions.fromDefaultListRequest(request);
        log.info("Serving ba/jira/ticket_count_fte across {} for {}", across, company);
        JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null, null, request.getAggInterval(), false);
        boolean useEs = shouldUseEs(forceSource, baReadStoryPointsESTenants, company);
        log.info("calculateStoryPointsFTE company {}, useEs {}", company, useEs);

        return PaginatedResponse.of(
                request.getPage(),
                request.getPageSize(),
                AggCacheUtils.cacheOrCall(disableCache, company,
                        "/ba/jira/story_points_fte_" + across,
                        CacheHashUtils.combineCacheHashes(jiraFilter.generateCacheHash(), baOptions.generateCacheHash(), (ouConfig.getOuId() != null ? ouConfig.getOuId().toString() : "")), jiraFilter.getIntegrationIds(), mapper, cacheService,
                        () ->  (useEs) ? baJiraAggsESService.doCalculateIssueFTE(company, across, Calculation.STORY_POINTS, jiraFilter, ouConfig, baOptions, page, pageSize)
                                : jiraBaAggsDatabaseService.calculateStoryPointsFTE(company, across, jiraFilter, baOptions, ouConfig, page, pageSize))
        );
    }

    public PaginatedResponse<DbAggregationResult> calculateTicketTimeSpentFTE(String company, DefaultListRequest originalRequest,Boolean disableCache, Boolean disablePrecalculatedResult, String forceSource) throws Exception {
        var opt = baJiraAggsPrecalculatedWidgetReadService.calculateTicketTimeSpentFTE(company, originalRequest, disablePrecalculatedResult);
        if(opt.isPresent()) {
            return opt.get();
        }
        
        var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
        var request = ouConfig.getRequest();
        JiraAcross across = parseJiraAcross(request);
        final var page = request.getPage();
        final var pageSize = request.getPageSize();
        BaJiraOptions baOptions = BaJiraOptions.fromDefaultListRequest(request);
        log.info("Serving ba/jira/ticket_time_fte across {} for {}", across, company);
        JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null, null, request.getAggInterval(), false);
        boolean useEs = shouldUseEs(forceSource, baReadTicketTimeESTenants, company);
        log.info("calculateTicketTimeSpentFTE company {}, useEs {}", company, useEs);

        return PaginatedResponse.of(
                request.getPage(),
                request.getPageSize(),
                AggCacheUtils.cacheOrCall(disableCache, company,
                        "/ba/jira/ticket_time_fte_" + across,
                        CacheHashUtils.combineCacheHashes(jiraFilter.generateCacheHash(), baOptions.generateCacheHash(), (ouConfig.getOuId() != null ? ouConfig.getOuId().toString() : "")), jiraFilter.getIntegrationIds(), mapper, cacheService,
                        () ->  (useEs) ? baJiraAggsESService.doCalculateIssueFTE(company, across, Calculation.TICKET_TIME_SPENT, jiraFilter, ouConfig, baOptions, page, pageSize)
                                : jiraBaAggsDatabaseService.calculateTicketTimeSpentFTE(company, across, jiraFilter, baOptions, ouConfig, page, pageSize))
        );
    }

    public PaginatedResponse<DbAggregationResult> calculateScmCommitCountFTE(String company, DefaultListRequest originalRequest,Boolean disableCache, Boolean disablePrecalculatedResult, String forceSource) throws Exception {
        var opt = baJiraAggsPrecalculatedWidgetReadService.calculateScmCommitCountFTE(company, originalRequest, disablePrecalculatedResult);
        if(opt.isPresent()) {
            return opt.get();
        }

        var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
        var request = ouConfig.getRequest();
        BaJiraScmAggsQueryBuilder.ScmAcross across = parseScmAcross(request);
        final var page = request.getPage();
        final var pageSize = request.getPageSize();
        log.info("Serving ba/jira/commit_count_fte across {} for {}", across, company);
        JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null, null, request.getAggInterval(), false);
        ScmCommitFilter commitsFilter = ScmCommitFilter.fromDefaultListRequest(request, null, null, null);
        return PaginatedResponse.of(
                request.getPage(),
                request.getPageSize(),
                AggCacheUtils.cacheOrCall(disableCache, company,
                        "/ba/jira/commit_count_fte_" + across,
                        CacheHashUtils.combineCacheHashes(jiraFilter.generateCacheHash(), commitsFilter.generateCacheHash(), (ouConfig.getOuId() != null ? ouConfig.getOuId().toString() : "")), jiraFilter.getIntegrationIds(), mapper, cacheService,
                        () ->  jiraBaAggsDatabaseService.calculateScmCommitCountFTE(company, across, commitsFilter, jiraFilter, ouConfig, page, pageSize))
        );
    }

    public PaginatedResponse<DbAggregationResult> calculateTicketCountActiveWork(String company, DefaultListRequest originalRequest,Boolean disableCache, String forceSource) throws Exception {
        var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
        var request = ouConfig.getRequest();
        JiraAcross across = parseJiraAcross(request);
        final var page = request.getPage();
        final var pageSize = request.getPageSize();
        log.info("Serving ba/jira/active_work/ticket_count across {} for {}", across, company);
        JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null, null, request.getAggInterval(), false);
        return PaginatedResponse.of(
                request.getPage(),
                request.getPageSize(),
                AggCacheUtils.cacheOrCall(disableCache, company,
                        "/ba/jira/active_work/ticket_count_" + across,
                        CacheHashUtils.combineCacheHashes(jiraFilter.generateCacheHash(), (ouConfig.getOuId() != null ? ouConfig.getOuId().toString() : "")), jiraFilter.getIntegrationIds(), mapper, cacheService,
                        () ->  jiraBaAggsDatabaseService.calculateTicketCountActiveWork(company, across, jiraFilter, ouConfig, page, pageSize))
        );
    }

    public PaginatedResponse<DbAggregationResult> calculateStoryPointsActiveWork(String company, DefaultListRequest originalRequest,Boolean disableCache, String forceSource) throws Exception {
        var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
        var request = ouConfig.getRequest();
        JiraAcross across = parseJiraAcross(request);
        final var page = request.getPage();
        final var pageSize = request.getPageSize();
        log.info("Serving ba/jira/active_work/story_points across {} for {}", across, company);
        JiraIssuesFilter jiraFilter = jiraFilterParser.createFilter(company, request, null, null, null, request.getAggInterval(), false);
        return PaginatedResponse.of(
                request.getPage(),
                request.getPageSize(),
                AggCacheUtils.cacheOrCall(disableCache, company,
                        "/ba/jira/active_work/story_points_" + across,
                        CacheHashUtils.combineCacheHashes(jiraFilter.generateCacheHash(), (ouConfig.getOuId() != null ? ouConfig.getOuId().toString() : "")), jiraFilter.getIntegrationIds(), mapper, cacheService,
                        () ->  jiraBaAggsDatabaseService.calculateStoryPointsActiveWork(company, across, jiraFilter, ouConfig, page, pageSize))
        );
    }
}
