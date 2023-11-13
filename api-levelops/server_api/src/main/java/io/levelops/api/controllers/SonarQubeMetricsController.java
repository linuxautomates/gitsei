package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeCoverage;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeMeasure;
import io.levelops.commons.databases.models.filters.SonarQubeMetricFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.SonarQubeProjectService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
@RequestMapping("/v1/sonarqube_metrics")
public class SonarQubeMetricsController {

    public static final Map<SonarQubeMetricFilter.SCOPE, EnumSet<SonarQubeMetricFilter.DISTINCT>> SCOPED_DISTINCTS =
            Map.of(SonarQubeMetricFilter.SCOPE.repo, EnumSet.of(SonarQubeMetricFilter.DISTINCT.organization,
                    SonarQubeMetricFilter.DISTINCT.project, SonarQubeMetricFilter.DISTINCT.visibility,
                    SonarQubeMetricFilter.DISTINCT.trend, SonarQubeMetricFilter.DISTINCT.metric),
                    SonarQubeMetricFilter.SCOPE.pull_request, EnumSet.of(SonarQubeMetricFilter.DISTINCT.pull_request,
                            SonarQubeMetricFilter.DISTINCT.pr_branch, SonarQubeMetricFilter.DISTINCT.pr_base_branch,
                            SonarQubeMetricFilter.DISTINCT.pr_target_branch, SonarQubeMetricFilter.DISTINCT.trend,
                            SonarQubeMetricFilter.DISTINCT.metric),
                    SonarQubeMetricFilter.SCOPE.branch, EnumSet.of(SonarQubeMetricFilter.DISTINCT.branch,
                            SonarQubeMetricFilter.DISTINCT.trend, SonarQubeMetricFilter.DISTINCT.metric));

    private final IntegrationService integrationService;
    private final IntegrationTrackingService trackingService;
    private final SonarQubeProjectService projectService;
    private final OrgUnitHelper orgUnitHelper;

    public SonarQubeMetricsController(IntegrationService integrationService, IntegrationTrackingService trackingService,
                                      SonarQubeProjectService projectService,
                                      final OrgUnitHelper orgUnitHelper) {
        this.integrationService = integrationService;
        this.trackingService = trackingService;
        this.projectService = projectService;
        this.orgUnitHelper = orgUnitHelper;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbSonarQubeMeasure>>> getMeasures(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            try {
                var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.SONARQUBE, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/sonarqube_metrics/list' for the request: {}", company, originalRequest, e);
            }
            
            return ResponseEntity.ok(PaginatedResponse.of(
                request.getPage(),
                request.getPageSize(),
                projectService.listMetrics(company,
                        getFilterBuilder(company, request).build(),
                        SortingConverter.fromFilter(request.getSort()).entrySet().stream()
                                .findFirst()
                                .map(Map.Entry::getValue)
                                .orElse(SortingOrder.DESC),
                        request.getPage(),
                        request.getPageSize())));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getMeasureValues(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            try {
                var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.SONARQUBE, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/sonarqube_metrics/values' for the request: {}", company, originalRequest, e);
            }

            if (CollectionUtils.isEmpty(request.getFields()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            for (String field : request.getFields()) {
                SonarQubeMetricFilter.DISTINCT across = SonarQubeMetricFilter.DISTINCT.fromString(field);
                SonarQubeMetricFilter metricFilter = getFilterBuilder(company, request)
                        .DISTINCT(across)
                        .build();
                if (!SCOPED_DISTINCTS.get(metricFilter.getScope()).contains(across)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Across field: " + across +
                            " is not supported for scope: " + metricFilter.getScope());
                }
                response.add(Map.of(field, projectService.groupByAndCalculate(company,
                        metricFilter, true).getRecords()));
            }
            return ResponseEntity.ok(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getMeasureReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            try {
                var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.SONARQUBE, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/sonarqube_metrics/report' for the request: {}", company, originalRequest, e);
            }

            SonarQubeMetricFilter.DISTINCT across = SonarQubeMetricFilter.DISTINCT.fromString(request.getAcross());
            SonarQubeMetricFilter metricFilter = getFilterBuilder(company, request)
                    .DISTINCT(across)
                    .build();
            EnumSet<SonarQubeMetricFilter.DISTINCT> scopedDistincts = SCOPED_DISTINCTS.get(metricFilter.getScope());
            if (!scopedDistincts.contains(across)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Across field: " + across +
                        " is not supported for scope: " + metricFilter.getScope());
            }
            List<SonarQubeMetricFilter.DISTINCT> stacks = request.getStacks().stream()
                    .map(SonarQubeMetricFilter.DISTINCT::fromString)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            Optional<SonarQubeMetricFilter.DISTINCT> unsupportedStackOpt = stacks.stream()
                    .filter(distinct -> !scopedDistincts.contains(distinct))
                    .findAny();
            if (unsupportedStackOpt.isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stack: " + unsupportedStackOpt.get() +
                        " is not supported for scope: " + metricFilter.getScope());
            }
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(),
                    request.getPageSize(),
                    projectService.stackedGroupBy(company,
                            metricFilter,
                            stacks)));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/coverage", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbSonarQubeCoverage>>> getCodeCovergae(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            try {
                var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.SONARQUBE, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/sonarqube_metrics/coverage' for the request: {}", company, originalRequest, e);
            }

            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(),
                    request.getPageSize(),
                    projectService.listCoverage(company,
                            getFilterBuilder(company, request).build(),
                            request.getPage(),
                            request.getPageSize())));
        });
    }

    @SuppressWarnings("unchecked")
    private SonarQubeMetricFilter.SonarQubeMetricFilterBuilder getFilterBuilder(
            String company,
            DefaultListRequest filter) throws SQLException {
        return SonarQubeMetricFilter.builder()
                .ingestedAt(getIngestedAt(company, filter))
                .integrationIds(getListOrDefault(filter, "integration_ids"))
                .organizations(getListOrDefault(filter, "organizations"))
                .projects(getListOrDefault(filter, "projects"))
                .visibilities(getListOrDefault(filter, "visibilities"))
                .metrics(getListOrDefault(filter, "metrics"))
                .pullRequests(getListOrDefault(filter, "pull_requests"))
                .prBranches(getListOrDefault(filter, "pr_branches"))
                .prTargetBranches(getListOrDefault(filter, "pr_target_branches"))
                .prBaseBranches(getListOrDefault(filter, "pr_base_branches"))
                .branches(getListOrDefault(filter, "branches"))
                .complexityScore((Map<String, String>) filter.getFilter().get("complexity_score"))
                .scope(filter.getFilterValue("scope", String.class)
                        .map(SonarQubeMetricFilter.SCOPE::fromString)
                        .orElse(SonarQubeMetricFilter.SCOPE.repo));
    }


    private Long getIngestedAt(String company, DefaultListRequest filter) throws SQLException {
        List<Integer> integration_ids = getListOrDefault(filter, "integration_ids").stream()
                .map(Integer::parseInt).collect(Collectors.toList());
        Integration integration = integrationService.listByFilter(company, null,
                List.of(IntegrationType.SONARQUBE.toString()), null,
                integration_ids, List.of(),
                0, 1).getRecords().stream().findFirst().orElse(null);
        Long ingestedAt = DateUtils.truncate(new Date(), Calendar.DATE);
        if (integration != null)
            ingestedAt = trackingService.get(company, integration.getId())
                    .orElse(IntegrationTracker.builder().latestIngestedAt(ingestedAt).build())
                    .getLatestIngestedAt();
        return ingestedAt;
    }
}
