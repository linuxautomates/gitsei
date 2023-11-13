package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Value
@AllArgsConstructor
@Builder(toBuilder = true)
public class JiraIssueFields {

    public JiraIssueFields() {
        this(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    Map<String, Object> dynamicFields = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getDynamicFields() {
        return dynamicFields;
    }

    @JsonAnySetter
    public void addDynamicField(String key, Object value) {
        if (value == null) {
            return;
        }
        dynamicFields.put(key, value);
    }

    @JsonProperty("issuetype")
    JiraIssueType issueType;

    @JsonProperty("project")
    JiraProject project;

    @JsonProperty("fixVersions")
    List<JiraVersion> fixVersions;

    @JsonProperty("resolution")
    JiraIssueResolution resolution;

    @JsonProperty("resolutiondate")
    Date resolutiondate;

    @JsonProperty("created")
    Date created;

    @JsonProperty("priority")
    JiraPriority priority;

    @JsonProperty("labels")
    List<String> labels;

    @JsonProperty("versions")
    List<JiraVersion> versions;

    @JsonProperty("issuelinks")
    List<JiraIssueLink> issueLinks;

    @JsonProperty("assignee")
    JiraUser assignee;

    @JsonProperty("updated")
    Date updated;

    @JsonProperty("status")
    JiraStatus status;

    @JsonProperty("components")
    List<JiraComponent> components;

    @JsonProperty("description")
    protected final Object description; // JiraContent

    public Object getDescription() {
        // setting to Null since aggregation framework only relies on description_text and won't have to normalize anything
        return null;
    }

    @JsonProperty("description_text")
    public String getDescriptionText() {
        return JiraContent.toString(description);
    }

    @JsonProperty("description_length")
    public Integer getDescriptionLength() {
        return getDescriptionText().length();
    }

    @JsonProperty("summary")
    String summary;

    @JsonProperty("creator")
    JiraUser creator;

    @JsonProperty("subtasks")
    List<JiraIssue> subtasks;

    @JsonProperty("reporter")
    JiraUser reporter;


    //    @JsonProperty("security")
//    Object security; // TODO
//
//    @JsonProperty("environment")
//    Object environment; // TODO
//
    @JsonProperty("duedate")
    String dueDate; // e.g "2020-02-20"

    @JsonProperty("progress")
    JiraIssueProgress progress;

    @JsonProperty("votes")
    JiraIssueVotes votes;

//    @JsonProperty("watches")
//    Object watches; // TODO
//
//    @JsonProperty("timetracking")
//    JiraApiTimeTracking timeTracking; // TODO unverified

    @JsonProperty("comment")
    JiraCommentsResult comment;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = JiraIssueLink.JiraIssueLinkBuilder.class)
    public static class JiraIssueLink {

        @JsonProperty("self")
        String self;

        @JsonProperty("id")
        String id;

        @JsonProperty("type")
        JiraIssueLinkType type;

        @JsonProperty("inwardIssue")
        JiraIssue inwardIssue;

        @JsonProperty("outwardIssue")
        JiraIssue outwardIssue;
    }


    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = JiraIssueLinkType.JiraIssueLinkTypeBuilder.class)
    public static class JiraIssueLinkType {
        @JsonProperty("id")
        String id;
        @JsonProperty("name")
        String name;
        @JsonProperty("inward")
        String inward;
        @JsonProperty("outward")
        String outward;
    }


    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = JiraPriority.JiraPriorityBuilder.class)
    public static class JiraPriority {
        @JsonProperty("name")
        String name;

        @JsonProperty("id")
        String id;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = JiraStatus.JiraStatusBuilder.class)
    public static class JiraStatus {
        @JsonProperty("self")
        String self;
        @JsonProperty("description")
        String description;
        @JsonProperty("name")
        String name;
        @JsonProperty("id")
        String id;

        @JsonProperty("statusCategory")
        JiraStatusCategory statusCategory;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = JiraStatusCategory.JiraStatusCategoryBuilder.class)
        public static class JiraStatusCategory {
            @JsonProperty("self")
            String self;
            @JsonProperty("id")
            String id;
            @JsonProperty("key")
            String key;
            @JsonProperty("name")
            String name;
            @JsonProperty("colorName")
            String colorName;
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = JiraIssueProgress.JiraIssueProgressBuilder.class)
    public static class JiraIssueProgress {
        @JsonProperty("progress")
        Long progress;
        @JsonProperty("total")
        Long total;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = JiraIssueVotes.JiraIssueVotesBuilder.class)
    public static class JiraIssueVotes {
        @JsonProperty("votes")
        Long votes;
        @JsonProperty("hasVoted")
        Boolean hasVoted;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = JiraTimeTracking.JiraTimeTrackingBuilder.class)
    public static class JiraTimeTracking {
        @JsonProperty("original_estimate")
        String originalEstimate;
        @JsonProperty("remaining_estimate")
        String remainingEstimate;
        @JsonProperty("time_spent")
        String timeSpent;
        @JsonProperty("original_estimate_seconds")
        Long originalEstimateSeconds;
        @JsonProperty("remaining_estimate_seconds")
        Long remainingEstimateSeconds;
        @JsonProperty("time_spent_seconds")
        Long timeSpentSeconds;

    }


    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = JiraIssueResolution.JiraIssueResolutionBuilder.class)
    public static class JiraIssueResolution {
        @JsonProperty("self")
        String self;
        @JsonProperty("id")
        String id;
        @JsonProperty("description")
        String description;
        @JsonProperty("name")
        String name;
    }
}
