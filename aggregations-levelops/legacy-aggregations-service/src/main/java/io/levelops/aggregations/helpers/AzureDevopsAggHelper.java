package io.levelops.aggregations.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.aggregations.services.CustomFieldService;
import io.levelops.commons.databases.converters.DbWorkItemHistoryConverters;
import io.levelops.commons.databases.issue_management.DbIssueMgmtSprintMapping;
import io.levelops.commons.databases.issue_management.DbIssueStatusMetadata;
import io.levelops.commons.databases.issue_management.DbIssuesMilestone;
import io.levelops.commons.databases.issue_management.DbProject;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.issue_management.DbWorkItemHistory;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.azuredevops.DbAzureDevopsBuild;
import io.levelops.commons.databases.models.database.azuredevops.DbAzureDevopsProject;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmTag;
import io.levelops.commons.databases.models.database.scm.converters.devops.AzureDevOpsPullRequestConverters;
import io.levelops.commons.databases.models.filters.CICDInstanceFilter;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.services.AzureDevopsProjectService;
import io.levelops.commons.databases.services.AzureDevopsReleaseService;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.IssueMgmtProjectService;
import io.levelops.commons.databases.services.IssueMgmtSprintMappingDatabaseService;
import io.levelops.commons.databases.services.IssuesMilestoneService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.WorkItemFieldsMetaService;
import io.levelops.commons.databases.services.WorkItemTimelineService;
import io.levelops.commons.databases.services.WorkItemsMetadataService;
import io.levelops.commons.databases.services.WorkItemsService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.models.controlplane.TriggerResults;
import io.levelops.integrations.azureDevops.models.ChangeSet;
import io.levelops.integrations.azureDevops.models.ClassificationNode;
import io.levelops.integrations.azureDevops.models.Commit;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.PullRequest;
import io.levelops.integrations.azureDevops.models.Repository;
import io.levelops.integrations.azureDevops.models.Team;
import io.levelops.integrations.azureDevops.models.TeamSetting;
import io.levelops.integrations.azureDevops.models.WorkItem;
import io.levelops.integrations.azureDevops.utils.JobCategory;
import io.levelops.integrations.azureDevops.utils.WorkItemUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Log4j2
@Service
public class AzureDevopsAggHelper {

    private static final String WORKITEMS_FIELDS = "workitemsfields";

    private static final String SUB_TASK_ISSUE_TYPE = "SUB-TASK";
    private static final Set<String> CLOSED_STATUS_CATEGORIES = Set.of("closed", "completed");
    private static final String ASSIGNEE_UNASSIGNED = "UNASSIGNED";

    @Value("${CICD_JOB_RUN_STAGE_LOGS_BUCKET}")
    private String bucketName;
    private Storage storage;
    private final JobDtoParser jobDtoParser;
    private final ScmAggService scmAggService;
    private final AzureDevopsProjectService azureDevopsProjectService;
    private final AzureDevopsReleaseService azureDevopsReleaseService;
    private final WorkItemsService workItemsService;
    private final IssueMgmtProjectService issueMgmtProjectService;
    private final IssuesMilestoneService issuesMilestoneService;
    private final WorkItemTimelineService workItemsTimelineService;
    private final WorkItemsMetadataService workItemsMetadataService;
    private final CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private final IssueMgmtSprintMappingDatabaseService sprintMappingDatabaseService;
    private final UserIdentityService userIdentityService;
    private final WorkItemFieldsMetaService workItemFieldsMetaService;
    private final CustomFieldService customFieldService;

    @Autowired
    public AzureDevopsAggHelper(DataSource dataSource, JobDtoParser jobDtoParser,
                                ScmAggService scmAggService, AzureDevopsProjectService azureDevopsProjectService, AzureDevopsReleaseService azureDevopsReleaseService,
                                IntegrationTrackingService trackingService, WorkItemsService workItemsService,
                                IssueMgmtProjectService issueMgmtProjectService, IssuesMilestoneService issuesMilestoneService,
                                WorkItemTimelineService workItemsTimelineService, WorkItemsMetadataService workItemsMetadataService,
                                CiCdInstancesDatabaseService ciCdInstancesDatabaseService, IssueMgmtSprintMappingDatabaseService sprintMappingDatabaseService,
                                WorkItemFieldsMetaService workItemFieldsMetaService, CustomFieldService customFieldService) {
        this.jobDtoParser = jobDtoParser;
        this.scmAggService = scmAggService;
        this.azureDevopsProjectService = azureDevopsProjectService;
        this.azureDevopsReleaseService = azureDevopsReleaseService;
        this.workItemsService = workItemsService;
        this.issueMgmtProjectService = issueMgmtProjectService;
        this.issuesMilestoneService = issuesMilestoneService;
        this.workItemsTimelineService = workItemsTimelineService;
        this.workItemsMetadataService = workItemsMetadataService;
        this.ciCdInstancesDatabaseService = ciCdInstancesDatabaseService;
        this.sprintMappingDatabaseService = sprintMappingDatabaseService;
        this.userIdentityService = new UserIdentityService(dataSource);
        this.workItemFieldsMetaService = workItemFieldsMetaService;
        this.customFieldService = customFieldService;
    }

    public void setupAzureDevopsFields(String company, String integrationId, MultipleTriggerResults results) throws SQLException {

        Map<String, DbWorkItemField> dbFieldsByKey = PaginationUtils.stream(0, 1, RuntimeStreamException.wrap(pageNumber ->
                        workItemFieldsMetaService.listByFilter(company, List.of(integrationId), null, null, null, null, null, null,
                                pageNumber, 100).getRecords()))
                .collect(Collectors.toMap(DbWorkItemField::getFieldKey, field -> field));

        // TODO stop after the first set of fields?

        Map<String, DbWorkItemField> fieldsToInsertByKey = new HashMap<>();
        jobDtoParser.applyToResults(company, WORKITEMS_FIELDS, EnrichedProjectData.class,
                results.getTriggerResults().get(0),
                enrichedProjectData -> enrichedProjectData.getWorkItemFields().stream()
                        .map(record -> DbWorkItemField.fromAzureDevopsWorkItemField(integrationId, record))
                        .filter(Objects::nonNull)
                        .filter(record -> BooleanUtils.isTrue(record.getCustom()))
                        .filter(record -> {
                            // dedupe field if it was already marked to be inserted
                            if (fieldsToInsertByKey.containsKey(record.getFieldKey())) {
                                return false;
                            }
                            //if nothing is changed with the record, no need to attempt to insert.
                            DbWorkItemField existingField = dbFieldsByKey.get(record.getFieldKey());
                            if (existingField == null) {
                                return true;
                            }
                            if (!StringUtils.equals(existingField.getName(), record.getName())) {
                                return true;
                            }
                            if (!StringUtils.equals(existingField.getFieldKey(), record.getFieldKey())) {
                                return true;
                            }
                            if (!StringUtils.equals(existingField.getFieldType(), record.getFieldType())) {
                                return true;
                            }
                            return !StringUtils.equals(existingField.getItemsType(), record.getItemsType());
                        })
                        .forEach(field -> fieldsToInsertByKey.put(field.getFieldKey(), field)),
                List.of());
        if (!fieldsToInsertByKey.isEmpty()) {
            workItemFieldsMetaService.batchUpsert(company, new ArrayList<>(fieldsToInsertByKey.values()));
        }
        try {
            customFieldService.insertPopularWorkitemFieldsToIntegrationConfig(new ArrayList<>(fieldsToInsertByKey.values()), company, integrationId);
        } catch (Exception e) {
            log.error("Unable to auto insert popular jira fields to integration mapping.", e);
        }
        log.info("Finished Ingesting Azure Devops Fields for company={}, integrationId={}", company, integrationId);
    }

