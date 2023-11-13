package io.levelops.api.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.DbJiraIssueSprintMapping;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.JiraIssueSprintMappingDatabaseService;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@Service
@Deprecated
public class JiraSprintMetricsServiceLegacy {

    private static final Integer SPRINTS_DEFAULT_PAGE_SIZE = 25;
    private static final String CLOSED_SPRINT_STATE = "closed";
    private final JiraIssueService jiraIssueService;
    private final JiraIssueSprintMappingDatabaseService sprintMappingDatabaseService;

    @Autowired
    public JiraSprintMetricsServiceLegacy(
            JiraIssueService jiraIssueService,
            JiraIssueSprintMappingDatabaseService sprintMappingDatabaseService) {
        this.jiraIssueService = jiraIssueService;
        this.sprintMappingDatabaseService = sprintMappingDatabaseService;
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

        @JsonProperty("committed_story_points")
        Integer committedStoryPoints;
        @JsonProperty("delivered_story_points")
        Integer deliveredStoryPoints;
        @JsonProperty("creep_story_points")
        Integer creepStoryPoints;
        @JsonProperty("delivered_creep_story_points")
        Integer deliveredCreepStoryPoints;

        @JsonProperty("committed_keys")
        Set<String> committedKeys;
        @JsonProperty("delivered_keys")
        Set<String> deliveredKeys;
        @JsonProperty("creep_keys")
        Set<String> creepKeys;
        @JsonProperty("delivered_creep_keys")
        Set<String> deliveredCreepKeys;
    }

    @Value
    @Builder(toBuilder = true)
    public static class SprintMetricsResponse {
        List<SprintMetrics> sprintMetrics;
        int sprintCount;
        boolean hasNext;
    }

    public SprintMetricsResponse generateSprintMetrics(String company,
                                                       @Nullable Integer pageNumber,
                                                       @Nullable Integer pageSize,
                                                       Long completedAtAfter,
                                                       Long completedAtBefore,
                                                       List<String> sprintNames,
                                                       String sprintNameStartsWith,
                                                       String sprintNameEndsWith,
                                                       String sprintNameContains,
                                                       List<String> excludeSprintNames,
                                                       @Nullable JiraIssuesFilter filter,
                                                       boolean includeIssueKeys,
                                                       OUConfiguration ouConfig) {
        pageSize = MoreObjects.firstNonNull(pageSize, SPRINTS_DEFAULT_PAGE_SIZE);
        List<DbJiraSprint> sprints = findCompletedSprintsWithinTimeRange(company, pageNumber, pageSize, completedAtAfter, completedAtBefore,
                sprintNames, sprintNameStartsWith, sprintNameEndsWith, sprintNameContains, excludeSprintNames,
                filter != null ? filter.getIntegrationIds() : null,
                filter != null ? filter.getSprintIds() : null);
        List<SprintMetrics> sprintMetrics = sprints.stream()
                .map(sprint -> generateSingleSprintMetrics(company, sprint, filter, includeIssueKeys, ouConfig))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return SprintMetricsResponse.builder()
                .sprintMetrics(sprintMetrics)
                .sprintCount(sprints.size())
                .hasNext(sprints.size() >= pageSize) // we can't rely on sprintMetrics.size() because some sprints may not return metrics at all
                .build();
    }

