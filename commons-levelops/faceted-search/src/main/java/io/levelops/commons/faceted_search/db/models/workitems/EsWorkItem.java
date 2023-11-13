package io.levelops.commons.faceted_search.db.models.workitems;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.faceted_search.db.models.EsIntegrationUser;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = EsWorkItem.EsWorkItemBuilder.class)
public class EsWorkItem implements EsObject {
    //Every single top level object should start with w_ , parent_level is an exception, because parent_level is not used in main object it is used in parent
    @JsonProperty("parent_level")
    Integer parentLevel;

    @JsonProperty("w_id")
    UUID id;

    @JsonProperty("w_workitem_id")
    String workitemId;

    @JsonProperty("w_integration_id")
    Integer integrationId;

    @JsonProperty("w_workitem_integ_id")
    String workitemIntegId;

    @JsonProperty("w_is_active")
    Boolean isActive;

    @JsonProperty("w_summary")
    String summary;

    @JsonProperty("w_priority")
    String priority;

    @JsonProperty("w_project")
    String project;

    @JsonProperty("w_epic")
    String epic;

    @JsonProperty("w_parent_workitem_id")
    String parentWorkItemId;

    @JsonProperty("w_status")
    String status;

    @JsonProperty("w_workitem_type")
    String workItemType;

    @JsonProperty("w_story_points")
    Float storyPoints;

    @JsonProperty("w_custom_fields")
    List<EsExtensibleField> customFields;

    @JsonProperty("w_components")
    List<String> components;

    @JsonProperty("w_labels")
    List<String> labels;

    @JsonProperty("w_assignee")
    EsIntegrationUser assignee;

    @JsonProperty("w_reporter")
    EsIntegrationUser reporter;

    @JsonProperty("w_first_assignee")
    EsIntegrationUser firstAssignee;

    @JsonProperty("w_desc_size")
    Integer descSize;

    @JsonProperty("w_hops")
    Integer hops;

    @JsonProperty("w_bounces")
    Integer bounces;

    @JsonProperty("w_num_attachments")
    Integer numAttachments;

    @JsonProperty("w_priority_order")
    Integer priorityOrder;

    @JsonProperty("w_created_at")
    Timestamp createdAt;

    @JsonProperty("w_updated_at")
    Timestamp updatedAt;

    @JsonProperty("w_resolved_at")
    Timestamp resolvedAt;

    @JsonProperty("w_due_at")
    Timestamp dueAt;

    @JsonProperty("w_first_assigned_at")
    Timestamp firstAssignedAt;

    @JsonProperty("w_first_comment_at")
    Timestamp firstCommentAt;

    @JsonProperty("w_first_attachment_at")
    Timestamp firstAttachmentAt;

    @JsonProperty("w_age")
    Long age;

    @JsonProperty("w_unestimated_ticket")
    Integer unestimatedTicket;

    @JsonProperty("w_ingested_at")
    Timestamp ingestedAt;

    @JsonProperty("w_state_transition_time")
    Timestamp stateTransitionTime;

    @JsonProperty("w_resolution")
    String resolution;

    @JsonProperty("w_status_category")
    String statusCategory;

    @JsonProperty("w_original_estimate")
    Float originalEstimate;

    @JsonProperty("w_attributes")
    List<EsExtensibleField> attributes;

    @JsonProperty("w_milestones")
    List<EsMilestone> milestones;

    @JsonProperty("w_versions")
    List<EsVersion> versions;

    @JsonProperty("w_fix_versions")
    List<EsVersion> fixVersions;

    @JsonProperty("w_sprint_mappings")
    List<EsSprintMapping> sprintMappings;

    @JsonProperty("w_links")
    List<EsLink> links;

    @JsonProperty("w_salesforce_cases")
    List<EsSalesforceCase> salesforceCases;

    @JsonProperty("w_priorities_sla")
    EsPrioritiesSla prioritiesSla;

    @JsonProperty("w_sprints")
    List<EsSprint> sprints;

    @JsonProperty("w_hist_assignees")
    List<EsHistoricalAssignee> histAssignees;

    @JsonProperty("w_hist_statuses")
    List<EsHistoricalStatus> histStatuses;

    @JsonProperty("w_hist_sprints")
    List<EsHistoricalSprint> historicalSprints;

    @JsonProperty("w_hist_story_points")
    List<EsHistoricalStoryPoints> historicalStoryPoints;

    @JsonProperty("w_hist_state_transitions")
    List<EsHistoricalStateTransition> historicalStateTransitions;

    @JsonProperty("w_integ_type")
    String integrationType;

    @JsonProperty("w_hist_assignee_statuses")
    List<EsHistAssigneeStatus> esHistAssigneeStatuses;

    @JsonProperty("w_parents")
    List<EsWorkItem> parents;

    @JsonProperty("w_epic_wi")
    EsWorkItem epicWorkItem;

    @JsonIgnore
    @Override
    public String generateESId(String company) {
        return company + "_" + ingestedAt + "_" + integrationId + "_" + workitemId;
    }

    @JsonIgnore
    @Override
    public Long getOffset() {
        return (updatedAt == null) ? null : updatedAt.toInstant().getEpochSecond();
    }

