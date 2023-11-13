package io.levelops.aggregations_shared.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.cache.LoadingCache;
import io.levelops.aggregations_shared.models.SnapshottingSettings;
import io.levelops.aggregations_shared.services.AutomationRulesEngine;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.automation_rules.ObjectType;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraIssueSprintMapping;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.jira.DbJiraStatusMetadata;
import io.levelops.commons.databases.models.database.jira.DbJiraStoryPoints;
import io.levelops.commons.databases.models.database.jira.DbJiraVersion;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser.JiraParserConfig;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.JiraIssueSprintMappingDatabaseService;
import io.levelops.commons.databases.services.JiraIssueStoryPointsDatabaseService;
import io.levelops.commons.databases.services.JiraStatusMetadataDatabaseService;
import io.levelops.commons.databases.services.UserIdentityMaskingService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.commons.utils.NumberUtils;
import io.levelops.controlplane.trigger.strategies.JobTags;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.models.EventsClientException;
import io.levelops.ingestion.integrations.jira.models.JiraIterativeScanQuery;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.integrations.jira.converters.JiraCustomFieldConverter;
import io.levelops.integrations.jira.models.JiraComment;
import io.levelops.integrations.jira.models.JiraCommentsResult;
import io.levelops.integrations.jira.models.JiraComponent;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraIssueFields;
import io.levelops.integrations.jira.models.JiraIssueType;
import io.levelops.integrations.jira.models.JiraProject;
import io.levelops.integrations.jira.models.JiraSprint;
import io.levelops.integrations.jira.models.JiraUser;
import io.levelops.integrations.jira.models.JiraVersion;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.aggregations_shared.utils.IntegrationUtils.DISABLE_SNAPSHOTTING;
import static io.levelops.commons.databases.models.database.jira.DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.ADDED;
import static io.levelops.commons.databases.models.database.jira.DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.REMOVED;


@Log4j2
@Service
public class JiraAggHelperService {

    private static final Set<String> RELEVANT_TENANT_IDS = Set.of("cdkglobal");
    private static final Set<String> DUPLICATE_STATUS_OVERLAP_TENANTS = Set.of("");
    private static final Set<String> RELEVANT_ISSUE_KEYS = Set.of("FLXPCKMIGR-2360", "FLXPCKMIGR-3503", "FLXPCKMIGR-2712", "FLXPCKMIGR-2363", "FLXPCKMIGR-2361", "FLXPCKMIGR-3348", "FLXPCKMIGR-3347", "FLXPCKMIGR-3531", "FLXPCKMIGR-3526", "FLXPCKMIGR-3525", "FLXPCKMIGR-3530", "FLXPCKMIGR-2742", "FLXPCKMIGR-2350", "FLXPCKMIGR-3604", "FLXPCKMIGR-3343", "FLXPCKMIGR-2711", "FLXPCKMIGR-2680", "FLXPCKMIGR-2679", "FLXPCKMIGR-2741", "FLXPCKMIGR-2678", "FLXPCKMIGR-2685", "FLXPCKMIGR-3593", "FLXPCKMIGR-3580", "FLXPCKMIGR-3567", "FLXPCKMIGR-3549", "FLXPCKMIGR-3501");

    private static final String DEFAULT_ISSUES_DATATYPE = "issues";
    public static final String DEFAULT_SPRINTS_DATATYPE = "sprints";
    private static final int MAX_SPRINT_ID_CACHE_SIZE = 10000;
    private static final String DONE_STATUS_CATEGORY = "DONE";
    private static final String SUB_TASK_ISSUE_TYPE = "SUB-TASK";
    private static final String UNKNOWN = "_UNKNOWN_";
    private static final String UNASSIGNED = "_UNASSIGNED_";
    private static final String EPIC_ISSUE_TYPE = "EPIC";
    private final JiraIssueService jiraIssueService;
    private final ObjectMapper mapper;
    private final InventoryService inventoryService;
    private final AutomationRulesEngine automationRulesEngine;
    private final EventsClient eventsClient;
    private final JiraIssueStoryPointsDatabaseService storyPointsDatabaseService;
    private final JiraIssueSprintMappingDatabaseService sprintMappingDatabaseService;
    private final JiraStatusMetadataDatabaseService statusMetadataDatabaseService;
    private final boolean sendJiraUpdateEvents;
    private final UserIdentityService userIdentityService;
    private final UserIdentityMaskingService userIdentityMaskingService;
    private final Set<String> parentLabelsTenants;
    private final Set<String> parentLabelsTenantsEpicsOnly;
    private final SnapshottingSettings snapshottingSettings;


    @Autowired
    public JiraAggHelperService(ObjectMapper mapper,
                                JiraIssueService jiraIssueService,
                                InventoryService inventoryService,
                                AutomationRulesEngine automationRulesEngine,
                                EventsClient eventsClient,
                                JiraIssueStoryPointsDatabaseService storyPointsDatabaseService,
                                JiraIssueSprintMappingDatabaseService sprintMappingDatabaseService,
                                JiraStatusMetadataDatabaseService statusMetadataDatabaseService,
                                UserIdentityService userIdentityService,
                                UserIdentityMaskingService userIdentityMaskingService,
                                @Value("${SEND_JIRA_UPDATE_EVENTS:false}") boolean sendJiraUpdateEvents,
                                @Value("${JIRA_PARENT_LABELS_TENANTS:}") String parentLabelsTenantsString,
                                @Value("${JIRA_PARENT_LABELS_TENANTS_EPICS_ONLY:}") String parentLabelsTenantsEpicsOnlyString,
                                SnapshottingSettings snapshottingSettings
    ) {
        this.jiraIssueService = jiraIssueService;
        this.inventoryService = inventoryService;
        this.mapper = mapper;
        this.automationRulesEngine = automationRulesEngine;
        this.eventsClient = eventsClient;
        this.storyPointsDatabaseService = storyPointsDatabaseService;
        this.sprintMappingDatabaseService = sprintMappingDatabaseService;
        this.statusMetadataDatabaseService = statusMetadataDatabaseService;
        this.sendJiraUpdateEvents = sendJiraUpdateEvents;
        this.userIdentityService = userIdentityService;
        this.userIdentityMaskingService = userIdentityMaskingService;
        parentLabelsTenants = CommaListSplitter.splitToSet(parentLabelsTenantsString);
        parentLabelsTenantsEpicsOnly = CommaListSplitter.splitToSet(parentLabelsTenantsEpicsOnlyString);
        this.snapshottingSettings = snapshottingSettings;
    }

    @lombok.Value
    @Builder
    public static class ProcessingStatus {
        boolean success;
        boolean todayIssueIsNew;
        boolean issueIsActuallyNewOrUpdated;
        boolean issueNeedsReprocessing;
        boolean shouldInsert;
        boolean eventSent;
    }

