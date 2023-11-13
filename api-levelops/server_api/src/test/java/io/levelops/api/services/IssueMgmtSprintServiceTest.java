package io.levelops.api.services;

import io.levelops.commons.databases.issue_management.DbIssueMgmtSprintMapping;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.IssueMgmtSprintMappingAggResult;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class IssueMgmtSprintServiceTest {
    private final IssueMgmtSprintService service = new IssueMgmtSprintService();

    @Test
    public void testSprintMetrics() {
        List<DbAggregationResult> aggResults = List.of(
                DbAggregationResult
                        .builder()
                        .integrationId("1")
                        .sprintId("Agile-Project\\Iteration 3")
                        .sprintName("Iteration 3")
                        .sprintStartedAt(1626652800L)
                        .sprintCompletedAt(0L)
                        .issueMgmtSprintMappingAggResults(List.of(IssueMgmtSprintMappingAggResult
                                .builder()
                                .workitemType("Task")
                                .sprintMapping(DbIssueMgmtSprintMapping.builder()
                                        .integrationId("1")
                                        .workitemId("143")
                                        .sprintId("Agile-Project\\Iteration 3")
                                        .outsideOfSprint(true)
                                        .planned(false)
                                        .addedAt(1629111380L)
                                        .delivered(false)
                                        .storyPointsPlanned((float) 5)
                                        .storyPointsDelivered((float) 2)
                                        .ignorableWorkitemType(false)
                                        .build())
                                .build()))
                        .build(),
                DbAggregationResult
                        .builder()
                        .integrationId("1")
                        .sprintId("Agile-Project\\Test")
                        .sprintName("Test")
                        .sprintCompletedAt(0L)
                        .sprintStartedAt(1627257600L)
                        .issueMgmtSprintMappingAggResults(List.of(
                                IssueMgmtSprintMappingAggResult
                                        .builder()
                                        .workitemType("Epic")
                                        .sprintMapping(DbIssueMgmtSprintMapping.builder()
                                                .integrationId("1")
                                                .workitemId("120")
                                                .sprintId("Agile-Project\\Test")
                                                .addedAt(1628768623L)
                                                .planned(false)
                                                .delivered(false)
                                                .outsideOfSprint(true)
                                                .ignorableWorkitemType(false)
                                                .storyPointsPlanned((float) 0)
                                                .storyPointsDelivered((float) 0)
                                                .build())
                                        .sprintMapping(DbIssueMgmtSprintMapping.builder()
                                                .integrationId("1")
                                                .workitemId("124")
                                                .sprintId("Agile-Project\\Test")
                                                .addedAt(1627900256L)
                                                .planned(false)
                                                .delivered(false)
                                                .outsideOfSprint(true)
                                                .ignorableWorkitemType(false)
                                                .storyPointsPlanned((float) 0)
                                                .storyPointsDelivered((float) 0)
                                                .build())
                                        .build()))
                        .build(),
                DbAggregationResult
                        .builder()
                        .integrationId("1")
                        .sprintId("Agile-Project\\sprint-2")
                        .sprintName("sprint-2")
                        .sprintStartedAt(1620691200L)
                        .sprintCompletedAt(0L)
                        .issueMgmtSprintMappingAggResults(List.of(
                                IssueMgmtSprintMappingAggResult
                                        .builder()
                                        .workitemType("User Story")
                                        .sprintMapping(DbIssueMgmtSprintMapping
                                                .builder()
                                                .integrationId("1")
                                                .workitemId("90")
                                                .sprintId("Agile-Project\\sprint-2")
                                                .addedAt(1624360067L)
                                                .planned(false)
                                                .delivered(false)
                                                .outsideOfSprint(true)
                                                .ignorableWorkitemType(false)
                                                .storyPointsPlanned((float) 0)
                                                .storyPointsDelivered((float) 0)
                                                .build())
                                        .build()
                                ,
                                IssueMgmtSprintMappingAggResult
                                        .builder()
                                        .workitemType("User Story")
                                        .sprintMapping(DbIssueMgmtSprintMapping
                                                .builder()
                                                .integrationId("1")
                                                .workitemId("105")
                                                .sprintId("Agile-Project\\sprint-2")
                                                .addedAt(1624358968L)
                                                .planned(false)
                                                .delivered(false)
                                                .outsideOfSprint(true)
                                                .ignorableWorkitemType(false)
                                                .storyPointsPlanned((float) 0)
                                                .storyPointsDelivered((float) 0)
                                                .build())
                                        .build()
                                ,
                                IssueMgmtSprintMappingAggResult
                                        .builder()
                                        .workitemType("Epic")
                                        .sprintMapping(DbIssueMgmtSprintMapping
                                                .builder()
                                                .integrationId("1")
                                                .workitemId("94")
                                                .sprintId("Agile-Project\\sprint-2")
                                                .addedAt(1629371003L)
                                                .planned(false)
                                                .delivered(false)
                                                .outsideOfSprint(true)
                                                .ignorableWorkitemType(false)
                                                .storyPointsPlanned((float) 0)
                                                .storyPointsDelivered((float) 0)
                                                .build())
                                        .build()
                        ))
                        .build()
        );
        List<IssueMgmtSprintService.SprintMetrics> test = aggResults.stream().map(r -> service.generateSingleSprintMetrics("test", r, IssueMgmtSprintService.SprintMetricsSettings.builder()
                        .includeWorkitemIds(true)
                        .treatOutsideOfSprintAsPlannedAndDelivered(true)
                        .build()))
                .collect(Collectors.toList());
        Assertions.assertThat(test.get(2).getCommittedKeys()).containsExactlyInAnyOrder("105", "90", "94");
        Assertions.assertThat(test.get(2).getCreepStoryPoints()).isEqualTo(0f);
        Assertions.assertThat(test.get(2).getTotalWorkitems()).isEqualTo(3);
        Assertions.assertThat(test.get(2).getDeliveredCreepKeys()).isEmpty();
        Assertions.assertThat(test.get(2).getCreepKeys()).isEmpty();
        Assertions.assertThat(test.get(2).getDeliveredKeys()).hasSize(3);
        Assertions.assertThat(test.get(2).getDeliveredKeys()).containsExactlyInAnyOrder("105", "90", "94");
    }

    @Test
    public void testOutsideOfSprintSetting() {
        DbAggregationResult aggResult = DbAggregationResult
                .builder()
                .integrationId("1")
                .sprintId("Agile-Project\\Iteration 3")
                .sprintName("Iteration 3")
                .sprintStartedAt(1626652800L)
                .sprintCompletedAt(0L)
                .issueMgmtSprintMappingAggResults(List.of(IssueMgmtSprintMappingAggResult
                        .builder()
                        .workitemType("Task")
                        .sprintMapping(DbIssueMgmtSprintMapping.builder()
                                .integrationId("1")
                                .workitemId("143")
                                .sprintId("Agile-Project\\Iteration 3")
                                .outsideOfSprint(true)
                                .planned(false)
                                .addedAt(1629111380L)
                                .delivered(false)
                                .storyPointsPlanned(5f)
                                .storyPointsDelivered(2f)
                                .ignorableWorkitemType(false)
                                .build())
                        .build()))
                .build();
        IssueMgmtSprintService.SprintMetrics test = service.generateSingleSprintMetrics("test", aggResult, IssueMgmtSprintService.SprintMetricsSettings.builder()
                        .includeWorkitemIds(true)
                        .treatOutsideOfSprintAsPlannedAndDelivered(true)
                        .build());
        Assertions.assertThat(test.getCommittedKeys()).containsExactlyInAnyOrder("143");
        Assertions.assertThat(test.getCreepStoryPoints()).isEqualTo(0f);
        Assertions.assertThat(test.getTotalWorkitems()).isEqualTo(1);
        Assertions.assertThat(test.getDeliveredCreepKeys()).isEmpty();
        Assertions.assertThat(test.getCreepKeys()).isEmpty();
        Assertions.assertThat(test.getDeliveredKeys()).containsExactlyInAnyOrder("143");


        test = service.generateSingleSprintMetrics("test", aggResult, IssueMgmtSprintService.SprintMetricsSettings.builder()
                .includeWorkitemIds(true)
                .treatOutsideOfSprintAsPlannedAndDelivered(false)
                .build());
        Assertions.assertThat(test.getCommittedKeys()).isEmpty();
        Assertions.assertThat(test.getDeliveredKeys()).isEmpty();
        Assertions.assertThat(test.getTotalWorkitems()).isEqualTo(0);
        Assertions.assertThat(test.getCreepKeys()).isEmpty();
        Assertions.assertThat(test.getDeliveredCreepKeys()).isEmpty();
        Assertions.assertThat(test.getCreepStoryPoints()).isEqualTo(0f);
    }
}
