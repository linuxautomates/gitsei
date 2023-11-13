package io.levelops.integrations.bitbucket.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketPullRequestActivity.BitbucketPullRequestActivityBuilder.class)
public class BitbucketPullRequestActivity {

    /**
     * When a PR is approved.
     */
    @JsonProperty("approval")
    Approval approval;

    /**
     * When a PR is open, closed, or merged.
     */
    @JsonProperty("update")
    Update update;

    /**
     * For all comments (inline or not).
     */
    @JsonProperty("comment")
    Comment comment;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Approval.ApprovalBuilder.class)
    public static class Approval {

        @JsonProperty("date")
        Date date;

        @JsonProperty("user")
        BitbucketUser user;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Update.UpdateBuilder.class)
    public static class Update {

        @JsonProperty("state")
        String state; // "OPEN", "MERGED", "DECLINED"

        @JsonProperty("date")
        Date date;

        @JsonProperty("author")
        BitbucketUser author;

        /**
         * When declining, the user can provide a reason.
         */
        @JsonProperty("reason")
        String reason;

        // there are more fields, not needed for now: title, description, reviewers, changes, source, destination
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Comment.CommentBuilder.class)
    public static class Comment {

        @JsonProperty("id")
        Long id;

        @JsonProperty("created_on")
        Date createdOn;

        @JsonProperty("updated_on")
        Date updatedOn;

        @JsonProperty("content")
        Content content;

        @JsonProperty("user")
        BitbucketUser user;

        @JsonProperty("deleted")
        Boolean deleted;

        // there are more fields, not needed for now: inline (which line, etc.), type

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = Content.ContentBuilder.class)
        public static class Content {

            @JsonProperty("raw")
            String raw;
        }
    }

}