    public ProcessingStatus processJiraIssue(String customer,
                                             String integrationId,
                                             JobDTO jobDTO,
                                             JiraIssue issue,
                                             Date currentTime,
                                             JiraParserConfig parserConfig,
                                             Long configVersion,
                                             boolean reprocessingRequested,
                                             List<String> productIds,
                                             Map<Integer, Long> sprintIdCache,
                                             LoadingCache<String, Optional<DbJiraSprint>> dbSprintLoadingCache,
                                             LoadingCache<String, Optional<String>> statusIdToStatusCategoryCache,
                                             LoadingCache<String, Optional<String>> userIdByDisplayNameCache,
                                             LoadingCache<String, Optional<String>> userIdByCloudIdCache,
                                             Boolean isEnableToMaskUser) {
        ProcessingStatus.ProcessingStatusBuilder processingStatusBuilder = ProcessingStatus.builder()
                .success(false);

        boolean snapshottingDisabled = !snapshottingSettings.isSnapshottingEnabled(customer, integrationId);

        // -- parse issue
        DbJiraIssue parsedIssue;
        try {
            parsedIssue = JiraIssueParser.parseJiraIssue(issue, integrationId, currentTime, parserConfig);
            parsedIssue = populateDbJiraIssueUserIds(customer, integrationId, issue, parsedIssue, userIdByDisplayNameCache, userIdByCloudIdCache, isEnableToMaskUser);
            logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, parsedIssue created", customer, parsedIssue.getKey());
        } catch (Exception e) {
            log.error("failed to parse a jira issue: customer={}, integrationId={}, key={}", customer, integrationId, issue.getKey(), e);
            log.debug("issue = {}, integrationId = {}, currentTime = {}, parserConfig={}",
                    issue, integrationId, currentTime, parserConfig);
            return processingStatusBuilder.build();
        }

        if (snapshottingDisabled) {
            // when snapshotting is disabled, we will overwrite ingestedAt with a special value
            // Note: we are not passing that value to JiraIssueParser.parseJiraIssue because it is too large and throws an error during truncation
            parsedIssue = parsedIssue.toBuilder()
                    .ingestedAt(DISABLE_SNAPSHOTTING)
                    .build();
        }

        // -- dedupe and insert
        Optional<DbJiraIssue> oldIssue;
        try {
            oldIssue = jiraIssueService.get(customer, parsedIssue.getKey(), integrationId, parsedIssue.getIngestedAt());
        } catch (Exception e) {
            log.warn("Error getting old issue from db for key={}", parsedIssue.getKey(), e);
            return processingStatusBuilder.build();
        }

        logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, oldIssue {}", customer, parsedIssue.getKey(), oldIssue);

        boolean todayIssueIsNew = oldIssue.isEmpty();
        boolean issueIsActuallyNewOrUpdated = todayIssueIsNew || oldIssue.get().getIssueUpdatedAt() < parsedIssue.getIssueUpdatedAt();
        boolean issueNeedsReprocessing = snapshottingDisabled && isReprocessingNeeded(oldIssue.orElse(null), configVersion);

        boolean shouldInsert = issueIsActuallyNewOrUpdated || issueNeedsReprocessing || reprocessingRequested;

        processingStatusBuilder
                .todayIssueIsNew(todayIssueIsNew)
                .issueIsActuallyNewOrUpdated(issueIsActuallyNewOrUpdated)
                .issueNeedsReprocessing(issueNeedsReprocessing)
                .shouldInsert(shouldInsert);

        logInfoIfRelevant(customer, parsedIssue, "customer={}, key={}, issueIsActuallyNewOrUpdated={}, todayIssueIsNew={}, issueNeedsReprocessing={}, shouldInsert={}, reprocessingRequested={}", customer, parsedIssue.getKey(), issueIsActuallyNewOrUpdated, todayIssueIsNew, issueNeedsReprocessing, shouldInsert, reprocessingRequested);
        if (shouldInsert) {
            DbJiraIssue.DbJiraIssueBuilder builder = parsedIssue.toBuilder();

            // -- inherit parent labels (if enabled)
            builder = builder.parentLabels(getInheritedParentLabels(customer, integrationId, parsedIssue));

            // -- set issue's config version to current
            builder = builder.configVersion(configVersion);

            // -- upsert issue
            parsedIssue = builder.build();
            try {
                jiraIssueService.insert(customer, parsedIssue);
                logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, inserted parsedIssue", customer, parsedIssue.getKey());
            } catch (Exception e) {
                log.warn("Error upserting issue with key={}, isEmpty={}", parsedIssue.getKey(), oldIssue.isEmpty(), e);
                return processingStatusBuilder.build();
            }
        }


        log.debug("customer={}, key={}, issueIsActuallyNewOrUpdated={}, issueNeedsReprocessing={}, shouldInsert={}, reprocessingRequested={}", customer, parsedIssue.getKey(), issueIsActuallyNewOrUpdated, issueNeedsReprocessing, shouldInsert, reprocessingRequested);
        logInfoIfRelevant(customer, parsedIssue, "customer={}, key={}, issueIsActuallyNewOrUpdated={}, issueNeedsReprocessing={}, shouldInsert={}, reprocessingRequested={}", customer, parsedIssue.getKey(), issueIsActuallyNewOrUpdated, issueNeedsReprocessing, shouldInsert, reprocessingRequested);

        if (!shouldInsert) {
            // if the issue has not changed, no need to update anything else
            return processingStatusBuilder.success(true).build();
        }

        if (DUPLICATE_STATUS_OVERLAP_TENANTS.contains(customer)) {
            log.info("customer {},  Check statuses overlap for issue {}", customer, parsedIssue.getKey());
            if (jiraIssueService.doIssueStatusOverlap(customer, parsedIssue)) {
                log.info("customer {}, {} issue has duplicate overlapping statuses", customer, parsedIssue.getKey());
                jiraIssueService.deleteAndUpdateStatuses(customer, parsedIssue);
                log.info("customer {}, status de-duping completed for issue {} ", customer, parsedIssue.getKey());
            }
        }

        // -- sprints
        if (StringUtils.isNotEmpty(parserConfig.getSprintFieldKey())) {
            try {
                DbJiraSprint.fromJiraIssue(issue, integrationId, parserConfig.getSprintFieldKey()).stream()
                        .filter(sprint -> sprint.getSprintId() != null &&
                                (!sprintIdCache.containsKey(sprint.getSprintId())
                                        || sprint.getUpdatedAt() > sprintIdCache.get(sprint.getSprintId())))
                        .forEach(sprint -> {
                            DbJiraSprint oldSprint = jiraIssueService.getSprint(customer, NumberUtils.toInteger(integrationId, null),
                                    sprint.getSprintId()).orElse(null);
                            if (oldSprint == null || sprint.getUpdatedAt() > oldSprint.getUpdatedAt()) {
                                // insert will automatically deduce by id
                                jiraIssueService.insertJiraSprint(customer, sprint);

                                // trying to save some DB calls, beyond that, we'll just query every time
                                if (sprintIdCache.size() < MAX_SPRINT_ID_CACHE_SIZE) {
                                    sprintIdCache.put(sprint.getSprintId(), sprint.getUpdatedAt());
                                }
                            }
                        });
            } catch (Exception e) {
                log.warn("Failed to insert sprints for Jira issue key={}", parsedIssue.getKey(), e);
                logErrorIfRelevant(customer, parsedIssue, "customer {}, key {}, Exception", customer, parsedIssue.getKey(), e);
            }
        }

        // -- sprint history
        SprintMappingEvents sprintMappingEvents = generateIssueSprintMappingsFromEvents(customer, integrationId, parsedIssue.getKey(), parsedIssue.getSprintEvents(), dbSprintLoadingCache);
        logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, issueSprintMappings size {}", customer, parsedIssue.getKey(), sprintMappingEvents.getIssueSprintMappings().size());
        handleSprintMappings(customer, parsedIssue, sprintMappingEvents, dbSprintLoadingCache, statusIdToStatusCategoryCache);
        logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, handleSprintMappings complete", customer, parsedIssue.getKey());

        // -- story points logs
        for (DbJiraStoryPoints item : ListUtils.emptyIfNull(parsedIssue.getStoryPointsLogs())) {
            try {
                storyPointsDatabaseService.upsert(customer, item);
            } catch (SQLException e) {
                log.warn("Failed to upsert story points log entry for company={}, issue={}", customer, issue.getKey(), e);
            }
        }

        // -- versions
        // As part of SEI-3577: stop aggregation of jira version from issues data source and do from project data source
        /*try {
            DbJiraVersion.fromJiraIssue(issue, integrationId)
                    .forEach(version -> jiraIssueService.insertJiraVersion(customer, version));
        } catch (Exception e) {
            log.warn("Failed to insert versions for Jira issue key={}", parsedIssue.getKey(), e);
        }*/

        // -- links
        if (CollectionUtils.isNotEmpty(issue.getFields().getIssueLinks())) {
            DbJiraIssue finalParsedIssue = parsedIssue;
            issue.getFields().getIssueLinks().forEach(jiraIssueLink -> {
                if (ObjectUtils.isNotEmpty(jiraIssueLink.getOutwardIssue())) {
                    jiraIssueService.insertJiraLinkedIssueRelation(customer, integrationId, finalParsedIssue.getKey(), jiraIssueLink.getOutwardIssue().getKey(), jiraIssueLink.getType().getOutward());
                } else if (ObjectUtils.isNotEmpty(jiraIssueLink.getInwardIssue())) {
                    jiraIssueService.insertJiraLinkedIssueRelation(customer, integrationId, finalParsedIssue.getKey(), jiraIssueLink.getInwardIssue().getKey(), jiraIssueLink.getType().getInward());
                }
            });
        }

        // -- populate children's "parent labels"
        bequeathLabelsToChildren(customer, integrationId, parsedIssue);

        // -- events, automation rules
        // only if issue is actually new or updated (i.e. for this ingested_at cycle) to avoid double events when retrying
        if (issueIsActuallyNewOrUpdated) {
            JiraIterativeScanQuery query = mapper.convertValue(jobDTO.getQuery(), JiraIterativeScanQuery.class);
            EventType eventType = null;
            Map<String, Object> eventData = null;
            try {
                // build event
                Set<String> customFields = getJiraCustomFieldsConfig(customer, integrationId);
                eventData = convertIssueToEventData(integrationId, productIds, customFields, Collections.emptyList(), issue);
                // call automation rule engine
                automationRulesEngine.scanWithRules(customer, ObjectType.JIRA_ISSUE, issue.getKey(), eventData);
                // determine event type
                eventType = determineEventType(customer, integrationId, parsedIssue.getKey(), query, jobDTO.getTags(), todayIssueIsNew);
                log.debug("Jira issue: determining event type for tenant={}, itId={}, key={}, createdAt={}, from={}, bw={} -> {}", customer, integrationId, parsedIssue.getKey(), issue.getFields().getCreated(), query.getFrom(), SetUtils.emptyIfNull(jobDTO.getTags()).contains(JobTags.BACKWARD_SCAN_TAG), eventType);
                if (eventType != null) {
                    eventsClient.emitEvent(customer, eventType, eventData);
                    processingStatusBuilder.eventSent(true);
                }
            } catch (InventoryException | EventsClientException e) {
                log.error("Error sending event for tenant={}, eventType={}, eventData={}", customer, eventType, eventData, e);
            }
        }

        return processingStatusBuilder.success(true).build();
    }

