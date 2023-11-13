package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.databases.issue_management.DbIssueMgmtSprintMapping;
import io.levelops.commons.databases.issue_management.DbIssueStatusMetadata;
import io.levelops.commons.databases.issue_management.DbIssuesMilestone;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.issue_management.DbWorkItemHistory;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.azureDevops.models.EnrichedProjectData;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.WorkItem;
import io.levelops.integrations.azureDevops.utils.WorkItemUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
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
public class IssueMgmtTestUtil {

    private static final String SUB_TASK_ISSUE_TYPE = "SUB-TASK";
    private static final String CLOSED_STATUS_CATEGORY = "Closed";
    private static final String ASSIGNEE_UNASSIGNED = "UNASSIGNED";

    private static final ObjectMapper m = DefaultObjectMapper.get();

    static void setup(String company, String integrationId, WorkItemsService workItemService,
                     WorkItemTimelineService workItemTimelineService, IssuesMilestoneService issuesMilestoneService,
                     IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService, Date currentTime,
                     String historiesResourcePath, String iterationsResourcePath, String workitemsResourcePath,
                     List<IntegrationConfig.ConfigEntry> customFieldConfig,
                     List<DbWorkItemField> customFieldProperties,
                     List<String> customFieldTypes, UserIdentityService userIdentityService) throws IOException {
        setupHistories(company, integrationId, workItemTimelineService, historiesResourcePath);
        setupIterations(company, integrationId, issuesMilestoneService, iterationsResourcePath);
        setupWorkItems(company, integrationId, workItemService, workItemTimelineService, issuesMilestoneService,
                issueMgmtSprintMappingDatabaseService, currentTime, workitemsResourcePath,
                customFieldConfig, customFieldProperties, customFieldTypes, userIdentityService);
    }

    public static void setupWorkItems(String company, String integrationId, WorkItemsService workItemsService,
                                      WorkItemTimelineService workItemsTimelineService, IssuesMilestoneService issuesMilestoneService,
                                      IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService, Date fetchTime,
                                      String workitemsResourcePath,
                                      List<IntegrationConfig.ConfigEntry> customFieldConfig,
                                      List<DbWorkItemField> customFieldProperties,
                                      List<String> customFieldTypes,
                                      UserIdentityService userIdentityService) throws IOException {
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
                                        dbWorkItem = upsertAndPopulateDbWorkItemIds(company, dbWorkItem, userIdentityService);
                                    } catch (SQLException e) {
                                        log.warn("setupAzureDevopsWorkItems: error populting assignee and reporter Ids", e);
                                    }
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