    public boolean setupAzureDevopsPipelineRuns(String customer, String integrationId, String productId,
                                                MultipleTriggerResults results, Date currentTime, String dataType) throws SQLException {
        Optional<UUID> instanceId = getCiCdInstanceId(customer, integrationId);
        if (instanceId.isEmpty()) {
            log.info("No CICD instance id found, skipping pipelines agg.");
            return true;
        }
        this.azureDevopsProjectService.setStorage(storage);
        this.azureDevopsProjectService.setBucketName(bucketName);
        return jobDtoParser.applyToResults(customer, dataType, EnrichedProjectData.class,
                results.getTriggerResults().get(0),
                enrichedProjectData -> {
                    DbAzureDevopsProject dbAzureDevopsProject = DbAzureDevopsProject.fromPipelineRuns(enrichedProjectData, integrationId, productId, currentTime);
                    try {
                        azureDevopsProjectService.insert(customer, instanceId.get(), dbAzureDevopsProject);
                    } catch (SQLException e) {
                        log.warn("setupAzureDevopsPipelineRuns: error inserting project: " + dbAzureDevopsProject.getName()
                                + " for integration id: " + dbAzureDevopsProject.getIntegrationId(), e);
                    }
                    DbProject dbProject = DbProject.fromAzureDevOpsProject(integrationId, enrichedProjectData.getProject());
                    try {
                        issueMgmtProjectService.insert(customer, dbProject);
                    } catch (SQLException e) {
                        log.warn("setupAzureDevopsPipelineRuns: error inserting project: " + dbProject.getName()
                                + " for integration id: " + dbProject.getIntegrationId(), e);
                    }
                },
                List.of());
    }
    public boolean setupAzureDevopsReleases(String customer, String integrationId, String productId,
                                                MultipleTriggerResults results, Date currentTime, String dataType) throws SQLException {
        Optional<UUID> instanceId = getCiCdInstanceId(customer, integrationId);
        if (instanceId.isEmpty()) {
            log.info("No CICD instance id found, skipping releases agg.");
            return true;
        }
        this.azureDevopsReleaseService.setStorage(storage);
        this.azureDevopsReleaseService.setBucketName(bucketName);
        return jobDtoParser.applyToResults(customer, dataType, EnrichedProjectData.class,
                results.getTriggerResults().get(0),
                enrichedProjectData -> {
                    DbAzureDevopsProject dbAzureDevopsProject = DbAzureDevopsProject.fromReleases(enrichedProjectData, integrationId, productId, currentTime);
                    try {
                        azureDevopsReleaseService.insert(customer, instanceId.get(), dbAzureDevopsProject);
                    } catch (SQLException e) {
                        log.warn("setupAzureDevopsReleases: error inserting project: " + dbAzureDevopsProject.getName()
                                + " for integration id: " + dbAzureDevopsProject.getIntegrationId(), e);
                    }
                },
                List.of());
    }

    public boolean setupAzureDevopsCommits(String customer, String integrationId,
                                           MultipleTriggerResults results, Date currentTime, String dataType) {
        Long truncatedDate = DateUtils.truncate(currentTime, Calendar.DATE);
        AtomicInteger commitInserts = new AtomicInteger();
        AtomicInteger commitDuplicates = new AtomicInteger();
        boolean success = jobDtoParser.applyToResults(customer, dataType, EnrichedProjectData.class,
                results.getTriggerResults().get(0),
                enrichedProjectData -> {
                    Project project = enrichedProjectData.getProject();
                    Repository repository = enrichedProjectData.getRepository();
                    List<Commit> commits = enrichedProjectData.getCommits();
                    log.info("setupAzureDevopsCommits company {}, integrationId {}, projectId {}, repo {}, no of commits {}",
                            customer, integrationId, project.getId(), repository.getName(), commits.size());
                    for (Commit commit : commits) {
                        DbScmCommit dbScmCommit = DbScmCommit.fromAzureDevopsCommit(commit,
                                project, repository.getName(), integrationId, truncatedDate);
                        log.info("setupAzureDevopsCommits company {}, integrationId {}, projectId {}, repo {}, commit {}," +
                                        " workitem ids {}, jira keys {}", customer, integrationId, project.getId(),
                                repository.getName(), dbScmCommit.getCommitSha(), dbScmCommit.getWorkitemIds(), dbScmCommit.getIssueKeys());
                        Optional<DbScmCommit> opt = scmAggService.getCommit(customer, dbScmCommit.getCommitSha(), repository.getName(), integrationId);
                        if (opt.isEmpty()) {
                            List<DbScmFile> dbScmFiles = DbScmFile.fromAzureDevopsCommitFiles(project, commit,
                                    repository.getName(), integrationId);
                            try {
                                scmAggService.insert(customer, dbScmCommit, dbScmFiles);
                            } catch (SQLException e) {
                                log.error("Failed to insert SCM commits and files for customer={}, integrationId={}", customer, integrationId, e);
                            }
                            commitInserts.incrementAndGet();
                        } else {
                            commitDuplicates.incrementAndGet();
                        }
                    }
                },
                List.of());
        log.info("setupAzureDevopsCommits commitInserts {}, commitDuplicates {}", commitInserts.get(), commitDuplicates.get());
        return success;
    }

