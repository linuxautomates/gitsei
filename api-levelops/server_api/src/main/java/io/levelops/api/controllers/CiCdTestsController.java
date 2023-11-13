package io.levelops.api.controllers;

import com.google.common.base.MoreObjects;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.CiCdJobRunTest;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobRunTestsFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.CiCdJobRunTestDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'AUDITOR','SUPER_ADMIN','ORG_ADMIN_USER')")
@RequestMapping("/v1/cicd/job_runs/tests")
@SuppressWarnings("unused")
public class CiCdTestsController {

    private final CiCdJobRunTestDatabaseService testDatabaseService;
    private final OrgUnitHelper orgUnitHelper;

    @Autowired
    public CiCdTestsController(CiCdJobRunTestDatabaseService testDatabaseService, OrgUnitHelper orgUnitHelper) {
        this.testDatabaseService = testDatabaseService;
        this.orgUnitHelper = orgUnitHelper;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<CiCdJobRunTest>>> getTests(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JENKINS, IntegrationType.CIRCLECI, IntegrationType.DRONECI, IntegrationType.HARNESSNG,
                    IntegrationType.AZURE_PIPELINES, IntegrationType.AZURE_DEVOPS, IntegrationType.GITLAB), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/cicd/job_runs/tests/list' for the request: {}", company, originalRequest, e);
        }
        log.debug("filter = {}", request);
        DefaultListRequest finalRequest = request;
        OUConfiguration finalOuConfig = ouConfig;
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(PaginatedResponse.of(
                finalRequest.getPage(),
                finalRequest.getPageSize(),
                testDatabaseService.list(company, ciCdJobRunTestsFilterBuilder(finalRequest).build(),
                        finalRequest.getPage(), finalRequest.getPageSize(), finalOuConfig))));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getValuesReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JENKINS, IntegrationType.CIRCLECI, IntegrationType.DRONECI, IntegrationType.HARNESSNG,
                    IntegrationType.AZURE_PIPELINES, IntegrationType.AZURE_DEVOPS, IntegrationType.GITLAB), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/cicd/job_runs/tests/values' for the request: {}", company, originalRequest, e);
        }
        log.debug("filter = {}", request);
        DefaultListRequest finalRequest = request;
        OUConfiguration finalOuConfig = ouConfig;
        return SpringUtils.deferResponse(() -> {
            if (CollectionUtils.isEmpty(finalRequest.getFields()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            for (String field : finalRequest.getFields()) {
                response.add(Map.of(field, testDatabaseService.groupByAndCalculate(company,
                        ciCdJobRunTestsFilterBuilder(finalRequest)
                                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                                .DISTINCT(MoreObjects.firstNonNull(
                                        CiCdJobRunTestsFilter.DISTINCT.fromString(field),
                                        CiCdJobRunTestsFilter.DISTINCT.trend))
                                .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(finalRequest.getAggInterval()),
                                        CICD_AGG_INTERVAL.day))
                                .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(List.of(Map.of("id", field, "desc", false)), List.of())))
                                .build(), finalOuConfig).getRecords()));
            }
            return ResponseEntity.ok(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'AUDITOR', 'PUBLIC_DASHBOARD','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, value = "/tests_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getTestsReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JENKINS, IntegrationType.CIRCLECI, IntegrationType.DRONECI, IntegrationType.HARNESSNG,
                    IntegrationType.AZURE_PIPELINES, IntegrationType.AZURE_DEVOPS, IntegrationType.GITLAB), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/cicd/job_runs/tests/tests_report' for the request: {}", company, originalRequest, e);
        }
        log.debug("filter = {}", request);
        DefaultListRequest finalRequest = request;
        OUConfiguration finalOuConfig = ouConfig;
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(PaginatedResponse.of(
                finalRequest.getPage(),
                finalRequest.getPageSize(),
                testDatabaseService.stackedGroupBy(company,
                        ciCdJobRunTestsFilterBuilder(finalRequest)
                                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.count)
                                .DISTINCT(MoreObjects.firstNonNull(
                                        CiCdJobRunTestsFilter.DISTINCT.fromString(
                                                finalRequest.getAcross()),
                                        CiCdJobRunTestsFilter.DISTINCT.trend))
                                .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(finalRequest.getAggInterval()),
                                        CICD_AGG_INTERVAL.day))
                                .build(), getStacks(finalRequest.getStacks()), SortingConverter.fromFilter(MoreObjects.firstNonNull(finalRequest.getSort(), List.of())), finalOuConfig)
        )));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'AUDITOR', 'PUBLIC_DASHBOARD','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, value = "/duration_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getDurationReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JENKINS, IntegrationType.CIRCLECI, IntegrationType.DRONECI, IntegrationType.HARNESSNG,
                    IntegrationType.AZURE_PIPELINES, IntegrationType.AZURE_DEVOPS, IntegrationType.GITLAB), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/cicd/job_runs/tests/duration_report' for the request: {}", company, originalRequest, e);
        }
        log.debug("filter = {}", request);
        DefaultListRequest finalRequest = request;
        OUConfiguration finalOuConfig = ouConfig;
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(PaginatedResponse.of(
                finalRequest.getPage(),
                finalRequest.getPageSize(),
                testDatabaseService.stackedGroupBy(company,
                        ciCdJobRunTestsFilterBuilder(finalRequest)
                                .CALCULATION(CiCdJobRunTestsFilter.CALCULATION.duration)
                                .DISTINCT(MoreObjects.firstNonNull(
                                        CiCdJobRunTestsFilter.DISTINCT.fromString(
                                                finalRequest.getAcross()),
                                        CiCdJobRunTestsFilter.DISTINCT.trend))
                                .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(finalRequest.getAggInterval()),
                                        CICD_AGG_INTERVAL.day))
                                .build(), getStacks(finalRequest.getStacks()), SortingConverter.fromFilter(MoreObjects.firstNonNull(finalRequest.getSort(), List.of())), finalOuConfig)
        )));
    }

    @SuppressWarnings("unchecked")
    private CiCdJobRunTestsFilter.CiCdJobRunTestsFilterBuilder ciCdJobRunTestsFilterBuilder(DefaultListRequest filter) {
        Map<String, String> jobStartedAtRange = filter.getFilterValue("start_time", Map.class)
                .orElse(Map.of());
        Long jobStartedRangeStart = jobStartedAtRange.get("$gt") != null ? Long.valueOf(jobStartedAtRange.get("$gt")) : null;
        Long jobStartedRangeEnd = jobStartedAtRange.get("$lt") != null ? Long.valueOf(jobStartedAtRange.get("$lt")) : null;
        Map<String, String> endTimeRange = filter.getFilterValue("end_time", Map.class).orElse(Map.of());
        final Long endTimeStart = endTimeRange.get("$gt") != null ? Long.valueOf(endTimeRange.get("$gt")) : null;
        final Long endTimeEnd = endTimeRange.get("$lt") != null ? Long.valueOf(endTimeRange.get("$lt")) : null;
        Map<String, Object> excludeFields = (Map<String, Object>) filter.getFilter()
                .getOrDefault("exclude", Map.of());
        return CiCdJobRunTestsFilter.builder()
                .jobNames(getListOrDefault(filter.getFilter(), "job_names"))
                .jobStatuses(getListOrDefault(filter.getFilter(), "job_statuses"))
                .jobRunIds(getListOrDefault(filter.getFilter(), "job_run_ids"))
                .jobRunNumbers(getListOrDefault(filter.getFilter(), "job_run_numbers"))
                .testStatuses(getListOrDefault(filter.getFilter(), "test_statuses"))
                .testSuites(getListOrDefault(filter.getFilter(), "test_suites"))
                .cicdUserIds(getListOrDefault(filter.getFilter(), "cicd_user_ids"))
                .instanceNames(getListOrDefault(filter.getFilter(), "instance_names"))
                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                .types(CICD_TYPE.parseFromFilter(filter))
                .projects(getListOrDefault(filter.getFilter(), "projects"))
                .excludeProjects(getListOrDefault(excludeFields, "projects"))
                .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                .excludeJobStatuses(getListOrDefault(excludeFields, "job_statuses"))
                .excludeTestStatuses(getListOrDefault(excludeFields, "test_statuses"))
                .excludeTestSuites(getListOrDefault(excludeFields, "test_suites"))
                .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                .excludeJobRunNumbers(getListOrDefault(excludeFields, "job_run_numbers"))
                .excludeJobRunIds(getListOrDefault(excludeFields, "job_run_ids"))
                .excludeCiCdUserIds(getListOrDefault(excludeFields, "cicd_user_ids"))
                .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                .startTimeRange(ImmutablePair.of(jobStartedRangeStart, jobStartedRangeEnd))
                .endTimeRange(ImmutablePair.of(endTimeStart, endTimeEnd))
                .orgProductsIds(getListOrDefault(filter.getFilter(), "org_product_ids").stream()
                        .map(UUID::fromString).collect(Collectors.toSet()))
                .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), List.of())));
    }

    private List<CiCdJobRunTestsFilter.DISTINCT> getStacks(List<String> stacks) {
        return CollectionUtils.emptyIfNull(stacks).stream()
                .map(CiCdJobRunTestsFilter.DISTINCT::fromString)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
