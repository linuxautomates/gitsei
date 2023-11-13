package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import com.google.common.base.MoreObjects;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.CICDJobRunDTO;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.JobRunStageStep;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdPipelineJobRunsFilter;
import io.levelops.commons.databases.models.filters.DefaultListRequestUtils;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageStepsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.databases.services.CiCdPipelinesAggsService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static io.levelops.api.converters.DefaultListRequestUtils.getListOfObjectOrDefault;
import static io.levelops.api.converters.DefaultListRequestUtils.parseCiCdJobRunParameters;
import static io.levelops.api.converters.DefaultListRequestUtils.parseCiCdQualifiedJobNames;
import static io.levelops.api.converters.DefaultListRequestUtils.parseCiCdStacks;
import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;


@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN', 'AUDITOR', 'PUBLIC_DASHBOARD','ORG_ADMIN_USER')")
@RequestMapping("/v1/cicd/pipelines")
public class CiCdPipelinesAggregationsController {
    private static final Boolean STAGES_ON_ALL_LEVELS = false;
    private static final boolean NOT_FULL_DETAILS = false;
    private static final boolean NOT_SINGLE_JOB = false;
    private static final boolean SKIP_STAGES = true;
    private static final boolean VALUES_ONLY = true;
    private static final boolean NOT_VALUES_ONLY = false;
    public static final boolean FULL_DETAILS = true;

    private final Storage storage;
    private final String bucketName;
    private final ObjectMapper objectMapper;
    private final CiCdJobRunsDatabaseService jobRunService;
    private final CiCdJobRunStageDatabaseService stagesService;
    private final CiCdPipelinesAggsService ciCdPipelinesAggsService;
    private final CiCdJobRunStageStepsDatabaseService stageStepService;
    private final OrgUnitHelper ouHelper;
    private final Executor dbValuesTaskExecutor;