    public boolean setupAzureDevopsPullRequests(String customer, String integrationId,
                                                MultipleTriggerResults results, String dataType) {
        return jobDtoParser.applyToResults(customer, dataType, EnrichedProjectData.class,
                results.getTriggerResults().get(0),
                enrichedProjectData -> {
                    Project project = enrichedProjectData.getProject();
                    Repository repository = enrichedProjectData.getRepository();
                    List<PullRequest> pullRequests = enrichedProjectData.getPullRequests();
                    for (PullRequest pullRequest : pullRequests) {
                        DbScmPullRequest dbScmPullRequest = AzureDevOpsPullRequestConverters
                                .fromAzureDevopsPullRequest(pullRequest, project, repository, integrationId);
                        try {
                            Optional<DbScmPullRequest> existingPr = scmAggService.getPr(
                                    customer, String.valueOf(pullRequest.getPullRequestId()),
                                    pullRequest.getRepository().getName(), integrationId);
                            if (existingPr.isEmpty() || existingPr.get().getPrUpdatedAt() < dbScmPullRequest.getPrUpdatedAt()) {
                                scmAggService.insert(customer, dbScmPullRequest);
                            } else if (StringUtils.isEmpty(existingPr.get().getPrLink()) && dbScmPullRequest.getMetadata() != null && dbScmPullRequest.getMetadata().containsKey("pr_link")) {
                                scmAggService.updateScmMetadata(customer, dbScmPullRequest.getId(), dbScmPullRequest.getMetadata());
                            }
                        } catch (SQLException e) {
                            log.warn("setupAzureDevopsPullRequests: error inserting project: " + pullRequest.getId()
                                    + " for integration id: " + dbScmPullRequest.getIntegrationId(), e);
                        }
                    }
                },
                List.of());
    }

    public boolean setupAzureDevopsBuilds(String customer, String integrationId,
                                          MultipleTriggerResults results,
                                          Date ingestedAt, String dataType) throws SQLException {
        Optional<UUID> instanceId = getCiCdInstanceId(customer, integrationId);
        if (instanceId.isEmpty()) {
            log.info("No CICD instance id found, skipping builds agg.");
            return true;
        }
        return jobDtoParser.applyToResults(customer, dataType, EnrichedProjectData.class,
                results.getTriggerResults().get(0),
                enrichedProjectData -> {
                    enrichedProjectData.getBuilds().forEach(
                            build -> {
                                DbAzureDevopsBuild dbAzureDevopsBuild = DbAzureDevopsBuild.fromBuild(build, integrationId, ingestedAt);
                                azureDevopsProjectService.processBuilds(customer, instanceId.get(), dbAzureDevopsBuild);
                            });
                },
                List.of());
    }

    public boolean setupAzureDevopsChangesets(String customer, String integrationId,
                                              MultipleTriggerResults results, Date currentTime, String dataType) {
        Long truncatedDate = DateUtils.truncate(currentTime, Calendar.DATE);
        return jobDtoParser.applyToResults(customer, dataType, EnrichedProjectData.class,
                results.getTriggerResults().get(0),
                enrichedProjectData -> {
                    Project project = enrichedProjectData.getProject();
                    List<ChangeSet> changeSets = enrichedProjectData.getChangeSets();
                    for (ChangeSet changeSet : changeSets) {
                        List<DbScmFile> dbScmFiles = DbScmFile.fromAzureDevopsChangeSetFiles(project, changeSet, integrationId);
                        DbScmCommit dbScmCommit = DbScmCommit.fromAzureDevopsChangeSets(changeSet,
                                project, integrationId, truncatedDate);
                        try {
                            scmAggService.insert(customer, dbScmCommit, dbScmFiles);
                        } catch (SQLException e) {
                            log.error("Failed to insert SCM commits and files for customer={}, integrationId={}", customer, integrationId, e);
                        }
                    }
                },
                List.of());
    }

    public boolean setupAzureDevopsIterations(String customer,
                                              String integrationId,
                                              MultipleTriggerResults results,
                                              String dataType,
                                              Date currentTime) {
        boolean closeSprintsAfterEndDate = true;
        log.info("Will consider sprints after end date as closed for customer={}", customer);
        Map<String, Long> iterationToJobCreatedAtMap = new HashMap<>();
        return jobDtoParser.applyToResults(customer, dataType, EnrichedProjectData.class,
                results.getTriggerResults().get(0),
                (enrichedProjectData, currentJob) -> {
                    long currentJobCreatedAt = MoreObjects.firstNonNull(currentJob.getCreatedAt(), 0L);
                    Project project = enrichedProjectData.getProject();
                    enrichedProjectData.getIterations().forEach(
                            iteration -> {
                                Long previousJobCreatedAt = iterationToJobCreatedAtMap.get(iteration.getId());
                                if (previousJobCreatedAt != null && currentJobCreatedAt < previousJobCreatedAt) {
                                    // skip iterations from older jobs
                                    return;
                                }
                                iterationToJobCreatedAtMap.put(iteration.getId(), currentJobCreatedAt);
                                DbIssuesMilestone dbIssuesMilestone = DbIssuesMilestone
                                        .fromAzureDevOpsIteration(integrationId, project, iteration, currentTime.toInstant(), closeSprintsAfterEndDate);
                                try {
                                    issuesMilestoneService.insert(customer, dbIssuesMilestone);
                                } catch (SQLException e) {
                                    log.warn("setupAzureDevopsIterations: error inserting iteration: " + iteration.getId()
                                            + " for project id: " + project.getId(), e);
                                }
                            });
                },
                List.of());
    }

    private List<DbIssueStatusMetadata> getIssueStatusMetadataFromDb(final String company, final String integrationId, final String projectId) {
        List<DbIssueStatusMetadata> issueStatusMetadatas = new ArrayList<>();
        boolean keepFetching = true;
        int pageNumber = 0;
        while (keepFetching) {
            try {
                DbListResponse<DbIssueStatusMetadata> dbListResponse = workItemsMetadataService.listByFilter(company, pageNumber, 100, null, List.of(Integer.parseInt(integrationId)), List.of(projectId), null);
                if (CollectionUtils.isNotEmpty(dbListResponse.getRecords())) {
                    issueStatusMetadatas.addAll(dbListResponse.getRecords());
                }
                keepFetching = (issueStatusMetadatas.size() < dbListResponse.getTotalCount());
                pageNumber++;
            } catch (SQLException e) {
                log.error("Error fetching issue status metadatas from db!! company {}, integrationId {}, projectId {}", company, integrationId, projectId, e);
                keepFetching = false;
            }
        }
        return issueStatusMetadatas;
    }

    private Map<String, String> getStatusToStatusCategoryMapping(final String company, final String integrationId, final String projectId) {
        List<DbIssueStatusMetadata> issueStatusMetadatas = getIssueStatusMetadataFromDb(company, integrationId, projectId);
        log.info("company {}, integrationId {}, projectId {}, issueStatusMetadatas.size() {}", company, integrationId, projectId, issueStatusMetadatas.size());
        Map<String, String> statusToStatusCategoryMapping = CollectionUtils.emptyIfNull(issueStatusMetadatas).stream()
                .filter(x -> StringUtils.isNotEmpty(x.getStatusId()) && StringUtils.isNotEmpty(x.getStatusCategory()))
                .collect(Collectors.toMap(DbIssueStatusMetadata::getStatusId, DbIssueStatusMetadata::getStatusCategory));
        log.info("company {}, integrationId {}, projectId {}, statusToStatusCategoryMapping.size() {}", company, integrationId, projectId, statusToStatusCategoryMapping.size());
        return statusToStatusCategoryMapping;
    }

