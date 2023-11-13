package io.levelops.commons.databases.models.database.testrails;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.integrations.testrails.models.TestPlan;
import io.levelops.integrations.testrails.models.User;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbTestRailsTestPlan.DbTestRailsTestPlanBuilder.class)
public class DbTestRailsTestPlan {

    private static final String UNASSIGNED = "_UNASSIGNED_";

    @JsonProperty("id")
    String id;

    @JsonProperty("plan_id")
    Integer planId;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("name")
    String name;

    @JsonProperty("assignee")
    String assignee;

    @JsonProperty("completed_on")
    Date completedOn;

    @JsonProperty("creator")
    String creator;

    @JsonProperty("created_on")
    Instant createdOn;

    @JsonProperty("description")
    String description;

    @JsonProperty("is_completed")
    Boolean isCompleted;

    @JsonProperty("milestone_id")
    Integer milestoneId;

    @JsonProperty("project_id")
    Integer projectId;

    @JsonProperty("url")
    String url;

    @JsonProperty("refs")
    String refs;

    @JsonProperty("ingested_at")
    Date ingestedAt;

    @JsonProperty("test_runs")
    List<DbTestRailsTestRun> testRuns;

    public static DbTestRailsTestPlan fromTestPlan(TestPlan testPlan, String integrationId) {
        return DbTestRailsTestPlan.builder()
                .integrationId(integrationId)
                .planId(testPlan.getId())
                .name(testPlan.getName())
                .assignee(Optional.ofNullable(testPlan.getAssignee()).map(User::getEmail).orElse(UNASSIGNED))
                .completedOn(testPlan.getCompletedOn())
                .creator(Optional.ofNullable(testPlan.getCreator()).map(User::getEmail).orElse(UNASSIGNED))
                .createdOn(testPlan.getCreatedOn() != null ? Instant.ofEpochSecond(testPlan.getCreatedOn()) : null)
                .description(StringUtils.truncate(testPlan.getDescription(), 100))
                .isCompleted(MoreObjects.firstNonNull(testPlan.getIsCompleted(), false))
                .milestoneId(testPlan.getMilestoneId())
                .projectId(testPlan.getProjectId())
                .url(testPlan.getUrl())
                .refs(testPlan.getRefs())
                .testRuns(CollectionUtils.emptyIfNull(testPlan.getTestRuns()).stream()
                        .map(testRun -> DbTestRailsTestRun.fromTestRun(testRun, integrationId,
                                testPlan.getMilestoneId()))
                        .collect(Collectors.toList()))
                .build();
    }
}
