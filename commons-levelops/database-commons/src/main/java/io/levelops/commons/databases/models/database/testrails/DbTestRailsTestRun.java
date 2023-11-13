package io.levelops.commons.databases.models.database.testrails;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.testrails.models.TestRun;
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
@JsonDeserialize(builder = DbTestRailsTestRun.DbTestRailsTestRunBuilder.class)
public class DbTestRailsTestRun {

    private static final String UNASSIGNED = "_UNASSIGNED_";

    @JsonProperty("id")
    String id;

    @JsonProperty("run_id")
    Integer runId;

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

    @JsonProperty("plan_id")
    Integer planId;

    @JsonProperty("project_id")
    Integer projectId;

    @JsonProperty("updated_on")
    Instant updatedOn;

    @JsonProperty("ingested_at")
    Date ingestedAt;

    @JsonProperty("url")
    String url;

    @JsonProperty("refs")
    String refs;

    @JsonProperty("tests")
    List<DbTestRailsTest> tests;

    public static DbTestRailsTestRun fromTestRun(TestRun testRun, String integrationId, Integer parentMilestoneId) {
        final Integer milestoneId = Optional.ofNullable(testRun.getMilestoneId()).orElse(parentMilestoneId);
        return DbTestRailsTestRun.builder()
                .runId(testRun.getId())
                .integrationId(integrationId)
                .name(testRun.getName())
                .assignee(Optional.ofNullable(testRun.getAssignee()).map(User::getEmail).orElse(UNASSIGNED))
                .completedOn(testRun.getCompletedOn())
                .creator(Optional.ofNullable(testRun.getCreator()).map(User::getEmail).orElse(UNASSIGNED))
                .createdOn(testRun.getCreatedOn()!= null ? Instant.ofEpochSecond(testRun.getCreatedOn()) : null)
                .description(StringUtils.truncate(testRun.getDescription(), 100))
                .isCompleted(testRun.getIsCompleted())
                .milestoneId(milestoneId)
                .planId(testRun.getPlanId())
                .projectId(testRun.getProjectId())
                .updatedOn(testRun.getUpdatedOn()!= null ? Instant.ofEpochSecond(testRun.getUpdatedOn()) : null)
                .url(testRun.getUrl())
                .refs(testRun.getRefs())
                .tests(CollectionUtils.emptyIfNull(testRun.getTests()).stream()
                        .map(test -> DbTestRailsTest.fromTest(test, integrationId, milestoneId))
                        .collect(Collectors.toList()))
                .build();
    }
}