    public boolean setupAzureDevopsWorkItems(String customer, String integrationId, Date currentTime,
                                             MultipleTriggerResults results, String dataType, List<IntegrationConfig.ConfigEntry> config,
                                             List<DbWorkItemField> dbWorkItemFields, @Nullable String storyPointsField) {
        LoadingCache<String, Optional<DbIssuesMilestone>> dbSprintLoadingCache = CacheBuilder.from("maximumSize=250")
                .build(CacheLoader.from(sprintId -> findDbSprintById(customer, integrationId, sprintId)));
        LoadingCache<String, Optional<String>> statusToStatusCategoryCache = CacheBuilder.from("maximumSize=250")
                .build(CacheLoader.from(status -> findStatusCategoryByStatus(customer, integrationId, status)));
        return jobDtoParser.applyToResults(customer, dataType, EnrichedProjectData.class,
                results.getTriggerResults().get(0),
                enrichedProjectData -> {
                    Project project = enrichedProjectData.getProject();
                    log.info("Starting processing workitems for company {}, integrationId {}, projectId {}, projectName {}", customer, integrationId, project.getId(), project.getName());
                    Map<String, String> statusToStatusCategoryMapping = getStatusToStatusCategoryMapping(customer, integrationId, project.getId());
                    enrichedProjectData.getWorkItems().forEach(
                            workItem -> {
                                DbWorkItem dbWorkItem = DbWorkItem.fromAzureDevOpsWorkItem(integrationId, project,
                                        currentTime, workItem, config, dbWorkItemFields, statusToStatusCategoryMapping, storyPointsField);
                                try {
                                    dbWorkItem = upsertAndPopulateDbWorkItemIds(customer, dbWorkItem);
                                } catch (SQLException e) {
                                    log.warn("setupAzureDevopsWorkItems: error populating assignee and reporter Ids", e);
                                }
                                log.info("setupAzureDevopsWorkItems company {}, integrationId {}, projectId {}, projectName {}, status {}, statusCategory {}", customer, integrationId, project.getId(), project.getName(), dbWorkItem.getStatus(), dbWorkItem.getStatusCategory());
                                try {
                                    // -- update all the events that have startDate = 0 to createdAt
                                    int updatedRows = 0;
                                    try {
                                        updatedRows = workItemsTimelineService.updateZeroStartDatesForWorkItem(customer, integrationId, dbWorkItem.getWorkItemId(), dbWorkItem.getWorkItemCreatedAt());
                                    } catch (SQLException e) {
                                        log.warn("setupAzureDevopsWorkItems: error updating zero start dates for customer=" + customer + ", workItemId=" + dbWorkItem.getId() + ", createdAt=," + dbWorkItem.getWorkItemCreatedAt() + ",updatedRows=" + updatedRows, e);
                                    }

                                    // -- create fake events for each missing type
                                    insertMissingFakeEvents(customer, integrationId, dbWorkItem.getWorkItemId(), dbWorkItem.getWorkItemCreatedAt(), workItem);

                                    // -- dedupe and insert work item
                                    DbWorkItem oldWorkItem = workItemsService.get(customer, integrationId, dbWorkItem.getWorkItemId(), dbWorkItem.getIngestedAt());
                                    if (oldWorkItem == null || (oldWorkItem.getWorkItemUpdatedAt() != null &&
                                            dbWorkItem.getWorkItemUpdatedAt() != null &&
                                            oldWorkItem.getWorkItemUpdatedAt().before(dbWorkItem.getWorkItemUpdatedAt()))) {
                                        dbWorkItem = computeHopsAndBounces(customer, integrationId, dbWorkItem);
                                        workItemsService.insert(customer, dbWorkItem);
                                    }

                                    // -- handle sprint mappings
                                    if (workItem.getFields() == null || StringUtils.isEmpty(workItem.getFields().getIterationPath())) {
                                        log.info("Skipping sprint mapping for workitem with no iterationPath for customer={}, integrationId={}, wiId={}", customer, integrationId, workItem.getId());
                                    } else {
                                        generateIssueSprintMappingsFromEvents(customer, integrationId, dbWorkItem, dbSprintLoadingCache, statusToStatusCategoryCache, currentTime);
                                    }
                                } catch (Exception e) {
                                    log.warn("setupAzureDevopsWorkItems: error inserting project: " + workItem.getId()
                                            + " for project id: " + project.getId(), e);
                                }
                            });
                },
                List.of());
    }

    private DbWorkItem upsertAndPopulateDbWorkItemIds(String company, DbWorkItem dbWorkItem) throws SQLException {
        String reporterId = null;
        if (StringUtils.isNotEmpty(dbWorkItem.getReporter())) {
            reporterId = userIdentityService.upsert(company, dbWorkItem.getReporterInfo());
        }
        String assigneeId = null;
        if (StringUtils.isNotEmpty(dbWorkItem.getAssignee())) {
            if (!dbWorkItem.getAssignee().equals("_UNASSIGNED_")) {
                assigneeId = userIdentityService.upsert(company, dbWorkItem.getAssigneeInfo());
            }
        }
        return dbWorkItem.toBuilder()
                .reporterId(reporterId)
                .assigneeId(assigneeId)
                .build();
    }

