package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.CICDScmJobRunDTO;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.filters.CiCdScmFilter;
import io.levelops.commons.databases.models.filters.DefaultListRequestUtils;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.CiCdAggsService;
import io.levelops.commons.databases.services.CiCdScmCombinedAggsService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.api.converters.DefaultListRequestUtils.getListOfObjectOrDefault;
import static io.levelops.api.converters.DefaultListRequestUtils.parseCiCdJobRunParameters;
import static io.levelops.api.converters.DefaultListRequestUtils.parseCiCdQualifiedJobNames;
import static io.levelops.api.converters.DefaultListRequestUtils.parseCiCdStacks;
import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;
import static io.levelops.commons.databases.models.filters.CiCdJobRunsFilter.parseCiCdJobRunsFilter;
import static io.levelops.commons.databases.models.filters.CiCdScmFilter.parseCiCdScmFilter;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN', 'AUDITOR', 'PUBLIC_DASHBOARD','ORG_ADMIN_USER')")
@RequestMapping("/v1/cicd_scm")
public class CiCdScmCombinedAggregationController {
    private static final boolean VALUES_ONLY = true;
    private static final boolean NOT_VALUES_ONLY = false;

    private final ObjectMapper objectMapper;
    private final CiCdAggsService ciCdAggsService;
    private final CiCdScmCombinedAggsService ciCdScmCombinedAggsService;
    private final OrgUnitHelper orgUnitHelper;

