package io.levelops.aggregations.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.databases.issue_management.DbIssueMgmtSprintMapping;
import io.levelops.commons.databases.issue_management.DbIssuesMilestone;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.issue_management.DbWorkItemHistory;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IssueMgmtSprintMappingDatabaseService;
import io.levelops.commons.databases.services.IssuesMilestoneService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.ScmIssueMgmtService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.WorkItemFieldsMetaService;
import io.levelops.commons.databases.services.WorkItemTimelineService;
import io.levelops.commons.databases.services.WorkItemsPrioritySLAService;
import io.levelops.commons.databases.services.WorkItemsService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.WorkItem;
import io.levelops.integrations.azureDevops.utils.WorkItemUtils;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Log4j2
public class AzureDevopsStoryPointsTest {
    private static final String company = "test";
    private static final String SUB_TASK_ISSUE_TYPE = "SUB-TASK";
    private static final String CLOSED_STATUS_CATEGORY = "Closed";
    private static final String ASSIGNEE_UNASSIGNED = "UNASSIGNED";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ScmAggService scmAggService;
    private static UserIdentityService userIdentityService;
    private static WorkItemsService workItemsService;
    private static WorkItemTimelineService workItemTimelineService;
    private static IssuesMilestoneService issuesMilestoneService;
    private static ScmIssueMgmtService scmIssueMgmtService;
    private static GitRepositoryService repositoryService;
    private static IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService;
    private static WorkItemsPrioritySLAService workItemsPrioritySLAService;
    private static String azureDevopsIntegrationId;
    private static Date currentTime;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        IntegrationService integrationService = new IntegrationService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        WorkItemFieldsMetaService workItemFieldsMetaService = new WorkItemFieldsMetaService(dataSource);
        scmIssueMgmtService = new ScmIssueMgmtService(dataSource, scmAggService, null, workItemFieldsMetaService);
        workItemsPrioritySLAService = new WorkItemsPrioritySLAService(dataSource);
        workItemsService = new WorkItemsService(dataSource, null, null,
                null, null, null,
                null, null, workItemsPrioritySLAService,
                null, null, null, workItemFieldsMetaService, null);
        workItemTimelineService = new WorkItemTimelineService(dataSource);
        issuesMilestoneService = new IssuesMilestoneService(dataSource);
        issueMgmtSprintMappingDatabaseService = new IssueMgmtSprintMappingDatabaseService(dataSource);

        repositoryService = new GitRepositoryService(dataSource);
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        currentTime = DateUtils.truncate(new Date(Instant.parse("2021-05-05T15:00:00-08:00").getEpochSecond()), Calendar.DATE);

