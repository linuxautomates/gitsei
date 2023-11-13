package io.levelops.integrations.testrails.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bean definition for testrails test
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class Test {

    public Test() {
        this(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    Map<String, Object> dynamicCustomFields = new HashMap<>();
    @JsonAnySetter
    public void setDynamicCustomFields(String key, Object value) {
        if (key == null || value == null || !key.startsWith("custom_")) {
            return;
        }
        dynamicCustomFields.put(key, value);
    }
    @JsonAnyGetter
    public Map<String, Object> getDynamicCustomFields() {
        return dynamicCustomFields;
    }

    @JsonProperty("custom_case_fields")
    Map<String, Object> customCaseFields;

    @JsonProperty("id")
    Integer id;

    @JsonProperty("case_id")
    Integer caseId;

    @JsonProperty("status_id")
    Integer statusId;

    @JsonProperty("assignedto_id")
    Integer assignedToId;

    @JsonProperty("run_id")
    Integer runId;

    @JsonProperty("title")
    String title;

    @JsonProperty("template_id")
    Integer templateId;

    @JsonProperty("type_id")
    Integer typeId;

    @JsonProperty("priority_id")
    Integer priorityId;

    @JsonProperty("estimate_forecast")
    String estimateForecast;

    @JsonProperty("estimate")
    String estimate;

    @JsonProperty("refs")
    String refs;

    @JsonProperty("milestone_id")
    Integer milestoneId;

    @JsonProperty("sections_display_order")
    Integer sectionsDisplayOrder;

    @JsonProperty("cases_display_order")
    Integer casesDisplayOrder;

    @JsonProperty("type")
    String type;

    @JsonProperty("status")
    String status;

    @JsonProperty("assignee")
    User assignee;

    @JsonProperty("priority")
    String priority;

    @JsonProperty("results")
    List<Result> results;
    @JsonProperty("created_on")
    Long createdOn;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Status.StatusBuilder.class)
    public static class Status {

        @JsonProperty("id")
        Integer id;

        @JsonProperty("name")
        String name;

        @JsonProperty("label")
        String label;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CaseType.CaseTypeBuilder.class)
    public static class CaseType {

        @JsonProperty("id")
        Integer id;

        @JsonProperty("name")
        String name;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Priority.PriorityBuilder.class)
    public static class Priority {

        @JsonProperty("id")
        Integer id;

        @JsonProperty("name")
        String name;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CustomStepsSeparated.CustomStepsSeparatedBuilder.class)
    public static class CustomStepsSeparated {

        @JsonProperty("content")
        String content;

        @JsonProperty("expected")
        String expected;

        @JsonProperty("additional_info")
        String additionalInfo;

        @JsonProperty("refs")
        String refs;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Result.ResultBuilder.class)
    public static class Result {
        @JsonProperty("id")
        Long id;
        @JsonProperty("test_id")
        Integer testId;
        @JsonProperty("status_id")
        Integer statusId;
        @JsonProperty("status")
        String status;
        @JsonProperty("created_on")
        Long createdOn;
        @JsonProperty("assignedto_id")
        Integer assignedToId;
        @JsonProperty("assignee")
        User assignee;
        @JsonProperty("comment")
        String comment;
        @JsonProperty("elapsed")
        String elapsed;
        @JsonProperty("defects")
        String defects;
        @JsonProperty("created_by")
        Integer createdBy;
        @JsonProperty("creator")
        User creator;
    }
}
