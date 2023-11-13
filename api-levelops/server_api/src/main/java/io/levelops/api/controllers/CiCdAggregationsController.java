package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.CICDJobConfigChangeDTO;
import io.levelops.commons.databases.models.database.CICDJobRunDTO;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobConfigChangesFilter;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.CiCdAggsService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
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
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'AUDITOR', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
@RequestMapping("/v1/cicd")
public class CiCdAggregationsController {
    private static final boolean VALUES_ONLY = true;
    private static final boolean NOT_VALUES_ONLY = false;

    private final ObjectMapper objectMapper;
    private final CiCdAggsService ciCdAggsService;
    private final OrgUnitHelper ouHelper;

    @Autowired
    public CiCdAggregationsController(ObjectMapper objectMapper, CiCdAggsService ciCdAggsService, final OrgUnitHelper ouHelper) {
        this.objectMapper = objectMapper;
        this.ciCdAggsService = ciCdAggsService;
        this.ouHelper = ouHelper;
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/job_config_changes", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getJobConfigChangesAggs(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        log.debug("filter = {}", originalRequest);
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JENKINS, IntegrationType.CIRCLECI, IntegrationType.DRONECI, IntegrationType.HARNESSNG,
                        IntegrationType.AZURE_PIPELINES, IntegrationType.AZURE_DEVOPS, IntegrationType.GITLAB), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/cicd/job_config_changes' for the request: {}", company, originalRequest, e);
            }
            Map<String, String> changedTimeRange = request.getFilterValue("job_config_changed_at", Map.class)
                    .orElse(Map.of());
            Long changeTimeStart = changedTimeRange.get("$gt") != null ? Long.valueOf(changedTimeRange.get("$gt")) : null;
            Long changeTimeEnd = changedTimeRange.get("$lt") != null ? Long.valueOf(changedTimeRange.get("$lt")) : null;
            Map<String, Object> excludeFields = (Map<String, Object>) request.getFilter()
                .getOrDefault("exclude", Map.of());

            return ResponseEntity.ok().body(
                PaginatedResponse.of(
                        request.getPage(),
                        request.getPageSize(),
                        ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(
                                company,
                                CiCdJobConfigChangesFilter.builder()
                                        .across(MoreObjects.firstNonNull(
                                                CiCdJobConfigChangesFilter.DISTINCT.fromString(
                                                        request.getAcross()),
                                                CiCdJobConfigChangesFilter.DISTINCT.trend))
                                        .changeEndTime(changeTimeEnd)
                                        .changeStartTime(changeTimeStart)
                                        .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                                        .stacks(parseCiCdStacks(request.getStacks(), CiCdJobConfigChangesFilter.DISTINCT.class))
                                        .cicdUserIds(getListOrDefault(request.getFilter(), "cicd_user_ids"))
                                        .jobNames(getListOrDefault(request.getFilter(), "job_names"))
                                        .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                                        .types(CICD_TYPE.parseFromFilter(request))
                                        .projects(getListOrDefault(request.getFilter(), "projects"))
                                        .instanceNames(getListOrDefault(request.getFilter(), "instance_names"))
                                        .excludeProjects(getListOrDefault(excludeFields, "projects"))
                                        .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                                        .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                                        .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                                        .excludeCiCdUserIds(getListOrDefault(excludeFields, "cicd_user_ids"))
                                        .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                                        .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(request.getFilter(), "qualified_job_names")))
                                        .orgProductsIds(getListOrDefault(request.getFilter(), "org_product_ids").stream()
                                                .map(UUID::fromString).collect(Collectors.toSet()))
                                        .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of())))
                                        .build(), NOT_VALUES_ONLY, ouConfig)
                                .getRecords()));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/job_config_changes/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<CICDJobConfigChangeDTO>>> getJobConfigChangesList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        log.debug("filter = {}", originalRequest);
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JENKINS, IntegrationType.CIRCLECI, IntegrationType.DRONECI, IntegrationType.HARNESSNG,
                        IntegrationType.AZURE_PIPELINES, IntegrationType.AZURE_DEVOPS, IntegrationType.GITLAB), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/cide/job_config_changes/list' for the request: {}", company, originalRequest, e);
            }
            Map<String, String> changedTimeRange = request.getFilterValue("job_config_changed_at", Map.class)
                    .orElse(Map.of());
            Long changeTimeStart = changedTimeRange.get("$gt") != null ? Long.valueOf(changedTimeRange.get("$gt")) : null;
            Long changeTimeEnd = changedTimeRange.get("$lt") != null ? Long.valueOf(changedTimeRange.get("$lt")) : null;
            Map<String, Object> excludeFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            ciCdAggsService.listCiCdJobConfigChanges(
                                    company,
                                    CiCdJobConfigChangesFilter.builder()
                                            .cicdUserIds(getListOrDefault(request.getFilter(), "cicd_user_ids"))
                                            .jobNames(getListOrDefault(request.getFilter(), "job_names"))
                                            .instanceNames(getListOrDefault(request.getFilter(), "instance_names"))
                                            .changeStartTime(changeTimeStart)
                                            .changeEndTime(changeTimeEnd)
                                            .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                                            .types(CICD_TYPE.parseFromFilter(request))
                                            .projects(getListOrDefault(request.getFilter(), "projects"))
                                            .excludeProjects(getListOrDefault(excludeFields, "projects"))
                                            .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                                            .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                                            .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                                            .excludeCiCdUserIds(getListOrDefault(excludeFields, "cicd_user_ids"))
                                            .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                                            .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(request.getFilter(), "qualified_job_names")))
                                            .orgProductsIds(getListOrDefault(request.getFilter(), "org_product_ids").stream()
                                                    .map(UUID::fromString).collect(Collectors.toSet()))
                                            .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of())))
                                            .build(),
                                    ouConfig,
                                    request.getPage(),
                                    request.getPageSize()
                            )
                    ));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/job_config_changes/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getJobConfigChangesValues(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        log.debug("filter = {}", originalRequest);
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JENKINS, IntegrationType.CIRCLECI, IntegrationType.DRONECI, IntegrationType.HARNESSNG,
                        IntegrationType.AZURE_PIPELINES, IntegrationType.AZURE_DEVOPS, IntegrationType.GITLAB), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/cicd/job_config_changes/values' for the request: {}", company, originalRequest, e);
            }
            if (CollectionUtils.isEmpty(request.getFields()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            Map<String, Object> excludeFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            for (String value : request.getFields()) {
                List<DbAggregationResult> dbAggregationResults = ciCdAggsService.groupByAndCalculateCiCdJobConfigChanges(
                        company,
                        CiCdJobConfigChangesFilter.builder()
                                .across(CiCdJobConfigChangesFilter.DISTINCT.fromString(value))
                                .calculation(CiCdJobConfigChangesFilter.CALCULATION.count)
                                .cicdUserIds(getListOrDefault(request.getFilter(), "cicd_user_ids"))
                                .jobNames(getListOrDefault(request.getFilter(), "job_names"))
                                .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                                .types(CICD_TYPE.parseFromFilter(request))
                                .projects(getListOrDefault(request.getFilter(), "projects"))
                                .instanceNames(getListOrDefault(request.getFilter(), "instance_names"))
                                .excludeProjects(getListOrDefault(excludeFields, "projects"))
                                .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                                .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                                .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                                .excludeCiCdUserIds(getListOrDefault(excludeFields, "cicd_user_ids"))
                                .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                                .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(request.getFilter(), "qualified_job_names")))
                                .orgProductsIds(getListOrDefault(request.getFilter(), "org_product_ids").stream()
                                        .map(UUID::fromString).collect(Collectors.toSet()))
                                .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(List.of(Map.of("id", value, "desc", false)), List.of())))
                                .build(), VALUES_ONLY, ouConfig)
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
    @RequestMapping(method = RequestMethod.POST, value = "/job_runs/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<CICDJobRunDTO>>> getJobRunsList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        log.debug("filter = {}", originalRequest);
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.getCICDIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/cicd/job_runs/list' for the request: {}", company, originalRequest, e);
            }
            Map<String, String> jobStartedAtRange = request.getFilterValue("start_time", Map.class)
                    .orElse(Map.of());
            Long jobStartedRangeStart = jobStartedAtRange.get("$gt") != null ? Long.valueOf(jobStartedAtRange.get("$gt")) : null;
            Long jobStartedRangeEnd = jobStartedAtRange.get("$lt") != null ? Long.valueOf(jobStartedAtRange.get("$lt")) : null;
            Map<String, String> endTimeRange = request.getFilterValue("end_time", Map.class)
                    .orElse(Map.of());
            final Long endTimeStart = endTimeRange.get("$gt") != null ? Long.valueOf(endTimeRange.get("$gt")) : null;
            final Long endTimeEnd = endTimeRange.get("$lt") != null ? Long.valueOf(endTimeRange.get("$lt")) : null;
            Map<String, Object> excludeFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            ciCdAggsService.listCiCdJobRuns(
                                    company,
                                    CiCdJobRunsFilter.builder()
                                            .across(MoreObjects.firstNonNull(
                                                    CiCdJobRunsFilter.DISTINCT.fromString(
                                                            request.getAcross()),
                                                    CiCdJobRunsFilter.DISTINCT.trend))
                                            .calculation(CiCdJobRunsFilter.CALCULATION.count)
                                            .stacks(parseCiCdStacks(getListOrDefault(request.getFilter(), "stacks"), CiCdJobRunsFilter.DISTINCT.class))
                                            .cicdUserIds(getListOrDefault(request.getFilter(), "cicd_user_ids"))
                                            .services(getListOrDefault(request.getFilter(), "services"))
                                            .environments(getListOrDefault(request.getFilter(), "environments"))
                                            .infrastructures(getListOrDefault(request.getFilter(), "infrastructures"))
                                            .repositories(getListOrDefault(request.getFilter(), "repositories"))
                                            .branches(getListOrDefault(request.getFilter(), "branches"))
                                            .deploymentTypes(getListOrDefault(request.getFilter(), "deployment_types"))
                                            .rollback((Boolean) MapUtils.emptyIfNull(request.getFilter()).getOrDefault("rollback", null))
                                            .tags(getListOrDefault(request.getFilter(), "tags"))
                                            .jobNames(getListOrDefault(request.getFilter(), "job_names"))
                                            .jobNormalizedFullNames(getListOrDefault(request.getFilter(), "job_normalized_full_names"))
                                            .jobStatuses(getListOrDefault(request.getFilter(), "job_statuses"))
                                            .instanceNames(getListOrDefault(request.getFilter(), "instance_names"))
                                            .startTimeRange(ImmutablePair.of(jobStartedRangeStart, jobStartedRangeEnd))
                                            .endTimeRange(ImmutablePair.of(endTimeStart, endTimeEnd))
                                            .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                                            .types(CICD_TYPE.parseFromFilter(request))
                                            .projects(getListOrDefault(request.getFilter(), "projects"))
                                            .stageNames(getListOrDefault(request.getFilter(), "stage_name"))
                                            .stepNames(getListOrDefault(request.getFilter(), "step_name"))
                                            .stageStatuses(getListOrDefault(request.getFilter(), "stage_status"))
                                            .stepStatuses(getListOrDefault(request.getFilter(), "step_status"))
                                            .triageRuleNames(getListOrDefault(request.getFilter(), "triage_rule"))
                                            .excludeServices(getListOrDefault(excludeFields, "services"))
                                            .excludeEnvironments(getListOrDefault(excludeFields, "environments"))
                                            .excludeInfrastructures(getListOrDefault(excludeFields, "infrastructures"))
                                            .excludeRepositories(getListOrDefault(excludeFields, "repositories"))
                                            .excludeBranches(getListOrDefault(excludeFields, "branches"))
                                            .excludeDeploymentTypes(getListOrDefault(excludeFields, "deployment_types"))
                                            .excludeRollback((Boolean) MapUtils.emptyIfNull(excludeFields).getOrDefault("rollback", null))
                                            .excludeTags(getListOrDefault(excludeFields, "tags"))
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
                                            .excludeCiCdUserIds(getListOrDefault(excludeFields, "cicd_user_ids"))
                                            .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                                            .orgProductsIds(getListOrDefault(request.getFilter(), "org_product_ids").stream()
                                                    .map(UUID::fromString).collect(Collectors.toSet()))
                                            .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(request.getFilter(), "parameters")))
                                            .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(request.getFilter(), "qualified_job_names")))
                                            .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of())))
                                            .partialMatch(partialMatchMap)
                                            .build(),
                                    request.getPage(),
                                    request.getPageSize(),
                                    ouConfig)));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/job_runs/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getJobRunsChangesValues(
        @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        log.debug("filter = {}", originalRequest);
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JENKINS, IntegrationType.CIRCLECI, IntegrationType.DRONECI, IntegrationType.HARNESSNG,
                        IntegrationType.AZURE_PIPELINES, IntegrationType.AZURE_DEVOPS, IntegrationType.GITLAB), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/cicd/job_runs/values' for the request: {}", company, originalRequest, e);
            }
            if (CollectionUtils.isEmpty(request.getFields()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            Map<String, String> endTimeRange = request.getFilterValue("end_time", Map.class)
                    .orElse(Map.of());
            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            final Long endTimeStart = endTimeRange.get("$gt") != null ? Long.valueOf(endTimeRange.get("$gt")) : null;
            final Long endTimeEnd = endTimeRange.get("$lt") != null ? Long.valueOf(endTimeRange.get("$lt")) : null;
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            Map<String, Object> excludeFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            for (String value : request.getFields()) {
                List<DbAggregationResult> dbAggregationResults = ciCdAggsService.groupByAndCalculateCiCdJobRuns(
                        company,
                        CiCdJobRunsFilter.builder()
                                .across(
                                        CiCdJobRunsFilter.DISTINCT.fromString(
                                                value))
                                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                                .cicdUserIds(getListOrDefault(request.getFilter(), "cicd_user_ids"))
                                .isCiJob((Boolean) request.getFilter().getOrDefault("is_ci_job", null))
                                .isCdJob((Boolean) request.getFilter().getOrDefault("is_cd_job", null))
                                .services(getListOrDefault(request.getFilter(), "services"))
                                .environments(getListOrDefault(request.getFilter(), "environments"))
                                .infrastructures(getListOrDefault(request.getFilter(), "infrastructures"))
                                .repositories(getListOrDefault(request.getFilter(), "repositories"))
                                .branches(getListOrDefault(request.getFilter(), "branches"))
                                .deploymentTypes(getListOrDefault(request.getFilter(), "deployment_types"))
                                .rollback((Boolean) MapUtils.emptyIfNull(request.getFilter()).getOrDefault("rollback", null))
                                .tags(getListOrDefault(request.getFilter(), "tags"))
                                .jobNames(getListOrDefault(request.getFilter(), "job_names"))
                                .jobNormalizedFullNames(getListOrDefault(request.getFilter(), "job_normalized_full_names"))
                                .jobStatuses(getListOrDefault(request.getFilter(), "job_statuses"))
                                .instanceNames(getListOrDefault(request.getFilter(), "instance_names"))
                                .endTimeRange(ImmutablePair.of(endTimeStart, endTimeEnd))
                                .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                                .types(CICD_TYPE.parseFromFilter(request))
                                .projects(getListOrDefault(request.getFilter(), "projects"))
                                .stageNames(getListOrDefault(request.getFilter(), "stage_name"))
                                .stepNames(getListOrDefault(request.getFilter(), "step_name"))
                                .stageStatuses(getListOrDefault(request.getFilter(), "stage_status"))
                                .stepStatuses(getListOrDefault(request.getFilter(), "step_status"))
                                .triageRuleNames(getListOrDefault(request.getFilter(), "triage_rule"))
                                .excludeServices(getListOrDefault(excludeFields, "services"))
                                .excludeEnvironments(getListOrDefault(excludeFields, "environments"))
                                .excludeInfrastructures(getListOrDefault(excludeFields, "infrastructures"))
                                .excludeRepositories(getListOrDefault(excludeFields, "repositories"))
                                .excludeBranches(getListOrDefault(excludeFields, "branches"))
                                .excludeDeploymentTypes(getListOrDefault(excludeFields, "deployment_types"))
                                .excludeRollback((Boolean) MapUtils.emptyIfNull(excludeFields).getOrDefault("rollback", null))
                                .excludeTags(getListOrDefault(excludeFields, "tags"))
                                .excludeStageNames(getListOrDefault(excludeFields, "stage_name"))
                                .excludeStepNames(getListOrDefault(excludeFields, "step_name"))
                                .excludeStageStatuses(getListOrDefault(excludeFields, "stage_status"))
                                .excludeStepStatuses(getListOrDefault(excludeFields, "step_status"))
                                .excludeTriageRuleNames(getListOrDefault(excludeFields, "triage_rule"))
                                .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                                .excludeJobNormalizedFullNames(getListOrDefault(excludeFields, "job_normalized_full_names"))
                                .excludeJobStatuses(getListOrDefault(excludeFields, "job_statuses"))
                                .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                                .excludeProjects(getListOrDefault(excludeFields, "projects"))
                                .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                                .excludeCiCdUserIds(getListOrDefault(excludeFields, "cicd_user_ids"))
                                .partialMatch(partialMatchMap)
                                .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                                .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(request.getFilter(), "parameters")))
                                .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(request.getFilter(), "qualified_job_names")))
                                .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(request.getAggInterval()),
                                        CICD_AGG_INTERVAL.day))
                                .orgProductsIds(getListOrDefault(request.getFilter(), "org_product_ids").stream()
                                        .map(UUID::fromString).collect(Collectors.toSet()))
                                .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(List.of(Map.of("id", value, "desc", false)), List.of())))
                                .build(), VALUES_ONLY, ouConfig)
                        .getRecords();
                log.debug("value = {}, dbAggregationResults = {}", value, dbAggregationResults);
                response.add(Map.of(value, dbAggregationResults));
            }
            log.debug("response = {}", response);
            return ResponseEntity.ok().body(PaginatedResponse.of(0, response.size(), response));
        });
    }


}