    @Autowired
    public CiCdPipelinesAggregationsController(@Value("${INGESTION_BUCKET:ingestion-levelops}") final String bucketName,
                                               final Storage storage, final ObjectMapper objectMapper,
                                               final CiCdPipelinesAggsService ciCdPipelinesAggsService,
                                               final CiCdJobRunStageDatabaseService stagesService,
                                               final CiCdJobRunStageStepsDatabaseService stageStepService,
                                               final CiCdJobRunsDatabaseService jobRunService,
                                               final OrgUnitHelper ouHelper,
                                               @Qualifier("dbValuesTaskExecutor") final Executor dbValuesTaskExecutor) {
        this.objectMapper = objectMapper;
        this.ciCdPipelinesAggsService = ciCdPipelinesAggsService;
        this.stagesService = stagesService;
        this.bucketName = bucketName;
        this.storage = storage;
        this.stageStepService = stageStepService;
        this.jobRunService = jobRunService;
        this.ouHelper = ouHelper;
        this.dbValuesTaskExecutor = dbValuesTaskExecutor;
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/job_counts", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getJobRunsCountAggs(
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
                log.error("[{}] Unable to process the OU config in '/cicd/pipelines/job_counts' for the request: {}", company, originalRequest, e);
            }
            Map<String, String> endTimeRange = request.getFilterValue("end_time", Map.class)
                    .orElse(Map.of());
            final Long endTimeStart = endTimeRange.get("$gt") != null ? Long.valueOf(endTimeRange.get("$gt")) : null;
            final Long endTimeEnd = endTimeRange.get("$lt") != null ? Long.valueOf(endTimeRange.get("$lt")) : null;

            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            Map<String, Object> excludeFields =
                    (Map<String, Object>) request.getFilter().getOrDefault("exclude", Map.of());
            return ResponseEntity.ok().body(
                PaginatedResponse.of(
                        request.getPage(),
                        request.getPageSize(),
                        ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(
                                company,
                                CiCdPipelineJobRunsFilter.builder()
                                        .across(MoreObjects.firstNonNull(
                                                CiCdPipelineJobRunsFilter.DISTINCT.fromString(
                                                        request.getAcross()),
                                                CiCdPipelineJobRunsFilter.DISTINCT.trend))
                                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                                        .stacks(parseCiCdStacks(request.getStacks(), CiCdPipelineJobRunsFilter.DISTINCT.class))
                                        .cicdUserIds(getListOrDefault(request.getFilter(), "cicd_user_ids"))
                                        .services(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "services"))
                                        .environments(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "environments"))
                                        .infrastructures(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "infrastructures"))
                                        .repositories(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "repositories"))
                                        .branches(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "branches"))
                                        .deploymentTypes(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "deployment_types"))
                                        .rollback((Boolean) MapUtils.emptyIfNull(request.getFilter()).getOrDefault("rollback", null))
                                        .tags(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "tags"))
                                        .jobNames(getListOrDefault(request.getFilter(), "job_names"))
                                        .jobNormalizedFullNames(getListOrDefault(request.getFilter(), "job_normalized_full_names"))
                                        .jobStatuses(getListOrDefault(request.getFilter(), "job_statuses"))
                                        .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                                        .types(CICD_TYPE.parseFromFilter(request))
                                        .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(request.getFilter(), "parameters")))
                                        .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(request.getFilter(), "qualified_job_names")))
                                        .parentCiCdJobIds(getListOrDefault(request.getFilter(), "parent_cicd_job_ids"))
                                        .instanceNames(getListOrDefault(request.getFilter(), "instance_names"))
                                        .endTimeRange(ImmutablePair.of(endTimeStart, endTimeEnd))
                                        .partialMatch(partialMatchMap)
                                        .orgProductsIds(getListOrDefault(request.getFilter(), "org_product_ids").stream()
                                                .map(UUID::fromString).collect(Collectors.toSet()))
                                        .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(request.getAggInterval()),
                                                CICD_AGG_INTERVAL.day))
                                        .projects(getListOrDefault(request.getFilter(), "projects"))
                                        .ciCdJobRunIds(getListOrDefault(request.getFilter(), "cicd_job_run_ids"))
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
                                        .excludeJobIds(getListOrDefault(excludeFields,"cicd_job_ids"))
                                        .excludeCiCdUserIds(getListOrDefault(excludeFields,"cicd_user_ids"))
                                        .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                                        .excludeCiCdJobRunIds(getListOrDefault(excludeFields, "cicd_job_run_ids"))
                                        .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                                        .jobIds(getListOrDefault(request.getFilter(), "cicd_job_ids"))
                                        .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of())))
                                        .build(), NOT_VALUES_ONLY, ouConfig)
                                .getRecords()));
        });
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/job_durations", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getJobDurationAggs(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.getCICDIntegrationTypes(), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/cicd/pipelines/job_durations' for the request: {}", company, originalRequest, e);
            }
            Map<String, String> endTimeRange = request.getFilterValue("end_time", Map.class)
                    .orElse(Map.of());
            final Long endTimeStart = endTimeRange.get("$gt") != null ? Long.valueOf(endTimeRange.get("$gt")) : null;
            final Long endTimeEnd = endTimeRange.get("$lt") != null ? Long.valueOf(endTimeRange.get("$lt")) : null;

            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            Map<String, Object> excludeFields =
                    (Map<String, Object>) request.getFilter().getOrDefault("exclude", Map.of());
            return ResponseEntity.ok().body(
                PaginatedResponse.of(
                        request.getPage(),
                        request.getPageSize(),
                        ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(
                                company,
                                CiCdPipelineJobRunsFilter.builder()
                                        .across(MoreObjects.firstNonNull(
                                                CiCdPipelineJobRunsFilter.DISTINCT.fromString(
                                                        request.getAcross()),
                                                CiCdPipelineJobRunsFilter.DISTINCT.trend))
                                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.duration)
                                        .stacks(parseCiCdStacks(request.getStacks(), CiCdPipelineJobRunsFilter.DISTINCT.class))
                                        .cicdUserIds(getListOrDefault(request.getFilter(), "cicd_user_ids"))
                                        .services(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "services"))
                                        .environments(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "environments"))
                                        .infrastructures(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "infrastructures"))
                                        .repositories(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "repositories"))
                                        .branches(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "branches"))
                                        .deploymentTypes(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "deployment_types"))
                                        .rollback((Boolean) MapUtils.emptyIfNull(request.getFilter()).getOrDefault("rollback", null))
                                        .tags(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "tags"))
                                        .jobNames(getListOrDefault(request.getFilter(), "job_names"))
                                        .jobNormalizedFullNames(getListOrDefault(request.getFilter(), "job_normalized_full_names"))
                                        .jobStatuses(getListOrDefault(request.getFilter(), "job_statuses"))
                                        .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                                        .types(CICD_TYPE.parseFromFilter(request))
                                        .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(request.getFilter(), "parameters")))
                                        .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(request.getFilter(), "qualified_job_names")))
                                        .parentCiCdJobIds(getListOrDefault(request.getFilter(), "parent_cicd_job_ids"))
                                        .instanceNames(getListOrDefault(request.getFilter(), "instance_names"))
                                        .endTimeRange(ImmutablePair.of(endTimeStart, endTimeEnd))
                                        .partialMatch(partialMatchMap)
                                        .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(request.getAggInterval()),
                                                CICD_AGG_INTERVAL.day))
                                        .projects(getListOrDefault(request.getFilter(), "projects"))
                                        .ciCdJobRunIds(getListOrDefault(request.getFilter(), "cicd_job_run_ids"))
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
                                        .excludeJobIds(getListOrDefault(excludeFields,"cicd_job_ids"))
                                        .excludeCiCdUserIds(getListOrDefault(excludeFields,"cicd_user_ids"))
                                        .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                                        .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                                        .excludeCiCdJobRunIds(getListOrDefault(excludeFields, "cicd_job_run_ids"))
                                        .orgProductsIds(getListOrDefault(request.getFilter(), "org_product_ids").stream()
                                                .map(UUID::fromString).collect(Collectors.toSet()))
                                        .jobIds(getListOrDefault(request.getFilter(), "cicd_job_ids"))
                                        .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of())))
                                        .build(), NOT_VALUES_ONLY, ouConfig)
                                .getRecords()));
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
                log.error("[{}] Unable to process the OU config in '/cicd/pipelines/job_runs/list' for the request: {}", company, originalRequest, e);
            }
            String partialJobName = (String) ((Map<String, Object>) request.getFilter()
                    .getOrDefault("partial", Collections.emptyMap()))
                    .getOrDefault("job_name", null);
            log.debug("partialJobName = {}", partialJobName);
            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            var jobStartedAtRange = request.<String, String>getFilterValueAsMap("start_time").orElse(Map.of());
            Long jobStartedRangeStart = jobStartedAtRange.get("$gt") != null ? Long.valueOf(jobStartedAtRange.get("$gt")) : null;
            Long jobStartedRangeEnd = jobStartedAtRange.get("$lt") != null ? Long.valueOf(jobStartedAtRange.get("$lt")) : null;
            Map<String, String> endTimeRange = request.getFilterValue("end_time", Map.class)
                    .orElse(Map.of());
            Map<String, Object> excludeFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            final Long endTimeStart = endTimeRange.get("$gt") != null ? Long.valueOf(endTimeRange.get("$gt")) : null;
            final Long endTimeEnd = endTimeRange.get("$lt") != null ? Long.valueOf(endTimeRange.get("$lt")) : null;
            return ResponseEntity.ok().body(
                PaginatedResponse.of(
                        request.getPage(),
                        request.getPageSize(),
                        ciCdPipelinesAggsService.listCiCdJobRuns(
                                company,
                                CiCdPipelineJobRunsFilter.builder()
                                        .across(MoreObjects.firstNonNull(
                                                CiCdPipelineJobRunsFilter.DISTINCT.fromString(
                                                        request.getAcross()),
                                                CiCdPipelineJobRunsFilter.DISTINCT.trend))
                                        .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                                        .cicdUserIds(getListOrDefault(request.getFilter(), "cicd_user_ids"))
                                        .services(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "services"))
                                        .environments(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "environments"))
                                        .infrastructures(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "infrastructures"))
                                        .repositories(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "repositories"))
                                        .branches(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "branches"))
                                        .deploymentTypes(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "deployment_types"))
                                        .rollback((Boolean) MapUtils.emptyIfNull(request.getFilter()).getOrDefault("rollback", null))
                                        .tags(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "tags"))
                                        .jobNames(getListOrDefault(request.getFilter(), "job_names"))
                                        .jobNormalizedFullNames(getListOrDefault(request.getFilter(), "job_normalized_full_names"))
                                        .jobStatuses(getListOrDefault(request.getFilter(), "job_statuses"))
                                        .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                                        .types(CICD_TYPE.parseFromFilter(request))
                                        .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(request.getFilter(), "parameters")))
                                        .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(request.getFilter(), "qualified_job_names")))
                                        .parentCiCdJobIds(getListOrDefault(request.getFilter(), "parent_cicd_job_ids"))
                                        .instanceNames(getListOrDefault(request.getFilter(), "instance_names"))
                                        .jobNamePartial(partialJobName)
                                        .partialMatch(partialMatchMap)
                                        .startTimeRange(ImmutablePair.of(jobStartedRangeStart, jobStartedRangeEnd))
                                        .endTimeRange(ImmutablePair.of(endTimeStart, endTimeEnd))
                                        .projects(getListOrDefault(request.getFilter(), "projects"))
                                        .ciCdJobRunIds(getListOrDefault(request.getFilter(), "cicd_job_run_ids"))
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
                                        .excludeJobIds(getListOrDefault(excludeFields,"cicd_job_ids"))
                                        .excludeCiCdUserIds(getListOrDefault(excludeFields,"cicd_user_ids"))
                                        .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                                        .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                                        .excludeCiCdJobRunIds(getListOrDefault(excludeFields, "cicd_job_run_ids"))
                                        .orgProductsIds(getListOrDefault(request.getFilter(), "org_product_ids").stream()
                                                .map(UUID::fromString).collect(Collectors.toSet()))
                                        .jobIds(getListOrDefault(request.getFilter(), "cicd_job_ids"))
                                        .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of())))
                                        .build(),
                                request.getPage(),
                                request.getPageSize(),
                                FULL_DETAILS,
                                NOT_SINGLE_JOB,
                                ouConfig)));
        });
    }


    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/triage_job_runs/list", produces = "application/json")
    @SuppressWarnings("unchecked")
    public DeferredResult<ResponseEntity<PaginatedResponse<CICDJobRunDTO>>> getTriageJobRunsList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        log.debug("filter = {}", originalRequest);
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.AZURE_PIPELINES, IntegrationType.GITLAB), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/cicd/pipelines/triage_job_runs/list' for the request: {}", company, originalRequest, e);
            }
            Map<String, String> endTimeRange = request.getFilterValue("end_time", Map.class)
                    .orElse(Map.of());
            final Long endTimeStart = endTimeRange.get("$gt") != null ? Long.valueOf(endTimeRange.get("$gt")) : null;
            final Long endTimeEnd = endTimeRange.get("$lt") != null ? Long.valueOf(endTimeRange.get("$lt")) : null;
            String partialJobName = (String) ((Map<String, Object>) request.getFilter()
                    .getOrDefault("partial", Collections.emptyMap()))
                    .getOrDefault("job_name", null);
            log.debug("partialJobName = {}", partialJobName);
            Map<String, Object> excludeFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            var cicdFilters = CiCdPipelineJobRunsFilter.builder()
                    .across(MoreObjects.firstNonNull(
                            CiCdPipelineJobRunsFilter.DISTINCT.fromString(
                                    request.getAcross()),
                            CiCdPipelineJobRunsFilter.DISTINCT.trend))
                    .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                    .cicdUserIds(getListOrDefault(request.getFilter(), "cicd_user_ids"))
                    .ciCdJobRunIds(getListOrDefault(request.getFilter(), "cicd_job_run_ids"))
                    .jobIds(getListOrDefault(request.getFilter(), "cicd_job_ids"))
                    .jobNames(getListOrDefault(request.getFilter(), "job_names"))
                    .jobNormalizedFullNames(getListOrDefault(request.getFilter(), "job_normalized_full_names"))
                    .jobStatuses(getListOrDefault(request.getFilter(), "job_statuses"))
                    .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                    .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(request.getFilter(), "parameters")))
                    .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(request.getFilter(), "qualified_job_names")))
                    .parentCiCdJobIds(getListOrDefault(request.getFilter(), "parent_cicd_job_ids"))
                    .instanceNames(getListOrDefault(request.getFilter(), "instance_names"))
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                    .excludeJobNormalizedFullNames(getListOrDefault(excludeFields, "job_normalized_full_names"))
                    .excludeJobStatuses(getListOrDefault(excludeFields, "job_statuses"))
                    .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                    .excludeJobIds(getListOrDefault(excludeFields,"cicd_job_ids"))
                    .excludeCiCdUserIds(getListOrDefault(excludeFields,"cicd_user_ids"))
                    .excludeCiCdJobRunIds(getListOrDefault(excludeFields, "cicd_job_run_ids"))
                    .excludeProjects(getListOrDefault(excludeFields, "projects"))
                    .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                    .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                    .endTimeRange(ImmutablePair.of(endTimeStart, endTimeEnd))
                    .orgProductsIds(getListOrDefault(request.getFilter(), "org_product_ids").stream()
                            .map(UUID::fromString).collect(Collectors.toSet()))
                    .types(List.of(CICD_TYPE.jenkins))
                    .jobNamePartial(partialJobName)
                    .partialMatch(partialMatchMap)
                    .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of())));

            var jobStartedAtRange = request.<String, String>getFilterValueAsMap("start_time").orElse(Map.of());
            if (MapUtils.isNotEmpty(jobStartedAtRange)) {
                Long jobStartedRangeStart = jobStartedAtRange.get("$gt") != null ? Long.valueOf(jobStartedAtRange.get("$gt")) : null;
                Long jobStartedRangeEnd = jobStartedAtRange.get("$lt") != null ? Long.valueOf(jobStartedAtRange.get("$lt")) : null;
                cicdFilters.startTimeRange(ImmutablePair.of(jobStartedRangeStart, jobStartedRangeEnd));
            }
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            ciCdPipelinesAggsService.listCiCdJobRuns(
                                    company,
                                    cicdFilters.build(),
                                    request.getPage(),
                                    request.getPageSize(),
                                    NOT_FULL_DETAILS,
                                    NOT_SINGLE_JOB,
                                    ouConfig
                            )));
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
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.AZURE_PIPELINES, IntegrationType.GITLAB), originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/cicd/pipelines/job_runs/values' for the request: {}", company, originalRequest, e);
            }
            Map<String, String> endTimeRange = request.getFilterValue("end_time", Map.class)
                    .orElse(Map.of());
            final Long endTimeStart = endTimeRange.get("$gt") != null ? Long.valueOf(endTimeRange.get("$gt")) : null;
            final Long endTimeEnd = endTimeRange.get("$lt") != null ? Long.valueOf(endTimeRange.get("$lt")) : null;
            Map<String, Object> excludeFields = (Map<String, Object>) request.getFilter()
                    .getOrDefault("exclude", Map.of());
            String partialJobName = (String) ((Map<String, Object>) request.getFilter()
                    .getOrDefault("partial", Collections.emptyMap()))
                    .getOrDefault("job_name", null);
            log.debug("partialJobName = {}", partialJobName);
            Map<String, Map<String, String>> partialMatchMap =
                    MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
            if (CollectionUtils.isEmpty(request.getFields())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            }
            CiCdPipelineJobRunsFilter ciCdPipelineJobRunsFilter = CiCdPipelineJobRunsFilter.builder()
                    .calculation(CiCdPipelineJobRunsFilter.CALCULATION.count)
                    .cicdUserIds(getListOrDefault(request.getFilter(), "cicd_user_ids"))
                    .jobIds(getListOrDefault(request.getFilter(), "cicd_job_ids"))
                    .jobNames(getListOrDefault(request.getFilter(), "job_names"))
                    .jobNormalizedFullNames(getListOrDefault(request.getFilter(), "job_normalized_full_names"))
                    .jobStatuses(getListOrDefault(request.getFilter(), "job_statuses"))
                    .integrationIds(getListOrDefault(request.getFilter(), "integration_ids"))
                    .types(CICD_TYPE.parseFromFilter(request))
                    .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(request.getFilter(), "parameters")))
                    .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(request.getFilter(), "qualified_job_names")))
                    .parentCiCdJobIds(getListOrDefault(request.getFilter(), "parent_cicd_job_ids"))
                    .instanceNames(getListOrDefault(request.getFilter(), "instance_names"))
                    .jobNamePartial(partialJobName)
                    .endTimeRange(ImmutablePair.of(endTimeStart, endTimeEnd))
                    .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(request.getAggInterval()),
                            CICD_AGG_INTERVAL.day))
                    .partialMatch(partialMatchMap)
                    .projects(getListOrDefault(request.getFilter(), "projects"))
                    .ciCdJobRunIds(getListOrDefault(request.getFilter(), "cicd_job_run_ids"))
                    .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                    .excludeJobNormalizedFullNames(getListOrDefault(excludeFields, "job_normalized_full_names"))
                    .excludeJobStatuses(getListOrDefault(excludeFields, "job_statuses"))
                    .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                    .excludeProjects(getListOrDefault(excludeFields, "projects"))
                    .excludeJobIds(getListOrDefault(excludeFields,"cicd_job_ids"))
                    .excludeCiCdUserIds(getListOrDefault(excludeFields,"cicd_user_ids"))
                    .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                    .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                    .excludeCiCdJobRunIds(getListOrDefault(excludeFields, "cicd_job_run_ids"))
                    .orgProductsIds(getListOrDefault(request.getFilter(), "org_product_ids").stream()
                            .map(UUID::fromString).collect(Collectors.toSet()))
                    .build();

            List<CompletableFuture<Map<String, List<DbAggregationResult>>>> futures = new ArrayList<>();
            for (String value : request.getFields()) {
                futures.add(calculateJobRunsValuesAsync(company, ciCdPipelineJobRunsFilter, value, ouConfig));
            }

            List<Map<String, List<DbAggregationResult>>> response = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
            log.debug("response = {}", response);
            return ResponseEntity.ok().body(PaginatedResponse.of(0, response.size(), response));
        });
    }

    public CompletableFuture<Map<String, List<DbAggregationResult>>> calculateJobRunsValuesAsync(String company, CiCdPipelineJobRunsFilter ciCdPipelineJobRunsFilter, String value, final OUConfiguration ouConfig) {
        return CompletableFuture.supplyAsync(() -> {
            List<DbAggregationResult> dbAggregationResults = ciCdPipelinesAggsService.groupByAndCalculateCiCdJobRuns(company,
                            ciCdPipelineJobRunsFilter.toBuilder()
                                    .across(CiCdPipelineJobRunsFilter.DISTINCT.fromString(value))
                                    .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(List.of(Map.of("id", value, "desc", false)), List.of())))
                                    .build(),
                            VALUES_ONLY, ouConfig)
                    .getRecords();
            log.debug("value = {}, dbAggregationResults = {}", value, dbAggregationResults);
            return Map.of(value, dbAggregationResults);
        }, dbValuesTaskExecutor);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @GetMapping(value = "/job_runs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<CICDJobRunDTO>> getJobRun(
            @SessionAttribute(name = "company") String company,
            @PathVariable("id") UUID uuid) {
        return SpringUtils.deferResponse(() -> {
            CICDJobRunDTO jobRun;
            final String id = uuid.toString();
            try {
                jobRun = ciCdPipelinesAggsService.get(company, id)
                        .orElseThrow(() ->
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find the requested job run: " + id));
                // TODO: only check if the check logs flag is on
                jobRun = jobRun.toBuilder()
                        .logs(checkLogsForJobRun(company, id, jobRun))
                        .build();

            } catch (SQLException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to complete the reques, please try again in a few minutes or contact support. requested id=" + id);
            }
            var url = stagesService.getFullUrl(company, jobRun.getId(), "", "");
            log.debug("job url={}", url);
            return ResponseEntity.ok(jobRun.toBuilder().url(url).build());
        });
    }

    /**
     *
     */
    private boolean checkLogsForJobRun(final String company, final String id, final CICDJobRunDTO jobRun) {
        // check if the state of the run is not success, we only sow logs for not successful job runs
        if (!"SUCCESS".equalsIgnoreCase(jobRun.getStatus())) {
            // check if there are logs directly or at the stage level
            if (Strings.isNotBlank(jobRun.getLogGcspath())) {
                // we already have logs at the job run level so we stop here.
                return true;
            } else {
                // we check if there are logs at the steps level
                // we skip stages as those are handled separately
                // and we should not show stages logs here.
                var logs = jobRunService.getLogs(company, id, SKIP_STAGES);
                return CollectionUtils.isNotEmpty(logs);

            }
        }
        return false;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @GetMapping(value = "/job_runs/{id}/log", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<byte[]>> getJobRunLog(
            @SessionAttribute(name = "company") String company,
            @PathVariable("id") UUID uuid) {
        return SpringUtils.deferResponse(() -> {
            CICDJobRunDTO jobRun;
            final String id = uuid.toString();
            try {
                jobRun = ciCdPipelinesAggsService.get(company, id)
                        .orElseThrow(() ->
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find the requested job run: " + id));
                // check if we need to look for logs at the steps level
                if (!"SUCCESS".equalsIgnoreCase(jobRun.getStatus()) && Strings.isBlank(jobRun.getLogGcspath())) {
                    var logs = jobRunService.getLogs(company, id, SKIP_STAGES);
                    if (CollectionUtils.isNotEmpty(logs)) {
                        return ResponseEntity.ok(downloadFile(logs));
                    }
                }
            } catch (SQLException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to complete the request, please try again in a few minutes or contact support. requested id=" + id);
            }
            if (StringUtils.isEmpty(jobRun.getLogGcspath()))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This job doesnt have a log file.");
            return ResponseEntity.ok(downloadFile(List.of(jobRun.getLogGcspath())));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @GetMapping(value = "/job_runs/stages/{id}/log", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<byte[]>> getStageLog(
            @SessionAttribute(name = "company") String company,
            @PathVariable("id") UUID uuid) {
        return SpringUtils.deferResponse(() -> {
            List<String> stepLogs;
            JobRunStage jobRunStage;
            final String id = uuid.toString();
            try {
                jobRunStage = stagesService.get(company, id)
                        .orElseThrow(() ->
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find the requested stage: " + id));
            } catch (SQLException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to complete the request, please try again in a few minutes or contact support. requested id=" + id);
            }

            if (StringUtils.isNotEmpty(jobRunStage.getLogs())) {
                stepLogs = List.of(jobRunStage.getLogs());
                return ResponseEntity.ok(downloadFile(stepLogs));
            }

            List<JobRunStageStep> jobRunSteps;
            try {
                jobRunSteps = stageStepService.listByFilter(company, 0, 10000, null,
                        List.of(UUID.fromString(id)), null).getRecords();
            } catch (SQLException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to complete the request, please try again in a few minutes or contact support. requested id=" + id);
            }
            stepLogs = jobRunSteps.stream().filter(j -> StringUtils.isNotEmpty(j.getGcsPath()))
                    .sorted(Comparator.comparing(JobRunStageStep::getStepId))
                    .map(JobRunStageStep::getGcsPath)
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(stepLogs))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stage:" + id + " doesnt have logs.");

            return ResponseEntity.ok(downloadFile(stepLogs));
        });
    }

    private byte[] downloadFile(final Collection<String> paths) throws IOException {
        try (ByteArrayOutputStream byos = new ByteArrayOutputStream()) {
            for (String path : paths) {
                var blob = storage.get(bucketName, path, Storage.BlobGetOption.fields(Storage.BlobField.METADATA));
                try (ByteArrayOutputStream internalByos = new ByteArrayOutputStream()) {
                    blob.downloadTo(internalByos);
                    byos.write(internalByos.toByteArray());
                }
            }
            return byos.toByteArray();
        }
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(path = {"/job_runs/stages", "/job_runs/{id}/stages"}, method = {RequestMethod.POST}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("unchecked")
    public DeferredResult<ResponseEntity<PaginatedResponse<JobRunStage>>> getStages(
            @SessionAttribute(name = "company") String company,
            @PathVariable(name = "id", required = false) UUID id,
            @RequestBody DefaultListRequest search) {
        return SpringUtils.deferResponse(() -> {
            var filters = QueryFilter.fromRequestFilters(search.getFilter());
            var strictMatches = new HashMap<>(filters.getStrictMatches());
            if (id != null) {
                if (Strings.isNotBlank((String) strictMatches.get("job_run_id"))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "the job_run_id is defined more than once, either pass it in as a path section or as a filter but not both");
                }
                strictMatches.put("cicd_job_run_id", id);
            } else if (Strings.isBlank((String) strictMatches.get("job_run_id")) && CollectionUtils.isEmpty((Collection<String>) strictMatches.get("job_ids"))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Neither the pipeline job_run_id nor the job_id were present but either one of them is required");
            } else if (Strings.isNotBlank((String) strictMatches.get("job_run_id"))) {
                strictMatches.put("cicd_job_run_id", UUID.fromString((String) strictMatches.get("job_run_id")));
            }
            if (CollectionUtils.isNotEmpty((Collection<String>) strictMatches.get("job_ids"))) {
                strictMatches.put("job_ids", strictMatches.get("job_ids"));
            }

            var startTime = search.<String, Object>getFilterValueAsMap("start_time").orElse(Map.of());
            if (MapUtils.isNotEmpty(startTime)) {
                strictMatches.put("start_time", startTime.get("$gt"));
                strictMatches.put("end_time", startTime.get("$lt"));
            } else {
                strictMatches.put("start_time", null);
            }
            // strictMatches.put("start_time", null);
            DbListResponse<JobRunStage> stages;
            try {
                stages = stagesService.list(
                        company,
                        search.getPage(),
                        search.getPageSize(),
                        filters.toBuilder().strictMatches(strictMatches).build(),
                        Pair.of(Set.of("start_time"), SortingOrder.DESC),
                        STAGES_ON_ALL_LEVELS
                );
            } catch (SQLException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to complete the reques, please try again in a few minutes or contact support. requested id=" + id);
            }
            return ResponseEntity.ok(PaginatedResponse.of(search.getPage(), search.getPageSize(), stages));
        });
    }
}
