package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubReview.GithubReviewBuilder.class)
public class GithubReview {
    @JsonProperty("id")
    String id;

    @JsonProperty("user")
    GithubUser user;

    @JsonProperty("body")
    String body;

    @JsonProperty("state")
    String state; // "APPROVED"

    @JsonProperty("author_association")
    String authorAssociation;

    // There is also an updated_at field in the issue comments, but we're ignoring it for now
    @JsonProperty("submitted_at")
    @JsonAlias({"created_at"})
    Date submitted_at;

    // NOTE: Use with caution, issue comments don't have this field
    @JsonProperty("commit_id")
    String commitId;

    @JsonProperty("html_url")
    String htmlUrl;
}
