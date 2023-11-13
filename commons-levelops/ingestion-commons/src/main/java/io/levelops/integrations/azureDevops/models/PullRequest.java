package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PullRequest.PullRequestBuilder.class)
public class PullRequest {

    @JsonProperty("repository")
    Repository repository;

    @JsonProperty("labels")
    List<Label> labels; //enriched.

    @JsonProperty("commits")
    List<CommitInfo> commits; //enriched.

    @JsonProperty("pullRequestId")
    int pullRequestId;

    @JsonProperty("codeReviewId")
    int codeReviewId;

    @JsonProperty("status")
    String status;

    @JsonProperty("createdBy")
    IdentityRef createdBy;

    @JsonProperty("closedBy")
    IdentityRef closedBy;

    @JsonProperty("creationDate")
    String creationDate;

    @JsonProperty("closedDate")
    String closedDate;

    @JsonProperty("title")
    String title;

    @JsonProperty("description")
    String description;

    @JsonProperty("sourceRefName")
    String sourceRefName;

    @JsonProperty("targetRefName")
    String targetRefName;

    @JsonProperty("mergeStatus")
    String mergeStatus;

    @JsonProperty("mergeId")
    String mergeId;

    @JsonProperty("lastMergeSourceCommit")
    CommitInfo lastMergeSourceCommit;

    @JsonProperty("lastMergeTargetCommit")
    CommitInfo lastMergeTargetCommit;

    @JsonProperty("lastMergeCommit")
    CommitInfo lastMergeCommit;

    @JsonProperty("reviewers")
    List<IdentityRef> reviewers;

    @JsonProperty("pullRequestThread")
    List<PullRequestHistory> pullRequestHistories;

    @JsonProperty("url")
    String url;


    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CommitInfo.CommitInfoBuilder.class)
    public static class CommitInfo {

        @JsonProperty("commitId")
        String commitId;

        @JsonProperty("url")
        String url;

        @JsonProperty("committer")
        CommiterInfo committer;

    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CommiterInfo.CommiterInfoBuilder.class)
    public static class CommiterInfo {

        @JsonProperty("id")
        String name;

        @JsonProperty("name")
        String email;

        @JsonProperty("date")
        String date;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = IdentityRef.IdentityRefBuilder.class)
    public static class IdentityRef {

        @JsonProperty("id")
        String id;

        @JsonProperty("reviewerUrl")
        String reviewerUrl;

        @JsonProperty("vote")
        int vote;

        @JsonProperty("hasDeclined")
        Boolean hasDeclined;

        @JsonProperty("isFlagged")
        Boolean isFlagged;

        @JsonProperty("displayName")
        String displayName;

        @JsonProperty("url")
        String url;

        @JsonProperty("uniqueName")
        String uniqueName;
    }

    @JsonProperty("id")
    String id;


    @JsonProperty("imageUrl")
    String imageUrl;

    @JsonProperty("descriptor")
    String descriptor;
}
