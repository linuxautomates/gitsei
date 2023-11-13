package io.levelops.integrations.testrails.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Bean definition for testrails test plan
 */
@Log4j2
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TestPlan.TestPlanBuilder.class)
public class TestPlan {

    @JsonProperty("id")
    Integer id;

    @JsonProperty("name")
    String name;

    @JsonProperty("assignedto_id")
    Integer assignedtoId;

    @JsonProperty("assignee")
    User assignee;

    @JsonProperty("blocked_count")
    Integer blockedCount;

    @JsonFormat(shape=JsonFormat.Shape.NUMBER, pattern="s")
    @JsonProperty("completed_on")
    Date completedOn;

    @JsonProperty("created_by")
    Integer createdBy;

    @JsonProperty("creator")
    User creator;

    @JsonProperty("created_on")
    Long createdOn;

    @JsonProperty("description")
    String description;

    @JsonProperty("entries")
    Entry[] entries;

    @JsonProperty("failed_count")
    Integer failedCount;

    @JsonProperty("is_completed")
    Boolean isCompleted;

    @JsonProperty("milestone_id")
    Integer milestoneId;

    @JsonProperty("passed_count")
    Integer passedCount;

    @JsonProperty("project_id")
    Integer projectId;

    @JsonProperty("retest_count")
    Integer retestCount;

    @JsonProperty("untested_count")
    Integer untestedCount;

    @JsonProperty("status_counts")
    Map<String, Integer> statusCounts;

    @JsonProperty("url")
    String url;

    @JsonProperty("refs")
    String refs;

    @JsonAnySetter
    public void setProperty(final String name, final String value) {
        if (name != null && name.endsWith("_count")) {
            if (!(name.equals("untested_count") || name.equals("retest_count") ||
                    name.equals("passed_count") || name.equals("failed_count"))) {
                try {
                    this.statusCounts.put(name, Integer.valueOf(value));
                } catch (NumberFormatException e) {
                    log.error("Error while setting value to custom status count " + e.getMessage());
                    this.statusCounts.put(name, 0);
                }
            }
        }
    }

    @JsonAnyGetter
    public Map<String, Integer> getProperty() {
        return this.statusCounts;
    }

    @JsonProperty("test_runs")
    List<TestRun> testRuns;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Entry.EntryBuilder.class)
    public static class Entry {

        @JsonProperty("id")
        String id;

        @JsonProperty("description")
        String description;

        @JsonProperty("include_all")
        Boolean includeAll;

        @JsonProperty("name")
        String name;

        @JsonProperty("runs")
        TestRun[] runs;

        @JsonProperty("refs")
        String refs;

        @JsonProperty("suite_id")
        Integer suiteId;
    }
}