    /**
     * Checks feature flags and if enabled, return the labels of the parent issue (if available).
     */
    @VisibleForTesting
    protected List<String> getInheritedParentLabels(String tenantId, String integrationId, DbJiraIssue parsedIssue) {
        if (!parentLabelsTenants.contains(tenantId)) {
            return List.of();
        }

        // if "epics only" is enabled for this tenant, then only proceed if the parent is an epic
        boolean epicsOnly = parentLabelsTenantsEpicsOnly.contains(tenantId);
        boolean parentIssueIsEpic = EPIC_ISSUE_TYPE.equalsIgnoreCase(parsedIssue.getParentIssueType()) || StringUtils.isNotEmpty(parsedIssue.getEpic());
        log.debug("issue={}, epicsOnly={}, parentIssueIsEpic={}", parsedIssue.getKey(), epicsOnly, parentIssueIsEpic);
        if (epicsOnly && !parentIssueIsEpic) {
            return List.of();
        }

        // find parent and return its labels
        try {
            Optional<DbJiraIssue> parentOpt = Optional.empty();
            if (StringUtils.isNotEmpty(parsedIssue.getParentKey())) {
                parentOpt = jiraIssueService.get(tenantId, parsedIssue.getParentKey(), integrationId, parsedIssue.getIngestedAt());
            }
            if (parentOpt.isEmpty() && StringUtils.isNotEmpty(parsedIssue.getEpic())) {
                parentOpt = jiraIssueService.get(tenantId, parsedIssue.getEpic(), integrationId, parsedIssue.getIngestedAt());
            }
            return parentOpt
                    .map(DbJiraIssue::getLabels)
                    .orElse(List.of());
        } catch (SQLException e) {
            log.warn("Failed to inherit parent labels for tenant={}, integration={}, ingestedAt={}, issueKey={}, parentIssueKey={}",
                    tenantId, integrationId, parsedIssue.getIngestedAt(), parsedIssue.getKey(), parsedIssue.getParentKey(), e);
            return List.of();
        }
    }

