package io.levelops.commons.services.velocity_productivity.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.filters.VelocityFilter;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.DbHistogramBucket;
import io.levelops.commons.databases.models.response.DbHistogramResult;
import io.levelops.commons.databases.models.response.VelocityRatingResult;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.WorkItemFieldsMetaService;
import io.levelops.commons.databases.services.jira.utils.JiraFilterParser;
import io.levelops.commons.databases.services.velocity.VelocityAggsDatabaseService;
import io.levelops.commons.databases.services.velocity.WorkItemsType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;

@Log4j2
@Service
public class VelocityAggsService {
    private final ObjectMapper objectMapper;
    private final VelocityConfigsService velocityConfigsService;
    private final JiraFilterParser jiraFilterParser;
    private final VelocityAggsDatabaseService velocityAggsDatabaseService;
    private final IntegrationService integrationService;
    private final WorkItemFieldsMetaService workItemFieldsMetaService;
    private final Set<String> enablePrJiraCorrelationForTenants;
    private final Executor velocityTaskExecutor;

    @Autowired
    public VelocityAggsService(ObjectMapper objectMapper,
                               VelocityConfigsService velocityConfigsService,
                               JiraFilterParser jiraFilterParser,
                               VelocityAggsDatabaseService velocityAggsDatabaseService,
                               IntegrationService integrationService,
                               WorkItemFieldsMetaService workItemFieldsMetaService,
                               @Value("${velocity.pr_velocity.enable_pr_jira_correlation_for_tenants:}") String enablePrJiraCorrelationForTenants,
                               @Qualifier("velocityTaskExecutor") Executor velocityTaskExecutor) {
        this.objectMapper = objectMapper;
        this.velocityConfigsService = velocityConfigsService;
        this.jiraFilterParser = jiraFilterParser;
        this.velocityAggsDatabaseService = velocityAggsDatabaseService;
        this.integrationService = integrationService;
        this.workItemFieldsMetaService = workItemFieldsMetaService;
        this.enablePrJiraCorrelationForTenants = CommaListSplitter.splitToStream(enablePrJiraCorrelationForTenants)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        this.velocityTaskExecutor = velocityTaskExecutor;
        log.info("Enabling pr/jira correlation for pr_velocity for tenants: {}", enablePrJiraCorrelationForTenants);
    }

    private boolean getDisablePrJiraCorrelation(String company) {
        return !enablePrJiraCorrelationForTenants.contains(company);
    }

    public VelocityConfigDTO getVelocityConfig(final String company, DefaultListRequest filter) throws SQLException, NotFoundException {
        String velocityConfigId = filter.getFilterValue("velocity_config_id", String.class).orElse(null);
        if (StringUtils.isNotBlank(velocityConfigId)) {
            VelocityConfigDTO velocityConfigDTO = velocityConfigsService.get(company, velocityConfigId).orElseThrow(() -> new NotFoundException(String.format("Company %s, Velocity Config Id %s not found!", company, velocityConfigId)));
            log.info("velocityConfigDTO = {}", velocityConfigDTO);
            return velocityConfigDTO;
        }
        Optional<VelocityConfigDTO> defaultVelocityConfigDTO = velocityConfigsService.getDefaultConfig(company);
        log.info("defaultVelocityConfigDTO = {}", defaultVelocityConfigDTO);
        return defaultVelocityConfigDTO.orElseThrow(() -> new NotFoundException(String.format("Company %s, Default Velocity Config not found!", company)));
    }