    @Autowired
    public CiCdScmCombinedAggregationController(ObjectMapper objectMapper, CiCdAggsService ciCdAggsService,
                                                CiCdScmCombinedAggsService ciCdScmCombinedAggsService, final OrgUnitHelper orgUnitHelper) {
        this.objectMapper = objectMapper;
        this.ciCdAggsService = ciCdAggsService;
        this.ciCdScmCombinedAggsService = ciCdScmCombinedAggsService;
        this.orgUnitHelper = orgUnitHelper;
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/job_counts", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getJobCountAggs(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getCICDIntegrationTypes(), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/cicd_scm/job_counts' for the request: {}", company, originalRequest, e);
        }
        log.debug("filter = {}", request);
        Map<String, String> endTimeRange = request.getFilterValue("end_time", Map.class)
                .orElse(Map.of());
        Map<String, String> startTimeRange = request.getFilterValue("start_time", Map.class)
                .orElse(Map.of());
        final Long endTimeStart = endTimeRange.get("$gt") != null ? Long.valueOf(endTimeRange.get("$gt")) : null;
        final Long endTimeEnd = endTimeRange.get("$lt") != null ? Long.valueOf(endTimeRange.get("$lt")) : null;
        final Long startTimeStart = startTimeRange.get("$gt") != null ? Long.valueOf(startTimeRange.get("$gt")) : null;
        final Long startTimeEnd = startTimeRange.get("$lt") != null ? Long.valueOf(startTimeRange.get("$lt")) : null;
        DefaultListRequest finalRequest = request;
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) finalRequest.getFilter().get("partial_match"));
        Map<String, Object> excludeFields = (Map<String, Object>) finalRequest.getFilter()
                .getOrDefault("exclude", Map.of());
        OUConfiguration finalOuConfig = ouConfig;
        return SpringUtils.deferResponse(() -> ResponseEntity.ok().body(
                PaginatedResponse.of(
                        finalRequest.getPage(),
                        finalRequest.getPageSize(),
                        ciCdAggsService.groupByAndCalculateCiCdJobRuns(
                                        company,
                                        CiCdJobRunsFilter.builder()
                                                .across(MoreObjects.firstNonNull(
                                                        CiCdJobRunsFilter.DISTINCT.fromString(
                                                                finalRequest.getAcross()),
                                                        CiCdJobRunsFilter.DISTINCT.trend))
                                                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                                                .stacks(parseCiCdStacks(finalRequest.getStacks(), CiCdJobRunsFilter.DISTINCT.class))
                                                .cicdUserIds(getListOrDefault(finalRequest.getFilter(), "cicd_user_ids"))
                                                .services(DefaultListRequestUtils.getListOrDefault(finalRequest, "services"))
                                                .environments(DefaultListRequestUtils.getListOrDefault(finalRequest, "environments"))
                                                .infrastructures(DefaultListRequestUtils.getListOrDefault(finalRequest, "infrastructures"))
                                                .repositories(DefaultListRequestUtils.getListOrDefault(finalRequest, "repositories"))
                                                .branches(DefaultListRequestUtils.getListOrDefault(finalRequest, "branches"))
                                                .deploymentTypes(DefaultListRequestUtils.getListOrDefault(finalRequest, "deployment_types"))
                                                .rollback((Boolean) MapUtils.emptyIfNull(finalRequest.getFilter()).getOrDefault("rollback", null))
                                                .tags(DefaultListRequestUtils.getListOrDefault(finalRequest, "tags"))
                                                .jobNames(getListOrDefault(finalRequest.getFilter(), "job_names"))
                                                .jobNormalizedFullNames(getListOrDefault(finalRequest.getFilter(), "job_normalized_full_names"))
                                                .jobStatuses(getListOrDefault(finalRequest.getFilter(), "job_statuses"))
                                                .instanceNames(getListOrDefault(finalRequest.getFilter(), "instance_names"))
                                                .integrationIds(getIntegrationIds(finalRequest))
                                                .types(CICD_TYPE.parseFromFilter(finalRequest))
                                                .stageNames(getListOrDefault(finalRequest.getFilter(), "stage_name"))
                                                .stepNames(getListOrDefault(finalRequest.getFilter(), "step_name"))
                                                .stageStatuses(getListOrDefault(finalRequest.getFilter(), "stage_status"))
                                                .stepStatuses(getListOrDefault(finalRequest.getFilter(), "step_status"))
                                                .projects(getListOrDefault(finalRequest.getFilter(), "projects"))
                                                .triageRuleNames(getListOrDefault(finalRequest.getFilter(), "triage_rule"))
                                                .excludeServices(DefaultListRequestUtils.getListOrDefault(excludeFields, "services"))
                                                .excludeEnvironments(DefaultListRequestUtils.getListOrDefault(excludeFields, "environments"))
                                                .excludeInfrastructures(DefaultListRequestUtils.getListOrDefault(excludeFields, "infrastructures"))
                                                .excludeRepositories(DefaultListRequestUtils.getListOrDefault(excludeFields, "repositories"))
                                                .excludeBranches(DefaultListRequestUtils.getListOrDefault(excludeFields, "branches"))
                                                .excludeDeploymentTypes(DefaultListRequestUtils.getListOrDefault(excludeFields, "deployment_types"))
                                                .excludeRollback((Boolean) MapUtils.emptyIfNull(excludeFields).getOrDefault("rollback", null))
                                                .excludeTags(DefaultListRequestUtils.getListOrDefault(excludeFields, "tags"))
                                                .excludeTriageRuleNames(getListOrDefault(excludeFields, "triage_rule"))
                                                .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                                                .excludeJobNormalizedFullNames(getListOrDefault(excludeFields, "job_normalized_full_names"))
                                                .excludeJobStatuses(getListOrDefault(excludeFields, "job_statuses"))
                                                .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                                                .excludeProjects(getListOrDefault(excludeFields, "projects"))
                                                .excludeStageNames(getListOrDefault(excludeFields, "stage_name"))
                                                .excludeStepNames(getListOrDefault(excludeFields, "step_name"))
                                                .excludeStageStatuses(getListOrDefault(excludeFields, "stage_status"))
                                                .excludeStepStatuses(getListOrDefault(excludeFields, "step_status"))
                                                .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                                                .excludeCiCdUserIds(DefaultListRequestUtils.getListOrDefault(excludeFields, "cicd_user_ids"))
                                                .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                                                .partialMatch(partialMatchMap)
                                                .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(finalRequest.getFilter(), "parameters")))
                                                .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(finalRequest.getFilter(), "qualified_job_names")))
                                                .endTimeRange(ImmutablePair.of(endTimeStart, endTimeEnd))
                                                .startTimeRange(ImmutablePair.of(startTimeStart, startTimeEnd))
                                                .orgProductsIds(getListOrDefault(finalRequest.getFilter(), "org_product_ids").stream()
                                                        .map(UUID::fromString).collect(Collectors.toSet()))
                                                .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(finalRequest.getAggInterval()),
                                                        CICD_AGG_INTERVAL.day))
                                                .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(finalRequest.getSort(), List.of())))
                                                .build(), NOT_VALUES_ONLY, finalOuConfig)
                                .getRecords())));
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/job_durations", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getJobDurationAggs(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getCICDIntegrationTypes(), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/cicd_scm/job_durations' for the request: {}", company, originalRequest, e);
        }
        log.debug("filter = {}", request);
        Map<String, String> endTimeRange = request.getFilterValue("end_time", Map.class)
                .orElse(Map.of());
        Map<String, String> startTimeRange = request.getFilterValue("start_time", Map.class)
                .orElse(Map.of());
        final Long endTimeStart = endTimeRange.get("$gt") != null ? Long.valueOf(endTimeRange.get("$gt")) : null;
        final Long endTimeEnd = endTimeRange.get("$lt") != null ? Long.valueOf(endTimeRange.get("$lt")) : null;
        final Long startTimeStart = startTimeRange.get("$gt") != null ? Long.valueOf(startTimeRange.get("$gt")) : null;
        final Long startTimeEnd = startTimeRange.get("$lt") != null ? Long.valueOf(startTimeRange.get("$lt")) : null;
        DefaultListRequest finalRequest = request;
        Map<String, Object> excludeFields = (Map<String, Object>) finalRequest.getFilter()
                .getOrDefault("exclude", Map.of());
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) finalRequest.getFilter().get("partial_match"));
        OUConfiguration finalOuConfig = ouConfig;
        return SpringUtils.deferResponse(() -> ResponseEntity.ok().body(
                PaginatedResponse.of(
                        finalRequest.getPage(),
                        finalRequest.getPageSize(),
                        ciCdAggsService.groupByAndCalculateCiCdJobRuns(
                                        company,
                                        CiCdJobRunsFilter.builder()
                                                .across(MoreObjects.firstNonNull(
                                                        CiCdJobRunsFilter.DISTINCT.fromString(
                                                                finalRequest.getAcross()),
                                                        CiCdJobRunsFilter.DISTINCT.trend))
                                                .calculation(CiCdJobRunsFilter.CALCULATION.duration)
                                                .stacks(parseCiCdStacks(finalRequest.getStacks(), CiCdJobRunsFilter.DISTINCT.class))
                                                .cicdUserIds(getListOrDefault(finalRequest.getFilter(), "cicd_user_ids"))
                                                .services(DefaultListRequestUtils.getListOrDefault(finalRequest, "services"))
                                                .environments(DefaultListRequestUtils.getListOrDefault(finalRequest, "environments"))
                                                .infrastructures(DefaultListRequestUtils.getListOrDefault(finalRequest, "infrastructures"))
                                                .repositories(DefaultListRequestUtils.getListOrDefault(finalRequest, "repositories"))
                                                .branches(DefaultListRequestUtils.getListOrDefault(finalRequest, "branches"))
                                                .deploymentTypes(DefaultListRequestUtils.getListOrDefault(finalRequest, "deployment_types"))
                                                .rollback((Boolean) MapUtils.emptyIfNull(finalRequest.getFilter()).getOrDefault("rollback", null))
                                                .tags(DefaultListRequestUtils.getListOrDefault(finalRequest, "tags"))
                                                .jobNames(getListOrDefault(finalRequest.getFilter(), "job_names"))
                                                .jobNormalizedFullNames(getListOrDefault(finalRequest.getFilter(), "job_normalized_full_names"))
                                                .jobStatuses(getListOrDefault(finalRequest.getFilter(), "job_statuses"))
                                                .instanceNames(getListOrDefault(finalRequest.getFilter(), "instance_names"))
                                                .integrationIds(getIntegrationIds(finalRequest))
                                                .types(CICD_TYPE.parseFromFilter(finalRequest))
                                                .projects(getListOrDefault(finalRequest.getFilter(), "projects"))
                                                .excludeServices(DefaultListRequestUtils.getListOrDefault(excludeFields, "services"))
                                                .excludeEnvironments(DefaultListRequestUtils.getListOrDefault(excludeFields, "environments"))
                                                .excludeInfrastructures(DefaultListRequestUtils.getListOrDefault(excludeFields, "infrastructures"))
                                                .excludeRepositories(DefaultListRequestUtils.getListOrDefault(excludeFields, "repositories"))
                                                .excludeBranches(DefaultListRequestUtils.getListOrDefault(excludeFields, "branches"))
                                                .excludeDeploymentTypes(DefaultListRequestUtils.getListOrDefault(excludeFields, "deployment_types"))
                                                .excludeRollback((Boolean) MapUtils.emptyIfNull(excludeFields).getOrDefault("rollback", null))
                                                .excludeTags(DefaultListRequestUtils.getListOrDefault(excludeFields, "tags"))
                                                .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                                                .excludeJobNormalizedFullNames(getListOrDefault(excludeFields, "job_normalized_full_names"))
                                                .excludeJobStatuses(getListOrDefault(excludeFields, "job_statuses"))
                                                .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                                                .excludeProjects(getListOrDefault(excludeFields, "projects"))
                                                .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                                                .excludeCiCdUserIds(DefaultListRequestUtils.getListOrDefault(excludeFields, "cicd_user_ids"))
                                                .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                                                .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(finalRequest.getFilter(), "parameters")))
                                                .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(finalRequest.getFilter(), "qualified_job_names")))
                                                .endTimeRange(ImmutablePair.of(endTimeStart, endTimeEnd))
                                                .startTimeRange(ImmutablePair.of(startTimeStart, startTimeEnd))
                                                .orgProductsIds(getListOrDefault(finalRequest.getFilter(), "org_product_ids").stream()
                                                        .map(UUID::fromString).collect(Collectors.toSet()))
                                                .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(finalRequest.getAggInterval()),
                                                        CICD_AGG_INTERVAL.day))
                                                .partialMatch(partialMatchMap)
                                                .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(finalRequest.getSort(), List.of())))
                                                .build(), NOT_VALUES_ONLY, finalOuConfig)
                                .getRecords())));
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/jobs_commit_lead_time", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getJobCommitLeadTimeAggs(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getCICDIntegrationTypes(), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/cicd_scm/jobs_commit_lead_time' for the request: {}", company, originalRequest, e);
        }
        Map<String, String> endTimeRange = request.getFilterValue("end_time", Map.class)
                .orElse(Map.of());
        Map<String, String> startTimeRange = request.getFilterValue("start_time", Map.class)
                .orElse(Map.of());
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
        Map<String, Object> excludeFields
                = (Map<String, Object>) request.getFilter().getOrDefault("exclude", Map.of());

        final Long endTimeStart = endTimeRange.get("$gt") != null ? Long.valueOf(endTimeRange.get("$gt")) : null;
        final Long endTimeEnd = endTimeRange.get("$lt") != null ? Long.valueOf(endTimeRange.get("$lt")) : null;
        final Long startTimeStart = startTimeRange.get("$gt") != null ? Long.valueOf(startTimeRange.get("$gt")) : null;
        final Long startTimeEnd = startTimeRange.get("$lt") != null ? Long.valueOf(startTimeRange.get("$lt")) : null;
        DefaultListRequest finalRequest = request;
        OUConfiguration finalOuConfig = ouConfig;
        return SpringUtils.deferResponse(() -> ResponseEntity.ok().body(
                PaginatedResponse.of(
                        finalRequest.getPage(),
                        finalRequest.getPageSize(),
                        ciCdScmCombinedAggsService.groupByAndCalculate(
                                        company,
                                        CiCdScmFilter.builder()
                                                .across(MoreObjects.firstNonNull(
                                                        CiCdScmFilter.DISTINCT.fromString(
                                                                finalRequest.getAcross()),
                                                        CiCdScmFilter.DISTINCT.trend))
                                                .calculation(CiCdScmFilter.CALCULATION.lead_time)
                                                .stacks(parseCiCdStacks(finalRequest.getStacks(), CiCdScmFilter.DISTINCT.class))
                                                .authors(getListOrDefault(finalRequest.getFilter(), "authors"))
                                                .cicdUserIds(getListOrDefault(finalRequest.getFilter(), "cicd_user_ids"))
                                                .jobNames(getListOrDefault(finalRequest.getFilter(), "job_names"))
                                                .jobNormalizedFullNames(getListOrDefault(finalRequest.getFilter(), "job_normalized_full_names"))
                                                .jobStatuses(getListOrDefault(finalRequest.getFilter(), "job_statuses"))
                                                .integrationIds(getListOrDefault(finalRequest.getFilter(), "integration_ids"))
                                                .cicdIntegrationIds(getListOrDefault(finalRequest.getFilter(), "cicd_integration_ids"))
                                                .types(CICD_TYPE.parseFromFilter(finalRequest))
                                                .instanceNames(getListOrDefault(finalRequest.getFilter(), "instance_names"))
                                                .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(finalRequest.getFilter(), "parameters")))
                                                .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(finalRequest.getFilter(), "qualified_job_names")))
                                                .endTimeRange(ImmutablePair.of(endTimeStart, endTimeEnd))
                                                .startTimeRange(ImmutablePair.of(startTimeStart, startTimeEnd))
                                                .orgProductsIds(getListOrDefault(finalRequest.getFilter(), "org_product_ids").stream()
                                                        .map(UUID::fromString).collect(Collectors.toSet()))
                                                .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(finalRequest.getAggInterval()),
                                                        CICD_AGG_INTERVAL.day))
                                                .repos(getListOrDefault(finalRequest.getFilter(), "repos"))
                                                .projects(getListOrDefault(finalRequest.getFilter(), "projects"))
                                                .excludeRepos(getListOrDefault(excludeFields, "repos"))
                                                .excludeProjects(getListOrDefault(excludeFields, "projects"))
                                                .excludeAuthors(getListOrDefault(excludeFields, "authors"))
                                                .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                                                .excludeJobNormalizedFullNames(getListOrDefault(excludeFields, "job_normalized_full_names"))
                                                .excludeJobStatuses(getListOrDefault(excludeFields, "job_statuses"))
                                                .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                                                .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                                                .excludeCiCdUserIds(getListOrDefault(excludeFields, "cicd_user_ids"))
                                                .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                                                .partialMatch(partialMatchMap)
                                                .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(finalRequest.getSort(), List.of())))
                                                .build(), NOT_VALUES_ONLY, finalOuConfig)
                                .getRecords())));
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/jobs_change_volumes", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getJobChangeVolumeAggs(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getCICDIntegrationTypes(), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/cicd_scm/jobs_change_volumes' for the request: {}", company, originalRequest, e);
        }
        Map<String, String> endTimeRange = request.getFilterValue("end_time", Map.class)
                .orElse(Map.of());
        Map<String, String> startTimeRange = request.getFilterValue("start_time", Map.class)
                .orElse(Map.of());
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
        Map<String, Object> excludeFields =
                (Map<String, Object>) request.getFilter().getOrDefault("exclude", Map.of());
        final Long endTimeStart = endTimeRange.get("$gt") != null ? Long.valueOf(endTimeRange.get("$gt")) : null;
        final Long endTimeEnd = endTimeRange.get("$lt") != null ? Long.valueOf(endTimeRange.get("$lt")) : null;
        final Long startTimeStart = startTimeRange.get("$gt") != null ? Long.valueOf(startTimeRange.get("$gt")) : null;
        final Long startTimeEnd = startTimeRange.get("$lt") != null ? Long.valueOf(startTimeRange.get("$lt")) : null;
        DefaultListRequest finalRequest = request;
        OUConfiguration finalOuConfig = ouConfig;
        return SpringUtils.deferResponse(() -> ResponseEntity.ok().body(
                PaginatedResponse.of(
                        finalRequest.getPage(),
                        finalRequest.getPageSize(),
                        ciCdScmCombinedAggsService.groupByAndCalculate(
                                        company,
                                        CiCdScmFilter.builder()
                                                .across(MoreObjects.firstNonNull(
                                                        CiCdScmFilter.DISTINCT.fromString(
                                                                finalRequest.getAcross()),
                                                        CiCdScmFilter.DISTINCT.trend))
                                                .calculation(CiCdScmFilter.CALCULATION.change_volume)
                                                .stacks(parseCiCdStacks(finalRequest.getStacks(), CiCdScmFilter.DISTINCT.class))
                                                .authors(getListOrDefault(finalRequest.getFilter(), "authors"))
                                                .cicdUserIds(getListOrDefault(finalRequest.getFilter(), "cicd_user_ids"))
                                                .jobNames(getListOrDefault(finalRequest.getFilter(), "job_names"))
                                                .jobNormalizedFullNames(getListOrDefault(finalRequest.getFilter(), "job_normalized_full_names"))
                                                .jobStatuses(getListOrDefault(finalRequest.getFilter(), "job_statuses"))
                                                .integrationIds(getListOrDefault(finalRequest.getFilter(), "integration_ids"))
                                                .cicdIntegrationIds(getListOrDefault(finalRequest.getFilter(), "cicd_integration_ids"))
                                                .types(CICD_TYPE.parseFromFilter(finalRequest))
                                                .projects(getListOrDefault(finalRequest.getFilter(), "projects"))
                                                .instanceNames(getListOrDefault(finalRequest.getFilter(), "instance_names"))
                                                .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(finalRequest.getFilter(), "parameters")))
                                                .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(finalRequest.getFilter(), "qualified_job_names")))
                                                .endTimeRange(ImmutablePair.of(endTimeStart, endTimeEnd))
                                                .startTimeRange(ImmutablePair.of(startTimeStart, startTimeEnd))
                                                .orgProductsIds(getListOrDefault(finalRequest.getFilter(), "org_product_ids").stream()
                                                        .map(UUID::fromString).collect(Collectors.toSet()))
                                                .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(finalRequest.getAggInterval()),
                                                        CICD_AGG_INTERVAL.day))
                                                .repos(getListOrDefault(finalRequest.getFilter(), "repos"))
                                                .excludeRepos(getListOrDefault(excludeFields, "repos"))
                                                .excludeProjects(getListOrDefault(excludeFields, "projects"))
                                                .excludeAuthors(getListOrDefault(excludeFields, "authors"))
                                                .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                                                .excludeJobNormalizedFullNames(getListOrDefault(excludeFields, "job_normalized_full_names"))
                                                .excludeJobStatuses(getListOrDefault(excludeFields, "job_statuses"))
                                                .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                                                .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                                                .excludeCiCdUserIds(getListOrDefault(excludeFields, "cicd_user_ids"))
                                                .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                                                .partialMatch(partialMatchMap)
                                                .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(finalRequest.getSort(), List.of())))
                                                .build(), NOT_VALUES_ONLY, finalOuConfig)
                                .getRecords())));
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getCiCdScmAggValues(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getCICDIntegrationTypes(), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/cicd_scm/values' for the request: {}", company, originalRequest, e);
        }
        Map<String, String> endTimeRange = request.getFilterValue("end_time", Map.class)
                .orElse(Map.of());
        Map<String, String> startTimeRange = request.getFilterValue("start_time", Map.class)
                .orElse(Map.of());
        final Long endTimeStart = endTimeRange.get("$gt") != null ? Long.valueOf(endTimeRange.get("$gt")) : null;
        final Long endTimeEnd = endTimeRange.get("$lt") != null ? Long.valueOf(endTimeRange.get("$lt")) : null;
        final Long startTimeStart = startTimeRange.get("$gt") != null ? Long.valueOf(startTimeRange.get("$gt")) : null;
        final Long startTimeEnd = startTimeRange.get("$lt") != null ? Long.valueOf(startTimeRange.get("$lt")) : null;
        DefaultListRequest finalRequest = request;
        Map<String, Object> excludeFields =
                (Map<String, Object>) finalRequest.getFilter().getOrDefault("exclude", Map.of());
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) finalRequest.getFilter().get("partial_match"));
        OUConfiguration finalOuConfig = ouConfig;
        return SpringUtils.deferResponse(() -> {
            if (CollectionUtils.isEmpty(finalRequest.getFields())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            }
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            for (String value : finalRequest.getFields()) {
                List<DbAggregationResult> dbAggregationResults = ciCdScmCombinedAggsService.groupByAndCalculate(
                                company,
                                CiCdScmFilter.builder()
                                        .across(
                                                CiCdScmFilter.DISTINCT.fromString(
                                                        value))
                                        .calculation(CiCdScmFilter.CALCULATION.count)
                                        .authors(getListOrDefault(finalRequest.getFilter(), "authors"))
                                        .cicdUserIds(getListOrDefault(finalRequest.getFilter(), "cicd_user_ids"))
                                        .jobNames(getListOrDefault(finalRequest.getFilter(), "job_names"))
                                        .jobStatuses(getListOrDefault(finalRequest.getFilter(), "job_statuses"))
                                        .integrationIds(getListOrDefault(finalRequest.getFilter(), "integration_ids"))
                                        .cicdIntegrationIds(getListOrDefault(finalRequest.getFilter(), "cicd_integration_ids"))
                                        .types(CICD_TYPE.parseFromFilter(finalRequest))
                                        .projects(getListOrDefault(finalRequest.getFilter(), "projects"))
                                        .instanceNames(getListOrDefault(finalRequest.getFilter(), "instance_names"))
                                        .jobNormalizedFullNames(getListOrDefault(finalRequest.getFilter(), "job_normalized_full_names"))
                                        .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(finalRequest.getFilter(), "parameters")))
                                        .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(finalRequest.getFilter(), "qualified_job_names")))
                                        .endTimeRange(ImmutablePair.of(endTimeStart, endTimeEnd))
                                        .startTimeRange(ImmutablePair.of(startTimeStart, startTimeEnd))
                                        .repos(getListOrDefault(finalRequest.getFilter(), "repos"))
                                        .excludeRepos(getListOrDefault(excludeFields, "repos"))
                                        .excludeProjects(getListOrDefault(excludeFields, "projects"))
                                        .excludeAuthors(getListOrDefault(excludeFields, "authors"))
                                        .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                                        .excludeJobNormalizedFullNames(getListOrDefault(excludeFields, "job_normalized_full_names"))
                                        .excludeJobStatuses(getListOrDefault(excludeFields, "job_statuses"))
                                        .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                                        .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                                        .excludeCiCdUserIds(getListOrDefault(excludeFields, "cicd_user_ids"))
                                        .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                                        .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(finalRequest.getAggInterval()),
                                                CICD_AGG_INTERVAL.day))
                                        .partialMatch(partialMatchMap)
                                        .orgProductsIds(getListOrDefault(finalRequest.getFilter(), "org_product_ids").stream()
                                                .map(UUID::fromString).collect(Collectors.toSet()))
                                        .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(List.of(Map.of("id", value, "desc", false)), List.of())))
                                        .build(), VALUES_ONLY, finalOuConfig)
                        .getRecords();
                log.debug("value = {}, dbAggregationResults = {}", value, dbAggregationResults);
                response.add(Map.of(value, dbAggregationResults));
            }
            log.debug("response = {}", response);
            return ResponseEntity.ok().body(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<CICDScmJobRunDTO>>> getCiCdScmCombinedAggsList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getCICDIntegrationTypes(), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/cicd_scm/list' for the request: {}", company, originalRequest, e);
        }
        Map<String, String> endTimeRange = request.getFilterValue("end_time", Map.class)
                .orElse(Map.of());
        Map<String, String> startTimeRange = request.getFilterValue("start_time", Map.class)
                .orElse(Map.of());
        final Long endTimeStart = endTimeRange.get("$gt") != null ? Long.valueOf(endTimeRange.get("$gt")) : null;
        final Long endTimeEnd = endTimeRange.get("$lt") != null ? Long.valueOf(endTimeRange.get("$lt")) : null;
        final Long startTimeStart = startTimeRange.get("$gt") != null ? Long.valueOf(startTimeRange.get("$gt")) : null;
        final Long startTimeEnd = startTimeRange.get("$lt") != null ? Long.valueOf(startTimeRange.get("$lt")) : null;
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
        log.debug("filter = {}", request);
        DefaultListRequest finalRequest = request;
        OUConfiguration finalOuConfig = ouConfig;
        return SpringUtils.deferResponse(() -> {
            Map<String, String> startedRange = finalRequest.getFilterValue("job_started_at", Map.class)
                    .orElse(Map.of());
            Long jobStart = startedRange.get("$gt") != null ? Long.valueOf(startedRange.get("$gt")) : null;
            Long jobEnd = startedRange.get("$lt") != null ? Long.valueOf(startedRange.get("$lt")) : null;
            Map<String, Object> excludeFields = (Map<String, Object>) finalRequest.getFilter()
                    .getOrDefault("exclude", Map.of());
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            finalRequest.getPage(),
                            finalRequest.getPageSize(),
                            ciCdScmCombinedAggsService.listCiCdScmCombinedData(
                                    company,
                                    CiCdScmFilter.builder()
                                            .authors(getListOrDefault(finalRequest.getFilter(), "authors"))
                                            .cicdUserIds(getListOrDefault(finalRequest.getFilter(), "cicd_user_ids"))
                                            .jobNames(getListOrDefault(finalRequest.getFilter(), "job_names"))
                                            .jobNormalizedFullNames(getListOrDefault(finalRequest.getFilter(), "job_normalized_full_names"))
                                            .jobStatuses(getListOrDefault(finalRequest.getFilter(), "job_statuses"))
                                            .integrationIds(getListOrDefault(finalRequest.getFilter(), "integration_ids"))
                                            .cicdIntegrationIds(getListOrDefault(finalRequest.getFilter(), "cicd_integration_ids"))
                                            .types(CICD_TYPE.parseFromFilter(finalRequest))
                                            .projects(getListOrDefault(finalRequest.getFilter(), "projects"))
                                            .instanceNames(getListOrDefault(finalRequest.getFilter(), "instance_names"))
                                            .jobStartTime(jobStart)
                                            .jobEndTime(jobEnd)
                                            .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(finalRequest.getFilter(), "parameters")))
                                            .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(finalRequest.getFilter(), "qualified_job_names")))
                                            .endTimeRange(ImmutablePair.of(endTimeStart, endTimeEnd))
                                            .startTimeRange(ImmutablePair.of(startTimeStart, startTimeEnd))
                                            .repos(getListOrDefault(finalRequest.getFilter(), "repos"))
                                            .excludeRepos(getListOrDefault(excludeFields, "repos"))
                                            .excludeProjects(getListOrDefault(excludeFields, "projects"))
                                            .excludeAuthors(getListOrDefault(excludeFields, "authors"))
                                            .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                                            .excludeJobNormalizedFullNames(getListOrDefault(excludeFields, "job_normalized_full_names"))
                                            .excludeJobStatuses(getListOrDefault(excludeFields, "job_statuses"))
                                            .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                                            .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                                            .excludeCiCdUserIds(getListOrDefault(excludeFields, "cicd_user_ids"))
                                            .partialMatch(partialMatchMap)
                                            .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                                            .orgProductsIds(getListOrDefault(finalRequest.getFilter(), "org_product_ids").stream()
                                                    .map(UUID::fromString).collect(Collectors.toSet()))
                                            .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(finalRequest.getSort(), List.of())))
                                            .build(),
                                    finalRequest.getPage(),
                                    finalRequest.getPageSize(),
                                    finalOuConfig
                            )
                    ));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/deploy_job_change_volume", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getDeployJobChangeVolume(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) throws BadRequestException {
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JENKINS, IntegrationType.CIRCLECI, IntegrationType.DRONECI, IntegrationType.HARNESSNG,
                    IntegrationType.AZURE_PIPELINES, IntegrationType.AZURE_DEVOPS, IntegrationType.GITLAB), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/cicd_scm/deploy_job_change_volume' for the request: {}", company, originalRequest, e);
        }
        Map<String, Object> deployJobRequest = request.getFilterValue("deploy_job", Map.class).orElse(Map.of());
        Map<String, Object> buildJobRequest = request.getFilterValue("build_job", Map.class).orElse(Map.of());
        CiCdJobRunsFilter deployJobFilter = parseCiCdJobRunsFilter(DefaultListRequest.builder().filter(deployJobRequest).build(), objectMapper);
        CiCdScmFilter buildJobFilter = parseCiCdScmFilter(DefaultListRequest.builder().filter(buildJobRequest).build(), objectMapper);
        if ((buildJobFilter.getJobNames() == null || CollectionUtils.isEmpty(buildJobFilter.getJobNames())) &&
                (buildJobFilter.getJobNormalizedFullNames() == null || CollectionUtils.isEmpty(buildJobFilter.getJobNormalizedFullNames()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected either build job names or build job normalized full names, but both are empty");
        }
        if ((deployJobFilter.getJobNames() == null || CollectionUtils.isEmpty(deployJobFilter.getJobNames())) &&
                (deployJobFilter.getJobNormalizedFullNames() == null || CollectionUtils.isEmpty(deployJobFilter.getJobNormalizedFullNames()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected either deploy job names or deploy job normalized full names, but both are empty");
        }
        log.debug("filter = {}", request);
        DefaultListRequest finalRequest = request;
        var aggInterval = MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(finalRequest.getAggInterval()), CICD_AGG_INTERVAL.day);
        OUConfiguration finalOuConfig = ouConfig;
        return SpringUtils.deferResponse(() -> ResponseEntity.ok().body(
                PaginatedResponse.of(
                        finalRequest.getPage(),
                        finalRequest.getPageSize(),
                        ciCdScmCombinedAggsService.computeDeployJobChangeVolume(
                                company, deployJobFilter, buildJobFilter, CiCdScmFilter.DISTINCT.job_end, aggInterval, finalOuConfig)
                )));
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/deploy_job_change_volume/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<CICDScmJobRunDTO>>> getDeployJobChangeVolumeList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) throws BadRequestException {
        var request = filter;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JENKINS, IntegrationType.CIRCLECI, IntegrationType.DRONECI, IntegrationType.HARNESSNG,
                    IntegrationType.AZURE_PIPELINES, IntegrationType.AZURE_DEVOPS, IntegrationType.GITLAB), filter);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/cicd_scm/deploy_job_change_volume/drilldown' for the request: {}", company, filter, e);
        }
        Map<String, Object> deployJobRequest = request.getFilterValue("deploy_job", Map.class).orElse(Map.of());
        Map<String, Object> buildJobRequest = request.getFilterValue("build_job", Map.class).orElse(Map.of());
        CiCdJobRunsFilter deployJobFilter = parseCiCdJobRunsFilter(DefaultListRequest.builder().filter(deployJobRequest).build(), objectMapper);
        CiCdScmFilter buildJobFilter = parseCiCdScmFilter(DefaultListRequest.builder().filter(buildJobRequest).build(), objectMapper);
        if ((buildJobFilter.getJobNames() == null || CollectionUtils.isEmpty(buildJobFilter.getJobNames())) &&
                (buildJobFilter.getJobNormalizedFullNames() == null || CollectionUtils.isEmpty(buildJobFilter.getJobNormalizedFullNames()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected either build job names or build job normalized full names, but both are empty");
        }
        if ((deployJobFilter.getJobNames() == null || CollectionUtils.isEmpty(deployJobFilter.getJobNames())) &&
                (deployJobFilter.getJobNormalizedFullNames() == null || CollectionUtils.isEmpty(deployJobFilter.getJobNormalizedFullNames()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected either deploy job names or deploy job normalized full names, but both are empty");
        }
        Map<String, String> deployEndTimeRange = request.getFilterValue("end_time", Map.class)
                .orElse(Map.of());
        final Long deployEndTimeStart = deployEndTimeRange.get("$gt") != null ? Long.valueOf(deployEndTimeRange.get("$gt")) : null;
        final Long deployEndTimeEnd = deployEndTimeRange.get("$lt") != null ? Long.valueOf(deployEndTimeRange.get("$lt")) : null;

        ImmutablePair<Long, Long> deployEndTimeRangePair = ImmutablePair.of(deployEndTimeStart, deployEndTimeEnd);

        log.debug("filter = {}", request);
        final var finalOuConfig = ouConfig;
        final var page = request.getPage();
        final var pageSize = request.getPageSize();
        return SpringUtils.deferResponse(() ->
                ResponseEntity.ok().body(
                        PaginatedResponse.of(
                                page,
                                pageSize,
                                ciCdScmCombinedAggsService.computeDeployJobChangeVolumeDrillDown(
                                        company, deployJobFilter, buildJobFilter, deployEndTimeRangePair, page,
                                        pageSize, finalOuConfig
                                )
                        )));
    }

    private List<String> getIntegrationIds(DefaultListRequest filter) {
        return CollectionUtils.isNotEmpty(getListOrDefault(filter.getFilter(), "cicd_integration_ids")) ?
                getListOrDefault(filter.getFilter(), "cicd_integration_ids") : getListOrDefault(filter.getFilter(), "integration_ids");
    }
}
