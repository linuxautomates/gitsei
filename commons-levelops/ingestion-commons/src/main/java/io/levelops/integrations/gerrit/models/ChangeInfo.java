package io.levelops.integrations.gerrit.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Bean describing a Change from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-changes.html#change-info
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ChangeInfo.ChangeInfoBuilder.class)
public class ChangeInfo {

    @JsonProperty
    String id;

    @JsonProperty
    String project;

    @JsonProperty
    String branch;

    @JsonProperty
    String topic;

    @JsonProperty
    AccountInfo assignee;

    @JsonProperty
    List<String> hashtags;

    @JsonProperty("change_id")
    String changeId;

    @JsonProperty
    String subject;

    @JsonProperty
    String status;

    @JsonProperty
    LocalDateTime created;

    @JsonProperty
    LocalDateTime updated;

    @JsonProperty
    LocalDateTime submitted;

    @JsonProperty
    AccountInfo submitter;

    @JsonProperty(defaultValue = "false")
    Boolean starred;

    @JsonProperty
    List<String> stars;

    @JsonProperty(defaultValue = "false")
    Boolean reviewed;

    @JsonProperty("submit_type")
    String submitType;

    @JsonProperty
    Boolean mergeable;

    @JsonProperty
    Boolean submittable;

    @JsonProperty
    Integer insertions;

    @JsonProperty
    Integer deletions;

    @JsonProperty("total_comment_count")
    Integer totalCommentCount;

    @JsonProperty("unresolved_comment_count")
    Integer unresolvedCommentCount;

    @JsonProperty("_number")
    Integer number;

    @JsonProperty
    AccountInfo owner;

    @JsonProperty
    Map<String, ActionInfo> actions;

    @JsonProperty
    List<Requirement> requirements;

    @JsonProperty
    Map<String, LabelInfo> labels;

    @JsonProperty("permitted_labels")
    Map<String, List<String>> permittedLabels;

    @JsonProperty("removable_reviewers")
    List<AccountInfo> removableReviewers;

    @JsonProperty
    Map<String, List<AccountInfo>> reviewers;

    @JsonProperty("pending_reviewers")
    Map<String, List<AccountInfo>> pendingReviewers;

    @JsonProperty("reviewer_updates")
    List<ReviewerUpdateInfo> reviewerUpdates;

    @JsonProperty
    List<ChangeMessageInfo> messages;

    @JsonProperty("current_revision")
    String currentRevision;

    @JsonProperty
    Map<String, RevisionInfo> revisions;

    @JsonProperty("tracking_ids")
    List<TrackingIdInfo> trackingIds;

    @JsonProperty(value = "_more_changes", defaultValue = "false")
    Boolean moreChanges;

    @JsonProperty
    List<ProblemInfo> problems;

    @JsonProperty(value = "is_private", defaultValue = "false")
    Boolean isPrivate;

    @JsonProperty(value = "work_in_progress", defaultValue = "false")
    Boolean workInProgress;

    @JsonProperty(value = "has_review_started", defaultValue = "false")
    Boolean hasReviewStarted;

    @JsonProperty("revert_of")
    String revertOf;

    @JsonProperty("submission_id")
    String submissionId;

    @JsonProperty("cherry_pick_of_change")
    String cherryPickOfChange;

    @JsonProperty("cherry_pick_of_patch_set")
    String cherryPickOfPatchSet;

    @JsonProperty(value = "contains_git_conflicts", defaultValue = "false")
    Boolean containsGitConflicts;

