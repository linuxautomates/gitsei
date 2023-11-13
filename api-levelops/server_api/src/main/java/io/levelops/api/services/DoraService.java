package io.levelops.api.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.hash.Hashing;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.models.database.CICDJobRunDTO;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.JiraReleaseResponse;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.dora.DoraDrillDownDTO;
import io.levelops.commons.databases.models.dora.DoraResponseDTO;
import io.levelops.commons.databases.models.dora.DoraSingleStateDTO;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.filters.DefaultListRequestUtils;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.CiCdAggsService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.service.dora.ADODoraService;
import io.levelops.commons.service.dora.CiCdDoraService;
import io.levelops.commons.service.dora.JiraDoraService;
import io.levelops.commons.service.dora.ScmDoraService;
import io.levelops.commons.services.velocity_productivity.services.VelocityConfigsService;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.commons.utils.dora.DoraCalculationUtils;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.api.converters.DefaultListRequestUtils.getListOfObjectOrDefault;
import static io.levelops.api.converters.DefaultListRequestUtils.parseCiCdJobRunParameters;
import static io.levelops.api.converters.DefaultListRequestUtils.parseCiCdQualifiedJobNames;
import static io.levelops.api.converters.DefaultListRequestUtils.parseCiCdStacks;
import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getTimeRange;

@Log4j2
@Service
public class DoraService {

    private final JiraFilterParser jiraFilterParser;

    private final JiraDoraService jiraDoraService;

    private final ADODoraService adoDoraService;

    private final OrgUnitHelper orgUnitHelper;

    private final ScmDoraService scmDoraService;

    private final VelocityConfigsService velocityConfigsService;
    private final CiCdDoraService ciCdDoraService;
    private final IntegrationService integrationService;
    private final AggCacheService aggCacheService;
    private final JiraIssueService jiraIssueService;

    public final String DEPLOYMENT_FREQUENCY = "deployment_frequency_report";
    public final String CHANGE_FAILURE_RATE = "change_failure_rate";

    // For CICD
    private final CiCdAggsService ciCdAggsService;
    private final ObjectMapper objectMapper;


    @Autowired
    public DoraService(JiraFilterParser jiraFilterParser,
                       JiraDoraService jiraDoraService,
                       ADODoraService adoDoraService,
                       OrgUnitHelper orgUnitHelper,
                       ScmDoraService scmDoraService,
                       AggCacheService aggCacheService,
                       VelocityConfigsService velocityConfigsService,
                       JiraIssueService jiraIssueService,
                       IntegrationService integrationService,
                       ObjectMapper objectMapper,
                       CiCdAggsService ciCdAggsService,
                       CiCdDoraService ciCdDoraService) {
        this.jiraFilterParser = jiraFilterParser;
        this.jiraDoraService = jiraDoraService;
        this.adoDoraService = adoDoraService;
        this.orgUnitHelper = orgUnitHelper;
        this.scmDoraService = scmDoraService;
        this.aggCacheService = aggCacheService;
        this.velocityConfigsService = velocityConfigsService;
        this.jiraIssueService = jiraIssueService;
        this.integrationService = integrationService;
        this.ciCdAggsService = ciCdAggsService;
        this.objectMapper = objectMapper;
        this.ciCdDoraService = ciCdDoraService;
    }

    public DoraResponseDTO generateDeploymentFrequencyForSCM(String company,
                                                             DefaultListRequest originalRequest,
                                                             Boolean disableCache,
                                                             VelocityConfigDTO velocityConfigDTO) throws Exception {
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/scm/dora/deployment_frequency' for the request: {}", company, originalRequest, e);
        }
        final var finalOuConfig = ouConfig;
        ScmPrFilter scmPrFilter = ScmPrFilter.fromDefaultListRequest(request, MoreObjects.firstNonNull(ScmPrFilter.DISTINCT.fromString(request.getAcross()), ScmPrFilter.DISTINCT.none), ScmPrFilter.CALCULATION.deployment_frequency);
        ScmCommitFilter scmCommitFilter = ScmCommitFilter.fromDefaultListRequest(request, null, null, Map.of());

        DoraResponseDTO doraresponse = AggCacheUtils.cacheOrCallGeneric(disableCache,
                company,
                "/dora/deployment_frequency",
                scmPrFilter.generateCacheRawString() + finalOuConfig.hashCode() + velocityConfigDTO.getId() + velocityConfigDTO.getUpdatedAt(),
                scmPrFilter.getIntegrationIds(), objectMapper, aggCacheService, DoraResponseDTO.class,
                null, null,
                () -> scmDoraService.calculateDeploymentFrequency(company, scmPrFilter, scmCommitFilter, finalOuConfig, velocityConfigDTO));

