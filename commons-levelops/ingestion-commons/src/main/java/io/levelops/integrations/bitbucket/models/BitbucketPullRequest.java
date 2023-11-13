package io.levelops.integrations.bitbucket.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketPullRequest.BitbucketPullRequestBuilder.class)
public class BitbucketPullRequest {
    //region Data Members
    @JsonProperty("id")
    Integer id;
    @JsonProperty("description")
    String description;
    @JsonProperty("title")
    String title;
    @JsonProperty("close_source_branch")
    Boolean closeSourceBranch;
    @JsonProperty("type")
    String type; // "pullrequest"
    @JsonProperty("created_on")
    Date createdOn;
    @JsonProperty("source")
    Ref source;
    @JsonProperty("destination")
    Ref destination;
    @JsonProperty("summary")
    BitbucketSummary summary;
    @JsonProperty("comment_count")
    Integer commentCount;
    @JsonProperty("state")
    String state; // "MERGED"
    @JsonProperty("task_count")
    Integer taskCount;
    @JsonProperty("reason")
    String reason;
    @JsonProperty("updated_on")
    Date updatedOn;
    @JsonProperty("author")
    BitbucketUser author;
    @JsonProperty("closed_by")
    BitbucketUser closedBy;
    @JsonProperty("merge_commit")
    BitbucketCommitRef mergeCommit;
    @JsonProperty("participants")
    List<Participant> participants;

    /**
     * Enriched. This is called "approvals" for backward compatiblity,
     * but PR activity contains comments, merges, etc.
     */
    @JsonProperty("approvals")
    List<BitbucketPullRequestActivity> approvals; //enriched
    //endregion

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Ref.RefBuilder.class)
    public static class Ref {
        @JsonProperty("commit")
        BitbucketCommitRef commit;
        @JsonProperty("repository")
        BitbucketRepoRef repository;
        @JsonProperty("branch")
        Branch branch;
    }



    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Branch.BranchBuilder.class)
    public static class Branch {
        @JsonProperty("name")
        String name;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Participant.ParticipantBuilder.class)
    public static class Participant {
        @JsonProperty("role")
        String role;
        @JsonProperty("participated_on")
        String participatedOn;
        @JsonProperty("type")
        String type;
        @JsonProperty("approved")
        Boolean approved;
        @JsonProperty("user")
        BitbucketUser user;
    }

}
