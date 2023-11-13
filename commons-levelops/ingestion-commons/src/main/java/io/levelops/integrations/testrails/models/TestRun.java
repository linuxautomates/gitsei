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
 * Bean definition for testrails test run
 */
@Log4j2
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TestRun.TestRunBuilder.class)
public class TestRun {

    @JsonProperty("id")
    Integer id;

    @JsonProperty("name")
    String name;

    @JsonProperty("assignedto_id")
    Integer assignedToId;

    @JsonProperty("assignee")
    User assignee;

    @JsonProperty("blocked_count")
    Integer blockedCount;

    @JsonFormat(shape=JsonFormat.Shape.NUMBER, pattern="s")
    @JsonProperty("completed_on")
    Date completedOn;

    @JsonProperty("config")
    String config;

    @JsonProperty("config_ids")
    Integer[] configIds;

    @JsonProperty("created_by")
    Integer createdBy;

    @JsonProperty("creator")
    User creator;

    @JsonProperty("created_on")
    Long createdOn;

    @JsonProperty("description")
    String description;

    @JsonProperty("failed_count")
    Integer failedCount;

    @JsonProperty("include_all")
    Boolean includeAll;

    @JsonProperty("is_completed")
    Boolean isCompleted;

    @JsonProperty("milestone_id")
    Integer milestoneId;

    @JsonProperty("plan_id")
    Integer planId;

    @JsonProperty("passed_count")
    Integer passedCount;

    @JsonProperty("project_id")
    Integer projectId;

    @JsonProperty("retest_count")
    Integer retestCount;

    @JsonProperty("suite_id")
    Integer suiteId;

    @JsonProperty("untested_count")
    Integer untestedCount;

    @JsonProperty("updated_on")
    Long updatedOn;

    @JsonProperty("status_counts")
    Map<String, Integer> statusCounts;

    @JsonProperty("url")
    String url;

    @JsonProperty("refs")
    String refs;

    @JsonProperty("tests")
    List<Test> tests;

    @JsonAnySetter
    public void setProperty(String name, Integer value) {
        if (name != null && name.endsWith("_count")) {
            if (!(name.equals("untested_count") || name.equals("retest_count") ||
                    name.equals("passed_count") || name.equals("failed_count"))) {
                try {
                    statusCounts.put(name, value);
                } catch (NumberFormatException e) {
                    log.error("Error while setting value to custom status count " + e.getMessage());
                    statusCounts.put(name, 0);
                }
            }
        }
    }

    @JsonAnyGetter
    public Map<String, Integer> getProperty() {
        return this.statusCounts;
    }
}
