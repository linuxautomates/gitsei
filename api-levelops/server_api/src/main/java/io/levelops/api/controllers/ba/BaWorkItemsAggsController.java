package io.levelops.api.controllers.ba;

import com.google.common.base.MoreObjects;
import io.levelops.api.services.IngestedAtCachingService;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.business_alignment.BaWorkItemsAggsDatabaseService;
import io.levelops.commons.databases.services.business_alignment.BaWorkItemsAggsQueryBuilder;
import io.levelops.commons.databases.services.business_alignment.BaWorkItemsScmAggsQueryBuilder;
import io.levelops.commons.databases.utils.IssueMgmtUtil;
import io.levelops.commons.databases.utils.LatestIngestedAt;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.sql.SQLException;

@RestController
@RequestMapping("/v1/ba/issue_mgmt/")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN','ORG_ADMIN_USER')")
@Log4j2
@SuppressWarnings("unused")
public class BaWorkItemsAggsController {

    private final BaWorkItemsAggsDatabaseService workItemsBaAggsDatabaseService;
    private final TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    private final OrgUnitHelper orgUnitHelper;
    private final IntegrationService integrationService;
    private final IngestedAtCachingService ingestedAtCachingService;

    @Autowired
    public BaWorkItemsAggsController(BaWorkItemsAggsDatabaseService workItemsBaAggsDatabaseService,
                                     TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService,
                                     OrgUnitHelper orgUnitHelper,
                                     IntegrationService integrationService,
                                     IngestedAtCachingService ingestedAtCachingService) {
        this.workItemsBaAggsDatabaseService = workItemsBaAggsDatabaseService;
        this.ticketCategorizationSchemeDatabaseService = ticketCategorizationSchemeDatabaseService;
        this.orgUnitHelper = orgUnitHelper;
        this.integrationService = integrationService;
        this.ingestedAtCachingService = ingestedAtCachingService;
    }

    private static BaWorkItemsAggsQueryBuilder.WorkItemsAcross parseWorkItemsAcross(DefaultListRequest request) {
        return MoreObjects.firstNonNull(
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.fromString(request.getAcross()),
                BaWorkItemsAggsQueryBuilder.WorkItemsAcross.TICKET_CATEGORY);
    }

    private static BaWorkItemsScmAggsQueryBuilder.ScmAcross parseScmAcross(DefaultListRequest request) {
        return MoreObjects.firstNonNull(
                BaWorkItemsScmAggsQueryBuilder.ScmAcross.fromString(request.getAcross()),
                BaWorkItemsScmAggsQueryBuilder.ScmAcross.TICKET_CATEGORY);
    }

    private static BaWorkItemsAggsDatabaseService.BaWorkItemsOptions parseBaOptions(DefaultListRequest request) {
        return BaWorkItemsAggsDatabaseService.BaWorkItemsOptions.builder()
                .completedWorkStatusCategories(request.<String>getFilterValueAsList("ba_completed_work_status_categories").orElse(null))
                .completedWorkStatuses(request.<String>getFilterValueAsList("ba_completed_work_statuses").orElse(null))
                .inProgressStatusCategories(request.<String>getFilterValueAsList("ba_in_progress_status_categories").orElse(null))
                .inProgressStatuses(request.<String>getFilterValueAsList("ba_in_progress_statuses").orElse(null))
                .build();
    }

    private WorkItemsFilter addIngestedAtToFilter(String company, WorkItemsFilter workItemsFilter) throws SQLException {
        LatestIngestedAt latestIngestedAt = IssueMgmtUtil.getIngestedAt(company, IntegrationType.AZURE_DEVOPS, workItemsFilter, integrationService, ingestedAtCachingService.getIngestedAtCache());
        return workItemsFilter.toBuilder()
                .ingestedAt(latestIngestedAt.getLatestIngestedAt())
                .ingestedAtByIntegrationId(latestIngestedAt.getLatestIngestedAtByIntegrationId())
                .build();
    }