    /**
     * Checks feature flags and if enabled, sets the parentLabels of all the children of this issue.
     */
    @VisibleForTesting
    protected void bequeathLabelsToChildren(String tenantId, String integrationId, DbJiraIssue parsedIssue) {
        if (!parentLabelsTenants.contains(tenantId)) {
            return;
        }

        // if "epics only" is enabled for this tenant, then only proceed if the current issue is an epic
        boolean epicsOnly = parentLabelsTenantsEpicsOnly.contains(tenantId);
        boolean currentIssueIsEpic = EPIC_ISSUE_TYPE.equalsIgnoreCase(parsedIssue.getIssueType());
        log.debug("issue={}, epicsOnly={}, currentIssueIsEpic={}", parsedIssue.getKey(), epicsOnly, currentIssueIsEpic);
        if (epicsOnly && !currentIssueIsEpic) {
            return;
        }

        // find children by parent link
        JiraIssuesFilter filterByParentLink = JiraIssuesFilter.builder()
                .integrationIds(List.of(integrationId))
                .ingestedAt(parsedIssue.getIngestedAt())
                .parentKeys(List.of(parsedIssue.getKey()))
                .build();
        Stream<DbJiraIssue> childrenStreamByParentLink = jiraIssueService.stream(tenantId, filterByParentLink, null, null, null, null, null);

        // find children by epic link
        Stream<DbJiraIssue> childrenStreamByEpic = Stream.empty();
        if (currentIssueIsEpic) {
            JiraIssuesFilter filterByEpicLink = JiraIssuesFilter.builder()
                    .integrationIds(List.of(integrationId))
                    .ingestedAt(parsedIssue.getIngestedAt())
                    .epics(List.of(parsedIssue.getKey()))
                    .build();
            childrenStreamByEpic = jiraIssueService.stream(tenantId, filterByEpicLink, null, null, null, null, null);
        }

        Stream<DbJiraIssue> childrenStream = Stream.concat(childrenStreamByParentLink, childrenStreamByEpic);

        // set children's parentLabels to this issue's labels
        MutableLong successCount = new MutableLong(0);
        MutableLong errorCount = new MutableLong(0);
        Set<String> processedChildrenKeys = new HashSet<>();
        childrenStream.forEach(child -> {
            if (processedChildrenKeys.contains(child.getKey())) {
                return;
            }
            processedChildrenKeys.add(child.getKey());
            try {
                jiraIssueService.insert(tenantId, child.toBuilder()
                        .parentLabels(parsedIssue.getLabels())
                        .build());
                successCount.increment();
            } catch (SQLException e) {
                log.warn("Failed to bequeath labels to child issue for tenant={}, integration={}, ingestedAt={}, issueKey={}, childIssueKey={}",
                        tenantId, integrationId, parsedIssue.getIngestedAt(), parsedIssue.getKey(), child.getKey(), e);
                errorCount.increment();
            }
        });
        log.debug("Updated {} child issues with parent labels ({} errors) for tenant={}, integration={}, ingestedAt={}, issueKey={}",
                successCount.longValue(), errorCount.longValue(), tenantId, integrationId, parsedIssue.getIngestedAt(), parsedIssue.getKey());
    }

    protected static boolean isReprocessingNeeded(@Nullable DbJiraIssue dbIssue, @Nullable Long currentConfigVersion) {
        if (currentConfigVersion == null) {
            return false; // when snapshotting is disabled, currentConfigVersion should never be null
        }
        if (dbIssue == null || dbIssue.getConfigVersion() == null) {
            return true; // if we didn't have the issue in the db or the version was missing, we will need to process it
        }
        // otherwise, only reprocess if the current version is newer than the issue's version
        return currentConfigVersion > dbIssue.getConfigVersion();
    }

    @Nonnull
    private DbJiraIssue populateDbJiraIssueUserIds(String company, String integrationId, JiraIssue jiraIssue, DbJiraIssue dbJiraIssue, LoadingCache<String, Optional<String>> userIdByDisplayNameCache, LoadingCache<String, Optional<String>> userIdByCloudIdCache, Boolean isEnableToMaskUser) {
        String reporterId = null;
        String assigneeId = null;
        String firstAssigneeId = null;
        if (StringUtils.isNotEmpty(dbJiraIssue.getReporter())) {
            if (jiraIssue.getFields().getReporter() != null) {
                JiraUser reporter = jiraIssue.getFields().getReporter();
                reporterId = getJiraUserId(company, integrationId, reporter, UNKNOWN, userIdByDisplayNameCache, userIdByCloudIdCache, isEnableToMaskUser).orElse(null);
            }
        }
        if (StringUtils.isNotEmpty(dbJiraIssue.getAssignee())) {
            if (jiraIssue.getFields().getAssignee() != null) {
                JiraUser assignee = jiraIssue.getFields().getAssignee();
                assigneeId = getJiraUserId(company, integrationId, assignee, UNASSIGNED, userIdByDisplayNameCache, userIdByCloudIdCache, isEnableToMaskUser).orElse(null);
            }
        }
        if (StringUtils.isNotEmpty(dbJiraIssue.getFirstAssignee())) {
            String firstAssignee = dbJiraIssue.getFirstAssignee();
            Optional<String> optFirstAssignee;
            try {
                optFirstAssignee = userIdByDisplayNameCache.get(firstAssignee);
            } catch (ExecutionException e) {
                log.warn("Failed to lookup user in cache", e);
                return null;
            }

            if (optFirstAssignee.isPresent() && !isEnableToMaskUser) {
                firstAssigneeId = optFirstAssignee.get();
            } else {
                try {
                    boolean isMasking = isToInsertOrUpdate(company, integrationId, firstAssignee, firstAssignee);
                    firstAssigneeId = insertJiraUser(company, integrationId, firstAssignee, firstAssignee, isMasking);
                } catch (SQLException e) {
                    log.error("Failed to lookup masking logic", e);
                }
            }
        }
        return dbJiraIssue.toBuilder()
                .reporterId(reporterId)
                .assigneeId(assigneeId)
                .firstAssigneeId(firstAssigneeId)
                .build();
    }

    private Optional<String> getJiraUserId(String company, String integrationId, JiraUser jiraUser, String defaultValue,
                                           LoadingCache<String, Optional<String>> userIdByDisplayNameCache,
                                           LoadingCache<String, Optional<String>> userIdByCloudIdCache, Boolean isEnableToMaskUser) {
        String userCloudId;
        String userId = null;
        try {
            // ideally, we treat accountId as cloudId, but we try to use the display name as backup
            if (jiraUser.getAccountId() != null) {
                userCloudId = jiraUser.getAccountId();
                userId = userIdByCloudIdCache.get(userCloudId).orElse(null);
            } else if (jiraUser.getName() != null) {
                userCloudId = jiraUser.getName();
                userId = userIdByDisplayNameCache.get(userCloudId).orElse(null);
            } else {
                userCloudId = defaultValue;
            }
        } catch (ExecutionException e) {
            log.warn("Failed to lookup user in cache", e);
            return Optional.empty();
        }
        try {
            boolean isMasking = isToInsertOrUpdate(company, integrationId, userCloudId, jiraUser.getDisplayName());
            if (userId == null && !StringUtils.equals(userCloudId, defaultValue)) {

                userId = insertJiraUser(company, integrationId, userCloudId, jiraUser.getDisplayName(), isMasking);

                // since we just inserted a user, let's invalidate the caches:
                userIdByCloudIdCache.invalidate(userCloudId);
                if (StringUtils.isNotEmpty(jiraUser.getDisplayName())) {
                    userIdByDisplayNameCache.invalidate(jiraUser.getDisplayName());
                }
            } else if (isEnableToMaskUser) {
                userId = insertJiraUser(company, integrationId, userCloudId, jiraUser.getDisplayName(), isMasking);
                // since we just inserted a user, let's invalidate the caches:
                userIdByCloudIdCache.invalidate(userCloudId);
                if (StringUtils.isNotEmpty(jiraUser.getDisplayName())) {
                    userIdByDisplayNameCache.invalidate(jiraUser.getDisplayName());
                }
            }

        } catch (SQLException e) {
            log.error("Failed to lookup masking logic");

        }
        return Optional.ofNullable(userId);
    }

    private boolean isToInsertOrUpdate(String company, String integrationId, String userCloudId, String displayName) throws SQLException {
        boolean isMasking = userIdentityMaskingService.isMasking(company, integrationId, userCloudId, displayName);
        return isMasking;

    }