    private void insertMissingFakeEvents(String customer, String integrationId, String dbWorkItemId, Timestamp dbWorkItemCreatedAt, WorkItem workItem) {
        Timestamp endDate = DateUtils.toTimestamp(new Date().toInstant());
        try {
            Set<String> fieldTypesPresent = workItemsTimelineService.getFirstEventOfEachType(customer, Integer.valueOf(integrationId), dbWorkItemId).stream()
                    .map(DbWorkItemHistory::getFieldType)
                    .filter(StringUtils::isNotEmpty)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            if (!fieldTypesPresent.contains("sprint") && workItem.getFields().getIterationPath() != null) {
                workItemsTimelineService.insertFakeEvent(customer, createFakeEvent(Integer.valueOf(integrationId),
                        dbWorkItemId, "sprint", workItem.getFields().getIterationPath(),
                        dbWorkItemCreatedAt, endDate));
            }
            if (!fieldTypesPresent.contains("status") && workItem.getFields().getState() != null) {
                workItemsTimelineService.insertFakeEvent(customer, createFakeEvent(Integer.valueOf(integrationId),
                        dbWorkItemId, "status", workItem.getFields().getState(),
                        dbWorkItemCreatedAt, endDate));
            }
            if (!fieldTypesPresent.contains("story_points") && workItem.getFields().getStoryPoints() != null) {
                workItemsTimelineService.insertFakeEvent(customer, createFakeEvent(Integer.valueOf(integrationId),
                        dbWorkItemId, "story_points", String.valueOf(workItem.getFields().getStoryPoints()),
                        dbWorkItemCreatedAt, endDate));
            }
            if (!fieldTypesPresent.contains("assignee") && workItem.getFields().getAssignedTo() != null && workItem.getFields().getAssignedTo().getUniqueName() != null) {
                workItemsTimelineService.insertFakeEvent(customer, createFakeEvent(Integer.valueOf(integrationId),
                        dbWorkItemId, "assignee", workItem.getFields().getAssignedTo().getUniqueName(),
                        dbWorkItemCreatedAt, endDate));
            }
        } catch (SQLException e) {
            log.warn("insertMissingFakeEvents: error inserting fake events for customer={}, workItemId={}, createdAt={}", customer, dbWorkItemId, dbWorkItemCreatedAt, e);
        }
    }

    public void updateTimelineZeroStartDates(String company, String integrationId, Long ingestedAt) {
        try {
            workItemsTimelineService.updateAllZeroStartDates(company, integrationId, ingestedAt);
        } catch (Exception e) {
            log.warn("updateTimelineZeroStartDates: error updating all zero start dates for customer={}, integrationId={}, ingestedAt={}", company, integrationId, ingestedAt, e);
        }
    }

    DbWorkItem computeHopsAndBounces(String company, String integrationId, DbWorkItem workItem) throws SQLException {
        List<DbWorkItemHistory> assignee = workItemsTimelineService.getEvents(
                company, Integer.valueOf(integrationId), "assignee", workItem.getWorkItemId());
        return computeHopsAndBounces(workItem, assignee);
    }

    DbWorkItem computeHopsAndBounces(DbWorkItem workItem, List<DbWorkItemHistory> assignee) {
        List<String> hops = assignee.stream()
                .map(DbWorkItemHistory::getFieldValue)
                .filter(val -> !val.equals(ASSIGNEE_UNASSIGNED))
                .collect(Collectors.toList());
        Set<String> distinctAssignees = new HashSet<>(hops);
        return workItem.toBuilder()
                .hops(Math.max(hops.size() - 1, 0))
                .bounces(Math.max(hops.size() - distinctAssignees.size(), 0))
                .build();
    }

    private Optional<DbIssuesMilestone> findDbSprintById(String customer, String integrationId, String iterationPath) {
        if (StringUtils.isBlank(iterationPath)) {
            return Optional.empty();
        }
        Pair<String, String> pathAndName = parseIterationPath(iterationPath);
        String parentKey = pathAndName.getLeft();
        String name = pathAndName.getRight();
        if (StringUtils.isEmpty(parentKey) || StringUtils.isEmpty(name)) {
            return Optional.empty();
        }
        try {
            return issuesMilestoneService.getMilestoneByParentKeyAndName(customer, integrationId, "sprint", parentKey, name);
        } catch (Exception e) {
            log.warn("Failed to lookup dbSprint by id", e);
            return Optional.empty();
        }
    }

    private Optional<String> findStatusCategoryByStatus(String customer, String integrationId, String status) {
        if (StringUtils.isBlank(status)) {
            return Optional.empty();
        }
        try {
            return workItemsMetadataService.getByStatus(customer, integrationId, status)
                    .map(DbIssueStatusMetadata::getStatusCategory);
        } catch (Exception e) {
            log.warn("Failed to lookup status category by status id", e);
            return Optional.empty();
        }
    }

    protected static Pair<String, String> parseIterationPath(String iterationPath) {
        if (!iterationPath.contains("\\")) {
            return Pair.of(null, iterationPath);
        }
        int lastBackslash = iterationPath.lastIndexOf("\\");
        String parentKey = iterationPath.substring(0, lastBackslash);
        String name = iterationPath.substring(lastBackslash + 1);
        return Pair.of(parentKey, name);
    }

