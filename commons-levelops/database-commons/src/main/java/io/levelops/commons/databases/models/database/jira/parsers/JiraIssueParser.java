package io.levelops.commons.databases.models.database.jira.parsers;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraAssignee;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue.JiraIssueSprintEvent;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue.JiraIssueSprintEvent.JiraIssueSprintEventBuilder;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.jira.DbJiraStoryPoints;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.integrations.jira.converters.JiraCustomFieldConverter;
import io.levelops.integrations.jira.models.JiraComponent;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraIssueChangeLog;
import io.levelops.integrations.jira.models.JiraIssueFields;
import io.levelops.integrations.jira.models.JiraIssueType;
import io.levelops.integrations.jira.models.JiraVersion;
import io.levelops.integrations.jira.utils.JiraUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.util.TriConsumer;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.database.jira.DbJiraIssue.DEFAULT_STORY_POINTS_LOG;
import static io.levelops.commons.databases.models.database.jira.DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.ADDED;
import static io.levelops.commons.databases.models.database.jira.DbJiraIssue.JiraIssueSprintEvent.JiraSprintEventType.REMOVED;
import static io.levelops.commons.databases.models.database.jira.DbJiraIssue.UNASSIGNED;
import static io.levelops.commons.databases.models.database.jira.DbJiraIssue.UNKNOWN;
import static io.levelops.commons.databases.models.database.jira.DbJiraIssue.UNPRIORITIZED;

/**
 * Parses an API JiraIssue into a DbJiraIssue.
 * Note: Ideally, we should remove any dependencies on API models from this module, and move this class to a new module.
 */
@Log4j2
public class JiraIssueParser {

    protected static final Comparator<JiraIssueChangeLog.ChangeLogEvent> DESC_CHANGE_LOG_EVENT_COMPARATOR = (o1, o2) ->
            (o1.getCreated() != null && o2.getCreated() != null) ?
                    (int) (o2.getCreated().toInstant().getEpochSecond()
                            - o1.getCreated().toInstant().getEpochSecond()) : 0;

