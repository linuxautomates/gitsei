package io.levelops.api.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.issue_management.DbIssueMgmtSprintMapping;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.IssueMgmtSprintMappingAggResult;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
@SuppressWarnings("unused")
public class IssueMgmtSprintService {
    @Autowired
    public IssueMgmtSprintService() {
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = IssueMgmtSprintService.SprintMetrics.SprintMetricsBuilder.class)
    public static class SprintMetrics {
        @JsonProperty("key") // time
        String key;
        @JsonProperty("additional_key") // sprint name
        String additionalKey;

        @JsonProperty("integration_id")
        String integrationId;
        @JsonProperty("sprint_id")
        String sprintId;

        @JsonProperty("committed_story_points")
        Float committedStoryPoints;
        @JsonProperty("commit_delivered_story_points")
        Float commitDeliveredStoryPoints;
        @JsonProperty("delivered_story_points")
        Float deliveredStoryPoints;
        @JsonProperty("creep_story_points")
        Float creepStoryPoints;
        @JsonProperty("delivered_creep_story_points")
        Float deliveredCreepStoryPoints;

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

        @JsonProperty("total_workitems")
        Integer totalWorkitems;
        @JsonProperty("total_unestimated_workitems")
        Integer totalUnestimatedWorkitems;
        @JsonProperty("unestimated_workitems_by_type")
        Map<String, Integer> unestimatedWorkitemsByType;
        @JsonProperty("story_points_by_workitem")
        Map<String, WorkitemStoryPoints> storyPointsByWorkitem;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = IssueMgmtSprintService.SprintMetrics.WorkitemStoryPoints.WorkitemStoryPointsBuilder.class)
        public static class WorkitemStoryPoints {
            @JsonProperty("before")
            Float before;
            @JsonProperty("after")
            Float after;
        }
    }

    @Value
    @Builder(toBuilder = true)
    public static class SprintMetricsSettings {
        boolean includeWorkitemIds;
        long creepBuffer;

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
    public List<IssueMgmtSprintService.SprintMetrics> generateSprintMetrics(String company, List<DbAggregationResult> records, SprintMetricsSettings settings) {
        return records.stream()
                .map(r -> generateSingleSprintMetrics(company, r, settings))
                .collect(Collectors.toList());
    }

    public IssueMgmtSprintService.SprintMetrics generateSingleSprintMetrics(String company, DbAggregationResult record, SprintMetricsSettings settings) {
        String integrationId = record.getIntegrationId();
        String sprintId = record.getSprintId();
        String sprintName = record.getSprintName();
        Long sprintCompletedAt = record.getSprintCompletedAt();
        Long sprintStartedAt = record.getSprintStartedAt();
        long creepBuffer = settings.getCreepBuffer();
        boolean includeWorkitemIds = settings.isIncludeWorkitemIds();
        boolean treatOutsideOfSprintAsPlannedAndDelivered = settings.isTreatOutsideOfSprintAsPlannedAndDelivered();

        log.info("Processing sprint metrics for company={}, integration_id={}, sprint_id={}, includeWorkitemIds={}", company, integrationId, sprintId, includeWorkitemIds);

        List<IssueMgmtSprintMappingAggResult> sprintMappingAggs = ListUtils.emptyIfNull(record.getIssueMgmtSprintMappingAggResults());

        Set<String> committedIds = new HashSet<>();
        Set<String> commitDeliveredIds = new HashSet<>();
        Set<String> deliveredIds = new HashSet<>();
        Set<String> creepIds = new HashSet<>();
        Set<String> deliveredCreepIds = new HashSet<>();
        Map<String, Integer> unestimatedWorkitemsByType = new HashMap<>();
        Map<String, IssueMgmtSprintService.SprintMetrics.WorkitemStoryPoints> storyPointsByWorkitem = new HashMap<>();
        float committedStoryPoints = 0;
        float deliveredStoryPoints = 0;
        float commitDeliveredStoryPoints = 0;
        float creepStoryPoints = 0;
        float deliveredCreepStoryPoints = 0;
        int totalUnestimatedWorkitems = 0;
        int totalIssues = 0;
        Set<String> seenWorkitemIds = new HashSet<>();
        for (IssueMgmtSprintMappingAggResult sprintMappingAgg : sprintMappingAggs) {
            DbIssueMgmtSprintMapping mapping = sprintMappingAgg.getSprintMapping();
            String workitemType = StringUtils.defaultIfEmpty(sprintMappingAgg.getWorkitemType(), "Unknown");
            String workitemId = mapping.getWorkitemId();

            if(sprintStartedAt != null && sprintMappingAgg.getSprintMapping().getRemovedAt() != null
                    && sprintMappingAgg.getSprintMapping().getRemovedAt() < sprintStartedAt){
                log.info("Issue Single Sprint Metric company={}, integration_id={}, sprint_id={}, workitemId={} was added and removed from the sprint before the start of the sprint!", company, integrationId, sprintId, workitemId);
                continue;
            }

            if (seenWorkitemIds.contains(workitemId)) {
                log.info("Issue Single Sprint Metric company={}, integration_id={}, sprint_id={}, committedStoryPoints={}, workitemId={} is duplicate!", company, integrationId, sprintId, committedStoryPoints, workitemId);
                continue;
            }
            seenWorkitemIds.add(workitemId);
            log.debug("Single Sprint Metric company={}, integration_id={}, sprint_id={}, committedStoryPoints={}, workitemId={} seen for first time!", company, integrationId, sprintId, committedStoryPoints, workitemId);
            boolean delivered = mapping.getDelivered();
            boolean planned = mapping.getPlanned();
            if (creepBuffer > 0 && mapping.getAddedAt() != null && sprintStartedAt != null) {
                // if the issue was added to the sprint *before* it started (+ buffer) then it is planned
                planned |= mapping.getAddedAt() < sprintStartedAt + creepBuffer;
            }
            boolean outsideOfSprint = mapping.getOutsideOfSprint();
            Float storyPointsPlanned = mapping.getStoryPointsPlanned();
            log.info("Issue Single Sprint Metric company={}, integration_id={}, sprint_id={}, committedStoryPoints={}, workitemId={}, planned={}, outsideOfSprint={}, storyPointsPlanned={}", company, integrationId, sprintId, committedStoryPoints, workitemId, planned, outsideOfSprint, storyPointsPlanned);
            boolean effectivePlanned = outsideOfSprint ? treatOutsideOfSprintAsPlannedAndDelivered : planned;
            if (effectivePlanned) {
                committedStoryPoints += storyPointsPlanned;
                if (includeWorkitemIds) {
                    committedIds.add(workitemId);
                }
            }
            boolean effectiveDelivered = outsideOfSprint ? treatOutsideOfSprintAsPlannedAndDelivered : delivered;
            if (effectiveDelivered) {
                deliveredStoryPoints += mapping.getStoryPointsDelivered();
                if (includeWorkitemIds) {
                    deliveredIds.add(workitemId);
                }
                if (effectivePlanned) {
                    commitDeliveredStoryPoints += storyPointsPlanned;
                    if (includeWorkitemIds) {
                        commitDeliveredIds.add(workitemId);
                    }
                }
            }
            if (!planned && !outsideOfSprint) {
                creepStoryPoints += mapping.getStoryPointsDelivered();
                if (includeWorkitemIds) {
                    creepIds.add(workitemId);
                }
                if (delivered) {
                    deliveredCreepStoryPoints += mapping.getStoryPointsDelivered();
                    if (includeWorkitemIds) {
                        deliveredCreepIds.add(workitemId);
                    }
                }
            }
            boolean shouldIncludeIssue = !outsideOfSprint || treatOutsideOfSprintAsPlannedAndDelivered;
            if (shouldIncludeIssue) {
                totalIssues++;
                if (storyPointsPlanned <= 0) {
                    Integer previousCount = unestimatedWorkitemsByType.getOrDefault(workitemType, 0);
                    unestimatedWorkitemsByType.put(workitemType, previousCount + 1);
                    totalUnestimatedWorkitems++;
                }
                if (includeWorkitemIds) {
                    storyPointsByWorkitem.put(workitemId, IssueMgmtSprintService.SprintMetrics.WorkitemStoryPoints.builder()
                            .before(mapping.getStoryPointsPlanned())
                            .after(mapping.getStoryPointsDelivered())
                            .build());
                }
            }
        }

        return SprintMetrics.builder()
                .key(sprintCompletedAt != null ? sprintCompletedAt.toString() : null)
                .additionalKey(sprintId) // we have to use sprintId because  iterationPath for ADU
                .integrationId(integrationId)
                .sprintId(sprintId)
                .committedStoryPoints(committedStoryPoints)
                .commitDeliveredStoryPoints(commitDeliveredStoryPoints)
                .deliveredStoryPoints(deliveredStoryPoints)
                .creepStoryPoints(creepStoryPoints)
                .deliveredCreepStoryPoints(deliveredCreepStoryPoints)
                .committedKeys(committedIds)
                .commitDeliveredKeys(commitDeliveredIds)
                .deliveredKeys(deliveredIds)
                .creepKeys(creepIds)
                .deliveredCreepKeys(deliveredCreepIds)
                .unestimatedWorkitemsByType(unestimatedWorkitemsByType)
                .totalWorkitems(totalIssues)
                .totalUnestimatedWorkitems(totalUnestimatedWorkitems)
                .storyPointsByWorkitem(storyPointsByWorkitem)
                .build();

    }
}