    protected void generateIssueSprintMappingsFromEvents(String customer, String integrationId, DbWorkItem dbWorkItem,
                                                         LoadingCache<String, Optional<DbIssuesMilestone>> dbSprintCache,
                                                         LoadingCache<String, Optional<String>> statusToStatusCategoryCache, Date currentTime) throws SQLException, ExecutionException {
        String workItemId = dbWorkItem.getWorkItemId();
        boolean ignorableTaskType = SUB_TASK_ISSUE_TYPE.equalsIgnoreCase(dbWorkItem.getWorkItemType());
        Long resolvedAt = DateUtils.toEpochSecond(dbWorkItem.getWorkItemResolvedAt());

        List<DbWorkItemHistory> sprintEvents = workItemsTimelineService.getEvents(customer, Integer.valueOf(integrationId), "sprint", workItemId);
        for (DbWorkItemHistory sprintEvent : DbWorkItemHistoryConverters.sanitizeEventList(sprintEvents, Instant.now())) {
            String iterationPath = sprintEvent.getFieldValue();
            log.info("Processing sprint event for customer=" + customer + ", integrationId=" + integrationId + ", workitemId=" + workItemId + ", sprint=" + sprintEvent);
            if (StringUtils.isEmpty(iterationPath)) {
                log.info("Ignoring sprint with blank id for customer=" + customer + ", integrationId=" + integrationId + ",workitemId=" + workItemId + ", sprint=" + sprintEvent);
                continue;
            }
            if (sprintEvent.getStartDate() == null || sprintEvent.getEndDate() == null) {
                log.info("Ignoring sprint event with no details for customer " + customer + ", integrationId=" + integrationId + ", workItemId=" + workItemId + ", sprint=" + sprintEvent);
                continue;
            }
            DbIssuesMilestone sprintDetails = dbSprintCache.get(iterationPath).orElse(null);
            if (sprintDetails == null) {
                log.info("Ignoring sprint event: no corresponding sprint for customer " + customer + ", integrationId=" + integrationId + ", workItemId=" + workItemId + ", sprint=" + sprintEvent);
                continue;
            }
            // -- find out sprint dates
            Long startDate = DateUtils.toEpochSecond(sprintDetails.getStartDate());
            Long endDate = DateUtils.toEpochSecond(sprintDetails.getEndDate());
            Long completedDate = DateUtils.toEpochSecond(sprintDetails.getCompletedAt());
            completedDate = (completedDate != null) ? completedDate : endDate;
            Long addedAt = DateUtils.toEpochSecond(sprintEvent.getStartDate());
            Long removedAt = DateUtils.toEpochSecond(sprintEvent.getEndDate());

            if (DateUtils.toEndOfDay(currentTime.toInstant()).getEpochSecond() == removedAt) {
                removedAt = null;
            }

            // > was it planned? (added_at < sprint.start_time)
            boolean planned = false;
            boolean outsideOfSprint = false; // means *completed* outside of sprint
            if (addedAt != null && startDate != null) {
                if (resolvedAt != null) {
                    outsideOfSprint = resolvedAt < startDate;
                    planned = !outsideOfSprint;
                } else {
                    planned = addedAt < startDate;
                }
            }

            // > story points at the start and the end of the sprint
            float storyPointsPlanned = 0;
            float storyPointsDelivered = 0;
            if (startDate != null && completedDate != null) {
                List<DbWorkItemHistory> storyPointsLogs = workItemsTimelineService.getEvents(customer, Integer.valueOf(integrationId), "story_points", workItemId);
                for (DbWorkItemHistory storyPointsLog : storyPointsLogs) {
                    final String storyPointValue = storyPointsLog.getFieldValue();
                    float storyPoints = "null".equals(storyPointValue) ? 0 : Float.parseFloat(MoreObjects.firstNonNull(storyPointValue, "0"));
                    Long storyPointsLogStartTime = Optional.ofNullable(storyPointsLog.getStartDate())
                            .map(Timestamp::toInstant)
                            .map(Instant::getEpochSecond)
                            .orElse(null);
                    Long storyPointsLogEndTime = Optional.ofNullable(storyPointsLog.getEndDate())
                            .map(Timestamp::toInstant)
                            .map(Instant::getEpochSecond)
                            .orElse(Instant.now().getEpochSecond());
                    if (storyPointsLogStartTime == null) {
                        log.info("Ignoring sprint for sprint log with null start time for customer=" + customer + ", integrationId=" + integrationId + ",workitem=" + workItemId + ",sprint=" + sprintEvent);
                        continue;
                    }
                    if (planned) {
                        if (startDate >= storyPointsLogStartTime && startDate < storyPointsLogEndTime) {
                            storyPointsPlanned = storyPoints;
                        }
                    } else {
                        if (addedAt != null && addedAt >= storyPointsLogStartTime && addedAt < storyPointsLogEndTime) {
                            storyPointsPlanned = storyPoints;
                        }
                    }
                    if (completedDate >= storyPointsLogStartTime && completedDate < storyPointsLogEndTime) {
                        storyPointsDelivered = storyPoints;
                    }
                }
            }

            // > was it delivered? (at sprint.end_time, issue was completed using status category)
            boolean delivered = false;
            if (completedDate != null) {
                List<DbWorkItemHistory> statusLogs = workItemsTimelineService.getEvents(customer, Integer.valueOf(integrationId), "status", workItemId);
                for (DbWorkItemHistory status : statusLogs) {
                    Long statusStartTime = DateUtils.toEpochSecond(status.getStartDate());
                    long statusEndTime = MoreObjects.firstNonNull(DateUtils.toEpochSecond(status.getEndDate()), Instant.now().getEpochSecond());
                    if (statusStartTime == null) {
                        continue;
                    }
                    // find status category
                    String statusCategory = null;
                    if (StringUtils.isNotBlank(status.getFieldValue())) {
                        try {
                            statusCategory = statusToStatusCategoryCache.get(status.getFieldValue()).orElse(null);
                        } catch (ExecutionException e) {
                            log.warn("Failed to lookup status category from cache", e);
                        }
                    } else {
                        log.warn("customer {}, key {}: status was empty", customer, dbWorkItem.getWorkItemId());
                    }
                    log.info("sprint_mapping/status: customer={}, integrationId={}, wiId={}, status={}, status_cat={}, closed={} - status_start={}, status_end={}, completed={}, delivered={}, oos={}", customer, integrationId, workItemId, status.getFieldValue(), statusCategory, CLOSED_STATUS_CATEGORIES.contains(StringUtils.lowerCase(statusCategory)), statusStartTime, statusEndTime, completedDate, (completedDate >= statusStartTime && completedDate < statusEndTime), ((completedDate >= statusStartTime && completedDate < statusEndTime) && startDate != null && statusStartTime < startDate));
                    // ignore incomplete statuses
                    if (!CLOSED_STATUS_CATEGORIES.contains(StringUtils.lowerCase(statusCategory))) {
                        continue;
                    }
                    // if the sprint was completed within the bounds of a completed status,
                    // consider the issue delivered during this sprint
                    if (completedDate >= statusStartTime && completedDate < statusEndTime) {
                        delivered = true;
                        if (startDate != null && statusStartTime < startDate) {
                            outsideOfSprint = true;
                        }
                        break;
                    }
                }
            }

            log.info("sprint_mapping: customer={}, integrationId={}, wiId={}, sprintId={}, addedAt={}, planned={}, oos={}, delivered={}, ignorable={}, spPlanned={}, spDelivered={}",
                    customer, integrationId, workItemId, iterationPath, addedAt, planned, outsideOfSprint, delivered, ignorableTaskType, storyPointsPlanned, storyPointsDelivered);

            DbIssueMgmtSprintMapping sprintMapping = DbIssueMgmtSprintMapping.builder()
                    .integrationId(integrationId)
                    .workitemId(workItemId)
                    .sprintId(iterationPath)
                    .addedAt(addedAt)
                    .removedAt(removedAt)
                    .planned(planned)
                    .outsideOfSprint(outsideOfSprint)
                    .delivered(delivered)
                    .ignorableWorkitemType(ignorableTaskType)
                    .storyPointsPlanned(storyPointsPlanned)
                    .storyPointsDelivered(storyPointsDelivered)
                    .build();
            sprintMappingDatabaseService.upsert(customer, sprintMapping);
        }
    }

