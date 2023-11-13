package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabIssue.GitlabIssueBuilder.class)
public class GitlabIssue {
    @JsonProperty("id")
    String id;
    @JsonProperty("iid")
    String iid;
    @JsonProperty("project_id")
    String projectId;
    @JsonProperty("title")
    String title;
    @JsonProperty("description")
    String description;
    @JsonProperty("state")
    String state;
    @JsonProperty("created_at")
    Date createdAt;
    @JsonProperty("updated_at")
    Date updatedAt;
    @JsonProperty("closed_by")
    GitlabUser closedBy;
    @JsonProperty("closed_at")
    Date closedAt;
    @JsonProperty("labels")
    List<String> labels;
    @JsonProperty("assignees")
    List<GitlabUser> assignees;
    @JsonProperty("author")
    GitlabUser author;
    @JsonProperty("assignee")
    GitlabUser assignee;
    @JsonProperty("user_notes_count")
    int userNotesCount;
    @JsonProperty("upvotes")
    int upVotes;
    @JsonProperty("downvotes")
    int downVotes;
    @JsonProperty("merge_requests_count")
    long mergeRequestsCount;
    @JsonProperty("due_date")
    Date dueDate;
    @JsonProperty("confidential")
    boolean confidential;
    @JsonProperty("web_url")
    String webUrl;
    @JsonProperty("has_tasks")
    boolean hasTasks;
    @JsonProperty("time_stats")
    TimeEstimate timeStats;
    @JsonProperty("task_completion_status")
    TaskCompletionStatus taskCompletionStatus;
    @JsonProperty("notes")
    List<GitlabIssueNote> notes;


    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = TimeEstimate.TimeEstimateBuilder.class)
    public static class TimeEstimate {
        @JsonProperty("time_estimate")
        long timeEstimate;
        @JsonProperty("total_time_spent")
        long totalTimeSpent;
        @JsonProperty("human_time_estimate")
        long humanTimeEstimate;
        @JsonProperty("human_total_time_spent")
        long humanTotalTimeSpent;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = TaskCompletionStatus.TaskCompletionStatusBuilder.class)
    public static class TaskCompletionStatus {
        @JsonProperty("count")
        long count;
        @JsonProperty("completed_count")
        long completedCount;
    }
}
