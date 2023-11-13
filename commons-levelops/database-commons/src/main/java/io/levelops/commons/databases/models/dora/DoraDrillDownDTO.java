package io.levelops.commons.databases.models.dora;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.issue_management.DbWorkItemHistory;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.jira.DbJiraAssignee;
import io.levelops.commons.databases.models.database.jira.DbJiraStatus;
import io.levelops.commons.databases.models.database.jira.DbJiraStoryPoints;
import io.levelops.commons.databases.models.database.jira.VelocityStageTime;
import io.levelops.commons.databases.models.database.scm.DbScmCommitStats;
import io.levelops.commons.databases.models.database.scm.DbScmPRLabel;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DoraDrillDownDTO.DoraDrillDownDTOBuilder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Log4j2
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DoraDrillDownDTO {

    @JsonProperty("id")
    String id;

    @JsonProperty("key")
    String key;

    @JsonProperty("is_active")
    Boolean isActive;

    @JsonProperty("issue_type")
    String issueType;

    @JsonProperty("parent_key")
    String parentKey;

    @JsonProperty("issue_created_at")
    Long issueCreatedAt;

    @JsonProperty("issue_updated_at")
    Long issueUpdatedAt;

    @JsonProperty("issue_resolved_at")
    Long issueResolvedAt;

    @JsonProperty("issue_due_at")
    Long issueDueAt;

    @JsonProperty("first_assigned_at")
    Long firstAssignedAt;

    @JsonProperty("first_assignee")
    String firstAssignee;

    @JsonProperty("first_assignee_id")
    String firstAssigneeId;

    @JsonProperty("state_transition_time")
    Long stateTransitionTime;

    @JsonProperty("salesforce_fields")
    Map<String, List<String>> salesforceFields;

    //this is the current list of components on the ticket. stored in a separate table
    @JsonProperty("component_list")
    List<String> components;

    // enriched
    @JsonProperty("priority_order")
    Integer priorityOrder;

    @JsonProperty("story_points")
    Integer storyPoints;

    @JsonIgnore
    List<DbJiraStoryPoints> storyPointsLogs;

    @JsonProperty("velocity_stage")
    String velocityStage;

    @JsonProperty("velocity_stage_time")
    Long velocityStageTime;

    @JsonProperty("velocity_stages")
    List<VelocityStageTime> velocityStages;

    @JsonProperty("asof_status")
    String asOfStatus;

    @JsonProperty("old_issue_key")
    String oldIssueKey;

    @JsonProperty(value = "components")
    private List<String> componentsAzure; // Only name were same not json value

    @JsonProperty("is_active_azure")
    @Builder.Default
    private Boolean isActiveAzure = true;

    // Common for JIRA and SCM
    @JsonProperty("created_at_im")
    Long createdAtIM;

    // Common for AZURE and SCM

    @JsonProperty(value = "project_im")
    private String projectIM;


    @JsonProperty("integration_id_im")
    private String integrationIdIM;

    @JsonProperty("labels_im")
    private List<String> labelsIM;

    @JsonProperty("commit_sha")
    private String commitSha;

    @JsonProperty("committer")
    private String committer;

    @JsonProperty("message")
    private String message;

    @JsonProperty("commit_pushed_at")
    private Long commitPushedAt;

    @JsonProperty("committed_at")
    private Long committedAt;

    @JsonProperty("branch")
    private String branch;

    // AZURE

    public static final Float DEFAULT_STORY_POINTS = 0f;

    public static final String VALUE_SEPARATOR = "; ";

    @JsonProperty("idAzure")
    private UUID idAzure;

    @JsonProperty("workitem_id")
    private String workItemId;

    @JsonProperty(value = "summary")
    private String summary;

    @JsonProperty(value = "priority")
    private String priority;

    @JsonProperty(value = "assignee")
    private String assignee;

    @JsonProperty(value = "epic")
    private String epic;

    @JsonProperty(value = "parent_workitem_id")
    private String parentWorkItemId;

    @JsonProperty(value = "reporter")
    private String reporter;

    @JsonProperty("assignee_info")
    private DbScmUser assigneeInfo; //contains DbScmUser object for assignee

    @JsonProperty("assignee_id")
    private String assigneeId;

    @JsonProperty("reporter_info")
    private DbScmUser reporterInfo; //contains DbScmUser object for reporter

    @JsonProperty("reporter_id")
    private String reporterId;

    @JsonProperty(value = "status")
    private String status;

    @JsonProperty(value = "workitem_type")
    private String workItemType;

    @JsonProperty(value = "story_point")
    private Float storyPoint;

    @JsonProperty(value = "ingested_at")
    private Long ingestedAt;

    @JsonProperty(value = "custom_fields")
    private Map<String, Object> customFields;

    @JsonProperty(value = "versions")
    private List<String> versions;

    @JsonProperty(value = "fix_versions")
    private List<String> fixVersions;

    @JsonProperty(value = "resolution")
    private String resolution;

    @JsonProperty(value = "status_category")
    private String statusCategory;

    @JsonProperty("ticket_category")
    String ticketCategory;

    @JsonProperty(value = "desc_size")
    private Integer descSize;

    @JsonProperty(value = "hops")
    private Integer hops;

    @JsonProperty(value = "bounces")
    private Integer bounces;

    @JsonProperty(value = "num_attachments")
    private Integer numAttachments;

    @JsonProperty(value = "workitem_created_at")
    private Timestamp workItemCreatedAt;

    @JsonProperty(value = "workitem_updated_at")
    private Timestamp workItemUpdatedAt;

    @JsonProperty(value = "workitem_resolved_at")
    private Timestamp workItemResolvedAt;

    @JsonProperty(value = "workitem_due_at")
    private Timestamp workItemDueAt;

    @JsonProperty(value = "attributes")
    private Map<String, Object> attributes;

    @JsonProperty(value = "completedWork")
    private Float completedWork;

    @JsonProperty(value = "remainingWork")
    private String remainingWork;

    @JsonProperty(value = "statusChangeDate")
    private Timestamp statusChangeDate;

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

    // CICD
    @JsonProperty("idCicd")
    private final UUID idCicd;
    @JsonProperty("cicd_job_id")
    private final UUID cicdJobId;
    @JsonProperty("job_run_number")
    private final Long jobRunNumber;
    @JsonProperty("statusCicd")
    private final String statusCicd;
    @JsonProperty("start_time")
    private final Instant startTime;
    @JsonProperty("duration")
    private final Integer duration;
    @JsonProperty("end_time")
    private final Instant endTime;
    @JsonProperty("cicd_user_id")
    private final String cicdUserId;
    @JsonProperty("params")
    private final List<CICDJobRun.JobRunParam> params;
    @JsonProperty("job_name")
    private final String jobName;
    @JsonProperty("job_normalized_full_name")
    private final String jobNormalizedFullName;
    @JsonProperty("project_name")
    private final String projectName;
    @JsonProperty("scm_commit_ids")
    private final List<String> scmCommitIds;
    @JsonProperty("scm_url")
    private final String scmUrl;
    @JsonProperty("log_gcspath")
    private final String logGcspath;
    @JsonProperty("cicd_instance_name")
    private final String cicdInstanceName;
    @JsonProperty("cicd_build_url")
    private final String cicdBuildUrl;
    @JsonProperty("cicd_instance_guid")
    private final UUID cicdInstanceGuid;
    @JsonProperty("url")
    private final String url;
    @JsonProperty("logs")
    private final Boolean logs;
    @JsonProperty("env_ids")
    private final List<String> environmentIds;
    @JsonProperty("infra_ids")
    private final List<String> infraIds;
    @JsonProperty("service_ids")
    private final List<String> serviceIds;
    @JsonProperty("service_types")
    private final List<String> serviceTypes;
    @JsonProperty("tags")
    private final List<String> tags;
    @JsonProperty("repo_url")
    private final String repoUrl;
    @JsonProperty("cicd_branch")
    private final String cicdBranch;
    @JsonProperty("rollback")
    private final Boolean rollBack;

    // SCM
    @JsonProperty("id_scm")
    @JsonAlias({"pr_id"})
    private String idScm;
    @JsonProperty("repo_id")
    @JsonAlias({"pr_repo_id"})
    private List<String> repoIds;
    @JsonProperty("number")
    @JsonAlias({"pr_number"})
    private String number;
    @JsonProperty("pr_link")
    private String prLink;
    @JsonProperty("creator")
    @JsonAlias({"pr_creator"})
    private String creator;
    @JsonProperty("merge_sha")
    @JsonAlias({"pr_merge_sha"})
    private String mergeSha;
    @JsonProperty("title")
    @JsonAlias({"pr_title"})
    private String title;
    @JsonProperty("source_branch")
    @JsonAlias({"pr_source_branch"})
    private String sourceBranch;
    @JsonProperty("target_branch")
    @JsonAlias({"pr_target_branch"})
    private String targetBranch;
    @JsonProperty("state")
    @JsonAlias({"pr_state"})
    private String state;
    @JsonProperty("merged")
    @JsonAlias({"pr_merged"})
    private Boolean merged;
    @JsonProperty("lines_added")
    @JsonAlias({"pr_lines_added"})
    private String linesAdded;
    @JsonProperty("lines_deleted")
    @JsonAlias({"pr_lines_deleted"})
    private String linesDeleted;
    @JsonProperty("lines_changed")
    @JsonAlias({"pr_lines_changed"})
    private String linesChanged;
    @JsonProperty("files_changed")
    @JsonAlias({"pr_files_changed"})
    private String filesChanged;
    @JsonProperty("code_change")
    @JsonAlias({"pr_code_change"})
    private String codeChange;
    @JsonProperty("comment_density")
    @JsonAlias({"pr_comment_density"})
    private String commentDensity;
    @JsonProperty("has_issue_keys")
    @JsonAlias({"pr_has_issue_keys"})
    private Boolean hasIssueKeys;
    @JsonProperty("review_type")
    @JsonAlias({"pr_review_type"})
    private String reviewType;
    @JsonProperty("collab_state")
    @JsonAlias({"pr_collab_state"})
    private String collabState;
    @JsonProperty("assignees")
    @JsonAlias({"pr_assignees"})
    private List<String> assignees;
    @JsonProperty("assignee_ids")
    @JsonAlias({"pr_assignee_ids"})
    private List<String> assigneeIds;
    @JsonProperty("assignees_info")
    @JsonAlias({"pr_assignees_info"})
    private List<DbScmUser> assigneesInfo;
    @JsonProperty("reviewers")
    @JsonAlias({"pr_reviewers"})
    private List<String> reviewers;
    @JsonProperty("reviewer_ids")
    @JsonAlias({"pr_reviewer_ids"})
    private List<String> reviewerIds;
    @JsonProperty("commenters")
    @JsonAlias({"pr_commenters"})
    private List<String> commenters;
    @JsonProperty("commenter_ids")
    @JsonAlias({"pr_commenter_ids"})
    private List<String> commenterIds;
    @JsonProperty("approver_ids")
    @JsonAlias({"pr_approver_ids"})
    private List<String> approverIds;
    @JsonProperty("approvers")
    @JsonAlias({"pr_approvers"})
    private List<String> approvers;
    @JsonProperty("pr_labels")
    @JsonAlias({"pr_pr_labels"})
    private List<DbScmPRLabel> prLabels;
    @JsonProperty("commit_shas")
    @JsonAlias({"pr_commit_shas"})
    private List<String> commitShas;
    @JsonProperty("reviews")
    @JsonAlias({"pr_reviews"})
    private List<DbScmReview> reviews;
    @JsonProperty("pr_updated_at")
    @JsonAlias({"pr_pr_updated_at"})
    private Long prUpdatedAt;
    @JsonProperty("pr_merged_at")
    @JsonAlias({"pr_pr_merged_at"})
    private Long prMergedAt;
    @JsonProperty("pr_closed_at")
    @JsonAlias({"pr_pr_closed_at"})
    private Long prClosedAt;
    @JsonProperty("pr_created_at")
    @JsonAlias({"pr_pr_created_at"})
    private Long prCreatedAt;
    @JsonProperty("avg_author_response_time")
    @JsonAlias({"pr_avg_author_response_time"})
    private Long avgAuthorResponseTime;
    @JsonProperty("avg_reviewer_response_time")
    @JsonAlias({"pr_avg_reviewer_response_time"})
    private Long avgReviewerResponseTime;
    @JsonProperty("comment_time")
    @JsonAlias({"pr_comment_time"})
    private Long commentTime;
    @JsonProperty("approval_time")
    @JsonAlias({"pr_approval_time"})
    private Long approvalTime;
    @JsonProperty("creator_info")
    @JsonAlias({"pr_creator_info"})
    private DbScmUser creatorInfo;
    @JsonProperty("creator_id")
    @JsonAlias({"pr_creator_id"})
    private String creatorId;
    @JsonProperty("workitem_ids")
    @JsonAlias({"pr_workitem_ids"})
    List<String> workitemIds;
    @JsonProperty("issue_keys")
    @JsonAlias({"pr_issue_keys"})
    private List<String> issueKeys;
    @JsonProperty("reviewers_info")
    @JsonAlias({"pr_reviewers_info"})
    private List<DbScmReview> reviewerInfo;
    @JsonProperty("commenters_info")
    @JsonAlias({"pr_commenters_info"})
    private List<DbScmReview> commenterInfo;
    @JsonProperty("approvers_info")
    @JsonAlias({"pr_approvers_info"})
    private List<DbScmReview> approverInfo;
    @JsonProperty("created_day")
    @JsonAlias({"pr_created_day"})
    private String createdDay;
    @JsonProperty("merged_day")
    @JsonAlias({"pr_merged_day"})
    private String mergedDay;
    @JsonProperty("closed_day")
    @JsonAlias({"pr_closed_day"})
    private String closedDay;
    @JsonProperty("commits_stat")
    @JsonAlias({"pr_commits_stat"})
    private List<DbScmCommitStats> commitStats;
    @JsonProperty("additions")
    @JsonAlias({"pr_additions"})
    private Integer additions;
    @JsonProperty("deletions")
    @JsonAlias({"pr_deletions"})
    private Integer deletions;
    @JsonProperty("changes")
    @JsonAlias({"pr_changes"})
    private Integer change;
    @JsonProperty("files_count")
    @JsonAlias({"pr_files_count"})
    private Integer filesCount;
    @JsonProperty("comment_count")
    @JsonAlias({"pr_comment_count"})
    private Integer commentCount;
    @JsonProperty("reviewer_count")
    @JsonAlias({"pr_reviewer_count"})
    private Integer reviewerCount;
    @JsonProperty("approver_count")
    @JsonAlias({"pr_approver_count"})
    private Integer approverCount;
    @JsonProperty("approval_status")
    @JsonAlias({"pr_approval_status"})
    private String approvalStatus;
    @JsonProperty("technology")
    @JsonAlias({"pr_technology"})
    private List<String> technology;
    @JsonProperty("first_committed_at")
    @JsonAlias({"pr_first_committed_at"})
    private Long firstCommittedAt;
    @JsonProperty("cycle_time")
    @JsonAlias({"pr_cycle_time"})
    private Long cycleTime;
    @JsonProperty("review_cycle_time")
    @JsonAlias({"pr_review_cycle_time"})
    private Long reviewCycleTime;
    @JsonProperty("review_merge_cycle_time")
    @JsonAlias({"pr_review_merge_cycle_time"})
    private Long reviewMergeCycleTime;
    @JsonProperty("created_at")
    @JsonAlias({"pr_createdat"})
    private Long createdAt;
    @JsonProperty("project")
    @JsonAlias({"pr_project"})
    private String project;
    @JsonProperty("integration_id")
    @JsonAlias({"pr_integration_id"})
    private String integrationId;
    @JsonProperty("labels")
    @JsonAlias({"pr_label"})
    private List<String> labels;

    // DF for jira release
    @JsonProperty("name")
    private final String name;
    @JsonProperty("issue_count")
    private final Integer issueCount;
    @JsonProperty("released_end_time")
    private final Long releaseEndTime;

}
