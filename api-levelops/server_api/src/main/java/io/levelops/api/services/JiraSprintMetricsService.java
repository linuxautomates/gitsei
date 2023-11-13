package io.levelops.api.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.jira.DbJiraIssueSprintMapping;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.JiraIssueSprintMappingAggResult;
import io.levelops.commons.databases.services.JiraIssueService;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
public class JiraSprintMetricsService {

    private final JiraIssueService jiraIssueService;

    @Autowired
    public JiraSprintMetricsService(JiraIssueService jiraIssueService) {
        this.jiraIssueService = jiraIssueService;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SprintMetrics.SprintMetricsBuilder.class)
    public static class SprintMetrics {
        @JsonProperty("key") // time
        String key;
        @JsonProperty("additional_key") // sprint name
        String additionalKey;

        @JsonProperty("integration_id")
        String integrationId;
        @JsonProperty("sprint_id")
        String sprintId;
        @JsonProperty("sprint_goal")
        String sprintGoal;

        @JsonProperty("committed_story_points")
        Integer committedStoryPoints;
        @JsonProperty("commit_delivered_story_points")
        Integer commitDeliveredStoryPoints;
        @JsonProperty("delivered_story_points")
        Integer deliveredStoryPoints;
        @JsonProperty("creep_story_points")
        Integer creepStoryPoints;
        @JsonProperty("delivered_creep_story_points")
        Integer deliveredCreepStoryPoints;

        @JsonProperty("committed_keys")
        Set<String> committedKeys;
        @JsonProperty("commit_delivered_keys")
        Set<String> commitDeliveredKeys;
        @JsonProperty("delivered_keys")
        Set<String> deliveredKeys;
        @JsonProperty("creep_keys")
        Set<String> creepKeys;
        @JsonProperty("delivered_creep_keys")
        Set<String> deliveredCreepKeys;

