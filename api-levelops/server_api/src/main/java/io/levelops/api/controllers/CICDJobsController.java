package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.services.CiCdJobsService;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/cicd")
@Log4j2
public class CICDJobsController {

    private final CiCdJobRunsDatabaseService jobRunsService;
    private final CiCdJobsService jobsService;
    private final OrgUnitHelper ouHelper;

    @Autowired
    public CICDJobsController (final CiCdJobRunsDatabaseService jobRunsDatabaseService, final CiCdJobsService jobsService,final OrgUnitHelper ouHelper) {
        this.jobRunsService = jobRunsDatabaseService;
        this.jobsService = jobsService;
        this.ouHelper = ouHelper;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PostMapping(path = "/jobs_agg/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Map<String, Object>>> jobAggregationsList(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.getCICDIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/cicd/jobs_agg/list' for the request: {}", company, originalRequest, e);
            }
            try {
                var startTime = request.<String, Object>getFilterValueAsMap("start_time").orElse(Map.of());
                var filters = QueryFilter.fromRequestFilters(request.getFilter());
                if (MapUtils.isNotEmpty(startTime)) {
                    filters = filters.toBuilder().strictMatch("start_time", startTime.get("$gt")).strictMatch("end_time", startTime.get("$lt")).build();
                }
                var results = jobRunsService.getTriageGridAggs(company, filters, ouConfig, request.getPage(), request.getPageSize());
                return ResponseEntity.ok(
                        Map.of("_metadata", Map.of(
                                "page", request.getPage(),
                                "page_size", request.getPageSize(),
                                "next_page", results.getCount() < results.getTotalCount() ? request.getPage() + 1 : request.getPage(),
                                "has_next", results.getCount() == request.getPageSize(),
                                "total_count", results.getTotalCount()),
                                "count", results.getCount(),
                                "records", results.getRecords(),
                                "totals", results.getTotals()
                        ));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to complete the request. Please try again in a few second or please contact support if the problem persist.", e);
            }
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PostMapping(path = "/jobs/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, Object>>>> jobsList(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.getCICDIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/cicd/jobs/list' for the request: {}", company, originalRequest, e);
            }
            try {
                var results = jobsService.list(company, request.getPage(), request.getPageSize());
                return ResponseEntity.ok(PaginatedResponse.of(
                        request.getPage(),
                        request.getPageSize(),
                        results.getTotalCount(),
                        results.getRecords().stream()
                            .map(item -> Map.<String, Object>of("id", item.getId(), "name", item.getJobName()))
                            .collect(Collectors.toList())));
            }
            catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to complete the request. Please try again in a few second or please contact support if the problem persist.", e);
            }
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PostMapping(path = "/jobs/steps", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Map<String, Object>>> jobsSteps(
            @SessionAttribute("company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.getCICDIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/cicd/jobs/steps' for the request: {}", company, originalRequest, e);
            }
            try {
                var results = jobsService.getSteps(company, request, request.getPage(), request.getPageSize());
                return ResponseEntity.ok(results);
            }
            catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to complete the request. Please try again in a few second or please contact support if the problem persist.", e);
            }
        });
    }

}

