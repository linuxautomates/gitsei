package io.levelops.api.services;

import io.levelops.api.services.JiraSprintMetricsService.SprintMetricsSettings;
import io.levelops.commons.databases.models.database.jira.DbJiraIssueSprintMapping;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.JiraIssueSprintMappingAggResult;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class JiraSprintMetricsServiceTest {

    @Mock
    JiraIssueService jiraIssueService;

    private JiraSprintMetricsService service;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        service = new JiraSprintMetricsService(jiraIssueService);
    }

    @Test
    public void testCreepBuffer() {
        DbAggregationResult aggResults = DbAggregationResult
                .builder()
                .integrationId("1")
                .sprintId("1")
                .sprintName("Sprint 1")
                .sprintStartedAt(1000L)
                .sprintCompletedAt(2000L)
                .sprintMappingAggs(List.of(JiraIssueSprintMappingAggResult
                                .builder()
                                .issueType("Unknown")
                                .sprintMapping(DbJiraIssueSprintMapping.builder()
                                        .issueKey("A")
                                        .outsideOfSprint(false)
                                        .planned(false)
                                        .addedAt(1010L)
                                        .delivered(false)
                                        .storyPointsPlanned(5)
                                        .storyPointsDelivered(3)
                                        .build())
                                .build(),
                        JiraIssueSprintMappingAggResult
                                .builder()
                                .issueType("Unknown")
                                .sprintMapping(DbJiraIssueSprintMapping.builder()
                                        .issueKey("B")
                                        .outsideOfSprint(false)
                                        .planned(true)
                                        .addedAt(900L)
                                        .delivered(false)
                                        .storyPointsPlanned(5)
                                        .storyPointsDelivered(3)
                                        .build()).build()))
                .build();
        JiraSprintMetricsService.SprintMetrics metrics = service.generateSingleSprintMetrics("test", aggResults, SprintMetricsSettings.builder()
                .includeIssueKeys(true)
                .creepBuffer(0)
                .build());
        // added at 1010L < started at 1000L + buffer 0 -> NO -> CREEP
        // added at 900L < started at 1000L + buffer 0 -> YES -> PLANNED
        assertThat(metrics.getCommittedKeys().size()).isEqualTo(1);
        assertThat(metrics.getCreepKeys().size()).isEqualTo(1);

        metrics = service.generateSingleSprintMetrics("test", aggResults, SprintMetricsSettings.builder()
                .includeIssueKeys(true)
                .creepBuffer(100L)
                .build());
        // added at 1010L < started at 1000L + buffer 100 -> YES -> PLANNED
        // added at 900L < started at 1000L + buffer 0 -> YES -> PLANNED
        assertThat(metrics.getCommittedKeys().size()).isEqualTo(2);
        assertThat(metrics.getCreepKeys().size()).isEqualTo(0);

    }

    @Test
    public void testAdditionalDoneStates() {
        DbAggregationResult aggResults = DbAggregationResult
                .builder()
                .integrationId("1")
                .sprintId("1")
                .sprintName("Sprint 1")
                .sprintStartedAt(1000L)
                .sprintCompletedAt(2000L)
                .sprintMappingAggs(List.of(JiraIssueSprintMappingAggResult
                                .builder()
                                .issueType("Unknown")
                                .sprintMapping(DbJiraIssueSprintMapping.builder()
                                        .issueKey("A")
                                        .outsideOfSprint(false)
                                        .planned(false)
                                        .addedAt(1010L)
                                        .delivered(false)
                                        .storyPointsPlanned(5)
                                        .storyPointsDelivered(3)
                                        .build())
                                .build(),
                        JiraIssueSprintMappingAggResult
                                .builder()
                                .issueType("Unknown")
                                .sprintMapping(DbJiraIssueSprintMapping.builder()
                                        .issueKey("B")
                                        .outsideOfSprint(false)
                                        .planned(true)
                                        .addedAt(900L)
                                        .delivered(false)
                                        .storyPointsPlanned(5)
                                        .storyPointsDelivered(3)
                                        .build()).build()))
                .build();
        when(jiraIssueService.getHistoricalStatusForIssues(eq("test"), eq("1"), eq(List.of("A", "B")), eq(2000L)))
                .thenReturn(Map.of("A", DbJiraStatus.builder()
                        .status("in QA")
                        .startTime(1500L)
                        .build()));

        JiraSprintMetricsService.SprintMetrics metrics = service.generateSingleSprintMetrics("test", aggResults, SprintMetricsSettings.builder()
                .includeIssueKeys(true)
                .build());
        assertThat(metrics.getCommittedKeys()).containsExactlyInAnyOrder("B");
        assertThat(metrics.getDeliveredKeys()).isEmpty();
        assertThat(metrics.getCreepKeys()).containsExactlyInAnyOrder("A");
        assertThat(metrics.getDeliveredCreepKeys()).isEmpty();
        assertThat(metrics.getTotalIssues()).isEqualTo(2);
        assertThat(metrics.getStoryPointsByIssue().size()).isEqualTo(2);
        assertThat(metrics.getTotalUnestimatedIssues()).isEqualTo(0);
        assertThat(metrics.getUnestimatedIssuesByType().size()).isEqualTo(0);

        metrics = service.generateSingleSprintMetrics("test", aggResults, SprintMetricsSettings.builder()
                .includeIssueKeys(true)
                .additionalDoneStatuses(List.of("in QA"))
                .build());
        DefaultObjectMapper.prettyPrint(metrics);
        assertThat(metrics.getCommittedKeys()).containsExactlyInAnyOrder("B");
        assertThat(metrics.getDeliveredKeys()).containsExactlyInAnyOrder("A");
        assertThat(metrics.getCreepKeys()).containsExactlyInAnyOrder("A");
        assertThat(metrics.getDeliveredCreepKeys()).containsExactlyInAnyOrder("A");
        assertThat(metrics.getTotalIssues()).isEqualTo(2);
        assertThat(metrics.getStoryPointsByIssue().size()).isEqualTo(2);
        assertThat(metrics.getTotalUnestimatedIssues()).isEqualTo(0);
        assertThat(metrics.getUnestimatedIssuesByType().size()).isEqualTo(0);

    }

    @Test
    public void testOutsideOfSprint() {
        DbAggregationResult aggResults = DbAggregationResult
                .builder()
                .integrationId("1")
                .sprintId("1")
                .sprintName("Sprint 1")
                .sprintStartedAt(1000L)
                .sprintCompletedAt(2000L)
                .sprintMappingAggs(List.of(JiraIssueSprintMappingAggResult
                                .builder()
                                .issueType("Unknown")
                                .sprintMapping(DbJiraIssueSprintMapping.builder()
                                        .issueKey("A")
                                        .outsideOfSprint(true)
                                        .planned(false)
                                        .addedAt(900L)
                                        .delivered(true)
                                        .storyPointsPlanned(5)
                                        .storyPointsDelivered(3)
                                        .build())
                                .build(),
                        JiraIssueSprintMappingAggResult
                                .builder()
                                .issueType("Unknown")
                                .sprintMapping(DbJiraIssueSprintMapping.builder()
                                        .issueKey("B")
                                        .outsideOfSprint(true)
                                        .planned(false)
                                        .addedAt(900L)
                                        .delivered(true)
                                        .storyPointsPlanned(0)
                                        .storyPointsDelivered(0)
                                        .build())
                                .build()))
                .build();
        JiraSprintMetricsService.SprintMetrics metrics = service.generateSingleSprintMetrics("test", aggResults, SprintMetricsSettings.builder()
                .includeIssueKeys(true)
                .creepBuffer(0)
                .treatOutsideOfSprintAsPlannedAndDelivered(false)
                .build());
        // added at 900L < started at 1000L + buffer 0 -> YES BUT completed outside of sprint -> ignore
        assertThat(metrics.getCommittedKeys().size()).isEqualTo(0);
        assertThat(metrics.getCommitDeliveredKeys().size()).isEqualTo(0);
        assertThat(metrics.getCreepKeys().size()).isEqualTo(0);
        assertThat(metrics.getDeliveredCreepKeys().size()).isEqualTo(0);
        assertThat(metrics.getTotalIssues()).isEqualTo(0);
        assertThat(metrics.getStoryPointsByIssue().size()).isEqualTo(0);
        assertThat(metrics.getTotalUnestimatedIssues()).isEqualTo(0);
        assertThat(metrics.getUnestimatedIssuesByType().size()).isEqualTo(0);


        metrics = service.generateSingleSprintMetrics("test", aggResults, SprintMetricsSettings.builder()
                .includeIssueKeys(true)
                .creepBuffer(0)
                .treatOutsideOfSprintAsPlannedAndDelivered(true)
                .build());
        // added at 900L < started at 1000L + buffer 0 -> YES BUT completed outside of sprint
        // -> WITH treatOutsideOfSprintAsPlannedAndDelivered = TRUE-> committed AND delivered
        assertThat(metrics.getCommittedKeys().size()).isEqualTo(2);
        assertThat(metrics.getCommitDeliveredKeys().size()).isEqualTo(2);
        assertThat(metrics.getCreepKeys().size()).isEqualTo(0);
        assertThat(metrics.getDeliveredCreepKeys().size()).isEqualTo(0);
        assertThat(metrics.getTotalIssues()).isEqualTo(2);
        assertThat(metrics.getStoryPointsByIssue().size()).isEqualTo(2);
        assertThat(metrics.getTotalUnestimatedIssues()).isEqualTo(1);
        assertThat(metrics.getUnestimatedIssuesByType().size()).isEqualTo(1);

        // check with includeIssueKeys false that the numbers are still correct
        metrics = service.generateSingleSprintMetrics("test", aggResults, SprintMetricsSettings.builder()
                .includeIssueKeys(false)
                .creepBuffer(0)
                .treatOutsideOfSprintAsPlannedAndDelivered(true)
                .build());
        assertThat(metrics.getTotalIssues()).isEqualTo(2);
        assertThat(metrics.getStoryPointsByIssue().size()).isEqualTo(0);
        assertThat(metrics.getTotalUnestimatedIssues()).isEqualTo(1);
        assertThat(metrics.getUnestimatedIssuesByType().size()).isEqualTo(1);


    }
}