    /**
     * Bean describing a ActionInfo from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-changes.html#action-info
     */
    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ActionInfo.ActionInfoBuilder.class)
    public static class ActionInfo {

        @JsonProperty
        String method;

        @JsonProperty
        String label;

        @JsonProperty
        String title;

        @JsonProperty
        Boolean enabled;
    }

    /**
     * Bean describing a Requirement from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-changes.html#requirement
     */
    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Requirement.RequirementBuilder.class)
    public static class Requirement {

        @JsonProperty
        String status;

        @JsonProperty
        String fallbackText;

        @JsonProperty
        String type;
    }

    /**
     * Bean describing a Label from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-changes.html#label-info
     */
    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = LabelInfo.LabelInfoBuilder.class)
    public static class LabelInfo {

        @JsonProperty(defaultValue = "false")
        Boolean optional;

        @JsonProperty
        AccountInfo approved;

        @JsonProperty
        AccountInfo rejected;

        @JsonProperty
        AccountInfo recommended;

        @JsonProperty
        AccountInfo disliked;

        @JsonProperty(defaultValue = "false")
        Boolean blocking;

        @JsonProperty
        String value;

        @JsonProperty("default_value")
        String defaultValue;

        @JsonProperty
        List<ApprovalInfo> all;

        @JsonProperty
        Map<String, String> values;

        /**
         * Bean describing a ApprovalInfo from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-changes.html#approval-info
         */
        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = ApprovalInfo.ApprovalInfoBuilder.class)
        public static class ApprovalInfo {

            @JsonProperty
            String value;

            @JsonProperty("permitted_voting_range")
            VotingRangeInfo permittedVotingRange;

            @JsonProperty
            LocalDateTime date;

            @JsonProperty
            String tag;

            @JsonProperty(value = "post_submit", defaultValue = "false")
            Boolean postSubmit;

            @JsonProperty("_account_id")
            String accountId;

            @JsonProperty
            String name;

            @JsonProperty("display_name")
            String displayName;

            @JsonProperty("email")
            String email;

            @JsonProperty("seconday_emails")
            List<String> secondaryEmails;

            @JsonProperty
            String username;

            @JsonProperty
            List<AccountInfo.AvatarInfo> avatars;

            @JsonProperty
            Boolean moreAccounts;

            @JsonProperty
            String status;

            @JsonProperty(defaultValue = "false")
            Boolean inactive;

            /**
             * Bean describing a VotingRangeInfo from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-changes.html#approval-info
             */
            @Value
            @Builder(toBuilder = true)
            @JsonDeserialize(builder = VotingRangeInfo.VotingRangeInfoBuilder.class)
            public static class VotingRangeInfo {

                @JsonProperty
                Integer min;

                @JsonProperty
                Integer max;
            }
        }
    }

    /**
     * Bean describing a ReviewerUpdateInfo from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-changes.html#review-update-info
     */
    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ReviewerUpdateInfo.ReviewerUpdateInfoBuilder.class)
    public static class ReviewerUpdateInfo {

        @JsonProperty
        LocalDateTime updated;

        @JsonProperty
        AccountInfo updatedBy;

        @JsonProperty
        AccountInfo reviewer;

        @JsonProperty
        String state;
    }

    /**
     * Bean describing a ChangeMessageInfo from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-changes.html#change-message-info
     */
    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ChangeMessageInfo.ChangeMessageInfoBuilder.class)
    public static class ChangeMessageInfo {

        @JsonProperty
        String id;

        @JsonProperty
        AccountInfo author;

        @JsonProperty("real_author")
        AccountInfo realAuthor;

        @JsonProperty
        LocalDateTime date;

        @JsonProperty
        String message;

        @JsonProperty
        String tag;

        @JsonProperty("_revision_number")
        String revisionNumber;
    }

    /**
     * Bean describing a TrackingIdInfo from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-changes.html#tracking-id-info
     */
    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = TrackingIdInfo.TrackingIdInfoBuilder.class)
    public static class TrackingIdInfo {

        @JsonProperty
        String system;

        @JsonProperty
        String id;
    }

    /**
     * Bean describing a ProblemInfo from https://gerrit-documentation.storage.googleapis.com/Documentation/3.2.2/rest-api-changes.html#change-info
     */
    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ProblemInfo.ProblemInfoBuilder.class)
    public static class ProblemInfo {

        @JsonProperty
        String message;

        @JsonProperty
        String status;

        @JsonProperty
        String outcome;
    }
}