        azureDevopsIntegrationId = integrationService.insert(company, Integration.builder()
                .application("azure_devops")
                .name("azure test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        workItemsService.ensureTableExistence(company);
        workItemTimelineService.ensureTableExistence(company);
        issueMgmtSprintMappingDatabaseService.ensureTableExistence(company);
        issuesMilestoneService.ensureTableExistence(company);
        repositoryService.ensureTableExistence(company);
        workItemsPrioritySLAService.ensureTableExistence(company);

        String workItemsResource = "azuredevops/azure_devops_work_items_3.json";
        String timelinesResource = "azuredevops/azure_devops_workitem_history_3.json";
        String iterationsResource = "azuredevops/azure_devops_iterations_2.json";
        setup(company, azureDevopsIntegrationId, workItemsService, workItemTimelineService,
                issuesMilestoneService, issueMgmtSprintMappingDatabaseService, currentTime, timelinesResource,
                iterationsResource, workItemsResource, null, null, null);
    }

    static void setup(String company, String integrationId, WorkItemsService workItemService,
                      WorkItemTimelineService workItemTimelineService, IssuesMilestoneService issuesMilestoneService,
                      IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService, Date currentTime,
                      String historiesResourcePath, String iterationsResourcePath, String workitemsResourcePath,
                      List<IntegrationConfig.ConfigEntry> customFieldConfig,
                      List<DbWorkItemField> customFieldProperties,
                      List<String> customFieldTypes) throws IOException {
        setupHistories(company, integrationId, workItemTimelineService, historiesResourcePath);
        setupIterations(company, integrationId, issuesMilestoneService, iterationsResourcePath);
        setupWorkItems(company, integrationId, workItemService, workItemTimelineService, issuesMilestoneService,
                issueMgmtSprintMappingDatabaseService, currentTime, workitemsResourcePath,
                customFieldConfig, customFieldProperties, customFieldTypes);
    }

    public static void setupWorkItems(String company, String integrationId, WorkItemsService workItemsService,
                                      WorkItemTimelineService workItemsTimelineService, IssuesMilestoneService issuesMilestoneService,
                                      IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService, Date fetchTime,
                                      String workitemsResourcePath,
                                      List<IntegrationConfig.ConfigEntry> customFieldConfig,
                                      List<DbWorkItemField> customFieldProperties,
                                      List<String> customFieldTypes) throws IOException {
        String input = ResourceUtils.getResourceAsString(workitemsResourcePath);
        PaginatedResponse<EnrichedProjectData> enrichedProjectDataPaginatedResponse = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, EnrichedProjectData.class));
        LoadingCache<String, Optional<DbIssuesMilestone>> dbSprintLoadingCache = CacheBuilder.from("maximumSize=250")
                .build(CacheLoader.from(sprintId -> findDbSprintById(company, integrationId, sprintId, issuesMilestoneService)));
        Map<String, String> statusToStatusCategoryMapping = new HashMap<>();
        enrichedProjectDataPaginatedResponse.getResponse().getRecords().forEach(
                enrichedProjectData -> {
                    Project project = enrichedProjectData.getProject();
                    enrichedProjectData.getWorkItems().forEach(
                            workItem -> {
                                DbWorkItem dbWorkItem = DbWorkItem.fromAzureDevOpsWorkItem(integrationId, project,
                                        fetchTime, workItem, customFieldConfig, customFieldProperties, statusToStatusCategoryMapping, null);
                                log.info("setupAzureDevopsWorkItems company {}, integrationId {}, projectId {}," +
                                                " projectName {}, status {}, statusCategory {}", company, integrationId, project.getId(),
                                        project.getName(), dbWorkItem.getStatus(), dbWorkItem.getStatusCategory());
                                try {
                                    // -- update all the events that have startDate = 0 to createdAt
                                    try {
                                        workItemsTimelineService.updateZeroStartDatesForWorkItem(company, integrationId, dbWorkItem.getWorkItemId(), dbWorkItem.getWorkItemCreatedAt());
                                    } catch (SQLException e) {
                                        log.warn("setupAzureDevopsWorkItems: error updating zero start dates for customer={}, workItemId={}, createdAt={}", company, dbWorkItem.getWorkItemId(), dbWorkItem.getWorkItemCreatedAt(), e);
                                    }

                                    // -- create fake events for each missing type
                                    insertMissingFakeEvents(company, integrationId, dbWorkItem.getWorkItemId(), dbWorkItem.getWorkItemCreatedAt(), workItem, workItemsTimelineService);

                                    // -- dedupe and insert work item
                                    DbWorkItem oldWorkItem = workItemsService.get(company, integrationId, dbWorkItem.getWorkItemId(), dbWorkItem.getIngestedAt());
                                    if (oldWorkItem == null || (oldWorkItem.getWorkItemUpdatedAt() != null &&
                                            dbWorkItem.getWorkItemUpdatedAt() != null &&
                                            oldWorkItem.getWorkItemUpdatedAt().before(dbWorkItem.getWorkItemUpdatedAt()))) {
                                        dbWorkItem = computeHopsAndBounces(company, integrationId, dbWorkItem, workItemsTimelineService);
                                        workItemsService.insert(company, dbWorkItem);
                                    }

                                    // -- handle sprint mappings
                                    generateIssueSprintMappingsFromEvents(company, integrationId, workItem, dbSprintLoadingCache, workItemsTimelineService, issueMgmtSprintMappingDatabaseService);
                                } catch (SQLException | ExecutionException e) {
                                    log.warn("setupAzureDevopsWorkItems: error inserting project: " + workItem.getId()
                                            + " for project id: " + project.getId(), e);
                                }
                            });
                });
    }

    public static void setupIterations(String company, String integrationId, IssuesMilestoneService issuesMilestoneService,
                                       String iterationsResourcePath) throws IOException {
        String input = ResourceUtils.getResourceAsString(iterationsResourcePath);
        PaginatedResponse<EnrichedProjectData> enrichedProjectDataPaginatedResponse = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, EnrichedProjectData.class));
        enrichedProjectDataPaginatedResponse.getResponse().getRecords().forEach(
                enrichedProjectData -> {
                    Project project = enrichedProjectData.getProject();
                    enrichedProjectData.getIterations().forEach(
                            iteration -> {
                                DbIssuesMilestone dbIssuesMilestone = DbIssuesMilestone
                                        .fromAzureDevOpsIteration(integrationId, project, iteration);
                                try {
                                    issuesMilestoneService.insert(company, dbIssuesMilestone);
                                } catch (SQLException e) {
                                    log.warn("setupAzureDevopsIterations: error inserting iteration: " + iteration.getId()
                                            + " for project id: " + project.getId(), e);
                                }
                            });
                });
    }

    public static void setupHistories(String company, String integrationId, WorkItemTimelineService workItemTimelineService,
                                      String historiesResourcePath) throws IOException {
        String input = ResourceUtils.getResourceAsString(historiesResourcePath);
        PaginatedResponse<EnrichedProjectData> data = m.readValue(input,
                m.getTypeFactory().constructParametricType(PaginatedResponse.class, EnrichedProjectData.class));
        data.getResponse().getRecords().forEach(projectData -> {
            Project project = projectData.getProject();
            projectData.getWorkItemHistories().stream()
                    .sorted(Comparator.comparing(WorkItemUtils::getChangedDateFromHistory))
                    .forEach(workItemHistory -> {
                        List<DbWorkItemHistory> dbWorkItemHistories = DbWorkItemHistory
                                .fromAzureDevopsWorkItemHistories(String.valueOf(integrationId), workItemHistory, new Date());
                        dbWorkItemHistories.forEach(dbWorkItemHistory -> {
                            String fieldType = dbWorkItemHistory.getFieldType();
                            String workItemId = dbWorkItemHistory.getWorkItemId();
                            Timestamp currentEventStartDate = dbWorkItemHistory.getStartDate();
                            if (currentEventStartDate == null) {
                                log.warn("setupAzureDevopsWorkItems: skipping '{}' event without a startDate for customer={}, project={}, workItem={}, historyId={}",
                                        fieldType, company, project.getId(), workItemId, workItemHistory.getId());
                                return;
                            }
                            try {
                                // -- handle "fake event" (i.e previous value of oldest event)
                                if (dbWorkItemHistory.getOldValue() != null) {
                                    DbWorkItemHistory fakeEvent = createFakeEvent(Integer.valueOf(integrationId), workItemId, fieldType,
                                            dbWorkItemHistory.getOldValue(),
                                            Timestamp.from(Instant.ofEpochSecond(0)),
                                            currentEventStartDate);
                                    Optional<DbWorkItemHistory> optFirstEvent = workItemTimelineService.getFirstEvent(company, Integer.valueOf(integrationId), fieldType, workItemId);
                                    if (optFirstEvent.isEmpty()) {
                                        // if there are no other event of this type in the db: insert fake event
                                        workItemTimelineService.insertFakeEvent(company, fakeEvent);
                                    } else {
                                        // check if the oldest event is more recent than this fake event, if yes: replace it
                                        DbWorkItemHistory firstEvent = optFirstEvent.get();
                                        if (firstEvent.getEndDate() != null && firstEvent.getEndDate().compareTo(fakeEvent.getEndDate()) > 0) {
                                            workItemTimelineService.deleteEvent(company, firstEvent.getId().toString());
                                            workItemTimelineService.insertFakeEvent(company, fakeEvent);
                                        }
                                    }
                                }

                                // -- handle regular event
                                Optional<DbWorkItemHistory> previousEvent = workItemTimelineService.getLastEventBefore(company, Integer.valueOf(integrationId), fieldType, workItemId, currentEventStartDate);
                                Optional<DbWorkItemHistory> nextEvent = workItemTimelineService.getFirstEventAfter(company, Integer.valueOf(integrationId), fieldType, workItemId, currentEventStartDate);

                                if (previousEvent.isPresent() && !currentEventStartDate.equals(previousEvent.get().getEndDate())) {
                                    // -- update last event's end date with current event's start date
                                    DbWorkItemHistory lastEventUpdatedHistory = previousEvent.get().toBuilder()
                                            .endDate(currentEventStartDate)
                                            .build();
                                    workItemTimelineService.updateEndDate(company, lastEventUpdatedHistory);
                                }
                                if (nextEvent.isPresent()) {
                                    // -- update current event's end date with next event's start date
                                    dbWorkItemHistory = dbWorkItemHistory.toBuilder()
                                            .endDate(nextEvent.get().getStartDate())
                                            .build();
                                }
                                // insert current event (potentially updating end date if it already exists)
                                workItemTimelineService.upsert(company, dbWorkItemHistory);
                            } catch (Exception ex) {
                                log.warn("setupAzureDevopsWorkItems: error inserting '{}' event for customer={}, project={}, workItem={}, historyId={}",
                                        fieldType, company, project.getId(), workItemId, workItemHistory.getId(), ex);
                            }
                        });
                    });
        });
    }

    static DbWorkItem computeHopsAndBounces(String company, String integrationId, DbWorkItem workItem,
                                            WorkItemTimelineService workItemTimelineService) throws SQLException {
        List<DbWorkItemHistory> assignee = workItemTimelineService.getEvents(
                company, Integer.valueOf(integrationId), "assignee", workItem.getWorkItemId());
        return computeHopsAndBounces(workItem, assignee);
    }

    static DbWorkItem computeHopsAndBounces(DbWorkItem workItem, List<DbWorkItemHistory> assignee) {
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

    private static void insertMissingFakeEvents(String customer, String integrationId, String dbWorkItemId,
                                                Timestamp dbWorkItemCreatedAt, WorkItem workItem,
                                                WorkItemTimelineService workItemTimelineService) {
        try {
            Set<String> fieldTypesPresent = workItemTimelineService.getFirstEventOfEachType(customer, Integer.valueOf(integrationId), dbWorkItemId).stream()
                    .map(DbWorkItemHistory::getFieldType)
                    .map(String::toLowerCase)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toSet());
            if (!fieldTypesPresent.contains("sprint") && workItem.getFields().getIterationPath() != null) {
                workItemTimelineService.insertFakeEvent(customer, createFakeEvent(Integer.valueOf(integrationId),
                        dbWorkItemId, "sprint", workItem.getFields().getIterationPath(),
                        dbWorkItemCreatedAt, null));
            }
            if (!fieldTypesPresent.contains("status") && workItem.getFields().getState() != null) {
                workItemTimelineService.insertFakeEvent(customer, createFakeEvent(Integer.valueOf(integrationId),
                        dbWorkItemId, "status", workItem.getFields().getState(),
                        dbWorkItemCreatedAt, null));
            }
            if (!fieldTypesPresent.contains("story_points") && workItem.getFields().getStoryPoints() != null) {
                workItemTimelineService.insertFakeEvent(customer, createFakeEvent(Integer.valueOf(integrationId),
                        dbWorkItemId, "story_points", String.valueOf(workItem.getFields().getStoryPoints()),
                        dbWorkItemCreatedAt, null));
            }
            if (!fieldTypesPresent.contains("assignee") && workItem.getFields().getAssignedTo() != null) {
                workItemTimelineService.insertFakeEvent(customer, createFakeEvent(Integer.valueOf(integrationId),
                        dbWorkItemId, "assignee", workItem.getFields().getAssignedTo().getUniqueName(),
                        dbWorkItemCreatedAt, null));
            }
        } catch (SQLException e) {
            log.warn("insertMissingFakeEvents: error inserting fake events for customer={}, workItemId={}, createdAt={}", customer, dbWorkItemId, dbWorkItemCreatedAt, e);
        }
    }

    private static DbWorkItemHistory createFakeEvent(Integer integrationId, String workitemId, String fieldType,
                                                     String fieldValue, Timestamp startDate, Timestamp endDate) {
        return DbWorkItemHistory.builder()
                .workItemId(workitemId)
                .integrationId(String.valueOf(integrationId))
                .fieldType(fieldType)
                .fieldValue(fieldValue)
                .startDate(startDate)
                .endDate(endDate).build();
    }

    private static Optional<DbIssuesMilestone> findDbSprintById(String customer, String integrationId, String sprintId,
                                                                IssuesMilestoneService issuesMilestoneService) {
        if (StringUtils.isBlank(sprintId)) {
            return Optional.empty();
        }
        Pair<String, String> pathAndName = parseIterationPath(sprintId);
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

    static Pair<String, String> parseIterationPath(String iterationPath) {
        if (!iterationPath.contains("\\")) {
            return Pair.of(null, iterationPath);
        }
        int lastBackslash = iterationPath.lastIndexOf("\\");
        String parentKey = iterationPath.substring(0, lastBackslash);
        String name = iterationPath.substring(lastBackslash + 1);
        return Pair.of(parentKey, name);
    }

    protected static void generateIssueSprintMappingsFromEvents(String customer, String integrationId, WorkItem workItem,
                                                                LoadingCache<String, Optional<DbIssuesMilestone>> dbSprintCache,
                                                                WorkItemTimelineService workItemTimelineService,
                                                                IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService) throws SQLException, ExecutionException {
        if (workItem.getFields() == null || StringUtils.isEmpty(workItem.getFields().getIterationPath())) {
            log.info("Skipping sprint mapping for workitem with no iterationpath for customer " + customer + ",integ" + integrationId);
            return;
        }
        List<DbWorkItemHistory> sprintEvents = workItemTimelineService.getEvents(customer, Integer.valueOf(integrationId), "sprint", workItem.getId());
        for (DbWorkItemHistory sprintEvent : sanitizeEventList(sprintEvents, Instant.now())) {
            String iterationPath = sprintEvent.getFieldValue();
            log.info("Processing sprint event for customer=" + customer + ", integ=" + integrationId + ", workitemId=" + workItem.getId() + ", sprint=" + sprintEvent);
            if (StringUtils.isEmpty(iterationPath)) {
                log.info("Ignoring sprint with blank id for customer=" + customer + ", integ=" + integrationId + ",workitemId=" + workItem.getId() + ", sprint=" + sprintEvent);
                continue;
            }
            if (sprintEvent.getStartDate() == null || sprintEvent.getEndDate() == null) {
                log.info("Ignoring sprint event with no details for customer " + customer + ", integ=" + integrationId + ", workItemId=" + workItem.getId() + ", sprint=" + sprintEvent);
                continue;
            }
            long addedAt = sprintEvent.getStartDate().toInstant().getEpochSecond();
            boolean ignorableTaskType = SUB_TASK_ISSUE_TYPE.equalsIgnoreCase(MoreObjects.firstNonNull(workItem.getFields().getWorkItemType(), ""));
            DbIssuesMilestone sprintDetails = dbSprintCache.get(iterationPath).orElse(null);
            if (sprintDetails == null) {
                log.info("Ignoring sprint event: no corresponding sprint for customer " + customer + ", integ=" + integrationId + ", workItemId=" + workItem.getId() + ", sprint=" + sprintEvent);
                continue;
            }
            boolean planned = false;
            Long startDate = io.levelops.commons.dates.DateUtils.toEpochSecond(sprintDetails.getStartDate());
            Long endDate = io.levelops.commons.dates.DateUtils.toEpochSecond(sprintDetails.getEndDate());
            Long completedDate = io.levelops.commons.dates.DateUtils.toEpochSecond(sprintDetails.getCompletedAt());
            completedDate = (completedDate != null) ? completedDate : endDate;
            if (startDate != null) {
                planned = addedAt < startDate;
            }
            // > story points at the start and the end of the sprint
            float storyPointsPlanned = 0;
            float storyPointsDelivered = 0;
            if (startDate != null && completedDate != null) {
                List<DbWorkItemHistory> storyPointsLogs = workItemTimelineService.getEvents(customer, Integer.valueOf(integrationId), "story_points", workItem.getId());
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
                        log.info("Ignoring sprint for sprint log with null start time for customer=" + customer + ", integ=" + integrationId + ",workitem=" + workItem.getId() + ",sprint=" + sprintEvent);
                        continue;
                    }
                    if (planned) {
                        if (startDate >= storyPointsLogStartTime && startDate < storyPointsLogEndTime) {
                            storyPointsPlanned = storyPoints;
                        }
                    } else {
                        if (addedAt >= storyPointsLogStartTime && addedAt < storyPointsLogEndTime) {
                            storyPointsPlanned = storyPoints;
                        }
                    }
                    if (completedDate >= storyPointsLogStartTime && completedDate < storyPointsLogEndTime) {
                        storyPointsDelivered = storyPoints;
                    }
                }
            }
            boolean delivered = false;
            boolean outsideOfSprint = false;
            if (completedDate != null) {
                if (CLOSED_STATUS_CATEGORY.equals(MoreObjects.firstNonNull(workItem.getFields().getState(), "")) && addedAt < completedDate) {
                    delivered = true;
                }
                if ((startDate != null && addedAt < startDate) || (endDate != null && addedAt > endDate)) {
                    outsideOfSprint = true;
                }
            }
            DbIssueMgmtSprintMapping sprintMapping = DbIssueMgmtSprintMapping.builder()
                    .integrationId(integrationId)
                    .workitemId(workItem.getId())
                    .sprintId(iterationPath)
                    .addedAt(addedAt)
                    .planned(planned)
                    .outsideOfSprint(outsideOfSprint)
                    .delivered(delivered)
                    .ignorableWorkitemType(ignorableTaskType)
                    .storyPointsPlanned(storyPointsPlanned)
                    .storyPointsDelivered(storyPointsDelivered)
                    .build();
            issueMgmtSprintMappingDatabaseService.upsert(customer, sprintMapping);
        }
    }

    static List<DbWorkItemHistory> sanitizeEventList(List<DbWorkItemHistory> events, Instant now) {
        if (CollectionUtils.isEmpty(events)) {
            return List.of();
        }
        ArrayList<DbWorkItemHistory> sanitizedEvents = new ArrayList<>();
        for (int i = 0; i < events.size() - 1; i++) {
            DbWorkItemHistory current = events.get(i);
            DbWorkItemHistory next = events.get(i + 1);
            if (current.getEndDate() == null) {
                current = current.toBuilder()
                        .endDate(next.getStartDate())
                        .build();
            }
            sanitizedEvents.add(current);
        }
        DbWorkItemHistory last = events.get(events.size() - 1);
        if (last.getEndDate() == null) {
            last = last.toBuilder()
                    .endDate(Timestamp.from(now))
                    .build();
        }
        sanitizedEvents.add(last);
        return sanitizedEvents;
    }

    @Test
    public void testStoryPoints() throws SQLException {
        //test for values from timelines table 
        Assertions.assertThat(workItemTimelineService.getLastEvent(company, Integer.valueOf(azureDevopsIntegrationId),
                "story_points", "130").get().getFieldValue()).isEqualTo("1.0");
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId, "130",
                "Agile-Project\\Test").getStoryPointsPlanned()).isEqualTo(1);
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId, "130",
                "Agile-Project\\Test").getStoryPointsDelivered()).isEqualTo(0);

        Assertions.assertThat(workItemTimelineService.getLastEvent(company, Integer.valueOf(azureDevopsIntegrationId),
                "story_points", "108").get().getFieldValue()).isEqualTo("5.0");
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId, "108",
                "project-test-9\\Sprint 2").getStoryPointsPlanned()).isEqualTo(5);
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId, "108",
                "project-test-9\\Sprint 2").getStoryPointsDelivered()).isEqualTo(5);

        Assertions.assertThat(workItemTimelineService.getLastEvent(company, Integer.valueOf(azureDevopsIntegrationId),
                "story_points", "106").orElse(null)).isNull();
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId, "106",
                "Agile-Project\\sprint-2").getStoryPointsPlanned()).isEqualTo(0);
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId, "106",
                "Agile-Project\\sprint-2").getStoryPointsDelivered()).isEqualTo(0);

        Assertions.assertThat(workItemTimelineService.getLastEvent(company, Integer.valueOf(azureDevopsIntegrationId),
                "story_points", "118").get().getFieldValue()).isEqualTo("10.0");
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId, "118",
                "Agile-Project\\Test").getStoryPointsPlanned()).isEqualTo(10);
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId, "118",
                "Agile-Project\\Test").getStoryPointsDelivered()).isEqualTo(10);

        Assertions.assertThat(workItemTimelineService.getLastEvent(company, Integer.valueOf(azureDevopsIntegrationId),
                "story_points", "99").orElse(null)).isNull();
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId, "99",
                "project-test-11\\April sprint 2").getStoryPointsPlanned()).isEqualTo(0);
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId, "99",
                "project-test-11\\April sprint 2").getStoryPointsDelivered()).isEqualTo(00);

        Assertions.assertThat(workItemTimelineService.getLastEvent(company, Integer.valueOf(azureDevopsIntegrationId),
                "story_points", "119").get().getFieldValue()).isEqualTo("1.0");
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId, "119",
                "Agile-Project\\Test").getStoryPointsPlanned()).isEqualTo(1);
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId, "119",
                "Agile-Project\\Test").getStoryPointsDelivered()).isEqualTo(1);

        Assertions.assertThat(workItemTimelineService.getLastEvent(company, Integer.valueOf(azureDevopsIntegrationId),
                "story_points", "121").get().getFieldValue()).isEqualTo("2.0");
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId, "121",
                "Agile-Project\\Test").getStoryPointsPlanned()).isEqualTo(2);
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId, "121",
                "Agile-Project\\Test").getStoryPointsDelivered()).isEqualTo(2);

        Assertions.assertThat(workItemTimelineService.getLastEvent(company, Integer.valueOf(azureDevopsIntegrationId),
                "story_points", "18").get().getFieldValue()).isEqualTo("2.0");
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId, "18",
                "Agile-Project\\Test").getStoryPointsPlanned()).isEqualTo(2);
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId, "18",
                "Agile-Project\\Test").getStoryPointsDelivered()).isEqualTo(2);

    }

    @Test
    public void testDifferentStoryPointsPlannedAndDelivered() throws SQLException {
        //check for different story points planned and delivered
        DbWorkItemHistory storyPointsEvent = workItemTimelineService.getLastEvent(company,
                Integer.valueOf(azureDevopsIntegrationId), "story_points", "15").get();
        String timelineStoryPointsValue = storyPointsEvent.getFieldValue();
        Optional<DbIssuesMilestone> milestone = issuesMilestoneService.getMilestoneByParentKeyAndName(company,
                azureDevopsIntegrationId, "sprint", "Agile-Project", "Test");
        Long sprintCompletedAt = io.levelops.commons.dates.DateUtils.toEpochSecond(milestone.get().getCompletedAt());
        Long storyPointsEventStartedAt = io.levelops.commons.dates.DateUtils.toEpochSecond(storyPointsEvent.getStartDate());
        Long storyPointsEventEndedAt = io.levelops.commons.dates.DateUtils.toEpochSecond(storyPointsEvent.getEndDate());
        Assertions.assertThat(timelineStoryPointsValue).isEqualTo("1.0");
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId,
                "15", "Agile-Project\\Test").getStoryPointsPlanned()).isEqualTo(1);
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId,
                "15", "Agile-Project\\Test").getStoryPointsDelivered()).isEqualTo(0);
        Assertions.assertThat(milestone.orElse(null)).isNotNull();
        Assertions.assertThat(sprintCompletedAt).isNotNull();
        Assertions.assertThat(sprintCompletedAt > storyPointsEventStartedAt && sprintCompletedAt < storyPointsEventEndedAt).isFalse();


        storyPointsEvent = workItemTimelineService.getLastEvent(company,
                Integer.valueOf(azureDevopsIntegrationId), "story_points", "130").get();
        timelineStoryPointsValue = storyPointsEvent.getFieldValue();
        milestone = issuesMilestoneService.getMilestoneByParentKeyAndName(company,
                azureDevopsIntegrationId, "sprint", "Agile-Project", "Test");
        sprintCompletedAt = io.levelops.commons.dates.DateUtils.toEpochSecond(milestone.get().getCompletedAt());
        storyPointsEventStartedAt = io.levelops.commons.dates.DateUtils.toEpochSecond(storyPointsEvent.getStartDate());
        storyPointsEventEndedAt = io.levelops.commons.dates.DateUtils.toEpochSecond(storyPointsEvent.getEndDate());
        Assertions.assertThat(timelineStoryPointsValue).isEqualTo("1.0");
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId,
                "130", "Agile-Project\\Test").getStoryPointsPlanned()).isEqualTo(1);
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId,
                "130", "Agile-Project\\Test").getStoryPointsDelivered()).isEqualTo(0);
        Assertions.assertThat(milestone.orElse(null)).isNotNull();
        Assertions.assertThat(sprintCompletedAt).isNotNull();
        Assertions.assertThat(sprintCompletedAt > storyPointsEventStartedAt && sprintCompletedAt < storyPointsEventEndedAt).isFalse();
    }

    @Test
    public void testSameStoryPointsPlannedAndDelivered() throws SQLException {
        //check for different story points planned and delivered
        DbWorkItemHistory storyPointsEvent = workItemTimelineService.getLastEvent(company,
                Integer.valueOf(azureDevopsIntegrationId), "story_points", "18").get();
        String timelineStoryPointsValue = storyPointsEvent.getFieldValue();
        Optional<DbIssuesMilestone> milestone = issuesMilestoneService.getMilestoneByParentKeyAndName(company,
                azureDevopsIntegrationId, "sprint", "Agile-Project", "Test");
        Long sprintCompletedAt = io.levelops.commons.dates.DateUtils.toEpochSecond(milestone.get().getCompletedAt());
        Long storyPointsEventStartedAt = io.levelops.commons.dates.DateUtils.toEpochSecond(storyPointsEvent.getStartDate());
        Long storyPointsEventEndedAt = io.levelops.commons.dates.DateUtils.toEpochSecond(Optional.ofNullable(storyPointsEvent.getEndDate()).orElse(Timestamp.from(Instant.now())));
        Assertions.assertThat(timelineStoryPointsValue).isEqualTo("2.0");
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId,
                "18", "Agile-Project\\Test").getStoryPointsPlanned()).isEqualTo(2);
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId,
                "18", "Agile-Project\\Test").getStoryPointsDelivered()).isEqualTo(2);
        Assertions.assertThat(milestone.orElse(null)).isNotNull();
        Assertions.assertThat(sprintCompletedAt).isNotNull();
        Assertions.assertThat(sprintCompletedAt >= storyPointsEventStartedAt && sprintCompletedAt < storyPointsEventEndedAt).isTrue();


        storyPointsEvent = workItemTimelineService.getLastEvent(company,
                Integer.valueOf(azureDevopsIntegrationId), "story_points", "108").get();
        timelineStoryPointsValue = storyPointsEvent.getFieldValue();
        milestone = issuesMilestoneService.getMilestoneByParentKeyAndName(company,
                azureDevopsIntegrationId, "sprint", "project-test-9", "Sprint 2");
        sprintCompletedAt = io.levelops.commons.dates.DateUtils.toEpochSecond(milestone.get().getCompletedAt());
        storyPointsEventStartedAt = io.levelops.commons.dates.DateUtils.toEpochSecond(storyPointsEvent.getStartDate());
        storyPointsEventEndedAt = io.levelops.commons.dates.DateUtils.toEpochSecond(Optional.ofNullable(storyPointsEvent.getEndDate()).orElse(Timestamp.from(Instant.now())));
        Assertions.assertThat(timelineStoryPointsValue).isEqualTo("5.0");
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId,
                "108", "project-test-9\\Sprint 2").getStoryPointsPlanned()).isEqualTo(5);
        Assertions.assertThat(issueMgmtSprintMappingDatabaseService.get(company, azureDevopsIntegrationId,
                "108", "project-test-9\\Sprint 2").getStoryPointsDelivered()).isEqualTo(5);
        Assertions.assertThat(milestone.orElse(null)).isNotNull();
        Assertions.assertThat(sprintCompletedAt).isNotNull();
        Assertions.assertThat(sprintCompletedAt > storyPointsEventStartedAt && sprintCompletedAt < storyPointsEventEndedAt).isTrue();
    }

    @Test
    public void testRelationBetweenSprintStartAndStoryPointLog() throws SQLException {
        List<DbWorkItemHistory> sprintEvents = workItemTimelineService.getEvents(company,
                Integer.valueOf(azureDevopsIntegrationId), "sprint", "149");
        for (DbWorkItemHistory sprintEvent : sanitizeEventList(sprintEvents, Instant.now())) {
            Assertions.assertThat(sprintEvent.getStartDate()).isNotNull();
            Assertions.assertThat(sprintEvent.getEndDate()).isNotNull();
            long addedAt = sprintEvent.getStartDate().toInstant().getEpochSecond();
            DbIssuesMilestone sprintDetails = issuesMilestoneService.getMilestoneByParentKeyAndName(company,
                    azureDevopsIntegrationId, "sprint", "Agile-Project", "sprint-2").orElse(null);
            Assertions.assertThat(sprintDetails).isNotNull();
            boolean planned = false;
            Long startDate = io.levelops.commons.dates.DateUtils.toEpochSecond(sprintDetails.getStartDate());
            Long endDate = io.levelops.commons.dates.DateUtils.toEpochSecond(sprintDetails.getEndDate());
            Long completedDate = io.levelops.commons.dates.DateUtils.toEpochSecond(sprintDetails.getCompletedAt());
            completedDate = (completedDate != null) ? completedDate : endDate;
            if (startDate != null) {
                planned = addedAt < startDate;
            }
            Assertions.assertThat(planned).isFalse();
            Assertions.assertThat(startDate).isNotNull();
            Assertions.assertThat(completedDate).isNotNull();
            List<DbWorkItemHistory> storyPointsLogs = workItemTimelineService.getEvents(company,
                    Integer.valueOf(azureDevopsIntegrationId), "story_points", "149");
            for (DbWorkItemHistory storyPointsLog : storyPointsLogs) {
                if (!storyPointsLog.getFieldValue().equals("null")) {
                    Long storyPointsLogStartTime = Optional.ofNullable(storyPointsLog.getStartDate())
                            .map(Timestamp::toInstant)
                            .map(Instant::getEpochSecond)
                            .orElse(null);
                    Long storyPointsLogEndTime = Optional.ofNullable(storyPointsLog.getEndDate())
                            .map(Timestamp::toInstant)
                            .map(Instant::getEpochSecond)
                            .orElse(Instant.now().getEpochSecond());
                    Assertions.assertThat(storyPointsLogStartTime).isNotNull();
                    Assertions.assertThat(startDate >= storyPointsLogStartTime && startDate < storyPointsLogEndTime).isFalse();
                    Assertions.assertThat(addedAt >= storyPointsLogStartTime && addedAt < storyPointsLogEndTime).isFalse();
                    Assertions.assertThat(completedDate >= storyPointsLogStartTime && completedDate < storyPointsLogEndTime).isFalse();
                }
            }
        }
    }
}
