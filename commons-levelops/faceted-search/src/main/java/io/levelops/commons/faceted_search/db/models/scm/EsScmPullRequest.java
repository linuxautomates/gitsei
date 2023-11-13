package io.levelops.commons.faceted_search.db.models.scm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.scm.DbScmCommitStats;
import io.levelops.commons.databases.models.database.scm.DbScmPRLabel;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = EsScmPullRequest.EsScmPullRequestBuilder.class)
public class EsScmPullRequest {

   @JsonProperty("pr_id")
    private String id;

   @JsonProperty("pr_repo_id")
    private List<String> repoIds;

   @JsonProperty("pr_project")
    private String project;

   @JsonProperty("pr_number")
    private String number;

   @JsonProperty("pr_integration_id")
    private String integrationId;

   @JsonProperty("pr_creator")
    private String creator;

   @JsonProperty("pr_merge_sha")
    private String mergeSha;

   @JsonProperty("pr_title")
    private String title;

   @JsonProperty("pr_source_branch")
    private String sourceBranch;

   @JsonProperty("pr_target_branch")
    private String targetBranch;

   @JsonProperty("pr_state")
    private String state;

   @JsonProperty("pr_merged")
    private Boolean merged;

   @JsonProperty("pr_lines_added")
    private String linesAdded;

   @JsonProperty("pr_lines_deleted")
    private String linesDeleted;

   @JsonProperty("pr_lines_changed")
    private String linesChanged;

   @JsonProperty("pr_files_changed")
    private String filesChanged;

   @JsonProperty("pr_code_change")
    private String codeChange;

   @JsonProperty("pr_comment_density")
    private String commentDensity;

   @JsonProperty("pr_has_issue_keys")
    private Boolean hasIssueKeys;

   @JsonProperty("pr_review_type")
    private String reviewType;

   @JsonProperty("pr_collab_state")
    private String collabState;

   @JsonProperty("pr_assignees")
    private List<String> assignees;

   @JsonProperty("pr_assignee_ids")
    private List<String> assigneeIds;

   @JsonProperty("pr_assignees_info")
    private List<DbScmUser> assigneesInfo;

   @JsonProperty("pr_reviewers")
    private List<String> reviewers;

   @JsonProperty("pr_reviewer_ids")
    private List<String> reviewerIds;

   @JsonProperty("pr_commenters")
    private List<String> commenters;

   @JsonProperty("pr_commenter_ids")
    private List<String> commenterIds;

   @JsonProperty("pr_approver_ids")
    private List<String> approverIds;

   @JsonProperty("pr_approvers")
    private List<String> approvers;

   @JsonProperty("pr_label")
    private List<String> labels;

   @JsonProperty("pr_pr_labels")
    private List<DbScmPRLabel> prLabels;

   @JsonProperty("pr_commit_shas")
    private List<String> commitShas;

   @JsonProperty("pr_reviews")
    private List<DbScmReview> reviews;

   @JsonProperty("pr_pr_updated_at")
    private Long prUpdatedAt;

   @JsonProperty("pr_pr_merged_at")
    private Long prMergedAt;

   @JsonProperty("pr_pr_closed_at")
    private Long prClosedAt;

   @JsonProperty("pr_pr_created_at")
    private Long prCreatedAt;

   @JsonProperty("pr_createdat")
    private Long createdAt;

   @JsonProperty("pr_avg_author_response_time")
    private Long avgAuthorResponseTime;

   @JsonProperty("pr_avg_reviewer_response_time")
    private Long avgReviewerResponseTime;

   @JsonProperty("pr_comment_time")
    private Long commentTime;

   @JsonProperty("pr_approval_time")
    private Long approvalTime;

   @JsonProperty("pr_creator_info")
    private DbScmUser creatorInfo; //contains DbScmUser object for creator

   @JsonProperty("pr_creator_id")
    private String creatorId; //refers to unique row in scm_users

   @JsonProperty("pr_workitem_ids")
    List<String> workitemIds;

   @JsonProperty("pr_issue_keys")
    private List<String> issueKeys;

   @JsonProperty("pr_reviewers_info")
    private List<DbScmReview> reviewerInfo;

   @JsonProperty("pr_commenters_info")
    private List<DbScmReview> commenterInfo;

   @JsonProperty("pr_approvers_info")
    private List<DbScmReview> approverInfo;

   @JsonProperty("pr_created_day")
    private String createdDay;

   @JsonProperty("pr_merged_day")
    private String mergedDay;

   @JsonProperty("pr_closed_day")
    private String closedDay;

   @JsonProperty("pr_commits_stat")
    private List<DbScmCommitStats> commitStats;

   @JsonProperty("pr_additions")
    private Integer additions;

   @JsonProperty("pr_deletions")
    private Integer deletions;

   @JsonProperty("pr_changes")
    private Integer change;

   @JsonProperty("pr_files_count")
    private Integer filesCount;

   @JsonProperty("pr_comment_count")
    private Integer commentCount;

   @JsonProperty("pr_reviewer_count")
    private Integer reviewerCount;

   @JsonProperty("pr_approver_count")
    private Integer approverCount;

   @JsonProperty("pr_approval_status")
    private String approvalStatus;

   @JsonProperty("pr_technology")
    private List<String> technology;

   @JsonProperty("pr_first_committed_at")
    private Long firstCommittedAt;

   @JsonProperty("pr_cycle_time")
    private Long cycleTime;

   @JsonProperty("pr_review_cycle_time")
    private Long reviewCycleTime;

   @JsonProperty("pr_review_merge_cycle_time")
    private Long reviewMergeCycleTime;

    @JsonProperty("pr_loc")
    private Integer loc;

}