    public static DbWorkItem upsertAndPopulateDbWorkItemIds(String company, DbWorkItem dbWorkItem,
                                                            UserIdentityService userIdentityService) throws SQLException {
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
                            Timestamp currentEventEndDate = dbWorkItemHistory.getEndDate();
                            if (currentEventStartDate == null) {
                                log.warn("setupAzureDevopsWorkItems: skipping '{}' event without a startDate for customer={}," +
                                                " project={}, workItem={}, historyId={}",
                                        fieldType, company, project.getId(), workItemId, workItemHistory.getId());
                                return;
                            }
                            try {
                                // -- handle "fake event" (i.e previous value of oldest event)
                                if (dbWorkItemHistory.getOldValue() != null) {
                                    DbWorkItemHistory fakeEvent = createFakeEvent(Integer.valueOf(integrationId), workItemId, fieldType,
                                            dbWorkItemHistory.getOldValue(),
                                            Timestamp.from(Instant.ofEpochSecond(0)),
                                            currentEventEndDate);
                                    Optional<DbWorkItemHistory> optFirstEvent = workItemTimelineService.getFirstEvent(company,
                                            Integer.valueOf(integrationId), fieldType, workItemId);
                                    if (optFirstEvent.isEmpty()) {
                                        // if there are no other event of this type in the db: insert fake event
                                        workItemTimelineService.insertFakeEvent(company, fakeEvent);
                                    } else {
                                        // check if the oldest event is more recent than this fake event, if yes: replace it
                                        DbWorkItemHistory firstEvent = optFirstEvent.get();
                                        if (firstEvent.getEndDate() != null && firstEvent.getEndDate().compareTo(fakeEvent.getEndDate()) > 0) {
                                            workItemTimelineService.delete(company, firstEvent.getId().toString());
                                            workItemTimelineService.insertFakeEvent(company, fakeEvent);
                                        }
                                    }
                                }

                                // -- handle regular event
                                Optional<DbWorkItemHistory> previousEvent = workItemTimelineService.getLastEventBefore(company,
                                        Integer.valueOf(integrationId), fieldType, workItemId, currentEventStartDate);
                                Optional<DbWorkItemHistory> nextEvent = workItemTimelineService.getFirstEventAfter(company,
                                        Integer.valueOf(integrationId), fieldType, workItemId, currentEventStartDate);

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
                                log.warn("setupAzureDevopsWorkItems: error inserting '{}' event for customer={}," +
                                                " project={}, workItem={}, historyId={}",
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
        Timestamp endDate = DateUtils.toTimestamp(new Date().toInstant());
        try {
            Set<String> fieldTypesPresent = workItemTimelineService.getFirstEventOfEachType(customer,
                            Integer.valueOf(integrationId), dbWorkItemId).stream()
                    .map(DbWorkItemHistory::getFieldType)
                    .map(String::toLowerCase)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toSet());
            if (!fieldTypesPresent.contains("sprint") && workItem.getFields().getIterationPath() != null) {
                workItemTimelineService.insertFakeEvent(customer, createFakeEvent(Integer.valueOf(integrationId),
                        dbWorkItemId, "sprint", workItem.getFields().getIterationPath(),
                        dbWorkItemCreatedAt, endDate));
            }
            if (!fieldTypesPresent.contains("status") && workItem.getFields().getState() != null) {
                workItemTimelineService.insertFakeEvent(customer, createFakeEvent(Integer.valueOf(integrationId),
                        dbWorkItemId, "status", workItem.getFields().getState(),
                        dbWorkItemCreatedAt, endDate));
            }
            if (!fieldTypesPresent.contains("story_points") && workItem.getFields().getStoryPoints() != null) {
                workItemTimelineService.insertFakeEvent(customer, createFakeEvent(Integer.valueOf(integrationId),
                        dbWorkItemId, "story_points", String.valueOf(workItem.getFields().getStoryPoints()),
                        dbWorkItemCreatedAt, endDate));
            }
            if (!fieldTypesPresent.contains("assignee") && workItem.getFields().getAssignedTo() != null) {
                workItemTimelineService.insertFakeEvent(customer, createFakeEvent(Integer.valueOf(integrationId),
                        dbWorkItemId, "assignee", workItem.getFields().getAssignedTo().getUniqueName(),
                        dbWorkItemCreatedAt, endDate));
            }
        } catch (SQLException e) {
            log.warn("setupAzureDevopsWorkItems: error inserting fake events for customer={}, workItemId={}," +
                    " createdAt={}", customer, dbWorkItemId, dbWorkItemCreatedAt, e);
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
        String parentKey = "";
        String name = "";
        if (sprintId.contains("\\")) {
            parentKey = sprintId.substring(0, sprintId.indexOf("\\"));
            name = sprintId.substring(sprintId.lastIndexOf("\\") + 1);
        }
        if (StringUtils.isEmpty(parentKey) || StringUtils.isEmpty(name))
            return Optional.empty();
        try {
            return issuesMilestoneService.getMilestoneByParentKeyAndName(customer, integrationId, "sprint", parentKey, name);
        } catch (Exception e) {
            log.warn("Failed to lookup dbSprint by id", e);
            return Optional.empty();
        }
    }

    protected static void generateIssueSprintMappingsFromEvents(String customer, String integrationId, WorkItem workItem,
                                                                LoadingCache<String, Optional<DbIssuesMilestone>> dbSprintCache,
                                                                WorkItemTimelineService workItemTimelineService,
                                                                IssueMgmtSprintMappingDatabaseService issueMgmtSprintMappingDatabaseService) throws SQLException, ExecutionException {
        if (workItem.getFields() == null || StringUtils.isEmpty(workItem.getFields().getIterationPath())) {
            return;
        }
        List<DbWorkItemHistory> sprints = workItemTimelineService.getEvents(customer, Integer.valueOf(integrationId), "sprint", workItem.getId());
        for (DbWorkItemHistory sprintHistory : sprints) {
            String sprintId = sprintHistory.getFieldValue();
            if (StringUtils.isEmpty(sprintId)) {
                log.warn("Ignoring sprint with blank id");
                continue;
            }
            if (sprintHistory.getStartDate() == null || sprintHistory.getEndDate() == null) {
                log.warn("Ignoring sprint with details not present for added_at date");
                continue;
            }
            long addedAt = sprintHistory.getStartDate().toInstant().getEpochSecond();
            boolean ignorableTaskType = SUB_TASK_ISSUE_TYPE.equalsIgnoreCase(MoreObjects.firstNonNull(workItem.getFields().getWorkItemType(), ""));
            DbIssuesMilestone sprintDetails = dbSprintCache.get(sprintId).orElse(null);
            if (sprintDetails == null) {
                log.warn("Ignoring sprint with no details");
                continue;
            }
            boolean planned = false;
            Long startDate = DateUtils.toEpochSecond(sprintDetails.getStartDate());
            Long endDate = DateUtils.toEpochSecond(sprintDetails.getEndDate());
            Long completedDate = DateUtils.toEpochSecond(sprintDetails.getCompletedAt());
            Long removed_at = DateUtils.toEpochSecond(sprintHistory.getEndDate());
            completedDate = (completedDate != null) ? completedDate : endDate;
            if (startDate != null) {
                planned = addedAt < startDate;
            }
            Float storyPointsPlanned = Float.valueOf(MoreObjects.firstNonNull(workItem.getFields().getOriginalEstimate(), "0"));
            Float storyPointsDelivered = Float.valueOf(MoreObjects.firstNonNull(workItem.getFields().getCompletedWork(), "0"));
            boolean delivered = false;
            boolean outsideOfSprint = false;
            if (completedDate != null) {
                if (CLOSED_STATUS_CATEGORY.equals(MoreObjects.firstNonNull(workItem.getFields().getState(), "")) && addedAt < completedDate) {
                    delivered = true;
                }
                if (addedAt < startDate || (endDate != null && addedAt > endDate))
                    outsideOfSprint = true;
            }
            DbIssueMgmtSprintMapping sprintMapping = DbIssueMgmtSprintMapping.builder()
                    .integrationId(integrationId)
                    .workitemId(workItem.getId())
                    .sprintId(sprintId)
                    .addedAt(addedAt)
                    .removedAt(removed_at)
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

    public static void setupStatusMetadata(String company, String integrationId, WorkItemsMetadataService workItemsMetadataService) throws IOException, SQLException {
        for (int i = 0; i < 5; i++) {
            DbIssueStatusMetadata statusMetadata = DbIssueStatusMetadata.builder()
                    .integrationId(integrationId)
                    .projectId("project-" + i)
                    .statusId("status-" + i)
                    .statusCategory("status-category-" + i)
                    .status("status-" + i)
                    .build();
            workItemsMetadataService.insert(company, statusMetadata);
        }
    }
}