    public boolean setupAzureDevopsWorkItemsTimelines(String customer, Integer integrationId,
                                                      MultipleTriggerResults results, String dataType, Date currentTime) {
        return jobDtoParser.applyToResults(customer, dataType, EnrichedProjectData.class,
                results.getTriggerResults().get(0),
                enrichedProjectData -> {
                    Project project = enrichedProjectData.getProject();
                    enrichedProjectData.getWorkItemHistories().stream()
                            .sorted(Comparator.comparing(WorkItemUtils::getChangedDateFromHistory))
                            .forEach(workItemHistory -> {
                                List<DbWorkItemHistory> dbWorkItemHistories = DbWorkItemHistory
                                        .fromAzureDevopsWorkItemHistories(String.valueOf(integrationId), workItemHistory, currentTime);
                                dbWorkItemHistories.forEach(dbWorkItemHistory -> {
                                    String fieldType = dbWorkItemHistory.getFieldType();
                                    String workItemId = dbWorkItemHistory.getWorkItemId();
                                    Timestamp currentEventStartDate = dbWorkItemHistory.getStartDate();
                                    Timestamp currentEventEndDate = dbWorkItemHistory.getEndDate();
                                    if (currentEventStartDate == null) {
                                        log.warn("setupAzureDevopsWorkItems: skipping '{}' event without a startDate for customer={}, project={}, workItem={}, historyId={}",
                                                fieldType, customer, project.getId(), workItemId, workItemHistory.getId());
                                        return;
                                    }
                                    try {
                                        // -- handle "fake event" (i.e previous value of oldest event)
                                        if (dbWorkItemHistory.getOldValue() != null) {
                                            DbWorkItemHistory fakeEvent = createFakeEvent(integrationId, workItemId, fieldType,
                                                    dbWorkItemHistory.getOldValue(),
                                                    Timestamp.from(Instant.ofEpochSecond(0)),
                                                    currentEventEndDate);
                                            Optional<DbWorkItemHistory> optFirstEvent = workItemsTimelineService.getFirstEvent(customer, integrationId, fieldType, workItemId);
                                            if (optFirstEvent.isEmpty()) {
                                                // if there are no other event of this type in the db: insert fake event
                                                workItemsTimelineService.insertFakeEvent(customer, fakeEvent);
                                            } else {
                                                // check if the oldest event is more recent than this fake event, if yes: replace it
                                                DbWorkItemHistory firstEvent = optFirstEvent.get();
                                                if (firstEvent.getEndDate() != null && firstEvent.getEndDate().compareTo(fakeEvent.getEndDate()) > 0) {
                                                    workItemsTimelineService.deleteEvent(customer, firstEvent.getId().toString());
                                                    workItemsTimelineService.insertFakeEvent(customer, fakeEvent);
                                                }
                                            }
                                        }

                                        // -- handle regular event
                                        Optional<DbWorkItemHistory> previousEvent = workItemsTimelineService.getLastEventBefore(customer, integrationId, fieldType, workItemId, currentEventStartDate);
                                        Optional<DbWorkItemHistory> nextEvent = workItemsTimelineService.getFirstEventAfter(customer, integrationId, fieldType, workItemId, currentEventStartDate);

                                        if (previousEvent.isPresent() && !currentEventStartDate.equals(previousEvent.get().getEndDate())) {
                                            // -- update last event's end date with current event's start date
                                            DbWorkItemHistory lastEventUpdatedHistory = previousEvent.get().toBuilder()
                                                    .endDate(currentEventStartDate)
                                                    .build();
                                            workItemsTimelineService.updateEndDate(customer, lastEventUpdatedHistory);
                                        }
                                        if (nextEvent.isPresent()) {
                                            // -- update current event's end date with next event's start date
                                            dbWorkItemHistory = dbWorkItemHistory.toBuilder()
                                                    .endDate(nextEvent.get().getStartDate())
                                                    .build();
                                        }
                                        // insert current event (potentially updating end date if it already exists)
                                        if (dbWorkItemHistory.getFieldValue() != null) {
                                            workItemsTimelineService.upsert(customer, dbWorkItemHistory);
                                        }
                                    } catch (Exception ex) {
                                        log.warn("setupAzureDevopsWorkItems: error inserting '{}' event for customer={}, project={}, workItem={}, historyId={}",
                                                fieldType, customer, project.getId(), workItemId, workItemHistory.getId(), ex);
                                    }
                                });
                            });
                },
                List.of());
    }

    private DbWorkItemHistory createFakeEvent(Integer integrationId, String workitemId, String fieldType, String fieldValue, Timestamp startDate, Timestamp endDate) {
        return DbWorkItemHistory.builder()
                .workItemId(workitemId)
                .integrationId(String.valueOf(integrationId))
                .fieldType(fieldType)
                .fieldValue(fieldValue)
                .startDate(startDate)
                .endDate(endDate).build();
    }

    public boolean setupAzureDevopsWorkItemsMetadata(String customer, String integrationId,
                                                     MultipleTriggerResults results, String dataType) {
        TriggerResults triggerWithLatestJob = getTriggerWithLatestJob(results, JobCategory.BOARDS_1.name());
        return jobDtoParser.applyToResults(customer, dataType, EnrichedProjectData.class,
                triggerWithLatestJob,
                (enrichedProjectData, latestJob) -> {
                    Project project = enrichedProjectData.getProject();
                    log.info("Ingesting workitem metada for the latest job created, job id :  {}, job created at : {}", latestJob.getId(), latestJob.getCreatedAt());
                    List<DbIssueStatusMetadata> dbIssueStatusMetadata = DbIssueStatusMetadata.fromAzureDevopsWorkItemMetadata(integrationId, project, enrichedProjectData.getMetadata());
                    dbIssueStatusMetadata.forEach(issueStatusMetadata -> {
                        try {
                            workItemsMetadataService.insert(customer, issueStatusMetadata);
                        } catch (SQLException e) {
                            log.warn("setupAzureDevopsWorkItemsMetadata: error inserting project: " + issueStatusMetadata.getProjectId()
                                    + " for project id: " + project.getId(), e);
                        }
                    });
                },
                List.of());
    }

    public boolean setupAzureDevopsTags(String customer, String integrationId,
                                        MultipleTriggerResults results, String dataType) {
        return jobDtoParser.applyToResults(customer, dataType, EnrichedProjectData.class,
                results.getTriggerResults().get(0),
                enrichedProjectData -> enrichedProjectData.getTags().stream()
                        //filtering commit tags out of Commit, Tree, Blob, Tag
                        .filter(tag -> "commit".equals(tag.getTaggedObject().getObjectType()))
                        .forEach(tag -> {
                            DbScmTag gitTag = DbScmTag.fromAzureDevopsTag(tag, enrichedProjectData.getProject().getId(), integrationId);
                            try {
                                scmAggService.insertTag(customer, gitTag);
                            } catch (SQLException e) {
                                log.error("Failed to insert the tag for customer:{}, integrationId:{}, repo:{}, tag:{}", customer, integrationId, enrichedProjectData.getProject().getId(), tag.getName());
                            }
                        }),
                List.of());
    }