    private static final String STANDARD_DUE_DATE_FIELD = "duedate";

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = JiraParserConfig.JiraParserConfigBuilder.class)
    public static class JiraParserConfig {
        @Nullable
        String epicLinkField;
        @Nullable
        String storyPointsField;
        @Nullable
        String sprintFieldKey;
        @Nullable
        String sprintFieldName;
        @Nullable
        String dueDateField;
        @Nullable
        List<IntegrationConfig.ConfigEntry> customFieldConfig;
        @Nullable
        List<IntegrationConfig.ConfigEntry> salesforceConfig;
        @Nullable
        List<DbJiraField> customFieldProperties;
    }

    @SuppressWarnings("unchecked")
    public static DbJiraIssue parseJiraIssue(JiraIssue jiraIssue,
                                             String integrationId,
                                             Date fetchTime,
                                             @Nullable JiraParserConfig config) {
        Validate.notNull(jiraIssue, "jiraIssue cannot be null.");
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        Validate.notNull(fetchTime, "fetchTime cannot be null.");
        config = config != null ? config : JiraParserConfig.builder().build();

        Date truncatedDate = DateUtils.truncate(fetchTime, Calendar.DATE);
        log.debug("truncatedDate = {}", truncatedDate);

        // --- ISSUE KEY
        String issueKey = jiraIssue.getKey().toUpperCase();

        // --- DUE DATE
        Long dueDate = parseDueDate(jiraIssue, config.getDueDateField());
        log.debug("dueDate = {}", dueDate);

        // --- CURRENT ASSIGNEE
        String currentAssignee = UNASSIGNED;
        String oldestAssignee = UNASSIGNED;
        if (jiraIssue.getFields().getAssignee() != null) {
            currentAssignee = jiraIssue.getFields().getAssignee().getDisplayName();
            oldestAssignee = currentAssignee;
        }
        Long assigneeTimeIterator = fetchTime.toInstant().getEpochSecond();
        log.debug("currentAssignee = {}", currentAssignee);

        // --- CURRENT STATUS
        String currentStatus = jiraIssue.getFields().getStatus().getName().toUpperCase();
        String currentStatusId = jiraIssue.getFields().getStatus().getId();
        String oldestStatus = currentStatus; // start with current status, and move back when parsing the change log
        String oldestStatusId = currentStatusId;
        log.debug("currentStatus = {}, assigneeTimeIterator = {}", currentStatus, assigneeTimeIterator);

        // --- CHANGELOG
        ParsedChangelog parsedChangelog = parseChangelog(integrationId, jiraIssue, oldestStatus, oldestStatusId, oldestAssignee, assigneeTimeIterator, config.getStoryPointsField(), config.getSprintFieldKey(), config.getSprintFieldName());
        int bounces = parsedChangelog.getBounces();
        int hops = parsedChangelog.getHops();
        oldestStatus = parsedChangelog.getOldestStatus();
        oldestStatusId = parsedChangelog.getOldestStatusId();
        List<JiraLogItem> statusLogs = parsedChangelog.getStatusLogs();
        Set<String> assigneeSet = parsedChangelog.getAssigneeSet();
        List<DbJiraAssignee> assigneesList = parsedChangelog.getAssigneesList();
        oldestAssignee = parsedChangelog.getOldestAssignee();
        assigneeTimeIterator = parsedChangelog.getAssigneeTimeIterator();
        String oldIssueKey = parsedChangelog.getOldIssueKey();

        Long issueCreatedAt = jiraIssue.getFields().getCreated().toInstant().getEpochSecond();
        log.debug("issueCreatedAt = {}", issueCreatedAt);

        // region --- CURRENT ASSIGNEE
        if (!UNASSIGNED.equals(oldestAssignee)) {
            hops += 1;
            if (assigneeSet.contains(oldestAssignee)) {
                bounces += 1;
            }
        }
        DbJiraAssignee dbJiraAssignee = DbJiraAssignee.builder()
                .integrationId(integrationId)
                .assignee(oldestAssignee)
                .startTime(issueCreatedAt)
                .endTime(assigneeTimeIterator)
                .issueKey(issueKey)
                .build();
        log.debug("dbJiraAssignee = {}", dbJiraAssignee);
        assigneesList.add(dbJiraAssignee);
        //endregion

        // --- STATUSES
        Instant fetchTimeEndOfDay = io.levelops.commons.dates.DateUtils.toEndOfDay(fetchTime.toInstant());
        List<DbJiraStatus> jiraStatuses = parseStatusLogs(statusLogs, oldestStatus, oldestStatusId, issueCreatedAt, fetchTimeEndOfDay);

        // --- ATTACHMENTS
        int numAttachments = CollectionUtils.size(jiraIssue.getFields().getDynamicFields().get("attachment"));
        log.debug("numAttachments = {}", numAttachments);
        Long firstAttachmentTime = extractFirstAttachmentTime(jiraIssue, numAttachments);

        // --- COMPONENTS
        List<String> components = parseComponents(jiraIssue);
        log.debug("components = {}", components);

        // --- REPORTER
        String reporter = UNKNOWN;
        if (jiraIssue.getFields().getReporter() != null) {
            reporter = MoreObjects.firstNonNull(
                    jiraIssue.getFields().getReporter().getDisplayName(), reporter);
        }
        log.debug("reporter = {}", reporter);

        // --- EPIC
        String epicId = parseEpicId(jiraIssue, config.getEpicLinkField());
        log.debug("epicId = {}", epicId);

        // --- PARENT
        String parentKey = null;
        String parentIssueType = null;
        if (jiraIssue.getFields().getDynamicFields().containsKey("parent")) {
            try {
                Map<String, Object> parent = (Map<String, Object>) jiraIssue.getFields().getDynamicFields().get("parent");
                JiraIssue parentIssue = DefaultObjectMapper.get().convertValue(parent, JiraIssue.class);

                parentKey = parentIssue.getKey();
                parentIssueType = Optional.ofNullable(parentIssue.getFields())
                        .map(JiraIssueFields::getIssueType)
                        .map(JiraIssueType::getName)
                        .orElse(null);
            } catch (Exception e) {
                log.warn("Failed to parse parent issue fields for issue={}", issueKey, e);
            }
        }

        // --- STORY POINTS
        Object storyPoints = jiraIssue.getFields().getDynamicFields().get(config.getStoryPointsField());
        log.debug("storyPoints = {}", storyPoints);
        Integer storyPointsValue = null;
        if (storyPoints instanceof Number) {
            storyPointsValue = ((Number) storyPoints).intValue();
        }
        log.debug("storyPointsValue = {}", storyPointsValue);

        List<DbJiraStoryPoints> storyPointsLogs = parseStoryPointsLogs(parsedChangelog.getStoryPointsLogs(), integrationId, issueKey, storyPointsValue, issueCreatedAt, fetchTime.toInstant().getEpochSecond());
        log.debug("storyPointsLogs={}", storyPointsLogs);

        // --- SPRINTS
        List<Integer> sprintIds = parseSprints(jiraIssue, config.getSprintFieldKey(), integrationId);
        log.debug("sprintIds = {}", sprintIds);
        Map<String, List<JiraIssueSprintEvent>> sprintEvents = parseSprintsLogs(parsedChangelog.getSprintLogs(), ListUtils.emptyIfNull(sprintIds).stream().map(Object::toString).collect(Collectors.toList()),
                issueCreatedAt, fetchTime.toInstant().getEpochSecond());


        // --- CUSTOM FIELDS
        Map<String, Object> customFields = JiraIssueCustomFieldParser.parseCustomFields(jiraIssue, config.getCustomFieldConfig(), config.getCustomFieldProperties());
        log.debug("customFields size = {}", CollectionUtils.size(customFields));

        // --- SF FIELDS
        Map<String, List<String>> salesforceFields = parseSalesforceFields(jiraIssue, config.getSalesforceConfig());

        // --- VERSIONS AND FIX VERSIONS
        List<JiraVersion> versions = MoreObjects.firstNonNull(jiraIssue.getFields().getVersions(), List.of());
        log.debug("versions = {}", versions);
        List<JiraVersion> fixVersions = MoreObjects.firstNonNull(jiraIssue.getFields().getFixVersions(), List.of());
        log.debug("fixVersions = {}", fixVersions);

        // --- ORIGINAL ESTIMATE
        Long origEstimate = NumberUtils.toLong(String.valueOf(jiraIssue.getFields().getDynamicFields().get("timeoriginalestimate")), 0);
        log.debug("origEstimate = {}", origEstimate);
        if (origEstimate == 0) {
            origEstimate = null;
        }
        log.debug("origEstimate = {}", origEstimate);

        // --- STATUS CATEGORY
        String statusCategory = Optional.ofNullable(jiraIssue.getFields().getStatus())
                .map(JiraIssueFields.JiraStatus::getStatusCategory)
                .map(JiraIssueFields.JiraStatus.JiraStatusCategory::getName)
                .orElse("");
        log.debug("statusCategory = {}", statusCategory);
        Long nonResolutionDate = null;
        if ("Done".equalsIgnoreCase(statusCategory)) {
            nonResolutionDate = IterableUtils.getLast(jiraStatuses).map(DbJiraStatus::getStartTime).orElse(null);
        }
        // --------------------------------------------------------------------------------------------------------
        DbJiraIssue dbJiraIssue = DbJiraIssue.builder()
                .hops(hops)
                .key(issueKey)
                .summary(jiraIssue.getFields().getSummary())
                .status(currentStatus)
                .integrationId(integrationId)
                .epic(epicId)
                .parentKey(parentKey)
                .parentIssueType(StringUtils.upperCase(parentIssueType))
                .assignee(currentAssignee)
                .components(components)
                .originalEstimate(origEstimate)
                .versions(versions.stream().map(JiraVersion::getName).collect(Collectors.toList()))
                .fixVersions(fixVersions.stream().map(JiraVersion::getName).collect(Collectors.toList()))
                .project(jiraIssue.getFields().getProject().getKey().toUpperCase())
                .descSize((Integer) jiraIssue.getFields().getDynamicFields()
                        .getOrDefault("description_length", 0))
                .reporter(reporter)
                .issueType(jiraIssue.getFields().getIssueType().getName().toUpperCase())
                .priority(jiraIssue.getFields().getPriority() != null ?
                        jiraIssue.getFields().getPriority().getName().toUpperCase() : UNPRIORITIZED)
                .assigneeList(assigneesList)
                .statuses(jiraStatuses.stream()
                        .map(status -> status.toBuilder()
                                .issueKey(issueKey)
                                .integrationId(integrationId)
                                .build())
                        .collect(Collectors.toList()))
                .firstCommentAt(Objects.nonNull(jiraIssue.getFirstCommentAt()) ? jiraIssue.getFirstCommentAt()
                        : JiraUtils.extractFirstCommentTimeFromJiraIssue(jiraIssue))
                .firstAssignedAt(assigneesList.stream()
                        .filter(assig -> !UNASSIGNED.equalsIgnoreCase(assig.getAssignee()))
                        .reduce((first, second) -> second) // get the last element
                        .orElse(DbJiraAssignee.builder().build()) // prevent npe
                        .getStartTime())
                .firstAssignee(assigneesList.stream()
                        .filter(assig -> !UNASSIGNED.equalsIgnoreCase(assig.getAssignee()))
                        .reduce((first, second) -> second)
                        .orElse(DbJiraAssignee.builder().build())
                        .getAssignee())
                .issueCreatedAt(jiraIssue.getFields().getCreated().toInstant().getEpochSecond())
                .issueUpdatedAt(jiraIssue.getFields().getUpdated().toInstant().getEpochSecond())
                .issueResolvedAt((jiraIssue.getFields().getResolutiondate() != null) ?
                        Long.valueOf(jiraIssue.getFields().getResolutiondate().toInstant().getEpochSecond()) : nonResolutionDate)
                .issueDueAt(dueDate)
                .labels(MoreObjects.firstNonNull(jiraIssue.getFields().getLabels(), List.of()))
                .bounces(bounces)
                .numAttachments(numAttachments)
                .ingestedAt(truncatedDate.toInstant().getEpochSecond())
                .firstAttachmentAt(firstAttachmentTime)
                .customFields(customFields)
                .salesforceFields(salesforceFields)
                .storyPoints(storyPointsValue)
                .storyPointsLogs(storyPointsLogs)
                .sprintIds(sprintIds)
                .sprintEvents(sprintEvents)
                .resolution(jiraIssue.getFields().getResolution() != null ?
                        jiraIssue.getFields().getResolution().getName().toUpperCase() : "")
                .statusCategory(statusCategory)
                .oldIssueKey(oldIssueKey)
                .build();
        log.debug("dbJiraIssue = {}", dbJiraIssue);
        return dbJiraIssue;
    }

    @Value
    @Builder(toBuilder = true)
    public static class ParsedChangelog {
        int hops;
        int bounces;
        Long firstCommentTime;
        String oldestStatus;
        String oldestStatusId;
        List<JiraLogItem> statusLogs; //     from, to = status ids; fromString, toString = status names
        List<JiraLogItem> storyPointsLogs;// from, to unused;       fromString, toString = story points
        List<JiraLogItem> sprintLogs;//      from, to = sprint ids; fromString, toString unused
        Set<String> assigneeSet;
        List<DbJiraAssignee> assigneesList;
        String oldestAssignee;
        Long assigneeTimeIterator;
        String oldIssueKey;
    }

    @Value
    @Builder(toBuilder = true)
    public static class JiraLogItem {
        String from;
        String fromString;
        String to;
        String toString;
        Long time;
    }

    @VisibleForTesting
    @NotNull
    protected static ParsedChangelog parseChangelog(String integrationId,
                                                    JiraIssue jiraIssue,
                                                    String oldestStatus,
                                                    String oldestStatusId,
                                                    String oldestAssignee,
                                                    Long assigneeTimeIterator,
                                                    @Nullable String storyPointsField,
                                                    @Nullable String sprintFieldKey,
                                                    @Nullable String sprintFieldName) {
        List<JiraLogItem> statusLogs = new ArrayList<>();
        List<JiraLogItem> storyPointsLogs = new ArrayList<>();
        List<JiraLogItem> sprintLogs = new ArrayList<>();
        Set<String> assigneeSet = new HashSet<>();
        List<DbJiraAssignee> assigneesList = new LinkedList<>();
        int hops = 0;
        int bounces = 0;
        Long firstCommentTime = null;
        String oldIssueKey = null;

        if (jiraIssue.getChangeLog() == null || CollectionUtils.isEmpty(jiraIssue.getChangeLog().getHistories())) {
            return ParsedChangelog.builder()
                    .oldestStatus(oldestStatus)
                    .oldestStatusId(oldestStatusId)
                    .statusLogs(statusLogs)
                    .storyPointsLogs(storyPointsLogs)
                    .sprintLogs(sprintLogs)
                    .assigneeSet(assigneeSet)
                    .assigneesList(assigneesList)
                    .assigneeTimeIterator(assigneeTimeIterator)
                    .hops(hops)
                    .bounces(bounces)
                    .firstCommentTime(firstCommentTime)
                    .oldestAssignee(oldestAssignee)
                    .build();
        }

        // sorting events from newest to oldest
        List<JiraIssueChangeLog.ChangeLogEvent> sortedEvents = new ArrayList<>(jiraIssue.getChangeLog().getHistories());
        sortedEvents.sort(DESC_CHANGE_LOG_EVENT_COMPARATOR);
        log.debug("after sorting sortedEvents = {}", sortedEvents);

        for (JiraIssueChangeLog.ChangeLogEvent event : sortedEvents) {
            Long logCreatedAt = event.getCreated().toInstant().getEpochSecond();
            log.debug("logCreatedAt = {}", logCreatedAt);
            for (JiraIssueChangeLog.ChangeLogDetails item : event.getItems()) {
                switch (StringUtils.trimToEmpty(item.getField()).toLowerCase()) {
                    case "assignee":
                        assigneeSet.add(oldestAssignee);
                        assigneesList.add(DbJiraAssignee.builder()
                                .integrationId(integrationId)
                                .assignee(oldestAssignee)
                                .startTime(logCreatedAt)
                                .endTime(assigneeTimeIterator)
                                .issueKey(jiraIssue.getKey().toUpperCase())
                                .build());
                        if (StringUtils.isEmpty(item.getFromString())) {
                            oldestAssignee = UNASSIGNED;
                        } else {
                            oldestAssignee = item.getFromString();
                        }
                        if (!UNASSIGNED.equals(oldestAssignee)) {
                            hops += 1;
                            if (assigneeSet.contains(oldestAssignee)) {
                                bounces += 1;
                            }
                        }
                        assigneeTimeIterator = logCreatedAt;
                        break;
                    case "status":
                        String fromStatus = MoreObjects.firstNonNull(item.getFromString(), UNKNOWN).toUpperCase();
                        String fromStatusId = item.getFrom();
                        String toStatus = MoreObjects.firstNonNull(item.getToString(), UNKNOWN).toUpperCase();
                        String toStatusId = item.getTo();
                        if (CollectionUtils.isNotEmpty(statusLogs)
                                && ObjectUtils.allNotNull(oldestStatus, fromStatus, fromStatusId)
                                && oldestStatus.equals(fromStatus)
                                && fromStatus.equals(toStatus)
                                && fromStatusId.equals(toStatusId)) {
                            break;
                        }
                        statusLogs.add(JiraLogItem.builder()
                                .fromString(fromStatus)
                                .from(fromStatusId)
                                .toString(toStatus)
                                .to(toStatusId)
                                .time(logCreatedAt)
                                .build());
                        // keep track of the oldest status
                        oldestStatus = fromStatus;
                        oldestStatusId = fromStatusId;
                        break;
                    case "comment":
                        if (firstCommentTime == null || firstCommentTime > logCreatedAt) {
                            firstCommentTime = logCreatedAt;
                        }
                        break;
                    case "key":
                        if (jiraIssue.getKey() != item.getFromString() && oldIssueKey == null) {
                            oldIssueKey = item.getFromString();
                        }
                        break;
                    default:
                        // --- STORY POINTS
                        if (storyPointsField != null && storyPointsField.trim().equalsIgnoreCase(item.getFieldId())) {
                            storyPointsLogs.add(JiraLogItem.builder()
                                    .fromString(MoreObjects.firstNonNull(item.getFromString(), DEFAULT_STORY_POINTS_LOG).toUpperCase())
                                    .toString(MoreObjects.firstNonNull(item.getToString(), DEFAULT_STORY_POINTS_LOG).toUpperCase())
                                    .time(logCreatedAt)
                                    .build());
                        }
                        // --- SPRINT
                        if (sprintFieldKey != null && sprintFieldKey.trim().equalsIgnoreCase(item.getFieldId())
                                || sprintFieldName != null && sprintFieldName.trim().equalsIgnoreCase(item.getField())) {
                            // Note: for Sprints, from & to contain the id and fromString & toString contain the name
                            sprintLogs.add(JiraLogItem.builder()
                                    .from(StringUtils.trimToEmpty(item.getFrom()).toUpperCase())
                                    .to(StringUtils.trimToEmpty(item.getTo()).toUpperCase())
                                    .time(logCreatedAt)
                                    .build());
                        }
                }
            }
        }

        return ParsedChangelog.builder()
                .oldestStatus(oldestStatus)
                .oldestStatusId(oldestStatusId)
                .statusLogs(statusLogs)
                .storyPointsLogs(storyPointsLogs)
                .sprintLogs(sprintLogs)
                .assigneeSet(assigneeSet)
                .assigneesList(assigneesList)
                .assigneeTimeIterator(assigneeTimeIterator)
                .hops(hops)
                .bounces(bounces)
                .firstCommentTime(firstCommentTime)
                .oldestAssignee(oldestAssignee)
                .oldIssueKey(oldIssueKey)
                .build();
    }

    @VisibleForTesting
    protected static List<DbJiraStatus> parseStatusLogs(List<JiraLogItem> statusLogs, String oldestStatus, String oldestStatusId, Long issueCreatedAt, Instant fetchTimeEndOfDay) {
        // region oldest status
        //     from, to = status ids; fromString, toString = status names
        JiraLogItem jiraStatusLog = JiraLogItem.builder()
                .toString(oldestStatus)
                .to(oldestStatusId)
                .time(issueCreatedAt)
                .build();
        log.debug("jiraStatusLog = {}", jiraStatusLog);
        statusLogs.add(jiraStatusLog);
        // endregion

        // region parse statuses from status log
        statusLogs = statusLogs.stream()
                .sorted(Comparator.comparing(JiraLogItem::getTime))
                .collect(Collectors.toList());
        DbJiraStatus fromStatus = null;

        List<DbJiraStatus> jiraStatuses = new ArrayList<>();
        for (JiraLogItem statusLog : statusLogs) {
            if (fromStatus != null) {
                DbJiraStatus dbJiraStatus = fromStatus.toBuilder()
                        .endTime(statusLog.getTime())
                        .build();
                log.debug("dbJiraStatus = {}", dbJiraStatus);
                jiraStatuses.add(dbJiraStatus);
            }
            fromStatus = DbJiraStatus.builder()
                    .status(statusLog.getToString())
                    .statusId(statusLog.getTo())
                    .startTime(statusLog.getTime())
                    .build();
            log.debug("fromStatus = {}", fromStatus);
        }
        if (fromStatus != null) {
            DbJiraStatus dbJiraStatus = fromStatus.toBuilder()
                    .endTime(fetchTimeEndOfDay.getEpochSecond())
                    .build();
            log.debug("dbJiraStatus = {}", dbJiraStatus);
            jiraStatuses.add(dbJiraStatus);
        }
        // endregion
        return jiraStatuses;
    }

    @VisibleForTesting
    protected static List<DbJiraStoryPoints> parseStoryPointsLogs(List<JiraLogItem> storyPointsLogs, String integrationId, String issueKey, Integer currentStoryPoints, Long issueCreatedAt, long fetchTimeEpochSeconds) {

        // sort input list by time (ascending)
        storyPointsLogs.sort(Comparator.comparing(JiraLogItem::getTime));

        //  output list
        List<DbJiraStoryPoints> jiraStoryPointsList = new ArrayList<>();

        boolean firstItemParsed = false;
        DbJiraStoryPoints.DbJiraStoryPointsBuilder jiraStoryPointsBuilder = null; // starts at previousItem's time and ends at currentItem's time

        for (JiraLogItem logItem : storyPointsLogs) {

            // for the first "story points" record, build it using the first change log as the end date
            if (!firstItemParsed) {
                int storyPointsOfFirstItem = parseStoryPointsFromStringValue(logItem.getFromString());
                DbJiraStoryPoints firstItem = DbJiraStoryPoints.builder()
                        .storyPoints(storyPointsOfFirstItem)
                        .startTime(issueCreatedAt)
                        .endTime(logItem.getTime())
                        .build();
                jiraStoryPointsList.add(firstItem);
                firstItemParsed = true;
            }

            // build the previous item using current time as end
            if (jiraStoryPointsBuilder != null) {
                DbJiraStoryPoints dbJiraStoryPoints = jiraStoryPointsBuilder
                        .endTime(logItem.getTime())
                        .build();
                jiraStoryPointsList.add(dbJiraStoryPoints);
            }

            // starts building current item using current time as start
            int storyPoints = parseStoryPointsFromStringValue(logItem.getToString());
            jiraStoryPointsBuilder = DbJiraStoryPoints.builder()
                    .storyPoints(storyPoints)
                    .startTime(logItem.getTime())
                    .endTime(null);
        }

        // build the last remaining item
        if (jiraStoryPointsBuilder != null) {
            DbJiraStoryPoints dbJiraStoryPoints = jiraStoryPointsBuilder
                    .endTime(fetchTimeEpochSeconds)
                    .build();
            jiraStoryPointsList.add(dbJiraStoryPoints);
        }

        // if there was no change log at all, we need to build the only "story points" record
        // (if the current issue has story points at all)
        if (!firstItemParsed && currentStoryPoints != null) {
            DbJiraStoryPoints firstItem = DbJiraStoryPoints.builder()
                    .storyPoints(currentStoryPoints)
                    .startTime(issueCreatedAt)
                    .endTime(fetchTimeEpochSeconds)
                    .build();
            jiraStoryPointsList.add(firstItem);
        }

        // add the integration id and key to all items
        return jiraStoryPointsList.stream()
                .map(item -> item.toBuilder()
                        .integrationId(integrationId)
                        .issueKey(issueKey)
                        .build())
                .collect(Collectors.toList());
    }

    private static int parseStoryPointsFromStringValue(String value) {
        if (StringUtils.isBlank(value)) {
            return 0;
        }
        try {
            return Math.round(Float.parseFloat(MoreObjects.firstNonNull(value, "0")));
        } catch (NumberFormatException e) {
            log.warn("Failed to parse story points from change log item value: {}", value, e);
            return 0;
        }
    }

    @VisibleForTesting
    protected static Map<String, List<JiraIssueSprintEvent>> parseSprintsLogs(List<JiraLogItem> sprintLogs, List<String> currentSprintIds, Long issueCreatedAt, Long fetchTimeEpochSeconds) {

        // We are going to store all the events in chronological order by sprintId.
        // In the first pass, events will only have a start time and a type.
        // In the second  pass, we will add the end time.

        // map of events by sprint id
        Map<String, List<JiraIssueSprintEventBuilder>> eventBuilders = new HashMap<>();

        TriConsumer<String, JiraSprintEventType, Long> addEvent = (sprintId, eventType, time) -> {
            JiraIssueSprintEventBuilder eventBuilder = JiraIssueSprintEvent.builder()
                    .sprintId(sprintId)
                    .startTime(time)
                    .eventType(eventType);
            List<JiraIssueSprintEventBuilder> eventsForThisSprint = eventBuilders.get(sprintId);
            if (eventsForThisSprint == null) {
                eventsForThisSprint = new ArrayList<>();
            }
            eventsForThisSprint.add(eventBuilder);
            eventBuilders.put(sprintId, eventsForThisSprint);
        };

        // sort input list by time (ascending)
        sprintLogs.sort(Comparator.comparing(JiraLogItem::getTime));

        boolean firstLogItemParsed = false;
        for (JiraLogItem logItem : sprintLogs) {

            // for the first log item, build events using the from values and the issue creation time
            if (!firstLogItemParsed) {
                List<String> sprintIdsAdded = parseSprintIdsFromString(logItem.getFrom());
                sprintIdsAdded.forEach(sprintId -> addEvent.accept(sprintId, ADDED, issueCreatedAt));
                firstLogItemParsed = true;
            }

            Set<String> sprintIdsBefore = CommaListSplitter.splitToStream(logItem.getFrom()).collect(Collectors.toSet());
            Set<String> sprintIdsAfter = CommaListSplitter.splitToStream(logItem.getTo()).collect(Collectors.toSet());
            Set<String> sprintIdsAdded = SetUtils.difference(sprintIdsAfter, sprintIdsBefore).toSet();
            Set<String> sprintIdsRemoved = SetUtils.difference(sprintIdsBefore, sprintIdsAfter).toSet();

            sprintIdsRemoved.forEach(sprintId -> addEvent.accept(sprintId, REMOVED, logItem.getTime()));
            sprintIdsAdded.forEach(sprintId -> addEvent.accept(sprintId, ADDED, logItem.getTime()));
        }

        // if there was no change log at all, we need to build the only event
        // (if the current issue has sprints at all)
        if (!firstLogItemParsed && currentSprintIds != null) {
            currentSprintIds.forEach(sprintId -> addEvent.accept(sprintId, ADDED, issueCreatedAt));
        }

        // -- second pass to add the end time to all events
        Map<String, List<JiraIssueSprintEvent>> eventsBySprintId = new HashMap<>();
        for (String sprintId : eventBuilders.keySet()) {
            // we will be going from the latest event to the earliest
            List<JiraIssueSprintEventBuilder> eventsInReverseChronologicalOrder = Lists.reverse(eventBuilders.get(sprintId));
            List<JiraIssueSprintEvent> outputEvents = new ArrayList<>();
            Long endTime = fetchTimeEpochSeconds;
            for (JiraIssueSprintEventBuilder eventBuilder : eventsInReverseChronologicalOrder) {
                JiraIssueSprintEvent event = eventBuilder.endTime(endTime).build();
                endTime = event.getStartTime();
                outputEvents.add(event);
            }
            eventsBySprintId.put(sprintId, Lists.reverse(outputEvents));
        }

        return eventsBySprintId;
    }

    private static List<String> parseSprintIdsFromString(String value) {
        return CommaListSplitter.split(value);
    }

    private static List<String> parseComponents(JiraIssue jiraIssue) {
        if (CollectionUtils.isEmpty(jiraIssue.getFields().getComponents())) {
            return List.of();
        }
        return jiraIssue.getFields()
                .getComponents()
                .stream()
                .map(JiraComponent::getName)
                .collect(Collectors.toList());
    }

    private static Long parseDueDate(JiraIssue jiraIssue, String dueDateField) {
        if (StringUtils.isEmpty(dueDateField) || dueDateField.equalsIgnoreCase(STANDARD_DUE_DATE_FIELD)) {
            return (jiraIssue.getFields().getDueDate() != null) ? parseDate(jiraIssue.getFields().getDueDate()) : null;
        } else {
            Object dueDateObj = jiraIssue.getFields().getDynamicFields().get(dueDateField);
            log.debug("dueDateObj = {}", dueDateObj);
            if (dueDateObj instanceof String) {
                return parseDate((String) dueDateObj);
            } else if (dueDateObj instanceof Date) {
                return ((Date) dueDateObj).getTime();
            } else if (dueDateObj instanceof DateTime) {
                return ((DateTime) dueDateObj).getMillis();
            } else {
                return null;
            }
        }
    }

    private static Long parseDate(String date) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date).toInstant().getEpochSecond();
        } catch (ParseException e) {
            log.warn("Failed to parse duedate for jira issue.", e);
        }
        return null;
    }

    private static String parseEpicId(JiraIssue jiraIssue, String epicLinkField) {
        if (StringUtils.isEmpty(epicLinkField)) {
            return null;
        }
        Object epicObj = jiraIssue.getFields().getDynamicFields().get(epicLinkField);
        log.debug("epicObj = {}", epicObj);
        if (!(epicObj instanceof String)) {
            return null;
        }
        return (String) epicObj;
    }

    private static Long extractFirstAttachmentTime(JiraIssue jiraIssue, long numAttachments) {
        if (numAttachments <= 0) {
            return null;
        }
        Long firstAttachmentTime = null;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> attachments = (List<Map<String, Object>>) jiraIssue.getFields().getDynamicFields().getOrDefault("attachment", List.of());
        for (Map<String, Object> c : attachments) {
            Long attachmentTime = null;
            try {
                attachmentTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse((String) c.get("created")).toInstant().getEpochSecond();
                log.debug("attachmentTime = {}", attachmentTime);
            } catch (ParseException | NumberFormatException e) {
                log.warn("Failed to parse attachment date for jira issue. date: {}", c.get("created"), e);
            }
            if (firstAttachmentTime == null || (attachmentTime != null && firstAttachmentTime > attachmentTime)) {
                firstAttachmentTime = attachmentTime;
                log.debug("firstAttachmentTime = {}", firstAttachmentTime);
            }
        }
        return firstAttachmentTime;
    }

    @SuppressWarnings({"rawtypes"})
    private static List<Integer> parseSprints(JiraIssue jiraIssue, String sprintFieldKey, String integrationId) {
        if (!StringUtils.isNotEmpty(sprintFieldKey)) {
            return List.of();
        }
        Object sprintVal = jiraIssue.getFields().getDynamicFields().get(sprintFieldKey);
        log.debug("sprintVal = {}", sprintVal);
        if (!(sprintVal instanceof Collection) || !CollectionUtils.isNotEmpty((Collection<?>) sprintVal)) {
            return List.of();
        }
        if (!(sprintVal instanceof Collection && CollectionUtils.isNotEmpty((Collection<?>) sprintVal))
                && !(sprintVal instanceof String && StringUtils.isNotBlank((String) sprintVal))) {
            return List.of();
        }
        Long issueUpdatedAt = jiraIssue.getFields().getUpdated().toInstant().getEpochSecond();
        if (sprintVal instanceof String) {
            return List.of(DbJiraSprint.parseStringSprint((String) sprintVal, integrationId, issueUpdatedAt).getSprintId());
        }
        return ((Collection<?>) sprintVal).stream()
                .map(sprint -> {
                    if (sprint instanceof Map) {
                        Object id = ((Map) sprint).get("id");
                        if (id instanceof Number) {
                            return (Integer) id;
                        }
                    } else if (sprint instanceof String) {
                        return DbJiraSprint.parseStringSprint((String) sprint, integrationId, issueUpdatedAt).getSprintId();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<String, List<String>> parseSalesforceFields(JiraIssue jiraIssue, @Nullable List<IntegrationConfig.ConfigEntry> salesforceConfig) {
        Map<String, List<String>> salesforceFields = new HashMap<>();
        if (CollectionUtils.isEmpty(salesforceConfig)
                || MapUtils.isEmpty(jiraIssue.getFields().getDynamicFields())) {
            return salesforceFields;
        }
        for (IntegrationConfig.ConfigEntry entry : salesforceConfig) {
            if (entry.getKey() == null) {
                continue;
            }
            Object val = jiraIssue.getFields().getDynamicFields().get(entry.getKey());
            log.debug("val = {}", val);
            if (val instanceof List && !((List) val).isEmpty()) {
                log.debug("True - val instanceof List && !((List) val).isEmpty()");
                salesforceFields.put(entry.getKey(), (List<String>) ((List) val).stream()
                        .map(JiraCustomFieldConverter::parseValue)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            } else {
                log.debug("False - val instanceof List && !((List) val).isEmpty()");
                String value = JiraCustomFieldConverter.parseValue(val);
                log.debug("value = {}", value);
                if (StringUtils.isEmpty(value)) {
                    continue;
                }
                salesforceFields.put(entry.getKey(), List.of(value));
            }
        }
        return salesforceFields;
    }
}