        @JsonProperty("total_issues")
        Integer totalIssues;
        @JsonProperty("total_unestimated_issues")
        Integer totalUnestimatedIssues;
        @JsonProperty("unestimated_issues_by_type")
        Map<String, Integer> unestimatedIssuesByType;
        @JsonProperty("story_points_by_issue")
        Map<String, IssueStoryPoints> storyPointsByIssue;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = IssueStoryPoints.IssueStoryPointsBuilder.class)
        public static class IssueStoryPoints {
            @JsonProperty("before")
            Integer before;
            @JsonProperty("after")
            Integer after;
        }
    }

    @Value
    @Builder(toBuilder = true)
    public static class SprintMetricsSettings {
        boolean includeIssueKeys;
        long creepBuffer;
        @Nullable
        List<String> additionalDoneStatuses;

        /**
         * PROP-3648 Jira used to treat "outside of sprint" issues as planned and delivered, but not anymore.
         * We can control this behavior with this setting.
         * <br/>
         * From <a href="https://support.atlassian.com/jira-software-cloud/docs/view-and-understand-the-team-managed-sprint-burndown-chart/">Jira</a> :
         * Issues completed outside of sprint include issues that were:
         * - completed and then added to the sprint, either before the sprint started or after the sprint started.
         * - added to the sprint, but completed before the sprint started.
         */
        boolean treatOutsideOfSprintAsPlannedAndDelivered;
    }

    @Nullable
    public List<SprintMetrics> generateSprintMetrics(String company, List<DbAggregationResult> records, SprintMetricsSettings settings) {
        return records.stream()
                .map(r -> generateSingleSprintMetrics(company, r, settings))
                .collect(Collectors.toList());
    }

    /**
     * Aggregates sprint mappings into Sprint metrics.
     * Note that some precalculated values are being overridden due to dynamic widget settings.
     */
    public SprintMetrics generateSingleSprintMetrics(String company, DbAggregationResult record, SprintMetricsSettings settings) {
        String integrationId = record.getIntegrationId();
        String sprintId = record.getSprintId();
        String sprintName = record.getSprintName();
        String sprintGoal = record.getSprintGoal();
        Long sprintCompletedAt = record.getSprintCompletedAt();
        Long sprintStartedAt = record.getSprintStartedAt();
        boolean includeIssueKeys = settings.isIncludeIssueKeys();
        boolean treatOutsideOfSprintAsPlannedAndDelivered = settings.isTreatOutsideOfSprintAsPlannedAndDelivered();
        long creepBuffer = settings.getCreepBuffer();
        Set<String> additionalDoneStatuses = ListUtils.emptyIfNull(settings.getAdditionalDoneStatuses()).stream()
                .filter(StringUtils::isNotEmpty)
                .map(String::toLowerCase)
                .map(String::trim)
                .collect(Collectors.toSet());
        List<JiraIssueSprintMappingAggResult> sprintMappingAggs = ListUtils.emptyIfNull(record.getSprintMappingAggs());

        log.info("Processing sprint metrics for company={}, integration_id={}, sprint_id={}, includeIssueKeys={}", company, integrationId, sprintId, includeIssueKeys);


        List<String> issueKeys = sprintMappingAggs.stream()
                .map(r -> r.getSprintMapping().getIssueKey())
                .distinct()
                .collect(Collectors.toList());
        Map<String, DbJiraStatus> historicalStatusByIssue = Collections.emptyMap();
        if (!additionalDoneStatuses.isEmpty() && sprintCompletedAt != null) {
            historicalStatusByIssue = jiraIssueService.getHistoricalStatusForIssues(company, integrationId, issueKeys, sprintCompletedAt);
        }

        Set<String> committedKeys = new HashSet<>();
        Set<String> commitDeliveredKeys = new HashSet<>();
        Set<String> deliveredKeys = new HashSet<>();
        Set<String> creepKeys = new HashSet<>();
        Set<String> deliveredCreepKeys = new HashSet<>();
        Map<String, Integer> unestimatedIssuesByType = new HashMap<>();
        Map<String, SprintMetrics.IssueStoryPoints> storyPointsByIssue = new HashMap<>();
        int committedStoryPoints = 0;
        int deliveredStoryPoints = 0;
        int commitDeliveredStoryPoints = 0;
        int creepStoryPoints = 0;
        int deliveredCreepStoryPoints = 0;
        int totalUnestimatedIssues = 0;
        int totalIssues = 0;
        Set<String> seenIssueKeys = new HashSet<>();
        for (JiraIssueSprintMappingAggResult sprintMappingAgg : sprintMappingAggs) {
            DbJiraIssueSprintMapping mapping = sprintMappingAgg.getSprintMapping();

            // -- dedupe issue
            String issueKey = mapping.getIssueKey();
            if (seenIssueKeys.contains(issueKey)) {
                log.info("Single Sprint Metric company={}, integration_id={}, sprint_id={}, committedStoryPoints={}, issueKey={} is duplicate!", company, integrationId, sprintId, committedStoryPoints, issueKey);
                continue;
            }
            seenIssueKeys.add(issueKey);
            log.debug("Single Sprint Metric company={}, integration_id={}, sprint_id={}, committedStoryPoints={}, issueKey={} seen for first time!", company, integrationId, sprintId, committedStoryPoints, issueKey);

            // -- gather issue metadata
            String issueType = StringUtils.defaultIfEmpty(sprintMappingAgg.getIssueType(), "Unknown");
            boolean delivered = mapping.getDelivered(); // = status was in done category
            boolean outsideOfSprint = mapping.getOutsideOfSprint();
            if (!delivered && !additionalDoneStatuses.isEmpty()) {
                // if it was not delivered, check if the historical status is in the list of additional done statuses
                DbJiraStatus dbJiraStatus = historicalStatusByIssue.get(issueKey);
                if (dbJiraStatus != null && additionalDoneStatuses.contains(StringUtils.trimToEmpty(dbJiraStatus.getStatus()).toLowerCase())) {
                    delivered = true;
                    if (sprintStartedAt != null && dbJiraStatus.getStartTime() < sprintStartedAt) {
                        outsideOfSprint = true;
                    }

                }
            }
            // LEV-5397 not using mapping.getPlanned()
            boolean planned = false;
            if (sprintStartedAt != null) {
                // if the issue was added to the sprint *before* it started (+ buffer) then it is planned
                planned = mapping.getAddedAt() < sprintStartedAt + creepBuffer;
            }

            Integer storyPointsAtTheStart = mapping.getStoryPointsPlanned();
            Integer storyPointsAtTheEnd = mapping.getStoryPointsDelivered(); // misnomer: this is the story points at the end of the sprint, regardless of them being delivered or not
            log.info("Single Sprint Metric company={}, integration_id={}, sprint_id={}, committedStoryPoints={}, issueKey={}, planned={}, delivered={}, outsideOfSprint={}, storyPointsAtTheStart={}", company, integrationId, sprintId, committedStoryPoints, issueKey, planned, delivered, outsideOfSprint, storyPointsAtTheStart);

            // -- sprint metrics logic
            boolean effectivePlanned = outsideOfSprint ? treatOutsideOfSprintAsPlannedAndDelivered : planned;
            if (effectivePlanned) {
                committedStoryPoints += storyPointsAtTheStart;
                if (includeIssueKeys) {
                    committedKeys.add(issueKey);
                }
            }
            boolean effectiveDelivered = outsideOfSprint ? treatOutsideOfSprintAsPlannedAndDelivered : delivered;
            if (effectiveDelivered) {
                deliveredStoryPoints += storyPointsAtTheEnd;
                if (includeIssueKeys) {
                    deliveredKeys.add(issueKey);
                }
                if (effectivePlanned) {
                    commitDeliveredStoryPoints += storyPointsAtTheEnd;
                    if (includeIssueKeys) {
                        commitDeliveredKeys.add(issueKey);
                    }
                }
            }
            if (!planned && !outsideOfSprint) {
                creepStoryPoints += storyPointsAtTheEnd;
                if (includeIssueKeys) {
                    creepKeys.add(issueKey);
                }
                if (delivered) {
                    deliveredCreepStoryPoints += storyPointsAtTheEnd;
                    if (includeIssueKeys) {
                        deliveredCreepKeys.add(issueKey);
                    }
                }
            }
            boolean shouldIncludeIssue = !outsideOfSprint || treatOutsideOfSprintAsPlannedAndDelivered;
            if (shouldIncludeIssue) {
                totalIssues++;
                if (storyPointsAtTheStart <= 0) {
                    Integer previousCount = unestimatedIssuesByType.getOrDefault(issueType, 0);
                    unestimatedIssuesByType.put(issueType, previousCount + 1);
                    totalUnestimatedIssues++;
                }
                if (includeIssueKeys) {
                    storyPointsByIssue.put(issueKey, SprintMetrics.IssueStoryPoints.builder()
                            .before(mapping.getStoryPointsPlanned())
                            .after(storyPointsAtTheEnd)
                            .build());
                }
            }
        }

        return SprintMetrics.builder()
                .key(sprintCompletedAt != null ? sprintCompletedAt.toString() : null)
                .additionalKey(sprintName)
                .integrationId(integrationId)
                .sprintId(sprintId)
                .sprintGoal(sprintGoal)
                .committedStoryPoints(committedStoryPoints)
                .commitDeliveredStoryPoints(commitDeliveredStoryPoints)
                .deliveredStoryPoints(deliveredStoryPoints)
                .creepStoryPoints(creepStoryPoints)
                .deliveredCreepStoryPoints(deliveredCreepStoryPoints)
                .committedKeys(committedKeys)
                .commitDeliveredKeys(commitDeliveredKeys)
                .deliveredKeys(deliveredKeys)
                .creepKeys(creepKeys)
                .deliveredCreepKeys(deliveredCreepKeys)
                .unestimatedIssuesByType(unestimatedIssuesByType)
                .totalIssues(totalIssues)
                .totalUnestimatedIssues(totalUnestimatedIssues)
                .storyPointsByIssue(storyPointsByIssue)
                .build();

    }

}
