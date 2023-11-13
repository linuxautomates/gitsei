package io.levelops.commons.databases.models.database.temporary;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.integrations.bitbucket.models.BitbucketPullRequest;
import io.levelops.integrations.github.models.GithubPullRequest;
import io.levelops.integrations.github.models.GithubReview;
import io.levelops.integrations.github.models.GithubUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Log4j2
public class TempGitPullRequest {
    @JsonProperty("id")
    private String id;

    @JsonProperty("creator_name")
    private String creatorName;

    @JsonProperty("created_date")
    private Long createdDate;

    @JsonProperty("updated_date")
    private Long updatedDate;

    @JsonProperty("merged_date")
    private Long mergedDate;

    @JsonProperty("closed_date")
    private Long closedDate;

    @JsonProperty("merge_sha")
    private String mergeSha;

    @JsonProperty("reviewers")
    private List<String> reviewers;

    @JsonProperty("assignees")
    private List<String> assignees;

    @JsonProperty("labels")
    private List<String> labels;

    @JsonProperty("reviews")
    private List<String> reviews;

    @JsonProperty("repo_name")
    private String repoName;

    public static TempGitPullRequest fromGithubPullRequest(GithubPullRequest source, String repoName) {
        if (source == null) {
            log.warn("NULL data provided for github PR parsing.");
            return null;
        }
        return TempGitPullRequest.builder()
                .id(source.getId()+":"+repoName)
                .assignees(IterableUtils.parseIterable(source.getAssignees(),
                        GithubUser::getLogin))
                .reviewers(IterableUtils.parseIterable(source.getRequestedReviewers(),
                        GithubUser::getLogin))
                .createdDate(DateUtils.toEpochSecond(source.getCreatedAt()))
                .creatorName(source.getUser().getLogin())
                .updatedDate(DateUtils.toEpochSecond(source.getUpdatedAt()))
                .mergedDate(DateUtils.toEpochSecond(source.getMergedAt()))
                .closedDate(DateUtils.toEpochSecond(source.getClosedAt()))
                .mergeSha(source.getMergeCommitSha())
                .labels(IterableUtils.parseIterable(source.getLabels(),
                        GithubPullRequest.Label::getName))
                .reviews(IterableUtils.parseIterable(source.getReviews(),
                        GithubReview::getState))
                .repoName(repoName)
                .build();
    }

    public static TempGitPullRequest fromBitbucketPullRequest(BitbucketPullRequest source, String repoName) {
        if (source == null) {
            log.warn("NULL data provided for github PR parsing.");
            return null;
        }
        List<String> participants = source.getParticipants()
                .stream().map(p -> p.getUser().getUsername())
                .collect(Collectors.toList());
        List<String> reviews = source.getParticipants().stream()
                .map(p -> (Boolean.TRUE.equals(p.getApproved())) ? "APPROVED" : "OPEN")
                .collect(Collectors.toList());

        return TempGitPullRequest.builder()
                .id(source.getId()+":"+repoName)
                .assignees(participants)
                .reviewers(participants)
                .createdDate(DateUtils.toEpochSecond(source.getCreatedOn()))
                .creatorName(source.getAuthor().getDisplayName())
                .updatedDate(DateUtils.toEpochSecond(source.getUpdatedOn()))
                .mergedDate((source.getMergeCommit()!= null) ? DateUtils.toEpochSecond(source.getUpdatedOn()) : null)
                .closedDate((source.getClosedBy()!= null) ? DateUtils.toEpochSecond(source.getUpdatedOn()) : null)
                .mergeSha((source.getMergeCommit()!= null) ? source.getMergeCommit().getHash() : null)
                .labels(Collections.emptyList())
                .reviews(reviews)
                .repoName(repoName)
                .build();
    }
}