    private String insertJiraUser(String company, String integrationId, String cloudId, String originalDisplayName, boolean isMasked) {
        String userId = null;
        String maskedDisplayName = null;
        try {
            if (isMasked) {
                maskedDisplayName = userIdentityMaskingService.maskedUser(company);
            } else {
                maskedDisplayName = originalDisplayName;
            }

            userId = userIdentityService.upsertIgnoreEmail(company,
                    DbScmUser.builder()
                            .integrationId(integrationId)
                            .cloudId(cloudId)
                            .displayName(maskedDisplayName)
                            .originalDisplayName(originalDisplayName)
                            .build());
        } catch (SQLException e) {
            log.error("Failed to insert jira user={} for integration={}", cloudId, integrationId);
        }
        return userId;
    }

    public Optional<String> getUserIdByDisplayName(String company, String integrationId, String displayName) {
        return userIdentityService.getUserByOriginalDisplayName(company, integrationId, displayName);
    }

    public Optional<String> getUserIdByCloudId(String company, String integrationId, String cloudId) {
        return Optional.ofNullable(userIdentityService.getUser(company, integrationId, cloudId));
    }

    public Optional<DbJiraSprint> findDbSprintById(String customer, String integrationId, String sprintId) {
        if (StringUtils.isBlank(sprintId)) {
            return Optional.empty();
        }
        try {
            return jiraIssueService.getSprint(customer, Integer.parseInt(integrationId), Integer.parseInt(sprintId));
        } catch (Exception e) {
            log.warn("Failed to lookup dbSprint by id", e);
            return Optional.empty();
        }
    }

    public Optional<String> findStatusCategoryByStatusId(String customer, String integrationId, String statusId) {
        if (StringUtils.isBlank(statusId)) {
            return Optional.empty();
        }
        try {
            return statusMetadataDatabaseService.getByStatusId(customer, integrationId, statusId)
                    .map(DbJiraStatusMetadata::getStatusCategory);
        } catch (Exception e) {
            log.warn("Failed to lookup status category by status id", e);
            return Optional.empty();
        }
    }

    @lombok.Value
    @Builder(toBuilder = true)
    public static class SprintMappingEvents {
        /**
         * List of "Added At" timestamps (see {@link JiraAggHelperService#findSprintAddedAtEventTime(String, String, String, String, List, LoadingCache)} per sprint & issue.
         */
        @Nonnull
        List<DbJiraIssueSprintMapping> issueSprintMappings;

        /**
         * List of sprint ids where the issue should not be part of any sprint report.
         * We need to keep track of this to clean up previously inserted sprint metrics (in case sprint dates changes for example, see PROP-2668)
         */
        @Nonnull
        List<String> sprintIdsToExcludeFromSprintMetrics;
    }

    @Nonnull
    protected SprintMappingEvents generateIssueSprintMappingsFromEvents(String customer, String integrationId, String issueKey, Map<String, List<DbJiraIssue.JiraIssueSprintEvent>> sprintEvents, LoadingCache<String, Optional<DbJiraSprint>> dbSprintLoadingCache) {
        if (MapUtils.isEmpty(sprintEvents)) {
            return SprintMappingEvents.builder()
                    .issueSprintMappings(List.of())
                    .sprintIdsToExcludeFromSprintMetrics(List.of())
                    .build();
        }

        List<DbJiraIssueSprintMapping> issueSprintMappings = new ArrayList<>();
        List<String> sprintIdsToExclude = new ArrayList<>();

        for (String sprintId : sprintEvents.keySet()) {
            if (StringUtils.isBlank(sprintId)) {
                log.debug("Ignoring sprint with blank id");
                continue;
            }
            Long addedAt = findSprintAddedAtEventTime(customer, integrationId, issueKey, sprintId, ListUtils.emptyIfNull(sprintEvents.get(sprintId)), dbSprintLoadingCache);
            if (addedAt == null) {
                // If there are events for that sprint, but we could not find a valid addedAt date,
                // then the issue must be excluded from this sprint report.
                // (for example, an issue is added then removed from the sprint before it starts, see PROP-2668)
                sprintIdsToExclude.add(sprintId);
                continue;
            }

            Boolean removedMidSprint = isRemovedFromActiveSprint(customer, integrationId, issueKey, sprintId, ListUtils.emptyIfNull(sprintEvents.get(sprintId)), dbSprintLoadingCache);

            issueSprintMappings.add(DbJiraIssueSprintMapping.builder()
                    .integrationId(integrationId)
                    .issueKey(issueKey)
                    .sprintId(sprintId)
                    .addedAt(addedAt)
                    .removedMidSprint(removedMidSprint)
                    .build());
        }

        return SprintMappingEvents.builder()
                .issueSprintMappings(issueSprintMappings)
                .sprintIdsToExcludeFromSprintMetrics(sprintIdsToExclude)
                .build();
    }

    /**
     * Find the "Added At" timestamp which is when an issue is added to a sprint according to the sprint metrics definition:
     * - the **last time** it got added to the sprint **before** the sprint started (unless it also got removed before the sprint start)
     * - otherwise, the **first time** it got added to sprint **after** the sprint started
     */
    @Nullable
    protected static Long findSprintAddedAtEventTime(String customer, String integrationId, String issueKey, @Nonnull String sprintId, List<DbJiraIssue.JiraIssueSprintEvent> sprintEvents, LoadingCache<String, Optional<DbJiraSprint>> dbSprintLoadingCache) {
        // -- find out sprint dates
        DbJiraSprint dbSprint = null;
        try {
            dbSprint = dbSprintLoadingCache.get(sprintId).orElse(null);
        } catch (ExecutionException e) {
            log.warn("Failed to load dbSprint for customer={}, integrationId={}, sprintId={}", customer, integrationId, sprintId, e);
        }
        if (dbSprint == null) {
            log.debug("Could not handle sprint events for company={}, integration_id={}, issue={}: sprint log without dbSprint for sprintId={}", customer, integrationId, issueKey, sprintId);
            return null;
        }

        // All the dbSprint dates are in milliseconds -> convert to seconds
        Long startDate = DateUtils.toEpochSecond(dbSprint.getStartDate());
        if (startDate == null) {
            log.debug("Could not handle sprint events for company={}, integration_id={}, issue={}: sprint without start date for sprintId={}", customer, integrationId, issueKey, sprintId);
            return null;
        }

        // -- parse events

        // find out the last event before the sprint start
        Optional<DbJiraIssue.JiraIssueSprintEvent> lastEventBeforeSprintStart = sprintEvents.stream()
                .filter(Objects::nonNull)
                .filter(event -> event.getStartTime() != null && event.getEndTime() != null)
                .filter(event -> event.getStartTime() < startDate && event.getEndTime() >= startDate)
                .reduce((a, b) -> b);

        // if the last event before start is an ADD, then use it as the addedAt time
        if (lastEventBeforeSprintStart.isPresent() && ADDED.equals(lastEventBeforeSprintStart.get().getEventType())) {
            return lastEventBeforeSprintStart.get().getStartTime();
        }

        // otherwise, use the first ADD after the sprint is started
        Optional<DbJiraIssue.JiraIssueSprintEvent> firstAddEventAfterSprintStart = sprintEvents.stream()
                .filter(Objects::nonNull)
                .filter(event -> event.getStartTime() != null && event.getStartTime() >= startDate)
                .filter(event -> ADDED.equals(event.getEventType()))
                .findFirst();
        if (firstAddEventAfterSprintStart.isPresent()) {
            return firstAddEventAfterSprintStart.get().getStartTime();
        }

        return null;
    }

