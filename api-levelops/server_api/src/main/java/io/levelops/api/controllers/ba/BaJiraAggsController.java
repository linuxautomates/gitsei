package io.levelops.api.controllers.ba;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.business_alignment.BaJiraAggsDatabaseService;
import io.levelops.commons.databases.services.business_alignment.BaJiraAggsWidgetService;
import io.levelops.commons.databases.services.business_alignment.BaJiraScmAggsQueryBuilder.ScmAcross;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping("/v1/ba/jira/")
@PreAuthorize("hasAnyAuthority('ADMIN','PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
@Log4j2
@SuppressWarnings("unused")
public class BaJiraAggsController {

    private final BaJiraAggsWidgetService baJiraAggsWidgetService;

    public BaJiraAggsController(BaJiraAggsWidgetService baJiraAggsWidgetService) {
        this.baJiraAggsWidgetService = baJiraAggsWidgetService;
    }

    @PostMapping("/ticket_count_fte")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getTicketCountFTE(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest,
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "there_is_no_precalculate", required = false, defaultValue = "false") Boolean disablePrecalculatedResult,
            @RequestParam(name = "force_source", required = false) String forceSource) {
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok().body(baJiraAggsWidgetService.calculateTicketCountFTE(company, originalRequest, disableCache, disablePrecalculatedResult, forceSource));
        });
    }

    @PostMapping("/story_points_fte")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getStoryPointsFTE(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest,
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "there_is_no_precalculate", required = false, defaultValue = "false") Boolean disablePrecalculatedResult,
            @RequestParam(name = "force_source", required = false) String forceSource) {
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok().body(baJiraAggsWidgetService.calculateStoryPointsFTE(company, originalRequest, disableCache, disablePrecalculatedResult, forceSource));
        });
    }

    @PostMapping("/ticket_time_fte")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getTicketTimeFTE(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest,
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "there_is_no_precalculate", required = false, defaultValue = "false") Boolean disablePrecalculatedResult,
            @RequestParam(name = "force_source", required = false) String forceSource) {
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok().body(baJiraAggsWidgetService.calculateTicketTimeSpentFTE(company, originalRequest, disableCache, disablePrecalculatedResult, forceSource));
        });
    }

    @PostMapping("/commit_count_fte")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getCommitCountFTE(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest,
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "there_is_no_precalculate", required = false, defaultValue = "false") Boolean disablePrecalculatedResult,
            @RequestParam(name = "force_source", required = false) String forceSource) {
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok().body(baJiraAggsWidgetService.calculateScmCommitCountFTE(company, originalRequest, disableCache, disablePrecalculatedResult, forceSource));
        });
    }

    @PostMapping("/active_work/ticket_count")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getTicketCountActiveWork(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest,
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource) {
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok().body(baJiraAggsWidgetService.calculateTicketCountActiveWork(company, originalRequest, disableCache, forceSource));
        });
    }

    @PostMapping("/active_work/story_points")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getStoryPointsActiveWork(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest,
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource) {
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok().body( baJiraAggsWidgetService.calculateStoryPointsActiveWork(company, originalRequest, disableCache, forceSource));
        });
    }
}
