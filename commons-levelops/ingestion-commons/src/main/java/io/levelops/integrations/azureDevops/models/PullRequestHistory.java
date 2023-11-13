package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PullRequestHistory.PullRequestHistoryBuilder.class)
public class PullRequestHistory {

    @JsonProperty("pullRequestThreadContext")
    Object pullRequestThreadContext;

    @JsonProperty("id")
    String id;

    @JsonProperty("publishedDate")
    String publishedDate;

    @JsonProperty("lastUpdatedDate")
    String lastUpdatedDate;

    @JsonProperty("comments")
    List<Comment> comments;

    @JsonProperty("properties")
    Property property;

    @JsonProperty("identities")
    Map<String, IdentityRef> identities;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = PullRequestHistory.Comment.CommentBuilder.class)
    public static class Comment {

        @JsonProperty("author")
        IdentityRef identities;

        @JsonProperty("commentType")
        String commentType;
    }
}
