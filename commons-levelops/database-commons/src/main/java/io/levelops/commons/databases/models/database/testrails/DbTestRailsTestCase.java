package io.levelops.commons.databases.models.database.testrails;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.integrations.testrails.models.TestRailsTestCase;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbTestRailsTestCase.DbTestRailsTestCaseBuilder.class)
public class DbTestRailsTestCase {

    private static final String UNASSIGNED = "_UNASSIGNED_";
    private static Pattern PATTERN = Pattern.compile("\\s*(?:(\\d+)\\s*(?:hours?|hrs?|h))?" +
            "\\s*(?:(\\d+)\\s*(?:minutes?|mins?|m))?" +
            "\\s*(?:(\\d+)\\s*(?:seconds?|secs?|s))?" +
            "\\s*", Pattern.CASE_INSENSITIVE);

    @JsonProperty("id")
    UUID id;
    @JsonProperty("case_id")
    Integer caseId;
    @JsonProperty("integration_id")
    String integrationId;
    @JsonProperty("project_id")
    Integer projectId;
    @JsonProperty("suite_id")
    Integer suiteId;
    @JsonProperty("milestone_id")
    Integer milestoneId;
    @JsonProperty("title")
    String title;
    @JsonProperty("type")
    String type;
    @JsonProperty("priority")
    String priority;
    @JsonProperty("refs")
    String refs;
    @JsonProperty("created_by")
    String createdBy;
    @JsonProperty("created_on")
    Instant createdOn;
    @JsonProperty("updated_by")
    String updatedBy;
    @JsonProperty("updated_on")
    Instant updatedOn;
    @JsonProperty("estimate")
    Long estimate;
    @JsonProperty("estimate_forecast")
    Long estimateForecast;
    @JsonProperty("custom_case_fields")
    Map<String, Object> customCaseFields;
    public static DbTestRailsTestCase fromTestCase(TestRailsTestCase testCase, String integrationId) {
        return DbTestRailsTestCase.builder()
                .caseId(testCase.getId())
                .integrationId(integrationId)
                .projectId(testCase.getProjectId())
                .suiteId(testCase.getSuiteId())
                .milestoneId(testCase.getMilestoneId())
                .title(testCase.getTitle())
                .type(MoreObjects.firstNonNull(testCase.getType(), UNASSIGNED))
                .priority(MoreObjects.firstNonNull(testCase.getPriority(), UNASSIGNED))
                .refs(testCase.getRefs())
                .createdBy(testCase.getCreatedByUser())
                .createdOn(Instant.ofEpochSecond(testCase.getCreatedOn()))
                .updatedBy(testCase.getUpdatedByUser())
                .updatedOn(Instant.ofEpochSecond(testCase.getUpdatedOn()))
                .estimate(parseDuration(testCase.getEstimate()))
                .estimateForecast(parseDuration(testCase.getEstimateForecast()))
                .customCaseFields(testCase.getCustomCaseFields())
                .build();
    }

    private static Long parseDuration(String duration) {
        if (duration == null)
            return 0L;
        Matcher m = PATTERN.matcher(duration);
        if (! m.matches())
            return 0L;
        int hours = (m.start(1) == -1 ? 0 : Integer.parseInt(m.group(1)));
        int minutes = (m.start(2) == -1 ? 0 : Integer.parseInt(m.group(2)));
        int seconds = (m.start(3) == -1 ? 0 : Integer.parseInt(m.group(3)));
        return TimeUnit.HOURS.toSeconds(hours) + TimeUnit.MINUTES.toSeconds(minutes) + seconds;
    }
}