    @PostMapping("/ticket_count_fte")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getTicketCountFTE(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.AZURE_DEVOPS, originalRequest);
            var request = ouConfig.getRequest();
            BaWorkItemsAggsQueryBuilder.WorkItemsAcross across = parseWorkItemsAcross(request);
            log.info("Serving ba/issue_mgmt/ticket_count_fte across {} for {}", across, company);
            WorkItemsFilter workItemsFilter = WorkItemsFilter.fromDefaultListRequest(request, WorkItemsFilter.DISTINCT.fromString(across.toString()), null);
            workItemsFilter = addIngestedAtToFilter(company, workItemsFilter);
            if (StringUtils.isNotEmpty(workItemsFilter.getTicketCategorizationSchemeId())) {
                workItemsFilter = workItemsFilter.toBuilder()
                        .ticketCategorizationFilters(IssueMgmtUtil.generateTicketCategorizationFilters(company,
                                workItemsFilter.getTicketCategorizationSchemeId(), ticketCategorizationSchemeDatabaseService))
                        .build();
            }
            WorkItemsMilestoneFilter workItemsMilestoneFilter = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            workItemsBaAggsDatabaseService.calculateTicketCountFTE(company, across, workItemsFilter, workItemsMilestoneFilter, parseBaOptions(request), ouConfig)
                    ));
        });
    }

    @PostMapping("/story_points_fte")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getStoryPointsFTE(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.AZURE_DEVOPS, originalRequest);
            var request = ouConfig.getRequest();
            BaWorkItemsAggsQueryBuilder.WorkItemsAcross across = parseWorkItemsAcross(request);
            log.info("Serving ba/issue_mgmt/story_points_fte across {} for {}", across, company);
            WorkItemsFilter workItemsFilter = WorkItemsFilter.fromDefaultListRequest(request, WorkItemsFilter.DISTINCT.fromString(across.toString()), null);
            workItemsFilter = addIngestedAtToFilter(company, workItemsFilter);
            if (StringUtils.isNotEmpty(workItemsFilter.getTicketCategorizationSchemeId())) {
                workItemsFilter = workItemsFilter.toBuilder()
                        .ticketCategorizationFilters(IssueMgmtUtil.generateTicketCategorizationFilters(company,
                                workItemsFilter.getTicketCategorizationSchemeId(), ticketCategorizationSchemeDatabaseService))
                        .build();
            }
            WorkItemsMilestoneFilter workItemsMilestoneFilter = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            workItemsBaAggsDatabaseService.calculateStoryPointsFTE(company, across, workItemsFilter, workItemsMilestoneFilter, parseBaOptions(request), ouConfig)
                    ));
        });
    }

    @PostMapping("/ticket_time_fte")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getTicketTimeFTE(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.AZURE_DEVOPS, originalRequest);
            var request = ouConfig.getRequest();
            BaWorkItemsAggsQueryBuilder.WorkItemsAcross across = parseWorkItemsAcross(request);
            log.info("Serving ba/issue_mgmt/ticket_time_fte across {} for {}", across, company);
            WorkItemsFilter workItemsFilter = WorkItemsFilter.fromDefaultListRequest(request, WorkItemsFilter.DISTINCT.fromString(across.toString()), null);
            workItemsFilter = addIngestedAtToFilter(company, workItemsFilter);
            if (StringUtils.isNotEmpty(workItemsFilter.getTicketCategorizationSchemeId())) {
                workItemsFilter = workItemsFilter.toBuilder()
                        .ticketCategorizationFilters(IssueMgmtUtil.generateTicketCategorizationFilters(company,
                                workItemsFilter.getTicketCategorizationSchemeId(), ticketCategorizationSchemeDatabaseService))
                        .build();
            }
            WorkItemsMilestoneFilter workItemsMilestoneFilter = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            workItemsBaAggsDatabaseService.calculateTicketTimeSpentFTE(company, across, workItemsFilter, workItemsMilestoneFilter, parseBaOptions(request), ouConfig)
                    ));
        });
    }

    @PostMapping("/commit_count_fte")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getCommitCountFTE(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
            var request = ouConfig.getRequest();
            BaWorkItemsScmAggsQueryBuilder.ScmAcross across = parseScmAcross(request);
            log.info("Serving ba/issue_mgmt/commit_count_fte across {} for {}", across, company);
            WorkItemsFilter workItemsFilter = WorkItemsFilter.fromDefaultListRequest(request, WorkItemsFilter.DISTINCT.fromString(across.toString()), null);
            workItemsFilter = addIngestedAtToFilter(company, workItemsFilter);
            if (StringUtils.isNotEmpty(workItemsFilter.getTicketCategorizationSchemeId())) {
                workItemsFilter = workItemsFilter.toBuilder()
                        .ticketCategorizationFilters(IssueMgmtUtil.generateTicketCategorizationFilters(company,
                                workItemsFilter.getTicketCategorizationSchemeId(), ticketCategorizationSchemeDatabaseService))
                        .build();
            }
            WorkItemsMilestoneFilter workItemsMilestoneFilter = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");
            ScmCommitFilter commitsFilter = ScmCommitFilter.fromDefaultListRequest(request, null, null, null);
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            workItemsBaAggsDatabaseService.calculateScmCommitCountFTE(company, across, commitsFilter, workItemsFilter, workItemsMilestoneFilter, ouConfig)
                    ));
        });
    }

    @PostMapping("/active_work/ticket_count")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getTicketCountActiveWork(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.AZURE_DEVOPS, originalRequest);
            var request = ouConfig.getRequest();
            BaWorkItemsAggsQueryBuilder.WorkItemsAcross across = parseWorkItemsAcross(request);
            log.info("Serving ba/issue_mgmt/active_work/ticket_count across {} for {}", across, company);
            WorkItemsFilter workItemsFilter = WorkItemsFilter.fromDefaultListRequest(request, WorkItemsFilter.DISTINCT.fromString(across.toString()), null);
            workItemsFilter = addIngestedAtToFilter(company, workItemsFilter);
            if (StringUtils.isNotEmpty(workItemsFilter.getTicketCategorizationSchemeId())) {
                workItemsFilter = workItemsFilter.toBuilder()
                        .ticketCategorizationFilters(IssueMgmtUtil.generateTicketCategorizationFilters(company,
                                workItemsFilter.getTicketCategorizationSchemeId(), ticketCategorizationSchemeDatabaseService))
                        .build();
            }
            WorkItemsMilestoneFilter workItemsMilestoneFilter = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            workItemsBaAggsDatabaseService.calculateTicketCountActiveWork(company, parseWorkItemsAcross(request), workItemsFilter, workItemsMilestoneFilter, ouConfig)
                    ));
        });
    }

    @PostMapping("active_work/story_points")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getStoryPointsActiveWork(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.AZURE_DEVOPS, originalRequest);
            var request = ouConfig.getRequest();
            BaWorkItemsAggsQueryBuilder.WorkItemsAcross across = parseWorkItemsAcross(request);
            log.info("Serving ba/issue_mgmt/active_work/story_points across {} for {}", across, company);
            WorkItemsFilter workItemsFilter = WorkItemsFilter.fromDefaultListRequest(request, WorkItemsFilter.DISTINCT.fromString(across.toString()), null);
            workItemsFilter = addIngestedAtToFilter(company, workItemsFilter);
            if (StringUtils.isNotEmpty(workItemsFilter.getTicketCategorizationSchemeId())) {
                workItemsFilter = workItemsFilter.toBuilder()
                        .ticketCategorizationFilters(IssueMgmtUtil.generateTicketCategorizationFilters(company,
                                workItemsFilter.getTicketCategorizationSchemeId(), ticketCategorizationSchemeDatabaseService))
                        .build();
            }
            WorkItemsMilestoneFilter workItemsMilestoneFilter = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            workItemsBaAggsDatabaseService.calculateStoryPointsActiveWork(company, across, workItemsFilter, workItemsMilestoneFilter, ouConfig)
                    ));
        });
    }
}