        return doraresponse;
    }

    public DoraResponseDTO generateChangeFailureRateForSCM(String company,
                                                           DefaultListRequest originalRequest,
                                                           Boolean disableCache,
                                                           VelocityConfigDTO velocityConfigDTO) throws Exception {
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/scm/dora/change_failure_rate' for the request: {}", company, originalRequest, e);
        }
        final var finalOuConfig = ouConfig;
        ScmPrFilter scmPrFilter = ScmPrFilter.fromDefaultListRequest(request, MoreObjects.firstNonNull(ScmPrFilter.DISTINCT.fromString(request.getAcross()), ScmPrFilter.DISTINCT.none), ScmPrFilter.CALCULATION.failure_rate);
        ScmCommitFilter scmCommitFilter = ScmCommitFilter.fromDefaultListRequest(request, null, null, Map.of());

        DoraResponseDTO doraresponse = AggCacheUtils.cacheOrCallGeneric(disableCache,
                company,
                "/dora/change_failure_rate",
                scmPrFilter.generateCacheRawString() + finalOuConfig.hashCode() + velocityConfigDTO.getId() + velocityConfigDTO.getUpdatedAt(),
                scmPrFilter.getIntegrationIds(), objectMapper, aggCacheService, DoraResponseDTO.class,
                null, null,
                () -> scmDoraService.calculateChangeFailureRate(company, scmPrFilter, scmCommitFilter, finalOuConfig, velocityConfigDTO));

        return doraresponse;
    }

    public DoraResponseDTO generateDFCountForIM(String company, DefaultListRequest originalRequest, VelocityConfigDTO velocityConfigDTO) throws InventoryException, SQLException, BadRequestException {
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getIssueManagementIntegrationTypes(), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/dora/deployment_frequency' for the request: {}", company, originalRequest, e);
        }
        final OUConfiguration finalOuConfig = ouConfig;

        DoraResponseDTO response = DoraResponseDTO.builder().build();

        VelocityConfigDTO.DeploymentFrequency deploymentFrequency = velocityConfigDTO.getDeploymentFrequency();
        String calculationField = deploymentFrequency.getVelocityConfigFilters().getDeploymentFrequency().getCalculationField().toString();
        ImmutablePair<Long, Long> calculationTimeRange;

        Map<String, Object> deploymentFilters = deploymentFrequency.getVelocityConfigFilters().getDeploymentFrequency().getFilter();
        DefaultListRequest requestForDF = request.toBuilder().filter(deploymentFilters).build();

        Optional<Integration> optionalIntegration = integrationService.get(company, String.valueOf(deploymentFrequency.getIntegrationIds().get(0)));
        Integration integration = optionalIntegration.get();
        switch (Objects.requireNonNull(IntegrationType.fromString(integration.getApplication()))) {
            case AZURE_DEVOPS:
                WorkItemsFilter reqWorkItemsFilter = WorkItemsFilter.fromDefaultListRequest(request,
                        WorkItemsFilter.DISTINCT.none,
                        WorkItemsFilter.CALCULATION.issue_count);
                if ("workitem_updated_at".equals(calculationField)) {
                    reqWorkItemsFilter = reqWorkItemsFilter.toBuilder()
                            .workItemResolvedRange(ImmutablePair.nullPair())
                            .build();
                } else {
                    reqWorkItemsFilter = reqWorkItemsFilter.toBuilder()
                            .workItemUpdatedRange(ImmutablePair.nullPair())
                            .build();
                }
                WorkItemsMilestoneFilter reqSprintFilter = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");

                WorkItemsFilter workItemsFilter = WorkItemsFilter.fromDefaultListRequest(requestForDF,
                        WorkItemsFilter.DISTINCT.none,
                        WorkItemsFilter.CALCULATION.issue_count);
                WorkItemsMilestoneFilter sprintFilter = WorkItemsMilestoneFilter.fromSprintRequest(requestForDF, "workitem_");
                response = adoDoraService.getTimeSeriesDataForDeployment(company, workItemsFilter, sprintFilter, reqWorkItemsFilter, reqSprintFilter, finalOuConfig, calculationField);

                if ("workitem_resolved_at".equals(calculationField)) {
                    calculationTimeRange = reqWorkItemsFilter.getWorkItemResolvedRange();
                } else {
                    calculationTimeRange = reqWorkItemsFilter.getWorkItemUpdatedRange();
                }
                DoraSingleStateDTO statsADO = response.getStats();
                if (calculationTimeRange != null && calculationTimeRange.getLeft() != null && calculationTimeRange.getRight() != null) {
                    double totalDays = DoraCalculationUtils.getTimeDifference(calculationTimeRange);
                    double countPerDay = (double) statsADO.getTotalDeployment() / totalDays;
                    DoraSingleStateDTO.Band band = DoraCalculationUtils.calculateDeploymentFrequencyBand(countPerDay);
                    response = response.toBuilder().timeSeries(response.getTimeSeries()).stats(statsADO.toBuilder().band(band).countPerDay(countPerDay).totalDeployment(statsADO.getTotalDeployment()).build()).build();
                }
                break;
            case JIRA:
                // Get Time series data for Deployment Frequency
                JiraIssuesFilter dfFilters = jiraFilterParser.createFilter(company, requestForDF, JiraIssuesFilter.CALCULATION.ticket_count, JiraIssuesFilter.DISTINCT.none, null, null, false, false);
                JiraIssuesFilter reqFilters = jiraFilterParser.createFilter(company, request, JiraIssuesFilter.CALCULATION.ticket_count, JiraIssuesFilter.DISTINCT.none, null, null, false, false);
                if ("issue_updated_at".equals(calculationField)) {
                    reqFilters = reqFilters.toBuilder()
                            .issueResolutionRange(ImmutablePair.nullPair())
                            .issueReleasedRange(ImmutablePair.nullPair())
                            .build();
                } else if ("issue_resolved_at".equals(calculationField)) {
                    reqFilters = reqFilters.toBuilder()
                            .issueUpdatedRange(ImmutablePair.nullPair())
                            .issueReleasedRange(ImmutablePair.nullPair())
                            .build();
                } else {
                    reqFilters = reqFilters.toBuilder()
                            .issueResolutionRange(ImmutablePair.nullPair())
                            .issueUpdatedRange(ImmutablePair.nullPair())
                            .build();
                }

                response = jiraDoraService.getTimeSeriesDataForDeployment(company, dfFilters, reqFilters, calculationField, finalOuConfig);
                DoraSingleStateDTO stats = response.getStats();
                if ("issue_resolved_at".equals(calculationField)) {
                    calculationTimeRange = reqFilters.getIssueResolutionRange();
                } else if("issue_updated_at".equals(calculationField)) {
                    calculationTimeRange = reqFilters.getIssueUpdatedRange();
                } else {
                    calculationTimeRange = reqFilters.getIssueReleasedRange();
                }
                if (calculationTimeRange != null && calculationTimeRange.getLeft() != null && calculationTimeRange.getRight() != null) {
                    double totalDays = DoraCalculationUtils.getTimeDifference(calculationTimeRange);
                    double countPerDay = (double) stats.getTotalDeployment() / totalDays;
                    DoraSingleStateDTO.Band band = DoraCalculationUtils.calculateDeploymentFrequencyBand(countPerDay);
                    response = response.toBuilder().timeSeries(response.getTimeSeries()).stats(stats.toBuilder().band(band).countPerDay(countPerDay).totalDeployment(stats.getTotalDeployment()).build()).build();
                }
                break;
            default:
                throw new BadRequestException("Invalid integration id " + deploymentFrequency.getIntegrationIds() + " for Issue Management");
        }
        return response;
    }

    public DoraResponseDTO generateDFCountForCICD(
            String company, DefaultListRequest originalRequest, VelocityConfigDTO velocityConfigDTO
    ) throws SQLException, BadRequestException {
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(
                    company, IntegrationType.getCICDIntegrationTypes(), originalRequest
            );
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error(
                    "[{}] Unable to process the OU config in 'dora/deployment_frequency' for the request: {}",
                    company,
                    originalRequest,
                    e
            );
        }
        OUConfiguration finalOuConfig = ouConfig;
        DoraResponseDTO response = DoraResponseDTO.builder().build();
        Map<String, Object> requestFilter = request.getFilter() != null ? request.getFilter() : new HashMap<>();

        Map<String, Object> velocityFilter = velocityConfigDTO.getDeploymentFrequency()
                .getVelocityConfigFilters().getDeploymentFrequency().getFilter();
        velocityFilter = velocityFilter!= null ? velocityFilter : new HashMap<>();

        Map<String, List<String>> jobRunParams = velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getEvent().getParams();
        addJobRunParamsInWorkflowProfileFilters(jobRunParams, velocityFilter);

        VelocityConfigDTO.FilterTypes filterTypes = velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency();
        if(filterTypes.getIsCiJob() != null){
            velocityFilter.put("is_ci_job", filterTypes.getIsCiJob());
        }
        if(filterTypes.getIsCdJob() != null){
            velocityFilter.put("is_cd_job", filterTypes.getIsCdJob());
        }
        request = request.toBuilder().filter(mergeTwoMaps(requestFilter, velocityFilter)).build();
        log.debug("filter = {}", request);
        String stackField = null;
        if (!CollectionUtils.isEmpty(originalRequest.getStacks()))
            stackField = originalRequest.getStacks().get(0);
        String calculationField = velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getCalculationField().toString();
        response = ciCdDoraService.calculateNewDeploymentFrequency(
                company, prepareCiCdJobRunFilter(request, calculationField), finalOuConfig, velocityConfigDTO, ("pipelines".equals(stackField)) ? "job_name" : stackField
        );
        return response;
    }

    private void addJobRunParamsInWorkflowProfileFilters(Map<String, List<String>> params, Map<String, Object> profileFilter) {
        if (params != null) {
            List<Map<String, Object>> modifiedParams =
                    CollectionUtils.emptyIfNull(params.keySet()).stream().map(s -> Map.of("name", s, "values", params.get(s)))
                            .collect(Collectors.toList());
            profileFilter.put("parameters", modifiedParams);
        }
    }

    public DoraResponseDTO generateCFRCountforCICD(
            String company, DefaultListRequest originalRequest, VelocityConfigDTO velocityConfigDTO
    ) throws BadRequestException {
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(
                    company, IntegrationType.getCICDIntegrationTypes(), originalRequest
            );
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error(
                    "[{}] Unable to process the OU config in 'dora/deployment_frequency' for the request: {}",
                    company,
                    originalRequest,
                    e
            );
        }
        OUConfiguration finalOuConfig = ouConfig;

        Map<String, Object> requestFilter = request.getFilter() != null ? request.getFilter() : new HashMap<>();

        Map<String, Object> velocityFDFilter = velocityConfigDTO.getChangeFailureRate()
                .getVelocityConfigFilters().getFailedDeployment().getFilter();
        VelocityConfigDTO.FilterTypes failedDepFilterTypes = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment();
        velocityFDFilter = velocityFDFilter != null ? velocityFDFilter : new HashMap<>();

        Map<String, List<String>> failedJobRunParams = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getEvent().getParams();
        addJobRunParamsInWorkflowProfileFilters(failedJobRunParams, velocityFDFilter);

        if(failedDepFilterTypes.getIsCiJob() != null){
            velocityFDFilter.put("is_ci_job", failedDepFilterTypes.getIsCiJob());
        }
        if(failedDepFilterTypes.getIsCdJob() != null){
            velocityFDFilter.put("is_cd_job", failedDepFilterTypes.getIsCdJob());
        }
        var requestForFD = finalOuConfig.getRequest();

        requestForFD = requestForFD.toBuilder().filter(mergeTwoMaps(requestFilter, velocityFDFilter)).build();

        CiCdJobRunsFilter totalCICDJobRunFilters = CiCdJobRunsFilter.builder().build();

        if(!velocityConfigDTO.getChangeFailureRate().getIsAbsoulte()) {

            Map<String, Object> velocityTDFilter = velocityConfigDTO.getChangeFailureRate()
                    .getVelocityConfigFilters().getTotalDeployment().getFilter();
            velocityTDFilter = velocityTDFilter != null ? velocityTDFilter : new HashMap<>();
            VelocityConfigDTO.FilterTypes totalDepFilterTypes = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment();

            Map<String, List<String>> totalJobRunParams = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().getEvent() != null
                    ? velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().getEvent().getParams() : new HashMap<>();
            addJobRunParamsInWorkflowProfileFilters(totalJobRunParams, velocityTDFilter);

            if (totalDepFilterTypes.getIsCiJob() != null) {
                velocityTDFilter.put("is_ci_job", totalDepFilterTypes.getIsCiJob());
            }
            if (totalDepFilterTypes.getIsCdJob() != null) {
                velocityTDFilter.put("is_cd_job", totalDepFilterTypes.getIsCdJob());
            }

            var requestForTD = finalOuConfig.getRequest();
            requestForTD = requestForTD.toBuilder().filter(mergeTwoMaps(requestFilter, velocityTDFilter)).build();
            log.debug("filter = {}", request);
            String calculationFieldTD = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().getCalculationField().toString();
            totalCICDJobRunFilters = prepareCiCdJobRunFilter(requestForTD, calculationFieldTD);
        }

        String stackField = null;
        if (!CollectionUtils.isEmpty(originalRequest.getStacks()))
            stackField = originalRequest.getStacks().get(0);

        String calculationFieldFD = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getCalculationField().toString();

        DoraResponseDTO response = ciCdDoraService.calculateNewChangeFailureRate(
                company, prepareCiCdJobRunFilter(requestForFD, calculationFieldFD), totalCICDJobRunFilters, finalOuConfig, velocityConfigDTO, ("pipelines".equals(stackField)) ? "job_name" : stackField
        );
        return response;
    }

    private CiCdJobRunsFilter prepareCiCdJobRunFilter(DefaultListRequest request, String calculationField) throws BadRequestException {

        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
        Map<String, Object> excludeFields = (Map<String, Object>) request.getFilter()
                .getOrDefault("exclude", Map.of());

        String startWith = "end_time";
        if("end_time".equals(calculationField))
            startWith = "start_time";
        final String removeCondition = startWith;
         request.getFilter().remove(removeCondition);

        return CiCdJobRunsFilter.builder()
                .across(MoreObjects.firstNonNull(
                        CiCdJobRunsFilter.DISTINCT.fromString(
                                request.getAcross()),
                        CiCdJobRunsFilter.DISTINCT.trend))
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .isCiJob((Boolean) MapUtils.emptyIfNull(request.getFilter()).getOrDefault("is_ci_job", null))
                .isCdJob((Boolean) MapUtils.emptyIfNull(request.getFilter()).getOrDefault("is_cd_job", null))
                .stacks(parseCiCdStacks(request.getStacks(), CiCdJobRunsFilter.DISTINCT.class))
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
                .instanceNames(getListOrDefault(request.getFilter(), "instance_names"))
                .integrationIds(getIntegrationIds(request))
                .types(CICD_TYPE.parseFromFilter(request))
                .projects(getListOrDefault(request.getFilter(), "projects"))
                .stageNames(getListOrDefault(request.getFilter(), "stage_name"))
                .stepNames(getListOrDefault(request.getFilter(), "step_name"))
                .stageStatuses(getListOrDefault(request.getFilter(), "stage_status"))
                .stepStatuses(getListOrDefault(request.getFilter(), "step_status"))
                .excludeServices(DefaultListRequestUtils.getListOrDefault(excludeFields, "services"))
                .excludeEnvironments(DefaultListRequestUtils.getListOrDefault(excludeFields, "environments"))
                .excludeInfrastructures(DefaultListRequestUtils.getListOrDefault(excludeFields, "infrastructures"))
                .excludeRepositories(DefaultListRequestUtils.getListOrDefault(excludeFields, "repositories"))
                .excludeBranches(DefaultListRequestUtils.getListOrDefault(excludeFields, "branches"))
                .excludeDeploymentTypes(DefaultListRequestUtils.getListOrDefault(excludeFields, "deployment_types"))
                .excludeRollback((Boolean) MapUtils.emptyIfNull(excludeFields).getOrDefault("rollback", null))
                .excludeTags(DefaultListRequestUtils.getListOrDefault(excludeFields, "tags"))
                .triageRuleNames(getListOrDefault(request.getFilter(), "triage_rule"))
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
                .excludeCiCdUserIds(DefaultListRequestUtils.getListOrDefault(excludeFields, "cicd_user_ids"))
                .excludeQualifiedJobNames(
                        parseCiCdQualifiedJobNames(
                                objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")
                        )
                )
                .partialMatch(partialMatchMap)
                .parameters(
                        parseCiCdJobRunParameters(
                                objectMapper,
                                getListOfObjectOrDefault(request.getFilter(), "parameters")
                        )
                )
                .qualifiedJobNames(
                        parseCiCdQualifiedJobNames(
                                objectMapper, getListOfObjectOrDefault(request.getFilter(), "qualified_job_names")
                        )
                )
                .endTimeRange(getTimeRange(request, "end_time"))
                .startTimeRange(getTimeRange(request, "start_time"))
                .orgProductsIds(getListOrDefault(request.getFilter(), "org_product_ids").stream()
                        .map(UUID::fromString).collect(Collectors.toSet()))
                .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(request.getAggInterval()),
                        CICD_AGG_INTERVAL.day))
                .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of())))
                .build();
    }

    public DoraResponseDTO generateCFRCountForIM(String company, DefaultListRequest originalRequest, VelocityConfigDTO velocityConfigDTO) throws SQLException, BadRequestException, InventoryException {
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getIssueManagementIntegrationTypes(), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/dora/change_failure_rate' for the request: {}", company, originalRequest, e);
        }
        DoraResponseDTO response = DoraResponseDTO.builder().build();
        Integer failedDeployment = 0;
        Integer totalDeployment;
        double failureRate;

        VelocityConfigDTO.ChangeFailureRate changeFailureRate = velocityConfigDTO.getChangeFailureRate();
        boolean isAbsolute = changeFailureRate.getIsAbsoulte();

        Map<String, Object> failedDeploymentFilters = changeFailureRate.getVelocityConfigFilters().getFailedDeployment().getFilter();
        Map<String, Object> totalDeploymentFilters = null;

        Optional<Integration> optionalIntegration = integrationService.get(company, String.valueOf(changeFailureRate.getIntegrationIds().get(0)));

        Integration integration = optionalIntegration.get();
        DefaultListRequest requestForFailedDeployment = request.toBuilder().filter(failedDeploymentFilters).build();
        switch (Objects.requireNonNull(IntegrationType.fromString(integration.getApplication()))) {
            case AZURE_DEVOPS:
                String failedDeploymentCalculationField = changeFailureRate.getVelocityConfigFilters().getFailedDeployment().getCalculationField().toString();
                WorkItemsFilter reqWorkItemsFilter = WorkItemsFilter.fromDefaultListRequest(request,
                        WorkItemsFilter.DISTINCT.none,
                        WorkItemsFilter.CALCULATION.issue_count);
                if ("workitem_updated_at".equals(failedDeploymentCalculationField)) {
                    reqWorkItemsFilter = reqWorkItemsFilter.toBuilder()
                            .workItemResolvedRange(ImmutablePair.nullPair())
                            .build();
                } else {
                    reqWorkItemsFilter = reqWorkItemsFilter.toBuilder()
                            .workItemUpdatedRange(ImmutablePair.nullPair())
                            .build();
                }

                WorkItemsMilestoneFilter reqSprintFilter = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");
                WorkItemsFilter workItemsFilter = WorkItemsFilter.fromDefaultListRequest(requestForFailedDeployment,
                        WorkItemsFilter.DISTINCT.none,
                        WorkItemsFilter.CALCULATION.issue_count);
                WorkItemsMilestoneFilter sprintFilter = WorkItemsMilestoneFilter.fromSprintRequest(requestForFailedDeployment, "workitem_");
                response = adoDoraService.getTimeSeriesDataForDeployment(
                        company, workItemsFilter, sprintFilter, reqWorkItemsFilter, reqSprintFilter, ouConfig, failedDeploymentCalculationField
                );

                if (!isAbsolute) {
                    // Get count of total deployment
                    String totalDeploymentCalculationField = changeFailureRate.getVelocityConfigFilters().getTotalDeployment().getCalculationField().toString();
                    WorkItemsFilter reqWorkItemsFilterTD = WorkItemsFilter.fromDefaultListRequest(request,
                            WorkItemsFilter.DISTINCT.none,
                            WorkItemsFilter.CALCULATION.issue_count);
                    if ("workitem_updated_at".equals(totalDeploymentCalculationField)) {
                        reqWorkItemsFilterTD = reqWorkItemsFilterTD.toBuilder()
                                .workItemResolvedRange(ImmutablePair.nullPair())
                                .build();
                    } else {
                        reqWorkItemsFilterTD = reqWorkItemsFilterTD.toBuilder()
                                .workItemUpdatedRange(ImmutablePair.nullPair())
                                .build();
                    }

                    totalDeploymentFilters = changeFailureRate.getVelocityConfigFilters().getTotalDeployment().getFilter();
                    DefaultListRequest requestForDeployment = request.toBuilder().filter(totalDeploymentFilters).build();
                    WorkItemsFilter deploymentWorkItemsFilter = WorkItemsFilter.fromDefaultListRequest(requestForDeployment,
                            WorkItemsFilter.DISTINCT.none,
                            WorkItemsFilter.CALCULATION.issue_count);
                    WorkItemsMilestoneFilter deploymentSprintFilter = WorkItemsMilestoneFilter.fromSprintRequest(requestForDeployment, "workitem_");

                    failedDeployment = response.getStats().getTotalDeployment();
                    totalDeployment = adoDoraService.getTimeSeriesDataForDeployment(
                            company, deploymentWorkItemsFilter, deploymentSprintFilter, reqWorkItemsFilterTD, reqSprintFilter, ouConfig, totalDeploymentCalculationField
                    ).getStats().getTotalDeployment();
                    if(failedDeployment > 0 && totalDeployment == 0){
                        throw new RuntimeException("Invalid configuration for Change Failure Rate for WorkFlow Profile: "+ velocityConfigDTO.getName());
                    }
                    failureRate = totalDeployment > 0.0 ? ((double) failedDeployment * 100) / totalDeployment : 0.0;

                    DoraSingleStateDTO.Band band = DoraCalculationUtils.calculateChangeFailureRateBand(failureRate);
                    response = response.toBuilder().stats(DoraSingleStateDTO.builder().band(band).failureRate(failureRate).totalDeployment(totalDeployment).build()).build();
                }
                break;
            case JIRA:
                // Get Time series data for Change Failure Rate
                String failedDeploymentCalculationFieldJira = changeFailureRate.getVelocityConfigFilters().getFailedDeployment().getCalculationField().toString();
                JiraIssuesFilter failedFilters = jiraFilterParser.createFilter(company, requestForFailedDeployment, JiraIssuesFilter.CALCULATION.ticket_count, JiraIssuesFilter.DISTINCT.none, null, null, false, false);
                JiraIssuesFilter requestFilters = jiraFilterParser.createFilter(company, request, JiraIssuesFilter.CALCULATION.ticket_count, JiraIssuesFilter.DISTINCT.none, null, null, false, false);
                if ("issue_updated_at".equals(failedDeploymentCalculationFieldJira)) {
                    requestFilters = requestFilters.toBuilder()
                            .issueResolutionRange(ImmutablePair.nullPair())
                            .build();
                } else {
                    requestFilters = requestFilters.toBuilder()
                            .issueUpdatedRange(ImmutablePair.nullPair())
                            .build();
                }

                response = jiraDoraService.getTimeSeriesDataForDeployment(
                        company, failedFilters, requestFilters, failedDeploymentCalculationFieldJira, ouConfig
                );

                if (!isAbsolute) {
                    // Get count of total deployment
                    String totalDeploymentCalculationFieldJira = changeFailureRate.getVelocityConfigFilters().getTotalDeployment().getCalculationField().toString();
                    JiraIssuesFilter requestFiltersTD = jiraFilterParser.createFilter(company, request, JiraIssuesFilter.CALCULATION.ticket_count, JiraIssuesFilter.DISTINCT.none, null, null, false, false);
                    if ("issue_updated_at".equals(totalDeploymentCalculationFieldJira)) {
                        requestFiltersTD = requestFiltersTD.toBuilder()
                                .issueResolutionRange(ImmutablePair.nullPair())
                                .build();
                    } else {
                        requestFiltersTD = requestFiltersTD.toBuilder()
                                .issueUpdatedRange(ImmutablePair.nullPair())
                                .build();
                    }

                    totalDeploymentFilters = changeFailureRate.getVelocityConfigFilters().getTotalDeployment().getFilter();
                    DefaultListRequest requestForDeployment = request.toBuilder().filter(totalDeploymentFilters).build();
                    JiraIssuesFilter deploymentFilters = jiraFilterParser.createFilter(company, requestForDeployment, JiraIssuesFilter.CALCULATION.ticket_count, JiraIssuesFilter.DISTINCT.none, null, null, false, false);

                    failedDeployment = response.getStats().getTotalDeployment();
                    totalDeployment = jiraDoraService.getTimeSeriesDataForDeployment(
                            company, deploymentFilters, requestFiltersTD, totalDeploymentCalculationFieldJira, ouConfig
                    ).getStats().getTotalDeployment();
                    if(failedDeployment > 0 && totalDeployment == 0){
                        throw new RuntimeException("Invalid configuration for Change Failure Rate for WorkFlow Profile: "+ velocityConfigDTO.getName());
                    }
                    failureRate = totalDeployment > 0.0 ? ((double) failedDeployment * 100) / totalDeployment : 0.0;

                    DoraSingleStateDTO.Band band = DoraCalculationUtils.calculateChangeFailureRateBand(failureRate);
                    response = response.toBuilder().stats(DoraSingleStateDTO.builder().band(band).failureRate(failureRate).totalDeployment(totalDeployment).build()).build();
                }
                break;
            default:
                throw new BadRequestException("Invalid integration id " + changeFailureRate.getIntegrationIds() + " for Issue Management");
        }
        return response.toBuilder().stats(response.getStats().toBuilder().isAbsolute(isAbsolute).build()).build();
    }

    public VelocityConfigDTO getVelocityConfigByOu(String company, DefaultListRequest request) throws SQLException {
        if (request.getOuIds() == null || request.getOuIds().stream().findFirst().isEmpty()) {
            throw new RuntimeException("ou_id is missing in request.");
        }
        int ouRefId = request.getOuIds().stream().findFirst().get();
        Optional<VelocityConfigDTO> velocityConfigDTO = velocityConfigsService.getByOuRefId(company, ouRefId);
        if (velocityConfigDTO.isEmpty()) {
            throw new RuntimeException("Failed to get workflow profile for ou_ref_id  " + ouRefId);
        }
        return velocityConfigDTO.get();
    }

    public DbListResponse getListForAzure(Boolean disableCache,
                                          String company,
                                          DefaultListRequest request,
                                          Map<String, Object> workflowFilters,
                                          VelocityConfigDTO velocityConfigDTO,
                                          final OUConfiguration finalOuConfig) throws Exception {
        String sortHash = Hashing.sha256().hashBytes(objectMapper.writeValueAsString(request.getSort()).getBytes()).toString();

        DefaultListRequest requestForDF = request.toBuilder().filter(workflowFilters).build();

        WorkItemsFilter reqWorkItemsFilter = WorkItemsFilter.fromDefaultListRequest(requestForDF,
                WorkItemsFilter.DISTINCT.none,
                null);
        WorkItemsMilestoneFilter reqSprintFilter = WorkItemsMilestoneFilter.fromSprintRequest(requestForDF, "workitem_");

        WorkItemsFilter workItemsFilter = WorkItemsFilter.fromDefaultListRequest(request,
                WorkItemsFilter.DISTINCT.none,
                null);
        WorkItemsMilestoneFilter sprintFilter = WorkItemsMilestoneFilter.fromSprintRequest(request, "workitem_");
        var page = request.getPage();
        var pageSize = request.getPageSize();
        return AggCacheUtils.cacheOrCall(disableCache, company,
                "/dora/drilldown/list_" + request.getPage() + "_" + request.getPageSize() + "_" + sortHash + "/azure_devops",
                workItemsFilter.generateCacheHash() + sprintFilter.generateCacheHash() + reqWorkItemsFilter.generateCacheHash() + reqSprintFilter.generateCacheHash() + finalOuConfig.hashCode() + velocityConfigDTO.getId() + velocityConfigDTO.getUpdatedAt(), workItemsFilter.getIntegrationIds(), objectMapper, aggCacheService,
                () -> adoDoraService.getDrillDownData(company, workItemsFilter, sprintFilter, reqWorkItemsFilter, reqSprintFilter, finalOuConfig, page, pageSize, true));
    }

    public DbListResponse getListForJira(Boolean disableCache,
                                         String company,
                                         DefaultListRequest request,
                                         Map<String, Object> workflowFilters,
                                         VelocityConfigDTO velocityConfigDTO,
                                         final OUConfiguration finalOuConfig) throws Exception {

        if (request.getFilter().containsKey("keys") && request.getFilter().containsKey("projects") && !request.getFilter().containsKey("projects")) {
            var sanitizedFilters = new HashMap<>(request.getFilter());
            sanitizedFilters.remove("projects");
            request = request.toBuilder().filter(sanitizedFilters).build();
        }
        request = request.toBuilder().filter(mergeTwoMaps(request.getFilter(), workflowFilters)).build();
        JiraIssuesFilter issueFilter;

        issueFilter = jiraFilterParser.createFilter(company, request, null, null, null, null, "", false, false);
        Optional<JiraIssuesFilter> linkedIssuesFilter;
        Optional<JiraIssuesFilter> workflowLinkedIssuesFilter;
        if (JiraIssueReadUtils.isLinkedIssuesRequired(issueFilter)) {
            linkedIssuesFilter = Optional.ofNullable(jiraFilterParser.createFilter(company, request, null, null, null, null, "linked_", false));
        } else {
            linkedIssuesFilter = Optional.empty();
        }
        String sortHash = Hashing.sha256().hashBytes(objectMapper.writeValueAsString(request.getSort()).getBytes()).toString();
        JiraSprintFilter jiraSprintFilter = JiraSprintFilter.fromDefaultListRequest(request);
        final var page = request.getPage();
        final var pageSize = request.getPageSize();
        final var sort = request.getSort();
        return AggCacheUtils.cacheOrCall(disableCache, company,
                "/dora/drilldown/list_" + request.getPage() + "_" + request.getPageSize() + "_" + sortHash + "/jira",
                issueFilter.generateCacheHash() + finalOuConfig.hashCode() + velocityConfigDTO.getId() + velocityConfigDTO.getUpdatedAt(), issueFilter.getIntegrationIds(), objectMapper, aggCacheService,
                () -> jiraIssueService.list(company, jiraSprintFilter, issueFilter, linkedIssuesFilter,
                        finalOuConfig, Optional.of(velocityConfigDTO),
                        SortingConverter.fromFilter(MoreObjects.firstNonNull(sort, List.of())),
                        page,
                        pageSize));

    }

    public DbListResponse getListForJiraRelease(Boolean disableCache,
                                                String company,
                                                DefaultListRequest request,
                                                Map<String, Object> workflowFilters,
                                                VelocityConfigDTO velocityConfigDTO,
                                                final OUConfiguration finalOuConfig) throws Exception {

        if (request.getFilter().containsKey("keys") && request.getFilter().containsKey("projects") && !request.getFilter().containsKey("projects")) {
            var sanitizedFilters = new HashMap<>(request.getFilter());
            sanitizedFilters.remove("projects");
            request = request.toBuilder().filter(sanitizedFilters).build();
        }
        DefaultListRequest requestForDF = request.toBuilder().filter(workflowFilters).build();

        JiraIssuesFilter dfFilters = jiraFilterParser.createFilter(company, requestForDF, JiraIssuesFilter.CALCULATION.ticket_count, JiraIssuesFilter.DISTINCT.none, null, null, false, false);
        JiraIssuesFilter reqFilters = jiraFilterParser.createFilter(company, request, JiraIssuesFilter.CALCULATION.ticket_count, JiraIssuesFilter.DISTINCT.none, null, null, false, false);

        String sortHash = Hashing.sha256().hashBytes(objectMapper.writeValueAsString(request.getSort()).getBytes()).toString();
        final var page = request.getPage();
        final var pageSize = request.getPageSize();
        return AggCacheUtils.cacheOrCall(disableCache, company,
                "/dora/drilldown/list_" + request.getPage() + "_" + request.getPageSize() + "_" + sortHash + "/jira",
                dfFilters.generateCacheHash() + finalOuConfig.hashCode() + velocityConfigDTO.getId() + velocityConfigDTO.getUpdatedAt(), dfFilters.getIntegrationIds(), objectMapper, aggCacheService,
                () -> jiraDoraService.getDeploymentForJiraRelease(company, dfFilters, reqFilters, finalOuConfig, page, pageSize)
        );
    }

    private DbListResponse<DoraDrillDownDTO> getListForAzureAndReturnResponseDTO(Boolean disableCache,
                                                                            String company,
                                                                            DefaultListRequest originalRequest,
                                                                            Map<String, Object> workflowFilters,
                                                                            VelocityConfigDTO velocityConfigDTO,
                                                                                 final OUConfiguration finalOUConfig) throws Exception {
        DbListResponse<DbWorkItem> dbListResponse = getListForAzure(disableCache, company, originalRequest, workflowFilters, velocityConfigDTO, finalOUConfig);
        List<DbWorkItem> list = objectMapper.convertValue(dbListResponse.getRecords(), new TypeReference<List<DbWorkItem>>() {
        });
        List<DoraDrillDownDTO> records = new ArrayList<>();
        for (DbWorkItem issue : list) {
            DoraDrillDownDTO doraDrillDownDTO = objectMapper.convertValue(issue, DoraDrillDownDTO.class);
            doraDrillDownDTO = doraDrillDownDTO.toBuilder().integrationIdIM(issue.getIntegrationId()).labelsIM(issue.getLabels()).projectIM(issue.getProject()).build();
            records.add(doraDrillDownDTO);
        }
        return DbListResponse.<DoraDrillDownDTO>builder().records(records).totalCount(dbListResponse.getTotalCount()).totals(dbListResponse.getTotals()).build();
    }

    // Get List for Jira, Azure, SCM & CICD
    public DbListResponse<DoraDrillDownDTO> getList(Boolean disableCache,
                                                    String company,
                                                    DefaultListRequest originalRequest,
                                                    VelocityConfigDTO velocityConfigDTO) throws Exception {
        String integrationType = null;
        int integrationId = 0;
        Map<String, Object> workflowFilters = new HashMap<>();
        String calculationField = "";
        List<Integer> integrationIds = new ArrayList<>();

        if (originalRequest.getWidget().equals(DEPLOYMENT_FREQUENCY)) {
            integrationType = velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getIntegrationType();
            workflowFilters = velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getFilter();
            workflowFilters = workflowFilters != null ? workflowFilters : new HashMap<>();

            calculationField = velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getCalculationField().toString();
            integrationIds = velocityConfigDTO.getDeploymentFrequency().getIntegrationIds();
        } else if (originalRequest.getWidget().equals(CHANGE_FAILURE_RATE)) {
            integrationType = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getIntegrationType();
            workflowFilters = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getFilter();
            workflowFilters = workflowFilters != null ? workflowFilters : new HashMap<>();

            calculationField = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getCalculationField().toString();
            integrationIds = velocityConfigDTO.getChangeFailureRate().getIntegrationIds();
        }

        Map<String, Object> filter = originalRequest.getFilter() != null ? originalRequest.getFilter() : new HashMap<>();
        if(!calculationField.equals("")){
            filter.put(calculationField, originalRequest.getFilter().get("time_range"));
        }
        filter.put("integration_ids", integrationIds);
        originalRequest.toBuilder().filter(filter);

        DbListResponse<DoraDrillDownDTO> dbListResponse = null;
        List<DoraDrillDownDTO> listNew = new ArrayList<>();
        switch (integrationType) {
            case ("SCM"):
                dbListResponse = getScmDrillDownList(disableCache, company, originalRequest,velocityConfigDTO);
                break;
            case ("IM"):
                var request = originalRequest;
                OUConfiguration ouConfig = null;
                try {
                    ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getIssueManagementIntegrationTypes(), originalRequest);
                    request = ouConfig.getRequest();
                } catch (SQLException e) {
                    log.error("[{}] Unable to process the OU config in '/issue_mgmt/workitems/list' for the request: {}", company, originalRequest, e);
                }
                final OUConfiguration finalOUConfig = ouConfig;
                Optional<Integration> integrationOptional = integrationService.get(company,String.valueOf(integrationIds.get(0)));
                if(integrationOptional.isEmpty()){
                    throw new RuntimeException("Invalid integration id " + integrationIds);
                }
                Integration integration = integrationOptional.get();

                switch(Objects.requireNonNull(IntegrationType.fromString(integration.getApplication()))){
                    case AZURE_DEVOPS:
                        dbListResponse = getListForAzureAndReturnResponseDTO(disableCache, company, request, workflowFilters,velocityConfigDTO, finalOUConfig);
                        break;
                    case JIRA:
                        if ("released_in".equals(calculationField)) {
                            dbListResponse = getListForJiraFromJiraReleaseAndReturnResponseDTO(disableCache, company, request, workflowFilters, velocityConfigDTO, finalOUConfig);
                        } else {
                            dbListResponse = getListForJiraAndReturnResponseDTO(disableCache, company, request, workflowFilters, velocityConfigDTO, finalOUConfig);
                        }
                        break;
                    default:
                        throw new RuntimeException("Invalid integration type for IM integration_id:" + integrationId);
                }
                break;
            case ("CICD"):
                dbListResponse = getListForCICDAndReturnResponseDTO(company, originalRequest, velocityConfigDTO, listNew, workflowFilters);
                break;
        }
        return  dbListResponse;
    }

    public DbListResponse<DoraDrillDownDTO> getScmDrillDownList(Boolean disableCache,
                                                                String company,
                                                                DefaultListRequest originalRequest,
                                                                VelocityConfigDTO velocityConfigDTO) throws Exception {
        String deploymentRoute = StringUtils.EMPTY;
        String deploymentCriteria = StringUtils.EMPTY;
        String calculationField = StringUtils.EMPTY;
        Map<String, Map<String, List<String>>> profileFilters = new HashMap<>();

        if(originalRequest.getWidget().equals(DEPLOYMENT_FREQUENCY)) {
             deploymentRoute = velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getDeploymentRoute().toString();
             deploymentCriteria = velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getDeploymentCriteria().toString();
             calculationField = velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getCalculationField().toString();
             profileFilters = velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getScmFilters();
        }
        else if(originalRequest.getWidget().equals(CHANGE_FAILURE_RATE)) {
             deploymentRoute = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getDeploymentRoute().toString();
             deploymentCriteria = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getDeploymentCriteria().toString();
             calculationField = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getCalculationField().toString();
             profileFilters = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getScmFilters();
        }

        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/scm/prs/list' for the request: {}", company, originalRequest, e);
        }
        Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
        List<DoraDrillDownDTO> resultList = new ArrayList<>();
        final var scmProfileFilters = profileFilters;
        final var finalDeploymentCriteria = deploymentCriteria;
        final var finalCalculationField = calculationField;

        if("pr".equals(deploymentRoute)) {
            final ScmPrFilter scmPrFilter = ScmPrFilter.fromDefaultListRequest(request, null, null);
            validatePartialMatchFilter(company, scmPrFilter.getPartialMatch(), ScmAggService.PRS_PARTIAL_MATCH_COLUMNS, ScmAggService.PRS_PARTIAL_MATCH_ARRAY_COLUMNS);
            final var finalOuConfig = ouConfig;
            final var page = request.getPage();
            final var pageSize = request.getPageSize();

            Callable<DbListResponse<DbScmPullRequest>> callable = () -> scmDoraService.getPrBasedDrillDownData(company,
                    scmPrFilter,
                    sorting,
                    finalOuConfig,
                    page,
                    pageSize,
                    scmProfileFilters,
                    finalDeploymentCriteria);

            DbListResponse<DbScmPullRequest> dbListResponse = AggCacheUtils.cacheOrCall(
                    disableCache,
                    company,
                    "/dora/drilldown/list_" + request.getPage() + "_" + request.getPageSize() + "_" + sorting.entrySet().stream().map(e -> e.getKey() + "-" + e.getValue()).collect(Collectors.joining(",")) + "/scm",
                    scmPrFilter.generateCacheHash() + finalOuConfig.hashCode() + velocityConfigDTO.getId() + velocityConfigDTO.getUpdatedAt(),
                    scmPrFilter.getIntegrationIds(),
                    objectMapper,
                    aggCacheService,
                    callable);

            List<DbScmPullRequest> list = objectMapper.convertValue(dbListResponse.getRecords(), new TypeReference<List<DbScmPullRequest>>() {
            });
            for (DbScmPullRequest dbScmPullRequest : list) {
                DoraDrillDownDTO doraDrillDownDTO = objectMapper.convertValue(dbScmPullRequest, DoraDrillDownDTO.class);
                resultList.add(doraDrillDownDTO);
            }
            return DbListResponse.<DoraDrillDownDTO>builder().records(resultList).totalCount(dbListResponse.getTotalCount()).totals(dbListResponse.getTotals()).build();
        }
        else {

            final var finalOuConfig = ouConfig;
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            ScmCommitFilter scmCommitFilter = ScmCommitFilter.fromDefaultListRequest(request, null, null, Map.of());
            Callable<DbListResponse<DbScmCommit>> callable = () -> scmDoraService.getCommitBasedDrillDownData(company,
                    scmCommitFilter,
                    finalOuConfig,
                    scmProfileFilters,
                    page,
                    pageSize,
                    sorting,
                    finalDeploymentCriteria,
                    finalCalculationField);

            DbListResponse<DbScmCommit> dbListResponse =  AggCacheUtils.cacheOrCall(disableCache,
                    company,
                    "/dora/drilldown/list_" + request.getPage() + "_" + request.getPageSize() + "_" + sorting.entrySet().stream().map(e -> e.getKey() + "-" + e.getValue()).collect(Collectors.joining(",")) + "/scm",
                    originalRequest.getFilter().toString() + originalRequest.getSort().toString() + finalOuConfig.hashCode() + velocityConfigDTO.getId() + velocityConfigDTO.getUpdatedAt(),
                    List.of(),
                    objectMapper,
                    aggCacheService,
                    callable);

            List<DbScmCommit> list = objectMapper.convertValue(dbListResponse.getRecords(), new TypeReference<List<DbScmCommit>>() {
            });

            List<DoraDrillDownDTO> listNew = new ArrayList<>();
            for (DbScmCommit dbScmCommit : list) {
                DoraDrillDownDTO doraDrillDownDTO = objectMapper.convertValue(dbScmCommit, DoraDrillDownDTO.class);
                listNew.add(doraDrillDownDTO);
            }
            return DbListResponse.<DoraDrillDownDTO>builder().records(listNew).totalCount(dbListResponse.getTotalCount()).totals(dbListResponse.getTotals()).build();
        }
    }

    public DbListResponse<DoraDrillDownDTO> getScmCommitList(Boolean disableCache,
                                                             String company,
                                                             DefaultListRequest originalRequest,
                                                             VelocityConfigDTO velocityConfigDTO) throws Exception {
        int integrationId = velocityConfigDTO.getChangeFailureRate().getIntegrationId();
        Map<String, Map<String, List<String>>> scmFilters = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getScmFilters();

        if (originalRequest.getWidget().equals(DEPLOYMENT_FREQUENCY)) {
            integrationId = velocityConfigDTO.getDeploymentFrequency().getIntegrationId();
            scmFilters = velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getScmFilters();
        }
        Map<String, Object> filter = originalRequest.getFilter() != null ? originalRequest.getFilter() : new HashMap<>();
        filter.put("commit_pushed_at", originalRequest.getFilter().get("time_range"));
        filter.put("integration_ids", List.of(integrationId));
        originalRequest.toBuilder().filter(filter);

        DbListResponse<DbScmCommit> dbListResponseSCM = getDirectMergeCommitsList(disableCache, company, originalRequest, velocityConfigDTO, scmFilters);
        List<DbScmCommit> list = objectMapper.convertValue(dbListResponseSCM.getRecords(), new TypeReference<List<DbScmCommit>>() {
        });

        List<DoraDrillDownDTO> listNew = new ArrayList<>();
        for (DbScmCommit dbScmCommit : list) {
            DoraDrillDownDTO doraDrillDownDTO = objectMapper.convertValue(dbScmCommit, DoraDrillDownDTO.class);
            listNew.add(doraDrillDownDTO);
        }
        return DbListResponse.<DoraDrillDownDTO>builder().records(listNew).totalCount(dbListResponseSCM.getTotalCount()).totals(dbListResponseSCM.getTotals()).build();
    }

    private DbListResponse<DbScmCommit> getDirectMergeCommitsList(Boolean disableCache,
                                                                  String company,
                                                                  DefaultListRequest originalRequest,
                                                                  VelocityConfigDTO velocityConfigDTO,
                                                                  Map<String, Map<String, List<String>>> scmFilters) throws Exception {
        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.getSCMIntegrationTypes(), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/scm/prs/list' for the request: {}", company, originalRequest, e);
        }

        Map<String, SortingOrder> sorting = SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of()));
        ScmCommitFilter scmCommitFilter = ScmCommitFilter.fromDefaultListRequest(request, null, null, Map.of());

        final var finalOuConfig = ouConfig;
        final var page = request.getPage();
        final var pageSize = request.getPageSize();

        return AggCacheUtils.cacheOrCall(disableCache,
                company,
                "/dora/drilldown/scm-commits/list_" + request.getPage() + "_" + request.getPageSize() + "_" + sorting.entrySet().stream().map(e -> e.getKey() + "-" + e.getValue()).collect(Collectors.joining(",")) + "/scm",
                originalRequest.getFilter().toString() + originalRequest.getSort().toString() + finalOuConfig.hashCode() + velocityConfigDTO.getId() + velocityConfigDTO.getUpdatedAt(),
                List.of(),
                objectMapper,
                aggCacheService, () -> scmDoraService.getScmCommitDrillDownData(company,
                        scmCommitFilter,
                        finalOuConfig,
                        scmFilters,
                        page,
                        pageSize,
                        sorting));
    }

    private void validatePartialMatchFilter(String company,
                                            Map<String, Map<String, String>> partialMatchMap,
                                            Set<String> partialMatchColumns, Set<String> partialMatchArrayColumns) {
        ArrayList<String> partialMatchKeys = new ArrayList<>(partialMatchMap.keySet());
        List<String> invalidPartialMatchKeys = partialMatchKeys.stream()
                .filter(key -> (!partialMatchColumns.contains(key)) && (!partialMatchArrayColumns.contains(key)))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(invalidPartialMatchKeys)) {
            log.warn("Company - " + company + ": " + String.join(",", invalidPartialMatchKeys)
                    + " are not valid fields for scm file partial match based filter");
        }
    }

    // CICD
    private DbListResponse<DoraDrillDownDTO> getListForCICDAndReturnResponseDTO(String company,
                                                                                DefaultListRequest originalRequest,
                                                                                VelocityConfigDTO velocityConfigDTO,
                                                                                List<DoraDrillDownDTO> listNew,
                                                                                Map<String, Object> workflowFilters) throws Exception {
        if (originalRequest.getWidget().equals(DEPLOYMENT_FREQUENCY)) {
            Map<String, List<String>> deployFreqJobRunParams = velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getEvent().getParams();
            addJobRunParamsInWorkflowProfileFilters(deployFreqJobRunParams, workflowFilters);

        } else if (originalRequest.getWidget().equals(CHANGE_FAILURE_RATE)) {
            Map<String, List<String>> failedJobRunParams = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getEvent().getParams();
            addJobRunParamsInWorkflowProfileFilters(failedJobRunParams, workflowFilters);
        }

        DbListResponse<CICDJobRunDTO> dbListResponse = getListForCICD(company, originalRequest, velocityConfigDTO, workflowFilters);
        List<CICDJobRunDTO> list = dbListResponse.getRecords();
        for (CICDJobRunDTO cicdJobRunDTO : list) {
            DoraDrillDownDTO doraDrillDownDTO = fromCicdJobRunDTOTODoraDrillDownDTO(cicdJobRunDTO);
            listNew.add(doraDrillDownDTO);
        }
        return DbListResponse.<DoraDrillDownDTO>builder().records(listNew).totalCount(dbListResponse.getTotalCount()).totals(dbListResponse.getTotals()).build();
    }

    private DoraDrillDownDTO fromCicdJobRunDTOTODoraDrillDownDTO(CICDJobRunDTO cicdJobRunDTO) {
        return DoraDrillDownDTO.builder()
                .idCicd(cicdJobRunDTO.getId())
                .cicdJobId(cicdJobRunDTO.getCicdJobId())
                .jobRunNumber(cicdJobRunDTO.getJobRunNumber())
                .status(cicdJobRunDTO.getStatus())
                .startTime(cicdJobRunDTO.getStartTime())
                .duration(cicdJobRunDTO.getDuration())
                .endTime(cicdJobRunDTO.getEndTime())
                .cicdUserId(cicdJobRunDTO.getCicdUserId())
                .jobName(cicdJobRunDTO.getJobName())
                .jobNormalizedFullName(cicdJobRunDTO.getJobNormalizedFullName())
                .projectName(cicdJobRunDTO.getProjectName())
                .integrationId(cicdJobRunDTO.getIntegrationId())
                .scmCommitIds(cicdJobRunDTO.getScmCommitIds())
                .scmUrl(cicdJobRunDTO.getScmUrl())
                .logGcspath(cicdJobRunDTO.getLogGcspath())
                .cicdInstanceName(cicdJobRunDTO.getCicdInstanceName())
                .cicdBuildUrl(cicdJobRunDTO.getCicdBuildUrl())
                .cicdInstanceGuid(cicdJobRunDTO.getCicdInstanceGuid())
                .url(cicdJobRunDTO.getUrl())
                .logs(cicdJobRunDTO.getLogs())
                .cicdBranch(cicdJobRunDTO.getCicdBranch())
                .repoUrl(cicdJobRunDTO.getRepoUrl())
                .rollBack(cicdJobRunDTO.getRollBack())
                .environmentIds(cicdJobRunDTO.getEnvironmentIds())
                .infraIds(cicdJobRunDTO.getInfraIds())
                .serviceIds(cicdJobRunDTO.getServiceIds())
                .serviceTypes(cicdJobRunDTO.getServiceTypes())
                .tags(cicdJobRunDTO.getTags())
                .build();
    }

    public DbListResponse getListForCICD(String company,
                                         DefaultListRequest originalRequest,
                                         VelocityConfigDTO velocityConfigDTO,
                                         Map<String, Object> workflowFilters) throws Exception {

        var request = originalRequest;
        OUConfiguration ouConfig = null;
        try {
            ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, Set.of(IntegrationType.JENKINS, IntegrationType.CIRCLECI, IntegrationType.DRONECI, IntegrationType.HARNESSNG,
                    IntegrationType.AZURE_PIPELINES, IntegrationType.AZURE_DEVOPS, IntegrationType.GITLAB), originalRequest);
            request = ouConfig.getRequest();
        } catch (SQLException e) {
            log.error("[{}] Unable to process the OU config in '/cicd/job_runs/list' for the request: {}", company, originalRequest, e);
        }
        Map<String, Object> requestFilter = request.getFilter() != null ? request.getFilter(): new HashMap<>();

        request = request.toBuilder().filter(mergeTwoMaps(requestFilter, workflowFilters)).build();

        Map<String, Object> excludeFields = (Map<String, Object>) request.getFilter()
                .getOrDefault("exclude", Map.of());
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));

        List<String> jobIds = null;
        List<Integer> integrationIds = null;
        VelocityConfigDTO.FilterTypes filterTypes = VelocityConfigDTO.FilterTypes.builder().build();
        if (originalRequest.getWidget().equals(DEPLOYMENT_FREQUENCY)) {
            jobIds = velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getEvent().getValues();
            filterTypes = velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency();
            integrationIds = velocityConfigDTO.getDeploymentFrequency().getIntegrationIds();
        } else if (originalRequest.getWidget().equals(CHANGE_FAILURE_RATE)) {
            jobIds = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getEvent().getValues();
            filterTypes = velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment();
            integrationIds = velocityConfigDTO.getChangeFailureRate().getIntegrationIds();
        }

        return ciCdAggsService.listCiCdJobRunsForDora(
                company,
                CiCdJobRunsFilter.builder()
                        .across(null)
                        .isCiJob(filterTypes.getIsCiJob() != null ? filterTypes.getIsCiJob() : null)
                        .isCdJob(filterTypes.getIsCdJob() != null ? filterTypes.getIsCdJob() : null)
                        .stacks(parseCiCdStacks(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "stacks"), CiCdJobRunsFilter.DISTINCT.class))
                        .cicdUserIds(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "cicd_user_ids"))
                        .services(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "services"))
                        .environments(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "environments"))
                        .infrastructures(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "infrastructures"))
                        .repositories(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "repositories"))
                        .branches(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "branches"))
                        .deploymentTypes(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "deployment_types"))
                        .rollback((Boolean) MapUtils.emptyIfNull(request.getFilter()).getOrDefault("rollback", null))
                        .tags(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "tags"))
                        .jobNames(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "job_names"))
                        .jobNormalizedFullNames(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "job_normalized_full_names"))
                        .jobStatuses(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "job_statuses"))
                        .instanceNames(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "instance_names"))
                        .startTimeRange(getTimeRange(request, "start_time"))
                        .endTimeRange(getTimeRange(request, "end_time"))
                        .integrationIds(DefaultListRequestUtils.getListOrDefault(request, "integration_ids"))
                        .types(CICD_TYPE.parseFromFilter(request))
                        .projects(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "projects"))
                        .triageRuleNames(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "triage_rule"))
                        .stageNames(getListOrDefault(request.getFilter(), "stage_name"))
                        .stepNames(getListOrDefault(request.getFilter(), "step_name"))
                        .stageStatuses(getListOrDefault(request.getFilter(), "stage_status"))
                        .stepStatuses(getListOrDefault(request.getFilter(), "step_status"))
                        .excludeServices(DefaultListRequestUtils.getListOrDefault(excludeFields, "services"))
                        .excludeEnvironments(DefaultListRequestUtils.getListOrDefault(excludeFields, "environments"))
                        .excludeInfrastructures(DefaultListRequestUtils.getListOrDefault(excludeFields, "infrastructures"))
                        .excludeRepositories(DefaultListRequestUtils.getListOrDefault(excludeFields, "repositories"))
                        .excludeBranches(DefaultListRequestUtils.getListOrDefault(excludeFields, "branches"))
                        .excludeDeploymentTypes(DefaultListRequestUtils.getListOrDefault(excludeFields, "deployment_types"))
                        .excludeRollback((Boolean) MapUtils.emptyIfNull(excludeFields).getOrDefault("rollback", null))
                        .excludeTags(DefaultListRequestUtils.getListOrDefault(excludeFields, "tags"))
                        .excludeTriageRuleNames(DefaultListRequestUtils.getListOrDefault(excludeFields, "triage_rule"))
                        .excludeJobNames(DefaultListRequestUtils.getListOrDefault(excludeFields, "job_names"))
                        .excludeJobNormalizedFullNames(DefaultListRequestUtils.getListOrDefault(excludeFields, "job_normalized_full_names"))
                        .excludeJobStatuses(DefaultListRequestUtils.getListOrDefault(excludeFields, "job_statuses"))
                        .excludeInstanceNames(DefaultListRequestUtils.getListOrDefault(excludeFields, "instance_names"))
                        .excludeProjects(DefaultListRequestUtils.getListOrDefault(excludeFields, "projects"))
                        .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                        .excludeCiCdUserIds(DefaultListRequestUtils.getListOrDefault(excludeFields, "cicd_user_ids"))
                        .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                        .excludeStageNames(getListOrDefault(excludeFields, "stage_name"))
                        .excludeStepNames(getListOrDefault(excludeFields, "step_name"))
                        .excludeStageStatuses(getListOrDefault(excludeFields, "stage_status"))
                        .excludeStepStatuses(getListOrDefault(excludeFields, "step_status"))
                        .orgProductsIds(DefaultListRequestUtils.getListOrDefault(request.getFilter(), "org_product_ids").stream()
                                .map(UUID::fromString).collect(Collectors.toSet()))
                        .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(request.getFilter(), "parameters")))
                        .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(request.getFilter(), "qualified_job_names")))
                        .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of())))
                        .partialMatch(partialMatchMap)
                        .build(),
                request.getPage(),
                request.getPageSize(),
                ouConfig,
                jobIds);
    }

    private DbListResponse<DoraDrillDownDTO> getListForJiraAndReturnResponseDTO(Boolean disableCache,
                                                                                String company,
                                                                                DefaultListRequest originalRequest,
                                                                                Map<String, Object> workflowFilters,
                                                                                VelocityConfigDTO velocityConfigDTO,
                                                                                final OUConfiguration finalOUConfig) throws Exception {
        DbListResponse<DbJiraIssue> dbListResponse = getListForJira(disableCache, company, originalRequest, workflowFilters,velocityConfigDTO, finalOUConfig);
        List<DbJiraIssue> list = objectMapper.convertValue(dbListResponse.getRecords(), new TypeReference<List<DbJiraIssue>>() {
        });
        List<DoraDrillDownDTO> records = new ArrayList<>();
        for (DbJiraIssue issue : list) {
            DoraDrillDownDTO doraDrillDownDTO = objectMapper.convertValue(issue, DoraDrillDownDTO.class);
            doraDrillDownDTO = doraDrillDownDTO.toBuilder().createdAtIM(issue.getCreatedAt()).integrationIdIM(issue.getIntegrationId()).labelsIM(issue.getLabels()).projectIM(issue.getProject()).build();
            records.add(doraDrillDownDTO);
        }
        return DbListResponse.<DoraDrillDownDTO>builder().records(records).totalCount(dbListResponse.getTotalCount()).totals(dbListResponse.getTotals()).build();
    }

    private DbListResponse<DoraDrillDownDTO> getListForJiraFromJiraReleaseAndReturnResponseDTO(Boolean disableCache,
                                                                                               String company,
                                                                                               DefaultListRequest originalRequest,
                                                                                               Map<String, Object> workflowFilters,
                                                                                               VelocityConfigDTO velocityConfigDTO,
                                                                                               final OUConfiguration finalOUConfig) throws Exception {
        DbListResponse<JiraReleaseResponse> dbListResponse = getListForJiraRelease(disableCache, company, originalRequest, workflowFilters, velocityConfigDTO, finalOUConfig);
        List<JiraReleaseResponse> list = objectMapper.convertValue(dbListResponse.getRecords(), new TypeReference<>() {
        });
        List<DoraDrillDownDTO> records = new ArrayList<>();
        for (JiraReleaseResponse release : list) {
            DoraDrillDownDTO doraDrillDownDTO = DoraDrillDownDTO.builder().name(release.getName())
                    .releaseEndTime(release.getReleaseEndTime())
                    .project(release.getProject())
                    .issueCount(release.getIssueCount())
                    .isActiveAzure(false)
                    .build();
            records.add(doraDrillDownDTO);
        }
        return DbListResponse.<DoraDrillDownDTO>builder().records(records).totalCount(dbListResponse.getTotalCount()).totals(dbListResponse.getTotals()).build();
    }

    private List<String> getIntegrationIds(DefaultListRequest filter) {
        return CollectionUtils.isNotEmpty(getListOrDefault(filter.getFilter(), "cicd_integration_ids")) ?
                getListOrDefault(filter.getFilter(), "cicd_integration_ids") :
                getListOrDefault(filter.getFilter(), "integration_ids");
    }

    private Map<String, Object> mergeTwoMaps(Map<String, Object> mainMap, Map<String, Object> otherMap) {
        return Stream.concat(mainMap.entrySet().stream(), otherMap.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (value1, value2) -> {
                    if (value1 instanceof List) {
                        return Stream.concat(((List<?>) value1).stream(), ((List<?>) value2).stream()).distinct().collect(Collectors.toList());
                    } else if (value1 instanceof Map) {
                        return mergeTwoMaps((Map<String, Object>) value1, (Map<String, Object>) value2);
                    }
                    return value2;
                }, HashMap::new));
    }

    public Map<String, List<String>> getCicdJobParams(String company, List<String> jobIds) throws Exception {
        Map<String, List<String>> result = new HashMap<>();
        List<Map<String, Object>> dbResults = ciCdDoraService.getCicdJobParams(company, jobIds);

        for (Map<String, Object> dbResult : dbResults) {
            String name = (String) dbResult.get("name");
            String value = (String) dbResult.get("value");

            if (result.get(name) != null) {
                List<String> listJobIds = result.get(name);
                listJobIds.add(value);
                result.put(name, listJobIds);
            } else {
                result.put(name, new ArrayList<>() {{
                    add(value);
                }});
            }
        }
        return result;
    }
}