    public boolean setupAzureDevopsTeams(String customer, String integrationId,
                                         MultipleTriggerResults results, String dataType) {
        TriggerResults triggerWithLatestJob = getTriggerWithLatestJob(results, JobCategory.BOARDS_1.name());
        Map<String, List<String>> areaAcrossTeams = new HashMap<>();
        jobDtoParser.applyToResults(customer, dataType, EnrichedProjectData.class,
                triggerWithLatestJob,
                (enrichedProjectData) -> {
                    Project project = enrichedProjectData.getProject();
                    enrichedProjectData.getTeams().stream()
                            .map(team -> CollectionUtils.emptyIfNull(updateTeamSettingValues(team, enrichedProjectData.getCodeAreas()).getValues())
                                    .stream()
                                    .map(TeamSetting.FieldValue::getValue)
                                    .map(area -> {
                                        String areaProject = String.join("::", area,
                                                project.getOrganization() + "/" + project.getName());
                                        String teamName = "\"" + String.join("/",
                                                project.getOrganization(), project.getName(), team.getName()) + "\"";
                                        log.info("setupAzureDevopsTeams company {}," +
                                                        " integrationId {}, areaProject {}, teamName {}", customer
                                                , integrationId, areaProject, teamName);
                                        return areaAcrossTeams.computeIfAbsent(areaProject, k -> new ArrayList<>())
                                                .add(teamName);
                                    }).collect(Collectors.toList()))
                            .collect(Collectors.toList());
                },
                List.of());
        log.info("setupAzureDevopsTeams areaAcrossTeams.size() = {}", areaAcrossTeams.size());
        String setClause = " SET attributes = jsonb_set(attributes,'{\"teams\"}', :teams::jsonb) ";
        areaAcrossTeams.entrySet().stream()
                .map(es -> {
                    String projectName = es.getKey().split("::")[1];
                    String area = es.getKey().split("::")[0];
                    Map<String, Object> params = new HashMap<>();
                    Set<String> teams = new HashSet<>(es.getValue());
                    params.put("teams", "[" + String.join(",", teams) + "]");
                    List<String> conditions = getWhereClause(integrationId, projectName, area, params);
                    log.info("setupAzureDevopsTeams parsed company {}, integrationId {}, area {}, projectName {}, params {}, conditions {}", customer, integrationId, area, projectName, params, conditions);
                    boolean updateResult = workItemsService.updateWorkItems(customer, setClause, conditions, params);
                    log.info("setupAzureDevopsTeams parsed company {}, integrationId {}, updateResult {}", customer, integrationId, updateResult);
                    return updateResult;
                }).collect(Collectors.toList());
        return true;
    }

    /*
        For metadata, we will use the latest job and process them.
        In Azure Devops, we treat team settings as metadata. Note the following scenario:

        Job - J1 (backward scan for Jun-Jul) ingested data D1 and teams metadata (team1=code-area-1,code-area-2)
        Job - J2 (forward scan for last 1 hr) ingested data D2 and teams metadata (team1=code-area-1,code-area-2)
        User updated the metadata team1=code-area-1 and moved code-area-2 to team2
        Job - J3 (backward scan for Jul-Aug) ingested data D3
        Job - J4 (forward scan for last 1 hr) ingested data D3 and teams metadata (team1=code-area-1 and team2=code-area-2)

        So even while processing data D3 (old data) the new metadata will be applied to it).
    */
    private TriggerResults getTriggerWithLatestJob(MultipleTriggerResults results, String jobCategory) {
        ObjectMapper oMapper = DefaultObjectMapper.get();
        TriggerResults triggerResults = results.getTriggerResults().get(0);
        Optional<JobDTO> latestJob = triggerResults.getJobs().stream()
                .filter(Objects::nonNull)
                .filter(jobDTO -> {
                    if (jobDTO.getTags().contains("backward")) {
                        Map<String, Object> query = oMapper.convertValue(jobDTO.getQuery(), Map.class);
                        String queryJobCategory = (String) query.get("job_category");
                        return jobCategory.equals(queryJobCategory);
                    }
                    return true;
                })
                .max(Comparator.comparing(JobDTO::getCreatedAt));
        return triggerResults.toBuilder()
                .jobs(latestJob.map(List::of).orElseGet(List::of))
                .build();
    }

    private List<String> getWhereClause(String integrationId, String project, String area, Map<String, Object> params) {
        List<String> conditions = new ArrayList<>();
        conditions.add("(attributes ->> 'code_area') = :area ");
        params.put("area", area);
        conditions.add("integration_id = :integration_id");
        params.put("integration_id", Integer.parseInt(integrationId));
        conditions.add("project = :project");
        params.put("project", project);
        return conditions;
    }

    private Optional<UUID> getCiCdInstanceId(String company, String integrationId) throws SQLException {
        DbListResponse<CICDInstance> dbListResponse = ciCdInstancesDatabaseService.list(company,
                CICDInstanceFilter.builder()
                        .integrationIds(List.of(integrationId))
                        .types(List.of(CICD_TYPE.azure_devops))
                        .build(), null, null, null);
        return IterableUtils.getFirst(dbListResponse.getRecords()).map(CICDInstance::getId);
    }

    private TeamSetting updateTeamSettingValues(Team team, ClassificationNode codeAreas) {
        List<String> areaPaths = new ArrayList<>();
        if (codeAreas.getHasChildren()) {
            areaPaths.addAll(getSubAreas(List.of(codeAreas)));
        }
        TeamSetting teamSetting = team.getTeamSetting();
        List<String> parentAreas = teamSetting.getValues().stream()
                .filter(TeamSetting.FieldValue::getIncludeChildren)
                .map(TeamSetting.FieldValue::getValue)
                .collect(Collectors.toList());
        List<TeamSetting.FieldValue> subAreasForTeam = getSubAreasForTeam(parentAreas, areaPaths);
        teamSetting.getValues().addAll(subAreasForTeam);
        return teamSetting;
    }

    private String sanitizeAreaPath(String s) {
        List<String> split = Arrays.asList(s.replaceFirst("\\\\", "").split("\\\\"));
        if (split.size() > 2) {
            return String.join("\\", split.get(0),
                    String.join("\\", split.subList(2, split.size())));
        } else {
            return split.get(0);
        }
    }

    private List<String> getSubAreas(List<ClassificationNode> children) {
        List<String> areaPaths = new ArrayList<>();
        children.forEach(child -> {
            areaPaths.add(sanitizeAreaPath(child.getPath()));
            if (child.getHasChildren()) {
                areaPaths.addAll(getSubAreas(child.getChildren()));
            }
        });
        return areaPaths;
    }

    private List<TeamSetting.FieldValue> getSubAreasForTeam(List<String> parentAreas, List<String> projectAreas) {
        return parentAreas.stream()
                .flatMap(parentArea -> projectAreas.stream()
                        .filter(subArea -> subArea.contains(parentArea))
                        .map(subArea -> TeamSetting.FieldValue.builder()
                                .value(subArea)
                                .build())
                        .collect(Collectors.toList())
                        .stream())
                .collect(Collectors.toList());
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }
}