    /**
     * Find the last event between sprint start and end time and look for the last event from that
     * - if it is REMOVED then this method will return true
     * - For rest of the cases it will return false
     */
    protected static Boolean isRemovedFromActiveSprint(String customer,
                                                       String integrationId,
                                                       String issueKey,
                                                       @Nonnull String sprintId,
                                                       List<DbJiraIssue.JiraIssueSprintEvent> sprintEvents,
                                                       LoadingCache<String, Optional<DbJiraSprint>> dbSprintLoadingCache) {
        // -- find out sprint dates
        DbJiraSprint dbSprint = null;
        try {
            dbSprint = dbSprintLoadingCache.get(sprintId).orElse(null);
        } catch (ExecutionException e) {
            log.warn("Failed to load dbSprint for customer={}, integrationId={}, sprintId={}", customer, integrationId, sprintId, e);
        }
        if (dbSprint == null) {
            log.debug("Could not handle sprint events for company={}, integration_id={}, issue={}: sprint log without dbSprint for sprintId={}", customer, integrationId, issueKey, sprintId);
            return false;
        }

        // All the dbSprint dates are in milliseconds -> convert to seconds
        Long startDate = DateUtils.toEpochSecond(dbSprint.getStartDate());
        Long endDate = DateUtils.toEpochSecond(dbSprint.getEndDate());
        if (startDate == null || endDate == null) {
            log.debug("Could not handle sprint events for company={}, integration_id={}, issue={}: sprint without start date for sprintId={}", customer, integrationId, issueKey, sprintId);
            return false;
        }

        // -- parse events

        // find out the events between sprint
        Optional<DbJiraIssue.JiraIssueSprintEvent> lastEventBetweenSprint = sprintEvents.stream()
                .filter(Objects::nonNull)
                .filter(event -> event.getStartTime() != null)
                .filter(event -> event.getStartTime() >= startDate && event.getStartTime() < endDate)
                .reduce((a, b) -> b);

        // if the last event in between the sprint is REMOVED, then use it is true, otherwise it will be false
        return lastEventBetweenSprint.isPresent() && REMOVED.equals(lastEventBetweenSprint.get().getEventType());
    }

    public void processJiraSprint(String customer, String integrationId, JiraSprint sprint, Long updatedAt) {
        DbJiraSprint dbJiraSprint = DbJiraSprint.fromJiraSprint(sprint, integrationId, updatedAt);
        DbJiraSprint oldSprint = jiraIssueService.getSprint(customer, NumberUtils.toInteger(integrationId, null),
                NumberUtils.toInteger(sprint.getId(), null)).orElse(null);
        if (oldSprint == null || dbJiraSprint.getUpdatedAt() > oldSprint.getUpdatedAt()) {
            jiraIssueService.insertJiraSprint(customer, dbJiraSprint);
        }
    }

    private boolean isRelevant(String customer, DbJiraIssue parsedIssue) {
        return RELEVANT_TENANT_IDS.contains(customer) && RELEVANT_ISSUE_KEYS.contains(parsedIssue.getKey());
    }

    protected void logInfoIfRelevant(String customer, DbJiraIssue parsedIssue, String message, Object... args) {
        if (isRelevant(customer, parsedIssue)) {
            log.info(message, args);
        }
    }

    private void logErrorIfRelevant(String customer, DbJiraIssue parsedIssue, String message, Object... args) {
        if (isRelevant(customer, parsedIssue)) {
            log.error(message, args);
        }
    }

    protected void handleSprintMappings(String customer, DbJiraIssue parsedIssue, SprintMappingEvents sprintMappingEvents, LoadingCache<String, Optional<DbJiraSprint>> dbSprintLoadingCache, LoadingCache<String, Optional<String>> statusIdToStatusCategoryCache) {
        cleanUpExcludedSprints(customer, parsedIssue, sprintMappingEvents.getSprintIdsToExcludeFromSprintMetrics());
        upsertSprintMappings(customer, parsedIssue, sprintMappingEvents.getIssueSprintMappings(), dbSprintLoadingCache, statusIdToStatusCategoryCache);
    }

    protected void cleanUpExcludedSprints(String customer, DbJiraIssue parsedIssue, List<String> sprintIdsToExclude) {
        if (CollectionUtils.isEmpty(sprintIdsToExclude)) {
            // if we have nothing to exclude, we don't want to delete anything
            return;
        }
        Validate.notBlank(customer, "customer cannot be null or empty.");
        Validate.notNull(parsedIssue, "parsedIssue cannot be null.");
        Validate.notBlank(parsedIssue.getIntegrationId(), "parsedIssue.getIntegrationId() cannot be null or empty.");
        Validate.notBlank(parsedIssue.getKey(), "parsedIssue.getKey() cannot be null or empty.");

        log.debug("Cleaning up sprint mappings for issue_key={}, sprints={}", parsedIssue.getKey(), sprintIdsToExclude);
        List<String> mappingIdsToDelete = sprintMappingDatabaseService.stream(customer,
                        JiraIssueSprintMappingDatabaseService.JiraIssueSprintMappingFilter.builder()
                                .integrationIds(List.of(parsedIssue.getIntegrationId()))
                                .issueKey(parsedIssue.getKey())
                                .sprintIds(sprintIdsToExclude)
                                .build())
                .map(DbJiraIssueSprintMapping::getId)
                .collect(Collectors.toList());

        log.debug("Mappings to delete: {}", mappingIdsToDelete);
        mappingIdsToDelete.forEach(id -> {
            try {
                sprintMappingDatabaseService.delete(customer, id);
            } catch (SQLException e) {
                log.error("Failed to delete sprint mapping id={}", id, e);
            }
        });
    }