    public List<DbJiraSprint> findCompletedSprintsWithinTimeRange(String company,
                                                                  @Nullable Integer pageNumber,
                                                                  @Nullable Integer pageSize,
                                                                  Long completedAtAfter,
                                                                  Long completedAtBefore,
                                                                  List<String> sprintNames,
                                                                  String sprintNameStartsWith,
                                                                  String sprintNameEndsWith,
                                                                  String sprintNameContains,
                                                                  List<String> excludeSprintNames,
                                                                  @Nullable List<String> integrationIds,
                                                                  @Nullable List<String> sprintIds) {
        return jiraIssueService.filterSprints(company, pageNumber, pageSize,
                JiraSprintFilter.builder()
                        .integrationIds(integrationIds)
                        .sprintIds(sprintIds)
                        .completedAtAfter(completedAtAfter)
                        .completedAtBefore(completedAtBefore)
                        .names(sprintNames)
                        .excludeNames(excludeSprintNames)
                        .nameStartsWith(sprintNameStartsWith)
                        .nameEndsWith(sprintNameEndsWith)
                        .nameContains(sprintNameContains)
                        .state(CLOSED_SPRINT_STATE)
                        .build()).getRecords()
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Nullable
    public SprintMetrics generateSingleSprintMetrics(String company, DbJiraSprint sprint, @Nullable JiraIssuesFilter filter, boolean includeIssueKeys, OUConfiguration ouConfig) {
        String integrationId = String.valueOf(sprint.getIntegrationId());
        String sprintId = String.valueOf(sprint.getSprintId());
        Map<String, DbJiraIssueSprintMapping> sprintMappingPerIssueKey = sprintMappingDatabaseService.streamWithCustomPageSize(company, JiraIssueSprintMappingDatabaseService.JiraIssueSprintMappingFilter.builder()
                .integrationIds(List.of(integrationId))
                .sprintIds(List.of(sprintId))
                .ignorableIssueType(false)
                .build(), 500)
                .collect(Collectors.toMap(DbJiraIssueSprintMapping::getIssueKey, Function.identity(), (a, b) -> {
                    log.warn("Map collect found duplicate key: a={}, b={}", a, b);
                    return b;
                }));

        Set<String> issueKeys = sprintMappingPerIssueKey.keySet();
        Set<String> filteredKeys = filterIssues(company, integrationId, issueKeys, filter, ouConfig);
        if (CollectionUtils.isEmpty(filteredKeys)) {
            return null;
        }

        Set<String> committedKeys = new HashSet<>();
        Set<String> deliveredKeys = new HashSet<>();
        Set<String> creepKeys = new HashSet<>();
        Set<String> deliveredCreepKeys = new HashSet<>();
        int committedStoryPoints = 0;
        int deliveredStoryPoints = 0;
        int creepStoryPoints = 0;
        int deliveredCreepStoryPoints = 0;
        for (String issueKey : filteredKeys) {
            DbJiraIssueSprintMapping mapping = sprintMappingPerIssueKey.get(issueKey);
            boolean delivered = mapping.getDelivered();
            boolean planned = mapping.getPlanned();
            boolean outsideOfSprint = mapping.getOutsideOfSprint();
            if (planned && !outsideOfSprint) {
                committedStoryPoints += mapping.getStoryPointsPlanned();
                if (includeIssueKeys) {
                    committedKeys.add(issueKey);
                }
            }
            if (delivered || outsideOfSprint) {
                deliveredStoryPoints += mapping.getStoryPointsDelivered();
                if (includeIssueKeys) {
                    deliveredKeys.add(issueKey);
                }
            }
            if (!planned && !outsideOfSprint) {
                creepStoryPoints += mapping.getStoryPointsDelivered();
                if (includeIssueKeys) {
                    creepKeys.add(issueKey);
                }
                if (delivered) {
                    deliveredCreepStoryPoints += mapping.getStoryPointsDelivered();
                    if (includeIssueKeys) {
                        deliveredCreepKeys.add(issueKey);
                    }
                }
            }
        }

        return SprintMetrics.builder()
                .key(sprint.getCompletedDate() != null ? sprint.getCompletedDate().toString() : null)
                .additionalKey(sprint.getName())
                .integrationId(integrationId)
                .sprintId(sprintId)
                .committedStoryPoints(committedStoryPoints)
                .deliveredStoryPoints(deliveredStoryPoints)
                .creepStoryPoints(creepStoryPoints)
                .deliveredCreepStoryPoints(deliveredCreepStoryPoints)
                .committedKeys(committedKeys)
                .deliveredKeys(deliveredKeys)
                .creepKeys(creepKeys)
                .deliveredCreepKeys(deliveredCreepKeys)
                .build();

    }

    public Set<String> filterIssues(String company, String integrationId, Set<String> issueKeys, @Nullable JiraIssuesFilter filter, final OUConfiguration ouConfiguration) {
        if (filter == null || CollectionUtils.isEmpty(issueKeys)) {
            return issueKeys;
        }
        log.debug("Filtering out issues within set={}", issueKeys);
        filter = filter.toBuilder()
                .integrationIds(List.of(integrationId))
                .keys(new ArrayList<>(issueKeys))
                .build();
        final JiraIssuesFilter finalFilter = filter;
        try {
            return PaginationUtils.stream(0, 1, page -> {
                try {
                    return jiraIssueService.list(company, JiraSprintFilter.builder().build(), finalFilter, ouConfiguration, Map.of(), page, 500).getRecords().stream()
                            .map(DbJiraIssue::getKey)
                            .filter(StringUtils::isNotEmpty)
                            .collect(Collectors.toList());
                } catch (SQLException e) {
                    throw new RuntimeStreamException(e);
                }
            }).collect(Collectors.toSet());
        } catch (RuntimeStreamException e) {
            log.warn("Failed to list jira issues for company=" + company, e);
            return Set.of();
        }
    }

}
