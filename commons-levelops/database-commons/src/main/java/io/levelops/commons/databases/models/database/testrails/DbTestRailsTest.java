package io.levelops.commons.databases.models.database.testrails;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.integrations.testrails.models.Test;
import io.levelops.integrations.testrails.models.User;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbTestRailsTest.DbTestRailsTestBuilder.class)
public class DbTestRailsTest {

    private static final String UNASSIGNED = "_UNASSIGNED_";

    @JsonProperty("id")
    String id;

    @JsonProperty("test_id")
    Integer testId;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("case_id")
    Integer caseId;

    @JsonProperty("milestone_id")
    Integer milestoneId;

    @JsonProperty("run_id")
    Integer runId;

    @JsonProperty("assignee")
    String assignee;

    @JsonProperty("priority")
    String priority;

    @JsonProperty("estimate_forecast")
    Long estimateForecast;

    @JsonProperty("estimate")
    Long estimate;

    @JsonProperty("title")
    String title;

    @JsonProperty("type")
    String type;

    @JsonProperty("refs")
    String refs;

    @JsonProperty("status")
    String status;

    @JsonProperty("test_run")
    String testRun;

    @JsonProperty("project")
    String project;

    @JsonProperty("milestone")
    String milestone;

    @JsonProperty("test_plan")
    String testPlan;

    @JsonProperty("results")
    List<DbTestRailsTestResult> results;

    @JsonProperty("defects")
    List<String> defects;

    @JsonProperty("custom_case_fields")
    Map<String, Object> customCaseFields;

    @JsonProperty("created_on")
    Instant created_on;

    public static DbTestRailsTest fromTest(Test test, String integrationId, Integer parentMilestoneId) {
        return DbTestRailsTest.builder()
                .integrationId(integrationId)
                .testId(test.getId())
                .caseId(test.getCaseId())
                .milestoneId(Optional.ofNullable(test.getMilestoneId()).orElse(parentMilestoneId))
                .runId(test.getRunId())
                .assignee(Optional.ofNullable(test.getAssignee()).map(User::getEmail).orElse(UNASSIGNED))
                .priority(MoreObjects.firstNonNull(test.getPriority(), UNASSIGNED))
                .type(MoreObjects.firstNonNull(test.getType(), UNASSIGNED))
                .estimate(parseDuration(test.getEstimate()))
                .estimateForecast(parseDuration(test.getEstimateForecast()))
                .title(test.getTitle())
                .refs(test.getRefs())
                .created_on(test.getCreatedOn()!= null ? Instant.ofEpochSecond(test.getCreatedOn()) : null)
                .results(CollectionUtils.emptyIfNull(test.getResults()).stream().map(testResult -> DbTestRailsTestResult.fromTestResult(testResult, integrationId)).collect(Collectors.toList()))
                .status(MoreObjects.firstNonNull(test.getStatus(), UNASSIGNED))
                .customCaseFields(test.getCustomCaseFields())
                .build();
    }

    private static Long parseDuration(String duration) {
        if (duration == null)
            return 0L;
        Matcher m = Pattern.compile("\\s*(?:(\\d+)\\s*(?:hours?|hrs?|h))?" +
                "\\s*(?:(\\d+)\\s*(?:minutes?|mins?|m))?" +
                "\\s*(?:(\\d+)\\s*(?:seconds?|secs?|s))?" +
                "\\s*", Pattern.CASE_INSENSITIVE)
                .matcher(duration);
        if (! m.matches())
            return 0L;
        int hours = (m.start(1) == -1 ? 0 : Integer.parseInt(m.group(1)));
        int minutes = (m.start(2) == -1 ? 0 : Integer.parseInt(m.group(2)));
        int seconds = (m.start(3) == -1 ? 0 : Integer.parseInt(m.group(3)));
        return TimeUnit.HOURS.toSeconds(hours) + TimeUnit.MINUTES.toSeconds(minutes) + seconds;
    }
}