    protected void upsertSprintMappings(String customer, DbJiraIssue parsedIssue, List<DbJiraIssueSprintMapping> issueSprintMappings, LoadingCache<String, Optional<DbJiraSprint>> dbSprintLoadingCache, LoadingCache<String, Optional<String>> statusIdToStatusCategoryCache) {
        if (CollectionUtils.isEmpty(issueSprintMappings)) {
            logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, issueSprintMappings is empty", customer, parsedIssue.getKey());
            return;
        }

        // for now, only sub task, but there could be more
        boolean ignorableTaskType = SUB_TASK_ISSUE_TYPE.equalsIgnoreCase(parsedIssue.getIssueType());
        logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, ignorableTaskType {}", customer, parsedIssue.getKey(), ignorableTaskType);

        for (DbJiraIssueSprintMapping sprintMapping : issueSprintMappings) {
            if (StringUtils.isBlank(sprintMapping.getSprintId())) {
                log.debug("Ignoring sprint with blank id");
                continue;
            }
            logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, sprintId {}", customer, parsedIssue.getKey(), sprintMapping.getSprintId());

            // -- find out sprint dates
            DbJiraSprint dbSprint = null;
            try {
                dbSprint = dbSprintLoadingCache.get(sprintMapping.getSprintId()).orElse(null);
                logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, dbSprint {}", customer, parsedIssue.getKey(), dbSprint);
            } catch (ExecutionException e) {
                log.warn("Failed to load dbSprint for customer={}, integrationId={}, sprintId={}", customer, parsedIssue.getIntegrationId(), sprintMapping.getSprintId(), e);
                logErrorIfRelevant(customer, parsedIssue, "customer {}, key {}, ExecutionException", customer, parsedIssue.getKey(), e);
            }
            if (dbSprint == null) {
                log.debug("Could not handle sprint mapping for company={}, integration_id={}, issue={}: sprint log without dbSprint for sprintId={}", customer, parsedIssue.getIntegrationId(), parsedIssue.getKey(), sprintMapping.getSprintId());
                logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, dbSprint = null", customer, parsedIssue.getKey());
                continue;
            }
            // All the dbSprint dates are in milliseconds -> convert to seconds
            Long startDate = DateUtils.toEpochSecond(dbSprint.getStartDate());
            Long endDate = DateUtils.toEpochSecond(dbSprint.getEndDate());
            Long completedDate = DateUtils.toEpochSecond(dbSprint.getCompletedDate());
            logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, startDate {}, endDate {}, initial completedDate {}", customer, parsedIssue.getKey(), startDate, endDate, completedDate);
            completedDate = (completedDate != null) ? completedDate : endDate;
            logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, startDate {}, endDate {}, effective completedDate {}", customer, parsedIssue.getKey(), startDate, endDate, completedDate);

            // > was it planned? (added_at < sprint.start_time)
            boolean planned = false;
            boolean outsideOfSprint = false; // means *completed* outside of sprint
            if (sprintMapping.getAddedAt() != null && startDate != null) {
                if (parsedIssue.getIssueResolvedAt() != null) {
                    // explicitly marking it as outside-of-sprint but it will anyways qualify
                    // to be marked so based on its resolved status start time
                    outsideOfSprint = parsedIssue.getIssueResolvedAt() < startDate;
                    planned = !outsideOfSprint;
                } else {
                    planned = sprintMapping.getAddedAt() < startDate;
                }
            }
            logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, startDate {}, sprintMapping.getAddedAt() {}, planned {}", customer, parsedIssue.getKey(), startDate, sprintMapping.getAddedAt(), planned);

            // > was it delivered? (at sprint.end_time, issue was completed using status category)
            boolean delivered = false;
            if (completedDate != null) {
                logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, completedDate != null", customer, parsedIssue.getKey());

                for (DbJiraStatus status : ListUtils.emptyIfNull(parsedIssue.getStatuses())) {
                    Long statusStartTime = status.getStartTime();
                    Long statusEndTime = status.getEndTime();
                    logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, statusStartTime {}, statusEndTime {}", customer, parsedIssue.getKey(), statusStartTime, statusEndTime);
                    if (statusStartTime == null || statusEndTime == null) {
                        logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, statusStartTime == null or statusEndTime == null", customer, parsedIssue.getKey());
                        continue;
                    }
                    // find status category
                    String statusCategory = null;
                    if (StringUtils.isNotBlank(status.getStatusId())) {
                        try {
                            statusCategory = statusIdToStatusCategoryCache.get(status.getStatusId()).orElse(null);
                            logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, statusId {}, statusCategory {}", customer, parsedIssue.getKey(), status.getStatusId(), statusCategory);
                        } catch (ExecutionException e) {
                            log.warn("Failed to lookup status category from cache", e);
                            logErrorIfRelevant(customer, parsedIssue, "customer {}, key {}, statusId {}, ExecutionException", customer, parsedIssue.getKey(), status.getStatusId(), e);
                        }
                    } else {
                        log.debug("customer {}, key {}: statusId was empty", customer, parsedIssue.getKey());
                    }
                    // ignore incomplete statuses
                    if (!DONE_STATUS_CATEGORY.equalsIgnoreCase(statusCategory)) {
                        logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, statusCategory {} is not done status category", customer, parsedIssue.getKey(), statusCategory);
                        continue;
                    }
                    // if the sprint was completed within the bounds of a completed status,
                    // consider the issue delivered during this sprint
                    if (completedDate >= statusStartTime && completedDate < statusEndTime) {
                        delivered = true;
                        logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, statusStartTime {}, statusEndTime {}, completedDate {}, delivered = true", customer, parsedIssue.getKey(), statusStartTime, statusEndTime, completedDate);
                        if (startDate != null && statusStartTime < startDate) {
                            outsideOfSprint = true;
                        }
                        break;
                    }
                }
            }
            logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, effective delivered = {}", customer, parsedIssue.getKey(), delivered);

            // > story points at the start and the end of the sprint
            int storyPointsPlanned = 0;
            int storyPointsDelivered = 0;
            if (startDate != null && completedDate != null) {
                for (DbJiraStoryPoints storyPointsLog : ListUtils.emptyIfNull(parsedIssue.getStoryPointsLogs())) {
                    int storyPoints = MoreObjects.firstNonNull(storyPointsLog.getStoryPoints(), 0);
                    Long storyPointsLogStartTime = storyPointsLog.getStartTime();
                    Long storyPointsLogEndTime = storyPointsLog.getEndTime();
                    if (storyPointsLogStartTime == null || storyPointsLogEndTime == null) {
                        continue;
                    }
                    if (planned) {
                        if (startDate >= storyPointsLogStartTime && startDate < storyPointsLogEndTime) {
                            storyPointsPlanned = storyPoints;
                        }
                    } else {
                        if (sprintMapping.getAddedAt() >= storyPointsLogStartTime && sprintMapping.getAddedAt() < storyPointsLogEndTime) {
                            storyPointsPlanned = storyPoints;
                        }
                    }
                    if (completedDate >= storyPointsLogStartTime && completedDate < storyPointsLogEndTime) {
                        storyPointsDelivered = storyPoints;
                    }
                }

            }

            log.debug("sprint mapping: customer={}, integrationId={}, issueKey={}, sprintId={}, added_at={}, start={}, end={} - planned={}, delivered={}. outsideOfSprint={}, ignorable={}, storyPointsPlanned={}, storyPointsDelivered={}",
                    customer, parsedIssue.getIntegrationId(), parsedIssue.getKey(), sprintMapping.getSprintId(), sprintMapping.getAddedAt(), startDate, endDate, planned, delivered, outsideOfSprint, ignorableTaskType, storyPointsPlanned, storyPointsDelivered);

            // -- insert sprint mapping
            DbJiraIssueSprintMapping updatedSprintMapping = sprintMapping.toBuilder()
                    .planned(planned)
                    .delivered(delivered)
                    .outsideOfSprint(outsideOfSprint)
                    .ignorableIssueType(ignorableTaskType)
                    .storyPointsPlanned(storyPointsPlanned)
                    .storyPointsDelivered(storyPointsDelivered)
                    .build();
            try {
                logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, delivered = {}, before sprint mapping update", customer, parsedIssue.getKey(), updatedSprintMapping.getDelivered());
                sprintMappingDatabaseService.upsert(customer, updatedSprintMapping);
                logInfoIfRelevant(customer, parsedIssue, "customer {}, key {}, delivered = {}, after sprint mapping update", customer, parsedIssue.getKey(), updatedSprintMapping.getDelivered());
            } catch (SQLException e) {
                log.warn("Failed to insert sprint mapping for customer={}: {}", customer, updatedSprintMapping, e);
                logErrorIfRelevant(customer, parsedIssue, "customer {}, key {}, SQLException", customer, parsedIssue.getKey(), e);
            }
        }
    }