    private WorkItemsType calculateWorkItemsType(final String company, DefaultListRequest filter) throws SQLException {
        List<Integer> integrationIds = CollectionUtils.emptyIfNull(getListOrDefault(filter, "integration_ids")).stream().map(Integer::parseInt).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(integrationIds)) {
            return WorkItemsType.NONE;
        }
        DbListResponse<Integration> dbListResponse = integrationService.listByFilter(company, null, null, null, integrationIds, null, 0, integrationIds.size());
        Set<IntegrationType> applications = dbListResponse.getRecords().stream().map(Integration::getApplication).map(IntegrationType::fromString).filter(Objects::nonNull).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(applications)) {
            return WorkItemsType.NONE;
        }
        if ((applications.contains(IntegrationType.JIRA)) && (applications.contains(IntegrationType.AZURE_DEVOPS))) {
            return WorkItemsType.JIRA_AND_WORK_ITEM;
        } else if (applications.contains(IntegrationType.JIRA)) {
            return WorkItemsType.JIRA;
        } else if (applications.contains(IntegrationType.AZURE_DEVOPS)) {
            return WorkItemsType.WORK_ITEM;
        } else {
            return WorkItemsType.NONE;
        }
    }

    private List<DbWorkItemField> getWorkItemFields(final String company, final WorkItemsFilter workItemsFilter) {
        if ((workItemsFilter == null) || (MapUtils.isEmpty(workItemsFilter.getCustomFields()))) {
            return List.of();
        }
        try {
            List<DbWorkItemField> workItemFields = workItemFieldsMetaService.listByFilter(company, workItemsFilter.getIntegrationIds(), true,
                    null, null, null, null, null, 0,
                    1000).getRecords();
            log.info("workItemFields = {}", workItemFields);
            return workItemFields;
        } catch (SQLException e) {
            log.error("Error while querying workitem field meta table. Reason: " + e.getMessage());
            return List.of();
        }
    }

    private WorkItemsType validateWorkItemsType(final String company, DefaultListRequest filter) throws SQLException, BadRequestException {
        //Check if listRequest has work_items_type
        WorkItemsType workItemsTypeFromRequest = WorkItemsType.fromListRequest(filter);
        log.info("workItemsTypeFromRequest = {}", workItemsTypeFromRequest);
        if(workItemsTypeFromRequest != null) {
            //If request has work_items_type use it
            return workItemsTypeFromRequest;
        }
        //If not calculate WorkItemsType from integration_ids
        WorkItemsType workItemsType = calculateWorkItemsType(company, filter);
        log.info("workItemsType = {}", workItemsType);
        if (workItemsType == WorkItemsType.JIRA_AND_WORK_ITEM) {
            throw new BadRequestException("Both Jira and Azure Work Items are not supported together for lead time!");
        }
        return workItemsType;
    }

    private WorkItemsMilestoneFilter parseWIMilestoneFilter(final DefaultListRequest filter) throws BadRequestException {
        WorkItemsMilestoneFilter workItemsMilestoneFilter = WorkItemsMilestoneFilter.fromSprintRequest(filter, "workitem_");
        log.info("workItemsMilestoneFilter = {}", workItemsMilestoneFilter);
        return workItemsMilestoneFilter;
    }

    private ScmPrFilter parseSCMPrFilter(final DefaultListRequest filter) throws BadRequestException {
        ScmPrFilter scmPrFilter = ScmPrFilter.fromDefaultListRequest(filter, null, null);
        log.info("scmPrFilter = {}", scmPrFilter);
        return scmPrFilter;
    }

    private ScmCommitFilter parseSCMCommitFilter(final DefaultListRequest filter) throws BadRequestException {
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) filter.getFilter().get("partial_match"));
        ScmCommitFilter scmCommitFilter = ScmCommitFilter.fromDefaultListRequest(filter, null, null, partialMatchMap);
        log.info("scmCommitFilter = {}", scmCommitFilter);
        return scmCommitFilter;
    }

    private CiCdJobRunsFilter parseCiCdJobRunsFilter(final DefaultListRequest filter) throws BadRequestException {
        CiCdJobRunsFilter ciCdJobRunsFilter = CiCdJobRunsFilter.fromDefaultListRequest(filter, null, null, objectMapper);
        log.info("ciCdJobRunsFilter = {}", ciCdJobRunsFilter);
        return ciCdJobRunsFilter;
    }

    private VelocityFilter parseVelocityFilter(final DefaultListRequest filter) {
        VelocityFilter velocityFilter = VelocityFilter.fromListRequest(filter);
        log.info("velocityFilter = {}", velocityFilter);
        return velocityFilter;
    }

    public List<DbAggregationResult> calculateVelocity(final String company,
                                                       DefaultListRequest originalFilter,
                                                       DefaultListRequest filter,
                                                       VelocityConfigDTO velocityConfigDTO,
                                                       OUConfiguration ouConfig) throws SQLException, BadRequestException, IOException {
        WorkItemsType workItemsType = validateWorkItemsType(company, filter);

        VelocityFilter velocityFilter = parseVelocityFilter(filter);
        if (velocityFilter.getAcross() == VelocityFilter.DISTINCT.values) {
            throw new BadRequestException("across value " + velocityFilter.getAcross() + " not supported!");
        }

        JiraIssuesFilter jiraFilter = null;
        WorkItemsFilter workItemsFilter = null;
        if(workItemsType == WorkItemsType.WORK_ITEM)
            workItemsFilter = getWorkItemsFilter(velocityFilter.getCalculation(), filter, originalFilter);
        else {
            jiraFilter =  getJiraFilter(velocityFilter.getCalculation(), company, filter, originalFilter);
            log.info("ingested_at = {}", jiraFilter.getIngestedAt());
        }

        log.info("jiraFilter = {}", jiraFilter);
        log.info("workItemsFilter = {}", workItemsFilter);

        List<List<DbAggregationResult>> dbListResponse = velocityAggsDatabaseService.calculateVelocityWithoutStacks(
                company, velocityConfigDTO, velocityFilter, workItemsType, jiraFilter, workItemsFilter,
                parseWIMilestoneFilter(filter), getScmPrFilter(velocityFilter.getCalculation(), filter, originalFilter),
                getScmCommitFilter(velocityFilter.getCalculation(), filter, originalFilter),
                getCicdJobRunsFilter(filter, originalFilter), getWorkItemFields(company, workItemsFilter), ouConfig,
                getDisablePrJiraCorrelation(company)).getRecords();

        if (CollectionUtils.isEmpty(dbListResponse)) {
            return Collections.emptyList();
        }
        if (velocityFilter.getAcross() == VelocityFilter.DISTINCT.trend) {
            log.info("dbListResponse = {}", DefaultObjectMapper.get().writeValueAsString(dbListResponse));
            return dbListResponse.stream().flatMap(Collection::stream).collect(Collectors.toList());
        } else {
            if (CollectionUtils.isEmpty(velocityFilter.getStacks())) {
                return dbListResponse.get(0);
            } else {
                return dbListResponse.stream().flatMap(Collection::stream).collect(Collectors.toList());
            }
        }
    }

    public DbListResponse<DbAggregationResult> calculateVelocityValues(final String company,
                                                                       DefaultListRequest originalFilter,
                                                                       DefaultListRequest filter,
                                                                       VelocityConfigDTO velocityConfigDTO,
                                                                       OUConfiguration ouConfig) throws SQLException, BadRequestException {
        WorkItemsType workItemsType = validateWorkItemsType(company, filter);
        VelocityFilter velocityFilter = parseVelocityFilter(filter);
        if (velocityFilter.getAcross() != VelocityFilter.DISTINCT.values) {
            throw new BadRequestException("across value " + velocityFilter.getAcross() + " not supported! For this endpoint only " + VelocityFilter.DISTINCT.values + " is supported.");
        }

        JiraIssuesFilter jiraFilter = null;
        WorkItemsFilter workItemsFilter = null;
        if(workItemsType == WorkItemsType.WORK_ITEM)
            workItemsFilter = getWorkItemsFilter(velocityFilter.getCalculation(), filter, originalFilter);
        else {
            jiraFilter =  getJiraFilter(velocityFilter.getCalculation(), company, filter, originalFilter);
            log.info("ingested_at = {}", jiraFilter.getIngestedAt());
        }

        log.info("jiraFilter = {}", jiraFilter);
        log.info("workItemsFilter = {}", workItemsFilter);

        DbListResponse<DbAggregationResult> dbListResponse = velocityAggsDatabaseService.calculateVelocityValues(
                company, velocityConfigDTO, velocityFilter, workItemsType, jiraFilter, workItemsFilter,
                parseWIMilestoneFilter(filter), getScmPrFilter(velocityFilter.getCalculation(), filter, originalFilter),
                getScmCommitFilter(velocityFilter.getCalculation(), filter, originalFilter), getCicdJobRunsFilter(filter, originalFilter), getWorkItemFields(company, workItemsFilter), ouConfig,
                getDisablePrJiraCorrelation(company));
        if (CollectionUtils.isEmpty(dbListResponse.getRecords())) {
            return DbListResponse.of(Collections.emptyList(), 0);
        }
        log.info("totalCount = {}", dbListResponse.getTotalCount());
        return dbListResponse;
    }

    public DbListResponse<DbHistogramBucket> calculateVelocityHistogram(final String company,
                                                                        DefaultListRequest originalFilter,
                                                                        DefaultListRequest filter,
                                                                        VelocityConfigDTO velocityConfigDTO,
                                                                        OUConfiguration ouConfig) throws SQLException, BadRequestException {
        WorkItemsType workItemsType = validateWorkItemsType(company, filter);

        VelocityFilter velocityFilter = parseVelocityFilter(filter);
        if (velocityFilter.getAcross() != VelocityFilter.DISTINCT.histogram) {
            throw new BadRequestException("across value " + velocityFilter.getAcross() + " not supported! For this endpoint only " + VelocityFilter.DISTINCT.histogram + " is supported.");
        }
        if (StringUtils.isBlank(velocityFilter.getHistogramStageName())) {
            throw new BadRequestException("histogram stage name cannot be null or empty");
        }
        if (velocityFilter.getHistogramBucketsCount() == null) {
            throw new BadRequestException("histogram bucket count cannot be null");
        }

        JiraIssuesFilter jiraFilter = null;
        WorkItemsFilter workItemsFilter = null;
        if(workItemsType == WorkItemsType.WORK_ITEM)
            workItemsFilter = getWorkItemsFilter(velocityFilter.getCalculation(), filter, originalFilter);
        else {
            jiraFilter =  getJiraFilter(velocityFilter.getCalculation(), company, filter, originalFilter);
            log.info("ingested_at = {}", jiraFilter.getIngestedAt());
        }
        log.info("jiraFilter = {}", jiraFilter);
        log.info("workItemsFilter = {}", workItemsFilter);

        DbHistogramResult dbHistogramResult = velocityAggsDatabaseService.calculateVelocityStageHistogram(
                company, velocityConfigDTO, velocityFilter, workItemsType, jiraFilter, workItemsFilter,
                parseWIMilestoneFilter(filter), getScmPrFilter(velocityFilter.getCalculation(), filter, originalFilter),
                getScmCommitFilter(velocityFilter.getCalculation(), filter, originalFilter), getCicdJobRunsFilter(filter, originalFilter), getWorkItemFields(company, workItemsFilter), ouConfig,
                getDisablePrJiraCorrelation(company));
        if (CollectionUtils.isEmpty(dbHistogramResult.getBuckets())) {
            return DbListResponse.of(Collections.emptyList(), 0);
        }
        log.info("data = {}", dbHistogramResult.getBuckets());
        return DbListResponse.of(dbHistogramResult.getBuckets(), dbHistogramResult.getBuckets().size());
    }

    private List<VelocityConfigDTO.Stage> getAllStages(final VelocityConfigDTO velocityConfigDTO) {
        List<VelocityConfigDTO.Stage> stages = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(velocityConfigDTO.getPreDevelopmentCustomStages())) {
            stages.addAll(velocityConfigDTO.getPreDevelopmentCustomStages());
        }
        if (CollectionUtils.isNotEmpty(velocityConfigDTO.getFixedStages())) {
            stages.addAll(velocityConfigDTO.getFixedStages());
        }
        if (CollectionUtils.isNotEmpty(velocityConfigDTO.getPostDevelopmentCustomStages())) {
            stages.addAll(velocityConfigDTO.getPostDevelopmentCustomStages());
        }
        return stages;
    }

    public DbListResponse<DbHistogramResult> calculateVelocityHistogramsSerial(final String company,
                                                                               DefaultListRequest originalFilter,
                                                                               DefaultListRequest filter,
                                                                               OUConfiguration ouConfig) throws SQLException, BadRequestException, NotFoundException {
        WorkItemsType workItemsType = validateWorkItemsType(company, filter);

        VelocityFilter velocityFilter = parseVelocityFilter(filter);
        if (velocityFilter.getAcross() != VelocityFilter.DISTINCT.histogram) {
            throw new BadRequestException("across value " + velocityFilter.getAcross() + " not supported! For this endpoint only " + VelocityFilter.DISTINCT.histogram + " is supported.");
        }

        if (velocityFilter.getHistogramBucketsCount() == null) {
            throw new BadRequestException("histogram bucket count cannot be null");
        }

        VelocityConfigDTO velocityConfigDTO = getVelocityConfig(company, filter);
        log.info("velocityConfigDTO = {}", velocityConfigDTO);

        JiraIssuesFilter jiraFilter = null;
        WorkItemsFilter workItemsFilter = null;
        if(workItemsType == WorkItemsType.WORK_ITEM)
            workItemsFilter = getWorkItemsFilter(velocityFilter.getCalculation(), filter, originalFilter);
        else {
            jiraFilter =  getJiraFilter(velocityFilter.getCalculation(), company, filter, originalFilter);
            log.info("ingested_at = {}", jiraFilter.getIngestedAt());
        }
        log.info("jiraFilter = {}", jiraFilter);
        log.info("workItemsFilter = {}", workItemsFilter);

        List<VelocityConfigDTO.Stage> stages = getAllStages(velocityConfigDTO);
        List<DbHistogramResult> stageResults = new ArrayList<>();
        for (int i = 0; i < stages.size(); i++) {
            String stageName = stages.get(i).getName();
            VelocityFilter velocityFilterUpdated = velocityFilter.toBuilder().histogramStageName(stageName).build();
            DbHistogramResult dbHistogramResult = velocityAggsDatabaseService.calculateVelocityStageHistogram(company,
                    velocityConfigDTO, velocityFilterUpdated, workItemsType, jiraFilter, workItemsFilter,
                    parseWIMilestoneFilter(filter), getScmPrFilter(velocityFilter.getCalculation(), filter, originalFilter),
                    getScmCommitFilter(velocityFilter.getCalculation(), filter, originalFilter), getCicdJobRunsFilter(filter, originalFilter),
                    getWorkItemFields(company, workItemsFilter), ouConfig, getDisablePrJiraCorrelation(company));
            dbHistogramResult = dbHistogramResult.toBuilder().index(i).name(stageName).build();
            stageResults.add(dbHistogramResult);
        }
        return DbListResponse.of(stageResults, stageResults.size());
    }

    public DbListResponse<DbHistogramResult> calculateVelocityHistograms(final String company,
                                                                         DefaultListRequest originalFilter,
                                                                         DefaultListRequest filter,
                                                                         VelocityConfigDTO velocityConfigDTO,
                                                                         OUConfiguration ouConfig) throws SQLException, BadRequestException {
        WorkItemsType workItemsType = validateWorkItemsType(company, filter);
        VelocityFilter velocityFilter = parseVelocityFilter(filter);
        if (velocityFilter.getAcross() != VelocityFilter.DISTINCT.histogram) {
            throw new BadRequestException("across value " + velocityFilter.getAcross() + " not supported! For this endpoint only " + VelocityFilter.DISTINCT.histogram + " is supported.");
        }

        if (velocityFilter.getHistogramBucketsCount() == null) {
            throw new BadRequestException("histogram bucket count cannot be null");
        }

        WorkItemsMilestoneFilter workItemsMilestoneFilter = parseWIMilestoneFilter(filter);
        JiraIssuesFilter jiraFilter = null;
        WorkItemsFilter workItemsFilter = null;
        if(workItemsType == WorkItemsType.WORK_ITEM)
            workItemsFilter = getWorkItemsFilter(velocityFilter.getCalculation(), filter, originalFilter);
        else {
            jiraFilter =  getJiraFilter(velocityFilter.getCalculation(), company, filter, originalFilter);
            log.info("ingested_at = {}", jiraFilter.getIngestedAt());
        }
        log.info("jiraFilter = {}", jiraFilter);
        log.info("workItemsFilter = {}", workItemsFilter);
        List<DbWorkItemField> workItemFields = getWorkItemFields(company, workItemsFilter);

        List<VelocityConfigDTO.Stage> stages = getAllStages(velocityConfigDTO);
        List<CompletableFuture<DbHistogramResult>> futures = new ArrayList<>();
        for (int i = 0; i < stages.size(); i++) {
            String stageName = stages.get(i).getName();
            VelocityFilter velocityFilterUpdated = velocityFilter.toBuilder().histogramStageName(stageName).build();
            futures.add(calculateVelocityStageHistogramAsync(company, velocityConfigDTO, velocityFilterUpdated,
                    workItemsType, jiraFilter, workItemsFilter, workItemsMilestoneFilter,
                    getScmPrFilter(velocityFilter.getCalculation(), filter, originalFilter),
                    getScmCommitFilter(velocityFilter.getCalculation(), filter, originalFilter),
                    getCicdJobRunsFilter(filter, originalFilter), workItemFields, i, stageName, ouConfig));
        }

        List<DbHistogramResult> stageResults = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        return DbListResponse.of(stageResults, stageResults.size());
    }

    private CompletableFuture<DbHistogramResult> calculateVelocityStageHistogramAsync(String company, VelocityConfigDTO velocityConfigDTO, VelocityFilter velocityFilter,
                                                                                      WorkItemsType workItemsType, JiraIssuesFilter jiraFilter, WorkItemsFilter workItemsFilter, WorkItemsMilestoneFilter workItemsMilestoneFilter, ScmPrFilter scmPrFilter, ScmCommitFilter scmCommitFilter, CiCdJobRunsFilter ciCdJobRunsFilter, List<DbWorkItemField> workItemFields,
                                                                                      int stageIndex, String stageName, OUConfiguration ouConfig) {
        return CompletableFuture.supplyAsync(() -> {
            VelocityFilter velocityFilterUpdated = velocityFilter.toBuilder().histogramStageName(stageName).build();
            DbHistogramResult dbHistogramResult = null;
            try {
                dbHistogramResult = velocityAggsDatabaseService.calculateVelocityStageHistogram(company, velocityConfigDTO, velocityFilterUpdated, workItemsType, jiraFilter, workItemsFilter, workItemsMilestoneFilter, scmPrFilter, scmCommitFilter, ciCdJobRunsFilter, workItemFields, ouConfig, getDisablePrJiraCorrelation(company));
                dbHistogramResult = dbHistogramResult.toBuilder().index(stageIndex).name(stageName).build();
                return dbHistogramResult;
            } catch (BadRequestException e) {
                throw new RuntimeException(e);
            }
        }, velocityTaskExecutor);
    }

    public DbListResponse<VelocityRatingResult> calculateVelocityRatings(final String company,
                                                                         DefaultListRequest originalFilter,
                                                                         DefaultListRequest filter,
                                                                         VelocityConfigDTO velocityConfigDTO,
                                                                         OUConfiguration ouConfig) throws SQLException, BadRequestException {
        WorkItemsType workItemsType = validateWorkItemsType(company, filter);
        VelocityFilter velocityFilter = parseVelocityFilter(filter);
        if (velocityFilter.getAcross() != VelocityFilter.DISTINCT.rating) {
            throw new BadRequestException("across value " + velocityFilter.getAcross() + " not supported! For this endpoint only " + VelocityFilter.DISTINCT.rating + " is supported.");
        }

        WorkItemsMilestoneFilter workItemsMilestoneFilter = parseWIMilestoneFilter(filter);
        JiraIssuesFilter jiraFilter = null;
        WorkItemsFilter workItemsFilter = null;
        if(workItemsType == WorkItemsType.WORK_ITEM)
            workItemsFilter = getWorkItemsFilter(velocityFilter.getCalculation(), filter, originalFilter);
        else {
            jiraFilter =  getJiraFilter(velocityFilter.getCalculation(), company, filter, originalFilter);
            log.info("ingested_at = {}", jiraFilter.getIngestedAt());
        }
        log.info("jiraFilter = {}", jiraFilter);
        log.info("workItemsFilter = {}", workItemsFilter);
        List<DbWorkItemField> workItemFields = getWorkItemFields(company, workItemsFilter);

        List<VelocityConfigDTO.Stage> stages = getAllStages(velocityConfigDTO);
        List<CompletableFuture<VelocityRatingResult>> futures = new ArrayList<>();
        for (int i = 0; i < stages.size(); i++) {
            String stageName = stages.get(i).getName();
            VelocityFilter velocityFilterUpdated = velocityFilter.toBuilder().histogramStageName(stageName).histogramBucketsCount(1).build();
            futures.add(calculateVelocityStageRatingAsync(company, velocityConfigDTO, velocityFilterUpdated,
                    workItemsType, jiraFilter, workItemsFilter, workItemsMilestoneFilter,
                    getScmPrFilter(velocityFilter.getCalculation(), filter, originalFilter),
                    getScmCommitFilter(velocityFilter.getCalculation(), filter, originalFilter),
                    getCicdJobRunsFilter(filter, originalFilter), workItemFields, i, stageName, ouConfig));
        }

        List<VelocityRatingResult> stageResults = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        return DbListResponse.of(stageResults, stageResults.size());
    }

    private CompletableFuture<VelocityRatingResult> calculateVelocityStageRatingAsync(String company, VelocityConfigDTO velocityConfigDTO, VelocityFilter velocityFilter,
                                                                                      WorkItemsType workItemsType, JiraIssuesFilter jiraFilter, WorkItemsFilter workItemsFilter, WorkItemsMilestoneFilter workItemsMilestoneFilter, ScmPrFilter scmPrFilter, ScmCommitFilter scmCommitFilter, CiCdJobRunsFilter ciCdJobRunsFilter, List<DbWorkItemField> workItemFields,
                                                                                      int stageIndex, String stageName, OUConfiguration ouConfig) {
        return CompletableFuture.supplyAsync(() -> {
            VelocityFilter velocityFilterUpdated = velocityFilter.toBuilder().histogramStageName(stageName).build();
            VelocityRatingResult velocityRatingResult = null;
            try {
                velocityRatingResult = velocityAggsDatabaseService.calculateVelocityStageRating(company, velocityConfigDTO, velocityFilterUpdated, workItemsType, jiraFilter, workItemsFilter, workItemsMilestoneFilter, scmPrFilter, scmCommitFilter, ciCdJobRunsFilter, workItemFields, ouConfig, getDisablePrJiraCorrelation(company));
                velocityRatingResult = velocityRatingResult.toBuilder().index(stageIndex).name(stageName).build();
                return velocityRatingResult;
            } catch (BadRequestException e) {
                throw new RuntimeException(e);
            }
        }, velocityTaskExecutor);
    }

    private JiraIssuesFilter getJiraFilter(VelocityFilter.CALCULATION calculation, String company, DefaultListRequest filter, DefaultListRequest originalFilter) throws SQLException, BadRequestException {
        boolean applyOu = calculation == VelocityFilter.CALCULATION.ticket_velocity || filter.getApplyOuOnVelocityReport();
        if(applyOu)
            return jiraFilterParser.createFilter(company, filter, null, null, null,
                    null, true, true);
        else
            return jiraFilterParser.createFilter(company, originalFilter, null, null, null,
                    null, true, true);
    }

    private WorkItemsFilter getWorkItemsFilter(VelocityFilter.CALCULATION calculation, DefaultListRequest filter, DefaultListRequest originalFilter) throws BadRequestException {
        boolean applyOu = calculation == VelocityFilter.CALCULATION.ticket_velocity || filter.getApplyOuOnVelocityReport();
        if(applyOu)
            return WorkItemsFilter.fromDefaultListRequest(filter, null, null);
        else
            return WorkItemsFilter.fromDefaultListRequest(originalFilter, null, null);
    }

    private ScmPrFilter getScmPrFilter(VelocityFilter.CALCULATION calculation, DefaultListRequest filter, DefaultListRequest originalFilter) throws BadRequestException {
        boolean applyOu = calculation == VelocityFilter.CALCULATION.pr_velocity || filter.getApplyOuOnVelocityReport();
        if(applyOu) {
            return parseSCMPrFilter(filter);
        } else {
            return parseSCMPrFilter(originalFilter);
        }
    }

    private ScmCommitFilter getScmCommitFilter(VelocityFilter.CALCULATION calculation, DefaultListRequest filter, DefaultListRequest originalFilter) throws BadRequestException {
        boolean applyOu = calculation == VelocityFilter.CALCULATION.pr_velocity || filter.getApplyOuOnVelocityReport();
        if(applyOu) {
            return parseSCMCommitFilter(filter);
        } else {
            return parseSCMCommitFilter(originalFilter);
        }
    }

    private CiCdJobRunsFilter getCicdJobRunsFilter(DefaultListRequest filter, DefaultListRequest originalFilter) throws BadRequestException {
        boolean applyOu = filter.getApplyOuOnVelocityReport();
        if(applyOu) {
            return parseCiCdJobRunsFilter(filter);
        } else {
            return parseCiCdJobRunsFilter(originalFilter);
        }
    }
}