    @Data
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = EsHistoricalStoryPoints.EsHistoricalStoryPointsBuilder.class)
    public static class EsHistoricalStoryPoints {

        @JsonProperty("story_points")
        Float storyPoints;

        @JsonProperty("start_time")
        Timestamp startTime;

        @JsonProperty("end_time")
        Timestamp endTime;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = EsHistoricalSprint.EsHistoricalSprintBuilder.class)
    public static class EsHistoricalSprint {

        @JsonProperty("id")
        String sprintId;

        @JsonProperty("name")
        String name;

        @JsonProperty("state")
        String state;

        @JsonProperty("goal")
        String goal;

        @JsonProperty("start_time")
        Timestamp startTime;

        @JsonProperty("end_time")
        Timestamp endTime;

        @JsonProperty("completed_at")
        Timestamp completedAt;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = EsSprint.EsSprintBuilder.class)
    public static class EsSprint {

        @JsonProperty("id")
        String sprintId;

        @JsonProperty("name")
        String name;

        @JsonProperty("state")
        String state;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = EsHistoricalStatus.EsHistoricalStatusBuilder.class)
    public static class EsHistoricalStatus {

        @JsonProperty("time_spent")
        Long timeSpent;

        @JsonProperty("status")
        String status;

        @JsonProperty("id")
        String statusId;

        @JsonProperty("start_time")
        Timestamp startTime;

        @JsonProperty("end_time")
        Timestamp endTime;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = EsHistoricalAssignee.EsHistoricalAssigneeBuilder.class)
    public static class EsHistoricalAssignee {

        @JsonProperty("assignee")
        EsIntegrationUser assignee;

        @JsonProperty("start_time")
        Timestamp startTime;

        @JsonProperty("end_time")
        Timestamp endTime;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = EsSalesforceCase.EsSalesforceCaseBuilder.class)
    public static class EsSalesforceCase {

        @JsonProperty("field_key")
        String fieldKey;

        @JsonProperty("field_value")
        String fieldValue;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = EsVersion.EsVersionBuilder.class)
    public static class EsVersion {

        @JsonProperty("id")
        Integer id;

        @JsonProperty("name")
        String name;

        @JsonProperty("description")
        String description;

        @JsonProperty("archived")
        Boolean archived;

        @JsonProperty("released")
        Boolean released;

        @JsonProperty("overdue")
        Boolean overdue;

        @JsonProperty("start_time")
        Timestamp startTime;

        @JsonProperty("end_time")
        Timestamp endTime;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = EsLink.EsLinkBuilder.class)
    public static class EsLink {

        @JsonProperty("to_workitem_id")
        String toWorkitemId;

        @JsonProperty("relation")
        String relation;

        @JsonProperty("to_project")
        String toProject;
    }

    @Data
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = EsMilestone.EsMilestoneBuilder.class)
    public static class EsMilestone {

        @JsonProperty("id")
        String id;

        @JsonProperty("name")
        String name;

        @JsonProperty("parent_name")
        String parentName;

        @JsonProperty("full_name")
        String fullName;

        @JsonProperty("state")
        String state;

        @JsonProperty("start_time")
        Timestamp startTime;

        @JsonProperty("end_time")
        Timestamp endTime;

        @JsonProperty("completed_at")
        Timestamp completedAt;

        @JsonProperty("attributes")
        List<EsExtensibleField> attributes;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = EsSprintMapping.EsSprintMappingBuilder.class)
    public static class EsSprintMapping {

        @JsonProperty("id")
        String id;

        @JsonProperty("sprint_id")
        String sprintId;

        @JsonProperty("added_at")
        Long addedAt;

        @JsonProperty("planned")
        Boolean planned;

        @JsonProperty("delivered")
        Boolean delivered;

        @JsonProperty("outside_of_sprint")
        Boolean outsideOfSprint;

        @JsonProperty("ignorable_workitem_type")
        Boolean ignorableIssueType;

        @JsonProperty("story_points_planned")
        Float storyPointsPlanned;

        @JsonProperty("story_points_delivered")
        Float storyPointsDelivered;
    }

    @Data
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = EsPrioritiesSla.EsPrioritiesSlaBuilder.class)
    public static class EsPrioritiesSla {

        @JsonProperty("response_time")
        Long responseTime;

        @JsonProperty("solve_time")
        Long solveTime;
    }

    @Data
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = EsHistoricalStateTransition.EsHistoricalStateTransitionBuilder.class)
    public static class EsHistoricalStateTransition {

        @JsonProperty("to_status")
        String toStatus;

        @JsonProperty("from_status")
        String fromStatus;

        @JsonProperty("state_transition_time")
        Long stateTransitionTime;
    }

    @Data
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = EsHistAssigneeStatus.EsHistAssigneeStatusBuilder.class)
    public static class EsHistAssigneeStatus {

        @JsonProperty("historical_assignee")
        String historicalAssignee;

        @JsonProperty("historical_assignee_id")
        String historicalAssigneeId;

        @JsonProperty("issue_status")
        String issueStatus;

        @JsonProperty("issue_status_id")
        String issueStatusId;

        @JsonProperty("issue_status_category")
        String issueStatusCategory;

        @JsonProperty("assignee_start_time")
        Long assigneeStartTime;

        @JsonProperty("assignee_end_time")
        Long assigneeEndTime;

        @JsonProperty("status_start_time")
        Long statusStartTime;

        @JsonProperty("status_end_time")
        Long statusEndTime;

        @JsonProperty("hist_assignee_time")
        Long histAssigneeTime;

        @JsonProperty("hist_assignee_time_excluding_resolution")
        Long histAssigneeTimeExcludingResolution;

        @JsonProperty("interval_start_time")
        Long intervalStartTime;

        @JsonProperty("interval_end_time")
        Long intervalEndTime;

        //This is same as jira issue/wi issue_resolved_at. It is duplicated only due to limitations in ES Composite Aggs
        @JsonProperty("w_resolved_at")
        Timestamp resolvedAt;
    }

}