    private Map<String, Object> convertIssueToEventData(String integrationId, List<String> productIds, Set<String> customFields,
                                                        List<String> runbookIds, JiraIssue issue) {
        Optional<JiraIssueFields> fieldsOpt = Optional.ofNullable(issue.getFields());
        var data = new HashMap<String, Object>(); // couldn't use Map.of() with such large map
        data.put("id", issue.getKey());
        data.put("runbook_ids", runbookIds); // to select specific runbooks
        data.put("integration_id", integrationId);
        data.put("title", fieldsOpt.map(JiraIssueFields::getSummary).orElse(""));
        data.put("author", fieldsOpt.map(JiraIssueFields::getCreator).map(JiraUser::getName).orElse(""));
        data.put("body", fieldsOpt.map(JiraIssueFields::getDescriptionText).orElse(""));
        List<JiraComment> comments = Optional.of(fieldsOpt.map(JiraIssueFields::getComment).map(JiraCommentsResult::getComments)).get().orElse(List.of());
        data.put("comments", comments.stream().filter(Objects::nonNull).map(JiraComment::getBodyText).collect(Collectors.joining()));
        data.put("jira_project", fieldsOpt.map(JiraIssueFields::getProject).map(JiraProject::getKey).orElse(""));
        data.put("created_at", fieldsOpt.map(JiraIssueFields::getCreated).map(x -> x.toInstant().toEpochMilli()).orElse(0L));
        data.put("updated_at", fieldsOpt.map(JiraIssueFields::getUpdated).map(x -> x.toInstant().toEpochMilli()).orElse(0L));
        data.put("issue_type", fieldsOpt.map(JiraIssueFields::getIssueType).map(JiraIssueType::getName).orElse(""));
        data.put("priority", fieldsOpt.map(JiraIssueFields::getPriority).map(JiraIssueFields.JiraPriority::getName).orElse(""));
        data.put("status", fieldsOpt.map(JiraIssueFields::getStatus).map(JiraIssueFields.JiraStatus::getName).orElse(""));
        data.put("resolution", fieldsOpt.map(JiraIssueFields::getResolution).map(JiraIssueFields.JiraIssueResolution::getName).orElse(""));
        data.put("products", productIds);
        data.put("custom_fields", extractCustomFieldsFromIssue(issue, integrationId, customFields));
        data.put("fix_versions", fieldsOpt.map(JiraIssueFields::getFixVersions).orElse(List.of())
                .stream().map(JiraVersion::getName).collect(Collectors.toList()));
        data.put("versions", fieldsOpt.map(JiraIssueFields::getVersions).orElse(List.of())
                .stream().map(JiraVersion::getName).collect(Collectors.toList()));
        data.put("components", fieldsOpt.map(JiraIssueFields::getComponents).orElse(List.of())
                .stream().map(JiraComponent::getName).collect(Collectors.toList()));
        data.put("labels", fieldsOpt.map(JiraIssueFields::getLabels).orElse(List.of()));
        return data;
    }

    private Map<String, Object> extractCustomFieldsFromIssue(JiraIssue issue, String integrationId, Set<String> customFields) {
        return Map.of(
                "integration_id", integrationId,
                "custom_fields", MapUtils.emptyIfNull(issue.getFields().getDynamicFields()).entrySet().stream()
                        .filter(entry -> customFields.contains(entry.getKey()))
                        .map(entry -> {
                            String value = JiraCustomFieldConverter.parseValue(entry.getValue());
                            return (entry.getKey() == null || value == null) ? null : Map.of(
                                    "key", entry.getKey(),
                                    "value", value);
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
    }

    private Set<String> getJiraCustomFieldsConfig(String company, String integrationId) throws InventoryException {
        IntegrationConfig config = inventoryService.listConfigs(company, List.of(integrationId), 0, 1)
                .getRecords().stream().findFirst().orElse(null);
        if (config == null || config.getConfig() == null) {
            return Collections.emptySet();
        }
        List<IntegrationConfig.ConfigEntry> customFieldConfigs = config.getConfig().get("agg_custom_fields");
        return ListUtils.emptyIfNull(customFieldConfigs).stream()
                .map(IntegrationConfig.ConfigEntry::getKey)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    @Nullable
    private EventType determineEventType(String company, String integrationId, String parsedIssueKey, JiraIterativeScanQuery query, Set<String> jobTags, boolean todayIssueIsNew) {
        //We come here only if issue was new or updated today
        if (query.getFrom() == null || SetUtils.emptyIfNull(jobTags).contains(JobTags.BACKWARD_SCAN_TAG)) {
            // ignore historic scan
            return null;
        }

        // Today issue was new, next check if issues was seen in previous snapshots
        // there should only be 1 across all snapshots (the one just newly inserted
        /*
        Today, issue was updated => trulyNewJiraIssue is false
        Today, issue is new but we have seen issue in prev snapshots => trulyNewJiraIssue is false
        Today, issue is new and we have NOT seen issue in prev snapshots => trulyNewJiraIssue is true
         */
        boolean trulyNewJiraIssue = todayIssueIsNew && isIssueNewAcrossSnapshots(company, integrationId, parsedIssueKey);

        if (trulyNewJiraIssue) {
            return EventType.JIRA_ISSUE_CREATED;
        } else {
            // we don't do anything with update events so far, so putting them behind a feature flag
            return sendJiraUpdateEvents ? EventType.JIRA_ISSUE_UPDATED : null;
        }
    }

    private boolean isIssueNewAcrossSnapshots(String company, String integrationId, String key) {
        if (StringUtils.isEmpty(key)) {
            return false;
        }
        try {
            DbListResponse<DbJiraIssue> response = jiraIssueService.list(company, JiraSprintFilter.builder().build(), JiraIssuesFilter.builder()
                    .integrationIds(List.of(integrationId))
                    .keys(List.of(key))
                    .build(), null, Map.of(), 0, 2);
            log.debug("Jira issue lookup for company={}, itId={}, key='{}': {} -> {}", company, integrationId, key, ListUtils.emptyIfNull(response.getRecords()).stream()
                    .map(issue -> String.format("itId=%s, key=%s, ingAt=%s", issue.getIntegrationId(), issue.getKey(), issue.getIngestedAt()))
                    .collect(Collectors.toList()), CollectionUtils.size(response.getRecords()) <= 1 ? "NEW" : "NOT NEW");
            return CollectionUtils.size(response.getRecords()) <= 1; // that 'one' issue is the one that was just inserted during this cycle
        } catch (SQLException e) {
            log.warn("Failed to list jira issues", e);
            return false;
        }
    }
}


