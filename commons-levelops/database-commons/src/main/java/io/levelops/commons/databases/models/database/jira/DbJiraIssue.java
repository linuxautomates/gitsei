package io.levelops.commons.databases.models.database.jira;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.jackson.DefaultObjectMapper;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbJiraIssue.DbJiraIssueBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Log4j2
public class DbJiraIssue {
    public static final String UNASSIGNED = "_UNASSIGNED_";
    public static final String UNKNOWN = "_UNKNOWN_";
    public static final String DEFAULT_STORY_POINTS_LOG = "0";
    public static final String UNPRIORITIZED = "_UNPRIORITIZED_";

    @JsonProperty("id")
    String id;

    @JsonProperty("key")
    String key;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("project")
    String project;

    @JsonProperty("summary")
    String summary;

    @JsonProperty("status")
    String status;

    @JsonProperty("is_active")
    Boolean isActive;

    @JsonProperty("issue_type")
    String issueType;

    @JsonProperty("priority")
    String priority;

    @JsonProperty("assignee")
    String assignee;

    @JsonProperty("assignee_id")
    String assigneeId;

    @JsonProperty("reporter")
    String reporter;

    @JsonProperty("reporter_id")
    String reporterId;

    @JsonProperty("epic")
    String epic;

    @JsonProperty("epic_summary")
    String epicSummary;

    @JsonProperty("parent_key")
    String parentKey;

    @JsonProperty("parent_issue_type")
    String parentIssueType;

    @JsonProperty("parent_labels")
    List<String> parentLabels;

    @JsonProperty("desc_size")
    Integer descSize;

    @JsonProperty("hops")
    Integer hops;

    @JsonProperty("bounces")
    Integer bounces;

    @JsonProperty("num_attachments")
    Integer numAttachments;

    @JsonProperty("first_attachment_at")
    Long firstAttachmentAt;

    @JsonProperty("issue_created_at")
    Long issueCreatedAt;

    @JsonProperty("issue_updated_at")
    Long issueUpdatedAt;

    @JsonProperty("issue_resolved_at")
    Long issueResolvedAt;

    @JsonProperty("issue_due_at")
    Long issueDueAt;

    @JsonProperty("original_estimate")
    Long originalEstimate;

    @JsonProperty("first_comment_at")
    Long firstCommentAt;

    @JsonProperty("first_assigned_at")
    Long firstAssignedAt;

    @JsonProperty("first_assignee")
    String firstAssignee;

    @JsonProperty("first_assignee_id")
    String firstAssigneeId;

    @JsonProperty("ingested_at")
    Long ingestedAt; //RESOLUTION IS TO THE DATE ONLY. seconds and hours will not be stored

    @JsonProperty("created_at")
    Long createdAt;

    @JsonProperty("state_transition_time")
    Long stateTransitionTime;

    @JsonProperty("custom_fields")
    Map<String, Object> customFields;
    
    @JsonProperty("config_version")
    Long configVersion;

    @JsonProperty("salesforce_fields")
    Map<String, List<String>> salesforceFields;

    //this is the current list of labels on the ticket
    @JsonProperty("labels")
    List<String> labels;

    //this is the current list of fix versions on the ticket
    @JsonProperty("fix_versions")
    List<String> fixVersions;

    //this is the current list of versions on the ticket -- refers to affects versions i think
    @JsonProperty("versions")
    List<String> versions;

    //this is the current list of components on the ticket. stored in a separate table
    @JsonProperty("component_list")
    List<String> components;

    //this is the historical list of statuses and duration of each. stored in a separate table
    @JsonProperty("status_list")
    List<DbJiraStatus> statuses;

    //this is the historical list of assignees and duration of each. stored in a separate table
    @JsonProperty("assignee_list")
    List<DbJiraAssignee> assigneeList;

    // enriched
    @JsonProperty("priority_order")
    Integer priorityOrder;

    @JsonProperty("story_points")
    Integer storyPoints;

    @JsonIgnore
    List<DbJiraStoryPoints> storyPointsLogs;

    @JsonProperty("sprint_ids")
    List<Integer> sprintIds;

    @JsonIgnore
    Map<String, List<JiraIssueSprintEvent>> sprintEvents;

    @JsonProperty("resolution")
    String resolution;

    @JsonProperty("status_category")
    String statusCategory;

    // enriched
    @JsonProperty("ticket_category")
    String ticketCategory;

    @JsonProperty("velocity_stage")
    String velocityStage;

    @JsonProperty("release_time")
    Long releaseTime;

    @JsonProperty("released_end_time")
    Long releaseEndTime;

    @JsonProperty("fix_version")
    String fixVersion;

    @JsonProperty("velocity_stage_total_time")
    Long velocityStageTotalTime;

    @JsonProperty("velocity_stage_time")
    Long velocityStageTime;

    @JsonProperty("velocity_stages")
    List<VelocityStageTime> velocityStages;

    @JsonProperty("asof_status")
    String asOfStatus;

    @JsonProperty("old_issue_key")
    String oldIssueKey;

    @JsonProperty("response_time")
    Long responseTime;

    @JsonProperty("solve_time")
    Long solveTime;

    @JsonProperty("ticket_portion")
    Double ticketPortion;

    @JsonProperty("story_points_portion")
    Double storyPointsPortion;

    @JsonProperty("assignee_time")
    Long assigneeTime;

    @Value
    @Builder(toBuilder = true)
    public static class JiraIssueSprintEvent {
        String sprintId;
        JiraSprintEventType eventType;
        Long startTime;
        Long endTime;

        public enum JiraSprintEventType {
            ADDED, REMOVED
        }
    }

    public String toString() {
        return DefaultObjectMapper.writeAsPrettyJson(this);
    }
}
